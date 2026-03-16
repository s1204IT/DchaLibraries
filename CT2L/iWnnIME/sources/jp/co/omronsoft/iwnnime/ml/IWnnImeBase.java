package jp.co.omronsoft.iwnnime.ml;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;
import com.android.common.speech.LoggingEvents;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.co.omronsoft.iwnnime.ml.candidate.CandidatesManager;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelPrefFragment;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiUtil;
import jp.co.omronsoft.iwnnime.ml.hangul.IWnnImeHangul;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnSymbolEngine;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;
import jp.co.omronsoft.iwnnime.ml.jajp.Directkan;
import jp.co.omronsoft.iwnnime.ml.jajp.IWnnImeJaJp;
import jp.co.omronsoft.iwnnime.ml.jajp.Romkan;
import jp.co.omronsoft.iwnnime.ml.jajp.RomkanFullKatakana;
import jp.co.omronsoft.iwnnime.ml.jajp.RomkanHalfKatakana;
import jp.co.omronsoft.iwnnime.ml.latin.IWnnImeLatin;
import jp.co.omronsoft.iwnnime.ml.zh.IWnnImeZh;

public class IWnnImeBase {
    protected static final int AUTO_COMMIT_ENGLISH_OFF = 1;
    protected static final int AUTO_COMMIT_ENGLISH_ON = 0;
    protected static final int AUTO_COMMIT_ENGLISH_SYMBOL = 16;
    protected static final int AUTO_CURSOR_MOVEMENT_OFF = 0;
    protected static final char CHAR_0 = '0';
    protected static final char CHAR_9 = '9';
    protected static final char CHAR_FULL_WIDTH_SPACE = 12288;
    protected static final char CHAR_HALF_WIDTH_SPACE = ' ';
    protected static final char CHAR_TAB = '\t';
    protected static final int COUNT_WILDCARD_PREDICTION_PREFIX_MATCHING = 4;
    protected static final boolean DEBUG = false;
    private static final int DELAY_MS_COMMIT_MUSHROOM_STRING = 100;
    protected static final int DELAY_MS_DELETE_DECOEMOJI_LEARNING_DICTIONARY = 0;
    protected static final int DELAY_MS_DISABLE_TOGGLE = 500;
    protected static final int DELAY_MS_UPDATE_DECOEMOJI_DICTIONARY = 0;
    protected static final int DELAY_MS_UPDATE_LEARNING_DICTIONARY = 0;
    public static final int ENGINE_MODE_EISU_KANA = 103;
    public static final int ENGINE_MODE_FULL_KATAKANA = 101;
    public static final int ENGINE_MODE_HALF_KATAKANA = 102;
    public static final int ENGINE_MODE_OPT_TYPE_12KEY = 106;
    public static final int ENGINE_MODE_OPT_TYPE_50KEY = 112;
    public static final int ENGINE_MODE_OPT_TYPE_QWERTY = 105;
    public static final int ENGINE_MODE_SYMBOL = 104;
    public static final int ENGINE_MODE_SYMBOL_ADD_SYMBOL = 111;
    public static final int ENGINE_MODE_SYMBOL_DECOEMOJI = 110;
    public static final int ENGINE_MODE_SYMBOL_EMOJI = 107;
    public static final int ENGINE_MODE_SYMBOL_KAO_MOJI = 109;
    public static final int ENGINE_MODE_SYMBOL_SYMBOL = 108;
    protected static final int LIMIT_INPUT_NUMBER = 50;
    protected static final int MSG_CLOSE = 0;
    protected static final int MSG_COMMIT_MUSHROOM = 5;
    protected static final int MSG_DELETE_DECOEMOJI_LEARNING_DICTIONARY = 4;
    protected static final int MSG_TOGGLE_TIME_LIMIT = 1;
    protected static final int MSG_UPDATE_DECOEMOJI_DICTIONARY = 3;
    protected static final int MSG_UPDATE_LEARNING_DICTIONARY = 2;
    protected static final String PREF_HAS_USED_VOICE_INPUT = "has_used_voice_input";
    protected static final int PRIVATE_AREA_CODE = 61184;
    protected static final boolean PROFILE = false;
    protected static final int STATUS_AUTO_CURSOR_DONE = 256;
    protected static final int STATUS_CANDIDATE_FULL = 16;
    protected static final int STATUS_INIT = 0;
    protected static final int STATUS_INPUT = 1;
    protected static final int STATUS_INPUT_EDIT = 3;
    private static final String STRING_ALLOWDECOEMOJI = "allowDecoEmoji";
    private static final String STRING_CIRCLE = "○";
    private static final String STRING_EMOJITYPE = "emojiType";
    protected static final String STRING_HALF_WIDTH_SPACE = " ";
    protected static final String STRING_KANA_INPUT = "Kana";
    private static final String STRING_MUSHROOM_NOT_USE = "notuse";
    private static final String TAG = "IWnnImeBase";
    protected CandidatesManager mCandidatesManager;
    protected IWnnSymbolEngine mConverterSymbolEngineBack;
    protected DefaultSoftKeyboard mDefaultSoftKeyboard;
    protected boolean mHasContinuedPrediction;
    protected boolean mHasUsedVoiceInput;
    protected int mLanguageType;
    protected LetterConverter mPreConverterBack;
    protected RomkanFullKatakana mPreConverterFullKatakana;
    protected RomkanHalfKatakana mPreConverterHalfKatakana;
    protected Romkan mPreConverterHiragana;
    protected Directkan mPreConverterHiraganaDirect;
    protected boolean mRecognizing;
    protected CharacterStyle mSpanToggleCharacterBgHl;
    protected CharacterStyle mSpanToggleCharacterText;
    protected IWnnIME mWnn;
    protected static final CharacterStyle SPAN_CONVERT_BGCOLOR_HL = new BackgroundColorSpan(-9512705);
    protected static final CharacterStyle SPAN_EXACT_BGCOLOR_HL = new BackgroundColorSpan(-10039894);
    protected static final CharacterStyle SPAN_EISUKANA_BGCOLOR_HL = new BackgroundColorSpan(-6310195);
    protected static final CharacterStyle SPAN_REMAIN_BGCOLOR_HL = new BackgroundColorSpan(-983041);
    protected static final CharacterStyle SPAN_TEXTCOLOR = new ForegroundColorSpan(IWnnImeEvent.PRIVATE_EVENT_OFFSET);
    protected static final CharacterStyle SPAN_UNDERLINE = new UnderlineSpan();
    private static final HashMap<Integer, Integer> SYMBOLLIST_MODE_REPLACE_TABLE = new HashMap<Integer, Integer>() {
        {
            put(3, Integer.valueOf(IWnnImeBase.ENGINE_MODE_SYMBOL_EMOJI));
            put(1, Integer.valueOf(IWnnImeBase.ENGINE_MODE_SYMBOL_SYMBOL));
            put(2, Integer.valueOf(IWnnImeBase.ENGINE_MODE_SYMBOL_KAO_MOJI));
            put(6, Integer.valueOf(IWnnImeBase.ENGINE_MODE_SYMBOL_DECOEMOJI));
            put(7, Integer.valueOf(IWnnImeBase.ENGINE_MODE_SYMBOL_ADD_SYMBOL));
        }
    };
    private static final ArrayList<String> PARENTHESIS_PAIR_LIST = new ArrayList<String>() {
        {
            add("()");
            add("[]");
            add("{}");
            add("«»");
            add("“”");
            add("‘’");
            add("””");
            add("’’");
            add("„“");
            add("‚‘");
            add("„”");
            add("‛’");
            add("‹›");
            add("❨❩");
            add("❪❫");
            add("❬❭");
            add("❮❯");
            add("❰❱");
            add("❲❳");
            add("❴❵");
            add("⦅⦆");
            add("〈〉");
            add("《》");
            add("「」");
            add("『』");
            add("【】");
            add("〔〕");
            add("〖〗");
            add("〘〙");
            add("〝〞");
            add("〝〟");
            add("﴾﴿");
            add("︗︘");
            add("︵︶");
            add("︷︸");
            add("︹︺");
            add("︻︼");
            add("︽︾");
            add("︿﹀");
            add("﹁﹂");
            add("﹃﹄");
            add("﹙﹚");
            add("﹛﹜");
            add("﹝﹞");
            add("（）");
            add("［］");
            add("｛｝");
            add("｢｣");
        }
    };
    protected EngineState mEngineState = new EngineState();
    protected boolean mHasCommitedByVoiceInput = false;
    protected boolean mIsGetAllText = false;
    protected boolean mIsGetTextType = false;
    protected int mFunfun = 0;
    protected char mLastToggleCharTypeNull = 0;
    protected CharSequence mMushroomResultString = null;
    protected boolean mMushroomResulttype = false;
    protected int mMushroomSelectionStart = 0;
    protected int mMushroomSelectionEnd = 0;
    protected KeyboardManager mKeyboardManager = null;
    protected ComposingText mComposingText = new ComposingText();
    protected EditorInfo mAttribute = null;
    protected int mTargetLayer = 1;
    protected int mStatus = 0;
    protected boolean mEnableSymbolList = true;
    protected boolean mHardKeyboardHidden = true;
    protected boolean mFullCandidate = false;
    protected SpannableStringBuilder mDisplayText = new SpannableStringBuilder();
    protected StringBuffer mPrevCommitText = new StringBuffer();
    protected ArrayList<StrSegment> mCommitLayer1StrSegment = new ArrayList<>();
    protected Pattern mEnglishAutoCommitDelimiter = null;
    protected boolean mHasStartedTextSelection = true;
    protected boolean mIgnoreCursorMove = false;
    protected boolean mEnableAutoDeleteSpace = false;
    protected int mCommitStartCursor = 0;
    protected int mComposingStartCursor = 0;
    protected boolean mEnableHardware12Keyboard = false;
    protected int mRotation = -1;
    protected boolean mExactMatchMode = false;
    protected boolean mEnableFunfun = true;
    protected boolean mEnableLearning = true;
    protected boolean mEnablePrediction = true;
    protected boolean mEnableConverter = true;
    protected boolean mEnableSpellCorrection = true;
    private boolean mEnableParenthesisCursorMovement = true;
    protected int mCommitCount = 0;
    protected boolean mAutoCaps = false;
    protected boolean mEnableMushroom = false;
    protected boolean mEnableAutoInsertSpace = true;
    protected int mDisableAutoCommitEnglishMask = 0;
    protected WnnEngine mConverterBack = null;
    protected LetterConverter mPreConverter = null;
    protected boolean mEnableHeadConv = false;
    protected boolean mNonVisiblePassWord = false;
    private boolean mIsAfterCommit = false;
    protected boolean mEnableHalfSpace = false;
    protected boolean mDirectKana = false;
    protected int mAutoCursorMovementSpeed = 0;
    protected LetterConverter mPreConverterKanaRoman = null;
    protected LetterConverter mPreConverterKanaDirect = null;
    private String mCommitPredictKey = null;
    private int mCommitFunfun = 0;
    private int mCommitCursorPosition = 0;
    private boolean mCommitExactMatch = false;
    private int mCommitConvertType = 0;
    private boolean mCanUndo = false;
    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (IWnnIME.isPerformanceDebugging()) {
                Log.d(IWnnImeBase.TAG, "handel Message Start");
            }
            IWnnImeBase.this.onHandleMessage(msg.what);
            if (IWnnIME.isPerformanceDebugging()) {
                Log.d(IWnnImeBase.TAG, "handel Message End");
            }
        }
    };
    protected iWnnEngine mConverterIWnn = iWnnEngine.getEngine();

    public static class EngineState {
        public static final int CONVERT_TYPE_EISU_KANA = 2;
        public static final int CONVERT_TYPE_NONE = 0;
        public static final int CONVERT_TYPE_RENBUN = 1;
        public static final int INVALID = -1;
        public static final int KEYBOARD_12KEY = 2;
        public static final int KEYBOARD_50KEY = 3;
        public static final int KEYBOARD_QWERTY = 1;
        public static final int KEYBOARD_UNDEF = 0;
        public static final int LANGUAGE_DEFAULT = 2;
        public static final int LANGUAGE_EN = 1;
        public static final int LANGUAGE_HANGUL = 4;
        public static final int LANGUAGE_JP = 0;
        public static final int LANGUAGE_RUSSIAN = 3;
        public static final int LANGUAGE_ZH = 5;
        public static final int PREFERENCE_DICTIONARY_EMAIL_ADDRESS_URI = 3;
        public static final int PREFERENCE_DICTIONARY_NONE = 0;
        public static final int PREFERENCE_DICTIONARY_PERSON_NAME = 1;
        public static final int PREFERENCE_DICTIONARY_POSTAL_ADDRESS = 2;
        public static final int TEMPORARY_DICTIONARY_MODE_NONE = 0;
        public static final int TEMPORARY_DICTIONARY_MODE_SYMBOL = 1;
        public static final int TEMPORARY_DICTIONARY_MODE_USER = 2;
        public int language = -1;
        public int convertType = -1;
        public int temporaryMode = -1;
        public int preferenceDictionary = -1;
        public int keyboard = -1;

        public boolean isEisuKana() {
            return this.convertType == 2;
        }

        public boolean isConvertState() {
            return (this.convertType == 0 || this.convertType == -1) ? false : true;
        }

        public boolean isSymbolList() {
            return this.temporaryMode == 1;
        }

        public boolean isEnglish() {
            return this.language == 1;
        }

        public boolean isHangul() {
            return this.language == 4;
        }
    }

    public IWnnImeBase(IWnnIME wnn, int languageType) {
        this.mWnn = null;
        this.mSpanToggleCharacterBgHl = null;
        this.mSpanToggleCharacterText = null;
        this.mDefaultSoftKeyboard = null;
        this.mCandidatesManager = null;
        this.mLanguageType = -1;
        this.mWnn = wnn;
        this.mLanguageType = languageType;
        setConverter(this.mConverterIWnn);
        Resources res = getResources();
        if (res != null) {
            this.mSpanToggleCharacterBgHl = new BackgroundColorSpan(res.getInteger(R.integer.span_color_toggle_character_bg_hl));
            this.mSpanToggleCharacterText = new ForegroundColorSpan(res.getInteger(R.integer.span_color_toggle_character_text));
        }
        CandidatesManager tempManager = getCandidatesManager();
        if (tempManager == null) {
            this.mCandidatesManager = new CandidatesManager(this.mWnn);
            setCandidatesManager(this.mCandidatesManager);
        } else {
            this.mCandidatesManager = tempManager;
        }
        setComposingText(this.mComposingText);
        this.mDefaultSoftKeyboard = createDefaultSoftKeyboard();
        setCurrentDefaultSoftKeyboard(this.mDefaultSoftKeyboard);
        setAutoHideMode(false);
        this.mDefaultSoftKeyboard.resetCurrentKeyboard();
    }

    public void onCreate() {
        Resources res = getResources();
        if (res != null) {
            String delimiter = Pattern.quote(res.getString(R.string.ti_en_word_separators_txt));
            this.mEnglishAutoCommitDelimiter = Pattern.compile(".*[" + delimiter + "]$", 32);
        }
        this.mKeyboardManager = this.mWnn.getCurrentKeyboardManager();
    }

    public void onCreateInputView() {
        Resources res = getResources();
        if (res != null) {
            int hiddenState = res.getConfiguration().hardKeyboardHidden;
            this.mHardKeyboardHidden = hiddenState == 2;
        }
        this.mDefaultSoftKeyboard.setHardKeyboardHidden(this.mHardKeyboardHidden);
        this.mCandidatesManager.setHardKeyboardHidden(this.mHardKeyboardHidden);
    }

    public void onStartInput(EditorInfo attribute, boolean restarting) {
        MushroomControl mush = MushroomControl.getInstance();
        CharSequence result = mush.getResultString();
        Boolean type = mush.getResultType();
        if (result != null) {
            this.mMushroomResultString = result;
            this.mMushroomResulttype = type.booleanValue();
            if (this.mWnn != null) {
                this.mWnn.showSoftInputFromInputMethod(false);
            }
        }
        if (attribute.initialSelStart != attribute.initialSelEnd) {
            this.mHasStartedTextSelection = true;
        }
    }

    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        if (this.mWnn != null) {
            this.mAttribute = attribute;
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
            if (this.mWnn.isKeepInput() || this.mWnn.isSubtypeEmojiInput()) {
                this.mWnn.startInputView(attribute);
            } else {
                this.mIgnoreCursorMove = false;
                clearCommitInfo();
                this.mComposingText.clear();
                initializeScreen();
                this.mDefaultSoftKeyboard.resetCurrentKeyboard();
                this.mWnn.startInputView(attribute);
                setCandidateIsViewTypeFull(false);
                this.mCandidatesManager.clearCandidates();
                InputConnection inputConnection = getCurrentInputConnection();
                if (inputConnection != null) {
                    inputConnection.finishComposingText();
                }
            }
            if (this.mPreConverter != null) {
                this.mPreConverter.setPreferences(pref);
            }
            fitInputType(pref, attribute);
            updateFullscreenMode();
            if (isEnableL2Converter()) {
                breakSequence();
            }
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5), 100L);
            if (this.mKeyboardManager != null) {
                if (this.mKeyboardManager.isInitialized()) {
                    this.mKeyboardManager.showKeyboard();
                    setSubTypeEmojiInput();
                } else {
                    this.mKeyboardManager.initKeyboard();
                }
            }
            this.mIsAfterCommit = false;
        }
    }

    public boolean onConfigurationChanged(Configuration newConfig) {
        if (this.mWnn == null) {
            return false;
        }
        boolean ret = false;
        boolean candidateShown = false;
        try {
            WnnEngine converter = getConverter();
            if (converter != null && converter.hasCandidate()) {
                candidateShown = true;
            }
            InputConnection inputConnection = getCurrentInputConnection();
            if (inputConnection != null) {
                WindowManager wm = (WindowManager) this.mWnn.getSystemService("window");
                if (wm == null) {
                    return false;
                }
                boolean onRotation = false;
                int newRotation = wm.getDefaultDisplay().getRotation();
                if (this.mRotation != newRotation) {
                    this.mRotation = newRotation;
                    onRotation = true;
                    if (this.mKeyboardManager != null) {
                        this.mKeyboardManager.removeMessages();
                    }
                    if (this.mComposingText.size(1) + this.mFunfun > 0 || candidateShown) {
                        this.mWnn.setStateOfKeepInput(true);
                    }
                }
                int hiddenState = newConfig.hardKeyboardHidden;
                boolean hidden = hiddenState == 2;
                boolean type12Key = newConfig.keyboard == 3;
                boolean changeStaHardware = false;
                if (this instanceof IWnnImeJaJp) {
                    if (this.mEnableHardware12Keyboard != type12Key) {
                        changeStaHardware = true;
                    }
                    this.mEnableHardware12Keyboard = type12Key;
                } else if (this.mHardKeyboardHidden != hidden) {
                    changeStaHardware = true;
                }
                this.mHardKeyboardHidden = hidden;
                if (changeStaHardware) {
                    if (this.mRecognizing) {
                        this.mRecognizing = false;
                    }
                    commitConvertingText();
                    commitText(false);
                    this.mFunfun = 0;
                    setFunFun(this.mFunfun);
                    initializeScreen();
                    updateViewStatus(this.mTargetLayer, false, true);
                }
                this.mWnn.onConfigurationChangedIWnnIME(newConfig);
                if (isInputViewShown()) {
                    boolean requestCandidate = !onRotation || candidateShown;
                    if (requestCandidate && (this instanceof IWnnImeJaJp) && converter != null && (converter instanceof IWnnSymbolEngine)) {
                        IWnnSymbolEngine symbolEngine = (IWnnSymbolEngine) converter;
                        if (symbolEngine.getMode() == 6) {
                            symbolEngine.setMode(6);
                        }
                    }
                    updateViewStatus(this.mTargetLayer, requestCandidate, true);
                }
                this.mDefaultSoftKeyboard.setHardKeyboardHidden(this.mHardKeyboardHidden);
                this.mCandidatesManager.setHardKeyboardHidden(this.mHardKeyboardHidden);
                ret = true;
            } else {
                this.mWnn.onConfigurationChangedIWnnIME(newConfig);
            }
        } catch (Exception ex) {
            Log.e(TAG, "onConfigurationChanged() " + ex.toString());
        }
        this.mWnn.setStateOfKeepInput(false);
        return ret;
    }

    public void hideWindow() {
        this.mCandidatesManager.cancelCreateCandidates();
        BaseInputView baseInputView = (BaseInputView) this.mDefaultSoftKeyboard.getCurrentView();
        if (baseInputView != null) {
            baseInputView.closeDialog();
        }
        this.mDefaultSoftKeyboard.closing();
        if (this.mConverterSymbolEngineBack != null) {
            this.mConverterSymbolEngineBack.close();
        }
        this.mComposingText.clear();
        clearCommitInfo();
        closeInputView();
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 0L);
    }

    public void closeInputView() {
        if (this.mWnn != null) {
            this.mWnn.closeInputView();
        }
    }

    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        CharSequence composingString;
        InputConnection inputConnection = getCurrentInputConnection();
        if (this.mEnableParenthesisCursorMovement && this.mIsAfterCommit && inputConnection != null && PARENTHESIS_PAIR_LIST.contains(inputConnection.getTextBeforeCursor(2, 0))) {
            inputConnection.setSelection(newSelEnd - 1, newSelEnd - 1);
        }
        this.mIsAfterCommit = false;
        this.mComposingStartCursor = candidatesStart < 0 ? newSelEnd : candidatesStart;
        boolean prevSelection = this.mHasStartedTextSelection;
        if (newSelStart != newSelEnd) {
            clearCommitInfo();
            this.mHasStartedTextSelection = true;
        } else {
            this.mHasStartedTextSelection = false;
        }
        if (this.mHasContinuedPrediction) {
            this.mHasContinuedPrediction = false;
            this.mIgnoreCursorMove = false;
            return;
        }
        if (!this.mEngineState.isSymbolList()) {
            boolean isNotComposing = candidatesStart < 0 && candidatesEnd < 0;
            if (this.mComposingText.size(1) + this.mFunfun != 0 && !isNotComposing) {
                if (this.mHasStartedTextSelection) {
                    if (inputConnection != null) {
                        if (this.mFunfun > 0) {
                            if (this instanceof IWnnImeJaJp) {
                                composingString = convertComposingToCommitText(this.mComposingText, this.mTargetLayer);
                            } else {
                                composingString = this.mComposingText.toString(this.mTargetLayer);
                            }
                            inputConnection.setComposingText(composingString, 1);
                            int funfunStart = Math.max(candidatesEnd - this.mFunfun, 0);
                            int selStart = newSelStart;
                            int selEnd = newSelEnd;
                            if (funfunStart < newSelStart) {
                                selStart = Math.max(newSelStart - this.mFunfun, funfunStart);
                            }
                            if (funfunStart < newSelEnd) {
                                selEnd = Math.max(newSelEnd - this.mFunfun, funfunStart);
                            }
                            inputConnection.setSelection(selStart, selEnd);
                            this.mFunfun = 0;
                            setFunFun(this.mFunfun);
                        }
                        inputConnection.finishComposingText();
                    }
                    this.mComposingText.clear();
                    initializeScreen();
                } else {
                    updateViewStatus(this.mTargetLayer, false, true);
                }
                this.mIgnoreCursorMove = false;
                return;
            }
            if (this.mIgnoreCursorMove) {
                this.mIgnoreCursorMove = false;
                return;
            }
            int commitEnd = this.mCommitStartCursor;
            if (this.mPrevCommitText != null) {
                commitEnd += this.mPrevCommitText.length();
            }
            if (((newSelEnd < oldSelEnd || commitEnd < newSelEnd) && clearCommitInfo()) || isNotComposing) {
                if (isEnableL2Converter()) {
                    breakSequence();
                }
                if (inputConnection != null && isNotComposing && this.mComposingText.size(1) != 0) {
                    inputConnection.finishComposingText();
                }
                if (prevSelection != this.mHasStartedTextSelection || !this.mHasStartedTextSelection) {
                    this.mComposingText.clear();
                    initializeScreen();
                }
            }
        }
    }

    public synchronized boolean onEvent(IWnnImeEvent ev) {
        boolean ret;
        if (ev == null) {
            ret = false;
        } else {
            ret = false;
            if ((this.mFullCandidate && (this.mStatus & 16) != 0) || isRightOrLeftKeyEvents(ev) || ev.code == -268435417) {
                ret = onHandleEvent(ev);
            } else {
                InputConnection inputConnection = getCurrentInputConnection();
                if (inputConnection != null) {
                    inputConnection.beginBatchEdit();
                    ret = onHandleEvent(ev);
                    inputConnection.endBatchEdit();
                }
            }
        }
        return ret;
    }

    private boolean onHandleEvent(IWnnImeEvent ev) {
        int errorCodeResourceId;
        boolean isJaJp = this instanceof IWnnImeJaJp;
        switch (ev.code) {
            case IWnnImeEvent.CHANGE_MODE:
                changeEngineMode(ev.mode);
                if (ev.mode == 104 || ev.mode == 103 || ev.mode == 107 || ev.mode == 108 || ev.mode == 110 || ev.mode == 109 || ev.mode == 111 || this.mWnn.isKeepInput()) {
                    return true;
                }
                initializeScreen();
                return true;
            case IWnnImeEvent.UPDATE_CANDIDATE:
                if (isRenbun()) {
                    this.mComposingText.setCursor(1, this.mComposingText.toString(1).length());
                    this.mExactMatchMode = false;
                    this.mFunfun = 0;
                    setFunFun(this.mFunfun);
                    updateViewStatusForPrediction(true, true);
                    return true;
                }
                updateViewStatus(this.mTargetLayer, true, true);
                return true;
            case IWnnImeEvent.UNDO:
                boolean ret = undo();
                return ret;
            case IWnnImeEvent.CHANGE_INPUT_VIEW:
                changeInputCandidatesView(false);
                return true;
            case IWnnImeEvent.CHANGE_INPUT_CANDIDATE_VIEW:
                changeInputCandidatesView(true);
                return true;
            case IWnnImeEvent.KEYUP:
                onKeyUpEvent(ev);
                return true;
            case IWnnImeEvent.TOUCH_OTHER_KEY:
                this.mStatus |= 3;
                return true;
            case IWnnImeEvent.KEYLONGPRESS:
                boolean ret2 = onKeyLongPressEvent(ev.keyEvent);
                return ret2;
            case IWnnImeEvent.VOICE_INPUT:
                startShortcutIME();
                return true;
            case IWnnImeEvent.CHANGE_FLOATING:
                commitAllText();
                clearCommitInfo();
                initializeScreen();
                return true;
            case IWnnImeEvent.INIT_CONVERTER:
                initConverter();
                return true;
            case IWnnImeEvent.SPACE_KEY:
                processKeyboardSpaceKey();
                this.mEnableAutoDeleteSpace = false;
                return true;
            case IWnnImeEvent.START_SYMBOL_MODE:
                commitAllText();
                changeEngineMode(getPrioritySymbollist());
                return true;
            case IWnnImeEvent.FIT_INPUT_TYPE:
                WnnEngine converter = getConverter();
                boolean candidateShown = false;
                if (converter != null && converter.hasCandidate()) {
                    candidateShown = true;
                    this.mWnn.setStateOfKeepInput(true);
                }
                fitInputType(PreferenceManager.getDefaultSharedPreferences(this.mWnn), this.mAttribute);
                if (!candidateShown) {
                    return true;
                }
                if (this.mKeyboardManager != null) {
                    this.mKeyboardManager.setCandidatesViewShown(true);
                }
                this.mWnn.setStateOfKeepInput(false);
                return true;
            case IWnnImeEvent.INIT_KEYBOARD_DONE:
                if (this.mKeyboardManager == null) {
                    return true;
                }
                this.mKeyboardManager.showKeyboard();
                setSubTypeEmojiInput();
                return true;
            case IWnnImeEvent.CANCEL_WEBAPI:
                if (isJaJp) {
                    this.mConverterIWnn.onDoneGettingCandidates();
                    return true;
                }
                break;
            case IWnnImeEvent.TIMEOUT_WEBAPI:
                if (isJaJp) {
                    boolean success = this.mConverterIWnn.isWebApiSuccessReceived();
                    if (success) {
                        errorCodeResourceId = R.string.ti_webapi_timeout_error_received_display_txt;
                    } else {
                        errorCodeResourceId = R.string.ti_webapi_timeout_error_txt;
                    }
                    Resources res = getResources();
                    if (res != null) {
                        this.mCandidatesManager.onWebApiError(res.getString(errorCodeResourceId));
                    }
                    if (!success) {
                        return true;
                    }
                    onEvent(new IWnnImeEvent(IWnnImeEvent.RESULT_WEBAPI_OK));
                    this.mConverterIWnn.onDoneGettingCandidates();
                    return true;
                }
                break;
            case IWnnImeEvent.FOCUS_CANDIDATE_START:
                this.mDefaultSoftKeyboard.setOkToEnterKey();
                return true;
            case IWnnImeEvent.FOCUS_CANDIDATE_END:
                this.mDefaultSoftKeyboard.onUpdateState();
                return true;
            case IWnnImeEvent.RECEIVE_DECOEMOJI:
                if (isJaJp) {
                    if (this.mConverterIWnn.getLanguage() != -1 && !this.mHandler.hasMessages(3)) {
                        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(3), 0L);
                    }
                    if (this.mConverterIWnn.getLanguage() != 0 || this.mHandler.hasMessages(4)) {
                        return true;
                    }
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4), 0L);
                    return true;
                }
                break;
        }
        if (isDirectInputMode()) {
            boolean ret3 = handleKeyEventForDirectInputMode(ev);
            return ret3;
        }
        boolean ret4 = handleKeyEvent(ev);
        return ret4;
    }

    protected boolean handleKeyEventForDirectInputMode(IWnnImeEvent ev) {
        if (ev == null) {
            return false;
        }
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            if (this.mHandler.hasMessages(1) && ev.code != -268435444) {
                this.mHandler.removeMessages(1);
                sendKeyChar(this.mLastToggleCharTypeNull);
                this.mLastToggleCharTypeNull = (char) 0;
            }
            KeyEvent keyEvent = ev.keyEvent;
            switch (ev.code) {
                case IWnnImeEvent.INPUT_CHAR:
                    if (ev.chars[0] >= '0' && ev.chars[0] <= '9') {
                        commitTextToInputConnection(String.valueOf(ev.chars[0]), null);
                    } else {
                        sendKeyChar(ev.chars[0]);
                    }
                    return true;
                case IWnnImeEvent.TOGGLE_CHAR:
                    String[] table = ev.toggleTable;
                    if (this.mLastToggleCharTypeNull == 0) {
                        this.mLastToggleCharTypeNull = table[0].charAt(0);
                    } else {
                        this.mLastToggleCharTypeNull = this.mLastToggleCharTypeNull == table[0].charAt(0) ? table[1].charAt(0) : table[0].charAt(0);
                    }
                    this.mHandler.removeMessages(1);
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), 500L);
                    break;
                case IWnnImeEvent.INPUT_SOFT_KEY:
                    sendKeyEventDirect(keyEvent);
                    return true;
                default:
                    int keyCode = 0;
                    if (keyEvent != null) {
                        keyCode = keyEvent.getKeyCode();
                    }
                    switch (keyCode) {
                        case 4:
                            if (isInputViewShown()) {
                                this.mDefaultSoftKeyboard.closing();
                                requestHideSelf(0);
                                return true;
                            }
                            break;
                        case 62:
                            if (!isInputViewShown() && keyEvent.isShiftPressed()) {
                                return this.mWnn.showSoftInputFromInputMethod(true);
                            }
                            break;
                    }
                    break;
            }
        }
        return false;
    }

    protected boolean handleKeyEvent(IWnnImeEvent ev) {
        if (ev == null || this.mWnn == null) {
            return false;
        }
        boolean ret = false;
        KeyEvent keyEvent = ev.keyEvent;
        int keyCode = 0;
        if (keyEvent != null) {
            keyCode = keyEvent.getKeyCode();
        }
        if (isThroughKeyCodeForSubTypeEmoji(keyCode, keyEvent)) {
            return false;
        }
        if (this.mEngineState.isSymbolList() && (!this.mEnableHardware12Keyboard || (keyCode != 67 && keyCode != 1))) {
            if (keyEvent != null && keyEvent.isPrintingKey() && isTenKeyCode(keyCode) && !keyEvent.isNumLockOn()) {
                return false;
            }
            switch (keyCode) {
                case 4:
                case ENGINE_MODE_SYMBOL_ADD_SYMBOL:
                    handleBackEventForSymbolList();
                    break;
                case 17:
                case 18:
                    if (this.mEnableHardware12Keyboard) {
                    }
                    if (ev.code == -268435449 && keyCode != 84 && keyCode != 57 && keyCode != 58 && keyCode != 59 && keyCode != 60) {
                        EngineState state = new EngineState();
                        state.temporaryMode = 0;
                        updateEngineState(state);
                    }
                    break;
                case 19:
                    if (this.mCandidatesManager.isFocusCandidate()) {
                        processMoveKeyEvent(keyCode);
                    }
                    break;
                case 20:
                    processMoveKeyEvent(keyCode);
                    break;
                case 21:
                    if (this.mCandidatesManager.isFocusCandidate()) {
                        processLeftKeyEvent();
                    }
                    break;
                case 22:
                    if (this.mCandidatesManager.isFocusCandidate()) {
                        processRightKeyEvent();
                    }
                    break;
                case 23:
                case 66:
                case 160:
                    if (this.mCandidatesManager.isFocusCandidate()) {
                        this.mCandidatesManager.selectFocusCandidate();
                    }
                    break;
                case 61:
                    if (keyEvent != null && keyEvent.getRepeatCount() == 0 && !keyEvent.isAltPressed() && !keyEvent.isCtrlPressed()) {
                        this.mCandidatesManager.changePosition(keyEvent.isShiftPressed() ? false : true);
                        break;
                    }
                    break;
                case 62:
                    if (keyEvent.isAltPressed()) {
                        if (keyEvent.getRepeatCount() == 0) {
                            switchSymbolList();
                        }
                    } else {
                        commitText(STRING_HALF_WIDTH_SPACE);
                    }
                    break;
                case 63:
                    switchSymbolList();
                    break;
                case 67:
                case ENGINE_MODE_OPT_TYPE_50KEY:
                    break;
                case 82:
                    break;
                case 92:
                    this.mCandidatesManager.scrollPageAndUpdateFocus(false);
                    break;
                case 93:
                    this.mCandidatesManager.scrollPageAndUpdateFocus(true);
                    break;
                case 94:
                    if (keyEvent != null && keyEvent.getRepeatCount() == 0) {
                        switchSymbolList();
                        break;
                    }
                    break;
                case 95:
                    if (processSwitchCharset(true)) {
                    }
                    if (ev.code == -268435449) {
                        EngineState state2 = new EngineState();
                        state2.temporaryMode = 0;
                        updateEngineState(state2);
                    }
                    break;
                case 122:
                case 123:
                    if (this.mCandidatesManager.isFocusCandidate()) {
                        processMoveKeyEvent(keyCode);
                    }
                    break;
                default:
                    if (ev.code == -268435449) {
                    }
                    break;
            }
            return false;
        }
        if (ev.code == -268435449 && processHardware12Keyboard(keyEvent)) {
            return true;
        }
        if (ev.code == -268435452) {
            setCandidateIsViewTypeFull(true);
            return true;
        }
        if (ev.code == -268435453) {
            setCandidateIsViewTypeFull(false);
            return true;
        }
        if (ev.code != -268435429 && ev.code != -268435440 && (keyEvent == null || (keyCode != 59 && keyCode != 60 && keyCode != 57 && keyCode != 58 && ((!keyEvent.isShiftPressed() || (keyCode != 67 && keyCode != 112 && keyCode != 62)) && (!keyEvent.isAltPressed() || keyCode != 62))))) {
            clearCommitInfo();
        }
        switch (ev.code) {
            case IWnnImeEvent.TOGGLE_REVERSE_CHAR:
                ret = processToggleReverseChar(ev);
                break;
            case IWnnImeEvent.CONVERT:
                ret = processConvert();
                break;
            case IWnnImeEvent.INPUT_CHAR:
                ret = processInputChar(ev);
                break;
            case IWnnImeEvent.INPUT_KEY:
                if (keyEvent != null) {
                    switch (keyCode) {
                        case 21:
                        case 22:
                            if (!this.mCandidatesManager.isFocusCandidate()) {
                                this.mDefaultSoftKeyboard.onUpdateState();
                            }
                            ret = processKeyEvent(ev);
                            break;
                        case iWnnEngine.SetType.DICTIONARY_TYPE_MAX:
                        case 58:
                        case 59:
                        case 60:
                            break;
                        case 92:
                            if (this.mCandidatesManager.hasCandidates()) {
                                if (this.mFullCandidate || this.mWnn.isHwCandWindow()) {
                                    this.mCandidatesManager.scrollPageAndUpdateFocus(false);
                                } else if (this.mCandidatesManager.getCanReadMore()) {
                                    onEvent(new IWnnImeEvent(IWnnImeEvent.LIST_CANDIDATES_FULL));
                                }
                            } else if (getConvertingForFuncKeyType() == 1) {
                            }
                            break;
                        case 93:
                            if (this.mCandidatesManager.hasCandidates()) {
                                if (this.mFullCandidate || this.mWnn.isHwCandWindow()) {
                                    this.mCandidatesManager.scrollPageAndUpdateFocus(true);
                                } else if (this.mCandidatesManager.getCanReadMore()) {
                                    onEvent(new IWnnImeEvent(IWnnImeEvent.LIST_CANDIDATES_FULL));
                                }
                            } else if (getConvertingForFuncKeyType() == 1) {
                            }
                            break;
                        case 94:
                            onEvent(new IWnnImeEvent(IWnnImeEvent.START_SYMBOL_MODE));
                            break;
                        case 95:
                            if (processSwitchCharset(false)) {
                            }
                            ret = processKeyEvent(ev);
                            break;
                        case 113:
                        case 114:
                            if (this.mComposingText.size(1) + this.mFunfun < 1) {
                            }
                            break;
                        default:
                            ret = processKeyEvent(ev);
                            break;
                    }
                    return false;
                }
                break;
            case IWnnImeEvent.SELECT_CANDIDATE:
                ret = processSelectCandidate(ev);
                break;
            case IWnnImeEvent.TOGGLE_CHAR:
                ret = processToggleChar(ev);
                break;
            case IWnnImeEvent.REPLACE_CHAR:
                ret = processReplaceChar(ev);
                break;
            case IWnnImeEvent.INPUT_SOFT_KEY:
                ret = processKeyEvent(ev);
                if (!ret) {
                    sendKeyEventDirect(keyEvent);
                    ret = true;
                }
                break;
            case IWnnImeEvent.COMMIT_COMPOSING_TEXT:
                commitAllText();
                break;
            case IWnnImeEvent.FLICK_INPUT_CHAR:
                ret = processFlickInputChar(ev);
                break;
            case IWnnImeEvent.TOGGLE_INPUT_CANCEL:
                if (this.mHandler.hasMessages(1)) {
                    this.mHandler.removeMessages(1);
                    processToggleTimeLimit();
                }
                ret = true;
                break;
            case IWnnImeEvent.SELECT_WEBAPI:
                this.mConverterIWnn.startWebAPI(this.mComposingText);
                break;
            case IWnnImeEvent.RESULT_WEBAPI_OK:
                this.mCandidatesManager.addCandidatesWebAPI();
                break;
            case IWnnImeEvent.RESULT_WEBAPI_NG:
                this.mCandidatesManager.onWebApiError(ev.string);
                break;
            case IWnnImeEvent.SELECT_WEBAPI_GET_AGAIN:
                this.mConverterIWnn.startWebAPIGetAgain(this.mComposingText);
                break;
            case IWnnImeEvent.CALL_MUSHROOM:
                callMushRoom(ev.word);
                break;
        }
        return ret;
    }

    protected boolean processSwitchCharset(boolean isSymbol) {
        return false;
    }

    protected boolean processInputChar(IWnnImeEvent ev) {
        if (ev == null) {
            return false;
        }
        this.mFunfun = 0;
        setFunFun(this.mFunfun);
        if (!isEnableL2Converter()) {
            commitText(false);
            commitText(new String(ev.chars));
            this.mCandidatesManager.clearCandidates();
            this.mEnableAutoDeleteSpace = false;
        } else {
            processSoftKeyboardCode(ev.chars);
        }
        return true;
    }

    protected boolean processFlickInputChar(IWnnImeEvent ev) {
        return false;
    }

    protected boolean processToggleChar(IWnnImeEvent ev) {
        String[] table;
        InputConnection inputConnection;
        StrSegment strSegment;
        if (ev == null || (table = ev.toggleTable) == null || (inputConnection = getCurrentInputConnection()) == null) {
            return false;
        }
        if (!isEnableL2Converter()) {
            commitText(false);
        }
        String c = null;
        if ((this.mStatus & (-17)) == 1) {
            String prevChar = null;
            if (this.mComposingText.size(1) > 0) {
                int cursor = this.mComposingText.getCursor(1);
                if (cursor > 0 && (strSegment = this.mComposingText.getStrSegment(1, cursor - 1)) != null) {
                    prevChar = strSegment.string;
                }
            } else {
                prevChar = inputConnection.getTextBeforeCursor(1, 0).toString();
            }
            if (prevChar != null) {
                c = searchToggleCharacter(prevChar, table, true);
            }
        }
        if (c != null) {
            if (this.mComposingText.size(1) > 0) {
                this.mComposingText.delete(1, false);
            } else {
                inputConnection.deleteSurroundingText(1, 0);
                this.mIgnoreCursorMove = true;
            }
        } else {
            c = table[0];
            if (this.mAutoCaps && getShiftKeyState(getCurrentInputEditorInfo()) == 1) {
                c = Character.toString(Character.toUpperCase(c.charAt(0)));
            }
        }
        commitConvertingText();
        appendStrSegment(new StrSegment(c));
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), 500L);
        this.mFunfun = 0;
        setFunFun(this.mFunfun);
        this.mStatus = 1;
        updateViewStatusForPrediction(true, true);
        return true;
    }

    protected boolean processToggleReverseChar(IWnnImeEvent ev) {
        int cursor;
        StrSegment strSegment;
        String prevChar;
        String c;
        if (ev == null) {
            return false;
        }
        String[] table = ev.toggleTable;
        if (table == null) {
            return false;
        }
        if (((this.mStatus & (-17)) != 1 && (this.mStatus & 256) == 0) || this.mEngineState.isConvertState() || (cursor = this.mComposingText.getCursor(1)) <= 0 || (strSegment = this.mComposingText.getStrSegment(1, cursor - 1)) == null || (prevChar = strSegment.string) == null || (c = searchToggleCharacter(prevChar, ev.toggleTable, true)) == null) {
            return false;
        }
        this.mComposingText.delete(1, false);
        if (this instanceof IWnnImeJaJp) {
            appendToggleString(c);
        } else {
            appendStrSegment(new StrSegment(c));
        }
        this.mFunfun = 0;
        setFunFun(this.mFunfun);
        updateViewStatusForPrediction(true, true);
        return true;
    }

    protected boolean processReplaceChar(IWnnImeEvent ev) {
        int cursor;
        StrSegment strSegment;
        String search;
        String c;
        if (ev == null || (cursor = this.mComposingText.getCursor(1)) <= 0 || this.mEngineState.isConvertState() || (strSegment = this.mComposingText.getStrSegment(1, cursor - 1)) == null || (search = strSegment.string) == null || ev.replaceTable == null || (c = (String) ev.replaceTable.get(search)) == null) {
            return false;
        }
        this.mComposingText.delete(1, false);
        appendStrSegment(new StrSegment(c));
        this.mFunfun = 0;
        setFunFun(this.mFunfun);
        updateViewStatusForPrediction(true, true);
        this.mStatus = 3;
        return true;
    }

    protected boolean processSelectCandidate(IWnnImeEvent ev) {
        if (ev == null) {
            return false;
        }
        initCommitInfoForWatchCursor();
        boolean isEnglishPrediction = isEnglishPrediction();
        if ((this instanceof IWnnImeJaJp) && isEnglishPrediction) {
            this.mComposingText.clear();
        }
        this.mStatus = commitText(ev.word);
        if (!this.mEngineState.isSymbolList() && this.mEnableAutoInsertSpace && (isEnglishPrediction || isHangulPrediction())) {
            commitSpaceJustOne();
        }
        checkCommitInfo();
        if (this.mEngineState.isSymbolList()) {
            this.mEnableAutoDeleteSpace = false;
        }
        return true;
    }

    protected boolean processConvert() {
        if (isRenbun()) {
            if (!this.mCandidatesManager.isFocusCandidate()) {
                processMoveKeyEvent(20);
            }
            processRightKeyEvent();
        } else {
            startConvert(1);
        }
        return true;
    }

    protected boolean processKeyEvent(IWnnImeEvent wnnEv) {
        KeyEvent ev;
        KeyboardView keyboardView;
        int charCode;
        if (wnnEv != null && this.mWnn != null && (ev = wnnEv.keyEvent) != null) {
            int key = ev.getKeyCode();
            int tempConvertingForFuncKeyType = getConvertingForFuncKeyType();
            boolean convertingForFuncKey = tempConvertingForFuncKeyType != 1;
            setConvertingForFuncKeyType(1);
            if (convertingForFuncKey) {
                int len = this.mComposingText.size(1) + this.mFunfun;
                if (len > 0) {
                    switch (key) {
                        case 4:
                        case 23:
                        case 66:
                        case 67:
                        case ENGINE_MODE_SYMBOL_ADD_SYMBOL:
                        case 160:
                            break;
                        case 136:
                        case 137:
                        case 138:
                        case 139:
                        case 140:
                        case 213:
                            setConvertingForFuncKeyType(tempConvertingForFuncKeyType);
                            break;
                        case 211:
                            setConvertingForFuncKeyType(tempConvertingForFuncKeyType);
                            return true;
                        default:
                            if (!ev.isPrintingKey()) {
                                setConvertingForFuncKeyType(tempConvertingForFuncKeyType);
                                return !WnnUtility.isThroughKeyCode(ev);
                            }
                            break;
                    }
                } else {
                    convertingForFuncKey = false;
                }
            }
            if (211 == key) {
                if (isInputViewShown()) {
                    if (ev.isAltPressed()) {
                        switchCharset();
                        return true;
                    }
                    if (ev.getRepeatCount() != 0) {
                        return true;
                    }
                    this.mDefaultSoftKeyboard.closing();
                    requestHideSelf(0);
                    return true;
                }
                if (ev.getRepeatCount() == 0) {
                    return this.mWnn.showSoftInputFromInputMethod(true);
                }
                return true;
            }
            if (ev.isPrintingKey()) {
                if (convertingForFuncKey) {
                    setConvertingForFuncKeyType(tempConvertingForFuncKeyType);
                }
                if (isTenKeyCode(key) && !ev.isNumLockOn()) {
                    return false;
                }
                if (ev.isCtrlPressed() && (key == 29 || key == 34 || key == 31 || key == 50 || key == 52 || key == 53 || key == 54)) {
                    return convertingForFuncKey || this.mComposingText.size(1) + this.mFunfun >= 1;
                }
                if ((ev.isAltPressed() && ev.isShiftPressed() && ((charCode = ev.getUnicodeChar(3)) == 0 || (Integer.MIN_VALUE & charCode) != 0 || charCode == PRIVATE_AREA_CODE)) || processPrintingKey(ev, convertingForFuncKey)) {
                    return true;
                }
            } else {
                if (key == 62) {
                    processHardwareKeyboardSpaceKey(ev);
                    return true;
                }
                if (key == 82 && this.mEnableMushroom && ev.isAltPressed()) {
                    callMushRoom(null);
                    return true;
                }
                if (key == 63) {
                    onEvent(new IWnnImeEvent(IWnnImeEvent.START_SYMBOL_MODE));
                    return true;
                }
                if (key == 204) {
                    if (this instanceof IWnnImeJaJp) {
                        return processLanguageSwitch();
                    }
                } else if (key == 212) {
                    if (processEisu()) {
                        return true;
                    }
                } else if (key == 218) {
                    if (processKana()) {
                        return true;
                    }
                } else if (key == 215) {
                    if (processKatakanaHiragana(ev)) {
                        return true;
                    }
                } else if (key == 214) {
                    if (processHenkan(ev)) {
                        return true;
                    }
                } else if (key == 213 && processMuhenkan(ev)) {
                    return true;
                }
            }
            if (key == 4 && (keyboardView = this.mDefaultSoftKeyboard.getKeyboardView()) != null && keyboardView.handleBack()) {
                return true;
            }
            WnnEngine converter = getConverter();
            if (this.mComposingText.size(1) + this.mFunfun > 0) {
                switch (key) {
                    case 4:
                    case ENGINE_MODE_SYMBOL_ADD_SYMBOL:
                        if (this.mCandidatesManager.getViewType() == 1) {
                            setCandidateIsViewTypeFull(false);
                        } else if (!this.mEngineState.isConvertState() && !convertingForFuncKey) {
                            initializeScreen();
                            if (converter != null) {
                                converter.init(this.mWnn.getFilesDirPath());
                            }
                        } else {
                            this.mCandidatesManager.clearCandidates();
                            this.mStatus = 3;
                            this.mFunfun = 0;
                            setFunFun(this.mFunfun);
                            this.mExactMatchMode = false;
                            this.mComposingText.setCursor(1, this.mComposingText.toString(1).length());
                            updateViewStatusForPrediction(true, true);
                        }
                        break;
                    case 19:
                        if (this.mCandidatesManager.isFocusCandidate()) {
                            processMoveKeyEvent(key);
                        }
                        break;
                    case 20:
                        processMoveKeyEvent(key);
                        break;
                    case 21:
                        break;
                    case 22:
                        if (!isEnableL2Converter()) {
                            if (this.mEngineState.keyboard == 2 || this.mEngineState.keyboard == 3) {
                                commitText(false);
                            }
                        } else {
                            processRightKeyEvent();
                        }
                        break;
                    case 23:
                    case 66:
                    case 160:
                        if (this.mCandidatesManager.isFocusCandidate()) {
                            this.mCandidatesManager.selectFocusCandidate();
                        } else {
                            if (!(this instanceof IWnnImeJaJp) || !isEnglishPrediction()) {
                                int cursor = this.mComposingText.getCursor(1) + this.mFunfun;
                                if (cursor < 1) {
                                }
                            }
                            initCommitInfoForWatchCursor();
                            this.mStatus = commitText(true);
                            checkCommitInfo();
                            if (isEnglishPrediction()) {
                                initializeScreen();
                            }
                            if (this.mFunfun > 0) {
                                initializeScreen();
                                breakSequence();
                            }
                            if (this instanceof IWnnImeJaJp) {
                                this.mEnableAutoDeleteSpace = false;
                            }
                        }
                        break;
                    case 61:
                        processTabKeyEvent(ev);
                        break;
                    case 67:
                    case ENGINE_MODE_OPT_TYPE_50KEY:
                        if (!processDelKeyEventForUndo(ev)) {
                            this.mStatus = 3;
                            if (this.mEngineState.isConvertState() || convertingForFuncKey) {
                                this.mComposingText.setCursor(1, this.mComposingText.toString(1).length());
                                this.mExactMatchMode = false;
                                this.mFunfun = 0;
                                setFunFun(this.mFunfun);
                            } else if (this.mFunfun > 0) {
                                this.mFunfun = 0;
                                setFunFun(this.mFunfun);
                            } else if (key == 112) {
                                this.mComposingText.deleteForward(1);
                            } else if (this.mComposingText.size(1) == 1 && this.mComposingText.getCursor(1) != 0) {
                                initializeScreen();
                            } else {
                                this.mComposingText.delete(1, false);
                            }
                            updateViewStatusForPrediction(true, true);
                            break;
                        }
                        break;
                    case 122:
                        break;
                    case 123:
                        break;
                    case 136:
                    case 137:
                    case 138:
                    case 139:
                    case 140:
                        if (!processFunctionConvert(key)) {
                        }
                        break;
                    default:
                        if (WnnUtility.isThroughKeyCode(ev)) {
                        }
                        break;
                }
                return false;
            }
            if (converter != null && converter.hasCandidate()) {
                switch (key) {
                    case 19:
                        if (this.mCandidatesManager.isFocusCandidate()) {
                            processMoveKeyEvent(key);
                            return true;
                        }
                        break;
                    case 20:
                        processMoveKeyEvent(key);
                        return true;
                    case 21:
                        if (this.mCandidatesManager.isFocusCandidate()) {
                            processMoveKeyEvent(key);
                            return true;
                        }
                        if (isEnableL2Converter()) {
                            converter.init(this.mWnn.getFilesDirPath());
                        }
                        this.mStatus = 3;
                        updateViewStatusForPrediction(true, true);
                        return false;
                    case 22:
                        if (this.mCandidatesManager.isFocusCandidate()) {
                            processMoveKeyEvent(key);
                            return true;
                        }
                        if (this.mEnableFunfun) {
                            this.mFunfun++;
                            setFunFun(this.mFunfun);
                        } else if (isEnableL2Converter()) {
                            converter.init(this.mWnn.getFilesDirPath());
                        }
                        this.mStatus = 3;
                        updateViewStatusForPrediction(true, true);
                        return this.mEnableFunfun;
                    case 23:
                    case 66:
                    case 160:
                        if (this.mCandidatesManager.isFocusCandidate()) {
                            this.mCandidatesManager.selectFocusCandidate();
                            return true;
                        }
                        break;
                    case 61:
                        processTabKeyEvent(ev);
                        return true;
                    case 122:
                    case 123:
                        if (!this.mCandidatesManager.isFocusCandidate()) {
                            return true;
                        }
                        processMoveKeyEvent(key);
                        return true;
                }
                return processKeyEventNoInputCandidateShown(wnnEv);
            }
            if (this instanceof IWnnImeLatin) {
                this.mStatus = 0;
            }
            switch (key) {
                case 4:
                case ENGINE_MODE_SYMBOL_ADD_SYMBOL:
                    if (isInputViewShown()) {
                        this.mDefaultSoftKeyboard.closing();
                        requestHideSelf(0);
                    }
                    break;
                case 67:
                case ENGINE_MODE_OPT_TYPE_50KEY:
                    if (!processDelKeyEventForUndo(ev)) {
                        break;
                    }
                    break;
            }
            return false;
        }
        return false;
    }

    protected boolean processPrintingKey(KeyEvent ev, boolean convertingForFuncKey) {
        StrSegment str;
        if (ev == null) {
            return false;
        }
        int key = ev.getKeyCode();
        EditorInfo edit = getCurrentInputEditorInfo();
        if (!ev.isAltPressed() && !ev.isShiftPressed()) {
            int shift = this.mAutoCaps ? getShiftKeyState(edit) : 0;
            if (shift != 0 && key >= 29 && key <= 54) {
                str = createStrSegment(ev.getUnicodeChar(1));
            } else {
                str = createStrSegment(ev.getUnicodeChar());
            }
        } else if (ev.isShiftPressed() && !ev.isAltPressed()) {
            str = createStrSegment(ev.getUnicodeChar(1));
        } else if (!ev.isShiftPressed() && ev.isAltPressed()) {
            str = createStrSegment(ev.getUnicodeChar(2));
        } else {
            str = createStrSegment(ev.getUnicodeChar(3));
        }
        if (str == null) {
            return true;
        }
        this.mHandler.removeMessages(1);
        this.mDefaultSoftKeyboard.setPrevInputKeyCode(key);
        if (str.string.charAt(0) != '\t') {
            processHardwareKeyboardInputChar(str, key);
        } else {
            commitText(true);
            commitText(str.string);
            initializeScreen();
        }
        return true;
    }

    protected boolean processLanguageSwitch() {
        return false;
    }

    protected boolean processEisu() {
        return false;
    }

    protected boolean processKana() {
        return false;
    }

    protected boolean processKatakanaHiragana(KeyEvent ev) {
        return false;
    }

    protected boolean processHenkan(KeyEvent ev) {
        return false;
    }

    protected boolean processMuhenkan(KeyEvent ev) {
        return false;
    }

    protected boolean processMoveHome() {
        if (this.mCandidatesManager.isFocusCandidate()) {
            processMoveKeyEvent(122);
        } else {
            int len = this.mComposingText.getCursor(1);
            int funfunlen = this.mFunfun;
            for (int i = 0; i <= len + funfunlen; i++) {
                processLeftKeyEvent();
            }
        }
        return true;
    }

    protected boolean processMoveEnd() {
        if (this.mCandidatesManager.isFocusCandidate()) {
            processMoveKeyEvent(123);
        } else {
            int pos = this.mComposingText.getCursor(1);
            int maxlen = this.mComposingText.toString(1).length();
            if (pos != maxlen && this.mFunfun == 0) {
                while (true) {
                    if (isRenbun()) {
                        if (pos == maxlen) {
                            break;
                        }
                        processRightKeyEvent();
                        pos++;
                    } else {
                        if (pos == maxlen + 1) {
                            break;
                        }
                        processRightKeyEvent();
                        pos++;
                    }
                }
            }
        }
        return true;
    }

    protected boolean processDpadLeftInput() {
        if (this.mCandidatesManager.isFocusCandidate()) {
            processLeftKeyEvent();
            return true;
        }
        if (this.mEnableFunfun && this.mFunfun > 0) {
            this.mFunfun--;
            setFunFun(this.mFunfun);
            this.mStatus = 3;
            updateViewStatus(this.mTargetLayer, true, true);
            return true;
        }
        commitText(false);
        return false;
    }

    protected boolean processFunctionConvert(int keycode) {
        return false;
    }

    protected boolean processKeyEventNoInputCandidateShown(IWnnImeEvent wnnEv) {
        KeyEvent ev;
        if (wnnEv != null && (ev = wnnEv.keyEvent) != null) {
            int key = ev.getKeyCode();
            boolean ret = true;
            switch (key) {
                case 4:
                    if (this.mCandidatesManager.getViewType() == 1) {
                        setCandidateIsViewTypeFull(false);
                        return true;
                    }
                    break;
                case 19:
                case 20:
                case 66:
                case 82:
                case 160:
                    ret = false;
                    break;
                case 23:
                case ENGINE_MODE_SYMBOL_ADD_SYMBOL:
                    break;
                case 67:
                case ENGINE_MODE_OPT_TYPE_50KEY:
                    if (processDelKeyEventForUndo(ev)) {
                        return true;
                    }
                    if (wnnEv.code == -268435442) {
                        sendKeyEventDirect(ev);
                    } else {
                        ret = false;
                    }
                    break;
                    break;
                default:
                    return WnnUtility.isThroughKeyCode(ev) ? false : true;
            }
            WnnEngine converter = getConverter();
            if (converter != null) {
                converter.init(this.mWnn.getFilesDirPath());
            }
            updateViewStatusForPrediction(true, true);
            return ret;
        }
        return false;
    }

    protected void processTabKeyEvent(KeyEvent ev) {
        if (ev != null && this.mCandidatesManager.hasCandidates()) {
            boolean isFocusCand = this.mCandidatesManager.isFocusCandidate();
            if (ev.isShiftPressed()) {
                if (isFocusCand) {
                    processLeftKeyEvent();
                    return;
                } else {
                    processMoveKeyEvent(123);
                    return;
                }
            }
            if (isFocusCand) {
                processRightKeyEvent();
            } else {
                processMoveKeyEvent(20);
            }
        }
    }

    protected void processHardwareKeyboardSpaceKey(KeyEvent ev) {
        if (ev != null) {
            if (ev.isAltPressed()) {
                onEvent(new IWnnImeEvent(IWnnImeEvent.START_SYMBOL_MODE));
            } else {
                processKeyboardSpaceKey();
                this.mEnableAutoDeleteSpace = false;
            }
        }
    }

    private int getPrioritySymbollist() {
        if (this.mWnn == null) {
            return 0;
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
        int prioritySymbollist = pref.getInt(IWnnSymbolEngine.LAST_SYMBOLLIST, ENGINE_MODE_SYMBOL_EMOJI);
        if (SYMBOLLIST_MODE_REPLACE_TABLE.containsKey(Integer.valueOf(prioritySymbollist))) {
            prioritySymbollist = SYMBOLLIST_MODE_REPLACE_TABLE.get(Integer.valueOf(prioritySymbollist)).intValue();
        }
        if (prioritySymbollist == 111 && !this.mConverterSymbolEngineBack.setPriorityAddSymbollist()) {
            prioritySymbollist = ENGINE_MODE_SYMBOL_EMOJI;
        }
        if (prioritySymbollist == 109 && !isEnableEmoticon()) {
            prioritySymbollist = ENGINE_MODE_SYMBOL_EMOJI;
        }
        if (prioritySymbollist == 110 && !isEnableDecoEmoji()) {
            prioritySymbollist = ENGINE_MODE_SYMBOL_EMOJI;
        }
        if (prioritySymbollist == 107 && !isEnableEmoji()) {
            return ENGINE_MODE_SYMBOL_SYMBOL;
        }
        return prioritySymbollist;
    }

    protected void processHardwareKeyboardInputChar(StrSegment str, int key) {
        if (str != null) {
            if (isEnableL2Converter()) {
                boolean commit = false;
                Matcher m = this.mEnglishAutoCommitDelimiter.matcher(str.string);
                if (m.matches()) {
                    commitText(true);
                    commit = true;
                }
                appendStrSegment(str);
                if (commit) {
                    commitText(true);
                    return;
                }
                this.mStatus = 1;
                this.mFunfun = 0;
                setFunFun(this.mFunfun);
                updateViewStatusForPrediction(true, true);
                return;
            }
            appendStrSegment(str);
            commitText(false);
        }
    }

    public boolean onEvaluateFullscreenMode() {
        if (this.mWnn == null) {
            return false;
        }
        return this.mWnn.onEvaluateFullscreenMode();
    }

    public void onFinishInput() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            if (this.mFunfun > 0) {
                this.mFunfun = 0;
                setFunFun(this.mFunfun);
            }
            initializeScreen();
        }
        this.mIsAfterCommit = false;
    }

    public void close(boolean now) {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(4);
        Message message = this.mHandler.obtainMessage(0);
        if (now) {
            this.mHandler.handleMessage(message);
        } else {
            this.mHandler.sendMessageDelayed(message, 0L);
        }
    }

    protected Resources getResources() {
        if (this.mWnn == null) {
            return null;
        }
        return this.mWnn.getResources();
    }

    protected boolean isInputViewShown() {
        if (this.mWnn == null) {
            return false;
        }
        return this.mWnn.isInputViewShown();
    }

    protected void sendDownUpKeyEvents(int keyEventCode) {
        if (this.mWnn != null) {
            this.mWnn.sendDownUpKeyEvents(keyEventCode);
        }
    }

    protected void sendKeyChar(char charCode) {
        if (this.mWnn != null) {
            this.mWnn.sendKeyChar(charCode);
        }
    }

    protected void updateFullscreenMode() {
        if (this.mWnn != null) {
            this.mWnn.updateFullscreenMode();
        }
    }

    protected EditorInfo getCurrentInputEditorInfo() {
        if (this.mWnn == null) {
            return null;
        }
        return this.mWnn.getCurrentInputEditorInfo();
    }

    protected void requestHideSelf(int flag) {
        if (this.mWnn != null) {
            this.mWnn.requestHideSelf(flag);
        }
    }

    protected String searchToggleCharacter(String prevChar, String[] toggleTable, boolean reverse) {
        for (int i = 0; i < toggleTable.length; i++) {
            if (prevChar.equals(toggleTable[i])) {
                if (reverse) {
                    int i2 = i - 1;
                    if (i2 < 0) {
                        return toggleTable[toggleTable.length - 1];
                    }
                    return toggleTable[i2];
                }
                int i3 = i + 1;
                if (i3 == toggleTable.length) {
                    return toggleTable[0];
                }
                return toggleTable[i3];
            }
        }
        return null;
    }

    protected InputConnection getCurrentInputConnection() {
        if (this.mWnn == null) {
            return null;
        }
        return this.mWnn.getCurrentInputConnection();
    }

    private void setCandidatesManager(CandidatesManager candidatesViewManager) {
        if (this.mWnn != null) {
            this.mWnn.setCurrentCandidatesManager(candidatesViewManager);
        }
    }

    private CandidatesManager getCandidatesManager() {
        if (this.mWnn == null) {
            return null;
        }
        return this.mWnn.getCurrentCandidatesManager();
    }

    private void setCurrentDefaultSoftKeyboard(DefaultSoftKeyboard keyboard) {
        if (this.mWnn != null) {
            this.mWnn.setCurrentDefaultSoftKeyboard(keyboard);
        }
    }

    protected void setConverter(WnnEngine converter) {
        if (this.mWnn != null) {
            this.mWnn.setCurrentEngine(converter);
        }
    }

    protected WnnEngine getConverter() {
        if (this.mWnn == null) {
            return null;
        }
        return this.mWnn.getCurrentEngine();
    }

    private void setComposingText(ComposingText composingText) {
        if (this.mWnn != null) {
            this.mWnn.setComposingText(composingText);
        }
    }

    private void setDirectInputMode(boolean directInputMode) {
        if (this.mWnn != null) {
            this.mWnn.setDirectInputMode(directInputMode);
        }
    }

    protected boolean isDirectInputMode() {
        if (this.mWnn == null) {
            return false;
        }
        return this.mWnn.isDirectInputMode();
    }

    private void setAutoHideMode(boolean autoHideMode) {
        if (this.mWnn != null) {
            this.mWnn.setAutoHideMode(autoHideMode);
        }
    }

    private boolean isAutoHideMode() {
        if (this.mWnn == null) {
            return false;
        }
        return this.mWnn.isAutoHideMode();
    }

    protected void initializeScreen() {
        InputConnection inputConnection;
        if (this.mComposingText.size(0) != 0 && (inputConnection = getCurrentInputConnection()) != null) {
            inputConnection.setComposingText(LoggingEvents.EXTRA_CALLING_APP_NAME, 0);
        }
        this.mComposingText.clear();
        this.mExactMatchMode = false;
        this.mFunfun = 0;
        setFunFun(this.mFunfun);
        this.mStatus = 0;
        this.mCandidatesManager.clearCandidates();
        this.mDefaultSoftKeyboard.onUpdateState();
        EngineState state = new EngineState();
        state.temporaryMode = 0;
        if (this.mWnn.isSubtypeEmojiInput()) {
            state.temporaryMode = 1;
        }
        updateEngineState(state);
        if (this.mFullCandidate) {
            this.mFullCandidate = false;
            updateFullscreenMode();
        }
    }

    public boolean isEnableL2Converter() {
        if (getConverter() == null || !this.mEnableConverter) {
            return false;
        }
        return !this.mEngineState.isEnglish() || this.mEnablePrediction;
    }

    protected void callMushRoom(WnnWord word) {
        Resources res;
        CharSequence oldString;
        if (this.mWnn != null && !this.mWnn.isScreenLock() && (res = getResources()) != null) {
            this.mIsGetTextType = false;
            this.mMushroomSelectionStart = 0;
            this.mMushroomSelectionEnd = 0;
            this.mIsGetAllText = res.getBoolean(R.bool.mushroomplus_get_text_value);
            int editorSelectionStart = this.mWnn.getEditorSelectionStart();
            int editorSelectionEnd = this.mWnn.getEditorSelectionEnd();
            if (this.mCandidatesManager.isFocusCandidate()) {
                word = this.mCandidatesManager.getFocusedWnnWord();
            }
            if (word == null) {
                if (this.mComposingText.size(2) != 0) {
                    oldString = DecoEmojiUtil.convertComposingToCommitText(this.mComposingText, 2, 0, this.mComposingText.size(2) - 1).toString();
                } else if (this.mComposingText.size(1) != 0) {
                    oldString = this.mComposingText.toString(1);
                } else {
                    InputConnection inputConnection = getCurrentInputConnection();
                    if (inputConnection != null) {
                        if (editorSelectionStart != editorSelectionEnd) {
                            inputConnection.setSelection(editorSelectionStart, editorSelectionStart);
                            if (editorSelectionStart < editorSelectionEnd) {
                                oldString = inputConnection.getTextAfterCursor(editorSelectionEnd - editorSelectionStart, 1);
                            } else {
                                oldString = inputConnection.getTextBeforeCursor(editorSelectionStart - editorSelectionEnd, 1);
                            }
                            if (oldString == null) {
                                BaseInputView baseInput = (BaseInputView) this.mDefaultSoftKeyboard.getCurrentView();
                                if (baseInput != null) {
                                    baseInput.closeDialog();
                                }
                                Toast.makeText(this.mWnn, res.getString(R.string.ti_error_message_txt), 0).show();
                                return;
                            }
                            this.mMushroomSelectionStart = editorSelectionStart;
                            this.mMushroomSelectionEnd = editorSelectionEnd;
                            inputConnection.setSelection(editorSelectionStart, editorSelectionEnd);
                        } else if (this.mIsGetAllText) {
                            CharSequence before = inputConnection.getTextBeforeCursor(1073741823, 1);
                            CharSequence after = inputConnection.getTextAfterCursor(1073741823, 1);
                            if (before == null || after == null) {
                                BaseInputView baseInput2 = (BaseInputView) this.mDefaultSoftKeyboard.getCurrentView();
                                if (baseInput2 != null) {
                                    baseInput2.closeDialog();
                                }
                                Toast.makeText(this.mWnn, res.getString(R.string.ti_error_message_txt), 0).show();
                                return;
                            }
                            SpannableStringBuilder text = new SpannableStringBuilder();
                            text.append(before);
                            text.append(after);
                            oldString = text;
                            this.mIsGetTextType = true;
                        } else {
                            oldString = LoggingEvents.EXTRA_CALLING_APP_NAME;
                        }
                    } else {
                        oldString = LoggingEvents.EXTRA_CALLING_APP_NAME;
                    }
                }
            } else if ((word.attribute & 512) != 0 || (word.attribute & 2048) != 0 || (word.attribute & 16384) != 0) {
                oldString = LoggingEvents.EXTRA_CALLING_APP_NAME;
            } else {
                String normal = LoggingEvents.EXTRA_CALLING_APP_NAME;
                try {
                    byte[] charArray = word.candidate.toString().getBytes(IWnnIME.CHARSET_NAME_UTF8);
                    byte[] charArrayNormalTemp = new byte[charArray.length];
                    int j = 0;
                    int last = charArray.length;
                    int i = 0;
                    while (i < last) {
                        if (charArray[i] == 27) {
                            i += 5;
                        } else {
                            charArrayNormalTemp[j] = charArray[i];
                            j++;
                            i++;
                        }
                    }
                    byte[] charArrayNormal = new byte[j];
                    for (int k = 0; k < charArrayNormal.length; k++) {
                        charArrayNormal[k] = charArrayNormalTemp[k];
                    }
                    String normal2 = new String(charArrayNormal, IWnnIME.CHARSET_NAME_UTF8);
                    normal = normal2;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                oldString = normal;
            }
            initializeScreen();
            requestHideSelf(0);
            MushroomControl.getInstance().startMushroomLauncher(oldString, Boolean.valueOf(this.mIsGetTextType));
        }
    }

    protected void setFunFun(int funfun) {
        if (this.mWnn != null) {
            this.mWnn.setFunfun(funfun);
        }
    }

    protected void commitMushRoomString() {
        InputConnection inputConnection = getCurrentInputConnection();
        int editorSelectionStart = this.mWnn.getEditorSelectionStart();
        int editorSelectionEnd = this.mWnn.getEditorSelectionEnd();
        if (this.mMushroomResultString != null && inputConnection != null) {
            if (this.mConverterIWnn.hasNonSupportCharacters(this.mMushroomResultString.toString())) {
                this.mMushroomResultString = LoggingEvents.EXTRA_CALLING_APP_NAME;
                Toast.makeText(this.mWnn, R.string.ti_string_output_cancel_message_txt, 0).show();
            }
            if (!this.mMushroomResultString.equals(LoggingEvents.EXTRA_CALLING_APP_NAME)) {
                if (this.mIsGetTextType && this.mMushroomResulttype) {
                    inputConnection.deleteSurroundingText(1073741823, 1073741823);
                    this.mIsGetTextType = false;
                }
                int wordcnt = this.mMushroomSelectionEnd - this.mMushroomSelectionStart;
                if (wordcnt != 0 && editorSelectionEnd == editorSelectionStart) {
                    if (editorSelectionStart == this.mMushroomSelectionEnd) {
                        if (wordcnt > 0) {
                            inputConnection.deleteSurroundingText(wordcnt, 0);
                        } else {
                            inputConnection.deleteSurroundingText(0, Math.abs(wordcnt));
                        }
                    } else if (editorSelectionStart == this.mMushroomSelectionStart) {
                        if (wordcnt > 0) {
                            inputConnection.deleteSurroundingText(0, wordcnt);
                        } else {
                            inputConnection.deleteSurroundingText(Math.abs(wordcnt), 0);
                        }
                    }
                    this.mMushroomSelectionStart = 0;
                    this.mMushroomSelectionEnd = 0;
                }
                commitTextToInputConnection(this.mMushroomResultString, this.mMushroomResultString.toString());
            }
            this.mMushroomResultString = null;
        }
    }

    protected void showVoiceWarningDialog(boolean swipe) {
        Resources res;
        if (this.mWnn != null && (res = getResources()) != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.mWnn);
            builder.setCancelable(true);
            builder.setIcon(R.drawable.ic_mic_dialog);
            builder.setPositiveButton(R.string.ti_dialog_button_ok_txt, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    IWnnImeBase.this.reallyStartShortcutIME();
                }
            });
            builder.setNegativeButton(R.string.ti_dialog_button_cancel_txt, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
            String message = res.getString(R.string.ti_voice_warning_may_not_understand_txt) + "\n\n" + res.getString(R.string.ti_voice_warning_how_to_turn_off_txt);
            builder.setMessage(message);
            builder.setTitle(R.string.ti_voice_warning_title_txt);
            BaseInputView baseInputView = (BaseInputView) this.mDefaultSoftKeyboard.getCurrentView();
            if (baseInputView != null) {
                baseInputView.showDialog(builder);
            }
        }
    }

    protected void startShortcutIME() {
        if (!this.mHasUsedVoiceInput) {
            showVoiceWarningDialog(false);
        } else {
            reallyStartShortcutIME();
        }
    }

    protected void reallyStartShortcutIME() {
        if (this.mWnn != null) {
            if (!this.mHasUsedVoiceInput) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mWnn).edit();
                editor.putBoolean(PREF_HAS_USED_VOICE_INPUT, true);
                editor.commit();
                this.mHasUsedVoiceInput = true;
            }
            initializeScreen();
            this.mWnn.updateShortcutIME();
            this.mWnn.switchToShortcutIME();
        }
    }

    protected void commitVoiceResult(String result) {
        clearCommitInfo();
        initCommitInfoForWatchCursor();
        commitText(result);
        checkCommitInfo();
    }

    protected boolean isThroughKeyCodeForSubTypeEmoji(int keyCode, KeyEvent keyEvent) {
        if (this.mWnn == null) {
            return true;
        }
        if (!this.mWnn.isSubtypeEmojiInput() || keyCode == 0) {
            return false;
        }
        switch (keyCode) {
            case 4:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 66:
            case 92:
            case 93:
            case ENGINE_MODE_SYMBOL_ADD_SYMBOL:
            case 122:
            case 123:
            case 160:
                break;
            case 61:
            case 62:
                if (keyEvent == null || !keyEvent.isCtrlPressed()) {
                }
                break;
            case 211:
                if (keyEvent == null || !keyEvent.isAltPressed()) {
                }
                break;
        }
        return true;
    }

    public void restartSelf(EditorInfo attribute) {
        this.mAttribute = attribute;
        this.mAttribute.initialSelStart = this.mWnn.getEditorSelectionStart();
        this.mAttribute.initialSelEnd = this.mWnn.getEditorSelectionEnd();
        onStartInput(this.mAttribute, false);
        onStartInputView(this.mAttribute, false);
    }

    protected boolean isTenKeyCode(int keyCode) {
        switch (keyCode) {
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 153:
            case 158:
                return true;
            case 154:
            case 155:
            case 156:
            case 157:
            default:
                return false;
        }
    }

    protected boolean isFullTenKeyCode(int keyCode) {
        switch (keyCode) {
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 161:
            case 162:
            case 163:
                return true;
            case 160:
            default:
                return false;
        }
    }

    public void setConvertingForFuncKeyType(int set) {
    }

    public int getConvertingForFuncKeyType() {
        return 1;
    }

    protected void processToggleTimeLimit() {
        if (isDirectInputMode()) {
            sendKeyChar(this.mLastToggleCharTypeNull);
            this.mLastToggleCharTypeNull = (char) 0;
        } else if (this.mStatus == 1) {
            this.mStatus = 3;
        }
        this.mDefaultSoftKeyboard.setKeepShiftMode(true);
        updateViewStatus(this.mTargetLayer, false, false);
        this.mDefaultSoftKeyboard.setKeepShiftMode(false);
    }

    protected void updateViewStatus(int layer, boolean updateCandidates, boolean updateEmptyText) {
    }

    public void updateViewStatus(boolean updateCandidates, boolean updateEmptyText) {
        updateViewStatus(this.mTargetLayer, updateCandidates, updateEmptyText);
    }

    protected void updateViewStatusForPrediction(boolean updateCandidates, boolean updateEmptyText) {
        EngineState state = new EngineState();
        state.convertType = 0;
        updateEngineState(state);
        updateViewStatus(1, updateCandidates, updateEmptyText);
    }

    protected void sendKeyEventDirect(KeyEvent keyEvent) {
        if (keyEvent != null) {
            int keyCode = keyEvent.getKeyCode();
            switch (keyCode) {
                case 19:
                case 20:
                case 21:
                case 22:
                    sendDownUpKeyEvents(keyEvent.getKeyCode());
                    break;
                case 66:
                case 160:
                    processKeyEventEnter();
                    break;
                case 67:
                    processKeyEventDel();
                    break;
            }
        }
    }

    private void processKeyEventEnter() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            EditorInfo edit = getCurrentInputEditorInfo();
            int actionId = edit.imeOptions & 1073742079;
            switch (actionId) {
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    inputConnection.performEditorAction(actionId);
                    break;
                default:
                    ApplicationInfo appInfo = this.mWnn.getTargetApplicationInfo();
                    if (appInfo != null && appInfo.targetSdkVersion < 16) {
                        sendDownUpKeyEventForBackwardCompatibility(66);
                    } else {
                        inputConnection.commitText("\n", 1);
                    }
                    break;
            }
        }
    }

    public void processKeyEventDel() {
        InputConnection inputConnection = getCurrentInputConnection();
        int editorSelectionStart = this.mWnn.getEditorSelectionStart();
        int editorSelectionEnd = this.mWnn.getEditorSelectionEnd();
        if (inputConnection != null) {
            if (editorSelectionStart != editorSelectionEnd) {
                int lengthToDelete = editorSelectionEnd - editorSelectionStart;
                inputConnection.setSelection(editorSelectionEnd, editorSelectionEnd);
                inputConnection.deleteSurroundingText(lengthToDelete, 0);
                return;
            }
            ApplicationInfo appInfo = this.mWnn.getTargetApplicationInfo();
            if (appInfo != null && appInfo.targetSdkVersion < 16) {
                sendDownUpKeyEventForBackwardCompatibility(67);
                return;
            }
            CharSequence lastChar = inputConnection.getTextBeforeCursor(2, 0);
            if (!TextUtils.isEmpty(lastChar) && Character.isHighSurrogate(lastChar.charAt(0))) {
                inputConnection.deleteSurroundingText(2, 0);
            } else {
                inputConnection.deleteSurroundingText(1, 0);
            }
        }
    }

    public void sendDownUpKeyEventForBackwardCompatibility(int code) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            long eventTime = SystemClock.uptimeMillis();
            inputConnection.sendKeyEvent(new KeyEvent(eventTime, eventTime, 0, code, 0, 0, -1, 0, 6));
            inputConnection.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime, 1, code, 0, 0, -1, 0, 6));
        }
    }

    protected void startControlPanelStandard() {
        if (this.mWnn != null) {
            this.mWnn.startControlPanelStandard();
        }
    }

    protected void setFunFunText(String base) {
        if (this.mWnn != null && this.mFunfun > 0) {
            StringBuffer funfunText = new StringBuffer(base);
            for (int i = 0; i < this.mFunfun; i++) {
                funfunText.append(STRING_CIRCLE);
            }
            this.mWnn.setFunfunText(funfunText.toString());
        }
    }

    protected boolean isRenbun() {
        return this.mEngineState.convertType == 1;
    }

    protected CharSequence convertComposingToCommitText(ComposingText composingText, int targetLayer) {
        return convertComposingToCommitText(composingText, targetLayer, 0, composingText.size(targetLayer) - 1);
    }

    protected CharSequence convertComposingToCommitText(ComposingText composingText, int targetLayer, int from, int to) {
        if (composingText == null) {
            return null;
        }
        if (isEnableDecoEmoji() && isRenbun()) {
            return DecoEmojiUtil.convertComposingToCommitText(composingText, targetLayer, from, to);
        }
        return composingText.toString(targetLayer, from, to);
    }

    public boolean isHardKeyboardHidden() {
        return this.mHardKeyboardHidden;
    }

    protected void initConverter() {
        WnnEngine converter = getConverter();
        if (converter != null && this.mWnn != null) {
            converter.init(this.mWnn.getFilesDirPath());
        }
    }

    protected void setSubTypeEmojiInput() {
        if (this.mWnn != null && this.mWnn.isSubtypeEmojiInput() && !this.mWnn.isKeepInput()) {
            if (isEnableEmojiList()) {
                onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, ENGINE_MODE_SYMBOL_EMOJI));
            } else {
                this.mWnn.switchToLastInputMethod(false);
            }
        }
    }

    protected void handleBackEventForSymbolList() {
        if (this.mWnn != null && this.mWnn.isSubtypeEmojiInput()) {
            this.mWnn.switchToLastInputMethod(true);
        } else {
            initializeScreen();
        }
    }

    public void switchToNextInputMethod() {
        if (this.mWnn != null) {
            this.mWnn.switchToNextInputMethod();
        }
    }

    protected void changeInputCandidatesView(boolean isChangeCand) {
        if (this.mWnn != null) {
            this.mWnn.setInputView(this.mWnn.onCreateInputView());
            if (isChangeCand) {
                this.mWnn.setCandidatesView(this.mWnn.onCreateCandidatesView());
            }
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
            this.mDefaultSoftKeyboard.setPreferences(pref, this.mAttribute);
        }
    }

    private boolean isRightOrLeftKeyEvents(IWnnImeEvent ev) {
        KeyEvent keyEvent;
        if (ev == null || ev.code != -268435442 || (keyEvent = ev.keyEvent) == null) {
            return false;
        }
        int keyCode = keyEvent.getKeyCode();
        if (keyCode != 21 && keyCode != 22) {
            return false;
        }
        return true;
    }

    protected void onHandleMessage(int message) {
        switch (message) {
            case 0:
                this.mConverterIWnn.close();
                if (this.mConverterSymbolEngineBack != null) {
                    this.mConverterSymbolEngineBack.close();
                }
                break;
            case 1:
                processToggleTimeLimit();
                break;
            case 2:
                this.mConverterIWnn.writeoutDictionary(this.mLanguageType, 11);
                int[] allMode = this.mDefaultSoftKeyboard.getAllModeTable();
                for (int mode : allMode) {
                    if (mode == 3) {
                        this.mConverterIWnn.writeoutDictionary(1, 11);
                    }
                    break;
                }
                break;
            case 5:
                commitMushRoomString();
                break;
        }
    }

    protected DefaultSoftKeyboard createDefaultSoftKeyboard() {
        return new DefaultSoftKeyboard(this.mWnn);
    }

    protected boolean clearCommitInfo() {
        if (this.mCommitStartCursor < 0) {
            return false;
        }
        this.mCommitStartCursor = -1;
        this.mDefaultSoftKeyboard.setUndoKey(false);
        this.mCanUndo = false;
        this.mHasCommitedByVoiceInput = false;
        return true;
    }

    protected void setCandidateIsViewTypeFull(boolean isFullCandidate) {
        this.mFullCandidate = isFullCandidate;
        if (this.mFullCandidate) {
            this.mStatus |= 16;
            this.mCandidatesManager.setViewType(1);
            updateFullscreenMode();
            if (!this.mEngineState.isSymbolList()) {
                this.mDefaultSoftKeyboard.hideInputView();
            }
        } else {
            this.mStatus &= -17;
            this.mCandidatesManager.setViewType(0);
            updateFullscreenMode();
            this.mDefaultSoftKeyboard.showInputView();
        }
        if (this.mKeyboardManager != null) {
            boolean isShown = isAutoHideMode() ? false : true;
            WnnEngine converter = getConverter();
            if ((converter != null && converter.hasCandidate()) || this.mCandidatesManager.hasCandidates()) {
                isShown = true;
            }
            this.mKeyboardManager.setCandidatesViewShown(isShown);
        }
    }

    protected void fitInputType(SharedPreferences preference, EditorInfo info) {
        if (preference != null && info != null && this.mWnn != null) {
            this.mHasUsedVoiceInput = preference.getBoolean(PREF_HAS_USED_VOICE_INPUT, false);
            this.mDisableAutoCommitEnglishMask &= -2;
            int preferenceDictionary = 0;
            this.mEnableConverter = true;
            this.mEnableSymbolList = true;
            this.mConverterIWnn.setEmailAddressFilter(false);
            Resources res = getResources();
            if (res != null) {
                this.mEnableMushroom = !preference.getString(ControlPanelPrefFragment.MUSHROOM_KEY, res.getString(R.string.mushroom_id_default)).equals(STRING_MUSHROOM_NOT_USE);
                this.mEnableAutoInsertSpace = preference.getBoolean(ControlPanelPrefFragment.AUTO_SPACE_KEY, res.getBoolean(R.bool.opt_auto_space_default_value));
                this.mEnableParenthesisCursorMovement = preference.getBoolean(ControlPanelPrefFragment.PARENTHESIS_CURSOR_KEY, res.getBoolean(R.bool.parenthesis_cursor_movement_default_value));
                boolean isJaJp = this instanceof IWnnImeJaJp;
                int keymode = this.mDefaultSoftKeyboard.getKeyMode();
                if (isJaJp && keymode != 0) {
                    this.mEnableHeadConv = preference.getBoolean(ControlPanelPrefFragment.OPT_HEAD_CONV_KEY, res.getBoolean(R.bool.opt_head_conversion_default_value));
                    this.mEnableFunfun = preference.getBoolean(ControlPanelPrefFragment.OPT_FUNFUN_JA_KEY, res.getBoolean(R.bool.opt_funfun_ja_default_value));
                    this.mEnableLearning = preference.getBoolean(ControlPanelPrefFragment.OPT_ENABLE_LEARNING_JA_KEY, res.getBoolean(R.bool.opt_enable_learning_ja_default_value));
                    this.mEnablePrediction = preference.getBoolean(ControlPanelPrefFragment.OPT_PREDICTION_JA_KEY, res.getBoolean(R.bool.opt_prediction_ja_default_value));
                    this.mEnableSpellCorrection = preference.getBoolean(ControlPanelPrefFragment.OPT_SPELL_CORRECTION_JA_KEY, res.getBoolean(R.bool.opt_spell_correction_ja_default_value));
                } else {
                    this.mEnableHeadConv = false;
                    this.mEnableFunfun = preference.getBoolean(ControlPanelPrefFragment.OPT_FUNFUN_EN_KEY, res.getBoolean(R.bool.opt_funfun_en_default_value));
                    this.mEnableLearning = preference.getBoolean(ControlPanelPrefFragment.OPT_ENABLE_LEARNING_EN_KEY, res.getBoolean(R.bool.opt_enable_learning_en_default_value));
                    this.mEnablePrediction = preference.getBoolean(ControlPanelPrefFragment.OPT_PREDICTION_EN_KEY, res.getBoolean(R.bool.opt_prediction_en_default_value));
                    this.mEnableSpellCorrection = preference.getBoolean(ControlPanelPrefFragment.OPT_SPELL_CORRECTION_EN_KEY, res.getBoolean(R.bool.opt_spell_correction_en_default_value));
                }
                this.mConverterIWnn.setEnableHeadConversion(this.mEnableHeadConv);
                boolean isLatin = this instanceof IWnnImeLatin;
                if (isJaJp || isLatin) {
                    this.mAutoCaps = preference.getBoolean(ControlPanelPrefFragment.AUTO_CAPS_KEY, res.getBoolean(R.bool.auto_caps_default_value));
                }
                if (isJaJp) {
                    this.mEnableHalfSpace = preference.getBoolean(ControlPanelPrefFragment.HALF_SPACE_INPUT_JA_KEY, res.getBoolean(R.bool.opt_half_space_input_ja_default_value));
                    this.mDirectKana = preference.getString(ControlPanelPrefFragment.KANA_ROMAN_INPUT_KEY, res.getString(R.string.kana_roman_input_default_value)).equals(STRING_KANA_INPUT);
                    this.mAutoCursorMovementSpeed = 0;
                    if (!WnnAccessibility.isAccessibility(this.mWnn)) {
                        this.mAutoCursorMovementSpeed = Integer.parseInt(preference.getString(ControlPanelPrefFragment.AUTO_CURSOR_MOVEMENT_KEY, res.getString(R.string.auto_cursor_movement_id_default)));
                        if (this.mAutoCursorMovementSpeed != 0 && preference.getBoolean(ControlPanelPrefFragment.FLICK_INPUT_KEY, res.getBoolean(R.bool.flick_input_default_value)) && !preference.getBoolean(ControlPanelPrefFragment.FLICK_TOGGLE_INPUT_KEY, res.getBoolean(R.bool.flick_toggle_input_default_value))) {
                            this.mAutoCursorMovementSpeed = 0;
                        }
                    }
                    if (this.mDefaultSoftKeyboard.isEnableKeyMode(3)) {
                        if (isEnableKanaInput()) {
                            this.mPreConverter = this.mPreConverterKanaDirect;
                            this.mPreConverterBack = this.mPreConverter;
                        } else if (!this.mEngineState.isEnglish()) {
                            this.mPreConverter = this.mPreConverterKanaRoman;
                            this.mPreConverterBack = this.mPreConverter;
                        }
                    }
                    this.mConverterIWnn.setPreferences(preference);
                }
                boolean enableEmoji = true;
                boolean enableEmoticon = true;
                boolean enableDecoEmoji = false;
                if ((info.inputType & AccessibilityEventCompat.TYPE_GESTURE_DETECTION_END) != 0) {
                    if (keymode == 3 && this.mLanguageType != 10) {
                        if (this.mWnn.isHwCandWindow()) {
                            this.mEnablePrediction = false;
                            this.mEnableFunfun = false;
                        }
                    } else {
                        this.mEnablePrediction = false;
                        this.mEnableLearning = false;
                        this.mEnableConverter = false;
                        this.mEnableFunfun = false;
                        this.mEnableSpellCorrection = false;
                    }
                }
                this.mNonVisiblePassWord = false;
                if (info.inputType == 0) {
                    this.mEnableConverter = false;
                    setDirectInputMode(true);
                } else if (isDefaultKeyModeNoting(info)) {
                    int maskVariation = info.inputType & 4080;
                    switch (info.inputType & 15) {
                        case 1:
                            switch (maskVariation) {
                                case 16:
                                case 32:
                                case 208:
                                    this.mEnableAutoInsertSpace = false;
                                    preferenceDictionary = 3;
                                    break;
                                case 96:
                                    preferenceDictionary = 1;
                                    break;
                                case ENGINE_MODE_OPT_TYPE_50KEY:
                                    preferenceDictionary = 2;
                                    break;
                                case 128:
                                case 144:
                                case 224:
                                    this.mEnablePrediction = false;
                                    this.mEnableLearning = false;
                                    this.mEnableConverter = false;
                                    enableEmoji = false;
                                    enableEmoticon = false;
                                    enableDecoEmoji = false;
                                    this.mConverterIWnn.setEmailAddressFilter(true);
                                    this.mDisableAutoCommitEnglishMask |= 1;
                                    if (maskVariation != 144) {
                                        this.mNonVisiblePassWord = true;
                                    }
                                    break;
                                case 192:
                                    this.mEnableLearning = false;
                                    this.mEnableConverter = false;
                                    this.mEnableSymbolList = false;
                                    break;
                            }
                            break;
                        case 2:
                            if (maskVariation == 16) {
                                this.mNonVisiblePassWord = true;
                            }
                            this.mEnableConverter = false;
                            this.mEnableSymbolList = false;
                            enableEmoji = false;
                            enableEmoticon = false;
                            enableDecoEmoji = false;
                            break;
                        case 3:
                            this.mEnableSymbolList = false;
                            this.mEnableConverter = false;
                            this.mConverterIWnn.setEmailAddressFilter(true);
                            enableEmoji = false;
                            enableEmoticon = false;
                            enableDecoEmoji = false;
                            this.mDisableAutoCommitEnglishMask |= 1;
                            break;
                        case 4:
                            this.mEnableConverter = false;
                            this.mEnableSymbolList = false;
                            enableEmoji = false;
                            enableEmoticon = false;
                            enableDecoEmoji = false;
                            break;
                    }
                }
                if (WnnAccessibility.isAccessibility(this.mWnn)) {
                    enableEmoticon = false;
                }
                this.mConverterSymbolEngineBack.setEnableEmoji(enableEmoji);
                this.mConverterSymbolEngineBack.setEnableEmoticon(enableEmoticon);
                this.mConverterSymbolEngineBack.setEnableDecoEmoji(enableDecoEmoji);
                this.mConverterIWnn.setEmojiFilter(!isEnableEmoji());
                this.mConverterIWnn.setDecoEmojiFilter(!isEnableDecoEmoji());
                DecoEmojiUtil.setConvertFunctionEnabled(isEnableDecoEmoji());
                if (!this.mEnablePrediction) {
                    this.mEnableFunfun = false;
                }
                boolean autoHide = true;
                this.mWnn.updateInputViewShown();
                if (this.mEnableConverter && !this.mWnn.isHwCandWindow() && res != null && this.mWnn.isInputViewShown()) {
                    autoHide = !preference.getBoolean(ControlPanelPrefFragment.SHOW_CANDIDATE_AREA_ALWAYS_KEY, res.getBoolean(R.bool.show_candidate_area_always_default_value));
                }
                setAutoHideMode(autoHide);
                this.mCandidatesManager.setAutoHide(autoHide);
                this.mCandidatesManager.setNumberOfDisplayLines(this.mEnableConverter);
                if (this.mKeyboardManager != null) {
                    this.mKeyboardManager.setCandidatesViewShown(!autoHide);
                }
                if (!this.mWnn.isKeepInput()) {
                    EngineState state = new EngineState();
                    state.preferenceDictionary = preferenceDictionary;
                    state.language = 2;
                    if (!(this instanceof IWnnImeLatin)) {
                        state.language = this.mEngineState.language;
                        state.convertType = 0;
                    }
                    updateEngineState(state);
                }
                if (isLatin && (info.inputType & 15) == 1) {
                    switch (info.inputType & 4080) {
                        case 16:
                            this.mDisableAutoCommitEnglishMask |= 1;
                            break;
                        case 32:
                        case 208:
                            this.mConverterIWnn.setEmailAddressFilter(true);
                            this.mDisableAutoCommitEnglishMask |= 1;
                            break;
                    }
                }
            }
        }
    }

    public boolean isEnablePrediction() {
        return this.mEnablePrediction;
    }

    protected void breakSequence() {
        this.mEnableAutoDeleteSpace = false;
        this.mConverterIWnn.breakSequence();
    }

    protected void commitConvertingText() {
        CharSequence text;
        if (isRenbun()) {
            int size = this.mComposingText.size(2);
            if (size > 0) {
                WnnEngine converter = getConverter();
                for (int i = this.mCommitCount; i < size; i++) {
                    if (converter != null) {
                        converter.makeCandidateListOf(i);
                    }
                    learnWord(null);
                }
                if (this instanceof IWnnImeJaJp) {
                    text = convertComposingToCommitText(this.mComposingText, 2);
                } else {
                    text = this.mComposingText.toString(2);
                }
                commitTextToInputConnection(text, this.mComposingText.toString(1));
                this.mPrevCommitText.append(text);
                this.mIgnoreCursorMove = true;
                initializeScreen();
            } else {
                return;
            }
        }
        if (this.mEngineState.isEisuKana()) {
            this.mComposingText.setCursor(1, this.mComposingText.size(1));
            this.mStatus = commitText(true);
        }
    }

    protected void commitTextToInputConnection(CharSequence commitText, String stroke) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.commitText(commitText, 1);
            this.mIsAfterCommit = true;
        }
    }

    protected int commitText(boolean learn) {
        return 0;
    }

    protected void commitTextLearnProcess(boolean learn, String cand, String stroke) {
        if (getConverter() != null) {
            if (learn) {
                if (isRenbun()) {
                    learnWord(null);
                    return;
                } else {
                    if (this.mComposingText.size(1) != 0) {
                        WnnWord word = new WnnWord(0, cand, stroke, 4);
                        learnWord(word);
                        return;
                    }
                    return;
                }
            }
            breakSequence();
        }
    }

    protected void commitText(String str) {
        this.mFunfun = 0;
        setFunFun(this.mFunfun);
        commitTextToInputConnection(str, null);
        this.mPrevCommitText.append(str);
        this.mIgnoreCursorMove = true;
        this.mEnableAutoDeleteSpace = true;
        updateViewStatusForPrediction(false, false);
    }

    protected void onKeyUpEvent(IWnnImeEvent ev) {
    }

    protected void changeEngineMode(int mode) {
        boolean isJaJp = this instanceof IWnnImeJaJp;
        EngineState state = new EngineState();
        if (isJaJp) {
            switch (mode) {
                case ENGINE_MODE_EISU_KANA:
                    if (this.mEngineState.isEisuKana()) {
                        state.temporaryMode = 0;
                        updateEngineState(state);
                        updateViewStatusForPrediction(true, true);
                    } else {
                        startConvert(2);
                    }
                    break;
                case ENGINE_MODE_OPT_TYPE_12KEY:
                    state.keyboard = 2;
                    updateEngineState(state);
                    clearCommitInfo();
                    break;
                case ENGINE_MODE_OPT_TYPE_50KEY:
                    state.keyboard = 3;
                    updateEngineState(state);
                    clearCommitInfo();
                    break;
            }
        }
        boolean isLatin = this instanceof IWnnImeLatin;
        switch (mode) {
            case ENGINE_MODE_SYMBOL:
                if (this.mEnableSymbolList && !isDirectInputMode()) {
                    if (isLatin && this.mFunfun != 0) {
                        this.mIgnoreCursorMove = true;
                    }
                    this.mFunfun = 0;
                    setFunFun(this.mFunfun);
                    state.temporaryMode = 1;
                    updateEngineState(state);
                    updateViewStatusForPrediction(true, true);
                    break;
                }
                break;
            case ENGINE_MODE_OPT_TYPE_QWERTY:
                state.keyboard = 1;
                updateEngineState(state);
                clearCommitInfo();
                break;
            case ENGINE_MODE_OPT_TYPE_12KEY:
            default:
                if (isJaJp || isLatin) {
                    state.temporaryMode = 0;
                    updateEngineState(state);
                    state.temporaryMode = -1;
                }
                if (!isLatin || this.mLanguageType == 10) {
                    switch (mode) {
                        case 0:
                            if (isJaJp) {
                                state.language = 0;
                                updateEngineState(state);
                                setConverter(this.mConverterIWnn);
                                this.mPreConverterKanaRoman = this.mPreConverterHiragana;
                                this.mPreConverterKanaDirect = this.mPreConverterHiraganaDirect;
                            } else {
                                setConverter(this.mConverterIWnn);
                                if (this.mLanguageType == 10) {
                                    state.language = 3;
                                } else {
                                    state.language = getEngineStateDefaultLanguage();
                                }
                                updateEngineState(state);
                            }
                            break;
                        case 1:
                            if (isJaJp) {
                                state.language = 1;
                                updateEngineState(state);
                                this.mPreConverter = null;
                            }
                            setConverter(null);
                            break;
                        case 2:
                            state.language = 1;
                            updateEngineState(state);
                            setConverter(this.mConverterIWnn);
                            this.mPreConverter = null;
                            break;
                        case 3:
                            setConverter(null);
                            this.mPreConverterKanaRoman = this.mPreConverterHiragana;
                            this.mPreConverterKanaDirect = this.mPreConverterHiraganaDirect;
                            break;
                        case ENGINE_MODE_FULL_KATAKANA:
                            state.language = 0;
                            updateEngineState(state);
                            setConverter(null);
                            this.mPreConverterKanaRoman = this.mPreConverterFullKatakana;
                            this.mPreConverterKanaDirect = this.mPreConverterFullKatakana;
                            break;
                        case ENGINE_MODE_HALF_KATAKANA:
                            state.language = 0;
                            updateEngineState(state);
                            setConverter(null);
                            this.mPreConverterKanaRoman = this.mPreConverterHalfKatakana;
                            this.mPreConverterKanaDirect = this.mPreConverterHalfKatakana;
                            break;
                    }
                    if (!isLatin) {
                        if (isJaJp) {
                            if (mode != 1 && mode != 2) {
                                if (isEnableKanaInput()) {
                                    this.mPreConverter = this.mPreConverterKanaDirect;
                                } else if (!this.mEngineState.isEnglish()) {
                                    this.mPreConverter = this.mPreConverterKanaRoman;
                                }
                            }
                            this.mPreConverterBack = this.mPreConverter;
                        }
                        this.mConverterBack = getConverter();
                    }
                }
                break;
            case ENGINE_MODE_SYMBOL_EMOJI:
                changeSymbolEngineState(state, 3);
                break;
            case ENGINE_MODE_SYMBOL_SYMBOL:
                changeSymbolEngineState(state, 1);
                break;
            case ENGINE_MODE_SYMBOL_KAO_MOJI:
                changeSymbolEngineState(state, 2);
                break;
            case ENGINE_MODE_SYMBOL_DECOEMOJI:
                changeSymbolEngineState(state, 6);
                break;
            case ENGINE_MODE_SYMBOL_ADD_SYMBOL:
                changeSymbolEngineState(state, 7);
                break;
        }
    }

    protected int getEngineStateDefaultLanguage() {
        return -1;
    }

    protected boolean undo() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null || !this.mCanUndo) {
            return false;
        }
        boolean hasCommitedByVoiceInput = this.mHasCommitedByVoiceInput;
        EngineState state = new EngineState();
        state.temporaryMode = 0;
        updateEngineState(state);
        inputConnection.deleteSurroundingText(this.mPrevCommitText.length(), 0);
        this.mDefaultSoftKeyboard.undoKeyMode();
        clearCommitInfo();
        this.mIgnoreCursorMove = true;
        if (this.mEnableLearning && !hasCommitedByVoiceInput) {
            this.mConverterIWnn.undo(1);
        }
        this.mComposingText.clear();
        String[] strSpilit = this.mCommitPredictKey.split(LoggingEvents.EXTRA_CALLING_APP_NAME);
        int length = this.mCommitPredictKey.split(LoggingEvents.EXTRA_CALLING_APP_NAME).length - 1;
        for (int i = 0; i < length; i++) {
            appendStrSegment(new StrSegment(strSpilit[i + 1]));
        }
        if (length != 0) {
            int size = this.mCommitLayer1StrSegment.size();
            StrSegment[] layer1StrSegment = new StrSegment[size];
            this.mCommitLayer1StrSegment.toArray(layer1StrSegment);
            this.mComposingText.setCursor(1, length);
            this.mComposingText.replaceStrSegment(1, layer1StrSegment, length);
            this.mFunfun = this.mCommitFunfun;
            setFunFun(this.mFunfun);
        }
        if (!(this instanceof IWnnImeLatin)) {
            this.mExactMatchMode = this.mCommitExactMatch;
        }
        this.mComposingText.setCursor(1, this.mCommitCursorPosition);
        if (this instanceof IWnnImeJaJp) {
            this.mStatus = 3;
        } else {
            this.mStatus = 1;
        }
        if (isEnableL2Converter()) {
            getConverter().init(this.mWnn.getFilesDirPath());
        }
        if (this.mCommitConvertType == 2) {
            startConvert(2);
        } else {
            updateViewStatusForPrediction(true, true);
        }
        breakSequence();
        return true;
    }

    protected boolean onKeyLongPressEvent(KeyEvent ev) {
        return false;
    }

    protected void updateEngineState(EngineState state) {
        int flexibleType;
        if (state.language != -1) {
            this.mEngineState.language = state.language;
            setDictionary();
            breakSequence();
            if (state.keyboard == -1) {
                state.keyboard = this.mEngineState.keyboard;
            }
        }
        if (!(this instanceof IWnnImeLatin) && state.convertType != -1 && this.mEngineState.convertType != state.convertType) {
            this.mEngineState.convertType = state.convertType;
            setDictionary();
        }
        if (state.temporaryMode != -1) {
            switch (state.temporaryMode) {
                case 0:
                    if (this.mEngineState.temporaryMode != 0) {
                        setDictionary();
                        this.mConverterSymbolEngineBack.initializeMode();
                        if (this instanceof IWnnImeJaJp) {
                            this.mPreConverter = this.mPreConverterBack;
                        }
                        WnnEngine converter = this.mConverterBack;
                        if (this instanceof IWnnImeLatin) {
                            converter = this.mConverterIWnn;
                        }
                        setConverter(converter);
                        this.mDisableAutoCommitEnglishMask &= -17;
                        changeSymbolMode(false);
                    }
                    break;
                case 1:
                    setConverter(this.mConverterSymbolEngineBack);
                    this.mDisableAutoCommitEnglishMask |= 16;
                    this.mConverterSymbolEngineBack.updateAdditionalSymbolInfo();
                    changeSymbolMode(true);
                    break;
            }
            this.mEngineState.temporaryMode = state.temporaryMode;
        }
        if (state.preferenceDictionary != -1 && this.mEngineState.preferenceDictionary != state.preferenceDictionary) {
            this.mEngineState.preferenceDictionary = state.preferenceDictionary;
            setDictionary();
        }
        if (!(this instanceof IWnnImeJaJp) && state.keyboard != -1) {
            if (this.mEnableSpellCorrection) {
                flexibleType = 1;
            } else {
                flexibleType = 0;
            }
            if (this.mWnn != null && !this.mWnn.isKeepInput()) {
                this.mConverterIWnn.setFlexibleCharset(flexibleType, 1);
            }
            this.mEngineState.keyboard = state.keyboard;
        }
    }

    private void changeSymbolMode(boolean isSymbolMode) {
        boolean restart = false;
        if (!this.mHardKeyboardHidden && isSymbolMode != this.mCandidatesManager.isSymbolMode()) {
            restart = true;
        }
        if (isSymbolMode) {
            this.mCandidatesManager.setMode(true, this.mConverterSymbolEngineBack.getMode(), this.mConverterSymbolEngineBack);
            this.mDefaultSoftKeyboard.setSymbolKeyboard();
        } else {
            this.mCandidatesManager.setMode(false, 0, this.mConverterIWnn);
            this.mDefaultSoftKeyboard.setNormalKeyboard();
        }
        if (restart) {
            changeInputCandidatesView(true);
            if (this.mKeyboardManager != null) {
                this.mKeyboardManager.showKeyboard();
            }
        }
    }

    protected void commitAllText() {
        initCommitInfoForWatchCursor();
        if (this.mEngineState.isConvertState()) {
            commitConvertingText();
        } else {
            this.mComposingText.setCursor(1, this.mComposingText.size(1));
            this.mStatus = commitText(true);
        }
        checkCommitInfo();
    }

    protected void updateCandidateView() {
        WnnEngine converter = getConverter();
        if (converter != null) {
            switch (this.mTargetLayer) {
                case 0:
                case 1:
                    if (this instanceof IWnnImeHangul) {
                        int keymode = this.mDefaultSoftKeyboard.getKeyMode();
                        if (keymode == 3 && this.mComposingText.size(1) > 0) {
                            this.mComposingText.setCursor(1, 0);
                            this.mExactMatchMode = false;
                            this.mCommitCount = 0;
                            converter.convert(this.mComposingText);
                        }
                    }
                    if (this.mEnablePrediction || this.mEngineState.isSymbolList() || this.mEngineState.isEisuKana()) {
                        updatePrediction();
                    } else {
                        this.mCandidatesManager.clearCandidates();
                    }
                    break;
                case 2:
                    if (!(this instanceof IWnnImeLatin)) {
                        if (this.mCommitCount == 0) {
                            if (!(this instanceof IWnnImeJaJp)) {
                                this.mConverterIWnn.setConvertedCandidateEnabled(false);
                            }
                            converter.convert(this.mComposingText);
                        }
                        int candidates = converter.makeCandidateListOf(this.mCommitCount);
                        if (candidates != 0) {
                            this.mComposingText.setCursor(2, 1);
                            boolean isExistOtherSegment = this.mComposingText.size(2) > 1;
                            this.mCandidatesManager.setEnableCandidateLongClick(isExistOtherSegment ? false : true);
                            this.mCandidatesManager.displayCandidates();
                        } else {
                            this.mComposingText.setCursor(1, this.mComposingText.toString(1).length());
                            this.mCandidatesManager.clearCandidates();
                        }
                    }
                    break;
            }
        }
    }

    protected boolean initCommitInfoForWatchCursor() {
        if (!isEnableL2Converter()) {
            return false;
        }
        this.mCommitStartCursor = this.mComposingStartCursor;
        this.mPrevCommitText.delete(0, this.mPrevCommitText.length());
        if (this.mCommitStartCursor >= 0 && !this.mEngineState.isSymbolList()) {
            this.mCommitConvertType = this.mEngineState.convertType;
            this.mCommitFunfun = this.mFunfun;
            this.mCommitPredictKey = this.mComposingText.toString(0);
            boolean isLatin = this instanceof IWnnImeLatin;
            if (!isLatin && isRenbun()) {
                this.mCommitCursorPosition = this.mComposingText.size(1);
            } else {
                this.mCommitCursorPosition = this.mComposingText.getCursor(1);
            }
            if (!isLatin) {
                this.mCommitExactMatch = this.mExactMatchMode;
            }
            this.mCommitLayer1StrSegment.clear();
            int size = this.mComposingText.size(1);
            for (int i = 0; i < size; i++) {
                StrSegment c = this.mComposingText.getStrSegment(1, i);
                if (c != null) {
                    this.mCommitLayer1StrSegment.add(new StrSegment(c.string, c.from, c.to));
                }
            }
        }
        return true;
    }

    protected void checkCommitInfo() {
        InputConnection inputConnection;
        int composingLength;
        if (this.mCommitStartCursor >= 0 && (inputConnection = getCurrentInputConnection()) != null) {
            if (this instanceof IWnnImeJaJp) {
                CharSequence composingString = convertComposingToCommitText(this.mComposingText, this.mTargetLayer);
                composingLength = composingString.length();
            } else {
                composingLength = this.mComposingText.toString(this.mTargetLayer).length();
            }
            CharSequence seq = inputConnection.getTextBeforeCursor(this.mPrevCommitText.length() + composingLength, 0);
            if (seq != null && seq.length() >= composingLength) {
                if (!seq.subSequence(0, seq.length() - composingLength).equals(this.mPrevCommitText.toString())) {
                    this.mIgnoreCursorMove = false;
                    clearCommitInfo();
                    return;
                }
                return;
            }
            this.mIgnoreCursorMove = false;
            clearCommitInfo();
        }
    }

    protected void processLeftKeyEvent() {
        if (this.mCandidatesManager.isFocusCandidate()) {
            processMoveKeyEvent(21);
            return;
        }
        if (this.mEngineState.isConvertState()) {
            if (this.mEngineState.isEisuKana()) {
                this.mExactMatchMode = true;
            }
            if (1 < this.mComposingText.getCursor(1)) {
                this.mComposingText.moveCursor(1, -1);
            }
        } else if (this.mExactMatchMode) {
            this.mComposingText.moveCursor(1, -1);
        } else if (this.mEnableFunfun && this.mFunfun > 0) {
            this.mFunfun--;
            setFunFun(this.mFunfun);
        } else if ((this instanceof IWnnImeJaJp) && isEnglishPrediction()) {
            this.mComposingText.moveCursor(1, -1);
        } else {
            this.mExactMatchMode = true;
        }
        this.mCommitCount = 0;
        this.mStatus = 3;
        updateViewStatus(this.mTargetLayer, true, true);
    }

    protected void processRightKeyEvent() {
        if (this.mCandidatesManager.isFocusCandidate()) {
            processMoveKeyEvent(22);
            return;
        }
        int layer = this.mTargetLayer;
        if (this.mExactMatchMode || this.mEngineState.isConvertState()) {
            int textSize = this.mComposingText.size(1);
            if (this.mComposingText.getCursor(1) == textSize) {
                this.mExactMatchMode = false;
                layer = 1;
                EngineState state = new EngineState();
                state.convertType = 0;
                updateEngineState(state);
            } else {
                this.mComposingText.moveCursor(1, 1);
            }
        } else if (this.mComposingText.getCursor(1) < this.mComposingText.size(1)) {
            this.mComposingText.moveCursor(1, 1);
        } else if (this.mEnableFunfun) {
            this.mFunfun++;
            setFunFun(this.mFunfun);
        }
        this.mCommitCount = 0;
        this.mStatus = 3;
        updateViewStatus(layer, true, true);
    }

    protected void processMoveKeyEvent(int key) {
        this.mCandidatesManager.processMoveKeyEvent(key);
    }

    protected boolean isEnglishPrediction() {
        return this.mEngineState.isEnglish() && isEnableL2Converter();
    }

    protected void switchSymbolList() {
        if (this.mWnn != null && !this.mWnn.isSubtypeEmojiInput()) {
            changeSymbolEngineState(new EngineState(), -1);
        }
    }

    protected void switchCharset() {
    }

    protected void changeSymbolEngineState(EngineState state, int mode) {
        if (this.mEnableSymbolList && !isDirectInputMode()) {
            if (mode == -1) {
                this.mConverterSymbolEngineBack.setSymToggle();
            } else {
                this.mConverterSymbolEngineBack.setMode(mode);
            }
            this.mFunfun = 0;
            setFunFun(this.mFunfun);
            if ((this instanceof IWnnImeJaJp) || (this instanceof IWnnImeLatin)) {
                clearCommitInfo();
            }
            state.temporaryMode = 1;
            updateEngineState(state);
            updateViewStatusForPrediction(true, true);
        }
    }

    protected boolean processHardware12Keyboard(KeyEvent keyEvent) {
        return false;
    }

    protected boolean appendStrSegment(StrSegment str) {
        if (this.mComposingText.size(1) >= 50) {
            return false;
        }
        this.mComposingText.insertStrSegment(0, 1, str);
        return true;
    }

    protected void processSoftKeyboardCode(char[] chars) {
        if (chars != null && chars.length > 0) {
            boolean isLatin = this instanceof IWnnImeLatin;
            if (!isLatin) {
                commitConvertingText();
            }
            boolean commit = false;
            if ((this.mEngineState.keyboard == 1 || this.mEngineState.keyboard == 3) && (isLatin || isEnglishPrediction() || isHangulPrediction())) {
                Matcher m = this.mEnglishAutoCommitDelimiter.matcher(new String(chars));
                if (m.matches()) {
                    commit = true;
                }
            }
            if (commit) {
                commitText(true);
                appendStrSegment(new StrSegment(chars));
                commitText(true);
                return;
            }
            appendStrSegment(new StrSegment(chars));
            LetterConverter preConverter = this.mPreConverter;
            if ((this instanceof IWnnImeJaJp) && preConverter != null) {
                preConverter.convert(this.mComposingText);
                this.mStatus = 1;
            }
            this.mHandler.removeMessages(1);
            updateViewStatusForPrediction(true, true);
        }
    }

    protected int getShiftKeyState(EditorInfo editor) {
        InputConnection inputConnection = getCurrentInputConnection();
        return (inputConnection == null || inputConnection.getCursorCapsMode(editor.inputType) == 0) ? 0 : 1;
    }

    protected void processKeyboardSpaceKey() {
        if (!isEnableL2Converter()) {
            commitText(false);
            commitText(STRING_HALF_WIDTH_SPACE);
            this.mEnableAutoDeleteSpace = false;
            this.mCandidatesManager.clearCandidates();
            return;
        }
        if (this.mComposingText.size(0) == 0) {
            commitText(STRING_HALF_WIDTH_SPACE);
            this.mCandidatesManager.clearCandidates();
            breakSequence();
            return;
        }
        if (!isRenbun() && !this.mEngineState.isEnglish() && (this instanceof IWnnImeZh)) {
            startConvert(1);
            return;
        }
        if (isRenbun()) {
            if (!this.mCandidatesManager.isFocusCandidate()) {
                processMoveKeyEvent(20);
            }
            processRightKeyEvent();
            return;
        }
        int cursor = this.mComposingText.getCursor(1) + this.mFunfun;
        if (cursor >= 1) {
            int funfun = this.mFunfun;
            initCommitInfoForWatchCursor();
            this.mStatus = commitText(true);
            commitSpaceJustOne();
            checkCommitInfo();
            if (funfun > 0) {
                initializeScreen();
                breakSequence();
            }
        }
    }

    protected void commitSpaceJustOne() {
        CharSequence seq;
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null && (seq = inputConnection.getTextBeforeCursor(1, 0)) != null && seq.length() > 0 && seq.charAt(0) != ' ') {
            commitText(STRING_HALF_WIDTH_SPACE);
        }
    }

    protected void startConvert(int convertType) {
        int layer;
        if (isEnableL2Converter() && this.mEngineState.convertType != convertType) {
            if (!this.mExactMatchMode) {
                if (convertType == 1) {
                    this.mComposingText.setCursor(1, 0);
                } else if (isRenbun()) {
                    this.mExactMatchMode = true;
                } else {
                    this.mComposingText.setCursor(1, this.mComposingText.size(1));
                }
            }
            if (convertType == 1) {
                this.mFunfun = 0;
                setFunFun(this.mFunfun);
                this.mExactMatchMode = false;
            }
            this.mCommitCount = 0;
            if (convertType == 2) {
                this.mFunfun = 0;
                setFunFun(this.mFunfun);
                layer = 1;
            } else {
                layer = 2;
            }
            EngineState state = new EngineState();
            state.convertType = convertType;
            updateEngineState(state);
            updateViewStatus(layer, true, true);
        }
    }

    protected StrSegment createStrSegment(int charCode) {
        if (charCode == 0 || (Integer.MIN_VALUE & charCode) != 0 || charCode == PRIVATE_AREA_CODE) {
            return null;
        }
        return new StrSegment(Character.toChars(charCode));
    }

    protected boolean processDelKeyEventForUndo(KeyEvent ev) {
        if (ev.isShiftPressed() && undo()) {
            return true;
        }
        clearCommitInfo();
        return false;
    }

    protected void updatePrediction() {
        boolean z = false;
        WnnEngine converter = getConverter();
        if (converter != null) {
            int candidates = 0;
            int cursor = this.mComposingText.getCursor(1);
            if (isEnableL2Converter() || this.mEngineState.isSymbolList()) {
                if (this instanceof IWnnImeJaJp) {
                    this.mConverterIWnn.setEnableHeadConversion(this.mEnableHeadConv);
                }
                if (this.mExactMatchMode) {
                    if (this instanceof IWnnImeJaJp) {
                        this.mConverterIWnn.setEnableHeadConversion(false);
                    } else {
                        this.mConverterIWnn.setConvertedCandidateEnabled(true);
                    }
                    candidates = converter.predict(this.mComposingText, 0, cursor);
                } else if (this.mEnableFunfun && this.mFunfun > 0) {
                    if (this.mFunfun < 4) {
                        candidates = converter.predict(this.mComposingText, this.mFunfun + cursor, this.mFunfun + cursor);
                    }
                    if (candidates <= 0) {
                        while (true) {
                            candidates = converter.predict(this.mComposingText, this.mFunfun + cursor, -1);
                            if (candidates > 0) {
                                break;
                            }
                            int i = this.mFunfun - 1;
                            this.mFunfun = i;
                            if (i == 0) {
                                setFunFun(this.mFunfun);
                                candidates = converter.predict(this.mComposingText, 0, -1);
                                break;
                            }
                        }
                    }
                } else {
                    candidates = converter.predict(this.mComposingText, 0, -1);
                }
            } else {
                this.mFunfun = 0;
                setFunFun(this.mFunfun);
            }
            if (candidates > 0) {
                if (this.mComposingText.size(1) + this.mFunfun == 0 && !this.mEngineState.isSymbolList()) {
                    z = true;
                }
                this.mHasContinuedPrediction = z;
                this.mCandidatesManager.setEnableCandidateLongClick(true);
                this.mCandidatesManager.displayCandidates();
                return;
            }
            this.mCandidatesManager.clearCandidates();
        }
    }

    protected int commitText(WnnWord word) {
        if (getConverter() != null) {
            learnWord(word);
        }
        return commitTextThroughInputConnection(DecoEmojiUtil.getSpannedCandidate(word));
    }

    protected int commitTextThroughInputConnection(CharSequence string) {
        int layer = this.mTargetLayer;
        InputConnection inputConnection = getCurrentInputConnection();
        if (string != null && string.length() > 0 && inputConnection != null) {
            commitTextToInputConnection(string, null);
            this.mPrevCommitText.append(string);
            this.mIgnoreCursorMove = true;
            int cursor = this.mComposingText.getCursor(layer);
            if (cursor > 0) {
                this.mComposingText.deleteStrSegment(layer, 0, this.mComposingText.getCursor(layer) - 1);
                this.mComposingText.setCursor(layer, this.mComposingText.size(layer));
            }
            boolean isLatin = this instanceof IWnnImeLatin;
            this.mFunfun = 0;
            setFunFun(this.mFunfun);
            if (!isLatin) {
                this.mExactMatchMode = false;
                this.mCommitCount++;
            }
            if (layer == 2 && this.mComposingText.size(layer) == 0) {
                layer = 1;
            }
            boolean committed = false;
            if (string.length() == 1) {
                committed = autoCommitEnglish();
            }
            if (!this.mEnableAutoInsertSpace && ((1 == this.mEngineState.language || isLatin) && !this.mEngineState.isSymbolList())) {
                committed = true;
                this.mCandidatesManager.clearCandidates();
            }
            if (!isLatin && 3 == this.mEngineState.preferenceDictionary && !this.mEngineState.isSymbolList()) {
                committed = true;
                this.mCandidatesManager.clearCandidates();
            }
            this.mEnableAutoDeleteSpace = true;
            if (layer == 2 && !isLatin) {
                EngineState state = new EngineState();
                state.convertType = 1;
                updateEngineState(state);
                updateViewStatus(layer, committed ? false : true, false);
            } else {
                updateViewStatusForPrediction(committed ? false : true, false);
            }
        }
        return this.mComposingText.size(0) == 0 ? 0 : 3;
    }

    protected void learnWord(WnnWord word) {
        if (isLearningConvertMultiple(word)) {
            this.mConverterIWnn.learn(this.mEnableLearning);
            return;
        }
        if (!this.mEnableLearning) {
            word.attribute |= 64;
        }
        WnnEngine converter = getConverter();
        if (converter != null) {
            converter.learn(word);
        }
    }

    protected boolean isLearningConvertMultiple(WnnWord word) {
        return word == null;
    }

    protected boolean autoCommitEnglish() {
        InputConnection inputConnection;
        CharSequence seq;
        if ((!isEnglishPrediction() && !isHangulPrediction()) || this.mDisableAutoCommitEnglishMask != 0 || (inputConnection = getCurrentInputConnection()) == null || (seq = inputConnection.getTextBeforeCursor(2, 0)) == null) {
            return false;
        }
        Matcher m = this.mEnglishAutoCommitDelimiter.matcher(seq);
        if (!m.matches()) {
            return false;
        }
        if (seq.charAt(0) == ' ' && this.mEnableAutoDeleteSpace) {
            inputConnection.deleteSurroundingText(2, 0);
            CharSequence str = seq.subSequence(1, 2);
            autoCommitEnglishCharacteristic(str);
        }
        this.mCandidatesManager.clearCandidates();
        return true;
    }

    protected void autoCommitEnglishCharacteristic(CharSequence str) {
        commitTextThroughInputConnection(str);
        this.mPrevCommitText.append(str);
        this.mIgnoreCursorMove = true;
        this.mEnableAutoDeleteSpace = false;
    }

    protected boolean isHangulPrediction() {
        return false;
    }

    protected void setDictionary() {
        if (this.mWnn != null) {
            int language = this.mLanguageType;
            int dictionary = 0;
            int keymode = this.mDefaultSoftKeyboard.getKeyMode();
            if (keymode == 0) {
                language = 1;
            }
            switch (this.mEngineState.preferenceDictionary) {
                case 3:
                    dictionary = 5;
                    break;
            }
            this.mConverterIWnn.setConvertedCandidateEnabled(true);
            this.mConverterIWnn.setDictionary(language, dictionary, this.mWnn.hashCode());
        }
    }

    protected boolean isDefaultKeyModeNoting(EditorInfo info) {
        return true;
    }

    protected boolean appendToggleString(String appendString) {
        return false;
    }

    public void resetHistories() {
        this.mConverterSymbolEngineBack.resetHistories();
    }

    protected boolean isEnableKanaInput() {
        return false;
    }

    public boolean isKeepInput() {
        WnnEngine converter = getConverter();
        return (converter != null && converter.hasCandidate()) || this.mComposingText.size(1) + this.mFunfun > 0;
    }

    public boolean isEnableConverter() {
        return this.mEnableConverter;
    }

    public boolean isNonVisiblePassWord() {
        return this.mNonVisiblePassWord;
    }

    public boolean isEnableEmojiList() {
        if (!isEnableEmoji() || !this.mEnableSymbolList) {
            return false;
        }
        return true;
    }

    private boolean isEnableEmoji() {
        return this.mConverterSymbolEngineBack.isEnableEmoji();
    }

    private boolean isEnableEmoticon() {
        return this.mConverterSymbolEngineBack.isEnableEmoticon();
    }

    private boolean isEnableDecoEmoji() {
        return this.mConverterSymbolEngineBack.isEnableDecoEmoji();
    }
}
