package com.android.settings;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.wifi.tether.WifiTetherPreferenceController;
import com.android.settingslib.TetherUtil;
import com.android.settingslib.wifi.AccessPoint;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class TetherSettings extends RestrictedSettingsFragment implements DataSaverBackend.Listener {
    private boolean mBluetoothEnableForTether;
    private AtomicReference<BluetoothPan> mBluetoothPan;
    private String[] mBluetoothRegexs;
    private SwitchPreference mBluetoothTether;
    private ConnectivityManager mCm;
    private DataSaverBackend mDataSaverBackend;
    private boolean mDataSaverEnabled;
    private Preference mDataSaverFooter;
    private Handler mHandler;
    private boolean mMassStorageActive;
    private BluetoothProfile.ServiceListener mProfileServiceListener;
    private OnStartTetheringCallback mStartTetheringCallback;
    private BroadcastReceiver mTetherChangeReceiver;
    private boolean mUnavailable;
    private boolean mUsbConnected;
    private String[] mUsbRegexs;
    private SwitchPreference mUsbTether;
    private WifiTetherPreferenceController mWifiTetherPreferenceController;

    @Override
    public int getMetricsCategory() {
        return 90;
    }

    public TetherSettings() {
        super("no_config_tethering");
        this.mBluetoothPan = new AtomicReference<>();
        this.mHandler = new Handler();
        this.mProfileServiceListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                Log.d("TetheringSettings", "onServiceConnected ");
                TetherSettings.this.mBluetoothPan.set((BluetoothPan) bluetoothProfile);
            }

            @Override
            public void onServiceDisconnected(int i) {
                BluetoothAdapter defaultAdapter;
                Log.d("TetheringSettings", "onServiceDisconnected ");
                BluetoothProfile bluetoothProfile = (BluetoothPan) TetherSettings.this.mBluetoothPan.get();
                if (bluetoothProfile != null && (defaultAdapter = BluetoothAdapter.getDefaultAdapter()) != null) {
                    defaultAdapter.closeProfileProxy(5, bluetoothProfile);
                }
                TetherSettings.this.mBluetoothPan.set(null);
            }
        };
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mWifiTetherPreferenceController = new WifiTetherPreferenceController(context, getLifecycle());
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.tether_prefs);
        this.mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.tethering_footer_info);
        this.mDataSaverBackend = new DataSaverBackend(getContext());
        this.mDataSaverEnabled = this.mDataSaverBackend.isDataSaverEnabled();
        this.mDataSaverFooter = findPreference("disabled_on_data_saver");
        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted()) {
            this.mUnavailable = true;
            getPreferenceScreen().removeAll();
            return;
        }
        Activity activity = getActivity();
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null && defaultAdapter.getState() == 12) {
            defaultAdapter.getProfileProxy(activity.getApplicationContext(), this.mProfileServiceListener, 5);
        }
        this.mUsbTether = (SwitchPreference) findPreference("usb_tether_settings");
        this.mBluetoothTether = (SwitchPreference) findPreference("enable_bluetooth_tethering");
        this.mDataSaverBackend.addListener(this);
        this.mCm = (ConnectivityManager) getSystemService("connectivity");
        this.mUsbRegexs = this.mCm.getTetherableUsbRegexs();
        this.mBluetoothRegexs = this.mCm.getTetherableBluetoothRegexs();
        boolean z = this.mUsbRegexs.length != 0;
        boolean z2 = this.mBluetoothRegexs.length != 0;
        if (!z || Utils.isMonkeyRunning()) {
            getPreferenceScreen().removePreference(this.mUsbTether);
        }
        this.mWifiTetherPreferenceController.displayPreference(getPreferenceScreen());
        if (!z2) {
            getPreferenceScreen().removePreference(this.mBluetoothTether);
        } else {
            BluetoothPan bluetoothPan = this.mBluetoothPan.get();
            if (bluetoothPan != null && bluetoothPan.isTetheringOn()) {
                this.mBluetoothTether.setChecked(true);
            } else {
                this.mBluetoothTether.setChecked(false);
            }
        }
        onDataSaverChanged(this.mDataSaverBackend.isDataSaverEnabled());
    }

    @Override
    public void onDestroy() {
        this.mDataSaverBackend.remListener(this);
        this.mBluetoothPan.get();
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothProfile bluetoothProfile = (BluetoothProfile) this.mBluetoothPan.getAndSet(null);
        if (bluetoothProfile != null && defaultAdapter != null) {
            defaultAdapter.closeProfileProxy(5, bluetoothProfile);
            this.mBluetoothPan.set(null);
        }
        super.onDestroy();
    }

    @Override
    public void onDataSaverChanged(boolean z) {
        this.mDataSaverEnabled = z;
        this.mUsbTether.setEnabled(!this.mDataSaverEnabled);
        this.mBluetoothTether.setEnabled(!this.mDataSaverEnabled);
        this.mDataSaverFooter.setVisible(this.mDataSaverEnabled);
    }

    @Override
    public void onWhitelistStatusChanged(int i, boolean z) {
    }

    @Override
    public void onBlacklistStatusChanged(int i, boolean z) {
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        private TetherChangeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("TetheringSettings", "TetherChangeReceiver onReceive  action=" + action);
            if (action.equals("android.net.conn.TETHER_STATE_CHANGED")) {
                ArrayList<String> stringArrayListExtra = intent.getStringArrayListExtra("availableArray");
                ArrayList<String> stringArrayListExtra2 = intent.getStringArrayListExtra("tetherArray");
                ArrayList<String> stringArrayListExtra3 = intent.getStringArrayListExtra("erroredArray");
                TetherSettings.this.updateState((String[]) stringArrayListExtra.toArray(new String[stringArrayListExtra.size()]), (String[]) stringArrayListExtra2.toArray(new String[stringArrayListExtra2.size()]), (String[]) stringArrayListExtra3.toArray(new String[stringArrayListExtra3.size()]));
            } else if (action.equals("android.intent.action.MEDIA_SHARED")) {
                TetherSettings.this.mMassStorageActive = true;
                TetherSettings.this.updateState();
            } else if (action.equals("android.intent.action.MEDIA_UNSHARED")) {
                TetherSettings.this.mMassStorageActive = false;
                TetherSettings.this.updateState();
            } else if (action.equals("android.hardware.usb.action.USB_STATE")) {
                TetherSettings.this.mUsbConnected = intent.getBooleanExtra("connected", false);
                TetherSettings.this.updateState();
            } else if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                if (TetherSettings.this.mBluetoothEnableForTether) {
                    int intExtra = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", AccessPoint.UNREACHABLE_RSSI);
                    if (intExtra == Integer.MIN_VALUE || intExtra == 10) {
                        TetherSettings.this.mBluetoothEnableForTether = false;
                    } else if (intExtra == 12) {
                        TetherSettings.this.startTethering(2);
                        TetherSettings.this.mBluetoothEnableForTether = false;
                    }
                }
                TetherSettings.this.updateState();
            }
            TetherSettings.this.onReceiveExt(action, intent);
        }
    }

    @Override
    public void onStart() {
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
        this.mMassStorageActive = "shared".equals(Environment.getExternalStorageState());
        this.mTetherChangeReceiver = new TetherChangeReceiver();
        Intent intentRegisterReceiver = activity.registerReceiver(this.mTetherChangeReceiver, new IntentFilter("android.net.conn.TETHER_STATE_CHANGED"));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.hardware.usb.action.USB_STATE");
        activity.registerReceiver(this.mTetherChangeReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.MEDIA_SHARED");
        intentFilter2.addAction("android.intent.action.MEDIA_UNSHARED");
        intentFilter2.addDataScheme("file");
        activity.registerReceiver(this.mTetherChangeReceiver, intentFilter2);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter3.addAction("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
        activity.registerReceiver(this.mTetherChangeReceiver, intentFilter3);
        if (intentRegisterReceiver != null) {
            this.mTetherChangeReceiver.onReceive(activity, intentRegisterReceiver);
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
    }

    public void updateState() {
        updateState(this.mCm.getTetherableIfaces(), this.mCm.getTetheredIfaces(), this.mCm.getTetheringErroredIfaces());
    }

    public void updateState(String[] strArr, String[] strArr2, String[] strArr3) {
        updateUsbState(strArr, strArr2, strArr3);
        updateBluetoothState();
    }

    private void updateUsbState(String[] strArr, String[] strArr2, String[] strArr3) {
        boolean z = this.mUsbConnected && !this.mMassStorageActive;
        int length = strArr.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            String str = strArr[i];
            int lastTetherError = i2;
            for (String str2 : this.mUsbRegexs) {
                if (str.matches(str2) && lastTetherError == 0) {
                    lastTetherError = this.mCm.getLastTetherError(str);
                }
            }
            i++;
            i2 = lastTetherError;
        }
        int length2 = strArr2.length;
        int i3 = 0;
        boolean z2 = false;
        while (i3 < length2) {
            String str3 = strArr2[i3];
            boolean z3 = z2;
            for (String str4 : this.mUsbRegexs) {
                if (str3.matches(str4)) {
                    z3 = true;
                }
            }
            i3++;
            z2 = z3;
        }
        for (String str5 : strArr3) {
            for (String str6 : this.mUsbRegexs) {
                str5.matches(str6);
            }
        }
        if (z2) {
            this.mUsbTether.setEnabled(!this.mDataSaverEnabled);
            this.mUsbTether.setChecked(true);
        } else if (z) {
            this.mUsbTether.setEnabled(!this.mDataSaverEnabled);
            this.mUsbTether.setChecked(false);
        } else {
            this.mUsbTether.setEnabled(false);
            this.mUsbTether.setChecked(false);
        }
    }

    private void updateBluetoothState() {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter == null) {
            return;
        }
        int state = defaultAdapter.getState();
        if (state == 13) {
            this.mBluetoothTether.setEnabled(false);
            return;
        }
        if (state == 11) {
            this.mBluetoothTether.setEnabled(false);
            return;
        }
        BluetoothPan bluetoothPan = this.mBluetoothPan.get();
        Log.d("TetheringSettings", "updateBluetoothState bluetoothPan= " + bluetoothPan + " btState= " + state + " mDataSaverEnabled=" + this.mDataSaverEnabled);
        if (bluetoothPan != null) {
            Log.d("TetheringSettings", "updateBluetoothState tetheringon= " + bluetoothPan.isTetheringOn());
        }
        if (state == 12 && bluetoothPan != null && bluetoothPan.isTetheringOn()) {
            this.mBluetoothTether.setChecked(true);
            this.mBluetoothTether.setEnabled(!this.mDataSaverEnabled);
        } else {
            this.mBluetoothTether.setEnabled(!this.mDataSaverEnabled);
            this.mBluetoothTether.setChecked(false);
        }
    }

    public static boolean isProvisioningNeededButUnavailable(Context context) {
        return TetherUtil.isProvisioningNeeded(context) && !isIntentAvailable(context);
    }

    private static boolean isIntentAvailable(Context context) {
        String[] stringArray = context.getResources().getStringArray(android.R.array.config_cell_retries_per_error_code);
        if (stringArray.length < 2) {
            return false;
        }
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClassName(stringArray[0], stringArray[1]);
        return packageManager.queryIntentActivities(intent, 65536).size() > 0;
    }

    public void startTethering(int i) {
        if (i == 2) {
            BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
            if (this.mBluetoothPan.get() == null) {
                defaultAdapter.getProfileProxy(getActivity().getApplicationContext(), this.mProfileServiceListener, 5);
            }
            if (defaultAdapter.getState() == 10) {
                this.mBluetoothEnableForTether = true;
                defaultAdapter.enable();
                this.mBluetoothTether.setEnabled(false);
                return;
            }
        }
        this.mCm.startTethering(i, true, this.mStartTetheringCallback, this.mHandler);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == this.mUsbTether) {
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
                updateState();
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_tether;
    }

    private static final class OnStartTetheringCallback extends ConnectivityManager.OnStartTetheringCallback {
        final WeakReference<TetherSettings> mTetherSettings;

        OnStartTetheringCallback(TetherSettings tetherSettings) {
            this.mTetherSettings = new WeakReference<>(tetherSettings);
        }

        public void onTetheringStarted() {
            update();
        }

        public void onTetheringFailed() {
            update();
        }

        private void update() {
            TetherSettings tetherSettings = this.mTetherSettings.get();
            if (tetherSettings != null) {
                tetherSettings.updateState();
            }
        }
    }

    public void onReceiveExt(String str, Intent intent) {
        if (str.equals("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED")) {
            updateState();
        }
    }
}
