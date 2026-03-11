package com.android.settings.fuelgauge;

import android.os.BatteryStats;
import android.util.ArrayMap;
import android.util.SparseArray;

public class FakeUid extends BatteryStats.Uid {
    private final int mUid;

    public FakeUid(int uid) {
        this.mUid = uid;
    }

    public int getUid() {
        return this.mUid;
    }

    public ArrayMap<String, ? extends BatteryStats.Uid.Wakelock> getWakelockStats() {
        return null;
    }

    public ArrayMap<String, ? extends BatteryStats.Timer> getSyncStats() {
        return null;
    }

    public ArrayMap<String, ? extends BatteryStats.Timer> getJobStats() {
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

    public void noteWifiRunningLocked(long elapsedRealtime) {
    }

    public void noteWifiStoppedLocked(long elapsedRealtime) {
    }

    public void noteFullWifiLockAcquiredLocked(long elapsedRealtime) {
    }

    public void noteFullWifiLockReleasedLocked(long elapsedRealtime) {
    }

    public void noteWifiScanStartedLocked(long elapsedRealtime) {
    }

    public void noteWifiScanStoppedLocked(long elapsedRealtime) {
    }

    public void noteWifiBatchedScanStartedLocked(int csph, long elapsedRealtime) {
    }

    public void noteWifiBatchedScanStoppedLocked(long elapsedRealtime) {
    }

    public void noteWifiMulticastEnabledLocked(long elapsedRealtime) {
    }

    public void noteWifiMulticastDisabledLocked(long elapsedRealtime) {
    }

    public void noteActivityResumedLocked(long elapsedRealtime) {
    }

    public void noteActivityPausedLocked(long elapsedRealtime) {
    }

    public long getWifiRunningTime(long elapsedRealtimeUs, int which) {
        return 0L;
    }

    public long getFullWifiLockTime(long elapsedRealtimeUs, int which) {
        return 0L;
    }

    public long getWifiScanTime(long elapsedRealtimeUs, int which) {
        return 0L;
    }

    public int getWifiScanCount(int which) {
        return 0;
    }

    public long getWifiBatchedScanTime(int csphBin, long elapsedRealtimeUs, int which) {
        return 0L;
    }

    public int getWifiBatchedScanCount(int csphBin, int which) {
        return 0;
    }

    public long getWifiMulticastTime(long elapsedRealtimeUs, int which) {
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

    public long getProcessStateTime(int state, long elapsedRealtimeUs, int which) {
        return 0L;
    }

    public BatteryStats.Timer getProcessStateTimer(int state) {
        return null;
    }

    public BatteryStats.Timer getVibratorOnTimer() {
        return null;
    }

    public void noteUserActivityLocked(int type) {
    }

    public boolean hasUserActivity() {
        return false;
    }

    public int getUserActivityCount(int type, int which) {
        return 0;
    }

    public boolean hasNetworkActivity() {
        return false;
    }

    public long getNetworkActivityBytes(int type, int which) {
        return 0L;
    }

    public long getNetworkActivityPackets(int type, int which) {
        return 0L;
    }

    public long getMobileRadioActiveTime(int which) {
        return 0L;
    }

    public int getMobileRadioActiveCount(int which) {
        return 0;
    }

    public long getUserCpuTimeUs(int which) {
        return 0L;
    }

    public long getSystemCpuTimeUs(int which) {
        return 0L;
    }

    public long getTimeAtCpuSpeed(int cluster, int step, int which) {
        return 0L;
    }

    public long getCpuPowerMaUs(int which) {
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
}
