package ir.shecan.data.modelDio;

public class PriceApiInput {
    private final String sla;
    private final String period;
    private final int discount;

    public PriceApiInput(String sla, String period, int discount) {
        this.sla = sla;
        this.period = period;
        this.discount = discount;
    }

    public String getSla() {
        return sla;
    }

    public String getPeriod() {
        return period;
    }

    public int getDiscount() {
        return discount;
    }
}
