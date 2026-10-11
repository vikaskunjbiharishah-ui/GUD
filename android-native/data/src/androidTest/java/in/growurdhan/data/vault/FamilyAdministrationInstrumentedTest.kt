package `in`.growurdhan.data.vault

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import `in`.growurdhan.core.model.*
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic family/HUF, custody, goal and encrypted-portable-backup tests. */
@RunWith(AndroidJUnit4::class)
class FamilyAdministrationInstrumentedTest {
    private fun testVault(action: suspend (SqlCipherVaultRepository) -> Unit) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val root = File(context.noBackupFilesDir, "m26-admin-$suffix")
        val alias = "in.growurdhan.test.m26.$suffix"
        val vault = SqlCipherVaultRepository(context, root, alias)
        try {
            vault.openOrCreate()
            action(vault)
        } finally {
            vault.close()
            root.deleteRecursively()
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
        }
    }

    @Test fun twoMemberHufGoalsCustodyAndNomineesStayOwnerScoped() = testVault { db ->
        val self = OwnerId("self")
        val huf = OwnerId("huf")
        db.createMember(FamilyMember(self, "Synthetic Self", "Self", true))
        db.createMember(FamilyMember(huf, "Synthetic HUF", "HUF", false))
        db.applyFamilyAdministration(FamilyAdminChange.Member(
            self, "Synthetic Self", "Self", "1990-01-01", null,
            "#ABCDEF", "Private synthetic notes", "ABCDE1234F",
        ))
        db.applyFamilyAdministration(FamilyAdminChange.Member(
            huf, "Synthetic HUF", "HUF", null, null, "#F1F5F9", null, null,
        ))
        val a = db.registerCustodyAccount(self,"CAMS","Self Folio")
        val b = db.registerCustodyAccount(huf,"DEMAT","HUF Demat")
        db.applyFamilyAdministration(FamilyAdminChange.Account(
            a.id,self,"CAMS","Self Folio","Synthetic Registrar","DPSYNTH001",
            "CLI0001","BO0001","Nominee Self","2026-01-01",
        ))
        db.applyFamilyAdministration(FamilyAdminChange.Account(
            b.id,huf,"DEMAT","HUF Demat","Synthetic DP","DPSYNTH002",
            "CLI0002","BO0002","Nominee HUF",null,
        ))
        val holdingA = Holding(HoldingId("h-self"), self, a.id, "Self equity",
            "EQUITY",QuantityScaled(100_000_000L),MoneyScaled(100_000L))
        val holdingB = Holding(HoldingId("h-huf"), huf, b.id, "HUF fund",
            "MUTUAL_FUND",QuantityScaled(200_000_000L),MoneyScaled(500_000L))
        db.createHolding(holdingA)
        db.createHolding(holdingB)
        db.appendEvent(LedgerEvent(
            "event-stable", holdingA.id, self, LedgerKind.BUY,
            "2026-01-01", 100_000_000L,MoneyScaled(100_000),null,
        ))
        db.applyFamilyAdministration(FamilyAdminChange.Goal(
            null,null,"Whole family retirement",MoneyScaled(10_000_000),"2036-01-01","HIGH"))
        db.applyFamilyAdministration(FamilyAdminChange.Goal(
            null,huf,"HUF reserve",null,null,"MEDIUM"))
        val goals = db.familyAdministration().goals
        val family = goals.single { it.name == "Whole family retirement" }
        val hufOnly = goals.single { it.name == "HUF reserve" }
        db.applyFamilyAdministration(FamilyAdminChange.TagHolding(
            family.id,holdingA.id,self,true))
        db.applyFamilyAdministration(FamilyAdminChange.TagHolding(
            family.id,holdingB.id,huf,true))
        db.applyFamilyAdministration(FamilyAdminChange.TagHolding(
            hufOnly.id,holdingB.id,huf,true))
        val state = db.familyAdministration()
        assertEquals("ABCDE****F",state.family.single { it.id==self }.panMasked)
        assertEquals(2,state.accounts.size)
        assertEquals("****0001",state.accounts.single { it.id == a.id }.clientMasked)
        assertEquals("Nominee HUF",state.accounts.single { it.id == b.id }.nominee)
        // Leaving optional fields blank during edit must preserve stored identifiers.
        db.applyFamilyAdministration(FamilyAdminChange.Account(
            b.id,huf,"DEMAT","HUF Demat Updated",null,null,null,null,null,null,
        ))
        assertEquals("****0002",db.familyAdministration().accounts.single { it.id==b.id }.clientMasked)
        assertEquals("Nominee HUF",db.familyAdministration().accounts.single { it.id==b.id }.nominee)

        assertEquals(2,state.goals.single { it.id == family.id }.holdingIds.size)
        assertEquals(1_100_000L,state.goals.single { it.id == family.id }.taggedExposure.raw)
        assertEquals(1_000_000L,state.goals.single { it.id == hufOnly.id }.taggedExposure.raw)
        assertNull(state.goals.single { it.id == hufOnly.id }.target)
        assertEquals("event-stable",db.events().single().id)
        db.applyFamilyAdministration(FamilyAdminChange.CloseAccount(a.id,self))
        assertNotNull(db.familyAdministration().accounts.single { it.id == a.id }.closedOn)
        assertEquals("event-stable",db.events().single().id)
        db.applyFamilyAdministration(FamilyAdminChange.DeleteGoal(hufOnly.id))
        assertEquals(1,db.familyAdministration().goals.size)
        assertEquals(2,db.holdings().size)
        assertEquals(1,db.events().size)
        assertEquals(0,db.health().foreignKeyViolations)
    }

    @Test fun crossOwnerLinksReassignmentAndDeactivationWithHoldingsFailClosed() =
        testVault { db ->
            val self=OwnerId("self")
            val spouse=OwnerId("spouse")
            db.createMember(FamilyMember(self,"Self","Self",true))
            db.createMember(FamilyMember(spouse,"Spouse","Spouse",false))
            val account=db.registerCustodyAccount(self,"CAMS","Self Folio")
            db.createHolding(Holding(HoldingId("h1"),self,account.id,"Recorded Fund",
                "MUTUAL_FUND",QuantityScaled(0),MoneyScaled(0)))
            db.applyFamilyAdministration(FamilyAdminChange.Goal(null,spouse,"Spouse Goal",
                MoneyScaled(100_000),"2035-01-01",null))
            val goal=db.familyAdministration().goals.single()
            assertThrows(Exception::class.java) {
                runBlocking { db.applyFamilyAdministration(FamilyAdminChange.TagHolding(
                    goal.id,HoldingId("h1"),self,true)) }
            }
            assertThrows(Exception::class.java) {
                runBlocking { db.applyFamilyAdministration(FamilyAdminChange.Account(
                    account.id,spouse,"CAMS","Claimed Folio",null,null,null,null,null,null)) }
            }
            assertThrows(Exception::class.java) {
                runBlocking { db.applyFamilyAdministration(FamilyAdminChange.SetMemberActive(
                    self,false)) }
            }
            // Goals are ownership dependencies: remove the spouse-only goal
            // before testing reversible deactivation on an otherwise empty member.
            db.applyFamilyAdministration(FamilyAdminChange.DeleteGoal(goal.id))
            db.applyFamilyAdministration(FamilyAdminChange.SetMemberActive(spouse,false))
            assertFalse(db.familyAdministration().family.single { it.id == spouse }.active)
            db.applyFamilyAdministration(FamilyAdminChange.SetMemberActive(spouse,true))
            assertTrue(db.familyAdministration().family.single { it.id == spouse }.active)
            assertEquals(1,db.holdings().size)
            assertEquals(1,db.accounts().size)
            assertEquals(0,db.health().foreignKeyViolations)
        }

    @Test fun encryptedV4PortableBackupRetainsProfileGoalsAndNomination() = testVault { original ->
        val owner=OwnerId("self")
        original.createMember(FamilyMember(owner,"Self","Self",true))
        val acc=original.registerCustodyAccount(owner,"BANK","Account A")
        original.applyFamilyAdministration(FamilyAdminChange.Account(
            acc.id,owner,"BANK","Account A","Synthetic Bank",null,null,null,
            "Nominee A",null))
        original.applyFamilyAdministration(FamilyAdminChange.Goal(
            null,owner,"Child Education",MoneyScaled(2_000_000),"2030-01-01",null))
        val pass="m26-fixture-backup-passphrase".toCharArray()
        val bytes=try { original.exportEncryptedBackup(pass) }
            finally { pass.fill('\u0000') }
        try {
            testVault { target ->
                val password="m26-fixture-backup-passphrase".toCharArray()
                try { target.restoreEncryptedBackup(password,bytes) }
                finally { password.fill('\u0000') }
                val snapshot=target.familyAdministration()
                assertEquals("Nominee A",snapshot.accounts.single().nominee)
                assertEquals("Child Education",snapshot.goals.single().name)
                assertEquals(2_000_000L,snapshot.goals.single().target?.raw)
                assertEquals(0,target.health().foreignKeyViolations)
            }
        } finally { bytes.fill(0) }
    }

    @Test fun minorChildRequiresOwnedGuardianAndPrimaryRelationshipRemainsImmutable() =
        testVault { db ->
            val self=OwnerId("self")
            val child=OwnerId("child")
            db.createMember(FamilyMember(self,"Synthetic Self","Self",true))
            db.createMember(FamilyMember(child,"Synthetic Child","Child",false))
            assertThrows(Exception::class.java) {
                runBlocking { db.applyFamilyAdministration(FamilyAdminChange.Member(
                    child,"Synthetic Child","Child","2020-01-01",null,null,null,null)) }
            }
            db.applyFamilyAdministration(FamilyAdminChange.Member(
                child,"Synthetic Child","Child","2020-01-01",self,null,null,null))
            assertEquals(self,db.familyAdministration().family.single { it.id==child }.guardianId)
            assertThrows(Exception::class.java) {
                runBlocking { db.applyFamilyAdministration(FamilyAdminChange.Member(
                    self,"Synthetic Self","HUF",null,null,null,null,null)) }
            }
            assertEquals(0,db.health().foreignKeyViolations)
        }
}
