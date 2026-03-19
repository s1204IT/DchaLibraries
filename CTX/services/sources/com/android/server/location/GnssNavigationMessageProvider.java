package com.android.server.location;

import android.location.GnssNavigationMessage;
import android.location.IGnssNavigationMessageListener;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.location.RemoteListenerHelper;

public abstract class GnssNavigationMessageProvider extends RemoteListenerHelper<IGnssNavigationMessageListener> {
    private boolean mCollectionStarted;
    private final GnssNavigationMessageProviderNative mNative;
    private static final String TAG = "GnssNavigationMessageProvider";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    private static native boolean native_is_navigation_message_supported();

    private static native boolean native_start_navigation_message_collection();

    private static native boolean native_stop_navigation_message_collection();

    @Override
    public boolean isRegistered() {
        return super.isRegistered();
    }

    protected GnssNavigationMessageProvider(Handler handler) {
        this(handler, new GnssNavigationMessageProviderNative());
    }

    @VisibleForTesting
    GnssNavigationMessageProvider(Handler handler, GnssNavigationMessageProviderNative gnssNavigationMessageProviderNative) {
        super(handler, TAG);
        this.mNative = gnssNavigationMessageProviderNative;
    }

    void resumeIfStarted() {
        if (DEBUG) {
            Log.d(TAG, "resumeIfStarted");
        }
        if (this.mCollectionStarted) {
            this.mNative.startNavigationMessageCollection();
        }
    }

    @Override
    protected boolean isAvailableInPlatform() {
        return this.mNative.isNavigationMessageSupported();
    }

    @Override
    protected int registerWithService() {
        if (this.mNative.startNavigationMessageCollection()) {
            this.mCollectionStarted = true;
            return 0;
        }
        return 4;
    }

    @Override
    protected void unregisterFromService() {
        if (this.mNative.stopNavigationMessageCollection()) {
            this.mCollectionStarted = false;
        }
    }

    public void onNavigationMessageAvailable(final GnssNavigationMessage gnssNavigationMessage) {
        foreach(new RemoteListenerHelper.ListenerOperation<IGnssNavigationMessageListener>() {
            @Override
            public void execute(IGnssNavigationMessageListener iGnssNavigationMessageListener) throws RemoteException {
                iGnssNavigationMessageListener.onGnssNavigationMessageReceived(gnssNavigationMessage);
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
    protected RemoteListenerHelper.ListenerOperation<IGnssNavigationMessageListener> getHandlerOperation(int i) {
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
            default:
                Log.v(TAG, "Unhandled addListener result: " + i);
                return null;
        }
        return new StatusChangedOperation(i2);
    }

    private static class StatusChangedOperation implements RemoteListenerHelper.ListenerOperation<IGnssNavigationMessageListener> {
        private final int mStatus;

        public StatusChangedOperation(int i) {
            this.mStatus = i;
        }

        @Override
        public void execute(IGnssNavigationMessageListener iGnssNavigationMessageListener) throws RemoteException {
            iGnssNavigationMessageListener.onStatusChanged(this.mStatus);
        }
    }

    @VisibleForTesting
    static class GnssNavigationMessageProviderNative {
        GnssNavigationMessageProviderNative() {
        }

        public boolean isNavigationMessageSupported() {
            return GnssNavigationMessageProvider.native_is_navigation_message_supported();
        }

        public boolean startNavigationMessageCollection() {
            return GnssNavigationMessageProvider.native_start_navigation_message_collection();
        }

        public boolean stopNavigationMessageCollection() {
            return GnssNavigationMessageProvider.native_stop_navigation_message_collection();
        }
    }
}
