package ir.shecan.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class LoadingCircleView extends View {

    private Paint paint;
    private RectF rectF;

    private static final float DEFAULT_SWEEP_ANGLE = 90f;

    private float sweepAngle = DEFAULT_SWEEP_ANGLE;       // مقدار پیشفرض
    private final float startAngle = 225f;
    private float rotation = 0f;
    private final int strokeWidth = 8;

    private float circleSizePercent = 1.0f;

    private boolean isSpinning = true;   // حالت چرخش فعال

    public LoadingCircleView(Context context) {
        super(context);
        init();
    }

    public LoadingCircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        paint = new Paint();
        paint.setColor(0xFFFFFFFF);   // سفید
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setAntiAlias(true);

        rectF = new RectF();

        // شروع پیش‌فرض چرخش
        post(rotationRunnable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float padding = strokeWidth / 2f;
        float width = getWidth() * circleSizePercent;
        float height = getHeight() * circleSizePercent;

        float left = (getWidth() - width) / 2f + padding;
        float top = (getHeight() - height) / 2f + padding;
        float right = left + width - padding * 2;
        float bottom = top + height - padding * 2;

        rectF.set(left, top, right, bottom);

        canvas.drawArc(rectF, startAngle + rotation, sweepAngle, false, paint);
    }

    // انیمیشن چرخش
    private final Runnable rotationRunnable = new Runnable() {
        @Override
        public void run() {

            if (isSpinning) {
                rotation += 5;
                if (rotation >= 360) rotation -= 360;
                invalidate();
            }

            postDelayed(this, 16); // 60fps
        }
    };

    // ----------- کنترل‌ها -----------

    // شروع چرخش
    public void start() {
        sweepAngle = DEFAULT_SWEEP_ANGLE;
        isSpinning = true;
        invalidate();
    }

    // توقف چرخش
    public void stop() {
        isSpinning = false;
        rotation = 0f;
        invalidate();
    }

    // تغییر رنگ
    public void setColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    // تنظیم درصد پیشرفت (0 تا 100)
    public void setProgress(int percent) {
        sweepAngle = (360f * percent) / 100f;
        invalidate();
    }

    // مخصوص حالت Connected
    public void setConnectedState() {
        stop();
        setProgress(100);
        setColor(0x16C385); // سبز
    }

    public void setCircleSizePercent(float percent) {
        circleSizePercent = percent;
        invalidate();
    }
}
