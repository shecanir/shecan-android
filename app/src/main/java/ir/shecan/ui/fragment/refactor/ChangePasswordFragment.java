package ir.shecan.ui.fragment.refactor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import ir.shecan.R;
import ir.shecan.core.util.AppUtils;
import ir.shecan.core.util.ToastManager;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.databinding.FragmentChangePasswordBinding;
import ir.shecan.data.modelDto.EmptyResponse;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;

public class ChangePasswordFragment extends Fragment {

    private FragmentChangePasswordBinding binding;

    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable typingStoppedRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);

        binding.iconBackImg.setOnClickListener(view -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        binding.bottomFrameLayout.setOnClickListener(v -> {
            AppUtils.hideKeyboard(getActivity());
        });

        setupTypingAnimation();

        AppStorage storage = new AppStorage(getContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);

        binding.btnContinue.setOnClickListener(view -> {
            if (!binding.edtPassword.getText().toString().equals(binding.edtRepeatPassword.getText().toString())) {
                ToastManager.show(getContext(), getString(R.string.passworsNotSame));
                return;
            }
            showLoading(true);
            AuthApi auth = new AuthApi(requireContext());
            auth.updatePassword(
                    token.getApiKey(),
                    binding.edtPassword.getText().toString(),
                    new ApiCallback<EmptyResponse>() {
                        @Override
                        public void onSuccess(EmptyResponse data, boolean fromCache) {
                            showLoading(false);
                            getActivity().finish();
                        }

                        @Override
                        public void onError(int statusCode, String message) {
                            showLoading(false);
                            ToastManager.show(getContext(), message);

                        }
                    }
            );
        });

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

    private void setupTypingAnimation() {

        typingStoppedRunnable = () -> {
            binding.mascot.setImageResource(R.drawable.ic_shecan_normal);
        };

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (!hasFocus) {
                binding.mascot.setImageResource(R.drawable.ic_shecan_normal);
            }
        };

        View.OnKeyListener typingListener = (v, keyCode, event) -> {
            binding.mascot.setImageResource(R.drawable.ic_shecan_hide_eye);
            typingHandler.removeCallbacks(typingStoppedRunnable);
            typingHandler.postDelayed(typingStoppedRunnable, 500);

            return false;
        };

        binding.edtPassword.setOnKeyListener(typingListener);
        binding.edtRepeatPassword.setOnKeyListener(typingListener);

        binding.edtPassword.setOnFocusChangeListener(focusListener);
        binding.edtRepeatPassword.setOnFocusChangeListener(focusListener);
    }

    private void showLoading(boolean loading) {
        if (loading) {
            binding.btnContinue.setEnabled(false);
            binding.btnContinue.setAlpha(0.5f);
            binding.progress.setVisibility(View.VISIBLE);
            binding.btnContinue.setText("");
        } else {
            binding.btnContinue.setEnabled(true);
            binding.btnContinue.setAlpha(1f);
            binding.progress.setVisibility(View.GONE);
            binding.btnContinue.setText(ContextCompat.getString(getContext(), R.string.login));
        }
    }
}
