package ir.shecan.ui.fragment.refactor;

import static android.view.View.GONE;

import android.os.Build;
import android.os.Bundle;
import android.transition.TransitionInflater;
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
import ir.shecan.core.util.PersianTools;
import ir.shecan.core.util.ToastManager;
import ir.shecan.core.util.TrackingUtils;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.databinding.ActivityAuthorizeBinding;
import ir.shecan.databinding.FragmentLoginBinding;
import ir.shecan.data.modelDto.ExistApiViewModel;
import ir.shecan.ui.activity.AuthorizeActivity;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentLoginBinding.inflate(inflater, container, false);

        binding.agreementView.setupText("https://shecan.ir/");

        binding.bottomFrameLayout.setOnClickListener(v -> {
            AppUtils.hideKeyboard(getActivity());
        });

        binding.edtPhoneNumber.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                binding.btnContinue.performClick();
                return true;
            }
            return false;
        });

        binding.btnContinue.setOnClickListener(v -> {

            showLoading(true);
            TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_LOGIN_CONTINUE_CLICK);

            String inputNumber = binding.edtPhoneNumber.getText().toString();
            String identifier = formatPhoneNumber(inputNumber);
            AuthApi auth = new AuthApi(requireContext());
            auth.exists(
                    identifier,
                    new ApiCallback<ExistApiViewModel>() {
                        @Override
                        public void onSuccess(ExistApiViewModel res, boolean fromCache) {
                            if (res != null) {
                                android.os.Bundle params = new android.os.Bundle();
                                TrackingUtils.put(params, TrackingUtils.PARAM_HAS_ACCOUNT, res.getExists());
                                TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_LOGIN_IDENTIFIER_EXISTS, params);

                                if (res.getExists()) {
                                    showLoading(false);
                                    goToLoginWithPasswordFragment();
                                } else {
                                    showLoading(false);
                                    goToLoginWithOtpFragment();
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

        AuthorizeActivity activity = (AuthorizeActivity) getActivity();
        if(activity != null && !activity.isShowBackButton){
            binding.iconBackImg.setVisibility(GONE);
        }

        binding.iconBackImg.setOnClickListener(view -> {
            getActivity().finish();
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

    private void goToLoginWithOtpFragment() {
        String inputNumber = binding.edtPhoneNumber.getText().toString();
        String identifier = formatPhoneNumber(inputNumber);

        OtpFragment otpFragment = new OtpFragment(identifier, false);
        route(otpFragment);
    }

    private void goToLoginWithPasswordFragment() {
        String inputNumber = binding.edtPhoneNumber.getText().toString();
        String identifier = formatPhoneNumber(inputNumber);

        PasswordFragment otpFragment = new PasswordFragment(identifier);
        route(otpFragment);
    }

    private void route(Fragment fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fragment.setSharedElementEnterTransition(
                    TransitionInflater.from(getContext())
                            .inflateTransition(R.transition.change_bounds)
            );
            fragment.setSharedElementReturnTransition(
                    TransitionInflater.from(getContext())
                            .inflateTransition(R.transition.change_bounds)
            );
        }

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .addSharedElement(binding.iconToolbar, "toolbar_logo")
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
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
            binding.progress.setVisibility(GONE);
            binding.btnContinue.setText(ContextCompat.getString(getContext(), R.string.continuee));
        }
    }

    public String formatPhoneNumber(String phoneNumber) {
        String normalized = PersianTools.convertToEnglishDigits(phoneNumber);
        if (normalized != null) normalized = normalized.trim();

        if (normalized != null && normalized.matches("[0-9]{10}")) {
            return "0" + normalized;
        }
        return normalized;
    }
}
