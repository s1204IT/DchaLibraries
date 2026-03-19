package com.android.server.am;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import com.android.server.pm.DumpState;

class BaseErrorDialog extends AlertDialog {
    private static final int DISABLE_BUTTONS = 1;
    private static final int ENABLE_BUTTONS = 0;
    private boolean mConsuming;
    private Handler mHandler;

    public BaseErrorDialog(Context context) {
        super(context, R.style.TextAppearance.Material.TimePicker.InputField);
        this.mConsuming = true;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 0) {
                    BaseErrorDialog.this.mConsuming = false;
                    BaseErrorDialog.this.setEnabled(true);
                } else if (message.what == 1) {
                    BaseErrorDialog.this.setEnabled(false);
                }
            }
        };
        context.assertRuntimeOverlayThemable();
        getWindow().setType(2003);
        getWindow().setFlags(DumpState.DUMP_INTENT_FILTER_VERIFIERS, DumpState.DUMP_INTENT_FILTER_VERIFIERS);
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.setTitle("Error Dialog");
        getWindow().setAttributes(attributes);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mHandler.sendEmptyMessage(1);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), 1000L);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (this.mConsuming) {
            return true;
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    private void setEnabled(boolean z) {
        Button button = (Button) findViewById(R.id.button1);
        if (button != null) {
            button.setEnabled(z);
        }
        Button button2 = (Button) findViewById(R.id.button2);
        if (button2 != null) {
            button2.setEnabled(z);
        }
        Button button3 = (Button) findViewById(R.id.button3);
        if (button3 != null) {
            button3.setEnabled(z);
        }
    }
}
