package android.telephony;

import android.annotation.SystemApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.mbms.InternalStreamingServiceCallback;
import android.telephony.mbms.InternalStreamingSessionCallback;
import android.telephony.mbms.MbmsStreamingSessionCallback;
import android.telephony.mbms.MbmsUtils;
import android.telephony.mbms.StreamingService;
import android.telephony.mbms.StreamingServiceCallback;
import android.telephony.mbms.StreamingServiceInfo;
import android.telephony.mbms.vendor.IMbmsStreamingService;
import android.util.ArraySet;
import android.util.Log;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MbmsStreamingSession implements AutoCloseable {
    private static final String LOG_TAG = "MbmsStreamingSession";

    @SystemApi
    public static final String MBMS_STREAMING_SERVICE_ACTION = "android.telephony.action.EmbmsStreaming";
    public static final String MBMS_STREAMING_SERVICE_OVERRIDE_METADATA = "mbms-streaming-service-override";
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);
    private final Context mContext;
    private InternalStreamingSessionCallback mInternalCallback;
    private int mSubscriptionId;
    private AtomicReference<IMbmsStreamingService> mService = new AtomicReference<>(null);
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            MbmsStreamingSession.sIsInitialized.set(false);
            MbmsStreamingSession.this.sendErrorToApp(3, "Received death notification");
        }
    };
    private Set<StreamingService> mKnownActiveStreamingServices = new ArraySet();

    private MbmsStreamingSession(Context context, Executor executor, int i, MbmsStreamingSessionCallback mbmsStreamingSessionCallback) {
        this.mSubscriptionId = -1;
        this.mContext = context;
        this.mSubscriptionId = i;
        this.mInternalCallback = new InternalStreamingSessionCallback(mbmsStreamingSessionCallback, executor);
    }

    public static MbmsStreamingSession create(Context context, Executor executor, int i, final MbmsStreamingSessionCallback mbmsStreamingSessionCallback) {
        if (!sIsInitialized.compareAndSet(false, true)) {
            throw new IllegalStateException("Cannot create two instances of MbmsStreamingSession");
        }
        MbmsStreamingSession mbmsStreamingSession = new MbmsStreamingSession(context, executor, i, mbmsStreamingSessionCallback);
        final int iBindAndInitialize = mbmsStreamingSession.bindAndInitialize();
        if (iBindAndInitialize != 0) {
            sIsInitialized.set(false);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    mbmsStreamingSessionCallback.onError(iBindAndInitialize, null);
                }
            });
            return null;
        }
        return mbmsStreamingSession;
    }

    public static MbmsStreamingSession create(Context context, Executor executor, MbmsStreamingSessionCallback mbmsStreamingSessionCallback) {
        return create(context, executor, SubscriptionManager.getDefaultSubscriptionId(), mbmsStreamingSessionCallback);
    }

    @Override
    public void close() {
        try {
            IMbmsStreamingService iMbmsStreamingService = this.mService.get();
            if (iMbmsStreamingService != null) {
                iMbmsStreamingService.dispose(this.mSubscriptionId);
                Iterator<StreamingService> it = this.mKnownActiveStreamingServices.iterator();
                while (it.hasNext()) {
                    it.next().getCallback().stop();
                }
                this.mKnownActiveStreamingServices.clear();
            } else {
                this.mService.set(null);
                sIsInitialized.set(false);
                this.mInternalCallback.stop();
                return;
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            this.mService.set(null);
            sIsInitialized.set(false);
            this.mInternalCallback.stop();
            throw th;
        }
        this.mService.set(null);
        sIsInitialized.set(false);
        this.mInternalCallback.stop();
    }

    public void requestUpdateStreamingServices(List<String> list) {
        IMbmsStreamingService iMbmsStreamingService = this.mService.get();
        if (iMbmsStreamingService == null) {
            throw new IllegalStateException("Middleware not yet bound");
        }
        try {
            int iRequestUpdateStreamingServices = iMbmsStreamingService.requestUpdateStreamingServices(this.mSubscriptionId, list);
            if (iRequestUpdateStreamingServices == -1) {
                close();
                throw new IllegalStateException("Middleware must not return an unknown error code");
            }
            if (iRequestUpdateStreamingServices != 0) {
                sendErrorToApp(iRequestUpdateStreamingServices, null);
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Remote process died");
            this.mService.set(null);
            sIsInitialized.set(false);
            sendErrorToApp(3, null);
        }
    }

    public StreamingService startStreaming(StreamingServiceInfo streamingServiceInfo, Executor executor, StreamingServiceCallback streamingServiceCallback) {
        IMbmsStreamingService iMbmsStreamingService = this.mService.get();
        if (iMbmsStreamingService == null) {
            throw new IllegalStateException("Middleware not yet bound");
        }
        InternalStreamingServiceCallback internalStreamingServiceCallback = new InternalStreamingServiceCallback(streamingServiceCallback, executor);
        StreamingService streamingService = new StreamingService(this.mSubscriptionId, iMbmsStreamingService, this, streamingServiceInfo, internalStreamingServiceCallback);
        this.mKnownActiveStreamingServices.add(streamingService);
        try {
            int iStartStreaming = iMbmsStreamingService.startStreaming(this.mSubscriptionId, streamingServiceInfo.getServiceId(), internalStreamingServiceCallback);
            if (iStartStreaming == -1) {
                close();
                throw new IllegalStateException("Middleware must not return an unknown error code");
            }
            if (iStartStreaming != 0) {
                sendErrorToApp(iStartStreaming, null);
                return null;
            }
            return streamingService;
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Remote process died");
            this.mService.set(null);
            sIsInitialized.set(false);
            sendErrorToApp(3, null);
            return null;
        }
    }

    public void onStreamingServiceStopped(StreamingService streamingService) {
        this.mKnownActiveStreamingServices.remove(streamingService);
    }

    private int bindAndInitialize() {
        return MbmsUtils.startBinding(this.mContext, MBMS_STREAMING_SERVICE_ACTION, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                IMbmsStreamingService iMbmsStreamingServiceAsInterface = IMbmsStreamingService.Stub.asInterface(iBinder);
                try {
                    int iInitialize = iMbmsStreamingServiceAsInterface.initialize(MbmsStreamingSession.this.mInternalCallback, MbmsStreamingSession.this.mSubscriptionId);
                    if (iInitialize == -1) {
                        MbmsStreamingSession.this.close();
                        throw new IllegalStateException("Middleware must not return an unknown error code");
                    }
                    if (iInitialize != 0) {
                        MbmsStreamingSession.this.sendErrorToApp(iInitialize, "Error returned during initialization");
                        MbmsStreamingSession.sIsInitialized.set(false);
                        return;
                    }
                    try {
                        iMbmsStreamingServiceAsInterface.asBinder().linkToDeath(MbmsStreamingSession.this.mDeathRecipient, 0);
                        MbmsStreamingSession.this.mService.set(iMbmsStreamingServiceAsInterface);
                    } catch (RemoteException e) {
                        MbmsStreamingSession.this.sendErrorToApp(3, "Middleware lost during initialization");
                        MbmsStreamingSession.sIsInitialized.set(false);
                    }
                } catch (RemoteException e2) {
                    Log.e(MbmsStreamingSession.LOG_TAG, "Service died before initialization");
                    MbmsStreamingSession.this.sendErrorToApp(103, e2.toString());
                    MbmsStreamingSession.sIsInitialized.set(false);
                } catch (RuntimeException e3) {
                    Log.e(MbmsStreamingSession.LOG_TAG, "Runtime exception during initialization");
                    MbmsStreamingSession.this.sendErrorToApp(103, e3.toString());
                    MbmsStreamingSession.sIsInitialized.set(false);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                MbmsStreamingSession.sIsInitialized.set(false);
                MbmsStreamingSession.this.mService.set(null);
            }
        });
    }

    private void sendErrorToApp(int i, String str) {
        try {
            this.mInternalCallback.onError(i, str);
        } catch (RemoteException e) {
        }
    }
}
