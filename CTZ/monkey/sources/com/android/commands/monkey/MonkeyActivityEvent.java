package com.android.commands.monkey;

import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.IWindowManager;

public class MonkeyActivityEvent extends MonkeyEvent {
    long mAlarmTime;
    private ComponentName mApp;

    public MonkeyActivityEvent(ComponentName componentName) {
        super(4);
        this.mAlarmTime = 0L;
        this.mApp = componentName;
    }

    public MonkeyActivityEvent(ComponentName componentName, long j) {
        super(4);
        this.mAlarmTime = 0L;
        this.mApp = componentName;
        this.mAlarmTime = j;
    }

    private Intent getEvent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setComponent(this.mApp);
        intent.addFlags(270532608);
        return intent;
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) {
        Intent event = getEvent();
        if (i > 0) {
            Logger.out.println(":Switch: " + event.toUri(0));
        }
        if (this.mAlarmTime != 0) {
            Bundle bundle = new Bundle();
            bundle.putLong("alarmTime", this.mAlarmTime);
            event.putExtras(bundle);
        }
        try {
            iActivityManager.startActivity((IApplicationThread) null, (String) null, event, (String) null, (IBinder) null, (String) null, 0, 0, (ProfilerInfo) null, (Bundle) null);
            return 1;
        } catch (RemoteException e) {
            Logger.err.println("** Failed talking with activity manager!");
            return -1;
        } catch (SecurityException e2) {
            if (i > 0) {
                Logger.out.println("** Permissions error starting activity " + event.toUri(0));
                return -2;
            }
            return -2;
        }
    }
}
