package ir.shecan.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import ir.shecan.R;

public class ShadowLayout extends View {

    private int shadowColor = 0x55000000;
    private float shadowSize = 30f;     // مقدار blur
    private float shadowSpread = 0f;    // فاصله از داخل
    private float shadowRadius = 0f;    // گردی گوشه‌ها

    private Paint paint;
    private RectF rectF;

    public ShadowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ShadowLayout);
        shadowColor = a.getColor(R.styleable.ShadowLayout_shadowColor, shadowColor);
        shadowSize = a.getDimension(R.styleable.ShadowLayout_shadowSize, shadowSize);
        shadowSpread = a.getDimension(R.styleable.ShadowLayout_shadowSpread, shadowSpread);
        shadowRadius = a.getDimension(R.styleable.ShadowLayout_shadowRadius, shadowRadius);
        a.recycle();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        // paint شفاف می‌شود، سایه اعمال می‌شود
        paint.setColor(Color.TRANSPARENT);

        // سایه واقعی
        paint.setShadowLayer(shadowSize, 0, 0, shadowColor);

        rectF = new RectF();

        // حتماً برای سایه نرم
        setLayerType(LAYER_TYPE_SOFTWARE, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        rectF.set(
                shadowSpread,
                shadowSpread,
                getWidth() - shadowSpread,
                getHeight() - shadowSpread
        );

        canvas.drawRoundRect(rectF, shadowRadius, shadowRadius, paint);
    }
}


