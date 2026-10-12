package `in`.growurdhan.domain.vault

import `in`.growurdhan.core.model.*
import `in`.growurdhan.domain.finance.ReviewedStatement
import `in`.growurdhan.domain.finance.StatementLine
import `in`.growurdhan.domain.finance.FinancialEvent
import `in`.growurdhan.domain.finance.ManualTradeDraft
import `in`.growurdhan.domain.finance.ManualTradePlan

/**
 * Financial mutations are serialized and committed as SQLite transactions.
 * The device-bound key is not the vault's future user login password.
 */
interface VaultRepository {
    /** Does not create an empty vault. */
    suspend fun status(): VaultStatus
    /** Explicit operation; first call creates a private encrypted vault. */
    suspend fun openOrCreate(): VaultStatus
    suspend fun close()
    suspend fun createMember(member: FamilyMember)
    /** Onboard a member atomically; the first family member becomes the primary owner. */
    suspend fun registerFamilyMember(displayName: String, relationship: String): FamilyMember
    suspend fun createAccount(account: CustodyAccount)
    /** Create a labelled registrar account after authenticated user approval. */
    suspend fun registerCustodyAccount(
        ownerId: OwnerId, accountType: String, label: String,
    ): CustodyAccount
    suspend fun createHolding(holding: Holding)
    /** User-approved, zero-unit asset without implied purchase or CAS identity. */
    suspend fun registerManualAsset(ownerId: OwnerId, name: String, category: String): Holding
    /** Atomically enroll canonical category and subtype, with zero quantity/cost. */
    suspend fun registerDetailedAsset(
        ownerId: OwnerId, name: String, category: String, subCategory: String,
        accountId: String? = null,
    ): Holding
    /** Atomically add a zero-unit holding and its exact CAS ISIN/folio identity. */
    suspend fun registerCasHolding(
        ownerId: OwnerId, accountId: String, displayName: String,
        isin: String, folio: String,
    ): Holding
    suspend fun appendEvent(event: LedgerEvent)
    /** Validated rich events are appended as one all-or-nothing financial transaction. */
    suspend fun appendFinancialEvents(events: List<FinancialEvent>)
    /** Revalidate reviewed trade under the same SQLCipher write transaction. */
    suspend fun commitManualTrade(draft: ManualTradeDraft, reviewed: ManualTradePlan)
    /** Refuse an incomplete statement before performing any database writes. */
    suspend fun importSnapshot(snapshot: StatementSnapshot): SnapshotResult
    /** A locked/read-consistent review receipt. Not an automatic financial import. */
    suspend fun prepareStatementReview(
        snapshotId: String,
        ownerId: OwnerId,
        sourceSha256: String,
        lines: List<StatementLine>,
    ): ReviewedStatement
    /** Explicit caller action: re-validate review and commit observation/events atomically. */
    suspend fun commitReviewedStatement(review: ReviewedStatement): SnapshotResult
    /** Encrypted owner/account/goal projection; no PAN plaintext leaves repository. */
    suspend fun familyAdministration(): FamilyAdministrationSnapshot
    /** Single atomic write protected by the device's newest credential approval. */
    suspend fun applyFamilyAdministration(change: FamilyAdminChange)
    suspend fun members(): List<FamilyMember>
    suspend fun accounts(): List<CustodyAccount>
    suspend fun casIdentities(): List<CasHoldingIdentity>
    suspend fun holdings(): List<Holding>
    suspend fun events(): List<LedgerEvent>
    /** Encrypted extended holding details; never infer an acquisition lot. */
    suspend fun holdingProfiles(): List<HoldingProfileSnapshot>
    /** OS-approved atomic owner-preserving profile and custom-price edit. */
    suspend fun updateHoldingProfile(edit: HoldingProfileEdit)
    /** Consistent members/holdings/ledger captured under ONE SQL read transaction. */
    suspend fun readBook(): VaultBook
    suspend fun health(): VaultHealth
    /** A password-encrypted, portable backup; no user data is logged or written unencrypted to disk. */
    suspend fun exportEncryptedBackup(passphrase: CharArray): ByteArray
    /** Restores ONLY into a new, empty vault; database transaction rolls back on any error. */
    suspend fun restoreEncryptedBackup(passphrase: CharArray, ciphertext: ByteArray)
    /** M25: strictly verified old GUDVLT01 backup, empty-vault only; not a native login reset. */
    suspend fun restoreOriginalLegacyBackup(identity: LegacyRestoreIdentity, archive: ByteArray)

}
