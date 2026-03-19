package com.android.server.sip;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.telephony.Rlog;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.Executor;

class SipWakeupTimer extends BroadcastReceiver {
    private static final boolean DBG = false;
    private static final String TAG = "SipWakeupTimer";
    private static final String TRIGGER_TIME = "TriggerTime";
    private AlarmManager mAlarmManager;
    private Context mContext;
    private TreeSet<MyEvent> mEventQueue = new TreeSet<>(new MyEventComparator());
    private Executor mExecutor;
    private PendingIntent mPendingIntent;

    public SipWakeupTimer(Context context, Executor executor) {
        this.mContext = context;
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        context.registerReceiver(this, new IntentFilter(getAction()));
        this.mExecutor = executor;
    }

    public synchronized void stop() {
        this.mContext.unregisterReceiver(this);
        if (this.mPendingIntent != null) {
            this.mAlarmManager.cancel(this.mPendingIntent);
            this.mPendingIntent = null;
        }
        this.mEventQueue.clear();
        this.mEventQueue = null;
    }

    private boolean stopped() {
        if (this.mEventQueue == null) {
            return true;
        }
        return DBG;
    }

    private void cancelAlarm() {
        this.mAlarmManager.cancel(this.mPendingIntent);
        this.mPendingIntent = null;
    }

    private void recalculatePeriods() {
        if (this.mEventQueue.isEmpty()) {
            return;
        }
        MyEvent myEventFirst = this.mEventQueue.first();
        int i = myEventFirst.mMaxPeriod;
        long j = myEventFirst.mTriggerTime;
        for (MyEvent myEvent : this.mEventQueue) {
            myEvent.mPeriod = (myEvent.mMaxPeriod / i) * i;
            myEvent.mTriggerTime = ((long) ((((int) ((myEvent.mLastTriggerTime + ((long) myEvent.mMaxPeriod)) - j)) / i) * i)) + j;
        }
        TreeSet<MyEvent> treeSet = new TreeSet<>(this.mEventQueue.comparator());
        treeSet.addAll(this.mEventQueue);
        this.mEventQueue.clear();
        this.mEventQueue = treeSet;
    }

