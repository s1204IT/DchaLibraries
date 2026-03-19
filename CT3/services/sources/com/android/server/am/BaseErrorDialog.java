package com.android.server.am;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import com.android.server.pm.PackageManagerService;

class BaseErrorDialog extends AlertDialog {
    private static final int DISABLE_BUTTONS = 1;
    private static final int ENABLE_BUTTONS = 0;
    private boolean mConsuming;
    private Handler mHandler;

    public BaseErrorDialog(Context context) {
        super(context, R.style.Widget.DeviceDefault.Light.DatePicker);
        this.mConsuming = true;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    BaseErrorDialog.this.mConsuming = false;
                    BaseErrorDialog.this.setEnabled(true);
                } else {
                    if (msg.what != 1) {
                        return;
                    }
                    BaseErrorDialog.this.setEnabled(false);
                }
            }
        };
        getWindow().setType(2003);
        getWindow().setFlags(PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS, PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("Error Dialog");
        getWindow().setAttributes(attrs);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mHandler.sendEmptyMessage(1);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), 1000L);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mConsuming) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void setEnabled(boolean enabled) {
        Button b = (Button) findViewById(R.id.button1);
        if (b != null) {
            b.setEnabled(enabled);
        }
        Button b2 = (Button) findViewById(R.id.button2);
        if (b2 != null) {
            b2.setEnabled(enabled);
        }
        Button b3 = (Button) findViewById(R.id.button3);
        if (b3 == null) {
            return;
        }
        b3.setEnabled(enabled);
    }
}
