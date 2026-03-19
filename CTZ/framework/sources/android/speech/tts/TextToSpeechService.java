package android.speech.tts;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.provider.Settings;
import android.speech.tts.ITextToSpeechService;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

public abstract class TextToSpeechService extends Service {
    private static final boolean DBG = false;
    private static final String SYNTH_THREAD_NAME = "SynthThread";
    private static final String TAG = "TextToSpeechService";
    private AudioPlaybackHandler mAudioPlaybackHandler;
    private CallbackMap mCallbacks;
    private TtsEngines mEngineHelper;
    private String mPackageName;
    private SynthHandler mSynthHandler;
    private final Object mVoicesInfoLock = new Object();
    private final ITextToSpeechService.Stub mBinder = new ITextToSpeechService.Stub() {
        @Override
        public int speak(IBinder iBinder, CharSequence charSequence, int i, Bundle bundle, String str) {
            if (checkNonNull(iBinder, charSequence, bundle)) {
                return TextToSpeechService.this.mSynthHandler.enqueueSpeechItem(i, TextToSpeechService.this.new SynthesisSpeechItem(iBinder, Binder.getCallingUid(), Binder.getCallingPid(), bundle, str, charSequence));
            }
            return -1;
        }

        @Override
        public int synthesizeToFileDescriptor(IBinder iBinder, CharSequence charSequence, ParcelFileDescriptor parcelFileDescriptor, Bundle bundle, String str) {
            if (checkNonNull(iBinder, charSequence, parcelFileDescriptor, bundle)) {
                return TextToSpeechService.this.mSynthHandler.enqueueSpeechItem(1, TextToSpeechService.this.new SynthesisToFileOutputStreamSpeechItem(iBinder, Binder.getCallingUid(), Binder.getCallingPid(), bundle, str, charSequence, new ParcelFileDescriptor.AutoCloseOutputStream(ParcelFileDescriptor.adoptFd(parcelFileDescriptor.detachFd()))));
            }
            return -1;
        }

        @Override
        public int playAudio(IBinder iBinder, Uri uri, int i, Bundle bundle, String str) {
            if (checkNonNull(iBinder, uri, bundle)) {
                return TextToSpeechService.this.mSynthHandler.enqueueSpeechItem(i, TextToSpeechService.this.new AudioSpeechItem(iBinder, Binder.getCallingUid(), Binder.getCallingPid(), bundle, str, uri));
            }
            return -1;
        }

        @Override
        public int playSilence(IBinder iBinder, long j, int i, String str) {
            if (checkNonNull(iBinder)) {
                return TextToSpeechService.this.mSynthHandler.enqueueSpeechItem(i, TextToSpeechService.this.new SilenceSpeechItem(iBinder, Binder.getCallingUid(), Binder.getCallingPid(), str, j));
            }
            return -1;
        }

        @Override
        public boolean isSpeaking() {
            return TextToSpeechService.this.mSynthHandler.isSpeaking() || TextToSpeechService.this.mAudioPlaybackHandler.isSpeaking();
        }

        @Override
        public int stop(IBinder iBinder) {
            if (checkNonNull(iBinder)) {
                return TextToSpeechService.this.mSynthHandler.stopForApp(iBinder);
            }
            return -1;
        }

        @Override
        public String[] getLanguage() {
            return TextToSpeechService.this.onGetLanguage();
        }

        @Override
        public String[] getClientDefaultLanguage() {
            return TextToSpeechService.this.getSettingsLocale();
        }

        @Override
        public int isLanguageAvailable(String str, String str2, String str3) {
            if (!checkNonNull(str)) {
                return -1;
            }
            return TextToSpeechService.this.onIsLanguageAvailable(str, str2, str3);
        }

        @Override
        public String[] getFeaturesForLanguage(String str, String str2, String str3) {
            Set<String> setOnGetFeaturesForLanguage = TextToSpeechService.this.onGetFeaturesForLanguage(str, str2, str3);
            if (setOnGetFeaturesForLanguage != null) {
                String[] strArr = new String[setOnGetFeaturesForLanguage.size()];
                setOnGetFeaturesForLanguage.toArray(strArr);
                return strArr;
            }
            return new String[0];
        }

        @Override
        public int loadLanguage(IBinder iBinder, String str, String str2, String str3) {
            if (!checkNonNull(str)) {
                return -1;
            }
            int iOnIsLanguageAvailable = TextToSpeechService.this.onIsLanguageAvailable(str, str2, str3);
            if ((iOnIsLanguageAvailable == 0 || iOnIsLanguageAvailable == 1 || iOnIsLanguageAvailable == 2) && TextToSpeechService.this.mSynthHandler.enqueueSpeechItem(1, TextToSpeechService.this.new LoadLanguageItem(iBinder, Binder.getCallingUid(), Binder.getCallingPid(), str, str2, str3)) != 0) {
                return -1;
            }
            return iOnIsLanguageAvailable;
        }

        @Override
        public List<Voice> getVoices() {
            return TextToSpeechService.this.onGetVoices();
        }

        @Override
        public int loadVoice(IBinder iBinder, String str) {
            if (!checkNonNull(str)) {
                return -1;
            }
            int iOnIsValidVoiceName = TextToSpeechService.this.onIsValidVoiceName(str);
            if (iOnIsValidVoiceName != 0 || TextToSpeechService.this.mSynthHandler.enqueueSpeechItem(1, TextToSpeechService.this.new LoadVoiceItem(iBinder, Binder.getCallingUid(), Binder.getCallingPid(), str)) == 0) {
                return iOnIsValidVoiceName;
            }
            return -1;
        }

        @Override
        public String getDefaultVoiceNameFor(String str, String str2, String str3) {
            if (!checkNonNull(str)) {
                return null;
            }
            int iOnIsLanguageAvailable = TextToSpeechService.this.onIsLanguageAvailable(str, str2, str3);
            if (iOnIsLanguageAvailable == 0 || iOnIsLanguageAvailable == 1 || iOnIsLanguageAvailable == 2) {
                return TextToSpeechService.this.onGetDefaultVoiceNameFor(str, str2, str3);
            }
            return null;
        }

        @Override
        public void setCallback(IBinder iBinder, ITextToSpeechCallback iTextToSpeechCallback) {
            if (checkNonNull(iBinder)) {
                TextToSpeechService.this.mCallbacks.setCallback(iBinder, iTextToSpeechCallback);
            }
        }

        private String intern(String str) {
            return str.intern();
        }

        private boolean checkNonNull(Object... objArr) {
            for (Object obj : objArr) {
                if (obj == null) {
                    return false;
                }
            }
            return true;
        }
    };

