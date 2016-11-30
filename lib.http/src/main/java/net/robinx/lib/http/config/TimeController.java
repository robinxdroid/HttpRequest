package net.robinx.lib.http.config;

/**
 * control the expiration time and timeout
 * 
 * @author Robin
 * @since 2015-05-07 20:17:47
 */
public class TimeController {

	private long expirationTime;

	private long timeout;

	public long getExpirationTime() {
		return expirationTime;
	}

	public TimeController setExpirationTime(long expirationTime) {
		this.expirationTime = expirationTime;
		return this;
	}

	public long getTimeout() {
		return timeout;
	}

	public TimeController setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	@Override
	public String toString() {
		return "TimeController [expirationTime=" + expirationTime + ", timeout=" + timeout + "]";
	}
	
}
