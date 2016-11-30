package net.robinx.lib.http.network.ex.hurl;

import net.robinx.lib.http.callback.BytesWriteListener;
import net.robinx.lib.http.network.ex.Body;
import net.robinx.lib.http.network.ex.RequestParams;
import net.robinx.lib.http.utils.CLog;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;

/**
 * Can submit key/value pair, files, key/value pair and files, JSON,
 * If the request parameter contains a JSON parameters, send JSON parameters,
 * this time even contain key/value pair or file parameter will not be sent
 *
 * @author Robin
 * @since 2016-01-07 18:53:19
 */
public class HurlRequestBody implements Body {

    private boolean isFixedStreamingMode;

    private BytesWriteListener mBytesWriteListener;

	/*======================================================
     *  Override Super
	 *======================================================
	 */

    public String buildBodyContentType(RequestParams params, int curTime) {
        if (params.hasJsonInParams()) {
            return String.format("application/json; charset=%s", "utf-8");
        }

        return String.format(RequestBodyConstants.CONTENT_TYPE_MULTIPART, "utf-8", curTime);
    }

    @Override
    public Object buildBody(RequestParams params, Object... args) {
        HttpURLConnection connection = (HttpURLConnection) args[0];
        connection.setDoOutput(true);
        final String charset = "utf-8";
        final int curTime = (int) (System.currentTimeMillis() / 1000);
        final String boundary = RequestBodyConstants.BOUNDARY_PREFIX + curTime;
        connection.setRequestProperty(RequestBodyConstants.HEADER_CONTENT_TYPE, buildBodyContentType(params, curTime));

        if (isFixedStreamingMode()) {
            int contentLength = RequestBodyConstants.getContentLength(boundary, params);
            connection.setFixedLengthStreamingMode(contentLength);
        } else {
            connection.setChunkedStreamingMode(0);
        }

        // Write parameters
        PrintWriter writer = null;
        try {
            OutputStream out = connection.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(out, charset), true);

            if (params.hasJsonInParams()) {
                // append json
                writer.append(params.buildJsonParams()).flush();

            } else {

                writeMultipartToOutputStream(params, boundary, writer, out);

                // End of multipart/form-data.
                writer.append(boundary + RequestBodyConstants.BOUNDARY_PREFIX).append(RequestBodyConstants.CRLF).flush();
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        return null;
    }

    private void writeMultipartToOutputStream(RequestParams params, String boundary, PrintWriter writer, OutputStream out)  throws IOException {
        int currentFileIndex = 1;
        for (String key : params.keySet()) {
            Object value = params.get(key);
            if (value == null) {
                continue;
            }

            if (value instanceof File) {    // add file
                currentFileIndex = writeFileToOutputStream(boundary, writer, out, currentFileIndex, key, (File) value);
            } else {       // add field
                writeFieldToOutputStream(boundary, writer, key, value);
            }
        }
    }

    private void writeFieldToOutputStream(String boundary, PrintWriter writer, String key, Object value) {
        String param = String.valueOf(value);

        writer.append(boundary).append(RequestBodyConstants.CRLF)
                .append(String.format(RequestBodyConstants.HEADER_CONTENT_DISPOSITION + RequestBodyConstants.COLON_SPACE + RequestBodyConstants.FORM_DATA, key)).append(RequestBodyConstants.CRLF)
                .append(RequestBodyConstants.HEADER_CONTENT_TYPE + RequestBodyConstants.COLON_SPACE + RequestBodyConstants.CONTENT_TYPE_TEXT)
                .append(RequestBodyConstants.CRLF).append(RequestBodyConstants.CRLF)
                .append(param).append(RequestBodyConstants.CRLF).flush();
    }

    private int writeFileToOutputStream(String boundary, PrintWriter writer, OutputStream out, int currentFileIndex, String key, File value) throws IOException {
        File file = value;

        if (!file.exists()) {
            CLog.e("File not found: %s", file.getAbsolutePath());
            throw new IOException(String.format("File not found: %s", file.getAbsolutePath()));
        }

        if (file.isDirectory()) {
            CLog.e("File is a directory: %s", file.getAbsolutePath());
            throw new IOException(String.format("File is a directory: %s", file.getAbsolutePath()));
        }

        writer.append(boundary).append(RequestBodyConstants.CRLF)
                .append(String.format(
                        RequestBodyConstants.HEADER_CONTENT_DISPOSITION + RequestBodyConstants.COLON_SPACE + RequestBodyConstants.FORM_DATA + RequestBodyConstants.SEMICOLON_SPACE + RequestBodyConstants.FILENAME, key,
                        file.getName()))
                .append(RequestBodyConstants.CRLF).append(RequestBodyConstants.HEADER_CONTENT_TYPE + RequestBodyConstants.COLON_SPACE + RequestBodyConstants.CONTENT_TYPE_OCTET_STREAM).append(RequestBodyConstants.CRLF)
                .append(RequestBodyConstants.HEADER_CONTENT_TRANSFER_ENCODING + RequestBodyConstants.COLON_SPACE + RequestBodyConstants.BINARY).append(RequestBodyConstants.CRLF).append(RequestBodyConstants.CRLF)
                .flush();

        BufferedInputStream input = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            int transferredBytesSize = 0;
            int totalSize = (int) file.length();
            input = new BufferedInputStream(fis);
            int bufferLength = 0;

            byte[] buffer = new byte[1024];
            while ((bufferLength = input.read(buffer)) > 0) {
                CLog.w("<getBody> thread name : %s", Thread.currentThread().getName());
                out.write(buffer, 0, bufferLength);
                transferredBytesSize += bufferLength;
                if (mBytesWriteListener != null) {
                    mBytesWriteListener.onWrite(transferredBytesSize, totalSize, currentFileIndex, file);
                }

            }
            // Important! Output cannot be closed. Close of writer will
            // close output as well.
            out.flush();
        } finally {
            if (input != null)
                try {
                    input.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
        }
        // CRLF is important! It indicates end of binary boundary.
        writer.append(RequestBodyConstants.CRLF).flush();

        currentFileIndex++;
        return currentFileIndex;
    }

    public boolean isFixedStreamingMode() {
        return isFixedStreamingMode;
    }

    public void setFixedStreamingMode(boolean isFixedStreamingMode) {
        this.isFixedStreamingMode = isFixedStreamingMode;
    }

    @Override
    public void setBytesWriteListener(BytesWriteListener listener) {
        this.mBytesWriteListener = listener;
    }
}
