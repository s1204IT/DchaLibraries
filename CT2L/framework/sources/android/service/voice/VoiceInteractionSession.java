package android.service.voice;

import android.R;
import android.app.Dialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.Region;
import android.inputmethodservice.SoftInputWindow;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.app.IVoiceInteractorCallback;
import com.android.internal.app.IVoiceInteractorRequest;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;
import java.lang.ref.WeakReference;

public abstract class VoiceInteractionSession implements KeyEvent.Callback {
    static final boolean DEBUG = true;
    static final int MSG_CANCEL = 6;
    static final int MSG_CLOSE_SYSTEM_DIALOGS = 102;
    static final int MSG_DESTROY = 103;
    static final int MSG_START_ABORT_VOICE = 3;
    static final int MSG_START_COMMAND = 4;
    static final int MSG_START_COMPLETE_VOICE = 2;
    static final int MSG_START_CONFIRMATION = 1;
    static final int MSG_SUPPORTS_COMMANDS = 5;
    static final int MSG_TASK_FINISHED = 101;
    static final int MSG_TASK_STARTED = 100;
    static final String TAG = "VoiceInteractionSession";
    final ArrayMap<IBinder, Request> mActiveRequests;
    final MyCallbacks mCallbacks;
    FrameLayout mContentFrame;
    final Context mContext;
    final KeyEvent.DispatcherState mDispatcherState;
    final HandlerCaller mHandlerCaller;
    boolean mInShowWindow;
    LayoutInflater mInflater;
    boolean mInitialized;
    final ViewTreeObserver.OnComputeInternalInsetsListener mInsetsComputer;
    final IVoiceInteractor mInteractor;
    View mRootView;
    final IVoiceInteractionSession mSession;
    IVoiceInteractionManagerService mSystemService;
    int mTheme;
    TypedArray mThemeAttrs;
    final Insets mTmpInsets;
    final int[] mTmpLocation;
    IBinder mToken;
    final WeakReference<VoiceInteractionSession> mWeakRef;
    SoftInputWindow mWindow;
    boolean mWindowAdded;
    boolean mWindowVisible;
    boolean mWindowWasVisible;

    public static final class Insets {
        public static final int TOUCHABLE_INSETS_CONTENT = 1;
        public static final int TOUCHABLE_INSETS_FRAME = 0;
        public static final int TOUCHABLE_INSETS_REGION = 3;
        public int touchableInsets;
        public final Rect contentInsets = new Rect();
        public final Region touchableRegion = new Region();
    }

    public abstract void onCancel(Request request);

    public abstract void onCommand(Caller caller, Request request, String str, Bundle bundle);

    public abstract void onConfirm(Caller caller, Request request, CharSequence charSequence, Bundle bundle);

    public static class Request {
        final IVoiceInteractorCallback mCallback;
        final IVoiceInteractorRequest mInterface = new IVoiceInteractorRequest.Stub() {
            @Override
            public void cancel() throws RemoteException {
                VoiceInteractionSession session = Request.this.mSession.get();
                if (session != null) {
                    session.mHandlerCaller.sendMessage(session.mHandlerCaller.obtainMessageO(6, Request.this));
                }
            }
        };
        final WeakReference<VoiceInteractionSession> mSession;

        Request(IVoiceInteractorCallback callback, VoiceInteractionSession session) {
            this.mCallback = callback;
            this.mSession = session.mWeakRef;
        }

        void finishRequest() {
            VoiceInteractionSession session = this.mSession.get();
            if (session == null) {
                throw new IllegalStateException("VoiceInteractionSession has been destroyed");
            }
            Request req = session.removeRequest(this.mInterface.asBinder());
            if (req == null) {
                throw new IllegalStateException("Request not active: " + this);
            }
            if (req != this) {
                throw new IllegalStateException("Current active request " + req + " not same as calling request " + this);
            }
        }

        public void sendConfirmResult(boolean confirmed, Bundle result) {
            try {
                Log.d(VoiceInteractionSession.TAG, "sendConfirmResult: req=" + this.mInterface + " confirmed=" + confirmed + " result=" + result);
                finishRequest();
                this.mCallback.deliverConfirmationResult(this.mInterface, confirmed, result);
            } catch (RemoteException e) {
            }
        }

        public void sendCompleteVoiceResult(Bundle result) {
            try {
                Log.d(VoiceInteractionSession.TAG, "sendCompleteVoiceResult: req=" + this.mInterface + " result=" + result);
                finishRequest();
                this.mCallback.deliverCompleteVoiceResult(this.mInterface, result);
            } catch (RemoteException e) {
            }
        }

