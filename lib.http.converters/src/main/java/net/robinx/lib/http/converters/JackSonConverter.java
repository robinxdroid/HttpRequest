package net.robinx.lib.http.converters;

import net.robinx.lib.http.converter.Converter;
import net.robinx.lib.http.utils.CLog;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Robin on 2016/5/11 19:55.
 */
public class JackSonConverter<T> implements Converter<T> {

    private ObjectMapper mapper;

    public JackSonConverter() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            CLog.w("Create JackSon");
        }
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
        try {
            JavaType javaType = mapper.getTypeFactory().constructType(cls);
            T bean = mapper.readValue(json, javaType);
            return bean;
        } catch (IOException e) {
            e.printStackTrace();
            CLog.e("JackSonConverter:%s", e.getMessage());
            return null;
        }

    }

    public ArrayList<T> fromList(String json, Type cls) {
        try {
            ArrayList<T> list = mapper.readValue(json, new TypeReference<List<T>>() {
            });
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            CLog.e("JackSonConverter:%s", e.getMessage());
            return null;
        }

    }

}
