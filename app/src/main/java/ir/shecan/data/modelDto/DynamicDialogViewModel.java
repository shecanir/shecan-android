package ir.shecan.data.modelDto;

import com.google.gson.annotations.SerializedName;

public class DynamicDialogViewModel {

    @SerializedName(value = "dialog_id", alternate = {"dialogId", "id"})
    private String dialogId;
    private String title;
    private String message;
    private Integer priority;
    @SerializedName(value = "is_active", alternate = {"isActive"})
    private Boolean active;
    @SerializedName(value = "is_dismissible", alternate = {"isDismissible", "dismissible"})
    private Boolean dismissible;
    @SerializedName(value = "time_to_show", alternate = {"timeToShow", "duration", "duration_seconds"})
    private Long timeToShow;
    @SerializedName(value = "primary_button", alternate = {"primaryButton"})
    private DialogButton primaryButton;
    @SerializedName(value = "secondary_button", alternate = {"secondaryButton"})
    private DialogButton secondaryButton;

    public String getDialogId() {
        return dialogId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Integer getPriority() {
        return priority;
    }

    public Boolean getActive() {
        return active;
    }

    public boolean isActive() {
        return active == null || active;
    }

    public boolean isDismissible() {
        return Boolean.TRUE.equals(dismissible);
    }

    public Long getTimeToShow() {
        return timeToShow;
    }

    public long getTimeToShowMs() {
        if (timeToShow == null || timeToShow <= 0L) return 0L;
        return timeToShow <= 120L ? timeToShow * 1000L : timeToShow;
    }

    public DialogButton getPrimaryButton() {
        return primaryButton;
    }

    public DialogButton getSecondaryButton() {
        return secondaryButton;
    }

    public boolean hasContent() {
        return !isBlank(title) || !isBlank(message);
    }

    public static class DialogButton {
        private String title;
        @SerializedName(value = "redirect_link", alternate = {"redirectLink", "link", "url"})
        private String redirectLink;

        public String getTitle() {
            return title;
        }

        public String getRedirectLink() {
            return redirectLink;
        }

        public boolean hasTitle() {
            return !isBlank(title);
        }

        public boolean hasRedirectLink() {
            return !isBlank(redirectLink);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
