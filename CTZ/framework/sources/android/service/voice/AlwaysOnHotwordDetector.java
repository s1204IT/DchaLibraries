package android.service.voice;

import android.content.Intent;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.KeyphraseEnrollmentInfo;
import android.hardware.soundtrigger.KeyphraseMetadata;
import android.hardware.soundtrigger.SoundTrigger;
import android.media.AudioFormat;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.app.IVoiceInteractionManagerService;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

public class AlwaysOnHotwordDetector {
    static final boolean DBG = false;
    public static final int MANAGE_ACTION_ENROLL = 0;
    public static final int MANAGE_ACTION_RE_ENROLL = 1;
    public static final int MANAGE_ACTION_UN_ENROLL = 2;
    private static final int MSG_AVAILABILITY_CHANGED = 1;
    private static final int MSG_DETECTION_ERROR = 3;
    private static final int MSG_DETECTION_PAUSE = 4;
    private static final int MSG_DETECTION_RESUME = 5;
    private static final int MSG_HOTWORD_DETECTED = 2;
    public static final int RECOGNITION_FLAG_ALLOW_MULTIPLE_TRIGGERS = 2;
    public static final int RECOGNITION_FLAG_CAPTURE_TRIGGER_AUDIO = 1;
    public static final int RECOGNITION_FLAG_NONE = 0;
    public static final int RECOGNITION_MODE_USER_IDENTIFICATION = 2;
    public static final int RECOGNITION_MODE_VOICE_TRIGGER = 1;
    public static final int STATE_HARDWARE_UNAVAILABLE = -2;
    private static final int STATE_INVALID = -3;
    public static final int STATE_KEYPHRASE_ENROLLED = 2;
    public static final int STATE_KEYPHRASE_UNENROLLED = 1;
    public static final int STATE_KEYPHRASE_UNSUPPORTED = -1;
    private static final int STATE_NOT_READY = 0;
    private static final int STATUS_ERROR = Integer.MIN_VALUE;
    private static final int STATUS_OK = 0;
    static final String TAG = "AlwaysOnHotwordDetector";
    private final Callback mExternalCallback;
    private final KeyphraseEnrollmentInfo mKeyphraseEnrollmentInfo;
    private final KeyphraseMetadata mKeyphraseMetadata;
    private final Locale mLocale;
    private final IVoiceInteractionManagerService mModelManagementService;
    private final String mText;
    private final IVoiceInteractionService mVoiceInteractionService;
    private final Object mLock = new Object();
    private int mAvailability = 0;
    private final Handler mHandler = new MyHandler();
    private final SoundTriggerListener mInternalCallback = new SoundTriggerListener(this.mHandler);

    public static abstract class Callback {
        public abstract void onAvailabilityChanged(int i);

        public abstract void onDetected(EventPayload eventPayload);

        public abstract void onError();

        public abstract void onRecognitionPaused();