    interface UtteranceProgressDispatcher {
        void dispatchOnAudioAvailable(byte[] bArr);

        void dispatchOnBeginSynthesis(int i, int i2, int i3);

        void dispatchOnError(int i);

        void dispatchOnRangeStart(int i, int i2, int i3);

        void dispatchOnStart();

        void dispatchOnStop();

        void dispatchOnSuccess();
    }

    protected abstract String[] onGetLanguage();

    protected abstract int onIsLanguageAvailable(String str, String str2, String str3);

    protected abstract int onLoadLanguage(String str, String str2, String str3);

    protected abstract void onStop();

    protected abstract void onSynthesizeText(SynthesisRequest synthesisRequest, SynthesisCallback synthesisCallback);

    @Override
    public void onCreate() {
        super.onCreate();
        SynthThread synthThread = new SynthThread();
        synthThread.start();
        this.mSynthHandler = new SynthHandler(synthThread.getLooper());
        this.mAudioPlaybackHandler = new AudioPlaybackHandler();
        this.mAudioPlaybackHandler.start();
        this.mEngineHelper = new TtsEngines(this);
        this.mCallbacks = new CallbackMap();
        this.mPackageName = getApplicationInfo().packageName;
        String[] settingsLocale = getSettingsLocale();
        onLoadLanguage(settingsLocale[0], settingsLocale[1], settingsLocale[2]);
    }

    @Override
    public void onDestroy() {
        this.mSynthHandler.quit();
        this.mAudioPlaybackHandler.quit();
        this.mCallbacks.kill();
        super.onDestroy();
    }

    protected Set<String> onGetFeaturesForLanguage(String str, String str2, String str3) {
        return new HashSet();
    }

    private int getExpectedLanguageAvailableStatus(Locale locale) {
        if (locale.getVariant().isEmpty()) {
            if (locale.getCountry().isEmpty()) {
                return 0;
            }
            return 1;
        }
        return 2;
    }

    public List<Voice> onGetVoices() {
        ArrayList arrayList = new ArrayList();
        for (Locale locale : Locale.getAvailableLocales()) {
            try {
                if (onIsLanguageAvailable(locale.getISO3Language(), locale.getISO3Country(), locale.getVariant()) == getExpectedLanguageAvailableStatus(locale)) {
                    arrayList.add(new Voice(onGetDefaultVoiceNameFor(locale.getISO3Language(), locale.getISO3Country(), locale.getVariant()), locale, 300, 300, false, onGetFeaturesForLanguage(locale.getISO3Language(), locale.getISO3Country(), locale.getVariant())));
                }
            } catch (MissingResourceException e) {
            }
        }
        return arrayList;
    }

