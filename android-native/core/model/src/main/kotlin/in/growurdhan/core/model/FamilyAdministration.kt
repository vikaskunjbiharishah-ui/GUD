package `in`.growurdhan.core.model

/** Read-only owner-scoped administration projection, backed by encrypted native v4 tables. */
data class FamilyProfile(
    val id: OwnerId,
    val name: String,
    val relation: String,
    val primary: Boolean,
    val active: Boolean,
    val dob: String?,
    val guardianId: OwnerId?,
    val color: String?,
    val notes: String?,
    val panMasked: String?,
)
data class FinancialAccountProfile(
    val id: String,
    val ownerId: OwnerId,
    val type: String,
    val nickname: String,
    val institution: String?,
    val dpMasked: String?,
    val clientMasked: String?,
    val boMasked: String?,
    val nominee: String?,
    val openedOn: String?,
    val closedOn: String?,
)
data class FamilyGoalProfile(
    val id: String,
    val ownerId: OwnerId?,
    val name: String,
    val target: MoneyScaled?,
    val targetDate: String?,
    val priority: String?,
    val holdingIds: List<HoldingId>,
    val taggedExposure: MoneyScaled,
)
data class FamilyAdministrationSnapshot(
    val family: List<FamilyProfile>,
    val accounts: List<FinancialAccountProfile>,
    val goals: List<FamilyGoalProfile>,
    val holdings: List<Holding>,
)

/** Every command is staged before a NEW Android device credential approval. */
sealed interface FamilyAdminChange {
    class Member(
        val ownerId: OwnerId, val name: String, val relation: String,
        val dob: String?, val guardianId: OwnerId?, val color: String?,
        val notes: String?, val panForStatements: String?, val clearPan: Boolean = false,
    ) : FamilyAdminChange
    data class SetMemberActive(val ownerId: OwnerId, val active: Boolean) : FamilyAdminChange
    class Account(
        val id: String?, val ownerId: OwnerId, val type: String, val nickname: String,
        val institution: String?, val dpId: String?, val clientId: String?,
        val boId: String?, val nominee: String?, val openedOn: String?,
        val clearNominee: Boolean = false,
        val clearIdentifiers: Boolean = false,
    ) : FamilyAdminChange
    data class CloseAccount(val id: String, val ownerId: OwnerId) : FamilyAdminChange
    data class Goal(
        val id: String?, val ownerId: OwnerId?, val name: String,
        val target: MoneyScaled?, val targetDate: String?, val priority: String?,
    ) : FamilyAdminChange
    data class DeleteGoal(val id: String) : FamilyAdminChange
    data class TagHolding(
        val goalId: String, val holdingId: HoldingId, val ownerId: OwnerId,
        val attach: Boolean,
    ) : FamilyAdminChange
}
