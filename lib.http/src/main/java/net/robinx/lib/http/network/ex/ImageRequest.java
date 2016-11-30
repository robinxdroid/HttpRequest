package net.robinx.lib.http.network.ex;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.widget.ImageView.ScaleType;

import net.robinx.lib.http.config.HttpMethod;
import net.robinx.lib.http.config.Priority;
import net.robinx.lib.http.config.RequestCacheOptions;
import net.robinx.lib.http.callback.OnRequestListener;
import net.robinx.lib.http.network.HttpError;
import net.robinx.lib.http.network.HttpException;
import net.robinx.lib.http.response.NetworkResponse;
import net.robinx.lib.http.response.Response;
import net.robinx.lib.http.retry.DefaultRetryPolicyImpl;
import net.robinx.lib.http.utils.CLog;

/**
 * Get a bitmap from network
 * @author Robin
 * @since 2016-01-05 19:18:45
 */
public class ImageRequest extends HttpRequest<Bitmap> {
	/** Socket timeout in milliseconds for image requests */
	private static final int IMAGE_TIMEOUT_MS = 1000;

	/** Default number of retries for image requests */
	private static final int IMAGE_MAX_RETRIES = 2;

	/** Default backoff multiplier for image requests */
	private static final float IMAGE_BACKOFF_MULT = 2f;

	private final Config mDecodeConfig;
	private final int mMaxWidth;
	private final int mMaxHeight;
	private ScaleType mScaleType;

	/**
	 * Decoding lock so that we don't decode more than one image at a time (to
	 * avoid OOM's)
	 */
	private static final Object sDecodeLock = new Object();
	
	public ImageRequest(String url, OnRequestListener<Bitmap> onRequestListener) {
		this(RequestCacheOptions.buildImageCacheOptions(), url, url, onRequestListener, 0,0, ScaleType.CENTER_INSIDE, Config.ARGB_8888);
	}
	
	public ImageRequest(String url, String cacheKey, OnRequestListener<Bitmap> onRequestListener) {
		this(RequestCacheOptions.buildImageCacheOptions(), url, cacheKey, onRequestListener, 0,0, ScaleType.CENTER_INSIDE, Config.ARGB_8888);
	}
	
	public ImageRequest(RequestCacheOptions cacheConfig, String url, String cacheKey, OnRequestListener<Bitmap> onRequestListener) {
		this(cacheConfig, url, cacheKey, onRequestListener, 0,0, ScaleType.CENTER_INSIDE, Config.ARGB_8888);
	}
	
	public ImageRequest(RequestCacheOptions cacheConfig, String url, String cacheKey,
						OnRequestListener<Bitmap> onRequestListener, int maxWidth, int maxHeight, ScaleType scaleType, Config decodeConfig) {
		super();

		setRequestCacheOptions(cacheConfig);
		setUrl(url);
		setCacheKey(cacheKey);
        setOnRequestListener(onRequestListener);

		setRetryPolicy(new DefaultRetryPolicyImpl(IMAGE_TIMEOUT_MS, IMAGE_MAX_RETRIES, IMAGE_BACKOFF_MULT));
		setPriority(Priority.LOW);
		setHttpMethod(HttpMethod.GET);
		mDecodeConfig = decodeConfig;
		mMaxWidth = maxWidth;
		mMaxHeight = maxHeight;
		mScaleType = scaleType;
	}


