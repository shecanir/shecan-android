package ir.shecan.data.modelDto;


public class ExistApiViewModel {

    private boolean exists;

    public ExistApiViewModel(boolean exists) {
        this.exists = exists;
    }

    public boolean getExists() {
        return exists;
    }

    public void setExists(Boolean exists) {
        this.exists = exists;
    }
}
