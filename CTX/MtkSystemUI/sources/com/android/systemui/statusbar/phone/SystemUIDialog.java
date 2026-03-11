package com.android.systemui.statusbar.phone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.view.WindowManager;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.policy.KeyguardMonitor;

public class SystemUIDialog extends AlertDialog {
    private final Context mContext;

    public SystemUIDialog(Context context) {
        this(context, 2131886643);
    }

    public SystemUIDialog(Context context, int i) {
        super(context, i);
        this.mContext = context;
        applyFlags(this);
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.setTitle(getClass().getSimpleName());
        getWindow().setAttributes(attributes);
        registerDismissListener(this);
    }

    public void setShowForAllUsers(boolean z) {
        setShowForAllUsers(this, z);
    }

    public void setMessage(int i) {
        setMessage(this.mContext.getString(i));
    }

    public void setPositiveButton(int i, DialogInterface.OnClickListener onClickListener) {
        setButton(-1, this.mContext.getString(i), onClickListener);
    }

    public void setNegativeButton(int i, DialogInterface.OnClickListener onClickListener) {
        setButton(-2, this.mContext.getString(i), onClickListener);
    }

    public void setNeutralButton(int i, DialogInterface.OnClickListener onClickListener) {
        setButton(-3, this.mContext.getString(i), onClickListener);
    }

    public static void setShowForAllUsers(Dialog dialog, boolean z) {
        if (z) {
            dialog.getWindow().getAttributes().privateFlags |= 16;
        } else {
            dialog.getWindow().getAttributes().privateFlags &= -17;
        }
    }

    public static void setWindowOnTop(Dialog dialog) {
        if (((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).isShowing()) {
            dialog.getWindow().setType(2014);
        } else {
            dialog.getWindow().setType(2017);
        }
    }

    public static AlertDialog applyFlags(AlertDialog alertDialog) {
        alertDialog.getWindow().setType(2014);
        alertDialog.getWindow().addFlags(655360);
        return alertDialog;
    }

    public static void registerDismissListener(Dialog dialog) {
        new DismissReceiver(dialog).register();
    }

    private static class DismissReceiver extends BroadcastReceiver implements DialogInterface.OnDismissListener {
        private static final IntentFilter INTENT_FILTER = new IntentFilter();
        private final Dialog mDialog;
        private boolean mRegistered;

        static {
            INTENT_FILTER.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            INTENT_FILTER.addAction("android.intent.action.SCREEN_OFF");
        }

        DismissReceiver(Dialog dialog) {
            this.mDialog = dialog;
        }

        void register() {
            this.mDialog.getContext().registerReceiverAsUser(this, UserHandle.CURRENT, INTENT_FILTER, null, null);
            this.mRegistered = true;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.mDialog.dismiss();
        }

        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            if (this.mRegistered) {
                this.mDialog.getContext().unregisterReceiver(this);
                this.mRegistered = false;
            }
        }
    }
}
