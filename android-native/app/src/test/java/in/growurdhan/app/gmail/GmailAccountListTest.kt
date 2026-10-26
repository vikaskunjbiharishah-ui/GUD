package `in`.growurdhan.app.gmail

import org.junit.Assert.assertEquals
import org.junit.Test

class GmailAccountListTest {
    @Test fun rememberKeepsOneRowPerInbox() {
        val once = GmailAccountList.remember(emptyList(), " Family@gmail.com ")
        val twice = GmailAccountList.remember(once, "family@gmail.com")
        assertEquals(listOf("Family@gmail.com"), twice)
    }

    @Test fun forgetIsCaseInsensitive() {
        val left = GmailAccountList.forget(listOf("a@gmail.com", "b@gmail.com"), "A@gmail.com")
        assertEquals(listOf("b@gmail.com"), left)
    }

    @Test fun blankAddressIsIgnored() {
        assertEquals(emptyList<String>(), GmailAccountList.remember(emptyList(), "  "))
    }
}
