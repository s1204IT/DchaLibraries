package android.speech.srec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public final class Recognizer {
    public static final int EVENT_END_OF_VOICING = 6;
    public static final int EVENT_INCOMPLETE = 2;
    public static final int EVENT_INVALID = 0;
    public static final int EVENT_MAX_SPEECH = 12;
    public static final int EVENT_NEED_MORE_AUDIO = 11;
    public static final int EVENT_NO_MATCH = 1;
    public static final int EVENT_RECOGNITION_RESULT = 8;
    public static final int EVENT_RECOGNITION_TIMEOUT = 10;
    public static final int EVENT_SPOKE_TOO_SOON = 7;
    public static final int EVENT_STARTED = 3;
    public static final int EVENT_START_OF_UTTERANCE_TIMEOUT = 9;
    public static final int EVENT_START_OF_VOICING = 5;
    public static final int EVENT_STOPPED = 4;
    public static final String KEY_CONFIDENCE = "conf";
    public static final String KEY_LITERAL = "literal";
    public static final String KEY_MEANING = "meaning";
    private static String TAG;
    private Grammar mActiveGrammar = null;
    private byte[] mPutAudioBuffer = null;
    private long mRecognizer;
    private long mVocabulary;

    private static native void PMemInit();

    private static native void PMemShutdown();

    private static native String SR_AcousticStateGet(long j);

    private static native void SR_AcousticStateReset(long j);

    private static native void SR_AcousticStateSet(long j, String str);

    private static native void SR_GrammarAddWordToSlot(long j, String str, String str2, String str3, int i, String str4);

    private static native void SR_GrammarAllowAll(long j);

    private static native void SR_GrammarAllowOnly(long j, String str);

    private static native void SR_GrammarCompile(long j);

    private static native long SR_GrammarCreate();

    private static native void SR_GrammarDestroy(long j);

    private static native long SR_GrammarLoad(String str);

    private static native void SR_GrammarResetAllSlots(long j);

    private static native void SR_GrammarSave(long j, String str);

    private static native void SR_GrammarSetupRecognizer(long j, long j2);

    private static native void SR_GrammarSetupVocabulary(long j, long j2);

    private static native void SR_GrammarUnsetupRecognizer(long j);

    private static native void SR_RecognizerActivateRule(long j, long j2, String str, int i);

    private static native int SR_RecognizerAdvance(long j);

    private static native boolean SR_RecognizerCheckGrammarConsistency(long j, long j2);

    private static native long SR_RecognizerCreate();

    private static native void SR_RecognizerDeactivateAllRules(long j);

    private static native void SR_RecognizerDeactivateRule(long j, long j2, String str);

    private static native void SR_RecognizerDestroy(long j);

    private static native boolean SR_RecognizerGetBoolParameter(long j, String str);

    private static native String SR_RecognizerGetParameter(long j, String str);

    private static native int SR_RecognizerGetSize_tParameter(long j, String str);

    private static native boolean SR_RecognizerHasSetupRules(long j);

    private static native boolean SR_RecognizerIsActiveRule(long j, long j2, String str);

    private static native boolean SR_RecognizerIsSetup(long j);

    private static native boolean SR_RecognizerIsSignalClipping(long j);

    private static native boolean SR_RecognizerIsSignalDCOffset(long j);

    private static native boolean SR_RecognizerIsSignalNoisy(long j);

    private static native boolean SR_RecognizerIsSignalTooFewSamples(long j);

    private static native boolean SR_RecognizerIsSignalTooManySamples(long j);

    private static native boolean SR_RecognizerIsSignalTooQuiet(long j);

    private static native int SR_RecognizerPutAudio(long j, byte[] bArr, int i, int i2, boolean z);

    private static native int SR_RecognizerResultGetKeyCount(long j, int i);

    private static native String[] SR_RecognizerResultGetKeyList(long j, int i);

    private static native int SR_RecognizerResultGetSize(long j);

    private static native String SR_RecognizerResultGetValue(long j, int i, String str);

    private static native byte[] SR_RecognizerResultGetWaveform(long j);

    private static native void SR_RecognizerSetBoolParameter(long j, String str, boolean z);

    private static native void SR_RecognizerSetParameter(long j, String str, String str2);

    private static native void SR_RecognizerSetSize_tParameter(long j, String str, int i);

    private static native void SR_RecognizerSetup(long j);

    private static native void SR_RecognizerSetupRule(long j, long j2, String str);

    private static native void SR_RecognizerStart(long j);

    private static native void SR_RecognizerStop(long j);

    private static native void SR_RecognizerUnsetup(long j);

    private static native void SR_SessionCreate(String str);

    private static native void SR_SessionDestroy();

    private static native void SR_VocabularyDestroy(long j);

    private static native String SR_VocabularyGetPronunciation(long j, String str);

    private static native long SR_VocabularyLoad();

    static {
        System.loadLibrary("srec_jni");
        TAG = "Recognizer";
    }

    public static String getConfigDir(Locale locale) {
        if (locale == null) {
            locale = Locale.US;
        }
        String dir = "/system/usr/srec/config/" + locale.toString().replace('_', '.').toLowerCase(Locale.ROOT);
        if (new File(dir).isDirectory()) {
            return dir;
        }
        return null;
    }

    public Recognizer(String configFile) throws IOException {
        this.mVocabulary = 0L;
        this.mRecognizer = 0L;
        PMemInit();
        SR_SessionCreate(configFile);
        this.mRecognizer = SR_RecognizerCreate();
        SR_RecognizerSetup(this.mRecognizer);
        this.mVocabulary = SR_VocabularyLoad();
    }

    public class Grammar {
        private long mGrammar;

        public Grammar(String g2gFileName) throws IOException {
            this.mGrammar = 0L;
            this.mGrammar = Recognizer.SR_GrammarLoad(g2gFileName);
            Recognizer.SR_GrammarSetupVocabulary(this.mGrammar, Recognizer.this.mVocabulary);
        }

        public void resetAllSlots() {
            Recognizer.SR_GrammarResetAllSlots(this.mGrammar);
        }

        public void addWordToSlot(String slot, String word, String pron, int weight, String tag) {
            Recognizer.SR_GrammarAddWordToSlot(this.mGrammar, slot, word, pron, weight, tag);
        }

        public void compile() {
            Recognizer.SR_GrammarCompile(this.mGrammar);
        }

        public void setupRecognizer() {
            Recognizer.SR_GrammarSetupRecognizer(this.mGrammar, Recognizer.this.mRecognizer);
            Recognizer.this.mActiveGrammar = this;
        }

        public void save(String g2gFileName) throws IOException {
            Recognizer.SR_GrammarSave(this.mGrammar, g2gFileName);
        }

        public void destroy() {
            if (this.mGrammar != 0) {
                Recognizer.SR_GrammarDestroy(this.mGrammar);
                this.mGrammar = 0L;
            }
        }

        protected void finalize() {
            if (this.mGrammar != 0) {
                destroy();
                throw new IllegalStateException("someone forgot to destroy Grammar");
            }
        }
    }

    public void start() {
        SR_RecognizerActivateRule(this.mRecognizer, this.mActiveGrammar.mGrammar, "trash", 1);
        SR_RecognizerStart(this.mRecognizer);
    }

    public int advance() {
        return SR_RecognizerAdvance(this.mRecognizer);
    }

    public int putAudio(byte[] buf, int offset, int length, boolean isLast) {
        return SR_RecognizerPutAudio(this.mRecognizer, buf, offset, length, isLast);
    }

    public void putAudio(InputStream audio) throws IOException {
        if (this.mPutAudioBuffer == null) {
            this.mPutAudioBuffer = new byte[512];
        }
        int nbytes = audio.read(this.mPutAudioBuffer);
        if (nbytes == -1) {
            SR_RecognizerPutAudio(this.mRecognizer, this.mPutAudioBuffer, 0, 0, true);
        } else if (nbytes != SR_RecognizerPutAudio(this.mRecognizer, this.mPutAudioBuffer, 0, nbytes, false)) {
            throw new IOException("SR_RecognizerPutAudio failed nbytes=" + nbytes);
        }
    }

    public int getResultCount() {
        return SR_RecognizerResultGetSize(this.mRecognizer);
    }

    public String[] getResultKeys(int index) {
        return SR_RecognizerResultGetKeyList(this.mRecognizer, index);
    }

    public String getResult(int index, String key) {
        return SR_RecognizerResultGetValue(this.mRecognizer, index, key);
    }

    public void stop() {
        SR_RecognizerStop(this.mRecognizer);
        SR_RecognizerDeactivateRule(this.mRecognizer, this.mActiveGrammar.mGrammar, "trash");
    }

    public void resetAcousticState() {
        SR_AcousticStateReset(this.mRecognizer);
    }

    public void setAcousticState(String state) {
        SR_AcousticStateSet(this.mRecognizer, state);
    }

    public String getAcousticState() {
        return SR_AcousticStateGet(this.mRecognizer);
    }

    public void destroy() {
        try {
            if (this.mVocabulary != 0) {
                SR_VocabularyDestroy(this.mVocabulary);
            }
            this.mVocabulary = 0L;
            try {
                if (this.mRecognizer != 0) {
                    SR_RecognizerUnsetup(this.mRecognizer);
                }
                try {
                    if (this.mRecognizer != 0) {
                        SR_RecognizerDestroy(this.mRecognizer);
                    }
                    this.mRecognizer = 0L;
                    try {
                        SR_SessionDestroy();
                    } finally {
                    }
                } catch (Throwable th) {
                    this.mRecognizer = 0L;
                    try {
                        SR_SessionDestroy();
                        throw th;
                    } finally {
                    }
                }
            } catch (Throwable th2) {
                try {
                    if (this.mRecognizer != 0) {
                        SR_RecognizerDestroy(this.mRecognizer);
                    }
                    this.mRecognizer = 0L;
                    try {
                        SR_SessionDestroy();
                        throw th2;
                    } finally {
                    }
                } catch (Throwable th3) {
                    this.mRecognizer = 0L;
                    try {
                        SR_SessionDestroy();
                        throw th3;
                    } finally {
                    }
                }
            }
        } catch (Throwable th4) {
            this.mVocabulary = 0L;
            try {
                if (this.mRecognizer != 0) {
                    SR_RecognizerUnsetup(this.mRecognizer);
                }
                try {
                    if (this.mRecognizer != 0) {
                        SR_RecognizerDestroy(this.mRecognizer);
                    }
                    this.mRecognizer = 0L;
                    try {
                        SR_SessionDestroy();
                        throw th4;
                    } finally {
                    }
                } catch (Throwable th5) {
                    this.mRecognizer = 0L;
                    try {
                        SR_SessionDestroy();
                        throw th5;
                    } finally {
                    }
                }
            } catch (Throwable th6) {
                try {
                    if (this.mRecognizer != 0) {
                        SR_RecognizerDestroy(this.mRecognizer);
                    }
                    this.mRecognizer = 0L;
                    try {
                        SR_SessionDestroy();
                        throw th6;
                    } finally {
                    }
                } catch (Throwable th7) {
                    this.mRecognizer = 0L;
                    try {
                        SR_SessionDestroy();
                        throw th7;
                    } finally {
                    }
                }
            }
        }
    }

    protected void finalize() throws Throwable {
        if (this.mVocabulary != 0 || this.mRecognizer != 0) {
            destroy();
            throw new IllegalStateException("someone forgot to destroy Recognizer");
        }
    }

    public static String eventToString(int event) {
        switch (event) {
            case 0:
                return "EVENT_INVALID";
            case 1:
                return "EVENT_NO_MATCH";
            case 2:
                return "EVENT_INCOMPLETE";
            case 3:
                return "EVENT_STARTED";
            case 4:
                return "EVENT_STOPPED";
            case 5:
                return "EVENT_START_OF_VOICING";
            case 6:
                return "EVENT_END_OF_VOICING";
            case 7:
                return "EVENT_SPOKE_TOO_SOON";
            case 8:
                return "EVENT_RECOGNITION_RESULT";
            case 9:
                return "EVENT_START_OF_UTTERANCE_TIMEOUT";
            case 10:
                return "EVENT_RECOGNITION_TIMEOUT";
            case 11:
                return "EVENT_NEED_MORE_AUDIO";
            case 12:
                return "EVENT_MAX_SPEECH";
            default:
                return "EVENT_" + event;
        }
    }
}
