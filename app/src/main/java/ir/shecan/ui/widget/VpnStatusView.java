package ir.shecan.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import ir.shecan.R;
import ir.shecan.core.service.ShecanVpnService;
import ir.shecan.databinding.ViewServiceStatusBinding;

public class VpnStatusView extends FrameLayout {

    private ViewServiceStatusBinding binding;
    private RequestQueue queue;
    private String checkUrl;
    private static final String STATUS_REQUEST_TAG = "vpn_status_view";

    public VpnStatusView(Context context) {
        super(context);
        init(context, null);
    }

    public VpnStatusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public VpnStatusView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        binding = ViewServiceStatusBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
        );

        queue = Volley.newRequestQueue(context);

        // ✅ STATE INITIAL (مهم)
        showLoadingState();

        binding.btnRefresh.setOnClickListener(v -> checkStatus());
    }


    public void setCheckUrl(String url) {
        this.checkUrl = url;
        checkStatus();
    }

    public void checkStatus() {
        if (checkUrl == null) return;

        queue.cancelAll(STATUS_REQUEST_TAG);
        if (!ShecanVpnService.isActivated()) {
            stopLoadingAnimation();
            setDisconnected();
            return;
        }

        showLoadingState();
        startLoadingAnimation();

        StringRequest request = new StringRequest(
                Request.Method.GET,
                checkUrl,
                response -> {
                    stopLoadingAnimation();
                    if (ShecanVpnService.isActivated()) {
                        handleResponse(response.trim());
                    } else {
                        setDisconnected();
                    }
                },
                error -> {
                    stopLoadingAnimation();
                    setUnavailableStatus();
                }
        );
        request.setTag(STATUS_REQUEST_TAG);

//        request.setRetryPolicy(new DefaultRetryPolicy(
//                8000, // timeout ms (8 seconds)
//                1,    // retry count
//                1.0f
//        ));

        queue.add(request);
    }

    @Override
    protected void onDetachedFromWindow() {
        queue.cancelAll(STATUS_REQUEST_TAG);
        stopLoadingAnimation();
        super.onDetachedFromWindow();
    }


    private void handleResponse(String response) {
        switch (response) {
            case "2":
                setPro();
                break;
            case "0":
                setFree();
                break;
            default:
                setUnavailableStatus();
        }
    }

    private void setUnavailableStatus() {
        setDisconnected();
    }

    private void startLoadingAnimation() {
        binding.imgStatus.animate()
                .rotationBy(360f)
                .setDuration(800)
                .setInterpolator(new LinearInterpolator())
                .setListener(null)
                .start();
    }

    private void stopLoadingAnimation() {
        binding.imgStatus.animate().cancel();
        binding.imgStatus.setRotation(0f);
    }


    private void showLoadingState() {
        binding.rootLayout.setBackgroundResource(R.drawable.bg_gradient_disconnected);
        binding.txtStatus.setText("در حال بررسی وضعیت اتصال...");
        binding.imgStatus.setImageResource(0);
        binding.btnRefresh.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_refresh_circle));
    }

    private void setPro() {
        binding.rootLayout.setBackgroundResource(R.drawable.bg_gradient_pro);
        binding.txtStatus.setText("به شکن حرفه‌ای متصل هستید.");
        binding.btnRefresh.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_refresh_success_circle));
//        binding.imgStatus.setImageResource(R.drawable.ic_moon_not_selected);
    }

    private void setFree() {
        binding.rootLayout.setBackgroundResource(R.drawable.bg_gradient_pro);
        binding.txtStatus.setText("به شکن رایگان متصل هستید.");
        binding.btnRefresh.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_refresh_success_circle));
//        binding.imgStatus.setImageResource(R.drawable.ic_donate);
    }

    private void setDisconnected() {
        binding.rootLayout.setBackgroundResource(R.drawable.bg_gradient_disconnected);
        binding.txtStatus.setText("به شکن متصل نیستید!");
        binding.btnRefresh.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_refresh_circle));
//        binding.imgStatus.setImageResource(R.drawable.ic_done);
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
