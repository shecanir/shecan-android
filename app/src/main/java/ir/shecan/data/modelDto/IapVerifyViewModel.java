package ir.shecan.data.modelDto;

public class IapVerifyViewModel {
    private Boolean ok;
    private Boolean duplicate;
    private String market;
    private Long orderId;
    private Long paymentId;
    private String status;
    private String error;
    private String detail;

    public boolean isOk() {
        return ok != null && ok;
    }

    public boolean isDuplicate() {
        return duplicate != null && duplicate;
    }

    public String getMarket() {
        return market;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getDetail() {
        return detail;
    }
}
