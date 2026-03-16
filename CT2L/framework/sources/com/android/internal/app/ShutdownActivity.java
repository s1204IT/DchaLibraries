package com.android.internal.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;

public class ShutdownActivity extends Activity {
    private static final String TAG = "ShutdownActivity";
    private boolean mConfirm;
    private boolean mReboot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        this.mReboot = Intent.ACTION_REBOOT.equals(intent.getAction());
        this.mConfirm = intent.getBooleanExtra(Intent.EXTRA_KEY_CONFIRM, false);
        Slog.i(TAG, "onCreate(): confirm=" + this.mConfirm);
        Thread thr = new Thread(TAG) {
            @Override
            public void run() {
                IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE));
                try {
                    if (ShutdownActivity.this.mReboot) {
                        pm.reboot(ShutdownActivity.this.mConfirm, null, false);
                    } else {
                        pm.shutdown(ShutdownActivity.this.mConfirm, false);
                    }
                } catch (RemoteException e) {
                }
            }
        };
        thr.start();
        finish();
        try {
            thr.join();
        } catch (InterruptedException e) {
        }
    }
}
