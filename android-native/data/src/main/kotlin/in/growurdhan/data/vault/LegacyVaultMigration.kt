package `in`.growurdhan.data.vault

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject
import `in`.growurdhan.domain.vault.LegacyRestoreIdentity

/**
 * M25: Authenticate an original GUDVLT01 backup AND independently verify the
 * embedded owner's PBKDF2 verifier, PAN, DOB and two selected security answers.
 *
 * No password reset, no native login bypass, no plaintext disk staging,
 * no automatic merge, no phantom BUY and no partial SQLite write. The returned
 * bytes are a *transient plaintext conversion document* used ONLY by the
 * native transaction in SqlCipherVaultRepository and wiped immediately.
 */
object LegacyVaultMigration {
    const val MAX_INPUT_BYTES = 32 * 1024 * 1024
    private val magic = "GUDVLT01".toByteArray(Charsets.US_ASCII)
    private const val HEADER_SIZE = 40
    private const val ITERATIONS = 600_000
    private const val ACCOUNT_ITERATIONS = 210_000
    private val idPattern = Regex("^[a-zA-Z0-9._:\\-]{1,128}$")
    private val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val categories = setOf(
        "EQUITY", "MUTUAL_FUND", "GOLD_SGB", "FIXED_INCOME",
        "REAL_ESTATE", "SPECIALISED", "CASH_BANK", "LIABILITY",
    )
    private val eventKinds = setOf(
        "OPENING_BALANCE", "SNAPSHOT_ADJUSTMENT", "BUY", "SELL",
        "BONUS", "SPLIT", "DIVIDEND", "INTEREST", "VALUATION_UPDATE",
        "REPAYMENT", "CORRECTION",
    )

    private fun requireId(value: String): String {
        require(idPattern.matches(value)) { "Original record ID is invalid." }
        return value
    }

    private fun JSONObject.requiredString(key: String): String {
        require(!isNull(key) && has(key)) { "Missing required legacy field." }
        return getString(key).also { require(it.isNotBlank() && it.length <= 1_048_576) }
    }

