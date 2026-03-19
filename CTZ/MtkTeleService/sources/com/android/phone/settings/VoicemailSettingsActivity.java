package com.android.phone.settings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncResult;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telecom.PhoneAccountHandle;
import android.telephony.CarrierConfigManager;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.Toast;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.Phone;
import com.android.phone.EditPhoneNumberPreference;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.android.phone.TimeConsumingPreferenceActivity;
import com.android.phone.settings.VoicemailProviderListPreference;
import com.mediatek.settings.CallSettingUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class VoicemailSettingsActivity extends PreferenceActivity implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener, EditPhoneNumberPreference.GetDefaultNumberListener, EditPhoneNumberPreference.OnDialogClosedListener {
    private static final String LOG_TAG = VoicemailSettingsActivity.class.getSimpleName();
    private boolean mForeground;
    private CallForwardInfo[] mNewFwdSettings;
    private String mNewVMNumber;
    private String mOldVmNumber;
    private Phone mPhone;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private Preference mVoicemailNotificationPreference;
    private VoicemailProviderListPreference mVoicemailProviders;
    private PreferenceScreen mVoicemailSettings;
    private CallForwardInfo[] mForwardingReadResults = null;
    private Map<Integer, AsyncResult> mForwardingChangeResults = null;
    private Collection<Integer> mExpectedChangeResultReasons = null;
    private AsyncResult mVoicemailChangeResult = null;
    private String mPreviousVMProviderKey = null;
    private int mCurrentDialogId = 0;
    private boolean mVMProviderSettingsForced = false;
    private boolean mChangingVMorFwdDueToProviderChange = false;
    private boolean mVMChangeCompletedSuccessfully = false;
    private boolean mFwdChangesRequireRollback = false;
    private int mVMOrFwdSetError = 0;
    private boolean mShowVoicemailPreference = false;
    private EditPhoneNumberPreference mSubMenuVoicemailSettings = null;
    private final Handler mGetOptionComplete = new Handler() {
        @Override
        public void handleMessage(Message message) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (message.what == 502) {
                VoicemailSettingsActivity.this.handleForwardingSettingsReadResult(asyncResult, message.arg1);
            }
        }
    };
    private final Handler mSetOptionComplete = new Handler() {
        @Override
        public void handleMessage(Message message) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            boolean z = true;
            switch (message.what) {
                case TimeConsumingPreferenceActivity.RADIO_OFF_ERROR:
                    VoicemailSettingsActivity.this.mVoicemailChangeResult = asyncResult;
                    VoicemailSettingsActivity.this.mVMChangeCompletedSuccessfully = VoicemailSettingsActivity.this.isVmChangeSuccess();
                    PhoneGlobals.getInstance().refreshMwiIndicator(VoicemailSettingsActivity.this.mSubscriptionInfoHelper.getSubId());
                    break;
                case 501:
                    VoicemailSettingsActivity.this.mForwardingChangeResults.put(Integer.valueOf(message.arg1), asyncResult);
                    if (asyncResult.exception != null) {
                        Log.w(VoicemailSettingsActivity.LOG_TAG, "Error in setting fwd# " + message.arg1 + ": " + asyncResult.exception.getMessage());
                    }
                    if (VoicemailSettingsActivity.this.isForwardingCompleted()) {
                        if (VoicemailSettingsActivity.this.isFwdChangeSuccess()) {
                            VoicemailSettingsActivity.log("Overall fwd changes completed ok, starting vm change");
                            VoicemailSettingsActivity.this.setVoicemailNumberWithCarrier();
                        } else {
                            Log.w(VoicemailSettingsActivity.LOG_TAG, "Overall fwd changes completed in failure. Check if we need to try rollback for some settings.");
                            VoicemailSettingsActivity.this.mFwdChangesRequireRollback = false;
                            Iterator it = VoicemailSettingsActivity.this.mForwardingChangeResults.entrySet().iterator();
                            while (true) {
                                if (it.hasNext()) {
                                    if (((AsyncResult) ((Map.Entry) it.next()).getValue()).exception == null) {
                                        Log.i(VoicemailSettingsActivity.LOG_TAG, "Rollback will be required");
                                        VoicemailSettingsActivity.this.mFwdChangesRequireRollback = true;
                                    }
                                }
                            }
                            if (!VoicemailSettingsActivity.this.mFwdChangesRequireRollback) {
                                Log.i(VoicemailSettingsActivity.LOG_TAG, "No rollback needed.");
                            }
                        }
                        break;
                    }
                default:
                    z = false;
                    break;
            }
            if (z) {
                VoicemailSettingsActivity.log("All VM provider related changes done");
                if (VoicemailSettingsActivity.this.mForwardingChangeResults != null) {
                    VoicemailSettingsActivity.this.dismissDialogSafely(601);
                }
                VoicemailSettingsActivity.this.handleSetVmOrFwdMessage();
            }
        }
    };
    private final Handler mRevertOptionComplete = new Handler() {
        @Override
        public void handleMessage(Message message) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            switch (message.what) {
                case TimeConsumingPreferenceActivity.RADIO_OFF_ERROR:
                    VoicemailSettingsActivity.log("VM revert complete msg");
                    VoicemailSettingsActivity.this.mVoicemailChangeResult = asyncResult;
                    break;
                case 501:
                    VoicemailSettingsActivity.log("FWD revert complete msg ");
                    VoicemailSettingsActivity.this.mForwardingChangeResults.put(Integer.valueOf(message.arg1), asyncResult);
                    if (asyncResult.exception != null) {
                        VoicemailSettingsActivity.log("Error in reverting fwd# " + message.arg1 + ": " + asyncResult.exception.getMessage());
                    }
                    break;
            }
            if (!(VoicemailSettingsActivity.this.mVMChangeCompletedSuccessfully && VoicemailSettingsActivity.this.mVoicemailChangeResult == null) && (!VoicemailSettingsActivity.this.mFwdChangesRequireRollback || VoicemailSettingsActivity.this.isForwardingCompleted())) {
                VoicemailSettingsActivity.log("All VM reverts done");
                VoicemailSettingsActivity.this.dismissDialogSafely(603);
                VoicemailSettingsActivity.this.onRevertDone();
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        boolean z = false;
        if (!((UserManager) getApplicationContext().getSystemService(UserManager.class)).isPrimaryUser()) {
            Toast.makeText(this, R.string.voice_number_setting_primary_user_only, 0).show();
            finish();
            return;
        }
        if (bundle == null && TextUtils.equals(getIntent().getAction(), "com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL")) {
            z = true;
        }
        this.mShowVoicemailPreference = z;
        PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) getIntent().getParcelableExtra("android.telephony.extra.PHONE_ACCOUNT_HANDLE");
        if (phoneAccountHandle != null) {
            getIntent().putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, PhoneUtils.getSubIdForPhoneAccountHandle(phoneAccountHandle));
        }
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        log("onCreate subId: " + this.mSubscriptionInfoHelper.getSubId());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.voicemail_settings_with_label);
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
        addPreferencesFromResource(R.xml.voicemail_settings);
        this.mVoicemailNotificationPreference = findPreference(getString(R.string.voicemail_notifications_key));
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent("android.settings.CHANNEL_NOTIFICATION_SETTINGS");
        intent.putExtra("android.provider.extra.CHANNEL_ID", "voiceMail");
        intent.putExtra("android.provider.extra.APP_PACKAGE", this.mPhone.getContext().getPackageName());
        this.mVoicemailNotificationPreference.setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mForeground = true;
        getPreferenceScreen();
        if (this.mSubMenuVoicemailSettings == null) {
            this.mSubMenuVoicemailSettings = (EditPhoneNumberPreference) findPreference("button_voicemail_key");
        }
        if (this.mSubMenuVoicemailSettings != null) {
            this.mSubMenuVoicemailSettings.setParentActivity(this, 1, this);
            this.mSubMenuVoicemailSettings.setDialogOnClosedListener(this);
            this.mSubMenuVoicemailSettings.setDialogTitle(R.string.voicemail_settings_number_label);
            if (!getBooleanCarrierConfig("editable_voicemail_number_setting_bool")) {
                this.mSubMenuVoicemailSettings.setEnabled(false);
            }
        }
        this.mVoicemailProviders = (VoicemailProviderListPreference) findPreference("button_voicemail_provider_key");
        this.mVoicemailProviders.init(this.mPhone, getIntent());
        this.mVoicemailProviders.setOnPreferenceChangeListener(this);
        this.mPreviousVMProviderKey = this.mVoicemailProviders.getValue();
        this.mVoicemailSettings = (PreferenceScreen) findPreference("button_voicemail_setting_key");
        maybeHidePublicSettings();
        updateVMPreferenceWidgets(this.mVoicemailProviders.getValue());
        if (this.mShowVoicemailPreference) {
            log("ACTION_ADD_VOICEMAIL Intent is thrown");
            if (this.mVoicemailProviders.hasMoreThanOneVoicemailProvider()) {
                log("Voicemail data has more than one provider.");
                simulatePreferenceClick(this.mVoicemailProviders);
            } else {
                onPreferenceChange(this.mVoicemailProviders, "");
                this.mVoicemailProviders.setValue("");
            }
            this.mShowVoicemailPreference = false;
        }
        updateVoiceNumberField();
        this.mVMProviderSettingsForced = false;
        Dialog dialog = this.mVoicemailSettings.getDialog();
        if (dialog != null) {
            dialog.getActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void maybeHidePublicSettings() {
        if (!getIntent().getBooleanExtra("android.telephony.extra.HIDE_PUBLIC_SETTINGS", false)) {
            return;
        }
        log("maybeHidePublicSettings: settings hidden by EXTRA_HIDE_PUBLIC_SETTINGS");
        getPreferenceScreen().removePreference(this.mVoicemailNotificationPreference);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mForeground = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == this.mSubMenuVoicemailSettings) {
            return true;
        }
        if (!preference.getKey().equals(this.mVoicemailSettings.getKey())) {
            return false;
        }
        log("onPreferenceTreeClick: Voicemail Settings Preference is clicked.");
        Dialog dialog = ((PreferenceScreen) preference).getDialog();
        if (dialog != null) {
            dialog.getActionBar().setDisplayHomeAsUpEnabled(false);
        }
        this.mSubMenuVoicemailSettings = (EditPhoneNumberPreference) findPreference("button_voicemail_key");
        this.mSubMenuVoicemailSettings.setParentActivity(this, 1, this);
        this.mSubMenuVoicemailSettings.setDialogOnClosedListener(this);
        this.mSubMenuVoicemailSettings.setDialogTitle(R.string.voicemail_settings_number_label);
        updateVoiceNumberField();
        if (preference.getIntent() != null) {
            log("Invoking cfg intent " + preference.getIntent().getPackage());
            startActivityForResult(preference.getIntent(), 2);
            return true;
        }
        log("onPreferenceTreeClick(). No intent; use default behavior in xml.");
        this.mPreviousVMProviderKey = "";
        this.mVMProviderSettingsForced = false;
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        log("onPreferenceChange: \"" + preference + "\" changed to \"" + obj + "\"");
        if (preference == this.mVoicemailProviders) {
            String str = (String) obj;
            if (this.mPreviousVMProviderKey.equals(str)) {
                log("No change is made to the VM provider setting.");
                return true;
            }
            updateVMPreferenceWidgets(str);
            VoicemailProviderSettings voicemailProviderSettingsLoad = VoicemailProviderSettingsUtil.load(this, str);
            if (voicemailProviderSettingsLoad == null) {
                Log.w(LOG_TAG, "Saved preferences not found - invoking config");
                this.mVMProviderSettingsForced = true;
                simulatePreferenceClick(this.mVoicemailSettings);
            } else {
                log("Saved preferences found - switching to them");
                this.mChangingVMorFwdDueToProviderChange = true;
                saveVoiceMailAndForwardingNumber(str, voicemailProviderSettingsLoad);
            }
        }
        return true;
    }

    @Override
    public String onGetDefaultNumber(EditPhoneNumberPreference editPhoneNumberPreference) {
        if (editPhoneNumberPreference == this.mSubMenuVoicemailSettings) {
            log("updating default for voicemail dialog");
            updateVoiceNumberField();
            return null;
        }
        String voiceMailNumber = this.mPhone.getVoiceMailNumber();
        if (TextUtils.isEmpty(voiceMailNumber)) {
            return null;
        }
        log("updating default for call forwarding dialogs");
        return getString(R.string.voicemail_abbreviated) + " " + voiceMailNumber;
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) throws Throwable {
        Cursor cursorQuery;
        log("onActivityResult: requestCode: " + i + ", resultCode: " + i2 + ", data: " + intent);
        String stringExtra = null;
        boolean z = true;
        if (i == 2) {
            log("mVMProviderSettingsForced: " + this.mVMProviderSettingsForced);
            boolean z2 = this.mVMProviderSettingsForced;
            this.mVMProviderSettingsForced = false;
            if (i2 != -1) {
                log("onActivityResult: vm provider cfg result not OK.");
            } else if (intent == null) {
                log("onActivityResult: vm provider cfg result has no data");
            } else {
                if (intent.getBooleanExtra("com.android.phone.Signout", false)) {
                    log("Provider requested signout");
                    if (z2) {
                        log("Going back to previous provider on signout");
                        switchToPreviousVoicemailProvider();
                        return;
                    }
                    String key = this.mVoicemailProviders.getKey();
                    log("Relaunching activity and ignoring " + key);
                    Intent intent2 = new Intent("com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL");
                    intent2.putExtra("com.android.phone.ProviderToIgnore", key);
                    intent2.setFlags(67108864);
                    startActivity(intent2);
                    return;
                }
                stringExtra = intent.getStringExtra("com.android.phone.VoicemailNumber");
                if (stringExtra == null || stringExtra.length() == 0) {
                    log("onActivityResult: vm provider cfg result has no vmnum");
                } else {
                    z = false;
                }
            }
            if (z) {
                log("Failure in return from voicemail provider.");
                if (z2) {
                    switchToPreviousVoicemailProvider();
                    return;
                }
                return;
            }
            this.mChangingVMorFwdDueToProviderChange = z2;
            String stringExtra2 = intent.getStringExtra("com.android.phone.ForwardingNumber");
            int intExtra = intent.getIntExtra("com.android.phone.ForwardingNumberTime", 20);
            log("onActivityResult: cfg result has forwarding number " + stringExtra2);
            saveVoiceMailAndForwardingNumber(this.mVoicemailProviders.getKey(), new VoicemailProviderSettings(stringExtra, stringExtra2, intExtra));
            return;
        }
        if (i != 1) {
            super.onActivityResult(i, i2, intent);
            return;
        }
        if (i2 != -1) {
            log("onActivityResult: contact picker result not OK.");
            return;
        }
        try {
            cursorQuery = getContentResolver().query(intent.getData(), new String[]{"data1"}, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        if (this.mSubMenuVoicemailSettings != null) {
                            this.mSubMenuVoicemailSettings.onPickActivityResult(cursorQuery.getString(0));
                        } else {
                            Log.w(LOG_TAG, "VoicemailSettingsActivity destroyed while setting contacts.");
                        }
                        if (cursorQuery != null) {
                            cursorQuery.close();
                            return;
                        }
                        return;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            log("onActivityResult: bad contact data, no results found.");
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private void simulatePreferenceClick(Preference preference) {
        ListAdapter rootAdapter = getPreferenceScreen().getRootAdapter();
        for (int i = 0; i < rootAdapter.getCount(); i++) {
            if (rootAdapter.getItem(i) == preference) {
                getPreferenceScreen().onItemClick(getListView(), null, i, rootAdapter.getItemId(i));
                return;
            }
        }
    }

    private boolean getBooleanCarrierConfig(String str) {
        PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId());
        if (carrierConfigForSubId != null) {
            return carrierConfigForSubId.getBoolean(str);
        }
        return CarrierConfigManager.getDefaultConfig().getBoolean(str);
    }

    @Override
    protected void onPrepareDialog(int i, Dialog dialog) {
        super.onPrepareDialog(i, dialog);
        this.mCurrentDialogId = i;
    }

    @Override
    protected Dialog onCreateDialog(int i) {
        return VoicemailDialogUtil.getDialog(this, i);
    }

    @Override
    public void onDialogClosed(EditPhoneNumberPreference editPhoneNumberPreference, int i) {
        log("onDialogClosed: Button clicked is " + i);
        if (i != -2 && editPhoneNumberPreference == this.mSubMenuVoicemailSettings) {
            saveVoiceMailAndForwardingNumber(this.mVoicemailProviders.getKey(), new VoicemailProviderSettings(this.mSubMenuVoicemailSettings.getPhoneNumber(), VoicemailProviderSettings.NO_FORWARDING));
        }
    }

    private void showDialogIfForeground(int i) {
        if (this.mForeground) {
            showDialog(i);
        }
    }

    private void dismissDialogSafely(int i) {
        try {
            dismissDialog(i);
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        log("onClick: button clicked is " + i);
        dialogInterface.dismiss();
        switch (i) {
            case -2:
                if (this.mCurrentDialogId == 502) {
                    switchToPreviousVoicemailProvider();
                }
                break;
            case SubscriptionInfoHelper.NO_SUB_ID:
                if (this.mCurrentDialogId == 502) {
                    setVoicemailNumberWithCarrier();
                    return;
                } else {
                    finish();
                    return;
                }
        }
        if ("com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL".equals(getIntent() != null ? getIntent().getAction() : null)) {
            finish();
        }
    }

    private void saveVoiceMailAndForwardingNumber(String str, VoicemailProviderSettings voicemailProviderSettings) {
        CallSettingUtils.sensitiveLog(LOG_TAG, "saveVoiceMailAndForwardingNumber: ", voicemailProviderSettings.toString());
        this.mNewVMNumber = voicemailProviderSettings.getVoicemailNumber();
        this.mNewVMNumber = this.mNewVMNumber == null ? "" : this.mNewVMNumber;
        this.mNewFwdSettings = voicemailProviderSettings.getForwardingSettings();
        if (this.mPhone.getPhoneType() == 2) {
            log("Ignoring forwarding setting since this is CDMA phone");
            this.mNewFwdSettings = VoicemailProviderSettings.NO_FORWARDING;
        }
        if (this.mNewVMNumber.equals(this.mOldVmNumber) && this.mNewFwdSettings == VoicemailProviderSettings.NO_FORWARDING) {
            showDialogIfForeground(TimeConsumingPreferenceActivity.RESPONSE_ERROR);
            return;
        }
        VoicemailProviderSettingsUtil.save(this, str, voicemailProviderSettings);
        this.mVMChangeCompletedSuccessfully = false;
        this.mFwdChangesRequireRollback = false;
        this.mVMOrFwdSetError = 0;
        if (this.mNewFwdSettings == VoicemailProviderSettings.NO_FORWARDING || str.equals(this.mPreviousVMProviderKey)) {
            log("Set voicemail number. No changes to forwarding number.");
            setVoicemailNumberWithCarrier();
            return;
        }
        log("Reading current forwarding settings.");
        this.mForwardingReadResults = new CallForwardInfo[VoicemailProviderSettings.FORWARDING_SETTINGS_REASONS.length];
        for (int i = 0; i < this.mForwardingReadResults.length; i++) {
            this.mPhone.getCallForwardingOption(VoicemailProviderSettings.FORWARDING_SETTINGS_REASONS[i], this.mGetOptionComplete.obtainMessage(502, i, 0));
        }
        showDialogIfForeground(602);
    }

    private void handleForwardingSettingsReadResult(AsyncResult asyncResult, int i) {
        Throwable th;
        Log.d(LOG_TAG, "handleForwardingSettingsReadResult: " + i);
        if (asyncResult.exception != null) {
            th = asyncResult.exception;
            Log.d(LOG_TAG, "FwdRead: ar.exception=" + th.getMessage());
        } else {
            th = null;
        }
        if (asyncResult.userObj instanceof Throwable) {
            th = (Throwable) asyncResult.userObj;
            Log.d(LOG_TAG, "FwdRead: userObj=" + th.getMessage());
        }
        if (this.mForwardingReadResults == null) {
            Log.d(LOG_TAG, "Ignoring fwd reading result: " + i);
            return;
        }
        if (th != null) {
            Log.d(LOG_TAG, "Error discovered for fwd read : " + i);
            this.mForwardingReadResults = null;
            dismissDialogSafely(602);
            showDialogIfForeground(502);
            return;
        }
        this.mForwardingReadResults[i] = CallForwardInfoUtil.getCallForwardInfo((CallForwardInfo[]) asyncResult.result, VoicemailProviderSettings.FORWARDING_SETTINGS_REASONS[i]);
        boolean z = false;
        int i2 = 0;
        while (true) {
            if (i2 < this.mForwardingReadResults.length) {
                if (this.mForwardingReadResults[i2] == null) {
                    break;
                } else {
                    i2++;
                }
            } else {
                z = true;
                break;
            }
        }
        if (z) {
            Log.d(LOG_TAG, "Done receiving fwd info");
            dismissDialogSafely(602);
            if (this.mPreviousVMProviderKey.equals("")) {
                VoicemailProviderSettingsUtil.save(this.mPhone.getContext(), "", new VoicemailProviderSettings(this.mOldVmNumber, this.mForwardingReadResults));
            }
            saveVoiceMailAndForwardingNumberStage2();
        }
    }

    private void resetForwardingChangeState() {
        this.mForwardingChangeResults = new HashMap();
        this.mExpectedChangeResultReasons = new HashSet();
    }

    private void saveVoiceMailAndForwardingNumberStage2() {
        this.mForwardingChangeResults = null;
        this.mVoicemailChangeResult = null;
        resetForwardingChangeState();
        for (int i = 0; i < this.mNewFwdSettings.length; i++) {
            CallForwardInfo callForwardInfo = this.mNewFwdSettings[i];
            if (CallForwardInfoUtil.isUpdateRequired(CallForwardInfoUtil.infoForReason(this.mForwardingReadResults, callForwardInfo.reason), callForwardInfo)) {
                log("Setting fwd #: " + i + ": " + callForwardInfo.toString());
                this.mExpectedChangeResultReasons.add(Integer.valueOf(i));
                CallForwardInfoUtil.setCallForwardingOption(this.mPhone, callForwardInfo, this.mSetOptionComplete.obtainMessage(501, callForwardInfo.reason, 0));
            }
        }
        showDialogIfForeground(601);
    }

    private void setVoicemailNumberWithCarrier() {
        CallSettingUtils.sensitiveLog(LOG_TAG, "save voicemail #: ", this.mNewVMNumber);
        this.mVoicemailChangeResult = null;
        this.mPhone.setVoiceMailNumber(this.mPhone.getVoiceMailAlphaTag().toString(), this.mNewVMNumber, Message.obtain(this.mSetOptionComplete, TimeConsumingPreferenceActivity.RADIO_OFF_ERROR));
    }

    private void switchToPreviousVoicemailProvider() {
        log("switchToPreviousVoicemailProvider " + this.mPreviousVMProviderKey);
        if (this.mPreviousVMProviderKey == null) {
            return;
        }
        if (this.mVMChangeCompletedSuccessfully || this.mFwdChangesRequireRollback) {
            showDialogIfForeground(603);
            VoicemailProviderSettings voicemailProviderSettingsLoad = VoicemailProviderSettingsUtil.load(this, this.mPreviousVMProviderKey);
            if (voicemailProviderSettingsLoad == null) {
                Log.e(LOG_TAG, "VoicemailProviderSettings for the key \"" + this.mPreviousVMProviderKey + "\" is null but should be loaded.");
                return;
            }
            if (this.mVMChangeCompletedSuccessfully) {
                this.mNewVMNumber = voicemailProviderSettingsLoad.getVoicemailNumber();
                Log.i(LOG_TAG, "VM change is already completed successfully.Have to revert VM back to " + this.mNewVMNumber + " again.");
                this.mPhone.setVoiceMailNumber(this.mPhone.getVoiceMailAlphaTag().toString(), this.mNewVMNumber, Message.obtain(this.mRevertOptionComplete, TimeConsumingPreferenceActivity.RADIO_OFF_ERROR));
            }
            if (this.mFwdChangesRequireRollback) {
                Log.i(LOG_TAG, "Requested to rollback forwarding changes.");
                CallForwardInfo[] forwardingSettings = voicemailProviderSettingsLoad.getForwardingSettings();
                if (forwardingSettings != null) {
                    Map<Integer, AsyncResult> map = this.mForwardingChangeResults;
                    resetForwardingChangeState();
                    for (int i = 0; i < forwardingSettings.length; i++) {
                        CallForwardInfo callForwardInfo = forwardingSettings[i];
                        log("Reverting fwd #: " + i + ": " + callForwardInfo.toString());
                        AsyncResult asyncResult = map.get(Integer.valueOf(callForwardInfo.reason));
                        if (asyncResult != null && asyncResult.exception == null) {
                            this.mExpectedChangeResultReasons.add(Integer.valueOf(callForwardInfo.reason));
                            CallForwardInfoUtil.setCallForwardingOption(this.mPhone, callForwardInfo, this.mRevertOptionComplete.obtainMessage(501, i, 0));
                        }
                    }
                    return;
                }
                return;
            }
            return;
        }
        log("No need to revert");
        onRevertDone();
    }

    private void updateVMPreferenceWidgets(String str) {
        VoicemailProviderListPreference.VoicemailProvider voicemailProvider = this.mVoicemailProviders.getVoicemailProvider(str);
        if (voicemailProvider == null) {
            log("updateVMPreferenceWidget: key: " + str + " -> null.");
            this.mVoicemailProviders.setSummary(getString(R.string.sum_voicemail_choose_provider));
            this.mVoicemailSettings.setEnabled(false);
            this.mVoicemailSettings.setIntent(null);
            return;
        }
        log("updateVMPreferenceWidget: key: " + str + " -> " + voicemailProvider.toString());
        this.mVoicemailProviders.setSummary(voicemailProvider.name);
        this.mVoicemailSettings.setEnabled(true);
        this.mVoicemailSettings.setIntent(voicemailProvider.intent);
    }

    private void updateVoiceNumberField() {
        this.mOldVmNumber = this.mPhone.getVoiceMailNumber();
        CallSettingUtils.sensitiveLog(LOG_TAG, "updateVoiceNumberField(), mOldVmNumber = ", this.mOldVmNumber);
        if (TextUtils.isEmpty(this.mOldVmNumber)) {
            this.mSubMenuVoicemailSettings.setPhoneNumber("");
            this.mSubMenuVoicemailSettings.setSummary(getString(R.string.voicemail_number_not_set));
        } else {
            this.mSubMenuVoicemailSettings.setPhoneNumber(this.mOldVmNumber);
            this.mSubMenuVoicemailSettings.setSummary(BidiFormatter.getInstance().unicodeWrap(this.mOldVmNumber, TextDirectionHeuristics.LTR));
        }
    }

    private void handleSetVmOrFwdMessage() {
        log("handleSetVMMessage: set VM request complete");
        if (!isFwdChangeSuccess()) {
            handleVmOrFwdSetError(501);
        } else if (!isVmChangeSuccess()) {
            handleVmOrFwdSetError(TimeConsumingPreferenceActivity.RADIO_OFF_ERROR);
        } else {
            handleVmAndFwdSetSuccess(TimeConsumingPreferenceActivity.FDN_CHECK_FAILURE);
        }
    }

    private void handleVmOrFwdSetError(int i) {
        if (this.mChangingVMorFwdDueToProviderChange) {
            this.mVMOrFwdSetError = i;
            this.mChangingVMorFwdDueToProviderChange = false;
            switchToPreviousVoicemailProvider();
        } else {
            this.mChangingVMorFwdDueToProviderChange = false;
            showDialogIfForeground(i);
            updateVoiceNumberField();
        }
    }

    private void handleVmAndFwdSetSuccess(int i) {
        log("handleVmAndFwdSetSuccess: key is " + this.mVoicemailProviders.getKey());
        this.mPreviousVMProviderKey = this.mVoicemailProviders.getKey();
        this.mChangingVMorFwdDueToProviderChange = false;
        showDialogIfForeground(i);
        updateVoiceNumberField();
    }

    private void onRevertDone() {
        log("onRevertDone: Changing provider key back to " + this.mPreviousVMProviderKey);
        updateVMPreferenceWidgets(this.mPreviousVMProviderKey);
        updateVoiceNumberField();
        if (this.mVMOrFwdSetError != 0) {
            showDialogIfForeground(this.mVMOrFwdSetError);
            this.mVMOrFwdSetError = 0;
        }
    }

    private boolean isForwardingCompleted() {
        if (this.mForwardingChangeResults == null) {
            return true;
        }
        Iterator<Integer> it = this.mExpectedChangeResultReasons.iterator();
        while (it.hasNext()) {
            if (this.mForwardingChangeResults.get(it.next()) == null) {
                return false;
            }
        }
        return true;
    }

    private boolean isFwdChangeSuccess() {
        if (this.mForwardingChangeResults == null) {
            return true;
        }
        Iterator<AsyncResult> it = this.mForwardingChangeResults.values().iterator();
        while (it.hasNext()) {
            Throwable th = it.next().exception;
            if (th != null) {
                String message = th.getMessage();
                if (message == null) {
                    message = "";
                }
                Log.w(LOG_TAG, "Failed to change forwarding setting. Reason: " + message);
                return false;
            }
        }
        return true;
    }

    private boolean isVmChangeSuccess() {
        if (this.mVoicemailChangeResult.exception != null) {
            String message = this.mVoicemailChangeResult.exception.getMessage();
            if (message == null) {
                message = "";
            }
            Log.w(LOG_TAG, "Failed to change voicemail. Reason: " + message);
            return false;
        }
        return true;
    }

    private static void log(String str) {
        Log.d(LOG_TAG, str);
    }
}
