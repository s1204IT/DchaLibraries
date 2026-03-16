package com.android.contacts.common.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;

public class IndeterminateProgressDialog extends DialogFragment {
    private static final String TAG = IndeterminateProgressDialog.class.getSimpleName();
    private boolean mAllowStateLoss;
    private CharSequence mMessage;
    private long mMinDisplayTime;
    private Dialog mOldDialog;
    private CharSequence mTitle;
    private long mShowTime = 0;
    private boolean mActivityReady = false;
    private final Handler mHandler = new Handler();
    private boolean mCalledSuperDismiss = false;
    private final Runnable mDismisser = new Runnable() {
        @Override
        public void run() {
            IndeterminateProgressDialog.this.superDismiss();
        }
    };

    public static IndeterminateProgressDialog show(FragmentManager fragmentManager, CharSequence title, CharSequence message, long minDisplayTime) {
        IndeterminateProgressDialog dialogFragment = new IndeterminateProgressDialog();
        dialogFragment.mTitle = title;
        dialogFragment.mMessage = message;
        dialogFragment.mMinDisplayTime = minDisplayTime;
        dialogFragment.show(fragmentManager, TAG);
        dialogFragment.mShowTime = System.currentTimeMillis();
        dialogFragment.setCancelable(false);
        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setIndeterminateDrawable(null);
        dialog.setTitle(this.mTitle);
        dialog.setMessage(this.mMessage);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mActivityReady = true;
        if (this.mCalledSuperDismiss) {
            superDismiss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mActivityReady = false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (this.mOldDialog == null || this.mOldDialog != dialog) {
            super.onDismiss(dialog);
        }
    }

    @Override
    public void onDestroyView() {
        this.mOldDialog = getDialog();
        super.onDestroyView();
    }

    @Override
    public void dismiss() {
        this.mAllowStateLoss = false;
        dismissWhenReady();
    }

    @Override
    public void dismissAllowingStateLoss() {
        this.mAllowStateLoss = true;
        dismissWhenReady();
    }

    private void dismissWhenReady() {
        long shownTime = System.currentTimeMillis() - this.mShowTime;
        if (shownTime >= this.mMinDisplayTime) {
            this.mHandler.post(this.mDismisser);
        } else {
            long sleepTime = this.mMinDisplayTime - shownTime;
            this.mHandler.postDelayed(this.mDismisser, sleepTime);
        }
    }

    private void superDismiss() {
        this.mCalledSuperDismiss = true;
        if (this.mActivityReady) {
            if (this.mAllowStateLoss) {
                super.dismissAllowingStateLoss();
            } else {
                super.dismiss();
            }
        }
    }
}