	@Override
	public Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
		// Serialize all decode on a global lock to reduce concurrent heap
		// usage.
		synchronized (sDecodeLock) {
			try {
				return doParse(response);
			} catch (OutOfMemoryError e) {
				CLog.e("Caught OOM for %d byte image, url=%s", response.data.length, getUrl());
				return Response.error(new HttpException(e.getMessage(), HttpError.ERROR_PARSE));
			}
		}
	}

	/**
	 * The real guts of parseNetworkResponse. Broken out for readability.
	 */
	private Response<Bitmap> doParse(NetworkResponse response) {
		byte[] data = response.data;
		BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
		Bitmap bitmap = null;
		if (mMaxWidth == 0 && mMaxHeight == 0) {
			decodeOptions.inPreferredConfig = mDecodeConfig;
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
		} else {
			// If we have to resize this image, first get the natural bounds.
			decodeOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
			int actualWidth = decodeOptions.outWidth;
			int actualHeight = decodeOptions.outHeight;

			// Then compute the dimensions we would ideally like to decode to.
			int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight, actualWidth, actualHeight, mScaleType);
			int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth, actualHeight, actualWidth, mScaleType);

			// Decode to the nearest power of two scaling factor.
			decodeOptions.inJustDecodeBounds = false;
			// TODO(ficus): Do we need this or is it okay since API 8 doesn't
			// support it?
			// decodeOptions.inPreferQualityOverSpeed =
			// PREFER_QUALITY_OVER_SPEED;
			decodeOptions.inSampleSize = findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
			Bitmap tempBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);

			// If necessary, scale down to the maximal acceptable size.
			if (tempBitmap != null
					&& (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight)) {
				bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
				tempBitmap.recycle();
			} else {
				bitmap = tempBitmap;
			}
		}

		if (bitmap == null) {
			return Response.error(new HttpException("bitmap == null", HttpError.ERROR_PARSE));
		} else {
			super.onParseNetworkResponse(response, bitmap);
			return Response.success(bitmap, response.headers);
		}
	}
	
	/**
	 * Scales one side of a rectangle to fit aspect ratio.
	 *
	 * @param maxPrimary
	 *            Maximum size of the primary dimension (i.e. width for max
	 *            width), or zero to maintain aspect ratio with secondary
	 *            dimension
	 * @param maxSecondary
	 *            Maximum size of the secondary dimension, or zero to maintain
	 *            aspect ratio with primary dimension
	 * @param actualPrimary
	 *            Actual size of the primary dimension
	 * @param actualSecondary
	 *            Actual size of the secondary dimension
	 * @param scaleType
	 *            The ScaleType used to calculate the needed image size.
	 */
	private static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary, int actualSecondary,
			ScaleType scaleType) {

		// If no dominant value at all, just return the actual.
		if ((maxPrimary == 0) && (maxSecondary == 0)) {
			return actualPrimary;
		}

		// If ScaleType.FIT_XY fill the whole rectangle, ignore ratio.
		if (scaleType == ScaleType.FIT_XY) {
			if (maxPrimary == 0) {
				return actualPrimary;
			}
			return maxPrimary;
		}

		// If primary is unspecified, scale primary to match secondary's scaling
		// ratio.
		if (maxPrimary == 0) {
			double ratio = (double) maxSecondary / (double) actualSecondary;
			return (int) (actualPrimary * ratio);
		}

		if (maxSecondary == 0) {
			return maxPrimary;
		}

		double ratio = (double) actualSecondary / (double) actualPrimary;
		int resized = maxPrimary;

		// If ScaleType.CENTER_CROP fill the whole rectangle, preserve aspect
		// ratio.
		if (scaleType == ScaleType.CENTER_CROP) {
			if ((resized * ratio) < maxSecondary) {
				resized = (int) (maxSecondary / ratio);
			}
			return resized;
		}

		if ((resized * ratio) > maxSecondary) {
			resized = (int) (maxSecondary / ratio);
		}
		return resized;
	}

	/**
	 * Returns the largest power-of-two divisor for use in downscaling a bitmap
	 * that will not result in the scaling past the desired dimensions.
	 *
	 * @param actualWidth
	 *            Actual width of the bitmap
	 * @param actualHeight
	 *            Actual height of the bitmap
	 * @param desiredWidth
	 *            Desired width of the bitmap
	 * @param desiredHeight
	 *            Desired height of the bitmap
	 */
	// Visible for testing.
	static int findBestSampleSize(int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
		double wr = (double) actualWidth / desiredWidth;
		double hr = (double) actualHeight / desiredHeight;
		double ratio = Math.min(wr, hr);
		float n = 1.0f;
		while ((n * 2) <= ratio) {
			n *= 2;
		}

		return (int) n;
	}
}
