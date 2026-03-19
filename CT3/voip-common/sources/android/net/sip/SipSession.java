package android.net.sip;

import android.net.sip.ISipSessionListener;
import android.os.RemoteException;
import android.telephony.Rlog;

public final class SipSession {
    private static final String TAG = "SipSession";
    private Listener mListener;
    private final ISipSession mSession;

    public static class State {
        public static final int DEREGISTERING = 2;
        public static final int ENDING_CALL = 10;
        public static final int INCOMING_CALL = 3;
        public static final int INCOMING_CALL_ANSWERING = 4;
        public static final int IN_CALL = 8;
        public static final int NOT_DEFINED = 101;
        public static final int OUTGOING_CALL = 5;
        public static final int OUTGOING_CALL_CANCELING = 7;
        public static final int OUTGOING_CALL_RING_BACK = 6;
        public static final int PINGING = 9;
        public static final int READY_TO_CALL = 0;
        public static final int REGISTERING = 1;

        public static String toString(int state) {
            switch (state) {
                case 0:
                    return "READY_TO_CALL";
                case 1:
                    return "REGISTERING";
                case 2:
                    return "DEREGISTERING";
                case 3:
                    return "INCOMING_CALL";
                case INCOMING_CALL_ANSWERING:
                    return "INCOMING_CALL_ANSWERING";
                case OUTGOING_CALL:
                    return "OUTGOING_CALL";
                case OUTGOING_CALL_RING_BACK:
                    return "OUTGOING_CALL_RING_BACK";
                case OUTGOING_CALL_CANCELING:
                    return "OUTGOING_CALL_CANCELING";
                case IN_CALL:
                    return "IN_CALL";
                case PINGING:
                    return "PINGING";
                default:
                    return "NOT_DEFINED";
            }
        }

        private State() {
        }
    }

    public static class Listener {
        public void onCalling(SipSession session) {
        }

        public void onRinging(SipSession session, SipProfile caller, String sessionDescription) {
        }

        public void onRingingBack(SipSession session) {
        }

        public void onCallEstablished(SipSession session, String sessionDescription) {
        }

        public void onCallEnded(SipSession session) {
        }

        public void onCallBusy(SipSession session) {
        }

        public void onCallTransferring(SipSession newSession, String sessionDescription) {
        }

        public void onError(SipSession session, int errorCode, String errorMessage) {
        }

        public void onCallChangeFailed(SipSession session, int errorCode, String errorMessage) {
        }

        public void onRegistering(SipSession session) {
        }

        public void onRegistrationDone(SipSession session, int duration) {
        }

        public void onRegistrationFailed(SipSession session, int errorCode, String errorMessage) {
        }

        public void onRegistrationTimeout(SipSession session) {
        }
    }

    SipSession(ISipSession realSession) {
        this.mSession = realSession;
        if (realSession == null) {
            return;
        }
        try {
            realSession.setListener(createListener());
        } catch (RemoteException e) {
            loge("SipSession.setListener:", e);
        }
    }

    SipSession(ISipSession realSession, Listener listener) {
        this(realSession);
        setListener(listener);
    }

    public String getLocalIp() {
        try {
            return this.mSession.getLocalIp();
        } catch (RemoteException e) {
            loge("getLocalIp:", e);
            return "127.0.0.1";
        }
    }

    public SipProfile getLocalProfile() {
        try {
            return this.mSession.getLocalProfile();
        } catch (RemoteException e) {
            loge("getLocalProfile:", e);
            return null;
        }
    }

    public SipProfile getPeerProfile() {
        try {
            return this.mSession.getPeerProfile();
        } catch (RemoteException e) {
            loge("getPeerProfile:", e);
            return null;
        }
    }

    public int getState() {
        try {
            return this.mSession.getState();
        } catch (RemoteException e) {
            loge("getState:", e);
            return 101;
        }
    }

    public boolean isInCall() {
        try {
            return this.mSession.isInCall();
        } catch (RemoteException e) {
            loge("isInCall:", e);
            return false;
        }
    }

    public String getCallId() {
        try {
            return this.mSession.getCallId();
        } catch (RemoteException e) {
            loge("getCallId:", e);
            return null;
        }
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void register(int duration) {
        try {
            this.mSession.register(duration);
        } catch (RemoteException e) {
            loge("register:", e);
        }
    }

    public void unregister() {
        try {
            this.mSession.unregister();
        } catch (RemoteException e) {
            loge("unregister:", e);
        }
    }

    public void makeCall(SipProfile callee, String sessionDescription, int timeout) {
        try {
            this.mSession.makeCall(callee, sessionDescription, timeout);
        } catch (RemoteException e) {
            loge("makeCall:", e);
        }
    }

    public void answerCall(String sessionDescription, int timeout) {
        try {
            this.mSession.answerCall(sessionDescription, timeout);
        } catch (RemoteException e) {
            loge("answerCall:", e);
        }
    }

    public void endCall() {
        try {
            this.mSession.endCall();
        } catch (RemoteException e) {
            loge("endCall:", e);
        }
    }

    public void changeCall(String sessionDescription, int timeout) {
        try {
            this.mSession.changeCall(sessionDescription, timeout);
        } catch (RemoteException e) {
            loge("changeCall:", e);
        }
    }

    ISipSession getRealSession() {
        return this.mSession;
    }

    private ISipSessionListener createListener() {
        return new ISipSessionListener.Stub() {
            @Override
            public void onCalling(ISipSession session) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onCalling(SipSession.this);
            }

            @Override
            public void onRinging(ISipSession session, SipProfile caller, String sessionDescription) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onRinging(SipSession.this, caller, sessionDescription);
            }

            @Override
            public void onRingingBack(ISipSession session) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onRingingBack(SipSession.this);
            }

            @Override
            public void onCallEstablished(ISipSession session, String sessionDescription) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onCallEstablished(SipSession.this, sessionDescription);
            }

            @Override
            public void onCallEnded(ISipSession session) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onCallEnded(SipSession.this);
            }

            @Override
            public void onCallBusy(ISipSession session) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onCallBusy(SipSession.this);
            }

            @Override
            public void onCallTransferring(ISipSession session, String sessionDescription) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onCallTransferring(new SipSession(session, SipSession.this.mListener), sessionDescription);
            }

            @Override
            public void onCallChangeFailed(ISipSession session, int errorCode, String message) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onCallChangeFailed(SipSession.this, errorCode, message);
            }

            @Override
            public void onError(ISipSession session, int errorCode, String message) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onError(SipSession.this, errorCode, message);
            }

            @Override
            public void onRegistering(ISipSession session) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onRegistering(SipSession.this);
            }

            @Override
            public void onRegistrationDone(ISipSession session, int duration) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onRegistrationDone(SipSession.this, duration);
            }

            @Override
            public void onRegistrationFailed(ISipSession session, int errorCode, String message) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onRegistrationFailed(SipSession.this, errorCode, message);
            }

            @Override
            public void onRegistrationTimeout(ISipSession session) {
                if (SipSession.this.mListener == null) {
                    return;
                }
                SipSession.this.mListener.onRegistrationTimeout(SipSession.this);
            }
        };
    }

    private void loge(String s, Throwable t) {
        Rlog.e(TAG, s, t);
    }
}
