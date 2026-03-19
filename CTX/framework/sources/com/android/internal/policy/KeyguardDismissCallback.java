package com.android.internal.policy;

import android.os.RemoteException;
import com.android.internal.policy.IKeyguardDismissCallback;

public class KeyguardDismissCallback extends IKeyguardDismissCallback.Stub {
    @Override
    public void onDismissError() throws RemoteException {
    }

    @Override
    public void onDismissSucceeded() throws RemoteException {
    }

    @Override
    public void onDismissCancelled() throws RemoteException {
    }
}
