package com.android.phone;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import java.util.ArrayList;

public class MMIDialogActivity extends Activity {
    private static final String TAG = MMIDialogActivity.class.getSimpleName();
    private CallManager mCM = PhoneGlobals.getInstance().getCallManager();
    private Handler mHandler;
    private Dialog mMMIDialog;
    private Phone mPhone;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        int intExtra = getIntent().getIntExtra("subscription", Integer.MAX_VALUE);
        this.mPhone = PhoneGlobals.getPhone(intExtra);
        if (this.mPhone == null) {
            Log.w(TAG, "onCreate: invalid subscription id (" + intExtra + ") lead to null Phone.");
            finish();
            return;
        }
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 52:
                        MMIDialogActivity.this.onMMIComplete((MmiCode) ((AsyncResult) message.obj).result);
                        break;
                    case 53:
                        MMIDialogActivity.this.onMMICancel();
                        break;
                }
            }
        };
        Log.d(TAG, "onCreate; registering for mmi complete.");
        this.mCM.registerForMmiComplete(this.mHandler, 52, (Object) null);
        if (this.mPhone.getState() == PhoneConstants.State.OFFHOOK) {
            Toast.makeText(this, R.string.incall_status_dialed_mmi, 0).show();
        }
        showMMIDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mMMIDialog != null) {
            this.mMMIDialog.dismiss();
            this.mMMIDialog = null;
        }
        if (this.mHandler != null) {
            this.mCM.unregisterForMmiComplete(this.mHandler);
            this.mHandler = null;
        }
    }

    private void showMMIDialog() {
        ArrayList arrayList = new ArrayList(this.mPhone.getPendingMmiCodes());
        if (this.mPhone.getImsPhone() != null) {
            arrayList.addAll(this.mPhone.getImsPhone().getPendingMmiCodes());
        }
        if (arrayList.size() > 0) {
            MmiCode mmiCode = (MmiCode) arrayList.get(0);
            Message messageObtain = Message.obtain(this.mHandler, 53);
            Log.d(TAG, "showMMIDialog: mmiCode = " + mmiCode);
            this.mMMIDialog = PhoneUtils.displayMMIInitiate(this, mmiCode, messageObtain, this.mMMIDialog);
            return;
        }
        Log.d(TAG, "showMMIDialog: no pending MMIs; finishing");
        finish();
    }

    private void onMMIComplete(MmiCode mmiCode) {
        Log.d(TAG, "onMMIComplete: mmi=" + mmiCode);
        int phoneType = this.mPhone.getPhoneType();
        if (phoneType == 2) {
            PhoneUtils.displayMMIComplete(this.mPhone, this, mmiCode, null, null);
            return;
        }
        if (phoneType == 1) {
            if (mmiCode.getState() != MmiCode.State.PENDING) {
                Log.d(TAG, "onMMIComplete: Got MMI_COMPLETE, finishing dialog activity...");
                dismissDialogsAndFinish();
            } else {
                Log.d(TAG, "onMMIComplete: still pending.");
            }
        }
    }

    private void onMMICancel() {
        Log.v(TAG, "onMMICancel()...");
        PhoneUtils.cancelMmiCode(this.mPhone);
        Log.d(TAG, "onMMICancel: finishing MMI dialog...");
        dismissDialogsAndFinish();
    }

    private void dismissDialogsAndFinish() {
        if (this.mMMIDialog != null) {
            this.mMMIDialog.dismiss();
            this.mMMIDialog = null;
        }
        if (this.mHandler != null) {
            this.mCM.unregisterForMmiComplete(this.mHandler);
            this.mHandler = null;
        }
        Log.v(TAG, "dismissDialogsAndFinish");
        finish();
    }
}
