package com.mediatek.server;

import android.app.AlarmManager;
import android.app.IAlarmListener;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.WifiDisplayStatus;
import android.os.Build;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.text.format.Time;
import android.util.Slog;
import com.android.server.AlarmManagerService;
import com.mediatek.amplus.AlarmManagerPlus;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;

public class MtkAlarmManagerService extends AlarmManagerService {
    static final String ClockReceiver_TAG = "ClockReceiver";
    static final long POWER_OFF_ALARM_BUFFER_TIME = 150000;
    private static int mAlarmMode = 2;
    private static boolean mSupportAlarmGrouping = false;
    private ArrayList<String> mAlarmIconPackageList;
    private AlarmManagerPlus mAmPlus;
    boolean mIsWFDConnected;
    private boolean mNeedGrouping;
    boolean mNeedRebatchForRepeatingAlarm;
    private boolean mPPLEnable;
    private ArrayList<PendingIntent> mPPLFreeList;
    private Object mPPLLock;
    private PPLReceiver mPPLReceiver;
    private ArrayList<AlarmManagerService.Alarm> mPPLResendList;
    private Object mPowerOffAlarmLock;
    private final ArrayList<AlarmManagerService.Alarm> mPoweroffAlarms;
    WFDStatusChangedReceiver mWFDStatusChangedReceiver;
    private Object mWaitThreadlock;

    public MtkAlarmManagerService(Context context) {
        super(context);
        this.mNeedRebatchForRepeatingAlarm = false;
        this.mNeedGrouping = true;
        this.mWaitThreadlock = new Object();
        this.mPowerOffAlarmLock = new Object();
        this.mPoweroffAlarms = new ArrayList<>();
        this.mPPLReceiver = null;
        this.mPPLEnable = true;
        this.mPPLLock = new Object();
        this.mPPLFreeList = null;
        this.mAlarmIconPackageList = null;
        this.mPPLResendList = null;
        this.mIsWFDConnected = false;
        if (Build.TYPE.equals("eng")) {
            localLOGV = true;
            DEBUG_WAKELOCK = true;
            DEBUG_BATCH = true;
            DEBUG_STANDBY = true;
            DEBUG_ALARM_CLOCK = true;
            DEBUG_VALIDATE = true;
        }
    }

    protected void registerWFDStatusChangeReciever() {
        this.mWFDStatusChangedReceiver = new WFDStatusChangedReceiver();
    }

    protected boolean isWFDConnected() {
        if (DEBUG_WAKELOCK) {
            Slog.v("AlarmManager", "checkAllowNonWakeupDelayLocked isWFDConnected :" + this.mIsWFDConnected);
        }
        return this.mIsWFDConnected;
    }

    protected boolean needAlarmGrouping() {
        return this.mNeedGrouping;
    }

    protected void resetneedRebatchForRepeatingAlarm() {
        this.mNeedRebatchForRepeatingAlarm = false;
    }

    protected boolean needRebatchForRepeatingAlarm() {
        return this.mNeedRebatchForRepeatingAlarm;
    }

    protected boolean supportAlarmGrouping() {
        return mSupportAlarmGrouping && this.mAmPlus != null;
    }

