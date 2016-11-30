package net.robinx.lib.http.network.ex;

import android.text.TextUtils;

import net.robinx.lib.http.callback.OnRequestListener;
import net.robinx.lib.http.config.HttpMethod;
import net.robinx.lib.http.config.Priority;
import net.robinx.lib.http.config.RequestCacheOptions;
import net.robinx.lib.http.converter.Converter;
import net.robinx.lib.http.converter.GsonConverter;
import net.robinx.lib.http.callback.OnRequestListenerAdapter;
import net.robinx.lib.http.response.NetworkResponse;
import net.robinx.lib.http.response.Response;
import net.robinx.lib.http.retry.DefaultRetryPolicyImpl;
import net.robinx.lib.http.retry.RetryPolicy;
import net.robinx.lib.http.utils.CLog;
import net.robinx.lib.http.utils.GenericsUtils;

import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Only used to set the parameters and the parse results
 *
 * @param <T>
 * @author Robin
 * @since 2016-01-07 19:55:16
 */
public class MultipartRequest<T> extends HttpRequest<T> {

    private Type mBeanType;

    private Converter<T> mConverter;

    private MultipartRequest(Builder<T> builder) {
        super();

        super.mRequestCacheOptions = builder.requestCacheOptions;
        super.retryPolicy = builder.retryPolicy;
        super.onRequestListener = builder.onRequestListener;
        super.mPriority = builder.priority;
        super.httpMethod = builder.httpMethod;
        super.cacheKey = builder.cacheKey;
        super.mTag = builder.tag;
        super.mUrl = builder.url;

        this.mConverter = builder.converter;

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

        if (super.onRequestListener == null) {
            //Must specify a default instance, in order to obtain a generic needs
            super.onRequestListener = new OnRequestListenerAdapter<T>() {
            };
        }

        if (super.httpMethod == HttpMethod.GET) {
            super.mUrl = builder.url+super.mRequestParams.buildQueryParameters();
        } else {
            super.mUrl = builder.url;
        }

        if (this.mConverter == null) {
            this.mConverter = new GsonConverter<>();
        }

        mBeanType = builder.beanType;
        if (mBeanType == null) {
            mBeanType = GenericsUtils.getBeanClassType(onRequestListener);
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public Response<T> parseNetworkResponse(NetworkResponse response) {
        String result = new String(response.data);

        CLog.d("[Original String Data]:%s", result);

        //Because Android Studio cannot print string contains special characters, so here to filter out the special characters
        if (!TextUtils.isEmpty(result)) {
            Pattern CRLF = Pattern.compile("(\r\n|\r|\n|\n\r|\t)");
            Matcher matcher = CRLF.matcher(result);
            result = matcher.replaceAll("");
            //CLog.d("[Filter the special characters of the original character data]:%s", result);
        }

        if (mBeanType.equals(String.class)) {
            T parseResult = (T) result;
            CLog.d("parse network response complete");
            super.onParseNetworkResponse(response, parseResult);

            return Response.success(parseResult, response.headers);
        }

        if (mConverter == null) {
            throw new RuntimeException("You must set a converter instance is used to convert the JSON data");
        }

        if (result.startsWith("[") && result.endsWith("]")) {
            T parseResult = mConverter.fromJSONArray(result, mBeanType);
            CLog.d("parse network response complete");
            super.onParseNetworkResponse(response, parseResult);

            return Response.success(parseResult, response.headers);
        }
        if (result.startsWith("{") && result.endsWith("}")) {
            T parseResult = mConverter.fromJSONObject(result, mBeanType);
            CLog.d("parse network response complete");
            super.onParseNetworkResponse(response, parseResult);

            return Response.success(parseResult, response.headers);
        }

        //DefaultHandler
        T parseResult = (T) result;
        CLog.d("parse network response complete");
        super.onParseNetworkResponse(response, parseResult);
        return Response.success(parseResult, response.headers);
    }

    /*
     * =========================================================================
     * Inner class
     * =========================================================================
     */
    public static class Builder<T> implements net.robinx.lib.http.base.Builder<MultipartRequest<T>> {

        //Local property
        private Converter<T> converter;

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
        private OnRequestListener<T> onRequestListener;
        private int httpMethod;

        private Type beanType;

        public Builder() {
        }

        public Builder addConverter(Converter<T> converter) {
            this.converter = converter;
            return this;
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

        public Builder onRequestListener(OnRequestListener<T> onRequestListener) {
            this.onRequestListener = onRequestListener;
            return this;
        }

        public Builder httpMethod(int httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public Builder parseBeanType(Type beanType) {
            this.beanType = beanType;
            return this;
        }

        @Override
        public MultipartRequest<T> build() {
            return new MultipartRequest(this);
        }
    }

}