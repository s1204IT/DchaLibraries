package android.telecom;

import android.content.ComponentName;
import android.os.RemoteException;
import com.android.internal.telecom.IConnectionService;
import java.util.HashMap;
import java.util.Map;

public class RemoteConnectionManager {
    private final ConnectionService mOurConnectionServiceImpl;
    private final Map<ComponentName, RemoteConnectionService> mRemoteConnectionServices = new HashMap();

    public RemoteConnectionManager(ConnectionService connectionService) {
        this.mOurConnectionServiceImpl = connectionService;
    }

    void addConnectionService(ComponentName componentName, IConnectionService iConnectionService) {
        if (!this.mRemoteConnectionServices.containsKey(componentName)) {
            try {
                this.mRemoteConnectionServices.put(componentName, new RemoteConnectionService(iConnectionService, this.mOurConnectionServiceImpl));
            } catch (RemoteException e) {
            }
        }
    }

    public RemoteConnection createRemoteConnection(PhoneAccountHandle phoneAccountHandle, ConnectionRequest connectionRequest, boolean z) {
        if (connectionRequest.getAccountHandle() == null) {
            throw new IllegalArgumentException("accountHandle must be specified.");
        }
        ComponentName componentName = connectionRequest.getAccountHandle().getComponentName();
        if (!this.mRemoteConnectionServices.containsKey(componentName)) {
            throw new UnsupportedOperationException("accountHandle not supported: " + componentName);
        }
        RemoteConnectionService remoteConnectionService = this.mRemoteConnectionServices.get(componentName);
        if (remoteConnectionService != null) {
            return remoteConnectionService.createRemoteConnection(phoneAccountHandle, connectionRequest, z);
        }
        return null;
    }

    public void conferenceRemoteConnections(RemoteConnection remoteConnection, RemoteConnection remoteConnection2) {
        if (remoteConnection.getConnectionService() == remoteConnection2.getConnectionService()) {
            try {
                remoteConnection.getConnectionService().conference(remoteConnection.getId(), remoteConnection2.getId(), null);
            } catch (RemoteException e) {
            }
        } else {
            Log.w(this, "Request to conference incompatible remote connections (%s,%s) (%s,%s)", remoteConnection.getConnectionService(), remoteConnection.getId(), remoteConnection2.getConnectionService(), remoteConnection2.getId());
        }
    }
}
