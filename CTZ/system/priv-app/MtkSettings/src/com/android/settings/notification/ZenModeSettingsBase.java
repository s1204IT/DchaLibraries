package com.android.settings.notification;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import com.android.settings.dashboard.RestrictedDashboardFragment;

/* loaded from: classes.dex */
public abstract class ZenModeSettingsBase extends RestrictedDashboardFragment {
    protected static final boolean DEBUG = Log.isLoggable("ZenModeSettings", 3);
    protected ZenModeBackend mBackend;
    protected Context mContext;
    private final Handler mHandler;
    private final SettingsObserver mSettingsObserver;
    protected int mZenMode;

    protected void onZenModeConfigChanged() {
    }

    public ZenModeSettingsBase() {
        super("no_adjust_volume");
        this.mHandler = new Handler();
        this.mSettingsObserver = new SettingsObserver();
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "ZenModeSettings";
    }

    @Override // com.android.settings.dashboard.RestrictedDashboardFragment, com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        this.mContext = getActivity();
        this.mBackend = ZenModeBackend.getInstance(this.mContext);
        super.onCreate(bundle);
        updateZenMode(false);
    }

    @Override // com.android.settings.dashboard.RestrictedDashboardFragment, com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        updateZenMode(true);
        this.mSettingsObserver.register();
        if (isUiRestricted()) {
            if (isUiRestrictedByOnlyAdmin()) {
                getPreferenceScreen().removeAll();
            } else {
                finish();
            }
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mSettingsObserver.unregister();
    }

    private void updateZenMode(boolean z) {
        int i = Settings.Global.getInt(getContentResolver(), "zen_mode", this.mZenMode);
        if (i == this.mZenMode) {
            return;
        }
        this.mZenMode = i;
        if (DEBUG) {
            Log.d("ZenModeSettings", "updateZenMode mZenMode=" + this.mZenMode + " " + z);
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri ZEN_MODE_CONFIG_ETAG_URI;
        private final Uri ZEN_MODE_URI;

        private SettingsObserver() {
            super(ZenModeSettingsBase.this.mHandler);
            this.ZEN_MODE_URI = Settings.Global.getUriFor("zen_mode");
            this.ZEN_MODE_CONFIG_ETAG_URI = Settings.Global.getUriFor("zen_mode_config_etag");
        }

        public void register() {
            ZenModeSettingsBase.this.getContentResolver().registerContentObserver(this.ZEN_MODE_URI, false, this);
            ZenModeSettingsBase.this.getContentResolver().registerContentObserver(this.ZEN_MODE_CONFIG_ETAG_URI, false, this);
        }

        public void unregister() {
            ZenModeSettingsBase.this.getContentResolver().unregisterContentObserver(this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            if (this.ZEN_MODE_URI.equals(uri)) {
                ZenModeSettingsBase.this.updateZenMode(true);
            }
            if (this.ZEN_MODE_CONFIG_ETAG_URI.equals(uri)) {
                ZenModeSettingsBase.this.mBackend.updatePolicy();
                ZenModeSettingsBase.this.onZenModeConfigChanged();
            }
        }
    }
}
