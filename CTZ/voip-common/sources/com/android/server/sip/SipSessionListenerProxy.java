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

    public void setListener(ISipSessionListener iSipSessionListener) {
        this.mListener = iSipSessionListener;
    }

    public ISipSessionListener getListener() {
        return this.mListener;
    }

    private void proxy(Runnable runnable) {
        new Thread(runnable, "SipSessionCallbackThread").start();
    }

    @Override
    public void onCalling(final ISipSession iSipSession) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onCalling(iSipSession);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onCalling()");
                }
            }
        });
    }

    @Override
    public void onRinging(final ISipSession iSipSession, final SipProfile sipProfile, final String str) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onRinging(iSipSession, sipProfile, str);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onRinging()");
                }
            }
        });
    }

    @Override
    public void onRingingBack(final ISipSession iSipSession) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onRingingBack(iSipSession);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onRingingBack()");
                }
            }
        });
    }

    @Override
    public void onCallEstablished(final ISipSession iSipSession, final String str) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onCallEstablished(iSipSession, str);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onCallEstablished()");
                }
            }
        });
    }

    @Override
    public void onCallEnded(final ISipSession iSipSession) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onCallEnded(iSipSession);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onCallEnded()");
                }
            }
        });
    }

    @Override
    public void onCallTransferring(final ISipSession iSipSession, final String str) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onCallTransferring(iSipSession, str);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onCallTransferring()");
                }
            }
        });
    }

    @Override
    public void onCallBusy(final ISipSession iSipSession) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onCallBusy(iSipSession);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onCallBusy()");
                }
            }
        });
    }

    @Override
    public void onCallChangeFailed(final ISipSession iSipSession, final int i, final String str) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onCallChangeFailed(iSipSession, i, str);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onCallChangeFailed()");
                }
            }
        });
    }

    @Override
    public void onError(final ISipSession iSipSession, final int i, final String str) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onError(iSipSession, i, str);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onError()");
                }
            }
        });
    }

    @Override
    public void onRegistering(final ISipSession iSipSession) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onRegistering(iSipSession);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onRegistering()");
                }
            }
        });
    }

    @Override
    public void onRegistrationDone(final ISipSession iSipSession, final int i) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onRegistrationDone(iSipSession, i);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onRegistrationDone()");
                }
            }
        });
    }

    @Override
    public void onRegistrationFailed(final ISipSession iSipSession, final int i, final String str) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onRegistrationFailed(iSipSession, i, str);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onRegistrationFailed()");
                }
            }
        });
    }

    @Override
    public void onRegistrationTimeout(final ISipSession iSipSession) {
        if (this.mListener == null) {
            return;
        }
        proxy(new Runnable() {
            @Override
            public void run() {
                try {
                    SipSessionListenerProxy.this.mListener.onRegistrationTimeout(iSipSession);
                } catch (Throwable th) {
                    SipSessionListenerProxy.this.handle(th, "onRegistrationTimeout()");
                }
            }
        });
    }

    private void handle(Throwable th, String str) {
        if (th instanceof DeadObjectException) {
            this.mListener = null;
        } else if (this.mListener != null) {
            loge(str, th);
        }
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }

    private void loge(String str, Throwable th) {
        Rlog.e(TAG, str, th);
    }
}
