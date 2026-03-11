package com.android.settings.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.LinkifyUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.location.ScanningSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BluetoothSettings extends DeviceListPreferenceFragment implements Indexable {
    private PreferenceGroup mAvailableDevicesCategory;
    private boolean mAvailableDevicesCategoryIsPresent;
    private BluetoothEnabler mBluetoothEnabler;
    private final View.OnClickListener mDeviceProfilesListener;
    private boolean mInitialScanStarted;
    private boolean mInitiateDiscoverable;
    private final IntentFilter mIntentFilter;
    Preference mMyDevicePreference;
    private PreferenceGroup mPairedDevicesCategory;
    private final BroadcastReceiver mReceiver;
    private SwitchBar mSwitchBar;
    private static View mSettingsDialogView = null;
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList<>();
            Resources res = context.getResources();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.bluetooth_settings);
            data.screenTitle = res.getString(R.string.bluetooth_settings);
            result.add(data);
            LocalBluetoothManager lbtm = Utils.getLocalBtManager(context);
            if (lbtm != null) {
                Set<BluetoothDevice> bondedDevices = lbtm.getBluetoothAdapter().getBondedDevices();
                for (BluetoothDevice device : bondedDevices) {
                    SearchIndexableRaw data2 = new SearchIndexableRaw(context);
                    data2.title = device.getName();
                    data2.screenTitle = res.getString(R.string.bluetooth_settings);
                    data2.enabled = enabled;
                    result.add(data2);
                }
            }
            return result;
        }
    };

    public BluetoothSettings() {
        super("no_config_bluetooth");
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
                if (action.equals("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED")) {
                    updateDeviceName(context);
                }
                if (state != 12) {
                    return;
                }
                BluetoothSettings.this.mInitiateDiscoverable = true;
            }

            private void updateDeviceName(Context context) {
                if (!BluetoothSettings.this.mLocalAdapter.isEnabled() || BluetoothSettings.this.mMyDevicePreference == null) {
                    return;
                }
                Resources res = context.getResources();
                Locale locale = res.getConfiguration().getLocales().get(0);
                BidiFormatter bidiFormatter = BidiFormatter.getInstance(locale);
                BluetoothSettings.this.mMyDevicePreference.setSummary(res.getString(R.string.bluetooth_is_visible_message, bidiFormatter.unicodeWrap(BluetoothSettings.this.mLocalAdapter.getName())));
            }
        };
        this.mDeviceProfilesListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(v.getTag() instanceof CachedBluetoothDevice)) {
                    Log.w("BluetoothSettings", "onClick() called for other View: " + v);
                    return;
                }
                CachedBluetoothDevice device = (CachedBluetoothDevice) v.getTag();
                Log.d("BluetoothSettings", "onClick " + device.getName());
                Bundle args = new Bundle();
                args.putString("device_address", device.getDevice().getAddress());
                DeviceProfilesSettings profileSettings = new DeviceProfilesSettings();
                profileSettings.setArguments(args);
                profileSettings.show(BluetoothSettings.this.getFragmentManager(), DeviceProfilesSettings.class.getSimpleName());
            }
        };
        this.mIntentFilter = new IntentFilter("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
    }

    @Override
    protected int getMetricsCategory() {
        return 24;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mInitialScanStarted = false;
        this.mInitiateDiscoverable = true;
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitchBar = activity.getSwitchBar();
        this.mBluetoothEnabler = new BluetoothEnabler(activity, this.mSwitchBar);
        this.mBluetoothEnabler.setupSwitchBar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.mBluetoothEnabler.teardownSwitchBar();
    }

    @Override
    void addPreferencesForActivity() {
        addPreferencesFromResource(R.xml.bluetooth_settings);
        this.mPairedDevicesCategory = new PreferenceCategory(getPrefContext());
        this.mPairedDevicesCategory.setKey("paired_devices");
        this.mPairedDevicesCategory.setOrder(1);
        getPreferenceScreen().addPreference(this.mPairedDevicesCategory);
        this.mAvailableDevicesCategory = new BluetoothProgressCategory(getActivity());
        this.mAvailableDevicesCategory.setSelectable(false);
        this.mAvailableDevicesCategory.setOrder(2);
        getPreferenceScreen().addPreference(this.mAvailableDevicesCategory);
        this.mMyDevicePreference = new Preference(getPrefContext());
        this.mMyDevicePreference.setSelectable(false);
        this.mMyDevicePreference.setOrder(3);
        getPreferenceScreen().addPreference(this.mMyDevicePreference);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        if (this.mBluetoothEnabler != null) {
            this.mBluetoothEnabler.resume(getActivity());
        }
        super.onResume();
        this.mInitiateDiscoverable = true;
        if (isUiRestricted()) {
            setDeviceListGroup(getPreferenceScreen());
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.bluetooth_empty_list_user_restricted);
            }
            removeAllDevices();
            return;
        }
        getActivity().registerReceiver(this.mReceiver, this.mIntentFilter);
        if (this.mLocalAdapter == null) {
            return;
        }
        updateContent(this.mLocalAdapter.getBluetoothState());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mBluetoothEnabler != null) {
            this.mBluetoothEnabler.pause();
        }
        this.mLocalAdapter.setScanMode(21);
        if (isUiRestricted()) {
            return;
        }
        getActivity().unregisterReceiver(this.mReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mPairedDevicesCategory != null) {
            this.mPairedDevicesCategory.removeAll();
        }
        if (this.mAvailableDevicesCategory == null) {
            return;
        }
        this.mAvailableDevicesCategory.removeAll();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (this.mLocalAdapter == null || isUiRestricted()) {
            return;
        }
        boolean bluetoothIsEnabled = this.mLocalAdapter.getBluetoothState() == 12;
        boolean isDiscovering = this.mLocalAdapter.isDiscovering();
        Log.d("BluetoothSettings", "onCreateOptionsMenu, isDiscovering " + isDiscovering);
        int textId = isDiscovering ? R.string.bluetooth_searching_for_devices : R.string.bluetooth_search_for_devices;
        menu.add(0, 1, 0, textId).setEnabled(bluetoothIsEnabled && !isDiscovering).setShowAsAction(0);
        menu.add(0, 2, 0, R.string.bluetooth_rename_device).setEnabled(bluetoothIsEnabled).setShowAsAction(0);
        menu.add(0, 3, 0, R.string.bluetooth_show_received_files).setShowAsAction(0);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DefaultWfcSettingsExt.PAUSE:
                if (this.mLocalAdapter.getBluetoothState() == 12) {
                    MetricsLogger.action(getActivity(), 160);
                    startScanning();
                }
                return true;
            case DefaultWfcSettingsExt.CREATE:
                MetricsLogger.action(getActivity(), 161);
                new BluetoothNameDialogFragment().show(getFragmentManager(), "rename device");
                return true;
            case DefaultWfcSettingsExt.DESTROY:
                MetricsLogger.action(getActivity(), 162);
                Intent intent = new Intent("android.btopp.intent.action.OPEN_RECEIVED_FILES");
                getActivity().sendBroadcast(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startScanning() {
        if (isUiRestricted()) {
            return;
        }
        if (!this.mAvailableDevicesCategoryIsPresent) {
            getPreferenceScreen().addPreference(this.mAvailableDevicesCategory);
            this.mAvailableDevicesCategoryIsPresent = true;
        }
        if (this.mAvailableDevicesCategory != null) {
            setDeviceListGroup(this.mAvailableDevicesCategory);
            removeAllDevices();
        }
        this.mLocalManager.getCachedDeviceManager().clearNonBondedDevices();
        this.mAvailableDevicesCategory.removeAll();
        this.mInitialScanStarted = true;
        this.mLocalAdapter.startScanning(true);
    }

    @Override
    void onDevicePreferenceClick(BluetoothDevicePreference btPreference) {
        this.mLocalAdapter.stopScanning();
        super.onDevicePreferenceClick(btPreference);
    }

    private void addDeviceCategory(PreferenceGroup preferenceGroup, int titleId, BluetoothDeviceFilter.Filter filter, boolean addCachedDevices) {
        cacheRemoveAllPrefs(preferenceGroup);
        preferenceGroup.setTitle(titleId);
        setFilter(filter);
        setDeviceListGroup(preferenceGroup);
        if (addCachedDevices) {
            addCachedDevices();
        }
        preferenceGroup.setEnabled(true);
        removeCachedPrefs(preferenceGroup);
    }

    private void updateContent(int bluetoothState) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        int messageId = 0;
        switch (bluetoothState) {
            case 10:
                setOffMessage();
                if (isUiRestricted()) {
                    messageId = R.string.bluetooth_empty_list_user_restricted;
                }
                break;
            case 11:
                messageId = R.string.bluetooth_turning_on;
                this.mInitialScanStarted = false;
                break;
            case 12:
                this.mDevicePreferenceMap.clear();
                if (isUiRestricted()) {
                    messageId = R.string.bluetooth_empty_list_user_restricted;
                } else {
                    getPreferenceScreen().removeAll();
                    getPreferenceScreen().addPreference(this.mPairedDevicesCategory);
                    getPreferenceScreen().addPreference(this.mAvailableDevicesCategory);
                    getPreferenceScreen().addPreference(this.mMyDevicePreference);
                    addDeviceCategory(this.mPairedDevicesCategory, R.string.bluetooth_preference_paired_devices, BluetoothDeviceFilter.BONDED_DEVICE_FILTER, true);
                    int numberOfPairedDevices = this.mPairedDevicesCategory.getPreferenceCount();
                    if (isUiRestricted() || numberOfPairedDevices <= 0) {
                        if (preferenceScreen.findPreference("paired_devices") != null) {
                            preferenceScreen.removePreference(this.mPairedDevicesCategory);
                        }
                    } else if (preferenceScreen.findPreference("paired_devices") == null) {
                        preferenceScreen.addPreference(this.mPairedDevicesCategory);
                    }
                    addDeviceCategory(this.mAvailableDevicesCategory, R.string.bluetooth_preference_found_devices, BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER, this.mInitialScanStarted);
                    if (!this.mInitialScanStarted) {
                        startScanning();
                    }
                    Resources res = getResources();
                    Locale locale = res.getConfiguration().getLocales().get(0);
                    BidiFormatter bidiFormatter = BidiFormatter.getInstance(locale);
                    this.mMyDevicePreference.setSummary(res.getString(R.string.bluetooth_is_visible_message, bidiFormatter.unicodeWrap(this.mLocalAdapter.getName())));
                    getActivity().invalidateOptionsMenu();
                    if (this.mInitiateDiscoverable) {
                        this.mLocalAdapter.setScanMode(23);
                        this.mInitiateDiscoverable = false;
                        return;
                    }
                    return;
                }
                break;
            case 13:
                messageId = R.string.bluetooth_turning_off;
                break;
        }
        setDeviceListGroup(preferenceScreen);
        removeAllDevices();
        if (messageId != 0) {
            getEmptyTextView().setText(messageId);
        }
        if (isUiRestricted()) {
            return;
        }
        getActivity().invalidateOptionsMenu();
    }

    private void setOffMessage() {
        TextView emptyView = getEmptyTextView();
        if (emptyView == null) {
            return;
        }
        CharSequence briefText = getText(R.string.bluetooth_empty_list_bluetooth_off);
        ContentResolver resolver = getActivity().getContentResolver();
        boolean bleScanningMode = Settings.Global.getInt(resolver, "ble_scan_always_enabled", 0) == 1;
        if (!bleScanningMode) {
            emptyView.setText(briefText, TextView.BufferType.SPANNABLE);
        } else {
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(briefText);
            contentBuilder.append("\n\n");
            contentBuilder.append(getText(R.string.ble_scan_notify_text));
            LinkifyUtils.linkify(emptyView, contentBuilder, new LinkifyUtils.OnClickListener() {
                @Override
                public void onClick() {
                    SettingsActivity activity = (SettingsActivity) BluetoothSettings.this.getActivity();
                    activity.startPreferencePanel(ScanningSettings.class.getName(), null, R.string.location_scanning_screen_title, null, null, 0);
                }
            });
        }
        if (this.mAvailableDevicesCategory != null) {
            this.mAvailableDevicesCategory.removeAll();
        }
        getPreferenceScreen().removeAll();
        Spannable boldSpan = (Spannable) emptyView.getText();
        boldSpan.setSpan(new TextAppearanceSpan(getActivity(), android.R.style.TextAppearance.Medium), 0, briefText.length(), 33);
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        super.onBluetoothStateChanged(bluetoothState);
        if (12 == bluetoothState) {
            this.mInitiateDiscoverable = true;
        }
        updateContent(bluetoothState);
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        Log.d("BluetoothSettings", "onScanningStateChanged() started : " + started);
        super.onScanningStateChanged(started);
        if (getActivity() == null) {
            return;
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        setDeviceListGroup(getPreferenceScreen());
        removeAllDevices();
        updateContent(this.mLocalAdapter.getBluetoothState());
    }

    @Override
    void initDevicePreference(BluetoothDevicePreference preference) {
        CachedBluetoothDevice cachedDevice = preference.getCachedDevice();
        if (cachedDevice.getBondState() != 12) {
            return;
        }
        preference.setOnSettingsClickListener(this.mDeviceProfilesListener);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_bluetooth;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider, BluetoothCallback {
        private final LocalBluetoothManager mBluetoothManager;
        private boolean mConnected;
        private final Context mContext;
        private boolean mEnabled;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mBluetoothManager = Utils.getLocalBtManager(context);
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
            if (defaultAdapter == null) {
                return;
            }
            if (listening) {
                this.mEnabled = defaultAdapter.isEnabled();
                this.mConnected = defaultAdapter.getConnectionState() == 2;
                this.mSummaryLoader.setSummary(this, getSummary());
                this.mBluetoothManager.getEventManager().registerCallback(this);
                return;
            }
            this.mBluetoothManager.getEventManager().unregisterCallback(this);
        }

        private CharSequence getSummary() {
            int i;
            Context context = this.mContext;
            if (this.mEnabled) {
                i = this.mConnected ? R.string.bluetooth_connected : R.string.bluetooth_disconnected;
            } else {
                i = R.string.bluetooth_disabled;
            }
            return context.getString(i);
        }

        @Override
        public void onBluetoothStateChanged(int bluetoothState) {
            this.mEnabled = bluetoothState == 12;
            this.mSummaryLoader.setSummary(this, getSummary());
        }

        @Override
        public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
            this.mConnected = state == 2;
            this.mSummaryLoader.setSummary(this, getSummary());
        }

        @Override
        public void onScanningStateChanged(boolean started) {
        }

        @Override
        public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        }

        @Override
        public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        }

        @Override
        public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        }
    }
}
