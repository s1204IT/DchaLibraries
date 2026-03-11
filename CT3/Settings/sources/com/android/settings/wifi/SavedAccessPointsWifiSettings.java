package com.android.settings.wifi;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.wifi.WifiDialog;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiTracker;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SavedAccessPointsWifiSettings extends SettingsPreferenceFragment implements Indexable, WifiDialog.WifiDialogListener {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList<>();
            Resources res = context.getResources();
            String title = res.getString(R.string.wifi_saved_access_points_titlebar);
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = title;
            data.screenTitle = title;
            data.enabled = enabled;
            result.add(data);
            List<AccessPoint> accessPoints = WifiTracker.getCurrentAccessPoints(context, true, false, true);
            int accessPointsSize = accessPoints.size();
            for (int i = 0; i < accessPointsSize; i++) {
                SearchIndexableRaw data2 = new SearchIndexableRaw(context);
                data2.title = accessPoints.get(i).getSsidStr();
                data2.screenTitle = title;
                data2.enabled = enabled;
                result.add(data2);
            }
            return result;
        }
    };
    private Bundle mAccessPointSavedState;
    private WifiDialog mDialog;
    private AccessPoint mDlgAccessPoint;
    private AccessPoint mSelectedAccessPoint;
    private AccessPointPreference.UserBadgeCache mUserBadgeCache;
    private WifiManager mWifiManager;

    @Override
    protected int getMetricsCategory() {
        return 106;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_display_saved_access_points);
        this.mUserBadgeCache = new AccessPointPreference.UserBadgeCache(getPackageManager());
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        if (savedInstanceState == null || !savedInstanceState.containsKey("wifi_ap_state")) {
            return;
        }
        this.mAccessPointSavedState = savedInstanceState.getBundle("wifi_ap_state");
    }

    private void initPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Context context = getPrefContext();
        List<AccessPoint> accessPoints = WifiTracker.getCurrentAccessPoints(context, true, false, true);
        Collections.sort(accessPoints, new Comparator<AccessPoint>() {
            @Override
            public int compare(AccessPoint ap1, AccessPoint ap2) {
                if (ap1.getConfigName() != null) {
                    return ap1.getConfigName().compareTo(ap2.getConfigName());
                }
                return -1;
            }
        });
        preferenceScreen.removeAll();
        int accessPointsSize = accessPoints.size();
        for (int i = 0; i < accessPointsSize; i++) {
            LongPressAccessPointPreference preference = new LongPressAccessPointPreference(accessPoints.get(i), context, this.mUserBadgeCache, true, this);
            preference.setIcon((Drawable) null);
            preferenceScreen.addPreference(preference);
        }
        if (getPreferenceScreen().getPreferenceCount() >= 1) {
            return;
        }
        Log.w("SavedAccessPointsWifiSettings", "Saved networks activity loaded, but there are no saved networks!");
    }

    private void showDialog(LongPressAccessPointPreference accessPoint, boolean edit) {
        if (this.mDialog != null) {
            removeDialog(1);
            this.mDialog = null;
        }
        this.mDlgAccessPoint = accessPoint.getAccessPoint();
        showDialog(1);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DefaultWfcSettingsExt.PAUSE:
                if (this.mDlgAccessPoint == null) {
                    this.mDlgAccessPoint = new AccessPoint(getActivity(), this.mAccessPointSavedState);
                    this.mAccessPointSavedState = null;
                }
                this.mSelectedAccessPoint = this.mDlgAccessPoint;
                this.mDialog = new WifiDialog(getActivity(), this, this.mDlgAccessPoint, 0, true);
                return this.mDialog;
            default:
                return super.onCreateDialog(dialogId);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mDialog == null || !this.mDialog.isShowing() || this.mDlgAccessPoint == null) {
            return;
        }
        this.mAccessPointSavedState = new Bundle();
        this.mDlgAccessPoint.saveWifiState(this.mAccessPointSavedState);
        outState.putBundle("wifi_ap_state", this.mAccessPointSavedState);
    }

    @Override
    public void onForget(WifiDialog dialog) {
        if (this.mSelectedAccessPoint == null) {
            return;
        }
        this.mWifiManager.forget(this.mSelectedAccessPoint.getConfig().networkId, null);
        if (findSelectedAccessPointPreference() != null) {
            getPreferenceScreen().removePreference(findSelectedAccessPointPreference());
        }
        this.mSelectedAccessPoint = null;
    }

    private AccessPointPreference findSelectedAccessPointPreference() {
        PreferenceScreen prefScreen = getPreferenceScreen();
        int size = prefScreen.getPreferenceCount();
        for (int i = 0; i < size; i++) {
            AccessPointPreference ap = (AccessPointPreference) prefScreen.getPreference(i);
            if (ap.getAccessPoint() != null && this.mSelectedAccessPoint != null && ap.getAccessPoint().getConfig().networkId == this.mSelectedAccessPoint.getConfig().networkId) {
                return ap;
            }
        }
        return null;
    }

    @Override
    public void onSubmit(WifiDialog dialog) {
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof LongPressAccessPointPreference) {
            showDialog((LongPressAccessPointPreference) preference, false);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }
}
