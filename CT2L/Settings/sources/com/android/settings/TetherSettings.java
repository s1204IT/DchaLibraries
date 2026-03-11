package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
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
import android.os.Environment;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.widget.TextView;
import android.widget.Toast;
import com.android.settings.wifi.WifiApDialog;
import com.android.settings.wifi.WifiApEnabler;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class TetherSettings extends SettingsPreferenceFragment implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener {
    private boolean mBluetoothEnableForTether;
    private String[] mBluetoothRegexs;
    private SwitchPreference mBluetoothTether;
    private Preference mCreateNetwork;
    private WifiApDialog mDialog;
    private SwitchPreference mEnableWifiAp;
    private boolean mMassStorageActive;
    private String[] mProvisionApp;
    private String[] mSecurityType;
    private BroadcastReceiver mTetherChangeReceiver;
    private UserManager mUm;
    private boolean mUnavailable;
    private boolean mUsbConnected;
    private String[] mUsbRegexs;
    private SwitchPreference mUsbTether;
    private WifiApEnabler mWifiApEnabler;
    private WifiManager mWifiManager;
    private String[] mWifiRegexs;
    private AtomicReference<BluetoothPan> mBluetoothPan = new AtomicReference<>();
    private WifiConfiguration mWifiConfig = null;
    private int mTetherChoice = -1;
    private BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            TetherSettings.this.mBluetoothPan.set((BluetoothPan) proxy);
        }

        @Override
        public void onServiceDisconnected(int profile) {
            TetherSettings.this.mBluetoothPan.set(null);
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null) {
            this.mTetherChoice = icicle.getInt("TETHER_TYPE");
        }
        addPreferencesFromResource(R.xml.tether_prefs);
        this.mUm = (UserManager) getSystemService("user");
        if (this.mUm.hasUserRestriction("no_config_tethering")) {
            this.mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            return;
        }
        Activity activity = getActivity();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(activity.getApplicationContext(), this.mProfileServiceListener, 5);
        }
        this.mEnableWifiAp = (SwitchPreference) findPreference("enable_wifi_ap");
        Preference wifiApSettings = findPreference("wifi_ap_ssid_and_security");
        this.mUsbTether = (SwitchPreference) findPreference("usb_tether_settings");
        this.mBluetoothTether = (SwitchPreference) findPreference("enable_bluetooth_tethering");
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        this.mUsbRegexs = cm.getTetherableUsbRegexs();
        this.mWifiRegexs = cm.getTetherableWifiRegexs();
        this.mBluetoothRegexs = cm.getTetherableBluetoothRegexs();
        boolean usbAvailable = this.mUsbRegexs.length != 0;
        boolean wifiAvailable = this.mWifiRegexs.length != 0;
        boolean bluetoothAvailable = this.mBluetoothRegexs.length != 0;
        if (!usbAvailable || Utils.isMonkeyRunning()) {
            getPreferenceScreen().removePreference(this.mUsbTether);
        }
        if (wifiAvailable && !Utils.isMonkeyRunning()) {
            this.mWifiApEnabler = new WifiApEnabler(activity, this.mEnableWifiAp);
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
        }
        this.mProvisionApp = getResources().getStringArray(android.R.array.config_autoBrightnessLcdBacklightValues_doze);
        Toast.makeText(getActivity(), R.string.invalid_function, 1).show();
        activity.finish();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("TETHER_TYPE", this.mTetherChoice);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void initWifiTethering() {
        Activity activity = getActivity();
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        this.mWifiConfig = this.mWifiManager.getWifiApConfiguration();
        this.mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);
        this.mCreateNetwork = findPreference("wifi_ap_ssid_and_security");
        if (this.mWifiConfig == null) {
            String s = activity.getString(android.R.string.imProtocolSkype);
            this.mCreateNetwork.setSummary(String.format(activity.getString(R.string.wifi_tether_configure_subtext), s, this.mSecurityType[0]));
        } else {
            int index = WifiApDialog.getSecurityTypeIndex(this.mWifiConfig);
            this.mCreateNetwork.setSummary(String.format(activity.getString(R.string.wifi_tether_configure_subtext), this.mWifiConfig.SSID, this.mSecurityType[index]));
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id != 1) {
            return null;
        }
        Activity activity = getActivity();
        this.mDialog = new WifiApDialog(activity, this, this.mWifiConfig);
        return this.mDialog;
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        private TetherChangeReceiver() {
        }

        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.net.conn.TETHER_STATE_CHANGED")) {
                ArrayList<String> available = intent.getStringArrayListExtra("availableArray");
                ArrayList<String> active = intent.getStringArrayListExtra("activeArray");
                ArrayList<String> errored = intent.getStringArrayListExtra("erroredArray");
                TetherSettings.this.updateState((String[]) available.toArray(new String[available.size()]), (String[]) active.toArray(new String[active.size()]), (String[]) errored.toArray(new String[errored.size()]));
                return;
            }
            if (action.equals("android.intent.action.MEDIA_SHARED")) {
                TetherSettings.this.mMassStorageActive = true;
                TetherSettings.this.updateState();
                return;
            }
            if (action.equals("android.intent.action.MEDIA_UNSHARED")) {
                TetherSettings.this.mMassStorageActive = false;
                TetherSettings.this.updateState();
                return;
            }
            if (action.equals("android.hardware.usb.action.USB_STATE")) {
                TetherSettings.this.mUsbConnected = intent.getBooleanExtra("connected", false);
                TetherSettings.this.updateState();
            } else if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                if (TetherSettings.this.mBluetoothEnableForTether) {
                    switch (intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE)) {
                        case Integer.MIN_VALUE:
                        case 10:
                            TetherSettings.this.mBluetoothEnableForTether = false;
                            break;
                        case 12:
                            BluetoothPan bluetoothPan = (BluetoothPan) TetherSettings.this.mBluetoothPan.get();
                            if (bluetoothPan != null) {
                                bluetoothPan.setBluetoothTethering(true);
                                TetherSettings.this.mBluetoothEnableForTether = false;
                            }
                            break;
                    }
                }
                TetherSettings.this.updateState();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.mUnavailable) {
            TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
            getListView().setEmptyView(emptyView);
            if (emptyView != null) {
                emptyView.setText(R.string.tethering_settings_not_available);
                return;
            }
            return;
        }
        Activity activity = getActivity();
        this.mMassStorageActive = "shared".equals(Environment.getExternalStorageState());
        this.mTetherChangeReceiver = new TetherChangeReceiver();
        Intent intent = activity.registerReceiver(this.mTetherChangeReceiver, new IntentFilter("android.net.conn.TETHER_STATE_CHANGED"));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_STATE");
        activity.registerReceiver(this.mTetherChangeReceiver, filter);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.MEDIA_SHARED");
        filter2.addAction("android.intent.action.MEDIA_UNSHARED");
        filter2.addDataScheme("file");
        activity.registerReceiver(this.mTetherChangeReceiver, filter2);
        IntentFilter filter3 = new IntentFilter();
        filter3.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        activity.registerReceiver(this.mTetherChangeReceiver, filter3);
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
        if (!this.mUnavailable) {
            getActivity().unregisterReceiver(this.mTetherChangeReceiver);
            this.mTetherChangeReceiver = null;
            if (this.mWifiApEnabler != null) {
                this.mEnableWifiAp.setOnPreferenceChangeListener(null);
                this.mWifiApEnabler.pause();
            }
        }
    }

    public void updateState() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        String[] available = cm.getTetherableIfaces();
        String[] tethered = cm.getTetheredIfaces();
        String[] errored = cm.getTetheringErroredIfaces();
        updateState(available, tethered, errored);
    }

    public void updateState(String[] available, String[] tethered, String[] errored) {
        updateUsbState(available, tethered, errored);
        updateBluetoothState(available, tethered, errored);
    }

    private void updateUsbState(String[] available, String[] tethered, String[] errored) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        boolean usbAvailable = this.mUsbConnected && !this.mMassStorageActive;
        int usbError = 0;
        for (String s : available) {
            String[] arr$ = this.mUsbRegexs;
            for (String regex : arr$) {
                if (s.matches(regex) && usbError == 0) {
                    usbError = cm.getLastTetherError(s);
                }
            }
        }
        boolean usbTethered = false;
        for (String s2 : tethered) {
            String[] arr$2 = this.mUsbRegexs;
            for (String regex2 : arr$2) {
                if (s2.matches(regex2)) {
                    usbTethered = true;
                }
            }
        }
        boolean usbErrored = false;
        for (String s3 : errored) {
            String[] arr$3 = this.mUsbRegexs;
            for (String regex3 : arr$3) {
                if (s3.matches(regex3)) {
                    usbErrored = true;
                }
            }
        }
        if (usbTethered) {
            this.mUsbTether.setSummary(R.string.usb_tethering_active_subtext);
            this.mUsbTether.setEnabled(true);
            this.mUsbTether.setChecked(true);
            return;
        }
        if (usbAvailable) {
            if (usbError == 0) {
                this.mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
            } else {
                this.mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            }
            this.mUsbTether.setEnabled(true);
            this.mUsbTether.setChecked(false);
            return;
        }
        if (usbErrored) {
            this.mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            this.mUsbTether.setEnabled(false);
            this.mUsbTether.setChecked(false);
        } else if (this.mMassStorageActive) {
            this.mUsbTether.setSummary(R.string.usb_tethering_storage_active_subtext);
            this.mUsbTether.setEnabled(false);
            this.mUsbTether.setChecked(false);
        } else {
            this.mUsbTether.setSummary(R.string.usb_tethering_unavailable_subtext);
            this.mUsbTether.setEnabled(false);
            this.mUsbTether.setChecked(false);
        }
    }

    private void updateBluetoothState(String[] available, String[] tethered, String[] errored) {
        boolean bluetoothErrored = false;
        for (String s : errored) {
            String[] arr$ = this.mBluetoothRegexs;
            for (String regex : arr$) {
                if (s.matches(regex)) {
                    bluetoothErrored = true;
                }
            }
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            int btState = adapter.getState();
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
            if (btState == 12 && bluetoothPan != null && bluetoothPan.isTetheringOn()) {
                this.mBluetoothTether.setChecked(true);
                this.mBluetoothTether.setEnabled(true);
                int bluetoothTethered = bluetoothPan.getConnectedDevices().size();
                if (bluetoothTethered > 1) {
                    String summary = getString(R.string.bluetooth_tethering_devices_connected_subtext, new Object[]{Integer.valueOf(bluetoothTethered)});
                    this.mBluetoothTether.setSummary(summary);
                    return;
                } else if (bluetoothTethered == 1) {
                    this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_device_connected_subtext);
                    return;
                } else if (bluetoothErrored) {
                    this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_errored_subtext);
                    return;
                } else {
                    this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_available_subtext);
                    return;
                }
            }
            this.mBluetoothTether.setEnabled(true);
            this.mBluetoothTether.setChecked(false);
            this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_off_subtext);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean enable = ((Boolean) value).booleanValue();
        if (enable) {
            startProvisioningIfNecessary(0);
        } else {
            if (isProvisioningNeeded(this.mProvisionApp)) {
                TetherService.cancelRecheckAlarmIfNecessary(getActivity(), 0);
            }
            this.mWifiApEnabler.setSoftapEnabled(false);
        }
        return false;
    }

    public static boolean isProvisioningNeededButUnavailable(Context context) {
        String[] provisionApp = context.getResources().getStringArray(android.R.array.config_autoBrightnessLcdBacklightValues_doze);
        return isProvisioningNeeded(provisionApp) && !isIntentAvailable(context, provisionApp);
    }

    private static boolean isIntentAvailable(Context context, String[] provisionApp) {
        if (provisionApp.length < 2) {
            throw new IllegalArgumentException("provisionApp length should at least be 2");
        }
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClassName(provisionApp[0], provisionApp[1]);
        return packageManager.queryIntentActivities(intent, 65536).size() > 0;
    }

    private static boolean isProvisioningNeeded(String[] provisionApp) {
        return (SystemProperties.getBoolean("net.tethering.noprovisioning", false) || provisionApp == null || provisionApp.length != 2) ? false : true;
    }

    private void startProvisioningIfNecessary(int choice) {
        this.mTetherChoice = choice;
        if (isProvisioningNeeded(this.mProvisionApp)) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setClassName(this.mProvisionApp[0], this.mProvisionApp[1]);
            intent.putExtra("TETHER_TYPE", this.mTetherChoice);
            startActivityForResult(intent, 0);
            return;
        }
        startTethering();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == 0) {
            if (resultCode == -1) {
                TetherService.scheduleRecheckAlarm(getActivity(), this.mTetherChoice);
                startTethering();
                return;
            }
            switch (this.mTetherChoice) {
                case 1:
                    this.mUsbTether.setChecked(false);
                    break;
                case 2:
                    this.mBluetoothTether.setChecked(false);
                    break;
            }
            this.mTetherChoice = -1;
        }
    }

    private void startTethering() {
        switch (this.mTetherChoice) {
            case 0:
                this.mWifiApEnabler.setSoftapEnabled(true);
                break;
            case 1:
                setUsbTethering(true);
                break;
            case 2:
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter.getState() == 10) {
                    this.mBluetoothEnableForTether = true;
                    adapter.enable();
                    this.mBluetoothTether.setSummary(R.string.bluetooth_turning_on);
                    this.mBluetoothTether.setEnabled(false);
                } else {
                    BluetoothPan bluetoothPan = this.mBluetoothPan.get();
                    if (bluetoothPan != null) {
                        bluetoothPan.setBluetoothTethering(true);
                    }
                    this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_available_subtext);
                }
                break;
        }
    }

    private void setUsbTethering(boolean enabled) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        this.mUsbTether.setChecked(false);
        if (cm.setUsbTethering(enabled) != 0) {
            this.mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
        } else {
            this.mUsbTether.setSummary("");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        if (preference == this.mUsbTether) {
            boolean newState = this.mUsbTether.isChecked();
            if (newState) {
                startProvisioningIfNecessary(1);
            } else {
                if (isProvisioningNeeded(this.mProvisionApp)) {
                    TetherService.cancelRecheckAlarmIfNecessary(getActivity(), 1);
                }
                setUsbTethering(newState);
            }
        } else if (preference == this.mBluetoothTether) {
            boolean bluetoothTetherState = this.mBluetoothTether.isChecked();
            if (bluetoothTetherState) {
                startProvisioningIfNecessary(2);
            } else {
                if (isProvisioningNeeded(this.mProvisionApp)) {
                    TetherService.cancelRecheckAlarmIfNecessary(getActivity(), 2);
                }
                boolean errored = false;
                String[] tethered = cm.getTetheredIfaces();
                String bluetoothIface = findIface(tethered, this.mBluetoothRegexs);
                if (bluetoothIface != null && cm.untether(bluetoothIface) != 0) {
                    errored = true;
                }
                BluetoothPan bluetoothPan = this.mBluetoothPan.get();
                if (bluetoothPan != null) {
                    bluetoothPan.setBluetoothTethering(false);
                }
                if (errored) {
                    this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_errored_subtext);
                } else {
                    this.mBluetoothTether.setSummary(R.string.bluetooth_tethering_off_subtext);
                }
            }
        } else if (preference == this.mCreateNetwork) {
            showDialog(1);
        }
        return super.onPreferenceTreeClick(screen, preference);
    }

    private static String findIface(String[] ifaces, String[] regexes) {
        for (String iface : ifaces) {
            for (String regex : regexes) {
                if (iface.matches(regex)) {
                    return iface;
                }
            }
        }
        return null;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == -1) {
            this.mWifiConfig = this.mDialog.getConfig();
            if (this.mWifiConfig != null) {
                if (this.mWifiManager.getWifiApState() == 13) {
                    this.mWifiManager.setWifiApEnabled(null, false);
                    this.mWifiManager.setWifiApEnabled(this.mWifiConfig, true);
                } else {
                    this.mWifiManager.setWifiApConfiguration(this.mWifiConfig);
                }
                int index = WifiApDialog.getSecurityTypeIndex(this.mWifiConfig);
                this.mCreateNetwork.setSummary(String.format(getActivity().getString(R.string.wifi_tether_configure_subtext), this.mWifiConfig.SSID, this.mSecurityType[index]));
            }
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_tether;
    }

    public static boolean showInShortcuts(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        boolean isSecondaryUser = UserHandle.myUserId() != 0;
        return !isSecondaryUser && cm.isTetheringSupported();
    }
}
