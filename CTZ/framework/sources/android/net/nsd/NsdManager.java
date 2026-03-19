package android.net.nsd;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.Preconditions;
import java.util.concurrent.CountDownLatch;

public final class NsdManager {
    public static final String ACTION_NSD_STATE_CHANGED = "android.net.nsd.STATE_CHANGED";
    private static final int BASE = 393216;
    private static final boolean DBG = false;
    public static final int DISABLE = 393241;
    public static final int DISCOVER_SERVICES = 393217;
    public static final int DISCOVER_SERVICES_FAILED = 393219;
    public static final int DISCOVER_SERVICES_STARTED = 393218;
    public static final int ENABLE = 393240;
    public static final String EXTRA_NSD_STATE = "nsd_state";
    public static final int FAILURE_ALREADY_ACTIVE = 3;
    public static final int FAILURE_INTERNAL_ERROR = 0;
    public static final int FAILURE_MAX_LIMIT = 4;
    private static final int FIRST_LISTENER_KEY = 1;
    public static final int NATIVE_DAEMON_EVENT = 393242;
    public static final int NSD_STATE_DISABLED = 1;
    public static final int NSD_STATE_ENABLED = 2;
    public static final int PROTOCOL_DNS_SD = 1;
    public static final int REGISTER_SERVICE = 393225;
    public static final int REGISTER_SERVICE_FAILED = 393226;
    public static final int REGISTER_SERVICE_SUCCEEDED = 393227;
    public static final int RESOLVE_SERVICE = 393234;
    public static final int RESOLVE_SERVICE_FAILED = 393235;
    public static final int RESOLVE_SERVICE_SUCCEEDED = 393236;
    public static final int SERVICE_FOUND = 393220;
    public static final int SERVICE_LOST = 393221;
    public static final int STOP_DISCOVERY = 393222;
    public static final int STOP_DISCOVERY_FAILED = 393223;
    public static final int STOP_DISCOVERY_SUCCEEDED = 393224;
    public static final int UNREGISTER_SERVICE = 393228;
    public static final int UNREGISTER_SERVICE_FAILED = 393229;
    public static final int UNREGISTER_SERVICE_SUCCEEDED = 393230;
    private final Context mContext;
    private ServiceHandler mHandler;
    private final INsdManager mService;
    private static final String TAG = NsdManager.class.getSimpleName();
    private static final SparseArray<String> EVENT_NAMES = new SparseArray<>();
    private int mListenerKey = 1;
    private final SparseArray mListenerMap = new SparseArray();
    private final SparseArray<NsdServiceInfo> mServiceMap = new SparseArray<>();
    private final Object mMapLock = new Object();
    private final AsyncChannel mAsyncChannel = new AsyncChannel();
    private final CountDownLatch mConnected = new CountDownLatch(1);

    public interface DiscoveryListener {
        void onDiscoveryStarted(String str);

        void onDiscoveryStopped(String str);

        void onServiceFound(NsdServiceInfo nsdServiceInfo);

        void onServiceLost(NsdServiceInfo nsdServiceInfo);

        void onStartDiscoveryFailed(String str, int i);

        void onStopDiscoveryFailed(String str, int i);
    }

    public interface RegistrationListener {
        void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i);

        void onServiceRegistered(NsdServiceInfo nsdServiceInfo);

        void onServiceUnregistered(NsdServiceInfo nsdServiceInfo);

