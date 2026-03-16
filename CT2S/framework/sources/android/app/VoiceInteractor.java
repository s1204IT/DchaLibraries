package android.app;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.app.IVoiceInteractorCallback;
import com.android.internal.app.IVoiceInteractorRequest;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;
import java.util.ArrayList;

public class VoiceInteractor {
    static final boolean DEBUG = true;
    static final int MSG_ABORT_VOICE_RESULT = 3;
    static final int MSG_CANCEL_RESULT = 5;
    static final int MSG_COMMAND_RESULT = 4;
    static final int MSG_COMPLETE_VOICE_RESULT = 2;
    static final int MSG_CONFIRMATION_RESULT = 1;
    static final String TAG = "VoiceInteractor";
    Activity mActivity;
    Context mContext;
    final HandlerCaller mHandlerCaller;
    final IVoiceInteractor mInteractor;
    final HandlerCaller.Callback mHandlerCallerCallback = new HandlerCaller.Callback() {
        @Override
        public void executeMessage(Message msg) {
            SomeArgs args = (SomeArgs) msg.obj;
            switch (msg.what) {
                case 1:
                    Request request = VoiceInteractor.this.pullRequest((IVoiceInteractorRequest) args.arg1, true);
                    Log.d(VoiceInteractor.TAG, "onConfirmResult: req=" + ((IVoiceInteractorRequest) args.arg1).asBinder() + "/" + request + " confirmed=" + msg.arg1 + " result=" + args.arg2);
                    if (request != null) {
                        ((ConfirmationRequest) request).onConfirmationResult(msg.arg1 != 0, (Bundle) args.arg2);
                        request.clear();
                    }
                    break;
                case 2:
                    Request request2 = VoiceInteractor.this.pullRequest((IVoiceInteractorRequest) args.arg1, true);
                    Log.d(VoiceInteractor.TAG, "onCompleteVoice: req=" + ((IVoiceInteractorRequest) args.arg1).asBinder() + "/" + request2 + " result=" + args.arg1);
                    if (request2 != null) {
                        ((CompleteVoiceRequest) request2).onCompleteResult((Bundle) args.arg2);
                        request2.clear();
                    }
                    break;
                case 3:
                    Request request3 = VoiceInteractor.this.pullRequest((IVoiceInteractorRequest) args.arg1, true);
                    Log.d(VoiceInteractor.TAG, "onAbortVoice: req=" + ((IVoiceInteractorRequest) args.arg1).asBinder() + "/" + request3 + " result=" + args.arg1);
                    if (request3 != null) {
                        ((AbortVoiceRequest) request3).onAbortResult((Bundle) args.arg2);
                        request3.clear();
                    }
                    break;
                case 4:
                    Request request4 = VoiceInteractor.this.pullRequest((IVoiceInteractorRequest) args.arg1, msg.arg1 != 0);
                    Log.d(VoiceInteractor.TAG, "onCommandResult: req=" + ((IVoiceInteractorRequest) args.arg1).asBinder() + "/" + request4 + " result=" + args.arg2);
                    if (request4 != null) {
                        ((CommandRequest) request4).onCommandResult((Bundle) args.arg2);
                        if (msg.arg1 != 0) {
                            request4.clear();
                        }
                    }
                    break;
                case 5:
                    Request request5 = VoiceInteractor.this.pullRequest((IVoiceInteractorRequest) args.arg1, true);
                    Log.d(VoiceInteractor.TAG, "onCancelResult: req=" + ((IVoiceInteractorRequest) args.arg1).asBinder() + "/" + request5);
                    if (request5 != null) {
                        request5.onCancel();
                        request5.clear();
                    }
                    break;
            }
        }
    };
    final IVoiceInteractorCallback.Stub mCallback = new IVoiceInteractorCallback.Stub() {
        @Override
        public void deliverConfirmationResult(IVoiceInteractorRequest request, boolean confirmed, Bundle result) {
            VoiceInteractor.this.mHandlerCaller.sendMessage(VoiceInteractor.this.mHandlerCaller.obtainMessageIOO(1, confirmed ? 1 : 0, request, result));
        }

        @Override
        public void deliverCompleteVoiceResult(IVoiceInteractorRequest request, Bundle result) {
            VoiceInteractor.this.mHandlerCaller.sendMessage(VoiceInteractor.this.mHandlerCaller.obtainMessageOO(2, request, result));
        }

        @Override
        public void deliverAbortVoiceResult(IVoiceInteractorRequest request, Bundle result) {
            VoiceInteractor.this.mHandlerCaller.sendMessage(VoiceInteractor.this.mHandlerCaller.obtainMessageOO(3, request, result));
        }

        @Override
        public void deliverCommandResult(IVoiceInteractorRequest request, boolean complete, Bundle result) {
            VoiceInteractor.this.mHandlerCaller.sendMessage(VoiceInteractor.this.mHandlerCaller.obtainMessageIOO(4, complete ? 1 : 0, request, result));
        }

        @Override
        public void deliverCancel(IVoiceInteractorRequest request) throws RemoteException {
            VoiceInteractor.this.mHandlerCaller.sendMessage(VoiceInteractor.this.mHandlerCaller.obtainMessageO(5, request));
        }
    };
    final ArrayMap<IBinder, Request> mActiveRequests = new ArrayMap<>();

    public static abstract class Request {
        Activity mActivity;
        Context mContext;
        IVoiceInteractorRequest mRequestInterface;

