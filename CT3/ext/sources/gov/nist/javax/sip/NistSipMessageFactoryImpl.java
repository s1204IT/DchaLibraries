package gov.nist.javax.sip;

import gov.nist.core.Separators;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPTransaction;
import gov.nist.javax.sip.stack.SIPTransactionStack;
import gov.nist.javax.sip.stack.ServerRequestInterface;
import gov.nist.javax.sip.stack.ServerResponseInterface;
import gov.nist.javax.sip.stack.StackMessageFactory;
import javax.sip.TransactionState;

class NistSipMessageFactoryImpl implements StackMessageFactory {
    private SipStackImpl sipStack;

    @Override
    public ServerRequestInterface newSIPServerRequest(SIPRequest sipRequest, MessageChannel messageChannel) {
        if (messageChannel == 0 || sipRequest == null) {
            throw new IllegalArgumentException("Null Arg!");
        }
        SipStackImpl theStack = (SipStackImpl) messageChannel.getSIPStack();
        DialogFilter retval = new DialogFilter(theStack);
        if (messageChannel instanceof SIPTransaction) {
            retval.transactionChannel = messageChannel;
        }
        retval.listeningPoint = messageChannel.getMessageProcessor().getListeningPoint();
        if (retval.listeningPoint == null) {
            return null;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Returning request interface for " + sipRequest.getFirstLine() + Separators.SP + retval + " messageChannel = " + messageChannel);
        }
        return retval;
    }

    @Override
    public ServerResponseInterface newSIPServerResponse(SIPResponse sipResponse, MessageChannel messageChannel) {
        SIPTransactionStack theStack = messageChannel.getSIPStack();
        SIPTransaction tr = theStack.findTransaction(sipResponse, false);
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Found Transaction " + tr + " for " + sipResponse);
        }
        if (tr != null) {
            if (tr.getState() == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dropping response - null transaction state");
                }
                return null;
            }
            if (TransactionState.COMPLETED == tr.getState() && sipResponse.getStatusCode() / 100 == 1) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dropping response - late arriving " + sipResponse.getStatusCode());
                }
                return null;
            }
        }
        DialogFilter retval = new DialogFilter(this.sipStack);
        retval.transactionChannel = tr;
        retval.listeningPoint = messageChannel.getMessageProcessor().getListeningPoint();
        return retval;
    }

    public NistSipMessageFactoryImpl(SipStackImpl sipStackImpl) {
        this.sipStack = sipStackImpl;
    }
}
