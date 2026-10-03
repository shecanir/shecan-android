package ir.shecan.data.modelDto;

public class AppConfig {

    private int mode;

    public AppConfig(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}
