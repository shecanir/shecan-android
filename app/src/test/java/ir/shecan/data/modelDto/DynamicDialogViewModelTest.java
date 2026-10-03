package ir.shecan.data.modelDto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

public class DynamicDialogViewModelTest {

    @Test
    public void parsesDocumentResponseFields() {
        String json = "{"
                + "\"dialog_id\":\"renew-1\","
                + "\"title\":\"تمدید اشتراک\","
                + "\"message\":\"اشتراک شما تا ۳ روز دیگر منقضی می‌شود.\","
                + "\"primary_button\":{\"title\":\"تمدید اشتراک\",\"redirect_link\":\"https://my.shecan.ir/panel\"},"
                + "\"secondary_button\":{\"title\":\"بعدا\",\"redirect_link\":\"\"},"
                + "\"time_to_show\":15,"
                + "\"priority\":20,"
                + "\"is_active\":true,"
                + "\"is_dismissible\":false"
                + "}";

        DynamicDialogViewModel model = new Gson().fromJson(json, DynamicDialogViewModel.class);

        assertEquals("renew-1", model.getDialogId());
        assertEquals("تمدید اشتراک", model.getTitle());
        assertEquals("اشتراک شما تا ۳ روز دیگر منقضی می‌شود.", model.getMessage());
        assertEquals("تمدید اشتراک", model.getPrimaryButton().getTitle());
        assertEquals("https://my.shecan.ir/panel", model.getPrimaryButton().getRedirectLink());
        assertEquals("بعدا", model.getSecondaryButton().getTitle());
        assertEquals(15_000L, model.getTimeToShowMs());
        assertEquals(Integer.valueOf(20), model.getPriority());
        assertTrue(model.isActive());
        assertFalse(model.isDismissible());
        assertTrue(model.hasContent());
    }

    @Test
    public void supportsAlternateCamelCaseFields() {
        String json = "{"
                + "\"dialogId\":\"id-2\","
                + "\"isActive\":true,"
                + "\"isDismissible\":true,"
                + "\"timeToShow\":2500,"
                + "\"primaryButton\":{\"title\":\"Open\",\"redirectLink\":\"https://shecan.ir\"}"
                + "}";

        DynamicDialogViewModel model = new Gson().fromJson(json, DynamicDialogViewModel.class);

        assertEquals("id-2", model.getDialogId());
        assertTrue(model.isActive());
        assertTrue(model.isDismissible());
        assertEquals(2500L, model.getTimeToShowMs());
        assertEquals("Open", model.getPrimaryButton().getTitle());
        assertEquals("https://shecan.ir", model.getPrimaryButton().getRedirectLink());
    }
}
