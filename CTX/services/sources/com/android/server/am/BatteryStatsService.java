package com.android.server.am;

import android.R;
import android.app.ActivityManager;
import android.bluetooth.BluetoothActivityEnergyInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiActivityEnergyInfo;
import android.os.BatteryStats;
import android.os.BatteryStatsInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFormatException;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.WorkSource;
import android.os.connectivity.CellularBatteryStats;
import android.os.connectivity.GpsBatteryStats;
import android.os.connectivity.WifiBatteryStats;
import android.os.health.HealthStatsParceler;
import android.os.health.HealthStatsWriter;
import android.os.health.UidHealthStats;
import android.telephony.ModemActivityInfo;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Slog;
import android.util.StatsLog;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.PowerProfile;
import com.android.internal.os.RpmStats;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.UiModeManagerService;
import com.android.server.utils.PriorityDump;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class BatteryStatsService extends IBatteryStats.Stub implements PowerManagerInternal.LowPowerModeListener, BatteryStatsImpl.PlatformIdleStateCallback {
    static final boolean DBG = false;
    private static final int MAX_LOW_POWER_STATS_SIZE = 2048;
    static final String TAG = "BatteryStatsService";
    private static IBatteryStats sService;
    private final Context mContext;
    final BatteryStatsImpl mStats;
    private final BatteryExternalStatsWorker mWorker;
    private CharsetDecoder mDecoderStat = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith("?");
    private ByteBuffer mUtf8BufferStat = ByteBuffer.allocateDirect(2048);
    private CharBuffer mUtf16BufferStat = CharBuffer.allocate(2048);
    private final BatteryStatsImpl.UserInfoProvider mUserManagerUserInfoProvider = new BatteryStatsImpl.UserInfoProvider() {
        private UserManagerInternal umi;

        public int[] getUserIds() {
            if (this.umi == null) {
                this.umi = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            }
            if (this.umi != null) {
                return this.umi.getUserIds();
            }
            return null;
        }
    };

    private native void getLowPowerStats(RpmStats rpmStats);

    private native int getPlatformLowPowerStats(ByteBuffer byteBuffer);

    private native int getSubsystemLowPowerStats(ByteBuffer byteBuffer);

    private static native int nativeWaitWakeup(ByteBuffer byteBuffer);

    public void fillLowPowerStats(RpmStats rpmStats) {
        getLowPowerStats(rpmStats);
    }

    public String getPlatformLowPowerStats() {
        this.mUtf8BufferStat.clear();
        this.mUtf16BufferStat.clear();
        this.mDecoderStat.reset();
        int platformLowPowerStats = getPlatformLowPowerStats(this.mUtf8BufferStat);
        if (platformLowPowerStats < 0) {
            return null;
        }
        if (platformLowPowerStats == 0) {
            return "Empty";
        }
        this.mUtf8BufferStat.limit(platformLowPowerStats);
        this.mDecoderStat.decode(this.mUtf8BufferStat, this.mUtf16BufferStat, true);
        this.mUtf16BufferStat.flip();
        return this.mUtf16BufferStat.toString();
    }

    public String getSubsystemLowPowerStats() {
        this.mUtf8BufferStat.clear();
        this.mUtf16BufferStat.clear();
        this.mDecoderStat.reset();
        int subsystemLowPowerStats = getSubsystemLowPowerStats(this.mUtf8BufferStat);
        if (subsystemLowPowerStats < 0) {
            return null;
        }
        if (subsystemLowPowerStats == 0) {
            return "Empty";
        }
        this.mUtf8BufferStat.limit(subsystemLowPowerStats);
        this.mDecoderStat.decode(this.mUtf8BufferStat, this.mUtf16BufferStat, true);
        this.mUtf16BufferStat.flip();
        return this.mUtf16BufferStat.toString();
    }

    BatteryStatsService(Context context, File file, Handler handler) {
        this.mContext = context;
        this.mStats = new BatteryStatsImpl(file, handler, this, this.mUserManagerUserInfoProvider);
        this.mWorker = new BatteryExternalStatsWorker(context, this.mStats);
        this.mStats.setExternalStatsSyncLocked(this.mWorker);
        this.mStats.setRadioScanningTimeoutLocked(((long) this.mContext.getResources().getInteger(R.integer.config_dockedStackDividerSnapMode)) * 1000);
        this.mStats.setPowerProfileLocked(new PowerProfile(context));
    }

    public void publish() {
        LocalServices.addService(BatteryStatsInternal.class, new LocalService());
        ServiceManager.addService("batterystats", asBinder());
    }

    public void systemServicesReady() {
        this.mStats.systemServicesReady(this.mContext);
    }

    private final class LocalService extends BatteryStatsInternal {
        private LocalService() {
        }

        public String[] getWifiIfaces() {
            return (String[]) BatteryStatsService.this.mStats.getWifiIfaces().clone();
        }

        public String[] getMobileIfaces() {
            return (String[]) BatteryStatsService.this.mStats.getMobileIfaces().clone();
        }

        public void noteJobsDeferred(int i, int i2, long j) {
            BatteryStatsService.this.noteJobsDeferred(i, i2, j);
        }
    }

    private static void awaitUninterruptibly(Future<?> future) {
        while (true) {
            try {
                future.get();
                return;
            } catch (InterruptedException e) {
            } catch (ExecutionException e2) {
                return;
            }
        }
    }

    private void syncStats(String str, int i) {
        awaitUninterruptibly(this.mWorker.scheduleSync(str, i));
    }

    public void initPowerManagement() {
        PowerManagerInternal powerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        powerManagerInternal.registerLowPowerModeObserver(this);
        synchronized (this.mStats) {
            this.mStats.notePowerSaveModeLocked(powerManagerInternal.getLowPowerState(9).batterySaverEnabled);
        }
        new WakeupReasonThread().start();
    }

    public void shutdown() {
        Slog.w("BatteryStats", "Writing battery stats before shutdown...");
        syncStats("shutdown", 31);
        synchronized (this.mStats) {
            this.mStats.shutdownLocked();
        }
        this.mWorker.shutdown();
    }

    public static IBatteryStats getService() {
        if (sService != null) {
            return sService;
        }
        sService = asInterface(ServiceManager.getService("batterystats"));
        return sService;
    }

    public int getServiceType() {
        return 9;
    }

    public void onLowPowerModeChanged(PowerSaveState powerSaveState) {
        synchronized (this.mStats) {
            this.mStats.notePowerSaveModeLocked(powerSaveState.batterySaverEnabled);
        }
    }

    public BatteryStatsImpl getActiveStatistics() {
        return this.mStats;
    }

    public void scheduleWriteToDisk() {
        this.mWorker.scheduleWrite();
    }

    void removeUid(int i) {
        synchronized (this.mStats) {
            this.mStats.removeUidStatsLocked(i);
        }
    }

    void onCleanupUser(int i) {
        synchronized (this.mStats) {
            this.mStats.onCleanupUserLocked(i);
        }
    }

    void onUserRemoved(int i) {
        synchronized (this.mStats) {
            this.mStats.onUserRemovedLocked(i);
        }
    }

    void addIsolatedUid(int i, int i2) {
        synchronized (this.mStats) {
            this.mStats.addIsolatedUidLocked(i, i2);
        }
    }

    void removeIsolatedUid(int i, int i2) {
        synchronized (this.mStats) {
            this.mStats.scheduleRemoveIsolatedUidLocked(i, i2);
        }
    }

    void noteProcessStart(String str, int i) {
        synchronized (this.mStats) {
            this.mStats.noteProcessStartLocked(str, i);
            StatsLog.write(28, i, str, 1);
        }
    }

    void noteProcessCrash(String str, int i) {
        synchronized (this.mStats) {
            this.mStats.noteProcessCrashLocked(str, i);
            StatsLog.write(28, i, str, 2);
        }
    }

    void noteProcessAnr(String str, int i) {
        synchronized (this.mStats) {
            this.mStats.noteProcessAnrLocked(str, i);
        }
    }

    void noteProcessFinish(String str, int i) {
        synchronized (this.mStats) {
            this.mStats.noteProcessFinishLocked(str, i);
            StatsLog.write(28, i, str, 0);
        }
    }

    void noteUidProcessState(int i, int i2) {
        synchronized (this.mStats) {
            StatsLog.write(27, i, ActivityManager.processStateAmToProto(i2));
            this.mStats.noteUidProcessStateLocked(i, i2);
        }
    }

    public byte[] getStatistics() {
        this.mContext.enforceCallingPermission("android.permission.BATTERY_STATS", null);
        Parcel parcelObtain = Parcel.obtain();
        syncStats("get-stats", 31);
        synchronized (this.mStats) {
            this.mStats.writeToParcel(parcelObtain, 0);
        }
        byte[] bArrMarshall = parcelObtain.marshall();
        parcelObtain.recycle();
        return bArrMarshall;
    }

    public ParcelFileDescriptor getStatisticsStream() {
        this.mContext.enforceCallingPermission("android.permission.BATTERY_STATS", null);
        Parcel parcelObtain = Parcel.obtain();
        syncStats("get-stats", 31);
        synchronized (this.mStats) {
            this.mStats.writeToParcel(parcelObtain, 0);
        }
        byte[] bArrMarshall = parcelObtain.marshall();
        parcelObtain.recycle();
        try {
            return ParcelFileDescriptor.fromData(bArrMarshall, "battery-stats");
        } catch (IOException e) {
            Slog.w(TAG, "Unable to create shared memory", e);
            return null;
        }
    }

    public boolean isCharging() {
        boolean zIsCharging;
        synchronized (this.mStats) {
            zIsCharging = this.mStats.isCharging();
        }
        return zIsCharging;
    }

    public long computeBatteryTimeRemaining() {
        long jComputeBatteryTimeRemaining;
        synchronized (this.mStats) {
            jComputeBatteryTimeRemaining = this.mStats.computeBatteryTimeRemaining(SystemClock.elapsedRealtime());
            if (jComputeBatteryTimeRemaining >= 0) {
                jComputeBatteryTimeRemaining /= 1000;
            }
        }
        return jComputeBatteryTimeRemaining;
    }

    public long computeChargeTimeRemaining() {
        long jComputeChargeTimeRemaining;
        synchronized (this.mStats) {
            jComputeChargeTimeRemaining = this.mStats.computeChargeTimeRemaining(SystemClock.elapsedRealtime());
            if (jComputeChargeTimeRemaining >= 0) {
                jComputeChargeTimeRemaining /= 1000;
            }
        }
        return jComputeChargeTimeRemaining;
    }

    public void noteEvent(int i, String str, int i2) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteEventLocked(i, str, i2);
        }
    }

    public void noteSyncStart(String str, int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteSyncStartLocked(str, i);
            StatsLog.write_non_chained(7, i, null, str, 1);
        }
    }

    public void noteSyncFinish(String str, int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteSyncFinishLocked(str, i);
            StatsLog.write_non_chained(7, i, null, str, 0);
        }
    }

    public void noteJobStart(String str, int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteJobStartLocked(str, i);
            StatsLog.write_non_chained(8, i, null, str, 1, -1);
        }
    }

    public void noteJobFinish(String str, int i, int i2) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteJobFinishLocked(str, i, i2);
            StatsLog.write_non_chained(8, i, null, str, 0, i2);
        }
    }

    void noteJobsDeferred(int i, int i2, long j) {
        synchronized (this.mStats) {
            this.mStats.noteJobsDeferredLocked(i, i2, j);
        }
    }

    public void noteWakupAlarm(String str, int i, WorkSource workSource, String str2) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWakupAlarmLocked(str, i, workSource, str2);
        }
    }

    public void noteAlarmStart(String str, WorkSource workSource, int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteAlarmStartLocked(str, workSource, i);
        }
    }

    public void noteAlarmFinish(String str, WorkSource workSource, int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteAlarmFinishLocked(str, workSource, i);
        }
    }

    public void noteStartWakelock(int i, int i2, String str, String str2, int i3, boolean z) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStartWakeLocked(i, i2, (WorkSource.WorkChain) null, str, str2, i3, z, SystemClock.elapsedRealtime(), SystemClock.uptimeMillis());
        }
    }

    public void noteStopWakelock(int i, int i2, String str, String str2, int i3) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStopWakeLocked(i, i2, (WorkSource.WorkChain) null, str, str2, i3, SystemClock.elapsedRealtime(), SystemClock.uptimeMillis());
        }
    }

    public void noteStartWakelockFromSource(WorkSource workSource, int i, String str, String str2, int i2, boolean z) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStartWakeFromSourceLocked(workSource, i, str, str2, i2, z);
        }
    }

    public void noteChangeWakelockFromSource(WorkSource workSource, int i, String str, String str2, int i2, WorkSource workSource2, int i3, String str3, String str4, int i4, boolean z) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteChangeWakelockFromSourceLocked(workSource, i, str, str2, i2, workSource2, i3, str3, str4, i4, z);
        }
    }

    public void noteStopWakelockFromSource(WorkSource workSource, int i, String str, String str2, int i2) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStopWakeFromSourceLocked(workSource, i, str, str2, i2);
        }
    }

    public void noteLongPartialWakelockStart(String str, String str2, int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockStart(str, str2, i);
        }
    }

    public void noteLongPartialWakelockStartFromSource(String str, String str2, WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockStartFromSource(str, str2, workSource);
        }
    }

    public void noteLongPartialWakelockFinish(String str, String str2, int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockFinish(str, str2, i);
        }
    }

    public void noteLongPartialWakelockFinishFromSource(String str, String str2, WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteLongPartialWakelockFinishFromSource(str, str2, workSource);
        }
    }

    public void noteStartSensor(int i, int i2) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStartSensorLocked(i, i2);
            StatsLog.write_non_chained(5, i, null, i2, 1);
        }
    }

    public void noteStopSensor(int i, int i2) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteStopSensorLocked(i, i2);
            StatsLog.write_non_chained(5, i, null, i2, 0);
        }
    }

    public void noteVibratorOn(int i, long j) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteVibratorOnLocked(i, j);
        }
    }

    public void noteVibratorOff(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteVibratorOffLocked(i);
        }
    }

    public void noteGpsChanged(WorkSource workSource, WorkSource workSource2) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteGpsChangedLocked(workSource, workSource2);
        }
    }

    public void noteGpsSignalQuality(int i) {
        synchronized (this.mStats) {
            this.mStats.noteGpsSignalQualityLocked(i);
        }
    }

    public void noteScreenState(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            StatsLog.write(29, i);
            this.mStats.noteScreenStateLocked(i);
        }
    }

    public void noteScreenBrightness(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            StatsLog.write(9, i);
            this.mStats.noteScreenBrightnessLocked(i);
        }
    }

    public void noteUserActivity(int i, int i2) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteUserActivityLocked(i, i2);
        }
    }

    public void noteWakeUp(String str, int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWakeUpLocked(str, i);
        }
    }

    public void noteInteractive(boolean z) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteInteractiveLocked(z);
        }
    }

    public void noteConnectivityChanged(int i, String str) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteConnectivityChangedLocked(i, str);
        }
    }

    public void noteMobileRadioPowerState(int i, long j, int i2) {
        boolean zNoteMobileRadioPowerStateLocked;
        enforceCallingPermission();
        synchronized (this.mStats) {
            zNoteMobileRadioPowerStateLocked = this.mStats.noteMobileRadioPowerStateLocked(i, j, i2);
        }
        if (zNoteMobileRadioPowerStateLocked) {
            this.mWorker.scheduleSync("modem-data", 4);
        }
    }

    public void notePhoneOn() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePhoneOnLocked();
        }
    }

    public void notePhoneOff() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePhoneOffLocked();
        }
    }

    public void notePhoneSignalStrength(SignalStrength signalStrength) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePhoneSignalStrengthLocked(signalStrength);
        }
    }

    public void notePhoneDataConnectionState(int i, boolean z) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePhoneDataConnectionStateLocked(i, z);
        }
    }

    public void notePhoneState(int i) {
        enforceCallingPermission();
        int simState = TelephonyManager.getDefault().getSimState();
        synchronized (this.mStats) {
            this.mStats.notePhoneStateLocked(i, simState);
        }
    }

    public void noteWifiOn() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiOnLocked();
        }
    }

    public void noteWifiOff() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiOffLocked();
        }
    }

    public void noteStartAudio(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteAudioOnLocked(i);
            StatsLog.write_non_chained(23, i, null, 1);
        }
    }

    public void noteStopAudio(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteAudioOffLocked(i);
            StatsLog.write_non_chained(23, i, null, 0);
        }
    }

    public void noteStartVideo(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteVideoOnLocked(i);
            StatsLog.write_non_chained(24, i, null, 1);
        }
    }

    public void noteStopVideo(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteVideoOffLocked(i);
            StatsLog.write_non_chained(24, i, null, 0);
        }
    }

    public void noteResetAudio() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetAudioLocked();
            StatsLog.write_non_chained(23, -1, null, 2);
        }
    }

    public void noteResetVideo() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetVideoLocked();
            StatsLog.write_non_chained(24, -1, null, 2);
        }
    }

    public void noteFlashlightOn(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFlashlightOnLocked(i);
            StatsLog.write_non_chained(26, i, null, 1);
        }
    }

    public void noteFlashlightOff(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFlashlightOffLocked(i);
            StatsLog.write_non_chained(26, i, null, 0);
        }
    }

    public void noteStartCamera(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteCameraOnLocked(i);
            StatsLog.write_non_chained(25, i, null, 1);
        }
    }

    public void noteStopCamera(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteCameraOffLocked(i);
            StatsLog.write_non_chained(25, i, null, 0);
        }
    }

    public void noteResetCamera() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetCameraLocked();
            StatsLog.write_non_chained(25, -1, null, 2);
        }
    }

    public void noteResetFlashlight() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetFlashlightLocked();
            StatsLog.write_non_chained(26, -1, null, 2);
        }
    }

    public void noteWifiRadioPowerState(int i, long j, int i2) {
        String str;
        enforceCallingPermission();
        synchronized (this.mStats) {
            if (this.mStats.isOnBattery()) {
                if (i == 3 || i == 2) {
                    str = "active";
                } else {
                    str = "inactive";
                }
                this.mWorker.scheduleSync("wifi-data: " + str, 2);
            }
            this.mStats.noteWifiRadioPowerState(i, j, i2);
        }
    }

    public void noteWifiRunning(WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiRunningLocked(workSource);
        }
    }

    public void noteWifiRunningChanged(WorkSource workSource, WorkSource workSource2) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiRunningChangedLocked(workSource, workSource2);
        }
    }

    public void noteWifiStopped(WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiStoppedLocked(workSource);
        }
    }

    public void noteWifiState(int i, String str) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiStateLocked(i, str);
        }
    }

    public void noteWifiSupplicantStateChanged(int i, boolean z) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiSupplicantStateChangedLocked(i, z);
        }
    }

    public void noteWifiRssiChanged(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiRssiChangedLocked(i);
        }
    }

    public void noteFullWifiLockAcquired(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockAcquiredLocked(i);
        }
    }

    public void noteFullWifiLockReleased(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockReleasedLocked(i);
        }
    }

    public void noteWifiScanStarted(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStartedLocked(i);
        }
    }

    public void noteWifiScanStopped(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStoppedLocked(i);
        }
    }

    public void noteWifiMulticastEnabled(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiMulticastEnabledLocked(i);
        }
    }

    public void noteWifiMulticastDisabled(int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiMulticastDisabledLocked(i);
        }
    }

    public void noteFullWifiLockAcquiredFromSource(WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockAcquiredFromSourceLocked(workSource);
        }
    }

    public void noteFullWifiLockReleasedFromSource(WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteFullWifiLockReleasedFromSourceLocked(workSource);
        }
    }

    public void noteWifiScanStartedFromSource(WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStartedFromSourceLocked(workSource);
        }
    }

    public void noteWifiScanStoppedFromSource(WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiScanStoppedFromSourceLocked(workSource);
        }
    }

    public void noteWifiBatchedScanStartedFromSource(WorkSource workSource, int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiBatchedScanStartedFromSourceLocked(workSource, i);
        }
    }

    public void noteWifiBatchedScanStoppedFromSource(WorkSource workSource) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteWifiBatchedScanStoppedFromSourceLocked(workSource);
        }
    }

    public void noteNetworkInterfaceType(String str, int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteNetworkInterfaceTypeLocked(str, i);
        }
    }

    public void noteNetworkStatsEnabled() {
        enforceCallingPermission();
        this.mWorker.scheduleSync("network-stats-enabled", 6);
    }

    public void noteDeviceIdleMode(int i, String str, int i2) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteDeviceIdleModeLocked(i, str, i2);
        }
    }

    public void notePackageInstalled(String str, long j) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePackageInstalledLocked(str, j);
        }
    }

    public void notePackageUninstalled(String str) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.notePackageUninstalledLocked(str);
        }
    }

    public void noteBleScanStarted(WorkSource workSource, boolean z) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteBluetoothScanStartedFromSourceLocked(workSource, z);
        }
    }

    public void noteBleScanStopped(WorkSource workSource, boolean z) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteBluetoothScanStoppedFromSourceLocked(workSource, z);
        }
    }

    public void noteResetBleScan() {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteResetBluetoothScanLocked();
        }
    }

    public void noteBleScanResults(WorkSource workSource, int i) {
        enforceCallingPermission();
        synchronized (this.mStats) {
            this.mStats.noteBluetoothScanResultsFromSourceLocked(workSource, i);
        }
    }

    public void noteWifiControllerActivity(WifiActivityEnergyInfo wifiActivityEnergyInfo) {
        enforceCallingPermission();
        if (wifiActivityEnergyInfo == null || !wifiActivityEnergyInfo.isValid()) {
            Slog.e(TAG, "invalid wifi data given: " + wifiActivityEnergyInfo);
            return;
        }
        this.mStats.updateWifiState(wifiActivityEnergyInfo);
    }

    public void noteBluetoothControllerActivity(BluetoothActivityEnergyInfo bluetoothActivityEnergyInfo) {
        enforceCallingPermission();
        if (bluetoothActivityEnergyInfo == null || !bluetoothActivityEnergyInfo.isValid()) {
            Slog.e(TAG, "invalid bluetooth data given: " + bluetoothActivityEnergyInfo);
            return;
        }
        synchronized (this.mStats) {
            this.mStats.updateBluetoothStateLocked(bluetoothActivityEnergyInfo);
        }
    }

    public void noteModemControllerActivity(ModemActivityInfo modemActivityInfo) {
        enforceCallingPermission();
        if (modemActivityInfo == null || !modemActivityInfo.isValid()) {
            Slog.e(TAG, "invalid modem data given: " + modemActivityInfo);
            return;
        }
        this.mStats.updateMobileRadioState(modemActivityInfo);
    }

    public boolean isOnBattery() {
        return this.mStats.isOnBattery();
    }

    public void setBatteryState(final int i, final int i2, final int i3, final int i4, final int i5, final int i6, final int i7, final int i8) {
        enforceCallingPermission();
        this.mWorker.scheduleRunnable(new Runnable() {
            @Override
            public final void run() {
                BatteryStatsService.lambda$setBatteryState$1(this.f$0, i3, i, i2, i4, i5, i6, i7, i8);
            }
        });
    }

    public static void lambda$setBatteryState$1(final BatteryStatsService batteryStatsService, final int i, final int i2, final int i3, final int i4, final int i5, final int i6, final int i7, final int i8) {
        synchronized (batteryStatsService.mStats) {
            if (batteryStatsService.mStats.isOnBattery() == BatteryStatsImpl.isOnBattery(i, i2)) {
                batteryStatsService.mStats.setBatteryStateLocked(i2, i3, i, i4, i5, i6, i7, i8);
            } else {
                batteryStatsService.mWorker.scheduleSync("battery-state", 31);
                batteryStatsService.mWorker.scheduleRunnable(new Runnable() {
                    @Override
                    public final void run() {
                        BatteryStatsService.lambda$setBatteryState$0(this.f$0, i2, i3, i, i4, i5, i6, i7, i8);
                    }
                });
            }
        }
    }

    public static void lambda$setBatteryState$0(BatteryStatsService batteryStatsService, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        synchronized (batteryStatsService.mStats) {
            batteryStatsService.mStats.setBatteryStateLocked(i, i2, i3, i4, i5, i6, i7, i8);
        }
    }

    public long getAwakeTimeBattery() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        return this.mStats.getAwakeTimeBattery();
    }

    public long getAwakeTimePlugged() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        return this.mStats.getAwakeTimePlugged();
    }

    public void enforceCallingPermission() {
        if (Binder.getCallingPid() == Process.myPid()) {
            return;
        }
        this.mContext.enforcePermission("android.permission.UPDATE_DEVICE_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
    }

    final class WakeupReasonThread extends Thread {
        private static final int MAX_REASON_SIZE = 512;
        private CharsetDecoder mDecoder;
        private CharBuffer mUtf16Buffer;
        private ByteBuffer mUtf8Buffer;

        WakeupReasonThread() {
            super("BatteryStats_wakeupReason");
        }

        @Override
        public void run() {
            Process.setThreadPriority(-2);
            this.mDecoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith("?");
            this.mUtf8Buffer = ByteBuffer.allocateDirect(512);
            this.mUtf16Buffer = CharBuffer.allocate(512);
            while (true) {
                try {
                    String strWaitWakeup = waitWakeup();
                    if (strWaitWakeup != null) {
                        synchronized (BatteryStatsService.this.mStats) {
                            BatteryStatsService.this.mStats.noteWakeupReasonLocked(strWaitWakeup);
                        }
                    } else {
                        return;
                    }
                } catch (RuntimeException e) {
                    Slog.e(BatteryStatsService.TAG, "Failure reading wakeup reasons", e);
                    return;
                }
            }
        }

        private String waitWakeup() {
            this.mUtf8Buffer.clear();
            this.mUtf16Buffer.clear();
            this.mDecoder.reset();
            int iNativeWaitWakeup = BatteryStatsService.nativeWaitWakeup(this.mUtf8Buffer);
            if (iNativeWaitWakeup < 0) {
                return null;
            }
            if (iNativeWaitWakeup == 0) {
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
            }
            this.mUtf8Buffer.limit(iNativeWaitWakeup);
            this.mDecoder.decode(this.mUtf8Buffer, this.mUtf16Buffer, true);
            this.mUtf16Buffer.flip();
            return this.mUtf16Buffer.toString();
        }
    }

    private void dumpHelp(PrintWriter printWriter) {
        printWriter.println("Battery stats (batterystats) dump options:");
        printWriter.println("  [--checkin] [--proto] [--history] [--history-start] [--charged] [-c]");
        printWriter.println("  [--daily] [--reset] [--write] [--new-daily] [--read-daily] [-h] [<package.name>]");
        printWriter.println("  --checkin: generate output for a checkin report; will write (and clear) the");
        printWriter.println("             last old completed stats when they had been reset.");
        printWriter.println("  -c: write the current stats in checkin format.");
        printWriter.println("  --proto: write the current aggregate stats (without history) in proto format.");
        printWriter.println("  --history: show only history data.");
        printWriter.println("  --history-start <num>: show only history data starting at given time offset.");
        printWriter.println("  --charged: only output data since last charged.");
        printWriter.println("  --daily: only output full daily data.");
        printWriter.println("  --reset: reset the stats, clearing all current data.");
        printWriter.println("  --write: force write current collected stats to disk.");
        printWriter.println("  --new-daily: immediately create and write new daily stats record.");
        printWriter.println("  --read-daily: read-load last written daily stats.");
        printWriter.println("  --settings: dump the settings key/values related to batterystats");
        printWriter.println("  --cpu: dump cpu stats for debugging purpose");
        printWriter.println("  <package.name>: optional name of package to filter output by.");
        printWriter.println("  -h: print this help text.");
        printWriter.println("Battery stats (batterystats) commands:");
        printWriter.println("  enable|disable <option>");
        printWriter.println("    Enable or disable a running option.  Option state is not saved across boots.");
        printWriter.println("    Options are:");
        printWriter.println("      full-history: include additional detailed events in battery history:");
        printWriter.println("          wake_lock_in, alarms and proc events");
        printWriter.println("      no-auto-reset: don't automatically reset stats when unplugged");
        printWriter.println("      pretend-screen-off: pretend the screen is off, even if screen state changes");
    }

    private void dumpSettings(PrintWriter printWriter) {
        synchronized (this.mStats) {
            this.mStats.dumpConstantsLocked(printWriter);
        }
    }

    private void dumpCpuStats(PrintWriter printWriter) {
        synchronized (this.mStats) {
            this.mStats.dumpCpuStatsLocked(printWriter);
        }
    }

    private int doEnableOrDisable(PrintWriter printWriter, int i, String[] strArr, boolean z) {
        int i2 = i + 1;
        if (i2 >= strArr.length) {
            StringBuilder sb = new StringBuilder();
            sb.append("Missing option argument for ");
            sb.append(z ? "--enable" : "--disable");
            printWriter.println(sb.toString());
            dumpHelp(printWriter);
            return -1;
        }
        if ("full-wake-history".equals(strArr[i2]) || "full-history".equals(strArr[i2])) {
            synchronized (this.mStats) {
                this.mStats.setRecordAllHistoryLocked(z);
            }
        } else if ("no-auto-reset".equals(strArr[i2])) {
            synchronized (this.mStats) {
                this.mStats.setNoAutoReset(z);
            }
        } else if ("pretend-screen-off".equals(strArr[i2])) {
            synchronized (this.mStats) {
                this.mStats.setPretendScreenOff(z);
            }
        } else {
            printWriter.println("Unknown enable/disable option: " + strArr[i2]);
            dumpHelp(printWriter);
            return -1;
        }
        return i2;
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        long j;
        int i;
        boolean z;
        int i2;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, printWriter)) {
            if (strArr != null) {
                j = -1;
                int packageUidAsUser = -1;
                int i3 = 0;
                z = false;
                i2 = 0;
                z2 = false;
                z3 = false;
                z4 = false;
                z5 = false;
                while (i3 < strArr.length) {
                    String str = strArr[i3];
                    if ("--checkin".equals(str)) {
                        z3 = true;
                        z4 = true;
                    } else if ("--history".equals(str)) {
                        i2 |= 8;
                    } else if ("--history-start".equals(str)) {
                        i2 |= 8;
                        i3++;
                        if (i3 >= strArr.length) {
                            printWriter.println("Missing time argument for --history-since");
                            dumpHelp(printWriter);
                            return;
                        } else {
                            j = Long.parseLong(strArr[i3]);
                            z5 = true;
                        }
                    } else if ("-c".equals(str)) {
                        i2 |= 16;
                        z4 = true;
                    } else if (PriorityDump.PROTO_ARG.equals(str)) {
                        z2 = true;
                    } else if ("--charged".equals(str)) {
                        i2 |= 2;
                    } else if ("--daily".equals(str)) {
                        i2 |= 4;
                    } else {
                        if ("--reset".equals(str)) {
                            synchronized (this.mStats) {
                                this.mStats.resetAllStatsCmdLocked();
                                printWriter.println("Battery stats reset.");
                            }
                            this.mWorker.scheduleSync("dump", 31);
                        } else if ("--write".equals(str)) {
                            syncStats("dump", 31);
                            synchronized (this.mStats) {
                                this.mStats.writeSyncLocked();
                                printWriter.println("Battery stats written.");
                            }
                        } else if ("--new-daily".equals(str)) {
                            synchronized (this.mStats) {
                                this.mStats.recordDailyStatsLocked();
                                printWriter.println("New daily stats written.");
                            }
                        } else if ("--read-daily".equals(str)) {
                            synchronized (this.mStats) {
                                this.mStats.readDailyStatsLocked();
                                printWriter.println("Last daily stats read.");
                            }
                        } else {
                            if ("--enable".equals(str) || "enable".equals(str)) {
                                int iDoEnableOrDisable = doEnableOrDisable(printWriter, i3, strArr, true);
                                if (iDoEnableOrDisable < 0) {
                                    return;
                                }
                                printWriter.println("Enabled: " + strArr[iDoEnableOrDisable]);
                                return;
                            }
                            if ("--disable".equals(str) || "disable".equals(str)) {
                                int iDoEnableOrDisable2 = doEnableOrDisable(printWriter, i3, strArr, false);
                                if (iDoEnableOrDisable2 < 0) {
                                    return;
                                }
                                printWriter.println("Disabled: " + strArr[iDoEnableOrDisable2]);
                                return;
                            }
                            if ("-h".equals(str)) {
                                dumpHelp(printWriter);
                                return;
                            }
                            if ("--settings".equals(str)) {
                                dumpSettings(printWriter);
                                return;
                            }
                            if ("--cpu".equals(str)) {
                                dumpCpuStats(printWriter);
                                return;
                            }
                            if ("-a".equals(str)) {
                                i2 |= 32;
                            } else {
                                if (str.length() > 0 && str.charAt(0) == '-') {
                                    printWriter.println("Unknown option: " + str);
                                    dumpHelp(printWriter);
                                    return;
                                }
                                try {
                                    packageUidAsUser = this.mContext.getPackageManager().getPackageUidAsUser(str, UserHandle.getCallingUserId());
                                } catch (PackageManager.NameNotFoundException e) {
                                    printWriter.println("Unknown package: " + str);
                                    dumpHelp(printWriter);
                                    return;
                                }
                            }
                        }
                        z = true;
                    }
                    i3++;
                }
                i = packageUidAsUser;
            } else {
                j = -1;
                i = -1;
                z = false;
                i2 = 0;
                z2 = false;
                z3 = false;
                z4 = false;
                z5 = false;
            }
            if (z) {
                return;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (BatteryStatsHelper.checkWifiOnly(this.mContext)) {
                    i2 |= 64;
                }
                syncStats("dump", 31);
                int i4 = (i < 0 || (i2 & 10) != 0) ? i2 : (i2 | 2) & (-17);
                if (z2) {
                    List<ApplicationInfo> installedApplications = this.mContext.getPackageManager().getInstalledApplications(4325376);
                    if (z3) {
                        synchronized (this.mStats.mCheckinFile) {
                            if (this.mStats.mCheckinFile.exists()) {
                                try {
                                    byte[] fully = this.mStats.mCheckinFile.readFully();
                                    if (fully != null) {
                                        Parcel parcelObtain = Parcel.obtain();
                                        parcelObtain.unmarshall(fully, 0, fully.length);
                                        parcelObtain.setDataPosition(0);
                                        BatteryStatsImpl batteryStatsImpl = new BatteryStatsImpl((File) null, this.mStats.mHandler, (BatteryStatsImpl.PlatformIdleStateCallback) null, this.mUserManagerUserInfoProvider);
                                        batteryStatsImpl.readSummaryFromParcel(parcelObtain);
                                        parcelObtain.recycle();
                                        batteryStatsImpl.dumpProtoLocked(this.mContext, fileDescriptor, installedApplications, i4, j);
                                        this.mStats.mCheckinFile.delete();
                                        return;
                                    }
                                } catch (ParcelFormatException | IOException e2) {
                                    Slog.w(TAG, "Failure reading checkin file " + this.mStats.mCheckinFile.getBaseFile(), e2);
                                }
                            }
                        }
                    }
                    synchronized (this.mStats) {
                        this.mStats.dumpProtoLocked(this.mContext, fileDescriptor, installedApplications, i4, j);
                        if (z5) {
                            this.mStats.writeAsyncLocked();
                        }
                    }
                    return;
                }
                if (!z4) {
                    synchronized (this.mStats) {
                        this.mStats.dumpLocked(this.mContext, printWriter, i4, i, j);
                        if (z5) {
                            this.mStats.writeAsyncLocked();
                        }
                    }
                    return;
                }
                List<ApplicationInfo> installedApplications2 = this.mContext.getPackageManager().getInstalledApplications(4325376);
                if (z3) {
                    synchronized (this.mStats.mCheckinFile) {
                        if (this.mStats.mCheckinFile.exists()) {
                            try {
                                byte[] fully2 = this.mStats.mCheckinFile.readFully();
                                if (fully2 != null) {
                                    Parcel parcelObtain2 = Parcel.obtain();
                                    parcelObtain2.unmarshall(fully2, 0, fully2.length);
                                    parcelObtain2.setDataPosition(0);
                                    BatteryStatsImpl batteryStatsImpl2 = new BatteryStatsImpl((File) null, this.mStats.mHandler, (BatteryStatsImpl.PlatformIdleStateCallback) null, this.mUserManagerUserInfoProvider);
                                    batteryStatsImpl2.readSummaryFromParcel(parcelObtain2);
                                    parcelObtain2.recycle();
                                    batteryStatsImpl2.dumpCheckinLocked(this.mContext, printWriter, installedApplications2, i4, j);
                                    this.mStats.mCheckinFile.delete();
                                    return;
                                }
                            } catch (ParcelFormatException | IOException e3) {
                                Slog.w(TAG, "Failure reading checkin file " + this.mStats.mCheckinFile.getBaseFile(), e3);
                            }
                        }
                    }
                }
                synchronized (this.mStats) {
                    this.mStats.dumpCheckinLocked(this.mContext, printWriter, installedApplications2, i4, j);
                    if (z5) {
                        this.mStats.writeAsyncLocked();
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public CellularBatteryStats getCellularBatteryStats() {
        CellularBatteryStats cellularBatteryStats;
        synchronized (this.mStats) {
            cellularBatteryStats = this.mStats.getCellularBatteryStats();
        }
        return cellularBatteryStats;
    }

    public WifiBatteryStats getWifiBatteryStats() {
        WifiBatteryStats wifiBatteryStats;
        synchronized (this.mStats) {
            wifiBatteryStats = this.mStats.getWifiBatteryStats();
        }
        return wifiBatteryStats;
    }

    public GpsBatteryStats getGpsBatteryStats() {
        GpsBatteryStats gpsBatteryStats;
        synchronized (this.mStats) {
            gpsBatteryStats = this.mStats.getGpsBatteryStats();
        }
        return gpsBatteryStats;
    }

    public HealthStatsParceler takeUidSnapshot(int i) {
        HealthStatsParceler healthStatsForUidLocked;
        if (i != Binder.getCallingUid()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                if (shouldCollectExternalStats()) {
                    syncStats("get-health-stats-for-uids", 31);
                }
                synchronized (this.mStats) {
                    healthStatsForUidLocked = getHealthStatsForUidLocked(i);
                }
                return healthStatsForUidLocked;
            } catch (Exception e) {
                Slog.w(TAG, "Crashed while writing for takeUidSnapshot(" + i + ")", e);
                throw e;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public HealthStatsParceler[] takeUidSnapshots(int[] iArr) {
        HealthStatsParceler[] healthStatsParcelerArr;
        if (!onlyCaller(iArr)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BATTERY_STATS", null);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                if (shouldCollectExternalStats()) {
                    syncStats("get-health-stats-for-uids", 31);
                }
                synchronized (this.mStats) {
                    int length = iArr.length;
                    healthStatsParcelerArr = new HealthStatsParceler[length];
                    for (int i = 0; i < length; i++) {
                        healthStatsParcelerArr[i] = getHealthStatsForUidLocked(iArr[i]);
                    }
                }
                return healthStatsParcelerArr;
            } catch (Exception e) {
                throw e;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean shouldCollectExternalStats() {
        return SystemClock.elapsedRealtime() - this.mWorker.getLastCollectionTimeStamp() > this.mStats.getExternalStatsCollectionRateLimitMs();
    }

    private static boolean onlyCaller(int[] iArr) {
        int callingUid = Binder.getCallingUid();
        for (int i : iArr) {
            if (i != callingUid) {
                return false;
            }
        }
        return true;
    }

    HealthStatsParceler getHealthStatsForUidLocked(int i) {
        HealthStatsBatteryStatsWriter healthStatsBatteryStatsWriter = new HealthStatsBatteryStatsWriter();
        HealthStatsWriter healthStatsWriter = new HealthStatsWriter(UidHealthStats.CONSTANTS);
        BatteryStats.Uid uid = (BatteryStats.Uid) this.mStats.getUidStats().get(i);
        if (uid != null) {
            healthStatsBatteryStatsWriter.writeUid(healthStatsWriter, this.mStats, uid);
        }
        return new HealthStatsParceler(healthStatsWriter);
    }
}
