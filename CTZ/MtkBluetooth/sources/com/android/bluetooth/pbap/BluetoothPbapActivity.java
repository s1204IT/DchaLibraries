package com.android.bluetooth.pbap;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.bluetooth.R;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class BluetoothPbapActivity extends AlertActivity implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener, TextWatcher {
    private static final int BLUETOOTH_OBEX_AUTHKEY_MAX_LENGTH = 16;
    private static final int DIALOG_YES_NO_AUTH = 1;
    private static final int DISMISS_TIMEOUT_DIALOG = 0;
    private static final int DISMISS_TIMEOUT_DIALOG_VALUE = 2000;
    private static final String KEY_USER_TIMEOUT = "user_timeout";
    private static final String TAG = "BluetoothPbapActivity";
    private static final boolean V = BluetoothPbapService.VERBOSE;
    private int mCurrentDialog;
    private BluetoothDevice mDevice;
    private EditText mKeyView;
    private TextView mMessageView;
    private Button mOkButton;
    private View mView;
    private String mSessionKey = "";
    private boolean mTimeout = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.android.bluetooth.pbap.userconfirmtimeout".equals(intent.getAction())) {
                BluetoothPbapActivity.this.onTimeout();
            }
        }
    };
    private final Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                if (BluetoothPbapActivity.V) {
                    Log.v(BluetoothPbapActivity.TAG, "Received DISMISS_TIMEOUT_DIALOG msg.");
                }
                BluetoothPbapActivity.this.finish();
            }
        }
    };

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        String action = intent.getAction();
        this.mDevice = (BluetoothDevice) intent.getParcelableExtra("com.android.bluetooth.pbap.device");
        if (action.equals("com.android.bluetooth.pbap.authchall")) {
            showPbapDialog(1);
            this.mCurrentDialog = 1;
        } else {
            Log.e(TAG, "Error: this activity may be started only with intent PBAP_ACCESS_REQUEST or PBAP_AUTH_CHALL ");
            finish();
        }
        registerReceiver(this.mReceiver, new IntentFilter("com.android.bluetooth.pbap.userconfirmtimeout"));
    }

    private void showPbapDialog(int i) {
        AlertController.AlertParams alertParams = ((AlertActivity) this).mAlertParams;
        if (i == 1) {
            alertParams.mTitle = getString(R.string.pbap_session_key_dialog_header);
            alertParams.mView = createView(1);
            alertParams.mPositiveButtonText = getString(android.R.string.ok);
            alertParams.mPositiveButtonListener = this;
            alertParams.mNegativeButtonText = getString(android.R.string.cancel);
            alertParams.mNegativeButtonListener = this;
            setupAlert();
            this.mOkButton = ((AlertActivity) this).mAlert.getButton(-1);
            this.mOkButton.setEnabled(false);
        }
    }

    private String createDisplayText(int i) {
        if (i == 1) {
            return getString(R.string.pbap_session_key_dialog_title, this.mDevice);
        }
        return null;
    }

    private View createView(int i) {
        if (i != 1) {
            return null;
        }
        this.mView = getLayoutInflater().inflate(R.layout.auth, (ViewGroup) null);
        this.mMessageView = (TextView) this.mView.findViewById(R.id.message);
        this.mMessageView.setText(createDisplayText(i));
        this.mKeyView = (EditText) this.mView.findViewById(R.id.text);
        this.mKeyView.addTextChangedListener(this);
        this.mKeyView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
        return this.mView;
    }

    private void onPositive() {
        if (!this.mTimeout && this.mCurrentDialog == 1) {
            sendIntentToReceiver("com.android.bluetooth.pbap.authresponse", "com.android.bluetooth.pbap.sessionkey", this.mSessionKey);
            this.mKeyView.removeTextChangedListener(this);
        }
        this.mTimeout = false;
        finish();
    }

    private void onNegative() {
        if (this.mCurrentDialog == 1) {
            sendIntentToReceiver("com.android.bluetooth.pbap.authcancelled", null, null);
            this.mKeyView.removeTextChangedListener(this);
        }
        finish();
    }

    private void sendIntentToReceiver(String str, String str2, String str3) {
        Intent intent = new Intent(str);
        intent.setPackage("com.android.bluetooth");
        intent.putExtra("com.android.bluetooth.pbap.device", this.mDevice);
        if (str2 != null) {
            intent.putExtra(str2, str3);
        }
        sendBroadcast(intent);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case -2:
                onNegative();
                break;
            case -1:
                if (this.mCurrentDialog == 1) {
                    this.mSessionKey = this.mKeyView.getText().toString();
                }
                onPositive();
                break;
        }
    }

    private void onTimeout() {
        this.mTimeout = true;
        if (this.mCurrentDialog == 1) {
            this.mMessageView.setText(getString(R.string.pbap_authentication_timeout_message, this.mDevice));
            this.mKeyView.setVisibility(8);
            this.mKeyView.clearFocus();
            this.mKeyView.removeTextChangedListener(this);
            this.mOkButton.setEnabled(true);
            ((AlertActivity) this).mAlert.getButton(-2).setVisibility(8);
        }
        this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(0), 2000L);
    }

    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        this.mTimeout = bundle.getBoolean(KEY_USER_TIMEOUT);
        if (V) {
            Log.v(TAG, "onRestoreInstanceState() mTimeout: " + this.mTimeout);
        }
        if (this.mTimeout) {
            onTimeout();
        }
    }

    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(KEY_USER_TIMEOUT, this.mTimeout);
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        return true;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (editable.length() > 0) {
            this.mOkButton.setEnabled(true);
        }
    }
}
