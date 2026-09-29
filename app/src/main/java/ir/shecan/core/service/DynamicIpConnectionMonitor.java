package ir.shecan.core.service;

/** Verifies an active Pro connection and refreshes its registered IP when verification fails. */
public final class DynamicIpConnectionMonitor {
    public interface Gateway {
        void checkStatus(ConnectionStatusApiListener listener);

        void refreshIp(CoreApiResponseListener listener);
    }

    public interface Listener {
        boolean shouldCheck();

        void onChecking();

        void onConnected();

        void onActivationRejected();
    }

    private final Gateway gateway;
    private final Listener listener;
    private boolean inFlight;
    private boolean recovering;
    private int generation;

    public DynamicIpConnectionMonitor(Gateway gateway, Listener listener) {
        this.gateway = gateway;
        this.listener = listener;
    }

    public synchronized boolean isRecovering() {
        return recovering;
    }

    public synchronized void reset() {
        generation++;
        inFlight = false;
        recovering = false;
    }

    public synchronized void poll() {
        if (inFlight || !listener.shouldCheck()) return;

        inFlight = true;
        int requestGeneration = generation;
        checkStatus(requestGeneration, false);
    }

    private synchronized boolean isCurrent(int requestGeneration) {
        return generation == requestGeneration && listener.shouldCheck();
    }

    private void checkStatus(int requestGeneration, boolean afterRefresh) {
        gateway.checkStatus(new ConnectionStatusApiListener() {
            @Override
            public void onConnected() {
                synchronized (DynamicIpConnectionMonitor.this) {
                    if (!isCurrent(requestGeneration)) return;
                    recovering = false;
                    inFlight = false;
                    listener.onConnected();
                }
            }

            @Override
            public void onRetry() {
                synchronized (DynamicIpConnectionMonitor.this) {
                    if (!isCurrent(requestGeneration)) return;
                    recovering = true;
                    listener.onChecking();
                    if (afterRefresh) {
                        inFlight = false;
                    } else {
                        refreshIp(requestGeneration);
                    }
                }
            }
        });
    }

    private void refreshIp(int requestGeneration) {
        gateway.refreshIp(new CoreApiResponseListener() {
            @Override
            public void onSuccess(String response) {
                synchronized (DynamicIpConnectionMonitor.this) {
                    if (isCurrent(requestGeneration)) checkStatus(requestGeneration, true);
                }
            }

            @Override
            public void onInTheRange() {
                synchronized (DynamicIpConnectionMonitor.this) {
                    if (isCurrent(requestGeneration)) checkStatus(requestGeneration, true);
                }
            }

            @Override
            public void onError(String errorMessage) {
                synchronized (DynamicIpConnectionMonitor.this) {
                    if (isCurrent(requestGeneration)) inFlight = false;
                }
            }

            @Override
            public void onInvalid() {
                reject(requestGeneration);
            }

            @Override
            public void onOutOfRange() {
                reject(requestGeneration);
            }
        });
    }

    private synchronized void reject(int requestGeneration) {
        if (!isCurrent(requestGeneration)) return;
        reset();
        listener.onActivationRejected();
    }
}
