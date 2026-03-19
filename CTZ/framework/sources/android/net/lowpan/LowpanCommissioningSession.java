package android.net.lowpan;

import android.net.IpPrefix;
import android.net.lowpan.ILowpanInterfaceListener;
import android.net.lowpan.LowpanCommissioningSession;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

public class LowpanCommissioningSession {
    private final LowpanBeaconInfo mBeaconInfo;
    private final ILowpanInterface mBinder;
    private Handler mHandler;
    private final Looper mLooper;
    private final ILowpanInterfaceListener mInternalCallback = new InternalCallback();
    private Callback mCallback = null;
    private volatile boolean mIsClosed = false;

    public static abstract class Callback {
        public void onReceiveFromCommissioner(byte[] bArr) {
        }

        public void onClosed() {
        }
    }

    private class InternalCallback extends ILowpanInterfaceListener.Stub {
        private InternalCallback() {
        }

        @Override
        public void onStateChanged(String str) {
            if (!LowpanCommissioningSession.this.mIsClosed) {
                byte b = -1;
                int iHashCode = str.hashCode();
                if (iHashCode != -1548612125) {
                    if (iHashCode == 97204770 && str.equals("fault")) {
                        b = 1;
                    }
                } else if (str.equals("offline")) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                    case 1:
                        synchronized (LowpanCommissioningSession.this) {
                            LowpanCommissioningSession.this.lockedCleanup();
                            break;
                        }
                        return;
                    default:
                        return;
                }
            }
        }

        @Override
        public void onReceiveFromCommissioner(final byte[] bArr) {
            LowpanCommissioningSession.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    LowpanCommissioningSession.InternalCallback.lambda$onReceiveFromCommissioner$0(this.f$0, bArr);
                }
            });
        }

        public static void lambda$onReceiveFromCommissioner$0(InternalCallback internalCallback, byte[] bArr) {
            synchronized (LowpanCommissioningSession.this) {
                if (!LowpanCommissioningSession.this.mIsClosed && LowpanCommissioningSession.this.mCallback != null) {
                    LowpanCommissioningSession.this.mCallback.onReceiveFromCommissioner(bArr);
                }
            }
        }

        @Override
        public void onEnabledChanged(boolean z) {
        }

        @Override
        public void onConnectedChanged(boolean z) {
        }

        @Override
        public void onUpChanged(boolean z) {
        }

        @Override
        public void onRoleChanged(String str) {
        }

        @Override
        public void onLowpanIdentityChanged(LowpanIdentity lowpanIdentity) {
        }

        @Override
        public void onLinkNetworkAdded(IpPrefix ipPrefix) {
        }

        @Override
        public void onLinkNetworkRemoved(IpPrefix ipPrefix) {
        }

        @Override
        public void onLinkAddressAdded(String str) {
        }

        @Override
        public void onLinkAddressRemoved(String str) {
        }
    }

    LowpanCommissioningSession(ILowpanInterface iLowpanInterface, LowpanBeaconInfo lowpanBeaconInfo, Looper looper) {
        this.mBinder = iLowpanInterface;
        this.mBeaconInfo = lowpanBeaconInfo;
        this.mLooper = looper;
        if (this.mLooper != null) {
            this.mHandler = new Handler(this.mLooper);
        } else {
            this.mHandler = new Handler();
        }
        try {
            this.mBinder.addListener(this.mInternalCallback);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    private void lockedCleanup() {
        if (!this.mIsClosed) {
            try {
                this.mBinder.removeListener(this.mInternalCallback);
            } catch (DeadObjectException e) {
            } catch (RemoteException e2) {
                throw e2.rethrowAsRuntimeException();
            }
            if (this.mCallback != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mCallback.onClosed();
                    }
                });
            }
        }
        this.mCallback = null;
        this.mIsClosed = true;
    }

    public LowpanBeaconInfo getBeaconInfo() {
        return this.mBeaconInfo;
    }

    public void sendToCommissioner(byte[] bArr) {
        if (!this.mIsClosed) {
            try {
                this.mBinder.sendToCommissioner(bArr);
            } catch (DeadObjectException e) {
            } catch (RemoteException e2) {
                throw e2.rethrowAsRuntimeException();
            }
        }
    }

    public synchronized void setCallback(Callback callback, Handler handler) {
        if (!this.mIsClosed) {
            if (handler != null) {
                this.mHandler = handler;
            } else if (this.mLooper != null) {
                this.mHandler = new Handler(this.mLooper);
            } else {
                this.mHandler = new Handler();
            }
            this.mCallback = callback;
        }
    }

    public synchronized void close() {
        if (!this.mIsClosed) {
            try {
                this.mBinder.closeCommissioningSession();
                lockedCleanup();
            } catch (DeadObjectException e) {
            } catch (RemoteException e2) {
                throw e2.rethrowAsRuntimeException();
            }
        }
    }
}
