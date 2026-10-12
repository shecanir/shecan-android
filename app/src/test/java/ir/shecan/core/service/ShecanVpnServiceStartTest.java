package ir.shecan.core.service;

import static org.junit.Assert.assertEquals;
import android.app.Application;
import android.content.Intent;
import android.net.VpnService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = Application.class, sdk = 28)
public class ShecanVpnServiceStartTest {
    private static class RecordingService extends ShecanVpnService {
        int activations;
        @Override protected void activateVpn() { activations++; }
    }

    @Test
    public void handlesSystemAlwaysOnStart() {
        RecordingService service = new RecordingService();
        assertEquals(VpnService.START_STICKY,
                service.onStartCommand(new Intent(VpnService.SERVICE_INTERFACE), 0, 1));
        assertEquals(1, service.activations);
    }

    @Test
    public void handlesStickyRestartWithoutIntent() {
        RecordingService service = new RecordingService();
        assertEquals(VpnService.START_STICKY, service.onStartCommand(null, 0, 2));
        assertEquals(1, service.activations);
    }

    @Test
    public void handlesIntentWithoutAction() {
        RecordingService service = new RecordingService();
        assertEquals(VpnService.START_STICKY, service.onStartCommand(new Intent(), 0, 3));
        assertEquals(1, service.activations);
    }

    @Test
    public void ignoresUnrecognizedAction() {
        RecordingService service = new RecordingService();
        assertEquals(VpnService.START_NOT_STICKY,
                service.onStartCommand(new Intent("unrecognized"), 0, 4));
        assertEquals(0, service.activations);
    }
}
