package com.android.phone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.IBluetoothHeadsetPhone;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
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
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.sip.SipPhone;
import com.android.phone.CallGatewayManager;
import com.android.phone.CdmaPhoneCallState;
import com.android.services.telephony.TelephonyConnectionService;
import java.util.Arrays;
import java.util.List;

public class PhoneUtils {
    private static ConnectionHandler mConnectionHandler;
    private static boolean sIsSpeakerEnabled = false;
    private static boolean sIsNoiseSuppressionEnabled = true;
    private static AlertDialog sUssdDialog = null;
    private static StringBuilder sUssdMsg = new StringBuilder();
    static CallerInfoAsyncQuery.OnQueryCompleteListener sCallerInfoQueryListener = new CallerInfoAsyncQuery.OnQueryCompleteListener() {
        public void onQueryComplete(int token, Object cookie, CallerInfo ci) {
            PhoneUtils.log("query complete, updating connection.userdata");
            Connection conn = (Connection) cookie;
            PhoneUtils.log("- onQueryComplete: CallerInfo:" + ci);
            if (ci.contactExists || ci.isEmergencyNumber() || ci.isVoiceMailNumber()) {
                if (ci.numberPresentation == 0) {
                    ci.numberPresentation = conn.getNumberPresentation();
                }
            } else {
                CallerInfo newCi = PhoneUtils.getCallerInfo(null, conn);
                if (newCi != null) {
                    newCi.phoneNumber = ci.phoneNumber;
                    newCi.geoDescription = ci.geoDescription;
                    ci = newCi;
                }
            }
            PhoneUtils.log("==> Stashing CallerInfo " + ci + " into the connection...");
            conn.setUserData(ci);
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

        public FgRingCalls(Call fg, Call ring) {
            this.fgCall = fg;
            this.ringing = ring;
        }
    }

    private static class ConnectionHandler extends Handler {
        private ConnectionHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    FgRingCalls frC = (FgRingCalls) msg.obj;
                    if (frC.fgCall != null && frC.fgCall.getState() == Call.State.DISCONNECTING && msg.arg1 < 8) {
                        Message retryMsg = PhoneUtils.mConnectionHandler.obtainMessage(100);
                        retryMsg.arg1 = msg.arg1 + 1;
                        retryMsg.obj = msg.obj;
                        PhoneUtils.mConnectionHandler.sendMessageDelayed(retryMsg, 200L);
                    } else if (frC.ringing.isRinging()) {
                        if (msg.arg1 == 8) {
                            Log.e("PhoneUtils", "DISCONNECTING time out");
                        }
                        PhoneUtils.answerCall(frC.ringing);
                    }
                    break;
            }
        }
    }

    public static void initializeConnectionHandler(CallManager cm) {
        if (mConnectionHandler == null) {
            mConnectionHandler = new ConnectionHandler();
        }
        cm.registerForPreciseCallStateChanged(mConnectionHandler, -1, cm);
    }

    static boolean answerCall(Call ringingCall) {
        log("answerCall(" + ringingCall + ")...");
        PhoneGlobals app = PhoneGlobals.getInstance();
        CallNotifier notifier = app.notifier;
        Phone phone = ringingCall.getPhone();
        boolean phoneIsCdma = phone.getPhoneType() == 2;
        boolean answered = false;
        IBluetoothHeadsetPhone btPhone = null;
        if (phoneIsCdma && ringingCall.getState() == Call.State.WAITING) {
            notifier.stopSignalInfoTone();
        }
        if (ringingCall != null && ringingCall.isRinging()) {
            log("answerCall: call state = " + ringingCall.getState());
            if (phoneIsCdma) {
                try {
                    if (app.cdmaPhoneCallState.getCurrentCallState() == CdmaPhoneCallState.PhoneCallState.IDLE) {
                        app.cdmaPhoneCallState.setCurrentCallState(CdmaPhoneCallState.PhoneCallState.SINGLE_ACTIVE);
                    } else {
                        app.cdmaPhoneCallState.setCurrentCallState(CdmaPhoneCallState.PhoneCallState.CONF_CALL);
                        app.cdmaPhoneCallState.setAddCallMenuStateAfterCallWaiting(true);
                    }
                } catch (CallStateException ex) {
                    Log.w("PhoneUtils", "answerCall: caught " + ex, ex);
                    if (phoneIsCdma) {
                        app.cdmaPhoneCallState.setCurrentCallState(app.cdmaPhoneCallState.getPreviousCallState());
                        if (0 != 0) {
                            try {
                                btPhone.cdmaSetSecondCallState(false);
                            } catch (RemoteException e) {
                                Log.e("PhoneUtils", Log.getStackTraceString(new Throwable()));
                            }
                        }
                    }
                }
            }
            boolean isRealIncomingCall = isRealIncomingCall(ringingCall.getState());
            app.mCM.acceptCall(ringingCall);
            answered = true;
            setAudioMode();
            boolean speakerActivated = activateSpeakerIfDocked(phone);
            BluetoothManager btManager = app.getBluetoothManager();
            if (isRealIncomingCall && !speakerActivated && isSpeakerOn(app) && !btManager.isBluetoothHeadsetAudioOn()) {
                Log.i("PhoneUtils", "Forcing speaker off due to new incoming call...");
                turnOnSpeaker(app, false, true);
            }
        }
        return answered;
    }

    static boolean hangup(CallManager cm) {
        boolean hungup = false;
        Call ringing = cm.getFirstActiveRingingCall();
        Call fg = cm.getActiveFgCall();
        Call bg = cm.getFirstActiveBgCall();
        if (!ringing.isIdle()) {
            log("hangup(): hanging up ringing call");
            hungup = hangupRingingCall(ringing);
        } else if (!fg.isIdle()) {
            log("hangup(): hanging up foreground call");
            hungup = hangup(fg);
        } else if (!bg.isIdle()) {
            log("hangup(): hanging up background call");
            hungup = hangup(bg);
        } else {
            log("hangup(): no active call to hang up");
        }
        log("==> hungup = " + hungup);
        return hungup;
    }

    static boolean hangupRingingCall(Call ringing) {
        log("hangup ringing call");
        ringing.getPhone().getPhoneType();
        Call.State state = ringing.getState();
        if (state == Call.State.INCOMING) {
            log("hangupRingingCall(): regular incoming call: hangup()");
            return hangup(ringing);
        }
        Log.w("PhoneUtils", "hangupRingingCall: no INCOMING or WAITING call");
        return false;
    }

    static boolean hangupActiveCall(Call foreground) {
        log("hangup active call");
        return hangup(foreground);
    }

    static boolean hangupRingingAndActive(Phone phone) {
        boolean hungUpRingingCall = false;
        boolean hungUpFgCall = false;
        Call ringingCall = phone.getRingingCall();
        Call fgCall = phone.getForegroundCall();
        if (!ringingCall.isIdle()) {
            log("hangupRingingAndActive: Hang up Ringing Call");
            hungUpRingingCall = hangupRingingCall(ringingCall);
        }
        if (!fgCall.isIdle()) {
            log("hangupRingingAndActive: Hang up Foreground Call");
            hungUpFgCall = hangupActiveCall(fgCall);
        }
        return hungUpRingingCall || hungUpFgCall;
    }

    static boolean hangup(Call call) {
        try {
            CallManager cm = PhoneGlobals.getInstance().mCM;
            if (call.getState() == Call.State.ACTIVE && cm.hasActiveBgCall()) {
                log("- hangup(Call): hangupForegroundResumeBackground...");
                cm.hangupForegroundResumeBackground(cm.getFirstActiveBgCall());
            } else {
                log("- hangup(Call): regular hangup()...");
                call.hangup();
            }
            return true;
        } catch (CallStateException ex) {
            Log.e("PhoneUtils", "Call hangup: caught " + ex, ex);
            return false;
        }
    }

    static boolean answerAndEndActive(CallManager cm, Call ringing) {
        log("answerAndEndActive()...");
        Call fgCall = cm.getActiveFgCall();
        if (!hangupActiveCall(fgCall)) {
            Log.w("PhoneUtils", "end active call failed!");
            return false;
        }
        mConnectionHandler.removeMessages(100);
        Message msg = mConnectionHandler.obtainMessage(100);
        msg.arg1 = 1;
        msg.obj = new FgRingCalls(fgCall, ringing);
        mConnectionHandler.sendMessage(msg);
        return true;
    }

    private static void updateCdmaCallStateOnNewOutgoingCall(PhoneGlobals app, Connection connection) {
        if (app.cdmaPhoneCallState.getCurrentCallState() == CdmaPhoneCallState.PhoneCallState.IDLE) {
            app.cdmaPhoneCallState.setCurrentCallState(CdmaPhoneCallState.PhoneCallState.SINGLE_ACTIVE);
        } else {
            app.cdmaPhoneCallState.setCurrentCallState(CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE);
        }
    }

    public static int placeCall(Context context, Phone phone, String number, Uri contactRef, boolean isEmergencyCall) {
        return placeCall(context, phone, number, contactRef, isEmergencyCall, CallGatewayManager.EMPTY_INFO, null);
    }

    public static int placeCall(Context context, Phone phone, String number, Uri contactRef, boolean isEmergencyCall, CallGatewayManager.RawGatewayInfo gatewayInfo, CallGatewayManager callGateway) {
        String numberToDial;
        Uri gatewayUri = gatewayInfo.gatewayUri;
        log("placeCall()... number: " + toLogSafePhoneNumber(number) + ", GW: " + (gatewayUri != null ? "non-null" : "null") + ", emergency? " + isEmergencyCall);
        PhoneGlobals app = PhoneGlobals.getInstance();
        boolean useGateway = false;
        if (gatewayUri != null && !isEmergencyCall && isRoutableViaGateway(number)) {
            useGateway = true;
        }
        int status = 0;
        if (useGateway) {
            if (gatewayUri == null || !"tel".equals(gatewayUri.getScheme())) {
                Log.e("PhoneUtils", "Unsupported URL:" + gatewayUri);
                return 2;
            }
            numberToDial = gatewayUri.getSchemeSpecificPart();
        } else {
            numberToDial = number;
        }
        boolean initiallyIdle = app.mCM.getState() == PhoneConstants.State.IDLE;
        try {
            Connection connection = app.mCM.dial(phone, numberToDial, 0);
            int phoneType = phone.getPhoneType();
            if (connection == null) {
                status = 2;
            } else {
                if (callGateway != null) {
                    callGateway.setGatewayInfoForConnection(connection, gatewayInfo);
                }
                if (phoneType == 2) {
                    updateCdmaCallStateOnNewOutgoingCall(app, connection);
                }
                if (gatewayUri == null) {
                    context.getContentResolver();
                    if (contactRef != null && contactRef.getScheme().equals("content")) {
                        Object userDataObject = connection.getUserData();
                        if (userDataObject == null) {
                            connection.setUserData(contactRef);
                        } else if (userDataObject instanceof CallerInfo) {
                            ((CallerInfo) userDataObject).contactRefUri = contactRef;
                        } else {
                            ((CallerInfoToken) userDataObject).currentInfo.contactRefUri = contactRef;
                        }
                    }
                }
                startGetCallerInfo(context, connection, null, null, gatewayInfo);
                setAudioMode();
                log("about to activate speaker");
                boolean speakerActivated = activateSpeakerIfDocked(phone);
                BluetoothManager btManager = app.getBluetoothManager();
                if (initiallyIdle && !speakerActivated && isSpeakerOn(app) && !btManager.isBluetoothHeadsetAudioOn()) {
                    Log.i("PhoneUtils", "Forcing speaker off when initiating a new outgoing call...");
                    turnOnSpeaker(app, false, true);
                }
            }
            return status;
        } catch (CallStateException ex) {
            Log.w("PhoneUtils", "Exception from app.mCM.dial()", ex);
            return 2;
        }
    }

    static String toLogSafePhoneNumber(String number) {
        if (number == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (c == '-' || c == '@' || c == '.') {
                builder.append(c);
            } else {
                builder.append('x');
            }
        }
        return builder.toString();
    }

    static void sendEmptyFlash(Phone phone) {
        if (phone.getPhoneType() == 2) {
            Call fgCall = phone.getForegroundCall();
            if (fgCall.getState() == Call.State.ACTIVE) {
                Log.d("PhoneUtils", "onReceive: (CDMA) sending empty flash to network");
                switchHoldingAndActive(phone.getBackgroundCall());
            }
        }
    }

    static void switchHoldingAndActive(Call heldCall) {
        log("switchHoldingAndActive()...");
        try {
            CallManager cm = PhoneGlobals.getInstance().mCM;
            if (heldCall.isIdle()) {
                cm.switchHoldingAndActive(cm.getFgPhone().getBackgroundCall());
            } else {
                cm.switchHoldingAndActive(heldCall);
            }
            setAudioMode(cm);
        } catch (CallStateException ex) {
            Log.w("PhoneUtils", "switchHoldingAndActive: caught " + ex, ex);
        }
    }

    static Dialog displayMMIInitiate(Context context, MmiCode mmiCode, Message buttonCallbackMessage, Dialog previousAlert) {
        log("displayMMIInitiate: " + mmiCode);
        if (previousAlert != null) {
            previousAlert.dismiss();
        }
        boolean isCancelable = mmiCode != null && mmiCode.isCancelable();
        if (!isCancelable) {
            log("not a USSD code, displaying status toast.");
            CharSequence text = context.getText(R.string.mmiStarted);
            Toast.makeText(context, text, 0).show();
            return null;
        }
        log("running USSD code, displaying indeterminate progress.");
        ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage(context.getText(R.string.ussdRunning));
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.getWindow().addFlags(2);
        pd.show();
        return pd;
    }

    static void displayMMIComplete(final Phone phone, Context context, final MmiCode mmiCode, Message dismissCallbackMessage, AlertDialog previousAlert) {
        CharSequence text;
        final PhoneGlobals app = PhoneGlobals.getInstance();
        int title = 0;
        MmiCode.State state = mmiCode.getState();
        log("displayMMIComplete: state=" + state);
        switch (AnonymousClass5.$SwitchMap$com$android$internal$telephony$MmiCode$State[state.ordinal()]) {
            case 1:
                text = mmiCode.getMessage();
                log("- using text from PENDING MMI message: '" + ((Object) text) + "'");
                if (previousAlert != null) {
                    previousAlert.dismiss();
                }
                if (app.getPUKEntryActivity() == null && state == MmiCode.State.COMPLETE) {
                    log("displaying PUK unblocking progress dialog.");
                    ProgressDialog pd = new ProgressDialog(app);
                    pd.setTitle(title);
                    pd.setMessage(text);
                    pd.setCancelable(false);
                    pd.setIndeterminate(true);
                    pd.getWindow().setType(2008);
                    pd.getWindow().addFlags(2);
                    pd.show();
                    app.setPukEntryProgressDialog(pd);
                    return;
                }
                if (app.getPUKEntryActivity() != null) {
                    app.setPukEntryActivity(null);
                }
                if (state == MmiCode.State.PENDING) {
                    log("MMI code has finished running.");
                    log("Extended NW displayMMIInitiate (" + ((Object) text) + ")");
                    if (text != null && text.length() != 0) {
                        if (sUssdDialog == null) {
                            sUssdDialog = new AlertDialog.Builder(context, 5).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).setCancelable(true).setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    PhoneUtils.sUssdMsg.setLength(0);
                                }
                            }).create();
                            sUssdDialog.getWindow().setType(2008);
                            sUssdDialog.getWindow().addFlags(2);
                        }
                        if (sUssdMsg.length() != 0) {
                            sUssdMsg.insert(0, "\n").insert(0, app.getResources().getString(R.string.ussd_dialog_sep)).insert(0, "\n");
                        }
                        sUssdMsg.insert(0, text);
                        sUssdDialog.setMessage(sUssdMsg.toString());
                        sUssdDialog.show();
                        return;
                    }
                    return;
                }
                log("USSD code has requested user input. Constructing input dialog.");
                ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, R.style.DialerAlertDialogTheme);
                LayoutInflater inflater = (LayoutInflater) contextThemeWrapper.getSystemService("layout_inflater");
                View dialogView = inflater.inflate(R.layout.dialog_ussd_response, (ViewGroup) null);
                final EditText inputText = (EditText) dialogView.findViewById(R.id.input_field);
                DialogInterface.OnClickListener mUSSDDialogListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        switch (whichButton) {
                            case -2:
                                if (mmiCode.isCancelable()) {
                                    mmiCode.cancel();
                                }
                                break;
                            case -1:
                                if (inputText.length() < 1 || inputText.length() > 160) {
                                    Toast.makeText(app, app.getResources().getString(R.string.enter_input, 1, 160), 1).show();
                                    if (mmiCode.isCancelable()) {
                                        mmiCode.cancel();
                                    }
                                } else {
                                    phone.sendUssdResponse(inputText.getText().toString());
                                }
                                break;
                        }
                    }
                };
                final AlertDialog newDialog = new AlertDialog.Builder(contextThemeWrapper).setMessage(text).setView(dialogView).setPositiveButton(R.string.send_button, mUSSDDialogListener).setNegativeButton(R.string.cancel, mUSSDDialogListener).setCancelable(false).create();
                View.OnKeyListener mUSSDDialogInputListener = new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        switch (keyCode) {
                            case 5:
                            case 66:
                                if (event.getAction() == 0) {
                                    phone.sendUssdResponse(inputText.getText().toString());
                                    newDialog.dismiss();
                                }
                                return true;
                            default:
                                return false;
                        }
                    }
                };
                inputText.setOnKeyListener(mUSSDDialogInputListener);
                inputText.requestFocus();
                newDialog.getWindow().setType(2008);
                newDialog.getWindow().addFlags(2);
                newDialog.show();
                newDialog.getButton(-1).setTextColor(context.getResources().getColor(R.color.dialer_theme_color));
                newDialog.getButton(-2).setTextColor(context.getResources().getColor(R.color.dialer_theme_color));
                return;
            case 2:
                text = null;
                if (previousAlert != null) {
                }
                if (app.getPUKEntryActivity() == null) {
                }
                if (app.getPUKEntryActivity() != null) {
                }
                if (state == MmiCode.State.PENDING) {
                }
                break;
            case 3:
                if (app.getPUKEntryActivity() != null) {
                    title = android.R.string.PERSOSUBSTATE_SIM_IMPI_ENTRY;
                    text = context.getText(R.string.puk_unlocked);
                }
                if (previousAlert != null) {
                }
                if (app.getPUKEntryActivity() == null) {
                }
                if (app.getPUKEntryActivity() != null) {
                }
                if (state == MmiCode.State.PENDING) {
                }
            case 4:
                text = mmiCode.getMessage();
                log("- using text from MMI message: '" + ((Object) text) + "'");
                if (previousAlert != null) {
                }
                if (app.getPUKEntryActivity() == null) {
                }
                if (app.getPUKEntryActivity() != null) {
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
        List<? extends MmiCode> pendingMmis = phone.getPendingMmiCodes();
        int count = pendingMmis.size();
        log("cancelMmiCode: num pending MMIs = " + count);
        if (count <= 0) {
            return false;
        }
        MmiCode mmiCode = (MmiCode) pendingMmis.get(0);
        if (!mmiCode.isCancelable()) {
            return false;
        }
        mmiCode.cancel();
        return true;
    }

    public static class VoiceMailNumberMissingException extends Exception {
        VoiceMailNumberMissingException() {
        }
    }

    public static String getInitialNumber(Intent intent) throws VoiceMailNumberMissingException {
        log("getInitialNumber(): " + intent);
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return null;
        }
        if (intent.hasExtra("android.phone.extra.ACTUAL_NUMBER_TO_DIAL")) {
            String actualNumberToDial = intent.getStringExtra("android.phone.extra.ACTUAL_NUMBER_TO_DIAL");
            log("==> got EXTRA_ACTUAL_NUMBER_TO_DIAL; returning '" + toLogSafePhoneNumber(actualNumberToDial) + "'");
            return actualNumberToDial;
        }
        return getNumberFromIntent(PhoneGlobals.getInstance(), intent);
    }

    private static String getNumberFromIntent(Context context, Intent intent) throws VoiceMailNumberMissingException {
        Uri uri = intent.getData();
        String scheme = uri.getScheme();
        if ("sip".equals(scheme)) {
            return uri.getSchemeSpecificPart();
        }
        String number = PhoneNumberUtils.getNumberFromIntent(intent, context);
        if (!"voicemail".equals(scheme)) {
            return number;
        }
        if (number == null || TextUtils.isEmpty(number)) {
            throw new VoiceMailNumberMissingException();
        }
        return number;
    }

    static CallerInfo getCallerInfo(Context context, Connection c) {
        CallerInfo info = null;
        if (c != null) {
            Object userDataObject = c.getUserData();
            if (userDataObject instanceof Uri) {
                info = CallerInfo.getCallerInfo(context, (Uri) userDataObject);
                if (info != null) {
                    c.setUserData(info);
                }
            } else {
                if (userDataObject instanceof CallerInfoToken) {
                    info = ((CallerInfoToken) userDataObject).currentInfo;
                } else {
                    info = (CallerInfo) userDataObject;
                }
                if (info == null) {
                    String number = c.getAddress();
                    log("getCallerInfo: number = " + toLogSafePhoneNumber(number));
                    if (!TextUtils.isEmpty(number) && (info = CallerInfo.getCallerInfo(context, number)) != null) {
                        c.setUserData(info);
                    }
                }
            }
        }
        return info;
    }

    static CallerInfoToken startGetCallerInfo(Context context, Connection c, CallerInfoAsyncQuery.OnQueryCompleteListener listener, Object cookie, CallGatewayManager.RawGatewayInfo info) {
        CallerInfoToken cit;
        if (c == null) {
            CallerInfoToken cit2 = new CallerInfoToken();
            cit2.asyncQuery = null;
            return cit2;
        }
        Object userDataObject = c.getUserData();
        if (userDataObject instanceof Uri) {
            cit = new CallerInfoToken();
            cit.currentInfo = new CallerInfo();
            cit.asyncQuery = CallerInfoAsyncQuery.startQuery(-1, context, (Uri) userDataObject, sCallerInfoQueryListener, c);
            cit.asyncQuery.addQueryListener(-1, listener, cookie);
            cit.isFinal = false;
            c.setUserData(cit);
            log("startGetCallerInfo: query based on Uri: " + userDataObject);
        } else if (userDataObject == null) {
            String number = c.getAddress();
            if (info != null && info != CallGatewayManager.EMPTY_INFO) {
                number = info.trueNumber;
            }
            log("PhoneUtils.startGetCallerInfo: new query for phone number...");
            log("- number (address): " + toLogSafePhoneNumber(number));
            log("- c: " + c);
            log("- phone: " + c.getCall().getPhone());
            int phoneType = c.getCall().getPhone().getPhoneType();
            log("- phoneType: " + phoneType);
            switch (phoneType) {
                case 0:
                    log("  ==> PHONE_TYPE_NONE");
                    break;
                case 1:
                    log("  ==> PHONE_TYPE_GSM");
                    break;
                case 2:
                    log("  ==> PHONE_TYPE_CDMA");
                    break;
                case 3:
                    log("  ==> PHONE_TYPE_SIP");
                    break;
                case 4:
                    log("  ==> PHONE_TYPE_THIRD_PARTY");
                    break;
                case 5:
                    log("  ==> PHONE_TYPE_IMS");
                    break;
                default:
                    log("  ==> Unknown phone type");
                    break;
            }
            cit = new CallerInfoToken();
            cit.currentInfo = new CallerInfo();
            cit.currentInfo.cnapName = c.getCnapName();
            cit.currentInfo.name = cit.currentInfo.cnapName;
            cit.currentInfo.numberPresentation = c.getNumberPresentation();
            cit.currentInfo.namePresentation = c.getCnapNamePresentation();
            if (!TextUtils.isEmpty(number)) {
                number = modifyForSpecialCnapCases(context, cit.currentInfo, number, cit.currentInfo.numberPresentation);
                cit.currentInfo.phoneNumber = number;
                if (cit.currentInfo.numberPresentation != 1) {
                    cit.isFinal = true;
                } else {
                    log("==> Actually starting CallerInfoAsyncQuery.startQuery()...");
                    cit.asyncQuery = CallerInfoAsyncQuery.startQuery(-1, context, number, sCallerInfoQueryListener, c);
                    cit.asyncQuery.addQueryListener(-1, listener, cookie);
                    cit.isFinal = false;
                }
            } else {
                log("startGetCallerInfo: No query to start, send trivial reply.");
                cit.isFinal = true;
            }
            c.setUserData(cit);
            log("startGetCallerInfo: query based on number: " + toLogSafePhoneNumber(number));
        } else if (userDataObject instanceof CallerInfoToken) {
            cit = (CallerInfoToken) userDataObject;
            if (cit.asyncQuery != null) {
                cit.asyncQuery.addQueryListener(-1, listener, cookie);
                log("startGetCallerInfo: query already running, adding listener: " + listener.getClass().toString());
            } else {
                String updatedNumber = c.getAddress();
                if (info != null) {
                    updatedNumber = info.trueNumber;
                }
                log("startGetCallerInfo: updatedNumber initially = " + toLogSafePhoneNumber(updatedNumber));
                if (!TextUtils.isEmpty(updatedNumber)) {
                    cit.currentInfo.cnapName = c.getCnapName();
                    cit.currentInfo.name = cit.currentInfo.cnapName;
                    cit.currentInfo.numberPresentation = c.getNumberPresentation();
                    cit.currentInfo.namePresentation = c.getCnapNamePresentation();
                    String updatedNumber2 = modifyForSpecialCnapCases(context, cit.currentInfo, updatedNumber, cit.currentInfo.numberPresentation);
                    cit.currentInfo.phoneNumber = updatedNumber2;
                    log("startGetCallerInfo: updatedNumber=" + toLogSafePhoneNumber(updatedNumber2));
                    log("startGetCallerInfo: CNAP Info from FW(2)");
                    if (cit.currentInfo.numberPresentation != 1) {
                        cit.isFinal = true;
                    } else {
                        cit.asyncQuery = CallerInfoAsyncQuery.startQuery(-1, context, updatedNumber2, sCallerInfoQueryListener, c);
                        cit.asyncQuery.addQueryListener(-1, listener, cookie);
                        cit.isFinal = false;
                    }
                } else {
                    log("startGetCallerInfo: No query to attach to, send trivial reply.");
                    if (cit.currentInfo == null) {
                        cit.currentInfo = new CallerInfo();
                    }
                    cit.currentInfo.cnapName = c.getCnapName();
                    cit.currentInfo.name = cit.currentInfo.cnapName;
                    cit.currentInfo.numberPresentation = c.getNumberPresentation();
                    cit.currentInfo.namePresentation = c.getCnapNamePresentation();
                    log("startGetCallerInfo: CNAP Info from FW(3)");
                    cit.isFinal = true;
                }
            }
        } else {
            cit = new CallerInfoToken();
            cit.currentInfo = (CallerInfo) userDataObject;
            cit.asyncQuery = null;
            cit.isFinal = true;
            log("startGetCallerInfo: query already done, returning CallerInfo");
            log("==> cit.currentInfo = " + cit.currentInfo);
        }
        return cit;
    }

    static void turnOnSpeaker(Context context, boolean flag, boolean store) {
        log("turnOnSpeaker(flag=" + flag + ", store=" + store + ")...");
        PhoneGlobals app = PhoneGlobals.getInstance();
        AudioManager audioManager = (AudioManager) context.getSystemService("audio");
        audioManager.setSpeakerphoneOn(flag);
        if (store) {
            sIsSpeakerEnabled = flag;
        }
        app.updateWakeState();
        app.mCM.setEchoSuppressionEnabled();
    }

    static boolean isSpeakerOn(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService("audio");
        return audioManager.isSpeakerphoneOn();
    }

    static void setAudioMode() {
    }

    static void setAudioMode(CallManager cm) {
    }

    private static int checkCnapSpecialCases(String n) {
        if (n.equals("PRIVATE") || n.equals("P") || n.equals("RES")) {
            log("checkCnapSpecialCases, PRIVATE string: " + n);
            return 2;
        }
        if (n.equals("UNAVAILABLE") || n.equals("UNKNOWN") || n.equals("UNA") || n.equals("U")) {
            log("checkCnapSpecialCases, UNKNOWN string: " + n);
            return 3;
        }
        log("checkCnapSpecialCases, normal str. number: " + n);
        return -1;
    }

    static String modifyForSpecialCnapCases(Context context, CallerInfo ci, String number, int presentation) {
        int cnapSpecialCase;
        if (ci == null || number == null) {
            return number;
        }
        log("modifyForSpecialCnapCases: initially, number=" + toLogSafePhoneNumber(number) + ", presentation=" + presentation + " ci " + ci);
        String[] absentNumberValues = context.getResources().getStringArray(R.array.absent_num);
        if (Arrays.asList(absentNumberValues).contains(number) && presentation == 1) {
            number = context.getString(R.string.unknown);
            ci.numberPresentation = 3;
        }
        if ((ci.numberPresentation == 1 || (ci.numberPresentation != presentation && presentation == 1)) && (cnapSpecialCase = checkCnapSpecialCases(number)) != -1) {
            if (cnapSpecialCase == 2) {
                number = context.getString(R.string.private_num);
            } else if (cnapSpecialCase == 3) {
                number = context.getString(R.string.unknown);
            }
            log("SpecialCnap: number=" + toLogSafePhoneNumber(number) + "; presentation now=" + cnapSpecialCase);
            ci.numberPresentation = cnapSpecialCase;
        }
        log("modifyForSpecialCnapCases: returning number string=" + toLogSafePhoneNumber(number));
        return number;
    }

    private static boolean isRoutableViaGateway(String number) {
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        String number2 = PhoneNumberUtils.stripSeparators(number);
        if (number2.equals(PhoneNumberUtils.convertKeypadLettersToDigits(number2))) {
            return PhoneNumberUtils.isGlobalPhoneNumber(PhoneNumberUtils.extractNetworkPortion(number2));
        }
        return false;
    }

    private static boolean activateSpeakerIfDocked(Phone phone) {
        log("activateSpeakerIfDocked()...");
        if (PhoneGlobals.mDockState != 0) {
            log("activateSpeakerIfDocked(): In a dock -> may need to turn on speaker.");
            PhoneGlobals app = PhoneGlobals.getInstance();
            app.getBluetoothManager();
        }
        return false;
    }

    static boolean isPhoneInEcm(Phone phone) {
        String ecmMode;
        if (phone == null || !TelephonyCapabilities.supportsEcm(phone) || (ecmMode = SystemProperties.get("ril.cdma.inecmmode")) == null) {
            return false;
        }
        return ecmMode.equals("true");
    }

    public static Phone pickPhoneBasedOnNumber(CallManager cm, String scheme, String number, String primarySipUri, ComponentName thirdPartyCallComponent) {
        Phone phone;
        log("pickPhoneBasedOnNumber: scheme " + scheme + ", number " + toLogSafePhoneNumber(number) + ", sipUri " + (primarySipUri != null ? Uri.parse(primarySipUri).toSafeString() : "null") + ", thirdPartyCallComponent: " + thirdPartyCallComponent);
        return (primarySipUri == null || (phone = getSipPhoneFromUri(cm, primarySipUri)) == null) ? cm.getDefaultPhone() : phone;
    }

    public static Phone getSipPhoneFromUri(CallManager cm, String target) {
        for (SipPhone sipPhone : cm.getAllPhones()) {
            if (sipPhone.getPhoneType() == 3) {
                String sipUri = sipPhone.getSipUri();
                if (target.equals(sipUri)) {
                    log("- pickPhoneBasedOnNumber:found SipPhone! obj = " + sipPhone + ", " + sipPhone.getClass());
                    return sipPhone;
                }
            }
        }
        return null;
    }

    public static boolean isRealIncomingCall(Call.State state) {
        return state == Call.State.INCOMING && !PhoneGlobals.getInstance().mCM.hasActiveFgCall();
    }

    static void displaySSN(Context context, SuppServiceNotification notification) {
        int textid = -1;
        try {
            if (notification.notificationType == 0) {
                int[] mo_string = {R.string.mo_unconditional_cf_active, R.string.mo_some_cf_active, R.string.mo_call_forwarded, R.string.mo_call_is_waiting, R.string.mo_cug_call, R.string.mo_outgoing_calls_barred, R.string.mo_incoming_calls_barred, R.string.mo_clir_suppression_rejected, R.string.mo_call_deflected};
                textid = mo_string[notification.code];
            } else if (notification.notificationType == 1) {
                int[] mt_string = {R.string.mt_forwarded_call, R.string.mt_cug_call, R.string.mt_call_on_hold, R.string.mt_call_retrieved, R.string.mt_multi_party_call, R.string.mt_on_hold_call_released, R.string.mt_forward_check_received, R.string.mt_call_connecting_ect, R.string.mt_call_connected_ect, R.string.mt_deflected_call, R.string.mt_additional_call_forwarded};
                textid = mt_string[notification.code];
            }
        } catch (IndexOutOfBoundsException ex) {
            Log.e("PhoneUtils", "SSN code is incorrect: " + notification.code + ", catch " + ex, ex);
        }
        if (textid != -1) {
            Toast.makeText(context, context.getText(textid), 1).show();
        }
    }

    private static void log(String msg) {
        Log.d("PhoneUtils", msg);
    }

    public static PhoneAccountHandle makePstnPhoneAccountHandle(Phone phone) {
        return makePstnPhoneAccountHandleWithPrefix(phone, "", false);
    }

    public static PhoneAccountHandle makePstnPhoneAccountHandleWithPrefix(Phone phone, String prefix, boolean isEmergency) {
        ComponentName pstnConnectionServiceName = new ComponentName(phone.getContext(), (Class<?>) TelephonyConnectionService.class);
        String id = isEmergency ? "E" : prefix + String.valueOf(phone.getSubId());
        return new PhoneAccountHandle(pstnConnectionServiceName, id);
    }

    static final void registerIccStatus(Handler handler, int event) {
        Phone[] arr$ = PhoneFactory.getPhones();
        for (Phone phone : arr$) {
            IccCard sim = phone.getIccCard();
            if (sim != null) {
                sim.registerForNetworkLocked(handler, event, phone);
            }
        }
    }

    static final void registerForSuppServiceNotification(Handler handler, int event, Object obj) {
        Phone[] arr$ = PhoneFactory.getPhones();
        for (Phone phone : arr$) {
            if (phone != null) {
                phone.registerForSuppServiceNotification(handler, event, obj);
            }
        }
    }
}
