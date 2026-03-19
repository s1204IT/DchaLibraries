package gov.nist.javax.sip;

import javax.sip.Dialog;
import javax.sip.ResponseEvent;
import javax.sip.message.Response;

public class ResponseEventExt extends ResponseEvent {
    private ClientTransactionExt m_originalTransaction;

    public ResponseEventExt(Object obj, ClientTransactionExt clientTransactionExt, Dialog dialog, Response response) {
        super(obj, clientTransactionExt, dialog, response);
        this.m_originalTransaction = clientTransactionExt;
    }

    public boolean isForkedResponse() {
        return super.getClientTransaction() == null && this.m_originalTransaction != null;
    }

    public void setOriginalTransaction(ClientTransactionExt clientTransactionExt) {
        this.m_originalTransaction = clientTransactionExt;
    }

    public ClientTransactionExt getOriginalTransaction() {
        return this.m_originalTransaction;
    }
}
