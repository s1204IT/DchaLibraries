package com.android.settings.notification;

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import com.android.settings.RestrictedSettingsFragment;
import java.util.Map;
import java.util.Set;

public abstract class ZenModeSettingsBase extends RestrictedSettingsFragment {
    protected static final boolean DEBUG = Log.isLoggable("ZenModeSettings", 3);
    protected Context mContext;
    private final Handler mHandler;
    protected Set<Map.Entry<String, AutomaticZenRule>> mRules;
    private final SettingsObserver mSettingsObserver;
    protected int mZenMode;

    protected abstract void onZenModeChanged();

    protected abstract void onZenModeConfigChanged();

    public ZenModeSettingsBase() {
        super("no_adjust_volume");
        this.mHandler = new Handler();
        this.mSettingsObserver = new SettingsObserver(this, null);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mContext = getActivity();
        updateZenMode(false);
        maybeRefreshRules(true, false);
        if (DEBUG) {
            Log.d("ZenModeSettings", "Loaded mRules=" + this.mRules);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateZenMode(true);
        maybeRefreshRules(true, true);
        this.mSettingsObserver.register();
        if (!isUiRestricted()) {
            return;
        }
        if (isUiRestrictedByOnlyAdmin()) {
            getPreferenceScreen().removeAll();
        } else {
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mSettingsObserver.unregister();
    }

    public void updateZenMode(boolean fireChanged) {
        int zenMode = Settings.Global.getInt(getContentResolver(), "zen_mode", this.mZenMode);
        if (zenMode == this.mZenMode) {
            return;
        }
        this.mZenMode = zenMode;
        if (DEBUG) {
            Log.d("ZenModeSettings", "updateZenMode mZenMode=" + this.mZenMode);
        }
        if (!fireChanged) {
            return;
        }
        onZenModeChanged();
    }

    protected String addZenRule(AutomaticZenRule rule) {
        try {
            String id = NotificationManager.from(this.mContext).addAutomaticZenRule(rule);
            AutomaticZenRule savedRule = NotificationManager.from(this.mContext).getAutomaticZenRule(id);
            maybeRefreshRules(savedRule != null, true);
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    protected boolean setZenRule(String id, AutomaticZenRule rule) {
        boolean success = NotificationManager.from(this.mContext).updateAutomaticZenRule(id, rule);
        maybeRefreshRules(success, true);
        return success;
    }

    protected boolean removeZenRule(String id) {
        boolean success = NotificationManager.from(this.mContext).removeAutomaticZenRule(id);
        maybeRefreshRules(success, true);
        return success;
    }

    protected void maybeRefreshRules(boolean success, boolean fireChanged) {
        if (!success) {
            return;
        }
        this.mRules = getZenModeRules();
        if (DEBUG) {
            Log.d("ZenModeSettings", "Refreshed mRules=" + this.mRules);
        }
        if (!fireChanged) {
            return;
        }
        onZenModeConfigChanged();
    }

    private Set<Map.Entry<String, AutomaticZenRule>> getZenModeRules() {
        Map<String, AutomaticZenRule> ruleMap = NotificationManager.from(this.mContext).getAutomaticZenRules();
        return ruleMap.entrySet();
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri ZEN_MODE_CONFIG_ETAG_URI;
        private final Uri ZEN_MODE_URI;

        SettingsObserver(ZenModeSettingsBase this$0, SettingsObserver settingsObserver) {
            this();
        }

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

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (this.ZEN_MODE_URI.equals(uri)) {
                ZenModeSettingsBase.this.updateZenMode(true);
            }
            if (!this.ZEN_MODE_CONFIG_ETAG_URI.equals(uri)) {
                return;
            }
            ZenModeSettingsBase.this.maybeRefreshRules(true, true);
        }
    }
}
