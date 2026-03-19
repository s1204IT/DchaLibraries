package javax.sip;

import java.util.EventObject;

public class TransactionTerminatedEvent extends EventObject {
    private ClientTransaction mClientTransaction;
    private boolean mIsServerTransaction;
    private ServerTransaction mServerTransaction;

    public TransactionTerminatedEvent(Object obj, ServerTransaction serverTransaction) {
        super(obj);
        this.mServerTransaction = serverTransaction;
        this.mIsServerTransaction = true;
    }

    public TransactionTerminatedEvent(Object obj, ClientTransaction clientTransaction) {
        super(obj);
        this.mClientTransaction = clientTransaction;
        this.mIsServerTransaction = false;
    }

    public boolean isServerTransaction() {
        return this.mIsServerTransaction;
    }

    public ClientTransaction getClientTransaction() {
        return this.mClientTransaction;
    }

    public ServerTransaction getServerTransaction() {
        return this.mServerTransaction;
    }
}
