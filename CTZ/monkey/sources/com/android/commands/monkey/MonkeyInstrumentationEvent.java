package com.android.commands.monkey;

import android.app.IActivityManager;
import android.app.IInstrumentationWatcher;
import android.app.IUiAutomationConnection;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.IWindowManager;

public class MonkeyInstrumentationEvent extends MonkeyEvent {
    String mRunnerName;
    String mTestCaseName;

    public MonkeyInstrumentationEvent(String str, String str2) {
        super(4);
        this.mTestCaseName = str;
        this.mRunnerName = str2;
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) {
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(this.mRunnerName);
        if (componentNameUnflattenFromString == null || this.mTestCaseName == null) {
            throw new IllegalArgumentException("Bad component name");
        }
        Bundle bundle = new Bundle();
        bundle.putString("class", this.mTestCaseName);
        try {
            iActivityManager.startInstrumentation(componentNameUnflattenFromString, (String) null, 0, bundle, (IInstrumentationWatcher) null, (IUiAutomationConnection) null, 0, (String) null);
            return 1;
        } catch (RemoteException e) {
            Logger.err.println("** Failed talking with activity manager!");
            return -1;
        }
    }
}
