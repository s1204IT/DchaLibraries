package com.android.systemui.qs.external;

import android.os.IBinder;
import android.service.quicksettings.IQSTileService;
import android.util.Log;
/* loaded from: a.zip:com/android/systemui/qs/external/QSTileServiceWrapper.class */
public class QSTileServiceWrapper {
    private final IQSTileService mService;

    public QSTileServiceWrapper(IQSTileService iQSTileService) {
        this.mService = iQSTileService;
    }

    public IBinder asBinder() {
        return this.mService.asBinder();
    }

    public boolean onClick(IBinder iBinder) {
        try {
            this.mService.onClick(iBinder);
            return true;
        } catch (Exception e) {
            Log.d("IQSTileServiceWrapper", "Caught exception from TileService", e);
            return false;
        }
    }

    public boolean onStartListening() {
        try {
            this.mService.onStartListening();
            return true;
        } catch (Exception e) {
            Log.d("IQSTileServiceWrapper", "Caught exception from TileService", e);
            return false;
        }
    }

    public boolean onStopListening() {
        try {
            this.mService.onStopListening();
            return true;
        } catch (Exception e) {
            Log.d("IQSTileServiceWrapper", "Caught exception from TileService", e);
            return false;
        }
    }

    public boolean onTileAdded() {
        try {
            this.mService.onTileAdded();
            return true;
        } catch (Exception e) {
            Log.d("IQSTileServiceWrapper", "Caught exception from TileService", e);
            return false;
        }
    }

    public boolean onTileRemoved() {
        try {
            this.mService.onTileRemoved();
            return true;
        } catch (Exception e) {
            Log.d("IQSTileServiceWrapper", "Caught exception from TileService", e);
            return false;
        }
    }

    public boolean onUnlockComplete() {
        try {
            this.mService.onUnlockComplete();
            return true;
        } catch (Exception e) {
            Log.d("IQSTileServiceWrapper", "Caught exception from TileService", e);
            return false;
        }
    }
}
