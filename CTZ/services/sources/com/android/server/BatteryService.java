package com.android.server;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.health.V1_0.HealthInfo;
import android.hardware.health.V2_0.IHealth;
import android.hardware.health.V2_0.IHealthInfoCallback;
import android.hardware.health.V2_0.Result;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.metrics.LogMaker;
import android.os.BatteryManagerInternal;
import android.os.BatteryProperty;
import android.os.Binder;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBatteryPropertiesListener;
import android.os.IBatteryPropertiesRegistrar;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.EventLog;
import android.util.MutableInt;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.DumpUtils;
import com.android.server.BatteryService;
import com.android.server.am.BatteryStatsService;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.pm.DumpState;
import com.android.server.storage.DeviceStorageMonitorService;
import com.android.server.utils.PriorityDump;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class BatteryService extends SystemService {
    private static final long BATTERY_LEVEL_CHANGE_THROTTLE_MS = 60000;
    private static final int BATTERY_PLUGGED_NONE = 0;
    private static final int BATTERY_SCALE = 100;
    private static final boolean DEBUG = false;
    private static final String DUMPSYS_DATA_PATH = "/data/system/";
    private static final long HEALTH_HAL_WAIT_MS = 1000;
    private static final int MAX_BATTERY_LEVELS_QUEUE_SIZE = 100;
    static final int OPTION_FORCE_UPDATE = 1;
    private ActivityManagerInternal mActivityManagerInternal;
    private boolean mBatteryLevelCritical;
    private boolean mBatteryLevelLow;
    private ArrayDeque<Bundle> mBatteryLevelsEventQueue;
    private BatteryPropertiesRegistrar mBatteryPropertiesRegistrar;
    private final IBatteryStats mBatteryStats;
    BinderService mBinderService;
    private int mChargeStartLevel;
    private long mChargeStartTime;
    private final Context mContext;
    private int mCriticalBatteryLevel;
    private int mDischargeStartLevel;
    private long mDischargeStartTime;
    private final Handler mHandler;
    private HealthHalCallback mHealthHalCallback;
    private HealthInfo mHealthInfo;
    private HealthServiceWrapper mHealthServiceWrapper;
    private int mInvalidCharger;
    private int mLastBatteryHealth;
    private int mLastBatteryLevel;
    private long mLastBatteryLevelChangedSentMs;
    private boolean mLastBatteryLevelCritical;
    private boolean mLastBatteryPresent;
    private int mLastBatteryStatus;
    private int mLastBatteryTemperature;
    private int mLastBatteryVoltage;
    private int mLastChargeCounter;
    private final HealthInfo mLastHealthInfo;
    private int mLastInvalidCharger;
    private int mLastMaxChargingCurrent;
    private int mLastMaxChargingVoltage;
    private int mLastPlugType;
    private Led mLed;
    private final Object mLock;
    private int mLowBatteryCloseWarningLevel;
    private int mLowBatteryWarningLevel;
    private MetricsLogger mMetricsLogger;
    private int mPlugType;
    private boolean mSentLowBatteryBroadcast;
    private int mSequence;
    private int mShutdownBatteryTemperature;
    private boolean mUpdatesStopped;
    private static final String TAG = BatteryService.class.getSimpleName();
    private static final String[] DUMPSYS_ARGS = {"--checkin", "--unplugged"};

    public BatteryService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mLastHealthInfo = new HealthInfo();
        this.mSequence = 1;
        this.mLastPlugType = -1;
        this.mSentLowBatteryBroadcast = false;
        this.mContext = context;
        this.mHandler = new Handler(true);
        this.mLed = new Led(context, (LightsManager) getLocalService(LightsManager.class));
        this.mBatteryStats = BatteryStatsService.getService();
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mCriticalBatteryLevel = this.mContext.getResources().getInteger(R.integer.config_autoBrightnessLightSensorRate);
        this.mLowBatteryWarningLevel = this.mContext.getResources().getInteger(R.integer.config_defaultBinderHeavyHitterWatcherBatchSize);
        this.mLowBatteryCloseWarningLevel = this.mLowBatteryWarningLevel + this.mContext.getResources().getInteger(R.integer.config_defaultBinderHeavyHitterAutoSamplerBatchSize);
        this.mShutdownBatteryTemperature = this.mContext.getResources().getInteger(R.integer.config_dreamsBatteryLevelDrainCutoff);
        this.mBatteryLevelsEventQueue = new ArrayDeque<>();
        this.mMetricsLogger = new MetricsLogger();
        if (new File("/sys/devices/virtual/switch/invalid_charger/state").exists()) {
            new UEventObserver() {
                public void onUEvent(UEventObserver.UEvent uEvent) {
                    boolean zEquals = "1".equals(uEvent.get("SWITCH_STATE"));
                    synchronized (BatteryService.this.mLock) {
                        if (BatteryService.this.mInvalidCharger != zEquals) {
                            BatteryService.this.mInvalidCharger = zEquals ? 1 : 0;
                        }
                    }
                }
            }.startObserving("DEVPATH=/devices/virtual/switch/invalid_charger");
        }
    }

    @Override
    public void onStart() {
        registerHealthCallback();
        this.mBinderService = new BinderService();
        publishBinderService("battery", this.mBinderService);
        this.mBatteryPropertiesRegistrar = new BatteryPropertiesRegistrar();
        publishBinderService("batteryproperties", this.mBatteryPropertiesRegistrar);
        publishLocalService(BatteryManagerInternal.class, new LocalService());
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 550) {
            synchronized (this.mLock) {
                this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("low_power_trigger_level"), false, new ContentObserver(this.mHandler) {
                    @Override
                    public void onChange(boolean z) {
                        synchronized (BatteryService.this.mLock) {
                            BatteryService.this.updateBatteryWarningLevelLocked();
                        }
                    }
                }, -1);
                updateBatteryWarningLevelLocked();
            }
        }
    }

    private void registerHealthCallback() {
        traceBegin("HealthInitWrapper");
        this.mHealthServiceWrapper = new HealthServiceWrapper();
        this.mHealthHalCallback = new HealthHalCallback();
        try {
            try {
                this.mHealthServiceWrapper.init(this.mHealthHalCallback, new HealthServiceWrapper.IServiceManagerSupplier() {
                }, new HealthServiceWrapper.IHealthSupplier() {
                });
                traceEnd();
                traceBegin("HealthInitWaitUpdate");
                long jUptimeMillis = SystemClock.uptimeMillis();
                synchronized (this.mLock) {
                    while (this.mHealthInfo == null) {
                        Slog.i(TAG, "health: Waited " + (SystemClock.uptimeMillis() - jUptimeMillis) + "ms for callbacks. Waiting another 1000 ms...");
                        try {
                            this.mLock.wait(1000L);
                        } catch (InterruptedException e) {
                            Slog.i(TAG, "health: InterruptedException when waiting for update.  Continuing...");
                        }
                    }
                }
                Slog.i(TAG, "health: Waited " + (SystemClock.uptimeMillis() - jUptimeMillis) + "ms and received the update.");
            } finally {
                traceEnd();
            }
        } catch (RemoteException e2) {
            Slog.e(TAG, "health: cannot register callback. (RemoteException)");
            throw e2.rethrowFromSystemServer();
        } catch (NoSuchElementException e3) {
            Slog.e(TAG, "health: cannot register callback. (no supported health HAL service)");
            throw e3;
        }
    }

    private void updateBatteryWarningLevelLocked() throws Throwable {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        int integer = this.mContext.getResources().getInteger(R.integer.config_defaultBinderHeavyHitterWatcherBatchSize);
        this.mLowBatteryWarningLevel = Settings.Global.getInt(contentResolver, "low_power_trigger_level", integer);
        if (this.mLowBatteryWarningLevel == 0) {
            this.mLowBatteryWarningLevel = integer;
        }
        if (this.mLowBatteryWarningLevel < this.mCriticalBatteryLevel) {
            this.mLowBatteryWarningLevel = this.mCriticalBatteryLevel;
        }
        this.mLowBatteryCloseWarningLevel = this.mLowBatteryWarningLevel + this.mContext.getResources().getInteger(R.integer.config_defaultBinderHeavyHitterAutoSamplerBatchSize);
        processValuesLocked(true);
    }

    private boolean isPoweredLocked(int i) {
        if (this.mHealthInfo.batteryStatus == 1) {
            return true;
        }
        if ((i & 1) != 0 && this.mHealthInfo.chargerAcOnline) {
            return true;
        }
        if ((i & 2) == 0 || !this.mHealthInfo.chargerUsbOnline) {
            return (i & 4) != 0 && this.mHealthInfo.chargerWirelessOnline;
        }
        return true;
    }

    private boolean shouldSendBatteryLowLocked() {
        boolean z = this.mPlugType != 0;
        boolean z2 = this.mLastPlugType != 0;
        if (z || this.mHealthInfo.batteryStatus == 1 || this.mHealthInfo.batteryLevel > this.mLowBatteryWarningLevel) {
            return false;
        }
        return z2 || this.mLastBatteryLevel > this.mLowBatteryWarningLevel;
    }

    private void shutdownIfNoPowerLocked() {
        if (this.mHealthInfo.batteryLevel == 0 && !isPoweredLocked(7)) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (BatteryService.this.mActivityManagerInternal.isSystemReady()) {
                        Intent intent = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
                        intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                        intent.putExtra("android.intent.extra.REASON", "battery");
                        intent.setFlags(268435456);
                        BatteryService.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    }
                }
            });
        }
    }

    private void shutdownIfOverTempLocked() {
        if (this.mHealthInfo.batteryTemperature > this.mShutdownBatteryTemperature) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (BatteryService.this.mActivityManagerInternal.isSystemReady()) {
                        Intent intent = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
                        intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                        intent.putExtra("android.intent.extra.REASON", "thermal,battery");
                        intent.setFlags(268435456);
                        BatteryService.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    }
                }
            });
        }
    }

    private void update(android.hardware.health.V2_0.HealthInfo healthInfo) {
        traceBegin("HealthInfoUpdate");
        synchronized (this.mLock) {
            if (!this.mUpdatesStopped) {
                this.mHealthInfo = healthInfo.legacy;
                processValuesLocked(false);
                this.mLock.notifyAll();
            } else {
                copy(this.mLastHealthInfo, healthInfo.legacy);
            }
        }
        traceEnd();
    }

    private static void copy(HealthInfo healthInfo, HealthInfo healthInfo2) {
        healthInfo.chargerAcOnline = healthInfo2.chargerAcOnline;
        healthInfo.chargerUsbOnline = healthInfo2.chargerUsbOnline;
        healthInfo.chargerWirelessOnline = healthInfo2.chargerWirelessOnline;
        healthInfo.maxChargingCurrent = healthInfo2.maxChargingCurrent;
        healthInfo.maxChargingVoltage = healthInfo2.maxChargingVoltage;
        healthInfo.batteryStatus = healthInfo2.batteryStatus;
        healthInfo.batteryHealth = healthInfo2.batteryHealth;
        healthInfo.batteryPresent = healthInfo2.batteryPresent;
        healthInfo.batteryLevel = healthInfo2.batteryLevel;
        healthInfo.batteryVoltage = healthInfo2.batteryVoltage;
        healthInfo.batteryTemperature = healthInfo2.batteryTemperature;
        healthInfo.batteryCurrent = healthInfo2.batteryCurrent;
        healthInfo.batteryCycleCount = healthInfo2.batteryCycleCount;
        healthInfo.batteryFullCharge = healthInfo2.batteryFullCharge;
        healthInfo.batteryChargeCounter = healthInfo2.batteryChargeCounter;
        healthInfo.batteryTechnology = healthInfo2.batteryTechnology;
    }

    private void processValuesLocked(boolean z) throws Throwable {
        boolean z2;
        long jElapsedRealtime;
        this.mBatteryLevelCritical = this.mHealthInfo.batteryStatus != 1 && this.mHealthInfo.batteryLevel <= this.mCriticalBatteryLevel;
        if (this.mHealthInfo.chargerAcOnline) {
            this.mPlugType = 1;
        } else if (this.mHealthInfo.chargerUsbOnline) {
            this.mPlugType = 2;
        } else if (this.mHealthInfo.chargerWirelessOnline) {
            this.mPlugType = 4;
        } else {
            this.mPlugType = 0;
        }
        try {
            this.mBatteryStats.setBatteryState(this.mHealthInfo.batteryStatus, this.mHealthInfo.batteryHealth, this.mPlugType, this.mHealthInfo.batteryLevel, this.mHealthInfo.batteryTemperature, this.mHealthInfo.batteryVoltage, this.mHealthInfo.batteryChargeCounter, this.mHealthInfo.batteryFullCharge);
        } catch (RemoteException e) {
        }
        shutdownIfNoPowerLocked();
        shutdownIfOverTempLocked();
        if (z || this.mHealthInfo.batteryStatus != this.mLastBatteryStatus || this.mHealthInfo.batteryHealth != this.mLastBatteryHealth || this.mHealthInfo.batteryPresent != this.mLastBatteryPresent || this.mHealthInfo.batteryLevel != this.mLastBatteryLevel || this.mPlugType != this.mLastPlugType || this.mHealthInfo.batteryVoltage != this.mLastBatteryVoltage || this.mHealthInfo.batteryTemperature != this.mLastBatteryTemperature || this.mHealthInfo.maxChargingCurrent != this.mLastMaxChargingCurrent || this.mHealthInfo.maxChargingVoltage != this.mLastMaxChargingVoltage || this.mHealthInfo.batteryChargeCounter != this.mLastChargeCounter || this.mInvalidCharger != this.mLastInvalidCharger) {
            if (this.mPlugType != this.mLastPlugType) {
                if (this.mLastPlugType == 0) {
                    this.mChargeStartLevel = this.mHealthInfo.batteryLevel;
                    this.mChargeStartTime = SystemClock.elapsedRealtime();
                    LogMaker logMaker = new LogMaker(1417);
                    logMaker.setType(4);
                    logMaker.addTaggedData(1421, Integer.valueOf(this.mPlugType));
                    logMaker.addTaggedData(1418, Integer.valueOf(this.mHealthInfo.batteryLevel));
                    this.mMetricsLogger.write(logMaker);
                    if (this.mDischargeStartTime == 0 || this.mDischargeStartLevel == this.mHealthInfo.batteryLevel) {
                        z2 = false;
                        jElapsedRealtime = 0;
                    } else {
                        jElapsedRealtime = SystemClock.elapsedRealtime() - this.mDischargeStartTime;
                        EventLog.writeEvent(EventLogTags.BATTERY_DISCHARGE, Long.valueOf(jElapsedRealtime), Integer.valueOf(this.mDischargeStartLevel), Integer.valueOf(this.mHealthInfo.batteryLevel));
                        this.mDischargeStartTime = 0L;
                        z2 = true;
                    }
                } else {
                    if (this.mPlugType == 0) {
                        this.mDischargeStartTime = SystemClock.elapsedRealtime();
                        this.mDischargeStartLevel = this.mHealthInfo.batteryLevel;
                        long jElapsedRealtime2 = SystemClock.elapsedRealtime() - this.mChargeStartTime;
                        if (this.mChargeStartTime != 0 && jElapsedRealtime2 != 0) {
                            LogMaker logMaker2 = new LogMaker(1417);
                            logMaker2.setType(5);
                            logMaker2.addTaggedData(1421, Integer.valueOf(this.mLastPlugType));
                            logMaker2.addTaggedData(1420, Long.valueOf(jElapsedRealtime2));
                            logMaker2.addTaggedData(1418, Integer.valueOf(this.mChargeStartLevel));
                            logMaker2.addTaggedData(1419, Integer.valueOf(this.mHealthInfo.batteryLevel));
                            this.mMetricsLogger.write(logMaker2);
                        }
                        this.mChargeStartTime = 0L;
                    }
                    z2 = false;
                    jElapsedRealtime = 0;
                }
            } else {
                z2 = false;
                jElapsedRealtime = 0;
            }
            if (this.mHealthInfo.batteryStatus != this.mLastBatteryStatus || this.mHealthInfo.batteryHealth != this.mLastBatteryHealth || this.mHealthInfo.batteryPresent != this.mLastBatteryPresent || this.mPlugType != this.mLastPlugType) {
                EventLog.writeEvent(EventLogTags.BATTERY_STATUS, Integer.valueOf(this.mHealthInfo.batteryStatus), Integer.valueOf(this.mHealthInfo.batteryHealth), Integer.valueOf(this.mHealthInfo.batteryPresent ? 1 : 0), Integer.valueOf(this.mPlugType), this.mHealthInfo.batteryTechnology);
            }
            if (this.mHealthInfo.batteryLevel != this.mLastBatteryLevel) {
                EventLog.writeEvent(EventLogTags.BATTERY_LEVEL, Integer.valueOf(this.mHealthInfo.batteryLevel), Integer.valueOf(this.mHealthInfo.batteryVoltage), Integer.valueOf(this.mHealthInfo.batteryTemperature));
            }
            boolean z3 = z2;
            if (this.mBatteryLevelCritical) {
                z3 = z2;
                if (!this.mLastBatteryLevelCritical) {
                    z3 = z2;
                    if (this.mPlugType == 0) {
                        jElapsedRealtime = SystemClock.elapsedRealtime() - this.mDischargeStartTime;
                        z3 = true;
                    }
                }
            }
            if (!this.mBatteryLevelLow) {
                if (this.mPlugType == 0 && this.mHealthInfo.batteryStatus != 1 && this.mHealthInfo.batteryLevel <= this.mLowBatteryWarningLevel) {
                    this.mBatteryLevelLow = true;
                }
            } else if (this.mPlugType != 0 || this.mHealthInfo.batteryLevel >= this.mLowBatteryCloseWarningLevel) {
                this.mBatteryLevelLow = false;
            } else if (z && this.mHealthInfo.batteryLevel >= this.mLowBatteryWarningLevel) {
                this.mBatteryLevelLow = false;
            }
            this.mSequence++;
            if (this.mPlugType != 0 && this.mLastPlugType == 0) {
                final Intent intent = new Intent("android.intent.action.ACTION_POWER_CONNECTED");
                intent.setFlags(67108864);
                intent.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        BatteryService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                    }
                });
            } else if (this.mPlugType == 0 && this.mLastPlugType != 0) {
                final Intent intent2 = new Intent("android.intent.action.ACTION_POWER_DISCONNECTED");
                intent2.setFlags(67108864);
                intent2.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        BatteryService.this.mContext.sendBroadcastAsUser(intent2, UserHandle.ALL);
                    }
                });
            }
            if (shouldSendBatteryLowLocked()) {
                this.mSentLowBatteryBroadcast = true;
                final Intent intent3 = new Intent("android.intent.action.BATTERY_LOW");
                intent3.setFlags(67108864);
                intent3.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        BatteryService.this.mContext.sendBroadcastAsUser(intent3, UserHandle.ALL);
                    }
                });
            } else if (this.mSentLowBatteryBroadcast && this.mHealthInfo.batteryLevel >= this.mLowBatteryCloseWarningLevel) {
                this.mSentLowBatteryBroadcast = false;
                final Intent intent4 = new Intent("android.intent.action.BATTERY_OKAY");
                intent4.setFlags(67108864);
                intent4.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        BatteryService.this.mContext.sendBroadcastAsUser(intent4, UserHandle.ALL);
                    }
                });
            }
            sendBatteryChangedIntentLocked();
            if (this.mLastBatteryLevel != this.mHealthInfo.batteryLevel) {
                sendBatteryLevelChangedIntentLocked();
            }
            this.mLed.updateLightsLocked();
            if (z3 && jElapsedRealtime != 0) {
                logOutlierLocked(jElapsedRealtime);
            }
            this.mLastBatteryStatus = this.mHealthInfo.batteryStatus;
            this.mLastBatteryHealth = this.mHealthInfo.batteryHealth;
            this.mLastBatteryPresent = this.mHealthInfo.batteryPresent;
            this.mLastBatteryLevel = this.mHealthInfo.batteryLevel;
            this.mLastPlugType = this.mPlugType;
            this.mLastBatteryVoltage = this.mHealthInfo.batteryVoltage;
            this.mLastBatteryTemperature = this.mHealthInfo.batteryTemperature;
            this.mLastMaxChargingCurrent = this.mHealthInfo.maxChargingCurrent;
            this.mLastMaxChargingVoltage = this.mHealthInfo.maxChargingVoltage;
            this.mLastChargeCounter = this.mHealthInfo.batteryChargeCounter;
            this.mLastBatteryLevelCritical = this.mBatteryLevelCritical;
            this.mLastInvalidCharger = this.mInvalidCharger;
        }
    }

    private void sendBatteryChangedIntentLocked() {
        final Intent intent = new Intent("android.intent.action.BATTERY_CHANGED");
        intent.addFlags(1610612736);
        int iconLocked = getIconLocked(this.mHealthInfo.batteryLevel);
        intent.putExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
        intent.putExtra("status", this.mHealthInfo.batteryStatus);
        intent.putExtra("health", this.mHealthInfo.batteryHealth);
        intent.putExtra("present", this.mHealthInfo.batteryPresent);
        intent.putExtra("level", this.mHealthInfo.batteryLevel);
        intent.putExtra("battery_low", this.mSentLowBatteryBroadcast);
        intent.putExtra("scale", 100);
        intent.putExtra("icon-small", iconLocked);
        intent.putExtra("plugged", this.mPlugType);
        intent.putExtra("voltage", this.mHealthInfo.batteryVoltage);
        intent.putExtra("temperature", this.mHealthInfo.batteryTemperature);
        intent.putExtra("technology", this.mHealthInfo.batteryTechnology);
        intent.putExtra("invalid_charger", this.mInvalidCharger);
        intent.putExtra("max_charging_current", this.mHealthInfo.maxChargingCurrent);
        intent.putExtra("max_charging_voltage", this.mHealthInfo.maxChargingVoltage);
        intent.putExtra("charge_counter", this.mHealthInfo.batteryChargeCounter);
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                ActivityManager.broadcastStickyIntent(intent, -1);
            }
        });
    }

    private void sendBatteryLevelChangedIntentLocked() {
        Bundle bundle = new Bundle();
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        bundle.putInt(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mSequence);
        bundle.putInt("status", this.mHealthInfo.batteryStatus);
        bundle.putInt("health", this.mHealthInfo.batteryHealth);
        bundle.putBoolean("present", this.mHealthInfo.batteryPresent);
        bundle.putInt("level", this.mHealthInfo.batteryLevel);
        bundle.putBoolean("battery_low", this.mSentLowBatteryBroadcast);
        bundle.putInt("scale", 100);
        bundle.putInt("plugged", this.mPlugType);
        bundle.putInt("voltage", this.mHealthInfo.batteryVoltage);
        bundle.putLong("android.os.extra.EVENT_TIMESTAMP", jElapsedRealtime);
        boolean zIsEmpty = this.mBatteryLevelsEventQueue.isEmpty();
        this.mBatteryLevelsEventQueue.add(bundle);
        if (this.mBatteryLevelsEventQueue.size() > 100) {
            this.mBatteryLevelsEventQueue.removeFirst();
        }
        if (zIsEmpty) {
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.sendEnqueuedBatteryLevelChangedEvents();
                }
            }, jElapsedRealtime - this.mLastBatteryLevelChangedSentMs > 60000 ? 0L : (this.mLastBatteryLevelChangedSentMs + 60000) - jElapsedRealtime);
        }
    }

    private void sendEnqueuedBatteryLevelChangedEvents() {
        ArrayList<? extends Parcelable> arrayList;
        synchronized (this.mLock) {
            arrayList = new ArrayList<>(this.mBatteryLevelsEventQueue);
            this.mBatteryLevelsEventQueue.clear();
        }
        Intent intent = new Intent("android.intent.action.BATTERY_LEVEL_CHANGED");
        intent.addFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
        intent.putParcelableArrayListExtra("android.os.extra.EVENTS", arrayList);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.BATTERY_STATS");
        this.mLastBatteryLevelChangedSentMs = SystemClock.elapsedRealtime();
    }

    private void logBatteryStatsLocked() throws Throwable {
        DropBoxManager dropBoxManager;
        File file;
        String str;
        ?? sb;
        FileOutputStream fileOutputStream;
        IBinder service = ServiceManager.getService("batterystats");
        if (service == null || (dropBoxManager = (DropBoxManager) this.mContext.getSystemService("dropbox")) == null || !dropBoxManager.isTagEnabled("BATTERY_DISCHARGE_INFO")) {
            return;
        }
        ?? absolutePath = 0;
        absolutePath = 0;
        absolutePath = 0;
        absolutePath = 0;
        absolutePath = 0;
        absolutePath = 0;
        try {
            try {
                file = new File("/data/system/batterystats.dump");
                try {
                    fileOutputStream = new FileOutputStream(file);
                } catch (RemoteException e) {
                    e = e;
                } catch (IOException e2) {
                    e = e2;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                service.dump(fileOutputStream.getFD(), DUMPSYS_ARGS);
                FileUtils.sync(fileOutputStream);
                absolutePath = 2;
                dropBoxManager.addFile("BATTERY_DISCHARGE_INFO", file, 2);
                try {
                    fileOutputStream.close();
                } catch (IOException e3) {
                    Slog.e(TAG, "failed to close dumpsys output stream");
                }
            } catch (RemoteException e4) {
                e = e4;
                absolutePath = fileOutputStream;
                Slog.e(TAG, "failed to dump battery service", e);
                if (absolutePath != 0) {
                    try {
                        absolutePath.close();
                    } catch (IOException e5) {
                        Slog.e(TAG, "failed to close dumpsys output stream");
                    }
                }
                if (file != null && !file.delete()) {
                    str = TAG;
                    sb = new StringBuilder();
                }
            } catch (IOException e6) {
                e = e6;
                absolutePath = fileOutputStream;
                Slog.e(TAG, "failed to write dumpsys file", e);
                if (absolutePath != 0) {
                    try {
                        absolutePath.close();
                    } catch (IOException e7) {
                        Slog.e(TAG, "failed to close dumpsys output stream");
                    }
                }
                if (file != null && !file.delete()) {
                    str = TAG;
                    sb = new StringBuilder();
                }
            } catch (Throwable th2) {
                th = th2;
                absolutePath = fileOutputStream;
                if (absolutePath != 0) {
                    try {
                        absolutePath.close();
                    } catch (IOException e8) {
                        Slog.e(TAG, "failed to close dumpsys output stream");
                    }
                }
                if (file == null || file.delete()) {
                    throw th;
                }
                Slog.e(TAG, "failed to delete temporary dumpsys file: " + file.getAbsolutePath());
                throw th;
            }
        } catch (RemoteException e9) {
            e = e9;
            file = null;
        } catch (IOException e10) {
            e = e10;
            file = null;
        } catch (Throwable th3) {
            th = th3;
            file = null;
        }
        if (!file.delete()) {
            str = TAG;
            sb = new StringBuilder();
            sb.append("failed to delete temporary dumpsys file: ");
            absolutePath = file.getAbsolutePath();
            sb.append(absolutePath);
            Slog.e(str, sb.toString());
        }
    }

    private void logOutlierLocked(long j) throws Throwable {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String string = Settings.Global.getString(contentResolver, "battery_discharge_threshold");
        String string2 = Settings.Global.getString(contentResolver, "battery_discharge_duration_threshold");
        if (string != null && string2 != null) {
            try {
                long j2 = Long.parseLong(string2);
                int i = Integer.parseInt(string);
                if (j <= j2 && this.mDischargeStartLevel - this.mHealthInfo.batteryLevel >= i) {
                    logBatteryStatsLocked();
                }
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Invalid DischargeThresholds GService string: " + string2 + " or " + string);
            }
        }
    }

    private int getIconLocked(int i) {
        if (this.mHealthInfo.batteryStatus == 2) {
            return R.drawable.pointer_horizontal_double_arrow_large;
        }
        if (this.mHealthInfo.batteryStatus == 3) {
            return R.drawable.pointer_hand_vector;
        }
        if (this.mHealthInfo.batteryStatus == 4 || this.mHealthInfo.batteryStatus == 5) {
            return (!isPoweredLocked(7) || this.mHealthInfo.batteryLevel < 100) ? R.drawable.pointer_hand_vector : R.drawable.pointer_horizontal_double_arrow_large;
        }
        return R.drawable.pointer_spot_hover;
    }

    class Shell extends ShellCommand {
        Shell() {
        }

        public int onCommand(String str) {
            return BatteryService.this.onShellCommand(this, str);
        }

        public void onHelp() {
            BatteryService.dumpHelp(getOutPrintWriter());
        }
    }

    static void dumpHelp(PrintWriter printWriter) {
        printWriter.println("Battery service (battery) commands:");
        printWriter.println("  help");
        printWriter.println("    Print this help text.");
        printWriter.println("  set [-f] [ac|usb|wireless|status|level|temp|present|invalid] <value>");
        printWriter.println("    Force a battery property value, freezing battery state.");
        printWriter.println("    -f: force a battery change broadcast be sent, prints new sequence.");
        printWriter.println("  unplug [-f]");
        printWriter.println("    Force battery unplugged, freezing battery state.");
        printWriter.println("    -f: force a battery change broadcast be sent, prints new sequence.");
        printWriter.println("  reset [-f]");
        printWriter.println("    Unfreeze battery state, returning to current hardware values.");
        printWriter.println("    -f: force a battery change broadcast be sent, prints new sequence.");
    }

    int parseOptions(Shell shell) {
        int i = 0;
        while (true) {
            String nextOption = shell.getNextOption();
            if (nextOption != null) {
                if ("-f".equals(nextOption)) {
                    i |= 1;
                }
            } else {
                return i;
            }
        }
    }

    int onShellCommand(Shell shell, String str) {
        byte b;
        long jClearCallingIdentity;
        boolean z;
        if (str == null) {
            return shell.handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = shell.getOutPrintWriter();
        int iHashCode = str.hashCode();
        byte b2 = 2;
        if (iHashCode != -840325209) {
            if (iHashCode != 113762) {
                b = (iHashCode == 108404047 && str.equals("reset")) ? (byte) 2 : (byte) -1;
            } else if (str.equals("set")) {
                b = 1;
            }
        } else if (str.equals("unplug")) {
            b = 0;
        }
        switch (b) {
            case 0:
                int options = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                if (!this.mUpdatesStopped) {
                    copy(this.mLastHealthInfo, this.mHealthInfo);
                }
                this.mHealthInfo.chargerAcOnline = false;
                this.mHealthInfo.chargerUsbOnline = false;
                this.mHealthInfo.chargerWirelessOnline = false;
                jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mUpdatesStopped = true;
                    processValuesFromShellLocked(outPrintWriter, options);
                    return 0;
                } finally {
                }
            case 1:
                int options2 = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                String nextArg = shell.getNextArg();
                if (nextArg == null) {
                    outPrintWriter.println("No property specified");
                    return -1;
                }
                String nextArg2 = shell.getNextArg();
                if (nextArg2 == null) {
                    outPrintWriter.println("No value specified");
                    return -1;
                }
                try {
                    if (!this.mUpdatesStopped) {
                        copy(this.mLastHealthInfo, this.mHealthInfo);
                    }
                    switch (nextArg.hashCode()) {
                        case -1000044642:
                            b2 = !nextArg.equals("wireless") ? (byte) -1 : (byte) 3;
                            break;
                        case -892481550:
                            if (nextArg.equals("status")) {
                                b2 = 4;
                                break;
                            }
                            break;
                        case -318277445:
                            if (nextArg.equals("present")) {
                                b2 = 0;
                                break;
                            }
                            break;
                        case 3106:
                            if (nextArg.equals("ac")) {
                                b2 = 1;
                                break;
                            }
                            break;
                        case 116100:
                            if (nextArg.equals("usb")) {
                                break;
                            }
                            break;
                        case 3556308:
                            if (nextArg.equals("temp")) {
                                b2 = 7;
                                break;
                            }
                            break;
                        case 102865796:
                            if (nextArg.equals("level")) {
                                b2 = 5;
                                break;
                            }
                            break;
                        case 957830652:
                            if (nextArg.equals("counter")) {
                                b2 = 6;
                                break;
                            }
                            break;
                        case 1959784951:
                            if (nextArg.equals("invalid")) {
                                b2 = 8;
                                break;
                            }
                            break;
                        default:
                            break;
                    }
                    switch (b2) {
                        case 0:
                            this.mHealthInfo.batteryPresent = Integer.parseInt(nextArg2) != 0;
                            z = true;
                            break;
                        case 1:
                            this.mHealthInfo.chargerAcOnline = Integer.parseInt(nextArg2) != 0;
                            z = true;
                            break;
                        case 2:
                            this.mHealthInfo.chargerUsbOnline = Integer.parseInt(nextArg2) != 0;
                            z = true;
                            break;
                        case 3:
                            this.mHealthInfo.chargerWirelessOnline = Integer.parseInt(nextArg2) != 0;
                            z = true;
                            break;
                        case 4:
                            this.mHealthInfo.batteryStatus = Integer.parseInt(nextArg2);
                            z = true;
                            break;
                        case 5:
                            this.mHealthInfo.batteryLevel = Integer.parseInt(nextArg2);
                            z = true;
                            break;
                        case 6:
                            this.mHealthInfo.batteryChargeCounter = Integer.parseInt(nextArg2);
                            z = true;
                            break;
                        case 7:
                            this.mHealthInfo.batteryTemperature = Integer.parseInt(nextArg2);
                            z = true;
                            break;
                        case 8:
                            this.mInvalidCharger = Integer.parseInt(nextArg2);
                            z = true;
                            break;
                        default:
                            outPrintWriter.println("Unknown set option: " + nextArg);
                            z = false;
                            break;
                    }
                    if (z) {
                        jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            this.mUpdatesStopped = true;
                            processValuesFromShellLocked(outPrintWriter, options2);
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        } finally {
                        }
                    }
                    return 0;
                } catch (NumberFormatException e) {
                    outPrintWriter.println("Bad value: " + nextArg2);
                    return -1;
                }
            case 2:
                int options3 = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (this.mUpdatesStopped) {
                        this.mUpdatesStopped = false;
                        copy(this.mHealthInfo, this.mLastHealthInfo);
                        processValuesFromShellLocked(outPrintWriter, options3);
                        break;
                    }
                    return 0;
                } finally {
                }
            default:
                return shell.handleDefaultCommands(str);
        }
    }

    private void processValuesFromShellLocked(PrintWriter printWriter, int i) throws Throwable {
        int i2 = i & 1;
        processValuesLocked(i2 != 0);
        if (i2 != 0) {
            printWriter.println(this.mSequence);
        }
    }

    private void dumpInternal(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (this.mLock) {
            if (strArr != null) {
                try {
                    if (strArr.length == 0 || "-a".equals(strArr[0])) {
                        printWriter.println("Current Battery Service state:");
                        if (this.mUpdatesStopped) {
                            printWriter.println("  (UPDATES STOPPED -- use 'reset' to restart)");
                        }
                        printWriter.println("  AC powered: " + this.mHealthInfo.chargerAcOnline);
                        printWriter.println("  USB powered: " + this.mHealthInfo.chargerUsbOnline);
                        printWriter.println("  Wireless powered: " + this.mHealthInfo.chargerWirelessOnline);
                        printWriter.println("  Max charging current: " + this.mHealthInfo.maxChargingCurrent);
                        printWriter.println("  Max charging voltage: " + this.mHealthInfo.maxChargingVoltage);
                        printWriter.println("  Charge counter: " + this.mHealthInfo.batteryChargeCounter);
                        printWriter.println("  status: " + this.mHealthInfo.batteryStatus);
                        printWriter.println("  health: " + this.mHealthInfo.batteryHealth);
                        printWriter.println("  present: " + this.mHealthInfo.batteryPresent);
                        printWriter.println("  level: " + this.mHealthInfo.batteryLevel);
                        printWriter.println("  scale: 100");
                        printWriter.println("  voltage: " + this.mHealthInfo.batteryVoltage);
                        printWriter.println("  temperature: " + this.mHealthInfo.batteryTemperature);
                        printWriter.println("  technology: " + this.mHealthInfo.batteryTechnology);
                    } else {
                        new Shell().exec(this.mBinderService, null, fileDescriptor, null, strArr, null, new ResultReceiver(null));
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    private void dumpProto(FileDescriptor fileDescriptor) {
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
        synchronized (this.mLock) {
            protoOutputStream.write(1133871366145L, this.mUpdatesStopped);
            int i = 0;
            if (this.mHealthInfo.chargerAcOnline) {
                i = 1;
            } else if (this.mHealthInfo.chargerUsbOnline) {
                i = 2;
            } else if (this.mHealthInfo.chargerWirelessOnline) {
                i = 4;
            }
            protoOutputStream.write(1159641169922L, i);
            protoOutputStream.write(1120986464259L, this.mHealthInfo.maxChargingCurrent);
            protoOutputStream.write(1120986464260L, this.mHealthInfo.maxChargingVoltage);
            protoOutputStream.write(1120986464261L, this.mHealthInfo.batteryChargeCounter);
            protoOutputStream.write(1159641169926L, this.mHealthInfo.batteryStatus);
            protoOutputStream.write(1159641169927L, this.mHealthInfo.batteryHealth);
            protoOutputStream.write(1133871366152L, this.mHealthInfo.batteryPresent);
            protoOutputStream.write(1120986464265L, this.mHealthInfo.batteryLevel);
            protoOutputStream.write(1120986464266L, 100);
            protoOutputStream.write(1120986464267L, this.mHealthInfo.batteryVoltage);
            protoOutputStream.write(1120986464268L, this.mHealthInfo.batteryTemperature);
            protoOutputStream.write(1138166333453L, this.mHealthInfo.batteryTechnology);
        }
        protoOutputStream.flush();
    }

    private static void traceBegin(String str) {
        Trace.traceBegin(524288L, str);
    }

    private static void traceEnd() {
        Trace.traceEnd(524288L);
    }

    private final class Led {
        private final int mBatteryFullARGB;
        private final int mBatteryLedOff;
        private final int mBatteryLedOn;
        private final Light mBatteryLight;
        private final int mBatteryLowARGB;
        private final int mBatteryMediumARGB;

        public Led(Context context, LightsManager lightsManager) {
            this.mBatteryLight = lightsManager.getLight(3);
            this.mBatteryLowARGB = context.getResources().getInteger(R.integer.config_displayWhiteBalanceColorTemperatureDefault);
            this.mBatteryMediumARGB = context.getResources().getInteger(R.integer.config_displayWhiteBalanceColorTemperatureFilterHorizon);
            this.mBatteryFullARGB = context.getResources().getInteger(R.integer.config_deviceStateRearDisplay);
            this.mBatteryLedOn = context.getResources().getInteger(R.integer.config_displayWhiteBalanceBrightnessSensorRate);
            this.mBatteryLedOff = context.getResources().getInteger(R.integer.config_displayWhiteBalanceBrightnessFilterHorizon);
        }

        public void updateLightsLocked() {
            int i = BatteryService.this.mHealthInfo.batteryLevel;
            int i2 = BatteryService.this.mHealthInfo.batteryStatus;
            if (i < BatteryService.this.mLowBatteryWarningLevel) {
                if (i2 == 2) {
                    this.mBatteryLight.setColor(this.mBatteryLowARGB);
                    return;
                } else {
                    this.mBatteryLight.setFlashing(this.mBatteryLowARGB, 1, this.mBatteryLedOn, this.mBatteryLedOff);
                    return;
                }
            }
            if (i2 == 2 || i2 == 5) {
                if (i2 == 5 || i >= 90) {
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

    private final class HealthHalCallback extends IHealthInfoCallback.Stub implements HealthServiceWrapper.Callback {
        private HealthHalCallback() {
        }

        @Override
        public void healthInfoChanged(android.hardware.health.V2_0.HealthInfo healthInfo) {
            BatteryService.this.update(healthInfo);
        }

        @Override
        public void onRegistration(IHealth iHealth, IHealth iHealth2, String str) {
            int iRegisterCallback;
            if (iHealth2 == null) {
                return;
            }
            BatteryService.traceBegin("HealthUnregisterCallback");
            if (iHealth != null) {
                try {
                    try {
                        int iUnregisterCallback = iHealth.unregisterCallback(this);
                        if (iUnregisterCallback != 0) {
                            Slog.w(BatteryService.TAG, "health: cannot unregister previous callback: " + Result.toString(iUnregisterCallback));
                        }
                    } catch (RemoteException e) {
                        Slog.w(BatteryService.TAG, "health: cannot unregister previous callback (transaction error): " + e.getMessage());
                    }
                } finally {
                }
            }
            BatteryService.traceEnd();
            BatteryService.traceBegin("HealthRegisterCallback");
            try {
                try {
                    iRegisterCallback = iHealth2.registerCallback(this);
                } catch (RemoteException e2) {
                    Slog.e(BatteryService.TAG, "health: cannot register callback (transaction error): " + e2.getMessage());
                }
                if (iRegisterCallback == 0) {
                    iHealth2.update();
                    return;
                }
                Slog.w(BatteryService.TAG, "health: cannot register callback: " + Result.toString(iRegisterCallback));
            } finally {
            }
        }
    }

    private final class BinderService extends Binder {
        private BinderService() {
        }

        @Override
        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(BatteryService.this.mContext, BatteryService.TAG, printWriter)) {
                if (strArr.length <= 0 || !PriorityDump.PROTO_ARG.equals(strArr[0])) {
                    BatteryService.this.dumpInternal(fileDescriptor, printWriter, strArr);
                } else {
                    BatteryService.this.dumpProto(fileDescriptor);
                }
            }
        }

        public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
            BatteryService.this.new Shell().exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
        }
    }

    private final class BatteryPropertiesRegistrar extends IBatteryPropertiesRegistrar.Stub {
        private BatteryPropertiesRegistrar() {
        }

        public void registerListener(IBatteryPropertiesListener iBatteryPropertiesListener) {
            Slog.e(BatteryService.TAG, "health: must not call registerListener on battery properties");
        }

        public void unregisterListener(IBatteryPropertiesListener iBatteryPropertiesListener) {
            Slog.e(BatteryService.TAG, "health: must not call unregisterListener on battery properties");
        }

        public int getProperty(int i, final BatteryProperty batteryProperty) throws RemoteException {
            BatteryService.traceBegin("HealthGetProperty");
            try {
                IHealth lastService = BatteryService.this.mHealthServiceWrapper.getLastService();
                if (lastService == null) {
                    throw new RemoteException("no health service");
                }
                final MutableInt mutableInt = new MutableInt(1);
                switch (i) {
                    case 1:
                        lastService.getChargeCounter(new IHealth.getChargeCounterCallback() {
                            @Override
                            public final void onValues(int i2, int i3) {
                                BatteryService.BatteryPropertiesRegistrar.lambda$getProperty$0(mutableInt, batteryProperty, i2, i3);
                            }
                        });
                        break;
                    case 2:
                        lastService.getCurrentNow(new IHealth.getCurrentNowCallback() {
                            @Override
                            public final void onValues(int i2, int i3) {
                                BatteryService.BatteryPropertiesRegistrar.lambda$getProperty$1(mutableInt, batteryProperty, i2, i3);
                            }
                        });
                        break;
                    case 3:
                        lastService.getCurrentAverage(new IHealth.getCurrentAverageCallback() {
                            @Override
                            public final void onValues(int i2, int i3) {
                                BatteryService.BatteryPropertiesRegistrar.lambda$getProperty$2(mutableInt, batteryProperty, i2, i3);
                            }
                        });
                        break;
                    case 4:
                        lastService.getCapacity(new IHealth.getCapacityCallback() {
                            @Override
                            public final void onValues(int i2, int i3) {
                                BatteryService.BatteryPropertiesRegistrar.lambda$getProperty$3(mutableInt, batteryProperty, i2, i3);
                            }
                        });
                        break;
                    case 5:
                        lastService.getEnergyCounter(new IHealth.getEnergyCounterCallback() {
                            @Override
                            public final void onValues(int i2, long j) {
                                BatteryService.BatteryPropertiesRegistrar.lambda$getProperty$5(mutableInt, batteryProperty, i2, j);
                            }
                        });
                        break;
                    case 6:
                        lastService.getChargeStatus(new IHealth.getChargeStatusCallback() {
                            @Override
                            public final void onValues(int i2, int i3) {
                                BatteryService.BatteryPropertiesRegistrar.lambda$getProperty$4(mutableInt, batteryProperty, i2, i3);
                            }
                        });
                        break;
                }
                return mutableInt.value;
            } finally {
                BatteryService.traceEnd();
            }
        }

        static void lambda$getProperty$0(MutableInt mutableInt, BatteryProperty batteryProperty, int i, int i2) {
            mutableInt.value = i;
            if (i == 0) {
                batteryProperty.setLong(i2);
            }
        }

        static void lambda$getProperty$1(MutableInt mutableInt, BatteryProperty batteryProperty, int i, int i2) {
            mutableInt.value = i;
            if (i == 0) {
                batteryProperty.setLong(i2);
            }
        }

        static void lambda$getProperty$2(MutableInt mutableInt, BatteryProperty batteryProperty, int i, int i2) {
            mutableInt.value = i;
            if (i == 0) {
                batteryProperty.setLong(i2);
            }
        }

        static void lambda$getProperty$3(MutableInt mutableInt, BatteryProperty batteryProperty, int i, int i2) {
            mutableInt.value = i;
            if (i == 0) {
                batteryProperty.setLong(i2);
            }
        }

        static void lambda$getProperty$4(MutableInt mutableInt, BatteryProperty batteryProperty, int i, int i2) {
            mutableInt.value = i;
            if (i == 0) {
                batteryProperty.setLong(i2);
            }
        }

        static void lambda$getProperty$5(MutableInt mutableInt, BatteryProperty batteryProperty, int i, long j) {
            mutableInt.value = i;
            if (i == 0) {
                batteryProperty.setLong(j);
            }
        }

        public void scheduleUpdate() throws RemoteException {
            BatteryService.traceBegin("HealthScheduleUpdate");
            try {
                IHealth lastService = BatteryService.this.mHealthServiceWrapper.getLastService();
                if (lastService == null) {
                    throw new RemoteException("no health service");
                }
                lastService.update();
            } finally {
                BatteryService.traceEnd();
            }
        }
    }

    private final class LocalService extends BatteryManagerInternal {
        private LocalService() {
        }

        public boolean isPowered(int i) {
            boolean zIsPoweredLocked;
            synchronized (BatteryService.this.mLock) {
                zIsPoweredLocked = BatteryService.this.isPoweredLocked(i);
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
                i = BatteryService.this.mHealthInfo.batteryLevel;
            }
            return i;
        }

        public int getBatteryChargeCounter() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mHealthInfo.batteryChargeCounter;
            }
            return i;
        }

        public int getBatteryFullCharge() {
            int i;
            synchronized (BatteryService.this.mLock) {
                i = BatteryService.this.mHealthInfo.batteryFullCharge;
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

    @VisibleForTesting
    static final class HealthServiceWrapper {
        private static final String TAG = "HealthServiceWrapper";
        private Callback mCallback;
        private IHealthSupplier mHealthSupplier;
        private String mInstanceName;
        public static final String INSTANCE_VENDOR = "default";
        public static final String INSTANCE_HEALTHD = "backup";
        private static final List<String> sAllInstances = Arrays.asList(INSTANCE_VENDOR, INSTANCE_HEALTHD);
        private final IServiceNotification mNotification = new Notification();
        private final HandlerThread mHandlerThread = new HandlerThread("HealthServiceRefresh");
        private final AtomicReference<IHealth> mLastService = new AtomicReference<>();

        interface Callback {
            void onRegistration(IHealth iHealth, IHealth iHealth2, String str);
        }

        HealthServiceWrapper() {
        }

        IHealth getLastService() {
            return this.mLastService.get();
        }

        void init(Callback callback, IServiceManagerSupplier iServiceManagerSupplier, IHealthSupplier iHealthSupplier) throws RemoteException, NoSuchElementException, NullPointerException {
            if (callback == null || iServiceManagerSupplier == null || iHealthSupplier == null) {
                throw new NullPointerException();
            }
            this.mCallback = callback;
            this.mHealthSupplier = iHealthSupplier;
            Iterator<String> it = sAllInstances.iterator();
            IHealth iHealth = null;
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                String next = it.next();
                BatteryService.traceBegin("HealthInitGetService_" + next);
                try {
                    IHealth iHealth2 = iHealthSupplier.get(next);
                    BatteryService.traceEnd();
                    iHealth = iHealth2;
                } catch (NoSuchElementException e) {
                } finally {
                }
                if (iHealth != null) {
                    this.mInstanceName = next;
                    this.mLastService.set(iHealth);
                    break;
                }
            }
            if (this.mInstanceName == null || iHealth == null) {
                throw new NoSuchElementException(String.format("No IHealth service instance among %s is available. Perhaps no permission?", sAllInstances.toString()));
            }
            this.mCallback.onRegistration(null, iHealth, this.mInstanceName);
            BatteryService.traceBegin("HealthInitRegisterNotification");
            this.mHandlerThread.start();
            try {
                iServiceManagerSupplier.get().registerForNotifications(IHealth.kInterfaceName, this.mInstanceName, this.mNotification);
                BatteryService.traceEnd();
                Slog.i(TAG, "health: HealthServiceWrapper listening to instance " + this.mInstanceName);
            } finally {
            }
        }

        @VisibleForTesting
        HandlerThread getHandlerThread() {
            return this.mHandlerThread;
        }

        interface IServiceManagerSupplier {
            default IServiceManager get() throws RemoteException, NoSuchElementException {
                return IServiceManager.getService();
            }
        }

        interface IHealthSupplier {
            default IHealth get(String str) throws RemoteException, NoSuchElementException {
                return IHealth.getService(str, true);
            }
        }

        private class Notification extends IServiceNotification.Stub {
            private Notification() {
            }

            public final void onRegistration(String str, String str2, boolean z) {
                if (IHealth.kInterfaceName.equals(str) && HealthServiceWrapper.this.mInstanceName.equals(str2)) {
                    HealthServiceWrapper.this.mHandlerThread.getThreadHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                IHealth iHealth = HealthServiceWrapper.this.mHealthSupplier.get(HealthServiceWrapper.this.mInstanceName);
                                IHealth iHealth2 = (IHealth) HealthServiceWrapper.this.mLastService.getAndSet(iHealth);
                                if (Objects.equals(iHealth, iHealth2)) {
                                    return;
                                }
                                Slog.i(HealthServiceWrapper.TAG, "health: new instance registered " + HealthServiceWrapper.this.mInstanceName);
                                HealthServiceWrapper.this.mCallback.onRegistration(iHealth2, iHealth, HealthServiceWrapper.this.mInstanceName);
                            } catch (RemoteException | NoSuchElementException e) {
                                Slog.e(HealthServiceWrapper.TAG, "health: Cannot get instance '" + HealthServiceWrapper.this.mInstanceName + "': " + e.getMessage() + ". Perhaps no permission?");
                            }
                        }
                    });
                }
            }
        }
    }
}