    private void insertEvent(MyEvent myEvent) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        if (this.mEventQueue.isEmpty()) {
            myEvent.mTriggerTime = jElapsedRealtime + ((long) myEvent.mPeriod);
            this.mEventQueue.add(myEvent);
            return;
        }
        MyEvent myEventFirst = this.mEventQueue.first();
        int i = myEventFirst.mPeriod;
        if (i <= myEvent.mMaxPeriod) {
            myEvent.mPeriod = (myEvent.mMaxPeriod / i) * i;
            myEvent.mTriggerTime = myEventFirst.mTriggerTime + ((long) (((myEvent.mMaxPeriod - ((int) (myEventFirst.mTriggerTime - jElapsedRealtime))) / i) * i));
            this.mEventQueue.add(myEvent);
            return;
        }
        long j = jElapsedRealtime + ((long) myEvent.mPeriod);
        if (myEventFirst.mTriggerTime < j) {
            myEvent.mTriggerTime = myEventFirst.mTriggerTime;
            myEvent.mLastTriggerTime -= (long) myEvent.mPeriod;
        } else {
            myEvent.mTriggerTime = j;
        }
        this.mEventQueue.add(myEvent);
        recalculatePeriods();
    }

    public synchronized void set(int i, Runnable runnable) {
        if (stopped()) {
            return;
        }
        MyEvent myEvent = new MyEvent(i, runnable, SystemClock.elapsedRealtime());
        insertEvent(myEvent);
        if (this.mEventQueue.first() == myEvent) {
            if (this.mEventQueue.size() > 1) {
                cancelAlarm();
            }
            scheduleNext();
        }
        long j = myEvent.mTriggerTime;
    }

    public synchronized void cancel(Runnable runnable) {
        if (!stopped() && !this.mEventQueue.isEmpty()) {
            MyEvent myEventFirst = this.mEventQueue.first();
            Iterator<MyEvent> it = this.mEventQueue.iterator();
            while (it.hasNext()) {
                if (it.next().mCallback == runnable) {
                    it.remove();
                }
            }
            if (this.mEventQueue.isEmpty()) {
                cancelAlarm();
            } else if (this.mEventQueue.first() != myEventFirst) {
                cancelAlarm();
                MyEvent myEventFirst2 = this.mEventQueue.first();
                myEventFirst2.mPeriod = myEventFirst2.mMaxPeriod;
                myEventFirst2.mTriggerTime = myEventFirst2.mLastTriggerTime + ((long) myEventFirst2.mPeriod);
                recalculatePeriods();
                scheduleNext();
            }
        }
    }

    private void scheduleNext() {
        if (stopped() || this.mEventQueue.isEmpty()) {
            return;
        }
        if (this.mPendingIntent != null) {
            throw new RuntimeException("pendingIntent is not null!");
        }
        MyEvent myEventFirst = this.mEventQueue.first();
        Intent intent = new Intent(getAction());
        intent.putExtra(TRIGGER_TIME, myEventFirst.mTriggerTime);
        PendingIntent broadcast = PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728);
        this.mPendingIntent = broadcast;
        this.mAlarmManager.set(2, myEventFirst.mTriggerTime, broadcast);
    }

    @Override
    public synchronized void onReceive(Context context, Intent intent) {
        if (getAction().equals(intent.getAction()) && intent.getExtras().containsKey(TRIGGER_TIME)) {
            this.mPendingIntent = null;
            execute(intent.getLongExtra(TRIGGER_TIME, -1L));
        } else {
            log("onReceive: unrecognized intent: " + intent);
        }
    }

    private void printQueue() {
        int i = 0;
        for (MyEvent myEvent : this.mEventQueue) {
            log("     " + myEvent + ": scheduled at " + showTime(myEvent.mTriggerTime) + ": last at " + showTime(myEvent.mLastTriggerTime));
            i++;
            if (i >= 5) {
                break;
            }
        }
        if (this.mEventQueue.size() > i) {
            log("     .....");
        } else if (i == 0) {
            log("     <empty>");
        }
    }

    private void execute(long j) {
        if (stopped() || this.mEventQueue.isEmpty()) {
            return;
        }
        for (MyEvent myEvent : this.mEventQueue) {
            if (myEvent.mTriggerTime == j) {
                myEvent.mLastTriggerTime = j;
                myEvent.mTriggerTime += (long) myEvent.mPeriod;
                this.mExecutor.execute(myEvent.mCallback);
            }
        }
        scheduleNext();
    }

    private String getAction() {
        return toString();
    }

    private String showTime(long j) {
        int i = (int) (j % 1000);
        int i2 = (int) (j / 1000);
        return String.format("%d.%d.%d", Integer.valueOf(i2 / 60), Integer.valueOf(i2 % 60), Integer.valueOf(i));
    }

    private static class MyEvent {
        Runnable mCallback;
        long mLastTriggerTime;
        int mMaxPeriod;
        int mPeriod;
        long mTriggerTime;

        MyEvent(int i, Runnable runnable, long j) {
            this.mMaxPeriod = i;
            this.mPeriod = i;
            this.mCallback = runnable;
            this.mLastTriggerTime = j;
        }

        public String toString() {
            String string = super.toString();
            return string.substring(string.indexOf("@")) + ":" + (this.mPeriod / 1000) + ":" + (this.mMaxPeriod / 1000) + ":" + toString(this.mCallback);
        }

        private String toString(Object obj) {
            String string = obj.toString();
            int iIndexOf = string.indexOf("$");
            return iIndexOf > 0 ? string.substring(iIndexOf + 1) : string;
        }
    }

    private static class MyEventComparator implements Comparator<MyEvent> {
        private MyEventComparator() {
        }

        @Override
        public int compare(MyEvent myEvent, MyEvent myEvent2) {
            if (myEvent == myEvent2) {
                return 0;
            }
            int i = myEvent.mMaxPeriod - myEvent2.mMaxPeriod;
            if (i == 0) {
                return -1;
            }
            return i;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return SipWakeupTimer.DBG;
        }
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }
}
