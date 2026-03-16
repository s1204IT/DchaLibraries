package jp.co.omronsoft.iwnnime.ml;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import com.android.common.speech.LoggingEvents;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import jp.co.omronsoft.iwnnime.ml.Keyboard;
import jp.co.omronsoft.iwnnime.ml.KeyboardView;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelPrefFragment;
import jp.co.omronsoft.iwnnime.ml.controlpanel.UserDictionaryToolsActivity;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;
import jp.co.omronsoft.iwnnime.ml.jajp.DefaultSoftKeyboardJAJP;

public class DefaultSoftKeyboard implements KeyboardView.OnKeyboardActionListener, OnFlickKeyboardActionListener, View.OnTouchListener, View.OnHoverListener {
    private static final int CORRECTION_SIZE = 1;
    private static final int DELAY_TIME_ANNOUNCE_STATE_KEYBOARD = 30;
    public static final int FLICK_NOT_PRESS = -99999;
    protected static final int IME_OPTIONS_INIT = -1;
    private static final int INDICATOR_HEIGHT = 1;
    protected static final int INPUT_STATE_INPUTING = 1;
    protected static final int INPUT_STATE_NO = 0;
    protected static final int INVALID_KEYMODE = -1;
    public static final int KEYBOARD_12KEY = 1;
    public static final int KEYBOARD_50KEY = 3;
    protected static final int KEYBOARD_FACTORY_SIZE_INPUT_COLUMN = 1;
    protected static final int KEYBOARD_FACTORY_SIZE_NON_ORIGINAL_MODE_COLUMN = 3;
    protected static final int KEYBOARD_FACTORY_SIZE_ROTATION_COLUMN = 2;
    protected static final int KEYBOARD_FACTORY_SIZE_SHIFT_COLUMN = 2;
    protected static final int KEYBOARD_FACTORY_SIZE_TYPE_COLUMN = 1;
    protected static final int KEYBOARD_FACTORY_SIZE_VOICE_COLUMN = 2;
    public static final int KEYBOARD_QWERTY = 0;
    protected static final int KEYBOARD_SHIFT_OFF = 0;
    protected static final int KEYBOARD_SHIFT_ON = 1;
    public static final int KEYCODE_4KEY_CLEAR = -234;
    public static final int KEYCODE_4KEY_KEYBOAD = -231;
    public static final int KEYCODE_BACKSPACE = -100;
    public static final int KEYCODE_CLOSE_WINDOWS = -417;
    public static final int KEYCODE_DOWN = -236;
    public static final int KEYCODE_DUMMY_KEY = -99999;
    public static final int KEYCODE_EISU_KANA = -305;
    public static final int KEYCODE_ENTER = -101;
    public static final int KEYCODE_JP12_0 = -210;
    public static final int KEYCODE_JP12_1 = -201;
    public static final int KEYCODE_JP12_2 = -202;
    public static final int KEYCODE_JP12_3 = -203;
    public static final int KEYCODE_JP12_4 = -204;
    public static final int KEYCODE_JP12_5 = -205;
    public static final int KEYCODE_JP12_6 = -206;
    public static final int KEYCODE_JP12_7 = -207;
    public static final int KEYCODE_JP12_8 = -208;
    public static final int KEYCODE_JP12_9 = -209;
    public static final int KEYCODE_JP12_ASTER = -213;
    public static final int KEYCODE_JP12_CONV = -238;
    public static final int KEYCODE_JP12_REVERSE = -219;
    public static final int KEYCODE_JP12_SHARP = -211;
    public static final int KEYCODE_JP12_SYM = -222;
    public static final int KEYCODE_LANGUAGE_SWITCH = -412;
    public static final int KEYCODE_LEFT = -218;
    public static final int KEYCODE_QWERTY_SHIFT = -1;
    public static final int KEYCODE_QWERTY_SYM = -106;
    public static final int KEYCODE_RIGHT = -217;
    public static final int KEYCODE_SETTING_MENU = -239;
    public static final int KEYCODE_SHIFT = -1;
    public static final int KEYCODE_SPACE_BEFORE_V24 = 32;
    public static final int KEYCODE_SPACE_CONV = -122;
    public static final int KEYCODE_SPACE_JP = -121;
    public static final int KEYCODE_SPACE_LANGUAGE_PACK = 32;
    public static final int KEYCODE_SWITCH_VOICE = -311;
    public static final int KEYCODE_TOGGLE_COMMA = -116;
    public static final int KEYCODE_TOGGLE_EXCLAMATION = -117;
    public static final int KEYCODE_TOGGLE_INVERTED_EXCLAMATION = -118;
    public static final int KEYCODE_TOGGLE_MODE = -114;
    public static final int KEYCODE_TOGGLE_STOP = -150;
    public static final int KEYCODE_UNDO = -237;
    public static final int KEYCODE_UP = -235;
    public static final int KEYMODE_ALPHABET = 0;
    private static final int KEYMODE_BIT_HARDWARE = 9;
    protected static final int KEYMODE_BIT_NONE = 0;
    public static final int KEYMODE_JA_FULL_ALPHABET = 6;
    public static final int KEYMODE_JA_FULL_KATAKANA = 4;
    public static final int KEYMODE_JA_FULL_NUMBER = 7;
    public static final int KEYMODE_JA_HALF_KATAKANA = 5;
    public static final int KEYMODE_NUMBER = 1;
    public static final int KEYMODE_ORIGINAL = 3;
    public static final int KEYMODE_PHONE = 2;
    public static final int KEYMODE_VOICE = 8;
    public static final int LANDSCAPE = 1;
    public static final int LAYOUT_50KEY_HORIZONTAL = 2;
    public static final int LAYOUT_50KEY_VERTICAL_LEFT = 1;
    public static final int LAYOUT_50KEY_VERTICAL_RIGHT = 0;
    private static final int MSG_ANNOUNCE_STATE_KEYBOARD = 2;
    private static final int MSG_HOVERING_LONG_PRESS = 1;
    public static final int PORTRAIT = 0;
    protected static final int POS_50KEYTYPE = 15;
    protected static final int POS_INPUTMODE = 4;
    protected static final int POS_KEYBOARDTYPE = 3;
    protected static final int POS_MUSHROOM = 2;
    protected static final int POS_SETTINGS = 0;
    protected static final int POS_USER_DIC_DE = 8;
    protected static final int POS_USER_DIC_EN = 5;
    protected static final int POS_USER_DIC_JA = 6;
    protected static final int POS_USER_DIC_KO = 11;
    protected static final int POS_USER_DIC_RU = 7;
    protected static final int POS_USER_DIC_ZH_CN = 9;
    protected static final int POS_USER_DIC_ZH_TW = 10;
    private static final String TAG = "DefaultSoftKeyboard";
    protected static int mCurrentKeyMode;
    protected int mAllowedKeyMode;
    protected boolean mCapsLock;
    protected int mCurrent50KeyType;
    protected String[] mCurrentCycleTable;
    protected Keyboard mCurrentKeyboard;
    protected int mCurrentKeyboardType;
    protected int mFlickDirection;
    protected FlickKeyboardView mFlickKeyboardView;
    protected boolean mHasFlickStarted;
    protected WnnKeyboardFactory[][][][][] mKeyboard;
    protected KeyboardView mKeyboardView;
    protected int mLastImeOptions;
    protected int mLastInputType;
    protected BaseInputView mMainView;
    private Integer[] mMenuItem;
    protected MultiTouchKeyboardView mMultiTouchKeyboardView;
    protected int mShiftOn;
    protected ViewGroup mSubView;
    protected TextView mTextView;
    protected IWnnIME mWnn;
    protected static final int[] MODE_NUMBER_TABLE = {1};
    protected static final int[] MODE_PHONE_TABLE = {2};
    protected static final int[] MODE_DATETIME_TABLE = {0, 1};
    protected static final int[] MODE_NULL_TABLE = {0, 1};
    private static final SparseIntArray POS_AND_TITLE_LIST_USER_DICTIONARY = new SparseIntArray() {
        {
            put(5, R.string.ti_preference_dictionary_menu_en_txt);
            put(6, R.string.ti_preference_dictionary_menu_ja_txt);
            put(7, R.string.ti_preference_dictionary_menu_ru_txt);
            put(8, R.string.ti_preference_dictionary_menu_de_txt);
            put(9, R.string.ti_preference_dictionary_menu_zhcn_txt);
            put(10, R.string.ti_preference_dictionary_menu_zhtw_txt);
            put(11, R.string.ti_preference_dictionary_menu_ko_txt);
        }
    };
    private static final SparseIntArray POS_AND_TITLE_LIST_OTHER_MENU_ITMES = new SparseIntArray() {
        {
            put(0, R.string.ti_long_press_dialog_setting_menu_txt);
            put(2, R.string.ti_dialog_option_menu_item_mushroom_txt);
            put(3, R.string.ti_long_press_dialog_keyboard_type_txt);
            put(15, R.string.ti_long_press_dialog_50key_type_txt);
            put(4, R.string.ti_long_press_dialog_input_mode_txt);
        }
    };
    protected static boolean mEnableVoiceInput = false;
    protected static boolean mEnableSwitchIme = true;
    protected static boolean mEnableLeftRightKey = true;
    protected static final String[] CYCLE_TABLE_HALF_PERIOD = {iWnnEngine.DECO_OPERATION_SEPARATOR, "."};
    protected static final String[] CYCLE_TABLE_EXCLAMATION = {"?", "!"};
    protected static final String[] CYCLE_TABLE_INVERT_EXCLAMATION = {"¿", "¡"};
    private static final int[] DUMMY_INT_TABLE = {0};
    private static final int DELAY_TIME_HOVERING_LONG_PRESS = 3000 - KeyboardView.LONGPRESS_TIMEOUT;
    protected boolean mDisableKeyInput = true;
    protected int mDisplayMode = 0;
    protected boolean mHardKeyboardHidden = true;
    protected boolean mEnableHardware12Keyboard = false;
    protected boolean mIsSymbolKeyboard = false;
    protected boolean mNoInput = true;
    private boolean mEnableVibrate = false;
    private boolean mEnablePlaySound = false;
    protected boolean mEnableMushroom = false;
    protected boolean mEnableNumberKey = false;
    private boolean mPreEnableVoiceInput = false;
    private boolean mPreEnableSwitchIme = true;
    private boolean mPreEnableLeftRightKey = true;
    protected boolean mPreEnableNumberKey = false;
    private Drawable mEnterKeyIcon = null;
    private Drawable mEnterKeyIconPreview = null;
    private CharSequence mEnterKeyLabel = null;
    protected int mFlickPressKey = -99999;
    protected boolean mIsKeyProcessFinish = false;
    protected int mPrevInputKeyCode = 0;
    private int mPrevResIdEnterKey = 0;
    private int mPrevLabelResIdEnterKey = 0;
    protected boolean mIsImeOptionsForceAscii = false;
    protected boolean mEnableFlick = false;
    protected boolean mKeepShiftMode = false;
    protected int mLanguageType = -1;
    protected boolean mIsInputTypeNull = false;
    protected boolean mEnablePopup = true;
    protected boolean mEnableAutoCaps = true;
    protected int mPreferenceKeyMode = -1;
    protected boolean mAutoCaps = false;
    protected boolean mForceShift = true;
    private Keyboard.Key mLastHoverKey = null;
    private Keyboard.Key mLastHoveringLongPressKey = null;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (DefaultSoftKeyboard.this.mKeyboardView != null) {
                switch (msg.what) {
                    case 1:
                        if (DefaultSoftKeyboard.this.mLastHoverKey != null) {
                            int x = DefaultSoftKeyboard.this.mLastHoverKey.x + (DefaultSoftKeyboard.this.mLastHoverKey.width / 2);
                            int y = DefaultSoftKeyboard.this.mLastHoverKey.y + (DefaultSoftKeyboard.this.mLastHoverKey.height / 2);
                            long downTime = SystemClock.uptimeMillis();
                            MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, 0, x, y, 0);
                            DefaultSoftKeyboard.this.mKeyboardView.handleTouchEvent(downEvent);
                            downEvent.recycle();
                            DefaultSoftKeyboard.this.mLastHoveringLongPressKey = DefaultSoftKeyboard.this.mLastHoverKey;
                        }
                        break;
                    case 2:
                        if (DefaultSoftKeyboard.this.mWnn != null) {
                            ArrayList<Object> preKeyboard = DefaultSoftKeyboard.this.mWnn.getPreKeyboardParameters();
                            ArrayList<Object> currentKeyboard = DefaultSoftKeyboard.this.getKeyboardParameters();
                            ArrayList<Boolean> preShift = DefaultSoftKeyboard.this.mWnn.getPreShiftParameters();
                            ArrayList<Boolean> currentShift = DefaultSoftKeyboard.this.getShiftParameters();
                            if (preKeyboard == null || !preKeyboard.equals(currentKeyboard)) {
                                CharSequence description = WnnAccessibility.getDescriptionKeyboard(DefaultSoftKeyboard.this.mWnn, DefaultSoftKeyboard.mCurrentKeyMode, DefaultSoftKeyboard.this.mCurrentKeyboardType, DefaultSoftKeyboard.this.mCurrent50KeyType, DefaultSoftKeyboard.this.mKeyboardView.isShifted(), DefaultSoftKeyboard.this.mCapsLock);
                                WnnAccessibility.announceForAccessibility(DefaultSoftKeyboard.this.mWnn, description, DefaultSoftKeyboard.this.mKeyboardView);
                            } else if (preShift == null || !preShift.equals(currentShift)) {
                                CharSequence description2 = WnnAccessibility.getDescriptionSwitchShiftState(DefaultSoftKeyboard.this.mWnn, DefaultSoftKeyboard.mCurrentKeyMode, DefaultSoftKeyboard.this.mKeyboardView.isShifted(), DefaultSoftKeyboard.this.mCapsLock);
                                WnnAccessibility.announceForAccessibility(DefaultSoftKeyboard.this.mWnn, description2, DefaultSoftKeyboard.this.mKeyboardView);
                            }
                            DefaultSoftKeyboard.this.mWnn.setPreKeyboardParameters(currentKeyboard);
                            DefaultSoftKeyboard.this.mWnn.setPreShiftParameters(currentShift);
                        }
                        break;
                }
            }
        }
    };

    public DefaultSoftKeyboard(IWnnIME wnn) {
        this.mShiftOn = 0;
        this.mLastInputType = -1;
        this.mLastImeOptions = -1;
        this.mAllowedKeyMode = 0;
        this.mWnn = wnn;
        this.mAllowedKeyMode = getUnlimitedKeyMode();
        mCurrentKeyMode = getDefaultKeyMode();
        this.mCurrentKeyboardType = 0;
        this.mCurrent50KeyType = 0;
        this.mShiftOn = 0;
        if (this.mWnn != null) {
            if (this instanceof DefaultSoftKeyboardJAJP) {
                this.mCurrentKeyboardType = 1;
                String type = this.mWnn.getResources().getString(R.string.input_mode_type_default_value);
                if (type.equals(Integer.toString(0))) {
                    this.mCurrentKeyboardType = 0;
                }
            }
            EditorInfo editorInfo = this.mWnn.getCurrentInputEditorInfo();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
            setPreferences(pref, editorInfo);
            this.mLastInputType = -1;
            this.mLastImeOptions = -1;
        }
    }

    protected void createKeyboards() {
        boolean isOriginal = false;
        int sizeModeColumn = 3;
        int[] allMode = getAllModeTable();
        for (int mode : allMode) {
            if (mode == 3) {
                isOriginal = true;
                sizeModeColumn++;
            }
        }
        this.mKeyboard = (WnnKeyboardFactory[][][][][]) Array.newInstance((Class<?>) WnnKeyboardFactory.class, 2, 1, 2, sizeModeColumn, 1);
        if (this.mHardKeyboardHidden && this.mWnn != null) {
            boolean isOriginalShift = this.mLanguageType != 15;
            WnnKeyboardFactory[][] keyList = this.mKeyboard[0][0][0];
            keyList[0][0] = new WnnKeyboardFactory(this.mWnn, 0);
            keyList[1][0] = new WnnKeyboardFactory(this.mWnn, 4);
            keyList[2][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_phone);
            if (isOriginal) {
                keyList[3][0] = new WnnKeyboardFactory(this.mWnn, 8);
            }
            WnnKeyboardFactory[][] keyList2 = this.mKeyboard[0][0][1];
            keyList2[0][0] = new WnnKeyboardFactory(this.mWnn, 1);
            keyList2[1][0] = new WnnKeyboardFactory(this.mWnn, 5);
            keyList2[2][0] = this.mKeyboard[0][0][0][2][0];
            if (isOriginal && isOriginalShift) {
                keyList2[3][0] = new WnnKeyboardFactory(this.mWnn, 9);
            }
            WnnKeyboardFactory[][] keyList3 = this.mKeyboard[0][0][0];
            keyList3[0][0] = new WnnKeyboardFactory(this.mWnn, 2);
            keyList3[1][0] = new WnnKeyboardFactory(this.mWnn, 6);
            keyList3[2][0] = this.mKeyboard[0][0][0][2][0];
            if (isOriginal) {
                keyList3[3][0] = new WnnKeyboardFactory(this.mWnn, 10);
            }
            WnnKeyboardFactory[][] keyList4 = this.mKeyboard[0][0][1];
            keyList4[0][0] = new WnnKeyboardFactory(this.mWnn, 3);
            keyList4[1][0] = new WnnKeyboardFactory(this.mWnn, 7);
            keyList4[2][0] = this.mKeyboard[0][0][0][2][0];
            if (isOriginal && isOriginalShift) {
                keyList4[3][0] = new WnnKeyboardFactory(this.mWnn, 11);
            }
            WnnKeyboardFactory[][] keyList5 = this.mKeyboard[1][0][0];
            WnnKeyboardFactory[][] keyListPortrait = this.mKeyboard[0][0][0];
            keyList5[0][0] = keyListPortrait[0][0];
            keyList5[1][0] = keyListPortrait[1][0];
            keyList5[2][0] = keyListPortrait[2][0];
            if (isOriginal) {
                keyList5[3][0] = keyListPortrait[3][0];
            }
            WnnKeyboardFactory[][] keyList6 = this.mKeyboard[1][0][1];
            WnnKeyboardFactory[][] keyListPortrait2 = this.mKeyboard[0][0][1];
            keyList6[0][0] = keyListPortrait2[0][0];
            keyList6[1][0] = keyListPortrait2[1][0];
            keyList6[2][0] = keyListPortrait2[2][0];
            if (isOriginal && isOriginalShift) {
                keyList6[3][0] = keyListPortrait2[3][0];
            }
            WnnKeyboardFactory[][] keyList7 = this.mKeyboard[1][0][0];
            WnnKeyboardFactory[][] keyListPortrait3 = this.mKeyboard[0][0][0];
            keyList7[0][0] = keyListPortrait3[0][0];
            keyList7[1][0] = keyListPortrait3[1][0];
            keyList7[2][0] = keyListPortrait3[2][0];
            if (isOriginal) {
                keyList7[3][0] = keyListPortrait3[3][0];
            }
            WnnKeyboardFactory[][] keyList8 = this.mKeyboard[1][0][1];
            WnnKeyboardFactory[][] keyListPortrait4 = this.mKeyboard[0][0][1];
            keyList8[0][0] = keyListPortrait4[0][0];
            keyList8[1][0] = keyListPortrait4[1][0];
            keyList8[2][0] = keyListPortrait4[2][0];
            if (isOriginal && isOriginalShift) {
                keyList8[3][0] = keyListPortrait4[3][0];
            }
        }
    }

    private int add50KeyOffset(int keyboardType) {
        int offset = 0;
        if (keyboardType == 3) {
            offset = this.mCurrent50KeyType;
        }
        return keyboardType + offset;
    }

    protected Keyboard getShiftChangeKeyboard(int shift) {
        WnnKeyboardFactory kbd;
        try {
            WnnKeyboardFactory[] kbds = this.mKeyboard[this.mDisplayMode][add50KeyOffset(this.mCurrentKeyboardType)][shift][mCurrentKeyMode];
            if (!this.mNoInput && kbds.length > 1 && kbds[1] != null) {
                kbd = kbds[1];
            } else {
                kbd = kbds[0];
            }
            return kbd.getKeyboard(mCurrentKeyMode, this.mCurrentKeyboardType, getKeyboardCondition(false, shift > 0));
        } catch (Exception e) {
            return null;
        }
    }

    protected Keyboard getModeChangeKeyboard(int mode) {
        WnnKeyboardFactory kbd;
        try {
            WnnKeyboardFactory[] kbds = this.mKeyboard[this.mDisplayMode][add50KeyOffset(this.mCurrentKeyboardType)][this.mShiftOn][mode];
            if (!this.mNoInput && kbds.length > 1 && kbds[1] != null) {
                kbd = kbds[1];
            } else {
                kbd = kbds[0];
            }
            return kbd.getKeyboard(mode, this.mCurrentKeyboardType, getKeyboardCondition(false, false));
        } catch (Exception e) {
            return null;
        }
    }

    protected Keyboard getTypeChangeKeyboard(int type) {
        WnnKeyboardFactory kbd;
        try {
            WnnKeyboardFactory[] kbds = this.mKeyboard[this.mDisplayMode][add50KeyOffset(type)][this.mShiftOn][mCurrentKeyMode];
            if (!this.mNoInput && kbds.length > 1 && kbds[1] != null) {
                kbd = kbds[1];
            } else {
                kbd = kbds[0];
            }
            return kbd.getKeyboard(mCurrentKeyMode, type, getKeyboardCondition(false, false));
        } catch (Exception e) {
            return null;
        }
    }

    public Keyboard getKeyboardInputted(boolean inputted) {
        WnnKeyboardFactory kbd;
        try {
            WnnKeyboardFactory[] kbds = this.mKeyboard[this.mDisplayMode][add50KeyOffset(this.mCurrentKeyboardType)][this.mShiftOn][mCurrentKeyMode];
            if (inputted && kbds.length > 1 && kbds[1] != null) {
                kbd = kbds[1];
            } else {
                kbd = kbds[0];
            }
            return kbd.getKeyboard(mCurrentKeyMode, this.mCurrentKeyboardType, getKeyboardCondition(inputted, false));
        } catch (Exception e) {
            return null;
        }
    }

    protected void toggleShiftLock() {
        if (this.mShiftOn == 0) {
            Keyboard newKeyboard = getShiftChangeKeyboard(1);
            if (newKeyboard != null) {
                this.mShiftOn = 1;
                changeKeyboard(newKeyboard);
            }
            this.mCapsLock = true;
            return;
        }
        Keyboard newKeyboard2 = getShiftChangeKeyboard(0);
        if (newKeyboard2 != null) {
            this.mShiftOn = 0;
            changeKeyboard(newKeyboard2);
        }
        this.mCapsLock = false;
    }

    protected boolean changeKeyboard(Keyboard keyboard) {
        if (this.mIsSymbolKeyboard || keyboard == null || this.mKeyboardView == null) {
            return false;
        }
        if (this.mCurrentKeyboardType == 0 || this.mCurrentKeyboardType == 3) {
            this.mKeyboardView = this.mMultiTouchKeyboardView;
            this.mFlickKeyboardView.setVisibility(8);
            this.mMultiTouchKeyboardView.setVisibility(0);
        } else if (this.mCurrentKeyboardType == 1) {
            this.mKeyboardView = this.mFlickKeyboardView;
            this.mFlickKeyboardView.setVisibility(0);
            this.mMultiTouchKeyboardView.setVisibility(8);
        }
        if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
            restoreEnterKey(this.mKeyboardView.getKeyboard());
        } else {
            restoreEnterKey(this.mCurrentKeyboard);
        }
        saveEnterKey(keyboard);
        setEnterKey(keyboard);
        setSpaceKey(keyboard, !this.mNoInput);
        if (this.mCurrentKeyboard != keyboard) {
            clearKeyboardPress(keyboard);
            if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                ((MultiTouchKeyboardView) this.mKeyboardView).setKeyboard(getShiftChangeKeyboard(0), getShiftChangeKeyboard(1));
                ((MultiTouchKeyboardView) this.mKeyboardView).copyEnterKeyState();
            } else {
                this.mKeyboardView.setKeyboard(keyboard);
                this.mKeyboardView.setShifted(this.mShiftOn != 0);
            }
            this.mCurrentKeyboard = keyboard;
            announceStateOfKeyboard();
            if (IWnnIME.isDebugging() && this.mCurrentKeyboard != null) {
                Log.d(TAG, "SHIFT = " + this.mShiftOn + ", KBD CHANGE : " + this.mCurrentKeyboard.isShifted());
            }
            return true;
        }
        if (this.mKeyboardView instanceof FlickKeyboardView) {
            this.mKeyboardView.setShifted(this.mShiftOn != 0);
        }
        if (!IWnnIME.isDebugging() || this.mCurrentKeyboard == null) {
            return false;
        }
        Log.d(TAG, "SHIFT = " + this.mShiftOn + ", KBD NO CHANGE : " + this.mCurrentKeyboard.isShifted());
        return false;
    }

    public View initView(int width) {
        if (this.mWnn == null) {
            return null;
        }
        Resources res = this.mWnn.getResources();
        this.mDisplayMode = res.getConfiguration().orientation == 2 ? 1 : 0;
        mEnableSwitchIme = getEnableSwitchIme();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        keyskin.setPreferences(pref);
        LayoutInflater inflater = this.mWnn.getLayoutInflater();
        this.mMainView = (BaseInputView) inflater.inflate(R.layout.keyboard_default_main, (ViewGroup) null);
        createKeyboards();
        boolean isKeep = this.mWnn.isKeepInput();
        if (!isKeep) {
            this.mIsSymbolKeyboard = false;
        }
        boolean shift = this.mMultiTouchKeyboardView != null ? this.mMultiTouchKeyboardView.isShifted() : false;
        this.mMultiTouchKeyboardView = (MultiTouchKeyboardView) this.mMainView.findViewById(R.id.multi_keyboard);
        this.mMultiTouchKeyboardView.setOnKeyboardActionListener((OnFlickKeyboardActionListener) this);
        this.mMultiTouchKeyboardView.setShifted(shift);
        boolean shift2 = this.mFlickKeyboardView != null ? this.mFlickKeyboardView.isShifted() : false;
        this.mFlickKeyboardView = (FlickKeyboardView) this.mMainView.findViewById(R.id.flick_keyboard);
        this.mFlickKeyboardView.setShifted(shift2);
        this.mFlickKeyboardView.initPopupView(this.mWnn);
        this.mFlickKeyboardView.setOnTouchListener(this);
        this.mFlickKeyboardView.setModeCycleCount(getSlideCycleCount());
        this.mFlickKeyboardView.setOnKeyboardActionListener((OnFlickKeyboardActionListener) this);
        if (isKeep) {
            if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                this.mKeyboardView = this.mMultiTouchKeyboardView;
            } else {
                this.mKeyboardView = this.mFlickKeyboardView;
            }
        } else {
            String type = res.getString(R.string.input_mode_type_default_value);
            if (type.equals(Integer.toString(0))) {
                this.mKeyboardView = this.mMultiTouchKeyboardView;
            } else {
                this.mKeyboardView = this.mFlickKeyboardView;
            }
        }
        restoreEnterKey(this.mCurrentKeyboard);
        this.mCurrentKeyboard = null;
        this.mTextView = new TextView(this.mWnn);
        if (!this.mHardKeyboardHidden) {
            this.mMainView = (BaseInputView) inflater.inflate(R.layout.keyboard_default_main_sub, (ViewGroup) null);
            if (!this.mWnn.isSubtypeEmojiInput()) {
                this.mSubView = (ViewGroup) inflater.inflate(R.layout.keyboard_default_sub, (ViewGroup) null);
                if (!this.mEnableHardware12Keyboard) {
                    this.mMainView.addView(this.mSubView);
                }
                this.mSubView.addView(this.mTextView);
                this.mSubView.setPadding(res.getDimensionPixelSize(R.dimen.keyboard_subview_padding_left), res.getDimensionPixelSize(R.dimen.keyboard_subview_padding_top), res.getDimensionPixelSize(R.dimen.keyboard_subview_padding_right), res.getDimensionPixelSize(R.dimen.keyboard_subview_padding_bottom));
                this.mTextView.setTextColor(-1);
                this.mTextView.setBackgroundColor(IWnnImeEvent.PRIVATE_EVENT_OFFSET);
                this.mTextView.setGravity(17);
                this.mTextView.setWidth(res.getDimensionPixelSize(R.dimen.subview_label_width_size));
                this.mTextView.setTextSize(0, 1.0f);
                this.mTextView.setVisibility(4);
                this.mSubView.setVisibility(4);
                this.mMainView.setVisibility(4);
            }
        } else if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
            this.mFlickKeyboardView.setVisibility(8);
        } else {
            this.mMultiTouchKeyboardView.setVisibility(8);
        }
        return this.mMainView;
    }

    public View getCurrentView() {
        return this.mMainView;
    }

    public FlickKeyboardView getCurrentFlickKeyboardView() {
        return this.mFlickKeyboardView;
    }

    public MultiTouchKeyboardView getCurrentMultiTouchKeyboardView() {
        return this.mMultiTouchKeyboardView;
    }

    public void onUpdateState() {
        if (this.mWnn != null) {
            try {
                if (this.mWnn.getComposingText().size(1) == 0) {
                    if (!this.mNoInput) {
                        this.mNoInput = true;
                        Keyboard newKeyboard = getKeyboardInputted(false);
                        if (this.mCurrentKeyboard != newKeyboard) {
                            changeKeyboard(newKeyboard);
                        }
                        if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                            setSpaceKey(this.mKeyboardView.getKeyboard(), false);
                        } else {
                            setSpaceKey(this.mCurrentKeyboard, false);
                        }
                    }
                } else if (this.mNoInput) {
                    this.mNoInput = false;
                    Keyboard newKeyboard2 = getKeyboardInputted(true);
                    if (this.mCurrentKeyboard != newKeyboard2) {
                        changeKeyboard(newKeyboard2);
                    }
                    if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                        setSpaceKey(this.mKeyboardView.getKeyboard(), true);
                    } else {
                        setSpaceKey(this.mCurrentKeyboard, true);
                    }
                }
                if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                    setEnterKey(this.mKeyboardView.getKeyboard());
                    ((MultiTouchKeyboardView) this.mKeyboardView).copyEnterKeyState();
                } else {
                    setEnterKey(this.mCurrentKeyboard);
                }
            } catch (Exception ex) {
                Log.e(TAG, "DefaultSoftKeyboard::onUpdateState " + ex.toString());
            }
            if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                this.mCapsLock = ((MultiTouchKeyboardView) this.mKeyboardView).isCapsLock();
            }
            if (!this.mCapsLock && !this.mKeepShiftMode) {
                setShiftByEditorInfo(false);
            }
        }
    }

    public void setPreferences(SharedPreferences pref, EditorInfo editor) {
        if (editor != null && this.mWnn != null && pref != null) {
            Resources res = this.mWnn.getResources();
            this.mPreEnableSwitchIme = mEnableSwitchIme;
            this.mPreEnableLeftRightKey = mEnableLeftRightKey;
            this.mPreEnableVoiceInput = mEnableVoiceInput;
            this.mPreEnableNumberKey = this.mEnableNumberKey;
            this.mEnableVibrate = pref.getBoolean(ControlPanelPrefFragment.VIBRATION_KEY, res.getBoolean(R.bool.key_vibration_default_value));
            this.mEnablePlaySound = pref.getBoolean(ControlPanelPrefFragment.KEY_SOUND_KEY, res.getBoolean(R.bool.key_sound_default_value));
            this.mEnableMushroom = !pref.getString(ControlPanelPrefFragment.MUSHROOM_KEY, res.getString(R.string.mushroom_id_default)).equals("notuse");
            mEnableSwitchIme = getEnableSwitchIme();
            mEnableLeftRightKey = pref.getBoolean(ControlPanelPrefFragment.DISPLAY_LEFT_RIGHT_KEY, res.getBoolean(R.bool.opt_display_left_right_key_default_value));
            this.mEnableNumberKey = pref.getBoolean(ControlPanelPrefFragment.DISPLAY_NUMBER_KEY, res.getBoolean(R.bool.opt_display_number_key_default_value));
            if (!this.mWnn.isKeepInput()) {
                mEnableVoiceInput = false;
                boolean enableVoiceInputSetting = pref.getBoolean(ControlPanelPrefFragment.VOICE_SETTINGS_KEY, res.getBoolean(R.bool.voice_input_default_value));
                boolean availableShortcutIme = this.mWnn.isAvailableShortcutIME();
                boolean disableVoiceInputPrivateIme = this.mWnn.getDisableVoiceInputInPrivateImeOptions(editor);
                if (enableVoiceInputSetting && availableShortcutIme && !disableVoiceInputPrivateIme) {
                    mEnableVoiceInput = true;
                }
            }
            this.mEnablePopup = pref.getBoolean(ControlPanelPrefFragment.POPUP_PREVIEW_KEY, res.getBoolean(R.bool.popup_preview_default_value));
            if (this.mMultiTouchKeyboardView != null) {
                this.mMultiTouchKeyboardView.setPreviewEnabled(this.mEnablePopup);
                this.mMultiTouchKeyboardView.cancelTouchEvent();
                this.mMultiTouchKeyboardView.invalidateAllKeys();
                this.mMultiTouchKeyboardView.setEnableMushroom(this.mEnableMushroom);
                this.mMultiTouchKeyboardView.clearWindowInfo();
            }
            clearKeyboardPress(this.mCurrentKeyboard);
            if (this.mFlickKeyboardView != null) {
                this.mFlickKeyboardView.setPreviewEnabled(this.mEnablePopup);
                int thres = res.getInteger(R.integer.flick_sensitivity_preference_default);
                try {
                    thres = Integer.parseInt(pref.getString(ControlPanelPrefFragment.FLICK_SENSITIVITY_KEY, String.valueOf(thres)));
                } catch (NumberFormatException e) {
                }
                this.mFlickKeyboardView.setFlickSensitivity(thres);
                this.mFlickKeyboardView.setEnableMushroom(this.mEnableMushroom);
                this.mFlickKeyboardView.setModeCycleCount(getSlideCycleCount());
                this.mFlickKeyboardView.clearWindowInfo();
            }
            setPreferencesCharacteristic(pref, editor);
        }
    }

    public boolean isEnableReplace(String input) {
        return false;
    }

    public static boolean isKanaMode(int mode) {
        switch (mode) {
            case 3:
            case 4:
            case 5:
                return true;
            default:
                return false;
        }
    }

    protected void setPreferencesCharacteristic(SharedPreferences pref, EditorInfo editor) {
        if (editor != null && this.mWnn != null && !this.mWnn.isKeepInput()) {
            int inputType = editor.inputType;
            if (inputType == 0 && editor.packageName.equals("jp.co.omronsoft.iwnnime.ml")) {
                inputType = 1;
            }
            this.mAutoCaps = false;
            this.mForceShift = true;
            this.mPreferenceKeyMode = -1;
            this.mNoInput = true;
            this.mDisableKeyInput = false;
            this.mIsInputTypeNull = false;
            this.mAllowedKeyMode = getUnlimitedKeyMode();
            switch (inputType & 15) {
                case 1:
                    switch (inputType & 4080) {
                        case 16:
                        case 32:
                        case 208:
                            this.mPreferenceKeyMode = 0;
                            break;
                        case 128:
                        case 144:
                        case 224:
                            setAllowedKeyMode(getModePassWordTable());
                            if (isEnableKeyMode(0)) {
                                this.mPreferenceKeyMode = 0;
                            }
                            disableVoiceInput();
                            break;
                        case 192:
                            disableVoiceInput();
                            break;
                    }
                    if ((inputType & 28672) != 0) {
                        this.mAutoCaps = true;
                    }
                    break;
                case 2:
                    setAllowedKeyMode(MODE_NUMBER_TABLE);
                    disableVoiceInput();
                    break;
                case 3:
                    this.mForceShift = false;
                    if (this.mHardKeyboardHidden) {
                        setAllowedKeyMode(MODE_PHONE_TABLE);
                    } else {
                        setAllowedKeyMode(getModeDefaultTable());
                    }
                    break;
                case 4:
                    this.mPreferenceKeyMode = 1;
                    switch (inputType & 4080) {
                        case 16:
                            setAllowedKeyMode(MODE_NUMBER_TABLE);
                            break;
                        default:
                            setAllowedKeyMode(MODE_DATETIME_TABLE);
                            break;
                    }
                    disableVoiceInput();
                    break;
                default:
                    if (inputType == 0) {
                        this.mIsInputTypeNull = true;
                        setAllowedKeyMode(MODE_NULL_TABLE);
                        disableVoiceInput();
                        this.mAutoCaps = false;
                        this.mPreferenceKeyMode = 0;
                    }
                    break;
            }
            setHardwareKeyModeFilter();
            updateKeyboards();
            int imeOptions = editor.imeOptions;
            if ((Integer.MIN_VALUE & imeOptions) != 0) {
                this.mIsImeOptionsForceAscii = true;
                if (this.mAllowedKeyMode == getUnlimitedKeyMode() && this.mPreferenceKeyMode == -1) {
                    this.mPreferenceKeyMode = 0;
                }
            }
            forceCloseVoiceInputKeyboard();
        }
    }

    public void closing() {
        if (this.mFlickKeyboardView != null) {
            this.mFlickKeyboardView.closing();
        }
        if (this.mMultiTouchKeyboardView != null) {
            this.mMultiTouchKeyboardView.closing();
        }
        this.mDisableKeyInput = true;
    }

    public void showInputView() {
        if (this.mKeyboardView != null) {
            this.mKeyboardView.setVisibility(0);
        }
    }

    public void hideInputView() {
        if (this.mKeyboardView != null) {
            this.mKeyboardView.setVisibility(8);
        }
    }

    public void dismissPopupKeyboard() {
        if (this.mKeyboardView != null) {
            this.mKeyboardView.closing();
        }
    }

    public Boolean isPopupKeyboard() {
        if (this.mKeyboardView != null) {
            return Boolean.valueOf(this.mKeyboardView.isMiniKeyboardOnScreen());
        }
        return false;
    }

    public void closePopupKeyboard() {
        if (this.mKeyboardView != null) {
            this.mKeyboardView.dismissPopupKeyboard();
        }
    }

    public Boolean isPhoneMode() {
        return Boolean.valueOf(mCurrentKeyMode == 2);
    }

    public boolean isEnableKeyMode(int keyMode) {
        int bit = 1 << keyMode;
        return (this.mAllowedKeyMode & bit) != 0;
    }

    protected void enableKeyModeFlag(int keyMode) {
        int bit = 1 << keyMode;
        this.mAllowedKeyMode |= bit;
    }

    protected void disableKeyModeFlag(int keyMode) {
        int bit = 1 << keyMode;
        this.mAllowedKeyMode &= bit ^ (-1);
    }

    protected void setAllowedKeyMode(int[] keyModeArray) {
        this.mAllowedKeyMode = 0;
        for (int i : keyModeArray) {
            enableKeyModeFlag(i);
        }
    }

    protected int getUnlimitedKeyMode() {
        return 0;
    }

    @Override
    public boolean onKey(int primaryCode, int[] keyCodes) {
        MultiTouchKeyboardView keyboardView;
        this.mIsKeyProcessFinish = false;
        if (this.mWnn != null && !this.mDisableKeyInput) {
            if (!this.mWnn.isEnableAnnounce()) {
                this.mWnn.setPreKeyboardParameters(getKeyboardParameters());
                this.mWnn.setPreShiftParameters(getShiftParameters());
            }
            this.mWnn.setEnableAnnounce(true);
            EditorInfo info = this.mWnn.getCurrentInputEditorInfo();
            if (info != null && WnnUtility.PACKAGE_NAME_KEYGUARD_SERVICE.equals(info.packageName)) {
                switch (primaryCode) {
                    case KEYCODE_ENTER:
                        this.mWnn.setEnableAnnounce(false);
                        break;
                }
            }
            if (this.mFlickPressKey != -99999 && (this.mKeyboardView instanceof FlickKeyboardView)) {
                ((FlickKeyboardView) this.mKeyboardView).setFlickDetectMode(false, 0);
                boolean started = this.mHasFlickStarted;
                this.mHasFlickStarted = false;
                if (this.mFlickDirection != 0 || started) {
                    this.mIsKeyProcessFinish = true;
                    inputByFlickDirection(this.mFlickDirection, true);
                    if (!this.mCapsLock && primaryCode != -1) {
                        setShiftByEditorInfo(false);
                    }
                    this.mPrevInputKeyCode = primaryCode;
                    if (this.mIsKeyProcessFinish) {
                        return true;
                    }
                }
            }
            switch (primaryCode) {
                case KEYCODE_LANGUAGE_SWITCH:
                    this.mWnn.switchToNextInputMethod();
                    break;
                case KEYCODE_DOWN:
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INPUT_SOFT_KEY, new KeyEvent(0, 20)));
                    break;
                case KEYCODE_UP:
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INPUT_SOFT_KEY, new KeyEvent(0, 19)));
                    break;
                case KEYCODE_JP12_SYM:
                case KEYCODE_QWERTY_SYM:
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.START_SYMBOL_MODE));
                    break;
                case KEYCODE_LEFT:
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INPUT_SOFT_KEY, new KeyEvent(0, 21)));
                    break;
                case KEYCODE_RIGHT:
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INPUT_SOFT_KEY, new KeyEvent(0, 22)));
                    break;
                case KEYCODE_SPACE_JP:
                case 32:
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.SPACE_KEY));
                    this.mPrevInputKeyCode = primaryCode;
                    break;
                case KEYCODE_TOGGLE_MODE:
                    nextKeyMode();
                    break;
                case KEYCODE_ENTER:
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INPUT_SOFT_KEY, new KeyEvent(0, 66)));
                    break;
                case KEYCODE_BACKSPACE:
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INPUT_SOFT_KEY, new KeyEvent(0, 67)));
                    break;
                case -1:
                    if ((this.mKeyboardView instanceof MultiTouchKeyboardView) && (keyboardView = (MultiTouchKeyboardView) this.mKeyboardView) != null) {
                        if (isSoftLockEnabled()) {
                            if (keyboardView.isShifted()) {
                                if (keyboardView.isCapsLock()) {
                                    keyboardView.setShifted(false);
                                    keyboardView.setCapsLock(false);
                                    this.mCapsLock = false;
                                } else {
                                    keyboardView.setCapsLock(true);
                                    this.mCapsLock = true;
                                }
                            } else {
                                keyboardView.setShifted(true);
                            }
                        } else if (keyboardView.isShifted()) {
                            keyboardView.setShifted(false);
                            keyboardView.setCapsLock(false);
                            this.mCapsLock = false;
                        } else {
                            keyboardView.setShifted(true);
                            keyboardView.setCapsLock(true);
                            this.mCapsLock = true;
                        }
                        announceStateOfKeyboard();
                        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.TOGGLE_INPUT_CANCEL));
                    }
                    break;
                default:
                    if (primaryCode >= 0) {
                        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INPUT_CHAR, (char) primaryCode));
                    }
                    break;
            }
            return false;
        }
        return true;
    }

    protected void onKeyCharacteristic(int primaryCode, int[] keyCodes) {
    }

    protected void setShiftByEditorInfo(boolean force) {
        int shift;
        if (this.mWnn != null && !this.mWnn.isKeepInput()) {
            if (isAutoCapsOn()) {
                shift = getShiftKeyState(this.mWnn.getCurrentInputEditorInfo());
            } else {
                shift = 0;
            }
            if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                boolean isShift = shift == 1;
                if (force || isShift != ((MultiTouchKeyboardView) this.mKeyboardView).isShifted()) {
                    setShiftedByMultiTouchKeyboard(isShift);
                    return;
                }
                return;
            }
            if (force || shift != this.mShiftOn) {
                setShifted(shift);
            }
        }
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeUp() {
    }

    @Override
    public void onRelease(int x) {
    }

    @Override
    public void onPress(int x) {
        if (x != 0 && this.mWnn != null) {
            if (this.mEnableVibrate) {
                WnnUtility.vibrate(this.mWnn.getApplicationContext());
            }
            playKeyClick(x);
        }
        if (this.mKeyboardView instanceof FlickKeyboardView) {
            if (isEnableFlickMode(x)) {
                this.mFlickDirection = 0;
                if (isFlickKey(x)) {
                    this.mFlickPressKey = x;
                    ((FlickKeyboardView) this.mKeyboardView).setFlickDetectMode(true, x);
                    ((FlickKeyboardView) this.mKeyboardView).setFlickedKeyGuide(true);
                    return;
                } else {
                    this.mFlickPressKey = -99999;
                    ((FlickKeyboardView) this.mKeyboardView).setFlickDetectMode(false, 0);
                    return;
                }
            }
            this.mFlickPressKey = -99999;
            ((FlickKeyboardView) this.mKeyboardView).setFlickDetectMode(false, 0);
        }
    }

    protected void updateKeyboards() {
        if (this.mPreEnableSwitchIme != mEnableSwitchIme || this.mPreEnableLeftRightKey != mEnableLeftRightKey || this.mPreEnableVoiceInput != mEnableVoiceInput || this.mPreEnableNumberKey != this.mEnableNumberKey) {
            createKeyboards();
        }
    }

    private void playKeyClick(int primaryCode) {
        int sound;
        if (this.mEnablePlaySound && this.mWnn != null) {
            switch (primaryCode) {
                case KEYCODE_SPACE_JP:
                case 32:
                    sound = 6;
                    break;
                case KEYCODE_ENTER:
                    sound = 8;
                    break;
                case KEYCODE_BACKSPACE:
                    sound = 7;
                    break;
                default:
                    sound = 5;
                    break;
            }
            WnnUtility.playSoundEffect(this.mWnn.getApplicationContext(), sound);
        }
    }

    private void restoreEnterKey(Keyboard keyboard) {
        if (keyboard != null) {
            int enterIndex = keyboard.getKeyIndex(KEYCODE_ENTER);
            if (enterIndex >= 0) {
                Keyboard.Key enterKey = keyboard.getKey(enterIndex);
                if (this.mEnterKeyIcon != null || this.mEnterKeyLabel != null) {
                    enterKey.label = this.mEnterKeyLabel;
                    enterKey.icon = this.mEnterKeyIcon;
                    enterKey.iconPreview = this.mEnterKeyIconPreview;
                    if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                        ((MultiTouchKeyboardView) this.mKeyboardView).copyEnterKeyState();
                    }
                }
            } else {
                return;
            }
        }
        this.mPrevResIdEnterKey = 0;
        this.mPrevLabelResIdEnterKey = 0;
    }

    private void saveEnterKey(Keyboard keyboard) {
        int enterIndex;
        if (keyboard != null && (enterIndex = keyboard.getKeyIndex(KEYCODE_ENTER)) >= 0) {
            Keyboard.Key enterKey = keyboard.getKey(enterIndex);
            this.mEnterKeyLabel = enterKey.label;
            this.mEnterKeyIcon = enterKey.icon;
            this.mEnterKeyIconPreview = enterKey.iconPreview;
        }
    }

    private void setEnterKey(Keyboard newKeyboard) {
        int enterIndex;
        String description;
        if (this.mWnn != null && newKeyboard != null && this.mKeyboardView != null && (enterIndex = newKeyboard.getKeyIndex(KEYCODE_ENTER)) >= 0) {
            Keyboard.Key newEnterKey = newKeyboard.getKey(enterIndex);
            newEnterKey.popupCharacters = null;
            newEnterKey.popupResId = 0;
            EditorInfo edit = this.mWnn.getCurrentInputEditorInfo();
            int iconResId = 0;
            int iconPreviewResId = 0;
            int labelResId = 0;
            int imeAction = edit.imeOptions & 1073742079;
            if (this.mNoInput && this.mWnn.getFunfun() == 0) {
                switch (imeAction) {
                    case 2:
                        iconResId = R.drawable.key_enter_go;
                        iconPreviewResId = R.drawable.key_enter_go;
                        description = this.mWnn.getString(R.string.ti_description_key_enter_gone);
                        break;
                    case 3:
                        iconResId = R.drawable.key_enter_search;
                        iconPreviewResId = R.drawable.key_enter_search;
                        description = this.mWnn.getString(R.string.ti_description_key_enter_search);
                        break;
                    case 4:
                        iconResId = R.drawable.key_enter_send;
                        iconPreviewResId = R.drawable.key_enter_send;
                        description = this.mWnn.getString(R.string.ti_description_key_enter_send);
                        break;
                    case 5:
                        iconResId = R.drawable.key_enter_next;
                        iconPreviewResId = R.drawable.key_enter_next;
                        description = this.mWnn.getString(R.string.ti_description_key_enter_next);
                        break;
                    case 6:
                        iconResId = R.drawable.key_enter_done;
                        iconPreviewResId = R.drawable.key_enter_done;
                        description = this.mWnn.getString(R.string.ti_description_key_enter_done);
                        break;
                    case 7:
                        iconResId = R.drawable.key_enter_previous;
                        iconPreviewResId = R.drawable.key_enter_previous;
                        description = this.mWnn.getString(R.string.ti_description_key_enter_previous);
                        break;
                    default:
                        iconResId = R.drawable.key_enter;
                        iconPreviewResId = R.drawable.key_enter;
                        description = this.mWnn.getString(R.string.ti_description_key_enter_none);
                        break;
                }
            } else {
                imeAction = 0;
                labelResId = R.string.ti_enterkey_ok;
                description = this.mWnn.getString(R.string.ti_description_key_enter_inputting);
            }
            newEnterKey.description = description;
            if (this.mPrevResIdEnterKey != iconResId || this.mPrevLabelResIdEnterKey != labelResId) {
                this.mPrevResIdEnterKey = iconResId;
                this.mPrevLabelResIdEnterKey = labelResId;
                int keycode = newEnterKey.codes[0];
                if (labelResId == 0) {
                    newEnterKey.label = null;
                    newEnterKey.icon = this.mWnn.getResources().getDrawable(iconResId);
                    newEnterKey.iconPreview = this.mWnn.getResources().getDrawable(iconPreviewResId);
                    KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
                    boolean langValid = langPack.isValid();
                    if (langValid) {
                        Drawable icon = langPack.getDrawable(keycode, newKeyboard, imeAction);
                        if (icon != null) {
                            newEnterKey.icon = icon;
                        }
                        Drawable iconPreview = langPack.getDrawablePreview(keycode, newKeyboard, imeAction);
                        if (iconPreview != null) {
                            newEnterKey.iconPreview = iconPreview;
                        }
                    }
                } else {
                    newEnterKey.label = this.mWnn.getResources().getString(labelResId);
                    newEnterKey.icon = null;
                    newEnterKey.iconPreview = null;
                }
                newEnterKey.mIsIconSkin = false;
                KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
                if (keyskin.isValid()) {
                    Drawable icon2 = keyskin.getDrawable(keycode, newKeyboard, imeAction);
                    if (icon2 != null) {
                        newEnterKey.icon = icon2;
                        newEnterKey.mIsIconSkin = true;
                    }
                    Drawable iconPreview2 = keyskin.getDrawablePreview(keycode, newKeyboard, imeAction);
                    if (iconPreview2 != null) {
                        newEnterKey.iconPreview = iconPreview2;
                    }
                }
                if (newEnterKey.iconPreview != null) {
                    newEnterKey.iconPreview.setBounds(0, 0, newEnterKey.iconPreview.getIntrinsicWidth(), newEnterKey.iconPreview.getIntrinsicHeight());
                }
                Keyboard oldKeyboard = this.mCurrentKeyboard;
                if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                    oldKeyboard = this.mKeyboardView.getKeyboard();
                }
                if (oldKeyboard == newKeyboard && this.mKeyboardView.isShown()) {
                    this.mKeyboardView.invalidateKey(enterIndex);
                }
            }
        }
    }

    protected void setSpaceKey(Keyboard newKeyboard, boolean inputted) {
    }

    @Override
    public void onText(CharSequence text) {
    }

    public int getKeyMode() {
        return mCurrentKeyMode;
    }

    public int getKeyboardType() {
        return this.mCurrentKeyboardType;
    }

    public void setHardKeyboardHidden(boolean hidden) {
        if (this.mWnn != null) {
            if (!hidden) {
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_QWERTY));
            }
            if (this.mHardKeyboardHidden != hidden) {
                if (this.mAllowedKeyMode == getUnlimitedKeyMode() || mCurrentKeyMode != getDefaultKeyMode()) {
                    this.mLastInputType = 0;
                    this.mLastImeOptions = -1;
                    if (this.mWnn.isInputViewShown()) {
                        setDefaultKeyboard();
                    }
                }
                if (hidden) {
                    this.mLastInputType = -1;
                    this.mLastImeOptions = -1;
                }
            }
        }
        this.mHardKeyboardHidden = hidden;
    }

    public void setHardware12Keyboard(boolean type12Key) {
        this.mEnableHardware12Keyboard = type12Key;
    }

    public KeyboardView getKeyboardView() {
        return this.mKeyboardView;
    }

    public void resetCurrentKeyboard() {
        closing();
        this.mDisableKeyInput = false;
        Keyboard keyboard = this.mCurrentKeyboard;
        restoreEnterKey(this.mCurrentKeyboard);
        this.mCurrentKeyboard = null;
        changeKeyboard(keyboard);
    }

    @Override
    public void onFlick(int x, int direction) {
        if ((this.mKeyboardView instanceof FlickKeyboardView) && this.mFlickDirection != direction) {
            this.mHasFlickStarted = true;
            this.mFlickDirection = direction;
            if (isFlickKey(this.mFlickPressKey)) {
                inputByFlickDirection(direction, false);
            }
        }
    }

    protected void inputByFlickDirection(int direction, boolean isCommit) {
    }

    protected void inputByFlick(int directionIndex, boolean isCommit) {
    }

    @Override
    public boolean onLongPress(Keyboard.Key key) {
        if (isEnableLongPressMenu(key)) {
            showLongPressMenu();
            return true;
        }
        if (key.codes[0] == -1) {
            this.mKeyboardView.dismissKeyPreview();
        }
        if (this.mEnableMushroom && getLongpressMushroomKey(key)) {
            if (this.mWnn == null || this.mWnn.isScreenLock()) {
                return false;
            }
            BaseInputView baseInputView = (BaseInputView) getCurrentView();
            AlertDialog.Builder builder = new AlertDialog.Builder(baseInputView.getContext());
            baseInputView.showDialog(builder);
            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CALL_MUSHROOM));
            return true;
        }
        switch (key.codes[0]) {
            case KEYCODE_LANGUAGE_SWITCH:
                WnnUtility.showInputMethodPicker(this.mWnn);
                return true;
            default:
                return false;
        }
    }

    protected void setShifted(int shiftState) {
        Keyboard.Key shiftKey;
        if (this.mKeyboardView != null && !(this.mKeyboardView instanceof MultiTouchKeyboardView)) {
            Keyboard kbd = getShiftChangeKeyboard(shiftState);
            this.mShiftOn = shiftState;
            if (this.mShiftOn == 0) {
                this.mCapsLock = false;
            }
            changeKeyboard(kbd);
            if (kbd != null && (shiftKey = kbd.getShiftKey()) != null) {
                shiftKey.on = this.mCapsLock;
                int index = kbd.getKeys().indexOf(shiftKey);
                this.mKeyboardView.invalidateKey(index);
            }
        }
    }

    protected boolean getLongpressMushroomKey(Keyboard.Key key) {
        if (key != null) {
            switch (key.codes[0]) {
                case KEYCODE_JP12_SYM:
                case KEYCODE_QWERTY_SYM:
                    break;
            }
            return false;
        }
        return false;
    }

    protected int getSlideCycleCount() {
        return 1;
    }

    protected boolean isFlickKey(int key) {
        return false;
    }

    protected boolean isEnableFlickMode(int key) {
        return false;
    }

    public void setNormalKeyboard() {
        if (this.mMainView != null) {
            this.mMainView.setVisibility(0);
        }
        if (this.mCurrentKeyboard != null) {
            if (this.mKeyboardView != null) {
                this.mKeyboardView.mIgnoreTouchEvent = true;
                if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                    ((MultiTouchKeyboardView) this.mKeyboardView).setKeyboard(getShiftChangeKeyboard(0), getShiftChangeKeyboard(1));
                } else {
                    this.mKeyboardView.setKeyboard(this.mCurrentKeyboard);
                }
                this.mKeyboardView.setOnKeyboardActionListener(this);
                Keyboard kb = this.mKeyboardView.getKeyboard();
                setSpaceKey(kb, !this.mNoInput);
                announceStateOfKeyboard();
            }
            this.mIsSymbolKeyboard = false;
        }
        if (!this.mCapsLock) {
            setShiftByEditorInfo(true);
        }
    }

    public void announceStateOfKeyboard() {
        if ((this.mWnn == null || this.mWnn.isEnableAnnounce()) && !this.mHandler.hasMessages(2)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 30L);
        }
    }

    public void setSymbolKeyboard() {
        if (this.mMainView != null && this.mHardKeyboardHidden) {
            this.mMainView.setVisibility(8);
        }
        this.mIsSymbolKeyboard = true;
        if (this.mWnn != null) {
            this.mWnn.setPreKeyboardParameters(getKeyboardParameters());
        }
    }

    protected void clearKeyboardPress(Keyboard keyboard) {
        if (keyboard != null) {
            for (Keyboard.Key k : keyboard.getKeys()) {
                k.pressed = false;
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return this.mDisableKeyInput;
    }

    public void setOkToEnterKey() {
        int enterIndex;
        if (this.mWnn != null && this.mCurrentKeyboard != null && this.mKeyboardView != null && (enterIndex = this.mCurrentKeyboard.getKeyIndex(KEYCODE_ENTER)) >= 0) {
            Keyboard.Key newEnterKey = this.mCurrentKeyboard.getKey(enterIndex);
            newEnterKey.popupCharacters = null;
            newEnterKey.popupResId = 0;
            this.mPrevResIdEnterKey = 0;
            this.mPrevLabelResIdEnterKey = R.string.ti_enterkey_ok;
            newEnterKey.label = this.mWnn.getResources().getString(R.string.ti_enterkey_ok);
            newEnterKey.icon = null;
            newEnterKey.iconPreview = null;
            KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
            if (keyskin.isValid()) {
                Drawable tmpIcon = keyskin.getDrawable(newEnterKey.codes[0], this.mCurrentKeyboard, 0);
                if (tmpIcon != null) {
                    newEnterKey.icon = tmpIcon;
                }
                Drawable tmpPreview = keyskin.getDrawablePreview(newEnterKey.codes[0], this.mCurrentKeyboard, 0);
                if (tmpPreview != null) {
                    newEnterKey.iconPreview = tmpPreview;
                    newEnterKey.iconPreview.setBounds(0, 0, newEnterKey.iconPreview.getIntrinsicWidth(), newEnterKey.iconPreview.getIntrinsicHeight());
                }
            }
            this.mKeyboardView.invalidateKey(enterIndex);
        }
    }

    protected void initializeFlick() {
        this.mHasFlickStarted = false;
        this.mFlickDirection = 0;
        this.mFlickPressKey = -99999;
    }

    protected void disableVoiceInput() {
        mEnableVoiceInput = false;
        if (this.mFlickKeyboardView != null) {
            this.mFlickKeyboardView.setModeCycleCount(getSlideCycleCount());
        }
    }

    protected void forceCloseVoiceInputKeyboard() {
        if (!mEnableVoiceInput && mCurrentKeyMode == 8) {
            this.mLastInputType = -1;
        }
    }

    private boolean getEnableSwitchIme() {
        InputMethodManager manager;
        if (this.mWnn == null) {
            return false;
        }
        Resources res = this.mWnn.getResources();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
        boolean setting = pref.getBoolean(ControlPanelPrefFragment.DISPLAY_LANGUAGE_SWITCH_KEY, res.getBoolean(R.bool.opt_display_language_switch_key_default_value));
        if (!setting || Build.VERSION.SDK_INT < 16 || (manager = (InputMethodManager) this.mWnn.getSystemService("input_method")) == null) {
            return false;
        }
        List<InputMethodInfo> imiList = manager.getEnabledInputMethodList();
        boolean enable = pref.getBoolean(ControlPanelPrefFragment.CHANGE_OTHER_IME_KEY, res.getBoolean(R.bool.opt_change_otherime_default_value));
        if (imiList.size() != 1 && enable) {
            return true;
        }
        String packageName = this.mWnn.getApplicationContext().getPackageName();
        InputMethodInfo selfImi = null;
        Iterator<InputMethodInfo> it = manager.getInputMethodList().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            InputMethodInfo imi = it.next();
            if (imi.getPackageName().equals(packageName)) {
                selfImi = imi;
                break;
            }
        }
        if (selfImi != null && manager.getEnabledInputMethodSubtypeList(selfImi, true).size() > 1) {
            return true;
        }
        return false;
    }

    protected int getKeyboardCondition(boolean isInput, boolean isShift) {
        int ret;
        int ret2;
        int ret3;
        int ret4;
        int ret5;
        int ret6;
        int ret7 = mEnableVoiceInput ? 0 | Keyboard.CONDITION_VOICE_ON : 0 | Keyboard.CONDITION_VOICE_OFF;
        if (mEnableSwitchIme) {
            ret = ret7 | Keyboard.CONDITION_SWITCH_IME_ON;
        } else {
            ret = ret7 | Keyboard.CONDITION_SWITCH_IME_OFF;
        }
        if (mEnableLeftRightKey) {
            ret2 = ret | Keyboard.CONDITION_CURSOR_ON;
        } else {
            ret2 = ret | Keyboard.CONDITION_CURSOR_OFF;
        }
        if (this.mEnableNumberKey) {
            ret3 = ret2 | Keyboard.CONDITION_NUMBER_ON;
        } else {
            ret3 = ret2 | Keyboard.CONDITION_NUMBER_OFF;
        }
        if (isInput) {
            ret4 = ret3 | Keyboard.CONDITION_INPUT;
        } else {
            ret4 = ret3 | Keyboard.CONDITION_NOINPUT;
        }
        if (isShift) {
            ret5 = ret4 | Keyboard.CONDITION_SHIFT_ON;
        } else {
            ret5 = ret4 | Keyboard.CONDITION_SHIFT_OFF;
        }
        if (this.mCurrent50KeyType == 0) {
            ret6 = ret5 | Keyboard.CONDITION_50KEY_VERTICAL_RIGHT;
        } else if (this.mCurrent50KeyType == 1) {
            ret6 = ret5 | Keyboard.CONDITION_50KEY_VERTICAL_LEFT;
        } else {
            ret6 = ret5 | Keyboard.CONDITION_50KEY_HORIZONTAL;
        }
        return ret6 | getModeCondition();
    }

    protected int getModeCondition() {
        switch (mCurrentKeyMode) {
            case 0:
                int result = Keyboard.CONDITION_MODE_HALF_ALPHA;
                return result;
            case 1:
                int result2 = Keyboard.CONDITION_MODE_HALF_NUM;
                return result2;
            default:
                return 0;
        }
    }

    public void setKeepShiftMode(boolean set) {
        this.mKeepShiftMode = set;
    }

    public void setPrevInputKeyCode(int set) {
        this.mPrevInputKeyCode = set;
    }

    protected void setShiftedByMultiTouchKeyboard(boolean isShift) {
        if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
            if (!this.mIsSymbolKeyboard) {
                restoreEnterKey(this.mKeyboardView.getKeyboard());
            }
            this.mKeyboardView.setShifted(isShift);
            announceStateOfKeyboard();
            if (!this.mIsSymbolKeyboard) {
                Keyboard newKeyboard = this.mKeyboardView.getKeyboard();
                saveEnterKey(newKeyboard);
                setEnterKey(newKeyboard);
                ((MultiTouchKeyboardView) this.mKeyboardView).copyEnterKeyState();
            }
        }
    }

    public ArrayList<Boolean> getShiftParameters() {
        ArrayList<Boolean> params = new ArrayList<>();
        boolean shifted = false;
        if (this.mKeyboardView != null) {
            shifted = this.mKeyboardView.isShifted();
        }
        params.add(Boolean.valueOf(shifted));
        params.add(Boolean.valueOf(this.mCapsLock));
        return params;
    }

    public ArrayList<Object> getKeyboardParameters() {
        ArrayList<Object> params = new ArrayList<>();
        params.add(Integer.valueOf(mCurrentKeyMode));
        params.add(Integer.valueOf(this.mCurrentKeyboardType));
        params.add(Integer.valueOf(this.mCurrent50KeyType));
        params.add(Boolean.valueOf(this.mIsSymbolKeyboard));
        return params;
    }

    protected int getShiftKeyState(EditorInfo editor) {
        InputConnection connection;
        if (this.mWnn == null || (connection = this.mWnn.getCurrentInputConnection()) == null) {
            return 0;
        }
        int caps = connection.getCursorCapsMode(editor.inputType);
        return caps != 0 ? 1 : 0;
    }

    protected void startControlPanelStandard() {
        if (this.mWnn != null) {
            this.mWnn.startControlPanelStandard();
        }
    }

    public boolean isEnableFlick() {
        return this.mEnableFlick;
    }

    public synchronized void callUserDictionary(int languageType) {
        if (this.mWnn != null) {
            Intent intent = new Intent();
            intent.setClass(this.mWnn, UserDictionaryToolsActivity.class);
            intent.addFlags(402653184);
            intent.putExtra(UserDictionaryToolsActivity.EDIT_FROM_DIALOG_KEY, true);
            intent.putExtra(UserDictionaryToolsActivity.LANGUAGE_TYPE_KEY, languageType);
            intent.putExtra(UserDictionaryToolsActivity.DIALOG_THEME_KEY, R.style.UserDictionaryTools);
            intent.putExtra(UserDictionaryToolsActivity.DIALOG_ID_KEY, "android.intent.action.INSERT");
            intent.putExtra(UserDictionaryToolsActivity.DIALOG_STROKE_KEY, LoggingEvents.EXTRA_CALLING_APP_NAME);
            intent.putExtra(UserDictionaryToolsActivity.DIALOG_CANDIDATE_KEY, LoggingEvents.EXTRA_CALLING_APP_NAME);
            this.mWnn.startActivity(intent);
        }
    }

    protected void showLongPressMenu() {
        if (this.mWnn != null) {
            ArrayList<Integer> menuItemList = new ArrayList<>();
            boolean isUserDictionaryEditing = UserDictionaryToolsActivity.isEditing();
            boolean isUserDictionaryEditingFromDialog = UserDictionaryToolsActivity.isEditingFromDialog();
            boolean isScreenLock = this.mWnn.isScreenLock();
            if ((!isUserDictionaryEditing || isUserDictionaryEditingFromDialog) && !isScreenLock) {
                menuItemList.add(0);
            }
            if (this instanceof DefaultSoftKeyboardJAJP) {
                menuItemList.add(3);
                if (this.mCurrentKeyboardType == 3 && (mCurrentKeyMode == 3 || mCurrentKeyMode == 4 || mCurrentKeyMode == 5)) {
                    menuItemList.add(15);
                }
                if (isEnableSwitchInputMode()) {
                    menuItemList.add(4);
                }
            }
            if (this.mEnableMushroom && !isScreenLock) {
                menuItemList.add(2);
            }
            if (!isUserDictionaryEditing && !isScreenLock) {
                menuItemList.addAll(getLongPressMenuItemsUserDic());
            }
            this.mMenuItem = (Integer[]) menuItemList.toArray(new Integer[0]);
            Resources res = this.mWnn.getResources();
            CharSequence[] dialogItem = new CharSequence[this.mMenuItem.length];
            for (int i = 0; i < this.mMenuItem.length; i++) {
                int userDicTitleId = POS_AND_TITLE_LIST_USER_DICTIONARY.get(this.mMenuItem[i].intValue());
                if (userDicTitleId > 0) {
                    dialogItem[i] = getUserDicMenuTitle(userDicTitleId);
                } else {
                    dialogItem[i] = res.getString(POS_AND_TITLE_LIST_OTHER_MENU_ITMES.get(this.mMenuItem[i].intValue()));
                }
            }
            BaseInputView baseInputView = (BaseInputView) getCurrentView();
            AlertDialog.Builder builder = new AlertDialog.Builder(baseInputView.getContext());
            builder.setCancelable(true);
            builder.setNegativeButton(R.string.ti_dialog_button_cancel_txt, (DialogInterface.OnClickListener) null);
            builder.setItems(dialogItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface longPressDialog, int position) {
                    switch (DefaultSoftKeyboard.this.mMenuItem[position].intValue()) {
                        case 0:
                            DefaultSoftKeyboard.this.startControlPanelStandard();
                            break;
                        case 2:
                            DefaultSoftKeyboard.this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CALL_MUSHROOM));
                            break;
                        case 3:
                            DefaultSoftKeyboard.this.showKeyboardTypeSwitchDialog();
                            break;
                        case 4:
                            DefaultSoftKeyboard.this.showInputModeSwitchDialog();
                            break;
                        case 5:
                            DefaultSoftKeyboard.this.callUserDictionary(1);
                            break;
                        case 6:
                            DefaultSoftKeyboard.this.callUserDictionary(0);
                            break;
                        case 7:
                            DefaultSoftKeyboard.this.callUserDictionary(10);
                            break;
                        case 8:
                            DefaultSoftKeyboard.this.callUserDictionary(2);
                            break;
                        case 9:
                            DefaultSoftKeyboard.this.callUserDictionary(14);
                            break;
                        case 10:
                            DefaultSoftKeyboard.this.callUserDictionary(15);
                            break;
                        case 11:
                            DefaultSoftKeyboard.this.callUserDictionary(18);
                            break;
                        case 15:
                            DefaultSoftKeyboard.this.show50KeyTypeSwitchDialog();
                            break;
                    }
                }
            });
            builder.setTitle(res.getString(R.string.ti_long_press_dialog_title_txt));
            baseInputView.showDialog(builder);
            initializeFlick();
        }
    }

    protected ArrayList<Integer> getLongPressMenuItemsUserDic() {
        return new ArrayList<>();
    }

    private CharSequence getUserDicMenuTitle(int titleResId) {
        if (this.mWnn == null) {
            return null;
        }
        Resources res = this.mWnn.getResources();
        return res.getString(R.string.ti_user_dictionary_add_words_txt) + res.getString(R.string.ti_user_dictionary_word_separator_txt) + res.getString(titleResId);
    }

    protected boolean isEnableSwitchInputMode() {
        return false;
    }

    protected void showKeyboardTypeSwitchDialog() {
    }

    protected void show50KeyTypeSwitchDialog() {
    }

    public boolean showInputModeSwitchDialog() {
        return false;
    }

    protected void setKeyboardTypePref() {
    }

    protected void set50KeyTypePref() {
    }

    public String getInputModeKeyMainLabel() {
        if (this.mWnn == null) {
            return null;
        }
        Resources res = this.mWnn.getResources();
        return res.getString(R.string.ti_switch_input_mode_key_main_common_txt);
    }

    public int getInputModeKeyModeLabelList(String[] label, int[] offset, boolean[] currentMode, int labelSize) {
        Resources res = this.mWnn.getResources();
        if (label == null || label.length < 3 || offset == null || offset.length < 3 || currentMode == null || currentMode.length < 3) {
            return 0;
        }
        Arrays.fill(offset, 0);
        switch (mCurrentKeyMode) {
            case 0:
            case 1:
            case 3:
                int index = 0;
                if (isEnableKeyMode(3)) {
                    KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
                    if (langPack.isValid()) {
                        label[0] = langPack.getString(R.string.ti_switch_input_mode_key_mode_original_txt);
                    } else {
                        label[0] = res.getString(R.string.ti_switch_input_mode_key_mode_original_txt);
                    }
                    if (label[0] != null) {
                        offset[0] = (int) res.getFraction(R.fraction.mode_key_label_offset_original, labelSize, labelSize);
                        currentMode[0] = mCurrentKeyMode == 3;
                        index = 0 + 1;
                    }
                }
                if (isEnableKeyMode(0)) {
                    label[index] = res.getString(R.string.ti_switch_input_mode_key_mode_alphabet_txt);
                    offset[index] = (int) res.getFraction(R.fraction.mode_key_label_offset_alphabet, labelSize, labelSize);
                    currentMode[index] = mCurrentKeyMode == 0;
                    index++;
                }
                if (isEnableKeyMode(1)) {
                    label[index] = res.getString(R.string.ti_switch_input_mode_key_mode_number_txt);
                    currentMode[index] = mCurrentKeyMode == 1;
                    index++;
                }
                if (index <= 2) {
                    Arrays.fill(offset, 0);
                }
                break;
            case 4:
                label[0] = res.getString(R.string.ti_switch_input_mode_key_mode_full_katakana_txt);
                currentMode[0] = true;
                break;
            case 5:
                label[0] = res.getString(R.string.ti_switch_input_mode_key_mode_half_katakana_txt);
                currentMode[0] = true;
                break;
            case 6:
                label[0] = res.getString(R.string.ti_switch_input_mode_key_mode_alphabet_txt);
                currentMode[0] = true;
                break;
            case 7:
                label[0] = res.getString(R.string.ti_switch_input_mode_key_mode_number_txt);
                currentMode[0] = true;
                break;
        }
        return 0;
    }

    protected boolean isEnableLongPressMenu(Keyboard.Key key) {
        switch (key.codes[0]) {
            case KEYCODE_TOGGLE_MODE:
                return true;
            default:
                return false;
        }
    }

    protected void commitText() {
        if (!this.mNoInput && this.mWnn != null) {
            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.COMMIT_COMPOSING_TEXT));
        }
    }

    protected int getNextKeyMode() {
        boolean found = false;
        int length = 0;
        int[] cycleTable = getModeCycleTable();
        int[] tmp = new int[cycleTable.length];
        for (int i = 0; i < cycleTable.length; i++) {
            if (isEnableKeyMode(cycleTable[i])) {
                tmp[length] = cycleTable[i];
                length++;
            }
        }
        int[] table = new int[length];
        System.arraycopy(tmp, 0, table, 0, length);
        int index = 0;
        while (true) {
            if (index >= length) {
                break;
            }
            if (table[index] != mCurrentKeyMode) {
                index++;
            } else {
                found = true;
                break;
            }
        }
        int keyMode = -1;
        if (found) {
            for (int i2 = 0; i2 < length; i2++) {
                index = (index + 1) % length;
                keyMode = filterKeyMode(table[index]);
                if (keyMode != -1) {
                    return keyMode;
                }
            }
            return keyMode;
        }
        int keyMode2 = getDefaultKeyMode();
        return keyMode2;
    }

    protected void nextKeyMode() {
        int keyMode = getNextKeyMode();
        if (keyMode != -1) {
            changeKeyMode(keyMode);
        }
    }

    protected boolean isSoftLockEnabled() {
        return false;
    }

    public void changeKeyMode(int keyMode) {
        setStatusIcon();
        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.FIT_INPUT_TYPE));
    }

    protected int[] getModePhoneTable() {
        return DUMMY_INT_TABLE;
    }

    protected int[] getModePassWordTable() {
        return DUMMY_INT_TABLE;
    }

    protected int[] getModeDefaultTable() {
        return DUMMY_INT_TABLE;
    }

    protected int[] getModeCycleTable() {
        return DUMMY_INT_TABLE;
    }

    protected int[] getAllModeTable() {
        return DUMMY_INT_TABLE;
    }

    protected void setStatusIcon() {
    }

    protected void setHardwareKeyModeFilter() {
        if (!this.mHardKeyboardHidden) {
            this.mAllowedKeyMode &= 9;
            if (this.mAllowedKeyMode == 0) {
                enableKeyModeFlag(0);
            }
        }
    }

    protected void setDefaultKeyboard() {
        int keymode = getDefaultKeyMode();
        changeKeyMode(keymode);
    }

    public int getDefaultKeyMode() {
        if (this.mPreferenceKeyMode != -1 && isEnableKeyMode(this.mPreferenceKeyMode)) {
            return this.mPreferenceKeyMode;
        }
        int[] allModeTable = getAllModeTable();
        for (int i = 0; i < allModeTable.length; i++) {
            if (isEnableKeyMode(allModeTable[i])) {
                int allowedMode = allModeTable[i];
                return allowedMode;
            }
        }
        return -1;
    }

    protected int filterKeyMode(int keyMode) {
        if (isEnableKeyMode(keyMode)) {
            return keyMode;
        }
        if (isEnableKeyMode(mCurrentKeyMode)) {
            return -1;
        }
        int targetMode = getDefaultKeyMode();
        return targetMode;
    }

    protected boolean isAutoCapsOn() {
        return this.mEnableAutoCaps && mCurrentKeyMode == 0;
    }

    public void undoKeyMode() {
    }

    public void setUndoKey(boolean undo) {
    }

    public String[] getTableForFlickGuide() {
        return null;
    }

    @Override
    public boolean onHover(View v, MotionEvent event) {
        Keyboard kbd;
        if (this.mKeyboardView == null || this.mCurrentKeyboard == null || (kbd = this.mKeyboardView.getKeyboard()) == null || !WnnAccessibility.isAccessibility(this.mWnn)) {
            return false;
        }
        int posX = (int) event.getX();
        int posY = (int) event.getY();
        int leftPos = this.mKeyboardView.getLeft();
        int topPos = this.mKeyboardView.getTop();
        int rightPos = (this.mKeyboardView.getWidth() + leftPos) - 1;
        int bottomPos = (this.mKeyboardView.getHeight() + topPos) - 1;
        Rect rect = new Rect(leftPos + 1, topPos + 1, rightPos - 1, bottomPos - 1);
        Keyboard.Key key = null;
        if (rect.contains(posX, posY)) {
            key = this.mKeyboardView.getKey(posX, posY);
        }
        int action = event.getActionMasked();
        switch (action) {
            case 7:
                boolean ret = onHandleHoverMove(key, kbd);
                if (ret) {
                    return onHandleHoverEnter(key, kbd);
                }
                return ret;
            case 8:
            default:
                cancelHoveringLongPress();
                clearLastHoverKey(kbd);
                return false;
            case 9:
                return onHandleHoverEnter(key, kbd);
            case 10:
                return onHandleHoverExit(key, kbd);
        }
    }

    private boolean onHandleHoverEnter(Keyboard.Key key, Keyboard kbd) {
        if (key == null || kbd == null) {
            cancelHoveringLongPress();
        } else {
            int code = key.codes[0];
            int virtualId = kbd.getKeyIndex(code);
            CharSequence str = getDescriptionKey(key);
            WnnAccessibility.sendAccessibilityEventHoverEnter(this.mWnn, str, this.mKeyboardView, virtualId, key);
            cancelHoveringLongPress();
            this.mLastHoverKey = key;
        }
        return true;
    }

    private boolean onHandleHoverMove(Keyboard.Key key, Keyboard kbd) {
        if (key == null || kbd == null) {
            cancelHoveringLongPress();
            return true;
        }
        if (this.mLastHoverKey != null) {
            int code = key.codes[0];
            int lastCode = this.mLastHoverKey.codes[0];
            if (lastCode == code) {
                if (this.mHandler.hasMessages(1) || this.mLastHoveringLongPressKey != null || code != -114) {
                    return false;
                }
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), DELAY_TIME_HOVERING_LONG_PRESS);
                return false;
            }
            CharSequence str = getDescriptionKey(this.mLastHoverKey);
            int virtualId = kbd.getKeyIndex(lastCode);
            WnnAccessibility.sendAccessibilityEventHoverExit(this.mWnn, str, this.mKeyboardView, virtualId, this.mLastHoverKey);
        }
        cancelHoveringLongPress();
        return true;
    }

    private boolean onHandleHoverExit(Keyboard.Key key, Keyboard kbd) {
        cancelHoveringLongPress();
        clearLastHoverKey(kbd);
        if (key == null || kbd == null) {
            return true;
        }
        CharSequence str = getDescriptionKey(key);
        int virtualId = kbd.getKeyIndex(key.codes[0]);
        WnnAccessibility.sendAccessibilityEventHoverExit(this.mWnn, str, this.mKeyboardView, virtualId, key);
        if (this.mMainView != null && this.mMainView.isDialogShowing()) {
            return true;
        }
        int x = key.x + (key.width / 2);
        int y = key.y + (key.height / 2);
        long downTime = SystemClock.uptimeMillis();
        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, 0, x, y, 0);
        MotionEvent upEvent = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), 1, x, y, 0);
        this.mKeyboardView.handleTouchEvent(downEvent);
        this.mKeyboardView.handleTouchEvent(upEvent);
        downEvent.recycle();
        upEvent.recycle();
        return true;
    }

    private void cancelHoveringLongPress() {
        this.mHandler.removeMessages(1);
        if (this.mLastHoveringLongPressKey != null) {
            int x = this.mLastHoveringLongPressKey.x + (this.mLastHoveringLongPressKey.width / 2);
            int y = this.mLastHoveringLongPressKey.y + (this.mLastHoveringLongPressKey.height / 2);
            long downTime = SystemClock.uptimeMillis();
            MotionEvent cancelEvent = MotionEvent.obtain(downTime, downTime, 3, x, y, 0);
            this.mKeyboardView.handleTouchEvent(cancelEvent);
            if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                ((MultiTouchKeyboardView) this.mKeyboardView).cancelTouchEvent();
            }
            cancelEvent.recycle();
            this.mLastHoveringLongPressKey = null;
        }
    }

    private CharSequence getDescriptionKey(Keyboard.Key key) {
        if (this.mWnn == null) {
            return null;
        }
        int keycode = key.codes[0];
        int nextKeyMode = 0;
        switch (keycode) {
            case KEYCODE_TOGGLE_MODE:
                nextKeyMode = getNextKeyMode();
                break;
        }
        return WnnAccessibility.getDescriptionKey(this.mWnn, key, nextKeyMode);
    }

    private void clearLastHoverKey(Keyboard kbd) {
        if (this.mLastHoverKey != null && kbd != null) {
            CharSequence str = getDescriptionKey(this.mLastHoverKey);
            int virtualId = kbd.getKeyIndex(this.mLastHoverKey.codes[0]);
            WnnAccessibility.sendAccessibilityEventHoverExit(this.mWnn, str, this.mKeyboardView, virtualId, this.mLastHoverKey);
        }
        this.mLastHoverKey = null;
    }

    public void cancelEvent() {
        cancelHoveringLongPress();
        this.mHandler.removeMessages(2);
    }
}
