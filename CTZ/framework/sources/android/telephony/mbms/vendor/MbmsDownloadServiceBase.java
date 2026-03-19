package android.telephony.mbms.vendor;

import android.annotation.SystemApi;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.mbms.DownloadProgressListener;
import android.telephony.mbms.DownloadRequest;
import android.telephony.mbms.DownloadStatusListener;
import android.telephony.mbms.FileInfo;
import android.telephony.mbms.FileServiceInfo;
import android.telephony.mbms.IDownloadProgressListener;
import android.telephony.mbms.IDownloadStatusListener;
import android.telephony.mbms.IMbmsDownloadSessionCallback;
import android.telephony.mbms.MbmsDownloadSessionCallback;
import android.telephony.mbms.vendor.IMbmsDownloadService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SystemApi
public class MbmsDownloadServiceBase extends IMbmsDownloadService.Stub {
    private final Map<IBinder, DownloadStatusListener> mDownloadStatusListenerBinderMap = new HashMap();
    private final Map<IBinder, DownloadProgressListener> mDownloadProgressListenerBinderMap = new HashMap();
    private final Map<IBinder, IBinder.DeathRecipient> mDownloadCallbackDeathRecipients = new HashMap();

    private static abstract class VendorDownloadStatusListener extends DownloadStatusListener {
        private final IDownloadStatusListener mListener;

        protected abstract void onRemoteException(RemoteException remoteException);

        public VendorDownloadStatusListener(IDownloadStatusListener iDownloadStatusListener) {
            this.mListener = iDownloadStatusListener;
        }

        @Override
        public void onStatusUpdated(DownloadRequest downloadRequest, FileInfo fileInfo, int i) {
            try {
                this.mListener.onStatusUpdated(downloadRequest, fileInfo, i);
            } catch (RemoteException e) {
                onRemoteException(e);
            }
        }
    }

    private static abstract class VendorDownloadProgressListener extends DownloadProgressListener {
        private final IDownloadProgressListener mListener;

        protected abstract void onRemoteException(RemoteException remoteException);

        public VendorDownloadProgressListener(IDownloadProgressListener iDownloadProgressListener) {
            this.mListener = iDownloadProgressListener;
        }

        @Override
        public void onProgressUpdated(DownloadRequest downloadRequest, FileInfo fileInfo, int i, int i2, int i3, int i4) {
            try {
                this.mListener.onProgressUpdated(downloadRequest, fileInfo, i, i2, i3, i4);
            } catch (RemoteException e) {
                onRemoteException(e);
            }
        }
    }

    public int initialize(int i, MbmsDownloadSessionCallback mbmsDownloadSessionCallback) throws RemoteException {
        return 0;
    }

