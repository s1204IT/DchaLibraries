package com.mediatek.settings.cdma;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.android.phone.TimeConsumingPreferenceActivity;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.settings.CallSettingUtils;

public class CdmaCallWaitingUtOptions extends TimeConsumingPreferenceActivity implements PhoneGlobals.SubInfoUpdateListener {
    private Dialog mDialog;
    private Preference mDummyPreference;
    private final MyHandler mHandler = new MyHandler();
    private Phone mPhone;
    private RadioGroup mRadioGroup;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
        this.mDummyPreference = new Preference(this);
        this.mDummyPreference.setKey("dummy_preference_key");
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mDialog = createDialog();
        this.mDialog.show();
        initForUt();
    }

    private void initForUt() {
        Log.d("CdmaCallWaitingUtOptions", "init...");
        onStarted(this.mDummyPreference, true);
        this.mPhone.getCallWaiting(this.mHandler.obtainMessage(0, 0, 0));
    }

    public Dialog createDialog() {
        final Dialog dialog = new Dialog(this, R.style.CWDialogTheme);
        dialog.setContentView(R.layout.mtk_cdma_cf_dialog);
        dialog.setTitle(R.string.labelCW);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Log.d("CdmaCallWaitingUtOptions", "Dialog Cancels, so finish the activity");
                dialogInterface.dismiss();
                CdmaCallWaitingUtOptions.this.finish();
            }
        });
        final RadioGroup radioGroup = (RadioGroup) dialog.findViewById(R.id.group);
        TextView textView = (TextView) dialog.findViewById(R.id.dialog_sum);
        if (textView != null) {
            textView.setVisibility(8);
        } else {
            Log.d("CdmaCallWaitingUtOptions", "--------------[text view is null]---------------");
        }
        EditText editText = (EditText) dialog.findViewById(R.id.EditNumber);
        if (editText != null) {
            editText.setVisibility(8);
        }
        ImageButton imageButton = (ImageButton) dialog.findViewById(R.id.select_contact);
        if (imageButton != null) {
            imageButton.setVisibility(8);
        }
        Button button = (Button) dialog.findViewById(R.id.save);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean z;
                    if (radioGroup.getCheckedRadioButtonId() != -1) {
                        if (radioGroup.getCheckedRadioButtonId() != R.id.enable) {
                            z = false;
                        } else {
                            z = true;
                        }
                        CdmaCallWaitingUtOptions.this.mPhone.setCallWaiting(z, CdmaCallWaitingUtOptions.this.mHandler.obtainMessage(1));
                    }
                    dialog.dismiss();
                    CdmaCallWaitingUtOptions.this.finish();
                }
            });
        }
        Button button2 = (Button) dialog.findViewById(R.id.cancel);
        if (button2 != null) {
            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    CdmaCallWaitingUtOptions.this.finish();
                }
            });
        }
        this.mRadioGroup = radioGroup;
        return dialog;
    }

    @Override
    public void handleSubInfoUpdate() {
        Log.d("CdmaCallWaitingUtOptions", "handleSubInfoUpdate...");
        if (this.mDialog != null) {
            this.mDialog.dismiss();
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
    }

    @Override
    public void onPause() {
        Log.d("CdmaCallWaitingUtOptions", "onPause...");
        super.onPause();
        if (this.mDialog != null) {
            this.mDialog.dismiss();
        }
        finish();
    }

    private class MyHandler extends Handler {
        private MyHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    handleGetCallWaitingResponse(message);
                    break;
                case 1:
                    handleSetCallWaitingResponse(message);
                    break;
            }
        }

        private boolean isUtError(CommandException.Error error) {
            boolean z = error == CommandException.Error.OPERATION_NOT_ALLOWED || error == CommandException.Error.OEM_ERROR_2 || error == CommandException.Error.OEM_ERROR_3;
            Log.d("CdmaCallWaitingUtOptions", "Has UT Error: " + z);
            return z;
        }

        private void handleGetCallWaitingResponse(Message message) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (message.arg2 == 1) {
                CdmaCallWaitingUtOptions.this.onFinished(CdmaCallWaitingUtOptions.this.mDummyPreference, false);
            } else {
                CdmaCallWaitingUtOptions.this.onFinished(CdmaCallWaitingUtOptions.this.mDummyPreference, true);
            }
            if (asyncResult.exception instanceof CommandException) {
                Log.d("CdmaCallWaitingUtOptions", "handleGetCallWaitingResponse: CommandException=" + asyncResult.exception);
                handleCommandException((CommandException) asyncResult.exception);
                return;
            }
            if ((asyncResult.userObj instanceof Throwable) || asyncResult.exception != null) {
                Log.d("CdmaCallWaitingUtOptions", "handleGetCallWaitingResponse: Exception" + asyncResult.exception);
                if (CdmaCallWaitingUtOptions.this.mDialog != null) {
                    CdmaCallWaitingUtOptions.this.mDialog.dismiss();
                }
                CdmaCallWaitingUtOptions.this.onError(CdmaCallWaitingUtOptions.this.mDummyPreference, TimeConsumingPreferenceActivity.RESPONSE_ERROR);
                return;
            }
            Log.d("CdmaCallWaitingUtOptions", "handleGetCallWaitingResponse: CW state successfully queried.");
            int[] iArr = (int[]) asyncResult.result;
            try {
                boolean z = iArr[0] == 1 && (iArr[1] & 1) == 1;
                CdmaCallWaitingUtOptions.this.mRadioGroup.check(z ? R.id.enable : R.id.disable);
                Log.d("CdmaCallWaitingUtOptions", "handleGetCallWaitingResponse, enabled: " + z + ", cwArray[0]:cwArray[1] = " + iArr[0] + ":" + iArr[1]);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e("CdmaCallWaitingUtOptions", "handleGetCallWaitingResponse: improper result: err =" + e.getMessage());
            }
        }

        private void handleSetCallWaitingResponse(Message message) {
            final AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null) {
                Log.d("CdmaCallWaitingUtOptions", "handleSetCallWaitingResponse: ar.exception=" + asyncResult.exception);
                int checkedRadioButtonId = CdmaCallWaitingUtOptions.this.mRadioGroup.getCheckedRadioButtonId();
                int i = R.id.enable;
                boolean z = checkedRadioButtonId == R.id.enable;
                RadioGroup radioGroup = CdmaCallWaitingUtOptions.this.mRadioGroup;
                if (z) {
                    i = R.id.disable;
                }
                radioGroup.check(i);
            }
            Log.d("CdmaCallWaitingUtOptions", "handleSetCallWaitingResponse: re get start");
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d("CdmaCallWaitingUtOptions", "handleSetCallWaitingResponse: re get");
                    CdmaCallWaitingUtOptions.this.mPhone.getCallWaiting(MyHandler.this.obtainMessage(0, 1, 1, asyncResult.exception));
                }
            }, 1000L);
        }

        private void handleCommandException(CommandException commandException) {
            if (!isUtError(commandException.getCommandError())) {
                if (CdmaCallWaitingUtOptions.this.mDialog != null) {
                    CdmaCallWaitingUtOptions.this.mDialog.dismiss();
                }
                CdmaCallWaitingUtOptions.this.onException(CdmaCallWaitingUtOptions.this.mDummyPreference, commandException);
                return;
            }
            Log.d("CdmaCallWaitingUtOptions", "403 received, path to CS...");
            if (!MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(CdmaCallWaitingUtOptions.this, CdmaCallWaitingUtOptions.this.mPhone.getPhoneId()) || ((!TelephonyUtilsEx.isCapabilityPhone(CdmaCallWaitingUtOptions.this.mPhone) && !MtkImsManager.isSupportMims()) || CallSettingUtils.isCtVolteMix4gSim(CdmaCallWaitingUtOptions.this.mPhone.getSubId()))) {
                if (CdmaCallWaitingUtOptions.this.mDialog != null) {
                    CdmaCallWaitingUtOptions.this.mDialog.dismiss();
                }
                CdmaCallWaitOptions cdmaCallWaitOptions = new CdmaCallWaitOptions(CdmaCallWaitingUtOptions.this, CdmaCallWaitingUtOptions.this.mPhone);
                CdmaCallWaitingUtOptions.this.mDialog = cdmaCallWaitOptions.createDialog();
                CdmaCallWaitingUtOptions.this.mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        CdmaCallWaitingUtOptions.this.finish();
                    }
                });
                if (!CdmaCallWaitingUtOptions.this.isFinishing()) {
                    CdmaCallWaitingUtOptions.this.mDialog.show();
                    return;
                }
                return;
            }
            Log.d("CdmaCallWaitingUtOptions", "volte enabled, show alert...");
            if (CdmaCallWaitingUtOptions.this.mDialog != null) {
                CdmaCallWaitingUtOptions.this.mDialog.dismiss();
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(CdmaCallWaitingUtOptions.this);
            builder.setMessage(R.string.alert_turn_off_volte);
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (CdmaCallWaitingUtOptions.this.mDialog != null) {
                        CdmaCallWaitingUtOptions.this.mDialog.dismiss();
                    }
                    CdmaCallWaitingUtOptions.this.finish();
                }
            });
            AlertDialog alertDialogCreate = builder.create();
            alertDialogCreate.getWindow().addFlags(4);
            if (!CdmaCallWaitingUtOptions.this.isFinishing()) {
                alertDialogCreate.show();
            }
            CdmaCallWaitingUtOptions.this.mDialog = alertDialogCreate;
        }
    }
}
