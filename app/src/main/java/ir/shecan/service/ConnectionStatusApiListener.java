package ir.shecan.service;

public interface ConnectionStatusApiListener {
    void onConnected();
    void onRetry();
}
