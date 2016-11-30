package net.robinx.lib.http.config;

/**
 * cache used configuration
 * 
 * @author Robin
 * @since 2015-05-07 13:06:54
 */
public class RequestCacheOptions {
	
	/** Default expiration time */
	public static final long DEFAULT_EXPIRATION_TIME=30*1000;
	
	/** Default timeout */
	public static final long DEFAULT_TIMEOUT=20*1000;

	/**
	 * whether allow cache data
	 */
	private boolean shouldCache;

	/**
	 * use cache data first ,no matter the cache have expired ,then update cache
	 * when request finish
	 */
	private boolean useCacheDataAnyway;

	/**
	 * use cache data if request failed
	 */
	private boolean useCacheDataWhenRequestFailed;

	/**
	 * use cache data if the cache data is not expired
	 */
	private boolean useCacheDataWhenUnexpired;

	/**
	 * use cache data if timeout
	 */
	private boolean useCacheDataWhenTimeout;

	/**
	 * Retry if request failed
	 */
	private boolean retryWhenRequestFailed;
	
	/**
	 * Set cache never expired.
	 */
	private boolean isNeverExpired;

	/**
	 * control expirationtime and timeout
	 */
	private TimeController timeController;

	private RequestCacheOptions(Builder builder){
		this.shouldCache = builder.shouldCache;
		this.useCacheDataAnyway = builder.useCacheDataAnyway;
		this.useCacheDataWhenRequestFailed = builder.useCacheDataWhenRequestFailed;
		this.useCacheDataWhenUnexpired = builder.useCacheDataWhenUnexpired;
		this.useCacheDataWhenTimeout = builder.useCacheDataWhenTimeout;
		this.retryWhenRequestFailed = builder.retryWhenRequestFailed;
		this.isNeverExpired = builder.isNeverExpired;
		this.timeController = builder.timeController;
	}

	public RequestCacheOptions() {
		super();
	}

	public boolean isShouldCache() {
		return shouldCache;
	}

	public RequestCacheOptions setShouldCache(boolean shouldCache) {
		this.shouldCache = shouldCache;
		return this;
	}

	public boolean isUseCacheDataAnyway() {
		return useCacheDataAnyway;
	}

	public RequestCacheOptions setUseCacheDataAnyway(boolean useCacheDataAnyway) {
		this.useCacheDataAnyway = useCacheDataAnyway;
		return this;
	}

	public boolean isUseCacheDataWhenRequestFailed() {
		return useCacheDataWhenRequestFailed;
	}

	public RequestCacheOptions setUseCacheDataWhenRequestFailed(boolean useCacheDataWhenRequestFailed) {
		this.useCacheDataWhenRequestFailed = useCacheDataWhenRequestFailed;
		return this;
	}

	public boolean isUseCacheDataWhenUnexpired() {
		return useCacheDataWhenUnexpired;
	}

	public RequestCacheOptions setUseCacheDataWhenUnexpired(boolean useCacheDataWhenUnexpired) {
		this.useCacheDataWhenUnexpired = useCacheDataWhenUnexpired;
		return this;
	}

	public boolean isUseCacheDataWhenTimeout() {
		return useCacheDataWhenTimeout;
	}

	public RequestCacheOptions setUseCacheDataWhenTimeout(boolean useCacheDataWhenTimeout) {
		this.useCacheDataWhenTimeout = useCacheDataWhenTimeout;
		return this;
	}

	public boolean isRetryWhenRequestFailed() {
		return retryWhenRequestFailed;
	}

	public RequestCacheOptions setRetryWhenRequestFailed(boolean retryWhenRequestFailed) {
		this.retryWhenRequestFailed = retryWhenRequestFailed;
		return this;
	}
	
	public boolean isNeverExpired() {
		return isNeverExpired;
	}

	public RequestCacheOptions setNeverExpired(boolean isNeverExpired) {
		this.isNeverExpired = isNeverExpired;
		return this;
	}

	public TimeController getTimeController() {
		return timeController;
	}

	public RequestCacheOptions setTimeController(TimeController timeController) {
		this.timeController = timeController;
		return this;
	}
	
