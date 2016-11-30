package net.robinx.lib.http.base;

import android.net.Uri;
import android.text.TextUtils;

import net.robinx.lib.http.cache.CacheData;
import net.robinx.lib.http.cache.Entry;
import net.robinx.lib.http.cache.disk.DiskCache;
import net.robinx.lib.http.cache.memory.MemoryCache;
import net.robinx.lib.http.callback.OnRequestListener;
import net.robinx.lib.http.config.DataType;
import net.robinx.lib.http.config.HttpMethod;
import net.robinx.lib.http.config.Priority;
import net.robinx.lib.http.config.RequestCacheOptions;
import net.robinx.lib.http.network.HttpException;
import net.robinx.lib.http.queue.RequestQueue;
import net.robinx.lib.http.response.NetworkResponse;
import net.robinx.lib.http.response.Response;
import net.robinx.lib.http.retry.RetryPolicy;
import net.robinx.lib.http.utils.CLog;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * Base request
 *
 * @param <T> Return data type
 * @author Robin
 * @since 2015-05-07 17:18:06
 */
public abstract class Request<T> implements IRequest<T>, IResponseDelivery<T>, Comparable<Request<T>> {
    private static final String DEFAULT_PARAMS_ENCODING = "UTF-8";

	/*
     * properties
	 */

    /**
     * This request will request's address
     */
    protected String mUrl;

    /**
     * An opaque token tagging this request; used for bulk cancellation.
     */
    protected Object mTag;

    /**
     * Whether or not this request has been canceled.
     */
    private boolean mCanceled = false;

    /**
     * Default tag for {@link android.net.TrafficStats}.
     */
    private int mDefaultTrafficStatsTag;

    /**
     * Whether or not a response has been delivered for this request yet.
     */
    private boolean mResponseDelivered = false;

    /**
     * Sequence number of this request, used to enforce FIFO ordering.
     */
    protected Integer mSequence;

    /**
     * Priority of this request ,default is "NORMAL"
     */
    protected Priority mPriority = Priority.NORMAL;

    /**
     * The request's cache key if this request need to cache
     */
    protected String cacheKey;

    /**
     * The method for request
     */
    protected int httpMethod;

	/*
     * Object
	 */
    /**
     * The request queue this request is associated with.
     */
    protected RequestQueue mRequestQueue;

    /**
     * This request's related configuration
     */
    public RequestCacheOptions mRequestCacheOptions;

    protected RetryPolicy retryPolicy;

    /**
     * The callback when this request perform finished
     */
    protected OnRequestListener<T> onRequestListener;

	/*
     * =========================================================================
	 * constructor
	 * =========================================================================
	 */

    public Request() {
        this(null, null, null, null);
    }

    public Request(RequestCacheOptions options, String url, String cacheKey, OnRequestListener<T> onRequestListener) {
        this.mRequestCacheOptions = options;
        this.mUrl = url;
        this.cacheKey = cacheKey;
        this.onRequestListener = onRequestListener;

        /*if (options == null) {
            setRequestCacheOptions(RequestCacheOptions.buildDefaultCacheOptions());
        }*/
        mDefaultTrafficStatsTag = findDefaultTrafficStatsTag(url);

        /*retryPolicy = new DefaultRetryPolicyImpl();*/

    }

	/*
     * =========================================================================
	 * Getters and Setters
	 * =========================================================================
	 */

    public RequestCacheOptions getRequestCacheOptions() {
        return mRequestCacheOptions;
    }