    protected void initAlarmGrouping() {
        if (SystemProperties.get("ro.vendor.mtk_bg_power_saving_support").equals("1")) {
            mSupportAlarmGrouping = true;
        }
        if (mSupportAlarmGrouping && this.mAmPlus == null) {
            try {
                this.mAmPlus = new AlarmManagerPlus(getContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected long getMaxTriggerTimeforAlarmGrouping(int i, long j, long j2, long j3, long j4, PendingIntent pendingIntent, AlarmManagerService.Alarm alarm) {
        long maxTriggerTime = this.mAmPlus.getMaxTriggerTime(i, j, j3, j4, pendingIntent, mAlarmMode, true);
        if (j2 < 0) {
            Slog.v("AlarmManager", "Past alarm, need to trigger immediatly (min_futurity)");
            if (alarm != null) {
                alarm.needGrouping = false;
            } else {
                this.mNeedGrouping = false;
            }
            return j;
        }
        if (maxTriggerTime >= 0) {
            if (alarm != null) {
                alarm.needGrouping = true;
                return maxTriggerTime;
            }
            this.mNeedGrouping = true;
            return maxTriggerTime;
        }
        long j5 = 0 - maxTriggerTime;
        if (alarm != null) {
            alarm.needGrouping = false;
            return j5;
        }
        this.mNeedGrouping = false;
        return j5;
    }

    protected boolean removeInvalidAlarmLocked(final PendingIntent pendingIntent, final IAlarmListener iAlarmListener) {
        Predicate predicate = new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((AlarmManagerService.Alarm) obj).matches(pendingIntent, iAlarmListener);
            }
        };
        boolean zRemove = false;
        for (int size = this.mAlarmBatches.size() - 1; size >= 0; size--) {
            AlarmManagerService.Batch batch = (AlarmManagerService.Batch) this.mAlarmBatches.get(size);
            zRemove |= batch.remove(predicate);
            if (batch.size() == 0) {
                this.mAlarmBatches.remove(size);
            }
        }
        if (zRemove) {
            this.mNeedRebatchForRepeatingAlarm = true;
        }
        return zRemove;
    }

    class WFDStatusChangedReceiver extends BroadcastReceiver {
        public WFDStatusChangedReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
            MtkAlarmManagerService.this.getContext().registerReceiver(this, intentFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED".equals(intent.getAction())) {
                WifiDisplayStatus parcelableExtra = intent.getParcelableExtra("android.hardware.display.extra.WIFI_DISPLAY_STATUS");
                MtkAlarmManagerService.this.mIsWFDConnected = 2 == parcelableExtra.getActiveDisplayState();
            }
        }
    }

    protected void initPpl() {
        this.mPPLReceiver = new PPLReceiver();
        this.mPPLFreeList = new ArrayList<>();
        this.mPPLFreeList.add(this.mTimeTickSender);
        this.mPPLFreeList.add(this.mDateChangeSender);
        this.mPPLResendList = new ArrayList<>();
    }

    protected boolean freePplCheck(ArrayList<AlarmManagerService.Alarm> arrayList, long j) {
        boolean z;
        synchronized (this.mPPLLock) {
            if (!this.mPPLEnable) {
                FreePPLIntent(arrayList, this.mPPLFreeList, j, this.mPPLResendList);
                z = true;
            } else {
                z = false;
            }
        }
        return z;
    }

    class PPLReceiver extends BroadcastReceiver {
        public PPLReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.mediatek.ppl.NOTIFY_LOCK");
            intentFilter.addAction("com.mediatek.ppl.NOTIFY_UNLOCK");
            MtkAlarmManagerService.this.getContext().registerReceiver(this, intentFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.mediatek.ppl.NOTIFY_LOCK")) {
                MtkAlarmManagerService.this.mPPLEnable = false;
            } else if (action.equals("com.mediatek.ppl.NOTIFY_UNLOCK")) {
                MtkAlarmManagerService.this.mPPLEnable = true;
                MtkAlarmManagerService.this.enablePPL();
            }
        }
    }

    public int enablePPL() {
        synchronized (this.mPPLLock) {
            if (this.mPPLEnable) {
                resendPPLPendingList(this.mPPLResendList);
                this.mPPLResendList = null;
                this.mPPLResendList = new ArrayList<>();
            }
        }
        return -1;
    }

