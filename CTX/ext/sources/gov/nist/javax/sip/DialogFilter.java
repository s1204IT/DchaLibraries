package gov.nist.javax.sip;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.address.ParameterNames;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Contact;
import gov.nist.javax.sip.header.Event;
import gov.nist.javax.sip.header.RetryAfter;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPDialog;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.stack.SIPTransaction;
import gov.nist.javax.sip.stack.ServerRequestInterface;
import gov.nist.javax.sip.stack.ServerResponseInterface;
import java.io.IOException;
import java.util.EventObject;
import javax.sip.ClientTransaction;
import javax.sip.DialogState;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.TransactionState;
import javax.sip.header.Header;
import javax.sip.header.ReferToHeader;
import javax.sip.header.ServerHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

class DialogFilter implements ServerRequestInterface, ServerResponseInterface {
    protected ListeningPointImpl listeningPoint;
    private SipStackImpl sipStack;
    protected SIPTransaction transactionChannel;

    public DialogFilter(SipStackImpl sipStackImpl) {
        this.sipStack = sipStackImpl;
    }

    private void sendRequestPendingResponse(SIPRequest sIPRequest, SIPServerTransaction sIPServerTransaction) {
        Response responseCreateResponse = sIPRequest.createResponse(Response.REQUEST_PENDING);
        Header defaultServerHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (defaultServerHeader != null) {
            responseCreateResponse.setHeader(defaultServerHeader);
        }
        try {
            RetryAfter retryAfter = new RetryAfter();
            retryAfter.setRetryAfter(1);
            responseCreateResponse.setHeader(retryAfter);
            if (sIPRequest.getMethod().equals("INVITE")) {
                this.sipStack.addTransactionPendingAck(sIPServerTransaction);
            }
            sIPServerTransaction.sendResponse(responseCreateResponse);
            sIPServerTransaction.releaseSem();
        } catch (Exception e) {
            this.sipStack.getStackLogger().logError("Problem sending error response", e);
            sIPServerTransaction.releaseSem();
            this.sipStack.removeTransaction(sIPServerTransaction);
        }
    }

    private void sendBadRequestResponse(SIPRequest sIPRequest, SIPServerTransaction sIPServerTransaction, String str) {
        SIPResponse sIPResponseCreateResponse = sIPRequest.createResponse(Response.BAD_REQUEST);
        if (str != null) {
            sIPResponseCreateResponse.setReasonPhrase(str);
        }
        ServerHeader defaultServerHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (defaultServerHeader != null) {
            sIPResponseCreateResponse.setHeader(defaultServerHeader);
        }
        try {
            if (sIPRequest.getMethod().equals("INVITE")) {
                this.sipStack.addTransactionPendingAck(sIPServerTransaction);
            }
            sIPServerTransaction.sendResponse((Response) sIPResponseCreateResponse);
            sIPServerTransaction.releaseSem();
        } catch (Exception e) {
            this.sipStack.getStackLogger().logError("Problem sending error response", e);
            sIPServerTransaction.releaseSem();
            this.sipStack.removeTransaction(sIPServerTransaction);
        }
    }

    private void sendCallOrTransactionDoesNotExistResponse(SIPRequest sIPRequest, SIPServerTransaction sIPServerTransaction) {
        SIPResponse sIPResponseCreateResponse = sIPRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
        ServerHeader defaultServerHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (defaultServerHeader != null) {
            sIPResponseCreateResponse.setHeader(defaultServerHeader);
        }
        try {
            if (sIPRequest.getMethod().equals("INVITE")) {
                this.sipStack.addTransactionPendingAck(sIPServerTransaction);
            }
            sIPServerTransaction.sendResponse((Response) sIPResponseCreateResponse);
            sIPServerTransaction.releaseSem();
        } catch (Exception e) {
            this.sipStack.getStackLogger().logError("Problem sending error response", e);
            sIPServerTransaction.releaseSem();
            this.sipStack.removeTransaction(sIPServerTransaction);
        }
    }

    private void sendLoopDetectedResponse(SIPRequest sIPRequest, SIPServerTransaction sIPServerTransaction) {
        SIPResponse sIPResponseCreateResponse = sIPRequest.createResponse(Response.LOOP_DETECTED);
        ServerHeader defaultServerHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (defaultServerHeader != null) {
            sIPResponseCreateResponse.setHeader(defaultServerHeader);
        }
        try {
            this.sipStack.addTransactionPendingAck(sIPServerTransaction);
            sIPServerTransaction.sendResponse((Response) sIPResponseCreateResponse);
            sIPServerTransaction.releaseSem();
        } catch (Exception e) {
            this.sipStack.getStackLogger().logError("Problem sending error response", e);
            sIPServerTransaction.releaseSem();
            this.sipStack.removeTransaction(sIPServerTransaction);
        }
    }

