package com.android.settings.wifi;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavedAccessPointsWifiSettings extends SettingsPreferenceFragment implements DialogInterface.OnClickListener, Indexable {
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
            WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
            List<AccessPoint> accessPoints = SavedAccessPointsWifiSettings.constructSavedAccessPoints(context, wifiManager);
            int accessPointsSize = accessPoints.size();
            for (int i = 0; i < accessPointsSize; i++) {
                SearchIndexableRaw data2 = new SearchIndexableRaw(context);
                data2.title = accessPoints.get(i).getTitle().toString();
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
    private WifiManager mWifiManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_display_saved_access_points);
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
        if (savedInstanceState != null && savedInstanceState.containsKey("wifi_ap_state")) {
            this.mAccessPointSavedState = savedInstanceState.getBundle("wifi_ap_state");
        }
    }

    private void initPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Context context = getActivity();
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        List<AccessPoint> accessPoints = constructSavedAccessPoints(context, this.mWifiManager);
        preferenceScreen.removeAll();
        int accessPointsSize = accessPoints.size();
        for (int i = 0; i < accessPointsSize; i++) {
            preferenceScreen.addPreference(accessPoints.get(i));
        }
        if (getPreferenceScreen().getPreferenceCount() < 1) {
            Log.w("SavedAccessPointsWifiSettings", "Saved networks activity loaded, but there are no saved networks!");
        }
    }

    public static List<AccessPoint> constructSavedAccessPoints(Context context, WifiManager wifiManager) {
        List<AccessPoint> accessPoints = new ArrayList<>();
        Map<String, List<ScanResult>> resultsMap = new HashMap<>();
        List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
        List<ScanResult> scanResults = wifiManager.getScanResults();
        if (configs != null) {
            int scanResultsSize = scanResults.size();
            for (int i = 0; i < scanResultsSize; i++) {
                ScanResult result = scanResults.get(i);
                List<ScanResult> res = resultsMap.get(result.SSID);
                if (res == null) {
                    res = new ArrayList<>();
                    resultsMap.put(result.SSID, res);
                }
                res.add(result);
            }
            int configsSize = configs.size();
            for (int i2 = 0; i2 < configsSize; i2++) {
                WifiConfiguration config = configs.get(i2);
                if (!config.selfAdded || config.numAssociation != 0) {
                    AccessPoint accessPoint = new AccessPoint(context, config);
                    List<ScanResult> results = resultsMap.get(accessPoint.ssid);
                    accessPoint.setShowSummary(false);
                    if (results != null) {
                        int resultsSize = results.size();
                        for (int j = 0; j < resultsSize; j++) {
                            accessPoint.update(results.get(j));
                            accessPoint.setIcon((Drawable) null);
                        }
                    }
                    accessPoints.add(accessPoint);
                }
            }
        }
        return accessPoints;
    }

    private void showDialog(AccessPoint accessPoint, boolean edit) {
        if (this.mDialog != null) {
            removeDialog(1);
            this.mDialog = null;
        }
        this.mDlgAccessPoint = accessPoint;
        showDialog(1);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case 1:
                if (this.mDlgAccessPoint == null) {
                    this.mDlgAccessPoint = new AccessPoint(getActivity(), this.mAccessPointSavedState);
                    this.mAccessPointSavedState = null;
                }
                this.mSelectedAccessPoint = this.mDlgAccessPoint;
                this.mDialog = new WifiDialog(getActivity(), this, this.mDlgAccessPoint, false, true);
                return this.mDialog;
            default:
                return super.onCreateDialog(dialogId);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mDialog != null && this.mDialog.isShowing() && this.mDlgAccessPoint != null) {
            this.mAccessPointSavedState = new Bundle();
            this.mDlgAccessPoint.saveWifiState(this.mAccessPointSavedState);
            outState.putBundle("wifi_ap_state", this.mAccessPointSavedState);
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == -3 && this.mSelectedAccessPoint != null) {
            this.mWifiManager.forget(this.mSelectedAccessPoint.networkId, null);
            getPreferenceScreen().removePreference(this.mSelectedAccessPoint);
            this.mSelectedAccessPoint = null;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (!(preference instanceof AccessPoint)) {
            return super.onPreferenceTreeClick(screen, preference);
        }
        showDialog((AccessPoint) preference, false);
        return true;
    }
}
