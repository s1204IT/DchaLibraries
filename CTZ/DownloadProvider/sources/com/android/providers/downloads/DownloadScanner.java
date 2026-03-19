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
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DownloadScanner implements MediaScannerConnection.MediaScannerConnectionClient {
    private final MediaScannerConnection mConnection;
    private final Context mContext;
    private CountDownLatch mLatch;

    @GuardedBy("mConnection")
    private HashMap<String, ScanRequest> mPending = Maps.newHashMap();

    private static class ScanRequest {
        public final long id;
        public final String mimeType;
        public final String path;
        public final long requestRealtime = SystemClock.elapsedRealtime();

        public ScanRequest(long j, String str, String str2) {
            this.id = j;
            this.path = str;
            this.mimeType = str2;
        }

        public void exec(MediaScannerConnection mediaScannerConnection) {
            mediaScannerConnection.scanFile(this.path, this.mimeType);
        }
    }

    public DownloadScanner(Context context) {
        this.mContext = context;
        this.mConnection = new MediaScannerConnection(context, this);
    }

    public static void requestScanBlocking(Context context, long j, String str, String str2) {
        DownloadScanner downloadScanner = new DownloadScanner(context);
        downloadScanner.mLatch = new CountDownLatch(1);
        downloadScanner.requestScan(new ScanRequest(j, str, str2));
        try {
            try {
                downloadScanner.mLatch.await(60000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } finally {
            downloadScanner.shutdown();
        }
    }

    public void requestScan(ScanRequest scanRequest) {
        if (Constants.LOGV) {
            Log.v("DownloadManager", "requestScan() for " + scanRequest.path);
        }
        synchronized (this.mConnection) {
            this.mPending.put(scanRequest.path, scanRequest);
            if (this.mConnection.isConnected()) {
                scanRequest.exec(this.mConnection);
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
            Iterator<ScanRequest> it = this.mPending.values().iterator();
            while (it.hasNext()) {
                it.next().exec(this.mConnection);
            }
        }
    }

    @Override
    public void onScanCompleted(String str, Uri uri) {
        ScanRequest scanRequestRemove;
        synchronized (this.mConnection) {
            scanRequestRemove = this.mPending.remove(str);
        }
        if (scanRequestRemove == null) {
            Log.w("DownloadManager", "Missing request for path " + str);
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("scanned", (Integer) 1);
        if (uri != null) {
            contentValues.put("mediaprovider_uri", uri.toString());
        }
        ContentResolver contentResolver = this.mContext.getContentResolver();
        if (contentResolver.update(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, scanRequestRemove.id), contentValues, null, null) == 0) {
            contentResolver.delete(uri, null, null);
        }
        if (this.mLatch != null) {
            this.mLatch.countDown();
        }
    }
}
