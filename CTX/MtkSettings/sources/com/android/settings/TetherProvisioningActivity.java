package com.android.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.UserHandle;
import android.util.Log;

public class TetherProvisioningActivity extends Activity {
    private static final boolean DEBUG = Log.isLoggable("TetherProvisioningAct", 3);
    private ResultReceiver mResultReceiver;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mResultReceiver = (ResultReceiver) getIntent().getParcelableExtra("extraProvisionCallback");
        int intExtra = getIntent().getIntExtra("extraAddTetherType", -1);
        String[] stringArray = getResources().getStringArray(android.R.array.config_cell_retries_per_error_code);
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClassName(stringArray[0], stringArray[1]);
        intent.putExtra("TETHER_TYPE", intExtra);
        if (DEBUG) {
            Log.d("TetherProvisioningAct", "Starting provisioning app: " + stringArray[0] + "." + stringArray[1]);
        }
        if (!getPackageManager().queryIntentActivities(intent, 65536).isEmpty()) {
            startActivityForResultAsUser(intent, 0, UserHandle.CURRENT);
            return;
        }
        Log.e("TetherProvisioningAct", "Provisioning app is configured, but not available.");
        this.mResultReceiver.send(11, null);
        finish();
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        int i3;
        super.onActivityResult(i, i2, intent);
        if (i == 0) {
            if (DEBUG) {
                Log.d("TetherProvisioningAct", "Got result from app: " + i2);
            }
            if (i2 == -1) {
                i3 = 0;
            } else {
                i3 = 11;
            }
            this.mResultReceiver.send(i3, null);
            finish();
        }
    }
}
