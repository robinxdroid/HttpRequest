package net.robinx.lib.http.okhttp;

import net.robinx.lib.http.base.Request;
import net.robinx.lib.http.config.HttpMethod;
import net.robinx.lib.http.network.HttpResponse;
import net.robinx.lib.http.network.HttpStack;
import net.robinx.lib.http.utils.CLog;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;


import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by Robin on 2016/4/23.
 *
 *  OkHttpClient okClient = new OkHttpClient.Builder().build();
 *  RequestQueue queue = Volley.newRequestQueue(context, new OkHttpStack(okClient));
 */
public class OkHttpStack implements HttpStack {


    public OkHttpStack() {
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException {
        CLog.w("Current Stack:%s","OK_HTTP");

        int timeoutMs = request.getRetryPolicy().getCurrentTimeout();
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        builder.connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        OkHttpClient client = builder.build();

        okhttp3.Request.Builder okHttpRequestBuilder = new okhttp3.Request.Builder();
        okHttpRequestBuilder.url(request.getUrl());

        Map<String, String> headers = request.getHeaders();
        for (final String name : headers.keySet()) {
            okHttpRequestBuilder.addHeader(name, headers.get(name));
        }
        for (final String name : additionalHeaders.keySet()) {
            // 这里用header方法，如果有重复的name，会覆盖，否则某些请求会被判定为非法
            okHttpRequestBuilder.header(name, additionalHeaders.get(name));
        }

        setConnectionParametersForRequest(okHttpRequestBuilder, request);

        okhttp3.Request okHttpRequest = okHttpRequestBuilder.build();
        Call okHttpCall = client.newCall(okHttpRequest);
        Response okHttpResponse = okHttpCall.execute();

        HttpResponse response = responseFromConnection(okHttpResponse);

        return response;
    }

    /**
     * Create HttpResponse from OkHttp
     */
    private HttpResponse responseFromConnection(Response okHttpResponse) throws IOException {
        HttpResponse response = new HttpResponse();
        int responseCode = okHttpResponse.code();
        if (responseCode == -1) {
            throw new IOException("Could not retrieve response code from OkHttp.");
        }
        response.setResponseCode(responseCode);
        response.setResponseMessage(okHttpResponse.message());
        // contentStream
        ResponseBody body = okHttpResponse.body();

        InputStream inputStream = body.byteStream();
        response.setContentStream(inputStream);
        response.setContentLength(body.contentLength());
        response.setContentEncoding(okHttpResponse.header("Content-Encoding"));
        response.setContentType(body.contentType().type());
        // header
        Map<String, String> headerMap = new HashMap<String, String>();

        Headers responseHeaders = okHttpResponse.headers();
        for (int i = 0, len = responseHeaders.size(); i < len; i++) {
            final String name = responseHeaders.name(i), value = responseHeaders.value(i);
            if (name != null) {
                headerMap.put(name, value);
                response.setHeaders(headerMap);
            }
        }

        return response;
    }

    private static void setConnectionParametersForRequest(
            okhttp3.Request.Builder builder, Request<?> request) throws IOException{
        switch (request.getHttpMethod()) {
            case HttpMethod.GET:
                builder.get();
                break;
            case HttpMethod.DELETE:
                builder.delete();
                break;
            case HttpMethod.POST:
                builder.post(addBodyIfExists(request));
                break;
            case HttpMethod.PUT:
                builder.put(addBodyIfExists(request));
                break;
            case HttpMethod.HEAD:
                builder.head();
                break;
            case HttpMethod.OPTIONS:
                builder.method("OPTIONS", null);
                break;
            case HttpMethod.TRACE:
                builder.method("TRACE", null);
                break;
            case HttpMethod.PATCH:
                builder.patch(addBodyIfExists(request));
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    private static RequestBody addBodyIfExists(Request<?> request) {
        return  request.buildBody();
    }
}