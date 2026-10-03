package ir.shecan.ui.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static ir.shecan.core.util.AppUtils.adjustUIForFragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.R;
import ir.shecan.ui.adapter.ThemeAdapter;
import ir.shecan.core.constant.Constant;
import ir.shecan.databinding.ActivityThemeBinding;
import ir.shecan.data.modelDto.AppConfig;
import ir.shecan.data.modelDto.ThemeItem;
import ir.shecan.data.storage.AppStorage;

public class ThemeActivity extends AppCompatActivity {

    private ActivityThemeBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityThemeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        adjustUi();
        setupRecycler();
        vipClickHandler();
    }

    private void adjustUi() {
        binding.toolbar.back.setVisibility(VISIBLE);
        binding.toolbar.back.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        adjustUIForFragment(this, R.color.mainBack, R.color.mainBack);
        binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.mainBack));

        binding.toolbar.toolbarLogo.setVisibility(GONE);
        binding.toolbar.toolbarTitle.setVisibility(VISIBLE);
        binding.toolbar.toolbarTitle.setText("حالت نمایش");
    }

    private void vipClickHandler() {
        binding.toolbar.vip.setVisibility(Constant.IsSiteMode? VISIBLE : GONE);
        binding.toolbar.vip.setOnClickListener(view -> startActivity(new Intent(this, BillingPlansActivity.class)));
    }

    private void setupRecycler() {

        List<ThemeItem> list = new ArrayList<>();
        list.add(new ThemeItem(getString(R.string.systemSetting), AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        list.add(new ThemeItem(getString(R.string.lightMode), AppCompatDelegate.MODE_NIGHT_NO));
        list.add(new ThemeItem(getString(R.string.darkMode), AppCompatDelegate.MODE_NIGHT_YES));

        int mode = AppCompatDelegate.MODE_NIGHT_NO;
        AppStorage appStorage = new AppStorage(getApplicationContext());
        AppConfig appConfig = appStorage.getAppConfig(AppConfig.class);
        if(appConfig != null){
            mode = appConfig.getMode();
        }

        ThemeAdapter adapter = new ThemeAdapter(this, mode, list, item -> {
            saveThemeMode(item.getMode());
            AppCompatDelegate.setDefaultNightMode(item.getMode());
            updateThemeWithoutRecreate();
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void updateThemeWithoutRecreate() {
        ViewGroup root = (ViewGroup) binding.getRoot().getParent();

        if (root != null) {
            root.removeAllViews();

            LayoutInflater inflater = LayoutInflater.from(this);
            binding = ActivityThemeBinding.inflate(inflater, root, false);

            root.addView(binding.getRoot());

            setupRecycler();

            recreate();

            // اگر Toolbar داخل MainActivity بود:
            // ((MainActivityNew) this).refreshToolbarTheme();
        }
    }

    private void saveThemeMode(int mode) {
        AppStorage appStorage = new AppStorage(getApplicationContext());
        appStorage.saveAppConfig(new AppConfig(mode));

//        getSharedPreferences("settings", MODE_PRIVATE)
//                .edit()
//                .putInt("theme_mode", mode)
//                .apply();
    }
}
