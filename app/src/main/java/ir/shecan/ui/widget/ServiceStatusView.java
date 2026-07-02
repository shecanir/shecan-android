package ir.shecan.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ir.shecan.databinding.SelectedConfigViewBinding;
import ir.shecan.data.modelDto.ServiceItem;
import saman.zamani.persiandate.PersianDate;
import saman.zamani.persiandate.PersianDateFormat;

public class ServiceStatusView extends ConstraintLayout {

    private SelectedConfigViewBinding binding;

    public ServiceStatusView(Context context) {
        super(context);
        init(context);
    }

    public ServiceStatusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ServiceStatusView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        binding = SelectedConfigViewBinding.inflate(LayoutInflater.from(context), this, true);
    }

//    // -------------------------------------------
//    // Data Model
//    // -------------------------------------------
//    public static class ServiceStatus {
//        public String serviceType;
//        public boolean isPurchased;
//        public String orderCode;
//        public String expireDate;
//        public String updateLink;
//
//        // Purchased
//        public ServiceStatus(String serviceType, String orderCode, String expireDate, String updateLink) {
//            this.serviceType = serviceType;
//            this.orderCode = orderCode;
//            this.expireDate = expireDate;
//            this.updateLink = updateLink;
//            this.isPurchased = true;
//        }
//
//        // Free
//        public ServiceStatus(String serviceType) {
//            this.serviceType = serviceType;
//            this.isPurchased = false;
//        }
//    }

    // -------------------------------------------
    // Set status
    // -------------------------------------------
    public void setStatus(ServiceItem status) {
        if (status == null) {
            binding.valueService.setText("-");
            binding.colOrder.setVisibility(View.GONE);
            binding.colExpire.setVisibility(View.GONE);
            return;
        }

        String serviceType = status.getServiceType();
        binding.valueService.setText(serviceType != null && !serviceType.trim().isEmpty() ? serviceType : "-");

        if (status.getModel() != null && status.getModel().getId() > 0) {
            binding.colOrder.setVisibility(View.VISIBLE);
            binding.colExpire.setVisibility(View.VISIBLE);

            binding.valueOrder.setText(status.getOrderCode() != null ? status.getOrderCode() : "-");
            binding.valueExpire.setText(formatDueDate(status.getModel().getDueDate()));

        } else {
            binding.colOrder.setVisibility(View.GONE);
            binding.colExpire.setVisibility(View.GONE);
        }
    }

    private String formatDueDate(String dueDate) {
        if (dueDate == null || dueDate.trim().isEmpty()) return "-";

        try {
            String normalized = dueDate.trim();
            int timeSeparator = normalized.indexOf('T');
            if (timeSeparator > 0) normalized = normalized.substring(0, timeSeparator);
            int spaceSeparator = normalized.indexOf(' ');
            if (spaceSeparator > 0) normalized = normalized.substring(0, spaceSeparator);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setLenient(false);
            Date date = sdf.parse(normalized);
            if (date == null) return "-";

            PersianDate pDate = new PersianDate(date);
            return new PersianDateFormat("Y/m/d").format(pDate);
        } catch (Exception ignored) {
            return "-";
        }
    }

    public boolean isPurchased() {
        return binding.colOrder.getVisibility() == VISIBLE;
    }
}