    private void FreePPLIntent(ArrayList<AlarmManagerService.Alarm> arrayList, ArrayList<PendingIntent> arrayList2, long j, ArrayList<AlarmManagerService.Alarm> arrayList3) {
        AlarmManagerService.Alarm alarm;
        Iterator<AlarmManagerService.Alarm> it;
        boolean z;
        Iterator<AlarmManagerService.Alarm> it2 = arrayList.iterator();
        while (it2.hasNext()) {
            AlarmManagerService.Alarm next = it2.next();
            if (next.operation != null) {
                int i = 0;
                while (true) {
                    try {
                        if (i >= arrayList2.size()) {
                            alarm = next;
                            it = it2;
                            z = false;
                            break;
                        } else {
                            try {
                                if (next.operation.equals(arrayList2.get(i))) {
                                    break;
                                } else {
                                    i++;
                                }
                            } catch (PendingIntent.CanceledException e) {
                                alarm = next;
                                it = it2;
                            }
                        }
                    } catch (PendingIntent.CanceledException e2) {
                        alarm = next;
                        it = it2;
                    }
                }
                if (!z) {
                    try {
                        arrayList3.add(alarm);
                    } catch (PendingIntent.CanceledException e3) {
                        long j2 = alarm.repeatInterval;
                    }
                }
                it2 = it;
            } else if (DEBUG_WAKELOCK) {
                Slog.v("AlarmManager", "FreePPLIntent skip with null operation APP listener(" + next.listenerTag + ") : type = " + next.type + " triggerAtTime = " + next.when);
            }
        }
    }

    private void resendPPLPendingList(ArrayList<AlarmManagerService.Alarm> arrayList) {
        for (AlarmManagerService.Alarm alarm : arrayList) {
            if (alarm.operation == null) {
                if (localLOGV) {
                    Slog.v("AlarmManager", "resendPPLPendingList skip with null operation, APP listener(" + alarm.listenerTag + ") : type = " + alarm.type + " triggerAtTime = " + alarm.when);
                }
            } else {
                try {
                    if (DEBUG_WAKELOCK) {
                        Slog.v("AlarmManager", "sending alarm " + alarm);
                    }
                    alarm.operation.send(getContext(), 0, this.mBackgroundIntent.putExtra("android.intent.extra.ALARM_COUNT", alarm.count), this.mDeliveryTracker, this.mHandler);
                    if (this.mBroadcastRefCount == 0) {
                        setWakelockWorkSource(alarm.operation, alarm.workSource, alarm.type, alarm.statsTag, alarm.uid, true);
                        this.mWakeLock.acquire();
                    }
                    AlarmManagerService.InFlight inFlight = new AlarmManagerService.InFlight(this, alarm.operation, alarm.listener, alarm.workSource, alarm.uid, alarm.packageName, alarm.type, alarm.statsTag, 0L);
                    this.mInFlight.add(inFlight);
                    this.mBroadcastRefCount++;
                    AlarmManagerService.BroadcastStats broadcastStats = inFlight.mBroadcastStats;
                    broadcastStats.count++;
                    if (broadcastStats.nesting == 0) {
                        broadcastStats.nesting = 1;
                        broadcastStats.startTime = SystemClock.elapsedRealtime();
                    } else {
                        broadcastStats.nesting++;
                    }
                    AlarmManagerService.FilterStats filterStats = inFlight.mFilterStats;
                    filterStats.count++;
                    if (filterStats.nesting == 0) {
                        filterStats.nesting = 1;
                        filterStats.startTime = SystemClock.elapsedRealtime();
                    } else {
                        filterStats.nesting++;
                    }
                    if (alarm.type == 2 || alarm.type == 0) {
                        broadcastStats.numWakeup++;
                        filterStats.numWakeup++;
                    }
                } catch (PendingIntent.CanceledException e) {
                    long j = alarm.repeatInterval;
                }
            }
        }
    }

    protected boolean isPowerOffAlarmType(int i) {
        if (i != 7) {
            return false;
        }
        return true;
    }

