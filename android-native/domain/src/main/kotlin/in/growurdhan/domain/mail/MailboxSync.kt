package `in`.growurdhan.domain.mail

import `in`.growurdhan.core.model.OwnerId

/**
 * One page from a fake or future Gmail gateway. The token is an opaque
 * cursor, never an email address. Bytes and tokens are not stored here.
 */
data class MailboxPage(
    val attachments: List<RemoteMailAttachment>,
    val nextPageToken: String?,
)

data class RemoteMailAttachment(
    val mailboxId: String,
    val messageId: String,
    val sourceSha256: String,
    val sizeBytes: Long,
    val contentType: String,
    val verifiedDocumentOwner: OwnerId?,
    val exactEnrolledIdentity: Boolean,
)

fun interface MailboxGateway {
    fun list(mailboxId: String, pageToken: String?): MailboxPage
}

data class MailboxSyncOutcome(
    val cursors: Map<String, String?>,
    val staged: List<IntakeDecision.StageForHumanReview>,
    val quarantined: List<IntakeDecision.Quarantine>,
)

/**
 * Pulls attachment metadata for every connected mailbox and stages or
 * quarantines each one. It does not download bytes, call Gmail, or write
 * a ledger event. A saved cursor is the next unread page, or null when
 * that mailbox was fully read.
 */
object MailboxSync {
    const val MAX_PAGES_PER_MAILBOX = 5

    fun pull(
        mailboxes: List<MailboxIdentity>,
        links: List<MemberMailboxLink>,
        enrolledOwners: Set<OwnerId>,
        observedSources: Set<String>,
        cursors: Map<String, String?>,
        gateway: MailboxGateway,
    ): MailboxSyncOutcome {
        val seen = observedSources.toMutableSet()
        val staged = mutableListOf<IntakeDecision.StageForHumanReview>()
        val quarantined = mutableListOf<IntakeDecision.Quarantine>()
        val nextCursors = LinkedHashMap<String, String?>()
        for (box in mailboxes) {
            var token = cursors[box.id]
            var pages = 0
            while (pages < MAX_PAGES_PER_MAILBOX) {
                val page = gateway.list(box.id, token)
                pages += 1
                for (item in page.attachments) {
                    require(item.mailboxId == box.id) { "A page mixed two mailboxes." }
                    require(item.messageId.isNotBlank() && item.messageId.length <= 128)
                    val decision = MailAttachmentIntakePolicy.decide(
                        CandidateAttachment(
                            mailboxId = item.mailboxId,
                            sourceSha256 = item.sourceSha256,
                            sizeBytes = item.sizeBytes,
                            contentType = item.contentType,
                            verifiedDocumentOwner = item.verifiedDocumentOwner,
                            exactEnrolledIdentity = item.exactEnrolledIdentity,
                        ),
                        mailboxes,
                        links,
                        enrolledOwners,
                        seen,
                    )
                    when (decision) {
                        is IntakeDecision.StageForHumanReview -> {
                            staged += decision
                            seen += decision.sha256
                        }
                        is IntakeDecision.Quarantine -> quarantined += decision
                    }
                }
                token = page.nextPageToken
                if (token == null) break
            }
            nextCursors[box.id] = token
        }
        return MailboxSyncOutcome(nextCursors, staged.toList(), quarantined.toList())
    }
}

/** Names and the monthly period. The Android wrapper enqueues these names. */
object MailboxSyncSchedule {
    const val MONTHLY_DAYS = 30L
    const val MONTHLY_WORK = "gud.mailbox.monthly"
    const val MANUAL_WORK = "gud.mailbox.manual"

    fun monthlyIntervalMillis(): Long = MONTHLY_DAYS * 24L * 60L * 60L * 1000L
}
