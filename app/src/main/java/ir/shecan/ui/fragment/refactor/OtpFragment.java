package ir.shecan.ui.fragment.refactor;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionInflater;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.tasks.Task;
import ir.shecan.R;
import ir.shecan.core.receiver.OtpReceiver;
import ir.shecan.core.util.AppUtils;
import ir.shecan.core.util.ToastManager;
import ir.shecan.core.util.TrackingUtils;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.databinding.FragmentOtpBinding;
import ir.shecan.data.modelDto.SendOtpApiViewModel;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;

public class OtpFragment extends Fragment {

    private final String identifier;
    private final boolean isExist;

    private OtpReceiver otpReceiver;

    private boolean isSendingOtp = false;
    private boolean isVerifyingOtp = false;
    private boolean suppressOtpChanges = false;



    public OtpFragment(String identifier, boolean isExist) {
        this.identifier = identifier;
        this.isExist = isExist;
    }

    private FragmentOtpBinding binding;
    EditText[] otpFields = new EditText[6];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentOtpBinding.inflate(inflater, container, false);

        binding.agreementView.setupText("https://shecan.ir/terms/");

        binding.otpLayout.otpBackground.setOnClickListener(v -> {
            AppUtils.hideKeyboard(getActivity());
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setSharedElementEnterTransition(
                    TransitionInflater.from(getContext())
                            .inflateTransition(R.transition.change_bounds)
            );
            getActivity().getWindow().setSharedElementReturnTransition(
                    TransitionInflater.from(getContext())
                            .inflateTransition(R.transition.change_bounds)
            );
        }

        startSmsListener();

        otpReceiver = new OtpReceiver();
        otpReceiver.setListener(otp -> {
            autoFillOtp(otp);
        });

        otpFields[0] = binding.otpLayout.otp1;
        otpFields[1] = binding.otpLayout.otp2;
        otpFields[2] = binding.otpLayout.otp3;
        otpFields[3] = binding.otpLayout.otp4;
        otpFields[4] = binding.otpLayout.otp5;
        otpFields[5] = binding.otpLayout.otp6;

        setupOtpFields();
        startTimer();

        binding.otpLayout.btnVerify.setOnClickListener(v -> validateOtp());
        binding.otpLayout.tvResend.setOnClickListener(v -> startTimer());

        binding.iconBackImg.setOnClickListener(view -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        binding.iconBackImg.setAlpha(0f);
        binding.iconBackImg.animate()
                .alpha(1f)
                .setDuration(1000)
                .start();


        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(() -> {

            if (!isAdded() || getContext() == null) return;

            int screenHeight = binding.getRoot().getHeight();

            int fiftyDp = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    170,
                    getResources().getDisplayMetrics()
            );

            int finalHeight = screenHeight - fiftyDp;

            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) binding.bottomFrameLayout.getLayoutParams();

            params.height = finalHeight;
            binding.bottomFrameLayout.setLayoutParams(params);
        });


