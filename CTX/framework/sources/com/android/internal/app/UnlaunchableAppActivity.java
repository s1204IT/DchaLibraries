package com.android.internal.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import com.android.internal.R;

public class UnlaunchableAppActivity extends Activity implements DialogInterface.OnDismissListener, DialogInterface.OnClickListener {
    private static final String EXTRA_UNLAUNCHABLE_REASON = "unlaunchable_reason";
    private static final String TAG = "UnlaunchableAppActivity";
    private static final int UNLAUNCHABLE_REASON_QUIET_MODE = 1;
    private int mReason;
    private IntentSender mTarget;
    private int mUserId;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        Intent intent = getIntent();
        this.mReason = intent.getIntExtra(EXTRA_UNLAUNCHABLE_REASON, -1);
        this.mUserId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -10000);
        this.mTarget = (IntentSender) intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (this.mUserId != -10000) {
            if (this.mReason == 1) {
                AlertDialog.Builder onDismissListener = new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.work_mode_off_title)).setMessage(getResources().getString(R.string.work_mode_off_message)).setOnDismissListener(this);
                if (this.mReason == 1) {
                    onDismissListener.setPositiveButton(R.string.work_mode_turn_on, this).setNegativeButton(17039360, (DialogInterface.OnClickListener) null);
                } else {
                    onDismissListener.setPositiveButton(17039370, (DialogInterface.OnClickListener) null);
                }
                onDismissListener.show();
                return;
            }
            Log.wtf(TAG, "Invalid unlaunchable type: " + this.mReason);
            finish();
            return;
        }
        Log.wtf(TAG, "Invalid user id: " + this.mUserId + ". Stopping.");
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        finish();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (this.mReason == 1 && i == -1) {
            UserManager.get(this).requestQuietModeEnabled(false, UserHandle.of(this.mUserId), this.mTarget);
        }
    }

    private static final Intent createBaseIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(ZenModeConfig.SYSTEM_AUTHORITY, UnlaunchableAppActivity.class.getName()));
        intent.setFlags(276824064);
        return intent;
    }

    public static Intent createInQuietModeDialogIntent(int i) {
        Intent intentCreateBaseIntent = createBaseIntent();
        intentCreateBaseIntent.putExtra(EXTRA_UNLAUNCHABLE_REASON, 1);
        intentCreateBaseIntent.putExtra(Intent.EXTRA_USER_HANDLE, i);
        return intentCreateBaseIntent;
    }

    public static Intent createInQuietModeDialogIntent(int i, IntentSender intentSender) {
        Intent intentCreateInQuietModeDialogIntent = createInQuietModeDialogIntent(i);
        intentCreateInQuietModeDialogIntent.putExtra(Intent.EXTRA_INTENT, intentSender);
        return intentCreateInQuietModeDialogIntent;
    }
}
