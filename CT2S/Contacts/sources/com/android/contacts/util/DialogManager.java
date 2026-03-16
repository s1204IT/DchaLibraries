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

    public static final boolean isManagedId(int id) {
        return id == R.id.dialog_manager_id_1 || id == R.id.dialog_manager_id_2;
    }

    public DialogManager(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("activity must not be null");
        }
        this.mActivity = activity;
    }

    public void showDialogInView(View view, Bundle bundle) {
        int viewId = view.getId();
        if (bundle.containsKey("view_id")) {
            throw new IllegalArgumentException("Bundle already contains a view_id");
        }
        if (viewId == -1) {
            throw new IllegalArgumentException("View does not have a proper ViewId");
        }
        bundle.putInt("view_id", viewId);
        int dialogId = this.mUseDialogId2 ? R.id.dialog_manager_id_2 : R.id.dialog_manager_id_1;
        this.mActivity.showDialog(dialogId, bundle);
    }

    public Dialog onCreateDialog(final int id, Bundle bundle) {
        Dialog dialog = null;
        if (id == R.id.dialog_manager_id_1) {
            this.mUseDialogId2 = true;
        } else {
            if (id == R.id.dialog_manager_id_2) {
                this.mUseDialogId2 = false;
            }
            return dialog;
        }
        if (!bundle.containsKey("view_id")) {
            throw new IllegalArgumentException("Bundle does not contain a ViewId");
        }
        int viewId = bundle.getInt("view_id");
        KeyEvent.Callback callbackFindViewById = this.mActivity.findViewById(viewId);
        if (callbackFindViewById != null && (callbackFindViewById instanceof DialogShowingView) && (dialog = ((DialogShowingView) callbackFindViewById).createDialog(bundle)) != null) {
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    DialogManager.this.mActivity.removeDialog(id);
                }
            });
        }
        return dialog;
    }
}
