package ir.shecan.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import ir.shecan.R;
import ir.shecan.databinding.ActivityAuthorizeBinding;
import ir.shecan.ui.fragment.refactor.LoginFragment;

public class AuthorizeActivity extends AppCompatActivity {

    private ActivityAuthorizeBinding binding;

    public boolean isShowBackButton = true;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle bundle = intent.getExtras();
        if(bundle != null){
            isShowBackButton = bundle.getBoolean("isShowBackButton" , true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.setStatusBarColor(getColor(R.color.authorizeBackgroundColor));
        }

        binding = ActivityAuthorizeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getApplicationContext().setTheme(R.style.AppTheme);

        // Load first fragment
        loadFragment(new LoginFragment(), false);
    }

    // ---------------- Fragment Loader ----------------
    public void loadFragment(Fragment fragment, boolean addToBackstack) {
        if (addToBackstack) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }
}
