package ir.shecan.core.billing

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.result.ActivityResultRegistry
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.shecan.BuildConfig
import ir.shecan.core.constant.Constant
import org.json.JSONObject

class CafeBazaarBillingManager(context: Context) {

    private val appContext = context.applicationContext
    private val preferences: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val consumableSkus = mutableSetOf<String>()
    private val nonConsumableSkus = mutableSetOf<String>()

    private var payment: Payment? = null
    private var connection: Connection? = null
    private var listener: Listener? = null
    private var ready = false

    fun startSetup(
        consumables: List<String>?,
        nonConsumables: List<String>?,
        listener: Listener?
    ) {
        this.listener = listener
        consumableSkus.clear()
        nonConsumableSkus.clear()
        consumables?.filter { it.isNotBlank() }?.mapTo(consumableSkus) { it.trim() }
        nonConsumables?.filter { it.isNotBlank() }?.mapTo(nonConsumableSkus) { it.trim() }

        if (!Constant.IsCafeBazaarMode) {
            notifyUnavailable("Cafe Bazaar billing is available only in the cafeBazaar flavor.")
            return
        }

        if (BuildConfig.IAB_PUBLIC_KEY.isBlank()) {
            notifyUnavailable("BAZAAR_IAB_PUBLIC_KEY is empty.")
            return
        }

        dispose()

        val securityCheck = SecurityCheck.Enable(rsaPublicKey = BuildConfig.IAB_PUBLIC_KEY)
        val configuration = PaymentConfiguration(localSecurityCheck = securityCheck)
        payment = Payment(context = appContext, config = configuration)
        connection = payment?.connect {
            connectionSucceed {
                ready = true
                this@CafeBazaarBillingManager.listener?.onBillingReady()
                queryInventory()
            }
            connectionFailed { throwable ->
                ready = false
                notifyUnavailable("Problem connecting to Cafe Bazaar billing: ${throwable.message}")
            }
            disconnected {
                ready = false
                this@CafeBazaarBillingManager.listener?.onBillingDisconnected()
            }
        }
    }

    fun isReady(): Boolean = ready && payment != null && connection != null

    fun queryInventory() {
        if (!isReady()) {
            notifyError("Cafe Bazaar billing is not ready.")
            return
        }

        querySkuDetails()
        queryPurchasedProducts()
    }

    fun launchPurchaseFlow(registry: ActivityResultRegistry, sku: String, renewalOrderId: Long?) {
        val currentPayment = payment
        if (!isReady() || currentPayment == null) {
            notifyError("Cafe Bazaar billing is not ready.")
            return
        }

        if (sku.isBlank()) {
            notifyError("Invalid Cafe Bazaar purchase request.")
            return
        }

        val payload = createDeveloperPayload(sku, renewalOrderId)
        savePendingPayload(sku, payload)
        val request = PurchaseRequest(productId = sku, payload = payload)

        currentPayment.purchaseProduct(registry = registry, request = request) {
            purchaseFlowBegan {
                listener?.onPurchaseFlowBegan(sku)
            }
            failedToBeginFlow { throwable ->
                notifyError("Failed to begin Cafe Bazaar purchase flow: ${throwable.message}")
            }
            purchaseSucceed { purchaseInfo ->
                handleVerifiedPurchase(purchaseInfo, restoredFromInventory = false)
            }
            purchaseCanceled {
                listener?.onPurchaseCanceled(sku)
            }
            purchaseFailed { throwable ->
                notifyError("Cafe Bazaar purchase failed: ${throwable.message}")
            }
        }
    }

    fun consumePurchase(purchaseToken: String) {
        val currentPayment = payment
        if (!isReady() || currentPayment == null) {
            notifyError("Cafe Bazaar billing is not ready.")
            return
        }

        if (purchaseToken.isBlank()) {
            notifyError("Purchase token is empty.")
            return
        }

        currentPayment.consumeProduct(purchaseToken) {
            consumeSucceed {
                listener?.onPurchaseConsumed(purchaseToken)
            }
            consumeFailed { throwable ->
                notifyError("Error consuming Cafe Bazaar purchase: ${throwable.message}")
            }
        }
    }

    fun dispose() {
        ready = false
        try {
            connection?.disconnect()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Error disconnecting Cafe Bazaar billing", e)
        }
        connection = null
        payment = null
    }

    fun querySkuDetails() {
        if (!isReady()) return
        val currentPayment = payment ?: return
        val inAppSkus = (consumableSkus + nonConsumableSkus).toList()

        if (inAppSkus.isNotEmpty()) {
            currentPayment.getInAppSkuDetails(inAppSkus) {
                getSkuDetailsSucceed { skuDetails ->
                    listener?.onInAppSkuDetailsLoaded(skuDetails)
                }
                getSkuDetailsFailed { throwable ->
                    notifyError("Failed to load Cafe Bazaar in-app sku details: ${throwable.message}")
                }
            }
        }
    }

