package ir.shecan.service;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// API Service Class
public class ApiService {
    private final OkHttpClient client;

    public ApiService() {
        client = new OkHttpClient();
    }

    public void callGetApi(String url, final ApiResponseListener listener) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Pass the error message to the listener
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    // Pass the response to the listener
                    if (listener != null) {
                        listener.onSuccess(responseBody);
                    }
                } else {
                    // Handle non-successful response codes
                    if (listener != null) {
                        listener.onError("Error: " + response.code() + ", Message: " + response.message());
                    }
                }
            }
        });
    }
}