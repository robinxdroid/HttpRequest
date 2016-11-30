package net.robinx.lib.http.okhttp;

import net.robinx.lib.http.callback.BytesWriteListener;
import net.robinx.lib.http.network.ex.Body;
import net.robinx.lib.http.network.ex.RequestParams;

import java.io.File;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Can submit key/value pair, files, key/value pair and files, JSON, 
 * If the request parameter contains a JSON parameters, send JSON parameters, 
 * this time even contain key/value pair or file parameter will not be sent
 * @author Robin
 * @since 2016-01-07 18:53:19
 *
 */
public class OkRequestBody implements Body{

    private BytesWriteListener mBytesWriteListener;

	/*======================================================
	 *  Override Super
	 *====================================================== 
	 */
	

	@Override
	public RequestBody buildBody(RequestParams params,Object...args) {
		if (params == null) {
			return RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"), "".getBytes());
		}
		if (params.hasJsonInParams()) {
            // has json
			return  RequestBody.create(MediaType.parse("application/json; charset=utf-8"), params.buildJsonParams());

        } else if (params.isEmpty()) {
            return RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"), "".getBytes());
        } else if (params.hasFileInParams()) {
            // has file
            return createMultipartBody(params);
        } else {
            // key/value
            return createFormBody(params);

        }
    }

    private RequestBody createFormBody(RequestParams params) {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }

            formBodyBuilder.add(entry.getKey(),String.valueOf(value));

        }

        RequestBody formBody = formBodyBuilder.build();
        return formBody;
    }

    private RequestBody createMultipartBody(RequestParams params) {
		MultipartBody.Builder builder = new MultipartBody.Builder();
		builder.setType(MultipartBody.FORM);

		int fileIndex = 1;
		if(params != null && !params.isEmpty()) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				Object value = entry.getValue();
				if (value == null) {
					continue;
				}

				if (value instanceof File) {       //add file
					final File file = (File) value;
					String fileName = file.getName();


					RequestBody fileBody = RequestBody.create(getMediaType(fileName),file);
					builder.addFormDataPart(entry.getKey(), fileName, new RequestBodyProxy(fileBody,fileIndex,file, new BytesWriteListener() {
						@Override
						public void onWrite(long transferredBytesSize, long totalSize, int currentFileIndex, File currentFile) {
							if (mBytesWriteListener != null) {
								mBytesWriteListener.onWrite(transferredBytesSize,totalSize,currentFileIndex,currentFile);
							}

                        }

                    }));
                    //builder.addFormDataPart(entry.getKey(),fileName,fileBody);
					fileIndex++;

				} else {    //add field
					builder.addFormDataPart(entry.getKey(),String.valueOf(value));
				}

			}
		}

		 return builder.build();

	}


	/**
	 * get ContentType by file name
	 * @param fileName
	 * @return
	 */
	private MediaType getMediaType(String fileName) {
		boolean isPng = fileName.lastIndexOf("png") > 0 || fileName.lastIndexOf("PNG") > 0;
		if (isPng) {
			return MediaType.parse("image/png; charset=UTF-8");
		}

		boolean isJpg = fileName.lastIndexOf("jpg") > 0 || fileName.lastIndexOf("JPG") > 0
				||fileName.lastIndexOf("jpeg") > 0 || fileName.lastIndexOf("JPEG") > 0;
		if (isJpg) {
			return  MediaType.parse("image/jpeg; charset=UTF-8");
		}
		return null;
	}

	@Override
    public void setBytesWriteListener(BytesWriteListener listener) {
        this.mBytesWriteListener = listener;
    }
}
