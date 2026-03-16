package com.android.systemui.statusbar.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;
import com.android.systemui.R;

public class SystemUIDialog extends AlertDialog {
    private final Context mContext;

    public SystemUIDialog(Context context) {
        super(context, R.style.Theme_SystemUI_Dialog);
        this.mContext = context;
        getWindow().setType(2014);
        getWindow().addFlags(655360);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle(getClass().getSimpleName());
        getWindow().setAttributes(attrs);
    }

    public void setShowForAllUsers(boolean show) {
        if (show) {
            getWindow().getAttributes().privateFlags |= 16;
        } else {
            getWindow().getAttributes().privateFlags &= -17;
        }
    }

    public void setMessage(int resId) {
        setMessage(this.mContext.getString(resId));
    }

    public void setPositiveButton(int resId, DialogInterface.OnClickListener onClick) {
        setButton(-1, this.mContext.getString(resId), onClick);
    }

    public void setNegativeButton(int resId, DialogInterface.OnClickListener onClick) {
        setButton(-2, this.mContext.getString(resId), onClick);
    }
}
