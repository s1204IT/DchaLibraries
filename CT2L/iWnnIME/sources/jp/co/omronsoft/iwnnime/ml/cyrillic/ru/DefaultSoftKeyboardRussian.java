package jp.co.omronsoft.iwnnime.ml.cyrillic.ru;

import jp.co.omronsoft.iwnnime.ml.DefaultSoftKeyboard;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.Keyboard;
import jp.co.omronsoft.iwnnime.ml.latin.DefaultSoftKeyboardLatin;

public class DefaultSoftKeyboardRussian extends DefaultSoftKeyboardLatin {
    private static final int RUSSIAN_KEYMODE_BIT_UNLIMITED;
    private static final int[] RUSSIAN_MODE_ALL_TABLE = {3, 0, 1, 2, 8};
    private static final int[] RUSSIAN_MODE_CYCLE_TABLE = {3, 0, 1};
    private static final int[] RUSSIAN_MODE_DEFAULT_TABLE = {3};

    static {
        int unlimitedBit = 0;
        int[] arr$ = RUSSIAN_MODE_ALL_TABLE;
        for (int mode : arr$) {
            if (mode != 2) {
                unlimitedBit |= 1 << mode;
            }
        }
        RUSSIAN_KEYMODE_BIT_UNLIMITED = unlimitedBit;
    }

    public DefaultSoftKeyboardRussian(IWnnIME wnn) {
        super(wnn);
    }

    @Override
    protected void onKeyCharacteristic(int primaryCode, int[] keyCodes) {
        switch (primaryCode) {
            case DefaultSoftKeyboard.KEYCODE_SWITCH_VOICE:
                changeKeyMode(8);
                break;
            default:
                super.onKeyCharacteristic(primaryCode, keyCodes);
                break;
        }
    }

    @Override
    protected int getUnlimitedKeyMode() {
        return RUSSIAN_KEYMODE_BIT_UNLIMITED;
    }

    @Override
    protected int[] getModePassWordTable() {
        return RUSSIAN_MODE_CYCLE_TABLE;
    }

    @Override
    protected int[] getModeDefaultTable() {
        return RUSSIAN_MODE_DEFAULT_TABLE;
    }

    @Override
    protected int[] getModeCycleTable() {
        return RUSSIAN_MODE_CYCLE_TABLE;
    }

    @Override
    protected int[] getAllModeTable() {
        return RUSSIAN_MODE_ALL_TABLE;
    }

    @Override
    protected boolean isAutoCapsOn() {
        return this.mEnableAutoCaps && (mCurrentKeyMode == 3 || mCurrentKeyMode == 0);
    }

    @Override
    protected boolean isSoftLockEnabled() {
        return mCurrentKeyMode == 3 || mCurrentKeyMode == 0;
    }

    @Override
    protected int getModeCondition() {
        int result = super.getModeCondition();
        if (result != 0) {
            return result;
        }
        if (mCurrentKeyMode == 3) {
            result = Keyboard.CONDITION_MODE_CYRILLIC;
        }
        return result;
    }
}
