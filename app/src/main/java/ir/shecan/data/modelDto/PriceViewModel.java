package ir.shecan.data.modelDto;

public class PriceViewModel {
    private Long price;
    private String dueDate;
    private Long credit;
    private Long newCredit;
    private Long roundedDeference;

    public static PriceViewModel fromPrice(long price) {
        PriceViewModel model = new PriceViewModel();
        model.price = Math.max(0L, price);
        return model;
    }

    public Long getPrice() {
        return price;
    }

    public String getDueDate() {
        return dueDate;
    }

    public Long getCredit() {
        return credit;
    }

    public Long getNewCredit() {
        return newCredit;
    }

    public Long getRoundedDeference() {
        return roundedDeference;
    }

    public long getSafePrice() {
        return price != null ? price : 0L;
    }
}
