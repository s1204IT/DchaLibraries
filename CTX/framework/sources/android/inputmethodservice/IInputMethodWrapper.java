package android.inputmethodservice;

import android.Manifest;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.InputChannel;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodSession;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethod;
import com.android.internal.view.IInputMethodSession;
import com.android.internal.view.IInputSessionCallback;
import com.android.internal.view.InputConnectionWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class IInputMethodWrapper extends IInputMethod.Stub implements HandlerCaller.Callback {
    private static final int DO_ATTACH_TOKEN = 10;
    private static final int DO_CHANGE_INPUTMETHOD_SUBTYPE = 80;
    private static final int DO_CREATE_SESSION = 40;
    private static final int DO_DUMP = 1;
    private static final int DO_HIDE_SOFT_INPUT = 70;
    private static final int DO_REVOKE_SESSION = 50;
    private static final int DO_SET_INPUT_CONTEXT = 20;
    private static final int DO_SET_SESSION_ENABLED = 45;
    private static final int DO_SHOW_SOFT_INPUT = 60;
    private static final int DO_START_INPUT = 32;
    private static final int DO_UNSET_INPUT_CONTEXT = 30;
    private static final String TAG = "InputMethodWrapper";
    final HandlerCaller mCaller;
    final Context mContext;
    final WeakReference<InputMethod> mInputMethod;
    AtomicBoolean mIsUnbindIssued = null;
    final WeakReference<AbstractInputMethodService> mTarget;
    final int mTargetSdkVersion;

    static final class InputMethodSessionCallbackWrapper implements InputMethod.SessionCallback {
        final IInputSessionCallback mCb;
        final InputChannel mChannel;
        final Context mContext;

        InputMethodSessionCallbackWrapper(Context context, InputChannel inputChannel, IInputSessionCallback iInputSessionCallback) {
            this.mContext = context;
            this.mChannel = inputChannel;
            this.mCb = iInputSessionCallback;
        }

        @Override
        public void sessionCreated(InputMethodSession inputMethodSession) {
            try {
                if (inputMethodSession != null) {
                    this.mCb.sessionCreated(new IInputMethodSessionWrapper(this.mContext, inputMethodSession, this.mChannel));
                } else {
                    if (this.mChannel != null) {
                        this.mChannel.dispose();
                    }
                    this.mCb.sessionCreated(null);
                }
            } catch (RemoteException e) {
            }
        }
    }

    public IInputMethodWrapper(AbstractInputMethodService abstractInputMethodService, InputMethod inputMethod) {
        this.mTarget = new WeakReference<>(abstractInputMethodService);
        this.mContext = abstractInputMethodService.getApplicationContext();
        this.mCaller = new HandlerCaller(this.mContext, null, this, true);
        this.mInputMethod = new WeakReference<>(inputMethod);
        this.mTargetSdkVersion = abstractInputMethodService.getApplicationInfo().targetSdkVersion;
    }

    @Override
    public void executeMessage(Message message) {
        InputConnectionWrapper inputConnectionWrapper;
        InputMethod inputMethod = this.mInputMethod.get();
        if (inputMethod == null && message.what != 1) {
            Log.w(TAG, "Input method reference was null, ignoring message: " + message.what);
            return;
        }
        switch (message.what) {
            case 1:
                AbstractInputMethodService abstractInputMethodService = this.mTarget.get();
                if (abstractInputMethodService == null) {
                    return;
                }
                SomeArgs someArgs = (SomeArgs) message.obj;
                try {
                    abstractInputMethodService.dump((FileDescriptor) someArgs.arg1, (PrintWriter) someArgs.arg2, (String[]) someArgs.arg3);
                    break;
                } catch (RuntimeException e) {
                    ((PrintWriter) someArgs.arg2).println("Exception: " + e);
                }
                synchronized (someArgs.arg4) {
                    ((CountDownLatch) someArgs.arg4).countDown();
                    break;
                }
                someArgs.recycle();
                return;
            case 10:
                inputMethod.attachToken((IBinder) message.obj);
                return;
            case 20:
                inputMethod.bindInput((InputBinding) message.obj);
                return;
            case 30:
                inputMethod.unbindInput();
                return;
            case 32:
                SomeArgs someArgs2 = (SomeArgs) message.obj;
                int i = message.arg1;
                if (message.arg2 == 0) {
                    z = false;
                }
                IBinder iBinder = (IBinder) someArgs2.arg1;
                IInputContext iInputContext = (IInputContext) someArgs2.arg2;
                EditorInfo editorInfo = (EditorInfo) someArgs2.arg3;
                AtomicBoolean atomicBoolean = (AtomicBoolean) someArgs2.arg4;
                if (iInputContext != null) {
                    inputConnectionWrapper = new InputConnectionWrapper(this.mTarget, iInputContext, i, atomicBoolean);
                } else {
                    inputConnectionWrapper = null;
                }
                editorInfo.makeCompatible(this.mTargetSdkVersion);
                inputMethod.dispatchStartInputWithToken(inputConnectionWrapper, editorInfo, z, iBinder);
                someArgs2.recycle();
                return;
            case 40:
                SomeArgs someArgs3 = (SomeArgs) message.obj;
                inputMethod.createSession(new InputMethodSessionCallbackWrapper(this.mContext, (InputChannel) someArgs3.arg1, (IInputSessionCallback) someArgs3.arg2));
                someArgs3.recycle();
                return;
            case 45:
                inputMethod.setSessionEnabled((InputMethodSession) message.obj, message.arg1 != 0);
                return;
            case 50:
                inputMethod.revokeSession((InputMethodSession) message.obj);
                return;
            case 60:
                inputMethod.showSoftInput(message.arg1, (ResultReceiver) message.obj);
                return;
            case 70:
                inputMethod.hideSoftInput(message.arg1, (ResultReceiver) message.obj);
                return;
            case 80:
                inputMethod.changeInputMethodSubtype((InputMethodSubtype) message.obj);
                return;
            default:
                Log.w(TAG, "Unhandled message code: " + message.what);
                return;
        }
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        AbstractInputMethodService abstractInputMethodService = this.mTarget.get();
        if (abstractInputMethodService == null) {
            return;
        }
        if (abstractInputMethodService.checkCallingOrSelfPermission(Manifest.permission.DUMP) != 0) {
            printWriter.println("Permission Denial: can't dump InputMethodManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageOOOO(1, fileDescriptor, printWriter, strArr, countDownLatch));
        try {
            if (!countDownLatch.await(5L, TimeUnit.SECONDS)) {
                printWriter.println("Timeout waiting for dump");
            }
        } catch (InterruptedException e) {
            printWriter.println("Interrupted waiting for dump");
        }
    }

    @Override
    public void attachToken(IBinder iBinder) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(10, iBinder));
    }

    @Override
    public void bindInput(InputBinding inputBinding) {
        if (this.mIsUnbindIssued != null) {
            Log.e(TAG, "bindInput must be paired with unbindInput.");
        }
        this.mIsUnbindIssued = new AtomicBoolean();
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(20, new InputBinding(new InputConnectionWrapper(this.mTarget, IInputContext.Stub.asInterface(inputBinding.getConnectionToken()), 0, this.mIsUnbindIssued), inputBinding)));
    }

    @Override
    public void unbindInput() {
        if (this.mIsUnbindIssued != null) {
            this.mIsUnbindIssued.set(true);
            this.mIsUnbindIssued = null;
        } else {
            Log.e(TAG, "unbindInput must be paired with bindInput.");
        }
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessage(30));
    }

    @Override
    public void startInput(IBinder iBinder, IInputContext iInputContext, int i, EditorInfo editorInfo, boolean z) {
        if (this.mIsUnbindIssued == null) {
            Log.e(TAG, "startInput must be called after bindInput.");
            this.mIsUnbindIssued = new AtomicBoolean();
        }
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageIIOOOO(32, i, z ? 1 : 0, iBinder, iInputContext, editorInfo, this.mIsUnbindIssued));
    }

    @Override
    public void createSession(InputChannel inputChannel, IInputSessionCallback iInputSessionCallback) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageOO(40, inputChannel, iInputSessionCallback));
    }

    @Override
    public void setSessionEnabled(IInputMethodSession iInputMethodSession, boolean z) {
        try {
            InputMethodSession internalInputMethodSession = ((IInputMethodSessionWrapper) iInputMethodSession).getInternalInputMethodSession();
            if (internalInputMethodSession == null) {
                Log.w(TAG, "Session is already finished: " + iInputMethodSession);
                return;
            }
            this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageIO(45, z ? 1 : 0, internalInputMethodSession));
        } catch (ClassCastException e) {
            Log.w(TAG, "Incoming session not of correct type: " + iInputMethodSession, e);
        }
    }

    @Override
    public void revokeSession(IInputMethodSession iInputMethodSession) {
        try {
            InputMethodSession internalInputMethodSession = ((IInputMethodSessionWrapper) iInputMethodSession).getInternalInputMethodSession();
            if (internalInputMethodSession == null) {
                Log.w(TAG, "Session is already finished: " + iInputMethodSession);
                return;
            }
            this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(50, internalInputMethodSession));
        } catch (ClassCastException e) {
            Log.w(TAG, "Incoming session not of correct type: " + iInputMethodSession, e);
        }
    }

    @Override
    public void showSoftInput(int i, ResultReceiver resultReceiver) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageIO(60, i, resultReceiver));
    }

    @Override
    public void hideSoftInput(int i, ResultReceiver resultReceiver) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageIO(70, i, resultReceiver));
    }

    @Override
    public void changeInputMethodSubtype(InputMethodSubtype inputMethodSubtype) {
        this.mCaller.executeOrSendMessage(this.mCaller.obtainMessageO(80, inputMethodSubtype));
    }
}
