package ir.shecan.ui.fragment.refactor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class LoginFragmentTest {

    private final LoginFragment fragment = new LoginFragment();

    @Test
    public void convertsPersianMobileNumberAndAddsLeadingZero() {
        assertEquals("09123456789", fragment.formatPhoneNumber("۹۱۲۳۴۵۶۷۸۹"));
    }

    @Test
    public void convertsArabicMobileNumberAndKeepsExistingLeadingZero() {
        assertEquals("09123456789", fragment.formatPhoneNumber("٠٩١٢٣٤٥٦٧٨٩"));
    }

    @Test
    public void keepsEmailAndTrimsWhitespace() {
        assertEquals("user12@example.com", fragment.formatPhoneNumber("  user۱۲@example.com  "));
        assertNull(fragment.formatPhoneNumber(null));
    }
}
