package com.android.server.fingerprint;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.service.fingerprint.FingerprintUtils;
import android.service.fingerprint.IFingerprintService;
import android.service.fingerprint.IFingerprintServiceReceiver;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.SystemService;
import java.lang.ref.WeakReference;

public class FingerprintService extends SystemService {
    private static final boolean DEBUG = true;
    public static final String ENROLL_FINGERPRINT = "android.permission.ENROLL_FINGERPRINT";
    private static final int MSG_NOTIFY = 10;
    private static final long MS_PER_SEC = 1000;
    private static final int STATE_ENROLLING = 2;
    private static final int STATE_IDLE = 0;
    private static final int STATE_LISTENING = 1;
    private static final int STATE_REMOVING = 3;
    public static final String USE_FINGERPRINT = "android.permission.USE_FINGERPRINT";
    private final String TAG;
    private ArrayMap<IBinder, ClientData> mClients;
    private Context mContext;
    Handler mHandler;

    native int nativeCloseHal();

    native int nativeEnroll(int i);

    native int nativeEnrollCancel();

    native void nativeInit(FingerprintService fingerprintService);

    native int nativeOpenHal();

    native int nativeRemove(int i);

    private static final class ClientData {
        public IFingerprintServiceReceiver receiver;
        int state;
        public TokenWatcher tokenWatcher;
        int userId;

        private ClientData() {
        }

        IBinder getToken() {
            return this.tokenWatcher.getToken();
        }
    }

    private class TokenWatcher implements IBinder.DeathRecipient {
        WeakReference<IBinder> token;

        TokenWatcher(IBinder token) {
            this.token = new WeakReference<>(token);
        }

        IBinder getToken() {
            return this.token.get();
        }

        @Override
        public void binderDied() {
            FingerprintService.this.mClients.remove(this.token);
            this.token = null;
        }

        protected void finalize() throws Throwable {
            try {
                if (this.token != null) {
                    Slog.w("FingerprintService", "removing leaked reference: " + this.token);
                    FingerprintService.this.mClients.remove(this.token);
                }
            } finally {
                super.finalize();
            }
        }
    }

