package com.android.phone;

import android.app.Activity;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;

public class EnableIccPinScreen extends Activity {
    private static final boolean DBG = false;
    private static final int ENABLE_ICC_PIN_COMPLETE = 100;
    private static final String LOG_TAG = "PhoneGlobals";
    private boolean mEnable;
    private Phone mPhone;
    private EditText mPinField;
    private LinearLayout mPinFieldContainer;
    private TextView mStatusField;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 100) {
                EnableIccPinScreen.this.handleResult((AsyncResult) message.obj);
            }
        }
    };
    private View.OnClickListener mClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!TextUtils.isEmpty(EnableIccPinScreen.this.mPinField.getText())) {
                EnableIccPinScreen.this.showStatus(EnableIccPinScreen.this.getResources().getText(R.string.enable_in_progress));
                EnableIccPinScreen.this.enableIccPin();
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.enable_sim_pin_screen);
        setupView();
        this.mPhone = PhoneGlobals.getPhone();
        this.mEnable = !this.mPhone.getIccCard().getIccLockEnabled();
        setTitle(getResources().getText(this.mEnable ? R.string.enable_sim_pin : R.string.disable_sim_pin));
    }

    private void setupView() {
        this.mPinField = (EditText) findViewById(R.id.pin);
        this.mPinField.setKeyListener(DigitsKeyListener.getInstance());
        this.mPinField.setMovementMethod(null);
        this.mPinField.setOnClickListener(this.mClicked);
        this.mPinFieldContainer = (LinearLayout) findViewById(R.id.pinc);
        this.mStatusField = (TextView) findViewById(R.id.status);
    }

    private void showStatus(CharSequence charSequence) {
        if (charSequence != null) {
            this.mStatusField.setText(charSequence);
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
        this.mPhone.getIccCard().setIccLockEnabled(this.mEnable, getPin(), Message.obtain(this.mHandler, 100));
    }

    private void handleResult(AsyncResult asyncResult) {
        if (asyncResult.exception == null) {
            showStatus(getResources().getText(this.mEnable ? R.string.enable_pin_ok : R.string.disable_pin_ok));
        } else if (asyncResult.exception instanceof CommandException) {
            showStatus(getResources().getText(R.string.pin_failed));
        }
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                EnableIccPinScreen.this.finish();
            }
        }, 3000L);
    }

    private void log(String str) {
        Log.d("PhoneGlobals", "[EnableIccPin] " + str);
    }
}
