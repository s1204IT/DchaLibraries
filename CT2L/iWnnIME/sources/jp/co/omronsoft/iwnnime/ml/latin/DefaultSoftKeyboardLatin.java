package jp.co.omronsoft.iwnnime.ml.latin;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import java.util.ArrayList;
import jp.co.omronsoft.iwnnime.ml.DefaultSoftKeyboard;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.Keyboard;
import jp.co.omronsoft.iwnnime.ml.MultiTouchKeyboardView;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnKeyboardFactory;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelPrefFragment;
import jp.co.omronsoft.iwnnime.ml.cyrillic.ru.DefaultSoftKeyboardRussian;

public class DefaultSoftKeyboardLatin extends DefaultSoftKeyboard {
    private static final int LATIN_KEYMODE_BIT_UNLIMITED;
    private static final int[] LATIN_MODE_ALL_TABLE = {0, 1, 2, 8};
    private static final int[] LATIN_MODE_CYCLE_TABLE = {0, 1};
    private static final int[] LATIN_MODE_DEFAULT_TABLE = {0};
    private static final String TAG = "DefaultSoftKeyboardLatin";

    static {
        int unlimitedBit = 0;
        int[] arr$ = LATIN_MODE_ALL_TABLE;
        for (int mode : arr$) {
            if (mode != 2) {
                unlimitedBit |= 1 << mode;
            }
        }
        LATIN_KEYMODE_BIT_UNLIMITED = unlimitedBit;
    }

    public DefaultSoftKeyboardLatin(IWnnIME wnn) {
        super(wnn);
    }

    @Override
    public void dismissPopupKeyboard() {
        try {
            if (this.mKeyboardView != null) {
                this.mKeyboardView.handleBack();
            }
        } catch (Exception ex) {
            Log.e(TAG, "dismissPopupKeyboard " + ex.toString());
        }
        super.dismissPopupKeyboard();
    }

    @Override
    public void changeKeyMode(int keyMode) {
        if (this.mWnn != null) {
            if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                this.mKeyboardView.setShifted(false);
                ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLock(false);
            }
            this.mShiftOn = 0;
            this.mCapsLock = false;
            this.mPrevInputKeyCode = 0;
            boolean isRussian = this instanceof DefaultSoftKeyboardRussian;
            if (isRussian) {
                commitText();
            }
            if (keyMode == 8) {
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.VOICE_INPUT));
                return;
            }
            mCurrentKeyMode = keyMode;
            Keyboard keyboard = getModeChangeKeyboard(keyMode);
            if (keyboard != null) {
                changeKeyboard(keyboard);
                if (isRussian) {
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
        if (!isKeep) {
            this.mCurrentKeyboardType = 0;
            this.mShiftOn = 0;
        }
        WnnKeyboardFactory keyboard = this.mKeyboard[this.mDisplayMode][this.mCurrentKeyboardType][this.mShiftOn][mCurrentKeyMode][0];
        if (keyboard != null) {
            if (isKeep) {
                this.mCurrentKeyboard = keyboard.getKeyboard(mCurrentKeyMode, getKeyboardCondition(false, false));
                if (this.mIsSymbolKeyboard) {
                    setSymbolKeyboard();
                } else {
                    setNormalKeyboard();
                    setShifted(this.mShiftOn);
                }
            } else {
                this.mCurrentKeyboard = null;
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
                if (this.mKeyboardView != null) {
                    this.mKeyboardView.setIsInputTypeNull(this.mIsInputTypeNull);
                }
                Resources res = this.mWnn.getResources();
                this.mEnableAutoCaps = this.mAutoCaps && pref.getBoolean(ControlPanelPrefFragment.AUTO_CAPS_KEY, res.getBoolean(R.bool.auto_caps_default_value));
                int inputType = editor.inputType;
                int imeOptions = editor.imeOptions;
                boolean hasInputTypeChanged = inputType != this.mLastInputType;
                boolean hasImeOptionsChanged = imeOptions != this.mLastImeOptions;
                if (hasInputTypeChanged || isAutoCapsOn() || hasImeOptionsChanged) {
                    if (hasInputTypeChanged || hasImeOptionsChanged) {
                        this.mCurrentKeyboardType = 0;
                        setDefaultKeyboard();
                    }
                    if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                        ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLock(false);
                    }
                    setShiftByEditorInfo(this.mForceShift);
                    this.mLastInputType = inputType;
                    this.mLastImeOptions = imeOptions;
                } else if (isShiftOnUnlocked()) {
                    setShifted(1);
                }
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
        return LATIN_KEYMODE_BIT_UNLIMITED;
    }

    @Override
    protected int[] getModeCycleTable() {
        return LATIN_MODE_CYCLE_TABLE;
    }

    @Override
    protected int[] getModePassWordTable() {
        return LATIN_MODE_CYCLE_TABLE;
    }

    @Override
    protected int[] getModeDefaultTable() {
        return LATIN_MODE_DEFAULT_TABLE;
    }

    @Override
    protected int[] getAllModeTable() {
        return LATIN_MODE_ALL_TABLE;
    }

    @Override
    protected boolean isSoftLockEnabled() {
        return mCurrentKeyMode == 0;
    }

    @Override
    protected void setStatusIcon() {
        if (this.mWnn != null && this.mTextView != null) {
            this.mTextView.setText(R.string.ti_key_switch_half_alphabet_txt);
            this.mWnn.showStatusIcon(0);
        }
    }

    @Override
    protected ArrayList<Integer> getLongPressMenuItemsUserDic() {
        ArrayList<Integer> list = new ArrayList<>();
        int local = getCurrentLocal();
        if (local == 10) {
            list.add(7);
            list.add(5);
        } else if (local == 2) {
            list.add(8);
        } else {
            list.add(5);
        }
        return list;
    }

    @Override
    public void setHardKeyboardHidden(boolean hidden) {
        if (this.mWnn != null && this.mHardKeyboardHidden != hidden) {
            this.mLastImeOptions = -1;
            if (this.mWnn.isInputViewShown() && this.mIsImeOptionsForceAscii) {
                changeKeyMode(0);
            }
        }
        this.mHardKeyboardHidden = hidden;
    }

    private int getCurrentLocal() {
        int tmpLanguageType = WnnUtility.getCurrentLanguageType(this.mWnn);
        switch (tmpLanguageType) {
            case 2:
                return 2;
            case 10:
                return 10;
            default:
                return 1;
        }
    }

    private boolean isShiftOnUnlocked() {
        return !this.mCapsLock && this.mShiftOn == 1;
    }
}
