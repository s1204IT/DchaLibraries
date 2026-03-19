package gov.nist.javax.sip;

import gov.nist.core.Separators;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPTransaction;
import gov.nist.javax.sip.stack.ServerRequestInterface;
import gov.nist.javax.sip.stack.ServerResponseInterface;
import gov.nist.javax.sip.stack.StackMessageFactory;
import javax.sip.TransactionState;

class NistSipMessageFactoryImpl implements StackMessageFactory {
    private SipStackImpl sipStack;

    @Override
    public ServerRequestInterface newSIPServerRequest(SIPRequest sIPRequest, MessageChannel messageChannel) {
        if (messageChannel == null || sIPRequest == null) {
            throw new IllegalArgumentException("Null Arg!");
        }
        DialogFilter dialogFilter = new DialogFilter((SipStackImpl) messageChannel.getSIPStack());
        if (messageChannel instanceof SIPTransaction) {
            dialogFilter.transactionChannel = (SIPTransaction) messageChannel;
        }
        dialogFilter.listeningPoint = messageChannel.getMessageProcessor().getListeningPoint();
        if (dialogFilter.listeningPoint == null) {
            return null;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Returning request interface for " + sIPRequest.getFirstLine() + Separators.SP + dialogFilter + " messageChannel = " + messageChannel);
        }
        return dialogFilter;
    }

    @Override
    public ServerResponseInterface newSIPServerResponse(SIPResponse sIPResponse, MessageChannel messageChannel) throws Throwable {
        SIPTransaction sIPTransactionFindTransaction = messageChannel.getSIPStack().findTransaction(sIPResponse, false);
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Found Transaction " + sIPTransactionFindTransaction + " for " + sIPResponse);
        }
        if (sIPTransactionFindTransaction != null) {
            if (sIPTransactionFindTransaction.getState() == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dropping response - null transaction state");
                }
                return null;
            }
            if (TransactionState.COMPLETED == sIPTransactionFindTransaction.getState() && sIPResponse.getStatusCode() / 100 == 1) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dropping response - late arriving " + sIPResponse.getStatusCode());
                }
                return null;
            }
        }
        DialogFilter dialogFilter = new DialogFilter(this.sipStack);
        dialogFilter.transactionChannel = sIPTransactionFindTransaction;
        dialogFilter.listeningPoint = messageChannel.getMessageProcessor().getListeningPoint();
        return dialogFilter;
    }

    public NistSipMessageFactoryImpl(SipStackImpl sipStackImpl) {
        this.sipStack = sipStackImpl;
    }
}
