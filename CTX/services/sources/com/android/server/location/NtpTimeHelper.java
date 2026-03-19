package com.android.server.location;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.util.NtpTrustedTime;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Date;

class NtpTimeHelper {
    private static final boolean DEBUG = Log.isLoggable("NtpTimeHelper", 3);
    private static final long MAX_RETRY_INTERVAL = 14400000;

    @VisibleForTesting
    static final long NTP_INTERVAL = 86400000;

    @VisibleForTesting
    static final long RETRY_INTERVAL = 300000;
    private static final int STATE_IDLE = 2;
    private static final int STATE_PENDING_NETWORK = 0;
    private static final int STATE_RETRIEVING_AND_INJECTING = 1;
    private static final String TAG = "NtpTimeHelper";
    private static final String WAKELOCK_KEY = "NtpTimeHelper";
    private static final long WAKELOCK_TIMEOUT_MILLIS = 60000;

    @GuardedBy("this")
    private final InjectNtpTimeCallback mCallback;
    private final ConnectivityManager mConnMgr;
    private final Handler mHandler;

    @GuardedBy("this")
    private int mInjectNtpTimeState;
    private final ExponentialBackOff mNtpBackOff;
    private final NtpTrustedTime mNtpTime;

    @GuardedBy("this")
    private boolean mOnDemandTimeInjection;
    private final PowerManager.WakeLock mWakeLock;

    interface InjectNtpTimeCallback {
        void injectTime(long j, long j2, int i);
    }

    @VisibleForTesting
    NtpTimeHelper(Context context, Looper looper, InjectNtpTimeCallback injectNtpTimeCallback, NtpTrustedTime ntpTrustedTime) {
        this.mNtpBackOff = new ExponentialBackOff(300000L, 14400000L);
        this.mInjectNtpTimeState = 0;
        this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
        this.mCallback = injectNtpTimeCallback;
        this.mNtpTime = ntpTrustedTime;
        this.mHandler = new Handler(looper);
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "NtpTimeHelper");
    }

    NtpTimeHelper(Context context, Looper looper, InjectNtpTimeCallback injectNtpTimeCallback) {
        this(context, looper, injectNtpTimeCallback, NtpTrustedTime.getInstance(context));
    }

    synchronized void enablePeriodicTimeInjection() {
        this.mOnDemandTimeInjection = true;
    }

    synchronized void onNetworkAvailable() {
        if (this.mInjectNtpTimeState == 0) {
            retrieveAndInjectNtpTime();
        }
    }

    private boolean isNetworkConnected() {
        NetworkInfo activeNetworkInfo = this.mConnMgr.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    synchronized void retrieveAndInjectNtpTime() {
        if (this.mInjectNtpTimeState == 1) {
            return;
        }
        if (!isNetworkConnected()) {
            this.mInjectNtpTimeState = 0;
            return;
        }
        this.mInjectNtpTimeState = 1;
        this.mWakeLock.acquire(60000L);
        new Thread(new Runnable() {
            @Override
            public final void run() {
                this.f$0.blockingGetNtpTimeAndInject();
            }
        }).start();
    }

    private void blockingGetNtpTimeAndInject() {
        boolean zForceRefresh;
        long jNextBackoffMillis;
        if (this.mNtpTime.getCacheAge() >= 86400000) {
            zForceRefresh = this.mNtpTime.forceRefresh();
        } else {
            zForceRefresh = true;
        }
        synchronized (this) {
            this.mInjectNtpTimeState = 2;
            if (this.mNtpTime.getCacheAge() < 86400000) {
                final long cachedNtpTime = this.mNtpTime.getCachedNtpTime();
                final long cachedNtpTimeReference = this.mNtpTime.getCachedNtpTimeReference();
                final long cacheCertainty = this.mNtpTime.getCacheCertainty();
                if (DEBUG) {
                    Log.d("NtpTimeHelper", "NTP server returned: " + cachedNtpTime + " (" + new Date(cachedNtpTime) + ") reference: " + cachedNtpTimeReference + " certainty: " + cacheCertainty + " system time offset: " + (cachedNtpTime - System.currentTimeMillis()));
                }
                this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mCallback.injectTime(cachedNtpTime, cachedNtpTimeReference, (int) cacheCertainty);
                    }
                });
                this.mNtpBackOff.reset();
                jNextBackoffMillis = 86400000;
            } else {
                Log.e("NtpTimeHelper", "requestTime failed");
                jNextBackoffMillis = this.mNtpBackOff.nextBackoffMillis();
            }
            if (DEBUG) {
                Log.d("NtpTimeHelper", String.format("onDemandTimeInjection=%s, refreshSuccess=%s, delay=%s", Boolean.valueOf(this.mOnDemandTimeInjection), Boolean.valueOf(zForceRefresh), Long.valueOf(jNextBackoffMillis)));
            }
            if (this.mOnDemandTimeInjection || !zForceRefresh) {
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.retrieveAndInjectNtpTime();
                    }
                }, jNextBackoffMillis);
            }
        }
        try {
            this.mWakeLock.release();
        } catch (Exception e) {
        }
    }

    public void setNtpTimeStateIdle() {
        this.mInjectNtpTimeState = 2;
    }
}
