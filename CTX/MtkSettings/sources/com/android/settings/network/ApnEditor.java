package com.android.settings.network;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.utils.ThreadUtils;
import com.mediatek.internal.telephony.MtkPhoneConstants;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaApnSetting;
import com.mediatek.settings.ext.IApnSettingsExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ApnEditor extends SettingsPreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener, View.OnKeyListener {
    static final int APN_INDEX = 2;
    static final int CARRIER_ENABLED_INDEX = 17;
    static final int MCC_INDEX = 9;
    static final int MNC_INDEX = 10;
    static final int NAME_INDEX = 1;
    static String sNotSet;
    EditTextPreference mApn;
    ApnData mApnData;
    private IApnSettingsExt mApnExt;
    EditTextPreference mApnType;
    ListPreference mAuthType;
    MultiSelectListPreference mBearerMulti;
    SwitchPreference mCarrierEnabled;
    private Uri mCarrierUri;
    private String mCurMcc;
    private String mCurMnc;
    EditTextPreference mMcc;
    EditTextPreference mMmsPort;
    EditTextPreference mMmsProxy;
    EditTextPreference mMmsc;
    EditTextPreference mMnc;
    EditTextPreference mMvnoMatchData;
    private String mMvnoMatchDataStr;
    ListPreference mMvnoType;
    private String mMvnoTypeStr;
    EditTextPreference mName;
    private boolean mNewApn;
    EditTextPreference mPassword;
    EditTextPreference mPort;
    ListPreference mProtocol;
    EditTextPreference mProxy;
    private boolean mReadOnlyApn;
    private String[] mReadOnlyApnFields;
    private String[] mReadOnlyApnTypes;
    ListPreference mRoamingProtocol;
    EditTextPreference mServer;
    private SimHotSwapHandler mSimHotSwapHandler;
    private int mSubId;
    private TelephonyManager mTelephonyManager;
    EditTextPreference mUser;
    private static final String TAG = ApnEditor.class.getSimpleName();
    private static String[] sProjection = {"_id", "name", "apn", "proxy", "port", "user", "server", "password", "mmsc", "mcc", "mnc", "numeric", "mmsproxy", "mmsport", "authtype", "type", "protocol", "carrier_enabled", "bearer", "bearer_bitmask", "roaming_protocol", "mvno_type", "mvno_match_data", "edited", "user_editable", "sourcetype"};
    private int mBearerInitialVal = 0;
    private int mSourceType = 0;
    private boolean mReadOnlyMode = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                if (intent.getBooleanExtra("state", false)) {
                    Log.d(ApnEditor.TAG, "receiver: ACTION_AIRPLANE_MODE_CHANGED in ApnEditor");
                    ApnEditor.this.exitWithoutSave();
                    return;
                }
                return;
            }
            if (!action.equals("android.intent.action.ANY_DATA_STATE")) {
                if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                    Log.d(ApnEditor.TAG, "receiver: ACTION_SIM_STATE_CHANGED");
                    ApnEditor.this.updateScreenEnableState();
                    return;
                }
                return;
            }
            String stringExtra = intent.getStringExtra("apnType");
            Log.d(ApnEditor.TAG, "Receiver,send MMS status, get type = " + stringExtra);
            if ("mms".equals(stringExtra)) {
                ApnEditor.this.updateScreenEnableState();
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        PersistableBundle config;
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.apn_editor);
        sNotSet = getResources().getString(R.string.apn_not_set);
        this.mName = (EditTextPreference) findPreference("apn_name");
        this.mApn = (EditTextPreference) findPreference("apn_apn");
        this.mProxy = (EditTextPreference) findPreference("apn_http_proxy");
        this.mPort = (EditTextPreference) findPreference("apn_http_port");
        this.mUser = (EditTextPreference) findPreference("apn_user");
        this.mServer = (EditTextPreference) findPreference("apn_server");
        this.mPassword = (EditTextPreference) findPreference("apn_password");
        this.mMmsProxy = (EditTextPreference) findPreference("apn_mms_proxy");
        this.mMmsPort = (EditTextPreference) findPreference("apn_mms_port");
        this.mMmsc = (EditTextPreference) findPreference("apn_mmsc");
        this.mMcc = (EditTextPreference) findPreference("apn_mcc");
        this.mMnc = (EditTextPreference) findPreference("apn_mnc");
        this.mApnType = (EditTextPreference) findPreference("apn_type");
        this.mAuthType = (ListPreference) findPreference("auth_type");
        this.mAuthType.setOnPreferenceChangeListener(this);
        this.mProtocol = (ListPreference) findPreference("apn_protocol");
        this.mProtocol.setOnPreferenceChangeListener(this);
        this.mRoamingProtocol = (ListPreference) findPreference("apn_roaming_protocol");
        this.mRoamingProtocol.setOnPreferenceChangeListener(this);
        this.mCarrierEnabled = (SwitchPreference) findPreference("carrier_enabled");
        this.mBearerMulti = (MultiSelectListPreference) findPreference("bearer_multi");
        this.mBearerMulti.setOnPreferenceChangeListener(this);
        this.mBearerMulti.setPositiveButtonText(android.R.string.ok);
        this.mBearerMulti.setNegativeButtonText(android.R.string.cancel);
        this.mMvnoType = (ListPreference) findPreference("mvno_type");
        this.mMvnoType.setOnPreferenceChangeListener(this);
        this.mMvnoMatchData = (EditTextPreference) findPreference("mvno_match_data");
        Intent intent = getIntent();
        String action = intent.getAction();
        this.mSubId = intent.getIntExtra("sub_id", -1);
        this.mReadOnlyApn = false;
        Uri data = null;
        this.mReadOnlyApnTypes = null;
        this.mReadOnlyApnFields = null;
        this.mApnExt = UtilsExt.getApnSettingsExt(getContext());
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) getSystemService("carrier_config");
        if (carrierConfigManager != null && (config = carrierConfigManager.getConfig()) != null) {
            this.mReadOnlyApnTypes = config.getStringArray("read_only_apn_types_string_array");
            if (!ArrayUtils.isEmpty(this.mReadOnlyApnTypes)) {
                for (String str : this.mReadOnlyApnTypes) {
                    Log.d(TAG, "onCreate: read only APN type: " + str);
                }
            }
            this.mReadOnlyApnFields = config.getStringArray("read_only_apn_fields_string_array");
        }
        if (action.equals("android.intent.action.EDIT")) {
            data = intent.getData();
            if (data.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
                this.mReadOnlyMode = intent.getBooleanExtra("readOnly", false);
                Log.d(TAG, "Read only mode : " + this.mReadOnlyMode);
            } else {
                Log.e(TAG, "Edit request not for carrier table. Uri: " + data);
                finish();
                return;
            }
        } else if (action.equals("android.intent.action.INSERT")) {
            this.mCarrierUri = intent.getData();
            if (!this.mCarrierUri.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
                Log.e(TAG, "Insert request not for carrier table. Uri: " + this.mCarrierUri);
                finish();
                return;
            }
            this.mNewApn = true;
            this.mMvnoTypeStr = intent.getStringExtra("mvno_type");
            this.mMvnoMatchDataStr = intent.getStringExtra("mvno_match_data");
            Log.d(TAG, "mvnoType = " + this.mMvnoTypeStr + ", mvnoMatchData =" + this.mMvnoMatchDataStr);
        } else {
            finish();
            return;
        }
        sProjection = this.mApnExt.customizeApnProjection(sProjection);
        this.mApnExt.customizePreference(this.mSubId, getPreferenceScreen());
        if (data != null) {
            this.mApnData = getApnDataFromUri(data);
            Log.d(TAG, "uri = " + data);
        } else {
            this.mApnData = new ApnData(sProjection.length);
            Log.d(TAG, "sProjection.length = " + sProjection.length);
        }
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        boolean z = this.mApnData.getInteger(23, 1).intValue() == 1;
        Log.d(TAG, "onCreate: EDITED " + z);
        if (!z && (this.mApnData.getInteger(24, 1).intValue() == 0 || apnTypesMatch(this.mReadOnlyApnTypes, this.mApnData.getString(15)))) {
            Log.d(TAG, "onCreate: apnTypesMatch; read-only APN");
            this.mReadOnlyApn = true;
            disableAllFields();
        } else if (!ArrayUtils.isEmpty(this.mReadOnlyApnFields)) {
            disableFields(this.mReadOnlyApnFields);
        }
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            getPreferenceScreen().getPreference(i).setOnPreferenceChangeListener(this);
        }
        fillUI(bundle == null);
        this.mSimHotSwapHandler = new SimHotSwapHandler(getContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                Log.d(ApnEditor.TAG, "onSimHotSwap, finish Activity~~");
                ApnEditor.this.finish();
            }
        });
        IntentFilter intentFilter = new IntentFilter("android.intent.action.ANY_DATA_STATE");
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        getContext().registerReceiver(this.mReceiver, intentFilter);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    static String formatInteger(String str) {
        try {
            return String.format("%d", Integer.valueOf(Integer.parseInt(str)));
        } catch (NumberFormatException e) {
            return str;
        }
    }

    static boolean hasAllApns(String[] strArr) {
        if (ArrayUtils.isEmpty(strArr)) {
            return false;
        }
        List listAsList = Arrays.asList(strArr);
        if (listAsList.contains("*")) {
            Log.d(TAG, "hasAllApns: true because apnList.contains(PhoneConstants.APN_TYPE_ALL)");
            return true;
        }
        for (String str : PhoneConstants.APN_TYPES) {
            if (!listAsList.contains(str)) {
                return false;
            }
        }
        Log.d(TAG, "hasAllApns: true");
        return true;
    }

    private boolean apnTypesMatch(String[] strArr, String str) {
        if (ArrayUtils.isEmpty(strArr)) {
            return false;
        }
        if (hasAllApns(strArr) || TextUtils.isEmpty(str)) {
            return true;
        }
        List listAsList = Arrays.asList(strArr);
        for (String str2 : str.split(",")) {
            if (listAsList.contains(str2.trim())) {
                Log.d(TAG, "apnTypesMatch: true because match found for " + str2.trim());
                return true;
            }
        }
        Log.d(TAG, "apnTypesMatch: false");
        return false;
    }

    private Preference getPreferenceFromFieldName(String str) {
        switch (str) {
            case "name":
                return this.mName;
            case "apn":
                return this.mApn;
            case "proxy":
                return this.mProxy;
            case "port":
                return this.mPort;
            case "user":
                return this.mUser;
            case "server":
                return this.mServer;
            case "password":
                return this.mPassword;
            case "mmsproxy":
                return this.mMmsProxy;
            case "mmsport":
                return this.mMmsPort;
            case "mmsc":
                return this.mMmsc;
            case "mcc":
                return this.mMcc;
            case "mnc":
                return this.mMnc;
            case "type":
                return this.mApnType;
            case "authtype":
                return this.mAuthType;
            case "protocol":
                return this.mProtocol;
            case "roaming_protocol":
                return this.mRoamingProtocol;
            case "carrier_enabled":
                return this.mCarrierEnabled;
            case "bearer":
            case "bearer_bitmask":
                return this.mBearerMulti;
            case "mvno_type":
                return this.mMvnoType;
            case "mvno_match_data":
                return this.mMvnoMatchData;
            default:
                return null;
        }
    }

    private void disableFields(String[] strArr) {
        for (String str : strArr) {
            Preference preferenceFromFieldName = getPreferenceFromFieldName(str);
            if (preferenceFromFieldName != null) {
                preferenceFromFieldName.setEnabled(false);
            }
        }
    }

    private void disableAllFields() {
        this.mName.setEnabled(false);
        this.mApn.setEnabled(false);
        this.mProxy.setEnabled(false);
        this.mPort.setEnabled(false);
        this.mUser.setEnabled(false);
        this.mServer.setEnabled(false);
        this.mPassword.setEnabled(false);
        this.mMmsProxy.setEnabled(false);
        this.mMmsPort.setEnabled(false);
        this.mMmsc.setEnabled(false);
        this.mMcc.setEnabled(false);
        this.mMnc.setEnabled(false);
        this.mApnType.setEnabled(false);
        this.mAuthType.setEnabled(false);
        this.mProtocol.setEnabled(false);
        this.mRoamingProtocol.setEnabled(false);
        this.mCarrierEnabled.setEnabled(false);
        this.mBearerMulti.setEnabled(false);
        this.mMvnoType.setEnabled(false);
        this.mMvnoMatchData.setEnabled(false);
    }

    @Override
    public int getMetricsCategory() {
        return 13;
    }

    void fillUI(boolean z) {
        Log.d(TAG, "fillUi... firstTime = " + z);
        if (z) {
            this.mName.setText(this.mApnData.getString(1));
            this.mApn.setText(this.mApnData.getString(2));
            this.mProxy.setText(this.mApnData.getString(3));
            this.mPort.setText(this.mApnData.getString(4));
            this.mUser.setText(this.mApnData.getString(5));
            this.mServer.setText(this.mApnData.getString(6));
            this.mPassword.setText(this.mApnData.getString(7));
            this.mMmsProxy.setText(this.mApnData.getString(12));
            this.mMmsPort.setText(this.mApnData.getString(13));
            this.mMmsc.setText(this.mApnData.getString(8));
            this.mMcc.setText(this.mApnData.getString(MCC_INDEX));
            this.mMnc.setText(this.mApnData.getString(10));
            this.mApnType.setText(this.mApnData.getString(15));
            if (this.mNewApn) {
                String simOperator = this.mTelephonyManager.getSimOperator(this.mSubId);
                Log.d(TAG, " fillUi, numeric = " + simOperator);
                String strUpdateMccMncForCdma = CdmaApnSetting.updateMccMncForCdma(simOperator, this.mSubId);
                if (strUpdateMccMncForCdma != null && strUpdateMccMncForCdma.length() > 4) {
                    String strSubstring = strUpdateMccMncForCdma.substring(0, 3);
                    String strSubstring2 = strUpdateMccMncForCdma.substring(3);
                    this.mMcc.setText(strSubstring);
                    this.mMnc.setText(strSubstring2);
                    this.mCurMnc = strSubstring2;
                    this.mCurMcc = strSubstring;
                }
                this.mSourceType = 1;
            } else {
                this.mSourceType = this.mApnData.getInteger(25).intValue();
            }
            int iIntValue = this.mApnData.getInteger(14, -1).intValue();
            if (iIntValue != -1) {
                this.mAuthType.setValueIndex(iIntValue);
            } else {
                this.mAuthType.setValue(null);
            }
            this.mProtocol.setValue(this.mApnData.getString(16));
            this.mRoamingProtocol.setValue(this.mApnData.getString(20));
            this.mCarrierEnabled.setChecked(this.mApnData.getInteger(CARRIER_ENABLED_INDEX, 1).intValue() == 1);
            this.mBearerInitialVal = this.mApnData.getInteger(18, 0).intValue();
            HashSet hashSet = new HashSet();
            int iIntValue2 = this.mApnData.getInteger(19, 0).intValue();
            if (iIntValue2 == 0) {
                if (this.mBearerInitialVal == 0) {
                    hashSet.add("0");
                }
            } else {
                int i = 1;
                while (iIntValue2 != 0) {
                    if ((iIntValue2 & 1) == 1) {
                        hashSet.add("" + i);
                    }
                    iIntValue2 >>= 1;
                    i++;
                }
            }
            if (this.mBearerInitialVal != 0) {
                if (!hashSet.contains("" + this.mBearerInitialVal)) {
                    hashSet.add("" + this.mBearerInitialVal);
                }
            }
            this.mBearerMulti.setValues(hashSet);
            this.mMvnoType.setValue(this.mApnData.getString(21));
            this.mMvnoMatchData.setEnabled(false);
            this.mMvnoMatchData.setText(this.mApnData.getString(22));
            if (this.mNewApn && this.mMvnoTypeStr != null && this.mMvnoMatchDataStr != null) {
                this.mMvnoType.setValue(this.mMvnoTypeStr);
                this.mMvnoMatchData.setText(this.mMvnoMatchDataStr);
            }
        }
        this.mName.setSummary(checkNull(this.mName.getText()));
        this.mApn.setSummary(checkNull(this.mApn.getText()));
        this.mProxy.setSummary(checkNull(this.mProxy.getText()));
        this.mPort.setSummary(checkNull(this.mPort.getText()));
        this.mUser.setSummary(checkNull(this.mUser.getText()));
        this.mServer.setSummary(checkNull(this.mServer.getText()));
        this.mPassword.setSummary(starify(this.mPassword.getText()));
        this.mMmsProxy.setSummary(checkNull(this.mMmsProxy.getText()));
        this.mMmsPort.setSummary(checkNull(this.mMmsPort.getText()));
        this.mMmsc.setSummary(checkNull(this.mMmsc.getText()));
        this.mMcc.setSummary(formatInteger(checkNull(this.mMcc.getText())));
        this.mMnc.setSummary(formatInteger(checkNull(this.mMnc.getText())));
        this.mApnType.setSummary(checkNull(this.mApnType.getText()));
        String value = this.mAuthType.getValue();
        if (value != null) {
            int i2 = Integer.parseInt(value);
            this.mAuthType.setValueIndex(i2);
            this.mAuthType.setSummary(getResources().getStringArray(R.array.apn_auth_entries)[i2]);
        } else {
            this.mAuthType.setSummary(sNotSet);
        }
        this.mProtocol.setSummary(checkNull(protocolDescription(this.mProtocol.getValue(), this.mProtocol)));
        this.mRoamingProtocol.setSummary(checkNull(protocolDescription(this.mRoamingProtocol.getValue(), this.mRoamingProtocol)));
        this.mBearerMulti.setSummary(checkNull(bearerMultiDescription(this.mBearerMulti.getValues())));
        this.mMvnoType.setSummary(checkNull(mvnoDescription(this.mMvnoType.getValue())));
        this.mMvnoMatchData.setSummary(checkNull(this.mMvnoMatchData.getText()));
        if (!getResources().getBoolean(R.bool.config_allow_edit_carrier_enabled)) {
            this.mCarrierEnabled.setEnabled(false);
        } else {
            this.mCarrierEnabled.setEnabled(true);
        }
    }

    private String protocolDescription(String str, ListPreference listPreference) {
        int iFindIndexOfValue = listPreference.findIndexOfValue(str);
        if (iFindIndexOfValue == -1) {
            return null;
        }
        try {
            return getResources().getStringArray(R.array.apn_protocol_entries)[iFindIndexOfValue];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    private String bearerMultiDescription(Set<String> set) {
        String[] stringArray = getResources().getStringArray(R.array.bearer_entries);
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = set.iterator();
        boolean z = true;
        while (it.hasNext()) {
            int iFindIndexOfValue = this.mBearerMulti.findIndexOfValue(it.next());
            if (z) {
                try {
                    sb.append(stringArray[iFindIndexOfValue]);
                    z = false;
                } catch (ArrayIndexOutOfBoundsException e) {
                }
            } else {
                sb.append(", " + stringArray[iFindIndexOfValue]);
            }
        }
        String string = sb.toString();
        if (!TextUtils.isEmpty(string)) {
            return string;
        }
        return null;
    }

    private String mvnoDescription(String str) {
        int iFindIndexOfValue = this.mMvnoType.findIndexOfValue(str);
        String value = this.mMvnoType.getValue();
        if (iFindIndexOfValue == -1) {
            return null;
        }
        String[] stringArray = getResources().getStringArray(R.array.ext_mvno_type_entries);
        this.mMvnoMatchData.setEnabled(((this.mReadOnlyApn || (this.mReadOnlyApnFields != null && Arrays.asList(this.mReadOnlyApnFields).contains("mvno_match_data"))) || iFindIndexOfValue == 0) ? false : true);
        if (str != null && !str.equals(value)) {
            if (stringArray[iFindIndexOfValue].equals("SPN")) {
                this.mMvnoMatchData.setText(this.mTelephonyManager.getSimOperatorName());
            } else if (stringArray[iFindIndexOfValue].equals("IMSI")) {
                String simOperator = this.mTelephonyManager.getSimOperator(this.mSubId);
                this.mMvnoMatchData.setText(simOperator + "x");
            } else if (stringArray[iFindIndexOfValue].equals("GID")) {
                this.mMvnoMatchData.setText(this.mTelephonyManager.getGroupIdLevel1());
            }
        }
        try {
            return stringArray[iFindIndexOfValue];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        String key = preference.getKey();
        if ("auth_type".equals(key)) {
            try {
                int i = Integer.parseInt((String) obj);
                this.mAuthType.setValueIndex(i);
                this.mAuthType.setSummary(getResources().getStringArray(R.array.apn_auth_entries)[i]);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if ("apn_protocol".equals(key)) {
            String str = (String) obj;
            String strProtocolDescription = protocolDescription(str, this.mProtocol);
            if (strProtocolDescription == null) {
                return false;
            }
            this.mProtocol.setSummary(strProtocolDescription);
            this.mProtocol.setValue(str);
            return true;
        }
        if ("apn_roaming_protocol".equals(key)) {
            String str2 = (String) obj;
            String strProtocolDescription2 = protocolDescription(str2, this.mRoamingProtocol);
            if (strProtocolDescription2 == null) {
                return false;
            }
            this.mRoamingProtocol.setSummary(strProtocolDescription2);
            this.mRoamingProtocol.setValue(str2);
            return true;
        }
        if ("bearer_multi".equals(key)) {
            Set<String> set = (Set) obj;
            String strBearerMultiDescription = bearerMultiDescription(set);
            if (strBearerMultiDescription == null) {
                return false;
            }
            this.mBearerMulti.setValues(set);
            this.mBearerMulti.setSummary(strBearerMultiDescription);
            return true;
        }
        if ("mvno_type".equals(key)) {
            String str3 = (String) obj;
            String strMvnoDescription = mvnoDescription(str3);
            if (strMvnoDescription == null) {
                return false;
            }
            this.mMvnoType.setValue(str3);
            this.mMvnoType.setSummary(strMvnoDescription);
            return true;
        }
        if (!preference.equals(this.mPassword)) {
            if (preference.equals(this.mCarrierEnabled) || preference.equals(this.mBearerMulti)) {
                return true;
            }
            preference.setSummary(checkNull(obj != null ? String.valueOf(obj) : null));
            return true;
        }
        preference.setSummary(starify(obj != null ? String.valueOf(obj) : ""));
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        Log.d(TAG, "onCreateOptionsMenu mReadOnlyMode = " + this.mReadOnlyMode);
        if (this.mReadOnlyMode) {
            return;
        }
        if (!this.mNewApn && !this.mReadOnlyApn && this.mSourceType != 0) {
            menu.add(0, 1, 0, R.string.menu_delete).setIcon(R.drawable.ic_delete);
        }
        menu.add(0, 2, 0, R.string.menu_save).setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, 3, 0, R.string.menu_cancel).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case 1:
                deleteApn();
                finish();
                return true;
            case 2:
                if (this.mSourceType == 0) {
                    showDialog(1);
                } else if (validateAndSaveApnData()) {
                    finish();
                }
                return true;
            case 3:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        view.setOnKeyListener(this);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (keyEvent.getAction() != 0 || i != 4) {
            return false;
        }
        if (validateAndSaveApnData()) {
            finish();
            return true;
        }
        return true;
    }

    boolean setStringValueAndCheckIfDiff(ContentValues contentValues, String str, String str2, boolean z, int i) {
        String string = this.mApnData.getString(i);
        boolean z2 = z || (!(TextUtils.isEmpty(str2) && TextUtils.isEmpty(string)) && (str2 == null || !str2.equals(string)));
        if (z2 && str2 != null) {
            contentValues.put(str, str2);
        }
        return z2;
    }

    boolean setIntValueAndCheckIfDiff(ContentValues contentValues, String str, int i, boolean z, int i2) {
        boolean z2 = z || i != this.mApnData.getInteger(i2).intValue();
        if (z2) {
            contentValues.put(str, Integer.valueOf(i));
        }
        return z2;
    }

    boolean validateAndSaveApnData() {
        int i;
        int i2;
        Log.d(TAG, "validateAndSave...");
        if (this.mReadOnlyApn) {
            return true;
        }
        String strCheckNotSet = checkNotSet(this.mName.getText());
        String strCheckNotSet2 = checkNotSet(this.mApn.getText());
        String strCheckNotSet3 = checkNotSet(this.mMcc.getText());
        String strCheckNotSet4 = checkNotSet(this.mMnc.getText());
        if (validateApnData() != null) {
            showError();
            return false;
        }
        ContentValues contentValues = new ContentValues();
        boolean stringValueAndCheckIfDiff = setStringValueAndCheckIfDiff(contentValues, "mmsc", checkNotSet(this.mMmsc.getText()), setStringValueAndCheckIfDiff(contentValues, "password", checkNotSet(this.mPassword.getText()), setStringValueAndCheckIfDiff(contentValues, "server", checkNotSet(this.mServer.getText()), setStringValueAndCheckIfDiff(contentValues, "user", checkNotSet(this.mUser.getText()), setStringValueAndCheckIfDiff(contentValues, "mmsport", checkNotSet(this.mMmsPort.getText()), setStringValueAndCheckIfDiff(contentValues, "mmsproxy", checkNotSet(this.mMmsProxy.getText()), setStringValueAndCheckIfDiff(contentValues, "port", checkNotSet(this.mPort.getText()), setStringValueAndCheckIfDiff(contentValues, "proxy", checkNotSet(this.mProxy.getText()), setStringValueAndCheckIfDiff(contentValues, "apn", strCheckNotSet2, setStringValueAndCheckIfDiff(contentValues, "name", strCheckNotSet, this.mNewApn, 1), 2), 3), 4), 12), 13), 5), 6), 7), 8);
        String value = this.mAuthType.getValue();
        if (value != null) {
            stringValueAndCheckIfDiff = setIntValueAndCheckIfDiff(contentValues, "authtype", Integer.parseInt(value), stringValueAndCheckIfDiff, 14);
        }
        boolean stringValueAndCheckIfDiff2 = setStringValueAndCheckIfDiff(contentValues, "mnc", strCheckNotSet4, setStringValueAndCheckIfDiff(contentValues, "mcc", strCheckNotSet3, setStringValueAndCheckIfDiff(contentValues, "type", checkNotSet(getUserEnteredApnType()), setStringValueAndCheckIfDiff(contentValues, "roaming_protocol", checkNotSet(this.mRoamingProtocol.getValue()), setStringValueAndCheckIfDiff(contentValues, "protocol", checkNotSet(this.mProtocol.getValue()), stringValueAndCheckIfDiff, 16), 20), 15), MCC_INDEX), 10);
        contentValues.put("numeric", strCheckNotSet3 + strCheckNotSet4);
        if (this.mCurMnc != null && this.mCurMcc != null && this.mCurMnc.equals(strCheckNotSet4) && this.mCurMcc.equals(strCheckNotSet3)) {
            contentValues.put("current", (Integer) 1);
        }
        Iterator<String> it = this.mBearerMulti.getValues().iterator();
        int bitmaskForTech = 0;
        while (true) {
            if (it.hasNext()) {
                String next = it.next();
                if (Integer.parseInt(next) != 0) {
                    bitmaskForTech |= ServiceState.getBitmaskForTech(Integer.parseInt(next));
                } else {
                    i = 0;
                    break;
                }
            } else {
                i = bitmaskForTech;
                break;
            }
        }
        boolean intValueAndCheckIfDiff = setIntValueAndCheckIfDiff(contentValues, "bearer_bitmask", i, stringValueAndCheckIfDiff2, 19);
        if (i != 0 && this.mBearerInitialVal != 0 && ServiceState.bitmaskHasTech(i, this.mBearerInitialVal)) {
            i2 = this.mBearerInitialVal;
        } else {
            i2 = 0;
        }
        boolean intValueAndCheckIfDiff2 = setIntValueAndCheckIfDiff(contentValues, "carrier_enabled", this.mCarrierEnabled.isChecked() ? 1 : 0, setStringValueAndCheckIfDiff(contentValues, "mvno_match_data", checkNotSet(this.mMvnoMatchData.getText()), setStringValueAndCheckIfDiff(contentValues, "mvno_type", checkNotSet(this.mMvnoType.getValue()), setIntValueAndCheckIfDiff(contentValues, "bearer", i2, intValueAndCheckIfDiff, 18), 21), 22), CARRIER_ENABLED_INDEX);
        contentValues.put("edited", (Integer) 1);
        contentValues.put("sourcetype", Integer.valueOf(this.mSourceType));
        this.mApnExt.saveApnValues(contentValues);
        Log.d(TAG, "Save apn " + contentValues.getAsString("apn"));
        if (intValueAndCheckIfDiff2) {
            updateApnDataToDatabase(this.mApnData.getUri() == null ? this.mCarrierUri : this.mApnData.getUri(), contentValues);
        }
        return true;
    }

    private void updateApnDataToDatabase(final Uri uri, final ContentValues contentValues) {
        ThreadUtils.postOnBackgroundThread(new Runnable() {
            @Override
            public final void run() {
                ApnEditor.lambda$updateApnDataToDatabase$0(this.f$0, uri, contentValues);
            }
        });
    }

    public static void lambda$updateApnDataToDatabase$0(ApnEditor apnEditor, Uri uri, ContentValues contentValues) {
        if (uri.equals(apnEditor.mCarrierUri)) {
            if (apnEditor.getContentResolver().insert(apnEditor.mCarrierUri, contentValues) == null) {
                Log.e(TAG, "Can't add a new apn to database " + apnEditor.mCarrierUri);
                return;
            }
            return;
        }
        apnEditor.getContentResolver().update(uri, contentValues, null, null);
    }

    String validateApnData() {
        String string;
        String strCheckNotSet = checkNotSet(this.mName.getText());
        String strCheckNotSet2 = checkNotSet(this.mApn.getText());
        String strCheckNotSet3 = checkNotSet(this.mMcc.getText());
        String strCheckNotSet4 = checkNotSet(this.mMnc.getText());
        String text = this.mApnType.getText();
        if (TextUtils.isEmpty(strCheckNotSet)) {
            string = getResources().getString(R.string.error_name_empty);
        } else if (TextUtils.isEmpty(strCheckNotSet2) && (text == null || !text.contains("ia"))) {
            string = getResources().getString(R.string.error_apn_empty);
        } else if (strCheckNotSet3 == null || strCheckNotSet3.length() != 3) {
            string = getResources().getString(R.string.error_mcc_not3);
        } else if (strCheckNotSet4 == null || (strCheckNotSet4.length() & 65534) != 2) {
            string = getResources().getString(R.string.error_mnc_not23);
        } else {
            string = null;
        }
        if (string == null && !ArrayUtils.isEmpty(this.mReadOnlyApnTypes) && apnTypesMatch(this.mReadOnlyApnTypes, getUserEnteredApnType())) {
            StringBuilder sb = new StringBuilder();
            for (String str : this.mReadOnlyApnTypes) {
                sb.append(str);
                sb.append(", ");
                Log.d(TAG, "validateApnData: appending type: " + str);
            }
            if (sb.length() >= 2) {
                sb.delete(sb.length() - 2, sb.length());
            }
            return String.format(getResources().getString(R.string.error_adding_apn_type), sb);
        }
        return string;
    }

    @Override
    public Dialog onCreateDialog(int i) {
        if (i == 1) {
            return new AlertDialog.Builder(getContext()).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.error_title).setMessage(getString(R.string.apn_predefine_change_dialog_notice)).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    if (ApnEditor.this.validateAndSaveApnData()) {
                        ApnEditor.this.finish();
                    }
                }
            }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create();
        }
        return super.onCreateDialog(i);
    }

    @Override
    public int getDialogMetricsCategory(int i) {
        if (i == 1) {
            return 530;
        }
        return 0;
    }

    void showError() {
        ErrorDialog.showError(this);
    }

    private void deleteApn() {
        if (this.mApnData.getUri() != null) {
            getContentResolver().delete(this.mApnData.getUri(), null, null);
            this.mApnData = new ApnData(sProjection.length);
        }
    }

    private String starify(String str) {
        if (str == null || str.length() == 0) {
            return sNotSet;
        }
        char[] cArr = new char[str.length()];
        for (int i = 0; i < cArr.length; i++) {
            cArr[i] = '*';
        }
        return new String(cArr);
    }

    private String checkNull(String str) {
        return TextUtils.isEmpty(str) ? sNotSet : str;
    }

    private String checkNotSet(String str) {
        if (sNotSet.equals(str)) {
            return null;
        }
        return str;
    }

    private String getUserEnteredApnType() {
        String text = this.mApnType.getText();
        if (text != null) {
            text = text.trim();
        }
        if ((TextUtils.isEmpty(text) || "*".equals(text)) && !ArrayUtils.isEmpty(this.mReadOnlyApnTypes)) {
            StringBuilder sb = new StringBuilder();
            List listAsList = Arrays.asList(this.mReadOnlyApnTypes);
            boolean z = true;
            for (String str : MtkPhoneConstants.MTK_APN_TYPES) {
                if ((!TextUtils.isEmpty(text) || !str.equals("ims")) && !listAsList.contains(str) && !str.equals("ia") && !str.equals("emergency")) {
                    if (!z) {
                        sb.append(",");
                    } else {
                        z = false;
                    }
                    sb.append(str);
                }
            }
            String string = sb.toString();
            Log.d(TAG, "getUserEnteredApnType: changed apn type to editable apn types: " + string);
            return string;
        }
        return text;
    }

    public static class ErrorDialog extends InstrumentedDialogFragment {
        public static void showError(ApnEditor apnEditor) {
            ErrorDialog errorDialog = new ErrorDialog();
            errorDialog.setTargetFragment(apnEditor, 0);
            errorDialog.show(apnEditor.getFragmentManager(), "error");
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getContext()).setTitle(R.string.error_title).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).setMessage(((ApnEditor) getTargetFragment()).validateApnData()).create();
        }

        @Override
        public int getMetricsCategory() {
            return 530;
        }
    }

    ApnData getApnDataFromUri(Uri uri) {
        Cursor cursorQuery = getContentResolver().query(uri, sProjection, null, null, null);
        ApnData apnData = null;
        th = null;
        Throwable th = null;
        if (cursorQuery != null) {
            try {
                try {
                    cursorQuery.moveToFirst();
                    apnData = new ApnData(uri, cursorQuery);
                } finally {
                }
            } catch (Throwable th2) {
                if (cursorQuery != null) {
                    if (th != null) {
                        try {
                            cursorQuery.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        cursorQuery.close();
                    }
                }
                throw th2;
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        if (apnData == null) {
            Log.d(TAG, "Can't get apnData from Uri " + uri);
        }
        return apnData;
    }

    static class ApnData {
        Object[] mData;
        Uri mUri;

        ApnData(int i) {
            this.mData = new Object[i];
        }

        ApnData(Uri uri, Cursor cursor) {
            this.mUri = uri;
            this.mData = new Object[cursor.getColumnCount()];
            for (int i = 0; i < this.mData.length; i++) {
                switch (cursor.getType(i)) {
                    case 1:
                        this.mData[i] = Integer.valueOf(cursor.getInt(i));
                        break;
                    case 2:
                        this.mData[i] = Float.valueOf(cursor.getFloat(i));
                        break;
                    case 3:
                        this.mData[i] = cursor.getString(i);
                        break;
                    case 4:
                        this.mData[i] = cursor.getBlob(i);
                        break;
                    default:
                        this.mData[i] = null;
                        break;
                }
            }
        }

        Uri getUri() {
            return this.mUri;
        }

        Integer getInteger(int i) {
            return (Integer) this.mData[i];
        }

        Integer getInteger(int i, Integer num) {
            Integer integer = getInteger(i);
            return integer == null ? num : integer;
        }

        String getString(int i) {
            return (String) this.mData[i];
        }
    }

    @Override
    public void onDestroy() {
        if (this.mSimHotSwapHandler != null) {
            this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        }
        getContext().unregisterReceiver(this.mReceiver);
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
        this.mApnExt.onDestroy();
    }

    public void exitWithoutSave() {
        if (this.mNewApn && this.mApnData.getUri() != null) {
            getContentResolver().delete(this.mApnData.getUri(), null, null);
        }
        finish();
    }

    public void updateScreenEnableState() {
        boolean zIsSimReadyAndRadioOn = isSimReadyAndRadioOn();
        Log.d(TAG, "enable = " + zIsSimReadyAndRadioOn + " mReadOnlyMode = " + this.mReadOnlyMode);
        getPreferenceScreen().setEnabled(zIsSimReadyAndRadioOn && !this.mReadOnlyMode && this.mApnExt.getScreenEnableState(this.mSubId, getActivity()));
        this.mApnExt.setApnTypePreferenceState(this.mApnType, this.mApnType.getText());
        this.mApnExt.updateFieldsStatus(this.mSubId, this.mSourceType, getPreferenceScreen(), this.mApn.getText());
        this.mApnExt.setMvnoPreferenceState(this.mMvnoType, this.mMvnoMatchData);
    }

    private boolean isSimReadyAndRadioOn() {
        boolean z = false;
        boolean z2 = 5 == TelephonyManager.getDefault().getSimState(SubscriptionManager.getSlotIndex(this.mSubId));
        boolean z3 = Settings.System.getInt(getContentResolver(), "airplane_mode_on", -1) == 1;
        if (!z3 && z2) {
            z = true;
        }
        Log.d(TAG, "isSimReadyAndRadioOn(), subId = " + this.mSubId + " ,airplaneModeEnabled = " + z3 + " ,simReady = " + z2);
        return z;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        Preference preferenceFindPreference = findPreference(str);
        if (preferenceFindPreference != null) {
            if (preferenceFindPreference.equals(this.mCarrierEnabled)) {
                preferenceFindPreference.setSummary(checkNull(String.valueOf(sharedPreferences.getBoolean(str, true))));
                return;
            }
            if (preferenceFindPreference.equals(this.mPort)) {
                String string = sharedPreferences.getString(str, "");
                if (!string.equals("")) {
                    try {
                        int i = Integer.parseInt(string);
                        if (i > 65535 || i <= 0) {
                            Toast.makeText(getContext(), getString(R.string.apn_port_warning), 1).show();
                            ((EditTextPreference) preferenceFindPreference).setText("");
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), getString(R.string.apn_port_warning), 1).show();
                        ((EditTextPreference) preferenceFindPreference).setText("");
                    }
                }
                preferenceFindPreference.setSummary(checkNull(sharedPreferences.getString(str, "")));
                return;
            }
            preferenceFindPreference.setSummary(checkNull(sharedPreferences.getString(str, "")));
        }
    }
}