        public abstract void onRecognitionResumed();
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface ManageActions {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RecognitionFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RecognitionModes {
    }

    public static class EventPayload {
        private final AudioFormat mAudioFormat;
        private final boolean mCaptureAvailable;
        private final int mCaptureSession;
        private final byte[] mData;
        private final boolean mTriggerAvailable;

        private EventPayload(boolean z, boolean z2, AudioFormat audioFormat, int i, byte[] bArr) {
            this.mTriggerAvailable = z;
            this.mCaptureAvailable = z2;
            this.mCaptureSession = i;
            this.mAudioFormat = audioFormat;
            this.mData = bArr;
        }

        public AudioFormat getCaptureAudioFormat() {
            return this.mAudioFormat;
        }

        public byte[] getTriggerAudio() {
            if (this.mTriggerAvailable) {
                return this.mData;
            }
            return null;
        }

        public Integer getCaptureSession() {
            if (this.mCaptureAvailable) {
                return Integer.valueOf(this.mCaptureSession);
            }
            return null;
        }
    }

    public AlwaysOnHotwordDetector(String str, Locale locale, Callback callback, KeyphraseEnrollmentInfo keyphraseEnrollmentInfo, IVoiceInteractionService iVoiceInteractionService, IVoiceInteractionManagerService iVoiceInteractionManagerService) {
        this.mText = str;
        this.mLocale = locale;
        this.mKeyphraseEnrollmentInfo = keyphraseEnrollmentInfo;
        this.mKeyphraseMetadata = this.mKeyphraseEnrollmentInfo.getKeyphraseMetadata(str, locale);
        this.mExternalCallback = callback;
        this.mVoiceInteractionService = iVoiceInteractionService;
        this.mModelManagementService = iVoiceInteractionManagerService;
        new RefreshAvailabiltyTask().execute(new Void[0]);
    }

    public int getSupportedRecognitionModes() {
        int supportedRecognitionModesLocked;
        synchronized (this.mLock) {
            supportedRecognitionModesLocked = getSupportedRecognitionModesLocked();
        }
        return supportedRecognitionModesLocked;
    }

    private int getSupportedRecognitionModesLocked() {
        if (this.mAvailability == -3) {
            throw new IllegalStateException("getSupportedRecognitionModes called on an invalid detector");
        }
        if (this.mAvailability != 2 && this.mAvailability != 1) {
            throw new UnsupportedOperationException("Getting supported recognition modes for the keyphrase is not supported");
        }
        return this.mKeyphraseMetadata.recognitionModeFlags;
    }

    public boolean startRecognition(int i) {
        boolean z;
        synchronized (this.mLock) {
            if (this.mAvailability == -3) {
                throw new IllegalStateException("startRecognition called on an invalid detector");
            }
            if (this.mAvailability != 2) {
                throw new UnsupportedOperationException("Recognition for the given keyphrase is not supported");
            }
            z = startRecognitionLocked(i) == 0;
        }
        return z;
    }

    public boolean stopRecognition() {
        boolean z;
        synchronized (this.mLock) {
            if (this.mAvailability == -3) {
                throw new IllegalStateException("stopRecognition called on an invalid detector");
            }
            if (this.mAvailability != 2) {
                throw new UnsupportedOperationException("Recognition for the given keyphrase is not supported");
            }
            z = stopRecognitionLocked() == 0;
        }
        return z;
    }

    public Intent createEnrollIntent() {
        Intent manageIntentLocked;
        synchronized (this.mLock) {
            manageIntentLocked = getManageIntentLocked(0);
        }
        return manageIntentLocked;
    }

    public Intent createUnEnrollIntent() {
        Intent manageIntentLocked;
        synchronized (this.mLock) {
            manageIntentLocked = getManageIntentLocked(2);
        }
        return manageIntentLocked;
    }

    public Intent createReEnrollIntent() {
        Intent manageIntentLocked;
        synchronized (this.mLock) {
            manageIntentLocked = getManageIntentLocked(1);
        }
        return manageIntentLocked;
    }

    private Intent getManageIntentLocked(int i) {
        if (this.mAvailability == -3) {
            throw new IllegalStateException("getManageIntent called on an invalid detector");
        }
        if (this.mAvailability != 2 && this.mAvailability != 1) {
            throw new UnsupportedOperationException("Managing the given keyphrase is not supported");
        }
        return this.mKeyphraseEnrollmentInfo.getManageKeyphraseIntent(i, this.mText, this.mLocale);
    }

    void invalidate() {
        synchronized (this.mLock) {
            this.mAvailability = -3;
            notifyStateChangedLocked();
        }
    }

    void onSoundModelsChanged() {
        synchronized (this.mLock) {
            if (this.mAvailability != -3 && this.mAvailability != -2 && this.mAvailability != -1) {
                stopRecognitionLocked();
                new RefreshAvailabiltyTask().execute(new Void[0]);
                return;
            }
            Slog.w(TAG, "Received onSoundModelsChanged for an unsupported keyphrase/config");
        }
    }

    private int startRecognitionLocked(int i) throws RemoteException {
        boolean z = true;
        SoundTrigger.KeyphraseRecognitionExtra[] keyphraseRecognitionExtraArr = {new SoundTrigger.KeyphraseRecognitionExtra(this.mKeyphraseMetadata.id, this.mKeyphraseMetadata.recognitionModeFlags, 0, new SoundTrigger.ConfidenceLevel[0])};
        boolean z2 = (i & 1) != 0;
        if ((i & 2) == 0) {
            z = false;
        }
        int iStartRecognition = Integer.MIN_VALUE;
        try {
            iStartRecognition = this.mModelManagementService.startRecognition(this.mVoiceInteractionService, this.mKeyphraseMetadata.id, this.mLocale.toLanguageTag(), this.mInternalCallback, new SoundTrigger.RecognitionConfig(z2, z, keyphraseRecognitionExtraArr, null));
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException in startRecognition!", e);
        }
        if (iStartRecognition != 0) {
            Slog.w(TAG, "startRecognition() failed with error code " + iStartRecognition);
        }
        return iStartRecognition;
    }

    private int stopRecognitionLocked() {
        int iStopRecognition;
        try {
            iStopRecognition = this.mModelManagementService.stopRecognition(this.mVoiceInteractionService, this.mKeyphraseMetadata.id, this.mInternalCallback);
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException in stopRecognition!", e);
            iStopRecognition = Integer.MIN_VALUE;
        }
        if (iStopRecognition != 0) {
            Slog.w(TAG, "stopRecognition() failed with error code " + iStopRecognition);
        }
        return iStopRecognition;
    }

    private void notifyStateChangedLocked() {
        Message messageObtain = Message.obtain(this.mHandler, 1);
        messageObtain.arg1 = this.mAvailability;
        messageObtain.sendToTarget();
    }

    static final class SoundTriggerListener extends IRecognitionStatusCallback.Stub {
        private final Handler mHandler;

        public SoundTriggerListener(Handler handler) {
            this.mHandler = handler;
        }

        @Override
        public void onKeyphraseDetected(SoundTrigger.KeyphraseRecognitionEvent keyphraseRecognitionEvent) {
            Slog.i(AlwaysOnHotwordDetector.TAG, "onDetected");
            Message.obtain(this.mHandler, 2, new EventPayload(keyphraseRecognitionEvent.triggerInData, keyphraseRecognitionEvent.captureAvailable, keyphraseRecognitionEvent.captureFormat, keyphraseRecognitionEvent.captureSession, keyphraseRecognitionEvent.data)).sendToTarget();
        }

        @Override
        public void onGenericSoundTriggerDetected(SoundTrigger.GenericRecognitionEvent genericRecognitionEvent) {
            Slog.w(AlwaysOnHotwordDetector.TAG, "Generic sound trigger event detected at AOHD: " + genericRecognitionEvent);
        }

        @Override
        public void onError(int i) {
            Slog.i(AlwaysOnHotwordDetector.TAG, "onError: " + i);
            this.mHandler.sendEmptyMessage(3);
        }

        @Override
        public void onRecognitionPaused() {
            Slog.i(AlwaysOnHotwordDetector.TAG, "onRecognitionPaused");
            this.mHandler.sendEmptyMessage(4);
        }

        @Override
        public void onRecognitionResumed() {
            Slog.i(AlwaysOnHotwordDetector.TAG, "onRecognitionResumed");
            this.mHandler.sendEmptyMessage(5);
        }
    }

    class MyHandler extends Handler {
        MyHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            synchronized (AlwaysOnHotwordDetector.this.mLock) {
                if (AlwaysOnHotwordDetector.this.mAvailability != -3) {
                    switch (message.what) {
                        case 1:
                            AlwaysOnHotwordDetector.this.mExternalCallback.onAvailabilityChanged(message.arg1);
                            return;
                        case 2:
                            AlwaysOnHotwordDetector.this.mExternalCallback.onDetected((EventPayload) message.obj);
                            return;
                        case 3:
                            AlwaysOnHotwordDetector.this.mExternalCallback.onError();
                            return;
                        case 4:
                            AlwaysOnHotwordDetector.this.mExternalCallback.onRecognitionPaused();
                            return;
                        case 5:
                            AlwaysOnHotwordDetector.this.mExternalCallback.onRecognitionResumed();
                            return;
                        default:
                            super.handleMessage(message);
                            return;
                    }
                }
                Slog.w(AlwaysOnHotwordDetector.TAG, "Received message: " + message.what + " for an invalid detector");
            }
        }
    }