    protected boolean schedulePoweroffAlarm(int i, long j, long j2, PendingIntent pendingIntent, IAlarmListener iAlarmListener, String str, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClockInfo, String str2) {
        if (i != 7) {
            return true;
        }
        if (this.mNativeData == -1) {
            Slog.w("AlarmManager", "alarm driver not open ,return!");
            return false;
        }
        if (DEBUG_ALARM_CLOCK) {
            Slog.d("AlarmManager", "alarm set type 7 , package name " + pendingIntent.getTargetPackage());
        }
        pendingIntent.getTargetPackage();
        long jCurrentTimeMillis = System.currentTimeMillis();
        long j3 = j - POWER_OFF_ALARM_BUFFER_TIME;
        if (j3 < jCurrentTimeMillis) {
            if (DEBUG_ALARM_CLOCK) {
                Slog.w("AlarmManager", "PowerOff alarm set time is wrong! nowTime = " + jCurrentTimeMillis + " ; triggerAtTime = " + j3);
            }
            return false;
        }
        if (DEBUG_ALARM_CLOCK) {
            Slog.d("AlarmManager", "PowerOff alarm TriggerTime = " + j3 + " now = " + jCurrentTimeMillis);
        }
        synchronized (this.mPowerOffAlarmLock) {
            removePoweroffAlarmLocked(pendingIntent.getTargetPackage());
            addPoweroffAlarmLocked(new AlarmManagerService.Alarm(i, j3, 0L, 0L, 0L, j2, pendingIntent, iAlarmListener, str, workSource, 0, alarmClockInfo, UserHandle.getCallingUserId(), str2, false));
            if (this.mPoweroffAlarms.size() > 0) {
                resetPoweroffAlarm(this.mPoweroffAlarms.get(0));
            }
        }
        return true;
    }

    protected void updatePoweroffAlarmtoNowRtc() {
        updatePoweroffAlarm(System.currentTimeMillis());
    }

    private void updatePoweroffAlarm(long j) {
        synchronized (this.mPowerOffAlarmLock) {
            if (this.mPoweroffAlarms.size() == 0) {
                return;
            }
            if (this.mPoweroffAlarms.get(0).when > j) {
                return;
            }
            Iterator<AlarmManagerService.Alarm> it = this.mPoweroffAlarms.iterator();
            while (it.hasNext() && it.next().when <= j) {
                if (DEBUG_ALARM_CLOCK) {
                    Slog.w("AlarmManager", "power off alarm update deleted");
                }
                it.remove();
            }
            if (this.mPoweroffAlarms.size() > 0) {
                resetPoweroffAlarm(this.mPoweroffAlarms.get(0));
            }
        }
    }

    private int addPoweroffAlarmLocked(AlarmManagerService.Alarm alarm) {
        ArrayList<AlarmManagerService.Alarm> arrayList = this.mPoweroffAlarms;
        int iBinarySearch = Collections.binarySearch(arrayList, alarm, sIncreasingTimeOrder);
        int i = 0;
        if (iBinarySearch < 0) {
            iBinarySearch = (0 - iBinarySearch) - 1;
        }
        if (localLOGV) {
            Slog.v("AlarmManager", "Adding alarm " + alarm + " at " + iBinarySearch);
        }
        arrayList.add(iBinarySearch, alarm);
        if (localLOGV) {
            Slog.v("AlarmManager", "alarms: " + arrayList.size() + " type: " + alarm.type);
            for (AlarmManagerService.Alarm alarm2 : arrayList) {
                Time time = new Time();
                time.set(alarm2.when);
                Slog.v("AlarmManager", i + ": " + time.format("%b %d %I:%M:%S %p") + " " + alarm2.operation.getTargetPackage());
                i++;
            }
        }
        return iBinarySearch;
    }

    private void removePoweroffAlarmLocked(String str) {
        ArrayList<AlarmManagerService.Alarm> arrayList = this.mPoweroffAlarms;
        if (arrayList.size() <= 0) {
            return;
        }
        Iterator<AlarmManagerService.Alarm> it = arrayList.iterator();
        while (it.hasNext()) {
            if (it.next().operation.getTargetPackage().equals(str)) {
                it.remove();
            }
        }
    }

    private void resetPoweroffAlarm(AlarmManagerService.Alarm alarm) {
        String targetPackage = alarm.operation.getTargetPackage();
        long j = alarm.when;
        if (this.mNativeData != 0 && this.mNativeData != -1) {
            if (targetPackage.equals("com.android.deskclock")) {
                if (DEBUG_ALARM_CLOCK) {
                    Slog.i("AlarmManager", "mBootPackage = " + targetPackage + " set Prop 2");
                }
                set(this.mNativeData, 7, j / 1000, (j % 1000) * 1000 * 1000);
            } else if (targetPackage.equals("com.mediatek.sqa8.aging")) {
                Slog.i("AlarmManager", "mBootPackage = " + targetPackage + " set Prop 2");
                set(this.mNativeData, 7, j / 1000, (j % 1000) * 1000 * 1000);
            } else if (DEBUG_ALARM_CLOCK) {
                Slog.w("AlarmManager", "unknown package (" + targetPackage + ") to set power off alarm");
            }
            if (DEBUG_ALARM_CLOCK) {
                Slog.i("AlarmManager", "reset power off alarm is " + targetPackage);
                return;
            }
            return;
        }
        if (DEBUG_ALARM_CLOCK) {
            Slog.i("AlarmManager", " do not set alarm to RTC when fd close ");
        }
    }

