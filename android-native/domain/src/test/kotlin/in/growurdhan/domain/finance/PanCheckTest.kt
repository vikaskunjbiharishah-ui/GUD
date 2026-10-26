package `in`.growurdhan.domain.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Goldens from src/lib/kosha.test.ts section P. */
class PanCheckTest {
    private val self = FamilyPan("fam_1", "Vikas", panForStatements = "ABCPS1234K", panMasked = "ABCPS****K")
    private val spouse = FamilyPan("fam_2", "Nancy", panMasked = "ABTPS****M")
    private val huf = FamilyPan("fam_3", "Vikas HUF", panMasked = "AAAPS****R")
    private val family = listOf(self, spouse, huf)

    @Test fun importPanMustMatchTheSelectedMember() {
        assertTrue(PanCheck.matches("ABCDE1234F", "ABCDE****F"))
        assertTrue(PanCheck.matches("abcde 1234 f", "ABCDE****F"))
        assertFalse(PanCheck.matches("ABCDE1234F", "ABTPS9999M"))
        assertEquals("ABCDE****F", PanCheck.fingerprint("abcde1234f"))
        assertEquals("ABCDE****F", PanCheck.display("abcde1234f"))
        assertEquals("not printed", PanCheck.display(null))

        assertEquals(PanImport.Match, PanCheck.checkImport(self, family, "ABCPS1234K"))
        val switched = PanCheck.checkImport(self, family, "ABTPS8888M")
        assertTrue(switched is PanImport.Switch)
        assertEquals("Nancy", (switched as PanImport.Switch).matched.name)
        assertTrue(PanCheck.checkImport(self, family, "ZZZZZ9999Z") is PanImport.Unknown)
        assertTrue(PanCheck.checkImport(huf.copy(panMasked = null), family, "ZZZZZ9999Z") is PanImport.MemberHasNoPan)
        assertEquals(PanImport.NoPdfPan, PanCheck.checkImport(self, family, null))
        assertEquals(PanImport.NoPdfPan, PanCheck.checkImport(self, family, "not a pan"))

        assertTrue(PanCheck.blocksCommit(PanImport.Switch(spouse)))
        assertTrue(PanCheck.blocksCommit(PanImport.Unknown("ZZZZZ9999Z")))
        assertTrue(PanCheck.blocksCommit(PanImport.MemberHasNoPan("ZZZZZ9999Z")))
        assertFalse(PanCheck.blocksCommit(PanImport.Match))
        assertFalse(PanCheck.blocksCommit(PanImport.NoPdfPan))

        assertEquals("Nancy", PanCheck.findDuplicate(family, "ABTPS0000M")?.name)
        assertNull(PanCheck.findDuplicate(family, "ABCPS1234K", excludeId = "fam_1"))
        assertNull(PanCheck.findDuplicate(family, "123"))
    }
}
