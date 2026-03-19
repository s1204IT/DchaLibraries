package android.telephony.mbms;

import android.os.Binder;
import android.telephony.mbms.IMbmsDownloadSessionCallback;
import java.util.List;
import java.util.concurrent.Executor;

public class InternalDownloadSessionCallback extends IMbmsDownloadSessionCallback.Stub {
    private final MbmsDownloadSessionCallback mAppCallback;
    private final Executor mExecutor;
    private volatile boolean mIsStopped = false;

    public InternalDownloadSessionCallback(MbmsDownloadSessionCallback mbmsDownloadSessionCallback, Executor executor) {
        this.mAppCallback = mbmsDownloadSessionCallback;
        this.mExecutor = executor;
    }

    @Override
    public void onError(final int i, final String str) {
        if (this.mIsStopped) {
            return;
        }
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    InternalDownloadSessionCallback.this.mAppCallback.onError(i, str);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        });
    }

    @Override
    public void onFileServicesUpdated(final List<FileServiceInfo> list) {
        if (this.mIsStopped) {
            return;
        }
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    InternalDownloadSessionCallback.this.mAppCallback.onFileServicesUpdated(list);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        });
    }

    @Override
    public void onMiddlewareReady() {
        if (this.mIsStopped) {
            return;
        }
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    InternalDownloadSessionCallback.this.mAppCallback.onMiddlewareReady();
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
