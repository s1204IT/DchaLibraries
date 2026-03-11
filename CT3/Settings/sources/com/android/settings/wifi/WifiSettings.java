package com.android.settings.wifi;

import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.HandlerThread;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Spannable;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.LinkifyUtils;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.location.ScanningSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.wifi.WifiDialog;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiStatusTracker;
import com.android.settingslib.wifi.WifiTracker;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.wifi.WifiSettingsExt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WifiSettings extends RestrictedSettingsFragment implements Indexable, WifiTracker.WifiListener, AccessPoint.AccessPointListener, WifiDialog.WifiDialogListener {
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
            Collection<AccessPoint> accessPoints = WifiTracker.getCurrentAccessPoints(context, true, false, false);
            for (AccessPoint accessPoint : accessPoints) {
                SearchIndexableRaw data2 = new SearchIndexableRaw(context);
                data2.title = accessPoint.getSsidStr();
                data2.screenTitle = res.getString(R.string.wifi_settings);
                data2.enabled = enabled;
                result.add(data2);
            }
            return result;
        }
    };
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    private Bundle mAccessPointSavedState;
    private Preference mAddPreference;
    private HandlerThread mBgThread;
    private WifiManager.ActionListener mConnectListener;
    private WifiDialog mDialog;
    private int mDialogMode;
    private AccessPoint mDlgAccessPoint;
    private boolean mEnableNextOnConnection;
    private final IntentFilter mFilter;
    private WifiManager.ActionListener mForgetListener;
    private String mOpenSsid;
    private ProgressBar mProgressHeader;
    private final BroadcastReceiver mReceiver;
    private WifiManager.ActionListener mSaveListener;
    private MenuItem mScanMenuItem;
    private AccessPoint mSelectedAccessPoint;
    private AccessPointPreference.UserBadgeCache mUserBadgeCache;
    private WifiEnabler mWifiEnabler;
    protected WifiManager mWifiManager;
    private Bundle mWifiNfcDialogSavedState;
    private WifiSettingsExt mWifiSettingsExt;
    private WriteWifiConfigToNfcDialog mWifiToNfcDialog;
    private WifiTracker mWifiTracker;

    public WifiSettings() {
        super("no_config_wifi");
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.wifi.NO_CERTIFICATION");
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiSettings.this.handleEvent(intent);
            }
        };
    }

    public void handleEvent(Intent intent) {
        String action = intent.getAction();
        Log.d("WifiSettings", "handleEvent(), action = " + action);
        if (!"android.net.wifi.NO_CERTIFICATION".equals(action)) {
            return;
        }
        String apSSID = "";
        if (this.mSelectedAccessPoint != null) {
            apSSID = "[" + this.mSelectedAccessPoint.getSsidStr() + "] ";
        }
        Log.i("WifiSettings", "Receive  no certification broadcast for AP " + apSSID);
        String message = getResources().getString(R.string.wifi_no_cert_for_wapi) + apSSID;
        Toast.makeText(getActivity(), message, 1).show();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        this.mProgressHeader = (ProgressBar) setPinnedHeaderView(R.layout.wifi_progress_header);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.wifi_settings);
        this.mAddPreference = new Preference(getContext());
        this.mAddPreference.setIcon(R.drawable.ic_menu_add_inset);
        this.mAddPreference.setTitle(R.string.wifi_add_network);
        this.mUserBadgeCache = new AccessPointPreference.UserBadgeCache(getPackageManager());
        this.mBgThread = new HandlerThread("WifiSettings", 10);
        this.mBgThread.start();
        this.mWifiSettingsExt = new WifiSettingsExt(getActivity());
        this.mWifiSettingsExt.onCreate();
    }

    @Override
    public void onDestroy() {
        this.mBgThread.quit();
        this.mWifiSettingsExt.unregisterPriorityObserver(getContentResolver());
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        ConnectivityManager connectivity;
        super.onActivityCreated(savedInstanceState);
        this.mWifiTracker = new WifiTracker(getActivity(), this, this.mBgThread.getLooper(), true, true, false);
        this.mWifiManager = this.mWifiTracker.getManager();
        this.mConnectListener = new WifiManager.ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {
                Activity activity = WifiSettings.this.getActivity();
                if (activity == null) {
                    return;
                }
                Toast.makeText(activity, R.string.wifi_failed_connect_message, 0).show();
            }
        };
        this.mSaveListener = new WifiManager.ActionListener() {
            public void onSuccess() {
                WifiSettings.this.mWifiSettingsExt.updatePriority();
            }

            public void onFailure(int reason) {
                Activity activity = WifiSettings.this.getActivity();
                if (activity == null) {
                    return;
                }
                Toast.makeText(activity, R.string.wifi_failed_save_message, 0).show();
            }
        };
        this.mForgetListener = new WifiManager.ActionListener() {
            public void onSuccess() {
                WifiSettings.this.mWifiSettingsExt.updatePriority();
            }

            public void onFailure(int reason) {
                Activity activity = WifiSettings.this.getActivity();
                if (activity == null) {
                    return;
                }
                Toast.makeText(activity, R.string.wifi_failed_forget_message, 0).show();
            }
        };
        if (savedInstanceState != null) {
            this.mDialogMode = savedInstanceState.getInt("dialog_mode");
            if (savedInstanceState.containsKey("wifi_ap_state")) {
                this.mAccessPointSavedState = savedInstanceState.getBundle("wifi_ap_state");
            }
            if (savedInstanceState.containsKey("wifi_nfc_dlg_state")) {
                this.mWifiNfcDialogSavedState = savedInstanceState.getBundle("wifi_nfc_dlg_state");
            }
        }
        Intent intent = getActivity().getIntent();
        this.mEnableNextOnConnection = intent.getBooleanExtra("wifi_enable_next_on_connect", false);
        if (this.mEnableNextOnConnection && hasNextButton() && (connectivity = (ConnectivityManager) getActivity().getSystemService("connectivity")) != null) {
            NetworkInfo info = connectivity.getNetworkInfo(1);
            changeNextButtonState(info.isConnected());
        }
        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
        this.mWifiSettingsExt.onActivityCreated(this, this.mWifiManager);
        if (!intent.hasExtra("wifi_start_connect_ssid")) {
            return;
        }
        this.mOpenSsid = intent.getStringExtra("wifi_start_connect_ssid");
        onAccessPointsChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mWifiEnabler == null) {
            return;
        }
        this.mWifiEnabler.teardownSwitchBar();
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
        removePreference("dummy");
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.resume(activity);
        }
        this.mWifiTracker.startTracking();
        activity.invalidateOptionsMenu();
        activity.registerReceiver(this.mReceiver, this.mFilter);
        this.mWifiSettingsExt.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.pause();
        }
        getActivity().unregisterReceiver(this.mReceiver);
        this.mWifiTracker.stopTracking();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (isUiRestricted()) {
            return;
        }
        addOptionsMenuItems(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    void addOptionsMenuItems(Menu menu) {
        boolean wifiIsEnabled = this.mWifiTracker.isWifiEnabled();
        this.mScanMenuItem = menu.add(0, 6, 0, R.string.menu_stats_refresh);
        this.mScanMenuItem.setEnabled(wifiIsEnabled).setShowAsAction(0);
        menu.add(0, 5, 0, R.string.wifi_menu_advanced).setShowAsAction(0);
        menu.add(0, 11, 0, R.string.wifi_menu_configure).setIcon(R.drawable.ic_settings_24dp).setShowAsAction(1);
    }

    @Override
    protected int getMetricsCategory() {
        return 103;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mDialog != null && this.mDialog.isShowing()) {
            outState.putInt("dialog_mode", this.mDialogMode);
            if (this.mDlgAccessPoint != null) {
                this.mAccessPointSavedState = new Bundle();
                this.mDlgAccessPoint.saveWifiState(this.mAccessPointSavedState);
                outState.putBundle("wifi_ap_state", this.mAccessPointSavedState);
            }
        }
        if (this.mWifiToNfcDialog == null || !this.mWifiToNfcDialog.isShowing()) {
            return;
        }
        Bundle savedState = new Bundle();
        this.mWifiToNfcDialog.saveState(savedState);
        outState.putBundle("wifi_nfc_dlg_state", savedState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isUiRestricted()) {
            return false;
        }
        switch (item.getItemId()) {
            case DefaultWfcSettingsExt.PAUSE:
                showDialog(2);
                return true;
            case DefaultWfcSettingsExt.CREATE:
                showDialog(3);
                return true;
            case DefaultWfcSettingsExt.DESTROY:
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
            case 7:
            case 8:
            case 9:
            case 10:
            default:
                return super.onOptionsItemSelected(item);
            case 5:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(AdvancedWifiSettings.class.getCanonicalName(), null, R.string.wifi_advanced_titlebar, null, this, 0);
                } else {
                    startFragment(this, AdvancedWifiSettings.class.getCanonicalName(), R.string.wifi_advanced_titlebar, -1, null);
                }
                return true;
            case 6:
                MetricsLogger.action(getActivity(), 136);
                this.mWifiTracker.forceScan();
                return true;
            case 11:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(ConfigureWifiSettings.class.getCanonicalName(), null, R.string.wifi_configure_titlebar, null, this, 0);
                } else {
                    startFragment(this, ConfigureWifiSettings.class.getCanonicalName(), R.string.wifi_configure_titlebar, -1, null);
                }
                return true;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        Preference preference = (Preference) view.getTag();
        if (!(preference instanceof LongPressAccessPointPreference)) {
            return;
        }
        this.mSelectedAccessPoint = ((LongPressAccessPointPreference) preference).getAccessPoint();
        menu.setHeaderTitle(this.mSelectedAccessPoint.getSsid());
        if (this.mSelectedAccessPoint.isConnectable()) {
            menu.add(0, 7, 0, R.string.wifi_menu_connect);
        }
        WifiConfiguration config = this.mSelectedAccessPoint.getConfig();
        if (isEditabilityLockedDown(getActivity(), config)) {
            return;
        }
        if (this.mSelectedAccessPoint.isSaved() || this.mSelectedAccessPoint.isEphemeral()) {
            menu.add(0, 8, 0, R.string.wifi_menu_forget);
        }
        this.mWifiSettingsExt.onCreateContextMenu(menu, this.mSelectedAccessPoint.getDetailedState(), this.mSelectedAccessPoint);
        if (!this.mSelectedAccessPoint.isSaved()) {
            return;
        }
        menu.add(0, 9, 0, R.string.wifi_menu_modify);
        try {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
            if (nfcAdapter == null || !nfcAdapter.isEnabled() || nfcAdapter.getModeFlag(1) != 1 || this.mSelectedAccessPoint.getSecurity() == 0) {
                return;
            }
            menu.add(0, 10, 0, R.string.wifi_menu_write_to_nfc);
        } catch (UnsupportedOperationException e) {
            Log.d("WifiSettings", "this device doesn't support NFC");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (this.mSelectedAccessPoint == null) {
            return super.onContextItemSelected(item);
        }
        switch (item.getItemId()) {
            case 7:
                if (this.mSelectedAccessPoint.isSaved()) {
                    connect(this.mSelectedAccessPoint.getConfig());
                } else if (this.mSelectedAccessPoint.getSecurity() == 0) {
                    this.mSelectedAccessPoint.generateOpenNetworkConfig();
                    connect(this.mSelectedAccessPoint.getConfig());
                } else {
                    showDialog(this.mSelectedAccessPoint, 1);
                }
                return true;
            case 8:
                forget();
                return true;
            case 9:
                showDialog(this.mSelectedAccessPoint, 2);
                return true;
            case 10:
                showDialog(6);
                return true;
            default:
                if (this.mWifiSettingsExt != null && this.mSelectedAccessPoint != null) {
                    return this.mWifiSettingsExt.onContextItemSelected(item, this.mSelectedAccessPoint.getConfig());
                }
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof LongPressAccessPointPreference) {
            this.mSelectedAccessPoint = ((LongPressAccessPointPreference) preference).getAccessPoint();
            if (this.mSelectedAccessPoint == null) {
                return false;
            }
            if (this.mSelectedAccessPoint.getSecurity() == 0 && !this.mSelectedAccessPoint.isSaved() && !this.mSelectedAccessPoint.isActive()) {
                this.mSelectedAccessPoint.generateOpenNetworkConfig();
                connect(this.mSelectedAccessPoint.getConfig());
            } else if (this.mSelectedAccessPoint.isSaved()) {
                showDialog(this.mSelectedAccessPoint, 0);
            } else {
                showDialog(this.mSelectedAccessPoint, 1);
            }
        } else if (preference == this.mAddPreference) {
            onAddNetworkPressed();
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    private void showDialog(AccessPoint accessPoint, int dialogMode) {
        if (accessPoint != null) {
            WifiConfiguration config = accessPoint.getConfig();
            if (isEditabilityLockedDown(getActivity(), config) && accessPoint.isActive()) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), RestrictedLockUtils.getDeviceOwner(getActivity()));
                return;
            }
        }
        if (this.mDialog != null) {
            removeDialog(1);
            this.mDialog = null;
        }
        this.mDlgAccessPoint = accessPoint;
        this.mDialogMode = dialogMode;
        showDialog(1);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DefaultWfcSettingsExt.PAUSE:
                AccessPoint ap = this.mDlgAccessPoint;
                if (ap == null && this.mAccessPointSavedState != null) {
                    ap = new AccessPoint(getActivity(), this.mAccessPointSavedState);
                    this.mDlgAccessPoint = ap;
                    this.mAccessPointSavedState = null;
                }
                this.mSelectedAccessPoint = ap;
                if (this.mSelectedAccessPoint != null) {
                    this.mWifiSettingsExt.recordPriority(this.mSelectedAccessPoint.getConfig());
                }
                this.mDialog = new WifiDialog(getActivity(), this, ap, this.mDialogMode, false);
                return this.mDialog;
            case DefaultWfcSettingsExt.CREATE:
                return new WpsDialog(getActivity(), 0);
            case DefaultWfcSettingsExt.DESTROY:
                return new WpsDialog(getActivity(), 1);
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
            case 5:
            default:
                return super.onCreateDialog(dialogId);
            case 6:
                if (this.mSelectedAccessPoint != null) {
                    this.mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(getActivity(), this.mSelectedAccessPoint.getConfig().networkId, this.mSelectedAccessPoint.getSecurity(), this.mWifiManager);
                } else if (this.mWifiNfcDialogSavedState != null) {
                    this.mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(getActivity(), this.mWifiNfcDialogSavedState, this.mWifiManager);
                }
                return this.mWifiToNfcDialog;
        }
    }

    @Override
    public void onAccessPointsChanged() {
        if (getActivity() == null) {
        }
        if (isUiRestricted()) {
            if (!isUiRestrictedByOnlyAdmin()) {
                addMessagePreference(R.string.wifi_empty_list_user_restricted);
            }
            getPreferenceScreen().removeAll();
            return;
        }
        int wifiState = this.mWifiManager.getWifiState();
        switch (wifiState) {
            case DefaultWfcSettingsExt.RESUME:
                addMessagePreference(R.string.wifi_stopping);
                setProgressBarVisible(true);
                break;
            case DefaultWfcSettingsExt.PAUSE:
                setOffMessage();
                setProgressBarVisible(false);
                if (this.mScanMenuItem != null) {
                    this.mScanMenuItem.setEnabled(false);
                }
                break;
            case DefaultWfcSettingsExt.CREATE:
                this.mWifiSettingsExt.emptyScreen(getPreferenceScreen());
                setProgressBarVisible(true);
                break;
            case DefaultWfcSettingsExt.DESTROY:
                Collection<AccessPoint> accessPoints = this.mWifiTracker.getAccessPoints();
                this.mWifiSettingsExt.emptyCategory(getPreferenceScreen());
                boolean hasAvailableAccessPoints = false;
                Log.d("WifiSettings", "accessPoints.size() = " + accessPoints.size());
                int index = 0;
                cacheRemoveAllPrefs(getPreferenceScreen());
                for (AccessPoint accessPoint : accessPoints) {
                    if (accessPoint.getLevel() != -1) {
                        String key = accessPoint.getBssid();
                        hasAvailableAccessPoints = true;
                        LongPressAccessPointPreference pref = (LongPressAccessPointPreference) getCachedPreference(key);
                        if (pref != null) {
                            int index2 = index + 1;
                            pref.setOrder(index);
                            this.mWifiSettingsExt.addPreference(getPreferenceScreen(), pref, accessPoint.getConfig() != null);
                            index = index2;
                        } else {
                            LongPressAccessPointPreference preference = new LongPressAccessPointPreference(accessPoint, getPrefContext(), this.mUserBadgeCache, false, this);
                            preference.setKey(key);
                            int index3 = index + 1;
                            preference.setOrder(index);
                            if (this.mOpenSsid != null && this.mOpenSsid.equals(accessPoint.getSsidStr()) && !accessPoint.isSaved() && accessPoint.getSecurity() != 0) {
                                onPreferenceTreeClick(preference);
                                this.mOpenSsid = null;
                            }
                            this.mWifiSettingsExt.addPreference(getPreferenceScreen(), preference, accessPoint.getConfig() != null);
                            accessPoint.setListener(this);
                            index = index3;
                        }
                    }
                }
                removeCachedPrefs(getPreferenceScreen());
                if (!hasAvailableAccessPoints) {
                    setProgressBarVisible(true);
                    Preference pref2 = new Preference(getContext()) {
                        @Override
                        public void onBindViewHolder(PreferenceViewHolder holder) {
                            super.onBindViewHolder(holder);
                            holder.setDividerAllowedBelow(true);
                        }
                    };
                    pref2.setSelectable(false);
                    pref2.setSummary(R.string.wifi_empty_list_wifi_on);
                    pref2.setOrder(0);
                    getPreferenceScreen().addPreference(pref2);
                    this.mAddPreference.setOrder(1);
                    getPreferenceScreen().addPreference(this.mAddPreference);
                } else {
                    int i = index + 1;
                    this.mAddPreference.setOrder(index);
                    getPreferenceScreen().addPreference(this.mAddPreference);
                    setProgressBarVisible(false);
                }
                if (this.mScanMenuItem != null) {
                    this.mScanMenuItem.setEnabled(true);
                }
                this.mWifiSettingsExt.refreshCategory(getPreferenceScreen());
                break;
        }
    }

    private void setOffMessage() {
        if (isUiRestricted()) {
            if (!isUiRestrictedByOnlyAdmin()) {
                addMessagePreference(R.string.wifi_empty_list_user_restricted);
            }
            getPreferenceScreen().removeAll();
            return;
        }
        TextView emptyTextView = getEmptyTextView();
        if (emptyTextView == null) {
            return;
        }
        CharSequence briefText = getText(R.string.wifi_empty_list_wifi_off);
        ContentResolver resolver = getActivity().getContentResolver();
        boolean wifiScanningMode = Settings.Global.getInt(resolver, "wifi_scan_always_enabled", 0) == 1;
        if (!wifiScanningMode) {
            emptyTextView.setText(briefText, TextView.BufferType.SPANNABLE);
        } else {
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(briefText);
            contentBuilder.append("\n\n");
            contentBuilder.append(getText(R.string.wifi_scan_notify_text));
            LinkifyUtils.linkify(emptyTextView, contentBuilder, new LinkifyUtils.OnClickListener() {
                @Override
                public void onClick() {
                    SettingsActivity activity = (SettingsActivity) WifiSettings.this.getActivity();
                    activity.startPreferencePanel(ScanningSettings.class.getName(), null, R.string.location_scanning_screen_title, null, null, 0);
                }
            });
        }
        Spannable boldSpan = (Spannable) emptyTextView.getText();
        boldSpan.setSpan(new TextAppearanceSpan(getActivity(), android.R.style.TextAppearance.Medium), 0, briefText.length(), 33);
        this.mWifiSettingsExt.emptyScreen(getPreferenceScreen());
    }

    private void addMessagePreference(int messageId) {
        TextView emptyTextView = getEmptyTextView();
        if (emptyTextView != null) {
            emptyTextView.setText(messageId);
        }
        this.mWifiSettingsExt.emptyScreen(getPreferenceScreen());
    }

    protected void setProgressBarVisible(boolean visible) {
        if (this.mProgressHeader == null) {
            return;
        }
        this.mProgressHeader.setVisibility(visible ? 0 : 8);
    }

    @Override
    public void onWifiStateChanged(int state) {
        if (getActivity() == null) {
        }
        switch (state) {
            case DefaultWfcSettingsExt.PAUSE:
                setOffMessage();
                setProgressBarVisible(false);
                break;
            case DefaultWfcSettingsExt.CREATE:
                addMessagePreference(R.string.wifi_starting);
                setProgressBarVisible(true);
                break;
        }
    }

    @Override
    public void onConnectedChanged() {
        if (this.mWifiTracker.isConnected()) {
            this.mWifiSettingsExt.updatePriority();
        }
        changeNextButtonState(this.mWifiTracker.isConnected());
    }

    private void changeNextButtonState(boolean enabled) {
        if (!this.mEnableNextOnConnection || !hasNextButton()) {
            return;
        }
        getNextButton().setEnabled(enabled);
    }

    @Override
    public void onForget(WifiDialog dialog) {
        forget();
    }

    @Override
    public void onSubmit(WifiDialog dialog) {
        if (this.mDialog == null) {
            return;
        }
        submit(this.mDialog.getController());
    }

    void submit(WifiConfigController configController) {
        WifiConfiguration config = configController.getConfig();
        Log.d("WifiSettings", "submit, config = " + config);
        if (this.mSelectedAccessPoint != null) {
            this.mWifiSettingsExt.submit(config, this.mSelectedAccessPoint, this.mSelectedAccessPoint.getNetworkInfo() != null ? this.mSelectedAccessPoint.getNetworkInfo().getDetailedState() : null);
        }
        if (config == null) {
            if (this.mSelectedAccessPoint != null && this.mSelectedAccessPoint.isSaved()) {
                connect(this.mSelectedAccessPoint.getConfig());
            }
        } else if (configController.getMode() == 2) {
            this.mWifiManager.save(config, this.mSaveListener);
        } else {
            this.mWifiManager.save(config, this.mSaveListener);
            if (this.mSelectedAccessPoint != null) {
                connect(config);
            }
        }
        this.mWifiTracker.resumeScanning();
    }

    void forget() {
        MetricsLogger.action(getActivity(), 137);
        if (!this.mSelectedAccessPoint.isSaved()) {
            if (this.mSelectedAccessPoint.getNetworkInfo() != null && this.mSelectedAccessPoint.getNetworkInfo().getState() != NetworkInfo.State.DISCONNECTED) {
                this.mWifiManager.disableEphemeralNetwork(AccessPoint.convertToQuotedString(this.mSelectedAccessPoint.getSsidStr()));
            } else {
                Log.e("WifiSettings", "Failed to forget invalid network " + this.mSelectedAccessPoint.getConfig());
                return;
            }
        } else {
            this.mWifiManager.forget(this.mSelectedAccessPoint.getConfig().networkId, this.mForgetListener);
        }
        this.mWifiTracker.resumeScanning();
        changeNextButtonState(false);
        this.mWifiSettingsExt.updatePriority();
    }

    protected void connect(WifiConfiguration config) {
        MetricsLogger.action(getActivity(), 135);
        this.mWifiManager.connect(config, this.mConnectListener);
    }

    void onAddNetworkPressed() {
        MetricsLogger.action(getActivity(), 134);
        this.mSelectedAccessPoint = null;
        showDialog(null, 1);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_wifi;
    }

    @Override
    public void onAccessPointChanged(AccessPoint accessPoint) {
        ((LongPressAccessPointPreference) accessPoint.getTag()).refresh();
    }

    @Override
    public void onLevelChanged(AccessPoint accessPoint) {
        ((LongPressAccessPointPreference) accessPoint.getTag()).onLevelChanged();
    }

    static boolean isEditabilityLockedDown(Context context, WifiConfiguration config) {
        return !canModifyNetwork(context, config);
    }

    static boolean canModifyNetwork(Context context, WifiConfiguration config) {
        ComponentName deviceOwner;
        if (config == null) {
            return true;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature("android.software.device_admin") && dpm == null) {
            return false;
        }
        boolean isConfigEligibleForLockdown = false;
        if (dpm != null && (deviceOwner = dpm.getDeviceOwnerComponentOnAnyUser()) != null) {
            int deviceOwnerUserId = dpm.getDeviceOwnerUserId();
            try {
                int deviceOwnerUid = pm.getPackageUidAsUser(deviceOwner.getPackageName(), deviceOwnerUserId);
                isConfigEligibleForLockdown = deviceOwnerUid == config.creatorUid;
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        if (!isConfigEligibleForLockdown) {
            return true;
        }
        ContentResolver resolver = context.getContentResolver();
        boolean isLockdownFeatureEnabled = Settings.Global.getInt(resolver, "wifi_device_owner_configs_lockdown", 0) != 0;
        return !isLockdownFeatureEnabled;
    }

    private static class SummaryProvider extends BroadcastReceiver implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;
        private final WifiManager mWifiManager;
        private final WifiStatusTracker mWifiTracker;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
            this.mWifiManager = (WifiManager) context.getSystemService(WifiManager.class);
            this.mWifiTracker = new WifiStatusTracker(this.mWifiManager);
        }

        private CharSequence getSummary() {
            if (!this.mWifiTracker.enabled) {
                return this.mContext.getString(R.string.wifi_disabled_generic);
            }
            if (!this.mWifiTracker.connected) {
                return this.mContext.getString(R.string.disconnected);
            }
            return this.mWifiTracker.ssid;
        }

        @Override
        public void setListening(boolean listening) {
            if (!listening) {
                return;
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            filter.addAction("android.net.wifi.STATE_CHANGE");
            filter.addAction("android.net.wifi.RSSI_CHANGED");
            this.mSummaryLoader.registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.mWifiTracker.handleBroadcast(intent);
            this.mSummaryLoader.setSummary(this, getSummary());
        }
    }
}
