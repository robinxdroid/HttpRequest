package net.robinx.lib.http.callback;

/**
 * the operation with cache callback
 * @author Robin
 * @since 2015-05-08 00:26:40
 */
public interface OnCacheDataListener <ReturnDataType>{

	public void onFinish(ReturnDataType data);
}
