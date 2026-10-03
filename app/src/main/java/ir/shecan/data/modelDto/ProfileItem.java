package ir.shecan.data.modelDto;

public class ProfileItem {
    private final int id;
    private final int iconRes;
    private final String title;

    public ProfileItem(int id, int iconRes, String title) {
        this.id = id;
        this.iconRes = iconRes;
        this.title = title;
    }

    public int getId() {
        return id;
    }
    public int getIconRes() {
        return iconRes;
    }

    public String getTitle() {
        return title;
    }
}
