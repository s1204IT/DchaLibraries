package com.android.server.am;

import android.os.BatteryStats;
import android.os.SystemClock;
import android.os.health.HealthStatsWriter;
import android.os.health.PackageHealthStats;
import android.os.health.PidHealthStats;
import android.os.health.ProcessHealthStats;
import android.os.health.ServiceHealthStats;
import android.os.health.TimerStat;
import android.util.SparseArray;
import java.util.Map;

public class HealthStatsBatteryStatsWriter {
    private final long mNowRealtimeMs = SystemClock.elapsedRealtime();
    private final long mNowUptimeMs = SystemClock.uptimeMillis();

    public void writeUid(HealthStatsWriter healthStatsWriter, BatteryStats batteryStats, BatteryStats.Uid uid) {
        healthStatsWriter.addMeasurement(10001, batteryStats.computeBatteryRealtime(this.mNowRealtimeMs * 1000, 2) / 1000);
        healthStatsWriter.addMeasurement(10002, batteryStats.computeBatteryUptime(this.mNowUptimeMs * 1000, 2) / 1000);
        healthStatsWriter.addMeasurement(10003, batteryStats.computeBatteryScreenOffRealtime(this.mNowRealtimeMs * 1000, 2) / 1000);
        healthStatsWriter.addMeasurement(10004, batteryStats.computeBatteryScreenOffUptime(this.mNowUptimeMs * 1000, 2) / 1000);
        for (Map.Entry entry : uid.getWakelockStats().entrySet()) {
            String str = (String) entry.getKey();
            BatteryStats.Uid.Wakelock wakelock = (BatteryStats.Uid.Wakelock) entry.getValue();
            addTimers(healthStatsWriter, 10005, str, wakelock.getWakeTime(1));
            addTimers(healthStatsWriter, 10006, str, wakelock.getWakeTime(0));
            addTimers(healthStatsWriter, 10007, str, wakelock.getWakeTime(2));
            addTimers(healthStatsWriter, 10008, str, wakelock.getWakeTime(18));
        }
        for (Map.Entry entry2 : uid.getSyncStats().entrySet()) {
            addTimers(healthStatsWriter, 10009, (String) entry2.getKey(), (BatteryStats.Timer) entry2.getValue());
        }
        for (Map.Entry entry3 : uid.getJobStats().entrySet()) {
            addTimers(healthStatsWriter, 10010, (String) entry3.getKey(), (BatteryStats.Timer) entry3.getValue());
        }
        SparseArray sensorStats = uid.getSensorStats();
        int size = sensorStats.size();
        for (int i = 0; i < size; i++) {
            int iKeyAt = sensorStats.keyAt(i);
            if (iKeyAt == -10000) {
                addTimer(healthStatsWriter, 10011, ((BatteryStats.Uid.Sensor) sensorStats.valueAt(i)).getSensorTime());
            } else {
                addTimers(healthStatsWriter, 10012, Integer.toString(iKeyAt), ((BatteryStats.Uid.Sensor) sensorStats.valueAt(i)).getSensorTime());
            }
        }
        SparseArray pidStats = uid.getPidStats();
        int size2 = pidStats.size();
        for (int i2 = 0; i2 < size2; i2++) {
            HealthStatsWriter healthStatsWriter2 = new HealthStatsWriter(PidHealthStats.CONSTANTS);
            writePid(healthStatsWriter2, (BatteryStats.Uid.Pid) pidStats.valueAt(i2));
            healthStatsWriter.addStats(10013, Integer.toString(pidStats.keyAt(i2)), healthStatsWriter2);
        }
        for (Map.Entry entry4 : uid.getProcessStats().entrySet()) {
            HealthStatsWriter healthStatsWriter3 = new HealthStatsWriter(ProcessHealthStats.CONSTANTS);
            writeProc(healthStatsWriter3, (BatteryStats.Uid.Proc) entry4.getValue());
            healthStatsWriter.addStats(10014, (String) entry4.getKey(), healthStatsWriter3);
        }
        for (Map.Entry entry5 : uid.getPackageStats().entrySet()) {
            HealthStatsWriter healthStatsWriter4 = new HealthStatsWriter(PackageHealthStats.CONSTANTS);
            writePkg(healthStatsWriter4, (BatteryStats.Uid.Pkg) entry5.getValue());
            healthStatsWriter.addStats(10015, (String) entry5.getKey(), healthStatsWriter4);
        }
        BatteryStats.ControllerActivityCounter wifiControllerActivity = uid.getWifiControllerActivity();
        if (wifiControllerActivity != null) {
            healthStatsWriter.addMeasurement(10016, wifiControllerActivity.getIdleTimeCounter().getCountLocked(2));
            healthStatsWriter.addMeasurement(10017, wifiControllerActivity.getRxTimeCounter().getCountLocked(2));
            long countLocked = 0;
            for (BatteryStats.LongCounter longCounter : wifiControllerActivity.getTxTimeCounters()) {
                countLocked += longCounter.getCountLocked(2);
            }
            healthStatsWriter.addMeasurement(10018, countLocked);
            healthStatsWriter.addMeasurement(10019, wifiControllerActivity.getPowerCounter().getCountLocked(2));
        }
        BatteryStats.ControllerActivityCounter bluetoothControllerActivity = uid.getBluetoothControllerActivity();
        if (bluetoothControllerActivity != null) {
            healthStatsWriter.addMeasurement(10020, bluetoothControllerActivity.getIdleTimeCounter().getCountLocked(2));
            healthStatsWriter.addMeasurement(10021, bluetoothControllerActivity.getRxTimeCounter().getCountLocked(2));
            long countLocked2 = 0;
            for (BatteryStats.LongCounter longCounter2 : bluetoothControllerActivity.getTxTimeCounters()) {
                countLocked2 += longCounter2.getCountLocked(2);
            }
            healthStatsWriter.addMeasurement(10022, countLocked2);
            healthStatsWriter.addMeasurement(10023, bluetoothControllerActivity.getPowerCounter().getCountLocked(2));
        }
        BatteryStats.ControllerActivityCounter modemControllerActivity = uid.getModemControllerActivity();
        if (modemControllerActivity != null) {
            healthStatsWriter.addMeasurement(10024, modemControllerActivity.getIdleTimeCounter().getCountLocked(2));
            healthStatsWriter.addMeasurement(10025, modemControllerActivity.getRxTimeCounter().getCountLocked(2));
            long countLocked3 = 0;
            for (BatteryStats.LongCounter longCounter3 : modemControllerActivity.getTxTimeCounters()) {
                countLocked3 += longCounter3.getCountLocked(2);
            }
            healthStatsWriter.addMeasurement(10026, countLocked3);
            healthStatsWriter.addMeasurement(10027, modemControllerActivity.getPowerCounter().getCountLocked(2));
        }
        healthStatsWriter.addMeasurement(10028, uid.getWifiRunningTime(this.mNowRealtimeMs * 1000, 2) / 1000);
        healthStatsWriter.addMeasurement(10029, uid.getFullWifiLockTime(this.mNowRealtimeMs * 1000, 2) / 1000);
        healthStatsWriter.addTimer(10030, uid.getWifiScanCount(2), uid.getWifiScanTime(this.mNowRealtimeMs * 1000, 2) / 1000);
        healthStatsWriter.addMeasurement(10031, uid.getWifiMulticastTime(this.mNowRealtimeMs * 1000, 2) / 1000);
        addTimer(healthStatsWriter, 10032, uid.getAudioTurnedOnTimer());
        addTimer(healthStatsWriter, 10033, uid.getVideoTurnedOnTimer());
        addTimer(healthStatsWriter, 10034, uid.getFlashlightTurnedOnTimer());
        addTimer(healthStatsWriter, 10035, uid.getCameraTurnedOnTimer());
        addTimer(healthStatsWriter, 10036, uid.getForegroundActivityTimer());
        addTimer(healthStatsWriter, 10037, uid.getBluetoothScanTimer());
        addTimer(healthStatsWriter, 10038, uid.getProcessStateTimer(0));
        addTimer(healthStatsWriter, 10039, uid.getProcessStateTimer(1));
        addTimer(healthStatsWriter, 10040, uid.getProcessStateTimer(4));
        addTimer(healthStatsWriter, 10041, uid.getProcessStateTimer(2));
        addTimer(healthStatsWriter, 10042, uid.getProcessStateTimer(3));
        addTimer(healthStatsWriter, 10043, uid.getProcessStateTimer(6));
        addTimer(healthStatsWriter, 10044, uid.getVibratorOnTimer());
        healthStatsWriter.addMeasurement(10045, uid.getUserActivityCount(0, 2));
        healthStatsWriter.addMeasurement(10046, uid.getUserActivityCount(1, 2));
        healthStatsWriter.addMeasurement(10047, uid.getUserActivityCount(2, 2));
        healthStatsWriter.addMeasurement(10048, uid.getNetworkActivityBytes(0, 2));
        healthStatsWriter.addMeasurement(10049, uid.getNetworkActivityBytes(1, 2));
        healthStatsWriter.addMeasurement(10050, uid.getNetworkActivityBytes(2, 2));
        healthStatsWriter.addMeasurement(10051, uid.getNetworkActivityBytes(3, 2));
        healthStatsWriter.addMeasurement(10052, uid.getNetworkActivityBytes(4, 2));
        healthStatsWriter.addMeasurement(10053, uid.getNetworkActivityBytes(5, 2));
        healthStatsWriter.addMeasurement(10054, uid.getNetworkActivityPackets(0, 2));
        healthStatsWriter.addMeasurement(10055, uid.getNetworkActivityPackets(1, 2));
        healthStatsWriter.addMeasurement(10056, uid.getNetworkActivityPackets(2, 2));
        healthStatsWriter.addMeasurement(10057, uid.getNetworkActivityPackets(3, 2));
        healthStatsWriter.addMeasurement(10058, uid.getNetworkActivityPackets(4, 2));
        healthStatsWriter.addMeasurement(10059, uid.getNetworkActivityPackets(5, 2));
        healthStatsWriter.addTimer(10061, uid.getMobileRadioActiveCount(2), uid.getMobileRadioActiveTime(2));
        healthStatsWriter.addMeasurement(10062, uid.getUserCpuTimeUs(2) / 1000);
        healthStatsWriter.addMeasurement(10063, uid.getSystemCpuTimeUs(2) / 1000);
        healthStatsWriter.addMeasurement(10064, 0L);
    }

