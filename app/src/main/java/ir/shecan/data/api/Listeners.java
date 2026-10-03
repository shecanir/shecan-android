package ir.shecan.data.api;

public class Listeners {

    public interface ApiListener<T> {
        void onReceived(T response, boolean fromCache);
    }
}
