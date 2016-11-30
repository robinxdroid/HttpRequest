package net.robinx.lib.http.okhttp;

import net.robinx.lib.http.callback.BytesWriteListener;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * Created by Robin on 2016/4/26.
 */
public class RequestBodyProxy extends RequestBody {

    private BytesWriteListener mBytesWriteListener;

    private RequestBody mRequestBody;

    private BufferedSink mBufferedSink;

    private int mCurrentFileIndex;

    private File mCurrentFile;

    public RequestBodyProxy(RequestBody requestBody, int currentFileIndex, File currentFile, BytesWriteListener listener) {
        super();
        this.mRequestBody = requestBody;
        this.mCurrentFileIndex = currentFileIndex;
        this.mCurrentFile = currentFile;
        this.mBytesWriteListener = listener;
    }

    @Override
    public long contentLength() throws IOException {
        return mRequestBody.contentLength();
    }

    @Override
    public MediaType contentType() {
        return mRequestBody.contentType();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (mBufferedSink == null) {
            mBufferedSink = Okio.buffer(sink(sink));
        }
        mRequestBody.writeTo(mBufferedSink);
        mBufferedSink.flush();
    }

    private Sink sink(Sink sink) {
        return new ForwardingSink(sink) {
            long bytesWritten = 0L;
            long contentLength = 0L;

            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                if (contentLength == 0) {
                    contentLength = contentLength();
                }
                bytesWritten += byteCount;
                mBytesWriteListener.onWrite(bytesWritten,contentLength,mCurrentFileIndex,mCurrentFile);
            }
        };
    }

}
