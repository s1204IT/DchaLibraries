package com.android.settings.wifi;

import android.content.Context;
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
import android.os.UserManager;
import android.security.KeyStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
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
import com.android.settings.Utils;
import com.android.settingslib.wifi.AccessPoint;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class WifiConfigController implements TextWatcher, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener, TextView.OnEditorActionListener, View.OnKeyListener {
    private final AccessPoint mAccessPoint;
    private int mAccessPointSecurity;
    private final WifiConfigUiBase mConfigUi;
    private Context mContext;
    private TextView mDns1View;
    private TextView mDns2View;
    private String mDoNotProvideEapUserCertString;
    private String mDoNotValidateEapServerString;
    private TextView mEapAnonymousView;
    private Spinner mEapCaCertSpinner;
    private TextView mEapDomainView;
    private TextView mEapIdentityView;
    private Spinner mEapMethodSpinner;
    private Spinner mEapUserCertSpinner;
    private TextView mGatewayView;
    private TextView mIpAddressView;
    private Spinner mIpSettingsSpinner;
    private String[] mLevels;
    private int mMode;
    private String mMultipleCertSetString;
    private TextView mNetworkPrefixLengthView;
    private TextView mPasswordView;
    private ArrayAdapter<String> mPhase2Adapter;
    private final ArrayAdapter<String> mPhase2FullAdapter;
    private final ArrayAdapter<String> mPhase2PeapAdapter;
    private Spinner mPhase2Spinner;
    private TextView mProxyExclusionListView;
    private TextView mProxyHostView;
    private TextView mProxyPacView;
    private TextView mProxyPortView;
    private Spinner mProxySettingsSpinner;
    private Spinner mSecuritySpinner;
    private CheckBox mSharedCheckBox;
    private TextView mSsidView;
    private final Handler mTextViewChangedHandler;
    private String mUnspecifiedCertString;
    private String mUseSystemCertsString;
    private final View mView;
    private IpConfiguration.IpAssignment mIpAssignment = IpConfiguration.IpAssignment.UNASSIGNED;
    private IpConfiguration.ProxySettings mProxySettings = IpConfiguration.ProxySettings.UNASSIGNED;
    private ProxyInfo mHttpProxy = null;
    private StaticIpConfiguration mStaticIpConfiguration = null;

    public WifiConfigController(WifiConfigUiBase parent, View view, AccessPoint accessPoint, int mode) {
        this.mConfigUi = parent;
        this.mView = view;
        this.mAccessPoint = accessPoint;
        this.mAccessPointSecurity = accessPoint == null ? 0 : accessPoint.getSecurity();
        this.mMode = mode;
        this.mTextViewChangedHandler = new Handler();
        this.mContext = this.mConfigUi.getContext();
        Resources res = this.mContext.getResources();
        this.mLevels = res.getStringArray(R.array.wifi_signal);
        this.mPhase2PeapAdapter = new ArrayAdapter<>(this.mContext, android.R.layout.simple_spinner_item, res.getStringArray(R.array.wifi_peap_phase2_entries));
        this.mPhase2PeapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mPhase2FullAdapter = new ArrayAdapter<>(this.mContext, android.R.layout.simple_spinner_item, res.getStringArray(R.array.wifi_phase2_entries));
        this.mPhase2FullAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mUnspecifiedCertString = this.mContext.getString(R.string.wifi_unspecified);
        this.mMultipleCertSetString = this.mContext.getString(R.string.wifi_multiple_cert_added);
        this.mUseSystemCertsString = this.mContext.getString(R.string.wifi_use_system_certs);
        this.mDoNotProvideEapUserCertString = this.mContext.getString(R.string.wifi_do_not_provide_eap_user_cert);
        this.mDoNotValidateEapServerString = this.mContext.getString(R.string.wifi_do_not_validate_eap_server);
        this.mIpSettingsSpinner = (Spinner) this.mView.findViewById(R.id.ip_settings);
        this.mIpSettingsSpinner.setOnItemSelectedListener(this);
        this.mProxySettingsSpinner = (Spinner) this.mView.findViewById(R.id.proxy_settings);
        this.mProxySettingsSpinner.setOnItemSelectedListener(this);
        this.mSharedCheckBox = (CheckBox) this.mView.findViewById(R.id.shared);
        if (this.mAccessPoint == null) {
            this.mConfigUi.setTitle(R.string.wifi_add_network);
            this.mSsidView = (TextView) this.mView.findViewById(R.id.ssid);
            this.mSsidView.addTextChangedListener(this);
            this.mSecuritySpinner = (Spinner) this.mView.findViewById(R.id.security);
            this.mSecuritySpinner.setOnItemSelectedListener(this);
            this.mView.findViewById(R.id.type).setVisibility(0);
            showIpConfigFields();
            showProxyFields();
            this.mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(0);
            ((CheckBox) this.mView.findViewById(R.id.wifi_advanced_togglebox)).setOnCheckedChangeListener(this);
            this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
        } else {
            this.mConfigUi.setTitle(this.mAccessPoint.getSsid());
            ViewGroup group = (ViewGroup) this.mView.findViewById(R.id.info);
            boolean showAdvancedFields = false;
            if (this.mAccessPoint.isSaved()) {
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
                this.mSharedCheckBox.setEnabled(config.shared);
                showAdvancedFields = config.shared ? showAdvancedFields : true;
                if (config.getProxySettings() == IpConfiguration.ProxySettings.STATIC) {
                    this.mProxySettingsSpinner.setSelection(1);
                    showAdvancedFields = true;
                } else if (config.getProxySettings() == IpConfiguration.ProxySettings.PAC) {
                    this.mProxySettingsSpinner.setSelection(2);
                    showAdvancedFields = true;
                } else {
                    this.mProxySettingsSpinner.setSelection(0);
                }
                if (config != null && config.isPasspoint()) {
                    addRow(group, R.string.passpoint_label, String.format(this.mContext.getString(R.string.passpoint_content), config.providerFriendlyName));
                }
            }
            if ((!this.mAccessPoint.isSaved() && !this.mAccessPoint.isActive()) || this.mMode != 0) {
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
            if (this.mMode == 2) {
                this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
            } else if (this.mMode == 1) {
                this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_connect));
            } else {
                NetworkInfo.DetailedState state = this.mAccessPoint.getDetailedState();
                String signalLevel = getSignalString();
                if (state == null && signalLevel != null) {
                    this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_connect));
                } else {
                    if (state != null) {
                        boolean isEphemeral = this.mAccessPoint.isEphemeral();
                        WifiConfiguration config2 = this.mAccessPoint.getConfig();
                        String providerFriendlyName = null;
                        if (config2 != null && config2.isPasspoint()) {
                            providerFriendlyName = config2.providerFriendlyName;
                        }
                        String summary = AccessPoint.getSummary(this.mConfigUi.getContext(), state, isEphemeral, providerFriendlyName);
                        addRow(group, R.string.wifi_status, summary);
                    }
                    if (signalLevel != null) {
                        addRow(group, R.string.wifi_signal, signalLevel);
                    }
                    android.net.wifi.WifiInfo info = this.mAccessPoint.getInfo();
                    if (info != null && info.getLinkSpeed() != -1) {
                        addRow(group, R.string.wifi_speed, String.format(res.getString(R.string.link_speed), Integer.valueOf(info.getLinkSpeed())));
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
                if (this.mAccessPoint.isSaved() || this.mAccessPoint.isActive()) {
                    this.mConfigUi.setForgetButton(res.getString(R.string.wifi_forget));
                }
            }
        }
        if (!UserManager.isSplitSystemUser()) {
            this.mSharedCheckBox.setVisibility(8);
        }
        this.mConfigUi.setCancelButton(res.getString(R.string.wifi_cancel));
        if (this.mConfigUi.getSubmitButton() == null) {
            return;
        }
        enableSubmitIfAppropriate();
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

    void hideForgetButton() {
        Button forget = this.mConfigUi.getForgetButton();
        if (forget == null) {
            return;
        }
        forget.setVisibility(8);
    }

    void hideSubmitButton() {
        Button submit = this.mConfigUi.getSubmitButton();
        if (submit == null) {
            return;
        }
        submit.setVisibility(8);
    }

    void enableSubmitIfAppropriate() {
        Button submit = this.mConfigUi.getSubmitButton();
        if (submit == null) {
            return;
        }
        submit.setEnabled(isSubmittable());
    }

    boolean isSubmittable() {
        boolean enabled;
        boolean passwordInvalid = false;
        if (this.mPasswordView != null && ((this.mAccessPointSecurity == 1 && this.mPasswordView.length() == 0) || (this.mAccessPointSecurity == 2 && this.mPasswordView.length() < 8))) {
            passwordInvalid = true;
        }
        if ((this.mSsidView != null && this.mSsidView.length() == 0) || ((this.mAccessPoint == null || !this.mAccessPoint.isSaved()) && passwordInvalid)) {
            enabled = false;
        } else {
            enabled = ipAndProxyFieldsAreValid();
        }
        if (this.mEapCaCertSpinner != null && this.mView.findViewById(R.id.l_ca_cert).getVisibility() != 8) {
            String caCertSelection = (String) this.mEapCaCertSpinner.getSelectedItem();
            if (caCertSelection.equals(this.mUnspecifiedCertString)) {
                enabled = false;
            }
            if (caCertSelection.equals(this.mUseSystemCertsString) && this.mEapDomainView != null && this.mView.findViewById(R.id.l_domain).getVisibility() != 8 && TextUtils.isEmpty(this.mEapDomainView.getText().toString())) {
                enabled = false;
            }
        }
        if (this.mEapUserCertSpinner != null && this.mView.findViewById(R.id.l_user_cert).getVisibility() != 8 && ((String) this.mEapUserCertSpinner.getSelectedItem()).equals(this.mUnspecifiedCertString)) {
            return false;
        }
        return enabled;
    }

    void showWarningMessagesIfAppropriate() {
        this.mView.findViewById(R.id.no_ca_cert_warning).setVisibility(8);
        this.mView.findViewById(R.id.no_domain_warning).setVisibility(8);
        if (this.mEapCaCertSpinner == null || this.mView.findViewById(R.id.l_ca_cert).getVisibility() == 8) {
            return;
        }
        String caCertSelection = (String) this.mEapCaCertSpinner.getSelectedItem();
        if (caCertSelection.equals(this.mDoNotValidateEapServerString)) {
            this.mView.findViewById(R.id.no_ca_cert_warning).setVisibility(0);
        }
        if (!caCertSelection.equals(this.mUseSystemCertsString) || this.mEapDomainView == null || this.mView.findViewById(R.id.l_domain).getVisibility() == 8 || !TextUtils.isEmpty(this.mEapDomainView.getText().toString())) {
            return;
        }
        this.mView.findViewById(R.id.no_domain_warning).setVisibility(0);
    }

    WifiConfiguration getConfig() {
        if (this.mMode == 0) {
            return null;
        }
        WifiConfiguration config = new WifiConfiguration();
        if (this.mAccessPoint == null) {
            config.SSID = AccessPoint.convertToQuotedString(this.mSsidView.getText().toString());
            config.hiddenSSID = true;
        } else if (this.mAccessPoint.isSaved()) {
            config.networkId = this.mAccessPoint.getConfig().networkId;
        } else {
            config.SSID = AccessPoint.convertToQuotedString(this.mAccessPoint.getSsidStr());
        }
        config.shared = this.mSharedCheckBox.isChecked();
        switch (this.mAccessPointSecurity) {
            case DefaultWfcSettingsExt.RESUME:
                config.allowedKeyManagement.set(0);
                break;
            case DefaultWfcSettingsExt.PAUSE:
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
            case DefaultWfcSettingsExt.CREATE:
                config.allowedKeyManagement.set(1);
                if (this.mPasswordView.length() != 0) {
                    String password2 = this.mPasswordView.getText().toString();
                    if (!password2.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = '\"' + password2 + '\"';
                    } else {
                        config.preSharedKey = password2;
                    }
                }
                break;
            case DefaultWfcSettingsExt.DESTROY:
                config.allowedKeyManagement.set(2);
                config.allowedKeyManagement.set(3);
                config.enterpriseConfig = new WifiEnterpriseConfig();
                int eapMethod = this.mEapMethodSpinner.getSelectedItemPosition();
                int phase2Method = this.mPhase2Spinner.getSelectedItemPosition();
                config.enterpriseConfig.setEapMethod(eapMethod);
                switch (eapMethod) {
                    case DefaultWfcSettingsExt.RESUME:
                        switch (phase2Method) {
                            case DefaultWfcSettingsExt.RESUME:
                                config.enterpriseConfig.setPhase2Method(0);
                                break;
                            case DefaultWfcSettingsExt.PAUSE:
                                config.enterpriseConfig.setPhase2Method(3);
                                break;
                            case DefaultWfcSettingsExt.CREATE:
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
                config.enterpriseConfig.setCaCertificateAliases(null);
                config.enterpriseConfig.setCaPath(null);
                config.enterpriseConfig.setDomainSuffixMatch(this.mEapDomainView.getText().toString());
                if (!caCert.equals(this.mUnspecifiedCertString) && !caCert.equals(this.mDoNotValidateEapServerString)) {
                    if (caCert.equals(this.mUseSystemCertsString)) {
                        config.enterpriseConfig.setCaPath("/system/etc/security/cacerts");
                    } else if (!caCert.equals(this.mMultipleCertSetString)) {
                        config.enterpriseConfig.setCaCertificateAliases(new String[]{caCert});
                    } else if (this.mAccessPoint != null) {
                        if (!this.mAccessPoint.isSaved()) {
                            Log.e("WifiConfigController", "Multiple certs can only be set when editing saved network");
                        }
                        config.enterpriseConfig.setCaCertificateAliases(this.mAccessPoint.getConfig().enterpriseConfig.getCaCertificateAliases());
                    }
                }
                if (config.enterpriseConfig.getCaCertificateAliases() != null && config.enterpriseConfig.getCaPath() != null) {
                    Log.e("WifiConfigController", "ca_cert (" + config.enterpriseConfig.getCaCertificateAliases() + ") and ca_path (" + config.enterpriseConfig.getCaPath() + ") should not both be non-null");
                }
                String clientCert = (String) this.mEapUserCertSpinner.getSelectedItem();
                if (clientCert.equals(this.mUnspecifiedCertString) || clientCert.equals(this.mDoNotProvideEapUserCertString)) {
                    clientCert = "";
                }
                config.enterpriseConfig.setClientCertificateAlias(clientCert);
                if (eapMethod == 4 || eapMethod == 5 || eapMethod == 6) {
                    config.enterpriseConfig.setIdentity("");
                    config.enterpriseConfig.setAnonymousIdentity("");
                } else if (eapMethod == 3) {
                    config.enterpriseConfig.setIdentity(this.mEapIdentityView.getText().toString());
                    config.enterpriseConfig.setAnonymousIdentity("");
                } else {
                    config.enterpriseConfig.setIdentity(this.mEapIdentityView.getText().toString());
                    config.enterpriseConfig.setAnonymousIdentity(this.mEapAnonymousView.getText().toString());
                }
                if (!this.mPasswordView.isShown() || this.mPasswordView.length() > 0) {
                    config.enterpriseConfig.setPassword(this.mPasswordView.getText().toString());
                }
                break;
            default:
                return null;
        }
        config.setIpConfiguration(new IpConfiguration(this.mIpAssignment, this.mProxySettings, this.mStaticIpConfiguration, this.mHttpProxy));
        return config;
    }

    private boolean ipAndProxyFieldsAreValid() {
        IpConfiguration.IpAssignment ipAssignment;
        Uri uri;
        int result;
        if (this.mIpSettingsSpinner != null && this.mIpSettingsSpinner.getSelectedItemPosition() == 1) {
            ipAssignment = IpConfiguration.IpAssignment.STATIC;
        } else {
            ipAssignment = IpConfiguration.IpAssignment.DHCP;
        }
        this.mIpAssignment = ipAssignment;
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
            if (TextUtils.isEmpty(uriSequence) || (uri = Uri.parse(uriSequence.toString())) == null) {
                return false;
            }
            this.mHttpProxy = new ProxyInfo(uri);
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
        if (TextUtils.isEmpty(ipAddr) || (inetAddr = getIPv4Address(ipAddr)) == null || inetAddr.equals(Inet4Address.ANY)) {
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
        } catch (IllegalArgumentException e2) {
            return R.string.wifi_ip_settings_invalid_ip_address;
        }
        String gateway = this.mGatewayView.getText().toString();
        if (TextUtils.isEmpty(gateway)) {
            try {
                InetAddress netPart = NetworkUtils.getNetworkPart(inetAddr, networkPrefixLength);
                byte[] addr = netPart.getAddress();
                addr[addr.length - 1] = 1;
                this.mGatewayView.setText(InetAddress.getByAddress(addr).getHostAddress());
            } catch (RuntimeException e3) {
            } catch (UnknownHostException e4) {
            }
        } else {
            InetAddress gatewayAddr = getIPv4Address(gateway);
            if (gatewayAddr == null || gatewayAddr.isMulticastAddress()) {
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
            return 0;
        }
        return 0;
    }

    private void showSecurityFields() {
        if (this.mAccessPointSecurity == 0) {
            this.mView.findViewById(R.id.security_fields).setVisibility(8);
            return;
        }
        this.mView.findViewById(R.id.security_fields).setVisibility(0);
        if (this.mPasswordView == null) {
            this.mPasswordView = (TextView) this.mView.findViewById(R.id.password);
            this.mPasswordView.addTextChangedListener(this);
            this.mPasswordView.setOnEditorActionListener(this);
            this.mPasswordView.setOnKeyListener(this);
            ((CheckBox) this.mView.findViewById(R.id.show_password)).setOnCheckedChangeListener(this);
            if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
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
            if (Utils.isWifiOnly(this.mContext) || !this.mContext.getResources().getBoolean(android.R.^attr-private.outKeycode)) {
                String[] eapMethods = this.mContext.getResources().getStringArray(R.array.eap_method_without_sim_auth);
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this.mContext, android.R.layout.simple_spinner_item, eapMethods);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                this.mEapMethodSpinner.setAdapter((SpinnerAdapter) spinnerAdapter);
            }
            this.mPhase2Spinner = (Spinner) this.mView.findViewById(R.id.phase2);
            this.mEapCaCertSpinner = (Spinner) this.mView.findViewById(R.id.ca_cert);
            this.mEapCaCertSpinner.setOnItemSelectedListener(this);
            this.mEapDomainView = (TextView) this.mView.findViewById(R.id.domain);
            this.mEapDomainView.addTextChangedListener(this);
            this.mEapUserCertSpinner = (Spinner) this.mView.findViewById(R.id.user_cert);
            this.mEapUserCertSpinner.setOnItemSelectedListener(this);
            this.mEapIdentityView = (TextView) this.mView.findViewById(R.id.identity);
            this.mEapAnonymousView = (TextView) this.mView.findViewById(R.id.anonymous);
            loadCertificates(this.mEapCaCertSpinner, "CACERT_", this.mDoNotValidateEapServerString, false, true);
            loadCertificates(this.mEapUserCertSpinner, "USRPKEY_", this.mDoNotProvideEapUserCertString, false, false);
            if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
                WifiEnterpriseConfig enterpriseConfig = this.mAccessPoint.getConfig().enterpriseConfig;
                int eapMethod = enterpriseConfig.getEapMethod();
                int phase2Method = enterpriseConfig.getPhase2Method();
                this.mEapMethodSpinner.setSelection(eapMethod);
                showEapFieldsByMethod(eapMethod);
                switch (eapMethod) {
                    case DefaultWfcSettingsExt.RESUME:
                        switch (phase2Method) {
                            case DefaultWfcSettingsExt.RESUME:
                                this.mPhase2Spinner.setSelection(0);
                                break;
                            case DefaultWfcSettingsExt.PAUSE:
                            case DefaultWfcSettingsExt.CREATE:
                            default:
                                Log.e("WifiConfigController", "Invalid phase 2 method " + phase2Method);
                                break;
                            case DefaultWfcSettingsExt.DESTROY:
                                this.mPhase2Spinner.setSelection(1);
                                break;
                            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                                this.mPhase2Spinner.setSelection(2);
                                break;
                        }
                        break;
                    default:
                        this.mPhase2Spinner.setSelection(phase2Method);
                        break;
                }
                if (!TextUtils.isEmpty(enterpriseConfig.getCaPath())) {
                    setSelection(this.mEapCaCertSpinner, this.mUseSystemCertsString);
                } else {
                    String[] caCerts = enterpriseConfig.getCaCertificateAliases();
                    if (caCerts == null) {
                        setSelection(this.mEapCaCertSpinner, this.mDoNotValidateEapServerString);
                    } else if (caCerts.length == 1) {
                        setSelection(this.mEapCaCertSpinner, caCerts[0]);
                    } else {
                        loadCertificates(this.mEapCaCertSpinner, "CACERT_", this.mDoNotValidateEapServerString, true, true);
                        setSelection(this.mEapCaCertSpinner, this.mMultipleCertSetString);
                    }
                }
                this.mEapDomainView.setText(enterpriseConfig.getDomainSuffixMatch());
                String userCert = enterpriseConfig.getClientCertificateAlias();
                if (TextUtils.isEmpty(userCert)) {
                    setSelection(this.mEapUserCertSpinner, this.mDoNotProvideEapUserCertString);
                } else {
                    setSelection(this.mEapUserCertSpinner, userCert);
                }
                this.mEapIdentityView.setText(enterpriseConfig.getIdentity());
                this.mEapAnonymousView.setText(enterpriseConfig.getAnonymousIdentity());
                return;
            }
            showEapFieldsByMethod(this.mEapMethodSpinner.getSelectedItemPosition());
            return;
        }
        showEapFieldsByMethod(this.mEapMethodSpinner.getSelectedItemPosition());
    }

    private void showEapFieldsByMethod(int eapMethod) {
        this.mView.findViewById(R.id.l_method).setVisibility(0);
        this.mView.findViewById(R.id.l_identity).setVisibility(0);
        this.mView.findViewById(R.id.l_domain).setVisibility(0);
        this.mView.findViewById(R.id.l_ca_cert).setVisibility(0);
        this.mView.findViewById(R.id.password_layout).setVisibility(0);
        this.mView.findViewById(R.id.show_password_layout).setVisibility(0);
        this.mConfigUi.getContext();
        switch (eapMethod) {
            case DefaultWfcSettingsExt.RESUME:
                if (this.mPhase2Adapter != this.mPhase2PeapAdapter) {
                    this.mPhase2Adapter = this.mPhase2PeapAdapter;
                    this.mPhase2Spinner.setAdapter((SpinnerAdapter) this.mPhase2Adapter);
                }
                this.mView.findViewById(R.id.l_phase2).setVisibility(0);
                this.mView.findViewById(R.id.l_anonymous).setVisibility(0);
                setUserCertInvisible();
                break;
            case DefaultWfcSettingsExt.PAUSE:
                this.mView.findViewById(R.id.l_user_cert).setVisibility(0);
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setPasswordInvisible();
                break;
            case DefaultWfcSettingsExt.CREATE:
                if (this.mPhase2Adapter != this.mPhase2FullAdapter) {
                    this.mPhase2Adapter = this.mPhase2FullAdapter;
                    this.mPhase2Spinner.setAdapter((SpinnerAdapter) this.mPhase2Adapter);
                }
                this.mView.findViewById(R.id.l_phase2).setVisibility(0);
                this.mView.findViewById(R.id.l_anonymous).setVisibility(0);
                setUserCertInvisible();
                break;
            case DefaultWfcSettingsExt.DESTROY:
                setPhase2Invisible();
                setCaCertInvisible();
                setDomainInvisible();
                setAnonymousIdentInvisible();
                setUserCertInvisible();
                break;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
            case 5:
            case 6:
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setCaCertInvisible();
                setDomainInvisible();
                setUserCertInvisible();
                setPasswordInvisible();
                setIdentityInvisible();
                break;
        }
        if (this.mView.findViewById(R.id.l_ca_cert).getVisibility() == 8) {
            return;
        }
        String eapCertSelection = (String) this.mEapCaCertSpinner.getSelectedItem();
        if (!eapCertSelection.equals(this.mDoNotValidateEapServerString) && !eapCertSelection.equals(this.mUnspecifiedCertString)) {
            return;
        }
        setDomainInvisible();
    }

    private void setIdentityInvisible() {
        this.mView.findViewById(R.id.l_identity).setVisibility(8);
        this.mPhase2Spinner.setSelection(0);
    }

    private void setPhase2Invisible() {
        this.mView.findViewById(R.id.l_phase2).setVisibility(8);
        this.mPhase2Spinner.setSelection(0);
    }

    private void setCaCertInvisible() {
        this.mView.findViewById(R.id.l_ca_cert).setVisibility(8);
        setSelection(this.mEapCaCertSpinner, this.mUnspecifiedCertString);
    }

    private void setDomainInvisible() {
        this.mView.findViewById(R.id.l_domain).setVisibility(8);
        this.mEapDomainView.setText("");
    }

    private void setUserCertInvisible() {
        this.mView.findViewById(R.id.l_user_cert).setVisibility(8);
        setSelection(this.mEapUserCertSpinner, this.mUnspecifiedCertString);
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
        if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
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
            if (config == null || (staticConfig = config.getStaticIpConfiguration()) == null) {
                return;
            }
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
            if (!dnsIterator.hasNext()) {
                return;
            }
            this.mDns2View.setText(dnsIterator.next().getHostAddress());
            return;
        }
        this.mView.findViewById(R.id.staticip).setVisibility(8);
    }

    private void showProxyFields() {
        ProxyInfo proxyInfo;
        ProxyInfo proxyProperties;
        WifiConfiguration config = null;
        this.mView.findViewById(R.id.proxy_settings_fields).setVisibility(0);
        if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
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
            if (config == null || (proxyProperties = config.getHttpProxy()) == null) {
                return;
            }
            this.mProxyHostView.setText(proxyProperties.getHost());
            this.mProxyPortView.setText(Integer.toString(proxyProperties.getPort()));
            this.mProxyExclusionListView.setText(proxyProperties.getExclusionListAsString());
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
            if (config == null || (proxyInfo = config.getHttpProxy()) == null) {
                return;
            }
            this.mProxyPacView.setText(proxyInfo.getPacFileUrl().toString());
            return;
        }
        setVisibility(R.id.proxy_warning_limited_support, 8);
        setVisibility(R.id.proxy_fields, 8);
        setVisibility(R.id.proxy_pac_field, 8);
    }

    private void setVisibility(int id, int visibility) {
        View v = this.mView.findViewById(id);
        if (v == null) {
            return;
        }
        v.setVisibility(visibility);
    }

    private void loadCertificates(Spinner spinner, String prefix, String noCertificateString, boolean showMultipleCerts, boolean showUsePreinstalledCertOption) {
        Context context = this.mConfigUi.getContext();
        ArrayList<String> certs = new ArrayList<>();
        certs.add(this.mUnspecifiedCertString);
        if (showMultipleCerts) {
            certs.add(this.mMultipleCertSetString);
        }
        if (showUsePreinstalledCertOption) {
            certs.add(this.mUseSystemCertsString);
        }
        certs.addAll(Arrays.asList(KeyStore.getInstance().list(prefix, 1010)));
        certs.add(noCertificateString);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, (String[]) certs.toArray(new String[certs.size()]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) adapter);
    }

    private void setSelection(Spinner spinner, String value) {
        if (value == null) {
            return;
        }
        ArrayAdapter<String> adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = adapter.getCount() - 1; i >= 0; i--) {
            if (value.equals(adapter.getItem(i))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    public int getMode() {
        return this.mMode;
    }

    @Override
    public void afterTextChanged(Editable s) {
        this.mTextViewChangedHandler.post(new Runnable() {
            @Override
            public void run() {
                WifiConfigController.this.showWarningMessagesIfAppropriate();
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
    public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
        if (textView == this.mPasswordView && id == 6 && isSubmittable()) {
            this.mConfigUi.dispatchSubmit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if (view == this.mPasswordView && keyCode == 66 && isSubmittable()) {
            this.mConfigUi.dispatchSubmit();
            return true;
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        if (view.getId() == R.id.show_password) {
            int pos = this.mPasswordView.getSelectionEnd();
            this.mPasswordView.setInputType((isChecked ? 144 : 128) | 1);
            if (pos < 0) {
                return;
            }
            ((EditText) this.mPasswordView).setSelection(pos);
            return;
        }
        if (view.getId() != R.id.wifi_advanced_togglebox) {
            return;
        }
        if (isChecked) {
            this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(0);
        } else {
            this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(8);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == this.mSecuritySpinner) {
            this.mAccessPointSecurity = position;
            showSecurityFields();
        } else if (parent == this.mEapMethodSpinner || parent == this.mEapCaCertSpinner) {
            showSecurityFields();
        } else if (parent == this.mProxySettingsSpinner) {
            showProxyFields();
        } else {
            showIpConfigFields();
        }
        showWarningMessagesIfAppropriate();
        enableSubmitIfAppropriate();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void updatePassword() {
        int i;
        TextView passwdView = (TextView) this.mView.findViewById(R.id.password);
        if (((CheckBox) this.mView.findViewById(R.id.show_password)).isChecked()) {
            i = 144;
        } else {
            i = 128;
        }
        passwdView.setInputType(i | 1);
    }

    public AccessPoint getAccessPoint() {
        return this.mAccessPoint;
    }
}
