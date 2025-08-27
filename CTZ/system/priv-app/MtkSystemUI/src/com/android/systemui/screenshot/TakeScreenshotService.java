package com.android.systemui.screenshot;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.Log;

/* loaded from: classes.dex */
public class TakeScreenshotService extends Service {
    private static GlobalScreenshot mScreenshot;
    private Handler mHandler = new Handler() { // from class: com.android.systemui.screenshot.TakeScreenshotService.1
        @Override // android.os.Handler
        public void handleMessage(Message message) throws Resources.NotFoundException {
            final Messenger messenger = message.replyTo;
            Runnable runnable = new Runnable() { // from class: com.android.systemui.screenshot.TakeScreenshotService.1.1
                @Override // java.lang.Runnable
                public void run() throws RemoteException {
                    try {
                        messenger.send(Message.obtain((Handler) null, 1));
                    } catch (RemoteException e) {
                    }
                }
            };
            if (((UserManager) TakeScreenshotService.this.getSystemService(UserManager.class)).isUserUnlocked()) {
                if (TakeScreenshotService.mScreenshot == null) {
                    GlobalScreenshot unused = TakeScreenshotService.mScreenshot = new GlobalScreenshot(TakeScreenshotService.this);
                }
                switch (message.what) {
                    case 1:
                        TakeScreenshotService.mScreenshot.takeScreenshot(runnable, message.arg1 > 0, message.arg2 > 0);
                        break;
                    case 2:
                        TakeScreenshotService.mScreenshot.takeScreenshotPartial(runnable, message.arg1 > 0, message.arg2 > 0);
                        break;
                    default:
                        Log.d("TakeScreenshotService", "Invalid screenshot option: " + message.what);
                        break;
                }
            }
            Log.w("TakeScreenshotService", "Skipping screenshot because storage is locked!");
            post(runnable);
        }
    };

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return new Messenger(this.mHandler).getBinder();
    }

    @Override // android.app.Service
    public boolean onUnbind(Intent intent) {
        if (mScreenshot != null) {
            mScreenshot.stopScreenshot();
            return true;
        }
        return true;
    }
}
