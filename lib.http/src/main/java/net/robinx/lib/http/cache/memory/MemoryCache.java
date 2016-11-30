package net.robinx.lib.http.cache.memory;

import net.robinx.lib.http.cache.Cache;
import net.robinx.lib.http.cache.CacheConfig;
import net.robinx.lib.http.cache.CacheData;
import net.robinx.lib.http.cache.Entry;

/**
 * Created by Robin on 2016/5/7.
 */
public enum  MemoryCache implements Cache<CacheData<Entry>>{

    INSTANCE;

    private LruCache<String, CacheData<Entry>> mMemoryCache;

    private MemoryCache(){
        if (mMemoryCache==null) {
            if (CacheConfig.MEMORY_CACHE_MAX_SIZE <= 0) {
                throw new RuntimeException("Maybe you forgot to initialize,like \"XRequest.init(context)\"");
            }
            mMemoryCache=new LruCache<String,CacheData<Entry>>(CacheConfig.MEMORY_CACHE_MAX_SIZE){
                @Override
                protected int sizeOf(String key, CacheData<Entry> value) {
                    return super.sizeOf(key, value);
                }
            };
        }
    }


    @Override
    public void put(String key, CacheData value) {
        if (get(key)==null) {
            mMemoryCache.put(key, value);
        }
    }

    @Override
    public CacheData get(String key) {
        return mMemoryCache.get(key);
    }

    @Override
    public void delete(String key) {
        mMemoryCache.remove(key);
    }

    @Override
    public void update(String key, CacheData value) {
        mMemoryCache.put(key, value);
    }

    @Override
    public void clear() {
        mMemoryCache.evictAll();
    }
}
