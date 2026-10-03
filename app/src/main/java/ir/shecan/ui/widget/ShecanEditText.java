package ir.shecan.ui.widget;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ShecanEditText extends androidx.appcompat.widget.AppCompatEditText implements TextWatcher {

    private static final int TypingInterval = 3000;
    private OnTypingModified typingChangedListener;
    private boolean currentTypingState = false;
    private final Handler handler = new Handler();

    private final Runnable stoppedTypingNotifier = new Runnable() {
        @Override
        public void run() {
            //part A of the magic...
            if (null != typingChangedListener) {
                typingChangedListener.onIsTypingModified(ShecanEditText.this, false);
                currentTypingState = false;
            }
        }
    };

    public interface OnTypingModified {
        void onIsTypingModified(ShecanEditText view, boolean isTyping);
    }

    public ShecanEditText(@NonNull Context context) {
        super(context);
        run(context);
        this.addTextChangedListener(this);
    }

    public ShecanEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        run(context);
        this.addTextChangedListener(this);
    }

    public ShecanEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        run(context);
        this.addTextChangedListener(this);
    }

    private void run(Context context) {
        try {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize());
        } catch (Exception e) {

        }
    }

//    @Override
//    public void setTextSize(int unit, float size) {
//        try {
//            super.setTextSize(unit, size * AppController.getInstance().userData.getTextSize());
//        } catch (Exception e) {
//
//        }
//    }

    public void setOnTypingModified(OnTypingModified typingChangedListener) {
        this.typingChangedListener = typingChangedListener;
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (null != typingChangedListener) {
            if (!currentTypingState) {
                typingChangedListener.onIsTypingModified(this, true);
                currentTypingState = true;
            }

            handler.removeCallbacks(stoppedTypingNotifier);
            handler.postDelayed(stoppedTypingNotifier, TypingInterval);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }


    @Override
    public void onTextChanged(CharSequence text, int start, int before, int after) {

    }
}
