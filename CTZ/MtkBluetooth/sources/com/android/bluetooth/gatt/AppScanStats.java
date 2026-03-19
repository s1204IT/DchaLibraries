package com.android.bluetooth.gatt;

import android.bluetooth.le.ScanSettings;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.WorkSource;
import android.util.StatsLog;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.gatt.ContextMap;
import com.android.internal.app.IBatteryStats;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

class AppScanStats {
    static final long EXCESSIVE_SCANNING_PERIOD_MS = 30000;
    static final int NUM_SCAN_DURATIONS_KEPT = 5;
    static final int SCAN_TIMEOUT_MS = 1800000;
    public String appName;
    ContextMap mContextMap;
    GattService mGattService;
    public WorkSource mWorkSource;
    private static final String TAG = AppScanStats.class.getSimpleName();
    static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss");
    private int mScansStarted = 0;
    private int mScansStopped = 0;
    public boolean isRegistered = false;
    private long mMinScanTime = Long.MAX_VALUE;
    private long mMaxScanTime = 0;
    private long mScanStartTime = 0;
    private long mTotalScanTime = 0;
    private long mTotalSuspendTime = 0;
    private List<LastScan> mLastScans = new ArrayList(5);
    private HashMap<Integer, LastScan> mOngoingScans = new HashMap<>();
    public long startTime = 0;
    public long stopTime = 0;
    public int results = 0;
    IBatteryStats mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));

    class LastScan {
        public boolean background;
        public long duration;
        public boolean filtered;
        public boolean opportunistic;
        public int scannerId;
        public boolean timeout;
        public long timestamp;
        public int results = 0;
        public long suspendDuration = 0;
        public long suspendStartTime = 0;
        public boolean isSuspended = false;

        LastScan(long j, long j2, boolean z, boolean z2, boolean z3, int i) {
            this.duration = j2;
            this.timestamp = j;
            this.opportunistic = z;
            this.background = z2;
            this.filtered = z3;
            this.scannerId = i;
        }
    }

    AppScanStats(String str, WorkSource workSource, ContextMap contextMap, GattService gattService) {
        this.appName = str;
        this.mContextMap = contextMap;
        this.mGattService = gattService;
        this.mWorkSource = workSource == null ? new WorkSource(Binder.getCallingUid(), this.appName) : workSource;
    }

    synchronized void addResult(int i) {
        LastScan scanFromScannerId = getScanFromScannerId(i);
        if (scanFromScannerId != null) {
            int i2 = scanFromScannerId.results + 1;
            scanFromScannerId.results = i2;
            if (i2 % 100 == 0) {
                try {
                    this.mBatteryStats.noteBleScanResults(this.mWorkSource, 100);
                } catch (RemoteException e) {
                }
            }
        }
        this.results++;
    }

    boolean isScanning() {
        return !this.mOngoingScans.isEmpty();
    }

    LastScan getScanFromScannerId(int i) {
        return this.mOngoingScans.get(Integer.valueOf(i));
    }

    synchronized void recordScanStart(ScanSettings scanSettings, boolean z, int i) {
        if (getScanFromScannerId(i) != null) {
            return;
        }
        this.mScansStarted++;
        this.startTime = SystemClock.elapsedRealtime();
        LastScan lastScan = new LastScan(this.startTime, 0L, false, false, z, i);
        boolean z2 = false;
        if (scanSettings != null) {
            lastScan.opportunistic = scanSettings.getScanMode() == -1;
            lastScan.background = (scanSettings.getCallbackType() & 2) != 0;
        }
        this.mGattService.addScanEvent(BluetoothMetricsProto.ScanEvent.newBuilder().setScanEventType(BluetoothMetricsProto.ScanEvent.ScanEventType.SCAN_EVENT_START).setScanTechnologyType(BluetoothMetricsProto.ScanEvent.ScanTechnologyType.SCAN_TECH_TYPE_LE).setEventTimeMillis(System.currentTimeMillis()).setInitiator(truncateAppName(this.appName)).build());
        if (!isScanning()) {
            this.mScanStartTime = this.startTime;
        }
        try {
            if (!lastScan.filtered && !lastScan.background && !lastScan.opportunistic) {
                z2 = true;
            }
            this.mBatteryStats.noteBleScanStarted(this.mWorkSource, z2);
        } catch (RemoteException e) {
        }
        writeToStatsLog(lastScan, 1);
        this.mOngoingScans.put(Integer.valueOf(i), lastScan);
    }

    synchronized void recordScanStop(int i) {
        LastScan scanFromScannerId = getScanFromScannerId(i);
        if (scanFromScannerId == null) {
            return;
        }
        boolean z = true;
        this.mScansStopped++;
        this.stopTime = SystemClock.elapsedRealtime();
        scanFromScannerId.duration = this.stopTime - scanFromScannerId.timestamp;
        if (scanFromScannerId.isSuspended) {
            long j = this.stopTime - scanFromScannerId.suspendStartTime;
            scanFromScannerId.suspendDuration += j;
            this.mTotalSuspendTime += j;
        }
        this.mOngoingScans.remove(Integer.valueOf(i));
        if (this.mLastScans.size() >= 5) {
            this.mLastScans.remove(0);
        }
        this.mLastScans.add(scanFromScannerId);
        this.mGattService.addScanEvent(BluetoothMetricsProto.ScanEvent.newBuilder().setScanEventType(BluetoothMetricsProto.ScanEvent.ScanEventType.SCAN_EVENT_STOP).setScanTechnologyType(BluetoothMetricsProto.ScanEvent.ScanTechnologyType.SCAN_TECH_TYPE_LE).setEventTimeMillis(System.currentTimeMillis()).setInitiator(truncateAppName(this.appName)).setNumberResults(scanFromScannerId.results).build());
        if (!isScanning()) {
            long j2 = this.stopTime - this.mScanStartTime;
            this.mTotalScanTime += j2;
            this.mMinScanTime = Math.min(j2, this.mMinScanTime);
            this.mMaxScanTime = Math.max(j2, this.mMaxScanTime);
        }
        try {
            if (scanFromScannerId.filtered || scanFromScannerId.background || scanFromScannerId.opportunistic) {
                z = false;
            }
            this.mBatteryStats.noteBleScanResults(this.mWorkSource, scanFromScannerId.results % 100);
            this.mBatteryStats.noteBleScanStopped(this.mWorkSource, z);
        } catch (RemoteException e) {
        }
        writeToStatsLog(scanFromScannerId, 0);
    }

    synchronized void recordScanSuspend(int i) {
        LastScan scanFromScannerId = getScanFromScannerId(i);
        if (scanFromScannerId != null && !scanFromScannerId.isSuspended) {
            scanFromScannerId.suspendStartTime = SystemClock.elapsedRealtime();
            scanFromScannerId.isSuspended = true;
        }
    }

    synchronized void recordScanResume(int i) {
        LastScan scanFromScannerId = getScanFromScannerId(i);
        if (scanFromScannerId != null && scanFromScannerId.isSuspended) {
            scanFromScannerId.isSuspended = false;
            this.stopTime = SystemClock.elapsedRealtime();
            long j = this.stopTime - scanFromScannerId.suspendStartTime;
            scanFromScannerId.suspendDuration += j;
            this.mTotalSuspendTime += j;
        }
    }

    private void writeToStatsLog(LastScan lastScan, int i) {
        for (int i2 = 0; i2 < this.mWorkSource.size(); i2++) {
            StatsLog.write_non_chained(2, this.mWorkSource.get(i2), null, i, lastScan.filtered, lastScan.background, lastScan.opportunistic);
        }
        ArrayList workChains = this.mWorkSource.getWorkChains();
        if (workChains != null) {
            for (int i3 = 0; i3 < workChains.size(); i3++) {
                WorkSource.WorkChain workChain = (WorkSource.WorkChain) workChains.get(i3);
                StatsLog.write(2, workChain.getUids(), workChain.getTags(), i, lastScan.filtered, lastScan.background, lastScan.opportunistic);
            }
        }
    }

    synchronized void setScanTimeout(int i) {
        if (isScanning()) {
            LastScan scanFromScannerId = getScanFromScannerId(i);
            if (scanFromScannerId != null) {
                scanFromScannerId.timeout = true;
            }
        }
    }

    synchronized boolean isScanningTooFrequently() {
        if (this.mLastScans.size() < 5) {
            return false;
        }
        return SystemClock.elapsedRealtime() - this.mLastScans.get(0).timestamp < EXCESSIVE_SCANNING_PERIOD_MS;
    }

    synchronized boolean isScanningTooLong() {
        if (isScanning()) {
            return SystemClock.elapsedRealtime() - this.mScanStartTime > 1800000;
        }
        return false;
    }

    private String truncateAppName(String str) {
        String[] strArrSplit = str.split("\\.");
        if (strArrSplit.length > 3) {
            return strArrSplit[0] + "." + strArrSplit[1] + "." + strArrSplit[2];
        }
        if (strArrSplit.length == 3) {
            return strArrSplit[0] + "." + strArrSplit[1];
        }
        return str;
    }

    synchronized void dumpToString(StringBuilder sb) {
        long j;
        long j2;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        long j3 = this.mMaxScanTime;
        long j4 = this.mMinScanTime;
        if (isScanning()) {
            j = jElapsedRealtime - this.mScanStartTime;
        } else {
            j = 0;
        }
        long jMin = Math.min(j, j4);
        long jMax = Math.max(j, j3);
        if (jMin == Long.MAX_VALUE) {
            jMin = 0;
        }
        long j5 = this.mTotalScanTime + j;
        if (this.mScansStarted > 0) {
            j2 = j5 / ((long) this.mScansStarted);
        } else {
            j2 = 0;
        }
        sb.append("  " + this.appName);
        if (this.isRegistered) {
            sb.append(" (Registered)");
        }
        if (!this.mLastScans.isEmpty()) {
            LastScan lastScan = this.mLastScans.get(this.mLastScans.size() - 1);
            if (lastScan.opportunistic) {
                sb.append(" (Opportunistic)");
            }
            if (lastScan.background) {
                sb.append(" (Background)");
            }
            if (lastScan.timeout) {
                sb.append(" (Forced-Opportunistic)");
            }
            if (lastScan.filtered) {
                sb.append(" (Filtered)");
            }
        }
        sb.append("\n");
        sb.append("  LE scans (started/stopped)         : " + this.mScansStarted + " / " + this.mScansStopped + "\n");
        sb.append("  Scan time in ms (min/max/avg/total): " + jMin + " / " + jMax + " / " + j2 + " / " + j5 + "\n");
        if (this.mTotalSuspendTime != 0) {
            sb.append("  Total time suspended               : " + this.mTotalSuspendTime + "ms\n");
        }
        sb.append("  Total number of results            : " + this.results + "\n");
        long jCurrentTimeMillis = System.currentTimeMillis();
        long jElapsedRealtime2 = SystemClock.elapsedRealtime();
        if (!this.mLastScans.isEmpty()) {
            sb.append("  Last " + this.mLastScans.size() + " scans                       :\n");
            for (int i = 0; i < this.mLastScans.size(); i++) {
                LastScan lastScan2 = this.mLastScans.get(i);
                sb.append("    " + DATE_FORMAT.format(new Date((jCurrentTimeMillis - jElapsedRealtime2) + lastScan2.timestamp)) + " - ");
                StringBuilder sb2 = new StringBuilder();
                sb2.append(lastScan2.duration);
                sb2.append("ms ");
                sb.append(sb2.toString());
                if (lastScan2.opportunistic) {
                    sb.append("Opp ");
                }
                if (lastScan2.background) {
                    sb.append("Back ");
                }
                if (lastScan2.timeout) {
                    sb.append("Forced ");
                }
                if (lastScan2.filtered) {
                    sb.append("Filter ");
                }
                sb.append(lastScan2.results + " results");
                sb.append(" (" + lastScan2.scannerId + ")");
                sb.append("\n");
                if (lastScan2.suspendDuration != 0) {
                    sb.append("      └ Suspended Time: " + lastScan2.suspendDuration + "ms\n");
                }
            }
        }
        if (!this.mOngoingScans.isEmpty()) {
            sb.append("  Ongoing scans                      :\n");
            Iterator<Integer> it = this.mOngoingScans.keySet().iterator();
            while (it.hasNext()) {
                LastScan lastScan3 = this.mOngoingScans.get(it.next());
                sb.append("    " + DATE_FORMAT.format(new Date((jCurrentTimeMillis - jElapsedRealtime2) + lastScan3.timestamp)) + " - ");
                StringBuilder sb3 = new StringBuilder();
                sb3.append(jElapsedRealtime2 - lastScan3.timestamp);
                sb3.append("ms ");
                sb.append(sb3.toString());
                if (lastScan3.opportunistic) {
                    sb.append("Opp ");
                }
                if (lastScan3.background) {
                    sb.append("Back ");
                }
                if (lastScan3.timeout) {
                    sb.append("Forced ");
                }
                if (lastScan3.filtered) {
                    sb.append("Filter ");
                }
                if (lastScan3.isSuspended) {
                    sb.append("Suspended ");
                }
                sb.append(lastScan3.results + " results");
                sb.append(" (" + lastScan3.scannerId + ")");
                sb.append("\n");
                if (lastScan3.suspendStartTime != 0) {
                    sb.append("      └ Suspended Time: " + (lastScan3.suspendDuration + (lastScan3.isSuspended ? jElapsedRealtime2 - lastScan3.suspendStartTime : 0L)) + "ms\n");
                }
            }
        }
        ContextMap.App byName = this.mContextMap.getByName(this.appName);
        if (byName != null && this.isRegistered) {
            sb.append("  Application ID                     : " + byName.id + "\n");
            sb.append("  UUID                               : " + byName.uuid + "\n");
            List<ContextMap.Connection> connectionByApp = this.mContextMap.getConnectionByApp(byName.id);
            sb.append("  Connections: " + connectionByApp.size() + "\n");
            for (ContextMap.Connection connection : connectionByApp) {
                long j6 = jElapsedRealtime2 - connection.startTime;
                sb.append("    " + DATE_FORMAT.format(new Date((jCurrentTimeMillis - jElapsedRealtime2) + connection.startTime)) + " - ");
                StringBuilder sb4 = new StringBuilder();
                sb4.append(j6);
                sb4.append("ms ");
                sb.append(sb4.toString());
                sb.append(": " + connection.address + " (" + connection.connId + ")\n");
            }
        }
        sb.append("\n");
    }
}
