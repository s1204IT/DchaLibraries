package com.android.phone.settings.fdn;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.phone.CallFeaturesSetting;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.android.phone.settings.fdn.EditPinPreference;
import com.mediatek.settings.CallSettingUtils;
import com.mediatek.settings.TelephonyUtils;

public class FdnSetting extends PreferenceActivity implements DialogInterface.OnCancelListener, Preference.OnPreferenceClickListener, PhoneGlobals.SubInfoUpdateListener, EditPinPreference.OnPinEnteredListener {
    private EditPinPreference mButtonChangePin2;
    private EditPinPreference mButtonEnableFDN;
    private Preference mButtonFDNList;
    private boolean mIsPuk2Locked;
    private String mNewPin;
    private String mOldPin;
    private Phone mPhone;
    private int mPinChangeState;
    private String mPuk2;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private final Handler mFDNHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 100) {
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.exception != null) {
                    if (!(asyncResult.exception instanceof CommandException)) {
                        FdnSetting.this.displayMessage(R.string.pin2_error_exception);
                    } else {
                        int i2 = message.arg1;
                        switch (AnonymousClass3.$SwitchMap$com$android$internal$telephony$CommandException$Error[asyncResult.exception.getCommandError().ordinal()]) {
                            case 1:
                                FdnSetting.this.displayMessage(R.string.fdn_enable_puk2_requested, i2);
                                FdnSetting.this.resetPinChangeStateForPUK2();
                                break;
                            case 2:
                                FdnSetting.this.displayMessage(R.string.pin2_invalid, i2);
                                break;
                            default:
                                FdnSetting.this.displayMessage(R.string.fdn_failed, i2);
                                break;
                        }
                    }
                }
                FdnSetting.this.updateWholeScreen(FdnSetting.this.mSubId);
                return;
            }
            if (i == 200) {
                FdnSetting.this.log("Handle EVENT_PIN2_CHANGE_COMPLETE");
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                if (asyncResult2.exception == null) {
                    if (FdnSetting.this.mPinChangeState == 3) {
                        FdnSetting.this.displayMessage(R.string.pin2_unblocked);
                    } else {
                        FdnSetting.this.displayMessage(R.string.pin2_changed);
                    }
                    FdnSetting.this.resetPinChangeState(FdnSetting.this.mSubId);
                } else if (!(asyncResult2.exception instanceof CommandException)) {
                    FdnSetting.this.displayMessage(R.string.pin2_error_exception);
                } else {
                    int i3 = message.arg1;
                    FdnSetting.this.log("Handle EVENT_PIN2_CHANGE_COMPLETE attemptsRemaining=" + i3);
                    CommandException commandException = asyncResult2.exception;
                    if (FdnSetting.this.handleChangePIN2ErrorForMTK(commandException)) {
                        FdnSetting.this.log("Handle handleChangePIN2ErrorForMTK Enter~");
                    } else if (commandException.getCommandError() != CommandException.Error.SIM_PUK2) {
                        if (!FdnSetting.this.mIsPuk2Locked) {
                            FdnSetting.this.displayMessage(R.string.badPin2, i3);
                            FdnSetting.this.resetPinChangeState();
                        } else {
                            FdnSetting.this.displayMessage(R.string.badPuk2, i3);
                            FdnSetting.this.resetPinChangeStateForPUK2();
                        }
                    } else {
                        AlertDialog alertDialogCreate = new AlertDialog.Builder(FdnSetting.this).setMessage(R.string.puk2_requested).setCancelable(true).setOnCancelListener(FdnSetting.this).setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i4) {
                                FdnSetting.this.resetPinChangeStateForPUK2();
                                FdnSetting.this.displayPinChangeDialog(0, true);
                            }
                        }).create();
                        alertDialogCreate.getWindow().addFlags(2);
                        alertDialogCreate.show();
                    }
                }
                FdnSetting.this.updateWholeScreen(FdnSetting.this.mSubId);
            }
        }
    };
    private int mSubId = -1;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("mediatek.intent.action.PHB_STATE_CHANGED".equals(intent.getAction())) {
                FdnSetting.this.log("PHB_STATE_CHANGED, update screen");
                FdnSetting.this.updateWholeScreen(FdnSetting.this.mSubId);
            } else if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
                FdnSetting.this.log("SIM_STATE_CHANGED, update screen");
                FdnSetting.this.updateWholeScreen(FdnSetting.this.mSubId);
            } else if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                FdnSetting.this.log("AIRPLANE_MODE_CHANGED, so finish FDN Settings");
                FdnSetting.this.finish();
            }
        }
    };

    @Override
    public void onPinEntered(EditPinPreference editPinPreference, boolean z) {
        if (editPinPreference == this.mButtonEnableFDN) {
            toggleFDNEnable(z);
        } else if (editPinPreference == this.mButtonChangePin2) {
            updatePINChangeState(z);
        }
    }

    private void toggleFDNEnable(boolean z) {
        log("[toggleFDNEnable] positiveResult: " + z);
        updateEnableFDNDialog();
        if (!z) {
            return;
        }
        String text = this.mButtonEnableFDN.getText();
        if (validatePin(text, false)) {
            this.mPhone.getIccCard().setIccFdnEnabled(!this.mPhone.getIccCard().getIccFdnEnabled(), text, this.mFDNHandler.obtainMessage(100));
        } else {
            displayMessage(R.string.invalidPin2);
        }
        this.mButtonEnableFDN.setText("");
    }

    private void updatePINChangeState(boolean z) {
        log("updatePINChangeState positive=" + z + " mPinChangeState=" + this.mPinChangeState + " mSkipOldPin=" + this.mIsPuk2Locked);
        if (!z) {
            if (!this.mIsPuk2Locked) {
                resetPinChangeState(this.mSubId);
                return;
            }
            resetPinChangeStateForPUK2(this.mSubId);
        }
        switch (this.mPinChangeState) {
            case 0:
                this.mOldPin = this.mButtonChangePin2.getText();
                this.mButtonChangePin2.setText("");
                if (validatePin(this.mOldPin, false)) {
                    this.mPinChangeState = 1;
                    displayPinChangeDialog();
                } else {
                    displayPinChangeDialog(R.string.invalidPin2, true);
                }
                break;
            case 1:
                this.mNewPin = this.mButtonChangePin2.getText();
                this.mButtonChangePin2.setText("");
                if (validatePin(this.mNewPin, false)) {
                    this.mPinChangeState = 2;
                    displayPinChangeDialog();
                } else {
                    displayPinChangeDialog(R.string.invalidPin2, true);
                }
                break;
            case 2:
                if (!this.mNewPin.equals(this.mButtonChangePin2.getText())) {
                    this.mPinChangeState = 1;
                    this.mButtonChangePin2.setText("");
                    displayPinChangeDialog(R.string.mismatchPin2, true);
                } else {
                    this.mButtonChangePin2.setText("");
                    this.mPhone.getIccCard().changeIccFdnPassword(this.mOldPin, this.mNewPin, this.mFDNHandler.obtainMessage(200));
                }
                break;
            case 3:
                this.mPuk2 = this.mButtonChangePin2.getText();
                this.mButtonChangePin2.setText("");
                if (validatePin(this.mPuk2, true)) {
                    this.mPinChangeState = 4;
                    displayPinChangeDialog();
                } else {
                    displayPinChangeDialog(R.string.invalidPuk2, true);
                }
                break;
            case 4:
                this.mNewPin = this.mButtonChangePin2.getText();
                this.mButtonChangePin2.setText("");
                if (validatePin(this.mNewPin, false)) {
                    this.mPinChangeState = 5;
                    displayPinChangeDialog();
                } else {
                    displayPinChangeDialog(R.string.invalidPin2, true);
                }
                break;
            case 5:
                if (!this.mNewPin.equals(this.mButtonChangePin2.getText())) {
                    this.mPinChangeState = 4;
                    this.mButtonChangePin2.setText("");
                    displayPinChangeDialog(R.string.mismatchPin2, true);
                } else {
                    this.mButtonChangePin2.setText("");
                    this.mPhone.getIccCard().supplyPuk2(this.mPuk2, this.mNewPin, this.mFDNHandler.obtainMessage(200));
                }
                break;
        }
    }

    static class AnonymousClass3 {
        static final int[] $SwitchMap$com$android$internal$telephony$CommandException$Error = new int[CommandException.Error.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.SIM_PUK2.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.PASSWORD_INCORRECT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        resetPinChangeStateForPUK2(this.mSubId);
        displayPinChangeDialog(0, true);
        updateEnableFDNDialog();
    }

    private final void displayMessage(int i, int i2) {
        String string = getString(i);
        if (i == R.string.badPin2 || i == R.string.badPuk2 || i == R.string.pin2_invalid) {
            if (i2 >= 0) {
                string = getString(i) + getString(R.string.pin2_attempts, new Object[]{Integer.valueOf(i2)});
            } else {
                string = getString(i);
            }
        }
        log("displayMessage: attemptsRemaining=" + i2 + " s=" + string);
        Toast.makeText(this, string, 0).show();
    }

    private final void displayMessage(int i) {
        displayMessage(i, -1);
    }

    private final void displayPinChangeDialog() {
        displayPinChangeDialog(0, true);
    }

    private final void displayPinChangeDialog(int i, boolean z) {
        int i2;
        log("[displayPinChangeDialog] mPinChangeState : " + this.mPinChangeState);
        String str = "";
        switch (this.mPinChangeState) {
            case 0:
                i2 = R.string.oldPin2Label;
                str = "\n" + CallSettingUtils.getPinPuk2RetryLeftNumTips(this, this.mSubId, true);
                break;
            case 1:
            case 4:
                i2 = R.string.newPin2Label;
                break;
            case 2:
            case 5:
                i2 = R.string.confirmPin2Label;
                break;
            case 3:
            default:
                i2 = R.string.label_puk2_code;
                str = "\n" + CallSettingUtils.getPinPuk2RetryLeftNumTips(this, this.mSubId, false);
                break;
        }
        if (i != 0) {
            this.mButtonChangePin2.setDialogMessage(((Object) getText(i2)) + str + "\n" + ((Object) getText(i)));
        } else {
            this.mButtonChangePin2.setDialogMessage(((Object) getText(i2)) + str);
        }
        if (z) {
            this.mButtonChangePin2.showPinDialog();
        }
    }

    private final void resetPinChangeState() {
        log("resetPinChangeState");
        this.mPinChangeState = 0;
        displayPinChangeDialog(0, false);
        this.mNewPin = "";
        this.mOldPin = "";
        this.mIsPuk2Locked = false;
    }

    private final void resetPinChangeStateForPUK2() {
        log("resetPinChangeStateForPUK2");
        this.mPinChangeState = 3;
        displayPinChangeDialog(0, false);
        this.mPuk2 = "";
        this.mNewPin = "";
        this.mOldPin = "";
        this.mIsPuk2Locked = true;
    }

    private boolean validatePin(String str, boolean z) {
        int i;
        if (!z) {
            i = 4;
        } else {
            i = 8;
        }
        if (str == null || str.length() < i || str.length() > 8) {
            return false;
        }
        return true;
    }

    private void updateEnableFDN() {
        if (this.mPhone.getIccCard().getIccFdnEnabled()) {
            this.mButtonEnableFDN.setTitle(R.string.enable_fdn_ok);
            this.mButtonEnableFDN.setSummary(R.string.fdn_enabled);
            this.mButtonEnableFDN.setDialogTitle(R.string.disable_fdn);
        } else {
            this.mButtonEnableFDN.setTitle(R.string.disable_fdn_ok);
            this.mButtonEnableFDN.setSummary(R.string.fdn_disabled);
            this.mButtonEnableFDN.setDialogTitle(R.string.enable_fdn);
        }
    }

    private void updateChangePIN2() {
        if (this.mPhone.getIccCard().getIccPin2Blocked()) {
            resetPinChangeStateForPUK2();
        } else {
            resetPinChangeState();
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
        if (this.mPhone == null) {
            log("onCreate: mPhone is null, finish!!!");
            finish();
            return;
        }
        addPreferencesFromResource(R.xml.fdn_setting);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mButtonEnableFDN = (EditPinPreference) preferenceScreen.findPreference("button_fdn_enable_key");
        this.mButtonChangePin2 = (EditPinPreference) preferenceScreen.findPreference("button_change_pin2_key");
        this.mButtonEnableFDN.setOnPinEnteredListener(this);
        updateEnableFDN();
        this.mButtonChangePin2.setOnPinEnteredListener(this);
        ((PreferenceScreen) preferenceScreen.findPreference("fdn_list_pref_screen_key")).setIntent(this.mSubscriptionInfoHelper.getIntent(FdnList.class));
        if (bundle == null) {
            resetPinChangeState();
        } else {
            this.mIsPuk2Locked = bundle.getBoolean("skip_old_pin_key");
            this.mPinChangeState = bundle.getInt("pin_change_state_key");
            this.mOldPin = bundle.getString("old_pin_key");
            this.mNewPin = bundle.getString("new_pin_key");
            this.mButtonChangePin2.setDialogMessage(bundle.getString("dialog_message_key"));
            this.mButtonChangePin2.setText(bundle.getString("dialog_pin_entry_key"));
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            this.mSubscriptionInfoHelper.setActionBarTitle(actionBar, getResources(), R.string.fdn_with_label);
        }
        this.mSubId = this.mPhone.getSubId();
        onCreateMTK(preferenceScreen);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("mediatek.intent.action.PHB_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        registerReceiver(this.mReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWholeScreen(this.mSubId);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("skip_old_pin_key", this.mIsPuk2Locked);
        bundle.putInt("pin_change_state_key", this.mPinChangeState);
        bundle.putString("old_pin_key", this.mOldPin);
        bundle.putString("new_pin_key", this.mNewPin);
        bundle.putString("dialog_message_key", this.mButtonChangePin2.getDialogMessage().toString());
        bundle.putString("dialog_pin_entry_key", this.mButtonChangePin2.getText());
        bundle.putInt(SubscriptionInfoHelper.SUB_ID_EXTRA, this.mSubId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            CallFeaturesSetting.goUpToTopLevelSetting(this, this.mSubscriptionInfoHelper);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void log(String str) {
        Log.d(PhoneGlobals.LOG_TAG, "FdnSetting: " + str);
    }

    private void onCreateMTK(PreferenceScreen preferenceScreen) {
        this.mButtonFDNList = (PreferenceScreen) preferenceScreen.findPreference("fdn_list_pref_screen_key");
        if (this.mButtonFDNList != null) {
            this.mButtonFDNList.setOnPreferenceClickListener(this);
        }
        if (this.mButtonEnableFDN != null) {
            this.mButtonEnableFDN.setOnPreferenceClickListener(this);
            updateEnableFDNDialog();
        }
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        if (!PhoneUtils.isValidSubId(this.mSubId)) {
            finish();
            log("onCreate, finish for invalid sub = " + this.mSubId);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        log("[onPreferenceClick]");
        if (preference == this.mButtonEnableFDN) {
            if (CallSettingUtils.getPin2RetryNumber(this.mSubId) == 0) {
                log("[onPreferenceClick] retry number is 0, tips...");
                if (this.mButtonEnableFDN.getDialog() != null) {
                    this.mButtonEnableFDN.getDialog().dismiss();
                }
                displayMessage(R.string.fdn_puk_need_tips);
                return true;
            }
            return false;
        }
        if (preference == this.mButtonFDNList) {
            if (CallSettingUtils.isPhoneBookReady(this, this.mSubId)) {
                startActivity(preference.getIntent().putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, this.mSubId));
            }
            return true;
        }
        return false;
    }

    private void updateEnableFDNDialog() {
        log("[updateEnableFDNDialog]");
        this.mButtonEnableFDN.setDialogMessage(getString(R.string.enter_pin2_text) + "\n" + CallSettingUtils.getPinPuk2RetryLeftNumTips(this, this.mSubId, true));
    }

    private final void resetPinChangeState(int i) {
        log("resetPinChangeState...");
        if (CallSettingUtils.getPin2RetryNumber(i) == 0) {
            resetPinChangeStateForPUK2(this.mSubId);
        } else {
            resetPinChangeState();
            updateEnableFDNDialog();
        }
    }

    private final void resetPinChangeStateForPUK2(int i) {
        log("resetPinChangeStateForPUK2...");
        if (CallSettingUtils.getPuk2RetryNumber(i) == 0) {
            log("[resetPinChangeStateForPUK2] PUK Retry number is 0, PUK2 Locked!!");
            displayMessage(R.string.puk2_blocked);
        } else {
            resetPinChangeStateForPUK2();
            updateEnableFDNDialog();
        }
    }

    @Override
    public void onDestroy() {
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        unregisterReceiver(this.mReceiver);
        super.onDestroy();
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }

    private void updateWholeScreen(int i) {
        if (TelephonyUtils.isAirplaneModeOn(this)) {
            log("[updateWholeScreen] AirplaneMode ON, so finish FdnSettings");
            finish();
        }
        IccCard iccCard = this.mPhone.getIccCard();
        if (iccCard != null) {
            boolean iccFdnAvailable = iccCard.getIccFdnAvailable();
            log("[updateWholeScreen] FDN available = " + iccFdnAvailable);
            this.mButtonEnableFDN.setEnabled(iccFdnAvailable);
            this.mButtonFDNList.setEnabled(iccFdnAvailable);
            this.mButtonChangePin2.setEnabled(CallSettingUtils.getPin2RetryNumber(this.mSubId) != 255);
            updateEnableFDN();
            updateChangePIN2();
            resetPinChangeState(i);
            updateEnableFDNDialog();
        }
    }

    private boolean handleChangePIN2ErrorForMTK(CommandException commandException) {
        log("Handle EVENT_PIN2_CHANGE_COMPLETE Error Code =" + commandException.getCommandError().toString());
        if (CommandException.Error.RADIO_NOT_AVAILABLE == commandException.getCommandError()) {
            displayMessage(R.string.fdn_errorcode_unknown_info);
            return true;
        }
        return false;
    }
}
