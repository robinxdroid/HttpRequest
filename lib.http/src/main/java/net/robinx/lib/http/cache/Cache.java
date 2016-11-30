package net.robinx.lib.http.cache;

/**
 * Created by Robin on 2016/5/7.
 */
public interface Cache<V> {
    void put(String key, V value);

    V get(String key);

    void delete(String key);

    void update(String key, V value);

    void clear();
}
