package ir.shecan.core.service;

public interface ConnectionStatusApiListener {
    void onConnected();
    void onRetry();
}
