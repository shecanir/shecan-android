package ir.shecan.core.billing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BillingPlanCatalog {

    public static final String SKU_BRONZE_1M = "shecan_bronze_1m";
    public static final String SKU_BRONZE_3M = "shecan_bronze_3m";
    public static final String SKU_BRONZE_6M = "shecan_bronze_6m";
    public static final String SKU_BRONZE_1Y = "shecan_bronze_1y";

    public static final String SKU_SILVER_1M = "shecan_silver_1m";
    public static final String SKU_SILVER_3M = "shecan_silver_3m";
    public static final String SKU_SILVER_6M = "shecan_silver_6m";
    public static final String SKU_SILVER_1Y = "shecan_silver_1y";

    public static final String SKU_GOLD_1M = "shecan_gold_1m";
    public static final String SKU_GOLD_3M = "shecan_gold_3m";
    public static final String SKU_GOLD_6M = "shecan_gold_6m";
    public static final String SKU_GOLD_1Y = "shecan_gold_1y";

    public static final String SKU_COMMERCIAL_1M = "shecan_commercial_1m";
    public static final String SKU_COMMERCIAL_3M = "shecan_commercial_3m";
    public static final String SKU_COMMERCIAL_6M = "shecan_commercial_6m";
    public static final String SKU_COMMERCIAL_1Y = "shecan_commercial_1y";

    private static final List<BillingPlan> PLANS = buildPlans();

    private BillingPlanCatalog() {
    }

    public static List<BillingPlan> plans() {
        return PLANS;
    }

    public static List<String> skus() {
        return skus(PLANS);
    }

    public static List<BillingPlan> purchasablePlans() {
        List<BillingPlan> plans = new ArrayList<>();
        for (BillingPlan plan : PLANS) {
            if (plan.isPurchasable()) plans.add(plan);
        }
        return Collections.unmodifiableList(plans);
    }

    public static List<String> purchasableSkus() {
        return skus(purchasablePlans());
    }

    private static List<String> skus(List<BillingPlan> plans) {
        List<String> skus = new ArrayList<>();
        for (BillingPlan plan : plans) {
            skus.add(plan.getSku());
        }
        return skus;
    }

    public static BillingPlan findBySku(String sku) {
        if (sku == null) return null;
        for (BillingPlan plan : PLANS) {
            if (sku.equals(plan.getSku())) return plan;
        }
        return null;
    }

    private static List<BillingPlan> buildPlans() {
        List<BillingPlan> plans = new ArrayList<>();
        plans.add(new BillingPlan(BillingSla.BRONZE, BillingPeriod.ONE_MONTH, SKU_BRONZE_1M, true));
        plans.add(new BillingPlan(BillingSla.BRONZE, BillingPeriod.THREE_MONTHS, SKU_BRONZE_3M, true));
        plans.add(new BillingPlan(BillingSla.BRONZE, BillingPeriod.SIX_MONTHS, SKU_BRONZE_6M, true));
        plans.add(new BillingPlan(BillingSla.BRONZE, BillingPeriod.ONE_YEAR, SKU_BRONZE_1Y, true));

        plans.add(new BillingPlan(BillingSla.SILVER, BillingPeriod.ONE_MONTH, SKU_SILVER_1M, true));
        plans.add(new BillingPlan(BillingSla.SILVER, BillingPeriod.THREE_MONTHS, SKU_SILVER_3M, true));
        plans.add(new BillingPlan(BillingSla.SILVER, BillingPeriod.SIX_MONTHS, SKU_SILVER_6M, true));
        plans.add(new BillingPlan(BillingSla.SILVER, BillingPeriod.ONE_YEAR, SKU_SILVER_1Y, true));

        plans.add(new BillingPlan(BillingSla.GOLD, BillingPeriod.ONE_MONTH, SKU_GOLD_1M, true));
        plans.add(new BillingPlan(BillingSla.GOLD, BillingPeriod.THREE_MONTHS, SKU_GOLD_3M, true));
        plans.add(new BillingPlan(BillingSla.GOLD, BillingPeriod.SIX_MONTHS, SKU_GOLD_6M, true));
        plans.add(new BillingPlan(BillingSla.GOLD, BillingPeriod.ONE_YEAR, SKU_GOLD_1Y, true));

        plans.add(new BillingPlan(BillingSla.COMMERCIAL, BillingPeriod.ONE_MONTH, SKU_COMMERCIAL_1M, true));
        plans.add(new BillingPlan(BillingSla.COMMERCIAL, BillingPeriod.THREE_MONTHS, SKU_COMMERCIAL_3M, true));
        plans.add(new BillingPlan(BillingSla.COMMERCIAL, BillingPeriod.SIX_MONTHS, SKU_COMMERCIAL_6M, true));
        plans.add(new BillingPlan(BillingSla.COMMERCIAL, BillingPeriod.ONE_YEAR, SKU_COMMERCIAL_1Y, true));
        return Collections.unmodifiableList(plans);
    }
}