    public String onGetDefaultVoiceNameFor(String str, String str2, String str3) {
        Locale locale;
        switch (onIsLanguageAvailable(str, str2, str3)) {
            case 0:
                locale = new Locale(str);
                break;
            case 1:
                locale = new Locale(str, str2);
                break;
            case 2:
                locale = new Locale(str, str2, str3);
                break;
            default:
                return null;
        }
        String languageTag = TtsEngines.normalizeTTSLocale(locale).toLanguageTag();
        if (onIsValidVoiceName(languageTag) != 0) {
            return null;
        }
        return languageTag;
    }

    public int onLoadVoice(String str) {
        Locale localeForLanguageTag = Locale.forLanguageTag(str);
        if (localeForLanguageTag == null) {
            return -1;
        }
        try {
            if (onIsLanguageAvailable(localeForLanguageTag.getISO3Language(), localeForLanguageTag.getISO3Country(), localeForLanguageTag.getVariant()) != getExpectedLanguageAvailableStatus(localeForLanguageTag)) {
                return -1;
            }
            onLoadLanguage(localeForLanguageTag.getISO3Language(), localeForLanguageTag.getISO3Country(), localeForLanguageTag.getVariant());
            return 0;
        } catch (MissingResourceException e) {
            return -1;
        }
    }

    public int onIsValidVoiceName(String str) {
        Locale localeForLanguageTag = Locale.forLanguageTag(str);
        if (localeForLanguageTag == null) {
            return -1;
        }
        try {
            if (onIsLanguageAvailable(localeForLanguageTag.getISO3Language(), localeForLanguageTag.getISO3Country(), localeForLanguageTag.getVariant()) != getExpectedLanguageAvailableStatus(localeForLanguageTag)) {
                return -1;
            }
            return 0;
        } catch (MissingResourceException e) {
            return -1;
        }
    }

    private int getDefaultSpeechRate() {
        return getSecureSettingInt(Settings.Secure.TTS_DEFAULT_RATE, 100);
    }

    private int getDefaultPitch() {
        return getSecureSettingInt(Settings.Secure.TTS_DEFAULT_PITCH, 100);
    }

    private String[] getSettingsLocale() {
        return TtsEngines.toOldLocaleStringFormat(this.mEngineHelper.getLocalePrefForEngine(this.mPackageName));
    }

    private int getSecureSettingInt(String str, int i) {
        return Settings.Secure.getInt(getContentResolver(), str, i);
    }

    private class SynthThread extends HandlerThread implements MessageQueue.IdleHandler {
        private boolean mFirstIdle;

        public SynthThread() {
            super(TextToSpeechService.SYNTH_THREAD_NAME, 0);
            this.mFirstIdle = true;
        }

        @Override
        protected void onLooperPrepared() {
            getLooper().getQueue().addIdleHandler(this);
        }

        @Override
        public boolean queueIdle() {
            if (this.mFirstIdle) {
                this.mFirstIdle = false;
                return true;
            }
            broadcastTtsQueueProcessingCompleted();
            return true;
        }

        private void broadcastTtsQueueProcessingCompleted() {
            TextToSpeechService.this.sendBroadcast(new Intent(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED));
        }
    }

    private class SynthHandler extends Handler {
        private SpeechItem mCurrentSpeechItem;
        private int mFlushAll;
        private List<Object> mFlushedObjects;

        public SynthHandler(Looper looper) {
            super(looper);
            this.mCurrentSpeechItem = null;
            this.mFlushedObjects = new ArrayList();
            this.mFlushAll = 0;
        }

