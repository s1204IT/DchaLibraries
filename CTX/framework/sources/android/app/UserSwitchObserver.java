package android.app;

import android.app.IUserSwitchObserver;
import android.os.IRemoteCallback;
import android.os.RemoteException;

public class UserSwitchObserver extends IUserSwitchObserver.Stub {
    public void onUserSwitching(int i, IRemoteCallback iRemoteCallback) throws RemoteException {
        if (iRemoteCallback != null) {
            iRemoteCallback.sendResult(null);
        }
    }

    @Override
    public void onUserSwitchComplete(int i) throws RemoteException {
    }

    @Override
    public void onForegroundProfileSwitch(int i) throws RemoteException {
    }

    @Override
    public void onLockedBootComplete(int i) throws RemoteException {
    }
}
