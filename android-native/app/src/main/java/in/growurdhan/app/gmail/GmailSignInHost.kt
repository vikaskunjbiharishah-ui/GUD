package `in`.growurdhan.app.gmail

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class GmailUiState(
    val emails: List<String> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
)

/**
 * Same Google Play services grant as the Tauri shell: gmail.readonly,
 * matched by package name and the device-test certificate. No client secret.
 */
class GmailSignInHost(
    private val activity: ComponentActivity,
    private val scope: CoroutineScope,
) {
    private val client = GmailClient(activity.applicationContext)
    private val stateFlow = MutableStateFlow(
        GmailUiState(emails = runCatching { client.emails() }.getOrDefault(emptyList())),
    )
    val state: StateFlow<GmailUiState> = stateFlow
    private val gmailScope = Scope("https://www.googleapis.com/auth/gmail.readonly")

    private val authLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val data = result.data
        if (data != null) {
            try {
                val auth = Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
                finish(auth.accessToken)
            } catch (e: Exception) {
                fail(e.message ?: "Google sign-in failed.")
            }
            return@registerForActivityResult
        }
        if (result.resultCode != Activity.RESULT_OK) fail("Google sign-in was cancelled.")
        else fail("Google sign-in returned no account.")
    }

    @Suppress("DEPRECATION")
    private val pickLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            fail("Google sign-in was cancelled.")
            return@registerForActivityResult
        }
        val email = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME).orEmpty()
        if (email.isBlank()) fail("Choose a Gmail account.") else authorize(email)
    }

    fun connect() {
        if (stateFlow.value.busy) return
        stateFlow.value = stateFlow.value.copy(busy = true, message = null)
        if (runCatching { client.emails() }.getOrDefault(emptyList()).isNotEmpty()) pickAccount()
        else authorize(null)
    }

    fun disconnect(email: String) {
        runCatching { client.forget(email) }
        stateFlow.value = stateFlow.value.copy(
            emails = runCatching { client.emails() }.getOrDefault(emptyList()),
            busy = false,
            message = null,
        )
    }

    fun clearMessage() {
        stateFlow.value = stateFlow.value.copy(message = null)
    }

    @Suppress("DEPRECATION")
    private fun pickAccount() {
        val intent = AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google"),
            true,
            "Choose the Gmail inbox that receives these statements",
            null,
            null,
            null,
        )
        pickLauncher.launch(intent)
    }

    private fun authorize(email: String?) {
        val builder = AuthorizationRequest.builder().setRequestedScopes(listOf(gmailScope))
        if (!email.isNullOrBlank()) builder.setAccount(Account(email, "com.google"))
        Identity.getAuthorizationClient(activity).authorize(builder.build())
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    val pending = result.pendingIntent
                    if (pending == null) {
                        fail("Google sign-in could not open.")
                        return@addOnSuccessListener
                    }
                    authLauncher.launch(IntentSenderRequest.Builder(pending).build())
                } else {
                    finish(result.accessToken)
                }
            }
            .addOnFailureListener { error -> fail(error.message ?: "Google sign-in failed.") }
    }

    private fun finish(token: String?) {
        if (token.isNullOrBlank()) {
            fail("Google did not allow Gmail to be read.")
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                val email = client.profileEmail(token)
                if (email.isBlank()) {
                    fail("Gmail did not return an email address.")
                    return@launch
                }
                client.remember(email)
                stateFlow.value = GmailUiState(emails = client.emails())
            } catch (e: Exception) {
                fail(e.message ?: "Could not read this Gmail account.")
            }
        }
    }

    private fun fail(message: String) {
        stateFlow.value = stateFlow.value.copy(
            busy = false,
            emails = runCatching { client.emails() }.getOrDefault(stateFlow.value.emails),
            message = message,
        )
    }
}
