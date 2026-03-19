package com.android.contacts.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import com.android.contacts.R;

public class DialogManager {
    private final Activity mActivity;
    private boolean mUseDialogId2 = false;

    public interface DialogShowingView {
        Dialog createDialog(Bundle bundle);
    }

    public interface DialogShowingViewActivity {
        DialogManager getDialogManager();
    }

    public static final boolean isManagedId(int i) {
        return i == R.id.dialog_manager_id_1 || i == R.id.dialog_manager_id_2;
    }

    public DialogManager(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("activity must not be null");
        }
        this.mActivity = activity;
    }

    public void showDialogInView(View view, Bundle bundle) {
        int id = view.getId();
        if (bundle.containsKey("view_id")) {
            throw new IllegalArgumentException("Bundle already contains a view_id");
        }
        if (id == -1) {
            throw new IllegalArgumentException("View does not have a proper ViewId");
        }
        bundle.putInt("view_id", id);
        this.mActivity.showDialog(this.mUseDialogId2 ? R.id.dialog_manager_id_2 : R.id.dialog_manager_id_1, bundle);
    }

    public Dialog onCreateDialog(final int i, Bundle bundle) {
        if (i == R.id.dialog_manager_id_1) {
            this.mUseDialogId2 = true;
        } else {
            if (i != R.id.dialog_manager_id_2) {
                return null;
            }
            this.mUseDialogId2 = false;
        }
        if (!bundle.containsKey("view_id")) {
            throw new IllegalArgumentException("Bundle does not contain a ViewId");
        }
        KeyEvent.Callback callbackFindViewById = this.mActivity.findViewById(bundle.getInt("view_id"));
        if (callbackFindViewById == null || !(callbackFindViewById instanceof DialogShowingView)) {
            return null;
        }
        Dialog dialogCreateDialog = ((DialogShowingView) callbackFindViewById).createDialog(bundle);
        if (dialogCreateDialog == null) {
            return dialogCreateDialog;
        }
        dialogCreateDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                DialogManager.this.mActivity.removeDialog(i);
            }
        });
        return dialogCreateDialog;
    }
}