        abstract IVoiceInteractorRequest submit(IVoiceInteractor iVoiceInteractor, String str, IVoiceInteractorCallback iVoiceInteractorCallback) throws RemoteException;

        public void cancel() {
            try {
                this.mRequestInterface.cancel();
            } catch (RemoteException e) {
                Log.w(VoiceInteractor.TAG, "Voice interactor has died", e);
            }
        }

        public Context getContext() {
            return this.mContext;
        }

        public Activity getActivity() {
            return this.mActivity;
        }

        public void onCancel() {
        }

        public void onAttached(Activity activity) {
        }

        public void onDetached() {
        }

        void clear() {
            this.mRequestInterface = null;
            this.mContext = null;
            this.mActivity = null;
        }
    }

    public static class ConfirmationRequest extends Request {
        final Bundle mExtras;
        final CharSequence mPrompt;

        public ConfirmationRequest(CharSequence prompt, Bundle extras) {
            this.mPrompt = prompt;
            this.mExtras = extras;
        }

        public void onConfirmationResult(boolean confirmed, Bundle result) {
        }

        @Override
        IVoiceInteractorRequest submit(IVoiceInteractor interactor, String packageName, IVoiceInteractorCallback callback) throws RemoteException {
            return interactor.startConfirmation(packageName, callback, this.mPrompt, this.mExtras);
        }
    }

    public static class CompleteVoiceRequest extends Request {
        final Bundle mExtras;
        final CharSequence mMessage;

        public CompleteVoiceRequest(CharSequence message, Bundle extras) {
            this.mMessage = message;
            this.mExtras = extras;
        }

        public void onCompleteResult(Bundle result) {
        }

        @Override
        IVoiceInteractorRequest submit(IVoiceInteractor interactor, String packageName, IVoiceInteractorCallback callback) throws RemoteException {
            return interactor.startCompleteVoice(packageName, callback, this.mMessage, this.mExtras);
        }
    }

    public static class AbortVoiceRequest extends Request {
        final Bundle mExtras;
        final CharSequence mMessage;

        public AbortVoiceRequest(CharSequence message, Bundle extras) {
            this.mMessage = message;
            this.mExtras = extras;
        }

        public void onAbortResult(Bundle result) {
        }

        @Override
        IVoiceInteractorRequest submit(IVoiceInteractor interactor, String packageName, IVoiceInteractorCallback callback) throws RemoteException {
            return interactor.startAbortVoice(packageName, callback, this.mMessage, this.mExtras);
        }
    }

    public static class CommandRequest extends Request {
        final Bundle mArgs;
        final String mCommand;

        public CommandRequest(String command, Bundle args) {
            this.mCommand = command;
            this.mArgs = args;
        }

        public void onCommandResult(Bundle result) {
        }

        @Override
        IVoiceInteractorRequest submit(IVoiceInteractor interactor, String packageName, IVoiceInteractorCallback callback) throws RemoteException {
            return interactor.startCommand(packageName, callback, this.mCommand, this.mArgs);
        }
    }

    VoiceInteractor(IVoiceInteractor interactor, Context context, Activity activity, Looper looper) {
        this.mInteractor = interactor;
        this.mContext = context;
        this.mActivity = activity;
        this.mHandlerCaller = new HandlerCaller(context, looper, this.mHandlerCallerCallback, true);
    }

    Request pullRequest(IVoiceInteractorRequest request, boolean complete) {
        Request req;
        synchronized (this.mActiveRequests) {
            req = this.mActiveRequests.get(request.asBinder());
            if (req != null && complete) {
                this.mActiveRequests.remove(request.asBinder());
            }
        }
        return req;
    }

    private ArrayList<Request> makeRequestList() {
        int N = this.mActiveRequests.size();
        if (N < 1) {
            return null;
        }
        ArrayList<Request> list = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            list.add(this.mActiveRequests.valueAt(i));
        }
        return list;
    }

    void attachActivity(Activity activity) {
        if (this.mActivity != activity) {
            this.mContext = activity;
            this.mActivity = activity;
            ArrayList<Request> reqs = makeRequestList();
            if (reqs != null) {
                for (int i = 0; i < reqs.size(); i++) {
                    Request req = reqs.get(i);
                    req.mContext = activity;
                    req.mActivity = activity;
                    req.onAttached(activity);
                }
            }
        }
    }

    void detachActivity() {
        ArrayList<Request> reqs = makeRequestList();
        if (reqs != null) {
            for (int i = 0; i < reqs.size(); i++) {
                Request req = reqs.get(i);
                req.onDetached();
                req.mActivity = null;
                req.mContext = null;
            }
        }
        this.mContext = null;
        this.mActivity = null;
    }

    public boolean submitRequest(Request request) {
        try {
            IVoiceInteractorRequest ireq = request.submit(this.mInteractor, this.mContext.getOpPackageName(), this.mCallback);
            request.mRequestInterface = ireq;
            request.mContext = this.mContext;
            request.mActivity = this.mActivity;
            synchronized (this.mActiveRequests) {
                this.mActiveRequests.put(ireq.asBinder(), request);
            }
            return true;
        } catch (RemoteException e) {
            Log.w(TAG, "Remove voice interactor service died", e);
            return false;
        }
    }

    public boolean[] supportsCommands(String[] commands) {
        try {
            boolean[] res = this.mInteractor.supportsCommands(this.mContext.getOpPackageName(), commands);
            Log.d(TAG, "supportsCommands: cmds=" + commands + " res=" + res);
            return res;
        } catch (RemoteException e) {
            throw new RuntimeException("Voice interactor has died", e);
        }
    }
}
