package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.settings.R;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.IWifiApDialogExt;
import java.nio.charset.Charset;

public class WifiApDialog extends AlertDialog implements View.OnClickListener, TextWatcher, AdapterView.OnItemSelectedListener {
    private int mBandIndex;
    private Context mContext;
    IWifiApDialogExt mExt;
    private final DialogInterface.OnClickListener mListener;
    private EditText mPassword;
    private int mSecurityTypeIndex;
    private TextView mSsid;
    private View mView;
    WifiConfiguration mWifiConfig;
    WifiManager mWifiManager;

    public WifiApDialog(Context context, DialogInterface.OnClickListener listener, WifiConfiguration wifiConfig) {
        super(context);
        this.mSecurityTypeIndex = 0;
        this.mBandIndex = 0;
        this.mListener = listener;
        this.mWifiConfig = wifiConfig;
        if (wifiConfig != null) {
            this.mSecurityTypeIndex = getSecurityTypeIndex(wifiConfig);
        }
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mContext = context;
    }

    public static int getSecurityTypeIndex(WifiConfiguration wifiConfig) {
        if (wifiConfig.allowedKeyManagement.get(4)) {
            return 1;
        }
        return 0;
    }

    public WifiConfiguration getConfig() {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = this.mSsid.getText().toString();
        config.apBand = this.mBandIndex;
        switch (this.mSecurityTypeIndex) {
            case DefaultWfcSettingsExt.RESUME:
                config.allowedKeyManagement.set(0);
                break;
            case DefaultWfcSettingsExt.PAUSE:
                config.allowedKeyManagement.set(4);
                config.allowedAuthAlgorithms.set(0);
                if (this.mPassword.length() != 0) {
                    String password = this.mPassword.getText().toString();
                    config.preSharedKey = password;
                }
                break;
        }
        return config;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ArrayAdapter<CharSequence> channelAdapter;
        this.mView = getLayoutInflater().inflate(R.layout.wifi_ap_dialog, (ViewGroup) null);
        Spinner mSecurity = (Spinner) this.mView.findViewById(R.id.security);
        final Spinner mChannel = (Spinner) this.mView.findViewById(R.id.choose_channel);
        Context context = getContext();
        this.mExt = UtilsExt.getWifiApDialogPlugin(context);
        this.mExt.setAdapter(context, mSecurity, R.array.wifi_ap_security);
        setView(this.mView);
        setInverseBackgroundForced(true);
        setTitle(R.string.wifi_tether_configure_ap_text);
        this.mView.findViewById(R.id.type).setVisibility(0);
        this.mSsid = (TextView) this.mView.findViewById(R.id.ssid);
        this.mPassword = (EditText) this.mView.findViewById(R.id.password);
        String countryCode = this.mWifiManager.getCountryCode();
        if (!this.mWifiManager.isDualBandSupported() || countryCode == null) {
            Log.i("WifiApDialog", (!this.mWifiManager.isDualBandSupported() ? "Device do not support 5GHz " : "") + (countryCode == null ? " NO country code" : "") + " forbid 5GHz");
            channelAdapter = ArrayAdapter.createFromResource(this.mContext, R.array.wifi_ap_band_config_2G_only, android.R.layout.simple_spinner_item);
            this.mWifiConfig.apBand = 0;
        } else {
            channelAdapter = ArrayAdapter.createFromResource(this.mContext, R.array.wifi_ap_band_config_full, android.R.layout.simple_spinner_item);
        }
        channelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setButton(-1, context.getString(R.string.wifi_save), this.mListener);
        setButton(-2, context.getString(R.string.wifi_cancel), this.mListener);
        if (this.mWifiConfig != null) {
            this.mSsid.setText(this.mWifiConfig.SSID);
            if (this.mWifiConfig.apBand == 0) {
                this.mBandIndex = 0;
            } else {
                this.mBandIndex = 1;
            }
            mSecurity.setSelection(this.mSecurityTypeIndex);
            if (this.mSecurityTypeIndex == 1) {
                this.mPassword.setText(this.mWifiConfig.preSharedKey);
            }
        }
        mChannel.setAdapter((SpinnerAdapter) channelAdapter);
        mChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean mInit = true;

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (!this.mInit) {
                    WifiApDialog.this.mBandIndex = position;
                    WifiApDialog.this.mWifiConfig.apBand = WifiApDialog.this.mBandIndex;
                    Log.i("WifiApDialog", "config on channelIndex : " + WifiApDialog.this.mBandIndex + " Band: " + WifiApDialog.this.mWifiConfig.apBand);
                    return;
                }
                this.mInit = false;
                mChannel.setSelection(WifiApDialog.this.mBandIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        this.mSsid.addTextChangedListener(this);
        this.mPassword.addTextChangedListener(this);
        ((CheckBox) this.mView.findViewById(R.id.show_password)).setOnClickListener(this);
        mSecurity.setOnItemSelectedListener(this);
        super.onCreate(savedInstanceState);
        showSecurityFields();
        validate();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        int i;
        super.onRestoreInstanceState(savedInstanceState);
        EditText editText = this.mPassword;
        if (((CheckBox) this.mView.findViewById(R.id.show_password)).isChecked()) {
            i = 144;
        } else {
            i = 128;
        }
        editText.setInputType(i | 1);
    }

    private void validate() {
        String mSsidString = this.mSsid.getText().toString();
        if ((this.mSsid != null && this.mSsid.length() == 0) || ((this.mSecurityTypeIndex == 1 && this.mPassword.length() < 8) || (this.mSsid != null && Charset.forName("UTF-8").encode(mSsidString).limit() > 32))) {
            getButton(-1).setEnabled(false);
        } else {
            getButton(-1).setEnabled(true);
        }
    }

    @Override
    public void onClick(View view) {
        int i;
        EditText editText = this.mPassword;
        if (((CheckBox) view).isChecked()) {
            i = 144;
        } else {
            i = 128;
        }
        editText.setInputType(i | 1);
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
        } else {
            this.mView.findViewById(R.id.fields).setVisibility(0);
        }
    }
}
