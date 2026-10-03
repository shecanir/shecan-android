package ir.shecan.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.shecan.R;
import ir.shecan.data.modelDto.ThemeItem;

public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {

    private final Context context;
    private int selectedMode;   // mode انتخاب شده
    private final List<ThemeItem> list;
    private final OnItemClick listener;

    public interface OnItemClick {
        void onClick(ThemeItem item);
    }

    public ThemeAdapter(Context context, int mode, List<ThemeItem> list, OnItemClick listener) {
        this.context = context;
        this.selectedMode = mode;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_theme, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ThemeItem item = list.get(position);
        boolean isSelected = (item.getMode() == selectedMode);

        // تغییر عنوان
        holder.title.setText(item.getTitle());
        holder.title.setTextColor(ContextCompat.getColor(
                context,
                isSelected ? R.color.primaryTextColor : R.color.greenSecondaryTextColor
        ));

        // تغییر آیکون
        holder.icon.setImageDrawable(getDrawable(context, item.getMode()));
        holder.icon.setImageTintList(ContextCompat.getColorStateList(
                context,
                isSelected ? R.color.primaryTextColor : R.color.greenSecondaryTextColor
        ));

        // تغییر بک‌گراند کارت
        holder.view.setBackground(getBackgroundDrawable(context, selectedMode, item.getMode()));

        // کلیک
        holder.itemView.setOnClickListener(v -> {
            selectedMode = item.getMode();
            notifyDataSetChanged();
            listener.onClick(item);
        });
    }

    private Drawable getBackgroundDrawable(Context context, int currentMode, int mode) {
        return context.getDrawable(
                currentMode == mode
                        ? R.drawable.theme_card_selected_bg
                        : R.drawable.theme_card_unselected_bg
        );
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private Drawable getDrawable(Context context, int mode) {
        boolean isSelected = (mode == selectedMode);

        switch (mode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                return context.getDrawable(isSelected
                        ? R.drawable.ic_sun_selected
                        : R.drawable.ic_sun_not_selected
                );

            case AppCompatDelegate.MODE_NIGHT_YES:
                return context.getDrawable(isSelected
                        ? R.drawable.ic_moon_selected
                        : R.drawable.ic_moon_not_selected
                );

            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                return context.getDrawable(isSelected
                        ? R.drawable.ic_auto
                        : R.drawable.ic_auto
                );
        }

        return context.getDrawable(R.drawable.ic_sun_not_selected);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView icon;
        LinearLayout view;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.themeText);
            icon = itemView.findViewById(R.id.themeIcon);
            view = itemView.findViewById(R.id.themeBackground);
        }
    }
}
