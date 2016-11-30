package net.robinx.lib.http.network.ex;

import android.text.TextUtils;

import net.robinx.lib.http.callback.OnRequestListener;
import net.robinx.lib.http.config.HttpMethod;
import net.robinx.lib.http.config.Priority;
import net.robinx.lib.http.config.RequestCacheOptions;
import net.robinx.lib.http.response.NetworkResponse;
import net.robinx.lib.http.response.Response;
import net.robinx.lib.http.retry.DefaultRetryPolicyImpl;
import net.robinx.lib.http.retry.RetryPolicy;
import net.robinx.lib.http.utils.CLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Download request
 *
 * @author Robin
 * @since 2016-1-14 18:53:03
 */
public class DownloadRequest extends HttpRequest<File> {

    private String mDownloadPath;
    private String mFileName;

    private DownloadRequest(Builder builder) {
        super();
        this.mDownloadPath = builder.downloadPath;
        this.mFileName = builder.fileName;

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
    public Response<File> parseNetworkResponse(NetworkResponse response) {
        File downloadFile = null;
        try {
            byte[] data = response.data;
            //convert array of bytes into file
            File directory = new File(mDownloadPath);
            if (!directory.exists()) {
                directory.mkdir();
            }

            String path = mDownloadPath;
            if (!TextUtils.isEmpty(mFileName)) {
                path = mDownloadPath + File.separator + mFileName;
            }
            FileOutputStream fileOuputStream = new FileOutputStream(path);
            fileOuputStream.write(data);
            fileOuputStream.close();
            downloadFile = new File(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            CLog.e("Download directory %s is not exsit", mDownloadPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Response.success(downloadFile, response.headers);
    }


    /*
     * =========================================================================
     * Inner class
     * =========================================================================
     */
    public static class Builder implements net.robinx.lib.http.base.Builder<HttpRequest<File>> {

        //Local property
        private final String downloadPath;
        private final String fileName;

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
        private OnRequestListener<File> onRequestListener;
        private int httpMethod;

        public Builder(String downloadPath, String fileName) {
            this.downloadPath = downloadPath;
            this.fileName = fileName;
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

        public Builder onRequestListener(OnRequestListener<File> onRequestListener) {
            this.onRequestListener = onRequestListener;
            return this;
        }

        public Builder httpMethod(int httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        @Override
        public HttpRequest<File> build() {
            return new DownloadRequest(this);
        }
    }

}
