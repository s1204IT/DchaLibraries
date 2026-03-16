package com.android.providers.downloads;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Downloads;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.google.common.collect.Maps;
import java.util.HashMap;

public class DownloadScanner implements MediaScannerConnection.MediaScannerConnectionClient {
    private final MediaScannerConnection mConnection;
    private final Context mContext;

    @GuardedBy("mConnection")
    private HashMap<String, ScanRequest> mPending = Maps.newHashMap();

    private static class ScanRequest {
        public final long id;
        public final String mimeType;
        public final String path;
        public final long requestRealtime = SystemClock.elapsedRealtime();

        public ScanRequest(long id, String path, String mimeType) {
            this.id = id;
            this.path = path;
            this.mimeType = mimeType;
        }

        public void exec(MediaScannerConnection conn) {
            conn.scanFile(this.path, this.mimeType);
        }
    }

    public DownloadScanner(Context context) {
        this.mContext = context;
        this.mConnection = new MediaScannerConnection(context, this);
    }

    public void requestScan(DownloadInfo info) {
        if (Constants.LOGV) {
            Log.v("DownloadManager", "requestScan() for " + info.mFileName);
        }
        synchronized (this.mConnection) {
            ScanRequest req = new ScanRequest(info.mId, info.mFileName, info.mMimeType);
            this.mPending.put(req.path, req);
            if (this.mConnection.isConnected()) {
                req.exec(this.mConnection);
            } else {
                this.mConnection.connect();
            }
        }
    }

    public void shutdown() {
        this.mConnection.disconnect();
    }

    @Override
    public void onMediaScannerConnected() {
        synchronized (this.mConnection) {
            for (ScanRequest req : this.mPending.values()) {
                req.exec(this.mConnection);
            }
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        ScanRequest req;
        synchronized (this.mConnection) {
            req = this.mPending.remove(path);
        }
        if (req == null) {
            Log.w("DownloadManager", "Missing request for path " + path);
            return;
        }
        ContentValues values = new ContentValues();
        values.put("scanned", (Integer) 1);
        if (uri != null) {
            values.put("mediaprovider_uri", uri.toString());
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        Uri downloadUri = ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, req.id);
        int rows = resolver.update(downloadUri, values, null, null);
        if (rows == 0) {
            resolver.delete(uri, null, null);
        }
    }
}
