package com.android.settings.wifi;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.icu.text.Collator;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.Indexable;
import com.android.settings.wifi.WifiDialog;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiSavedConfigUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SavedAccessPointsWifiSettings extends SettingsPreferenceFragment implements Indexable, WifiDialog.WifiDialogListener {
    static final int MSG_UPDATE_PREFERENCES = 1;
    private static final Comparator<AccessPoint> SAVED_NETWORK_COMPARATOR = new Comparator<AccessPoint>() {
        final Collator mCollator = Collator.getInstance();

        @Override
        public int compare(AccessPoint accessPoint, AccessPoint accessPoint2) {
            return this.mCollator.compare(nullToEmpty(accessPoint.getConfigName()), nullToEmpty(accessPoint2.getConfigName()));
        }

        private String nullToEmpty(String str) {
            return str == null ? "" : str;
        }
    };
    private Bundle mAccessPointSavedState;
    private Preference mAddNetworkPreference;
    private WifiDialog mDialog;
    private AccessPoint mDlgAccessPoint;
    final WifiManager.ActionListener mForgetListener = new WifiManager.ActionListener() {
        public void onSuccess() {
            SavedAccessPointsWifiSettings.this.postUpdatePreference();
        }

        public void onFailure(int i) {
            SavedAccessPointsWifiSettings.this.postUpdatePreference();
        }
    };
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                SavedAccessPointsWifiSettings.this.initPreferences();
            }
        }
    };
    private final WifiManager.ActionListener mSaveListener = new WifiManager.ActionListener() {
        public void onSuccess() {
            SavedAccessPointsWifiSettings.this.postUpdatePreference();
        }

        public void onFailure(int i) {
            Activity activity = SavedAccessPointsWifiSettings.this.getActivity();
            if (activity != null) {
                Toast.makeText(activity, R.string.wifi_failed_save_message, 0).show();
            }
        }
    };
    private AccessPoint mSelectedAccessPoint;
    private AccessPointPreference.UserBadgeCache mUserBadgeCache;
    private WifiManager mWifiManager;

    @Override
    public int getMetricsCategory() {
        return 106;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.wifi_display_saved_access_points);
        this.mUserBadgeCache = new AccessPointPreference.UserBadgeCache(getPackageManager());
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        if (bundle != null && bundle.containsKey("wifi_ap_state")) {
            this.mAccessPointSavedState = bundle.getBundle("wifi_ap_state");
        }
    }

    public void initPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Context prefContext = getPrefContext();
        List<AccessPoint> allConfigs = WifiSavedConfigUtils.getAllConfigs(prefContext, this.mWifiManager);
        Collections.sort(allConfigs, SAVED_NETWORK_COMPARATOR);
        cacheRemoveAllPrefs(preferenceScreen);
        int size = allConfigs.size();
        for (int i = 0; i < size; i++) {
            AccessPoint accessPoint = allConfigs.get(i);
            String key = accessPoint.getKey();
            LongPressAccessPointPreference longPressAccessPointPreference = (LongPressAccessPointPreference) getCachedPreference(key);
            if (longPressAccessPointPreference == null) {
                LongPressAccessPointPreference longPressAccessPointPreference2 = new LongPressAccessPointPreference(accessPoint, prefContext, this.mUserBadgeCache, true, this);
                longPressAccessPointPreference2.setKey(key);
                longPressAccessPointPreference2.setIcon((Drawable) null);
                preferenceScreen.addPreference(longPressAccessPointPreference2);
                longPressAccessPointPreference = longPressAccessPointPreference2;
            }
            longPressAccessPointPreference.setOrder(i);
        }
        removeCachedPrefs(preferenceScreen);
        if (this.mAddNetworkPreference == null) {
            this.mAddNetworkPreference = new Preference(getPrefContext());
            this.mAddNetworkPreference.setIcon(R.drawable.ic_menu_add_inset);
            this.mAddNetworkPreference.setTitle(R.string.wifi_add_network);
        }
        this.mAddNetworkPreference.setOrder(size);
        preferenceScreen.addPreference(this.mAddNetworkPreference);
        if (getPreferenceScreen().getPreferenceCount() < 1) {
            Log.w("SavedAccessPoints", "Saved networks activity loaded, but there are no saved networks!");
        }
    }

    public void postUpdatePreference() {
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.sendEmptyMessage(1);
        }
    }

    private void showWifiDialog(LongPressAccessPointPreference longPressAccessPointPreference) {
        if (this.mDialog != null) {
            removeDialog(1);
            this.mDialog = null;
        }
        if (longPressAccessPointPreference != null) {
            this.mDlgAccessPoint = longPressAccessPointPreference.getAccessPoint();
        } else {
            this.mDlgAccessPoint = null;
            this.mAccessPointSavedState = null;
        }
        showDialog(1);
    }

    @Override
    public Dialog onCreateDialog(int i) {
        if (i == 1) {
            if (this.mDlgAccessPoint == null && this.mAccessPointSavedState == null) {
                this.mDialog = WifiDialog.createFullscreen(getActivity(), this, null, 1);
            } else {
                if (this.mDlgAccessPoint == null) {
                    this.mDlgAccessPoint = new AccessPoint(getActivity(), this.mAccessPointSavedState);
                    this.mAccessPointSavedState = null;
                }
                this.mDialog = WifiDialog.createModal(getActivity(), this, this.mDlgAccessPoint, 0);
            }
            this.mSelectedAccessPoint = this.mDlgAccessPoint;
            return this.mDialog;
        }
        return super.onCreateDialog(i);
    }

    @Override
    public int getDialogMetricsCategory(int i) {
        if (i == 1) {
            return 602;
        }
        return 0;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mDialog != null && this.mDialog.isShowing() && this.mDlgAccessPoint != null) {
            this.mAccessPointSavedState = new Bundle();
            this.mDlgAccessPoint.saveWifiState(this.mAccessPointSavedState);
            bundle.putBundle("wifi_ap_state", this.mAccessPointSavedState);
        }
    }

    @Override
    public void onForget(WifiDialog wifiDialog) {
        if (this.mSelectedAccessPoint != null) {
            if (this.mSelectedAccessPoint.isPasspointConfig()) {
                try {
                    this.mWifiManager.removePasspointConfiguration(this.mSelectedAccessPoint.getPasspointFqdn());
                } catch (RuntimeException e) {
                    Log.e("SavedAccessPoints", "Failed to remove Passpoint configuration for " + this.mSelectedAccessPoint.getConfigName());
                }
                postUpdatePreference();
            } else {
                this.mWifiManager.forget(this.mSelectedAccessPoint.getConfig().networkId, this.mForgetListener);
            }
            this.mSelectedAccessPoint = null;
        }
    }

    @Override
    public void onSubmit(WifiDialog wifiDialog) {
        this.mWifiManager.save(wifiDialog.getController().getConfig(), this.mSaveListener);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof LongPressAccessPointPreference) {
            showWifiDialog((LongPressAccessPointPreference) preference);
            return true;
        }
        if (preference == this.mAddNetworkPreference) {
            showWifiDialog(null);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }
}
