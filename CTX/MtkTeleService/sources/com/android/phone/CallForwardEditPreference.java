package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.SpannableString;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.CallSettingUtils;
import com.mediatek.settings.MobileDataDialogFragment;
import com.mediatek.settings.MobileDataDialogInterface;
import com.mediatek.settings.cdma.CdmaCallForwardOptions;
import com.mediatek.settings.cdma.TelephonyUtilsEx;

public class CallForwardEditPreference extends EditPhoneNumberPreference implements MobileDataDialogInterface {
    private static final int DELAY_TIME = 1500;
    private static final String LOG_TAG = "CallForwardEditPreference";
    private static final String[] SRC_TAGS = {"{0}"};
    CallForwardInfo callForwardInfo;
    private int mButtonClicked;
    private boolean mEnableVolteTips;
    private MyHandler mHandler;
    private boolean mHasUtError;
    private Phone mPhone;
    private boolean mReplaceInvalidCFNumber;
    private int mServiceClass;
    private CharSequence mSummaryOnTemplate;
    private TimeConsumingPreferenceListener mTcpListener;
    int reason;

    public CallForwardEditPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mHandler = new MyHandler();
        this.mReplaceInvalidCFNumber = false;
        this.mHasUtError = false;
        this.mEnableVolteTips = true;
        this.mSummaryOnTemplate = getSummaryOn();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.CallForwardEditPreference, 0, R.style.EditPhoneNumberPreference);
        this.mServiceClass = typedArrayObtainStyledAttributes.getInt(1, 1);
        this.reason = typedArrayObtainStyledAttributes.getInt(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        Log.d(LOG_TAG, "mServiceClass=" + this.mServiceClass + ", reason=" + this.reason);
    }

    public CallForwardEditPreference(Context context) {
        this(context, null);
    }

    void init(TimeConsumingPreferenceListener timeConsumingPreferenceListener, boolean z, Phone phone, boolean z2) {
        this.mPhone = phone;
        this.mTcpListener = timeConsumingPreferenceListener;
        this.mReplaceInvalidCFNumber = z2;
        if (!z) {
            getCallForwardingOption(0, 0, null);
            if (this.mTcpListener != null) {
                this.mTcpListener.onStarted(this, true);
                return;
            }
            return;
        }
        updateSummaryText();
    }

    @Override
    protected void onBindDialogView(View view) {
        this.mButtonClicked = -2;
        super.onBindDialogView(view);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        CallSettingUtils.DialogType dialogTipsType;
        if (i != -2 && ((dialogTipsType = CallSettingUtils.getDialogTipsType(getContext(), this.mPhone.getSubId())) == CallSettingUtils.DialogType.DATA_OPEN || dialogTipsType == CallSettingUtils.DialogType.DATA_ROAMING)) {
            CallSettingUtils.showDialogTips(getContext(), this.mPhone.getSubId(), dialogTipsType, null);
        } else {
            super.onClick(dialogInterface, i);
            this.mButtonClicked = i;
        }
    }

    @Override
    protected void onDialogClosed(boolean z) {
        super.onDialogClosed(z);
        Log.d(LOG_TAG, "mButtonClicked=" + this.mButtonClicked + ", positiveResult=" + z);
        if (this.mButtonClicked != -2) {
            int i = (isToggled() || this.mButtonClicked == -1) ? 3 : 0;
            int i2 = this.reason == 2 ? 20 : 0;
            String phoneNumber = getPhoneNumber();
            Log.d(LOG_TAG, "callForwardInfo=" + this.callForwardInfo);
            if (i == 3 && this.callForwardInfo != null && this.callForwardInfo.status == 1 && phoneNumber.equals(this.callForwardInfo.number)) {
                Log.d(LOG_TAG, "no change, do nothing");
                return;
            }
            int subId = this.mPhone.getSubId();
            if (CallSettingUtils.shouldShowDataTrafficDialog(subId)) {
                MobileDataDialogFragment.show(this, subId, ((Activity) getContext()).getFragmentManager(), phoneNumber, i2, i);
                setSummaryOn("");
            } else {
                doAction(phoneNumber, i2, i);
            }
        }
    }

    void handleCallForwardResult(CallForwardInfo callForwardInfo) {
        PersistableBundle carrierConfigForSubId;
        this.callForwardInfo = callForwardInfo;
        Log.d(LOG_TAG, "handleGetCFResponse done, callForwardInfo=" + this.callForwardInfo);
        if (this.mReplaceInvalidCFNumber && PhoneNumberUtils.formatNumber(this.callForwardInfo.number, getCurrentCountryIso()) == null) {
            this.callForwardInfo.number = getContext().getString(R.string.voicemail);
            Log.i(LOG_TAG, "handleGetCFResponse: Overridding CF number");
        }
        boolean z = false;
        setToggled(this.callForwardInfo.status == 1);
        if (this.mPhone != null && TextUtils.isEmpty(this.callForwardInfo.number) && (carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId())) != null) {
            z = carrierConfigForSubId.getBoolean("display_voicemail_number_as_default_call_forwarding_number");
            Log.d(LOG_TAG, "display voicemail number as default");
        }
        String voiceMailNumber = this.mPhone != null ? this.mPhone.getVoiceMailNumber() : null;
        if (!z) {
            voiceMailNumber = this.callForwardInfo.number;
        }
        setPhoneNumber(voiceMailNumber);
        ExtensionManager.getCallFeaturesSettingExt().disableCallFwdPref(getContext(), this.mPhone, this, callForwardInfo.reason);
    }

    private void updateSummaryText() {
        if (isToggled()) {
            String rawPhoneNumber = getRawPhoneNumber();
            if (rawPhoneNumber != null && rawPhoneNumber.length() > 0) {
                String strUnicodeWrap = BidiFormatter.getInstance().unicodeWrap(rawPhoneNumber, TextDirectionHeuristics.LTR);
                String strValueOf = String.valueOf(TextUtils.replace(this.mSummaryOnTemplate, SRC_TAGS, new String[]{strUnicodeWrap}));
                int iIndexOf = strValueOf.indexOf(strUnicodeWrap);
                SpannableString spannableString = new SpannableString(strValueOf);
                PhoneNumberUtils.addTtsSpan(spannableString, iIndexOf, strUnicodeWrap.length() + iIndexOf);
                setSummaryOn(spannableString);
                return;
            }
            setSummaryOn(getContext().getString(R.string.sum_cfu_enabled_no_number));
        }
    }

    private String getCurrentCountryIso() {
        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService("phone");
        if (telephonyManager == null) {
            return "";
        }
        return telephonyManager.getNetworkCountryIso().toUpperCase();
    }

    private class MyHandler extends Handler {
        static final int MESSAGE_GET_CF = 0;
        static final int MESSAGE_SET_CF = 1;

        private MyHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    handleGetCFResponse(message);
                    break;
                case 1:
                    handleSetCFResponse(message);
                    break;
            }
        }

        private void handleGetCFResponse(Message message) {
            CharSequence text;
            Log.d(CallForwardEditPreference.LOG_TAG, "handleGetCFResponse: done");
            AsyncResult asyncResult = (AsyncResult) message.obj;
            CallForwardEditPreference.this.resetUtError(asyncResult);
            CallForwardEditPreference.this.mTcpListener.onFinished(CallForwardEditPreference.this, message.arg2 != 1);
            CallForwardEditPreference.this.callForwardInfo = null;
            if (asyncResult.exception != null) {
                Log.d(CallForwardEditPreference.LOG_TAG, "handleGetCFResponse: ar.exception=" + asyncResult.exception);
                if (asyncResult.exception instanceof CommandException) {
                    CallForwardEditPreference.this.handleCommandException(asyncResult.exception);
                } else {
                    CallForwardEditPreference.this.mTcpListener.onError(CallForwardEditPreference.this, TimeConsumingPreferenceActivity.EXCEPTION_ERROR);
                }
            } else {
                if (asyncResult.userObj instanceof Throwable) {
                    CallForwardEditPreference.this.mTcpListener.onError(CallForwardEditPreference.this, TimeConsumingPreferenceActivity.RESPONSE_ERROR);
                }
                CallForwardInfo[] callForwardInfoArr = (CallForwardInfo[]) asyncResult.result;
                if (callForwardInfoArr == null || callForwardInfoArr.length == 0) {
                    Log.d(CallForwardEditPreference.LOG_TAG, "handleGetCFResponse: cfInfoArray.length==0");
                    CallForwardEditPreference.this.setEnabled(false);
                    CallForwardEditPreference.this.mTcpListener.onError(CallForwardEditPreference.this, TimeConsumingPreferenceActivity.RESPONSE_ERROR);
                } else {
                    int length = callForwardInfoArr.length;
                    for (int i = 0; i < length; i++) {
                        Log.d(CallForwardEditPreference.LOG_TAG, "handleGetCFResponse, cfInfoArray[" + i + "]=" + callForwardInfoArr[i]);
                        if ((CallForwardEditPreference.this.mServiceClass & callForwardInfoArr[i].serviceClass) != 0) {
                            CallForwardInfo callForwardInfo = callForwardInfoArr[i];
                            CallForwardEditPreference.this.handleCallForwardResult(callForwardInfo);
                            if (message.arg2 == 1 && message.arg1 == 0 && callForwardInfo.status == 1) {
                                if (CallForwardEditPreference.this.isSkipCFFailToDisableDialog()) {
                                    Log.d(CallForwardEditPreference.LOG_TAG, "Skipped Callforwarding fail-to-disable dialog");
                                } else {
                                    switch (CallForwardEditPreference.this.reason) {
                                        case 1:
                                            text = CallForwardEditPreference.this.getContext().getText(R.string.disable_cfb_forbidden);
                                            break;
                                        case 2:
                                            text = CallForwardEditPreference.this.getContext().getText(R.string.disable_cfnry_forbidden);
                                            break;
                                        case 3:
                                            text = CallForwardEditPreference.this.getContext().getText(R.string.disable_cfnrc_forbidden);
                                            break;
                                        default:
                                            Log.d(CallForwardEditPreference.LOG_TAG, "handleGetCFResponse: fail to disable");
                                            text = null;
                                            break;
                                    }
                                    AlertDialog.Builder builder = new AlertDialog.Builder(CallForwardEditPreference.this.getContext());
                                    builder.setNeutralButton(R.string.close_dialog, (DialogInterface.OnClickListener) null);
                                    builder.setTitle(CallForwardEditPreference.this.getContext().getText(R.string.error_updating_title));
                                    builder.setMessage(text);
                                    builder.setCancelable(true);
                                    if (!((Activity) CallForwardEditPreference.this.getContext()).isFinishing() && text != null) {
                                        builder.create().show();
                                    }
                                }
                            }
                        }
                    }
                    ExtensionManager.getCallFeaturesSettingExt().resetImsPdnOverSSComplete(CallForwardEditPreference.this.getContext(), message.arg2);
                }
            }
            CallForwardEditPreference.this.updateSummaryText();
        }

        private void handleSetCFResponse(final Message message) {
            final AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null) {
                Log.d(CallForwardEditPreference.LOG_TAG, "handleSetCFResponse: ar.exception=" + asyncResult.exception);
            }
            Log.d(CallForwardEditPreference.LOG_TAG, "handleSetCFResponse: re get");
            final int i = message.arg1;
            postDelayed(new Runnable() {
                final Message msgCopy;

                {
                    this.msgCopy = Message.obtain(message);
                }

                @Override
                public void run() {
                    Log.d(CallForwardEditPreference.LOG_TAG, "handleSetCFResponse: re get start");
                    CallForwardEditPreference.this.getCallForwardingOption(i, 1, asyncResult.exception);
                }
            }, 1500L);
        }
    }

    private boolean isSkipCFFailToDisableDialog() {
        PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId());
        if (carrierConfigForSubId != null) {
            return carrierConfigForSubId.getBoolean("skip_cf_fail_to_disable_dialog_bool");
        }
        return false;
    }

    public void setServiceClass(int i) {
        this.mServiceClass = i;
        Log.d(LOG_TAG, "set service class to: " + this.mServiceClass);
    }

    private void startCdmaCallForwardOptions() {
        Log.d(LOG_TAG, "startCdmaCallForwardOptions to sub " + this.mPhone.getSubId());
        Intent intent = new Intent(getContext(), (Class<?>) CdmaCallForwardOptions.class);
        SubscriptionInfoHelper.addExtrasToIntent(intent, MtkSubscriptionManager.getSubInfo((String) null, this.mPhone.getSubId()));
        getContext().startActivity(intent);
    }

    public boolean hasUtError() {
        return this.mHasUtError;
    }

    private void resetUtError(AsyncResult asyncResult) {
        boolean zIsUtPreferOnlyByCdmaSim = CallSettingUtils.isUtPreferOnlyByCdmaSim(this.mPhone.getSubId());
        boolean zIsCtVolteMix = CallSettingUtils.isCtVolteMix();
        if (asyncResult.exception != null && (asyncResult.exception instanceof CommandException) && zIsUtPreferOnlyByCdmaSim) {
            this.mHasUtError = isUtError(asyncResult.exception.getCommandError());
        } else {
            this.mHasUtError = false;
        }
        if (zIsUtPreferOnlyByCdmaSim && zIsCtVolteMix) {
            this.mEnableVolteTips = false;
        }
        Log.d(LOG_TAG, "Reset UT Error: " + this.mHasUtError + " tips: " + this.mEnableVolteTips);
    }

    private boolean isUtError(CommandException.Error error) {
        boolean z = error == CommandException.Error.OPERATION_NOT_ALLOWED || error == CommandException.Error.OEM_ERROR_2 || error == CommandException.Error.OEM_ERROR_3;
        Log.d(LOG_TAG, "Has Ut Error: " + z);
        return z;
    }

    private void getCallForwardingOption(int i, int i2, Object obj) {
        if (this.mPhone instanceof MtkGsmCdmaPhone) {
            Log.d(LOG_TAG, "getCallForwardingOption mServiceClass: " + this.mServiceClass);
            this.mPhone.getCallForwardingOptionForServiceClass(this.reason, this.mServiceClass, this.mHandler.obtainMessage(0, i, i2, obj));
            return;
        }
        this.mPhone.getCallForwardingOption(this.reason, this.mHandler.obtainMessage(0, i, i2, obj));
    }

    private void setCallForwardingOption(String str, int i, int i2) {
        if (this.mPhone instanceof MtkGsmCdmaPhone) {
            Log.d(LOG_TAG, "setCallForwardingOption mServiceClass: " + this.mServiceClass);
            this.mPhone.setCallForwardingOptionForServiceClass(i2, this.reason, str, i, this.mServiceClass, this.mHandler.obtainMessage(1, i2, 1));
            return;
        }
        this.mPhone.setCallForwardingOption(i2, this.reason, str, i, this.mHandler.obtainMessage(1, i2, 1));
    }

    private void handleCommandException(CommandException commandException) {
        boolean z;
        boolean zIsCapabilityPhone;
        if (!this.mHasUtError) {
            if (commandException.getCommandError() == CommandException.Error.REQUEST_NOT_SUPPORTED) {
                Log.d(LOG_TAG, "receive REQUEST_NOT_SUPPORTED");
                setEnabled(false);
                return;
            } else {
                this.mTcpListener.onException(this, commandException);
                return;
            }
        }
        Log.d(LOG_TAG, "403 received, path to CS...");
        setEnabled(false);
        if (MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(getContext(), this.mPhone.getPhoneId()) && this.mEnableVolteTips && ((zIsCapabilityPhone = TelephonyUtilsEx.isCapabilityPhone(this.mPhone)) || MtkImsManager.isSupportMims())) {
            z = true;
            if (TelephonyUtilsEx.isBothslotCt4gSim(SubscriptionManager.from(getContext())) && !zIsCapabilityPhone) {
                Log.d(LOG_TAG, "dual ct 4g sim and none main capability phone");
                z = false;
            }
        } else {
            z = false;
        }
        if (z) {
            Log.d(LOG_TAG, "volte enabled, show alert...");
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.alert_turn_off_volte);
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    CallForwardEditPreference.this.mParentActivity.finish();
                }
            });
            AlertDialog alertDialogCreate = builder.create();
            if (!((Activity) getContext()).isFinishing()) {
                alertDialogCreate.show();
                return;
            }
            return;
        }
        startCdmaCallForwardOptions();
        this.mParentActivity.finish();
    }

    @Override
    public void doAction(String str, int i, int i2) {
        Log.d(LOG_TAG, "reason=" + this.reason + ", action=" + i2 + ", number=" + str);
        setSummaryOn("");
        setCallForwardingOption(str, i, i2);
        if (this.mTcpListener != null) {
            this.mTcpListener.onStarted(this, false);
        }
    }

    @Override
    public void doCancel() {
        Log.d(LOG_TAG, "doCancel callForwardInfo: " + this.callForwardInfo);
        handleCallForwardResult(this.callForwardInfo);
        updateSummaryText();
    }
}