    public void writePid(HealthStatsWriter healthStatsWriter, BatteryStats.Uid.Pid pid) {
        if (pid == null) {
            return;
        }
        healthStatsWriter.addMeasurement(20001, pid.mWakeNesting);
        healthStatsWriter.addMeasurement(20002, pid.mWakeSumMs);
        healthStatsWriter.addMeasurement(20002, pid.mWakeStartMs);
    }

    public void writeProc(HealthStatsWriter healthStatsWriter, BatteryStats.Uid.Proc proc) {
        healthStatsWriter.addMeasurement(EventLogTags.AM_FINISH_ACTIVITY, proc.getUserTime(2));
        healthStatsWriter.addMeasurement(EventLogTags.AM_TASK_TO_FRONT, proc.getSystemTime(2));
        healthStatsWriter.addMeasurement(EventLogTags.AM_NEW_INTENT, proc.getStarts(2));
        healthStatsWriter.addMeasurement(EventLogTags.AM_CREATE_TASK, proc.getNumCrashes(2));
        healthStatsWriter.addMeasurement(EventLogTags.AM_CREATE_ACTIVITY, proc.getNumAnrs(2));
        healthStatsWriter.addMeasurement(EventLogTags.AM_RESTART_ACTIVITY, proc.getForegroundTime(2));
    }

