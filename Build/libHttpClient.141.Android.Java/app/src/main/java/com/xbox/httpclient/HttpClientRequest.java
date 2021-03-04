package com.xbox.httpclient;

import java.io.IOException;
import java.net.UnknownHostException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;

public class HttpClientRequest {
    private static OkHttpClient OK_CLIENT;
    private static final byte[] NO_BODY = new byte[0];

    private Request okHttpRequest;
    private Request.Builder requestBuilder;

    static {
        OK_CLIENT = new OkHttpClient.Builder()
                .retryOnConnectionFailure(false) // Explicitly disable retries; retry logic will be managed by native code in libHttpClient
                .build();
    }

    public HttpClientRequest() {
        requestBuilder = new Request.Builder();
    }

    public static HttpClientRequest createClientRequest() {
        return new HttpClientRequest();
    }

    public void setHttpUrl(String url) {
        this.requestBuilder = this.requestBuilder.url(url);
    }

    public void setHttpMethodAndBody(String method, String contentType, byte[] body) {
        if (body == null || body.length == 0) {
            if ("POST".equals(method) || "PUT".equals(method)) {
                this.requestBuilder = this.requestBuilder.method(method, RequestBody.create(NO_BODY, null));
            } else {
                this.requestBuilder = this.requestBuilder.method(method, null);
            }
        } else {
            this.requestBuilder = this.requestBuilder.method(method, RequestBody.create(body, MediaType.parse(contentType)));
        }
    }

    public void setHttpHeader(String name, String value) {
        this.requestBuilder = requestBuilder.addHeader(name, value);
    }

    public void doRequestAsync(final long sourceCall) {
        OK_CLIENT.newCall(this.requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, IOException e) {
                boolean isNoNetworkFailure = e instanceof UnknownHostException;
                OnRequestFailed(sourceCall, e.getClass().getCanonicalName(), isNoNetworkFailure);
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                OnRequestCompleted(sourceCall, new HttpClientResponse(response));
            }
        });
    }

    private native void OnRequestCompleted(long call, HttpClientResponse response);
    private native void OnRequestFailed(long call, String errorMessage, boolean isNoNetwork);
}