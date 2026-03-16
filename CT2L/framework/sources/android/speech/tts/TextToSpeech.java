package android.speech.tts;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioAttributes;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.speech.tts.ITextToSpeechCallback;
import android.speech.tts.ITextToSpeechService;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

public class TextToSpeech {
    public static final String ACTION_TTS_QUEUE_PROCESSING_COMPLETED = "android.speech.tts.TTS_QUEUE_PROCESSING_COMPLETED";
    public static final int ERROR = -1;
    public static final int ERROR_INVALID_REQUEST = -8;
    public static final int ERROR_NETWORK = -6;
    public static final int ERROR_NETWORK_TIMEOUT = -7;
    public static final int ERROR_NOT_INSTALLED_YET = -9;
    public static final int ERROR_OUTPUT = -5;
    public static final int ERROR_SERVICE = -4;
    public static final int ERROR_SYNTHESIS = -3;
    public static final int LANG_AVAILABLE = 0;
    public static final int LANG_COUNTRY_AVAILABLE = 1;
    public static final int LANG_COUNTRY_VAR_AVAILABLE = 2;
    public static final int LANG_MISSING_DATA = -1;
    public static final int LANG_NOT_SUPPORTED = -2;
    public static final int QUEUE_ADD = 1;
    static final int QUEUE_DESTROY = 2;
    public static final int QUEUE_FLUSH = 0;
    public static final int STOPPED = -2;
    public static final int SUCCESS = 0;
    private static final String TAG = "TextToSpeech";
    private Connection mConnectingServiceConnection;
    private final Context mContext;
    private volatile String mCurrentEngine;
    private final Map<String, Uri> mEarcons;
    private final TtsEngines mEnginesHelper;
    private OnInitListener mInitListener;
    private final Bundle mParams;
    private String mRequestedEngine;
    private Connection mServiceConnection;
    private final Object mStartLock;
    private final boolean mUseFallback;
    private volatile UtteranceProgressListener mUtteranceProgressListener;
    private final Map<CharSequence, Uri> mUtterances;

    private interface Action<R> {
        R run(ITextToSpeechService iTextToSpeechService) throws RemoteException;
    }

    public interface OnInitListener {
        void onInit(int i);
    }

    @Deprecated
    public interface OnUtteranceCompletedListener {
        void onUtteranceCompleted(String str);
    }

    public class Engine {
        public static final String ACTION_CHECK_TTS_DATA = "android.speech.tts.engine.CHECK_TTS_DATA";
        public static final String ACTION_GET_SAMPLE_TEXT = "android.speech.tts.engine.GET_SAMPLE_TEXT";
        public static final String ACTION_INSTALL_TTS_DATA = "android.speech.tts.engine.INSTALL_TTS_DATA";
        public static final String ACTION_TTS_DATA_INSTALLED = "android.speech.tts.engine.TTS_DATA_INSTALLED";

        @Deprecated
        public static final int CHECK_VOICE_DATA_BAD_DATA = -1;
        public static final int CHECK_VOICE_DATA_FAIL = 0;

        @Deprecated
        public static final int CHECK_VOICE_DATA_MISSING_DATA = -2;

        @Deprecated
        public static final int CHECK_VOICE_DATA_MISSING_VOLUME = -3;
        public static final int CHECK_VOICE_DATA_PASS = 1;

        @Deprecated
        public static final String DEFAULT_ENGINE = "com.svox.pico";
        public static final float DEFAULT_PAN = 0.0f;
        public static final int DEFAULT_PITCH = 100;
        public static final int DEFAULT_RATE = 100;
        public static final int DEFAULT_STREAM = 3;
        public static final float DEFAULT_VOLUME = 1.0f;
        public static final String EXTRA_AVAILABLE_VOICES = "availableVoices";

        @Deprecated
        public static final String EXTRA_CHECK_VOICE_DATA_FOR = "checkVoiceDataFor";
        public static final String EXTRA_SAMPLE_TEXT = "sampleText";

        @Deprecated
        public static final String EXTRA_TTS_DATA_INSTALLED = "dataInstalled";
        public static final String EXTRA_UNAVAILABLE_VOICES = "unavailableVoices";

        @Deprecated
        public static final String EXTRA_VOICE_DATA_FILES = "dataFiles";

        @Deprecated
        public static final String EXTRA_VOICE_DATA_FILES_INFO = "dataFilesInfo";

        @Deprecated
        public static final String EXTRA_VOICE_DATA_ROOT_DIRECTORY = "dataRoot";
        public static final String INTENT_ACTION_TTS_SERVICE = "android.intent.action.TTS_SERVICE";

