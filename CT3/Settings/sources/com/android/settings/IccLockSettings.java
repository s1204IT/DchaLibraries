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
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.Toast;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.EditPinPreference;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimRoamingExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.TelephonyUtils;

public class IccLockSettings extends SettingsPreferenceFragment implements EditPinPreference.OnPinEnteredListener {
    private String mError;
    private ListView mListView;
    private ISettingsMiscExt mMiscExt;
    private String mNewPin;
    private String mOldPin;
    private Phone mPhone;
    private String mPin;
    private EditPinPreference mPinDialog;
    private SwitchPreference mPinToggle;
    private Resources mRes;
    SimHotSwapHandler mSimHotSwapHandler;
    private ISimRoamingExt mSimRoamingExt;
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
                    IccLockSettings.this.iccLockChanged(ar.exception, msg.arg1);
                    break;
                case 101:
                    IccLockSettings.this.iccPinChanged(ar.exception, msg.arg1);
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
            Log.d("IccLockSettings", "onReceive, action = " + action);
            if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                IccLockSettings.this.mHandler.sendMessage(IccLockSettings.this.mHandler.obtainMessage(102));
            } else {
                if (!"android.intent.action.AIRPLANE_MODE".equals(action)) {
                    return;
                }
                IccLockSettings.this.mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
                IccLockSettings.this.updatePreferences();
            }
        }
    };
    private TabHost.OnTabChangeListener mTabListener = new TabHost.OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            int slotId = Integer.parseInt(tabId);
            SubscriptionInfo sir = SubscriptionManager.from(IccLockSettings.this.getActivity().getBaseContext()).getActiveSubscriptionInfoForSimSlotIndex(slotId);
            IccLockSettings.this.mPhone = sir != null ? PhoneFactory.getPhone(SubscriptionManager.getPhoneId(sir.getSubscriptionId())) : null;
            Log.d("IccLockSettings", "onTabChanged()... mPhone: " + IccLockSettings.this.mPhone);
            IccLockSettings.this.updatePreferences();
            IccLockSettings.this.changeSimTitle();
        }
    };
    private TabHost.TabContentFactory mEmptyTabContent = new TabHost.TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(IccLockSettings.this.mTabHost.getContext());
        }
    };
    private boolean mIsAirplaneModeOn = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getActivity());
        this.mMiscExt = UtilsExt.getMiscPlugin(getContext());
        this.mSimRoamingExt = UtilsExt.getSimRoamingExtPlugin(getActivity());
        addPreferencesFromResource(R.xml.sim_lock_settings);
        this.mPinDialog = (EditPinPreference) findPreference("sim_pin");
        this.mPinToggle = (SwitchPreference) findPreference("sim_toggle");
        if (savedInstanceState != null && savedInstanceState.containsKey("dialogState")) {
            this.mDialogState = savedInstanceState.getInt("dialogState");
            this.mPin = savedInstanceState.getString("dialogPin");
            this.mError = savedInstanceState.getString("dialogError");
            this.mToState = savedInstanceState.getBoolean("enableState");
            switch (this.mDialogState) {
                case DefaultWfcSettingsExt.DESTROY:
                    this.mOldPin = savedInstanceState.getString("oldPinCode");
                    break;
                case DefaultWfcSettingsExt.CONFIG_CHANGE:
                    this.mOldPin = savedInstanceState.getString("oldPinCode");
                    this.mNewPin = savedInstanceState.getString("newPinCode");
                    break;
            }
        }
        this.mPinDialog.setOnPinEnteredListener(this);
        getPreferenceScreen().setPersistent(false);
        this.mRes = getResources();
        this.mSimHotSwapHandler = new SimHotSwapHandler(getActivity());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                Log.d("IccLockSettings", "onSimHotSwap, finish Activity~~");
                IccLockSettings.this.finish();
            }
        });
        getActivity().setTitle(this.mMiscExt.customizeSimDisplayString(getActivity().getTitle().toString(), -1));
        if (!Utils.isMonkeyRunning()) {
            return;
        }
        finish();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        CharSequence displayName;
        TelephonyManager tm = (TelephonyManager) getContext().getSystemService("phone");
        int numSims = tm.getSimCount();
        if (numSims > 1) {
            View view = inflater.inflate(R.layout.icc_lock_tabs, container, false);
            ViewGroup prefs_container = (ViewGroup) view.findViewById(R.id.prefs_container);
            Utils.prepareCustomPreferencesList(container, view, prefs_container, false);
            View prefs = super.onCreateView(inflater, prefs_container, savedInstanceState);
            prefs_container.addView(prefs);
            this.mTabHost = (TabHost) view.findViewById(android.R.id.tabhost);
            this.mTabWidget = (TabWidget) view.findViewById(android.R.id.tabs);
            this.mListView = (ListView) view.findViewById(android.R.id.list);
            this.mTabHost.setup();
            this.mTabHost.setOnTabChangedListener(this.mTabListener);
            this.mTabHost.clearAllTabs();
            SubscriptionManager sm = SubscriptionManager.from(getContext());
            for (int i = 0; i < numSims; i++) {
                SubscriptionInfo subInfo = sm.getActiveSubscriptionInfoForSimSlotIndex(i);
                TabHost tabHost = this.mTabHost;
                String strValueOf = String.valueOf(i);
                if (subInfo == null) {
                    displayName = getContext().getString(R.string.sim_editor_title, Integer.valueOf(i + 1));
                } else {
                    displayName = subInfo.getDisplayName();
                }
                tabHost.addTab(buildTabSpec(strValueOf, String.valueOf(displayName)));
            }
            SubscriptionInfo sir = sm.getActiveSubscriptionInfoForSimSlotIndex(0);
            this.mPhone = sir == null ? null : PhoneFactory.getPhone(SubscriptionManager.getPhoneId(sir.getSubscriptionId()));
            Log.d("IccLockSettings", "onCreateView()... mPhone: " + this.mPhone);
            return view;
        }
        this.mPhone = PhoneFactory.getDefaultPhone();
        changeSimTitle();
        Log.d("IccLockSettings", "onCreateView()... mPhone: " + this.mPhone);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatePreferences();
    }

    public void updatePreferences() {
        boolean z = false;
        this.mPinDialog.setEnabled((this.mPhone == null || this.mIsAirplaneModeOn) ? false : true);
        SwitchPreference switchPreference = this.mPinToggle;
        if (this.mPhone != null && !this.mIsAirplaneModeOn) {
            z = true;
        }
        switchPreference.setEnabled(z);
        if (this.mPhone == null) {
            return;
        }
        boolean enabled = this.mPhone.getIccCard().getIccLockEnabled();
        Log.d("IccLockSettings", "getIccLockEnabled = " + enabled);
        this.mPinToggle.setChecked(enabled);
    }

    @Override
    protected int getMetricsCategory() {
        return 56;
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        getContext().registerReceiver(this.mSimStateReceiver, filter);
        this.mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getActivity());
        updatePreferences();
        if (this.mDialogState != 0) {
            showPinDialog();
        } else {
            resetDialogState();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().unregisterReceiver(this.mSimStateReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        if (this.mPinDialog.isDialogOpen()) {
            out.putInt("dialogState", this.mDialogState);
            out.putString("dialogPin", this.mPinDialog.getEditText().getText().toString());
            out.putString("dialogError", this.mError);
            out.putBoolean("enableState", this.mToState);
            switch (this.mDialogState) {
                case DefaultWfcSettingsExt.DESTROY:
                    out.putString("oldPinCode", this.mOldPin);
                    break;
                case DefaultWfcSettingsExt.CONFIG_CHANGE:
                    out.putString("oldPinCode", this.mOldPin);
                    out.putString("newPinCode", this.mNewPin);
                    break;
            }
            return;
        }
        super.onSaveInstanceState(out);
    }

    private void showPinDialog() {
        if (this.mDialogState == 0) {
            return;
        }
        setDialogValues();
        this.mPinDialog.showPinDialog();
    }

    private void setDialogValues() {
        String string;
        this.mPinDialog.setText(this.mPin);
        String message = "";
        switch (this.mDialogState) {
            case DefaultWfcSettingsExt.PAUSE:
                message = this.mRes.getString(R.string.sim_enter_pin);
                EditPinPreference editPinPreference = this.mPinDialog;
                if (this.mToState) {
                    string = this.mRes.getString(R.string.sim_enable_sim_lock);
                } else {
                    string = this.mRes.getString(R.string.sim_disable_sim_lock);
                }
                editPinPreference.setDialogTitle(string);
                break;
            case DefaultWfcSettingsExt.CREATE:
                message = this.mRes.getString(R.string.sim_enter_old);
                this.mPinDialog.setDialogTitle(this.mRes.getString(R.string.sim_change_pin));
                break;
            case DefaultWfcSettingsExt.DESTROY:
                message = this.mRes.getString(R.string.sim_enter_new);
                this.mPinDialog.setDialogTitle(this.mRes.getString(R.string.sim_change_pin));
                break;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                message = this.mRes.getString(R.string.sim_reenter_new);
                this.mPinDialog.setDialogTitle(this.mRes.getString(R.string.sim_change_pin));
                break;
        }
        if (this.mError != null) {
            message = this.mError + "\n" + message;
            this.mError = null;
        }
        Log.d("IccLockSettings", "setDialogValues mDialogState = " + this.mDialogState);
        this.mPinDialog.setDialogMessage(message);
        changeDialogStrings(this.mPinDialog.getDialogTitle().toString(), message);
    }

    @Override
    public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
        if (!positiveResult) {
            resetDialogState();
        }
        this.mPin = preference.getText();
        if (!reasonablePin(this.mPin)) {
            this.mError = this.mRes.getString(R.string.sim_bad_pin);
            if (isResumed()) {
                showPinDialog();
                return;
            }
            return;
        }
        switch (this.mDialogState) {
            case DefaultWfcSettingsExt.PAUSE:
                tryChangeIccLockState();
                break;
            case DefaultWfcSettingsExt.CREATE:
                this.mOldPin = this.mPin;
                this.mDialogState = 3;
                this.mError = null;
                this.mPin = null;
                showPinDialog();
                break;
            case DefaultWfcSettingsExt.DESTROY:
                this.mNewPin = this.mPin;
                this.mDialogState = 4;
                this.mPin = null;
                showPinDialog();
                break;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
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
    public boolean onPreferenceTreeClick(Preference preference) {
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
        if (this.mPhone == null) {
            return;
        }
        Log.d("IccLockSettings", "setIccLockEnabled, mToState = " + this.mToState + " mPin = " + this.mPin);
        this.mPhone.getIccCard().setIccLockEnabled(this.mToState, this.mPin, callback);
        this.mPinToggle.setEnabled(false);
    }

    public void iccLockChanged(Throwable exception, int attemptsRemaining) {
        Log.d("IccLockSettings", "iccLockChanged, exception = " + exception + ",attemptsRemaining = " + attemptsRemaining);
        boolean success = exception == null;
        if (success) {
            this.mPinToggle.setChecked(this.mToState);
            this.mSimRoamingExt.showPinToast(this.mToState);
        } else {
            Toast.makeText(getContext(), getPinPasswordErrorMessage(attemptsRemaining, exception), 1).show();
        }
        this.mPinToggle.setEnabled(true);
        resetDialogState();
    }

    public void iccPinChanged(Throwable exception, int attemptsRemaining) {
        Log.d("IccLockSettings", "iccPinChanged, exception = " + exception + ",attemptsRemaining = " + attemptsRemaining);
        boolean success = exception == null;
        if (!success) {
            Toast.makeText(getContext(), getPinPasswordErrorMessage(attemptsRemaining, exception), 1).show();
        } else {
            String successMsg = this.mRes.getString(R.string.sim_change_succeeded);
            Toast.makeText(getContext(), this.mMiscExt.customizeSimDisplayString(successMsg, this.mPhone.getSubId()), 0).show();
        }
        resetDialogState();
    }

    private void tryChangePin() {
        Message callback = Message.obtain(this.mHandler, 101);
        if (this.mPhone == null) {
            return;
        }
        Log.d("IccLockSettings", "changeIccLockPassword, mOldPin = " + this.mOldPin + " mNewPin = " + this.mNewPin);
        this.mPhone.getIccCard().changeIccLockPassword(this.mOldPin, this.mNewPin, callback);
    }

    private String getPinPasswordErrorMessage(int attemptsRemaining, Throwable exception) {
        String displayMessage;
        if ((exception instanceof CommandException) && ((CommandException) exception).getCommandError() == CommandException.Error.GENERIC_FAILURE) {
            displayMessage = this.mRes.getString(R.string.pin_failed);
        } else if (attemptsRemaining == 0) {
            displayMessage = this.mRes.getString(R.string.wrong_pin_code_pukked);
        } else if (attemptsRemaining > 0) {
            displayMessage = this.mRes.getQuantityString(R.plurals.wrong_pin_code, attemptsRemaining, Integer.valueOf(attemptsRemaining));
        } else {
            displayMessage = this.mRes.getString(R.string.pin_failed);
        }
        String displayMessage2 = this.mMiscExt.customizeSimDisplayString(displayMessage, this.mPhone.getSubId());
        Log.d("IccLockSettings", "getPinPasswordErrorMessage: attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage2);
        return displayMessage2;
    }

    private boolean reasonablePin(String pin) {
        if (pin == null || pin.length() < 4 || pin.length() > 8) {
            return false;
        }
        return true;
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mSimHotSwapHandler == null) {
            return;
        }
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
    }

    public void changeSimTitle() {
        if (this.mPhone == null) {
            return;
        }
        int subId = this.mPhone.getSubId();
        Log.d("IccLockSettings", "changeSimTitle subId = " + subId);
        this.mPinToggle.setTitle(this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.sim_pin_toggle), subId));
        this.mPinDialog.setTitle(this.mMiscExt.customizeSimDisplayString(getResources().getString(R.string.sim_pin_change), subId));
    }

    private void changeDialogStrings(String dialogTitle, String dialogMessage) {
        if (this.mPhone == null) {
            return;
        }
        int subId = this.mPhone.getSubId();
        Log.d("IccLockSettings", "changeSimTitle subId = " + subId);
        this.mPinDialog.setDialogTitle(this.mMiscExt.customizeSimDisplayString(dialogTitle, subId));
        this.mPinDialog.setDialogMessage(this.mMiscExt.customizeSimDisplayString(dialogMessage, subId));
    }
}
