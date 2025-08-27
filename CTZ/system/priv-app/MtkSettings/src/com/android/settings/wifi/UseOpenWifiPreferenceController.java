package com.android.settings.wifi;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import java.util.Iterator;

/* loaded from: classes.dex */
public class UseOpenWifiPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private final ContentResolver mContentResolver;
    private boolean mDoFeatureSupportedScorersExist;
    private ComponentName mEnableUseWifiComponentName;
    private final Fragment mFragment;
    private final NetworkScoreManager mNetworkScoreManager;
    private Preference mPreference;
    private final SettingObserver mSettingObserver;

    public UseOpenWifiPreferenceController(Context context, Fragment fragment, Lifecycle lifecycle) {
        super(context);
        this.mContentResolver = context.getContentResolver();
        this.mFragment = fragment;
        this.mNetworkScoreManager = (NetworkScoreManager) context.getSystemService("network_score");
        this.mSettingObserver = new SettingObserver();
        updateEnableUseWifiComponentName();
        checkForFeatureSupportedScorers();
        lifecycle.addObserver(this);
    }

    private void updateEnableUseWifiComponentName() {
        NetworkScorerAppData activeScorer = this.mNetworkScoreManager.getActiveScorer();
        this.mEnableUseWifiComponentName = activeScorer == null ? null : activeScorer.getEnableUseOpenWifiActivity();
    }

    private void checkForFeatureSupportedScorers() {
        if (this.mEnableUseWifiComponentName != null) {
            this.mDoFeatureSupportedScorersExist = true;
            return;
        }
        Iterator it = this.mNetworkScoreManager.getAllValidScorers().iterator();
        while (it.hasNext()) {
            if (((NetworkScorerAppData) it.next()).getEnableUseOpenWifiActivity() != null) {
                this.mDoFeatureSupportedScorersExist = true;
                return;
            }
        }
        this.mDoFeatureSupportedScorersExist = false;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference("use_open_wifi_automatically");
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        this.mSettingObserver.register(this.mContentResolver);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        this.mSettingObserver.unregister(this.mContentResolver);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mDoFeatureSupportedScorersExist;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "use_open_wifi_automatically";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (!(preference instanceof SwitchPreference)) {
            return;
        }
        SwitchPreference switchPreference = (SwitchPreference) preference;
        boolean z = false;
        boolean z2 = this.mNetworkScoreManager.getActiveScorerPackage() != null;
        boolean z3 = this.mEnableUseWifiComponentName != null;
        switchPreference.setChecked(isSettingEnabled());
        switchPreference.setVisible(isAvailable());
        if (z2 && z3) {
            z = true;
        }
        switchPreference.setEnabled(z);
        if (!z2) {
            switchPreference.setSummary(R.string.use_open_wifi_automatically_summary_scoring_disabled);
        } else if (!z3) {
            switchPreference.setSummary(R.string.use_open_wifi_automatically_summary_scorer_unsupported_disabled);
        } else {
            switchPreference.setSummary(R.string.use_open_wifi_automatically_summary);
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (!TextUtils.equals(preference.getKey(), "use_open_wifi_automatically") || !isAvailable()) {
            return false;
        }
        if (isSettingEnabled()) {
            Settings.Global.putString(this.mContentResolver, "use_open_wifi_package", "");
            return true;
        }
        Intent intent = new Intent("android.net.scoring.CUSTOM_ENABLE");
        intent.setComponent(this.mEnableUseWifiComponentName);
        this.mFragment.startActivityForResult(intent, 400);
        return false;
    }

    private boolean isSettingEnabled() {
        return TextUtils.equals(Settings.Global.getString(this.mContentResolver, "use_open_wifi_package"), this.mEnableUseWifiComponentName == null ? null : this.mEnableUseWifiComponentName.getPackageName());
    }

    public boolean onActivityResult(int i, int i2) {
        if (i != 400) {
            return false;
        }
        if (i2 == -1) {
            Settings.Global.putString(this.mContentResolver, "use_open_wifi_package", this.mEnableUseWifiComponentName.getPackageName());
            return true;
        }
        return true;
    }

    class SettingObserver extends ContentObserver {
        private final Uri NETWORK_RECOMMENDATIONS_ENABLED_URI;

        public SettingObserver() {
            super(new Handler(Looper.getMainLooper()));
            this.NETWORK_RECOMMENDATIONS_ENABLED_URI = Settings.Global.getUriFor("network_recommendations_enabled");
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(this.NETWORK_RECOMMENDATIONS_ENABLED_URI, false, this);
            onChange(true, this.NETWORK_RECOMMENDATIONS_ENABLED_URI);
        }

        public void unregister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            if (this.NETWORK_RECOMMENDATIONS_ENABLED_URI.equals(uri)) {
                UseOpenWifiPreferenceController.this.updateEnableUseWifiComponentName();
                UseOpenWifiPreferenceController.this.updateState(UseOpenWifiPreferenceController.this.mPreference);
            }
        }
    }
}
