package ir.shecan.core.billing;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public final class MyketBillingProducts {

    private MyketBillingProducts() {
    }

    public static List<String> consumableSkus() {
        return legacySkus(BillingPlanCatalog.purchasableSkus());
    }

    public static List<String> nonConsumableSkus() {
        return Collections.emptyList();
    }

    public static List<String> allSkus() {
        return legacySkus(BillingPlanCatalog.purchasableSkus());
    }

    public static boolean hasAnySku() {
        return !allSkus().isEmpty();
    }

    public static String legacySku(String sku) {
        if (sku == null) return null;
        return sku.endsWith("_v2") ? sku.substring(0, sku.length() - 3) : sku;
    }

    private static List<String> legacySkus(List<String> skus) {
        List<String> legacySkus = new ArrayList<>();
        for (String sku : skus) {
            legacySkus.add(legacySku(sku));
        }
        return Collections.unmodifiableList(legacySkus);
    }
}
