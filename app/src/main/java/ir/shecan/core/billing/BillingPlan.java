package ir.shecan.core.billing;

public class BillingPlan {

    private final BillingSla sla;
    private final BillingPeriod period;
    private final String sku;
    private final boolean purchasable;

    public BillingPlan(BillingSla sla, BillingPeriod period, String sku, boolean purchasable) {
        this.sla = sla;
        this.period = period;
        this.sku = sku;
        this.purchasable = purchasable;
    }

    public BillingSla getSla() {
        return sla;
    }

    public BillingPeriod getPeriod() {
        return period;
    }

    public String getSku() {
        return sku;
    }

    public boolean isPurchasable() {
        return purchasable;
    }

    public int getPlanId() {
        return sla.getPlanId();
    }

    public int getDurationId() {
        return period.getDurationId();
    }

    public String getTitle() {
        return sla.getTitle() + " - " + period.getTitle();
    }
}