	@Override
	public String toString() {
		return "RequestCacheOptions [shouldCache=" + shouldCache + ", useCacheDataAnyway=" + useCacheDataAnyway
				+ ", useCacheDataWhenRequestFailed=" + useCacheDataWhenRequestFailed + ", useCacheDataWhenUnexpired="
				+ useCacheDataWhenUnexpired + ", useCacheDataWhenTimeout=" + useCacheDataWhenTimeout
				+ ", retryWhenRequestFailed=" + retryWhenRequestFailed + ", isNeverExpired=" + isNeverExpired
				+ ", timeController=" + timeController + "]";
	}

	public static class Builder implements net.robinx.lib.http.base.Builder<RequestCacheOptions>{
		private boolean shouldCache;
		private boolean useCacheDataAnyway;
		private boolean useCacheDataWhenRequestFailed;
		private boolean useCacheDataWhenUnexpired;
		private boolean useCacheDataWhenTimeout;
		private boolean retryWhenRequestFailed;
		private boolean isNeverExpired;
		private TimeController timeController;

		public Builder shouldCache(boolean shouldCache) {
			this.shouldCache = shouldCache;
			return this;
		}

		public Builder useCacheDataAnyway(boolean useCacheDataAnyway) {
			this.useCacheDataAnyway = useCacheDataAnyway;
			return this;
		}

		public Builder useCacheDataWhenRequestFailed(boolean useCacheDataWhenRequestFailed) {
			this.useCacheDataWhenRequestFailed = useCacheDataWhenRequestFailed;
			return this;
		}

		public Builder useCacheDataWhenUnexpired(boolean useCacheDataWhenUnexpired) {
			this.useCacheDataWhenUnexpired = useCacheDataWhenUnexpired;
			return this;
		}

		public Builder useCacheDataWhenTimeout(boolean useCacheDataWhenTimeout) {
			this.useCacheDataWhenTimeout = useCacheDataWhenTimeout;
			return this;
		}

		public Builder retryWhenRequestFailed(boolean retryWhenRequestFailed) {
			this.retryWhenRequestFailed = retryWhenRequestFailed;
			return this;
		}

		public Builder neverExpired(boolean neverExpired) {
			isNeverExpired = neverExpired;
			return this;
		}

		public Builder timeController(TimeController timeController) {
			this.timeController = timeController;
			return this;
		}

		@Override
		public RequestCacheOptions build() {
			return new RequestCacheOptions(this);
		}
	}

	/**
	 * create a default cache configuration when cacheConfig is null
	 * @return
	 */
	public static RequestCacheOptions buildDefaultCacheOptions() {
		RequestCacheOptions options=new RequestCacheOptions.Builder()
                .shouldCache(true)
                .useCacheDataAnyway(false)
                .useCacheDataWhenRequestFailed(true)
                .useCacheDataWhenTimeout(false)
                .useCacheDataWhenUnexpired(true)
                .retryWhenRequestFailed(true)
                .neverExpired(false)
                .timeController(new TimeController().setExpirationTime(DEFAULT_EXPIRATION_TIME).setTimeout(DEFAULT_TIMEOUT))
                .build();

		return options;
	}
	
	public static RequestCacheOptions buildAllCloseOptions() {
        RequestCacheOptions options=new RequestCacheOptions.Builder()
                .shouldCache(false)
                .useCacheDataAnyway(false)
                .useCacheDataWhenRequestFailed(false)
                .useCacheDataWhenTimeout(false)
                .useCacheDataWhenUnexpired(false)
                .retryWhenRequestFailed(false)
                .neverExpired(false)
                .timeController(new TimeController().setExpirationTime(0).setTimeout(0))
                .build();
		
		return options;
	}
	
	public static RequestCacheOptions buildImageCacheOptions() {
        RequestCacheOptions options=new RequestCacheOptions.Builder()
                .shouldCache(true)
                .useCacheDataAnyway(false)
                .useCacheDataWhenRequestFailed(false)
                .useCacheDataWhenTimeout(false)
                .useCacheDataWhenUnexpired(true)
                .retryWhenRequestFailed(true)
                .neverExpired(true)
                .timeController(new TimeController().setTimeout(DEFAULT_TIMEOUT))
                .build();
		
		return options;
	}
}
