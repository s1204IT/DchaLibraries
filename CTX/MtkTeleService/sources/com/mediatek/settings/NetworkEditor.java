package com.mediatek.settings;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.mediatek.phone.PhoneFeatureConstants;

public class NetworkEditor extends PreferenceActivity implements Preference.OnPreferenceChangeListener, TextWatcher, PhoneGlobals.SubInfoUpdateListener {
    private int mAct;
    private String mInitNetworkId;
    private String mInitNetworkMode;
    private IntentFilter mIntentFilter;
    private EditText mNetworkIdText;
    private NetworkInfo mNetworkInfo;
    private String mPLMNName;
    private PhoneStateListener mPhoneStateListener;
    private int mSubId;
    private TelephonyManager mTelephonyManager;
    private Preference mNetworkId = null;
    private NetworkTypePreference mNetworkMode = null;
    private String mNotSet = null;
    private boolean mAirplaneModeEnabled = false;
    private boolean mActSupport = true;
    private AlertDialog mIdDialog = null;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.AIRPLANE_MODE")) {
                NetworkEditor.this.finish();
            }
        }
    };
    private DialogInterface.OnClickListener mNetworkIdListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -1) {
                NetworkEditor.this.mNetworkInfo.setNetworkId(NetworkEditor.this.checkNull(NetworkEditor.this.mNetworkIdText.getText().toString()));
                NetworkEditor.this.mNetworkId.setSummary(NetworkEditor.this.checkNull(NetworkEditor.this.mNetworkIdText.getText().toString()));
                NetworkEditor.this.invalidateOptionsMenu();
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.mtk_plmn_editor);
        this.mNotSet = getResources().getString(R.string.voicemail_number_not_set);
        this.mNetworkId = findPreference("network_id_key");
        this.mNetworkMode = (NetworkTypePreference) findPreference("key_network_type");
        this.mIntentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        this.mNetworkInfo = new NetworkInfo();
        createNetworkInfo(getIntent());
        this.mNetworkMode.initCheckState(this.mAct);
        this.mNetworkMode.setOnPreferenceChangeListener(this);
        this.mPhoneStateListener = new PhoneStateListener(Integer.valueOf(this.mSubId)) {
            @Override
            public void onCallStateChanged(int i, String str) {
                super.onCallStateChanged(i, str);
                if (i == 0) {
                    NetworkEditor.this.setScreenEnabled();
                }
            }
        };
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
        registerReceiver(this.mReceiver, this.mIntentFilter);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == this.mNetworkId) {
            removeDialog(0);
            showDialog(0);
            validate();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mAirplaneModeEnabled = Settings.System.getInt(getContentResolver(), "airplane_mode_on", -1) == 1;
        setScreenEnabled();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (!getIntent().getBooleanExtra("plmn_add", false)) {
            menu.add(0, 1, 0, android.R.string.biometric_dangling_notification_action_not_now);
        }
        menu.add(0, 2, 0, R.string.save);
        menu.add(0, 3, 0, android.R.string.cancel);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean zIsShouldEnable = isShouldEnable();
        boolean zEquals = this.mNotSet.equals(this.mNetworkId.getSummary());
        if (menu != null) {
            boolean z = false;
            menu.setGroupEnabled(0, zIsShouldEnable);
            if (getIntent().getBooleanExtra("plmn_add", true)) {
                MenuItem item = menu.getItem(0);
                if (zIsShouldEnable && !zEquals) {
                    z = true;
                }
                item.setEnabled(z);
            } else {
                Log.d("Settings/NetworkEditor", "networkID: " + ((Object) this.mNetworkId.getSummary()) + ", networkmode: " + ((Object) this.mNetworkMode.getSummary()));
                boolean z2 = this.mInitNetworkId.equals(this.mNetworkId.getSummary()) && this.mInitNetworkMode.equals(this.mNetworkMode.getSummary());
                MenuItem item2 = menu.getItem(1);
                if (zIsShouldEnable && !z2 && !zEquals) {
                    z = true;
                }
                item2.setEnabled(z);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private boolean isShouldEnable() {
        return (TelephonyManager.getDefault().getCallState(this.mSubId) == 0) && !this.mAirplaneModeEnabled && TelephonyUtils.isRadioOn(this.mSubId, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId != 16908332) {
            switch (itemId) {
                case 1:
                    setRemovedNetwork();
                    break;
                case 2:
                    validateAndSetResult();
                    break;
            }
            finish();
            return super.onOptionsItemSelected(menuItem);
        }
        finish();
        return true;
    }

    private void validateAndSetResult() {
        Intent intent = new Intent(this, (Class<?>) PLMNListPreference.class);
        setResult(100, intent);
        genNetworkInfo(intent);
    }

    private void genNetworkInfo(Intent intent) {
        intent.putExtra("plmn_name", checkNotSet(this.mPLMNName));
        intent.putExtra("plmn_code", this.mNetworkId.getSummary());
        intent.putExtra("plmn_priority", this.mNetworkInfo.getPriority());
        try {
            intent.putExtra("plmn_service", this.mAct);
        } catch (NumberFormatException e) {
            intent.putExtra("plmn_service", covertApNW2Ril(0));
        }
    }

    private void setRemovedNetwork() {
        Intent intent = new Intent(this, (Class<?>) PLMNListPreference.class);
        setResult(200, intent);
        genNetworkInfo(intent);
    }

    public static int covertRilNW2Ap(Context context, int i, int i2) {
        boolean z = (i & 1) != 0;
        boolean z2 = (i & 4) != 0;
        boolean z3 = TelephonyUtils.isUSIMCard(context, i2) && (i & 8) != 0 && PhoneFeatureConstants.FeatureOption.isMtkLteSupport();
        if (z && z2 && z3) {
            return 6;
        }
        if (!z && z2 && z3) {
            return 5;
        }
        if (!z || z2 || !z3) {
            if (z && z2 && !z3) {
                return 3;
            }
            if (!z && !z2 && z3) {
                return 2;
            }
            if (!z && z2 && !z3) {
                return 1;
            }
            if (!z || z2 || !z3) {
            }
            return 0;
        }
        return 4;
    }

    public static int covertApNW2Ril(int i) {
        switch (i) {
            case 0:
                return 1;
            case 1:
                return 4;
            case 2:
                return 8;
            case 3:
                return 5;
            case 4:
                return 9;
            case 5:
                return 12;
            case 6:
                return 13;
            default:
                return 0;
        }
    }

    private void createNetworkInfo(Intent intent) {
        this.mPLMNName = intent.getStringExtra("plmn_name");
        this.mSubId = intent.getIntExtra("plmn_sub", -1);
        this.mAct = intent.getIntExtra("plmn_service", 0);
        if (this.mAct == 0 || this.mAct == 2) {
            this.mAct = 1;
        }
        updateNetWorkInfo(intent);
    }

    private String checkNotSet(String str) {
        if (str == null || str.equals(this.mNotSet)) {
            return "";
        }
        return str;
    }

    private String checkNull(String str) {
        if (str == null || str.length() == 0) {
            return this.mNotSet;
        }
        return str;
    }

    private void setScreenEnabled() {
        boolean zIsShouldEnable = isShouldEnable();
        getPreferenceScreen().setEnabled(zIsShouldEnable);
        invalidateOptionsMenu();
        this.mNetworkMode.setEnabled(zIsShouldEnable);
    }

    @Override
    public Dialog onCreateDialog(int i) {
        if (i == 0) {
            View viewInflate = LayoutInflater.from(this).inflate(R.xml.mtk_plmn_id_editor, (ViewGroup) null);
            this.mNetworkIdText = (EditText) viewInflate.findViewById(R.id.plmn_id);
            if (!this.mNotSet.equals(this.mNetworkId.getSummary())) {
                this.mNetworkIdText.setText(this.mNetworkId.getSummary());
                this.mNetworkIdText.setSelection(this.mNetworkIdText.getText().toString().length());
            }
            this.mNetworkIdText.addTextChangedListener(this);
            this.mNetworkIdText.setInputType(2);
            this.mIdDialog = new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.network_id)).setView(viewInflate).setPositiveButton(getResources().getString(android.R.string.ok), this.mNetworkIdListener).setNegativeButton(getResources().getString(android.R.string.cancel), (DialogInterface.OnClickListener) null).create();
            this.mIdDialog.getWindow().setSoftInputMode(4);
            return this.mIdDialog;
        }
        return null;
    }

    public void validate() {
        boolean z;
        int length = this.mNetworkIdText.getText().toString().length();
        this.mNetworkIdText.setSelection(length);
        if (length < 5 || length > 6) {
            z = false;
        } else {
            z = true;
        }
        if (this.mIdDialog != null) {
            this.mIdDialog.getButton(-1).setEnabled(z);
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
        validate();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    private void updateNetWorkInfo(Intent intent) {
        Log.d("Settings/NetworkEditor", "---updateNetWorkInfo-- " + this.mNetworkInfo.getPriority() + " : " + this.mNetworkInfo.getNetworkId() + " : " + this.mNetworkInfo.getNetWorkMode());
        if (TextUtils.isEmpty(this.mNetworkInfo.getNetworkId())) {
            this.mInitNetworkId = intent.getStringExtra("plmn_code");
            Log.d("Settings/NetworkEditor", "mInitNetworkId: " + this.mInitNetworkId);
            this.mNetworkInfo.setNetworkId(this.mInitNetworkId);
        }
        this.mNetworkId.setSummary(checkNull(this.mNetworkInfo.getNetworkId()));
        if (this.mNetworkInfo.mPriority == -1) {
            this.mNetworkInfo.setPriority(intent.getIntExtra("plmn_priority", 0));
        }
        if (TextUtils.isEmpty(this.mNetworkInfo.getNetWorkMode())) {
            int intExtra = intent.getIntExtra("plmn_service", 0);
            Log.d("Settings/NetworkEditor", "act = " + intExtra);
            if (!getIntent().getBooleanExtra("plmn_add", true)) {
                this.mActSupport = intExtra != 0;
            }
            Log.d("Settings/NetworkEditor", "mActSupport = " + this.mActSupport);
            this.mInitNetworkMode = getResources().getStringArray(R.array.plmn_prefer_network_type_choices)[covertRilNW2Ap(this, intExtra, this.mSubId)];
            Log.d("Settings/NetworkEditor", "mInitNetworkMode: " + this.mInitNetworkMode);
            this.mNetworkInfo.setNetWorkMode(this.mInitNetworkMode);
        }
        this.mNetworkMode.setSummary(this.mNetworkInfo.getNetWorkMode());
    }

    class NetworkInfo {
        private String mNetworkId = null;
        private int mPriority = -1;
        private String mNetWorkMode = null;

        public NetworkInfo() {
        }

        public String getNetworkId() {
            return this.mNetworkId;
        }

        public void setNetworkId(String str) {
            this.mNetworkId = str;
        }

        public int getPriority() {
            return this.mPriority;
        }

        public void setPriority(int i) {
            this.mPriority = i;
        }

        public String getNetWorkMode() {
            return this.mNetWorkMode;
        }

        public void setNetWorkMode(String str) {
            this.mNetWorkMode = str;
        }
    }

    private void updateNetworkType(int i) {
        int iCovertRilNW2Ap = covertRilNW2Ap(this, i, this.mSubId);
        Log.d("Settings/NetworkEditor", "updateNetworkType: act = " + i + ", index = " + iCovertRilNW2Ap);
        this.mNetworkInfo.setNetWorkMode(getResources().getStringArray(R.array.plmn_prefer_network_type_choices)[iCovertRilNW2Ap]);
        this.mNetworkMode.setSummary(this.mNetworkInfo.getNetWorkMode());
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        String key = preference.getKey();
        Log.d("Settings/NetworkEditor", "key: " + key);
        if ("key_network_type".equals(key)) {
            this.mAct = ((Integer) obj).intValue();
            updateNetworkType(this.mAct);
            invalidateOptionsMenu();
            return true;
        }
        return false;
    }
}
