package com.android.location.provider;

import android.location.ILocationManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.WorkSource;
import android.util.Log;
import com.android.internal.location.ILocationProvider;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.internal.util.FastPrintWriter;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public abstract class LocationProviderBase {
    public static final String EXTRA_NO_GPS_LOCATION = "noGPSLocation";
    public static final String FUSED_PROVIDER = "fused";
    private final String TAG;
    private final ProviderProperties mProperties;
    protected final ILocationManager mLocationManager = ILocationManager.Stub.asInterface(ServiceManager.getService("location"));
    private final IBinder mBinder = new Service();

    public abstract void onDisable();

    public abstract void onEnable();

    public abstract int onGetStatus(Bundle bundle);

    public abstract long onGetStatusUpdateTime();

    public abstract void onSetRequest(ProviderRequestUnbundled providerRequestUnbundled, WorkSource workSource);

    private final class Service extends ILocationProvider.Stub {
        private Service() {
        }

        public void enable() {
            LocationProviderBase.this.onEnable();
        }

        public void disable() {
            LocationProviderBase.this.onDisable();
        }

        public void setRequest(ProviderRequest providerRequest, WorkSource workSource) {
            LocationProviderBase.this.onSetRequest(new ProviderRequestUnbundled(providerRequest), workSource);
        }

        public ProviderProperties getProperties() {
            return LocationProviderBase.this.mProperties;
        }

        public int getStatus(Bundle bundle) {
            return LocationProviderBase.this.onGetStatus(bundle);
        }

        public long getStatusUpdateTime() {
            return LocationProviderBase.this.onGetStatusUpdateTime();
        }

        public boolean sendExtraCommand(String str, Bundle bundle) {
            return LocationProviderBase.this.onSendExtraCommand(str, bundle);
        }

        public void dump(FileDescriptor fileDescriptor, String[] strArr) {
            PrintWriter fastPrintWriter = new FastPrintWriter(new FileOutputStream(fileDescriptor));
            LocationProviderBase.this.onDump(fileDescriptor, fastPrintWriter, strArr);
            fastPrintWriter.flush();
        }
    }

    public LocationProviderBase(String str, ProviderPropertiesUnbundled providerPropertiesUnbundled) {
        this.TAG = str;
        this.mProperties = providerPropertiesUnbundled.getProviderProperties();
    }

    public IBinder getBinder() {
        return this.mBinder;
    }

    public final void reportLocation(Location location) {
        try {
            this.mLocationManager.reportLocation(location, false);
        } catch (RemoteException e) {
            Log.e(this.TAG, "RemoteException", e);
        } catch (Exception e2) {
            Log.e(this.TAG, "Exception", e2);
        }
    }

    public void onDump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
    }

    public boolean onSendExtraCommand(String str, Bundle bundle) {
        return false;
    }
}
