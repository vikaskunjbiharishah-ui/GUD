package `in`.growurdhan.data.vault

import `in`.growurdhan.core.model.*
import java.time.LocalDate
import java.time.Instant
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.util.UUID
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * M26: all family/account/goal edits run inside SqlCipherVaultRepository.mutate.
 * No ledger_event deletion or ownership rewrites. Inactive family members remain
 * persisted and reversible, with all event IDs and prior ownership unchanged.
 */
internal object FamilyAdministrationSql {
    private val relationships = setOf("Self", "Spouse", "Parent", "Child", "HUF", "Joint")
    private val colorPattern = Regex("^#[0-9a-fA-F]{6}$")
    private val panPattern = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")

    private fun clean(value: String?, max: Int = 200): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }?.also { require(it.length <= max) }

    private fun date(value: String?): String? = clean(value, 10)?.also {
        require(it == LocalDate.parse(it).toString()) { "Invalid date." }
    }
    private fun required(value: String, max: Int = 200): String =
        requireNotNull(clean(value, max)) { "A required field is empty." }

    private fun count(db: SQLiteDatabase, sql: String, vararg args: String): Long =
        db.rawQuery(sql, args.toList().toTypedArray()).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun ownerExists(db: SQLiteDatabase, id: OwnerId, active: Boolean = true) {
        val sql = """SELECT count(*) FROM family_member m
            LEFT JOIN family_profile p ON p.owner_id=m.id
            WHERE m.id=? """ + if (active) " AND coalesce(p.is_active,1)=1" else ""
        require(count(db, sql, id.raw) == 1L) { "Family owner is missing or inactive." }
    }

    private fun masked(value: String?): String? = value?.takeIf { it.isNotBlank() }?.let {
        if (it.length <= 4) "****" else "****" + it.takeLast(4)
    }

    fun read(db: SQLiteDatabase): FamilyAdministrationSnapshot {
        val family = mutableListOf<FamilyProfile>()
        db.rawQuery(
            """SELECT m.id,m.display_name,m.relationship,m.is_primary,
              coalesce(p.is_active,1),p.date_of_birth,p.guardian_id,
              p.color,p.notes,p.pan_masked FROM family_member m
              LEFT JOIN family_profile p ON p.owner_id=m.id ORDER BY m.is_primary DESC,m.display_name""",
            null,
        ).use { c ->
            while (c.moveToNext()) family.add(FamilyProfile(
                OwnerId(c.getString(0)), c.getString(1), c.getString(2),
                c.getInt(3) == 1, c.getInt(4) == 1,
                if (c.isNull(5)) null else c.getString(5),
                if (c.isNull(6)) null else OwnerId(c.getString(6)),
                if (c.isNull(7)) null else c.getString(7),
                if (c.isNull(8)) null else c.getString(8),
                if (c.isNull(9)) null else c.getString(9).let { raw ->
                    if (raw.length == 10) raw.take(5) + "****" + raw.takeLast(1)
                    else "**********"
                },
            ))
        }
        val accounts = mutableListOf<FinancialAccountProfile>()
        db.rawQuery(
            """SELECT a.id,a.owner_id,a.account_type,a.identifier_masked,
              p.custodian,p.dp_id,p.client_id,p.bo_id,p.nominee,p.opened_on,p.closed_on
              FROM custody_account a LEFT JOIN custody_profile p
              ON p.account_id=a.id AND p.owner_id=a.owner_id
              ORDER BY a.owner_id,a.identifier_masked""",
            null,
        ).use { c ->
            fun value(i: Int) = if (c.isNull(i)) null else c.getString(i)
            while (c.moveToNext()) accounts.add(FinancialAccountProfile(
                c.getString(0), OwnerId(c.getString(1)), c.getString(2), c.getString(3),
                value(4), masked(value(5)), masked(value(6)), masked(value(7)),
                value(8), value(9), value(10),
            ))
        }
        val holdings = mutableListOf<Holding>()
        db.rawQuery(
            "SELECT id,owner_id,account_id,display_name,asset_category,quantity_scaled,price_scaled FROM holding ORDER BY owner_id,id",
            null,
        ).use { c ->
            while (c.moveToNext()) holdings.add(Holding(
                HoldingId(c.getString(0)), OwnerId(c.getString(1)),
                if (c.isNull(2)) null else c.getString(2),
                c.getString(3), c.getString(4),
                QuantityScaled(c.getLong(5)), MoneyScaled(c.getLong(6)),
            ))
        }
        val holdingMap = holdings.associateBy { it.id.raw }
        val tags = mutableMapOf<String, MutableList<HoldingId>>()
        db.rawQuery("SELECT goal_id,holding_id,owner_id FROM holding_goal ORDER BY goal_id,holding_id", null).use { c ->
            while (c.moveToNext()) {
                val holding = holdingMap[c.getString(1)]
                check(holding != null && holding.ownerId.raw == c.getString(2)) {
                    "Goal linkage has an invalid holding owner."
                }
                tags.getOrPut(c.getString(0)) { mutableListOf() }.add(holding.id)
            }
        }
        val goals = mutableListOf<FamilyGoalProfile>()
        db.rawQuery(
            "SELECT id,owner_id,name,target_scaled,target_date,priority FROM family_goal ORDER BY created_at,id",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val attached = tags[id]?.toList() ?: emptyList()
                var exposure = MoneyScaled(0)
                for (holdingId in attached) {
                    val h = requireNotNull(holdingMap[holdingId.raw])
                    exposure += marketValue(h.quantity, h.currentUnitPrice)
                }
                goals.add(FamilyGoalProfile(
                    id, if (c.isNull(1)) null else OwnerId(c.getString(1)),
                    c.getString(2), if (c.isNull(3)) null else MoneyScaled(c.getLong(3)),
                    if (c.isNull(4)) null else c.getString(4),
                    if (c.isNull(5)) null else c.getString(5), attached, exposure,
                ))
            }
        }
        val identity = db.rawQuery(
            "SELECT first_name,last_name,email,pan_masked,date_of_birth," +
                "security_q1_id,security_q2_id FROM legacy_account_identity WHERE id=1",null,
        ).use { c ->
            if (!c.moveToFirst()) null else NativeOwnerIdentityPublic(
                c.getString(0) ?: "",c.getString(1) ?: "",c.getString(2) ?: "",
                if (c.isNull(3)) null else c.getString(3).let { pan ->
                    if (pan.length == 10) pan.take(5) + "****" + pan.takeLast(1)
                    else "**********"
                },
                if (c.isNull(4)) null else c.getString(4),
                if (c.isNull(5)) null else c.getString(5),
                if (c.isNull(6)) null else c.getString(6),
            )
        }
        return FamilyAdministrationSnapshot(family, accounts, goals, holdings, identity)
    }

    fun apply(db: SQLiteDatabase, cmd: FamilyAdminChange) {
        when (cmd) {
            is FamilyAdminChange.RegisterOwnerIdentity -> registerIdentity(db,cmd)
            is FamilyAdminChange.Member -> updateMember(db, cmd)
            is FamilyAdminChange.SetMemberActive -> setActive(db, cmd)
            is FamilyAdminChange.Account -> saveAccount(db, cmd)
            is FamilyAdminChange.CloseAccount -> closeAccount(db, cmd)
            is FamilyAdminChange.Goal -> saveGoal(db, cmd)
            is FamilyAdminChange.DeleteGoal -> {
                require(count(db, "SELECT count(*) FROM family_goal WHERE id=?", cmd.id) == 1L)
                db.execSQL("DELETE FROM holding_goal WHERE goal_id=?", arrayOf(cmd.id))
                db.execSQL("DELETE FROM family_goal WHERE id=?", arrayOf(cmd.id))
            }
            is FamilyAdminChange.TagHolding -> tag(db, cmd)
        }
    }

    private val securityQuestionIds = setOf(
        "maiden", "school", "pet", "city", "teacher", "vehicle",
    )

    /** Original auth answer semantics: trim/lowercase/collapse whitespace,
     * PBKDF2-SHA256 210k, independent 16-byte salts. No cleartext answer disk. */
    private fun hashAnswer(input: CharArray): String {
        val normalized = input.concatToString().trim().lowercase(java.util.Locale.ROOT)
            .replace(Regex("\\s+")," ").toCharArray()
        if (normalized.size !in 3..256) {
            normalized.fill('\u0000')
            throw IllegalArgumentException("A nonblank security answer is required.")
        }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val spec = PBEKeySpec(normalized, salt, 210_000, 256)
        return try {
            val value = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
            try {
                Base64.encodeToString(salt,Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(value,Base64.NO_WRAP) + ":210000"
            } finally { value.fill(0) }
        } finally {
            normalized.fill('\u0000')
            spec.clearPassword()
            salt.fill(0)
        }
    }

    private fun registerIdentity(db: SQLiteDatabase, c: FamilyAdminChange.RegisterOwnerIdentity) {
        require(count(db, "SELECT count(*) FROM family_member WHERE is_primary=1") == 1L) {
            "Register the primary Self family member before account identity."
        }
        require(count(db,"SELECT count(*) FROM legacy_account_identity WHERE id=1") == 0L) {
            "An existing account identity must never be silently overwritten."
        }
        val first = required(c.firstName,100)
        val last = required(c.lastName,100)
        val email = required(c.email,254).lowercase(java.util.Locale.ROOT)
        require(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)) {
            "Email is not valid."
        }
        val pan = required(c.pan,10).uppercase(java.util.Locale.ROOT)
        require(panPattern.matches(pan)) { "PAN is invalid." }
        val dob = requireNotNull(date(c.dateOfBirth))
        require(!LocalDate.parse(dob).isAfter(LocalDate.now()))
        require(c.firstQuestionId != c.secondQuestionId &&
            c.firstQuestionId in securityQuestionIds &&
            c.secondQuestionId in securityQuestionIds) {
            "Two distinct registered security questions are required."
        }
        val firstHash = hashAnswer(c.firstAnswer)
        val secondHash = hashAnswer(c.secondAnswer)
        db.execSQL(
            """INSERT INTO legacy_account_identity(
                id,email,first_name,last_name,pan_masked,pan_for_statements,
                date_of_birth,security_q1_id,security_q1_hash,
                security_q2_id,security_q2_hash,created_at,extra_json)
               VALUES(1,?,?,?,?,?,?,?,?,?,?,?,?)""",
            arrayOf(email,first,last,pan.take(5)+"****"+pan.takeLast(1),pan,dob,
                c.firstQuestionId,firstHash,c.secondQuestionId,secondHash,
                Instant.now().toString(),"""{"native_identity":true}"""),
        )
        // The local app password verifier remains Keystore-HMAC bound, not
        // an exportable legacy password hash. Native backups carry the identity
        // only inside the separate authenticated portable ciphertext.
    }

    private fun updateMember(db: SQLiteDatabase, c: FamilyAdminChange.Member) {
        ownerExists(db, c.ownerId, active = false)
        val original = db.rawQuery("SELECT is_primary FROM family_member WHERE id=?",
            arrayOf(c.ownerId.raw)).use { cursor -> check(cursor.moveToFirst()); cursor.getInt(0) == 1 }
        require(c.relation in relationships)
        require(if (original) c.relation == "Self" else c.relation != "Self") {
            "Only the original primary owner can be Self."
        }
        val name = required(c.name, 100)
        require(count(db,
            "SELECT count(*) FROM family_member WHERE id!=? AND display_name=? COLLATE NOCASE",
            c.ownerId.raw, name) == 0L) { "A family member already has that name." }
        val guardian = c.guardianId
        require(guardian != c.ownerId)
        if (guardian != null) ownerExists(db, guardian)
        val birth = date(c.dob)
        if (birth != null) require(!LocalDate.parse(birth).isAfter(LocalDate.now()))
        if (c.relation == "Child" && birth != null &&
            LocalDate.parse(birth).plusYears(18).isAfter(LocalDate.now())) {
            require(guardian != null) { "A minor child requires a registered guardian." }
        }
        val color = clean(c.color, 7)?.also { require(colorPattern.matches(it)) }
        val notes = clean(c.notes, 2000)
        val pan = clean(c.panForStatements, 10)?.uppercase()?.also {
            require(panPattern.matches(it)) { "PAN must match the standard 10-character format." }
        }
        require(!(c.clearPan && pan != null))
        db.execSQL("UPDATE family_member SET display_name=?,relationship=? WHERE id=?",
            arrayOf(name, c.relation, c.ownerId.raw))
        db.execSQL("INSERT OR IGNORE INTO family_profile(owner_id,created_at) VALUES(?,?)",
            arrayOf(c.ownerId.raw, Instant.now().toString()))
        db.execSQL(
            """UPDATE family_profile SET date_of_birth=?,guardian_id=?,color=?,notes=?,
              pan_for_statements=CASE WHEN ?=1 THEN NULL
                WHEN ? IS NOT NULL THEN ? ELSE pan_for_statements END,
              pan_masked=CASE WHEN ?=1 THEN NULL
                WHEN ? IS NOT NULL THEN ? ELSE pan_masked END WHERE owner_id=?""",
            arrayOf(birth,guardian?.raw,color,notes,if(c.clearPan) 1 else 0,pan,pan,
                if(c.clearPan) 1 else 0,pan,pan?.let { it.take(5)+"****"+it.takeLast(1) },c.ownerId.raw),
        )
    }

    private fun setActive(db: SQLiteDatabase, c: FamilyAdminChange.SetMemberActive) {
        ownerExists(db, c.ownerId, active = false)
        if (!c.active) {
            require(count(db, "SELECT count(*) FROM family_member WHERE id=? AND is_primary=1",
                c.ownerId.raw) == 0L) { "Primary owner cannot be deactivated." }
            // Even with no financial holdings, guardian/goal references prevent deactivation.
            val dependent = listOf(
                "SELECT count(*) FROM holding WHERE owner_id=?",
                "SELECT count(*) FROM custody_account WHERE owner_id=?",
                "SELECT count(*) FROM family_goal WHERE owner_id=?",
                "SELECT count(*) FROM family_profile WHERE guardian_id=?",
                "SELECT count(*) FROM snapshot_import WHERE owner_id=?",
                "SELECT count(*) FROM ledger_event WHERE owner_id=?",
            )
            require(dependent.all { count(db,it,c.ownerId.raw) == 0L }) {
                "Member has financial accounts, goals, or dependents; transfer data explicitly first."
            }
        }
        db.execSQL("INSERT OR IGNORE INTO family_profile(owner_id,created_at) VALUES(?,?)",
            arrayOf(c.ownerId.raw,Instant.now().toString()))
        db.execSQL("UPDATE family_profile SET is_active=? WHERE owner_id=?",
            arrayOf(if(c.active) 1 else 0,c.ownerId.raw))
    }

    private fun saveAccount(db: SQLiteDatabase, c: FamilyAdminChange.Account) {
        ownerExists(db,c.ownerId)
        val type = required(c.type,40)
        val nickname = required(c.nickname,100)
        require(count(db,
            """SELECT count(*) FROM custody_account
               WHERE owner_id=? AND identifier_masked=? COLLATE NOCASE AND id!=?""",
            c.ownerId.raw,nickname,c.id ?: "") == 0L) { "Duplicate account nickname for owner." }
        require(!(c.clearNominee && !c.nominee.isNullOrBlank()))
        require(!(c.clearIdentifiers && listOf(c.dpId,c.clientId,c.boId).any { !it.isNullOrBlank() }))
        val id = c.id ?: UUID.randomUUID().toString()
        require(id.length <= 128)
        val existing = count(db,"SELECT count(*) FROM custody_account WHERE id=? AND owner_id=?",
            id,c.ownerId.raw) == 1L
        require(c.id == null || existing) { "Cannot reassign accounts across family owners." }
        if (existing) {
            require(count(db,"SELECT count(*) FROM custody_profile WHERE account_id=? AND closed_on IS NOT NULL",
                id) == 0L) { "Closed account cannot be edited." }
            db.execSQL(
                "UPDATE custody_account SET account_type=?,identifier_masked=? WHERE id=? AND owner_id=?",
                arrayOf(type,nickname,id,c.ownerId.raw))
        } else {
            db.execSQL("INSERT INTO custody_account(id,owner_id,account_type,identifier_masked) VALUES(?,?,?,?)",
                arrayOf(id,c.ownerId.raw,type,nickname))
        }
        db.execSQL("INSERT OR IGNORE INTO custody_profile(account_id,owner_id,base_currency,created_at) VALUES(?,?,?,?)",
            arrayOf(id,c.ownerId.raw,"INR",Instant.now().toString()))
        db.execSQL("""UPDATE custody_profile SET custodian=coalesce(?,custodian),
            dp_id=CASE WHEN ?=1 THEN NULL ELSE coalesce(?,dp_id) END,
            client_id=CASE WHEN ?=1 THEN NULL ELSE coalesce(?,client_id) END,
            bo_id=CASE WHEN ?=1 THEN NULL ELSE coalesce(?,bo_id) END,
            nominee=CASE WHEN ?=1 THEN NULL ELSE coalesce(?,nominee) END,
            opened_on=coalesce(?,opened_on) WHERE account_id=? AND owner_id=?""",
            arrayOf(clean(c.institution,200),
                if(c.clearIdentifiers) 1 else 0,clean(c.dpId,80),
                if(c.clearIdentifiers) 1 else 0,clean(c.clientId,80),
                if(c.clearIdentifiers) 1 else 0,clean(c.boId,80),
                if(c.clearNominee) 1 else 0,clean(c.nominee,300),
                date(c.openedOn),id,c.ownerId.raw))
    }

    private fun closeAccount(db: SQLiteDatabase, c: FamilyAdminChange.CloseAccount) {
        ownerExists(db,c.ownerId)
        require(count(db,"SELECT count(*) FROM custody_account WHERE id=? AND owner_id=?",
            c.id,c.ownerId.raw) == 1L) { "Account ownership mismatch." }
        db.execSQL("INSERT OR IGNORE INTO custody_profile(account_id,owner_id,base_currency,created_at) VALUES(?,?,?,?)",
            arrayOf(c.id,c.ownerId.raw,"INR",Instant.now().toString()))
        db.execSQL("UPDATE custody_profile SET closed_on=coalesce(closed_on,?) WHERE account_id=? AND owner_id=?",
            arrayOf(LocalDate.now().toString(),c.id,c.ownerId.raw))
        // Historical holdings, CAS identity and immutable ledger remain untouched.
    }

    private fun saveGoal(db: SQLiteDatabase, c: FamilyAdminChange.Goal) {
        c.ownerId?.let { ownerExists(db,it) }
        val name = required(c.name,200)
        val target = c.target
        require(target == null || target.raw >= 0)
        val targetDate = date(c.targetDate)
        val id = c.id ?: UUID.randomUUID().toString()
        require(id.length <= 128)
        val present = count(db,"SELECT count(*) FROM family_goal WHERE id=?",id) == 1L
        if (present) {
            require(c.id != null)
            // Prevent a goal becoming owned by someone other than its already-linked holdings.
            val ownerId = c.ownerId
            if (ownerId != null) {
                require(count(db,"SELECT count(*) FROM holding_goal WHERE goal_id=? AND owner_id!=?",
                    id,ownerId.raw) == 0L) {
                    "Goal ownership conflicts with existing linked holdings."
                }
            }
            db.execSQL("UPDATE family_goal SET owner_id=?,name=?,target_scaled=?,target_date=?,priority=? WHERE id=?",
                arrayOf(c.ownerId?.raw,name,c.target?.raw,targetDate,clean(c.priority,50),id))
        } else {
            db.execSQL("""INSERT INTO family_goal(id,owner_id,name,target_scaled,target_date,priority,created_at)
                    VALUES(?,?,?,?,?,?,?)""",
                arrayOf(id,c.ownerId?.raw,name,c.target?.raw,targetDate,
                    clean(c.priority,50),Instant.now().toString()))
        }
    }

    private fun tag(db: SQLiteDatabase, c: FamilyAdminChange.TagHolding) {
        ownerExists(db,c.ownerId)
        require(count(db,"SELECT count(*) FROM holding WHERE id=? AND owner_id=?",
            c.holdingId.raw,c.ownerId.raw) == 1L) { "Holding owner mismatch." }
        require(count(db,"SELECT count(*) FROM family_goal WHERE id=? AND (owner_id IS NULL OR owner_id=?)",
            c.goalId,c.ownerId.raw) == 1L) { "Goal does not belong to this family owner." }
        if (c.attach) {
            db.execSQL("INSERT OR IGNORE INTO holding_goal(goal_id,holding_id,owner_id) VALUES(?,?,?)",
                arrayOf(c.goalId,c.holdingId.raw,c.ownerId.raw))
        } else {
            db.execSQL("DELETE FROM holding_goal WHERE goal_id=? AND holding_id=? AND owner_id=?",
                arrayOf(c.goalId,c.holdingId.raw,c.ownerId.raw))
        }
    }
}
