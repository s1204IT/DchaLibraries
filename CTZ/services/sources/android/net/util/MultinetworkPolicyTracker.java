package android.net.util;

import android.R;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MultinetworkPolicyTracker {
    private static String TAG = MultinetworkPolicyTracker.class.getSimpleName();
    private volatile boolean mAvoidBadWifi;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private final Handler mHandler;
    private volatile int mMeteredMultipathPreference;
    private final Runnable mReevaluateRunnable;
    private final ContentResolver mResolver;
    private final SettingObserver mSettingObserver;
    private final List<Uri> mSettingsUris;

    public MultinetworkPolicyTracker(Context context, Handler handler) {
        this(context, handler, null);
    }

    public MultinetworkPolicyTracker(Context context, Handler handler, final Runnable runnable) {
        this.mAvoidBadWifi = true;
        this.mContext = context;
        this.mHandler = handler;
        this.mReevaluateRunnable = new Runnable() {
            @Override
            public final void run() {
                MultinetworkPolicyTracker.lambda$new$0(this.f$0, runnable);
            }
        };
        this.mSettingsUris = Arrays.asList(Settings.Global.getUriFor("network_avoid_bad_wifi"), Settings.Global.getUriFor("network_metered_multipath_preference"));
        this.mResolver = this.mContext.getContentResolver();
        this.mSettingObserver = new SettingObserver();
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                MultinetworkPolicyTracker.this.reevaluate();
            }
        };
        updateAvoidBadWifi();
        updateMeteredMultipathPreference();
    }

    public static void lambda$new$0(MultinetworkPolicyTracker multinetworkPolicyTracker, Runnable runnable) {
        if (multinetworkPolicyTracker.updateAvoidBadWifi() && runnable != null) {
            runnable.run();
        }
        multinetworkPolicyTracker.updateMeteredMultipathPreference();
    }

    public void start() {
        Iterator<Uri> it = this.mSettingsUris.iterator();
        while (it.hasNext()) {
            this.mResolver.registerContentObserver(it.next(), false, this.mSettingObserver);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, null);
        reevaluate();
    }

    public void shutdown() {
        this.mResolver.unregisterContentObserver(this.mSettingObserver);
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }

    public boolean getAvoidBadWifi() {
        return this.mAvoidBadWifi;
    }

    public int getMeteredMultipathPreference() {
        return this.mMeteredMultipathPreference;
    }

    public boolean configRestrictsAvoidBadWifi() {
        return this.mContext.getResources().getInteger(R.integer.config_defaultRefreshRateInZone) == 0;
    }

    public boolean shouldNotifyWifiUnvalidated() {
        return configRestrictsAvoidBadWifi() && getAvoidBadWifiSetting() == null;
    }

    public String getAvoidBadWifiSetting() {
        return Settings.Global.getString(this.mResolver, "network_avoid_bad_wifi");
    }

    @VisibleForTesting
    public void reevaluate() {
        this.mHandler.post(this.mReevaluateRunnable);
    }

    public boolean updateAvoidBadWifi() {
        boolean zEquals = "1".equals(getAvoidBadWifiSetting());
        boolean z = this.mAvoidBadWifi;
        this.mAvoidBadWifi = zEquals || !configRestrictsAvoidBadWifi();
        return this.mAvoidBadWifi != z;
    }

    public int configMeteredMultipathPreference() {
        return this.mContext.getResources().getInteger(R.integer.config_defaultUiModeType);
    }

    public void updateMeteredMultipathPreference() {
        try {
            this.mMeteredMultipathPreference = Integer.parseInt(Settings.Global.getString(this.mResolver, "network_metered_multipath_preference"));
        } catch (NumberFormatException e) {
            this.mMeteredMultipathPreference = configMeteredMultipathPreference();
        }
    }

    private class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean z) {
            Slog.wtf(MultinetworkPolicyTracker.TAG, "Should never be reached.");
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            if (!MultinetworkPolicyTracker.this.mSettingsUris.contains(uri)) {
                Slog.wtf(MultinetworkPolicyTracker.TAG, "Unexpected settings observation: " + uri);
            }
            MultinetworkPolicyTracker.this.reevaluate();
        }
    }
}
