package android.net.sip;

import android.R;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.sip.ISipService;
import android.net.sip.SipAudioCall;
import android.net.sip.SipProfile;
import android.net.sip.SipSession;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import java.text.ParseException;

public class SipManager {
    public static final String ACTION_SIP_ADD_PHONE = "com.android.phone.SIP_ADD_PHONE";
    public static final String ACTION_SIP_CALL_OPTION_CHANGED = "com.android.phone.SIP_CALL_OPTION_CHANGED";
    public static final String ACTION_SIP_INCOMING_CALL = "com.android.phone.SIP_INCOMING_CALL";
    public static final String ACTION_SIP_REMOVE_PHONE = "com.android.phone.SIP_REMOVE_PHONE";
    public static final String ACTION_SIP_SERVICE_UP = "android.net.sip.SIP_SERVICE_UP";
    public static final String EXTRA_CALL_ID = "android:sipCallID";
    public static final String EXTRA_LOCAL_URI = "android:localSipUri";
    public static final String EXTRA_OFFER_SD = "android:sipOfferSD";
    public static final int INCOMING_CALL_RESULT_CODE = 101;
    private static final String TAG = "SipManager";
    private Context mContext;
    private ISipService mSipService;

    public static SipManager newInstance(Context context) {
        if (isApiSupported(context)) {
            return new SipManager(context);
        }
        return null;
    }

    public static boolean isApiSupported(Context context) {
        return context.getPackageManager().hasSystemFeature("android.software.sip");
    }

    public static boolean isVoipSupported(Context context) {
        return context.getPackageManager().hasSystemFeature("android.software.sip.voip") && isApiSupported(context);
    }

    public static boolean isSipWifiOnly(Context context) {
        return context.getResources().getBoolean(R.^attr-private.navigationButtonStyle);
    }

    private SipManager(Context context) {
        this.mContext = context;
        createSipService();
    }

    private void createSipService() {
        if (this.mSipService == null) {
            this.mSipService = ISipService.Stub.asInterface(ServiceManager.getService("sip"));
        }
    }

    private void checkSipServiceConnection() throws SipException {
        createSipService();
        if (this.mSipService == null) {
            throw new SipException("SipService is dead and is restarting...", new Exception());
        }
    }

