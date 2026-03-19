package android.media.soundtrigger;

import android.annotation.SystemApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.soundtrigger.SoundTrigger;
import android.media.soundtrigger.ISoundTriggerDetectionService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import java.util.UUID;

@SystemApi
public abstract class SoundTriggerDetectionService extends Service {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = SoundTriggerDetectionService.class.getSimpleName();
    private Handler mHandler;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final ArrayMap<UUID, ISoundTriggerDetectionServiceClient> mClients = new ArrayMap<>();

    public abstract void onStopOperation(UUID uuid, Bundle bundle, int i);

    @Override
    protected final void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        this.mHandler = new Handler(context.getMainLooper());
    }

    private void setClient(UUID uuid, Bundle bundle, ISoundTriggerDetectionServiceClient iSoundTriggerDetectionServiceClient) {
        synchronized (this.mLock) {
            this.mClients.put(uuid, iSoundTriggerDetectionServiceClient);
        }
        onConnected(uuid, bundle);
    }

    private void removeClient(UUID uuid, Bundle bundle) {
        synchronized (this.mLock) {
            this.mClients.remove(uuid);
        }
        onDisconnected(uuid, bundle);
    }

    public void onConnected(UUID uuid, Bundle bundle) {
    }

    public void onDisconnected(UUID uuid, Bundle bundle) {
    }

    public void onGenericRecognitionEvent(UUID uuid, Bundle bundle, int i, SoundTrigger.RecognitionEvent recognitionEvent) {
        operationFinished(uuid, i);
    }

    public void onError(UUID uuid, Bundle bundle, int i, int i2) {
        operationFinished(uuid, i);
    }

    public final void operationFinished(UUID uuid, int i) {
        try {
            synchronized (this.mLock) {
                ISoundTriggerDetectionServiceClient iSoundTriggerDetectionServiceClient = this.mClients.get(uuid);
                if (iSoundTriggerDetectionServiceClient == null) {
                    Log.w(LOG_TAG, "operationFinished called, but no client for " + uuid + ". Was this called after onDisconnected?");
                    return;
                }
                iSoundTriggerDetectionServiceClient.onOpFinished(i);
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "operationFinished, remote exception for client " + uuid, e);
        }
    }

    class AnonymousClass1 extends ISoundTriggerDetectionService.Stub {
        private final Object mBinderLock = new Object();

        @GuardedBy("mBinderLock")
        public final ArrayMap<UUID, Bundle> mParams = new ArrayMap<>();

        AnonymousClass1() {
        }

        @Override
        public void setClient(ParcelUuid parcelUuid, Bundle bundle, ISoundTriggerDetectionServiceClient iSoundTriggerDetectionServiceClient) {
            UUID uuid = parcelUuid.getUuid();
            synchronized (this.mBinderLock) {
                this.mParams.put(uuid, bundle);
            }
            SoundTriggerDetectionService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuadConsumer() {
                @Override
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                    ((SoundTriggerDetectionService) obj).setClient((UUID) obj2, (Bundle) obj3, (ISoundTriggerDetectionServiceClient) obj4);
                }
            }, SoundTriggerDetectionService.this, uuid, bundle, iSoundTriggerDetectionServiceClient));
        }

        @Override
        public void removeClient(ParcelUuid parcelUuid) {
            Bundle bundleRemove;
            UUID uuid = parcelUuid.getUuid();
            synchronized (this.mBinderLock) {
                bundleRemove = this.mParams.remove(uuid);
            }
            SoundTriggerDetectionService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() {
                @Override
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((SoundTriggerDetectionService) obj).removeClient((UUID) obj2, (Bundle) obj3);
                }
            }, SoundTriggerDetectionService.this, uuid, bundleRemove));
        }

        @Override
        public void onGenericRecognitionEvent(ParcelUuid parcelUuid, int i, SoundTrigger.GenericRecognitionEvent genericRecognitionEvent) {
            Bundle bundle;
            UUID uuid = parcelUuid.getUuid();
            synchronized (this.mBinderLock) {
                bundle = this.mParams.get(uuid);
            }
            SoundTriggerDetectionService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuintConsumer() {
                @Override
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                    ((SoundTriggerDetectionService) obj).onGenericRecognitionEvent((UUID) obj2, (Bundle) obj3, ((Integer) obj4).intValue(), (SoundTrigger.GenericRecognitionEvent) obj5);
                }
            }, SoundTriggerDetectionService.this, uuid, bundle, Integer.valueOf(i), genericRecognitionEvent));
        }

        @Override
        public void onError(ParcelUuid parcelUuid, int i, int i2) {
            Bundle bundle;
            UUID uuid = parcelUuid.getUuid();
            synchronized (this.mBinderLock) {
                bundle = this.mParams.get(uuid);
            }
            SoundTriggerDetectionService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuintConsumer() {
                @Override
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                    ((SoundTriggerDetectionService) obj).onError((UUID) obj2, (Bundle) obj3, ((Integer) obj4).intValue(), ((Integer) obj5).intValue());
                }
            }, SoundTriggerDetectionService.this, uuid, bundle, Integer.valueOf(i), Integer.valueOf(i2)));
        }

        @Override
        public void onStopOperation(ParcelUuid parcelUuid, int i) {
            Bundle bundle;
            UUID uuid = parcelUuid.getUuid();
            synchronized (this.mBinderLock) {
                bundle = this.mParams.get(uuid);
            }
            SoundTriggerDetectionService.this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuadConsumer() {
                @Override
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                    ((SoundTriggerDetectionService) obj).onStopOperation((UUID) obj2, (Bundle) obj3, ((Integer) obj4).intValue());
                }
            }, SoundTriggerDetectionService.this, uuid, bundle, Integer.valueOf(i)));
        }
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return new AnonymousClass1();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        this.mClients.clear();
        return false;
    }
}
