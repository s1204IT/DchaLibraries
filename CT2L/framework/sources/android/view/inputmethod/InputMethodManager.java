package android.view.inputmethod;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.Trace;
import android.text.style.SuggestionSpan;
import android.util.Log;
import android.util.Pools;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.SparseArray;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventSender;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewRootImpl;
import com.android.internal.os.SomeArgs;
import com.android.internal.view.IInputConnectionWrapper;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodManager;
import com.android.internal.view.IInputMethodSession;
import com.android.internal.view.InputBindResult;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class InputMethodManager {
    public static final int CONTROL_START_INITIAL = 256;
    public static final int CONTROL_WINDOW_FIRST = 4;
    public static final int CONTROL_WINDOW_IS_TEXT_EDITOR = 2;
    public static final int CONTROL_WINDOW_VIEW_HAS_FOCUS = 1;
    static final boolean DEBUG = false;
    public static final int DISPATCH_HANDLED = 1;
    public static final int DISPATCH_IN_PROGRESS = -1;
    public static final int DISPATCH_NOT_HANDLED = 0;
    public static final int HIDE_IMPLICIT_ONLY = 1;
    public static final int HIDE_NOT_ALWAYS = 2;
    static final long INPUT_METHOD_NOT_RESPONDING_TIMEOUT = 2500;
    static final int MSG_BIND = 2;
    static final int MSG_DUMP = 1;
    static final int MSG_FLUSH_INPUT_EVENT = 7;
    static final int MSG_SEND_INPUT_EVENT = 5;
    static final int MSG_SET_ACTIVE = 4;
    static final int MSG_SET_USER_ACTION_NOTIFICATION_SEQUENCE_NUMBER = 9;
    static final int MSG_TIMEOUT_INPUT_EVENT = 6;
    static final int MSG_UNBIND = 3;
    private static final int NOT_AN_ACTION_NOTIFICATION_SEQUENCE_NUMBER = -1;
    static final String PENDING_EVENT_COUNTER = "aq:imm";
    private static final int REQUEST_UPDATE_CURSOR_ANCHOR_INFO_NONE = 0;
    public static final int RESULT_HIDDEN = 3;
    public static final int RESULT_SHOWN = 2;
    public static final int RESULT_UNCHANGED_HIDDEN = 1;
    public static final int RESULT_UNCHANGED_SHOWN = 0;
    public static final int SHOW_FORCED = 2;
    public static final int SHOW_IMPLICIT = 1;
    static final String TAG = "InputMethodManager";
    static InputMethodManager sInstance;
    CompletionInfo[] mCompletions;
    InputChannel mCurChannel;
    String mCurId;
    IInputMethodSession mCurMethod;
    View mCurRootView;
    ImeInputEventSender mCurSender;
    EditorInfo mCurrentTextBoxAttribute;
    int mCursorCandEnd;
    int mCursorCandStart;
    int mCursorSelEnd;
    int mCursorSelStart;
    boolean mFullscreenMode;
    final H mH;
    final IInputContext mIInputContext;
    final Looper mMainLooper;
    View mNextServedView;
    boolean mServedConnecting;
    InputConnection mServedInputConnection;
    ControlledInputConnectionWrapper mServedInputConnectionWrapper;
    View mServedView;
    final IInputMethodManager mService;
    boolean mActive = false;
    boolean mHasBeenInactive = true;
    Rect mTmpCursorRect = new Rect();
    Rect mCursorRect = new Rect();
    private int mNextUserActionNotificationSequenceNumber = -1;
    private int mLastSentUserActionNotificationSequenceNumber = -1;
    private CursorAnchorInfo mCursorAnchorInfo = null;
    private final int[] mViewTopLeft = new int[2];
    private final Matrix mViewToScreenMatrix = new Matrix();
    int mBindSequence = -1;
    private int mRequestUpdateCursorAnchorInfoMonitorMode = 0;
    final Pools.Pool<PendingEvent> mPendingEventPool = new Pools.SimplePool(20);
    final SparseArray<PendingEvent> mPendingEvents = new SparseArray<>(20);
    final IInputMethodClient.Stub mClient = new IInputMethodClient.Stub() {
        @Override
        protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
            CountDownLatch latch = new CountDownLatch(1);
            SomeArgs sargs = SomeArgs.obtain();
            sargs.arg1 = fd;
            sargs.arg2 = fout;
            sargs.arg3 = args;
            sargs.arg4 = latch;
            InputMethodManager.this.mH.sendMessage(InputMethodManager.this.mH.obtainMessage(1, sargs));
            try {
                if (!latch.await(5L, TimeUnit.SECONDS)) {
                    fout.println("Timeout waiting for dump");
                }
            } catch (InterruptedException e) {
                fout.println("Interrupted waiting for dump");
            }
        }

        @Override
        public void setUsingInputMethod(boolean state) {
        }

        @Override
        public void onBindMethod(InputBindResult res) {
            InputMethodManager.this.mH.sendMessage(InputMethodManager.this.mH.obtainMessage(2, res));
        }

        @Override
        public void onUnbindMethod(int sequence) {
            InputMethodManager.this.mH.sendMessage(InputMethodManager.this.mH.obtainMessage(3, sequence, 0));
        }

        @Override
        public void setActive(boolean active) {
            InputMethodManager.this.mH.sendMessage(InputMethodManager.this.mH.obtainMessage(4, active ? 1 : 0, 0));
        }

        @Override
        public void setUserActionNotificationSequenceNumber(int sequenceNumber) {
            InputMethodManager.this.mH.sendMessage(InputMethodManager.this.mH.obtainMessage(9, sequenceNumber, 0));
        }
    };
    final InputConnection mDummyInputConnection = new BaseInputConnection(this, false);

    public interface FinishedInputEventCallback {
        void onFinishedInputEvent(Object obj, boolean z);
    }

    class H extends Handler {
        H(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SomeArgs args = (SomeArgs) msg.obj;
                    try {
                        InputMethodManager.this.doDump((FileDescriptor) args.arg1, (PrintWriter) args.arg2, (String[]) args.arg3);
                        break;
                    } catch (RuntimeException e) {
                        ((PrintWriter) args.arg2).println("Exception: " + e);
                    }
                    synchronized (args.arg4) {
                        ((CountDownLatch) args.arg4).countDown();
                        break;
                    }
                    args.recycle();
                    return;
                case 2:
                    InputBindResult res = (InputBindResult) msg.obj;
                    synchronized (InputMethodManager.this.mH) {
                        if (InputMethodManager.this.mBindSequence >= 0 && InputMethodManager.this.mBindSequence == res.sequence) {
                            InputMethodManager.this.mRequestUpdateCursorAnchorInfoMonitorMode = 0;
                            InputMethodManager.this.setInputChannelLocked(res.channel);
                            InputMethodManager.this.mCurMethod = res.method;
                            InputMethodManager.this.mCurId = res.id;
                            InputMethodManager.this.mBindSequence = res.sequence;
                            InputMethodManager.this.startInputInner(null, 0, 0, 0);
                            return;
                        }
                        Log.w(InputMethodManager.TAG, "Ignoring onBind: cur seq=" + InputMethodManager.this.mBindSequence + ", given seq=" + res.sequence);
                        if (res.channel != null && res.channel != InputMethodManager.this.mCurChannel) {
                            res.channel.dispose();
                        }
                        return;
                    }
                case 3:
                    int sequence = msg.arg1;
                    boolean startInput = false;
                    synchronized (InputMethodManager.this.mH) {
                        if (InputMethodManager.this.mBindSequence == sequence) {
                            InputMethodManager.this.clearBindingLocked();
                            if (InputMethodManager.this.mServedView != null && InputMethodManager.this.mServedView.isFocused()) {
                                InputMethodManager.this.mServedConnecting = true;
                            }
                            if (InputMethodManager.this.mActive) {
                                startInput = true;
                            }
                        }
                        break;
                    }
                    if (startInput) {
                        InputMethodManager.this.startInputInner(null, 0, 0, 0);
                        return;
                    }
                    return;
                case 4:
                    boolean active = msg.arg1 != 0;
                    synchronized (InputMethodManager.this.mH) {
                        InputMethodManager.this.mActive = active;
                        InputMethodManager.this.mFullscreenMode = false;
                        if (!active) {
                            InputMethodManager.this.mHasBeenInactive = true;
                            try {
                                InputMethodManager.this.mIInputContext.finishComposingText();
                                break;
                            } catch (RemoteException e2) {
                            }
                            if (InputMethodManager.this.mServedView != null && InputMethodManager.this.mServedView.hasWindowFocus() && InputMethodManager.this.checkFocusNoStartInput(InputMethodManager.this.mHasBeenInactive, false)) {
                                InputMethodManager.this.startInputInner(null, 0, 0, 0);
                            }
                        }
                        break;
                    }
                    return;
                case 5:
                    InputMethodManager.this.sendInputEventAndReportResultOnMainLooper((PendingEvent) msg.obj);
                    return;
                case 6:
                    InputMethodManager.this.finishedInputEvent(msg.arg1, false, true);
                    return;
                case 7:
                    InputMethodManager.this.finishedInputEvent(msg.arg1, false, false);
                    return;
                case 8:
                default:
                    return;
                case 9:
                    synchronized (InputMethodManager.this.mH) {
                        InputMethodManager.this.mNextUserActionNotificationSequenceNumber = msg.arg1;
                        break;
                    }
                    return;
            }
        }
    }

    private static class ControlledInputConnectionWrapper extends IInputConnectionWrapper {
        private boolean mActive;
        private final InputMethodManager mParentInputMethodManager;

        public ControlledInputConnectionWrapper(Looper mainLooper, InputConnection conn, InputMethodManager inputMethodManager) {
            super(mainLooper, conn);
            this.mParentInputMethodManager = inputMethodManager;
            this.mActive = true;
        }

        @Override
        public boolean isActive() {
            return this.mParentInputMethodManager.mActive && this.mActive;
        }

        void deactivate() {
            this.mActive = false;
        }
    }

    InputMethodManager(IInputMethodManager service, Looper looper) {
        this.mService = service;
        this.mMainLooper = looper;
        this.mH = new H(looper);
        this.mIInputContext = new ControlledInputConnectionWrapper(looper, this.mDummyInputConnection, this);
    }

    public static InputMethodManager getInstance() {
        InputMethodManager inputMethodManager;
        synchronized (InputMethodManager.class) {
            if (sInstance == null) {
                IBinder b = ServiceManager.getService(Context.INPUT_METHOD_SERVICE);
                IInputMethodManager service = IInputMethodManager.Stub.asInterface(b);
                sInstance = new InputMethodManager(service, Looper.getMainLooper());
            }
            inputMethodManager = sInstance;
        }
        return inputMethodManager;
    }

    public static InputMethodManager peekInstance() {
        return sInstance;
    }

    public IInputMethodClient getClient() {
        return this.mClient;
    }

    public IInputContext getInputContext() {
        return this.mIInputContext;
    }

    public List<InputMethodInfo> getInputMethodList() {
        try {
            return this.mService.getInputMethodList();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public List<InputMethodInfo> getEnabledInputMethodList() {
        try {
            return this.mService.getEnabledInputMethodList();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public List<InputMethodSubtype> getEnabledInputMethodSubtypeList(InputMethodInfo imi, boolean allowsImplicitlySelectedSubtypes) {
        try {
            return this.mService.getEnabledInputMethodSubtypeList(imi == null ? null : imi.getId(), allowsImplicitlySelectedSubtypes);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void showStatusIcon(IBinder imeToken, String packageName, int iconId) {
        try {
            this.mService.updateStatusIcon(imeToken, packageName, iconId);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void hideStatusIcon(IBinder imeToken) {
        try {
            this.mService.updateStatusIcon(imeToken, null, 0);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setImeWindowStatus(IBinder imeToken, int vis, int backDisposition) {
        try {
            this.mService.setImeWindowStatus(imeToken, vis, backDisposition);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setFullscreenMode(boolean fullScreen) {
        this.mFullscreenMode = fullScreen;
    }

    public void registerSuggestionSpansForNotification(SuggestionSpan[] spans) {
        try {
            this.mService.registerSuggestionSpansForNotification(spans);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void notifySuggestionPicked(SuggestionSpan span, String originalString, int index) {
        try {
            this.mService.notifySuggestionPicked(span, originalString, index);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isFullscreenMode() {
        return this.mFullscreenMode;
    }

    public boolean isActive(View view) {
        boolean z;
        checkFocus();
        synchronized (this.mH) {
            z = (this.mServedView == view || (this.mServedView != null && this.mServedView.checkInputConnectionProxy(view))) && this.mCurrentTextBoxAttribute != null;
        }
        return z;
    }

    public boolean isActive() {
        boolean z;
        checkFocus();
        synchronized (this.mH) {
            z = (this.mServedView == null || this.mCurrentTextBoxAttribute == null) ? false : true;
        }
        return z;
    }

    public boolean isAcceptingText() {
        checkFocus();
        return this.mServedInputConnection != null;
    }

    void clearBindingLocked() {
        clearConnectionLocked();
        setInputChannelLocked(null);
        this.mBindSequence = -1;
        this.mCurId = null;
        this.mCurMethod = null;
    }

    void setInputChannelLocked(InputChannel channel) {
        if (this.mCurChannel != channel) {
            if (this.mCurSender != null) {
                flushPendingEventsLocked();
                this.mCurSender.dispose();
                this.mCurSender = null;
            }
            if (this.mCurChannel != null) {
                this.mCurChannel.dispose();
            }
            this.mCurChannel = channel;
        }
    }

    void clearConnectionLocked() {
        this.mCurrentTextBoxAttribute = null;
        this.mServedInputConnection = null;
        if (this.mServedInputConnectionWrapper != null) {
            this.mServedInputConnectionWrapper.deactivate();
            this.mServedInputConnectionWrapper = null;
        }
    }

    void finishInputLocked() {
        this.mCurRootView = null;
        this.mNextServedView = null;
        if (this.mServedView != null) {
            if (this.mCurrentTextBoxAttribute != null) {
                try {
                    this.mService.finishInput(this.mClient);
                } catch (RemoteException e) {
                }
            }
            notifyInputConnectionFinished();
            this.mServedView = null;
            this.mCompletions = null;
            this.mServedConnecting = false;
            clearConnectionLocked();
        }
    }

    private void notifyInputConnectionFinished() {
        ViewRootImpl viewRootImpl;
        if (this.mServedView != null && this.mServedInputConnection != null && (viewRootImpl = this.mServedView.getViewRootImpl()) != null) {
            viewRootImpl.dispatchFinishInputConnection(this.mServedInputConnection);
        }
    }

    public void reportFinishInputConnection(InputConnection ic) {
        if (this.mServedInputConnection != ic) {
            ic.finishComposingText();
            if (ic instanceof BaseInputConnection) {
                ((BaseInputConnection) ic).reportFinish();
            }
        }
    }

    public void displayCompletions(View view, CompletionInfo[] completions) {
        checkFocus();
        synchronized (this.mH) {
            if (this.mServedView == view || (this.mServedView != null && this.mServedView.checkInputConnectionProxy(view))) {
                this.mCompletions = completions;
                if (this.mCurMethod != null) {
                    try {
                        this.mCurMethod.displayCompletions(this.mCompletions);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    public void updateExtractedText(View view, int token, ExtractedText text) {
        checkFocus();
        synchronized (this.mH) {
            if (this.mServedView == view || (this.mServedView != null && this.mServedView.checkInputConnectionProxy(view))) {
                if (this.mCurMethod != null) {
                    try {
                        this.mCurMethod.updateExtractedText(token, text);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    public boolean showSoftInput(View view, int flags) {
        return showSoftInput(view, flags, null);
    }

    public boolean showSoftInput(View view, int flags, ResultReceiver resultReceiver) {
        boolean zShowSoftInput = false;
        checkFocus();
        synchronized (this.mH) {
            if (this.mServedView == view || (this.mServedView != null && this.mServedView.checkInputConnectionProxy(view))) {
                try {
                    zShowSoftInput = this.mService.showSoftInput(this.mClient, flags, resultReceiver);
                } catch (RemoteException e) {
                }
            }
        }
        return zShowSoftInput;
    }

    public void showSoftInputUnchecked(int flags, ResultReceiver resultReceiver) {
        try {
            this.mService.showSoftInput(this.mClient, flags, resultReceiver);
        } catch (RemoteException e) {
        }
    }

    public boolean hideSoftInputFromWindow(IBinder windowToken, int flags) {
        return hideSoftInputFromWindow(windowToken, flags, null);
    }

    public boolean hideSoftInputFromWindow(IBinder windowToken, int flags, ResultReceiver resultReceiver) {
        boolean zHideSoftInput = false;
        checkFocus();
        synchronized (this.mH) {
            if (this.mServedView != null && this.mServedView.getWindowToken() == windowToken) {
                try {
                    zHideSoftInput = this.mService.hideSoftInput(this.mClient, flags, resultReceiver);
                } catch (RemoteException e) {
                }
            }
        }
        return zHideSoftInput;
    }

    public void toggleSoftInputFromWindow(IBinder windowToken, int showFlags, int hideFlags) {
        synchronized (this.mH) {
            if (this.mServedView != null && this.mServedView.getWindowToken() == windowToken) {
                if (this.mCurMethod != null) {
                    try {
                        this.mCurMethod.toggleSoftInput(showFlags, hideFlags);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    public void toggleSoftInput(int showFlags, int hideFlags) {
        if (this.mCurMethod != null) {
            try {
                this.mCurMethod.toggleSoftInput(showFlags, hideFlags);
            } catch (RemoteException e) {
            }
        }
    }

    public void restartInput(View view) {
        checkFocus();
        synchronized (this.mH) {
            if (this.mServedView == view || (this.mServedView != null && this.mServedView.checkInputConnectionProxy(view))) {
                this.mServedConnecting = true;
                startInputInner(null, 0, 0, 0);
            }
        }
    }

    boolean startInputInner(IBinder windowGainingFocus, int controlFlags, int softInputMode, int windowFlags) {
        ControlledInputConnectionWrapper servedContext;
        InputBindResult res;
        synchronized (this.mH) {
            View view = this.mServedView;
            if (view == null) {
                return false;
            }
            Handler vh = view.getHandler();
            if (vh == null) {
                closeCurrentInput();
                return false;
            }
            if (vh.getLooper() != Looper.myLooper()) {
                vh.post(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager.this.startInputInner(null, 0, 0, 0);
                    }
                });
                return false;
            }
            EditorInfo tba = new EditorInfo();
            tba.packageName = view.getContext().getPackageName();
            tba.fieldId = view.getId();
            InputConnection ic = view.onCreateInputConnection(tba);
            synchronized (this.mH) {
                if (this.mServedView != view || !this.mServedConnecting) {
                    return false;
                }
                if (this.mCurrentTextBoxAttribute == null) {
                    controlFlags |= 256;
                }
                this.mCurrentTextBoxAttribute = tba;
                this.mServedConnecting = false;
                notifyInputConnectionFinished();
                this.mServedInputConnection = ic;
                if (ic != null) {
                    this.mCursorSelStart = tba.initialSelStart;
                    this.mCursorSelEnd = tba.initialSelEnd;
                    this.mCursorCandStart = -1;
                    this.mCursorCandEnd = -1;
                    this.mCursorRect.setEmpty();
                    this.mCursorAnchorInfo = null;
                    servedContext = new ControlledInputConnectionWrapper(vh.getLooper(), ic, this);
                } else {
                    servedContext = null;
                }
                if (this.mServedInputConnectionWrapper != null) {
                    this.mServedInputConnectionWrapper.deactivate();
                }
                this.mServedInputConnectionWrapper = servedContext;
                try {
                    if (windowGainingFocus != null) {
                        res = this.mService.windowGainedFocus(this.mClient, windowGainingFocus, controlFlags, softInputMode, windowFlags, tba, servedContext);
                    } else {
                        res = this.mService.startInput(this.mClient, servedContext, tba, controlFlags);
                    }
                    if (res != null) {
                        if (res.id != null) {
                            setInputChannelLocked(res.channel);
                            this.mBindSequence = res.sequence;
                            this.mCurMethod = res.method;
                            this.mCurId = res.id;
                            this.mNextUserActionNotificationSequenceNumber = res.userActionNotificationSequenceNumber;
                        } else {
                            if (res.channel != null && res.channel != this.mCurChannel) {
                                res.channel.dispose();
                            }
                            if (this.mCurMethod == null) {
                                return true;
                            }
                        }
                    }
                    if (this.mCurMethod != null) {
                        if (this.mCompletions != null) {
                            try {
                                this.mCurMethod.displayCompletions(this.mCompletions);
                            } catch (RemoteException e) {
                            }
                        }
                    }
                } catch (RemoteException e2) {
                    Log.w(TAG, "IME died: " + this.mCurId, e2);
                }
                return true;
            }
        }
    }

    public void windowDismissed(IBinder appWindowToken) {
        checkFocus();
        synchronized (this.mH) {
            if (this.mServedView != null && this.mServedView.getWindowToken() == appWindowToken) {
                finishInputLocked();
            }
        }
    }

    public void focusIn(View view) {
        synchronized (this.mH) {
            focusInLocked(view);
        }
    }

    void focusInLocked(View view) {
        if (this.mCurRootView == view.getRootView()) {
            this.mNextServedView = view;
            scheduleCheckFocusLocked(view);
        }
    }

    public void focusOut(View view) {
        synchronized (this.mH) {
            if (this.mServedView != view) {
            }
        }
    }

    static void scheduleCheckFocusLocked(View view) {
        ViewRootImpl viewRootImpl = view.getViewRootImpl();
        if (viewRootImpl != null) {
            viewRootImpl.dispatchCheckFocus();
        }
    }

    public void checkFocus() {
        if (checkFocusNoStartInput(false, true)) {
            startInputInner(null, 0, 0, 0);
        }
    }

    private boolean checkFocusNoStartInput(boolean forceNewFocus, boolean finishComposingText) {
        boolean z = false;
        if (this.mServedView != this.mNextServedView || forceNewFocus) {
            synchronized (this.mH) {
                if (this.mServedView != this.mNextServedView || forceNewFocus) {
                    if (this.mNextServedView == null) {
                        finishInputLocked();
                        closeCurrentInput();
                    } else {
                        InputConnection ic = this.mServedInputConnection;
                        this.mServedView = this.mNextServedView;
                        this.mCurrentTextBoxAttribute = null;
                        this.mCompletions = null;
                        this.mServedConnecting = true;
                        if (finishComposingText && ic != null) {
                            ic.finishComposingText();
                        }
                        z = true;
                    }
                }
            }
        }
        return z;
    }

    void closeCurrentInput() {
        try {
            this.mService.hideSoftInput(this.mClient, 2, null);
        } catch (RemoteException e) {
        }
    }

    public void onWindowFocus(View rootView, View focusedView, int softInputMode, boolean first, int windowFlags) {
        boolean forceNewFocus = false;
        synchronized (this.mH) {
            if (this.mHasBeenInactive) {
                this.mHasBeenInactive = false;
                forceNewFocus = true;
            }
            focusInLocked(focusedView != null ? focusedView : rootView);
        }
        int controlFlags = 0;
        if (focusedView != null) {
            controlFlags = 0 | 1;
            if (focusedView.onCheckIsTextEditor()) {
                controlFlags |= 2;
            }
        }
        if (first) {
            controlFlags |= 4;
        }
        if (!checkFocusNoStartInput(forceNewFocus, true) || !startInputInner(rootView.getWindowToken(), controlFlags, softInputMode, windowFlags)) {
            synchronized (this.mH) {
                try {
                    this.mService.windowGainedFocus(this.mClient, rootView.getWindowToken(), controlFlags, softInputMode, windowFlags, null, null);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void startGettingWindowFocus(View rootView) {
        synchronized (this.mH) {
            this.mCurRootView = rootView;
        }
    }

    public void updateSelection(View view, int selStart, int selEnd, int candidatesStart, int candidatesEnd) {
        checkFocus();
        synchronized (this.mH) {
            if ((this.mServedView == view || (this.mServedView != null && this.mServedView.checkInputConnectionProxy(view))) && this.mCurrentTextBoxAttribute != null && this.mCurMethod != null) {
                if (this.mCursorSelStart != selStart || this.mCursorSelEnd != selEnd || this.mCursorCandStart != candidatesStart || this.mCursorCandEnd != candidatesEnd) {
                    try {
                        int oldSelStart = this.mCursorSelStart;
                        int oldSelEnd = this.mCursorSelEnd;
                        this.mCursorSelStart = selStart;
                        this.mCursorSelEnd = selEnd;
                        this.mCursorCandStart = candidatesStart;
                        this.mCursorCandEnd = candidatesEnd;
                        this.mCurMethod.updateSelection(oldSelStart, oldSelEnd, selStart, selEnd, candidatesStart, candidatesEnd);
                    } catch (RemoteException e) {
                        Log.w(TAG, "IME died: " + this.mCurId, e);
                    }
                }
            }
        }
    }

    public void viewClicked(View view) {
        boolean focusChanged = this.mServedView != this.mNextServedView;
        checkFocus();
        synchronized (this.mH) {
            if ((this.mServedView == view || (this.mServedView != null && this.mServedView.checkInputConnectionProxy(view))) && this.mCurrentTextBoxAttribute != null && this.mCurMethod != null) {
                try {
                    this.mCurMethod.viewClicked(focusChanged);
                } catch (RemoteException e) {
                    Log.w(TAG, "IME died: " + this.mCurId, e);
                }
            }
        }
    }

    @Deprecated
    public boolean isWatchingCursor(View view) {
        return false;
    }

    public boolean isCursorAnchorInfoEnabled() {
        boolean z;
        synchronized (this.mH) {
            boolean isImmediate = (this.mRequestUpdateCursorAnchorInfoMonitorMode & 1) != 0;
            boolean isMonitoring = (this.mRequestUpdateCursorAnchorInfoMonitorMode & 2) != 0;
            z = isImmediate || isMonitoring;
        }
        return z;
    }

    public void setUpdateCursorAnchorInfoMode(int flags) {
        synchronized (this.mH) {
            this.mRequestUpdateCursorAnchorInfoMonitorMode = flags;
        }
    }

    @Deprecated
    public void updateCursor(View view, int left, int top, int right, int bottom) {
        checkFocus();
        synchronized (this.mH) {
            if ((this.mServedView == view || (this.mServedView != null && this.mServedView.checkInputConnectionProxy(view))) && this.mCurrentTextBoxAttribute != null && this.mCurMethod != null) {
                this.mTmpCursorRect.set(left, top, right, bottom);
                if (!this.mCursorRect.equals(this.mTmpCursorRect)) {
                    try {
                        this.mCurMethod.updateCursor(this.mTmpCursorRect);
                        this.mCursorRect.set(this.mTmpCursorRect);
                    } catch (RemoteException e) {
                        Log.w(TAG, "IME died: " + this.mCurId, e);
                    }
                }
            }
        }
    }

    public void updateCursorAnchorInfo(View view, CursorAnchorInfo cursorAnchorInfo) {
        if (view != null && cursorAnchorInfo != null) {
            checkFocus();
            synchronized (this.mH) {
                if ((this.mServedView == view || (this.mServedView != null && this.mServedView.checkInputConnectionProxy(view))) && this.mCurrentTextBoxAttribute != null && this.mCurMethod != null) {
                    boolean isImmediate = (this.mRequestUpdateCursorAnchorInfoMonitorMode & 1) != 0;
                    if (isImmediate || !Objects.equals(this.mCursorAnchorInfo, cursorAnchorInfo)) {
                        try {
                            this.mCurMethod.updateCursorAnchorInfo(cursorAnchorInfo);
                            this.mCursorAnchorInfo = cursorAnchorInfo;
                            this.mRequestUpdateCursorAnchorInfoMonitorMode &= -2;
                        } catch (RemoteException e) {
                            Log.w(TAG, "IME died: " + this.mCurId, e);
                        }
                    }
                }
            }
        }
    }

    public void sendAppPrivateCommand(View view, String action, Bundle data) {
        checkFocus();
        synchronized (this.mH) {
            if ((this.mServedView == view || (this.mServedView != null && this.mServedView.checkInputConnectionProxy(view))) && this.mCurrentTextBoxAttribute != null && this.mCurMethod != null) {
                try {
                    this.mCurMethod.appPrivateCommand(action, data);
                } catch (RemoteException e) {
                    Log.w(TAG, "IME died: " + this.mCurId, e);
                }
            }
        }
    }

    public void setInputMethod(IBinder token, String id) {
        try {
            this.mService.setInputMethod(token, id);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setInputMethodAndSubtype(IBinder token, String id, InputMethodSubtype subtype) {
        try {
            this.mService.setInputMethodAndSubtype(token, id, subtype);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void hideSoftInputFromInputMethod(IBinder token, int flags) {
        try {
            this.mService.hideMySoftInput(token, flags);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void showSoftInputFromInputMethod(IBinder token, int flags) {
        try {
            this.mService.showMySoftInput(token, flags);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public int dispatchInputEvent(InputEvent event, Object token, FinishedInputEventCallback callback, Handler handler) {
        synchronized (this.mH) {
            if (this.mCurMethod != null) {
                if (event instanceof KeyEvent) {
                    KeyEvent keyEvent = (KeyEvent) event;
                    if (keyEvent.getAction() == 0 && keyEvent.getKeyCode() == 63 && keyEvent.getRepeatCount() == 0) {
                        showInputMethodPickerLocked();
                        return 1;
                    }
                }
                PendingEvent p = obtainPendingEventLocked(event, token, this.mCurId, callback, handler);
                if (this.mMainLooper.isCurrentThread()) {
                    return sendInputEventOnMainLooperLocked(p);
                }
                Message msg = this.mH.obtainMessage(5, p);
                msg.setAsynchronous(true);
                this.mH.sendMessage(msg);
                return -1;
            }
            return 0;
        }
    }

    void sendInputEventAndReportResultOnMainLooper(PendingEvent p) {
        synchronized (this.mH) {
            int result = sendInputEventOnMainLooperLocked(p);
            if (result != -1) {
                boolean handled = result == 1;
                invokeFinishedInputEventCallback(p, handled);
            }
        }
    }

    int sendInputEventOnMainLooperLocked(PendingEvent p) {
        if (this.mCurChannel != null) {
            if (this.mCurSender == null) {
                this.mCurSender = new ImeInputEventSender(this.mCurChannel, this.mH.getLooper());
            }
            InputEvent event = p.mEvent;
            int seq = event.getSequenceNumber();
            if (this.mCurSender.sendInputEvent(seq, event)) {
                this.mPendingEvents.put(seq, p);
                Trace.traceCounter(4L, PENDING_EVENT_COUNTER, this.mPendingEvents.size());
                Message msg = this.mH.obtainMessage(6, p);
                msg.setAsynchronous(true);
                this.mH.sendMessageDelayed(msg, INPUT_METHOD_NOT_RESPONDING_TIMEOUT);
                return -1;
            }
            Log.w(TAG, "Unable to send input event to IME: " + this.mCurId + " dropping: " + event);
        }
        return 0;
    }

    void finishedInputEvent(int seq, boolean handled, boolean timeout) {
        synchronized (this.mH) {
            int index = this.mPendingEvents.indexOfKey(seq);
            if (index >= 0) {
                PendingEvent p = this.mPendingEvents.valueAt(index);
                this.mPendingEvents.removeAt(index);
                Trace.traceCounter(4L, PENDING_EVENT_COUNTER, this.mPendingEvents.size());
                if (timeout) {
                    Log.w(TAG, "Timeout waiting for IME to handle input event after 2500 ms: " + p.mInputMethodId);
                } else {
                    this.mH.removeMessages(6, p);
                }
                invokeFinishedInputEventCallback(p, handled);
            }
        }
    }

    void invokeFinishedInputEventCallback(PendingEvent p, boolean handled) {
        p.mHandled = handled;
        if (p.mHandler.getLooper().isCurrentThread()) {
            p.run();
            return;
        }
        Message msg = Message.obtain(p.mHandler, p);
        msg.setAsynchronous(true);
        msg.sendToTarget();
    }

    private void flushPendingEventsLocked() {
        this.mH.removeMessages(7);
        int count = this.mPendingEvents.size();
        for (int i = 0; i < count; i++) {
            int seq = this.mPendingEvents.keyAt(i);
            Message msg = this.mH.obtainMessage(7, seq, 0);
            msg.setAsynchronous(true);
            msg.sendToTarget();
        }
    }

    private PendingEvent obtainPendingEventLocked(InputEvent event, Object token, String inputMethodId, FinishedInputEventCallback callback, Handler handler) {
        PendingEvent p = this.mPendingEventPool.acquire();
        if (p == null) {
            p = new PendingEvent();
        }
        p.mEvent = event;
        p.mToken = token;
        p.mInputMethodId = inputMethodId;
        p.mCallback = callback;
        p.mHandler = handler;
        return p;
    }

    private void recyclePendingEventLocked(PendingEvent p) {
        p.recycle();
        this.mPendingEventPool.release(p);
    }

    public void showInputMethodPicker() {
        synchronized (this.mH) {
            showInputMethodPickerLocked();
        }
    }

    private void showInputMethodPickerLocked() {
        try {
            this.mService.showInputMethodPickerFromClient(this.mClient);
        } catch (RemoteException e) {
            Log.w(TAG, "IME died: " + this.mCurId, e);
        }
    }

    public void showInputMethodAndSubtypeEnabler(String imiId) {
        synchronized (this.mH) {
            try {
                this.mService.showInputMethodAndSubtypeEnablerFromClient(this.mClient, imiId);
            } catch (RemoteException e) {
                Log.w(TAG, "IME died: " + this.mCurId, e);
            }
        }
    }

    public InputMethodSubtype getCurrentInputMethodSubtype() {
        InputMethodSubtype currentInputMethodSubtype;
        synchronized (this.mH) {
            try {
                currentInputMethodSubtype = this.mService.getCurrentInputMethodSubtype();
            } catch (RemoteException e) {
                Log.w(TAG, "IME died: " + this.mCurId, e);
                currentInputMethodSubtype = null;
            }
        }
        return currentInputMethodSubtype;
    }

    public boolean setCurrentInputMethodSubtype(InputMethodSubtype subtype) {
        boolean currentInputMethodSubtype;
        synchronized (this.mH) {
            try {
                currentInputMethodSubtype = this.mService.setCurrentInputMethodSubtype(subtype);
            } catch (RemoteException e) {
                Log.w(TAG, "IME died: " + this.mCurId, e);
                currentInputMethodSubtype = false;
            }
        }
        return currentInputMethodSubtype;
    }

    public void notifyUserAction() {
        synchronized (this.mH) {
            if (this.mLastSentUserActionNotificationSequenceNumber != this.mNextUserActionNotificationSequenceNumber) {
                try {
                    this.mService.notifyUserAction(this.mNextUserActionNotificationSequenceNumber);
                    this.mLastSentUserActionNotificationSequenceNumber = this.mNextUserActionNotificationSequenceNumber;
                } catch (RemoteException e) {
                    Log.w(TAG, "IME died: " + this.mCurId, e);
                }
            }
        }
    }

    public Map<InputMethodInfo, List<InputMethodSubtype>> getShortcutInputMethodsAndSubtypes() {
        HashMap<InputMethodInfo, List<InputMethodSubtype>> ret;
        List<Object> info;
        ArrayList<InputMethodSubtype> subtypes;
        int N;
        synchronized (this.mH) {
            ret = new HashMap<>();
            try {
                info = this.mService.getShortcutInputMethodsAndSubtypes();
                subtypes = null;
                N = info.size();
            } catch (RemoteException e) {
                Log.w(TAG, "IME died: " + this.mCurId, e);
            }
            if (info != null && N > 0) {
                int i = 0;
                while (true) {
                    if (i >= N) {
                        break;
                    }
                    Object o = info.get(i);
                    if (o instanceof InputMethodInfo) {
                        if (ret.containsKey(o)) {
                            break;
                        }
                        subtypes = new ArrayList<>();
                        ret.put((InputMethodInfo) o, subtypes);
                    } else if (subtypes != null && (o instanceof InputMethodSubtype)) {
                        subtypes.add((InputMethodSubtype) o);
                    }
                    i++;
                }
            }
        }
        return ret;
    }

    public int getInputMethodWindowVisibleHeight() {
        int inputMethodWindowVisibleHeight;
        synchronized (this.mH) {
            try {
                inputMethodWindowVisibleHeight = this.mService.getInputMethodWindowVisibleHeight();
            } catch (RemoteException e) {
                Log.w(TAG, "IME died: " + this.mCurId, e);
                inputMethodWindowVisibleHeight = 0;
            }
        }
        return inputMethodWindowVisibleHeight;
    }

    public boolean switchToLastInputMethod(IBinder imeToken) {
        boolean zSwitchToLastInputMethod;
        synchronized (this.mH) {
            try {
                zSwitchToLastInputMethod = this.mService.switchToLastInputMethod(imeToken);
            } catch (RemoteException e) {
                Log.w(TAG, "IME died: " + this.mCurId, e);
                zSwitchToLastInputMethod = false;
            }
        }
        return zSwitchToLastInputMethod;
    }

    public boolean switchToNextInputMethod(IBinder imeToken, boolean onlyCurrentIme) {
        boolean zSwitchToNextInputMethod;
        synchronized (this.mH) {
            try {
                zSwitchToNextInputMethod = this.mService.switchToNextInputMethod(imeToken, onlyCurrentIme);
            } catch (RemoteException e) {
                Log.w(TAG, "IME died: " + this.mCurId, e);
                zSwitchToNextInputMethod = false;
            }
        }
        return zSwitchToNextInputMethod;
    }

    public boolean shouldOfferSwitchingToNextInputMethod(IBinder imeToken) {
        boolean zShouldOfferSwitchingToNextInputMethod;
        synchronized (this.mH) {
            try {
                zShouldOfferSwitchingToNextInputMethod = this.mService.shouldOfferSwitchingToNextInputMethod(imeToken);
            } catch (RemoteException e) {
                Log.w(TAG, "IME died: " + this.mCurId, e);
                zShouldOfferSwitchingToNextInputMethod = false;
            }
        }
        return zShouldOfferSwitchingToNextInputMethod;
    }

    public void setAdditionalInputMethodSubtypes(String imiId, InputMethodSubtype[] subtypes) {
        synchronized (this.mH) {
            try {
                this.mService.setAdditionalInputMethodSubtypes(imiId, subtypes);
            } catch (RemoteException e) {
                Log.w(TAG, "IME died: " + this.mCurId, e);
            }
        }
    }

    public InputMethodSubtype getLastInputMethodSubtype() {
        InputMethodSubtype lastInputMethodSubtype;
        synchronized (this.mH) {
            try {
                lastInputMethodSubtype = this.mService.getLastInputMethodSubtype();
            } catch (RemoteException e) {
                Log.w(TAG, "IME died: " + this.mCurId, e);
                lastInputMethodSubtype = null;
            }
        }
        return lastInputMethodSubtype;
    }

    void doDump(FileDescriptor fd, PrintWriter fout, String[] args) {
        Printer p = new PrintWriterPrinter(fout);
        p.println("Input method client state for " + this + ":");
        p.println("  mService=" + this.mService);
        p.println("  mMainLooper=" + this.mMainLooper);
        p.println("  mIInputContext=" + this.mIInputContext);
        p.println("  mActive=" + this.mActive + " mHasBeenInactive=" + this.mHasBeenInactive + " mBindSequence=" + this.mBindSequence + " mCurId=" + this.mCurId);
        p.println("  mCurMethod=" + this.mCurMethod);
        p.println("  mCurRootView=" + this.mCurRootView);
        p.println("  mServedView=" + this.mServedView);
        p.println("  mNextServedView=" + this.mNextServedView);
        p.println("  mServedConnecting=" + this.mServedConnecting);
        if (this.mCurrentTextBoxAttribute != null) {
            p.println("  mCurrentTextBoxAttribute:");
            this.mCurrentTextBoxAttribute.dump(p, "    ");
        } else {
            p.println("  mCurrentTextBoxAttribute: null");
        }
        p.println("  mServedInputConnection=" + this.mServedInputConnection);
        p.println("  mCompletions=" + this.mCompletions);
        p.println("  mCursorRect=" + this.mCursorRect);
        p.println("  mCursorSelStart=" + this.mCursorSelStart + " mCursorSelEnd=" + this.mCursorSelEnd + " mCursorCandStart=" + this.mCursorCandStart + " mCursorCandEnd=" + this.mCursorCandEnd);
        p.println("  mNextUserActionNotificationSequenceNumber=" + this.mNextUserActionNotificationSequenceNumber + " mLastSentUserActionNotificationSequenceNumber=" + this.mLastSentUserActionNotificationSequenceNumber);
    }

    private final class ImeInputEventSender extends InputEventSender {
        public ImeInputEventSender(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        @Override
        public void onInputEventFinished(int seq, boolean handled) {
            InputMethodManager.this.finishedInputEvent(seq, handled, false);
        }
    }

    private final class PendingEvent implements Runnable {
        public FinishedInputEventCallback mCallback;
        public InputEvent mEvent;
        public boolean mHandled;
        public Handler mHandler;
        public String mInputMethodId;
        public Object mToken;

        private PendingEvent() {
        }

        public void recycle() {
            this.mEvent = null;
            this.mToken = null;
            this.mInputMethodId = null;
            this.mCallback = null;
            this.mHandler = null;
            this.mHandled = false;
        }

        @Override
        public void run() {
            this.mCallback.onFinishedInputEvent(this.mToken, this.mHandled);
            synchronized (InputMethodManager.this.mH) {
                InputMethodManager.this.recyclePendingEventLocked(this);
            }
        }
    }
}
