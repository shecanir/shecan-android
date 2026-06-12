package ir.shecan.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import ir.shecan.R;
import ir.shecan.databinding.ButtonLayoutBinding;

public class PowerButtonView extends ConstraintLayout {

    public enum State {
        NORMAL,
        LOADING,
        CONNECTED
    }

    private ButtonLayoutBinding binding;
    private State currentState = State.NORMAL;

    private OnClickListener externalClickListener = null;

    public PowerButtonView(Context context) {
        super(context);
        init(context);
    }

    public PowerButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PowerButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        this.externalClickListener = l;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context) {
        binding = ButtonLayoutBinding.inflate(LayoutInflater.from(context), this, true);

        setClickable(true);
        setFocusable(true);

        // مخفی بودن لودینگ در ابتدا
        binding.loadingCircle.setVisibility(View.GONE);
        binding.loadingCircle.setCircleSizePercent(0.85f);

        // هندل کردن لمس
        setOnTouchListener((v, event) -> {

//            if (currentState == State.LOADING) {
//                return true; // بلاک کامل
//            }

            if (currentState == State.CONNECTED) {
                return false; // کلیک عبور کند → بدون انیمیشن
            }

            // NORMAL → انیمیشن scale فعال
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    scaleDown();
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    scaleUp();
                    break;
            }

            return false;
        });

        // مدیریت کلیک
        super.setOnClickListener(v -> {
            if (externalClickListener == null) return;

            //if (currentState == State.LOADING) return;

            // NORMAL و CONNECTED هر دو اجازه کلیک دارند
            externalClickListener.onClick(v);
        });

        applyState();
    }

    // -----------------------------------------------------
    // STATE HANDLING
    // -----------------------------------------------------

    public void setState(State state) {
        currentState = state;
        applyState();
    }

    public State getState() {
        return currentState;
    }

    private void applyState() {
        switch (currentState) {

            case NORMAL:
                setEnabled(true);
                setClickable(true);

                binding.loadingCircle.stop();
                binding.loadingCircle.setVisibility(View.GONE);

                scaleUp();
                break;

            case LOADING:
                setEnabled(true);
                setClickable(true);

                scaleUp();

                binding.loadingCircle.setVisibility(View.VISIBLE);
                binding.loadingCircle.setColor(ContextCompat.getColor(getContext(), R.color.connectionButtonProgressBar));
                binding.loadingCircle.start();
                break;

            case CONNECTED:
                setEnabled(true);
                setClickable(true);

                scaleDown();

                binding.loadingCircle.setVisibility(View.VISIBLE);
                binding.loadingCircle.stop();
                binding.loadingCircle.setProgress(100);
                binding.loadingCircle.setColor(ContextCompat.getColor(getContext(), R.color.connectionButtonConnectedBar));
                break;
        }
    }

    // -----------------------------------------------------
    // SCALE ANIMATION
    // -----------------------------------------------------

    private void scaleDown() {
        binding.circle.setScaleX(0.92f);
        binding.circle.setScaleY(0.92f);

        binding.power.setScaleX(0.88f);
        binding.power.setScaleY(0.88f);
    }

    private void scaleUp() {
        binding.circle.setScaleX(1f);
        binding.circle.setScaleY(1f);

        binding.power.setScaleX(1f);
        binding.power.setScaleY(1f);
    }

    // -----------------------------------------------------
    // SHORTCUT HELPERS
    // -----------------------------------------------------

    public void showLoading(boolean show) {
        setState(show ? State.LOADING : State.NORMAL);
    }

    public boolean isLoading() {
        return currentState == State.LOADING;
    }

    public void setConnected(boolean connected) {
        setState(connected ? State.CONNECTED : State.NORMAL);
    }

    public boolean isConnected() {
        return currentState == State.CONNECTED;
    }
}
