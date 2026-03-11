package com.mediatek.settings;

import android.app.Activity;
import android.bluetooth.BluetoothDun;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Utils;
import com.mediatek.settings.ext.IApnSettingsExt;
import java.util.concurrent.atomic.AtomicReference;

public class TetherSettingsExt implements Preference.OnPreferenceChangeListener {
    private BluetoothDun mBluetoothDunProxy;
    private String[] mBluetoothRegexs;
    private int mBtErrorIpv4;
    private int mBtErrorIpv6;
    private ConnectivityManager mConnectService;
    private Context mContext;
    IApnSettingsExt mExt;
    private PreferenceScreen mPrfscreen;
    private Resources mResources;
    private ListPreference mTetherIpv6;
    public ListPreference mUsbTetherType;
    private IMountService mMountService = null;
    private int mUsbErrorIpv4 = 0;
    private int mUsbErrorIpv6 = 16;
    private AtomicReference<BluetoothDun> mBluetoothDun = new AtomicReference<>();
    private BluetoothDun.ServiceListener mDunServiceListener = new BluetoothDun.ServiceListener() {
        public void onServiceConnected(BluetoothDun proxy) {
            Log.d("TetherSettingsExt", "BluetoothDun service connected");
            TetherSettingsExt.this.mBluetoothDun.set(proxy);
        }

        public void onServiceDisconnected() {
            Log.d("TetherSettingsExt", "BluetoothDun service disconnected");
            BluetoothDun dun = (BluetoothDun) TetherSettingsExt.this.mBluetoothDun.get();
            if (dun != null) {
                dun.close();
            }
            TetherSettingsExt.this.mBluetoothDun.set(null);
            TetherSettingsExt.this.mBluetoothDunProxy = null;
        }
    };

    public TetherSettingsExt(Context context) {
        Log.d("TetherSettingsExt", "TetherSettingsExt");
        this.mContext = context;
        initServices();
    }

    public void onCreate(PreferenceScreen screen) {
        Log.d("TetherSettingsExt", "onCreate");
        this.mPrfscreen = screen;
        initPreference(screen);
        this.mExt = UtilsExt.getApnSettingsPlugin(this.mContext);
        this.mExt.customizeTetherApnSettings(screen);
        this.mBluetoothDunProxy = new BluetoothDun(this.mContext, this.mDunServiceListener);
    }

    public void onStart(Activity activity, BroadcastReceiver receiver) {
        IntentFilter filter = getIntentFilter();
        activity.registerReceiver(receiver, filter);
        if (this.mUsbTetherType == null) {
            return;
        }
        this.mUsbTetherType.setOnPreferenceChangeListener(this);
        int value = Settings.System.getInt(activity.getContentResolver(), "usb_tethering_type", 0);
        this.mUsbTetherType.setValue(String.valueOf(value));
        this.mUsbTetherType.setSummary(activity.getResources().getStringArray(R.array.usb_tether_type_entries)[value]);
    }

    public void onDestroy() {
        BluetoothDun dun = this.mBluetoothDun.get();
        if (dun != null) {
            dun.close();
            this.mBluetoothDun.set(null);
        }
        if (this.mBluetoothDunProxy == null) {
            return;
        }
        this.mBluetoothDunProxy.close();
        this.mBluetoothDunProxy = null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String key = preference.getKey();
        Log.d("TetherSettingsExt", "onPreferenceChange key=" + key);
        if ("usb_tethering_type".equals(key)) {
            int index = Integer.parseInt((String) value);
            Settings.System.putInt(this.mContext.getContentResolver(), "usb_tethering_type", index);
            this.mUsbTetherType.setSummary(this.mResources.getStringArray(R.array.usb_tether_type_entries)[index]);
            Log.d("TetherSettingsExt", "onPreferenceChange USB_TETHERING_TYPE value = " + index);
        } else if ("tethered_ipv6".equals(key)) {
            int ipv6Value = Integer.parseInt(String.valueOf(value));
            if (this.mConnectService != null) {
                this.mConnectService.setTetheringIpv6Enable(ipv6Value == 1);
            }
            this.mTetherIpv6.setValueIndex(ipv6Value);
            this.mTetherIpv6.setSummary(this.mResources.getStringArray(R.array.tethered_ipv6_entries)[ipv6Value]);
        }
        return true;
    }

