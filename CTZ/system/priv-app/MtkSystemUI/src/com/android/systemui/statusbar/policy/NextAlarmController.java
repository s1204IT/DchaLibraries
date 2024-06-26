package com.android.systemui.statusbar.policy;

import android.app.AlarmManager;
import com.android.systemui.Dumpable;
/* loaded from: classes.dex */
public interface NextAlarmController extends Dumpable, CallbackController<NextAlarmChangeCallback> {

    /* loaded from: classes.dex */
    public interface NextAlarmChangeCallback {
        void onNextAlarmChanged(AlarmManager.AlarmClockInfo alarmClockInfo);
    }
}
