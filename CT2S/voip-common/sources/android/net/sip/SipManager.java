package android.net.sip;

import android.R;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.sip.ISipService;
import android.net.sip.SipAudioCall;
import android.net.sip.SipProfile;
import android.net.sip.SipSession;
import android.os.IBinder;
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
        return context.getResources().getBoolean(R.^attr-private.floatingToolbarCloseDrawable);
    }

    private SipManager(Context context) {
        this.mContext = context;
        createSipService();
    }

    private void createSipService() {
        IBinder b = ServiceManager.getService("sip");
        this.mSipService = ISipService.Stub.asInterface(b);
    }

    public void open(SipProfile localProfile) throws SipException {
        try {
            this.mSipService.open(localProfile);
        } catch (RemoteException e) {
            throw new SipException("open()", e);
        }
    }

    public void open(SipProfile localProfile, PendingIntent incomingCallPendingIntent, SipRegistrationListener listener) throws SipException {
        if (incomingCallPendingIntent == null) {
            throw new NullPointerException("incomingCallPendingIntent cannot be null");
        }
        try {
            this.mSipService.open3(localProfile, incomingCallPendingIntent, createRelay(listener, localProfile.getUriString()));
        } catch (RemoteException e) {
            throw new SipException("open()", e);
        }
    }

    public void setRegistrationListener(String localProfileUri, SipRegistrationListener listener) throws SipException {
        try {
            this.mSipService.setRegistrationListener(localProfileUri, createRelay(listener, localProfileUri));
        } catch (RemoteException e) {
            throw new SipException("setRegistrationListener()", e);
        }
    }

    public void close(String localProfileUri) throws SipException {
        try {
            this.mSipService.close(localProfileUri);
        } catch (RemoteException e) {
            throw new SipException("close()", e);
        }
    }

    public boolean isOpened(String localProfileUri) throws SipException {
        try {
            return this.mSipService.isOpened(localProfileUri);
        } catch (RemoteException e) {
            throw new SipException("isOpened()", e);
        }
    }

    public boolean isRegistered(String localProfileUri) throws SipException {
        try {
            return this.mSipService.isRegistered(localProfileUri);
        } catch (RemoteException e) {
            throw new SipException("isRegistered()", e);
        }
    }

    public SipAudioCall makeAudioCall(SipProfile localProfile, SipProfile peerProfile, SipAudioCall.Listener listener, int timeout) throws SipException {
        if (!isVoipSupported(this.mContext)) {
            throw new SipException("VOIP API is not supported");
        }
        SipAudioCall call = new SipAudioCall(this.mContext, localProfile);
        call.setListener(listener);
        SipSession s = createSipSession(localProfile, null);
        call.makeCall(peerProfile, s, timeout);
        return call;
    }

    public SipAudioCall makeAudioCall(String localProfileUri, String peerProfileUri, SipAudioCall.Listener listener, int timeout) throws SipException {
        if (!isVoipSupported(this.mContext)) {
            throw new SipException("VOIP API is not supported");
        }
        try {
            return makeAudioCall(new SipProfile.Builder(localProfileUri).build(), new SipProfile.Builder(peerProfileUri).build(), listener, timeout);
        } catch (ParseException e) {
            throw new SipException("build SipProfile", e);
        }
    }

    public SipAudioCall takeAudioCall(Intent incomingCallIntent, SipAudioCall.Listener listener) throws SipException {
        if (incomingCallIntent == null) {
            throw new SipException("Cannot retrieve session with null intent");
        }
        String callId = getCallId(incomingCallIntent);
        if (callId == null) {
            throw new SipException("Call ID missing in incoming call intent");
        }
        String offerSd = getOfferSessionDescription(incomingCallIntent);
        if (offerSd == null) {
            throw new SipException("Session description missing in incoming call intent");
        }
        try {
            ISipSession session = this.mSipService.getPendingSession(callId);
            if (session == null) {
                throw new SipException("No pending session for the call");
            }
            SipAudioCall call = new SipAudioCall(this.mContext, session.getLocalProfile());
            call.attachCall(new SipSession(session), offerSd);
            call.setListener(listener);
            return call;
        } catch (Throwable t) {
            throw new SipException("takeAudioCall()", t);
        }
    }

    public static boolean isIncomingCallIntent(Intent intent) {
        if (intent == null) {
            return false;
        }
        String callId = getCallId(intent);
        String offerSd = getOfferSessionDescription(intent);
        return (callId == null || offerSd == null) ? false : true;
    }

    public static String getCallId(Intent incomingCallIntent) {
        return incomingCallIntent.getStringExtra(EXTRA_CALL_ID);
    }

    public static String getOfferSessionDescription(Intent incomingCallIntent) {
        return incomingCallIntent.getStringExtra(EXTRA_OFFER_SD);
    }

    public static Intent createIncomingCallBroadcast(String callId, String sessionDescription) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CALL_ID, callId);
        intent.putExtra(EXTRA_OFFER_SD, sessionDescription);
        return intent;
    }

    public void register(SipProfile localProfile, int expiryTime, SipRegistrationListener listener) throws SipException {
        try {
            ISipSession session = this.mSipService.createSession(localProfile, createRelay(listener, localProfile.getUriString()));
            if (session == null) {
                throw new SipException("SipService.createSession() returns null");
            }
            session.register(expiryTime);
        } catch (RemoteException e) {
            throw new SipException("register()", e);
        }
    }

    public void unregister(SipProfile localProfile, SipRegistrationListener listener) throws SipException {
        try {
            ISipSession session = this.mSipService.createSession(localProfile, createRelay(listener, localProfile.getUriString()));
            if (session == null) {
                throw new SipException("SipService.createSession() returns null");
            }
            session.unregister();
        } catch (RemoteException e) {
            throw new SipException("unregister()", e);
        }
    }

    public SipSession getSessionFor(Intent incomingCallIntent) throws SipException {
        try {
            String callId = getCallId(incomingCallIntent);
            ISipSession s = this.mSipService.getPendingSession(callId);
            if (s == null) {
                return null;
            }
            return new SipSession(s);
        } catch (RemoteException e) {
            throw new SipException("getSessionFor()", e);
        }
    }

    private static ISipSessionListener createRelay(SipRegistrationListener listener, String uri) {
        if (listener == null) {
            return null;
        }
        return new ListenerRelay(listener, uri);
    }

    public SipSession createSipSession(SipProfile localProfile, SipSession.Listener listener) throws SipException {
        try {
            ISipSession s = this.mSipService.createSession(localProfile, null);
            if (s == null) {
                throw new SipException("Failed to create SipSession; network unavailable?");
            }
            return new SipSession(s, listener);
        } catch (RemoteException e) {
            throw new SipException("createSipSession()", e);
        }
    }

    public SipProfile[] getListOfProfiles() {
        try {
            return this.mSipService.getListOfProfiles();
        } catch (RemoteException e) {
            return new SipProfile[0];
        }
    }

    private static class ListenerRelay extends SipSessionAdapter {
        private SipRegistrationListener mListener;
        private String mUri;

        public ListenerRelay(SipRegistrationListener listener, String uri) {
            this.mListener = listener;
            this.mUri = uri;
        }

        private String getUri(ISipSession session) {
            try {
                return session == null ? this.mUri : session.getLocalProfile().getUriString();
            } catch (Throwable e) {
                Rlog.e(SipManager.TAG, "getUri(): ", e);
                return null;
            }
        }

        @Override
        public void onRegistering(ISipSession session) {
            this.mListener.onRegistering(getUri(session));
        }

        @Override
        public void onRegistrationDone(ISipSession session, int duration) {
            long expiryTime = duration;
            if (duration > 0) {
                expiryTime += System.currentTimeMillis();
            }
            this.mListener.onRegistrationDone(getUri(session), expiryTime);
        }

        @Override
        public void onRegistrationFailed(ISipSession session, int errorCode, String message) {
            this.mListener.onRegistrationFailed(getUri(session), errorCode, message);
        }

        @Override
        public void onRegistrationTimeout(ISipSession session) {
            this.mListener.onRegistrationFailed(getUri(session), -5, "registration timed out");
        }
    }
}
