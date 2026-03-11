package com.android.settings.wifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.settings.R;
import java.io.IOException;

class WriteWifiConfigToNfcDialog extends AlertDialog implements TextWatcher, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = WriteWifiConfigToNfcDialog.class.getName().toString();
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private Button mCancelButton;
    private Context mContext;
    private TextView mLabelView;
    private boolean mNFCTagWritingSucceed;
    private int mNetworkId;
    private Handler mOnTextChangedHandler;
    private CheckBox mPasswordCheckBox;
    private TextView mPasswordView;
    private ProgressBar mProgressBar;
    private int mSecurity;
    private Button mSubmitButton;
    private View mView;
    private final PowerManager.WakeLock mWakeLock;
    private WifiManager mWifiManager;
    private String mWpsNfcConfigurationToken;

    WriteWifiConfigToNfcDialog(Context context, int networkId, int security, WifiManager wifiManager) {
        super(context);
        this.mContext = context;
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "WriteWifiConfigToNfcDialog:wakeLock");
        this.mOnTextChangedHandler = new Handler();
        this.mNetworkId = networkId;
        this.mSecurity = security;
        this.mWifiManager = wifiManager;
    }

    WriteWifiConfigToNfcDialog(Context context, Bundle savedState, WifiManager wifiManager) {
        super(context);
        this.mContext = context;
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "WriteWifiConfigToNfcDialog:wakeLock");
        this.mOnTextChangedHandler = new Handler();
        this.mNetworkId = savedState.getInt("network_id");
        this.mSecurity = savedState.getInt("security");
        this.mWifiManager = wifiManager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "it's onCreate(),release the wakelock=" + this.mWakeLock.isHeld());
        this.mView = getLayoutInflater().inflate(R.layout.write_wifi_config_to_nfc, (ViewGroup) null);
        setView(this.mView);
        setInverseBackgroundForced(true);
        setTitle(R.string.setup_wifi_nfc_tag);
        setCancelable(true);
        setButton(-3, this.mContext.getResources().getString(R.string.write_tag), (DialogInterface.OnClickListener) null);
        setButton(-2, this.mContext.getResources().getString(android.R.string.cancel), (DialogInterface.OnClickListener) null);
        this.mPasswordView = (TextView) this.mView.findViewById(R.id.password);
        this.mLabelView = (TextView) this.mView.findViewById(R.id.password_label);
        this.mPasswordView.addTextChangedListener(this);
        this.mPasswordCheckBox = (CheckBox) this.mView.findViewById(R.id.show_password);
        this.mPasswordCheckBox.setOnCheckedChangeListener(this);
        this.mProgressBar = (ProgressBar) this.mView.findViewById(R.id.progress_bar);
        super.onCreate(savedInstanceState);
        this.mSubmitButton = getButton(-3);
        this.mSubmitButton.setOnClickListener(this);
        this.mSubmitButton.setEnabled(false);
        this.mCancelButton = getButton(-2);
    }

    @Override
    public void onStop() {
        Activity activity;
        NfcAdapter nfcAdapter;
        super.onStop();
        Log.d(TAG, "it's onStop(),release the wakelock=" + this.mWakeLock.isHeld());
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        if (this.mNFCTagWritingSucceed && (nfcAdapter = NfcAdapter.getDefaultAdapter((activity = getOwnerActivity()))) != null) {
            nfcAdapter.disableReaderMode(activity);
        }
        this.mNFCTagWritingSucceed = false;
    }

    @Override
    public Bundle onSaveInstanceState() {
        Log.d(TAG, "it's onSaveInstanceState(),release the wakelock=" + this.mWakeLock.isHeld());
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        return super.onSaveInstanceState();
    }

    @Override
    public void onClick(View v) {
        String passwordLength;
        Log.d(TAG, "it's onClick(),release the wakelock=" + this.mWakeLock.isHeld());
        if (!this.mWakeLock.isHeld()) {
            this.mWakeLock.acquire();
        }
        String password = this.mPasswordView.getText().toString();
        String wpsNfcConfigurationToken = this.mWifiManager.getWpsNfcConfigurationToken(this.mNetworkId);
        String passwordHex = byteArrayToHexString(password.getBytes());
        if (password.length() >= 16) {
            passwordLength = Integer.toString(password.length(), 16);
        } else {
            passwordLength = "0" + Character.forDigit(password.length(), 16);
        }
        if (wpsNfcConfigurationToken.contains(String.format("102700%s%s", passwordLength, passwordHex).toUpperCase())) {
            this.mWpsNfcConfigurationToken = wpsNfcConfigurationToken;
            Activity activity = getOwnerActivity();
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
            nfcAdapter.enableReaderMode(activity, new NfcAdapter.ReaderCallback() {
                @Override
                public void onTagDiscovered(Tag tag) {
                    WriteWifiConfigToNfcDialog.this.handleWriteNfcEvent(tag);
                }
            }, 31, null);
            this.mPasswordView.setVisibility(8);
            this.mPasswordCheckBox.setVisibility(8);
            this.mSubmitButton.setVisibility(8);
            InputMethodManager imm = (InputMethodManager) getOwnerActivity().getSystemService("input_method");
            imm.hideSoftInputFromWindow(this.mPasswordView.getWindowToken(), 0);
            this.mLabelView.setText(R.string.status_awaiting_tap);
            this.mView.findViewById(R.id.password_layout).setTextAlignment(4);
            this.mProgressBar.setVisibility(0);
            return;
        }
        this.mLabelView.setText(R.string.status_invalid_password);
    }

    public void saveState(Bundle state) {
        state.putInt("network_id", this.mNetworkId);
        state.putInt("security", this.mSecurity);
    }

    public void handleWriteNfcEvent(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            if (ndef.isWritable()) {
                NdefRecord record = NdefRecord.createMime("application/vnd.wfa.wsc", hexStringToByteArray(this.mWpsNfcConfigurationToken));
                try {
                    ndef.connect();
                    ndef.writeNdefMessage(new NdefMessage(record, new NdefRecord[0]));
                    getOwnerActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            WriteWifiConfigToNfcDialog.this.mProgressBar.setVisibility(8);
                        }
                    });
                    setViewText(this.mLabelView, R.string.status_write_success);
                    setViewText(this.mCancelButton, android.R.string.loading);
                    this.mNFCTagWritingSucceed = true;
                    return;
                } catch (FormatException e) {
                    setViewText(this.mLabelView, R.string.status_failed_to_write);
                    Log.e(TAG, "Unable to write Wi-Fi config to NFC tag.", e);
                    return;
                } catch (IOException e2) {
                    setViewText(this.mLabelView, R.string.status_failed_to_write);
                    Log.e(TAG, "Unable to write Wi-Fi config to NFC tag.", e2);
                    return;
                }
            }
            setViewText(this.mLabelView, R.string.status_tag_not_writable);
            Log.e(TAG, "Tag is not writable");
            return;
        }
        setViewText(this.mLabelView, R.string.status_tag_not_writable);
        Log.e(TAG, "Tag does not support NDEF");
    }

    @Override
    public void dismiss() {
        Log.d(TAG, "it's dismiss(),release the wakelock=" + this.mWakeLock.isHeld());
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        super.dismiss();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        this.mOnTextChangedHandler.post(new Runnable() {
            @Override
            public void run() {
                WriteWifiConfigToNfcDialog.this.enableSubmitIfAppropriate();
            }
        });
    }

    public void enableSubmitIfAppropriate() {
        if (this.mPasswordView != null) {
            if (this.mSecurity == 1) {
                this.mSubmitButton.setEnabled(this.mPasswordView.length() > 0);
                return;
            } else {
                if (this.mSecurity != 2) {
                    return;
                }
                this.mSubmitButton.setEnabled(this.mPasswordView.length() >= 8);
                return;
            }
        }
        this.mSubmitButton.setEnabled(false);
    }

    private void setViewText(final TextView view, final int resid) {
        getOwnerActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setText(resid);
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int i;
        TextView textView = this.mPasswordView;
        if (isChecked) {
            i = 144;
        } else {
            i = 128;
        }
        textView.setInputType(i | 1);
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static String byteArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[(j * 2) + 1] = hexArray[v & 15];
        }
        return new String(hexChars);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
