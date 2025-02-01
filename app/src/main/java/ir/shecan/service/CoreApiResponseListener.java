package ir.shecan.service;

// Listener Interface
public interface CoreApiResponseListener {
    void onSuccess(String response);
    void onError(String errorMessage);
    void onInvalid();
    void onOutOfRange();
    void onInTheRange();
}
