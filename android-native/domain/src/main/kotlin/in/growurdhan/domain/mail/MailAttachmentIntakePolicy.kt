package `in`.growurdhan.domain.mail

import `in`.growurdhan.core.model.OwnerId

/** Metadata only. OAuth tokens and actual message attachments never go in these objects. */
data class MailboxIdentity(val id: String, val displayLabel: String) {
    init {
        require(Regex("[a-zA-Z0-9_-]{1,80}").matches(id))
        require(displayLabel.isNotBlank() && displayLabel.length <= 120)
    }
}

/** A shared family inbox can be linked to many members, and vice versa. */
data class MemberMailboxLink(val mailboxId: String, val ownerId: OwnerId)

data class CandidateAttachment(
    val mailboxId: String,
    val sourceSha256: String,
    val sizeBytes: Long,
    val contentType: String,
    /** Verified by the independently validated on-device statement parser. */
    val verifiedDocumentOwner: OwnerId?,
    /** True only after exact enrolled financial identity matching (folio/PRAN/etc.). */
    val exactEnrolledIdentity: Boolean,
)

enum class IntakeRejection {
    UNKNOWN_MAILBOX, MALFORMED_FINGERPRINT, TOO_LARGE, UNSUPPORTED_TYPE,
    ALREADY_SEEN, AMBIGUOUS_OWNER, UNLINKED_OWNER, UNVERIFIED_ACCOUNT,
}

sealed interface IntakeDecision {
    data class StageForHumanReview(
        val ownerId: OwnerId, val sha256: String,
    ) : IntakeDecision
    data class Quarantine(val reason: IntakeRejection) : IntakeDecision
}

/**
 * Preflight for an eventual on-device Gmail discovery worker. Does NOT
 * connect to Gmail, parse a PDF, fetch bytes, or append a financial event.
 * Sender, recipient, filename and mailbox address are never accepted as
 * financial identity evidence.
 */
object MailAttachmentIntakePolicy {
    const val MAX_ATTACHMENT_BYTES = 15 * 1024 * 1024L
    const val MAX_PAGE_SIZE = 100

    fun decide(
        attachment: CandidateAttachment,
        mailboxes: List<MailboxIdentity>,
        links: List<MemberMailboxLink>,
        enrolledOwners: Set<OwnerId>,
        observedSources: Set<String>,
    ): IntakeDecision {
        if (mailboxes.none { it.id == attachment.mailboxId }) {
            return IntakeDecision.Quarantine(IntakeRejection.UNKNOWN_MAILBOX)
        }
        if (!Regex("[0-9a-f]{64}").matches(attachment.sourceSha256)) {
            return IntakeDecision.Quarantine(IntakeRejection.MALFORMED_FINGERPRINT)
        }
        if (attachment.sizeBytes <= 0 || attachment.sizeBytes > MAX_ATTACHMENT_BYTES) {
            return IntakeDecision.Quarantine(IntakeRejection.TOO_LARGE)
        }
        if (attachment.contentType.lowercase() !in setOf(
                "application/pdf", "application/octet-stream",
            )
        ) {
            return IntakeDecision.Quarantine(IntakeRejection.UNSUPPORTED_TYPE)
        }
        if (attachment.sourceSha256 in observedSources) {
            return IntakeDecision.Quarantine(IntakeRejection.ALREADY_SEEN)
        }
        val verified = attachment.verifiedDocumentOwner
            ?: return IntakeDecision.Quarantine(IntakeRejection.AMBIGUOUS_OWNER)
        if (verified !in enrolledOwners || links.none {
                it.mailboxId == attachment.mailboxId && it.ownerId == verified
            }
        ) {
            return IntakeDecision.Quarantine(IntakeRejection.UNLINKED_OWNER)
        }
        if (!attachment.exactEnrolledIdentity) {
            return IntakeDecision.Quarantine(IntakeRejection.UNVERIFIED_ACCOUNT)
        }
        return IntakeDecision.StageForHumanReview(verified, attachment.sourceSha256)
    }

    /** Data processing caps for a future OAuth-backed pager. */
    fun validatePage(pageSize: Int, attachmentBytes: Long) {
        require(pageSize in 1..MAX_PAGE_SIZE) { "Exceeded Gmail pagination cap." }
        require(attachmentBytes in 1..MAX_ATTACHMENT_BYTES) {
            "Exceeded Gmail attachment size cap."
        }
    }
}