    private void initPreference(PreferenceScreen screen) {
        if (FeatureOption.MTK_TETHERING_EEM_SUPPORT) {
            this.mUsbTetherType = new ListPreference(this.mContext);
            this.mUsbTetherType.setKey("usb_tethering_type");
            this.mUsbTetherType.setTitle(R.string.usb_tether_type_title);
            screen.addPreference(this.mUsbTetherType);
            this.mUsbTetherType.setEntries(R.array.usb_tether_type_entries);
            this.mUsbTetherType.setEntryValues(R.array.usb_tether_type_values);
            this.mUsbTetherType.setPersistent(false);
            int order = -99;
            Preference usbTetherSettings = screen.findPreference("usb_tether_settings");
            if (usbTetherSettings != null) {
                order = usbTetherSettings.getOrder() + 1;
            }
            this.mUsbTetherType.setOrder(order);
        }
        String[] usbRegexs = this.mConnectService.getTetherableUsbRegexs();
        boolean usbAvailable = usbRegexs.length != 0;
        if ((!usbAvailable || Utils.isMonkeyRunning()) && this.mUsbTetherType != null) {
            Log.d("TetherSettingsExt", "remove mUsbTetherType");
            screen.removePreference(this.mUsbTetherType);
        }
        if (!FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            return;
        }
        this.mTetherIpv6 = new ListPreference(this.mContext);
        this.mTetherIpv6.setKey("tethered_ipv6");
        this.mTetherIpv6.setTitle(R.string.tethered_ipv6_title);
        screen.addPreference(this.mTetherIpv6);
        this.mTetherIpv6.setEntries(R.array.tethered_ipv6_entries);
        this.mTetherIpv6.setEntryValues(R.array.tethered_ipv6_values);
        this.mTetherIpv6.setPersistent(false);
        this.mTetherIpv6.setOnPreferenceChangeListener(this);
    }

    public IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("mediatek.intent.action.USB_DATA_STATE");
        filter.addAction("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
        filter.addAction("android.bluetooth.dun.intent.DUN_STATE");
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        filter.addAction("action.wifi.tethered_switch");
        return filter;
    }

    private synchronized void initServices() {
        if (this.mMountService == null) {
            IBinder service = ServiceManager.getService("mount");
            if (service != null) {
                this.mMountService = IMountService.Stub.asInterface(service);
            } else {
                Log.e("TetherSettingsExt", "Can't get mount service");
            }
        }
        this.mConnectService = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mResources = this.mContext.getResources();
        this.mBluetoothRegexs = this.mConnectService.getTetherableBluetoothRegexs();
    }

    public boolean isUMSEnabled() {
        if (this.mMountService == null) {
            Log.d("TetherSettingsExt", " mMountService is null, return");
            return false;
        }
        try {
            return this.mMountService.isUsbMassStorageEnabled();
        } catch (RemoteException e) {
            Log.e("TetherSettingsExt", "Util:RemoteException when isUsbMassStorageEnabled: " + e);
            return false;
        } catch (UnsupportedOperationException e2) {
            Log.e("TetherSettingsExt", "this device doesn't support UMS");
            return false;
        }
    }

    public void updateIpv6Preference(SwitchPreference usbTether, SwitchPreference bluetoothTether, WifiManager wifiManager) {
        boolean z = false;
        if (!FeatureOption.MTK_TETHERINGIPV6_SUPPORT || this.mTetherIpv6 == null) {
            return;
        }
        ListPreference listPreference = this.mTetherIpv6;
        if (!usbTether.isChecked() && !bluetoothTether.isChecked() && !wifiManager.isWifiApEnabled()) {
            z = true;
        }
        listPreference.setEnabled(z);
        if (this.mConnectService == null) {
            return;
        }
        int ipv6Value = this.mConnectService.getTetheringIpv6Enable() ? 1 : 0;
        this.mTetherIpv6.setValueIndex(ipv6Value);
        this.mTetherIpv6.setSummary(this.mResources.getStringArray(R.array.tethered_ipv6_entries)[ipv6Value]);
    }

