package com.android.phone;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ProgressBar;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.phone.OtaUtils;

public class OutgoingCallBroadcaster extends Activity implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
    private static final boolean DBG;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 101) {
                Log.i("OutgoingCallBroadcaster", "Outgoing call takes too long. Showing the spinner.");
                OutgoingCallBroadcaster.this.mWaitingSpinner.setVisibility(0);
            } else if (msg.what == 102) {
                OutgoingCallBroadcaster.this.finish();
            } else {
                Log.wtf("OutgoingCallBroadcaster", "Unknown message id: " + msg.what);
            }
        }
    };
    private ProgressBar mWaitingSpinner;

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
    }

    private void startDelayedFinish() {
        this.mHandler.sendEmptyMessageDelayed(102, 2000L);
    }

    public class OutgoingCallReceiver extends BroadcastReceiver {
        public OutgoingCallReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            OutgoingCallBroadcaster.this.mHandler.removeMessages(101);
            boolean isAttemptingCall = doReceive(context, intent);
            if (OutgoingCallBroadcaster.DBG) {
                Log.v("OutgoingCallReceiver", "OutgoingCallReceiver is going to finish the Activity itself.");
            }
            if (isAttemptingCall) {
                OutgoingCallBroadcaster.this.startDelayedFinish();
            } else {
                OutgoingCallBroadcaster.this.finish();
            }
        }

        public boolean doReceive(Context context, Intent intent) {
            if (OutgoingCallBroadcaster.DBG) {
                Log.v("OutgoingCallReceiver", "doReceive: " + intent);
            }
            boolean alreadyCalled = intent.getBooleanExtra("android.phone.extra.ALREADY_CALLED", false);
            if (alreadyCalled) {
                if (OutgoingCallBroadcaster.DBG) {
                    Log.v("OutgoingCallReceiver", "CALL already placed -- returning.");
                }
                return false;
            }
            String number = getResultData();
            PhoneGlobals app = PhoneGlobals.getInstance();
            Phone phone = PhoneGlobals.getPhone();
            if (TelephonyCapabilities.supportsOtasp(phone)) {
                boolean activateState = app.cdmaOtaScreenState.otaScreenState == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION;
                boolean dialogState = app.cdmaOtaScreenState.otaScreenState == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_SUCCESS_FAILURE_DLG;
                boolean isOtaCallActive = false;
                if (app.cdmaOtaScreenState.otaScreenState == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_PROGRESS || app.cdmaOtaScreenState.otaScreenState == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_LISTENING) {
                    isOtaCallActive = true;
                }
                if (activateState || dialogState) {
                    if (dialogState) {
                        app.dismissOtaDialogs();
                    }
                    app.clearOtaState();
                } else if (isOtaCallActive) {
                    Log.w("OutgoingCallReceiver", "OTASP call is active: disallowing a new outgoing call.");
                    return false;
                }
            }
            if (number == null) {
                if (OutgoingCallBroadcaster.DBG) {
                    Log.v("OutgoingCallReceiver", "CALL cancelled (null number), returning...");
                }
                return false;
            }
            if (TelephonyCapabilities.supportsOtasp(phone) && phone.getState() != PhoneConstants.State.IDLE && phone.isOtaSpNumber(number)) {
                if (OutgoingCallBroadcaster.DBG) {
                    Log.v("OutgoingCallReceiver", "Call is active, a 2nd OTA call cancelled -- returning.");
                }
                return false;
            }
            if (PhoneNumberUtils.isPotentialLocalEmergencyNumber(context, number)) {
                Log.w("OutgoingCallReceiver", "Cannot modify outgoing call to emergency number " + number + ".");
                return false;
            }
            String originalUri = intent.getStringExtra("android.phone.extra.ORIGINAL_URI");
            if (originalUri == null) {
                Log.e("OutgoingCallReceiver", "Intent is missing EXTRA_ORIGINAL_URI -- returning.");
                return false;
            }
            Uri uri = Uri.parse(originalUri);
            String number2 = PhoneNumberUtils.stripSeparators(PhoneNumberUtils.convertKeypadLettersToDigits(number));
            if (OutgoingCallBroadcaster.DBG) {
                Log.v("OutgoingCallReceiver", "doReceive: proceeding with call...");
            }
            OutgoingCallBroadcaster.this.startSipCallOptionHandler(context, intent, uri, number2);
            return true;
        }
    }

    private void startSipCallOptionHandler(Context context, Intent intent, Uri uri, String number) {
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.outgoing_call_broadcaster);
        this.mWaitingSpinner = (ProgressBar) findViewById(R.id.spinner);
        Intent intent = getIntent();
        if (DBG) {
            Configuration configuration = getResources().getConfiguration();
            Log.v("OutgoingCallBroadcaster", "onCreate: this = " + this + ", icicle = " + icicle);
            Log.v("OutgoingCallBroadcaster", " - getIntent() = " + intent);
            Log.v("OutgoingCallBroadcaster", " - configuration = " + configuration);
        }
        if (icicle != null) {
            Log.i("OutgoingCallBroadcaster", "onCreate: non-null icicle!  Bailing out, not sending NEW_OUTGOING_CALL broadcast...");
            return;
        }
        processIntent(intent);
        if (DBG) {
            Log.v("OutgoingCallBroadcaster", "At the end of onCreate(). isFinishing(): " + isFinishing());
        }
    }

    private void processIntent(Intent intent) {
        int launchedFromUid;
        String launchedFromPackage;
        boolean callNow;
        if (DBG) {
            Log.v("OutgoingCallBroadcaster", "processIntent() = " + intent + ", thread: " + Thread.currentThread());
        }
        getResources().getConfiguration();
        if (!PhoneGlobals.sVoiceCapable) {
            Log.i("OutgoingCallBroadcaster", "This device is detected as non-voice-capable device.");
            handleNonVoiceCapable(intent);
            return;
        }
        String action = intent.getAction();
        String number = PhoneNumberUtils.getNumberFromIntent(intent, this);
        if (number != null) {
            if (!PhoneNumberUtils.isUriNumber(number)) {
                number = PhoneNumberUtils.stripSeparators(PhoneNumberUtils.convertKeypadLettersToDigits(number));
            }
        } else {
            Log.w("OutgoingCallBroadcaster", "The number obtained from Intent is null.");
        }
        AppOpsManager appOps = (AppOpsManager) getSystemService("appops");
        try {
            launchedFromUid = ActivityManagerNative.getDefault().getLaunchedFromUid(getActivityToken());
            launchedFromPackage = ActivityManagerNative.getDefault().getLaunchedFromPackage(getActivityToken());
        } catch (RemoteException e) {
            launchedFromUid = -1;
            launchedFromPackage = null;
        }
        if (appOps.noteOpNoThrow(13, launchedFromUid, launchedFromPackage) != 0) {
            Log.w("OutgoingCallBroadcaster", "Rejecting call from uid " + launchedFromUid + " package " + launchedFromPackage);
            finish();
            return;
        }
        if (getClass().getName().equals(intent.getComponent().getClassName()) && !"android.intent.action.CALL".equals(intent.getAction())) {
            Log.w("OutgoingCallBroadcaster", "Attempt to deliver non-CALL action; forcing to CALL");
            intent.setAction("android.intent.action.CALL");
        }
        if (number == null || PhoneNumberUtils.isLocalEmergencyNumber(this, number)) {
        }
        boolean isPotentialEmergencyNumber = number != null && PhoneNumberUtils.isPotentialLocalEmergencyNumber(this, number);
        if ("android.intent.action.CALL_PRIVILEGED".equals(action)) {
            if (isPotentialEmergencyNumber) {
                Log.i("OutgoingCallBroadcaster", "ACTION_CALL_PRIVILEGED is used while the number is a potential emergency number. Use ACTION_CALL_EMERGENCY as an action instead.");
                action = "android.intent.action.CALL_EMERGENCY";
            } else {
                action = "android.intent.action.CALL";
            }
            if (DBG) {
                Log.v("OutgoingCallBroadcaster", " - updating action from CALL_PRIVILEGED to " + action);
            }
            intent.setAction(action);
        }
        if ("android.intent.action.CALL".equals(action)) {
            if (isPotentialEmergencyNumber) {
                Log.w("OutgoingCallBroadcaster", "Cannot call potential emergency number '" + number + "' with CALL Intent " + intent + ".");
                Log.i("OutgoingCallBroadcaster", "Launching default dialer instead...");
                Intent invokeFrameworkDialer = new Intent();
                Resources resources = getResources();
                invokeFrameworkDialer.setClassName(resources.getString(R.string.ui_default_package), resources.getString(R.string.dialer_default_class));
                invokeFrameworkDialer.setAction("android.intent.action.DIAL");
                invokeFrameworkDialer.setData(intent.getData());
                if (DBG) {
                    Log.v("OutgoingCallBroadcaster", "onCreate(): calling startActivity for Dialer: " + invokeFrameworkDialer);
                }
                startActivity(invokeFrameworkDialer);
                finish();
                return;
            }
            callNow = false;
        } else if ("android.intent.action.CALL_EMERGENCY".equals(action)) {
            if (!isPotentialEmergencyNumber) {
                Log.w("OutgoingCallBroadcaster", "Cannot call non-potential-emergency number " + number + " with EMERGENCY_CALL Intent " + intent + ". Finish the Activity immediately.");
                finish();
                return;
            }
            callNow = true;
        } else {
            Log.e("OutgoingCallBroadcaster", "Unhandled Intent " + intent + ". Finish the Activity immediately.");
            finish();
            return;
        }
        PhoneGlobals.getInstance().wakeUpScreen();
        if (TextUtils.isEmpty(number)) {
            if (intent.getBooleanExtra("com.android.phone.extra.SEND_EMPTY_FLASH", false)) {
                Log.i("OutgoingCallBroadcaster", "onCreate: SEND_EMPTY_FLASH...");
                PhoneUtils.sendEmptyFlash(PhoneGlobals.getPhone());
                finish();
                return;
            }
            Log.i("OutgoingCallBroadcaster", "onCreate: null or empty number, setting callNow=true...");
            callNow = true;
        }
        if (callNow) {
            Log.i("OutgoingCallBroadcaster", "onCreate(): callNow case! Calling placeCall(): " + intent);
            PhoneGlobals.getInstance().callController.placeCall(intent);
        }
        Uri uri = intent.getData();
        String scheme = uri.getScheme();
        if ("sip".equals(scheme) || PhoneNumberUtils.isUriNumber(number)) {
            Log.i("OutgoingCallBroadcaster", "The requested number was detected as SIP call.");
            startSipCallOptionHandler(this, intent, uri, number);
            finish();
            return;
        }
        Intent broadcastIntent = new Intent("android.intent.action.NEW_OUTGOING_CALL");
        if (number != null) {
            broadcastIntent.putExtra("android.intent.extra.PHONE_NUMBER", number);
        }
        CallGatewayManager.checkAndCopyPhoneProviderExtras(intent, broadcastIntent);
        broadcastIntent.putExtra("android.phone.extra.ALREADY_CALLED", callNow);
        broadcastIntent.putExtra("android.phone.extra.ORIGINAL_URI", uri.toString());
        broadcastIntent.addFlags(268435456);
        if (DBG) {
            Log.v("OutgoingCallBroadcaster", " - Broadcasting intent: " + broadcastIntent + ".");
        }
        this.mHandler.sendEmptyMessageDelayed(101, 2000L);
        sendOrderedBroadcastAsUser(broadcastIntent, UserHandle.OWNER, "android.permission.PROCESS_OUTGOING_CALLS", new OutgoingCallReceiver(), null, -1, number, null);
    }

    @Override
    protected void onStop() {
        removeDialog(1);
        super.onStop();
    }

    private void handleNonVoiceCapable(Intent intent) {
        if (DBG) {
            Log.v("OutgoingCallBroadcaster", "handleNonVoiceCapable: handling " + intent + " on non-voice-capable device...");
        }
        showDialog(1);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 1:
                Dialog dialog = new AlertDialog.Builder(this).setTitle(R.string.not_voice_capable).setIconAttribute(android.R.attr.alertDialogIcon).setPositiveButton(android.R.string.ok, this).setOnCancelListener(this).create();
                return dialog;
            default:
                Log.w("OutgoingCallBroadcaster", "onCreateDialog: unexpected ID " + id);
                return null;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int id) {
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (DBG) {
            Log.v("OutgoingCallBroadcaster", "onConfigurationChanged: newConfig = " + newConfig);
        }
    }
}
