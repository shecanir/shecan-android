package ir.shecan.data.modelDio;

import com.google.gson.annotations.SerializedName;

public class DialogDismissApiInput {

    @SerializedName("dialog_id")
    private final String dialogId;
    @SerializedName("mobile_number")
    private final String mobileNumber;

    public DialogDismissApiInput(String dialogId, String mobileNumber) {
        this.dialogId = dialogId;
        this.mobileNumber = mobileNumber;
    }
}