    public FingerprintService(Context context) {
        super(context);
        this.TAG = "FingerprintService";
        this.mClients = new ArrayMap<>();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 10:
                        FingerprintService.this.handleNotify(msg.arg1, msg.arg2, ((Integer) msg.obj).intValue());
                        break;
                    default:
                        Slog.w("FingerprintService", "Unknown message:" + msg.what);
                        break;
                }
            }
        };
        this.mContext = context;
        nativeInit(this);
    }

    void notify(int msg, int arg1, int arg2) {
        this.mHandler.obtainMessage(10, msg, arg1, Integer.valueOf(arg2)).sendToTarget();
    }

    void handleNotify(int msg, int arg1, int arg2) {
        Slog.v("FingerprintService", "handleNotify(msg=" + msg + ", arg1=" + arg1 + ", arg2=" + arg2 + ")");
        for (int i = 0; i < this.mClients.size(); i++) {
            ClientData clientData = this.mClients.valueAt(i);
            if (clientData == null || clientData.receiver == null) {
                Slog.v("FingerprintService", "clientData at " + i + " is invalid!!");
            } else {
                switch (msg) {
                    case -1:
                        try {
                            clientData.receiver.onError(arg1);
                        } catch (RemoteException e) {
                            Slog.e("FingerprintService", "can't send message to client. Did it die?", e);
                            this.mClients.remove(this.mClients.keyAt(i));
                        }
                        break;
                    case 1:
                        try {
                            clientData.receiver.onAcquired(arg1);
                        } catch (RemoteException e2) {
                            Slog.e("FingerprintService", "can't send message to client. Did it die?", e2);
                            this.mClients.remove(this.mClients.keyAt(i));
                        }
                        break;
                    case 2:
                        try {
                            clientData.receiver.onProcessed(arg1);
                        } catch (RemoteException e3) {
                            Slog.e("FingerprintService", "can't send message to client. Did it die?", e3);
                            this.mClients.remove(this.mClients.keyAt(i));
                        }
                        break;
                    case 3:
                        if (clientData.state == 2) {
                            try {
                                clientData.receiver.onEnrollResult(arg1, arg2);
                            } catch (RemoteException e4) {
                                Slog.e("FingerprintService", "can't send message to client. Did it die?", e4);
                                this.mClients.remove(this.mClients.keyAt(i));
                            }
                            if (arg2 == 0) {
                                FingerprintUtils.addFingerprintIdForUser(arg1, this.mContext.getContentResolver(), clientData.userId);
                                clientData.state = 0;
                            }
                        } else {
                            Slog.w("FingerprintService", "Client not enrolling");
                        }
                        break;
                    case 4:
                        if (arg1 == 0) {
                            throw new IllegalStateException("Got illegal id from HAL");
                        }
                        FingerprintUtils.removeFingerprintIdForUser(arg1, this.mContext.getContentResolver(), clientData.userId);
                        if (clientData.receiver != null) {
                            try {
                                clientData.receiver.onRemoved(arg1);
                            } catch (RemoteException e5) {
                                Slog.e("FingerprintService", "can't send message to client. Did it die?", e5);
                                this.mClients.remove(this.mClients.keyAt(i));
                            }
                        }
                        clientData.state = 1;
                        break;
                        break;
                }
            }
        }
    }

    void startEnroll(IBinder token, long timeout, int userId) {
        ClientData clientData = this.mClients.get(token);
        if (clientData != null) {
            if (clientData.userId != userId) {
                throw new IllegalStateException("Bad user");
            }
            clientData.state = 2;
            nativeEnroll((int) (timeout / MS_PER_SEC));
            return;
        }
        Slog.w("FingerprintService", "enroll(): No listener registered");
    }

    void startEnrollCancel(IBinder token, int userId) {
        ClientData clientData = this.mClients.get(token);
        if (clientData != null) {
            if (clientData.userId != userId) {
                throw new IllegalStateException("Bad user");
            }
            clientData.state = 1;
            nativeEnrollCancel();
            return;
        }
        Slog.w("FingerprintService", "enrollCancel(): No listener registered");
    }

    void startRemove(IBinder token, int fingerId, int userId) {
        ClientData clientData = this.mClients.get(token);
        if (clientData != null) {
            if (clientData.userId != userId) {
                throw new IllegalStateException("Bad user");
            }
            clientData.state = 3;
            int result = nativeRemove(fingerId);
            if (result != 0) {
                Slog.w("FingerprintService", "Error removing fingerprint with id = " + fingerId);
                return;
            }
            return;
        }
        Slog.w("FingerprintService", "remove(" + token + "): No listener registered");
    }

    void addListener(IBinder token, IFingerprintServiceReceiver receiver, int userId) {
        Slog.v("FingerprintService", "startListening(" + receiver + ")");
        if (this.mClients.get(token) == null) {
            ClientData clientData = new ClientData();
            clientData.state = 1;
            clientData.receiver = receiver;
            clientData.userId = userId;
            clientData.tokenWatcher = new TokenWatcher(token);
            try {
                token.linkToDeath(clientData.tokenWatcher, 0);
                this.mClients.put(token, clientData);
                return;
            } catch (RemoteException e) {
                Slog.w("FingerprintService", "caught remote exception in linkToDeath: ", e);
                return;
            }
        }
        Slog.v("FingerprintService", "listener already registered for " + token);
    }

    void removeListener(IBinder token, int userId) {
        Slog.v("FingerprintService", "stopListening(" + token + ")");
        ClientData clientData = this.mClients.get(token);
        if (clientData != null) {
            token.unlinkToDeath(clientData.tokenWatcher, 0);
            this.mClients.remove(token);
        } else {
            Slog.v("FingerprintService", "listener not registered: " + token);
        }
        this.mClients.remove(token);
    }

    void checkPermission(String permisison) {
    }

    private final class FingerprintServiceWrapper extends IFingerprintService.Stub {
        private FingerprintServiceWrapper() {
        }

        public void enroll(IBinder token, long timeout, int userId) {
            FingerprintService.this.checkPermission(FingerprintService.ENROLL_FINGERPRINT);
            FingerprintService.this.startEnroll(token, timeout, userId);
        }

        public void enrollCancel(IBinder token, int userId) {
            FingerprintService.this.checkPermission(FingerprintService.ENROLL_FINGERPRINT);
            FingerprintService.this.startEnrollCancel(token, userId);
        }

        public void remove(IBinder token, int fingerprintId, int userId) {
            FingerprintService.this.checkPermission(FingerprintService.ENROLL_FINGERPRINT);
            FingerprintService.this.startRemove(token, fingerprintId, userId);
        }

        public void startListening(IBinder token, IFingerprintServiceReceiver receiver, int userId) {
            FingerprintService.this.checkPermission(FingerprintService.USE_FINGERPRINT);
            FingerprintService.this.addListener(token, receiver, userId);
        }

        public void stopListening(IBinder token, int userId) {
            FingerprintService.this.checkPermission(FingerprintService.USE_FINGERPRINT);
            FingerprintService.this.removeListener(token, userId);
        }
    }

    @Override
    public void onStart() {
        publishBinderService("fingerprint", new FingerprintServiceWrapper());
        nativeOpenHal();
    }
}
