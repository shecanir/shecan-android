package ir.shecan.data.modelDio;

public class VerifyApiInput {

    private String identifier;
    private String code;

    public VerifyApiInput(String code, String identifier) {
        this.code = code;
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
