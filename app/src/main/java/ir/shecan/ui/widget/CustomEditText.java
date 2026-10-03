package ir.shecan.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Field;

import ir.shecan.R;

public class CustomEditText extends LinearLayout implements TextWatcher, View.OnClickListener {

    private final static int DARK = 1;
    private final static int LIGHT = 2;
    private final static int ERROR = 3;
    private final static int DISABLED = 4;
    private final static int IMAGE_ID_LEFT = 1;
    private final static int IMAGE_ID_RIGHT = 2;

    private CustomEditTextInterface customEditTextInterface;

    private final RelativeLayout relativeLayout = new RelativeLayout(getContext());
    private final ShecanEditText editText = new ShecanEditText(getContext());
    private final AppCompatImageView imageViewLeft = new AppCompatImageView(getContext());
    private final AppCompatImageView imageViewRight = new AppCompatImageView(getContext());
    private final ShecanTextView errorTv = new ShecanTextView(getContext());
    private final ShecanTextView labelTv = new ShecanTextView(getContext());
    private String text;
    private String label;
    private int labelBackground;
    private String hint;
    private Integer textColor;
    private Integer textColorHint;
    private Integer inputType = InputType.TYPE_CLASS_TEXT;
    private Integer maxLength = 500;
    private Integer gravity = Gravity.START;
    private Integer maxLines = 10;
    private Integer imeOptions = EditorInfo.IME_ACTION_NEXT;
    private Integer theme = LIGHT;
    private Integer imageLeft = 0;
    private Integer imageRight = 0;
    private Typeface typeface;
    private boolean isEnabled = true;
    private boolean hasError = true;
    private boolean numberSpeller = false;
    private boolean isPassShow;
    private float textSize = dpToPx(14);
    private final Integer paddingRight = dpToPx(14);
    private final Integer paddingLeft = dpToPx(14);
    private final Integer paddingTop = dpToPx(14);
    private final Integer paddingBottom = dpToPx(14);

    public CustomEditText(Context context) {
        super(context);
        init();
    }

    public CustomEditText(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setAttributes(context, attrs);
        init();
    }

