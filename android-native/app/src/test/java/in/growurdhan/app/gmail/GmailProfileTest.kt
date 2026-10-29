package `in`.growurdhan.app.gmail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GmailProfileTest {
    @Test fun fakeProfileSuppliesTheInboxAndKeepsTheTokenOutOfTheUrl() {
        var seenUrl = ""
        var seenToken = ""
        val email = GmailProfile.readEmail("synthetic-token") { url, bearer ->
            seenUrl = url
            seenToken = bearer
            """{"emailAddress":"family@gmail.com","messagesTotal":1}"""
        }
        assertEquals("family@gmail.com", email)
        assertEquals(GmailProfile.PROFILE_URL, seenUrl)
        assertEquals("synthetic-token", seenToken)
        assertEquals(false, seenUrl.contains("synthetic-token"))
    }

    @Test fun htmlAndMissingEmailAreRefused() {
        assertThrows(IllegalArgumentException::class.java) {
            GmailProfile.emailAddress("<html>login</html>")
        }
        assertThrows(IllegalArgumentException::class.java) {
            GmailProfile.emailAddress("""{"emailAddress":""}""")
        }
    }
}
