package net.robinx.lib.http.converter;

import java.lang.reflect.Type;

/**
 * Created by Robin on 2016/5/11 16:03.
 */
public interface Converter<T> {
    T fromJSONObject(String json, Type cls);
    T fromJSONArray(String json, Type cls);
}
