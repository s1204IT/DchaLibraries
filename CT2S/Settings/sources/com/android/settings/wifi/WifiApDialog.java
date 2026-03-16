package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.settings.R;

public class WifiApDialog extends AlertDialog implements TextWatcher, View.OnClickListener, AdapterView.OnItemSelectedListener {
    private int mChannelIndex;
    private TextView mHint;
    private final DialogInterface.OnClickListener mListener;
    private EditText mPassword;
    private int mSecurityTypeIndex;
    private TextView mSsid;
    private View mView;
    WifiConfiguration mWifiConfig;

    public WifiApDialog(Context context, DialogInterface.OnClickListener listener, WifiConfiguration wifiConfig) {
        super(context);
        this.mSecurityTypeIndex = 0;
        this.mChannelIndex = 0;
        this.mListener = listener;
        this.mWifiConfig = wifiConfig;
        if (wifiConfig != null) {
            this.mSecurityTypeIndex = getSecurityTypeIndex(wifiConfig);
            this.mChannelIndex = wifiConfig.wifiApChannelIndex;
        }
    }

    public static int getSecurityTypeIndex(WifiConfiguration wifiConfig) {
        if (wifiConfig.allowedKeyManagement.get(4)) {
            return 1;
        }
        return wifiConfig.wepKeys[0] != null ? 2 : 0;
    }

    public WifiConfiguration getConfig() {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = this.mSsid.getText().toString();
        config.wifiApChannelIndex = this.mChannelIndex;
        switch (this.mSecurityTypeIndex) {
            case 0:
                config.allowedKeyManagement.set(0);
                break;
            case 1:
                config.allowedKeyManagement.set(4);
                config.allowedAuthAlgorithms.set(0);
                if (this.mPassword.length() != 0) {
                    String password = this.mPassword.getText().toString();
                    config.preSharedKey = password;
                }
                break;
            case 2:
                config.allowedKeyManagement.set(0);
                config.allowedAuthAlgorithms.set(1);
                if (this.mPassword.length() != 0) {
                    String password2 = this.mPassword.getText().toString();
                    config.wepKeys[0] = password2;
                    config.wepTxKeyIndex = 0;
                }
                break;
        }
        return config;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.mView = getLayoutInflater().inflate(R.layout.wifi_ap_dialog, (ViewGroup) null);
        Spinner mSecurity = (Spinner) this.mView.findViewById(R.id.security);
        Spinner mChannel = (Spinner) this.mView.findViewById(R.id.channel);
        setView(this.mView);
        setInverseBackgroundForced(true);
        Context context = getContext();
        setTitle(R.string.wifi_tether_configure_ap_text);
        this.mView.findViewById(R.id.type).setVisibility(0);
        this.mSsid = (TextView) this.mView.findViewById(R.id.ssid);
        this.mPassword = (EditText) this.mView.findViewById(R.id.password);
        setButton(-1, context.getString(R.string.wifi_save), this.mListener);
        setButton(-2, context.getString(R.string.wifi_cancel), this.mListener);
        if (this.mWifiConfig != null) {
            this.mSsid.setText(this.mWifiConfig.SSID);
            mChannel.setSelection(this.mChannelIndex);
            mSecurity.setSelection(this.mSecurityTypeIndex);
            if (this.mSecurityTypeIndex == 1) {
                this.mPassword.setText(this.mWifiConfig.preSharedKey);
            } else if (this.mSecurityTypeIndex == 2) {
                this.mPassword.setText(this.mWifiConfig.wepKeys[0]);
            }
        }
        this.mSsid.addTextChangedListener(this);
        this.mPassword.addTextChangedListener(this);
        ((CheckBox) this.mView.findViewById(R.id.show_password)).setOnClickListener(this);
        mSecurity.setOnItemSelectedListener(this);
        mChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                WifiApDialog.this.mChannelIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        super.onCreate(savedInstanceState);
        showSecurityFields();
        validate();
    }

    private void validate() {
        if ((this.mSsid != null && this.mSsid.length() == 0) || ((this.mSecurityTypeIndex == 1 && this.mPassword.length() < 8) || (this.mSecurityTypeIndex == 2 && this.mPassword.length() != 13))) {
            getButton(-1).setEnabled(false);
        } else {
            getButton(-1).setEnabled(true);
        }
    }

    @Override
    public void onClick(View view) {
        this.mPassword.setInputType((((CheckBox) view).isChecked() ? 144 : 128) | 1);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        validate();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        this.mSecurityTypeIndex = position;
        showSecurityFields();
        validate();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void showSecurityFields() {
        if (this.mSecurityTypeIndex == 0) {
            this.mView.findViewById(R.id.fields).setVisibility(8);
            return;
        }
        this.mView.findViewById(R.id.fields).setVisibility(0);
        this.mHint = (TextView) this.mView.findViewById(R.id.hint);
        if (this.mSecurityTypeIndex == 2) {
            this.mHint.setText("The password must have 13 characters.");
        } else {
            this.mHint.setText("The password must at least have 8 characters.");
        }
    }
}
