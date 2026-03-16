package com.android.server.location;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.location.GeofenceHardwareService;
import android.hardware.location.IGeofenceHardware;
import android.location.IFusedGeofenceHardware;
import android.location.IGeofenceProvider;
import android.location.IGpsGeofenceHardware;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.ServiceWatcher;

public final class GeofenceProxy {
    private static final int GEOFENCE_GPS_HARDWARE_CONNECTED = 4;
    private static final int GEOFENCE_GPS_HARDWARE_DISCONNECTED = 5;
    private static final int GEOFENCE_HARDWARE_CONNECTED = 2;
    private static final int GEOFENCE_HARDWARE_DISCONNECTED = 3;
    private static final int GEOFENCE_PROVIDER_CONNECTED = 1;
    private static final String SERVICE_ACTION = "com.android.location.service.GeofenceProvider";
    private static final String TAG = "GeofenceProxy";
    private final Context mContext;
    private final IFusedGeofenceHardware mFusedGeofenceHardware;
    private IGeofenceHardware mGeofenceHardware;
    private final IGpsGeofenceHardware mGpsGeofenceHardware;
    private final ServiceWatcher mServiceWatcher;
    private final Object mLock = new Object();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            GeofenceProxy.this.mHandler.sendEmptyMessage(1);
        }
    };
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (GeofenceProxy.this.mLock) {
                GeofenceProxy.this.mGeofenceHardware = IGeofenceHardware.Stub.asInterface(service);
                GeofenceProxy.this.mHandler.sendEmptyMessage(2);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (GeofenceProxy.this.mLock) {
                GeofenceProxy.this.mGeofenceHardware = null;
                GeofenceProxy.this.mHandler.sendEmptyMessage(3);
            }
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (GeofenceProxy.this.mLock) {
                        if (GeofenceProxy.this.mGeofenceHardware != null) {
                            GeofenceProxy.this.setGeofenceHardwareInProviderLocked();
                        }
                        break;
                    }
                    return;
                case 2:
                    synchronized (GeofenceProxy.this.mLock) {
                        if (GeofenceProxy.this.mGeofenceHardware != null) {
                            GeofenceProxy.this.setGpsGeofenceLocked();
                            GeofenceProxy.this.setFusedGeofenceLocked();
                            GeofenceProxy.this.setGeofenceHardwareInProviderLocked();
                        }
                        break;
                    }
                    return;
                case 3:
                    synchronized (GeofenceProxy.this.mLock) {
                        if (GeofenceProxy.this.mGeofenceHardware == null) {
                            GeofenceProxy.this.setGeofenceHardwareInProviderLocked();
                        }
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
    };

    public static GeofenceProxy createAndBind(Context context, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Handler handler, IGpsGeofenceHardware gpsGeofence, IFusedGeofenceHardware fusedGeofenceHardware) {
        GeofenceProxy proxy = new GeofenceProxy(context, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, handler, gpsGeofence, fusedGeofenceHardware);
        if (proxy.bindGeofenceProvider()) {
            return proxy;
        }
        return null;
    }

    private GeofenceProxy(Context context, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Handler handler, IGpsGeofenceHardware gpsGeofence, IFusedGeofenceHardware fusedGeofenceHardware) {
        this.mContext = context;
        this.mServiceWatcher = new ServiceWatcher(context, TAG, SERVICE_ACTION, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, this.mRunnable, handler);
        this.mGpsGeofenceHardware = gpsGeofence;
        this.mFusedGeofenceHardware = fusedGeofenceHardware;
        bindHardwareGeofence();
    }

    private boolean bindGeofenceProvider() {
        return this.mServiceWatcher.start();
    }

    private void bindHardwareGeofence() {
        this.mContext.bindServiceAsUser(new Intent(this.mContext, (Class<?>) GeofenceHardwareService.class), this.mServiceConnection, 1, UserHandle.OWNER);
    }

    private void setGeofenceHardwareInProviderLocked() {
        try {
            IGeofenceProvider provider = IGeofenceProvider.Stub.asInterface(this.mServiceWatcher.getBinder());
            if (provider != null) {
                provider.setGeofenceHardware(this.mGeofenceHardware);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Remote Exception: setGeofenceHardwareInProviderLocked: " + e);
        }
    }

    private void setGpsGeofenceLocked() {
        try {
            this.mGeofenceHardware.setGpsGeofenceHardware(this.mGpsGeofenceHardware);
        } catch (RemoteException e) {
            Log.e(TAG, "Error while connecting to GeofenceHardwareService");
        }
    }

    private void setFusedGeofenceLocked() {
        try {
            this.mGeofenceHardware.setFusedGeofenceHardware(this.mFusedGeofenceHardware);
        } catch (RemoteException e) {
            Log.e(TAG, "Error while connecting to GeofenceHardwareService");
        }
    }
}
