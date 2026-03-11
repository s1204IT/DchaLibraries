package com.android.settings;

import android.app.Activity;
import android.app.QueuedWork;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class RadioInfo extends Activity {
    private TextView attempts;
    private TextView callState;
    private Button cellInfoListRateButton;
    private TextView dBm;
    private TextView disconnects;
    private TextView dnsCheckState;
    private Button dnsCheckToggleButton;
    private TextView gprsState;
    private TextView gsmState;
    private Button imsRegRequiredButton;
    private Button imsVoLteProvisionedButton;
    private Button lteRamDumpButton;
    private TextView mCellInfo;
    private List<CellInfo> mCellInfoValue;
    private TextView mCfi;
    private TextView mDcRtInfoTv;
    private TextView mDeviceId;
    private TextView mHttpClientTest;
    private String mHttpClientTestResult;
    private TextView mLocation;
    private TextView mMwi;
    private TextView mNeighboringCids;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private TextView mPingHostname;
    private String mPingHostnameResult;
    private TextView mPingIpAddr;
    private String mPingIpAddrResult;
    private TelephonyManager mTelephonyManager;
    private TextView network;
    private TextView number;
    private Button oemInfoButton;
    private TextView operatorName;
    private Button pingTestButton;
    private Spinner preferredNetworkType;
    private Button radioPowerButton;
    private TextView received;
    private Button refreshSmscButton;
    private TextView resets;
    private TextView roamingState;
    private TextView sent;
    private TextView sentSinceReceived;
    private Button smsOverImsButton;
    private EditText smsc;
    private TextView successes;
    private Button updateSmscButton;
    private final String TAG = "phone";
    private Phone phone = null;
    private boolean mMwiValue = false;
    private boolean mCfiValue = false;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDataConnectionStateChanged(int state) {
            RadioInfo.this.updateDataState();
            RadioInfo.this.updateDataStats();
            RadioInfo.this.updatePdpList();
            RadioInfo.this.updateNetworkType();
        }

        @Override
        public void onDataActivity(int direction) {
            RadioInfo.this.updateDataStats2();
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
            RadioInfo.this.updateCellInfoTv(arrayCi);
        }

        public void onDataConnectionRealTimeInfoChanged(DataConnectionRealTimeInfo dcRtInfo) {
            RadioInfo.this.log("onDataConnectionRealTimeInfoChanged: dcRtInfo=" + dcRtInfo);
            RadioInfo.this.updateDcRtInfoTv(dcRtInfo);
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    RadioInfo.this.updatePhoneState();
                    break;
                case 200:
                    RadioInfo.this.updateSignalStrength();
                    break;
                case 300:
                    RadioInfo.this.updateServiceState();
                    RadioInfo.this.updatePowerState();
                    RadioInfo.this.updateImsVoLteProvisionedState();
                    break;
                case 1000:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        RadioInfo.this.preferredNetworkType.setSelection(RadioInfo.this.mPreferredNetworkLabels.length - 1, true);
                    } else {
                        int type = ((int[]) ar.result)[0];
                        if (type >= RadioInfo.this.mPreferredNetworkLabels.length) {
                            RadioInfo.this.log("EVENT_QUERY_PREFERRED_TYPE_DONE: unknown type=" + type);
                            type = RadioInfo.this.mPreferredNetworkLabels.length - 1;
                        }
                        RadioInfo.this.preferredNetworkType.setSelection(type, true);
                    }
                    break;
                case 1001:
                    if (((AsyncResult) msg.obj).exception != null) {
                        RadioInfo.this.phone.getPreferredNetworkType(obtainMessage(1000));
                    }
                    break;
                case 1002:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    if (ar2.exception == null) {
                        RadioInfo.this.updateNeighboringCids((ArrayList) ar2.result);
                    } else {
                        RadioInfo.this.mNeighboringCids.setText("unknown");
                    }
                    break;
                case 1005:
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    if (ar3.exception != null) {
                        RadioInfo.this.smsc.setText("refresh error");
                    } else {
                        RadioInfo.this.smsc.setText((String) ar3.result);
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
                case 0:
                    RadioInfo.this.phone.setDataEnabled(true);
                    return true;
                case 1:
                default:
                    return true;
                case 2:
                    RadioInfo.this.phone.setDataEnabled(false);
                    return true;
            }
        }
    };
    View.OnClickListener mPowerButtonHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RadioInfo.this.phone.setRadioPower(!RadioInfo.this.isRadioOn());
        }
    };
    CellInfoListRateHandler mCellInfoListRateHandler = new CellInfoListRateHandler();
    View.OnClickListener mImsRegRequiredHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RadioInfo radioInfo = RadioInfo.this;
            Object[] objArr = new Object[2];
            objArr[0] = "persist.radio.imsregrequired";
            objArr[1] = RadioInfo.this.isImsRegRequired() ? "on" : "off";
            radioInfo.log(String.format("toggle %s: currently %s", objArr));
            boolean newValue = RadioInfo.this.isImsRegRequired() ? false : true;
            SystemProperties.set("persist.radio.imsregrequired", newValue ? "1" : "0");
            RadioInfo.this.updateImsRegRequiredState();
        }
    };
    View.OnClickListener mSmsOverImsHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RadioInfo radioInfo = RadioInfo.this;
            Object[] objArr = new Object[2];
            objArr[0] = "persist.radio.imsallowmtsms";
            objArr[1] = RadioInfo.this.isSmsOverImsEnabled() ? "on" : "off";
            radioInfo.log(String.format("toggle %s: currently %s", objArr));
            boolean newValue = RadioInfo.this.isSmsOverImsEnabled() ? false : true;
            SystemProperties.set("persist.radio.imsallowmtsms", newValue ? "1" : "0");
            RadioInfo.this.updateSmsOverImsState();
        }
    };
    View.OnClickListener mImsVoLteProvisionedHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final ImsManager imsManager;
            RadioInfo radioInfo = RadioInfo.this;
            Object[] objArr = new Object[1];
            objArr[0] = RadioInfo.this.isImsVoLteProvisioned() ? "on" : "off";
            radioInfo.log(String.format("toggle VoLTE provisioned: currently %s", objArr));
            final boolean newValue = RadioInfo.this.isImsVoLteProvisioned() ? false : true;
            if (RadioInfo.this.phone != null && (imsManager = ImsManager.getInstance(RadioInfo.this.phone.getContext(), RadioInfo.this.phone.getSubId())) != null) {
                QueuedWork.singleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            imsManager.getConfigInterface().setProvisionedValue(10, newValue ? 1 : 0);
                        } catch (ImsException e) {
                            Log.e("phone", "setImsVoLteProvisioned() exception:", e);
                        }
                    }
                });
            }
            RadioInfo.this.updateImsVoLteProvisionedState();
        }
    };
    View.OnClickListener mLteRamDumpHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RadioInfo radioInfo = RadioInfo.this;
            Object[] objArr = new Object[2];
            objArr[0] = "persist.radio.ramdump";
            objArr[1] = RadioInfo.this.isSmsOverImsEnabled() ? "on" : "off";
            radioInfo.log(String.format("toggle %s: currently %s", objArr));
            boolean newValue = RadioInfo.this.isLteRamDumpEnabled() ? false : true;
            SystemProperties.set("persist.radio.ramdump", newValue ? "1" : "0");
            RadioInfo.this.updateLteRamDumpState();
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
            Message msg = RadioInfo.this.mHandler.obtainMessage(1001);
            if (pos >= 0 && pos <= RadioInfo.this.mPreferredNetworkLabels.length - 2) {
                RadioInfo.this.phone.setPreferredNetworkType(pos, msg);
            }
        }

        @Override
        public void onNothingSelected(AdapterView parent) {
        }
    };
    private String[] mPreferredNetworkLabels = {"WCDMA preferred", "GSM only", "WCDMA only", "GSM auto (PRL)", "CDMA auto (PRL)", "CDMA only", "EvDo only", "GSM/CDMA auto (PRL)", "LTE/CDMA auto (PRL)", "LTE/GSM auto (PRL)", "LTE/GSM/CDMA auto (PRL)", "LTE only", "Unknown"};

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.radio_info);
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        this.phone = PhoneFactory.getDefaultPhone();
        this.mDeviceId = (TextView) findViewById(R.id.imei);
        this.number = (TextView) findViewById(R.id.number);
        this.callState = (TextView) findViewById(R.id.call);
        this.operatorName = (TextView) findViewById(R.id.operator);
        this.roamingState = (TextView) findViewById(R.id.roaming);
        this.gsmState = (TextView) findViewById(R.id.gsm);
        this.gprsState = (TextView) findViewById(R.id.gprs);
        this.network = (TextView) findViewById(R.id.network);
        this.dBm = (TextView) findViewById(R.id.dbm);
        this.mMwi = (TextView) findViewById(R.id.mwi);
        this.mCfi = (TextView) findViewById(R.id.cfi);
        this.mLocation = (TextView) findViewById(R.id.location);
        this.mNeighboringCids = (TextView) findViewById(R.id.neighboring);
        this.mCellInfo = (TextView) findViewById(R.id.cellinfo);
        this.mDcRtInfoTv = (TextView) findViewById(R.id.dcrtinfo);
        this.resets = (TextView) findViewById(R.id.resets);
        this.attempts = (TextView) findViewById(R.id.attempts);
        this.successes = (TextView) findViewById(R.id.successes);
        this.disconnects = (TextView) findViewById(R.id.disconnects);
        this.sentSinceReceived = (TextView) findViewById(R.id.sentSinceReceived);
        this.sent = (TextView) findViewById(R.id.sent);
        this.received = (TextView) findViewById(R.id.received);
        this.smsc = (EditText) findViewById(R.id.smsc);
        this.dnsCheckState = (TextView) findViewById(R.id.dnsCheckState);
        this.mPingIpAddr = (TextView) findViewById(R.id.pingIpAddr);
        this.mPingHostname = (TextView) findViewById(R.id.pingHostname);
        this.mHttpClientTest = (TextView) findViewById(R.id.httpClientTest);
        this.preferredNetworkType = (Spinner) findViewById(R.id.preferredNetworkType);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, this.mPreferredNetworkLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.preferredNetworkType.setAdapter((SpinnerAdapter) adapter);
        this.preferredNetworkType.setOnItemSelectedListener(this.mPreferredNetworkHandler);
        this.radioPowerButton = (Button) findViewById(R.id.radio_power);
        this.radioPowerButton.setOnClickListener(this.mPowerButtonHandler);
        this.cellInfoListRateButton = (Button) findViewById(R.id.cell_info_list_rate);
        this.cellInfoListRateButton.setOnClickListener(this.mCellInfoListRateHandler);
        this.imsRegRequiredButton = (Button) findViewById(R.id.ims_reg_required);
        this.imsRegRequiredButton.setOnClickListener(this.mImsRegRequiredHandler);
        this.imsVoLteProvisionedButton = (Button) findViewById(R.id.volte_provisioned_flag);
        this.imsVoLteProvisionedButton.setOnClickListener(this.mImsVoLteProvisionedHandler);
        this.smsOverImsButton = (Button) findViewById(R.id.sms_over_ims);
        this.smsOverImsButton.setOnClickListener(this.mSmsOverImsHandler);
        this.lteRamDumpButton = (Button) findViewById(R.id.lte_ram_dump);
        this.lteRamDumpButton.setOnClickListener(this.mLteRamDumpHandler);
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
        this.mPhoneStateReceiver = new PhoneStateIntentReceiver(this, this.mHandler);
        this.mPhoneStateReceiver.notifySignalStrength(200);
        this.mPhoneStateReceiver.notifyServiceState(300);
        this.mPhoneStateReceiver.notifyPhoneCallState(100);
        this.phone.getPreferredNetworkType(this.mHandler.obtainMessage(1000));
        this.phone.getNeighboringCids(this.mHandler.obtainMessage(1002));
        CellLocation.requestLocationUpdate();
        this.mCellInfoValue = this.mTelephonyManager.getAllCellInfo();
        log("onCreate: mCellInfoValue=" + this.mCellInfoValue);
        Toast.makeText(this, R.string.invalid_function, 1).show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePhoneState();
        updateSignalStrength();
        updateMessageWaiting();
        updateCallRedirect();
        updateServiceState();
        updateLocation(this.mTelephonyManager.getCellLocation());
        updateDataState();
        updateDataStats();
        updateDataStats2();
        updatePowerState();
        updateCellInfoListRate();
        updateImsRegRequiredState();
        updateImsVoLteProvisionedState();
        updateSmsOverImsState();
        updateLteRamDumpState();
        updateProperties();
        updateDnsCheckState();
        log("onResume: register phone & data intents");
        this.mPhoneStateReceiver.registerIntent();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 9436);
    }

    @Override
    public void onPause() {
        super.onPause();
        log("onPause: unregister phone & data intents");
        this.mPhoneStateReceiver.unregisterIntent();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.radio_info_band_mode_label).setOnMenuItemClickListener(this.mSelectBandCallback).setAlphabeticShortcut('b');
        menu.add(1, 1, 0, R.string.radioInfo_menu_viewADN).setOnMenuItemClickListener(this.mViewADNCallback);
        menu.add(1, 2, 0, R.string.radioInfo_menu_viewFDN).setOnMenuItemClickListener(this.mViewFDNCallback);
        menu.add(1, 3, 0, R.string.radioInfo_menu_viewSDN).setOnMenuItemClickListener(this.mViewSDNCallback);
        menu.add(1, 4, 0, R.string.radioInfo_menu_getPDP).setOnMenuItemClickListener(this.mGetPdpList);
        menu.add(1, 5, 0, "Disable data connection").setOnMenuItemClickListener(this.mToggleData);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(5);
        int state = this.mTelephonyManager.getDataState();
        boolean visible = true;
        switch (state) {
            case 0:
                item.setTitle("Enable data connection");
                break;
            case 1:
            default:
                visible = false;
                break;
            case 2:
            case 3:
                item.setTitle("Disable data connection");
                break;
        }
        item.setVisible(visible);
        return true;
    }

    public boolean isRadioOn() {
        return this.phone.getServiceState().getState() != 3;
    }

    public void updatePowerState() {
        String buttonText = isRadioOn() ? getString(R.string.turn_off_radio) : getString(R.string.turn_on_radio);
        this.radioPowerButton.setText(buttonText);
    }

    public void updateCellInfoListRate() {
        this.cellInfoListRateButton.setText("CellInfoListRate " + this.mCellInfoListRateHandler.getRate());
        updateCellInfoTv(this.mTelephonyManager.getAllCellInfo());
    }

    public void updateDnsCheckState() {
        this.dnsCheckState.setText(this.phone.isDnsCheckDisabled() ? "0.0.0.0 allowed" : "0.0.0.0 not allowed");
    }

    public final void updateSignalStrength() {
        int state = this.mPhoneStateReceiver.getServiceState().getState();
        Resources r = getResources();
        if (1 == state || 3 == state) {
            this.dBm.setText("0");
        }
        int signalDbm = this.mPhoneStateReceiver.getSignalStrengthDbm();
        if (-1 == signalDbm) {
            signalDbm = 0;
        }
        int signalAsu = this.mPhoneStateReceiver.getSignalStrengthLevelAsu();
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
        if (location instanceof CdmaCellLocation) {
            CdmaCellLocation loc2 = (CdmaCellLocation) location;
            int bid = loc2.getBaseStationId();
            int sid = loc2.getSystemId();
            int nid = loc2.getNetworkId();
            int lat = loc2.getBaseStationLatitude();
            int lon = loc2.getBaseStationLongitude();
            this.mLocation.setText("BID = " + (bid == -1 ? "unknown" : Integer.toHexString(bid)) + "   SID = " + (sid == -1 ? "unknown" : Integer.toHexString(sid)) + "   NID = " + (nid == -1 ? "unknown" : Integer.toHexString(nid)) + "\nLAT = " + (lat == -1 ? "unknown" : Integer.toHexString(lat)) + "   LONG = " + (lon == -1 ? "unknown" : Integer.toHexString(lon)));
            return;
        }
        this.mLocation.setText("unknown");
    }

    public final void updateNeighboringCids(ArrayList<NeighboringCellInfo> cids) {
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

    public final void updateCellInfoTv(List<CellInfo> arrayCi) {
        this.mCellInfoValue = arrayCi;
        StringBuilder value = new StringBuilder();
        if (this.mCellInfoValue != null) {
            int index = 0;
            for (CellInfo ci : this.mCellInfoValue) {
                value.append('[');
                value.append(index);
                value.append("]=");
                value.append(ci.toString());
                index++;
                if (index < this.mCellInfoValue.size()) {
                    value.append("\n");
                }
            }
        }
        this.mCellInfo.setText(value.toString());
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

    public final void updateServiceState() {
        ServiceState serviceState = this.mPhoneStateReceiver.getServiceState();
        int state = serviceState.getState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);
        switch (state) {
            case 0:
                display = r.getString(R.string.radioInfo_service_in);
                break;
            case 1:
            case 2:
                display = r.getString(R.string.radioInfo_service_emergency);
                break;
            case 3:
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

    public final void updatePhoneState() {
        PhoneConstants.State state = this.mPhoneStateReceiver.getPhoneState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);
        switch (AnonymousClass24.$SwitchMap$com$android$internal$telephony$PhoneConstants$State[state.ordinal()]) {
            case 1:
                display = r.getString(R.string.radioInfo_phone_idle);
                break;
            case 2:
                display = r.getString(R.string.radioInfo_phone_ringing);
                break;
            case 3:
                display = r.getString(R.string.radioInfo_phone_offhook);
                break;
        }
        this.callState.setText(display);
    }

    static class AnonymousClass24 {
        static final int[] $SwitchMap$com$android$internal$telephony$PhoneConstants$State = new int[PhoneConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$PhoneConstants$State[PhoneConstants.State.IDLE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$PhoneConstants$State[PhoneConstants.State.RINGING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$PhoneConstants$State[PhoneConstants.State.OFFHOOK.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public final void updateDataState() {
        int state = this.mTelephonyManager.getDataState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);
        switch (state) {
            case 0:
                display = r.getString(R.string.radioInfo_data_disconnected);
                break;
            case 1:
                display = r.getString(R.string.radioInfo_data_connecting);
                break;
            case 2:
                display = r.getString(R.string.radioInfo_data_connected);
                break;
            case 3:
                display = r.getString(R.string.radioInfo_data_suspended);
                break;
        }
        this.gprsState.setText(display);
    }

    public final void updateNetworkType() {
        Resources r = getResources();
        String display = SystemProperties.get("gsm.network.type", r.getString(R.string.radioInfo_unknown));
        this.network.setText(display);
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

    public final void updateDataStats() {
        String s = SystemProperties.get("net.gsm.radio-reset", "0");
        this.resets.setText(s);
        String s2 = SystemProperties.get("net.gsm.attempt-gprs", "0");
        this.attempts.setText(s2);
        String s3 = SystemProperties.get("net.gsm.succeed-gprs", "0");
        this.successes.setText(s3);
        String s4 = SystemProperties.get("net.ppp.reset-by-timeout", "0");
        this.sentSinceReceived.setText(s4);
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

    public final void pingIpAddr() {
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 74.125.47.104");
            int status = p.waitFor();
            if (status == 0) {
                this.mPingIpAddrResult = "Pass";
            } else {
                this.mPingIpAddrResult = "Fail: IP addr not reachable";
            }
        } catch (IOException e) {
            this.mPingIpAddrResult = "Fail: IOException";
        } catch (InterruptedException e2) {
            this.mPingIpAddrResult = "Fail: InterruptedException";
        }
    }

    public final void pingHostname() {
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 www.google.com");
            int status = p.waitFor();
            if (status == 0) {
                this.mPingHostnameResult = "Pass";
            } else {
                this.mPingHostnameResult = "Fail: Host unreachable";
            }
        } catch (IOException e) {
            this.mPingHostnameResult = "Fail: IOException";
        } catch (InterruptedException e2) {
            this.mPingHostnameResult = "Fail: InterruptedException";
        } catch (UnknownHostException e3) {
            this.mPingHostnameResult = "Fail: Unknown Host";
        }
    }

    public void httpClientTest() {
        HttpClient client = new DefaultHttpClient();
        try {
            HttpGet request = new HttpGet("http://www.google.com");
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                this.mHttpClientTestResult = "Pass";
            } else {
                this.mHttpClientTestResult = "Fail: Code: " + String.valueOf(response);
            }
            request.abort();
        } catch (IOException e) {
            this.mHttpClientTestResult = "Fail: IOException";
        }
    }

    public void refreshSmsc() {
        this.phone.getSmscAddress(this.mHandler.obtainMessage(1005));
    }

    public final void updatePingState() {
        final Handler handler = new Handler();
        this.mPingIpAddrResult = getResources().getString(R.string.radioInfo_unknown);
        this.mPingHostnameResult = getResources().getString(R.string.radioInfo_unknown);
        this.mHttpClientTestResult = getResources().getString(R.string.radioInfo_unknown);
        this.mPingIpAddr.setText(this.mPingIpAddrResult);
        this.mPingHostname.setText(this.mPingHostnameResult);
        this.mHttpClientTest.setText(this.mHttpClientTestResult);
        final Runnable updatePingResults = new Runnable() {
            @Override
            public void run() {
                RadioInfo.this.mPingIpAddr.setText(RadioInfo.this.mPingIpAddrResult);
                RadioInfo.this.mPingHostname.setText(RadioInfo.this.mPingHostnameResult);
                RadioInfo.this.mHttpClientTest.setText(RadioInfo.this.mHttpClientTestResult);
            }
        };
        Thread ipAddr = new Thread() {
            @Override
            public void run() {
                RadioInfo.this.pingIpAddr();
                handler.post(updatePingResults);
            }
        };
        ipAddr.start();
        Thread hostname = new Thread() {
            @Override
            public void run() {
                RadioInfo.this.pingHostname();
                handler.post(updatePingResults);
            }
        };
        hostname.start();
        Thread httpClient = new Thread() {
            @Override
            public void run() {
                RadioInfo.this.httpClientTest();
                handler.post(updatePingResults);
            }
        };
        httpClient.start();
    }

    public final void updatePdpList() {
        StringBuilder sb = new StringBuilder("========DATA=======\n");
        this.disconnects.setText(sb.toString());
    }

    class CellInfoListRateHandler implements View.OnClickListener {
        int[] rates = {Integer.MAX_VALUE, 0, 1000};
        int index = 0;

        CellInfoListRateHandler() {
        }

        public int getRate() {
            return this.rates[this.index];
        }

        @Override
        public void onClick(View v) {
            this.index++;
            if (this.index >= this.rates.length) {
                this.index = 0;
            }
            RadioInfo.this.phone.setCellInfoListRate(this.rates[this.index]);
            RadioInfo.this.updateCellInfoListRate();
        }
    }

    public boolean isImsRegRequired() {
        return SystemProperties.getBoolean("persist.radio.imsregrequired", false);
    }

    public void updateImsRegRequiredState() {
        log("updateImsRegRequiredState isImsRegRequired()=" + isImsRegRequired());
        String buttonText = isImsRegRequired() ? getString(R.string.ims_reg_required_off) : getString(R.string.ims_reg_required_on);
        this.imsRegRequiredButton.setText(buttonText);
    }

    public boolean isSmsOverImsEnabled() {
        return SystemProperties.getBoolean("persist.radio.imsallowmtsms", false);
    }

    public boolean isImsVoLteProvisioned() {
        if (this.phone == null) {
            return false;
        }
        ImsManager.getInstance(this.phone.getContext(), this.phone.getSubId());
        return ImsManager.isVolteProvisionedOnDevice(this.phone.getContext());
    }

    public void updateImsVoLteProvisionedState() {
        log("updateImsVoLteProvisionedState isImsVoLteProvisioned()=" + isImsVoLteProvisioned());
        String buttonText = isImsVoLteProvisioned() ? getString(R.string.volte_provisioned_flag_off) : getString(R.string.volte_provisioned_flag_on);
        this.imsVoLteProvisionedButton.setText(buttonText);
    }

    public void updateSmsOverImsState() {
        log("updateSmsOverImsState isSmsOverImsEnabled()=" + isSmsOverImsEnabled());
        String buttonText = isSmsOverImsEnabled() ? getString(R.string.sms_over_ims_off) : getString(R.string.sms_over_ims_on);
        this.smsOverImsButton.setText(buttonText);
    }

    public boolean isLteRamDumpEnabled() {
        return SystemProperties.getBoolean("persist.radio.ramdump", false);
    }

    public void updateLteRamDumpState() {
        log("updateLteRamDumpState isLteRamDumpEnabled()=" + isLteRamDumpEnabled());
        String buttonText = isLteRamDumpEnabled() ? getString(R.string.lte_ram_dump_off) : getString(R.string.lte_ram_dump_on);
        this.lteRamDumpButton.setText(buttonText);
    }

    public void log(String s) {
        Log.d("phone", "[RadioInfo] " + s);
    }
}
