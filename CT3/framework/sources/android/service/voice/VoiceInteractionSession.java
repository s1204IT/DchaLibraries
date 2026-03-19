package android.service.voice;

import android.R;
import android.app.Dialog;
import android.app.Instrumentation;
import android.app.VoiceInteractor;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Region;
import android.inputmethodservice.SoftInputWindow;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArrayMap;
import android.util.DebugUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.app.IVoiceInteractorCallback;
import com.android.internal.app.IVoiceInteractorRequest;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;

public class VoiceInteractionSession implements KeyEvent.Callback, ComponentCallbacks2 {
    static final boolean DEBUG = false;
    public static final String KEY_CONTENT = "content";
    public static final String KEY_DATA = "data";
    public static final String KEY_RECEIVER_EXTRAS = "receiverExtras";
    public static final String KEY_STRUCTURE = "structure";
    static final int MSG_CANCEL = 7;
    static final int MSG_CLOSE_SYSTEM_DIALOGS = 102;
    static final int MSG_DESTROY = 103;
    static final int MSG_HANDLE_ASSIST = 104;
    static final int MSG_HANDLE_SCREENSHOT = 105;
    static final int MSG_HIDE = 107;
    static final int MSG_ON_LOCKSCREEN_SHOWN = 108;
    static final int MSG_SHOW = 106;
    static final int MSG_START_ABORT_VOICE = 4;
    static final int MSG_START_COMMAND = 5;
    static final int MSG_START_COMPLETE_VOICE = 3;
    static final int MSG_START_CONFIRMATION = 1;
    static final int MSG_START_PICK_OPTION = 2;
    static final int MSG_SUPPORTS_COMMANDS = 6;
    static final int MSG_TASK_FINISHED = 101;
    static final int MSG_TASK_STARTED = 100;
    public static final int SHOW_SOURCE_ACTIVITY = 16;
    public static final int SHOW_SOURCE_APPLICATION = 8;
    public static final int SHOW_SOURCE_ASSIST_GESTURE = 4;
    public static final int SHOW_WITH_ASSIST = 1;
    public static final int SHOW_WITH_SCREENSHOT = 2;
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

    public static class Request {
        final IVoiceInteractorCallback mCallback;
        final String mCallingPackage;
        final int mCallingUid;
        final Bundle mExtras;
        final IVoiceInteractorRequest mInterface = new IVoiceInteractorRequest.Stub() {
            public void cancel() throws RemoteException {
                VoiceInteractionSession session = Request.this.mSession.get();
                if (session == null) {
                    return;
                }
                session.mHandlerCaller.sendMessage(session.mHandlerCaller.obtainMessageO(7, Request.this));
            }
        };
        final WeakReference<VoiceInteractionSession> mSession;

        Request(String packageName, int uid, IVoiceInteractorCallback callback, VoiceInteractionSession session, Bundle extras) {
            this.mCallingPackage = packageName;
            this.mCallingUid = uid;
            this.mCallback = callback;
            this.mSession = session.mWeakRef;
            this.mExtras = extras;
        }

        public int getCallingUid() {
            return this.mCallingUid;
        }

        public String getCallingPackage() {
            return this.mCallingPackage;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }

        public boolean isActive() {
            VoiceInteractionSession session = this.mSession.get();
            if (session == null) {
                return false;
            }
            return session.isRequestActive(this.mInterface.asBinder());
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
            if (req == this) {
            } else {
                throw new IllegalStateException("Current active request " + req + " not same as calling request " + this);
            }
        }

        public void cancel() {
            try {
                finishRequest();
                this.mCallback.deliverCancel(this.mInterface);
            } catch (RemoteException e) {
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            DebugUtils.buildShortClassTag(this, sb);
            sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            sb.append(this.mInterface.asBinder());
            sb.append(" pkg=");
            sb.append(this.mCallingPackage);
            sb.append(" uid=");
            UserHandle.formatUid(sb, this.mCallingUid);
            sb.append('}');
            return sb.toString();
        }

        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            writer.print(prefix);
            writer.print("mInterface=");
            writer.println(this.mInterface.asBinder());
            writer.print(prefix);
            writer.print("mCallingPackage=");
            writer.print(this.mCallingPackage);
            writer.print(" mCallingUid=");
            UserHandle.formatUid(writer, this.mCallingUid);
            writer.println();
            writer.print(prefix);
            writer.print("mCallback=");
            writer.println(this.mCallback.asBinder());
            if (this.mExtras == null) {
                return;
            }
            writer.print(prefix);
            writer.print("mExtras=");
            writer.println(this.mExtras);
        }
    }

