package net.robinx.lib.http.delivered;

import java.io.File;

import net.robinx.lib.http.base.Request;
import net.robinx.lib.http.network.HttpException;
import net.robinx.lib.http.response.Response;

/**
 * The distributor, the result of the asynchronous thread to the UI thread
 * 
 * @author Robin
 * @since 2015-07-02 14:49:51
 * 
 */
public interface IDelivery {
    /**
     * Distribution request response result
     * 
     * @param request
     * @param response
     */
    public void postRequestResponse(Request<?> request, Response<?> response);

    /**
     * Distribute Failure events
     * 
     * @param request
     * @param error
     */
    public void postError(Request<?> request, HttpException error);

    /**
     * Distribution to read the cached results response
     * @param request
     * @param cacheData
     */
    public <T>void postCacheResponse(Request<?> request, T cacheData);
    
    /**
     * Prepare events at the start of the request
     * @param request
     */
    public void postRequestPrepare(Request<?> request);
    
    /**
     * Distribution to retry event
     * @param request
     * @param previousError An exception before
     */
    public void postRequestRetry(Request<?> request, int currentRetryCount, HttpException previousError);
    
    /**
     *  Delivered current progress for request
     * @param request
     * @param transferredBytesSize
     * @param totalSize
     */
    public void postRequestDownloadProgress(Request<?> request, long transferredBytesSize, long totalSize);
    
    /**
     * Delivered upload progress for request
     * @param request
     * @param transferredBytesSize
     * @param totalSize
     * @param currentFileIndex The subscript is currently being uploaded file
     * @param currentFile Is currently being uploaded files
     */
	public void postRequestUploadProgress(Request<?> request, long transferredBytesSize, long totalSize, int currentFileIndex, File currentFile);

}
