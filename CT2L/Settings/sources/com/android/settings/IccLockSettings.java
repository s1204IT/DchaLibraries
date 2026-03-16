package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.Toast;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.EditPinPreference;

public class IccLockSettings extends PreferenceActivity implements EditPinPreference.OnPinEnteredListener {
    private String mError;
    private ListView mListView;
    private String mNewPin;
    private String mOldPin;
    private Phone mPhone;
    private String mPin;
    private EditPinPreference mPinDialog;
    private SwitchPreference mPinToggle;
    private Resources mRes;
    private TabHost mTabHost;
    private TabWidget mTabWidget;
    private boolean mToState;
    private int mDialogState = 0;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
                case 100:
                    IccLockSettings.this.iccLockChanged(ar.exception == null, msg.arg1);
                    break;
                case 101:
                    IccLockSettings.this.iccPinChanged(ar.exception == null, msg.arg1);
                    break;
                case 102:
                    IccLockSettings.this.updatePreferences();
                    break;
            }
        }
    };
    private final BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                IccLockSettings.this.mHandler.sendMessage(IccLockSettings.this.mHandler.obtainMessage(102));
            }
        }
    };
    private TabHost.OnTabChangeListener mTabListener = new TabHost.OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            int slotId = Integer.parseInt(tabId);
            SubscriptionInfo sir = Utils.findRecordBySlotId(IccLockSettings.this.getBaseContext(), slotId);
            IccLockSettings.this.mPhone = sir == null ? null : PhoneFactory.getPhone(SubscriptionManager.getPhoneId(sir.getSubscriptionId()));
            IccLockSettings.this.updatePreferences();
        }
    };
    private TabHost.TabContentFactory mEmptyTabContent = new TabHost.TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(IccLockSettings.this.mTabHost.getContext());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        int numSims = tm.getSimCount();
        if (Utils.isMonkeyRunning()) {
            finish();
            return;
        }
        addPreferencesFromResource(R.xml.sim_lock_settings);
        this.mPinDialog = (EditPinPreference) findPreference("sim_pin");
        this.mPinToggle = (SwitchPreference) findPreference("sim_toggle");
        if (savedInstanceState != null && savedInstanceState.containsKey("dialogState")) {
            this.mDialogState = savedInstanceState.getInt("dialogState");
            this.mPin = savedInstanceState.getString("dialogPin");
            this.mError = savedInstanceState.getString("dialogError");
            this.mToState = savedInstanceState.getBoolean("enableState");
            switch (this.mDialogState) {
                case 3:
                    this.mOldPin = savedInstanceState.getString("oldPinCode");
                    break;
                case 4:
                    this.mOldPin = savedInstanceState.getString("oldPinCode");
                    this.mNewPin = savedInstanceState.getString("newPinCode");
                    break;
            }
        }
        this.mPinDialog.setOnPinEnteredListener(this);
        getPreferenceScreen().setPersistent(false);
        if (numSims > 1) {
            setContentView(R.layout.icc_lock_tabs);
            this.mTabHost = (TabHost) findViewById(android.R.id.tabhost);
            this.mTabWidget = (TabWidget) findViewById(android.R.id.tabs);
            this.mListView = (ListView) findViewById(android.R.id.list);
            this.mTabHost.setup();
            this.mTabHost.setOnTabChangedListener(this.mTabListener);
            this.mTabHost.clearAllTabs();
            for (int i = 0; i < numSims; i++) {
                SubscriptionInfo subInfo = Utils.findRecordBySlotId(this, i);
                this.mTabHost.addTab(buildTabSpec(String.valueOf(i), String.valueOf(subInfo == null ? context.getString(R.string.sim_editor_title, Integer.valueOf(i + 1)) : subInfo.getDisplayName())));
            }
            SubscriptionInfo sir = Utils.findRecordBySlotId(getBaseContext(), 0);
            this.mPhone = sir == null ? null : PhoneFactory.getPhone(SubscriptionManager.getPhoneId(sir.getSubscriptionId()));
        } else {
            this.mPhone = PhoneFactory.getDefaultPhone();
        }
        this.mRes = getResources();
        updatePreferences();
    }

    private void updatePreferences() {
        this.mPinDialog.setEnabled(this.mPhone != null);
        this.mPinToggle.setEnabled(this.mPhone != null);
        if (this.mPhone != null) {
            this.mPinToggle.setChecked(this.mPhone.getIccCard().getIccLockEnabled());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        registerReceiver(this.mSimStateReceiver, filter);
        if (this.mDialogState != 0) {
            showPinDialog();
        } else {
            resetDialogState();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(this.mSimStateReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        if (this.mPinDialog.isDialogOpen()) {
            out.putInt("dialogState", this.mDialogState);
            out.putString("dialogPin", this.mPinDialog.getEditText().getText().toString());
            out.putString("dialogError", this.mError);
            out.putBoolean("enableState", this.mToState);
            switch (this.mDialogState) {
                case 3:
                    out.putString("oldPinCode", this.mOldPin);
                    break;
                case 4:
                    out.putString("oldPinCode", this.mOldPin);
                    out.putString("newPinCode", this.mNewPin);
                    break;
            }
            return;
        }
        super.onSaveInstanceState(out);
    }

    private void showPinDialog() {
        if (this.mDialogState != 0) {
            setDialogValues();
            this.mPinDialog.showPinDialog();
        }
    }

    private void setDialogValues() {
        this.mPinDialog.setText(this.mPin);
        String message = "";
        switch (this.mDialogState) {
            case 1:
                message = this.mRes.getString(R.string.sim_enter_pin);
                this.mPinDialog.setDialogTitle(this.mToState ? this.mRes.getString(R.string.sim_enable_sim_lock) : this.mRes.getString(R.string.sim_disable_sim_lock));
                break;
            case 2:
                message = this.mRes.getString(R.string.sim_enter_old);
                this.mPinDialog.setDialogTitle(this.mRes.getString(R.string.sim_change_pin));
                break;
            case 3:
                message = this.mRes.getString(R.string.sim_enter_new);
                this.mPinDialog.setDialogTitle(this.mRes.getString(R.string.sim_change_pin));
                break;
            case 4:
                message = this.mRes.getString(R.string.sim_reenter_new);
                this.mPinDialog.setDialogTitle(this.mRes.getString(R.string.sim_change_pin));
                break;
        }
        if (this.mError != null) {
            message = this.mError + "\n" + message;
            this.mError = null;
        }
        this.mPinDialog.setDialogMessage(message);
    }

    @Override
    public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
        if (!positiveResult) {
            resetDialogState();
        }
        this.mPin = preference.getText();
        if (!reasonablePin(this.mPin)) {
            this.mError = this.mRes.getString(R.string.sim_bad_pin);
            showPinDialog();
            return;
        }
        switch (this.mDialogState) {
            case 1:
                tryChangeIccLockState();
                break;
            case 2:
                this.mOldPin = this.mPin;
                this.mDialogState = 3;
                this.mError = null;
                this.mPin = null;
                showPinDialog();
                break;
            case 3:
                this.mNewPin = this.mPin;
                this.mDialogState = 4;
                this.mPin = null;
                showPinDialog();
                break;
            case 4:
                if (!this.mPin.equals(this.mNewPin)) {
                    this.mError = this.mRes.getString(R.string.sim_pins_dont_match);
                    this.mDialogState = 3;
                    this.mPin = null;
                    showPinDialog();
                } else {
                    this.mError = null;
                    tryChangePin();
                }
                break;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == this.mPinToggle) {
            this.mToState = this.mPinToggle.isChecked();
            this.mPinToggle.setChecked(this.mToState ? false : true);
            this.mDialogState = 1;
            showPinDialog();
        } else if (preference == this.mPinDialog) {
            this.mDialogState = 2;
            return false;
        }
        return true;
    }

    private void tryChangeIccLockState() {
        Message callback = Message.obtain(this.mHandler, 100);
        this.mPhone.getIccCard().setIccLockEnabled(this.mToState, this.mPin, callback);
        this.mPinToggle.setEnabled(false);
    }

    private void iccLockChanged(boolean success, int attemptsRemaining) {
        if (success) {
            this.mPinToggle.setChecked(this.mToState);
        } else {
            Toast.makeText(this, getPinPasswordErrorMessage(attemptsRemaining), 1).show();
        }
        this.mPinToggle.setEnabled(true);
        resetDialogState();
    }

    private void iccPinChanged(boolean success, int attemptsRemaining) {
        if (!success) {
            Toast.makeText(this, getPinPasswordErrorMessage(attemptsRemaining), 1).show();
        } else {
            Toast.makeText(this, this.mRes.getString(R.string.sim_change_succeeded), 0).show();
        }
        resetDialogState();
    }

    private void tryChangePin() {
        Message callback = Message.obtain(this.mHandler, 101);
        this.mPhone.getIccCard().changeIccLockPassword(this.mOldPin, this.mNewPin, callback);
    }

    private String getPinPasswordErrorMessage(int attemptsRemaining) {
        String displayMessage;
        if (attemptsRemaining == 0) {
            displayMessage = this.mRes.getString(R.string.wrong_pin_code_pukked);
        } else if (attemptsRemaining > 0) {
            displayMessage = this.mRes.getQuantityString(R.plurals.wrong_pin_code, attemptsRemaining, Integer.valueOf(attemptsRemaining));
        } else {
            displayMessage = this.mRes.getString(R.string.pin_failed);
        }
        Log.d("IccLockSettings", "getPinPasswordErrorMessage: attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    private boolean reasonablePin(String pin) {
        return pin != null && pin.length() >= 4 && pin.length() <= 8;
    }

    private void resetDialogState() {
        this.mError = null;
        this.mDialogState = 2;
        this.mPin = "";
        setDialogValues();
        this.mDialogState = 0;
    }

    private TabHost.TabSpec buildTabSpec(String tag, String title) {
        return this.mTabHost.newTabSpec(tag).setIndicator(title).setContent(this.mEmptyTabContent);
    }
}
