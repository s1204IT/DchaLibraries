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
import android.os.UserManager;
import android.security.KeyStore;
import android.text.Editable;
import android.text.InputFilter;
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
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.settings.ProxySelector;
import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.wifi.AccessPoint;
import com.mediatek.settings.wifi.Utf8ByteLengthFilter;
import com.mediatek.settings.wifi.WifiConfigControllerExt;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class WifiConfigController implements TextWatcher, View.OnKeyListener, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener, TextView.OnEditorActionListener {
    private final AccessPoint mAccessPoint;
    private int mAccessPointSecurity;
    private final WifiConfigUiBase mConfigUi;
    private Context mContext;
    private ScrollView mDialogContainer;
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
    private Spinner mHiddenSettingsSpinner;
    private TextView mHiddenWarningView;
    private TextView mIpAddressView;
    private Spinner mIpSettingsSpinner;
    private String[] mLevels;
    private Spinner mMeteredSettingsSpinner;
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
    private String mUnspecifiedCertString;
    private String mUseSystemCertsString;
    private final View mView;
    private WifiConfigControllerExt mWifiConfigControllerExt;
    private IpConfiguration.IpAssignment mIpAssignment = IpConfiguration.IpAssignment.UNASSIGNED;
    private IpConfiguration.ProxySettings mProxySettings = IpConfiguration.ProxySettings.UNASSIGNED;
    private ProxyInfo mHttpProxy = null;
    private StaticIpConfiguration mStaticIpConfiguration = null;

    public WifiConfigController(WifiConfigUiBase wifiConfigUiBase, View view, AccessPoint accessPoint, int i) {
        boolean z;
        String str;
        String string = null;
        this.mConfigUi = wifiConfigUiBase;
        this.mView = view;
        this.mAccessPoint = accessPoint;
        this.mAccessPointSecurity = accessPoint == null ? 0 : accessPoint.getSecurity();
        this.mMode = i;
        this.mContext = this.mConfigUi.getContext();
        Resources resources = this.mContext.getResources();
        this.mWifiConfigControllerExt = new WifiConfigControllerExt(this, this.mConfigUi, this.mView);
        this.mLevels = resources.getStringArray(R.array.wifi_signal);
        if (Utils.isWifiOnly(this.mContext) || !this.mContext.getResources().getBoolean(android.R.^attr-private.floatingToolbarCloseDrawable)) {
            this.mPhase2PeapAdapter = new ArrayAdapter<>(this.mContext, android.R.layout.simple_spinner_item, resources.getStringArray(R.array.wifi_peap_phase2_entries));
        } else {
            this.mPhase2PeapAdapter = new ArrayAdapter<>(this.mContext, android.R.layout.simple_spinner_item, resources.getStringArray(R.array.wifi_peap_phase2_entries_with_sim_auth));
        }
        this.mPhase2PeapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mPhase2FullAdapter = new ArrayAdapter<>(this.mContext, android.R.layout.simple_spinner_item, resources.getStringArray(R.array.wifi_phase2_entries));
        this.mPhase2FullAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.mUnspecifiedCertString = this.mContext.getString(R.string.wifi_unspecified);
        this.mMultipleCertSetString = this.mContext.getString(R.string.wifi_multiple_cert_added);
        this.mUseSystemCertsString = this.mContext.getString(R.string.wifi_use_system_certs);
        this.mDoNotProvideEapUserCertString = this.mContext.getString(R.string.wifi_do_not_provide_eap_user_cert);
        this.mDoNotValidateEapServerString = this.mContext.getString(R.string.wifi_do_not_validate_eap_server);
        this.mDialogContainer = (ScrollView) this.mView.findViewById(R.id.dialog_scrollview);
        this.mIpSettingsSpinner = (Spinner) this.mView.findViewById(R.id.ip_settings);
        this.mIpSettingsSpinner.setOnItemSelectedListener(this);
        this.mProxySettingsSpinner = (Spinner) this.mView.findViewById(R.id.proxy_settings);
        this.mProxySettingsSpinner.setOnItemSelectedListener(this);
        this.mSharedCheckBox = (CheckBox) this.mView.findViewById(R.id.shared);
        this.mMeteredSettingsSpinner = (Spinner) this.mView.findViewById(R.id.metered_settings);
        this.mHiddenSettingsSpinner = (Spinner) this.mView.findViewById(R.id.hidden_settings);
        this.mHiddenSettingsSpinner.setOnItemSelectedListener(this);
        this.mHiddenWarningView = (TextView) this.mView.findViewById(R.id.hidden_settings_warning);
        this.mHiddenWarningView.setVisibility(this.mHiddenSettingsSpinner.getSelectedItemPosition() == 0 ? 8 : 0);
        if (this.mAccessPoint == null) {
            this.mConfigUi.setTitle(R.string.wifi_add_network);
            this.mSsidView = (TextView) this.mView.findViewById(R.id.ssid);
            this.mSsidView.addTextChangedListener(this);
            this.mSsidView.setFilters(new InputFilter[]{new Utf8ByteLengthFilter(32)});
            this.mSecuritySpinner = (Spinner) this.mView.findViewById(R.id.security);
            this.mSecuritySpinner.setOnItemSelectedListener(this);
            this.mView.findViewById(R.id.type).setVisibility(0);
            showIpConfigFields();
            showProxyFields();
            this.mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(0);
            ((CheckBox) this.mView.findViewById(R.id.wifi_advanced_togglebox)).setOnCheckedChangeListener(this);
            this.mConfigUi.setSubmitButton(resources.getString(R.string.wifi_save));
            if (this.mPasswordView == null) {
                this.mPasswordView = (TextView) this.mView.findViewById(R.id.password);
                this.mPasswordView.addTextChangedListener(this);
                ((CheckBox) this.mView.findViewById(R.id.show_password)).setOnCheckedChangeListener(this);
            }
        } else {
            if (!this.mAccessPoint.isPasspointConfig()) {
                this.mConfigUi.setTitle(this.mAccessPoint.getSsid());
            } else {
                this.mConfigUi.setTitle(this.mAccessPoint.getConfigName());
            }
            this.mWifiConfigControllerExt.addViews(this.mConfigUi, this.mAccessPoint.getSecurityString(false));
            ViewGroup viewGroup = (ViewGroup) this.mView.findViewById(R.id.info);
            if (this.mAccessPoint.isSaved()) {
                WifiConfiguration config = this.mAccessPoint.getConfig();
                this.mMeteredSettingsSpinner.setSelection(config.meteredOverride);
                this.mHiddenSettingsSpinner.setSelection(config.hiddenSSID ? 1 : 0);
                if (config.getIpAssignment() != IpConfiguration.IpAssignment.STATIC) {
                    this.mIpSettingsSpinner.setSelection(0);
                    z = false;
                } else {
                    this.mIpSettingsSpinner.setSelection(1);
                    StaticIpConfiguration staticIpConfiguration = config.getStaticIpConfiguration();
                    if (staticIpConfiguration != null && staticIpConfiguration.ipAddress != null) {
                        addRow(viewGroup, R.string.wifi_ip_address, staticIpConfiguration.ipAddress.getAddress().getHostAddress());
                    }
                    z = true;
                }
                this.mSharedCheckBox.setEnabled(config.shared);
                z = config.shared ? z : true;
                if (config.getProxySettings() == IpConfiguration.ProxySettings.STATIC) {
                    this.mProxySettingsSpinner.setSelection(1);
                } else if (config.getProxySettings() != IpConfiguration.ProxySettings.PAC) {
                    this.mProxySettingsSpinner.setSelection(0);
                    if (config != null && config.isPasspoint()) {
                        addRow(viewGroup, R.string.passpoint_label, String.format(this.mContext.getString(R.string.passpoint_content), config.providerFriendlyName));
                    }
                } else {
                    this.mProxySettingsSpinner.setSelection(2);
                }
                z = true;
                if (config != null) {
                    addRow(viewGroup, R.string.passpoint_label, String.format(this.mContext.getString(R.string.passpoint_content), config.providerFriendlyName));
                }
            } else {
                z = false;
            }
            if ((!this.mAccessPoint.isSaved() && !this.mAccessPoint.isActive() && !this.mAccessPoint.isPasspointConfig()) || this.mMode != 0) {
                showSecurityFields();
                showIpConfigFields();
                showProxyFields();
                CheckBox checkBox = (CheckBox) this.mView.findViewById(R.id.wifi_advanced_togglebox);
                this.mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(this.mAccessPoint.isCarrierAp() ? 8 : 0);
                checkBox.setOnCheckedChangeListener(this);
                checkBox.setChecked(z);
                this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(z ? 0 : 8);
                if (this.mAccessPoint.isCarrierAp()) {
                    addRow(viewGroup, R.string.wifi_carrier_connect, String.format(this.mContext.getString(R.string.wifi_carrier_content), this.mAccessPoint.getCarrierName()));
                }
            }
            if (this.mMode == 2) {
                this.mConfigUi.setSubmitButton(resources.getString(R.string.wifi_save));
            } else if (this.mMode == 1) {
                this.mConfigUi.setSubmitButton(resources.getString(R.string.wifi_connect));
            } else {
                NetworkInfo.DetailedState detailedState = this.mAccessPoint.getDetailedState();
                String signalString = getSignalString();
                if ((detailedState == null || detailedState == NetworkInfo.DetailedState.DISCONNECTED) && signalString != null) {
                    this.mConfigUi.setSubmitButton(resources.getString(R.string.wifi_connect));
                } else {
                    if (detailedState != null) {
                        boolean zIsEphemeral = this.mAccessPoint.isEphemeral();
                        WifiConfiguration config2 = this.mAccessPoint.getConfig();
                        if (config2 != null && config2.isPasspoint()) {
                            str = config2.providerFriendlyName;
                        } else {
                            str = null;
                        }
                        addRow(viewGroup, R.string.wifi_status, AccessPoint.getSummary(this.mConfigUi.getContext(), detailedState, zIsEphemeral, str));
                    }
                    if (signalString != null) {
                        addRow(viewGroup, R.string.wifi_signal, signalString);
                    }
                    android.net.wifi.WifiInfo info = this.mAccessPoint.getInfo();
                    if (info != null && info.getLinkSpeed() != -1) {
                        addRow(viewGroup, R.string.wifi_speed, String.format(resources.getString(R.string.link_speed), Integer.valueOf(info.getLinkSpeed())));
                    }
                    if (info != null && info.getFrequency() != -1) {
                        int frequency = info.getFrequency();
                        if (frequency >= 2400 && frequency < 2500) {
                            string = resources.getString(R.string.wifi_band_24ghz);
                        } else if (frequency >= 4900 && frequency < 5900) {
                            string = resources.getString(R.string.wifi_band_5ghz);
                        } else {
                            Log.e("WifiConfigController", "Unexpected frequency " + frequency);
                        }
                        if (string != null) {
                            addRow(viewGroup, R.string.wifi_frequency, string);
                        }
                    }
                    this.mView.findViewById(R.id.ip_fields).setVisibility(8);
                }
                if (this.mAccessPoint.isSaved() || this.mAccessPoint.isActive() || this.mAccessPoint.isPasspointConfig()) {
                    this.mConfigUi.setForgetButton(resources.getString(R.string.wifi_forget));
                }
            }
        }
        if (!isSplitSystemUser()) {
            this.mSharedCheckBox.setVisibility(8);
        }
        this.mConfigUi.setCancelButton(resources.getString(R.string.wifi_cancel));
        if (this.mConfigUi.getSubmitButton() != null) {
            enableSubmitIfAppropriate();
        }
        this.mView.findViewById(R.id.l_wifidialog).requestFocus();
        this.mWifiConfigControllerExt.addWifiConfigView(this.mMode != 0);
    }

    boolean isSplitSystemUser() {
        return UserManager.isSplitSystemUser();
    }

    private void addRow(ViewGroup viewGroup, int i, String str) {
        View viewInflate = this.mConfigUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, viewGroup, false);
        ((TextView) viewInflate.findViewById(R.id.name)).setText(i);
        ((TextView) viewInflate.findViewById(R.id.value)).setText(str);
        viewGroup.addView(viewInflate);
    }

    String getSignalString() {
        int level;
        if (this.mAccessPoint.isReachable() && (level = this.mAccessPoint.getLevel()) > -1 && level < this.mLevels.length) {
            return this.mLevels[level];
        }
        return null;
    }

    void hideForgetButton() {
        Button forgetButton = this.mConfigUi.getForgetButton();
        if (forgetButton == null) {
            return;
        }
        forgetButton.setVisibility(8);
    }

    void hideSubmitButton() {
        Button submitButton = this.mConfigUi.getSubmitButton();
        if (submitButton == null) {
            return;
        }
        submitButton.setVisibility(8);
    }

    void enableSubmitIfAppropriate() {
        Button submitButton = this.mConfigUi.getSubmitButton();
        if (submitButton == null) {
            return;
        }
        submitButton.setEnabled(isSubmittable());
    }

    boolean isValidPsk(String str) {
        if (str.length() == 64 && str.matches("[0-9A-Fa-f]{64}")) {
            return true;
        }
        return str.length() >= 8 && str.length() <= 63;
    }

    boolean isSubmittable() {
        boolean zIpAndProxyFieldsAreValid;
        boolean z = true;
        if (this.mPasswordView == null || ((this.mAccessPointSecurity != 1 || this.mPasswordView.length() != 0) && (this.mAccessPointSecurity != 2 || isValidPsk(this.mPasswordView.getText().toString())))) {
            z = false;
        }
        boolean zEnableSubmitIfAppropriate = this.mWifiConfigControllerExt.enableSubmitIfAppropriate(this.mPasswordView, this.mAccessPointSecurity, z);
        if ((this.mSsidView == null || this.mSsidView.length() != 0) && (((this.mAccessPoint != null && this.mAccessPoint.isSaved()) || !zEnableSubmitIfAppropriate) && (this.mAccessPoint == null || !this.mAccessPoint.isSaved() || !zEnableSubmitIfAppropriate || this.mPasswordView.length() <= 0))) {
            zIpAndProxyFieldsAreValid = ipAndProxyFieldsAreValid();
        } else {
            zIpAndProxyFieldsAreValid = false;
        }
        if (this.mEapCaCertSpinner != null && this.mView.findViewById(R.id.l_ca_cert).getVisibility() != 8) {
            String str = (String) this.mEapCaCertSpinner.getSelectedItem();
            if (str.equals(this.mUnspecifiedCertString)) {
                zIpAndProxyFieldsAreValid = false;
            }
            if (str.equals(this.mUseSystemCertsString) && this.mEapDomainView != null && this.mView.findViewById(R.id.l_domain).getVisibility() != 8 && TextUtils.isEmpty(this.mEapDomainView.getText().toString())) {
                zIpAndProxyFieldsAreValid = false;
            }
        }
        if (this.mEapUserCertSpinner == null || this.mView.findViewById(R.id.l_user_cert).getVisibility() == 8 || !((String) this.mEapUserCertSpinner.getSelectedItem()).equals(this.mUnspecifiedCertString)) {
            return zIpAndProxyFieldsAreValid;
        }
        return false;
    }

    void showWarningMessagesIfAppropriate() {
        this.mView.findViewById(R.id.no_ca_cert_warning).setVisibility(8);
        this.mView.findViewById(R.id.no_domain_warning).setVisibility(8);
        this.mView.findViewById(R.id.ssid_too_long_warning).setVisibility(8);
        if (this.mSsidView != null && WifiUtils.isSSIDTooLong(this.mSsidView.getText().toString())) {
            this.mView.findViewById(R.id.ssid_too_long_warning).setVisibility(0);
        }
        if (this.mEapCaCertSpinner != null && this.mView.findViewById(R.id.l_ca_cert).getVisibility() != 8) {
            String str = (String) this.mEapCaCertSpinner.getSelectedItem();
            if (str.equals(this.mDoNotValidateEapServerString)) {
                this.mView.findViewById(R.id.no_ca_cert_warning).setVisibility(0);
            }
            if (str.equals(this.mUseSystemCertsString) && this.mEapDomainView != null && this.mView.findViewById(R.id.l_domain).getVisibility() != 8 && TextUtils.isEmpty(this.mEapDomainView.getText().toString())) {
                this.mView.findViewById(R.id.no_domain_warning).setVisibility(0);
            }
        }
    }

    public WifiConfiguration getConfig() {
        if (this.mMode == 0) {
            return null;
        }
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        if (this.mAccessPoint == null) {
            wifiConfiguration.SSID = AccessPoint.convertToQuotedString(this.mSsidView.getText().toString());
            wifiConfiguration.hiddenSSID = true;
        } else if (!this.mAccessPoint.isSaved()) {
            wifiConfiguration.SSID = AccessPoint.convertToQuotedString(this.mAccessPoint.getSsidStr());
        } else {
            wifiConfiguration.networkId = this.mAccessPoint.getConfig().networkId;
            wifiConfiguration.hiddenSSID = this.mAccessPoint.getConfig().hiddenSSID;
        }
        if (this.mAccessPoint != null && this.mAccessPoint.getConfig() != null && this.mAccessPoint.getConfig().hiddenSSID) {
            Log.d("WifiConfigController", "set hiddenSSID as true for " + this.mAccessPoint.getSsidStr());
            wifiConfiguration.hiddenSSID = true;
        }
        wifiConfiguration.shared = this.mSharedCheckBox.isChecked();
        switch (this.mAccessPointSecurity) {
            case 0:
                wifiConfiguration.allowedKeyManagement.set(0);
                break;
            case 1:
                wifiConfiguration.allowedKeyManagement.set(0);
                wifiConfiguration.allowedAuthAlgorithms.set(0);
                wifiConfiguration.allowedAuthAlgorithms.set(1);
                if (this.mPasswordView.length() != 0) {
                    int length = this.mPasswordView.length();
                    String string = this.mPasswordView.getText().toString();
                    if ((length == 10 || length == 26 || length == 58) && string.matches("[0-9A-Fa-f]*")) {
                        wifiConfiguration.wepKeys[0] = string;
                    } else {
                        wifiConfiguration.wepKeys[0] = '\"' + string + '\"';
                    }
                }
                break;
            case 2:
                wifiConfiguration.allowedKeyManagement.set(1);
                if (this.mPasswordView.length() != 0) {
                    String string2 = this.mPasswordView.getText().toString();
                    if (string2.matches("[0-9A-Fa-f]{64}")) {
                        wifiConfiguration.preSharedKey = string2;
                    } else {
                        wifiConfiguration.preSharedKey = '\"' + string2 + '\"';
                    }
                }
                break;
            case 3:
                wifiConfiguration.allowedKeyManagement.set(2);
                wifiConfiguration.allowedKeyManagement.set(3);
                wifiConfiguration.enterpriseConfig = new WifiEnterpriseConfig();
                int eapMethod = this.mWifiConfigControllerExt.getEapMethod(this.mEapMethodSpinner.getSelectedItemPosition());
                int selectedItemPosition = this.mPhase2Spinner.getSelectedItemPosition();
                wifiConfiguration.enterpriseConfig.setEapMethod(eapMethod);
                if (eapMethod == 0) {
                    switch (selectedItemPosition) {
                        case 0:
                            wifiConfiguration.enterpriseConfig.setPhase2Method(0);
                            break;
                        case 1:
                            wifiConfiguration.enterpriseConfig.setPhase2Method(3);
                            break;
                        case 2:
                            wifiConfiguration.enterpriseConfig.setPhase2Method(4);
                            break;
                        case 3:
                            wifiConfiguration.enterpriseConfig.setPhase2Method(5);
                            break;
                        case 4:
                            wifiConfiguration.enterpriseConfig.setPhase2Method(6);
                            break;
                        case 5:
                            wifiConfiguration.enterpriseConfig.setPhase2Method(7);
                            break;
                        default:
                            Log.e("WifiConfigController", "Unknown phase2 method" + selectedItemPosition);
                            break;
                    }
                } else {
                    wifiConfiguration.enterpriseConfig.setPhase2Method(selectedItemPosition);
                }
                String str = (String) this.mEapCaCertSpinner.getSelectedItem();
                wifiConfiguration.enterpriseConfig.setCaCertificateAliases(null);
                wifiConfiguration.enterpriseConfig.setCaPath(null);
                wifiConfiguration.enterpriseConfig.setDomainSuffixMatch(this.mEapDomainView.getText().toString());
                if (!str.equals(this.mUnspecifiedCertString) && !str.equals(this.mDoNotValidateEapServerString)) {
                    if (str.equals(this.mUseSystemCertsString)) {
                        wifiConfiguration.enterpriseConfig.setCaPath("/system/etc/security/cacerts");
                    } else if (str.equals(this.mMultipleCertSetString)) {
                        if (this.mAccessPoint != null) {
                            if (!this.mAccessPoint.isSaved()) {
                                Log.e("WifiConfigController", "Multiple certs can only be set when editing saved network");
                            }
                            wifiConfiguration.enterpriseConfig.setCaCertificateAliases(this.mAccessPoint.getConfig().enterpriseConfig.getCaCertificateAliases());
                        }
                    } else {
                        wifiConfiguration.enterpriseConfig.setCaCertificateAliases(new String[]{str});
                    }
                }
                if (wifiConfiguration.enterpriseConfig.getCaCertificateAliases() != null && wifiConfiguration.enterpriseConfig.getCaPath() != null) {
                    Log.e("WifiConfigController", "ca_cert (" + wifiConfiguration.enterpriseConfig.getCaCertificateAliases() + ") and ca_path (" + wifiConfiguration.enterpriseConfig.getCaPath() + ") should not both be non-null");
                }
                String str2 = (String) this.mEapUserCertSpinner.getSelectedItem();
                if (str2.equals(this.mUnspecifiedCertString) || str2.equals(this.mDoNotProvideEapUserCertString)) {
                    str2 = "";
                }
                wifiConfiguration.enterpriseConfig.setClientCertificateAlias(str2);
                if (eapMethod == 4 || eapMethod == 5 || eapMethod == 6) {
                    wifiConfiguration.enterpriseConfig.setIdentity("");
                    wifiConfiguration.enterpriseConfig.setAnonymousIdentity("");
                } else if (eapMethod == 3) {
                    wifiConfiguration.enterpriseConfig.setIdentity(this.mEapIdentityView.getText().toString());
                    wifiConfiguration.enterpriseConfig.setAnonymousIdentity("");
                } else {
                    wifiConfiguration.enterpriseConfig.setIdentity(this.mEapIdentityView.getText().toString());
                    wifiConfiguration.enterpriseConfig.setAnonymousIdentity(this.mEapAnonymousView.getText().toString());
                }
                if (!this.mPasswordView.isShown() || this.mPasswordView.length() > 0) {
                    wifiConfiguration.enterpriseConfig.setPassword(this.mPasswordView.getText().toString());
                }
                break;
            case 4:
            case 5:
                break;
            default:
                return null;
        }
        this.mWifiConfigControllerExt.setConfig(wifiConfiguration, this.mAccessPointSecurity, this.mPasswordView, this.mEapMethodSpinner);
        wifiConfiguration.setIpConfiguration(new IpConfiguration(this.mIpAssignment, this.mProxySettings, this.mStaticIpConfiguration, this.mHttpProxy));
        if (this.mMeteredSettingsSpinner != null) {
            wifiConfiguration.meteredOverride = this.mMeteredSettingsSpinner.getSelectedItemPosition();
        }
        return wifiConfiguration;
    }

    private boolean ipAndProxyFieldsAreValid() {
        IpConfiguration.IpAssignment ipAssignment;
        Uri uri;
        int i;
        int iValidate;
        if (this.mIpSettingsSpinner != null && this.mIpSettingsSpinner.getSelectedItemPosition() == 1) {
            ipAssignment = IpConfiguration.IpAssignment.STATIC;
        } else {
            ipAssignment = IpConfiguration.IpAssignment.DHCP;
        }
        this.mIpAssignment = ipAssignment;
        if (this.mIpAssignment == IpConfiguration.IpAssignment.STATIC) {
            this.mStaticIpConfiguration = new StaticIpConfiguration();
            if (validateIpConfigFields(this.mStaticIpConfiguration) != 0) {
                return false;
            }
        }
        int selectedItemPosition = this.mProxySettingsSpinner.getSelectedItemPosition();
        this.mProxySettings = IpConfiguration.ProxySettings.NONE;
        this.mHttpProxy = null;
        if (selectedItemPosition == 1 && this.mProxyHostView != null) {
            this.mProxySettings = IpConfiguration.ProxySettings.STATIC;
            String string = this.mProxyHostView.getText().toString();
            String string2 = this.mProxyPortView.getText().toString();
            String string3 = this.mProxyExclusionListView.getText().toString();
            try {
                i = Integer.parseInt(string2);
            } catch (NumberFormatException e) {
                i = 0;
            }
            try {
                iValidate = ProxySelector.validate(string, string2, string3);
            } catch (NumberFormatException e2) {
                iValidate = R.string.proxy_error_invalid_port;
            }
            if (iValidate != 0) {
                return false;
            }
            this.mHttpProxy = new ProxyInfo(string, i, string3);
        } else if (selectedItemPosition == 2 && this.mProxyPacView != null) {
            this.mProxySettings = IpConfiguration.ProxySettings.PAC;
            CharSequence text = this.mProxyPacView.getText();
            if (TextUtils.isEmpty(text) || (uri = Uri.parse(text.toString())) == null) {
                return false;
            }
            this.mHttpProxy = new ProxyInfo(uri);
        }
        return true;
    }

    private Inet4Address getIPv4Address(String str) {
        try {
            return (Inet4Address) NetworkUtils.numericToInetAddress(str);
        } catch (ClassCastException | IllegalArgumentException e) {
            return null;
        }
    }

    private int validateIpConfigFields(StaticIpConfiguration staticIpConfiguration) {
        Inet4Address iPv4Address;
        int i;
        if (this.mIpAddressView == null) {
            return 0;
        }
        String string = this.mIpAddressView.getText().toString();
        if (TextUtils.isEmpty(string) || (iPv4Address = getIPv4Address(string)) == null || iPv4Address.equals(Inet4Address.ANY)) {
            return R.string.wifi_ip_settings_invalid_ip_address;
        }
        try {
            try {
                i = Integer.parseInt(this.mNetworkPrefixLengthView.getText().toString());
            } catch (IllegalArgumentException e) {
                return R.string.wifi_ip_settings_invalid_ip_address;
            }
        } catch (NumberFormatException e2) {
            i = -1;
        }
        if (i < 0 || i > 32) {
            return R.string.wifi_ip_settings_invalid_network_prefix_length;
        }
        try {
            staticIpConfiguration.ipAddress = new LinkAddress(iPv4Address, i);
        } catch (NumberFormatException e3) {
            this.mNetworkPrefixLengthView.setText(this.mConfigUi.getContext().getString(R.string.wifi_network_prefix_length_hint));
        }
        String string2 = this.mGatewayView.getText().toString();
        if (TextUtils.isEmpty(string2)) {
            try {
                byte[] address = NetworkUtils.getNetworkPart(iPv4Address, i).getAddress();
                address[address.length - 1] = 1;
                this.mGatewayView.setText(InetAddress.getByAddress(address).getHostAddress());
            } catch (RuntimeException e4) {
            } catch (UnknownHostException e5) {
            }
        } else {
            Inet4Address iPv4Address2 = getIPv4Address(string2);
            if (iPv4Address2 == null || iPv4Address2.isMulticastAddress()) {
                return R.string.wifi_ip_settings_invalid_gateway;
            }
            staticIpConfiguration.gateway = iPv4Address2;
        }
        String string3 = this.mDns1View.getText().toString();
        if (TextUtils.isEmpty(string3)) {
            this.mDns1View.setText(this.mConfigUi.getContext().getString(R.string.wifi_dns1_hint));
        } else {
            Inet4Address iPv4Address3 = getIPv4Address(string3);
            if (iPv4Address3 == null) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(iPv4Address3);
        }
        if (this.mDns2View.length() > 0) {
            Inet4Address iPv4Address4 = getIPv4Address(this.mDns2View.getText().toString());
            if (iPv4Address4 == null) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(iPv4Address4);
        }
        return 0;
    }

    private void showSecurityFields() {
        if (this.mWifiConfigControllerExt.showSecurityFields(this.mAccessPointSecurity, this.mMode != 0)) {
            return;
        }
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
            this.mWifiConfigControllerExt.setEapmethodSpinnerAdapter();
            this.mEapMethodSpinner.setOnItemSelectedListener(this);
            if (Utils.isWifiOnly(this.mContext) || !this.mContext.getResources().getBoolean(android.R.^attr-private.floatingToolbarCloseDrawable)) {
                ArrayAdapter arrayAdapter = new ArrayAdapter(this.mContext, android.R.layout.simple_spinner_item, this.mContext.getResources().getStringArray(R.array.eap_method_without_sim_auth));
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                this.mEapMethodSpinner.setAdapter((SpinnerAdapter) arrayAdapter);
            }
            this.mPhase2Spinner = (Spinner) this.mView.findViewById(R.id.phase2);
            this.mPhase2Spinner.setOnItemSelectedListener(this);
            this.mEapCaCertSpinner = (Spinner) this.mView.findViewById(R.id.ca_cert);
            this.mEapCaCertSpinner.setOnItemSelectedListener(this);
            this.mEapDomainView = (TextView) this.mView.findViewById(R.id.domain);
            this.mEapDomainView.addTextChangedListener(this);
            this.mEapUserCertSpinner = (Spinner) this.mView.findViewById(R.id.user_cert);
            this.mEapUserCertSpinner.setOnItemSelectedListener(this);
            this.mEapIdentityView = (TextView) this.mView.findViewById(R.id.identity);
            this.mEapAnonymousView = (TextView) this.mView.findViewById(R.id.anonymous);
            if (this.mAccessPoint != null && this.mAccessPoint.isCarrierAp()) {
                this.mEapMethodSpinner.setSelection(this.mAccessPoint.getCarrierApEapType());
            }
            loadCertificates(this.mEapCaCertSpinner, "CACERT_", this.mDoNotValidateEapServerString, false, true);
            loadCertificates(this.mEapUserCertSpinner, "USRPKEY_", this.mDoNotProvideEapUserCertString, false, false);
            if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
                WifiEnterpriseConfig wifiEnterpriseConfig = this.mAccessPoint.getConfig().enterpriseConfig;
                int eapMethod = wifiEnterpriseConfig.getEapMethod();
                int phase2Method = wifiEnterpriseConfig.getPhase2Method();
                this.mEapMethodSpinner.setSelection(eapMethod);
                showEapFieldsByMethod(eapMethod);
                this.mWifiConfigControllerExt.setEapMethodSelection(this.mEapMethodSpinner, eapMethod);
                if (eapMethod == 0) {
                    if (phase2Method == 0) {
                        this.mPhase2Spinner.setSelection(0);
                    } else {
                        switch (phase2Method) {
                            case 3:
                                this.mPhase2Spinner.setSelection(1);
                                break;
                            case 4:
                                this.mPhase2Spinner.setSelection(2);
                                break;
                            case 5:
                                this.mPhase2Spinner.setSelection(3);
                                break;
                            case 6:
                                this.mPhase2Spinner.setSelection(4);
                                break;
                            case 7:
                                this.mPhase2Spinner.setSelection(5);
                                break;
                            default:
                                Log.e("WifiConfigController", "Invalid phase 2 method " + phase2Method);
                                break;
                        }
                    }
                } else {
                    this.mPhase2Spinner.setSelection(phase2Method);
                }
                if (!TextUtils.isEmpty(wifiEnterpriseConfig.getCaPath())) {
                    setSelection(this.mEapCaCertSpinner, this.mUseSystemCertsString);
                } else {
                    String[] caCertificateAliases = wifiEnterpriseConfig.getCaCertificateAliases();
                    if (caCertificateAliases == null) {
                        setSelection(this.mEapCaCertSpinner, this.mDoNotValidateEapServerString);
                    } else if (caCertificateAliases.length == 1) {
                        setSelection(this.mEapCaCertSpinner, caCertificateAliases[0]);
                    } else {
                        loadCertificates(this.mEapCaCertSpinner, "CACERT_", this.mDoNotValidateEapServerString, true, true);
                        setSelection(this.mEapCaCertSpinner, this.mMultipleCertSetString);
                    }
                }
                this.mEapDomainView.setText(wifiEnterpriseConfig.getDomainSuffixMatch());
                String clientCertificateAlias = wifiEnterpriseConfig.getClientCertificateAlias();
                if (TextUtils.isEmpty(clientCertificateAlias)) {
                    setSelection(this.mEapUserCertSpinner, this.mDoNotProvideEapUserCertString);
                } else {
                    setSelection(this.mEapUserCertSpinner, clientCertificateAlias);
                }
                this.mEapIdentityView.setText(wifiEnterpriseConfig.getIdentity());
                this.mEapAnonymousView.setText(wifiEnterpriseConfig.getAnonymousIdentity());
            } else {
                this.mPhase2Spinner = (Spinner) this.mView.findViewById(R.id.phase2);
                showEapFieldsByMethod(this.mEapMethodSpinner.getSelectedItemPosition());
            }
        } else {
            showEapFieldsByMethod(this.mEapMethodSpinner.getSelectedItemPosition());
        }
        this.mWifiConfigControllerExt.setEapMethodFields(this.mMode != 0);
        this.mWifiConfigControllerExt.showEapSimSlotByMethod(this.mEapMethodSpinner.getSelectedItemPosition());
    }

    private void showEapFieldsByMethod(int i) {
        this.mView.findViewById(R.id.l_method).setVisibility(0);
        this.mView.findViewById(R.id.l_identity).setVisibility(0);
        this.mView.findViewById(R.id.l_domain).setVisibility(0);
        this.mView.findViewById(R.id.l_ca_cert).setVisibility(0);
        this.mView.findViewById(R.id.password_layout).setVisibility(0);
        this.mView.findViewById(R.id.show_password_layout).setVisibility(0);
        this.mConfigUi.getContext();
        switch (i) {
            case 0:
                if (this.mPhase2Adapter != this.mPhase2PeapAdapter) {
                    this.mPhase2Adapter = this.mPhase2PeapAdapter;
                    this.mPhase2Spinner.setAdapter((SpinnerAdapter) this.mPhase2Adapter);
                }
                this.mView.findViewById(R.id.l_phase2).setVisibility(0);
                this.mView.findViewById(R.id.l_anonymous).setVisibility(0);
                showPeapFields();
                setUserCertInvisible();
                break;
            case 1:
                this.mView.findViewById(R.id.l_user_cert).setVisibility(0);
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setPasswordInvisible();
                break;
            case 2:
                if (this.mPhase2Adapter != this.mPhase2FullAdapter) {
                    this.mPhase2Adapter = this.mPhase2FullAdapter;
                    this.mPhase2Spinner.setAdapter((SpinnerAdapter) this.mPhase2Adapter);
                }
                this.mView.findViewById(R.id.l_phase2).setVisibility(0);
                this.mView.findViewById(R.id.l_anonymous).setVisibility(0);
                setUserCertInvisible();
                break;
            case 3:
                setPhase2Invisible();
                setCaCertInvisible();
                setDomainInvisible();
                setAnonymousIdentInvisible();
                setUserCertInvisible();
                break;
            case 4:
            case 5:
            case 6:
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setCaCertInvisible();
                setDomainInvisible();
                setUserCertInvisible();
                setPasswordInvisible();
                setIdentityInvisible();
                if (this.mAccessPoint != null && this.mAccessPoint.isCarrierAp()) {
                    setEapMethodInvisible();
                }
                break;
        }
        if (this.mView.findViewById(R.id.l_ca_cert).getVisibility() != 8) {
            String str = (String) this.mEapCaCertSpinner.getSelectedItem();
            if (str.equals(this.mDoNotValidateEapServerString) || str.equals(this.mUnspecifiedCertString)) {
                setDomainInvisible();
            }
        }
    }

    private void showPeapFields() {
        int selectedItemPosition = this.mPhase2Spinner.getSelectedItemPosition();
        if (selectedItemPosition != 3 && selectedItemPosition != 4 && selectedItemPosition != 5) {
            this.mView.findViewById(R.id.l_identity).setVisibility(0);
            this.mView.findViewById(R.id.l_anonymous).setVisibility(0);
            this.mView.findViewById(R.id.password_layout).setVisibility(0);
            this.mView.findViewById(R.id.show_password_layout).setVisibility(0);
            return;
        }
        this.mEapIdentityView.setText("");
        this.mView.findViewById(R.id.l_identity).setVisibility(8);
        setPasswordInvisible();
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

    private void setEapMethodInvisible() {
        this.mView.findViewById(R.id.eap).setVisibility(8);
    }

    private void showIpConfigFields() {
        WifiConfiguration config;
        StaticIpConfiguration staticIpConfiguration;
        this.mView.findViewById(R.id.ip_fields).setVisibility(0);
        if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
            config = this.mAccessPoint.getConfig();
        } else {
            config = null;
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
            if (config != null && (staticIpConfiguration = config.getStaticIpConfiguration()) != null) {
                if (staticIpConfiguration.ipAddress != null) {
                    this.mIpAddressView.setText(staticIpConfiguration.ipAddress.getAddress().getHostAddress());
                    this.mNetworkPrefixLengthView.setText(Integer.toString(staticIpConfiguration.ipAddress.getNetworkPrefixLength()));
                }
                if (staticIpConfiguration.gateway != null) {
                    this.mGatewayView.setText(staticIpConfiguration.gateway.getHostAddress());
                }
                Iterator it = staticIpConfiguration.dnsServers.iterator();
                if (it.hasNext()) {
                    this.mDns1View.setText(((InetAddress) it.next()).getHostAddress());
                }
                if (it.hasNext()) {
                    this.mDns2View.setText(((InetAddress) it.next()).getHostAddress());
                    return;
                }
                return;
            }
            return;
        }
        this.mView.findViewById(R.id.staticip).setVisibility(8);
    }

    private void showProxyFields() {
        WifiConfiguration config;
        ProxyInfo httpProxy;
        ProxyInfo httpProxy2;
        this.mView.findViewById(R.id.proxy_settings_fields).setVisibility(0);
        if (this.mAccessPoint != null && this.mAccessPoint.isSaved()) {
            config = this.mAccessPoint.getConfig();
        } else {
            config = null;
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
                this.mWifiConfigControllerExt.setProxyText(this.mView);
            }
            if (config != null && (httpProxy2 = config.getHttpProxy()) != null) {
                this.mProxyHostView.setText(httpProxy2.getHost());
                this.mProxyPortView.setText(Integer.toString(httpProxy2.getPort()));
                this.mProxyExclusionListView.setText(httpProxy2.getExclusionListAsString());
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
            if (config != null && (httpProxy = config.getHttpProxy()) != null) {
                this.mProxyPacView.setText(httpProxy.getPacFileUrl().toString());
                return;
            }
            return;
        }
        setVisibility(R.id.proxy_warning_limited_support, 8);
        setVisibility(R.id.proxy_fields, 8);
        setVisibility(R.id.proxy_pac_field, 8);
    }

    private void setVisibility(int i, int i2) {
        View viewFindViewById = this.mView.findViewById(i);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(i2);
        }
    }

    KeyStore getKeyStore() {
        return KeyStore.getInstance();
    }

    private void loadCertificates(Spinner spinner, String str, String str2, boolean z, boolean z2) {
        Context context = this.mConfigUi.getContext();
        ArrayList arrayList = new ArrayList();
        arrayList.add(this.mUnspecifiedCertString);
        if (z) {
            arrayList.add(this.mMultipleCertSetString);
        }
        if (z2) {
            arrayList.add(this.mUseSystemCertsString);
        }
        try {
            arrayList.addAll(Arrays.asList(getKeyStore().list(str, 1010)));
        } catch (Exception e) {
            Log.e("WifiConfigController", "can't get the certificate list from KeyStore");
        }
        arrayList.add(str2);
        ArrayAdapter arrayAdapter = new ArrayAdapter(context, android.R.layout.simple_spinner_item, (String[]) arrayList.toArray(new String[arrayList.size()]));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) arrayAdapter);
    }

    private void setSelection(Spinner spinner, String str) {
        if (str != null) {
            ArrayAdapter arrayAdapter = (ArrayAdapter) spinner.getAdapter();
            for (int count = arrayAdapter.getCount() - 1; count >= 0; count--) {
                if (str.equals(arrayAdapter.getItem(count))) {
                    spinner.setSelection(count);
                    return;
                }
            }
        }
    }

    public int getMode() {
        return this.mMode;
    }

    @Override
    public void afterTextChanged(Editable editable) {
        ThreadUtils.postOnMainThread(new Runnable() {
            @Override
            public final void run() {
                WifiConfigController.lambda$afterTextChanged$0(this.f$0);
            }
        });
    }

    public static void lambda$afterTextChanged$0(WifiConfigController wifiConfigController) {
        wifiConfigController.showWarningMessagesIfAppropriate();
        wifiConfigController.enableSubmitIfAppropriate();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (textView == this.mPasswordView && i == 6 && isSubmittable()) {
            this.mConfigUi.dispatchSubmit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (view == this.mPasswordView && i == 66 && isSubmittable()) {
            this.mConfigUi.dispatchSubmit();
            return true;
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        int i;
        int i2;
        if (compoundButton.getId() == R.id.show_password) {
            int selectionEnd = this.mPasswordView.getSelectionEnd();
            this.mPasswordView.setInputType((z ? 144 : 128) | 1);
            if (selectionEnd >= 0) {
                ((EditText) this.mPasswordView).setSelection(selectionEnd);
                return;
            }
            return;
        }
        if (compoundButton.getId() == R.id.wifi_advanced_togglebox) {
            View viewFindViewById = this.mView.findViewById(R.id.wifi_advanced_toggle);
            if (z) {
                i = 0;
                i2 = R.string.wifi_advanced_toggle_description_expanded;
            } else {
                i = 8;
                i2 = R.string.wifi_advanced_toggle_description_collapsed;
            }
            this.mView.findViewById(R.id.wifi_advanced_fields).setVisibility(i);
            viewFindViewById.setContentDescription(this.mContext.getString(i2));
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        int i2;
        if (adapterView == this.mSecuritySpinner) {
            this.mAccessPointSecurity = i;
            this.mAccessPointSecurity = this.mWifiConfigControllerExt.getSecurity(this.mAccessPointSecurity);
            showSecurityFields();
        } else if (adapterView == this.mEapMethodSpinner || adapterView == this.mEapCaCertSpinner) {
            showSecurityFields();
        } else if (adapterView == this.mPhase2Spinner && this.mEapMethodSpinner.getSelectedItemPosition() == 0) {
            showPeapFields();
        } else if (adapterView == this.mProxySettingsSpinner) {
            showProxyFields();
        } else if (adapterView == this.mHiddenSettingsSpinner) {
            TextView textView = this.mHiddenWarningView;
            if (i == 0) {
                i2 = 8;
            } else {
                i2 = 0;
            }
            textView.setVisibility(i2);
            if (i == 1) {
                this.mDialogContainer.post(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mDialogContainer.fullScroll(130);
                    }
                });
            }
        } else {
            showIpConfigFields();
        }
        showWarningMessagesIfAppropriate();
        enableSubmitIfAppropriate();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    public void updatePassword() {
        int i;
        TextView textView = (TextView) this.mView.findViewById(R.id.password);
        if (((CheckBox) this.mView.findViewById(R.id.show_password)).isChecked()) {
            i = 144;
        } else {
            i = 128;
        }
        textView.setInputType(i | 1);
    }

    public AccessPoint getAccessPoint() {
        return this.mAccessPoint;
    }
}
