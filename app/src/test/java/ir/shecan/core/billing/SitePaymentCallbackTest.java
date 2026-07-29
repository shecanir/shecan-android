package ir.shecan.core.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SitePaymentCallbackTest {

    @Test
    public void barePanelCallbackIsAllowedToRender() {
        Uri uri = Uri.parse("https://my.shecan.ir/panel/payment");

        assertTrue(SitePaymentCallback.isCallback(uri));
        assertFalse(SitePaymentCallback.hasResolvableOutcome(uri));
    }

    @Test
    public void parsesPaymentIdFromPanelPath() {
        Uri uri = Uri.parse("https://my.shecan.ir/panel/payment/1088881");

        assertEquals(1088881L, SitePaymentCallback.resolvePaymentId(uri));
        assertTrue(SitePaymentCallback.hasResolvableOutcome(uri));
    }

    @Test
    public void recognizesBackendPaidStatusId() {
        Uri uri = Uri.parse("https://my.shecan.ir/panel/payment?status_id=20");

        assertEquals(SitePaymentCallback.RESULT_SUCCESS, SitePaymentCallback.resolveResult(uri));
    }

    @Test
    public void recognizesFailedCustomSchemeCallback() {
        Uri uri = Uri.parse("shecan://payment-callback?payment_status=failed");

        assertTrue(SitePaymentCallback.isCallback(uri));
        assertEquals(SitePaymentCallback.RESULT_FAILED, SitePaymentCallback.resolveResult(uri));
    }
}
