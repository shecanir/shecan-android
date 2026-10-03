package ir.shecan.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ir.shecan.R;
import ir.shecan.core.billing.BillingPlanPrice;
import ir.shecan.core.billing.BillingStore;
import ir.shecan.data.modelDto.PriceViewModel;
import ir.shecan.databinding.ItemBillingPlanBinding;
import saman.zamani.persiandate.PersianDate;
import saman.zamani.persiandate.PersianDateFormat;

public class BillingPlanAdapter extends RecyclerView.Adapter<BillingPlanAdapter.ViewHolder> {

    public interface OnBuyClick {
        void onClick(BillingPlanPrice item);
    }

    public interface OnDiscountClick {
        void onClick(BillingPlanPrice item, String code);
    }

    private final List<BillingPlanPrice> items;
    private final OnBuyClick onBuyClick;
    private final OnDiscountClick onDiscountClick;
    private final boolean showDiscount;
    private final NumberFormat numberFormat = NumberFormat.getInstance(new Locale("fa", "IR"));

    public BillingPlanAdapter(
            List<BillingPlanPrice> items,
            OnBuyClick onBuyClick,
            OnDiscountClick onDiscountClick,
            boolean showDiscount
    ) {
        this.items = items;
        this.onBuyClick = onBuyClick;
        this.onDiscountClick = onDiscountClick;
        this.showDiscount = showDiscount;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemBillingPlanBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        ));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BillingPlanPrice item = items.get(position);
        Context context = holder.itemView.getContext();
        boolean purchasable = item.getPlan().isPurchasable();
        PriceViewModel price = item.getPrice();

        holder.binding.cardRoot.setBackgroundResource(
                purchasable ? R.drawable.bg_billing_plan_card : R.drawable.bg_billing_plan_disabled
        );
        holder.binding.tvTitle.setText(item.getPlan().getTitle());
        holder.binding.tvSku.setText(item.getPlan().getSku());
        holder.binding.tvAvailability.setText(purchasable ? R.string.billing_available : R.string.billing_unavailable);
        holder.binding.tvDueDate.setText(price != null && price.getDueDate() != null
                ? context.getString(R.string.billing_due_date, formatPersianDate(price.getDueDate()))
                : "");

        if (item.isLoading()) {
            holder.binding.tvPrice.setText(R.string.billing_loading_price);
            holder.binding.tvMeta.setText(R.string.billing_wait);
        } else if (item.getErrorMessage() != null) {
            holder.binding.tvPrice.setText(R.string.billing_unknown_price);
            holder.binding.tvMeta.setText(item.getErrorMessage());
        } else if (price != null) {
            holder.binding.tvPrice.setText(context.getString(R.string.billing_price_toman, numberFormat.format(item.getEffectivePrice(BillingStore.current()))));
            holder.binding.tvMeta.setText(buildPriceMeta(context, item));
        } else {
            holder.binding.tvPrice.setText(R.string.billing_unknown_price);
            holder.binding.tvMeta.setText(R.string.billing_no_price);
        }

        holder.binding.tvDiscountMessage.setVisibility(showDiscount && item.getDiscountMessage() != null ? View.VISIBLE : View.GONE);
        holder.binding.tvDiscountMessage.setText(item.getDiscountMessage());
        holder.binding.etDiscountCode.setText(item.getDiscountCode() != null ? item.getDiscountCode() : "");
        holder.binding.discountRow.setVisibility(showDiscount && purchasable && price != null ? View.VISIBLE : View.GONE);
        holder.binding.btnApplyDiscount.setText(item.isDiscountLoading() ? "..." : context.getString(R.string.billing_apply_discount));
        holder.binding.btnApplyDiscount.setEnabled(!item.isDiscountLoading());
        holder.binding.btnApplyDiscount.setOnClickListener(v -> {
            String code = holder.binding.etDiscountCode.getText() != null
                    ? holder.binding.etDiscountCode.getText().toString().trim()
                    : "";
            onDiscountClick.onClick(item, code);
        });

        holder.binding.btnBuy.setEnabled(purchasable && price != null && !item.isLoading() && !item.isDiscountLoading());
        holder.binding.btnBuy.setAlpha(holder.binding.btnBuy.isEnabled() ? 1f : 0.55f);
        holder.binding.btnBuy.setBackgroundResource(
                holder.binding.btnBuy.isEnabled()
                        ? R.drawable.primary_button
                        : R.drawable.bg_billing_button_disabled
        );
        holder.binding.btnBuy.setText(purchasable ? R.string.billing_buy_plan : R.string.billing_buy_disabled);
        holder.binding.btnBuy.setOnClickListener(v -> {
            if (holder.binding.btnBuy.isEnabled()) onBuyClick.onClick(item);
        });
    }

    private String buildPriceMeta(Context context, BillingPlanPrice item) {
        PriceViewModel price = item.getPrice();
        StringBuilder meta = new StringBuilder(context.getString(R.string.billing_price_loaded));
        if (item.getDiscountedPrice() != null) {
            meta.append(" | ").append(context.getString(R.string.billing_discount_code_applied));
        }
        if (price.getCredit() != null && price.getCredit() > 0) {
            meta.append(" | ").append(context.getString(R.string.billing_current_credit, numberFormat.format(price.getCredit())));
        }
        if (price.getNewCredit() != null && price.getNewCredit() > 0) {
            meta.append(" | ").append(context.getString(R.string.billing_new_credit, numberFormat.format(price.getNewCredit())));
        }
        return meta.toString();
    }

    private String formatPersianDate(String rawDate) {
        try {
            if (rawDate == null || rawDate.trim().isEmpty()) return "";
            String normalized = rawDate.trim();
            if (normalized.contains("T")) normalized = normalized.substring(0, normalized.indexOf("T"));
            if (normalized.contains(" ")) normalized = normalized.substring(0, normalized.indexOf(" "));

            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(normalized);
            if (date == null) return rawDate;

            PersianDate persianDate = new PersianDate(date);
            return new PersianDateFormat("Y/m/d").format(persianDate);
        } catch (Exception ignored) {
            return rawDate;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void replaceItems(List<BillingPlanPrice> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemBillingPlanBinding binding;

        ViewHolder(ItemBillingPlanBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
