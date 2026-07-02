package ir.shecan.ui.fragment.refactor;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import ir.shecan.R;
import ir.shecan.core.util.AppUtils;
import ir.shecan.core.util.ToastManager;
import ir.shecan.core.util.TrackingUtils;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.databinding.FragmentPasswordBinding;
import ir.shecan.data.modelDto.SendOtpApiViewModel;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;

public class PasswordFragment extends Fragment {

    private final String identifier;

    public PasswordFragment(String identifier) {
        this.identifier = identifier;
    }

    private FragmentPasswordBinding binding;

    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable typingStoppedRunnable;
    boolean isEyePressed = false;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentPasswordBinding.inflate(inflater, container, false);

        binding.iconBackImg.setOnClickListener(view -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        binding.bottomFrameLayout.setOnClickListener(v -> {
            AppUtils.hideKeyboard(getActivity());
        });

        binding.iconBackImg.setAlpha(0f);
        binding.iconBackImg.animate()
                .alpha(1f)
                .setDuration(1000)
                .start();

        setupTypingAnimation();

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

        binding.btnContinue.setOnClickListener(view -> {

            showLoading(true);
            TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_LOGIN_PASSWORD_ATTEMPT);

            AuthApi auth = new AuthApi(requireContext());
            auth.login(
                    identifier,
                    binding.edtPassword.getText().toString(),
                    new ApiCallback<VerifyApiViewModel>() {
                        @Override
                        public void onSuccess(VerifyApiViewModel res, boolean fromCache) {
                            showLoading(false);
                            if (res != null) {
                                AppStorage storage = new AppStorage(getContext());
                                storage.prepareServiceSelectionAfterLogin();
                                storage.saveToken(res);
                                TrackingUtils.setUserId(requireContext(), String.valueOf(res.getId()));
                                TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_LOGIN_PASSWORD_SUCCESS);
                                if (!res.getMail().contains("shecan.fake")) {
                                    getActivity().finish();
                                } else {
                                    getActivity().getSupportFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.fragmentContainer, new SignUpFragment())
                                            .addToBackStack(null)
                                            .commit();
                                }
                            }
                        }

                        @Override
                        public void onError(int statusCode, String message) {
                            showLoading(false);
                            if (isAdded() && message != null && !message.isEmpty()) {
                                ToastManager.show(getContext(), message);
                            }
                        }
                    }
            );
        });

        binding.btnContinueWithOtpCode.setOnClickListener(view -> {
            TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_LOGIN_OTP_REQUEST,
                    TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "password_screen"));
//            AuthApi auth = new AuthApi(requireContext());
//            auth.sendOtp(identifier, new ApiCallback<SendOtpApiViewModel>() {
//                @Override
//                public void onSuccess(SendOtpApiViewModel data, boolean fromCache) {
//
//                }
//
//                @Override
//                public void onError(int statusCode, String message) {
////                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
//                }
//            });

            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new OtpFragment(identifier, true))
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnEye.setOnTouchListener((v, event) -> {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    isEyePressed = true;
                    onSensitiveAction();

                    binding.edtPassword.setTransformationMethod(
                            android.text.method.HideReturnsTransformationMethod.getInstance()
                    );
                    forceLtr(binding.edtPassword);
                    binding.btnEye.setImageResource(R.drawable.ic_eye_open);
                    binding.edtPassword.setSelection(binding.edtPassword.length());
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isEyePressed = false;
                    binding.edtPassword.setTransformationMethod(
                            android.text.method.PasswordTransformationMethod.getInstance()
                    );
                    forceLtr(binding.edtPassword);
                    binding.btnEye.setImageResource(R.drawable.ic_eye_close);

                    // ⏱ بعد از رها کردن، برگرده نرمال
                    typingHandler.postDelayed(typingStoppedRunnable, 300);
                    binding.edtPassword.setSelection(binding.edtPassword.length());
                    return true;
            }
            return false;
        });



        return binding.getRoot();
    }

    private void forceLtr(EditText editText) {
        editText.setTextDirection(View.TEXT_DIRECTION_LTR);
        editText.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        editText.setGravity(Gravity.START);
        editText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
    }

    private void setupTypingAnimation() {

        typingStoppedRunnable = () -> {
            binding.mascot.setImageResource(R.drawable.ic_shecan_normal);
        };

        binding.edtPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                binding.mascot.setImageResource(R.drawable.ic_shecan_normal);
            }
        });

        binding.edtPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isEyePressed) return;
                onSensitiveAction();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isEyePressed) return;
                typingHandler.postDelayed(typingStoppedRunnable, 500);
            }
        });
    }

    private void onSensitiveAction() {
        binding.mascot.setImageResource(R.drawable.ic_shecan_hide_eye);
        typingHandler.removeCallbacks(typingStoppedRunnable);
    }

    private void showLoading(boolean loading) {
        if (loading) {
            binding.btnContinue.setEnabled(false);
            binding.btnContinue.setAlpha(0.5f);
            binding.progressVerify.setVisibility(View.VISIBLE);
            binding.btnContinue.setText("");
        } else {
            binding.btnContinue.setEnabled(true);
            binding.btnContinue.setAlpha(1f);
            binding.progressVerify.setVisibility(View.GONE);
            binding.btnContinue.setText(ContextCompat.getString(getContext(), R.string.login));
        }
    }
}
