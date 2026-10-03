package ir.shecan.ui.widget.rateHelper;

public class RatingState {

    public long firstOpenTime;          // زمان اولین باز شدن اپ
    public long lastPromptDismissTime;  // آخرین باری که پیام بسته شده
    public long lastRatingTime;         // آخرین ثبت امتیاز
    public int lastRatingValue;         // آخرین امتیاز (1-5)
    public boolean hasRatedInStore;     // آیا رفته استور یا نه

}