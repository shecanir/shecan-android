package ir.shecan.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AuthApiUrlTest {

    @Test
    public void issuesUrlUsesUserOrdersSavedQuery() {
        Uri uri = Uri.parse(AuthApi.buildIssuesUrl("api key", 0, 1000));

        assertEquals("106", uri.getQueryParameter("query_id"));
        assertEquals("0", uri.getQueryParameter("offset"));
        assertEquals("1000", uri.getQueryParameter("limit"));
        assertEquals("api key", uri.getQueryParameter("key"));
    }

    @Test
    public void sitePaymentUrlIncludesSelectedRenewalOrder() {
        Uri uri = Uri.parse(AuthApi.createSitePaymentUrl(
                "key", 1000L, "commercial", "1m", 0L, null, 341933L
        ));

        assertEquals("341933", uri.getQueryParameter("order_id"));
    }

    @Test
    public void newPurchaseDoesNotIncludeRenewalOrder() {
        Uri uri = Uri.parse(AuthApi.createSitePaymentUrl(
                "key", 1000L, "commercial", "1m", 0L, null, null
        ));

        assertNull(uri.getQueryParameter("order_id"));
    }
}
