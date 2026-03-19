package com.android.commands.monkey;

import android.app.IActivityManager;
import android.os.RemoteException;
import android.view.IWindowManager;

public class MonkeyRotationEvent extends MonkeyEvent {
    private final boolean mPersist;
    private final int mRotationDegree;

    public MonkeyRotationEvent(int i, boolean z) {
        super(3);
        this.mRotationDegree = i;
        this.mPersist = z;
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) {
        if (i > 0) {
            Logger.out.println(":Sending rotation degree=" + this.mRotationDegree + ", persist=" + this.mPersist);
        }
        try {
            iWindowManager.freezeRotation(this.mRotationDegree);
            if (!this.mPersist) {
                iWindowManager.thawRotation();
                return 1;
            }
            return 1;
        } catch (RemoteException e) {
            return -1;
        }
    }
}
