package com.android.providers.downloads;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.internal.util.IndentingPrintWriter;
import java.io.CharArrayWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DownloadInfo {
    public boolean mAllowMetered;
    public boolean mAllowRoaming;
    public int mAllowedNetworkTypes;
    public int mBypassRecommendedSizeLimit;
    public String mClass;
    private final Context mContext;
    public int mControl;
    public String mCookies;
    public long mCurrentBytes;
    public boolean mDeleted;
    public String mDescription;
    public int mDestination;
    public String mETag;
    public String mExtras;
    public String mFileName;
    public int mFlags;
    public String mHint;
    public long mId;
    public boolean mIsPublicApi;
    public long mLastMod;
    public String mMediaProviderUri;
    public int mMediaScanned;
    public String mMimeType;

    @Deprecated
    public boolean mNoIntegrity;
    public int mNumFailed;
    public String mPackage;
    public String mReferer;
    private List<Pair<String, String>> mRequestHeaders = new ArrayList();
    public int mRetryAfter;
    public int mStatus;
    private final SystemFacade mSystemFacade;
    public String mTitle;
    public long mTotalBytes;
    public int mUid;
    public String mUri;
    public String mUserAgent;
    public int mVisibility;

    public static class Reader {
        private Cursor mCursor;
        private ContentResolver mResolver;

        public Reader(ContentResolver contentResolver, Cursor cursor) {
            this.mResolver = contentResolver;
            this.mCursor = cursor;
        }

        public void updateFromDatabase(DownloadInfo downloadInfo) {
            downloadInfo.mId = getLong("_id").longValue();
            downloadInfo.mUri = getString("uri");
            downloadInfo.mNoIntegrity = getInt("no_integrity").intValue() == 1;
            downloadInfo.mHint = getString("hint");
            downloadInfo.mFileName = getString("_data");
            downloadInfo.mMimeType = Intent.normalizeMimeType(getString("mimetype"));
            downloadInfo.mDestination = getInt("destination").intValue();
            downloadInfo.mVisibility = getInt("visibility").intValue();
            downloadInfo.mStatus = getInt("status").intValue();
            downloadInfo.mNumFailed = getInt("numfailed").intValue();
            downloadInfo.mRetryAfter = getInt("method").intValue() & 268435455;
            downloadInfo.mLastMod = getLong("lastmod").longValue();
            downloadInfo.mPackage = getString("notificationpackage");
            downloadInfo.mClass = getString("notificationclass");
            downloadInfo.mExtras = getString("notificationextras");
            downloadInfo.mCookies = getString("cookiedata");
            downloadInfo.mUserAgent = getString("useragent");
            downloadInfo.mReferer = getString("referer");
            downloadInfo.mTotalBytes = getLong("total_bytes").longValue();
            downloadInfo.mCurrentBytes = getLong("current_bytes").longValue();
            downloadInfo.mETag = getString("etag");
            downloadInfo.mUid = getInt("uid").intValue();
            downloadInfo.mMediaScanned = getInt("scanned").intValue();
            downloadInfo.mDeleted = getInt("deleted").intValue() == 1;
            downloadInfo.mMediaProviderUri = getString("mediaprovider_uri");
            downloadInfo.mIsPublicApi = getInt("is_public_api").intValue() != 0;
            downloadInfo.mAllowedNetworkTypes = getInt("allowed_network_types").intValue();
            downloadInfo.mAllowRoaming = getInt("allow_roaming").intValue() != 0;
            downloadInfo.mAllowMetered = getInt("allow_metered").intValue() != 0;
            downloadInfo.mFlags = getInt("flags").intValue();
            downloadInfo.mTitle = getString("title");
            downloadInfo.mDescription = getString("description");
            downloadInfo.mBypassRecommendedSizeLimit = getInt("bypass_recommended_size_limit").intValue();
            synchronized (this) {
                downloadInfo.mControl = getInt("control").intValue();
            }
        }

        public void readRequestHeaders(DownloadInfo downloadInfo) {
            downloadInfo.mRequestHeaders.clear();
            Cursor cursorQuery = this.mResolver.query(Uri.withAppendedPath(downloadInfo.getAllDownloadsUri(), "headers"), null, null, null, null);
            try {
                int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("header");
                int columnIndexOrThrow2 = cursorQuery.getColumnIndexOrThrow("value");
                cursorQuery.moveToFirst();
                while (!cursorQuery.isAfterLast()) {
                    addHeader(downloadInfo, cursorQuery.getString(columnIndexOrThrow), cursorQuery.getString(columnIndexOrThrow2));
                    cursorQuery.moveToNext();
                }
                cursorQuery.close();
                if (downloadInfo.mCookies != null) {
                    addHeader(downloadInfo, "Cookie", downloadInfo.mCookies);
                }
                if (downloadInfo.mReferer != null) {
                    addHeader(downloadInfo, "Referer", downloadInfo.mReferer);
                }
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }

        private void addHeader(DownloadInfo downloadInfo, String str, String str2) {
            downloadInfo.mRequestHeaders.add(Pair.create(str, str2));
        }

        private String getString(String str) {
            String string = this.mCursor.getString(this.mCursor.getColumnIndexOrThrow(str));
            if (TextUtils.isEmpty(string)) {
                return null;
            }
            return string;
        }

        private Integer getInt(String str) {
            return Integer.valueOf(this.mCursor.getInt(this.mCursor.getColumnIndexOrThrow(str)));
        }

        private Long getLong(String str) {
            return Long.valueOf(this.mCursor.getLong(this.mCursor.getColumnIndexOrThrow(str)));
        }
    }

    public DownloadInfo(Context context) {
        this.mContext = context;
        this.mSystemFacade = Helpers.getSystemFacade(context);
    }

    public static DownloadInfo queryDownloadInfo(Context context, long j) throws Exception {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursorQuery = contentResolver.query(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, j), null, null, null, null);
        try {
            Reader reader = new Reader(contentResolver, cursorQuery);
            DownloadInfo downloadInfo = new DownloadInfo(context);
            if (cursorQuery.moveToFirst()) {
                reader.updateFromDatabase(downloadInfo);
                reader.readRequestHeaders(downloadInfo);
                return downloadInfo;
            }
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return null;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public Collection<Pair<String, String>> getHeaders() {
        return Collections.unmodifiableList(this.mRequestHeaders);
    }

    public String getUserAgent() {
        if (this.mUserAgent != null) {
            return this.mUserAgent;
        }
        return Constants.DEFAULT_USER_AGENT;
    }

    public void sendIntentIfRequested() {
        Intent intent;
        if (this.mPackage == null) {
            return;
        }
        if (this.mIsPublicApi) {
            intent = new Intent("android.intent.action.DOWNLOAD_COMPLETE");
            intent.setPackage(this.mPackage);
            intent.putExtra("extra_download_id", this.mId);
        } else {
            if (this.mClass == null) {
                return;
            }
            intent = new Intent("android.intent.action.DOWNLOAD_COMPLETED");
            intent.setClassName(this.mPackage, this.mClass);
            if (this.mExtras != null) {
                intent.putExtra("notificationextras", this.mExtras);
            }
            intent.setData(getMyDownloadsUri());
        }
        this.mSystemFacade.sendBroadcast(intent);
    }

    public boolean isVisible() {
        switch (this.mVisibility) {
            case 0:
            case 1:
                return true;
            default:
                return false;
        }
    }

    private long fuzzDelay(long j) {
        return j + ((long) Helpers.sRandom.nextInt((int) (j / 2)));
    }

    public long getMinimumLatency() {
        long jFuzzDelay;
        if (this.mStatus != 194) {
            return 0L;
        }
        long jCurrentTimeMillis = this.mSystemFacade.currentTimeMillis();
        if (this.mNumFailed != 0) {
            if (this.mRetryAfter > 0) {
                jFuzzDelay = this.mLastMod + fuzzDelay(this.mRetryAfter);
            } else {
                jFuzzDelay = fuzzDelay(30000 * ((long) (1 << (this.mNumFailed - 1)))) + this.mLastMod;
            }
        } else {
            jFuzzDelay = jCurrentTimeMillis;
        }
        return Math.max(0L, jFuzzDelay - jCurrentTimeMillis);
    }

    public int getRequiredNetworkType(long j) {
        if (!this.mAllowMetered || this.mAllowedNetworkTypes == 2 || j > this.mSystemFacade.getMaxBytesOverMobile()) {
            return 2;
        }
        if (j > this.mSystemFacade.getRecommendedMaxBytesOverMobile() && this.mBypassRecommendedSizeLimit == 0) {
            return 2;
        }
        if (!this.mAllowRoaming) {
            return 3;
        }
        return 1;
    }

    public boolean isReadyToSchedule() {
        if (this.mControl == 1) {
            return false;
        }
        int i = this.mStatus;
        if (i != 0 && i != 190 && i != 192) {
            if (i != 199) {
                switch (i) {
                }
                return false;
            }
            Uri uri = Uri.parse(this.mUri);
            if ("file".equals(uri.getScheme())) {
                return "mounted".equals(Environment.getExternalStorageState(new File(uri.getPath())));
            }
            Log.w("DownloadManager", "Expected file URI on external storage: " + this.mUri);
            return false;
        }
        return true;
    }

    public boolean isMeteredAllowed(long j) {
        return getRequiredNetworkType(j) != 2;
    }

    public boolean isRoamingAllowed() {
        if (this.mIsPublicApi) {
            return this.mAllowRoaming;
        }
        return this.mDestination != 3;
    }

    public Uri getMyDownloadsUri() {
        return ContentUris.withAppendedId(Downloads.Impl.CONTENT_URI, this.mId);
    }

    public Uri getAllDownloadsUri() {
        return ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, this.mId);
    }

    public String toString() {
        CharArrayWriter charArrayWriter = new CharArrayWriter();
        dump(new IndentingPrintWriter(charArrayWriter, "  "));
        return charArrayWriter.toString();
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("DownloadInfo:");
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.printPair("mId", Long.valueOf(this.mId));
        indentingPrintWriter.printPair("mLastMod", Long.valueOf(this.mLastMod));
        indentingPrintWriter.printPair("mPackage", this.mPackage);
        indentingPrintWriter.printPair("mUid", Integer.valueOf(this.mUid));
        indentingPrintWriter.println();
        indentingPrintWriter.printPair("mUri", this.mUri);
        indentingPrintWriter.println();
        indentingPrintWriter.printPair("mMimeType", this.mMimeType);
        indentingPrintWriter.printPair("mCookies", this.mCookies != null ? "yes" : "no");
        indentingPrintWriter.printPair("mReferer", this.mReferer != null ? "yes" : "no");
        indentingPrintWriter.printPair("mUserAgent", this.mUserAgent);
        indentingPrintWriter.println();
        indentingPrintWriter.printPair("mFileName", this.mFileName);
        indentingPrintWriter.printPair("mDestination", Integer.valueOf(this.mDestination));
        indentingPrintWriter.println();
        indentingPrintWriter.printPair("mStatus", Downloads.Impl.statusToString(this.mStatus));
        indentingPrintWriter.printPair("mCurrentBytes", Long.valueOf(this.mCurrentBytes));
        indentingPrintWriter.printPair("mTotalBytes", Long.valueOf(this.mTotalBytes));
        indentingPrintWriter.println();
        indentingPrintWriter.printPair("mNumFailed", Integer.valueOf(this.mNumFailed));
        indentingPrintWriter.printPair("mRetryAfter", Integer.valueOf(this.mRetryAfter));
        indentingPrintWriter.printPair("mETag", this.mETag);
        indentingPrintWriter.printPair("mIsPublicApi", Boolean.valueOf(this.mIsPublicApi));
        indentingPrintWriter.println();
        indentingPrintWriter.printPair("mAllowedNetworkTypes", Integer.valueOf(this.mAllowedNetworkTypes));
        indentingPrintWriter.printPair("mAllowRoaming", Boolean.valueOf(this.mAllowRoaming));
        indentingPrintWriter.printPair("mAllowMetered", Boolean.valueOf(this.mAllowMetered));
        indentingPrintWriter.printPair("mFlags", Integer.valueOf(this.mFlags));
        indentingPrintWriter.println();
        indentingPrintWriter.decreaseIndent();
    }

    public boolean shouldScanFile(int i) {
        return this.mMediaScanned == 0 && (this.mDestination == 0 || this.mDestination == 4 || this.mDestination == 6) && Downloads.Impl.isStatusSuccess(i);
    }

    public int queryDownloadStatus() {
        return queryDownloadInt("status", 190);
    }

    public int queryDownloadControl() {
        return queryDownloadInt("control", 0);
    }

    public int queryDownloadInt(String str, int i) throws Exception {
        Cursor cursorQuery = this.mContext.getContentResolver().query(getAllDownloadsUri(), new String[]{str}, null, null, null);
        Throwable th = null;
        try {
            try {
                if (cursorQuery.moveToFirst()) {
                    return cursorQuery.getInt(0);
                }
                if (cursorQuery != null) {
                    $closeResource(null, cursorQuery);
                }
                return i;
            } finally {
            }
        } finally {
            if (cursorQuery != null) {
            }
        }
        if (cursorQuery != null) {
            $closeResource(th, cursorQuery);
        }
    }
}
