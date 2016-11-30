package net.robinx.lib.http.converters;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import net.robinx.lib.http.converter.Converter;
import net.robinx.lib.http.utils.CLog;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Robin on 2016/5/11 19:55.
 */
public class FastjsonConverter<T> implements Converter<T> {


    public FastjsonConverter() {
        CLog.w("Create Fastjson");
    }

    @Override
    public T fromJSONObject(String json, Type cls) {
        CLog.w("From JSON Object");
        return fromObject(json, cls);
    }

    @Override
    public T fromJSONArray(String json, Type cls) {
        CLog.w("From JSON Array");
        return (T) fromList(json, cls);
    }

    public T fromObject(String json, Type cls) {
        T bean = JSON.parseObject(json,cls);
        return bean;
    }

    public ArrayList<T> fromList(String json, Type cls) {
        ArrayList<T> list= (ArrayList<T>) JSON.parseObject(json, new TypeReference<List<T>>(){});
        return list;
    }
}
