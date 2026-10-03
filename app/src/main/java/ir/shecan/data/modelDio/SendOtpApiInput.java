package ir.shecan.data.modelDio;

public class SendOtpApiInput {
    private String identifier;

    public SendOtpApiInput(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
