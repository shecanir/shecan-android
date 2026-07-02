package ir.shecan.ui.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static ir.shecan.core.util.AppUtils.adjustUIForFragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import ir.shecan.R;
import ir.shecan.core.util.ToastManager;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.core.constant.Constant;
import ir.shecan.databinding.ActivityUpdatePrrofileBinding;
import ir.shecan.data.modelDto.AccountViewModel;
import ir.shecan.data.modelDto.EmptyResponse;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.core.util.AppUtils;

public class UpdateProfileActivity extends AppCompatActivity {

    private ActivityUpdatePrrofileBinding binding;
    private AppStorage storage;
    private int currentPosition = 1;
    private VerifyApiViewModel token;
    private AuthApi authApi;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityUpdatePrrofileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initDependencies();
        initViews();
        initListeners();
    }

    private void initDependencies() {
        storage = new AppStorage(getApplicationContext());
        token = storage.getToken(VerifyApiViewModel.class);
        authApi = new AuthApi(getApplicationContext());
    }

    private void initViews() {
        binding.toolbar.back.setVisibility(VISIBLE);
        binding.edtPersianName.setText(token.getFirstname());
        binding.edtPersianFamilyName.setText(token.getLastname());
        binding.edtEmail.setText(token.getMail());
        binding.edtPhoneNumber.setText(token.getLogin());
        handleTabChange(1);

        adjustUIForFragment(this, R.color.mainBack, R.color.mainBack);
        binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.mainBack));

        binding.toolbar.toolbarLogo.setVisibility(GONE);
        binding.toolbar.toolbarTitle.setVisibility(VISIBLE);
        binding.toolbar.toolbarTitle.setText("حساب کاربری");
    }

    private void initListeners() {

        binding.toolbar.back.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        binding.btnCancel.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        binding.btnSave.setOnClickListener(v -> {
            if (currentPosition == 1) {
                updateProfile();
            } else {
                updatePassword();
            }
        });

        binding.profileTabBar.setOnProfileTabSelected(index -> {
            currentPosition = index;
            handleTabChange(index);
        });

        binding.profileTab.setOnClickListener(v -> {
            AppUtils.hideKeyboard(this);
        });

        binding.passwordTab.setOnClickListener(v -> {
            AppUtils.hideKeyboard(this);
        });

        binding.toolbar.vip.setVisibility(Constant.IsSiteMode? VISIBLE : GONE);
        binding.toolbar.vip.setOnClickListener(view -> startActivity(new Intent(this, BillingPlansActivity.class)));

    }

    private void handleTabChange(int index) {
        boolean isProfileTab = index == 1;

        binding.profileTab.setVisibility(isProfileTab ? VISIBLE : GONE);
        binding.passwordTab.setVisibility(isProfileTab ? GONE : VISIBLE);
    }

    private void updatePassword() {
        String newPass = binding.edtPasswordNew.getText().toString();
        String repeat = binding.edtPasswordNewRepeat.getText().toString();

        if (!newPass.equals(repeat)) {
            ToastManager.show(this, getString(R.string.passworsNotSame));
            return;
        }

        showLoading(true);

        authApi.updatePassword(
                token.getApiKey(),
                newPass,
                new ApiCallback<EmptyResponse>() {
                    @Override
                    public void onSuccess(EmptyResponse data, boolean fromCache) {
                        showLoading(false);
                        updateUserInformation();
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        if (statusCode == -2) {
                            updateUserInformation();
                        } else {
                            showLoading(false);
                            ToastManager.show(getApplicationContext(), message);
                        }
                    }
                }
        );
    }

    private void updateProfile() {

        showLoading(true);

        String firstname = binding.edtPersianName.getText().toString();
        String lastname = binding.edtPersianFamilyName.getText().toString();
        String mail = binding.edtEmail.getText().toString();
        String phoneNumber = binding.edtPhoneNumber.getText().toString();

        authApi.updateProfile(
                token.getApiKey(),
                firstname,
                lastname,
                null,
                mail,
                phoneNumber,
                new ApiCallback<EmptyResponse>() {
                    @Override
                    public void onSuccess(EmptyResponse data, boolean fromCache) {
                        showLoading(false);
                        token.setFirstname(firstname);
                        token.setLastname(lastname);
                        token.setMail(mail);
                        token.setLogin(phoneNumber);
                        storage.saveToken(token);
                        storage.markProfileUpdated();
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        if (statusCode == -2) {
                            updateUserInformation();
                        } else {
                            showLoading(false);
                            ToastManager.show(getApplicationContext(), message);
                        }
                    }
                }
        );
    }

    private void updateUserInformation() {

        showLoading(true);

        authApi.me(
                token.getApiKey(),
                new ApiCallback<AccountViewModel>() {
                    @Override
                    public void onSuccess(AccountViewModel res, boolean fromCache) {
                        showLoading(false);

                        if (res != null && res.getUser() != null) {

                            // update token fields
                            token.setFirstname(res.getUser().getFirstname());
                            token.setLastname(res.getUser().getLastname());
                            token.setMail(res.getUser().getMail());

                            // save both
                            storage.saveToken(token);

                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        showLoading(false);
                        ToastManager.show(getApplicationContext(), message);
                    }
                }
        );
    }

    private void showLoading(boolean loading) {
        binding.btnSave.setEnabled(!loading);
        binding.btnSave.setAlpha(loading ? 0.5f : 1f);
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);

        binding.btnSave.setText(
                loading ? "" : ContextCompat.getString(this, R.string.saveChange)
        );
    }
}
