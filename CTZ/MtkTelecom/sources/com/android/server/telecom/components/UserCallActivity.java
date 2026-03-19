package com.android.server.telecom.components;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.telecom.Log;
import com.mediatek.server.telecom.MtkUtil;

public class UserCallActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PowerManager.WakeLock wakeLockNewWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(1, "UserCallActivity");
        wakeLockNewWakeLock.acquire();
        Log.startSession("UCA.oC");
        try {
            Intent intent = getIntent();
            verifyCallAction(intent);
            if (MtkUtil.isConferenceInvitation(intent.getExtras())) {
                Log.w(this, "[onCreate] Unsupported conference invitation via startActivity", new Object[0]);
                finish();
                return;
            }
            new UserCallIntentProcessor(this, new UserHandle(((UserManager) getSystemService("user")).getUserHandle())).processIntent(new Intent(intent), getCallingPackage(), true, false);
            Log.endSession();
            wakeLockNewWakeLock.release();
            Log.i(this, "onCreate done", new Object[0]);
            finish();
        } finally {
            Log.endSession();
            wakeLockNewWakeLock.release();
        }
    }

    private void verifyCallAction(Intent intent) {
        if (getClass().getName().equals(intent.getComponent().getClassName()) && !"android.intent.action.CALL".equals(intent.getAction())) {
            Log.w(this, "Attempt to deliver non-CALL action; forcing to CALL", new Object[0]);
            intent.setAction("android.intent.action.CALL");
        }
    }
}
