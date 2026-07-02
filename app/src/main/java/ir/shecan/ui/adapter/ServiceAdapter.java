package ir.shecan.ui.adapter;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.shecan.R;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.databinding.ItemServicePurchaseFooterBinding;
import ir.shecan.databinding.LayoutItemServiceBinding;

public class ServiceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SERVICE = 0;
    private static final int VIEW_TYPE_PURCHASE_FOOTER = 1;

    private final Context context;
    private final List<ServiceItem> items;
    private final OnMoreClickListener listener;

    private int selectedPosition = -1;

    public interface OnMoreClickListener {
        void onBackgroundClicked(ServiceItem item);
        void onOptionClicked(ServiceItem item);
        void onPurchaseClicked();
    }

    public ServiceAdapter(Context context, List<ServiceItem> items, OnMoreClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        LayoutItemServiceBinding binding;

        public ViewHolder(LayoutItemServiceBinding binding) {
            super(binding.root);
            this.binding = binding;
        }

        public void bind(Context context, ServiceItem item, boolean isSelected, OnMoreClickListener listener) {
            if (item == null) return;

            String orderCode = item.getOrderCode() != null ? item.getOrderCode() : "";
            boolean isFreeMode = "0".equals(orderCode);
            boolean isClosed = item.isClosed();

            binding.txtOrderCode.setText(isFreeMode ? "-" : orderCode);
            binding.txtServiceType.setText(item.getServiceType());
            binding.txtStatus.setText(isFreeMode ? context.getString(R.string.readyToConnect) : item.getStatusText());
            binding.txtStatus.setTextColor(item.getStatusColor());
            binding.statusBoxIcon.setImageResource(item.getStatusIcon());
            binding.root.setAlpha(isClosed ? 0.45f : 1f);
            binding.root.setEnabled(!isClosed);

            if (isSelected && !isClosed) {
                binding.greenHalfOval.setVisibility(VISIBLE);
                binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.lightBack));
            } else {
                binding.greenHalfOval.setVisibility(INVISIBLE);
                binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
            }

            binding.btnOptions.setVisibility(isFreeMode || isClosed ? INVISIBLE : VISIBLE);
            binding.root.setOnClickListener(isClosed ? null : v -> listener.onBackgroundClicked(item));
            binding.btnOptions.setOnClickListener(isClosed ? null : v -> listener.onOptionClicked(item));
        }
    }

    public static class PurchaseFooterViewHolder extends RecyclerView.ViewHolder {

        ItemServicePurchaseFooterBinding binding;

        public PurchaseFooterViewHolder(ItemServicePurchaseFooterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(OnMoreClickListener listener) {
            binding.fabBuyService.setOnClickListener(v -> listener.onPurchaseClicked());
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_PURCHASE_FOOTER) {
            ItemServicePurchaseFooterBinding binding = ItemServicePurchaseFooterBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            );
            return new PurchaseFooterViewHolder(binding);
        }

        LayoutItemServiceBinding binding = LayoutItemServiceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PurchaseFooterViewHolder) {
            ((PurchaseFooterViewHolder) holder).bind(listener);
            return;
        }

        boolean isSelected = position == selectedPosition;

        ((ViewHolder) holder).bind(context, items.get(position), isSelected, new OnMoreClickListener() {
            @Override
            public void onBackgroundClicked(ServiceItem item) {
                if (item == null || item.isClosed()) return;
                selectedPosition = holder.getAdapterPosition();
                notifyDataSetChanged();
                listener.onBackgroundClicked(item);
            }

            @Override
            public void onOptionClicked(ServiceItem item) {
                if (item == null || item.isClosed()) return;
                selectedPosition = holder.getAdapterPosition();
                notifyDataSetChanged();
                listener.onOptionClicked(item);
            }

            @Override
            public void onPurchaseClicked() {
                listener.onPurchaseClicked();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position >= items.size() ? VIEW_TYPE_PURCHASE_FOOTER : VIEW_TYPE_SERVICE;
    }

    public void setSelectedPosition(int pos) {
        this.selectedPosition = pos;
        notifyDataSetChanged();
    }
}