    @Override
    public final int initialize(final int i, final IMbmsDownloadSessionCallback iMbmsDownloadSessionCallback) throws RemoteException {
        if (iMbmsDownloadSessionCallback == null) {
            throw new NullPointerException("Callback must not be null");
        }
        final int callingUid = Binder.getCallingUid();
        int iInitialize = initialize(i, new MbmsDownloadSessionCallback() {
            @Override
            public void onError(int i2, String str) {
                try {
                    if (i2 == -1) {
                        throw new IllegalArgumentException("Middleware cannot send an unknown error.");
                    }
                    iMbmsDownloadSessionCallback.onError(i2, str);
                } catch (RemoteException e) {
                    MbmsDownloadServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }

            @Override
            public void onFileServicesUpdated(List<FileServiceInfo> list) {
                try {
                    iMbmsDownloadSessionCallback.onFileServicesUpdated(list);
                } catch (RemoteException e) {
                    MbmsDownloadServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }

            @Override
            public void onMiddlewareReady() {
                try {
                    iMbmsDownloadSessionCallback.onMiddlewareReady();
                } catch (RemoteException e) {
                    MbmsDownloadServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }
        });
        if (iInitialize == 0) {
            iMbmsDownloadSessionCallback.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    MbmsDownloadServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }, 0);
        }
        return iInitialize;
    }

    @Override
    public int requestUpdateFileServices(int i, List<String> list) throws RemoteException {
        return 0;
    }

    @Override
    public int setTempFileRootDirectory(int i, String str) throws RemoteException {
        return 0;
    }

    @Override
    public int download(DownloadRequest downloadRequest) throws RemoteException {
        return 0;
    }

    public int addStatusListener(DownloadRequest downloadRequest, DownloadStatusListener downloadStatusListener) throws RemoteException {
        return 0;
    }

    @Override
    public final int addStatusListener(final DownloadRequest downloadRequest, final IDownloadStatusListener iDownloadStatusListener) throws RemoteException {
        final int callingUid = Binder.getCallingUid();
        if (downloadRequest == null) {
            throw new NullPointerException("Download request must not be null");
        }
        if (iDownloadStatusListener == null) {
            throw new NullPointerException("Callback must not be null");
        }
        VendorDownloadStatusListener vendorDownloadStatusListener = new VendorDownloadStatusListener(iDownloadStatusListener) {
            @Override
            protected void onRemoteException(RemoteException remoteException) {
                MbmsDownloadServiceBase.this.onAppCallbackDied(callingUid, downloadRequest.getSubscriptionId());
            }
        };
        int iAddStatusListener = addStatusListener(downloadRequest, vendorDownloadStatusListener);
        if (iAddStatusListener == 0) {
            IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    MbmsDownloadServiceBase.this.onAppCallbackDied(callingUid, downloadRequest.getSubscriptionId());
                    MbmsDownloadServiceBase.this.mDownloadStatusListenerBinderMap.remove(iDownloadStatusListener.asBinder());
                    MbmsDownloadServiceBase.this.mDownloadCallbackDeathRecipients.remove(iDownloadStatusListener.asBinder());
                }
            };
            this.mDownloadCallbackDeathRecipients.put(iDownloadStatusListener.asBinder(), deathRecipient);
            iDownloadStatusListener.asBinder().linkToDeath(deathRecipient, 0);
            this.mDownloadStatusListenerBinderMap.put(iDownloadStatusListener.asBinder(), vendorDownloadStatusListener);
        }
        return iAddStatusListener;
    }

    public int removeStatusListener(DownloadRequest downloadRequest, DownloadStatusListener downloadStatusListener) throws RemoteException {
        return 0;
    }

    @Override
    public final int removeStatusListener(DownloadRequest downloadRequest, IDownloadStatusListener iDownloadStatusListener) throws RemoteException {
        if (downloadRequest == null) {
            throw new NullPointerException("Download request must not be null");
        }
        if (iDownloadStatusListener == null) {
            throw new NullPointerException("Callback must not be null");
        }
        IBinder.DeathRecipient deathRecipientRemove = this.mDownloadCallbackDeathRecipients.remove(iDownloadStatusListener.asBinder());
        if (deathRecipientRemove == null) {
            throw new IllegalArgumentException("Unknown listener");
        }
        iDownloadStatusListener.asBinder().unlinkToDeath(deathRecipientRemove, 0);
        DownloadStatusListener downloadStatusListenerRemove = this.mDownloadStatusListenerBinderMap.remove(iDownloadStatusListener.asBinder());
        if (downloadStatusListenerRemove == null) {
            throw new IllegalArgumentException("Unknown listener");
        }
        return removeStatusListener(downloadRequest, downloadStatusListenerRemove);
    }

    public int addProgressListener(DownloadRequest downloadRequest, DownloadProgressListener downloadProgressListener) throws RemoteException {
        return 0;
    }

    @Override
    public final int addProgressListener(final DownloadRequest downloadRequest, final IDownloadProgressListener iDownloadProgressListener) throws RemoteException {
        final int callingUid = Binder.getCallingUid();
        if (downloadRequest == null) {
            throw new NullPointerException("Download request must not be null");
        }
        if (iDownloadProgressListener == null) {
            throw new NullPointerException("Callback must not be null");
        }
        VendorDownloadProgressListener vendorDownloadProgressListener = new VendorDownloadProgressListener(iDownloadProgressListener) {
            @Override
            protected void onRemoteException(RemoteException remoteException) {
                MbmsDownloadServiceBase.this.onAppCallbackDied(callingUid, downloadRequest.getSubscriptionId());
            }
        };
        int iAddProgressListener = addProgressListener(downloadRequest, vendorDownloadProgressListener);
        if (iAddProgressListener == 0) {
            IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    MbmsDownloadServiceBase.this.onAppCallbackDied(callingUid, downloadRequest.getSubscriptionId());
                    MbmsDownloadServiceBase.this.mDownloadProgressListenerBinderMap.remove(iDownloadProgressListener.asBinder());
                    MbmsDownloadServiceBase.this.mDownloadCallbackDeathRecipients.remove(iDownloadProgressListener.asBinder());
                }
            };
            this.mDownloadCallbackDeathRecipients.put(iDownloadProgressListener.asBinder(), deathRecipient);
            iDownloadProgressListener.asBinder().linkToDeath(deathRecipient, 0);
            this.mDownloadProgressListenerBinderMap.put(iDownloadProgressListener.asBinder(), vendorDownloadProgressListener);
        }
        return iAddProgressListener;
    }

    public int removeProgressListener(DownloadRequest downloadRequest, DownloadProgressListener downloadProgressListener) throws RemoteException {
        return 0;
    }

    @Override
    public final int removeProgressListener(DownloadRequest downloadRequest, IDownloadProgressListener iDownloadProgressListener) throws RemoteException {
        if (downloadRequest == null) {
            throw new NullPointerException("Download request must not be null");
        }
        if (iDownloadProgressListener == null) {
            throw new NullPointerException("Callback must not be null");
        }
        IBinder.DeathRecipient deathRecipientRemove = this.mDownloadCallbackDeathRecipients.remove(iDownloadProgressListener.asBinder());
        if (deathRecipientRemove == null) {
            throw new IllegalArgumentException("Unknown listener");
        }
        iDownloadProgressListener.asBinder().unlinkToDeath(deathRecipientRemove, 0);
        DownloadProgressListener downloadProgressListenerRemove = this.mDownloadProgressListenerBinderMap.remove(iDownloadProgressListener.asBinder());
        if (downloadProgressListenerRemove == null) {
            throw new IllegalArgumentException("Unknown listener");
        }
        return removeProgressListener(downloadRequest, downloadProgressListenerRemove);
    }

    @Override
    public List<DownloadRequest> listPendingDownloads(int i) throws RemoteException {
        return null;
    }

    @Override
    public int cancelDownload(DownloadRequest downloadRequest) throws RemoteException {
        return 0;
    }

    @Override
    public int requestDownloadState(DownloadRequest downloadRequest, FileInfo fileInfo) throws RemoteException {
        return 0;
    }

    @Override
    public int resetDownloadKnowledge(DownloadRequest downloadRequest) throws RemoteException {
        return 0;
    }

    @Override
    public void dispose(int i) throws RemoteException {
    }

    public void onAppCallbackDied(int i, int i2) {
    }
}