        void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i);
    }

    public interface ResolveListener {
        void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i);

        void onServiceResolved(NsdServiceInfo nsdServiceInfo);
    }

    static {
        EVENT_NAMES.put(DISCOVER_SERVICES, "DISCOVER_SERVICES");
        EVENT_NAMES.put(DISCOVER_SERVICES_STARTED, "DISCOVER_SERVICES_STARTED");
        EVENT_NAMES.put(DISCOVER_SERVICES_FAILED, "DISCOVER_SERVICES_FAILED");
        EVENT_NAMES.put(SERVICE_FOUND, "SERVICE_FOUND");
        EVENT_NAMES.put(SERVICE_LOST, "SERVICE_LOST");
        EVENT_NAMES.put(STOP_DISCOVERY, "STOP_DISCOVERY");
        EVENT_NAMES.put(STOP_DISCOVERY_FAILED, "STOP_DISCOVERY_FAILED");
        EVENT_NAMES.put(STOP_DISCOVERY_SUCCEEDED, "STOP_DISCOVERY_SUCCEEDED");
        EVENT_NAMES.put(REGISTER_SERVICE, "REGISTER_SERVICE");
        EVENT_NAMES.put(REGISTER_SERVICE_FAILED, "REGISTER_SERVICE_FAILED");
        EVENT_NAMES.put(REGISTER_SERVICE_SUCCEEDED, "REGISTER_SERVICE_SUCCEEDED");
        EVENT_NAMES.put(UNREGISTER_SERVICE, "UNREGISTER_SERVICE");
        EVENT_NAMES.put(UNREGISTER_SERVICE_FAILED, "UNREGISTER_SERVICE_FAILED");
        EVENT_NAMES.put(UNREGISTER_SERVICE_SUCCEEDED, "UNREGISTER_SERVICE_SUCCEEDED");
        EVENT_NAMES.put(RESOLVE_SERVICE, "RESOLVE_SERVICE");
        EVENT_NAMES.put(RESOLVE_SERVICE_FAILED, "RESOLVE_SERVICE_FAILED");
        EVENT_NAMES.put(RESOLVE_SERVICE_SUCCEEDED, "RESOLVE_SERVICE_SUCCEEDED");
        EVENT_NAMES.put(ENABLE, "ENABLE");
        EVENT_NAMES.put(DISABLE, "DISABLE");
        EVENT_NAMES.put(NATIVE_DAEMON_EVENT, "NATIVE_DAEMON_EVENT");
    }

    public static String nameOf(int i) {
        String str = EVENT_NAMES.get(i);
        if (str == null) {
            return Integer.toString(i);
        }
        return str;
    }

    public NsdManager(Context context, INsdManager iNsdManager) {
        this.mService = iNsdManager;
        this.mContext = context;
        init();
    }

    @VisibleForTesting
    public void disconnect() {
        this.mAsyncChannel.disconnect();
        this.mHandler.getLooper().quitSafely();
    }

    @VisibleForTesting
    class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            Object obj;
            NsdServiceInfo nsdServiceInfo;
            int i = message.what;
            int i2 = message.arg2;
            if (i == 69632) {
                NsdManager.this.mAsyncChannel.sendMessage(AsyncChannel.CMD_CHANNEL_FULL_CONNECTION);
                return;
            }
            if (i == 69634) {
                NsdManager.this.mConnected.countDown();
                return;
            }
            if (i != 69636) {
                synchronized (NsdManager.this.mMapLock) {
                    obj = NsdManager.this.mListenerMap.get(i2);
                    nsdServiceInfo = (NsdServiceInfo) NsdManager.this.mServiceMap.get(i2);
                }
                if (obj == null) {
                    Log.d(NsdManager.TAG, "Stale key " + message.arg2);
                    return;
                }
                switch (i) {
                    case NsdManager.DISCOVER_SERVICES_STARTED:
                        ((DiscoveryListener) obj).onDiscoveryStarted(NsdManager.getNsdServiceInfoType((NsdServiceInfo) message.obj));
                        return;
                    case NsdManager.DISCOVER_SERVICES_FAILED:
                        NsdManager.this.removeListener(i2);
                        ((DiscoveryListener) obj).onStartDiscoveryFailed(NsdManager.getNsdServiceInfoType(nsdServiceInfo), message.arg1);
                        return;
                    case NsdManager.SERVICE_FOUND:
                        ((DiscoveryListener) obj).onServiceFound((NsdServiceInfo) message.obj);
                        return;
                    case NsdManager.SERVICE_LOST:
                        ((DiscoveryListener) obj).onServiceLost((NsdServiceInfo) message.obj);
                        return;
                    case NsdManager.STOP_DISCOVERY:
                    case NsdManager.REGISTER_SERVICE:
                    case NsdManager.UNREGISTER_SERVICE:
                    case 393231:
                    case 393232:
                    case 393233:
                    case NsdManager.RESOLVE_SERVICE:
                    default:
                        Log.d(NsdManager.TAG, "Ignored " + message);
                        return;
                    case NsdManager.STOP_DISCOVERY_FAILED:
                        NsdManager.this.removeListener(i2);
                        ((DiscoveryListener) obj).onStopDiscoveryFailed(NsdManager.getNsdServiceInfoType(nsdServiceInfo), message.arg1);
                        return;
                    case NsdManager.STOP_DISCOVERY_SUCCEEDED:
                        NsdManager.this.removeListener(i2);
                        ((DiscoveryListener) obj).onDiscoveryStopped(NsdManager.getNsdServiceInfoType(nsdServiceInfo));
                        return;
                    case NsdManager.REGISTER_SERVICE_FAILED:
                        NsdManager.this.removeListener(i2);
                        ((RegistrationListener) obj).onRegistrationFailed(nsdServiceInfo, message.arg1);
                        return;
                    case NsdManager.REGISTER_SERVICE_SUCCEEDED:
                        ((RegistrationListener) obj).onServiceRegistered((NsdServiceInfo) message.obj);
                        return;
                    case NsdManager.UNREGISTER_SERVICE_FAILED:
                        NsdManager.this.removeListener(i2);
                        ((RegistrationListener) obj).onUnregistrationFailed(nsdServiceInfo, message.arg1);
                        return;
                    case NsdManager.UNREGISTER_SERVICE_SUCCEEDED:
                        NsdManager.this.removeListener(message.arg2);
                        ((RegistrationListener) obj).onServiceUnregistered(nsdServiceInfo);
                        return;
                    case NsdManager.RESOLVE_SERVICE_FAILED:
                        NsdManager.this.removeListener(i2);
                        ((ResolveListener) obj).onResolveFailed(nsdServiceInfo, message.arg1);
                        return;
                    case NsdManager.RESOLVE_SERVICE_SUCCEEDED:
                        NsdManager.this.removeListener(i2);
                        ((ResolveListener) obj).onServiceResolved((NsdServiceInfo) message.obj);
                        return;
                }
            }
            Log.e(NsdManager.TAG, "Channel lost");
        }
    }

    private int nextListenerKey() {
        this.mListenerKey = Math.max(1, this.mListenerKey + 1);
        return this.mListenerKey;
    }

    private int putListener(Object obj, NsdServiceInfo nsdServiceInfo) {
        int iNextListenerKey;
        checkListener(obj);
        synchronized (this.mMapLock) {
            Preconditions.checkArgument(this.mListenerMap.indexOfValue(obj) == -1, "listener already in use");
            iNextListenerKey = nextListenerKey();
            this.mListenerMap.put(iNextListenerKey, obj);
            this.mServiceMap.put(iNextListenerKey, nsdServiceInfo);
        }
        return iNextListenerKey;
    }

    private void removeListener(int i) {
        synchronized (this.mMapLock) {
            this.mListenerMap.remove(i);
            this.mServiceMap.remove(i);
        }
    }

    private int getListenerKey(Object obj) {
        int iKeyAt;
        checkListener(obj);
        synchronized (this.mMapLock) {
            int iIndexOfValue = this.mListenerMap.indexOfValue(obj);
            Preconditions.checkArgument(iIndexOfValue != -1, "listener not registered");
            iKeyAt = this.mListenerMap.keyAt(iIndexOfValue);
        }
        return iKeyAt;
    }

    private static String getNsdServiceInfoType(NsdServiceInfo nsdServiceInfo) {
        return nsdServiceInfo == null ? "?" : nsdServiceInfo.getServiceType();
    }

    private void init() {
        Messenger messenger = getMessenger();
        if (messenger == null) {
            fatal("Failed to obtain service Messenger");
        }
        HandlerThread handlerThread = new HandlerThread("NsdManager");
        handlerThread.start();
        this.mHandler = new ServiceHandler(handlerThread.getLooper());
        this.mAsyncChannel.connect(this.mContext, this.mHandler, messenger);
        try {
            this.mConnected.await();
        } catch (InterruptedException e) {
            fatal("Interrupted wait at init");
        }
    }

    private static void fatal(String str) {
        Log.e(TAG, str);
        throw new RuntimeException(str);
    }

    public void registerService(NsdServiceInfo nsdServiceInfo, int i, RegistrationListener registrationListener) {
        Preconditions.checkArgument(nsdServiceInfo.getPort() > 0, "Invalid port number");
        checkServiceInfo(nsdServiceInfo);
        checkProtocol(i);
        this.mAsyncChannel.sendMessage(REGISTER_SERVICE, 0, putListener(registrationListener, nsdServiceInfo), nsdServiceInfo);
    }

    public void unregisterService(RegistrationListener registrationListener) {
        this.mAsyncChannel.sendMessage(UNREGISTER_SERVICE, 0, getListenerKey(registrationListener));
    }

    public void discoverServices(String str, int i, DiscoveryListener discoveryListener) {
        Preconditions.checkStringNotEmpty(str, "Service type cannot be empty");
        checkProtocol(i);
        NsdServiceInfo nsdServiceInfo = new NsdServiceInfo();
        nsdServiceInfo.setServiceType(str);
        this.mAsyncChannel.sendMessage(DISCOVER_SERVICES, 0, putListener(discoveryListener, nsdServiceInfo), nsdServiceInfo);
    }

    public void stopServiceDiscovery(DiscoveryListener discoveryListener) {
        this.mAsyncChannel.sendMessage(STOP_DISCOVERY, 0, getListenerKey(discoveryListener));
    }

    public void resolveService(NsdServiceInfo nsdServiceInfo, ResolveListener resolveListener) {
        checkServiceInfo(nsdServiceInfo);
        this.mAsyncChannel.sendMessage(RESOLVE_SERVICE, 0, putListener(resolveListener, nsdServiceInfo), nsdServiceInfo);
    }

    public void setEnabled(boolean z) {
        try {
            this.mService.setEnabled(z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private Messenger getMessenger() {
        try {
            return this.mService.getMessenger();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static void checkListener(Object obj) {
        Preconditions.checkNotNull(obj, "listener cannot be null");
    }

    private static void checkProtocol(int i) {
        Preconditions.checkArgument(i == 1, "Unsupported protocol");
    }

    private static void checkServiceInfo(NsdServiceInfo nsdServiceInfo) {
        Preconditions.checkNotNull(nsdServiceInfo, "NsdServiceInfo cannot be null");
        Preconditions.checkStringNotEmpty(nsdServiceInfo.getServiceName(), "Service name cannot be empty");
        Preconditions.checkStringNotEmpty(nsdServiceInfo.getServiceType(), "Service type cannot be empty");
    }
}
