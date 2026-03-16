package jp.co.omronsoft.iwnnime.ml.zh;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import java.util.ArrayList;
import java.util.Locale;
import jp.co.omronsoft.iwnnime.ml.DefaultSoftKeyboard;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeBase;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.Keyboard;
import jp.co.omronsoft.iwnnime.ml.MultiTouchKeyboardView;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnKeyboardFactory;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelPrefFragment;

public class DefaultSoftKeyboardZh extends DefaultSoftKeyboard {
    private static final int CN_KEYMODE_BIT_UNLIMITED;
    private static final String TAG = "DefaultSoftKeyboardZh";
    private Locale mCurrentLocal;
    private static final String[] CYCLE_TABLE_STOP = {"，", "。"};
    private static final int[] CN_MODE_ALL_TABLE = {3, 0, 1, 2, 8};
    private static final int[] CN_MODE_CYCLE_TABLE = {3, 0, 1};
    private static final int[] CN_MODE_PASSWORD_TABLE = {0, 1};
    private static final int[] CN_MODE_DEFAULT_TABLE = {3};

    static {
        int unlimitedBit = 0;
        int[] arr$ = CN_MODE_ALL_TABLE;
        for (int mode : arr$) {
            if (mode != 2) {
                unlimitedBit |= 1 << mode;
            }
        }
        CN_KEYMODE_BIT_UNLIMITED = unlimitedBit;
    }

    public DefaultSoftKeyboardZh(IWnnIME wnn, int languageType) {
        super(wnn);
        this.mLanguageType = languageType;
        switch (languageType) {
            case 14:
                this.mCurrentLocal = Locale.SIMPLIFIED_CHINESE;
                break;
            case 15:
                this.mCurrentLocal = Locale.TRADITIONAL_CHINESE;
                break;
        }
    }

