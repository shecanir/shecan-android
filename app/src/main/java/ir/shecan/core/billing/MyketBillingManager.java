package ir.shecan.core.billing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ir.myket.billingclient.IabHelper;
import ir.myket.billingclient.util.IabResult;
import ir.myket.billingclient.util.Inventory;
import ir.myket.billingclient.util.Purchase;
import ir.myket.billingclient.util.SkuDetails;
import ir.shecan.BuildConfig;
import ir.shecan.core.constant.Constant;

public class MyketBillingManager {

    private static final String TAG = "MyketBillingManager";
    private static final String PREFS_NAME = "myket_billing";
    private static final String PAYLOAD_PREFIX = "payload_";

    private final Context appContext;
    private final SharedPreferences preferences;
    private final Set<String> consumableSkus = new HashSet<>();
    private final Set<String> nonConsumableSkus = new HashSet<>();
    private final Map<String, Purchase> ownedPurchases = new HashMap<>();

    private IabHelper helper;
    private Listener listener;
    private boolean ready;
    private String activePurchaseSku;
    private String inventoryRecoverySku;

    public MyketBillingManager(Context context) {
        appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void startSetup(
            List<String> consumables,
            List<String> nonConsumables,
            Listener listener
    ) {
        this.listener = listener;
        consumableSkus.clear();
        nonConsumableSkus.clear();
        addAllNonEmpty(consumableSkus, consumables);
        addAllNonEmpty(nonConsumableSkus, nonConsumables);

        if (!Constant.IsMyketMode) {
            notifyUnavailable("Myket billing is available only in the myket flavor.");
            return;
        }

        if (TextUtils.isEmpty(BuildConfig.IAB_PUBLIC_KEY)) {
            notifyUnavailable("MYKET_IAB_PUBLIC_KEY is empty.");
            return;
        }

        dispose();
        helper = new IabHelper(appContext, BuildConfig.IAB_PUBLIC_KEY);
        helper.enableDebugLogging(BuildConfig.DEBUG);
        helper.startSetup(result -> {
            if (helper == null) return;

            if (!result.isSuccess()) {
                ready = false;
                notifyUnavailable("Problem setting up Myket billing: " + result);
                return;
            }

            ready = true;
            if (this.listener != null) this.listener.onBillingReady();
            queryInventory();
        });
    }

    public boolean isReady() {
        return ready && helper != null;
    }

    public void queryInventory() {
        if (!isReady()) {
            notifyError("Myket billing is not ready.");
            return;
        }

        List<String> allSkus = getAllSkus();
        boolean querySkuDetails = !allSkus.isEmpty();

        try {
            helper.queryInventoryAsync(querySkuDetails, allSkus, inventoryListener);
        } catch (RuntimeException e) {
            notifyError("Error querying Myket inventory: " + e.getMessage());
        }
    }

    public void querySkuDetails() {
        if (!isReady()) return;

        List<String> allSkus = getAllSkus();
        if (allSkus.isEmpty()) return;

        try {
            helper.queryInventoryAsync(true, allSkus, (result, inventory) -> {
                if (helper == null) return;
                if (result.isFailure() || inventory == null) {
                    notifyError("Failed to refresh Myket product details: " + result);
                    return;
                }
                if (listener != null) {
                    listener.onSkuDetailsLoaded(inventory.getAllProducts());
                }
            });
        } catch (RuntimeException e) {
            notifyError("Error refreshing Myket product details: " + e.getMessage());
        }
    }

    public void launchPurchaseFlow(Activity activity, String sku, Long renewalOrderId) {
        if (!isReady()) {
            notifyError("Myket billing is not ready.");
            return;
        }

        if (activity == null || TextUtils.isEmpty(sku)) {
            notifyError("Invalid Myket purchase request.");
            return;
        }

        Purchase ownedPurchase = ownedPurchases.get(sku);
        if (ownedPurchase != null) {
            Log.d(TAG, "Myket sku is already owned. Redelivering before launching purchase: " + sku);
            handleVerifiedPurchase(ownedPurchase, true);
            return;
        }

        String payload = createDeveloperPayload(sku, renewalOrderId);
        savePendingPayload(sku, payload);
        activePurchaseSku = sku;

        try {
            helper.launchPurchaseFlow(activity, sku, purchaseFinishedListener, payload);
        } catch (RuntimeException e) {
            notifyError("Error launching Myket purchase flow: " + e.getMessage());
        }
    }

    public void consumePurchase(Object purchaseObject) {
        if (!isReady()) {
            notifyError("Myket billing is not ready.");
            return;
        }

        if (!(purchaseObject instanceof Purchase)) {
            notifyError("Invalid Myket purchase object.");
            return;
        }

        Purchase purchase = (Purchase) purchaseObject;
        if (purchase == null) {
            notifyError("Purchase is empty.");
            return;
        }

        try {
            helper.consumeAsync(purchase, consumeFinishedListener);
        } catch (RuntimeException e) {
            notifyError("Error consuming Myket purchase: " + e.getMessage());
        }
    }

    public void redeliverOwnedPurchases() {
        if (ownedPurchases.isEmpty()) return;
        for (Purchase purchase : new ArrayList<>(ownedPurchases.values())) {
            handleVerifiedPurchase(purchase, true);
        }
    }

    public void dispose() {
        ready = false;
        activePurchaseSku = null;
        inventoryRecoverySku = null;
        ownedPurchases.clear();
        if (helper != null) {
            try {
                helper.dispose();
            } catch (RuntimeException e) {
                Log.w(TAG, "Error disposing Myket helper", e);
            }
            helper = null;
        }
    }

    private final IabHelper.QueryInventoryFinishedListener inventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (helper == null) return;

            if (result.isFailure()) {
                notifyError("Failed to query Myket inventory: " + result);
                return;
            }

            if (inventory == null) {
                notifyError("Myket inventory is empty.");
                return;
            }

            if (listener != null) {
                listener.onSkuDetailsLoaded(inventory.getAllProducts());
            }

            String recoveringSku = inventoryRecoverySku;
            boolean recoveredFailedPurchase = false;
            ownedPurchases.clear();
            for (Purchase purchase : inventory.getAllPurchases()) {
                if (purchase != null && !TextUtils.isEmpty(purchase.getSku())) {
                    ownedPurchases.put(purchase.getSku(), purchase);
                    if (purchase.getSku().equals(recoveringSku)) {
                        recoveredFailedPurchase = true;
                    }
                }
                handleVerifiedPurchase(purchase, true);
            }
            if (!TextUtils.isEmpty(recoveringSku)) {
                inventoryRecoverySku = null;
                if (!recoveredFailedPurchase) {
                    notifyError("Myket purchase failed: item was not returned in inventory after purchase error.");
                }
            }
        }
    };

    private final IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (helper == null) return;

            if (result.isFailure()) {
                if (shouldRefreshInventoryAfterPurchaseFailure(result)) {
                    Log.w(TAG, "Myket purchase failed with recoverable response. Querying inventory: " + result);
                    inventoryRecoverySku = activePurchaseSku;
                    activePurchaseSku = null;
                    queryInventory();
                    return;
                }
                activePurchaseSku = null;
                notifyError("Myket purchase failed: " + result);
                return;
            }

            activePurchaseSku = null;
            handleVerifiedPurchase(purchase, false);
        }
    };

    private final IabHelper.OnConsumeFinishedListener consumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        @Override
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if (helper == null) return;

            if (result.isSuccess()) {
                if (purchase != null && !TextUtils.isEmpty(purchase.getSku())) {
                    ownedPurchases.remove(purchase.getSku());
                }
                if (listener != null) listener.onPurchaseConsumed(purchase);
            } else {
                notifyError("Error while consuming Myket purchase: " + result);
            }
        }
    };

    private void handleVerifiedPurchase(Purchase purchase, boolean restoredFromInventory) {
        if (purchase == null) {
            notifyError("Myket returned an empty purchase.");
            return;
        }

        if (!restoredFromInventory && !verifyPendingDeveloperPayload(purchase)) {
            notifyError("Myket developer payload verification failed for sku: " + purchase.getSku());
            return;
        }

        if (!restoredFromInventory) {
            clearPendingPayload(purchase.getSku());
        }

        if (consumableSkus.contains(purchase.getSku())) {
            if (listener != null) listener.onConsumablePurchaseReady(purchase, restoredFromInventory);
        } else {
            if (listener != null) listener.onNonConsumablePurchaseReady(purchase, restoredFromInventory);
        }
    }

    private boolean shouldRefreshInventoryAfterPurchaseFailure(IabResult result) {
        return result != null && result.toString() != null && result.toString().contains("response: 6");
    }

    private boolean verifyPendingDeveloperPayload(Purchase purchase) {
        String expectedPayload = preferences.getString(payloadKey(purchase.getSku()), null);
        // The backend remains authoritative. Some Myket responses omit or normalize developerPayload,
        // so only reject when both payloads are present and clearly conflict.
        return isDeveloperPayloadCompatible(expectedPayload, purchase.getDeveloperPayload());
    }

    private boolean isDeveloperPayloadCompatible(String expectedPayload, String actualPayload) {
        if (TextUtils.isEmpty(expectedPayload) || TextUtils.isEmpty(actualPayload)) return true;
        if (expectedPayload.equals(actualPayload)) return true;

        try {
            JSONObject expected = new JSONObject(expectedPayload);
            JSONObject actual = new JSONObject(actualPayload);
            return matchesStringField(expected, actual, "sla")
                    && matchesStringField(expected, actual, "period")
                    && matchesLongField(expected, actual, "order_id");
        } catch (Exception e) {
            Log.w(TAG, "Could not compare Myket developer payload. Continuing with backend verification.", e);
            return true;
        }
    }

    private boolean matchesStringField(JSONObject expected, JSONObject actual, String key) {
        if (!expected.has(key) || !actual.has(key)) return true;
        return expected.optString(key, "").equals(actual.optString(key, ""));
    }

    private boolean matchesLongField(JSONObject expected, JSONObject actual, String key) {
        if (!expected.has(key) || !actual.has(key)) return true;
        return expected.optLong(key, 0L) == actual.optLong(key, 0L);
    }

    private String createDeveloperPayload(String sku, Long renewalOrderId) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("version", 1);
            BillingPlan plan = findPlanBySku(sku);
            if (plan != null) {
                payload.put("sla", plan.getSla().getApiValue());
                payload.put("period", plan.getPeriod().getApiValue());
            }
            if (renewalOrderId != null && renewalOrderId > 0L) {
                payload.put("order_id", renewalOrderId);
            }
        } catch (Exception ignored) {
        }
        return payload.toString();
    }

    private BillingPlan findPlanBySku(String sku) {
        BillingPlan plan = BillingPlanCatalog.findBySku(sku);
        if (plan != null || TextUtils.isEmpty(sku) || sku.endsWith("_v2")) return plan;
        return BillingPlanCatalog.findBySku(sku + "_v2");
    }

    private void savePendingPayload(String sku, String payload) {
        preferences.edit().putString(payloadKey(sku), payload).apply();
    }

    private void clearPendingPayload(String sku) {
        preferences.edit().remove(payloadKey(sku)).apply();
    }

    private String payloadKey(String sku) {
        return PAYLOAD_PREFIX + sku;
    }

    private List<String> getAllSkus() {
        List<String> allSkus = new ArrayList<>();
        allSkus.addAll(consumableSkus);
        allSkus.addAll(nonConsumableSkus);
        return allSkus;
    }

    private void addAllNonEmpty(Set<String> target, List<String> source) {
        if (source == null) return;
        for (String sku : source) {
            if (!TextUtils.isEmpty(sku)) {
                target.add(sku);
            }
        }
    }

    private void notifyUnavailable(String message) {
        Log.w(TAG, message);
        if (listener != null) listener.onBillingUnavailable(message);
    }

    private void notifyError(String message) {
        Log.e(TAG, message);
        if (listener != null) listener.onBillingError(message);
    }

    public interface Listener {
        void onBillingReady();

        void onBillingUnavailable(String message);

        void onSkuDetailsLoaded(List<?> skuDetails);

        void onConsumablePurchaseReady(Object purchase, boolean restoredFromInventory);

        void onNonConsumablePurchaseReady(Object purchase, boolean restoredFromInventory);

        void onPurchaseConsumed(Object purchase);

        void onBillingError(String message);
    }
}
