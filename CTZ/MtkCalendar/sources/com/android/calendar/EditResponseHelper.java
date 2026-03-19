package com.android.calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class EditResponseHelper implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private AlertDialog mAlertDialog;
    private DialogInterface.OnClickListener mDialogListener;
    private DialogInterface.OnDismissListener mDismissListener;
    private final Activity mParent;
    private int mWhichEvents = -1;
    private boolean mClickedOk = false;
    private DialogInterface.OnClickListener mListListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            EditResponseHelper.this.mWhichEvents = i;
            EditResponseHelper.this.mAlertDialog.getButton(-1).setEnabled(true);
        }
    };

    public EditResponseHelper(Activity activity) {
        this.mParent = activity;
    }

    public int getWhichEvents() {
        return this.mWhichEvents;
    }

    public void setWhichEvents(int i) {
        this.mWhichEvents = i;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        setClickedOk(true);
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        if (!getClickedOk()) {
            setWhichEvents(-1);
        }
        setClickedOk(false);
        if (this.mDismissListener != null) {
            this.mDismissListener.onDismiss(dialogInterface);
        }
    }

    private boolean getClickedOk() {
        return this.mClickedOk;
    }

    private void setClickedOk(boolean z) {
        this.mClickedOk = z;
    }

    public void setDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.mDismissListener = onDismissListener;
    }

    public void showDialog(int i) {
        if (this.mDialogListener == null) {
            this.mDialogListener = this;
        }
        AlertDialog alertDialogShow = new AlertDialog.Builder(this.mParent).setTitle(R.string.change_response_title).setIconAttribute(android.R.attr.alertDialogIcon).setSingleChoiceItems(R.array.change_response_labels, i, this.mListListener).setPositiveButton(android.R.string.ok, this.mDialogListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
        alertDialogShow.setOnDismissListener(this);
        this.mAlertDialog = alertDialogShow;
        if (i == -1) {
            alertDialogShow.getButton(-1).setEnabled(false);
        }
    }

    public void dismissAlertDialog() {
        if (this.mAlertDialog != null) {
            this.mAlertDialog.dismiss();
        }
    }
}
