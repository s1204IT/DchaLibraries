package com.android.managedprovisioning.task;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Base64;
import com.android.managedprovisioning.ProvisionLogger;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class DownloadPackageTask {
    private final Callback mCallback;
    private final Context mContext;
    private boolean mDoneDownloading = false;
    private long mDownloadId;
    private final String mDownloadLocationFrom;
    private String mDownloadLocationTo;
    private final byte[] mHash;
    private final String mHttpCookieHeader;
    private BroadcastReceiver mReceiver;

    public static abstract class Callback {
        public abstract void onError(int i);

        public abstract void onSuccess();
    }

    public DownloadPackageTask(Context context, String downloadLocation, byte[] hash, String httpCookieHeader, Callback callback) {
        this.mCallback = callback;
        this.mContext = context;
        this.mDownloadLocationFrom = downloadLocation;
        this.mHash = hash;
        this.mHttpCookieHeader = httpCookieHeader;
    }

    public void run() {
        this.mReceiver = createDownloadReceiver();
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.DOWNLOAD_COMPLETE"));
        ProvisionLogger.logd("Starting download from " + this.mDownloadLocationFrom);
        DownloadManager dm = (DownloadManager) this.mContext.getSystemService("download");
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(this.mDownloadLocationFrom));
        if (this.mHttpCookieHeader != null) {
            request.addRequestHeader("Cookie", this.mHttpCookieHeader);
            ProvisionLogger.logd("Downloading with http cookie header: " + this.mHttpCookieHeader);
        }
        this.mDownloadId = dm.enqueue(request);
    }

    private BroadcastReceiver createDownloadReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) throws Throwable {
                if ("android.intent.action.DOWNLOAD_COMPLETE".equals(intent.getAction())) {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(DownloadPackageTask.this.mDownloadId);
                    DownloadManager dm = (DownloadManager) DownloadPackageTask.this.mContext.getSystemService("download");
                    Cursor c = dm.query(q);
                    if (c.moveToFirst()) {
                        int columnIndex = c.getColumnIndex("status");
                        if (8 == c.getInt(columnIndex)) {
                            String location = c.getString(c.getColumnIndex("local_filename"));
                            c.close();
                            DownloadPackageTask.this.onDownloadSuccess(location);
                        } else if (16 == c.getInt(columnIndex)) {
                            int reason = c.getColumnIndex("reason");
                            c.close();
                            DownloadPackageTask.this.onDownloadFail(reason);
                        }
                    }
                }
            }
        };
    }

    private void onDownloadSuccess(String location) throws Throwable {
        if (!this.mDoneDownloading) {
            this.mDoneDownloading = true;
            ProvisionLogger.logd("Downloaded succesfully to: " + location);
            byte[] hash = computeHash(location);
            if (hash != null) {
                if (Arrays.equals(this.mHash, hash)) {
                    ProvisionLogger.logd("SHA-1-hashes matched, both are " + byteArrayToString(hash));
                    this.mDownloadLocationTo = location;
                    this.mCallback.onSuccess();
                } else {
                    ProvisionLogger.loge("SHA-1-hash of downloaded file does not match given hash.");
                    ProvisionLogger.loge("SHA-1-hash of downloaded file: " + byteArrayToString(hash));
                    ProvisionLogger.loge("SHA-1-hash provided by programmer: " + byteArrayToString(this.mHash));
                    this.mCallback.onError(0);
                }
            }
        }
    }

    private void onDownloadFail(int errorCode) {
        ProvisionLogger.loge("Downloading package failed.");
        ProvisionLogger.loge("COLUMN_REASON in DownloadManager response has value: " + errorCode);
        this.mCallback.onError(1);
    }

    private byte[] computeHash(String fileLocation) throws Throwable {
        InputStream fis;
        InputStream fis2 = null;
        byte[] hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            try {
                try {
                    fis = new FileInputStream(fileLocation);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (IOException e) {
                e = e;
            }
            try {
                byte[] buffer = new byte[256];
                int n = 0;
                while (n != -1) {
                    n = fis.read(buffer);
                    if (n > 0) {
                        md.update(buffer, 0, n);
                    }
                }
                hash = md.digest();
                if (fis != null) {
                    try {
                        fis.close();
                        fis2 = fis;
                    } catch (IOException e2) {
                        fis2 = fis;
                    }
                } else {
                    fis2 = fis;
                }
            } catch (IOException e3) {
                e = e3;
                fis2 = fis;
                ProvisionLogger.loge("IO error.", e);
                this.mCallback.onError(2);
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e4) {
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                fis2 = fis;
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e5) {
                    }
                }
                throw th;
            }
            return hash;
        } catch (NoSuchAlgorithmException e6) {
            ProvisionLogger.loge("Hashing algorithm SHA-1 not supported.", e6);
            this.mCallback.onError(2);
            return null;
        }
    }

    public String getDownloadedPackageLocation() {
        return this.mDownloadLocationTo;
    }

    public void cleanUp() {
        if (this.mReceiver != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
        DownloadManager dm = (DownloadManager) this.mContext.getSystemService("download");
        boolean removeSuccess = dm.remove(this.mDownloadId) == 1;
        if (removeSuccess) {
            ProvisionLogger.logd("Successfully removed the device owner installer file.");
        } else {
            ProvisionLogger.loge("Could not remove the device owner installer file.");
        }
    }

    String byteArrayToString(byte[] ba) {
        return Base64.encodeToString(ba, 11);
    }
}
