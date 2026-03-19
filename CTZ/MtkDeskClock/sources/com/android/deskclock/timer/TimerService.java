package com.android.deskclock.timer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.events.Events;
import com.android.deskclock.uidata.UiDataModel;

public final class TimerService extends Service {
    public static final String ACTION_ADD_MINUTE_TIMER = "com.android.deskclock.action.ADD_MINUTE_TIMER";
    public static final String ACTION_ADD_MINUTE_TIMER_UNEXPIRED = "com.android.deskclock.action.ADD_MINUTE_TIMER_UNEXPIRED";
    public static final String ACTION_PAUSE_TIMER = "com.android.deskclock.action.PAUSE_TIMER";
    private static final String ACTION_PREFIX = "com.android.deskclock.action.";
    private static final String ACTION_RESET_EXPIRED_TIMERS = "com.android.deskclock.action.RESET_EXPIRED_TIMERS";
    private static final String ACTION_RESET_MISSED_TIMERS = "com.android.deskclock.action.RESET_MISSED_TIMERS";
    public static final String ACTION_RESET_TIMER = "com.android.deskclock.action.RESET_TIMER";
    private static final String ACTION_RESET_UNEXPIRED_TIMERS = "com.android.deskclock.action.RESET_UNEXPIRED_TIMERS";
    public static final String ACTION_SHOW_TIMER = "com.android.deskclock.action.SHOW_TIMER";
    public static final String ACTION_START_TIMER = "com.android.deskclock.action.START_TIMER";
    private static final String ACTION_TIMER_EXPIRED = "com.android.deskclock.action.TIMER_EXPIRED";
    private static final String ACTION_UPDATE_NOTIFICATION = "com.android.deskclock.action.UPDATE_NOTIFICATION";
    public static final String EXTRA_TIMER_ID = "com.android.deskclock.extra.TIMER_ID";

    public static Intent createTimerExpiredIntent(Context context, Timer timer) {
        return new Intent(context, (Class<?>) TimerService.class).setAction(ACTION_TIMER_EXPIRED).putExtra(EXTRA_TIMER_ID, timer == null ? -1 : timer.getId());
    }

    public static Intent createResetExpiredTimersIntent(Context context) {
        return new Intent(context, (Class<?>) TimerService.class).setAction(ACTION_RESET_EXPIRED_TIMERS);
    }

    public static Intent createResetUnexpiredTimersIntent(Context context) {
        return new Intent(context, (Class<?>) TimerService.class).setAction(ACTION_RESET_UNEXPIRED_TIMERS);
    }

    public static Intent createResetMissedTimersIntent(Context context) {
        return new Intent(context, (Class<?>) TimerService.class).setAction(ACTION_RESET_MISSED_TIMERS);
    }

    public static Intent createAddMinuteTimerIntent(Context context, int i) {
        return new Intent(context, (Class<?>) TimerService.class).setAction(ACTION_ADD_MINUTE_TIMER).putExtra(EXTRA_TIMER_ID, i);
    }

