package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.Toast;
import com.android.ims.ImsManager;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.Phone;
import com.android.phone.EditPhoneNumberPreference;
import com.android.phone.settings.CallForwardInfoUtil;
import com.android.phone.settings.TtyModeListPreference;
import com.android.phone.settings.VoicemailDialogUtil;
import com.android.phone.settings.VoicemailNotificationSettingsUtil;
import com.android.phone.settings.VoicemailProviderListPreference;
import com.android.phone.settings.VoicemailProviderSettings;
import com.android.phone.settings.VoicemailProviderSettingsUtil;
import com.android.phone.settings.VoicemailRingtonePreference;
import com.android.phone.settings.fdn.FdnSetting;
import com.android.services.telephony.sip.SipUtil;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class CallFeaturesSetting extends PreferenceActivity implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener, EditPhoneNumberPreference.GetDefaultNumberListener, EditPhoneNumberPreference.OnDialogClosedListener {
    private static final String[] NUM_PROJECTION = {"data1"};
    private AudioManager mAudioManager;
    private CheckBoxPreference mButtonAutoRetry;
    private ListPreference mButtonDTMF;
    private CheckBoxPreference mButtonHAC;
    private TtyModeListPreference mButtonTTY;
    private CheckBoxPreference mEnableVideoCalling;
    private boolean mForeground;
    private CallForwardInfo[] mNewFwdSettings;
    private String mNewVMNumber;
    private String mOldVmNumber;
    private Phone mPhone;
    private EditPhoneNumberPreference mSubMenuVoicemailSettings;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private VoicemailRingtonePreference mVoicemailNotificationRingtone;
    private CheckBoxPreference mVoicemailNotificationVibrate;
    private VoicemailProviderListPreference mVoicemailProviders;
    private PreferenceScreen mVoicemailSettings;
    private PreferenceScreen mVoicemailSettingsScreen;
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
    private boolean mReadingSettingsForDefaultProvider = false;
    private boolean mShowVoicemailPreference = false;
    private boolean mSetupVoicemail = false;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            CallFeaturesSetting.log("PhoneStateListener.onCallStateChanged: state=" + state);
            Preference pref = CallFeaturesSetting.this.getPreferenceScreen().findPreference("button_tty_mode_key");
            if (pref != null) {
                pref.setEnabled(state == 0);
            }
        }
    };
    private final Handler mGetOptionComplete = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult result = (AsyncResult) msg.obj;
            switch (msg.what) {
                case 502:
                    CallFeaturesSetting.this.handleForwardingSettingsReadResult(result, msg.arg1);
                    break;
            }
        }
    };
    private final Handler mSetOptionComplete = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult result = (AsyncResult) msg.obj;
            boolean done = false;
            switch (msg.what) {
                case 500:
                    CallFeaturesSetting.this.mVoicemailChangeResult = result;
                    CallFeaturesSetting.this.mVMChangeCompletedSuccessfully = CallFeaturesSetting.this.isVmChangeSuccess();
                    done = true;
                    break;
                case 501:
                    CallFeaturesSetting.this.mForwardingChangeResults.put(Integer.valueOf(msg.arg1), result);
                    if (result.exception != null) {
                        Log.w("CallFeaturesSetting", "Error in setting fwd# " + msg.arg1 + ": " + result.exception.getMessage());
                    }
                    if (CallFeaturesSetting.this.isForwardingCompleted()) {
                        if (CallFeaturesSetting.this.isFwdChangeSuccess()) {
                            CallFeaturesSetting.log("Overall fwd changes completed ok, starting vm change");
                            CallFeaturesSetting.this.setVMNumberWithCarrier();
                        } else {
                            Log.w("CallFeaturesSetting", "Overall fwd changes completed in failure. Check if we need to try rollback for some settings.");
                            CallFeaturesSetting.this.mFwdChangesRequireRollback = false;
                            Iterator<Map.Entry<Integer, AsyncResult>> it = CallFeaturesSetting.this.mForwardingChangeResults.entrySet().iterator();
                            while (true) {
                                if (it.hasNext()) {
                                    Map.Entry<Integer, AsyncResult> entry = it.next();
                                    if (entry.getValue().exception == null) {
                                        Log.i("CallFeaturesSetting", "Rollback will be required");
                                        CallFeaturesSetting.this.mFwdChangesRequireRollback = true;
                                    }
                                }
                            }
                            if (!CallFeaturesSetting.this.mFwdChangesRequireRollback) {
                                Log.i("CallFeaturesSetting", "No rollback needed.");
                            }
                            done = true;
                        }
                    }
                    break;
            }
            if (done) {
                CallFeaturesSetting.log("All VM provider related changes done");
                if (CallFeaturesSetting.this.mForwardingChangeResults != null) {
                    CallFeaturesSetting.this.dismissDialogSafely(601);
                }
                CallFeaturesSetting.this.handleSetVmOrFwdMessage();
            }
        }
    };
    private final Handler mRevertOptionComplete = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult result = (AsyncResult) msg.obj;
            switch (msg.what) {
                case 500:
                    CallFeaturesSetting.log("VM revert complete msg");
                    CallFeaturesSetting.this.mVoicemailChangeResult = result;
                    break;
                case 501:
                    CallFeaturesSetting.log("FWD revert complete msg ");
                    CallFeaturesSetting.this.mForwardingChangeResults.put(Integer.valueOf(msg.arg1), result);
                    if (result.exception != null) {
                        CallFeaturesSetting.log("Error in reverting fwd# " + msg.arg1 + ": " + result.exception.getMessage());
                    }
                    break;
            }
            boolean done = !(CallFeaturesSetting.this.mVMChangeCompletedSuccessfully && CallFeaturesSetting.this.mVoicemailChangeResult == null) && (!CallFeaturesSetting.this.mFwdChangesRequireRollback || CallFeaturesSetting.this.isForwardingCompleted());
            if (done) {
                CallFeaturesSetting.log("All VM reverts done");
                CallFeaturesSetting.this.dismissDialogSafely(603);
                CallFeaturesSetting.this.onRevertDone();
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        this.mForeground = false;
        if (ImsManager.isVolteEnabledByPlatform(this) && !this.mPhone.getContext().getResources().getBoolean(android.R.^attr-private.lightRadius)) {
            TelephonyManager tm = (TelephonyManager) getSystemService("phone");
            tm.listen(this.mPhoneStateListener, 0);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == this.mSubMenuVoicemailSettings || preference == this.mButtonDTMF || preference == this.mButtonTTY) {
            return true;
        }
        if (preference == this.mButtonAutoRetry) {
            Settings.Global.putInt(this.mPhone.getContext().getContentResolver(), "call_auto_retry", this.mButtonAutoRetry.isChecked() ? 1 : 0);
            return true;
        }
        if (preference == this.mButtonHAC) {
            int hac = this.mButtonHAC.isChecked() ? 1 : 0;
            Settings.System.putInt(this.mPhone.getContext().getContentResolver(), "hearing_aid", hac);
            this.mAudioManager.setParameter("HACSetting", hac != 0 ? "ON" : "OFF");
            return true;
        }
        if (preference.getKey().equals(this.mVoicemailSettings.getKey())) {
            log("onPreferenceTreeClick: Voicemail Settings Preference is clicked.");
            Dialog dialog = ((PreferenceScreen) preference).getDialog();
            if (dialog != null) {
                dialog.getActionBar().setDisplayHomeAsUpEnabled(false);
            }
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
        if (preference != this.mVoicemailSettingsScreen) {
            return false;
        }
        Dialog dialog2 = this.mVoicemailSettingsScreen.getDialog();
        if (dialog2 != null) {
            dialog2.getActionBar().setDisplayHomeAsUpEnabled(false);
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        log("onPreferenceChange: \"" + preference + "\" changed to \"" + objValue + "\"");
        if (preference == this.mButtonDTMF) {
            int index = this.mButtonDTMF.findIndexOfValue((String) objValue);
            Settings.System.putInt(this.mPhone.getContext().getContentResolver(), "dtmf_tone_type", index);
            return true;
        }
        if (preference == this.mVoicemailProviders) {
            String newProviderKey = (String) objValue;
            if (this.mPreviousVMProviderKey.equals(newProviderKey)) {
                log("No change is made to the VM provider setting.");
                return true;
            }
            updateVMPreferenceWidgets(newProviderKey);
            VoicemailProviderSettings newProviderSettings = VoicemailProviderSettingsUtil.load(this, newProviderKey);
            if (newProviderSettings == null) {
                Log.w("CallFeaturesSetting", "Saved preferences not found - invoking config");
                this.mVMProviderSettingsForced = true;
                simulatePreferenceClick(this.mVoicemailSettings);
                return true;
            }
            log("Saved preferences found - switching to them");
            this.mChangingVMorFwdDueToProviderChange = true;
            saveVoiceMailAndForwardingNumber(newProviderKey, newProviderSettings);
            return true;
        }
        if (preference.getKey().equals(this.mVoicemailNotificationVibrate.getKey())) {
            VoicemailNotificationSettingsUtil.setVibrationEnabled(this.mPhone, Boolean.TRUE.equals(objValue));
            return true;
        }
        if (preference != this.mEnableVideoCalling) {
            return true;
        }
        if (ImsManager.isEnhanced4gLteModeSettingEnabledByUser(this.mPhone.getContext())) {
            PhoneGlobals.getInstance().phoneMgr.enableVideoCalling(((Boolean) objValue).booleanValue());
            return true;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogInterface.OnClickListener networkSettingsClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CallFeaturesSetting.this.startActivity(new Intent(CallFeaturesSetting.this.mPhone.getContext(), (Class<?>) MobileNetworkSettings.class));
            }
        };
        builder.setMessage(getResources().getString(R.string.enable_video_calling_dialog_msg)).setNeutralButton(getResources().getString(R.string.enable_video_calling_dialog_settings), networkSettingsClickListener).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
        return false;
    }

    @Override
    public void onDialogClosed(EditPhoneNumberPreference preference, int buttonClicked) {
        log("onDialogClosed: Button clicked is " + buttonClicked);
        if (buttonClicked != -2 && preference == this.mSubMenuVoicemailSettings) {
            VoicemailProviderSettings newSettings = new VoicemailProviderSettings(this.mSubMenuVoicemailSettings.getPhoneNumber(), VoicemailProviderSettings.NO_FORWARDING);
            saveVoiceMailAndForwardingNumber(this.mVoicemailProviders.getKey(), newSettings);
        }
    }

    @Override
    public String onGetDefaultNumber(EditPhoneNumberPreference preference) {
        if (preference == this.mSubMenuVoicemailSettings) {
            log("updating default for voicemail dialog");
            updateVoiceNumberField();
            return null;
        }
        String vmDisplay = this.mPhone.getVoiceMailNumber();
        if (TextUtils.isEmpty(vmDisplay)) {
            return null;
        }
        log("updating default for call forwarding dialogs");
        return getString(R.string.voicemail_abbreviated) + " " + vmDisplay;
    }

    private void switchToPreviousVoicemailProvider() {
        log("switchToPreviousVoicemailProvider " + this.mPreviousVMProviderKey);
        if (this.mPreviousVMProviderKey != null) {
            if (this.mVMChangeCompletedSuccessfully || this.mFwdChangesRequireRollback) {
                showDialogIfForeground(603);
                VoicemailProviderSettings prevSettings = VoicemailProviderSettingsUtil.load(this, this.mPreviousVMProviderKey);
                if (prevSettings == null) {
                    Log.e("CallFeaturesSetting", "VoicemailProviderSettings for the key \"" + this.mPreviousVMProviderKey + "\" is null but should be loaded.");
                }
                if (this.mVMChangeCompletedSuccessfully) {
                    this.mNewVMNumber = prevSettings.getVoicemailNumber();
                    Log.i("CallFeaturesSetting", "VM change is already completed successfully.Have to revert VM back to " + this.mNewVMNumber + " again.");
                    this.mPhone.setVoiceMailNumber(this.mPhone.getVoiceMailAlphaTag().toString(), this.mNewVMNumber, Message.obtain(this.mRevertOptionComplete, 500));
                }
                if (this.mFwdChangesRequireRollback) {
                    Log.i("CallFeaturesSetting", "Requested to rollback forwarding changes.");
                    CallForwardInfo[] prevFwdSettings = prevSettings.getForwardingSettings();
                    if (prevFwdSettings != null) {
                        Map<Integer, AsyncResult> results = this.mForwardingChangeResults;
                        resetForwardingChangeState();
                        for (int i = 0; i < prevFwdSettings.length; i++) {
                            CallForwardInfo fi = prevFwdSettings[i];
                            log("Reverting fwd #: " + i + ": " + fi.toString());
                            AsyncResult result = results.get(Integer.valueOf(fi.reason));
                            if (result != null && result.exception == null) {
                                this.mExpectedChangeResultReasons.add(Integer.valueOf(fi.reason));
                                CallForwardInfoUtil.setCallForwardingOption(this.mPhone, fi, this.mRevertOptionComplete.obtainMessage(501, i, 0));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        log("onActivityResult: requestCode: " + requestCode + ", resultCode: " + resultCode + ", data: " + data);
        if (requestCode == 2) {
            boolean failure = false;
            log("mVMProviderSettingsForced: " + this.mVMProviderSettingsForced);
            boolean isVMProviderSettingsForced = this.mVMProviderSettingsForced;
            this.mVMProviderSettingsForced = false;
            String vmNum = null;
            if (resultCode != -1) {
                log("onActivityResult: vm provider cfg result not OK.");
                failure = true;
            } else if (data == null) {
                log("onActivityResult: vm provider cfg result has no data");
                failure = true;
            } else {
                if (data.getBooleanExtra("com.android.phone.Signout", false)) {
                    log("Provider requested signout");
                    if (isVMProviderSettingsForced) {
                        log("Going back to previous provider on signout");
                        switchToPreviousVoicemailProvider();
                        return;
                    }
                    String victim = this.mVoicemailProviders.getKey();
                    log("Relaunching activity and ignoring " + victim);
                    Intent i = new Intent("com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL");
                    i.putExtra("com.android.phone.ProviderToIgnore", victim);
                    i.setFlags(67108864);
                    startActivity(i);
                    return;
                }
                vmNum = data.getStringExtra("com.android.phone.VoicemailNumber");
                if (vmNum == null || vmNum.length() == 0) {
                    log("onActivityResult: vm provider cfg result has no vmnum");
                    failure = true;
                }
            }
            if (failure) {
                log("Failure in return from voicemail provider.");
                if (isVMProviderSettingsForced) {
                    switchToPreviousVoicemailProvider();
                    return;
                }
                return;
            }
            this.mChangingVMorFwdDueToProviderChange = isVMProviderSettingsForced;
            String fwdNum = data.getStringExtra("com.android.phone.ForwardingNumber");
            int fwdNumTime = data.getIntExtra("com.android.phone.ForwardingNumberTime", 20);
            log("onActivityResult: cfg result has forwarding number " + fwdNum);
            saveVoiceMailAndForwardingNumber(this.mVoicemailProviders.getKey(), new VoicemailProviderSettings(vmNum, fwdNum, fwdNumTime));
            return;
        }
        if (requestCode == 1) {
            if (resultCode != -1) {
                log("onActivityResult: contact picker result not OK.");
                return;
            }
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(data.getData(), NUM_PROJECTION, null, null, null);
                if (cursor == null || !cursor.moveToFirst()) {
                    log("onActivityResult: bad contact data, no results found.");
                } else {
                    this.mSubMenuVoicemailSettings.onPickActivityResult(cursor.getString(0));
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showDialogIfForeground(int id) {
        if (this.mForeground) {
            showDialog(id);
        }
    }

    private void dismissDialogSafely(int id) {
        try {
            dismissDialog(id);
        } catch (IllegalArgumentException e) {
        }
    }

    private void saveVoiceMailAndForwardingNumber(String key, VoicemailProviderSettings newSettings) {
        log("saveVoiceMailAndForwardingNumber: " + newSettings.toString());
        this.mNewVMNumber = newSettings.getVoicemailNumber();
        this.mNewVMNumber = this.mNewVMNumber == null ? "" : this.mNewVMNumber;
        this.mNewFwdSettings = newSettings.getForwardingSettings();
        boolean isCdma = this.mPhone.getPhoneType() == 2;
        if (isCdma) {
            log("ignoring forwarding setting since this is CDMA phone");
            this.mNewFwdSettings = VoicemailProviderSettings.NO_FORWARDING;
        }
        if (this.mNewVMNumber.equals(this.mOldVmNumber) && this.mNewFwdSettings == VoicemailProviderSettings.NO_FORWARDING) {
            showDialogIfForeground(400);
            return;
        }
        VoicemailProviderSettingsUtil.save(this, key, newSettings);
        this.mVMChangeCompletedSuccessfully = false;
        this.mFwdChangesRequireRollback = false;
        this.mVMOrFwdSetError = 0;
        if (!key.equals(this.mPreviousVMProviderKey) && !isCdma) {
            this.mReadingSettingsForDefaultProvider = this.mPreviousVMProviderKey.equals("");
            log("Reading current forwarding settings");
            int numSettingsReasons = VoicemailProviderSettings.FORWARDING_SETTINGS_REASONS.length;
            this.mForwardingReadResults = new CallForwardInfo[numSettingsReasons];
            for (int i = 0; i < this.mForwardingReadResults.length; i++) {
                this.mPhone.getCallForwardingOption(VoicemailProviderSettings.FORWARDING_SETTINGS_REASONS[i], this.mGetOptionComplete.obtainMessage(502, i, 0));
            }
            showDialogIfForeground(602);
        } else {
            saveVoiceMailAndForwardingNumberStage2();
        }
        PhoneGlobals.getInstance().refreshMwiIndicator(this.mSubscriptionInfoHelper.getSubId());
    }

    private void handleForwardingSettingsReadResult(AsyncResult ar, int idx) {
        Log.d("CallFeaturesSetting", "handleForwardingSettingsReadResult: " + idx);
        Throwable error = null;
        if (ar.exception != null) {
            error = ar.exception;
            Log.d("CallFeaturesSetting", "FwdRead: ar.exception=" + error.getMessage());
        }
        if (ar.userObj instanceof Throwable) {
            error = (Throwable) ar.userObj;
            Log.d("CallFeaturesSetting", "FwdRead: userObj=" + error.getMessage());
        }
        if (this.mForwardingReadResults == null) {
            Log.d("CallFeaturesSetting", "Ignoring fwd reading result: " + idx);
            return;
        }
        if (error != null) {
            Log.d("CallFeaturesSetting", "Error discovered for fwd read : " + idx);
            this.mForwardingReadResults = null;
            dismissDialogSafely(602);
            showDialogIfForeground(502);
            return;
        }
        this.mForwardingReadResults[idx] = CallForwardInfoUtil.getCallForwardInfo((CallForwardInfo[]) ar.result, VoicemailProviderSettings.FORWARDING_SETTINGS_REASONS[idx]);
        boolean done = true;
        int i = 0;
        while (true) {
            if (i >= this.mForwardingReadResults.length) {
                break;
            }
            if (this.mForwardingReadResults[i] != null) {
                i++;
            } else {
                done = false;
                break;
            }
        }
        if (done) {
            Log.d("CallFeaturesSetting", "Done receiving fwd info");
            dismissDialogSafely(602);
            if (this.mReadingSettingsForDefaultProvider) {
                VoicemailProviderSettingsUtil.save(this.mPhone.getContext(), "", new VoicemailProviderSettings(this.mOldVmNumber, this.mForwardingReadResults));
                this.mReadingSettingsForDefaultProvider = false;
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
        if (this.mNewFwdSettings != VoicemailProviderSettings.NO_FORWARDING) {
            resetForwardingChangeState();
            for (int i = 0; i < this.mNewFwdSettings.length; i++) {
                CallForwardInfo fi = this.mNewFwdSettings[i];
                CallForwardInfo fiForReason = CallForwardInfoUtil.infoForReason(this.mForwardingReadResults, fi.reason);
                boolean doUpdate = CallForwardInfoUtil.isUpdateRequired(fiForReason, fi);
                if (doUpdate) {
                    log("Setting fwd #: " + i + ": " + fi.toString());
                    this.mExpectedChangeResultReasons.add(Integer.valueOf(i));
                    CallForwardInfoUtil.setCallForwardingOption(this.mPhone, fi, this.mSetOptionComplete.obtainMessage(501, fi.reason, 0));
                }
            }
            showDialogIfForeground(601);
            return;
        }
        log("Not touching fwd #");
        setVMNumberWithCarrier();
    }

    private void setVMNumberWithCarrier() {
        log("save voicemail #: " + this.mNewVMNumber);
        this.mPhone.setVoiceMailNumber(this.mPhone.getVoiceMailAlphaTag().toString(), this.mNewVMNumber, Message.obtain(this.mSetOptionComplete, 500));
    }

    private boolean isForwardingCompleted() {
        if (this.mForwardingChangeResults == null) {
            return true;
        }
        for (Integer reason : this.mExpectedChangeResultReasons) {
            if (this.mForwardingChangeResults.get(reason) == null) {
                return false;
            }
        }
        return true;
    }

    private boolean isFwdChangeSuccess() {
        if (this.mForwardingChangeResults == null) {
            return true;
        }
        for (AsyncResult result : this.mForwardingChangeResults.values()) {
            Throwable exception = result.exception;
            if (exception != null) {
                String msg = exception.getMessage();
                if (msg == null) {
                    msg = "";
                }
                Log.w("CallFeaturesSetting", "Failed to change forwarding setting. Reason: " + msg);
                return false;
            }
        }
        return true;
    }

    private boolean isVmChangeSuccess() {
        if (this.mVoicemailChangeResult.exception != null) {
            String msg = this.mVoicemailChangeResult.exception.getMessage();
            if (msg == null) {
                msg = "";
            }
            Log.w("CallFeaturesSetting", "Failed to change voicemail. Reason: " + msg);
            return false;
        }
        return true;
    }

    private void handleSetVmOrFwdMessage() {
        log("handleSetVMMessage: set VM request complete");
        if (!isFwdChangeSuccess()) {
            handleVmOrFwdSetError(501);
        } else if (!isVmChangeSuccess()) {
            handleVmOrFwdSetError(500);
        } else {
            handleVmAndFwdSetSuccess(600);
        }
    }

    private void handleVmOrFwdSetError(int dialogId) {
        if (this.mChangingVMorFwdDueToProviderChange) {
            this.mVMOrFwdSetError = dialogId;
            this.mChangingVMorFwdDueToProviderChange = false;
            switchToPreviousVoicemailProvider();
        } else {
            this.mChangingVMorFwdDueToProviderChange = false;
            showDialogIfForeground(dialogId);
            updateVoiceNumberField();
        }
    }

    private void handleVmAndFwdSetSuccess(int dialogId) {
        log("handleVmAndFwdSetSuccess: key is " + this.mVoicemailProviders.getKey());
        this.mPreviousVMProviderKey = this.mVoicemailProviders.getKey();
        this.mChangingVMorFwdDueToProviderChange = false;
        showDialogIfForeground(dialogId);
        updateVoiceNumberField();
    }

    private void updateVoiceNumberField() {
        log("updateVoiceNumberField()");
        this.mOldVmNumber = this.mPhone.getVoiceMailNumber();
        if (TextUtils.isEmpty(this.mOldVmNumber)) {
            this.mSubMenuVoicemailSettings.setPhoneNumber("");
            this.mSubMenuVoicemailSettings.setSummary(getString(R.string.voicemail_number_not_set));
        } else {
            this.mSubMenuVoicemailSettings.setPhoneNumber(this.mOldVmNumber);
            this.mSubMenuVoicemailSettings.setSummary(this.mOldVmNumber);
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        this.mCurrentDialogId = id;
    }

    @Override
    protected Dialog onCreateDialog(int dialogId) {
        return VoicemailDialogUtil.getDialog(this, dialogId);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        log("onClick: button clicked is " + which);
        dialog.dismiss();
        switch (which) {
            case -2:
                if (this.mCurrentDialogId == 502) {
                    switchToPreviousVoicemailProvider();
                }
                break;
            case -1:
                if (this.mCurrentDialogId == 502) {
                    saveVoiceMailAndForwardingNumberStage2();
                    return;
                } else {
                    finish();
                    return;
                }
        }
        if (getIntent().getAction().equals("com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL")) {
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        log("onCreate: Intent is " + getIntent());
        if (UserHandle.myUserId() != 0) {
            Toast.makeText(this, R.string.call_settings_primary_user_only, 0).show();
            finish();
            return;
        }
        this.mAudioManager = (AudioManager) getSystemService("audio");
        this.mShowVoicemailPreference = icicle == null && TextUtils.equals(getIntent().getAction(), "com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL");
        this.mSetupVoicemail = this.mShowVoicemailPreference && getIntent().getBooleanExtra("com.android.phone.SetupVoicemail", false);
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.call_settings_with_label);
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mForeground = true;
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }
        addPreferencesFromResource(R.xml.call_feature_setting);
        TelecomManager telecomManager = TelecomManager.from(this);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService("phone");
        Preference phoneAccountSettingsPreference = findPreference("phone_account_settings_preference_screen");
        if (telephonyManager.isMultiSimEnabled() || (telecomManager.getSimCallManagers().isEmpty() && !SipUtil.isVoipSupported(this.mPhone.getContext()))) {
            getPreferenceScreen().removePreference(phoneAccountSettingsPreference);
        }
        PreferenceScreen prefSet = getPreferenceScreen();
        this.mSubMenuVoicemailSettings = (EditPhoneNumberPreference) findPreference("button_voicemail_key");
        this.mSubMenuVoicemailSettings.setParentActivity(this, 1, this);
        this.mSubMenuVoicemailSettings.setDialogOnClosedListener(this);
        this.mSubMenuVoicemailSettings.setDialogTitle(R.string.voicemail_settings_number_label);
        this.mButtonDTMF = (ListPreference) findPreference("button_dtmf_settings");
        this.mButtonAutoRetry = (CheckBoxPreference) findPreference("button_auto_retry_key");
        this.mButtonHAC = (CheckBoxPreference) findPreference("button_hac_key");
        this.mButtonTTY = (TtyModeListPreference) findPreference(getResources().getString(R.string.tty_mode_key));
        this.mVoicemailProviders = (VoicemailProviderListPreference) findPreference("button_voicemail_provider_key");
        this.mVoicemailProviders.init(this.mPhone, getIntent());
        this.mVoicemailProviders.setOnPreferenceChangeListener(this);
        this.mPreviousVMProviderKey = this.mVoicemailProviders.getValue();
        this.mVoicemailSettingsScreen = (PreferenceScreen) findPreference("button_voicemail_category_key");
        this.mVoicemailSettings = (PreferenceScreen) findPreference("button_voicemail_setting_key");
        this.mVoicemailNotificationRingtone = (VoicemailRingtonePreference) findPreference(getResources().getString(R.string.voicemail_notification_ringtone_key));
        this.mVoicemailNotificationRingtone.init(this.mPhone);
        this.mVoicemailNotificationVibrate = (CheckBoxPreference) findPreference(getResources().getString(R.string.voicemail_notification_vibrate_key));
        this.mVoicemailNotificationVibrate.setOnPreferenceChangeListener(this);
        updateVMPreferenceWidgets(this.mVoicemailProviders.getValue());
        this.mEnableVideoCalling = (CheckBoxPreference) findPreference("button_enable_video_calling");
        if (getResources().getBoolean(R.bool.dtmf_type_enabled)) {
            this.mButtonDTMF.setOnPreferenceChangeListener(this);
            int dtmf = Settings.System.getInt(getContentResolver(), "dtmf_tone_type", 0);
            this.mButtonDTMF.setValueIndex(dtmf);
        } else {
            prefSet.removePreference(this.mButtonDTMF);
            this.mButtonDTMF = null;
        }
        if (getResources().getBoolean(R.bool.auto_retry_enabled)) {
            this.mButtonAutoRetry.setOnPreferenceChangeListener(this);
            int autoretry = Settings.Global.getInt(getContentResolver(), "call_auto_retry", 0);
            this.mButtonAutoRetry.setChecked(autoretry != 0);
        } else {
            prefSet.removePreference(this.mButtonAutoRetry);
            this.mButtonAutoRetry = null;
        }
        if (getResources().getBoolean(R.bool.hac_enabled)) {
            this.mButtonHAC.setOnPreferenceChangeListener(this);
            int hac = Settings.System.getInt(getContentResolver(), "hearing_aid", 0);
            this.mButtonHAC.setChecked(hac != 0);
        } else {
            prefSet.removePreference(this.mButtonHAC);
            this.mButtonHAC = null;
        }
        if (!telephonyManager.isMultiSimEnabled() && telecomManager.isTtySupported()) {
            this.mButtonTTY.init();
        } else {
            prefSet.removePreference(this.mButtonTTY);
            this.mButtonTTY = null;
        }
        if (!getResources().getBoolean(R.bool.world_phone)) {
            Preference cdmaOptions = prefSet.findPreference("button_cdma_more_expand_key");
            prefSet.removePreference(cdmaOptions);
            Preference gsmOptions = prefSet.findPreference("button_gsm_more_expand_key");
            prefSet.removePreference(gsmOptions);
            int phoneType = this.mPhone.getPhoneType();
            Preference fdnButton = prefSet.findPreference("button_fdn_key");
            boolean shouldHideCarrierSettings = Settings.Global.getInt(getContentResolver(), "hide_carrier_network_settings", 0) == 1;
            if (shouldHideCarrierSettings) {
                prefSet.removePreference(fdnButton);
                if (this.mButtonDTMF != null) {
                    prefSet.removePreference(this.mButtonDTMF);
                }
            } else if (phoneType == 2) {
                prefSet.removePreference(fdnButton);
                if (!getResources().getBoolean(R.bool.config_voice_privacy_disable)) {
                    addPreferencesFromResource(R.xml.cdma_call_privacy);
                }
            } else if (phoneType == 1) {
                fdnButton.setIntent(this.mSubscriptionInfoHelper.getIntent(FdnSetting.class));
                if (getResources().getBoolean(R.bool.config_additional_call_setting)) {
                    addPreferencesFromResource(R.xml.gsm_umts_call_options);
                    Preference callForwardingPref = prefSet.findPreference("call_forwarding_key");
                    callForwardingPref.setIntent(this.mSubscriptionInfoHelper.getIntent(GsmUmtsCallForwardOptions.class));
                    Preference additionalGsmSettingsPref = prefSet.findPreference("additional_gsm_call_settings_key");
                    additionalGsmSettingsPref.setIntent(this.mSubscriptionInfoHelper.getIntent(GsmUmtsAdditionalCallOptions.class));
                }
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
        }
        if (this.mShowVoicemailPreference) {
            log("ACTION_ADD_VOICEMAIL Intent is thrown");
            if (this.mSetupVoicemail) {
                simulatePreferenceClick(this.mVoicemailSettingsScreen);
                this.mSetupVoicemail = false;
            } else if (this.mVoicemailProviders.hasMoreThanOneVoicemailProvider()) {
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
        this.mVoicemailNotificationVibrate.setChecked(VoicemailNotificationSettingsUtil.isVibrationEnabled(this.mPhone));
        if (ImsManager.isVtEnabledByPlatform(this.mPhone.getContext())) {
        }
        prefSet.removePreference(this.mEnableVideoCalling);
        if (ImsManager.isVolteEnabledByPlatform(this) && !this.mPhone.getContext().getResources().getBoolean(android.R.^attr-private.lightRadius)) {
            TelephonyManager tm = (TelephonyManager) getSystemService("phone");
            tm.listen(this.mPhoneStateListener, 32);
        }
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        setIntent(newIntent);
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.call_settings_with_label);
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
    }

    private static void log(String msg) {
        Log.d("CallFeaturesSetting", msg);
    }

    private void updateVMPreferenceWidgets(String currentProviderSetting) {
        VoicemailProviderListPreference.VoicemailProvider provider = this.mVoicemailProviders.getVoicemailProvider(currentProviderSetting);
        if (provider == null) {
            log("updateVMPreferenceWidget: key: " + currentProviderSetting + " -> null.");
            this.mVoicemailProviders.setSummary(getString(R.string.sum_voicemail_choose_provider));
            this.mVoicemailSettings.setEnabled(false);
            this.mVoicemailSettings.setIntent(null);
            this.mVoicemailNotificationVibrate.setEnabled(false);
            return;
        }
        log("updateVMPreferenceWidget: key: " + currentProviderSetting + " -> " + provider.toString());
        String providerName = provider.name;
        this.mVoicemailProviders.setSummary(providerName);
        this.mVoicemailSettings.setEnabled(true);
        this.mVoicemailSettings.setIntent(provider.intent);
        this.mVoicemailNotificationVibrate.setEnabled(true);
    }

    private void simulatePreferenceClick(Preference preference) {
        ListAdapter adapter = getPreferenceScreen().getRootAdapter();
        for (int idx = 0; idx < adapter.getCount(); idx++) {
            if (adapter.getItem(idx) == preference) {
                getPreferenceScreen().onItemClick(getListView(), null, idx, adapter.getItemId(idx));
                return;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        onBackPressed();
        return true;
    }

    public static void goUpToTopLevelSetting(Activity activity, SubscriptionInfoHelper subscriptionInfoHelper) {
        Intent intent = subscriptionInfoHelper.getIntent(CallFeaturesSetting.class);
        intent.setAction("android.intent.action.MAIN");
        intent.addFlags(67108864);
        activity.startActivity(intent);
        activity.finish();
    }
}
