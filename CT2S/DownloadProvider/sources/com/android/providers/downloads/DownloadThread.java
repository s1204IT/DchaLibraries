package com.android.providers.downloads;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.drm.DrmManagerClient;
import android.drm.DrmOutputStream;
import android.net.INetworkPolicyListener;
import android.net.NetworkInfo;
import android.net.NetworkPolicyManager;
import android.net.TrafficStats;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.os.WorkSource;
import android.provider.Downloads;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.Pair;
import com.android.providers.downloads.DownloadInfo;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import libcore.io.IoUtils;

public class DownloadThread implements Runnable {
    private final Context mContext;
    private final long mId;
    private final DownloadInfo mInfo;
    private final DownloadInfoDelta mInfoDelta;
    private final DownloadNotifier mNotifier;
    private volatile boolean mPolicyDirty;
    private long mSpeed;
    private long mSpeedSampleBytes;
    private long mSpeedSampleStart;
    private final SystemFacade mSystemFacade;
    private boolean mMadeProgress = false;
    private long mLastUpdateBytes = 0;
    private long mLastUpdateTime = 0;
    private int mNetworkType = -1;
    private INetworkPolicyListener mPolicyListener = new INetworkPolicyListener.Stub() {
        public void onUidRulesChanged(int uid, int uidRules) {
            if (uid == DownloadThread.this.mInfo.mUid) {
                DownloadThread.this.mPolicyDirty = true;
            }
        }

        public void onMeteredIfacesChanged(String[] meteredIfaces) {
            DownloadThread.this.mPolicyDirty = true;
        }

        public void onRestrictBackgroundChanged(boolean restrictBackground) {
            DownloadThread.this.mPolicyDirty = true;
        }
    };

    private class DownloadInfoDelta {
        public long mCurrentBytes;
        public String mETag;
        public String mErrorMsg;
        public String mFileName;
        public String mMimeType;
        public int mNumFailed;
        public int mRetryAfter;
        public int mStatus;
        public long mTotalBytes;
        public String mUri;

        public DownloadInfoDelta(DownloadInfo info) {
            this.mUri = info.mUri;
            this.mFileName = info.mFileName;
            this.mMimeType = info.mMimeType;
            this.mStatus = info.mStatus;
            this.mNumFailed = info.mNumFailed;
            this.mRetryAfter = info.mRetryAfter;
            this.mTotalBytes = info.mTotalBytes;
            this.mCurrentBytes = info.mCurrentBytes;
            this.mETag = info.mETag;
        }

        private ContentValues buildContentValues() {
            ContentValues values = new ContentValues();
            values.put("uri", this.mUri);
            values.put("_data", this.mFileName);
            values.put("mimetype", this.mMimeType);
            values.put("status", Integer.valueOf(this.mStatus));
            values.put("numfailed", Integer.valueOf(this.mNumFailed));
            values.put("method", Integer.valueOf(this.mRetryAfter));
            values.put("total_bytes", Long.valueOf(this.mTotalBytes));
            values.put("current_bytes", Long.valueOf(this.mCurrentBytes));
            values.put("etag", this.mETag);
            values.put("lastmod", Long.valueOf(DownloadThread.this.mSystemFacade.currentTimeMillis()));
            values.put("errorMsg", this.mErrorMsg);
            return values;
        }

        public void writeToDatabase() {
            DownloadThread.this.mContext.getContentResolver().update(DownloadThread.this.mInfo.getAllDownloadsUri(), buildContentValues(), null, null);
        }

        public void writeToDatabaseOrThrow() throws StopRequestException {
            if (DownloadThread.this.mContext.getContentResolver().update(DownloadThread.this.mInfo.getAllDownloadsUri(), buildContentValues(), "deleted == '0'", null) == 0) {
                throw new StopRequestException(490, "Download deleted or missing!");
            }
        }
    }

    public DownloadThread(Context context, SystemFacade systemFacade, DownloadNotifier notifier, DownloadInfo info) {
        this.mContext = context;
        this.mSystemFacade = systemFacade;
        this.mNotifier = notifier;
        this.mId = info.mId;
        this.mInfo = info;
        this.mInfoDelta = new DownloadInfoDelta(info);
    }

