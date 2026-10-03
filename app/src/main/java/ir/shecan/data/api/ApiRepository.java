package ir.shecan.data.api;

import android.content.Context;

import java.util.List;
import java.util.Map;

public class ApiRepository {

    public final APIManager apiManager;

    public ApiRepository(Context context) {
        apiManager = APIManager.getInstance(context);
    }

    public <T, P> void request(
            String cacheKey,
            P payload,
            String url,
            HttpMethod method,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz
    ) {
        apiManager.requestObject(cacheKey, payload, url, method, useCache, callback, clazz);
    }

    public <T, P> void request(
            String cacheKey,
            P payload,
            Map<String, String> headers,
            String url,
            HttpMethod method,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz
    ) {
        apiManager.requestObject(cacheKey, payload, headers, url, method, useCache, callback, clazz);
    }

    public <T, P> void requestList(
            String cacheKey,
            P payload,
            String url,
            HttpMethod method,
            boolean useCache,
            boolean isPublicApi,
            ApiCallback<List<T>> callback,
            Class<T> clazz
    ) {
        apiManager.requestList(cacheKey, payload, url, method, useCache, isPublicApi, callback, clazz);
    }

    public <T, P> void requestList(
            String cacheKey,
            P payload,
            Map<String, String> headers,
            String url,
            HttpMethod method,
            boolean useCache,
            boolean isPublicApi,
            ApiCallback<List<T>> callback,
            Class<T> clazz
    ) {
        apiManager.requestList(cacheKey, payload, headers, url, method, useCache, isPublicApi, callback, clazz);
    }

    public <T> void requestForm(
            String cacheKey,
            Map<String, String> payload,
            String url,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz
    ) {
        apiManager.requestFormObject(cacheKey, payload, url, useCache, callback, clazz);
    }

    public <T> void requestForm(
            String cacheKey,
            Map<String, String> payload,
            Map<String, String> headers,
            String url,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz
    ) {
        apiManager.requestFormObject(cacheKey, payload, headers, url, useCache, callback, clazz);
    }

    public <T> void requestRawForm(
            String cacheKey,
            String rawBody,
            Map<String, String> headers,
            String url,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz
    ) {
        apiManager.requestRawFormObject(cacheKey, rawBody, headers, url, useCache, callback, clazz);
    }

    public void requestText(
            String cacheKey,
            String url,
            HttpMethod method,
            boolean useCache,
            ApiCallback<String> callback
    ) {
        apiManager.requestText(cacheKey, url, method, useCache, callback);
    }

}
