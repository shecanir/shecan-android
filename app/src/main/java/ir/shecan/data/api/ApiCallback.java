package ir.shecan.data.api;

public interface ApiCallback<T> {

    void onSuccess(T data, boolean fromCache);

    void onError(int statusCode, String message);
}