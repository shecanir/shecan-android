package ir.shecan.data.modelDto;

import com.google.gson.annotations.SerializedName;

public class BannerViewModel {

    private static final long DEFAULT_DURATION_MS = 5000L;

    private int type;
    private int order;
    @SerializedName(value = "banner_id", alternate = {"bannerId", "id"})
    private String bannerId;
    @SerializedName(value = "priority", alternate = {"order_priority", "orderPriority"})
    private Integer priority;
    @SerializedName(value = "is_active", alternate = {"isActive"})
    private Boolean active;
    private String url;
    @SerializedName(value = "redirect_link", alternate = {"redirectLink", "link"})
    private String redirectLink;
    @SerializedName(value = "imageURL", alternate = {
            "image_url",
            "imageUrl",
            "banner_link",
            "bannerLink"
    })
    private String imageURL;
    private String imageBase64;
    @SerializedName(value = "duration", alternate = {
            "time_to_show",
            "timeToShow",
            "duration_seconds",
            "durationSeconds",
            "display_duration",
            "displayDuration",
            "display_seconds",
            "displaySeconds",
            "slide_duration",
            "slideDuration",
            "interval",
            "interval_seconds",
            "intervalSeconds"
    })
    private Long duration;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getBannerId() {
        return bannerId;
    }

    public void setBannerId(String bannerId) {
        this.bannerId = bannerId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getUrl() {
        return redirectLink != null && !redirectLink.trim().isEmpty() ? redirectLink : url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRedirectLink() {
        return redirectLink;
    }

    public void setRedirectLink(String redirectLink) {
        this.redirectLink = redirectLink;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public long getDurationMs() {
        if (duration == null || duration <= 0L) return DEFAULT_DURATION_MS;
        return duration <= 120L ? duration * 1000L : duration;
    }
}
