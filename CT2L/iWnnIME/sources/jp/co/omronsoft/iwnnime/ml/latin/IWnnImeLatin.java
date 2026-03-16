package jp.co.omronsoft.iwnnime.ml.latin;

import android.view.inputmethod.InputConnection;
import jp.co.omronsoft.iwnnime.ml.DefaultSoftKeyboard;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeBase;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;
import jp.co.omronsoft.iwnnime.ml.cyrillic.ru.DefaultSoftKeyboardRussian;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnSymbolEngine;
import jp.co.omronsoft.iwnnime.ml.latin.es.DefaultSoftKeyboardSpanish;

public class IWnnImeLatin extends IWnnImeBase {
    public IWnnImeLatin(IWnnIME wnn, int languageType) {
        super(wnn, languageType);
        this.mConverterSymbolEngineBack = new IWnnSymbolEngine(wnn, null);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IWnnImeBase.EngineState state = new IWnnImeBase.EngineState();
        state.keyboard = 1;
        updateEngineState(state);
        this.mConverterIWnn.setConvertedCandidateEnabled(true);
    }

    @Override
    public boolean isEnableL2Converter() {
        return getConverter() != null && this.mEnableConverter && this.mEnablePrediction;
    }

    @Override
    protected DefaultSoftKeyboard createDefaultSoftKeyboard() {
        switch (this.mLanguageType) {
            case 7:
                DefaultSoftKeyboard ret = new DefaultSoftKeyboardSpanish(this.mWnn);
                return ret;
            case 8:
            case 9:
            default:
                DefaultSoftKeyboard ret2 = new DefaultSoftKeyboardLatin(this.mWnn);
                return ret2;
            case 10:
                DefaultSoftKeyboard ret3 = new DefaultSoftKeyboardRussian(this.mWnn);
                return ret3;
        }
    }

    @Override
    protected void updateViewStatusForPrediction(boolean updateCandidates, boolean updateEmptyText) {
        updateViewStatus(1, updateCandidates, updateEmptyText);
    }

    @Override
    protected void updateViewStatus(int layer, boolean updateCandidates, boolean updateEmptyText) {
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
                    if (cursor < this.mComposingText.size(1)) {
                        WnnUtility.setSpan(this.mDisplayText, SPAN_EXACT_BGCOLOR_HL, 0, cursor, 33);
                        highlightEnd = cursor;
                    } else if (this.mStatus == 1 && this.mHandler.hasMessages(1)) {
                        WnnUtility.setSpan(this.mDisplayText, this.mSpanToggleCharacterBgHl, cursor - 1, cursor, 33);
                        WnnUtility.setSpan(this.mDisplayText, this.mSpanToggleCharacterText, cursor - 1, cursor, 33);
                    }
                    if (highlightEnd > 0) {
                        WnnUtility.setSpan(this.mDisplayText, SPAN_REMAIN_BGCOLOR_HL, highlightEnd, this.mComposingText.toString(layer).length(), 33);
                        WnnUtility.setSpan(this.mDisplayText, SPAN_TEXTCOLOR, 0, this.mComposingText.toString(layer).length(), 33);
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
        this.mComposingText.setCursor(1, this.mComposingText.size(1));
        int layer = this.mTargetLayer;
        int cursor = this.mComposingText.getCursor(layer);
        String tmp = this.mComposingText.toString(layer, 0, cursor - 1);
        commitTextLearnProcess(learn, tmp, tmp);
        CharSequence commitText = convertComposingToCommitText(this.mComposingText, layer, 0, cursor - 1);
        return commitTextThroughInputConnection(commitText);
    }

    @Override
    protected void commitText(String str) {
        this.mFunfun = 0;
        setFunFun(this.mFunfun);
        if (str.length() > 0) {
            commitTextToInputConnection(str, null);
            this.mPrevCommitText.append(str);
            this.mIgnoreCursorMove = true;
        }
        this.mEnableAutoDeleteSpace = true;
        updateViewStatusForPrediction(false, false);
    }

    @Override
    protected void setDictionary() {
        if (this.mWnn != null) {
            int language = this.mLanguageType;
            int dictionary = 0;
            switch (this.mLanguageType) {
                case 10:
                    int keymode = this.mDefaultSoftKeyboard.getKeyMode();
                    if (keymode == 0) {
                        language = 1;
                        this.mConverterIWnn.setConvertedCandidateEnabled(true);
                    }
                    break;
            }
            switch (this.mEngineState.preferenceDictionary) {
                case 3:
                    dictionary = 5;
                    break;
            }
            this.mConverterIWnn.setDictionary(language, dictionary, this.mWnn.hashCode());
        }
    }

    @Override
    protected void autoCommitEnglishCharacteristic(CharSequence str) {
        commitTextToInputConnection(str, null);
        this.mPrevCommitText.append(str);
        this.mIgnoreCursorMove = true;
        this.mEnableAutoDeleteSpace = false;
    }

    @Override
    protected void commitVoiceResult(String result) {
        clearCommitInfo();
        initCommitInfoForWatchCursor();
        commitSpaceJustOne();
        commitText(result);
        checkCommitInfo();
    }

    @Override
    protected boolean isRenbun() {
        return false;
    }

    @Override
    protected void startConvert(int convertType) {
    }

    @Override
    protected boolean isEnglishPrediction() {
        return true;
    }

    @Override
    protected void commitConvertingText() {
    }

    @Override
    protected void processRightKeyEvent() {
        if (this.mCandidatesManager.isFocusCandidate()) {
            processMoveKeyEvent(22);
            return;
        }
        int layer = this.mTargetLayer;
        if (this.mComposingText.getCursor(1) < this.mComposingText.size(1)) {
            this.mComposingText.moveCursor(1, 1);
        } else if (this.mEnableFunfun) {
            this.mFunfun++;
            setFunFun(this.mFunfun);
        }
        this.mStatus = 3;
        updateViewStatus(layer, true, true);
    }

    @Override
    protected void processLeftKeyEvent() {
        processMoveKeyEvent(21);
    }

    @Override
    protected boolean processMoveHome() {
        if (this.mCandidatesManager.isFocusCandidate()) {
            processMoveKeyEvent(122);
            return true;
        }
        commitText(false);
        return false;
    }

    @Override
    protected boolean processMoveEnd() {
        if (this.mCandidatesManager.isFocusCandidate()) {
            processMoveKeyEvent(123);
            return true;
        }
        return true;
    }

    @Override
    protected void processKeyboardSpaceKey() {
        if (this.mComposingText.size(0) == 0) {
            this.mCandidatesManager.clearCandidates();
            commitText(" ");
            breakSequence();
        } else {
            initCommitInfoForWatchCursor();
            commitText(true);
            commitSpaceJustOne();
            checkCommitInfo();
        }
    }

    @Override
    protected boolean processToggleReverseChar(IWnnImeEvent ev) {
        return false;
    }

    @Override
    protected boolean processReplaceChar(IWnnImeEvent ev) {
        return false;
    }

    @Override
    protected boolean processConvert() {
        return false;
    }
}
