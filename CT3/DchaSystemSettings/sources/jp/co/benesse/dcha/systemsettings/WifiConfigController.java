package jp.co.benesse.dcha.systemsettings;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.UserManager;
import android.security.KeyStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.co.benesse.dcha.util.Logger;

public class WifiConfigController implements TextWatcher, View.OnClickListener, AdapterView.OnItemSelectedListener, TextView.OnEditorActionListener, View.OnKeyListener {
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
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^$|^[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*$");
    private static final Pattern EXCLUSION_PATTERN = Pattern.compile("$|^[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*$");
    private IpConfiguration.IpAssignment mIpAssignment = IpConfiguration.IpAssignment.UNASSIGNED;
    private IpConfiguration.ProxySettings mProxySettings = IpConfiguration.ProxySettings.UNASSIGNED;
    private ProxyInfo mHttpProxy = null;
    private StaticIpConfiguration mStaticIpConfiguration = null;

    public WifiConfigController(NetworkSettingActivity activity, WifiConfigUiBase parent, View view, AccessPoint accessPoint, int mode) {
        Logger.d("WifiConfigController", "WifiConfigController 0001");
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
            this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
        } else {
            Logger.d("WifiConfigController", "WifiConfigController 0003");
            this.mConfigUi.setTitle(this.mAccessPoint.getSsid());
            ViewGroup group = (ViewGroup) this.mView.findViewById(R.id.info);
            boolean showAdvancedFields = false;
            if (this.mAccessPoint.isSaved()) {
                Logger.d("WifiConfigController", "WifiConfigController 0004");
                WifiConfiguration config = this.mAccessPoint.getConfig();
                if (config.getIpAssignment() == IpConfiguration.IpAssignment.STATIC) {
                    Logger.d("WifiConfigController", "WifiConfigController 0005");
                    this.mIpSettingsSpinner.setSelection(1);
                    showAdvancedFields = true;
                    StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                    if (staticConfig != null && staticConfig.ipAddress != null) {
                        Logger.d("WifiConfigController", "WifiConfigController 0006");
                        addRow(group, R.string.wifi_ip_address, staticConfig.ipAddress.getAddress().getHostAddress());
                    }
                } else {
                    Logger.d("WifiConfigController", "WifiConfigController 0007");
                    this.mIpSettingsSpinner.setSelection(0);
                }
                this.mSharedCheckBox.setEnabled(config.shared);
                if (!config.shared) {
                    Logger.d("WifiConfigController", "WifiConfigController 0008");
                    showAdvancedFields = true;
                }
                if (config.getProxySettings() == IpConfiguration.ProxySettings.STATIC) {
                    Logger.d("WifiConfigController", "WifiConfigController 0009");
                    this.mProxySettingsSpinner.setSelection(1);
                    showAdvancedFields = true;
                } else if (config.getProxySettings() == IpConfiguration.ProxySettings.PAC) {
                    Logger.d("WifiConfigController", "WifiConfigController 0010");
                    this.mProxySettingsSpinner.setSelection(2);
                    showAdvancedFields = true;
                } else {
                    Logger.d("WifiConfigController", "WifiConfigController 0011");
                    this.mProxySettingsSpinner.setSelection(0);
                }
                if (config != null && config.isPasspoint()) {
                    Logger.d("WifiConfigController", "WifiConfigController 0012");
                    addRow(group, R.string.passpoint_label, String.format(this.mContext.getString(R.string.passpoint_content), config.providerFriendlyName));
                }
            }
            if ((!this.mAccessPoint.isSaved() && !this.mAccessPoint.isActive()) || this.mMode != 0) {
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
            if (this.mMode == 2) {
                Logger.d("WifiConfigController", "WifiConfigController 0015");
                this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
            } else if (this.mMode == 1) {
                Logger.d("WifiConfigController", "WifiConfigController 0016");
                this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_connect));
            }
            NetworkInfo.DetailedState state = this.mAccessPoint.getDetailedState();
            String signalLevel = getSignalString();
            if (state == null && signalLevel != null) {
                Logger.d("WifiConfigController", "WifiConfigController 0017");
                this.mConfigUi.setSubmitButton(res.getString(R.string.wifi_connect));
            }
            if (state != null) {
                Logger.d("WifiConfigController", "WifiConfigController 0018");
                boolean isEphemeral = this.mAccessPoint.isEphemeral();
                WifiConfiguration config2 = this.mAccessPoint.getConfig();
                String providerFriendlyName = null;
                if (config2 != null && config2.isPasspoint()) {
                    Logger.d("WifiConfigController", "WifiConfigController 0019");
                    providerFriendlyName = config2.providerFriendlyName;
                }
                String summary = AccessPoint.getSummary(this.mConfigUi.getContext(), state, isEphemeral, providerFriendlyName);
                addRow(group, R.string.wifi_status, summary);
            }
            if (signalLevel != null) {
                Logger.d("WifiConfigController", "WifiConfigController 0020");
                addRow(group, R.string.wifi_signal, signalLevel);
            }
            WifiInfo info = this.mAccessPoint.getInfo();
            if (info != null && info.getLinkSpeed() != -1) {
                Logger.d("WifiConfigController", "WifiConfigController 0021");
                addRow(group, R.string.wifi_speed, String.format(res.getString(R.string.link_speed), Integer.valueOf(info.getLinkSpeed())));
            }
            if (info != null && info.getFrequency() != -1) {
                Logger.d("WifiConfigController", "WifiConfigController 0022");
                int frequency = info.getFrequency();
                String band = null;
                if (frequency >= 2400 && frequency < 2500) {
                    Logger.d("WifiConfigController", "WifiConfigController 0023");
                    band = res.getString(R.string.wifi_band_24ghz);
                } else if (frequency >= 4900 && frequency < 5900) {
                    Logger.d("WifiConfigController", "WifiConfigController 0024");
                    band = res.getString(R.string.wifi_band_5ghz);
                } else {
                    Logger.d("WifiConfigController", "WifiConfigController 0025");
                    Logger.d("WifiConfigController", "Unexpected frequency " + frequency);
                }
                if (band != null) {
                    Logger.d("WifiConfigController", "WifiConfigController 0026");
                    addRow(group, R.string.wifi_frequency, band);
                }
            }
            addRow(group, R.string.wifi_security, this.mAccessPoint.getSecurityString(false));
            this.mView.findViewById(R.id.ip_fields).setVisibility(8);
            if (this.mAccessPoint.isSaved() || this.mAccessPoint.isActive()) {
                Logger.d("WifiConfigController", "WifiConfigController 0027");
                this.mConfigUi.setForgetButton(res.getString(R.string.wifi_forget));
            }
        }
        if (!UserManager.isSplitSystemUser()) {
            Logger.d("WifiConfigController", "WifiConfigController 0027");
            this.mSharedCheckBox.setVisibility(8);
        }
        this.mConfigUi.setCancelButton(res.getString(R.string.wifi_cancel));
        if (this.mConfigUi.getSubmitButton() != null) {
            Logger.d("WifiConfigController", "WifiConfigController 0028");
            enableSubmitIfAppropriate();
        }
        Logger.d("WifiConfigController", "WifiConfigController 0029");
    }

    private void addRow(ViewGroup group, int name, String value) {
        Logger.d("WifiConfigController", "addRow 0001");
        View row = this.mConfigUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(name);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        group.addView(row);
        Logger.d("WifiConfigController", "addRow 0002");
    }

    private String getSignalString() {
        Logger.d("WifiConfigController", "getSignalString 0001");
        int level = this.mAccessPoint.getLevel();
        if (level <= -1 || level >= this.mLevels.length) {
            return null;
        }
        return this.mLevels[level];
    }

    void hideForgetButton() {
        Logger.d("WifiConfigController", "hideForgetButton 0001");
        Button forget = this.mConfigUi.getForgetButton();
        if (forget == null) {
            Logger.d("WifiConfigController", "hideForgetButton 0002");
        } else {
            forget.setVisibility(8);
            Logger.d("WifiConfigController", "hideForgetButton 0003");
        }
    }

    void hideSubmitButton() {
        Logger.d("WifiConfigController", "hideSubmitButton 0001");
        Button submit = this.mConfigUi.getSubmitButton();
        if (submit == null) {
            Logger.d("WifiConfigController", "hideSubmitButton 0002");
        } else {
            submit.setVisibility(8);
            Logger.d("WifiConfigController", "hideSubmitButton 0003");
        }
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
        if ((this.mSsidView != null && this.mSsidView.length() == 0) || ((this.mAccessPoint == null || !this.mAccessPoint.isSaved()) && passwordInvalid)) {
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

    void enableSubmitIfAppropriate() {
        Logger.d("WifiConfigController", "enableSubmitIfAppropriate 0001");
        Button submit = this.mConfigUi.getSubmitButton();
        if (submit == null) {
            Logger.d("WifiConfigController", "enableSubmitIfAppropriate 0002");
        } else {
            submit.setEnabled(isSubmittable());
            Logger.d("WifiConfigController", "enableSubmitIfAppropriate 0003");
        }
    }

    boolean isSubmittable() {
        boolean enabled;
        Logger.d("WifiConfigController", "isSubmittable 0001");
        boolean passwordInvalid = false;
        if (this.mPasswordView != null && ((this.mAccessPointSecurity == 1 && this.mPasswordView.length() == 0) || (this.mAccessPointSecurity == 2 && this.mPasswordView.length() < 8))) {
            Logger.d("WifiConfigController", "isSubmittable 0002");
            passwordInvalid = true;
        }
        if ((this.mSsidView != null && this.mSsidView.length() == 0) || ((this.mAccessPoint == null || !this.mAccessPoint.isSaved()) && passwordInvalid)) {
            Logger.d("WifiConfigController", "isSubmittable 0003");
            enabled = false;
        } else {
            Logger.d("WifiConfigController", "isSubmittable 0004");
            enabled = ipAndProxyFieldsAreValid();
        }
        if (this.mEapCaCertSpinner != null && this.mView.findViewById(R.id.l_ca_cert).getVisibility() != 8) {
            Logger.d("WifiConfigController", "isSubmittable 0005");
            String caCertSelection = (String) this.mEapCaCertSpinner.getSelectedItem();
            if (caCertSelection.equals(this.mUnspecifiedCertString)) {
                Logger.d("WifiConfigController", "isSubmittable 0006");
                enabled = false;
            }
            if (caCertSelection.equals(this.mUseSystemCertsString) && this.mEapDomainView != null && this.mView.findViewById(R.id.l_domain).getVisibility() != 8 && TextUtils.isEmpty(this.mEapDomainView.getText().toString())) {
                Logger.d("WifiConfigController", "isSubmittable 0007");
                enabled = false;
            }
        }
        if (this.mEapUserCertSpinner != null && this.mView.findViewById(R.id.l_user_cert).getVisibility() != 8 && ((String) this.mEapUserCertSpinner.getSelectedItem()).equals(this.mUnspecifiedCertString)) {
            Logger.d("WifiConfigController", "isSubmittable 0008");
            enabled = false;
        }
        Logger.d("WifiConfigController", "isSubmittable 0009");
        return enabled;
    }

    void showWarningMessagesIfAppropriate() {
        Logger.d("WifiConfigController", "showWarningMessagesIfAppropriate 0001");
        this.mView.findViewById(R.id.no_ca_cert_warning).setVisibility(8);
        this.mView.findViewById(R.id.no_domain_warning).setVisibility(8);
        if (this.mEapCaCertSpinner != null && this.mView.findViewById(R.id.l_ca_cert).getVisibility() != 8) {
            Logger.d("WifiConfigController", "showWarningMessagesIfAppropriate 0002");
            String caCertSelection = (String) this.mEapCaCertSpinner.getSelectedItem();
            if (caCertSelection.equals(this.mDoNotValidateEapServerString)) {
                Logger.d("WifiConfigController", "showWarningMessagesIfAppropriate 0003");
                this.mView.findViewById(R.id.no_ca_cert_warning).setVisibility(0);
            }
            if (caCertSelection.equals(this.mUseSystemCertsString) && this.mEapDomainView != null && this.mView.findViewById(R.id.l_domain).getVisibility() != 8 && TextUtils.isEmpty(this.mEapDomainView.getText().toString())) {
                Logger.d("WifiConfigController", "showWarningMessagesIfAppropriate 0004");
                this.mView.findViewById(R.id.no_domain_warning).setVisibility(0);
            }
        }
        Logger.d("WifiConfigController", "showWarningMessagesIfAppropriate 0005");
    }

    WifiConfiguration getConfig() {
        Logger.d("WifiConfigController", "getConfig 0001");
        if (this.mMode == 0) {
            Logger.d("WifiConfigController", "getConfig 0002");
            return null;
        }
        WifiConfiguration config = new WifiConfiguration();
        if (this.mAccessPoint == null) {
            Logger.d("WifiConfigController", "getConfig 0003");
            config.SSID = AccessPoint.convertToQuotedString(this.mSsidView.getText().toString());
            config.hiddenSSID = true;
        } else if (this.mAccessPoint.isSaved()) {
            Logger.d("WifiConfigController", "getConfig 0005");
            config.networkId = this.mAccessPoint.getConfig().networkId;
        } else {
            Logger.d("WifiConfigController", "getConfig 0004");
            config.SSID = AccessPoint.convertToQuotedString(this.mAccessPoint.getSsidStr());
        }
        config.shared = this.mSharedCheckBox.isChecked();
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
                    if (!password2.matches("[0-9A-Fa-f]{64}")) {
                        Logger.d("WifiConfigController", "getConfig 0014");
                        config.preSharedKey = '\"' + password2 + '\"';
                    } else {
                        Logger.d("WifiConfigController", "getConfig 0013");
                        config.preSharedKey = password2;
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
                        Logger.d("WifiConfigController", "getConfig 0016");
                        switch (phase2Method) {
                            case 0:
                                Logger.d("WifiConfigController", "getConfig 0017");
                                config.enterpriseConfig.setPhase2Method(0);
                                break;
                            case 1:
                                Logger.d("WifiConfigController", "getConfig 0018");
                                config.enterpriseConfig.setPhase2Method(3);
                                break;
                            case 2:
                                Logger.d("WifiConfigController", "getConfig 0019");
                                config.enterpriseConfig.setPhase2Method(4);
                                break;
                            default:
                                Logger.d("WifiConfigController", "getConfig 0020");
                                Logger.e("WifiConfigController", "Unknown phase2 method" + phase2Method);
                                break;
                        }
                        break;
                    default:
                        Logger.d("WifiConfigController", "getConfig 0021");
                        config.enterpriseConfig.setPhase2Method(phase2Method);
                        break;
                }
                String caCert = (String) this.mEapCaCertSpinner.getSelectedItem();
                config.enterpriseConfig.setCaCertificateAliases(null);
                config.enterpriseConfig.setCaPath(null);
                config.enterpriseConfig.setDomainSuffixMatch(this.mEapDomainView.getText().toString());
                if (caCert.equals(this.mUnspecifiedCertString) || caCert.equals(this.mDoNotValidateEapServerString)) {
                    Logger.d("WifiConfigController", "getConfig 0022");
                } else if (caCert.equals(this.mUseSystemCertsString)) {
                    Logger.d("WifiConfigController", "getConfig 0023");
                    config.enterpriseConfig.setCaPath("/system/etc/security/cacerts");
                } else if (caCert.equals(this.mMultipleCertSetString)) {
                    Logger.d("WifiConfigController", "getConfig 0024");
                    if (this.mAccessPoint != null) {
                        Logger.d("WifiConfigController", "getConfig 0025");
                        if (!this.mAccessPoint.isSaved()) {
                            Logger.d("WifiConfigController", "getConfig 0026");
                            Logger.e("WifiConfigController", "Multiple certs can only be set when editing saved network");
                        }
                        config.enterpriseConfig.setCaCertificateAliases(this.mAccessPoint.getConfig().enterpriseConfig.getCaCertificateAliases());
                    }
                } else {
                    Logger.d("WifiConfigController", "getConfig 0027");
                    config.enterpriseConfig.setCaCertificateAliases(new String[]{caCert});
                }
                if (config.enterpriseConfig.getCaCertificateAliases() != null && config.enterpriseConfig.getCaPath() != null) {
                    Logger.d("WifiConfigController", "getConfig 0028");
                    Logger.e("WifiConfigController", "ca_cert (" + config.enterpriseConfig.getCaCertificateAliases() + ") and ca_path (" + config.enterpriseConfig.getCaPath() + ") should not both be non-null");
                }
                String clientCert = (String) this.mEapUserCertSpinner.getSelectedItem();
                if (clientCert.equals(this.mUnspecifiedCertString) || clientCert.equals(this.mDoNotProvideEapUserCertString)) {
                    Logger.d("WifiConfigController", "getConfig 0029");
                    clientCert = "";
                }
                config.enterpriseConfig.setClientCertificateAlias(clientCert);
                if (eapMethod == 4 || eapMethod == 5 || eapMethod == 6) {
                    Logger.d("WifiConfigController", "getConfig 0030");
                    config.enterpriseConfig.setIdentity("");
                    config.enterpriseConfig.setAnonymousIdentity("");
                } else if (eapMethod == 3) {
                    Logger.d("WifiConfigController", "getConfig 0031");
                    config.enterpriseConfig.setIdentity(this.mEapIdentityView.getText().toString());
                    config.enterpriseConfig.setAnonymousIdentity("");
                } else {
                    Logger.d("WifiConfigController", "getConfig 0032");
                    config.enterpriseConfig.setIdentity(this.mEapIdentityView.getText().toString());
                    config.enterpriseConfig.setAnonymousIdentity(this.mEapAnonymousView.getText().toString());
                }
                if (!this.mPasswordView.isShown()) {
                    Logger.d("WifiConfigController", "getConfig 0035");
                    config.enterpriseConfig.setPassword(this.mPasswordView.getText().toString());
                } else {
                    Logger.d("WifiConfigController", "getConfig 0033");
                    if (this.mPasswordView.length() > 0) {
                        Logger.d("WifiConfigController", "getConfig 0034");
                        config.enterpriseConfig.setPassword(this.mPasswordView.getText().toString());
                    }
                }
                break;
            default:
                Logger.d("WifiConfigController", "getConfig 0036");
                return null;
        }
        config.setIpConfiguration(new IpConfiguration(this.mIpAssignment, this.mProxySettings, this.mStaticIpConfiguration, this.mHttpProxy));
        Logger.d("WifiConfigController", "getConfig 0037");
        return config;
    }

    private boolean ipAndProxyFieldsAreValid() {
        IpConfiguration.IpAssignment ipAssignment;
        int result;
        Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0001");
        if (this.mIpSettingsSpinner != null && this.mIpSettingsSpinner.getSelectedItemPosition() == 1) {
            ipAssignment = IpConfiguration.IpAssignment.STATIC;
        } else {
            ipAssignment = IpConfiguration.IpAssignment.DHCP;
        }
        this.mIpAssignment = ipAssignment;
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
                port = Integer.parseInt(portStr);
                result = validate(host, portStr, exclusionList);
            } catch (NumberFormatException e) {
                Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0005");
                Logger.e("WifiConfigController", "NumberFormatException", e);
                result = R.string.proxy_error_invalid_port;
            }
            if (result == 0) {
                Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0006");
                this.mHttpProxy = new ProxyInfo(host, port, exclusionList);
            } else {
                Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0007");
                return false;
            }
        } else if (selectedPosition == 2 && this.mProxyPacView != null) {
            Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0008");
            this.mProxySettings = IpConfiguration.ProxySettings.PAC;
            CharSequence uriSequence = this.mProxyPacView.getText();
            if (TextUtils.isEmpty(uriSequence)) {
                Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0009");
                return false;
            }
            Uri uri = Uri.parse(uriSequence.toString());
            if (uri == null) {
                Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0010");
                return false;
            }
            this.mHttpProxy = new ProxyInfo(uri);
        }
        Logger.d("WifiConfigController", "ipAndProxyFieldsAreValid 0011");
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
                int portVal = Integer.parseInt(port);
                if (portVal <= 0 || portVal > 65535) {
                    Logger.d("WifiConfigController", "validate 0009");
                    return R.string.proxy_error_invalid_port;
                }
            } catch (NumberFormatException e) {
                Logger.d("WifiConfigController", "validate 0008");
                Logger.e("WifiConfigController", "NumberFormatException", e);
                return R.string.proxy_error_invalid_port;
            }
        }
        Logger.d("WifiConfigController", "validate 0010");
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
        if (inetAddr == null || inetAddr.isAnyLocalAddress()) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0004");
            return R.string.wifi_ip_settings_invalid_ip_address;
        }
        int networkPrefixLength = -1;
        try {
            Logger.d("WifiConfigController", "validateIpConfigFields 0005");
            networkPrefixLength = Integer.parseInt(this.mNetworkPrefixLengthView.getText().toString());
        } catch (NumberFormatException e) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0007");
            Logger.e("WifiConfigController", "NumberFormatException", e);
            this.mNetworkPrefixLengthView.setText(this.mConfigUi.getContext().getString(R.string.wifi_network_prefix_length_hint));
        }
        if (networkPrefixLength < 0 || networkPrefixLength > 32) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0006");
            return R.string.wifi_ip_settings_invalid_network_prefix_length;
        }
        staticIpConfiguration.ipAddress = new LinkAddress(inetAddr, networkPrefixLength);
        String gateway = this.mGatewayView.getText().toString();
        if (TextUtils.isEmpty(gateway)) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0008");
            try {
                InetAddress netPart = NetworkUtils.getNetworkPart(inetAddr, networkPrefixLength);
                byte[] addr = netPart.getAddress();
                addr[addr.length - 1] = 1;
                this.mGatewayView.setText(InetAddress.getByAddress(addr).getHostAddress());
            } catch (RuntimeException e2) {
                Logger.d("WifiConfigController", "validateIpConfigFields 0009");
                Logger.e("WifiConfigController", "RuntimeException", e2);
            } catch (UnknownHostException e3) {
                Logger.d("WifiConfigController", "validateIpConfigFields 0010");
                Logger.e("WifiConfigController", "UnknownHostException", e3);
            }
        } else {
            Logger.d("WifiConfigController", "validateIpConfigFields 0011");
            InetAddress gatewayAddr = getIPv4Address(gateway);
            if (gatewayAddr == null) {
                Logger.d("WifiConfigController", "validateIpConfigFields 0012");
                return R.string.wifi_ip_settings_invalid_gateway;
            }
            if (gatewayAddr.isMulticastAddress()) {
                Logger.d("WifiConfigController", "validateIpConfigFields 0013");
                return R.string.wifi_ip_settings_invalid_gateway;
            }
            staticIpConfiguration.gateway = gatewayAddr;
        }
        String dns = this.mDns1View.getText().toString();
        if (TextUtils.isEmpty(dns)) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0014");
            this.mDns1View.setText(this.mConfigUi.getContext().getString(R.string.wifi_dns1_hint));
        } else {
            Logger.d("WifiConfigController", "validateIpConfigFields 0015");
            Inet4Address iPv4Address = getIPv4Address(dns);
            if (iPv4Address == null) {
                Logger.d("WifiConfigController", "validateIpConfigFields 0016");
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(iPv4Address);
        }
        if (this.mDns2View.length() > 0) {
            Logger.d("WifiConfigController", "validateIpConfigFields 0017");
            Inet4Address iPv4Address2 = getIPv4Address(this.mDns2View.getText().toString());
            if (iPv4Address2 == null) {
                Logger.d("WifiConfigController", "validateIpConfigFields 0018");
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(iPv4Address2);
        }
        Logger.d("WifiConfigController", "validateIpConfigFields 0019");
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
            this.mPasswordView.setOnEditorActionListener(this);
            this.mPasswordView.setOnKeyListener(this);
            ((CheckBox) this.mView.findViewById(R.id.show_password)).setOnClickListener(this);
            if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
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
            if (isWifiOnly(this.mContext) || !this.mContext.getResources().getBoolean(android.R.^attr-private.outKeycode)) {
                Logger.d("WifiConfigController", "showSecurityFields 0007");
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
                Logger.d("WifiConfigController", "showSecurityFields 0008");
                WifiEnterpriseConfig enterpriseConfig = this.mAccessPoint.getConfig().enterpriseConfig;
                int eapMethod = enterpriseConfig.getEapMethod();
                int phase2Method = enterpriseConfig.getPhase2Method();
                this.mEapMethodSpinner.setSelection(eapMethod);
                switch (eapMethod) {
                    case 0:
                        Logger.d("WifiConfigController", "showSecurityFields 0009");
                        switch (phase2Method) {
                            case 0:
                                Logger.d("WifiConfigController", "showSecurityFields 0010");
                                this.mPhase2Spinner.setSelection(0);
                                break;
                            case 1:
                            case 2:
                            default:
                                Logger.d("WifiConfigController", "showSecurityFields 0013");
                                Logger.e("WifiConfigController", "Invalid phase 2 method " + phase2Method);
                                break;
                            case 3:
                                Logger.d("WifiConfigController", "showSecurityFields 0011");
                                this.mPhase2Spinner.setSelection(1);
                                break;
                            case 4:
                                Logger.d("WifiConfigController", "showSecurityFields 0012");
                                this.mPhase2Spinner.setSelection(2);
                                break;
                        }
                        break;
                    default:
                        Logger.d("WifiConfigController", "showSecurityFields 0014");
                        this.mPhase2Spinner.setSelection(phase2Method);
                        break;
                }
                if (!TextUtils.isEmpty(enterpriseConfig.getCaPath())) {
                    Logger.d("WifiConfigController", "showSecurityFields 0015");
                    setSelection(this.mEapCaCertSpinner, this.mUseSystemCertsString);
                } else {
                    Logger.d("WifiConfigController", "showSecurityFields 0016");
                    String[] caCerts = enterpriseConfig.getCaCertificateAliases();
                    if (caCerts == null) {
                        Logger.d("WifiConfigController", "showSecurityFields 0017");
                        setSelection(this.mEapCaCertSpinner, this.mDoNotValidateEapServerString);
                    } else if (caCerts.length == 1) {
                        Logger.d("WifiConfigController", "showSecurityFields 0018");
                        setSelection(this.mEapCaCertSpinner, caCerts[0]);
                    } else {
                        Logger.d("WifiConfigController", "showSecurityFields 0019");
                        loadCertificates(this.mEapCaCertSpinner, "CACERT_", this.mDoNotValidateEapServerString, true, true);
                        setSelection(this.mEapCaCertSpinner, this.mMultipleCertSetString);
                    }
                }
                this.mEapDomainView.setText(enterpriseConfig.getDomainSuffixMatch());
                String userCert = enterpriseConfig.getClientCertificateAlias();
                if (TextUtils.isEmpty(userCert)) {
                    Logger.d("WifiConfigController", "showSecurityFields 0020");
                    setSelection(this.mEapUserCertSpinner, this.mDoNotProvideEapUserCertString);
                } else {
                    Logger.d("WifiConfigController", "showSecurityFields 0021");
                    setSelection(this.mEapUserCertSpinner, userCert);
                }
                this.mEapIdentityView.setText(enterpriseConfig.getIdentity());
                this.mEapAnonymousView.setText(enterpriseConfig.getAnonymousIdentity());
            } else {
                Logger.d("WifiConfigController", "showSecurityFields 0022");
                showEapFieldsByMethod(this.mEapMethodSpinner.getSelectedItemPosition());
            }
        } else {
            Logger.d("WifiConfigController", "showSecurityFields 0023");
            showEapFieldsByMethod(this.mEapMethodSpinner.getSelectedItemPosition());
        }
        Logger.d("WifiConfigController", "showSecurityFields 0024");
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
            case 0:
                Logger.d("WifiConfigController", "showEapFieldsByMethod 0003");
                if (this.mPhase2Adapter != this.mPhase2PeapAdapter) {
                    Logger.d("WifiConfigController", "showEapFieldsByMethod 0004");
                    this.mPhase2Adapter = this.mPhase2PeapAdapter;
                    this.mPhase2Spinner.setAdapter((SpinnerAdapter) this.mPhase2Adapter);
                }
                this.mView.findViewById(R.id.l_phase2).setVisibility(0);
                this.mView.findViewById(R.id.l_anonymous).setVisibility(0);
                setUserCertInvisible();
                break;
            case 1:
                Logger.d("WifiConfigController", "showEapFieldsByMethod 0002");
                this.mView.findViewById(R.id.l_user_cert).setVisibility(0);
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setPasswordInvisible();
                break;
            case 2:
                Logger.d("WifiConfigController", "showEapFieldsByMethod 0005");
                if (this.mPhase2Adapter != this.mPhase2FullAdapter) {
                    Logger.d("WifiConfigController", "showEapFieldsByMethod 0006");
                    this.mPhase2Adapter = this.mPhase2FullAdapter;
                    this.mPhase2Spinner.setAdapter((SpinnerAdapter) this.mPhase2Adapter);
                }
                this.mView.findViewById(R.id.l_phase2).setVisibility(0);
                this.mView.findViewById(R.id.l_anonymous).setVisibility(0);
                setUserCertInvisible();
                break;
            case 3:
                Logger.d("WifiConfigController", "showEapFieldsByMethod 0001");
                setPhase2Invisible();
                setCaCertInvisible();
                setDomainInvisible();
                setAnonymousIdentInvisible();
                setUserCertInvisible();
                break;
            case 4:
                Logger.d("WifiConfigController", "showEapFieldsByMethod 0007");
            case 5:
                Logger.d("WifiConfigController", "showEapFieldsByMethod 0008");
            case 6:
                Logger.d("WifiConfigController", "showEapFieldsByMethod 0009");
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setCaCertInvisible();
                setDomainInvisible();
                setUserCertInvisible();
                setPasswordInvisible();
                setIdentityInvisible();
                break;
        }
        if (this.mView.findViewById(R.id.l_ca_cert).getVisibility() != 8) {
            String eapCertSelection = (String) this.mEapCaCertSpinner.getSelectedItem();
            Logger.d("WifiConfigController", "showEapFieldsByMethod 0010");
            if (eapCertSelection.equals(this.mDoNotValidateEapServerString) || eapCertSelection.equals(this.mUnspecifiedCertString)) {
                Logger.d("WifiConfigController", "showEapFieldsByMethod 0011");
                setDomainInvisible();
            }
        }
        Logger.d("WifiConfigController", "showEapFieldsByMethod 0012");
    }

    private void setIdentityInvisible() {
        Logger.d("WifiConfigController", "setIdentityInvisible 0001");
        this.mView.findViewById(R.id.l_identity).setVisibility(8);
        this.mPhase2Spinner.setSelection(0);
        Logger.d("WifiConfigController", "setIdentityInvisible 0002");
    }

    private void setPhase2Invisible() {
        Logger.d("WifiConfigController", "setPhase2Invisible 0001");
        this.mView.findViewById(R.id.l_phase2).setVisibility(8);
        this.mPhase2Spinner.setSelection(0);
        Logger.d("WifiConfigController", "setPhase2Invisible 0002");
    }

    private void setCaCertInvisible() {
        Logger.d("WifiConfigController", "setCaCertInvisible 0001");
        this.mView.findViewById(R.id.l_ca_cert).setVisibility(8);
        setSelection(this.mEapCaCertSpinner, this.mUnspecifiedCertString);
        Logger.d("WifiConfigController", "setCaCertInvisible 0002");
    }

    private void setDomainInvisible() {
        Logger.d("WifiConfigController", "setDomainInvisible 0001");
        this.mView.findViewById(R.id.l_domain).setVisibility(8);
        this.mEapDomainView.setText("");
        Logger.d("WifiConfigController", "setDomainInvisible 0002");
    }

    private void setUserCertInvisible() {
        Logger.d("WifiConfigController", "setUserCertInvisible 0001");
        this.mView.findViewById(R.id.l_user_cert).setVisibility(8);
        setSelection(this.mEapUserCertSpinner, this.mUnspecifiedCertString);
        Logger.d("WifiConfigController", "setUserCertInvisible 0002");
    }

    private void setAnonymousIdentInvisible() {
        Logger.d("WifiConfigController", "setAnonymousIdentInvisible 0001");
        this.mView.findViewById(R.id.l_anonymous).setVisibility(8);
        this.mEapAnonymousView.setText("");
        Logger.d("WifiConfigController", "setAnonymousIdentInvisible 0002");
    }

    private void setPasswordInvisible() {
        Logger.d("WifiConfigController", "setPasswordInvisible 0001");
        this.mPasswordView.setText("");
        this.mView.findViewById(R.id.password_layout).setVisibility(8);
        this.mView.findViewById(R.id.show_password_layout).setVisibility(8);
        Logger.d("WifiConfigController", "setPasswordInvisible 0002");
    }

    private void showIpConfigFields() {
        Logger.d("WifiConfigController", "showIpConfigFields 0001");
        WifiConfiguration config = null;
        this.mView.findViewById(R.id.ip_fields).setVisibility(0);
        if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
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
            if (config != null) {
                Logger.d("WifiConfigController", "showIpConfigFields 0005");
                StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                if (staticConfig != null) {
                    Logger.d("WifiConfigController", "showIpConfigFields 0006");
                    if (staticConfig.ipAddress != null) {
                        Logger.d("WifiConfigController", "showIpConfigFields 0007");
                        this.mIpAddressView.setText(staticConfig.ipAddress.getAddress().getHostAddress());
                        this.mNetworkPrefixLengthView.setText(Integer.toString(staticConfig.ipAddress.getNetworkPrefixLength()));
                    }
                    if (staticConfig.gateway != null) {
                        Logger.d("WifiConfigController", "showIpConfigFields 0008");
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
        if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
            Logger.d("WifiConfigController", "showProxyFields 0002");
            config = this.mAccessPoint.getConfig();
        }
        if (this.mProxySettingsSpinner.getSelectedItemPosition() == 1) {
            Logger.d("WifiConfigController", "showProxyFields 0003");
            setVisibility(R.id.proxy_warning_limited_support, 0);
            setVisibility(R.id.proxy_fields, 0);
            setVisibility(R.id.proxy_pac_field, 8);
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
                    Logger.d("WifiConfigController", "showProxyFields 0006");
                    this.mProxyHostView.setText(proxyProperties.getHost());
                    this.mProxyPortView.setText(Integer.toString(proxyProperties.getPort()));
                    this.mProxyExclusionListView.setText(proxyProperties.getExclusionListAsString());
                }
            }
        } else if (this.mProxySettingsSpinner.getSelectedItemPosition() == 2) {
            Logger.d("WifiConfigController", "showProxyFields 0007");
            setVisibility(R.id.proxy_warning_limited_support, 8);
            setVisibility(R.id.proxy_fields, 8);
            setVisibility(R.id.proxy_pac_field, 0);
            if (this.mProxyPacView == null) {
                Logger.d("WifiConfigController", "showProxyFields 0008");
                this.mProxyPacView = (TextView) this.mView.findViewById(R.id.proxy_pac);
                this.mProxyPacView.addTextChangedListener(this);
            }
            if (config != null) {
                Logger.d("WifiConfigController", "showProxyFields 0009");
                ProxyInfo proxyInfo = config.getHttpProxy();
                if (proxyInfo != null) {
                    Logger.d("WifiConfigController", "showProxyFields 0010");
                    this.mProxyPacView.setText(proxyInfo.getPacFileUrl().toString());
                }
            }
        } else {
            Logger.d("WifiConfigController", "showProxyFields 0011");
            setVisibility(R.id.proxy_warning_limited_support, 8);
            setVisibility(R.id.proxy_fields, 8);
        }
        Logger.d("WifiConfigController", "showProxyFields 0012");
    }

    private void setVisibility(int id, int visibility) {
        View v = this.mView.findViewById(id);
        if (v == null) {
            return;
        }
        v.setVisibility(visibility);
    }

    private void loadCertificates(Spinner spinner, String prefix, String noCertificateString, boolean showMultipleCerts, boolean showUsePreinstalledCertOption) {
        Logger.d("WifiConfigController", "loadCertificates 0001");
        Context context = this.mConfigUi.getContext();
        ArrayList<String> certs = new ArrayList<>();
        certs.add(this.mUnspecifiedCertString);
        if (showMultipleCerts) {
            Logger.d("WifiConfigController", "loadCertificates 0002");
            certs.add(this.mMultipleCertSetString);
        }
        if (showUsePreinstalledCertOption) {
            Logger.d("WifiConfigController", "loadCertificates 0003");
            certs.add(this.mUseSystemCertsString);
        }
        certs.addAll(Arrays.asList(KeyStore.getInstance().list(prefix, 1010)));
        certs.add(noCertificateString);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, (String[]) certs.toArray(new String[certs.size()]));
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

    public int getMode() {
        Logger.d("WifiConfigController", "getMode 0001");
        return this.mMode;
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
    public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
        Logger.d("WifiConfigController", "onEditorAction 0001");
        if (textView == this.mPasswordView) {
            Logger.d("WifiConfigController", "onEditorAction 0002");
            if (id == 6 && isSubmittable()) {
                Logger.d("WifiConfigController", "onEditorAction 0003");
                this.mConfigUi.dispatchSubmit();
                return true;
            }
        }
        Logger.d("WifiConfigController", "onEditorAction 0004");
        return false;
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        Logger.d("WifiConfigController", "onKey 0001");
        if (view == this.mPasswordView) {
            Logger.d("WifiConfigController", "onKey 0002");
            if (keyCode == 66 && isSubmittable()) {
                Logger.d("WifiConfigController", "onKey 0003");
                this.mConfigUi.dispatchSubmit();
                return true;
            }
        }
        Logger.d("WifiConfigController", "onKey 0004");
        return false;
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
        } else if (parent == this.mEapMethodSpinner || parent == this.mEapCaCertSpinner) {
            Logger.d("WifiConfigController", "onItemSelected 0003");
            showSecurityFields();
        } else if (parent == this.mProxySettingsSpinner) {
            Logger.d("WifiConfigController", "onItemSelected 0004");
            showProxyFields();
        } else {
            Logger.d("WifiConfigController", "onItemSelected 0005");
            showIpConfigFields();
        }
        showWarningMessagesIfAppropriate();
        enableSubmitIfAppropriate();
        setSubmitOrEnable();
        Logger.d("WifiConfigController", "onItemSelected 0006");
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Logger.d("WifiConfigController", "onNothingSelected 0001");
    }

    public void updatePassword() {
        Logger.d("WifiConfigController", "updatePassword 0001");
        TextView passwdView = (TextView) this.mView.findViewById(R.id.password);
        passwdView.setInputType((((CheckBox) this.mView.findViewById(R.id.show_password)).isChecked() ? 144 : 128) | 1);
        Logger.d("WifiConfigController", "updatePassword 0002");
    }

    private static boolean isWifiOnly(Context context) {
        Logger.d("WifiConfigController", "isWifiOnly 0001");
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        return !cm.isNetworkSupported(0);
    }
}
