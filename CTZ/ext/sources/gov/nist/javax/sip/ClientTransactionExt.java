package gov.nist.javax.sip;

import javax.sip.ClientTransaction;
import javax.sip.address.Hop;

public interface ClientTransactionExt extends ClientTransaction, TransactionExt {
    @Override
    void alertIfStillInCallingStateBy(int i);

    @Override
    Hop getNextHop();

    boolean isSecure();

    @Override
    void setNotifyOnRetransmit(boolean z);
}
