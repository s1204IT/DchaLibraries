package com.android.settings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.wifi.WifiApEnabler;
import java.util.ArrayList;

public class TetherService extends Service {
    private static final boolean DEBUG = Log.isLoggable("TetherService", 3);
    private ArrayList<Integer> mCurrentTethers;
    private int mCurrentTypeIndex;
    private boolean mEnableWifiAfterCheck;
    private boolean mInProvisionCheck;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TetherService.DEBUG) {
                Log.d("TetherService", "Got provision result " + intent);
            }
            String provisionResponse = context.getResources().getString(android.R.string.autofill);
            if (provisionResponse.equals(intent.getAction())) {
                TetherService.this.mInProvisionCheck = false;
                int checkType = ((Integer) TetherService.this.mCurrentTethers.get(TetherService.this.mCurrentTypeIndex)).intValue();
                if (intent.getIntExtra("EntitlementResult", 0) != -1) {
                    switch (checkType) {
                        case 0:
                            TetherService.this.disableWifiTethering();
                            break;
                        case 1:
                            TetherService.this.disableUsbTethering();
                            break;
                        case 2:
                            TetherService.this.disableBtTethering();
                            break;
                    }
                } else if (checkType == 0 && TetherService.this.mEnableWifiAfterCheck) {
                    TetherService.this.enableWifiTetheringIfNeeded();
                    TetherService.this.mEnableWifiAfterCheck = false;
                }
                if (TetherService.access$204(TetherService.this) != TetherService.this.mCurrentTethers.size()) {
                    TetherService.this.startProvisioning(TetherService.this.mCurrentTypeIndex);
                } else {
                    TetherService.this.stopSelf();
                }
            }
        }
    };

    static int access$204(TetherService x0) {
        int i = x0.mCurrentTypeIndex + 1;
        x0.mCurrentTypeIndex = i;
        return i;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) {
            Log.d("TetherService", "Creating WifiProvisionService");
        }
        String provisionResponse = getResources().getString(android.R.string.autofill);
        registerReceiver(this.mReceiver, new IntentFilter(provisionResponse), "android.permission.CONNECTIVITY_INTERNAL", null);
        SharedPreferences prefs = getSharedPreferences("tetherPrefs", 0);
        this.mCurrentTethers = stringToTethers(prefs.getString("currentTethers", ""));
        this.mCurrentTypeIndex = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra("extraAddTetherType")) {
            int type = intent.getIntExtra("extraAddTetherType", -1);
            if (!this.mCurrentTethers.contains(Integer.valueOf(type))) {
                if (DEBUG) {
                    Log.d("TetherService", "Adding tether " + type);
                }
                this.mCurrentTethers.add(Integer.valueOf(type));
            }
        }
        if (intent.hasExtra("extraRemTetherType")) {
            int type2 = intent.getIntExtra("extraRemTetherType", -1);
            if (DEBUG) {
                Log.d("TetherService", "Removing tether " + type2);
            }
            int index = this.mCurrentTethers.indexOf(Integer.valueOf(type2));
            if (index >= 0) {
                this.mCurrentTethers.remove(index);
                if (index <= this.mCurrentTypeIndex && this.mCurrentTypeIndex > 0) {
                    this.mCurrentTypeIndex--;
                }
            }
            cancelAlarmIfNecessary();
        }
        if (intent.getBooleanExtra("extraSetAlarm", false) && this.mCurrentTethers.size() == 1) {
            scheduleAlarm();
        }
        if (intent.getBooleanExtra("extraEnableWifiTether", false)) {
            this.mEnableWifiAfterCheck = true;
        }
        if (intent.getBooleanExtra("extraRunProvision", false)) {
            startProvisioning(this.mCurrentTypeIndex);
            return 1;
        }
        if (this.mInProvisionCheck) {
            return 1;
        }
        stopSelf();
        return 2;
    }

    @Override
    public void onDestroy() {
        if (this.mInProvisionCheck) {
            Log.e("TetherService", "TetherService getting destroyed while mid-provisioning" + this.mCurrentTethers.get(this.mCurrentTypeIndex));
        }
        SharedPreferences prefs = getSharedPreferences("tetherPrefs", 0);
        prefs.edit().putString("currentTethers", tethersToString(this.mCurrentTethers)).commit();
        if (DEBUG) {
            Log.d("TetherService", "Destroying WifiProvisionService");
        }
        unregisterReceiver(this.mReceiver);
        super.onDestroy();
    }

    private ArrayList<Integer> stringToTethers(String tethersStr) {
        ArrayList<Integer> ret = new ArrayList<>();
        if (!TextUtils.isEmpty(tethersStr)) {
            String[] tethersSplit = tethersStr.split(",");
            for (String str : tethersSplit) {
                ret.add(Integer.valueOf(Integer.parseInt(str)));
            }
        }
        return ret;
    }

    private String tethersToString(ArrayList<Integer> tethers) {
        StringBuffer buffer = new StringBuffer();
        int N = tethers.size();
        for (int i = 0; i < N; i++) {
            if (i != 0) {
                buffer.append(',');
            }
            buffer.append(tethers.get(i));
        }
        return buffer.toString();
    }

    private void enableWifiTetheringIfNeeded() {
        if (!isHotspotEnabled(this)) {
            new WifiApEnabler(this, null).setSoftapEnabled(true);
        }
    }

    private void disableWifiTethering() {
        WifiApEnabler enabler = new WifiApEnabler(this, null);
        enabler.setSoftapEnabled(false);
    }

    private void disableUsbTethering() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        cm.setUsbTethering(false);
    }

    private void disableBtTethering() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceDisconnected(int profile) {
                }

                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    ((BluetoothPan) proxy).setBluetoothTethering(false);
                    adapter.closeProfileProxy(5, proxy);
                }
            }, 5);
        }
    }

    private void startProvisioning(int index) {
        String provisionAction = getResources().getString(android.R.string.paste_as_plain_text);
        if (DEBUG) {
            Log.d("TetherService", "Sending provisioning broadcast: " + provisionAction + " type: " + this.mCurrentTethers.get(index));
        }
        Intent intent = new Intent(provisionAction);
        intent.putExtra("TETHER_TYPE", this.mCurrentTethers.get(index));
        intent.setFlags(268435456);
        sendBroadcast(intent);
        this.mInProvisionCheck = true;
    }

    private static boolean isHotspotEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
        return wifiManager.getWifiApState() == 13;
    }

    public static void scheduleRecheckAlarm(Context context, int type) {
        Intent intent = new Intent(context, (Class<?>) TetherService.class);
        intent.putExtra("extraAddTetherType", type);
        intent.putExtra("extraSetAlarm", true);
        context.startService(intent);
    }

    private void scheduleAlarm() {
        Intent intent = new Intent(this, (Class<?>) TetherService.class);
        intent.putExtra("extraRunProvision", true);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService("alarm");
        int period = getResources().getInteger(android.R.integer.config_MaxConcurrentDownloadsAllowed);
        long periodMs = 3600000 * period;
        long firstTime = SystemClock.elapsedRealtime() + periodMs;
        if (DEBUG) {
            Log.d("TetherService", "Scheduling alarm at interval " + periodMs);
        }
        alarmManager.setRepeating(3, firstTime, periodMs, pendingIntent);
    }

    public static void cancelRecheckAlarmIfNecessary(Context context, int type) {
        Intent intent = new Intent(context, (Class<?>) TetherService.class);
        intent.putExtra("extraRemTetherType", type);
        context.startService(intent);
    }

    private void cancelAlarmIfNecessary() {
        if (this.mCurrentTethers.size() != 0) {
            if (DEBUG) {
                Log.d("TetherService", "Tethering still active, not cancelling alarm");
                return;
            }
            return;
        }
        Intent intent = new Intent(this, (Class<?>) TetherService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService("alarm");
        alarmManager.cancel(pendingIntent);
        if (DEBUG) {
            Log.d("TetherService", "Tethering no longer active, canceling recheck");
        }
    }
}
