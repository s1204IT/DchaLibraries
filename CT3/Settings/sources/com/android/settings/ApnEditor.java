package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
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
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaApnSetting;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.IApnSettingsExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ApnEditor extends SettingsPreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener, View.OnKeyListener {
    private static String sNotSet;
    private EditTextPreference mApn;
    private IApnSettingsExt mApnExt;
    private EditTextPreference mApnType;
    private ListPreference mAuthType;
    private MultiSelectListPreference mBearerMulti;
    private SwitchPreference mCarrierEnabled;
    private String mCurMcc;
    private String mCurMnc;
    private Cursor mCursor;
    private boolean mFirstTime;
    private EditTextPreference mMcc;
    private EditTextPreference mMmsPort;
    private EditTextPreference mMmsProxy;
    private EditTextPreference mMmsc;
    private EditTextPreference mMnc;
    private EditTextPreference mMvnoMatchData;
    private String mMvnoMatchDataStr;
    private ListPreference mMvnoType;
    private String mMvnoTypeStr;
    private EditTextPreference mName;
    private boolean mNewApn;
    private EditTextPreference mPassword;
    private EditTextPreference mPort;
    private ListPreference mProtocol;
    private EditTextPreference mProxy;
    private Resources mRes;
    private ListPreference mRoamingProtocol;
    private EditTextPreference mServer;
    private SimHotSwapHandler mSimHotSwapHandler;
    private int mSubId;
    private TelephonyManager mTelephonyManager;
    private Uri mUri;
    private EditTextPreference mUser;
    private static final String TAG = ApnEditor.class.getSimpleName();
    private static String[] sProjection = {"_id", "name", "apn", "proxy", "port", "user", "server", "password", "mmsc", "mcc", "mnc", "numeric", "mmsproxy", "mmsport", "authtype", "type", "protocol", "carrier_enabled", "bearer", "bearer_bitmask", "roaming_protocol", "mvno_type", "mvno_match_data", "sourcetype"};
    private int mBearerInitialVal = 0;
    private int mSourceType = 0;
    private boolean mReadOnlyMode = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                boolean airplaneModeEnabled = intent.getBooleanExtra("state", false);
                if (!airplaneModeEnabled) {
                    return;
                }
                Log.d(ApnEditor.TAG, "receiver: ACTION_AIRPLANE_MODE_CHANGED in ApnEditor");
                ApnEditor.this.exitWithoutSave();
                return;
            }
            if (action.equals("android.intent.action.ANY_DATA_STATE")) {
                String apnType = intent.getStringExtra("apnType");
                Log.d(ApnEditor.TAG, "Receiver,send MMS status, get type = " + apnType);
                if (!"mms".equals(apnType)) {
                    return;
                }
                ApnEditor.this.updateScreenEnableState();
                return;
            }
            if (!action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                return;
            }
            Log.d(ApnEditor.TAG, "receiver: ACTION_SIM_STATE_CHANGED");
            ApnEditor.this.updateScreenEnableState();
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
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
        this.mMvnoType = (ListPreference) findPreference("mvno_type");
        this.mMvnoType.setOnPreferenceChangeListener(this);
        this.mMvnoMatchData = (EditTextPreference) findPreference("mvno_match_data");
        this.mRes = getResources();
        Intent intent = getIntent();
        String action = intent.getAction();
        this.mSubId = intent.getIntExtra("sub_id", -1);
        this.mFirstTime = icicle == null;
        this.mApnExt = UtilsExt.getApnSettingsPlugin(getContext());
        if (action.equals("android.intent.action.EDIT")) {
            Uri uri = intent.getData();
            if (!uri.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
                Log.e(TAG, "Edit request not for carrier table. Uri: " + uri);
                finish();
                return;
            } else {
                this.mUri = uri;
                this.mReadOnlyMode = intent.getBooleanExtra("readOnly", false);
                Log.d(TAG, "Read only mode : " + this.mReadOnlyMode);
            }
        } else if (action.equals("android.intent.action.INSERT")) {
            if (this.mFirstTime || icicle.getInt("pos") == 0) {
                Uri uri2 = intent.getData();
                if (!uri2.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
                    Log.e(TAG, "Insert request not for carrier table. Uri: " + uri2);
                    finish();
                    return;
                } else {
                    this.mUri = getContentResolver().insert(uri2, new ContentValues());
                    this.mUri = this.mApnExt.getUriFromIntent(this.mUri, getContext(), intent);
                }
            } else {
                Log.d(TAG, "saved pos is " + icicle.getInt("pos"));
                this.mUri = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, icicle.getInt("pos"));
            }
            this.mNewApn = true;
            this.mMvnoTypeStr = intent.getStringExtra("mvno_type");
            this.mMvnoMatchDataStr = intent.getStringExtra("mvno_match_data");
            Log.d(TAG, "mvnoType = " + this.mMvnoTypeStr + ", mvnoMatchData =" + this.mMvnoMatchDataStr);
            if (this.mUri == null) {
                Log.w(TAG, "Failed to insert new telephony provider into " + getIntent().getData());
                finish();
                return;
            }
            setResult(-1, new Intent().setAction(this.mUri.toString()));
        } else {
            finish();
            return;
        }
        sProjection = this.mApnExt.customizeApnProjection(sProjection);
        this.mApnExt.customizePreference(this.mSubId, getPreferenceScreen());
        this.mCursor = getActivity().managedQuery(this.mUri, sProjection, null, null);
        this.mCursor.moveToFirst();
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            getPreferenceScreen().getPreference(i).setOnPreferenceChangeListener(this);
        }
        fillUi();
        this.mSimHotSwapHandler = new SimHotSwapHandler(getContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                Log.d(ApnEditor.TAG, "onSimHotSwap, finish Activity~~");
                ApnEditor.this.finish();
            }
        });
    }

    @Override
    protected int getMetricsCategory() {
        return 13;
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("android.intent.action.ANY_DATA_STATE");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        getContext().registerReceiver(this.mReceiver, filter);
        updateScreenEnableState();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getContext().unregisterReceiver(this.mReceiver);
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    private void fillUi() {
        Log.d(TAG, "fillUi... mFirstTime = " + this.mFirstTime);
        if (this.mCursor.getCount() == 0) {
            Log.w(TAG, "fillUi(), cursor count is 0, finish~~");
            finish();
            return;
        }
        this.mName.setText(this.mCursor.getString(1));
        this.mApn.setText(this.mCursor.getString(2));
        this.mProxy.setText(this.mCursor.getString(3));
        this.mPort.setText(this.mCursor.getString(4));
        this.mUser.setText(this.mCursor.getString(5));
        this.mServer.setText(this.mCursor.getString(6));
        this.mPassword.setText(this.mCursor.getString(7));
        this.mMmsProxy.setText(this.mCursor.getString(12));
        this.mMmsPort.setText(this.mCursor.getString(13));
        this.mMmsc.setText(this.mCursor.getString(8));
        this.mMcc.setText(this.mCursor.getString(9));
        this.mMnc.setText(this.mCursor.getString(10));
        this.mApnType.setText(this.mCursor.getString(15));
        this.mSourceType = this.mCursor.getInt(23);
        if (this.mNewApn) {
            String numeric = this.mTelephonyManager.getSimOperator(this.mSubId);
            Log.d(TAG, " fillUi, numeric = " + numeric);
            String numeric2 = CdmaApnSetting.updateMccMncForCdma(numeric, this.mSubId);
            if (numeric2 != null && numeric2.length() > 4) {
                String mcc = numeric2.substring(0, 3);
                String mnc = numeric2.substring(3);
                this.mMcc.setText(mcc);
                this.mMnc.setText(mnc);
                this.mCurMnc = mnc;
                this.mCurMcc = mcc;
            }
            this.mSourceType = 1;
        }
        int authVal = this.mCursor.getInt(14);
        if (authVal != -1) {
            this.mAuthType.setValueIndex(authVal);
        } else {
            this.mAuthType.setValue(null);
        }
        this.mProtocol.setValue(this.mCursor.getString(16));
        this.mRoamingProtocol.setValue(this.mCursor.getString(20));
        this.mCarrierEnabled.setChecked(this.mCursor.getInt(17) == 1);
        this.mBearerInitialVal = this.mCursor.getInt(18);
        HashSet<String> bearers = new HashSet<>();
        int bearerBitmask = this.mCursor.getInt(19);
        if (bearerBitmask == 0) {
            if (this.mBearerInitialVal == 0) {
                bearers.add("0");
            }
        } else {
            int i = 1;
            while (bearerBitmask != 0) {
                if ((bearerBitmask & 1) == 1) {
                    bearers.add("" + i);
                }
                bearerBitmask >>= 1;
                i++;
            }
        }
        if (this.mBearerInitialVal != 0 && !bearers.contains("" + this.mBearerInitialVal)) {
            bearers.add("" + this.mBearerInitialVal);
        }
        this.mBearerMulti.setValues(bearers);
        this.mMvnoType.setValue(this.mCursor.getString(21));
        this.mMvnoMatchData.setEnabled(false);
        this.mMvnoMatchData.setText(this.mCursor.getString(22));
        if (this.mNewApn && this.mMvnoTypeStr != null && this.mMvnoMatchDataStr != null) {
            this.mMvnoType.setValue(this.mMvnoTypeStr);
            this.mMvnoMatchData.setText(this.mMvnoMatchDataStr);
        }
        this.mApnExt.setPreferenceTextAndSummary(this.mSubId, this.mCursor.getString(sProjection.length - 1));
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
        this.mMcc.setSummary(checkNull(this.mMcc.getText()));
        this.mMnc.setSummary(checkNull(this.mMnc.getText()));
        this.mApnType.setSummary(checkNull(this.mApnType.getText()));
        String authValue = this.mAuthType.getValue();
        if (authValue != null) {
            int authValIndex = Integer.parseInt(authValue);
            this.mAuthType.setValueIndex(authValIndex);
            String[] values = this.mRes.getStringArray(R.array.apn_auth_entries);
            this.mAuthType.setSummary(values[authValIndex]);
        } else {
            this.mAuthType.setSummary(sNotSet);
        }
        this.mProtocol.setSummary(checkNull(protocolDescription(this.mProtocol.getValue(), this.mProtocol)));
        this.mRoamingProtocol.setSummary(checkNull(protocolDescription(this.mRoamingProtocol.getValue(), this.mRoamingProtocol)));
        this.mBearerMulti.setSummary(checkNull(bearerMultiDescription(this.mBearerMulti.getValues())));
        this.mMvnoType.setSummary(checkNull(mvnoDescription(this.mMvnoType.getValue())));
        this.mMvnoMatchData.setSummary(checkNull(this.mMvnoMatchData.getText()));
        boolean ceEditable = getResources().getBoolean(R.bool.config_allow_edit_carrier_enabled);
        if (ceEditable) {
            this.mCarrierEnabled.setEnabled(true);
        } else {
            this.mCarrierEnabled.setEnabled(false);
        }
    }

    private String protocolDescription(String raw, ListPreference protocol) {
        int protocolIndex = protocol.findIndexOfValue(raw);
        if (protocolIndex == -1) {
            return null;
        }
        String[] values = this.mRes.getStringArray(R.array.apn_protocol_entries);
        try {
            return values[protocolIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    private String bearerMultiDescription(Set<String> raw) {
        String[] values = this.mRes.getStringArray(R.array.bearer_entries);
        StringBuilder retVal = new StringBuilder();
        boolean first = true;
        for (String bearer : raw) {
            int bearerIndex = this.mBearerMulti.findIndexOfValue(bearer);
            if (first) {
                try {
                    retVal.append(values[bearerIndex]);
                    first = false;
                } catch (ArrayIndexOutOfBoundsException e) {
                }
            } else {
                retVal.append(", ").append(values[bearerIndex]);
            }
        }
        String val = retVal.toString();
        if (!TextUtils.isEmpty(val)) {
            return val;
        }
        return null;
    }

    private String mvnoDescription(String newValue) {
        int mvnoIndex = this.mMvnoType.findIndexOfValue(newValue);
        String oldValue = this.mMvnoType.getValue();
        if (mvnoIndex == -1) {
            return null;
        }
        String[] values = this.mRes.getStringArray(R.array.ext_mvno_type_entries);
        this.mMvnoMatchData.setEnabled(mvnoIndex != 0);
        if (newValue != null && !newValue.equals(oldValue)) {
            if (values[mvnoIndex].equals("SPN")) {
                this.mMvnoMatchData.setText(this.mTelephonyManager.getSimOperatorName());
            } else if (values[mvnoIndex].equals("IMSI")) {
                String numeric = this.mTelephonyManager.getSimOperator(this.mSubId);
                this.mMvnoMatchData.setText(numeric + "x");
            } else if (values[mvnoIndex].equals("GID")) {
                this.mMvnoMatchData.setText(this.mTelephonyManager.getGroupIdLevel1());
            }
        }
        try {
            return values[mvnoIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if ("auth_type".equals(key)) {
            try {
                int index = Integer.parseInt((String) newValue);
                this.mAuthType.setValueIndex(index);
                String[] values = this.mRes.getStringArray(R.array.apn_auth_entries);
                this.mAuthType.setSummary(values[index]);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if ("apn_protocol".equals(key)) {
            String protocol = protocolDescription((String) newValue, this.mProtocol);
            if (protocol == null) {
                return false;
            }
            this.mProtocol.setSummary(protocol);
            this.mProtocol.setValue((String) newValue);
            return true;
        }
        if ("apn_roaming_protocol".equals(key)) {
            String protocol2 = protocolDescription((String) newValue, this.mRoamingProtocol);
            if (protocol2 == null) {
                return false;
            }
            this.mRoamingProtocol.setSummary(protocol2);
            this.mRoamingProtocol.setValue((String) newValue);
            return true;
        }
        if ("bearer_multi".equals(key)) {
            String bearer = bearerMultiDescription((Set) newValue);
            if (bearer == null) {
                return false;
            }
            this.mBearerMulti.setValues((Set) newValue);
            this.mBearerMulti.setSummary(bearer);
            return true;
        }
        if ("mvno_type".equals(key)) {
            String mvno = mvnoDescription((String) newValue);
            if (mvno == null) {
                return false;
            }
            this.mMvnoType.setValue((String) newValue);
            this.mMvnoType.setSummary(mvno);
            return true;
        }
        if (!preference.equals(this.mPassword)) {
            if (preference.equals(this.mCarrierEnabled) || preference.equals(this.mBearerMulti)) {
                return true;
            }
            preference.setSummary(checkNull(newValue != null ? String.valueOf(newValue) : null));
            return true;
        }
        preference.setSummary(starify(newValue != null ? String.valueOf(newValue) : ""));
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, "onCreateOptionsMenu mReadOnlyMode = " + this.mReadOnlyMode);
        if (this.mReadOnlyMode) {
            return;
        }
        if (!this.mNewApn && this.mSourceType != 0) {
            menu.add(0, 1, 0, R.string.menu_delete).setIcon(R.drawable.ic_menu_delete);
        }
        menu.add(0, 2, 0, R.string.menu_save).setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, 3, 0, R.string.menu_cancel).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DefaultWfcSettingsExt.PAUSE:
                deleteApn();
                return true;
            case DefaultWfcSettingsExt.CREATE:
                if (this.mSourceType == 0) {
                    showDialog(1);
                } else if (validateAndSave(false)) {
                    finish();
                }
                return true;
            case DefaultWfcSettingsExt.DESTROY:
                if (this.mNewApn && this.mUri != null) {
                    getContentResolver().delete(this.mUri, null, null);
                }
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setOnKeyListener(this);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != 0) {
            return false;
        }
        switch (keyCode) {
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                if (validateAndSave(false)) {
                    finish();
                }
                break;
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        if (!validateAndSave(true) || this.mCursor == null || this.mCursor.getCount() == 0) {
            return;
        }
        icicle.putInt("pos", this.mCursor.getInt(0));
    }

    public boolean validateAndSave(boolean force) {
        int bearerVal;
        Log.d(TAG, "validateAndSave... force = " + force);
        String name = checkNotSet(this.mName.getText());
        String apn = checkNotSet(this.mApn.getText());
        String mcc = checkNotSet(this.mMcc.getText());
        String mnc = checkNotSet(this.mMnc.getText());
        if (getErrorMsg() != null && !force) {
            ErrorDialog.showError(this);
            return false;
        }
        if (!this.mCursor.moveToFirst() && !this.mNewApn) {
            Log.w(TAG, "Could not go to the first row in the Cursor when saving data.");
            return false;
        }
        if (force && this.mNewApn && name.length() < 1 && apn.length() < 1 && this.mUri != null) {
            getContentResolver().delete(this.mUri, null, null);
            this.mUri = null;
            return false;
        }
        ContentValues values = new ContentValues();
        if (name.length() < 1) {
            name = getResources().getString(R.string.untitled_apn);
        }
        values.put("name", name);
        values.put("apn", apn);
        values.put("proxy", checkNotSet(this.mProxy.getText()));
        values.put("port", checkNotSet(this.mPort.getText()));
        values.put("mmsproxy", checkNotSet(this.mMmsProxy.getText()));
        values.put("mmsport", checkNotSet(this.mMmsPort.getText()));
        values.put("user", checkNotSet(this.mUser.getText()));
        values.put("server", checkNotSet(this.mServer.getText()));
        values.put("password", checkNotSet(this.mPassword.getText()));
        values.put("mmsc", checkNotSet(this.mMmsc.getText()));
        String authVal = this.mAuthType.getValue();
        if (authVal != null) {
            values.put("authtype", Integer.valueOf(Integer.parseInt(authVal)));
        }
        values.put("protocol", checkNotSet(this.mProtocol.getValue()));
        values.put("roaming_protocol", checkNotSet(this.mRoamingProtocol.getValue()));
        values.put("type", checkNotSet(this.mApnType.getText()));
        values.put("mcc", mcc);
        values.put("mnc", mnc);
        values.put("numeric", mcc + mnc);
        if (this.mCurMnc != null && this.mCurMcc != null && this.mCurMnc.equals(mnc) && this.mCurMcc.equals(mcc)) {
            values.put("current", (Integer) 1);
        }
        Set<String> bearerSet = this.mBearerMulti.getValues();
        int bearerBitmask = 0;
        Iterator bearer$iterator = bearerSet.iterator();
        while (true) {
            if (!bearer$iterator.hasNext()) {
                break;
            }
            String bearer = (String) bearer$iterator.next();
            if (Integer.parseInt(bearer) == 0) {
                bearerBitmask = 0;
                break;
            }
            bearerBitmask |= ServiceState.getBitmaskForTech(Integer.parseInt(bearer));
        }
        values.put("bearer_bitmask", Integer.valueOf(bearerBitmask));
        if (bearerBitmask != 0 && this.mBearerInitialVal != 0 && ServiceState.bitmaskHasTech(bearerBitmask, this.mBearerInitialVal)) {
            bearerVal = this.mBearerInitialVal;
        } else {
            bearerVal = 0;
        }
        values.put("bearer", Integer.valueOf(bearerVal));
        values.put("mvno_type", checkNotSet(this.mMvnoType.getValue()));
        values.put("mvno_match_data", checkNotSet(this.mMvnoMatchData.getText()));
        values.put("carrier_enabled", Integer.valueOf(this.mCarrierEnabled.isChecked() ? 1 : 0));
        values.put("sourcetype", Integer.valueOf(this.mSourceType));
        this.mApnExt.saveApnValues(values);
        Log.d(TAG, "vaules:" + values);
        if (this.mUri == null) {
            Log.i(TAG, "former inserted URI was already deleted, insert a new one");
            this.mUri = getContentResolver().insert(getIntent().getData(), values);
            return true;
        }
        getContentResolver().update(this.mUri, values, null, null);
        return true;
    }

    public String getErrorMsg() {
        String name = checkNotSet(this.mName.getText());
        String apn = checkNotSet(this.mApn.getText());
        String mcc = checkNotSet(this.mMcc.getText());
        String mnc = checkNotSet(this.mMnc.getText());
        String apnType = this.mApnType.getText();
        if (name.length() < 1) {
            String errorMsg = this.mRes.getString(R.string.error_name_empty);
            return errorMsg;
        }
        if ((apnType == null || !apnType.contains("ia")) && apn.length() < 1) {
            String errorMsg2 = this.mRes.getString(R.string.error_apn_empty);
            return errorMsg2;
        }
        if (mcc.length() != 3) {
            String errorMsg3 = this.mRes.getString(R.string.error_mcc_not3);
            return errorMsg3;
        }
        if ((mnc.length() & 65534) == 2) {
            return null;
        }
        String errorMsg4 = this.mRes.getString(R.string.error_mnc_not23);
        return errorMsg4;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        return id == 1 ? new AlertDialog.Builder(getContext()).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.error_title).setMessage(getString(R.string.apn_predefine_change_dialog_notice)).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!ApnEditor.this.validateAndSave(false)) {
                    return;
                }
                ApnEditor.this.finish();
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create() : super.onCreateDialog(id);
    }

    private void deleteApn() {
        if (this.mUri != null) {
            getContentResolver().delete(this.mUri, null, null);
        }
        finish();
    }

    private String starify(String value) {
        if (value == null || value.length() == 0) {
            return sNotSet;
        }
        char[] password = new char[value.length()];
        for (int i = 0; i < password.length; i++) {
            password[i] = '*';
        }
        return new String(password);
    }

    private String checkNull(String value) {
        if (value == null || value.length() == 0) {
            return sNotSet;
        }
        return value;
    }

    private String checkNotSet(String value) {
        if (value == null || value.equals(sNotSet)) {
            return "";
        }
        return value;
    }

    public static class ErrorDialog extends DialogFragment {
        public static void showError(ApnEditor editor) {
            ErrorDialog dialog = new ErrorDialog();
            dialog.setTargetFragment(editor, 0);
            dialog.show(editor.getFragmentManager(), "error");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String msg = ((ApnEditor) getTargetFragment()).getErrorMsg();
            return new AlertDialog.Builder(getContext()).setTitle(R.string.error_title).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).setMessage(msg).create();
        }
    }

    @Override
    public void onDestroy() {
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        super.onDestroy();
        this.mApnExt.onDestroy();
    }

    public void exitWithoutSave() {
        if (this.mNewApn && this.mUri != null) {
            getContentResolver().delete(this.mUri, null, null);
        }
        finish();
    }

    public void updateScreenEnableState() {
        boolean screenEnableState;
        boolean enable = isSimReadyAndRadioOn();
        Log.d(TAG, "enable = " + enable + " mReadOnlyMode = " + this.mReadOnlyMode);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (!enable || this.mReadOnlyMode) {
            screenEnableState = false;
        } else {
            screenEnableState = this.mApnExt.getScreenEnableState(this.mSubId, getActivity());
        }
        preferenceScreen.setEnabled(screenEnableState);
        this.mApnExt.setApnTypePreferenceState(this.mApnType, this.mApnType.getText());
        this.mApnExt.updateFieldsStatus(this.mSubId, this.mSourceType, getPreferenceScreen());
        this.mApnExt.setMvnoPreferenceState(this.mMvnoType, this.mMvnoMatchData);
    }

    private boolean isSimReadyAndRadioOn() {
        boolean simReady = 5 == TelephonyManager.getDefault().getSimState(SubscriptionManager.getSlotId(this.mSubId));
        boolean airplaneModeEnabled = Settings.System.getInt(getContentResolver(), "airplane_mode_on", -1) == 1;
        boolean z = !airplaneModeEnabled ? simReady : false;
        Log.d(TAG, "isSimReadyAndRadioOn(), subId = " + this.mSubId + " ,airplaneModeEnabled = " + airplaneModeEnabled + " ,simReady = " + simReady);
        return z;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref == null) {
            return;
        }
        if (pref.equals(this.mCarrierEnabled)) {
            pref.setSummary(checkNull(String.valueOf(sharedPreferences.getBoolean(key, true))));
            return;
        }
        if (pref.equals(this.mPort)) {
            String portStr = sharedPreferences.getString(key, "");
            if (!portStr.equals("")) {
                try {
                    int portNum = Integer.parseInt(portStr);
                    if (portNum > 65535 || portNum <= 0) {
                        Toast.makeText(getContext(), getString(R.string.apn_port_warning), 1).show();
                        ((EditTextPreference) pref).setText("");
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), getString(R.string.apn_port_warning), 1).show();
                    ((EditTextPreference) pref).setText("");
                }
            }
            pref.setSummary(checkNull(sharedPreferences.getString(key, "")));
            return;
        }
        pref.setSummary(checkNull(sharedPreferences.getString(key, "")));
    }
}
