package `in`.growurdhan.domain.finance

/**
 * Unlock memory from src/lib/sessionSecret.ts and the picker pause in src/lib/autoLock.ts.
 * The password stays in this process. It is not written down. A folder picker
 * must not lock the vault while it is on screen. After 30 seconds away, it locks.
 */
object SessionGate {
    const val LOCK_AFTER_MS = 30_000
    private var email: String? = null
    private var password: String? = null
    private var pickerDepth = 0

    fun remember(nextEmail: String, nextPassword: String) {
        email = nextEmail.trim().lowercase()
        password = nextPassword
    }

    fun peek(): SessionSecret? {
        val savedEmail = email
        val savedPassword = password
        if (savedEmail.isNullOrEmpty() || savedPassword.isNullOrEmpty()) return null
        return SessionSecret(savedEmail, savedPassword)
    }

    fun clear() {
        email = null
        password = null
    }

    fun beginPicker() {
        pickerDepth += 1
    }

    fun endPicker() {
        pickerDepth = maxOf(0, pickerDepth - 1)
    }

    fun pickerOpen(): Boolean = pickerDepth > 0
}

data class SessionSecret(val email: String, val password: String)