    public Request<?> setRequestCacheOptions(RequestCacheOptions requestCacheOptions) {
        this.mRequestCacheOptions = requestCacheOptions;
        return this;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public Request<?> setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    public String getUrl() {
        return mUrl;
    }

    public Request<?> setUrl(String url) {
        this.mUrl = url;
        this.cacheKey = url;
        return this;
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    public void setRequestQueue(RequestQueue mRequestQueue) {
        this.mRequestQueue = mRequestQueue;
    }

    public Object getTag() {
        return mTag;
    }

    public void setTag(Object mTag) {
        this.mTag = mTag;
    }

    /**
     * Mark this request as canceled. No callback will be delivered.
     */
    public void cancel() {
        mCanceled = true;
    }

    /**
     * Returns true if this request has been canceled.
     */
    public boolean isCanceled() {
        return mCanceled;
    }

    public void markDelivered() {
        mResponseDelivered = true;
    }

    public void resetDelivered() {
        mResponseDelivered = false;
    }

    public boolean hasHadResponseDelivered() {
        return mResponseDelivered;
    }

    public Request<?> setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
        return this;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public int getTrafficStatsTag() {
        return mDefaultTrafficStatsTag;
    }

    /**
     * @return The hashcode of the URL's host component, or 0 if there is none.
     */
    private static int findDefaultTrafficStatsTag(String url) {
        if (!TextUtils.isEmpty(url)) {
            Uri uri = Uri.parse(url);
            if (uri != null) {
                String host = uri.getHost();
                if (host != null) {
                    return host.hashCode();
                }
            }
        }
        return 0;
    }

    public Integer getSequence() {
        return mSequence;
    }

    public void setSequence(Integer mSequence) {
        this.mSequence = mSequence;
    }

    public void setPriority(Priority priority) {
        this.mPriority = priority;
    }

    /**
     * Returns the {@link net.robinx.lib.http.config.Priority} of this request;
     * {@link net.robinx.lib.http.config.Priority#NORMAL} by default.
     */
    public Priority getPriority() {
        return mPriority;
    }

    public OnRequestListener<T> getOnRequestListener() {
        return onRequestListener;
    }

    public Request<?> setOnRequestListener(OnRequestListener<T> onRequestListener) {
        this.onRequestListener = onRequestListener;
        return this;
    }

    public int getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(int httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Map<String, String> getHeaders() {
        return Collections.emptyMap();
    }

    protected String getParamsEncoding() {
        return DEFAULT_PARAMS_ENCODING;
    }

    public String getBodyContentType() {
        return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
    }

	/*
     * =========================================================================
	 * Override IRequest
	 * =========================================================================
	 */

    @Override
    public void requestPrepare() {
        if (mCanceled) {
            return;
        }

        if (mRequestCacheOptions == null) {
            throw new IllegalArgumentException("please use \"setRequestCacheOptions\" method to set a RequestCacheOptions Instance");
        }

        CLog.w("<requestPrepare> thread name: %s", Thread.currentThread().getName());

        CLog.d("network-http-prepare");

        if (onRequestListener != null) {
            onRequestListener.onRequestPrepare(this);
        }

    }

    @Override
    public void onRequestFinish(Map<String, String> headers, T result) {
        if (mCanceled) {
            return;
        }

        CLog.w("<onRequestFinish> thread name: %s", Thread.currentThread().getName());

        CLog.d("network-http-complete");

        if (onRequestListener != null) {
            onRequestListener.onRequestFinish(this, headers, result);
        }

        // If the data has not been delivered, then delivery
        if (!hasHadResponseDelivered()) {
            deliveryResponse(this, headers, result, DataType.NETWORK_DATA);

            // Post the response back.
            this.markDelivered();
        }

        // // Write to cache if applicable.
        // if (this.getRequestCacheOptions().isShouldCache()) {
        // handlerCache(headers, result);
        // }

        // if already delivered
        if (hasHadResponseDelivered()) {
            // release the same request in the "mWaitingRequests" map
            finish();
        }

    }

    @Override
    public void onRequestFailed(HttpException httpException) {
        if (mCanceled) {
            return;
        }

        CLog.w("<onRequestFailed> thread name: %s", Thread.currentThread().getName());

        CLog.d("network-http-failed : " + httpException.toString());

        if (!hasHadResponseDelivered()) {
            if (onRequestListener != null) {
                onRequestListener.onRequestFailed(this, httpException);
            }
            if (mRequestCacheOptions.isUseCacheDataWhenRequestFailed()) {
                // read cache
                CacheData<Entry<T>> cacheData = getCache(getCacheKey());
                if (cacheData != null) {
                    // deliveryResponse(this,
                    // cacheData.getData().responseHeaders,cacheData.getData().data,
                    // DataType.CACHE_DATA);
                    onCacheDataLoadFinish(cacheData);
                }
            }

            // Post the response back.
            this.markDelivered();
        }

        // if already delivered
        if (hasHadResponseDelivered()) {
            // release the same request in the "mWaitingRequests" map
            finish();
        }

    }

    @Override
    public void onRequestRetry(int currentRetryCount, HttpException previousError) {
        if (mCanceled) {
            return;
        }

        CLog.w("<onRequestRetry> thread name: %s", Thread.currentThread().getName());

        CLog.d("network-http-retry");

        if (onRequestListener != null) {
            onRequestListener.onRequestRetry(this, currentRetryCount, previousError);
        }

    }

    @Override
    public void onRequestDownloadProgress(long transferredBytesSize, long totalSize) {
        if (mCanceled) {
            return;
        }

        CLog.w("<onRequestDownloadProgress> thread name: %s", Thread.currentThread().getName());

        if (onRequestListener != null) {
            onRequestListener.onRequestDownloadProgress(this, transferredBytesSize, totalSize);
        }

    }

    @Override
    public void onRequestUploadProgress(long transferredBytesSize, long totalSize, int currentFileIndex, File currentFile) {
        if (mCanceled) {
            return;
        }

        CLog.w("<onRequestUploadProgress> thread name: %s", Thread.currentThread().getName());

        if (onRequestListener != null) {
            onRequestListener.onRequestUploadProgress(this, transferredBytesSize, totalSize, currentFileIndex, currentFile);
        }
    }

    @Override
    public void onCacheDataLoadFinish(CacheData<Entry<T>> cacheData) {
        if (mCanceled) {
            return;
        }

        CLog.w("<onCacheDataLoadFinish> thread name: %s", Thread.currentThread().getName());

        if (!hasHadResponseDelivered()) {
            if (onRequestListener != null) {
                onRequestListener.onCacheDataLoadFinish(this, cacheData.getEntry().getResponseHeaders(), cacheData.getEntry().getResult());
            }
            deliveryResponse(this, cacheData.getEntry().getResponseHeaders(), cacheData.getEntry().getResult(), DataType.CACHE_DATA);
            // Post the response back.
            this.markDelivered();
        }

        // if already delivered
        if (hasHadResponseDelivered()) {
            // release the same request in the "mWaitingRequests" map
            finish();
        }

    }

    @Override
    public CacheData<Entry<T>> getCache(String key) {
        CacheData<Entry<T>> cacheData = MemoryCache.INSTANCE.get(key);
        if (cacheData != null) {
            CLog.d("cache-hint-memory");
            return cacheData;
        }

        cacheData = DiskCache.INSTANCE.get(key);
        if (cacheData != null) {
            CLog.d("cache-hint-disk");
            return cacheData;
        }
        return null;
    }

    @Override
    public void onParseNetworkResponse(NetworkResponse networkResponse, T result) {
        if (mCanceled) {
            return;
        }

        CLog.w("<onParseNetworkResponse> thread name: %s", Thread.currentThread().getName());

        boolean canCache = true;
        if (onRequestListener != null) {
            canCache = onRequestListener.onParseNetworkResponse(this, networkResponse, result);
        }

        // Write to cache if applicable.
        if (this.getRequestCacheOptions().isShouldCache() && canCache) {
            handlerCache(networkResponse.headers, result);
        }

    }

    @Override
    public void finish() {
        if (mRequestQueue != null) {
            mRequestQueue.finish(this);
        }
    }

    private void handlerCache(Map<String, String> headers, T result) {
        if (mCanceled) {
            return;
        }
        if (result == null) {
            return;
        }

        CLog.w("<handlerCache> thread name: %s", Thread.currentThread().getName());

        // write memory cache
        CacheData<Entry<T>> cacheData = MemoryCache.INSTANCE.get(getCacheKey());
        if (cacheData != null) {
            cacheData.setWriteTime(System.currentTimeMillis());
            CLog.d("cache-memory-update");
        } else {
            Entry<T> entry = new Entry<T>(result, headers);
            cacheData = new CacheData<Entry<T>>(entry, mRequestCacheOptions.getTimeController().getExpirationTime(), System.currentTimeMillis(), getRequestCacheOptions().isNeverExpired());
            MemoryCache.INSTANCE.put(cacheKey, cacheData);
            CLog.d("cache-memory-written");
        }
        // write disk cache
        CacheData<Entry<T>> diskCacheData = DiskCache.INSTANCE.get(getCacheKey());
        Entry<T> entry = new Entry<T>(result, headers);
        CacheData<Entry> finalCacheData = new CacheData<Entry>(entry, mRequestCacheOptions.getTimeController().getExpirationTime(), System.currentTimeMillis(), getRequestCacheOptions().isNeverExpired());
        if (diskCacheData != null) {
            DiskCache.INSTANCE.update(getCacheKey(), finalCacheData);
            CLog.d("cache-disk-update");
        } else {
            DiskCache.INSTANCE.put(getCacheKey(), finalCacheData);
            CLog.d("cache-disk-written");
        }

    }

	/*
     * =========================================================================
	 *  Override IResponseDelivery
	 * =========================================================================
	 */

    @Override
    public void deliveryResponse(Request<?> request, Map<String, String> headers, T result, DataType dataType) {
        CLog.w("<deliveryResponse( onDone )> thread name: %s", Thread.currentThread().getName());

        if (onRequestListener != null) {
            onRequestListener.onDone(this, headers, result, dataType);
        }

        // LOG
        String requestMethod = "";
        switch (getHttpMethod()) {
            case HttpMethod.GET:
                requestMethod = "GET";
                break;
            case HttpMethod.DELETE:
                requestMethod = "DELETE";
                break;
            case HttpMethod.POST:
                requestMethod = "POST";
                break;
            case HttpMethod.PUT:
                requestMethod = "PUT";
                break;
            case HttpMethod.HEAD:
                requestMethod = "HEAD";
                break;
            case HttpMethod.OPTIONS:
                requestMethod = "OPTIONS";
                break;
            case HttpMethod.TRACE:
                requestMethod = "TRACE";
                break;
            case HttpMethod.PATCH:
                requestMethod = "PATCH";
                break;
        }

        try {
            CLog.i("|Response Delivered|" + "\n[DataType] : " + dataType + "\n[CacheKey] : " + getCacheKey() + "\n[Tag] : " + getTag() + "\n[URL] : " + getUrl() + "\n[Method] : " + requestMethod + "\n[Headers] : " + getHeaders() + "\n[Params] : " + getParams() + "\n[Result Headers] : " + headers + "\n[Result Data] : " + result);
        } catch (Exception e) {
            e.printStackTrace();
            CLog.e("deliveryResponse（onDone）print log error：%s", e.toString());
        }

    }

	/*
	 * =========================================================================
	 * Abstract
	 * =========================================================================
	 */

    /**
     * Used for printing log
     *
     * @return
     */
    public abstract String getParams();

    /**
     * If the request parameters, the subclass must be rewritten
     */
    public abstract <T> T buildBody(Object... args);

    /**
     * The network request results resolved as want to format, subclasses must
     * override
     *
     * @param response
     * @return
     */
    public abstract Response<T> parseNetworkResponse(NetworkResponse response);

    /*
     * =========================================================================
     * Override Comparable
     * =========================================================================
     */
    @Override
    public int compareTo(Request<T> another) {
        Priority left = this.getPriority();
        Priority right = another.getPriority();

        // High-priority requests are "lesser" so they are sorted to the front.
        // Equal priorities are sorted by sequence number to provide FIFO
        // ordering.
        return left == right ? this.mSequence - another.mSequence : right.ordinal() - left.ordinal();
    }

}
