package ir.shecan.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.R;
import ir.shecan.databinding.BottomItemBinding;
import ir.shecan.databinding.CustomBottomBarBinding;

public class CustomBottomBar extends LinearLayout {

    private final CustomBottomBarBinding binding;
    private final List<Item> items = new ArrayList<>();
    private int selectedIndex = -1;

    public interface OnItemSelected {
        void onSelect(int index);
    }

    private OnItemSelected listener;

    public void setOnItemSelected(OnItemSelected listener) {
        this.listener = listener;
    }

    public CustomBottomBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = LayoutInflater.from(context);
        binding = CustomBottomBarBinding.inflate(inflater, this, true);
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(false);
        setFocusable(false);
        setOrientation(HORIZONTAL);
    }

    public void addItem(String title, int inactiveRes, int activeRes) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        BottomItemBinding itemBinding =
                BottomItemBinding.inflate(inflater, binding.getRoot(), false);

        Item item = new Item(itemBinding, inactiveRes, activeRes, title);
        items.add(item);

        itemBinding.getRoot().setClickable(true);

        itemBinding.label.setText(title);
        itemBinding.iconNormal.setImageResource(inactiveRes);

        int index = items.size() - 1;

        itemBinding.getRoot().setOnClickListener(v -> {
            select(index);
            if (listener != null) listener.onSelect(index);
        });

        binding.getRoot().addView(itemBinding.getRoot());
    }

    public void removeItems() {
        binding.getRoot().removeAllViews();
        items.clear();
        selectedIndex = -1;
    }

    public void select(int index) {
        selectedIndex = index;

        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);

            if (i == index) {
                // آیکون فعال
                it.binding.iconActive.setImageResource(it.activeRes);
                it.binding.iconActive.setVisibility(VISIBLE);
                it.binding.iconNormal.setVisibility(GONE);
                it.binding.label.setTextColor(
                        ContextCompat.getColor(getContext(), R.color.primaryTextColor)
                );

            } else {
                // آیکون غیرفعال
                it.binding.iconNormal.setImageResource(it.inactiveRes);
                it.binding.iconNormal.setVisibility(VISIBLE);
                it.binding.iconActive.setVisibility(GONE);
                it.binding.label.setTextColor(
                        ContextCompat.getColor(getContext(), R.color.greenSecondaryTextColor)
                );
            }
        }
    }

    private static class Item {
        BottomItemBinding binding;
        int inactiveRes;
        int activeRes;
        String title;

        Item(BottomItemBinding binding, int inactiveRes, int activeRes, String title) {
            this.binding = binding;
            this.inactiveRes = inactiveRes;
            this.activeRes = activeRes;
            this.title = title;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // لمس در فضای خالی نادیده گرفته شود
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false; // هیچی رو نگیر
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        float y = ev.getY();
        float height = getHeight();

        // فقط پایین‌ترین بخش کلیک‌پذیر باشد
        float clickableHeight = height * 0.6f; // یعنی فقط 45% پایین کلیک شود

        if (y < height - clickableHeight) {
            // لمس در ناحیه غیرکلیک‌پذیر → عبور بده
            return false;
        }

        // لمس در بخش واقعی bottom bar → اجازه بده فرزندان کلیک بگیرن
        return super.dispatchTouchEvent(ev);
    }

    private void setIconSize(ImageView icon, float multiplier) {
        int baseSize = dpToPx(52); // سایز اصلی (52dp)
        int newSize = (int) (baseSize * multiplier);

        ViewGroup.LayoutParams params = icon.getLayoutParams();
        params.width = newSize;
        params.height = newSize;
        icon.setLayoutParams(params);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}

