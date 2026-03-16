package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import com.android.phone.HfaLogic;

public class HfaActivity extends Activity {
    private static final String TAG = HfaActivity.class.getSimpleName();
    private AlertDialog mDialog;
    private HfaLogic mHfaLogic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        PendingIntent otaResponseIntent = (PendingIntent) getIntent().getParcelableExtra("otasp_result_code_pending_intent");
        this.mHfaLogic = new HfaLogic(getApplicationContext(), new HfaLogic.HfaLogicCallback() {
            @Override
            public void onSuccess() {
                HfaActivity.this.onHfaSuccess();
            }

            @Override
            public void onError(String error) {
                HfaActivity.this.onHfaError(error);
            }
        }, otaResponseIntent);
        startProvisioning();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
    }

    private void startProvisioning() {
        buildAndShowDialog();
        this.mHfaLogic.start();
    }

    private void buildAndShowDialog() {
        this.mDialog = new AlertDialog.Builder(this, 5).setTitle(R.string.ota_hfa_activation_title).setMessage(R.string.ota_hfa_activation_dialog_message).setPositiveButton(R.string.ota_skip_activation_dialog_skip_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface di, int which) {
                HfaActivity.this.onUserSkip();
            }
        }).create();
        this.mDialog.setCanceledOnTouchOutside(false);
        this.mDialog.setCancelable(false);
        Log.i(TAG, "showing dialog");
        this.mDialog.show();
    }

    private void onHfaError(String errorMsg) {
        this.mDialog.dismiss();
        AlertDialog errorDialog = new AlertDialog.Builder(this, 5).setMessage(errorMsg).setPositiveButton(R.string.ota_skip_activation_dialog_skip_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface di, int which) {
                di.dismiss();
                HfaActivity.this.onUserSkip();
            }
        }).setNegativeButton(R.string.ota_try_again, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface di, int which) {
                di.dismiss();
                HfaActivity.this.startProvisioning();
            }
        }).create();
        errorDialog.show();
    }

    private void onHfaSuccess() {
        finish();
    }

    private void onUserSkip() {
        finish();
    }
}
