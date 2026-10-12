package ir.shecan.ui.fragment.refactor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.app.Application;
import android.os.Bundle;
import android.os.Parcel;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import org.junit.After;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import ir.shecan.R;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.ui.fragment.bottomSheet.SubscriptionBottomSheet;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = Application.class, sdk = 28)
public class FragmentRecreationTest {
    public static class RecreationActivity extends FragmentActivity {
        @Override protected void onCreate(Bundle state) {
            setTheme(R.style.AppTheme);
            super.onCreate(state);
        }
    }

    private ActivityController<RecreationActivity> controller;

    private <T extends Fragment> T restore(T original) {
        controller = Robolectric.buildActivity(RecreationActivity.class).setup();
        controller.get().getSupportFragmentManager().beginTransaction()
                .add(original, "restored")
                .setMaxLifecycle(original, Lifecycle.State.CREATED).commitNow();
        Bundle state = new Bundle();
        controller.pause().saveInstanceState(state).stop().destroy();
        // Parcel the whole activity state as Android does across process death.
        Parcel parcel = Parcel.obtain();
        parcel.writeBundle(state);
        parcel.setDataPosition(0);
        state = parcel.readBundle(original.getClass().getClassLoader());
        parcel.recycle();
        controller = Robolectric.buildActivity(RecreationActivity.class).setup(state);
        @SuppressWarnings("unchecked")
        T restored = (T) controller.get().getSupportFragmentManager().findFragmentByTag("restored");
        return restored;
    }

    @After public void tearDown() {
        if (controller != null) controller.pause().stop().destroy();
    }

    private Object field(Fragment fragment, String name) throws Exception {
        Field field = fragment.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(fragment);
    }

    @Test
    public void restoresOtpIdentifierAndAccountMode() throws Exception {
        OtpFragment fragment = restore(OtpFragment.newInstance("09120000000", false));
        assertEquals("09120000000", field(fragment, "identifier"));
        assertFalse((Boolean) field(fragment, "isExist"));
    }

    @Test
    public void restoresPasswordIdentifier() throws Exception {
        PasswordFragment fragment = restore(PasswordFragment.newInstance("user@example.com"));
        assertEquals("user@example.com", field(fragment, "identifier"));
    }

    @Test
    public void restoresSubscriptionAndRenewalOrder() throws Exception {
        ServiceItem item = new ServiceItem("306814", "silver", "1m", "active", "link", 1, 2, null);
        item.id = 306814;
        item.cfServiceType = 1;
        item.cfDuration = 17;
        item.dueDate = "2030-01-01";
        SubscriptionBottomSheet sheet = restore(SubscriptionBottomSheet.newInstance(item));
        ServiceItem restored = (ServiceItem) field(sheet, "item");
        assertEquals(item.id, restored.id);
        assertEquals(item.cfServiceType, restored.cfServiceType);
        assertEquals(item.cfDuration, restored.cfDuration);
        assertEquals(item.dueDate, restored.dueDate);
        assertEquals(item.getOrderCode(), restored.getOrderCode());
    }
}
