package ir.shecan.data.modelDto;

public class SendOtpApiViewModel {

    private String expiresAt;
    private String message;

    public SendOtpApiViewModel(String expiresAt, String message) {
        this.expiresAt = expiresAt;
        this.message = message;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
