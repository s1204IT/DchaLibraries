package com.android.storagemanager.deletionhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.format.Formatter;
import com.android.internal.logging.MetricsLogger;
import com.android.storagemanager.R;

public class ConfirmDeletionDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private long mFreeableBytes;

    public static ConfirmDeletionDialog newInstance(long j) {
        Bundle bundle = new Bundle(1);
        bundle.putLong("total_freeable", j);
        ConfirmDeletionDialog confirmDeletionDialog = new ConfirmDeletionDialog();
        confirmDeletionDialog.setArguments(bundle);
        return confirmDeletionDialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        this.mFreeableBytes = getArguments().getLong("total_freeable");
        Context context = getContext();
        return new AlertDialog.Builder(context).setTitle(R.string.deletion_helper_clear_dialog_title).setMessage(context.getString(getClearWarningText(), Formatter.formatFileSize(context, this.mFreeableBytes))).setPositiveButton(R.string.deletion_helper_clear_dialog_remove, this).setNegativeButton(android.R.string.cancel, this).create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        SharedPreferences.Editor editorEdit = getSharedPreferences().edit();
        editorEdit.putBoolean("shown_before", true);
        editorEdit.apply();
        switch (i) {
            case -2:
                MetricsLogger.action(getContext(), 470);
                break;
            case -1:
                ((DeletionHelperSettings) getTargetFragment()).clearData();
                MetricsLogger.action(getContext(), 469);
                if (StorageManagerUpsellDialog.shouldShow(getContext(), System.currentTimeMillis())) {
                    StorageManagerUpsellDialog.newInstance(this.mFreeableBytes).show(getFragmentManager(), "StorageManagerUpsellDialog");
                } else {
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.finish();
                    }
                }
                break;
        }
    }

    private int getClearWarningText() {
        if (SystemProperties.getBoolean("ro.storage_manager.enabled", false) || getSharedPreferences().getBoolean("shown_before", false)) {
            return R.string.deletion_helper_clear_dialog_message;
        }
        return R.string.deletion_helper_clear_dialog_message_first_time;
    }

    private SharedPreferences getSharedPreferences() {
        return getContext().getSharedPreferences("StorageManager", 0);
    }
}