    @Override
    public void run() {
        Process.setThreadPriority(10);
        if (DownloadInfo.queryDownloadStatus(this.mContext.getContentResolver(), this.mId) == 200) {
            logDebug("Already finished; skipping");
            return;
        }
        NetworkPolicyManager netPolicy = NetworkPolicyManager.from(this.mContext);
        PowerManager.WakeLock wakeLock = null;
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        try {
            try {
                wakeLock = pm.newWakeLock(1, "DownloadManager");
                wakeLock.setWorkSource(new WorkSource(this.mInfo.mUid));
                wakeLock.acquire();
                netPolicy.registerListener(this.mPolicyListener);
                logDebug("Starting");
                NetworkInfo info = this.mSystemFacade.getActiveNetworkInfo(this.mInfo.mUid);
                if (info != null) {
                    this.mNetworkType = info.getType();
                }
                TrafficStats.setThreadStatsTag(-255);
                TrafficStats.setThreadStatsUid(this.mInfo.mUid);
                executeDownload();
                this.mInfoDelta.mStatus = 200;
                TrafficStats.incrementOperationCount(1);
                if (this.mInfoDelta.mTotalBytes == -1) {
                    this.mInfoDelta.mTotalBytes = this.mInfoDelta.mCurrentBytes;
                }
                logDebug("Finished with status " + Downloads.Impl.statusToString(this.mInfoDelta.mStatus));
                this.mNotifier.notifyDownloadSpeed(this.mId, 0L);
                finalizeDestination();
                this.mInfoDelta.writeToDatabase();
                if (Downloads.Impl.isStatusCompleted(this.mInfoDelta.mStatus)) {
                    this.mInfo.sendIntentIfRequested();
                }
                TrafficStats.clearThreadStatsTag();
                TrafficStats.clearThreadStatsUid();
                netPolicy.unregisterListener(this.mPolicyListener);
                if (wakeLock != null) {
                    wakeLock.release();
                }
            } catch (StopRequestException e) {
                this.mInfoDelta.mStatus = e.getFinalStatus();
                this.mInfoDelta.mErrorMsg = e.getMessage();
                logWarning("Stop requested with status " + Downloads.Impl.statusToString(this.mInfoDelta.mStatus) + ": " + this.mInfoDelta.mErrorMsg);
                if (this.mInfoDelta.mStatus == 194) {
                    throw new IllegalStateException("Execution should always throw final error codes");
                }
                if (isStatusRetryable(this.mInfoDelta.mStatus)) {
                    if (this.mMadeProgress) {
                        this.mInfoDelta.mNumFailed = 1;
                    } else {
                        this.mInfoDelta.mNumFailed++;
                    }
                    if (this.mInfoDelta.mNumFailed < 5) {
                        NetworkInfo info2 = this.mSystemFacade.getActiveNetworkInfo(this.mInfo.mUid);
                        if (info2 != null && info2.getType() == this.mNetworkType && info2.isConnected()) {
                            this.mInfoDelta.mStatus = 194;
                        } else {
                            this.mInfoDelta.mStatus = 195;
                        }
                        if ((this.mInfoDelta.mETag == null && this.mMadeProgress) || DownloadDrmHelper.isDrmConvertNeeded(this.mInfoDelta.mMimeType)) {
                            this.mInfoDelta.mStatus = 489;
                        }
                    }
                }
                logDebug("Finished with status " + Downloads.Impl.statusToString(this.mInfoDelta.mStatus));
                this.mNotifier.notifyDownloadSpeed(this.mId, 0L);
                finalizeDestination();
                this.mInfoDelta.writeToDatabase();
                if (Downloads.Impl.isStatusCompleted(this.mInfoDelta.mStatus)) {
                    this.mInfo.sendIntentIfRequested();
                }
                TrafficStats.clearThreadStatsTag();
                TrafficStats.clearThreadStatsUid();
                netPolicy.unregisterListener(this.mPolicyListener);
                if (wakeLock != null) {
                    wakeLock.release();
                }
            } catch (Throwable t) {
                this.mInfoDelta.mStatus = 491;
                this.mInfoDelta.mErrorMsg = t.toString();
                logError("Failed: " + this.mInfoDelta.mErrorMsg, t);
                logDebug("Finished with status " + Downloads.Impl.statusToString(this.mInfoDelta.mStatus));
                this.mNotifier.notifyDownloadSpeed(this.mId, 0L);
                finalizeDestination();
                this.mInfoDelta.writeToDatabase();
                if (Downloads.Impl.isStatusCompleted(this.mInfoDelta.mStatus)) {
                    this.mInfo.sendIntentIfRequested();
                }
                TrafficStats.clearThreadStatsTag();
                TrafficStats.clearThreadStatsUid();
                netPolicy.unregisterListener(this.mPolicyListener);
                if (wakeLock != null) {
                    wakeLock.release();
                }
            }
        } catch (Throwable th) {
            logDebug("Finished with status " + Downloads.Impl.statusToString(this.mInfoDelta.mStatus));
            this.mNotifier.notifyDownloadSpeed(this.mId, 0L);
            finalizeDestination();
            this.mInfoDelta.writeToDatabase();
            if (Downloads.Impl.isStatusCompleted(this.mInfoDelta.mStatus)) {
                this.mInfo.sendIntentIfRequested();
            }
            TrafficStats.clearThreadStatsTag();
            TrafficStats.clearThreadStatsUid();
            netPolicy.unregisterListener(this.mPolicyListener);
            if (wakeLock != null) {
                wakeLock.release();
            }
            throw th;
        }
    }

