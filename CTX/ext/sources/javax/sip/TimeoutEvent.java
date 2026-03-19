package javax.sip;

public class TimeoutEvent extends TransactionTerminatedEvent {
    private Timeout mTimeout;

    public TimeoutEvent(Object obj, ServerTransaction serverTransaction, Timeout timeout) {
        super(obj, serverTransaction);
        this.mTimeout = timeout;
    }

    public TimeoutEvent(Object obj, ClientTransaction clientTransaction, Timeout timeout) {
        super(obj, clientTransaction);
        this.mTimeout = timeout;
    }

    public Timeout getTimeout() {
        return this.mTimeout;
    }
}
