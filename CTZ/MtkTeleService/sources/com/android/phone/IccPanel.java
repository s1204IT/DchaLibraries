package com.android.phone;

import android.app.Dialog;
import android.app.StatusBarManager;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;

public class IccPanel extends Dialog {
    protected static final String TAG = "PhoneGlobals";
    private StatusBarManager mStatusBarManager;

    public IccPanel(Context context) {
        super(context, R.style.IccPanel);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Window window = getWindow();
        window.setType(2007);
        window.setLayout(-1, -1);
        window.setGravity(17);
        this.mStatusBarManager = (StatusBarManager) PhoneGlobals.getInstance().getSystemService("statusbar");
        requestWindowFeature(1);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mStatusBarManager.disable(65536);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mStatusBarManager.disable(0);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 4) {
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }
}
