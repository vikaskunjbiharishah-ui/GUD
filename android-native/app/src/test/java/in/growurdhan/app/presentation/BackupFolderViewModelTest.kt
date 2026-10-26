package `in`.growurdhan.app.presentation

import `in`.growurdhan.core.model.VaultStatus
import `in`.growurdhan.domain.vault.UnitTestPasswordAdmission
import `in`.growurdhan.domain.vault.VaultRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupFolderViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun lockedVaultDoesNotExportOrKeepThePassword() {
        val gate = VaultSessionGate(UnitTestPasswordAdmission)
        val repo = mockk<VaultRepository>()
        val vm = BackupFolderViewModel(mockk(relaxed = true), repo, gate)
        vm.useMemoryForTest(object : FolderMemory {
            override fun read() = StoredFolder("content://folder", "Drive", true, null, null, 0)
            override fun write(next: StoredFolder) = Unit
        })
        val password = "synthetic-only-test-password".toCharArray()
        assertNull(vm.stage(password))
        assertTrue(password.all { it == '\u0000' })
        assertEquals(
            "Unlock the vault to run scheduled backup. The password is not stored.",
            vm.state.value.message,
        )
        coVerify(exactly = 0) { repo.exportEncryptedBackup(any()) }
    }

    @Test fun writesTheNewCopyAndDeletesOnlyTheOldest() = runTest {
        val gate = VaultSessionGate(UnitTestPasswordAdmission).also { it.grantDeviceCredential() }
        val repo = mockk<VaultRepository>()
        val payload = byteArrayOf(7, 8, 9)
        coEvery { repo.openOrCreate() } returns VaultStatus(true, true, 4)
        coEvery { repo.exportEncryptedBackup(any()) } returns payload
        val existing = mutableListOf("a" to 1L, "b" to 2L, "c" to 3L, "d" to 4L, "e" to 5L)
        val written = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        var stored = StoredFolder("content://folder", "Drive", true, null, null, 0)
        val vm = BackupFolderViewModel(mockk(relaxed = true), repo, gate)
        vm.useMemoryForTest(object : FolderMemory {
            override fun read() = stored
            override fun write(next: StoredFolder) { stored = next }
        })
        vm.openTree = {
            object : KoshaTree {
                override val label = "Drive"
                override fun files() = existing.toList()
                override fun write(name: String, bytes: ByteArray) {
                    written += name
                    existing += name to Long.MAX_VALUE
                    assertTrue(bytes.contentEquals(byteArrayOf(7, 8, 9)))
                }
                override fun delete(name: String) {
                    deleted += name
                    existing.removeAll { it.first == name }
                }
            }
        }
        val password = "synthetic-only-test-password".toCharArray()
        val staged = vm.stage(password)
        assertTrue(staged != null)
        gate.grantDeviceCredential()
        vm.writeAfterCredential()
        advanceUntilIdle()
        assertEquals(1, written.size)
        assertTrue(written.single().endsWith(".kosha"))
        assertEquals(listOf("a"), deleted)
        assertTrue(password.all { it == '\u0000' })
        assertTrue(payload.all { it == 0.toByte() })
        assertEquals(5, stored.lastKept)
        assertNull(stored.lastError)
        assertEquals(
            "Locked backup saved. Only the last five copies are kept.",
            vm.state.value.message,
        )
    }
}