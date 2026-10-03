package ir.shecan.data.modelDio;

import com.google.gson.annotations.SerializedName;

public class DialogActionApiInput {

    @SerializedName("dialog_id")
    private final String dialogId;
    @SerializedName("mobile_number")
    private final String mobileNumber;
    @SerializedName("button_type")
    private final String buttonType;

    public DialogActionApiInput(String dialogId, String mobileNumber, String buttonType) {
        this.dialogId = dialogId;
        this.mobileNumber = mobileNumber;
        this.buttonType = buttonType;
    }
}
