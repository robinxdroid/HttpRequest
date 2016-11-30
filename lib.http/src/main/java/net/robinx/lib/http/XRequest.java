package net.robinx.lib.http;

import net.robinx.lib.http.base.Request;
import net.robinx.lib.http.cache.CacheConfig;
import net.robinx.lib.http.config.RequestCacheOptions;
import net.robinx.lib.http.network.HttpStack;
import net.robinx.lib.http.queue.RequestQueue;
import net.robinx.lib.http.utils.AppUtils;

import android.content.Context;

/**
 * Encapsulates the request，for the convenience of call
 *
 * @author Robin
 * @since 2015-08-12 17:43:03
 */
public enum  XRequest {
    INSTANCE;
    private XRequest(){

    }

    public static void initXRequest(Context context) {
        RequestContext.init(context);
        CacheConfig.DISK_CACHE_MAX_SIZE = CacheConfig.DEFAULT_MAX_SIZE;
        CacheConfig.DISK_CACHE_DIRECTORY = AppUtils.getDiskCacheDir(context, "xrequest");
        CacheConfig.DISK_CACHE_APP_VERSION = AppUtils.getAppVersion(context);
        CacheConfig.MEMORY_CACHE_MAX_SIZE = (int) Runtime.getRuntime().maxMemory() / 8;
    }

    private RequestQueue queue;

    /**
     * Best during application initialization calls only once
     *
     * @param threadPoolSize
     */
    public void setRequestThreadPoolSize(int threadPoolSize) {
        if (queue != null) {
            queue.stop();
            queue = null;
        }
        queue = new RequestQueue(threadPoolSize);
        queue.start();
    }


    /**
     * Add a request to queue to execute
     *
     * @param request Target request
     */
    public void addToRequestQueue(Request<?> request) {
        if (queue == null) {
            queue = new RequestQueue();
            queue.start();
        }
        queue.add(request);
    }

    public void setStack(HttpStack stack){
        //Need to initialize the queue
        shutdown();
        if (queue != null) {
            queue.stop();
            queue = null;
        }
        queue = new RequestQueue(stack);
        queue.start();
    }

    /**
     * Create a default cache configuration
     *
     * @return
     */
    public RequestCacheOptions getDefaultRequestCacheOptions() {
        return RequestCacheOptions.buildDefaultCacheOptions();
    }

    /**
     * To cancel a request that is requesting
     *
     * @param request
     */
    public void cancelRequest(Request<?> request) {
        if (null != request) {
            request.cancel();
        }
    }

    /**
     * Cancel all of this request in the request queue , not including is
     * requested
     *
     * @param tag If there is no special Settings, then introduction the
     *            instance of activity
     */
    public void cancelAllRequestInQueueByTag(Object tag) {
        if (queue != null) {
            queue.cancelAll(tag);
        }
    }

    /**
     * Start the request，start the thread pool
     */
    public void start() {
        if (queue != null) {
            queue.start();
        }
    }

    /**
     * Close the request, quit all threads, release the request queue
     */
    public void shutdown() {
        if (queue != null) {
            queue.stop();
            queue = null;
        }
    }

}
