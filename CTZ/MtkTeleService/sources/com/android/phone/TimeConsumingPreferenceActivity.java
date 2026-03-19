package com.android.phone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.CommandException;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.cdma.CdmaCallWaitingUtOptions;
import java.util.ArrayList;
import java.util.Iterator;

public class TimeConsumingPreferenceActivity extends PreferenceActivity implements DialogInterface.OnCancelListener, TimeConsumingPreferenceListener {
    private static final int BUSY_READING_DIALOG = 100;
    private static final int BUSY_SAVING_DIALOG = 200;
    public static final int EXCEPTION_ERROR = 300;
    public static final int FDN_CHECK_FAILURE = 600;
    private static final String LOG_TAG = "TimeConsumingPreferenceActivity";
    public static final int PASSWORD_ERROR = 10000;
    public static final int RADIO_OFF_ERROR = 500;
    public static final int RESPONSE_ERROR = 400;
    public static final int STK_CC_SS_TO_DIAL_ERROR = 700;
    public static final int STK_CC_SS_TO_DIAL_VIDEO_ERROR = 1000;
    public static final int STK_CC_SS_TO_SS_ERROR = 900;
    public static final int STK_CC_SS_TO_USSD_ERROR = 800;
    private final DialogInterface.OnClickListener mDismiss;
    private final DialogInterface.OnClickListener mDismissAndFinish;
    private final boolean DBG = true;
    private final ArrayList<String> mBusyList = new ArrayList<>();
    protected boolean mIsForeground = false;
    private CharSequence mTitle = null;

    public TimeConsumingPreferenceActivity() {
        this.mDismiss = new DismissOnClickListener();
        this.mDismissAndFinish = new DismissAndFinishOnClickListener();
    }

    private class DismissOnClickListener implements DialogInterface.OnClickListener {
        private DismissOnClickListener() {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
        }
    }

    private class DismissAndFinishOnClickListener implements DialogInterface.OnClickListener {
        private DismissAndFinishOnClickListener() {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
            TimeConsumingPreferenceActivity.this.finish();
        }
    }

