package com.android.internal.util;

import android.app.job.JobInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.InputDevice;

public class ScreenshotHelper {
    private static final String SYSUI_PACKAGE = "com.android.systemui";
    private static final String SYSUI_SCREENSHOT_ERROR_RECEIVER = "com.android.systemui.screenshot.ScreenshotServiceErrorReceiver";
    private static final String SYSUI_SCREENSHOT_SERVICE = "com.android.systemui.screenshot.TakeScreenshotService";
    private static final String TAG = "ScreenshotHelper";
    private final Context mContext;
    private final int SCREENSHOT_TIMEOUT_MS = 10000;
    private final Object mScreenshotLock = new Object();
    private ServiceConnection mScreenshotConnection = null;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (ScreenshotHelper.this.mScreenshotLock) {
                if (Intent.ACTION_USER_SWITCHED.equals(intent.getAction())) {
                    ScreenshotHelper.this.resetConnection();
                }
            }
        }
    };

    public ScreenshotHelper(Context context) {
        this.mContext = context;
        this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter(Intent.ACTION_USER_SWITCHED));
    }

    public void takeScreenshot(final int i, final boolean z, final boolean z2, final Handler handler) {
        synchronized (this.mScreenshotLock) {
            if (this.mScreenshotConnection != null) {
                return;
            }
            ComponentName componentName = new ComponentName(SYSUI_PACKAGE, SYSUI_SCREENSHOT_SERVICE);
            Intent intent = new Intent();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (ScreenshotHelper.this.mScreenshotLock) {
                        if (ScreenshotHelper.this.mScreenshotConnection != null) {
                            Log.e(ScreenshotHelper.TAG, "Timed out before getting screenshot capture response");
                            ScreenshotHelper.this.resetConnection();
                            ScreenshotHelper.this.notifyScreenshotError();
                        }
                    }
                }
            };
            intent.setComponent(componentName);
            ServiceConnection serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName2, IBinder iBinder) {
                    synchronized (ScreenshotHelper.this.mScreenshotLock) {
                        if (ScreenshotHelper.this.mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(iBinder);
                        Message messageObtain = Message.obtain((Handler) null, i);
                        messageObtain.replyTo = new Messenger(new Handler(handler.getLooper()) {
                            @Override
                            public void handleMessage(Message message) {
                                synchronized (ScreenshotHelper.this.mScreenshotLock) {
                                    if (ScreenshotHelper.this.mScreenshotConnection == this) {
                                        ScreenshotHelper.this.resetConnection();
                                        handler.removeCallbacks(runnable);
                                    }
                                }
                            }
                        });
                        messageObtain.arg1 = z ? 1 : 0;
                        messageObtain.arg2 = z2 ? 1 : 0;
                        try {
                            messenger.send(messageObtain);
                        } catch (RemoteException e) {
                            Log.e(ScreenshotHelper.TAG, "Couldn't take screenshot: " + e);
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName2) {
                    synchronized (ScreenshotHelper.this.mScreenshotLock) {
                        if (ScreenshotHelper.this.mScreenshotConnection != null) {
                            ScreenshotHelper.this.resetConnection();
                            handler.removeCallbacks(runnable);
                            ScreenshotHelper.this.notifyScreenshotError();
                        }
                    }
                }
            };
            if (this.mContext.bindServiceAsUser(intent, serviceConnection, InputDevice.SOURCE_HDMI, UserHandle.CURRENT)) {
                this.mScreenshotConnection = serviceConnection;
                handler.postDelayed(runnable, JobInfo.MIN_BACKOFF_MILLIS);
            }
        }
    }

    private void resetConnection() {
        if (this.mScreenshotConnection != null) {
            this.mContext.unbindService(this.mScreenshotConnection);
            this.mScreenshotConnection = null;
        }
    }

    private void notifyScreenshotError() {
        ComponentName componentName = new ComponentName(SYSUI_PACKAGE, SYSUI_SCREENSHOT_ERROR_RECEIVER);
        Intent intent = new Intent(Intent.ACTION_USER_PRESENT);
        intent.setComponent(componentName);
        intent.addFlags(335544320);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }
}
