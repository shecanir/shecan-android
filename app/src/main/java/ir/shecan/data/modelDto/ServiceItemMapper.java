package ir.shecan.data.modelDto;

import android.content.Context;
import androidx.core.content.ContextCompat;
import ir.shecan.R;
import ir.shecan.core.constant.DurationType;
import ir.shecan.core.constant.RequestStatus;
import ir.shecan.core.constant.ServiceType;
import ir.shecan.data.storage.AppStorage;

public class ServiceItemMapper {

    public static ServiceItem map(Context context, IssuesViewModel.IssuesDTO dto) {
        if (dto == null) {
            dto = IssuesViewModel.IssuesDTO.createDefault();
        }

        String orderCode = String.valueOf(dto.getId());

        int serviceTypeId = getCustomFieldInt(dto, 58);
        String serviceTypeTitle = resolveServiceTypeTitle(context, serviceTypeId);

        int durationId = getCustomFieldInt(dto, 21);
        String durationTitle = resolveDurationTitle(context, durationId);

        String updateLink = getCustomFieldString(dto, 95);

        RequestStatus status = RequestStatus.fromValue(dto.getStatus() != null ? dto.getStatus().getId() : 0);
        if (status == null) status = RequestStatus.READY_TO_CONNECT;

        String statusText = getStatusTitle(context, status);
        int statusIcon = getStatusIcon(status);
        int statusColor = getStatusColor(context, status);

        // ساخت ServiceItem
        ServiceItem item = new ServiceItem(
                orderCode,
                serviceTypeTitle,
                durationTitle,
                statusText,
                updateLink,
                statusIcon,
                statusColor,
                dto
        );

        // 🎉‌ ست کردن تمام فیلدهای جدید
        item.id = dto.getId();

        item.projectId = dto.getProject() != null ? dto.getProject().getId() : 0;
        item.projectName = dto.getProject() != null ? safeString(dto.getProject().getName()) : "";

        item.trackerId = dto.getTracker() != null ? dto.getTracker().getId() : 0;
        item.trackerName = dto.getTracker() != null ? safeString(dto.getTracker().getName()) : "";

        item.statusId = dto.getStatus() != null ? dto.getStatus().getId() : 0;
        item.statusName = dto.getStatus() != null ? safeString(dto.getStatus().getName()) : statusText;

        item.priorityId = dto.getPriority() != null ? dto.getPriority().getId() : 0;
        item.priorityName = dto.getPriority() != null ? safeString(dto.getPriority().getName()) : "";

        item.authorId = dto.getAuthor() != null ? dto.getAuthor().getId() : 0;
        item.authorName = dto.getAuthor() != null ? safeString(dto.getAuthor().getName()) : "";

        item.subject = safeString(dto.getSubject());
        item.description = safeString(dto.getDescription());
        item.startDate = safeString(dto.getStartDate());
        item.dueDate = safeString(dto.getDueDate());
        item.doneRatio = dto.getDoneRatio();
        item.isPrivate = dto.isIsPrivate();
        item.createdOn = safeString(dto.getCreatedOn());
        item.updatedOn = safeString(dto.getUpdatedOn());
        item.closedOn = dto.getClosedOn();

        // custom fields
        item.cfDuration = getCustomFieldInt(dto, 21);
        item.cfNameFa = getCustomFieldString(dto, 5);
        item.cfFamilyFa = getCustomFieldString(dto, 25);
        item.cfMobile = getCustomFieldString(dto, 18);
        item.cfEmail = getCustomFieldString(dto, 4);
        item.cfWebsite = getCustomFieldString(dto, 36);
        item.cfServiceType = getCustomFieldInt(dto, 58);
        item.cfUpdateLink = getCustomFieldString(dto, 95);

        return item;
    }

    private static String resolveServiceTypeTitle(Context context, int serviceTypeId) {
        if (context != null) {
            ServicesViewModel catalog = new AppStorage(context).getServiceCatalog(ServicesViewModel.class);
            ServicesViewModel.ServiceDTO service = catalog != null
                    ? catalog.findByPeygirCode(serviceTypeId)
                    : null;
            if (service != null && service.getNameFa() != null && !service.getNameFa().trim().isEmpty()) {
                return service.getNameFa().trim();
            }
        }
        return ServiceType.fromId(serviceTypeId).getTitle();
    }

    private static String resolveDurationTitle(Context context, int durationId) {
        if (context != null) {
            ServicesViewModel catalog = new AppStorage(context).getServiceCatalog(ServicesViewModel.class);
            ServicesViewModel.DurationDTO duration = catalog != null
                    ? catalog.findDurationById(durationId)
                    : null;
            if (duration != null && duration.getTitle() != null && !duration.getTitle().trim().isEmpty()) {
                return duration.getTitle().trim();
            }
        }
        return DurationType.fromId(durationId).getTitle();
    }

    private static int getCustomFieldInt(IssuesViewModel.IssuesDTO dto, int id) {
        if (dto.getCustomFields() == null) return 0;
        for (IssuesViewModel.IssuesDTO.CustomFieldsDTO c : dto.getCustomFields()) {
            if (c.getId() == id) {
                try { return Integer.parseInt(c.getValue()); }
                catch (Exception e) { return 0; }
            }
        }
        return 0;
    }

    private static String getCustomFieldString(IssuesViewModel.IssuesDTO dto, int id) {
        if (dto.getCustomFields() == null) return "";
        for (IssuesViewModel.IssuesDTO.CustomFieldsDTO c : dto.getCustomFields()) {
            if (c.getId() == id) {
                return safeString(c.getValue());
            }
        }
        return "";
    }

    private static String safeString(String value) {
        return value != null ? value : "";
    }

    private static String getStatusTitle(Context context, RequestStatus status) {
        if (context == null) return "نامشخص";

        try {
            switch (status) {
                case READY_TO_CONNECT: return context.getString(R.string.readyToConnect);
                case ACTIVE: return context.getString(R.string.active);
                case EXPIRING: return context.getString(R.string.expiring);
                case WAITING_FOR_ACTIVATION: return context.getString(R.string.waitingForActivate);
                case NEW: return context.getString(R.string.newConnection);
                default: return "نامشخص";
            }
        } catch (Exception e) {
            return "نامشخص";
        }
    }

    private static int getStatusIcon(RequestStatus status) {
        switch (status) {
            case ACTIVE: return R.drawable.ic_connect;
            case EXPIRING: return R.drawable.ic_alert;
            case WAITING_FOR_ACTIVATION: return R.drawable.ic_watch;
            case NEW: return R.drawable.ic_new;
            default: return R.drawable.ic_done;
        }
    }

    private static int getStatusColor(Context context, RequestStatus status) {
        if (context == null) return 0;
        switch (status) {
            case ACTIVE: return ContextCompat.getColor(context, R.color.connectionIsActiveColor);
            case EXPIRING: return ContextCompat.getColor(context, R.color.colorAccent);
            case WAITING_FOR_ACTIVATION: return ContextCompat.getColor(context, R.color.primaryTextColor);
            case NEW: return ContextCompat.getColor(context, R.color.primaryTextColor);
            default: return ContextCompat.getColor(context, R.color.primaryTextColor);
        }
    }
}
