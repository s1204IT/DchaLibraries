package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.QueuedWork;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthLte;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.PhysicalChannelConfig;
import android.telephony.PreciseCallState;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

public class RadioInfo extends Activity {
    private TextView callState;
    private Button carrierProvisioningButton;
    private Spinner cellInfoRefreshRateSpinner;
    private TextView dBm;
    private TextView dataNetwork;
    private TextView dnsCheckState;
    private Button dnsCheckToggleButton;
    private Switch eabProvisionedSwitch;
    private TextView gprsState;
    private TextView gsmState;
    private Switch imsVolteProvisionedSwitch;
    private Switch imsVtProvisionedSwitch;
    private Switch imsWfcProvisionedSwitch;
    private TextView mCellInfo;
    private int mCellInfoRefreshRateIndex;
    private TextView mCfi;
    private ConnectivityManager mConnectivityManager;
    private TextView mDcRtInfoTv;
    private TextView mDeviceId;
    private TextView mDownlinkKbps;
    private TextView mHttpClientTest;
    private String mHttpClientTestResult;
    private TextView mLocation;
    private TextView mMwi;
    private TextView mNeighboringCids;
    private TextView mPhyChanConfig;
    private String mPingHostnameResultV4;
    private String mPingHostnameResultV6;
    private TextView mPingHostnameV4;
    private TextView mPingHostnameV6;
    private int mPreferredNetworkTypeResult;
    private TextView mSubscriberId;
    private TelephonyManager mTelephonyManager;
    private TextView mUplinkKbps;
    private TextView number;
    private Button oemInfoButton;
    private TextView operatorName;
    private Button pingTestButton;
    private Spinner preferredNetworkType;
    private Switch radioPowerOnSwitch;
    private TextView received;
    private Button refreshSmscButton;
    private TextView roamingState;
    private TextView sent;
    private EditText smsc;
    private Button triggercarrierProvisioningButton;
    private Button updateSmscButton;
    private TextView voiceNetwork;
    private static final String[] mPreferredNetworkLabels = {"WCDMA preferred", "GSM only", "WCDMA only", "GSM auto (PRL)", "CDMA auto (PRL)", "CDMA only", "EvDo only", "Global auto (PRL)", "LTE/CDMA auto (PRL)", "LTE/UMTS auto (PRL)", "LTE/CDMA/UMTS auto (PRL)", "LTE only", "LTE/WCDMA", "TD-SCDMA only", "TD-SCDMA/WCDMA", "LTE/TD-SCDMA", "TD-SCDMA/GSM", "TD-SCDMA/UMTS", "LTE/TD-SCDMA/WCDMA", "LTE/TD-SCDMA/UMTS", "TD-SCDMA/CDMA/UMTS", "Global/TD-SCDMA", "Unknown"};
    private static final String[] mCellInfoRefreshRateLabels = {"Disabled", "Immediate", "Min 5s", "Min 10s", "Min 60s"};
    private static final int[] mCellInfoRefreshRates = {Preference.DEFAULT_ORDER, 0, 5000, 10000, 60000};
    private ImsManager mImsManager = null;
    private Phone phone = null;
    private boolean mMwiValue = false;
    private boolean mCfiValue = false;
    private List<CellInfo> mCellInfoResult = null;
    private CellLocation mCellLocationResult = null;
    private List<NeighboringCellInfo> mNeighboringCellResult = null;
    private final NetworkRequest mDefaultNetworkRequest = new NetworkRequest.Builder().addTransportType(0).addCapability(12).build();
    private final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            RadioInfo.this.updateBandwidths(networkCapabilities.getLinkDownstreamBandwidthKbps(), networkCapabilities.getLinkUpstreamBandwidthKbps());
        }
    };
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDataConnectionStateChanged(int i) {
            RadioInfo.this.updateDataState();
            RadioInfo.this.updateNetworkType();
        }

        @Override
        public void onDataActivity(int i) {
            RadioInfo.this.updateDataStats2();
        }

        @Override
        public void onCallStateChanged(int i, String str) {
            RadioInfo.this.updateNetworkType();
            RadioInfo.this.updatePhoneState(i);
        }

        public void onPreciseCallStateChanged(PreciseCallState preciseCallState) {
            RadioInfo.this.updateNetworkType();
        }

        @Override
        public void onCellLocationChanged(CellLocation cellLocation) {
            RadioInfo.this.updateLocation(cellLocation);
        }

        @Override
        public void onMessageWaitingIndicatorChanged(boolean z) {
            RadioInfo.this.mMwiValue = z;
            RadioInfo.this.updateMessageWaiting();
        }

        @Override
        public void onCallForwardingIndicatorChanged(boolean z) {
            RadioInfo.this.mCfiValue = z;
            RadioInfo.this.updateCallRedirect();
        }

        @Override
        public void onCellInfoChanged(List<CellInfo> list) {
            RadioInfo.this.log("onCellInfoChanged: arrayCi=" + list);
            RadioInfo.this.mCellInfoResult = list;
            RadioInfo.this.updateCellInfo(RadioInfo.this.mCellInfoResult);
        }

        public void onDataConnectionRealTimeInfoChanged(DataConnectionRealTimeInfo dataConnectionRealTimeInfo) {
            RadioInfo.this.log("onDataConnectionRealTimeInfoChanged: dcRtInfo=" + dataConnectionRealTimeInfo);
            RadioInfo.this.updateDcRtInfoTv(dataConnectionRealTimeInfo);
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            RadioInfo.this.log("onSignalStrengthChanged: SignalStrength=" + signalStrength);
            RadioInfo.this.updateSignalStrength(signalStrength);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            RadioInfo.this.log("onServiceStateChanged: ServiceState=" + serviceState);
            RadioInfo.this.updateServiceState(serviceState);
            RadioInfo.this.updateRadioPowerState();
            RadioInfo.this.updateNetworkType();
            RadioInfo.this.updateImsProvisionedState();
        }

        public void onPhysicalChannelConfigurationChanged(List<PhysicalChannelConfig> list) {
            RadioInfo.this.updatePhysicalChannelConfiguration(list);
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1000:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception != null || asyncResult.result == null) {
                        RadioInfo.this.updatePreferredNetworkType(RadioInfo.mPreferredNetworkLabels.length - 1);
                    } else {
                        RadioInfo.this.updatePreferredNetworkType(((int[]) asyncResult.result)[0]);
                    }
                    break;
                case 1001:
                    if (((AsyncResult) message.obj).exception != null) {
                        RadioInfo.this.log("Set preferred network type failed.");
                    }
                    break;
                case 1002:
                case 1003:
                case 1004:
                default:
                    super.handleMessage(message);
                    break;
                case 1005:
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    if (asyncResult2.exception != null) {
                        RadioInfo.this.smsc.setText("refresh error");
                    } else {
                        RadioInfo.this.smsc.setText((String) asyncResult2.result);
                    }
                    break;
                case 1006:
                    RadioInfo.this.updateSmscButton.setEnabled(true);
                    if (((AsyncResult) message.obj).exception != null) {
                        RadioInfo.this.smsc.setText("update error");
                    }
                    break;
            }
        }
    };
    private MenuItem.OnMenuItemClickListener mViewADNCallback = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setClassName("com.android.phone", "com.android.phone.SimContacts");
            RadioInfo.this.startActivity(intent);
            return true;
        }
    };
    private MenuItem.OnMenuItemClickListener mViewFDNCallback = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setClassName("com.android.phone", "com.android.phone.settings.fdn.FdnList");
            RadioInfo.this.startActivity(intent);
            return true;
        }
    };
    private MenuItem.OnMenuItemClickListener mViewSDNCallback = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("content://icc/sdn"));
            intent.setClassName("com.android.phone", "com.android.phone.ADNList");
            RadioInfo.this.startActivity(intent);
            return true;
        }
    };
    private MenuItem.OnMenuItemClickListener mGetImsStatus = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            String string;
            boolean zIsImsRegistered = RadioInfo.this.phone.isImsRegistered();
            boolean zIsVolteEnabled = RadioInfo.this.phone.isVolteEnabled();
            boolean zIsWifiCallingEnabled = RadioInfo.this.phone.isWifiCallingEnabled();
            boolean zIsVideoEnabled = RadioInfo.this.phone.isVideoEnabled();
            boolean zIsUtEnabled = RadioInfo.this.phone.isUtEnabled();
            if (zIsImsRegistered) {
                string = RadioInfo.this.getString(R.string.radio_info_ims_reg_status_registered);
            } else {
                string = RadioInfo.this.getString(R.string.radio_info_ims_reg_status_not_registered);
            }
            String string2 = RadioInfo.this.getString(R.string.radio_info_ims_feature_status_available);
            String string3 = RadioInfo.this.getString(R.string.radio_info_ims_feature_status_unavailable);
            RadioInfo radioInfo = RadioInfo.this;
            Object[] objArr = new Object[5];
            objArr[0] = string;
            objArr[1] = zIsVolteEnabled ? string2 : string3;
            objArr[2] = zIsWifiCallingEnabled ? string2 : string3;
            objArr[3] = zIsVideoEnabled ? string2 : string3;
            if (!zIsUtEnabled) {
                string2 = string3;
            }
            objArr[4] = string2;
            new AlertDialog.Builder(RadioInfo.this).setMessage(radioInfo.getString(R.string.radio_info_ims_reg_status, objArr)).setTitle(RadioInfo.this.getString(R.string.radio_info_ims_reg_status_title)).create().show();
            return true;
        }
    };
    private MenuItem.OnMenuItemClickListener mSelectBandCallback = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            Intent intent = new Intent();
            intent.setClass(RadioInfo.this, BandMode.class);
            RadioInfo.this.startActivity(intent);
            return true;
        }
    };
    private MenuItem.OnMenuItemClickListener mToggleData = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            int dataState = RadioInfo.this.mTelephonyManager.getDataState();
            if (dataState == 0) {
                RadioInfo.this.phone.setUserDataEnabled(true);
            } else if (dataState == 2) {
                RadioInfo.this.phone.setUserDataEnabled(false);
            }
            return true;
        }
    };
    CompoundButton.OnCheckedChangeListener mRadioPowerOnChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            RadioInfo radioInfo = RadioInfo.this;
            StringBuilder sb = new StringBuilder();
            sb.append("toggle radio power: currently ");
            sb.append(RadioInfo.this.isRadioOn() ? "on" : "off");
            radioInfo.log(sb.toString());
            RadioInfo.this.phone.setRadioPower(z);
        }
    };
    CompoundButton.OnCheckedChangeListener mImsVolteCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            RadioInfo.this.setImsVolteProvisionedState(z);
        }
    };
    CompoundButton.OnCheckedChangeListener mImsVtCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            RadioInfo.this.setImsVtProvisionedState(z);
        }
    };
    CompoundButton.OnCheckedChangeListener mImsWfcCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            RadioInfo.this.setImsWfcProvisionedState(z);
        }
    };
    CompoundButton.OnCheckedChangeListener mEabCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            RadioInfo.this.setEabProvisionedState(z);
        }
    };
    View.OnClickListener mDnsCheckButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            RadioInfo.this.phone.disableDnsCheck(!RadioInfo.this.phone.isDnsCheckDisabled());
            RadioInfo.this.updateDnsCheckState();
        }
    };
    View.OnClickListener mOemInfoButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {
                RadioInfo.this.startActivity(new Intent("com.android.settings.OEM_RADIO_INFO"));
            } catch (ActivityNotFoundException e) {
                RadioInfo.this.log("OEM-specific Info/Settings Activity Not Found : " + e);
            }
        }
    };
    View.OnClickListener mPingButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            RadioInfo.this.updatePingState();
        }
    };
    View.OnClickListener mUpdateSmscButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            RadioInfo.this.updateSmscButton.setEnabled(false);
            RadioInfo.this.phone.setSmscAddress(RadioInfo.this.smsc.getText().toString(), RadioInfo.this.mHandler.obtainMessage(1006));
        }
    };
    View.OnClickListener mRefreshSmscButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            RadioInfo.this.refreshSmsc();
        }
    };
    View.OnClickListener mCarrierProvisioningButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent("com.android.settings.CARRIER_PROVISIONING");
            intent.setComponent(ComponentName.unflattenFromString("com.android.omadm.service/.DMIntentReceiver"));
            RadioInfo.this.sendBroadcast(intent);
        }
    };
    View.OnClickListener mTriggerCarrierProvisioningButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent("com.android.settings.TRIGGER_CARRIER_PROVISIONING");
            intent.setComponent(ComponentName.unflattenFromString("com.android.omadm.service/.DMIntentReceiver"));
            RadioInfo.this.sendBroadcast(intent);
        }
    };
    AdapterView.OnItemSelectedListener mPreferredNetworkHandler = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView adapterView, View view, int i, long j) {
            if (RadioInfo.this.mPreferredNetworkTypeResult != i && i >= 0 && i <= RadioInfo.mPreferredNetworkLabels.length - 2) {
                RadioInfo.this.mPreferredNetworkTypeResult = i;
                int subId = RadioInfo.this.phone.getSubId();
                if (SubscriptionManager.isUsableSubIdValue(subId)) {
                    Settings.Global.putInt(RadioInfo.this.phone.getContext().getContentResolver(), "preferred_network_mode" + subId, RadioInfo.this.mPreferredNetworkTypeResult);
                }
                RadioInfo.this.log("Calling setPreferredNetworkType(" + RadioInfo.this.mPreferredNetworkTypeResult + ")");
                RadioInfo.this.phone.setPreferredNetworkType(RadioInfo.this.mPreferredNetworkTypeResult, RadioInfo.this.mHandler.obtainMessage(1001));
            }
        }

        @Override
        public void onNothingSelected(AdapterView adapterView) {
        }
    };
    AdapterView.OnItemSelectedListener mCellInfoRefreshRateHandler = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView adapterView, View view, int i, long j) {
            RadioInfo.this.mCellInfoRefreshRateIndex = i;
            RadioInfo.this.mTelephonyManager.setCellInfoListRate(RadioInfo.mCellInfoRefreshRates[i]);
            RadioInfo.this.updateAllCellInfo();
        }

        @Override
        public void onNothingSelected(AdapterView adapterView) {
        }
    };

    private void log(String str) {
        Log.d("RadioInfo", str);
    }

    private void updatePhysicalChannelConfiguration(List<PhysicalChannelConfig> list) {
        StringBuilder sb = new StringBuilder();
        String str = "";
        sb.append("{");
        if (list != null) {
            for (PhysicalChannelConfig physicalChannelConfig : list) {
                sb.append(str);
                sb.append(physicalChannelConfig);
                str = ",";
            }
        }
        sb.append("}");
        this.mPhyChanConfig.setText(sb.toString());
    }

    private void updatePreferredNetworkType(int i) {
        if (i >= mPreferredNetworkLabels.length || i < 0) {
            log("EVENT_QUERY_PREFERRED_TYPE_DONE: unknown type=" + i);
            i = mPreferredNetworkLabels.length - 1;
        }
        this.mPreferredNetworkTypeResult = i;
        this.preferredNetworkType.setSelection(this.mPreferredNetworkTypeResult, true);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (!Process.myUserHandle().isSystem()) {
            Log.e("RadioInfo", "Not run from system user, don't do anything.");
            finish();
            return;
        }
        setContentView(R.layout.radio_info);
        log("Started onCreate");
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        this.mConnectivityManager = (ConnectivityManager) getSystemService("connectivity");
        this.phone = PhoneFactory.getDefaultPhone();
        this.mImsManager = ImsManager.getInstance(getApplicationContext(), SubscriptionManager.getDefaultVoicePhoneId());
        this.mDeviceId = (TextView) findViewById(R.id.imei);
        this.number = (TextView) findViewById(R.id.number);
        this.mSubscriberId = (TextView) findViewById(R.id.imsi);
        this.callState = (TextView) findViewById(R.id.call);
        this.operatorName = (TextView) findViewById(R.id.operator);
        this.roamingState = (TextView) findViewById(R.id.roaming);
        this.gsmState = (TextView) findViewById(R.id.gsm);
        this.gprsState = (TextView) findViewById(R.id.gprs);
        this.voiceNetwork = (TextView) findViewById(R.id.voice_network);
        this.dataNetwork = (TextView) findViewById(R.id.data_network);
        this.dBm = (TextView) findViewById(R.id.dbm);
        this.mMwi = (TextView) findViewById(R.id.mwi);
        this.mCfi = (TextView) findViewById(R.id.cfi);
        this.mLocation = (TextView) findViewById(R.id.location);
        this.mNeighboringCids = (TextView) findViewById(R.id.neighboring);
        this.mCellInfo = (TextView) findViewById(R.id.cellinfo);
        this.mCellInfo.setTypeface(Typeface.MONOSPACE);
        this.mDcRtInfoTv = (TextView) findViewById(R.id.dcrtinfo);
        this.sent = (TextView) findViewById(R.id.sent);
        this.received = (TextView) findViewById(R.id.received);
        this.smsc = (EditText) findViewById(R.id.smsc);
        this.dnsCheckState = (TextView) findViewById(R.id.dnsCheckState);
        this.mPingHostnameV4 = (TextView) findViewById(R.id.pingHostnameV4);
        this.mPingHostnameV6 = (TextView) findViewById(R.id.pingHostnameV6);
        this.mHttpClientTest = (TextView) findViewById(R.id.httpClientTest);
        this.mPhyChanConfig = (TextView) findViewById(R.id.phy_chan_config);
        this.preferredNetworkType = (Spinner) findViewById(R.id.preferredNetworkType);
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, mPreferredNetworkLabels);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.preferredNetworkType.setAdapter((SpinnerAdapter) arrayAdapter);
        this.cellInfoRefreshRateSpinner = (Spinner) findViewById(R.id.cell_info_rate_select);
        ArrayAdapter arrayAdapter2 = new ArrayAdapter(this, android.R.layout.simple_spinner_item, mCellInfoRefreshRateLabels);
        arrayAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.cellInfoRefreshRateSpinner.setAdapter((SpinnerAdapter) arrayAdapter2);
        this.imsVolteProvisionedSwitch = (Switch) findViewById(R.id.volte_provisioned_switch);
        this.imsVtProvisionedSwitch = (Switch) findViewById(R.id.vt_provisioned_switch);
        this.imsWfcProvisionedSwitch = (Switch) findViewById(R.id.wfc_provisioned_switch);
        this.eabProvisionedSwitch = (Switch) findViewById(R.id.eab_provisioned_switch);
        this.radioPowerOnSwitch = (Switch) findViewById(R.id.radio_power);
        this.mDownlinkKbps = (TextView) findViewById(R.id.dl_kbps);
        this.mUplinkKbps = (TextView) findViewById(R.id.ul_kbps);
        updateBandwidths(0, 0);
        this.pingTestButton = (Button) findViewById(R.id.ping_test);
        this.pingTestButton.setOnClickListener(this.mPingButtonHandler);
        this.updateSmscButton = (Button) findViewById(R.id.update_smsc);
        this.updateSmscButton.setOnClickListener(this.mUpdateSmscButtonHandler);
        this.refreshSmscButton = (Button) findViewById(R.id.refresh_smsc);
        this.refreshSmscButton.setOnClickListener(this.mRefreshSmscButtonHandler);
        this.dnsCheckToggleButton = (Button) findViewById(R.id.dns_check_toggle);
        this.dnsCheckToggleButton.setOnClickListener(this.mDnsCheckButtonHandler);
        this.carrierProvisioningButton = (Button) findViewById(R.id.carrier_provisioning);
        this.carrierProvisioningButton.setOnClickListener(this.mCarrierProvisioningButtonHandler);
        this.triggercarrierProvisioningButton = (Button) findViewById(R.id.trigger_carrier_provisioning);
        this.triggercarrierProvisioningButton.setOnClickListener(this.mTriggerCarrierProvisioningButtonHandler);
        this.oemInfoButton = (Button) findViewById(R.id.oem_info);
        this.oemInfoButton.setOnClickListener(this.mOemInfoButtonHandler);
        if (getPackageManager().queryIntentActivities(new Intent("com.android.settings.OEM_RADIO_INFO"), 0).size() == 0) {
            this.oemInfoButton.setEnabled(false);
        }
        this.mCellInfoRefreshRateIndex = 0;
        this.mPreferredNetworkTypeResult = mPreferredNetworkLabels.length - 1;
        this.phone.getPreferredNetworkType(this.mHandler.obtainMessage(1000));
        restoreFromBundle(bundle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("Started onResume");
        updateMessageWaiting();
        updateCallRedirect();
        updateDataState();
        updateDataStats2();
        updateRadioPowerState();
        updateImsProvisionedState();
        updateProperties();
        updateDnsCheckState();
        updateNetworkType();
        updateNeighboringCids(this.mNeighboringCellResult);
        updateLocation(this.mCellLocationResult);
        updateCellInfo(this.mCellInfoResult);
        this.mPingHostnameV4.setText(this.mPingHostnameResultV4);
        this.mPingHostnameV6.setText(this.mPingHostnameResultV6);
        this.mHttpClientTest.setText(this.mHttpClientTestResult);
        this.cellInfoRefreshRateSpinner.setOnItemSelectedListener(this.mCellInfoRefreshRateHandler);
        this.cellInfoRefreshRateSpinner.setSelection(this.mCellInfoRefreshRateIndex);
        this.preferredNetworkType.setSelection(this.mPreferredNetworkTypeResult, true);
        this.preferredNetworkType.setOnItemSelectedListener(this.mPreferredNetworkHandler);
        this.radioPowerOnSwitch.setOnCheckedChangeListener(this.mRadioPowerOnChangeListener);
        this.imsVolteProvisionedSwitch.setOnCheckedChangeListener(this.mImsVolteCheckedChangeListener);
        this.imsVtProvisionedSwitch.setOnCheckedChangeListener(this.mImsVtCheckedChangeListener);
        this.imsWfcProvisionedSwitch.setOnCheckedChangeListener(this.mImsWfcCheckedChangeListener);
        this.eabProvisionedSwitch.setOnCheckedChangeListener(this.mEabCheckedChangeListener);
        this.mTelephonyManager.listen(this.mPhoneStateListener, 1050109);
        this.mConnectivityManager.registerNetworkCallback(this.mDefaultNetworkRequest, this.mNetworkCallback, this.mHandler);
        this.smsc.clearFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause: unregister phone & data intents");
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        this.mTelephonyManager.setCellInfoListRate(Preference.DEFAULT_ORDER);
        this.mConnectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
    }

    private void restoreFromBundle(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        this.mPingHostnameResultV4 = bundle.getString("mPingHostnameResultV4", "");
        this.mPingHostnameResultV6 = bundle.getString("mPingHostnameResultV6", "");
        this.mHttpClientTestResult = bundle.getString("mHttpClientTestResult", "");
        this.mPingHostnameV4.setText(this.mPingHostnameResultV4);
        this.mPingHostnameV6.setText(this.mPingHostnameResultV6);
        this.mHttpClientTest.setText(this.mHttpClientTestResult);
        this.mPreferredNetworkTypeResult = bundle.getInt("mPreferredNetworkTypeResult", mPreferredNetworkLabels.length - 1);
        this.mCellInfoRefreshRateIndex = bundle.getInt("mCellInfoRefreshRateIndex", 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putString("mPingHostnameResultV4", this.mPingHostnameResultV4);
        bundle.putString("mPingHostnameResultV6", this.mPingHostnameResultV6);
        bundle.putString("mHttpClientTestResult", this.mHttpClientTestResult);
        bundle.putInt("mPreferredNetworkTypeResult", this.mPreferredNetworkTypeResult);
        bundle.putInt("mCellInfoRefreshRateIndex", this.mCellInfoRefreshRateIndex);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.radio_info_band_mode_label).setOnMenuItemClickListener(this.mSelectBandCallback).setAlphabeticShortcut('b');
        menu.add(1, 1, 0, R.string.radioInfo_menu_viewADN).setOnMenuItemClickListener(this.mViewADNCallback);
        menu.add(1, 2, 0, R.string.radioInfo_menu_viewFDN).setOnMenuItemClickListener(this.mViewFDNCallback);
        menu.add(1, 3, 0, R.string.radioInfo_menu_viewSDN).setOnMenuItemClickListener(this.mViewSDNCallback);
        menu.add(1, 4, 0, R.string.radioInfo_menu_getIMS).setOnMenuItemClickListener(this.mGetImsStatus);
        menu.add(1, 5, 0, R.string.radio_info_data_connection_disable).setOnMenuItemClickListener(this.mToggleData);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean z;
        MenuItem menuItemFindItem = menu.findItem(5);
        int dataState = this.mTelephonyManager.getDataState();
        if (dataState != 0) {
            switch (dataState) {
                case 2:
                case 3:
                    menuItemFindItem.setTitle(R.string.radio_info_data_connection_disable);
                    break;
                default:
                    z = false;
                    break;
            }
            menuItemFindItem.setVisible(z);
            return true;
        }
        menuItemFindItem.setTitle(R.string.radio_info_data_connection_enable);
        z = true;
        menuItemFindItem.setVisible(z);
        return true;
    }

    private void updateDnsCheckState() {
        this.dnsCheckState.setText(this.phone.isDnsCheckDisabled() ? "0.0.0.0 allowed" : "0.0.0.0 not allowed");
    }

    private void updateBandwidths(int i, int i2) {
        if (i < 0 || i == Integer.MAX_VALUE) {
            i = -1;
        }
        if (i2 < 0 || i2 == Integer.MAX_VALUE) {
            i2 = -1;
        }
        this.mDownlinkKbps.setText(String.format("%-5d", Integer.valueOf(i)));
        this.mUplinkKbps.setText(String.format("%-5d", Integer.valueOf(i2)));
    }

    private final void updateSignalStrength(SignalStrength signalStrength) {
        Resources resources = getResources();
        int dbm = signalStrength.getDbm();
        int asuLevel = signalStrength.getAsuLevel();
        if (-1 == asuLevel) {
            asuLevel = 0;
        }
        this.dBm.setText(String.valueOf(dbm) + " " + resources.getString(R.string.radioInfo_display_dbm) + "   " + String.valueOf(asuLevel) + " " + resources.getString(R.string.radioInfo_display_asu));
    }

    private final void updateLocation(CellLocation cellLocation) {
        Resources resources = getResources();
        if (cellLocation instanceof GsmCellLocation) {
            GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
            int lac = gsmCellLocation.getLac();
            int cid = gsmCellLocation.getCid();
            TextView textView = this.mLocation;
            StringBuilder sb = new StringBuilder();
            sb.append(resources.getString(R.string.radioInfo_lac));
            sb.append(" = ");
            sb.append(lac == -1 ? "unknown" : Integer.toHexString(lac));
            sb.append("   ");
            sb.append(resources.getString(R.string.radioInfo_cid));
            sb.append(" = ");
            sb.append(cid == -1 ? "unknown" : Integer.toHexString(cid));
            textView.setText(sb.toString());
            return;
        }
        if (cellLocation instanceof CdmaCellLocation) {
            CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
            int baseStationId = cdmaCellLocation.getBaseStationId();
            int systemId = cdmaCellLocation.getSystemId();
            int networkId = cdmaCellLocation.getNetworkId();
            int baseStationLatitude = cdmaCellLocation.getBaseStationLatitude();
            int baseStationLongitude = cdmaCellLocation.getBaseStationLongitude();
            TextView textView2 = this.mLocation;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("BID = ");
            sb2.append(baseStationId == -1 ? "unknown" : Integer.toHexString(baseStationId));
            sb2.append("   SID = ");
            sb2.append(systemId == -1 ? "unknown" : Integer.toHexString(systemId));
            sb2.append("   NID = ");
            sb2.append(networkId == -1 ? "unknown" : Integer.toHexString(networkId));
            sb2.append("\nLAT = ");
            sb2.append(baseStationLatitude == -1 ? "unknown" : Integer.toHexString(baseStationLatitude));
            sb2.append("   LONG = ");
            sb2.append(baseStationLongitude == -1 ? "unknown" : Integer.toHexString(baseStationLongitude));
            textView2.setText(sb2.toString());
            return;
        }
        this.mLocation.setText("unknown");
    }

    private final void updateNeighboringCids(List<NeighboringCellInfo> list) {
        StringBuilder sb = new StringBuilder();
        if (list != null) {
            if (list.isEmpty()) {
                sb.append("no neighboring cells");
            } else {
                Iterator<NeighboringCellInfo> it = list.iterator();
                while (it.hasNext()) {
                    sb.append(it.next().toString());
                    sb.append(" ");
                }
            }
        } else {
            sb.append("unknown");
        }
        this.mNeighboringCids.setText(sb.toString());
    }

    private final String getCellInfoDisplayString(int i) {
        return i != Integer.MAX_VALUE ? Integer.toString(i) : "";
    }

    private final String getConnectionStatusString(CellInfo cellInfo) {
        String str = "";
        String str2 = "";
        String str3 = "";
        if (cellInfo.isRegistered()) {
            str = "R";
        }
        int cellConnectionStatus = cellInfo.getCellConnectionStatus();
        if (cellConnectionStatus != Integer.MAX_VALUE) {
            switch (cellConnectionStatus) {
                case 0:
                    str2 = "N";
                    break;
                case 1:
                    str2 = "P";
                    break;
                case 2:
                    str2 = "S";
                    break;
            }
        }
        if (!TextUtils.isEmpty(str) && !TextUtils.isEmpty(str2)) {
            str3 = "+";
        }
        return str + str3 + str2;
    }

    private final String buildCdmaInfoString(CellInfoCdma cellInfoCdma) {
        CellIdentityCdma cellIdentity = cellInfoCdma.getCellIdentity();
        CellSignalStrengthCdma cellSignalStrength = cellInfoCdma.getCellSignalStrength();
        return String.format("%-3.3s %-5.5s %-5.5s %-5.5s %-6.6s %-6.6s %-6.6s %-6.6s %-5.5s", getConnectionStatusString(cellInfoCdma), getCellInfoDisplayString(cellIdentity.getSystemId()), getCellInfoDisplayString(cellIdentity.getNetworkId()), getCellInfoDisplayString(cellIdentity.getBasestationId()), getCellInfoDisplayString(cellSignalStrength.getCdmaDbm()), getCellInfoDisplayString(cellSignalStrength.getCdmaEcio()), getCellInfoDisplayString(cellSignalStrength.getEvdoDbm()), getCellInfoDisplayString(cellSignalStrength.getEvdoEcio()), getCellInfoDisplayString(cellSignalStrength.getEvdoSnr()));
    }

    private final String buildGsmInfoString(CellInfoGsm cellInfoGsm) {
        CellIdentityGsm cellIdentity = cellInfoGsm.getCellIdentity();
        return String.format("%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-6.6s %-4.4s %-4.4s\n", getConnectionStatusString(cellInfoGsm), getCellInfoDisplayString(cellIdentity.getMcc()), getCellInfoDisplayString(cellIdentity.getMnc()), getCellInfoDisplayString(cellIdentity.getLac()), getCellInfoDisplayString(cellIdentity.getCid()), getCellInfoDisplayString(cellIdentity.getArfcn()), getCellInfoDisplayString(cellIdentity.getBsic()), getCellInfoDisplayString(cellInfoGsm.getCellSignalStrength().getDbm()));
    }

    private final String buildLteInfoString(CellInfoLte cellInfoLte) {
        CellIdentityLte cellIdentity = cellInfoLte.getCellIdentity();
        CellSignalStrengthLte cellSignalStrength = cellInfoLte.getCellSignalStrength();
        return String.format("%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-3.3s %-6.6s %-2.2s %-4.4s %-4.4s %-2.2s\n", getConnectionStatusString(cellInfoLte), getCellInfoDisplayString(cellIdentity.getMcc()), getCellInfoDisplayString(cellIdentity.getMnc()), getCellInfoDisplayString(cellIdentity.getTac()), getCellInfoDisplayString(cellIdentity.getCi()), getCellInfoDisplayString(cellIdentity.getPci()), getCellInfoDisplayString(cellIdentity.getEarfcn()), getCellInfoDisplayString(cellIdentity.getBandwidth()), getCellInfoDisplayString(cellSignalStrength.getDbm()), getCellInfoDisplayString(cellSignalStrength.getRsrq()), getCellInfoDisplayString(cellSignalStrength.getTimingAdvance()));
    }

    private final String buildWcdmaInfoString(CellInfoWcdma cellInfoWcdma) {
        CellIdentityWcdma cellIdentity = cellInfoWcdma.getCellIdentity();
        return String.format("%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-6.6s %-3.3s %-4.4s\n", getConnectionStatusString(cellInfoWcdma), getCellInfoDisplayString(cellIdentity.getMcc()), getCellInfoDisplayString(cellIdentity.getMnc()), getCellInfoDisplayString(cellIdentity.getLac()), getCellInfoDisplayString(cellIdentity.getCid()), getCellInfoDisplayString(cellIdentity.getUarfcn()), getCellInfoDisplayString(cellIdentity.getPsc()), getCellInfoDisplayString(cellInfoWcdma.getCellSignalStrength().getDbm()));
    }

    private final String buildCellInfoString(List<CellInfo> list) {
        String str = new String();
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        StringBuilder sb3 = new StringBuilder();
        StringBuilder sb4 = new StringBuilder();
        if (list != null) {
            for (CellInfo cellInfo : list) {
                if (cellInfo instanceof CellInfoLte) {
                    sb3.append(buildLteInfoString((CellInfoLte) cellInfo));
                } else if (cellInfo instanceof CellInfoWcdma) {
                    sb4.append(buildWcdmaInfoString((CellInfoWcdma) cellInfo));
                } else if (cellInfo instanceof CellInfoGsm) {
                    sb2.append(buildGsmInfoString((CellInfoGsm) cellInfo));
                } else if (cellInfo instanceof CellInfoCdma) {
                    sb.append(buildCdmaInfoString((CellInfoCdma) cellInfo));
                }
            }
            if (sb3.length() != 0) {
                str = (str + String.format("LTE\n%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-3.3s %-6.6s %-2.2s %-4.4s %-4.4s %-2.2s\n", "SRV", "MCC", "MNC", "TAC", "CID", "PCI", "EARFCN", "BW", "RSRP", "RSRQ", "TA")) + sb3.toString();
            }
            if (sb4.length() != 0) {
                str = (str + String.format("WCDMA\n%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-6.6s %-3.3s %-4.4s\n", "SRV", "MCC", "MNC", "LAC", "CID", "UARFCN", "PSC", "RSCP")) + sb4.toString();
            }
            if (sb2.length() != 0) {
                str = (str + String.format("GSM\n%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-6.6s %-4.4s %-4.4s\n", "SRV", "MCC", "MNC", "LAC", "CID", "ARFCN", "BSIC", "RSSI")) + sb2.toString();
            }
            if (sb.length() != 0) {
                str = (str + String.format("CDMA/EVDO\n%-3.3s %-5.5s %-5.5s %-5.5s %-6.6s %-6.6s %-6.6s %-6.6s %-5.5s\n", "SRV", "SID", "NID", "BSID", "C-RSSI", "C-ECIO", "E-RSSI", "E-ECIO", "E-SNR")) + sb.toString();
            }
        } else {
            str = "unknown";
        }
        return str.toString();
    }

    private final void updateCellInfo(List<CellInfo> list) {
        this.mCellInfo.setText(buildCellInfoString(list));
    }

    private final void updateDcRtInfoTv(DataConnectionRealTimeInfo dataConnectionRealTimeInfo) {
        this.mDcRtInfoTv.setText(dataConnectionRealTimeInfo.toString());
    }

    private final void updateMessageWaiting() {
        this.mMwi.setText(String.valueOf(this.mMwiValue));
    }

    private final void updateCallRedirect() {
        this.mCfi.setText(String.valueOf(this.mCfiValue));
    }

    private final void updateServiceState(ServiceState serviceState) {
        int state = serviceState.getState();
        Resources resources = getResources();
        String string = resources.getString(R.string.radioInfo_unknown);
        switch (state) {
            case 0:
                string = resources.getString(R.string.radioInfo_service_in);
                break;
            case 1:
            case 2:
                string = resources.getString(R.string.radioInfo_service_emergency);
                break;
            case 3:
                string = resources.getString(R.string.radioInfo_service_off);
                break;
        }
        this.gsmState.setText(string);
        if (serviceState.getRoaming()) {
            this.roamingState.setText(R.string.radioInfo_roaming_in);
        } else {
            this.roamingState.setText(R.string.radioInfo_roaming_not);
        }
        this.operatorName.setText(serviceState.getOperatorAlphaLong());
    }

    private final void updatePhoneState(int i) {
        Resources resources = getResources();
        String string = resources.getString(R.string.radioInfo_unknown);
        switch (i) {
            case 0:
                string = resources.getString(R.string.radioInfo_phone_idle);
                break;
            case 1:
                string = resources.getString(R.string.radioInfo_phone_ringing);
                break;
            case 2:
                string = resources.getString(R.string.radioInfo_phone_offhook);
                break;
        }
        this.callState.setText(string);
    }

    private final void updateDataState() {
        int dataState = this.mTelephonyManager.getDataState();
        Resources resources = getResources();
        String string = resources.getString(R.string.radioInfo_unknown);
        switch (dataState) {
            case 0:
                string = resources.getString(R.string.radioInfo_data_disconnected);
                break;
            case 1:
                string = resources.getString(R.string.radioInfo_data_connecting);
                break;
            case 2:
                string = resources.getString(R.string.radioInfo_data_connected);
                break;
            case 3:
                string = resources.getString(R.string.radioInfo_data_suspended);
                break;
        }
        this.gprsState.setText(string);
    }

    private final void updateNetworkType() {
        if (this.phone != null) {
            this.phone.getServiceState();
            this.dataNetwork.setText(ServiceState.rilRadioTechnologyToString(this.phone.getServiceState().getRilDataRadioTechnology()));
            this.voiceNetwork.setText(ServiceState.rilRadioTechnologyToString(this.phone.getServiceState().getRilVoiceRadioTechnology()));
        }
    }

    private final void updateProperties() {
        Resources resources = getResources();
        String deviceId = this.phone.getDeviceId();
        if (deviceId == null) {
            deviceId = resources.getString(R.string.radioInfo_unknown);
        }
        this.mDeviceId.setText(deviceId);
        String subscriberId = this.phone.getSubscriberId();
        if (subscriberId == null) {
            subscriberId = resources.getString(R.string.radioInfo_unknown);
        }
        this.mSubscriberId.setText(subscriberId);
        String line1Number = this.phone.getLine1Number();
        if (line1Number == null) {
            line1Number = resources.getString(R.string.radioInfo_unknown);
        }
        this.number.setText(line1Number);
    }

    private final void updateDataStats2() {
        Resources resources = getResources();
        long mobileTxPackets = TrafficStats.getMobileTxPackets();
        long mobileRxPackets = TrafficStats.getMobileRxPackets();
        long mobileTxBytes = TrafficStats.getMobileTxBytes();
        long mobileRxBytes = TrafficStats.getMobileRxBytes();
        String string = resources.getString(R.string.radioInfo_display_packets);
        String string2 = resources.getString(R.string.radioInfo_display_bytes);
        this.sent.setText(mobileTxPackets + " " + string + ", " + mobileTxBytes + " " + string2);
        this.received.setText(mobileRxPackets + " " + string + ", " + mobileRxBytes + " " + string2);
    }

    private final void pingHostname() {
        try {
            try {
                int iWaitFor = Runtime.getRuntime().exec("ping -c 1 www.google.com").waitFor();
                if (iWaitFor == 0) {
                    this.mPingHostnameResultV4 = "Pass";
                } else {
                    this.mPingHostnameResultV4 = String.format("Fail(%d)", Integer.valueOf(iWaitFor));
                }
            } catch (IOException e) {
                this.mPingHostnameResultV4 = "Fail: IOException";
            }
            try {
                int iWaitFor2 = Runtime.getRuntime().exec("ping6 -c 1 www.google.com").waitFor();
                if (iWaitFor2 == 0) {
                    this.mPingHostnameResultV6 = "Pass";
                } else {
                    this.mPingHostnameResultV6 = String.format("Fail(%d)", Integer.valueOf(iWaitFor2));
                }
            } catch (IOException e2) {
                this.mPingHostnameResultV6 = "Fail: IOException";
            }
        } catch (InterruptedException e3) {
            this.mPingHostnameResultV6 = "Fail: InterruptedException";
            this.mPingHostnameResultV4 = "Fail: InterruptedException";
        }
    }

    private void httpClientTest() throws Throwable {
        HttpURLConnection httpURLConnection;
        Throwable th;
        HttpURLConnection httpURLConnection2 = null;
        try {
            try {
                httpURLConnection = (HttpURLConnection) new URL("https://www.google.com").openConnection();
            } catch (IOException e) {
            }
        } catch (Throwable th2) {
            httpURLConnection = httpURLConnection2;
            th = th2;
        }
        try {
            if (httpURLConnection.getResponseCode() == 200) {
                this.mHttpClientTestResult = "Pass";
            } else {
                this.mHttpClientTestResult = "Fail: Code: " + httpURLConnection.getResponseMessage();
            }
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        } catch (IOException e2) {
            httpURLConnection2 = httpURLConnection;
            this.mHttpClientTestResult = "Fail: IOException";
            if (httpURLConnection2 != null) {
                httpURLConnection2.disconnect();
            }
        } catch (Throwable th3) {
            th = th3;
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            throw th;
        }
    }

    private void refreshSmsc() {
        this.phone.getSmscAddress(this.mHandler.obtainMessage(1005));
    }

    private final void updateAllCellInfo() {
        this.mCellInfo.setText("");
        this.mNeighboringCids.setText("");
        this.mLocation.setText("");
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                RadioInfo.this.updateNeighboringCids(RadioInfo.this.mNeighboringCellResult);
                RadioInfo.this.updateLocation(RadioInfo.this.mCellLocationResult);
                RadioInfo.this.updateCellInfo(RadioInfo.this.mCellInfoResult);
            }
        };
        new Thread() {
            @Override
            public void run() {
                RadioInfo.this.mCellInfoResult = RadioInfo.this.mTelephonyManager.getAllCellInfo();
                RadioInfo.this.mCellLocationResult = RadioInfo.this.mTelephonyManager.getCellLocation();
                RadioInfo.this.mNeighboringCellResult = RadioInfo.this.mTelephonyManager.getNeighboringCellInfo();
                RadioInfo.this.mHandler.post(runnable);
            }
        }.start();
    }

    private final void updatePingState() {
        this.mPingHostnameResultV4 = getResources().getString(R.string.radioInfo_unknown);
        this.mPingHostnameResultV6 = getResources().getString(R.string.radioInfo_unknown);
        this.mHttpClientTestResult = getResources().getString(R.string.radioInfo_unknown);
        this.mPingHostnameV4.setText(this.mPingHostnameResultV4);
        this.mPingHostnameV6.setText(this.mPingHostnameResultV6);
        this.mHttpClientTest.setText(this.mHttpClientTestResult);
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                RadioInfo.this.mPingHostnameV4.setText(RadioInfo.this.mPingHostnameResultV4);
                RadioInfo.this.mPingHostnameV6.setText(RadioInfo.this.mPingHostnameResultV6);
                RadioInfo.this.mHttpClientTest.setText(RadioInfo.this.mHttpClientTestResult);
            }
        };
        new Thread() {
            @Override
            public void run() {
                RadioInfo.this.pingHostname();
                RadioInfo.this.mHandler.post(runnable);
            }
        }.start();
        new Thread() {
            @Override
            public void run() throws Throwable {
                RadioInfo.this.httpClientTest();
                RadioInfo.this.mHandler.post(runnable);
            }
        }.start();
    }

    private boolean isRadioOn() {
        return this.phone.getServiceState().getState() != 3;
    }

    private void updateRadioPowerState() {
        this.radioPowerOnSwitch.setOnCheckedChangeListener(null);
        this.radioPowerOnSwitch.setChecked(isRadioOn());
        this.radioPowerOnSwitch.setOnCheckedChangeListener(this.mRadioPowerOnChangeListener);
    }

    void setImsVolteProvisionedState(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("setImsVolteProvisioned state: ");
        sb.append(z ? "on" : "off");
        Log.d("RadioInfo", sb.toString());
        setImsConfigProvisionedState(10, z);
    }

    void setImsVtProvisionedState(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("setImsVtProvisioned() state: ");
        sb.append(z ? "on" : "off");
        Log.d("RadioInfo", sb.toString());
        setImsConfigProvisionedState(11, z);
    }

    void setImsWfcProvisionedState(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("setImsWfcProvisioned() state: ");
        sb.append(z ? "on" : "off");
        Log.d("RadioInfo", sb.toString());
        setImsConfigProvisionedState(28, z);
    }

    void setEabProvisionedState(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("setEabProvisioned() state: ");
        sb.append(z ? "on" : "off");
        Log.d("RadioInfo", sb.toString());
        setImsConfigProvisionedState(25, z);
    }

    void setImsConfigProvisionedState(final int i, final boolean z) {
        if (this.phone != null && this.mImsManager != null) {
            QueuedWork.queue(new Runnable() {
                @Override
                public void run() {
                    try {
                        RadioInfo.this.mImsManager.getConfigInterface().setProvisionedValue(i, z ? 1 : 0);
                    } catch (ImsException e) {
                        Log.e("RadioInfo", "setImsConfigProvisioned() exception:", e);
                    }
                }
            }, false);
        }
    }

    private boolean isImsVolteProvisioned() {
        if (this.phone == null || this.mImsManager == null) {
            return false;
        }
        ImsManager imsManager = this.mImsManager;
        if (!ImsManager.isVolteEnabledByPlatform(this.phone.getContext())) {
            return false;
        }
        ImsManager imsManager2 = this.mImsManager;
        return ImsManager.isVolteProvisionedOnDevice(this.phone.getContext());
    }

    private boolean isImsVtProvisioned() {
        if (this.phone == null || this.mImsManager == null) {
            return false;
        }
        ImsManager imsManager = this.mImsManager;
        if (!ImsManager.isVtEnabledByPlatform(this.phone.getContext())) {
            return false;
        }
        ImsManager imsManager2 = this.mImsManager;
        return ImsManager.isVtProvisionedOnDevice(this.phone.getContext());
    }

    private boolean isImsWfcProvisioned() {
        if (this.phone == null || this.mImsManager == null) {
            return false;
        }
        ImsManager imsManager = this.mImsManager;
        if (!ImsManager.isWfcEnabledByPlatform(this.phone.getContext())) {
            return false;
        }
        ImsManager imsManager2 = this.mImsManager;
        return ImsManager.isWfcProvisionedOnDevice(this.phone.getContext());
    }

    private boolean isEabProvisioned() {
        return isFeatureProvisioned(25, false);
    }

    private boolean isFeatureProvisioned(int i, boolean z) {
        if (this.mImsManager != null) {
            try {
                ImsConfig configInterface = this.mImsManager.getConfigInterface();
                if (configInterface != null) {
                    z = true;
                    if (configInterface.getProvisionedValue(i) != 1) {
                        z = false;
                    }
                }
            } catch (ImsException e) {
                Log.e("RadioInfo", "isFeatureProvisioned() exception:", e);
            }
        }
        log("isFeatureProvisioned() featureId=" + i + " provisioned=" + z);
        return z;
    }

    private static boolean isEabEnabledByPlatform(Context context) {
        CarrierConfigManager carrierConfigManager;
        if (context != null && (carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config")) != null && carrierConfigManager.getConfig().getBoolean("use_rcs_presence_bool")) {
            return true;
        }
        return false;
    }

    private void updateImsProvisionedState() {
        log("updateImsProvisionedState isImsVolteProvisioned()=" + isImsVolteProvisioned());
        this.imsVolteProvisionedSwitch.setOnCheckedChangeListener(null);
        this.imsVolteProvisionedSwitch.setChecked(isImsVolteProvisioned());
        this.imsVolteProvisionedSwitch.setOnCheckedChangeListener(this.mImsVolteCheckedChangeListener);
        Switch r0 = this.imsVolteProvisionedSwitch;
        ImsManager imsManager = this.mImsManager;
        r0.setEnabled(ImsManager.isVolteEnabledByPlatform(this.phone.getContext()));
        this.imsVtProvisionedSwitch.setOnCheckedChangeListener(null);
        this.imsVtProvisionedSwitch.setChecked(isImsVtProvisioned());
        this.imsVtProvisionedSwitch.setOnCheckedChangeListener(this.mImsVtCheckedChangeListener);
        Switch r02 = this.imsVtProvisionedSwitch;
        ImsManager imsManager2 = this.mImsManager;
        r02.setEnabled(ImsManager.isVtEnabledByPlatform(this.phone.getContext()));
        this.imsWfcProvisionedSwitch.setOnCheckedChangeListener(null);
        this.imsWfcProvisionedSwitch.setChecked(isImsWfcProvisioned());
        this.imsWfcProvisionedSwitch.setOnCheckedChangeListener(this.mImsWfcCheckedChangeListener);
        Switch r03 = this.imsWfcProvisionedSwitch;
        ImsManager imsManager3 = this.mImsManager;
        r03.setEnabled(ImsManager.isWfcEnabledByPlatform(this.phone.getContext()));
        this.eabProvisionedSwitch.setOnCheckedChangeListener(null);
        this.eabProvisionedSwitch.setChecked(isEabProvisioned());
        this.eabProvisionedSwitch.setOnCheckedChangeListener(this.mEabCheckedChangeListener);
        this.eabProvisionedSwitch.setEnabled(isEabEnabledByPlatform(this.phone.getContext()));
    }
}
