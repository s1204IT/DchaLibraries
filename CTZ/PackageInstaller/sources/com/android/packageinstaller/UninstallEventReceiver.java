package com.android.packageinstaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.packageinstaller.EventResultPersister;

public class UninstallEventReceiver extends BroadcastReceiver {
    private static final Object sLock = new Object();
    private static EventResultPersister sReceiver;

    private static EventResultPersister getReceiver(Context context) {
        synchronized (sLock) {
            if (sReceiver == null) {
                sReceiver = new EventResultPersister(TemporaryFileManager.getUninstallStateFile(context));
            }
        }
        return sReceiver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        getReceiver(context).onEventReceived(context, intent);
    }

    static int addObserver(Context context, int i, EventResultPersister.EventResultObserver eventResultObserver) throws EventResultPersister.OutOfIdsException {
        return getReceiver(context).addObserver(i, eventResultObserver);
    }

    static void removeObserver(Context context, int i) {
        getReceiver(context).removeObserver(i);
    }

    static int getNewId(Context context) throws EventResultPersister.OutOfIdsException {
        return getReceiver(context).getNewId();
    }
}
