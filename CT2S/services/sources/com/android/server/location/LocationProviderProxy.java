package com.android.server.location;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;
import com.android.internal.location.ILocationProvider;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.server.LocationManagerService;
import com.android.server.ServiceWatcher;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class LocationProviderProxy implements LocationProviderInterface {
    private static final boolean D = LocationManagerService.D;
    private static final String TAG = "LocationProviderProxy";
    private final Context mContext;
    private final String mName;
    private ProviderProperties mProperties;
    private final ServiceWatcher mServiceWatcher;
    private Object mLock = new Object();
    private boolean mEnabled = false;
    private ProviderRequest mRequest = null;
    private WorkSource mWorksource = new WorkSource();
    private Runnable mNewServiceWork = new Runnable() {
        @Override
        public void run() {
            boolean enabled;
            ProviderRequest request;
            WorkSource source;
            ILocationProvider service;
            if (LocationProviderProxy.D) {
                Log.d(LocationProviderProxy.TAG, "applying state to connected service");
            }
            ProviderProperties properties = null;
            synchronized (LocationProviderProxy.this.mLock) {
                enabled = LocationProviderProxy.this.mEnabled;
                request = LocationProviderProxy.this.mRequest;
                source = LocationProviderProxy.this.mWorksource;
                service = LocationProviderProxy.this.getService();
            }
            if (service != null) {
                try {
                    properties = service.getProperties();
                    if (properties == null) {
                        Log.e(LocationProviderProxy.TAG, LocationProviderProxy.this.mServiceWatcher.getBestPackageName() + " has invalid locatino provider properties");
                    }
                    if (enabled) {
                        service.enable();
                        if (request != null) {
                            service.setRequest(request, source);
                        }
                    }
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    Log.e(LocationProviderProxy.TAG, "Exception from " + LocationProviderProxy.this.mServiceWatcher.getBestPackageName(), e2);
                }
                synchronized (LocationProviderProxy.this.mLock) {
                    LocationProviderProxy.this.mProperties = properties;
                }
            }
        }
    };

    public static LocationProviderProxy createAndBind(Context context, String name, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Handler handler) {
        LocationProviderProxy proxy = new LocationProviderProxy(context, name, action, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, handler);
        if (proxy.bind()) {
            return proxy;
        }
        return null;
    }

    private LocationProviderProxy(Context context, String name, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Handler handler) {
        this.mContext = context;
        this.mName = name;
        this.mServiceWatcher = new ServiceWatcher(this.mContext, "LocationProviderProxy-" + name, action, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, this.mNewServiceWork, handler);
    }

    private boolean bind() {
        return this.mServiceWatcher.start();
    }

    private ILocationProvider getService() {
        return ILocationProvider.Stub.asInterface(this.mServiceWatcher.getBinder());
    }

    public String getConnectedPackageName() {
        return this.mServiceWatcher.getBestPackageName();
    }

    @Override
    public String getName() {
        return this.mName;
    }

    @Override
    public ProviderProperties getProperties() {
        ProviderProperties providerProperties;
        synchronized (this.mLock) {
            providerProperties = this.mProperties;
        }
        return providerProperties;
    }

    @Override
    public void enable() {
        synchronized (this.mLock) {
            this.mEnabled = true;
        }
        ILocationProvider service = getService();
        if (service != null) {
            try {
                service.enable();
            } catch (RemoteException e) {
                Log.w(TAG, e);
            } catch (Exception e2) {
                Log.e(TAG, "Exception from " + this.mServiceWatcher.getBestPackageName(), e2);
            }
        }
    }

    @Override
    public void disable() {
        synchronized (this.mLock) {
            this.mEnabled = false;
        }
        ILocationProvider service = getService();
        if (service != null) {
            try {
                service.disable();
            } catch (RemoteException e) {
                Log.w(TAG, e);
            } catch (Exception e2) {
                Log.e(TAG, "Exception from " + this.mServiceWatcher.getBestPackageName(), e2);
            }
        }
    }

    @Override
    public boolean isEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mEnabled;
        }
        return z;
    }

    @Override
    public void setRequest(ProviderRequest request, WorkSource source) {
        synchronized (this.mLock) {
            this.mRequest = request;
            this.mWorksource = source;
        }
        ILocationProvider service = getService();
        if (service != null) {
            try {
                service.setRequest(request, source);
            } catch (RemoteException e) {
                Log.w(TAG, e);
            } catch (Exception e2) {
                Log.e(TAG, "Exception from " + this.mServiceWatcher.getBestPackageName(), e2);
            }
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.append("REMOTE SERVICE");
        pw.append(" name=").append((CharSequence) this.mName);
        pw.append(" pkg=").append((CharSequence) this.mServiceWatcher.getBestPackageName());
        pw.append(" version=").append((CharSequence) ("" + this.mServiceWatcher.getBestVersion()));
        pw.append('\n');
        ILocationProvider service = getService();
        if (service == null) {
            pw.println("service down (null)");
            return;
        }
        pw.flush();
        try {
            service.asBinder().dump(fd, args);
        } catch (RemoteException e) {
            pw.println("service down (RemoteException)");
            Log.w(TAG, e);
        } catch (Exception e2) {
            pw.println("service down (Exception)");
            Log.e(TAG, "Exception from " + this.mServiceWatcher.getBestPackageName(), e2);
        }
    }

    @Override
    public int getStatus(Bundle extras) {
        ILocationProvider service = getService();
        if (service == null) {
            return 1;
        }
        try {
            return service.getStatus(extras);
        } catch (RemoteException e) {
            Log.w(TAG, e);
            return 1;
        } catch (Exception e2) {
            Log.e(TAG, "Exception from " + this.mServiceWatcher.getBestPackageName(), e2);
            return 1;
        }
    }

    @Override
    public long getStatusUpdateTime() {
        ILocationProvider service = getService();
        if (service == null) {
            return 0L;
        }
        try {
            return service.getStatusUpdateTime();
        } catch (RemoteException e) {
            Log.w(TAG, e);
            return 0L;
        } catch (Exception e2) {
            Log.e(TAG, "Exception from " + this.mServiceWatcher.getBestPackageName(), e2);
            return 0L;
        }
    }

    @Override
    public boolean sendExtraCommand(String command, Bundle extras) {
        ILocationProvider service = getService();
        if (service == null) {
            return false;
        }
        try {
            return service.sendExtraCommand(command, extras);
        } catch (RemoteException e) {
            Log.w(TAG, e);
            return false;
        } catch (Exception e2) {
            Log.e(TAG, "Exception from " + this.mServiceWatcher.getBestPackageName(), e2);
            return false;
        }
    }
}
