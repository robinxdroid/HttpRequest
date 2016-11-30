package net.robinx.lib.http.queue;

import android.os.Handler;
import android.os.Looper;

import net.robinx.lib.http.base.Request;
import net.robinx.lib.http.delivered.DeliveryImpl;
import net.robinx.lib.http.delivered.IDelivery;
import net.robinx.lib.http.dispatcher.CacheDispatcher;
import net.robinx.lib.http.dispatcher.NetworkDispatcher;
import net.robinx.lib.http.network.HttpStack;
import net.robinx.lib.http.network.Network;
import net.robinx.lib.http.network.ex.hurl.HurlStack;
import net.robinx.lib.http.utils.CLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A request dispatch queue with a thread pool of dispatchers.
 *@author Robin
 *@since 2015-05-08 13:35:10
 */
public class RequestQueue {
	
    /** Used for generating monotonically-increasing sequence numbers for requests. */
    private AtomicInteger mSequenceGenerator = new AtomicInteger();

    /**
     * Staging area for requests that already have a duplicate request in flight.
     *
     * <ul>
     *     <li>containsKey(cacheKey) indicates that there is a request in flight for the given cache
     *          key.</li>
     *     <li>get(cacheKey) returns waiting requests for the given cache key. The in flight request
     *          is <em>not</em> contained in that list. Is null if no requests are staged.</li>
     * </ul>
     */
    private final Map<String, Queue<Request<?>>> mWaitingRequests =
            new HashMap<String, Queue<Request<?>>>();

    /**
     * The set of all requests currently being processed by this RequestQueue. A Request
     * will be in this set if it is waiting in any queue or currently being processed by
     * any dispatcher.
     */
    private final Set<Request<?>> mCurrentRequests = new HashSet<Request<?>>();

    /** The cache triage queue. */
    private final PriorityBlockingQueue<Request<?>> mCacheQueue =
        new PriorityBlockingQueue<Request<?>>();

    /** The queue of requests that are actually going out to the network. */
    private final PriorityBlockingQueue<Request<?>> mNetworkQueue =
        new PriorityBlockingQueue<Request<?>>();

    /** Number of network request dispatcher threads to start. */
    private static final int DEFAULT_NETWORK_THREAD_POOL_SIZE = 4;


    /** The network dispatchers. */
    private NetworkDispatcher[] mDispatchers;

    /** The cache dispatcher. */
    private CacheDispatcher mCacheDispatcher;
    
    /** Network request actuator **/
    public Network mNetwork;
    
    /** Http request results and the cache results for the response of the distributor **/
    public IDelivery mDelivery;

    private List<RequestFinishedListener> mFinishedListeners = new ArrayList<RequestFinishedListener>();
    
    /**
     * Creates the worker pool. Processing will not begin until {@link #start()} is called.
     */
    public RequestQueue() {
        this(DEFAULT_NETWORK_THREAD_POOL_SIZE,new HurlStack());
    }

    /**
     * Creates the worker pool. Processing will not begin until {@link #start()} is called.
     *
     * @param threadPoolSize Number of network dispatcher threads to create
     */
    public RequestQueue(int threadPoolSize) {
        this(threadPoolSize,new HurlStack());
    }

    /**
     * Creates the worker pool. Processing will not begin until {@link #start()} is called.
     *
     * @param stack
     */
    public RequestQueue(HttpStack stack) {
        this(DEFAULT_NETWORK_THREAD_POOL_SIZE,stack);
    }
    
    /**
     * Creates the worker pool. Processing will not begin until {@link #start()} is called.
     *
     * @param threadPoolSize Number of network dispatcher threads to create
     * @param stack
     */
    public RequestQueue(int threadPoolSize,HttpStack stack) {
        mDispatchers = new NetworkDispatcher[threadPoolSize];
        mNetwork = new Network(stack);
    	mDelivery= new DeliveryImpl(new Handler(Looper.getMainLooper()));
    }

    /**
     * Starts the dispatchers in this queue.
     */
    public void start() {
        stop();  // Make sure any currently running dispatchers are stopped.
        // Create the cache dispatcher and start it.
        mCacheDispatcher = new CacheDispatcher(mCacheQueue, mNetworkQueue,mDelivery);
        mCacheDispatcher.start();

        // Create network dispatchers (and corresponding threads) up to the pool size. 
        //Use thread array, thread death not rebuild and test the upload and download at the same time, poor performance, often with no connection
        /*for (int i = 0; i < mDispatchers.length; i++) {
            NetworkDispatcher networkDispatcher = new NetworkDispatcher(mNetworkQueue,mNetwork,mDelivery);
            mDispatchers[i] = networkDispatcher;
            networkDispatcher.start();
        }*/
        
        // Create network dispatchers (and corresponding threads) up to the pool size.
        //Use fixed number of thread pool, performance is superior to the thread array, test and upload and download, found no big problem
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(mDispatchers.length);
        for (int i = 0; i < mDispatchers.length; i++) {
            NetworkDispatcher networkDispatcher = new NetworkDispatcher(mNetworkQueue, mNetwork,mDelivery);
            mDispatchers[i] = networkDispatcher;
            threadPoolExecutor.submit(networkDispatcher);
        }

    }

