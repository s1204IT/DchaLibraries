package com.android.settings.wifi;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.android.settings.LinkifyUtils;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.location.ScanningSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SummaryUpdater;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.wifi.ConnectedAccessPointPreference;
import com.android.settings.wifi.WifiDialog;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWifiSettingsExt;
import java.util.ArrayList;
import java.util.List;

public class WifiSettings extends RestrictedSettingsFragment implements Indexable, WifiDialog.WifiDialogListener, AccessPoint.AccessPointListener, WifiTracker.WifiListener {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            Resources resources = context.getResources();
            if (resources.getBoolean(R.bool.config_show_wifi_settings)) {
                SearchIndexableRaw searchIndexableRaw = new SearchIndexableRaw(context);
                searchIndexableRaw.title = resources.getString(R.string.wifi_settings);
                searchIndexableRaw.screenTitle = resources.getString(R.string.wifi_settings);
                searchIndexableRaw.keywords = resources.getString(R.string.keywords_wifi);
                searchIndexableRaw.key = "main_toggle_wifi";
                arrayList.add(searchIndexableRaw);
            }
            return arrayList;
        }
    };
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    private Bundle mAccessPointSavedState;
    private PreferenceCategory mAccessPointsPreferenceCategory;
    private Preference mAddPreference;
    private PreferenceCategory mAdditionalSettingsPreferenceCategory;
    private CaptivePortalNetworkCallback mCaptivePortalNetworkCallback;
    private boolean mClickedConnect;
    private Preference mConfigureWifiSettingsPreference;
    private WifiManager.ActionListener mConnectListener;
    private PreferenceCategory mConnectedAccessPointPreferenceCategory;
    private ConnectivityManager mConnectivityManager;
    private WifiDialog mDialog;
    private int mDialogMode;
    private AccessPoint mDlgAccessPoint;
    private boolean mEnableNextOnConnection;
    private WifiManager.ActionListener mForgetListener;
    private final Runnable mHideProgressBarRunnable;
    private boolean mIsRestricted;
    private String mOpenSsid;
    private View mProgressHeader;
    private WifiManager.ActionListener mSaveListener;
    private Preference mSavedNetworksPreference;
    private AccessPoint mSelectedAccessPoint;
    private LinkablePreference mStatusMessagePreference;
    private final Runnable mUpdateAccessPointsRunnable;
    private AccessPointPreference.UserBadgeCache mUserBadgeCache;
    private WifiEnabler mWifiEnabler;
    protected WifiManager mWifiManager;
    private Bundle mWifiNfcDialogSavedState;
    private IWifiSettingsExt mWifiSettingsExt;
    private WriteWifiConfigToNfcDialog mWifiToNfcDialog;
    private WifiTracker mWifiTracker;

    private static boolean isVerboseLoggingEnabled() {
        return WifiTracker.sVerboseLogging || Log.isLoggable("WifiSettings", 2);
    }

    public WifiSettings() {
        super("no_config_wifi");
        this.mUpdateAccessPointsRunnable = new Runnable() {
            @Override
            public final void run() {
                this.f$0.updateAccessPointPreferences();
            }
        };
        this.mHideProgressBarRunnable = new Runnable() {
            @Override
            public final void run() {
                this.f$0.setProgressBarVisible(false);
            }
        };
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        Activity activity = getActivity();
        if (activity != null) {
            this.mProgressHeader = setPinnedHeaderView(R.layout.wifi_progress_header).findViewById(R.id.progress_bar_animation);
            setProgressBarVisible(false);
            if (activity != null) {
                SettingsActivity settingsActivity = (SettingsActivity) activity;
                if (settingsActivity.getSwitchBar() != null) {
                    settingsActivity.getSwitchBar().setSwitchBarText(R.string.wifi_settings_master_switch_title, R.string.wifi_settings_master_switch_title);
                    return;
                }
            }
            Log.w("WifiSettings", "SettingsActivity or switchbar doesn't exist, returning");
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mWifiSettingsExt = UtilsExt.getWifiSettingsExt(getPrefContext());
        setAnimationAllowed(false);
        addPreferences();
        this.mIsRestricted = isUiRestricted();
    }

    private void addPreferences() {
        addPreferencesFromResource(R.xml.wifi_settings);
        this.mConnectedAccessPointPreferenceCategory = (PreferenceCategory) findPreference("connected_access_point");
        this.mAccessPointsPreferenceCategory = (PreferenceCategory) findPreference("access_points");
        this.mAdditionalSettingsPreferenceCategory = (PreferenceCategory) findPreference("additional_settings");
        this.mConfigureWifiSettingsPreference = findPreference("configure_settings");
        this.mSavedNetworksPreference = findPreference("saved_networks");
        Context prefContext = getPrefContext();
        this.mAddPreference = new Preference(prefContext);
        this.mAddPreference.setIcon(R.drawable.ic_menu_add_inset);
        this.mAddPreference.setTitle(R.string.wifi_add_network);
        this.mStatusMessagePreference = new LinkablePreference(prefContext);
        this.mUserBadgeCache = new AccessPointPreference.UserBadgeCache(getPackageManager());
        this.mWifiSettingsExt.init(getPreferenceScreen());
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        ConnectivityManager connectivityManager;
        super.onActivityCreated(bundle);
        this.mWifiTracker = WifiTrackerFactory.create(getActivity(), this, getLifecycle(), true, true);
        this.mWifiManager = this.mWifiTracker.getManager();
        if (getActivity() != null) {
            this.mConnectivityManager = (ConnectivityManager) getActivity().getSystemService(ConnectivityManager.class);
        }
        this.mConnectListener = new WifiManager.ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int i) {
                Activity activity = WifiSettings.this.getActivity();
                if (activity != null) {
                    Toast.makeText(activity, R.string.wifi_failed_connect_message, 0).show();
                }
            }
        };
        this.mSaveListener = new WifiManager.ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int i) {
                Activity activity = WifiSettings.this.getActivity();
                if (activity != null) {
                    Toast.makeText(activity, R.string.wifi_failed_save_message, 0).show();
                }
            }
        };
        this.mForgetListener = new WifiManager.ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int i) {
                Activity activity = WifiSettings.this.getActivity();
                if (activity != null) {
                    Toast.makeText(activity, R.string.wifi_failed_forget_message, 0).show();
                }
            }
        };
        if (bundle != null) {
            this.mDialogMode = bundle.getInt("dialog_mode");
            if (bundle.containsKey("wifi_ap_state")) {
                this.mAccessPointSavedState = bundle.getBundle("wifi_ap_state");
            }
            if (bundle.containsKey("wifi_nfc_dlg_state")) {
                this.mWifiNfcDialogSavedState = bundle.getBundle("wifi_nfc_dlg_state");
            }
        }
        Intent intent = getActivity().getIntent();
        this.mEnableNextOnConnection = intent.getBooleanExtra("wifi_enable_next_on_connect", false);
        if (this.mEnableNextOnConnection && hasNextButton() && (connectivityManager = (ConnectivityManager) getActivity().getSystemService("connectivity")) != null) {
            changeNextButtonState(connectivityManager.getNetworkInfo(1).isConnected());
        }
        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
        if (intent.hasExtra("wifi_start_connect_ssid")) {
            this.mOpenSsid = intent.getStringExtra("wifi_start_connect_ssid");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.teardownSwitchController();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mWifiEnabler = createWifiEnabler();
        if (this.mIsRestricted) {
            restrictUi();
        } else {
            onWifiStateChanged(this.mWifiManager.getWifiState());
        }
    }

    private void restrictUi() {
        if (!isUiRestrictedByOnlyAdmin()) {
            getEmptyTextView().setText(R.string.wifi_empty_list_user_restricted);
        }
        getPreferenceScreen().removeAll();
    }

    private WifiEnabler createWifiEnabler() {
        SettingsActivity settingsActivity = (SettingsActivity) getActivity();
        return new WifiEnabler(settingsActivity, new SwitchBarController(settingsActivity.getSwitchBar()), this.mMetricsFeatureProvider);
    }

    @Override
    public void onResume() {
        Activity activity = getActivity();
        super.onResume();
        boolean z = this.mIsRestricted;
        this.mIsRestricted = isUiRestricted();
        if (!z && this.mIsRestricted) {
            restrictUi();
        }
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.resume(activity);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.pause();
        }
    }

    @Override
    public void onStop() {
        getView().removeCallbacks(this.mUpdateAccessPointsRunnable);
        getView().removeCallbacks(this.mHideProgressBarRunnable);
        unregisterCaptivePortalNetworkCallback();
        super.onStop();
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        boolean z = this.mIsRestricted;
        this.mIsRestricted = isUiRestricted();
        if (z && !this.mIsRestricted && getPreferenceScreen().getPreferenceCount() == 0) {
            addPreferences();
        }
    }

    @Override
    public int getMetricsCategory() {
        return 103;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mDialog != null && this.mDialog.isShowing()) {
            bundle.putInt("dialog_mode", this.mDialogMode);
            if (this.mDlgAccessPoint != null) {
                this.mAccessPointSavedState = new Bundle();
                this.mDlgAccessPoint.saveWifiState(this.mAccessPointSavedState);
                bundle.putBundle("wifi_ap_state", this.mAccessPointSavedState);
            }
        }
        if (this.mWifiToNfcDialog != null && this.mWifiToNfcDialog.isShowing()) {
            Bundle bundle2 = new Bundle();
            this.mWifiToNfcDialog.saveState(bundle2);
            bundle.putBundle("wifi_nfc_dlg_state", bundle2);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        Preference preference = (Preference) view.getTag();
        if (preference instanceof LongPressAccessPointPreference) {
            this.mSelectedAccessPoint = ((LongPressAccessPointPreference) preference).getAccessPoint();
            contextMenu.setHeaderTitle(this.mSelectedAccessPoint.getSsid());
            if (this.mSelectedAccessPoint.isConnectable()) {
                contextMenu.add(0, 7, 0, R.string.wifi_menu_connect);
            }
            if (WifiUtils.isNetworkLockedDown(getActivity(), this.mSelectedAccessPoint.getConfig())) {
                return;
            }
            if (this.mSelectedAccessPoint.isSaved() || this.mSelectedAccessPoint.isEphemeral()) {
                contextMenu.add(0, 8, 0, R.string.wifi_menu_forget);
            }
            this.mWifiSettingsExt.updateContextMenu(contextMenu, 101, this.mSelectedAccessPoint.getDetailedState());
            if (this.mSelectedAccessPoint.isSaved()) {
                contextMenu.add(0, 9, 0, R.string.wifi_menu_modify);
                NfcAdapter defaultAdapter = NfcAdapter.getDefaultAdapter(getActivity());
                if (defaultAdapter != null && defaultAdapter.isEnabled() && this.mSelectedAccessPoint.getSecurity() != 0) {
                    contextMenu.add(0, 10, 0, R.string.wifi_menu_write_to_nfc);
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        if (this.mSelectedAccessPoint == null) {
            return super.onContextItemSelected(menuItem);
        }
        switch (menuItem.getItemId()) {
            case 7:
                boolean zIsSaved = this.mSelectedAccessPoint.isSaved();
                if (zIsSaved) {
                    connect(this.mSelectedAccessPoint.getConfig(), zIsSaved);
                } else if (this.mSelectedAccessPoint.getSecurity() == 0) {
                    this.mSelectedAccessPoint.generateOpenNetworkConfig();
                    connect(this.mSelectedAccessPoint.getConfig(), zIsSaved);
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
            case AccessPoint.Speed.MODERATE:
                showDialog(6);
                return true;
            default:
                if (this.mWifiSettingsExt != null && this.mSelectedAccessPoint != null) {
                    return this.mWifiSettingsExt.disconnect(menuItem, this.mSelectedAccessPoint.getConfig());
                }
                return super.onContextItemSelected(menuItem);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getFragment() != null) {
            preference.setOnPreferenceClickListener(null);
            return super.onPreferenceTreeClick(preference);
        }
        if (this.mWifiSettingsExt.customRefreshButtonClick(preference)) {
            return true;
        }
        if (preference instanceof LongPressAccessPointPreference) {
            this.mSelectedAccessPoint = ((LongPressAccessPointPreference) preference).getAccessPoint();
            if (this.mSelectedAccessPoint == null) {
                return false;
            }
            if (this.mSelectedAccessPoint.isActive()) {
                return super.onPreferenceTreeClick(preference);
            }
            WifiConfiguration config = this.mSelectedAccessPoint.getConfig();
            if (this.mSelectedAccessPoint.getSecurity() == 0) {
                this.mSelectedAccessPoint.generateOpenNetworkConfig();
                connect(this.mSelectedAccessPoint.getConfig(), this.mSelectedAccessPoint.isSaved());
            } else if ((this.mSelectedAccessPoint.isSaved() && config != null && config.getNetworkSelectionStatus() != null && config.getNetworkSelectionStatus().getHasEverConnected()) || this.mSelectedAccessPoint.isPasspoint()) {
                connect(config, true);
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

    private void showDialog(AccessPoint accessPoint, int i) {
        if (accessPoint != null) {
            if (WifiUtils.isNetworkLockedDown(getActivity(), accessPoint.getConfig()) && accessPoint.isActive()) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), RestrictedLockUtils.getDeviceOwner(getActivity()));
                return;
            }
        }
        if (this.mDialog != null) {
            removeDialog(1);
            this.mDialog = null;
        }
        this.mDlgAccessPoint = accessPoint;
        this.mDialogMode = i;
        showDialog(1);
    }

    @Override
    public Dialog onCreateDialog(int i) {
        if (i != 1) {
            if (i == 6) {
                if (this.mSelectedAccessPoint != null) {
                    this.mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(getActivity(), this.mSelectedAccessPoint.getSecurity());
                } else if (this.mWifiNfcDialogSavedState != null) {
                    this.mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(getActivity(), this.mWifiNfcDialogSavedState);
                }
                return this.mWifiToNfcDialog;
            }
            return super.onCreateDialog(i);
        }
        if (this.mDlgAccessPoint == null && this.mAccessPointSavedState == null) {
            this.mDialog = WifiDialog.createFullscreen(getActivity(), this, this.mDlgAccessPoint, this.mDialogMode);
        } else {
            if (this.mDlgAccessPoint == null) {
                this.mDlgAccessPoint = new AccessPoint(getActivity(), this.mAccessPointSavedState);
                this.mAccessPointSavedState = null;
            }
            AccessPoint accessPoint = this.mSelectedAccessPoint;
            this.mDialog = WifiDialog.createModal(getActivity(), this, this.mDlgAccessPoint, this.mDialogMode);
        }
        this.mSelectedAccessPoint = this.mDlgAccessPoint;
        return this.mDialog;
    }

    @Override
    public int getDialogMetricsCategory(int i) {
        if (i == 1) {
            return 603;
        }
        if (i == 6) {
            return 606;
        }
        return 0;
    }

    @Override
    public void onAccessPointsChanged() {
        Log.d("WifiSettings", "onAccessPointsChanged (WifiTracker) callback initiated");
        updateAccessPointsDelayed();
    }

    private void updateAccessPointsDelayed() {
        if (getActivity() != null && !this.mIsRestricted && this.mWifiManager.isWifiEnabled()) {
            View view = getView();
            Handler handler = view.getHandler();
            if (handler != null && handler.hasCallbacks(this.mUpdateAccessPointsRunnable)) {
                return;
            }
            setProgressBarVisible(true);
            view.postDelayed(this.mUpdateAccessPointsRunnable, 300L);
        }
    }

    @Override
    public void onWifiStateChanged(int i) {
        if (this.mIsRestricted) {
        }
        switch (this.mWifiManager.getWifiState()) {
            case 0:
                removeConnectedAccessPointPreference();
                this.mAccessPointsPreferenceCategory.removeAll();
                this.mWifiSettingsExt.emptyCategory(getPreferenceScreen());
                addMessagePreference(R.string.wifi_stopping);
                break;
            case 1:
                this.mWifiSettingsExt.emptyCategory(getPreferenceScreen());
                setOffMessage();
                setAdditionalSettingsSummaries();
                setProgressBarVisible(false);
                break;
            case 2:
                removeConnectedAccessPointPreference();
                this.mAccessPointsPreferenceCategory.removeAll();
                addMessagePreference(R.string.wifi_starting);
                this.mWifiSettingsExt.emptyCategory(getPreferenceScreen());
                setProgressBarVisible(true);
                break;
            case 3:
                updateAccessPointPreferences();
                break;
        }
    }

    @Override
    public void onConnectedChanged() {
        this.mWifiTracker.isConnected();
        changeNextButtonState(this.mWifiTracker.isConnected());
    }

    private static boolean isDisabledByWrongPassword(AccessPoint accessPoint) {
        WifiConfiguration.NetworkSelectionStatus networkSelectionStatus;
        WifiConfiguration config = accessPoint.getConfig();
        return (config == null || (networkSelectionStatus = config.getNetworkSelectionStatus()) == null || networkSelectionStatus.isNetworkEnabled() || 13 != networkSelectionStatus.getNetworkSelectionDisableReason()) ? false : true;
    }

    private void updateAccessPointPreferences() {
        if (!this.mWifiManager.isWifiEnabled()) {
            return;
        }
        this.mWifiSettingsExt.emptyCategory(getPreferenceScreen());
        List<AccessPoint> accessPoints = this.mWifiTracker.getAccessPoints();
        if (isVerboseLoggingEnabled()) {
            Log.i("WifiSettings", "updateAccessPoints called for: " + accessPoints);
        }
        this.mAccessPointsPreferenceCategory.removePreference(this.mStatusMessagePreference);
        cacheRemoveAllPrefs(this.mAccessPointsPreferenceCategory);
        boolean zConfigureConnectedAccessPointPreferenceCategory = configureConnectedAccessPointPreferenceCategory(accessPoints);
        int size = accessPoints.size();
        boolean z = false;
        int i = zConfigureConnectedAccessPointPreferenceCategory;
        while (i < size) {
            AccessPoint accessPoint = accessPoints.get(i);
            if (accessPoint.isReachable()) {
                String key = accessPoint.getKey();
                LongPressAccessPointPreference longPressAccessPointPreference = (LongPressAccessPointPreference) getCachedPreference(key);
                if (longPressAccessPointPreference != null) {
                    longPressAccessPointPreference.setOrder(i);
                } else {
                    LongPressAccessPointPreference longPressAccessPointPreferenceCreateLongPressAccessPointPreference = createLongPressAccessPointPreference(accessPoint);
                    longPressAccessPointPreferenceCreateLongPressAccessPointPreference.setKey(key);
                    longPressAccessPointPreferenceCreateLongPressAccessPointPreference.setOrder(i);
                    if (this.mOpenSsid != null && this.mOpenSsid.equals(accessPoint.getSsidStr()) && accessPoint.getSecurity() != 0 && (!accessPoint.isSaved() || isDisabledByWrongPassword(accessPoint))) {
                        onPreferenceTreeClick(longPressAccessPointPreferenceCreateLongPressAccessPointPreference);
                        this.mOpenSsid = null;
                    }
                    this.mWifiSettingsExt.addPreference(getPreferenceScreen(), this.mAccessPointsPreferenceCategory, longPressAccessPointPreferenceCreateLongPressAccessPointPreference, accessPoint.getConfig() != null);
                    accessPoint.setListener(this);
                    longPressAccessPointPreferenceCreateLongPressAccessPointPreference.refresh();
                }
                z = true;
            }
            i++;
        }
        removeCachedPrefs(this.mAccessPointsPreferenceCategory);
        this.mAddPreference.setOrder(i);
        this.mWifiSettingsExt.addPreference(getPreferenceScreen(), this.mAccessPointsPreferenceCategory, this.mAddPreference, false);
        setAdditionalSettingsSummaries();
        if (!z) {
            setProgressBarVisible(true);
            Preference preference = new Preference(getPrefContext());
            preference.setSelectable(false);
            preference.setSummary(R.string.wifi_empty_list_wifi_on);
            preference.setOrder(i);
            preference.setKey("wifi_empty_list");
            this.mWifiSettingsExt.addPreference(getPreferenceScreen(), this.mAccessPointsPreferenceCategory, preference, false);
            return;
        }
        getView().postDelayed(this.mHideProgressBarRunnable, 1700L);
    }

    private LongPressAccessPointPreference createLongPressAccessPointPreference(AccessPoint accessPoint) {
        return new LongPressAccessPointPreference(accessPoint, getPrefContext(), this.mUserBadgeCache, false, R.drawable.ic_wifi_signal_0, this);
    }

    private ConnectedAccessPointPreference createConnectedAccessPointPreference(AccessPoint accessPoint) {
        return new ConnectedAccessPointPreference(accessPoint, getPrefContext(), this.mUserBadgeCache, R.drawable.ic_wifi_signal_0, false);
    }

    private boolean configureConnectedAccessPointPreferenceCategory(List<AccessPoint> list) {
        if (this.mWifiSettingsExt.removeConnectedAccessPointPreference()) {
            return false;
        }
        if (list.size() == 0) {
            removeConnectedAccessPointPreference();
            return false;
        }
        AccessPoint accessPoint = list.get(0);
        if (!accessPoint.isActive()) {
            removeConnectedAccessPointPreference();
            return false;
        }
        if (this.mConnectedAccessPointPreferenceCategory.getPreferenceCount() == 0) {
            addConnectedAccessPointPreference(accessPoint);
            return true;
        }
        ConnectedAccessPointPreference connectedAccessPointPreference = (ConnectedAccessPointPreference) this.mConnectedAccessPointPreferenceCategory.getPreference(0);
        if (connectedAccessPointPreference.getAccessPoint() != accessPoint) {
            removeConnectedAccessPointPreference();
            addConnectedAccessPointPreference(accessPoint);
            return true;
        }
        connectedAccessPointPreference.refresh();
        registerCaptivePortalNetworkCallback(getCurrentWifiNetwork(), connectedAccessPointPreference);
        return true;
    }

    private void addConnectedAccessPointPreference(AccessPoint accessPoint) {
        final ConnectedAccessPointPreference connectedAccessPointPreferenceCreateConnectedAccessPointPreference = createConnectedAccessPointPreference(accessPoint);
        registerCaptivePortalNetworkCallback(getCurrentWifiNetwork(), connectedAccessPointPreferenceCreateConnectedAccessPointPreference);
        connectedAccessPointPreferenceCreateConnectedAccessPointPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public final boolean onPreferenceClick(Preference preference) {
                return WifiSettings.lambda$addConnectedAccessPointPreference$2(this.f$0, connectedAccessPointPreferenceCreateConnectedAccessPointPreference, preference);
            }
        });
        connectedAccessPointPreferenceCreateConnectedAccessPointPreference.setOnGearClickListener(new ConnectedAccessPointPreference.OnGearClickListener() {
            @Override
            public final void onGearClick(ConnectedAccessPointPreference connectedAccessPointPreference) {
                WifiSettings.lambda$addConnectedAccessPointPreference$3(this.f$0, connectedAccessPointPreferenceCreateConnectedAccessPointPreference, connectedAccessPointPreference);
            }
        });
        connectedAccessPointPreferenceCreateConnectedAccessPointPreference.refresh();
        this.mConnectedAccessPointPreferenceCategory.addPreference(connectedAccessPointPreferenceCreateConnectedAccessPointPreference);
        this.mConnectedAccessPointPreferenceCategory.setVisible(true);
        if (this.mClickedConnect) {
            this.mClickedConnect = false;
            scrollToPreference(this.mConnectedAccessPointPreferenceCategory);
        }
    }

    public static boolean lambda$addConnectedAccessPointPreference$2(WifiSettings wifiSettings, ConnectedAccessPointPreference connectedAccessPointPreference, Preference preference) {
        connectedAccessPointPreference.getAccessPoint().saveWifiState(connectedAccessPointPreference.getExtras());
        if (wifiSettings.mCaptivePortalNetworkCallback != null && wifiSettings.mCaptivePortalNetworkCallback.isCaptivePortal()) {
            wifiSettings.mConnectivityManager.startCaptivePortalApp(wifiSettings.mCaptivePortalNetworkCallback.getNetwork());
            return true;
        }
        wifiSettings.launchNetworkDetailsFragment(connectedAccessPointPreference);
        return true;
    }

    public static void lambda$addConnectedAccessPointPreference$3(WifiSettings wifiSettings, ConnectedAccessPointPreference connectedAccessPointPreference, ConnectedAccessPointPreference connectedAccessPointPreference2) {
        connectedAccessPointPreference.getAccessPoint().saveWifiState(connectedAccessPointPreference.getExtras());
        wifiSettings.launchNetworkDetailsFragment(connectedAccessPointPreference);
    }

    private void registerCaptivePortalNetworkCallback(Network network, ConnectedAccessPointPreference connectedAccessPointPreference) {
        if (network == null || connectedAccessPointPreference == null) {
            Log.w("WifiSettings", "Network or Preference were null when registering callback.");
        } else {
            if (this.mCaptivePortalNetworkCallback != null && this.mCaptivePortalNetworkCallback.isSameNetworkAndPreference(network, connectedAccessPointPreference)) {
                return;
            }
            unregisterCaptivePortalNetworkCallback();
            this.mCaptivePortalNetworkCallback = new CaptivePortalNetworkCallback(network, connectedAccessPointPreference);
            this.mConnectivityManager.registerNetworkCallback(new NetworkRequest.Builder().clearCapabilities().addTransportType(1).build(), this.mCaptivePortalNetworkCallback, new Handler(Looper.getMainLooper()));
        }
    }

    private void unregisterCaptivePortalNetworkCallback() {
        if (this.mCaptivePortalNetworkCallback != null) {
            try {
                this.mConnectivityManager.unregisterNetworkCallback(this.mCaptivePortalNetworkCallback);
            } catch (RuntimeException e) {
                Log.e("WifiSettings", "Unregistering CaptivePortalNetworkCallback failed.", e);
            }
            this.mCaptivePortalNetworkCallback = null;
        }
    }

    private void launchNetworkDetailsFragment(ConnectedAccessPointPreference connectedAccessPointPreference) {
        new SubSettingLauncher(getContext()).setTitle(R.string.pref_title_network_details).setDestination(WifiNetworkDetailsFragment.class.getName()).setArguments(connectedAccessPointPreference.getExtras()).setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    private Network getCurrentWifiNetwork() {
        if (this.mWifiManager != null) {
            return this.mWifiManager.getCurrentNetwork();
        }
        return null;
    }

    private void removeConnectedAccessPointPreference() {
        this.mConnectedAccessPointPreferenceCategory.removeAll();
        this.mConnectedAccessPointPreferenceCategory.setVisible(false);
        unregisterCaptivePortalNetworkCallback();
    }

    private void setAdditionalSettingsSummaries() {
        int i;
        this.mWifiSettingsExt.addRefreshPreference(getPreferenceScreen(), this.mWifiTracker, this.mIsRestricted);
        this.mAdditionalSettingsPreferenceCategory.addPreference(this.mConfigureWifiSettingsPreference);
        Preference preference = this.mConfigureWifiSettingsPreference;
        if (isWifiWakeupEnabled()) {
            i = R.string.wifi_configure_settings_preference_summary_wakeup_on;
        } else {
            i = R.string.wifi_configure_settings_preference_summary_wakeup_off;
        }
        preference.setSummary(getString(i));
        int numSavedNetworks = this.mWifiTracker.getNumSavedNetworks();
        if (numSavedNetworks > 0) {
            this.mAdditionalSettingsPreferenceCategory.addPreference(this.mSavedNetworksPreference);
            this.mSavedNetworksPreference.setSummary(getResources().getQuantityString(R.plurals.wifi_saved_access_points_summary, numSavedNetworks, Integer.valueOf(numSavedNetworks)));
        } else {
            this.mAdditionalSettingsPreferenceCategory.removePreference(this.mSavedNetworksPreference);
        }
    }

    private boolean isWifiWakeupEnabled() {
        PowerManager powerManager = (PowerManager) getSystemService("power");
        ContentResolver contentResolver = getContentResolver();
        return Settings.Global.getInt(contentResolver, "wifi_wakeup_enabled", 0) == 1 && Settings.Global.getInt(contentResolver, "wifi_scan_always_enabled", 0) == 1 && Settings.Global.getInt(contentResolver, "airplane_mode_on", 0) == 0 && !powerManager.isPowerSaveMode();
    }

    private void setOffMessage() {
        this.mStatusMessagePreference.setText(getText(R.string.wifi_empty_list_wifi_off), Settings.Global.getInt(getActivity().getContentResolver(), "wifi_scan_always_enabled", 0) == 1 ? getText(R.string.wifi_scan_notify_text) : getText(R.string.wifi_scan_notify_text_scanning_off), new LinkifyUtils.OnClickListener() {
            @Override
            public final void onClick() {
                WifiSettings wifiSettings = this.f$0;
                new SubSettingLauncher(wifiSettings.getContext()).setDestination(ScanningSettings.class.getName()).setTitle(R.string.location_scanning_screen_title).setSourceMetricsCategory(wifiSettings.getMetricsCategory()).launch();
            }
        });
        removeConnectedAccessPointPreference();
        this.mAccessPointsPreferenceCategory.removeAll();
        this.mWifiSettingsExt.addPreference(getPreferenceScreen(), this.mAccessPointsPreferenceCategory, this.mStatusMessagePreference, false);
    }

    private void addMessagePreference(int i) {
        this.mStatusMessagePreference.setTitle(i);
        removeConnectedAccessPointPreference();
        this.mAccessPointsPreferenceCategory.removeAll();
        this.mWifiSettingsExt.addPreference(getPreferenceScreen(), this.mAccessPointsPreferenceCategory, this.mStatusMessagePreference, false);
    }

    protected void setProgressBarVisible(boolean z) {
        if (this.mProgressHeader != null) {
            this.mProgressHeader.setVisibility(z ? 0 : 8);
        }
    }

    private void changeNextButtonState(boolean z) {
        if (this.mEnableNextOnConnection && hasNextButton()) {
            getNextButton().setEnabled(z);
        }
    }

    @Override
    public void onForget(WifiDialog wifiDialog) {
        forget();
    }

    @Override
    public void onSubmit(WifiDialog wifiDialog) {
        if (this.mDialog != null) {
            submit(this.mDialog.getController());
        }
    }

    void submit(WifiConfigController wifiConfigController) {
        WifiConfiguration config = wifiConfigController.getConfig();
        Log.d("WifiSettings", "submit, config = " + config);
        if (config == null) {
            if (this.mSelectedAccessPoint != null && this.mSelectedAccessPoint.isSaved()) {
                connect(this.mSelectedAccessPoint.getConfig(), true);
            }
        } else if (wifiConfigController.getMode() == 2) {
            this.mWifiManager.save(config, this.mSaveListener);
        } else {
            this.mWifiManager.save(config, this.mSaveListener);
            if (this.mSelectedAccessPoint != null) {
                connect(config, false);
            }
        }
        this.mWifiTracker.resumeScanning();
    }

    void forget() {
        this.mMetricsFeatureProvider.action(getActivity(), 137, new Pair[0]);
        if (!this.mSelectedAccessPoint.isSaved()) {
            if (this.mSelectedAccessPoint.getNetworkInfo() != null && this.mSelectedAccessPoint.getNetworkInfo().getState() != NetworkInfo.State.DISCONNECTED) {
                this.mWifiManager.disableEphemeralNetwork(AccessPoint.convertToQuotedString(this.mSelectedAccessPoint.getSsidStr()));
            } else {
                Log.e("WifiSettings", "Failed to forget invalid network " + this.mSelectedAccessPoint.getConfig());
                return;
            }
        } else if (this.mSelectedAccessPoint.getConfig().isPasspoint()) {
            this.mWifiManager.removePasspointConfiguration(this.mSelectedAccessPoint.getConfig().FQDN);
        } else {
            this.mWifiManager.forget(this.mSelectedAccessPoint.getConfig().networkId, this.mForgetListener);
        }
        this.mWifiTracker.resumeScanning();
        changeNextButtonState(false);
    }

    protected void connect(WifiConfiguration wifiConfiguration, boolean z) {
        this.mMetricsFeatureProvider.action(getVisibilityLogger(), 135, z);
        this.mWifiManager.connect(wifiConfiguration, this.mConnectListener);
        this.mClickedConnect = true;
    }

    void onAddNetworkPressed() {
        this.mMetricsFeatureProvider.action(getActivity(), 134, new Pair[0]);
        this.mSelectedAccessPoint = null;
        showDialog(null, 1);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_wifi;
    }

    @Override
    public void onAccessPointChanged(final AccessPoint accessPoint) {
        Log.d("WifiSettings", "onAccessPointChanged (singular) callback initiated");
        View view = getView();
        if (view != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    Object tag = accessPoint.getTag();
                    if (tag != null) {
                        ((AccessPointPreference) tag).refresh();
                    }
                }
            });
        }
    }

    @Override
    public void onLevelChanged(AccessPoint accessPoint) {
        ((AccessPointPreference) accessPoint.getTag()).onLevelChanged();
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider, SummaryUpdater.OnSummaryChangeListener {
        private final Context mContext;
        WifiSummaryUpdater mSummaryHelper;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
            this.mSummaryHelper = new WifiSummaryUpdater(this.mContext, this);
        }

        @Override
        public void setListening(boolean z) {
            this.mSummaryHelper.register(z);
        }

        @Override
        public void onSummaryChanged(String str) {
            this.mSummaryLoader.setSummary(this, str);
        }
    }
}
