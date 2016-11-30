package net.robinx.lib.http.network.ex;

import net.robinx.lib.http.callback.BytesWriteListener;

/**
 * Created by Robin on 2016/5/4.
 */
public interface Body {
    <T> T buildBody(RequestParams params,Object... args);

    void setBytesWriteListener(BytesWriteListener listener);
}
