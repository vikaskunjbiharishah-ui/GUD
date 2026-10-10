package `in`.growurdhan.domain.mail

import `in`.growurdhan.core.model.OwnerId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MailAttachmentIntakePolicyTest {
    private val first = OwnerId("primary")
    private val second = OwnerId("relative")
    private val firstMailbox = MailboxIdentity("shared_mailbox", "Family inbox")
    private val secondMailbox = MailboxIdentity("private_mailbox", "Personal inbox")
    private val links = listOf(
        MemberMailboxLink(firstMailbox.id, first),
        MemberMailboxLink(firstMailbox.id, second),
        MemberMailboxLink(secondMailbox.id, first),
    )
    private val sha = "a".repeat(64)
    private fun request(owner: OwnerId? = first) = CandidateAttachment(
        firstMailbox.id, sha, 12_000L, "application/pdf", owner, true,
    )
    private fun decide(candidate: CandidateAttachment, seen: Set<String> = emptySet()) =
        MailAttachmentIntakePolicy.decide(candidate,
            listOf(firstMailbox, secondMailbox), links, setOf(first, second), seen)

    @Test fun sharedAndMultipleMailboxesRequireExactDocumentOwnership() {
        assertEquals(IntakeDecision.StageForHumanReview(first, sha), decide(request(first)))
        assertEquals(IntakeDecision.StageForHumanReview(second, sha), decide(request(second)))
        assertEquals(IntakeDecision.Quarantine(IntakeRejection.AMBIGUOUS_OWNER),
            decide(request(null)))
        val privateMatch = request(first).copy(mailboxId = secondMailbox.id)
        assertEquals(IntakeDecision.StageForHumanReview(first, sha), decide(privateMatch))
        assertEquals(IntakeDecision.Quarantine(IntakeRejection.UNLINKED_OWNER),
            decide(privateMatch.copy(verifiedDocumentOwner = second)))
    }

    @Test fun senderAddressOrFilenameCannotOverrideMissingVerifiedOwner() {
        assertEquals(IntakeDecision.Quarantine(IntakeRejection.AMBIGUOUS_OWNER),
            decide(request(null)))
        assertEquals(IntakeDecision.Quarantine(IntakeRejection.UNVERIFIED_ACCOUNT),
            decide(request().copy(exactEnrolledIdentity = false)))
    }

    @Test fun duplicateSourcesAreQuarantinedBeforeParserReview() {
        assertEquals(IntakeDecision.Quarantine(IntakeRejection.ALREADY_SEEN),
            decide(request(), setOf(sha)))
    }

    @Test fun rejectsHugeEmptyNonPdfAndUnknownMailboxAttachments() {
        assertEquals(IntakeDecision.Quarantine(IntakeRejection.TOO_LARGE),
            decide(request().copy(sizeBytes = MailAttachmentIntakePolicy.MAX_ATTACHMENT_BYTES + 1)))
        assertEquals(IntakeDecision.Quarantine(IntakeRejection.TOO_LARGE),
            decide(request().copy(sizeBytes = 0)))
        assertEquals(IntakeDecision.Quarantine(IntakeRejection.UNSUPPORTED_TYPE),
            decide(request().copy(contentType = "image/png")))
        assertEquals(IntakeDecision.Quarantine(IntakeRejection.UNKNOWN_MAILBOX),
            decide(request().copy(mailboxId = "not_connected")))
    }

    @Test fun fingerprintAndPageLimitsAreStrict() {
        assertEquals(IntakeDecision.Quarantine(IntakeRejection.MALFORMED_FINGERPRINT),
            decide(request().copy(sourceSha256 = "A".repeat(64))))
        MailAttachmentIntakePolicy.validatePage(100, 15 * 1024 * 1024L)
        assertThrows(IllegalArgumentException::class.java) {
            MailAttachmentIntakePolicy.validatePage(101, 1024)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MailAttachmentIntakePolicy.validatePage(1, 15 * 1024 * 1024L + 1)
        }
    }

    @Test fun successfulDecisionNeverAutomaticallyApprovesOrInsertsMoney() {
        val review = decide(request())
        assertTrue(review is IntakeDecision.StageForHumanReview)
        assertFalse(review is IntakeDecision.Quarantine)
    }
}
