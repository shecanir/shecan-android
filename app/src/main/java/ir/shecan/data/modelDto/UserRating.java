package ir.shecan.data.modelDto;

public class UserRating {

    private String mobileNumber;
    private long weeklyConnectionTime;
    private String appVersion;
    private String storeName;
    private int rating;
    private boolean isProUser;
    private String createdDate;

//    // Empty Constructor (برای Gson / Retrofit لازم است)
//    public UserRating() {
//    }

    // Full Constructor
    public UserRating(String mobileNumber,
                      long weeklyConnectionTime,
                      String appVersion,
                      String storeName,
                      int rating,
                      boolean isProUser,
                      String createdDate) {
        this.mobileNumber = mobileNumber;
        this.weeklyConnectionTime = weeklyConnectionTime;
        this.appVersion = appVersion;
        this.storeName = storeName;
        this.rating = rating;
        this.isProUser = isProUser;
        this.createdDate = createdDate;
    }

    // Getters and Setters

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public long getWeeklyConnectionTime() {
        return weeklyConnectionTime;
    }

    public void setWeeklyConnectionTime(long weeklyConnectionTime) {
        this.weeklyConnectionTime = weeklyConnectionTime;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public boolean isProUser() {
        return isProUser;
    }

    public void setProUser(boolean proUser) {
        isProUser = proUser;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }
}