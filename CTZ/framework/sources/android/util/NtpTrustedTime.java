package android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.SntpClient;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.internal.R;

public class NtpTrustedTime implements TrustedTime {
    private static final boolean LOGD = false;
    private static final String TAG = "NtpTrustedTime";
    private static Context sContext;
    private static NtpTrustedTime sSingleton;
    private ConnectivityManager mCM;
    private long mCachedNtpCertainty;
    private long mCachedNtpElapsedRealtime;
    private long mCachedNtpTime;
    private boolean mHasCache;
    private String mServer;
    private final long mTimeout;

    private NtpTrustedTime(String str, long j) {
        this.mServer = str;
        this.mTimeout = j;
    }

    public static synchronized NtpTrustedTime getInstance(Context context) {
        if (sSingleton == null) {
            Resources resources = context.getResources();
            ContentResolver contentResolver = context.getContentResolver();
            String string = resources.getString(R.string.config_ntpServer);
            long integer = resources.getInteger(R.integer.config_ntpTimeout);
            String string2 = Settings.Global.getString(contentResolver, Settings.Global.NTP_SERVER);
            long j = Settings.Global.getLong(contentResolver, Settings.Global.NTP_TIMEOUT, integer);
            if (string2 == null) {
                string2 = string;
            }
            sSingleton = new NtpTrustedTime(string2, j);
            sContext = context;
        }
        return sSingleton;
    }

    @Override
    public boolean forceRefresh() {
        synchronized (this) {
            if (this.mCM == null) {
                this.mCM = (ConnectivityManager) sContext.getSystemService(ConnectivityManager.class);
            }
        }
        return forceRefresh(this.mCM == null ? null : this.mCM.getActiveNetwork());
    }

    public boolean forceRefresh(Network network) {
        if (TextUtils.isEmpty(this.mServer)) {
            return false;
        }
        synchronized (this) {
            if (this.mCM == null) {
                this.mCM = (ConnectivityManager) sContext.getSystemService(ConnectivityManager.class);
            }
        }
        NetworkInfo networkInfo = this.mCM == null ? null : this.mCM.getNetworkInfo(network);
        if (networkInfo == null || !networkInfo.isConnected()) {
            return false;
        }
        SntpClient sntpClient = new SntpClient();
        if (!sntpClient.requestTime(this.mServer, (int) this.mTimeout, network)) {
            return false;
        }
        this.mHasCache = true;
        this.mCachedNtpTime = sntpClient.getNtpTime();
        this.mCachedNtpElapsedRealtime = sntpClient.getNtpTimeReference();
        this.mCachedNtpCertainty = sntpClient.getRoundTripTime() / 2;
        return true;
    }

    @Override
    public boolean hasCache() {
        return this.mHasCache;
    }

    @Override
    public long getCacheAge() {
        if (this.mHasCache) {
            return SystemClock.elapsedRealtime() - this.mCachedNtpElapsedRealtime;
        }
        return Long.MAX_VALUE;
    }

    @Override
    public long getCacheCertainty() {
        if (this.mHasCache) {
            return this.mCachedNtpCertainty;
        }
        return Long.MAX_VALUE;
    }

    @Override
    public long currentTimeMillis() {
        if (!this.mHasCache) {
            throw new IllegalStateException("Missing authoritative time source");
        }
        return this.mCachedNtpTime + getCacheAge();
    }

    public long getCachedNtpTime() {
        return this.mCachedNtpTime;
    }

    public long getCachedNtpTimeReference() {
        return this.mCachedNtpElapsedRealtime;
    }

    public void setServer(String str) {
        Log.d(TAG, "setServer:[" + str + "]");
        if (str != null) {
            this.mServer = str;
        }
    }

    public String getServer() {
        Log.d(TAG, "getServer:[" + this.mServer + "]");
        return this.mServer;
    }
}
