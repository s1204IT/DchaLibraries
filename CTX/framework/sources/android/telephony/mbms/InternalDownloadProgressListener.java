package android.telephony.mbms;

import android.os.Binder;
import android.os.RemoteException;
import android.telephony.mbms.IDownloadProgressListener;
import java.util.concurrent.Executor;

public class InternalDownloadProgressListener extends IDownloadProgressListener.Stub {
    private final DownloadProgressListener mAppListener;
    private final Executor mExecutor;
    private volatile boolean mIsStopped = false;

    public InternalDownloadProgressListener(DownloadProgressListener downloadProgressListener, Executor executor) {
        this.mAppListener = downloadProgressListener;
        this.mExecutor = executor;
    }

    @Override
    public void onProgressUpdated(final DownloadRequest downloadRequest, final FileInfo fileInfo, final int i, final int i2, final int i3, final int i4) throws RemoteException {
        if (this.mIsStopped) {
            return;
        }
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    InternalDownloadProgressListener.this.mAppListener.onProgressUpdated(downloadRequest, fileInfo, i, i2, i3, i4);
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
