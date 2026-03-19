package com.android.deskclock.stopwatch;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;
import com.android.deskclock.uidata.UiDataModel;

public final class StopwatchService extends Service {
    public static final String ACTION_LAP_STOPWATCH = "com.android.deskclock.action.LAP_STOPWATCH";
    public static final String ACTION_PAUSE_STOPWATCH = "com.android.deskclock.action.PAUSE_STOPWATCH";
    private static final String ACTION_PREFIX = "com.android.deskclock.action.";
    public static final String ACTION_RESET_STOPWATCH = "com.android.deskclock.action.RESET_STOPWATCH";
    public static final String ACTION_SHOW_STOPWATCH = "com.android.deskclock.action.SHOW_STOPWATCH";
    public static final String ACTION_START_STOPWATCH = "com.android.deskclock.action.START_STOPWATCH";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        int intExtra;
        String action = intent.getAction();
        intExtra = intent.getIntExtra(Events.EXTRA_EVENT_LABEL, R.string.label_intent);
        switch (action) {
            case "com.android.deskclock.action.SHOW_STOPWATCH":
                Events.sendStopwatchEvent(R.string.action_show, intExtra);
                UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.STOPWATCH);
                startActivity(new Intent(this, (Class<?>) DeskClock.class).addFlags(268435456));
                return 2;
            case "com.android.deskclock.action.START_STOPWATCH":
                Events.sendStopwatchEvent(R.string.action_start, intExtra);
                DataModel.getDataModel().startStopwatch();
                return 2;
            case "com.android.deskclock.action.PAUSE_STOPWATCH":
                Events.sendStopwatchEvent(R.string.action_pause, intExtra);
                DataModel.getDataModel().pauseStopwatch();
                return 2;
            case "com.android.deskclock.action.RESET_STOPWATCH":
                Events.sendStopwatchEvent(R.string.action_reset, intExtra);
                DataModel.getDataModel().resetStopwatch();
                return 2;
            case "com.android.deskclock.action.LAP_STOPWATCH":
                Events.sendStopwatchEvent(R.string.action_lap, intExtra);
                DataModel.getDataModel().addLap();
                return 2;
            default:
                return 2;
        }
    }
}
