package com.android.providers.downloads;

import android.app.job.JobParameters;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.drm.DrmManagerClient;
import android.drm.DrmOutputStream;
import android.net.INetworkPolicyListener;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkPolicyManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.provider.Downloads;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.MathUtils;
import android.util.Pair;
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
import java.security.GeneralSecurityException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import libcore.io.IoUtils;

public class DownloadThread extends Thread {
    private final Context mContext;
    private final long mId;
    private boolean mIgnoreBlocked;
    private final DownloadInfo mInfo;
    private final DownloadInfoDelta mInfoDelta;
    private final DownloadJobService mJobService;
    private Network mNetwork;
    private final NetworkPolicyManager mNetworkPolicy;
    private final DownloadNotifier mNotifier;
    private final JobParameters mParams;
    private volatile boolean mPolicyDirty;
    private volatile boolean mShutdownRequested;
    private long mSpeed;
    private long mSpeedSampleBytes;
    private long mSpeedSampleStart;
    private final StorageManager mStorage;
    private final SystemFacade mSystemFacade;
    private boolean mMadeProgress = false;
    private long mLastUpdateBytes = 0;
    private long mLastUpdateTime = 0;
    private int mNetworkType = -1;
    private INetworkPolicyListener mPolicyListener = new NetworkPolicyManager.Listener() {
        public void onUidRulesChanged(int i, int i2) {
            if (i == DownloadThread.this.mInfo.mUid) {
                DownloadThread.this.mPolicyDirty = true;
            }
        }

        public void onMeteredIfacesChanged(String[] strArr) {
            DownloadThread.this.mPolicyDirty = true;
        }

        public void onRestrictBackgroundChanged(boolean z) {
            DownloadThread.this.mPolicyDirty = true;
        }

        public void onUidPoliciesChanged(int i, int i2) {
            if (i == DownloadThread.this.mInfo.mUid) {
                DownloadThread.this.mPolicyDirty = true;
            }
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

        public DownloadInfoDelta(DownloadInfo downloadInfo) {
            this.mUri = downloadInfo.mUri;
            this.mFileName = downloadInfo.mFileName;
            this.mMimeType = downloadInfo.mMimeType;
            this.mStatus = downloadInfo.mStatus;
            this.mNumFailed = downloadInfo.mNumFailed;
            this.mRetryAfter = downloadInfo.mRetryAfter;
            this.mTotalBytes = downloadInfo.mTotalBytes;
            this.mCurrentBytes = downloadInfo.mCurrentBytes;
            this.mETag = downloadInfo.mETag;
        }

        private ContentValues buildContentValues() {
            ContentValues contentValues = new ContentValues();
            contentValues.put("uri", this.mUri);
            contentValues.put("_data", this.mFileName);
            contentValues.put("mimetype", this.mMimeType);
            contentValues.put("status", Integer.valueOf(this.mStatus));
            contentValues.put("numfailed", Integer.valueOf(this.mNumFailed));
            contentValues.put("method", Integer.valueOf(this.mRetryAfter));
            contentValues.put("total_bytes", Long.valueOf(this.mTotalBytes));
            contentValues.put("current_bytes", Long.valueOf(this.mCurrentBytes));
            contentValues.put("etag", this.mETag);
            contentValues.put("lastmod", Long.valueOf(DownloadThread.this.mSystemFacade.currentTimeMillis()));
            contentValues.put("errorMsg", this.mErrorMsg);
            return contentValues;
        }

        public void writeToDatabase() {
            DownloadThread.this.mContext.getContentResolver().update(DownloadThread.this.mInfo.getAllDownloadsUri(), buildContentValues(), null, null);
        }

        public void writeToDatabaseOrThrow() throws StopRequestException {
            if (DownloadThread.this.mContext.getContentResolver().update(DownloadThread.this.mInfo.getAllDownloadsUri(), buildContentValues(), "status != '490' AND deleted == '0' AND (control IS NULL OR control != '1')", null) == 0) {
                if (DownloadThread.this.mInfo.queryDownloadControl() == 1) {
                    throw new StopRequestException(193, "Download paused!");
                }
                throw new StopRequestException(490, "Download deleted or missing!");
            }
        }
    }

    public DownloadThread(DownloadJobService downloadJobService, JobParameters jobParameters, DownloadInfo downloadInfo) {
        this.mContext = downloadJobService;
        this.mSystemFacade = Helpers.getSystemFacade(this.mContext);
        this.mNotifier = Helpers.getDownloadNotifier(this.mContext);
        this.mNetworkPolicy = (NetworkPolicyManager) this.mContext.getSystemService(NetworkPolicyManager.class);
        this.mStorage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        this.mJobService = downloadJobService;
        this.mParams = jobParameters;
        this.mId = downloadInfo.mId;
        this.mInfo = downloadInfo;
        this.mInfoDelta = new DownloadInfoDelta(downloadInfo);
    }

    @Override
    public void run() {
        StringBuilder sb;
        Process.setThreadPriority(10);
        if (this.mInfo.queryDownloadStatus() == 200) {
            logDebug("Already finished; skipping");
            return;
        }
        try {
            try {
                this.mNetworkPolicy.registerListener(this.mPolicyListener);
                logDebug("Starting");
                this.mInfoDelta.mStatus = 192;
                this.mInfoDelta.writeToDatabase();
                this.mIgnoreBlocked = this.mInfo.isVisible();
                this.mNetwork = this.mSystemFacade.getNetwork(this.mParams);
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
                        NetworkInfo networkInfo = this.mSystemFacade.getNetworkInfo(this.mNetwork, this.mInfo.mUid, this.mIgnoreBlocked);
                        if (networkInfo != null && networkInfo.getType() == this.mNetworkType && networkInfo.isConnected()) {
                            this.mInfoDelta.mStatus = 194;
                        } else {
                            this.mInfoDelta.mStatus = 195;
                        }
                        if ((this.mInfoDelta.mETag == null && this.mMadeProgress) || DownloadDrmHelper.isDrmConvertNeeded(this.mInfoDelta.mMimeType)) {
                            this.mInfoDelta.mStatus = 489;
                        }
                    }
                }
                if (this.mInfoDelta.mStatus == 195 && !this.mInfo.isMeteredAllowed(this.mInfoDelta.mTotalBytes)) {
                    this.mInfoDelta.mStatus = 196;
                }
                sb = new StringBuilder();
            } catch (Throwable th) {
                this.mInfoDelta.mStatus = 491;
                this.mInfoDelta.mErrorMsg = th.toString();
                logError("Failed: " + this.mInfoDelta.mErrorMsg, th);
                sb = new StringBuilder();
            }
            if (this.mNetwork == null) {
                throw new StopRequestException(195, "No network associated with requesting UID");
            }
            NetworkInfo networkInfo2 = this.mSystemFacade.getNetworkInfo(this.mNetwork, this.mInfo.mUid, this.mIgnoreBlocked);
            if (networkInfo2 != null) {
                this.mNetworkType = networkInfo2.getType();
            }
            TrafficStats.setThreadStatsTag(-255);
            TrafficStats.setThreadStatsUid(this.mInfo.mUid);
            executeDownload();
            this.mInfoDelta.mStatus = 200;
            TrafficStats.incrementOperationCount(1);
            if (this.mInfoDelta.mTotalBytes == -1) {
                this.mInfoDelta.mTotalBytes = this.mInfoDelta.mCurrentBytes;
            }
            sb = new StringBuilder();
            sb.append("Finished with status ");
            sb.append(Downloads.Impl.statusToString(this.mInfoDelta.mStatus));
            logDebug(sb.toString());
            this.mNotifier.notifyDownloadSpeed(this.mId, 0L);
            finalizeDestination();
            this.mInfoDelta.writeToDatabase();
            TrafficStats.clearThreadStatsTag();
            TrafficStats.clearThreadStatsUid();
            this.mNetworkPolicy.unregisterListener(this.mPolicyListener);
            boolean z = false;
            if (Downloads.Impl.isStatusCompleted(this.mInfoDelta.mStatus)) {
                if (this.mInfo.shouldScanFile(this.mInfoDelta.mStatus)) {
                    DownloadScanner.requestScanBlocking(this.mContext, this.mInfo.mId, this.mInfoDelta.mFileName, this.mInfoDelta.mMimeType);
                }
            } else if (this.mInfoDelta.mStatus == 194 || this.mInfoDelta.mStatus == 195 || this.mInfoDelta.mStatus == 196) {
                z = true;
            }
            this.mJobService.jobFinishedInternal(this.mParams, z);
        } catch (Throwable th2) {
            logDebug("Finished with status " + Downloads.Impl.statusToString(this.mInfoDelta.mStatus));
            this.mNotifier.notifyDownloadSpeed(this.mId, 0L);
            finalizeDestination();
            this.mInfoDelta.writeToDatabase();
            TrafficStats.clearThreadStatsTag();
            TrafficStats.clearThreadStatsUid();
            this.mNetworkPolicy.unregisterListener(this.mPolicyListener);
            throw th2;
        }
    }

    public void requestShutdown() {
        this.mShutdownRequested = true;
    }

    private void executeDownload() throws Throwable {
        HttpURLConnection httpURLConnection;
        boolean z = this.mInfoDelta.mCurrentBytes != 0;
        try {
            URL url = new URL(this.mInfoDelta.mUri);
            boolean zIsCleartextTrafficPermitted = this.mSystemFacade.isCleartextTrafficPermitted(this.mInfo.mPackage, url.getHost());
            try {
                SSLContext sSLContextForPackage = this.mSystemFacade.getSSLContextForPackage(this.mContext, this.mInfo.mPackage);
                URL url2 = url;
                int i = 0;
                while (true) {
                    int i2 = i + 1;
                    if (i >= 5) {
                        throw new StopRequestException(497, "Too many redirects");
                    }
                    if (!zIsCleartextTrafficPermitted && "http".equalsIgnoreCase(url2.getProtocol())) {
                        throw new StopRequestException(400, "Cleartext traffic not permitted for package " + this.mInfo.mPackage + ": " + Uri.parse(url2.toString()).toSafeString());
                    }
                    try {
                        try {
                            checkConnectivity();
                            httpURLConnection = (HttpURLConnection) this.mNetwork.openConnection(url2);
                        } catch (IOException e) {
                            e = e;
                        }
                    } catch (Throwable th) {
                        th = th;
                        httpURLConnection = null;
                    }
                    try {
                        httpURLConnection.setInstanceFollowRedirects(false);
                        httpURLConnection.setConnectTimeout(20000);
                        httpURLConnection.setReadTimeout(20000);
                        if (httpURLConnection instanceof HttpsURLConnection) {
                            ((HttpsURLConnection) httpURLConnection).setSSLSocketFactory(sSLContextForPackage.getSocketFactory());
                        }
                        addRequestHeaders(httpURLConnection, z);
                        int responseCode = httpURLConnection.getResponseCode();
                        if (responseCode == 200) {
                            if (z) {
                                throw new StopRequestException(489, "Expected partial, but received OK");
                            }
                            parseOkHeaders(httpURLConnection);
                            transferData(httpURLConnection);
                            if (httpURLConnection != null) {
                                httpURLConnection.disconnect();
                                return;
                            }
                            return;
                        }
                        if (responseCode == 206) {
                            if (!z) {
                                throw new StopRequestException(489, "Expected OK, but received partial");
                            }
                            transferData(httpURLConnection);
                            if (httpURLConnection != null) {
                                httpURLConnection.disconnect();
                                return;
                            }
                            return;
                        }
                        if (responseCode != 307) {
                            if (responseCode == 412) {
                                throw new StopRequestException(489, "Precondition failed");
                            }
                            if (responseCode == 416) {
                                throw new StopRequestException(489, "Requested range not satisfiable");
                            }
                            if (responseCode == 500) {
                                throw new StopRequestException(500, httpURLConnection.getResponseMessage());
                            }
                            if (responseCode == 503) {
                                parseUnavailableHeaders(httpURLConnection);
                                throw new StopRequestException(503, httpURLConnection.getResponseMessage());
                            }
                            switch (responseCode) {
                                case 301:
                                case 302:
                                case 303:
                                    break;
                                default:
                                    StopRequestException.throwUnhandledHttpError(responseCode, httpURLConnection.getResponseMessage());
                                    if (httpURLConnection != null) {
                                        httpURLConnection.disconnect();
                                    }
                                    i = i2;
                                    continue;
                            }
                        }
                        URL url3 = new URL(url2, httpURLConnection.getHeaderField("Location"));
                        if (responseCode == 301) {
                            this.mInfoDelta.mUri = url3.toString();
                        }
                        if (httpURLConnection != null) {
                            httpURLConnection.disconnect();
                        }
                        i = i2;
                        url2 = url3;
                    } catch (IOException e2) {
                        e = e2;
                        if (!(e instanceof ProtocolException) || !e.getMessage().startsWith("Unexpected status line")) {
                            throw new StopRequestException(495, e);
                        }
                        throw new StopRequestException(494, e);
                    } catch (Throwable th2) {
                        th = th2;
                        if (httpURLConnection != null) {
                            httpURLConnection.disconnect();
                        }
                        throw th;
                    }
                }
            } catch (GeneralSecurityException e3) {
                throw new StopRequestException(491, "Unable to create SSLContext.");
            }
        } catch (MalformedURLException e4) {
            throw new StopRequestException(400, e4);
        }
    }

    private void transferData(HttpURLConnection httpURLConnection) throws Throwable {
        InputStream inputStream;
        boolean z = this.mInfoDelta.mTotalBytes != -1;
        ?? EqualsIgnoreCase = "close".equalsIgnoreCase(httpURLConnection.getHeaderField("Connection"));
        ?? EqualsIgnoreCase2 = "chunked".equalsIgnoreCase(httpURLConnection.getHeaderField("Transfer-Encoding"));
        if (!((!z && EqualsIgnoreCase == 0 && EqualsIgnoreCase2 == 0) ? false : true)) {
            throw new StopRequestException(489, "can't know size of download, giving up");
        }
        DrmManagerClient drmManagerClient = null;
        try {
            try {
                inputStream = httpURLConnection.getInputStream();
            } catch (IOException e) {
                throw new StopRequestException(495, e);
            }
        } catch (Throwable th) {
            th = th;
            inputStream = null;
            EqualsIgnoreCase = 0;
        }
        try {
            try {
                ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor = this.mContext.getContentResolver().openFileDescriptor(this.mInfo.getAllDownloadsUri(), "rw");
                EqualsIgnoreCase = parcelFileDescriptorOpenFileDescriptor.getFileDescriptor();
                try {
                    if (DownloadDrmHelper.isDrmConvertNeeded(this.mInfoDelta.mMimeType)) {
                        DrmManagerClient drmManagerClient2 = new DrmManagerClient(this.mContext);
                        try {
                            drmManagerClient = drmManagerClient2;
                            EqualsIgnoreCase2 = new DrmOutputStream(drmManagerClient2, parcelFileDescriptorOpenFileDescriptor, this.mInfoDelta.mMimeType);
                        } catch (ErrnoException e2) {
                            e = e2;
                            throw new StopRequestException(492, e);
                        } catch (IOException e3) {
                            e = e3;
                            throw new StopRequestException(492, e);
                        } catch (Throwable th2) {
                            th = th2;
                            EqualsIgnoreCase2 = 0;
                            drmManagerClient = drmManagerClient2;
                            if (drmManagerClient != null) {
                                drmManagerClient.close();
                            }
                            IoUtils.closeQuietly(inputStream);
                            if (EqualsIgnoreCase2 != 0) {
                                try {
                                    EqualsIgnoreCase2.flush();
                                } catch (IOException e4) {
                                    IoUtils.closeQuietly((AutoCloseable) EqualsIgnoreCase2);
                                    throw th;
                                } catch (Throwable th3) {
                                    IoUtils.closeQuietly((AutoCloseable) EqualsIgnoreCase2);
                                    throw th3;
                                }
                            }
                            if (EqualsIgnoreCase != 0) {
                                EqualsIgnoreCase.sync();
                            }
                            IoUtils.closeQuietly((AutoCloseable) EqualsIgnoreCase2);
                            throw th;
                        }
                    } else {
                        EqualsIgnoreCase2 = new ParcelFileDescriptor.AutoCloseOutputStream(parcelFileDescriptorOpenFileDescriptor);
                    }
                    try {
                        Os.lseek(EqualsIgnoreCase, this.mInfoDelta.mCurrentBytes, OsConstants.SEEK_SET);
                        try {
                            if (this.mInfoDelta.mTotalBytes > 0 && this.mStorage.isAllocationSupported(EqualsIgnoreCase)) {
                                this.mStorage.allocateBytes(EqualsIgnoreCase, this.mInfoDelta.mTotalBytes);
                            }
                            transferData(inputStream, EqualsIgnoreCase2, EqualsIgnoreCase);
                            try {
                                if (EqualsIgnoreCase2 instanceof DrmOutputStream) {
                                    ((DrmOutputStream) EqualsIgnoreCase2).finish();
                                }
                                if (drmManagerClient != null) {
                                    drmManagerClient.close();
                                }
                                IoUtils.closeQuietly(inputStream);
                                try {
                                    EqualsIgnoreCase2.flush();
                                    if (EqualsIgnoreCase != 0) {
                                        EqualsIgnoreCase.sync();
                                    }
                                } catch (IOException e5) {
                                } catch (Throwable th4) {
                                    IoUtils.closeQuietly((AutoCloseable) EqualsIgnoreCase2);
                                    throw th4;
                                }
                                IoUtils.closeQuietly((AutoCloseable) EqualsIgnoreCase2);
                            } catch (IOException e6) {
                                throw new StopRequestException(492, e6);
                            }
                        } catch (IOException e7) {
                            throw new StopRequestException(198, e7);
                        }
                    } catch (ErrnoException e8) {
                        e = e8;
                        throw new StopRequestException(492, e);
                    } catch (IOException e9) {
                        e = e9;
                        throw new StopRequestException(492, e);
                    }
                } catch (ErrnoException e10) {
                    e = e10;
                } catch (IOException e11) {
                    e = e11;
                } catch (Throwable th5) {
                    th = th5;
                    EqualsIgnoreCase2 = 0;
                }
            } catch (Throwable th6) {
                th = th6;
            }
        } catch (ErrnoException e12) {
            e = e12;
        } catch (IOException e13) {
            e = e13;
        } catch (Throwable th7) {
            th = th7;
            EqualsIgnoreCase = 0;
            EqualsIgnoreCase2 = EqualsIgnoreCase;
            if (drmManagerClient != null) {
            }
            IoUtils.closeQuietly(inputStream);
            if (EqualsIgnoreCase2 != 0) {
            }
            if (EqualsIgnoreCase != 0) {
            }
            IoUtils.closeQuietly((AutoCloseable) EqualsIgnoreCase2);
            throw th;
        }
    }

    private void transferData(InputStream inputStream, OutputStream outputStream, FileDescriptor fileDescriptor) throws StopRequestException {
        byte[] bArr = new byte[8192];
        while (true) {
            if (this.mPolicyDirty) {
                checkConnectivity();
            }
            if (this.mShutdownRequested) {
                throw new StopRequestException(495, "Local halt requested; job probably timed out");
            }
            try {
                int i = inputStream.read(bArr);
                if (i == -1) {
                    if (this.mInfoDelta.mTotalBytes != -1 && this.mInfoDelta.mCurrentBytes != this.mInfoDelta.mTotalBytes) {
                        throw new StopRequestException(495, "Content length mismatch; found " + this.mInfoDelta.mCurrentBytes + " instead of " + this.mInfoDelta.mTotalBytes);
                    }
                    return;
                }
                try {
                    outputStream.write(bArr, 0, i);
                    this.mMadeProgress = true;
                    this.mInfoDelta.mCurrentBytes += (long) i;
                    updateProgress(fileDescriptor);
                } catch (IOException e) {
                    throw new StopRequestException(492, e);
                }
            } catch (IOException e2) {
                throw new StopRequestException(495, "Failed reading response: " + e2, e2);
            }
        }
    }

    private void finalizeDestination() {
        if (Downloads.Impl.isStatusError(this.mInfoDelta.mStatus)) {
            try {
                ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor = this.mContext.getContentResolver().openFileDescriptor(this.mInfo.getAllDownloadsUri(), "rw");
                try {
                    try {
                        Os.ftruncate(parcelFileDescriptorOpenFileDescriptor.getFileDescriptor(), 0L);
                        IoUtils.closeQuietly(parcelFileDescriptorOpenFileDescriptor);
                    } finally {
                        IoUtils.closeQuietly(parcelFileDescriptorOpenFileDescriptor);
                    }
                } catch (ErrnoException e) {
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
        if (Downloads.Impl.isStatusSuccess(this.mInfoDelta.mStatus) && this.mInfoDelta.mFileName != null && this.mInfo.mDestination != 4) {
            try {
                File file = new File(this.mInfoDelta.mFileName);
                File runningDestinationDirectory = Helpers.getRunningDestinationDirectory(this.mContext, this.mInfo.mDestination);
                File successDestinationDirectory = Helpers.getSuccessDestinationDirectory(this.mContext, this.mInfo.mDestination);
                if (!runningDestinationDirectory.equals(successDestinationDirectory) && file.getParentFile().equals(runningDestinationDirectory)) {
                    File file2 = new File(successDestinationDirectory, file.getName());
                    if (file.renameTo(file2)) {
                        this.mInfoDelta.mFileName = file2.getAbsolutePath();
                    }
                }
            } catch (IOException e3) {
            }
        }
    }

    private void checkConnectivity() throws StopRequestException {
        this.mPolicyDirty = false;
        NetworkInfo networkInfo = this.mSystemFacade.getNetworkInfo(this.mNetwork, this.mInfo.mUid, this.mIgnoreBlocked);
        NetworkCapabilities networkCapabilities = this.mSystemFacade.getNetworkCapabilities(this.mNetwork);
        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new StopRequestException(195, "Network is disconnected");
        }
        if (!networkCapabilities.hasCapability(18) && !this.mInfo.isRoamingAllowed()) {
            throw new StopRequestException(195, "Network is roaming");
        }
        if (!networkCapabilities.hasCapability(11) && !this.mInfo.isMeteredAllowed(this.mInfoDelta.mTotalBytes)) {
            throw new StopRequestException(195, "Network is metered");
        }
    }

    private void updateProgress(FileDescriptor fileDescriptor) throws IOException, StopRequestException {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        long j = this.mInfoDelta.mCurrentBytes;
        long j2 = jElapsedRealtime - this.mSpeedSampleStart;
        if (j2 > 500) {
            long j3 = ((j - this.mSpeedSampleBytes) * 1000) / j2;
            if (this.mSpeed == 0) {
                this.mSpeed = j3;
            } else {
                this.mSpeed = ((this.mSpeed * 3) + j3) / 4;
            }
            if (this.mSpeedSampleStart != 0) {
                this.mNotifier.notifyDownloadSpeed(this.mId, this.mSpeed);
            }
            this.mSpeedSampleStart = jElapsedRealtime;
            this.mSpeedSampleBytes = j;
        }
        long j4 = j - this.mLastUpdateBytes;
        long j5 = jElapsedRealtime - this.mLastUpdateTime;
        if (j4 > 65536 && j5 > 2000) {
            fileDescriptor.sync();
            this.mInfoDelta.writeToDatabaseOrThrow();
            this.mLastUpdateBytes = j;
            this.mLastUpdateTime = jElapsedRealtime;
        }
    }

    private void parseOkHeaders(HttpURLConnection httpURLConnection) throws StopRequestException {
        if (this.mInfoDelta.mFileName == null) {
            try {
                this.mInfoDelta.mFileName = Helpers.generateSaveFile(this.mContext, this.mInfoDelta.mUri, this.mInfo.mHint, httpURLConnection.getHeaderField("Content-Disposition"), httpURLConnection.getHeaderField("Content-Location"), this.mInfoDelta.mMimeType, this.mInfo.mDestination);
            } catch (IOException e) {
                throw new StopRequestException(492, "Failed to generate filename: " + e);
            }
        }
        if (this.mInfoDelta.mMimeType == null) {
            this.mInfoDelta.mMimeType = Intent.normalizeMimeType(httpURLConnection.getContentType());
        }
        if (httpURLConnection.getHeaderField("Transfer-Encoding") != null) {
            this.mInfoDelta.mTotalBytes = -1L;
        } else {
            this.mInfoDelta.mTotalBytes = getHeaderFieldLong(httpURLConnection, "Content-Length", -1L);
        }
        this.mInfoDelta.mETag = httpURLConnection.getHeaderField("ETag");
        this.mInfoDelta.writeToDatabaseOrThrow();
        checkConnectivity();
    }

    private void parseUnavailableHeaders(HttpURLConnection httpURLConnection) {
        this.mInfoDelta.mRetryAfter = (int) (MathUtils.constrain(httpURLConnection.getHeaderFieldInt("Retry-After", -1), 30L, 86400L) * 1000);
    }

    private void addRequestHeaders(HttpURLConnection httpURLConnection, boolean z) {
        for (Pair<String, String> pair : this.mInfo.getHeaders()) {
            httpURLConnection.addRequestProperty((String) pair.first, (String) pair.second);
        }
        if (httpURLConnection.getRequestProperty("User-Agent") == null) {
            httpURLConnection.addRequestProperty("User-Agent", this.mInfo.getUserAgent());
        }
        httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
        httpURLConnection.setRequestProperty("Connection", "close");
        if (z) {
            if (this.mInfoDelta.mETag != null) {
                httpURLConnection.addRequestProperty("If-Match", this.mInfoDelta.mETag);
            }
            httpURLConnection.addRequestProperty("Range", "bytes=" + this.mInfoDelta.mCurrentBytes + "-");
        }
    }

    private void logDebug(String str) {
        Log.d("DownloadManager", "[" + this.mId + "] " + str);
    }

    private void logWarning(String str) {
        Log.w("DownloadManager", "[" + this.mId + "] " + str);
    }

    private void logError(String str, Throwable th) {
        Log.e("DownloadManager", "[" + this.mId + "] " + str, th);
    }

    private static long getHeaderFieldLong(URLConnection uRLConnection, String str, long j) {
        try {
            return Long.parseLong(uRLConnection.getHeaderField(str));
        } catch (NumberFormatException e) {
            return j;
        }
    }

    public static boolean isStatusRetryable(int i) {
        if (i == 492 || i == 495 || i == 500 || i == 503) {
            return true;
        }
        return false;
    }
}