        @Deprecated
        public static final String KEY_FEATURE_EMBEDDED_SYNTHESIS = "embeddedTts";
        public static final String KEY_FEATURE_NETWORK_RETRIES_COUNT = "networkRetriesCount";

        @Deprecated
        public static final String KEY_FEATURE_NETWORK_SYNTHESIS = "networkTts";
        public static final String KEY_FEATURE_NETWORK_TIMEOUT_MS = "networkTimeoutMs";
        public static final String KEY_FEATURE_NOT_INSTALLED = "notInstalled";
        public static final String KEY_PARAM_AUDIO_ATTRIBUTES = "audioAttributes";
        public static final String KEY_PARAM_COUNTRY = "country";
        public static final String KEY_PARAM_ENGINE = "engine";
        public static final String KEY_PARAM_LANGUAGE = "language";
        public static final String KEY_PARAM_PAN = "pan";
        public static final String KEY_PARAM_PITCH = "pitch";
        public static final String KEY_PARAM_RATE = "rate";
        public static final String KEY_PARAM_SESSION_ID = "sessionId";
        public static final String KEY_PARAM_STREAM = "streamType";
        public static final String KEY_PARAM_UTTERANCE_ID = "utteranceId";
        public static final String KEY_PARAM_VARIANT = "variant";
        public static final String KEY_PARAM_VOICE_NAME = "voiceName";
        public static final String KEY_PARAM_VOLUME = "volume";
        public static final String SERVICE_META_DATA = "android.speech.tts";
        public static final int USE_DEFAULTS = 0;

        public Engine() {
        }
    }

    public TextToSpeech(Context context, OnInitListener listener) {
        this(context, listener, null);
    }

    public TextToSpeech(Context context, OnInitListener listener, String engine) {
        this(context, listener, engine, null, true);
    }

    public TextToSpeech(Context context, OnInitListener listener, String engine, String packageName, boolean useFallback) {
        this.mStartLock = new Object();
        this.mParams = new Bundle();
        this.mCurrentEngine = null;
        this.mContext = context;
        this.mInitListener = listener;
        this.mRequestedEngine = engine;
        this.mUseFallback = useFallback;
        this.mEarcons = new HashMap();
        this.mUtterances = new HashMap();
        this.mUtteranceProgressListener = null;
        this.mEnginesHelper = new TtsEngines(this.mContext);
        initTts();
    }

    private <R> R runActionNoReconnect(Action<R> action, R r, String str, boolean z) {
        return (R) runAction(action, r, str, false, z);
    }

    private <R> R runAction(Action<R> action, R r, String str) {
        return (R) runAction(action, r, str, true, true);
    }

    private <R> R runAction(Action<R> action, R r, String str, boolean z, boolean z2) {
        synchronized (this.mStartLock) {
            if (this.mServiceConnection == null) {
                Log.w(TAG, str + " failed: not bound to TTS engine");
            } else {
                r = (R) this.mServiceConnection.runAction(action, r, str, z, z2);
            }
        }
        return r;
    }

    private int initTts() {
        if (this.mRequestedEngine != null) {
            if (this.mEnginesHelper.isEngineInstalled(this.mRequestedEngine)) {
                if (connectToEngine(this.mRequestedEngine)) {
                    this.mCurrentEngine = this.mRequestedEngine;
                    return 0;
                }
                if (!this.mUseFallback) {
                    this.mCurrentEngine = null;
                    dispatchOnInit(-1);
                    return -1;
                }
            } else if (!this.mUseFallback) {
                Log.i(TAG, "Requested engine not installed: " + this.mRequestedEngine);
                this.mCurrentEngine = null;
                dispatchOnInit(-1);
                return -1;
            }
        }
        String defaultEngine = getDefaultEngine();
        if (defaultEngine != null && !defaultEngine.equals(this.mRequestedEngine) && connectToEngine(defaultEngine)) {
            this.mCurrentEngine = defaultEngine;
            return 0;
        }
        String highestRanked = this.mEnginesHelper.getHighestRankedEngineName();
        if (highestRanked != null && !highestRanked.equals(this.mRequestedEngine) && !highestRanked.equals(defaultEngine) && connectToEngine(highestRanked)) {
            this.mCurrentEngine = highestRanked;
            return 0;
        }
        this.mCurrentEngine = null;
        dispatchOnInit(-1);
        return -1;
    }

    private boolean connectToEngine(String engine) {
        Connection connection = new Connection();
        Intent intent = new Intent(Engine.INTENT_ACTION_TTS_SERVICE);
        intent.setPackage(engine);
        boolean bound = this.mContext.bindService(intent, connection, 1);
        if (!bound) {
            Log.e(TAG, "Failed to bind to " + engine);
            return false;
        }
        Log.i(TAG, "Sucessfully bound to " + engine);
        this.mConnectingServiceConnection = connection;
        return true;
    }

