package ir.shecan.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import ir.shecan.R;

public class ProfileTabBar extends LinearLayout {

    LinearLayout tab1, tab2;
    FrameLayout circle1, circle2;
    ImageView profile, password;
    TextView text1, text2;

    private OnProfileTabSelected listener;

    public ProfileTabBar(Context context) {
        super(context);
        init(context);
    }

    public ProfileTabBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ProfileTabBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_profile_tab_bar, this, true);

        tab1 = findViewById(R.id.tab1);
        tab2 = findViewById(R.id.tab2);

        circle1 = findViewById(R.id.circle1);
        circle2 = findViewById(R.id.circle2);

        profile = findViewById(R.id.icProfile);
        password = findViewById(R.id.icPassword);

        text1 = findViewById(R.id.text1);
        text2 = findViewById(R.id.text2);

        tab1.setOnClickListener(v -> {
            selectTab(1);
            if (listener != null) listener.onTabSelected(1);
        });

        tab2.setOnClickListener(v -> {
            selectTab(2);
            if (listener != null) listener.onTabSelected(2);
        });

        selectTab(1); // پیش‌فرض
    }

    public void selectTab(int index) {

        if (index == 1) {
            tab1.setBackgroundResource(R.drawable.tab_selected);
            circle1.setBackgroundResource(R.drawable.circle_selected);
            text1.setTextColor(ContextCompat.getColor(getContext(), R.color.black));

            tab2.setBackgroundResource(R.drawable.tab_unselected);
            circle2.setBackgroundResource(R.drawable.circle_unselected);
            text1.setTextColor(ContextCompat.getColor(getContext(), R.color.black));

            profile.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            password.setColorFilter(ContextCompat.getColor(getContext(), R.color.black));

        } else {
            tab2.setBackgroundResource(R.drawable.tab_selected);
            circle2.setBackgroundResource(R.drawable.circle_selected);
            text1.setTextColor(ContextCompat.getColor(getContext(), R.color.black));

            tab1.setBackgroundResource(R.drawable.tab_unselected);
            circle1.setBackgroundResource(R.drawable.circle_unselected);
            text1.setTextColor(ContextCompat.getColor(getContext(), R.color.black));

            password.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            profile.setColorFilter(ContextCompat.getColor(getContext(), R.color.black));
        }
    }

    public void setOnProfileTabSelected(OnProfileTabSelected listener) {
        this.listener = listener;
    }

    public interface OnProfileTabSelected {
        void onTabSelected(int index);
    }
}