    @Override
    protected Dialog onCreateDialog(int i) {
        int i2;
        if (i == 100 || i == BUSY_SAVING_DIALOG) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            if (!TextUtils.isEmpty(this.mTitle)) {
                Log.d(LOG_TAG, "onCreateDialog, use customer's dialog title:" + ((Object) this.mTitle));
                progressDialog.setTitle(this.mTitle);
            } else {
                progressDialog.setTitle(getText(R.string.updating_title));
            }
            progressDialog.setIndeterminate(true);
            progressDialog.setCanceledOnTouchOutside(false);
            if (i == 100) {
                progressDialog.setCancelable(true);
                progressDialog.setOnCancelListener(this);
                progressDialog.setMessage(getText(R.string.reading_settings));
                return progressDialog;
            }
            if (i != BUSY_SAVING_DIALOG) {
                return null;
            }
            progressDialog.setCancelable(false);
            progressDialog.setMessage(getText(R.string.updating_settings));
            return progressDialog;
        }
        if (i != 400 && i != 500 && i != 300 && i != 600 && i != 700 && i != 800 && i != 900 && i != 1000 && i != 10000) {
            return null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (i == 400) {
            i2 = R.string.response_error;
            builder.setPositiveButton(R.string.close_dialog, this.mDismiss);
        } else if (i == 500) {
            i2 = R.string.radio_off_error;
            builder.setPositiveButton(R.string.close_dialog, this.mDismissAndFinish);
        } else if (i == 600) {
            i2 = R.string.fdn_check_failure;
            builder.setPositiveButton(R.string.close_dialog, this.mDismiss);
        } else if (i == 700) {
            i2 = R.string.stk_cc_ss_to_dial_error;
            builder.setPositiveButton(R.string.close_dialog, this.mDismiss);
        } else if (i == 800) {
            i2 = R.string.stk_cc_ss_to_ussd_error;
            builder.setPositiveButton(R.string.close_dialog, this.mDismiss);
        } else if (i == 900) {
            i2 = R.string.stk_cc_ss_to_ss_error;
            builder.setPositiveButton(R.string.close_dialog, this.mDismiss);
        } else if (i != 1000) {
            if (i == 10000) {
                i2 = android.R.string.factorytest_not_system;
                builder.setNeutralButton(R.string.close_dialog, this.mDismiss);
            } else {
                i2 = R.string.exception_error;
                builder.setPositiveButton(R.string.close_dialog, this.mDismiss);
            }
        } else {
            i2 = R.string.stk_cc_ss_to_dial_video_error;
            builder.setPositiveButton(R.string.close_dialog, this.mDismiss);
        }
        builder.setTitle(getText(R.string.error_updating_title));
        builder.setMessage(getText(i2));
        builder.setCancelable(false);
        AlertDialog alertDialogCreate = builder.create();
        alertDialogCreate.getWindow().addFlags(4);
        if (this instanceof CdmaCallWaitingUtOptions) {
            alertDialogCreate.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    Log.d(TimeConsumingPreferenceActivity.LOG_TAG, "CdmaCallWaitingUtOptions error, dismiss...");
                    TimeConsumingPreferenceActivity.this.finish();
                }
            });
        }
        return alertDialogCreate;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mIsForeground = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mIsForeground = false;
    }

    @Override
    public void onStarted(Preference preference, boolean z) {
        dumpState();
        Log.i(LOG_TAG, "onStarted, preference=" + preference.getKey() + ", reading=" + z);
        this.mBusyList.add(preference.getKey());
        if (this.mIsForeground) {
            if (z) {
                showDialog(100);
            } else {
                showDialog(BUSY_SAVING_DIALOG);
            }
        }
    }

    public void onFinished(Preference preference, boolean z) {
        dumpState();
        Log.i(LOG_TAG, "onFinished, preference=" + preference.getKey() + ", reading=" + z);
        this.mBusyList.remove(preference.getKey());
        if (this.mBusyList.isEmpty()) {
            if (z) {
                dismissDialogSafely(100);
            } else {
                dismissDialogSafely(BUSY_SAVING_DIALOG);
            }
        }
        preference.setEnabled(true);
    }

    @Override
    public void onError(Preference preference, int i) {
        dumpState();
        Log.i(LOG_TAG, "onError, preference=" + preference.getKey() + ", error=" + i);
        if (this.mIsForeground) {
            showDialog(i);
        }
        preference.setEnabled(false);
        ExtensionManager.getCallFeaturesSettingExt().onError(preference);
    }

    @Override
    public void onException(Preference preference, CommandException commandException) {
        if (commandException.getCommandError() == CommandException.Error.FDN_CHECK_FAILURE) {
            onError(preference, FDN_CHECK_FAILURE);
            return;
        }
        if (commandException.getCommandError() == CommandException.Error.RADIO_NOT_AVAILABLE) {
            onError(preference, RADIO_OFF_ERROR);
            return;
        }
        if (commandException.getCommandError() == CommandException.Error.SS_MODIFIED_TO_DIAL) {
            onError(preference, STK_CC_SS_TO_DIAL_ERROR);
            return;
        }
        if (commandException.getCommandError() == CommandException.Error.SS_MODIFIED_TO_DIAL_VIDEO) {
            onError(preference, STK_CC_SS_TO_DIAL_VIDEO_ERROR);
            return;
        }
        if (commandException.getCommandError() == CommandException.Error.SS_MODIFIED_TO_USSD) {
            onError(preference, STK_CC_SS_TO_USSD_ERROR);
            return;
        }
        if (commandException.getCommandError() == CommandException.Error.SS_MODIFIED_TO_SS) {
            onError(preference, STK_CC_SS_TO_SS_ERROR);
        } else if (commandException.getCommandError() == CommandException.Error.PASSWORD_INCORRECT) {
            onError(preference, PASSWORD_ERROR);
        } else {
            preference.setEnabled(false);
            onError(preference, EXCEPTION_ERROR);
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        dumpState();
        finish();
    }

    private void dismissDialogSafely(int i) {
        try {
            dismissDialog(i);
        } catch (IllegalArgumentException e) {
        }
    }

    void dumpState() {
        Log.d(LOG_TAG, "dumpState begin");
        Iterator<String> it = this.mBusyList.iterator();
        while (it.hasNext()) {
            Log.d(LOG_TAG, "mBusyList: key=" + it.next());
        }
        Log.d(LOG_TAG, "dumpState end");
    }

    public void setDialogTitle(CharSequence charSequence) {
        this.mTitle = charSequence;
    }
}
