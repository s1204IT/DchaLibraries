package android.media.soundtrigger;

import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.soundtrigger.SoundTrigger;
import android.media.soundtrigger.SoundTriggerDetector;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.app.ISoundTriggerService;
import com.android.internal.util.Preconditions;
import java.util.HashMap;
import java.util.UUID;

@SystemApi
public final class SoundTriggerManager {
    private static final boolean DBG = false;
    public static final String EXTRA_MESSAGE_TYPE = "android.media.soundtrigger.MESSAGE_TYPE";
    public static final String EXTRA_RECOGNITION_EVENT = "android.media.soundtrigger.RECOGNITION_EVENT";
    public static final String EXTRA_STATUS = "android.media.soundtrigger.STATUS";
    public static final int FLAG_MESSAGE_TYPE_RECOGNITION_ERROR = 1;
    public static final int FLAG_MESSAGE_TYPE_RECOGNITION_EVENT = 0;
    public static final int FLAG_MESSAGE_TYPE_RECOGNITION_PAUSED = 2;
    public static final int FLAG_MESSAGE_TYPE_RECOGNITION_RESUMED = 3;
    public static final int FLAG_MESSAGE_TYPE_UNKNOWN = -1;
    private static final String TAG = "SoundTriggerManager";
    private final Context mContext;
    private final HashMap<UUID, SoundTriggerDetector> mReceiverInstanceMap = new HashMap<>();
    private final ISoundTriggerService mSoundTriggerService;

    public SoundTriggerManager(Context context, ISoundTriggerService iSoundTriggerService) {
        this.mSoundTriggerService = iSoundTriggerService;
        this.mContext = context;
    }

    public void updateModel(Model model) {
        try {
            this.mSoundTriggerService.updateSoundModel(model.getGenericSoundModel());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Model getModel(UUID uuid) {
        try {
            return new Model(this.mSoundTriggerService.getSoundModel(new ParcelUuid(uuid)));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void deleteModel(UUID uuid) {
        try {
            this.mSoundTriggerService.deleteSoundModel(new ParcelUuid(uuid));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public SoundTriggerDetector createSoundTriggerDetector(UUID uuid, SoundTriggerDetector.Callback callback, Handler handler) {
        if (uuid == null) {
            return null;
        }
        this.mReceiverInstanceMap.get(uuid);
        SoundTriggerDetector soundTriggerDetector = new SoundTriggerDetector(this.mSoundTriggerService, uuid, callback, handler);
        this.mReceiverInstanceMap.put(uuid, soundTriggerDetector);
        return soundTriggerDetector;
    }

    public static class Model {
        private SoundTrigger.GenericSoundModel mGenericSoundModel;

        Model(SoundTrigger.GenericSoundModel genericSoundModel) {
            this.mGenericSoundModel = genericSoundModel;
        }

        public static Model create(UUID uuid, UUID uuid2, byte[] bArr) {
            return new Model(new SoundTrigger.GenericSoundModel(uuid, uuid2, bArr));
        }

        public UUID getModelUuid() {
            return this.mGenericSoundModel.uuid;
        }

        public UUID getVendorUuid() {
            return this.mGenericSoundModel.vendorUuid;
        }

        public byte[] getModelData() {
            return this.mGenericSoundModel.data;
        }

        SoundTrigger.GenericSoundModel getGenericSoundModel() {
            return this.mGenericSoundModel;
        }
    }

    public int loadSoundModel(SoundTrigger.SoundModel soundModel) {
        if (soundModel == null) {
            return Integer.MIN_VALUE;
        }
        try {
            switch (soundModel.type) {
                case 0:
                    return this.mSoundTriggerService.loadKeyphraseSoundModel((SoundTrigger.KeyphraseSoundModel) soundModel);
                case 1:
                    return this.mSoundTriggerService.loadGenericSoundModel((SoundTrigger.GenericSoundModel) soundModel);
                default:
                    Slog.e(TAG, "Unkown model type");
                    return Integer.MIN_VALUE;
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int startRecognition(UUID uuid, PendingIntent pendingIntent, SoundTrigger.RecognitionConfig recognitionConfig) {
        if (uuid == null || pendingIntent == null || recognitionConfig == null) {
            return Integer.MIN_VALUE;
        }
        try {
            return this.mSoundTriggerService.startRecognitionForIntent(new ParcelUuid(uuid), pendingIntent, recognitionConfig);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int startRecognition(UUID uuid, Bundle bundle, ComponentName componentName, SoundTrigger.RecognitionConfig recognitionConfig) {
        Preconditions.checkNotNull(uuid);
        Preconditions.checkNotNull(componentName);
        Preconditions.checkNotNull(recognitionConfig);
        try {
            return this.mSoundTriggerService.startRecognitionForService(new ParcelUuid(uuid), bundle, componentName, recognitionConfig);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int stopRecognition(UUID uuid) {
        if (uuid == null) {
            return Integer.MIN_VALUE;
        }
        try {
            return this.mSoundTriggerService.stopRecognitionForIntent(new ParcelUuid(uuid));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int unloadSoundModel(UUID uuid) {
        if (uuid == null) {
            return Integer.MIN_VALUE;
        }
        try {
            return this.mSoundTriggerService.unloadSoundModel(new ParcelUuid(uuid));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isRecognitionActive(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        try {
            return this.mSoundTriggerService.isRecognitionActive(new ParcelUuid(uuid));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getDetectionServiceOperationsTimeout() {
        try {
            return Settings.Global.getInt(this.mContext.getContentResolver(), Settings.Global.SOUND_TRIGGER_DETECTION_SERVICE_OP_TIMEOUT);
        } catch (Settings.SettingNotFoundException e) {
            return Integer.MAX_VALUE;
        }
    }
}
