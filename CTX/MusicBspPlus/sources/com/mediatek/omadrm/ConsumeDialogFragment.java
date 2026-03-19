package com.mediatek.omadrm;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

public class ConsumeDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private static StringBuilder mAlertMsg;
    private DialogInterface.OnClickListener mClickListener = null;

    public static ConsumeDialogFragment newInstance(StringBuilder sb) {
        mAlertMsg = sb;
        return new ConsumeDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(R.drawable.ic_dialog_info);
        builder.setTitle(134545499);
        builder.setPositiveButton(R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setMessage(mAlertMsg);
        return builder.create();
    }

    @Override
    public void onPause() {
        Log.d("OmaDrmUtils/ConsumeDialogFragment", "showConsumerDialog: dismiss dialog when activity paused");
        dismissAllowingStateLoss();
        super.onPause();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (this.mClickListener != null) {
            this.mClickListener.onClick(dialogInterface, i);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        Log.d("OmaDrmUtils/ConsumeDialogFragment", "showConsumerDialog onDismiss dialog: " + dialogInterface);
        if (this.mClickListener != null) {
            this.mClickListener.onClick(dialogInterface, -2);
        }
        super.onDismiss(dialogInterface);
    }

    public void setOnClickListener(DialogInterface.OnClickListener onClickListener) {
        this.mClickListener = onClickListener;
    }
}