    public void cancelPoweroffAlarmImpl(String str) {
        if (DEBUG_ALARM_CLOCK) {
            Slog.i("AlarmManager", "remove power off alarm pacakge name " + str);
        }
        synchronized (this.mPowerOffAlarmLock) {
            removePoweroffAlarmLocked(str);
            if (this.mNativeData != 0 && this.mNativeData != -1 && str.equals("com.android.deskclock")) {
                set(this.mNativeData, 7, 0L, 0L);
            }
            if (this.mPoweroffAlarms.size() > 0) {
                resetPoweroffAlarm(this.mPoweroffAlarms.get(0));
            }
        }
    }

    protected void configLogTag(PrintWriter printWriter, String[] strArr, int i) {
        if (i >= strArr.length) {
            printWriter.println("  Invalid argument!");
            return;
        }
        if ("on".equals(strArr[i])) {
            localLOGV = true;
            DEBUG_BATCH = true;
            DEBUG_VALIDATE = true;
            DEBUG_ALARM_CLOCK = true;
            DEBUG_WAKELOCK = true;
            DEBUG_STANDBY = true;
            return;
        }
        if ("off".equals(strArr[i])) {
            localLOGV = false;
            DEBUG_BATCH = false;
            DEBUG_VALIDATE = false;
            DEBUG_ALARM_CLOCK = false;
            DEBUG_WAKELOCK = false;
            DEBUG_STANDBY = false;
            return;
        }
        if ("0".equals(strArr[i])) {
            mAlarmMode = 0;
            Slog.v("AlarmManager", "mAlarmMode = " + mAlarmMode);
            return;
        }
        if ("1".equals(strArr[i])) {
            mAlarmMode = 1;
            Slog.v("AlarmManager", "mAlarmMode = " + mAlarmMode);
            return;
        }
        if ("2".equals(strArr[i])) {
            mAlarmMode = 2;
            Slog.v("AlarmManager", "mAlarmMode = " + mAlarmMode);
            return;
        }
        printWriter.println("  Invalid argument!");
    }

    protected void dumpWithargs(PrintWriter printWriter, String[] strArr) {
        String str;
        int i = 0;
        while (i < strArr.length && (str = strArr[i]) != null && str.length() > 0 && str.charAt(0) == '-') {
            i++;
            if ("-h".equals(str)) {
                printWriter.println("alarm manager dump options:");
                printWriter.println("  log  [on/off]");
                printWriter.println("  Example:");
                printWriter.println("  $adb shell dumpsys alarm log on");
                printWriter.println("  $adb shell dumpsys alarm log off");
                return;
            }
            printWriter.println("Unknown argument: " + str + "; use -h for help");
        }
        if (i < strArr.length) {
            String str2 = strArr[i];
            int i2 = i + 1;
            if ("log".equals(str2)) {
                configLogTag(printWriter, strArr, i2);
                return;
            }
        }
        dumpImpl(printWriter, strArr);
    }

    protected void updateWakeupAlarmLog(AlarmManagerService.Alarm alarm) {
        if (DEBUG_BATCH) {
            if (alarm.type == 0 || alarm.type == 2) {
                if (alarm.operation == null) {
                    Slog.d("AlarmManager", "wakeup alarm = " + alarm + "; listener package = " + alarm.listenerTag + "needGrouping = " + alarm.needGrouping);
                    return;
                }
                Slog.d("AlarmManager", "wakeup alarm = " + alarm + "; package = " + alarm.operation.getTargetPackage() + "needGrouping = " + alarm.needGrouping);
            }
        }
    }
}
