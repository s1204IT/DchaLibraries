package com.mediatek.services.telephony;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.phone.R;

public class RoamingAlertDialog extends Activity {
    private AlertDialog mAlertDialog = null;
    private int mRoamingType;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        showAlertDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("In onPause(): mAlertDialog = " + this.mAlertDialog);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("In onResume(): mAlertDialog = " + this.mAlertDialog);
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("In onStop(): mAlertDialog = " + this.mAlertDialog);
    }

    @Override
    protected void onDestroy() {
        log("In onDestroy()");
        onDialogDismissed();
        super.onDestroy();
    }

    private void showAlertDialog() {
        int i;
        this.mRoamingType = getIntent().getIntExtra("Roaming type", 0);
        log("In showAlertDialog(): mRoamingType =" + this.mRoamingType);
        if (2 == this.mRoamingType) {
            i = R.string.roaming_network;
        } else if (3 == this.mRoamingType) {
            i = R.string.international_roaming_network;
        } else {
            log("Not correct roaming type, so return, mRoamingType = " + this.mRoamingType);
            return;
        }
        this.mAlertDialog = new AlertDialog.Builder(this).setTitle(android.R.string.dialog_alert_title).setIcon(android.R.drawable.ic_dialog_alert).setMessage(i).setPositiveButton(R.string.modify_roaming_settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                RoamingAlertDialog.this.log("positive button clicked, show roaming settings");
                RoamingAlertDialog.this.showRoamingSettings();
                RoamingAlertDialog.this.onDialogDismissed();
                RoamingAlertDialog.this.finish();
            }
        }).setNegativeButton(R.string.continue_roaming, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                RoamingAlertDialog.this.log("negative button clicked, let data continue in roaming");
                RoamingAlertDialog.this.onDialogDismissed();
                RoamingAlertDialog.this.finish();
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                RoamingAlertDialog.this.onDialogDismissed();
                RoamingAlertDialog.this.finish();
            }
        }).create();
        this.mAlertDialog.setCanceledOnTouchOutside(false);
        this.mAlertDialog.getWindow().addFlags(2);
        this.mAlertDialog.show();
    }

    private void log(String str) {
        Log.d("RoamingAlertDialog", str);
    }

    private void onDialogDismissed() {
        if (this.mAlertDialog != null) {
            this.mAlertDialog.dismiss();
            this.mAlertDialog = null;
        }
    }

    private void showRoamingSettings() {
        Intent intent = new Intent();
        intent.setAction("com.mediatek.services.telephony.ACTION_SHOW_ROAMING_SETTINGS");
        startActivity(intent);
    }
}
