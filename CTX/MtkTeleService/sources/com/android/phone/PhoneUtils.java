package com.android.phone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallerInfoAsyncQuery;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.internal.telephony.sip.SipPhone;
import com.android.phone.CallGatewayManager;
import com.android.phone.CdmaPhoneCallState;
import com.mediatek.internal.telephony.MtkCallerInfo;
import com.mediatek.phone.ext.ExtensionManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PhoneUtils {
    static final String ADD_CALL_MODE_KEY = "add_call_mode";
    static final int AUDIO_IDLE = 0;
    static final int AUDIO_OFFHOOK = 2;
    static final int AUDIO_RINGING = 1;
    public static final int CALL_STATUS_DIALED = 0;
    public static final int CALL_STATUS_DIALED_MMI = 1;
    public static final int CALL_STATUS_FAILED = 2;
    private static final int CNAP_SPECIAL_CASE_NO = -1;
    private static final boolean DBG = false;
    private static final boolean DBG_SETAUDIOMODE_STACK = false;
    private static final int DISCONNECTING_POLLING_INTERVAL_MS = 200;
    private static final int DISCONNECTING_POLLING_TIMES_LIMIT = 8;
    public static final String EMERGENCY_ACCOUNT_HANDLE_ID = "E";
    private static final String LOG_TAG = "PhoneUtils";
    static final int MAX_USSD_LEN = 160;
    static final int MIN_USSD_LEN = 1;
    private static final int MSG_CHECK_STATUS_ANSWERCALL = 100;
    private static final int PHONE_STATE_CHANGED = -1;
    private static final int QUERY_TOKEN = -1;
    private static final int THEME = 16974545;
    private static final boolean VDBG = false;
    private static ConnectionHandler mConnectionHandler;
    private static boolean sIsSpeakerEnabled = false;
    private static AlertDialog sUssdDialog = null;
    private static StringBuilder sUssdMsg = new StringBuilder();
    private static final ComponentName PSTN_CONNECTION_SERVICE_COMPONENT = new ComponentName("com.android.phone", "com.android.services.telephony.TelephonyConnectionService");
    static CallerInfoAsyncQuery.OnQueryCompleteListener sCallerInfoQueryListener = new CallerInfoAsyncQuery.OnQueryCompleteListener() {
        public void onQueryComplete(int i, Object obj, CallerInfo callerInfo) {
            CallerInfo callerInfo2;
            Connection connection = (Connection) obj;
            if (callerInfo.contactExists || callerInfo.isEmergencyNumber() || callerInfo.isVoiceMailNumber()) {
                if (callerInfo.numberPresentation == 0) {
                    callerInfo.numberPresentation = connection.getNumberPresentation();
                }
            } else {
                callerInfo2 = PhoneUtils.getCallerInfo(null, connection);
                if (callerInfo2 != null) {
                    callerInfo2.phoneNumber = callerInfo.phoneNumber;
                    callerInfo2.geoDescription = callerInfo.geoDescription;
                }
                connection.setUserData(callerInfo2);
            }
            callerInfo2 = callerInfo;
            connection.setUserData(callerInfo2);
        }
    };

    public static class CallerInfoToken {
        public CallerInfoAsyncQuery asyncQuery;
        public CallerInfo currentInfo;
        public boolean isFinal;
    }

    private static class FgRingCalls {
        private Call fgCall;
        private Call ringing;

        public FgRingCalls(Call call, Call call2) {
            this.fgCall = call;
            this.ringing = call2;
        }
    }

    private static class ConnectionHandler extends Handler {
        private ConnectionHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 100) {
                FgRingCalls fgRingCalls = (FgRingCalls) message.obj;
                if (fgRingCalls.fgCall != null && fgRingCalls.fgCall.getState() == Call.State.DISCONNECTING && message.arg1 < 8) {
                    Message messageObtainMessage = PhoneUtils.mConnectionHandler.obtainMessage(100);
                    messageObtainMessage.arg1 = 1 + message.arg1;
                    messageObtainMessage.obj = message.obj;
                    PhoneUtils.mConnectionHandler.sendMessageDelayed(messageObtainMessage, 200L);
                    return;
                }
                if (fgRingCalls.ringing.isRinging()) {
                    if (message.arg1 == 8) {
                        Log.e(PhoneUtils.LOG_TAG, "DISCONNECTING time out");
                    }
                    PhoneUtils.answerCall(fgRingCalls.ringing);
                }
            }
        }
    }

    public static void initializeConnectionHandler(CallManager callManager) {
        if (mConnectionHandler == null) {
            mConnectionHandler = new ConnectionHandler();
        }
        callManager.registerForPreciseCallStateChanged(mConnectionHandler, -1, callManager);
    }

    private PhoneUtils() {
    }

    static boolean answerCall(Call call) {
        CallStateException e;
        log("answerCall(" + call + ")...");
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        CallNotifier callNotifier = phoneGlobals.notifier;
        boolean z = false;
        boolean z2 = call.getPhone().getPhoneType() == 2;
        if (z2 && call.getState() == Call.State.WAITING) {
            callNotifier.stopSignalInfoTone();
        }
        if (call == null || !call.isRinging()) {
            return false;
        }
        if (z2) {
            try {
                if (phoneGlobals.cdmaPhoneCallState.getCurrentCallState() == CdmaPhoneCallState.PhoneCallState.IDLE) {
                    phoneGlobals.cdmaPhoneCallState.setCurrentCallState(CdmaPhoneCallState.PhoneCallState.SINGLE_ACTIVE);
                } else {
                    phoneGlobals.cdmaPhoneCallState.setCurrentCallState(CdmaPhoneCallState.PhoneCallState.CONF_CALL);
                    phoneGlobals.cdmaPhoneCallState.setAddCallMenuStateAfterCallWaiting(true);
                }
            } catch (CallStateException e2) {
                e = e2;
                Log.w(LOG_TAG, "answerCall: caught " + e, e);
                if (!z2) {
                    phoneGlobals.cdmaPhoneCallState.setCurrentCallState(phoneGlobals.cdmaPhoneCallState.getPreviousCallState());
                    return z;
                }
                return z;
            }
        }
        isRealIncomingCall(call.getState());
        phoneGlobals.mCM.acceptCall(call);
        try {
            setAudioMode();
            return true;
        } catch (CallStateException e3) {
            e = e3;
            z = true;
            Log.w(LOG_TAG, "answerCall: caught " + e, e);
            if (!z2) {
            }
        }
    }

    static void hangupAllCalls(CallManager callManager) {
        Call firstActiveRingingCall = callManager.getFirstActiveRingingCall();
        Call activeFgCall = callManager.getActiveFgCall();
        Call firstActiveBgCall = callManager.getFirstActiveBgCall();
        if (firstActiveBgCall != null && !firstActiveBgCall.isIdle()) {
            hangup(firstActiveBgCall);
        }
        if (activeFgCall != null && !activeFgCall.isIdle()) {
            hangup(activeFgCall);
        }
        if (firstActiveRingingCall != null && !firstActiveRingingCall.isIdle()) {
            hangupRingingCall(activeFgCall);
        }
    }

    static boolean hangup(CallManager callManager) {
        Call firstActiveRingingCall = callManager.getFirstActiveRingingCall();
        Call activeFgCall = callManager.getActiveFgCall();
        Call firstActiveBgCall = callManager.getFirstActiveBgCall();
        if (!firstActiveRingingCall.isIdle()) {
            log("hangup(): hanging up ringing call");
            return hangupRingingCall(firstActiveRingingCall);
        }
        if (!activeFgCall.isIdle()) {
            log("hangup(): hanging up foreground call");
            return hangup(activeFgCall);
        }
        if (!firstActiveBgCall.isIdle()) {
            log("hangup(): hanging up background call");
            return hangup(firstActiveBgCall);
        }
        log("hangup(): no active call to hang up");
        return false;
    }

    static boolean hangupRingingCall(Call call) {
        call.getPhone().getPhoneType();
        if (call.getState() == Call.State.INCOMING) {
            log("hangupRingingCall(): regular incoming call: hangup()");
            return hangup(call);
        }
        Log.w(LOG_TAG, "hangupRingingCall: no INCOMING or WAITING call");
        return false;
    }

    static boolean hangupActiveCall(Call call) {
        return hangup(call);
    }

    static boolean hangupHoldingCall(Call call) {
        return hangup(call);
    }

    static boolean hangupRingingAndActive(Phone phone) {
        boolean zHangupRingingCall;
        boolean zHangupActiveCall;
        Call ringingCall = phone.getRingingCall();
        Call foregroundCall = phone.getForegroundCall();
        if (!ringingCall.isIdle()) {
            log("hangupRingingAndActive: Hang up Ringing Call");
            zHangupRingingCall = hangupRingingCall(ringingCall);
        } else {
            zHangupRingingCall = false;
        }
        if (!foregroundCall.isIdle()) {
            log("hangupRingingAndActive: Hang up Foreground Call");
            zHangupActiveCall = hangupActiveCall(foregroundCall);
        } else {
            zHangupActiveCall = false;
        }
        return zHangupRingingCall || zHangupActiveCall;
    }

    static boolean hangup(Call call) {
        try {
            CallManager callManager = PhoneGlobals.getInstance().mCM;
            if (call.getState() == Call.State.ACTIVE && callManager.hasActiveBgCall()) {
                log("- hangup(Call): hangupForegroundResumeBackground...");
                callManager.hangupForegroundResumeBackground(callManager.getFirstActiveBgCall());
                return true;
            }
            log("- hangup(Call): regular hangup()...");
            call.hangup();
            return true;
        } catch (CallStateException e) {
            Log.e(LOG_TAG, "Call hangup: caught " + e, e);
            return false;
        }
    }

    static void hangup(Connection connection) {
        if (connection != null) {
            try {
                connection.hangup();
            } catch (CallStateException e) {
                Log.w(LOG_TAG, "Connection hangup: caught " + e, e);
            }
        }
    }

    static boolean answerAndEndHolding(CallManager callManager, Call call) {
        if (!hangupHoldingCall(callManager.getFirstActiveBgCall())) {
            Log.e(LOG_TAG, "end holding failed!");
            return false;
        }
        return answerCall(call);
    }

    static boolean answerAndEndActive(CallManager callManager, Call call) {
        Call activeFgCall = callManager.getActiveFgCall();
        if (!hangupActiveCall(activeFgCall)) {
            Log.w(LOG_TAG, "end active call failed!");
            return false;
        }
        mConnectionHandler.removeMessages(100);
        Message messageObtainMessage = mConnectionHandler.obtainMessage(100);
        messageObtainMessage.arg1 = 1;
        messageObtainMessage.obj = new FgRingCalls(activeFgCall, call);
        mConnectionHandler.sendMessage(messageObtainMessage);
        return true;
    }

    private static void updateCdmaCallStateOnNewOutgoingCall(PhoneGlobals phoneGlobals, Connection connection) {
        if (phoneGlobals.cdmaPhoneCallState.getCurrentCallState() == CdmaPhoneCallState.PhoneCallState.IDLE) {
            phoneGlobals.cdmaPhoneCallState.setCurrentCallState(CdmaPhoneCallState.PhoneCallState.SINGLE_ACTIVE);
        } else {
            phoneGlobals.cdmaPhoneCallState.setCurrentCallState(CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE);
        }
    }

    public static int placeCall(Context context, Phone phone, String str, Uri uri, boolean z) {
        return placeCall(context, phone, str, uri, z, CallGatewayManager.EMPTY_INFO, null);
    }

    public static int placeCall(Context context, Phone phone, String str, Uri uri, boolean z, CallGatewayManager.RawGatewayInfo rawGatewayInfo, CallGatewayManager callGatewayManager) {
        boolean z2;
        Uri uri2 = rawGatewayInfo.gatewayUri;
        StringBuilder sb = new StringBuilder();
        sb.append("placeCall()... number: ");
        sb.append(toLogSafePhoneNumber(str));
        sb.append(", GW: ");
        sb.append(uri2 != null ? "non-null" : "null");
        sb.append(", emergency? ");
        sb.append(z);
        log(sb.toString());
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        if (uri2 != null && !z && isRoutableViaGateway(str)) {
            z2 = true;
        } else {
            z2 = false;
        }
        if (z2) {
            if (uri2 == null || !"tel".equals(uri2.getScheme())) {
                Log.e(LOG_TAG, "Unsupported URL:" + uri2);
                return 2;
            }
            str = uri2.getSchemeSpecificPart();
        }
        phoneGlobals.mCM.getState();
        PhoneConstants.State state = PhoneConstants.State.IDLE;
        try {
            Connection connectionDial = phoneGlobals.mCM.dial(phone, str, 0);
            int phoneType = phone.getPhoneType();
            if (connectionDial == null) {
                return 2;
            }
            if (callGatewayManager != null) {
                callGatewayManager.setGatewayInfoForConnection(connectionDial, rawGatewayInfo);
            }
            if (phoneType == 2) {
                updateCdmaCallStateOnNewOutgoingCall(phoneGlobals, connectionDial);
            }
            if (uri2 == null) {
                context.getContentResolver();
                if (uri != null && uri.getScheme().equals("content")) {
                    Object userData = connectionDial.getUserData();
                    if (userData == null) {
                        connectionDial.setUserData(uri);
                    } else if (userData instanceof CallerInfo) {
                        ((CallerInfo) userData).contactRefUri = uri;
                    } else {
                        ((CallerInfoToken) userData).currentInfo.contactRefUri = uri;
                    }
                }
            }
            startGetCallerInfo(context, connectionDial, null, null, rawGatewayInfo);
            setAudioMode();
            return 0;
        } catch (CallStateException e) {
            Log.w(LOG_TAG, "Exception from app.mCM.dial()", e);
            return 2;
        }
    }

    static String toLogSafePhoneNumber(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '-' || cCharAt == '@' || cCharAt == '.') {
                sb.append(cCharAt);
            } else {
                sb.append('x');
            }
        }
        return sb.toString();
    }

    static void sendEmptyFlash(Phone phone) {
        if (phone.getPhoneType() == 2 && phone.getForegroundCall().getState() == Call.State.ACTIVE) {
            switchHoldingAndActive(phone.getBackgroundCall());
        }
    }

    static void swap() {
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        if (!okToSwapCalls(phoneGlobals.mCM)) {
            return;
        }
        switchHoldingAndActive(phoneGlobals.mCM.getFirstActiveBgCall());
    }

    static void switchHoldingAndActive(Call call) {
        log("switchHoldingAndActive()...");
        try {
            CallManager callManager = PhoneGlobals.getInstance().mCM;
            if (call.isIdle()) {
                callManager.switchHoldingAndActive(callManager.getFgPhone().getBackgroundCall());
            } else {
                callManager.switchHoldingAndActive(call);
            }
            setAudioMode(callManager);
        } catch (CallStateException e) {
            Log.w(LOG_TAG, "switchHoldingAndActive: caught " + e, e);
        }
    }

    static void mergeCalls() {
        mergeCalls(PhoneGlobals.getInstance().mCM);
    }

    static void mergeCalls(CallManager callManager) {
        if (callManager.getFgPhone().getPhoneType() == 2) {
            log("mergeCalls(): CDMA...");
            PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
            if (phoneGlobals.cdmaPhoneCallState.getCurrentCallState() == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE) {
                phoneGlobals.cdmaPhoneCallState.setCurrentCallState(CdmaPhoneCallState.PhoneCallState.CONF_CALL);
                log("- sending flash...");
                switchHoldingAndActive(callManager.getFirstActiveBgCall());
                return;
            }
            return;
        }
        try {
            log("mergeCalls(): calling cm.conference()...");
            callManager.conference(callManager.getFirstActiveBgCall());
        } catch (CallStateException e) {
            Log.w(LOG_TAG, "mergeCalls: caught " + e, e);
        }
    }

    static void separateCall(Connection connection) {
        try {
            connection.separate();
        } catch (CallStateException e) {
            Log.w(LOG_TAG, "separateCall: caught " + e, e);
        }
    }

    static Dialog displayMMIInitiate(Context context, MmiCode mmiCode, Message message, Dialog dialog) {
        log("displayMMIInitiate: " + android.telecom.Log.pii(mmiCode.toString()));
        if (dialog != null) {
            dialog.dismiss();
        }
        if (!(mmiCode != null && mmiCode.isCancelable())) {
            log("displayMMIInitiate: not a USSD code, displaying status toast.");
            Toast.makeText(context, context.getText(R.string.mmiStarted), 0).show();
            return null;
        }
        log("displayMMIInitiate: running USSD code, displaying intermediate progress.");
        ProgressDialog progressDialog = new ProgressDialog(context, 16974545);
        progressDialog.setMessage(context.getText(R.string.ussdRunning));
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.getWindow().addFlags(2);
        progressDialog.show();
        ExtensionManager.getMmiCodeExt().onMmiDailogShow(message);
        return progressDialog;
    }

    static void displayMMIComplete(final Phone phone, Context context, final MmiCode mmiCode, Message message, AlertDialog alertDialog) {
        CharSequence message2;
        CharSequence text;
        int i;
        final PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        MmiCode.State state = mmiCode.getState();
        log("displayMMIComplete: state=" + state);
        switch (AnonymousClass5.$SwitchMap$com$android$internal$telephony$MmiCode$State[state.ordinal()]) {
            case 1:
                message2 = mmiCode.getMessage();
                log("displayMMIComplete: using text from PENDING MMI message: '" + ((Object) message2) + "'");
                text = message2;
                i = 0;
                if (alertDialog != null) {
                    alertDialog.dismiss();
                }
                if (phoneGlobals.getPUKEntryActivity() == null && state == MmiCode.State.COMPLETE) {
                    ProgressDialog progressDialog = new ProgressDialog(phoneGlobals, 16974545);
                    progressDialog.setTitle(i);
                    progressDialog.setMessage(text);
                    progressDialog.setCancelable(false);
                    progressDialog.setIndeterminate(true);
                    progressDialog.getWindow().setType(2008);
                    progressDialog.getWindow().addFlags(2);
                    progressDialog.show();
                    phoneGlobals.setPukEntryProgressDialog(progressDialog);
                    return;
                }
                if (phoneGlobals.getPUKEntryActivity() != null) {
                    phoneGlobals.setPukEntryActivity(null);
                }
                if (state == MmiCode.State.PENDING) {
                    log("displayMMIComplete: MMI code has finished running.");
                    log("displayMMIComplete: Extended NW displayMMIInitiate (" + ((Object) text) + ")");
                    if (text == null || text.length() == 0) {
                        return;
                    }
                    if (sUssdDialog != null) {
                        sUssdDialog.dismiss();
                        sUssdDialog = null;
                    }
                    if (sUssdDialog == null) {
                        sUssdDialog = new AlertDialog.Builder(context, 16974545).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).setCancelable(true).setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                PhoneUtils.sUssdMsg.setLength(0);
                            }
                        }).create();
                        sUssdDialog.getWindow().setType(2009);
                        sUssdDialog.getWindow().addFlags(2);
                    }
                    if (sUssdMsg.length() != 0) {
                        sUssdMsg.insert(0, "\n").insert(0, phoneGlobals.getResources().getString(R.string.ussd_dialog_sep)).insert(0, "\n");
                    }
                    sUssdMsg.insert(0, text);
                    sUssdDialog.setMessage(sUssdMsg.toString());
                    sUssdDialog.show();
                    return;
                }
                log("displayMMIComplete: USSD code has requested user input. Constructing input dialog.");
                ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, R.style.DialerAlertDialogTheme);
                View viewInflate = ((LayoutInflater) contextThemeWrapper.getSystemService("layout_inflater")).inflate(R.layout.dialog_ussd_response, (ViewGroup) null);
                final EditText editText = (EditText) viewInflate.findViewById(R.id.input_field);
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        switch (i2) {
                            case -2:
                                if (mmiCode.isCancelable()) {
                                    mmiCode.cancel();
                                }
                                break;
                            case SubscriptionInfoHelper.NO_SUB_ID:
                                if (!ExtensionManager.getMmiCodeExt().showUssdInteractionDialog(phone, editText)) {
                                    if (editText.length() < 1 || editText.length() > PhoneUtils.MAX_USSD_LEN) {
                                        Toast.makeText(phoneGlobals, phoneGlobals.getResources().getString(R.string.enter_input, 1, Integer.valueOf(PhoneUtils.MAX_USSD_LEN)), 1).show();
                                        if (mmiCode.isCancelable()) {
                                            mmiCode.cancel();
                                        }
                                    } else {
                                        phone.sendUssdResponse(editText.getText().toString());
                                    }
                                    break;
                                }
                                break;
                        }
                    }
                };
                final AlertDialog alertDialogCreate = new AlertDialog.Builder(contextThemeWrapper).setMessage(text).setView(viewInflate).setPositiveButton(R.string.send_button, onClickListener).setNegativeButton(R.string.cancel, onClickListener).setCancelable(false).create();
                editText.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View view, int i2, KeyEvent keyEvent) {
                        if (i2 == 5 || i2 == 66) {
                            if (keyEvent.getAction() == 0) {
                                phone.sendUssdResponse(editText.getText().toString());
                                alertDialogCreate.dismiss();
                                return true;
                            }
                            return true;
                        }
                        return false;
                    }
                });
                editText.requestFocus();
                alertDialogCreate.getWindow().setType(2008);
                alertDialogCreate.getWindow().addFlags(2);
                alertDialogCreate.show();
                alertDialogCreate.getButton(-1).setTextColor(context.getResources().getColor(R.color.dialer_theme_color));
                alertDialogCreate.getButton(-2).setTextColor(context.getResources().getColor(R.color.dialer_theme_color));
                return;
            case 2:
                text = null;
                i = 0;
                if (alertDialog != null) {
                }
                if (phoneGlobals.getPUKEntryActivity() == null) {
                }
                if (phoneGlobals.getPUKEntryActivity() != null) {
                }
                if (state == MmiCode.State.PENDING) {
                }
                break;
            case 3:
                if (phoneGlobals.getPUKEntryActivity() != null) {
                    i = android.R.string.config_systemTelevisionNotificationHandler;
                    text = context.getText(R.string.puk_unlocked);
                    if (alertDialog != null) {
                    }
                    if (phoneGlobals.getPUKEntryActivity() == null) {
                    }
                    if (phoneGlobals.getPUKEntryActivity() != null) {
                    }
                    if (state == MmiCode.State.PENDING) {
                    }
                }
            case 4:
                message2 = mmiCode.getMessage();
                log("displayMMIComplete (failed): using text from MMI message: '" + ((Object) message2) + "'");
                text = message2;
                i = 0;
                if (alertDialog != null) {
                }
                if (phoneGlobals.getPUKEntryActivity() == null) {
                }
                if (phoneGlobals.getPUKEntryActivity() != null) {
                }
                if (state == MmiCode.State.PENDING) {
                }
                break;
            default:
                throw new IllegalStateException("Unexpected MmiCode state: " + state);
        }
    }

    static class AnonymousClass5 {
        static final int[] $SwitchMap$com$android$internal$telephony$MmiCode$State = new int[MmiCode.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$MmiCode$State[MmiCode.State.PENDING.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$MmiCode$State[MmiCode.State.CANCELLED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$MmiCode$State[MmiCode.State.COMPLETE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$MmiCode$State[MmiCode.State.FAILED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    static boolean cancelMmiCode(Phone phone) {
        List pendingMmiCodes = phone.getPendingMmiCodes();
        if (pendingMmiCodes.size() <= 0) {
            return false;
        }
        MmiCode mmiCode = (MmiCode) pendingMmiCodes.get(0);
        if (!mmiCode.isCancelable()) {
            return false;
        }
        mmiCode.cancel();
        return true;
    }

    public static class VoiceMailNumberMissingException extends Exception {
        VoiceMailNumberMissingException() {
        }

        VoiceMailNumberMissingException(String str) {
            super(str);
        }
    }

    private static String getNumberFromIntent(Context context, Intent intent) throws VoiceMailNumberMissingException {
        Uri data = intent.getData();
        String scheme = data.getScheme();
        if ("sip".equals(scheme)) {
            return data.getSchemeSpecificPart();
        }
        String numberFromIntent = PhoneNumberUtils.getNumberFromIntent(intent, context);
        if ("voicemail".equals(scheme) && (numberFromIntent == null || TextUtils.isEmpty(numberFromIntent))) {
            throw new VoiceMailNumberMissingException();
        }
        return numberFromIntent;
    }

    static CallerInfo getCallerInfo(Context context, Connection connection) {
        CallerInfo callerInfo;
        if (connection == null) {
            return null;
        }
        Object userData = connection.getUserData();
        if (userData instanceof Uri) {
            CallerInfo callerInfo2 = CallerInfo.getCallerInfo(context, (Uri) userData);
            if (callerInfo2 == null) {
                return callerInfo2;
            }
            connection.setUserData(callerInfo2);
            return callerInfo2;
        }
        if (userData instanceof CallerInfoToken) {
            callerInfo = ((CallerInfoToken) userData).currentInfo;
        } else {
            callerInfo = (CallerInfo) userData;
        }
        if (callerInfo == null) {
            String address = connection.getAddress();
            if (!TextUtils.isEmpty(address)) {
                CallerInfo callerInfo3 = CallerInfo.getCallerInfo(context, address);
                if (callerInfo3 == null) {
                    return callerInfo3;
                }
                connection.setUserData(callerInfo3);
                return callerInfo3;
            }
        }
        return callerInfo;
    }

    static CallerInfoToken startGetCallerInfo(Context context, Call call, CallerInfoAsyncQuery.OnQueryCompleteListener onQueryCompleteListener, Object obj) {
        Connection earliestConnection;
        int phoneType = call.getPhone().getPhoneType();
        if (phoneType == 2) {
            earliestConnection = call.getLatestConnection();
        } else if (phoneType == 1 || phoneType == 3 || phoneType == 5 || phoneType == 4) {
            earliestConnection = call.getEarliestConnection();
        } else {
            throw new IllegalStateException("Unexpected phone type: " + phoneType);
        }
        return startGetCallerInfo(context, earliestConnection, onQueryCompleteListener, obj);
    }

    static CallerInfoToken startGetCallerInfo(Context context, Connection connection, CallerInfoAsyncQuery.OnQueryCompleteListener onQueryCompleteListener, Object obj) {
        return startGetCallerInfo(context, connection, onQueryCompleteListener, obj, null);
    }

    static CallerInfoToken startGetCallerInfo(Context context, Connection connection, CallerInfoAsyncQuery.OnQueryCompleteListener onQueryCompleteListener, Object obj, CallGatewayManager.RawGatewayInfo rawGatewayInfo) {
        if (connection == null) {
            CallerInfoToken callerInfoToken = new CallerInfoToken();
            callerInfoToken.asyncQuery = null;
            return callerInfoToken;
        }
        Object userData = connection.getUserData();
        if (userData instanceof Uri) {
            CallerInfoToken callerInfoToken2 = new CallerInfoToken();
            callerInfoToken2.currentInfo = new MtkCallerInfo();
            callerInfoToken2.asyncQuery = CallerInfoAsyncQuery.startQuery(-1, context, (Uri) userData, sCallerInfoQueryListener, connection);
            callerInfoToken2.asyncQuery.addQueryListener(-1, onQueryCompleteListener, obj);
            callerInfoToken2.isFinal = false;
            connection.setUserData(callerInfoToken2);
            return callerInfoToken2;
        }
        if (userData == null) {
            String address = connection.getAddress();
            if (rawGatewayInfo != null && rawGatewayInfo != CallGatewayManager.EMPTY_INFO) {
                address = rawGatewayInfo.trueNumber;
            }
            CallerInfoToken callerInfoToken3 = new CallerInfoToken();
            callerInfoToken3.currentInfo = new MtkCallerInfo();
            callerInfoToken3.currentInfo.cnapName = connection.getCnapName();
            callerInfoToken3.currentInfo.name = callerInfoToken3.currentInfo.cnapName;
            callerInfoToken3.currentInfo.numberPresentation = connection.getNumberPresentation();
            callerInfoToken3.currentInfo.namePresentation = connection.getCnapNamePresentation();
            if (!TextUtils.isEmpty(address)) {
                String strModifyForSpecialCnapCases = modifyForSpecialCnapCases(context, callerInfoToken3.currentInfo, address, callerInfoToken3.currentInfo.numberPresentation);
                callerInfoToken3.currentInfo.phoneNumber = strModifyForSpecialCnapCases;
                if (callerInfoToken3.currentInfo.numberPresentation != 1) {
                    callerInfoToken3.isFinal = true;
                } else {
                    callerInfoToken3.asyncQuery = CallerInfoAsyncQuery.startQuery(-1, context, strModifyForSpecialCnapCases, sCallerInfoQueryListener, connection);
                    callerInfoToken3.asyncQuery.addQueryListener(-1, onQueryCompleteListener, obj);
                    callerInfoToken3.isFinal = false;
                }
            } else {
                callerInfoToken3.isFinal = true;
            }
            connection.setUserData(callerInfoToken3);
            return callerInfoToken3;
        }
        if (userData instanceof CallerInfoToken) {
            CallerInfoToken callerInfoToken4 = (CallerInfoToken) userData;
            if (callerInfoToken4.asyncQuery != null) {
                callerInfoToken4.asyncQuery.addQueryListener(-1, onQueryCompleteListener, obj);
            } else {
                String address2 = connection.getAddress();
                if (rawGatewayInfo != null) {
                    address2 = rawGatewayInfo.trueNumber;
                }
                if (!TextUtils.isEmpty(address2)) {
                    callerInfoToken4.currentInfo.cnapName = connection.getCnapName();
                    callerInfoToken4.currentInfo.name = callerInfoToken4.currentInfo.cnapName;
                    callerInfoToken4.currentInfo.numberPresentation = connection.getNumberPresentation();
                    callerInfoToken4.currentInfo.namePresentation = connection.getCnapNamePresentation();
                    String strModifyForSpecialCnapCases2 = modifyForSpecialCnapCases(context, callerInfoToken4.currentInfo, address2, callerInfoToken4.currentInfo.numberPresentation);
                    callerInfoToken4.currentInfo.phoneNumber = strModifyForSpecialCnapCases2;
                    if (callerInfoToken4.currentInfo.numberPresentation != 1) {
                        callerInfoToken4.isFinal = true;
                    } else {
                        callerInfoToken4.asyncQuery = CallerInfoAsyncQuery.startQuery(-1, context, strModifyForSpecialCnapCases2, sCallerInfoQueryListener, connection);
                        callerInfoToken4.asyncQuery.addQueryListener(-1, onQueryCompleteListener, obj);
                        callerInfoToken4.isFinal = false;
                    }
                } else {
                    if (callerInfoToken4.currentInfo == null) {
                        callerInfoToken4.currentInfo = new MtkCallerInfo();
                    }
                    callerInfoToken4.currentInfo.cnapName = connection.getCnapName();
                    callerInfoToken4.currentInfo.name = callerInfoToken4.currentInfo.cnapName;
                    callerInfoToken4.currentInfo.numberPresentation = connection.getNumberPresentation();
                    callerInfoToken4.currentInfo.namePresentation = connection.getCnapNamePresentation();
                    callerInfoToken4.isFinal = true;
                }
            }
            return callerInfoToken4;
        }
        CallerInfoToken callerInfoToken5 = new CallerInfoToken();
        callerInfoToken5.currentInfo = (CallerInfo) userData;
        callerInfoToken5.asyncQuery = null;
        callerInfoToken5.isFinal = true;
        return callerInfoToken5;
    }

    static String getCompactNameFromCallerInfo(CallerInfo callerInfo, Context context) {
        String strModifyForSpecialCnapCases;
        if (callerInfo != null) {
            if (TextUtils.isEmpty(callerInfo.name)) {
                strModifyForSpecialCnapCases = modifyForSpecialCnapCases(context, callerInfo, callerInfo.phoneNumber, callerInfo.numberPresentation);
            } else {
                strModifyForSpecialCnapCases = callerInfo.name;
            }
        } else {
            strModifyForSpecialCnapCases = null;
        }
        if (strModifyForSpecialCnapCases == null || TextUtils.isEmpty(strModifyForSpecialCnapCases)) {
            if (callerInfo != null && callerInfo.numberPresentation == 2) {
                return context.getString(R.string.private_num);
            }
            if (callerInfo != null && callerInfo.numberPresentation == 4) {
                return context.getString(R.string.payphone);
            }
            return context.getString(R.string.unknown);
        }
        return strModifyForSpecialCnapCases;
    }

    static boolean isConferenceCall(Call call) {
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        if (call.getPhone().getPhoneType() == 2) {
            CdmaPhoneCallState.PhoneCallState currentCallState = phoneGlobals.cdmaPhoneCallState.getCurrentCallState();
            if (currentCallState == CdmaPhoneCallState.PhoneCallState.CONF_CALL || (currentCallState == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE && !phoneGlobals.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing())) {
                return true;
            }
            return false;
        }
        List connections = call.getConnections();
        if (connections != null && connections.size() > 1) {
            return true;
        }
        return false;
    }

    static boolean startNewCall(CallManager callManager) {
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        if (!okToAddCall(callManager)) {
            Log.w(LOG_TAG, "startNewCall: can't add a new call in the current state");
            dumpCallManager();
            return false;
        }
        Intent intent = new Intent("android.intent.action.DIAL");
        intent.addFlags(268435456);
        intent.putExtra(ADD_CALL_MODE_KEY, true);
        try {
            phoneGlobals.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(LOG_TAG, "Activity for adding calls isn't found.");
            return false;
        }
    }

    static void turnOnSpeaker(Context context, boolean z, boolean z2) {
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        ((AudioManager) context.getSystemService("audio")).setSpeakerphoneOn(z);
        if (z2) {
            sIsSpeakerEnabled = z;
        }
        phoneGlobals.updateWakeState();
        phoneGlobals.mCM.setEchoSuppressionEnabled();
    }

    static void restoreSpeakerMode(Context context) {
        if (isSpeakerOn(context) != sIsSpeakerEnabled) {
            turnOnSpeaker(context, sIsSpeakerEnabled, false);
        }
    }

    static boolean isSpeakerOn(Context context) {
        return ((AudioManager) context.getSystemService("audio")).isSpeakerphoneOn();
    }

    static boolean isInEmergencyCall(CallManager callManager) {
        Call activeFgCall = callManager.getActiveFgCall();
        if (!activeFgCall.isIdle()) {
            Iterator it = activeFgCall.getConnections().iterator();
            while (it.hasNext()) {
                if (PhoneNumberUtils.isLocalEmergencyNumber(PhoneGlobals.getInstance(), ((Connection) it.next()).getAddress())) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    static boolean getMute() {
        return false;
    }

    static void setAudioMode() {
    }

    static void setAudioMode(CallManager callManager) {
    }

    static boolean hasDisconnectedConnections(Phone phone) {
        return hasDisconnectedConnections(phone.getForegroundCall()) || hasDisconnectedConnections(phone.getBackgroundCall()) || hasDisconnectedConnections(phone.getRingingCall());
    }

    private static final boolean hasDisconnectedConnections(Call call) {
        Iterator it = call.getConnections().iterator();
        while (it.hasNext()) {
            if (!((Connection) it.next()).isAlive()) {
                return true;
            }
        }
        return false;
    }

    static boolean okToHoldCall(CallManager callManager) {
        Call activeFgCall = callManager.getActiveFgCall();
        boolean zHasActiveBgCall = callManager.hasActiveBgCall();
        Call.State state = activeFgCall.getState();
        return (state == Call.State.ACTIVE && !zHasActiveBgCall) || (callManager.hasActiveBgCall() && state == Call.State.IDLE);
    }

    static boolean okToSupportHold(CallManager callManager) {
        Call firstActiveBgCall;
        Call activeFgCall = callManager.getActiveFgCall();
        boolean zHasActiveBgCall = callManager.hasActiveBgCall();
        Call.State state = activeFgCall.getState();
        if (TelephonyCapabilities.supportsHoldAndUnhold(activeFgCall.getPhone())) {
            return true;
        }
        return zHasActiveBgCall && state == Call.State.IDLE && (firstActiveBgCall = callManager.getFirstActiveBgCall()) != null && TelephonyCapabilities.supportsHoldAndUnhold(firstActiveBgCall.getPhone());
    }

    static boolean okToSwapCalls(CallManager callManager) {
        int phoneType = callManager.getDefaultPhone().getPhoneType();
        if (phoneType == 2) {
            return PhoneGlobals.getInstance().cdmaPhoneCallState.getCurrentCallState() == CdmaPhoneCallState.PhoneCallState.CONF_CALL;
        }
        if (phoneType == 1 || phoneType == 3 || phoneType == 5 || phoneType == 4) {
            return !callManager.hasActiveRingingCall() && callManager.getActiveFgCall().getState() == Call.State.ACTIVE && callManager.getFirstActiveBgCall().getState() == Call.State.HOLDING;
        }
        throw new IllegalStateException("Unexpected phone type: " + phoneType);
    }

    static boolean okToMergeCalls(CallManager callManager) {
        if (callManager.getFgPhone().getPhoneType() != 2) {
            return !callManager.hasActiveRingingCall() && callManager.hasActiveFgCall() && callManager.hasActiveBgCall() && callManager.canConference(callManager.getFirstActiveBgCall());
        }
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        return phoneGlobals.cdmaPhoneCallState.getCurrentCallState() == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE && !phoneGlobals.cdmaPhoneCallState.IsThreeWayCallOrigStateDialing();
    }

    static boolean okToAddCall(CallManager callManager) {
        Phone phone = callManager.getActiveFgCall().getPhone();
        if (isPhoneInEcm(phone)) {
            return false;
        }
        int phoneType = phone.getPhoneType();
        Call.State state = callManager.getActiveFgCall().getState();
        if (phoneType == 2) {
            return state == Call.State.ACTIVE && PhoneGlobals.getInstance().cdmaPhoneCallState.getAddCallMenuStateAfterCallWaiting();
        }
        if (phoneType == 1 || phoneType == 3 || phoneType == 5 || phoneType == 4) {
            boolean zHasActiveRingingCall = callManager.hasActiveRingingCall();
            boolean z = callManager.hasActiveFgCall() && callManager.hasActiveBgCall();
            if (zHasActiveRingingCall || z) {
                return false;
            }
            return state == Call.State.ACTIVE || state == Call.State.IDLE || state == Call.State.DISCONNECTED;
        }
        throw new IllegalStateException("Unexpected phone type: " + phoneType);
    }

    private static int checkCnapSpecialCases(String str) {
        if (str.equals("PRIVATE") || str.equals("P") || str.equals("RES")) {
            return 2;
        }
        if (str.equals("UNAVAILABLE") || str.equals("UNKNOWN") || str.equals("UNA") || str.equals("U")) {
            return 3;
        }
        return -1;
    }

    static String modifyForSpecialCnapCases(Context context, CallerInfo callerInfo, String str, int i) {
        int iCheckCnapSpecialCases;
        String string;
        if (callerInfo == null || str == null) {
            return str;
        }
        if (Arrays.asList(context.getResources().getStringArray(R.array.absent_num)).contains(str) && i == 1) {
            str = context.getString(R.string.unknown);
            callerInfo.numberPresentation = 3;
        }
        if ((callerInfo.numberPresentation == 1 || (callerInfo.numberPresentation != i && i == 1)) && (iCheckCnapSpecialCases = checkCnapSpecialCases(str)) != -1) {
            if (iCheckCnapSpecialCases == 2) {
                string = context.getString(R.string.private_num);
            } else {
                if (iCheckCnapSpecialCases == 3) {
                    string = context.getString(R.string.unknown);
                }
                callerInfo.numberPresentation = iCheckCnapSpecialCases;
            }
            str = string;
            callerInfo.numberPresentation = iCheckCnapSpecialCases;
        }
        return str;
    }

    private static boolean isRoutableViaGateway(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        String strStripSeparators = PhoneNumberUtils.stripSeparators(str);
        if (strStripSeparators.equals(PhoneNumberUtils.convertKeypadLettersToDigits(strStripSeparators))) {
            return PhoneNumberUtils.isGlobalPhoneNumber(PhoneNumberUtils.extractNetworkPortion(strStripSeparators));
        }
        return false;
    }

    static boolean isPhoneInEcm(Phone phone) {
        if (phone != null && TelephonyCapabilities.supportsEcm(phone)) {
            return phone.isInEcm();
        }
        return false;
    }

    public static Phone pickPhoneBasedOnNumber(CallManager callManager, String str, String str2, String str3, ComponentName componentName) {
        Phone sipPhoneFromUri;
        return (str3 == null || (sipPhoneFromUri = getSipPhoneFromUri(callManager, str3)) == null) ? callManager.getDefaultPhone() : sipPhoneFromUri;
    }

    public static Phone getSipPhoneFromUri(CallManager callManager, String str) {
        for (SipPhone sipPhone : callManager.getAllPhones()) {
            if (sipPhone.getPhoneType() == 3 && str.equals(sipPhone.getSipUri())) {
                return sipPhone;
            }
        }
        return null;
    }

    public static boolean isRealIncomingCall(Call.State state) {
        return state == Call.State.INCOMING && !PhoneGlobals.getInstance().mCM.hasActiveFgCall();
    }

    public static String getPresentationString(Context context, int i) {
        String string = context.getString(R.string.unknown);
        if (i == 2) {
            return context.getString(R.string.private_num);
        }
        if (i == 4) {
            return context.getString(R.string.payphone);
        }
        return string;
    }

    public static void sendViewNotificationAsync(Context context, Uri uri) {
        Intent intent = new Intent("com.android.contacts.VIEW_NOTIFICATION", uri);
        intent.setClassName("com.android.contacts", "com.android.contacts.ViewNotificationService");
        context.startService(intent);
    }

    static void dumpCallState(Phone phone) {
        PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
        Log.d(LOG_TAG, "dumpCallState():");
        Log.d(LOG_TAG, "- Phone: " + phone + ", name = " + phone.getPhoneName() + ", state = " + phone.getState());
        StringBuilder sb = new StringBuilder(128);
        Call foregroundCall = phone.getForegroundCall();
        sb.setLength(0);
        sb.append("  - FG call: ");
        sb.append(foregroundCall.getState());
        sb.append(" isAlive ");
        sb.append(foregroundCall.getState().isAlive());
        sb.append(" isRinging ");
        sb.append(foregroundCall.getState().isRinging());
        sb.append(" isDialing ");
        sb.append(foregroundCall.getState().isDialing());
        sb.append(" isIdle ");
        sb.append(foregroundCall.isIdle());
        sb.append(" hasConnections ");
        sb.append(foregroundCall.hasConnections());
        Log.d(LOG_TAG, sb.toString());
        Call backgroundCall = phone.getBackgroundCall();
        sb.setLength(0);
        sb.append("  - BG call: ");
        sb.append(backgroundCall.getState());
        sb.append(" isAlive ");
        sb.append(backgroundCall.getState().isAlive());
        sb.append(" isRinging ");
        sb.append(backgroundCall.getState().isRinging());
        sb.append(" isDialing ");
        sb.append(backgroundCall.getState().isDialing());
        sb.append(" isIdle ");
        sb.append(backgroundCall.isIdle());
        sb.append(" hasConnections ");
        sb.append(backgroundCall.hasConnections());
        Log.d(LOG_TAG, sb.toString());
        Call ringingCall = phone.getRingingCall();
        sb.setLength(0);
        sb.append("  - RINGING call: ");
        sb.append(ringingCall.getState());
        sb.append(" isAlive ");
        sb.append(ringingCall.getState().isAlive());
        sb.append(" isRinging ");
        sb.append(ringingCall.getState().isRinging());
        sb.append(" isDialing ");
        sb.append(ringingCall.getState().isDialing());
        sb.append(" isIdle ");
        sb.append(ringingCall.isIdle());
        sb.append(" hasConnections ");
        sb.append(ringingCall.hasConnections());
        Log.d(LOG_TAG, sb.toString());
        boolean z = true;
        boolean z2 = !phone.getRingingCall().isIdle();
        boolean z3 = !phone.getForegroundCall().isIdle();
        boolean z4 = !phone.getBackgroundCall().isIdle();
        if (!z3 || !z4) {
            z = false;
        }
        sb.setLength(0);
        sb.append("  - hasRingingCall ");
        sb.append(z2);
        sb.append(" hasActiveCall ");
        sb.append(z3);
        sb.append(" hasHoldingCall ");
        sb.append(z4);
        sb.append(" allLinesTaken ");
        sb.append(z);
        Log.d(LOG_TAG, sb.toString());
        if (phone.getPhoneType() == 2) {
            if (phoneGlobals.cdmaPhoneCallState != null) {
                Log.d(LOG_TAG, "  - CDMA call state: " + phoneGlobals.cdmaPhoneCallState.getCurrentCallState());
                return;
            }
            Log.d(LOG_TAG, "  - CDMA device, but null cdmaPhoneCallState!");
        }
    }

    private static void log(String str) {
        Log.d(LOG_TAG, str);
    }

    static void dumpCallManager() {
        CallManager callManager = PhoneGlobals.getInstance().mCM;
        StringBuilder sb = new StringBuilder(128);
        Log.d(LOG_TAG, "############### dumpCallManager() ##############");
        Log.d(LOG_TAG, "CallManager: state = " + callManager.getState());
        sb.setLength(0);
        Call activeFgCall = callManager.getActiveFgCall();
        sb.append(" - FG call: ");
        sb.append(callManager.hasActiveFgCall() ? "YES " : "NO ");
        sb.append(activeFgCall);
        sb.append("  State: ");
        sb.append(callManager.getActiveFgCallState());
        sb.append("  Conn: ");
        sb.append(callManager.getFgCallConnections());
        Log.d(LOG_TAG, sb.toString());
        sb.setLength(0);
        Call firstActiveBgCall = callManager.getFirstActiveBgCall();
        sb.append(" - BG call: ");
        sb.append(callManager.hasActiveBgCall() ? "YES " : "NO ");
        sb.append(firstActiveBgCall);
        sb.append("  State: ");
        sb.append(callManager.getFirstActiveBgCall().getState());
        sb.append("  Conn: ");
        sb.append(callManager.getBgCallConnections());
        Log.d(LOG_TAG, sb.toString());
        sb.setLength(0);
        Call firstActiveRingingCall = callManager.getFirstActiveRingingCall();
        sb.append(" - RINGING call: ");
        sb.append(callManager.hasActiveRingingCall() ? "YES " : "NO ");
        sb.append(firstActiveRingingCall);
        sb.append("  State: ");
        sb.append(callManager.getFirstActiveRingingCall().getState());
        Log.d(LOG_TAG, sb.toString());
        for (Phone phone : CallManager.getInstance().getAllPhones()) {
            if (phone != null) {
                Log.d(LOG_TAG, "Phone: " + phone + ", name = " + phone.getPhoneName() + ", state = " + phone.getState());
                sb.setLength(0);
                Call foregroundCall = phone.getForegroundCall();
                sb.append(" - FG call: ");
                sb.append(foregroundCall);
                sb.append("  State: ");
                sb.append(foregroundCall.getState());
                sb.append("  Conn: ");
                sb.append(foregroundCall.hasConnections());
                Log.d(LOG_TAG, sb.toString());
                sb.setLength(0);
                Call backgroundCall = phone.getBackgroundCall();
                sb.append(" - BG call: ");
                sb.append(backgroundCall);
                sb.append("  State: ");
                sb.append(backgroundCall.getState());
                sb.append("  Conn: ");
                sb.append(backgroundCall.hasConnections());
                Log.d(LOG_TAG, sb.toString());
                sb.setLength(0);
                Call ringingCall = phone.getRingingCall();
                sb.append(" - RINGING call: ");
                sb.append(ringingCall);
                sb.append("  State: ");
                sb.append(ringingCall.getState());
                sb.append("  Conn: ");
                sb.append(ringingCall.hasConnections());
                Log.d(LOG_TAG, sb.toString());
            }
        }
        Log.d(LOG_TAG, "############## END dumpCallManager() ###############");
    }

    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == 2;
    }

    public static PhoneAccountHandle makePstnPhoneAccountHandle(String str) {
        return makePstnPhoneAccountHandleWithPrefix(str, "", false);
    }

    public static PhoneAccountHandle makePstnPhoneAccountHandle(int i) {
        return makePstnPhoneAccountHandle(PhoneFactory.getPhone(i));
    }

    public static PhoneAccountHandle makePstnPhoneAccountHandle(Phone phone) {
        return makePstnPhoneAccountHandleWithPrefix(phone, "", false);
    }

    public static PhoneAccountHandle makePstnPhoneAccountHandleWithPrefix(Phone phone, String str, boolean z) {
        String fullIccSerialNumber;
        String str2;
        if (!z) {
            fullIccSerialNumber = phone.getFullIccSerialNumber();
            int slotIndex = SubscriptionController.getInstance().getSlotIndex(phone.getSubId());
            if (TextUtils.isEmpty(fullIccSerialNumber)) {
                fullIccSerialNumber = TelephonyManager.getDefault().getSimSerialNumber(phone.getSubId());
            }
            log("Non ECC PhoneAccountHandle mId=" + fullIccSerialNumber + ", slotId=" + slotIndex);
        } else {
            fullIccSerialNumber = null;
        }
        if (z) {
            str2 = EMERGENCY_ACCOUNT_HANDLE_ID;
        } else {
            str2 = str + String.valueOf(fullIccSerialNumber);
        }
        return makePstnPhoneAccountHandleWithPrefix(str2, str, z);
    }

    public static PhoneAccountHandle makePstnPhoneAccountHandleWithPrefix(String str, String str2, boolean z) {
        return new PhoneAccountHandle(getPstnConnectionServiceName(), str);
    }

    public static int getSubIdForPhoneAccount(PhoneAccount phoneAccount) {
        if (phoneAccount != null && phoneAccount.hasCapabilities(4)) {
            return getSubIdForPhoneAccountHandle(phoneAccount.getAccountHandle());
        }
        return -1;
    }

    public static int getSubIdForPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle) {
        Phone phoneForPhoneAccountHandle = getPhoneForPhoneAccountHandle(phoneAccountHandle);
        if (phoneForPhoneAccountHandle != null) {
            return phoneForPhoneAccountHandle.getSubId();
        }
        return -1;
    }

    public static Phone getPhoneForPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle) {
        if (phoneAccountHandle != null && phoneAccountHandle.getComponentName().equals(getPstnConnectionServiceName())) {
            return getPhoneFromIccId(ExtensionManager.getDigitsUtilExt().getIccidFromPhoneAccountHandle(phoneAccountHandle));
        }
        return null;
    }

    public static boolean isPhoneAccountActive(SubscriptionManager subscriptionManager, PhoneAccountHandle phoneAccountHandle) {
        return subscriptionManager.getActiveSubscriptionInfoForIccIndex(phoneAccountHandle.getId()) != null;
    }

    private static ComponentName getPstnConnectionServiceName() {
        return PSTN_CONNECTION_SERVICE_COMPONENT;
    }

    private static Phone getPhoneFromIccId(String str) {
        if (!TextUtils.isEmpty(str)) {
            for (Phone phone : PhoneFactory.getPhones()) {
                String fullIccSerialNumber = phone.getFullIccSerialNumber();
                SubscriptionController.getInstance().getSlotIndex(phone.getSubId());
                if (TextUtils.isEmpty(fullIccSerialNumber)) {
                    fullIccSerialNumber = TelephonyManager.getDefault().getSimSerialNumber(phone.getSubId());
                }
                if (str.equals(fullIccSerialNumber)) {
                    return phone;
                }
            }
            return null;
        }
        return null;
    }

    static final void registerIccStatus(Handler handler, int i) {
        for (Phone phone : PhoneFactory.getPhones()) {
            IccCard iccCard = phone.getIccCard();
            if (iccCard != null) {
                iccCard.registerForNetworkLocked(handler, i, phone);
            }
        }
    }

    static final void setRadioPower(boolean z) {
        for (Phone phone : PhoneFactory.getPhones()) {
            phone.setRadioPower(z);
        }
    }

    public static List<SubscriptionInfo> getActiveSubInfoList() {
        List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(PhoneGlobals.getInstance()).getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList == null) {
            return new ArrayList();
        }
        return new ArrayList(activeSubscriptionInfoList);
    }

    public static String getSubDisplayName(int i) {
        CharSequence displayName = "";
        SubscriptionInfo activeSubscriptionInfo = SubscriptionManager.from(PhoneGlobals.getInstance()).getActiveSubscriptionInfo(i);
        if (activeSubscriptionInfo != null) {
            displayName = activeSubscriptionInfo.getDisplayName();
        }
        return displayName.toString();
    }

    public static boolean isValidSubId(int i) {
        List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(PhoneGlobals.getInstance()).getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList != null) {
            Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
            while (it.hasNext()) {
                if (it.next().getSubscriptionId() == i) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Phone getPhoneUsingSubId(int i) {
        int phoneId = SubscriptionManager.getPhoneId(i);
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            return PhoneFactory.getPhone(0);
        }
        return PhoneFactory.getPhone(phoneId);
    }

    public static final boolean hasPhoneType(int i) {
        for (int i2 = 0; i2 < TelephonyManager.getDefault().getPhoneCount(); i2++) {
            if (PhoneFactory.getPhone(i2).getPhoneType() == i) {
                return true;
            }
        }
        return false;
    }

    public static void dismissUssdDialog() {
        if (sUssdDialog != null) {
            sUssdDialog.dismiss();
            sUssdDialog = null;
        }
    }
}
