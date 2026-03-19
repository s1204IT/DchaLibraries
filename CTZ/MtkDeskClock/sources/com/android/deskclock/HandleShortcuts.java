package com.android.deskclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.android.deskclock.LogUtils;
import com.android.deskclock.events.Events;
import com.android.deskclock.stopwatch.StopwatchService;
import com.android.deskclock.uidata.UiDataModel;

public class HandleShortcuts extends Activity {
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("HandleShortcuts");

    @Override
    protected void onCreate(Bundle bundle) {
        byte b;
        super.onCreate(bundle);
        Intent intent = getIntent();
        try {
            try {
                String action = intent.getAction();
                int iHashCode = action.hashCode();
                if (iHashCode != -1166176035) {
                    b = (iHashCode == -637728399 && action.equals(StopwatchService.ACTION_PAUSE_STOPWATCH)) ? (byte) 0 : (byte) -1;
                    switch (b) {
                        case 0:
                            Events.sendStopwatchEvent(R.string.action_pause, R.string.label_shortcut);
                            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.STOPWATCH);
                            startActivity(new Intent(this, (Class<?>) DeskClock.class).setAction(StopwatchService.ACTION_PAUSE_STOPWATCH));
                            setResult(-1);
                            break;
                        case 1:
                            Events.sendStopwatchEvent(R.string.action_start, R.string.label_shortcut);
                            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.STOPWATCH);
                            startActivity(new Intent(this, (Class<?>) DeskClock.class).setAction(StopwatchService.ACTION_START_STOPWATCH));
                            setResult(-1);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported action: " + action);
                    }
                } else {
                    if (action.equals(StopwatchService.ACTION_START_STOPWATCH)) {
                        b = 1;
                    }
                    switch (b) {
                    }
                }
            } catch (Exception e) {
                LOGGER.e("Error handling intent: " + intent, e);
                setResult(0);
            }
        } finally {
            finish();
        }
    }
}
