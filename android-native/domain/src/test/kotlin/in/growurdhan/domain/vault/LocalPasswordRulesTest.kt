package `in`.growurdhan.domain.vault

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LocalPasswordRulesTest {
    @Test fun originalLengthAndComplexityRulesRemainMandatory() {
        assertTrue(LocalPasswordRules.issues("Good!Password123".toCharArray()).isEmpty())
        assertFalse(LocalPasswordRules.issues("onlylowercase".toCharArray()).isEmpty())
        assertFalse(LocalPasswordRules.issues("Ab1!".toCharArray()).isEmpty())
        assertFalse(LocalPasswordRules.issues("A".repeat(130).toCharArray()).isEmpty())
        assertFalse(LocalPasswordRules.issues("Good!Password123\n".toCharArray()).isEmpty())
    }

    @Test fun passwordRotationUsesBoundedWallClockAndRejectsClockRollback() {
        val now = 1_800_000_000_000L
        val recent = now - 179L * 86_400_000L
        assertFalse(LocalPasswordRules.requiresRotation(recent, now))
        assertTrue(LocalPasswordRules.requiresRotation(now - 180L * 86_400_000L, now))
        assertTrue(LocalPasswordRules.requiresRotation(now + 1, now))
        assertTrue(LocalPasswordRules.requiresRotation(0, now))
    }
}
