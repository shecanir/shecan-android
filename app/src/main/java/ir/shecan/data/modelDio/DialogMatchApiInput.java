package ir.shecan.data.modelDio;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class DialogMatchApiInput {

    @SerializedName("mobile_number")
    private final String mobileNumber;
    @SerializedName("api_key")
    private final String apiKey;
    @SerializedName("service_type")
    private final String serviceType;
    private final String plan;
    @SerializedName("start_date")
    private final String startDate;
    @SerializedName("due_date")
    private final String dueDate;
    private final List<String> store;
    private final boolean rated;
    @SerializedName("plan_status")
    private final String planStatus;

    public DialogMatchApiInput(
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
        this.store = store == null || store.trim().isEmpty()
                ? Collections.emptyList()
                : Collections.singletonList(store.trim());
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

    public List<String> getStore() {
        return store;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public boolean isRated() {
        return rated;
    }

    public String getPlanStatus() {
        return planStatus;
    }
}
