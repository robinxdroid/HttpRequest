package com.app.core.example;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Application;

import net.robinx.lib.http.XRequest;
import net.robinx.lib.http.cache.CacheConfig;

public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		
		configXRequestCache();
		
	}

	@SuppressLint("SdCardPath")
	private void configXRequestCache() {
		//磁盘缓存路径
		File DISK_CACHE_DIR_PATH = new File("/sdcard/xrequest/diskcache");
		//磁盘缓存最大值
		int DISK_CACHE_MAX_SIZE = 30*1024*1024;
		
		XRequest.initXRequest(getApplicationContext());
		//XRequest.INSTANCE.setStack(new OkHttpStack());
		CacheConfig.DISK_CACHE_MAX_SIZE = DISK_CACHE_MAX_SIZE;
		CacheConfig.DISK_CACHE_DIRECTORY = DISK_CACHE_DIR_PATH;
	}
}
