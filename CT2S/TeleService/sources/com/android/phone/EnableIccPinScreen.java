package com.android.phone;

import android.app.Activity;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;

public class EnableIccPinScreen extends Activity {
    private boolean mEnable;
    private Phone mPhone;
    private EditText mPinField;
    private LinearLayout mPinFieldContainer;
    private TextView mStatusField;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    EnableIccPinScreen.this.handleResult(ar);
                    break;
            }
        }
    };
    private View.OnClickListener mClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!TextUtils.isEmpty(EnableIccPinScreen.this.mPinField.getText())) {
                EnableIccPinScreen.this.showStatus(EnableIccPinScreen.this.getResources().getText(R.string.enable_in_progress));
                EnableIccPinScreen.this.enableIccPin();
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.enable_sim_pin_screen);
        setupView();
        this.mPhone = PhoneGlobals.getPhone();
        this.mEnable = !this.mPhone.getIccCard().getIccLockEnabled();
        int id = this.mEnable ? R.string.enable_sim_pin : R.string.disable_sim_pin;
        setTitle(getResources().getText(id));
    }

    private void setupView() {
        this.mPinField = (EditText) findViewById(R.id.pin);
        this.mPinField.setKeyListener(DigitsKeyListener.getInstance());
        this.mPinField.setMovementMethod(null);
        this.mPinField.setOnClickListener(this.mClicked);
        this.mPinFieldContainer = (LinearLayout) findViewById(R.id.pinc);
        this.mStatusField = (TextView) findViewById(R.id.status);
    }

    private void showStatus(CharSequence statusMsg) {
        if (statusMsg != null) {
            this.mStatusField.setText(statusMsg);
            this.mStatusField.setVisibility(0);
            this.mPinFieldContainer.setVisibility(8);
        } else {
            this.mPinFieldContainer.setVisibility(0);
            this.mStatusField.setVisibility(8);
        }
    }

    private String getPin() {
        return this.mPinField.getText().toString();
    }

    private void enableIccPin() {
        Message callback = Message.obtain(this.mHandler, 100);
        this.mPhone.getIccCard().setIccLockEnabled(this.mEnable, getPin(), callback);
    }

    private void handleResult(AsyncResult ar) {
        if (ar.exception == null) {
            showStatus(getResources().getText(this.mEnable ? R.string.enable_pin_ok : R.string.disable_pin_ok));
        } else if (ar.exception instanceof CommandException) {
            showStatus(getResources().getText(R.string.pin_failed));
        }
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                EnableIccPinScreen.this.finish();
            }
        }, 3000L);
    }
}
