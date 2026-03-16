package com.android.bluetooth.ftp;

import android.R;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class BluetoothFtpActivity extends AlertActivity implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener, TextWatcher {
    private static final int BLUETOOTH_OBEX_AUTHKEY_MAX_LENGTH = 16;
    private static final int DIALOG_YES_NO_AUTH = 2;
    private static final int DIALOG_YES_NO_CONNECT = 1;
    private static final int DISMISS_TIMEOUT_DIALOG = 0;
    private static final int DISMISS_TIMEOUT_DIALOG_VALUE = 2000;
    private static final String KEY_USER_TIMEOUT = "user_timeout";
    private static final String TAG = "BluetoothFtpActivity";
    private static final boolean V = true;
    private CheckBox mAlwaysAllowed;
    private int mCurrentDialog;
    private EditText mKeyView;
    private Button mOkButton;
    private View mView;
    private TextView messageView;
    private String mSessionKey = "";
    private boolean mTimeout = false;
    private boolean mAlwaysAllowedValue = true;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothFtpService.USER_CONFIRM_TIMEOUT_ACTION.equals(intent.getAction())) {
                BluetoothFtpActivity.this.onTimeout();
            }
        }
    };
    private final Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Log.v(BluetoothFtpActivity.TAG, "Received DISMISS_TIMEOUT_DIALOG msg");
                    BluetoothFtpActivity.this.finish();
                    break;
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        String action = i.getAction();
        Log.v(TAG, "onCreate action = " + action);
        if (action.equals(BluetoothFtpService.ACCESS_REQUEST_ACTION)) {
            showFtpDialog(1);
            this.mCurrentDialog = 1;
        } else if (action.equals(BluetoothFtpService.AUTH_CHALL_ACTION)) {
            showFtpDialog(2);
            this.mCurrentDialog = 2;
        } else {
            Log.e(TAG, "Error: this activity may be started only with intent FTP_ACCESS_REQUEST or FTP_AUTH_CHALL ");
            finish();
        }
        Log.i(TAG, "onCreate");
        registerReceiver(this.mReceiver, new IntentFilter(BluetoothFtpService.USER_CONFIRM_TIMEOUT_ACTION));
    }

    private void showFtpDialog(int id) {
        AlertController.AlertParams p = this.mAlertParams;
        switch (id) {
            case 1:
                Log.v(TAG, "showFtpDialog DIALOG_YES_NO_CONNECT");
                p.mIconId = R.drawable.ic_dialog_info;
                p.mTitle = getString(com.android.bluetooth.R.string.ftp_acceptance_dialog_header);
                p.mView = createView(1);
                p.mPositiveButtonText = getString(R.string.yes);
                p.mPositiveButtonListener = this;
                p.mNegativeButtonText = getString(R.string.no);
                p.mNegativeButtonListener = this;
                this.mOkButton = this.mAlert.getButton(-1);
                setupAlert();
                break;
            case 2:
                Log.v(TAG, "showFtpDialog DIALOG_YES_NO_AUTH");
                p.mIconId = R.drawable.ic_dialog_info;
                p.mTitle = getString(com.android.bluetooth.R.string.ftp_session_key_dialog_header);
                p.mView = createView(2);
                p.mPositiveButtonText = getString(R.string.ok);
                p.mPositiveButtonListener = this;
                p.mNegativeButtonText = getString(R.string.cancel);
                p.mNegativeButtonListener = this;
                setupAlert();
                this.mOkButton = this.mAlert.getButton(-1);
                this.mOkButton.setEnabled(false);
                break;
        }
    }

    private String createDisplayText(int id) {
        String mRemoteName = BluetoothFtpService.getRemoteDeviceName();
        Log.v(TAG, "createDisplayText" + id);
        switch (id) {
            case 1:
                return getString(com.android.bluetooth.R.string.ftp_acceptance_dialog_title, new Object[]{mRemoteName, mRemoteName});
            case 2:
                String mMessage2 = getString(com.android.bluetooth.R.string.ftp_session_key_dialog_title, new Object[]{mRemoteName});
                return mMessage2;
            default:
                Log.e(TAG, "Display Text id (" + id + ")not part of FTP resource");
                return null;
        }
    }

    private View createView(int id) {
        Log.v(TAG, "createView" + id);
        switch (id) {
            case 1:
                this.mView = getLayoutInflater().inflate(com.android.bluetooth.R.layout.access, (ViewGroup) null);
                this.messageView = (TextView) this.mView.findViewById(com.android.bluetooth.R.id.message);
                this.messageView.setText(createDisplayText(id));
                this.mAlwaysAllowed = (CheckBox) this.mView.findViewById(com.android.bluetooth.R.id.alwaysallowed);
                this.mAlwaysAllowed.setChecked(true);
                this.mAlwaysAllowed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            BluetoothFtpActivity.this.mAlwaysAllowedValue = true;
                        } else {
                            BluetoothFtpActivity.this.mAlwaysAllowedValue = false;
                        }
                    }
                });
                return this.mView;
            case 2:
                this.mView = getLayoutInflater().inflate(com.android.bluetooth.R.layout.auth, (ViewGroup) null);
                this.messageView = (TextView) this.mView.findViewById(com.android.bluetooth.R.id.message);
                this.messageView.setText(createDisplayText(id));
                this.mKeyView = (EditText) this.mView.findViewById(com.android.bluetooth.R.id.text);
                this.mKeyView.addTextChangedListener(this);
                this.mKeyView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
                return this.mView;
            default:
                Log.e(TAG, "Create view id (" + id + ")not part of FTP resource");
                return null;
        }
    }

    private void onPositive() {
        Log.v(TAG, "onPositive mtimeout = " + this.mTimeout + "mCurrentDialog = " + this.mCurrentDialog);
        if (!this.mTimeout) {
            if (this.mCurrentDialog == 1) {
                sendIntentToReceiver(BluetoothFtpService.ACCESS_ALLOWED_ACTION, BluetoothFtpService.EXTRA_ALWAYS_ALLOWED, this.mAlwaysAllowedValue);
            } else if (this.mCurrentDialog == 2) {
                sendIntentToReceiver(BluetoothFtpService.AUTH_RESPONSE_ACTION, BluetoothFtpService.EXTRA_SESSION_KEY, this.mSessionKey);
                this.mKeyView.removeTextChangedListener(this);
            }
        }
        this.mTimeout = false;
        finish();
    }

    private void onNegative() {
        Log.v(TAG, "onNegative mtimeout = " + this.mTimeout + "mCurrentDialog = " + this.mCurrentDialog);
        if (this.mCurrentDialog == 1) {
            sendIntentToReceiver(BluetoothFtpService.ACCESS_DISALLOWED_ACTION, (String) null, (String) null);
        } else if (this.mCurrentDialog == 2) {
            sendIntentToReceiver(BluetoothFtpService.AUTH_CANCELLED_ACTION, (String) null, (String) null);
            this.mKeyView.removeTextChangedListener(this);
        }
        finish();
    }

    private void sendIntentToReceiver(String intentName, String extraName, String extraValue) {
        Intent intent = new Intent(intentName);
        intent.setClassName("com.android.bluetooth", BluetoothFtpReceiver.class.getName());
        if (extraName != null) {
            intent.putExtra(extraName, extraValue);
        }
        sendBroadcast(intent);
    }

    private void sendIntentToReceiver(String intentName, String extraName, boolean extraValue) {
        Intent intent = new Intent(intentName);
        intent.setClassName("com.android.bluetooth", BluetoothFtpReceiver.class.getName());
        if (extraName != null) {
            intent.putExtra(extraName, extraValue);
        }
        sendBroadcast(intent);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Log.v(TAG, "onClick which = " + which);
        switch (which) {
            case -2:
                onNegative();
                break;
            case -1:
                if (this.mCurrentDialog == 2) {
                    this.mSessionKey = this.mKeyView.getText().toString();
                }
                onPositive();
                break;
        }
    }

    private void onTimeout() {
        this.mTimeout = true;
        Log.v(TAG, "onTimeout mCurrentDialog = " + this.mCurrentDialog);
        if (this.mCurrentDialog == 1) {
            if (this.mView != null) {
                this.messageView.setText(getString(com.android.bluetooth.R.string.ftp_acceptance_timeout_message, new Object[]{BluetoothFtpService.getRemoteDeviceName()}));
                this.mAlert.getButton(-2).setVisibility(8);
                this.mAlwaysAllowed.setVisibility(8);
                this.mAlwaysAllowed.clearFocus();
            }
        } else if (this.mCurrentDialog == 2 && this.mView != null) {
            this.messageView.setText(getString(com.android.bluetooth.R.string.ftp_authentication_timeout_message, new Object[]{BluetoothFtpService.getRemoteDeviceName()}));
            this.mKeyView.setVisibility(8);
            this.mKeyView.clearFocus();
            this.mKeyView.removeTextChangedListener(this);
            this.mOkButton.setEnabled(true);
            this.mAlert.getButton(-2).setVisibility(8);
        }
        this.mTimeoutHandler.sendMessageDelayed(this.mTimeoutHandler.obtainMessage(0), 2000L);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.mTimeout = savedInstanceState.getBoolean(KEY_USER_TIMEOUT);
        Log.v(TAG, "onRestoreInstanceState() mTimeout: " + this.mTimeout);
        if (this.mTimeout) {
            onTimeout();
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_USER_TIMEOUT, this.mTimeout);
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int before, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() > 0) {
            this.mOkButton.setEnabled(true);
        }
    }
}
