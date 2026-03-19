package com.android.internal.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;

public class ShutdownActivity extends Activity {
    private static final String TAG = "ShutdownActivity";
    private boolean mConfirm;
    private boolean mReboot;
    private boolean mUserRequested;

    @Override
    protected void onCreate(Bundle bundle) {
        final String stringExtra;
        super.onCreate(bundle);
        Intent intent = getIntent();
        this.mReboot = Intent.ACTION_REBOOT.equals(intent.getAction());
        this.mConfirm = intent.getBooleanExtra(Intent.EXTRA_KEY_CONFIRM, false);
        this.mUserRequested = intent.getBooleanExtra(Intent.EXTRA_USER_REQUESTED_SHUTDOWN, false);
        if (this.mUserRequested) {
            stringExtra = PowerManager.SHUTDOWN_USER_REQUESTED;
        } else {
            stringExtra = intent.getStringExtra(Intent.EXTRA_REASON);
        }
        Slog.i(TAG, "onCreate(): confirm=" + this.mConfirm);
        Thread thread = new Thread(TAG) {
            @Override
            public void run() {
                IPowerManager iPowerManagerAsInterface = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE));
                try {
                    if (ShutdownActivity.this.mReboot) {
                        iPowerManagerAsInterface.reboot(ShutdownActivity.this.mConfirm, null, false);
                    } else {
                        iPowerManagerAsInterface.shutdown(ShutdownActivity.this.mConfirm, stringExtra, false);
                    }
                } catch (RemoteException e) {
                }
            }
        };
        thread.start();
        finish();
        try {
            thread.join();
        } catch (InterruptedException e) {
        }
    }
}
