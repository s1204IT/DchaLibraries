package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.view.KeyEvent;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class DefaultSmsDialogExt extends ContextWrapper implements ISmsDialogExt {
    private static final String TAG = "DefaultSmsDialogExt";

    public DefaultSmsDialogExt(Context context) {
        super(context);
    }

    @Override
    public boolean onClick(String str, AlertActivity alertActivity, Context context, int i) {
        return true;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent, AlertActivity alertActivity) {
        return false;
    }

    @Override
    public void buildMessage(AlertController.AlertParams alertParams, String str, Intent intent, String str2, String str3) {
    }
}