    private void dispatchOnInit(int result) {
        synchronized (this.mStartLock) {
            if (this.mInitListener != null) {
                this.mInitListener.onInit(result);
                this.mInitListener = null;
            }
        }
    }

    private IBinder getCallerIdentity() {
        return this.mServiceConnection.getCallerIdentity();
    }

    public void shutdown() {
        synchronized (this.mStartLock) {
            if (this.mConnectingServiceConnection != null) {
                this.mContext.unbindService(this.mConnectingServiceConnection);
                this.mConnectingServiceConnection = null;
            } else {
                runActionNoReconnect(new Action<Void>() {
                    @Override
                    public Void run(ITextToSpeechService service) throws RemoteException {
                        service.setCallback(TextToSpeech.this.getCallerIdentity(), null);
                        service.stop(TextToSpeech.this.getCallerIdentity());
                        TextToSpeech.this.mServiceConnection.disconnect();
                        TextToSpeech.this.mServiceConnection = null;
                        TextToSpeech.this.mCurrentEngine = null;
                        return null;
                    }
                }, null, "shutdown", false);
            }
        }
    }

    public int addSpeech(String text, String packagename, int resourceId) {
        synchronized (this.mStartLock) {
            this.mUtterances.put(text, makeResourceUri(packagename, resourceId));
        }
        return 0;
    }

    public int addSpeech(CharSequence text, String packagename, int resourceId) {
        synchronized (this.mStartLock) {
            this.mUtterances.put(text, makeResourceUri(packagename, resourceId));
        }
        return 0;
    }

    public int addSpeech(String text, String filename) {
        synchronized (this.mStartLock) {
            this.mUtterances.put(text, Uri.parse(filename));
        }
        return 0;
    }

    public int addSpeech(CharSequence text, File file) {
        synchronized (this.mStartLock) {
            this.mUtterances.put(text, Uri.fromFile(file));
        }
        return 0;
    }

    public int addEarcon(String earcon, String packagename, int resourceId) {
        synchronized (this.mStartLock) {
            this.mEarcons.put(earcon, makeResourceUri(packagename, resourceId));
        }
        return 0;
    }

    @Deprecated
    public int addEarcon(String earcon, String filename) {
        synchronized (this.mStartLock) {
            this.mEarcons.put(earcon, Uri.parse(filename));
        }
        return 0;
    }

    public int addEarcon(String earcon, File file) {
        synchronized (this.mStartLock) {
            this.mEarcons.put(earcon, Uri.fromFile(file));
        }
        return 0;
    }

