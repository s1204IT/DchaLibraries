package com.android.server.location;

import android.location.ILocationManager;
import android.location.Location;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;
import android.util.PrintWriterPrinter;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.server.backup.BackupManagerConstants;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class MockProvider implements LocationProviderInterface {
    private static final String TAG = "MockProvider";
    private boolean mEnabled;
    private final Bundle mExtras = new Bundle();
    private boolean mHasLocation;
    private boolean mHasStatus;
    private final Location mLocation;
    private final ILocationManager mLocationManager;
    private final String mName;
    private final ProviderProperties mProperties;
    private int mStatus;
    private long mStatusUpdateTime;

    public MockProvider(String str, ILocationManager iLocationManager, ProviderProperties providerProperties) {
        if (providerProperties == null) {
            throw new NullPointerException("properties is null");
        }
        this.mName = str;
        this.mLocationManager = iLocationManager;
        this.mProperties = providerProperties;
        this.mLocation = new Location(str);
    }

    @Override
    public String getName() {
        return this.mName;
    }

    @Override
    public ProviderProperties getProperties() {
        return this.mProperties;
    }

    @Override
    public void disable() {
        this.mEnabled = false;
    }

    @Override
    public void enable() {
        this.mEnabled = true;
    }

    @Override
    public boolean isEnabled() {
        return this.mEnabled;
    }

    @Override
    public int getStatus(Bundle bundle) {
        if (this.mHasStatus) {
            bundle.clear();
            bundle.putAll(this.mExtras);
            return this.mStatus;
        }
        return 2;
    }

    @Override
    public long getStatusUpdateTime() {
        return this.mStatusUpdateTime;
    }

    public void setLocation(Location location) {
        this.mLocation.set(location);
        this.mHasLocation = true;
        if (this.mEnabled) {
            try {
                this.mLocationManager.reportLocation(this.mLocation, false);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException calling reportLocation");
            }
        }
    }

    public void clearLocation() {
        this.mHasLocation = false;
    }

    public void setStatus(int i, Bundle bundle, long j) {
        this.mStatus = i;
        this.mStatusUpdateTime = j;
        this.mExtras.clear();
        if (bundle != null) {
            this.mExtras.putAll(bundle);
        }
        this.mHasStatus = true;
    }

    public void clearStatus() {
        this.mHasStatus = false;
        this.mStatusUpdateTime = 0L;
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        dump(printWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + this.mName);
        printWriter.println(str + "mHasLocation=" + this.mHasLocation);
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("mLocation:");
        printWriter.println(sb.toString());
        this.mLocation.dump(new PrintWriterPrinter(printWriter), str + "  ");
        printWriter.println(str + "mHasStatus=" + this.mHasStatus);
        printWriter.println(str + "mStatus=" + this.mStatus);
        printWriter.println(str + "mStatusUpdateTime=" + this.mStatusUpdateTime);
        printWriter.println(str + "mExtras=" + this.mExtras);
    }

    @Override
    public void setRequest(ProviderRequest providerRequest, WorkSource workSource) {
    }

    @Override
    public boolean sendExtraCommand(String str, Bundle bundle) {
        return false;
    }
}