    /**
     * Stops the cache and network dispatchers.
     */
    public void stop() {
        if (mCacheDispatcher != null) {
            mCacheDispatcher.quit();
        }
        for (int i = 0; i < mDispatchers.length; i++) {
            if (mDispatchers[i] != null) {
                mDispatchers[i].quit();
            }
        }
    }

    /**
     * Gets a sequence number.
     */
    public int getSequenceNumber() {
        return mSequenceGenerator.incrementAndGet();
    }

    /**
     * A simple predicate or filter interface for Requests, for use by
     * {@link net.robinx.lib.http.queue.RequestQueue#cancelAll(net.robinx.lib.http.queue.RequestQueue.RequestFilter)}.
     */
    public interface RequestFilter {
        public boolean apply(Request<?> request);
    }

    /**
     * Cancels all requests in this queue for which the given filter applies.
     * @param filter The filtering function to use
     */
    public void cancelAll(RequestFilter filter) {
        synchronized (mCurrentRequests) {
            for (Request<?> request : mCurrentRequests) {
                if (filter.apply(request)) {
                    request.cancel();
                }
            }
        }
    }

    /**
     * Cancels all requests in this queue with the given tag. Tag must be non-null
     * and equality is by identity.
     */
    public void cancelAll(final Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Cannot cancelAll with a null tag");
        }
        cancelAll(new RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return request.getTag() == tag;
            }
        });
    }

    /**
     * Adds a Request to the dispatch queue.
     * @param request The request to service
     * @return The passed-in request
     */
    public  <T> Request<T> add(Request<T> request) {
        // Tag the request as belonging to this queue and add it to the set of current requests.
        request.setRequestQueue(this);
        synchronized (mCurrentRequests) {
            mCurrentRequests.add(request);
        }

        // Process requests in the order they are added.
        request.setSequence(getSequenceNumber());
        CLog.v("add-to-queue");

        // If the request is uncacheable, skip the cache queue and go straight to the network.
        if (!request.getRequestCacheOptions().isShouldCache()) {
            mNetworkQueue.add(request);
            return request;
        }

        // Insert request into stage if there's already a request with the same cache key in flight.
        synchronized (mWaitingRequests) {
            String cacheKey = request.getCacheKey();
            if (mWaitingRequests.containsKey(cacheKey)) {
                // There is already a request in flight. Queue up.
                Queue<Request<?>> stagedRequests = mWaitingRequests.get(cacheKey);
                if (stagedRequests == null) {
                    stagedRequests = new LinkedList<Request<?>>();
                }
                stagedRequests.add(request);
                mWaitingRequests.put(cacheKey, stagedRequests);
                CLog.i("Request for cacheKey="+cacheKey+" is in flight, putting on hold.");
            } else {
                // Insert 'null' queue for this cacheKey, indicating there is now a request in
                // flight.
                mWaitingRequests.put(cacheKey, null);
                mCacheQueue.add(request);
            }
            return request;
        }
    }

    /**
     * Called from {@link Request#finish()}, indicating that processing of the given request
     * has finished.
     *
     * <p>Releases waiting requests for <code>request.getCacheKey()</code> if
     *      <code>request.shouldCache()</code>.</p>
     */
    public <T> void finish(Request<T> request) {
        // Remove from the set of requests currently being processed.
        synchronized (mCurrentRequests) {
            mCurrentRequests.remove(request);
        }

        synchronized (mFinishedListeners) {
            for (RequestFinishedListener<T> listener : mFinishedListeners) {
                listener.onRequestFinished(request);
            }
        }

        if (request.getRequestCacheOptions().isShouldCache()) {
            synchronized (mWaitingRequests) {
                String cacheKey = request.getCacheKey();
                Queue<Request<?>> waitingRequests = mWaitingRequests.remove(cacheKey);
                if (waitingRequests != null) {
                    CLog.i("Releasing "+waitingRequests.size()+" waiting requests for cacheKey="+cacheKey);
                    // Process all queued up requests. They won't be considered as in flight, but
                    // that's not a problem as the cache has been primed by 'request'.
                    mCacheQueue.addAll(waitingRequests);
                }
            }
        }
    }

	public IDelivery getDelivery() {
		return mDelivery;
	}

    public  <T> void addRequestFinishedListener(RequestFinishedListener<T> listener) {
        synchronized (mFinishedListeners) {
            mFinishedListeners.add(listener);
        }
    }

    /**
     * Remove a RequestFinishedListener. Has no effect if listener was not previously added.
     */
    public  <T> void removeRequestFinishedListener(RequestFinishedListener<T> listener) {
        synchronized (mFinishedListeners) {
            mFinishedListeners.remove(listener);
        }
    }

    /** Callback interface for completed requests. */
    public static interface RequestFinishedListener<T> {
        /** Called when a request has finished processing. */
        public void onRequestFinished(Request<T> request);
    }
}
