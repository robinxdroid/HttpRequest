package net.robinx.lib.http.network.ex;

import net.robinx.lib.http.XRequest;
import net.robinx.lib.http.base.Request;
import net.robinx.lib.http.callback.BytesWriteListener;
import net.robinx.lib.http.network.ex.hurl.HurlRequestBody;

import java.io.File;
import java.util.Map;

/**
 * Created by Robin on 2016/4/30.
 */
public abstract class HttpRequest<T> extends Request<T> {
    protected RequestParams mRequestParams = new RequestParams();

    protected Body mBody;

    public HttpRequest() {
        super();

        mBody = new HurlRequestBody();
        mBody.setBytesWriteListener(getBytesWriteListener());
    }

    protected BytesWriteListener getBytesWriteListener() {
        return new BytesWriteListener() {
            @Override
            public void onWrite(long transferredBytesSize, long totalSize, int currentFileIndex, File currentFile) {
                //super.getRequestQueue().getDelivery().postRequestUploadProgress(this, transferredBytesSize,totalSize);  //UI Thread
                onRequestUploadProgress(transferredBytesSize, totalSize, currentFileIndex, currentFile); //Thread
            }
        };
    }

    @Override
    public Map<String, String> getHeaders() {
        return mRequestParams.buildHeaders();
    }

    @Override
    public String getParams() {
        return mRequestParams.toString();
    }

    @Override
    public <R> R buildBody(Object... args) {
        return mBody.buildBody(mRequestParams, args);
    }

    public void setRequestParams(RequestParams requestParams) {
        this.mRequestParams = requestParams;
    }

    public RequestParams getRequestParams() {
        return mRequestParams;
    }

    public HttpRequest<T> execute(){
        XRequest.INSTANCE.addToRequestQueue(this);
        return this;
    }

}
