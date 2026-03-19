package com.android.server.telecom.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.server.telecom.R;

public class CallBlockDisabledActivity extends Activity {
    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        showCallBlockingOffDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissCallBlockingOffDialog();
    }

    private void showCallBlockingOffDialog() {
        if (isShowingCallBlockingOffDialog()) {
            return;
        }
        this.mDialog = new AlertDialog.Builder(this).setTitle(R.string.phone_strings_emergency_call_made_dialog_title_txt).setMessage(R.string.phone_strings_emergency_call_made_dialog_call_blocking_text_txt).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                BlockedNumbersUtil.setEnhancedBlockSetting(CallBlockDisabledActivity.this, "show_emergency_call_notification", false);
                BlockedNumbersUtil.updateEmergencyCallNotification(CallBlockDisabledActivity.this, false);
                CallBlockDisabledActivity.this.finish();
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                CallBlockDisabledActivity.this.finish();
            }
        }).create();
        this.mDialog.setCanceledOnTouchOutside(false);
        this.mDialog.show();
    }

    private void dismissCallBlockingOffDialog() {
        if (isShowingCallBlockingOffDialog()) {
            this.mDialog.dismiss();
        }
        this.mDialog = null;
    }

    private boolean isShowingCallBlockingOffDialog() {
        return this.mDialog != null && this.mDialog.isShowing();
    }
}
