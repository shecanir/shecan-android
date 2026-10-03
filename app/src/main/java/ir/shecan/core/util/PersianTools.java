package ir.shecan.core.util;

public class PersianTools {
    private PersianTools() {
    }

    public static String convertToPersianDigits(String input) {
        if (input == null) return null;

        char[] persianDigits = {'۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'};
        StringBuilder persianString = new StringBuilder();

        for (char c : input.toCharArray()) {
            int digit = Character.digit(c, 10);
            if (digit >= 0) {
                persianString.append(persianDigits[digit]);
            } else {
                persianString.append(c);
            }
        }
        if (LanguageHelper.getLanguage().equals("fa"))
            return persianString.toString();

        return input;
    }

    public static String convertToEnglishDigits(String input) {
        if (input == null) return null;

        StringBuilder englishString = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            int digit = Character.digit(c, 10);
            if (digit >= 0) {
                englishString.append((char) ('0' + digit));
            } else {
                englishString.append(c);
            }
        }
        return englishString.toString();
    }

    public static String extractEnglishDigits(String input) {
        String normalized = convertToEnglishDigits(input);
        if (normalized == null) return "";

        StringBuilder digits = new StringBuilder(normalized.length());
        for (char c : normalized.toCharArray()) {
            if (c >= '0' && c <= '9') digits.append(c);
        }
        return digits.toString();
    }
}
