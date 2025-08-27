package com.android.settings.fuelgauge;

import android.os.BatteryStats;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.util.SparseIntArray;

/* loaded from: classes.dex */
public class FakeUid extends BatteryStats.Uid {
    private final int mUid;

    public FakeUid(int i) {
        this.mUid = i;
    }

    public int getUid() {
        return this.mUid;
    }

    public ArrayMap<String, ? extends BatteryStats.Uid.Wakelock> getWakelockStats() {
        return null;
    }

    public BatteryStats.Timer getAggregatedPartialWakelockTimer() {
        return null;
    }

    public BatteryStats.Timer getMulticastWakelockStats() {
        return null;
    }

    public ArrayMap<String, ? extends BatteryStats.Timer> getSyncStats() {
        return null;
    }

    public ArrayMap<String, ? extends BatteryStats.Timer> getJobStats() {
        return null;
    }

    public ArrayMap<String, SparseIntArray> getJobCompletionStats() {
        return null;
    }

    public SparseArray<? extends BatteryStats.Uid.Sensor> getSensorStats() {
        return null;
    }

    public SparseArray<? extends BatteryStats.Uid.Pid> getPidStats() {
        return null;
    }

    public ArrayMap<String, ? extends BatteryStats.Uid.Proc> getProcessStats() {
        return null;
    }

    public ArrayMap<String, ? extends BatteryStats.Uid.Pkg> getPackageStats() {
        return null;
    }

    public void noteWifiRunningLocked(long j) {
    }

    public void noteWifiStoppedLocked(long j) {
    }

    public void noteFullWifiLockAcquiredLocked(long j) {
    }

    public void noteFullWifiLockReleasedLocked(long j) {
    }

    public void noteWifiScanStartedLocked(long j) {
    }

    public void noteWifiScanStoppedLocked(long j) {
    }

    public void noteWifiBatchedScanStartedLocked(int i, long j) {
    }

    public void noteWifiBatchedScanStoppedLocked(long j) {
    }

    public void noteWifiMulticastEnabledLocked(long j) {
    }

    public void noteWifiMulticastDisabledLocked(long j) {
    }

    public void noteActivityResumedLocked(long j) {
    }

    public void noteActivityPausedLocked(long j) {
    }

    public long getWifiRunningTime(long j, int i) {
        return 0L;
    }

    public long getFullWifiLockTime(long j, int i) {
        return 0L;
    }

    public long getWifiScanTime(long j, int i) {
        return 0L;
    }

    public int getWifiScanCount(int i) {
        return 0;
    }

    public BatteryStats.Timer getWifiScanTimer() {
        return null;
    }

    public int getWifiScanBackgroundCount(int i) {
        return 0;
    }

    public long getWifiScanActualTime(long j) {
        return 0L;
    }

    public long getWifiScanBackgroundTime(long j) {
        return 0L;
    }

    public BatteryStats.Timer getWifiScanBackgroundTimer() {
        return null;
    }

    public long getWifiBatchedScanTime(int i, long j, int i2) {
        return 0L;
    }

    public int getWifiBatchedScanCount(int i, int i2) {
        return 0;
    }

    public long getWifiMulticastTime(long j, int i) {
        return 0L;
    }

    public BatteryStats.Timer getAudioTurnedOnTimer() {
        return null;
    }

    public BatteryStats.Timer getVideoTurnedOnTimer() {
        return null;
    }

    public BatteryStats.Timer getFlashlightTurnedOnTimer() {
        return null;
    }

    public BatteryStats.Timer getCameraTurnedOnTimer() {
        return null;
    }

    public BatteryStats.Timer getForegroundActivityTimer() {
        return null;
    }

    public BatteryStats.Timer getForegroundServiceTimer() {
        return null;
    }

    public long getProcessStateTime(int i, long j, int i2) {
        return 0L;
    }

    public BatteryStats.Timer getProcessStateTimer(int i) {
        return null;
    }

    public BatteryStats.Timer getVibratorOnTimer() {
        return null;
    }

    public void noteUserActivityLocked(int i) {
    }

    public boolean hasUserActivity() {
        return false;
    }

    public int getUserActivityCount(int i, int i2) {
        return 0;
    }

    public boolean hasNetworkActivity() {
        return false;
    }

    public long getNetworkActivityBytes(int i, int i2) {
        return 0L;
    }

    public long getNetworkActivityPackets(int i, int i2) {
        return 0L;
    }

    public long getMobileRadioActiveTime(int i) {
        return 0L;
    }

    public int getMobileRadioActiveCount(int i) {
        return 0;
    }

    public long getUserCpuTimeUs(int i) {
        return 0L;
    }

    public long getSystemCpuTimeUs(int i) {
        return 0L;
    }

    public long getTimeAtCpuSpeed(int i, int i2, int i3) {
        return 0L;
    }

    public BatteryStats.ControllerActivityCounter getWifiControllerActivity() {
        return null;
    }

    public BatteryStats.ControllerActivityCounter getBluetoothControllerActivity() {
        return null;
    }

    public BatteryStats.ControllerActivityCounter getModemControllerActivity() {
        return null;
    }

    public BatteryStats.Timer getBluetoothScanTimer() {
        return null;
    }

    public BatteryStats.Timer getBluetoothScanBackgroundTimer() {
        return null;
    }

    public BatteryStats.Timer getBluetoothUnoptimizedScanTimer() {
        return null;
    }

    public BatteryStats.Timer getBluetoothUnoptimizedScanBackgroundTimer() {
        return null;
    }

    public BatteryStats.Counter getBluetoothScanResultCounter() {
        return null;
    }

    public BatteryStats.Counter getBluetoothScanResultBgCounter() {
        return null;
    }

    public long getWifiRadioApWakeupCount(int i) {
        return 0L;
    }

    public void getDeferredJobsCheckinLineLocked(StringBuilder sb, int i) {
    }

    public void getDeferredJobsLineLocked(StringBuilder sb, int i) {
    }

    public long getMobileRadioApWakeupCount(int i) {
        return 0L;
    }

    public long[] getCpuFreqTimes(int i) {
        return null;
    }

    public long[] getScreenOffCpuFreqTimes(int i) {
        return null;
    }

    public long getCpuActiveTime() {
        return 0L;
    }

    public long[] getCpuClusterTimes() {
        return null;
    }

    public long[] getCpuFreqTimes(int i, int i2) {
        return null;
    }

    public long[] getScreenOffCpuFreqTimes(int i, int i2) {
        return null;
    }
}
