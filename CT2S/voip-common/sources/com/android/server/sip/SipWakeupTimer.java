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
    private static final boolean DBG = true;
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
        IntentFilter filter = new IntentFilter(getAction());
        context.registerReceiver(this, filter);
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
        if (this.mEventQueue != null) {
            return false;
        }
        log("Timer stopped");
        return DBG;
    }

    private void cancelAlarm() {
        this.mAlarmManager.cancel(this.mPendingIntent);
        this.mPendingIntent = null;
    }

    private void recalculatePeriods() {
        if (!this.mEventQueue.isEmpty()) {
            MyEvent firstEvent = this.mEventQueue.first();
            int minPeriod = firstEvent.mMaxPeriod;
            long minTriggerTime = firstEvent.mTriggerTime;
            for (MyEvent e : this.mEventQueue) {
                e.mPeriod = (e.mMaxPeriod / minPeriod) * minPeriod;
                int interval = (int) ((e.mLastTriggerTime + ((long) e.mMaxPeriod)) - minTriggerTime);
                e.mTriggerTime = ((long) ((interval / minPeriod) * minPeriod)) + minTriggerTime;
            }
            TreeSet<MyEvent> newQueue = new TreeSet<>(this.mEventQueue.comparator());
            newQueue.addAll(this.mEventQueue);
            this.mEventQueue.clear();
            this.mEventQueue = newQueue;
            log("queue re-calculated");
            printQueue();
        }
    }

    private void insertEvent(MyEvent event) {
        long now = SystemClock.elapsedRealtime();
        if (this.mEventQueue.isEmpty()) {
            event.mTriggerTime = ((long) event.mPeriod) + now;
            this.mEventQueue.add(event);
            return;
        }
        MyEvent firstEvent = this.mEventQueue.first();
        int minPeriod = firstEvent.mPeriod;
        if (minPeriod <= event.mMaxPeriod) {
            event.mPeriod = (event.mMaxPeriod / minPeriod) * minPeriod;
            int interval = event.mMaxPeriod;
            event.mTriggerTime = firstEvent.mTriggerTime + ((long) (((interval - ((int) (firstEvent.mTriggerTime - now))) / minPeriod) * minPeriod));
            this.mEventQueue.add(event);
            return;
        }
        long triggerTime = now + ((long) event.mPeriod);
        if (firstEvent.mTriggerTime < triggerTime) {
            event.mTriggerTime = firstEvent.mTriggerTime;
            event.mLastTriggerTime -= (long) event.mPeriod;
        } else {
            event.mTriggerTime = triggerTime;
        }
        this.mEventQueue.add(event);
        recalculatePeriods();
    }

    public synchronized void set(int period, Runnable callback) {
        if (!stopped()) {
            long now = SystemClock.elapsedRealtime();
            MyEvent event = new MyEvent(period, callback, now);
            insertEvent(event);
            if (this.mEventQueue.first() == event) {
                if (this.mEventQueue.size() > 1) {
                    cancelAlarm();
                }
                scheduleNext();
            }
            long triggerTime = event.mTriggerTime;
            log("set: add event " + event + " scheduled on " + showTime(triggerTime) + " at " + showTime(now) + ", #events=" + this.mEventQueue.size());
            printQueue();
        }
    }

    public synchronized void cancel(Runnable callback) {
        if (!stopped() && !this.mEventQueue.isEmpty()) {
            log("cancel:" + callback);
            MyEvent firstEvent = this.mEventQueue.first();
            Iterator<MyEvent> iter = this.mEventQueue.iterator();
            while (iter.hasNext()) {
                MyEvent event = iter.next();
                if (event.mCallback == callback) {
                    iter.remove();
                    log("    cancel found:" + event);
                }
            }
            if (this.mEventQueue.isEmpty()) {
                cancelAlarm();
            } else if (this.mEventQueue.first() != firstEvent) {
                cancelAlarm();
                MyEvent firstEvent2 = this.mEventQueue.first();
                MyEvent firstEvent3 = firstEvent2;
                firstEvent3.mPeriod = firstEvent3.mMaxPeriod;
                firstEvent3.mTriggerTime = firstEvent3.mLastTriggerTime + ((long) firstEvent3.mPeriod);
                recalculatePeriods();
                scheduleNext();
            }
            log("cancel: X");
            printQueue();
        }
    }

    private void scheduleNext() {
        if (!stopped() && !this.mEventQueue.isEmpty()) {
            if (this.mPendingIntent != null) {
                throw new RuntimeException("pendingIntent is not null!");
            }
            MyEvent event = this.mEventQueue.first();
            Intent intent = new Intent(getAction());
            intent.putExtra(TRIGGER_TIME, event.mTriggerTime);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728);
            this.mPendingIntent = pendingIntent;
            this.mAlarmManager.set(2, event.mTriggerTime, pendingIntent);
        }
    }

    @Override
    public synchronized void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (getAction().equals(action) && intent.getExtras().containsKey(TRIGGER_TIME)) {
            this.mPendingIntent = null;
            long triggerTime = intent.getLongExtra(TRIGGER_TIME, -1L);
            execute(triggerTime);
        } else {
            log("onReceive: unrecognized intent: " + intent);
        }
    }

    private void printQueue() {
        int count = 0;
        for (MyEvent event : this.mEventQueue) {
            log("     " + event + ": scheduled at " + showTime(event.mTriggerTime) + ": last at " + showTime(event.mLastTriggerTime));
            count++;
            if (count >= 5) {
                break;
            }
        }
        if (this.mEventQueue.size() > count) {
            log("     .....");
        } else if (count == 0) {
            log("     <empty>");
        }
    }

    private void execute(long triggerTime) {
        log("time's up, triggerTime = " + showTime(triggerTime) + ": " + this.mEventQueue.size());
        if (!stopped() && !this.mEventQueue.isEmpty()) {
            for (MyEvent event : this.mEventQueue) {
                if (event.mTriggerTime == triggerTime) {
                    log("execute " + event);
                    event.mLastTriggerTime = triggerTime;
                    event.mTriggerTime += (long) event.mPeriod;
                    this.mExecutor.execute(event.mCallback);
                }
            }
            log("after timeout execution");
            printQueue();
            scheduleNext();
        }
    }

    private String getAction() {
        return toString();
    }

    private String showTime(long time) {
        int ms = (int) (time % 1000);
        int s = (int) (time / 1000);
        int m = s / 60;
        return String.format("%d.%d.%d", Integer.valueOf(m), Integer.valueOf(s % 60), Integer.valueOf(ms));
    }

    private static class MyEvent {
        Runnable mCallback;
        long mLastTriggerTime;
        int mMaxPeriod;
        int mPeriod;
        long mTriggerTime;

        MyEvent(int period, Runnable callback, long now) {
            this.mMaxPeriod = period;
            this.mPeriod = period;
            this.mCallback = callback;
            this.mLastTriggerTime = now;
        }

        public String toString() {
            String s = super.toString();
            return s.substring(s.indexOf("@")) + ":" + (this.mPeriod / 1000) + ":" + (this.mMaxPeriod / 1000) + ":" + toString(this.mCallback);
        }

        private String toString(Object o) {
            String s = o.toString();
            int index = s.indexOf("$");
            return index > 0 ? s.substring(index + 1) : s;
        }
    }

    private static class MyEventComparator implements Comparator<MyEvent> {
        private MyEventComparator() {
        }

        @Override
        public int compare(MyEvent e1, MyEvent e2) {
            if (e1 == e2) {
                return 0;
            }
            int diff = e1.mMaxPeriod - e2.mMaxPeriod;
            if (diff == 0) {
                return -1;
            }
            return diff;
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return SipWakeupTimer.DBG;
            }
            return false;
        }
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }
}
