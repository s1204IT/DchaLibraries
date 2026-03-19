package android.security;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.gatekeeper.IGateKeeperService;

public abstract class GateKeeper {
    public static final long INVALID_SECURE_USER_ID = 0;

    private GateKeeper() {
    }

    public static IGateKeeperService getService() {
        IGateKeeperService iGateKeeperServiceAsInterface = IGateKeeperService.Stub.asInterface(ServiceManager.getService(Context.GATEKEEPER_SERVICE));
        if (iGateKeeperServiceAsInterface == null) {
            throw new IllegalStateException("Gatekeeper service not available");
        }
        return iGateKeeperServiceAsInterface;
    }

    public static long getSecureUserId() throws IllegalStateException {
        try {
            return getService().getSecureUserId(UserHandle.myUserId());
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to obtain secure user ID from gatekeeper", e);
        }
    }
}
