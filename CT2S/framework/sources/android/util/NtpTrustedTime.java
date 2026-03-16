package android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.SntpClient;
import android.os.SystemClock;
import android.provider.Settings;
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
    private final String mServer;
    private final long mTimeout;

    private NtpTrustedTime(String server, long timeout) {
        this.mServer = server;
        this.mTimeout = timeout;
    }

    public static synchronized NtpTrustedTime getInstance(Context context) {
        if (sSingleton == null) {
            Resources res = context.getResources();
            ContentResolver resolver = context.getContentResolver();
            String defaultServer = res.getString(R.string.config_ntpServer);
            long defaultTimeout = res.getInteger(R.integer.config_ntpTimeout);
            String secureServer = Settings.Global.getString(resolver, Settings.Global.NTP_SERVER);
            long timeout = Settings.Global.getLong(resolver, Settings.Global.NTP_TIMEOUT, defaultTimeout);
            String server = secureServer != null ? secureServer : defaultServer;
            sSingleton = new NtpTrustedTime(server, timeout);
            sContext = context;
        }
        return sSingleton;
    }

    @Override
    public boolean forceRefresh() {
        if (this.mServer == null) {
            return false;
        }
        synchronized (this) {
            if (this.mCM == null) {
                this.mCM = (ConnectivityManager) sContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            }
        }
        NetworkInfo ni = this.mCM == null ? null : this.mCM.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            return false;
        }
        SntpClient client = new SntpClient();
        if (!client.requestTime(this.mServer, (int) this.mTimeout)) {
            return false;
        }
        this.mHasCache = true;
        this.mCachedNtpTime = client.getNtpTime();
        this.mCachedNtpElapsedRealtime = client.getNtpTimeReference();
        this.mCachedNtpCertainty = client.getRoundTripTime() / 2;
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
}
