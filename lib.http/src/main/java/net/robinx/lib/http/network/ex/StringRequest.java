package net.robinx.lib.http.network.ex;

import net.robinx.lib.http.callback.OnRequestListener;
import net.robinx.lib.http.config.HttpMethod;
import net.robinx.lib.http.config.Priority;
import net.robinx.lib.http.config.RequestCacheOptions;
import net.robinx.lib.http.response.NetworkResponse;
import net.robinx.lib.http.response.Response;
import net.robinx.lib.http.retry.DefaultRetryPolicyImpl;
import net.robinx.lib.http.retry.RetryPolicy;

/**
 * Get char sequence from network
 *
 * @author Robin
 * @since 2016-01-05 19:15:06
 */
public class StringRequest extends HttpRequest<String> {

    private StringRequest(Builder builder) {
        super();

        super.mRequestCacheOptions = builder.requestCacheOptions;
        super.retryPolicy = builder.retryPolicy;
        super.onRequestListener = builder.onRequestListener;
        super.mPriority = builder.priority;
        super.httpMethod = builder.httpMethod;
        super.cacheKey = builder.cacheKey;
        super.mTag = builder.tag;
        super.mUrl = builder.url;

        if (builder.requestParams != null) {
            super.mRequestParams = builder.requestParams;
        }
        if (builder.body != null) {
            super.mBody = builder.body;
            super.mBody.setBytesWriteListener(super.getBytesWriteListener());
        }

        //handler default
        if (super.cacheKey == null) {
            super.cacheKey = builder.url;
        }

        if (super.mPriority == null) {
            setPriority(Priority.NORMAL);
        }

        if (super.retryPolicy == null) {
            setRetryPolicy(new DefaultRetryPolicyImpl(DefaultRetryPolicyImpl.DEFAULT_TIMEOUT_MS, DefaultRetryPolicyImpl.DEFAULT_MAX_RETRIES, DefaultRetryPolicyImpl.DEFAULT_BACKOFF_MULT));
        }

        if (super.mRequestCacheOptions == null) {
            setRequestCacheOptions(RequestCacheOptions.buildDefaultCacheOptions());
        }

        if (super.httpMethod == HttpMethod.GET) {
            super.mUrl = builder.url+super.mRequestParams.buildQueryParameters();
        } else {
            super.mUrl = builder.url;
        }

    }

    @Override
    public Response<String> parseNetworkResponse(NetworkResponse response) {
        return Response.success(new String(response.data), response.headers);
    }


    /*
     * =========================================================================
     * Inner class
     * =========================================================================
     */
    public static class Builder implements net.robinx.lib.http.base.Builder<HttpRequest<String>> {

        //HttpRequest property
        private RequestParams requestParams;
        private Body body;

        //Request property
        private RequestCacheOptions requestCacheOptions;
        private RetryPolicy retryPolicy;
        private String url;
        private String cacheKey;
        private Object tag;
        private Priority priority;
        private OnRequestListener<String> onRequestListener;
        private int httpMethod;

        public Builder() {
        }

        public Builder requestParams(RequestParams requestParams) {
            this.requestParams = requestParams;
            return this;
        }

        public Builder body(Body body) {
            this.body = body;
            return this;
        }

        public Builder requestCacheOptions(RequestCacheOptions requestCacheOptions) {
            this.requestCacheOptions = requestCacheOptions;
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder cacheKey(String cacheKey) {
            this.cacheKey = cacheKey;
            return this;
        }

        public Builder tag(Object tag) {
            this.tag = tag;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder onRequestListener(OnRequestListener<String> onRequestListener) {
            this.onRequestListener = onRequestListener;
            return this;
        }

        public Builder httpMethod(int httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        @Override
        public HttpRequest<String> build() {
            return new StringRequest(this);
        }
    }

}
