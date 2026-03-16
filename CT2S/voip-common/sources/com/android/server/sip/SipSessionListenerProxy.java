package com.android.server.sip;

import android.net.sip.ISipSession;
import android.net.sip.ISipSessionListener;
import android.net.sip.SipProfile;
import android.os.DeadObjectException;
import android.telephony.Rlog;

class SipSessionListenerProxy extends ISipSessionListener.Stub {
    private static final String TAG = "SipSessionListnerProxy";
    private ISipSessionListener mListener;

    SipSessionListenerProxy() {
    }

    public void setListener(ISipSessionListener listener) {
        this.mListener = listener;
    }

    public ISipSessionListener getListener() {
        return this.mListener;
    }

    private void proxy(Runnable runnable) {
        new Thread(runnable, "SipSessionCallbackThread").start();
    }

    @Override
    public void onCalling(final ISipSession session) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onCalling(session);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onCalling()");
                    }
                }
            });
        }
    }

    @Override
    public void onRinging(final ISipSession session, final SipProfile caller, final String sessionDescription) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onRinging(session, caller, sessionDescription);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onRinging()");
                    }
                }
            });
        }
    }

    @Override
    public void onRingingBack(final ISipSession session) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onRingingBack(session);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onRingingBack()");
                    }
                }
            });
        }
    }

    @Override
    public void onCallEstablished(final ISipSession session, final String sessionDescription) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onCallEstablished(session, sessionDescription);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onCallEstablished()");
                    }
                }
            });
        }
    }

    @Override
    public void onCallEnded(final ISipSession session) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onCallEnded(session);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onCallEnded()");
                    }
                }
            });
        }
    }

    @Override
    public void onCallTransferring(final ISipSession newSession, final String sessionDescription) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onCallTransferring(newSession, sessionDescription);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onCallTransferring()");
                    }
                }
            });
        }
    }

    @Override
    public void onCallBusy(final ISipSession session) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onCallBusy(session);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onCallBusy()");
                    }
                }
            });
        }
    }

    @Override
    public void onCallChangeFailed(final ISipSession session, final int errorCode, final String message) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onCallChangeFailed(session, errorCode, message);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onCallChangeFailed()");
                    }
                }
            });
        }
    }

    @Override
    public void onError(final ISipSession session, final int errorCode, final String message) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onError(session, errorCode, message);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onError()");
                    }
                }
            });
        }
    }

    @Override
    public void onRegistering(final ISipSession session) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onRegistering(session);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onRegistering()");
                    }
                }
            });
        }
    }

    @Override
    public void onRegistrationDone(final ISipSession session, final int duration) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onRegistrationDone(session, duration);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onRegistrationDone()");
                    }
                }
            });
        }
    }

    @Override
    public void onRegistrationFailed(final ISipSession session, final int errorCode, final String message) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onRegistrationFailed(session, errorCode, message);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onRegistrationFailed()");
                    }
                }
            });
        }
    }

    @Override
    public void onRegistrationTimeout(final ISipSession session) {
        if (this.mListener != null) {
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionListenerProxy.this.mListener.onRegistrationTimeout(session);
                    } catch (Throwable t) {
                        SipSessionListenerProxy.this.handle(t, "onRegistrationTimeout()");
                    }
                }
            });
        }
    }

    private void handle(Throwable t, String message) {
        if (t instanceof DeadObjectException) {
            this.mListener = null;
        } else if (this.mListener != null) {
            loge(message, t);
        }
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s, Throwable t) {
        Rlog.e(TAG, s, t);
    }
}
