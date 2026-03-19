package android.inputmethodservice;

import android.R;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.Region;
import android.inputmethodservice.AbstractInputMethodService;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.provider.Settings;
import android.provider.SettingsStringUtil;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.MovementMethod;
import android.util.Log;
import android.util.PrintWriterPrinter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class InputMethodService extends AbstractInputMethodService {
    public static final int BACK_DISPOSITION_ADJUST_NOTHING = 3;
    public static final int BACK_DISPOSITION_DEFAULT = 0;
    private static final int BACK_DISPOSITION_MAX = 3;
    private static final int BACK_DISPOSITION_MIN = 0;

    @Deprecated
    public static final int BACK_DISPOSITION_WILL_DISMISS = 2;

    @Deprecated
    public static final int BACK_DISPOSITION_WILL_NOT_DISMISS = 1;
    static final boolean DEBUG = false;
    public static final int IME_ACTIVE = 1;
    public static final int IME_VISIBLE = 2;
    static final int MOVEMENT_DOWN = -1;
    static final int MOVEMENT_UP = -2;
    static final String TAG = "InputMethodService";
    int mBackDisposition;
    FrameLayout mCandidatesFrame;
    boolean mCandidatesViewStarted;
    int mCandidatesVisibility;
    CompletionInfo[] mCurCompletions;
    ViewGroup mExtractAccessories;
    View mExtractAction;
    ExtractEditText mExtractEditText;
    FrameLayout mExtractFrame;
    View mExtractView;
    boolean mExtractViewHidden;
    ExtractedText mExtractedText;
    int mExtractedToken;
    boolean mFullscreenApplied;
    ViewGroup mFullscreenArea;
    InputMethodManager mImm;
    boolean mInShowWindow;
    LayoutInflater mInflater;
    boolean mInitialized;
    InputBinding mInputBinding;
    InputConnection mInputConnection;
    EditorInfo mInputEditorInfo;
    FrameLayout mInputFrame;
    boolean mInputStarted;
    View mInputView;
    boolean mInputViewStarted;
    boolean mIsFullscreen;
    boolean mIsInputViewShown;
    boolean mLastShowInputRequested;
    View mRootView;
    private SettingsObserver mSettingsObserver;
    boolean mShouldClearInsetOfPreviousIme;
    int mShowInputFlags;
    boolean mShowInputRequested;
    private IBinder mStartInputToken;
    InputConnection mStartedInputConnection;
    int mStatusIcon;
    TypedArray mThemeAttrs;
    IBinder mToken;
    SoftInputWindow mWindow;
    boolean mWindowAdded;
    boolean mWindowCreated;
    boolean mWindowVisible;
    boolean mWindowWasVisible;
    int mTheme = 0;
    final Insets mTmpInsets = new Insets();
    final int[] mTmpLocation = new int[2];
    final ViewTreeObserver.OnComputeInternalInsetsListener mInsetsComputer = new ViewTreeObserver.OnComputeInternalInsetsListener() {
        @Override
        public final void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
            InputMethodService.lambda$new$0(this.f$0, internalInsetsInfo);
        }
    };
    final View.OnClickListener mActionClickListener = new View.OnClickListener() {
        @Override
        public final void onClick(View view) {
            InputMethodService.lambda$new$1(this.f$0, view);
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface BackDispositionMode {
    }

    public static final class Insets {
        public static final int TOUCHABLE_INSETS_CONTENT = 1;
        public static final int TOUCHABLE_INSETS_FRAME = 0;
        public static final int TOUCHABLE_INSETS_REGION = 3;
        public static final int TOUCHABLE_INSETS_VISIBLE = 2;
        public int contentTopInsets;
        public int touchableInsets;
        public final Region touchableRegion = new Region();
        public int visibleTopInsets;
    }

    public static void lambda$new$0(InputMethodService inputMethodService, ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
        if (inputMethodService.isExtractViewShown()) {
            View decorView = inputMethodService.getWindow().getWindow().getDecorView();
            Rect rect = internalInsetsInfo.contentInsets;
            Rect rect2 = internalInsetsInfo.visibleInsets;
            int height = decorView.getHeight();
            rect2.top = height;
            rect.top = height;
            internalInsetsInfo.touchableRegion.setEmpty();
            internalInsetsInfo.setTouchableInsets(0);
            return;
        }
        inputMethodService.onComputeInsets(inputMethodService.mTmpInsets);
        internalInsetsInfo.contentInsets.top = inputMethodService.mTmpInsets.contentTopInsets;
        internalInsetsInfo.visibleInsets.top = inputMethodService.mTmpInsets.visibleTopInsets;
        internalInsetsInfo.touchableRegion.set(inputMethodService.mTmpInsets.touchableRegion);
        internalInsetsInfo.setTouchableInsets(inputMethodService.mTmpInsets.touchableInsets);
    }

    public static void lambda$new$1(InputMethodService inputMethodService, View view) {
        EditorInfo currentInputEditorInfo = inputMethodService.getCurrentInputEditorInfo();
        InputConnection currentInputConnection = inputMethodService.getCurrentInputConnection();
        if (currentInputEditorInfo != null && currentInputConnection != null) {
            if (currentInputEditorInfo.actionId != 0) {
                currentInputConnection.performEditorAction(currentInputEditorInfo.actionId);
            } else if ((currentInputEditorInfo.imeOptions & 255) != 1) {
                currentInputConnection.performEditorAction(currentInputEditorInfo.imeOptions & 255);
            }
        }
    }

    public class InputMethodImpl extends AbstractInputMethodService.AbstractInputMethodImpl {
        public InputMethodImpl() {
            super();
        }

        @Override
        public void attachToken(IBinder iBinder) {
            if (InputMethodService.this.mToken == null) {
                InputMethodService.this.mToken = iBinder;
                InputMethodService.this.mWindow.setToken(iBinder);
            }
        }

        @Override
        public void bindInput(InputBinding inputBinding) {
            InputMethodService.this.mInputBinding = inputBinding;
            InputMethodService.this.mInputConnection = inputBinding.getConnection();
            if (InputMethodService.this.mImm != null && InputMethodService.this.mToken != null) {
                InputMethodService.this.mImm.reportFullscreenMode(InputMethodService.this.mToken, InputMethodService.this.mIsFullscreen);
            }
            InputMethodService.this.initialize();
            InputMethodService.this.onBindInput();
        }

        @Override
        public void unbindInput() {
            InputMethodService.this.onUnbindInput();
            InputMethodService.this.mInputBinding = null;
            InputMethodService.this.mInputConnection = null;
        }

        @Override
        public void startInput(InputConnection inputConnection, EditorInfo editorInfo) {
            InputMethodService.this.doStartInput(inputConnection, editorInfo, false);
        }

        @Override
        public void restartInput(InputConnection inputConnection, EditorInfo editorInfo) {
            InputMethodService.this.doStartInput(inputConnection, editorInfo, true);
        }

        @Override
        public void dispatchStartInputWithToken(InputConnection inputConnection, EditorInfo editorInfo, boolean z, IBinder iBinder) {
            InputMethodService.this.mStartInputToken = iBinder;
            super.dispatchStartInputWithToken(inputConnection, editorInfo, z, iBinder);
        }

        @Override
        public void hideSoftInput(int i, ResultReceiver resultReceiver) {
            boolean zIsInputViewShown = InputMethodService.this.isInputViewShown();
            int i2 = 0;
            InputMethodService.this.mShowInputFlags = 0;
            InputMethodService.this.mShowInputRequested = false;
            InputMethodService.this.doHideWindow();
            InputMethodService.this.clearInsetOfPreviousIme();
            if (resultReceiver != null) {
                if (zIsInputViewShown != InputMethodService.this.isInputViewShown()) {
                    i2 = 3;
                } else if (!zIsInputViewShown) {
                    i2 = 1;
                }
                resultReceiver.send(i2, null);
            }
        }

        @Override
        public void showSoftInput(int i, ResultReceiver resultReceiver) {
            boolean zIsInputViewShown = InputMethodService.this.isInputViewShown();
            int i2 = 0;
            if (InputMethodService.this.dispatchOnShowInputRequested(i, false)) {
                try {
                    InputMethodService.this.showWindow(true);
                } catch (WindowManager.BadTokenException e) {
                }
            }
            InputMethodService.this.clearInsetOfPreviousIme();
            InputMethodService.this.mImm.setImeWindowStatus(InputMethodService.this.mToken, InputMethodService.this.mStartInputToken, InputMethodService.mapToImeWindowStatus(InputMethodService.this.isInputViewShown()), InputMethodService.this.mBackDisposition);
            if (resultReceiver != null) {
                if (zIsInputViewShown != InputMethodService.this.isInputViewShown()) {
                    i2 = 2;
                } else if (!zIsInputViewShown) {
                    i2 = 1;
                }
                resultReceiver.send(i2, null);
            }
        }

        @Override
        public void changeInputMethodSubtype(InputMethodSubtype inputMethodSubtype) {
            InputMethodService.this.onCurrentInputMethodSubtypeChanged(inputMethodSubtype);
        }
    }

    public class InputMethodSessionImpl extends AbstractInputMethodService.AbstractInputMethodSessionImpl {
        public InputMethodSessionImpl() {
            super();
        }

        @Override
        public void finishInput() {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.doFinishInput();
        }

        @Override
        public void displayCompletions(CompletionInfo[] completionInfoArr) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.mCurCompletions = completionInfoArr;
            InputMethodService.this.onDisplayCompletions(completionInfoArr);
        }

        @Override
        public void updateExtractedText(int i, ExtractedText extractedText) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onUpdateExtractedText(i, extractedText);
        }

        @Override
        public void updateSelection(int i, int i2, int i3, int i4, int i5, int i6) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onUpdateSelection(i, i2, i3, i4, i5, i6);
        }

        @Override
        public void viewClicked(boolean z) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onViewClicked(z);
        }

        @Override
        public void updateCursor(Rect rect) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onUpdateCursor(rect);
        }

        @Override
        public void appPrivateCommand(String str, Bundle bundle) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onAppPrivateCommand(str, bundle);
        }

        @Override
        public void toggleSoftInput(int i, int i2) {
            InputMethodService.this.onToggleSoftInput(i, i2);
        }

        @Override
        public void updateCursorAnchorInfo(CursorAnchorInfo cursorAnchorInfo) {
            if (!isEnabled()) {
                return;
            }
            InputMethodService.this.onUpdateCursorAnchorInfo(cursorAnchorInfo);
        }
    }

    private static final class SettingsObserver extends ContentObserver {
        private final InputMethodService mService;
        private int mShowImeWithHardKeyboard;

        @Retention(RetentionPolicy.SOURCE)
        private @interface ShowImeWithHardKeyboardType {
            public static final int FALSE = 1;
            public static final int TRUE = 2;
            public static final int UNKNOWN = 0;
        }

        private SettingsObserver(InputMethodService inputMethodService) {
            super(new Handler(inputMethodService.getMainLooper()));
            this.mShowImeWithHardKeyboard = 0;
            this.mService = inputMethodService;
        }

        public static SettingsObserver createAndRegister(InputMethodService inputMethodService) {
            SettingsObserver settingsObserver = new SettingsObserver(inputMethodService);
            inputMethodService.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.SHOW_IME_WITH_HARD_KEYBOARD), false, settingsObserver);
            return settingsObserver;
        }

        void unregister() {
            this.mService.getContentResolver().unregisterContentObserver(this);
        }

        private boolean shouldShowImeWithHardKeyboard() {
            if (this.mShowImeWithHardKeyboard == 0) {
                this.mShowImeWithHardKeyboard = Settings.Secure.getInt(this.mService.getContentResolver(), Settings.Secure.SHOW_IME_WITH_HARD_KEYBOARD, 0) != 0 ? 2 : 1;
            }
            switch (this.mShowImeWithHardKeyboard) {
                case 1:
                    return false;
                case 2:
                    return true;
                default:
                    Log.e(InputMethodService.TAG, "Unexpected mShowImeWithHardKeyboard=" + this.mShowImeWithHardKeyboard);
                    return false;
            }
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            if (Settings.Secure.getUriFor(Settings.Secure.SHOW_IME_WITH_HARD_KEYBOARD).equals(uri)) {
                this.mShowImeWithHardKeyboard = Settings.Secure.getInt(this.mService.getContentResolver(), Settings.Secure.SHOW_IME_WITH_HARD_KEYBOARD, 0) != 0 ? 2 : 1;
                this.mService.resetStateForNewConfiguration();
            }
        }

        public String toString() {
            return "SettingsObserver{mShowImeWithHardKeyboard=" + this.mShowImeWithHardKeyboard + "}";
        }
    }

    @Override
    public void setTheme(int i) {
        if (this.mWindow != null) {
            throw new IllegalStateException("Must be called before onCreate()");
        }
        this.mTheme = i;
    }

    @Deprecated
    public boolean enableHardwareAcceleration() {
        if (this.mWindow != null) {
            throw new IllegalStateException("Must be called before onCreate()");
        }
        return ActivityManager.isHighEndGfx();
    }

    @Override
    public void onCreate() {
        this.mTheme = Resources.selectSystemTheme(this.mTheme, getApplicationInfo().targetSdkVersion, 16973908, 16973951, 16974142, 16974142);
        super.setTheme(this.mTheme);
        super.onCreate();
        this.mImm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        this.mSettingsObserver = SettingsObserver.createAndRegister(this);
        this.mShouldClearInsetOfPreviousIme = this.mImm.getInputMethodWindowVisibleHeight() > 0;
        this.mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mWindow = new SoftInputWindow(this, "InputMethod", this.mTheme, null, null, this.mDispatcherState, WindowManager.LayoutParams.TYPE_INPUT_METHOD, 80, false);
        this.mWindow.getWindow().setFlags(Integer.MIN_VALUE, Integer.MIN_VALUE);
        initViews();
        this.mWindow.getWindow().setLayout(-1, -2);
    }

    public void onInitializeInterface() {
    }

    void initialize() {
        if (!this.mInitialized) {
            this.mInitialized = true;
            onInitializeInterface();
        }
    }

    void initViews() {
        this.mInitialized = false;
        this.mWindowCreated = false;
        this.mShowInputRequested = false;
        this.mShowInputFlags = 0;
        this.mThemeAttrs = obtainStyledAttributes(R.styleable.InputMethodService);
        this.mRootView = this.mInflater.inflate(com.android.internal.R.layout.input_method, (ViewGroup) null);
        this.mWindow.setContentView(this.mRootView);
        this.mRootView.getViewTreeObserver().removeOnComputeInternalInsetsListener(this.mInsetsComputer);
        this.mRootView.getViewTreeObserver().addOnComputeInternalInsetsListener(this.mInsetsComputer);
        if (Settings.Global.getInt(getContentResolver(), Settings.Global.FANCY_IME_ANIMATIONS, 0) != 0) {
            this.mWindow.getWindow().setWindowAnimations(com.android.internal.R.style.Animation_InputMethodFancy);
        }
        this.mFullscreenArea = (ViewGroup) this.mRootView.findViewById(com.android.internal.R.id.fullscreenArea);
        this.mExtractViewHidden = false;
        this.mExtractFrame = (FrameLayout) this.mRootView.findViewById(16908316);
        this.mExtractView = null;
        this.mExtractEditText = null;
        this.mExtractAccessories = null;
        this.mExtractAction = null;
        this.mFullscreenApplied = false;
        this.mCandidatesFrame = (FrameLayout) this.mRootView.findViewById(16908317);
        this.mInputFrame = (FrameLayout) this.mRootView.findViewById(16908318);
        this.mInputView = null;
        this.mIsInputViewShown = false;
        this.mExtractFrame.setVisibility(8);
        this.mCandidatesVisibility = getCandidatesHiddenVisibility();
        this.mCandidatesFrame.setVisibility(this.mCandidatesVisibility);
        this.mInputFrame.setVisibility(8);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mRootView.getViewTreeObserver().removeOnComputeInternalInsetsListener(this.mInsetsComputer);
        doFinishInput();
        if (this.mWindowAdded) {
            this.mWindow.getWindow().setWindowAnimations(0);
            this.mWindow.dismiss();
        }
        if (this.mSettingsObserver != null) {
            this.mSettingsObserver.unregister();
            this.mSettingsObserver = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        resetStateForNewConfiguration();
    }

    private void resetStateForNewConfiguration() {
        boolean z = this.mWindowVisible;
        int i = this.mShowInputFlags;
        boolean z2 = this.mShowInputRequested;
        CompletionInfo[] completionInfoArr = this.mCurCompletions;
        initViews();
        this.mInputViewStarted = false;
        this.mCandidatesViewStarted = false;
        if (this.mInputStarted) {
            doStartInput(getCurrentInputConnection(), getCurrentInputEditorInfo(), true);
        }
        if (z) {
            if (z2) {
                if (dispatchOnShowInputRequested(i, true)) {
                    showWindow(true);
                    if (completionInfoArr != null) {
                        this.mCurCompletions = completionInfoArr;
                        onDisplayCompletions(completionInfoArr);
                    }
                } else {
                    doHideWindow();
                }
            } else if (this.mCandidatesVisibility == 0) {
                showWindow(false);
            } else {
                doHideWindow();
            }
            this.mImm.setImeWindowStatus(this.mToken, this.mStartInputToken, 1 | (onEvaluateInputViewShown() ? 2 : 0), this.mBackDisposition);
        }
    }

    @Override
    public AbstractInputMethodService.AbstractInputMethodImpl onCreateInputMethodInterface() {
        return new InputMethodImpl();
    }

    @Override
    public AbstractInputMethodService.AbstractInputMethodSessionImpl onCreateInputMethodSessionInterface() {
        return new InputMethodSessionImpl();
    }

    public LayoutInflater getLayoutInflater() {
        return this.mInflater;
    }

    public Dialog getWindow() {
        return this.mWindow;
    }

    public void setBackDisposition(int i) {
        if (i == this.mBackDisposition) {
            return;
        }
        if (i > 3 || i < 0) {
            Log.e(TAG, "Invalid back disposition value (" + i + ") specified.");
            return;
        }
        this.mBackDisposition = i;
        this.mImm.setImeWindowStatus(this.mToken, this.mStartInputToken, mapToImeWindowStatus(isInputViewShown()), this.mBackDisposition);
    }

    public int getBackDisposition() {
        return this.mBackDisposition;
    }

    public int getMaxWidth() {
        return ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
    }

    public InputBinding getCurrentInputBinding() {
        return this.mInputBinding;
    }

    public InputConnection getCurrentInputConnection() {
        InputConnection inputConnection = this.mStartedInputConnection;
        if (inputConnection != null) {
            return inputConnection;
        }
        return this.mInputConnection;
    }

    public final boolean switchToPreviousInputMethod() {
        return this.mImm.switchToPreviousInputMethodInternal(this.mToken);
    }

    public final boolean switchToNextInputMethod(boolean z) {
        return this.mImm.switchToNextInputMethodInternal(this.mToken, z);
    }

    public final boolean shouldOfferSwitchingToNextInputMethod() {
        return this.mImm.shouldOfferSwitchingToNextInputMethodInternal(this.mToken);
    }

    public boolean getCurrentInputStarted() {
        return this.mInputStarted;
    }

    public EditorInfo getCurrentInputEditorInfo() {
        return this.mInputEditorInfo;
    }

    public void updateFullscreenMode() {
        View viewOnCreateExtractTextView;
        boolean z = this.mShowInputRequested && onEvaluateFullscreenMode();
        boolean z2 = this.mLastShowInputRequested != this.mShowInputRequested;
        if (this.mIsFullscreen != z || !this.mFullscreenApplied) {
            this.mIsFullscreen = z;
            if (this.mImm != null && this.mToken != null) {
                this.mImm.reportFullscreenMode(this.mToken, this.mIsFullscreen);
            }
            this.mFullscreenApplied = true;
            initialize();
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mFullscreenArea.getLayoutParams();
            if (z) {
                this.mFullscreenArea.setBackgroundDrawable(this.mThemeAttrs.getDrawable(0));
                layoutParams.height = 0;
                layoutParams.weight = 1.0f;
            } else {
                this.mFullscreenArea.setBackgroundDrawable(null);
                layoutParams.height = -2;
                layoutParams.weight = 0.0f;
            }
            ((ViewGroup) this.mFullscreenArea.getParent()).updateViewLayout(this.mFullscreenArea, layoutParams);
            if (z) {
                if (this.mExtractView == null && (viewOnCreateExtractTextView = onCreateExtractTextView()) != null) {
                    setExtractView(viewOnCreateExtractTextView);
                }
                startExtractingText(false);
            }
            updateExtractFrameVisibility();
            z2 = true;
        }
        if (z2) {
            onConfigureWindow(this.mWindow.getWindow(), z, true ^ this.mShowInputRequested);
            this.mLastShowInputRequested = this.mShowInputRequested;
        }
    }

    public void onConfigureWindow(Window window, boolean z, boolean z2) {
        int i;
        int i2 = this.mWindow.getWindow().getAttributes().height;
        if (!z) {
            i = -2;
        } else {
            i = -1;
        }
        boolean z3 = this.mIsInputViewShown;
        this.mWindow.getWindow().setLayout(-1, i);
    }

    public boolean isFullscreenMode() {
        return this.mIsFullscreen;
    }

    public boolean onEvaluateFullscreenMode() {
        if (getResources().getConfiguration().orientation != 2) {
            return false;
        }
        return this.mInputEditorInfo == null || (this.mInputEditorInfo.imeOptions & 33554432) == 0;
    }

    public void setExtractViewShown(boolean z) {
        if (this.mExtractViewHidden == z) {
            this.mExtractViewHidden = !z;
            updateExtractFrameVisibility();
        }
    }

    public boolean isExtractViewShown() {
        return this.mIsFullscreen && !this.mExtractViewHidden;
    }

    void updateExtractFrameVisibility() {
        int i;
        if (isFullscreenMode()) {
            i = this.mExtractViewHidden ? 4 : 0;
            this.mExtractFrame.setVisibility(i);
        } else {
            this.mExtractFrame.setVisibility(8);
            i = 0;
        }
        updateCandidatesVisibility(this.mCandidatesVisibility == 0);
        if (this.mWindowWasVisible && this.mFullscreenArea.getVisibility() != i) {
            int resourceId = this.mThemeAttrs.getResourceId(i != 0 ? 2 : 1, 0);
            if (resourceId != 0) {
                this.mFullscreenArea.startAnimation(AnimationUtils.loadAnimation(this, resourceId));
            }
        }
        this.mFullscreenArea.setVisibility(i);
    }

    public void onComputeInsets(Insets insets) {
        int[] iArr = this.mTmpLocation;
        if (this.mInputFrame.getVisibility() == 0) {
            this.mInputFrame.getLocationInWindow(iArr);
        } else {
            iArr[1] = getWindow().getWindow().getDecorView().getHeight();
        }
        if (isFullscreenMode()) {
            insets.contentTopInsets = getWindow().getWindow().getDecorView().getHeight();
        } else {
            insets.contentTopInsets = iArr[1];
        }
        if (this.mCandidatesFrame.getVisibility() == 0) {
            this.mCandidatesFrame.getLocationInWindow(iArr);
        }
        insets.visibleTopInsets = iArr[1];
        insets.touchableInsets = 2;
        insets.touchableRegion.setEmpty();
    }

    public void updateInputViewShown() {
        boolean z = this.mShowInputRequested && onEvaluateInputViewShown();
        if (this.mIsInputViewShown != z && this.mWindowVisible) {
            this.mIsInputViewShown = z;
            this.mInputFrame.setVisibility(z ? 0 : 8);
            if (this.mInputView == null) {
                initialize();
                View viewOnCreateInputView = onCreateInputView();
                if (viewOnCreateInputView != null) {
                    setInputView(viewOnCreateInputView);
                }
            }
        }
    }

    public boolean isShowInputRequested() {
        return this.mShowInputRequested;
    }

    public boolean isInputViewShown() {
        return this.mIsInputViewShown && this.mWindowVisible;
    }

    public boolean onEvaluateInputViewShown() {
        if (this.mSettingsObserver != null) {
            if (this.mSettingsObserver.shouldShowImeWithHardKeyboard()) {
                return true;
            }
            Configuration configuration = getResources().getConfiguration();
            return configuration.keyboard == 1 || configuration.hardKeyboardHidden == 2;
        }
        Log.w(TAG, "onEvaluateInputViewShown: mSettingsObserver must not be null here.");
        return false;
    }

    public void setCandidatesViewShown(boolean z) {
        updateCandidatesVisibility(z);
        if (!this.mShowInputRequested && this.mWindowVisible != z) {
            if (z) {
                showWindow(false);
            } else {
                doHideWindow();
            }
        }
    }

    void updateCandidatesVisibility(boolean z) {
        int candidatesHiddenVisibility = z ? 0 : getCandidatesHiddenVisibility();
        if (this.mCandidatesVisibility != candidatesHiddenVisibility) {
            this.mCandidatesFrame.setVisibility(candidatesHiddenVisibility);
            this.mCandidatesVisibility = candidatesHiddenVisibility;
        }
    }

    public int getCandidatesHiddenVisibility() {
        return isExtractViewShown() ? 8 : 4;
    }

    public void showStatusIcon(int i) {
        this.mStatusIcon = i;
        this.mImm.showStatusIconInternal(this.mToken, getPackageName(), i);
    }

    public void hideStatusIcon() {
        this.mStatusIcon = 0;
        this.mImm.hideStatusIconInternal(this.mToken);
    }

    public void switchInputMethod(String str) {
        this.mImm.setInputMethodInternal(this.mToken, str);
    }

    public final void switchInputMethod(String str, InputMethodSubtype inputMethodSubtype) {
        this.mImm.setInputMethodAndSubtypeInternal(this.mToken, str, inputMethodSubtype);
    }

    public void setExtractView(View view) {
        this.mExtractFrame.removeAllViews();
        this.mExtractFrame.addView(view, new FrameLayout.LayoutParams(-1, -1));
        this.mExtractView = view;
        if (view != null) {
            this.mExtractEditText = (ExtractEditText) view.findViewById(16908325);
            this.mExtractEditText.setIME(this);
            this.mExtractAction = view.findViewById(com.android.internal.R.id.inputExtractAction);
            if (this.mExtractAction != null) {
                this.mExtractAccessories = (ViewGroup) view.findViewById(com.android.internal.R.id.inputExtractAccessories);
            }
            startExtractingText(false);
            return;
        }
        this.mExtractEditText = null;
        this.mExtractAccessories = null;
        this.mExtractAction = null;
    }

    public void setCandidatesView(View view) {
        this.mCandidatesFrame.removeAllViews();
        this.mCandidatesFrame.addView(view, new FrameLayout.LayoutParams(-1, -2));
    }

    public void setInputView(View view) {
        this.mInputFrame.removeAllViews();
        this.mInputFrame.addView(view, new FrameLayout.LayoutParams(-1, -2));
        this.mInputView = view;
    }

    public View onCreateExtractTextView() {
        return this.mInflater.inflate(com.android.internal.R.layout.input_method_extract_view, (ViewGroup) null);
    }

    public View onCreateCandidatesView() {
        return null;
    }

    public View onCreateInputView() {
        return null;
    }

    public void onStartInputView(EditorInfo editorInfo, boolean z) {
    }

    public void onFinishInputView(boolean z) {
        InputConnection currentInputConnection;
        if (!z && (currentInputConnection = getCurrentInputConnection()) != null) {
            currentInputConnection.finishComposingText();
        }
    }

    public void onStartCandidatesView(EditorInfo editorInfo, boolean z) {
    }

    public void onFinishCandidatesView(boolean z) {
        InputConnection currentInputConnection;
        if (!z && (currentInputConnection = getCurrentInputConnection()) != null) {
            currentInputConnection.finishComposingText();
        }
    }

    public boolean onShowInputRequested(int i, boolean z) {
        if (!onEvaluateInputViewShown()) {
            return false;
        }
        if ((i & 1) == 0) {
            if (!z && onEvaluateFullscreenMode()) {
                return false;
            }
            if (!this.mSettingsObserver.shouldShowImeWithHardKeyboard() && getResources().getConfiguration().keyboard != 1) {
                return false;
            }
        }
        return true;
    }

    private boolean dispatchOnShowInputRequested(int i, boolean z) {
        boolean zOnShowInputRequested = onShowInputRequested(i, z);
        if (zOnShowInputRequested) {
            this.mShowInputFlags = i;
        } else {
            this.mShowInputFlags = 0;
        }
        return zOnShowInputRequested;
    }

    public void showWindow(boolean z) {
        if (this.mInShowWindow) {
            Log.w(TAG, "Re-entrance in to showWindow");
            return;
        }
        try {
            try {
                this.mWindowWasVisible = this.mWindowVisible;
                this.mInShowWindow = true;
                showWindowInner(z);
            } catch (WindowManager.BadTokenException e) {
                this.mWindowVisible = false;
                this.mWindowAdded = false;
                throw e;
            }
        } finally {
            this.mWindowWasVisible = true;
            this.mInShowWindow = false;
        }
    }

    void showWindowInner(boolean z) {
        Object[] objArr;
        int i = (this.mWindowVisible ? 1 : 0) | (isInputViewShown() ? 2 : 0);
        this.mWindowVisible = true;
        if (!this.mShowInputRequested && this.mInputStarted && z) {
            this.mShowInputRequested = true;
            objArr = true;
        } else {
            objArr = false;
        }
        initialize();
        updateFullscreenMode();
        updateInputViewShown();
        if (!this.mWindowAdded || !this.mWindowCreated) {
            this.mWindowAdded = true;
            this.mWindowCreated = true;
            initialize();
            View viewOnCreateCandidatesView = onCreateCandidatesView();
            if (viewOnCreateCandidatesView != null) {
                setCandidatesView(viewOnCreateCandidatesView);
            }
        }
        if (this.mShowInputRequested) {
            if (!this.mInputViewStarted) {
                this.mInputViewStarted = true;
                onStartInputView(this.mInputEditorInfo, false);
            }
        } else if (!this.mCandidatesViewStarted) {
            this.mCandidatesViewStarted = true;
            onStartCandidatesView(this.mInputEditorInfo, false);
        }
        if (objArr != false) {
            startExtractingText(false);
        }
        int iMapToImeWindowStatus = mapToImeWindowStatus(isInputViewShown());
        if (i != iMapToImeWindowStatus) {
            this.mImm.setImeWindowStatus(this.mToken, this.mStartInputToken, iMapToImeWindowStatus, this.mBackDisposition);
        }
        if ((i & 1) == 0) {
            onWindowShown();
            this.mWindow.show();
            this.mShouldClearInsetOfPreviousIme = false;
        }
    }

    private void finishViews() {
        if (this.mInputViewStarted) {
            onFinishInputView(false);
        } else if (this.mCandidatesViewStarted) {
            onFinishCandidatesView(false);
        }
        this.mInputViewStarted = false;
        this.mCandidatesViewStarted = false;
    }

    private void doHideWindow() {
        this.mImm.setImeWindowStatus(this.mToken, this.mStartInputToken, 0, this.mBackDisposition);
        hideWindow();
    }

    public void hideWindow() {
        finishViews();
        if (this.mWindowVisible) {
            this.mWindow.hide();
            this.mWindowVisible = false;
            onWindowHidden();
            this.mWindowWasVisible = false;
        }
        updateFullscreenMode();
    }

    public void onWindowShown() {
    }

    public void onWindowHidden() {
    }

    private void clearInsetOfPreviousIme() {
        if (this.mShouldClearInsetOfPreviousIme) {
            this.mImm.clearLastInputMethodWindowForTransition(this.mToken);
            this.mShouldClearInsetOfPreviousIme = false;
        }
    }

    public void onBindInput() {
    }

    public void onUnbindInput() {
    }

    public void onStartInput(EditorInfo editorInfo, boolean z) {
    }

    void doFinishInput() {
        if (this.mInputViewStarted) {
            onFinishInputView(true);
        } else if (this.mCandidatesViewStarted) {
            onFinishCandidatesView(true);
        }
        this.mInputViewStarted = false;
        this.mCandidatesViewStarted = false;
        if (this.mInputStarted) {
            onFinishInput();
        }
        this.mInputStarted = false;
        this.mStartedInputConnection = null;
        this.mCurCompletions = null;
    }

    void doStartInput(InputConnection inputConnection, EditorInfo editorInfo, boolean z) {
        if (!z) {
            doFinishInput();
        }
        this.mInputStarted = true;
        this.mStartedInputConnection = inputConnection;
        this.mInputEditorInfo = editorInfo;
        initialize();
        onStartInput(editorInfo, z);
        if (this.mWindowVisible) {
            if (this.mShowInputRequested) {
                this.mInputViewStarted = true;
                onStartInputView(this.mInputEditorInfo, z);
                startExtractingText(true);
            } else if (this.mCandidatesVisibility == 0) {
                this.mCandidatesViewStarted = true;
                onStartCandidatesView(this.mInputEditorInfo, z);
            }
        }
    }

    public void onFinishInput() {
        InputConnection currentInputConnection = getCurrentInputConnection();
        if (currentInputConnection != null) {
            currentInputConnection.finishComposingText();
        }
    }

    public void onDisplayCompletions(CompletionInfo[] completionInfoArr) {
    }

    public void onUpdateExtractedText(int i, ExtractedText extractedText) {
        if (this.mExtractedToken == i && extractedText != null && this.mExtractEditText != null) {
            this.mExtractedText = extractedText;
            this.mExtractEditText.setExtractedText(extractedText);
        }
    }

    public void onUpdateSelection(int i, int i2, int i3, int i4, int i5, int i6) {
        ExtractEditText extractEditText = this.mExtractEditText;
        if (extractEditText != null && isFullscreenMode() && this.mExtractedText != null) {
            int i7 = this.mExtractedText.startOffset;
            extractEditText.startInternalChanges();
            int i8 = i3 - i7;
            int i9 = i4 - i7;
            int length = extractEditText.getText().length();
            if (i8 >= 0) {
                if (i8 > length) {
                    i8 = length;
                }
            } else {
                i8 = 0;
            }
            if (i9 >= 0) {
                if (i9 <= length) {
                    length = i9;
                }
            } else {
                length = 0;
            }
            extractEditText.setSelection(i8, length);
            extractEditText.finishInternalChanges();
        }
    }

    public void onViewClicked(boolean z) {
    }

    @Deprecated
    public void onUpdateCursor(Rect rect) {
    }

    public void onUpdateCursorAnchorInfo(CursorAnchorInfo cursorAnchorInfo) {
    }

    public void requestHideSelf(int i) {
        this.mImm.hideSoftInputFromInputMethodInternal(this.mToken, i);
    }

    public final void requestShowSelf(int i) {
        this.mImm.showSoftInputFromInputMethodInternal(this.mToken, i);
    }

    private boolean handleBack(boolean z) {
        if (this.mShowInputRequested) {
            if (z) {
                requestHideSelf(0);
            }
            return true;
        }
        if (!this.mWindowVisible) {
            return false;
        }
        if (this.mCandidatesVisibility == 0) {
            if (z) {
                setCandidatesViewShown(false);
            }
        } else if (z) {
            doHideWindow();
        }
        return true;
    }

    private ExtractEditText getExtractEditTextIfVisible() {
        if (!isExtractViewShown() || !isInputViewShown()) {
            return null;
        }
        return this.mExtractEditText;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == 4) {
            ExtractEditText extractEditTextIfVisible = getExtractEditTextIfVisible();
            if (extractEditTextIfVisible != null && extractEditTextIfVisible.handleBackInTextActionModeIfNeeded(keyEvent)) {
                return true;
            }
            if (!handleBack(false)) {
                return false;
            }
            keyEvent.startTracking();
            return true;
        }
        return doMovementKey(i, keyEvent, -1);
    }

    @Override
    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean onKeyMultiple(int i, int i2, KeyEvent keyEvent) {
        return doMovementKey(i, keyEvent, i2);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == 4) {
            ExtractEditText extractEditTextIfVisible = getExtractEditTextIfVisible();
            if (extractEditTextIfVisible != null && extractEditTextIfVisible.handleBackInTextActionModeIfNeeded(keyEvent)) {
                return true;
            }
            if (keyEvent.isTracking() && !keyEvent.isCanceled()) {
                return handleBack(true);
            }
        }
        return doMovementKey(i, keyEvent, -2);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        return false;
    }

    public void onAppPrivateCommand(String str, Bundle bundle) {
    }

    private void onToggleSoftInput(int i, int i2) {
        if (isInputViewShown()) {
            requestHideSelf(i2);
        } else {
            requestShowSelf(i);
        }
    }

    void reportExtractedMovement(int i, int i2) {
        int i3 = 0;
        switch (i) {
            case 19:
                i2 = -i2;
                break;
            case 20:
                break;
            case 21:
                i2 = -i2;
            case 22:
                i3 = i2;
                i2 = 0;
                break;
            default:
                i2 = 0;
                break;
        }
        onExtractedCursorMovement(i3, i2);
    }

    boolean doMovementKey(int i, KeyEvent keyEvent, int i2) {
        ExtractEditText extractEditTextIfVisible = getExtractEditTextIfVisible();
        if (extractEditTextIfVisible != null) {
            MovementMethod movementMethod = extractEditTextIfVisible.getMovementMethod();
            Layout layout = extractEditTextIfVisible.getLayout();
            if (movementMethod != null && layout != null) {
                if (i2 == -1) {
                    if (movementMethod.onKeyDown(extractEditTextIfVisible, extractEditTextIfVisible.getText(), i, keyEvent)) {
                        reportExtractedMovement(i, 1);
                        return true;
                    }
                } else if (i2 == -2) {
                    if (movementMethod.onKeyUp(extractEditTextIfVisible, extractEditTextIfVisible.getText(), i, keyEvent)) {
                        return true;
                    }
                } else if (movementMethod.onKeyOther(extractEditTextIfVisible, extractEditTextIfVisible.getText(), keyEvent)) {
                    reportExtractedMovement(i, i2);
                } else {
                    KeyEvent keyEventChangeAction = KeyEvent.changeAction(keyEvent, 0);
                    if (movementMethod.onKeyDown(extractEditTextIfVisible, extractEditTextIfVisible.getText(), i, keyEventChangeAction)) {
                        KeyEvent keyEventChangeAction2 = KeyEvent.changeAction(keyEvent, 1);
                        movementMethod.onKeyUp(extractEditTextIfVisible, extractEditTextIfVisible.getText(), i, keyEventChangeAction2);
                        while (true) {
                            i2--;
                            if (i2 <= 0) {
                                break;
                            }
                            movementMethod.onKeyDown(extractEditTextIfVisible, extractEditTextIfVisible.getText(), i, keyEventChangeAction);
                            movementMethod.onKeyUp(extractEditTextIfVisible, extractEditTextIfVisible.getText(), i, keyEventChangeAction2);
                        }
                        reportExtractedMovement(i, i2);
                    }
                }
            }
            switch (i) {
            }
            return true;
        }
        return false;
    }

    public void sendDownUpKeyEvents(int i) {
        InputConnection currentInputConnection = getCurrentInputConnection();
        if (currentInputConnection == null) {
            return;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        currentInputConnection.sendKeyEvent(new KeyEvent(jUptimeMillis, jUptimeMillis, 0, i, 0, 0, -1, 0, 6));
        currentInputConnection.sendKeyEvent(new KeyEvent(jUptimeMillis, SystemClock.uptimeMillis(), 1, i, 0, 0, -1, 0, 6));
    }

    public boolean sendDefaultEditorAction(boolean z) {
        EditorInfo currentInputEditorInfo = getCurrentInputEditorInfo();
        if (currentInputEditorInfo == null) {
            return false;
        }
        if ((!z || (currentInputEditorInfo.imeOptions & 1073741824) == 0) && (currentInputEditorInfo.imeOptions & 255) != 1) {
            InputConnection currentInputConnection = getCurrentInputConnection();
            if (currentInputConnection != null) {
                currentInputConnection.performEditorAction(currentInputEditorInfo.imeOptions & 255);
            }
            return true;
        }
        return false;
    }

    public void sendKeyChar(char c) {
        if (c == '\n') {
            if (!sendDefaultEditorAction(true)) {
                sendDownUpKeyEvents(66);
            }
        } else {
            if (c >= '0' && c <= '9') {
                sendDownUpKeyEvents((c - '0') + 7);
                return;
            }
            InputConnection currentInputConnection = getCurrentInputConnection();
            if (currentInputConnection != null) {
                currentInputConnection.commitText(String.valueOf(c), 1);
            }
        }
    }

    public void onExtractedSelectionChanged(int i, int i2) {
        InputConnection currentInputConnection = getCurrentInputConnection();
        if (currentInputConnection != null) {
            currentInputConnection.setSelection(i, i2);
        }
    }

    public void onExtractedDeleteText(int i, int i2) {
        InputConnection currentInputConnection = getCurrentInputConnection();
        if (currentInputConnection != null) {
            currentInputConnection.finishComposingText();
            currentInputConnection.setSelection(i, i);
            currentInputConnection.deleteSurroundingText(0, i2 - i);
        }
    }

    public void onExtractedReplaceText(int i, int i2, CharSequence charSequence) {
        InputConnection currentInputConnection = getCurrentInputConnection();
        if (currentInputConnection != null) {
            currentInputConnection.setComposingRegion(i, i2);
            currentInputConnection.commitText(charSequence, 1);
        }
    }

    public void onExtractedSetSpan(Object obj, int i, int i2, int i3) {
        InputConnection currentInputConnection = getCurrentInputConnection();
        if (currentInputConnection == null || !currentInputConnection.setSelection(i, i2)) {
            return;
        }
        CharSequence selectedText = currentInputConnection.getSelectedText(1);
        if (selectedText instanceof Spannable) {
            ((Spannable) selectedText).setSpan(obj, 0, selectedText.length(), i3);
            currentInputConnection.setComposingRegion(i, i2);
            currentInputConnection.commitText(selectedText, 1);
        }
    }

    public void onExtractedTextClicked() {
        if (this.mExtractEditText != null && this.mExtractEditText.hasVerticalScrollBar()) {
            setCandidatesViewShown(false);
        }
    }

    public void onExtractedCursorMovement(int i, int i2) {
        if (this.mExtractEditText != null && i2 != 0 && this.mExtractEditText.hasVerticalScrollBar()) {
            setCandidatesViewShown(false);
        }
    }

    public boolean onExtractTextContextMenuItem(int i) {
        InputConnection currentInputConnection = getCurrentInputConnection();
        if (currentInputConnection != null) {
            currentInputConnection.performContextMenuAction(i);
            return true;
        }
        return true;
    }

    public CharSequence getTextForImeAction(int i) {
        switch (i & 255) {
            case 1:
                return null;
            case 2:
                return getText(com.android.internal.R.string.ime_action_go);
            case 3:
                return getText(com.android.internal.R.string.ime_action_search);
            case 4:
                return getText(com.android.internal.R.string.ime_action_send);
            case 5:
                return getText(com.android.internal.R.string.ime_action_next);
            case 6:
                return getText(com.android.internal.R.string.ime_action_done);
            case 7:
                return getText(com.android.internal.R.string.ime_action_previous);
            default:
                return getText(com.android.internal.R.string.ime_action_default);
        }
    }

    private int getIconForImeAction(int i) {
        switch (i & 255) {
            case 2:
                return com.android.internal.R.drawable.ic_input_extract_action_go;
            case 3:
                return com.android.internal.R.drawable.ic_input_extract_action_search;
            case 4:
                return com.android.internal.R.drawable.ic_input_extract_action_send;
            case 5:
                return com.android.internal.R.drawable.ic_input_extract_action_next;
            case 6:
                return com.android.internal.R.drawable.ic_input_extract_action_done;
            case 7:
                return com.android.internal.R.drawable.ic_input_extract_action_previous;
            default:
                return com.android.internal.R.drawable.ic_input_extract_action_return;
        }
    }

    public void onUpdateExtractingVisibility(EditorInfo editorInfo) {
        if (editorInfo.inputType == 0 || (editorInfo.imeOptions & 268435456) != 0) {
            setExtractViewShown(false);
        } else {
            setExtractViewShown(true);
        }
    }

    public void onUpdateExtractingViews(EditorInfo editorInfo) {
        if (!isExtractViewShown() || this.mExtractAccessories == null) {
            return;
        }
        boolean z = true;
        if (editorInfo.actionLabel == null && ((editorInfo.imeOptions & 255) == 1 || (editorInfo.imeOptions & 536870912) != 0 || editorInfo.inputType == 0)) {
            z = false;
        }
        if (z) {
            this.mExtractAccessories.setVisibility(0);
            if (this.mExtractAction != null) {
                if (this.mExtractAction instanceof ImageButton) {
                    ((ImageButton) this.mExtractAction).setImageResource(getIconForImeAction(editorInfo.imeOptions));
                    if (editorInfo.actionLabel != null) {
                        this.mExtractAction.setContentDescription(editorInfo.actionLabel);
                    } else {
                        this.mExtractAction.setContentDescription(getTextForImeAction(editorInfo.imeOptions));
                    }
                } else if (editorInfo.actionLabel != null) {
                    ((TextView) this.mExtractAction).setText(editorInfo.actionLabel);
                } else {
                    ((TextView) this.mExtractAction).setText(getTextForImeAction(editorInfo.imeOptions));
                }
                this.mExtractAction.setOnClickListener(this.mActionClickListener);
                return;
            }
            return;
        }
        this.mExtractAccessories.setVisibility(8);
        if (this.mExtractAction != null) {
            this.mExtractAction.setOnClickListener(null);
        }
    }

    public void onExtractingInputChanged(EditorInfo editorInfo) {
        if (editorInfo.inputType == 0) {
            requestHideSelf(2);
        }
    }

    void startExtractingText(boolean z) {
        ExtractEditText extractEditText = this.mExtractEditText;
        if (extractEditText != null && getCurrentInputStarted() && isFullscreenMode()) {
            this.mExtractedToken++;
            ExtractedTextRequest extractedTextRequest = new ExtractedTextRequest();
            extractedTextRequest.token = this.mExtractedToken;
            extractedTextRequest.flags = 1;
            extractedTextRequest.hintMaxLines = 10;
            extractedTextRequest.hintMaxChars = 10000;
            InputConnection currentInputConnection = getCurrentInputConnection();
            this.mExtractedText = currentInputConnection == null ? null : currentInputConnection.getExtractedText(extractedTextRequest, 1);
            if (this.mExtractedText == null || currentInputConnection == null) {
                Log.e(TAG, "Unexpected null in startExtractingText : mExtractedText = " + this.mExtractedText + ", input connection = " + currentInputConnection);
            }
            EditorInfo currentInputEditorInfo = getCurrentInputEditorInfo();
            try {
                extractEditText.startInternalChanges();
                onUpdateExtractingVisibility(currentInputEditorInfo);
                onUpdateExtractingViews(currentInputEditorInfo);
                int i = currentInputEditorInfo.inputType;
                if ((i & 15) == 1 && (262144 & i) != 0) {
                    i |= 131072;
                }
                extractEditText.setInputType(i);
                extractEditText.setHint(currentInputEditorInfo.hintText);
                if (this.mExtractedText != null) {
                    extractEditText.setEnabled(true);
                    extractEditText.setExtractedText(this.mExtractedText);
                } else {
                    extractEditText.setEnabled(false);
                    extractEditText.setText("");
                }
                if (z) {
                    onExtractingInputChanged(currentInputEditorInfo);
                }
            } finally {
                extractEditText.finishInternalChanges();
            }
        }
    }

    protected void onCurrentInputMethodSubtypeChanged(InputMethodSubtype inputMethodSubtype) {
    }

    public int getInputMethodWindowRecommendedHeight() {
        return this.mImm.getInputMethodWindowVisibleHeight();
    }

    @Override
    public final void exposeContent(InputContentInfo inputContentInfo, InputConnection inputConnection) {
        if (inputConnection == null || getCurrentInputConnection() != inputConnection) {
            return;
        }
        this.mImm.exposeContent(this.mToken, inputContentInfo, getCurrentInputEditorInfo());
    }

    private static int mapToImeWindowStatus(boolean z) {
        return (z ? 2 : 0) | 1;
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        PrintWriterPrinter printWriterPrinter = new PrintWriterPrinter(printWriter);
        printWriterPrinter.println("Input method service state for " + this + SettingsStringUtil.DELIMITER);
        printWriterPrinter.println("  mWindowCreated=" + this.mWindowCreated + " mWindowAdded=" + this.mWindowAdded);
        printWriterPrinter.println("  mWindowVisible=" + this.mWindowVisible + " mWindowWasVisible=" + this.mWindowWasVisible + " mInShowWindow=" + this.mInShowWindow);
        StringBuilder sb = new StringBuilder();
        sb.append("  Configuration=");
        sb.append(getResources().getConfiguration());
        printWriterPrinter.println(sb.toString());
        printWriterPrinter.println("  mToken=" + this.mToken);
        printWriterPrinter.println("  mInputBinding=" + this.mInputBinding);
        printWriterPrinter.println("  mInputConnection=" + this.mInputConnection);
        printWriterPrinter.println("  mStartedInputConnection=" + this.mStartedInputConnection);
        printWriterPrinter.println("  mInputStarted=" + this.mInputStarted + " mInputViewStarted=" + this.mInputViewStarted + " mCandidatesViewStarted=" + this.mCandidatesViewStarted);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("  mStartInputToken=");
        sb2.append(this.mStartInputToken);
        printWriterPrinter.println(sb2.toString());
        if (this.mInputEditorInfo != null) {
            printWriterPrinter.println("  mInputEditorInfo:");
            this.mInputEditorInfo.dump(printWriterPrinter, "    ");
        } else {
            printWriterPrinter.println("  mInputEditorInfo: null");
        }
        printWriterPrinter.println("  mShowInputRequested=" + this.mShowInputRequested + " mLastShowInputRequested=" + this.mLastShowInputRequested + " mShowInputFlags=0x" + Integer.toHexString(this.mShowInputFlags));
        printWriterPrinter.println("  mCandidatesVisibility=" + this.mCandidatesVisibility + " mFullscreenApplied=" + this.mFullscreenApplied + " mIsFullscreen=" + this.mIsFullscreen + " mExtractViewHidden=" + this.mExtractViewHidden);
        if (this.mExtractedText != null) {
            printWriterPrinter.println("  mExtractedText:");
            printWriterPrinter.println("    text=" + this.mExtractedText.text.length() + " chars startOffset=" + this.mExtractedText.startOffset);
            printWriterPrinter.println("    selectionStart=" + this.mExtractedText.selectionStart + " selectionEnd=" + this.mExtractedText.selectionEnd + " flags=0x" + Integer.toHexString(this.mExtractedText.flags));
        } else {
            printWriterPrinter.println("  mExtractedText: null");
        }
        printWriterPrinter.println("  mExtractedToken=" + this.mExtractedToken);
        printWriterPrinter.println("  mIsInputViewShown=" + this.mIsInputViewShown + " mStatusIcon=" + this.mStatusIcon);
        printWriterPrinter.println("Last computed insets:");
        printWriterPrinter.println("  contentTopInsets=" + this.mTmpInsets.contentTopInsets + " visibleTopInsets=" + this.mTmpInsets.visibleTopInsets + " touchableInsets=" + this.mTmpInsets.touchableInsets + " touchableRegion=" + this.mTmpInsets.touchableRegion);
        StringBuilder sb3 = new StringBuilder();
        sb3.append(" mShouldClearInsetOfPreviousIme=");
        sb3.append(this.mShouldClearInsetOfPreviousIme);
        printWriterPrinter.println(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append(" mSettingsObserver=");
        sb4.append(this.mSettingsObserver);
        printWriterPrinter.println(sb4.toString());
    }
}
