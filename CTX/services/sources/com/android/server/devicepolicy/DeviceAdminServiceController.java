package com.android.server.devicepolicy;

import android.app.admin.IDeviceAdminService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BackgroundThread;
import com.android.server.am.PersistentConnection;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import java.io.PrintWriter;
import java.util.List;

public class DeviceAdminServiceController {
    static final boolean DEBUG = false;
    static final String TAG = "DevicePolicyManager";
    private final DevicePolicyConstants mConstants;
    final Context mContext;
    private final DevicePolicyManagerService.Injector mInjector;
    private final DevicePolicyManagerService mService;
    final Object mLock = new Object();

    @GuardedBy("mLock")
    private final SparseArray<DevicePolicyServiceConnection> mConnections = new SparseArray<>();
    private final Handler mHandler = new Handler(BackgroundThread.get().getLooper());

    static void debug(String str, Object... objArr) {
    }

    private class DevicePolicyServiceConnection extends PersistentConnection<IDeviceAdminService> {
        public DevicePolicyServiceConnection(int i, ComponentName componentName) {
            super(DeviceAdminServiceController.TAG, DeviceAdminServiceController.this.mContext, DeviceAdminServiceController.this.mHandler, i, componentName, DeviceAdminServiceController.this.mConstants.DAS_DIED_SERVICE_RECONNECT_BACKOFF_SEC, DeviceAdminServiceController.this.mConstants.DAS_DIED_SERVICE_RECONNECT_BACKOFF_INCREASE, DeviceAdminServiceController.this.mConstants.DAS_DIED_SERVICE_RECONNECT_MAX_BACKOFF_SEC);
        }

        @Override
        protected IDeviceAdminService asInterface(IBinder iBinder) {
            return IDeviceAdminService.Stub.asInterface(iBinder);
        }
    }

    public DeviceAdminServiceController(DevicePolicyManagerService devicePolicyManagerService, DevicePolicyConstants devicePolicyConstants) {
        this.mService = devicePolicyManagerService;
        this.mInjector = devicePolicyManagerService.mInjector;
        this.mContext = this.mInjector.mContext;
        this.mConstants = devicePolicyConstants;
    }

    private ServiceInfo findService(String str, int i) {
        Intent intent = new Intent("android.app.action.DEVICE_ADMIN_SERVICE");
        intent.setPackage(str);
        try {
            ParceledListSlice parceledListSliceQueryIntentServices = this.mInjector.getIPackageManager().queryIntentServices(intent, (String) null, 0, i);
            if (parceledListSliceQueryIntentServices == null) {
                return null;
            }
            List list = parceledListSliceQueryIntentServices.getList();
            if (list.size() == 0) {
                return null;
            }
            if (list.size() > 1) {
                Log.e(TAG, "More than one DeviceAdminService's found in package " + str + ".  They'll all be ignored.");
                return null;
            }
            ServiceInfo serviceInfo = ((ResolveInfo) list.get(0)).serviceInfo;
            if (!"android.permission.BIND_DEVICE_ADMIN".equals(serviceInfo.permission)) {
                Log.e(TAG, "DeviceAdminService " + serviceInfo.getComponentName().flattenToShortString() + " must be protected with android.permission.BIND_DEVICE_ADMIN.");
                return null;
            }
            return serviceInfo;
        } catch (RemoteException e) {
            return null;
        }
    }

    public void startServiceForOwner(String str, int i, String str2) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (this.mLock) {
                ServiceInfo serviceInfoFindService = findService(str, i);
                if (serviceInfoFindService == null) {
                    debug("Owner package %s on u%d has no service.", str, Integer.valueOf(i));
                    disconnectServiceOnUserLocked(i, str2);
                    return;
                }
                if (this.mConnections.get(i) != null) {
                    debug("Disconnecting from existing service connection.", str, Integer.valueOf(i));
                    disconnectServiceOnUserLocked(i, str2);
                }
                debug("Owner package %s on u%d has service %s for %s", str, Integer.valueOf(i), serviceInfoFindService.getComponentName().flattenToShortString(), str2);
                DevicePolicyServiceConnection devicePolicyServiceConnection = new DevicePolicyServiceConnection(i, serviceInfoFindService.getComponentName());
                this.mConnections.put(i, devicePolicyServiceConnection);
                devicePolicyServiceConnection.bind();
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void stopServiceForOwner(int i, String str) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (this.mLock) {
                disconnectServiceOnUserLocked(i, str);
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    @GuardedBy("mLock")
    private void disconnectServiceOnUserLocked(int i, String str) {
        DevicePolicyServiceConnection devicePolicyServiceConnection = this.mConnections.get(i);
        if (devicePolicyServiceConnection != null) {
            debug("Stopping service for u%d if already running for %s.", Integer.valueOf(i), str);
            devicePolicyServiceConnection.unbind();
            this.mConnections.remove(i);
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        synchronized (this.mLock) {
            if (this.mConnections.size() == 0) {
                return;
            }
            printWriter.println();
            printWriter.print(str);
            printWriter.println("Owner Services:");
            for (int i = 0; i < this.mConnections.size(); i++) {
                int iKeyAt = this.mConnections.keyAt(i);
                printWriter.print(str);
                printWriter.print("  ");
                printWriter.print("User: ");
                printWriter.println(iKeyAt);
                this.mConnections.valueAt(i).dump(str + "    ", printWriter);
            }
            printWriter.println();
        }
    }
}
