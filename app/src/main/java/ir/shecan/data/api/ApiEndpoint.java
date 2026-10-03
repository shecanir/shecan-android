package ir.shecan.data.api;

public enum ApiEndpoint {
    OTP_EXISTS("/api/auth/exists"),
    VERIFY("/api/auth/verify"),
    LOGIN("/api/auth/login"),
    REGISTER("/api/auth/register"),
    USERS_LIST("/api/users");

    private final String path;
    ApiEndpoint(String path) { this.path = path; }
    public String getPath() { return "https://my.shecan.ir" + path; }
}