    public static final class ConfirmationRequest extends Request {
        final VoiceInteractor.Prompt mPrompt;

        ConfirmationRequest(String packageName, int uid, IVoiceInteractorCallback callback, VoiceInteractionSession session, VoiceInteractor.Prompt prompt, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            this.mPrompt = prompt;
        }

        public VoiceInteractor.Prompt getVoicePrompt() {
            return this.mPrompt;
        }

        public CharSequence getPrompt() {
            if (this.mPrompt != null) {
                return this.mPrompt.getVoicePromptAt(0);
            }
            return null;
        }

        public void sendConfirmationResult(boolean confirmed, Bundle result) {
            try {
                finishRequest();
                this.mCallback.deliverConfirmationResult(this.mInterface, confirmed, result);
            } catch (RemoteException e) {
            }
        }

        @Override
        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix);
            writer.print("mPrompt=");
            writer.println(this.mPrompt);
        }
    }

    public static final class PickOptionRequest extends Request {
        final VoiceInteractor.PickOptionRequest.Option[] mOptions;
        final VoiceInteractor.Prompt mPrompt;

        PickOptionRequest(String packageName, int uid, IVoiceInteractorCallback callback, VoiceInteractionSession session, VoiceInteractor.Prompt prompt, VoiceInteractor.PickOptionRequest.Option[] options, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            this.mPrompt = prompt;
            this.mOptions = options;
        }

        public VoiceInteractor.Prompt getVoicePrompt() {
            return this.mPrompt;
        }

        public CharSequence getPrompt() {
            if (this.mPrompt != null) {
                return this.mPrompt.getVoicePromptAt(0);
            }
            return null;
        }

        public VoiceInteractor.PickOptionRequest.Option[] getOptions() {
            return this.mOptions;
        }

        void sendPickOptionResult(boolean finished, VoiceInteractor.PickOptionRequest.Option[] selections, Bundle result) {
            if (finished) {
                try {
                    finishRequest();
                } catch (RemoteException e) {
                    return;
                }
            }
            this.mCallback.deliverPickOptionResult(this.mInterface, finished, selections, result);
        }

        public void sendIntermediatePickOptionResult(VoiceInteractor.PickOptionRequest.Option[] selections, Bundle result) {
            sendPickOptionResult(false, selections, result);
        }

        public void sendPickOptionResult(VoiceInteractor.PickOptionRequest.Option[] selections, Bundle result) {
            sendPickOptionResult(true, selections, result);
        }

        @Override
        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix);
            writer.print("mPrompt=");
            writer.println(this.mPrompt);
            if (this.mOptions == null) {
                return;
            }
            writer.print(prefix);
            writer.println("Options:");
            for (int i = 0; i < this.mOptions.length; i++) {
                VoiceInteractor.PickOptionRequest.Option op = this.mOptions[i];
                writer.print(prefix);
                writer.print("  #");
                writer.print(i);
                writer.println(":");
                writer.print(prefix);
                writer.print("    mLabel=");
                writer.println(op.getLabel());
                writer.print(prefix);
                writer.print("    mIndex=");
                writer.println(op.getIndex());
                if (op.countSynonyms() > 0) {
                    writer.print(prefix);
                    writer.println("    Synonyms:");
                    for (int j = 0; j < op.countSynonyms(); j++) {
                        writer.print(prefix);
                        writer.print("      #");
                        writer.print(j);
                        writer.print(": ");
                        writer.println(op.getSynonymAt(j));
                    }
                }
                if (op.getExtras() != null) {
                    writer.print(prefix);
                    writer.print("    mExtras=");
                    writer.println(op.getExtras());
                }
            }
        }
    }

    public static final class CompleteVoiceRequest extends Request {
        final VoiceInteractor.Prompt mPrompt;

        CompleteVoiceRequest(String packageName, int uid, IVoiceInteractorCallback callback, VoiceInteractionSession session, VoiceInteractor.Prompt prompt, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            this.mPrompt = prompt;
        }

        public VoiceInteractor.Prompt getVoicePrompt() {
            return this.mPrompt;
        }

        public CharSequence getMessage() {
            if (this.mPrompt != null) {
                return this.mPrompt.getVoicePromptAt(0);
            }
            return null;
        }

        public void sendCompleteResult(Bundle result) {
            try {
                finishRequest();
                this.mCallback.deliverCompleteVoiceResult(this.mInterface, result);
            } catch (RemoteException e) {
            }
        }

        @Override
        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix);
            writer.print("mPrompt=");
            writer.println(this.mPrompt);
        }
    }

    public static final class AbortVoiceRequest extends Request {
        final VoiceInteractor.Prompt mPrompt;

        AbortVoiceRequest(String packageName, int uid, IVoiceInteractorCallback callback, VoiceInteractionSession session, VoiceInteractor.Prompt prompt, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            this.mPrompt = prompt;
        }

        public VoiceInteractor.Prompt getVoicePrompt() {
            return this.mPrompt;
        }

        public CharSequence getMessage() {
            if (this.mPrompt != null) {
                return this.mPrompt.getVoicePromptAt(0);
            }
            return null;
        }

        public void sendAbortResult(Bundle result) {
            try {
                finishRequest();
                this.mCallback.deliverAbortVoiceResult(this.mInterface, result);
            } catch (RemoteException e) {
            }
        }

        @Override
        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix);
            writer.print("mPrompt=");
            writer.println(this.mPrompt);
        }
    }

    public static final class CommandRequest extends Request {
        final String mCommand;

        CommandRequest(String packageName, int uid, IVoiceInteractorCallback callback, VoiceInteractionSession session, String command, Bundle extras) {
            super(packageName, uid, callback, session, extras);
            this.mCommand = command;
        }

        public String getCommand() {
            return this.mCommand;
        }

        void sendCommandResult(boolean finished, Bundle result) {
            if (finished) {
                try {
                    finishRequest();
                } catch (RemoteException e) {
                    return;
                }
            }
            this.mCallback.deliverCommandResult(this.mInterface, finished, result);
        }

        public void sendIntermediateResult(Bundle result) {
            sendCommandResult(false, result);
        }

        public void sendResult(Bundle result) {
            sendCommandResult(true, result);
        }

        @Override
        void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            super.dump(prefix, fd, writer, args);
            writer.print(prefix);
            writer.print("mCommand=");
            writer.println(this.mCommand);
        }
    }

    class MyCallbacks implements HandlerCaller.Callback, SoftInputWindow.Callback {
        MyCallbacks() {
        }

        public void executeMessage(Message msg) {
            SomeArgs args = null;
            switch (msg.what) {
                case 1:
                    VoiceInteractionSession.this.onRequestConfirmation((ConfirmationRequest) msg.obj);
                    break;
                case 2:
                    VoiceInteractionSession.this.onRequestPickOption((PickOptionRequest) msg.obj);
                    break;
                case 3:
                    VoiceInteractionSession.this.onRequestCompleteVoice((CompleteVoiceRequest) msg.obj);
                    break;
                case 4:
                    VoiceInteractionSession.this.onRequestAbortVoice((AbortVoiceRequest) msg.obj);
                    break;
                case 5:
                    VoiceInteractionSession.this.onRequestCommand((CommandRequest) msg.obj);
                    break;
                case 6:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    args2.arg1 = VoiceInteractionSession.this.onGetSupportedCommands((String[]) args2.arg1);
                    args2.complete();
                    args = null;
                    break;
                case 7:
                    VoiceInteractionSession.this.onCancelRequest((Request) msg.obj);
                    break;
                case 100:
                    VoiceInteractionSession.this.onTaskStarted((Intent) msg.obj, msg.arg1);
                    break;
                case 101:
                    VoiceInteractionSession.this.onTaskFinished((Intent) msg.obj, msg.arg1);
                    break;
                case 102:
                    VoiceInteractionSession.this.onCloseSystemDialogs();
                    break;
                case 103:
                    VoiceInteractionSession.this.doDestroy();
                    break;
                case 104:
                    args = (SomeArgs) msg.obj;
                    if (args.argi5 == 0) {
                        VoiceInteractionSession.this.doOnHandleAssist((Bundle) args.arg1, (AssistStructure) args.arg2, (Throwable) args.arg3, (AssistContent) args.arg4);
                    } else {
                        VoiceInteractionSession.this.doOnHandleAssistSecondary((Bundle) args.arg1, (AssistStructure) args.arg2, (Throwable) args.arg3, (AssistContent) args.arg4, args.argi5, args.argi6);
                    }
                    break;
                case 105:
                    VoiceInteractionSession.this.onHandleScreenshot((Bitmap) msg.obj);
                    break;
                case 106:
                    args = (SomeArgs) msg.obj;
                    VoiceInteractionSession.this.doShow((Bundle) args.arg1, msg.arg1, (IVoiceInteractionSessionShowCallback) args.arg2);
                    break;
                case 107:
                    VoiceInteractionSession.this.doHide();
                    break;
                case 108:
                    VoiceInteractionSession.this.onLockscreenShown();
                    break;
            }
            if (args == null) {
                return;
            }
            args.recycle();
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
        this.mWeakRef = new WeakReference<>(this);
        this.mInteractor = new IVoiceInteractor.Stub() {
            public IVoiceInteractorRequest startConfirmation(String callingPackage, IVoiceInteractorCallback callback, VoiceInteractor.Prompt prompt, Bundle extras) {
                ConfirmationRequest request = new ConfirmationRequest(callingPackage, Binder.getCallingUid(), callback, VoiceInteractionSession.this, prompt, extras);
                VoiceInteractionSession.this.addRequest(request);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageO(1, request));
                return request.mInterface;
            }

            public IVoiceInteractorRequest startPickOption(String callingPackage, IVoiceInteractorCallback callback, VoiceInteractor.Prompt prompt, VoiceInteractor.PickOptionRequest.Option[] options, Bundle extras) {
                PickOptionRequest request = new PickOptionRequest(callingPackage, Binder.getCallingUid(), callback, VoiceInteractionSession.this, prompt, options, extras);
                VoiceInteractionSession.this.addRequest(request);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageO(2, request));
                return request.mInterface;
            }

            public IVoiceInteractorRequest startCompleteVoice(String callingPackage, IVoiceInteractorCallback callback, VoiceInteractor.Prompt message, Bundle extras) {
                CompleteVoiceRequest request = new CompleteVoiceRequest(callingPackage, Binder.getCallingUid(), callback, VoiceInteractionSession.this, message, extras);
                VoiceInteractionSession.this.addRequest(request);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageO(3, request));
                return request.mInterface;
            }

            public IVoiceInteractorRequest startAbortVoice(String callingPackage, IVoiceInteractorCallback callback, VoiceInteractor.Prompt message, Bundle extras) {
                AbortVoiceRequest request = new AbortVoiceRequest(callingPackage, Binder.getCallingUid(), callback, VoiceInteractionSession.this, message, extras);
                VoiceInteractionSession.this.addRequest(request);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageO(4, request));
                return request.mInterface;
            }

            public IVoiceInteractorRequest startCommand(String callingPackage, IVoiceInteractorCallback callback, String command, Bundle extras) {
                CommandRequest request = new CommandRequest(callingPackage, Binder.getCallingUid(), callback, VoiceInteractionSession.this, command, extras);
                VoiceInteractionSession.this.addRequest(request);
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageO(5, request));
                return request.mInterface;
            }

            public boolean[] supportsCommands(String callingPackage, String[] commands) {
                Message msg = VoiceInteractionSession.this.mHandlerCaller.obtainMessageIOO(6, 0, commands, (Object) null);
                SomeArgs args = VoiceInteractionSession.this.mHandlerCaller.sendMessageAndWait(msg);
                if (args != null) {
                    boolean[] res = (boolean[]) args.arg1;
                    args.recycle();
                    return res;
                }
                return new boolean[commands.length];
            }
        };
        this.mSession = new IVoiceInteractionSession.Stub() {
            @Override
            public void show(Bundle sessionArgs, int flags, IVoiceInteractionSessionShowCallback showCallback) {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageIOO(106, flags, sessionArgs, showCallback));
            }

            @Override
            public void hide() {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessage(107));
            }

            @Override
            public void handleAssist(final Bundle data, final AssistStructure structure, final AssistContent content, final int index, final int count) {
                Thread retriever = new Thread("AssistStructure retriever") {
                    @Override
                    public void run() {
                        Throwable failure = null;
                        if (structure != null) {
                            try {
                                structure.ensureData();
                            } catch (Throwable e) {
                                Log.w(VoiceInteractionSession.TAG, "Failure retrieving AssistStructure", e);
                                failure = e;
                            }
                        }
                        VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageOOOOII(104, data, failure == null ? structure : null, failure, content, index, count));
                    }
                };
                retriever.start();
            }

            @Override
            public void handleScreenshot(Bitmap screenshot) {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessageO(105, screenshot));
            }

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
            public void onLockscreenShown() {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessage(108));
            }

            @Override
            public void destroy() {
                VoiceInteractionSession.this.mHandlerCaller.sendMessage(VoiceInteractionSession.this.mHandlerCaller.obtainMessage(103));
            }
        };
        this.mCallbacks = new MyCallbacks();
        this.mInsetsComputer = new ViewTreeObserver.OnComputeInternalInsetsListener() {
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

    public Context getContext() {
        return this.mContext;
    }

    void addRequest(Request req) {
        synchronized (this) {
            this.mActiveRequests.put(req.mInterface.asBinder(), req);
        }
    }

    boolean isRequestActive(IBinder reqInterface) {
        boolean zContainsKey;
        synchronized (this) {
            zContainsKey = this.mActiveRequests.containsKey(reqInterface);
        }
        return zContainsKey;
    }

    Request removeRequest(IBinder reqInterface) {
        Request requestRemove;
        synchronized (this) {
            requestRemove = this.mActiveRequests.remove(reqInterface);
        }
        return requestRemove;
    }

    void doCreate(IVoiceInteractionManagerService service, IBinder token) {
        this.mSystemService = service;
        this.mToken = token;
        onCreate();
    }

    void doShow(Bundle args, int flags, final IVoiceInteractionSessionShowCallback showCallback) {
        if (this.mInShowWindow) {
            Log.w(TAG, "Re-entrance in to showWindow");
            return;
        }
        try {
            this.mInShowWindow = true;
            if (!this.mWindowVisible && !this.mWindowAdded) {
                this.mWindowAdded = true;
                View v = onCreateContentView();
                if (v != null) {
                    setContentView(v);
                }
            }
            onShow(args, flags);
            if (!this.mWindowVisible) {
                this.mWindowVisible = true;
                this.mWindow.show();
            }
            if (showCallback != null) {
                this.mRootView.invalidate();
                this.mRootView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        VoiceInteractionSession.this.mRootView.getViewTreeObserver().removeOnPreDrawListener(this);
                        try {
                            showCallback.onShown();
                            return true;
                        } catch (RemoteException e) {
                            Log.w(VoiceInteractionSession.TAG, "Error calling onShown", e);
                            return true;
                        }
                    }
                });
            }
        } finally {
            this.mWindowWasVisible = true;
            this.mInShowWindow = false;
        }
    }

    void doHide() {
        if (!this.mWindowVisible) {
            return;
        }
        this.mWindow.hide();
        this.mWindowVisible = false;
        onHide();
    }

    void doDestroy() {
        onDestroy();
        if (!this.mInitialized) {
            return;
        }
        this.mRootView.getViewTreeObserver().removeOnComputeInternalInsetsListener(this.mInsetsComputer);
        if (this.mWindowAdded) {
            this.mWindow.dismiss();
            this.mWindowAdded = false;
        }
        this.mInitialized = false;
    }

    void initViews() {
        this.mInitialized = true;
        this.mThemeAttrs = this.mContext.obtainStyledAttributes(R.styleable.VoiceInteractionSession);
        this.mRootView = this.mInflater.inflate(17367301, (ViewGroup) null);
        this.mRootView.setSystemUiVisibility(1792);
        this.mWindow.setContentView(this.mRootView);
        this.mRootView.getViewTreeObserver().addOnComputeInternalInsetsListener(this.mInsetsComputer);
        this.mContentFrame = (FrameLayout) this.mRootView.findViewById(R.id.content);
    }

    public void setDisabledShowContext(int flags) {
        try {
            this.mSystemService.setDisabledShowContext(flags);
        } catch (RemoteException e) {
        }
    }

    public int getDisabledShowContext() {
        try {
            return this.mSystemService.getDisabledShowContext();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int getUserDisabledShowContext() {
        try {
            return this.mSystemService.getUserDisabledShowContext();
        } catch (RemoteException e) {
            return 0;
        }
    }

    public void show(Bundle args, int flags) {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            this.mSystemService.showSessionFromSession(this.mToken, args, flags);
        } catch (RemoteException e) {
        }
    }

    public void hide() {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            this.mSystemService.hideSessionFromSession(this.mToken);
        } catch (RemoteException e) {
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
            intent.prepareToLeaveProcess(this.mContext);
            int res = this.mSystemService.startVoiceActivity(this.mToken, intent, intent.resolveType(this.mContext.getContentResolver()));
            Instrumentation.checkStartActivityResult(res, intent);
        } catch (RemoteException e) {
        }
    }

    public void setKeepAwake(boolean keepAwake) {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            this.mSystemService.setKeepAwake(this.mToken, keepAwake);
        } catch (RemoteException e) {
        }
    }

    public void closeSystemDialogs() {
        if (this.mToken == null) {
            throw new IllegalStateException("Can't call before onCreate()");
        }
        try {
            this.mSystemService.closeSystemDialogs(this.mToken);
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
        try {
            this.mSystemService.finish(this.mToken);
        } catch (RemoteException e) {
        }
    }

    public void onCreate() {
        doOnCreate();
    }

    private void doOnCreate() {
        this.mTheme = this.mTheme != 0 ? this.mTheme : 16974990;
        this.mInflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mWindow = new SoftInputWindow(this.mContext, TAG, this.mTheme, this.mCallbacks, this, this.mDispatcherState, 2031, 80, true);
        this.mWindow.getWindow().addFlags(R.attr.transcriptMode);
        initViews();
        this.mWindow.getWindow().setLayout(-1, -1);
        this.mWindow.setToken(this.mToken);
    }

    public void onShow(Bundle args, int showFlags) {
    }

    public void onHide() {
    }

    public void onDestroy() {
    }

    public View onCreateContentView() {
        return null;
    }

    public void setContentView(View view) {
        this.mContentFrame.removeAllViews();
        this.mContentFrame.addView(view, new FrameLayout.LayoutParams(-1, -1));
        this.mContentFrame.requestApplyInsets();
    }

    void doOnHandleAssist(Bundle data, AssistStructure structure, Throwable failure, AssistContent content) {
        if (failure != null) {
            onAssistStructureFailure(failure);
        }
        onHandleAssist(data, structure, content);
    }

    void doOnHandleAssistSecondary(Bundle data, AssistStructure structure, Throwable failure, AssistContent content, int index, int count) {
        if (failure != null) {
            onAssistStructureFailure(failure);
        }
        onHandleAssistSecondary(data, structure, content, index, count);
    }

    public void onAssistStructureFailure(Throwable failure) {
    }

    public void onHandleAssist(Bundle data, AssistStructure structure, AssistContent content) {
    }

    public void onHandleAssistSecondary(Bundle data, AssistStructure structure, AssistContent content, int index, int count) {
    }

    public void onHandleScreenshot(Bitmap screenshot) {
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
        hide();
    }

    public void onCloseSystemDialogs() {
        hide();
    }

    public void onLockscreenShown() {
        hide();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(int level) {
    }

    public void onComputeInsets(Insets outInsets) {
        outInsets.contentInsets.left = 0;
        outInsets.contentInsets.bottom = 0;
        outInsets.contentInsets.right = 0;
        View decor = getWindow().getWindow().getDecorView();
        outInsets.contentInsets.top = decor.getHeight();
        outInsets.touchableInsets = 0;
        outInsets.touchableRegion.setEmpty();
    }

    public void onTaskStarted(Intent intent, int taskId) {
    }

    public void onTaskFinished(Intent intent, int taskId) {
        hide();
    }

    public boolean[] onGetSupportedCommands(String[] commands) {
        return new boolean[commands.length];
    }

    public void onRequestConfirmation(ConfirmationRequest request) {
    }

    public void onRequestPickOption(PickOptionRequest request) {
    }

    public void onRequestCompleteVoice(CompleteVoiceRequest request) {
    }

    public void onRequestAbortVoice(AbortVoiceRequest request) {
    }

    public void onRequestCommand(CommandRequest request) {
    }

    public void onCancelRequest(Request request) {
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.print(prefix);
        writer.print("mToken=");
        writer.println(this.mToken);
        writer.print(prefix);
        writer.print("mTheme=#");
        writer.println(Integer.toHexString(this.mTheme));
        writer.print(prefix);
        writer.print("mInitialized=");
        writer.println(this.mInitialized);
        writer.print(prefix);
        writer.print("mWindowAdded=");
        writer.print(this.mWindowAdded);
        writer.print(" mWindowVisible=");
        writer.println(this.mWindowVisible);
        writer.print(prefix);
        writer.print("mWindowWasVisible=");
        writer.print(this.mWindowWasVisible);
        writer.print(" mInShowWindow=");
        writer.println(this.mInShowWindow);
        if (this.mActiveRequests.size() <= 0) {
            return;
        }
        writer.print(prefix);
        writer.println("Active requests:");
        String innerPrefix = prefix + "    ";
        for (int i = 0; i < this.mActiveRequests.size(); i++) {
            Request req = this.mActiveRequests.valueAt(i);
            writer.print(prefix);
            writer.print("  #");
            writer.print(i);
            writer.print(": ");
            writer.println(req);
            req.dump(innerPrefix, fd, writer, args);
        }
    }
}
