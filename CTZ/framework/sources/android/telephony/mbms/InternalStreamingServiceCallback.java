package android.telephony.mbms;

import android.os.Binder;
import android.os.RemoteException;
import android.telephony.mbms.IStreamingServiceCallback;
import java.util.concurrent.Executor;

public class InternalStreamingServiceCallback extends IStreamingServiceCallback.Stub {
    private final StreamingServiceCallback mAppCallback;
    private final Executor mExecutor;
    private volatile boolean mIsStopped = false;

    public InternalStreamingServiceCallback(StreamingServiceCallback streamingServiceCallback, Executor executor) {
        this.mAppCallback = streamingServiceCallback;
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
                    InternalStreamingServiceCallback.this.mAppCallback.onError(i, str);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        });
    }

    @Override
    public void onStreamStateUpdated(final int i, final int i2) throws RemoteException {
        if (this.mIsStopped) {
            return;
        }
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    InternalStreamingServiceCallback.this.mAppCallback.onStreamStateUpdated(i, i2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        });
    }

    @Override
    public void onMediaDescriptionUpdated() throws RemoteException {
        if (this.mIsStopped) {
            return;
        }
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    InternalStreamingServiceCallback.this.mAppCallback.onMediaDescriptionUpdated();
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        });
    }

    @Override
    public void onBroadcastSignalStrengthUpdated(final int i) throws RemoteException {
        if (this.mIsStopped) {
            return;
        }
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    InternalStreamingServiceCallback.this.mAppCallback.onBroadcastSignalStrengthUpdated(i);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        });
    }

    @Override
    public void onStreamMethodUpdated(final int i) throws RemoteException {
        if (this.mIsStopped) {
            return;
        }
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    InternalStreamingServiceCallback.this.mAppCallback.onStreamMethodUpdated(i);
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
