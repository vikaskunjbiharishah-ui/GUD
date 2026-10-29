package `in`.growurdhan.domain.mail

import `in`.growurdhan.core.model.OwnerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MailboxSyncTest {
    private val primary = OwnerId("primary")
    private val spouse = OwnerId("spouse")
    private val shared = MailboxIdentity("shared_box", "Family inbox")
    private val personal = MailboxIdentity("personal_box", "Personal inbox")
    private val links = listOf(
        MemberMailboxLink(shared.id, primary),
        MemberMailboxLink(shared.id, spouse),
        MemberMailboxLink(personal.id, primary),
    )

    @Test fun manyAccountsAdvanceCursorsAndSuppressDuplicates() {
        val pages = (1..6).map { n ->
            val tokenIn = if (n == 1) null else "page-$n"
            val tokenOut = if (n == 6) null else "page-${n + 1}"
            val sha = n.toString().repeat(64).take(64)
            tokenIn to MailboxPage(listOf(row(shared.id, "m$n", sha, primary)), tokenOut)
        }
        val personalPage = null to MailboxPage(
            listOf(row(personal.id, "p1", "c".repeat(64), primary)),
            null,
        )
        val gateway = Scripted(shared.id to pages, personal.id to listOf(personalPage))
        val first = MailboxSync.pull(
            listOf(shared, personal), links, setOf(primary, spouse),
            emptySet(), emptyMap(), gateway,
        )
        assertEquals(6, first.staged.size)
        assertEquals("c".repeat(64), first.staged.last().sha256)
        assertEquals("page-6", first.cursors[shared.id])
        assertEquals(null, first.cursors[personal.id])
        assertTrue(first.quarantined.isEmpty())

        val repeat = row(shared.id, "dup", "1".repeat(64), primary)
        val gateway2 = Scripted(
            shared.id to listOf("page-6" to MailboxPage(listOf(repeat, row(shared.id, "m6", "6".repeat(64), spouse)), null)),
        )
        val second = MailboxSync.pull(
            listOf(shared), links, setOf(primary, spouse),
            first.staged.map { it.sha256 }.toSet(), first.cursors, gateway2,
        )
        assertEquals(listOf("6".repeat(64)), second.staged.map { it.sha256 })
        assertEquals(spouse, second.staged.single().ownerId)
        assertEquals(IntakeRejection.ALREADY_SEEN, second.quarantined.single().reason)
        assertEquals(null, second.cursors[shared.id])
    }

    @Test fun wrongOwnerIsQuarantinedAndNotStaged() {
        val gateway = Scripted(
            personal.id to listOf(
                null to MailboxPage(listOf(row(personal.id, "cross", "a".repeat(64), spouse)), null),
            ),
        )
        val outcome = MailboxSync.pull(
            listOf(personal), links, setOf(primary, spouse),
            emptySet(), emptyMap(), gateway,
        )
        assertTrue(outcome.staged.isEmpty())
        assertEquals(IntakeRejection.UNLINKED_OWNER, outcome.quarantined.single().reason)
        assertEquals(mapOf(personal.id to null), outcome.cursors)
    }

    @Test fun monthlyPeriodIsThirtyDaysAndWorkNamesStayStable() {
        assertEquals(30L * 24L * 60L * 60L * 1000L, MailboxSyncSchedule.monthlyIntervalMillis())
        assertEquals("gud.mailbox.monthly", MailboxSyncSchedule.MONTHLY_WORK)
        assertEquals("gud.mailbox.manual", MailboxSyncSchedule.MANUAL_WORK)
    }

    private fun row(mailbox: String, message: String, sha: String, owner: OwnerId) =
        RemoteMailAttachment(mailbox, message, sha, 2_048, "application/pdf", owner, true)

    private class Scripted(
        vararg scripts: Pair<String, List<Pair<String?, MailboxPage>>>,
    ) : MailboxGateway {
        private val remaining = scripts.associate { (id, list) -> id to ArrayDeque(list) }
        override fun list(mailboxId: String, pageToken: String?): MailboxPage {
            val (expected, page) = remaining.getValue(mailboxId).removeFirst()
            assertEquals(expected, pageToken)
            assertEquals(mailboxId, page.attachments.first().mailboxId)
            return page
        }
    }
}
