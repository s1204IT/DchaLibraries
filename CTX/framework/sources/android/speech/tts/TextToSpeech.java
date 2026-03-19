package android.speech.tts;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioAttributes;
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
import com.android.internal.content.NativeLibraryHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

    @Retention(RetentionPolicy.SOURCE)
    public @interface Error {
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

    public TextToSpeech(Context context, OnInitListener onInitListener) {
        this(context, onInitListener, null);
    }

    public TextToSpeech(Context context, OnInitListener onInitListener, String str) {
        this(context, onInitListener, str, null, true);
    }

    public TextToSpeech(Context context, OnInitListener onInitListener, String str, String str2, boolean z) {
        this.mStartLock = new Object();
        this.mParams = new Bundle();
        this.mCurrentEngine = null;
        this.mContext = context;
        this.mInitListener = onInitListener;
        this.mRequestedEngine = str;
        this.mUseFallback = z;
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
                return r;
            }
            return (R) this.mServiceConnection.runAction(action, r, str, z, z2);
        }
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
        String highestRankedEngineName = this.mEnginesHelper.getHighestRankedEngineName();
        if (highestRankedEngineName != null && !highestRankedEngineName.equals(this.mRequestedEngine) && !highestRankedEngineName.equals(defaultEngine) && connectToEngine(highestRankedEngineName)) {
            this.mCurrentEngine = highestRankedEngineName;
            return 0;
        }
        this.mCurrentEngine = null;
        dispatchOnInit(-1);
        return -1;
    }

    private boolean connectToEngine(String str) {
        Connection connection = new Connection();
        Intent intent = new Intent(Engine.INTENT_ACTION_TTS_SERVICE);
        intent.setPackage(str);
        if (!this.mContext.bindService(intent, connection, 1)) {
            Log.e(TAG, "Failed to bind to " + str);
            return false;
        }
        Log.i(TAG, "Sucessfully bound to " + str);
        this.mConnectingServiceConnection = connection;
        return true;
    }

    private void dispatchOnInit(int i) {
        synchronized (this.mStartLock) {
            if (this.mInitListener != null) {
                this.mInitListener.onInit(i);
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
                    public Void run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                        iTextToSpeechService.setCallback(TextToSpeech.this.getCallerIdentity(), null);
                        iTextToSpeechService.stop(TextToSpeech.this.getCallerIdentity());
                        TextToSpeech.this.mServiceConnection.disconnect();
                        TextToSpeech.this.mServiceConnection = null;
                        TextToSpeech.this.mCurrentEngine = null;
                        return null;
                    }
                }, null, "shutdown", false);
            }
        }
    }

    public int addSpeech(String str, String str2, int i) {
        synchronized (this.mStartLock) {
            this.mUtterances.put(str, makeResourceUri(str2, i));
        }
        return 0;
    }

    public int addSpeech(CharSequence charSequence, String str, int i) {
        synchronized (this.mStartLock) {
            this.mUtterances.put(charSequence, makeResourceUri(str, i));
        }
        return 0;
    }

    public int addSpeech(String str, String str2) {
        synchronized (this.mStartLock) {
            this.mUtterances.put(str, Uri.parse(str2));
        }
        return 0;
    }

    public int addSpeech(CharSequence charSequence, File file) {
        synchronized (this.mStartLock) {
            this.mUtterances.put(charSequence, Uri.fromFile(file));
        }
        return 0;
    }

    public int addEarcon(String str, String str2, int i) {
        synchronized (this.mStartLock) {
            this.mEarcons.put(str, makeResourceUri(str2, i));
        }
        return 0;
    }

    @Deprecated
    public int addEarcon(String str, String str2) {
        synchronized (this.mStartLock) {
            this.mEarcons.put(str, Uri.parse(str2));
        }
        return 0;
    }

    public int addEarcon(String str, File file) {
        synchronized (this.mStartLock) {
            this.mEarcons.put(str, Uri.fromFile(file));
        }
        return 0;
    }

    private Uri makeResourceUri(String str, int i) {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).encodedAuthority(str).appendEncodedPath(String.valueOf(i)).build();
    }

    public int speak(final CharSequence charSequence, final int i, final Bundle bundle, final String str) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                Uri uri = (Uri) TextToSpeech.this.mUtterances.get(charSequence);
                if (uri == null) {
                    return Integer.valueOf(iTextToSpeechService.speak(TextToSpeech.this.getCallerIdentity(), charSequence, i, TextToSpeech.this.getParams(bundle), str));
                }
                return Integer.valueOf(iTextToSpeechService.playAudio(TextToSpeech.this.getCallerIdentity(), uri, i, TextToSpeech.this.getParams(bundle), str));
            }
        }, -1, "speak")).intValue();
    }

    @Deprecated
    public int speak(String str, int i, HashMap<String, String> map) {
        return speak(str, i, convertParamsHashMaptoBundle(map), map == null ? null : map.get(Engine.KEY_PARAM_UTTERANCE_ID));
    }

    public int playEarcon(final String str, final int i, final Bundle bundle, final String str2) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                Uri uri = (Uri) TextToSpeech.this.mEarcons.get(str);
                if (uri == null) {
                    return -1;
                }
                return Integer.valueOf(iTextToSpeechService.playAudio(TextToSpeech.this.getCallerIdentity(), uri, i, TextToSpeech.this.getParams(bundle), str2));
            }
        }, -1, "playEarcon")).intValue();
    }

    @Deprecated
    public int playEarcon(String str, int i, HashMap<String, String> map) {
        return playEarcon(str, i, convertParamsHashMaptoBundle(map), map == null ? null : map.get(Engine.KEY_PARAM_UTTERANCE_ID));
    }

    public int playSilentUtterance(final long j, final int i, final String str) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                return Integer.valueOf(iTextToSpeechService.playSilence(TextToSpeech.this.getCallerIdentity(), j, i, str));
            }
        }, -1, "playSilentUtterance")).intValue();
    }

    @Deprecated
    public int playSilence(long j, int i, HashMap<String, String> map) {
        return playSilentUtterance(j, i, map == null ? null : map.get(Engine.KEY_PARAM_UTTERANCE_ID));
    }

    @Deprecated
    public Set<String> getFeatures(final Locale locale) {
        return (Set) runAction(new Action<Set<String>>() {
            @Override
            public Set<String> run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                try {
                    String[] featuresForLanguage = iTextToSpeechService.getFeaturesForLanguage(locale.getISO3Language(), locale.getISO3Country(), locale.getVariant());
                    if (featuresForLanguage == null) {
                        return null;
                    }
                    HashSet hashSet = new HashSet();
                    Collections.addAll(hashSet, featuresForLanguage);
                    return hashSet;
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
            public Boolean run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                return Boolean.valueOf(iTextToSpeechService.isSpeaking());
            }
        }, false, "isSpeaking")).booleanValue();
    }

    public int stop() {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                return Integer.valueOf(iTextToSpeechService.stop(TextToSpeech.this.getCallerIdentity()));
            }
        }, -1, "stop")).intValue();
    }

    public int setSpeechRate(float f) {
        int i;
        if (f > 0.0f && (i = (int) (f * 100.0f)) > 0) {
            synchronized (this.mStartLock) {
                this.mParams.putInt(Engine.KEY_PARAM_RATE, i);
            }
            return 0;
        }
        return -1;
    }

    public int setPitch(float f) {
        int i;
        if (f > 0.0f && (i = (int) (f * 100.0f)) > 0) {
            synchronized (this.mStartLock) {
                this.mParams.putInt(Engine.KEY_PARAM_PITCH, i);
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
            public Locale run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                String[] clientDefaultLanguage = iTextToSpeechService.getClientDefaultLanguage();
                return new Locale(clientDefaultLanguage[0], clientDefaultLanguage[1], clientDefaultLanguage[2]);
            }
        }, null, "getDefaultLanguage");
    }

    public int setLanguage(final Locale locale) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                if (locale == null) {
                    return -2;
                }
                try {
                    String iSO3Language = locale.getISO3Language();
                    try {
                        String iSO3Country = locale.getISO3Country();
                        String variant = locale.getVariant();
                        int iIsLanguageAvailable = iTextToSpeechService.isLanguageAvailable(iSO3Language, iSO3Country, variant);
                        if (iIsLanguageAvailable >= 0) {
                            String defaultVoiceNameFor = iTextToSpeechService.getDefaultVoiceNameFor(iSO3Language, iSO3Country, variant);
                            if (!TextUtils.isEmpty(defaultVoiceNameFor)) {
                                if (iTextToSpeechService.loadVoice(TextToSpeech.this.getCallerIdentity(), defaultVoiceNameFor) != -1) {
                                    Voice voice = TextToSpeech.this.getVoice(iTextToSpeechService, defaultVoiceNameFor);
                                    if (voice == null) {
                                        Log.w(TextToSpeech.TAG, "getDefaultVoiceNameFor returned " + defaultVoiceNameFor + " for locale " + iSO3Language + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + iSO3Country + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + variant + " but getVoice returns null");
                                        return -2;
                                    }
                                    String iSO3Language2 = "";
                                    try {
                                        iSO3Language2 = voice.getLocale().getISO3Language();
                                    } catch (MissingResourceException e) {
                                        Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 639-2/T language code for locale: " + voice.getLocale(), e);
                                    }
                                    String iSO3Country2 = "";
                                    try {
                                        iSO3Country2 = voice.getLocale().getISO3Country();
                                    } catch (MissingResourceException e2) {
                                        Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 3166 country code for locale: " + voice.getLocale(), e2);
                                    }
                                    TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_VOICE_NAME, defaultVoiceNameFor);
                                    TextToSpeech.this.mParams.putString("language", iSO3Language2);
                                    TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_COUNTRY, iSO3Country2);
                                    TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_VARIANT, voice.getLocale().getVariant());
                                } else {
                                    Log.w(TextToSpeech.TAG, "The service claimed " + iSO3Language + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + iSO3Country + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + variant + " was available with voice name " + defaultVoiceNameFor + " but loadVoice returned ERROR");
                                    return -2;
                                }
                            } else {
                                Log.w(TextToSpeech.TAG, "Couldn't find the default voice for " + iSO3Language + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + iSO3Country + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + variant);
                                return -2;
                            }
                        }
                        return Integer.valueOf(iIsLanguageAvailable);
                    } catch (MissingResourceException e3) {
                        Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 3166 country code for locale: " + locale, e3);
                        return -2;
                    }
                } catch (MissingResourceException e4) {
                    Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 639-2/T language code for locale: " + locale, e4);
                    return -2;
                }
            }
        }, -2, "setLanguage")).intValue();
    }

    @Deprecated
    public Locale getLanguage() {
        return (Locale) runAction(new Action<Locale>() {
            @Override
            public Locale run(ITextToSpeechService iTextToSpeechService) {
                return new Locale(TextToSpeech.this.mParams.getString("language", ""), TextToSpeech.this.mParams.getString(Engine.KEY_PARAM_COUNTRY, ""), TextToSpeech.this.mParams.getString(Engine.KEY_PARAM_VARIANT, ""));
            }
        }, null, "getLanguage");
    }

    public Set<Locale> getAvailableLanguages() {
        return (Set) runAction(new Action<Set<Locale>>() {
            @Override
            public Set<Locale> run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                List<Voice> voices = iTextToSpeechService.getVoices();
                if (voices == null) {
                    return new HashSet();
                }
                HashSet hashSet = new HashSet();
                Iterator<Voice> it = voices.iterator();
                while (it.hasNext()) {
                    hashSet.add(it.next().getLocale());
                }
                return hashSet;
            }
        }, null, "getAvailableLanguages");
    }

    public Set<Voice> getVoices() {
        return (Set) runAction(new Action<Set<Voice>>() {
            @Override
            public Set<Voice> run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                List<Voice> voices = iTextToSpeechService.getVoices();
                return voices != null ? new HashSet(voices) : new HashSet();
            }
        }, null, "getVoices");
    }

    public int setVoice(final Voice voice) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                int iLoadVoice = iTextToSpeechService.loadVoice(TextToSpeech.this.getCallerIdentity(), voice.getName());
                if (iLoadVoice == 0) {
                    TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_VOICE_NAME, voice.getName());
                    String iSO3Language = "";
                    try {
                        iSO3Language = voice.getLocale().getISO3Language();
                    } catch (MissingResourceException e) {
                        Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 639-2/T language code for locale: " + voice.getLocale(), e);
                    }
                    String iSO3Country = "";
                    try {
                        iSO3Country = voice.getLocale().getISO3Country();
                    } catch (MissingResourceException e2) {
                        Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 3166 country code for locale: " + voice.getLocale(), e2);
                    }
                    TextToSpeech.this.mParams.putString("language", iSO3Language);
                    TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_COUNTRY, iSO3Country);
                    TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_VARIANT, voice.getLocale().getVariant());
                }
                return Integer.valueOf(iLoadVoice);
            }
        }, -2, "setVoice")).intValue();
    }

    public Voice getVoice() {
        return (Voice) runAction(new Action<Voice>() {
            @Override
            public Voice run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                String string = TextToSpeech.this.mParams.getString(Engine.KEY_PARAM_VOICE_NAME, "");
                if (!TextUtils.isEmpty(string)) {
                    return TextToSpeech.this.getVoice(iTextToSpeechService, string);
                }
                return null;
            }
        }, null, "getVoice");
    }

    private Voice getVoice(ITextToSpeechService iTextToSpeechService, String str) throws RemoteException {
        List<Voice> voices = iTextToSpeechService.getVoices();
        if (voices == null) {
            Log.w(TAG, "getVoices returned null");
            return null;
        }
        for (Voice voice : voices) {
            if (voice.getName().equals(str)) {
                return voice;
            }
        }
        Log.w(TAG, "Could not find voice " + str + " in voice list");
        return null;
    }

    public Voice getDefaultVoice() {
        return (Voice) runAction(new Action<Voice>() {
            @Override
            public Voice run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                List<Voice> voices;
                String[] clientDefaultLanguage = iTextToSpeechService.getClientDefaultLanguage();
                if (clientDefaultLanguage == null || clientDefaultLanguage.length == 0) {
                    Log.e(TextToSpeech.TAG, "service.getClientDefaultLanguage() returned empty array");
                    return null;
                }
                String str = clientDefaultLanguage[0];
                String str2 = clientDefaultLanguage.length > 1 ? clientDefaultLanguage[1] : "";
                String str3 = clientDefaultLanguage.length > 2 ? clientDefaultLanguage[2] : "";
                if (iTextToSpeechService.isLanguageAvailable(str, str2, str3) < 0) {
                    return null;
                }
                String defaultVoiceNameFor = iTextToSpeechService.getDefaultVoiceNameFor(str, str2, str3);
                if (TextUtils.isEmpty(defaultVoiceNameFor) || (voices = iTextToSpeechService.getVoices()) == null) {
                    return null;
                }
                for (Voice voice : voices) {
                    if (voice.getName().equals(defaultVoiceNameFor)) {
                        return voice;
                    }
                }
                return null;
            }
        }, null, "getDefaultVoice");
    }

    public int isLanguageAvailable(final Locale locale) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                try {
                    try {
                        return Integer.valueOf(iTextToSpeechService.isLanguageAvailable(locale.getISO3Language(), locale.getISO3Country(), locale.getVariant()));
                    } catch (MissingResourceException e) {
                        Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 3166 country code for locale: " + locale, e);
                        return -2;
                    }
                } catch (MissingResourceException e2) {
                    Log.w(TextToSpeech.TAG, "Couldn't retrieve ISO 639-2/T language code for locale: " + locale, e2);
                    return -2;
                }
            }
        }, -2, "isLanguageAvailable")).intValue();
    }

    public int synthesizeToFile(final CharSequence charSequence, final Bundle bundle, final File file, final String str) {
        return ((Integer) runAction(new Action<Integer>() {
            @Override
            public Integer run(ITextToSpeechService iTextToSpeechService) throws RemoteException {
                try {
                    if (file.exists() && !file.canWrite()) {
                        Log.e(TextToSpeech.TAG, "Can't write to " + file);
                        return -1;
                    }
                    ParcelFileDescriptor parcelFileDescriptorOpen = ParcelFileDescriptor.open(file, 738197504);
                    int iSynthesizeToFileDescriptor = iTextToSpeechService.synthesizeToFileDescriptor(TextToSpeech.this.getCallerIdentity(), charSequence, parcelFileDescriptorOpen, TextToSpeech.this.getParams(bundle), str);
                    parcelFileDescriptorOpen.close();
                    return Integer.valueOf(iSynthesizeToFileDescriptor);
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
    public int synthesizeToFile(String str, HashMap<String, String> map, String str2) {
        return synthesizeToFile(str, convertParamsHashMaptoBundle(map), new File(str2), map.get(Engine.KEY_PARAM_UTTERANCE_ID));
    }

    private Bundle convertParamsHashMaptoBundle(HashMap<String, String> map) {
        if (map != null && !map.isEmpty()) {
            Bundle bundle = new Bundle();
            copyIntParam(bundle, map, Engine.KEY_PARAM_STREAM);
            copyIntParam(bundle, map, Engine.KEY_PARAM_SESSION_ID);
            copyStringParam(bundle, map, Engine.KEY_PARAM_UTTERANCE_ID);
            copyFloatParam(bundle, map, "volume");
            copyFloatParam(bundle, map, Engine.KEY_PARAM_PAN);
            copyStringParam(bundle, map, Engine.KEY_FEATURE_NETWORK_SYNTHESIS);
            copyStringParam(bundle, map, Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
            copyIntParam(bundle, map, Engine.KEY_FEATURE_NETWORK_TIMEOUT_MS);
            copyIntParam(bundle, map, Engine.KEY_FEATURE_NETWORK_RETRIES_COUNT);
            if (!TextUtils.isEmpty(this.mCurrentEngine)) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String key = entry.getKey();
                    if (key != null && key.startsWith(this.mCurrentEngine)) {
                        bundle.putString(key, entry.getValue());
                    }
                }
            }
            return bundle;
        }
        return null;
    }

    private Bundle getParams(Bundle bundle) {
        if (bundle != null && !bundle.isEmpty()) {
            Bundle bundle2 = new Bundle(this.mParams);
            bundle2.putAll(bundle);
            verifyIntegerBundleParam(bundle2, Engine.KEY_PARAM_STREAM);
            verifyIntegerBundleParam(bundle2, Engine.KEY_PARAM_SESSION_ID);
            verifyStringBundleParam(bundle2, Engine.KEY_PARAM_UTTERANCE_ID);
            verifyFloatBundleParam(bundle2, "volume");
            verifyFloatBundleParam(bundle2, Engine.KEY_PARAM_PAN);
            verifyBooleanBundleParam(bundle2, Engine.KEY_FEATURE_NETWORK_SYNTHESIS);
            verifyBooleanBundleParam(bundle2, Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
            verifyIntegerBundleParam(bundle2, Engine.KEY_FEATURE_NETWORK_TIMEOUT_MS);
            verifyIntegerBundleParam(bundle2, Engine.KEY_FEATURE_NETWORK_RETRIES_COUNT);
            return bundle2;
        }
        return this.mParams;
    }

    private static boolean verifyIntegerBundleParam(Bundle bundle, String str) {
        if (bundle.containsKey(str) && !(bundle.get(str) instanceof Integer) && !(bundle.get(str) instanceof Long)) {
            bundle.remove(str);
            Log.w(TAG, "Synthesis request paramter " + str + " containst value  with invalid type. Should be an Integer or a Long");
            return false;
        }
        return true;
    }

    private static boolean verifyStringBundleParam(Bundle bundle, String str) {
        if (bundle.containsKey(str) && !(bundle.get(str) instanceof String)) {
            bundle.remove(str);
            Log.w(TAG, "Synthesis request paramter " + str + " containst value  with invalid type. Should be a String");
            return false;
        }
        return true;
    }

    private static boolean verifyBooleanBundleParam(Bundle bundle, String str) {
        if (bundle.containsKey(str) && !(bundle.get(str) instanceof Boolean) && !(bundle.get(str) instanceof String)) {
            bundle.remove(str);
            Log.w(TAG, "Synthesis request paramter " + str + " containst value  with invalid type. Should be a Boolean or String");
            return false;
        }
        return true;
    }

    private static boolean verifyFloatBundleParam(Bundle bundle, String str) {
        if (bundle.containsKey(str) && !(bundle.get(str) instanceof Float) && !(bundle.get(str) instanceof Double)) {
            bundle.remove(str);
            Log.w(TAG, "Synthesis request paramter " + str + " containst value  with invalid type. Should be a Float or a Double");
            return false;
        }
        return true;
    }

    private void copyStringParam(Bundle bundle, HashMap<String, String> map, String str) {
        String str2 = map.get(str);
        if (str2 != null) {
            bundle.putString(str, str2);
        }
    }

    private void copyIntParam(Bundle bundle, HashMap<String, String> map, String str) {
        String str2 = map.get(str);
        if (!TextUtils.isEmpty(str2)) {
            try {
                bundle.putInt(str, Integer.parseInt(str2));
            } catch (NumberFormatException e) {
            }
        }
    }

    private void copyFloatParam(Bundle bundle, HashMap<String, String> map, String str) {
        String str2 = map.get(str);
        if (!TextUtils.isEmpty(str2)) {
            try {
                bundle.putFloat(str, Float.parseFloat(str2));
            } catch (NumberFormatException e) {
            }
        }
    }

    @Deprecated
    public int setOnUtteranceCompletedListener(OnUtteranceCompletedListener onUtteranceCompletedListener) {
        this.mUtteranceProgressListener = UtteranceProgressListener.from(onUtteranceCompletedListener);
        return 0;
    }

    public int setOnUtteranceProgressListener(UtteranceProgressListener utteranceProgressListener) {
        this.mUtteranceProgressListener = utteranceProgressListener;
        return 0;
    }

    @Deprecated
    public int setEngineByPackageName(String str) {
        this.mRequestedEngine = str;
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
                public void onStop(String str, boolean z) throws RemoteException {
                    UtteranceProgressListener utteranceProgressListener = TextToSpeech.this.mUtteranceProgressListener;
                    if (utteranceProgressListener != null) {
                        utteranceProgressListener.onStop(str, z);
                    }
                }

                @Override
                public void onSuccess(String str) {
                    UtteranceProgressListener utteranceProgressListener = TextToSpeech.this.mUtteranceProgressListener;
                    if (utteranceProgressListener != null) {
                        utteranceProgressListener.onDone(str);
                    }
                }

                @Override
                public void onError(String str, int i) {
                    UtteranceProgressListener utteranceProgressListener = TextToSpeech.this.mUtteranceProgressListener;
                    if (utteranceProgressListener != null) {
                        utteranceProgressListener.onError(str);
                    }
                }

                @Override
                public void onStart(String str) {
                    UtteranceProgressListener utteranceProgressListener = TextToSpeech.this.mUtteranceProgressListener;
                    if (utteranceProgressListener != null) {
                        utteranceProgressListener.onStart(str);
                    }
                }

                @Override
                public void onBeginSynthesis(String str, int i, int i2, int i3) {
                    UtteranceProgressListener utteranceProgressListener = TextToSpeech.this.mUtteranceProgressListener;
                    if (utteranceProgressListener != null) {
                        utteranceProgressListener.onBeginSynthesis(str, i, i2, i3);
                    }
                }

                @Override
                public void onAudioAvailable(String str, byte[] bArr) {
                    UtteranceProgressListener utteranceProgressListener = TextToSpeech.this.mUtteranceProgressListener;
                    if (utteranceProgressListener != null) {
                        utteranceProgressListener.onAudioAvailable(str, bArr);
                    }
                }

                @Override
                public void onRangeStart(String str, int i, int i2, int i3) {
                    UtteranceProgressListener utteranceProgressListener = TextToSpeech.this.mUtteranceProgressListener;
                    if (utteranceProgressListener != null) {
                        utteranceProgressListener.onRangeStart(str, i, i2, i3);
                    }
                }
            };
        }

        private class SetupConnectionAsyncTask extends AsyncTask<Void, Void, Integer> {
            private final ComponentName mName;

            public SetupConnectionAsyncTask(ComponentName componentName) {
                this.mName = componentName;
            }

            @Override
            protected Integer doInBackground(Void... voidArr) {
                synchronized (TextToSpeech.this.mStartLock) {
                    if (!isCancelled()) {
                        try {
                            Connection.this.mService.setCallback(Connection.this.getCallerIdentity(), Connection.this.mCallback);
                            if (TextToSpeech.this.mParams.getString("language") == null) {
                                String[] clientDefaultLanguage = Connection.this.mService.getClientDefaultLanguage();
                                TextToSpeech.this.mParams.putString("language", clientDefaultLanguage[0]);
                                TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_COUNTRY, clientDefaultLanguage[1]);
                                TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_VARIANT, clientDefaultLanguage[2]);
                                TextToSpeech.this.mParams.putString(Engine.KEY_PARAM_VOICE_NAME, Connection.this.mService.getDefaultVoiceNameFor(clientDefaultLanguage[0], clientDefaultLanguage[1], clientDefaultLanguage[2]));
                            }
                            Log.i(TextToSpeech.TAG, "Set up connection to " + this.mName);
                            return 0;
                        } catch (RemoteException e) {
                            Log.e(TextToSpeech.TAG, "Error connecting to service, setCallback() failed");
                            return -1;
                        }
                    }
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Integer num) {
                synchronized (TextToSpeech.this.mStartLock) {
                    if (Connection.this.mOnSetupConnectionAsyncTask == this) {
                        Connection.this.mOnSetupConnectionAsyncTask = null;
                    }
                    Connection.this.mEstablished = true;
                    TextToSpeech.this.dispatchOnInit(num.intValue());
                }
            }
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (TextToSpeech.this.mStartLock) {
                TextToSpeech.this.mConnectingServiceConnection = null;
                Log.i(TextToSpeech.TAG, "Connected to " + componentName);
                if (this.mOnSetupConnectionAsyncTask != null) {
                    this.mOnSetupConnectionAsyncTask.cancel(false);
                }
                this.mService = ITextToSpeechService.Stub.asInterface(iBinder);
                TextToSpeech.this.mServiceConnection = this;
                this.mEstablished = false;
                this.mOnSetupConnectionAsyncTask = new SetupConnectionAsyncTask(componentName);
                this.mOnSetupConnectionAsyncTask.execute(new Void[0]);
            }
        }

        public IBinder getCallerIdentity() {
            return this.mCallback;
        }

        private boolean clearServiceConnection() {
            boolean zCancel;
            synchronized (TextToSpeech.this.mStartLock) {
                zCancel = false;
                if (this.mOnSetupConnectionAsyncTask != null) {
                    zCancel = this.mOnSetupConnectionAsyncTask.cancel(false);
                    this.mOnSetupConnectionAsyncTask = null;
                }
                this.mService = null;
                if (TextToSpeech.this.mServiceConnection == this) {
                    TextToSpeech.this.mServiceConnection = null;
                }
            }
            return zCancel;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TextToSpeech.TAG, "Asked to disconnect from " + componentName);
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

        public <R> R runAction(Action<R> action, R r, String str, boolean z, boolean z2) {
            synchronized (TextToSpeech.this.mStartLock) {
                try {
                    try {
                        if (this.mService == null) {
                            Log.w(TextToSpeech.TAG, str + " failed: not connected to TTS engine");
                            return r;
                        }
                        if (z2 && !isEstablished()) {
                            Log.w(TextToSpeech.TAG, str + " failed: TTS engine connection not fully set up");
                            return r;
                        }
                        return action.run(this.mService);
                    } catch (RemoteException e) {
                        Log.e(TextToSpeech.TAG, str + " failed", e);
                        if (z) {
                            disconnect();
                            TextToSpeech.this.initTts();
                        }
                        return r;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
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
