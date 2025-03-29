package ir.shecan.util;

public class PersianTools {
    public static String convertToPersianDigits(String input) {
        char[] persianDigits = {'۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'};
        StringBuilder persianString = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                persianString.append(persianDigits[c - '0']); // Map English digit to Persian digit
            } else {
                persianString.append(c); // Keep non-digit characters as is (e.g., ':')
            }
        }
        if(LanguageHelper.getLanguage().equals("fa"))
            return persianString.toString();

        return input;
    }
}
