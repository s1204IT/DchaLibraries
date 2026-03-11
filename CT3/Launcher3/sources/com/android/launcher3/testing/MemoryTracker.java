package com.android.launcher3.testing;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.LongSparseArray;
import com.android.launcher3.compat.PackageInstallerCompat;
import java.util.ArrayList;
import java.util.List;

public class MemoryTracker extends Service {
    public static final String TAG = MemoryTracker.class.getSimpleName();
    ActivityManager mAm;
    public final LongSparseArray<ProcessMemInfo> mData = new LongSparseArray<>();
    public final ArrayList<Long> mPids = new ArrayList<>();
    private int[] mPidsArray = new int[0];
    private final Object mLock = new Object();
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message m) {
            switch (m.what) {
                case PackageInstallerCompat.STATUS_INSTALLING:
                    MemoryTracker.this.mHandler.removeMessages(3);
                    MemoryTracker.this.mHandler.sendEmptyMessage(3);
                    break;
                case PackageInstallerCompat.STATUS_FAILED:
                    MemoryTracker.this.mHandler.removeMessages(3);
                    break;
                case 3:
                    MemoryTracker.this.update();
                    MemoryTracker.this.mHandler.removeMessages(3);
                    MemoryTracker.this.mHandler.sendEmptyMessageDelayed(3, 5000L);
                    break;
            }
        }
    };
    private final IBinder mBinder = new MemoryTrackerInterface();

    public static class ProcessMemInfo {
        public long currentPss;
        public long currentUss;
        public String name;
        public int pid;
        public long startTime;
        public long[] pss = new long[256];
        public long[] uss = new long[256];
        public long max = 1;
        public int head = 0;

        public ProcessMemInfo(int pid, String name, long start) {
            this.pid = pid;
            this.name = name;
            this.startTime = start;
        }

        public long getUptime() {
            return System.currentTimeMillis() - this.startTime;
        }
    }

    public ProcessMemInfo getMemInfo(int pid) {
        return this.mData.get(pid);
    }

    public int[] getTrackedProcesses() {
        return this.mPidsArray;
    }

    public void startTrackingProcess(int pid, String name, long start) {
        synchronized (this.mLock) {
            Long lpid = Long.valueOf(pid);
            if (this.mPids.contains(lpid)) {
                return;
            }
            this.mPids.add(lpid);
            updatePidsArrayL();
            this.mData.put(pid, new ProcessMemInfo(pid, name, start));
        }
    }

    void updatePidsArrayL() {
        int N = this.mPids.size();
        this.mPidsArray = new int[N];
        StringBuffer sb = new StringBuffer("Now tracking processes: ");
        for (int i = 0; i < N; i++) {
            int p = this.mPids.get(i).intValue();
            this.mPidsArray[i] = p;
            sb.append(p);
            sb.append(" ");
        }
        Log.v(TAG, sb.toString());
    }

    void update() {
        synchronized (this.mLock) {
            Debug.MemoryInfo[] dinfos = this.mAm.getProcessMemoryInfo(this.mPidsArray);
            int i = 0;
            while (true) {
                if (i >= dinfos.length) {
                    break;
                }
                Debug.MemoryInfo dinfo = dinfos[i];
                if (i > this.mPids.size()) {
                    Log.e(TAG, "update: unknown process info received: " + dinfo);
                    break;
                }
                long pid = this.mPids.get(i).intValue();
                ProcessMemInfo info = this.mData.get(pid);
                info.head = (info.head + 1) % info.pss.length;
                long[] jArr = info.pss;
                int i2 = info.head;
                int totalPss = dinfo.getTotalPss();
                info.currentPss = totalPss;
                jArr[i2] = totalPss;
                long[] jArr2 = info.uss;
                int i3 = info.head;
                int totalPrivateDirty = dinfo.getTotalPrivateDirty();
                info.currentUss = totalPrivateDirty;
                jArr2[i3] = totalPrivateDirty;
                if (info.currentPss > info.max) {
                    info.max = info.currentPss;
                }
                if (info.currentUss > info.max) {
                    info.max = info.currentUss;
                }
                if (info.currentPss == 0) {
                    Log.v(TAG, "update: pid " + pid + " has pss=0, it probably died");
                    this.mData.remove(pid);
                }
                i++;
            }
            for (int i4 = this.mPids.size() - 1; i4 >= 0; i4--) {
                if (this.mData.get(this.mPids.get(i4).intValue()) == null) {
                    this.mPids.remove(i4);
                    updatePidsArrayL();
                }
            }
        }
    }

    @Override
    public void onCreate() {
        this.mAm = (ActivityManager) getSystemService("activity");
        List<ActivityManager.RunningServiceInfo> svcs = this.mAm.getRunningServices(256);
        for (ActivityManager.RunningServiceInfo svc : svcs) {
            if (svc.service.getPackageName().equals(getPackageName())) {
                Log.v(TAG, "discovered running service: " + svc.process + " (" + svc.pid + ")");
                startTrackingProcess(svc.pid, svc.process, System.currentTimeMillis() - (SystemClock.elapsedRealtime() - svc.activeSince));
            }
        }
        List<ActivityManager.RunningAppProcessInfo> procs = this.mAm.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo proc : procs) {
            String pname = proc.processName;
            if (pname.startsWith(getPackageName())) {
                Log.v(TAG, "discovered other running process: " + pname + " (" + proc.pid + ")");
                startTrackingProcess(proc.pid, pname, System.currentTimeMillis());
            }
        }
    }

    @Override
    public void onDestroy() {
        this.mHandler.sendEmptyMessage(2);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Received start id " + startId + ": " + intent);
        if (intent != null && "com.android.launcher3.action.START_TRACKING".equals(intent.getAction())) {
            int pid = intent.getIntExtra("pid", -1);
            String name = intent.getStringExtra("name");
            long start = intent.getLongExtra("start", System.currentTimeMillis());
            startTrackingProcess(pid, name, start);
        }
        this.mHandler.sendEmptyMessage(1);
        return 1;
    }

    public class MemoryTrackerInterface extends Binder {
        public MemoryTrackerInterface() {
        }

        MemoryTracker getService() {
            return MemoryTracker.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.mHandler.sendEmptyMessage(1);
        return this.mBinder;
    }
}
