package ir.shecan.core.service;

public interface BaseApiResponseListener {
    void onError(String errorMessage);
    void onSuccess();
}
