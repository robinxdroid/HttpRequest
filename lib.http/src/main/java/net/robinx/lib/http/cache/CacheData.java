package net.robinx.lib.http.cache;

import java.io.Serializable;
import java.text.SimpleDateFormat;

import net.robinx.lib.http.utils.CLog;

import android.annotation.SuppressLint;

/**
 * define the cache data entity
 * 
 * @author Robin
 * @since 2015-05-08 10:06:46
 */
@SuppressWarnings("serial")
public class CacheData<E> implements Serializable {

	/**
	 * will storage's data
	 */
	private E entry;

	/**
	 * the data expired time
	 */
	private long expirationTime;

	/**
	 * the data write to cache time
	 */
	private long writeTime;

	/**
	 * Whether never expired
	 */
	private boolean isNeverExpiry;

	public CacheData(E entry, long expirationTime, long writeTime) {
		super();
		this.entry = entry;
		this.expirationTime = expirationTime;
		this.writeTime = writeTime;
	}

	public CacheData(E data, long expirationTime, long writeTime, boolean isNeverExpiry) {
		super();
		this.entry = data;
		this.expirationTime = expirationTime;
		this.writeTime = writeTime;
		this.isNeverExpiry = isNeverExpiry;
	}

	public E getEntry() {
		return entry;
	}

	public void setEntry(E data) {
		this.entry = data;
	}

	public long getExpirationTime() {
		return expirationTime;
	}

	public void setExpirationTime(long expirationTime) {
		this.expirationTime = expirationTime;
	}

	public long getWriteTime() {
		return writeTime;
	}

	public void setWriteTime(long writeTime) {
		this.writeTime = writeTime;
	}

	public boolean isNeverExpiry() {
		return isNeverExpiry;
	}

	public void setNeverExpiry(boolean isNeverExpiry) {
		this.isNeverExpiry = isNeverExpiry;
	}

	/**
	 * check out the data is or not expired
	 * 
	 * @return
	 */
	public Boolean isExpired() {
		if (isNeverExpiry) {
			return false;
		}

		if (writeTime <= 0) {
			return true;
		}
		if (expirationTime <= 0) {
			return true;
		}
		long intervalTime = System.currentTimeMillis() - writeTime;
		String format = "yyyy-MM-dd HH:mm";
		String currentTimeStr = getStringByFormat(System.currentTimeMillis(), format);
		String writeTimeStr = getStringByFormat(writeTime, format);
		CLog.d("currentTime:%s，writeTime:%s，interval:%s s", (Object)currentTimeStr, writeTimeStr , intervalTime/1000);
		
		if (intervalTime < expirationTime) {
			return false;
		} else {
			// expired
			return true;
		}
	}
	
	@SuppressLint("SimpleDateFormat")
	private static String getStringByFormat(long milliseconds,String format) {
		String thisDateTime = null;
		try {
			SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat(format);
			thisDateTime = mSimpleDateFormat.format(milliseconds);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return thisDateTime;
	}

}
