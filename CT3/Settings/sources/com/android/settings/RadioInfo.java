package com.android.settings;

import android.app.Activity;
import android.app.QueuedWork;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseCallState;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
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
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class RadioInfo extends Activity {
    private TextView callState;
    private Spinner cellInfoRefreshRateSpinner;
    private TextView dBm;
    private TextView dataNetwork;
    private TextView dnsCheckState;
    private Button dnsCheckToggleButton;
    private TextView gprsState;
    private TextView gsmState;
    private Switch imsVoLteProvisionedSwitch;
    private TextView mCellInfo;
    private int mCellInfoRefreshRateIndex;
    private TextView mCfi;
    private TextView mDcRtInfoTv;
    private TextView mDeviceId;
    private TextView mHttpClientTest;
    private String mHttpClientTestResult;
    private TextView mLocation;
    private TextView mMwi;
    private TextView mNeighboringCids;
    private String mPingHostnameResultV4;
    private String mPingHostnameResultV6;
    private TextView mPingHostnameV4;
    private TextView mPingHostnameV6;
    private int mPreferredNetworkTypeResult;
    private TelephonyManager mTelephonyManager;
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
    private Button updateSmscButton;
    private TextView voiceNetwork;
    private static final String[] mPreferredNetworkLabels = {"WCDMA preferred", "GSM only", "WCDMA only", "GSM auto (PRL)", "CDMA auto (PRL)", "CDMA only", "EvDo only", "Global auto (PRL)", "LTE/CDMA auto (PRL)", "LTE/UMTS auto (PRL)", "LTE/CDMA/UMTS auto (PRL)", "LTE only", "LTE/WCDMA", "TD-SCDMA only", "TD-SCDMA/WCDMA", "LTE/TD-SCDMA", "TD-SCDMA/GSM", "TD-SCDMA/UMTS", "LTE/TD-SCDMA/WCDMA", "LTE/TD-SCDMA/UMTS", "TD-SCDMA/CDMA/UMTS", "Global/TD-SCDMA", "Unknown"};
    private static final String[] mCellInfoRefreshRateLabels = {"Disabled", "Immediate", "Min 5s", "Min 10s", "Min 60s"};
    private static final int[] mCellInfoRefreshRates = {Integer.MAX_VALUE, 0, 5000, 10000, 60000};
    private ImsManager mImsManager = null;
    private Phone phone = null;
    private boolean mMwiValue = false;
    private boolean mCfiValue = false;
    private List<CellInfo> mCellInfoResult = null;
    private CellLocation mCellLocationResult = null;
    private List<NeighboringCellInfo> mNeighboringCellResult = null;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDataConnectionStateChanged(int state) {
            RadioInfo.this.updateDataState();
            RadioInfo.this.updateNetworkType();
        }

        @Override
        public void onDataActivity(int direction) {
            RadioInfo.this.updateDataStats2();
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            RadioInfo.this.updateNetworkType();
            RadioInfo.this.updatePhoneState(state);
        }

        public void onPreciseCallStateChanged(PreciseCallState preciseState) {
            RadioInfo.this.updateNetworkType();
        }

        @Override
        public void onCellLocationChanged(CellLocation location) {
            RadioInfo.this.updateLocation(location);
        }

        @Override
        public void onMessageWaitingIndicatorChanged(boolean mwi) {
            RadioInfo.this.mMwiValue = mwi;
            RadioInfo.this.updateMessageWaiting();
        }

        @Override
        public void onCallForwardingIndicatorChanged(boolean cfi) {
            RadioInfo.this.mCfiValue = cfi;
            RadioInfo.this.updateCallRedirect();
        }

        @Override
        public void onCellInfoChanged(List<CellInfo> arrayCi) {
            RadioInfo.this.log("onCellInfoChanged: arrayCi=" + arrayCi);
            RadioInfo.this.mCellInfoResult = arrayCi;
            RadioInfo.this.updateCellInfo(RadioInfo.this.mCellInfoResult);
        }

        public void onDataConnectionRealTimeInfoChanged(DataConnectionRealTimeInfo dcRtInfo) {
            RadioInfo.this.log("onDataConnectionRealTimeInfoChanged: dcRtInfo=" + dcRtInfo);
            RadioInfo.this.updateDcRtInfoTv(dcRtInfo);
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
            RadioInfo.this.updateImsVoLteProvisionedState();
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1000:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception == null && ar.result != null) {
                        RadioInfo.this.updatePreferredNetworkType(((int[]) ar.result)[0]);
                    } else {
                        RadioInfo.this.updatePreferredNetworkType(RadioInfo.mPreferredNetworkLabels.length - 1);
                    }
                    break;
                case 1001:
                    if (((AsyncResult) msg.obj).exception != null) {
                        RadioInfo.this.log("Set preferred network type failed.");
                    }
                    break;
                case 1002:
                case 1003:
                case 1004:
                default:
                    super.handleMessage(msg);
                    break;
                case 1005:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    if (ar2.exception != null) {
                        RadioInfo.this.smsc.setText("refresh error");
                    } else {
                        RadioInfo.this.smsc.setText((String) ar2.result);
                    }
                    break;
                case 1006:
                    RadioInfo.this.updateSmscButton.setEnabled(true);
                    if (((AsyncResult) msg.obj).exception != null) {
                        RadioInfo.this.smsc.setText("update error");
                    }
                    break;
            }
        }
    };
    private MenuItem.OnMenuItemClickListener mViewADNCallback = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setClassName("com.android.phone", "com.android.phone.SimContacts");
            RadioInfo.this.startActivity(intent);
            return true;
        }
    };
    private MenuItem.OnMenuItemClickListener mViewFDNCallback = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setClassName("com.android.phone", "com.android.phone.settings.fdn.FdnList");
            RadioInfo.this.startActivity(intent);
            return true;
        }
    };
    private MenuItem.OnMenuItemClickListener mViewSDNCallback = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("content://icc/sdn"));
            intent.setClassName("com.android.phone", "com.android.phone.ADNList");
            RadioInfo.this.startActivity(intent);
            return true;
        }
    };
    private MenuItem.OnMenuItemClickListener mGetPdpList = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            RadioInfo.this.phone.getDataCallList((Message) null);
            return true;
        }
    };
    private MenuItem.OnMenuItemClickListener mSelectBandCallback = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent();
            intent.setClass(RadioInfo.this, BandMode.class);
            RadioInfo.this.startActivity(intent);
            return true;
        }
    };
    private MenuItem.OnMenuItemClickListener mToggleData = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int state = RadioInfo.this.mTelephonyManager.getDataState();
            switch (state) {
                case DefaultWfcSettingsExt.RESUME:
                    RadioInfo.this.phone.setDataEnabled(true);
                    return true;
                case DefaultWfcSettingsExt.PAUSE:
                default:
                    return true;
                case DefaultWfcSettingsExt.CREATE:
                    RadioInfo.this.phone.setDataEnabled(false);
                    return true;
            }
        }
    };
    CompoundButton.OnCheckedChangeListener mRadioPowerOnChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            RadioInfo.this.log("toggle radio power: currently " + (RadioInfo.this.isRadioOn() ? "on" : "off"));
            RadioInfo.this.phone.setRadioPower(isChecked);
        }
    };
    CompoundButton.OnCheckedChangeListener mImsVoLteCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            RadioInfo.this.setImsVoLteProvisionedState(isChecked);
        }
    };
    View.OnClickListener mDnsCheckButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RadioInfo.this.phone.disableDnsCheck(!RadioInfo.this.phone.isDnsCheckDisabled());
            RadioInfo.this.updateDnsCheckState();
        }
    };
    View.OnClickListener mOemInfoButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent("com.android.settings.OEM_RADIO_INFO");
            try {
                RadioInfo.this.startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                RadioInfo.this.log("OEM-specific Info/Settings Activity Not Found : " + ex);
            }
        }
    };
    View.OnClickListener mPingButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RadioInfo.this.updatePingState();
        }
    };
    View.OnClickListener mUpdateSmscButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RadioInfo.this.updateSmscButton.setEnabled(false);
            RadioInfo.this.phone.setSmscAddress(RadioInfo.this.smsc.getText().toString(), RadioInfo.this.mHandler.obtainMessage(1006));
        }
    };
    View.OnClickListener mRefreshSmscButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RadioInfo.this.refreshSmsc();
        }
    };
    AdapterView.OnItemSelectedListener mPreferredNetworkHandler = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView parent, View v, int pos, long id) {
            if (RadioInfo.this.mPreferredNetworkTypeResult == pos || pos < 0 || pos > RadioInfo.mPreferredNetworkLabels.length - 2) {
                return;
            }
            RadioInfo.this.mPreferredNetworkTypeResult = pos;
            Message msg = RadioInfo.this.mHandler.obtainMessage(1001);
            RadioInfo.this.phone.setPreferredNetworkType(RadioInfo.this.mPreferredNetworkTypeResult, msg);
        }

        @Override
        public void onNothingSelected(AdapterView parent) {
        }
    };
    AdapterView.OnItemSelectedListener mCellInfoRefreshRateHandler = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView parent, View v, int pos, long id) {
            RadioInfo.this.mCellInfoRefreshRateIndex = pos;
            RadioInfo.this.phone.setCellInfoListRate(RadioInfo.mCellInfoRefreshRates[pos]);
            RadioInfo.this.updateAllCellInfo();
        }

        @Override
        public void onNothingSelected(AdapterView parent) {
        }
    };

    public void log(String s) {
        Log.d("RadioInfo", s);
    }

    public void updatePreferredNetworkType(int type) {
        if (type >= mPreferredNetworkLabels.length || type < 0) {
            log("EVENT_QUERY_PREFERRED_TYPE_DONE: unknown type=" + type);
            type = mPreferredNetworkLabels.length - 1;
        }
        this.mPreferredNetworkTypeResult = type;
        this.preferredNetworkType.setSelection(this.mPreferredNetworkTypeResult, true);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.radio_info);
        log("Started onCreate");
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        this.phone = PhoneFactory.getDefaultPhone();
        this.mImsManager = ImsManager.getInstance(getApplicationContext(), SubscriptionManager.getDefaultVoicePhoneId());
        this.mDeviceId = (TextView) findViewById(R.id.imei);
        this.number = (TextView) findViewById(R.id.number);
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
        this.preferredNetworkType = (Spinner) findViewById(R.id.preferredNetworkType);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mPreferredNetworkLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.preferredNetworkType.setAdapter((SpinnerAdapter) adapter);
        this.cellInfoRefreshRateSpinner = (Spinner) findViewById(R.id.cell_info_rate_select);
        ArrayAdapter<String> cellInfoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mCellInfoRefreshRateLabels);
        cellInfoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.cellInfoRefreshRateSpinner.setAdapter((SpinnerAdapter) cellInfoAdapter);
        this.imsVoLteProvisionedSwitch = (Switch) findViewById(R.id.volte_provisioned_switch);
        this.radioPowerOnSwitch = (Switch) findViewById(R.id.radio_power);
        this.pingTestButton = (Button) findViewById(R.id.ping_test);
        this.pingTestButton.setOnClickListener(this.mPingButtonHandler);
        this.updateSmscButton = (Button) findViewById(R.id.update_smsc);
        this.updateSmscButton.setOnClickListener(this.mUpdateSmscButtonHandler);
        this.refreshSmscButton = (Button) findViewById(R.id.refresh_smsc);
        this.refreshSmscButton.setOnClickListener(this.mRefreshSmscButtonHandler);
        this.dnsCheckToggleButton = (Button) findViewById(R.id.dns_check_toggle);
        this.dnsCheckToggleButton.setOnClickListener(this.mDnsCheckButtonHandler);
        this.oemInfoButton = (Button) findViewById(R.id.oem_info);
        this.oemInfoButton.setOnClickListener(this.mOemInfoButtonHandler);
        PackageManager pm = getPackageManager();
        Intent oemInfoIntent = new Intent("com.android.settings.OEM_RADIO_INFO");
        List<ResolveInfo> oemInfoIntentList = pm.queryIntentActivities(oemInfoIntent, 0);
        if (oemInfoIntentList.size() == 0) {
            this.oemInfoButton.setEnabled(false);
        }
        this.mCellInfoRefreshRateIndex = 0;
        this.mPreferredNetworkTypeResult = mPreferredNetworkLabels.length - 1;
        this.phone.getPreferredNetworkType(this.mHandler.obtainMessage(1000));
        restoreFromBundle(icicle);
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
        updateImsVoLteProvisionedState();
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
        this.imsVoLteProvisionedSwitch.setOnCheckedChangeListener(this.mImsVoLteCheckedChangeListener);
        this.mTelephonyManager.listen(this.mPhoneStateListener, 9725);
        this.smsc.clearFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause: unregister phone & data intents");
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        this.phone.setCellInfoListRate(Integer.MAX_VALUE);
    }

    private void restoreFromBundle(Bundle b) {
        if (b == null) {
            return;
        }
        this.mPingHostnameResultV4 = b.getString("mPingHostnameResultV4", "");
        this.mPingHostnameResultV6 = b.getString("mPingHostnameResultV6", "");
        this.mHttpClientTestResult = b.getString("mHttpClientTestResult", "");
        this.mPingHostnameV4.setText(this.mPingHostnameResultV4);
        this.mPingHostnameV6.setText(this.mPingHostnameResultV6);
        this.mHttpClientTest.setText(this.mHttpClientTestResult);
        this.mPreferredNetworkTypeResult = b.getInt("mPreferredNetworkTypeResult", mPreferredNetworkLabels.length - 1);
        this.mCellInfoRefreshRateIndex = b.getInt("mCellInfoRefreshRateIndex", 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("mPingHostnameResultV4", this.mPingHostnameResultV4);
        outState.putString("mPingHostnameResultV6", this.mPingHostnameResultV6);
        outState.putString("mHttpClientTestResult", this.mHttpClientTestResult);
        outState.putInt("mPreferredNetworkTypeResult", this.mPreferredNetworkTypeResult);
        outState.putInt("mCellInfoRefreshRateIndex", this.mCellInfoRefreshRateIndex);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.radio_info_band_mode_label).setOnMenuItemClickListener(this.mSelectBandCallback).setAlphabeticShortcut('b');
        menu.add(1, 1, 0, R.string.radioInfo_menu_viewADN).setOnMenuItemClickListener(this.mViewADNCallback);
        menu.add(1, 2, 0, R.string.radioInfo_menu_viewFDN).setOnMenuItemClickListener(this.mViewFDNCallback);
        menu.add(1, 3, 0, R.string.radioInfo_menu_viewSDN).setOnMenuItemClickListener(this.mViewSDNCallback);
        menu.add(1, 4, 0, R.string.radioInfo_menu_getPDP).setOnMenuItemClickListener(this.mGetPdpList);
        menu.add(1, 5, 0, R.string.radio_info_data_connection_disable).setOnMenuItemClickListener(this.mToggleData);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(5);
        int state = this.mTelephonyManager.getDataState();
        boolean visible = true;
        switch (state) {
            case DefaultWfcSettingsExt.RESUME:
                item.setTitle(R.string.radio_info_data_connection_enable);
                break;
            case DefaultWfcSettingsExt.PAUSE:
            default:
                visible = false;
                break;
            case DefaultWfcSettingsExt.CREATE:
            case DefaultWfcSettingsExt.DESTROY:
                item.setTitle(R.string.radio_info_data_connection_disable);
                break;
        }
        item.setVisible(visible);
        return true;
    }

    public void updateDnsCheckState() {
        this.dnsCheckState.setText(this.phone.isDnsCheckDisabled() ? "0.0.0.0 allowed" : "0.0.0.0 not allowed");
    }

    public final void updateSignalStrength(SignalStrength signalStrength) {
        Resources r = getResources();
        int signalDbm = signalStrength.getDbm();
        int signalAsu = signalStrength.getAsuLevel();
        if (-1 == signalAsu) {
            signalAsu = 0;
        }
        this.dBm.setText(String.valueOf(signalDbm) + " " + r.getString(R.string.radioInfo_display_dbm) + "   " + String.valueOf(signalAsu) + " " + r.getString(R.string.radioInfo_display_asu));
    }

    public final void updateLocation(CellLocation location) {
        Resources r = getResources();
        if (location instanceof GsmCellLocation) {
            GsmCellLocation loc = (GsmCellLocation) location;
            int lac = loc.getLac();
            int cid = loc.getCid();
            this.mLocation.setText(r.getString(R.string.radioInfo_lac) + " = " + (lac == -1 ? "unknown" : Integer.toHexString(lac)) + "   " + r.getString(R.string.radioInfo_cid) + " = " + (cid == -1 ? "unknown" : Integer.toHexString(cid)));
            return;
        }
        if (!(location instanceof CdmaCellLocation)) {
            this.mLocation.setText("unknown");
            return;
        }
        CdmaCellLocation loc2 = (CdmaCellLocation) location;
        int bid = loc2.getBaseStationId();
        int sid = loc2.getSystemId();
        int nid = loc2.getNetworkId();
        int lat = loc2.getBaseStationLatitude();
        int lon = loc2.getBaseStationLongitude();
        this.mLocation.setText("BID = " + (bid == -1 ? "unknown" : Integer.toHexString(bid)) + "   SID = " + (sid == -1 ? "unknown" : Integer.toHexString(sid)) + "   NID = " + (nid == -1 ? "unknown" : Integer.toHexString(nid)) + "\nLAT = " + (lat == -1 ? "unknown" : Integer.toHexString(lat)) + "   LONG = " + (lon == -1 ? "unknown" : Integer.toHexString(lon)));
    }

    public final void updateNeighboringCids(List<NeighboringCellInfo> cids) {
        StringBuilder sb = new StringBuilder();
        if (cids != null) {
            if (cids.isEmpty()) {
                sb.append("no neighboring cells");
            } else {
                for (NeighboringCellInfo cell : cids) {
                    sb.append(cell.toString()).append(" ");
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

    private final String buildCdmaInfoString(CellInfoCdma ci) {
        CellIdentityCdma cidCdma = ci.getCellIdentity();
        CellSignalStrengthCdma ssCdma = ci.getCellSignalStrength();
        Object[] objArr = new Object[9];
        objArr[0] = ci.isRegistered() ? "S  " : "   ";
        objArr[1] = getCellInfoDisplayString(cidCdma.getSystemId());
        objArr[2] = getCellInfoDisplayString(cidCdma.getNetworkId());
        objArr[3] = getCellInfoDisplayString(cidCdma.getBasestationId());
        objArr[4] = getCellInfoDisplayString(ssCdma.getCdmaDbm());
        objArr[5] = getCellInfoDisplayString(ssCdma.getCdmaEcio());
        objArr[6] = getCellInfoDisplayString(ssCdma.getEvdoDbm());
        objArr[7] = getCellInfoDisplayString(ssCdma.getEvdoEcio());
        objArr[8] = getCellInfoDisplayString(ssCdma.getEvdoSnr());
        return String.format("%-3.3s %-5.5s %-5.5s %-5.5s %-6.6s %-6.6s %-6.6s %-6.6s %-5.5s", objArr);
    }

    private final String buildGsmInfoString(CellInfoGsm ci) {
        CellIdentityGsm cidGsm = ci.getCellIdentity();
        CellSignalStrengthGsm ssGsm = ci.getCellSignalStrength();
        Object[] objArr = new Object[8];
        objArr[0] = ci.isRegistered() ? "S  " : "   ";
        objArr[1] = getCellInfoDisplayString(cidGsm.getMcc());
        objArr[2] = getCellInfoDisplayString(cidGsm.getMnc());
        objArr[3] = getCellInfoDisplayString(cidGsm.getLac());
        objArr[4] = getCellInfoDisplayString(cidGsm.getCid());
        objArr[5] = getCellInfoDisplayString(cidGsm.getArfcn());
        objArr[6] = getCellInfoDisplayString(cidGsm.getBsic());
        objArr[7] = getCellInfoDisplayString(ssGsm.getDbm());
        return String.format("%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-6.6s %-4.4s %-4.4s\n", objArr);
    }

    private final String buildLteInfoString(CellInfoLte ci) {
        CellIdentityLte cidLte = ci.getCellIdentity();
        CellSignalStrengthLte ssLte = ci.getCellSignalStrength();
        Object[] objArr = new Object[10];
        objArr[0] = ci.isRegistered() ? "S  " : "   ";
        objArr[1] = getCellInfoDisplayString(cidLte.getMcc());
        objArr[2] = getCellInfoDisplayString(cidLte.getMnc());
        objArr[3] = getCellInfoDisplayString(cidLte.getTac());
        objArr[4] = getCellInfoDisplayString(cidLte.getCi());
        objArr[5] = getCellInfoDisplayString(cidLte.getPci());
        objArr[6] = getCellInfoDisplayString(cidLte.getEarfcn());
        objArr[7] = getCellInfoDisplayString(ssLte.getDbm());
        objArr[8] = getCellInfoDisplayString(ssLte.getRsrq());
        objArr[9] = getCellInfoDisplayString(ssLte.getTimingAdvance());
        return String.format("%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-3.3s %-6.6s %-4.4s %-4.4s %-2.2s\n", objArr);
    }

    private final String buildWcdmaInfoString(CellInfoWcdma ci) {
        CellIdentityWcdma cidWcdma = ci.getCellIdentity();
        CellSignalStrengthWcdma ssWcdma = ci.getCellSignalStrength();
        Object[] objArr = new Object[8];
        objArr[0] = ci.isRegistered() ? "S  " : "   ";
        objArr[1] = getCellInfoDisplayString(cidWcdma.getMcc());
        objArr[2] = getCellInfoDisplayString(cidWcdma.getMnc());
        objArr[3] = getCellInfoDisplayString(cidWcdma.getLac());
        objArr[4] = getCellInfoDisplayString(cidWcdma.getCid());
        objArr[5] = getCellInfoDisplayString(cidWcdma.getUarfcn());
        objArr[6] = getCellInfoDisplayString(cidWcdma.getPsc());
        objArr[7] = getCellInfoDisplayString(ssWcdma.getDbm());
        return String.format("%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-6.6s %-3.3s %-4.4s\n", objArr);
    }

    private final String buildCellInfoString(List<CellInfo> arrayCi) {
        String value = new String();
        StringBuilder cdmaCells = new StringBuilder();
        StringBuilder gsmCells = new StringBuilder();
        StringBuilder lteCells = new StringBuilder();
        StringBuilder wcdmaCells = new StringBuilder();
        if (arrayCi != null) {
            for (CellInfo ci : arrayCi) {
                if (ci instanceof CellInfoLte) {
                    lteCells.append(buildLteInfoString((CellInfoLte) ci));
                } else if (ci instanceof CellInfoWcdma) {
                    wcdmaCells.append(buildWcdmaInfoString((CellInfoWcdma) ci));
                } else if (ci instanceof CellInfoGsm) {
                    gsmCells.append(buildGsmInfoString((CellInfoGsm) ci));
                } else if (ci instanceof CellInfoCdma) {
                    cdmaCells.append(buildCdmaInfoString((CellInfoCdma) ci));
                }
            }
            if (lteCells.length() != 0) {
                value = (value + String.format("LTE\n%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-3.3s %-6.6s %-4.4s %-4.4s %-2.2s\n", "SRV", "MCC", "MNC", "TAC", "CID", "PCI", "EARFCN", "RSRP", "RSRQ", "TA")) + lteCells.toString();
            }
            if (wcdmaCells.length() != 0) {
                value = (value + String.format("WCDMA\n%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-6.6s %-3.3s %-4.4s\n", "SRV", "MCC", "MNC", "LAC", "CID", "UARFCN", "PSC", "RSCP")) + wcdmaCells.toString();
            }
            if (gsmCells.length() != 0) {
                value = (value + String.format("GSM\n%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-6.6s %-4.4s %-4.4s\n", "SRV", "MCC", "MNC", "LAC", "CID", "ARFCN", "BSIC", "RSSI")) + gsmCells.toString();
            }
            if (cdmaCells.length() != 0) {
                value = (value + String.format("CDMA/EVDO\n%-3.3s %-5.5s %-5.5s %-5.5s %-6.6s %-6.6s %-6.6s %-6.6s %-5.5s\n", "SRV", "SID", "NID", "BSID", "C-RSSI", "C-ECIO", "E-RSSI", "E-ECIO", "E-SNR")) + cdmaCells.toString();
            }
        } else {
            value = "unknown";
        }
        return value.toString();
    }

    public final void updateCellInfo(List<CellInfo> arrayCi) {
        this.mCellInfo.setText(buildCellInfoString(arrayCi));
    }

    public final void updateDcRtInfoTv(DataConnectionRealTimeInfo dcRtInfo) {
        this.mDcRtInfoTv.setText(dcRtInfo.toString());
    }

    public final void updateMessageWaiting() {
        this.mMwi.setText(String.valueOf(this.mMwiValue));
    }

    public final void updateCallRedirect() {
        this.mCfi.setText(String.valueOf(this.mCfiValue));
    }

    public final void updateServiceState(ServiceState serviceState) {
        int state = serviceState.getState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);
        switch (state) {
            case DefaultWfcSettingsExt.RESUME:
                display = r.getString(R.string.radioInfo_service_in);
                break;
            case DefaultWfcSettingsExt.PAUSE:
            case DefaultWfcSettingsExt.CREATE:
                display = r.getString(R.string.radioInfo_service_emergency);
                break;
            case DefaultWfcSettingsExt.DESTROY:
                display = r.getString(R.string.radioInfo_service_off);
                break;
        }
        this.gsmState.setText(display);
        if (serviceState.getRoaming()) {
            this.roamingState.setText(R.string.radioInfo_roaming_in);
        } else {
            this.roamingState.setText(R.string.radioInfo_roaming_not);
        }
        this.operatorName.setText(serviceState.getOperatorAlphaLong());
    }

    public final void updatePhoneState(int state) {
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);
        switch (state) {
            case DefaultWfcSettingsExt.RESUME:
                display = r.getString(R.string.radioInfo_phone_idle);
                break;
            case DefaultWfcSettingsExt.PAUSE:
                display = r.getString(R.string.radioInfo_phone_ringing);
                break;
            case DefaultWfcSettingsExt.CREATE:
                display = r.getString(R.string.radioInfo_phone_offhook);
                break;
        }
        this.callState.setText(display);
    }

    public final void updateDataState() {
        int state = this.mTelephonyManager.getDataState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);
        switch (state) {
            case DefaultWfcSettingsExt.RESUME:
                display = r.getString(R.string.radioInfo_data_disconnected);
                break;
            case DefaultWfcSettingsExt.PAUSE:
                display = r.getString(R.string.radioInfo_data_connecting);
                break;
            case DefaultWfcSettingsExt.CREATE:
                display = r.getString(R.string.radioInfo_data_connected);
                break;
            case DefaultWfcSettingsExt.DESTROY:
                display = r.getString(R.string.radioInfo_data_suspended);
                break;
        }
        this.gprsState.setText(display);
    }

    public final void updateNetworkType() {
        if (this.phone == null) {
            return;
        }
        this.phone.getServiceState();
        this.dataNetwork.setText(ServiceState.rilRadioTechnologyToString(this.phone.getServiceState().getRilDataRadioTechnology()));
        this.voiceNetwork.setText(ServiceState.rilRadioTechnologyToString(this.phone.getServiceState().getRilVoiceRadioTechnology()));
    }

    private final void updateProperties() {
        Resources r = getResources();
        String s = this.phone.getDeviceId();
        if (s == null) {
            s = r.getString(R.string.radioInfo_unknown);
        }
        this.mDeviceId.setText(s);
        String s2 = this.phone.getLine1Number();
        if (s2 == null) {
            s2 = r.getString(R.string.radioInfo_unknown);
        }
        this.number.setText(s2);
    }

    public final void updateDataStats2() {
        Resources r = getResources();
        long txPackets = TrafficStats.getMobileTxPackets();
        long rxPackets = TrafficStats.getMobileRxPackets();
        long txBytes = TrafficStats.getMobileTxBytes();
        long rxBytes = TrafficStats.getMobileRxBytes();
        String packets = r.getString(R.string.radioInfo_display_packets);
        String bytes = r.getString(R.string.radioInfo_display_bytes);
        this.sent.setText(txPackets + " " + packets + ", " + txBytes + " " + bytes);
        this.received.setText(rxPackets + " " + packets + ", " + rxBytes + " " + bytes);
    }

    public final void pingHostname() {
        try {
            try {
                Process p4 = Runtime.getRuntime().exec("ping -c 1 www.google.com");
                int status4 = p4.waitFor();
                if (status4 == 0) {
                    this.mPingHostnameResultV4 = "Pass";
                } else {
                    this.mPingHostnameResultV4 = String.format("Fail(%d)", Integer.valueOf(status4));
                }
            } catch (IOException e) {
                this.mPingHostnameResultV4 = "Fail: IOException";
            }
            try {
                Process p6 = Runtime.getRuntime().exec("ping6 -c 1 www.google.com");
                int status6 = p6.waitFor();
                if (status6 == 0) {
                    this.mPingHostnameResultV6 = "Pass";
                } else {
                    this.mPingHostnameResultV6 = String.format("Fail(%d)", Integer.valueOf(status6));
                }
            } catch (IOException e2) {
                this.mPingHostnameResultV6 = "Fail: IOException";
            }
        } catch (InterruptedException e3) {
            this.mPingHostnameResultV6 = "Fail: InterruptedException";
            this.mPingHostnameResultV4 = "Fail: InterruptedException";
        }
    }

    public void httpClientTest() {
        HttpURLConnection httpURLConnection = null;
        try {
            try {
                URL url = new URL("https://www.google.com");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                if (urlConnection.getResponseCode() == 200) {
                    this.mHttpClientTestResult = "Pass";
                } else {
                    this.mHttpClientTestResult = "Fail: Code: " + urlConnection.getResponseMessage();
                }
                if (urlConnection == null) {
                    return;
                }
                urlConnection.disconnect();
            } catch (IOException e) {
                this.mHttpClientTestResult = "Fail: IOException";
                if (0 == 0) {
                    return;
                }
                httpURLConnection.disconnect();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                httpURLConnection.disconnect();
            }
            throw th;
        }
    }

    public void refreshSmsc() {
        this.phone.getSmscAddress(this.mHandler.obtainMessage(1005));
    }

    public final void updateAllCellInfo() {
        this.mCellInfo.setText("");
        this.mNeighboringCids.setText("");
        this.mLocation.setText("");
        final Runnable updateAllCellInfoResults = new Runnable() {
            @Override
            public void run() {
                RadioInfo.this.updateNeighboringCids(RadioInfo.this.mNeighboringCellResult);
                RadioInfo.this.updateLocation(RadioInfo.this.mCellLocationResult);
                RadioInfo.this.updateCellInfo(RadioInfo.this.mCellInfoResult);
            }
        };
        Thread locThread = new Thread() {
            @Override
            public void run() {
                RadioInfo.this.mCellInfoResult = RadioInfo.this.mTelephonyManager.getAllCellInfo();
                RadioInfo.this.mCellLocationResult = RadioInfo.this.mTelephonyManager.getCellLocation();
                RadioInfo.this.mNeighboringCellResult = RadioInfo.this.mTelephonyManager.getNeighboringCellInfo();
                RadioInfo.this.mHandler.post(updateAllCellInfoResults);
            }
        };
        locThread.start();
    }

    public final void updatePingState() {
        this.mPingHostnameResultV4 = getResources().getString(R.string.radioInfo_unknown);
        this.mPingHostnameResultV6 = getResources().getString(R.string.radioInfo_unknown);
        this.mHttpClientTestResult = getResources().getString(R.string.radioInfo_unknown);
        this.mPingHostnameV4.setText(this.mPingHostnameResultV4);
        this.mPingHostnameV6.setText(this.mPingHostnameResultV6);
        this.mHttpClientTest.setText(this.mHttpClientTestResult);
        final Runnable updatePingResults = new Runnable() {
            @Override
            public void run() {
                RadioInfo.this.mPingHostnameV4.setText(RadioInfo.this.mPingHostnameResultV4);
                RadioInfo.this.mPingHostnameV6.setText(RadioInfo.this.mPingHostnameResultV6);
                RadioInfo.this.mHttpClientTest.setText(RadioInfo.this.mHttpClientTestResult);
            }
        };
        Thread hostname = new Thread() {
            @Override
            public void run() {
                RadioInfo.this.pingHostname();
                RadioInfo.this.mHandler.post(updatePingResults);
            }
        };
        hostname.start();
        Thread httpClient = new Thread() {
            @Override
            public void run() {
                RadioInfo.this.httpClientTest();
                RadioInfo.this.mHandler.post(updatePingResults);
            }
        };
        httpClient.start();
    }

    public boolean isRadioOn() {
        return this.phone.getServiceState().getState() != 3;
    }

    public void updateRadioPowerState() {
        this.radioPowerOnSwitch.setOnCheckedChangeListener(null);
        this.radioPowerOnSwitch.setChecked(isRadioOn());
        this.radioPowerOnSwitch.setOnCheckedChangeListener(this.mRadioPowerOnChangeListener);
    }

    void setImsVoLteProvisionedState(final boolean state) {
        Object[] objArr = new Object[1];
        objArr[0] = state ? "on" : "off";
        log(String.format("toggle VoLTE provisioned: %s", objArr));
        if (this.phone == null || this.mImsManager == null) {
            return;
        }
        QueuedWork.singleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    RadioInfo.this.mImsManager.getConfigInterface().setProvisionedValue(10, state ? 1 : 0);
                } catch (ImsException e) {
                    Log.e("RadioInfo", "setImsVoLteProvisioned() exception:", e);
                }
            }
        });
    }

    private boolean isImsVoLteProvisioned() {
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

    public void updateImsVoLteProvisionedState() {
        log("updateImsVoLteProvisionedState isImsVoLteProvisioned()=" + isImsVoLteProvisioned());
        this.imsVoLteProvisionedSwitch.setOnCheckedChangeListener(null);
        this.imsVoLteProvisionedSwitch.setChecked(isImsVoLteProvisioned());
        this.imsVoLteProvisionedSwitch.setOnCheckedChangeListener(this.mImsVoLteCheckedChangeListener);
    }
}
