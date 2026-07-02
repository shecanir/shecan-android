package ir.shecan.core.billing;

import android.util.Log;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MarketplacePriceCatalog {

    private static final Map<BillingStore, Map<String, Long>> PRICES =
            new EnumMap<>(BillingStore.class);

    private MarketplacePriceCatalog() {
    }

    public static synchronized void putPrice(BillingStore store, String sku, String rawPrice) {
        long rawValue = parsePrice(rawPrice);
        long price = store == BillingStore.CAFE_BAZAAR
                ? Math.round(rawValue / 10d)
                : rawValue;
        if (store == null || sku == null || sku.trim().isEmpty() || price <= 0L) return;
        Log.d(
                "MarketplacePrice",
                "store=" + store + " sku=" + sku + " raw=" + rawPrice + " toman=" + price
        );
        Map<String, Long> storePrices = PRICES.get(store);
        if (storePrices == null) {
            storePrices = new HashMap<>();
            PRICES.put(store, storePrices);
        }
        storePrices.put(normalizeSku(sku), price);
    }

    public static synchronized Long getTomanPrice(BillingStore store, String sku) {
        Map<String, Long> storePrices = PRICES.get(store);
        if (storePrices == null || sku == null) return null;
        return storePrices.get(normalizeSku(sku));
    }

    public static synchronized void clear(BillingStore store) {
        if (store != null) PRICES.remove(store);
    }

    private static long parsePrice(String rawPrice) {
        if (rawPrice == null) return 0L;
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < rawPrice.length(); i++) {
            char c = rawPrice.charAt(i);
            int digit = Character.digit(c, 10);
            if (digit >= 0) digits.append(digit);
        }
        if (digits.length() == 0) return 0L;
        try {
            return Long.parseLong(digits.toString());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static String normalizeSku(String sku) {
        return sku.trim().toLowerCase(Locale.US);
    }
}
