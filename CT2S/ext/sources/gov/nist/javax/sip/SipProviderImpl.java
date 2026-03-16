package gov.nist.javax.sip;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.DialogTimeoutEvent;
import gov.nist.javax.sip.address.ParameterNames;
import gov.nist.javax.sip.address.RouterExt;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.HopImpl;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPDialog;
import gov.nist.javax.sip.stack.SIPDialogErrorEvent;
import gov.nist.javax.sip.stack.SIPDialogEventListener;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.stack.SIPTransaction;
import gov.nist.javax.sip.stack.SIPTransactionErrorEvent;
import gov.nist.javax.sip.stack.SIPTransactionEventListener;
import java.io.IOException;
import java.text.ParseException;
import java.util.EventObject;
import java.util.Iterator;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentHashMap;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.Timeout;
import javax.sip.TimeoutEvent;
import javax.sip.Transaction;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionState;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Hop;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class SipProviderImpl implements SipProvider, SipProviderExt, SIPTransactionEventListener, SIPDialogEventListener {
    private String IN6_ADDR_ANY;
    private String IN_ADDR_ANY;
    private String address;
    private boolean automaticDialogSupportEnabled;
    private boolean dialogErrorsAutomaticallyHandled;
    private EventScanner eventScanner;
    private ConcurrentHashMap listeningPoints;
    private int port;
    private SipListener sipListener;
    protected SipStackImpl sipStack;

    private SipProviderImpl() {
        this.IN_ADDR_ANY = "0.0.0.0";
        this.IN6_ADDR_ANY = "::0";
        this.dialogErrorsAutomaticallyHandled = true;
    }

    protected void stop() {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Exiting provider");
        }
        for (ListeningPointImpl listeningPoint : this.listeningPoints.values()) {
            listeningPoint.removeSipProvider();
        }
        this.eventScanner.stop();
    }

    @Override
    public ListeningPoint getListeningPoint(String transport) {
        if (transport == null) {
            throw new NullPointerException("Null transport param");
        }
        return (ListeningPoint) this.listeningPoints.get(transport.toUpperCase());
    }

    public void handleEvent(EventObject sipEvent, SIPTransaction transaction) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("handleEvent " + sipEvent + "currentTransaction = " + transaction + "this.sipListener = " + getSipListener() + "sipEvent.source = " + sipEvent.getSource());
            if (sipEvent instanceof RequestEvent) {
                Dialog dialog = ((RequestEvent) sipEvent).getDialog();
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dialog = " + dialog);
                }
            } else if (sipEvent instanceof ResponseEvent) {
                Dialog dialog2 = ((ResponseEvent) sipEvent).getDialog();
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dialog = " + dialog2);
                }
            }
            this.sipStack.getStackLogger().logStackTrace();
        }
        EventWrapper eventWrapper = new EventWrapper(sipEvent, transaction);
        if (!this.sipStack.reEntrantListener) {
            this.eventScanner.addEvent(eventWrapper);
        } else {
            this.eventScanner.deliverEvent(eventWrapper);
        }
    }

    protected SipProviderImpl(SipStackImpl sipStack) {
        this.IN_ADDR_ANY = "0.0.0.0";
        this.IN6_ADDR_ANY = "::0";
        this.dialogErrorsAutomaticallyHandled = true;
        this.eventScanner = sipStack.getEventScanner();
        this.sipStack = sipStack;
        this.eventScanner.incrementRefcount();
        this.listeningPoints = new ConcurrentHashMap();
        this.automaticDialogSupportEnabled = this.sipStack.isAutomaticDialogSupportEnabled();
        this.dialogErrorsAutomaticallyHandled = this.sipStack.isAutomaticDialogErrorHandlingEnabled();
    }

    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public void addSipListener(SipListener sipListener) throws TooManyListenersException {
        if (this.sipStack.sipListener == null) {
            this.sipStack.sipListener = sipListener;
        } else if (this.sipStack.sipListener != sipListener) {
            throw new TooManyListenersException("Stack already has a listener. Only one listener per stack allowed");
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("add SipListener " + sipListener);
        }
        this.sipListener = sipListener;
    }

    @Override
    public ListeningPoint getListeningPoint() {
        if (this.listeningPoints.size() > 0) {
            return (ListeningPoint) this.listeningPoints.values().iterator().next();
        }
        return null;
    }

    @Override
    public CallIdHeader getNewCallId() {
        String callId = Utils.getInstance().generateCallIdentifier(getListeningPoint().getIPAddress());
        CallID callid = new CallID();
        try {
            callid.setCallId(callId);
        } catch (ParseException e) {
        }
        return callid;
    }

    @Override
    public ClientTransaction getNewClientTransaction(Request request) throws TransactionUnavailableException {
        SIPClientTransaction ct;
        if (request == null) {
            throw new NullPointerException("null request");
        }
        if (!this.sipStack.isAlive()) {
            throw new TransactionUnavailableException("Stack is stopped");
        }
        SIPRequest sipRequest = (SIPRequest) request;
        if (sipRequest.getTransaction() != null) {
            throw new TransactionUnavailableException("Transaction already assigned to request");
        }
        if (sipRequest.getMethod().equals("ACK")) {
            throw new TransactionUnavailableException("Cannot create client transaction for  ACK");
        }
        if (sipRequest.getTopmostVia() == null) {
            ListeningPointImpl lp = (ListeningPointImpl) getListeningPoint(ParameterNames.UDP);
            Via via = lp.getViaHeader();
            request.setHeader(via);
        }
        try {
            sipRequest.checkHeaders();
            if (sipRequest.getTopmostVia().getBranch() != null && sipRequest.getTopmostVia().getBranch().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE) && this.sipStack.findTransaction((SIPRequest) request, false) != null) {
                throw new TransactionUnavailableException("Transaction already exists!");
            }
            if (request.getMethod().equalsIgnoreCase(Request.CANCEL) && (ct = (SIPClientTransaction) this.sipStack.findCancelTransaction((SIPRequest) request, false)) != null) {
                SIPClientTransaction sIPClientTransactionCreateClientTransaction = this.sipStack.createClientTransaction((SIPRequest) request, ct.getMessageChannel());
                sIPClientTransactionCreateClientTransaction.addEventListener(this);
                this.sipStack.addTransaction(sIPClientTransactionCreateClientTransaction);
                if (ct.getDialog() != null) {
                    sIPClientTransactionCreateClientTransaction.setDialog((SIPDialog) ct.getDialog(), sipRequest.getDialogId(false));
                    return sIPClientTransactionCreateClientTransaction;
                }
                return sIPClientTransactionCreateClientTransaction;
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("could not find existing transaction for " + ((SIPRequest) request).getFirstLine() + " creating a new one ");
            }
            try {
                Hop hop = this.sipStack.getNextHop((SIPRequest) request);
                if (hop == null) {
                    throw new TransactionUnavailableException("Cannot resolve next hop -- transaction unavailable");
                }
                String transport = hop.getTransport();
                ListeningPointImpl listeningPoint = (ListeningPointImpl) getListeningPoint(transport);
                String dialogId = sipRequest.getDialogId(false);
                SIPDialog dialog = this.sipStack.getDialog(dialogId);
                if (dialog != null && dialog.getState() == DialogState.TERMINATED) {
                    this.sipStack.removeDialog(dialog);
                }
                try {
                    if (sipRequest.getTopmostVia().getBranch() == null || !sipRequest.getTopmostVia().getBranch().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE) || this.sipStack.checkBranchId()) {
                        String branchId = Utils.getInstance().generateBranchId();
                        sipRequest.getTopmostVia().setBranch(branchId);
                    }
                    Via topmostVia = sipRequest.getTopmostVia();
                    if (topmostVia.getTransport() == null) {
                        topmostVia.setTransport(transport);
                    }
                    if (topmostVia.getPort() == -1) {
                        topmostVia.setPort(listeningPoint.getPort());
                    }
                    String branchId2 = sipRequest.getTopmostVia().getBranch();
                    SIPClientTransaction ct2 = (SIPClientTransaction) this.sipStack.createMessageChannel(sipRequest, listeningPoint.getMessageProcessor(), hop);
                    if (ct2 == null) {
                        throw new TransactionUnavailableException("Cound not create tx");
                    }
                    ct2.setNextHop(hop);
                    ct2.setOriginalRequest(sipRequest);
                    ct2.setBranch(branchId2);
                    SipStackImpl sipStackImpl = this.sipStack;
                    if (SipStackImpl.isDialogCreated(request.getMethod())) {
                        if (dialog != null) {
                            ct2.setDialog(dialog, sipRequest.getDialogId(false));
                        } else if (isAutomaticDialogSupportEnabled()) {
                            SIPDialog sipDialog = this.sipStack.createDialog(ct2);
                            ct2.setDialog(sipDialog, sipRequest.getDialogId(false));
                        }
                    } else if (dialog != null) {
                        ct2.setDialog(dialog, sipRequest.getDialogId(false));
                    }
                    ct2.addEventListener(this);
                    return ct2;
                } catch (IOException ex) {
                    throw new TransactionUnavailableException("Could not resolve next hop or listening point unavailable! ", ex);
                } catch (ParseException ex2) {
                    InternalErrorHandler.handleException(ex2);
                    throw new TransactionUnavailableException("Unexpected Exception FIXME! ", ex2);
                } catch (InvalidArgumentException ex3) {
                    InternalErrorHandler.handleException(ex3);
                    throw new TransactionUnavailableException("Unexpected Exception FIXME! ", ex3);
                }
            } catch (SipException ex4) {
                throw new TransactionUnavailableException("Cannot resolve next hop -- transaction unavailable", ex4);
            }
        } catch (ParseException ex5) {
            throw new TransactionUnavailableException(ex5.getMessage(), ex5);
        }
    }

    @Override
    public ServerTransaction getNewServerTransaction(Request request) throws TransactionUnavailableException, TransactionAlreadyExistsException {
        SIPServerTransaction transaction;
        if (!this.sipStack.isAlive()) {
            throw new TransactionUnavailableException("Stack is stopped");
        }
        SIPRequest sipRequest = (SIPRequest) request;
        try {
            sipRequest.checkHeaders();
            if (request.getMethod().equals("ACK")) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logError("Creating server transaction for ACK -- makes no sense!");
                }
                throw new TransactionUnavailableException("Cannot create Server transaction for ACK ");
            }
            if (sipRequest.getMethod().equals("NOTIFY") && sipRequest.getFromTag() != null && sipRequest.getToTag() == null) {
                SIPClientTransaction ct = this.sipStack.findSubscribeTransaction(sipRequest, (ListeningPointImpl) getListeningPoint());
                if (ct == null && !this.sipStack.deliverUnsolicitedNotify) {
                    throw new TransactionUnavailableException("Cannot find matching Subscription (and gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY not set)");
                }
            }
            if (!this.sipStack.acquireSem()) {
                throw new TransactionUnavailableException("Transaction not available -- could not acquire stack lock");
            }
            try {
                SipStackImpl sipStackImpl = this.sipStack;
                if (SipStackImpl.isDialogCreated(sipRequest.getMethod())) {
                    if (this.sipStack.findTransaction((SIPRequest) request, true) != null) {
                        throw new TransactionAlreadyExistsException("server transaction already exists!");
                    }
                    transaction = (SIPServerTransaction) ((SIPRequest) request).getTransaction();
                    if (transaction == null) {
                        throw new TransactionUnavailableException("Transaction not available");
                    }
                    if (transaction.getOriginalRequest() == null) {
                        transaction.setOriginalRequest(sipRequest);
                    }
                    try {
                        this.sipStack.addTransaction(transaction);
                        transaction.addEventListener(this);
                        if (isAutomaticDialogSupportEnabled()) {
                            String dialogId = sipRequest.getDialogId(true);
                            SIPDialog dialog = this.sipStack.getDialog(dialogId);
                            if (dialog == null) {
                                dialog = this.sipStack.createDialog(transaction);
                            }
                            transaction.setDialog(dialog, sipRequest.getDialogId(true));
                            if (sipRequest.getMethod().equals("INVITE") && isDialogErrorsAutomaticallyHandled()) {
                                this.sipStack.putInMergeTable(transaction, sipRequest);
                            }
                            dialog.addRoute(sipRequest);
                            if (dialog.getRemoteTag() != null && dialog.getLocalTag() != null) {
                                this.sipStack.putDialog(dialog);
                            }
                        }
                    } catch (IOException e) {
                        throw new TransactionUnavailableException("Error sending provisional response");
                    }
                } else {
                    if (!isAutomaticDialogSupportEnabled()) {
                        if (((SIPServerTransaction) this.sipStack.findTransaction((SIPRequest) request, true)) != null) {
                            throw new TransactionAlreadyExistsException("Transaction exists! ");
                        }
                        SIPServerTransaction transaction2 = (SIPServerTransaction) ((SIPRequest) request).getTransaction();
                        if (transaction2 != null) {
                            if (transaction2.getOriginalRequest() == null) {
                                transaction2.setOriginalRequest(sipRequest);
                            }
                            this.sipStack.mapTransaction(transaction2);
                            String dialogId2 = sipRequest.getDialogId(true);
                            SIPDialog dialog2 = this.sipStack.getDialog(dialogId2);
                            if (dialog2 != null) {
                                dialog2.addTransaction(transaction2);
                                dialog2.addRoute(sipRequest);
                                transaction2.setDialog(dialog2, sipRequest.getDialogId(true));
                            }
                            return transaction2;
                        }
                        MessageChannel mc = (MessageChannel) sipRequest.getMessageChannel();
                        SIPServerTransaction transaction3 = this.sipStack.createServerTransaction(mc);
                        if (transaction3 == null) {
                            throw new TransactionUnavailableException("Transaction unavailable -- too many servrer transactions");
                        }
                        transaction3.setOriginalRequest(sipRequest);
                        this.sipStack.mapTransaction(transaction3);
                        String dialogId3 = sipRequest.getDialogId(true);
                        SIPDialog dialog3 = this.sipStack.getDialog(dialogId3);
                        if (dialog3 != null) {
                            dialog3.addTransaction(transaction3);
                            dialog3.addRoute(sipRequest);
                            transaction3.setDialog(dialog3, sipRequest.getDialogId(true));
                        }
                        return transaction3;
                    }
                    if (((SIPServerTransaction) this.sipStack.findTransaction((SIPRequest) request, true)) != null) {
                        throw new TransactionAlreadyExistsException("Transaction exists! ");
                    }
                    transaction = (SIPServerTransaction) ((SIPRequest) request).getTransaction();
                    if (transaction == null) {
                        throw new TransactionUnavailableException("Transaction not available!");
                    }
                    if (transaction.getOriginalRequest() == null) {
                        transaction.setOriginalRequest(sipRequest);
                    }
                    try {
                        this.sipStack.addTransaction(transaction);
                        String dialogId4 = sipRequest.getDialogId(true);
                        SIPDialog dialog4 = this.sipStack.getDialog(dialogId4);
                        if (dialog4 != null) {
                            dialog4.addTransaction(transaction);
                            dialog4.addRoute(sipRequest);
                            transaction.setDialog(dialog4, sipRequest.getDialogId(true));
                        }
                    } catch (IOException e2) {
                        throw new TransactionUnavailableException("Could not send back provisional response!");
                    }
                }
                this.sipStack.releaseSem();
                return transaction;
            } finally {
                this.sipStack.releaseSem();
            }
        } catch (ParseException ex) {
            throw new TransactionUnavailableException(ex.getMessage(), ex);
        }
    }

    @Override
    public SipStack getSipStack() {
        return this.sipStack;
    }

    @Override
    public void removeSipListener(SipListener sipListener) {
        if (sipListener == getSipListener()) {
            this.sipListener = null;
        }
        boolean found = false;
        Iterator<SipProviderImpl> it = this.sipStack.getSipProviders();
        while (it.hasNext()) {
            SipProviderImpl nextProvider = it.next();
            if (nextProvider.getSipListener() != null) {
                found = true;
            }
        }
        if (!found) {
            this.sipStack.sipListener = null;
        }
    }

    @Override
    public void sendRequest(Request request) throws SipException {
        Via via;
        String branch;
        Dialog dialog;
        if (!this.sipStack.isAlive()) {
            throw new SipException("Stack is stopped.");
        }
        if (((SIPRequest) request).getRequestLine() != null && request.getMethod().equals("ACK") && (dialog = this.sipStack.getDialog(((SIPRequest) request).getDialogId(false))) != null && dialog.getState() != null && this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logWarning("Dialog exists -- you may want to use Dialog.sendAck() " + dialog.getState());
        }
        Hop hop = this.sipStack.getRouter((SIPRequest) request).getNextHop(request);
        if (hop == null) {
            throw new SipException("could not determine next hop!");
        }
        SIPRequest sipRequest = (SIPRequest) request;
        if (!sipRequest.isNullRequest() && sipRequest.getTopmostVia() == null) {
            throw new SipException("Invalid SipRequest -- no via header!");
        }
        try {
            try {
                if (!sipRequest.isNullRequest() && ((branch = (via = sipRequest.getTopmostVia()).getBranch()) == null || branch.length() == 0)) {
                    via.setBranch(sipRequest.getTransactionId());
                }
                MessageChannel messageChannel = null;
                if (this.listeningPoints.containsKey(hop.getTransport().toUpperCase())) {
                    messageChannel = this.sipStack.createRawMessageChannel(getListeningPoint(hop.getTransport()).getIPAddress(), getListeningPoint(hop.getTransport()).getPort(), hop);
                }
                if (messageChannel != null) {
                    messageChannel.sendMessage(sipRequest, hop);
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("done sending " + request.getMethod() + " to hop " + hop);
                        return;
                    }
                    return;
                }
                throw new SipException("Could not create a message channel for " + hop.toString());
            } catch (IOException ex) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logException(ex);
                }
                throw new SipException("IO Exception occured while Sending Request", ex);
            } catch (ParseException ex1) {
                InternalErrorHandler.handleException(ex1);
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("done sending " + request.getMethod() + " to hop " + hop);
                }
            }
        } catch (Throwable th) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("done sending " + request.getMethod() + " to hop " + hop);
            }
            throw th;
        }
    }

    @Override
    public void sendResponse(Response response) throws SipException {
        if (!this.sipStack.isAlive()) {
            throw new SipException("Stack is stopped");
        }
        SIPResponse sipResponse = (SIPResponse) response;
        Via via = sipResponse.getTopmostVia();
        if (via == null) {
            throw new SipException("No via header in response!");
        }
        SIPServerTransaction st = (SIPServerTransaction) this.sipStack.findTransaction((SIPMessage) response, true);
        if (st != null && st.getState() != TransactionState.TERMINATED && isAutomaticDialogSupportEnabled()) {
            throw new SipException("Transaction exists -- cannot send response statelessly");
        }
        String transport = via.getTransport();
        String host = via.getReceived();
        if (host == null) {
            host = via.getHost();
        }
        int port = via.getRPort();
        if (port == -1 && (port = via.getPort()) == -1) {
            if (transport.equalsIgnoreCase("TLS")) {
                port = 5061;
            } else {
                port = 5060;
            }
        }
        if (host.indexOf(Separators.COLON) > 0 && host.indexOf("[") < 0) {
            host = "[" + host + "]";
        }
        Hop hop = this.sipStack.getAddressResolver().resolveAddress(new HopImpl(host, port, transport));
        try {
            ListeningPointImpl listeningPoint = (ListeningPointImpl) getListeningPoint(transport);
            if (listeningPoint == null) {
                throw new SipException("whoopsa daisy! no listening point found for transport " + transport);
            }
            MessageChannel messageChannel = this.sipStack.createRawMessageChannel(getListeningPoint(hop.getTransport()).getIPAddress(), listeningPoint.port, hop);
            messageChannel.sendMessage(sipResponse);
        } catch (IOException ex) {
            throw new SipException(ex.getMessage());
        }
    }

    @Override
    public synchronized void setListeningPoint(ListeningPoint listeningPoint) {
        if (listeningPoint == null) {
            throw new NullPointerException("Null listening point");
        }
        ListeningPointImpl lp = (ListeningPointImpl) listeningPoint;
        lp.sipProvider = this;
        String transport = lp.getTransport().toUpperCase();
        this.address = listeningPoint.getIPAddress();
        this.port = listeningPoint.getPort();
        this.listeningPoints.clear();
        this.listeningPoints.put(transport, listeningPoint);
    }

    @Override
    public Dialog getNewDialog(Transaction transaction) throws SipException {
        SIPDialog dialog;
        if (transaction == null) {
            throw new NullPointerException("Null transaction!");
        }
        if (!this.sipStack.isAlive()) {
            throw new SipException("Stack is stopped.");
        }
        if (isAutomaticDialogSupportEnabled()) {
            throw new SipException(" Error - AUTOMATIC_DIALOG_SUPPORT is on");
        }
        SipStackImpl sipStackImpl = this.sipStack;
        if (!SipStackImpl.isDialogCreated(transaction.getRequest().getMethod())) {
            throw new SipException("Dialog cannot be created for this method " + transaction.getRequest().getMethod());
        }
        SIPTransaction sipTransaction = (SIPTransaction) transaction;
        if (transaction instanceof ServerTransaction) {
            SIPServerTransaction st = (SIPServerTransaction) transaction;
            Response response = st.getLastResponse();
            if (response != null && response.getStatusCode() != 100) {
                throw new SipException("Cannot set dialog after response has been sent");
            }
            SIPRequest sipRequest = (SIPRequest) transaction.getRequest();
            String dialogId = sipRequest.getDialogId(true);
            dialog = this.sipStack.getDialog(dialogId);
            if (dialog == null) {
                dialog = this.sipStack.createDialog((SIPTransaction) transaction);
                dialog.addTransaction(sipTransaction);
                dialog.addRoute(sipRequest);
                sipTransaction.setDialog(dialog, null);
            } else {
                sipTransaction.setDialog(dialog, sipRequest.getDialogId(true));
            }
            if (sipRequest.getMethod().equals("INVITE") && isDialogErrorsAutomaticallyHandled()) {
                this.sipStack.putInMergeTable(st, sipRequest);
            }
        } else {
            SIPClientTransaction sipClientTx = (SIPClientTransaction) transaction;
            if (sipClientTx.getLastResponse() == null) {
                SIPRequest request = (SIPRequest) sipClientTx.getRequest();
                String dialogId2 = request.getDialogId(false);
                if (this.sipStack.getDialog(dialogId2) != null) {
                    throw new SipException("Dialog already exists!");
                }
                dialog = this.sipStack.createDialog(sipTransaction);
                sipClientTx.setDialog(dialog, null);
            } else {
                throw new SipException("Cannot call this method after response is received!");
            }
        }
        dialog.addEventListener(this);
        return dialog;
    }

    @Override
    public void transactionErrorEvent(SIPTransactionErrorEvent transactionErrorEvent) {
        TimeoutEvent ev;
        TimeoutEvent ev2;
        TimeoutEvent ev3;
        SIPTransaction transaction = (SIPTransaction) transactionErrorEvent.getSource();
        if (transactionErrorEvent.getErrorID() == 2) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("TransportError occured on " + transaction);
            }
            Object errorObject = transactionErrorEvent.getSource();
            Timeout timeout = Timeout.TRANSACTION;
            if (errorObject instanceof SIPServerTransaction) {
                ev3 = new TimeoutEvent(this, (ServerTransaction) errorObject, timeout);
            } else {
                SIPClientTransaction clientTx = (SIPClientTransaction) errorObject;
                Hop hop = clientTx.getNextHop();
                if (this.sipStack.getRouter() instanceof RouterExt) {
                    ((RouterExt) this.sipStack.getRouter()).transactionTimeout(hop);
                }
                ev3 = new TimeoutEvent(this, (ClientTransaction) errorObject, timeout);
            }
            handleEvent(ev3, (SIPTransaction) errorObject);
            return;
        }
        if (transactionErrorEvent.getErrorID() == 1) {
            Object errorObject2 = transactionErrorEvent.getSource();
            Timeout timeout2 = Timeout.TRANSACTION;
            if (errorObject2 instanceof SIPServerTransaction) {
                ev2 = new TimeoutEvent(this, (ServerTransaction) errorObject2, timeout2);
            } else {
                SIPClientTransaction clientTx2 = (SIPClientTransaction) errorObject2;
                Hop hop2 = clientTx2.getNextHop();
                if (this.sipStack.getRouter() instanceof RouterExt) {
                    ((RouterExt) this.sipStack.getRouter()).transactionTimeout(hop2);
                }
                ev2 = new TimeoutEvent(this, (ClientTransaction) errorObject2, timeout2);
            }
            handleEvent(ev2, (SIPTransaction) errorObject2);
            return;
        }
        if (transactionErrorEvent.getErrorID() == 3) {
            Object errorObject3 = transactionErrorEvent.getSource();
            Transaction tx = (Transaction) errorObject3;
            if (tx.getDialog() != null) {
                InternalErrorHandler.handleException("Unexpected event !", this.sipStack.getStackLogger());
            }
            Timeout timeout3 = Timeout.RETRANSMIT;
            if (errorObject3 instanceof SIPServerTransaction) {
                ev = new TimeoutEvent(this, (ServerTransaction) errorObject3, timeout3);
            } else {
                ev = new TimeoutEvent(this, (ClientTransaction) errorObject3, timeout3);
            }
            handleEvent(ev, (SIPTransaction) errorObject3);
        }
    }

    @Override
    public synchronized void dialogErrorEvent(SIPDialogErrorEvent dialogErrorEvent) {
        SIPDialog sipDialog = (SIPDialog) dialogErrorEvent.getSource();
        DialogTimeoutEvent.Reason reason = DialogTimeoutEvent.Reason.AckNotReceived;
        if (dialogErrorEvent.getErrorID() == 2) {
            reason = DialogTimeoutEvent.Reason.AckNotSent;
        } else if (dialogErrorEvent.getErrorID() == 3) {
            reason = DialogTimeoutEvent.Reason.ReInviteTimeout;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Dialog TimeoutError occured on " + sipDialog);
        }
        DialogTimeoutEvent ev = new DialogTimeoutEvent(this, sipDialog, reason);
        handleEvent(ev, null);
    }

    @Override
    public synchronized ListeningPoint[] getListeningPoints() {
        ListeningPointImpl[] listeningPointImplArr;
        listeningPointImplArr = new ListeningPointImpl[this.listeningPoints.size()];
        this.listeningPoints.values().toArray(listeningPointImplArr);
        return listeningPointImplArr;
    }

    @Override
    public synchronized void addListeningPoint(ListeningPoint listeningPoint) throws ObjectInUseException {
        ListeningPointImpl lp = (ListeningPointImpl) listeningPoint;
        if (lp.sipProvider != null && lp.sipProvider != this) {
            throw new ObjectInUseException("Listening point assigned to another provider");
        }
        String transport = lp.getTransport().toUpperCase();
        if (this.listeningPoints.isEmpty()) {
            this.address = listeningPoint.getIPAddress();
            this.port = listeningPoint.getPort();
        } else if (!this.address.equals(listeningPoint.getIPAddress()) || this.port != listeningPoint.getPort()) {
            throw new ObjectInUseException("Provider already has different IP Address associated");
        }
        if (this.listeningPoints.containsKey(transport) && this.listeningPoints.get(transport) != listeningPoint) {
            throw new ObjectInUseException("Listening point already assigned for transport!");
        }
        lp.sipProvider = this;
        this.listeningPoints.put(transport, lp);
    }

    @Override
    public synchronized void removeListeningPoint(ListeningPoint listeningPoint) throws ObjectInUseException {
        ListeningPointImpl lp = (ListeningPointImpl) listeningPoint;
        if (lp.messageProcessor.inUse()) {
            throw new ObjectInUseException("Object is in use");
        }
        this.listeningPoints.remove(lp.getTransport().toUpperCase());
    }

    @Override
    public synchronized void removeListeningPoints() {
        Iterator it = this.listeningPoints.values().iterator();
        while (it.hasNext()) {
            ListeningPointImpl lp = (ListeningPointImpl) it.next();
            lp.messageProcessor.stop();
            it.remove();
        }
    }

    @Override
    public void setAutomaticDialogSupportEnabled(boolean automaticDialogSupportEnabled) {
        this.automaticDialogSupportEnabled = automaticDialogSupportEnabled;
        if (this.automaticDialogSupportEnabled) {
            this.dialogErrorsAutomaticallyHandled = true;
        }
    }

    @Override
    public boolean isAutomaticDialogSupportEnabled() {
        return this.automaticDialogSupportEnabled;
    }

    @Override
    public void setDialogErrorsAutomaticallyHandled() {
        this.dialogErrorsAutomaticallyHandled = true;
    }

    public boolean isDialogErrorsAutomaticallyHandled() {
        return this.dialogErrorsAutomaticallyHandled;
    }

    public SipListener getSipListener() {
        return this.sipListener;
    }
}
