package `in`.growurdhan.app.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.growurdhan.app.presentation.PackageInstallFacts
import `in`.growurdhan.domain.commercial.PlayReceipts
import `in`.growurdhan.domain.commercial.PurchaseVerificationEvidence
import `in`.growurdhan.domain.finance.Licence
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/** Play is the only seller. Callers pass observations, never a stored "subscribed" flag. */
interface AnnualPlay {
    fun load(onResult: (List<PurchaseVerificationEvidence>) -> Unit)
    fun purchase(onFinished: (String?) -> Unit)
}

@Singleton
class PlayBillingGateway @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : AnnualPlay, PurchasesUpdatedListener {
    private val handler = Handler(Looper.getMainLooper())
    private var activity: Activity? = null
    private var client: BillingClient? = null
    private var loadGeneration = 0
    private var pendingBuy: ((String?) -> Unit)? = null

    private val connection = object : BillingClientStateListener {
        override fun onBillingSetupFinished(result: BillingResult) = Unit
        override fun onBillingServiceDisconnected() {
            handler.postDelayed({
                val billing = client ?: return@postDelayed
                if (!billing.isReady) billing.startConnection(this)
            }, 1_500)
        }
    }

    fun attach(host: Activity) {
        activity = host
        if (client != null) return
        val billing = BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        client = billing
        billing.startConnection(connection)
    }

    fun shutdown() {
        activity = null
        loadGeneration++
        pendingBuy = null
        client?.endConnection()
        client = null
    }

    override fun load(onResult: (List<PurchaseVerificationEvidence>) -> Unit) {
        queryWhenReady(++loadGeneration, 0, onResult)
    }

    private fun queryWhenReady(
        generation: Int,
        attempt: Int,
        onResult: (List<PurchaseVerificationEvidence>) -> Unit,
    ) {
        if (generation != loadGeneration) return
        val billing = client
        if (billing == null || !billing.isReady) {
            if (attempt < 8) {
                handler.postDelayed({ queryWhenReady(generation, attempt + 1, onResult) }, 350)
            } else {
                handler.post { if (generation == loadGeneration) onResult(emptyList()) }
            }
            return
        }
        billing.queryPurchasesAsync(subsParams()) { result, purchases ->
            if (generation != loadGeneration) return@queryPurchasesAsync
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                handler.post { onResult(emptyList()) }
                return@queryPurchasesAsync
            }
            acknowledge(billing, purchases) { forced ->
                handler.post {
                    if (generation == loadGeneration) onResult(map(purchases, forced))
                }
            }
        }
    }

    override fun purchase(onFinished: (String?) -> Unit) {
        val host = activity
        val billing = client
        if (host == null || billing == null || !billing.isReady) {
            onFinished("Google Play Billing is not ready.")
            return
        }
        if (!PackageInstallFacts.isPlayInstaller(appContext)) {
            onFinished("Install GrowUrDhan from Google Play to buy a licence.")
            return
        }
        billing.queryPurchasesAsync(subsParams()) { _, purchases ->
            val owned = purchases.any {
                it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    it.products.contains(Licence.SKU_ANNUAL)
            }
            if (owned) {
                handler.post { onFinished("The annual plan is already on this Play account.") }
                return@queryPurchasesAsync
            }
            handler.post { launch(host, billing, onFinished) }
        }
    }

    private fun launch(host: Activity, billing: BillingClient, onFinished: (String?) -> Unit) {
        pendingBuy = onFinished
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(Licence.SKU_ANNUAL)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billing.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build(),
        ) { result, details ->
            handler.post {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    finishBuy(result.debugMessage.ifBlank { "Google Play could not open the ₹99 yearly plan." })
                    return@post
                }
                val item = productDetailsOf(details).firstOrNull { it.productId == Licence.SKU_ANNUAL }
                if (item == null) {
                    finishBuy("Product ${Licence.SKU_ANNUAL} is not in Play Console yet.")
                    return@post
                }
                val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(item)
                val offers = item.subscriptionOfferDetails
                if (!offers.isNullOrEmpty()) {
                    val offer = offers.firstOrNull { it.offerId.isNullOrEmpty() } ?: offers.first()
                    params.setOfferToken(offer.offerToken)
                }
                val flow = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(params.build()))
                    .setObfuscatedAccountId("gud")
                    .build()
                val launched = billing.launchBillingFlow(host, flow)
                if (launched.responseCode != BillingClient.BillingResponseCode.OK) {
                    finishBuy(launched.debugMessage.ifBlank { "Google Play could not open the ₹99 yearly plan." })
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val waiting = pendingBuy
        pendingBuy = null
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val billing = client
                if (billing == null) {
                    handler.post { waiting?.invoke(null) }
                    return
                }
                acknowledge(billing, purchases.orEmpty()) { handler.post { waiting?.invoke(null) } }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                handler.post { waiting?.invoke("Purchase cancelled.") }
            else -> handler.post {
                waiting?.invoke(result.debugMessage.ifBlank { "Play Billing error ${result.responseCode}" })
            }
        }
    }

    private fun finishBuy(message: String) {
        val waiting = pendingBuy
        pendingBuy = null
        handler.post { waiting?.invoke(message) }
    }

    private fun acknowledge(
        billing: BillingClient,
        purchases: List<Purchase>,
        done: (Set<Purchase>) -> Unit,
    ) {
        val waiting = purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
        }
        if (waiting.isEmpty()) {
            done(emptySet())
            return
        }
        val left = AtomicInteger(waiting.size)
        val forced = java.util.Collections.synchronizedSet(mutableSetOf<Purchase>())
        waiting.forEach { purchase ->
            billing.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
            ) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) forced.add(purchase)
                if (left.decrementAndGet() == 0) done(forced.toSet())
            }
        }
    }

    private fun map(
        purchases: List<Purchase>,
        forceAcknowledged: Set<Purchase> = emptySet(),
    ): List<PurchaseVerificationEvidence> {
        val checkedAt = Instant.now()
        return purchases.flatMap { purchase ->
            purchase.products.map { productId ->
                PlayReceipts.evidence(
                    productId,
                    purchase.purchaseState,
                    purchase.isAcknowledged || purchase in forceAcknowledged,
                    purchase.originalJson,
                    fromBillingClient = true,
                    checkedAt,
                )
            }
        }
    }

    private fun subsParams() = QueryPurchasesParams.newBuilder()
        .setProductType(BillingClient.ProductType.SUBS)
        .build()

    private fun productDetailsOf(details: Any?): List<ProductDetails> {
        if (details == null) return emptyList()
        if (details is List<*>) return details.filterIsInstance<ProductDetails>()
        return runCatching {
            val method = details.javaClass.methods.firstOrNull { it.name == "getProductDetailsList" }
            val list = method?.invoke(details)
            if (list is List<*>) list.filterIsInstance<ProductDetails>() else emptyList()
        }.getOrDefault(emptyList())
    }
}
