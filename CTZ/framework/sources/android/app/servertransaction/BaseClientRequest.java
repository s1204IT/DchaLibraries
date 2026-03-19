package android.app.servertransaction;

import android.app.ClientTransactionHandler;
import android.os.IBinder;

public interface BaseClientRequest extends ObjectPoolItem {
    void execute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions);

    default void preExecute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder) {
    }

    default void postExecute(ClientTransactionHandler clientTransactionHandler, IBinder iBinder, PendingTransactionActions pendingTransactionActions) {
    }
}
