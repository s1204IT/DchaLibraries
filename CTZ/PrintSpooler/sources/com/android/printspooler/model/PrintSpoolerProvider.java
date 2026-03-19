package com.android.printspooler.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.android.printspooler.model.PrintSpoolerService;

public class PrintSpoolerProvider implements ServiceConnection {
    private final Runnable mCallback;
    private final Context mContext;
    private PrintSpoolerService mSpooler;

    public PrintSpoolerProvider(Context context, Runnable runnable) {
        this.mContext = context;
        this.mCallback = runnable;
        this.mContext.bindService(new Intent(this.mContext, (Class<?>) PrintSpoolerService.class), this, 1);
    }

    public PrintSpoolerService getSpooler() {
        return this.mSpooler;
    }

    public void destroy() {
        if (this.mSpooler != null) {
            this.mContext.unbindService(this);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        this.mSpooler = ((PrintSpoolerService.PrintSpooler) iBinder).getService();
        if (this.mSpooler != null) {
            this.mCallback.run();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }
}
