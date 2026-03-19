package com.android.server.am;

import android.R;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

final class FactoryErrorDialog extends BaseErrorDialog {
    private final Handler mHandler;

    public FactoryErrorDialog(Context context, CharSequence charSequence) {
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                throw new RuntimeException("Rebooting from failed factory test");
            }
        };
        setCancelable(false);
        setTitle(context.getText(R.string.config_batterymeterFillMask));
        setMessage(charSequence);
        setButton(-1, context.getText(R.string.config_biometric_prompt_ui_package), this.mHandler.obtainMessage(0));
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.setTitle("Factory Error");
        getWindow().setAttributes(attributes);
    }

    @Override
    public void onStop() {
    }
}
