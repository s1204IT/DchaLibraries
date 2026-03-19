package com.android.server.am;

import android.R;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

final class FactoryErrorDialog extends BaseErrorDialog {
    private final Handler mHandler;

    public FactoryErrorDialog(Context context, CharSequence msg) {
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg2) {
                throw new RuntimeException("Rebooting from failed factory test");
            }
        };
        setCancelable(false);
        setTitle(context.getText(R.string.config_supervisedUserCreationPackage));
        setMessage(msg);
        setButton(-1, context.getText(R.string.config_systemTelevisionRemoteService), this.mHandler.obtainMessage(0));
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("Factory Error");
        getWindow().setAttributes(attrs);
    }

    @Override
    public void onStop() {
    }
}
