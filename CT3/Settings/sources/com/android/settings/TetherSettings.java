package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDun;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.wifi.WifiApDialog;
import com.android.settings.wifi.WifiApEnabler;
import com.android.settingslib.TetherUtil;
import com.mediatek.settings.TetherSettingsExt;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class TetherSettings extends RestrictedSettingsFragment implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener, DataSaverBackend.Listener {
    private boolean mBluetoothEnableForTether;
    private AtomicReference<BluetoothPan> mBluetoothPan;
    private String[] mBluetoothRegexs;
    private SwitchPreference mBluetoothTether;
    private ConnectivityManager mCm;
    private Preference mCreateNetwork;
    private DataSaverBackend mDataSaverBackend;
    private boolean mDataSaverEnabled;
    private Preference mDataSaverFooter;
    private WifiApDialog mDialog;
    private SwitchPreference mEnableWifiAp;
    private Handler mHandler;
    private boolean mIsPcKnowMe;
    private boolean mMassStorageActive;
    private BluetoothProfile.ServiceListener mProfileServiceListener;
    private boolean mRestartWifiApAfterConfigChange;
    private String[] mSecurityType;
    private OnStartTetheringCallback mStartTetheringCallback;
    private BroadcastReceiver mTetherChangeReceiver;
    private TetherSettingsExt mTetherSettingsExt;
    private boolean mUnavailable;
    private boolean mUsbConfigured;
    private boolean mUsbConnected;
    private boolean mUsbHwDisconnected;
    private String[] mUsbRegexs;
    private SwitchPreference mUsbTether;
    private boolean mUsbTetherCheckEnable;
    private boolean mUsbTetherDone;
    private boolean mUsbTetherFail;
    private boolean mUsbTethering;
    private boolean mUsbUnTetherDone;
    private WifiApEnabler mWifiApEnabler;
    private WifiConfiguration mWifiConfig;
    private WifiManager mWifiManager;
    private String[] mWifiRegexs;

    @Override
    protected int getMetricsCategory() {
        return 90;
    }

    public TetherSettings() {
        super("no_config_tethering");
        this.mBluetoothPan = new AtomicReference<>();
        this.mHandler = new Handler();
        this.mWifiConfig = null;
        this.mUsbTethering = false;
        this.mUsbTetherCheckEnable = false;
        this.mUsbUnTetherDone = true;
        this.mUsbTetherDone = true;
        this.mUsbTetherFail = false;
        this.mIsPcKnowMe = true;
        this.mProfileServiceListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                Log.d("TetheringSettings", "onServiceConnected ");
                TetherSettings.this.mBluetoothPan.set((BluetoothPan) proxy);
            }

            @Override
            public void onServiceDisconnected(int profile) {
                Log.d("TetheringSettings", "onServiceDisconnected ");
                BluetoothProfile bluetoothProfile = (BluetoothPan) TetherSettings.this.mBluetoothPan.get();
                if (bluetoothProfile != null) {
                    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                    if (adapter != null) {
                        adapter.closeProfileProxy(5, bluetoothProfile);
                    }
                }
                TetherSettings.this.mBluetoothPan.set(null);
            }
        };
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.tether_prefs);
        this.mDataSaverBackend = new DataSaverBackend(getContext());
        this.mDataSaverEnabled = this.mDataSaverBackend.isDataSaverEnabled();
        this.mDataSaverFooter = findPreference("disabled_on_data_saver");
        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted()) {
            this.mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getPrefContext(), null));
            return;
        }
        this.mTetherSettingsExt = new TetherSettingsExt(getActivity().getApplicationContext());
        this.mTetherSettingsExt.onCreate(getPreferenceScreen());
        Activity activity = getActivity();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.getState() == 12) {
            adapter.getProfileProxy(activity.getApplicationContext(), this.mProfileServiceListener, 5);
        }
        this.mEnableWifiAp = (SwitchPreference) findPreference("enable_wifi_ap");
        Preference wifiApSettings = findPreference("wifi_ap_ssid_and_security");
        this.mUsbTether = (SwitchPreference) findPreference("usb_tether_settings");
        this.mBluetoothTether = (SwitchPreference) findPreference("enable_bluetooth_tethering");
        this.mDataSaverBackend.addListener(this);
        this.mCm = (ConnectivityManager) getSystemService("connectivity");
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        this.mUsbRegexs = this.mCm.getTetherableUsbRegexs();
        this.mWifiRegexs = this.mCm.getTetherableWifiRegexs();
        this.mBluetoothRegexs = this.mCm.getTetherableBluetoothRegexs();
        boolean usbAvailable = this.mUsbRegexs.length != 0;
        boolean wifiAvailable = this.mWifiRegexs.length != 0;
        boolean bluetoothAvailable = this.mBluetoothRegexs.length != 0;
        if (!usbAvailable || Utils.isMonkeyRunning()) {
            getPreferenceScreen().removePreference(this.mUsbTether);
        }
        if (wifiAvailable && !Utils.isMonkeyRunning()) {
            this.mWifiApEnabler = new WifiApEnabler(activity, this.mDataSaverBackend, this.mEnableWifiAp);
            initWifiTethering();
        } else {
            getPreferenceScreen().removePreference(this.mEnableWifiAp);
            getPreferenceScreen().removePreference(wifiApSettings);
        }
        if (!bluetoothAvailable) {
            getPreferenceScreen().removePreference(this.mBluetoothTether);
        } else {
            BluetoothPan pan = this.mBluetoothPan.get();
            if (pan != null && pan.isTetheringOn()) {
                this.mBluetoothTether.setChecked(true);
            } else {
                this.mBluetoothTether.setChecked(false);
            }
            this.mTetherSettingsExt.updateBtTetherState(this.mBluetoothTether);
        }
        onDataSaverChanged(this.mDataSaverBackend.isDataSaverEnabled());
    }

    @Override
    public void onDestroy() {
        this.mDataSaverBackend.remListener(this);
        BluetoothProfile bluetoothProfile = (BluetoothPan) this.mBluetoothPan.get();
        if (bluetoothProfile != null) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                adapter.closeProfileProxy(5, bluetoothProfile);
            }
            this.mBluetoothPan.set(null);
        }
        if (this.mTetherSettingsExt != null) {
            this.mTetherSettingsExt.onDestroy();
            this.mTetherSettingsExt = null;
        }
        super.onDestroy();
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        this.mDataSaverEnabled = isDataSaving;
        this.mEnableWifiAp.setEnabled(!this.mDataSaverEnabled);
        this.mUsbTether.setEnabled(!this.mDataSaverEnabled);
        this.mBluetoothTether.setEnabled(this.mDataSaverEnabled ? false : true);
        this.mDataSaverFooter.setVisible(this.mDataSaverEnabled);
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
    }

    private void initWifiTethering() {
        Activity activity = getActivity();
        this.mWifiConfig = this.mWifiManager.getWifiApConfiguration();
        this.mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);
        this.mCreateNetwork = findPreference("wifi_ap_ssid_and_security");
        this.mRestartWifiApAfterConfigChange = false;
        if (this.mWifiConfig == null) {
            String s = activity.getString(android.R.string.ext_media_unmount_action);
            this.mCreateNetwork.setSummary(String.format(activity.getString(R.string.wifi_tether_configure_subtext), s, this.mSecurityType[0]));
        } else {
            int index = WifiApDialog.getSecurityTypeIndex(this.mWifiConfig);
            this.mCreateNetwork.setSummary(String.format(activity.getString(R.string.wifi_tether_configure_subtext), this.mWifiConfig.SSID, this.mSecurityType[index]));
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == 1) {
            Activity activity = getActivity();
            this.mDialog = new WifiApDialog(activity, this, this.mWifiConfig);
            return this.mDialog;
        }
        return null;
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        TetherChangeReceiver(TetherSettings this$0, TetherChangeReceiver tetherChangeReceiver) {
            this();
        }

        private TetherChangeReceiver() {
        }

        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            Log.d("TetheringSettings", "TetherChangeReceiver - onReceive, action is " + action);
            if (action.equals("android.net.conn.TETHER_STATE_CHANGED")) {
                ArrayList<String> available = intent.getStringArrayListExtra("availableArray");
                ArrayList<String> active = intent.getStringArrayListExtra("activeArray");
                ArrayList<String> errored = intent.getStringArrayListExtra("erroredArray");
                TetherSettings.this.mUsbUnTetherDone = intent.getBooleanExtra("UnTetherDone", false);
                TetherSettings.this.mUsbTetherDone = intent.getBooleanExtra("TetherDone", false);
                TetherSettings.this.mUsbTetherFail = intent.getBooleanExtra("TetherFail", false);
                Log.d("TetheringSettings", "mUsbUnTetherDone? :" + TetherSettings.this.mUsbUnTetherDone + " , mUsbTetherDonel? :" + TetherSettings.this.mUsbTetherDone + " , tether fail? :" + TetherSettings.this.mUsbTetherFail);
                TetherSettings.this.updateState((String[]) available.toArray(new String[available.size()]), (String[]) active.toArray(new String[active.size()]), (String[]) errored.toArray(new String[errored.size()]));
                if (TetherSettings.this.mWifiManager.getWifiApState() == 11 && TetherSettings.this.mRestartWifiApAfterConfigChange) {
                    TetherSettings.this.mRestartWifiApAfterConfigChange = false;
                    Log.d("TetheringSettings", "Restarting WifiAp due to prior config change.");
                    TetherSettings.this.startTethering(0);
                }
            } else if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                int state = intent.getIntExtra("wifi_state", 0);
                if (state == 11 && TetherSettings.this.mRestartWifiApAfterConfigChange) {
                    TetherSettings.this.mRestartWifiApAfterConfigChange = false;
                    Log.d("TetheringSettings", "Restarting WifiAp due to prior config change.");
                    TetherSettings.this.startTethering(0);
                }
            } else if (action.equals("android.intent.action.MEDIA_SHARED")) {
                TetherSettings.this.mMassStorageActive = true;
                TetherSettings.this.updateState();
            } else if (action.equals("android.intent.action.MEDIA_UNSHARED")) {
                TetherSettings.this.mMassStorageActive = false;
                TetherSettings.this.updateState();
            } else if (action.equals("android.hardware.usb.action.USB_STATE")) {
                TetherSettings.this.mUsbConnected = intent.getBooleanExtra("connected", false);
                TetherSettings.this.mUsbConfigured = intent.getBooleanExtra("configured", false);
                TetherSettings.this.mUsbHwDisconnected = intent.getBooleanExtra("USB_HW_DISCONNECTED", false);
                TetherSettings.this.mIsPcKnowMe = intent.getBooleanExtra("USB_IS_PC_KNOW_ME", true);
                Log.d("TetheringSettings", "TetherChangeReceiver - ACTION_USB_STATE mUsbConnected: " + TetherSettings.this.mUsbConnected + ", mUsbConfigured:  " + TetherSettings.this.mUsbConfigured + ", mUsbHwDisconnected: " + TetherSettings.this.mUsbHwDisconnected);
                TetherSettings.this.updateState();
            } else if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                if (TetherSettings.this.mBluetoothEnableForTether) {
                    switch (intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE)) {
                        case Integer.MIN_VALUE:
                        case 10:
                            TetherSettings.this.mBluetoothEnableForTether = false;
                            break;
                        case 12:
                            TetherSettings.this.startTethering(2);
                            TetherSettings.this.mBluetoothEnableForTether = false;
                            BluetoothDun bluetoothDun = TetherSettings.this.mTetherSettingsExt.BluetoothDunGetProxy();
                            if (bluetoothDun != null) {
                                bluetoothDun.setBluetoothTethering(true);
                                TetherSettings.this.mBluetoothEnableForTether = false;
                            }
                            break;
                    }
                }
                TetherSettings.this.updateState();
            }
            TetherSettings.this.onReceiveExt(action, intent);
        }
    }

    @Override
    public void onStart() {
        TetherChangeReceiver tetherChangeReceiver = null;
        super.onStart();
        if (this.mUnavailable) {
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.tethering_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }
        Activity activity = getActivity();
        this.mStartTetheringCallback = new OnStartTetheringCallback(this);
        this.mMassStorageActive = this.mTetherSettingsExt.isUMSEnabled();
        Log.d("TetheringSettings", "mMassStorageActive = " + this.mMassStorageActive);
        this.mTetherChangeReceiver = new TetherChangeReceiver(this, tetherChangeReceiver);
        IntentFilter filter = new IntentFilter("android.net.conn.TETHER_STATE_CHANGED");
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        Intent intent = activity.registerReceiver(this.mTetherChangeReceiver, filter);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.hardware.usb.action.USB_STATE");
        activity.registerReceiver(this.mTetherChangeReceiver, filter2);
        IntentFilter filter3 = new IntentFilter();
        filter3.addAction("android.intent.action.MEDIA_SHARED");
        filter3.addAction("android.intent.action.MEDIA_UNSHARED");
        filter3.addDataScheme("file");
        activity.registerReceiver(this.mTetherChangeReceiver, filter3);
        IntentFilter filter4 = new IntentFilter();
        filter4.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        activity.registerReceiver(this.mTetherChangeReceiver, filter4);
        this.mTetherSettingsExt.onStart(activity, this.mTetherChangeReceiver);
        if (intent != null) {
            this.mTetherChangeReceiver.onReceive(activity, intent);
        }
        if (this.mWifiApEnabler != null) {
            this.mEnableWifiAp.setOnPreferenceChangeListener(this);
            this.mWifiApEnabler.resume();
        }
        updateState();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.mUnavailable) {
            return;
        }
        getActivity().unregisterReceiver(this.mTetherChangeReceiver);
        this.mTetherChangeReceiver = null;
        this.mStartTetheringCallback = null;
        if (this.mWifiApEnabler == null) {
            return;
        }
        this.mEnableWifiAp.setOnPreferenceChangeListener(null);
        this.mWifiApEnabler.pause();
    }

    public void updateState() {
        String[] available = this.mCm.getTetherableIfaces();
        String[] tethered = this.mCm.getTetheredIfaces();
        String[] errored = this.mCm.getTetheringErroredIfaces();
        updateState(available, tethered, errored);
    }

    public void updateState(String[] available, String[] tethered, String[] errored) {
        if (updateStateExt(available, tethered, errored)) {
            return;
        }
        updateUsbState(available, tethered, errored);
        updateBluetoothState(available, tethered, errored);
        if (Utils.isMonkeyRunning()) {
            return;
        }
        this.mTetherSettingsExt.updateIpv6Preference(this.mUsbTether, this.mBluetoothTether, this.mWifiManager);
    }

    private void updateUsbState(String[] available, String[] tethered, String[] errored) {
        boolean usbAvailable = this.mUsbConnected && !this.mMassStorageActive;
        int usbError = 0;
        for (String s : available) {
            for (String regex : this.mUsbRegexs) {
                if (s.matches(regex) && usbError == 0) {
                    usbError = this.mCm.getLastTetherError(s);
                }
            }
        }
        boolean usbTethered = false;
        for (String s2 : tethered) {
            for (String regex2 : this.mUsbRegexs) {
                if (s2.matches(regex2)) {
                    usbTethered = true;
                }
            }
        }
        boolean usbErrored = false;
        for (String s3 : errored) {
            for (String regex3 : this.mUsbRegexs) {
                if (s3.matches(regex3)) {
                    usbErrored = true;
                }
            }
        }
        int usbError2 = this.mTetherSettingsExt.getUSBErrorCode(available, tethered, this.mUsbRegexs);
        Log.d("TetheringSettings", "updateUsbState - usbTethered : " + usbTethered + " usbErrored: " + usbErrored + " usbAvailable: " + usbAvailable);
        if (usbTethered) {
            Log.d("TetheringSettings", "updateUsbState: usbTethered ! mUsbTether checkbox setEnabled & checked ");
            this.mUsbTether.setEnabled(!this.mDataSaverEnabled);
            this.mUsbTether.setChecked(true);
            String summary = getString(R.string.usb_tethering_active_subtext);
            this.mTetherSettingsExt.updateUSBPrfSummary(this.mUsbTether, summary, usbTethered, usbAvailable);
            this.mUsbTethering = false;
            this.mTetherSettingsExt.updateUsbTypeListState(false);
            Log.d("TetheringSettings", "updateUsbState - usbTethered - mUsbTetherCheckEnable: " + this.mUsbTetherCheckEnable);
            return;
        }
        if (usbAvailable) {
            if (usbError2 == 0) {
                this.mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
            } else {
                this.mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            }
            this.mTetherSettingsExt.updateUSBPrfSummary(this.mUsbTether, null, usbTethered, usbAvailable);
            if (this.mUsbTetherCheckEnable) {
                Log.d("TetheringSettings", "updateUsbState - mUsbTetherCheckEnable, mUsbTether checkbox setEnabled, and set unchecked ");
                this.mUsbTether.setEnabled(!this.mDataSaverEnabled);
                this.mUsbTether.setChecked(false);
                this.mUsbTethering = false;
                this.mTetherSettingsExt.updateUsbTypeListState(true);
            }
            Log.d("TetheringSettings", "updateUsbState - usbAvailable - mUsbConfigured:  " + this.mUsbConfigured + " mUsbTethering: " + this.mUsbTethering + " mUsbTetherCheckEnable: " + this.mUsbTetherCheckEnable);
            return;
        }
        if (usbErrored) {
            this.mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            this.mUsbTether.setEnabled(false);
            this.mUsbTether.setChecked(false);
            this.mUsbTethering = false;
            return;
        }
        if (this.mMassStorageActive) {
            this.mUsbTether.setSummary(R.string.usb_tethering_storage_active_subtext);
            this.mUsbTether.setEnabled(false);
            this.mUsbTether.setChecked(false);
            this.mUsbTethering = false;
            return;
        }
        if (this.mUsbHwDisconnected || !(this.mUsbHwDisconnected || this.mUsbConnected || this.mUsbConfigured)) {
            this.mUsbTether.setSummary(R.string.usb_tethering_unavailable_subtext);
            this.mUsbTether.setEnabled(false);
            this.mUsbTether.setChecked(false);
            this.mUsbTethering = false;
        } else {
            Log.d("TetheringSettings", "updateUsbState - else, mUsbTether checkbox setEnabled, and set unchecked ");
            this.mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
            this.mUsbTether.setEnabled(true);
            this.mUsbTether.setChecked(false);
            this.mUsbTethering = false;
            this.mTetherSettingsExt.updateUsbTypeListState(true);
        }
        Log.d("TetheringSettings", "updateUsbState- usbAvailable- mUsbHwDisconnected:" + this.mUsbHwDisconnected);
    }

    private void updateBluetoothState(String[] available, String[] tethered, String[] errored) {
        this.mTetherSettingsExt.getBTErrorCode(available);
        boolean bluetoothErrored = false;
        for (String s : errored) {
            for (String regex : this.mBluetoothRegexs) {
                if (s.matches(regex)) {
                    bluetoothErrored = true;
                }
            }
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return;
        }
        int btState = adapter.getState();
        Log.d("TetheringSettings", "btState = " + btState);
        if (btState == 13) {
            this.mBluetoothTether.setEnabled(false);
            this.mBluetoothTether.setSummary(R.string.bluetooth_turning_off);
            return;
        }
        if (btState == 11) {
            this.mBluetoothTether.setEnabled(false);
            this.mBluetoothTether.setSummary(R.string.bluetooth_turning_on);
            return;
        }
        BluetoothPan bluetoothPan = this.mBluetoothPan.get();
        BluetoothDun bluetoothDun = this.mTetherSettingsExt.BluetoothDunGetProxy();
        if (btState == 12 && ((bluetoothPan != null && bluetoothPan.isTetheringOn()) || (bluetoothDun != null && bluetoothDun.isTetheringOn()))) {
            this.mBluetoothTether.setChecked(true);
            this.mBluetoothTether.setEnabled(!this.mDataSaverEnabled);
            int bluetoothTethered = 0;
            if (bluetoothPan != null && bluetoothPan.isTetheringOn()) {
                bluetoothTethered = bluetoothPan.getConnectedDevices().size();
                Log.d("TetheringSettings", "bluetooth Tethered PAN devices = " + bluetoothTethered);
            }
            if (bluetoothDun != null && bluetoothDun.isTetheringOn()) {
                bluetoothTethered += bluetoothDun.getConnectedDevices().size();
                Log.d("TetheringSettings", "bluetooth tethered total devices = " + bluetoothTethered);
            }
            if (bluetoothTethered > 1) {
                String summary = getString(R.string.bluetooth_tethering_devices_connected_subtext, new Object[]{Integer.valueOf(bluetoothTethered)});
                this.mTetherSettingsExt.updateBTPrfSummary(this.mBluetoothTether, summary);
                return;
            } else if (bluetoothTethered == 1) {
                String summary2 = getString(R.string.bluetooth_tethering_device_connected_subtext);
                this.mTetherSettingsExt.updateBTPrfSummary(this.mBluetoothTether, summary2);
                return;
            } else if (bluetoothErrored) {
                this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_errored_subtext);
                return;
            } else {
                String summary3 = getString(R.string.bluetooth_tethering_available_subtext);
                this.mTetherSettingsExt.updateBTPrfSummary(this.mBluetoothTether, summary3);
                return;
            }
        }
        this.mBluetoothTether.setEnabled(!this.mDataSaverEnabled);
        this.mBluetoothTether.setChecked(false);
        this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_off_subtext);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == this.mEnableWifiAp) {
            boolean enable = ((Boolean) value).booleanValue();
            if (enable) {
                startTethering(0);
            } else {
                this.mCm.stopTethering(0);
            }
            return false;
        }
        return this.mTetherSettingsExt.onPreferenceChange(preference, value);
    }

    public static boolean isProvisioningNeededButUnavailable(Context context) {
        return TetherUtil.isProvisioningNeeded(context) && !isIntentAvailable(context);
    }

    private static boolean isIntentAvailable(Context context) {
        String[] provisionApp = context.getResources().getStringArray(android.R.array.config_autoBrightnessLevelsIdle);
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClassName(provisionApp[0], provisionApp[1]);
        return packageManager.queryIntentActivities(intent, 65536).size() > 0;
    }

    public void startTethering(int choice) {
        if (choice == 2) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter.getState() == 10) {
                this.mBluetoothEnableForTether = true;
                if (this.mBluetoothPan.get() == null) {
                    adapter.getProfileProxy(getActivity().getApplicationContext(), this.mProfileServiceListener, 5);
                }
                adapter.enable();
                this.mBluetoothTether.setSummary(R.string.bluetooth_turning_on);
                this.mBluetoothTether.setEnabled(false);
                return;
            }
            this.mTetherSettingsExt.updateBtDunTether(true);
            String summary = getString(R.string.bluetooth_tethering_available_subtext);
            this.mTetherSettingsExt.updateBTPrfSummary(this.mBluetoothTether, summary);
        }
        this.mCm.startTethering(choice, true, this.mStartTetheringCallback, this.mHandler);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == this.mUsbTether) {
            if (this.mUsbTethering) {
                return true;
            }
            boolean newState = this.mUsbTether.isChecked();
            this.mUsbTether.setEnabled(false);
            this.mTetherSettingsExt.updateUsbTypeListState(false);
            this.mUsbTethering = true;
            this.mUsbTetherCheckEnable = false;
            if (newState) {
                this.mUsbTetherDone = false;
            } else {
                this.mUsbUnTetherDone = false;
            }
            this.mUsbTetherFail = false;
            Log.d("TetheringSettings", "onPreferenceTreeClick - setusbTethering(" + newState + ") mUsbTethering:  " + this.mUsbTethering);
            if (this.mUsbTether.isChecked()) {
                startTethering(1);
            } else {
                this.mCm.stopTethering(1);
            }
        } else if (preference == this.mBluetoothTether) {
            if (this.mBluetoothTether.isChecked()) {
                startTethering(2);
            } else {
                this.mCm.stopTethering(2);
                this.mTetherSettingsExt.updateBtDunTether(false);
                updateState();
            }
            if (!Utils.isMonkeyRunning()) {
                this.mTetherSettingsExt.updateIpv6Preference(this.mUsbTether, this.mBluetoothTether, this.mWifiManager);
            }
        } else if (preference == this.mCreateNetwork) {
            showDialog(1);
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button != -1) {
            return;
        }
        this.mWifiConfig = this.mDialog.getConfig();
        if (this.mWifiConfig == null) {
            return;
        }
        if (this.mWifiManager.getWifiApState() == 13) {
            Log.d("TetheringSettings", "Wifi AP config changed while enabled, stop and restart");
            this.mRestartWifiApAfterConfigChange = true;
            this.mCm.stopTethering(0);
        }
        this.mWifiManager.setWifiApConfiguration(this.mWifiConfig);
        int index = WifiApDialog.getSecurityTypeIndex(this.mWifiConfig);
        this.mCreateNetwork.setSummary(String.format(getActivity().getString(R.string.wifi_tether_configure_subtext), this.mWifiConfig.SSID, this.mSecurityType[index]));
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_tether;
    }

    private static final class OnStartTetheringCallback extends ConnectivityManager.OnStartTetheringCallback {
        final WeakReference<TetherSettings> mTetherSettings;

        OnStartTetheringCallback(TetherSettings settings) {
            this.mTetherSettings = new WeakReference<>(settings);
        }

        public void onTetheringStarted() {
            update();
        }

        public void onTetheringFailed() {
            update();
        }

        private void update() {
            TetherSettings settings = this.mTetherSettings.get();
            if (settings == null) {
                return;
            }
            settings.updateState();
        }
    }

    private boolean updateStateExt(String[] available, String[] tethered, String[] errored) {
        Log.d("TetheringSettings", "=======> updateState - mUsbConnected: " + this.mUsbConnected + ", mUsbConfigured:  " + this.mUsbConfigured + ", mUsbHwDisconnected: " + this.mUsbHwDisconnected + ", checked: " + this.mUsbTether.isChecked() + ", mUsbUnTetherDone: " + this.mUsbUnTetherDone + ", mUsbTetherDone: " + this.mUsbTetherDone + ", tetherfail: " + this.mUsbTetherFail + ", mIsPcKnowMe: " + this.mIsPcKnowMe);
        if (this.mUsbTether.isChecked()) {
            if (!this.mUsbConnected || !this.mUsbConfigured || this.mUsbHwDisconnected) {
                this.mUsbTetherCheckEnable = false;
            } else if (this.mUsbTetherFail || this.mUsbTetherDone || !this.mIsPcKnowMe) {
                this.mUsbTetherCheckEnable = true;
            }
        } else if (!this.mUsbConnected || this.mUsbHwDisconnected) {
            this.mUsbTetherCheckEnable = false;
        } else if (this.mUsbUnTetherDone || this.mUsbTetherFail) {
            this.mUsbTetherCheckEnable = true;
        }
        return false;
    }

    public void onReceiveExt(String action, Intent intent) {
        if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
            int state = intent.getIntExtra("wifi_state", 14);
            if ((state != 13 && state != 11) || Utils.isMonkeyRunning()) {
                return;
            }
            this.mTetherSettingsExt.updateIpv6Preference(this.mUsbTether, this.mBluetoothTether, this.mWifiManager);
            return;
        }
        if (action.equals("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED") || action.equals("android.bluetooth.dun.intent.DUN_STATE")) {
            updateState();
        } else {
            if (!"action.wifi.tethered_switch".equals(action) || Utils.isMonkeyRunning()) {
                return;
            }
            this.mTetherSettingsExt.updateIpv6Preference(this.mUsbTether, this.mBluetoothTether, this.mWifiManager);
        }
    }
}
