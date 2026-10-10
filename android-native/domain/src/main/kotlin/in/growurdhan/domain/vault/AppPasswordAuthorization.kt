package `in`.growurdhan.domain.vault

/** A process-local secondary gate. Does not decrypt the SQLCipher database key. */
interface AppPasswordAuthorization {
    fun hasVerifiedPassword(): Boolean
}

/**
 * Direct construction in synthetic JVM tests only. Production Hilt MUST bind
 * LocalAppPasswordStore, which defaults to locked and has no bypass path.
 */
object UnitTestPasswordAdmission : AppPasswordAuthorization {
    override fun hasVerifiedPassword(): Boolean = true
}