        private void startFlushingSpeechItems(Object obj) {
            synchronized (this.mFlushedObjects) {
                try {
                    if (obj == null) {
                        this.mFlushAll++;
                    } else {
                        this.mFlushedObjects.add(obj);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        private void endFlushingSpeechItems(Object obj) {
            synchronized (this.mFlushedObjects) {
                try {
                    if (obj == null) {
                        this.mFlushAll--;
                    } else {
                        this.mFlushedObjects.remove(obj);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        private boolean isFlushed(SpeechItem speechItem) {
            boolean z;
            synchronized (this.mFlushedObjects) {
                z = this.mFlushAll > 0 || this.mFlushedObjects.contains(speechItem.getCallerIdentity());
            }
            return z;
        }

        private synchronized SpeechItem getCurrentSpeechItem() {
            return this.mCurrentSpeechItem;
        }

        private synchronized boolean setCurrentSpeechItem(SpeechItem speechItem) {
            if (speechItem != null) {
                if (isFlushed(speechItem)) {
                    return false;
                }
                this.mCurrentSpeechItem = speechItem;
                return true;
            }
            this.mCurrentSpeechItem = speechItem;
            return true;
        }

        private synchronized SpeechItem removeCurrentSpeechItem() {
            SpeechItem speechItem;
            speechItem = this.mCurrentSpeechItem;
            this.mCurrentSpeechItem = null;
            return speechItem;
        }

        private synchronized SpeechItem maybeRemoveCurrentSpeechItem(Object obj) {
            if (this.mCurrentSpeechItem == null || this.mCurrentSpeechItem.getCallerIdentity() != obj) {
                return null;
            }
            SpeechItem speechItem = this.mCurrentSpeechItem;
            this.mCurrentSpeechItem = null;
            return speechItem;
        }

        public boolean isSpeaking() {
            return getCurrentSpeechItem() != null;
        }

        public void quit() {
            getLooper().quit();
            SpeechItem speechItemRemoveCurrentSpeechItem = removeCurrentSpeechItem();
            if (speechItemRemoveCurrentSpeechItem != null) {
                speechItemRemoveCurrentSpeechItem.stop();
            }
        }

        public int enqueueSpeechItem(int i, final SpeechItem speechItem) {
            UtteranceProgressDispatcher utteranceProgressDispatcher;
            if (speechItem instanceof UtteranceProgressDispatcher) {
                utteranceProgressDispatcher = (UtteranceProgressDispatcher) speechItem;
            } else {
                utteranceProgressDispatcher = null;
            }
            if (!speechItem.isValid()) {
                if (utteranceProgressDispatcher != null) {
                    utteranceProgressDispatcher.dispatchOnError(-8);
                }
                return -1;
            }
            if (i == 0) {
                stopForApp(speechItem.getCallerIdentity());
            } else if (i == 2) {
                stopAll();
            }
            Message messageObtain = Message.obtain(this, new Runnable() {
                @Override
                public void run() {
                    if (SynthHandler.this.setCurrentSpeechItem(speechItem)) {
                        speechItem.play();
                        SynthHandler.this.removeCurrentSpeechItem();
                    } else {
                        speechItem.stop();
                    }
                }
            });
            messageObtain.obj = speechItem.getCallerIdentity();
            if (sendMessage(messageObtain)) {
                return 0;
            }
            Log.w(TextToSpeechService.TAG, "SynthThread has quit");
            if (utteranceProgressDispatcher != null) {
                utteranceProgressDispatcher.dispatchOnError(-4);
            }
            return -1;
        }

        public int stopForApp(final Object obj) {
            if (obj == null) {
                return -1;
            }
            startFlushingSpeechItems(obj);
            SpeechItem speechItemMaybeRemoveCurrentSpeechItem = maybeRemoveCurrentSpeechItem(obj);
            if (speechItemMaybeRemoveCurrentSpeechItem != null) {
                speechItemMaybeRemoveCurrentSpeechItem.stop();
            }
            TextToSpeechService.this.mAudioPlaybackHandler.stopForApp(obj);
            sendMessage(Message.obtain(this, new Runnable() {
                @Override
                public void run() {
                    SynthHandler.this.endFlushingSpeechItems(obj);
                }
            }));
            return 0;
        }

        public int stopAll() {
            startFlushingSpeechItems(null);
            SpeechItem speechItemRemoveCurrentSpeechItem = removeCurrentSpeechItem();
            if (speechItemRemoveCurrentSpeechItem != null) {
                speechItemRemoveCurrentSpeechItem.stop();
            }
            TextToSpeechService.this.mAudioPlaybackHandler.stop();
            sendMessage(Message.obtain(this, new Runnable() {
                @Override
                public void run() {
                    SynthHandler.this.endFlushingSpeechItems(null);
                }
            }));
            return 0;
        }
    }

    static class AudioOutputParams {
        public final AudioAttributes mAudioAttributes;
        public final float mPan;
        public final int mSessionId;
        public final float mVolume;

        AudioOutputParams() {
            this.mSessionId = 0;
            this.mVolume = 1.0f;
            this.mPan = 0.0f;
            this.mAudioAttributes = null;
        }

        AudioOutputParams(int i, float f, float f2, AudioAttributes audioAttributes) {
            this.mSessionId = i;
            this.mVolume = f;
            this.mPan = f2;
            this.mAudioAttributes = audioAttributes;
        }

        static AudioOutputParams createFromParamsBundle(Bundle bundle, boolean z) {
            int i;
            if (bundle == null) {
                return new AudioOutputParams();
            }
            AudioAttributes audioAttributesBuild = (AudioAttributes) bundle.getParcelable(TextToSpeech.Engine.KEY_PARAM_AUDIO_ATTRIBUTES);
            if (audioAttributesBuild == null) {
                AudioAttributes.Builder legacyStreamType = new AudioAttributes.Builder().setLegacyStreamType(bundle.getInt(TextToSpeech.Engine.KEY_PARAM_STREAM, 3));
                if (z) {
                    i = 1;
                } else {
                    i = 4;
                }
                audioAttributesBuild = legacyStreamType.setContentType(i).build();
            }
            return new AudioOutputParams(bundle.getInt(TextToSpeech.Engine.KEY_PARAM_SESSION_ID, 0), bundle.getFloat("volume", 1.0f), bundle.getFloat(TextToSpeech.Engine.KEY_PARAM_PAN, 0.0f), audioAttributesBuild);
        }
    }

    private abstract class SpeechItem {
        private final Object mCallerIdentity;
        private final int mCallerPid;
        private final int mCallerUid;
        private boolean mStarted = false;
        private boolean mStopped = false;

        public abstract boolean isValid();

        protected abstract void playImpl();

        protected abstract void stopImpl();

        public SpeechItem(Object obj, int i, int i2) {
            this.mCallerIdentity = obj;
            this.mCallerUid = i;
            this.mCallerPid = i2;
        }

        public Object getCallerIdentity() {
            return this.mCallerIdentity;
        }

        public int getCallerUid() {
            return this.mCallerUid;
        }

        public int getCallerPid() {
            return this.mCallerPid;
        }

        public void play() {
            synchronized (this) {
                if (this.mStarted) {
                    throw new IllegalStateException("play() called twice");
                }
                this.mStarted = true;
            }
            playImpl();
        }

        public void stop() {
            synchronized (this) {
                if (this.mStopped) {
                    throw new IllegalStateException("stop() called twice");
                }
                this.mStopped = true;
            }
            stopImpl();
        }

        protected synchronized boolean isStopped() {
            return this.mStopped;
        }

        protected synchronized boolean isStarted() {
            return this.mStarted;
        }
    }

    private abstract class UtteranceSpeechItem extends SpeechItem implements UtteranceProgressDispatcher {
        public abstract String getUtteranceId();

        public UtteranceSpeechItem(Object obj, int i, int i2) {
            super(obj, i, i2);
        }

        @Override
        public void dispatchOnSuccess() {
            String utteranceId = getUtteranceId();
            if (utteranceId != null) {
                TextToSpeechService.this.mCallbacks.dispatchOnSuccess(getCallerIdentity(), utteranceId);
            }
        }

        @Override
        public void dispatchOnStop() {
            String utteranceId = getUtteranceId();
            if (utteranceId != null) {
                TextToSpeechService.this.mCallbacks.dispatchOnStop(getCallerIdentity(), utteranceId, isStarted());
            }
        }

        @Override
        public void dispatchOnStart() {
            String utteranceId = getUtteranceId();
            if (utteranceId != null) {
                TextToSpeechService.this.mCallbacks.dispatchOnStart(getCallerIdentity(), utteranceId);
            }
        }

        @Override
        public void dispatchOnError(int i) {
            String utteranceId = getUtteranceId();
            if (utteranceId != null) {
                TextToSpeechService.this.mCallbacks.dispatchOnError(getCallerIdentity(), utteranceId, i);
            }
        }

        @Override
        public void dispatchOnBeginSynthesis(int i, int i2, int i3) {
            String utteranceId = getUtteranceId();
            if (utteranceId != null) {
                TextToSpeechService.this.mCallbacks.dispatchOnBeginSynthesis(getCallerIdentity(), utteranceId, i, i2, i3);
            }
        }

        @Override
        public void dispatchOnAudioAvailable(byte[] bArr) {
            String utteranceId = getUtteranceId();
            if (utteranceId != null) {
                TextToSpeechService.this.mCallbacks.dispatchOnAudioAvailable(getCallerIdentity(), utteranceId, bArr);
            }
        }

        @Override
        public void dispatchOnRangeStart(int i, int i2, int i3) {
            String utteranceId = getUtteranceId();
            if (utteranceId != null) {
                TextToSpeechService.this.mCallbacks.dispatchOnRangeStart(getCallerIdentity(), utteranceId, i, i2, i3);
            }
        }

        String getStringParam(Bundle bundle, String str, String str2) {
            return bundle == null ? str2 : bundle.getString(str, str2);
        }

        int getIntParam(Bundle bundle, String str, int i) {
            return bundle == null ? i : bundle.getInt(str, i);
        }

        float getFloatParam(Bundle bundle, String str, float f) {
            return bundle == null ? f : bundle.getFloat(str, f);
        }
    }

    private abstract class UtteranceSpeechItemWithParams extends UtteranceSpeechItem {
        protected final Bundle mParams;
        protected final String mUtteranceId;

        UtteranceSpeechItemWithParams(Object obj, int i, int i2, Bundle bundle, String str) {
            super(obj, i, i2);
            this.mParams = bundle;
            this.mUtteranceId = str;
        }

        boolean hasLanguage() {
            return !TextUtils.isEmpty(getStringParam(this.mParams, "language", null));
        }

        int getSpeechRate() {
            return getIntParam(this.mParams, TextToSpeech.Engine.KEY_PARAM_RATE, TextToSpeechService.this.getDefaultSpeechRate());
        }

        int getPitch() {
            return getIntParam(this.mParams, TextToSpeech.Engine.KEY_PARAM_PITCH, TextToSpeechService.this.getDefaultPitch());
        }

        @Override
        public String getUtteranceId() {
            return this.mUtteranceId;
        }

        AudioOutputParams getAudioParams() {
            return AudioOutputParams.createFromParamsBundle(this.mParams, true);
        }
    }

    class SynthesisSpeechItem extends UtteranceSpeechItemWithParams {
        private final int mCallerUid;
        private final String[] mDefaultLocale;
        private final EventLogger mEventLogger;
        private AbstractSynthesisCallback mSynthesisCallback;
        private final SynthesisRequest mSynthesisRequest;
        private final CharSequence mText;

        public SynthesisSpeechItem(Object obj, int i, int i2, Bundle bundle, String str, CharSequence charSequence) {
            super(obj, i, i2, bundle, str);
            this.mText = charSequence;
            this.mCallerUid = i;
            this.mSynthesisRequest = new SynthesisRequest(this.mText, this.mParams);
            this.mDefaultLocale = TextToSpeechService.this.getSettingsLocale();
            setRequestParams(this.mSynthesisRequest);
            this.mEventLogger = new EventLogger(this.mSynthesisRequest, i, i2, TextToSpeechService.this.mPackageName);
        }

        public CharSequence getText() {
            return this.mText;
        }

        @Override
        public boolean isValid() {
            if (this.mText == null) {
                Log.e(TextToSpeechService.TAG, "null synthesis text");
                return false;
            }
            if (this.mText.length() >= TextToSpeech.getMaxSpeechInputLength()) {
                Log.w(TextToSpeechService.TAG, "Text too long: " + this.mText.length() + " chars");
                return false;
            }
            return true;
        }

        @Override
        protected void playImpl() {
            this.mEventLogger.onRequestProcessingStart();
            synchronized (this) {
                if (isStopped()) {
                    return;
                }
                this.mSynthesisCallback = createSynthesisCallback();
                AbstractSynthesisCallback abstractSynthesisCallback = this.mSynthesisCallback;
                TextToSpeechService.this.onSynthesizeText(this.mSynthesisRequest, abstractSynthesisCallback);
                if (abstractSynthesisCallback.hasStarted() && !abstractSynthesisCallback.hasFinished()) {
                    abstractSynthesisCallback.done();
                }
            }
        }

        protected AbstractSynthesisCallback createSynthesisCallback() {
            return new PlaybackSynthesisCallback(getAudioParams(), TextToSpeechService.this.mAudioPlaybackHandler, this, getCallerIdentity(), this.mEventLogger, false);
        }

        private void setRequestParams(SynthesisRequest synthesisRequest) {
            String voiceName = getVoiceName();
            synthesisRequest.setLanguage(getLanguage(), getCountry(), getVariant());
            if (!TextUtils.isEmpty(voiceName)) {
                synthesisRequest.setVoiceName(getVoiceName());
            }
            synthesisRequest.setSpeechRate(getSpeechRate());
            synthesisRequest.setCallerUid(this.mCallerUid);
            synthesisRequest.setPitch(getPitch());
        }

        @Override
        protected void stopImpl() {
            AbstractSynthesisCallback abstractSynthesisCallback;
            synchronized (this) {
                abstractSynthesisCallback = this.mSynthesisCallback;
            }
            if (abstractSynthesisCallback != null) {
                abstractSynthesisCallback.stop();
                TextToSpeechService.this.onStop();
            } else {
                dispatchOnStop();
            }
        }

        private String getCountry() {
            return !hasLanguage() ? this.mDefaultLocale[1] : getStringParam(this.mParams, TextToSpeech.Engine.KEY_PARAM_COUNTRY, "");
        }

        private String getVariant() {
            return !hasLanguage() ? this.mDefaultLocale[2] : getStringParam(this.mParams, TextToSpeech.Engine.KEY_PARAM_VARIANT, "");
        }

        public String getLanguage() {
            return getStringParam(this.mParams, "language", this.mDefaultLocale[0]);
        }

        public String getVoiceName() {
            return getStringParam(this.mParams, TextToSpeech.Engine.KEY_PARAM_VOICE_NAME, "");
        }
    }

    private class SynthesisToFileOutputStreamSpeechItem extends SynthesisSpeechItem {
        private final FileOutputStream mFileOutputStream;

        public SynthesisToFileOutputStreamSpeechItem(Object obj, int i, int i2, Bundle bundle, String str, CharSequence charSequence, FileOutputStream fileOutputStream) {
            super(obj, i, i2, bundle, str, charSequence);
            this.mFileOutputStream = fileOutputStream;
        }

        @Override
        protected AbstractSynthesisCallback createSynthesisCallback() {
            return new FileSynthesisCallback(this.mFileOutputStream.getChannel(), this, false);
        }

        @Override
        protected void playImpl() {
            dispatchOnStart();
            super.playImpl();
            try {
                this.mFileOutputStream.close();
            } catch (IOException e) {
                Log.w(TextToSpeechService.TAG, "Failed to close output file", e);
            }
        }
    }

    private class AudioSpeechItem extends UtteranceSpeechItemWithParams {
        private final AudioPlaybackQueueItem mItem;

        public AudioSpeechItem(Object obj, int i, int i2, Bundle bundle, String str, Uri uri) {
            super(obj, i, i2, bundle, str);
            this.mItem = new AudioPlaybackQueueItem(this, getCallerIdentity(), TextToSpeechService.this, uri, getAudioParams());
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        protected void playImpl() {
            TextToSpeechService.this.mAudioPlaybackHandler.enqueue(this.mItem);
        }

        @Override
        protected void stopImpl() {
        }

        @Override
        public String getUtteranceId() {
            return getStringParam(this.mParams, TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, null);
        }

        @Override
        AudioOutputParams getAudioParams() {
            return AudioOutputParams.createFromParamsBundle(this.mParams, false);
        }
    }

    private class SilenceSpeechItem extends UtteranceSpeechItem {
        private final long mDuration;
        private final String mUtteranceId;

        public SilenceSpeechItem(Object obj, int i, int i2, String str, long j) {
            super(obj, i, i2);
            this.mUtteranceId = str;
            this.mDuration = j;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        protected void playImpl() {
            TextToSpeechService.this.mAudioPlaybackHandler.enqueue(new SilencePlaybackQueueItem(this, getCallerIdentity(), this.mDuration));
        }

        @Override
        protected void stopImpl() {
        }

        @Override
        public String getUtteranceId() {
            return this.mUtteranceId;
        }
    }

    private class LoadLanguageItem extends SpeechItem {
        private final String mCountry;
        private final String mLanguage;
        private final String mVariant;

        public LoadLanguageItem(Object obj, int i, int i2, String str, String str2, String str3) {
            super(obj, i, i2);
            this.mLanguage = str;
            this.mCountry = str2;
            this.mVariant = str3;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        protected void playImpl() {
            TextToSpeechService.this.onLoadLanguage(this.mLanguage, this.mCountry, this.mVariant);
        }

        @Override
        protected void stopImpl() {
        }
    }

    private class LoadVoiceItem extends SpeechItem {
        private final String mVoiceName;

        public LoadVoiceItem(Object obj, int i, int i2, String str) {
            super(obj, i, i2);
            this.mVoiceName = str;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        protected void playImpl() {
            TextToSpeechService.this.onLoadVoice(this.mVoiceName);
        }

        @Override
        protected void stopImpl() {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE.equals(intent.getAction())) {
            return this.mBinder;
        }
        return null;
    }

    private class CallbackMap extends RemoteCallbackList<ITextToSpeechCallback> {
        private final HashMap<IBinder, ITextToSpeechCallback> mCallerToCallback;

        private CallbackMap() {
            this.mCallerToCallback = new HashMap<>();
        }

        public void setCallback(IBinder iBinder, ITextToSpeechCallback iTextToSpeechCallback) {
            ITextToSpeechCallback iTextToSpeechCallbackRemove;
            synchronized (this.mCallerToCallback) {
                try {
                    if (iTextToSpeechCallback != null) {
                        register(iTextToSpeechCallback, iBinder);
                        iTextToSpeechCallbackRemove = this.mCallerToCallback.put(iBinder, iTextToSpeechCallback);
                    } else {
                        iTextToSpeechCallbackRemove = this.mCallerToCallback.remove(iBinder);
                    }
                    if (iTextToSpeechCallbackRemove != null && iTextToSpeechCallbackRemove != iTextToSpeechCallback) {
                        unregister(iTextToSpeechCallbackRemove);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        public void dispatchOnStop(Object obj, String str, boolean z) {
            ITextToSpeechCallback callbackFor = getCallbackFor(obj);
            if (callbackFor == null) {
                return;
            }
            try {
                callbackFor.onStop(str, z);
            } catch (RemoteException e) {
                Log.e(TextToSpeechService.TAG, "Callback onStop failed: " + e);
            }
        }

        public void dispatchOnSuccess(Object obj, String str) {
            ITextToSpeechCallback callbackFor = getCallbackFor(obj);
            if (callbackFor == null) {
                return;
            }
            try {
                callbackFor.onSuccess(str);
            } catch (RemoteException e) {
                Log.e(TextToSpeechService.TAG, "Callback onDone failed: " + e);
            }
        }

        public void dispatchOnStart(Object obj, String str) {
            ITextToSpeechCallback callbackFor = getCallbackFor(obj);
            if (callbackFor == null) {
                return;
            }
            try {
                callbackFor.onStart(str);
            } catch (RemoteException e) {
                Log.e(TextToSpeechService.TAG, "Callback onStart failed: " + e);
            }
        }

        public void dispatchOnError(Object obj, String str, int i) {
            ITextToSpeechCallback callbackFor = getCallbackFor(obj);
            if (callbackFor == null) {
                return;
            }
            try {
                callbackFor.onError(str, i);
            } catch (RemoteException e) {
                Log.e(TextToSpeechService.TAG, "Callback onError failed: " + e);
            }
        }

        public void dispatchOnBeginSynthesis(Object obj, String str, int i, int i2, int i3) {
            ITextToSpeechCallback callbackFor = getCallbackFor(obj);
            if (callbackFor == null) {
                return;
            }
            try {
                callbackFor.onBeginSynthesis(str, i, i2, i3);
            } catch (RemoteException e) {
                Log.e(TextToSpeechService.TAG, "Callback dispatchOnBeginSynthesis(String, int, int, int) failed: " + e);
            }
        }

        public void dispatchOnAudioAvailable(Object obj, String str, byte[] bArr) {
            ITextToSpeechCallback callbackFor = getCallbackFor(obj);
            if (callbackFor == null) {
                return;
            }
            try {
                callbackFor.onAudioAvailable(str, bArr);
            } catch (RemoteException e) {
                Log.e(TextToSpeechService.TAG, "Callback dispatchOnAudioAvailable(String, byte[]) failed: " + e);
            }
        }

        public void dispatchOnRangeStart(Object obj, String str, int i, int i2, int i3) {
            ITextToSpeechCallback callbackFor = getCallbackFor(obj);
            if (callbackFor == null) {
                return;
            }
            try {
                callbackFor.onRangeStart(str, i, i2, i3);
            } catch (RemoteException e) {
                Log.e(TextToSpeechService.TAG, "Callback dispatchOnRangeStart(String, int, int, int) failed: " + e);
            }
        }

        @Override
        public void onCallbackDied(ITextToSpeechCallback iTextToSpeechCallback, Object obj) {
            IBinder iBinder = (IBinder) obj;
            synchronized (this.mCallerToCallback) {
                this.mCallerToCallback.remove(iBinder);
            }
        }

        @Override
        public void kill() {
            synchronized (this.mCallerToCallback) {
                this.mCallerToCallback.clear();
                super.kill();
            }
        }

        private ITextToSpeechCallback getCallbackFor(Object obj) {
            ITextToSpeechCallback iTextToSpeechCallback;
            IBinder iBinder = (IBinder) obj;
            synchronized (this.mCallerToCallback) {
                iTextToSpeechCallback = this.mCallerToCallback.get(iBinder);
            }
            return iTextToSpeechCallback;
        }
    }
}
