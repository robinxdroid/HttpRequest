package net.robinx.lib.http.callback;

import java.io.File;

/**
 * Created by Robin on 2016/4/30.
 */
public interface BytesWriteListener{
    public void onWrite(long transferredBytesSize, long totalSize, int currentFileIndex, File currentFile);
}