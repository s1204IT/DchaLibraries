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

        public static String toString(int i) {
            switch (i) {
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
        public void onCalling(SipSession sipSession) {
        }

        public void onRinging(SipSession sipSession, SipProfile sipProfile, String str) {
        }

        public void onRingingBack(SipSession sipSession) {
        }

        public void onCallEstablished(SipSession sipSession, String str) {
        }

        public void onCallEnded(SipSession sipSession) {
        }

        public void onCallBusy(SipSession sipSession) {
        }

        public void onCallTransferring(SipSession sipSession, String str) {
        }

        public void onError(SipSession sipSession, int i, String str) {
        }

        public void onCallChangeFailed(SipSession sipSession, int i, String str) {
        }

        public void onRegistering(SipSession sipSession) {
        }

        public void onRegistrationDone(SipSession sipSession, int i) {
        }

        public void onRegistrationFailed(SipSession sipSession, int i, String str) {
        }

        public void onRegistrationTimeout(SipSession sipSession) {
        }
    }

    SipSession(ISipSession iSipSession) {
        this.mSession = iSipSession;
        if (iSipSession != null) {
            try {
                iSipSession.setListener(createListener());
            } catch (RemoteException e) {
                loge("SipSession.setListener:", e);
            }
        }
    }

    SipSession(ISipSession iSipSession, Listener listener) {
        this(iSipSession);
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

    public void register(int i) {
        try {
            this.mSession.register(i);
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

    public void makeCall(SipProfile sipProfile, String str, int i) {
        try {
            this.mSession.makeCall(sipProfile, str, i);
        } catch (RemoteException e) {
            loge("makeCall:", e);
        }
    }

    public void answerCall(String str, int i) {
        try {
            this.mSession.answerCall(str, i);
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

    public void changeCall(String str, int i) {
        try {
            this.mSession.changeCall(str, i);
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
            public void onCalling(ISipSession iSipSession) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onCalling(SipSession.this);
                }
            }

            @Override
            public void onRinging(ISipSession iSipSession, SipProfile sipProfile, String str) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onRinging(SipSession.this, sipProfile, str);
                }
            }

            @Override
            public void onRingingBack(ISipSession iSipSession) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onRingingBack(SipSession.this);
                }
            }

            @Override
            public void onCallEstablished(ISipSession iSipSession, String str) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onCallEstablished(SipSession.this, str);
                }
            }

            @Override
            public void onCallEnded(ISipSession iSipSession) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onCallEnded(SipSession.this);
                }
            }

            @Override
            public void onCallBusy(ISipSession iSipSession) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onCallBusy(SipSession.this);
                }
            }

            @Override
            public void onCallTransferring(ISipSession iSipSession, String str) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onCallTransferring(new SipSession(iSipSession, SipSession.this.mListener), str);
                }
            }

            @Override
            public void onCallChangeFailed(ISipSession iSipSession, int i, String str) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onCallChangeFailed(SipSession.this, i, str);
                }
            }

            @Override
            public void onError(ISipSession iSipSession, int i, String str) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onError(SipSession.this, i, str);
                }
            }

            @Override
            public void onRegistering(ISipSession iSipSession) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onRegistering(SipSession.this);
                }
            }

            @Override
            public void onRegistrationDone(ISipSession iSipSession, int i) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onRegistrationDone(SipSession.this, i);
                }
            }

            @Override
            public void onRegistrationFailed(ISipSession iSipSession, int i, String str) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onRegistrationFailed(SipSession.this, i, str);
                }
            }

            @Override
            public void onRegistrationTimeout(ISipSession iSipSession) {
                if (SipSession.this.mListener != null) {
                    SipSession.this.mListener.onRegistrationTimeout(SipSession.this);
                }
            }
        };
    }

    private void loge(String str, Throwable th) {
        Rlog.e(TAG, str, th);
    }
}
