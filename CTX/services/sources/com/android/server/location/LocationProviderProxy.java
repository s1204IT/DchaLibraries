package com.android.server.location;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;
import com.android.internal.location.ILocationProvider;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.internal.os.TransferPipe;
import com.android.server.LocationManagerService;
import com.android.server.ServiceWatcher;
import com.android.server.backup.BackupManagerConstants;
import java.io.FileDescriptor;
import java.io.IOException;
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
            final boolean z;
            final ProviderRequest providerRequest;
            final WorkSource workSource;
            if (LocationProviderProxy.D) {
                Log.d(LocationProviderProxy.TAG, "applying state to connected service");
            }
            final ProviderProperties[] providerPropertiesArr = new ProviderProperties[1];
            synchronized (LocationProviderProxy.this.mLock) {
                z = LocationProviderProxy.this.mEnabled;
                providerRequest = LocationProviderProxy.this.mRequest;
                workSource = LocationProviderProxy.this.mWorksource;
            }
            LocationProviderProxy.this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderRunner() {
                @Override
                public void run(IBinder iBinder) {
                    ILocationProvider iLocationProviderAsInterface = ILocationProvider.Stub.asInterface(iBinder);
                    try {
                        providerPropertiesArr[0] = iLocationProviderAsInterface.getProperties();
                        if (providerPropertiesArr[0] == null) {
                            Log.e(LocationProviderProxy.TAG, LocationProviderProxy.this.mServiceWatcher.getBestPackageName() + " has invalid location provider properties");
                        }
                        if (z) {
                            iLocationProviderAsInterface.enable();
                            if (providerRequest != null) {
                                iLocationProviderAsInterface.setRequest(providerRequest, workSource);
                            }
                        }
                    } catch (RemoteException e) {
                        Log.w(LocationProviderProxy.TAG, e);
                    } catch (Exception e2) {
                        Log.e(LocationProviderProxy.TAG, "Exception from " + LocationProviderProxy.this.mServiceWatcher.getBestPackageName(), e2);
                    }
                }
            });
            synchronized (LocationProviderProxy.this.mLock) {
                LocationProviderProxy.this.mProperties = providerPropertiesArr[0];
            }
        }
    };

    public static LocationProviderProxy createAndBind(Context context, String str, String str2, int i, int i2, int i3, Handler handler) {
        LocationProviderProxy locationProviderProxy = new LocationProviderProxy(context, str, str2, i, i2, i3, handler);
        if (locationProviderProxy.bind()) {
            return locationProviderProxy;
        }
        return null;
    }

    private LocationProviderProxy(Context context, String str, String str2, int i, int i2, int i3, Handler handler) {
        this.mContext = context;
        this.mName = str;
        this.mServiceWatcher = new ServiceWatcher(this.mContext, "LocationProviderProxy-" + str, str2, i, i2, i3, this.mNewServiceWork, handler);
    }

    private boolean bind() {
        return this.mServiceWatcher.start();
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
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderRunner() {
            @Override
            public void run(IBinder iBinder) {
                try {
                    ILocationProvider.Stub.asInterface(iBinder).enable();
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    Log.e(LocationProviderProxy.TAG, "Exception from " + LocationProviderProxy.this.mServiceWatcher.getBestPackageName(), e2);
                }
            }
        });
    }

    @Override
    public void disable() {
        synchronized (this.mLock) {
            this.mEnabled = false;
        }
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderRunner() {
            @Override
            public void run(IBinder iBinder) {
                try {
                    ILocationProvider.Stub.asInterface(iBinder).disable();
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    Log.e(LocationProviderProxy.TAG, "Exception from " + LocationProviderProxy.this.mServiceWatcher.getBestPackageName(), e2);
                }
            }
        });
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
    public void setRequest(final ProviderRequest providerRequest, final WorkSource workSource) {
        synchronized (this.mLock) {
            this.mRequest = providerRequest;
            this.mWorksource = workSource;
        }
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderRunner() {
            @Override
            public void run(IBinder iBinder) {
                try {
                    ILocationProvider.Stub.asInterface(iBinder).setRequest(providerRequest, workSource);
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    Log.e(LocationProviderProxy.TAG, "Exception from " + LocationProviderProxy.this.mServiceWatcher.getBestPackageName(), e2);
                }
            }
        });
    }

    @Override
    public void dump(final FileDescriptor fileDescriptor, final PrintWriter printWriter, final String[] strArr) {
        printWriter.append("REMOTE SERVICE");
        printWriter.append(" name=").append((CharSequence) this.mName);
        printWriter.append(" pkg=").append((CharSequence) this.mServiceWatcher.getBestPackageName());
        printWriter.append(" version=").append((CharSequence) (BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + this.mServiceWatcher.getBestVersion()));
        printWriter.append('\n');
        if (!this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderRunner() {
            @Override
            public void run(IBinder iBinder) {
                try {
                    TransferPipe.dumpAsync(ILocationProvider.Stub.asInterface(iBinder).asBinder(), fileDescriptor, strArr);
                } catch (RemoteException | IOException e) {
                    printWriter.println("Failed to dump location provider: " + e);
                }
            }
        })) {
            printWriter.println("service down (null)");
        }
    }

    @Override
    public int getStatus(final Bundle bundle) {
        final int[] iArr = {1};
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderRunner() {
            @Override
            public void run(IBinder iBinder) {
                try {
                    iArr[0] = ILocationProvider.Stub.asInterface(iBinder).getStatus(bundle);
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    Log.e(LocationProviderProxy.TAG, "Exception from " + LocationProviderProxy.this.mServiceWatcher.getBestPackageName(), e2);
                }
            }
        });
        return iArr[0];
    }

    @Override
    public long getStatusUpdateTime() {
        final long[] jArr = {0};
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderRunner() {
            @Override
            public void run(IBinder iBinder) {
                try {
                    jArr[0] = ILocationProvider.Stub.asInterface(iBinder).getStatusUpdateTime();
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    Log.e(LocationProviderProxy.TAG, "Exception from " + LocationProviderProxy.this.mServiceWatcher.getBestPackageName(), e2);
                }
            }
        });
        return jArr[0];
    }

    @Override
    public boolean sendExtraCommand(final String str, final Bundle bundle) {
        final boolean[] zArr = {false};
        this.mServiceWatcher.runOnBinder(new ServiceWatcher.BinderRunner() {
            @Override
            public void run(IBinder iBinder) {
                try {
                    zArr[0] = ILocationProvider.Stub.asInterface(iBinder).sendExtraCommand(str, bundle);
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    Log.e(LocationProviderProxy.TAG, "Exception from " + LocationProviderProxy.this.mServiceWatcher.getBestPackageName(), e2);
                }
            }
        });
        return zArr[0];
    }
}