    private Uri makeResourceUri(String packageName, int resourceId) {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).encodedAuthority(packageName).appendEncodedPath(String.valueOf(resourceId)).build();
    }

    public int speak(final CharSequence text, final int queueMode, final Bundle params, final String utteranceId) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService service) throws RemoteException {
                Uri utteranceUri = (Uri) TextToSpeech.this.mUtterances.get(text);
                return utteranceUri != null ? Integer.valueOf(service.playAudio(TextToSpeech.this.getCallerIdentity(), utteranceUri, queueMode, TextToSpeech.this.getParams(params), utteranceId)) : Integer.valueOf(service.speak(TextToSpeech.this.getCallerIdentity(), text, queueMode, TextToSpeech.this.getParams(params), utteranceId));
            }
        }, -1, "speak")).intValue();
    }

    @Deprecated
    public int speak(String text, int queueMode, HashMap<String, String> params) {
        return speak(text, queueMode, convertParamsHashMaptoBundle(params), params == null ? null : params.get(Engine.KEY_PARAM_UTTERANCE_ID));
    }

    public int playEarcon(final String earcon, final int queueMode, final Bundle params, final String utteranceId) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService service) throws RemoteException {
                Uri earconUri = (Uri) TextToSpeech.this.mEarcons.get(earcon);
                if (earconUri != null) {
                    return Integer.valueOf(service.playAudio(TextToSpeech.this.getCallerIdentity(), earconUri, queueMode, TextToSpeech.this.getParams(params), utteranceId));
                }
                return -1;
            }
        }, -1, "playEarcon")).intValue();
    }

    @Deprecated
    public int playEarcon(String earcon, int queueMode, HashMap<String, String> params) {
        return playEarcon(earcon, queueMode, convertParamsHashMaptoBundle(params), params == null ? null : params.get(Engine.KEY_PARAM_UTTERANCE_ID));
    }

    public int playSilentUtterance(final long durationInMs, final int queueMode, final String utteranceId) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService service) throws RemoteException {
                return Integer.valueOf(service.playSilence(TextToSpeech.this.getCallerIdentity(), durationInMs, queueMode, utteranceId));
            }
        }, -1, "playSilentUtterance")).intValue();
    }

    @Deprecated
    public int playSilence(long durationInMs, int queueMode, HashMap<String, String> params) {
        return playSilentUtterance(durationInMs, queueMode, params == null ? null : params.get(Engine.KEY_PARAM_UTTERANCE_ID));
    }

    @Deprecated
    public Set<String> getFeatures(final Locale locale) {
        return (Set) runAction(new Action<Set<String>>() {
            @Override
            public Set<String> run(ITextToSpeechService service) throws RemoteException {
                try {
                    String[] features = service.getFeaturesForLanguage(locale.getISO3Language(), locale.getISO3Country(), locale.getVariant());
                    if (features == null) {
                        return null;
                    }
                    Set<String> featureSet = new HashSet<>();
                    Collections.addAll(featureSet, features);
                    return featureSet;
                } catch (MissingResourceException e) {
                    Log.w(TextToSpeech.TAG, "Couldn't retrieve 3 letter ISO 639-2/T language and/or ISO 3166 country code for locale: " + locale, e);
                    return null;
                }
            }
        }, null, "getFeatures");
    }

    public boolean isSpeaking() {
        return ((Boolean) runAction(new Action<Boolean>() {
            @Override
            public Boolean run(ITextToSpeechService service) throws RemoteException {
                return Boolean.valueOf(service.isSpeaking());
            }
        }, false, "isSpeaking")).booleanValue();
    }

    public int stop() {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService service) throws RemoteException {
                return Integer.valueOf(service.stop(TextToSpeech.this.getCallerIdentity()));
            }
        }, -1, "stop")).intValue();
    }

    public int setSpeechRate(float speechRate) {
        int intRate;
        if (speechRate > 0.0f && (intRate = (int) (100.0f * speechRate)) > 0) {
            synchronized (this.mStartLock) {
                this.mParams.putInt(Engine.KEY_PARAM_RATE, intRate);
            }
            return 0;
        }
        return -1;
    }

    public int setPitch(float pitch) {
        int intPitch;
        if (pitch > 0.0f && (intPitch = (int) (100.0f * pitch)) > 0) {
            synchronized (this.mStartLock) {
                this.mParams.putInt(Engine.KEY_PARAM_PITCH, intPitch);
            }
            return 0;
        }
        return -1;
    }

    public int setAudioAttributes(AudioAttributes audioAttributes) {
        if (audioAttributes != null) {
            synchronized (this.mStartLock) {
                this.mParams.putParcelable(Engine.KEY_PARAM_AUDIO_ATTRIBUTES, audioAttributes);
            }
            return 0;
        }
        return -1;
    }

    public String getCurrentEngine() {
        return this.mCurrentEngine;
    }

    @Deprecated
    public Locale getDefaultLanguage() {
        return (Locale) runAction(new Action<Locale>() {
            @Override
            public Locale run(ITextToSpeechService service) throws RemoteException {
                String[] defaultLanguage = service.getClientDefaultLanguage();
                return new Locale(defaultLanguage[0], defaultLanguage[1], defaultLanguage[2]);
            }
        }, null, "getDefaultLanguage");
    }

    public int setLanguage(final Locale loc) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService service) throws RemoteException {
                if (loc == null) {
                    return -2;
                }
                try {
                    String language = loc.getISO3Language();
                    try {
                        String country = loc.getISO3Country();
                        String variant = loc.getVariant();
                        int result = service.isLanguageAvailable(language, country, variant);
                        if (result >= 0) {
                            if (result < 2) {
                                variant = ProxyInfo.LOCAL_EXCL_LIST;
                                if (result < 1) {
                                    country = ProxyInfo.LOCAL_EXCL_LIST;
                                }
                            }
                            String voiceName = service.getDefaultVoiceNameFor(language, country, variant);
                            if (!TextUtils.isEmpty(voiceName)) {
                                if (service.loadVoice(TextToSpeech.this.getCallerIdentity(), voiceName) != -1) {
                                    TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_VOICE_NAME, voiceName);
                                    TextToSpeech.this.mParams.putString("language", language);
                                    TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_COUNTRY, country);
                                    TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_VARIANT, variant);
                                } else {
                                    return -2;
                                }
                            } else {
                                Log.w(TextToSpeech.TAG, "Couldn't find the default voice for " + language + "/" + country + "/" + variant);
                                return -2;
                            }
                        }
                        return Integer.valueOf(result);
                    } catch (MissingResourceException e) {
                        Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 3166 country code for locale: " + loc, e);
                        return -2;
                    }
                } catch (MissingResourceException e2) {
                    Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 639-2/T language code for locale: " + loc, e2);
                    return -2;
                }
            }
        }, -2, "setLanguage")).intValue();
    }

    @Deprecated
    public Locale getLanguage() {
        return (Locale) runAction(new Action<Locale>() {
            @Override
            public Locale run(ITextToSpeechService service) {
                String lang = TextToSpeech.this.mParams.getString("language", ProxyInfo.LOCAL_EXCL_LIST);
                String country = TextToSpeech.this.mParams.getString(Engine.KEY_PARAM_COUNTRY, ProxyInfo.LOCAL_EXCL_LIST);
                String variant = TextToSpeech.this.mParams.getString(Engine.KEY_PARAM_VARIANT, ProxyInfo.LOCAL_EXCL_LIST);
                return new Locale(lang, country, variant);
            }
        }, null, "getLanguage");
    }

    public Set<Locale> getAvailableLanguages() {
        return (Set) runAction(new Action<Set<Locale>>() {
            @Override
            public Set<Locale> run(ITextToSpeechService service) throws RemoteException {
                List<Voice> voices = service.getVoices();
                if (voices == null) {
                    return new HashSet();
                }
                HashSet<Locale> locales = new HashSet<>();
                for (Voice voice : voices) {
                    locales.add(voice.getLocale());
                }
                return locales;
            }
        }, null, "getAvailableLanguages");
    }

    public Set<Voice> getVoices() {
        return (Set) runAction(new Action<Set<Voice>>() {
            @Override
            public Set<Voice> run(ITextToSpeechService service) throws RemoteException {
                List<Voice> voices = service.getVoices();
                return voices != null ? new HashSet(voices) : new HashSet();
            }
        }, null, "getVoices");
    }

    public int setVoice(final Voice voice) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService service) throws RemoteException {
                int result = service.loadVoice(TextToSpeech.this.getCallerIdentity(), voice.getName());
                if (result == 0) {
                    TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_VOICE_NAME, voice.getName());
                    String language = ProxyInfo.LOCAL_EXCL_LIST;
                    try {
                        language = voice.getLocale().getISO3Language();
                    } catch (MissingResourceException e) {
                        Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 639-2/T language code for locale: " + voice.getLocale(), e);
                    }
                    String country = ProxyInfo.LOCAL_EXCL_LIST;
                    try {
                        country = voice.getLocale().getISO3Country();
                    } catch (MissingResourceException e2) {
                        Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 3166 country code for locale: " + voice.getLocale(), e2);
                    }
                    TextToSpeech.this.mParams.putString("language", language);
                    TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_COUNTRY, country);
                    TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_VARIANT, voice.getLocale().getVariant());
                }
                return Integer.valueOf(result);
            }
        }, -2, "setVoice")).intValue();
    }

    public Voice getVoice() {
        return (Voice) runAction(new Action<Voice>() {
            @Override
            public Voice run(ITextToSpeechService service) throws RemoteException {
                List<Voice> voices;
                String voiceName = TextToSpeech.this.mParams.getString(Engine.KEY_PARAM_VOICE_NAME, ProxyInfo.LOCAL_EXCL_LIST);
                if (!TextUtils.isEmpty(voiceName) && (voices = service.getVoices()) != null) {
                    for (Voice voice : voices) {
                        if (voice.getName().equals(voiceName)) {
                            return voice;
                        }
                    }
                    return null;
                }
                return null;
            }
        }, null, "getVoice");
    }

    public Voice getDefaultVoice() {
        return (Voice) runAction(new Action<Voice>() {
            @Override
            public Voice run(ITextToSpeechService service) throws RemoteException {
                List<Voice> voices;
                String[] defaultLanguage = service.getClientDefaultLanguage();
                if (defaultLanguage == null || defaultLanguage.length == 0) {
                    Log.e(TextToSpeech.TAG, "service.getClientDefaultLanguage() returned empty array");
                    return null;
                }
                String language = defaultLanguage[0];
                String country = defaultLanguage.length > 1 ? defaultLanguage[1] : ProxyInfo.LOCAL_EXCL_LIST;
                String variant = defaultLanguage.length > 2 ? defaultLanguage[2] : ProxyInfo.LOCAL_EXCL_LIST;
                int result = service.isLanguageAvailable(language, country, variant);
                if (result < 0) {
                    return null;
                }
                if (result < 2) {
                    variant = ProxyInfo.LOCAL_EXCL_LIST;
                    if (result < 1) {
                        country = ProxyInfo.LOCAL_EXCL_LIST;
                    }
                }
                String voiceName = service.getDefaultVoiceNameFor(language, country, variant);
                if (!TextUtils.isEmpty(voiceName) && (voices = service.getVoices()) != null) {
                    for (Voice voice : voices) {
                        if (voice.getName().equals(voiceName)) {
                            return voice;
                        }
                    }
                    return null;
                }
                return null;
            }
        }, null, "getDefaultVoice");
    }

    public int isLanguageAvailable(final Locale loc) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService service) throws RemoteException {
                try {
                    String language = loc.getISO3Language();
                    try {
                        String country = loc.getISO3Country();
                        return Integer.valueOf(service.isLanguageAvailable(language, country, loc.getVariant()));
                    } catch (MissingResourceException e) {
                        Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 3166 country code for locale: " + loc, e);
                        return -2;
                    }
                } catch (MissingResourceException e2) {
                    Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 639-2/T language code for locale: " + loc, e2);
                    return -2;
                }
            }
        }, -2, "isLanguageAvailable")).intValue();
    }

    public int synthesizeToFile(final CharSequence text, final Bundle params, final File file, final String utteranceId) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService service) throws RemoteException {
                int iValueOf;
                try {
                    if (file.exists() && !file.canWrite()) {
                        Log.e(TextToSpeech.TAG, "Can't write to " + file);
                        iValueOf = -1;
                    } else {
                        ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(file, 738197504);
                        int returnValue = service.synthesizeToFileDescriptor(TextToSpeech.this.getCallerIdentity(), text, fileDescriptor, TextToSpeech.this.getParams(params), utteranceId);
                        fileDescriptor.close();
                        iValueOf = Integer.valueOf(returnValue);
                    }
                    return iValueOf;
                } catch (FileNotFoundException e) {
                    Log.e(TextToSpeech.TAG, "Opening file " + file + " failed", e);
                    return -1;
                } catch (IOException e2) {
                    Log.e(TextToSpeech.TAG, "Closing file " + file + " failed", e2);
                    return -1;
                }
            }
        }, -1, "synthesizeToFile")).intValue();
    }

    @Deprecated
    public int synthesizeToFile(String text, HashMap<String, String> params, String filename) {
        return synthesizeToFile(text, convertParamsHashMaptoBundle(params), new File(filename), params.get(Engine.KEY_PARAM_UTTERANCE_ID));
    }

    private Bundle convertParamsHashMaptoBundle(HashMap<String, String> params) {
        if (params != null && !params.isEmpty()) {
            Bundle bundle = new Bundle();
            copyIntParam(bundle, params, Engine.KEY_PARAM_STREAM);
            copyIntParam(bundle, params, Engine.KEY_PARAM_SESSION_ID);
            copyStringParam(bundle, params, Engine.KEY_PARAM_UTTERANCE_ID);
            copyFloatParam(bundle, params, "volume");
            copyFloatParam(bundle, params, Engine.KEY_PARAM_PAN);
            copyStringParam(bundle, params, Engine.KEY_FEATURE_NETWORK_SYNTHESIS);
            copyStringParam(bundle, params, Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
            copyIntParam(bundle, params, Engine.KEY_FEATURE_NETWORK_TIMEOUT_MS);
            copyIntParam(bundle, params, Engine.KEY_FEATURE_NETWORK_RETRIES_COUNT);
            if (TextUtils.isEmpty(this.mCurrentEngine)) {
                return bundle;
            }
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                if (key != null && key.startsWith(this.mCurrentEngine)) {
                    bundle.putString(key, entry.getValue());
                }
            }
            return bundle;
        }
        return null;
    }

    private Bundle getParams(Bundle params) {
        if (params == null || params.isEmpty()) {
            return this.mParams;
        }
        Bundle bundle = new Bundle(this.mParams);
        bundle.putAll(params);
        verifyIntegerBundleParam(bundle, Engine.KEY_PARAM_STREAM);
        verifyIntegerBundleParam(bundle, Engine.KEY_PARAM_SESSION_ID);
        verifyStringBundleParam(bundle, Engine.KEY_PARAM_UTTERANCE_ID);
        verifyFloatBundleParam(bundle, "volume");
        verifyFloatBundleParam(bundle, Engine.KEY_PARAM_PAN);
        verifyBooleanBundleParam(bundle, Engine.KEY_FEATURE_NETWORK_SYNTHESIS);
        verifyBooleanBundleParam(bundle, Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
        verifyIntegerBundleParam(bundle, Engine.KEY_FEATURE_NETWORK_TIMEOUT_MS);
        verifyIntegerBundleParam(bundle, Engine.KEY_FEATURE_NETWORK_RETRIES_COUNT);
        return bundle;
    }

    private static boolean verifyIntegerBundleParam(Bundle bundle, String key) {
        if (!bundle.containsKey(key) || (bundle.get(key) instanceof Integer) || (bundle.get(key) instanceof Long)) {
            return true;
        }
        bundle.remove(key);
        Log.w(TAG, "Synthesis request paramter " + key + " containst value  with invalid type. Should be an Integer or a Long");
        return false;
    }

    private static boolean verifyStringBundleParam(Bundle bundle, String key) {
        if (!bundle.containsKey(key) || (bundle.get(key) instanceof String)) {
            return true;
        }
        bundle.remove(key);
        Log.w(TAG, "Synthesis request paramter " + key + " containst value  with invalid type. Should be a String");
        return false;
    }

    private static boolean verifyBooleanBundleParam(Bundle bundle, String key) {
        if (!bundle.containsKey(key) || (bundle.get(key) instanceof Boolean) || (bundle.get(key) instanceof String)) {
            return true;
        }
        bundle.remove(key);
        Log.w(TAG, "Synthesis request paramter " + key + " containst value  with invalid type. Should be a Boolean or String");
        return false;
    }

    private static boolean verifyFloatBundleParam(Bundle bundle, String key) {
        if (!bundle.containsKey(key) || (bundle.get(key) instanceof Float) || (bundle.get(key) instanceof Double)) {
            return true;
        }
        bundle.remove(key);
        Log.w(TAG, "Synthesis request paramter " + key + " containst value  with invalid type. Should be a Float or a Double");
        return false;
    }

    private void copyStringParam(Bundle bundle, HashMap<String, String> params, String key) {
        String value = params.get(key);
        if (value != null) {
            bundle.putString(key, value);
        }
    }

    private void copyIntParam(Bundle bundle, HashMap<String, String> params, String key) {
        String valueString = params.get(key);
        if (!TextUtils.isEmpty(valueString)) {
            try {
                int value = Integer.parseInt(valueString);
                bundle.putInt(key, value);
            } catch (NumberFormatException e) {
            }
        }
    }

    private void copyFloatParam(Bundle bundle, HashMap<String, String> params, String key) {
        String valueString = params.get(key);
        if (!TextUtils.isEmpty(valueString)) {
            try {
                float value = Float.parseFloat(valueString);
                bundle.putFloat(key, value);
            } catch (NumberFormatException e) {
            }
        }
    }

    @Deprecated
    public int setOnUtteranceCompletedListener(OnUtteranceCompletedListener listener) {
        this.mUtteranceProgressListener = UtteranceProgressListener.from(listener);
        return 0;
    }

    public int setOnUtteranceProgressListener(UtteranceProgressListener listener) {
        this.mUtteranceProgressListener = listener;
        return 0;
    }

    @Deprecated
    public int setEngineByPackageName(String enginePackageName) {
        this.mRequestedEngine = enginePackageName;
        return initTts();
    }

    public String getDefaultEngine() {
        return this.mEnginesHelper.getDefaultEngine();
    }

    @Deprecated
    public boolean areDefaultsEnforced() {
        return false;
    }

    public List<EngineInfo> getEngines() {
        return this.mEnginesHelper.getEngines();
    }

    private class Connection implements ServiceConnection {
        private final ITextToSpeechCallback.Stub mCallback;
        private boolean mEstablished;
        private SetupConnectionAsyncTask mOnSetupConnectionAsyncTask;
        private ITextToSpeechService mService;

        private Connection() {
            this.mCallback = new ITextToSpeechCallback.Stub() {
                @Override
                public void onStop(String utteranceId) throws RemoteException {
                    UtteranceProgressListener listener = TextToSpeech.this.mUtteranceProgressListener;
                    if (listener != null) {
                        listener.onDone(utteranceId);
                    }
                }

                @Override
                public void onSuccess(String utteranceId) {
                    UtteranceProgressListener listener = TextToSpeech.this.mUtteranceProgressListener;
                    if (listener != null) {
                        listener.onDone(utteranceId);
                    }
                }

                @Override
                public void onError(String utteranceId, int errorCode) {
                    UtteranceProgressListener listener = TextToSpeech.this.mUtteranceProgressListener;
                    if (listener != null) {
                        listener.onError(utteranceId);
                    }
                }

                @Override
                public void onStart(String utteranceId) {
                    UtteranceProgressListener listener = TextToSpeech.this.mUtteranceProgressListener;
                    if (listener != null) {
                        listener.onStart(utteranceId);
                    }
                }
            };
        }

        private class SetupConnectionAsyncTask extends AsyncTask<Void, Void, Integer> {
            private final ComponentName mName;

            public SetupConnectionAsyncTask(ComponentName name) {
                this.mName = name;
            }

            @Override
            protected Integer doInBackground(Void... params) {
                Integer num;
                synchronized (TextToSpeech.this.mStartLock) {
                    if (!isCancelled()) {
                        try {
                            Connection.this.mService.setCallback(Connection.this.getCallerIdentity(), Connection.this.mCallback);
                            if (TextToSpeech.this.mParams.getString("language") == null) {
                                String[] defaultLanguage = Connection.this.mService.getClientDefaultLanguage();
                                TextToSpeech.this.mParams.putString("language", defaultLanguage[0]);
                                TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_COUNTRY, defaultLanguage[1]);
                                TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_VARIANT, defaultLanguage[2]);
                                String defaultVoiceName = Connection.this.mService.getDefaultVoiceNameFor(defaultLanguage[0], defaultLanguage[1], defaultLanguage[2]);
                                TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_VOICE_NAME, defaultVoiceName);
                            }
                            Log.i(TextToSpeech.TAG, "Set up connection to " + this.mName);
                            num = 0;
                        } catch (RemoteException e) {
                            Log.e(TextToSpeech.TAG, "Error connecting to service, setCallback() failed");
                            num = -1;
                        }
                    } else {
                        num = null;
                    }
                }
                return num;
            }

            @Override
            protected void onPostExecute(Integer result) {
                synchronized (TextToSpeech.this.mStartLock) {
                    if (Connection.this.mOnSetupConnectionAsyncTask == this) {
                        Connection.this.mOnSetupConnectionAsyncTask = null;
                    }
                    Connection.this.mEstablished = true;
                    TextToSpeech.this.dispatchOnInit(result.intValue());
                }
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (TextToSpeech.this.mStartLock) {
                TextToSpeech.this.mConnectingServiceConnection = null;
                Log.i(TextToSpeech.TAG, "Connected to " + name);
                if (this.mOnSetupConnectionAsyncTask != null) {
                    this.mOnSetupConnectionAsyncTask.cancel(false);
                }
                this.mService = ITextToSpeechService.Stub.asInterface(service);
                TextToSpeech.this.mServiceConnection = this;
                this.mEstablished = false;
                this.mOnSetupConnectionAsyncTask = new SetupConnectionAsyncTask(name);
                this.mOnSetupConnectionAsyncTask.execute(new Void[0]);
            }
        }

        public IBinder getCallerIdentity() {
            return this.mCallback;
        }

        private boolean clearServiceConnection() {
            boolean result;
            synchronized (TextToSpeech.this.mStartLock) {
                result = false;
                if (this.mOnSetupConnectionAsyncTask != null) {
                    result = this.mOnSetupConnectionAsyncTask.cancel(false);
                    this.mOnSetupConnectionAsyncTask = null;
                }
                this.mService = null;
                if (TextToSpeech.this.mServiceConnection == this) {
                    TextToSpeech.this.mServiceConnection = null;
                }
            }
            return result;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TextToSpeech.TAG, "Asked to disconnect from " + name);
            if (clearServiceConnection()) {
                TextToSpeech.this.dispatchOnInit(-1);
            }
        }

        public void disconnect() {
            TextToSpeech.this.mContext.unbindService(this);
            clearServiceConnection();
        }

        public boolean isEstablished() {
            return this.mService != null && this.mEstablished;
        }

        public <R> R runAction(Action<R> action, R errorResult, String method, boolean reconnect, boolean onlyEstablishedConnection) {
            synchronized (TextToSpeech.this.mStartLock) {
                try {
                    if (this.mService == null) {
                        Log.w(TextToSpeech.TAG, method + " failed: not connected to TTS engine");
                    } else if (onlyEstablishedConnection && !isEstablished()) {
                        Log.w(TextToSpeech.TAG, method + " failed: TTS engine connection not fully set up");
                    } else {
                        errorResult = action.run(this.mService);
                    }
                } catch (RemoteException ex) {
                    Log.e(TextToSpeech.TAG, method + " failed", ex);
                    if (reconnect) {
                        disconnect();
                        TextToSpeech.this.initTts();
                    }
                }
            }
            return errorResult;
        }
    }

    public static class EngineInfo {
        public int icon;
        public String label;
        public String name;
        public int priority;
        public boolean system;

        public String toString() {
            return "EngineInfo{name=" + this.name + "}";
        }
    }

    public static int getMaxSpeechInputLength() {
        return 4000;
    }
}
