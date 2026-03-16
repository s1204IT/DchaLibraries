package com.android.phone;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.phone.CallGatewayManager;
import com.android.phone.CdmaPhoneCallState;
import com.android.phone.Constants;
import com.android.phone.PhoneUtils;

public class CallController extends Handler {
    private static final boolean DBG;
    private static CallController sInstance;
    private final PhoneGlobals mApp;
    private final CallManager mCM;
    private final CallGatewayManager mCallGatewayManager;
    private final CallLogger mCallLogger;
    private EmergencyCallHelper mEmergencyCallHelper;

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
    }

    static CallController init(PhoneGlobals app, CallLogger callLogger, CallGatewayManager callGatewayManager) {
        CallController callController;
        synchronized (CallController.class) {
            if (sInstance == null) {
                sInstance = new CallController(app, callLogger, callGatewayManager);
            } else {
                Log.wtf("CallController", "init() called multiple times!  sInstance = " + sInstance);
            }
            callController = sInstance;
        }
        return callController;
    }

    private CallController(PhoneGlobals app, CallLogger callLogger, CallGatewayManager callGatewayManager) {
        if (DBG) {
            log("CallController constructor: app = " + app);
        }
        this.mApp = app;
        this.mCM = app.mCM;
        this.mCallLogger = callLogger;
        this.mCallGatewayManager = callGatewayManager;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                if (DBG) {
                    log("THREEWAY_CALLERINFO_DISPLAY_DONE...");
                }
                if (this.mApp.cdmaPhoneCallState.getCurrentCallState() == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                    this.mApp.cdmaPhoneCallState.setThreeWayCallOrigState(false);
                }
                break;
            default:
                Log.wtf("CallController", "handleMessage: unexpected code: " + msg);
                break;
        }
    }

    public void placeCall(Intent intent) {
        log("placeCall()...  intent = " + intent);
        if (intent == null) {
            Log.wtf("CallController", "placeCall: called with null intent");
            throw new IllegalArgumentException("placeCall: called with null intent");
        }
        String action = intent.getAction();
        Uri uri = intent.getData();
        if (uri == null) {
            Log.wtf("CallController", "placeCall: intent had no data");
            throw new IllegalArgumentException("placeCall: intent had no data");
        }
        uri.getScheme();
        PhoneNumberUtils.getNumberFromIntent(intent, this.mApp);
        if (!"android.intent.action.CALL".equals(action) && !"android.intent.action.CALL_EMERGENCY".equals(action) && !"android.intent.action.CALL_PRIVILEGED".equals(action)) {
            Log.wtf("CallController", "placeCall: unexpected intent action " + action);
            throw new IllegalArgumentException("Unexpected action: " + action);
        }
        Phone phone = this.mApp.mCM.getDefaultPhone();
        if (TelephonyCapabilities.supportsOtasp(phone)) {
            checkForOtaspCall(intent);
        }
        this.mApp.setRestoreMuteOnInCallResume(false);
        Constants.CallStatusCode status = placeCallInternal(intent);
        switch (status) {
            case SUCCESS:
            case EXITED_ECM:
                if (DBG) {
                    log("==> placeCall(): success from placeCallInternal(): " + status);
                    return;
                }
                return;
            default:
                log("==> placeCall(): failure code from placeCallInternal(): " + status);
                handleOutgoingCallError(status);
                return;
        }
    }

    private Constants.CallStatusCode placeCallInternal(Intent intent) {
        if (DBG) {
            log("placeCallInternal()...  intent = " + intent);
        }
        Uri uri = intent.getData();
        String scheme = uri != null ? uri.getScheme() : null;
        Constants.CallStatusCode okToCallStatus = checkIfOkToInitiateOutgoingCall(this.mCM.getServiceState());
        try {
            String number = PhoneUtils.getInitialNumber(intent);
            String sipPhoneUri = intent.getStringExtra("android.phone.extra.SIP_PHONE_URI");
            ComponentName thirdPartyCallComponent = (ComponentName) intent.getParcelableExtra("android.phone.extra.THIRD_PARTY_CALL_COMPONENT");
            Phone phone = PhoneUtils.pickPhoneBasedOnNumber(this.mCM, scheme, number, sipPhoneUri, thirdPartyCallComponent);
            Constants.CallStatusCode okToCallStatus2 = checkIfOkToInitiateOutgoingCall(phone.getServiceState().getState());
            if (number == null) {
                Log.w("CallController", "placeCall: couldn't get a phone number from Intent " + intent);
                return Constants.CallStatusCode.NO_PHONE_NUMBER_SUPPLIED;
            }
            boolean isEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this.mApp, number);
            boolean isPotentialEmergencyNumber = PhoneNumberUtils.isPotentialLocalEmergencyNumber(this.mApp, number);
            boolean isEmergencyIntent = "android.intent.action.CALL_EMERGENCY".equals(intent.getAction());
            if (isPotentialEmergencyNumber && !isEmergencyIntent) {
                Log.e("CallController", "Non-CALL_EMERGENCY Intent " + intent + " attempted to call potential emergency number " + number + ".");
                return Constants.CallStatusCode.CALL_FAILED;
            }
            if (!isPotentialEmergencyNumber && isEmergencyIntent) {
                Log.e("CallController", "Received CALL_EMERGENCY Intent " + intent + " with non-potential-emergency number " + number + " -- failing call.");
                return Constants.CallStatusCode.CALL_FAILED;
            }
            if (isEmergencyNumber && (okToCallStatus2 == Constants.CallStatusCode.EMERGENCY_ONLY || okToCallStatus2 == Constants.CallStatusCode.OUT_OF_SERVICE)) {
                if (DBG) {
                    log("placeCall: Emergency number detected with status = " + okToCallStatus2);
                }
                okToCallStatus2 = Constants.CallStatusCode.SUCCESS;
                if (DBG) {
                    log("==> UPDATING status to: " + okToCallStatus2);
                }
            }
            if (okToCallStatus2 != Constants.CallStatusCode.SUCCESS) {
                if (isEmergencyNumber && okToCallStatus2 == Constants.CallStatusCode.POWER_OFF) {
                    Log.i("CallController", "placeCall: Trying to make emergency call while POWER_OFF!");
                    synchronized (this) {
                        if (this.mEmergencyCallHelper == null) {
                            this.mEmergencyCallHelper = new EmergencyCallHelper(this);
                        }
                    }
                    this.mEmergencyCallHelper.startEmergencyCallFromAirplaneModeSequence(number);
                    return Constants.CallStatusCode.SUCCESS;
                }
                if (DBG) {
                    log("==> placeCallInternal(): non-success status: " + okToCallStatus2);
                }
                this.mCallLogger.logCall(null, number, 0, 2, System.currentTimeMillis(), 0L);
                return okToCallStatus2;
            }
            Uri contactUri = intent.getData();
            CallGatewayManager callGatewayManager = this.mCallGatewayManager;
            CallGatewayManager.RawGatewayInfo rawGatewayInfo = CallGatewayManager.getRawGatewayInfo(intent, number);
            int callStatus = PhoneUtils.placeCall(this.mApp, phone, number, contactUri, isEmergencyNumber || isEmergencyIntent, rawGatewayInfo, this.mCallGatewayManager);
            switch (callStatus) {
                case 0:
                    if (scheme == null || scheme.equals("voicemail")) {
                    }
                    boolean exitedEcm = false;
                    if (PhoneUtils.isPhoneInEcm(phone) && !isEmergencyNumber) {
                        Log.i("CallController", "About to exit ECM because of an outgoing non-emergency call");
                        exitedEcm = true;
                    }
                    if (phone.getPhoneType() == 2 && this.mApp.cdmaPhoneCallState.getCurrentCallState() == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                        this.mApp.cdmaPhoneCallState.setThreeWayCallOrigState(true);
                        sendEmptyMessageDelayed(1, 3000L);
                    }
                    if (exitedEcm) {
                        return Constants.CallStatusCode.EXITED_ECM;
                    }
                    return Constants.CallStatusCode.SUCCESS;
                case 1:
                    if (DBG) {
                        log("placeCall: specified number was an MMI code: '" + number + "'.");
                    }
                    return Constants.CallStatusCode.DIALED_MMI;
                case 2:
                    Log.w("CallController", "placeCall: PhoneUtils.placeCall() FAILED for number '" + number + "'.");
                    this.mCallLogger.logCall(null, number, 0, 2, System.currentTimeMillis(), 0L);
                    return Constants.CallStatusCode.CALL_FAILED;
                default:
                    Log.wtf("CallController", "placeCall: unknown callStatus " + callStatus + " from PhoneUtils.placeCall() for number '" + number + "'.");
                    return Constants.CallStatusCode.SUCCESS;
            }
        } catch (PhoneUtils.VoiceMailNumberMissingException e) {
            if (okToCallStatus != Constants.CallStatusCode.SUCCESS) {
                if (DBG) {
                    log("Voicemail number not reachable in current SIM card state.");
                }
                return okToCallStatus;
            }
            if (DBG) {
                log("VoiceMailNumberMissingException from getInitialNumber()");
            }
            return Constants.CallStatusCode.VOICEMAIL_NUMBER_MISSING;
        }
    }

    private Constants.CallStatusCode checkIfOkToInitiateOutgoingCall(int state) {
        if (Settings.Global.getInt(this.mApp.getContentResolver(), "airplane_mode_on", 0) == 1) {
            return Constants.CallStatusCode.POWER_OFF;
        }
        switch (state) {
            case 0:
                return Constants.CallStatusCode.SUCCESS;
            case 1:
                return Constants.CallStatusCode.OUT_OF_SERVICE;
            case 2:
                return Constants.CallStatusCode.EMERGENCY_ONLY;
            case 3:
                return Constants.CallStatusCode.POWER_OFF;
            default:
                throw new IllegalStateException("Unexpected ServiceState: " + state);
        }
    }

    private void handleOutgoingCallError(Constants.CallStatusCode status) {
        if (DBG) {
            log("handleOutgoingCallError(): status = " + status);
        }
        Intent intent = new Intent(this.mApp, (Class<?>) ErrorDialogActivity.class);
        int errorMessageId = -1;
        switch (status) {
            case SUCCESS:
                Log.wtf("CallController", "handleOutgoingCallError: SUCCESS isn't an error");
                break;
            case EXITED_ECM:
            default:
                Log.wtf("CallController", "handleOutgoingCallError: unexpected status code " + status);
                errorMessageId = R.string.incall_error_call_failed;
                break;
            case CALL_FAILED:
                errorMessageId = R.string.incall_error_call_failed;
                break;
            case POWER_OFF:
                errorMessageId = R.string.incall_error_power_off;
                break;
            case EMERGENCY_ONLY:
                errorMessageId = R.string.incall_error_emergency_only;
                break;
            case OUT_OF_SERVICE:
                errorMessageId = R.string.incall_error_out_of_service;
                break;
            case NO_PHONE_NUMBER_SUPPLIED:
                errorMessageId = R.string.incall_error_no_phone_number_supplied;
                break;
            case VOICEMAIL_NUMBER_MISSING:
                intent.putExtra("show_missing_voicemail", true);
                break;
            case DIALED_MMI:
                Intent mmiIntent = new Intent(this.mApp, (Class<?>) MMIDialogActivity.class);
                mmiIntent.setFlags(276824064);
                this.mApp.startActivity(mmiIntent);
                return;
        }
        intent.setFlags(276824064);
        if (errorMessageId != -1) {
            intent.putExtra("error_message_id", errorMessageId);
        }
        this.mApp.startActivity(intent);
    }

    private void checkForOtaspCall(Intent intent) {
        if (OtaUtils.isOtaspCallIntent(intent)) {
            Log.i("CallController", "checkForOtaspCall: handling OTASP intent! " + intent);
            OtaUtils.setupOtaspCall(intent);
        } else if (DBG) {
            log("checkForOtaspCall: not an OTASP call.");
        }
    }

    private static void log(String msg) {
        Log.d("CallController", msg);
    }
}