    @Override
    public void changeKeyMode(int keyMode) {
        if (this.mWnn != null) {
            if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                this.mKeyboardView.setShifted(false);
                ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLock(false);
            }
            commitText();
            this.mShiftOn = 0;
            this.mCapsLock = false;
            this.mPrevInputKeyCode = 0;
            if (keyMode == 8) {
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.VOICE_INPUT));
                return;
            }
            mCurrentKeyMode = keyMode;
            Keyboard keyboard = getModeChangeKeyboard(keyMode);
            if (keyboard != null) {
                changeKeyboard(keyboard);
                int mode = 1;
                switch (keyMode) {
                    case 0:
                        mode = 2;
                        break;
                    case 3:
                        mode = 0;
                        break;
                }
                IWnnImeBase currentIme = this.mWnn.getCurrentIWnnIme();
                if (currentIme != null) {
                    if (!currentIme.isEnablePrediction()) {
                        if (keyMode == getDefaultKeyMode()) {
                            mode = 0;
                        } else {
                            mode = 1;
                        }
                    }
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, mode));
                } else {
                    return;
                }
            }
            super.changeKeyMode(keyMode);
        }
    }

    @Override
    public View initView(int width) {
        if (IWnnIME.isDebugging()) {
            Log.d(TAG, "initView()" + (this.mKeyboardView != null));
        }
        View view = super.initView(width);
        boolean isKeep = false;
        if (this.mWnn != null) {
            isKeep = this.mWnn.isKeepInput();
        }
        WnnKeyboardFactory keyboard = this.mKeyboard[this.mDisplayMode][this.mCurrentKeyboardType][this.mShiftOn][mCurrentKeyMode][0];
        if (keyboard != null && this.mKeyboardView != null) {
            if (isKeep) {
                this.mCurrentKeyboard = keyboard.getKeyboard(mCurrentKeyMode, getKeyboardCondition(false, false));
                if (this.mIsSymbolKeyboard) {
                    setSymbolKeyboard();
                } else {
                    setNormalKeyboard();
                    if ((this.mKeyboardView instanceof MultiTouchKeyboardView) && this.mCapsLock) {
                        ((MultiTouchKeyboardView) this.mKeyboardView).setShifted(true);
                        if (isSoftLockEnabled()) {
                            ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLock(true);
                        } else {
                            ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLockMode(true);
                        }
                    }
                }
            } else {
                changeKeyMode(mCurrentKeyMode);
            }
        }
        if (IWnnIME.isDebugging()) {
            Log.d(TAG, "initView(): width=" + width + ", kbdView=" + this.mKeyboardView);
        }
        return view;
    }

    @Override
    protected void setPreferencesCharacteristic(SharedPreferences pref, EditorInfo editor) {
        if (editor != null) {
            super.setPreferencesCharacteristic(pref, editor);
            if (this.mWnn != null && !this.mWnn.isKeepInput()) {
                if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                    this.mKeyboardView.setShifted(false);
                    ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLock(false);
                    this.mKeyboardView.setIsInputTypeNull(this.mIsInputTypeNull);
                }
                Resources res = this.mWnn.getResources();
                this.mEnableAutoCaps = this.mAutoCaps && pref.getBoolean(ControlPanelPrefFragment.AUTO_CAPS_KEY, res.getBoolean(R.bool.auto_caps_default_value));
                int inputType = editor.inputType;
                int imeOptions = editor.imeOptions;
                if (inputType != this.mLastInputType || imeOptions != this.mLastImeOptions) {
                    setDefaultKeyboard();
                    this.mLastInputType = inputType;
                    this.mLastImeOptions = imeOptions;
                }
                setShiftByEditorInfo(this.mForceShift);
                setStatusIcon();
            }
        }
    }

    @Override
    public boolean onKey(int primaryCode, int[] keyCodes) {
        boolean ret = super.onKey(primaryCode, keyCodes);
        if (!ret) {
            switch (primaryCode) {
                case DefaultSoftKeyboard.KEYCODE_SWITCH_VOICE:
                    changeKeyMode(8);
                    break;
                default:
                    if (this.mWnn != null) {
                        switch (primaryCode) {
                            case DefaultSoftKeyboard.KEYCODE_TOGGLE_STOP:
                                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.TOGGLE_CHAR, CYCLE_TABLE_STOP));
                                break;
                            case DefaultSoftKeyboard.KEYCODE_TOGGLE_INVERTED_EXCLAMATION:
                                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.TOGGLE_CHAR, CYCLE_TABLE_INVERT_EXCLAMATION));
                                break;
                            case DefaultSoftKeyboard.KEYCODE_TOGGLE_EXCLAMATION:
                                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.TOGGLE_CHAR, CYCLE_TABLE_EXCLAMATION));
                                break;
                            case DefaultSoftKeyboard.KEYCODE_TOGGLE_COMMA:
                                if (this.mPrevInputKeyCode != primaryCode) {
                                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.TOUCH_OTHER_KEY));
                                    commitText();
                                }
                                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.TOGGLE_CHAR, CYCLE_TABLE_HALF_PERIOD));
                                break;
                        }
                    }
                    this.mPrevInputKeyCode = primaryCode;
                    break;
            }
            if (!this.mCapsLock && primaryCode != -1) {
                setShiftByEditorInfo(false);
            }
        }
        return true;
    }

    @Override
    protected int getUnlimitedKeyMode() {
        return CN_KEYMODE_BIT_UNLIMITED;
    }

    @Override
    protected int[] getModeCycleTable() {
        return CN_MODE_CYCLE_TABLE;
    }

    @Override
    protected int[] getModePassWordTable() {
        return CN_MODE_PASSWORD_TABLE;
    }

    @Override
    protected int[] getModeDefaultTable() {
        return CN_MODE_DEFAULT_TABLE;
    }

    @Override
    protected int[] getAllModeTable() {
        return CN_MODE_ALL_TABLE;
    }

    @Override
    protected boolean isSoftLockEnabled() {
        return (this.mLanguageType == 14 && mCurrentKeyMode == 3) || mCurrentKeyMode == 0;
    }

    @Override
    protected void setStatusIcon() {
        if (this.mWnn != null && this.mTextView != null) {
            switch (mCurrentKeyMode) {
                case 0:
                    this.mTextView.setText(R.string.ti_key_switch_half_alphabet_txt);
                    break;
                case 3:
                    if (this.mLanguageType == 14) {
                        this.mTextView.setText(R.string.ti_key_switch_chinese_cn_txt);
                    } else if (this.mLanguageType == 15) {
                        this.mTextView.setText(R.string.ti_key_switch_chinese_tw_txt);
                    }
                    break;
            }
            this.mWnn.showStatusIcon(0);
        }
    }

    @Override
    protected void nextKeyMode() {
        if (isSoftLockEnabled() && this.mShiftOn == 1 && !this.mCapsLock) {
            this.mShiftOn = 0;
        }
        super.nextKeyMode();
    }

    @Override
    protected ArrayList<Integer> getLongPressMenuItemsUserDic() {
        ArrayList<Integer> list = new ArrayList<>();
        if (this.mCurrentLocal == Locale.SIMPLIFIED_CHINESE) {
            list.add(9);
        } else {
            list.add(10);
        }
        list.add(5);
        return list;
    }

    @Override
    protected void createKeyboards() {
        if (this.mWnn != null) {
            super.createKeyboards();
            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_QWERTY));
        }
    }

    @Override
    protected int getModeCondition() {
        int result = super.getModeCondition();
        if (result != 0) {
            return result;
        }
        if (mCurrentKeyMode == 3) {
            if (this.mLanguageType == 14) {
                result = Keyboard.CONDITION_MODE_PINYIN;
            } else if (this.mLanguageType == 15) {
                result = Keyboard.CONDITION_MODE_BOPOMOFO;
            }
        }
        return result;
    }
}
