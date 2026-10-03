package ir.shecan.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ir.shecan.databinding.ItemProfileBinding;
import ir.shecan.data.modelDto.ProfileItem;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {

    private final List<ProfileItem> items;
    private final OnItemClick listener;

    public interface OnItemClick {
        void onClick(int position, ProfileItem item);
    }

    public ProfileAdapter(List<ProfileItem> items, OnItemClick listener) {
        this.items = items;
        this.listener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemProfileBinding binding;

        public ViewHolder(ItemProfileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @NonNull
    @Override
    public ProfileAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemProfileBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        ));
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileAdapter.ViewHolder holder, int position) {
        ProfileItem item = items.get(position);

//        holder.binding.imgIcon.setImageResource(item.getIconRes());
        holder.binding.tvTitle.setText(item.getTitle());

        holder.itemView.setOnClickListener(v -> listener.onClick(position, item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