    public static Intent createUpdateNotificationIntent(Context context) {
        return new Intent(context, (Class<?>) TimerService.class).setAction(ACTION_UPDATE_NOTIFICATION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        byte b;
        try {
            String action = intent.getAction();
            int intExtra = intent.getIntExtra(Events.EXTRA_EVENT_LABEL, R.string.label_intent);
            int iHashCode = action.hashCode();
            byte b2 = 3;
            if (iHashCode != -2057684721) {
                if (iHashCode != -2007207221) {
                    if (iHashCode != 870094030) {
                        b = (iHashCode == 1775954148 && action.equals(ACTION_RESET_UNEXPIRED_TIMERS)) ? (byte) 2 : (byte) -1;
                    } else if (action.equals(ACTION_UPDATE_NOTIFICATION)) {
                        b = 0;
                    }
                } else if (action.equals(ACTION_RESET_EXPIRED_TIMERS)) {
                    b = 1;
                }
            } else if (action.equals(ACTION_RESET_MISSED_TIMERS)) {
                b = 3;
            }
            switch (b) {
                case 0:
                    DataModel.getDataModel().updateTimerNotification();
                    if (DataModel.getDataModel().getExpiredTimers().isEmpty()) {
                        stopSelf();
                    }
                    return 2;
                case 1:
                    DataModel.getDataModel().resetExpiredTimers(intExtra);
                    if (DataModel.getDataModel().getExpiredTimers().isEmpty()) {
                        stopSelf();
                    }
                    return 2;
                case 2:
                    DataModel.getDataModel().resetUnexpiredTimers(intExtra);
                    if (DataModel.getDataModel().getExpiredTimers().isEmpty()) {
                        stopSelf();
                    }
                    return 2;
                case 3:
                    DataModel.getDataModel().resetMissedTimers(intExtra);
                    return 2;
                default:
                    int intExtra2 = intent.getIntExtra(EXTRA_TIMER_ID, -1);
                    Timer timer = DataModel.getDataModel().getTimer(intExtra2);
                    if (timer == null) {
                        if (DataModel.getDataModel().getExpiredTimers().isEmpty()) {
                            stopSelf();
                        }
                        return 2;
                    }
                    switch (action.hashCode()) {
                        case -1391257067:
                            b2 = !action.equals(ACTION_START_TIMER) ? (byte) -1 : (byte) 1;
                            break;
                        case -1330774871:
                            if (action.equals(ACTION_PAUSE_TIMER)) {
                                b2 = 2;
                                break;
                            }
                            break;
                        case -1185812008:
                            if (action.equals(ACTION_TIMER_EXPIRED)) {
                                b2 = 6;
                                break;
                            }
                            break;
                        case -892279658:
                            if (action.equals(ACTION_SHOW_TIMER)) {
                                b2 = 0;
                                break;
                            }
                            break;
                        case -483111368:
                            if (action.equals(ACTION_ADD_MINUTE_TIMER_UNEXPIRED)) {
                                break;
                            }
                            break;
                        case -297710270:
                            if (action.equals(ACTION_RESET_TIMER)) {
                                b2 = 5;
                                break;
                            }
                            break;
                        case 2063487787:
                            if (action.equals(ACTION_ADD_MINUTE_TIMER)) {
                                b2 = 4;
                                break;
                            }
                            break;
                        default:
                            break;
                    }
                    switch (b2) {
                        case 0:
                            Events.sendTimerEvent(R.string.action_show, intExtra);
                            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.TIMERS);
                            startActivity(new Intent(this, (Class<?>) DeskClock.class).putExtra(EXTRA_TIMER_ID, intExtra2).addFlags(268435456));
                            break;
                        case 1:
                            Events.sendTimerEvent(R.string.action_start, intExtra);
                            DataModel.getDataModel().startTimer(this, timer);
                            break;
                        case 2:
                            Events.sendTimerEvent(R.string.action_pause, intExtra);
                            DataModel.getDataModel().pauseTimer(timer);
                            break;
                        case 3:
                        case 4:
                            Events.sendTimerEvent(R.string.action_add_minute, intExtra);
                            DataModel.getDataModel().addTimerMinute(timer);
                            break;
                        case 5:
                            DataModel.getDataModel().resetOrDeleteTimer(timer, intExtra);
                            break;
                        case 6:
                            Events.sendTimerEvent(R.string.action_fire, intExtra);
                            DataModel.getDataModel().expireTimer(this, timer);
                            break;
                    }
                    if (DataModel.getDataModel().getExpiredTimers().isEmpty()) {
                        stopSelf();
                    }
                    return 2;
            }
        } finally {
            if (DataModel.getDataModel().getExpiredTimers().isEmpty()) {
                stopSelf();
            }
        }
    }
}
