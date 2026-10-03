package ir.shecan.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.Locale;

import ir.shecan.BuildConfig;
import ir.shecan.core.constant.RequestStatus;
import ir.shecan.data.modelDio.DialogMatchApiInput;
import ir.shecan.data.modelDto.IssuesViewModel;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.ui.widget.rateHelper.RatingState;

@RunWith(RobolectricTestRunner.class)
public class DynamicDialogRequestFactoryTest {

    private Context context;
    private AppStorage storage;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        storage = new AppStorage(context);
        storage.clearAll();
    }

    @Test
    public void buildsPayloadFromStoredUserServiceAndRating() {
        storage.saveToken(new VerifyApiViewModel(
                false,
                "api-key",
                "2026-01-01T00:00:00Z",
                "فرشاد",
                1,
                "2026-02-01T00:00:00Z",
                "اصغرزاده",
                "Old9357706279",
                "user@example.com",
                null,
                1,
                null,
                null,
                Collections.emptyList()
        ));

        ServiceItem service = new ServiceItem("", "", "", "", "", 0, 0, IssuesViewModel.IssuesDTO.createDefault());
        service.cfServiceType = 48;
        service.statusId = RequestStatus.ACTIVE.getValue();
        service.startDate = "2026-01-01";
        service.dueDate = "2026-05-25";
        storage.saveServiceStatus(service);

        RatingState ratingState = new RatingState();
        ratingState.lastRatingValue = 5;
        storage.saveRatingState(ratingState);

        DialogMatchApiInput input = DynamicDialogRequestFactory.fromStorage(context);

        assertEquals("09357706279", input.getMobileNumber());
        assertEquals("api-key", input.getApiKey());
        assertEquals("pro", input.getServiceType());
        assertEquals("silver", input.getPlan());
        assertEquals("2026-01-01", input.getStartDate());
        assertEquals("2026-05-25", input.getDueDate());
        assertEquals(Collections.singletonList(expectedStore()), input.getStore());
        assertTrue(input.isRated());
        assertEquals("active", input.getPlanStatus());
    }

    @Test
    public void usesServiceMobileWhenTokenLoginIsMissing() {
        ServiceItem service = new ServiceItem("", "", "", "", "", 0, 0, IssuesViewModel.IssuesDTO.createDefault());
        service.cfMobile = "9351234567";
        service.cfServiceType = 83;
        service.statusId = RequestStatus.SUSPENDED.getValue();
        storage.saveServiceStatus(service);

        DialogMatchApiInput input = DynamicDialogRequestFactory.fromStorage(context);

        assertEquals("09351234567", input.getMobileNumber());
        assertEquals("commercial", input.getPlan());
        assertEquals("inactive", input.getPlanStatus());
    }

    private static String expectedStore() {
        String store = BuildConfig.STORE != null ? BuildConfig.STORE.toLowerCase(Locale.US) : "";
        if ("cafebazaar".equals(store)) return "bazar";
        return store;
    }
}
