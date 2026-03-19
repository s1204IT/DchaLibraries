package android.net.lowpan;

import android.content.Context;
import android.net.lowpan.ILowpanManager;
import android.net.lowpan.ILowpanManagerListener;
import android.net.lowpan.LowpanManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class LowpanManager {
    private static final String TAG = LowpanManager.class.getSimpleName();
    private final Map<IBinder, WeakReference<LowpanInterface>> mBinderCache;
    private final Context mContext;
    private final Map<String, LowpanInterface> mInterfaceCache;
    private final Map<Integer, ILowpanManagerListener> mListenerMap;
    private final Looper mLooper;
    private final ILowpanManager mService;

    public static abstract class Callback {
        public void onInterfaceAdded(LowpanInterface lowpanInterface) {
        }

        public void onInterfaceRemoved(LowpanInterface lowpanInterface) {
        }
    }

    public static LowpanManager from(Context context) {
        return (LowpanManager) context.getSystemService("lowpan");
    }

    public static LowpanManager getManager() {
        IBinder service = ServiceManager.getService("lowpan");
        if (service != null) {
            return new LowpanManager(ILowpanManager.Stub.asInterface(service));
        }
        return null;
    }

    LowpanManager(ILowpanManager iLowpanManager) {
        this.mListenerMap = new HashMap();
        this.mInterfaceCache = new HashMap();
        this.mBinderCache = new WeakHashMap();
        this.mService = iLowpanManager;
        this.mContext = null;
        this.mLooper = null;
    }

    public LowpanManager(Context context, ILowpanManager iLowpanManager, Looper looper) {
        this.mListenerMap = new HashMap();
        this.mInterfaceCache = new HashMap();
        this.mBinderCache = new WeakHashMap();
        this.mContext = context;
        this.mService = iLowpanManager;
        this.mLooper = looper;
    }

    public LowpanInterface getInterfaceNoCreate(ILowpanInterface iLowpanInterface) {
        LowpanInterface lowpanInterface;
        synchronized (this.mBinderCache) {
            if (this.mBinderCache.containsKey(iLowpanInterface.asBinder())) {
                lowpanInterface = this.mBinderCache.get(iLowpanInterface.asBinder()).get();
            } else {
                lowpanInterface = null;
            }
        }
        return lowpanInterface;
    }

    public LowpanInterface getInterface(final ILowpanInterface iLowpanInterface) {
        LowpanInterface lowpanInterface;
        try {
            synchronized (this.mBinderCache) {
                if (this.mBinderCache.containsKey(iLowpanInterface.asBinder())) {
                    lowpanInterface = this.mBinderCache.get(iLowpanInterface.asBinder()).get();
                } else {
                    lowpanInterface = null;
                }
                if (lowpanInterface == null) {
                    final String name = iLowpanInterface.getName();
                    LowpanInterface lowpanInterface2 = new LowpanInterface(this.mContext, iLowpanInterface, this.mLooper);
                    synchronized (this.mInterfaceCache) {
                        this.mInterfaceCache.put(lowpanInterface2.getName(), lowpanInterface2);
                    }
                    this.mBinderCache.put(iLowpanInterface.asBinder(), new WeakReference<>(lowpanInterface2));
                    iLowpanInterface.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                        @Override
                        public void binderDied() {
                            synchronized (LowpanManager.this.mInterfaceCache) {
                                LowpanInterface lowpanInterface3 = (LowpanInterface) LowpanManager.this.mInterfaceCache.get(name);
                                if (lowpanInterface3 != null && lowpanInterface3.getService() == iLowpanInterface) {
                                    LowpanManager.this.mInterfaceCache.remove(name);
                                }
                            }
                        }
                    }, 0);
                    lowpanInterface = lowpanInterface2;
                }
            }
            return lowpanInterface;
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public LowpanInterface getInterface(String str) {
        LowpanInterface lowpanInterface;
        try {
            synchronized (this.mInterfaceCache) {
                if (this.mInterfaceCache.containsKey(str)) {
                    lowpanInterface = this.mInterfaceCache.get(str);
                } else {
                    ILowpanInterface iLowpanInterface = this.mService.getInterface(str);
                    if (iLowpanInterface != null) {
                        lowpanInterface = getInterface(iLowpanInterface);
                    } else {
                        lowpanInterface = null;
                    }
                }
            }
            return lowpanInterface;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public LowpanInterface getInterface() {
        String[] interfaceList = getInterfaceList();
        if (interfaceList.length > 0) {
            return getInterface(interfaceList[0]);
        }
        return null;
    }

    public String[] getInterfaceList() {
        try {
            return this.mService.getInterfaceList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass2 extends ILowpanManagerListener.Stub {
        private Handler mHandler;
        final Callback val$cb;
        final Handler val$handler;

        AnonymousClass2(Handler handler, Callback callback) {
            this.val$handler = handler;
            this.val$cb = callback;
            if (this.val$handler == null) {
                if (LowpanManager.this.mLooper != null) {
                    this.mHandler = new Handler(LowpanManager.this.mLooper);
                    return;
                } else {
                    this.mHandler = new Handler();
                    return;
                }
            }
            this.mHandler = this.val$handler;
        }

        @Override
        public void onInterfaceAdded(final ILowpanInterface iLowpanInterface) {
            final Callback callback = this.val$cb;
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    LowpanManager.AnonymousClass2.lambda$onInterfaceAdded$0(this.f$0, iLowpanInterface, callback);
                }
            });
        }

        public static void lambda$onInterfaceAdded$0(AnonymousClass2 anonymousClass2, ILowpanInterface iLowpanInterface, Callback callback) {
            LowpanInterface lowpanInterface = LowpanManager.this.getInterface(iLowpanInterface);
            if (lowpanInterface != null) {
                callback.onInterfaceAdded(lowpanInterface);
            }
        }

        @Override
        public void onInterfaceRemoved(final ILowpanInterface iLowpanInterface) {
            final Callback callback = this.val$cb;
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    LowpanManager.AnonymousClass2.lambda$onInterfaceRemoved$1(this.f$0, iLowpanInterface, callback);
                }
            });
        }

        public static void lambda$onInterfaceRemoved$1(AnonymousClass2 anonymousClass2, ILowpanInterface iLowpanInterface, Callback callback) {
            LowpanInterface interfaceNoCreate = LowpanManager.this.getInterfaceNoCreate(iLowpanInterface);
            if (interfaceNoCreate != null) {
                callback.onInterfaceRemoved(interfaceNoCreate);
            }
        }
    }

    public void registerCallback(Callback callback, Handler handler) throws LowpanException {
        AnonymousClass2 anonymousClass2 = new AnonymousClass2(handler, callback);
        try {
            this.mService.addListener(anonymousClass2);
            synchronized (this.mListenerMap) {
                this.mListenerMap.put(Integer.valueOf(System.identityHashCode(callback)), anonymousClass2);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerCallback(Callback callback) throws LowpanException {
        registerCallback(callback, null);
    }

    public void unregisterCallback(Callback callback) {
        ILowpanManagerListener iLowpanManagerListener;
        Integer numValueOf = Integer.valueOf(System.identityHashCode(callback));
        synchronized (this.mListenerMap) {
            iLowpanManagerListener = this.mListenerMap.get(numValueOf);
            this.mListenerMap.remove(numValueOf);
        }
        if (iLowpanManagerListener != null) {
            try {
                this.mService.removeListener(iLowpanManagerListener);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        throw new RuntimeException("Attempt to unregister an unknown callback");
    }
}