    public void writePkg(HealthStatsWriter healthStatsWriter, BatteryStats.Uid.Pkg pkg) {
        for (Map.Entry entry : pkg.getServiceStats().entrySet()) {
            HealthStatsWriter healthStatsWriter2 = new HealthStatsWriter(ServiceHealthStats.CONSTANTS);
            writeServ(healthStatsWriter2, (BatteryStats.Uid.Pkg.Serv) entry.getValue());
            healthStatsWriter.addStats(com.android.server.EventLogTags.STREAM_DEVICES_CHANGED, (String) entry.getKey(), healthStatsWriter2);
        }
        for (Map.Entry entry2 : pkg.getWakeupAlarmStats().entrySet()) {
            if (((BatteryStats.Counter) entry2.getValue()) != null) {
                healthStatsWriter.addMeasurements(40002, (String) entry2.getKey(), r1.getCountLocked(2));
            }
        }
    }

    public void writeServ(HealthStatsWriter healthStatsWriter, BatteryStats.Uid.Pkg.Serv serv) {
        healthStatsWriter.addMeasurement(50001, serv.getStarts(2));
        healthStatsWriter.addMeasurement(50002, serv.getLaunches(2));
    }

    private void addTimer(HealthStatsWriter healthStatsWriter, int i, BatteryStats.Timer timer) {
        if (timer != null) {
            healthStatsWriter.addTimer(i, timer.getCountLocked(2), timer.getTotalTimeLocked(this.mNowRealtimeMs * 1000, 2) / 1000);
        }
    }

    private void addTimers(HealthStatsWriter healthStatsWriter, int i, String str, BatteryStats.Timer timer) {
        if (timer != null) {
            healthStatsWriter.addTimers(i, str, new TimerStat(timer.getCountLocked(2), timer.getTotalTimeLocked(this.mNowRealtimeMs * 1000, 2) / 1000));
        }
    }
}
