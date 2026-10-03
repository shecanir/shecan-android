package ir.shecan.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

import ir.shecan.R;

public class CustomButton extends AppCompatButton {

    public enum Type { PRIMARY, SECONDARY }

    private int primaryBg;
    private int primaryText;
    private int secondaryBg;
    private int secondaryBorder;
    private int secondaryText;
    private float radius;

    private int xmlTextColor;

    private int paddingAll;
    private int paddingVertical;
    private int paddingHorizontal;

    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadAttrs(context, attrs);
        init();
    }

    private void loadAttrs(Context context, AttributeSet attrs) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomButton);

        int typeValue = a.getInt(R.styleable.CustomButton_type, 0);

        primaryBg = a.getColor(R.styleable.CustomButton_cb_primaryBackground, 0xFF0BA36A);
        primaryText = a.getColor(R.styleable.CustomButton_cb_primaryTextColor, 0xFFFFFFFF);

        secondaryBg = a.getColor(R.styleable.CustomButton_cb_secondaryBackground, 0x00000000);
        secondaryBorder = a.getColor(R.styleable.CustomButton_cb_secondaryBorderColor, 0xFF0BA36A);
        secondaryText = a.getColor(R.styleable.CustomButton_cb_secondaryTextColor, 0xFF0BA36A);

        radius = a.getDimension(R.styleable.CustomButton_cb_cornerRadius, 40f);

        xmlTextColor = a.getColor(R.styleable.CustomButton_cb_textColor, -1);

        paddingAll = (int) a.getDimension(R.styleable.CustomButton_cb_padding, -1);
        paddingVertical = (int) a.getDimension(R.styleable.CustomButton_cb_paddingVertical, -1);
        paddingHorizontal = (int) a.getDimension(R.styleable.CustomButton_cb_paddingHorizontal, -1);

        a.recycle();

        setTag(typeValue == 0 ? Type.PRIMARY : Type.SECONDARY);
    }

    private void init() {

        Type type = (Type) getTag();

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(radius);

        if (type == Type.PRIMARY) {
            shape.setColor(primaryBg);
            shape.setStroke(0, 0);
        } else {
            shape.setColor(secondaryBg);
            shape.setStroke(3, secondaryBorder);
        }

        RippleDrawable ripple = new RippleDrawable(
                ColorStateList.valueOf(0x00000000), // بدون سایه
                shape,
                null
        );

        setBackground(ripple);
        setAllCaps(false);

        // لغو ارتفاع پیش‌فرض
        setMinHeight(0);
        setMinimumHeight(0);

        applyPadding();
        applyTextColor(type);
    }

    private void applyPadding() {
        int finalV = paddingVertical != -1 ? paddingVertical :
                paddingAll != -1 ? paddingAll : 20;

        int finalH = paddingHorizontal != -1 ? paddingHorizontal :
                paddingAll != -1 ? paddingAll : 20;

        setPadding(finalH, finalV, finalH, finalV);
    }

    private void applyTextColor(Type type) {
        if (xmlTextColor != -1) {
            setTextColor(xmlTextColor);
            return;
        }
        setTextColor(type == Type.PRIMARY ? primaryText : secondaryText);
    }
}
