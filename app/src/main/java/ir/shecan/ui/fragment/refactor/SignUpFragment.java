package ir.shecan.ui.fragment.refactor;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
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
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.databinding.FragmentSignUpBinding;
import ir.shecan.data.modelDto.EmptyResponse;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;

public class SignUpFragment extends Fragment {

    private FragmentSignUpBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSignUpBinding.inflate(inflater, container, false);

        getActivity().finish();

//        AppStorage storage = new AppStorage(getContext());
//        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
//
//        binding.isCompanyCB.setChecked(false);
//        binding.edtPersianName.setText("");
//        binding.edtPersianFamilyName.setText("");
//        binding.edtEmail.setText("");
//
//        binding.iconBackImg.setOnClickListener(view -> {
//            requireActivity().getOnBackPressedDispatcher().onBackPressed();
//        });
//
//        binding.bottomFrameLayout.setOnClickListener(v -> {
//            AppUtils.hideKeyboard(getActivity());
//        });
//
//        binding.isCompanyCB.setOnCheckedChangeListener((compoundButton, isChecked) -> {
//            binding.edtCompanyName.setVisibility(isChecked ? VISIBLE : GONE);
//        });
//
//        binding.btnPassword.setOnClickListener(view -> {
//            showLoading(true);
//            AuthApi auth = new AuthApi(requireContext());
//            auth.updateProfile(
//                    token.getApiKey(),
//                    binding.edtPersianName.getText().toString(),
//                    binding.edtPersianFamilyName.getText().toString(),
//                    binding.isCompanyCB.isChecked() ? binding.edtCompanyName.getText().toString() : null,
//                    binding.edtEmail.getText().toString(),
//                    null,
//                    new ApiCallback<EmptyResponse>() {
//                        @Override
//                        public void onSuccess(EmptyResponse data, boolean fromCache) {
//                            showLoading(false);
//                            getActivity().getSupportFragmentManager()
//                                    .beginTransaction()
//                                    .replace(R.id.fragmentContainer, new ChangePasswordFragment())
//                                    .addToBackStack(null)
//                                    .commit();
//                        }
//
//                        @Override
//                        public void onError(int statusCode, String message) {
//                            showLoading(false);
//                            if (isAdded() && message != null && !message.isEmpty()) {
//                                Toast.makeText(
//                                        requireContext(),
//                                        message,
//                                        Toast.LENGTH_SHORT
//                                ).show();
//                            }
//                        }
//                    }
//            );
//        });
//
//        binding.btnLogin.setOnClickListener(view -> {
//
//            String name = binding.edtPersianName.getText().toString().trim();
//            String family = binding.edtPersianFamilyName.getText().toString().trim();
//
//            if (name.isEmpty() || family.isEmpty()) {
//                Toast.makeText(getContext(), R.string.nameAndFamilyNameIsRequired, Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            if (!isPersianText(name) || !isPersianText(family)) {
//                Toast.makeText(getContext(), R.string.nameAndFamilyNameMustPersian, Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            showLoading(true);
//            AuthApi auth = new AuthApi(requireContext());
//
//            auth.updateProfile(
//                    token.getApiKey(),
//                    name,
//                    family,
//                    binding.isCompanyCB.isChecked() ? binding.edtCompanyName.getText().toString() : null,
//                    binding.edtEmail.getText().toString(),
//                    null,
//                    new ApiCallback<EmptyResponse>() {
//                        @Override
//                        public void onSuccess(EmptyResponse data, boolean fromCache) {
//                            showLoading(false);
//                            getActivity().finish();
//                        }
//
//                        @Override
//                        public void onError(int statusCode, String message) {
//                            showLoading(false);
//                            if (isAdded() && message != null && !message.isEmpty()) {
//                                Toast.makeText(
//                                        requireContext(),
//                                        message,
//                                        Toast.LENGTH_SHORT
//                                ).show();
//                            }
//                        }
//                    }
//            );
//        });
//
//
//        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
//
//            if (!isAdded() || getContext() == null) return;
//
//            int screenHeight = binding.getRoot().getHeight();
//
//            int fiftyDp = (int) TypedValue.applyDimension(
//                    TypedValue.COMPLEX_UNIT_DIP,
//                    170,
//                    getResources().getDisplayMetrics()
//            );
//
//            int finalHeight = screenHeight - fiftyDp;
//
//            RelativeLayout.LayoutParams params =
//                    (RelativeLayout.LayoutParams) binding.bottomFrameLayout.getLayoutParams();
//
//            params.height = finalHeight;
//            binding.bottomFrameLayout.setLayoutParams(params);
//        });

        return binding.getRoot();
    }

    private void showLoading(boolean loading) {
        if (loading) {
            binding.btnLogin.setEnabled(false);
            binding.btnLogin.setAlpha(0.5f);
            binding.progress.setVisibility(VISIBLE);
            binding.btnLogin.setText("");
        } else {
            binding.btnLogin.setEnabled(true);
            binding.btnLogin.setAlpha(1f);
            binding.progress.setVisibility(GONE);
            binding.btnLogin.setText(ContextCompat.getString(getContext(), R.string.login));
        }
    }

    private boolean isPersianText(String text) {
        return text.matches("^[\\u0600-\\u06FF\\s]+$");
    }
}
