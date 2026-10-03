package ir.shecan.ui.dialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;

import ir.shecan.R;
import ir.shecan.data.modelDto.DynamicDialogViewModel;

@RunWith(RobolectricTestRunner.class)
public class DynamicAppDialogTest {

    @After
    public void tearDown() {
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Test
    public void nonDismissibleDialogHidesCloseAndDisablesCancel() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.AppTheme);
        DynamicDialogViewModel model = fromJson("{"
                + "\"dialog_id\":\"force-1\","
                + "\"title\":\"بروزرسانی اجباری\","
                + "\"message\":\"برای ادامه اپ را بروزرسانی کنید.\","
                + "\"is_active\":true,"
                + "\"is_dismissible\":false,"
                + "\"primary_button\":{\"title\":\"بروزرسانی\",\"redirect_link\":\"https://shecan.ir\"}"
                + "}");

        boolean shown = new DynamicAppDialog(activity, "09120000000").showIfValid(model);

        assertTrue(shown);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());
        assertFalse(((ShadowDialog) org.robolectric.Shadows.shadowOf(dialog)).isCancelable());
        TextView close = dialog.findViewById(R.id.closeBtn);
        assertNotNull(close);
        assertEquals(View.GONE, close.getVisibility());
    }

    @Test
    public void dismissibleDialogShowsCloseAndAllowsCancel() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.AppTheme);
        DynamicDialogViewModel model = fromJson("{"
                + "\"dialog_id\":\"optional-1\","
                + "\"title\":\"پیام\","
                + "\"message\":\"متن پیام\","
                + "\"is_active\":true,"
                + "\"is_dismissible\":true"
                + "}");

        boolean shown = new DynamicAppDialog(activity, "09120000000").showIfValid(model);

        assertTrue(shown);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());
        assertTrue(((ShadowDialog) org.robolectric.Shadows.shadowOf(dialog)).isCancelable());
        TextView close = dialog.findViewById(R.id.closeBtn);
        assertNotNull(close);
        assertEquals(View.VISIBLE, close.getVisibility());
    }

    @Test
    public void inactiveDialogIsNotShown() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.AppTheme);
        DynamicDialogViewModel model = fromJson("{"
                + "\"dialog_id\":\"inactive-1\","
                + "\"title\":\"پیام\","
                + "\"is_active\":false,"
                + "\"is_dismissible\":true"
                + "}");

        boolean shown = new DynamicAppDialog(activity, "09120000000").showIfValid(model);

        assertFalse(shown);
    }

    private DynamicDialogViewModel fromJson(String json) {
        return new Gson().fromJson(json, DynamicDialogViewModel.class);
    }
}
