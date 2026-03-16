package com.android.server;

import android.R;
import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.app.IBatteryStats;
import com.android.server.am.BatteryStatsService;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public final class BatteryService extends SystemService {
    private static final int BATTERY_PLUGGED_NONE = 0;
    private static final int BATTERY_SCALE = 100;
    private static final boolean DEBUG = false;
    private static final String DUMPSYS_DATA_PATH = "/data/system/";
    private static final int DUMP_MAX_LENGTH = 24576;
    private static final String LOW_BATTERY_FILE = "/sys/class/backlight/lcd-bl/device/lowbattery";
    private boolean mBatteryLevelCritical;
    private boolean mBatteryLevelLow;
    private BatteryProperties mBatteryProps;
    private final IBatteryStats mBatteryStats;
    private final Context mContext;
    private int mCriticalBatteryLevel;
    private int mDischargeStartLevel;
    private long mDischargeStartTime;
    private final Handler mHandler;
    private int mInvalidCharger;
    private final UEventObserver mInvalidChargerObserver;
    private int mLastBatteryHealth;
    private int mLastBatteryLevel;
    private boolean mLastBatteryLevelCritical;
    private boolean mLastBatteryPresent;
    private final BatteryProperties mLastBatteryProps;
    private int mLastBatteryStatus;
    private int mLastBatteryTemperature;
    private int mLastBatteryVoltage;
    private int mLastInvalidCharger;
    private int mLastPlugType;
    private int mLastPlugType_all;
    private Led mLed;
    private final Object mLock;
    private int mLowBatteryCloseWarningLevel;
    private int mLowBatteryWarningLevel;
    private int mPlugType;
    private int mPlugType_all;
    private Runnable mSendBatteryChanged;
    private boolean mSentLowBatteryBroadcast;
    private int mShutdownBatteryTemperature;
    private boolean mUpdatesStopped;
    private static final String TAG = BatteryService.class.getSimpleName();
    private static final String[] DUMPSYS_ARGS = {"--checkin", "--unplugged"};

    public BatteryService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mLastBatteryProps = new BatteryProperties();
        this.mLastPlugType_all = -1;
        this.mLastPlugType = -1;
        this.mSentLowBatteryBroadcast = DEBUG;
        this.mInvalidChargerObserver = new UEventObserver() {
            public void onUEvent(UEventObserver.UEvent event) {
                int invalidCharger = "1".equals(event.get("SWITCH_STATE")) ? 1 : 0;
                synchronized (BatteryService.this.mLock) {
                    if (BatteryService.this.mInvalidCharger != invalidCharger) {
                        BatteryService.this.mInvalidCharger = invalidCharger;
                    }
                }
            }
        };
        this.mContext = context;
        this.mHandler = new Handler(true);
        this.mLed = new Led(context, (LightsManager) getLocalService(LightsManager.class));
        this.mBatteryStats = BatteryStatsService.getService();
        this.mCriticalBatteryLevel = this.mContext.getResources().getInteger(R.integer.config_carDockKeepsScreenOn);
        this.mLowBatteryWarningLevel = this.mContext.getResources().getInteger(R.integer.config_cdma_3waycall_flash_delay);
        this.mLowBatteryCloseWarningLevel = this.mLowBatteryWarningLevel + this.mContext.getResources().getInteger(R.integer.config_chooser_max_targets_per_row);
        this.mShutdownBatteryTemperature = this.mContext.getResources().getInteger(R.integer.config_carDockRotation);
        if (new File("/sys/devices/virtual/switch/invalid_charger/state").exists()) {
            this.mInvalidChargerObserver.startObserving("DEVPATH=/devices/virtual/switch/invalid_charger");
        }
    }

    @Override
    public void onStart() {
        IBinder b = ServiceManager.getService("batteryproperties");
        IBatteryPropertiesRegistrar batteryPropertiesRegistrar = IBatteryPropertiesRegistrar.Stub.asInterface(b);
        try {
            batteryPropertiesRegistrar.registerListener(new BatteryListener());
        } catch (RemoteException e) {
        }
        publishBinderService("battery", new BinderService());
        publishLocalService(BatteryManagerInternal.class, new LocalService());
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == 550) {
            synchronized (this.mLock) {
                ContentObserver obs = new ContentObserver(this.mHandler) {
                    @Override
                    public void onChange(boolean selfChange) {
                        synchronized (BatteryService.this.mLock) {
                            BatteryService.this.updateBatteryWarningLevelLocked();
                        }
                    }
                };
                ContentResolver resolver = this.mContext.getContentResolver();
                resolver.registerContentObserver(Settings.Global.getUriFor("low_power_trigger_level"), DEBUG, obs, -1);
                updateBatteryWarningLevelLocked();
            }
        }
    }

    private void updateBatteryWarningLevelLocked() throws Throwable {
        ContentResolver resolver = this.mContext.getContentResolver();
        int defWarnLevel = this.mContext.getResources().getInteger(R.integer.config_cdma_3waycall_flash_delay);
        this.mLowBatteryWarningLevel = Settings.Global.getInt(resolver, "low_power_trigger_level", defWarnLevel);
        if (this.mLowBatteryWarningLevel == 0) {
            this.mLowBatteryWarningLevel = defWarnLevel;
        }
        if (this.mLowBatteryWarningLevel < this.mCriticalBatteryLevel) {
            this.mLowBatteryWarningLevel = this.mCriticalBatteryLevel;
        }
        this.mLowBatteryCloseWarningLevel = this.mLowBatteryWarningLevel + this.mContext.getResources().getInteger(R.integer.config_chooser_max_targets_per_row);
        processValuesLocked(true);
    }

    private boolean isPoweredLocked(int plugTypeSet) {
        if (this.mBatteryProps.batteryStatus == 1) {
            return true;
        }
        if ((plugTypeSet & 1) != 0 && this.mBatteryProps.chargerAcOnline) {
            return true;
        }
        if ((plugTypeSet & 2) != 0 && this.mBatteryProps.chargerUsbOnline) {
            return true;
        }
        if ((plugTypeSet & 4) == 0 || !this.mBatteryProps.chargerWirelessOnline) {
            return DEBUG;
        }
        return true;
    }

    private boolean shouldSendBatteryLowLocked() {
        boolean plugged = this.mPlugType != 0;
        boolean oldPlugged = this.mLastPlugType != 0;
        if (plugged || this.mBatteryProps.batteryStatus == 1 || this.mBatteryProps.batteryLevel > this.mLowBatteryWarningLevel || (!oldPlugged && this.mLastBatteryLevel <= this.mLowBatteryWarningLevel)) {
            return DEBUG;
        }
        return true;
    }

    private void shutdownIfNoPowerLocked() {
        if (this.mBatteryProps.batteryLevel == 0 && !isPoweredLocked(7)) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (ActivityManagerNative.isSystemReady()) {
                        Intent intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
                        intent.putExtra("android.intent.extra.KEY_CONFIRM", BatteryService.DEBUG);
                        intent.setFlags(268435456);
                        BatteryService.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    }
                }
            });
        }
    }

    private void shutdownIfOverTempLocked() {
        if (this.mBatteryProps.batteryTemperature > this.mShutdownBatteryTemperature) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (ActivityManagerNative.isSystemReady()) {
                        Intent intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
                        intent.putExtra("android.intent.extra.KEY_CONFIRM", BatteryService.DEBUG);
                        intent.setFlags(268435456);
                        BatteryService.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    }
                }
            });
        }
    }

    private void setLowbattery(int val) throws Throwable {
        FileOutputStream fileOutputStream;
        FileOutputStream fileOutputStream2 = null;
        if (val == 0 || val == 1) {
            try {
                try {
                    File f = new File(LOW_BATTERY_FILE);
                    fileOutputStream = new FileOutputStream(f);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (FileNotFoundException e) {
                e = e;
            } catch (IOException e2) {
                e = e2;
            }
            try {
                fileOutputStream.write(String.valueOf(val).getBytes());
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                        fileOutputStream2 = fileOutputStream;
                    } catch (IOException e3) {
                        fileOutputStream2 = fileOutputStream;
                    }
                } else {
                    fileOutputStream2 = fileOutputStream;
                }
            } catch (FileNotFoundException e4) {
                e = e4;
                fileOutputStream2 = fileOutputStream;
                Slog.e(TAG, "failed to open lowbattery file", e);
                if (fileOutputStream2 != null) {
                    try {
                        fileOutputStream2.close();
                    } catch (IOException e5) {
                    }
                }
            } catch (IOException e6) {
                e = e6;
                fileOutputStream2 = fileOutputStream;
                Slog.e(TAG, "failed to write lowbattery file", e);
                if (fileOutputStream2 != null) {
                    try {
                        fileOutputStream2.close();
                    } catch (IOException e7) {
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                fileOutputStream2 = fileOutputStream;
                if (fileOutputStream2 != null) {
                    try {
                        fileOutputStream2.close();
                    } catch (IOException e8) {
                    }
                }
                throw th;
            }
        }
    }

    private void update(BatteryProperties props) {
        synchronized (this.mLock) {
            if (!this.mUpdatesStopped) {
                this.mBatteryProps = props;
                processValuesLocked(DEBUG);
            } else {
                this.mLastBatteryProps.set(props);
            }
        }
    }

    private void processValuesLocked(boolean force) throws Throwable {
        boolean logOutlier = DEBUG;
        long dischargeDuration = 0;
        this.mBatteryLevelCritical = this.mBatteryProps.batteryLevel <= this.mCriticalBatteryLevel ? true : DEBUG;
        if (this.mBatteryProps.chargerAcOnline && this.mBatteryProps.chargerUsbOnline) {
            this.mPlugType_all = 3;
            this.mPlugType = 1;
        } else if (this.mBatteryProps.chargerAcOnline) {
            this.mPlugType_all = 1;
            this.mPlugType = 1;
        } else if (this.mBatteryProps.chargerUsbOnline) {
            this.mPlugType_all = 2;
            this.mPlugType = 2;
        } else if (this.mBatteryProps.chargerWirelessOnline) {
            this.mPlugType_all = 4;
            this.mPlugType = 4;
        } else {
            this.mPlugType_all = 0;
            this.mPlugType = 0;
        }
        try {
            this.mBatteryStats.setBatteryState(this.mBatteryProps.batteryStatus, this.mBatteryProps.batteryHealth, this.mPlugType, this.mBatteryProps.batteryLevel, this.mBatteryProps.batteryTemperature, this.mBatteryProps.batteryVoltage);
        } catch (RemoteException e) {
        }
        shutdownIfNoPowerLocked();
        shutdownIfOverTempLocked();
        if (force || this.mBatteryProps.batteryStatus != this.mLastBatteryStatus || this.mBatteryProps.batteryHealth != this.mLastBatteryHealth || this.mBatteryProps.batteryPresent != this.mLastBatteryPresent || this.mBatteryProps.batteryLevel != this.mLastBatteryLevel || this.mPlugType != this.mLastPlugType || this.mBatteryProps.batteryVoltage != this.mLastBatteryVoltage || this.mBatteryProps.batteryTemperature != this.mLastBatteryTemperature || this.mInvalidCharger != this.mLastInvalidCharger || this.mPlugType_all != this.mLastPlugType_all) {
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
                this.mBatteryLevelLow = DEBUG;
            } else if (force && this.mBatteryProps.batteryLevel >= this.mLowBatteryWarningLevel) {
                this.mBatteryLevelLow = DEBUG;
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
                setLowbattery(0);
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
                setLowbattery(1);
            } else if (this.mSentLowBatteryBroadcast && this.mLastBatteryLevel >= this.mLowBatteryCloseWarningLevel) {
                this.mSentLowBatteryBroadcast = DEBUG;
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent statusIntent = new Intent("android.intent.action.BATTERY_OKAY");
                        statusIntent.setFlags(67108864);
                        BatteryService.this.mContext.sendBroadcastAsUser(statusIntent, UserHandle.ALL);
                    }
                });
            }
            this.mLed.updateLightsLocked();
            if (logOutlier && dischargeDuration != 0) {
                logOutlierLocked(dischargeDuration);
            }
            this.mLastBatteryStatus = this.mBatteryProps.batteryStatus;
            this.mLastBatteryHealth = this.mBatteryProps.batteryHealth;
            this.mLastBatteryPresent = this.mBatteryProps.batteryPresent;
            this.mLastBatteryLevel = this.mBatteryProps.batteryLevel;
            this.mLastPlugType = this.mPlugType;
            this.mLastBatteryVoltage = this.mBatteryProps.batteryVoltage;
            this.mLastBatteryTemperature = this.mBatteryProps.batteryTemperature;
            this.mLastBatteryLevelCritical = this.mBatteryLevelCritical;
            this.mLastInvalidCharger = this.mInvalidCharger;
        }
    }

    private void sendIntentLocked() {
        final Intent intent = new Intent("android.intent.action.BATTERY_CHANGED");
        intent.addFlags(1610612736);
        int icon = getIconLocked(this.mBatteryProps.batteryLevel);
        intent.putExtra("status", this.mBatteryProps.batteryStatus);
        intent.putExtra("health", this.mBatteryProps.batteryHealth);
        intent.putExtra("present", this.mBatteryProps.batteryPresent);
        intent.putExtra("level", this.mBatteryProps.batteryLevel);
        intent.putExtra("scale", 100);
        intent.putExtra("icon-small", icon);
        intent.putExtra("plugged", this.mPlugType);
        intent.putExtra("voltage", this.mBatteryProps.batteryVoltage);
        intent.putExtra("temperature", this.mBatteryProps.batteryTemperature);
        intent.putExtra("technology", this.mBatteryProps.batteryTechnology);
        intent.putExtra("invalid_charger", this.mInvalidCharger);
        this.mHandler.removeCallbacks(this.mSendBatteryChanged);
        this.mSendBatteryChanged = new Runnable() {
            @Override
            public void run() {
                ActivityManagerNative.broadcastStickyIntent(intent, (String) null, -1);
            }
        };
        this.mHandler.postDelayed(this.mSendBatteryChanged, 200L);
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
                        if (dumpFile2 == null || dumpFile2.delete()) {
                            dumpStream = dumpStream2;
                            dumpFile = dumpFile2;
                        } else {
                            Slog.e(TAG, "failed to delete temporary dumpsys file: " + dumpFile2.getAbsolutePath());
                            dumpStream = dumpStream2;
                            dumpFile = dumpFile2;
                        }
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
        if (dischargeThresholdString != null && durationThresholdString != null) {
            try {
                long durationThreshold = Long.parseLong(durationThresholdString);
                int dischargeThreshold = Integer.parseInt(dischargeThresholdString);
                if (duration <= durationThreshold && this.mDischargeStartLevel - this.mBatteryProps.batteryLevel >= dischargeThreshold) {
                    logBatteryStatsLocked();
                }
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Invalid DischargeThresholds GService string: " + durationThresholdString + " or " + dischargeThresholdString);
            }
        }
    }

    private int getIconLocked(int level) {
        if (this.mBatteryProps.batteryStatus == 2) {
            return R.drawable.jog_dial_bg;
        }
        if (this.mBatteryProps.batteryStatus == 3) {
            return R.drawable.item_background_borderless_material_light;
        }
        if (this.mBatteryProps.batteryStatus == 4 || this.mBatteryProps.batteryStatus == 5) {
            return (!isPoweredLocked(7) || this.mBatteryProps.batteryLevel < 100) ? R.drawable.item_background_borderless_material_light : R.drawable.jog_dial_bg;
        }
        return R.drawable.jog_tab_bar_right_end_confirm_green;
    }

    private void dumpInternal(PrintWriter pw, String[] args) {
        long ident;
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
                    pw.println("  status: " + this.mBatteryProps.batteryStatus);
                    pw.println("  health: " + this.mBatteryProps.batteryHealth);
                    pw.println("  present: " + this.mBatteryProps.batteryPresent);
                    pw.println("  level: " + this.mBatteryProps.batteryLevel);
                    pw.println("  scale: 100");
                    pw.println("  voltage: " + this.mBatteryProps.batteryVoltage);
                    pw.println("  temperature: " + this.mBatteryProps.batteryTemperature);
                    pw.println("  technology: " + this.mBatteryProps.batteryTechnology);
                } else if (args.length == 3 && "set".equals(args[0])) {
                    String key = args[1];
                    String value = args[2];
                    try {
                        if (!this.mUpdatesStopped) {
                            this.mLastBatteryProps.set(this.mBatteryProps);
                        }
                        boolean update = true;
                        if ("ac".equals(key)) {
                            this.mBatteryProps.chargerAcOnline = Integer.parseInt(value) != 0;
                        } else if ("usb".equals(key)) {
                            this.mBatteryProps.chargerUsbOnline = Integer.parseInt(value) != 0;
                        } else if ("wireless".equals(key)) {
                            this.mBatteryProps.chargerWirelessOnline = Integer.parseInt(value) != 0;
                        } else if ("status".equals(key)) {
                            this.mBatteryProps.batteryStatus = Integer.parseInt(value);
                        } else if ("level".equals(key)) {
                            this.mBatteryProps.batteryLevel = Integer.parseInt(value);
                        } else if ("invalid".equals(key)) {
                            this.mInvalidCharger = Integer.parseInt(value);
                        } else {
                            pw.println("Unknown set option: " + key);
                            update = DEBUG;
                        }
                        if (update) {
                            ident = Binder.clearCallingIdentity();
                            try {
                                this.mUpdatesStopped = true;
                                processValuesLocked(DEBUG);
                            } finally {
                            }
                        }
                    } catch (NumberFormatException e) {
                        pw.println("Bad value: " + value);
                    }
                } else if (args.length == 1 && "reset".equals(args[0])) {
                    ident = Binder.clearCallingIdentity();
                    try {
                        if (this.mUpdatesStopped) {
                            this.mUpdatesStopped = DEBUG;
                            this.mBatteryProps.set(this.mLastBatteryProps);
                            processValuesLocked(DEBUG);
                        }
                        Binder.restoreCallingIdentity(ident);
                    } finally {
                    }
                } else {
                    pw.println("Dump current battery state, or:");
                    pw.println("  set [ac|usb|wireless|status|level|invalid] <value>");
                    pw.println("  reset");
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
            this.mBatteryLowARGB = context.getResources().getInteger(R.integer.config_customizedMaxCachedProcesses);
            this.mBatteryMediumARGB = context.getResources().getInteger(R.integer.config_datagram_wait_for_connected_state_for_last_message_timeout_millis);
            this.mBatteryFullARGB = context.getResources().getInteger(R.integer.config_datagram_wait_for_connected_state_timeout_millis);
            this.mBatteryLedOn = context.getResources().getInteger(R.integer.config_datause_notification_type);
            this.mBatteryLedOff = context.getResources().getInteger(R.integer.config_datause_polling_period_sec);
        }

        public void updateLightsLocked() {
            int level = BatteryService.this.mBatteryProps.batteryLevel;
            int status = BatteryService.this.mBatteryProps.batteryStatus;
            if (level < BatteryService.this.mLowBatteryWarningLevel) {
                if (status == 2) {
                    this.mBatteryLight.setColor(this.mBatteryLowARGB);
                    return;
                } else {
                    this.mBatteryLight.setFlashing(this.mBatteryLowARGB, 1, this.mBatteryLedOn, this.mBatteryLedOff);
                    return;
                }
            }
            if (status == 2 || status == 5) {
                if (status == 5 || level >= 90) {
                    this.mBatteryLight.setColor(this.mBatteryFullARGB);
                    return;
                } else {
                    this.mBatteryLight.setColor(this.mBatteryMediumARGB);
                    return;
                }
            }
            this.mBatteryLight.turnOff();
        }
    }

    private final class BatteryListener extends IBatteryPropertiesListener.Stub {
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
        private BinderService() {
        }

        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (BatteryService.this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") == 0) {
                BatteryService.this.dumpInternal(pw, args);
            } else {
                pw.println("Permission Denial: can't dump Battery service from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            }
        }
    }

    private final class LocalService extends BatteryManagerInternal {
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

        public int getPlugType_all() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mPlugType_all;
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
