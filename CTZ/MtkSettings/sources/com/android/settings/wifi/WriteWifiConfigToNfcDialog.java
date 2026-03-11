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
    private CheckBox mPasswordCheckBox;
    private TextView mPasswordView;
    private ProgressBar mProgressBar;
    private int mSecurity;
    private Button mSubmitButton;
    private View mView;
    private final PowerManager.WakeLock mWakeLock;
    private WifiManager mWifiManager;
    private String mWpsNfcConfigurationToken;

    WriteWifiConfigToNfcDialog(Context context, int i) {
        super(context);
        this.mContext = context;
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "WriteWifiConfigToNfcDialog:wakeLock");
        this.mSecurity = i;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    WriteWifiConfigToNfcDialog(Context context, Bundle bundle) {
        super(context);
        this.mContext = context;
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "WriteWifiConfigToNfcDialog:wakeLock");
        this.mSecurity = bundle.getInt("security");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    @Override
    public void onCreate(Bundle bundle) {
        Log.d(TAG, "it's onCreate(),release the wakelock=" + this.mWakeLock.isHeld());
        this.mView = getLayoutInflater().inflate(R.layout.write_wifi_config_to_nfc, (ViewGroup) null);
        setView(this.mView);
        setInverseBackgroundForced(true);
        setTitle(R.string.setup_wifi_nfc_tag);
        setCancelable(true);
        DialogInterface.OnClickListener onClickListener = (DialogInterface.OnClickListener) null;
        setButton(-3, this.mContext.getResources().getString(R.string.write_tag), onClickListener);
        setButton(-2, this.mContext.getResources().getString(android.R.string.cancel), onClickListener);
        this.mPasswordView = (TextView) this.mView.findViewById(R.id.password);
        this.mLabelView = (TextView) this.mView.findViewById(R.id.password_label);
        this.mPasswordView.addTextChangedListener(this);
        this.mPasswordCheckBox = (CheckBox) this.mView.findViewById(R.id.show_password);
        this.mPasswordCheckBox.setOnCheckedChangeListener(this);
        this.mProgressBar = (ProgressBar) this.mView.findViewById(R.id.progress_bar);
        super.onCreate(bundle);
        this.mSubmitButton = getButton(-3);
        this.mSubmitButton.setOnClickListener(this);
        this.mSubmitButton.setEnabled(false);
        this.mCancelButton = getButton(-2);
    }

    @Override
    public void onStop() {
        Activity ownerActivity;
        NfcAdapter defaultAdapter;
        super.onStop();
        Log.d(TAG, "it's onStop(),release the wakelock=" + this.mWakeLock.isHeld());
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        if (this.mNFCTagWritingSucceed && (defaultAdapter = NfcAdapter.getDefaultAdapter((ownerActivity = getOwnerActivity()))) != null) {
            defaultAdapter.disableReaderMode(ownerActivity);
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
    public void onClick(View view) {
        String string;
        Log.d(TAG, "it's onClick(),release the wakelock=" + this.mWakeLock.isHeld());
        if (!this.mWakeLock.isHeld()) {
            this.mWakeLock.acquire();
        }
        String string2 = this.mPasswordView.getText().toString();
        String currentNetworkWpsNfcConfigurationToken = this.mWifiManager.getCurrentNetworkWpsNfcConfigurationToken();
        String strByteArrayToHexString = byteArrayToHexString(string2.getBytes());
        if (string2.length() >= 16) {
            string = Integer.toString(string2.length(), 16);
        } else {
            string = "0" + Character.forDigit(string2.length(), 16);
        }
        String lowerCase = String.format("102700%s%s", string, strByteArrayToHexString).toLowerCase();
        if (currentNetworkWpsNfcConfigurationToken != null && currentNetworkWpsNfcConfigurationToken.contains(lowerCase)) {
            this.mWpsNfcConfigurationToken = currentNetworkWpsNfcConfigurationToken;
            Activity ownerActivity = getOwnerActivity();
            NfcAdapter.getDefaultAdapter(ownerActivity).enableReaderMode(ownerActivity, new NfcAdapter.ReaderCallback() {
                @Override
                public void onTagDiscovered(Tag tag) {
                    WriteWifiConfigToNfcDialog.this.handleWriteNfcEvent(tag);
                }
            }, 31, null);
            this.mPasswordView.setVisibility(8);
            this.mPasswordCheckBox.setVisibility(8);
            this.mSubmitButton.setVisibility(8);
            ((InputMethodManager) getOwnerActivity().getSystemService("input_method")).hideSoftInputFromWindow(this.mPasswordView.getWindowToken(), 0);
            this.mLabelView.setText(R.string.status_awaiting_tap);
            this.mView.findViewById(R.id.password_layout).setTextAlignment(4);
            this.mProgressBar.setVisibility(0);
            return;
        }
        this.mLabelView.setText(R.string.status_invalid_password);
    }

    public void saveState(Bundle bundle) {
        bundle.putInt("security", this.mSecurity);
    }

    public void handleWriteNfcEvent(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            setViewText(this.mLabelView, R.string.status_tag_not_writable);
            Log.e(TAG, "Tag does not support NDEF");
            return;
        }
        if (!ndef.isWritable()) {
            setViewText(this.mLabelView, R.string.status_tag_not_writable);
            Log.e(TAG, "Tag is not writable");
            return;
        }
        NdefRecord ndefRecordCreateMime = NdefRecord.createMime("application/vnd.wfa.wsc", hexStringToByteArray(this.mWpsNfcConfigurationToken));
        try {
            ndef.connect();
            ndef.writeNdefMessage(new NdefMessage(ndefRecordCreateMime, new NdefRecord[0]));
            getOwnerActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WriteWifiConfigToNfcDialog.this.mProgressBar.setVisibility(8);
                }
            });
            setViewText(this.mLabelView, R.string.status_write_success);
            setViewText(this.mCancelButton, android.R.string.bugreport_screenshot_failure_toast);
            this.mNFCTagWritingSucceed = true;
        } catch (FormatException e) {
            setViewText(this.mLabelView, R.string.status_failed_to_write);
            Log.e(TAG, "Unable to write Wi-Fi config to NFC tag.", e);
        } catch (IOException e2) {
            setViewText(this.mLabelView, R.string.status_failed_to_write);
            Log.e(TAG, "Unable to write Wi-Fi config to NFC tag.", e2);
        }
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
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        enableSubmitIfAppropriate();
    }

    private void enableSubmitIfAppropriate() {
        if (this.mPasswordView != null) {
            if (this.mSecurity == 1) {
                this.mSubmitButton.setEnabled(this.mPasswordView.length() > 0);
                return;
            } else {
                if (this.mSecurity == 2) {
                    this.mSubmitButton.setEnabled(this.mPasswordView.length() >= 8);
                    return;
                }
                return;
            }
        }
        this.mSubmitButton.setEnabled(false);
    }

    private void setViewText(final TextView textView, final int i) {
        getOwnerActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(i);
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        int i;
        TextView textView = this.mPasswordView;
        if (z) {
            i = 144;
        } else {
            i = 128;
        }
        textView.setInputType(i | 1);
    }

    private static byte[] hexStringToByteArray(String str) {
        int length = str.length();
        byte[] bArr = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bArr[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return bArr;
    }

    private static String byteArrayToHexString(byte[] bArr) {
        char[] cArr = new char[bArr.length * 2];
        for (int i = 0; i < bArr.length; i++) {
            int i2 = bArr[i] & 255;
            int i3 = i * 2;
            cArr[i3] = hexArray[i2 >>> 4];
            cArr[i3 + 1] = hexArray[i2 & 15];
        }
        return new String(cArr);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }
}
