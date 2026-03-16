package com.android.settings.wifi;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class WifiSettings extends RestrictedSettingsFragment implements DialogInterface.OnClickListener, Indexable {
    private static boolean savedNetworksExist;
    private Bundle mAccessPointSavedState;
    private WifiManager.ActionListener mConnectListener;
    private final AtomicBoolean mConnected;
    private WifiDialog mDialog;
    private AccessPoint mDlgAccessPoint;
    private boolean mDlgEdit;
    private TextView mEmptyView;
    private boolean mEnableNextOnConnection;
    private final IntentFilter mFilter;
    private WifiManager.ActionListener mForgetListener;
    private android.net.wifi.WifiInfo mLastInfo;
    private NetworkInfo mLastNetworkInfo;
    private final BroadcastReceiver mReceiver;
    private WifiManager.ActionListener mSaveListener;
    private final Scanner mScanner;
    private AccessPoint mSelectedAccessPoint;
    private WifiEnabler mWifiEnabler;
    WifiManager mWifiManager;
    private WriteWifiConfigToNfcDialog mWifiToNfcDialog;
    public static int mVerboseLogging = 0;
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList<>();
            Resources res = context.getResources();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.wifi_settings);
            data.screenTitle = res.getString(R.string.wifi_settings);
            data.keywords = res.getString(R.string.keywords_wifi);
            result.add(data);
            WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
            Collection<AccessPoint> accessPoints = WifiSettings.constructAccessPoints(context, wifiManager, null, null);
            for (AccessPoint accessPoint : accessPoints) {
                if (accessPoint.getConfig() != null) {
                    SearchIndexableRaw data2 = new SearchIndexableRaw(context);
                    data2.title = accessPoint.getTitle().toString();
                    data2.screenTitle = res.getString(R.string.wifi_settings);
                    data2.enabled = enabled;
                    result.add(data2);
                }
            }
            return result;
        }
    };

    private static class Multimap<K, V> {
        private final HashMap<K, List<V>> store;

        private Multimap() {
            this.store = new HashMap<>();
        }

        List<V> getAll(K key) {
            List<V> values = this.store.get(key);
            return values != null ? values : Collections.emptyList();
        }

        void put(K key, V val) {
            List<V> curVals = this.store.get(key);
            if (curVals == null) {
                curVals = new ArrayList<>(3);
                this.store.put(key, curVals);
            }
            curVals.add(val);
        }
    }

    private static class Scanner extends Handler {
        private int mRetry = 0;
        private WifiSettings mWifiSettings;

        Scanner(WifiSettings wifiSettings) {
            this.mWifiSettings = null;
            this.mWifiSettings = wifiSettings;
        }

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void forceScan() {
            removeMessages(0);
            sendEmptyMessage(0);
        }

        void pause() {
            this.mRetry = 0;
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message message) {
            if (this.mWifiSettings.mWifiManager.startScan()) {
                this.mRetry = 0;
            } else {
                int i = this.mRetry + 1;
                this.mRetry = i;
                if (i >= 3) {
                    this.mRetry = 0;
                    Activity activity = this.mWifiSettings.getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, R.string.wifi_fail_to_scan, 1).show();
                        return;
                    }
                    return;
                }
            }
            sendEmptyMessageDelayed(0, 10000L);
        }
    }

    public WifiSettings() {
        super("no_config_wifi");
        this.mConnected = new AtomicBoolean(false);
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mFilter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mFilter.addAction("android.net.wifi.NETWORK_IDS_CHANGED");
        this.mFilter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
        this.mFilter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        this.mFilter.addAction("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mFilter.addAction("android.net.wifi.RSSI_CHANGED");
        this.mFilter.addAction("com.android.settings.wifi.WpsDialog.WpaReconnect");
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiSettings.this.handleEvent(context, intent);
            }
        };
        this.mScanner = new Scanner(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        ConnectivityManager connectivity;
        super.onActivityCreated(savedInstanceState);
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        this.mConnectListener = new WifiManager.ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {
                Activity activity = WifiSettings.this.getActivity();
                if (activity != null) {
                    Toast.makeText(activity, R.string.wifi_failed_connect_message, 0).show();
                }
            }
        };
        this.mSaveListener = new WifiManager.ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {
                Activity activity = WifiSettings.this.getActivity();
                if (activity != null) {
                    Toast.makeText(activity, R.string.wifi_failed_save_message, 0).show();
                }
            }
        };
        this.mForgetListener = new WifiManager.ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {
                Activity activity = WifiSettings.this.getActivity();
                if (activity != null) {
                    Toast.makeText(activity, R.string.wifi_failed_forget_message, 0).show();
                }
            }
        };
        if (savedInstanceState != null) {
            this.mDlgEdit = savedInstanceState.getBoolean("edit_mode");
            if (savedInstanceState.containsKey("wifi_ap_state")) {
                this.mAccessPointSavedState = savedInstanceState.getBundle("wifi_ap_state");
            }
        }
        Intent intent = getActivity().getIntent();
        this.mEnableNextOnConnection = intent.getBooleanExtra("wifi_enable_next_on_connect", false);
        if (this.mEnableNextOnConnection && hasNextButton() && (connectivity = (ConnectivityManager) getActivity().getSystemService("connectivity")) != null) {
            NetworkInfo info = connectivity.getNetworkInfo(1);
            changeNextButtonState(info.isConnected());
        }
        addPreferencesFromResource(R.xml.wifi_settings);
        this.mEmptyView = initEmptyView();
        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
        if (intent.hasExtra("wifi_start_connect_ssid")) {
            String ssid = intent.getStringExtra("wifi_start_connect_ssid");
            updateAccessPoints();
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
                Preference preference = preferenceScreen.getPreference(i);
                if (preference instanceof AccessPoint) {
                    AccessPoint accessPoint = (AccessPoint) preference;
                    if (ssid.equals(accessPoint.ssid) && accessPoint.networkId == -1 && accessPoint.security != 0) {
                        onPreferenceTreeClick(preferenceScreen, preference);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.teardownSwitchBar();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mWifiEnabler = createWifiEnabler();
    }

    WifiEnabler createWifiEnabler() {
        SettingsActivity activity = (SettingsActivity) getActivity();
        return new WifiEnabler(activity, activity.getSwitchBar());
    }

    @Override
    public void onResume() {
        Activity activity = getActivity();
        super.onResume();
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.resume(activity);
        }
        activity.registerReceiver(this.mReceiver, this.mFilter);
        updateAccessPoints();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.pause();
        }
        getActivity().unregisterReceiver(this.mReceiver);
        this.mScanner.pause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isUiRestricted()) {
            addOptionsMenuItems(menu);
            super.onCreateOptionsMenu(menu, inflater);
        }
    }

    void addOptionsMenuItems(Menu menu) {
        boolean wifiIsEnabled = this.mWifiManager.isWifiEnabled();
        TypedArray ta = getActivity().getTheme().obtainStyledAttributes(new int[]{R.attr.ic_menu_add, R.attr.ic_wps});
        menu.add(0, 4, 0, R.string.wifi_add_network).setIcon(ta.getDrawable(0)).setEnabled(wifiIsEnabled).setShowAsAction(0);
        if (savedNetworksExist) {
            menu.add(0, 3, 0, R.string.wifi_saved_access_points_label).setIcon(ta.getDrawable(0)).setEnabled(wifiIsEnabled).setShowAsAction(0);
        }
        menu.add(0, 6, 0, R.string.menu_stats_refresh).setEnabled(wifiIsEnabled).setShowAsAction(0);
        menu.add(0, 5, 0, R.string.wifi_menu_advanced).setShowAsAction(0);
        ta.recycle();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mDialog != null && this.mDialog.isShowing()) {
            outState.putBoolean("edit_mode", this.mDlgEdit);
            if (this.mDlgAccessPoint != null) {
                this.mAccessPointSavedState = new Bundle();
                this.mDlgAccessPoint.saveWifiState(this.mAccessPointSavedState);
                outState.putBundle("wifi_ap_state", this.mAccessPointSavedState);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isUiRestricted()) {
            return false;
        }
        switch (item.getItemId()) {
            case 1:
                showDialog(2);
                return true;
            case 2:
                showDialog(3);
                return true;
            case 3:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(SavedAccessPointsWifiSettings.class.getCanonicalName(), null, R.string.wifi_saved_access_points_titlebar, null, this, 0);
                } else {
                    startFragment(this, SavedAccessPointsWifiSettings.class.getCanonicalName(), R.string.wifi_saved_access_points_titlebar, -1, null);
                }
                return true;
            case 4:
                if (this.mWifiManager.isWifiEnabled()) {
                    onAddNetworkPressed();
                }
                return true;
            case 5:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(AdvancedWifiSettings.class.getCanonicalName(), null, R.string.wifi_advanced_titlebar, null, this, 0);
                } else {
                    startFragment(this, AdvancedWifiSettings.class.getCanonicalName(), R.string.wifi_advanced_titlebar, -1, null);
                }
                return true;
            case 6:
                if (this.mWifiManager.isWifiEnabled()) {
                    this.mScanner.forceScan();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        if (info instanceof AdapterView.AdapterContextMenuInfo) {
            Preference preference = (Preference) getListView().getItemAtPosition(((AdapterView.AdapterContextMenuInfo) info).position);
            if (preference instanceof AccessPoint) {
                this.mSelectedAccessPoint = (AccessPoint) preference;
                menu.setHeaderTitle(this.mSelectedAccessPoint.ssid);
                if (this.mSelectedAccessPoint.getLevel() != -1 && this.mSelectedAccessPoint.getState() == null) {
                    menu.add(0, 7, 0, R.string.wifi_menu_connect);
                }
                if (ActivityManager.getCurrentUser() == 0 && (this.mSelectedAccessPoint.networkId != -1 || (this.mSelectedAccessPoint.getNetworkInfo() != null && this.mSelectedAccessPoint.getNetworkInfo().getState() != NetworkInfo.State.DISCONNECTED))) {
                    menu.add(0, 8, 0, R.string.wifi_menu_forget);
                }
                if (this.mSelectedAccessPoint.networkId != -1) {
                    menu.add(0, 9, 0, R.string.wifi_menu_modify);
                    NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
                    if (nfcAdapter != null && nfcAdapter.isEnabled() && this.mSelectedAccessPoint.security != 0) {
                        menu.add(0, 10, 0, R.string.wifi_menu_write_to_nfc);
                    }
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (this.mSelectedAccessPoint == null) {
            return super.onContextItemSelected(item);
        }
        switch (item.getItemId()) {
            case 7:
                if (this.mSelectedAccessPoint.networkId != -1) {
                    connect(this.mSelectedAccessPoint.networkId);
                } else if (this.mSelectedAccessPoint.security == 0) {
                    this.mSelectedAccessPoint.generateOpenNetworkConfig();
                    connect(this.mSelectedAccessPoint.getConfig());
                } else {
                    showDialog(this.mSelectedAccessPoint, true);
                }
                break;
            case 8:
                forget();
                break;
            case 9:
                showDialog(this.mSelectedAccessPoint, true);
                break;
            case 10:
                showDialog(6);
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference instanceof AccessPoint) {
            this.mSelectedAccessPoint = (AccessPoint) preference;
            if (this.mSelectedAccessPoint.security == 0 && this.mSelectedAccessPoint.networkId == -1 && !this.mSelectedAccessPoint.isActive()) {
                this.mSelectedAccessPoint.generateOpenNetworkConfig();
                if (!savedNetworksExist) {
                    savedNetworksExist = true;
                    getActivity().invalidateOptionsMenu();
                }
                connect(this.mSelectedAccessPoint.getConfig());
                return true;
            }
            showDialog(this.mSelectedAccessPoint, false);
            return true;
        }
        return super.onPreferenceTreeClick(screen, preference);
    }

    private void showDialog(AccessPoint accessPoint, boolean edit) {
        if (this.mDialog != null) {
            removeDialog(1);
            this.mDialog = null;
        }
        this.mDlgAccessPoint = accessPoint;
        this.mDlgEdit = edit;
        showDialog(1);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case 1:
                AccessPoint ap = this.mDlgAccessPoint;
                if (ap == null && this.mAccessPointSavedState != null) {
                    ap = new AccessPoint(getActivity(), this.mAccessPointSavedState);
                    this.mDlgAccessPoint = ap;
                    this.mAccessPointSavedState = null;
                }
                this.mSelectedAccessPoint = ap;
                this.mDialog = new WifiDialog(getActivity(), this, ap, this.mDlgEdit);
                return this.mDialog;
            case 2:
                return new WpsDialog(getActivity(), 0);
            case 3:
                return new WpsDialog(getActivity(), 1);
            case 6:
                if (this.mSelectedAccessPoint != null) {
                    this.mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(getActivity(), this.mSelectedAccessPoint, this.mWifiManager);
                    return this.mWifiToNfcDialog;
                }
                break;
        }
        return super.onCreateDialog(dialogId);
    }

    private void updateAccessPoints() {
        if (getActivity() != null) {
            if (isUiRestricted()) {
                addMessagePreference(R.string.wifi_empty_list_user_restricted);
                return;
            }
            int wifiState = this.mWifiManager.getWifiState();
            mVerboseLogging = this.mWifiManager.getVerboseLoggingLevel();
            switch (wifiState) {
                case 0:
                    addMessagePreference(R.string.wifi_stopping);
                    break;
                case 1:
                    setOffMessage();
                    break;
                case 2:
                    getPreferenceScreen().removeAll();
                    break;
                case 3:
                    Collection<AccessPoint> accessPoints = constructAccessPoints(getActivity(), this.mWifiManager, this.mLastInfo, this.mLastNetworkInfo);
                    getPreferenceScreen().removeAll();
                    if (accessPoints.size() == 0) {
                        addMessagePreference(R.string.wifi_empty_list_wifi_on);
                    }
                    for (AccessPoint accessPoint : accessPoints) {
                        if (accessPoint.getLevel() != -1) {
                            getPreferenceScreen().addPreference(accessPoint);
                        }
                    }
                    break;
            }
        }
    }

    protected TextView initEmptyView() {
        TextView emptyView = (TextView) getActivity().findViewById(android.R.id.empty);
        getListView().setEmptyView(emptyView);
        return emptyView;
    }

    private void setOffMessage() {
        int resId;
        if (this.mEmptyView != null) {
            this.mEmptyView.setText(R.string.wifi_empty_list_wifi_off);
            if (Settings.Global.getInt(getActivity().getContentResolver(), "wifi_scan_always_enabled", 0) == 1) {
                this.mEmptyView.append("\n\n");
                if (Settings.Secure.isLocationProviderEnabled(getActivity().getContentResolver(), "network")) {
                    resId = R.string.wifi_scan_notify_text_location_on;
                } else {
                    resId = R.string.wifi_scan_notify_text_location_off;
                }
                CharSequence charSeq = getText(resId);
                this.mEmptyView.append(charSeq);
            }
        }
        getPreferenceScreen().removeAll();
    }

    private void addMessagePreference(int messageId) {
        if (this.mEmptyView != null) {
            this.mEmptyView.setText(messageId);
        }
        getPreferenceScreen().removeAll();
    }

    private static List<AccessPoint> constructAccessPoints(Context context, WifiManager wifiManager, android.net.wifi.WifiInfo lastInfo, NetworkInfo lastNetworkInfo) {
        ArrayList<AccessPoint> accessPoints = new ArrayList<>();
        Multimap<String, AccessPoint> apMap = new Multimap<>();
        List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
        if (configs != null) {
            if (savedNetworksExist != (configs.size() > 0)) {
                savedNetworksExist = !savedNetworksExist;
                if (context instanceof Activity) {
                    ((Activity) context).invalidateOptionsMenu();
                }
            }
            for (WifiConfiguration config : configs) {
                if (!config.selfAdded || config.numAssociation != 0) {
                    AccessPoint accessPoint = new AccessPoint(context, config);
                    if (lastInfo != null && lastNetworkInfo != null) {
                        accessPoint.update(lastInfo, lastNetworkInfo);
                    }
                    accessPoints.add(accessPoint);
                    apMap.put(accessPoint.ssid, accessPoint);
                }
            }
        }
        List<ScanResult> results = wifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                if (result.SSID != null && result.SSID.length() != 0 && !result.capabilities.contains("[IBSS]")) {
                    boolean found = false;
                    Iterator<AccessPoint> it = apMap.getAll(result.SSID).iterator();
                    while (it.hasNext()) {
                        if (it.next().update(result)) {
                            found = true;
                        }
                    }
                    if (!found) {
                        AccessPoint accessPoint2 = new AccessPoint(context, result);
                        if (lastInfo != null && lastNetworkInfo != null) {
                            accessPoint2.update(lastInfo, lastNetworkInfo);
                        }
                        accessPoints.add(accessPoint2);
                        apMap.put(accessPoint2.ssid, accessPoint2);
                    }
                }
            }
        }
        Collections.sort(accessPoints);
        return accessPoints;
    }

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
            updateWifiState(intent.getIntExtra("wifi_state", 4));
            return;
        }
        if ("android.net.wifi.SCAN_RESULTS".equals(action) || "android.net.wifi.CONFIGURED_NETWORKS_CHANGE".equals(action) || "android.net.wifi.LINK_CONFIGURATION_CHANGED".equals(action)) {
            updateAccessPoints();
            return;
        }
        if ("android.net.wifi.STATE_CHANGE".equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            this.mConnected.set(info.isConnected());
            changeNextButtonState(info.isConnected());
            updateAccessPoints();
            updateNetworkInfo(info);
            return;
        }
        if ("android.net.wifi.RSSI_CHANGED".equals(action)) {
            updateNetworkInfo(null);
            return;
        }
        if ("com.android.settings.wifi.WpsDialog.WpaReconnect".equals(action)) {
            WifiConfiguration config = (WifiConfiguration) intent.getParcelableExtra("wifiConfiguration");
            context.removeStickyBroadcast(intent);
            if (config != null) {
                int reNetworkId = config.networkId;
                for (int i = getPreferenceScreen().getPreferenceCount() - 1; i >= 0; i--) {
                    Preference preference = getPreferenceScreen().getPreference(i);
                    if (preference instanceof AccessPoint) {
                        AccessPoint accessPoint = (AccessPoint) preference;
                        if (accessPoint.networkId != -1 && accessPoint.networkId == reNetworkId) {
                            accessPoint.update(null, null);
                            showDialog(accessPoint, false);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void updateNetworkInfo(NetworkInfo networkInfo) {
        if (!this.mWifiManager.isWifiEnabled()) {
            this.mScanner.pause();
            return;
        }
        if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
            this.mScanner.pause();
        } else {
            this.mScanner.resume();
        }
        this.mLastInfo = this.mWifiManager.getConnectionInfo();
        if (networkInfo != null) {
            this.mLastNetworkInfo = networkInfo;
        }
        for (int i = getPreferenceScreen().getPreferenceCount() - 1; i >= 0; i--) {
            Preference preference = getPreferenceScreen().getPreference(i);
            if (preference instanceof AccessPoint) {
                AccessPoint accessPoint = (AccessPoint) preference;
                accessPoint.update(this.mLastInfo, this.mLastNetworkInfo);
            }
        }
    }

    private void updateWifiState(int state) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
        switch (state) {
            case 1:
                setOffMessage();
                break;
            case 2:
                addMessagePreference(R.string.wifi_starting);
                break;
            case 3:
                this.mScanner.resume();
                return;
        }
        this.mLastInfo = null;
        this.mLastNetworkInfo = null;
        this.mScanner.pause();
    }

    private void changeNextButtonState(boolean enabled) {
        if (this.mEnableNextOnConnection && hasNextButton()) {
            getNextButton().setEnabled(enabled);
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == -3 && this.mSelectedAccessPoint != null) {
            forget();
        } else if (button == -1 && this.mDialog != null) {
            submit(this.mDialog.getController());
        }
    }

    void submit(WifiConfigController configController) {
        WifiConfiguration config = configController.getConfig();
        if (config == null) {
            if (this.mSelectedAccessPoint != null && this.mSelectedAccessPoint.networkId != -1) {
                connect(this.mSelectedAccessPoint.networkId);
            }
        } else if (config.networkId != -1) {
            if (this.mSelectedAccessPoint != null) {
                this.mWifiManager.save(config, this.mSaveListener);
            }
        } else if (configController.isEdit()) {
            this.mWifiManager.save(config, this.mSaveListener);
        } else {
            connect(config);
        }
        if (this.mWifiManager.isWifiEnabled()) {
            this.mScanner.resume();
        }
        updateAccessPoints();
    }

    void forget() {
        if (this.mSelectedAccessPoint.networkId == -1) {
            if (this.mSelectedAccessPoint.getNetworkInfo().getState() != NetworkInfo.State.DISCONNECTED) {
                this.mWifiManager.disableEphemeralNetwork(AccessPoint.convertToQuotedString(this.mSelectedAccessPoint.ssid));
            } else {
                Log.e("WifiSettings", "Failed to forget invalid network " + this.mSelectedAccessPoint.getConfig());
                return;
            }
        } else {
            this.mWifiManager.forget(this.mSelectedAccessPoint.networkId, this.mForgetListener);
        }
        if (this.mWifiManager.isWifiEnabled()) {
            this.mScanner.resume();
        }
        updateAccessPoints();
        changeNextButtonState(false);
    }

    protected void connect(WifiConfiguration config) {
        this.mWifiManager.connect(config, this.mConnectListener);
    }

    protected void connect(int networkId) {
        this.mWifiManager.connect(networkId, this.mConnectListener);
    }

    void refreshAccessPoints() {
        if (this.mWifiManager.isWifiEnabled()) {
            this.mScanner.resume();
        }
        getPreferenceScreen().removeAll();
    }

    void onAddNetworkPressed() {
        this.mSelectedAccessPoint = null;
        showDialog(null, true);
    }

    void resumeWifiScan() {
        if (this.mWifiManager.isWifiEnabled()) {
            this.mScanner.resume();
        }
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_wifi;
    }
}
