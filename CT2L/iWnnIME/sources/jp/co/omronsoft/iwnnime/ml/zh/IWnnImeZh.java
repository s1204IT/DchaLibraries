package jp.co.omronsoft.iwnnime.ml.zh;

import android.view.inputmethod.InputConnection;
import jp.co.omronsoft.iwnnime.ml.DefaultSoftKeyboard;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeBase;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnSymbolEngine;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;

public class IWnnImeZh extends IWnnImeBase {
    public IWnnImeZh(IWnnIME wnn, int languageType) {
        super(wnn, languageType);
        String localeString = LanguageManager.getChosenLocale(this.mLanguageType).toString();
        this.mConverterSymbolEngineBack = new IWnnSymbolEngine(wnn, localeString);
    }

    @Override
    public void hideWindow() {
        super.hideWindow();
        this.mDefaultSoftKeyboard.onUpdateState();
    }

    @Override
    protected DefaultSoftKeyboard createDefaultSoftKeyboard() {
        return new DefaultSoftKeyboardZh(this.mWnn, this.mLanguageType);
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
                    if (this.mExactMatchMode) {
                        WnnUtility.setSpan(this.mDisplayText, SPAN_EXACT_BGCOLOR_HL, 0, cursor, 33);
                        highlightEnd = cursor;
                    } else if (layer == 2) {
                        highlightEnd = this.mComposingText.toString(layer, 0, 0).length();
                        WnnUtility.setSpan(this.mDisplayText, SPAN_CONVERT_BGCOLOR_HL, 0, highlightEnd, 33);
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
    protected int getEngineStateDefaultLanguage() {
        return 5;
    }

    @Override
    protected int commitText(boolean learn) {
        int layer = this.mTargetLayer;
        int cursor = this.mComposingText.getCursor(layer);
        String tmp = this.mComposingText.toString(layer, 0, cursor - 1);
        commitTextLearnProcess(learn, tmp, tmp);
        CharSequence commitText = convertComposingToCommitText(this.mComposingText, layer, 0, cursor - 1);
        return commitTextThroughInputConnection(commitText);
    }

    @Override
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
        if (!isEnableL2Converter() || this.mEngineState.isEnglish()) {
            commitText(false);
            return false;
        }
        processLeftKeyEvent();
        return true;
    }
}
