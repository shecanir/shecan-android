package ir.shecan.data.modelDto;

public class ServiceItem {

    // فیلدهای فعلی
    private final String orderCode;
    private final String serviceType;
    private final String durationTitle;
    private final String statusText;
    private final String updateLink;
    private final int statusIcon;
    private final int statusColor;
    private final IssuesViewModel.IssuesDTO model;

    // 🔥 فیلدهای جدید (همه فیلدهای IssuesDTO به صورت فلت)

    public int id;

    public int projectId;
    public String projectName;

    public int trackerId;
    public String trackerName;

    public int statusId;
    public String statusName;

    public int priorityId;
    public String priorityName;

    public int authorId;
    public String authorName;

    public String subject;
    public String description;

    public String startDate;
    public String dueDate;
    public int doneRatio;
    public boolean isPrivate;
    public String createdOn;
    public String updatedOn;
    public Object closedOn;

    public boolean isClosed() {
        if (closedOn == null) return false;
        String value = String.valueOf(closedOn).trim();
        return !value.isEmpty() && !"null".equalsIgnoreCase(value);
    }

    // Custom fields فلت شده
    public Integer cfDuration;          // 21
    public String cfNameFa;             // 5
    public String cfFamilyFa;           // 25
    public String cfMobile;             // 18
    public String cfEmail;              // 4
    public String cfWebsite;            // 36
    public Integer cfServiceType;       // 58
    public String cfUpdateLink;         // 95

    // سازنده آپدیت شده
    public ServiceItem(
            String orderCode,
            String serviceType,
            String durationTitle,
            String statusText,
            String updateLink,
            int statusIcon,
            int statusColor,
            IssuesViewModel.IssuesDTO model
    ) {
        this.orderCode = orderCode;
        this.serviceType = serviceType;
        this.durationTitle = durationTitle;
        this.statusText = statusText;
        this.updateLink = updateLink;
        this.statusIcon = statusIcon;
        this.statusColor = statusColor;
        this.model = model;
    }

    // گترهای قبلی
    public String getOrderCode() { return orderCode; }
    public String getServiceType() { return serviceType; }
    public String getDurationTitle() { return durationTitle; }
    public String getStatusText() { return statusName != null && !statusName.isEmpty() ? statusName : statusText; }
    public String getUpdateLink() { return updateLink; }
    public int getStatusIcon() { return statusIcon; }
    public int getStatusColor() { return statusColor; }
    public IssuesViewModel.IssuesDTO getModel() { return model; }
}
