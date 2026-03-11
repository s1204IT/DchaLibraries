package com.android.settings.network;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class PrivateDnsPreferenceController extends BasePreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {
    private static final String KEY_PRIVATE_DNS_SETTINGS = "private_dns_settings";
    private static final Uri[] SETTINGS_URIS = {Settings.Global.getUriFor("private_dns_mode"), Settings.Global.getUriFor("private_dns_default_mode"), Settings.Global.getUriFor("private_dns_specifier")};
    private final ConnectivityManager mConnectivityManager;
    private final Handler mHandler;
    private LinkProperties mLatestLinkProperties;
    private final ConnectivityManager.NetworkCallback mNetworkCallback;
    private Preference mPreference;
    private final ContentObserver mSettingsObserver;

    public PrivateDnsPreferenceController(Context context) {
        super(context, KEY_PRIVATE_DNS_SETTINGS);
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                PrivateDnsPreferenceController.this.mLatestLinkProperties = linkProperties;
                if (PrivateDnsPreferenceController.this.mPreference != null) {
                    PrivateDnsPreferenceController.this.updateState(PrivateDnsPreferenceController.this.mPreference);
                }
            }

            @Override
            public void onLost(Network network) {
                PrivateDnsPreferenceController.this.mLatestLinkProperties = null;
                if (PrivateDnsPreferenceController.this.mPreference != null) {
                    PrivateDnsPreferenceController.this.updateState(PrivateDnsPreferenceController.this.mPreference);
                }
            }
        };
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mSettingsObserver = new PrivateDnsSettingsObserver(this.mHandler);
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService(ConnectivityManager.class);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PRIVATE_DNS_SETTINGS;
    }

    @Override
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        for (Uri uri : SETTINGS_URIS) {
            this.mContext.getContentResolver().registerContentObserver(uri, false, this.mSettingsObserver);
        }
        Network activeNetwork = this.mConnectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            this.mLatestLinkProperties = this.mConnectivityManager.getLinkProperties(activeNetwork);
        }
        this.mConnectivityManager.registerDefaultNetworkCallback(this.mNetworkCallback, this.mHandler);
    }

    @Override
    public void onStop() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
        this.mConnectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
    }

    @Override
    public CharSequence getSummary() {
        Resources resources = this.mContext.getResources();
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String modeFromSettings = PrivateDnsModeDialogPreference.getModeFromSettings(contentResolver);
        LinkProperties linkProperties = this.mLatestLinkProperties;
        byte b = 1;
        boolean z = !ArrayUtils.isEmpty(linkProperties == null ? null : linkProperties.getValidatedPrivateDnsServers());
        int iHashCode = modeFromSettings.hashCode();
        if (iHashCode != -539229175) {
            if (iHashCode != -299803597) {
                b = (iHashCode == 109935 && modeFromSettings.equals("off")) ? (byte) 0 : (byte) -1;
            } else if (modeFromSettings.equals("hostname")) {
                b = 2;
            }
        } else if (!modeFromSettings.equals("opportunistic")) {
        }
        switch (b) {
            case 0:
                return resources.getString(R.string.private_dns_mode_off);
            case 1:
                return z ? resources.getString(R.string.switch_on_text) : resources.getString(R.string.private_dns_mode_opportunistic);
            case 2:
                if (z) {
                    return PrivateDnsModeDialogPreference.getHostnameFromSettings(contentResolver);
                }
                return resources.getString(R.string.private_dns_mode_provider_failure);
            default:
                return "";
        }
    }

    private class PrivateDnsSettingsObserver extends ContentObserver {
        public PrivateDnsSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z) {
            if (PrivateDnsPreferenceController.this.mPreference != null) {
                PrivateDnsPreferenceController.this.updateState(PrivateDnsPreferenceController.this.mPreference);
            }
        }
    }
}
