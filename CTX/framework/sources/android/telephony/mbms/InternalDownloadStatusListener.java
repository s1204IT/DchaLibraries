package android.telephony.mbms;

import android.os.Binder;
import android.os.RemoteException;
import android.telephony.mbms.IDownloadStatusListener;
import java.util.concurrent.Executor;

public class InternalDownloadStatusListener extends IDownloadStatusListener.Stub {
    private final DownloadStatusListener mAppListener;
    private final Executor mExecutor;
    private volatile boolean mIsStopped = false;

    public InternalDownloadStatusListener(DownloadStatusListener downloadStatusListener, Executor executor) {
        this.mAppListener = downloadStatusListener;
        this.mExecutor = executor;
    }

    @Override
    public void onStatusUpdated(final DownloadRequest downloadRequest, final FileInfo fileInfo, final int i) throws RemoteException {
        if (this.mIsStopped) {
            return;
        }
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    InternalDownloadStatusListener.this.mAppListener.onStatusUpdated(downloadRequest, fileInfo, i);
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
