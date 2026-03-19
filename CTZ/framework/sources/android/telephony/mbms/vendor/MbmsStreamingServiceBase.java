package android.telephony.mbms.vendor;

import android.annotation.SystemApi;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.mbms.IMbmsStreamingSessionCallback;
import android.telephony.mbms.IStreamingServiceCallback;
import android.telephony.mbms.MbmsStreamingSessionCallback;
import android.telephony.mbms.StreamingServiceCallback;
import android.telephony.mbms.StreamingServiceInfo;
import android.telephony.mbms.vendor.IMbmsStreamingService;
import java.util.List;

@SystemApi
public class MbmsStreamingServiceBase extends IMbmsStreamingService.Stub {
    public int initialize(MbmsStreamingSessionCallback mbmsStreamingSessionCallback, int i) throws RemoteException {
        return 0;
    }

    @Override
    public final int initialize(final IMbmsStreamingSessionCallback iMbmsStreamingSessionCallback, final int i) throws RemoteException {
        if (iMbmsStreamingSessionCallback == null) {
            throw new NullPointerException("Callback must not be null");
        }
        final int callingUid = Binder.getCallingUid();
        int iInitialize = initialize(new MbmsStreamingSessionCallback() {
            @Override
            public void onError(int i2, String str) {
                try {
                    if (i2 == -1) {
                        throw new IllegalArgumentException("Middleware cannot send an unknown error.");
                    }
                    iMbmsStreamingSessionCallback.onError(i2, str);
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }

            @Override
            public void onStreamingServicesUpdated(List<StreamingServiceInfo> list) {
                try {
                    iMbmsStreamingSessionCallback.onStreamingServicesUpdated(list);
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }

            @Override
            public void onMiddlewareReady() {
                try {
                    iMbmsStreamingSessionCallback.onMiddlewareReady();
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }
        }, i);
        if (iInitialize == 0) {
            iMbmsStreamingSessionCallback.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }, 0);
        }
        return iInitialize;
    }

    @Override
    public int requestUpdateStreamingServices(int i, List<String> list) throws RemoteException {
        return 0;
    }

    public int startStreaming(int i, String str, StreamingServiceCallback streamingServiceCallback) throws RemoteException {
        return 0;
    }

    @Override
    public int startStreaming(final int i, String str, final IStreamingServiceCallback iStreamingServiceCallback) throws RemoteException {
        if (iStreamingServiceCallback == null) {
            throw new NullPointerException("Callback must not be null");
        }
        final int callingUid = Binder.getCallingUid();
        int iStartStreaming = startStreaming(i, str, new StreamingServiceCallback() {
            @Override
            public void onError(int i2, String str2) {
                try {
                    if (i2 == -1) {
                        throw new IllegalArgumentException("Middleware cannot send an unknown error.");
                    }
                    iStreamingServiceCallback.onError(i2, str2);
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }

            @Override
            public void onStreamStateUpdated(int i2, int i3) {
                try {
                    iStreamingServiceCallback.onStreamStateUpdated(i2, i3);
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }

            @Override
            public void onMediaDescriptionUpdated() {
                try {
                    iStreamingServiceCallback.onMediaDescriptionUpdated();
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }

            @Override
            public void onBroadcastSignalStrengthUpdated(int i2) {
                try {
                    iStreamingServiceCallback.onBroadcastSignalStrengthUpdated(i2);
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }

            @Override
            public void onStreamMethodUpdated(int i2) {
                try {
                    iStreamingServiceCallback.onStreamMethodUpdated(i2);
                } catch (RemoteException e) {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }
        });
        if (iStartStreaming == 0) {
            iStreamingServiceCallback.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    MbmsStreamingServiceBase.this.onAppCallbackDied(callingUid, i);
                }
            }, 0);
        }
        return iStartStreaming;
    }

    @Override
    public Uri getPlaybackUri(int i, String str) throws RemoteException {
        return null;
    }

    @Override
    public void stopStreaming(int i, String str) throws RemoteException {
    }

    @Override
    public void dispose(int i) throws RemoteException {
    }

    public void onAppCallbackDied(int i, int i2) {
    }
}
