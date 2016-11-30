package net.robinx.lib.http.cache.disk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import net.robinx.lib.http.cache.BitmapCache;
import net.robinx.lib.http.cache.Cache;
import net.robinx.lib.http.cache.CacheConfig;
import net.robinx.lib.http.cache.CacheData;
import net.robinx.lib.http.cache.Entry;
import net.robinx.lib.http.utils.CLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Robin on 2016/5/7 23:17.
 */
public enum DiskCache implements Cache<CacheData<Entry>> {
    INSTANCE;

    private static final int DEFAULT_VALUE_COUNT = 1;

    private DiskLruCache mDiskLruCache;

    private DiskCache() {
        if (mDiskLruCache == null) {
            open();
        }
    }

    public void open() {
        File cacheDir = CacheConfig.DISK_CACHE_DIRECTORY;
        if (cacheDir == null) {
            throw new RuntimeException("Maybe you forgot to initialize,like \"XRequest.init(context)\"");
        }
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        try {
            mDiskLruCache = DiskLruCache.open(cacheDir, CacheConfig.DISK_CACHE_APP_VERSION, DEFAULT_VALUE_COUNT, CacheConfig.DISK_CACHE_MAX_SIZE);
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                throw new RuntimeException("Maybe you forgot to add storage permissions to the AndroidManifest file,or use the SDK compiled more than 23, then you must request permissions in Java code");
            }
            e.printStackTrace();
        }
    }

    /**
     * Using the MD5 algorithm to encrypt the key of the incoming and return.
     */
    public String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }


    @Override
    public void put(String key, CacheData<Entry> value) {
        if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
            open();
        }
        try {
            key = hashKeyForDisk(key);
            DiskLruCache.Editor editor = mDiskLruCache.edit(key);
            if (editor != null) {
                OutputStream outputStream = editor.newOutputStream(0);
                ObjectOutputStream oos = new ObjectOutputStream(outputStream);
                if (value != null) {
                    // handler bitmap
                    Entry<?> entry = value.getEntry();
                    if (entry.result instanceof Bitmap) {
                        Bitmap bitmap = (Bitmap) value.getEntry().result;
                        ByteArrayOutputStream baops = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 0, baops);
                        BitmapCache bitmapCache = new BitmapCache(baops.toByteArray(), "bitmap_cache.png");

                        entry = new Entry<>(bitmapCache, entry.responseHeaders);
                        value.setEntry(entry);
                    }

                    oos.writeObject(value);

                }
                 /*if (oos!=null) {
                    oos.close();
				}*/

                if (get(key) == null) {
                    editor.commit();
                } else {
                    editor.abort();
                }
            }
            mDiskLruCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public CacheData get(String key) {
        if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
            open();
        }
        try {
            key = hashKeyForDisk(key);
            DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
            if (snapShot != null) {
                InputStream is = snapShot.getInputStream(0);
                ObjectInputStream ois = new ObjectInputStream(is);
                try {
                    CacheData<Entry> value = (CacheData<Entry>) ois.readObject();

                    if (value != null) {
                        Entry<?> entry = value.getEntry();
                        // handler bitmap
                        if (entry.result instanceof BitmapCache) {
                            BitmapCache bitmapCache = (BitmapCache) entry.result;
                            byte[] data = bitmapCache.getBitmapBytes();
                            Bitmap resultBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                            entry = new Entry<>(resultBitmap, entry.responseHeaders);
                            value.setEntry(entry);

                        }
                        return value;
                    } else {
                        return null;
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;

    }

    @Override
    public void delete(String key) {
        if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
            open();
        }
        try {
            key = hashKeyForDisk(key);
            mDiskLruCache.remove(key);
        } catch (IOException e) {
            e.printStackTrace();
            CLog.e("DiskCache delete failed:%s ",e.getMessage());
        }
    }

    @Override
    public void update(String key, CacheData value) {
        delete(key);
        put(key,value);
    }

    @Override
    public void clear() {
        if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
            open();
        }
        if (mDiskLruCache != null) {
            try {
                mDiskLruCache.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public long getCurrentSize(){
        if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
            open();
        }
        if (mDiskLruCache != null) {
            return mDiskLruCache.size();
        }
        return 0;
    }

    public void setMaxSize(long maxSize){
        if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
            open();
        }
        if (mDiskLruCache != null) {
            mDiskLruCache.setMaxSize(maxSize);
            CacheConfig.DISK_CACHE_MAX_SIZE = maxSize;
        }
    }

    public long getMaxSize(){
        if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
            open();
        }
        if (mDiskLruCache != null) {
            return mDiskLruCache.getMaxSize();
        }
        return 0;
    }

    public File getDirectory(){
        if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
            open();
        }
        if (mDiskLruCache != null) {
            return mDiskLruCache.getDirectory();
        }
        return null;
    }
}
