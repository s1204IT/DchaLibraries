package com.android.settings.wifi;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Handler;
import android.security.KeyStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.settings.ProxySelector;
import com.android.settings.R;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;

public class WifiConfigController implements TextWatcher, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
    private final ArrayAdapter<String> PHASE2_FULL_ADAPTER;
    private final ArrayAdapter<String> PHASE2_PEAP_ADAPTER;
    private final AccessPoint mAccessPoint;
    private int mAccessPointSecurity;
    private ArrayList<String> mCerPathString;
    private final WifiConfigUiBase mConfigUi;
    private Context mContext;
    private TextView mDns1View;
    private TextView mDns2View;
    private TextView mEapAnonymousView;
    private Spinner mEapCaCertSpinner;
    private TextView mEapIdentityView;
    private Spinner mEapMethodSpinner;
    private Spinner mEapUserCertSpinner;
    private boolean mEdit;
    private TextView mGatewayView;
    private final boolean mInXlSetupWizard;
    private TextView mIpAddressView;
    private Spinner mIpSettingsSpinner;
    private String[] mLevels;
    private TextView mNetworkPrefixLengthView;
    private TextView mPasswordView;
    private ArrayAdapter<String> mPhase2Adapter;
    private Spinner mPhase2Spinner;
    private TextView mProxyExclusionListView;
    private TextView mProxyHostView;
    private TextView mProxyPacView;
    private TextView mProxyPortView;
    private Spinner mProxySettingsSpinner;
    private Spinner mSecuritySpinner;
    private TextView mSsidView;
    private final Handler mTextViewChangedHandler;
    private final View mView;
    private int mWapiCertType;
    private Spinner mWapiCertTypeSpinner;
    private Spinner mWapiCertsSpinner;
    private Spinner mWapiPskTypeSpinner;
    private String unspecifiedCert;
    private IpConfiguration.IpAssignment mIpAssignment = IpConfiguration.IpAssignment.UNASSIGNED;
    private IpConfiguration.ProxySettings mProxySettings = IpConfiguration.ProxySettings.UNASSIGNED;
    private ProxyInfo mHttpProxy = null;
    private StaticIpConfiguration mStaticIpConfiguration = null;

    public WifiConfigController(WifiConfigUiBase parent, View view, AccessPoint accessPoint, boolean edit) {
        this.unspecifiedCert = "unspecified";
        this.mConfigUi = parent;
        this.mInXlSetupWizard = parent instanceof WifiConfigUiForSetupWizardXL;
        this.mView = view;
        this.mAccessPoint = accessPoint;
        this.mAccessPointSecurity = accessPoint == null ? 0 : accessPoint.security;
        this.mEdit = edit;
        this.mTextViewChangedHandler = new Handler();
        this.mContext = this.mConfigUi.getContext();
        Resources res = this.mContext.getResources();
        this.mLevels = res.getStringArray(R.array.wifi_signal);
        this.PHASE2_PEAP_ADAPTER = new ArrayAdapter<>(this.mContext, android.R.layout.simple_spinner_item, res.getStringArray(R.array.wifi_peap_phase2_entries));
        this.PHASE2_PEAP_ADAPTER.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.PHASE2_FULL_ADAPTER = new ArrayAdapter<>(this.mContext, android.R.layout.simple_spinner_item, res.getStringArray(R.array.wifi_phase2_entries));
        this.PHASE2_FULL_ADAPTER.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.unspecifiedCert = this.mContext.getString(R.string.wifi_unspecified);
        this.mIpSettingsSpinner = (Spinner) this.mView.findViewById(R.id.ip_settings);
        this.mIpSettingsSpinner.setOnItemSelectedListener(this);
        this.mProxySettingsSpinner = (Spinner) this.mView.findViewById(R.id.proxy_settings);
        this.mProxySettingsSpinner.setOnItemSelectedListener(this);
        if (this.mAccessPoint == null) {
            this.mConfigUi.setTitle(R.string.wifi_add_network);
            this.mSsidView = (TextView) this.mView.findViewById(R.id.ssid);
            this.mSsidView.addTextChangedListener(this);
            this.mSecuritySpinner = (Spinner) this.mView.findViewById(R.id.security);
            this.mSecuritySpinner.setOnItemSelectedListener(this);
            if (this.mInXlSetupWizard) {
                this.mView.findViewById(R.id.type_ssid).setVisibility(0);
                this.mView.findViewById(R.id.type_security).setVisibility(0);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this.mContext, R.layout.wifi_setup_custom_list_item_1, android.R.id.text1, res.getStringArray(R.array.wifi_security_no_eap));
                this.mSecuritySpinner.setAdapter((SpinnerAdapter) adapter);
            } else {
                this.mView.findViewById(R.id.type).setVisibility(0);
            }
            showIpConfigFields();
            showProxyFields();
            this.mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(0);
            ((CheckBox) this.mView.findViewById(R.id.wifi_advanced_togglebox)).setOnCheckedChangeListener(this);
            this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
        } else {
            this.mConfigUi.setTitle(this.mAccessPoint.ssid);
            ViewGroup group = (ViewGroup) this.mView.findViewById(R.id.info);
            boolean showAdvancedFields = false;
            if (this.mAccessPoint.networkId != -1) {
                WifiConfiguration config = this.mAccessPoint.getConfig();
                if (config.getIpAssignment() == IpConfiguration.IpAssignment.STATIC) {
                    this.mIpSettingsSpinner.setSelection(1);
                    showAdvancedFields = true;
                    StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                    if (staticConfig != null && staticConfig.ipAddress != null) {
                        addRow(group, R.string.wifi_ip_address, staticConfig.ipAddress.getAddress().getHostAddress());
                    }
                } else {
                    this.mIpSettingsSpinner.setSelection(0);
                }
                if (config.getProxySettings() == IpConfiguration.ProxySettings.STATIC) {
                    this.mProxySettingsSpinner.setSelection(1);
                    showAdvancedFields = true;
                } else if (config.getProxySettings() == IpConfiguration.ProxySettings.PAC) {
                    this.mProxySettingsSpinner.setSelection(2);
                    showAdvancedFields = true;
                } else {
                    this.mProxySettingsSpinner.setSelection(0);
                }
            }
            if ((this.mAccessPoint.networkId == -1 && !this.mAccessPoint.isActive()) || this.mEdit) {
                showSecurityFields();
                showIpConfigFields();
                showProxyFields();
                this.mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(0);
                ((CheckBox) this.mView.findViewById(R.id.wifi_advanced_togglebox)).setOnCheckedChangeListener(this);
                if (showAdvancedFields) {
                    ((CheckBox) this.mView.findViewById(R.id.wifi_advanced_togglebox)).setChecked(true);
                    this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(0);
                }
            }
            if (this.mEdit) {
                this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
            } else {
                NetworkInfo.DetailedState state = this.mAccessPoint.getState();
                String signalLevel = getSignalString();
                if (state == null && signalLevel != null) {
                    this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_connect));
                } else {
                    if (state != null) {
                        addRow(group, R.string.wifi_status, Summary.get(this.mConfigUi.getContext(), state, this.mAccessPoint.networkId == -1));
                    }
                    if (signalLevel != null) {
                        addRow(group, R.string.wifi_signal, signalLevel);
                    }
                    android.net.wifi.WifiInfo info = this.mAccessPoint.getInfo();
                    if (info != null && info.getLinkSpeed() != -1) {
                        addRow(group, R.string.wifi_speed, info.getLinkSpeed() + "Mbps");
                    }
                    if (info != null && info.getFrequency() != -1) {
                        int frequency = info.getFrequency();
                        String band = null;
                        if (frequency >= 2400 && frequency < 2500) {
                            band = res.getString(R.string.wifi_band_24ghz);
                        } else if (frequency >= 4900 && frequency < 5900) {
                            band = res.getString(R.string.wifi_band_5ghz);
                        } else {
                            Log.e("WifiConfigController", "Unexpected frequency " + frequency);
                        }
                        if (band != null) {
                            addRow(group, R.string.wifi_frequency, band);
                        }
                    }
                    addRow(group, R.string.wifi_security, this.mAccessPoint.getSecurityString(false));
                    this.mView.findViewById(R.id.ip_fields).setVisibility(8);
                }
                if ((this.mAccessPoint.networkId != -1 || this.mAccessPoint.isActive()) && ActivityManager.getCurrentUser() == 0) {
                    this.mConfigUi.setForgetButton(res.getString(R.string.wifi_forget));
                }
            }
        }
        if (this.mEdit || (this.mAccessPoint != null && this.mAccessPoint.getState() == null && this.mAccessPoint.getLevel() != -1)) {
            this.mConfigUi.setCancelButton(res.getString(R.string.wifi_cancel));
        } else {
            this.mConfigUi.setCancelButton(res.getString(R.string.wifi_display_options_done));
        }
        if (this.mConfigUi.getSubmitButton() != null) {
            enableSubmitIfAppropriate();
        }
    }

    private void addRow(ViewGroup group, int name, String value) {
        View row = this.mConfigUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(name);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        group.addView(row);
    }

    private String getSignalString() {
        int level = this.mAccessPoint.getLevel();
        if (level <= -1 || level >= this.mLevels.length) {
            return null;
        }
        return this.mLevels[level];
    }

    void hideSubmitButton() {
        Button submit = this.mConfigUi.getSubmitButton();
        if (submit != null) {
            submit.setVisibility(8);
        }
    }

    void enableSubmitIfAppropriate() {
        boolean enabled;
        Button submit = this.mConfigUi.getSubmitButton();
        if (submit != null) {
            boolean passwordInvalid = false;
            if (this.mPasswordView != null && ((this.mAccessPointSecurity == 1 && this.mPasswordView.length() == 0) || ((this.mAccessPointSecurity == 2 && this.mPasswordView.length() < 8) || ((this.mAccessPointSecurity == 4 && (this.mPasswordView.length() < 8 || this.mPasswordView.length() > 64)) || ((this.mAccessPointSecurity == 5 && 1 == this.mWapiCertType && (this.mPasswordView.length() < 6 || this.mPasswordView.length() > 12)) || (this.mAccessPointSecurity == 5 && -1 == this.mWapiCertsSpinner.getSelectedItemPosition())))))) {
                passwordInvalid = true;
            }
            if ((this.mSsidView == null || this.mSsidView.length() != 0) && (((this.mAccessPoint != null && this.mAccessPoint.networkId != -1) || !passwordInvalid) && ipAndProxyFieldsAreValid())) {
                enabled = true;
            } else {
                enabled = false;
            }
            submit.setEnabled(enabled);
        }
    }

    WifiConfiguration getConfig() {
        if (this.mAccessPoint != null && this.mAccessPoint.networkId != -1 && !this.mEdit) {
            return null;
        }
        WifiConfiguration config = new WifiConfiguration();
        if (this.mAccessPoint == null) {
            config.SSID = AccessPoint.convertToQuotedString(this.mSsidView.getText().toString());
            config.hiddenSSID = true;
        } else if (this.mAccessPoint.networkId == -1) {
            config.SSID = AccessPoint.convertToQuotedString(this.mAccessPoint.ssid);
        } else {
            config.networkId = this.mAccessPoint.networkId;
        }
        switch (this.mAccessPointSecurity) {
            case 0:
                config.allowedKeyManagement.set(0);
                break;
            case 1:
                config.allowedKeyManagement.set(0);
                config.allowedAuthAlgorithms.set(0);
                config.allowedAuthAlgorithms.set(1);
                if (this.mPasswordView.length() != 0) {
                    int length = this.mPasswordView.length();
                    String password = this.mPasswordView.getText().toString();
                    if ((length == 10 || length == 26 || length == 58) && password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '\"' + password + '\"';
                    }
                }
                break;
            case 2:
                config.allowedKeyManagement.set(1);
                if (this.mPasswordView.length() != 0) {
                    String password2 = this.mPasswordView.getText().toString();
                    if (password2.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password2;
                    } else {
                        config.preSharedKey = '\"' + password2 + '\"';
                    }
                }
                break;
            case 3:
                config.allowedKeyManagement.set(2);
                config.allowedKeyManagement.set(3);
                config.enterpriseConfig = new WifiEnterpriseConfig();
                int eapMethod = this.mEapMethodSpinner.getSelectedItemPosition();
                int phase2Method = this.mPhase2Spinner.getSelectedItemPosition();
                config.enterpriseConfig.setEapMethod(eapMethod);
                switch (eapMethod) {
                    case 0:
                        switch (phase2Method) {
                            case 0:
                                config.enterpriseConfig.setPhase2Method(0);
                                break;
                            case 1:
                                config.enterpriseConfig.setPhase2Method(3);
                                break;
                            case 2:
                                config.enterpriseConfig.setPhase2Method(4);
                                break;
                            default:
                                Log.e("WifiConfigController", "Unknown phase2 method" + phase2Method);
                                break;
                        }
                        break;
                    default:
                        config.enterpriseConfig.setPhase2Method(phase2Method);
                        break;
                }
                String caCert = (String) this.mEapCaCertSpinner.getSelectedItem();
                if (caCert.equals(this.unspecifiedCert)) {
                    caCert = "";
                }
                config.enterpriseConfig.setCaCertificateAlias(caCert);
                String clientCert = (String) this.mEapUserCertSpinner.getSelectedItem();
                if (clientCert.equals(this.unspecifiedCert)) {
                    clientCert = "";
                }
                config.enterpriseConfig.setClientCertificateAlias(clientCert);
                config.enterpriseConfig.setIdentity(this.mEapIdentityView.getText().toString());
                config.enterpriseConfig.setAnonymousIdentity(this.mEapAnonymousView.getText().toString());
                if (!this.mPasswordView.isShown() || this.mPasswordView.length() > 0 || this.mPasswordView.getHint() == null || !this.mPasswordView.getHint().equals(this.mConfigUi.getContext().getResources().getText(R.string.wifi_unchanged))) {
                    config.enterpriseConfig.setPassword(this.mPasswordView.getText().toString());
                }
                break;
            case 4:
                if (this.mPasswordView.length() != 0) {
                    String password3 = this.mPasswordView.getText().toString();
                    String alertMessage = "";
                    if (this.mPasswordView.length() < 8 || this.mPasswordView.length() > 64) {
                        alertMessage = "Invalid length: Expected 8..64";
                    } else {
                        int wapi_psk_type = this.mWapiPskTypeSpinner.getSelectedItemPosition();
                        if (wapi_psk_type == -1) {
                            alertMessage = "Please select the PSK type for WAPI";
                        } else if (wapi_psk_type == 1 && !password3.matches("[0-9A-Fa-f]*")) {
                            alertMessage = "\nExpected hexadecimal digits!";
                        } else {
                            config.wapiPsk = password3;
                            config.wapiPskType = WifiConfiguration.WAPI_PSK_TYPE[wapi_psk_type];
                        }
                    }
                    if (!alertMessage.equals("")) {
                        new AlertDialog.Builder(this.mConfigUi.getContext()).setTitle(R.string.error_title).setIcon(android.R.drawable.ic_dialog_alert).setMessage("Invalid password!\n" + alertMessage).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
                    }
                }
                break;
            case 5:
                if (this.mCerPathString.size() > 0) {
                    String[] CertsArray = (String[]) this.mCerPathString.toArray(new String[0]);
                    config.wapiRootCert = '\"' + CertsArray[this.mWapiCertsSpinner.getSelectedItemPosition()] + "/root.cer\"";
                    if (this.mWapiCertType == 0) {
                        config.wapiUserCert = '\"' + CertsArray[this.mWapiCertsSpinner.getSelectedItemPosition()] + "/user.cer\"";
                        config.wapiPkcs12Key = "NULL";
                    } else if (this.mWapiCertType == 1) {
                        config.wapiUserCert = '\"' + CertsArray[this.mWapiCertsSpinner.getSelectedItemPosition()] + "/user.p12\"";
                        if (this.mPasswordView.length() != 0) {
                            config.wapiPkcs12Key = '\"' + this.mPasswordView.getText().toString() + '\"';
                            if (this.mPasswordView.length() < 6 || this.mPasswordView.length() > 12) {
                                new AlertDialog.Builder(this.mConfigUi.getContext()).setTitle(R.string.error_title).setIcon(android.R.drawable.ic_dialog_alert).setMessage("Invalid length of the password: Expected 6..12").setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
                            }
                        }
                    }
                }
                break;
            default:
                return null;
        }
        config.setIpConfiguration(new IpConfiguration(this.mIpAssignment, this.mProxySettings, this.mStaticIpConfiguration, this.mHttpProxy));
        return config;
    }

    private boolean ipAndProxyFieldsAreValid() {
        Uri uri;
        int result;
        this.mIpAssignment = (this.mIpSettingsSpinner == null || this.mIpSettingsSpinner.getSelectedItemPosition() != 1) ? IpConfiguration.IpAssignment.DHCP : IpConfiguration.IpAssignment.STATIC;
        if (this.mIpAssignment == IpConfiguration.IpAssignment.STATIC) {
            this.mStaticIpConfiguration = new StaticIpConfiguration();
            int result2 = validateIpConfigFields(this.mStaticIpConfiguration);
            if (result2 != 0) {
                return false;
            }
        }
        int selectedPosition = this.mProxySettingsSpinner.getSelectedItemPosition();
        this.mProxySettings = IpConfiguration.ProxySettings.NONE;
        this.mHttpProxy = null;
        if (selectedPosition == 1 && this.mProxyHostView != null) {
            this.mProxySettings = IpConfiguration.ProxySettings.STATIC;
            String host = this.mProxyHostView.getText().toString();
            String portStr = this.mProxyPortView.getText().toString();
            String exclusionList = this.mProxyExclusionListView.getText().toString();
            int port = 0;
            try {
                port = Integer.parseInt(portStr);
                result = ProxySelector.validate(host, portStr, exclusionList);
            } catch (NumberFormatException e) {
                result = R.string.proxy_error_invalid_port;
            }
            if (result != 0) {
                return false;
            }
            this.mHttpProxy = new ProxyInfo(host, port, exclusionList);
        } else if (selectedPosition == 2 && this.mProxyPacView != null) {
            this.mProxySettings = IpConfiguration.ProxySettings.PAC;
            CharSequence uriSequence = this.mProxyPacView.getText();
            if (!TextUtils.isEmpty(uriSequence) && (uri = Uri.parse(uriSequence.toString())) != null) {
                this.mHttpProxy = new ProxyInfo(uri);
            }
            return false;
        }
        return true;
    }

    private Inet4Address getIPv4Address(String text) {
        try {
            return (Inet4Address) NetworkUtils.numericToInetAddress(text);
        } catch (ClassCastException | IllegalArgumentException e) {
            return null;
        }
    }

    private int validateIpConfigFields(StaticIpConfiguration staticIpConfiguration) {
        Inet4Address inetAddr;
        if (this.mIpAddressView == null) {
            return 0;
        }
        String ipAddr = this.mIpAddressView.getText().toString();
        if (TextUtils.isEmpty(ipAddr) || (inetAddr = getIPv4Address(ipAddr)) == null) {
            return R.string.wifi_ip_settings_invalid_ip_address;
        }
        int networkPrefixLength = -1;
        try {
            networkPrefixLength = Integer.parseInt(this.mNetworkPrefixLengthView.getText().toString());
            if (networkPrefixLength < 0 || networkPrefixLength > 32) {
                return R.string.wifi_ip_settings_invalid_network_prefix_length;
            }
            staticIpConfiguration.ipAddress = new LinkAddress(inetAddr, networkPrefixLength);
        } catch (NumberFormatException e) {
            this.mNetworkPrefixLengthView.setText(this.mConfigUi.getContext().getString(R.string.wifi_network_prefix_length_hint));
        }
        String gateway = this.mGatewayView.getText().toString();
        if (TextUtils.isEmpty(gateway)) {
            try {
                InetAddress netPart = NetworkUtils.getNetworkPart(inetAddr, networkPrefixLength);
                byte[] addr = netPart.getAddress();
                addr[addr.length - 1] = 1;
                this.mGatewayView.setText(InetAddress.getByAddress(addr).getHostAddress());
            } catch (RuntimeException e2) {
            } catch (UnknownHostException e3) {
            }
        } else {
            InetAddress gatewayAddr = getIPv4Address(gateway);
            if (gatewayAddr == null) {
                return R.string.wifi_ip_settings_invalid_gateway;
            }
            staticIpConfiguration.gateway = gatewayAddr;
        }
        String dns = this.mDns1View.getText().toString();
        if (TextUtils.isEmpty(dns)) {
            this.mDns1View.setText(this.mConfigUi.getContext().getString(R.string.wifi_dns1_hint));
        } else {
            Inet4Address iPv4Address = getIPv4Address(dns);
            if (iPv4Address == null) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(iPv4Address);
        }
        if (this.mDns2View.length() > 0) {
            Inet4Address iPv4Address2 = getIPv4Address(this.mDns2View.getText().toString());
            if (iPv4Address2 == null) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(iPv4Address2);
        }
        return 0;
    }

    private void showSecurityFields() {
        if (!this.mInXlSetupWizard || ((WifiSettingsForSetupWizardXL) this.mConfigUi.getContext()).initSecurityFields(this.mView, this.mAccessPointSecurity)) {
            if (this.mAccessPointSecurity == 0) {
                this.mView.findViewById(R.id.security_fields).setVisibility(8);
                return;
            }
            this.mView.findViewById(R.id.security_fields).setVisibility(0);
            if (this.mAccessPointSecurity != 4 && this.mAccessPointSecurity != 5) {
                this.mView.findViewById(R.id.wapi).setVisibility(8);
            } else {
                this.mView.findViewById(R.id.wapi).setVisibility(0);
                WifiConfiguration config = null;
                if (this.mAccessPoint != null && this.mAccessPoint.networkId != -1) {
                    config = this.mAccessPoint.getConfig();
                }
                if (this.mAccessPointSecurity == 5) {
                    if (this.mWapiCertTypeSpinner == null) {
                        this.mWapiCertTypeSpinner = (Spinner) this.mView.findViewById(R.id.wapi_cert_type_spinner);
                        ((Spinner) this.mView.findViewById(R.id.wapi_cert_type_spinner)).setOnItemSelectedListener(this);
                        if (config != null) {
                            if (config.wapiPkcs12Key != null) {
                                this.mWapiCertTypeSpinner.setSelection(1);
                            } else if (config.wapiRootCert != null) {
                                this.mWapiCertTypeSpinner.setSelection(0);
                            }
                        }
                    }
                    this.mView.findViewById(R.id.wapi_psk).setVisibility(8);
                    this.mView.findViewById(R.id.wapi_cert).setVisibility(0);
                    this.mWapiCertType = this.mWapiCertTypeSpinner.getSelectedItemPosition();
                    if (this.mWapiCertType == 0) {
                        this.mView.findViewById(R.id.password_layout).setVisibility(8);
                    } else if (this.mWapiCertType == 1) {
                        this.mView.findViewById(R.id.password_layout).setVisibility(0);
                    } else {
                        Log.e("WifiConfigController", "Invalid WAPI cert type " + this.mWapiCertType);
                    }
                    if (this.mWapiCertsSpinner == null) {
                        this.mWapiCertsSpinner = (Spinner) this.mView.findViewById(R.id.wapi_certs_spinner);
                    }
                    loadWapiCerts();
                    if (config != null && this.mCerPathString.size() > 0) {
                        String[] CertsArray = (String[]) this.mCerPathString.toArray(new String[0]);
                        int position = 0;
                        while (true) {
                            if (position >= this.mCerPathString.size()) {
                                break;
                            }
                            if (!config.wapiRootCert.equals('\"' + CertsArray[position] + "/root.cer\"")) {
                                position++;
                            } else {
                                this.mWapiCertsSpinner.setSelection(position);
                                break;
                            }
                        }
                    }
                }
                if (this.mAccessPointSecurity == 4) {
                    if (this.mWapiPskTypeSpinner == null) {
                        this.mWapiPskTypeSpinner = (Spinner) this.mView.findViewById(R.id.wapi_psk_type_spinner);
                        if (config != null && config.wapiPskType != null) {
                            int wapi_psk_type_position = -1;
                            if (config.wapiPskType.equals(WifiConfiguration.WAPI_PSK_TYPE[0])) {
                                wapi_psk_type_position = 0;
                            } else if (config.wapiPskType.equals(WifiConfiguration.WAPI_PSK_TYPE[1])) {
                                wapi_psk_type_position = 1;
                            } else {
                                Log.e("WifiConfigController", "Unknown WAPI PSK type: " + config.wapiPskType);
                            }
                            if (wapi_psk_type_position != -1) {
                                this.mWapiPskTypeSpinner.setSelection(wapi_psk_type_position);
                            }
                        }
                    }
                    this.mView.findViewById(R.id.wapi_psk).setVisibility(0);
                    this.mView.findViewById(R.id.wapi_cert).setVisibility(8);
                    this.mView.findViewById(R.id.password_layout).setVisibility(0);
                }
            }
            if (this.mPasswordView == null) {
                this.mPasswordView = (TextView) this.mView.findViewById(R.id.password);
                this.mPasswordView.addTextChangedListener(this);
                ((CheckBox) this.mView.findViewById(R.id.show_password)).setOnCheckedChangeListener(this);
                if (this.mAccessPoint != null && this.mAccessPoint.networkId != -1) {
                    this.mPasswordView.setHint(R.string.wifi_unchanged);
                }
            }
            if (this.mAccessPointSecurity != 3) {
                this.mView.findViewById(R.id.eap).setVisibility(8);
                return;
            }
            this.mView.findViewById(R.id.eap).setVisibility(0);
            if (this.mEapMethodSpinner == null) {
                this.mEapMethodSpinner = (Spinner) this.mView.findViewById(R.id.method);
                this.mEapMethodSpinner.setOnItemSelectedListener(this);
                this.mPhase2Spinner = (Spinner) this.mView.findViewById(R.id.phase2);
                this.mEapCaCertSpinner = (Spinner) this.mView.findViewById(R.id.ca_cert);
                this.mEapUserCertSpinner = (Spinner) this.mView.findViewById(R.id.user_cert);
                this.mEapIdentityView = (TextView) this.mView.findViewById(R.id.identity);
                this.mEapAnonymousView = (TextView) this.mView.findViewById(R.id.anonymous);
                loadCertificates(this.mEapCaCertSpinner, "CACERT_");
                loadCertificates(this.mEapUserCertSpinner, "USRPKEY_");
                if (this.mAccessPoint != null && this.mAccessPoint.networkId != -1) {
                    WifiEnterpriseConfig enterpriseConfig = this.mAccessPoint.getConfig().enterpriseConfig;
                    int eapMethod = enterpriseConfig.getEapMethod();
                    int phase2Method = enterpriseConfig.getPhase2Method();
                    this.mEapMethodSpinner.setSelection(eapMethod);
                    showEapFieldsByMethod(eapMethod);
                    switch (eapMethod) {
                        case 0:
                            switch (phase2Method) {
                                case 0:
                                    this.mPhase2Spinner.setSelection(0);
                                    break;
                                case 1:
                                case 2:
                                default:
                                    Log.e("WifiConfigController", "Invalid phase 2 method " + phase2Method);
                                    break;
                                case 3:
                                    this.mPhase2Spinner.setSelection(1);
                                    break;
                                case 4:
                                    this.mPhase2Spinner.setSelection(2);
                                    break;
                            }
                            break;
                        default:
                            this.mPhase2Spinner.setSelection(phase2Method);
                            break;
                    }
                    setSelection(this.mEapCaCertSpinner, enterpriseConfig.getCaCertificateAlias());
                    setSelection(this.mEapUserCertSpinner, enterpriseConfig.getClientCertificateAlias());
                    this.mEapIdentityView.setText(enterpriseConfig.getIdentity());
                    this.mEapAnonymousView.setText(enterpriseConfig.getAnonymousIdentity());
                    return;
                }
                this.mEapMethodSpinner.setSelection(0);
                showEapFieldsByMethod(0);
                return;
            }
            showEapFieldsByMethod(this.mEapMethodSpinner.getSelectedItemPosition());
        }
    }

    private void showEapFieldsByMethod(int eapMethod) {
        this.mView.findViewById(R.id.l_method).setVisibility(0);
        this.mView.findViewById(R.id.l_identity).setVisibility(0);
        this.mView.findViewById(R.id.l_ca_cert).setVisibility(0);
        this.mView.findViewById(R.id.password_layout).setVisibility(0);
        this.mView.findViewById(R.id.show_password_layout).setVisibility(0);
        if (this.mAccessPoint != null && this.mAccessPoint.networkId != -1) {
            WifiEnterpriseConfig enterpriseConfig = this.mAccessPoint.getConfig().enterpriseConfig;
            int savedEapMethod = enterpriseConfig.getEapMethod();
            if (eapMethod != savedEapMethod) {
                resetAllEapFields(null);
            } else {
                resetAllEapFields(enterpriseConfig);
            }
        }
        this.mConfigUi.getContext();
        switch (eapMethod) {
            case 0:
                if (this.mPhase2Adapter != this.PHASE2_PEAP_ADAPTER) {
                    this.mPhase2Adapter = this.PHASE2_PEAP_ADAPTER;
                    this.mPhase2Spinner.setAdapter((SpinnerAdapter) this.mPhase2Adapter);
                }
                this.mView.findViewById(R.id.l_phase2).setVisibility(0);
                this.mView.findViewById(R.id.l_anonymous).setVisibility(0);
                setUserCertInvisible();
                break;
            case 1:
                this.mView.findViewById(R.id.l_user_cert).setVisibility(0);
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setPasswordInvisible();
                break;
            case 2:
                if (this.mPhase2Adapter != this.PHASE2_FULL_ADAPTER) {
                    this.mPhase2Adapter = this.PHASE2_FULL_ADAPTER;
                    this.mPhase2Spinner.setAdapter((SpinnerAdapter) this.mPhase2Adapter);
                }
                this.mView.findViewById(R.id.l_phase2).setVisibility(0);
                this.mView.findViewById(R.id.l_anonymous).setVisibility(0);
                setUserCertInvisible();
                break;
            case 3:
                setPhase2Invisible();
                setCaCertInvisible();
                setAnonymousIdentInvisible();
                setUserCertInvisible();
                break;
            case 4:
            case 5:
            case 6:
                setPhase2Invisible();
                setCaCertInvisible();
                setUserCertInvisible();
                setIdentInvisible();
                setAnonymousIdentInvisible();
                setPasswordInvisible();
                break;
        }
    }

    private void resetAllEapFields(WifiEnterpriseConfig enterpriseConfig) {
        if (enterpriseConfig == null) {
            this.mEapCaCertSpinner.setSelection(0);
            this.mPhase2Spinner.setSelection(0);
            this.mEapUserCertSpinner.setSelection(0);
            this.mEapIdentityView.setText("");
            this.mEapAnonymousView.setText("");
            this.mPasswordView.setHint("");
            this.mPasswordView.setText("");
        }
        setSelection(this.mEapCaCertSpinner, enterpriseConfig.getCaCertificateAlias());
        setSelection(this.mEapUserCertSpinner, enterpriseConfig.getClientCertificateAlias());
        this.mEapIdentityView.setText(enterpriseConfig.getIdentity());
        this.mEapAnonymousView.setText(enterpriseConfig.getAnonymousIdentity());
        this.mPasswordView.setHint(R.string.wifi_unchanged);
        int phase2Method = enterpriseConfig.getPhase2Method();
        int savedEapMethod = enterpriseConfig.getEapMethod();
        switch (savedEapMethod) {
            case 0:
                switch (phase2Method) {
                    case 0:
                        this.mPhase2Spinner.setSelection(0);
                        break;
                    case 1:
                    case 2:
                    default:
                        Log.e("WifiConfigController", "Invalid phase 2 method " + phase2Method);
                        break;
                    case 3:
                        this.mPhase2Spinner.setSelection(1);
                        break;
                    case 4:
                        this.mPhase2Spinner.setSelection(2);
                        break;
                }
                break;
            default:
                this.mPhase2Spinner.setSelection(phase2Method);
                break;
        }
    }

    private void setPhase2Invisible() {
        this.mView.findViewById(R.id.l_phase2).setVisibility(8);
        this.mPhase2Spinner.setSelection(0);
    }

    private void setCaCertInvisible() {
        this.mView.findViewById(R.id.l_ca_cert).setVisibility(8);
        this.mEapCaCertSpinner.setSelection(0);
    }

    private void setUserCertInvisible() {
        this.mView.findViewById(R.id.l_user_cert).setVisibility(8);
        this.mEapUserCertSpinner.setSelection(0);
    }

    private void setIdentInvisible() {
        this.mView.findViewById(R.id.l_identity).setVisibility(8);
        this.mEapIdentityView.setText("");
    }

    private void setAnonymousIdentInvisible() {
        this.mView.findViewById(R.id.l_anonymous).setVisibility(8);
        this.mEapAnonymousView.setText("");
    }

    private void setPasswordInvisible() {
        this.mPasswordView.setText("");
        this.mView.findViewById(R.id.password_layout).setVisibility(8);
        this.mView.findViewById(R.id.show_password_layout).setVisibility(8);
    }

    private void showIpConfigFields() {
        StaticIpConfiguration staticConfig;
        WifiConfiguration config = null;
        this.mView.findViewById(R.id.ip_fields).setVisibility(0);
        if (this.mAccessPoint != null && this.mAccessPoint.networkId != -1) {
            config = this.mAccessPoint.getConfig();
        }
        if (this.mIpSettingsSpinner.getSelectedItemPosition() == 1) {
            this.mView.findViewById(R.id.staticip).setVisibility(0);
            if (this.mIpAddressView == null) {
                this.mIpAddressView = (TextView) this.mView.findViewById(R.id.ipaddress);
                this.mIpAddressView.addTextChangedListener(this);
                this.mGatewayView = (TextView) this.mView.findViewById(R.id.gateway);
                this.mGatewayView.addTextChangedListener(this);
                this.mNetworkPrefixLengthView = (TextView) this.mView.findViewById(R.id.network_prefix_length);
                this.mNetworkPrefixLengthView.addTextChangedListener(this);
                this.mDns1View = (TextView) this.mView.findViewById(R.id.dns1);
                this.mDns1View.addTextChangedListener(this);
                this.mDns2View = (TextView) this.mView.findViewById(R.id.dns2);
                this.mDns2View.addTextChangedListener(this);
            }
            if (config != null && (staticConfig = config.getStaticIpConfiguration()) != null) {
                if (staticConfig.ipAddress != null) {
                    this.mIpAddressView.setText(staticConfig.ipAddress.getAddress().getHostAddress());
                    this.mNetworkPrefixLengthView.setText(Integer.toString(staticConfig.ipAddress.getNetworkPrefixLength()));
                }
                if (staticConfig.gateway != null) {
                    this.mGatewayView.setText(staticConfig.gateway.getHostAddress());
                }
                Iterator<InetAddress> dnsIterator = staticConfig.dnsServers.iterator();
                if (dnsIterator.hasNext()) {
                    this.mDns1View.setText(dnsIterator.next().getHostAddress());
                }
                if (dnsIterator.hasNext()) {
                    this.mDns2View.setText(dnsIterator.next().getHostAddress());
                    return;
                }
                return;
            }
            return;
        }
        this.mView.findViewById(R.id.staticip).setVisibility(8);
    }

    private void showProxyFields() {
        ProxyInfo proxyInfo;
        ProxyInfo proxyProperties;
        WifiConfiguration config = null;
        this.mView.findViewById(R.id.proxy_settings_fields).setVisibility(0);
        if (this.mAccessPoint != null && this.mAccessPoint.networkId != -1) {
            config = this.mAccessPoint.getConfig();
        }
        if (this.mProxySettingsSpinner.getSelectedItemPosition() == 1) {
            setVisibility(R.id.proxy_warning_limited_support, 0);
            setVisibility(R.id.proxy_fields, 0);
            setVisibility(R.id.proxy_pac_field, 8);
            if (this.mProxyHostView == null) {
                this.mProxyHostView = (TextView) this.mView.findViewById(R.id.proxy_hostname);
                this.mProxyHostView.addTextChangedListener(this);
                this.mProxyPortView = (TextView) this.mView.findViewById(R.id.proxy_port);
                this.mProxyPortView.addTextChangedListener(this);
                this.mProxyExclusionListView = (TextView) this.mView.findViewById(R.id.proxy_exclusionlist);
                this.mProxyExclusionListView.addTextChangedListener(this);
            }
            if (config != null && (proxyProperties = config.getHttpProxy()) != null) {
                this.mProxyHostView.setText(proxyProperties.getHost());
                this.mProxyPortView.setText(Integer.toString(proxyProperties.getPort()));
                this.mProxyExclusionListView.setText(proxyProperties.getExclusionListAsString());
                return;
            }
            return;
        }
        if (this.mProxySettingsSpinner.getSelectedItemPosition() == 2) {
            setVisibility(R.id.proxy_warning_limited_support, 8);
            setVisibility(R.id.proxy_fields, 8);
            setVisibility(R.id.proxy_pac_field, 0);
            if (this.mProxyPacView == null) {
                this.mProxyPacView = (TextView) this.mView.findViewById(R.id.proxy_pac);
                this.mProxyPacView.addTextChangedListener(this);
            }
            if (config != null && (proxyInfo = config.getHttpProxy()) != null) {
                this.mProxyPacView.setText(proxyInfo.getPacFileUrl().toString());
                return;
            }
            return;
        }
        setVisibility(R.id.proxy_warning_limited_support, 8);
        setVisibility(R.id.proxy_fields, 8);
        setVisibility(R.id.proxy_pac_field, 8);
    }

    private void setVisibility(int id, int visibility) {
        View v = this.mView.findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    private void loadCertificates(Spinner spinner, String prefix) {
        String[] certs;
        Context context = this.mConfigUi.getContext();
        String[] certs2 = KeyStore.getInstance().saw(prefix, 1010);
        if (certs2 == null || certs2.length == 0) {
            certs = new String[]{this.unspecifiedCert};
        } else {
            String[] array = new String[certs2.length + 1];
            array[0] = this.unspecifiedCert;
            System.arraycopy(certs2, 0, array, 1, certs2.length);
            certs = array;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, certs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) adapter);
    }

    private void setSelection(Spinner spinner, String value) {
        if (value != null) {
            ArrayAdapter<String> adapter = (ArrayAdapter) spinner.getAdapter();
            for (int i = adapter.getCount() - 1; i >= 0; i--) {
                if (value.equals(adapter.getItem(i))) {
                    spinner.setSelection(i);
                    return;
                }
            }
        }
    }

    private void loadWapiCerts() {
        File UserCertFile;
        Context context = this.mConfigUi.getContext();
        ArrayList<String> cerString = new ArrayList<>();
        this.mCerPathString = new ArrayList<>();
        File certificatePath = new File("/data/wapi_certs/");
        try {
            if (certificatePath.isDirectory()) {
                File[] certificateList = certificatePath.listFiles();
                for (int i = 0; i < certificateList.length; i++) {
                    if (certificateList[i].isDirectory()) {
                        File RootCertFile = new File(certificateList[i].getAbsoluteFile() + "/root.cer");
                        if (this.mWapiCertType == 0) {
                            UserCertFile = new File(certificateList[i].getAbsoluteFile() + "/user.cer");
                        } else if (this.mWapiCertType == 1) {
                            UserCertFile = new File(certificateList[i].getAbsoluteFile() + "/user.p12");
                        } else {
                            Log.e("WifiConfigController", "Invalid WAPI cert type " + this.mWapiCertType);
                            return;
                        }
                        if (RootCertFile.exists() && UserCertFile.exists()) {
                            cerString.add(certificateList[i].getName());
                            this.mCerPathString.add(certificateList[i].getAbsoluteFile().toString());
                        }
                    }
                }
                ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, (String[]) cerString.toArray(new String[0]));
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                this.mWapiCertsSpinner.setAdapter((SpinnerAdapter) adapter);
            }
        } catch (Exception e) {
            new AlertDialog.Builder(context).setTitle(R.string.error_title).setIcon(android.R.drawable.ic_dialog_alert).setMessage(e.toString()).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
        }
    }

    public boolean isEdit() {
        return this.mEdit;
    }

    @Override
    public void afterTextChanged(Editable s) {
        this.mTextViewChangedHandler.post(new Runnable() {
            @Override
            public void run() {
                WifiConfigController.this.enableSubmitIfAppropriate();
            }
        });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        if (view.getId() == R.id.show_password) {
            int pos = this.mPasswordView.getSelectionEnd();
            this.mPasswordView.setInputType((isChecked ? 144 : 128) | 1);
            if (pos >= 0) {
                ((EditText) this.mPasswordView).setSelection(pos);
                return;
            }
            return;
        }
        if (view.getId() == R.id.wifi_advanced_togglebox) {
            if (isChecked) {
                this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(0);
            } else {
                this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(8);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == this.mSecuritySpinner) {
            this.mAccessPointSecurity = position;
            showSecurityFields();
        } else if (parent == this.mEapMethodSpinner) {
            showSecurityFields();
        } else if (parent == this.mProxySettingsSpinner) {
            showProxyFields();
        } else if (parent == this.mWapiCertTypeSpinner) {
            this.mAccessPointSecurity = 5;
            showSecurityFields();
        } else {
            showIpConfigFields();
        }
        enableSubmitIfAppropriate();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
