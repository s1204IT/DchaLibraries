package com.android.internal.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import com.android.internal.app.AlertController;

public abstract class AlertActivity extends Activity implements DialogInterface {
    protected AlertController mAlert;
    protected AlertController.AlertParams mAlertParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mAlert = new AlertController(this, this, getWindow());
        this.mAlertParams = new AlertController.AlertParams(this);
    }

    @Override
    public void cancel() {
        finish();
    }

    @Override
    public void dismiss() {
        if (!isFinishing()) {
            finish();
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.setClassName(Dialog.class.getName());
        event.setPackageName(getPackageName());
        ViewGroup.LayoutParams params = getWindow().getAttributes();
        boolean isFullScreen = params.width == -1 && params.height == -1;
        event.setFullScreen(isFullScreen);
        return false;
    }

    protected void setupAlert() {
        this.mAlertParams.apply(this.mAlert);
        this.mAlert.installContent();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.mAlert.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (this.mAlert.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
