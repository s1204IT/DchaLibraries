package jp.co.omronsoft.iwnnime.ml.hangul;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import java.util.ArrayList;
import jp.co.omronsoft.iwnnime.ml.DefaultSoftKeyboard;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeBase;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.Keyboard;
import jp.co.omronsoft.iwnnime.ml.MultiTouchKeyboardView;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnKeyboardFactory;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelPrefFragment;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class DefaultSoftKeyboardHangul extends DefaultSoftKeyboard {
    private static final int KO_KEYMODE_BIT_UNLIMITED;
    private static final String TAG = "DefaultSoftKeyboardHangul";
    private static final String[] CYCLE_TABLE_COMMA = {iWnnEngine.DECO_OPERATION_SEPARATOR, "."};
    private static final int[] KO_MODE_ALL_TABLE = {3, 0, 1, 2, 8};
    private static final int[] KO_MODE_CYCLE_TABLE = {3, 0, 1};
    private static final int[] KO_MODE_PASSWORD_TABLE = {0, 1};
    private static final int[] KO_MODE_DEFAULT_TABLE = {3};

    static {
        int unlimitedBit = 0;
        int[] arr$ = KO_MODE_ALL_TABLE;
        for (int mode : arr$) {
            if (mode != 2) {
                unlimitedBit |= 1 << mode;
            }
        }
        KO_KEYMODE_BIT_UNLIMITED = unlimitedBit;
    }

    public DefaultSoftKeyboardHangul(IWnnIME wnn) {
        super(wnn);
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
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, mode));
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
                    if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                        if (this.mCapsLock) {
                            ((MultiTouchKeyboardView) this.mKeyboardView).setShifted(true);
                            if (isSoftLockEnabled()) {
                                ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLock(true);
                            } else {
                                ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLockMode(true);
                            }
                        }
                    } else {
                        setShifted(this.mShiftOn);
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
                                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.TOGGLE_CHAR, CYCLE_TABLE_COMMA));
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
        return KO_KEYMODE_BIT_UNLIMITED;
    }

    @Override
    protected int[] getModeCycleTable() {
        return KO_MODE_CYCLE_TABLE;
    }

    @Override
    protected int[] getModePassWordTable() {
        return KO_MODE_PASSWORD_TABLE;
    }

    @Override
    protected int[] getModeDefaultTable() {
        return KO_MODE_DEFAULT_TABLE;
    }

    @Override
    protected int[] getAllModeTable() {
        return KO_MODE_ALL_TABLE;
    }

    @Override
    protected boolean isSoftLockEnabled() {
        return mCurrentKeyMode == 3 || mCurrentKeyMode == 0;
    }

    @Override
    protected void setStatusIcon() {
        if (this.mWnn != null && this.mTextView != null) {
            switch (mCurrentKeyMode) {
                case 0:
                    this.mTextView.setText(R.string.ti_key_switch_half_alphabet_txt);
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
        return new ArrayList<Integer>() {
            {
                add(11);
                add(5);
            }
        };
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
            result = Keyboard.CONDITION_MODE_HANGUL;
        }
        return result;
    }
}