    private void executeDownload() throws Throwable {
        boolean resuming = this.mInfoDelta.mCurrentBytes != 0;
        try {
            URL url = new URL(this.mInfoDelta.mUri);
            int redirectionCount = 0;
            URL url2 = url;
            while (true) {
                int redirectionCount2 = redirectionCount + 1;
                if (redirectionCount >= 5) {
                    throw new StopRequestException(497, "Too many redirects");
                }
                HttpURLConnection conn = null;
                try {
                    checkConnectivity();
                    conn = (HttpURLConnection) url2.openConnection();
                    conn.setInstanceFollowRedirects(false);
                    conn.setConnectTimeout(60000);
                    conn.setReadTimeout(60000);
                    addRequestHeaders(conn, resuming);
                    int responseCode = conn.getResponseCode();
                    switch (responseCode) {
                        case 200:
                            if (resuming) {
                                throw new StopRequestException(489, "Expected partial, but received OK");
                            }
                            parseOkHeaders(conn);
                            transferData(conn);
                            if (conn != null) {
                                conn.disconnect();
                                return;
                            }
                            return;
                        case 206:
                            if (!resuming) {
                                throw new StopRequestException(489, "Expected OK, but received partial");
                            }
                            transferData(conn);
                            if (conn != null) {
                                conn.disconnect();
                                return;
                            }
                            return;
                        case 301:
                        case 302:
                        case 303:
                        case 307:
                            String location = conn.getHeaderField("Location");
                            URL url3 = new URL(url2, location);
                            if (responseCode == 301) {
                                try {
                                    try {
                                        this.mInfoDelta.mUri = url3.toString();
                                    } catch (Throwable th) {
                                        th = th;
                                        if (conn != null) {
                                            conn.disconnect();
                                        }
                                        throw th;
                                    }
                                } catch (IOException e) {
                                    e = e;
                                    if (!(e instanceof ProtocolException) || !e.getMessage().startsWith("Unexpected status line")) {
                                        throw new StopRequestException(495, e);
                                    }
                                    throw new StopRequestException(494, e);
                                }
                            }
                            if (conn != null) {
                                conn.disconnect();
                                redirectionCount = redirectionCount2;
                                url2 = url3;
                            } else {
                                redirectionCount = redirectionCount2;
                                url2 = url3;
                            }
                            break;
                        case 412:
                            throw new StopRequestException(489, "Precondition failed");
                        case 416:
                            throw new StopRequestException(489, "Requested range not satisfiable");
                        case 500:
                            throw new StopRequestException(500, conn.getResponseMessage());
                        case 503:
                            parseUnavailableHeaders(conn);
                            throw new StopRequestException(503, conn.getResponseMessage());
                        default:
                            StopRequestException.throwUnhandledHttpError(responseCode, conn.getResponseMessage());
                            if (conn != null) {
                                conn.disconnect();
                            }
                            redirectionCount = redirectionCount2;
                            break;
                    }
                } catch (IOException e2) {
                    e = e2;
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        } catch (MalformedURLException e3) {
            throw new StopRequestException(400, e3);
        }
    }

    private void transferData(HttpURLConnection conn) throws Throwable {
        boolean hasLength = this.mInfoDelta.mTotalBytes != -1;
        boolean isConnectionClose = "close".equalsIgnoreCase(conn.getHeaderField("Connection"));
        boolean isEncodingChunked = "chunked".equalsIgnoreCase(conn.getHeaderField("Transfer-Encoding"));
        boolean finishKnown = hasLength || isConnectionClose || isEncodingChunked;
        if (!finishKnown) {
            throw new StopRequestException(489, "can't know size of download, giving up");
        }
        DrmManagerClient drmClient = null;
        FileDescriptor outFd = null;
        InputStream in = null;
        OutputStream in2 = null;
        try {
            try {
                in = conn.getInputStream();
                try {
                    try {
                        ParcelFileDescriptor outPfd = this.mContext.getContentResolver().openFileDescriptor(this.mInfo.getAllDownloadsUri(), "rw");
                        outFd = outPfd.getFileDescriptor();
                        if (DownloadDrmHelper.isDrmConvertNeeded(this.mInfoDelta.mMimeType)) {
                            DrmManagerClient drmClient2 = new DrmManagerClient(this.mContext);
                            try {
                                OutputStream out = new DrmOutputStream(drmClient2, outPfd, this.mInfoDelta.mMimeType);
                                in2 = out;
                                drmClient = drmClient2;
                            } catch (ErrnoException e) {
                                e = e;
                                throw new StopRequestException(492, e);
                            } catch (IOException e2) {
                                e = e2;
                                throw new StopRequestException(492, e);
                            } catch (Throwable th) {
                                th = th;
                                drmClient = drmClient2;
                                if (drmClient != null) {
                                    drmClient.release();
                                }
                                if (in2 != null) {
                                    try {
                                        in2.flush();
                                    } catch (IOException e3) {
                                        throw th;
                                    } finally {
                                    }
                                }
                                if (outFd != null) {
                                    outFd.sync();
                                }
                                throw th;
                            }
                        } else {
                            OutputStream out2 = new ParcelFileDescriptor.AutoCloseOutputStream(outPfd);
                            in2 = out2;
                        }
                        if (this.mInfoDelta.mTotalBytes > 0) {
                            long curSize = Os.fstat(outFd).st_size;
                            long newBytes = this.mInfoDelta.mTotalBytes - curSize;
                            StorageUtils.ensureAvailableSpace(this.mContext, outFd, newBytes);
                            try {
                                Os.posix_fallocate(outFd, 0L, this.mInfoDelta.mTotalBytes);
                            } catch (ErrnoException e4) {
                                if (e4.errno != OsConstants.ENOSYS && e4.errno != OsConstants.ENOTSUP) {
                                    throw e4;
                                }
                                Log.w("DownloadManager", "fallocate() not supported; falling back to ftruncate()");
                                Os.ftruncate(outFd, this.mInfoDelta.mTotalBytes);
                            }
                        }
                        Os.lseek(outFd, this.mInfoDelta.mCurrentBytes, OsConstants.SEEK_SET);
                        transferData(in, in2, outFd);
                        try {
                            if (in2 instanceof DrmOutputStream) {
                                ((DrmOutputStream) in2).finish();
                            }
                            if (drmClient != null) {
                                drmClient.release();
                            }
                            IoUtils.closeQuietly(in);
                            if (in2 != null) {
                                try {
                                    in2.flush();
                                } catch (IOException e5) {
                                    return;
                                } finally {
                                }
                            }
                            if (outFd != null) {
                                outFd.sync();
                            }
                        } catch (IOException e6) {
                            throw new StopRequestException(492, e6);
                        }
                    } catch (IOException e7) {
                        e = e7;
                    }
                } catch (ErrnoException e8) {
                    e = e8;
                }
            } catch (IOException e9) {
                throw new StopRequestException(495, e9);
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void transferData(InputStream in, OutputStream out, FileDescriptor outFd) throws StopRequestException {
        byte[] buffer = new byte[8192];
        while (true) {
            checkPausedOrCanceled();
            try {
                int len = in.read(buffer);
                if (len == -1) {
                    break;
                }
                try {
                    if (this.mInfoDelta.mTotalBytes == -1) {
                        long curSize = Os.fstat(outFd).st_size;
                        long newBytes = (this.mInfoDelta.mCurrentBytes + ((long) len)) - curSize;
                        StorageUtils.ensureAvailableSpace(this.mContext, outFd, newBytes);
                    }
                    out.write(buffer, 0, len);
                    this.mMadeProgress = true;
                    this.mInfoDelta.mCurrentBytes += (long) len;
                    updateProgress(outFd);
                } catch (ErrnoException e) {
                    throw new StopRequestException(492, e);
                } catch (IOException e2) {
                    throw new StopRequestException(492, e2);
                }
            } catch (IOException e3) {
                throw new StopRequestException(495, "Failed reading response: " + e3, e3);
            }
        }
        if (this.mInfoDelta.mTotalBytes != -1 && this.mInfoDelta.mCurrentBytes != this.mInfoDelta.mTotalBytes) {
            throw new StopRequestException(495, "Content length mismatch");
        }
    }

    private void finalizeDestination() {
        if (Downloads.Impl.isStatusError(this.mInfoDelta.mStatus)) {
            try {
                ParcelFileDescriptor target = this.mContext.getContentResolver().openFileDescriptor(this.mInfo.getAllDownloadsUri(), "rw");
                try {
                    Os.ftruncate(target.getFileDescriptor(), 0L);
                    IoUtils.closeQuietly(target);
                } catch (ErrnoException e) {
                    IoUtils.closeQuietly(target);
                } catch (Throwable th) {
                    IoUtils.closeQuietly(target);
                    throw th;
                }
            } catch (FileNotFoundException e2) {
            }
            if (this.mInfoDelta.mFileName != null) {
                new File(this.mInfoDelta.mFileName).delete();
                this.mInfoDelta.mFileName = null;
                return;
            }
            return;
        }
        if (Downloads.Impl.isStatusSuccess(this.mInfoDelta.mStatus) && this.mInfoDelta.mFileName != null) {
            try {
                Os.chmod(this.mInfoDelta.mFileName, 420);
            } catch (ErrnoException e3) {
            }
            if (this.mInfo.mDestination != 4) {
                try {
                    File before = new File(this.mInfoDelta.mFileName);
                    File beforeDir = Helpers.getRunningDestinationDirectory(this.mContext, this.mInfo.mDestination);
                    File afterDir = Helpers.getSuccessDestinationDirectory(this.mContext, this.mInfo.mDestination);
                    if (!beforeDir.equals(afterDir) && before.getParentFile().equals(beforeDir)) {
                        File after = new File(afterDir, before.getName());
                        if (before.renameTo(after)) {
                            this.mInfoDelta.mFileName = after.getAbsolutePath();
                        }
                    }
                } catch (IOException e4) {
                }
            }
        }
    }

    private void checkConnectivity() throws StopRequestException {
        this.mPolicyDirty = false;
        DownloadInfo.NetworkState networkUsable = this.mInfo.checkCanUseNetwork(this.mInfoDelta.mTotalBytes);
        if (networkUsable != DownloadInfo.NetworkState.OK) {
            int status = 195;
            if (networkUsable == DownloadInfo.NetworkState.UNUSABLE_DUE_TO_SIZE) {
                status = 196;
                this.mInfo.notifyPauseDueToSize(true);
            } else if (networkUsable == DownloadInfo.NetworkState.RECOMMENDED_UNUSABLE_DUE_TO_SIZE) {
                status = 196;
                this.mInfo.notifyPauseDueToSize(false);
            }
            throw new StopRequestException(status, networkUsable.name());
        }
    }

    private void checkPausedOrCanceled() throws StopRequestException {
        synchronized (this.mInfo) {
            if (this.mInfo.mControl == 1) {
                throw new StopRequestException(193, "download paused by owner");
            }
            if (this.mInfo.mStatus == 490 || this.mInfo.mDeleted) {
                throw new StopRequestException(490, "download canceled");
            }
        }
        if (this.mPolicyDirty) {
            checkConnectivity();
        }
    }

    private void updateProgress(FileDescriptor outFd) throws IOException, StopRequestException {
        long now = SystemClock.elapsedRealtime();
        long currentBytes = this.mInfoDelta.mCurrentBytes;
        long sampleDelta = now - this.mSpeedSampleStart;
        if (sampleDelta > 500) {
            long sampleSpeed = ((currentBytes - this.mSpeedSampleBytes) * 1000) / sampleDelta;
            if (this.mSpeed == 0) {
                this.mSpeed = sampleSpeed;
            } else {
                this.mSpeed = ((this.mSpeed * 3) + sampleSpeed) / 4;
            }
            if (this.mSpeedSampleStart != 0) {
                this.mNotifier.notifyDownloadSpeed(this.mId, this.mSpeed);
            }
            this.mSpeedSampleStart = now;
            this.mSpeedSampleBytes = currentBytes;
        }
        long bytesDelta = currentBytes - this.mLastUpdateBytes;
        long timeDelta = now - this.mLastUpdateTime;
        if (bytesDelta > 65536 && timeDelta > 2000) {
            outFd.sync();
            this.mInfoDelta.writeToDatabaseOrThrow();
            this.mLastUpdateBytes = currentBytes;
            this.mLastUpdateTime = now;
        }
    }

    private void parseOkHeaders(HttpURLConnection conn) throws StopRequestException {
        if (this.mInfoDelta.mFileName == null) {
            String contentDisposition = conn.getHeaderField("Content-Disposition");
            String contentLocation = conn.getHeaderField("Content-Location");
            try {
                this.mInfoDelta.mFileName = Helpers.generateSaveFile(this.mContext, this.mInfoDelta.mUri, this.mInfo.mHint, contentDisposition, contentLocation, this.mInfoDelta.mMimeType, this.mInfo.mDestination);
            } catch (IOException e) {
                throw new StopRequestException(492, "Failed to generate filename: " + e);
            }
        }
        if (this.mInfoDelta.mMimeType == null) {
            this.mInfoDelta.mMimeType = Intent.normalizeMimeType(conn.getContentType());
        }
        String transferEncoding = conn.getHeaderField("Transfer-Encoding");
        if (transferEncoding == null) {
            this.mInfoDelta.mTotalBytes = getHeaderFieldLong(conn, "Content-Length", -1L);
        } else {
            this.mInfoDelta.mTotalBytes = -1L;
        }
        this.mInfoDelta.mETag = conn.getHeaderField("ETag");
        this.mInfoDelta.writeToDatabaseOrThrow();
        checkConnectivity();
    }

    private void parseUnavailableHeaders(HttpURLConnection conn) {
        long retryAfter;
        long retryAfter2 = conn.getHeaderFieldInt("Retry-After", -1);
        if (retryAfter2 < 0) {
            retryAfter = 0;
        } else {
            if (retryAfter2 < 30) {
                retryAfter2 = 30;
            } else if (retryAfter2 > 86400) {
                retryAfter2 = 86400;
            }
            retryAfter = retryAfter2 + ((long) Helpers.sRandom.nextInt(31));
        }
        this.mInfoDelta.mRetryAfter = (int) (1000 * retryAfter);
    }

    private void addRequestHeaders(HttpURLConnection conn, boolean resuming) {
        for (Pair<String, String> header : this.mInfo.getHeaders()) {
            conn.addRequestProperty((String) header.first, (String) header.second);
        }
        if (conn.getRequestProperty("User-Agent") == null) {
            conn.addRequestProperty("User-Agent", this.mInfo.getUserAgent());
        }
        conn.setRequestProperty("Accept-Encoding", "identity");
        conn.setRequestProperty("Connection", "close");
        if (resuming) {
            if (this.mInfoDelta.mETag != null) {
                conn.addRequestProperty("If-Match", this.mInfoDelta.mETag);
            }
            conn.addRequestProperty("Range", "bytes=" + this.mInfoDelta.mCurrentBytes + "-");
        }
    }

    private void logDebug(String msg) {
        Log.d("DownloadManager", "[" + this.mId + "] " + msg);
    }

    private void logWarning(String msg) {
        Log.w("DownloadManager", "[" + this.mId + "] " + msg);
    }

    private void logError(String msg, Throwable t) {
        Log.e("DownloadManager", "[" + this.mId + "] " + msg, t);
    }

    private static long getHeaderFieldLong(URLConnection conn, String field, long defaultValue) {
        try {
            long defaultValue2 = Long.parseLong(conn.getHeaderField(field));
            return defaultValue2;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean isStatusRetryable(int status) {
        switch (status) {
            case 492:
            case 495:
            case 500:
            case 503:
                return true;
            default:
                return false;
        }
    }
}
