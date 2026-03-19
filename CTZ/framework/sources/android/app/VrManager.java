package android.app;

import android.annotation.SystemApi;
import android.app.VrManager;
import android.content.ComponentName;
import android.os.Handler;
import android.os.RemoteException;
import android.service.vr.IPersistentVrStateCallbacks;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.ArrayMap;
import java.util.Map;

@SystemApi
public class VrManager {
    private Map<VrStateCallback, CallbackEntry> mCallbackMap = new ArrayMap();
    private final IVrManager mService;

    private static class CallbackEntry {
        final VrStateCallback mCallback;
        final Handler mHandler;
        final IVrStateCallbacks mStateCallback = new AnonymousClass1();
        final IPersistentVrStateCallbacks mPersistentStateCallback = new AnonymousClass2();

        class AnonymousClass1 extends IVrStateCallbacks.Stub {
            AnonymousClass1() {
            }

            @Override
            public void onVrStateChanged(final boolean z) {
                CallbackEntry.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        VrManager.CallbackEntry.this.mCallback.onVrStateChanged(z);
                    }
                });
            }
        }

        class AnonymousClass2 extends IPersistentVrStateCallbacks.Stub {
            AnonymousClass2() {
            }

            @Override
            public void onPersistentVrStateChanged(final boolean z) {
                CallbackEntry.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        VrManager.CallbackEntry.this.mCallback.onPersistentVrStateChanged(z);
                    }
                });
            }
        }

        CallbackEntry(VrStateCallback vrStateCallback, Handler handler) {
            this.mCallback = vrStateCallback;
            this.mHandler = handler;
        }
    }

    public VrManager(IVrManager iVrManager) {
        this.mService = iVrManager;
    }

    public void registerVrStateCallback(VrStateCallback vrStateCallback, Handler handler) {
        if (vrStateCallback == null || this.mCallbackMap.containsKey(vrStateCallback)) {
            return;
        }
        CallbackEntry callbackEntry = new CallbackEntry(vrStateCallback, handler);
        this.mCallbackMap.put(vrStateCallback, callbackEntry);
        try {
            this.mService.registerListener(callbackEntry.mStateCallback);
            this.mService.registerPersistentVrStateListener(callbackEntry.mPersistentStateCallback);
        } catch (RemoteException e) {
            try {
                unregisterVrStateCallback(vrStateCallback);
            } catch (Exception e2) {
                e.rethrowFromSystemServer();
            }
        }
    }

    public void unregisterVrStateCallback(VrStateCallback vrStateCallback) {
        CallbackEntry callbackEntryRemove = this.mCallbackMap.remove(vrStateCallback);
        if (callbackEntryRemove != null) {
            try {
                this.mService.unregisterListener(callbackEntryRemove.mStateCallback);
            } catch (RemoteException e) {
            }
            try {
                this.mService.unregisterPersistentVrStateListener(callbackEntryRemove.mPersistentStateCallback);
            } catch (RemoteException e2) {
            }
        }
    }

    public boolean getVrModeEnabled() {
        try {
            return this.mService.getVrModeState();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    public boolean getPersistentVrModeEnabled() {
        try {
            return this.mService.getPersistentVrModeEnabled();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    public void setPersistentVrModeEnabled(boolean z) {
        try {
            this.mService.setPersistentVrModeEnabled(z);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void setVr2dDisplayProperties(Vr2dDisplayProperties vr2dDisplayProperties) {
        try {
            this.mService.setVr2dDisplayProperties(vr2dDisplayProperties);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void setAndBindVrCompositor(ComponentName componentName) {
        try {
            this.mService.setAndBindCompositor(componentName == null ? null : componentName.flattenToString());
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void setStandbyEnabled(boolean z) {
        try {
            this.mService.setStandbyEnabled(z);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void setVrInputMethod(ComponentName componentName) {
        try {
            this.mService.setVrInputMethod(componentName);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }
}
