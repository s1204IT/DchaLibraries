package com.android.server.location;

import android.location.ILocationManager;
import android.location.Location;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class PassiveProvider implements LocationProviderInterface {
    private static final ProviderProperties PROPERTIES = new ProviderProperties(false, false, false, false, false, false, false, 1, 2);
    private static final String TAG = "PassiveProvider";
    private final ILocationManager mLocationManager;
    private boolean mReportLocation;

    public PassiveProvider(ILocationManager iLocationManager) {
        this.mLocationManager = iLocationManager;
    }

    @Override
    public String getName() {
        return "passive";
    }

    @Override
    public ProviderProperties getProperties() {
        return PROPERTIES;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable() {
    }

    @Override
    public int getStatus(Bundle bundle) {
        if (this.mReportLocation) {
            return 2;
        }
        return 1;
    }

    @Override
    public long getStatusUpdateTime() {
        return -1L;
    }

    @Override
    public void setRequest(ProviderRequest providerRequest, WorkSource workSource) {
        this.mReportLocation = providerRequest.reportLocation;
    }

    public void updateLocation(Location location) {
        if (this.mReportLocation) {
            try {
                this.mLocationManager.reportLocation(location, true);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException calling reportLocation");
            }
        }
    }

    @Override
    public boolean sendExtraCommand(String str, Bundle bundle) {
        return false;
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("mReportLocation=" + this.mReportLocation);
    }
}
