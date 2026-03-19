package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;

public class ChangeIccPinScreen extends Activity {
    private static final boolean DBG = false;
    private static final int EVENT_PIN_CHANGED = 100;
    private static final String LOG_TAG = "PhoneGlobals";
    private static final int MAX_PIN_LENGTH = 8;
    private static final int MIN_PIN_LENGTH = 4;
    private static final int NO_ERROR = 0;
    private static final int PIN_INVALID_LENGTH = 2;
    private static final int PIN_MISMATCH = 1;
    private TextView mBadPinError;
    private Button mButton;
    private boolean mChangePin2;
    private LinearLayout mIccPUKPanel;
    private TextView mMismatchError;
    private EditText mNewPin1;
    private EditText mNewPin2;
    private EditText mOldPin;
    private AlertDialog mPUKAlert;
    private EditText mPUKCode;
    private Button mPUKSubmit;
    private Phone mPhone;
    private ScrollView mScrollView;
    private EntryState mState;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 100) {
                ChangeIccPinScreen.this.handleResult((AsyncResult) message.obj);
            }
        }
    };
    private View.OnClickListener mClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String string;
            if (view == ChangeIccPinScreen.this.mOldPin) {
                ChangeIccPinScreen.this.mNewPin1.requestFocus();
                return;
            }
            if (view == ChangeIccPinScreen.this.mNewPin1) {
                ChangeIccPinScreen.this.mNewPin2.requestFocus();
                return;
            }
            if (view == ChangeIccPinScreen.this.mNewPin2) {
                ChangeIccPinScreen.this.mButton.requestFocus();
                return;
            }
            if (view == ChangeIccPinScreen.this.mButton) {
                IccCard iccCard = ChangeIccPinScreen.this.mPhone.getIccCard();
                if (iccCard != null) {
                    String string2 = ChangeIccPinScreen.this.mOldPin.getText().toString();
                    String string3 = ChangeIccPinScreen.this.mNewPin1.getText().toString();
                    int iValidateNewPin = ChangeIccPinScreen.this.validateNewPin(string3, ChangeIccPinScreen.this.mNewPin2.getText().toString());
                    switch (iValidateNewPin) {
                        case 1:
                        case 2:
                            ChangeIccPinScreen.this.mNewPin1.getText().clear();
                            ChangeIccPinScreen.this.mNewPin2.getText().clear();
                            ChangeIccPinScreen.this.mMismatchError.setVisibility(0);
                            Resources resources = ChangeIccPinScreen.this.getResources();
                            if (iValidateNewPin == 1) {
                                string = resources.getString(R.string.mismatchPin);
                            } else {
                                string = resources.getString(R.string.invalidPin);
                            }
                            ChangeIccPinScreen.this.mMismatchError.setText(string);
                            break;
                        default:
                            Message messageObtain = Message.obtain(ChangeIccPinScreen.this.mHandler, 100);
                            ChangeIccPinScreen.this.reset();
                            if (ChangeIccPinScreen.this.mChangePin2) {
                                iccCard.changeIccFdnPassword(string2, string3, messageObtain);
                            } else {
                                iccCard.changeIccLockPassword(string2, string3, messageObtain);
                            }
                            break;
                    }
                }
                return;
            }
            if (view == ChangeIccPinScreen.this.mPUKCode) {
                ChangeIccPinScreen.this.mPUKSubmit.requestFocus();
            } else if (view == ChangeIccPinScreen.this.mPUKSubmit) {
                ChangeIccPinScreen.this.mPhone.getIccCard().supplyPuk2(ChangeIccPinScreen.this.mPUKCode.getText().toString(), ChangeIccPinScreen.this.mNewPin1.getText().toString(), Message.obtain(ChangeIccPinScreen.this.mHandler, 100));
            }
        }
    };

    private enum EntryState {
        ES_PIN,
        ES_PUK
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mPhone = PhoneGlobals.getPhone();
        resolveIntent();
        setContentView(R.layout.change_sim_pin_screen);
        this.mOldPin = (EditText) findViewById(R.id.old_pin);
        this.mOldPin.setKeyListener(DigitsKeyListener.getInstance());
        this.mOldPin.setMovementMethod(null);
        this.mOldPin.setOnClickListener(this.mClicked);
        this.mNewPin1 = (EditText) findViewById(R.id.new_pin1);
        this.mNewPin1.setKeyListener(DigitsKeyListener.getInstance());
        this.mNewPin1.setMovementMethod(null);
        this.mNewPin1.setOnClickListener(this.mClicked);
        this.mNewPin2 = (EditText) findViewById(R.id.new_pin2);
        this.mNewPin2.setKeyListener(DigitsKeyListener.getInstance());
        this.mNewPin2.setMovementMethod(null);
        this.mNewPin2.setOnClickListener(this.mClicked);
        this.mBadPinError = (TextView) findViewById(R.id.bad_pin);
        this.mMismatchError = (TextView) findViewById(R.id.mismatch);
        this.mButton = (Button) findViewById(R.id.button);
        this.mButton.setOnClickListener(this.mClicked);
        this.mScrollView = (ScrollView) findViewById(R.id.scroll);
        this.mPUKCode = (EditText) findViewById(R.id.puk_code);
        this.mPUKCode.setKeyListener(DigitsKeyListener.getInstance());
        this.mPUKCode.setMovementMethod(null);
        this.mPUKCode.setOnClickListener(this.mClicked);
        this.mPUKSubmit = (Button) findViewById(R.id.puk_submit);
        this.mPUKSubmit.setOnClickListener(this.mClicked);
        this.mIccPUKPanel = (LinearLayout) findViewById(R.id.puk_panel);
        setTitle(getResources().getText(this.mChangePin2 ? R.string.change_pin2 : R.string.change_pin));
        this.mState = EntryState.ES_PIN;
    }

    private void resolveIntent() {
        this.mChangePin2 = getIntent().getBooleanExtra("pin2", this.mChangePin2);
    }

    private void reset() {
        this.mScrollView.scrollTo(0, 0);
        this.mBadPinError.setVisibility(8);
        this.mMismatchError.setVisibility(8);
    }

    private int validateNewPin(String str, String str2) {
        if (str == null) {
            return 2;
        }
        if (!str.equals(str2)) {
            return 1;
        }
        int length = str.length();
        if (length < 4 || length > 8) {
            return 2;
        }
        return 0;
    }

    private void handleResult(AsyncResult asyncResult) {
        if (asyncResult.exception == null) {
            if (this.mState == EntryState.ES_PUK) {
                this.mScrollView.setVisibility(0);
                this.mIccPUKPanel.setVisibility(8);
            }
            showConfirmation();
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ChangeIccPinScreen.this.finish();
                }
            }, 3000L);
            return;
        }
        if (asyncResult.exception instanceof CommandException) {
            if (this.mState != EntryState.ES_PIN) {
                if (this.mState == EntryState.ES_PUK) {
                    displayPUKAlert();
                    this.mPUKCode.getText().clear();
                    this.mPUKCode.requestFocus();
                    return;
                }
                return;
            }
            this.mOldPin.getText().clear();
            this.mBadPinError.setVisibility(0);
            if (asyncResult.exception.getCommandError() == CommandException.Error.SIM_PUK2) {
                this.mState = EntryState.ES_PUK;
                displayPUKAlert();
                this.mScrollView.setVisibility(8);
                this.mIccPUKPanel.setVisibility(0);
                this.mPUKCode.requestFocus();
            }
        }
    }

    private void displayPUKAlert() {
        if (this.mPUKAlert == null) {
            this.mPUKAlert = new AlertDialog.Builder(this).setMessage(R.string.puk_requested).setCancelable(false).show();
        } else {
            this.mPUKAlert.show();
        }
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ChangeIccPinScreen.this.mPUKAlert.dismiss();
            }
        }, 3000L);
    }

    private void showConfirmation() {
        Toast.makeText(this, this.mChangePin2 ? R.string.pin2_changed : R.string.pin_changed, 0).show();
    }

    private void log(String str) {
        Log.d("PhoneGlobals", (this.mChangePin2 ? "[ChgPin2]" : "[ChgPin]") + str);
    }
}
