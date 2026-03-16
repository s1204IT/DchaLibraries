package com.verizon.vzwcommonserviceinterface;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import java.util.Timer;
import java.util.TimerTask;

public class CommonInterface {
    static final boolean $assertionsDisabled;
    private static final String TAG = "VZWCSI";
    private static final String lock = "";
    private Context context;
    private Intent intent;
    private Bundle response;
    private Handler responseHandler;

    static {
        $assertionsDisabled = !CommonInterface.class.desiredAssertionStatus();
    }

    private CommonInterface() {
    }

    public CommonInterface(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
    }

    public Bundle sendCommand(Handler responseHandler) {
        PendingIntent pendingIntent = PendingIntent.getService(this.context, 0, new Intent(""), 0);
        this.responseHandler = responseHandler;
        boolean synchronous = responseHandler == null;
        Log.v(TAG, "synchronous = " + synchronous);
        IPC ipc = new IPC(pendingIntent, synchronous);
        ipc.start();
        if (synchronous) {
            synchronized ("") {
                try {
                    "".wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return this.response;
    }

    private class IPC extends Thread implements Handler.Callback {
        private final PendingIntent pendingIntent;
        private boolean synchronous;
        private Timer timer;

        public IPC(PendingIntent pendingIntent, boolean synchronous) {
            this.pendingIntent = pendingIntent;
            this.synchronous = synchronous;
        }

        @Override
        public void run() {
            Looper.prepare();
            Messenger messenger = new Messenger(new Handler(this));
            CommonInterface.this.intent.putExtra("messenger", messenger);
            CommonInterface.this.intent.putExtra("intent", this.pendingIntent);
            this.timer = new Timer();
            this.timer.schedule(CommonInterface.this.new NoResponse(messenger), 20000L);
            CommonInterface.this.context.startService(CommonInterface.this.intent);
            Log.v(CommonInterface.TAG, "Service started");
            Looper.loop();
        }

        @Override
        public boolean handleMessage(Message message) {
            CommonInterface.this.response = (Bundle) message.obj;
            this.timer.cancel();
            this.timer.purge();
            Looper looper = Looper.myLooper();
            if (looper != null) {
                looper.quit();
            }
            if (this.synchronous) {
                synchronized ("") {
                    "".notifyAll();
                }
            } else {
                Message newMessage = Message.obtain();
                newMessage.copyFrom(message);
                CommonInterface.this.responseHandler.sendMessage(newMessage);
            }
            interrupt();
            return true;
        }
    }

    public String getPakageName() {
        if (this.intent == null) {
            return null;
        }
        PendingIntent pendingIntent = (PendingIntent) this.intent.getParcelableExtra("intent");
        Log.v(TAG, "build version =" + Build.VERSION.SDK_INT);
        if (pendingIntent == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= 17) {
            Log.v(TAG, "creator package =" + pendingIntent.getCreatorPackage());
            return pendingIntent.getCreatorPackage();
        }
        Log.v(TAG, "target package =" + pendingIntent.getTargetPackage());
        return pendingIntent.getTargetPackage();
    }

    public void sendResponse(Bundle bundle) throws RemoteException {
        if (!$assertionsDisabled && bundle == null) {
            throw new AssertionError("Bundle cannot be null");
        }
        Messenger messenger = (Messenger) this.intent.getParcelableExtra("messenger");
        if (messenger != null) {
            messenger.send(Message.obtain(null, 0, bundle));
        }
    }

    private class NoResponse extends TimerTask {
        private Messenger messenger;

        public NoResponse(Messenger messenger) {
            this.messenger = messenger;
        }

        @Override
        public void run() {
            try {
                this.messenger.send(Message.obtain());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