        public void sendAbortVoiceResult(Bundle result) {
            try {
                Log.d(VoiceInteractionSession.TAG, "sendConfirmResult: req=" + this.mInterface + " result=" + result);
                finishRequest();
                this.mCallback.deliverAbortVoiceResult(this.mInterface, result);
            } catch (RemoteException e) {
            }
        }

        public void sendCommandResult(boolean complete, Bundle result) {
            try {
                Log.d(VoiceInteractionSession.TAG, "sendCommandResult: req=" + this.mInterface + " result=" + result);
                finishRequest();
                this.mCallback.deliverCommandResult(this.mInterface, complete, result);
            } catch (RemoteException e) {
            }
        }

        public void sendCancelResult() {
            try {
                Log.d(VoiceInteractionSession.TAG, "sendCancelResult: req=" + this.mInterface);
                finishRequest();
                this.mCallback.deliverCancel(this.mInterface);
            } catch (RemoteException e) {
            }
        }
    }

    public static class Caller {
        final String packageName;
        final int uid;

        Caller(String _packageName, int _uid) {
            this.packageName = _packageName;
            this.uid = _uid;
        }
    }

    class MyCallbacks implements HandlerCaller.Callback, SoftInputWindow.Callback {
        MyCallbacks() {
        }

        @Override
        public void executeMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SomeArgs args = (SomeArgs) msg.obj;
                    Log.d(VoiceInteractionSession.TAG, "onConfirm: req=" + ((Request) args.arg2).mInterface + " prompt=" + args.arg3 + " extras=" + args.arg4);
                    VoiceInteractionSession.this.onConfirm((Caller) args.arg1, (Request) args.arg2, (CharSequence) args.arg3, (Bundle) args.arg4);
                    break;
                case 2:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    Log.d(VoiceInteractionSession.TAG, "onCompleteVoice: req=" + ((Request) args2.arg2).mInterface + " message=" + args2.arg3 + " extras=" + args2.arg4);
                    VoiceInteractionSession.this.onCompleteVoice((Caller) args2.arg1, (Request) args2.arg2, (CharSequence) args2.arg3, (Bundle) args2.arg4);
                    break;
                case 3:
                    SomeArgs args3 = (SomeArgs) msg.obj;
                    Log.d(VoiceInteractionSession.TAG, "onAbortVoice: req=" + ((Request) args3.arg2).mInterface + " message=" + args3.arg3 + " extras=" + args3.arg4);
                    VoiceInteractionSession.this.onAbortVoice((Caller) args3.arg1, (Request) args3.arg2, (CharSequence) args3.arg3, (Bundle) args3.arg4);
                    break;
                case 4:
                    SomeArgs args4 = (SomeArgs) msg.obj;
                    Log.d(VoiceInteractionSession.TAG, "onCommand: req=" + ((Request) args4.arg2).mInterface + " command=" + args4.arg3 + " extras=" + args4.arg4);
                    VoiceInteractionSession.this.onCommand((Caller) args4.arg1, (Request) args4.arg2, (String) args4.arg3, (Bundle) args4.arg4);
                    break;
                case 5:
                    SomeArgs args5 = (SomeArgs) msg.obj;
                    Log.d(VoiceInteractionSession.TAG, "onGetSupportedCommands: cmds=" + args5.arg2);
                    args5.arg1 = VoiceInteractionSession.this.onGetSupportedCommands((Caller) args5.arg1, (String[]) args5.arg2);
                    break;
                case 6:
                    SomeArgs args6 = (SomeArgs) msg.obj;
                    Log.d(VoiceInteractionSession.TAG, "onCancel: req=" + ((Request) args6.arg1).mInterface);
                    VoiceInteractionSession.this.onCancel((Request) args6.arg1);
                    break;
                case 100:
                    Log.d(VoiceInteractionSession.TAG, "onTaskStarted: intent=" + msg.obj + " taskId=" + msg.arg1);
                    VoiceInteractionSession.this.onTaskStarted((Intent) msg.obj, msg.arg1);
                    break;
                case 101:
                    Log.d(VoiceInteractionSession.TAG, "onTaskFinished: intent=" + msg.obj + " taskId=" + msg.arg1);
                    VoiceInteractionSession.this.onTaskFinished((Intent) msg.obj, msg.arg1);
                    break;
                case 102:
                    Log.d(VoiceInteractionSession.TAG, "onCloseSystemDialogs");
                    VoiceInteractionSession.this.onCloseSystemDialogs();
                    break;
                case 103:
                    Log.d(VoiceInteractionSession.TAG, "doDestroy");
                    VoiceInteractionSession.this.doDestroy();
                    break;
            }
        }

        @Override
        public void onBackPressed() {
            VoiceInteractionSession.this.onBackPressed();
        }
    }

    public VoiceInteractionSession(Context context) {
        this(context, new Handler());
    }

    public VoiceInteractionSession(Context context, Handler handler) {
        this.mDispatcherState = new KeyEvent.DispatcherState();
        this.mTheme = 0;
        this.mActiveRequests = new ArrayMap<>();
        this.mTmpInsets = new Insets();
        this.mTmpLocation = new int[2];
        this.mWeakRef = new WeakReference<>(this);
        this.mInteractor = new IVoiceInteractor.Stub() {
            @Override
            public IVoiceInteractorRequest startConfirmation(String callingPackage, IVoiceInteractorCallback callback, CharSequence prompt, Bundle extras) {
                Request request = VoiceInteractionSession.this.newRequest(callback);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageOOOO(1, new Caller(callingPackage, Binder.getCallingUid()), request, prompt, extras));
                return request.mInterface;
            }

            @Override
            public IVoiceInteractorRequest startCompleteVoice(String callingPackage, IVoiceInteractorCallback callback, CharSequence message, Bundle extras) {
                Request request = VoiceInteractionSession.this.newRequest(callback);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageOOOO(2, new Caller(callingPackage, Binder.getCallingUid()), request, message, extras));
                return request.mInterface;
            }

            @Override
            public IVoiceInteractorRequest startAbortVoice(String callingPackage, IVoiceInteractorCallback callback, CharSequence message, Bundle extras) {
                Request request = VoiceInteractionSession.this.newRequest(callback);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageOOOO(3, new Caller(callingPackage, Binder.getCallingUid()), request, message, extras));
                return request.mInterface;
            }

            @Override
            public IVoiceInteractorRequest startCommand(String callingPackage, IVoiceInteractorCallback callback, String command, Bundle extras) {
                Request request = VoiceInteractionSession.this.newRequest(callback);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageOOOO(4, new Caller(callingPackage, Binder.getCallingUid()), request, command, extras));
                return request.mInterface;
            }

            @Override
            public boolean[] supportsCommands(String callingPackage, String[] commands) {
                Message msg = VoiceInteractionSession.this.mHandlerCaller.obtainMessageIOO(5, 0, new Caller(callingPackage, Binder.getCallingUid()), commands);
                SomeArgs args = VoiceInteractionSession.this.mHandlerCaller.sendMessageAndWait(msg);
                if (args != null) {
                    boolean[] res = (boolean[]) args.arg1;
                    args.recycle();
                    return res;
                }
                boolean[] res2 = new boolean[commands.length];
                return res2;
            }
        };
        this.mSession = new IVoiceInteractionSession.Stub() {
            @Override
            public void taskStarted(Intent intent, int taskId) {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageIO(100, taskId, intent));
            }

            @Override
            public void taskFinished(Intent intent, int taskId) {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageIO(101, taskId, intent));
            }

            @Override
            public void closeSystemDialogs() {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessage(102));
            }

            @Override
            public void destroy() {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessage(103));
            }
        };
        this.mCallbacks = new MyCallbacks();
        this.mInsetsComputer = new ViewTreeObserver.OnComputeInternalInsetsListener() {
            @Override
            public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo info) {
                VoiceInteractionSession.this.onComputeInsets(VoiceInteractionSession.this.mTmpInsets);
                info.contentInsets.set(VoiceInteractionSession.this.mTmpInsets.contentInsets);
                info.visibleInsets.set(VoiceInteractionSession.this.mTmpInsets.contentInsets);
                info.touchableRegion.set(VoiceInteractionSession.this.mTmpInsets.touchableRegion);
                info.setTouchableInsets(VoiceInteractionSession.this.mTmpInsets.touchableInsets);
            }
        };
        this.mContext = context;
        this.mHandlerCaller = new HandlerCaller(context, handler.getLooper(), this.mCallbacks, true);
    }

    Request newRequest(IVoiceInteractorCallback callback) {
        Request req;
        synchronized (this) {
            req = new Request(callback, this);
            this.mActiveRequests.put(req.mInterface.asBinder(), req);
        }
        return req;
    }

    Request removeRequest(IBinder reqInterface) {
        Request req;
        synchronized (this) {
            req = this.mActiveRequests.get(reqInterface);
            if (req != null) {
                this.mActiveRequests.remove(req);
            }
        }
        return req;
    }

    void doCreate(IVoiceInteractionManagerService service, IBinder token, Bundle args) {
        this.mSystemService = service;
        this.mToken = token;
        onCreate(args);
    }

    void doDestroy() {
        onDestroy();
        if (this.mInitialized) {
            this.mRootView.getViewTreeObserver().removeOnComputeInternalInsetsListener(this.mInsetsComputer);
            if (this.mWindowAdded) {
                this.mWindow.dismiss();
                this.mWindowAdded = false;
            }
            this.mInitialized = false;
        }
    }

    void initViews() {
        this.mInitialized = true;
        this.mThemeAttrs = this.mContext.obtainStyledAttributes(R.styleable.VoiceInteractionSession);
        this.mRootView = this.mInflater.inflate(com.android.internal.R.layout.voice_interaction_session, (ViewGroup) null);
        this.mRootView.setSystemUiVisibility(768);
        this.mWindow.setContentView(this.mRootView);
        this.mRootView.getViewTreeObserver().addOnComputeInternalInsetsListener(this.mInsetsComputer);
        this.mContentFrame = (FrameLayout) this.mRootView.findViewById(16908290);
    }

    public void showWindow() {
        Log.v(TAG, "Showing window: mWindowAdded=" + this.mWindowAdded + " mWindowVisible=" + this.mWindowVisible);
        if (this.mInShowWindow) {
            Log.w(TAG, "Re-entrance in to showWindow");
            return;
        }
        try {
            this.mInShowWindow = true;
            if (!this.mWindowVisible) {
                this.mWindowVisible = true;
                if (!this.mWindowAdded) {
                    this.mWindowAdded = true;
                    View v = onCreateContentView();
                    if (v != null) {
                        setContentView(v);
                    }
                }
                this.mWindow.show();
            }
        } finally {
            this.mWindowWasVisible = true;
            this.mInShowWindow = false;
        }
    }

    public void hideWindow() {
        if (this.mWindowVisible) {
            this.mWindow.hide();
            this.mWindowVisible = false;
        }
    }

    public void setTheme(int theme) {
        if (this.mWindow != null) {
            throw new IllegalStateException("Must be called before onCreate()");
        }
        this.mTheme = theme;
    }

    public void startVoiceActivity(Intent intent) {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            intent.migrateExtraStreamToClipData();
            intent.prepareToLeaveProcess();
            int res = this.mSystemService.startVoiceActivity(this.mToken, intent, intent.resolveType(this.mContext.getContentResolver()));
            Instrumentation.checkStartActivityResult(res, intent);
        } catch (RemoteException e) {
        }
    }

    public LayoutInflater getLayoutInflater() {
        return this.mInflater;
    }

    public Dialog getWindow() {
        return this.mWindow;
    }

    public void finish() {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        hideWindow();
        try {
            this.mSystemService.finish(this.mToken);
        } catch (RemoteException e) {
        }
    }

    public void onCreate(Bundle args) {
        this.mTheme = this.mTheme != 0 ? this.mTheme : com.android.internal.R.style.Theme_DeviceDefault_VoiceInteractionSession;
        this.mInflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mWindow = new SoftInputWindow(this.mContext, TAG, this.mTheme, this.mCallbacks, this, this.mDispatcherState, WindowManager.LayoutParams.TYPE_VOICE_INTERACTION, 48, true);
        this.mWindow.getWindow().addFlags(16777216);
        initViews();
        this.mWindow.getWindow().setLayout(-1, -2);
        this.mWindow.setToken(this.mToken);
    }

    public void onDestroy() {
    }

    public View onCreateContentView() {
        return null;
    }

    public void setContentView(View view) {
        this.mContentFrame.removeAllViews();
        this.mContentFrame.addView(view, new FrameLayout.LayoutParams(-1, -2));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        return false;
    }

    public void onBackPressed() {
        finish();
    }

    public void onCloseSystemDialogs() {
        finish();
    }

    public void onComputeInsets(Insets outInsets) {
        int[] loc = this.mTmpLocation;
        View decor = getWindow().getWindow().getDecorView();
        decor.getLocationInWindow(loc);
        outInsets.contentInsets.top = 0;
        outInsets.contentInsets.left = 0;
        outInsets.contentInsets.right = 0;
        outInsets.contentInsets.bottom = 0;
        outInsets.touchableInsets = 0;
        outInsets.touchableRegion.setEmpty();
    }

    public void onTaskStarted(Intent intent, int taskId) {
    }

    public void onTaskFinished(Intent intent, int taskId) {
        finish();
    }

    public boolean[] onGetSupportedCommands(Caller caller, String[] commands) {
        return new boolean[commands.length];
    }

    public void onCompleteVoice(Caller caller, Request request, CharSequence message, Bundle extras) {
        request.sendCompleteVoiceResult(null);
    }

    public void onAbortVoice(Caller caller, Request request, CharSequence message, Bundle extras) {
        request.sendAbortVoiceResult(null);
    }
}
