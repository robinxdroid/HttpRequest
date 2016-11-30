package net.robinx.lib.http.network.ex;

import net.robinx.lib.http.base.Request;
import net.robinx.lib.http.callback.OnRequestListener;
import net.robinx.lib.http.config.DataType;
import net.robinx.lib.http.network.HttpException;
import net.robinx.lib.http.response.NetworkResponse;
import net.robinx.lib.http.utils.CLog;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Robin on 2016/11/21 16:03.
 *  Is used to create a synchronous request
 */

public class SyncRequest<T> implements Future<T>,OnRequestListener<T> {

    private Request<?> mRequest;
    private boolean mResultReceived = false;
    private T mResult;
    private HttpException mHttpException;

    public static <E> SyncRequest<E> newSyncRequest() {
        return new SyncRequest<>();
    }

    private SyncRequest() {}

    public void setRequest(Request<?> request) {
        mRequest = request;
    }


    /*==========================================================
       Override Future
     *==========================================================
     */

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (mRequest == null) {
            return false;
        }

        if (!isDone()) {
            mRequest.cancel();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return doGet(null);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return doGet(TimeUnit.MILLISECONDS.convert(timeout, unit));
    }

    private synchronized T doGet(Long timeoutMs) throws InterruptedException, ExecutionException, TimeoutException {
        if (mHttpException != null) {
            throw new ExecutionException(mHttpException);
        }

        if (mResultReceived) {
            return mResult;
        }

        if (timeoutMs == null) {
            wait(0);
        } else if (timeoutMs > 0) {
            wait(timeoutMs);
        }

        if (mHttpException != null) {
            throw new ExecutionException(mHttpException);
        }

        if (!mResultReceived) {
            throw new TimeoutException();
        }

        return mResult;
    }

    @Override
    public boolean isCancelled() {
        if (mRequest == null) {
            return false;
        }
        return mRequest.isCanceled();
    }

    @Override
    public synchronized boolean isDone() {
        return mResultReceived || mHttpException != null || isCancelled();
    }

    /*==========================================================
       Override OnRequestListener
     *==========================================================
     */


    @Override
    public synchronized void onRequestPrepare(Request<?> request) {
        CLog.d("SyncRequest <onRequestPrepare>");
    }

    @Override
    public synchronized void onRequestFailed(Request<?> request, HttpException httpException) {
        CLog.d("SyncRequest <onRequestFailed>");
        mHttpException = httpException;
        notifyAll();
    }

    @Override
    public synchronized void onRequestRetry(Request<?> request, int currentRetryCount, HttpException previousError) {
        CLog.d("SyncRequest <onRequestRetry>");
    }

    @Override
    public synchronized void onRequestDownloadProgress(Request<?> request, long transferredBytesSize, long totalSize) {
        CLog.d("SyncRequest <onRequestDownloadProgress>");
    }

    @Override
    public synchronized void onRequestUploadProgress(Request<?> request, long transferredBytesSize, long totalSize, int currentFileIndex, File currentFile) {
        CLog.d("SyncRequest <onRequestUploadProgress>");
    }

    @Override
    public synchronized void onRequestFinish(Request<?> request, Map<String, String> headers, T result) {
        CLog.d("SyncRequest <onRequestFinish>");
    }

    @Override
    public synchronized void onCacheDataLoadFinish(Request<?> request, Map<String, String> headers, T result) {
        CLog.d("SyncRequest <onCacheDataLoadFinish>");
    }

    @Override
    public synchronized boolean onParseNetworkResponse(Request<?> request, NetworkResponse networkResponse, T result) {
        CLog.d("SyncRequest <onParseNetworkResponse>");
        return true;
    }

    @Override
    public synchronized void onDone(Request<?> request, Map<String, String> headers, T result, DataType dataType) {
        CLog.d("SyncRequest <onDone>");
        mResultReceived = true;
        mResult = result;
        notifyAll();
    }
}
