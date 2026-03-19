package com.android.server.content;

import android.content.Context;
import android.database.ContentObserver;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import java.io.PrintWriter;

public class SyncManagerConstants extends ContentObserver {
    private static final int DEF_EXEMPTION_TEMP_WHITELIST_DURATION_IN_SECONDS = 600;
    private static final int DEF_INITIAL_SYNC_RETRY_TIME_IN_SECONDS = 30;
    private static final int DEF_MAX_RETRIES_WITH_APP_STANDBY_EXEMPTION = 5;
    private static final int DEF_MAX_SYNC_RETRY_TIME_IN_SECONDS = 3600;
    private static final float DEF_RETRY_TIME_INCREASE_FACTOR = 2.0f;
    private static final String KEY_EXEMPTION_TEMP_WHITELIST_DURATION_IN_SECONDS = "exemption_temp_whitelist_duration_in_seconds";
    private static final String KEY_INITIAL_SYNC_RETRY_TIME_IN_SECONDS = "initial_sync_retry_time_in_seconds";
    private static final String KEY_MAX_RETRIES_WITH_APP_STANDBY_EXEMPTION = "max_retries_with_app_standby_exemption";
    private static final String KEY_MAX_SYNC_RETRY_TIME_IN_SECONDS = "max_sync_retry_time_in_seconds";
    private static final String KEY_RETRY_TIME_INCREASE_FACTOR = "retry_time_increase_factor";
    private static final String TAG = "SyncManagerConfig";
    private final Context mContext;
    private int mInitialSyncRetryTimeInSeconds;
    private int mKeyExemptionTempWhitelistDurationInSeconds;
    private final Object mLock;
    private int mMaxRetriesWithAppStandbyExemption;
    private int mMaxSyncRetryTimeInSeconds;
    private float mRetryTimeIncreaseFactor;

    protected SyncManagerConstants(Context context) {
        super(null);
        this.mLock = new Object();
        this.mInitialSyncRetryTimeInSeconds = 30;
        this.mRetryTimeIncreaseFactor = DEF_RETRY_TIME_INCREASE_FACTOR;
        this.mMaxSyncRetryTimeInSeconds = DEF_MAX_SYNC_RETRY_TIME_IN_SECONDS;
        this.mMaxRetriesWithAppStandbyExemption = 5;
        this.mKeyExemptionTempWhitelistDurationInSeconds = 600;
        this.mContext = context;
    }

    public void start() {
        BackgroundThread.getHandler().post(new Runnable() {
            @Override
            public final void run() {
                SyncManagerConstants.lambda$start$0(this.f$0);
            }
        });
    }

    public static void lambda$start$0(SyncManagerConstants syncManagerConstants) {
        syncManagerConstants.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("sync_manager_constants"), false, syncManagerConstants);
        syncManagerConstants.refresh();
    }

    @Override
    public void onChange(boolean z) {
        refresh();
    }

    private void refresh() {
        synchronized (this.mLock) {
            String string = Settings.Global.getString(this.mContext.getContentResolver(), "sync_manager_constants");
            KeyValueListParser keyValueListParser = new KeyValueListParser(',');
            try {
                keyValueListParser.setString(string);
            } catch (IllegalArgumentException e) {
                Slog.wtf(TAG, "Bad constants: " + string);
            }
            this.mInitialSyncRetryTimeInSeconds = keyValueListParser.getInt(KEY_INITIAL_SYNC_RETRY_TIME_IN_SECONDS, 30);
            this.mMaxSyncRetryTimeInSeconds = keyValueListParser.getInt(KEY_MAX_SYNC_RETRY_TIME_IN_SECONDS, DEF_MAX_SYNC_RETRY_TIME_IN_SECONDS);
            this.mRetryTimeIncreaseFactor = keyValueListParser.getFloat(KEY_RETRY_TIME_INCREASE_FACTOR, DEF_RETRY_TIME_INCREASE_FACTOR);
            this.mMaxRetriesWithAppStandbyExemption = keyValueListParser.getInt(KEY_MAX_RETRIES_WITH_APP_STANDBY_EXEMPTION, 5);
            this.mKeyExemptionTempWhitelistDurationInSeconds = keyValueListParser.getInt(KEY_EXEMPTION_TEMP_WHITELIST_DURATION_IN_SECONDS, 600);
        }
    }

    public int getInitialSyncRetryTimeInSeconds() {
        int i;
        synchronized (this.mLock) {
            i = this.mInitialSyncRetryTimeInSeconds;
        }
        return i;
    }

    public float getRetryTimeIncreaseFactor() {
        float f;
        synchronized (this.mLock) {
            f = this.mRetryTimeIncreaseFactor;
        }
        return f;
    }

    public int getMaxSyncRetryTimeInSeconds() {
        int i;
        synchronized (this.mLock) {
            i = this.mMaxSyncRetryTimeInSeconds;
        }
        return i;
    }

    public int getMaxRetriesWithAppStandbyExemption() {
        int i;
        synchronized (this.mLock) {
            i = this.mMaxRetriesWithAppStandbyExemption;
        }
        return i;
    }

    public int getKeyExemptionTempWhitelistDurationInSeconds() {
        int i;
        synchronized (this.mLock) {
            i = this.mKeyExemptionTempWhitelistDurationInSeconds;
        }
        return i;
    }

    public void dump(PrintWriter printWriter, String str) {
        synchronized (this.mLock) {
            printWriter.print(str);
            printWriter.println("SyncManager Config:");
            printWriter.print(str);
            printWriter.print("  mInitialSyncRetryTimeInSeconds=");
            printWriter.println(this.mInitialSyncRetryTimeInSeconds);
            printWriter.print(str);
            printWriter.print("  mRetryTimeIncreaseFactor=");
            printWriter.println(this.mRetryTimeIncreaseFactor);
            printWriter.print(str);
            printWriter.print("  mMaxSyncRetryTimeInSeconds=");
            printWriter.println(this.mMaxSyncRetryTimeInSeconds);
            printWriter.print(str);
            printWriter.print("  mMaxRetriesWithAppStandbyExemption=");
            printWriter.println(this.mMaxRetriesWithAppStandbyExemption);
            printWriter.print(str);
            printWriter.print("  mKeyExemptionTempWhitelistDurationInSeconds=");
            printWriter.println(this.mKeyExemptionTempWhitelistDurationInSeconds);
        }
    }
}