    public CustomEditText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setAttributes(context, attrs);
        init();
    }

    public CustomEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setAttributes(context, attrs);
        init();
    }

    private void setAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomEditText);
        try {
            text = typedArray.getString(R.styleable.CustomEditText_android_text);
            label = typedArray.getString(R.styleable.CustomEditText_label);
            hint = typedArray.getString(R.styleable.CustomEditText_android_hint);

            theme = typedArray.getInt(R.styleable.CustomEditText_themeEditTex, theme);

            labelBackground = typedArray.getColor(R.styleable.CustomEditText_labelBackground, getLabelBackgroundColor(theme));

            textColor = typedArray.getColor(R.styleable.CustomEditText_android_textColor, getTextColor(theme));
            textColorHint = typedArray.getColor(R.styleable.CustomEditText_android_textColorHint, getTextColorHint(theme));

            imageLeft = typedArray.getResourceId(R.styleable.CustomEditText_imageLeft, imageLeft);
            imageRight = typedArray.getResourceId(R.styleable.CustomEditText_imageRight, imageRight);

            textSize = typedArray.getDimensionPixelSize(R.styleable.CustomEditText_android_textSize, (int) textSize);

            inputType = typedArray.getInt(R.styleable.CustomEditText_android_inputType, inputType);
            imeOptions = typedArray.getInt(R.styleable.CustomEditText_android_imeOptions, imeOptions);
            gravity = typedArray.getInt(R.styleable.CustomEditText_android_gravity, gravity);
            maxLines = typedArray.getInt(R.styleable.CustomEditText_android_lines, maxLines);
            maxLength = typedArray.getInt(R.styleable.CustomEditText_android_maxLength, maxLength);

            isEnabled = typedArray.getBoolean(R.styleable.CustomEditText_android_enabled, isEnabled);
            hasError = typedArray.getBoolean(R.styleable.CustomEditText_hasError, hasError);
            numberSpeller = typedArray.getBoolean(R.styleable.CustomEditText_numberSpeller, numberSpeller);

        } finally {
            typedArray.recycle();
        }
    }

    public void init() {

//        if (!isInEditMode())
//            typeface = ResourcesCompat.getFont(getContext(), R.font.iran_sans);

        setOrientation(LinearLayout.VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        initEditText();
        initImageViewLeft();
        initImageViewRight();
        initLabel();
        initError();

        /*Add Views*/
        relativeLayout.addView(editText);

        if (imageLeft != 0)
            relativeLayout.addView(imageViewLeft);

        if (imageRight != 0)
            relativeLayout.addView(imageViewRight);

        if (label != null) {
            relativeLayout.addView(labelTv);
            addView(new ShecanTextView(getContext()));
        }

        setClipChildren(false);
        addView(relativeLayout);
        if (hasError)
            addView(errorTv);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Adjust width as necessary
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int mBoundedWidth = dpToPx(320);
        if (mBoundedWidth < measuredWidth) {
            int measureMode = MeasureSpec.getMode(widthMeasureSpec);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(mBoundedWidth, measureMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean isInEditMode() {
        return super.isInEditMode();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (numberSpeller) {
//            try {
//                if (inputType == InputType.TYPE_CLASS_NUMBER && editText.getText().toString().length() > 0) {
//                    if (editText.getText().toString().length() == 1) {
//                        Long l = Long.parseLong(editText.getText().toString().replaceAll(",", ""));
//                        String amount = PersianNumberToWord.onWork(new BigDecimal(l), AppController.getInstance().userData.getCurrency());
//                        errorTv.setText(amount.trim());
//                        errorTv.setTextColor(getTextColorHint(theme));
//                    } else {
//                        Long l = Long.parseLong(editText.getText().toString().replaceAll(",", "").substring(0, editText.getText().toString().replaceAll(",", "").length() - 1));
//                        String amount = PersianNumberToWord.onWork(new BigDecimal(l), "تومان");
//                        errorTv.setText(amount.trim());
//                        errorTv.setTextColor(getTextColorHint(theme));
//                    }
//                } else {
//                    clearError();
//                }
//            } catch (Exception e) {
//                clearError();
//            }
        } else {
            errorTv.setTextColor(getTextColor(ERROR));
            clearError();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case IMAGE_ID_LEFT:
                if (editText.getInputType() == 129)
                    if (isPassShow) {
                        isPassShow = false;
                        editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        editText.setSelection(getLength());
                    } else {
                        isPassShow = true;
                        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        editText.setSelection(getLength());
                    }
                if (customEditTextInterface != null)
                    customEditTextInterface.leftIconListener();
                break;
            case IMAGE_ID_RIGHT:
                customEditTextInterface.rightIconListener();
                break;
        }
    }

    /*Setters=====================================================================================*/

    public void setHint(String hint) {
        this.hint = hint;
        editText.setHint(hint);

        clearError();
    }

    public void setError(String error) {
        if (hasError) {
            editText.setTextColor(getTextColor(ERROR));
            editText.setHintTextColor(getTextColorHint(ERROR));
            editText.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            relativeLayout.setBackground(getBackgroundEdt(ERROR));

            errorTv.setText(error);
            errorTv.setPadding(dpToPx(8), 0, dpToPx(8), 0);
        }
    }

    public void setIconListener(CustomEditTextInterface customEditTextInterface) {
        this.customEditTextInterface = customEditTextInterface;
    }

    public ShecanEditText getEditText() {
        return editText;
    }

    /*Getters=====================================================================================*/

    public ImageView getRightIcon() {
        return imageViewRight;
    }

    public ImageView getLeftIcon() {
        return imageViewLeft;
    }

    public String getText() {
        return editText.getText().toString();
    }

    public void setText(String text) {
        this.text = text;
        editText.setText(text);
    }

    public Integer getLength() {
        return editText.getText().toString().trim().length();
    }

    public boolean isEmpty() {
        return editText.getText().toString().trim().isEmpty();
    }

    /*Methods=====================================================================================*/

    private void initEditText() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT
                , RelativeLayout.LayoutParams.WRAP_CONTENT);
        editText.setLayoutParams(params);
        editText.setText(text);
        editText.setTextColor(textColor);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        editText.setHint(hint);
        editText.setHintTextColor(textColorHint);
        editText.setInputType(inputType);
        editText.setImeOptions(imeOptions);
        editText.setGravity(gravity);
        editText.setMaxLines(maxLines);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        editText.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        editText.setBackgroundColor(Color.TRANSPARENT);
        if (!isInEditMode())
            editText.setTypeface(typeface);
        editText.setTextDirection(TEXT_DIRECTION_ANY_RTL);
        editText.addTextChangedListener(this);
        editText.measure(0, 0);

        if (imageLeft != 0)
            params.addRule(RelativeLayout.START_OF, IMAGE_ID_LEFT);

        if (imageRight != 0)
            params.addRule(RelativeLayout.END_OF, IMAGE_ID_RIGHT);

        if (inputType == InputType.TYPE_CLASS_PHONE)
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        if (!isEnabled) {
            editText.setEnabled(false);
            relativeLayout.setBackground(getBackgroundEdt(DISABLED));
        } else
            relativeLayout.setBackground(getBackgroundEdt(theme));
        editText.setLayoutParams(params);

        editText.setTypeface(editText.getTypeface(), Typeface.BOLD);

    }

    private void initImageViewLeft() {
        if (imageLeft != 0) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT
                    , RelativeLayout.LayoutParams.WRAP_CONTENT);

            imageViewLeft.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageViewLeft.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            imageViewLeft.measure(0, 0);
            imageViewLeft.setId(IMAGE_ID_LEFT);

            if (imageLeft != 0) {
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.addRule(RelativeLayout.CENTER_VERTICAL);
                params.setMarginEnd(paddingLeft);
                imageViewLeft.setImageResource(imageLeft);
            }

            imageViewLeft.setOnClickListener(this);
            imageViewLeft.setLayoutParams(params);
        }
    }

    private void initImageViewRight() {
        if (imageRight != 0) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT
                    , RelativeLayout.LayoutParams.WRAP_CONTENT);

            imageViewRight.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageViewRight.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            imageViewRight.measure(0, 0);
            imageViewRight.setId(IMAGE_ID_RIGHT);

            if (imageRight != 0) {
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.addRule(RelativeLayout.CENTER_VERTICAL);
                params.setMarginStart(paddingRight);
                imageViewRight.setImageResource(imageRight);
            }
            imageViewRight.setOnClickListener(this);
            imageViewRight.setLayoutParams(params);
        }
    }

    private void initLabel() {
        if (label != null) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT
                    , RelativeLayout.LayoutParams.WRAP_CONTENT);

            labelTv.setTextColor(textColor);
            labelTv.setText(label);
            labelTv.setPadding(paddingLeft, 0, paddingRight, 0);
            labelTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            labelTv.setBackgroundColor(labelBackground);
            if (!isInEditMode())
                labelTv.setTypeface(typeface);

            labelTv.measure(0, 0);
            params.rightMargin = paddingRight;
            params.topMargin = (labelTv.getMeasuredHeight() / 2 + 5) * -1;
            labelTv.setLayoutParams(params);
        }
    }

    private void initError() {
        errorTv.setTextColor(ContextCompat.getColor(getContext(), R.color.red));
//        errorTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.smallFontSize));
        if (!isInEditMode())
            errorTv.setTypeface(typeface);
        errorTv.measure(0, 0);
    }

    private Drawable getBackgroundEdt(Integer type) {
        /*Set Background for ShecanEditText*/
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        switch (type) {
            case DARK:
                drawable.setStroke(dpToPx(1), ContextCompat.getColor(getContext(), R.color.black));
                setCursorColor(R.color.black);
                break;
            case LIGHT:
                drawable.setStroke(dpToPx(1), ContextCompat.getColor(getContext(), R.color.white));
                setCursorColor(R.color.white);
                break;
            case ERROR:
                drawable.setStroke(dpToPx(1), ContextCompat.getColor(getContext(), R.color.red));
                setCursorColor(R.color.red);
                break;
            case DISABLED:
                drawable.setStroke(dpToPx(1), ContextCompat.getColor(getContext(), R.color.dividerColor));
                break;
        }
        drawable.setCornerRadius(dpToPx(8));
        drawable.setColor(Color.TRANSPARENT);
        return drawable;
    }

    public Integer getTextColor(Integer theme) {
        switch (theme) {
            case DARK:
                return ContextCompat.getColor(getContext(), R.color.black);
            case LIGHT:
                return ContextCompat.getColor(getContext(), R.color.primaryTextColor);
            case ERROR:
                return ContextCompat.getColor(getContext(), R.color.red);
            case DISABLED:
                return ContextCompat.getColor(getContext(), R.color.dividerColor);
            default:
                return ContextCompat.getColor(getContext(), R.color.primaryTextColor);
        }
    }

    public Integer getTextColorHint(Integer theme) {
        switch (theme) {
            case DARK:
                return ContextCompat.getColor(getContext(), R.color.greenSecondaryTextColor);
            case LIGHT:
                return ContextCompat.getColor(getContext(), R.color.greenSecondaryTextColor);
            case ERROR:
                return ContextCompat.getColor(getContext(), R.color.red);
            case DISABLED:
                return ContextCompat.getColor(getContext(), R.color.dividerColor);
            default:
                return ContextCompat.getColor(getContext(), R.color.greenSecondaryTextColor);
        }
    }

    public Integer getLabelBackgroundColor(Integer theme) {
        switch (theme) {
            case DARK:
                return ContextCompat.getColor(getContext(), R.color.authorizeBackgroundColor);
            case LIGHT:
                return ContextCompat.getColor(getContext(), R.color.authorizeBackgroundColor);
            default:
                return ContextCompat.getColor(getContext(), R.color.authorizeBackgroundColor);
        }
    }

    public void setLabel(String label) {
        labelTv.setText(label);
    }

    public void setImageLeftDrawable(Drawable drawable) {
        imageViewLeft.setImageDrawable(drawable);
    }

    public void setImageRightDrawable(Drawable drawable) {
        imageViewRight.setImageDrawable(drawable);
    }

    private void setCursorColor(int color) {
        try {
            // Get the cursor resource id
            Field field = ShecanTextView.class.getDeclaredField("mCursorDrawableRes");
            field.setAccessible(true);
            int drawableResId = field.getInt(editText);

            // Get the editor
            field = ShecanTextView.class.getDeclaredField("mEditor");
            field.setAccessible(true);
            Object editor = field.get(editText);

            // Get the drawable and set a color filter
            Drawable drawable = ContextCompat.getDrawable(editText.getContext(), drawableResId);
            drawable.setColorFilter(ContextCompat.getColor(getContext(), color), PorterDuff.Mode.SRC_IN);
            Drawable[] drawables = {drawable, drawable};

            // Set the drawables
            field = editor.getClass().getDeclaredField("mCursorDrawable");
            field.setAccessible(true);
            field.set(editor, drawables);
        } catch (Exception ignored) {
        }
    }

    public void clearInput() {
        editText.setText("");

        clearError();
    }

    public void clearError() {
        if (hasError) {
            editText.setTextColor(getTextColor(theme));
            editText.setHintTextColor(getTextColorHint(theme));
            editText.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
            relativeLayout.setBackground(getBackgroundEdt(theme));

            errorTv.setText("");
            errorTv.setPadding(dpToPx(8), 0, dpToPx(8), 0);
        }
    }

    /*Interface===================================================================================*/
    public interface CustomEditTextInterface {
        void leftIconListener();

        void rightIconListener();
    }

    public static int dpToPx(float dp) {
        return (int) ((dp) * Resources.getSystem().getDisplayMetrics().density);
    }

}