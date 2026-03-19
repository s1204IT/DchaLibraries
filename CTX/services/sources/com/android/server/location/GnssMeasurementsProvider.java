package com.android.server.location;

import android.content.Context;
import android.location.GnssMeasurementsEvent;
import android.location.IGnssMeasurementsListener;
import android.os.Handler;
import android.os.IInterface;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.location.RemoteListenerHelper;

public abstract class GnssMeasurementsProvider extends RemoteListenerHelper<IGnssMeasurementsListener> {
    private final Context mContext;
    private boolean mEnableFullTracking;
    private boolean mIsCollectionStarted;
    private final GnssMeasurementProviderNative mNative;
    private static final String TAG = "GnssMeasurementsProvider";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    private static native boolean native_is_measurement_supported();

    private static native boolean native_start_measurement_collection(boolean z);

    private static native boolean native_stop_measurement_collection();

    @Override
    public boolean isRegistered() {
        return super.isRegistered();
    }

    protected GnssMeasurementsProvider(Context context, Handler handler) {
        this(context, handler, new GnssMeasurementProviderNative());
    }

    @VisibleForTesting
    GnssMeasurementsProvider(Context context, Handler handler, GnssMeasurementProviderNative gnssMeasurementProviderNative) {
        super(handler, TAG);
        this.mContext = context;
        this.mNative = gnssMeasurementProviderNative;
    }

    void resumeIfStarted() {
        if (DEBUG) {
            Log.d(TAG, "resumeIfStarted");
        }
        if (this.mIsCollectionStarted) {
            this.mNative.startMeasurementCollection(this.mEnableFullTracking);
        }
    }

    @Override
    public boolean isAvailableInPlatform() {
        return this.mNative.isMeasurementSupported();
    }

    @Override
    protected int registerWithService() {
        boolean z = Settings.Secure.getInt(this.mContext.getContentResolver(), "development_settings_enabled", 0) == 1 && Settings.Global.getInt(this.mContext.getContentResolver(), "enable_gnss_raw_meas_full_tracking", 0) == 1;
        if (this.mNative.startMeasurementCollection(z)) {
            this.mIsCollectionStarted = true;
            this.mEnableFullTracking = z;
            return 0;
        }
        return 4;
    }

    @Override
    protected void unregisterFromService() {
        if (this.mNative.stopMeasurementCollection()) {
            this.mIsCollectionStarted = false;
        }
    }

    public void onMeasurementsAvailable(final GnssMeasurementsEvent gnssMeasurementsEvent) {
        foreach(new RemoteListenerHelper.ListenerOperation() {
            @Override
            public final void execute(IInterface iInterface) {
                ((IGnssMeasurementsListener) iInterface).onGnssMeasurementsReceived(gnssMeasurementsEvent);
            }
        });
    }

    public void onCapabilitiesUpdated(boolean z) {
        setSupported(z);
        updateResult();
    }

    public void onGpsEnabledChanged() {
        tryUpdateRegistrationWithService();
        updateResult();
    }

    @Override
    protected RemoteListenerHelper.ListenerOperation<IGnssMeasurementsListener> getHandlerOperation(int i) {
        int i2;
        switch (i) {
            case 0:
                i2 = 1;
                break;
            case 1:
            case 2:
            case 4:
                i2 = 0;
                break;
            case 3:
                i2 = 2;
                break;
            case 5:
                return null;
            case 6:
                i2 = 3;
                break;
            default:
                Log.v(TAG, "Unhandled addListener result: " + i);
                return null;
        }
        return new StatusChangedOperation(i2);
    }

    private static class StatusChangedOperation implements RemoteListenerHelper.ListenerOperation<IGnssMeasurementsListener> {
        private final int mStatus;

        public StatusChangedOperation(int i) {
            this.mStatus = i;
        }

        @Override
        public void execute(IGnssMeasurementsListener iGnssMeasurementsListener) throws RemoteException {
            iGnssMeasurementsListener.onStatusChanged(this.mStatus);
        }
    }

    @VisibleForTesting
    static class GnssMeasurementProviderNative {
        GnssMeasurementProviderNative() {
        }

        public boolean isMeasurementSupported() {
            return GnssMeasurementsProvider.native_is_measurement_supported();
        }

        public boolean startMeasurementCollection(boolean z) {
            return GnssMeasurementsProvider.native_start_measurement_collection(z);
        }

        public boolean stopMeasurementCollection() {
            return GnssMeasurementsProvider.native_stop_measurement_collection();
        }
    }
}
