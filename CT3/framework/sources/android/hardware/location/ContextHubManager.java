package android.hardware.location;

import android.content.Context;
import android.hardware.location.IContextHubCallback;
import android.hardware.location.IContextHubService;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public final class ContextHubManager {
    private static final String TAG = "ContextHubManager";
    private Callback mCallback;
    private Handler mCallbackHandler;
    private IContextHubCallback.Stub mClientCallback = new IContextHubCallback.Stub() {
        @Override
        public void onMessageReceipt(final int hubId, final int nanoAppId, final ContextHubMessage message) {
            if (ContextHubManager.this.mCallback != null) {
                synchronized (this) {
                    final Callback callback = ContextHubManager.this.mCallback;
                    Handler handler = ContextHubManager.this.mCallbackHandler == null ? new Handler(ContextHubManager.this.mMainLooper) : ContextHubManager.this.mCallbackHandler;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onMessageReceipt(hubId, nanoAppId, message);
                        }
                    });
                }
            }
            if (ContextHubManager.this.mLocalCallback != null) {
                synchronized (this) {
                    ContextHubManager.this.mLocalCallback.onMessageReceipt(hubId, nanoAppId, message);
                }
            } else {
                Log.d(ContextHubManager.TAG, "Context hub manager client callback is NULL");
            }
        }
    };
    private IContextHubService mContextHubService;

    @Deprecated
    private ICallback mLocalCallback;
    private final Looper mMainLooper;

    @Deprecated
    public interface ICallback {
        void onMessageReceipt(int i, int i2, ContextHubMessage contextHubMessage);
    }

    public static abstract class Callback {
        public abstract void onMessageReceipt(int i, int i2, ContextHubMessage contextHubMessage);

        protected Callback() {
        }
    }

    public int[] getContextHubHandles() {
        try {
            int[] retVal = getBinder().getContextHubHandles();
            return retVal;
        } catch (RemoteException e) {
            Log.w(TAG, "Could not fetch context hub handles : " + e);
            return null;
        }
    }

    public ContextHubInfo getContextHubInfo(int hubHandle) {
        try {
            ContextHubInfo retVal = getBinder().getContextHubInfo(hubHandle);
            return retVal;
        } catch (RemoteException e) {
            Log.w(TAG, "Could not fetch context hub info :" + e);
            return null;
        }
    }

    public int loadNanoApp(int hubHandle, NanoApp app) {
        if (app == null) {
            return -1;
        }
        try {
            int retVal = getBinder().loadNanoApp(hubHandle, app);
            return retVal;
        } catch (RemoteException e) {
            Log.w(TAG, "Could not load nanoApp :" + e);
            return -1;
        }
    }

    public int unloadNanoApp(int nanoAppHandle) {
        try {
            int retVal = getBinder().unloadNanoApp(nanoAppHandle);
            return retVal;
        } catch (RemoteException e) {
            Log.w(TAG, "Could not fetch unload nanoApp :" + e);
            return -1;
        }
    }

    public NanoAppInstanceInfo getNanoAppInstanceInfo(int nanoAppHandle) {
        try {
            NanoAppInstanceInfo retVal = getBinder().getNanoAppInstanceInfo(nanoAppHandle);
            return retVal;
        } catch (RemoteException e) {
            Log.w(TAG, "Could not fetch nanoApp info :" + e);
            return null;
        }
    }

    public int[] findNanoAppOnHub(int hubHandle, NanoAppFilter filter) {
        try {
            int[] retVal = getBinder().findNanoAppOnHub(hubHandle, filter);
            return retVal;
        } catch (RemoteException e) {
            Log.w(TAG, "Could not query nanoApp instance :" + e);
            return null;
        }
    }

    public int sendMessage(int hubHandle, int nanoAppHandle, ContextHubMessage message) {
        if (message == null || message.getData() == null) {
            Log.w(TAG, "null ptr");
            return -1;
        }
        try {
            int retVal = getBinder().sendMessage(hubHandle, nanoAppHandle, message);
            return retVal;
        } catch (RemoteException e) {
            Log.w(TAG, "Could not send message :" + e.toString());
            return -1;
        }
    }

    public int registerCallback(Callback callback) {
        return registerCallback(callback, null);
    }

    @Deprecated
    public int registerCallback(ICallback callback) {
        if (this.mLocalCallback != null) {
            Log.w(TAG, "Max number of local callbacks reached!");
            return -1;
        }
        this.mLocalCallback = callback;
        return 0;
    }

    public int registerCallback(Callback callback, Handler handler) {
        synchronized (this) {
            if (this.mCallback != null) {
                Log.w(TAG, "Max number of callbacks reached!");
                return -1;
            }
            this.mCallback = callback;
            this.mCallbackHandler = handler;
            return 0;
        }
    }

    public int unregisterCallback(Callback callback) {
        synchronized (this) {
            if (callback != this.mCallback) {
                Log.w(TAG, "Cannot recognize callback!");
                return -1;
            }
            this.mCallback = null;
            this.mCallbackHandler = null;
            return 0;
        }
    }

    public synchronized int unregisterCallback(ICallback callback) {
        if (callback != this.mLocalCallback) {
            Log.w(TAG, "Cannot recognize local callback!");
            return -1;
        }
        this.mLocalCallback = null;
        return 0;
    }

    public ContextHubManager(Context context, Looper mainLooper) {
        this.mMainLooper = mainLooper;
        IBinder b = ServiceManager.getService(ContextHubService.CONTEXTHUB_SERVICE);
        if (b != null) {
            this.mContextHubService = IContextHubService.Stub.asInterface(b);
            try {
                getBinder().registerCallback(this.mClientCallback);
                return;
            } catch (RemoteException e) {
                Log.w(TAG, "Could not register callback:" + e);
                return;
            }
        }
        Log.w(TAG, "failed to getService");
    }

    private IContextHubService getBinder() throws RemoteException {
        if (this.mContextHubService == null) {
            throw new RemoteException("Service not connected.");
        }
        return this.mContextHubService;
    }
}
