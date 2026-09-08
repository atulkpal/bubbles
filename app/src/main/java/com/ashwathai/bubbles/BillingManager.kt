package com.ashwathai.bubbles

import android.app.Activity
import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play Billing facade for the "Remove Ads" in-app purchase.
 *
 * - Singleton object accessed directly (no interface).
 * - Init in Application.onCreate via [init]; UI gates on [isReady].
 * - Product: "remove_ads" (one-time, non-consumable).
 * - Persists purchase via DataStore through SettingsRepository.
 *
 * Flow:
 *   1. User taps "Remove Ads" in Settings
 *   2. [launchRemoveAdsFlow] → BillingClient → Google Play purchase dialog
 *   3. On success → [onPurchaseAcknowledged] fires → ads disabled app-wide
 */
object BillingManager : PurchasesUpdatedListener {

    private const val TAG = "BillingManager"
    const val PRODUCT_REMOVE_ADS = "remove_ads"

    // ── Readiness ──
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // ── Ad-free state ──
    private val _adsRemoved = MutableStateFlow(false)
    val adsRemoved: StateFlow<Boolean> = _adsRemoved.asStateFlow()

    private var billingClient: BillingClient? = null
    private var appContext: Context? = null
    private var cachedProductDetails: ProductDetails? = null

    // ── Callback ──
    var onPurchaseAcknowledged: (() -> Unit)? = null

    // ── Init ──

    fun init(context: Context, adsRemovedPersisted: Boolean) {
        _adsRemoved.value = adsRemovedPersisted
        if (adsRemovedPersisted) {
            Log.i(TAG, "Ads already removed — skipping billing init")
            return
        }
        appContext = context.applicationContext
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Billing connected")
                    _isReady.value = true
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected — will retry on next request")
                _isReady.value = false
            }
        })
    }

    // ── Product Query ──

    private fun queryProductDetails() {
        val client = billingClient ?: return
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        // PBL 8: callback now yields a QueryProductDetailsResult wrapper
        client.queryProductDetailsAsync(params) { result, productDetailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val detailsList = productDetailsResult?.productDetailsList ?: emptyList()
                cachedProductDetails = detailsList.firstOrNull {
                    it.productId == PRODUCT_REMOVE_ADS
                }
                Log.i(TAG, "Product queried: ${cachedProductDetails != null}")
            } else {
                Log.e(TAG, "Product query failed: ${result.debugMessage}")
            }
        }
    }

    // ── Existing Purchase Check ──

    private fun queryExistingPurchases() {
        val client = billingClient ?: return
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasRemoveAds = purchases.any {
                    it.products.contains(PRODUCT_REMOVE_ADS) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (hasRemoveAds) {
                    Log.i(TAG, "Existing remove_ads purchase found")
                    handlePurchaseVerified(purchases.first {
                        it.products.contains(PRODUCT_REMOVE_ADS) &&
                            it.purchaseState == Purchase.PurchaseState.PURCHASED
                    })
                }
            }
        }
    }

    // ── Launch Purchase Flow ──

    fun launchRemoveAdsFlow(activity: Activity) {
        val client = billingClient
        val details = cachedProductDetails
        if (client == null || !client.isReady) {
            Log.e(TAG, "Billing not ready")
            return
        }
        if (details == null) {
            Log.e(TAG, "Product details not cached — retrying query")
            queryProductDetails()
            return
        }

        val productList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productList)
            .build()

        val billingResult = client.launchBillingFlow(activity, flowParams)
        Log.d(TAG, "Billing flow launched: ${billingResult.responseCode}")
    }

    // ── PurchasesUpdatedListener ──

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        handlePurchaseVerified(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "Purchase cancelled by user")
            }
            else -> {
                Log.e(TAG, "Purchase error: ${result.debugMessage}")
            }
        }
    }

    // ── Purchase Acknowledgement ──

    private fun handlePurchaseVerified(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient?.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Purchase acknowledged")
                    setAdsRemoved()
                } else {
                    Log.e(TAG, "Acknowledge failed: ${result.debugMessage}")
                }
            }
        } else {
            setAdsRemoved()
        }
    }

    private fun setAdsRemoved() {
        _adsRemoved.value = true
        onPurchaseAcknowledged?.invoke()
        Log.i(TAG, "Ads removed — app-wide")
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}
