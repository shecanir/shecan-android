package ir.shecan.data.modelDio;

public class BannerMatchApiInput {

    private final String mobileNumber;
    private final String apiKey;
    private final String serviceType;
    private final String plan;
    private final String startDate;
    private final String dueDate;
    private final String store;
    private final boolean rated;
    private final String planStatus;

    public BannerMatchApiInput(
            String mobileNumber,
            String apiKey,
            String serviceType,
            String plan,
            String startDate,
            String dueDate,
            String store,
            boolean rated,
            String planStatus
    ) {
        this.mobileNumber = mobileNumber;
        this.apiKey = apiKey;
        this.serviceType = serviceType;
        this.plan = plan;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.store = store;
        this.rated = rated;
        this.planStatus = planStatus;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getPlan() {
        return plan;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getStore() {
        return store;
    }

    public boolean isRated() {
        return rated;
    }

    public String getPlanStatus() {
        return planStatus;
    }
}
