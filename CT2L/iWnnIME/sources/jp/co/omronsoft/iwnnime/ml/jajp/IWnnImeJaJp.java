package jp.co.omronsoft.iwnnime.ml.jajp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import com.android.common.speech.LoggingEvents;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.co.omronsoft.iwnnime.ml.BaseInputView;
import jp.co.omronsoft.iwnnime.ml.DefaultSoftKeyboard;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeBase;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.LetterConverter;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.StrSegment;
import jp.co.omronsoft.iwnnime.ml.WnnEngine;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;
import jp.co.omronsoft.iwnnime.ml.WnnWord;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelPrefFragment;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiOperationQueue;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiUtil;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnSymbolEngine;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;

public class IWnnImeJaJp extends IWnnImeBase {
    private static final int CHARCODE_LARGE_A = 65;
    private static final int CHARCODE_LARGE_Z = 90;
    private static final int CHARCODE_OFFSET_LARGE_TO_SMALL = 32;
    private static final int CODE_HIRAGANA_WO = 12434;
    private static final String COMMIT_TEXT_THROUGH_ACTION = "jp.co.omronsoft.iwnnime.ml";
    private static final String COMMIT_TEXT_THROUGH_KEY_YOMI = "yomi";
    private static final String DEFAULT_KEYMODE_FULL_ALPHABET_STR = "jp.co.omronsoft.iwnnime.ml.mode=4";
    private static final String DEFAULT_KEYMODE_FULL_HIRAGANA_STR = "jp.co.omronsoft.iwnnime.ml.mode=1";
    private static final String DEFAULT_KEYMODE_FULL_KATAKANA_STR = "jp.co.omronsoft.iwnnime.ml.mode=2";
    private static final String DEFAULT_KEYMODE_FULL_NUMBER_STR = "jp.co.omronsoft.iwnnime.ml.mode=6";
    private static final String DEFAULT_KEYMODE_HALF_ALPHABET_STR = "jp.co.omronsoft.iwnnime.ml.mode=5";
    private static final String DEFAULT_KEYMODE_HALF_KATAKANA_STR = "jp.co.omronsoft.iwnnime.ml.mode=3";
    private static final String DEFAULT_KEYMODE_HALF_NUMBER_STR = "jp.co.omronsoft.iwnnime.ml.mode=7";
    private static final String DEFAULT_KEYMODE_HALF_PHONE_STR = "jp.co.omronsoft.iwnnime.ml.mode=9";
    public static final int DEFAULT_KEYMODE_NOTHING = -1;
    private static final int MAX_ASCII_CODE = 127;
    private static final String STRING_FULL_WIDTH_SPACE = "\u3000";
    private static final String STRING_HIRAGANA_LETTER_N = "ん";
    private static final String STRING_LOWER_N = "n";
    private static final String STRING_LOWER_Y = "y";
    private static final String STRING_UPPER_N = "N";
    private static final String STRING_UPPER_Y = "Y";
    private static final String TAG = "IWnnImeJaJp";
    private int mConvertingForFuncKeyType;
    private boolean mIsInputSequenced;
    private String mPreEditorPackageName;
    private static final Pattern ENGLISH_CHARACTER_LAST = Pattern.compile(".*[a-zA-Z]$");
    private static final Pattern ENGLISH_CHARACTER_ALL = Pattern.compile("^[a-zA-Z]+$");
    private static final HashMap<Integer, Integer> HW12KEYBOARD_KEYCODE_REPLACE_TABLE = new HashMap<Integer, Integer>() {
        {
            put(7, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_0));
            put(8, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_1));
            put(9, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_2));
            put(10, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_3));
            put(11, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_4));
            put(12, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_5));
            put(13, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_6));
            put(14, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_7));
            put(15, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_8));
            put(16, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_9));
            put(18, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_SHARP));
            put(17, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_ASTER));
            put(5, Integer.valueOf(DefaultSoftKeyboard.KEYCODE_JP12_REVERSE));
        }
    };

    public IWnnImeJaJp(IWnnIME wnn) {
        super(wnn, 0);
        this.mPreEditorPackageName = LoggingEvents.EXTRA_CALLING_APP_NAME;
        this.mConvertingForFuncKeyType = 1;
        this.mEnableHeadConv = true;
        String localeString = LanguageManager.getChosenLocale(0).toString();
        this.mConverterSymbolEngineBack = new IWnnSymbolEngine(this.mWnn, localeString);
        this.mPreConverterHiragana = new Romkan();
        this.mPreConverter = this.mPreConverterHiragana;
        this.mPreConverterFullKatakana = new RomkanFullKatakana();
        this.mPreConverterHalfKatakana = new RomkanHalfKatakana();
        this.mPreConverterHiraganaDirect = new Directkan();
        this.mPreConverterKanaRoman = this.mPreConverterHiragana;
        this.mPreConverterKanaDirect = this.mPreConverterHiraganaDirect;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IWnnImeBase.EngineState state = new IWnnImeBase.EngineState();
        state.keyboard = 1;
        updateEngineState(state);
    }

    @Override
    public void onCreateInputView() {
        Resources res = getResources();
        boolean type12Key = this.mEnableHardware12Keyboard;
        if (res != null) {
            type12Key = res.getConfiguration().keyboard == 3;
        }
        this.mDefaultSoftKeyboard.setHardware12Keyboard(type12Key);
        this.mEnableHardware12Keyboard = type12Key;
        super.onCreateInputView();
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if (!this.mPreEditorPackageName.equals(attribute.packageName) && this.mWnn != null) {
            this.mConverterIWnn.setDictionary(0, 45, this.mWnn.hashCode());
            this.mConverterIWnn.deleteAutoLearningDictionary(45);
            this.mPreEditorPackageName = attribute.packageName;
        }
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        if (!this.mHandler.hasMessages(3)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(3), 0L);
        }
        if (!this.mHandler.hasMessages(4)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4), 0L);
        }
    }

    @Override
    public void hideWindow() {
        super.hideWindow();
        this.mDefaultSoftKeyboard.onUpdateState();
        onEvent(new IWnnImeEvent(IWnnImeEvent.CANCEL_WEBAPI));
    }

    @Override
    public boolean onConfigurationChanged(Configuration newConfig) {
        boolean ret = super.onConfigurationChanged(newConfig);
        if (ret) {
            this.mDefaultSoftKeyboard.setHardware12Keyboard(this.mEnableHardware12Keyboard);
        }
        return ret;
    }

    public static int getDefaultKeyMode(EditorInfo editor) {
        if (editor.privateImeOptions == null) {
            return -1;
        }
        if (editor.privateImeOptions.equals(DEFAULT_KEYMODE_FULL_HIRAGANA_STR)) {
            return 3;
        }
        if (editor.privateImeOptions.equals(DEFAULT_KEYMODE_FULL_ALPHABET_STR)) {
            return 6;
        }
        if (editor.privateImeOptions.equals(DEFAULT_KEYMODE_FULL_NUMBER_STR)) {
            return 7;
        }
        if (editor.privateImeOptions.equals(DEFAULT_KEYMODE_FULL_KATAKANA_STR)) {
            return 4;
        }
        if (editor.privateImeOptions.equals(DEFAULT_KEYMODE_HALF_ALPHABET_STR)) {
            return 0;
        }
        if (editor.privateImeOptions.equals(DEFAULT_KEYMODE_HALF_NUMBER_STR)) {
            return 1;
        }
        if (editor.privateImeOptions.equals(DEFAULT_KEYMODE_HALF_KATAKANA_STR)) {
            return 5;
        }
        if (!editor.privateImeOptions.equals(DEFAULT_KEYMODE_HALF_PHONE_STR)) {
            return -1;
        }
        return 2;
    }

    public void switchKanaRomanMode() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
        SharedPreferences.Editor editor = pref.edit();
        Resources res = getResources();
        if (this.mDirectKana) {
            if (res != null) {
                editor.putString(ControlPanelPrefFragment.KANA_ROMAN_INPUT_KEY, res.getString(R.string.kana_roman_input_mode_list_item_roman));
            }
            this.mDirectKana = false;
        } else {
            if (res != null) {
                editor.putString(ControlPanelPrefFragment.KANA_ROMAN_INPUT_KEY, res.getString(R.string.kana_roman_input_mode_list_item_kana));
            }
            this.mDirectKana = true;
        }
        editor.commit();
        if (this.mEngineState.isEnglish()) {
            this.mDefaultSoftKeyboard.changeKeyMode(0);
        } else {
            this.mDefaultSoftKeyboard.changeKeyMode(3);
        }
        this.mCandidatesManager.clearCandidates();
    }

    @Override
    protected DefaultSoftKeyboard createDefaultSoftKeyboard() {
        return new DefaultSoftKeyboardJAJP(this.mWnn);
    }

    @Override
    protected boolean handleKeyEventForDirectInputMode(IWnnImeEvent ev) {
        if (ev == null) {
            return false;
        }
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) {
            return false;
        }
        KeyEvent keyEvent = ev.keyEvent;
        switch (ev.code) {
            case IWnnImeEvent.INPUT_CHAR:
                if (ev.chars[0] >= '0' && ev.chars[0] <= '9') {
                    commitTextToInputConnection(String.valueOf(ev.chars[0]), null);
                } else {
                    sendKeyChar(ev.chars[0]);
                }
                break;
            case IWnnImeEvent.INPUT_SOFT_KEY:
                sendKeyEventDirect(keyEvent);
                break;
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
                        }
                        break;
                    case 62:
                        if (this.mWnn != null && !isInputViewShown() && keyEvent.isShiftPressed()) {
                            break;
                        }
                        break;
                    case 211:
                        processKeyEvent(ev);
                        break;
                }
                break;
        }
        return false;
    }

    @Override
    protected void updateViewStatus(int layer, boolean updateCandidates, boolean updateEmptyText) {
        TextView tv;
        this.mTargetLayer = layer;
        if (updateCandidates) {
            updateCandidateView();
            if (this.mFullCandidate) {
                this.mFullCandidate = false;
                updateFullscreenMode();
            }
        }
        this.mDefaultSoftKeyboard.onUpdateState();
        this.mDisplayText.clear();
        this.mDisplayText.insert(0, (CharSequence) this.mComposingText.toString(layer));
        setFunFunText(this.mDisplayText.toString());
        int cursor = this.mComposingText.getCursor(layer);
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            if (this.mDisplayText.length() != 0 || updateEmptyText) {
                if (cursor != 0) {
                    int highlightEnd = 0;
                    if (((this.mExactMatchMode && !this.mEngineState.isEisuKana()) || (isEnglishPrediction() && cursor < this.mComposingText.size(1))) && getConvertingForFuncKeyType() == 1) {
                        WnnUtility.setSpan(this.mDisplayText, SPAN_EXACT_BGCOLOR_HL, 0, cursor, 33);
                        highlightEnd = cursor;
                    } else if (this.mEngineState.isEisuKana()) {
                        WnnUtility.setSpan(this.mDisplayText, SPAN_EISUKANA_BGCOLOR_HL, 0, cursor, 33);
                        highlightEnd = cursor;
                    } else if (layer == 2) {
                        CharSequence firstSegment = convertComposingToCommitText(this.mComposingText, 2, 0, 0);
                        if (this.mConverterSymbolEngineBack.isEnableDecoEmoji()) {
                            CharSequence composingString = convertComposingToCommitText(this.mComposingText, 2);
                            this.mDisplayText.clear();
                            if (composingString.length() == 0 && (tv = this.mCandidatesManager.getFirstTextView()) != null) {
                                composingString = tv.getText();
                            }
                            this.mDisplayText.insert(0, composingString);
                        }
                        highlightEnd = firstSegment.length();
                        WnnUtility.setSpan(this.mDisplayText, SPAN_CONVERT_BGCOLOR_HL, 0, highlightEnd, 33);
                    } else if (this.mAutoCursorMovementSpeed != 0 && this.mStatus == 1 && this.mHandler.hasMessages(1)) {
                        WnnUtility.setSpan(this.mDisplayText, this.mSpanToggleCharacterBgHl, cursor - 1, cursor, 33);
                        WnnUtility.setSpan(this.mDisplayText, this.mSpanToggleCharacterText, cursor - 1, cursor, 33);
                    }
                    if (highlightEnd > 0) {
                        WnnUtility.setSpan(this.mDisplayText, SPAN_REMAIN_BGCOLOR_HL, highlightEnd, this.mDisplayText.length(), 33);
                        WnnUtility.setSpan(this.mDisplayText, SPAN_TEXTCOLOR, 0, this.mDisplayText.length(), 33);
                    }
                }
                WnnUtility.setSpan(this.mDisplayText, SPAN_UNDERLINE, 0, this.mDisplayText.length(), 33);
                int displayCursor = this.mFunfun + cursor != 0 ? 1 : 0;
                if (this.mDisplayText.length() != 0 || !this.mHasStartedTextSelection) {
                    inputConnection.setComposingText(this.mDisplayText, displayCursor);
                }
            }
        }
    }

    @Override
    protected int commitText(boolean learn) {
        if (isEnglishPrediction()) {
            this.mComposingText.setCursor(1, this.mComposingText.size(1));
        }
        int layer = this.mTargetLayer;
        int cursor = this.mComposingText.getCursor(layer);
        String tmp = this.mComposingText.toString(layer, 0, cursor - 1);
        commitTextLearnProcess(learn, tmp, tmp);
        String stroke = null;
        if (!isEnglishPrediction()) {
            stroke = this.mComposingText.toString(1, 0, this.mComposingText.getCursor(1) - 1);
        }
        CharSequence commitText = convertComposingToCommitText(this.mComposingText, layer, 0, cursor - 1);
        return commitComposingText(commitText, stroke);
    }

    @Override
    protected int commitText(WnnWord word) {
        if (getConverter() != null) {
            learnWord(word);
        }
        String stroke = null;
        if (!isEnglishPrediction()) {
            stroke = word.stroke;
        }
        return commitComposingText(DecoEmojiUtil.getSpannedCandidate(word), stroke);
    }

    @Override
    protected void commitText(String str) {
        this.mIsInputSequenced = true;
        super.commitText(str);
    }

    @Override
    protected void updateEngineState(IWnnImeBase.EngineState state) {
        int flexibleType;
        super.updateEngineState(state);
        if (state.keyboard != -1) {
            int keyboardType = state.keyboard == 2 ? 0 : 1;
            switch (state.keyboard) {
                case 2:
                case 3:
                    if (this.mEngineState.language == 1) {
                        flexibleType = 0;
                    } else {
                        flexibleType = 1;
                    }
                    break;
                default:
                    Resources res = getResources();
                    if (res != null) {
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
                        if (this.mEngineState.language == 0) {
                            this.mEnableSpellCorrection = pref.getBoolean(ControlPanelPrefFragment.OPT_SPELL_CORRECTION_JA_KEY, res.getBoolean(R.bool.opt_spell_correction_ja_default_value));
                        } else {
                            this.mEnableSpellCorrection = pref.getBoolean(ControlPanelPrefFragment.OPT_SPELL_CORRECTION_EN_KEY, res.getBoolean(R.bool.opt_spell_correction_en_default_value));
                        }
                    }
                    if (this.mEngineState.language == 0 || !this.mEnableSpellCorrection) {
                        flexibleType = 0;
                    } else {
                        flexibleType = 1;
                    }
                    break;
            }
            if (this.mWnn != null && !this.mWnn.isKeepInput()) {
                this.mConverterIWnn.setFlexibleCharset(flexibleType, keyboardType);
            }
            this.mEngineState.keyboard = state.keyboard;
        }
    }

    @Override
    protected void setDictionary() {
        int language;
        if (this.mWnn != null) {
            int dictionary = 0;
            if (this.mEngineState.convertType == 2) {
                language = 0;
                dictionary = 1;
            } else {
                switch (this.mEngineState.language) {
                    case 1:
                        language = 1;
                        this.mConverterIWnn.setConvertedCandidateEnabled(true);
                        switch (this.mEngineState.preferenceDictionary) {
                            case 3:
                                dictionary = 5;
                                break;
                        }
                        break;
                    default:
                        language = 0;
                        this.mConverterIWnn.setConvertedCandidateEnabled(false);
                        switch (this.mEngineState.preferenceDictionary) {
                            case 1:
                                dictionary = 3;
                                break;
                            case 2:
                                dictionary = 4;
                                break;
                        }
                        break;
                }
            }
            this.mConverterIWnn.setDictionary(language, dictionary, this.mWnn.hashCode());
        }
    }

    @Override
    protected void autoCommitEnglishCharacteristic(CharSequence str) {
        commitTextToInputConnection(str, null);
        this.mPrevCommitText.append(str);
        this.mIgnoreCursorMove = true;
    }

    @Override
    protected boolean isLearningConvertMultiple(WnnWord word) {
        return word == null || !(this.mEnableLearning || (word.attribute & 1024) == 0);
    }

    @Override
    protected void onKeyUpEvent(IWnnImeEvent wnnEv) {
        CharSequence text;
        if (wnnEv != null) {
            KeyEvent ev = wnnEv.keyEvent;
            int key = 0;
            if (ev != null) {
                key = ev.getKeyCode();
            }
            if (this.mEnableHardware12Keyboard && !isDirectInputMode() && isHardKeyboard12KeyLongPress(key) && (ev.getFlags() & 256) == 0) {
                switch (key) {
                    case 1:
                        if (this.mEngineState.isSymbolList()) {
                            switchSymbolList();
                        } else if (this.mComposingText.size(0) != 0 && !isRenbun() && this.mDefaultSoftKeyboard.getKeyMode() == 3) {
                            startConvert(1);
                        } else {
                            this.mDefaultSoftKeyboard.onKey(DefaultSoftKeyboard.KEYCODE_JP12_SYM, null);
                        }
                        break;
                    case 2:
                        this.mDefaultSoftKeyboard.showInputModeSwitchDialog();
                        break;
                    case 67:
                        int newKeyCode = IWnnImeBase.ENGINE_MODE_OPT_TYPE_50KEY;
                        int composingTextSize = this.mComposingText.size(1);
                        if (this.mFunfun + composingTextSize > 0) {
                            if (this.mComposingText.getCursor(1) > composingTextSize - 1) {
                                newKeyCode = 67;
                            }
                            KeyEvent keyEvent = new KeyEvent(ev.getAction(), newKeyCode);
                            if (!processKeyEvent(wnnEv)) {
                                sendKeyEventDirect(keyEvent);
                            }
                        } else {
                            InputConnection inputConnection = getCurrentInputConnection();
                            if (inputConnection != null && ((text = inputConnection.getTextAfterCursor(1, 0)) == null || text.length() == 0)) {
                                newKeyCode = 67;
                            }
                            KeyEvent keyEvent2 = new KeyEvent(ev.getAction(), newKeyCode);
                            sendKeyEventDirect(keyEvent2);
                        }
                        break;
                }
            }
        }
    }

    @Override
    protected boolean onKeyLongPressEvent(KeyEvent ev) {
        if (this.mEnableHardware12Keyboard) {
            int keyCode = 0;
            if (ev != null) {
                keyCode = ev.getKeyCode();
            }
            switch (keyCode) {
                case 1:
                    if (this.mEnableMushroom) {
                        callMushRoom(null);
                    }
                    break;
                case 2:
                    startControlPanelStandard();
                    break;
                case 67:
                    initializeScreen();
                    InputConnection inputConnection = getCurrentInputConnection();
                    if (inputConnection != null) {
                        inputConnection.deleteSurroundingText(Integer.MAX_VALUE, Integer.MAX_VALUE);
                    }
                    break;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void breakSequence() {
        this.mIsInputSequenced = false;
        super.breakSequence();
    }

    @Override
    protected void switchCharset() {
        if (this.mEngineState.isEnglish()) {
            this.mDefaultSoftKeyboard.changeKeyMode(3);
        } else {
            this.mDefaultSoftKeyboard.changeKeyMode(0);
        }
        this.mCandidatesManager.clearCandidates();
    }

    @Override
    protected void onHandleMessage(int message) throws Throwable {
        switch (message) {
            case 3:
                boolean result = DecoEmojiOperationQueue.getInstance().executeOperation(LoggingEvents.EXTRA_CALLING_APP_NAME, null);
                if (result) {
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(3), 0L);
                }
                break;
            case 4:
                boolean result2 = this.mConverterIWnn.executeOperation(this.mWnn);
                if (result2) {
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4), 0L);
                }
                break;
            default:
                super.onHandleMessage(message);
                break;
        }
    }

    @Override
    public void setConvertingForFuncKeyType(int set) {
        this.mConvertingForFuncKeyType = set;
    }

    @Override
    public int getConvertingForFuncKeyType() {
        return this.mConvertingForFuncKeyType;
    }

    @Override
    protected boolean isDefaultKeyModeNoting(EditorInfo info) {
        int defaultKeyMode = getDefaultKeyMode(info);
        if (defaultKeyMode != -1) {
            return false;
        }
        return true;
    }

    @Override
    protected void commitTextToInputConnection(CharSequence commitText, String stroke) {
        super.commitTextToInputConnection(commitText, stroke);
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            Bundle data = new Bundle();
            if (stroke != null && stroke.length() > 0) {
                data.putString(COMMIT_TEXT_THROUGH_KEY_YOMI, stroke);
            } else {
                data.putString(COMMIT_TEXT_THROUGH_KEY_YOMI, commitText.toString());
            }
            inputConnection.performPrivateCommand(COMMIT_TEXT_THROUGH_ACTION, data);
        }
    }

    @Override
    protected boolean appendToggleString(String appendString) {
        boolean appendComplete = appendStrSegment(new StrSegment(appendString));
        forwardCursor();
        return appendComplete;
    }

    @Override
    protected boolean processSwitchCharset(boolean isSymbol) {
        if (isSymbol) {
            initializeScreen();
        }
        switchCharset();
        return true;
    }

    @Override
    protected boolean processInputChar(IWnnImeEvent ev) {
        if (ev == null) {
            return false;
        }
        this.mFunfun = 0;
        setFunFun(this.mFunfun);
        String string = new String(ev.chars);
        if (this.mPreConverter == null && !isEnableL2Converter()) {
            commitText(false);
            commitText(string);
            this.mCandidatesManager.clearCandidates();
            this.mEnableAutoDeleteSpace = false;
        } else if (!isEnableL2Converter()) {
            if (this.mDefaultSoftKeyboard.getKeyboardType() == 3 && this.mDefaultSoftKeyboard.isEnableReplace(string)) {
                commitText(false);
                processSoftKeyboardCode(ev.chars);
            } else {
                processSoftKeyboardCodeWithoutConversion(ev.chars);
            }
        } else {
            processSoftKeyboardCode(ev.chars);
        }
        return true;
    }

    @Override
    protected boolean processFlickInputChar(IWnnImeEvent ev) {
        char top;
        if (ev == null) {
            return false;
        }
        String input = new String(ev.chars);
        commitConvertingText();
        if (!isEnableL2Converter()) {
            commitText(false);
        }
        if (this.mAutoCaps && getShiftKeyState(getCurrentInputEditorInfo()) == 1 && (top = input.charAt(0)) <= 127 && Character.isLowerCase(top)) {
            input = Character.toString(Character.toUpperCase(top));
        }
        appendStrSegment(new StrSegment(input));
        this.mFunfun = 0;
        setFunFun(this.mFunfun);
        this.mStatus = 3;
        updateViewStatusForPrediction(true, true);
        return true;
    }

    @Override
    protected void processKeyboardSpaceKey() {
        boolean isInput = this.mComposingText.size(0) > 0;
        if (isInput && isEnableL2Converter()) {
            if (isEnglishPrediction()) {
                initCommitInfoForWatchCursor();
                commitText(true);
                commitSpaceJustOne();
                checkCommitInfo();
                return;
            }
            processConvert();
            return;
        }
        if (isInput) {
            commitText(false);
        }
        commitText(getCurrentSpaceString());
        this.mCandidatesManager.clearCandidates();
        breakSequence();
    }

    @Override
    protected void processHardwareKeyboardSpaceKey(KeyEvent ev) {
        if (ev != null) {
            if (ev.isShiftPressed()) {
                switchCharset();
            } else {
                super.processHardwareKeyboardSpaceKey(ev);
            }
        }
    }

    @Override
    protected void processHardwareKeyboardInputChar(StrSegment str, int key) {
        LetterConverter preConverter;
        if (str != null) {
            if (isEnableKanaInput() && isFullTenKeyCode(key)) {
                preConverter = this.mPreConverterKanaRoman;
            } else {
                preConverter = this.mPreConverter;
            }
            if (isEnableL2Converter()) {
                boolean commit = false;
                if (preConverter == null) {
                    Matcher m = this.mEnglishAutoCommitDelimiter.matcher(str.string);
                    if (m.matches()) {
                        commitText(true);
                        commit = true;
                    }
                    appendStrSegment(str);
                } else {
                    appendStrSegment(str);
                    preConverter.convert(this.mComposingText);
                }
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
            boolean completed = true;
            if (preConverter != null) {
                completed = preConverter.convert(this.mComposingText);
            }
            if (completed) {
                if (isEnableKanaInput()) {
                    commitTextWithoutLastKana();
                    return;
                } else if (!this.mEngineState.isEnglish()) {
                    commitTextWithoutLastAlphabet();
                    return;
                } else {
                    commitText(false);
                    return;
                }
            }
            updateViewStatus(1, false, true);
        }
    }

    @Override
    protected boolean processPrintingKey(KeyEvent ev, boolean convertingForFuncKey) {
        StrSegment str;
        if (ev == null) {
            return false;
        }
        commitConvertingText();
        int key = ev.getKeyCode();
        EditorInfo edit = getCurrentInputEditorInfo();
        if (!ev.isAltPressed() && !ev.isShiftPressed()) {
            int shift = this.mAutoCaps ? getShiftKeyState(edit) : 0;
            if (isEnableKanaInput()) {
                shift = 0;
            }
            if (shift != 0 && key >= 29 && key <= 54) {
                str = createStrSegment(ev.getUnicodeChar(1));
            } else if (isEnableKanaInput()) {
                int charCode = ev.getUnicodeChar();
                if (charCode >= CHARCODE_LARGE_A && charCode <= CHARCODE_LARGE_Z) {
                    str = createStrSegment(charCode + 32);
                } else {
                    switch (key) {
                        case 217:
                            str = createStrSegment(92);
                            break;
                        default:
                            str = createStrSegment(charCode);
                            break;
                    }
                }
            } else {
                str = createStrSegment(ev.getUnicodeChar());
            }
        } else if (ev.isShiftPressed() && !ev.isAltPressed()) {
            switch (key) {
                case 7:
                    if (isEnableKanaInput()) {
                        str = createStrSegment(CODE_HIRAGANA_WO);
                        break;
                    }
                default:
                    str = createStrSegment(ev.getUnicodeChar(1));
                    break;
            }
        } else if (!ev.isShiftPressed() && ev.isAltPressed()) {
            switch (key) {
                case 31:
                case 47:
                    str = null;
                    break;
                default:
                    str = createStrSegment(ev.getUnicodeChar(2));
                    break;
            }
        } else {
            switch (key) {
                case 31:
                case 47:
                    str = null;
                    break;
                default:
                    str = createStrSegment(ev.getUnicodeChar(3));
                    break;
            }
        }
        if (str == null) {
            return true;
        }
        if (convertingForFuncKey) {
            initCommitInfoForWatchCursor();
            this.mStatus = commitText(true);
            checkCommitInfo();
            this.mEnableAutoDeleteSpace = false;
        }
        setConvertingForFuncKeyType(1);
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

    @Override
    protected boolean processLanguageSwitch() {
        if (!isInputViewShown()) {
            return false;
        }
        switchCharset();
        return true;
    }

    @Override
    protected boolean processEisu() {
        if (!this.mEngineState.isEnglish()) {
            switchCharset();
            return true;
        }
        return true;
    }

    @Override
    protected boolean processKana() {
        if (this.mEngineState.isEnglish()) {
            switchCharset();
            return true;
        }
        return true;
    }

    @Override
    protected boolean processKatakanaHiragana(KeyEvent ev) {
        if (ev == null) {
            return false;
        }
        boolean isPressedAlt = ev.isAltPressed();
        boolean isPressedCtrl = ev.isCtrlPressed();
        if (isPressedAlt && !isPressedCtrl) {
            showSwitchKanaRomanModeDialog();
        } else if (this.mEngineState.isEnglish()) {
            switchCharset();
        }
        return true;
    }

    @Override
    protected boolean processHenkan(KeyEvent ev) {
        if (ev == null || ev.isShiftPressed() || ev.isAltPressed() || ev.isCtrlPressed() || this.mEngineState.isEnglish()) {
            return false;
        }
        if (isRenbun()) {
            if (!this.mCandidatesManager.isFocusCandidate()) {
                processMoveKeyEvent(20);
            }
            processRightKeyEvent();
        } else if (this.mComposingText.size(0) != 0) {
            startConvert(1);
        }
        return true;
    }

    @Override
    protected boolean processMuhenkan(KeyEvent ev) {
        if (ev == null) {
            return false;
        }
        boolean isShift = ev.isShiftPressed();
        if (!ev.isAltPressed() && !ev.isCtrlPressed()) {
            if (this.mComposingText.size(0) == 0) {
                boolean isEnglish = this.mEngineState.isEnglish();
                if ((isShift && !isEnglish) || (!isShift && isEnglish)) {
                    switchCharset();
                }
            } else {
                int convertingForFuncKeyType = getConvertingForFuncKeyType();
                if (isShift) {
                    switch (convertingForFuncKeyType) {
                        case 6:
                        case 8:
                        case 10:
                            converterComposingText(9);
                            break;
                        case 7:
                        case 9:
                        default:
                            converterComposingText(10);
                            break;
                    }
                } else {
                    switch (convertingForFuncKeyType) {
                        case 1:
                        case 2:
                            converterComposingText(3);
                            break;
                        case 3:
                            converterComposingText(4);
                            break;
                        default:
                            converterComposingText(2);
                            break;
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected boolean processDpadLeftInput() {
        if (!isEnableL2Converter()) {
            commitText(false);
            return false;
        }
        processLeftKeyEvent();
        return true;
    }

    @Override
    protected boolean processFunctionConvert(int keycode) {
        switch (keycode) {
            case 136:
                converterComposingText(2);
                break;
            case 137:
                converterComposingText(3);
                break;
            case 138:
                converterComposingText(4);
                break;
            case 139:
                switch (getConvertingForFuncKeyType()) {
                    case 8:
                        converterComposingText(6);
                        break;
                    case 9:
                    default:
                        converterComposingText(10);
                        break;
                    case 10:
                        converterComposingText(8);
                        break;
                }
                break;
            case 140:
                switch (getConvertingForFuncKeyType()) {
                    case 7:
                        converterComposingText(5);
                        break;
                    case 8:
                    default:
                        converterComposingText(9);
                        break;
                    case 9:
                        converterComposingText(7);
                        break;
                }
                break;
        }
        return true;
    }

    @Override
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
                this.mFunfun = 0;
                setFunFun(this.mFunfun);
                layer = 1;
                IWnnImeBase.EngineState state = new IWnnImeBase.EngineState();
                state.convertType = 0;
                updateEngineState(state);
            } else {
                if (this.mEngineState.isEisuKana()) {
                    this.mExactMatchMode = true;
                }
                this.mComposingText.moveCursor(1, 1);
            }
        } else if (this.mComposingText.getCursor(1) < this.mComposingText.size(1)) {
            this.mComposingText.moveCursor(1, 1);
        } else if (this.mEnableFunfun) {
            String composingString = this.mComposingText.toString(1);
            if (!isEnglishPrediction() && (this.mIsInputSequenced || !isAlphabetAll(composingString))) {
                int size = composingString.length();
                boolean change = false;
                boolean infront = false;
                for (int count = 0; count < size && isAlphabetLast(composingString); count++) {
                    int last = composingString.length();
                    change = false;
                    char a = composingString.charAt(last - 1);
                    if (a == STRING_LOWER_N.charAt(0) || a == STRING_UPPER_N.charAt(0)) {
                        change = true;
                    } else if (a == STRING_LOWER_Y.charAt(0) || a == STRING_UPPER_Y.charAt(0)) {
                        infront = true;
                    } else {
                        infront = false;
                    }
                    this.mComposingText.deleteStrSegment(1, last - 1, last - 1);
                    this.mComposingText.setCursor(1, last);
                    composingString = this.mComposingText.toString(1);
                }
                int clearedSize = composingString.length();
                if (size > 0 && clearedSize == 0) {
                    this.mIgnoreCursorMove = true;
                }
                if (change && !infront) {
                    this.mComposingText.insertStrSegment(0, 1, new StrSegment(STRING_HIRAGANA_LETTER_N));
                }
            }
            this.mFunfun++;
            setFunFun(this.mFunfun);
        }
        this.mCommitCount = 0;
        this.mStatus = 3;
        updateViewStatus(layer, true, true);
    }

    @Override
    protected boolean processKeyEventNoInputCandidateShown(IWnnImeEvent wnnEv) {
        KeyEvent ev;
        if (wnnEv == null || (ev = wnnEv.keyEvent) == null) {
            return false;
        }
        int key = ev.getKeyCode();
        if (key == 111 && this.mCandidatesManager.getViewType() == 1) {
            setCandidateIsViewTypeFull(false);
            return true;
        }
        return super.processKeyEventNoInputCandidateShown(wnnEv);
    }

    @Override
    protected boolean processToggleChar(IWnnImeEvent ev) {
        String[] table;
        char top;
        int cursor;
        StrSegment strSegment;
        String prevChar;
        String c;
        if (ev == null || (table = ev.toggleTable) == null) {
            return false;
        }
        commitConvertingText();
        boolean toggled = false;
        if ((this.mStatus & (-17)) == 1 && (cursor = this.mComposingText.getCursor(1)) > 0 && (strSegment = this.mComposingText.getStrSegment(1, cursor - 1)) != null && (prevChar = strSegment.string) != null && (c = searchToggleCharacter(prevChar, table, false)) != null) {
            this.mComposingText.delete(1, false);
            appendToggleString(c);
            toggled = true;
        }
        boolean appendComplete = true;
        if (!toggled) {
            if (!isEnableL2Converter()) {
                commitText(false);
            }
            String str = table[0];
            if (this.mAutoCaps && getShiftKeyState(getCurrentInputEditorInfo()) == 1 && (top = table[0].charAt(0)) <= 127 && Character.isLowerCase(top)) {
                str = Character.toString(Character.toUpperCase(top));
            }
            appendComplete = appendToggleString(str);
        }
        this.mFunfun = 0;
        setFunFun(this.mFunfun);
        if (appendComplete) {
            this.mStatus = 1;
        } else {
            this.mStatus = 3;
        }
        updateViewStatusForPrediction(true, true);
        return true;
    }

    @Override
    protected boolean processHardware12Keyboard(KeyEvent keyEvent) {
        if (!this.mEnableHardware12Keyboard || keyEvent == null) {
            return false;
        }
        int keyCode = keyEvent.getKeyCode();
        if (isHardKeyboard12KeyLongPress(keyCode)) {
            if (keyEvent.getRepeatCount() == 0) {
                keyEvent.startTracking();
            }
            return true;
        }
        Integer code = HW12KEYBOARD_KEYCODE_REPLACE_TABLE.get(Integer.valueOf(keyCode));
        if (code == null) {
            return false;
        }
        if (keyEvent.getRepeatCount() == 0) {
            this.mDefaultSoftKeyboard.onKey(code.intValue(), null);
        }
        return true;
    }

    @Override
    protected void processToggleTimeLimit() {
        if ((this.mStatus & (-17)) == 1) {
            this.mStatus = 259;
            this.mDefaultSoftKeyboard.setKeepShiftMode(true);
            updateViewStatus(this.mTargetLayer, false, false);
            this.mDefaultSoftKeyboard.setKeepShiftMode(false);
        }
    }

    private void processSoftKeyboardCodeWithoutConversion(char[] chars) {
        if (chars != null) {
            appendStrSegment(new StrSegment(chars));
            LetterConverter preConverter = this.mPreConverter;
            boolean completed = false;
            if (preConverter != null) {
                completed = preConverter.convert(this.mComposingText);
            }
            if (!isAlphabetLast(this.mComposingText.toString(1))) {
                commitText(false);
                return;
            }
            if (preConverter != null) {
                if (completed) {
                    commitTextWithoutLastAlphabet();
                    return;
                }
                this.mStatus = 1;
                this.mHandler.removeMessages(1);
                updateViewStatusForPrediction(true, true);
            }
        }
    }

    private void forwardCursor() {
        this.mHandler.removeMessages(1);
        if (!this.mExactMatchMode) {
            int cursor = this.mComposingText.getCursor(1);
            if (this.mAutoCursorMovementSpeed != 0 && this.mComposingText.size(1) == cursor) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), this.mAutoCursorMovementSpeed);
            }
        }
    }

    private void commitTextWithoutLastAlphabet() {
        int layer = this.mTargetLayer;
        StrSegment strSegment = this.mComposingText.getStrSegment(layer, -1);
        if (strSegment != null) {
            String tmp = strSegment.string;
            if (tmp != null && isAlphabetLast(tmp)) {
                this.mComposingText.moveCursor(1, -1);
                commitText(false);
                this.mComposingText.moveCursor(1, 1);
                return;
            }
            commitText(false);
        }
    }

    private void commitTextWithoutLastKana() {
        int layer = this.mTargetLayer;
        StrSegment strSegment = this.mComposingText.getStrSegment(layer, -1);
        if (this.mComposingText.size(1) > 1) {
            if (strSegment != null) {
                String tmp = strSegment.string;
                if (tmp != null) {
                    this.mComposingText.moveCursor(1, -1);
                    commitText(false);
                    this.mComposingText.moveCursor(1, 1);
                    return;
                }
                commitText(false);
                return;
            }
            return;
        }
        updateViewStatus(1, false, true);
    }

    private int commitComposingText(CharSequence string, String stroke) {
        int layer = this.mTargetLayer;
        if (string != null && string.length() > 0) {
            int deleteStrokeLen = calculateSizeOfDeleteStroke(stroke);
            commitTextToInputConnection(string, stroke);
            this.mPrevCommitText.append(string);
            this.mIgnoreCursorMove = true;
            int cursor = this.mComposingText.getCursor(layer);
            if (cursor > 0) {
                int check = cursor - deleteStrokeLen;
                if (deleteStrokeLen > 0 && check > 0 && this.mEnableHeadConv) {
                    this.mComposingText.deleteStrSegment(layer, 0, deleteStrokeLen - 1);
                } else {
                    this.mComposingText.deleteStrSegment(layer, 0, this.mComposingText.getCursor(layer) - 1);
                }
                this.mComposingText.setCursor(layer, this.mComposingText.size(layer));
            }
            this.mExactMatchMode = false;
            this.mFunfun = 0;
            setFunFun(this.mFunfun);
            this.mCommitCount++;
            if (layer == 2 && this.mComposingText.size(layer) == 0) {
                layer = 1;
            }
            boolean committed = false;
            if (string.length() == 1) {
                committed = autoCommitEnglish();
            }
            if (!this.mEnableAutoInsertSpace && 1 == this.mEngineState.language && !this.mEngineState.isSymbolList()) {
                committed = true;
                this.mCandidatesManager.clearCandidates();
            }
            this.mEnableAutoDeleteSpace = true;
            this.mIsInputSequenced = true;
            if (layer == 2) {
                IWnnImeBase.EngineState state = new IWnnImeBase.EngineState();
                state.convertType = 1;
                updateEngineState(state);
                updateViewStatus(layer, committed ? false : true, false);
            } else {
                updateViewStatusForPrediction(committed ? false : true, false);
            }
        }
        return this.mComposingText.size(0) == 0 ? 0 : 3;
    }

    private boolean isAlphabetLast(String str) {
        Matcher m = ENGLISH_CHARACTER_LAST.matcher(str);
        return m.matches();
    }

    private boolean isAlphabetAll(String str) {
        Matcher m = ENGLISH_CHARACTER_ALL.matcher(str);
        return m.matches();
    }

    private void converterComposingText(int type) {
        WnnEngine converter = getConverter();
        if (converter != null) {
            converter.convertGijiStr(this.mComposingText, type);
            this.mFunfun = 0;
            setFunFun(this.mFunfun);
            this.mExactMatchMode = false;
            setConvertingForFuncKeyType(type);
            updateViewStatus(2, false, true);
            this.mCandidatesManager.clearCandidates();
        }
    }

    @Override
    protected boolean isEnableKanaInput() {
        return (!this.mDirectKana || this.mHardKeyboardHidden || this.mEngineState.isEnglish()) ? false : true;
    }

    private boolean isHardKeyboard12KeyLongPress(int keyCode) {
        switch (keyCode) {
            case 1:
            case 2:
            case 67:
                return true;
            default:
                return false;
        }
    }

    private void showSwitchKanaRomanModeDialog() {
        BaseInputView baseInputView = (BaseInputView) this.mDefaultSoftKeyboard.getCurrentView();
        if (baseInputView != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(baseInputView.getContext());
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.ti_dialog_button_ok_txt, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    IWnnImeJaJp.this.switchKanaRomanMode();
                }
            });
            builder.setNegativeButton(R.string.ti_dialog_button_cancel_txt, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
            String message = baseInputView.getResources().getString(R.string.ti_switch_kana_input_mode_txt);
            if (this.mDirectKana) {
                message = baseInputView.getResources().getString(R.string.ti_switch_roman_input_mode_txt);
            }
            builder.setMessage(message);
            builder.setTitle(R.string.ti_preference_kana_roman_input_mode_title_txt);
            baseInputView.showDialog(builder);
        }
    }

    private int calculateSizeOfDeleteStroke(String stroke) {
        int keyLength;
        if (isEnglishPrediction() || stroke == null) {
            return 0;
        }
        int len = stroke.length();
        String forecastKey = this.mConverterIWnn.getForecastKey();
        if (forecastKey != null && len > (keyLength = forecastKey.length())) {
            int fullStrokeLen = len;
            len = keyLength;
            String fullStroke = this.mComposingText.toString(1);
            if (fullStroke != null) {
                String stroke2 = stroke.toLowerCase();
                String fullStroke2 = fullStroke.toLowerCase();
                if (fullStrokeLen > fullStroke2.length()) {
                    fullStrokeLen = fullStroke2.length();
                }
                len = keyLength;
                while (len < fullStrokeLen && stroke2.charAt(len) == fullStroke2.charAt(len)) {
                    len++;
                }
            }
        }
        return len;
    }

    private boolean isInputModeHalfWidthCharacter() {
        int keymode = this.mDefaultSoftKeyboard.getKeyMode();
        return keymode == 0 || keymode == 5 || keymode == 1 || keymode == 2;
    }

    private String getCurrentSpaceString() {
        if (!isInputModeHalfWidthCharacter() && !this.mEnableHalfSpace) {
            return STRING_FULL_WIDTH_SPACE;
        }
        return " ";
    }
}