    private fun queryPurchasedProducts() {
        val currentPayment = payment ?: return
        currentPayment.getPurchasedProducts {
            querySucceed { purchases ->
                purchases.forEach { handleVerifiedPurchase(it, restoredFromInventory = true) }
            }
            queryFailed { throwable ->
                Log.w(TAG, "Failed to query Cafe Bazaar purchases", throwable)
            }
        }
    }

    private fun handleVerifiedPurchase(purchaseInfo: PurchaseInfo, restoredFromInventory: Boolean) {
        if (!restoredFromInventory && !verifyPendingDeveloperPayload(purchaseInfo)) {
            notifyError("Cafe Bazaar developer payload verification failed for sku: ${purchaseInfo.productId}")
            return
        }

        if (!restoredFromInventory) {
            clearPendingPayload(purchaseInfo.productId)
        }

        when {
            nonConsumableSkus.contains(purchaseInfo.productId) ->
                listener?.onNonConsumablePurchaseReady(purchaseInfo, restoredFromInventory)
            else ->
                listener?.onConsumablePurchaseReady(purchaseInfo, restoredFromInventory)
        }
    }

    private fun verifyPendingDeveloperPayload(purchaseInfo: PurchaseInfo): Boolean {
        val expectedPayload = preferences.getString(payloadKey(purchaseInfo.productId), null)
        // Backend verification is authoritative. Some stores may omit or normalize payloads,
        // so only reject when both payloads are present and clearly conflict.
        return isDeveloperPayloadCompatible(expectedPayload, purchaseInfo.payload)
    }

    private fun isDeveloperPayloadCompatible(expectedPayload: String?, actualPayload: String?): Boolean {
        if (expectedPayload.isNullOrBlank() || actualPayload.isNullOrBlank()) return true
        if (expectedPayload == actualPayload) return true

        return try {
            val expected = JSONObject(expectedPayload)
            val actual = JSONObject(actualPayload)
            matchesStringField(expected, actual, "sla") &&
                matchesStringField(expected, actual, "period") &&
                matchesLongField(expected, actual, "order_id")
        } catch (e: Exception) {
            Log.w(TAG, "Could not compare Cafe Bazaar developer payload. Continuing with backend verification.", e)
            true
        }
    }

    private fun matchesStringField(expected: JSONObject, actual: JSONObject, key: String): Boolean {
        if (!expected.has(key) || !actual.has(key)) return true
        return expected.optString(key) == actual.optString(key)
    }

    private fun matchesLongField(expected: JSONObject, actual: JSONObject, key: String): Boolean {
        if (!expected.has(key) || !actual.has(key)) return true
        return expected.optLong(key, 0L) == actual.optLong(key, 0L)
    }

    private fun createDeveloperPayload(sku: String, renewalOrderId: Long?): String {
        val payload = JSONObject()
            .put("version", 1)
        BillingPlanCatalog.findBySku(sku)?.let { plan ->
            payload.put("sla", plan.sla.apiValue)
            payload.put("period", plan.period.apiValue)
        }
        if (renewalOrderId != null && renewalOrderId > 0L) {
            payload.put("order_id", renewalOrderId)
        }
        return payload.toString()
    }

    private fun savePendingPayload(sku: String, payload: String) {
        preferences.edit().putString(payloadKey(sku), payload).apply()
    }

    private fun clearPendingPayload(sku: String) {
        preferences.edit().remove(payloadKey(sku)).apply()
    }

    private fun payloadKey(sku: String): String = PAYLOAD_PREFIX + sku

    private fun notifyUnavailable(message: String) {
        Log.w(TAG, message)
        listener?.onBillingUnavailable(message)
    }

    private fun notifyError(message: String) {
        Log.e(TAG, message)
        listener?.onBillingError(message)
    }

    interface Listener {
        fun onBillingReady()

        fun onBillingUnavailable(message: String)

        fun onBillingDisconnected()

        fun onInAppSkuDetailsLoaded(skuDetails: List<*>)

        fun onPurchaseFlowBegan(sku: String)

        fun onConsumablePurchaseReady(purchaseInfo: Any, restoredFromInventory: Boolean)

        fun onNonConsumablePurchaseReady(purchaseInfo: Any, restoredFromInventory: Boolean)

        fun onPurchaseCanceled(sku: String)

        fun onPurchaseConsumed(purchaseToken: String)

        fun onBillingError(message: String)
    }

    companion object {
        private const val TAG = "CafeBazaarBilling"
        private const val PREFS_NAME = "cafe_bazaar_billing"
        private const val PAYLOAD_PREFIX = "payload_"
    }
}
