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
import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.TransactionState;
import javax.sip.header.ReferToHeader;
import javax.sip.header.ServerHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

class DialogFilter implements ServerRequestInterface, ServerResponseInterface {
    protected ListeningPointImpl listeningPoint;
    private SipStackImpl sipStack;
    protected SIPTransaction transactionChannel;

    public DialogFilter(SipStackImpl sipStack) {
        this.sipStack = sipStack;
    }

    private void sendRequestPendingResponse(SIPRequest sipRequest, SIPServerTransaction transaction) {
        SIPResponse sipResponse = sipRequest.createResponse(Response.REQUEST_PENDING);
        ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (serverHeader != null) {
            sipResponse.setHeader(serverHeader);
        }
        try {
            RetryAfter retryAfter = new RetryAfter();
            retryAfter.setRetryAfter(1);
            sipResponse.setHeader(retryAfter);
            if (sipRequest.getMethod().equals("INVITE")) {
                this.sipStack.addTransactionPendingAck(transaction);
            }
            transaction.sendResponse((Response) sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending error response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    private void sendBadRequestResponse(SIPRequest sipRequest, SIPServerTransaction transaction, String reasonPhrase) {
        SIPResponse sipResponse = sipRequest.createResponse(400);
        if (reasonPhrase != null) {
            sipResponse.setReasonPhrase(reasonPhrase);
        }
        ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (serverHeader != null) {
            sipResponse.setHeader(serverHeader);
        }
        try {
            if (sipRequest.getMethod().equals("INVITE")) {
                this.sipStack.addTransactionPendingAck(transaction);
            }
            transaction.sendResponse((Response) sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending error response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    private void sendCallOrTransactionDoesNotExistResponse(SIPRequest sipRequest, SIPServerTransaction transaction) {
        SIPResponse sipResponse = sipRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
        ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (serverHeader != null) {
            sipResponse.setHeader(serverHeader);
        }
        try {
            if (sipRequest.getMethod().equals("INVITE")) {
                this.sipStack.addTransactionPendingAck(transaction);
            }
            transaction.sendResponse((Response) sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending error response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    private void sendLoopDetectedResponse(SIPRequest sipRequest, SIPServerTransaction transaction) {
        SIPResponse sipResponse = sipRequest.createResponse(Response.LOOP_DETECTED);
        ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
        if (serverHeader != null) {
            sipResponse.setHeader(serverHeader);
        }
        try {
            this.sipStack.addTransactionPendingAck(transaction);
            transaction.sendResponse((Response) sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending error response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    private void sendServerInternalErrorResponse(SIPRequest sipRequest, SIPServerTransaction transaction) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Sending 500 response for out of sequence message");
        }
        SIPResponse sipResponse = sipRequest.createResponse(500);
        sipResponse.setReasonPhrase("Request out of order");
        if (MessageFactoryImpl.getDefaultServerHeader() != null) {
            ServerHeader serverHeader = MessageFactoryImpl.getDefaultServerHeader();
            sipResponse.setHeader(serverHeader);
        }
        try {
            RetryAfter retryAfter = new RetryAfter();
            retryAfter.setRetryAfter(10);
            sipResponse.setHeader(retryAfter);
            this.sipStack.addTransactionPendingAck(transaction);
            transaction.sendResponse((Response) sipResponse);
            transaction.releaseSem();
        } catch (Exception ex) {
            this.sipStack.getStackLogger().logError("Problem sending response", ex);
            transaction.releaseSem();
            this.sipStack.removeTransaction(transaction);
        }
    }

    @Override
    public void processRequest(SIPRequest sipRequest, MessageChannel incomingMessageChannel) {
        EventObject sipEvent;
        int port;
        Contact contact;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("PROCESSING INCOMING REQUEST " + sipRequest + " transactionChannel = " + this.transactionChannel + " listening point = " + this.listeningPoint.getIPAddress() + Separators.COLON + this.listeningPoint.getPort());
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
        String dialogId = sipRequest.getDialogId(true);
        SIPDialog dialog = sipStackImpl.getDialog(dialogId);
        if (dialog != null && provider != dialog.getSipProvider() && (contact = dialog.getMyContactHeader()) != null) {
            SipUri contactUri = (SipUri) contact.getAddress().getURI();
            String ipAddress = contactUri.getHost();
            int contactPort = contactUri.getPort();
            String contactTransport = contactUri.getTransportParam();
            if (contactTransport == null) {
                contactTransport = ParameterNames.UDP;
            }
            if (contactPort == -1) {
                if (contactTransport.equals(ParameterNames.UDP) || contactTransport.equals(ParameterNames.TCP)) {
                    contactPort = 5060;
                } else {
                    contactPort = 5061;
                }
            }
            if (ipAddress != null && (!ipAddress.equals(this.listeningPoint.getIPAddress()) || contactPort != this.listeningPoint.getPort())) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("nulling dialog -- listening point mismatch!  " + contactPort + "  lp port = " + this.listeningPoint.getPort());
                }
                dialog = null;
            }
        }
        if (provider.isAutomaticDialogSupportEnabled() && provider.isDialogErrorsAutomaticallyHandled() && sipRequest.getToTag() == null) {
            SIPServerTransaction sipServerTransaction = sipStackImpl.findMergedTransaction(sipRequest);
            if (sipServerTransaction != null) {
                sendLoopDetectedResponse(sipRequest, sIPServerTransaction);
                return;
            }
        }
        if (sipStackImpl.isLoggingEnabled()) {
            sipStackImpl.getStackLogger().logDebug("dialogId = " + dialogId);
            sipStackImpl.getStackLogger().logDebug("dialog = " + dialog);
        }
        if (sipRequest.getHeader("Route") != null && sIPServerTransaction.getDialog() != null) {
            RouteList routes = sipRequest.getRouteHeaders();
            Route route = (Route) routes.getFirst();
            SipUri uri = (SipUri) route.getAddress().getURI();
            if (uri.getHostPort().hasPort()) {
                port = uri.getHostPort().getPort();
            } else if (this.listeningPoint.getTransport().equalsIgnoreCase("TLS")) {
                port = 5061;
            } else {
                port = 5060;
            }
            String host = uri.getHost();
            if ((host.equals(this.listeningPoint.getIPAddress()) || host.equalsIgnoreCase(this.listeningPoint.getSentBy())) && port == this.listeningPoint.getPort()) {
                if (routes.size() == 1) {
                    sipRequest.removeHeader("Route");
                } else {
                    routes.removeFirst();
                }
            }
        }
        if (sipRequest.getMethod().equals(Request.REFER) && dialog != null && provider.isDialogErrorsAutomaticallyHandled()) {
            ReferToHeader sipHeader = (ReferToHeader) sipRequest.getHeader(ReferToHeader.NAME);
            if (sipHeader == null) {
                sendBadRequestResponse(sipRequest, sIPServerTransaction, "Refer-To header is missing");
                return;
            }
            SIPTransaction lastTransaction = dialog.getLastTransaction();
            if (lastTransaction != null && provider.isDialogErrorsAutomaticallyHandled()) {
                SIPRequest lastRequest = (SIPRequest) lastTransaction.getRequest();
                if (lastTransaction instanceof SIPServerTransaction) {
                    if (!dialog.isAckSeen() && lastRequest.getMethod().equals("INVITE")) {
                        sendRequestPendingResponse(sipRequest, sIPServerTransaction);
                        return;
                    }
                } else if (lastTransaction instanceof SIPClientTransaction) {
                    long cseqno = lastRequest.getCSeqHeader().getSeqNumber();
                    String method = lastRequest.getMethod();
                    if (method.equals("INVITE") && !dialog.isAckSent(cseqno)) {
                        sendRequestPendingResponse(sipRequest, sIPServerTransaction);
                        return;
                    }
                }
            }
        } else if (sipRequest.getMethod().equals(Request.UPDATE)) {
            if (provider.isAutomaticDialogSupportEnabled() && dialog == null) {
                sendCallOrTransactionDoesNotExistResponse(sipRequest, sIPServerTransaction);
                return;
            }
        } else if (sipRequest.getMethod().equals("ACK")) {
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
                        sipStackImpl.getStackLogger().logDebug("Dialog does not exist " + sipRequest.getFirstLine() + " isServerTransaction = true");
                    }
                    SIPServerTransaction st = sipStackImpl.getRetransmissionAlertTransaction(dialogId);
                    if (st != null && st.isRetransmissionAlertEnabled()) {
                        st.disableRetransmissionAlerts();
                    }
                    SIPServerTransaction ackTransaction = sipStackImpl.findTransactionPendingAck(sipRequest);
                    if (ackTransaction != null) {
                        if (sipStackImpl.isLoggingEnabled()) {
                            sipStackImpl.getStackLogger().logDebug("Found Tx pending ACK");
                        }
                        try {
                            ackTransaction.setAckSeen();
                            sipStackImpl.removeTransaction(ackTransaction);
                            sipStackImpl.removeTransactionPendingAck(ackTransaction);
                            return;
                        } catch (Exception ex) {
                            if (sipStackImpl.isLoggingEnabled()) {
                                sipStackImpl.getStackLogger().logError("Problem terminating transaction", ex);
                                return;
                            }
                            return;
                        }
                    }
                } else if (!dialog.handleAck(sIPServerTransaction)) {
                    if (!dialog.isSequnceNumberValidation()) {
                        if (sipStackImpl.isLoggingEnabled()) {
                            sipStackImpl.getStackLogger().logDebug("Dialog exists with loose dialog validation " + sipRequest.getFirstLine() + " isServerTransaction = true dialog = " + dialog.getDialogId());
                        }
                        SIPServerTransaction st2 = sipStackImpl.getRetransmissionAlertTransaction(dialogId);
                        if (st2 != null && st2.isRetransmissionAlertEnabled()) {
                            st2.disableRetransmissionAlerts();
                        }
                    } else {
                        if (sipStackImpl.isLoggingEnabled()) {
                            sipStackImpl.getStackLogger().logDebug("Dropping ACK - cannot find a transaction or dialog");
                        }
                        SIPServerTransaction ackTransaction2 = sipStackImpl.findTransactionPendingAck(sipRequest);
                        if (ackTransaction2 != null) {
                            if (sipStackImpl.isLoggingEnabled()) {
                                sipStackImpl.getStackLogger().logDebug("Found Tx pending ACK");
                            }
                            try {
                                ackTransaction2.setAckSeen();
                                sipStackImpl.removeTransaction(ackTransaction2);
                                sipStackImpl.removeTransactionPendingAck(ackTransaction2);
                                return;
                            } catch (Exception ex2) {
                                if (sipStackImpl.isLoggingEnabled()) {
                                    sipStackImpl.getStackLogger().logError("Problem terminating transaction", ex2);
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
                    dialog.addRoute(sipRequest);
                    sIPServerTransaction.setDialog(dialog, dialogId);
                    if (sipRequest.getMethod().equals("INVITE") && provider.isDialogErrorsAutomaticallyHandled()) {
                        sipStackImpl.putInMergeTable(sIPServerTransaction, sipRequest);
                    }
                    if (sipStackImpl.deliverTerminatedEventForAck) {
                        try {
                            sipStackImpl.addTransaction(sIPServerTransaction);
                            sIPServerTransaction.scheduleAckRemoval();
                        } catch (IOException e) {
                        }
                    } else {
                        sIPServerTransaction.setMapped(true);
                    }
                }
            }
        } else if (sipRequest.getMethod().equals(Request.PRACK)) {
            if (sipStackImpl.isLoggingEnabled()) {
                sipStackImpl.getStackLogger().logDebug("Processing PRACK for dialog " + dialog);
            }
            if (dialog == null && provider.isAutomaticDialogSupportEnabled()) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Dialog does not exist " + sipRequest.getFirstLine() + " isServerTransaction = true");
                }
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Sending 481 for PRACK - automatic dialog support is enabled -- cant find dialog!");
                }
                Response notExist = sipRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
                try {
                    provider.sendResponse(notExist);
                } catch (SipException e2) {
                    sipStackImpl.getStackLogger().logError("error sending response", e2);
                }
                if (sIPServerTransaction != null) {
                    sipStackImpl.removeTransaction(sIPServerTransaction);
                    sIPServerTransaction.releaseSem();
                    return;
                }
                return;
            }
            if (dialog != null) {
                if (!dialog.handlePrack(sipRequest)) {
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
                    dialog.addRoute(sipRequest);
                    sIPServerTransaction.setDialog(dialog, dialogId);
                } catch (Exception ex3) {
                    InternalErrorHandler.handleException(ex3);
                }
            } else if (sipStackImpl.isLoggingEnabled()) {
                sipStackImpl.getStackLogger().logDebug("Processing PRACK without a DIALOG -- this must be a proxy element");
            }
        } else if (sipRequest.getMethod().equals("BYE")) {
            if (dialog != null && !dialog.isRequestConsumable(sipRequest)) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Dropping out of sequence BYE " + dialog.getRemoteSeqNumber() + Separators.SP + sipRequest.getCSeq().getSeqNumber());
                }
                if (dialog.getRemoteSeqNumber() >= sipRequest.getCSeq().getSeqNumber() && sIPServerTransaction.getState() == TransactionState.TRYING) {
                    sendServerInternalErrorResponse(sipRequest, sIPServerTransaction);
                }
                if (sIPServerTransaction != null) {
                    sipStackImpl.removeTransaction(sIPServerTransaction);
                    return;
                }
                return;
            }
            if (dialog == null && provider.isAutomaticDialogSupportEnabled()) {
                Response response = sipRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
                response.setReasonPhrase("Dialog Not Found");
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("dropping request -- automatic dialog support enabled and dialog does not exist!");
                }
                try {
                    sIPServerTransaction.sendResponse(response);
                } catch (SipException ex4) {
                    sipStackImpl.getStackLogger().logError("Error in sending response", ex4);
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
                } catch (IOException ex5) {
                    InternalErrorHandler.handleException(ex5);
                }
            }
            if (sipStackImpl.isLoggingEnabled()) {
                sipStackImpl.getStackLogger().logDebug("BYE Tx = " + sIPServerTransaction + " isMapped =" + sIPServerTransaction.isTransactionMapped());
            }
        } else if (sipRequest.getMethod().equals(Request.CANCEL)) {
            SIPServerTransaction st3 = (SIPServerTransaction) sipStackImpl.findCancelTransaction(sipRequest, true);
            if (sipStackImpl.isLoggingEnabled()) {
                sipStackImpl.getStackLogger().logDebug("Got a CANCEL, InviteServerTx = " + st3 + " cancel Server Tx ID = " + sIPServerTransaction + " isMapped = " + sIPServerTransaction.isTransactionMapped());
            }
            if (sipRequest.getMethod().equals(Request.CANCEL)) {
                if (st3 != null && st3.getState() == SIPTransaction.TERMINATED_STATE) {
                    if (sipStackImpl.isLoggingEnabled()) {
                        sipStackImpl.getStackLogger().logDebug("Too late to cancel Transaction");
                    }
                    try {
                        sIPServerTransaction.sendResponse(sipRequest.createResponse(200));
                        return;
                    } catch (Exception ex6) {
                        if (ex6.getCause() != null && (ex6.getCause() instanceof IOException)) {
                            st3.raiseIOExceptionEvent();
                            return;
                        }
                        return;
                    }
                }
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Cancel transaction = " + st3);
                }
            }
            if (sIPServerTransaction != null && st3 != null && st3.getDialog() != null) {
                sIPServerTransaction.setDialog((SIPDialog) st3.getDialog(), dialogId);
                dialog = (SIPDialog) st3.getDialog();
            } else if (st3 == null && provider.isAutomaticDialogSupportEnabled() && sIPServerTransaction != null) {
                Response response2 = sipRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("dropping request -- automatic dialog support enabled and INVITE ST does not exist!");
                }
                try {
                    provider.sendResponse(response2);
                } catch (SipException ex7) {
                    InternalErrorHandler.handleException(ex7);
                }
                if (sIPServerTransaction != null) {
                    sipStackImpl.removeTransaction(sIPServerTransaction);
                    sIPServerTransaction.releaseSem();
                    return;
                }
                return;
            }
            if (st3 != null && sIPServerTransaction != null) {
                try {
                    sipStackImpl.addTransaction(sIPServerTransaction);
                    sIPServerTransaction.setPassToListener();
                    sIPServerTransaction.setInviteTransaction(st3);
                    st3.acquireSem();
                } catch (Exception ex8) {
                    InternalErrorHandler.handleException(ex8);
                }
            }
        } else if (sipRequest.getMethod().equals("INVITE")) {
            SIPServerTransaction lastTransaction2 = dialog == null ? null : dialog.getInviteTransaction();
            if (dialog != null && sIPServerTransaction != null && lastTransaction2 != null && sipRequest.getCSeq().getSeqNumber() > dialog.getRemoteSeqNumber() && (lastTransaction2 instanceof SIPServerTransaction) && provider.isDialogErrorsAutomaticallyHandled() && dialog.isSequnceNumberValidation() && lastTransaction2.isInviteTransaction() && lastTransaction2.getState() != TransactionState.COMPLETED && lastTransaction2.getState() != TransactionState.TERMINATED && lastTransaction2.getState() != TransactionState.CONFIRMED) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Sending 500 response for out of sequence message");
                }
                sendServerInternalErrorResponse(sipRequest, sIPServerTransaction);
                return;
            }
            SIPTransaction lastTransaction3 = dialog == null ? null : dialog.getLastTransaction();
            if (dialog != null && provider.isDialogErrorsAutomaticallyHandled() && lastTransaction3 != null && lastTransaction3.isInviteTransaction() && (lastTransaction3 instanceof ClientTransaction) && lastTransaction3.getLastResponse() != null && lastTransaction3.getLastResponse().getStatusCode() == 200 && !dialog.isAckSent(lastTransaction3.getLastResponse().getCSeq().getSeqNumber())) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Sending 491 response for client Dialog ACK not sent.");
                }
                sendRequestPendingResponse(sipRequest, sIPServerTransaction);
                return;
            } else if (dialog != null && lastTransaction3 != null && provider.isDialogErrorsAutomaticallyHandled() && lastTransaction3.isInviteTransaction() && (lastTransaction3 instanceof ServerTransaction) && !dialog.isAckSeen()) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Sending 491 response for server Dialog ACK not seen.");
                }
                sendRequestPendingResponse(sipRequest, sIPServerTransaction);
                return;
            }
        }
        if (sipStackImpl.isLoggingEnabled()) {
            sipStackImpl.getStackLogger().logDebug("CHECK FOR OUT OF SEQ MESSAGE " + dialog + " transaction " + sIPServerTransaction);
        }
        if (dialog != null && sIPServerTransaction != null && !sipRequest.getMethod().equals("BYE") && !sipRequest.getMethod().equals(Request.CANCEL) && !sipRequest.getMethod().equals("ACK") && !sipRequest.getMethod().equals(Request.PRACK)) {
            if (!dialog.isRequestConsumable(sipRequest)) {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("Dropping out of sequence message " + dialog.getRemoteSeqNumber() + Separators.SP + sipRequest.getCSeq());
                }
                if (dialog.getRemoteSeqNumber() < sipRequest.getCSeq().getSeqNumber() || !provider.isDialogErrorsAutomaticallyHandled()) {
                    return;
                }
                if (sIPServerTransaction.getState() == TransactionState.TRYING || sIPServerTransaction.getState() == TransactionState.PROCEEDING) {
                    sendServerInternalErrorResponse(sipRequest, sIPServerTransaction);
                    return;
                }
                return;
            }
            try {
                if (provider == dialog.getSipProvider()) {
                    sipStackImpl.addTransaction(sIPServerTransaction);
                    dialog.addTransaction(sIPServerTransaction);
                    dialog.addRoute(sipRequest);
                    sIPServerTransaction.setDialog(dialog, dialogId);
                }
            } catch (IOException e3) {
                sIPServerTransaction.raiseIOExceptionEvent();
                sipStackImpl.removeTransaction(sIPServerTransaction);
                return;
            }
        }
        if (sipStackImpl.isLoggingEnabled()) {
            sipStackImpl.getStackLogger().logDebug(sipRequest.getMethod() + " transaction.isMapped = " + sIPServerTransaction.isTransactionMapped());
        }
        if (dialog == null && sipRequest.getMethod().equals("NOTIFY")) {
            SIPClientTransaction pendingSubscribeClientTx = sipStackImpl.findSubscribeTransaction(sipRequest, this.listeningPoint);
            if (sipStackImpl.isLoggingEnabled()) {
                sipStackImpl.getStackLogger().logDebug("PROCESSING NOTIFY  DIALOG == null " + pendingSubscribeClientTx);
            }
            if (provider.isAutomaticDialogSupportEnabled() && pendingSubscribeClientTx == null && !sipStackImpl.deliverUnsolicitedNotify) {
                try {
                    if (sipStackImpl.isLoggingEnabled()) {
                        sipStackImpl.getStackLogger().logDebug("Could not find Subscription for Notify Tx.");
                    }
                    Response errorResponse = sipRequest.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
                    errorResponse.setReasonPhrase("Subscription does not exist");
                    provider.sendResponse(errorResponse);
                    return;
                } catch (Exception ex9) {
                    sipStackImpl.getStackLogger().logError("Exception while sending error response statelessly", ex9);
                    return;
                }
            }
            if (pendingSubscribeClientTx != null) {
                sIPServerTransaction.setPendingSubscribe(pendingSubscribeClientTx);
                SIPDialog subscriptionDialog = pendingSubscribeClientTx.getDefaultDialog();
                if (subscriptionDialog == null || subscriptionDialog.getDialogId() == null || !subscriptionDialog.getDialogId().equals(dialogId)) {
                    if (subscriptionDialog != null && subscriptionDialog.getDialogId() == null) {
                        subscriptionDialog.setDialogId(dialogId);
                    } else {
                        subscriptionDialog = pendingSubscribeClientTx.getDialog(dialogId);
                    }
                    if (sipStackImpl.isLoggingEnabled()) {
                        sipStackImpl.getStackLogger().logDebug("PROCESSING NOTIFY Subscribe DIALOG " + subscriptionDialog);
                    }
                    if (subscriptionDialog == null && (provider.isAutomaticDialogSupportEnabled() || pendingSubscribeClientTx.getDefaultDialog() != null)) {
                        Event event = (Event) sipRequest.getHeader("Event");
                        if (sipStackImpl.isEventForked(event.getEventType())) {
                            subscriptionDialog = SIPDialog.createFromNOTIFY(pendingSubscribeClientTx, sIPServerTransaction);
                        }
                    }
                    if (subscriptionDialog != null) {
                        sIPServerTransaction.setDialog(subscriptionDialog, dialogId);
                        subscriptionDialog.setState(DialogState.CONFIRMED.getValue());
                        sipStackImpl.putDialog(subscriptionDialog);
                        pendingSubscribeClientTx.setDialog(subscriptionDialog, dialogId);
                        if (!sIPServerTransaction.isTransactionMapped()) {
                            this.sipStack.mapTransaction(sIPServerTransaction);
                            sIPServerTransaction.setPassToListener();
                            try {
                                this.sipStack.addTransaction(sIPServerTransaction);
                            } catch (Exception e4) {
                            }
                        }
                    }
                } else {
                    sIPServerTransaction.setDialog(subscriptionDialog, dialogId);
                    if (!sIPServerTransaction.isTransactionMapped()) {
                        this.sipStack.mapTransaction(sIPServerTransaction);
                        sIPServerTransaction.setPassToListener();
                        try {
                            this.sipStack.addTransaction(sIPServerTransaction);
                        } catch (Exception e5) {
                        }
                    }
                    sipStackImpl.putDialog(subscriptionDialog);
                    if (pendingSubscribeClientTx != null) {
                        subscriptionDialog.addTransaction(pendingSubscribeClientTx);
                        pendingSubscribeClientTx.setDialog(subscriptionDialog, dialogId);
                    }
                }
                if (sIPServerTransaction != null && sIPServerTransaction.isTransactionMapped()) {
                    sipEvent = new RequestEvent(provider, sIPServerTransaction, subscriptionDialog, sipRequest);
                } else {
                    sipEvent = new RequestEvent(provider, null, subscriptionDialog, sipRequest);
                }
            } else {
                if (sipStackImpl.isLoggingEnabled()) {
                    sipStackImpl.getStackLogger().logDebug("could not find subscribe tx");
                }
                sipEvent = new RequestEvent(provider, null, null, sipRequest);
            }
        } else if (sIPServerTransaction != null && sIPServerTransaction.isTransactionMapped()) {
            sipEvent = new RequestEvent(provider, sIPServerTransaction, dialog, sipRequest);
        } else {
            sipEvent = new RequestEvent(provider, null, dialog, sipRequest);
        }
        provider.handleEvent(sipEvent, sIPServerTransaction);
    }

    @Override
    public void processResponse(SIPResponse response, MessageChannel incomingMessageChannel, SIPDialog dialog) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("PROCESSING INCOMING RESPONSE" + response.encodeMessage());
        }
        if (this.listeningPoint == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping message: No listening point registered!");
                return;
            }
            return;
        }
        if (this.sipStack.checkBranchId() && !Utils.getInstance().responseBelongsToUs(response)) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping response - topmost VIA header does not originate from this stack");
                return;
            }
            return;
        }
        SipProviderImpl sipProvider = this.listeningPoint.getProvider();
        if (sipProvider == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Dropping message:  no provider");
                return;
            }
            return;
        }
        if (sipProvider.getSipListener() == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("No listener -- dropping response!");
                return;
            }
            return;
        }
        SIPClientTransaction transaction = (SIPClientTransaction) this.transactionChannel;
        SipStackImpl sipStackImpl = sipProvider.sipStack;
        if (this.sipStack.isLoggingEnabled()) {
            sipStackImpl.getStackLogger().logDebug("Transaction = " + transaction);
        }
        if (transaction == null) {
            if (dialog != null) {
                if (response.getStatusCode() / 100 != 2) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Response is not a final response and dialog is found for response -- dropping response!");
                        return;
                    }
                    return;
                }
                if (dialog.getState() == DialogState.TERMINATED) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Dialog is terminated -- dropping response!");
                        return;
                    }
                    return;
                }
                boolean ackAlreadySent = false;
                if (dialog.isAckSeen() && dialog.getLastAckSent() != null && dialog.getLastAckSent().getCSeq().getSeqNumber() == response.getCSeq().getSeqNumber()) {
                    ackAlreadySent = true;
                }
                if (ackAlreadySent && response.getCSeq().getMethod().equals(dialog.getMethod())) {
                    try {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("Retransmission of OK detected: Resending last ACK");
                        }
                        dialog.resendAck();
                        return;
                    } catch (SipException ex) {
                        this.sipStack.getStackLogger().logError("could not resend ack", ex);
                    }
                }
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("could not find tx, handling statelessly Dialog =  " + dialog);
            }
            ResponseEventExt sipEvent = new ResponseEventExt(sipProvider, transaction, dialog, response);
            if (response.getCSeqHeader().getMethod().equals("INVITE")) {
                SIPClientTransaction forked = this.sipStack.getForkedTransaction(response.getTransactionId());
                sipEvent.setOriginalTransaction(forked);
            }
            sipProvider.handleEvent(sipEvent, transaction);
            return;
        }
        ResponseEventExt responseEvent = new ResponseEventExt(sipProvider, transaction, dialog, response);
        if (response.getCSeqHeader().getMethod().equals("INVITE")) {
            SIPClientTransaction forked2 = this.sipStack.getForkedTransaction(response.getTransactionId());
            responseEvent.setOriginalTransaction(forked2);
        }
        if (dialog != null && response.getStatusCode() != 100) {
            dialog.setLastResponse(transaction, response);
            transaction.setDialog(dialog, dialog.getDialogId());
        }
        sipProvider.handleEvent(responseEvent, transaction);
    }

    public String getProcessingInfo() {
        return null;
    }

    @Override
    public void processResponse(SIPResponse sipResponse, MessageChannel incomingChannel) {
        String dialogID = sipResponse.getDialogId(false);
        SIPDialog sipDialog = this.sipStack.getDialog(dialogID);
        String method = sipResponse.getCSeq().getMethod();
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("PROCESSING INCOMING RESPONSE: " + sipResponse.encodeMessage());
        }
        if (this.sipStack.checkBranchId() && !Utils.getInstance().responseBelongsToUs(sipResponse)) {
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
        SipProviderImpl sipProvider = this.listeningPoint.getProvider();
        if (sipProvider == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping message:  no provider");
                return;
            }
            return;
        }
        if (sipProvider.getSipListener() == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping message:  no sipListener registered!");
                return;
            }
            return;
        }
        SIPClientTransaction transaction = (SIPClientTransaction) this.transactionChannel;
        if (sipDialog == null && transaction != null && (sipDialog = transaction.getDialog(dialogID)) != null && sipDialog.getState() == DialogState.TERMINATED) {
            sipDialog = null;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Transaction = " + transaction + " sipDialog = " + sipDialog);
        }
        if (this.transactionChannel != null) {
            String originalFrom = ((SIPRequest) this.transactionChannel.getRequest()).getFromTag();
            if ((originalFrom == null) ^ (sipResponse.getFrom().getTag() == null)) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("From tag mismatch -- dropping response");
                    return;
                }
                return;
            } else if (originalFrom != null && !originalFrom.equalsIgnoreCase(sipResponse.getFrom().getTag())) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("From tag mismatch -- dropping response");
                    return;
                }
                return;
            }
        }
        SipStackImpl sipStackImpl = this.sipStack;
        if (SipStackImpl.isDialogCreated(method) && sipResponse.getStatusCode() != 100 && sipResponse.getFrom().getTag() != null && sipResponse.getTo().getTag() != null && sipDialog == null) {
            if (sipProvider.isAutomaticDialogSupportEnabled()) {
                if (this.transactionChannel != null) {
                    if (sipDialog == null) {
                        sipDialog = this.sipStack.createDialog((SIPClientTransaction) this.transactionChannel, sipResponse);
                        this.transactionChannel.setDialog(sipDialog, sipResponse.getDialogId(false));
                    }
                } else {
                    sipDialog = this.sipStack.createDialog(sipProvider, sipResponse);
                }
            }
        } else if (sipDialog != null && transaction == null && sipDialog.getState() != DialogState.TERMINATED) {
            if (sipResponse.getStatusCode() / 100 != 2) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("status code != 200 ; statusCode = " + sipResponse.getStatusCode());
                }
            } else {
                if (sipDialog.getState() == DialogState.TERMINATED) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Dialog is terminated -- dropping response!");
                    }
                    if (sipResponse.getStatusCode() / 100 == 2 && sipResponse.getCSeq().getMethod().equals("INVITE")) {
                        try {
                            Request ackRequest = sipDialog.createAck(sipResponse.getCSeq().getSeqNumber());
                            sipDialog.sendAck(ackRequest);
                            return;
                        } catch (Exception ex) {
                            this.sipStack.getStackLogger().logError("Error creating ack", ex);
                            return;
                        }
                    }
                    return;
                }
                boolean ackAlreadySent = false;
                if (sipDialog.isAckSeen() && sipDialog.getLastAckSent() != null && sipDialog.getLastAckSent().getCSeq().getSeqNumber() == sipResponse.getCSeq().getSeqNumber() && sipResponse.getDialogId(false).equals(sipDialog.getLastAckSent().getDialogId(false))) {
                    ackAlreadySent = true;
                }
                if (ackAlreadySent && sipResponse.getCSeq().getMethod().equals(sipDialog.getMethod())) {
                    try {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("resending ACK");
                        }
                        sipDialog.resendAck();
                        return;
                    } catch (SipException e) {
                    }
                }
            }
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("sending response to TU for processing ");
        }
        if (sipDialog != null && sipResponse.getStatusCode() != 100 && sipResponse.getTo().getTag() != null) {
            sipDialog.setLastResponse(transaction, sipResponse);
        }
        ResponseEventExt responseEvent = new ResponseEventExt(sipProvider, transaction, sipDialog, sipResponse);
        if (sipResponse.getCSeq().getMethod().equals("INVITE")) {
            ClientTransactionExt originalTx = this.sipStack.getForkedTransaction(sipResponse.getTransactionId());
            responseEvent.setOriginalTransaction(originalTx);
        }
        sipProvider.handleEvent(responseEvent, transaction);
    }
}
