package jp.co.omronsoft.iwnnime.ml.latin.es;

import android.content.SharedPreferences;
import android.view.inputmethod.EditorInfo;
import jp.co.omronsoft.iwnnime.ml.ComposingText;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.latin.DefaultSoftKeyboardLatin;

public class DefaultSoftKeyboardSpanish extends DefaultSoftKeyboardLatin {
    private boolean mIsInputingInvertChar;

    public DefaultSoftKeyboardSpanish(IWnnIME wnn) {
        super(wnn);
        this.mIsInputingInvertChar = false;
    }

    @Override
    public void setPreferences(SharedPreferences pref, EditorInfo editor) {
        this.mIsInputingInvertChar = false;
        super.setPreferences(pref, editor);
    }

    @Override
    public void onUpdateState() {
        super.onUpdateState();
        if (this.mIsInputingInvertChar) {
            if (this.mNoInput) {
                this.mIsInputingInvertChar = false;
            } else if (this.mWnn != null) {
                ComposingText composingText = this.mWnn.getComposingText();
                int lastStrPos = composingText.size(1) - 1;
                String lastStr = composingText.toString(1, lastStrPos, lastStrPos);
                if (!CYCLE_TABLE_INVERT_EXCLAMATION[0].equals(lastStr) && !CYCLE_TABLE_INVERT_EXCLAMATION[1].equals(lastStr)) {
                    this.mIsInputingInvertChar = false;
                }
            }
            if (!this.mCapsLock) {
                setShiftByEditorInfo(false);
            }
        }
    }

    @Override
    public boolean onKey(int primaryCode, int[] keyCodes) {
        this.mIsInputingInvertChar = primaryCode == -118;
        return super.onKey(primaryCode, keyCodes);
    }

    @Override
    protected void setShiftByEditorInfo(boolean force) {
        if (!this.mIsInputingInvertChar) {
            super.setShiftByEditorInfo(force);
        }
    }
}
