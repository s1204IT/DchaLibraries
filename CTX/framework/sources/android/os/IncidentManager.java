package android.os;

import android.annotation.SystemApi;
import android.content.Context;
import android.os.IBinder;
import android.os.IIncidentManager;
import android.util.Slog;

@SystemApi
public class IncidentManager {
    private static final String TAG = "IncidentManager";
    private final Context mContext;
    private IIncidentManager mService;

    public IncidentManager(Context context) {
        this.mContext = context;
    }

    public void reportIncident(IncidentReportArgs incidentReportArgs) {
        reportIncidentInternal(incidentReportArgs);
    }

    private class IncidentdDeathRecipient implements IBinder.DeathRecipient {
        private IncidentdDeathRecipient() {
        }

        @Override
        public void binderDied() {
            synchronized (this) {
                IncidentManager.this.mService = null;
            }
        }
    }

    private void reportIncidentInternal(IncidentReportArgs incidentReportArgs) {
        try {
            IIncidentManager iIncidentManagerLocked = getIIncidentManagerLocked();
            if (iIncidentManagerLocked == null) {
                Slog.e(TAG, "reportIncident can't find incident binder service");
            } else {
                iIncidentManagerLocked.reportIncident(incidentReportArgs);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "reportIncident failed", e);
        }
    }

    private IIncidentManager getIIncidentManagerLocked() throws RemoteException {
        if (this.mService != null) {
            return this.mService;
        }
        synchronized (this) {
            if (this.mService != null) {
                return this.mService;
            }
            this.mService = IIncidentManager.Stub.asInterface(ServiceManager.getService(Context.INCIDENT_SERVICE));
            if (this.mService != null) {
                this.mService.asBinder().linkToDeath(new IncidentdDeathRecipient(), 0);
            }
            return this.mService;
        }
    }
}
