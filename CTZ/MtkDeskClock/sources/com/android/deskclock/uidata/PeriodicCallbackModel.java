package com.android.deskclock.uidata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;
import com.android.deskclock.LogUtils;
import com.android.deskclock.Utils;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class PeriodicCallbackModel {
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("Periodic");
    private static final long QUARTER_HOUR_IN_MILLIS = 900000;
    private static Handler sHandler;
    private final BroadcastReceiver mTimeChangedReceiver = new TimeChangedReceiver();
    private final List<PeriodicRunnable> mPeriodicRunnables = new CopyOnWriteArrayList();

    @VisibleForTesting
    enum Period {
        MINUTE,
        QUARTER_HOUR,
        HOUR,
        MIDNIGHT
    }

    PeriodicCallbackModel(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.DATE_CHANGED");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        context.registerReceiver(this.mTimeChangedReceiver, intentFilter);
    }

    void addMinuteCallback(Runnable runnable, long j) {
        addPeriodicCallback(runnable, Period.MINUTE, j);
    }

    void addQuarterHourCallback(Runnable runnable, long j) {
        addPeriodicCallback(runnable, Period.QUARTER_HOUR, j);
    }

    void addHourCallback(Runnable runnable, long j) {
        addPeriodicCallback(runnable, Period.HOUR, j);
    }

    void addMidnightCallback(Runnable runnable, long j) {
        addPeriodicCallback(runnable, Period.MIDNIGHT, j);
    }

    private void addPeriodicCallback(Runnable runnable, Period period, long j) {
        PeriodicRunnable periodicRunnable = new PeriodicRunnable(runnable, period, j);
        this.mPeriodicRunnables.add(periodicRunnable);
        periodicRunnable.schedule();
    }

    void removePeriodicCallback(Runnable runnable) {
        for (PeriodicRunnable periodicRunnable : this.mPeriodicRunnables) {
            if (periodicRunnable.mDelegate == runnable) {
                periodicRunnable.unSchedule();
                this.mPeriodicRunnables.remove(periodicRunnable);
                return;
            }
        }
    }

    @VisibleForTesting
    static long getDelay(long j, Period period, long j2) {
        long j3 = j - j2;
        switch (period) {
            case MINUTE:
                return (((j3 - (j3 % 60000)) + 60000) - j) + j2;
            case QUARTER_HOUR:
                return (((j3 - (j3 % QUARTER_HOUR_IN_MILLIS)) + QUARTER_HOUR_IN_MILLIS) - j) + j2;
            case HOUR:
                return (((j3 - (j3 % 3600000)) + 3600000) - j) + j2;
            case MIDNIGHT:
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(j3);
                calendar.add(5, 1);
                calendar.set(11, 0);
                calendar.set(12, 0);
                calendar.set(13, 0);
                calendar.set(14, 0);
                return (calendar.getTimeInMillis() - j) + j2;
            default:
                throw new IllegalArgumentException("unexpected period: " + period);
        }
    }

    private static Handler getHandler() {
        Utils.enforceMainLooper();
        if (sHandler == null) {
            sHandler = new Handler();
        }
        return sHandler;
    }

    private static final class PeriodicRunnable implements Runnable {
        private final Runnable mDelegate;
        private final long mOffset;
        private final Period mPeriod;

        public PeriodicRunnable(Runnable runnable, Period period, long j) {
            this.mDelegate = runnable;
            this.mPeriod = period;
            this.mOffset = j;
        }

        @Override
        public void run() {
            PeriodicCallbackModel.LOGGER.i("Executing periodic callback for %s because the period ended", this.mPeriod);
            this.mDelegate.run();
            schedule();
        }

        private void runAndReschedule() {
            PeriodicCallbackModel.LOGGER.i("Executing periodic callback for %s because the time changed", this.mPeriod);
            unSchedule();
            this.mDelegate.run();
            schedule();
        }

        private void schedule() {
            PeriodicCallbackModel.getHandler().postDelayed(this, PeriodicCallbackModel.getDelay(System.currentTimeMillis(), this.mPeriod, this.mOffset));
        }

        private void unSchedule() {
            PeriodicCallbackModel.getHandler().removeCallbacks(this);
        }
    }

    private final class TimeChangedReceiver extends BroadcastReceiver {
        private TimeChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Iterator it = PeriodicCallbackModel.this.mPeriodicRunnables.iterator();
            while (it.hasNext()) {
                ((PeriodicRunnable) it.next()).runAndReschedule();
            }
        }
    }
}
