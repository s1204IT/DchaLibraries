package gov.nist.javax.sip;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.core.StackLogger;
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
        Iterator it = this.listeningPoints.values().iterator();
        while (it.hasNext()) {
            ((ListeningPointImpl) it.next()).removeSipProvider();
        }
        this.eventScanner.stop();
    }

    @Override
    public ListeningPoint getListeningPoint(String str) {
        if (str == null) {
            throw new NullPointerException("Null transport param");
        }
        return (ListeningPoint) this.listeningPoints.get(str.toUpperCase());
    }

    public void handleEvent(EventObject eventObject, SIPTransaction sIPTransaction) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("handleEvent " + eventObject + "currentTransaction = " + sIPTransaction + "this.sipListener = " + getSipListener() + "sipEvent.source = " + eventObject.getSource());
            if (eventObject instanceof RequestEvent) {
                Dialog dialog = ((RequestEvent) eventObject).getDialog();
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dialog = " + dialog);
                }
            } else if (eventObject instanceof ResponseEvent) {
                Dialog dialog2 = ((ResponseEvent) eventObject).getDialog();
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dialog = " + dialog2);
                }
            }
            this.sipStack.getStackLogger().logStackTrace();
        }
        EventWrapper eventWrapper = new EventWrapper(eventObject, sIPTransaction);
        if (!this.sipStack.reEntrantListener) {
            this.eventScanner.addEvent(eventWrapper);
        } else {
            this.eventScanner.deliverEvent(eventWrapper);
        }
    }

    protected SipProviderImpl(SipStackImpl sipStackImpl) {
        this.IN_ADDR_ANY = "0.0.0.0";
        this.IN6_ADDR_ANY = "::0";
        this.dialogErrorsAutomaticallyHandled = true;
        this.eventScanner = sipStackImpl.getEventScanner();
        this.sipStack = sipStackImpl;
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
        String strGenerateCallIdentifier = Utils.getInstance().generateCallIdentifier(getListeningPoint().getIPAddress());
        CallID callID = new CallID();
        try {
            callID.setCallId(strGenerateCallIdentifier);
        } catch (ParseException e) {
        }
        return callID;
    }

    @Override
    public ClientTransaction getNewClientTransaction(Request request) throws TransactionUnavailableException {
        SIPClientTransaction sIPClientTransaction;
        if (request == null) {
            throw new NullPointerException("null request");
        }
        if (!this.sipStack.isAlive()) {
            throw new TransactionUnavailableException("Stack is stopped");
        }
        SIPRequest sIPRequest = (SIPRequest) request;
        if (sIPRequest.getTransaction() != null) {
            throw new TransactionUnavailableException("Transaction already assigned to request");
        }
        if (sIPRequest.getMethod().equals("ACK")) {
            throw new TransactionUnavailableException("Cannot create client transaction for  ACK");
        }
        if (sIPRequest.getTopmostVia() == null) {
            request.setHeader(((ListeningPointImpl) getListeningPoint(ParameterNames.UDP)).getViaHeader());
        }
        try {
            sIPRequest.checkHeaders();
            if (sIPRequest.getTopmostVia().getBranch() != null && sIPRequest.getTopmostVia().getBranch().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE) && this.sipStack.findTransaction(sIPRequest, false) != null) {
                throw new TransactionUnavailableException("Transaction already exists!");
            }
            if (request.getMethod().equalsIgnoreCase(Request.CANCEL) && (sIPClientTransaction = (SIPClientTransaction) this.sipStack.findCancelTransaction(sIPRequest, false)) != null) {
                SIPClientTransaction sIPClientTransactionCreateClientTransaction = this.sipStack.createClientTransaction(sIPRequest, sIPClientTransaction.getMessageChannel());
                sIPClientTransactionCreateClientTransaction.addEventListener(this);
                SIPClientTransaction sIPClientTransaction2 = sIPClientTransactionCreateClientTransaction;
                this.sipStack.addTransaction(sIPClientTransaction2);
                if (sIPClientTransaction.getDialog() != null) {
                    sIPClientTransaction2.setDialog((SIPDialog) sIPClientTransaction.getDialog(), sIPRequest.getDialogId(false));
                }
                return sIPClientTransactionCreateClientTransaction;
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("could not find existing transaction for " + sIPRequest.getFirstLine() + " creating a new one ");
            }
            try {
                Hop nextHop = this.sipStack.getNextHop((SIPRequest) request);
                if (nextHop == null) {
                    throw new TransactionUnavailableException("Cannot resolve next hop -- transaction unavailable");
                }
                String transport = nextHop.getTransport();
                ListeningPointImpl listeningPointImpl = (ListeningPointImpl) getListeningPoint(transport);
                SIPDialog dialog = this.sipStack.getDialog(sIPRequest.getDialogId(false));
                if (dialog != null && dialog.getState() == DialogState.TERMINATED) {
                    this.sipStack.removeDialog(dialog);
                }
                try {
                    if (sIPRequest.getTopmostVia().getBranch() == null || !sIPRequest.getTopmostVia().getBranch().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE) || this.sipStack.checkBranchId()) {
                        sIPRequest.getTopmostVia().setBranch(Utils.getInstance().generateBranchId());
                    }
                    Via topmostVia = sIPRequest.getTopmostVia();
                    if (topmostVia.getTransport() == null) {
                        topmostVia.setTransport(transport);
                    }
                    if (topmostVia.getPort() == -1) {
                        topmostVia.setPort(listeningPointImpl.getPort());
                    }
                    String branch = sIPRequest.getTopmostVia().getBranch();
                    SIPClientTransaction sIPClientTransaction3 = (SIPClientTransaction) this.sipStack.createMessageChannel(sIPRequest, listeningPointImpl.getMessageProcessor(), nextHop);
                    if (sIPClientTransaction3 == null) {
                        throw new TransactionUnavailableException("Cound not create tx");
                    }
                    sIPClientTransaction3.setNextHop(nextHop);
                    sIPClientTransaction3.setOriginalRequest(sIPRequest);
                    sIPClientTransaction3.setBranch(branch);
                    SipStackImpl sipStackImpl = this.sipStack;
                    if (SipStackImpl.isDialogCreated(request.getMethod())) {
                        if (dialog != null) {
                            sIPClientTransaction3.setDialog(dialog, sIPRequest.getDialogId(false));
                        } else if (isAutomaticDialogSupportEnabled()) {
                            sIPClientTransaction3.setDialog(this.sipStack.createDialog(sIPClientTransaction3), sIPRequest.getDialogId(false));
                        }
                    } else if (dialog != null) {
                        sIPClientTransaction3.setDialog(dialog, sIPRequest.getDialogId(false));
                    }
                    sIPClientTransaction3.addEventListener(this);
                    return sIPClientTransaction3;
                } catch (IOException e) {
                    throw new TransactionUnavailableException("Could not resolve next hop or listening point unavailable! ", e);
                } catch (ParseException e2) {
                    InternalErrorHandler.handleException(e2);
                    throw new TransactionUnavailableException("Unexpected Exception FIXME! ", e2);
                } catch (InvalidArgumentException e3) {
                    InternalErrorHandler.handleException(e3);
                    throw new TransactionUnavailableException("Unexpected Exception FIXME! ", e3);
                }
            } catch (SipException e4) {
                throw new TransactionUnavailableException("Cannot resolve next hop -- transaction unavailable", e4);
            }
        } catch (ParseException e5) {
            throw new TransactionUnavailableException(e5.getMessage(), e5);
        }
    }

    @Override
    public ServerTransaction getNewServerTransaction(Request request) throws TransactionUnavailableException, TransactionAlreadyExistsException {
        SIPServerTransaction sIPServerTransaction;
        if (!this.sipStack.isAlive()) {
            throw new TransactionUnavailableException("Stack is stopped");
        }
        SIPRequest sIPRequest = (SIPRequest) request;
        try {
            sIPRequest.checkHeaders();
            if (request.getMethod().equals("ACK")) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logError("Creating server transaction for ACK -- makes no sense!");
                }
                throw new TransactionUnavailableException("Cannot create Server transaction for ACK ");
            }
            if (sIPRequest.getMethod().equals("NOTIFY") && sIPRequest.getFromTag() != null && sIPRequest.getToTag() == null && this.sipStack.findSubscribeTransaction(sIPRequest, (ListeningPointImpl) getListeningPoint()) == null && !this.sipStack.deliverUnsolicitedNotify) {
                throw new TransactionUnavailableException("Cannot find matching Subscription (and gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY not set)");
            }
            if (!this.sipStack.acquireSem()) {
                throw new TransactionUnavailableException("Transaction not available -- could not acquire stack lock");
            }
            try {
                SipStackImpl sipStackImpl = this.sipStack;
                if (SipStackImpl.isDialogCreated(sIPRequest.getMethod())) {
                    if (this.sipStack.findTransaction((SIPRequest) request, true) != null) {
                        throw new TransactionAlreadyExistsException("server transaction already exists!");
                    }
                    sIPServerTransaction = (SIPServerTransaction) ((SIPRequest) request).getTransaction();
                    if (sIPServerTransaction == null) {
                        throw new TransactionUnavailableException("Transaction not available");
                    }
                    if (sIPServerTransaction.getOriginalRequest() == null) {
                        sIPServerTransaction.setOriginalRequest(sIPRequest);
                    }
                    try {
                        this.sipStack.addTransaction(sIPServerTransaction);
                        sIPServerTransaction.addEventListener(this);
                        if (isAutomaticDialogSupportEnabled()) {
                            SIPDialog dialog = this.sipStack.getDialog(sIPRequest.getDialogId(true));
                            if (dialog == null) {
                                dialog = this.sipStack.createDialog(sIPServerTransaction);
                            }
                            sIPServerTransaction.setDialog(dialog, sIPRequest.getDialogId(true));
                            if (sIPRequest.getMethod().equals("INVITE") && isDialogErrorsAutomaticallyHandled()) {
                                this.sipStack.putInMergeTable(sIPServerTransaction, sIPRequest);
                            }
                            dialog.addRoute(sIPRequest);
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
                        SIPServerTransaction sIPServerTransaction2 = (SIPServerTransaction) ((SIPRequest) request).getTransaction();
                        if (sIPServerTransaction2 != null) {
                            if (sIPServerTransaction2.getOriginalRequest() == null) {
                                sIPServerTransaction2.setOriginalRequest(sIPRequest);
                            }
                            this.sipStack.mapTransaction(sIPServerTransaction2);
                            SIPDialog dialog2 = this.sipStack.getDialog(sIPRequest.getDialogId(true));
                            if (dialog2 != null) {
                                dialog2.addTransaction(sIPServerTransaction2);
                                dialog2.addRoute(sIPRequest);
                                sIPServerTransaction2.setDialog(dialog2, sIPRequest.getDialogId(true));
                            }
                            return sIPServerTransaction2;
                        }
                        SIPServerTransaction sIPServerTransactionCreateServerTransaction = this.sipStack.createServerTransaction((MessageChannel) sIPRequest.getMessageChannel());
                        if (sIPServerTransactionCreateServerTransaction == null) {
                            throw new TransactionUnavailableException("Transaction unavailable -- too many servrer transactions");
                        }
                        sIPServerTransactionCreateServerTransaction.setOriginalRequest(sIPRequest);
                        this.sipStack.mapTransaction(sIPServerTransactionCreateServerTransaction);
                        SIPDialog dialog3 = this.sipStack.getDialog(sIPRequest.getDialogId(true));
                        if (dialog3 != null) {
                            dialog3.addTransaction(sIPServerTransactionCreateServerTransaction);
                            dialog3.addRoute(sIPRequest);
                            sIPServerTransactionCreateServerTransaction.setDialog(dialog3, sIPRequest.getDialogId(true));
                        }
                        return sIPServerTransactionCreateServerTransaction;
                    }
                    if (((SIPServerTransaction) this.sipStack.findTransaction((SIPRequest) request, true)) != null) {
                        throw new TransactionAlreadyExistsException("Transaction exists! ");
                    }
                    sIPServerTransaction = (SIPServerTransaction) ((SIPRequest) request).getTransaction();
                    if (sIPServerTransaction == null) {
                        throw new TransactionUnavailableException("Transaction not available!");
                    }
                    if (sIPServerTransaction.getOriginalRequest() == null) {
                        sIPServerTransaction.setOriginalRequest(sIPRequest);
                    }
                    try {
                        this.sipStack.addTransaction(sIPServerTransaction);
                        SIPDialog dialog4 = this.sipStack.getDialog(sIPRequest.getDialogId(true));
                        if (dialog4 != null) {
                            dialog4.addTransaction(sIPServerTransaction);
                            dialog4.addRoute(sIPRequest);
                            sIPServerTransaction.setDialog(dialog4, sIPRequest.getDialogId(true));
                        }
                    } catch (IOException e2) {
                        throw new TransactionUnavailableException("Could not send back provisional response!");
                    }
                }
                return sIPServerTransaction;
            } finally {
                this.sipStack.releaseSem();
            }
        } catch (ParseException e3) {
            throw new TransactionUnavailableException(e3.getMessage(), e3);
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
        boolean z = false;
        Iterator<SipProviderImpl> sipProviders = this.sipStack.getSipProviders();
        while (sipProviders.hasNext()) {
            if (sipProviders.next().getSipListener() != null) {
                z = true;
            }
        }
        if (!z) {
            this.sipStack.sipListener = null;
        }
    }

    @Override
    public void sendRequest(Request request) throws SipException {
        StackLogger stackLogger;
        StringBuilder sb;
        MessageChannel messageChannelCreateRawMessageChannel;
        Via topmostVia;
        String branch;
        SIPDialog dialog;
        if (!this.sipStack.isAlive()) {
            throw new SipException("Stack is stopped.");
        }
        SIPRequest sIPRequest = (SIPRequest) request;
        if (sIPRequest.getRequestLine() != null && request.getMethod().equals("ACK") && (dialog = this.sipStack.getDialog(sIPRequest.getDialogId(false))) != null && dialog.getState() != null && this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logWarning("Dialog exists -- you may want to use Dialog.sendAck() " + dialog.getState());
        }
        Hop nextHop = this.sipStack.getRouter(sIPRequest).getNextHop(request);
        if (nextHop == null) {
            throw new SipException("could not determine next hop!");
        }
        if (!sIPRequest.isNullRequest() && sIPRequest.getTopmostVia() == null) {
            throw new SipException("Invalid SipRequest -- no via header!");
        }
        try {
            try {
                try {
                    if (!sIPRequest.isNullRequest() && ((branch = (topmostVia = sIPRequest.getTopmostVia()).getBranch()) == null || branch.length() == 0)) {
                        topmostVia.setBranch(sIPRequest.getTransactionId());
                    }
                    messageChannelCreateRawMessageChannel = this.listeningPoints.containsKey(nextHop.getTransport().toUpperCase()) ? this.sipStack.createRawMessageChannel(getListeningPoint(nextHop.getTransport()).getIPAddress(), getListeningPoint(nextHop.getTransport()).getPort(), nextHop) : null;
                } catch (ParseException e) {
                    InternalErrorHandler.handleException(e);
                    if (!this.sipStack.isLoggingEnabled()) {
                        return;
                    }
                    stackLogger = this.sipStack.getStackLogger();
                    sb = new StringBuilder();
                }
                if (messageChannelCreateRawMessageChannel == null) {
                    throw new SipException("Could not create a message channel for " + nextHop.toString());
                }
                messageChannelCreateRawMessageChannel.sendMessage(sIPRequest, nextHop);
                if (this.sipStack.isLoggingEnabled()) {
                    stackLogger = this.sipStack.getStackLogger();
                    sb = new StringBuilder();
                    sb.append("done sending ");
                    sb.append(request.getMethod());
                    sb.append(" to hop ");
                    sb.append(nextHop);
                    stackLogger.logDebug(sb.toString());
                }
            } catch (IOException e2) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logException(e2);
                }
                throw new SipException("IO Exception occured while Sending Request", e2);
            }
        } catch (Throwable th) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("done sending " + request.getMethod() + " to hop " + nextHop);
            }
            throw th;
        }
    }

    @Override
    public void sendResponse(Response response) throws SipException {
        if (!this.sipStack.isAlive()) {
            throw new SipException("Stack is stopped");
        }
        SIPResponse sIPResponse = (SIPResponse) response;
        Via topmostVia = sIPResponse.getTopmostVia();
        if (topmostVia == null) {
            throw new SipException("No via header in response!");
        }
        SIPServerTransaction sIPServerTransaction = (SIPServerTransaction) this.sipStack.findTransaction((SIPMessage) response, true);
        if (sIPServerTransaction != null && sIPServerTransaction.getState() != TransactionState.TERMINATED && isAutomaticDialogSupportEnabled()) {
            throw new SipException("Transaction exists -- cannot send response statelessly");
        }
        String transport = topmostVia.getTransport();
        String received = topmostVia.getReceived();
        if (received == null) {
            received = topmostVia.getHost();
        }
        int rPort = topmostVia.getRPort();
        if (rPort == -1 && (rPort = topmostVia.getPort()) == -1) {
            if (transport.equalsIgnoreCase(ListeningPoint.TLS)) {
                rPort = 5061;
            } else {
                rPort = 5060;
            }
        }
        if (received.indexOf(Separators.COLON) > 0 && received.indexOf("[") < 0) {
            received = "[" + received + "]";
        }
        Hop hopResolveAddress = this.sipStack.getAddressResolver().resolveAddress(new HopImpl(received, rPort, transport));
        try {
            ListeningPointImpl listeningPointImpl = (ListeningPointImpl) getListeningPoint(transport);
            if (listeningPointImpl == null) {
                throw new SipException("whoopsa daisy! no listening point found for transport " + transport);
            }
            this.sipStack.createRawMessageChannel(getListeningPoint(hopResolveAddress.getTransport()).getIPAddress(), listeningPointImpl.port, hopResolveAddress).sendMessage(sIPResponse);
        } catch (IOException e) {
            throw new SipException(e.getMessage());
        }
    }

    @Override
    public synchronized void setListeningPoint(ListeningPoint listeningPoint) {
        if (listeningPoint == null) {
            throw new NullPointerException("Null listening point");
        }
        ListeningPointImpl listeningPointImpl = (ListeningPointImpl) listeningPoint;
        listeningPointImpl.sipProvider = this;
        String upperCase = listeningPointImpl.getTransport().toUpperCase();
        this.address = listeningPoint.getIPAddress();
        this.port = listeningPoint.getPort();
        this.listeningPoints.clear();
        this.listeningPoints.put(upperCase, listeningPoint);
    }

    @Override
    public Dialog getNewDialog(Transaction transaction) throws SipException {
        SIPDialog sIPDialogCreateDialog;
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
        SIPTransaction sIPTransaction = (SIPTransaction) transaction;
        if (transaction instanceof ServerTransaction) {
            SIPServerTransaction sIPServerTransaction = (SIPServerTransaction) transaction;
            SIPResponse lastResponse = sIPServerTransaction.getLastResponse();
            if (lastResponse != null && lastResponse.getStatusCode() != 100) {
                throw new SipException("Cannot set dialog after response has been sent");
            }
            SIPRequest sIPRequest = (SIPRequest) transaction.getRequest();
            sIPDialogCreateDialog = this.sipStack.getDialog(sIPRequest.getDialogId(true));
            if (sIPDialogCreateDialog == null) {
                sIPDialogCreateDialog = this.sipStack.createDialog(sIPTransaction);
                sIPDialogCreateDialog.addTransaction(sIPTransaction);
                sIPDialogCreateDialog.addRoute(sIPRequest);
                sIPTransaction.setDialog(sIPDialogCreateDialog, null);
            } else {
                sIPTransaction.setDialog(sIPDialogCreateDialog, sIPRequest.getDialogId(true));
            }
            if (sIPRequest.getMethod().equals("INVITE") && isDialogErrorsAutomaticallyHandled()) {
                this.sipStack.putInMergeTable(sIPServerTransaction, sIPRequest);
            }
        } else {
            SIPClientTransaction sIPClientTransaction = (SIPClientTransaction) transaction;
            if (sIPClientTransaction.getLastResponse() == null) {
                if (this.sipStack.getDialog(((SIPRequest) sIPClientTransaction.getRequest()).getDialogId(false)) != null) {
                    throw new SipException("Dialog already exists!");
                }
                sIPDialogCreateDialog = this.sipStack.createDialog(sIPTransaction);
                sIPClientTransaction.setDialog(sIPDialogCreateDialog, null);
            } else {
                throw new SipException("Cannot call this method after response is received!");
            }
        }
        sIPDialogCreateDialog.addEventListener(this);
        return sIPDialogCreateDialog;
    }

    @Override
    public void transactionErrorEvent(SIPTransactionErrorEvent sIPTransactionErrorEvent) {
        TimeoutEvent timeoutEvent;
        TimeoutEvent timeoutEvent2;
        TimeoutEvent timeoutEvent3;
        SIPTransaction sIPTransaction = (SIPTransaction) sIPTransactionErrorEvent.getSource();
        if (sIPTransactionErrorEvent.getErrorID() == 2) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("TransportError occured on " + sIPTransaction);
            }
            Object source = sIPTransactionErrorEvent.getSource();
            Timeout timeout = Timeout.TRANSACTION;
            if (source instanceof SIPServerTransaction) {
                timeoutEvent3 = new TimeoutEvent(this, (ServerTransaction) source, timeout);
            } else {
                Hop nextHop = ((SIPClientTransaction) source).getNextHop();
                if (this.sipStack.getRouter() instanceof RouterExt) {
                    ((RouterExt) this.sipStack.getRouter()).transactionTimeout(nextHop);
                }
                timeoutEvent3 = new TimeoutEvent(this, (ClientTransaction) source, timeout);
            }
            handleEvent(timeoutEvent3, (SIPTransaction) source);
            return;
        }
        if (sIPTransactionErrorEvent.getErrorID() == 1) {
            Object source2 = sIPTransactionErrorEvent.getSource();
            Timeout timeout2 = Timeout.TRANSACTION;
            if (source2 instanceof SIPServerTransaction) {
                timeoutEvent2 = new TimeoutEvent(this, (ServerTransaction) source2, timeout2);
            } else {
                Hop nextHop2 = ((SIPClientTransaction) source2).getNextHop();
                if (this.sipStack.getRouter() instanceof RouterExt) {
                    ((RouterExt) this.sipStack.getRouter()).transactionTimeout(nextHop2);
                }
                timeoutEvent2 = new TimeoutEvent(this, (ClientTransaction) source2, timeout2);
            }
            handleEvent(timeoutEvent2, (SIPTransaction) source2);
            return;
        }
        if (sIPTransactionErrorEvent.getErrorID() == 3) {
            Object source3 = sIPTransactionErrorEvent.getSource();
            if (((Transaction) source3).getDialog() != null) {
                InternalErrorHandler.handleException("Unexpected event !", this.sipStack.getStackLogger());
            }
            Timeout timeout3 = Timeout.RETRANSMIT;
            if (source3 instanceof SIPServerTransaction) {
                timeoutEvent = new TimeoutEvent(this, (ServerTransaction) source3, timeout3);
            } else {
                timeoutEvent = new TimeoutEvent(this, (ClientTransaction) source3, timeout3);
            }
            handleEvent(timeoutEvent, (SIPTransaction) source3);
        }
    }

    @Override
    public synchronized void dialogErrorEvent(SIPDialogErrorEvent sIPDialogErrorEvent) {
        SIPDialog sIPDialog = (SIPDialog) sIPDialogErrorEvent.getSource();
        DialogTimeoutEvent.Reason reason = DialogTimeoutEvent.Reason.AckNotReceived;
        if (sIPDialogErrorEvent.getErrorID() == 2) {
            reason = DialogTimeoutEvent.Reason.AckNotSent;
        } else if (sIPDialogErrorEvent.getErrorID() == 3) {
            reason = DialogTimeoutEvent.Reason.ReInviteTimeout;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Dialog TimeoutError occured on " + sIPDialog);
        }
        handleEvent(new DialogTimeoutEvent(this, sIPDialog, reason), null);
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
        ListeningPointImpl listeningPointImpl = (ListeningPointImpl) listeningPoint;
        if (listeningPointImpl.sipProvider != null && listeningPointImpl.sipProvider != this) {
            throw new ObjectInUseException("Listening point assigned to another provider");
        }
        String upperCase = listeningPointImpl.getTransport().toUpperCase();
        if (this.listeningPoints.isEmpty()) {
            this.address = listeningPoint.getIPAddress();
            this.port = listeningPoint.getPort();
        } else if (!this.address.equals(listeningPoint.getIPAddress()) || this.port != listeningPoint.getPort()) {
            throw new ObjectInUseException("Provider already has different IP Address associated");
        }
        if (this.listeningPoints.containsKey(upperCase) && this.listeningPoints.get(upperCase) != listeningPoint) {
            throw new ObjectInUseException("Listening point already assigned for transport!");
        }
        listeningPointImpl.sipProvider = this;
        this.listeningPoints.put(upperCase, listeningPointImpl);
    }

    @Override
    public synchronized void removeListeningPoint(ListeningPoint listeningPoint) throws ObjectInUseException {
        ListeningPointImpl listeningPointImpl = (ListeningPointImpl) listeningPoint;
        if (listeningPointImpl.messageProcessor.inUse()) {
            throw new ObjectInUseException("Object is in use");
        }
        this.listeningPoints.remove(listeningPointImpl.getTransport().toUpperCase());
    }

    @Override
    public synchronized void removeListeningPoints() {
        Iterator it = this.listeningPoints.values().iterator();
        while (it.hasNext()) {
            ((ListeningPointImpl) it.next()).messageProcessor.stop();
            it.remove();
        }
    }

    @Override
    public void setAutomaticDialogSupportEnabled(boolean z) {
        this.automaticDialogSupportEnabled = z;
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
