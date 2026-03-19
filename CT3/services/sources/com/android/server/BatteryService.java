package com.android.server;

import android.R;
import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.BatteryManagerInternal;
import android.os.BatteryProperties;
import android.os.Binder;
import android.os.DropBoxManager;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBatteryPropertiesListener;
import android.os.IBatteryPropertiesRegistrar;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.IBatteryStats;
import com.android.server.am.BatteryStatsService;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public final class BatteryService extends SystemService {
    private static final int BATTERY_PLUGGED_NONE = 0;
    private static final int BATTERY_SCALE = 100;
    private static final boolean DEBUG = false;
    private static final String DUMPSYS_DATA_PATH = "/data/system/";
    private static final String IPO_POWER_OFF = "android.intent.action.ACTION_SHUTDOWN_IPO";
    private static final String IPO_POWER_ON = "android.intent.action.ACTION_BOOT_IPO";
    private boolean LowLevelFlag;
    private boolean ipo_led_off;
    private boolean ipo_led_on;
    private boolean mBatteryLevelCritical;
    private boolean mBatteryLevelLow;
    private BatteryProperties mBatteryProps;
    private final IBatteryStats mBatteryStats;
    BinderService mBinderService;
    private boolean mBootCompleted;
    private final Context mContext;
    private int mCriticalBatteryLevel;
    private int mDischargeStartLevel;
    private long mDischargeStartTime;
    private final Handler mHandler;
    private boolean mIPOBoot;
    private boolean mIPOShutdown;
    private boolean mIPOed;
    private int mInvalidCharger;
    private int mLastBatteryHealth;
    private int mLastBatteryLevel;
    private boolean mLastBatteryLevelCritical;
    private int mLastBatteryLevel_smb;
    private boolean mLastBatteryPresent;
    private boolean mLastBatteryPresent_smb;
    private final BatteryProperties mLastBatteryProps;
    private int mLastBatteryStatus;
    private int mLastBatteryStatus_smb;
    private int mLastBatteryTemperature;
    private int mLastBatteryVoltage;
    private int mLastChargeCounter;
    private int mLastInvalidCharger;
    private int mLastMaxChargingCurrent;
    private int mLastMaxChargingVoltage;
    private int mLastPlugType;
    private Led mLed;
    private final Object mLock;
    private int mLowBatteryCloseWarningLevel;
    private int mLowBatteryWarningLevel;
    private int mPlugType;
    private boolean mSentLowBatteryBroadcast;
    private int mShutdownBatteryTemperature;
    private boolean mUpdatesStopped;
    private static final String TAG = BatteryService.class.getSimpleName();
    private static final String[] DUMPSYS_ARGS = {"--checkin", "--unplugged"};

    public BatteryService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mLastBatteryProps = new BatteryProperties();
        this.mLastPlugType = -1;
        this.mSentLowBatteryBroadcast = false;
        this.mIPOShutdown = false;
        this.mIPOed = false;
        this.mIPOBoot = false;
        this.ipo_led_on = false;
        this.ipo_led_off = false;
        this.LowLevelFlag = false;
        this.mBootCompleted = false;
        this.mContext = context;
        this.mHandler = new Handler(true);
        this.mLed = new Led(context, (LightsManager) getLocalService(LightsManager.class));
        this.mBatteryStats = BatteryStatsService.getService();
        this.mCriticalBatteryLevel = this.mContext.getResources().getInteger(R.integer.config_datause_threshold_bytes);
        this.mLowBatteryWarningLevel = this.mContext.getResources().getInteger(R.integer.config_debugSystemServerPssThresholdBytes);
        this.mLowBatteryCloseWarningLevel = this.mLowBatteryWarningLevel + this.mContext.getResources().getInteger(R.integer.config_defaultActionModeHideDurationMillis);
        this.mShutdownBatteryTemperature = this.mContext.getResources().getInteger(R.integer.config_datause_throttle_kbitsps);
        if (!new File("/sys/devices/virtual/switch/invalid_charger/state").exists()) {
            return;
        }
        UEventObserver invalidChargerObserver = new UEventObserver() {
            public void onUEvent(UEventObserver.UEvent event) {
                int invalidCharger = "1".equals(event.get("SWITCH_STATE")) ? 1 : 0;
                synchronized (BatteryService.this.mLock) {
                    if (BatteryService.this.mInvalidCharger != invalidCharger) {
                        BatteryService.this.mInvalidCharger = invalidCharger;
                    }
                }
            }
        };
        invalidChargerObserver.startObserving("DEVPATH=/devices/virtual/switch/invalid_charger");
    }

    @Override
    public void onStart() {
        BinderService binderService = null;
        Object[] objArr = 0;
        try {
            IBatteryPropertiesRegistrar.Stub.asInterface(ServiceManager.getService("batteryproperties")).registerListener(new BatteryListener(this, null));
        } catch (RemoteException e) {
        }
        if (SystemProperties.get("ro.mtk_ipo_support").equals("1")) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.ACTION_BOOT_IPO");
            intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
            this.mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if ("android.intent.action.ACTION_BOOT_IPO".equals(intent.getAction())) {
                        BatteryService.this.mIPOShutdown = false;
                        BatteryService.this.mIPOBoot = true;
                        BatteryService.this.mLastBatteryLevel = BatteryService.this.mLowBatteryWarningLevel + 1;
                        BatteryService.this.update(BatteryService.this.mBatteryProps);
                        return;
                    }
                    if (!"android.intent.action.ACTION_SHUTDOWN_IPO".equals(intent.getAction())) {
                        return;
                    }
                    BatteryService.this.mIPOShutdown = true;
                }
            }, intentFilter);
        }
        this.mBinderService = new BinderService(this, binderService);
        publishBinderService("battery", this.mBinderService);
        publishLocalService(BatteryManagerInternal.class, new LocalService(this, objArr == true ? 1 : 0));
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase != 550) {
            return;
        }
        synchronized (this.mLock) {
            this.mBootCompleted = true;
            ContentObserver obs = new ContentObserver(this.mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    synchronized (BatteryService.this.mLock) {
                        BatteryService.this.updateBatteryWarningLevelLocked();
                    }
                }
            };
            ContentResolver resolver = this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Global.getUriFor("low_power_trigger_level"), false, obs, -1);
            updateBatteryWarningLevelLocked();
        }
    }

    private void updateBatteryWarningLevelLocked() throws Throwable {
        ContentResolver resolver = this.mContext.getContentResolver();
        int defWarnLevel = this.mContext.getResources().getInteger(R.integer.config_debugSystemServerPssThresholdBytes);
        this.mLowBatteryWarningLevel = Settings.Global.getInt(resolver, "low_power_trigger_level", defWarnLevel);
        if (this.mLowBatteryWarningLevel == 0) {
            this.mLowBatteryWarningLevel = defWarnLevel;
        }
        if (this.mLowBatteryWarningLevel < this.mCriticalBatteryLevel) {
            this.mLowBatteryWarningLevel = this.mCriticalBatteryLevel;
        }
        this.mLowBatteryCloseWarningLevel = this.mLowBatteryWarningLevel + this.mContext.getResources().getInteger(R.integer.config_defaultActionModeHideDurationMillis);
        processValuesLocked(true);
    }

    private boolean isPoweredLocked(int plugTypeSet) {
        if (this.mBatteryProps.batteryStatus == 1) {
            return true;
        }
        if ((plugTypeSet & 1) != 0 && this.mBatteryProps.chargerAcOnline) {
            return true;
        }
        if ((plugTypeSet & 2) == 0 || !this.mBatteryProps.chargerUsbOnline) {
            return (plugTypeSet & 4) != 0 && this.mBatteryProps.chargerWirelessOnline;
        }
        return true;
    }

    private boolean shouldSendBatteryLowLocked() {
        boolean plugged = this.mPlugType != 0;
        boolean oldPlugged = this.mLastPlugType != 0;
        if (plugged || this.mBatteryProps.batteryStatus == 1 || this.mBatteryProps.batteryLevel > this.mLowBatteryWarningLevel) {
            return false;
        }
        return oldPlugged || this.mLastBatteryLevel > this.mLowBatteryWarningLevel;
    }

    private void shutdownIfNoPowerLocked() {
        if (this.mBatteryProps.batteryLevel != 0 || isPoweredLocked(7)) {
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!ActivityManagerNative.isSystemReady()) {
                    return;
                }
                if (SystemProperties.get("ro.mtk_ipo_support").equals("1")) {
                    SystemProperties.set("sys.ipo.battlow", "1");
                }
                Intent intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
                intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                intent.setFlags(268435456);
                BatteryService.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            }
        });
    }

    private void shutdownIfOverTempLocked() {
        if (this.mBatteryProps.batteryTemperature <= this.mShutdownBatteryTemperature) {
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!ActivityManagerNative.isSystemReady()) {
                    return;
                }
                Intent intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
                intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                intent.setFlags(268435456);
                BatteryService.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            }
        });
    }

    private void update(BatteryProperties props) {
        synchronized (this.mLock) {
            if (!this.mUpdatesStopped) {
                this.mBatteryProps = props;
                if (SystemProperties.get("ro.mtk_ipo_support").equals("1") && this.mIPOShutdown) {
                    return;
                }
                if (this.mBootCompleted) {
                    processValuesLocked(false);
                }
            } else {
                this.mLastBatteryProps.set(props);
            }
        }
    }

    private void processValuesLocked(boolean force) throws Throwable {
        boolean logOutlier = false;
        long dischargeDuration = 0;
        this.mBatteryLevelCritical = this.mBatteryProps.batteryLevel <= this.mCriticalBatteryLevel;
        if (this.mBatteryProps.chargerAcOnline) {
            this.mPlugType = 1;
        } else if (this.mBatteryProps.chargerUsbOnline) {
            this.mPlugType = 2;
        } else if (this.mBatteryProps.chargerWirelessOnline) {
            this.mPlugType = 4;
        } else {
            this.mPlugType = 0;
        }
        if (SystemProperties.get("ro.mtk_diso_support").equals("true") && this.mBatteryProps.chargerAcOnline && this.mBatteryProps.chargerUsbOnline) {
            this.mPlugType = 3;
        }
        if (this.mLastBatteryVoltage != this.mBatteryProps.batteryVoltage) {
            Log.d(TAG, "mBatteryVoltage=" + this.mBatteryProps.batteryVoltage + ", batteryLevel=" + this.mBatteryProps.batteryLevel_smb);
        }
        this.mLed.updateLightsLocked();
        try {
            this.mBatteryStats.setBatteryState(this.mBatteryProps.batteryStatus, this.mBatteryProps.batteryHealth, this.mPlugType, this.mBatteryProps.batteryLevel, this.mBatteryProps.batteryTemperature, this.mBatteryProps.batteryVoltage, this.mBatteryProps.batteryChargeCounter);
        } catch (RemoteException e) {
        }
        shutdownIfNoPowerLocked();
        shutdownIfOverTempLocked();
        if (!force && this.mBatteryProps.batteryStatus == this.mLastBatteryStatus && this.mBatteryProps.batteryStatus_smb == this.mLastBatteryStatus_smb && this.mBatteryProps.batteryHealth == this.mLastBatteryHealth && this.mBatteryProps.batteryPresent == this.mLastBatteryPresent && this.mBatteryProps.batteryPresent_smb == this.mLastBatteryPresent_smb && this.mBatteryProps.batteryLevel == this.mLastBatteryLevel && this.mBatteryProps.batteryLevel_smb == this.mLastBatteryLevel_smb && this.mPlugType == this.mLastPlugType && this.mBatteryProps.batteryVoltage == this.mLastBatteryVoltage && this.mBatteryProps.batteryTemperature == this.mLastBatteryTemperature && this.mBatteryProps.maxChargingCurrent == this.mLastMaxChargingCurrent && this.mBatteryProps.maxChargingVoltage == this.mLastMaxChargingVoltage && this.mBatteryProps.batteryChargeCounter == this.mLastChargeCounter && this.mInvalidCharger == this.mLastInvalidCharger) {
            return;
        }
        if (this.mPlugType != this.mLastPlugType) {
            if (this.mLastPlugType == 0) {
                if (this.mDischargeStartTime != 0 && this.mDischargeStartLevel != this.mBatteryProps.batteryLevel) {
                    dischargeDuration = SystemClock.elapsedRealtime() - this.mDischargeStartTime;
                    logOutlier = true;
                    EventLog.writeEvent(EventLogTags.BATTERY_DISCHARGE, Long.valueOf(dischargeDuration), Integer.valueOf(this.mDischargeStartLevel), Integer.valueOf(this.mBatteryProps.batteryLevel));
                    this.mDischargeStartTime = 0L;
                }
            } else if (this.mPlugType == 0) {
                this.mDischargeStartTime = SystemClock.elapsedRealtime();
                this.mDischargeStartLevel = this.mBatteryProps.batteryLevel;
            }
        }
        if (this.mBatteryProps.batteryStatus != this.mLastBatteryStatus || this.mBatteryProps.batteryHealth != this.mLastBatteryHealth || this.mBatteryProps.batteryPresent != this.mLastBatteryPresent || this.mPlugType != this.mLastPlugType) {
            Object[] objArr = new Object[5];
            objArr[0] = Integer.valueOf(this.mBatteryProps.batteryStatus);
            objArr[1] = Integer.valueOf(this.mBatteryProps.batteryHealth);
            objArr[2] = Integer.valueOf(this.mBatteryProps.batteryPresent ? 1 : 0);
            objArr[3] = Integer.valueOf(this.mPlugType);
            objArr[4] = this.mBatteryProps.batteryTechnology;
            EventLog.writeEvent(EventLogTags.BATTERY_STATUS, objArr);
        }
        if (this.mBatteryProps.batteryLevel != this.mLastBatteryLevel) {
            EventLog.writeEvent(EventLogTags.BATTERY_LEVEL, Integer.valueOf(this.mBatteryProps.batteryLevel), Integer.valueOf(this.mBatteryProps.batteryVoltage), Integer.valueOf(this.mBatteryProps.batteryTemperature));
        }
        if (this.mBatteryLevelCritical && !this.mLastBatteryLevelCritical && this.mPlugType == 0) {
            dischargeDuration = SystemClock.elapsedRealtime() - this.mDischargeStartTime;
            logOutlier = true;
        }
        if (!this.mBatteryLevelLow) {
            if (this.mPlugType == 0 && this.mBatteryProps.batteryLevel <= this.mLowBatteryWarningLevel) {
                this.mBatteryLevelLow = true;
            }
        } else if (this.mPlugType != 0 || this.mBatteryProps.batteryLevel >= this.mLowBatteryCloseWarningLevel) {
            this.mBatteryLevelLow = false;
        } else if (force && this.mBatteryProps.batteryLevel >= this.mLowBatteryWarningLevel) {
            this.mBatteryLevelLow = false;
        }
        sendIntentLocked();
        if (this.mPlugType != 0 && this.mLastPlugType == 0) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Intent statusIntent = new Intent("android.intent.action.ACTION_POWER_CONNECTED");
                    statusIntent.setFlags(67108864);
                    BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                }
            });
        } else if (this.mPlugType == 0 && this.mLastPlugType != 0) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Intent statusIntent = new Intent("android.intent.action.ACTION_POWER_DISCONNECTED");
                    statusIntent.setFlags(67108864);
                    BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                }
            });
        }
        if (shouldSendBatteryLowLocked()) {
            this.mSentLowBatteryBroadcast = true;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Intent statusIntent = new Intent("android.intent.action.BATTERY_LOW");
                    statusIntent.setFlags(67108864);
                    BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                }
            });
        } else if (this.mSentLowBatteryBroadcast && this.mLastBatteryLevel >= this.mLowBatteryCloseWarningLevel) {
            this.mSentLowBatteryBroadcast = false;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Intent statusIntent = new Intent("android.intent.action.BATTERY_OKAY");
                    statusIntent.setFlags(67108864);
                    BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                }
            });
        }
        if (this.mBatteryProps.batteryStatus != this.mLastBatteryStatus && this.mBatteryProps.batteryStatus == 6) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(BatteryService.TAG, "sendBroadcast ACTION_IGNORE_DATA_USAGE_ALERT");
                    Intent statusIntent = new Intent("android.intent.action.IGNORE_DATA_USAGE_ALERT");
                    statusIntent.addFlags(536870912);
                    BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                }
            });
        }
        if (logOutlier && dischargeDuration != 0) {
            logOutlierLocked(dischargeDuration);
        }
        this.mLastBatteryStatus = this.mBatteryProps.batteryStatus;
        this.mLastBatteryStatus_smb = this.mBatteryProps.batteryStatus_smb;
        this.mLastBatteryHealth = this.mBatteryProps.batteryHealth;
        this.mLastBatteryPresent = this.mBatteryProps.batteryPresent;
        this.mLastBatteryPresent_smb = this.mBatteryProps.batteryPresent_smb;
        this.mLastBatteryLevel = this.mBatteryProps.batteryLevel;
        this.mLastBatteryLevel_smb = this.mBatteryProps.batteryLevel_smb;
        this.mLastPlugType = this.mPlugType;
        this.mLastBatteryVoltage = this.mBatteryProps.batteryVoltage;
        this.mLastBatteryTemperature = this.mBatteryProps.batteryTemperature;
        this.mLastMaxChargingCurrent = this.mBatteryProps.maxChargingCurrent;
        this.mLastMaxChargingVoltage = this.mBatteryProps.maxChargingVoltage;
        this.mLastChargeCounter = this.mBatteryProps.batteryChargeCounter;
        this.mLastBatteryLevelCritical = this.mBatteryLevelCritical;
        this.mLastInvalidCharger = this.mInvalidCharger;
    }

    private void sendIntentLocked() {
        final Intent intent = new Intent("android.intent.action.BATTERY_CHANGED");
        intent.addFlags(1610612736);
        int icon = getIconLocked(this.mBatteryProps.batteryLevel);
        intent.putExtra("status", this.mBatteryProps.batteryStatus);
        intent.putExtra("status_2nd", this.mBatteryProps.batteryStatus_smb);
        intent.putExtra("health", this.mBatteryProps.batteryHealth);
        intent.putExtra("present", this.mBatteryProps.batteryPresent);
        intent.putExtra("present_2nd", this.mBatteryProps.batteryPresent_smb);
        intent.putExtra("level", this.mBatteryProps.batteryLevel);
        intent.putExtra("level_2nd", this.mBatteryProps.batteryLevel_smb);
        intent.putExtra("scale", 100);
        intent.putExtra("icon-small", icon);
        intent.putExtra("plugged", this.mPlugType);
        intent.putExtra("voltage", this.mBatteryProps.batteryVoltage);
        intent.putExtra("temperature", this.mBatteryProps.batteryTemperature);
        intent.putExtra("technology", this.mBatteryProps.batteryTechnology);
        intent.putExtra("invalid_charger", this.mInvalidCharger);
        intent.putExtra("max_charging_current", this.mBatteryProps.maxChargingCurrent);
        intent.putExtra("max_charging_voltage", this.mBatteryProps.maxChargingVoltage);
        intent.putExtra("charge_counter", this.mBatteryProps.batteryChargeCounter);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                ActivityManagerNative.broadcastStickyIntent(intent, (String) null, -1);
            }
        });
    }

    private void logBatteryStatsLocked() throws Throwable {
        DropBoxManager db;
        IBinder batteryInfoService = ServiceManager.getService("batterystats");
        if (batteryInfoService == null || (db = (DropBoxManager) this.mContext.getSystemService("dropbox")) == null || !db.isTagEnabled("BATTERY_DISCHARGE_INFO")) {
            return;
        }
        File dumpFile = null;
        FileOutputStream dumpStream = null;
        try {
            try {
                File dumpFile2 = new File("/data/system/batterystats.dump");
                try {
                    FileOutputStream dumpStream2 = new FileOutputStream(dumpFile2);
                    try {
                        batteryInfoService.dump(dumpStream2.getFD(), DUMPSYS_ARGS);
                        FileUtils.sync(dumpStream2);
                        db.addFile("BATTERY_DISCHARGE_INFO", dumpFile2, 2);
                        if (dumpStream2 != null) {
                            try {
                                dumpStream2.close();
                            } catch (IOException e) {
                                Slog.e(TAG, "failed to close dumpsys output stream");
                            }
                        }
                        if (dumpFile2 != null && !dumpFile2.delete()) {
                            Slog.e(TAG, "failed to delete temporary dumpsys file: " + dumpFile2.getAbsolutePath());
                        }
                        dumpStream = dumpStream2;
                        dumpFile = dumpFile2;
                    } catch (RemoteException e2) {
                        e = e2;
                        dumpStream = dumpStream2;
                        dumpFile = dumpFile2;
                        Slog.e(TAG, "failed to dump battery service", e);
                        if (dumpStream != null) {
                            try {
                                dumpStream.close();
                            } catch (IOException e3) {
                                Slog.e(TAG, "failed to close dumpsys output stream");
                            }
                        }
                        if (dumpFile != null && !dumpFile.delete()) {
                            Slog.e(TAG, "failed to delete temporary dumpsys file: " + dumpFile.getAbsolutePath());
                        }
                    } catch (IOException e4) {
                        e = e4;
                        dumpStream = dumpStream2;
                        dumpFile = dumpFile2;
                        Slog.e(TAG, "failed to write dumpsys file", e);
                        if (dumpStream != null) {
                            try {
                                dumpStream.close();
                            } catch (IOException e5) {
                                Slog.e(TAG, "failed to close dumpsys output stream");
                            }
                        }
                        if (dumpFile != null && !dumpFile.delete()) {
                            Slog.e(TAG, "failed to delete temporary dumpsys file: " + dumpFile.getAbsolutePath());
                        }
                    } catch (Throwable th) {
                        th = th;
                        dumpStream = dumpStream2;
                        dumpFile = dumpFile2;
                        if (dumpStream != null) {
                            try {
                                dumpStream.close();
                            } catch (IOException e6) {
                                Slog.e(TAG, "failed to close dumpsys output stream");
                            }
                        }
                        if (dumpFile != null && !dumpFile.delete()) {
                            Slog.e(TAG, "failed to delete temporary dumpsys file: " + dumpFile.getAbsolutePath());
                        }
                        throw th;
                    }
                } catch (RemoteException e7) {
                    e = e7;
                    dumpFile = dumpFile2;
                } catch (IOException e8) {
                    e = e8;
                    dumpFile = dumpFile2;
                } catch (Throwable th2) {
                    th = th2;
                    dumpFile = dumpFile2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (RemoteException e9) {
            e = e9;
        } catch (IOException e10) {
            e = e10;
        }
    }

    private void logOutlierLocked(long duration) throws Throwable {
        ContentResolver cr = this.mContext.getContentResolver();
        String dischargeThresholdString = Settings.Global.getString(cr, "battery_discharge_threshold");
        String durationThresholdString = Settings.Global.getString(cr, "battery_discharge_duration_threshold");
        if (dischargeThresholdString == null || durationThresholdString == null) {
            return;
        }
        try {
            long durationThreshold = Long.parseLong(durationThresholdString);
            int dischargeThreshold = Integer.parseInt(dischargeThresholdString);
            if (duration > durationThreshold || this.mDischargeStartLevel - this.mBatteryProps.batteryLevel < dischargeThreshold) {
                return;
            }
            logBatteryStatsLocked();
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Invalid DischargeThresholds GService string: " + durationThresholdString + " or " + dischargeThresholdString);
        }
    }

    private int getIconLocked(int level) {
        if (this.mBatteryProps.batteryStatus == 2) {
            return R.drawable.list_selector_multiselect_holo_dark;
        }
        if (this.mBatteryProps.batteryStatus == 3) {
            return R.drawable.list_selector_background_pressed;
        }
        if (this.mBatteryProps.batteryStatus == 4 || this.mBatteryProps.batteryStatus == 5) {
            return (!isPoweredLocked(7) || this.mBatteryProps.batteryLevel < 100) ? R.drawable.list_selector_background_pressed : R.drawable.list_selector_multiselect_holo_dark;
        }
        return R.drawable.media_button_background;
    }

    class Shell extends ShellCommand {
        Shell() {
        }

        public int onCommand(String cmd) {
            return BatteryService.this.onShellCommand(this, cmd);
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            BatteryService.dumpHelp(pw);
        }
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Battery service (battery) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  set [ac|usb|wireless|status|level|invalid] <value>");
        pw.println("    Force a battery property value, freezing battery state.");
        pw.println("  unplug");
        pw.println("    Force battery unplugged, freezing battery state.");
        pw.println("  reset");
        pw.println("    Unfreeze battery state, returning to current hardware values.");
    }

    private void dumpInternal(PrintWriter pw, String[] args) {
        synchronized (this.mLock) {
            if (args != null) {
                if (args.length == 0 || "-a".equals(args[0])) {
                }
            }
            pw.println("Current Battery Service state:");
            if (this.mUpdatesStopped) {
                pw.println("  (UPDATES STOPPED -- use 'reset' to restart)");
            }
            pw.println("  AC powered: " + this.mBatteryProps.chargerAcOnline);
            pw.println("  USB powered: " + this.mBatteryProps.chargerUsbOnline);
            pw.println("  Wireless powered: " + this.mBatteryProps.chargerWirelessOnline);
            pw.println("  Max charging current: " + this.mBatteryProps.maxChargingCurrent);
            pw.println("  status: " + this.mBatteryProps.batteryStatus);
            pw.println("  status: " + this.mBatteryProps.batteryStatus_smb);
            pw.println("  health: " + this.mBatteryProps.batteryHealth);
            pw.println("  present: " + this.mBatteryProps.batteryPresent);
            pw.println("  present: " + this.mBatteryProps.batteryPresent_smb);
            pw.println("  level: " + this.mBatteryProps.batteryLevel);
            pw.println("  level: " + this.mBatteryProps.batteryLevel_smb);
            pw.println("  scale: 100");
            pw.println("  voltage: " + this.mBatteryProps.batteryVoltage);
            pw.println("  temperature: " + this.mBatteryProps.batteryTemperature);
            pw.println("  technology: " + this.mBatteryProps.batteryTechnology);
        }
    }

    int onShellCommand(Shell shell, String cmd) {
        long ident;
        if (cmd == null) {
            return shell.handleDefaultCommands(cmd);
        }
        PrintWriter pw = shell.getOutPrintWriter();
        if (cmd.equals("unplug")) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            if (!this.mUpdatesStopped) {
                this.mLastBatteryProps.set(this.mBatteryProps);
            }
            this.mBatteryProps.chargerAcOnline = false;
            this.mBatteryProps.chargerUsbOnline = false;
            this.mBatteryProps.chargerWirelessOnline = false;
            ident = Binder.clearCallingIdentity();
            try {
                this.mUpdatesStopped = true;
                processValuesLocked(false);
            } finally {
            }
        } else if (cmd.equals("set")) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            String key = shell.getNextArg();
            if (key == null) {
                pw.println("No property specified");
                return -1;
            }
            String value = shell.getNextArg();
            if (value == null) {
                pw.println("No value specified");
                return -1;
            }
            try {
                if (!this.mUpdatesStopped) {
                    this.mLastBatteryProps.set(this.mBatteryProps);
                }
                boolean update = true;
                if (key.equals("ac")) {
                    this.mBatteryProps.chargerAcOnline = Integer.parseInt(value) != 0;
                } else if (key.equals("usb")) {
                    this.mBatteryProps.chargerUsbOnline = Integer.parseInt(value) != 0;
                } else if (key.equals("wireless")) {
                    this.mBatteryProps.chargerWirelessOnline = Integer.parseInt(value) != 0;
                } else if (key.equals("status")) {
                    this.mBatteryProps.batteryStatus = Integer.parseInt(value);
                } else if (key.equals("level")) {
                    this.mBatteryProps.batteryLevel = Integer.parseInt(value);
                } else if (key.equals("invalid")) {
                    this.mInvalidCharger = Integer.parseInt(value);
                } else {
                    pw.println("Unknown set option: " + key);
                    update = false;
                }
                if ("ac".equals(key)) {
                    this.mBatteryProps.chargerAcOnline = Integer.parseInt(value) != 0;
                } else if ("usb".equals(key)) {
                    this.mBatteryProps.chargerUsbOnline = Integer.parseInt(value) != 0;
                } else if ("wireless".equals(key)) {
                    this.mBatteryProps.chargerWirelessOnline = Integer.parseInt(value) != 0;
                } else if ("status".equals(key)) {
                    this.mBatteryProps.batteryStatus = Integer.parseInt(value);
                } else if ("status_smb".equals(key)) {
                    this.mBatteryProps.batteryStatus_smb = Integer.parseInt(value);
                } else if ("level".equals(key)) {
                    this.mBatteryProps.batteryLevel = Integer.parseInt(value);
                } else if ("level_smb".equals(key)) {
                    this.mBatteryProps.batteryLevel_smb = Integer.parseInt(value);
                } else if ("invalid".equals(key)) {
                    this.mInvalidCharger = Integer.parseInt(value);
                } else {
                    pw.println("Unknown set option: " + key);
                    update = false;
                }
                if (update) {
                    ident = Binder.clearCallingIdentity();
                    try {
                        this.mUpdatesStopped = true;
                        processValuesLocked(false);
                    } finally {
                    }
                }
            } catch (NumberFormatException e) {
                pw.println("Bad value: " + value);
                return -1;
            }
        } else {
            if (!cmd.equals("reset")) {
                return shell.handleDefaultCommands(cmd);
            }
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            ident = Binder.clearCallingIdentity();
            try {
                if (this.mUpdatesStopped) {
                    this.mUpdatesStopped = false;
                    this.mBatteryProps.set(this.mLastBatteryProps);
                    processValuesLocked(false);
                }
            } finally {
            }
        }
        return 0;
    }

    private void dumpInternal(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mLock) {
            if (args != null) {
                if (args.length == 0 || "-a".equals(args[0])) {
                    pw.println("Current Battery Service state:");
                    if (this.mUpdatesStopped) {
                        pw.println("  (UPDATES STOPPED -- use 'reset' to restart)");
                    }
                    pw.println("  AC powered: " + this.mBatteryProps.chargerAcOnline);
                    pw.println("  USB powered: " + this.mBatteryProps.chargerUsbOnline);
                    pw.println("  Wireless powered: " + this.mBatteryProps.chargerWirelessOnline);
                    pw.println("  Max charging current: " + this.mBatteryProps.maxChargingCurrent);
                    pw.println("  Max charging voltage: " + this.mBatteryProps.maxChargingVoltage);
                    pw.println("  Charge counter: " + this.mBatteryProps.batteryChargeCounter);
                    pw.println("  status: " + this.mBatteryProps.batteryStatus);
                    pw.println("  health: " + this.mBatteryProps.batteryHealth);
                    pw.println("  present: " + this.mBatteryProps.batteryPresent);
                    pw.println("  level: " + this.mBatteryProps.batteryLevel);
                    pw.println("  scale: 100");
                    pw.println("  voltage: " + this.mBatteryProps.batteryVoltage);
                    pw.println("  temperature: " + this.mBatteryProps.batteryTemperature);
                    pw.println("  technology: " + this.mBatteryProps.batteryTechnology);
                } else {
                    Shell shell = new Shell();
                    shell.exec(this.mBinderService, null, fd, null, args, new ResultReceiver(null));
                }
            }
        }
    }

    private final class Led {
        private final int mBatteryFullARGB;
        private final int mBatteryLedOff;
        private final int mBatteryLedOn;
        private final Light mBatteryLight;
        private final int mBatteryLowARGB;
        private final int mBatteryMediumARGB;

        public Led(Context context, LightsManager lights) {
            this.mBatteryLight = lights.getLight(3);
            this.mBatteryLowARGB = context.getResources().getInteger(R.integer.config_defaultBinderHeavyHitterAutoSamplerBatchSize);
            this.mBatteryMediumARGB = context.getResources().getInteger(R.integer.config_defaultBinderHeavyHitterWatcherBatchSize);
            this.mBatteryFullARGB = context.getResources().getInteger(R.integer.config_defaultDisplayDefaultColorMode);
            this.mBatteryLedOn = context.getResources().getInteger(R.integer.config_defaultHapticFeedbackIntensity);
            this.mBatteryLedOff = context.getResources().getInteger(R.integer.config_defaultKeyboardVibrationIntensity);
        }

        public void updateLightsLocked() {
            int level = BatteryService.this.mBatteryProps.batteryLevel;
            int status = BatteryService.this.mBatteryProps.batteryStatus;
            if (BatteryService.this.mIPOBoot) {
                getIpoLedStatus();
            }
            if (level < BatteryService.this.mLowBatteryWarningLevel) {
                if (status == 2) {
                    updateLedStatus();
                    this.mBatteryLight.setColor(this.mBatteryLowARGB);
                    return;
                } else {
                    BatteryService.this.LowLevelFlag = true;
                    updateLedStatus();
                    this.mBatteryLight.setFlashing(this.mBatteryLowARGB, 1, this.mBatteryLedOn, this.mBatteryLedOff);
                    return;
                }
            }
            if (status == 2 || status == 5) {
                if (status == 5 || level >= 90) {
                    updateLedStatus();
                    this.mBatteryLight.setColor(this.mBatteryFullARGB);
                    return;
                } else {
                    updateLedStatus();
                    this.mBatteryLight.setColor(this.mBatteryMediumARGB);
                    return;
                }
            }
            if (BatteryService.this.ipo_led_on && BatteryService.this.mIPOBoot) {
                if (status == 5 || level >= 90) {
                    this.mBatteryLight.setColor(this.mBatteryFullARGB);
                } else {
                    this.mBatteryLight.setColor(this.mBatteryMediumARGB);
                }
                BatteryService.this.mIPOBoot = false;
                BatteryService.this.ipo_led_on = false;
            }
            this.mBatteryLight.turnOff();
        }

        private void getIpoLedStatus() {
            if ("1".equals(SystemProperties.get("sys.ipo.ledon"))) {
                BatteryService.this.ipo_led_on = true;
            } else {
                if (!"0".equals(SystemProperties.get("sys.ipo.ledon"))) {
                    return;
                }
                BatteryService.this.ipo_led_off = true;
            }
        }

        private void updateLedStatus() {
            if ((!BatteryService.this.ipo_led_off || !BatteryService.this.mIPOBoot) && (!BatteryService.this.LowLevelFlag || !BatteryService.this.mIPOBoot)) {
                return;
            }
            this.mBatteryLight.turnOff();
            BatteryService.this.mIPOBoot = false;
            BatteryService.this.ipo_led_off = false;
            BatteryService.this.ipo_led_on = false;
        }
    }

    private final class BatteryListener extends IBatteryPropertiesListener.Stub {
        BatteryListener(BatteryService this$0, BatteryListener batteryListener) {
            this();
        }

        private BatteryListener() {
        }

        public void batteryPropertiesChanged(BatteryProperties props) {
            long identity = Binder.clearCallingIdentity();
            try {
                BatteryService.this.update(props);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private final class BinderService extends Binder {
        BinderService(BatteryService this$0, BinderService binderService) {
            this();
        }

        private BinderService() {
        }

        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (BatteryService.this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump Battery service from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            } else {
                BatteryService.this.dumpInternal(fd, pw, args);
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) {
            BatteryService.this.new Shell().exec(this, in, out, err, args, resultReceiver);
        }
    }

    private final class LocalService extends BatteryManagerInternal {
        LocalService(BatteryService this$0, LocalService localService) {
            this();
        }

        private LocalService() {
        }

        public boolean isPowered(int plugTypeSet) {
            boolean zIsPoweredLocked;
            synchronized (BatteryService.this.mLock) {
                zIsPoweredLocked = BatteryService.this.isPoweredLocked(plugTypeSet);
            }
            return zIsPoweredLocked;
        }

        public int getPlugType() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mPlugType;
            }
            return i;
        }

        public int getBatteryLevel() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mBatteryProps.batteryLevel;
            }
            return i;
        }

        public boolean getBatteryLevelLow() {
            boolean z;
            synchronized (BatteryService.this.mLock) {
                z = BatteryService.this.mBatteryLevelLow;
            }
            return z;
        }

        public int getInvalidCharger() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mInvalidCharger;
            }
            return i;
        }
    }
}
