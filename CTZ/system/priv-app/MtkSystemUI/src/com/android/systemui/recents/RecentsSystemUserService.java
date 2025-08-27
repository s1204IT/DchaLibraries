package com.android.systemui.recents;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.android.systemui.SystemUIApplication;

/* loaded from: classes.dex */
public class RecentsSystemUserService extends Service {
    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        Recents recents = (Recents) ((SystemUIApplication) getApplication()).getComponent(Recents.class);
        if (recents != null) {
            return recents.getSystemUserCallbacks();
        }
        return null;
    }
}
