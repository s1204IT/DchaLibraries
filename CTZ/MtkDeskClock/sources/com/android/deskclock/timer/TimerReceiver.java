package com.android.deskclock.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.deskclock.LogUtils;
import com.android.deskclock.data.DataModel;

public class TimerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.e("TimerReceiver", "Received legacy timer broadcast: %s", intent.getAction());
        if ("times_up".equals(intent.getAction())) {
            context.startService(TimerService.createTimerExpiredIntent(context, DataModel.getDataModel().getTimer(intent.getIntExtra("timer.intent.extra", -1))));
        }
    }
}
