package net.robinx.lib.http.network.ex;

import android.text.TextUtils;

import net.robinx.lib.http.utils.CLog;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Request parameters
 *
 * @author Robin
 * @since 2016-01-08 14:21:27
 */
public class RequestParams extends ConcurrentHashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    private final Map<String, String> mHeaders = new ConcurrentHashMap<String, String>();
    private String mJsonParams;

	/*
     * =========================================================================
	 * Constructor
	 * =========================================================================
	 */

    public RequestParams() {
    }

    public RequestParams(String cookie) {
        mHeaders.put("cookie", cookie);
    }

	/*
	 * =========================================================================
	 * Override Super
	 * =========================================================================
	 */

    @Override
    public Object put(String key, Object value) {
        if (value instanceof String || value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Float || value instanceof Double || value instanceof File) {
            return super.put(key, value);
        } else {
            CLog.e("Parameters need to be a file type or can be converted to string type");
            throw new IllegalArgumentException("Parameters need to be a file type or can be converted to string type");
        }
    }

	/*
	 * =========================================================================
	 * Public Method
	 * =========================================================================
	 */

    public void putParams(String key, int value) {
        this.putParams(key, String.valueOf(value));
    }

    public void putParams(String key, String value) {
        put(key, value);
    }

    public void putParams(String key, File value) {
        put(key, value);
    }

    public void putParams(String jsonString) {
        this.mJsonParams = jsonString;
    }

    public void putHeaders(String key, int value) {
        this.putHeaders(key, String.valueOf(value));
    }

    public void putHeaders(String key, long value) {
        this.putHeaders(key, String.valueOf(value));
    }

    public void putHeaders(String key, short value) {
        this.putHeaders(key, String.valueOf(value));
    }

    public void putHeaders(String key, float value) {
        this.putHeaders(key, String.valueOf(value));
    }

    public void putHeaders(String key, double value) {
        this.putHeaders(key, String.valueOf(value));
    }

    public void putHeaders(String key, String value) {
        mHeaders.put(key, value);
    }

    public String buildJsonParams() {
        return mJsonParams;
    }

    /**
     * Converts params into an application/x-www-form-urlencoded encoded string.
     */
	/*public StringBuilder buildParameters() {
		StringBuilder result = new StringBuilder();
		try {
			for (Entry<String, Object> entry : this.entrySet()) {
				Object value = entry.getValue();
				if (value == null) {
					continue;
				}
				if (value instanceof String || value instanceof Integer) {
					result.append("&");
					result.append(URLEncoder.encode(entry.getKey(), "utf-8"));
					result.append("=");
					result.append(URLEncoder.encode(String.valueOf(value), "utf-8"));
				} else {
					CLog.e("Filter value,Type : %s,Value : %s", value.getClass().getName());
				}
			}
			return result;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Encoding not supported: " + "utf-8", e);
		}

	}*/
    public StringBuilder buildQueryParameters() {
        StringBuilder result = new StringBuilder();
        boolean isFirst = true;
        try {
            for (Entry<String, Object> entry : this.entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                if (value instanceof String || value instanceof Integer) {
                    if (!isFirst) {
                        result.append("&");
                    } else {
                        result.append("?");
                        isFirst = false;
                    }
                    result.append(URLEncoder.encode(entry.getKey(), "utf-8"));
                    result.append("=");
                    result.append(URLEncoder.encode(String.valueOf(value), "utf-8"));
                } else {
                    CLog.e("Filter value,Type : %s,Value : %s", value.getClass().getName());
                }

            }
            return result;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding not supported: " + "utf-8", e);
        }

    }

    public Map<String, String> buildHeaders() {
        return mHeaders;
    }

    public boolean hasFileInParams() {
        for (Entry<String, Object> entry : this.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof File) {
                return true;
            }
        }
        return false;
    }

    public boolean hasJsonInParams() {
        return !TextUtils.isEmpty(mJsonParams);
    }

    @Override
    public String toString() {
        if (!TextUtils.isEmpty(mJsonParams)) {
            return mJsonParams;
        }
        return super.toString();
    }

}
