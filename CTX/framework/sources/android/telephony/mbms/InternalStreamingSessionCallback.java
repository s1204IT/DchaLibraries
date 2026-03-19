package android.telephony.mbms;

import android.os.Binder;
import android.os.RemoteException;
import android.telephony.mbms.IMbmsStreamingSessionCallback;
import java.util.List;
import java.util.concurrent.Executor;

public class InternalStreamingSessionCallback extends IMbmsStreamingSessionCallback.Stub {
    private final MbmsStreamingSessionCallback mAppCallback;
    private final Executor mExecutor;
    private volatile boolean mIsStopped = false;

    public InternalStreamingSessionCallback(MbmsStreamingSessionCallback mbmsStreamingSessionCallback, Executor executor) {
        this.mAppCallback = mbmsStreamingSessionCallback;
        this.mExecutor = executor;
    }

    @Override
    public void onError(final int i, final String str) throws RemoteException {
        if (this.mIsStopped) {
            return;
        }
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    InternalStreamingSessionCallback.this.mAppCallback.onError(i, str);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        });
    }

    @Override
    public void onStreamingServicesUpdated(final List<StreamingServiceInfo> list) throws RemoteException {
        if (this.mIsStopped) {
            return;
        }
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    InternalStreamingSessionCallback.this.mAppCallback.onStreamingServicesUpdated(list);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        });
    }

    @Override
    public void onMiddlewareReady() throws RemoteException {
        if (this.mIsStopped) {
            return;
        }
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    InternalStreamingSessionCallback.this.mAppCallback.onMiddlewareReady();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        });
    }

    public void stop() {
        this.mIsStopped = true;
    }
}