        return binding.getRoot();
    }

    private void setupOtpFields() {

        for (int i = 0; i < otpFields.length; i++) {
            int index = i;

            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (suppressOtpChanges) return;

                    String digits = s.toString().replaceAll("\\D+", "");
                    if (digits.length() > 1) {
                        fillOtpFrom(index, digits);
                        return;
                    }

                    if (!digits.equals(s.toString())) {
                        setOtpField(index, digits);
                        return;
                    }

                    // وقتی یک رقم زده شد → برو فیلد بعد
                    if (digits.length() == 1) {
                        if (index < otpFields.length - 1) {
                            otpFields[index + 1].requestFocus();
                        }

                        // اگر فیلد آخر پر شد → اتوماتیک verify
                        if (index == otpFields.length - 1 && isOtpComplete()) {
                            validateOtp();
                        }
                    }
                }

            });


            otpFields[i].setOnKeyListener((v, keyCode, event) -> {

                if (event.getAction() != android.view.KeyEvent.ACTION_DOWN)
                    return false;

                if (keyCode == android.view.KeyEvent.KEYCODE_DEL) {

                    if (otpFields[index].getText().length() == 0) {
                        // اگر خالی بود → برو قبلی
                        if (index > 0) {
                            otpFields[index - 1].setText("");
                            otpFields[index - 1].requestFocus();
                        }
                    } else {
                        // اگر داخلش کاراکتر بود → خالی کن ولی فوکوس همینجا بماند
                        otpFields[index].setText("");
                    }

                    return true;
                }

                return false;
            });
        }
    }

    private void fillOtpFrom(int startIndex, String rawDigits) {
        String digits = rawDigits == null ? "" : rawDigits.replaceAll("\\D+", "");
        if (digits.isEmpty()) return;

        suppressOtpChanges = true;
        try {
            int target = Math.max(0, Math.min(startIndex, otpFields.length - 1));
            for (int i = 0; i < digits.length() && target < otpFields.length; i++, target++) {
                otpFields[target].setText(String.valueOf(digits.charAt(i)));
            }
            int focusIndex = Math.min(target, otpFields.length - 1);
            otpFields[focusIndex].requestFocus();
        } finally {
            suppressOtpChanges = false;
        }

        if (isOtpComplete()) {
            validateOtp();
        }
    }

    private void setOtpField(int index, String value) {
        suppressOtpChanges = true;
        try {
            otpFields[index].setText(value);
        } finally {
            suppressOtpChanges = false;
        }
    }

    private boolean isOtpComplete() {
        for (EditText otpField : otpFields) {
            if (otpField.getText() == null || otpField.getText().length() != 1) {
                return false;
            }
        }
        return true;
    }

    private void startSmsListener() {
        SmsRetrieverClient client = SmsRetriever.getClient(requireActivity());
        Task<Void> task = client.startSmsRetriever();

        task.addOnSuccessListener(aVoid -> {
            // Listener successfully started
        });

        task.addOnFailureListener(e -> {
            // Failed to start
        });
    }

    private void startTimer() {

        if (isSendingOtp) return;
        isSendingOtp = true;

        AuthApi auth = new AuthApi(requireContext());
        TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_LOGIN_OTP_REQUEST,
                TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "otp_timer"));
        auth.sendOtp(identifier, new ApiCallback<SendOtpApiViewModel>() {
            @Override
            public void onSuccess(SendOtpApiViewModel data, boolean fromCache) {
                isSendingOtp = false;
            }

            @Override
            public void onError(int statusCode, String message) {
                isSendingOtp = false;
            }
        });

        binding.otpLayout.tvResend.setVisibility(TextView.GONE);
        binding.otpLayout.tvTimer.setVisibility(TextView.VISIBLE);

        new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                binding.otpLayout.tvTimer.setText("ارسال مجدد کد در " + millisUntilFinished / 1000 + " ثانیه");
            }

            public void onFinish() {
                binding.otpLayout.tvTimer.setVisibility(TextView.GONE);
                binding.otpLayout.tvResend.setVisibility(TextView.VISIBLE);
            }
        }.start();
    }

    private void validateOtp() {

        if (isVerifyingOtp) return;

        StringBuilder codeBuilder = new StringBuilder();

        for (EditText otpField : otpFields) {
            if (otpField.getText().length() == 0) {
                shakeError(getString(R.string.codeIsUnCompleted));
                return;
            }
            codeBuilder.append(otpField.getText());
        }

        isVerifyingOtp = true;
        showLoading(true);
        TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_LOGIN_OTP_VERIFY_ATTEMPT);

        String code = codeBuilder.toString();
        AuthApi auth = new AuthApi(requireContext());

        if (isExist) {
            verifyNormal(auth, code);
        } else {
            verifyObjectWithFallback(auth, code);
        }
    }

    private void verifyObjectWithFallback(AuthApi auth, String code) {

        auth.verifyOtpObject(identifier, code, new ApiCallback<VerifyApiViewModel>() {

            @Override
            public void onSuccess(VerifyApiViewModel res, boolean fromCache) {

                if (res != null) {
                    handleVerifySuccess(res, false);
                } else {
                    verifyNormal(auth, code); // fallback
                }
            }

            @Override
            public void onError(int statusCode, String message) {
                verifyNormal(auth, code); // fallback
            }
        });
    }

    private void verifyNormal(AuthApi auth, String code) {

        auth.verifyOtp(identifier, code, new ApiCallback<VerifyApiViewModel>() {

            @Override
            public void onSuccess(VerifyApiViewModel res, boolean fromCache) {

                if (res != null) {
                    handleVerifySuccess(res, true);
                } else {
                    handleVerifyError(getString(R.string.codeIsWrong));
                }
            }

            @Override
            public void onError(int statusCode, String message) {
                handleVerifyError(message);
            }
        });
    }

    private void handleVerifySuccess(VerifyApiViewModel res, boolean isNormal) {

        isVerifyingOtp = false;
        showLoading(false);

        AppStorage storage = new AppStorage(getContext());
        storage.prepareServiceSelectionAfterLogin();
        storage.saveToken(res);
        TrackingUtils.setUserId(requireContext(), String.valueOf(res.getId()));
        TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_LOGIN_OTP_SUCCESS,
                TrackingUtils.bundleOf(TrackingUtils.PARAM_METHOD, isNormal ? "normal" : "object"));

        if (isNormal) {

            if (!res.getMail().contains("shecan.fake")) {
                requireActivity().finish();
                return;
            }
        }

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new SignUpFragment())
                .addToBackStack(null)
                .commit();
    }

    private void handleVerifyError(String message) {

        isVerifyingOtp = false;
        showLoading(false);

        if (message == null || message.isEmpty()) {
            shakeError(getString(R.string.codeIsWrong));
        } else if (isAdded()) {
            ToastManager.show(getContext(), message);
        }
    }

    private void callNormalVerify(AuthApi auth, String code) {

        auth.verifyOtp(identifier, code, new ApiCallback<VerifyApiViewModel>() {

            @Override
            public void onSuccess(VerifyApiViewModel res, boolean fromCache) {
                isVerifyingOtp = false;
                showLoading(false);

                if (res != null) {
                    AppStorage storage = new AppStorage(getContext());
                    storage.prepareServiceSelectionAfterLogin();
                    storage.saveToken(res);

                    if (!res.getMail().contains("shecan.fake")) {
                        getActivity().finish();
                    } else {
                        getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragmentContainer, new SignUpFragment())
                                .addToBackStack(null)
                                .commit();
                    }
                } else {
                    shakeError(getString(R.string.codeIsWrong));
                }
            }

            @Override
            public void onError(int statusCode, String message) {
                isVerifyingOtp = false;
                showLoading(false);

                if (isAdded() && message != null && !message.isEmpty()) {
                    ToastManager.show(getContext(), message);
                }
            }
        });
    }

    private void shakeError(String msg) {
        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
        binding.otpLayout.otpContainer.startAnimation(shake);

        binding.otpLayout.tvErrorOtp.setText(msg);
        binding.otpLayout.tvErrorOtp.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean loading) {
        if (loading) {
            binding.otpLayout.btnVerify.setEnabled(false);
            binding.otpLayout.btnVerify.setAlpha(0.5f);
            binding.progressVerify.setVisibility(View.VISIBLE);
            binding.otpLayout.btnVerify.setText("");
        } else {
            binding.otpLayout.btnVerify.setEnabled(true);
            binding.otpLayout.btnVerify.setAlpha(1f);
            binding.progressVerify.setVisibility(View.GONE);
            binding.otpLayout.btnVerify.setText("تایید");
        }
    }

    private void autoFillOtp(String otp) {
        fillOtpFrom(0, otp);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(
                    otpReceiver,
                    filter,
                    Context.RECEIVER_EXPORTED
            );
        } else {
            ContextCompat.registerReceiver(requireActivity(), otpReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        requireActivity().unregisterReceiver(otpReceiver);

    }
}
