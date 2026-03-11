package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

public class ApnEditor extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {
    private static String sNotSet;
    private EditTextPreference mApn;
    private EditTextPreference mApnType;
    private ListPreference mAuthType;
    private ListPreference mBearer;
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
    private ListPreference mMvnoType;
    private EditTextPreference mName;
    private boolean mNewApn;
    private EditTextPreference mPassword;
    private EditTextPreference mPort;
    private ListPreference mProtocol;
    private EditTextPreference mProxy;
    private Resources mRes;
    private ListPreference mRoamingProtocol;
    private EditTextPreference mServer;
    private int mSubId;
    private TelephonyManager mTelephonyManager;
    private Uri mUri;
    private EditTextPreference mUser;
    private static final String TAG = ApnEditor.class.getSimpleName();
    private static final String[] sProjection = {"_id", "name", "apn", "proxy", "port", "user", "server", "password", "mmsc", "mcc", "mnc", "numeric", "mmsproxy", "mmsport", "authtype", "type", "protocol", "carrier_enabled", "bearer", "roaming_protocol", "mvno_type", "mvno_match_data"};

    @Override
    protected void onCreate(Bundle icicle) {
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
        this.mBearer = (ListPreference) findPreference("bearer");
        this.mBearer.setOnPreferenceChangeListener(this);
        this.mMvnoType = (ListPreference) findPreference("mvno_type");
        this.mMvnoType.setOnPreferenceChangeListener(this);
        this.mMvnoMatchData = (EditTextPreference) findPreference("mvno_match_data");
        this.mRes = getResources();
        Intent intent = getIntent();
        String action = intent.getAction();
        this.mSubId = intent.getIntExtra("sub_id", -1);
        this.mFirstTime = icicle == null;
        if (action.equals("android.intent.action.EDIT")) {
            this.mUri = intent.getData();
        } else if (action.equals("android.intent.action.INSERT")) {
            if (this.mFirstTime || icicle.getInt("pos") == 0) {
                this.mUri = getContentResolver().insert(intent.getData(), new ContentValues());
            } else {
                this.mUri = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, icicle.getInt("pos"));
            }
            this.mNewApn = true;
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
        this.mCursor = managedQuery(this.mUri, sProjection, null, null);
        this.mCursor.moveToFirst();
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        fillUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    private void fillUi() {
        String numeric;
        if (this.mFirstTime) {
            this.mFirstTime = false;
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
            if (this.mNewApn && (numeric = this.mTelephonyManager.getSimOperator(this.mSubId)) != null && numeric.length() > 4) {
                String mcc = numeric.substring(0, 3);
                String mnc = numeric.substring(3);
                this.mMcc.setText(mcc);
                this.mMnc.setText(mnc);
                this.mCurMnc = mnc;
                this.mCurMcc = mcc;
            }
            int authVal = this.mCursor.getInt(14);
            if (authVal != -1) {
                this.mAuthType.setValueIndex(authVal);
            } else {
                this.mAuthType.setValue(null);
            }
            this.mProtocol.setValue(this.mCursor.getString(16));
            this.mRoamingProtocol.setValue(this.mCursor.getString(19));
            this.mCarrierEnabled.setChecked(this.mCursor.getInt(17) == 1);
            this.mBearer.setValue(this.mCursor.getString(18));
            this.mMvnoType.setValue(this.mCursor.getString(20));
            this.mMvnoMatchData.setEnabled(false);
            this.mMvnoMatchData.setText(this.mCursor.getString(21));
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
        this.mMcc.setSummary(checkNull(this.mMcc.getText()));
        this.mMnc.setSummary(checkNull(this.mMnc.getText()));
        this.mApnType.setSummary(checkNull(this.mApnType.getText()));
        String authVal2 = this.mAuthType.getValue();
        if (authVal2 != null) {
            int authValIndex = Integer.parseInt(authVal2);
            this.mAuthType.setValueIndex(authValIndex);
            String[] values = this.mRes.getStringArray(R.array.apn_auth_entries);
            this.mAuthType.setSummary(values[authValIndex]);
        } else {
            this.mAuthType.setSummary(sNotSet);
        }
        this.mProtocol.setSummary(checkNull(protocolDescription(this.mProtocol.getValue(), this.mProtocol)));
        this.mRoamingProtocol.setSummary(checkNull(protocolDescription(this.mRoamingProtocol.getValue(), this.mRoamingProtocol)));
        this.mBearer.setSummary(checkNull(bearerDescription(this.mBearer.getValue())));
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

    private String bearerDescription(String raw) {
        int mBearerIndex = this.mBearer.findIndexOfValue(raw);
        if (mBearerIndex == -1) {
            return null;
        }
        String[] values = this.mRes.getStringArray(R.array.bearer_entries);
        try {
            return values[mBearerIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    private String mvnoDescription(String newValue) {
        int mvnoIndex = this.mMvnoType.findIndexOfValue(newValue);
        String oldValue = this.mMvnoType.getValue();
        if (mvnoIndex == -1) {
            return null;
        }
        String[] values = this.mRes.getStringArray(R.array.mvno_type_entries);
        if (values[mvnoIndex].equals("None")) {
            this.mMvnoMatchData.setEnabled(false);
        } else {
            this.mMvnoMatchData.setEnabled(true);
        }
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
            } catch (NumberFormatException e) {
                return false;
            }
        } else if ("apn_protocol".equals(key)) {
            String protocol = protocolDescription((String) newValue, this.mProtocol);
            if (protocol == null) {
                return false;
            }
            this.mProtocol.setSummary(protocol);
            this.mProtocol.setValue((String) newValue);
        } else if ("apn_roaming_protocol".equals(key)) {
            String protocol2 = protocolDescription((String) newValue, this.mRoamingProtocol);
            if (protocol2 == null) {
                return false;
            }
            this.mRoamingProtocol.setSummary(protocol2);
            this.mRoamingProtocol.setValue((String) newValue);
        } else if ("bearer".equals(key)) {
            String bearer = bearerDescription((String) newValue);
            if (bearer == null) {
                return false;
            }
            this.mBearer.setValue((String) newValue);
            this.mBearer.setSummary(bearer);
        } else if ("mvno_type".equals(key)) {
            String mvno = mvnoDescription((String) newValue);
            if (mvno == null) {
                return false;
            }
            this.mMvnoType.setValue((String) newValue);
            this.mMvnoType.setSummary(mvno);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (!this.mNewApn) {
            menu.add(0, 1, 0, R.string.menu_delete).setIcon(R.drawable.ic_menu_delete);
        }
        menu.add(0, 2, 0, R.string.menu_save).setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, 3, 0, R.string.menu_cancel).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                deleteApn();
                return true;
            case 2:
                if (!validateAndSave(false)) {
                    return true;
                }
                finish();
                return true;
            case 3:
                if (this.mNewApn) {
                    getContentResolver().delete(this.mUri, null, null);
                }
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 4:
                if (validateAndSave(false)) {
                    finish();
                }
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        if (validateAndSave(true)) {
            icicle.putInt("pos", this.mCursor.getInt(0));
        }
    }

    private boolean validateAndSave(boolean force) {
        String name = checkNotSet(this.mName.getText());
        String apn = checkNotSet(this.mApn.getText());
        String mcc = checkNotSet(this.mMcc.getText());
        String mnc = checkNotSet(this.mMnc.getText());
        if (getErrorMsg() != null && !force) {
            showDialog(0);
            return false;
        }
        if (!this.mCursor.moveToFirst()) {
            Log.w(TAG, "Could not go to the first row in the Cursor when saving data.");
            return false;
        }
        if (force && this.mNewApn && name.length() < 1 && apn.length() < 1) {
            getContentResolver().delete(this.mUri, null, null);
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
        String bearerVal = this.mBearer.getValue();
        if (bearerVal != null) {
            values.put("bearer", Integer.valueOf(Integer.parseInt(bearerVal)));
        }
        values.put("mvno_type", checkNotSet(this.mMvnoType.getValue()));
        values.put("mvno_match_data", checkNotSet(this.mMvnoMatchData.getText()));
        values.put("carrier_enabled", Integer.valueOf(this.mCarrierEnabled.isChecked() ? 1 : 0));
        if (this.mNewApn || isContentChanged(values).booleanValue()) {
            getContentResolver().update(this.mUri, values, null, null);
        }
        return true;
    }

    private String getErrorMsg() {
        String name = checkNotSet(this.mName.getText());
        String apn = checkNotSet(this.mApn.getText());
        String mcc = checkNotSet(this.mMcc.getText());
        String mnc = checkNotSet(this.mMnc.getText());
        if (name.length() < 1) {
            String errorMsg = this.mRes.getString(R.string.error_name_empty);
            return errorMsg;
        }
        if (apn.length() < 1) {
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
    protected Dialog onCreateDialog(int id) {
        if (id != 0) {
            return super.onCreateDialog(id);
        }
        String msg = getErrorMsg();
        return new AlertDialog.Builder(this).setTitle(R.string.error_title).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).setMessage(msg).create();
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        String msg;
        super.onPrepareDialog(id, dialog);
        if (id == 0 && (msg = getErrorMsg()) != null) {
            ((AlertDialog) dialog).setMessage(msg);
        }
    }

    private void deleteApn() {
        getContentResolver().delete(this.mUri, null, null);
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            if (pref.equals(this.mPassword)) {
                pref.setSummary(starify(sharedPreferences.getString(key, "")));
            } else if (!pref.equals(this.mCarrierEnabled)) {
                pref.setSummary(checkNull(sharedPreferences.getString(key, "")));
            }
        }
    }

    private Boolean isContentChanged(ContentValues values) {
        String sval;
        String sval2;
        String sval3;
        String sval4;
        String sval5;
        String sval6;
        String sval7;
        String sval8;
        String sval9;
        String sval10;
        String sval11;
        String sval12;
        String sval13;
        Integer val;
        Integer val2;
        String sval14;
        Integer val3;
        String sval15;
        String sval16;
        try {
            String sval17 = (String) values.get("name");
            if (sval17 != null && sval17.equals(this.mCursor.getString(1)) && (sval = (String) values.get("apn")) != null && sval.equals(this.mCursor.getString(2)) && (sval2 = (String) values.get("proxy")) != null && sval2.equals(this.mCursor.getString(3)) && (sval3 = (String) values.get("port")) != null && sval3.equals(this.mCursor.getString(4)) && (sval4 = (String) values.get("user")) != null && sval4.equals(this.mCursor.getString(5)) && (sval5 = (String) values.get("server")) != null && sval5.equals(this.mCursor.getString(6)) && (sval6 = (String) values.get("password")) != null && sval6.equals(this.mCursor.getString(7)) && (sval7 = (String) values.get("mmsproxy")) != null && sval7.equals(this.mCursor.getString(12)) && (sval8 = (String) values.get("mmsport")) != null && sval8.equals(this.mCursor.getString(13)) && (sval9 = (String) values.get("mmsc")) != null && sval9.equals(this.mCursor.getString(8)) && (sval10 = (String) values.get("mcc")) != null && sval10.equals(this.mCursor.getString(9)) && (sval11 = (String) values.get("mnc")) != null && sval11.equals(this.mCursor.getString(10)) && (sval12 = (String) values.get("type")) != null && sval12.equals(this.mCursor.getString(15)) && (sval13 = (String) values.get("protocol")) != null && sval13.equals(this.mCursor.getString(16)) && (((val = values.getAsInteger("authtype")) == null || val.intValue() == this.mCursor.getInt(14)) && (((val2 = values.getAsInteger("current")) == null || val2.intValue() == this.mCursor.getInt(this.mCursor.getColumnIndexOrThrow("current"))) && (sval14 = (String) values.get("roaming_protocol")) != null && sval14.equals(this.mCursor.getString(19)) && (((val3 = values.getAsInteger("bearer")) == null || val3.intValue() == this.mCursor.getInt(18)) && (sval15 = (String) values.get("mvno_type")) != null && sval15.equals(this.mCursor.getString(20)) && (sval16 = (String) values.get("mvno_match_data")) != null && sval16.equals(this.mCursor.getString(21)))))) {
                Log.d(TAG, "isContentChanged() return  false.");
                return false;
            }
            Log.d(TAG, "isContentChanged() return true.");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "database error" + e);
            return true;
        }
    }
}
