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
    public boolean onClick(String newPackageName, AlertActivity activity, Context context, int which) {
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event, AlertActivity context) {
        return false;
    }

    @Override
    public void buildMessage(AlertController.AlertParams param, String packageName, Intent intent, String newName, String oldName) {
    }
}
