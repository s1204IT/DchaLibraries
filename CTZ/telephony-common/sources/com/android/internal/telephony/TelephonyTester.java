package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Build;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.ims.ImsConferenceState;
import android.telephony.ims.ImsExternalCallState;
import android.telephony.ims.ImsReasonInfo;
import com.android.ims.ImsCall;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.imsphone.ImsExternalCallTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.test.TestConferenceEventPackageParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class TelephonyTester {
    private static final String ACTION_RESET = "reset";
    private static final String ACTION_TEST_CONFERENCE_EVENT_PACKAGE = "com.android.internal.telephony.TestConferenceEventPackage";
    private static final String ACTION_TEST_DIALOG_EVENT_PACKAGE = "com.android.internal.telephony.TestDialogEventPackage";
    private static final String ACTION_TEST_HANDOVER_FAIL = "com.android.internal.telephony.TestHandoverFail";
    private static final String ACTION_TEST_SERVICE_STATE = "com.android.internal.telephony.TestServiceState";
    private static final String ACTION_TEST_SUPP_SRVC_FAIL = "com.android.internal.telephony.TestSuppSrvcFail";
    private static final String ACTION_TEST_SUPP_SRVC_NOTIFICATION = "com.android.internal.telephony.TestSuppSrvcNotification";
    private static final boolean DBG = true;
    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_CANPULL = "canPull";
    private static final String EXTRA_CODE = "code";
    private static final String EXTRA_DATA_RAT = "data_rat";
    private static final String EXTRA_DATA_REG_STATE = "data_reg_state";
    private static final String EXTRA_DATA_ROAMING_TYPE = "data_roaming_type";
    private static final String EXTRA_DIALOGID = "dialogId";
    private static final String EXTRA_FAILURE_CODE = "failureCode";
    private static final String EXTRA_FILENAME = "filename";
    private static final String EXTRA_NUMBER = "number";
    private static final String EXTRA_SENDPACKAGE = "sendPackage";
    private static final String EXTRA_STARTPACKAGE = "startPackage";
    private static final String EXTRA_STATE = "state";
    private static final String EXTRA_TYPE = "type";
    private static final String EXTRA_VOICE_RAT = "voice_rat";
    private static final String EXTRA_VOICE_REG_STATE = "voice_reg_state";
    private static final String EXTRA_VOICE_ROAMING_TYPE = "voice_roaming_type";
    private static final String LOG_TAG = "TelephonyTester";
    private static List<ImsExternalCallState> mImsExternalCallStates = null;
    protected BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            try {
                TelephonyTester.log("sIntentReceiver.onReceive: action=" + action);
                if (action.equals(TelephonyTester.this.mPhone.getActionDetached())) {
                    TelephonyTester.log("simulate detaching");
                    TelephonyTester.this.mPhone.getServiceStateTracker().mDetachedRegistrants.notifyRegistrants();
                } else if (action.equals(TelephonyTester.this.mPhone.getActionAttached())) {
                    TelephonyTester.log("simulate attaching");
                    TelephonyTester.this.mPhone.getServiceStateTracker().mAttachedRegistrants.notifyRegistrants();
                } else if (action.equals(TelephonyTester.ACTION_TEST_CONFERENCE_EVENT_PACKAGE)) {
                    TelephonyTester.log("inject simulated conference event package");
                    TelephonyTester.this.handleTestConferenceEventPackage(context, intent.getStringExtra(TelephonyTester.EXTRA_FILENAME));
                } else if (action.equals(TelephonyTester.ACTION_TEST_DIALOG_EVENT_PACKAGE)) {
                    TelephonyTester.log("handle test dialog event package intent");
                    TelephonyTester.this.handleTestDialogEventPackageIntent(intent);
                } else if (action.equals(TelephonyTester.ACTION_TEST_SUPP_SRVC_FAIL)) {
                    TelephonyTester.log("handle test supp svc failed intent");
                    TelephonyTester.this.handleSuppServiceFailedIntent(intent);
                } else if (action.equals(TelephonyTester.ACTION_TEST_HANDOVER_FAIL)) {
                    TelephonyTester.log("handle handover fail test intent");
                    TelephonyTester.this.handleHandoverFailedIntent();
                } else if (action.equals(TelephonyTester.ACTION_TEST_SUPP_SRVC_NOTIFICATION)) {
                    TelephonyTester.log("handle supp service notification test intent");
                    TelephonyTester.this.sendTestSuppServiceNotification(intent);
                } else if (action.equals(TelephonyTester.ACTION_TEST_SERVICE_STATE)) {
                    TelephonyTester.log("handle test service state changed intent");
                    TelephonyTester.this.mServiceStateTestIntent = intent;
                    TelephonyTester.this.mPhone.getServiceStateTracker().sendEmptyMessage(2);
                } else {
                    TelephonyTester.log("onReceive: unknown action=" + action);
                }
            } catch (BadParcelableException e) {
                Rlog.w(TelephonyTester.LOG_TAG, e);
            }
        }
    };
    private Phone mPhone;
    private Intent mServiceStateTestIntent;

    TelephonyTester(Phone phone) {
        this.mPhone = phone;
        if (Build.IS_DEBUGGABLE) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(this.mPhone.getActionDetached());
            log("register for intent action=" + this.mPhone.getActionDetached());
            intentFilter.addAction(this.mPhone.getActionAttached());
            log("register for intent action=" + this.mPhone.getActionAttached());
            if (this.mPhone.getPhoneType() == 5) {
                log("register for intent action=com.android.internal.telephony.TestConferenceEventPackage");
                intentFilter.addAction(ACTION_TEST_CONFERENCE_EVENT_PACKAGE);
                intentFilter.addAction(ACTION_TEST_DIALOG_EVENT_PACKAGE);
                intentFilter.addAction(ACTION_TEST_SUPP_SRVC_FAIL);
                intentFilter.addAction(ACTION_TEST_HANDOVER_FAIL);
                intentFilter.addAction(ACTION_TEST_SUPP_SRVC_NOTIFICATION);
                mImsExternalCallStates = new ArrayList();
            } else {
                intentFilter.addAction(ACTION_TEST_SERVICE_STATE);
                log("register for intent action=com.android.internal.telephony.TestServiceState");
            }
            phone.getContext().registerReceiver(this.mIntentReceiver, intentFilter, null, this.mPhone.getHandler());
        }
    }

    void dispose() {
        if (Build.IS_DEBUGGABLE) {
            this.mPhone.getContext().unregisterReceiver(this.mIntentReceiver);
        }
    }

    private static void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private void handleSuppServiceFailedIntent(Intent intent) {
        ImsPhone imsPhone = (ImsPhone) this.mPhone;
        if (imsPhone == null) {
            return;
        }
        imsPhone.notifySuppServiceFailed(PhoneInternalInterface.SuppService.values()[intent.getIntExtra(EXTRA_FAILURE_CODE, 0)]);
    }

    private void handleHandoverFailedIntent() {
        ImsPhoneCall foregroundCall;
        ImsCall imsCall;
        ImsPhone imsPhone = (ImsPhone) this.mPhone;
        if (imsPhone == null || (foregroundCall = imsPhone.getForegroundCall()) == null || (imsCall = foregroundCall.getImsCall()) == null) {
            return;
        }
        imsCall.getImsCallSessionListenerProxy().callSessionHandoverFailed(imsCall.getCallSession(), 14, 18, new ImsReasonInfo());
    }

    private void handleTestConferenceEventPackage(Context context, String str) {
        ImsPhoneCall foregroundCall;
        ImsCall imsCall;
        ImsPhone imsPhone = (ImsPhone) this.mPhone;
        if (imsPhone == null || (foregroundCall = imsPhone.getForegroundCall()) == null || (imsCall = foregroundCall.getImsCall()) == null) {
            return;
        }
        File file = new File(context.getFilesDir(), str);
        try {
            ImsConferenceState imsConferenceState = new TestConferenceEventPackageParser(new FileInputStream(file)).parse();
            if (imsConferenceState == null) {
                return;
            }
            imsCall.conferenceStateUpdated(imsConferenceState);
        } catch (FileNotFoundException e) {
            log("Test conference event package file not found: " + file.getAbsolutePath());
        }
    }

    private void handleTestDialogEventPackageIntent(Intent intent) {
        ImsExternalCallTracker externalCallTracker;
        ImsPhone imsPhone = (ImsPhone) this.mPhone;
        if (imsPhone == null || (externalCallTracker = imsPhone.getExternalCallTracker()) == null) {
            return;
        }
        if (intent.hasExtra(EXTRA_STARTPACKAGE)) {
            mImsExternalCallStates.clear();
            return;
        }
        if (intent.hasExtra(EXTRA_SENDPACKAGE)) {
            externalCallTracker.refreshExternalCallState(mImsExternalCallStates);
            mImsExternalCallStates.clear();
        } else if (intent.hasExtra(EXTRA_DIALOGID)) {
            mImsExternalCallStates.add(new ImsExternalCallState(intent.getIntExtra(EXTRA_DIALOGID, 0), Uri.parse(intent.getStringExtra(EXTRA_NUMBER)), intent.getBooleanExtra(EXTRA_CANPULL, true), intent.getIntExtra(EXTRA_STATE, 1), 2, false));
        }
    }

    private void sendTestSuppServiceNotification(Intent intent) {
        if (intent.hasExtra(EXTRA_CODE) && intent.hasExtra(EXTRA_TYPE)) {
            int intExtra = intent.getIntExtra(EXTRA_CODE, -1);
            int intExtra2 = intent.getIntExtra(EXTRA_TYPE, -1);
            ImsPhone imsPhone = (ImsPhone) this.mPhone;
            if (imsPhone == null) {
                return;
            }
            log("Test supp service notification:" + intExtra);
            SuppServiceNotification suppServiceNotification = new SuppServiceNotification();
            suppServiceNotification.code = intExtra;
            suppServiceNotification.notificationType = intExtra2;
            imsPhone.notifySuppSvcNotification(suppServiceNotification);
        }
    }

    void overrideServiceState(ServiceState serviceState) {
        if (this.mServiceStateTestIntent == null || serviceState == null) {
            return;
        }
        if (this.mServiceStateTestIntent.hasExtra(EXTRA_ACTION) && ACTION_RESET.equals(this.mServiceStateTestIntent.getStringExtra(EXTRA_ACTION))) {
            log("Service state override reset");
            return;
        }
        if (this.mServiceStateTestIntent.hasExtra(EXTRA_VOICE_REG_STATE)) {
            serviceState.setVoiceRegState(this.mServiceStateTestIntent.getIntExtra(EXTRA_VOICE_REG_STATE, 4));
            log("Override voice reg state with " + serviceState.getVoiceRegState());
        }
        if (this.mServiceStateTestIntent.hasExtra(EXTRA_DATA_REG_STATE)) {
            serviceState.setDataRegState(this.mServiceStateTestIntent.getIntExtra(EXTRA_DATA_REG_STATE, 4));
            log("Override data reg state with " + serviceState.getDataRegState());
        }
        if (this.mServiceStateTestIntent.hasExtra(EXTRA_VOICE_RAT)) {
            serviceState.setRilVoiceRadioTechnology(this.mServiceStateTestIntent.getIntExtra(EXTRA_VOICE_RAT, 0));
            log("Override voice rat with " + serviceState.getRilVoiceRadioTechnology());
        }
        if (this.mServiceStateTestIntent.hasExtra(EXTRA_DATA_RAT)) {
            serviceState.setRilDataRadioTechnology(this.mServiceStateTestIntent.getIntExtra(EXTRA_DATA_RAT, 0));
            log("Override data rat with " + serviceState.getRilDataRadioTechnology());
        }
        if (this.mServiceStateTestIntent.hasExtra(EXTRA_VOICE_ROAMING_TYPE)) {
            serviceState.setVoiceRoamingType(this.mServiceStateTestIntent.getIntExtra(EXTRA_VOICE_ROAMING_TYPE, 1));
            log("Override voice roaming type with " + serviceState.getVoiceRoamingType());
        }
        if (this.mServiceStateTestIntent.hasExtra(EXTRA_DATA_ROAMING_TYPE)) {
            serviceState.setDataRoamingType(this.mServiceStateTestIntent.getIntExtra(EXTRA_DATA_ROAMING_TYPE, 1));
            log("Override data roaming type with " + serviceState.getDataRoamingType());
        }
    }
}
