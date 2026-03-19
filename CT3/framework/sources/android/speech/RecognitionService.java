package android.speech;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.PermissionChecker;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.speech.IRecognitionService;
import android.util.Log;
import java.lang.ref.WeakReference;

public abstract class RecognitionService extends Service {
    private static final boolean DBG = false;
    private static final int MSG_CANCEL = 3;
    private static final int MSG_RESET = 4;
    private static final int MSG_START_LISTENING = 1;
    private static final int MSG_STOP_LISTENING = 2;
    public static final String SERVICE_INTERFACE = "android.speech.RecognitionService";
    public static final String SERVICE_META_DATA = "android.speech";
    private static final String TAG = "RecognitionService";
    private RecognitionServiceBinder mBinder = new RecognitionServiceBinder(this);
    private Callback mCurrentCallback = null;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    StartListeningArgs args = (StartListeningArgs) msg.obj;
                    RecognitionService.this.dispatchStartListening(args.mIntent, args.mListener, args.mCallingUid);
                    break;
                case 2:
                    RecognitionService.this.dispatchStopListening((IRecognitionListener) msg.obj);
                    break;
                case 3:
                    RecognitionService.this.dispatchCancel((IRecognitionListener) msg.obj);
                    break;
                case 4:
                    RecognitionService.this.dispatchClearCallback();
                    break;
            }
        }
    };

    protected abstract void onCancel(Callback callback);

    protected abstract void onStartListening(Intent intent, Callback callback);

    protected abstract void onStopListening(Callback callback);

    private void dispatchStartListening(Intent intent, final IRecognitionListener listener, int callingUid) {
        Callback callback = null;
        if (this.mCurrentCallback == null) {
            try {
                listener.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        RecognitionService.this.mHandler.sendMessage(RecognitionService.this.mHandler.obtainMessage(3, listener));
                    }
                }, 0);
                this.mCurrentCallback = new Callback(this, listener, callingUid, callback);
                onStartListening(intent, this.mCurrentCallback);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "dead listener on startListening");
                return;
            }
        }
        try {
            listener.onError(8);
        } catch (RemoteException e2) {
            Log.d(TAG, "onError call from startListening failed");
        }
        Log.i(TAG, "concurrent startListening received - ignoring this call");
    }

    private void dispatchStopListening(IRecognitionListener listener) {
        try {
            if (this.mCurrentCallback == null) {
                listener.onError(5);
                Log.w(TAG, "stopListening called with no preceding startListening - ignoring");
            } else if (this.mCurrentCallback.mListener.asBinder() != listener.asBinder()) {
                listener.onError(8);
                Log.w(TAG, "stopListening called by other caller than startListening - ignoring");
            } else {
                onStopListening(this.mCurrentCallback);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onError call from stopListening failed");
        }
    }

    private void dispatchCancel(IRecognitionListener listener) {
        if (this.mCurrentCallback == null) {
            return;
        }
        if (this.mCurrentCallback.mListener.asBinder() != listener.asBinder()) {
            Log.w(TAG, "cancel called by client who did not call startListening - ignoring");
        } else {
            onCancel(this.mCurrentCallback);
            this.mCurrentCallback = null;
        }
    }

    private void dispatchClearCallback() {
        this.mCurrentCallback = null;
    }

    private class StartListeningArgs {
        public final int mCallingUid;
        public final Intent mIntent;
        public final IRecognitionListener mListener;

        public StartListeningArgs(Intent intent, IRecognitionListener listener, int callingUid) {
            this.mIntent = intent;
            this.mListener = listener;
            this.mCallingUid = callingUid;
        }
    }

    private boolean checkPermissions(IRecognitionListener listener) {
        if (PermissionChecker.checkCallingOrSelfPermission(this, Manifest.permission.RECORD_AUDIO) == 0) {
            return true;
        }
        try {
            Log.e(TAG, "call for recognition service without RECORD_AUDIO permissions");
            listener.onError(9);
        } catch (RemoteException re) {
            Log.e(TAG, "sending ERROR_INSUFFICIENT_PERMISSIONS message failed", re);
        }
        return false;
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    @Override
    public void onDestroy() {
        this.mCurrentCallback = null;
        this.mBinder.clearReference();
        super.onDestroy();
    }

    public class Callback {
        private final int mCallingUid;
        private final IRecognitionListener mListener;

        Callback(RecognitionService this$0, IRecognitionListener listener, int callingUid, Callback callback) {
            this(listener, callingUid);
        }

        private Callback(IRecognitionListener listener, int callingUid) {
            this.mListener = listener;
            this.mCallingUid = callingUid;
        }

        public void beginningOfSpeech() throws RemoteException {
            this.mListener.onBeginningOfSpeech();
        }

        public void bufferReceived(byte[] buffer) throws RemoteException {
            this.mListener.onBufferReceived(buffer);
        }

        public void endOfSpeech() throws RemoteException {
            this.mListener.onEndOfSpeech();
        }

        public void error(int error) throws RemoteException {
            Message.obtain(RecognitionService.this.mHandler, 4).sendToTarget();
            this.mListener.onError(error);
        }

        public void partialResults(Bundle partialResults) throws RemoteException {
            this.mListener.onPartialResults(partialResults);
        }

        public void readyForSpeech(Bundle params) throws RemoteException {
            this.mListener.onReadyForSpeech(params);
        }

        public void results(Bundle results) throws RemoteException {
            Message.obtain(RecognitionService.this.mHandler, 4).sendToTarget();
            this.mListener.onResults(results);
        }

        public void rmsChanged(float rmsdB) throws RemoteException {
            this.mListener.onRmsChanged(rmsdB);
        }

        public int getCallingUid() {
            return this.mCallingUid;
        }
    }

    private static final class RecognitionServiceBinder extends IRecognitionService.Stub {
        private final WeakReference<RecognitionService> mServiceRef;

        public RecognitionServiceBinder(RecognitionService service) {
            this.mServiceRef = new WeakReference<>(service);
        }

        @Override
        public void startListening(Intent recognizerIntent, IRecognitionListener listener) {
            RecognitionService service = this.mServiceRef.get();
            if (service == null || !service.checkPermissions(listener)) {
                return;
            }
            Handler handler = service.mHandler;
            Handler handler2 = service.mHandler;
            service.getClass();
            handler.sendMessage(Message.obtain(handler2, 1, service.new StartListeningArgs(recognizerIntent, listener, Binder.getCallingUid())));
        }

        @Override
        public void stopListening(IRecognitionListener listener) {
            RecognitionService service = this.mServiceRef.get();
            if (service == null || !service.checkPermissions(listener)) {
                return;
            }
            service.mHandler.sendMessage(Message.obtain(service.mHandler, 2, listener));
        }

        @Override
        public void cancel(IRecognitionListener listener) {
            RecognitionService service = this.mServiceRef.get();
            if (service == null || !service.checkPermissions(listener)) {
                return;
            }
            service.mHandler.sendMessage(Message.obtain(service.mHandler, 3, listener));
        }

        public void clearReference() {
            this.mServiceRef.clear();
        }
    }
}
