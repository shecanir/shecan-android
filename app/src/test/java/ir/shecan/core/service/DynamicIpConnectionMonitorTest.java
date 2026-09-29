package ir.shecan.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DynamicIpConnectionMonitorTest {
    private FakeGateway gateway;
    private FakeListener listener;
    private DynamicIpConnectionMonitor monitor;

    @Before
    public void setUp() {
        gateway = new FakeGateway();
        listener = new FakeListener();
        monitor = new DynamicIpConnectionMonitor(gateway, listener);
    }

    @Test
    public void failedVerificationRefreshesIpAndWaitsForConfirmation() {
        monitor.poll();
        monitor.poll();
        assertEquals(1, gateway.statusChecks.size());

        gateway.statusChecks.get(0).onRetry();
        assertTrue(monitor.isRecovering());
        assertEquals(1, listener.checkingCount);
        assertEquals(1, gateway.ipRefreshes.size());

        gateway.ipRefreshes.get(0).onSuccess("203.0.113.2");
        assertEquals(2, gateway.statusChecks.size());
        assertEquals(0, listener.connectedCount);

        gateway.statusChecks.get(1).onConnected();
        assertFalse(monitor.isRecovering());
        assertEquals(1, listener.connectedCount);
    }

    @Test
    public void failedRefreshCanBeRetriedOnNextPoll() {
        monitor.poll();
        gateway.statusChecks.get(0).onRetry();
        gateway.ipRefreshes.get(0).onError("temporary failure");

        monitor.poll();
        assertEquals(2, gateway.statusChecks.size());
        gateway.statusChecks.get(1).onRetry();
        assertEquals(2, gateway.ipRefreshes.size());
    }

    @Test
    public void staleCallbacksAfterDisconnectCannotChangeState() {
        monitor.poll();
        ConnectionStatusApiListener staleCheck = gateway.statusChecks.get(0);
        monitor.reset();

        staleCheck.onRetry();
        assertEquals(0, gateway.ipRefreshes.size());
        assertEquals(0, listener.checkingCount);

        monitor.poll();
        assertEquals(2, gateway.statusChecks.size());
    }

    @Test
    public void invalidUpdaterResponseRejectsActivation() {
        monitor.poll();
        gateway.statusChecks.get(0).onRetry();
        gateway.ipRefreshes.get(0).onInvalid();

        assertEquals(1, listener.rejectedCount);
        assertFalse(monitor.isRecovering());
        assertEquals(0, listener.connectedCount);
    }

    private static final class FakeGateway implements DynamicIpConnectionMonitor.Gateway {
        final List<ConnectionStatusApiListener> statusChecks = new ArrayList<>();
        final List<CoreApiResponseListener> ipRefreshes = new ArrayList<>();

        @Override
        public void checkStatus(ConnectionStatusApiListener callback) {
            statusChecks.add(callback);
        }

        @Override
        public void refreshIp(CoreApiResponseListener callback) {
            ipRefreshes.add(callback);
        }
    }

    private static final class FakeListener implements DynamicIpConnectionMonitor.Listener {
        int checkingCount;
        int connectedCount;
        int rejectedCount;

        @Override
        public boolean shouldCheck() {
            return true;
        }

        @Override
        public void onChecking() {
            checkingCount++;
        }

        @Override
        public void onConnected() {
            connectedCount++;
        }

        @Override
        public void onActivationRejected() {
            rejectedCount++;
        }
    }
}
