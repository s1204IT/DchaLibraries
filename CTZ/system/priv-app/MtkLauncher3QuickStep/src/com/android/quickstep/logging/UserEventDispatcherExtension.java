package com.android.quickstep.logging;

import android.content.Context;
import android.util.Log;
import com.android.launcher3.logging.LoggerUtils;
import com.android.launcher3.logging.UserEventDispatcher;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.systemui.shared.system.MetricsLoggerCompat;

/* loaded from: classes.dex */
public class UserEventDispatcherExtension extends UserEventDispatcher {
    private static final String TAG = "UserEventDispatcher";

    public UserEventDispatcherExtension(Context context) {
    }

    @Override // com.android.launcher3.logging.UserEventDispatcher
    public void logStateChangeAction(int i, int i2, int i3, int i4, int i5, int i6) {
        new MetricsLoggerCompat().visibility(MetricsLoggerCompat.OVERVIEW_ACTIVITY, i5 == 12);
        super.logStateChangeAction(i, i2, i3, i4, i5, i6);
    }

    @Override // com.android.launcher3.logging.UserEventDispatcher
    public void logActionTip(int i, int i2) {
        LauncherLogProto.Action action = new LauncherLogProto.Action();
        LauncherLogProto.Target target = new LauncherLogProto.Target();
        switch (i) {
            case 0:
                action.type = 3;
                target.type = 3;
                target.containerType = 14;
                break;
            case 1:
                action.type = 0;
                action.touch = 0;
                target.type = 2;
                target.controlType = 14;
                break;
            default:
                Log.e(TAG, "Unexpected action type = " + i);
                break;
        }
        switch (i2) {
            case 0:
                target.tipType = 2;
                break;
            case 1:
                target.tipType = 3;
                break;
            default:
                Log.e(TAG, "Unexpected viewType = " + i2);
                break;
        }
        dispatchUserEvent(LoggerUtils.newLauncherEvent(action, target), null);
    }
}
