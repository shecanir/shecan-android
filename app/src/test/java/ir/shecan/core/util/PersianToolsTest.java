package ir.shecan.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PersianToolsTest {

    @Test
    public void convertsPersianAndArabicDigitsToEnglish() {
        assertEquals(
                "09123456789",
                PersianTools.convertToEnglishDigits("۰۹۱۲۳۴۵۶۷۸۹")
        );
        assertEquals(
                "09123456789",
                PersianTools.convertToEnglishDigits("٠٩١٢٣٤٥٦٧٨٩")
        );
        assertNull(PersianTools.convertToEnglishDigits(null));
    }

    @Test
    public void extractsOtpDigitsFromMixedText() {
        assertEquals("123456", PersianTools.extractEnglishDigits("کد: ۱۲۳-٤٥٦"));
        assertEquals("", PersianTools.extractEnglishDigits(null));
    }
}
