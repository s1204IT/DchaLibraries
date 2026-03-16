package jp.co.omronsoft.iwnnime.ml;

import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.inputmethodservice.ExtractEditText;
import android.inputmethodservice.InputMethodService;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;
import com.android.common.speech.LoggingEvents;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.IDecoEmojiManager;
import jp.co.omronsoft.android.emoji.EmojiAssist;
import jp.co.omronsoft.iwnnime.ml.candidate.CandidatesManager;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelPrefFragment;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelStandard;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiUtil;
import jp.co.omronsoft.iwnnime.ml.hangul.IWnnImeHangul;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnCore;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnSymbolEngine;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;
import jp.co.omronsoft.iwnnime.ml.jajp.IWnnImeJaJp;
import jp.co.omronsoft.iwnnime.ml.latin.IWnnImeLatin;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;
import jp.co.omronsoft.iwnnime.ml.zh.IWnnImeZh;

public class IWnnIME extends InputMethodService {
    private static final int BLUETOOTH_KEYBOARD_SPECIAL_VALUE = 8451;
    public static final String CHARSET_NAME_UTF8 = "UTF-8";
    private static final String CLASS_NAME_EXTRACTEDITLAYOUT = "android.inputmethodservice.ExtractEditLayout";
    private static final String DECOEMOJI_GIJI_DICTIONALY_NAME = "njdecoemoji.a";
    private static final String DECOEMOJI_GIJI_DICTIONALY_PATH = "/dicset/master/";
    private static final int DELAY_TIME_ANNOUNCE_PASSWORD_FIELD_CAUTION = 30;
    public static final String FILENAME_NOT_RESET_SETTINGS_PREFERENCE = "not_reset_settings_pref";
    private static final int MSG_ANNOUNCE_PASSWORD_FIELD_CAUTION = 1;
    private static final int PARAMETER_NOT_RESIZE = -1;
    private static final String PRIVATE_IME_OPTION_DISABLE_VOICE_INPUT = "com.google.android.inputmethod.latin.noMicrophoneKey";
    private static final String PRIVATE_IME_OPTION_DISABLE_VOICE_INPUT_COMPAT = "nm";
    private static final String SUBTYPE_EXTRAVALUE_REQUIRE_NETWORK_CONNECTIVITY = "requireNetworkConnectivity";
    public static final String SUBTYPE_LOCALE_EMOJI_INPUT = "emoji";
    private static final String TAG = "iWnn";
    private static final String TOAST_MESSAGE_KEY_SELECT_LANGUAGE = "ti_select_language_txt";
    private static IWnnIME mWnn;
    private ComposingExtraView mComposingExtraView;
    private boolean mConsumeDownEvent;
    private LinearLayout mExtraViewFloatingContainer;
    private int mFunfun;
    private boolean mIsEmojiAssistWorking;
    private boolean mIsKeep;
    private PopupWindow mPopupWindow;
    private ApplicationInfo mTargetApplicationInfo;
    private static boolean DEBUG = false;
    private static boolean PERFORMANCE_DEBUG = false;
    private static boolean mTabletMode = false;
    private static int mEmojiType = 0;
    private IWnnImeBase mIwnnIme = null;
    private CandidatesManager mCandidatesManager = null;
    private DefaultSoftKeyboard mDefaultSoftKeyboard = null;
    private WnnEngine mWnnEngine = null;
    private ComposingText mComposingText = null;
    private boolean mAutoHideMode = true;
    private boolean mDirectInputMode = true;
    private EditorInfo mAttribute = null;
    private List<KeyAction> KeyActionList = new ArrayList();
    private Locale mSelectedLocale = null;
    private Toast mSelectLangToast = null;
    private boolean mSubtypeEmojiInput = false;
    private boolean mCreatedCandidatesView = false;
    private InputMethodInfo mShortcutInputMethodInfo = null;
    private InputMethodSubtype mShortcutSubtype = null;
    private int mEditorSelectionStart = 0;
    private int mEditorSelectionEnd = 0;
    private KeyboardManager mKeyboardManager = null;
    private IDecoEmojiManager mDecoEmojiInterface = null;
    private boolean mIsBind = false;
    private boolean mIsEmoji = false;
    private EmojiAssist mEmojiAssist = null;
    public boolean mIsNotifyCursorAnchorInfo = false;
    private PopupTimer mPopupWindowTimer = new PopupTimer();
    private FrameLayout mInputViewBaseLayout = null;
    private AbsoluteLayout mFullScreenViewBaseLayout = null;
    private View mExtractEditLayout = null;
    private boolean mEnableAnnounce = false;
    private ArrayList<Object> mPreParametersKeyboard = null;
    private ArrayList<Boolean> mPreParametersShift = null;
    private int mEditorTargetSdkVersion = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (IWnnIME.this.mDefaultSoftKeyboard != null && WnnAccessibility.shouldObscureInput()) {
                        WnnAccessibility.announceForAccessibility(IWnnIME.this.getApplicationContext(), IWnnIME.this.getString(R.string.ti_description_password_field_caution), IWnnIME.this.mDefaultSoftKeyboard.getKeyboardView());
                        break;
                    }
                    break;
            }
        }
    };
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            if (IWnnIME.DEBUG) {
                Log.d(IWnnIME.TAG, "onServiceConnected() START");
            }
            IWnnIME.this.mDecoEmojiInterface = IDecoEmojiManager.Stub.asInterface(binder);
            if (IWnnIME.this.getPreferenceId() == -1) {
                IWnnIME.this.callCheckDecoEmoji();
            }
            if (IWnnIME.DEBUG) {
                Log.d(IWnnIME.TAG, "onServiceConnected() END");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (IWnnIME.DEBUG) {
                Log.d(IWnnIME.TAG, "onServiceDisconnected() START");
            }
            IWnnIME.this.mDecoEmojiInterface = null;
            if (IWnnIME.DEBUG) {
                Log.d(IWnnIME.TAG, "onServiceDisconnected() END");
            }
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (IWnnIME.this.mIsEmojiAssistWorking) {
                if (intent.getAction().equals("android.intent.action.SCREEN_ON")) {
                    IWnnIME.this.mEmojiAssist.startAnimation();
                } else if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                    IWnnIME.this.mEmojiAssist.stopAnimation();
                }
            }
            if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                if (IWnnIME.this.mDefaultSoftKeyboard != null) {
                    IWnnIME.this.mDefaultSoftKeyboard.dismissPopupKeyboard();
                    BaseInputView baseInputView = (BaseInputView) IWnnIME.this.mDefaultSoftKeyboard.getCurrentView();
                    if (baseInputView != null) {
                        baseInputView.closeDialog();
                    }
                }
                IWnnIME.this.onFinishInput();
                return;
            }
            if (intent.getAction().equals("android.intent.action.SCREEN_ON") && !IWnnIME.this.mEnableAnnounce && IWnnIME.this.isInputViewShown() && !IWnnIME.this.isScreenLock() && IWnnIME.this.mDefaultSoftKeyboard != null) {
                IWnnIME.this.mEnableAnnounce = true;
                IWnnIME.this.mDefaultSoftKeyboard.announceStateOfKeyboard();
            }
        }
    };
    private AccessibilityManager.AccessibilityStateChangeListener mAccessibilityStateChangeListener = new AccessibilityManager.AccessibilityStateChangeListener() {
        @Override
        public void onAccessibilityStateChanged(boolean enabled) {
            IWnnIME.this.onAccessibilityStateChanged();
        }
    };
    private AccessibilityManager.TouchExplorationStateChangeListener mTouchExplorationStateChangeListener = new AccessibilityManager.TouchExplorationStateChangeListener() {
        @Override
        public void onTouchExplorationStateChanged(boolean enabled) {
            IWnnIME.this.onAccessibilityStateChanged();
        }
    };

    private class PopupTimer extends Handler implements Runnable {
        private PopupTimer() {
        }

        void postShowFloatingWindow() {
            IWnnIME.this.mExtraViewFloatingContainer.measure(-2, -2);
            IWnnIME.this.mPopupWindow.setWidth(IWnnIME.this.mExtraViewFloatingContainer.getMeasuredWidth());
            IWnnIME.this.mPopupWindow.setHeight(IWnnIME.this.mExtraViewFloatingContainer.getMeasuredHeight());
            post(this);
        }

        void cancelShowing() {
            if (IWnnIME.this.mPopupWindow.isShowing()) {
                IWnnIME.this.mPopupWindow.dismiss();
            }
            removeCallbacks(this);
        }

        @Override
        public void run() {
            Point pos = IWnnIME.this.mKeyboardManager.getKeyboardPosition();
            int imtTop = IWnnIME.this.mKeyboardManager.getImeTopPosition(pos.y, true, true);
            int posX = pos.x;
            int posY = imtTop - IWnnIME.this.mPopupWindow.getHeight();
            View view = IWnnIME.this.getWindow().getWindow().getDecorView();
            if (IWnnIME.this.mPopupWindow.isShowing()) {
                IWnnIME.this.mPopupWindow.update(posX, posY, IWnnIME.this.mPopupWindow.getWidth(), -1);
            } else if (view != null) {
                IWnnIME.this.mPopupWindow.showAtLocation(view, 51, posX, posY);
                WnnUtility.addFlagsForPopupWindow(IWnnIME.this, IWnnIME.this.mPopupWindow);
            }
        }
    }

    @Override
    public void onCreate() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref != null) {
            DEBUG = pref.getBoolean("debug_log", false);
            if (DEBUG) {
                Log.d(TAG, "onCreate()");
            }
            PERFORMANCE_DEBUG = pref.getBoolean("performance_debug_log", false);
            if (PERFORMANCE_DEBUG) {
                Log.d(TAG, "IWnnIME::onCreate()  Start");
            }
        }
        updateTabletMode(this);
        KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
        langPack.setInputMethodSubtypeInstallLangPack(this);
        String currentLocale = getSubtypeLocale(WnnUtility.getCurrentInputMethodSubtype(this));
        int languageType = LanguageManager.getChosenLanguageType(currentLocale);
        setLanguage(LanguageManager.getChosenLocale(languageType), languageType, false);
        super.onCreate();
        mWnn = this;
        copyPresetDecoEmojiGijiDictionary();
        if (this.mWnnEngine != null) {
            this.mWnnEngine.init(getFilesDirPath());
        }
        if (this.mComposingText != null) {
            this.mComposingText.clear();
        }
        this.mKeyboardManager = new KeyboardManager(this);
        registReceiver();
        if (this.mIwnnIme != null) {
            this.mIwnnIme.onCreate();
        }
        KeyboardSkinData.getInstance().init(this);
        WnnAccessibility.addAccessibilityStateChangeListener(this, this.mAccessibilityStateChangeListener);
        WnnAccessibility.addTouchExplorationStateChangeListener(this, this.mTouchExplorationStateChangeListener);
    }

    @Override
    public View onCreateCandidatesView() {
        this.mCreatedCandidatesView = false;
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onCreateCandidatesView()  Unprocessing onCreate() ");
            return super.onCreateCandidatesView();
        }
        if (getCurrentInputConnection() == null) {
            Log.e(TAG, "IWnnIME::onCreateCandidatesView()  InputConnection is not active");
            return super.onCreateCandidatesView();
        }
        if (this.mCandidatesManager != null) {
            this.mCandidatesManager.initView(this.mKeyboardManager.getKeyboardSize(true).x);
            LayoutInflater inflater = getLayoutInflater();
            View view = (FrameLayout) inflater.inflate(R.layout.candidate_view_base_layout, (ViewGroup) null);
            this.mExtraViewFloatingContainer = (LinearLayout) inflater.inflate(R.layout.extra_view_floating_container, (ViewGroup) null);
            this.mComposingExtraView = (ComposingExtraView) this.mExtraViewFloatingContainer.getChildAt(0);
            closePopupWindow();
            this.mPopupWindow = new PopupWindow(this);
            this.mPopupWindow.setClippingEnabled(false);
            this.mPopupWindow.setBackgroundDrawable(null);
            this.mPopupWindow.setInputMethodMode(2);
            this.mPopupWindow.setContentView(this.mExtraViewFloatingContainer);
            setFunfun(0);
            if (DEBUG) {
                Log.d(TAG, "onCreateCandidatesView()=" + view);
            }
            this.mCreatedCandidatesView = true;
            return view;
        }
        if (DEBUG) {
            Log.d(TAG, "onCreateCandidatesView()=" + ((Object) null));
        }
        return super.onCreateCandidatesView();
    }

    @Override
    public View onCreateInputView() {
        this.mInputViewBaseLayout = null;
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onCreateInputView()  Unprocessing onCreate() ");
            return super.onCreateInputView();
        }
        if (getCurrentInputConnection() == null) {
            Log.e(TAG, "IWnnIME::onCreateInputView()  InputConnection is not active");
            return super.onCreateInputView();
        }
        if (this.mIwnnIme != null) {
            this.mIwnnIme.onCreateInputView();
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref != null) {
            DEBUG = pref.getBoolean("debug_log", false);
            if (DEBUG) {
                Log.d(TAG, "onCreateInputView()");
            }
            PERFORMANCE_DEBUG = pref.getBoolean("performance_debug_log", false);
            if (PERFORMANCE_DEBUG) {
                Log.d(TAG, "IWnnIME::onCreateInputView()  Start");
            }
        }
        if (this.mDefaultSoftKeyboard != null) {
            WindowManager wm = (WindowManager) getSystemService("window");
            if (wm == null) {
                return super.onCreateInputView();
            }
            this.mDefaultSoftKeyboard.initView(wm.getDefaultDisplay().getWidth());
            LayoutInflater inflater = getLayoutInflater();
            this.mInputViewBaseLayout = (FrameLayout) inflater.inflate(R.layout.input_view_base_layout, (ViewGroup) null);
            int displayHeight = this.mKeyboardManager.getDisplaySize().y;
            int posY = this.mKeyboardManager.getKeyboardPosition().y;
            View view = this.mInputViewBaseLayout.findViewById(R.id.input_view_base_layout_top);
            LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) view.getLayoutParams();
            param.height = posY;
            view.setLayoutParams(param);
            View view2 = this.mInputViewBaseLayout.findViewById(R.id.input_view_base_layout_bottom);
            LinearLayout.LayoutParams param2 = (LinearLayout.LayoutParams) view2.getLayoutParams();
            param2.height = displayHeight - posY;
            view2.setLayoutParams(param2);
            int color = R.color.input_view_base_layout_opaque_color;
            if (this.mKeyboardManager.getFloatingFromPref() || (this.mIwnnIme != null && !this.mIwnnIme.isHardKeyboardHidden())) {
                color = R.color.input_view_base_layout_transparent_color;
            }
            view2.setBackgroundResource(color);
            view2.setOnHoverListener(WnnAccessibility.ACCESSIBILITY_HOVER_LISTENER_EMPTY);
            return this.mInputViewBaseLayout;
        }
        return super.onCreateInputView();
    }

    @Override
    public void onDestroy() {
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onDestroy()  Unprocessing onCreate() ");
            super.onDestroy();
            return;
        }
        super.onDestroy();
        if (DEBUG) {
            Log.d(TAG, "onDestroy()");
        }
        if (this.mCandidatesManager != null) {
            this.mCandidatesManager.cancelCreateCandidates();
        }
        if (this.mDefaultSoftKeyboard != null) {
            this.mDefaultSoftKeyboard.cancelEvent();
        }
        this.mKeyboardManager.removeMessages();
        this.mHandler.removeMessages(1);
        mWnn = null;
        if (this.mIwnnIme != null) {
            this.mIwnnIme.close(false);
        }
        if (this.mIsBind) {
            unbindService(this.mServiceConn);
            this.mIsBind = false;
        }
        unregistReceiver();
        WnnAccessibility.removeAccessibilityStateChangeListener(this, this.mAccessibilityStateChangeListener);
        WnnAccessibility.removeTouchExplorationStateChangeListener(this, this.mTouchExplorationStateChangeListener);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        InputDevice dev;
        if (DEBUG) {
            Log.d(TAG, "onKeyDown(" + keyCode + ")");
        }
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onKeyDown()  Unprocessing onCreate() ");
            return super.onKeyDown(keyCode, event);
        }
        if (getCurrentInputConnection() == null) {
            Log.e(TAG, "IWnnIME::onKeyDown()  InputConnection is not active");
            return super.onKeyDown(keyCode, event);
        }
        if (event.getFlags() == 8 && (dev = event.getDevice()) != null && ((dev.isVirtual() || dev.getSources() != 257) && dev.getSources() != BLUETOOTH_KEYBOARD_SPECIAL_VALUE)) {
            Log.e(TAG, "IWnnIME::onKeyDown()  Through system event");
            return super.onKeyDown(keyCode, event);
        }
        if (!this.mDirectInputMode) {
            switch (keyCode) {
                case 82:
                    if (event.isShiftPressed()) {
                        if (isScreenLock()) {
                            return true;
                        }
                        if (this.mIwnnIme != null) {
                            int tempConvertingForFuncKeyType = this.mIwnnIme.getConvertingForFuncKeyType();
                            boolean convertingForFuncKey = tempConvertingForFuncKeyType != 1;
                            this.mIwnnIme.setConvertingForFuncKeyType(1);
                            if (convertingForFuncKey && this.mComposingText != null) {
                                int len = this.mComposingText.size(1);
                                if (len > 0) {
                                    this.mIwnnIme.setConvertingForFuncKeyType(tempConvertingForFuncKeyType);
                                    return true;
                                }
                            }
                        }
                        startControlPanelStandard();
                        return true;
                    }
                    break;
            }
        }
        cancelToast();
        if (keyCode == 4) {
            this.mConsumeDownEvent = super.onKeyDown(keyCode, event);
            return this.mConsumeDownEvent;
        }
        if (this.mAttribute == null || keyCode == 111 || keyCode == 211) {
            this.mConsumeDownEvent = onEvent(new IWnnImeEvent(event));
        } else {
            switch (this.mAttribute.inputType & 15) {
                case 4:
                    switch (this.mAttribute.inputType & 4080) {
                        case 16:
                        case 32:
                            this.mConsumeDownEvent = false;
                            return super.onKeyDown(keyCode, event);
                        default:
                            this.mConsumeDownEvent = onEvent(new IWnnImeEvent(event));
                            break;
                    }
                    break;
                default:
                    this.mConsumeDownEvent = onEvent(new IWnnImeEvent(event));
                    break;
            }
        }
        KeyAction Keycodeinfo = new KeyAction();
        Keycodeinfo.mConsumeDownEvent = this.mConsumeDownEvent;
        Keycodeinfo.mKeyCode = keyCode;
        int cnt = this.KeyActionList.size();
        if (cnt != 0) {
            int i = 0;
            while (true) {
                if (i < cnt) {
                    if (this.KeyActionList.get(i).mKeyCode != keyCode) {
                        i++;
                    } else {
                        this.KeyActionList.remove(i);
                    }
                }
            }
        }
        this.KeyActionList.add(Keycodeinfo);
        if (!this.mConsumeDownEvent) {
            return super.onKeyDown(keyCode, event);
        }
        return this.mConsumeDownEvent;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean ret;
        InputDevice dev;
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onKeyUp()  Unprocessing onCreate() ");
            return super.onKeyUp(keyCode, event);
        }
        if (getCurrentInputConnection() == null) {
            Log.e(TAG, "IWnnIME::onKeyUp()  InputConnection is not active");
            return super.onKeyUp(keyCode, event);
        }
        if (event.getFlags() == 8 && (dev = event.getDevice()) != null && ((dev.isVirtual() || dev.getSources() != 257) && dev.getSources() != BLUETOOTH_KEYBOARD_SPECIAL_VALUE)) {
            Log.e(TAG, "IWnnIME::onKeyUp()  Through system event");
            return super.onKeyUp(keyCode, event);
        }
        boolean ret2 = this.mConsumeDownEvent;
        int cnt = this.KeyActionList.size();
        int i = 0;
        while (true) {
            if (i >= cnt) {
                break;
            }
            KeyAction Keycodeinfo = this.KeyActionList.get(i);
            if (Keycodeinfo.mKeyCode != keyCode) {
                i++;
            } else {
                ret2 = Keycodeinfo.mConsumeDownEvent;
                this.KeyActionList.remove(i);
                break;
            }
        }
        if (ret2) {
            IWnnImeEvent wnnEvent = new IWnnImeEvent(event);
            if (keyCode == 4 && event.isTracking() && !event.isCanceled()) {
                wnnEvent.code = IWnnImeEvent.INPUT_KEY;
            }
            ret = onEvent(wnnEvent);
        } else {
            ret = super.onKeyUp(keyCode, event);
        }
        if (DEBUG) {
            Log.d(TAG, "onKeyUp(" + keyCode + ")");
            return ret;
        }
        return ret;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onKeyLongPress()  Unprocessing onCreate() ");
            return super.onKeyLongPress(keyCode, event);
        }
        if (DEBUG) {
            Log.d(TAG, "onKeyLongPress(" + keyCode + ")");
        }
        IWnnImeEvent wnnEvent = new IWnnImeEvent(event);
        wnnEvent.code = IWnnImeEvent.KEYLONGPRESS;
        return onEvent(wnnEvent);
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onStartInput()  Unprocessing onCreate() ");
            super.onStartInput(attribute, restarting);
            return;
        }
        this.mEditorSelectionStart = attribute.initialSelStart;
        this.mEditorSelectionEnd = attribute.initialSelEnd;
        super.onStartInput(attribute, restarting);
        if (!restarting && this.mComposingText != null && !isKeepInput()) {
            this.mComposingText.clear();
        }
        if (this.mIwnnIme != null) {
            this.mIwnnIme.onStartInput(attribute, restarting);
        }
        if (DEBUG) {
            Log.d(TAG, "onStartInput()");
        }
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        if (!IWnnCore.hasLibrary()) {
            Context context = getApplicationContext();
            Toast.makeText(context, R.string.ti_message_not_work_txt, 0).show();
            mWnn = null;
            setInputView(new View(context));
            setCandidatesView(new View(context));
            return;
        }
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onStartInputView()  Unprocessing onCreate() ");
            return;
        }
        PowerManager powerManager = (PowerManager) getSystemService("power");
        boolean isScreenOn = false;
        if (powerManager != null) {
            isScreenOn = powerManager.isScreenOn();
        }
        if (isScreenOn) {
            boolean prevState = this.mIsNotifyCursorAnchorInfo;
            this.mIsNotifyCursorAnchorInfo = false;
            Resources res = getResources();
            Configuration config = res.getConfiguration();
            boolean isReCreateCandidateList = false;
            this.mEditorTargetSdkVersion = getEditorTargetSdkVersion(attribute.packageName);
            if (config.hardKeyboardHidden != 2) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    boolean ret = ic.requestCursorUpdates(2);
                    if (ret) {
                        this.mIsNotifyCursorAnchorInfo = true;
                    }
                }
                if (this.mCandidatesManager != null) {
                    isReCreateCandidateList = this.mCandidatesManager.isRecreateHWCandidateList(this.mEditorTargetSdkVersion);
                }
            }
            if (prevState != this.mIsNotifyCursorAnchorInfo || isReCreateCandidateList) {
                this.mInputViewBaseLayout = null;
                this.mCreatedCandidatesView = false;
            }
        }
        if (!isCreatedInputView()) {
            View inputView = onCreateInputView();
            if (!isCreatedInputView()) {
                Log.e(TAG, "IWnnIME::onStartInputView() failed create input view");
                return;
            }
            setInputView(inputView);
        }
        if (!this.mCreatedCandidatesView) {
            View candidatesView = onCreateCandidatesView();
            if (!this.mCreatedCandidatesView) {
                Log.e(TAG, "IWnnIME::onStartInputView() failed create candidates view");
                return;
            }
            setCandidatesView(candidatesView);
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (this.mDefaultSoftKeyboard != null) {
            this.mDefaultSoftKeyboard.setPreferences(pref, attribute);
        }
        String currentLocale = getSubtypeLocale(WnnUtility.getCurrentInputMethodSubtype(this));
        int languageType = LanguageManager.getChosenLanguageType(currentLocale);
        setLanguage(LanguageManager.getChosenLocale(languageType), languageType, true);
        if (this.mIwnnIme != null) {
            this.mIwnnIme.onStartInputView(attribute, restarting);
        }
        KeyboardSkinData.getInstance().setPreferences(pref);
        PackageManager pm = getPackageManager();
        if (pm != null) {
            try {
                this.mTargetApplicationInfo = null;
                this.mTargetApplicationInfo = pm.getApplicationInfo(attribute.packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "IWnnIME::onStartInputView " + e.toString());
            }
        }
        if (isScreenOn) {
            this.mEnableAnnounce = true;
            if (this.mDefaultSoftKeyboard != null) {
                this.mDefaultSoftKeyboard.announceStateOfKeyboard();
            }
            if (!this.mHandler.hasMessages(1)) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), 30L);
            }
        }
        if (PERFORMANCE_DEBUG) {
            Log.d(TAG, "IWnnIME::onStartInputView()  End");
        }
    }

    public void startInputView(EditorInfo attribute) {
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onStartInputView()  Unprocessing onCreate() ");
            return;
        }
        if (!isCreatedInputView() || !this.mCreatedCandidatesView) {
            Log.e(TAG, "IWnnIME::onStartInputView() failed create input or candidates view ");
            return;
        }
        this.mAttribute = attribute;
        if (DEBUG) {
            Log.d(TAG, "onStartInputView()");
        }
        if (getCurrentInputConnection() != null) {
            this.mDirectInputMode = false;
            if (!isKeepInput() && this.mWnnEngine != null) {
                this.mWnnEngine.init(getFilesDirPath());
            }
        } else {
            this.mDirectInputMode = true;
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref != null) {
            if (this.mCandidatesManager != null) {
                this.mCandidatesManager.setPreferences(pref);
            }
            if (this.mDefaultSoftKeyboard != null) {
                this.mDefaultSoftKeyboard.setPreferences(pref, attribute);
            }
            if (this.mWnnEngine != null) {
                this.mWnnEngine.setPreferences(pref);
            }
        }
        decoEmojiBindStart();
        startEmojiAssist();
    }

    @Override
    public void onUpdateExtractingViews(EditorInfo attribute) {
        Bundle bundle;
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onUpdateExtractingViews()  Unprocessing onCreate() ");
            super.onUpdateExtractingViews(attribute);
            return;
        }
        super.onUpdateExtractingViews(attribute);
        ExtractEditText extractEditText = getExtractEditText();
        if (extractEditText != null && this.mAttribute != null && (bundle = this.mAttribute.extras) != null) {
            boolean allowDecoEmoji = bundle.getBoolean("allowDecoEmoji");
            Bundle extraBundle = extractEditText.getInputExtras(true);
            extraBundle.putBoolean("allowDecoEmoji", allowDecoEmoji);
            if (allowDecoEmoji) {
                EmojiAssist assist = EmojiAssist.getInstance();
                assist.removeView(extractEditText);
                assist.addView(extractEditText, false);
            }
        }
    }

    @Override
    public void setCandidatesViewShown(boolean shown) {
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::setCandidatesViewShown()  Unprocessing onCreate() ");
            super.setCandidatesViewShown(shown);
            return;
        }
        super.setCandidatesViewShown(shown);
        if (shown) {
            showWindow(true);
        } else if (this.mAutoHideMode && this.mDefaultSoftKeyboard == null) {
            closeInputView();
        }
        if (DEBUG) {
            Log.d(TAG, "setCandidatesViewShown(" + shown + ")");
        }
    }

    @Override
    public void hideWindow() {
        if (PERFORMANCE_DEBUG) {
            Log.d(TAG, "IWnnIME::hideWindow()  Start");
        }
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::hideWindow()  Unprocessing onCreate() ");
            closeInputView();
            return;
        }
        if (this.mDefaultSoftKeyboard != null && this.mEnableAnnounce) {
            this.mPreParametersKeyboard = this.mDefaultSoftKeyboard.getKeyboardParameters();
            this.mPreParametersShift = this.mDefaultSoftKeyboard.getShiftParameters();
        }
        this.mEnableAnnounce = false;
        if (this.mIwnnIme != null) {
            this.mIwnnIme.hideWindow();
        }
        if (PERFORMANCE_DEBUG) {
            Log.d(TAG, "IWnnIME::hideWindow()  End");
        }
    }

    public void closeInputView() {
        super.hideWindow();
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::hideWindow()  Unprocessing onCreate() ");
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "hideWindow()");
        }
        if (this.mCandidatesManager != null) {
            this.mCandidatesManager.cancelCreateCandidates();
            this.mCandidatesManager.closeDialog();
        }
        if (this.mDefaultSoftKeyboard != null) {
            this.mDefaultSoftKeyboard.cancelEvent();
        }
        this.mKeyboardManager.removeMessages();
        this.mHandler.removeMessages(1);
        cancelToast();
        this.mDirectInputMode = true;
        hideStatusIcon();
        callCheckDecoEmoji();
        endEmojiAssist();
        this.mIsKeep = false;
    }

    @Override
    public void onComputeInsets(InputMethodService.Insets outInsets) {
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onComputeInsets()  Unprocessing onCreate() ");
            super.onComputeInsets(outInsets);
            return;
        }
        super.onComputeInsets(outInsets);
        if (this.mInputViewBaseLayout != null) {
            View view = this.mInputViewBaseLayout.findViewById(R.id.input_view_base_layout_top);
            LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) view.getLayoutParams();
            if (isHwCandWindow()) {
                outInsets.visibleTopInsets = this.mKeyboardManager.getDisplaySize().y;
                outInsets.touchableInsets = 3;
                Rect rect = this.mCandidatesManager.getHwCandWindowRect();
                if (rect != null) {
                    outInsets.touchableRegion.set(rect);
                }
            } else if (this.mKeyboardManager.getFloatingFromPref()) {
                outInsets.visibleTopInsets = this.mKeyboardManager.getDisplaySize().y;
                Point pos = this.mKeyboardManager.getKeyboardPosition();
                Point size = this.mKeyboardManager.getKeyboardSize(true);
                outInsets.touchableInsets = 3;
                outInsets.touchableRegion.set(pos.x, param.height, pos.x + size.x, pos.y + size.y);
            } else {
                outInsets.visibleTopInsets += param.height;
            }
            outInsets.contentTopInsets = outInsets.visibleTopInsets;
        }
    }

    @Override
    public View onCreateExtractTextView() {
        Bundle bundle;
        this.mExtractEditLayout = super.onCreateExtractTextView();
        EmojiAssist assist = EmojiAssist.getInstance();
        if (this.mExtractEditLayout == null) {
            return this.mExtractEditLayout;
        }
        ViewGroup view = (ViewGroup) this.mExtractEditLayout;
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(-1, -1);
        view.setLayoutParams(param);
        LayoutInflater inflater = getLayoutInflater();
        this.mFullScreenViewBaseLayout = (AbsoluteLayout) inflater.inflate(R.layout.fullscreen_view_base_layout, (ViewGroup) null);
        this.mFullScreenViewBaseLayout.addView(view);
        ExtractEditText extractEditText = (ExtractEditText) view.findViewById(android.R.id.inputExtractEditText);
        if (extractEditText == null) {
            return this.mFullScreenViewBaseLayout;
        }
        if (this.mAttribute != null && (bundle = this.mAttribute.extras) != null) {
            boolean allowDecoEmoji = bundle.getBoolean("allowDecoEmoji");
            Bundle extraBundle = extractEditText.getInputExtras(true);
            extraBundle.putBoolean("allowDecoEmoji", allowDecoEmoji);
            if (allowDecoEmoji) {
                assist.removeView(extractEditText);
                assist.addView(extractEditText, false);
            }
        }
        extractEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Spannable text = new SpannableString(s);
                int ret = IWnnIME.this.getEmojiAssist().checkTextData(text);
                CandidatesManager textCandidate = IWnnIME.this.mCandidatesManager;
                if (ret > 1) {
                    IWnnIME.this.startEmojiAssist();
                } else if (textCandidate != null && !textCandidate.checkDecoEmoji()) {
                    IWnnIME.this.endEmojiAssist();
                }
            }
        });
        return this.mFullScreenViewBaseLayout;
    }

    @Override
    public void setExtractView(View view) {
        super.setExtractView(this.mFullScreenViewBaseLayout);
        try {
            Field f = InputMethodService.class.getDeclaredField("mExtractView");
            f.setAccessible(true);
            f.set(this, this.mExtractEditLayout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype) {
        Resources res;
        String packageName;
        InputMethodSubtype currentSubtype = WnnUtility.getCurrentInputMethodSubtype(this);
        if (currentSubtype == null) {
            currentSubtype = KeyboardLanguagePackData.SUBTYPE_JAPANESE_INSTANCE;
        }
        super.onCurrentInputMethodSubtypeChanged(currentSubtype);
        String currentLocale = getSubtypeLocale(currentSubtype);
        if (isSubtypeEmojiInput() && this.mIwnnIme != null && !this.mIwnnIme.isEnableEmojiList()) {
            switchToLastInputMethod(false);
            return;
        }
        if (isInputViewShown() && this.mIwnnIme != null) {
            InputConnection inputConnection = getCurrentInputConnection();
            if (inputConnection != null) {
                inputConnection.finishComposingText();
            }
            this.mIwnnIme.onFinishInput();
            int languageType = LanguageManager.getChosenLanguageType(currentLocale);
            setLanguage(LanguageManager.getChosenLocale(languageType), languageType, true);
            this.mIwnnIme.restartSelf(getCurrentInputEditorInfo());
            cancelToast();
            if (!isSubtypeEmojiInput() && (res = getResources()) != null) {
                int resId = res.getIdentifier(TOAST_MESSAGE_KEY_SELECT_LANGUAGE, "string", getPackageName());
                KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
                if (langPack.isValid() && (packageName = langPack.getEnableKeyboardLanguagePackDataName()) != null && (res = langPack.getResources()) != null) {
                    resId = res.getIdentifier(TOAST_MESSAGE_KEY_SELECT_LANGUAGE, "string", packageName);
                }
                if (res != null) {
                    this.mSelectLangToast = Toast.makeText(this, res.getString(resId), 0);
                    if (this.mSelectLangToast != null) {
                        this.mSelectLangToast.setGravity(80, 0, 0);
                        this.mSelectLangToast.show();
                        return;
                    }
                    return;
                }
                return;
            }
            return;
        }
        int languageType2 = LanguageManager.getChosenLanguageType(currentLocale);
        setLanguage(LanguageManager.getChosenLocale(languageType2), languageType2, true);
    }

    @Override
    public void onFinishInput() {
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onFinishInput()  Unprocessing onCreate() ");
            super.onFinishInput();
        } else if (this.mIwnnIme != null) {
            this.mIwnnIme.onFinishInput();
            super.onFinishInput();
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        if (this.mDefaultSoftKeyboard != null && this.mEnableAnnounce) {
            this.mPreParametersKeyboard = this.mDefaultSoftKeyboard.getKeyboardParameters();
            this.mPreParametersShift = this.mDefaultSoftKeyboard.getShiftParameters();
        }
        this.mEnableAnnounce = false;
        callExtractEditLayoutFinishActionMode();
        super.onFinishInputView(finishingInput);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onConfigurationChanged()  Unprocessing onCreate() ");
            super.onConfigurationChanged(newConfig);
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "IWnnIME.onConfigurationChanged");
        }
        if (this.mIwnnIme != null) {
            this.mIwnnIme.onConfigurationChanged(newConfig);
        }
    }

    public void onConfigurationChangedIWnnIME(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onUpdateSelection()  Unprocessing onCreate() ");
            super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
            return;
        }
        this.mEditorSelectionStart = newSelStart;
        this.mEditorSelectionEnd = newSelEnd;
        if (this.mIwnnIme != null) {
            super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
            this.mIwnnIme.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        }
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        if (getCurrentIme() == null) {
            Log.e(TAG, "IWnnIME::onEvaluateFullscreenMode()  Unprocessing onCreate() ");
            return super.onEvaluateFullscreenMode();
        }
        if (this.mIwnnIme == null) {
            return super.onEvaluateFullscreenMode();
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref == null) {
            return super.onEvaluateFullscreenMode();
        }
        Resources res = getResources();
        if (res == null) {
            return super.onEvaluateFullscreenMode();
        }
        EditorInfo edit = getCurrentInputEditorInfo();
        Configuration config = res.getConfiguration();
        boolean isFullScreen = pref.getBoolean(ControlPanelPrefFragment.FULLSCREEN_MODE_KEY, res.getBoolean(R.bool.fullscreen_mode_default_value));
        if ((edit != null && (edit.imeOptions & 268435456) != 0) || ((config != null && config.hardKeyboardHidden != 2) || !isFullScreen)) {
            return false;
        }
        return super.onEvaluateFullscreenMode();
    }

    @Override
    public boolean onEvaluateInputViewShown() {
        if (getCurrentIme() != null) {
            return true;
        }
        Log.e(TAG, "IWnnIME::onEvaluateInputViewShown()  Unprocessing onCreate() ");
        return super.onEvaluateInputViewShown();
    }

    @Override
    public void onUpdateCursorAnchorInfo(CursorAnchorInfo cursorAnchorInfo) {
        if (isHwCandWindow() && this.mCandidatesManager != null && cursorAnchorInfo != null) {
            RectF inputCharRect = new RectF();
            int startIndex = cursorAnchorInfo.getComposingTextStart();
            int endIndex = cursorAnchorInfo.getSelectionStart();
            if (startIndex < 0 || startIndex >= endIndex) {
                inputCharRect.top = cursorAnchorInfo.getInsertionMarkerTop();
                inputCharRect.bottom = cursorAnchorInfo.getInsertionMarkerBottom();
                inputCharRect.left = cursorAnchorInfo.getInsertionMarkerHorizontal();
                inputCharRect.right = inputCharRect.left;
            } else {
                RectF charBounds = cursorAnchorInfo.getCharacterBounds(startIndex);
                if (charBounds != null) {
                    inputCharRect.top = charBounds.top;
                    inputCharRect.left = charBounds.left;
                    inputCharRect.right = charBounds.right;
                    for (int i = startIndex + 1; i < endIndex; i++) {
                        RectF charBounds2 = cursorAnchorInfo.getCharacterBounds(i);
                        if (charBounds2 != null) {
                            if (inputCharRect.left > charBounds2.left) {
                                inputCharRect.left = charBounds2.left;
                            }
                            if (inputCharRect.right < charBounds2.right) {
                                inputCharRect.right = charBounds2.right;
                            }
                        } else {
                            return;
                        }
                    }
                    inputCharRect.bottom = cursorAnchorInfo.getCharacterBounds(endIndex - 1).bottom;
                } else {
                    return;
                }
            }
            Matrix matrix = cursorAnchorInfo.getMatrix();
            if (matrix != null) {
                matrix.mapRect(inputCharRect);
                Rect rect = new Rect(0, 0, 0, 0);
                getWindow().getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
                int statusBarHeight = rect.top;
                rect.set((int) inputCharRect.left, ((int) inputCharRect.top) - statusBarHeight, (int) inputCharRect.right, ((int) inputCharRect.bottom) - statusBarHeight);
                this.mCandidatesManager.setInputCharRect(rect);
            }
        }
    }

    public synchronized boolean onEvent(IWnnImeEvent ev) {
        boolean zOnEvent = false;
        synchronized (this) {
            if (getCurrentInputConnection() == null) {
                Log.e(TAG, "IWnnIME::onEvent()  InputConnection is not active");
            } else if (this.mIwnnIme != null) {
                zOnEvent = this.mIwnnIme.onEvent(ev);
            }
        }
        return zOnEvent;
    }

    public static Context getContext() {
        Context context = IWnnEngineService.getCurrentService();
        if (mWnn != null) {
            Context context2 = mWnn;
            return context2;
        }
        return context;
    }

    public void setFunfun(int funfun) {
        this.mFunfun = funfun;
        if (this.mComposingExtraView != null && this.mFunfun == 0) {
            setFunfunText(LoggingEvents.EXTRA_CALLING_APP_NAME);
            this.mComposingExtraView.requestLayout();
            this.mComposingExtraView.invalidate();
            this.mComposingExtraView.setVisibility(4);
            this.mExtraViewFloatingContainer.setVisibility(4);
            this.mPopupWindowTimer.cancelShowing();
        }
    }

    public void setFunfunText(CharSequence text) {
        if (this.mComposingExtraView != null) {
            this.mComposingExtraView.setComposingExtraText(text.toString());
            this.mComposingExtraView.requestLayout();
            this.mPopupWindowTimer.postShowFloatingWindow();
            if (!this.mExtraViewFloatingContainer.isShown()) {
                this.mExtraViewFloatingContainer.setVisibility(0);
            }
            if (!this.mComposingExtraView.isShown()) {
                this.mComposingExtraView.setVisibility(0);
            }
        }
    }

    public boolean showSoftInputFromInputMethod(boolean check) {
        IBinder token;
        InputMethodManager manager;
        EditorInfo info;
        if ((check && ((info = getCurrentInputEditorInfo()) == null || info.inputType == 0)) || (token = getIBinder()) == null || (manager = (InputMethodManager) getSystemService("input_method")) == null) {
            return false;
        }
        manager.showSoftInputFromInputMethod(token, 0);
        return true;
    }

    public static void updateTabletMode(Context context) {
        mTabletMode = context.getResources().getBoolean(R.bool.tablet_mode);
    }

    private void registReceiver() {
        IntentFilter filter = new IntentFilter("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        getApplicationContext().registerReceiver(this.mReceiver, filter);
    }

    private void unregistReceiver() {
        getApplicationContext().unregisterReceiver(this.mReceiver);
    }

    private static void closeChannel(FileChannel channel) {
        if (DEBUG) {
            Log.d(TAG, "closeChannel()");
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                Log.e(TAG, "Fail to close FileChannel", e);
            }
        }
    }

    public static int getIntFromNotResetSettingsPreference(Context context, String key, int defValue) {
        SharedPreferences pref;
        SharedPreferences defaultPref;
        SharedPreferences.Editor defaultEditor;
        if (context != null && (pref = context.getSharedPreferences(FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0)) != null && (defaultPref = PreferenceManager.getDefaultSharedPreferences(context)) != null) {
            int ret = defValue;
            if (pref.contains(key)) {
                ret = pref.getInt(key, defValue);
            } else if (defaultPref.contains(key)) {
                ret = defaultPref.getInt(key, defValue);
                SharedPreferences.Editor editor = pref.edit();
                if (editor != null) {
                    editor.putInt(key, ret);
                    editor.commit();
                }
            }
            if (defaultPref.contains(key) && (defaultEditor = defaultPref.edit()) != null) {
                defaultEditor.remove(key);
                defaultEditor.commit();
            }
            return ret;
        }
        return defValue;
    }

    public static boolean getBooleanFromNotResetSettingsPreference(Context context, String key, boolean defValue) {
        SharedPreferences pref;
        SharedPreferences defaultPref;
        SharedPreferences.Editor defaultEditor;
        if (context != null && (pref = context.getSharedPreferences(FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0)) != null && (defaultPref = PreferenceManager.getDefaultSharedPreferences(context)) != null) {
            boolean ret = defValue;
            if (pref.contains(key)) {
                ret = pref.getBoolean(key, defValue);
            } else if (defaultPref.contains(key)) {
                ret = defaultPref.getBoolean(key, defValue);
                SharedPreferences.Editor editor = pref.edit();
                if (editor != null) {
                    editor.putBoolean(key, ret);
                    editor.commit();
                }
            }
            if (defaultPref.contains(key) && (defaultEditor = defaultPref.edit()) != null) {
                defaultEditor.remove(key);
                defaultEditor.commit();
            }
            return ret;
        }
        return defValue;
    }

    public static String getStringFromNotResetSettingsPreference(Context context, String key, String defValue) {
        SharedPreferences pref;
        SharedPreferences defaultPref;
        SharedPreferences.Editor defaultEditor;
        if (context != null && (pref = context.getSharedPreferences(FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0)) != null && (defaultPref = PreferenceManager.getDefaultSharedPreferences(context)) != null) {
            String ret = defValue;
            if (pref.contains(key)) {
                ret = pref.getString(key, defValue);
            } else if (defaultPref.contains(key)) {
                ret = defaultPref.getString(key, defValue);
                SharedPreferences.Editor editor = pref.edit();
                if (editor != null) {
                    editor.putString(key, ret);
                    editor.commit();
                }
            }
            if (defaultPref.contains(key) && (defaultEditor = defaultPref.edit()) != null) {
                defaultEditor.remove(key);
                defaultEditor.commit();
            }
            return ret;
        }
        return defValue;
    }

    public String getFilesDirPath() {
        File dir = getFilesDir();
        if (dir == null) {
            return null;
        }
        return dir.getPath();
    }

    public static String getFilesDirPath(Context context) {
        File dir;
        if (context == null || (dir = context.getFilesDir()) == null) {
            return null;
        }
        return dir.getPath();
    }

    public void cancelToast() {
        if (this.mSelectLangToast != null) {
            this.mSelectLangToast.cancel();
            this.mSelectLangToast = null;
        }
    }

    public boolean isScreenLock() {
        KeyguardManager keyguard = (KeyguardManager) getSystemService("keyguard");
        if (keyguard == null) {
            return false;
        }
        return keyguard.inKeyguardRestrictedInputMode();
    }

    public void processKeyEventDel() {
        if (this.mIwnnIme != null) {
            this.mIwnnIme.processKeyEventDel();
        }
    }

    private void callExtractEditLayoutFinishActionMode() {
        try {
            Field f = InputMethodService.class.getDeclaredField("mExtractView");
            f.setAccessible(true);
            Object object = f.get(this);
            if (isExtractViewShown() && object != null) {
                Class<?> cls = object.getClass();
                if (CLASS_NAME_EXTRACTEDITLAYOUT.equals(cls.getName())) {
                    Method methodActionModeStarted = cls.getMethod("isActionModeStarted", new Class[0]);
                    Object retobj = methodActionModeStarted.invoke(object, new Object[0]);
                    boolean ret = ((Boolean) retobj).booleanValue();
                    if (ret) {
                        Method methodFinishActionMode = cls.getMethod("finishActionMode", new Class[0]);
                        methodFinishActionMode.invoke(object, new Object[0]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closePopupWindow() {
        if (this.mPopupWindow != null && this.mPopupWindow.isShowing()) {
            this.mPopupWindowTimer.cancelShowing();
        }
    }

    public void switchToLastInputMethod(final boolean isClose) {
        final IBinder token;
        final InputMethodManager manager = (InputMethodManager) getSystemService("input_method");
        if (manager != null && (token = getIBinder()) != null) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    boolean ret = manager.switchToLastInputMethod(token);
                    if (!ret) {
                        if (isClose) {
                            if (IWnnIME.this.mDefaultSoftKeyboard != null) {
                                IWnnIME.this.mDefaultSoftKeyboard.closing();
                            }
                            IWnnIME.this.requestHideSelf(0);
                            return null;
                        }
                        IWnnIME.this.switchToNextInputMethod();
                        return null;
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    public boolean switchToNextInputMethod() {
        SharedPreferences pref;
        InputMethodManager manager = (InputMethodManager) getSystemService("input_method");
        if (manager == null || (pref = PreferenceManager.getDefaultSharedPreferences(this)) == null) {
            return false;
        }
        boolean includesOtherImes = pref.getBoolean(ControlPanelPrefFragment.CHANGE_OTHER_IME_KEY, getResources().getBoolean(R.bool.opt_change_otherime_default_value));
        IBinder token = getIBinder();
        if (token != null) {
            return manager.switchToNextInputMethod(token, includesOtherImes ? false : true);
        }
        return false;
    }

    public void updateShortcutIME() {
        Map<InputMethodInfo, List<InputMethodSubtype>> shortcuts;
        this.mShortcutInputMethodInfo = null;
        this.mShortcutSubtype = null;
        InputMethodManager manager = (InputMethodManager) getSystemService("input_method");
        if (manager != null && (shortcuts = manager.getShortcutInputMethodsAndSubtypes()) != null && !shortcuts.isEmpty()) {
            this.mShortcutInputMethodInfo = shortcuts.keySet().iterator().next();
            if (this.mShortcutInputMethodInfo != null) {
                List<InputMethodSubtype> subtypes = shortcuts.get(this.mShortcutInputMethodInfo);
                this.mShortcutSubtype = subtypes.size() > 0 ? subtypes.get(0) : null;
            }
        }
    }

    public void switchToShortcutIME() {
        if (this.mShortcutInputMethodInfo != null) {
            String imiId = this.mShortcutInputMethodInfo.getId();
            InputMethodSubtype subtype = this.mShortcutSubtype;
            switchToTargetIME(imiId, subtype);
        }
    }

    private void switchToTargetIME(final String imiId, final InputMethodSubtype subtype) {
        final IBinder token;
        final InputMethodManager manager = (InputMethodManager) getSystemService("input_method");
        if (manager != null && (token = getIBinder()) != null) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    manager.setInputMethodAndSubtype(token, imiId, subtype);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    public boolean isEnableShortcutIME() {
        updateShortcutIME();
        if (this.mShortcutInputMethodInfo == null) {
            return false;
        }
        return true;
    }

    public boolean isAvailableShortcutIME() {
        ConnectivityManager connectivityManager;
        if (!isEnableShortcutIME()) {
            return false;
        }
        if (this.mShortcutSubtype == null) {
            return true;
        }
        boolean contain = contains(SUBTYPE_EXTRAVALUE_REQUIRE_NETWORK_CONNECTIVITY, this.mShortcutSubtype.getExtraValue());
        if (!contain || (connectivityManager = (ConnectivityManager) getSystemService("connectivity")) == null) {
            return true;
        }
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public boolean getDisableVoiceInputInPrivateImeOptions(EditorInfo editor) {
        if (editor != null) {
            return contains(PRIVATE_IME_OPTION_DISABLE_VOICE_INPUT, editor.privateImeOptions) || contains(PRIVATE_IME_OPTION_DISABLE_VOICE_INPUT_COMPAT, editor.privateImeOptions);
        }
        return false;
    }

    private boolean contains(String keyStr, String splitStr) {
        if (splitStr != null) {
            String[] arr$ = splitStr.split(iWnnEngine.DECO_OPERATION_SEPARATOR);
            for (String option : arr$) {
                if (option.equals(keyStr)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setLanguage(Locale locale, int langtype, boolean requestInitialize) {
        boolean isEqual = false;
        if (DEBUG) {
            Log.d(TAG, "IWnnIME.setLanguage(" + locale.toString() + ")");
        }
        if (IWnnCore.hasLibrary()) {
            KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
            langPack.init(this, langPack.getLangPackClassName(this, langtype));
            if (!langPack.isValid() && langtype != 0) {
                locale = LanguageManager.getChosenLocale(0);
            }
            if (this.mSelectedLocale != null && this.mSelectedLocale.equals(locale)) {
                isEqual = true;
            }
            if (!isEqual) {
                boolean ret = selectLanguage(locale);
                if (ret) {
                    iWnnEngine engine = iWnnEngine.getEngine();
                    if (engine != null) {
                        engine.close();
                    }
                    this.mSelectedLocale = locale;
                }
                if (requestInitialize) {
                    if (this.mIwnnIme != null) {
                        this.mIwnnIme.onCreate();
                        this.mIwnnIme.onCreateInputView();
                    }
                    WindowManager wm = (WindowManager) getSystemService("window");
                    if (wm != null && this.mDefaultSoftKeyboard != null) {
                        this.mDefaultSoftKeyboard.initView(wm.getDefaultDisplay().getWidth());
                    }
                    this.mKeyboardManager.removeKeyboardFromParent();
                }
            }
        }
    }

    private boolean selectLanguage(Locale locale) {
        int languageType;
        IWnnImeBase wnn = null;
        int category = -1;
        KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
        if (langPack.isValid()) {
            languageType = LanguageManager.getChosenLanguageType(locale);
            if (languageType != -1) {
                category = langPack.getLangCategory(this);
            }
        } else {
            languageType = 0;
            category = 0;
        }
        switch (category) {
            case 0:
                wnn = new IWnnImeJaJp(this);
                break;
            case 1:
                wnn = new IWnnImeLatin(this, languageType);
                break;
            case 2:
                wnn = new IWnnImeZh(this, languageType);
                break;
            case 3:
                wnn = new IWnnImeHangul(this, languageType);
                break;
        }
        if (wnn == null && this.mIwnnIme == null) {
            wnn = new IWnnImeJaJp(this);
        }
        if (wnn == null) {
            return false;
        }
        if (this.mIwnnIme != null) {
            this.mIwnnIme.close(true);
        }
        this.mIwnnIme = wnn;
        return true;
    }

    public static String getSubtypeLocaleDirect(InputMethodSubtype subtype) {
        if (subtype == null) {
            return "ja";
        }
        String locale = subtype.getLocale();
        String tempLocale = LanguageManager.getChosenLocaleCode(locale);
        if (tempLocale != null) {
            return tempLocale;
        }
        return locale.toLowerCase();
    }

    private String getSubtypeLocale(InputMethodSubtype subtype) {
        this.mSubtypeEmojiInput = false;
        if (subtype == null) {
            return "ja";
        }
        String locale = getSubtypeLocaleDirect(subtype);
        if (locale.equals(SUBTYPE_LOCALE_EMOJI_INPUT)) {
            this.mSubtypeEmojiInput = true;
            if (this.mSelectedLocale != null) {
                return LanguageManager.getChosenLocaleCode(this.mSelectedLocale);
            }
            return "ja";
        }
        return locale;
    }

    public void startControlPanelStandard() {
        Intent intent = new Intent();
        intent.setClass(this, ControlPanelStandard.class);
        intent.setFlags(268435456);
        startActivity(intent);
    }

    private void decoEmojiBindStart() {
        if (DEBUG) {
            Log.d(TAG, "decoEmojiBindStart() START");
        }
        EmojiAssist assist = EmojiAssist.getInstance();
        if (assist != null) {
            int functype = assist.getEmojiFunctionType();
            if (functype == 0) {
                return;
            }
        }
        Intent intent = new Intent();
        intent.setClassName("jp.co.omronsoft.android.decoemojimanager", DecoEmojiUtil.DECOEMOJIMANAGER_CLASSNAME);
        boolean success = bindService(intent, this.mServiceConn, 1);
        if (success) {
            this.mIsBind = true;
        }
        if (DEBUG) {
            Log.d(TAG, "decoEmojiBindStart() END");
        }
    }

    public IDecoEmojiManager getDecoEmojiBindInterface() {
        if (DEBUG) {
            Log.d(TAG, "getDecoEmojiBindInterface() START");
        }
        return this.mDecoEmojiInterface;
    }

    private void callCheckDecoEmoji() {
        if (DEBUG) {
            Log.d(TAG, "callCheckDecoEmoji() START");
        }
        if (DEBUG) {
            Log.d(TAG, "callCheckDecoEmoji() END");
        }
    }

    public EmojiAssist getEmojiAssist() {
        if (this.mEmojiAssist == null) {
            this.mEmojiAssist = EmojiAssist.getInstance();
        }
        return this.mEmojiAssist;
    }

    public void startEmojiAssist() {
        if (this.mEmojiAssist == null) {
            this.mEmojiAssist = EmojiAssist.getInstance();
        }
        this.mEmojiAssist.setPictureScale(true);
        this.mIsEmojiAssistWorking = true;
        this.mEmojiAssist.startAnimation();
    }

    public void endEmojiAssist() {
        if (this.mEmojiAssist == null) {
            this.mEmojiAssist = EmojiAssist.getInstance();
        }
        this.mEmojiAssist.stopAnimation();
        this.mIsEmojiAssistWorking = false;
    }

    public ExtractEditText getExtractEditText() {
        try {
            Field f = InputMethodService.class.getDeclaredField("mExtractEditText");
            f.setAccessible(true);
            Object object = f.get(this);
            ExtractEditText extractEditText = (ExtractEditText) object;
            return extractEditText;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int getPreferenceId() {
        return getIntFromNotResetSettingsPreference(this, DecoEmojiListener.PREF_KEY, -1);
    }

    public static void copyPresetDecoEmojiGijiDictionary() {
        if (DEBUG) {
            Log.d(TAG, "copyPresetDecoEmojiGijiDictionary()");
        }
        File decoEmojiGijiDic = new File(getFilesDirPath(getContext()) + DECOEMOJI_GIJI_DICTIONALY_PATH + DECOEMOJI_GIJI_DICTIONALY_NAME);
        if (decoEmojiGijiDic.exists()) {
            if (DEBUG) {
                Log.d(TAG, "  DecoEmoji pseudo dictionary is found");
                return;
            }
            return;
        }
        File presetDecoEmojiGijiDic = new File(Environment.getRootDirectory() + "/etc/" + DECOEMOJI_GIJI_DICTIONALY_NAME);
        if (!presetDecoEmojiGijiDic.exists()) {
            if (DEBUG) {
                Log.d(TAG, "  Preset DecoEmoji pseudo dictionary is not found");
                return;
            }
            return;
        }
        FileChannel channelSrc = null;
        FileChannel channelDest = null;
        long baseByte = 0;
        long copyByte = 0;
        try {
            try {
                File folder = new File(decoEmojiGijiDic.getParent());
                boolean mkdirret = folder.mkdirs();
                if (mkdirret) {
                    channelSrc = new FileInputStream(presetDecoEmojiGijiDic).getChannel();
                    channelDest = new FileOutputStream(decoEmojiGijiDic).getChannel();
                    baseByte = channelSrc.size();
                    copyByte = channelSrc.transferTo(0L, baseByte, channelDest);
                    closeChannel(channelSrc);
                    closeChannel(channelDest);
                    if (baseByte != copyByte) {
                        Log.e(TAG, "Fail to copy preset DecoEmoji pseudo dictionary : size error!!");
                        if (decoEmojiGijiDic.exists() && !decoEmojiGijiDic.delete()) {
                            Log.e(TAG, "Fail to copy preset DecoEmoji pseudo dictionary : delete error!!");
                        }
                    }
                } else {
                    Log.e(TAG, "Fail to copy preset DecoEmoji pseudo dictionary : mkdir fail!!");
                    closeChannel(null);
                    closeChannel(null);
                    if (0 != 0) {
                        Log.e(TAG, "Fail to copy preset DecoEmoji pseudo dictionary : size error!!");
                        if (decoEmojiGijiDic.exists() && !decoEmojiGijiDic.delete()) {
                            Log.e(TAG, "Fail to copy preset DecoEmoji pseudo dictionary : delete error!!");
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Fail to copy preset DecoEmoji pseudo dictionary", e);
                closeChannel(channelSrc);
                closeChannel(channelDest);
                if (baseByte != copyByte) {
                    Log.e(TAG, "Fail to copy preset DecoEmoji pseudo dictionary : size error!!");
                    if (!decoEmojiGijiDic.exists() || decoEmojiGijiDic.delete()) {
                        return;
                    }
                    Log.e(TAG, "Fail to copy preset DecoEmoji pseudo dictionary : delete error!!");
                }
            }
        } catch (Throwable th) {
            closeChannel(channelSrc);
            closeChannel(channelDest);
            if (baseByte != copyByte) {
                Log.e(TAG, "Fail to copy preset DecoEmoji pseudo dictionary : size error!!");
                if (decoEmojiGijiDic.exists() && !decoEmojiGijiDic.delete()) {
                    Log.e(TAG, "Fail to copy preset DecoEmoji pseudo dictionary : delete error!!");
                }
            }
            throw th;
        }
    }

    public static boolean isDebugging() {
        return DEBUG;
    }

    public static boolean isPerformanceDebugging() {
        return PERFORMANCE_DEBUG;
    }

    public static IWnnIME getCurrentIme() {
        return mWnn;
    }

    public IWnnImeBase getCurrentIWnnIme() {
        return this.mIwnnIme;
    }

    public void setCurrentCandidatesManager(CandidatesManager manager) {
        this.mCandidatesManager = manager;
    }

    public CandidatesManager getCurrentCandidatesManager() {
        return this.mCandidatesManager;
    }

    public void setCurrentDefaultSoftKeyboard(DefaultSoftKeyboard keyboard) {
        this.mDefaultSoftKeyboard = keyboard;
    }

    public DefaultSoftKeyboard getCurrentDefaultSoftKeyboard() {
        return this.mDefaultSoftKeyboard;
    }

    public void setCurrentEngine(WnnEngine engine) {
        this.mWnnEngine = engine;
    }

    public WnnEngine getCurrentEngine() {
        return this.mWnnEngine;
    }

    public void setComposingText(ComposingText composingText) {
        this.mComposingText = composingText;
    }

    public ComposingText getComposingText() {
        return this.mComposingText;
    }

    public void setAutoHideMode(boolean mode) {
        this.mAutoHideMode = mode;
    }

    public boolean isAutoHideMode() {
        return this.mAutoHideMode;
    }

    public boolean isHwCandWindow() {
        return this.mCandidatesManager != null && this.mIsNotifyCursorAnchorInfo && !this.mCandidatesManager.isSymbolMode() && this.mEditorTargetSdkVersion >= 4;
    }

    public void setDirectInputMode(boolean mode) {
        this.mDirectInputMode = mode;
    }

    public boolean isDirectInputMode() {
        return this.mDirectInputMode;
    }

    public static boolean isTabletMode() {
        return mTabletMode;
    }

    public int getFunfun() {
        return this.mFunfun;
    }

    public EditorInfo getEditorInfo() {
        return this.mAttribute;
    }

    public void setStateOfKeepInput(boolean in) {
        this.mIsKeep = in;
    }

    public boolean isKeepInput() {
        return this.mIsKeep;
    }

    public Locale getSelectedLocale() {
        return this.mSelectedLocale;
    }

    public boolean isSubtypeEmojiInput() {
        return this.mSubtypeEmojiInput;
    }

    public ApplicationInfo getTargetApplicationInfo() {
        return this.mTargetApplicationInfo;
    }

    public int getEditorSelectionStart() {
        return this.mEditorSelectionStart;
    }

    public int getEditorSelectionEnd() {
        return this.mEditorSelectionEnd;
    }

    public KeyboardManager getCurrentKeyboardManager() {
        return this.mKeyboardManager;
    }

    public void setEmojiFlag(boolean emoji) {
        this.mIsEmoji = emoji;
    }

    public boolean isEmoji() {
        return this.mIsEmoji;
    }

    public static void setEmojiType(int emojiType) {
        mEmojiType = emojiType;
    }

    public static int getEmojiType() {
        return mEmojiType;
    }

    public void updateInputBackView(int topHeight) {
        if (this.mInputViewBaseLayout != null) {
            int displayHeight = this.mKeyboardManager.getDisplaySize().y;
            boolean isFullScreenMode = onEvaluateFullscreenMode();
            View view = this.mInputViewBaseLayout.findViewById(R.id.input_view_base_layout_top);
            if (isFullScreenMode) {
                view.setVisibility(8);
            } else {
                view.setVisibility(0);
                LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) view.getLayoutParams();
                param.height = topHeight;
                view.setLayoutParams(param);
            }
            boolean isFloatingMode = this.mKeyboardManager.getFloatingFromPref();
            View view2 = this.mInputViewBaseLayout.findViewById(R.id.input_view_base_layout_bottom);
            if (isFloatingMode && isFullScreenMode) {
                view2.setVisibility(8);
                return;
            }
            view2.setVisibility(0);
            int color = R.color.input_view_base_layout_opaque_color;
            if (isFloatingMode || (this.mIwnnIme != null && !this.mIwnnIme.isHardKeyboardHidden())) {
                color = R.color.input_view_base_layout_transparent_color;
            }
            view2.setBackgroundResource(color);
            LinearLayout.LayoutParams param2 = (LinearLayout.LayoutParams) view2.getLayoutParams();
            param2.height = displayHeight - topHeight;
            view2.setLayoutParams(param2);
        }
    }

    public AbsoluteLayout getLayoutTop() {
        if (this.mInputViewBaseLayout != null) {
            return (AbsoluteLayout) this.mInputViewBaseLayout.findViewById(R.id.input_view_base_layout_top);
        }
        return null;
    }

    public AbsoluteLayout getLayoutOfSetKeyboard() {
        if (this.mInputViewBaseLayout != null) {
            return (AbsoluteLayout) this.mInputViewBaseLayout.findViewById(R.id.input_view_base_layout_bottom);
        }
        return null;
    }

    public AbsoluteLayout getFullScreenViewBaseLayout() {
        return this.mFullScreenViewBaseLayout;
    }

    public void replaceKeyboard() {
        this.mKeyboardManager.removeMessages();
        this.mKeyboardManager.removeKeyboardFromParent();
        this.mIsKeep = this.mIwnnIme.isKeepInput();
        onStartInput(getCurrentInputEditorInfo(), true);
        onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_INPUT_CANDIDATE_VIEW));
        onStartInputView(getCurrentInputEditorInfo(), false);
        if (isInputViewShown()) {
            if ((this.mIwnnIme instanceof IWnnImeJaJp) && (this.mWnnEngine instanceof IWnnSymbolEngine)) {
                IWnnSymbolEngine symbolEngine = (IWnnSymbolEngine) this.mWnnEngine;
                if (symbolEngine.getMode() == 6) {
                    symbolEngine.setMode(6);
                }
            }
            this.mIwnnIme.updateViewStatus(true, true);
        }
        this.mIsKeep = false;
    }

    public int getHeightOfFunFunWindow() {
        if (this.mExtraViewFloatingContainer == null) {
            return 0;
        }
        return this.mExtraViewFloatingContainer.getMeasuredHeight();
    }

    private boolean isCreatedInputView() {
        if (this.mInputViewBaseLayout == null) {
            return false;
        }
        return true;
    }

    protected void onAccessibilityStateChanged() {
        EditorInfo editorInfo = getCurrentInputEditorInfo();
        if (editorInfo != null) {
            replaceKeyboard();
            if (this.mWnnEngine instanceof IWnnSymbolEngine) {
                IWnnSymbolEngine symbolEngine = (IWnnSymbolEngine) this.mWnnEngine;
                symbolEngine.setLastSymbollist(symbolEngine.getMode());
                onEvent(new IWnnImeEvent(IWnnImeEvent.START_SYMBOL_MODE));
            }
        }
    }

    private IBinder getIBinder() {
        Window window;
        WindowManager.LayoutParams params;
        IBinder token;
        Dialog dialog = getWindow();
        if (dialog == null || (window = dialog.getWindow()) == null || (params = window.getAttributes()) == null || (token = params.token) == null) {
            return null;
        }
        return token;
    }

    public void setEnableAnnounce(boolean enable) {
        this.mEnableAnnounce = enable;
    }

    public boolean isEnableAnnounce() {
        return this.mEnableAnnounce;
    }

    public void setPreKeyboardParameters(ArrayList<Object> parameters) {
        this.mPreParametersKeyboard = parameters;
    }

    public ArrayList<Object> getPreKeyboardParameters() {
        return this.mPreParametersKeyboard;
    }

    public void setPreShiftParameters(ArrayList<Boolean> parameters) {
        this.mPreParametersShift = parameters;
    }

    public ArrayList<Boolean> getPreShiftParameters() {
        return this.mPreParametersShift;
    }

    private int getEditorTargetSdkVersion(String editorPackageName) {
        if (editorPackageName == null) {
            return 1;
        }
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(editorPackageName, 0);
            if (info != null) {
                return info.targetSdkVersion;
            }
            return 1;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 1;
        }
    }
}