    public void updateBTPrfSummary(Preference pref, String originSummary) {
        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            pref.setSummary(originSummary + getIPV6String(this.mBtErrorIpv4, this.mBtErrorIpv6));
        } else {
            pref.setSummary(originSummary);
        }
    }

    public void updateUSBPrfSummary(Preference pref, String originSummary, boolean usbTethered, boolean usbAvailable) {
        if (usbTethered) {
            if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                pref.setSummary(originSummary + getIPV6String(this.mUsbErrorIpv4, this.mUsbErrorIpv6));
            } else {
                pref.setSummary(originSummary);
            }
        }
        if (!usbAvailable || !FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            return;
        }
        if (this.mUsbErrorIpv4 == 0 || this.mUsbErrorIpv6 == 16) {
            pref.setSummary(R.string.usb_tethering_available_subtext);
        } else {
            pref.setSummary(R.string.usb_tethering_errored_subtext);
        }
    }

    public void updateUsbTypeListState(boolean state) {
        if (this.mUsbTetherType == null) {
            return;
        }
        Log.d("TetherSettingsExt", "set USB Tether Type state = " + state);
        this.mUsbTetherType.setEnabled(state);
    }

    public void getBTErrorCode(String[] available) {
        if (!FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            return;
        }
        this.mBtErrorIpv4 = 0;
        this.mBtErrorIpv6 = 16;
        for (String s : available) {
            for (String regex : this.mBluetoothRegexs) {
                if (s.matches(regex) && this.mConnectService != null) {
                    if (this.mBtErrorIpv4 == 0) {
                        this.mBtErrorIpv4 = this.mConnectService.getLastTetherError(s) & 15;
                    }
                    if (this.mBtErrorIpv6 == 16) {
                        this.mBtErrorIpv6 = this.mConnectService.getLastTetherError(s) & 240;
                    }
                }
            }
        }
    }

    public String getIPV6String(int errorIpv4, int errorIpv6) {
        if (this.mTetherIpv6 == null || !"1".equals(this.mTetherIpv6.getValue())) {
            return "";
        }
        Log.d("TetherSettingsExt", "[errorIpv4 =" + errorIpv4 + "];[errorIpv6 =" + errorIpv6 + "];");
        if (errorIpv4 == 0 && errorIpv6 == 32) {
            String text = this.mResources.getString(R.string.tethered_ipv4v6);
            return text;
        }
        if (errorIpv4 == 0) {
            String text2 = this.mResources.getString(R.string.tethered_ipv4);
            return text2;
        }
        if (errorIpv6 != 32) {
            return "";
        }
        String text3 = this.mResources.getString(R.string.tethered_ipv6);
        return text3;
    }

    public int getUSBErrorCode(String[] available, String[] tethered, String[] usbRegexs) {
        int usbError = 0;
        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            this.mUsbErrorIpv4 = 0;
            this.mUsbErrorIpv6 = 16;
        }
        for (String s : available) {
            for (String regex : usbRegexs) {
                if (s.matches(regex) && this.mConnectService != null) {
                    if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                        if (this.mUsbErrorIpv4 == 0) {
                            this.mUsbErrorIpv4 = this.mConnectService.getLastTetherError(s) & 15;
                        }
                        if (this.mUsbErrorIpv6 == 16) {
                            this.mUsbErrorIpv6 = this.mConnectService.getLastTetherError(s) & 240;
                        }
                    } else if (usbError == 0) {
                        usbError = this.mConnectService.getLastTetherError(s);
                    }
                }
            }
        }
        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            for (String s2 : tethered) {
                for (String regex2 : usbRegexs) {
                    if (s2.matches(regex2) && this.mConnectService != null && this.mUsbErrorIpv6 == 16) {
                        this.mUsbErrorIpv6 = this.mConnectService.getLastTetherError(s2) & 240;
                    }
                }
            }
        }
        return usbError;
    }

    public void updateBtTetherState(SwitchPreference btPrf) {
        BluetoothDun dun = BluetoothDunGetProxy();
        if (dun != null && dun.isTetheringOn() && btPrf != null) {
            btPrf.setChecked(true);
        } else {
            btPrf.setChecked(false);
        }
    }

    public void updateBtDunTether(boolean state) {
        BluetoothDun bluetoothDun = BluetoothDunGetProxy();
        if (bluetoothDun == null) {
            return;
        }
        bluetoothDun.setBluetoothTethering(state);
    }

    public BluetoothDun BluetoothDunGetProxy() {
        BluetoothDun Dun = this.mBluetoothDun.get();
        if (Dun == null) {
            if (this.mBluetoothDunProxy != null) {
                this.mBluetoothDun.set(this.mBluetoothDunProxy);
            } else {
                this.mBluetoothDunProxy = new BluetoothDun(this.mContext, this.mDunServiceListener);
            }
            return this.mBluetoothDunProxy;
        }
        return Dun;
    }
}
