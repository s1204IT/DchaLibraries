package com.android.server.am;

import android.app.IApplicationThread;
import android.app.servertransaction.ActivityLifecycleItem;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.ClientTransactionItem;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

class ClientLifecycleManager {
    ClientLifecycleManager() {
    }

    void scheduleTransaction(ClientTransaction clientTransaction) throws RemoteException {
        IApplicationThread client = clientTransaction.getClient();
        clientTransaction.schedule();
        if (!(client instanceof Binder)) {
            clientTransaction.recycle();
        }
    }

    void scheduleTransaction(IApplicationThread iApplicationThread, IBinder iBinder, ActivityLifecycleItem activityLifecycleItem) throws RemoteException {
        scheduleTransaction(transactionWithState(iApplicationThread, iBinder, activityLifecycleItem));
    }

    void scheduleTransaction(IApplicationThread iApplicationThread, IBinder iBinder, ClientTransactionItem clientTransactionItem) throws RemoteException {
        scheduleTransaction(transactionWithCallback(iApplicationThread, iBinder, clientTransactionItem));
    }

    void scheduleTransaction(IApplicationThread iApplicationThread, ClientTransactionItem clientTransactionItem) throws RemoteException {
        scheduleTransaction(transactionWithCallback(iApplicationThread, null, clientTransactionItem));
    }

    private static ClientTransaction transactionWithState(IApplicationThread iApplicationThread, IBinder iBinder, ActivityLifecycleItem activityLifecycleItem) {
        ClientTransaction clientTransactionObtain = ClientTransaction.obtain(iApplicationThread, iBinder);
        clientTransactionObtain.setLifecycleStateRequest(activityLifecycleItem);
        return clientTransactionObtain;
    }

    private static ClientTransaction transactionWithCallback(IApplicationThread iApplicationThread, IBinder iBinder, ClientTransactionItem clientTransactionItem) {
        ClientTransaction clientTransactionObtain = ClientTransaction.obtain(iApplicationThread, iBinder);
        clientTransactionObtain.addCallback(clientTransactionItem);
        return clientTransactionObtain;
    }
}
