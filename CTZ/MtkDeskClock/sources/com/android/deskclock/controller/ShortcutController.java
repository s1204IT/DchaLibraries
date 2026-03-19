package com.android.deskclock.controller;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.UserManager;
import com.android.deskclock.DeskClock;
import com.android.deskclock.HandleApiCalls;
import com.android.deskclock.HandleShortcuts;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.ScreensaverActivity;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Lap;
import com.android.deskclock.data.Stopwatch;
import com.android.deskclock.data.StopwatchListener;
import com.android.deskclock.events.Events;
import com.android.deskclock.events.ShortcutEventTracker;
import com.android.deskclock.stopwatch.StopwatchService;
import com.android.deskclock.uidata.UiDataModel;
import java.util.Arrays;
import java.util.Collections;

@TargetApi(25)
class ShortcutController {
    private final ComponentName mComponentName;
    private final Context mContext;
    private final ShortcutManager mShortcutManager;
    private final UserManager mUserManager;

    ShortcutController(Context context) {
        this.mContext = context;
        this.mComponentName = new ComponentName(this.mContext, (Class<?>) DeskClock.class);
        this.mShortcutManager = (ShortcutManager) this.mContext.getSystemService(ShortcutManager.class);
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        Controller.getController().addEventTracker(new ShortcutEventTracker(this.mContext));
        DataModel.getDataModel().addStopwatchListener(new StopwatchWatcher());
    }

    void updateShortcuts() {
        if (!this.mUserManager.isUserUnlocked()) {
            LogUtils.i("Skipping shortcut update because user is locked.", new Object[0]);
            return;
        }
        try {
            this.mShortcutManager.setDynamicShortcuts(Arrays.asList(createNewAlarmShortcut(), createNewTimerShortcut(), createStopwatchShortcut(), createScreensaverShortcut()));
        } catch (IllegalStateException e) {
            LogUtils.wtf(e);
        }
    }

    private ShortcutInfo createNewAlarmShortcut() {
        return new ShortcutInfo.Builder(this.mContext, UiDataModel.getUiDataModel().getShortcutId(R.string.category_alarm, R.string.action_create)).setIcon(Icon.createWithResource(this.mContext, R.drawable.shortcut_new_alarm)).setActivity(this.mComponentName).setShortLabel(this.mContext.getString(R.string.shortcut_new_alarm_short)).setLongLabel(this.mContext.getString(R.string.shortcut_new_alarm_long)).setIntent(new Intent("android.intent.action.SET_ALARM").setClass(this.mContext, HandleApiCalls.class).addFlags(268435456).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut)).setRank(0).build();
    }

    private ShortcutInfo createNewTimerShortcut() {
        return new ShortcutInfo.Builder(this.mContext, UiDataModel.getUiDataModel().getShortcutId(R.string.category_timer, R.string.action_create)).setIcon(Icon.createWithResource(this.mContext, R.drawable.shortcut_new_timer)).setActivity(this.mComponentName).setShortLabel(this.mContext.getString(R.string.shortcut_new_timer_short)).setLongLabel(this.mContext.getString(R.string.shortcut_new_timer_long)).setIntent(new Intent("android.intent.action.SET_TIMER").setClass(this.mContext, HandleApiCalls.class).addFlags(268435456).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut)).setRank(1).build();
    }

    private ShortcutInfo createStopwatchShortcut() {
        Intent intentPutExtra;
        ShortcutInfo.Builder rank = new ShortcutInfo.Builder(this.mContext, UiDataModel.getUiDataModel().getShortcutId(R.string.category_stopwatch, DataModel.getDataModel().getStopwatch().isRunning() ? R.string.action_pause : R.string.action_start)).setIcon(Icon.createWithResource(this.mContext, R.drawable.shortcut_stopwatch)).setActivity(this.mComponentName).setRank(2);
        if (DataModel.getDataModel().getStopwatch().isRunning()) {
            intentPutExtra = new Intent(StopwatchService.ACTION_PAUSE_STOPWATCH).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
            rank.setShortLabel(this.mContext.getString(R.string.shortcut_pause_stopwatch_short)).setLongLabel(this.mContext.getString(R.string.shortcut_pause_stopwatch_long));
        } else {
            intentPutExtra = new Intent(StopwatchService.ACTION_START_STOPWATCH).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut);
            rank.setShortLabel(this.mContext.getString(R.string.shortcut_start_stopwatch_short)).setLongLabel(this.mContext.getString(R.string.shortcut_start_stopwatch_long));
        }
        intentPutExtra.setClass(this.mContext, HandleShortcuts.class).addFlags(268435456);
        return rank.setIntent(intentPutExtra).build();
    }

    private ShortcutInfo createScreensaverShortcut() {
        return new ShortcutInfo.Builder(this.mContext, UiDataModel.getUiDataModel().getShortcutId(R.string.category_screensaver, R.string.action_show)).setIcon(Icon.createWithResource(this.mContext, R.drawable.shortcut_screensaver)).setActivity(this.mComponentName).setShortLabel(this.mContext.getString(R.string.shortcut_start_screensaver_short)).setLongLabel(this.mContext.getString(R.string.shortcut_start_screensaver_long)).setIntent(new Intent("android.intent.action.MAIN").setClass(this.mContext, ScreensaverActivity.class).addFlags(268435456).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_shortcut)).setRank(3).build();
    }

    private class StopwatchWatcher implements StopwatchListener {
        private StopwatchWatcher() {
        }

        @Override
        public void stopwatchUpdated(Stopwatch stopwatch, Stopwatch stopwatch2) {
            if (!ShortcutController.this.mUserManager.isUserUnlocked()) {
                LogUtils.i("Skipping stopwatch shortcut update because user is locked.", new Object[0]);
                return;
            }
            try {
                ShortcutController.this.mShortcutManager.updateShortcuts(Collections.singletonList(ShortcutController.this.createStopwatchShortcut()));
            } catch (IllegalStateException e) {
                LogUtils.wtf(e);
            }
        }

        @Override
        public void lapAdded(Lap lap) {
        }
    }
}
