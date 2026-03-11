package jp.co.benesse.dcha.systemsettings;

import android.content.Context;
import android.content.res.Resources;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.security.KeyStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.co.benesse.dcha.util.Logger;

public class WifiConfigController implements TextWatcher, View.OnClickListener, AdapterView.OnItemSelectedListener {
    private final AccessPoint mAccessPoint;
    private int mAccessPointSecurity;
    private final WifiConfigUiBase mConfigUi;
    private TextView mDns1View;
    private TextView mDns2View;
    private TextView mEapAnonymousView;
    private Spinner mEapCaCertSpinner;
    private TextView mEapIdentityView;
    private Spinner mEapMethodSpinner;
    private Spinner mEapUserCertSpinner;
    private boolean mEdit;
    private TextView mGatewayView;
    private TextView mIpAddressView;
    private Spinner mIpSettingsSpinner;
    private TextView mNetworkPrefixLengthView;
    private TextView mPasswordView;
    private Spinner mPhase2Spinner;
    private TextView mProxyExclusionListView;
    private TextView mProxyHostView;
    private TextView mProxyPortView;
    private Spinner mProxySettingsSpinner;
    private Spinner mSecuritySpinner;
    private TextView mSsidView;
    private final Handler mTextViewChangedHandler;
    private final View mView;
    private String unspecifiedCert;
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^$|^[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*$");
    private static final Pattern EXCLUSION_PATTERN = Pattern.compile("$|^[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*$");
    private IpConfiguration.IpAssignment mIpAssignment = IpConfiguration.IpAssignment.UNASSIGNED;
    private IpConfiguration.ProxySettings mProxySettings = IpConfiguration.ProxySettings.UNASSIGNED;
    private ProxyInfo mHttpProxy = null;
    private StaticIpConfiguration mStaticIpConfiguration = null;

    public WifiConfigController(NetworkSettingActivity activity, WifiConfigUiBase parent, View view, AccessPoint accessPoint, boolean edit) {
        this.unspecifiedCert = "unspecified";
        Logger.d("WifiConfigController", "WifiConfigController 0001");
        this.mConfigUi = parent;
        this.mView = view;
        this.mAccessPoint = accessPoint;
        this.mAccessPointSecurity = accessPoint == null ? 0 : accessPoint.security;
        this.mEdit = edit;
        this.mTextViewChangedHandler = new Handler();
        Context context = this.mConfigUi.getContext();
        Resources resources = context.getResources();
        this.unspecifiedCert = context.getString(R.string.wifi_unspecified);
        this.mIpSettingsSpinner = (Spinner) this.mView.findViewById(R.id.ip_settings);
        this.mIpSettingsSpinner.setOnItemSelectedListener(this);
        this.mProxySettingsSpinner = (Spinner) this.mView.findViewById(R.id.proxy_settings);
        this.mProxySettingsSpinner.setOnItemSelectedListener(this);
        if (this.mAccessPoint == null) {
            Logger.d("WifiConfigController", "WifiConfigController 0002");
            this.mConfigUi.setTitle(R.string.wifi_add_network);
            this.mSsidView = (TextView) this.mView.findViewById(R.id.ssid);
            this.mSsidView.addTextChangedListener(this);
            this.mSecuritySpinner = (Spinner) this.mView.findViewById(R.id.security);
            this.mSecuritySpinner.setOnItemSelectedListener(this);
            this.mView.findViewById(R.id.type).setVisibility(0);
            showIpConfigFields();
            showProxyFields();
            this.mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(0);
            ((CheckBox) this.mView.findViewById(R.id.wifi_advanced_togglebox)).setOnClickListener(this);
            this.mConfigUi.setSubmitButton(context.getString(R.string.wifi_save));
        } else {
            Logger.d("WifiConfigController", "WifiConfigController 0003");
            this.mConfigUi.setTitle(this.mAccessPoint.ssid);
            ViewGroup group = (ViewGroup) this.mView.findViewById(R.id.info);
            NetworkInfo.DetailedState state = this.mAccessPoint.getState();
            Logger.i("WifiConfigController", "onClick state = " + state);
            if (state != null) {
                Logger.d("WifiConfigController", "WifiConfigController 0004");
                addRow(group, R.string.wifi_status, Summary.get(this.mConfigUi.getContext(), state));
            }
            int level = this.mAccessPoint.getLevel();
            if (level != -1) {
                Logger.d("WifiConfigController", "WifiConfigController 0005");
                String[] signal = resources.getStringArray(R.array.wifi_signal);
                addRow(group, R.string.wifi_signal, signal[level]);
            }
            WifiInfo info = this.mAccessPoint.getInfo();
            if (info != null && info.getLinkSpeed() != -1) {
                Logger.d("WifiConfigController", "WifiConfigController 0006");
                addRow(group, R.string.wifi_speed, info.getLinkSpeed() + "Mbps");
            }
            if (info != null && info.getFrequency() != -1) {
                int frequency = info.getFrequency();
                String band = null;
                if (frequency >= 2400 && frequency < 2500) {
                    band = context.getString(R.string.wifi_band_24ghz);
                } else if (frequency >= 4900 && frequency < 5900) {
                    band = context.getString(R.string.wifi_band_5ghz);
                } else {
                    Logger.d("WifiConfigController", "Unexpected frequency " + frequency);
                }
                if (band != null) {
                    addRow(group, R.string.wifi_frequency, band);
                }
            }
            addRow(group, R.string.wifi_security, this.mAccessPoint.getSecurityString(false));
            boolean showAdvancedFields = false;
            if (this.mAccessPoint.networkId != -1) {
                Logger.d("WifiConfigController", "WifiConfigController 0007");
                WifiConfiguration config = this.mAccessPoint.getConfig();
                if (config.getIpAssignment() == IpConfiguration.IpAssignment.STATIC) {
                    Logger.d("WifiConfigController", "WifiConfigController 0008");
                    this.mIpSettingsSpinner.setSelection(1);
                    showAdvancedFields = true;
                    StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                    if (staticConfig != null && staticConfig.ipAddress != null) {
                        addRow(group, R.string.wifi_ip_address, staticConfig.ipAddress.getAddress().getHostAddress());
                    }
                } else {
                    Logger.d("WifiConfigController", "WifiConfigController 0009");
                    this.mIpSettingsSpinner.setSelection(0);
                }
                if (config.getProxySettings() == IpConfiguration.ProxySettings.STATIC) {
                    Logger.d("WifiConfigController", "WifiConfigController 0011");
                    this.mProxySettingsSpinner.setSelection(1);
                    showAdvancedFields = true;
                } else {
                    Logger.d("WifiConfigController", "WifiConfigController 0012");
                    this.mProxySettingsSpinner.setSelection(0);
                }
            }
            if ((this.mAccessPoint.networkId == -1 && !this.mAccessPoint.isActive()) || this.mEdit) {
                Logger.d("WifiConfigController", "WifiConfigController 0013");
                showSecurityFields();
                showIpConfigFields();
                showProxyFields();
                this.mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(0);
                ((CheckBox) this.mView.findViewById(R.id.wifi_advanced_togglebox)).setOnClickListener(this);
                if (showAdvancedFields) {
                    Logger.d("WifiConfigController", "WifiConfigController 0014");
                    ((CheckBox) this.mView.findViewById(R.id.wifi_advanced_togglebox)).setChecked(true);
                    this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(0);
                }
            }
            if (this.mEdit) {
                Logger.d("WifiConfigController", "WifiConfigController 0015");
                this.mConfigUi.setSubmitButton(context.getString(R.string.wifi_save));
            } else {
                Logger.d("WifiConfigController", "WifiConfigController 0016");
                if (state == null && level != -1) {
                    Logger.d("WifiConfigController", "WifiConfigController 0017");
                    this.mConfigUi.setSubmitButton(context.getString(R.string.wifi_connect));
                } else {
                    Logger.d("WifiConfigController", "WifiConfigController 0018");
                    this.mView.findViewById(R.id.ip_fields).setVisibility(8);
                }
                if (this.mAccessPoint.networkId != -1 || this.mAccessPoint.isActive()) {
                    Logger.d("WifiConfigController", "WifiConfigController 0019");
                    this.mConfigUi.setForgetButton(context.getString(R.string.wifi_forget));
                }
            }
        }
        if (this.mEdit || (this.mAccessPoint != null && this.mAccessPoint.getState() == null && this.mAccessPoint.getLevel() != -1)) {
            Logger.d("WifiConfigController", "WifiConfigController 0020");
            this.mConfigUi.setCancelButton(context.getString(R.string.wifi_cancel));
        } else {
            this.mConfigUi.setCancelButton(context.getString(R.string.wifi_done));
        }
        if (this.mConfigUi.getSubmitButton() != null) {
            Logger.d("WifiConfigController", "WifiConfigController 0021");
            setSubmitOrEnable();
        }
        Logger.d("WifiConfigController", "WifiConfigController 0022");
    }

    private void addRow(ViewGroup group, int name, String value) {
        Logger.d("WifiConfigController", "addRow 0001");
        View row = this.mConfigUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(name);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        group.addView(row);
        Logger.d("WifiConfigController", "addRow 0002");
    }

    void setSubmitOrEnable() {
        boolean enabled;
        Logger.d("WifiConfigController", "setSubmitOrEnable 0001");
        Button submit = this.mConfigUi.getSubmitButton();
        if (submit == null) {
            Logger.d("WifiConfigController", "setSubmitOrEnable 0002");
            return;
        }
        boolean passwordInvalid = false;
        if (this.mPasswordView != null && ((this.mAccessPointSecurity == 1 && this.mPasswordView.length() == 0) || (this.mAccessPointSecurity == 2 && this.mPasswordView.length() < 8))) {
            Logger.d("WifiConfigController", "setSubmitOrEnable 0003");
            passwordInvalid = true;
        }
        if ((this.mSsidView != null && this.mSsidView.length() == 0) || ((this.mAccessPoint == null || this.mAccessPoint.networkId == -1) && passwordInvalid)) {
            Logger.d("WifiConfigController", "setSubmitOrEnable 0004");
            enabled = false;
        } else {
            Logger.d("WifiConfigController", "setSubmitOrEnable 0005");
            if (ipAndProxyFieldsAreValid()) {
                Logger.d("WifiConfigController", "setSubmitOrEnable 0006");
                enabled = true;
            } else {
                Logger.d("WifiConfigController", "setSubmitOrEnable 0007");
                enabled = false;
            }
        }
        submit.setEnabled(enabled);
        Logger.d("WifiConfigController", "setSubmitOrEnable 0008");
    }

    WifiConfiguration getConfig() {
        Logger.d("WifiConfigController", "getConfig 0001");
        if (this.mAccessPoint != null && this.mAccessPoint.networkId != -1 && !this.mEdit) {
            Logger.d("WifiConfigController", "getConfig 0002");
            return null;
        }
        WifiConfiguration config = new WifiConfiguration();
        if (this.mAccessPoint == null) {
            Logger.d("WifiConfigController", "getConfig 0003");
            config.SSID = AccessPoint.convertToQuotedString(this.mSsidView.getText().toString());
            config.hiddenSSID = true;
        } else if (this.mAccessPoint.networkId == -1) {
            Logger.d("WifiConfigController", "getConfig 0004");
            config.SSID = AccessPoint.convertToQuotedString(this.mAccessPoint.ssid);
        } else {
            Logger.d("WifiConfigController", "getConfig 0005");
            config.networkId = this.mAccessPoint.networkId;
        }
        switch (this.mAccessPointSecurity) {
            case 0:
                Logger.d("WifiConfigController", "getConfig 0006");
                config.allowedKeyManagement.set(0);
                break;
            case 1:
                Logger.d("WifiConfigController", "getConfig 0007");
                config.allowedKeyManagement.set(0);
                config.allowedAuthAlgorithms.set(0);
                config.allowedAuthAlgorithms.set(1);
                if (this.mPasswordView.length() != 0) {
                    Logger.d("WifiConfigController", "getConfig 0008");
                    int length = this.mPasswordView.length();
                    String password = this.mPasswordView.getText().toString();
                    if ((length == 10 || length == 26 || length == 58) && password.matches("[0-9A-Fa-f]*")) {
                        Logger.d("WifiConfigController", "getConfig 0009");
                        config.wepKeys[0] = password;
                    } else {
                        Logger.d("WifiConfigController", "getConfig 0010");
                        config.wepKeys[0] = '\"' + password + '\"';
                    }
                }
                break;
            case 2:
                Logger.d("WifiConfigController", "getConfig 0011");
                config.allowedKeyManagement.set(1);
                if (this.mPasswordView.length() != 0) {
                    Logger.d("WifiConfigController", "getConfig 0012");
                    String password2 = this.mPasswordView.getText().toString();
                    if (password2.matches("[0-9A-Fa-f]{64}")) {
                        Logger.d("WifiConfigController", "getConfig 0013");
                        config.preSharedKey = password2;
                    } else {
                        Logger.d("WifiConfigController", "getConfig 0014");
                        config.preSharedKey = '\"' + password2 + '\"';
                    }
                }
                break;
            case 3:
                Logger.d("WifiConfigController", "getConfig 0015");
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
                                Logger.e("WifiConfigController", "Unknown phase2 method" + phase2Method);
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
                if (!this.mPasswordView.isShown() || this.mPasswordView.length() > 0) {
                    config.enterpriseConfig.setPassword(this.mPasswordView.getText().toString());
                }
                break;
            default:
                Logger.d("WifiConfigController", "getConfig 0017");
                return null;
        }
        config.setIpConfiguration(new IpConfiguration(this.mIpAssignment, this.mProxySettings, this.mStaticIpConfiguration, this.mHttpProxy));
        Logger.d("WifiConfigController", "getConfig 0018");
        return config;
    }

    private boolean ipAndProxyFieldsAreValid() {
        int result;
        Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0001");
        this.mIpAssignment = (this.mIpSettingsSpinner == null || this.mIpSettingsSpinner.getSelectedItemPosition() != 1) ? IpConfiguration.IpAssignment.DHCP : IpConfiguration.IpAssignment.STATIC;
        if (this.mIpAssignment == IpConfiguration.IpAssignment.STATIC) {
            Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0002");
            this.mStaticIpConfiguration = new StaticIpConfiguration();
            int result2 = validateIpConfigFields(this.mStaticIpConfiguration);
            if (result2 != 0) {
                Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0003");
                return false;
            }
        }
        int selectedPosition = this.mProxySettingsSpinner.getSelectedItemPosition();
        this.mProxySettings = IpConfiguration.ProxySettings.NONE;
        this.mHttpProxy = null;
        if (selectedPosition == 1 && this.mProxyHostView != null) {
            Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0004");
            this.mProxySettings = IpConfiguration.ProxySettings.STATIC;
            String host = this.mProxyHostView.getText().toString();
            String portStr = this.mProxyPortView.getText().toString();
            String exclusionList = this.mProxyExclusionListView.getText().toString();
            int port = 0;
            try {
                Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0005");
                port = Integer.parseInt(portStr);
                result = validate(host, portStr, exclusionList);
                Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0006");
            } catch (NumberFormatException e) {
                Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0007");
                Logger.e("WifiConfigController", "NumberFormatException", e);
                result = R.string.proxy_error_invalid_port;
            }
            if (result == 0) {
                Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0008");
                this.mHttpProxy = new ProxyInfo(host, port, exclusionList);
            } else {
                Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0009");
                return false;
            }
        }
        Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0010");
        return true;
    }

    public static int validate(String hostname, String port, String exclList) {
        Logger.d("WifiConfigController", "validate 0001");
        Matcher match = HOSTNAME_PATTERN.matcher(hostname);
        String[] exclListArray = exclList.split(",");
        if (!match.matches()) {
            Logger.d("WifiConfigController", "validate 0002");
            return R.string.proxy_error_invalid_host;
        }
        for (String excl : exclListArray) {
            Logger.d("WifiConfigController", "validate 0003");
            Matcher m = EXCLUSION_PATTERN.matcher(excl);
            if (!m.matches()) {
                Logger.d("WifiConfigController", "validate 0004");
                return R.string.proxy_error_invalid_exclusion_list;
            }
        }
        if (hostname.length() > 0 && port.length() == 0) {
            Logger.d("WifiConfigController", "validate 0005");
            return R.string.proxy_error_empty_port;
        }
        if (port.length() > 0) {
            Logger.d("WifiConfigController", "validate 0006");
            if (hostname.length() == 0) {
                Logger.d("WifiConfigController", "validate 0007");
                return R.string.proxy_error_empty_host_set_port;
            }
            try {
                Logger.d("WifiConfigController", "validate 0008");
                int portVal = Integer.parseInt(port);
                Logger.d("WifiConfigController", "validate 0009");
                if (portVal <= 0 || portVal > 65535) {
                    Logger.d("WifiConfigController", "validate 0011");
                    return R.string.proxy_error_invalid_port;
                }
            } catch (NumberFormatException e) {
                Logger.d("WifiConfigController", "validate 0010");
                Logger.e("WifiConfigController", "NumberFormatException", e);
                return R.string.proxy_error_invalid_port;
            }
        }
        Logger.d("WifiConfigController", "validate 0012");
        return 0;
    }

    private Inet4Address getIPv4Address(String text) {
        try {
            Logger.d("WifiConfigController", "getIPv4Address 0001");
            return (Inet4Address) NetworkUtils.numericToInetAddress(text);
        } catch (ClassCastException | IllegalArgumentException e) {
            Logger.d("WifiConfigController", "getIPv4Address 0002");
            return null;
        }
    }

    private int validateIpConfigFields(StaticIpConfiguration staticIpConfiguration) {
        Logger.d("WifiConfigController", "validateIpConfigFields 0001");
        if (this.mIpAddressView == null) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0002");
            return 0;
        }
        String ipAddr = this.mIpAddressView.getText().toString();
        if (TextUtils.isEmpty(ipAddr)) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0003");
            return R.string.wifi_ip_settings_invalid_ip_address;
        }
        Inet4Address inetAddr = getIPv4Address(ipAddr);
        if (inetAddr == null) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0006");
            return R.string.wifi_ip_settings_invalid_ip_address;
        }
        int networkPrefixLength = -1;
        try {
            Logger.d("WifiConfigController", "validateIpConfigFields 0007");
            networkPrefixLength = Integer.parseInt(this.mNetworkPrefixLengthView.getText().toString());
        } catch (NumberFormatException e) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0010");
            Logger.e("WifiConfigController", "NumberFormatException", e);
            this.mNetworkPrefixLengthView.setText(this.mConfigUi.getContext().getString(R.string.wifi_network_prefix_length_hint));
        }
        if (networkPrefixLength < 0 || networkPrefixLength > 32) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0008");
            return R.string.wifi_ip_settings_invalid_network_prefix_length;
        }
        staticIpConfiguration.ipAddress = new LinkAddress(inetAddr, networkPrefixLength);
        Logger.d("WifiConfigController", "validateIpConfigFields 0009");
        String gateway = this.mGatewayView.getText().toString();
        if (TextUtils.isEmpty(gateway)) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0011");
            try {
                Logger.d("WifiConfigController", "validateIpConfigFields 0012");
                InetAddress netPart = NetworkUtils.getNetworkPart(inetAddr, networkPrefixLength);
                byte[] addr = netPart.getAddress();
                addr[addr.length - 1] = 1;
                this.mGatewayView.setText(InetAddress.getByAddress(addr).getHostAddress());
                Logger.d("WifiConfigController", "validateIpConfigFields 0013");
            } catch (RuntimeException e2) {
                Logger.d("WifiConfigController", "validateIpConfigFields 0014");
                Logger.e("WifiConfigController", "RuntimeException", e2);
            } catch (UnknownHostException e3) {
                Logger.d("WifiConfigController", "validateIpConfigFields 0015");
                Logger.e("WifiConfigController", "UnknownHostException", e3);
            }
        } else {
            Logger.d("WifiConfigController", "validateIpConfigFields 0016");
            InetAddress gatewayAddr = getIPv4Address(gateway);
            if (gatewayAddr == null) {
                Logger.d("WifiConfigController", "validateIpConfigFields 0019");
                return R.string.wifi_ip_settings_invalid_gateway;
            }
            Logger.d("WifiConfigController", "validateIpConfigFields 0020");
            staticIpConfiguration.gateway = gatewayAddr;
        }
        String dns = this.mDns1View.getText().toString();
        if (TextUtils.isEmpty(dns)) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0021");
            this.mDns1View.setText(this.mConfigUi.getContext().getString(R.string.wifi_dns1_hint));
        } else {
            Logger.d("WifiConfigController", "validateIpConfigFields 0022");
            Inet4Address iPv4Address = getIPv4Address(dns);
            if (iPv4Address == null) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            Logger.d("WifiConfigController", "validateIpConfigFields 0026");
            staticIpConfiguration.dnsServers.add(iPv4Address);
        }
        if (this.mDns2View.length() > 0) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0027");
            Inet4Address iPv4Address2 = getIPv4Address(this.mDns2View.getText().toString());
            if (iPv4Address2 == null) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            Logger.d("WifiConfigController", "validateIpConfigFields 0031");
            staticIpConfiguration.dnsServers.add(iPv4Address2);
        }
        Logger.d("WifiConfigController", "validateIpConfigFields 0032");
        return 0;
    }

    private void showSecurityFields() {
        Logger.d("WifiConfigController", "showSecurityFields 0001");
        if (this.mAccessPointSecurity == 0) {
            Logger.d("WifiConfigController", "showSecurityFields 0002");
            this.mView.findViewById(R.id.security_fields).setVisibility(8);
            return;
        }
        this.mView.findViewById(R.id.security_fields).setVisibility(0);
        if (this.mPasswordView == null) {
            Logger.d("WifiConfigController", "showSecurityFields 0003");
            this.mPasswordView = (TextView) this.mView.findViewById(R.id.password);
            this.mPasswordView.addTextChangedListener(this);
            ((CheckBox) this.mView.findViewById(R.id.show_password)).setOnClickListener(this);
            if (this.mAccessPoint != null && this.mAccessPoint.networkId != -1) {
                Logger.d("WifiConfigController", "showSecurityFields 0004");
                this.mPasswordView.setHint(R.string.wifi_unchanged);
            }
        }
        if (this.mAccessPointSecurity != 3) {
            Logger.d("WifiConfigController", "showSecurityFields 0005");
            this.mView.findViewById(R.id.eap).setVisibility(8);
            return;
        }
        this.mView.findViewById(R.id.eap).setVisibility(0);
        if (this.mEapMethodSpinner == null) {
            Logger.d("WifiConfigController", "showSecurityFields 0006");
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
                Logger.d("WifiConfigController", "showSecurityFields 0007");
                WifiEnterpriseConfig enterpriseConfig = this.mAccessPoint.getConfig().enterpriseConfig;
                int eapMethod = enterpriseConfig.getEapMethod();
                int phase2Method = enterpriseConfig.getPhase2Method();
                this.mEapMethodSpinner.setSelection(eapMethod);
                switch (eapMethod) {
                    case 0:
                        switch (phase2Method) {
                            case 0:
                                this.mPhase2Spinner.setSelection(0);
                                break;
                            case 1:
                            case 2:
                            default:
                                Logger.e("WifiConfigController", "Invalid phase 2 method " + phase2Method);
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
            }
        }
        this.mView.findViewById(R.id.l_method).setVisibility(0);
        this.mView.findViewById(R.id.l_identity).setVisibility(0);
        if (this.mEapMethodSpinner.getSelectedItemPosition() == 3) {
            Logger.d("WifiConfigController", "showSecurityFields 0010");
            this.mView.findViewById(R.id.l_phase2).setVisibility(8);
            this.mView.findViewById(R.id.l_ca_cert).setVisibility(8);
            this.mView.findViewById(R.id.l_user_cert).setVisibility(8);
            this.mView.findViewById(R.id.l_anonymous).setVisibility(8);
        } else {
            Logger.d("WifiConfigController", "showSecurityFields 0011");
            this.mView.findViewById(R.id.l_phase2).setVisibility(0);
            this.mView.findViewById(R.id.l_ca_cert).setVisibility(0);
            this.mView.findViewById(R.id.l_user_cert).setVisibility(0);
            this.mView.findViewById(R.id.l_anonymous).setVisibility(0);
        }
        Logger.d("WifiConfigController", "showSecurityFields 0012");
    }

    private void showIpConfigFields() {
        StaticIpConfiguration staticConfig;
        Logger.d("WifiConfigController", "showIpConfigFields 0001");
        WifiConfiguration config = null;
        this.mView.findViewById(R.id.ip_fields).setVisibility(0);
        if (this.mAccessPoint != null && this.mAccessPoint.networkId != -1) {
            Logger.d("WifiConfigController", "showIpConfigFields 0002");
            config = this.mAccessPoint.getConfig();
        }
        if (this.mIpSettingsSpinner.getSelectedItemPosition() == 1) {
            Logger.d("WifiConfigController", "showIpConfigFields 0003");
            this.mView.findViewById(R.id.staticip).setVisibility(0);
            if (this.mIpAddressView == null) {
                Logger.d("WifiConfigController", "showIpConfigFields 0004");
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
                    Logger.d("WifiConfigController", "showIpConfigFields 0009");
                    this.mDns1View.setText(dnsIterator.next().getHostAddress());
                }
                if (dnsIterator.hasNext()) {
                    Logger.d("WifiConfigController", "showIpConfigFields 0010");
                    this.mDns2View.setText(dnsIterator.next().getHostAddress());
                }
            }
        } else {
            Logger.d("WifiConfigController", "showIpConfigFields 0011");
            this.mView.findViewById(R.id.staticip).setVisibility(8);
        }
        Logger.d("WifiConfigController", "showIpConfigFields 0012");
    }

    private void showProxyFields() {
        Logger.d("WifiConfigController", "showProxyFields 0001");
        WifiConfiguration config = null;
        this.mView.findViewById(R.id.proxy_settings_fields).setVisibility(0);
        if (this.mAccessPoint != null && this.mAccessPoint.networkId != -1) {
            Logger.d("WifiConfigController", "showProxyFields 0002");
            config = this.mAccessPoint.getConfig();
        }
        if (this.mProxySettingsSpinner.getSelectedItemPosition() == 1) {
            Logger.d("WifiConfigController", "showProxyFields 0003");
            this.mView.findViewById(R.id.proxy_warning_limited_support).setVisibility(0);
            this.mView.findViewById(R.id.proxy_fields).setVisibility(0);
            if (this.mProxyHostView == null) {
                Logger.d("WifiConfigController", "showProxyFields 0004");
                this.mProxyHostView = (TextView) this.mView.findViewById(R.id.proxy_hostname);
                this.mProxyHostView.addTextChangedListener(this);
                this.mProxyPortView = (TextView) this.mView.findViewById(R.id.proxy_port);
                this.mProxyPortView.addTextChangedListener(this);
                this.mProxyExclusionListView = (TextView) this.mView.findViewById(R.id.proxy_exclusionlist);
                this.mProxyExclusionListView.addTextChangedListener(this);
            }
            if (config != null) {
                Logger.d("WifiConfigController", "showProxyFields 0005");
                ProxyInfo proxyProperties = config.getHttpProxy();
                if (proxyProperties != null) {
                    this.mProxyHostView.setText(proxyProperties.getHost());
                    this.mProxyPortView.setText(Integer.toString(proxyProperties.getPort()));
                    this.mProxyExclusionListView.setText(proxyProperties.getExclusionListAsString());
                    return;
                }
                return;
            }
            return;
        }
        Logger.d("WifiConfigController", "showProxyFields 0007");
        this.mView.findViewById(R.id.proxy_warning_limited_support).setVisibility(8);
        this.mView.findViewById(R.id.proxy_fields).setVisibility(8);
    }

    private void loadCertificates(Spinner spinner, String prefix) {
        String[] certs;
        Logger.d("WifiConfigController", "loadCertificates 0001");
        Context context = this.mConfigUi.getContext();
        String[] certs2 = KeyStore.getInstance().saw(prefix, 1010);
        if (certs2 == null || certs2.length == 0) {
            Logger.d("WifiConfigController", "loadCertificates 0002");
            certs = new String[]{this.unspecifiedCert};
        } else {
            Logger.d("WifiConfigController", "loadCertificates 0003");
            String[] array = new String[certs2.length + 1];
            array[0] = this.unspecifiedCert;
            System.arraycopy(certs2, 0, array, 1, certs2.length);
            certs = array;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, certs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) adapter);
        Logger.d("WifiConfigController", "loadCertificates 0004");
    }

    private void setSelection(Spinner spinner, String value) {
        Logger.d("WifiConfigController", "setSelection 0001");
        if (value != null) {
            Logger.d("WifiConfigController", "setSelection 0002");
            ArrayAdapter<String> adapter = (ArrayAdapter) spinner.getAdapter();
            int i = adapter.getCount() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                Logger.d("WifiConfigController", "setSelection 0003");
                if (!value.equals(adapter.getItem(i))) {
                    i--;
                } else {
                    Logger.d("WifiConfigController", "setSelection 0004");
                    spinner.setSelection(i);
                    break;
                }
            }
        }
        Logger.d("WifiConfigController", "setSelection 0005");
    }

    public boolean isEdit() {
        Logger.d("WifiConfigController", "isEdit 0001");
        return this.mEdit;
    }

    @Override
    public void afterTextChanged(Editable s) {
        Logger.d("WifiConfigController", "afterTextChanged 0001");
        this.mTextViewChangedHandler.post(new Runnable() {
            @Override
            public void run() {
                Logger.d("WifiConfigController", "run 0001");
                WifiConfigController.this.setSubmitOrEnable();
            }
        });
        Logger.d("WifiConfigController", "afterTextChanged 0002");
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        Logger.d("WifiConfigController", "beforeTextChanged 0001");
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        Logger.d("WifiConfigController", "onTextChanged 0001");
    }

    @Override
    public void onClick(View view) {
        Logger.d("WifiConfigController", "onClick 0001");
        if (view.getId() == R.id.show_password) {
            Logger.d("WifiConfigController", "onClick 0002");
            int pos = this.mPasswordView.getSelectionEnd();
            this.mPasswordView.setInputType((((CheckBox) view).isChecked() ? 144 : 128) | 1);
            if (pos >= 0) {
                Logger.d("WifiConfigController", "onClick 0003");
                ((EditText) this.mPasswordView).setSelection(pos);
            }
        } else if (view.getId() == R.id.wifi_advanced_togglebox) {
            Logger.d("WifiConfigController", "onClick 0004");
            if (((CheckBox) view).isChecked()) {
                Logger.d("WifiConfigController", "onClick 0005");
                this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(0);
            } else {
                Logger.d("WifiConfigController", "onClick 0006");
                this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(8);
            }
        }
        Logger.d("WifiConfigController", "onClick 0007");
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Logger.d("WifiConfigController", "onItemSelected 0001");
        if (parent == this.mSecuritySpinner) {
            Logger.d("WifiConfigController", "onItemSelected 0002");
            this.mAccessPointSecurity = position;
            showSecurityFields();
        } else if (parent == this.mEapMethodSpinner) {
            Logger.d("WifiConfigController", "onItemSelected 0003");
            showSecurityFields();
        } else if (parent == this.mProxySettingsSpinner) {
            Logger.d("WifiConfigController", "onItemSelected 0004");
            showProxyFields();
        } else {
            Logger.d("WifiConfigController", "onItemSelected 0005");
            showIpConfigFields();
        }
        setSubmitOrEnable();
        Logger.d("WifiConfigController", "onItemSelected 0006");
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Logger.d("WifiConfigController", "onNothingSelected 0001");
    }
}