    public void open(SipProfile sipProfile) throws SipException {
        try {
            checkSipServiceConnection();
            this.mSipService.open(sipProfile, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw new SipException("open()", e);
        }
    }

    public void open(SipProfile sipProfile, PendingIntent pendingIntent, SipRegistrationListener sipRegistrationListener) throws SipException {
        if (pendingIntent == null) {
            throw new NullPointerException("incomingCallPendingIntent cannot be null");
        }
        try {
            checkSipServiceConnection();
            this.mSipService.open3(sipProfile, pendingIntent, createRelay(sipRegistrationListener, sipProfile.getUriString()), this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw new SipException("open()", e);
        }
    }

    public void setRegistrationListener(String str, SipRegistrationListener sipRegistrationListener) throws SipException {
        try {
            checkSipServiceConnection();
            this.mSipService.setRegistrationListener(str, createRelay(sipRegistrationListener, str), this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw new SipException("setRegistrationListener()", e);
        }
    }

    public void close(String str) throws SipException {
        try {
            checkSipServiceConnection();
            this.mSipService.close(str, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw new SipException("close()", e);
        }
    }

    public boolean isOpened(String str) throws SipException {
        try {
            checkSipServiceConnection();
            return this.mSipService.isOpened(str, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw new SipException("isOpened()", e);
        }
    }

    public boolean isRegistered(String str) throws SipException {
        try {
            checkSipServiceConnection();
            return this.mSipService.isRegistered(str, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw new SipException("isRegistered()", e);
        }
    }

    public SipAudioCall makeAudioCall(SipProfile sipProfile, SipProfile sipProfile2, SipAudioCall.Listener listener, int i) throws SipException {
        if (!isVoipSupported(this.mContext)) {
            throw new SipException("VOIP API is not supported");
        }
        SipAudioCall sipAudioCall = new SipAudioCall(this.mContext, sipProfile);
        sipAudioCall.setListener(listener);
        sipAudioCall.makeCall(sipProfile2, createSipSession(sipProfile, null), i);
        return sipAudioCall;
    }

    public SipAudioCall makeAudioCall(String str, String str2, SipAudioCall.Listener listener, int i) throws SipException {
        if (!isVoipSupported(this.mContext)) {
            throw new SipException("VOIP API is not supported");
        }
        try {
            return makeAudioCall(new SipProfile.Builder(str).build(), new SipProfile.Builder(str2).build(), listener, i);
        } catch (ParseException e) {
            throw new SipException("build SipProfile", e);
        }
    }

    public SipAudioCall takeAudioCall(Intent intent, SipAudioCall.Listener listener) throws SipException {
        if (intent == null) {
            throw new SipException("Cannot retrieve session with null intent");
        }
        String callId = getCallId(intent);
        if (callId == null) {
            throw new SipException("Call ID missing in incoming call intent");
        }
        String offerSessionDescription = getOfferSessionDescription(intent);
        if (offerSessionDescription == null) {
            throw new SipException("Session description missing in incoming call intent");
        }
        try {
            checkSipServiceConnection();
            ISipSession pendingSession = this.mSipService.getPendingSession(callId, this.mContext.getOpPackageName());
            if (pendingSession == null) {
                throw new SipException("No pending session for the call");
            }
            SipAudioCall sipAudioCall = new SipAudioCall(this.mContext, pendingSession.getLocalProfile());
            sipAudioCall.attachCall(new SipSession(pendingSession), offerSessionDescription);
            sipAudioCall.setListener(listener);
            return sipAudioCall;
        } catch (Throwable th) {
            throw new SipException("takeAudioCall()", th);
        }
    }

    public static boolean isIncomingCallIntent(Intent intent) {
        if (intent == null) {
            return false;
        }
        return (getCallId(intent) == null || getOfferSessionDescription(intent) == null) ? false : true;
    }

    public static String getCallId(Intent intent) {
        return intent.getStringExtra(EXTRA_CALL_ID);
    }

    public static String getOfferSessionDescription(Intent intent) {
        return intent.getStringExtra(EXTRA_OFFER_SD);
    }

    public static Intent createIncomingCallBroadcast(String str, String str2) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CALL_ID, str);
        intent.putExtra(EXTRA_OFFER_SD, str2);
        return intent;
    }

    public void register(SipProfile sipProfile, int i, SipRegistrationListener sipRegistrationListener) throws SipException {
        try {
            checkSipServiceConnection();
            ISipSession iSipSessionCreateSession = this.mSipService.createSession(sipProfile, createRelay(sipRegistrationListener, sipProfile.getUriString()), this.mContext.getOpPackageName());
            if (iSipSessionCreateSession == null) {
                throw new SipException("SipService.createSession() returns null");
            }
            iSipSessionCreateSession.register(i);
        } catch (RemoteException e) {
            throw new SipException("register()", e);
        }
    }

    public void unregister(SipProfile sipProfile, SipRegistrationListener sipRegistrationListener) throws SipException {
        try {
            checkSipServiceConnection();
            ISipSession iSipSessionCreateSession = this.mSipService.createSession(sipProfile, createRelay(sipRegistrationListener, sipProfile.getUriString()), this.mContext.getOpPackageName());
            if (iSipSessionCreateSession == null) {
                throw new SipException("SipService.createSession() returns null");
            }
            iSipSessionCreateSession.unregister();
        } catch (RemoteException e) {
            throw new SipException("unregister()", e);
        }
    }

    public SipSession getSessionFor(Intent intent) throws SipException {
        try {
            checkSipServiceConnection();
            ISipSession pendingSession = this.mSipService.getPendingSession(getCallId(intent), this.mContext.getOpPackageName());
            if (pendingSession == null) {
                return null;
            }
            return new SipSession(pendingSession);
        } catch (RemoteException e) {
            throw new SipException("getSessionFor()", e);
        }
    }

    private static ISipSessionListener createRelay(SipRegistrationListener sipRegistrationListener, String str) {
        if (sipRegistrationListener == null) {
            return null;
        }
        return new ListenerRelay(sipRegistrationListener, str);
    }

    public SipSession createSipSession(SipProfile sipProfile, SipSession.Listener listener) throws SipException {
        try {
            checkSipServiceConnection();
            ISipSession iSipSessionCreateSession = this.mSipService.createSession(sipProfile, null, this.mContext.getOpPackageName());
            if (iSipSessionCreateSession == null) {
                throw new SipException("Failed to create SipSession; network unavailable?");
            }
            return new SipSession(iSipSessionCreateSession, listener);
        } catch (RemoteException e) {
            throw new SipException("createSipSession()", e);
        }
    }

    public SipProfile[] getListOfProfiles() throws SipException {
        try {
            checkSipServiceConnection();
            return this.mSipService.getListOfProfiles(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            return new SipProfile[0];
        }
    }

    private static class ListenerRelay extends SipSessionAdapter {
        private SipRegistrationListener mListener;
        private String mUri;

        public ListenerRelay(SipRegistrationListener sipRegistrationListener, String str) {
            this.mListener = sipRegistrationListener;
            this.mUri = str;
        }

        private String getUri(ISipSession iSipSession) {
            String uriString;
            try {
                if (iSipSession == null) {
                    uriString = this.mUri;
                } else {
                    uriString = iSipSession.getLocalProfile().getUriString();
                }
                return uriString;
            } catch (Throwable th) {
                Rlog.e(SipManager.TAG, "getUri(): ", th);
                return null;
            }
        }

        @Override
        public void onRegistering(ISipSession iSipSession) {
            this.mListener.onRegistering(getUri(iSipSession));
        }

        @Override
        public void onRegistrationDone(ISipSession iSipSession, int i) {
            long jCurrentTimeMillis = i;
            if (i > 0) {
                jCurrentTimeMillis += System.currentTimeMillis();
            }
            this.mListener.onRegistrationDone(getUri(iSipSession), jCurrentTimeMillis);
        }

        @Override
        public void onRegistrationFailed(ISipSession iSipSession, int i, String str) {
            this.mListener.onRegistrationFailed(getUri(iSipSession), i, str);
        }

        @Override
        public void onRegistrationTimeout(ISipSession iSipSession) {
            this.mListener.onRegistrationFailed(getUri(iSipSession), -5, "registration timed out");
        }
    }
}
