package com.android.calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Button;

public class EditResponseHelper implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private AlertDialog mAlertDialog;
    private DialogInterface.OnClickListener mDialogListener;
    private DialogInterface.OnDismissListener mDismissListener;
    private final Activity mParent;
    private int mWhichEvents = -1;
    private boolean mClickedOk = false;
    private DialogInterface.OnClickListener mListListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            EditResponseHelper.this.mWhichEvents = which;
            Button ok = EditResponseHelper.this.mAlertDialog.getButton(-1);
            ok.setEnabled(true);
        }
    };

    public EditResponseHelper(Activity parent) {
        this.mParent = parent;
    }

    public int getWhichEvents() {
        return this.mWhichEvents;
    }

    public void setWhichEvents(int which) {
        this.mWhichEvents = which;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        setClickedOk(true);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (!getClickedOk()) {
            setWhichEvents(-1);
        }
        setClickedOk(false);
        if (this.mDismissListener != null) {
            this.mDismissListener.onDismiss(dialog);
        }
    }

    private boolean getClickedOk() {
        return this.mClickedOk;
    }

    private void setClickedOk(boolean clickedOk) {
        this.mClickedOk = clickedOk;
    }

    public void setDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.mDismissListener = onDismissListener;
    }

    public void showDialog(int whichEvents) {
        if (this.mDialogListener == null) {
            this.mDialogListener = this;
        }
        AlertDialog dialog = new AlertDialog.Builder(this.mParent).setTitle(R.string.change_response_title).setIconAttribute(android.R.attr.alertDialogIcon).setSingleChoiceItems(R.array.change_response_labels, whichEvents, this.mListListener).setPositiveButton(android.R.string.ok, this.mDialogListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
        dialog.setOnDismissListener(this);
        this.mAlertDialog = dialog;
        if (whichEvents == -1) {
            Button ok = dialog.getButton(-1);
            ok.setEnabled(false);
        }
    }

    public void dismissAlertDialog() {
        if (this.mAlertDialog != null) {
            this.mAlertDialog.dismiss();
        }
    }
}
