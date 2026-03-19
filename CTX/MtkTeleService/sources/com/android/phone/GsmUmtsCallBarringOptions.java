package com.android.phone;

import android.app.ActionBar;
import android.app.Dialog;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneGlobals;
import com.android.phone.settings.fdn.EditPinPreference;
import com.mediatek.settings.vtss.GsmUmtsVTUtils;
import java.util.ArrayList;

public class GsmUmtsCallBarringOptions extends TimeConsumingPreferenceActivity implements PhoneGlobals.SubInfoUpdateListener, EditPinPreference.OnPinEnteredListener {
    private static final int BUSY_READING_DIALOG = 100;
    private static final int BUSY_SAVING_DIALOG = 200;
    private static final String BUTTON_BAIC_KEY = "button_baic_key";
    private static final String BUTTON_BAICr_KEY = "button_baicr_key";
    private static final String BUTTON_BAOC_KEY = "button_baoc_key";
    private static final String BUTTON_BAOIC_KEY = "button_baoic_key";
    private static final String BUTTON_BAOICxH_KEY = "button_baoicxh_key";
    private static final String BUTTON_BA_ALL_KEY = "button_ba_all_key";
    private static final String BUTTON_BA_CHANGE_PW_KEY = "button_change_pw_key";
    private static final boolean DBG = true;
    private static final String DIALOG_MESSAGE_KEY = "dialog_message_key";
    private static final String DIALOG_PW_ENTRY_KEY = "dialog_pw_enter_key";
    private static final int EVENT_DISABLE_ALL_COMPLETE = 200;
    private static final int EVENT_PW_CHANGE_COMPLETE = 100;
    private static final String KEY_STATUS = "toggle";
    private static final String LOG_TAG = "GsmUmtsCallBarringOptions";
    private static final String NEW_PW_KEY = "new_pw_key";
    private static final String OLD_PW_KEY = "old_pw_key";
    private static final String PREFERENCE_ENABLED_KEY = "PREFERENCE_ENABLED";
    private static final String PREFERENCE_SHOW_PASSWORD_KEY = "PREFERENCE_SHOW_PASSWORD";
    private static final int PW_CHANGE_NEW = 1;
    private static final int PW_CHANGE_OLD = 0;
    private static final int PW_CHANGE_REENTER = 2;
    private static final String PW_CHANGE_STATE_KEY = "pin_change_state_key";
    private static final int PW_LENGTH = 4;
    private static final String SAVED_BEFORE_LOAD_COMPLETED_KEY = "PROGRESS_SHOWING";
    private CallBarringEditPreference mButtonBAIC;
    private CallBarringEditPreference mButtonBAICr;
    private CallBarringEditPreference mButtonBAOC;
    private CallBarringEditPreference mButtonBAOIC;
    private CallBarringEditPreference mButtonBAOICxH;
    private EditPinPreference mButtonChangePW;
    private CallBarringDeselectAllPreference mButtonDisableAll;
    private boolean mFirstResume;
    private Bundle mIcicle;
    private String mNewPassword;
    private String mOldPassword;
    private Phone mPhone;
    private Dialog mProgressDialog;
    private int mPwChangeDialogStrId;
    private int mPwChangeState;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private ArrayList<CallBarringEditPreference> mPreferences = new ArrayList<>();
    private int mInitIndex = 0;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            int i = message.what;
            if (i == 100) {
                GsmUmtsCallBarringOptions.this.onFinished(GsmUmtsCallBarringOptions.this.mButtonChangePW, false);
                if (asyncResult.exception != null) {
                    Log.d(GsmUmtsCallBarringOptions.LOG_TAG, "change password for call barring failed with exception: " + asyncResult.exception);
                    GsmUmtsCallBarringOptions.this.onException(GsmUmtsCallBarringOptions.this.mButtonChangePW, (CommandException) asyncResult.exception);
                    GsmUmtsCallBarringOptions.this.mButtonChangePW.setEnabled(GsmUmtsCallBarringOptions.DBG);
                } else if (asyncResult.userObj instanceof Throwable) {
                    GsmUmtsCallBarringOptions.this.onError(GsmUmtsCallBarringOptions.this.mButtonChangePW, TimeConsumingPreferenceActivity.RESPONSE_ERROR);
                } else {
                    GsmUmtsCallBarringOptions.this.displayMessage(R.string.call_barring_change_pwd_success);
                }
                GsmUmtsCallBarringOptions.this.resetPwChangeState();
                return;
            }
            if (i == 200) {
                GsmUmtsCallBarringOptions.this.onFinished(GsmUmtsCallBarringOptions.this.mButtonDisableAll, false);
                if (asyncResult.exception != null) {
                    Log.d(GsmUmtsCallBarringOptions.LOG_TAG, "can not disable all call barring with exception: " + asyncResult.exception);
                    GsmUmtsCallBarringOptions.this.onException(GsmUmtsCallBarringOptions.this.mButtonDisableAll, (CommandException) asyncResult.exception);
                    GsmUmtsCallBarringOptions.this.mButtonDisableAll.setEnabled(GsmUmtsCallBarringOptions.DBG);
                    return;
                }
                if (asyncResult.userObj instanceof Throwable) {
                    GsmUmtsCallBarringOptions.this.onError(GsmUmtsCallBarringOptions.this.mButtonDisableAll, TimeConsumingPreferenceActivity.RESPONSE_ERROR);
                    return;
                } else {
                    GsmUmtsCallBarringOptions.this.displayMessage(R.string.call_barring_deactivate_success);
                    GsmUmtsCallBarringOptions.this.resetCallBarringPrefState(false);
                    return;
                }
            }
            Log.d(GsmUmtsCallBarringOptions.LOG_TAG, "Unknown message id: " + message.what);
        }
    };

    @Override
    public void onPinEntered(EditPinPreference editPinPreference, boolean z) {
        if (editPinPreference == this.mButtonChangePW) {
            updatePWChangeState(z);
        } else if (editPinPreference == this.mButtonDisableAll) {
            disableAllBarring(z);
        }
    }

    private void displayMessage(int i) {
        Toast.makeText(this, getString(i), 0).show();
    }

    private void disableAllBarring(boolean z) {
        if (!z) {
            return;
        }
        String text = null;
        if (this.mButtonDisableAll.isPasswordShown()) {
            text = this.mButtonDisableAll.getText();
            if (!validatePassword(text)) {
                this.mButtonDisableAll.setText("");
                displayMessage(R.string.call_barring_right_pwd_number);
                return;
            }
        }
        this.mButtonDisableAll.setText("");
        this.mPhone.setCallBarring("AB", false, text, this.mHandler.obtainMessage(200), 0);
        onStarted(this.mButtonDisableAll, false);
    }

    private void updatePWChangeState(boolean z) {
        if (!z) {
            resetPwChangeState();
        }
        switch (this.mPwChangeState) {
            case 0:
                this.mOldPassword = this.mButtonChangePW.getText();
                this.mButtonChangePW.setText("");
                if (validatePassword(this.mOldPassword)) {
                    this.mPwChangeState = 1;
                    displayPwChangeDialog();
                } else {
                    displayPwChangeDialog(R.string.call_barring_right_pwd_number, DBG);
                }
                break;
            case 1:
                this.mNewPassword = this.mButtonChangePW.getText();
                this.mButtonChangePW.setText("");
                if (validatePassword(this.mNewPassword)) {
                    this.mPwChangeState = 2;
                    displayPwChangeDialog();
                } else {
                    displayPwChangeDialog(R.string.call_barring_right_pwd_number, DBG);
                }
                break;
            case 2:
                if (!this.mNewPassword.equals(this.mButtonChangePW.getText())) {
                    this.mPwChangeState = 1;
                    this.mButtonChangePW.setText("");
                    displayPwChangeDialog(R.string.call_barring_pwd_not_match, DBG);
                } else {
                    this.mButtonChangePW.setText("");
                    this.mPhone.changeCallBarringPassword("AB", this.mOldPassword, this.mNewPassword, this.mHandler.obtainMessage(100));
                    onStarted(this.mButtonChangePW, false);
                }
                break;
            default:
                Log.d(LOG_TAG, "updatePWChangeState: Unknown password change state: " + this.mPwChangeState);
                break;
        }
    }

    private void displayPwChangeDialog() {
        displayPwChangeDialog(0, DBG);
    }

    private void displayPwChangeDialog(int i, boolean z) {
        int i2;
        switch (this.mPwChangeState) {
            case 0:
                i2 = R.string.call_barring_old_pwd;
                break;
            case 1:
                i2 = R.string.call_barring_new_pwd;
                break;
            case 2:
                i2 = R.string.call_barring_confirm_pwd;
                break;
            default:
                i2 = 0;
                break;
        }
        if (i != 0) {
            this.mButtonChangePW.setDialogMessage(((Object) getText(i2)) + "\n" + ((Object) getText(i)));
        } else {
            this.mButtonChangePW.setDialogMessage(i2);
        }
        if (z) {
            this.mButtonChangePW.showPinDialog();
        }
        this.mPwChangeDialogStrId = i;
    }

    private void resetPwChangeState() {
        this.mPwChangeState = 0;
        displayPwChangeDialog(0, false);
        this.mOldPassword = "";
        this.mNewPassword = "";
    }

    private void resetCallBarringPrefState(boolean z) {
        for (CallBarringEditPreference callBarringEditPreference : this.mPreferences) {
            callBarringEditPreference.mIsActivated = z;
            callBarringEditPreference.updateSummaryText();
        }
    }

    private boolean validatePassword(String str) {
        if (str == null || str.length() != 4) {
            return false;
        }
        return DBG;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d(LOG_TAG, "onCreate, reading callbarring_options.xml file");
        addPreferencesFromResource(R.xml.callbarring_options);
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.call_barring_settings_with_label);
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
        Log.d(LOG_TAG, "onCreate, reading callbarring_options.xml file finished!");
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mButtonBAOC = (CallBarringEditPreference) preferenceScreen.findPreference(BUTTON_BAOC_KEY);
        this.mButtonBAOIC = (CallBarringEditPreference) preferenceScreen.findPreference(BUTTON_BAOIC_KEY);
        this.mButtonBAOICxH = (CallBarringEditPreference) preferenceScreen.findPreference(BUTTON_BAOICxH_KEY);
        this.mButtonBAIC = (CallBarringEditPreference) preferenceScreen.findPreference(BUTTON_BAIC_KEY);
        this.mButtonBAICr = (CallBarringEditPreference) preferenceScreen.findPreference(BUTTON_BAICr_KEY);
        this.mButtonDisableAll = (CallBarringDeselectAllPreference) preferenceScreen.findPreference(BUTTON_BA_ALL_KEY);
        this.mButtonChangePW = (EditPinPreference) preferenceScreen.findPreference(BUTTON_BA_CHANGE_PW_KEY);
        this.mButtonBAOC.setOnPinEnteredListener(this);
        this.mButtonBAOIC.setOnPinEnteredListener(this);
        this.mButtonBAOICxH.setOnPinEnteredListener(this);
        this.mButtonBAIC.setOnPinEnteredListener(this);
        this.mButtonBAICr.setOnPinEnteredListener(this);
        this.mButtonDisableAll.setOnPinEnteredListener(this);
        this.mButtonChangePW.setOnPinEnteredListener(this);
        if (PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId()).getBoolean("mtk_support_vt_ss_bool")) {
            int intExtra = getIntent().getIntExtra("service_class", 1);
            GsmUmtsVTUtils.setCBServiceClass(preferenceScreen, intExtra);
            this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), GsmUmtsVTUtils.getActionBarResId(intExtra, 1));
        }
        this.mPreferences.add(this.mButtonBAOC);
        this.mPreferences.add(this.mButtonBAOIC);
        this.mPreferences.add(this.mButtonBAOICxH);
        this.mPreferences.add(this.mButtonBAIC);
        this.mPreferences.add(this.mButtonBAICr);
        boolean z = TelephonyManager.from(this).getSimState(SubscriptionManager.getSlotIndex(this.mPhone.getSubId())) == 5;
        if (z) {
            this.mButtonDisableAll.setEnabled(DBG);
            this.mButtonDisableAll.init(this.mPhone);
        } else {
            this.mButtonDisableAll.setEnabled(false);
        }
        if (z) {
            this.mButtonChangePW.setEnabled(DBG);
        } else {
            this.mButtonChangePW.setEnabled(false);
            this.mButtonChangePW.setSummary(R.string.call_barring_change_pwd_description_disabled);
        }
        this.mFirstResume = DBG;
        this.mIcicle = bundle;
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(DBG);
        }
        if (this.mIcicle != null && !this.mIcicle.getBoolean(SAVED_BEFORE_LOAD_COMPLETED_KEY)) {
            Log.d(LOG_TAG, "restore stored states");
            this.mInitIndex = this.mPreferences.size();
            for (CallBarringEditPreference callBarringEditPreference : this.mPreferences) {
                Bundle bundle2 = (Bundle) this.mIcicle.getParcelable(callBarringEditPreference.getKey());
                if (bundle2 != null) {
                    callBarringEditPreference.handleCallBarringResult(bundle2.getBoolean(KEY_STATUS));
                    callBarringEditPreference.init(this, DBG, this.mPhone);
                    callBarringEditPreference.setEnabled(bundle2.getBoolean(PREFERENCE_ENABLED_KEY, callBarringEditPreference.isEnabled()));
                    callBarringEditPreference.setInputMethodNeeded(bundle2.getBoolean(PREFERENCE_SHOW_PASSWORD_KEY, callBarringEditPreference.needInputMethod()));
                }
            }
            this.mPwChangeState = this.mIcicle.getInt(PW_CHANGE_STATE_KEY);
            this.mOldPassword = this.mIcicle.getString(OLD_PW_KEY);
            this.mNewPassword = this.mIcicle.getString(NEW_PW_KEY);
            displayPwChangeDialog(this.mIcicle.getInt(DIALOG_MESSAGE_KEY, this.mPwChangeDialogStrId), false);
            this.mButtonChangePW.setText(this.mIcicle.getString(DIALOG_PW_ENTRY_KEY));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mFirstResume) {
            if (this.mIcicle == null || this.mIcicle.getBoolean(SAVED_BEFORE_LOAD_COMPLETED_KEY)) {
                Log.d(LOG_TAG, "onResume: start to init ");
                resetPwChangeState();
                this.mPreferences.get(this.mInitIndex).init(this, false, this.mPhone);
                removeDialog(200);
            }
            this.mFirstResume = false;
            this.mIcicle = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        for (CallBarringEditPreference callBarringEditPreference : this.mPreferences) {
            Bundle bundle2 = new Bundle();
            bundle2.putBoolean(KEY_STATUS, callBarringEditPreference.mIsActivated);
            bundle2.putBoolean(PREFERENCE_ENABLED_KEY, callBarringEditPreference.isEnabled());
            bundle2.putBoolean(PREFERENCE_SHOW_PASSWORD_KEY, callBarringEditPreference.needInputMethod());
            bundle.putParcelable(callBarringEditPreference.getKey(), bundle2);
        }
        bundle.putInt(PW_CHANGE_STATE_KEY, this.mPwChangeState);
        bundle.putString(OLD_PW_KEY, this.mOldPassword);
        bundle.putString(NEW_PW_KEY, this.mNewPassword);
        bundle.putInt(DIALOG_MESSAGE_KEY, this.mPwChangeDialogStrId);
        bundle.putString(DIALOG_PW_ENTRY_KEY, this.mButtonChangePW.getText());
        bundle.putBoolean(SAVED_BEFORE_LOAD_COMPLETED_KEY, (this.mProgressDialog == null || !this.mProgressDialog.isShowing()) ? false : DBG);
    }

    @Override
    public void onFinished(Preference preference, boolean z) {
        if (this.mInitIndex < this.mPreferences.size() - 1 && !isFinishing()) {
            this.mInitIndex++;
            this.mPreferences.get(this.mInitIndex).init(this, false, this.mPhone);
        }
        super.onFinished(preference, z);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            CallFeaturesSetting.goUpToTopLevelSetting(this, this.mSubscriptionInfoHelper);
            return DBG;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onPrepareDialog(int i, Dialog dialog, Bundle bundle) {
        super.onPrepareDialog(i, dialog, bundle);
        if (i == 100 || i == 200) {
            this.mProgressDialog = dialog;
        }
    }

    @Override
    public void onDestroy() {
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        super.onDestroy();
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }
}
