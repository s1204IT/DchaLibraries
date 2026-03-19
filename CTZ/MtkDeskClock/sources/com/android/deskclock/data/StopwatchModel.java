package com.android.deskclock.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationManagerCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

final class StopwatchModel {
    private final Context mContext;
    private List<Lap> mLaps;
    private final NotificationManagerCompat mNotificationManager;
    private final NotificationModel mNotificationModel;
    private final SharedPreferences mPrefs;
    private Stopwatch mStopwatch;
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();
    private final List<StopwatchListener> mStopwatchListeners = new ArrayList();
    private final StopwatchNotificationBuilder mNotificationBuilder = new StopwatchNotificationBuilder();

    StopwatchModel(Context context, SharedPreferences sharedPreferences, NotificationModel notificationModel) {
        this.mContext = context;
        this.mPrefs = sharedPreferences;
        this.mNotificationModel = notificationModel;
        this.mNotificationManager = NotificationManagerCompat.from(context);
        this.mContext.registerReceiver(this.mLocaleChangedReceiver, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
    }

    void addStopwatchListener(StopwatchListener stopwatchListener) {
        this.mStopwatchListeners.add(stopwatchListener);
    }

    void removeStopwatchListener(StopwatchListener stopwatchListener) {
        this.mStopwatchListeners.remove(stopwatchListener);
    }

    Stopwatch getStopwatch() {
        if (this.mStopwatch == null) {
            this.mStopwatch = StopwatchDAO.getStopwatch(this.mPrefs);
        }
        return this.mStopwatch;
    }

    Stopwatch setStopwatch(Stopwatch stopwatch) {
        Stopwatch stopwatch2 = getStopwatch();
        if (stopwatch2 != stopwatch) {
            StopwatchDAO.setStopwatch(this.mPrefs, stopwatch);
            this.mStopwatch = stopwatch;
            if (!this.mNotificationModel.isApplicationInForeground()) {
                updateNotification();
            }
            if (stopwatch.isReset()) {
                clearLaps();
            }
            Iterator<StopwatchListener> it = this.mStopwatchListeners.iterator();
            while (it.hasNext()) {
                it.next().stopwatchUpdated(stopwatch2, stopwatch);
            }
        }
        return stopwatch;
    }

    List<Lap> getLaps() {
        return Collections.unmodifiableList(getMutableLaps());
    }

    Lap addLap() {
        Stopwatch stopwatch = getStopwatch();
        if (!stopwatch.isRunning() || !canAddMoreLaps()) {
            return null;
        }
        long totalTime = stopwatch.getTotalTime();
        List<Lap> mutableLaps = getMutableLaps();
        int size = mutableLaps.size() + 1;
        StopwatchDAO.addLap(this.mPrefs, size, totalTime);
        Lap lap = new Lap(size, totalTime - (mutableLaps.isEmpty() ? 0L : mutableLaps.get(0).getAccumulatedTime()), totalTime);
        mutableLaps.add(0, lap);
        if (!this.mNotificationModel.isApplicationInForeground()) {
            updateNotification();
        }
        Iterator<StopwatchListener> it = this.mStopwatchListeners.iterator();
        while (it.hasNext()) {
            it.next().lapAdded(lap);
        }
        return lap;
    }

    @VisibleForTesting
    void clearLaps() {
        StopwatchDAO.clearLaps(this.mPrefs);
        getMutableLaps().clear();
    }

    boolean canAddMoreLaps() {
        return getLaps().size() < 98;
    }

    long getLongestLapTime() {
        List<Lap> laps = getLaps();
        long jMax = 0;
        if (laps.isEmpty()) {
            return 0L;
        }
        Iterator<Lap> it = getLaps().iterator();
        while (it.hasNext()) {
            jMax = Math.max(jMax, it.next().getLapTime());
        }
        return Math.max(jMax, getStopwatch().getTotalTime() - laps.get(0).getAccumulatedTime());
    }

    long getCurrentLapTime(long j) {
        return Math.max(0L, j - getLaps().get(0).getAccumulatedTime());
    }

    void updateNotification() {
        Stopwatch stopwatch = getStopwatch();
        if (stopwatch.isReset() || this.mNotificationModel.isApplicationInForeground()) {
            this.mNotificationManager.cancel(this.mNotificationModel.getStopwatchNotificationId());
        } else {
            this.mNotificationManager.notify(this.mNotificationModel.getStopwatchNotificationId(), this.mNotificationBuilder.build(this.mContext, this.mNotificationModel, stopwatch));
        }
    }

    private List<Lap> getMutableLaps() {
        if (this.mLaps == null) {
            this.mLaps = StopwatchDAO.getLaps(this.mPrefs);
        }
        return this.mLaps;
    }

    private final class LocaleChangedReceiver extends BroadcastReceiver {
        private LocaleChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            StopwatchModel.this.updateNotification();
        }
    }
}
