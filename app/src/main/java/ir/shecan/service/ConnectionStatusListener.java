package ir.shecan.service;

public interface ConnectionStatusListener {
    void onConnected();
    void onRetry();
}