    class RefreshAvailabiltyTask extends AsyncTask<Void, Void, Void> {
        RefreshAvailabiltyTask() {
        }

        @Override
        public Void doInBackground(Void... voidArr) throws RemoteException {
            int iInternalGetInitialAvailability = internalGetInitialAvailability();
            if (iInternalGetInitialAvailability == 0 || iInternalGetInitialAvailability == 1 || iInternalGetInitialAvailability == 2) {
                iInternalGetInitialAvailability = !internalGetIsEnrolled(AlwaysOnHotwordDetector.this.mKeyphraseMetadata.id, AlwaysOnHotwordDetector.this.mLocale) ? 1 : 2;
            }
            synchronized (AlwaysOnHotwordDetector.this.mLock) {
                AlwaysOnHotwordDetector.this.mAvailability = iInternalGetInitialAvailability;
                AlwaysOnHotwordDetector.this.notifyStateChangedLocked();
            }
            return null;
        }

        private int internalGetInitialAvailability() throws RemoteException {
            synchronized (AlwaysOnHotwordDetector.this.mLock) {
                if (AlwaysOnHotwordDetector.this.mAvailability == -3) {
                    return -3;
                }
                SoundTrigger.ModuleProperties dspModuleProperties = null;
                try {
                    dspModuleProperties = AlwaysOnHotwordDetector.this.mModelManagementService.getDspModuleProperties(AlwaysOnHotwordDetector.this.mVoiceInteractionService);
                } catch (RemoteException e) {
                    Slog.w(AlwaysOnHotwordDetector.TAG, "RemoteException in getDspProperties!", e);
                }
                if (dspModuleProperties != null) {
                    if (AlwaysOnHotwordDetector.this.mKeyphraseMetadata == null) {
                        return -1;
                    }
                    return 0;
                }
                return -2;
            }
        }

        private boolean internalGetIsEnrolled(int i, Locale locale) {
            try {
                return AlwaysOnHotwordDetector.this.mModelManagementService.isEnrolledForKeyphrase(AlwaysOnHotwordDetector.this.mVoiceInteractionService, i, locale.toLanguageTag());
            } catch (RemoteException e) {
                Slog.w(AlwaysOnHotwordDetector.TAG, "RemoteException in listRegisteredKeyphraseSoundModels!", e);
                return false;
            }
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        synchronized (this.mLock) {
            printWriter.print(str);
            printWriter.print("Text=");
            printWriter.println(this.mText);
            printWriter.print(str);
            printWriter.print("Locale=");
            printWriter.println(this.mLocale);
            printWriter.print(str);
            printWriter.print("Availability=");
            printWriter.println(this.mAvailability);
            printWriter.print(str);
            printWriter.print("KeyphraseMetadata=");
            printWriter.println(this.mKeyphraseMetadata);
            printWriter.print(str);
            printWriter.print("EnrollmentInfo=");
            printWriter.println(this.mKeyphraseEnrollmentInfo);
        }
    }
}
