package com.mediatek.amplus;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Slog;
import com.mediatek.common.jpe.a;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class PowerSavingUtils {
    private static final String FILEPATH = "/system/etc/alarmplus.config";
    private static final long MIN_FUZZABLE_INTERVAL = 10000;
    private static final int NEW_POWER_SAVING_MODE = 2;
    private static final long SCREENOFF_TIME_INTERVAL_THRESHOLD = 300000;
    private static final String TAG = "AlarmManager";
    private final Context mContext;
    private PowerSavingReceiver mPowerSavingReceiver;
    private boolean mScreenOff = false;
    private long mScreenOffTime = 0;
    private boolean mIsEnabled = false;
    private boolean mIsUsbConnected = false;
    private boolean mIsWFDConnected = false;
    private PowerSavingEnableObserver mPowerSavingEnableObserver = null;
    final ArrayList<String> mWhitelist = new ArrayList<>();
    private int mSavingMode = 0;

    public PowerSavingUtils(Context context) {
        this.mContext = context;
        init();
    }

    private void init() {
        new a().a();
        readList();
        this.mPowerSavingReceiver = new PowerSavingReceiver();
        this.mPowerSavingEnableObserver = new PowerSavingEnableObserver(null);
    }

    private void readList() {
        File file = new File(FILEPATH);
        if (!file.exists()) {
            return;
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                this.mWhitelist.add(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isAlarmNeedAlign(int i, PendingIntent pendingIntent, boolean z) {
        if (!isPowerSavingStart()) {
            return false;
        }
        if (this.mSavingMode != 2 && i != 0 && i != 2) {
            return false;
        }
        if (pendingIntent == null) {
            Slog.v(TAG, "isAlarmNeedAlign : operation is null");
            return false;
        }
        String targetPackage = pendingIntent.getTargetPackage();
        if (targetPackage == null) {
            Slog.v(TAG, "isAlarmNeedAlign : packageName is null");
            return false;
        }
        for (int i2 = 0; i2 < this.mWhitelist.size(); i2++) {
            if (this.mWhitelist.get(i2).equals(targetPackage)) {
                Slog.v(TAG, "isAlarmNeedAlign : packageName = " + targetPackage + "is in whitelist");
                return false;
            }
        }
        if (z) {
            PackageManager packageManager = this.mContext.getPackageManager();
            try {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(targetPackage, 0);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if ((applicationInfo.flags & 1) != 0 && (targetPackage.startsWith("com.android") || targetPackage.startsWith("android"))) {
                    if (!Build.TYPE.equals("eng")) {
                        return false;
                    }
                    Slog.v(TAG, "isAlarmNeedAlign : " + targetPackage + " skip!");
                    return false;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Slog.v(TAG, "isAlarmNeedAlign : packageName not fount");
                return false;
            }
        }
        return true;
    }

    private long getMTKMaxTriggerTime(int i, PendingIntent pendingIntent, long j, boolean z) {
        if (!z) {
            return j;
        }
        if (isAlarmNeedAlign(i, pendingIntent, true) && z) {
            return 300000 + j;
        }
        return 0 - j;
    }

    public boolean isPowerSavingStart() {
        if (!this.mIsEnabled || this.mIsUsbConnected || this.mIsWFDConnected || !this.mScreenOff) {
            return false;
        }
        if (System.currentTimeMillis() - this.mScreenOffTime >= (this.mSavingMode == 2 ? 60000L : 300000L)) {
            return true;
        }
        if (Build.TYPE.equals("eng")) {
            Slog.v(TAG, "mScreenOff time is not enough");
        }
        return false;
    }

    private long adjustMaxTriggerTime(long j, long j2, long j3, PendingIntent pendingIntent, int i, boolean z, boolean z2) {
        if (j3 == 0) {
            j3 = j2 - j;
        }
        if (!(j3 >= 10000)) {
            j3 = 0;
        }
        long j4 = j2 + ((long) (j3 * 0.75d));
        if (this.mSavingMode != 2) {
            if (!z) {
                return j4;
            }
            if (isAlarmNeedAlign(i, pendingIntent, true)) {
                if (!(j4 - j2 >= 300000)) {
                    return 300000 + j2;
                }
                return j4;
            }
            return 0 - j4;
        }
        if (isAlarmNeedAlign(i, pendingIntent, z2)) {
            if (!(j4 - j2 >= 300000)) {
                return 300000 + j2;
            }
        }
        return 0 - j4;
    }

    public long getMaxTriggerTime(int i, long j, long j2, long j3, PendingIntent pendingIntent, int i2, boolean z) {
        this.mSavingMode = i2;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        if (j2 == 0) {
            return getMTKMaxTriggerTime(i, pendingIntent, j, z);
        }
        if (j2 == -1) {
            return adjustMaxTriggerTime(jElapsedRealtime, j, j3, pendingIntent, i, z, false);
        }
        return j + j2;
    }

    private void setPowerSavingEnable() {
        this.mIsEnabled = Settings.System.getInt(this.mContext.getContentResolver(), "background_power_saving_enable", 1) != 0;
    }

    class PowerSavingReceiver extends BroadcastReceiver {
        public PowerSavingReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.SCREEN_OFF");
            intentFilter.addAction("android.intent.action.SCREEN_ON");
            intentFilter.addAction("android.hardware.usb.action.USB_STATE");
            intentFilter.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
            intentFilter.addAction("android.intent.action.TIME_TICK");
            PowerSavingUtils.this.mContext.registerReceiver(this, intentFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                PowerSavingUtils.this.mScreenOff = true;
                PowerSavingUtils.this.mScreenOffTime = System.currentTimeMillis();
                return;
            }
            if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                PowerSavingUtils.this.mScreenOff = false;
                PowerSavingUtils.this.mScreenOffTime = 0L;
                return;
            }
            if ("android.hardware.usb.action.USB_STATE".equals(intent.getAction())) {
                PowerSavingUtils.this.mIsUsbConnected = intent.getBooleanExtra("connected", false);
                return;
            }
            if ("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED".equals(intent.getAction())) {
                PowerSavingUtils.this.mIsWFDConnected = 2 == intent.getParcelableExtra("android.hardware.display.extra.WIFI_DISPLAY_STATUS").getActiveDisplayState();
                Slog.v(PowerSavingUtils.TAG, "PowerSavingReceiver mIsWFDConnected = " + PowerSavingUtils.this.mIsWFDConnected);
                return;
            }
            if ("android.intent.action.TIME_TICK".equals(intent.getAction())) {
                Slog.v(PowerSavingUtils.TAG, "isPowerSavingStart  mIsEnabled = " + PowerSavingUtils.this.mIsEnabled + "   mIsUsbConnected = " + PowerSavingUtils.this.mIsUsbConnected + "   mScreenOff = " + PowerSavingUtils.this.mScreenOff + "   mIsWFDConnected = " + PowerSavingUtils.this.mIsWFDConnected);
            }
        }
    }

    class PowerSavingEnableObserver extends ContentObserver {
        PowerSavingEnableObserver(Handler handler) {
            super(handler);
            observe();
        }

        void observe() {
            PowerSavingUtils.this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("background_power_saving_enable"), false, this, -1);
            PowerSavingUtils.this.setPowerSavingEnable();
        }

        @Override
        public void onChange(boolean z) {
            PowerSavingUtils.this.setPowerSavingEnable();
        }
    }
}