    private void sendServerInternalErrorResponse(SIPRequest sIPRequest, SIPServerTransaction sIPServerTransaction) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Sending 500 response for out of sequence message");
        }
        Response responseCreateResponse = sIPRequest.createResponse(500);
        responseCreateResponse.setReasonPhrase("Request out of order");
        if (MessageFactoryImpl.getDefaultServerHeader() != null) {
            responseCreateResponse.setHeader(MessageFactoryImpl.getDefaultServerHeader());
        }
        try {
            RetryAfter retryAfter = new RetryAfter();
            retryAfter.setRetryAfter(10);
            responseCreateResponse.setHeader(retryAfter);
            this.sipStack.addTransactionPendingAck(sIPServerTransaction);
            sIPServerTransaction.sendResponse(responseCreateResponse);
            sIPServerTransaction.releaseSem();
        } catch (Exception e) {
            this.sipStack.getStackLogger().logError("Problem sending response", e);
            sIPServerTransaction.releaseSem();
            this.sipStack.removeTransaction(sIPServerTransaction);
        }
    }

    @Override
    public void processRequest(SIPRequest sIPRequest, MessageChannel messageChannel) {
        SIPServerTransaction inviteTransaction;
        SIPTransaction lastTransaction;
        EventObject requestEvent;
        Contact myContactHeader;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("PROCESSING INCOMING REQUEST " + sIPRequest + " transactionChannel = " + this.transactionChannel + " listening point = " + this.listeningPoint.getIPAddress() + Separators.COLON + this.listeningPoint.getPort());
        }
        if (this.listeningPoint == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping message: No listening point registered!");
                return;
            }
            return;
        }
        SipStackImpl sipStackImpl = (SipStackImpl) this.transactionChannel.getSIPStack();
        SipProviderImpl provider = this.listeningPoint.getProvider();
        if (provider == null) {
            if (sipStackImpl.isLoggingEnabled()) {
                sipStackImpl.getStackLogger().logDebug("No provider - dropping !!");
                return;
            }
            return;
        }
        if (sipStackImpl == null) {
            InternalErrorHandler.handleException("Egads! no sip stack!");
        }
        SIPServerTransaction sIPServerTransaction = (SIPServerTransaction) this.transactionChannel;
        if (sIPServerTransaction != null && sipStackImpl.isLoggingEnabled()) {
            sipStackImpl.getStackLogger().logDebug("transaction state = " + sIPServerTransaction.getState());
        }
        String dialogId = sIPRequest.getDialogId(true);
        SIPDialog dialog = sipStackImpl.getDialog(dialogId);
        int port = 5060;
        if (dialog != null && provider != dialog.getSipProvider() && (myContactHeader = dialog.getMyContactHeader()) != null) {
            SipUri sipUri = (SipUri) myContactHeader.getAddress().getURI();
            String host = sipUri.getHost();
            int port2 = sipUri.getPort();
            String transportParam = sipUri.getTransportParam();
            if (transportParam == null) {
                transportParam = ParameterNames.UDP;
            }
            if (port2 == -1) {
                port2 = (transportParam.equals(ParameterNames.UDP) || transportParam.equals(ParameterNames.TCP)) ? 5060 : 5061;
            }
            if (host != null && (!host.equals(this.listeningPoint.getIPAddress()) || port2 != this.listeningPoint.getPort())) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("nulling dialog -- listening point mismatch!  " + port2 + "  lp port = " + this.listeningPoint.getPort());
                }
                dialog = null;
            }
        }
        if (provider.isAutomaticDialogSupportEnabled() && provider.isDialogErrorsAutomaticallyHandled() && sIPRequest.getToTag() == null && sipStackImpl.findMergedTransaction(sIPRequest) != null) {
            sendLoopDetectedResponse(sIPRequest, sIPServerTransaction);
            return;
        }
        if (sipStackImpl.isLoggingEnabled()) {
            sipStackImpl.getStackLogger().logDebug("dialogId = " + dialogId);
            sipStackImpl.getStackLogger().logDebug("dialog = " + dialog);
        }
        if (sIPRequest.getHeader("Route") != null && sIPServerTransaction.getDialog() != null) {
            RouteList routeHeaders = sIPRequest.getRouteHeaders();
            SipUri sipUri2 = (SipUri) ((Route) routeHeaders.getFirst()).getAddress().getURI();
            if (sipUri2.getHostPort().hasPort()) {
                port = sipUri2.getHostPort().getPort();
            } else if (this.listeningPoint.getTransport().equalsIgnoreCase(ListeningPoint.TLS)) {
                port = 5061;
            }
            String host2 = sipUri2.getHost();
            if ((host2.equals(this.listeningPoint.getIPAddress()) || host2.equalsIgnoreCase(this.listeningPoint.getSentBy())) && port == this.listeningPoint.getPort()) {
                if (routeHeaders.size() == 1) {
                    sIPRequest.removeHeader("Route");
                } else {
                    routeHeaders.removeFirst();
                }
            }
        }
        if (sIPRequest.getMethod().equals(Request.REFER) && dialog != null && provider.isDialogErrorsAutomaticallyHandled()) {
            if (((ReferToHeader) sIPRequest.getHeader(ReferToHeader.NAME)) == null) {
                sendBadRequestResponse(sIPRequest, sIPServerTransaction, "Refer-To header is missing");
                return;
            }
            SIPTransaction lastTransaction2 = dialog.getLastTransaction();
            if (lastTransaction2 != null && provider.isDialogErrorsAutomaticallyHandled()) {
                SIPRequest sIPRequest2 = (SIPRequest) lastTransaction2.getRequest();
                if (lastTransaction2 instanceof SIPServerTransaction) {
                    if (!dialog.isAckSeen() && sIPRequest2.getMethod().equals("INVITE")) {
                        sendRequestPendingResponse(sIPRequest, sIPServerTransaction);
                        return;
                    }
                } else if (lastTransaction2 instanceof SIPClientTransaction) {
                    long seqNumber = sIPRequest2.getCSeqHeader().getSeqNumber();
                    if (sIPRequest2.getMethod().equals("INVITE") && !dialog.isAckSent(seqNumber)) {
                        sendRequestPendingResponse(sIPRequest, sIPServerTransaction);
                        return;
                    }
                }
            }
        } else if (sIPRequest.getMethod().equals(Request.UPDATE)) {
            if (provider.isAutomaticDialogSupportEnabled() && dialog == null) {
                sendCallOrTransactionDoesNotExistResponse(sIPRequest, sIPServerTransaction);
                return;
            }
        } else if (sIPRequest.getMethod().equals("ACK")) {
            if (sIPServerTransaction != null && sIPServerTransaction.isInviteTransaction()) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Processing ACK for INVITE Tx ");
                }
            } else {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Processing ACK for dialog " + dialog);
                }
                if (dialog == null) {
                    if (sipStackImpl.isLoggingEnabled()) {
                        sipStackImpl.getStackLogger().logDebug("Dialog does not exist " + sIPRequest.getFirstLine() + " isServerTransaction = true");
                    }
                    SIPServerTransaction retransmissionAlertTransaction = sipStackImpl.getRetransmissionAlertTransaction(dialogId);
                    if (retransmissionAlertTransaction != null && retransmissionAlertTransaction.isRetransmissionAlertEnabled()) {
                        retransmissionAlertTransaction.disableRetransmissionAlerts();
                    }
                    SIPServerTransaction sIPServerTransactionFindTransactionPendingAck = sipStackImpl.findTransactionPendingAck(sIPRequest);
                    if (sIPServerTransactionFindTransactionPendingAck != null) {
                        if (sipStackImpl.isLoggingEnabled()) {
                            sipStackImpl.getStackLogger().logDebug("Found Tx pending ACK");
                        }
                        try {
                            sIPServerTransactionFindTransactionPendingAck.setAckSeen();
                            sipStackImpl.removeTransaction(sIPServerTransactionFindTransactionPendingAck);
                            sipStackImpl.removeTransactionPendingAck(sIPServerTransactionFindTransactionPendingAck);
                            return;
                        } catch (Exception e) {
                            if (sipStackImpl.isLoggingEnabled()) {
                                sipStackImpl.getStackLogger().logError("Problem terminating transaction", e);
                                return;
                            }
                            return;
                        }
                    }
                } else if (!dialog.handleAck(sIPServerTransaction)) {
                    if (!dialog.isSequnceNumberValidation()) {
                        if (sipStackImpl.isLoggingEnabled()) {
                            sipStackImpl.getStackLogger().logDebug("Dialog exists with loose dialog validation " + sIPRequest.getFirstLine() + " isServerTransaction = true dialog = " + dialog.getDialogId());
                        }
                        SIPServerTransaction retransmissionAlertTransaction2 = sipStackImpl.getRetransmissionAlertTransaction(dialogId);
                        if (retransmissionAlertTransaction2 != null && retransmissionAlertTransaction2.isRetransmissionAlertEnabled()) {
                            retransmissionAlertTransaction2.disableRetransmissionAlerts();
                        }
                    } else {
                        if (sipStackImpl.isLoggingEnabled()) {
                            sipStackImpl.getStackLogger().logDebug("Dropping ACK - cannot find a transaction or dialog");
                        }
                        SIPServerTransaction sIPServerTransactionFindTransactionPendingAck2 = sipStackImpl.findTransactionPendingAck(sIPRequest);
                        if (sIPServerTransactionFindTransactionPendingAck2 != null) {
                            if (sipStackImpl.isLoggingEnabled()) {
                                sipStackImpl.getStackLogger().logDebug("Found Tx pending ACK");
                            }
                            try {
                                sIPServerTransactionFindTransactionPendingAck2.setAckSeen();
                                sipStackImpl.removeTransaction(sIPServerTransactionFindTransactionPendingAck2);
                                sipStackImpl.removeTransactionPendingAck(sIPServerTransactionFindTransactionPendingAck2);
                                return;
                            } catch (Exception e2) {
                                if (sipStackImpl.isLoggingEnabled()) {
                                    sipStackImpl.getStackLogger().logError("Problem terminating transaction", e2);
                                    return;
                                }
                                return;
                            }
                        }
                        return;
                    }
                } else {
                    sIPServerTransaction.passToListener();
                    dialog.addTransaction(sIPServerTransaction);
                    dialog.addRoute(sIPRequest);
                    sIPServerTransaction.setDialog(dialog, dialogId);
                    if (sIPRequest.getMethod().equals("INVITE") && provider.isDialogErrorsAutomaticallyHandled()) {
                        sipStackImpl.putInMergeTable(sIPServerTransaction, sIPRequest);
                    }
                    if (sipStackImpl.deliverTerminatedEventForAck) {
                        try {
                            sipStackImpl.addTransaction(sIPServerTransaction);
                            sIPServerTransaction.scheduleAckRemoval();
                        } catch (IOException e3) {
                        }
                    } else {
                        sIPServerTransaction.setMapped(true);
                    }
                }
            }
        } else if (sIPRequest.getMethod().equals(Request.PRACK)) {
            if (sipStackImpl.isLoggingEnabled()) {
                sipStackImpl.getStackLogger().logDebug("Processing PRACK for dialog " + dialog);
            }
            if (dialog == null && provider.isAutomaticDialogSupportEnabled()) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Dialog does not exist " + sIPRequest.getFirstLine() + " isServerTransaction = true");
                }
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Sending 481 for PRACK - automatic dialog support is enabled -- cant find dialog!");
                }
                try {
                    provider.sendResponse(sIPRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST));
                } catch (SipException e4) {
                    sipStackImpl.getStackLogger().logError("error sending response", e4);
                }
                if (sIPServerTransaction != null) {
                    sipStackImpl.removeTransaction(sIPServerTransaction);
                    sIPServerTransaction.releaseSem();
                    return;
                }
                return;
            }
            if (dialog != null) {
                if (!dialog.handlePrack(sIPRequest)) {
                    if (sipStackImpl.isLoggingEnabled()) {
                        sipStackImpl.getStackLogger().logDebug("Dropping out of sequence PRACK ");
                    }
                    if (sIPServerTransaction != null) {
                        sipStackImpl.removeTransaction(sIPServerTransaction);
                        sIPServerTransaction.releaseSem();
                        return;
                    }
                    return;
                }
                try {
                    sipStackImpl.addTransaction(sIPServerTransaction);
                    dialog.addTransaction(sIPServerTransaction);
                    dialog.addRoute(sIPRequest);
                    sIPServerTransaction.setDialog(dialog, dialogId);
                } catch (Exception e5) {
                    InternalErrorHandler.handleException(e5);
                }
            } else if (sipStackImpl.isLoggingEnabled()) {
                sipStackImpl.getStackLogger().logDebug("Processing PRACK without a DIALOG -- this must be a proxy element");
            }
        } else if (sIPRequest.getMethod().equals("BYE")) {
            if (dialog != null && !dialog.isRequestConsumable(sIPRequest)) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Dropping out of sequence BYE " + dialog.getRemoteSeqNumber() + Separators.SP + sIPRequest.getCSeq().getSeqNumber());
                }
                if (dialog.getRemoteSeqNumber() >= sIPRequest.getCSeq().getSeqNumber() && sIPServerTransaction.getState() == TransactionState.TRYING) {
                    sendServerInternalErrorResponse(sIPRequest, sIPServerTransaction);
                }
                if (sIPServerTransaction != null) {
                    sipStackImpl.removeTransaction(sIPServerTransaction);
                    return;
                }
                return;
            }
            if (dialog == null && provider.isAutomaticDialogSupportEnabled()) {
                Response responseCreateResponse = sIPRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
                responseCreateResponse.setReasonPhrase("Dialog Not Found");
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("dropping request -- automatic dialog support enabled and dialog does not exist!");
                }
                try {
                    sIPServerTransaction.sendResponse(responseCreateResponse);
                } catch (SipException e6) {
                    sipStackImpl.getStackLogger().logError("Error in sending response", e6);
                }
                if (sIPServerTransaction != null) {
                    sipStackImpl.removeTransaction(sIPServerTransaction);
                    sIPServerTransaction.releaseSem();
                    return;
                }
                return;
            }
            if (sIPServerTransaction != null && dialog != null) {
                try {
                    if (provider == dialog.getSipProvider()) {
                        sipStackImpl.addTransaction(sIPServerTransaction);
                        dialog.addTransaction(sIPServerTransaction);
                        sIPServerTransaction.setDialog(dialog, dialogId);
                    }
                } catch (IOException e7) {
                    InternalErrorHandler.handleException(e7);
                }
            }
            if (sipStackImpl.isLoggingEnabled()) {
                sipStackImpl.getStackLogger().logDebug("BYE Tx = " + sIPServerTransaction + " isMapped =" + sIPServerTransaction.isTransactionMapped());
            }
        } else if (sIPRequest.getMethod().equals(Request.CANCEL)) {
            SIPServerTransaction sIPServerTransaction2 = (SIPServerTransaction) sipStackImpl.findCancelTransaction(sIPRequest, true);
            if (sipStackImpl.isLoggingEnabled()) {
                sipStackImpl.getStackLogger().logDebug("Got a CANCEL, InviteServerTx = " + sIPServerTransaction2 + " cancel Server Tx ID = " + sIPServerTransaction + " isMapped = " + sIPServerTransaction.isTransactionMapped());
            }
            if (sIPRequest.getMethod().equals(Request.CANCEL)) {
                if (sIPServerTransaction2 != null && sIPServerTransaction2.getState() == SIPTransaction.TERMINATED_STATE) {
                    if (sipStackImpl.isLoggingEnabled()) {
                        sipStackImpl.getStackLogger().logDebug("Too late to cancel Transaction");
                    }
                    try {
                        sIPServerTransaction.sendResponse(sIPRequest.createResponse(Response.OK));
                        return;
                    } catch (Exception e8) {
                        if (e8.getCause() != null && (e8.getCause() instanceof IOException)) {
                            sIPServerTransaction2.raiseIOExceptionEvent();
                            return;
                        }
                        return;
                    }
                }
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Cancel transaction = " + sIPServerTransaction2);
                }
            }
            if (sIPServerTransaction != null && sIPServerTransaction2 != null && sIPServerTransaction2.getDialog() != null) {
                sIPServerTransaction.setDialog((SIPDialog) sIPServerTransaction2.getDialog(), dialogId);
                dialog = (SIPDialog) sIPServerTransaction2.getDialog();
            } else if (sIPServerTransaction2 == null && provider.isAutomaticDialogSupportEnabled() && sIPServerTransaction != null) {
                Response responseCreateResponse2 = sIPRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("dropping request -- automatic dialog support enabled and INVITE ST does not exist!");
                }
                try {
                    provider.sendResponse(responseCreateResponse2);
                } catch (SipException e9) {
                    InternalErrorHandler.handleException(e9);
                }
                if (sIPServerTransaction != null) {
                    sipStackImpl.removeTransaction(sIPServerTransaction);
                    sIPServerTransaction.releaseSem();
                    return;
                }
                return;
            }
            if (sIPServerTransaction2 != null && sIPServerTransaction != null) {
                try {
                    sipStackImpl.addTransaction(sIPServerTransaction);
                    sIPServerTransaction.setPassToListener();
                    sIPServerTransaction.setInviteTransaction(sIPServerTransaction2);
                    sIPServerTransaction2.acquireSem();
                } catch (Exception e10) {
                    InternalErrorHandler.handleException(e10);
                }
            }
        } else if (sIPRequest.getMethod().equals("INVITE")) {
            if (dialog != null) {
                inviteTransaction = dialog.getInviteTransaction();
            } else {
                inviteTransaction = null;
            }
            if (dialog != null && sIPServerTransaction != null && inviteTransaction != null && sIPRequest.getCSeq().getSeqNumber() > dialog.getRemoteSeqNumber() && (inviteTransaction instanceof SIPServerTransaction) && provider.isDialogErrorsAutomaticallyHandled() && dialog.isSequnceNumberValidation() && inviteTransaction.isInviteTransaction() && inviteTransaction.getState() != TransactionState.COMPLETED && inviteTransaction.getState() != TransactionState.TERMINATED && inviteTransaction.getState() != TransactionState.CONFIRMED) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Sending 500 response for out of sequence message");
                }
                sendServerInternalErrorResponse(sIPRequest, sIPServerTransaction);
                return;
            }
            if (dialog != null) {
                lastTransaction = dialog.getLastTransaction();
            } else {
                lastTransaction = null;
            }
            if (dialog != null && provider.isDialogErrorsAutomaticallyHandled() && lastTransaction != null && lastTransaction.isInviteTransaction() && (lastTransaction instanceof ClientTransaction) && lastTransaction.getLastResponse() != null && lastTransaction.getLastResponse().getStatusCode() == 200 && !dialog.isAckSent(lastTransaction.getLastResponse().getCSeq().getSeqNumber())) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Sending 491 response for client Dialog ACK not sent.");
                }
                sendRequestPendingResponse(sIPRequest, sIPServerTransaction);
                return;
            } else if (dialog != null && lastTransaction != null && provider.isDialogErrorsAutomaticallyHandled() && lastTransaction.isInviteTransaction() && (lastTransaction instanceof ServerTransaction) && !dialog.isAckSeen()) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Sending 491 response for server Dialog ACK not seen.");
                }
                sendRequestPendingResponse(sIPRequest, sIPServerTransaction);
                return;
            }
        }
        if (sipStackImpl.isLoggingEnabled()) {
            sipStackImpl.getStackLogger().logDebug("CHECK FOR OUT OF SEQ MESSAGE " + dialog + " transaction " + sIPServerTransaction);
        }
        if (dialog != null && sIPServerTransaction != null && !sIPRequest.getMethod().equals("BYE") && !sIPRequest.getMethod().equals(Request.CANCEL) && !sIPRequest.getMethod().equals("ACK") && !sIPRequest.getMethod().equals(Request.PRACK)) {
            if (!dialog.isRequestConsumable(sIPRequest)) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Dropping out of sequence message " + dialog.getRemoteSeqNumber() + Separators.SP + sIPRequest.getCSeq());
                }
                if (dialog.getRemoteSeqNumber() >= sIPRequest.getCSeq().getSeqNumber() && provider.isDialogErrorsAutomaticallyHandled()) {
                    if (sIPServerTransaction.getState() == TransactionState.TRYING || sIPServerTransaction.getState() == TransactionState.PROCEEDING) {
                        sendServerInternalErrorResponse(sIPRequest, sIPServerTransaction);
                        return;
                    }
                    return;
                }
                return;
            }
            try {
                if (provider == dialog.getSipProvider()) {
                    sipStackImpl.addTransaction(sIPServerTransaction);
                    dialog.addTransaction(sIPServerTransaction);
                    dialog.addRoute(sIPRequest);
                    sIPServerTransaction.setDialog(dialog, dialogId);
                }
            } catch (IOException e11) {
                sIPServerTransaction.raiseIOExceptionEvent();
                sipStackImpl.removeTransaction(sIPServerTransaction);
                return;
            }
        }
        if (sipStackImpl.isLoggingEnabled()) {
            sipStackImpl.getStackLogger().logDebug(sIPRequest.getMethod() + " transaction.isMapped = " + sIPServerTransaction.isTransactionMapped());
        }
        if (dialog == null && sIPRequest.getMethod().equals("NOTIFY")) {
            SIPClientTransaction sIPClientTransactionFindSubscribeTransaction = sipStackImpl.findSubscribeTransaction(sIPRequest, this.listeningPoint);
            if (sipStackImpl.isLoggingEnabled()) {
                sipStackImpl.getStackLogger().logDebug("PROCESSING NOTIFY  DIALOG == null " + sIPClientTransactionFindSubscribeTransaction);
            }
            if (provider.isAutomaticDialogSupportEnabled() && sIPClientTransactionFindSubscribeTransaction == null && !sipStackImpl.deliverUnsolicitedNotify) {
                try {
                    if (sipStackImpl.isLoggingEnabled()) {
                        sipStackImpl.getStackLogger().logDebug("Could not find Subscription for Notify Tx.");
                    }
                    Response responseCreateResponse3 = sIPRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
                    responseCreateResponse3.setReasonPhrase("Subscription does not exist");
                    provider.sendResponse(responseCreateResponse3);
                    return;
                } catch (Exception e12) {
                    sipStackImpl.getStackLogger().logError("Exception while sending error response statelessly", e12);
                    return;
                }
            }
            if (sIPClientTransactionFindSubscribeTransaction != null) {
                sIPServerTransaction.setPendingSubscribe(sIPClientTransactionFindSubscribeTransaction);
                SIPDialog defaultDialog = sIPClientTransactionFindSubscribeTransaction.getDefaultDialog();
                if (defaultDialog == null || defaultDialog.getDialogId() == null || !defaultDialog.getDialogId().equals(dialogId)) {
                    if (defaultDialog != null && defaultDialog.getDialogId() == null) {
                        defaultDialog.setDialogId(dialogId);
                        defaultDialog = defaultDialog;
                    } else {
                        defaultDialog = sIPClientTransactionFindSubscribeTransaction.getDialog(dialogId);
                    }
                    if (sipStackImpl.isLoggingEnabled()) {
                        sipStackImpl.getStackLogger().logDebug("PROCESSING NOTIFY Subscribe DIALOG " + defaultDialog);
                    }
                    if (defaultDialog == null && ((provider.isAutomaticDialogSupportEnabled() || sIPClientTransactionFindSubscribeTransaction.getDefaultDialog() != null) && sipStackImpl.isEventForked(((Event) sIPRequest.getHeader("Event")).getEventType()))) {
                        defaultDialog = SIPDialog.createFromNOTIFY(sIPClientTransactionFindSubscribeTransaction, sIPServerTransaction);
                    }
                    if (defaultDialog != null) {
                        sIPServerTransaction.setDialog(defaultDialog, dialogId);
                        defaultDialog.setState(DialogState.CONFIRMED.getValue());
                        sipStackImpl.putDialog(defaultDialog);
                        sIPClientTransactionFindSubscribeTransaction.setDialog(defaultDialog, dialogId);
                        if (!sIPServerTransaction.isTransactionMapped()) {
                            this.sipStack.mapTransaction(sIPServerTransaction);
                            sIPServerTransaction.setPassToListener();
                            try {
                                this.sipStack.addTransaction(sIPServerTransaction);
                            } catch (Exception e13) {
                            }
                        }
                    }
                } else {
                    sIPServerTransaction.setDialog(defaultDialog, dialogId);
                    if (!sIPServerTransaction.isTransactionMapped()) {
                        this.sipStack.mapTransaction(sIPServerTransaction);
                        sIPServerTransaction.setPassToListener();
                        try {
                            this.sipStack.addTransaction(sIPServerTransaction);
                        } catch (Exception e14) {
                        }
                    }
                    sipStackImpl.putDialog(defaultDialog);
                    if (sIPClientTransactionFindSubscribeTransaction != null) {
                        defaultDialog.addTransaction(sIPClientTransactionFindSubscribeTransaction);
                        sIPClientTransactionFindSubscribeTransaction.setDialog(defaultDialog, dialogId);
                    }
                }
                if (sIPServerTransaction != null && sIPServerTransaction.isTransactionMapped()) {
                    requestEvent = new RequestEvent(provider, sIPServerTransaction, defaultDialog, sIPRequest);
                } else {
                    requestEvent = new RequestEvent(provider, null, defaultDialog, sIPRequest);
                }
            } else {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("could not find subscribe tx");
                }
                requestEvent = new RequestEvent(provider, null, null, sIPRequest);
            }
        } else if (sIPServerTransaction != null && sIPServerTransaction.isTransactionMapped()) {
            requestEvent = new RequestEvent(provider, sIPServerTransaction, dialog, sIPRequest);
        } else {
            requestEvent = new RequestEvent(provider, null, dialog, sIPRequest);
        }
        provider.handleEvent(requestEvent, sIPServerTransaction);
    }

    @Override
    public void processResponse(SIPResponse sIPResponse, MessageChannel messageChannel, SIPDialog sIPDialog) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("PROCESSING INCOMING RESPONSE" + sIPResponse.encodeMessage());
        }
        if (this.listeningPoint == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping message: No listening point registered!");
                return;
            }
            return;
        }
        if (this.sipStack.checkBranchId() && !Utils.getInstance().responseBelongsToUs(sIPResponse)) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping response - topmost VIA header does not originate from this stack");
                return;
            }
            return;
        }
        SipProviderImpl provider = this.listeningPoint.getProvider();
        if (provider == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping message:  no provider");
                return;
            }
            return;
        }
        if (provider.getSipListener() == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("No listener -- dropping response!");
                return;
            }
            return;
        }
        SIPClientTransaction sIPClientTransaction = (SIPClientTransaction) this.transactionChannel;
        SipStackImpl sipStackImpl = provider.sipStack;
        if (this.sipStack.isLoggingEnabled()) {
            sipStackImpl.getStackLogger().logDebug("Transaction = " + sIPClientTransaction);
        }
        if (sIPClientTransaction == null) {
            if (sIPDialog != null) {
                if (sIPResponse.getStatusCode() / 100 != 2) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Response is not a final response and dialog is found for response -- dropping response!");
                        return;
                    }
                    return;
                }
                if (sIPDialog.getState() == DialogState.TERMINATED) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Dialog is terminated -- dropping response!");
                        return;
                    }
                    return;
                }
                boolean z = false;
                if (sIPDialog.isAckSeen() && sIPDialog.getLastAckSent() != null && sIPDialog.getLastAckSent().getCSeq().getSeqNumber() == sIPResponse.getCSeq().getSeqNumber()) {
                    z = true;
                }
                if (z && sIPResponse.getCSeq().getMethod().equals(sIPDialog.getMethod())) {
                    try {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("Retransmission of OK detected: Resending last ACK");
                        }
                        sIPDialog.resendAck();
                        return;
                    } catch (SipException e) {
                        this.sipStack.getStackLogger().logError("could not resend ack", e);
                    }
                }
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("could not find tx, handling statelessly Dialog =  " + sIPDialog);
            }
            ResponseEventExt responseEventExt = new ResponseEventExt(provider, sIPClientTransaction, sIPDialog, sIPResponse);
            if (sIPResponse.getCSeqHeader().getMethod().equals("INVITE")) {
                responseEventExt.setOriginalTransaction(this.sipStack.getForkedTransaction(sIPResponse.getTransactionId()));
            }
            provider.handleEvent(responseEventExt, sIPClientTransaction);
            return;
        }
        ResponseEventExt responseEventExt2 = new ResponseEventExt(provider, sIPClientTransaction, sIPDialog, sIPResponse);
        if (sIPResponse.getCSeqHeader().getMethod().equals("INVITE")) {
            responseEventExt2.setOriginalTransaction(this.sipStack.getForkedTransaction(sIPResponse.getTransactionId()));
        }
        if (sIPDialog != null && sIPResponse.getStatusCode() != 100) {
            sIPDialog.setLastResponse(sIPClientTransaction, sIPResponse);
            sIPClientTransaction.setDialog(sIPDialog, sIPDialog.getDialogId());
        }
        provider.handleEvent(responseEventExt2, sIPClientTransaction);
    }

    public String getProcessingInfo() {
        return null;
    }

    @Override
    public void processResponse(SIPResponse sIPResponse, MessageChannel messageChannel) {
        boolean z = false;
        String dialogId = sIPResponse.getDialogId(false);
        SIPDialog dialog = this.sipStack.getDialog(dialogId);
        String method = sIPResponse.getCSeq().getMethod();
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("PROCESSING INCOMING RESPONSE: " + sIPResponse.encodeMessage());
        }
        if (this.sipStack.checkBranchId() && !Utils.getInstance().responseBelongsToUs(sIPResponse)) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Detected stray response -- dropping");
                return;
            }
            return;
        }
        if (this.listeningPoint == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping message: No listening point registered!");
                return;
            }
            return;
        }
        SipProviderImpl provider = this.listeningPoint.getProvider();
        if (provider == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping message:  no provider");
                return;
            }
            return;
        }
        if (provider.getSipListener() == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping message:  no sipListener registered!");
                return;
            }
            return;
        }
        SIPClientTransaction sIPClientTransaction = (SIPClientTransaction) this.transactionChannel;
        if (dialog == null && sIPClientTransaction != null && (dialog = sIPClientTransaction.getDialog(dialogId)) != null && dialog.getState() == DialogState.TERMINATED) {
            dialog = null;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Transaction = " + sIPClientTransaction + " sipDialog = " + dialog);
        }
        if (this.transactionChannel != null) {
            String fromTag = ((SIPRequest) this.transactionChannel.getRequest()).getFromTag();
            if ((fromTag == null) ^ (sIPResponse.getFrom().getTag() == null)) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("From tag mismatch -- dropping response");
                    return;
                }
                return;
            } else if (fromTag != null && !fromTag.equalsIgnoreCase(sIPResponse.getFrom().getTag())) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("From tag mismatch -- dropping response");
                    return;
                }
                return;
            }
        }
        SipStackImpl sipStackImpl = this.sipStack;
        if (SipStackImpl.isDialogCreated(method) && sIPResponse.getStatusCode() != 100 && sIPResponse.getFrom().getTag() != null && sIPResponse.getTo().getTag() != null && dialog == null) {
            if (provider.isAutomaticDialogSupportEnabled()) {
                if (this.transactionChannel != null) {
                    if (dialog == null) {
                        dialog = this.sipStack.createDialog((SIPClientTransaction) this.transactionChannel, sIPResponse);
                        this.transactionChannel.setDialog(dialog, sIPResponse.getDialogId(false));
                    }
                } else {
                    dialog = this.sipStack.createDialog(provider, sIPResponse);
                }
            }
        } else if (dialog != null && sIPClientTransaction == null && dialog.getState() != DialogState.TERMINATED) {
            if (sIPResponse.getStatusCode() / 100 != 2) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("status code != 200 ; statusCode = " + sIPResponse.getStatusCode());
                }
            } else {
                if (dialog.getState() == DialogState.TERMINATED) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Dialog is terminated -- dropping response!");
                    }
                    if (sIPResponse.getStatusCode() / 100 == 2 && sIPResponse.getCSeq().getMethod().equals("INVITE")) {
                        try {
                            dialog.sendAck(dialog.createAck(sIPResponse.getCSeq().getSeqNumber()));
                            return;
                        } catch (Exception e) {
                            this.sipStack.getStackLogger().logError("Error creating ack", e);
                            return;
                        }
                    }
                    return;
                }
                if (dialog.isAckSeen() && dialog.getLastAckSent() != null && dialog.getLastAckSent().getCSeq().getSeqNumber() == sIPResponse.getCSeq().getSeqNumber() && sIPResponse.getDialogId(false).equals(dialog.getLastAckSent().getDialogId(false))) {
                    z = true;
                }
                if (z && sIPResponse.getCSeq().getMethod().equals(dialog.getMethod())) {
                    try {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("resending ACK");
                        }
                        dialog.resendAck();
                        return;
                    } catch (SipException e2) {
                    }
                }
            }
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("sending response to TU for processing ");
        }
        if (dialog != null && sIPResponse.getStatusCode() != 100 && sIPResponse.getTo().getTag() != null) {
            dialog.setLastResponse(sIPClientTransaction, sIPResponse);
        }
        ResponseEventExt responseEventExt = new ResponseEventExt(provider, sIPClientTransaction, dialog, sIPResponse);
        if (sIPResponse.getCSeq().getMethod().equals("INVITE")) {
            responseEventExt.setOriginalTransaction(this.sipStack.getForkedTransaction(sIPResponse.getTransactionId()));
        }
        provider.handleEvent(responseEventExt, sIPClientTransaction);
    }
}