    private fun JSONObject.optionalString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key).also { require(it.length <= 1_048_576) }
        else null

    private fun JSONObject.integerString(key: String, allowNegative: Boolean = true): String? {
        val s = optionalString(key) ?: return null
        require(Regex("^-?(0|[1-9][0-9]*)$").matches(s) && s.length <= 21) {
            "Invalid scaled financial integer."
        }
        val value = s.toLong()
        require(allowNegative || value >= 0) { "Negative unsigned financial value." }
        return value.toString()
    }

    private fun JSONObject.objectJson(key: String): String? =
        if (!has(key) || isNull(key)) null else {
            val value = get(key)
            require(value is JSONObject || value is JSONArray) { "Expected legacy JSON object or array." }
            value.toString()
        }

    private fun decode64(value: String, expected: Int): ByteArray {
        val decoded = Base64.decode(value, Base64.DEFAULT)
        require(decoded.size == expected) { "Invalid stored recovery verifier." }
        return decoded
    }

    private fun matchesPbkdf(
        chars: CharArray, saltB64: String, hashB64: String, iterations: Int,
    ): Boolean {
        require(iterations == ACCOUNT_ITERATIONS) { "Unsupported legacy credential policy." }
        val salt = decode64(saltB64, 16)
        val expected = decode64(hashB64, 32)
        val spec = PBEKeySpec(chars, salt, iterations, 256)
        return try {
            val got = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
            try { MessageDigest.isEqual(got, expected) } finally { got.fill(0) }
        } finally {
            spec.clearPassword()
            salt.fill(0)
            expected.fill(0)
        }
    }

    private fun matchesPackedAnswer(input: CharArray, packed: String): Boolean {
        val parts = packed.split(':')
        require(parts.size == 3) { "Invalid legacy answer verifier." }
        val normalized = input.concatToString().trim().lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ").toCharArray()
        return try {
            matchesPbkdf(normalized, parts[0], parts[1], parts[2].toInt())
        } finally {
            normalized.fill('\u0000')
        }
    }

    private fun verifyIdentity(root: JSONObject, identity: LegacyRestoreIdentity) {
        val account = root.getJSONObject("account")
        val normalizedEmail = identity.email.trim().lowercase(Locale.ROOT)
        require(normalizedEmail.length in 5..254 &&
            account.requiredString("email").trim().lowercase(Locale.ROOT) == normalizedEmail
        ) { "Original backup account mismatch." }
        require(matchesPbkdf(
            identity.password, account.requiredString("passwordSalt"),
            account.requiredString("passwordHash"),
            account.getInt("passwordIterations"),
        )) { "Original backup credential mismatch." }
        val inputPan = identity.pan.trim().uppercase(Locale.ROOT)
        require(Regex("^[A-Z]{5}[0-9]{4}[A-Z]$").matches(inputPan)) {
            "Invalid PAN for source backup recovery."
        }
        val fullPan = account.optionalString("panForStatements")?.uppercase(Locale.ROOT)
        val maskedPan = account.optionalString("panMasked")?.uppercase(Locale.ROOT)
        require(when {
            fullPan != null -> MessageDigest.isEqual(
                fullPan.toByteArray(Charsets.US_ASCII), inputPan.toByteArray(Charsets.US_ASCII))
            maskedPan != null -> maskPan(inputPan) == maskedPan
            else -> false
        }) { "Original account identity could not be verified." }
        require(datePattern.matches(identity.dateOfBirth) &&
            identity.dateOfBirth == account.optionalString("dateOfBirth")
        ) { "Original account date of birth mismatch." }
        require(identity.firstQuestionId != identity.secondQuestionId &&
            identity.firstQuestionId == account.requiredString("q1Id") &&
            identity.secondQuestionId == account.requiredString("q2Id")
        ) { "Security question identity mismatch." }
        require(matchesPackedAnswer(identity.firstAnswer, account.requiredString("q1Hash")) &&
            matchesPackedAnswer(identity.secondAnswer, account.requiredString("q2Hash"))
        ) { "Security answers do not match the original account." }
    }

    private fun maskPan(pan: String): String = pan.take(5) + "****" + pan.takeLast(1)

    /**
     * Original exports have two mutually incompatible authenticated envelopes:
     *   GUDVLT01: 600k-round PBKDF2(email/password), nonce and binary GCM tag
     *   KOSHA1: JSON of saltB64 / ivB64 / cipherB64, 600k-round PBKDF2(password)
     * Both MUST carry the original account's 210k PBKDF verifier AND identity
     * challenge. Plaintext JSON export is deliberately not restorable.
     */
    private fun decryptOriginal(data: ByteArray, email: String, password: CharArray): ByteArray {
        require(data.size in HEADER_SIZE + 16..MAX_INPUT_BYTES) { "Unsupported archive length." }
        require(password.size in 8..1024) { "Original backup password is required." }
        val isBinary = data.copyOfRange(0, 8).contentEquals(magic)
        val salt: ByteArray
        val nonce: ByteArray
        val ciphertext: ByteArray
        val secret: CharArray
        val rounds: Int
        if (isBinary) {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            buffer.position(8)
            require(buffer.int == ITERATIONS) { "Unsupported KDF work factor." }
            salt = ByteArray(16).also(buffer::get)
            nonce = ByteArray(12).also(buffer::get)
            ciphertext = data.copyOfRange(HEADER_SIZE, data.size)
            val normalized = email.trim().lowercase(Locale.ROOT)
            require(normalized.length in 5..254 &&
                Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(normalized))
            secret = ("GrowUrDhan\n" + normalized + "\n" + password.concatToString()).toCharArray()
            rounds = ITERATIONS
        } else {
            require(data[0] == '{'.code.toByte()) {
                "Not a supported encrypted original backup."
            }
            val wrapper = JSONObject(data.toString(Charsets.UTF_8))
            require(wrapper.optString("magic") == "KOSHA1") {
                "Plaintext or unrecognized backup is not permitted."
            }
            require(wrapper.keys().asSequence().all { it in setOf(
                "magic", "saltB64", "ivB64", "cipherB64", "iterations",
            ) }) { "Unexpected KOSHA1 envelope fields." }
            val sourceRounds = wrapper.optInt("iterations", ITERATIONS)
            require(sourceRounds == ITERATIONS || sourceRounds == 210_000) {
                "Unsupported legacy KDF work factor."
            }
            rounds = sourceRounds
            salt = Base64.decode(wrapper.getString("saltB64"), Base64.DEFAULT)
            nonce = Base64.decode(wrapper.getString("ivB64"), Base64.DEFAULT)
            ciphertext = Base64.decode(wrapper.getString("cipherB64"), Base64.DEFAULT)
            require(ciphertext.size in 16..MAX_INPUT_BYTES)
            secret = password.copyOf()
        }
        require(salt.size == 16 && nonce.size == 12) {
            "Invalid legacy KDF salt or GCM nonce."
        }
        val spec = PBEKeySpec(secret, salt, rounds, 256)
        secret.fill('\u0000')
        val derived = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally { spec.clearPassword(); salt.fill(0) }
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(derived, "AES"),
                    GCMParameterSpec(128, nonce))
                doFinal(ciphertext)
            }.also { require(it.size <= MAX_INPUT_BYTES) }
        } finally {
            derived.fill(0)
            nonce.fill(0)
            ciphertext.fill(0)
        }
    }

    /** Inspect legacy KOSHA1 ciphertext without committing anything.
     *  Original account, full PAN and names are never exposed by the preview. */
    fun inspectLegacyJson(data: ByteArray, email: String, password: CharArray): LegacyBackupPreview {
        val decrypted = decryptOriginal(data, email, password)
        try {
            val root = normalizedBackup(
                JSONObject(decrypted.toString(Charsets.UTF_8)), historical = true)
            require(root.getJSONObject("account").getString("email")
                .trim().lowercase(Locale.ROOT) == email.trim().lowercase(Locale.ROOT))
            fun count(key: String) = root.getJSONArray(key).length().also {
                require(it <= 100_000)
            }
            val holdings = root.getJSONArray("holdings")
            val history = !root.isNull("casLatest")
            val extended = (0 until holdings.length()).any {
                holdings.getJSONObject(it).has("details")
            }
            val fingerprint = MessageDigest.getInstance("SHA-256").digest(data)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            return LegacyBackupPreview(count("familyMembers"), count("holdings"),
                count("ledgerEvents"), count("accounts"), count("goals"),
                history, extended, fingerprint, losslessImportAvailable = true)
        } finally { decrypted.fill(0) }
    }

    private fun normalizedBackup(root: JSONObject, historical: Boolean): JSONObject {
        if (historical) {
            require(!root.has("kind") || root.optString("kind") == "GUDVAULT")
            require(!root.has("version") || root.optInt("version") in 1..2)
            root.put("kind", "GUDVAULT").put("version", 2)
            for (name in listOf("familyMembers", "holdings", "ledgerEvents", "accounts", "goals")) {
                if (!root.has(name)) root.put(name, JSONArray())
            }
            if (!root.has("casLatest")) root.put("casLatest", JSONObject.NULL)
        }
        require(root.optString("kind") == "GUDVAULT" && root.optInt("version") == 2)
        return root
    }

    fun convertAuthenticated(data: ByteArray, identity: LegacyRestoreIdentity): ByteArray {
        val historical = data.isNotEmpty() && data[0] == '{'.code.toByte()
        val decrypted = decryptOriginal(data, identity.email, identity.password)
        try {
            val source = JSONObject(decrypted.toString(Charsets.UTF_8))
            val originalVersion = source.optInt("version", if (historical) 1 else 2)
            val root = normalizedBackup(source, historical)
            verifyIdentity(root, identity)
            val sourceHash = MessageDigest.getInstance("SHA-256").digest(data)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            val converted = convert(root, sourceHash,
                if (historical) "KOSHA1" else "GUDVLT01", originalVersion)
            return converted.toString().toByteArray(Charsets.UTF_8).also {
                require(it.size <= VaultBackupCodec.MAX_PLAINTEXT) {
                    "Restorable original archive is too large."
                }
            }
        } finally { decrypted.fill(0) }
    }

    /** Exact source object JSON is also retained in extra_json; mapped fields are
     * indexed for native use. This is a structural, fail-closed conversion. */
    private fun convert(
        source: JSONObject, sha256: String, sourceFormat: String, sourceVersion: Int,
    ): JSONObject {
        val dest = VaultBackupTables.emptyLegacyConversion()
        fun put(table: String, fields: Map<String, Any?>) =
            VaultBackupTables.appendConvertedLegacyRow(dest, table, fields)
        fun arr(key: String): JSONArray {
            val rows = source.getJSONArray(key)
            require(rows.length() <= 100_000) { "Too many legacy records." }
            return rows
        }
        fun rows(key: String): List<JSONObject> {
            val a = arr(key)
            return (0 until a.length()).map { a.getJSONObject(it) }
        }
        val members = rows("familyMembers")
        val holdings = rows("holdings")
        val events = rows("ledgerEvents")
        val accounts = rows("accounts")
        val goals = rows("goals")
        require(members.isNotEmpty() && members.count {
            it.optBoolean("isPrimary") || it.optString("relation") == "Self"
        } == 1) { "Original vault must identify exactly one primary owner." }
        val owners = members.map { requireId(it.requiredString("id")) }.toSet()
        require(owners.size == members.size) { "Duplicate original owners." }
        members.forEach { m ->
            val id = requireId(m.requiredString("id"))
            val relation = m.requiredString("relation")
            val primary = m.optBoolean("isPrimary") || relation == "Self"
            put("family_member", mapOf(
                "id" to id, "display_name" to m.requiredString("name"),
                "relationship" to relation, "is_primary" to if (primary) 1 else 0,
            ))
            put("family_profile", mapOf(
                "owner_id" to id, "entity_type" to if (relation == "HUF") "HUF" else "INDIVIDUAL",
                "pan_masked" to m.optionalString("panMasked"),
                "pan_for_statements" to m.optionalString("panForStatements"),
                "date_of_birth" to m.optionalString("dateOfBirth"),
                "color" to m.optionalString("color"), "notes" to m.optionalString("notes"),
                "is_active" to 1, "cas_id" to m.optionalString("casId"),
                "pran" to m.optionalString("pran"),
                "gmail_sync" to if (m.optBoolean("gmailSync")) 1 else 0,
                "cas_id_source" to m.optionalString("casIdSource"),
                "pran_source" to m.optionalString("pranSource"),
                "cas_sync_after" to m.optionalString("casSyncAfter"),
                "pran_sync_after" to m.optionalString("pranSyncAfter"),
                "extra_json" to m.toString(),
            ))
        }
        val accountOwners = mutableMapOf<String, String>()
        accounts.forEach { a ->
            val id = requireId(a.requiredString("id"))
            val owner = requireId(a.requiredString("ownerId"))
            require(owner in owners && accountOwners.put(id, owner) == null) {
                "Duplicate or cross-owner original custody account."
            }
            put("custody_account", mapOf("id" to id, "owner_id" to owner,
                "account_type" to a.requiredString("accountType"),
                "identifier_masked" to (a.optionalString("identifierMasked") ?: ""),
            ))
            put("custody_profile", mapOf(
                "account_id" to id, "owner_id" to owner,
                "custodian" to a.optionalString("custodian"),
                // Conversion uses an explicit column list, so a bound NULL would
                // bypass the SQL DEFAULT and violate the NOT NULL constraint.
                "base_currency" to (a.optionalString("baseCurrency") ?: "INR"),
                "dp_id" to a.optionalString("dpId"), "client_id" to a.optionalString("clientId"),
                "bo_id" to a.optionalString("boId"), "nominee" to a.optionalString("nominee"),
                "extra_json" to a.toString(),
            ))
        }
        val holdingOwners = mutableMapOf<String, String>()
        holdings.forEach { h ->
            val id = requireId(h.requiredString("id"))
            val owner = requireId(h.requiredString("ownerId"))
            require(owner in owners && holdingOwners.put(id, owner) == null) {
                "Duplicate or cross-owner legacy holding."
            }
            val accountId = h.optionalString("accountId")?.takeIf { it.isNotBlank() }
            require(accountId == null || accountOwners[accountId] == owner) {
                "Holding custody account belongs to another member."
            }
            val category = h.requiredString("category")
            require(category in categories) { "Unsupported old asset category." }
            val quantity = h.integerString("quantityScaled", false)
                ?: error("Missing original holding quantity.")
            val price = h.integerString("currentPriceMinor", false)
                ?: error("Missing original holding price.")
            put("holding", mapOf(
                "id" to id, "owner_id" to owner, "account_id" to accountId,
                "display_name" to h.requiredString("name"),
                "asset_category" to category, "quantity_scaled" to quantity, "price_scaled" to price,
            ))
            put("holding_profile", mapOf(
                "holding_id" to id, "owner_id" to owner,
                "symbol_or_folio" to h.optionalString("symbolOrFolio"),
                "isin" to h.optionalString("isin"), "sub_category" to h.optionalString("subCategory"),
                "average_cost_per_unit_scaled" to h.integerString("averageCostPerUnitMinor"),
                "cost_unknown" to if (h.integerString("averageCostPerUnitMinor") == null ||
                    h.integerString("averageCostPerUnitMinor") == "0") 1 else 0,
                "benchmark_price_scaled" to h.integerString("benchmarkPriceMinor"),
                "issue_price_scaled" to h.integerString("issuePriceMinor"),
                "next_coupon_date" to h.optionalString("nextCouponDate"),
                "maturity_date" to h.optionalString("maturityDate"),
                "institution_name" to h.optionalString("institutionName"),
                "account_number_masked" to h.optionalString("accountNumberMasked"),
                "monthly_emi_scaled" to h.integerString("monthlyEmiMinor"),
                "emi_status" to h.optionalString("emiStatus"),
                "remaining_emi_count" to h.integerString("remainingEmiCount", false),
                "acquired_on" to h.optionalString("acquiredOn"),
                "details_json" to h.objectJson("details"),
                "last_price_updated" to h.optionalString("lastPriceUpdated"),
                "is_price_stale" to if (h.optBoolean("isPriceStale")) 1 else 0,
                "price_source" to h.optionalString("priceSource"),
                "needs_review" to if (h.optBoolean("needsReview")) 1 else 0,
                "review_reasons_json" to h.objectJson("reviewReasons"),
                "fmv_31jan2018_scaled" to h.integerString("fmv31Jan2018Minor"),
                "exposure_node" to h.optionalString("exposureNode"),
                "l2_node" to h.optionalString("l2Node"),
                "wrapper" to h.optionalString("wrapper"),
                "allocation_split_json" to h.objectJson("allocationSplit"),
                "notes" to h.optionalString("notes"), "location" to h.optionalString("location"),
                "tag" to h.optionalString("tag"), "tags_json" to h.objectJson("tags"),
                "extra_json" to h.toString(),
            ))
        }
        // Preserve supersession topologically and exact event IDs. Never manufacture
        // a transaction from statement positions or a present holding quantity.
        val byId = mutableMapOf<String, JSONObject>()
        events.forEach { e ->
            val id = requireId(e.requiredString("id"))
            require(byId.put(id, e) == null) { "Duplicate legacy event identifier." }
        }
        val done = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        val ordered = mutableListOf<JSONObject>()
        fun walk(id: String) {
            if (id in done) return
            require(visiting.add(id)) { "Cyclic legacy correction chain." }
            val e = byId.getValue(id)
            val previous = e.optionalString("supersedesEventId")
            if (previous != null) {
                require(previous != id && previous in byId) { "Missing correction predecessor." }
                walk(previous)
            }
            visiting.remove(id)
            done.add(id)
            ordered.add(e)
        }
        events.forEach { walk(it.requiredString("id")) }
        ordered.forEach { e ->
            val owner = requireId(e.requiredString("ownerId"))
            val holding = requireId(e.requiredString("holdingId"))
            require(holdingOwners[holding] == owner) { "Legacy event holding ownership mismatch." }
            val kind = e.requiredString("eventType")
            require(kind in eventKinds) { "Unsupported ledger event type." }
            val old = e.optionalString("supersedesEventId")
            require((kind == "CORRECTION") == (old != null)) {
                "Inconsistent correction reference."
            }
            if (old != null) {
                val predecessor = byId.getValue(old)
                require(predecessor.requiredString("ownerId") == owner &&
                    predecessor.requiredString("holdingId") == holding) {
                    "Cross-member correction chain."
                }
            }
            val date = e.requiredString("eventDate")
            require(datePattern.matches(date)) { "Unsupported ledger date." }
            val carrier = if (kind in setOf(
                "BONUS","SPLIT","SNAPSHOT_ADJUSTMENT","VALUATION_UPDATE","REPAYMENT"
            )) "ADJUSTMENT" else kind
            put("ledger_event", mapOf(
                "id" to e.requiredString("id"), "owner_id" to owner,
                "holding_id" to holding, "kind" to carrier, "financial_kind" to kind,
                "event_date" to date,
                "quantity_delta_scaled" to e.integerString("quantityScaled"),
                "amount_scaled" to e.integerString("totalAmountMinor"),
                "unit_price_scaled" to e.integerString("unitPriceMinor"),
                "charges_scaled" to (e.integerString("chargesMinor", false) ?: "0"),
                "supersedes_event_id" to old,
            ))
            put("ledger_profile", mapOf(
                "event_id" to e.requiredString("id"), "owner_id" to owner,
                "holding_id" to holding,
                "holding_name" to e.optionalString("holdingName"),
                "import_batch_id" to e.optionalString("importBatchId"),
                "source_ref" to e.optionalString("sourceRef"),
                "notes" to e.optionalString("notes"),
                "legacy_created_at" to e.optionalString("createdAt"),
                "extra_json" to e.toString(),
            ))
        }
        val goalIds = mutableSetOf<String>()
        goals.forEach { g ->
            val id = requireId(g.requiredString("id"))
            require(goalIds.add(id)) { "Duplicate legacy goal identifier." }
            val owner = g.optionalString("ownerId")
            require(owner == null || owner in owners) { "Goal has missing owner." }
            put("family_goal", mapOf(
                "id" to id, "owner_id" to owner,
                "name" to g.requiredString("name"),
                "target_scaled" to g.integerString("targetMinor", false),
                "target_date" to g.optionalString("targetDate"),
                "priority" to g.optionalString("priority"),
                "created_at" to g.requiredString("createdAt"),
                "extra_json" to g.toString(),
            ))
        }
        val account = source.getJSONObject("account")
        put("legacy_account_identity", mapOf(
            "id" to 1, "email" to account.requiredString("email"),
            "first_name" to account.requiredString("firstName"),
            "last_name" to account.requiredString("lastName"),
            "pan_masked" to account.optionalString("panMasked"),
            "pan_for_statements" to account.optionalString("panForStatements"),
            "date_of_birth" to account.optionalString("dateOfBirth"),
            "password_salt" to account.requiredString("passwordSalt"),
            "password_hash" to account.requiredString("passwordHash"),
            "password_iterations" to account.getInt("passwordIterations"),
            "password_changed_at" to account.requiredString("passwordChangedAt"),
            "security_q1_id" to account.requiredString("q1Id"),
            "security_q1_hash" to account.requiredString("q1Hash"),
            "security_q2_id" to account.requiredString("q2Id"),
            "security_q2_hash" to account.requiredString("q2Hash"),
            "created_at" to account.requiredString("createdAt"),
            "extra_json" to account.toString(),
        ))
        val cas = if (!source.has("casLatest") || source.isNull("casLatest")) "null"
            else source.get("casLatest").toString()
        put("vault_meta", mapOf("key" to "original_source_format",
            "value" to sourceFormat))
        put("vault_meta", mapOf("key" to "original_source_version",
            "value" to sourceVersion.toString()))
        put("vault_meta", mapOf("key" to "original_backup_sha256", "value" to sha256))
        put("vault_meta", mapOf("key" to "original_exported_at",
            "value" to (source.optionalString("exportedAt") ?: "")))
        put("vault_meta", mapOf("key" to "original_cas_latest", "value" to cas))
        // Never silently drop an unrecognized root record family.
        val expectedRoot = setOf(
            "kind","version","exportedAt","account","familyMembers",
            "holdings","ledgerEvents","accounts","goals","casLatest",
        )
        val unhandled = source.keys().asSequence().filter { it !in expectedRoot }.toList()
        require(unhandled.isEmpty()) { "Unknown legacy backup collection: cannot guarantee lossless migration." }
        return dest
    }
}
