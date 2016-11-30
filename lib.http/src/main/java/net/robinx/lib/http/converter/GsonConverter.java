package net.robinx.lib.http.converter;

import net.robinx.lib.http.utils.CLog;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by Robin on 2016/5/11 16:07.
 */
public class GsonConverter<T> implements Converter<T> {

    private Gson gson;

    public GsonConverter() {
        if (gson == null) {
            gson = new Gson();
            CLog.w("Create GSON");
        }
    }

    public T fromJSONObject(String json, Type cls) {
        CLog.w("From JSON Object");
        return fromObject(json, cls);
    }

    @Override
    public T fromJSONArray(String json, Type cls) {
        CLog.w("From JSON Array");
        return (T) fromList(json, cls);
    }

    public <X> X fromObject(String json, Type cls) {
        X bean = gson.fromJson(json, cls);
        return bean;
    }

    public <X> ArrayList<X> fromList(String json, Type cls) {
        ArrayList<X> list;
        try {
            list = gson.fromJson(json, cls);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return list;

    }
}
