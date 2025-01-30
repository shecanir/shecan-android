package ir.shecan.service;

public interface BaseApiResponseListener {
    void onError(String errorMessage);
    void onSuccess();
}
