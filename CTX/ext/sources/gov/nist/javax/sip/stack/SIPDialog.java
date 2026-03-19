package gov.nist.javax.sip.stack;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.NameValueList;
import gov.nist.core.Separators;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.DialogExt;
import gov.nist.javax.sip.ListeningPointImpl;
import gov.nist.javax.sip.SipListenerExt;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.Utils;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.CSeq;
import gov.nist.javax.sip.header.Contact;
import gov.nist.javax.sip.header.ContactList;
import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.MaxForwards;
import gov.nist.javax.sip.header.RAck;
import gov.nist.javax.sip.header.RSeq;
import gov.nist.javax.sip.header.Reason;
import gov.nist.javax.sip.header.RecordRoute;
import gov.nist.javax.sip.header.RecordRouteList;
import gov.nist.javax.sip.header.Require;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.TimeStamp;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogDoesNotExistException;
import javax.sip.DialogState;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.SipException;
import javax.sip.Transaction;
import javax.sip.TransactionState;
import javax.sip.address.Address;
import javax.sip.address.Hop;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.Header;
import javax.sip.header.OptionTag;
import javax.sip.header.RequireHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import org.ccil.cowan.tagsoup.HTMLModels;

public class SIPDialog implements Dialog, DialogExt {
    private static final int DIALOG_LINGER_TIME = 8;
    public static final int NULL_STATE = -1;
    private static final long serialVersionUID = -1429794423085204069L;
    private transient int ackLine;
    protected transient boolean ackProcessed;
    protected transient boolean ackSeen;
    private transient Semaphore ackSem;
    private transient Object applicationData;
    public transient long auditTag;
    private transient boolean byeSent;
    protected CallIdHeader callIdHeader;
    protected Contact contactHeader;
    private transient DialogDeleteIfNoAckSentTask dialogDeleteIfNoAckSentTask;
    private transient DialogDeleteTask dialogDeleteTask;
    private String dialogId;
    private int dialogState;
    private transient boolean dialogTerminatedEventDelivered;
    private transient String earlyDialogId;
    private EventHeader eventHeader;
    private transient Set<SIPDialogEventListener> eventListeners;
    private transient SIPTransaction firstTransaction;
    protected String firstTransactionId;
    protected boolean firstTransactionIsServerTransaction;
    protected String firstTransactionMethod;
    protected int firstTransactionPort;
    protected boolean firstTransactionSecure;
    protected boolean firstTransactionSeen;
    private transient long highestSequenceNumberAcknowledged;
    protected String hisTag;
    private transient boolean isAcknowledged;
    private transient boolean isAssigned;
    private boolean isBackToBackUserAgent;
    private SIPRequest lastAckReceived;
    private transient SIPRequest lastAckSent;
    private transient long lastInviteOkReceived;
    private SIPResponse lastResponse;
    private transient SIPTransaction lastTransaction;
    protected Address localParty;
    private long localSequenceNumber;
    private String method;
    protected String myTag;
    protected transient Long nextSeqno;
    private long originalLocalSequenceNumber;
    private transient SIPRequest originalRequest;
    private transient int prevRetransmissionTicks;
    private boolean reInviteFlag;
    private transient int reInviteWaitTime;
    protected Address remoteParty;
    private long remoteSequenceNumber;
    private Address remoteTarget;
    private transient int retransmissionTicksLeft;
    private RouteList routeList;
    private boolean sequenceNumberValidation;
    private boolean serverTransactionFlag;
    private transient SipProviderImpl sipProvider;
    private transient SIPTransactionStack sipStack;
    private transient String stackTrace;
    private boolean terminateOnBye;
    protected transient DialogTimerTask timerTask;
    private Semaphore timerTaskLock;
    public static final int EARLY_STATE = DialogState._EARLY;
    public static final int CONFIRMED_STATE = DialogState._CONFIRMED;
    public static final int TERMINATED_STATE = DialogState._TERMINATED;

    public class ReInviteSender implements Runnable, Serializable {
        private static final long serialVersionUID = 1019346148741070635L;
        ClientTransaction ctx;

        public void terminate() {
            try {
                this.ctx.terminate();
                Thread.currentThread().interrupt();
            } catch (ObjectInUseException e) {
                SIPDialog.this.sipStack.getStackLogger().logError("unexpected error", e);
            }
        }

        public ReInviteSender(ClientTransaction clientTransaction) {
            this.ctx = clientTransaction;
        }

        @Override
        public void run() {
            try {
                try {
                    long jCurrentTimeMillis = System.currentTimeMillis();
                    if (!SIPDialog.this.takeAckSem()) {
                        if (SIPDialog.this.sipStack.isLoggingEnabled()) {
                            SIPDialog.this.sipStack.getStackLogger().logError("Could not send re-INVITE time out ClientTransaction");
                        }
                        ((SIPClientTransaction) this.ctx).fireTimeoutTimer();
                        if (SIPDialog.this.sipProvider.getSipListener() == null || !(SIPDialog.this.sipProvider.getSipListener() instanceof SipListenerExt)) {
                            Request requestCreateRequest = SIPDialog.this.createRequest("BYE");
                            if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
                                requestCreateRequest.addHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
                            }
                            Reason reason = new Reason();
                            reason.setCause(HTMLModels.M_HEAD);
                            reason.setText("Timed out waiting to re-INVITE");
                            requestCreateRequest.addHeader(reason);
                            SIPDialog.this.sendRequest(SIPDialog.this.getSipProvider().getNewClientTransaction(requestCreateRequest));
                            return;
                        }
                        SIPDialog.this.raiseErrorEvent(3);
                    }
                    if ((SIPDialog.this.getState() != DialogState.TERMINATED ? System.currentTimeMillis() - jCurrentTimeMillis : 0L) != 0) {
                        try {
                            Thread.sleep(SIPDialog.this.reInviteWaitTime);
                        } catch (InterruptedException e) {
                            if (SIPDialog.this.sipStack.isLoggingEnabled()) {
                                SIPDialog.this.sipStack.getStackLogger().logDebug("Interrupted sleep");
                            }
                            return;
                        }
                    }
                    if (SIPDialog.this.getState() != DialogState.TERMINATED) {
                        SIPDialog.this.sendRequest(this.ctx, true);
                    }
                    if (SIPDialog.this.sipStack.isLoggingEnabled()) {
                        SIPDialog.this.sipStack.getStackLogger().logDebug("re-INVITE successfully sent");
                    }
                } catch (Exception e2) {
                    SIPDialog.this.sipStack.getStackLogger().logError("Error sending re-INVITE", e2);
                }
            } finally {
                this.ctx = null;
            }
        }
    }

    class LingerTimer extends SIPStackTimerTask implements Serializable {
        public LingerTimer() {
        }

        @Override
        protected void runTask() {
            SIPDialog sIPDialog = SIPDialog.this;
            if (SIPDialog.this.eventListeners != null) {
                SIPDialog.this.eventListeners.clear();
            }
            SIPDialog.this.timerTaskLock = null;
            SIPDialog.this.sipStack.removeDialog(sIPDialog);
        }
    }

    class DialogTimerTask extends SIPStackTimerTask implements Serializable {
        int nRetransmissions = 0;
        SIPServerTransaction transaction;

        public DialogTimerTask(SIPServerTransaction sIPServerTransaction) {
            this.transaction = sIPServerTransaction;
        }

        @Override
        protected void runTask() {
            StackLogger stackLogger;
            StringBuilder sb;
            SIPTransactionStack sIPTransactionStack;
            SIPDialog sIPDialog = SIPDialog.this;
            if (SIPDialog.this.sipStack.isLoggingEnabled()) {
                SIPDialog.this.sipStack.getStackLogger().logDebug("Running dialog timer");
            }
            this.nRetransmissions++;
            SIPServerTransaction sIPServerTransaction = this.transaction;
            if (this.nRetransmissions > 64) {
                if (SIPDialog.this.sipProvider.getSipListener() == null || !(SIPDialog.this.sipProvider.getSipListener() instanceof SipListenerExt)) {
                    sIPDialog.delete();
                } else {
                    SIPDialog.this.raiseErrorEvent(1);
                }
                if (sIPServerTransaction != null && sIPServerTransaction.getState() != TransactionState.TERMINATED) {
                    sIPServerTransaction.raiseErrorEvent(1);
                }
            } else if (!sIPDialog.ackSeen && sIPServerTransaction != null) {
                SIPResponse lastResponse = sIPServerTransaction.getLastResponse();
                try {
                    if (lastResponse.getStatusCode() == 200) {
                        try {
                            if (sIPDialog.toRetransmitFinalResponse(sIPServerTransaction.T2)) {
                                sIPServerTransaction.sendMessage(lastResponse);
                            }
                            sIPTransactionStack = sIPDialog.sipStack;
                        } catch (IOException e) {
                            SIPDialog.this.raiseIOException(sIPServerTransaction.getPeerAddress(), sIPServerTransaction.getPeerPort(), sIPServerTransaction.getPeerProtocol());
                            SIPTransactionStack sIPTransactionStack2 = sIPDialog.sipStack;
                            if (sIPTransactionStack2.isLoggingEnabled()) {
                                stackLogger = sIPTransactionStack2.getStackLogger();
                                sb = new StringBuilder();
                            }
                            sIPServerTransaction.fireTimer();
                        }
                        if (sIPTransactionStack.isLoggingEnabled()) {
                            stackLogger = sIPTransactionStack.getStackLogger();
                            sb = new StringBuilder();
                            sb.append("resend 200 response from ");
                            sb.append(sIPDialog);
                            stackLogger.logDebug(sb.toString());
                        }
                        sIPServerTransaction.fireTimer();
                    }
                } catch (Throwable th) {
                    SIPTransactionStack sIPTransactionStack3 = sIPDialog.sipStack;
                    if (sIPTransactionStack3.isLoggingEnabled()) {
                        sIPTransactionStack3.getStackLogger().logDebug("resend 200 response from " + sIPDialog);
                    }
                    sIPServerTransaction.fireTimer();
                    throw th;
                }
            }
            if (sIPDialog.isAckSeen() || sIPDialog.dialogState == SIPDialog.TERMINATED_STATE) {
                this.transaction = null;
                cancel();
            }
        }
    }

    class DialogDeleteTask extends SIPStackTimerTask implements Serializable {
        DialogDeleteTask() {
        }

        @Override
        protected void runTask() {
            SIPDialog.this.delete();
        }
    }

    class DialogDeleteIfNoAckSentTask extends SIPStackTimerTask implements Serializable {
        private long seqno;

        public DialogDeleteIfNoAckSentTask(long j) {
            this.seqno = j;
        }

        @Override
        protected void runTask() {
            if (SIPDialog.this.highestSequenceNumberAcknowledged < this.seqno) {
                SIPDialog.this.dialogDeleteIfNoAckSentTask = null;
                if (!SIPDialog.this.isBackToBackUserAgent) {
                    if (SIPDialog.this.sipStack.isLoggingEnabled()) {
                        SIPDialog.this.sipStack.getStackLogger().logError("ACK Was not sent. killing dialog");
                    }
                    if (SIPDialog.this.sipProvider.getSipListener() instanceof SipListenerExt) {
                        SIPDialog.this.raiseErrorEvent(2);
                        return;
                    } else {
                        SIPDialog.this.delete();
                        return;
                    }
                }
                if (SIPDialog.this.sipStack.isLoggingEnabled()) {
                    SIPDialog.this.sipStack.getStackLogger().logError("ACK Was not sent. Sending BYE");
                }
                if (SIPDialog.this.sipProvider.getSipListener() instanceof SipListenerExt) {
                    SIPDialog.this.raiseErrorEvent(2);
                    return;
                }
                try {
                    Request requestCreateRequest = SIPDialog.this.createRequest("BYE");
                    if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
                        requestCreateRequest.addHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
                    }
                    Reason reason = new Reason();
                    reason.setProtocol("SIP");
                    reason.setCause(1025);
                    reason.setText("Timed out waiting to send ACK");
                    requestCreateRequest.addHeader(reason);
                    SIPDialog.this.sendRequest(SIPDialog.this.getSipProvider().getNewClientTransaction(requestCreateRequest));
                } catch (Exception e) {
                    SIPDialog.this.delete();
                }
            }
        }
    }

    private SIPDialog(SipProviderImpl sipProviderImpl) {
        this.auditTag = 0L;
        this.ackSem = new Semaphore(1);
        this.reInviteWaitTime = 100;
        this.highestSequenceNumberAcknowledged = -1L;
        this.sequenceNumberValidation = true;
        this.timerTaskLock = new Semaphore(1);
        this.firstTransactionPort = 5060;
        this.terminateOnBye = true;
        this.routeList = new RouteList();
        this.dialogState = -1;
        this.localSequenceNumber = 0L;
        this.remoteSequenceNumber = -1L;
        this.sipProvider = sipProviderImpl;
        this.eventListeners = new CopyOnWriteArraySet();
    }

    private void recordStackTrace() {
        StringWriter stringWriter = new StringWriter();
        new Exception().printStackTrace(new PrintWriter(stringWriter));
        this.stackTrace = stringWriter.getBuffer().toString();
    }

    public SIPDialog(SIPTransaction sIPTransaction) {
        this(sIPTransaction.getSipProvider());
        SIPRequest sIPRequest = (SIPRequest) sIPTransaction.getRequest();
        this.callIdHeader = sIPRequest.getCallId();
        this.earlyDialogId = sIPRequest.getDialogId(false);
        if (sIPTransaction == null) {
            throw new NullPointerException("Null tx");
        }
        this.sipStack = sIPTransaction.sipStack;
        this.sipProvider = sIPTransaction.getSipProvider();
        if (this.sipProvider == null) {
            throw new NullPointerException("Null Provider!");
        }
        addTransaction(sIPTransaction);
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Creating a dialog : " + this);
            this.sipStack.getStackLogger().logDebug("provider port = " + this.sipProvider.getListeningPoint().getPort());
            this.sipStack.getStackLogger().logStackTrace();
        }
        this.isBackToBackUserAgent = this.sipStack.isBackToBackUserAgent;
        addEventListener(this.sipStack);
    }

    public SIPDialog(SIPClientTransaction sIPClientTransaction, SIPResponse sIPResponse) {
        this(sIPClientTransaction);
        if (sIPResponse == null) {
            throw new NullPointerException("Null SipResponse");
        }
        setLastResponse(sIPClientTransaction, sIPResponse);
        this.isBackToBackUserAgent = this.sipStack.isBackToBackUserAgent;
    }

    public SIPDialog(SipProviderImpl sipProviderImpl, SIPResponse sIPResponse) {
        this(sipProviderImpl);
        this.sipStack = (SIPTransactionStack) sipProviderImpl.getSipStack();
        setLastResponse(null, sIPResponse);
        this.localSequenceNumber = sIPResponse.getCSeq().getSeqNumber();
        this.originalLocalSequenceNumber = this.localSequenceNumber;
        this.myTag = sIPResponse.getFrom().getTag();
        this.hisTag = sIPResponse.getTo().getTag();
        this.localParty = sIPResponse.getFrom().getAddress();
        this.remoteParty = sIPResponse.getTo().getAddress();
        this.method = sIPResponse.getCSeq().getMethod();
        this.callIdHeader = sIPResponse.getCallId();
        this.serverTransactionFlag = false;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Creating a dialog : " + this);
            this.sipStack.getStackLogger().logStackTrace();
        }
        this.isBackToBackUserAgent = this.sipStack.isBackToBackUserAgent;
        addEventListener(this.sipStack);
    }

    private void printRouteList() {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("this : " + this);
            this.sipStack.getStackLogger().logDebug("printRouteList : " + this.routeList.encode());
        }
    }

    private boolean isClientDialog() {
        return ((SIPTransaction) getFirstTransaction()) instanceof SIPClientTransaction;
    }

    private void raiseIOException(String str, int i, String str2) {
        this.sipProvider.handleEvent(new IOExceptionEvent(this, str, i, str2), null);
        setState(TERMINATED_STATE);
    }

    private void raiseErrorEvent(int i) {
        SIPDialogErrorEvent sIPDialogErrorEvent = new SIPDialogErrorEvent(this, i);
        synchronized (this.eventListeners) {
            Iterator<SIPDialogEventListener> it = this.eventListeners.iterator();
            while (it.hasNext()) {
                it.next().dialogErrorEvent(sIPDialogErrorEvent);
            }
        }
        this.eventListeners.clear();
        if (i != 2 && i != 1 && i != 3) {
            delete();
        }
        stopTimer();
    }

    private void setRemoteParty(SIPMessage sIPMessage) {
        if (!isServer()) {
            this.remoteParty = sIPMessage.getTo().getAddress();
        } else {
            this.remoteParty = sIPMessage.getFrom().getAddress();
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("settingRemoteParty " + this.remoteParty);
        }
    }

    private void addRoute(RecordRouteList recordRouteList) {
        try {
            if (isClientDialog()) {
                this.routeList = new RouteList();
                ListIterator<RecordRoute> listIterator = recordRouteList.listIterator(recordRouteList.size());
                while (listIterator.hasPrevious()) {
                    RecordRoute recordRoutePrevious = listIterator.previous();
                    Route route = new Route();
                    route.setAddress((AddressImpl) ((AddressImpl) recordRoutePrevious.getAddress()).clone());
                    route.setParameters((NameValueList) recordRoutePrevious.getParameters().clone());
                    this.routeList.add(route);
                }
            } else {
                this.routeList = new RouteList();
                ListIterator<RecordRoute> listIterator2 = recordRouteList.listIterator();
                while (listIterator2.hasNext()) {
                    RecordRoute next = listIterator2.next();
                    Route route2 = new Route();
                    route2.setAddress((AddressImpl) ((AddressImpl) next.getAddress()).clone());
                    route2.setParameters((NameValueList) next.getParameters().clone());
                    this.routeList.add(route2);
                }
            }
        } finally {
            if (this.sipStack.getStackLogger().isLoggingEnabled()) {
                Iterator<Route> it = this.routeList.iterator();
                while (it.hasNext()) {
                    if (!((SipURI) it.next().getAddress().getURI()).hasLrParam() && this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logWarning("NON LR route in Route set detected for dialog : " + this);
                        this.sipStack.getStackLogger().logStackTrace();
                    }
                }
            }
        }
    }

    void setRemoteTarget(ContactHeader contactHeader) {
        this.remoteTarget = contactHeader.getAddress();
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Dialog.setRemoteTarget: " + this.remoteTarget);
            this.sipStack.getStackLogger().logStackTrace();
        }
    }

    private synchronized void addRoute(SIPResponse sIPResponse) {
        ContactList contactHeaders;
        try {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("setContact: dialogState: " + this + "state = " + getState());
            }
            if (sIPResponse.getStatusCode() == 100) {
                return;
            }
            if (this.dialogState == TERMINATED_STATE) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logStackTrace();
                }
                return;
            }
            if (this.dialogState == CONFIRMED_STATE) {
                if (sIPResponse.getStatusCode() / 100 == 2 && !isServer() && (contactHeaders = sIPResponse.getContactHeaders()) != null && SIPRequest.isTargetRefresh(sIPResponse.getCSeq().getMethod())) {
                    setRemoteTarget((ContactHeader) contactHeaders.getFirst());
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logStackTrace();
                }
                return;
            }
            if (!isServer()) {
                if (getState() != DialogState.CONFIRMED && getState() != DialogState.TERMINATED) {
                    RecordRouteList recordRouteHeaders = sIPResponse.getRecordRouteHeaders();
                    if (recordRouteHeaders != null) {
                        addRoute(recordRouteHeaders);
                    } else {
                        this.routeList = new RouteList();
                    }
                }
                ContactList contactHeaders2 = sIPResponse.getContactHeaders();
                if (contactHeaders2 != null) {
                    setRemoteTarget((ContactHeader) contactHeaders2.getFirst());
                }
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logStackTrace();
            }
        } finally {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logStackTrace();
            }
        }
    }

    private synchronized RouteList getRouteList() {
        RouteList routeList;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("getRouteList " + this);
        }
        new RouteList();
        routeList = new RouteList();
        if (this.routeList != null) {
            ListIterator<Route> listIterator = this.routeList.listIterator();
            while (listIterator.hasNext()) {
                routeList.add((Route) listIterator.next().clone());
            }
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("----- ");
            this.sipStack.getStackLogger().logDebug("getRouteList for " + this);
            this.sipStack.getStackLogger().logDebug("RouteList = " + routeList.encode());
            if (this.routeList != null) {
                this.sipStack.getStackLogger().logDebug("myRouteList = " + this.routeList.encode());
            }
            this.sipStack.getStackLogger().logDebug("----- ");
        }
        return routeList;
    }

    void setRouteList(RouteList routeList) {
        this.routeList = routeList;
    }

    private void sendAck(Request request, boolean z) throws SipException {
        ListeningPointImpl listeningPointImpl;
        SIPRequest sIPRequest = (SIPRequest) request;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("sendAck" + this);
        }
        if (!sIPRequest.getMethod().equals("ACK")) {
            throw new SipException("Bad request method -- should be ACK");
        }
        if (getState() == null || getState().getValue() == EARLY_STATE) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Bad Dialog State for " + this + " dialogID = " + getDialogId());
            }
            throw new SipException("Bad dialog state " + getState());
        }
        if (!getCallId().getCallId().equals(sIPRequest.getCallId().getCallId())) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("CallID " + getCallId());
                this.sipStack.getStackLogger().logError("RequestCallID = " + sIPRequest.getCallId().getCallId());
                this.sipStack.getStackLogger().logError("dialog =  " + this);
            }
            throw new SipException("Bad call ID in request");
        }
        try {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("setting from tag For outgoing ACK= " + getLocalTag());
                this.sipStack.getStackLogger().logDebug("setting To tag for outgoing ACK = " + getRemoteTag());
                this.sipStack.getStackLogger().logDebug("ack = " + sIPRequest);
            }
            if (getLocalTag() != null) {
                sIPRequest.getFrom().setTag(getLocalTag());
            }
            if (getRemoteTag() != null) {
                sIPRequest.getTo().setTag(getRemoteTag());
            }
            Hop nextHop = this.sipStack.getNextHop(sIPRequest);
            if (nextHop == null) {
                throw new SipException("No route!");
            }
            try {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("hop = " + nextHop);
                }
                listeningPointImpl = (ListeningPointImpl) this.sipProvider.getListeningPoint(nextHop.getTransport());
            } catch (IOException e) {
                if (z) {
                    throw new SipException("Could not send ack", e);
                }
                raiseIOException(nextHop.getHost(), nextHop.getPort(), nextHop.getTransport());
            } catch (SipException e2) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logException(e2);
                }
                throw e2;
            } catch (Exception e3) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logException(e3);
                }
                throw new SipException("Could not create message channel", e3);
            }
            if (listeningPointImpl == null) {
                throw new SipException("No listening point for this provider registered at " + nextHop);
            }
            MessageChannel messageChannelCreateMessageChannel = listeningPointImpl.getMessageProcessor().createMessageChannel(InetAddress.getByName(nextHop.getHost()), nextHop.getPort());
            boolean z2 = false;
            if (!isAckSent(((SIPRequest) request).getCSeq().getSeqNumber())) {
                z2 = true;
            }
            setLastAckSent(sIPRequest);
            messageChannelCreateMessageChannel.sendMessage(sIPRequest);
            this.isAcknowledged = true;
            this.highestSequenceNumberAcknowledged = Math.max(this.highestSequenceNumberAcknowledged, sIPRequest.getCSeq().getSeqNumber());
            if (z2 && this.isBackToBackUserAgent) {
                releaseAckSem();
            } else if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Not releasing ack sem for " + this + " isAckSent " + z2);
            }
            if (this.dialogDeleteTask != null) {
                this.dialogDeleteTask.cancel();
                this.dialogDeleteTask = null;
            }
            this.ackSeen = true;
        } catch (ParseException e4) {
            throw new SipException(e4.getMessage());
        }
    }

    void setStack(SIPTransactionStack sIPTransactionStack) {
        this.sipStack = sIPTransactionStack;
    }

    SIPTransactionStack getStack() {
        return this.sipStack;
    }

    boolean isTerminatedOnBye() {
        return this.terminateOnBye;
    }

    void ackReceived(SIPRequest sIPRequest) {
        SIPServerTransaction inviteTransaction;
        if (!this.ackSeen && (inviteTransaction = getInviteTransaction()) != null && inviteTransaction.getCSeq() == sIPRequest.getCSeq().getSeqNumber()) {
            acquireTimerTaskSem();
            try {
                if (this.timerTask != null) {
                    this.timerTask.cancel();
                    this.timerTask = null;
                }
                releaseTimerTaskSem();
                this.ackSeen = true;
                if (this.dialogDeleteTask != null) {
                    this.dialogDeleteTask.cancel();
                    this.dialogDeleteTask = null;
                }
                setLastAckReceived(sIPRequest);
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("ackReceived for " + inviteTransaction.getMethod());
                    this.ackLine = this.sipStack.getStackLogger().getLineCount();
                    printDebugInfo();
                }
                if (this.isBackToBackUserAgent) {
                    releaseAckSem();
                }
                setState(CONFIRMED_STATE);
            } catch (Throwable th) {
                releaseTimerTaskSem();
                throw th;
            }
        }
    }

    synchronized boolean testAndSetIsDialogTerminatedEventDelivered() {
        boolean z;
        z = this.dialogTerminatedEventDelivered;
        this.dialogTerminatedEventDelivered = true;
        return z;
    }

    public void addEventListener(SIPDialogEventListener sIPDialogEventListener) {
        this.eventListeners.add(sIPDialogEventListener);
    }

    public void removeEventListener(SIPDialogEventListener sIPDialogEventListener) {
        this.eventListeners.remove(sIPDialogEventListener);
    }

    @Override
    public void setApplicationData(Object obj) {
        this.applicationData = obj;
    }

    @Override
    public Object getApplicationData() {
        return this.applicationData;
    }

    public synchronized void requestConsumed() {
        this.nextSeqno = Long.valueOf(getRemoteSeqNumber() + 1);
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Request Consumed -- next consumable Request Seqno = " + this.nextSeqno);
        }
    }

    public synchronized boolean isRequestConsumable(SIPRequest sIPRequest) {
        if (sIPRequest.getMethod().equals("ACK")) {
            throw new RuntimeException("Illegal method");
        }
        if (isSequnceNumberValidation()) {
            return this.remoteSequenceNumber < sIPRequest.getCSeq().getSeqNumber();
        }
        return true;
    }

    public void doDeferredDelete() {
        if (this.sipStack.getTimer() == null) {
            setState(TERMINATED_STATE);
        } else {
            this.dialogDeleteTask = new DialogDeleteTask();
            this.sipStack.getTimer().schedule(this.dialogDeleteTask, 32000L);
        }
    }

    public void setState(int i) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Setting dialog state for " + this + "newState = " + i);
            this.sipStack.getStackLogger().logStackTrace();
            if (i != -1 && i != this.dialogState && this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug(this + "  old dialog state is " + getState());
                this.sipStack.getStackLogger().logDebug(this + "  New dialog state is " + DialogState.getObject(i));
            }
        }
        this.dialogState = i;
        if (i == TERMINATED_STATE) {
            if (this.sipStack.getTimer() != null) {
                this.sipStack.getTimer().schedule(new LingerTimer(), 8000L);
            }
            stopTimer();
        }
    }

    public void printDebugInfo() {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("isServer = " + isServer());
            this.sipStack.getStackLogger().logDebug("localTag = " + getLocalTag());
            this.sipStack.getStackLogger().logDebug("remoteTag = " + getRemoteTag());
            this.sipStack.getStackLogger().logDebug("localSequenceNumer = " + getLocalSeqNumber());
            this.sipStack.getStackLogger().logDebug("remoteSequenceNumer = " + getRemoteSeqNumber());
            this.sipStack.getStackLogger().logDebug("ackLine:" + getRemoteTag() + Separators.SP + this.ackLine);
        }
    }

    public boolean isAckSeen() {
        return this.ackSeen;
    }

    public SIPRequest getLastAckSent() {
        return this.lastAckSent;
    }

    public boolean isAckSent(long j) {
        if (getLastTransaction() != null && (getLastTransaction() instanceof ClientTransaction)) {
            return getLastAckSent() != null && j <= getLastAckSent().getCSeq().getSeqNumber();
        }
        return true;
    }

    @Override
    public Transaction getFirstTransaction() {
        return this.firstTransaction;
    }

    @Override
    public Iterator getRouteSet() {
        if (this.routeList == null) {
            return new LinkedList().listIterator();
        }
        return getRouteList().listIterator();
    }

    public synchronized void addRoute(SIPRequest sIPRequest) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("setContact: dialogState: " + this + "state = " + getState());
        }
        if (this.dialogState == CONFIRMED_STATE && SIPRequest.isTargetRefresh(sIPRequest.getMethod())) {
            doTargetRefresh(sIPRequest);
        }
        if (this.dialogState != CONFIRMED_STATE && this.dialogState != TERMINATED_STATE) {
            if (sIPRequest.getToTag() != null) {
                return;
            }
            RecordRouteList recordRouteHeaders = sIPRequest.getRecordRouteHeaders();
            if (recordRouteHeaders != null) {
                addRoute(recordRouteHeaders);
            } else {
                this.routeList = new RouteList();
            }
            ContactList contactHeaders = sIPRequest.getContactHeaders();
            if (contactHeaders != null) {
                setRemoteTarget((ContactHeader) contactHeaders.getFirst());
            }
        }
    }

    public void setDialogId(String str) {
        this.dialogId = str;
    }

    public static SIPDialog createFromNOTIFY(SIPClientTransaction sIPClientTransaction, SIPTransaction sIPTransaction) {
        SIPDialog sIPDialog = new SIPDialog(sIPTransaction);
        sIPDialog.serverTransactionFlag = false;
        sIPDialog.lastTransaction = sIPClientTransaction;
        storeFirstTransactionInfo(sIPDialog, sIPClientTransaction);
        sIPDialog.terminateOnBye = false;
        sIPDialog.localSequenceNumber = sIPClientTransaction.getCSeq();
        SIPRequest sIPRequest = (SIPRequest) sIPTransaction.getRequest();
        sIPDialog.remoteSequenceNumber = sIPRequest.getCSeq().getSeqNumber();
        sIPDialog.setDialogId(sIPRequest.getDialogId(true));
        sIPDialog.setLocalTag(sIPRequest.getToTag());
        sIPDialog.setRemoteTag(sIPRequest.getFromTag());
        sIPDialog.setLastResponse(sIPClientTransaction, sIPClientTransaction.getLastResponse());
        sIPDialog.localParty = sIPRequest.getTo().getAddress();
        sIPDialog.remoteParty = sIPRequest.getFrom().getAddress();
        sIPDialog.addRoute(sIPRequest);
        sIPDialog.setState(CONFIRMED_STATE);
        return sIPDialog;
    }

    @Override
    public boolean isServer() {
        if (!this.firstTransactionSeen) {
            return this.serverTransactionFlag;
        }
        return this.firstTransactionIsServerTransaction;
    }

    protected boolean isReInvite() {
        return this.reInviteFlag;
    }

    @Override
    public String getDialogId() {
        if (this.dialogId == null && this.lastResponse != null) {
            this.dialogId = this.lastResponse.getDialogId(isServer());
        }
        return this.dialogId;
    }

    private static void storeFirstTransactionInfo(SIPDialog sIPDialog, SIPTransaction sIPTransaction) {
        sIPDialog.firstTransaction = sIPTransaction;
        sIPDialog.firstTransactionSeen = true;
        sIPDialog.firstTransactionIsServerTransaction = sIPTransaction.isServerTransaction();
        sIPDialog.firstTransactionSecure = sIPTransaction.getRequest().getRequestURI().getScheme().equalsIgnoreCase("sips");
        sIPDialog.firstTransactionPort = sIPTransaction.getPort();
        sIPDialog.firstTransactionId = sIPTransaction.getBranchId();
        sIPDialog.firstTransactionMethod = sIPTransaction.getMethod();
        if (sIPDialog.isServer()) {
            SIPResponse lastResponse = ((SIPServerTransaction) sIPTransaction).getLastResponse();
            sIPDialog.contactHeader = lastResponse != null ? lastResponse.getContactHeader() : null;
        } else {
            SIPClientTransaction sIPClientTransaction = (SIPClientTransaction) sIPTransaction;
            if (sIPClientTransaction != null) {
                sIPDialog.contactHeader = sIPClientTransaction.getOriginalRequest().getContactHeader();
            }
        }
    }

    public void addTransaction(SIPTransaction sIPTransaction) {
        SIPRequest originalRequest = sIPTransaction.getOriginalRequest();
        if (this.firstTransactionSeen && !this.firstTransactionId.equals(sIPTransaction.getBranchId()) && sIPTransaction.getMethod().equals(this.firstTransactionMethod)) {
            this.reInviteFlag = true;
        }
        if (!this.firstTransactionSeen) {
            storeFirstTransactionInfo(this, sIPTransaction);
            if (originalRequest.getMethod().equals("SUBSCRIBE")) {
                this.eventHeader = (EventHeader) originalRequest.getHeader("Event");
            }
            setLocalParty(originalRequest);
            setRemoteParty(originalRequest);
            setCallId(originalRequest);
            if (this.originalRequest == null) {
                this.originalRequest = originalRequest;
            }
            if (this.method == null) {
                this.method = originalRequest.getMethod();
            }
            if (sIPTransaction instanceof SIPServerTransaction) {
                this.hisTag = originalRequest.getFrom().getTag();
            } else {
                setLocalSequenceNumber(originalRequest.getCSeq().getSeqNumber());
                this.originalLocalSequenceNumber = this.localSequenceNumber;
                this.myTag = originalRequest.getFrom().getTag();
                if (this.myTag == null && this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logError("The request's From header is missing the required Tag parameter.");
                }
            }
        } else if (sIPTransaction.getMethod().equals(this.firstTransactionMethod) && this.firstTransactionIsServerTransaction != sIPTransaction.isServerTransaction()) {
            storeFirstTransactionInfo(this, sIPTransaction);
            setLocalParty(originalRequest);
            setRemoteParty(originalRequest);
            setCallId(originalRequest);
            this.originalRequest = originalRequest;
            this.method = originalRequest.getMethod();
        }
        if (sIPTransaction instanceof SIPServerTransaction) {
            setRemoteSequenceNumber(originalRequest.getCSeq().getSeqNumber());
        }
        this.lastTransaction = sIPTransaction;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Transaction Added " + this + this.myTag + Separators.SLASH + this.hisTag);
            this.sipStack.getStackLogger().logDebug("TID = " + sIPTransaction.getTransactionId() + Separators.SLASH + sIPTransaction.isServerTransaction());
            this.sipStack.getStackLogger().logStackTrace();
        }
    }

    private void setRemoteTag(String str) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("setRemoteTag(): " + this + " remoteTag = " + this.hisTag + " new tag = " + str);
        }
        if (this.hisTag != null && str != null && !str.equals(this.hisTag)) {
            if (getState() != DialogState.EARLY) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Dialog is already established -- ignoring remote tag re-assignment");
                    return;
                }
                return;
            }
            if (this.sipStack.isRemoteTagReassignmentAllowed()) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("UNSAFE OPERATION !  tag re-assignment " + this.hisTag + " trying to set to " + str + " can cause unexpected effects ");
                }
                boolean z = false;
                if (this.sipStack.getDialog(this.dialogId) == this) {
                    this.sipStack.removeDialog(this.dialogId);
                    z = true;
                }
                this.dialogId = null;
                this.hisTag = str;
                if (z) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("ReInserting Dialog");
                    }
                    this.sipStack.putDialog(this);
                    return;
                }
                return;
            }
            return;
        }
        if (str != null) {
            this.hisTag = str;
        } else if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logWarning("setRemoteTag : called with null argument ");
        }
    }

    public SIPTransaction getLastTransaction() {
        return this.lastTransaction;
    }

    public SIPServerTransaction getInviteTransaction() {
        DialogTimerTask dialogTimerTask = this.timerTask;
        if (dialogTimerTask != null) {
            return dialogTimerTask.transaction;
        }
        return null;
    }

    private void setLocalSequenceNumber(long j) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("setLocalSequenceNumber: original  " + this.localSequenceNumber + " new  = " + j);
        }
        if (j <= this.localSequenceNumber) {
            throw new RuntimeException("Sequence number should not decrease !");
        }
        this.localSequenceNumber = j;
    }

    public void setRemoteSequenceNumber(long j) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("setRemoteSeqno " + this + Separators.SLASH + j);
        }
        this.remoteSequenceNumber = j;
    }

    @Override
    public void incrementLocalSequenceNumber() {
        this.localSequenceNumber++;
    }

    @Override
    public int getRemoteSequenceNumber() {
        return (int) this.remoteSequenceNumber;
    }

    @Override
    public int getLocalSequenceNumber() {
        return (int) this.localSequenceNumber;
    }

    public long getOriginalLocalSequenceNumber() {
        return this.originalLocalSequenceNumber;
    }

    @Override
    public long getLocalSeqNumber() {
        return this.localSequenceNumber;
    }

    @Override
    public long getRemoteSeqNumber() {
        return this.remoteSequenceNumber;
    }

    @Override
    public String getLocalTag() {
        return this.myTag;
    }

    @Override
    public String getRemoteTag() {
        return this.hisTag;
    }

    private void setLocalTag(String str) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("set Local tag " + str + Separators.SP + this.dialogId);
            this.sipStack.getStackLogger().logStackTrace();
        }
        this.myTag = str;
    }

    @Override
    public void delete() {
        setState(TERMINATED_STATE);
    }

    @Override
    public CallIdHeader getCallId() {
        return this.callIdHeader;
    }

    private void setCallId(SIPRequest sIPRequest) {
        this.callIdHeader = sIPRequest.getCallId();
    }

    @Override
    public Address getLocalParty() {
        return this.localParty;
    }

    private void setLocalParty(SIPMessage sIPMessage) {
        if (!isServer()) {
            this.localParty = sIPMessage.getFrom().getAddress();
        } else {
            this.localParty = sIPMessage.getTo().getAddress();
        }
    }

    @Override
    public Address getRemoteParty() {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("gettingRemoteParty " + this.remoteParty);
        }
        return this.remoteParty;
    }

    @Override
    public Address getRemoteTarget() {
        return this.remoteTarget;
    }

    @Override
    public DialogState getState() {
        if (this.dialogState == -1) {
            return null;
        }
        return DialogState.getObject(this.dialogState);
    }

    @Override
    public boolean isSecure() {
        return this.firstTransactionSecure;
    }

    @Override
    public void sendAck(Request request) throws SipException {
        sendAck(request, true);
    }

    @Override
    public Request createRequest(String str) throws SipException {
        if (str.equals("ACK") || str.equals(Request.PRACK)) {
            throw new SipException("Invalid method specified for createRequest:" + str);
        }
        if (this.lastResponse != null) {
            return createRequest(str, this.lastResponse);
        }
        throw new SipException("Dialog not yet established -- no response!");
    }

    private Request createRequest(String str, SIPResponse sIPResponse) throws SipException {
        SipUri sipUri;
        if (str == null || sIPResponse == null) {
            throw new NullPointerException("null argument");
        }
        if (str.equals(Request.CANCEL)) {
            throw new SipException("Dialog.createRequest(): Invalid request");
        }
        if (getState() == null || ((getState().getValue() == TERMINATED_STATE && !str.equalsIgnoreCase("BYE")) || (isServer() && getState().getValue() == EARLY_STATE && str.equalsIgnoreCase("BYE")))) {
            throw new SipException("Dialog  " + getDialogId() + " not yet established or terminated " + getState());
        }
        if (getRemoteTarget() != null) {
            sipUri = (SipUri) getRemoteTarget().getURI().clone();
        } else {
            sipUri = (SipUri) getRemoteParty().getURI().clone();
            sipUri.clearUriParms();
        }
        SipUri sipUri2 = sipUri;
        CSeq cSeq = new CSeq();
        try {
            cSeq.setMethod(str);
            cSeq.setSeqNumber(getLocalSeqNumber());
        } catch (Exception e) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Unexpected error");
            }
            InternalErrorHandler.handleException(e);
        }
        ListeningPointImpl listeningPointImpl = (ListeningPointImpl) this.sipProvider.getListeningPoint(sIPResponse.getTopmostVia().getTransport());
        if (listeningPointImpl == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("Cannot find listening point for transport " + sIPResponse.getTopmostVia().getTransport());
            }
            throw new SipException("Cannot find listening point for transport " + sIPResponse.getTopmostVia().getTransport());
        }
        Via viaHeader = listeningPointImpl.getViaHeader();
        From from = new From();
        from.setAddress(this.localParty);
        To to = new To();
        to.setAddress(this.remoteParty);
        SIPRequest sIPRequestCreateRequest = sIPResponse.createRequest(sipUri2, viaHeader, cSeq, from, to);
        if (SIPRequest.isTargetRefresh(str)) {
            ContactHeader contactHeaderCreateContactHeader = ((ListeningPointImpl) this.sipProvider.getListeningPoint(listeningPointImpl.getTransport())).createContactHeader();
            ((SipURI) contactHeaderCreateContactHeader.getAddress().getURI()).setSecure(isSecure());
            sIPRequestCreateRequest.setHeader(contactHeaderCreateContactHeader);
        }
        try {
            ((CSeq) sIPRequestCreateRequest.getCSeq()).setSeqNumber(this.localSequenceNumber + 1);
        } catch (InvalidArgumentException e2) {
            InternalErrorHandler.handleException(e2);
        }
        if (str.equals("SUBSCRIBE") && this.eventHeader != null) {
            sIPRequestCreateRequest.addHeader(this.eventHeader);
        }
        try {
            if (getLocalTag() != null) {
                from.setTag(getLocalTag());
            } else {
                from.removeTag();
            }
            if (getRemoteTag() != null) {
                to.setTag(getRemoteTag());
            } else {
                to.removeTag();
            }
        } catch (ParseException e3) {
            InternalErrorHandler.handleException(e3);
        }
        updateRequest(sIPRequestCreateRequest);
        return sIPRequestCreateRequest;
    }

    @Override
    public void sendRequest(ClientTransaction clientTransaction) throws SipException {
        sendRequest(clientTransaction, !this.isBackToBackUserAgent);
    }

    public void sendRequest(ClientTransaction clientTransaction, boolean z) throws SipException {
        if (!z && clientTransaction.getRequest().getMethod().equals("INVITE")) {
            new Thread(new ReInviteSender(clientTransaction)).start();
            return;
        }
        SIPClientTransaction sIPClientTransaction = (SIPClientTransaction) clientTransaction;
        SIPRequest originalRequest = sIPClientTransaction.getOriginalRequest();
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("dialog.sendRequest  dialog = " + this + "\ndialogRequest = \n" + originalRequest);
        }
        if (clientTransaction == 0) {
            throw new NullPointerException("null parameter");
        }
        if (originalRequest.getMethod().equals("ACK") || originalRequest.getMethod().equals(Request.CANCEL)) {
            throw new SipException("Bad Request Method. " + originalRequest.getMethod());
        }
        if (this.byeSent && isTerminatedOnBye() && !originalRequest.getMethod().equals("BYE")) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("BYE already sent for " + this);
            }
            throw new SipException("Cannot send request; BYE already sent");
        }
        if (originalRequest.getTopmostVia() == null) {
            originalRequest.addHeader(sIPClientTransaction.getOutgoingViaHeader());
        }
        if (!getCallId().getCallId().equalsIgnoreCase(originalRequest.getCallId().getCallId())) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("CallID " + getCallId());
                this.sipStack.getStackLogger().logError("RequestCallID = " + originalRequest.getCallId().getCallId());
                this.sipStack.getStackLogger().logError("dialog =  " + this);
            }
            throw new SipException("Bad call ID in request");
        }
        sIPClientTransaction.setDialog(this, this.dialogId);
        addTransaction((SIPTransaction) clientTransaction);
        sIPClientTransaction.isMapped = true;
        From from = (From) originalRequest.getFrom();
        To to = (To) originalRequest.getTo();
        if (getLocalTag() != null && from.getTag() != null && !from.getTag().equals(getLocalTag())) {
            throw new SipException("From tag mismatch expecting  " + getLocalTag());
        }
        if (getRemoteTag() != null && to.getTag() != null && !to.getTag().equals(getRemoteTag()) && this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logWarning("To header tag mismatch expecting " + getRemoteTag());
        }
        if (getLocalTag() == null && originalRequest.getMethod().equals("NOTIFY")) {
            if (!getMethod().equals("SUBSCRIBE")) {
                throw new SipException("Trying to send NOTIFY without SUBSCRIBE Dialog!");
            }
            setLocalTag(from.getTag());
        }
        try {
            if (getLocalTag() != null) {
                from.setTag(getLocalTag());
            }
            if (getRemoteTag() != null) {
                to.setTag(getRemoteTag());
            }
        } catch (ParseException e) {
            InternalErrorHandler.handleException(e);
        }
        Hop nextHop = sIPClientTransaction.getNextHop();
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Using hop = " + nextHop.getHost() + " : " + nextHop.getPort());
        }
        try {
            MessageChannel messageChannelCreateRawMessageChannel = this.sipStack.createRawMessageChannel(getSipProvider().getListeningPoint(nextHop.getTransport()).getIPAddress(), this.firstTransactionPort, nextHop);
            MessageChannel messageChannel = ((SIPClientTransaction) clientTransaction).getMessageChannel();
            messageChannel.uncache();
            if (!this.sipStack.cacheClientConnections) {
                messageChannel.useCount--;
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("oldChannel: useCount " + messageChannel.useCount);
                }
            }
            if (messageChannelCreateRawMessageChannel == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Null message channel using outbound proxy !");
                }
                Hop outboundProxy = this.sipStack.getRouter(originalRequest).getOutboundProxy();
                if (outboundProxy == null) {
                    throw new SipException("No route found! hop=" + nextHop);
                }
                messageChannelCreateRawMessageChannel = this.sipStack.createRawMessageChannel(getSipProvider().getListeningPoint(outboundProxy.getTransport()).getIPAddress(), this.firstTransactionPort, outboundProxy);
                if (messageChannelCreateRawMessageChannel != null) {
                    ((SIPClientTransaction) clientTransaction).setEncapsulatedChannel(messageChannelCreateRawMessageChannel);
                }
            } else {
                ((SIPClientTransaction) clientTransaction).setEncapsulatedChannel(messageChannelCreateRawMessageChannel);
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("using message channel " + messageChannelCreateRawMessageChannel);
                }
            }
            if (messageChannelCreateRawMessageChannel != null) {
                messageChannelCreateRawMessageChannel.useCount++;
            }
            if (!this.sipStack.cacheClientConnections && messageChannel != null && messageChannel.useCount <= 0) {
                messageChannel.close();
            }
            try {
                this.localSequenceNumber++;
                originalRequest.getCSeq().setSeqNumber(getLocalSeqNumber());
            } catch (InvalidArgumentException e2) {
                this.sipStack.getStackLogger().logFatalError(e2.getMessage());
            }
            try {
                ((SIPClientTransaction) clientTransaction).sendMessage(originalRequest);
                if (originalRequest.getMethod().equals("BYE")) {
                    this.byeSent = true;
                    if (isTerminatedOnBye()) {
                        setState(DialogState._TERMINATED);
                    }
                }
            } catch (IOException e3) {
                throw new SipException("error sending message", e3);
            }
        } catch (Exception e4) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logException(e4);
            }
            throw new SipException("Could not create message channel", e4);
        }
    }

    private boolean toRetransmitFinalResponse(int i) {
        int i2 = this.retransmissionTicksLeft - 1;
        this.retransmissionTicksLeft = i2;
        if (i2 == 0) {
            if (this.prevRetransmissionTicks * 2 <= i) {
                this.retransmissionTicksLeft = 2 * this.prevRetransmissionTicks;
            } else {
                this.retransmissionTicksLeft = this.prevRetransmissionTicks;
            }
            this.prevRetransmissionTicks = this.retransmissionTicksLeft;
            return true;
        }
        return false;
    }

    protected void setRetransmissionTicks() {
        this.retransmissionTicksLeft = 1;
        this.prevRetransmissionTicks = 1;
    }

    public void resendAck() throws SipException {
        if (getLastAckSent() != null) {
            if (getLastAckSent().getHeader("Timestamp") != null && this.sipStack.generateTimeStampHeader) {
                TimeStamp timeStamp = new TimeStamp();
                try {
                    timeStamp.setTimeStamp(System.currentTimeMillis());
                    getLastAckSent().setHeader(timeStamp);
                } catch (InvalidArgumentException e) {
                }
            }
            sendAck(getLastAckSent(), false);
        }
    }

    public String getMethod() {
        return this.method;
    }

    protected void startTimer(SIPServerTransaction sIPServerTransaction) {
        if (this.timerTask != null && this.timerTask.transaction == sIPServerTransaction) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Timer already running for " + getDialogId());
                return;
            }
            return;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Starting dialog timer for " + getDialogId());
        }
        this.ackSeen = false;
        acquireTimerTaskSem();
        try {
            if (this.timerTask != null) {
                this.timerTask.transaction = sIPServerTransaction;
            } else {
                this.timerTask = new DialogTimerTask(sIPServerTransaction);
                this.sipStack.getTimer().schedule(this.timerTask, 500L, 500L);
            }
            releaseTimerTaskSem();
            setRetransmissionTicks();
        } catch (Throwable th) {
            releaseTimerTaskSem();
            throw th;
        }
    }

    protected void stopTimer() {
        try {
            acquireTimerTaskSem();
            try {
                if (this.timerTask != null) {
                    this.timerTask.cancel();
                    this.timerTask = null;
                }
                releaseTimerTaskSem();
            } catch (Throwable th) {
                releaseTimerTaskSem();
                throw th;
            }
        } catch (Exception e) {
        }
    }

    @Override
    public Request createPrack(Response response) throws SipException {
        if (getState() == null || getState().equals(DialogState.TERMINATED)) {
            throw new DialogDoesNotExistException("Dialog not initialized or terminated");
        }
        if (((RSeq) response.getHeader("RSeq")) == null) {
            throw new SipException("Missing RSeq Header");
        }
        try {
            SIPResponse sIPResponse = (SIPResponse) response;
            SIPRequest sIPRequest = (SIPRequest) createRequest(Request.PRACK, (SIPResponse) response);
            sIPRequest.setToTag(sIPResponse.getTo().getTag());
            RAck rAck = new RAck();
            RSeq rSeq = (RSeq) response.getHeader("RSeq");
            rAck.setMethod(sIPResponse.getCSeq().getMethod());
            rAck.setCSequenceNumber((int) sIPResponse.getCSeq().getSeqNumber());
            rAck.setRSequenceNumber(rSeq.getSeqNumber());
            sIPRequest.setHeader(rAck);
            return sIPRequest;
        } catch (Exception e) {
            InternalErrorHandler.handleException(e);
            return null;
        }
    }

    private void updateRequest(SIPRequest sIPRequest) {
        RouteList routeList = getRouteList();
        if (routeList.size() > 0) {
            sIPRequest.setHeader((Header) routeList);
        } else {
            sIPRequest.removeHeader("Route");
        }
        if (MessageFactoryImpl.getDefaultUserAgentHeader() != null) {
            sIPRequest.setHeader(MessageFactoryImpl.getDefaultUserAgentHeader());
        }
    }

    @Override
    public Request createAck(long j) throws SipException {
        SipURI sipURI;
        Header authorization;
        NameValueList parameters;
        if (!this.method.equals("INVITE")) {
            throw new SipException("Dialog was not created with an INVITE" + this.method);
        }
        if (j <= 0) {
            throw new InvalidArgumentException("bad cseq <= 0 ");
        }
        if (j > 4294967295L) {
            throw new InvalidArgumentException("bad cseq > 4294967295");
        }
        if (this.remoteTarget == null) {
            throw new SipException("Cannot create ACK - no remote Target!");
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("createAck " + this + " cseqno " + j);
        }
        if (this.lastInviteOkReceived < j) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("WARNING : Attempt to crete ACK without OK " + this);
                this.sipStack.getStackLogger().logDebug("LAST RESPONSE = " + this.lastResponse);
            }
            throw new SipException("Dialog not yet established -- no OK response!");
        }
        try {
            if (this.routeList != null && !this.routeList.isEmpty()) {
                sipURI = (SipURI) ((Route) this.routeList.getFirst()).getAddress().getURI();
            } else {
                sipURI = (SipURI) this.remoteTarget.getURI();
            }
            String transportParam = sipURI.getTransportParam();
            if (transportParam == null) {
                transportParam = sipURI.isSecure() ? ListeningPoint.TLS : ListeningPoint.UDP;
            }
            if (((ListeningPointImpl) this.sipProvider.getListeningPoint(transportParam)) == null) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logError("remoteTargetURI " + this.remoteTarget.getURI());
                    this.sipStack.getStackLogger().logError("uri4transport = " + sipURI);
                    this.sipStack.getStackLogger().logError("No LP found for transport=" + transportParam);
                }
                throw new SipException("Cannot create ACK - no ListeningPoint for transport towards next hop found:" + transportParam);
            }
            SIPRequest sIPRequest = new SIPRequest();
            sIPRequest.setMethod("ACK");
            sIPRequest.setRequestURI((SipUri) getRemoteTarget().getURI().clone());
            sIPRequest.setCallId(this.callIdHeader);
            sIPRequest.setCSeq(new CSeq(j, "ACK"));
            List arrayList = new ArrayList();
            Via topmostVia = this.lastResponse.getTopmostVia();
            topmostVia.removeParameters();
            if (this.originalRequest != null && this.originalRequest.getTopmostVia() != null && (parameters = this.originalRequest.getTopmostVia().getParameters()) != null && parameters.size() > 0) {
                topmostVia.setParameters((NameValueList) parameters.clone());
            }
            topmostVia.setBranch(Utils.getInstance().generateBranchId());
            arrayList.add(topmostVia);
            sIPRequest.setVia(arrayList);
            From from = new From();
            from.setAddress(this.localParty);
            from.setTag(this.myTag);
            sIPRequest.setFrom(from);
            To to = new To();
            to.setAddress(this.remoteParty);
            if (this.hisTag != null) {
                to.setTag(this.hisTag);
            }
            sIPRequest.setTo(to);
            sIPRequest.setMaxForwards(new MaxForwards(70));
            if (this.originalRequest != null && (authorization = this.originalRequest.getAuthorization()) != null) {
                sIPRequest.setHeader(authorization);
            }
            updateRequest(sIPRequest);
            return sIPRequest;
        } catch (Exception e) {
            InternalErrorHandler.handleException(e);
            throw new SipException("unexpected exception ", e);
        }
    }

    @Override
    public SipProviderImpl getSipProvider() {
        return this.sipProvider;
    }

    public void setSipProvider(SipProviderImpl sipProviderImpl) {
        this.sipProvider = sipProviderImpl;
    }

    public void setResponseTags(SIPResponse sIPResponse) {
        if (getLocalTag() != null || getRemoteTag() != null) {
            return;
        }
        String fromTag = sIPResponse.getFromTag();
        if (fromTag != null) {
            if (fromTag.equals(getLocalTag())) {
                sIPResponse.setToTag(getRemoteTag());
                return;
            } else {
                if (fromTag.equals(getRemoteTag())) {
                    sIPResponse.setToTag(getLocalTag());
                    return;
                }
                return;
            }
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logWarning("No from tag in response! Not RFC 3261 compatible.");
        }
    }

    public void setLastResponse(SIPTransaction sIPTransaction, SIPResponse sIPResponse) {
        RecordRouteList recordRouteHeaders;
        this.callIdHeader = sIPResponse.getCallId();
        int statusCode = sIPResponse.getStatusCode();
        if (statusCode == 100) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logWarning("Invalid status code - 100 in setLastResponse - ignoring");
                return;
            }
            return;
        }
        this.lastResponse = sIPResponse;
        setAssigned();
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("sipDialog: setLastResponse:" + this + " lastResponse = " + this.lastResponse.getFirstLine());
        }
        if (getState() == DialogState.TERMINATED) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("sipDialog: setLastResponse -- dialog is terminated - ignoring ");
            }
            if (sIPResponse.getCSeq().getMethod().equals("INVITE") && statusCode == 200) {
                this.lastInviteOkReceived = Math.max(sIPResponse.getCSeq().getSeqNumber(), this.lastInviteOkReceived);
                return;
            }
            return;
        }
        String method = sIPResponse.getCSeq().getMethod();
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logStackTrace();
            this.sipStack.getStackLogger().logDebug("cseqMethod = " + method);
            this.sipStack.getStackLogger().logDebug("dialogState = " + getState());
            this.sipStack.getStackLogger().logDebug("method = " + getMethod());
            this.sipStack.getStackLogger().logDebug("statusCode = " + statusCode);
            this.sipStack.getStackLogger().logDebug("transaction = " + sIPTransaction);
        }
        boolean z = false;
        if (sIPTransaction == null || (sIPTransaction instanceof ClientTransaction)) {
            SIPTransactionStack sIPTransactionStack = this.sipStack;
            if (SIPTransactionStack.isDialogCreated(method)) {
                if (getState() == null && statusCode / 100 == 1) {
                    setState(EARLY_STATE);
                    if ((sIPResponse.getToTag() != null || this.sipStack.rfc2543Supported) && getRemoteTag() == null) {
                        setRemoteTag(sIPResponse.getToTag());
                        setDialogId(sIPResponse.getDialogId(false));
                        this.sipStack.putDialog(this);
                        addRoute(sIPResponse);
                    }
                } else if (getState() != null && getState().equals(DialogState.EARLY) && statusCode / 100 == 1) {
                    if (method.equals(getMethod()) && sIPTransaction != null && (sIPResponse.getToTag() != null || this.sipStack.rfc2543Supported)) {
                        setRemoteTag(sIPResponse.getToTag());
                        setDialogId(sIPResponse.getDialogId(false));
                        this.sipStack.putDialog(this);
                        addRoute(sIPResponse);
                    }
                } else if (statusCode / 100 == 2) {
                    if (method.equals(getMethod()) && ((sIPResponse.getToTag() != null || this.sipStack.rfc2543Supported) && getState() != DialogState.CONFIRMED)) {
                        setRemoteTag(sIPResponse.getToTag());
                        setDialogId(sIPResponse.getDialogId(false));
                        this.sipStack.putDialog(this);
                        addRoute(sIPResponse);
                        setState(CONFIRMED_STATE);
                    }
                    if (method.equals("INVITE")) {
                        this.lastInviteOkReceived = Math.max(sIPResponse.getCSeq().getSeqNumber(), this.lastInviteOkReceived);
                    }
                } else if (statusCode >= 300 && statusCode <= 699 && (getState() == null || (method.equals(getMethod()) && getState().getValue() == EARLY_STATE))) {
                    setState(TERMINATED_STATE);
                }
                if (getState() != DialogState.CONFIRMED && getState() != DialogState.TERMINATED && this.originalRequest != null && (recordRouteHeaders = this.originalRequest.getRecordRouteHeaders()) != null) {
                    ListIterator<RecordRoute> listIterator = recordRouteHeaders.listIterator(recordRouteHeaders.size());
                    while (listIterator.hasPrevious()) {
                        RecordRoute recordRoutePrevious = listIterator.previous();
                        Route route = (Route) this.routeList.getFirst();
                        if (route != null && recordRoutePrevious.getAddress().equals(route.getAddress())) {
                            this.routeList.removeFirst();
                        } else {
                            return;
                        }
                    }
                    return;
                }
                return;
            }
            if (method.equals("NOTIFY") && ((getMethod().equals("SUBSCRIBE") || getMethod().equals(Request.REFER)) && sIPResponse.getStatusCode() / 100 == 2 && getState() == null)) {
                setDialogId(sIPResponse.getDialogId(true));
                this.sipStack.putDialog(this);
                setState(CONFIRMED_STATE);
                return;
            } else {
                if (method.equals("BYE") && statusCode / 100 == 2 && isTerminatedOnBye()) {
                    setState(TERMINATED_STATE);
                    return;
                }
                return;
            }
        }
        if (method.equals("BYE") && statusCode / 100 == 2 && isTerminatedOnBye()) {
            setState(TERMINATED_STATE);
            return;
        }
        if (getLocalTag() == null && sIPResponse.getTo().getTag() != null) {
            SIPTransactionStack sIPTransactionStack2 = this.sipStack;
            if (SIPTransactionStack.isDialogCreated(method) && method.equals(getMethod())) {
                setLocalTag(sIPResponse.getTo().getTag());
                z = true;
            }
        }
        int i = statusCode / 100;
        if (i != 2) {
            if (i == 1) {
                if (z) {
                    setState(EARLY_STATE);
                    setDialogId(sIPResponse.getDialogId(true));
                    this.sipStack.putDialog(this);
                    return;
                }
                return;
            }
            if (statusCode == 489 && (method.equals("NOTIFY") || method.equals("SUBSCRIBE"))) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("RFC 3265 : Not setting dialog to TERMINATED for 489");
                    return;
                }
                return;
            } else {
                if (!isReInvite() && getState() != DialogState.CONFIRMED) {
                    setState(TERMINATED_STATE);
                    return;
                }
                return;
            }
        }
        if (this.dialogState <= EARLY_STATE && (method.equals("INVITE") || method.equals("SUBSCRIBE") || method.equals(Request.REFER))) {
            setState(CONFIRMED_STATE);
        }
        if (z) {
            setDialogId(sIPResponse.getDialogId(true));
            this.sipStack.putDialog(this);
        }
        if (sIPTransaction.getState() != TransactionState.TERMINATED && sIPResponse.getStatusCode() == 200 && method.equals("INVITE") && this.isBackToBackUserAgent && !takeAckSem()) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Delete dialog -- cannot acquire ackSem");
            }
            delete();
        }
    }

    public void startRetransmitTimer(SIPServerTransaction sIPServerTransaction, Response response) {
        if (sIPServerTransaction.getRequest().getMethod().equals("INVITE") && response.getStatusCode() / 100 == 2) {
            startTimer(sIPServerTransaction);
        }
    }

    public SIPResponse getLastResponse() {
        return this.lastResponse;
    }

    private void doTargetRefresh(SIPMessage sIPMessage) {
        ContactList contactHeaders = sIPMessage.getContactHeaders();
        if (contactHeaders != null) {
            setRemoteTarget((Contact) contactHeaders.getFirst());
        }
    }

    private static final boolean optionPresent(ListIterator listIterator, String str) {
        while (listIterator.hasNext()) {
            OptionTag optionTag = (OptionTag) listIterator.next();
            if (optionTag != null && str.equalsIgnoreCase(optionTag.getOptionTag())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Response createReliableProvisionalResponse(int i) throws SipException {
        ListIterator<SIPHeader> headers;
        if (!this.firstTransactionIsServerTransaction) {
            throw new SipException("Not a Server Dialog!");
        }
        if (i <= 100 || i > 199) {
            throw new InvalidArgumentException("Bad status code ");
        }
        SIPRequest sIPRequest = this.originalRequest;
        if (!sIPRequest.getMethod().equals("INVITE")) {
            throw new SipException("Bad method");
        }
        ListIterator<SIPHeader> headers2 = sIPRequest.getHeaders("Supported");
        if ((headers2 == null || !optionPresent(headers2, "100rel")) && ((headers = sIPRequest.getHeaders("Require")) == null || !optionPresent(headers, "100rel"))) {
            throw new SipException("No Supported/Require 100rel header in the request");
        }
        SIPResponse sIPResponseCreateResponse = sIPRequest.createResponse(i);
        Require require = new Require();
        try {
            require.setOptionTag("100rel");
        } catch (Exception e) {
            InternalErrorHandler.handleException(e);
        }
        sIPResponseCreateResponse.addHeader(require);
        new RSeq().setSeqNumber(1L);
        RecordRouteList recordRouteHeaders = sIPRequest.getRecordRouteHeaders();
        if (recordRouteHeaders != null) {
            sIPResponseCreateResponse.setHeader((Header) recordRouteHeaders.clone());
        }
        return sIPResponseCreateResponse;
    }

    public boolean handlePrack(SIPRequest sIPRequest) {
        if (!isServer()) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping Prack -- not a server Dialog");
            }
            return false;
        }
        SIPServerTransaction sIPServerTransaction = (SIPServerTransaction) getFirstTransaction();
        SIPResponse reliableProvisionalResponse = sIPServerTransaction.getReliableProvisionalResponse();
        if (reliableProvisionalResponse == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping Prack -- ReliableResponse not found");
            }
            return false;
        }
        RAck rAck = (RAck) sIPRequest.getHeader("RAck");
        if (rAck == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping Prack -- rack header not found");
            }
            return false;
        }
        CSeq cSeq = (CSeq) reliableProvisionalResponse.getCSeq();
        if (!rAck.getMethod().equals(cSeq.getMethod())) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping Prack -- CSeq Header does not match PRACK");
            }
            return false;
        }
        if (rAck.getCSeqNumberLong() != cSeq.getSeqNumber()) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping Prack -- CSeq Header does not match PRACK");
            }
            return false;
        }
        if (rAck.getRSequenceNumber() != ((RSeq) reliableProvisionalResponse.getHeader("RSeq")).getSeqNumber()) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping Prack -- RSeq Header does not match PRACK");
            }
            return false;
        }
        return sIPServerTransaction.prackRecieved();
    }

    @Override
    public void sendReliableProvisionalResponse(Response response) throws SipException {
        if (!isServer()) {
            throw new SipException("Not a Server Dialog");
        }
        SIPResponse sIPResponse = (SIPResponse) response;
        if (response.getStatusCode() == 100) {
            throw new SipException("Cannot send 100 as a reliable provisional response");
        }
        if (response.getStatusCode() / 100 > 2) {
            throw new SipException("Response code is not a 1xx response - should be in the range 101 to 199 ");
        }
        if (sIPResponse.getToTag() == null) {
            throw new SipException("Badly formatted response -- To tag mandatory for Reliable Provisional Response");
        }
        ListIterator headers = response.getHeaders("Require");
        boolean z = false;
        if (headers != null) {
            while (headers.hasNext() && !z) {
                if (((RequireHeader) headers.next()).getOptionTag().equalsIgnoreCase("100rel")) {
                    z = true;
                }
            }
        }
        if (!z) {
            response.addHeader(new Require("100rel"));
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Require header with optionTag 100rel is needed -- adding one");
            }
        }
        SIPServerTransaction sIPServerTransaction = (SIPServerTransaction) getFirstTransaction();
        setLastResponse(sIPServerTransaction, sIPResponse);
        setDialogId(sIPResponse.getDialogId(true));
        sIPServerTransaction.sendReliableProvisionalResponse(response);
        startRetransmitTimer(sIPServerTransaction, response);
    }

    @Override
    public void terminateOnBye(boolean z) throws SipException {
        this.terminateOnBye = z;
    }

    public void setAssigned() {
        this.isAssigned = true;
    }

    public boolean isAssigned() {
        return this.isAssigned;
    }

    public Contact getMyContactHeader() {
        return this.contactHeader;
    }

    public boolean handleAck(SIPServerTransaction sIPServerTransaction) {
        SIPRequest originalRequest = sIPServerTransaction.getOriginalRequest();
        if (isAckSeen() && getRemoteSeqNumber() == originalRequest.getCSeq().getSeqNumber()) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("ACK already seen by dialog -- dropping Ack retransmission");
            }
            acquireTimerTaskSem();
            try {
                if (this.timerTask != null) {
                    this.timerTask.cancel();
                    this.timerTask = null;
                }
                return false;
            } finally {
                releaseTimerTaskSem();
            }
        }
        if (getState() == DialogState.TERMINATED) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dialog is terminated -- dropping ACK");
            }
            return false;
        }
        SIPServerTransaction inviteTransaction = getInviteTransaction();
        SIPResponse lastResponse = inviteTransaction != null ? inviteTransaction.getLastResponse() : null;
        if (inviteTransaction != null && lastResponse != null && lastResponse.getStatusCode() / 100 == 2 && lastResponse.getCSeq().getMethod().equals("INVITE") && lastResponse.getCSeq().getSeqNumber() == originalRequest.getCSeq().getSeqNumber()) {
            sIPServerTransaction.setDialog(this, lastResponse.getDialogId(false));
            ackReceived(originalRequest);
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("ACK for 2XX response --- sending to TU ");
                return true;
            }
            return true;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug(" INVITE transaction not found  -- Discarding ACK");
        }
        return false;
    }

    void setEarlyDialogId(String str) {
        this.earlyDialogId = str;
    }

    String getEarlyDialogId() {
        return this.earlyDialogId;
    }

    void releaseAckSem() {
        if (this.isBackToBackUserAgent) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("releaseAckSem]" + this);
            }
            this.ackSem.release();
        }
    }

    boolean takeAckSem() {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("[takeAckSem " + this);
        }
        try {
            if (!this.ackSem.tryAcquire(2L, TimeUnit.SECONDS)) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logError("Cannot aquire ACK semaphore");
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Semaphore previously acquired at " + this.stackTrace);
                    this.sipStack.getStackLogger().logStackTrace();
                }
                return false;
            }
            if (this.sipStack.isLoggingEnabled()) {
                recordStackTrace();
                return true;
            }
            return true;
        } catch (InterruptedException e) {
            this.sipStack.getStackLogger().logError("Cannot aquire ACK semaphore");
            return false;
        }
    }

    private void setLastAckReceived(SIPRequest sIPRequest) {
        this.lastAckReceived = sIPRequest;
    }

    protected SIPRequest getLastAckReceived() {
        return this.lastAckReceived;
    }

    private void setLastAckSent(SIPRequest sIPRequest) {
        this.lastAckSent = sIPRequest;
    }

    public boolean isAtleastOneAckSent() {
        return this.isAcknowledged;
    }

    public boolean isBackToBackUserAgent() {
        return this.isBackToBackUserAgent;
    }

    public synchronized void doDeferredDeleteIfNoAckSent(long j) {
        if (this.sipStack.getTimer() == null) {
            setState(TERMINATED_STATE);
        } else if (this.dialogDeleteIfNoAckSentTask == null) {
            this.dialogDeleteIfNoAckSentTask = new DialogDeleteIfNoAckSentTask(j);
            this.sipStack.getTimer().schedule(this.dialogDeleteIfNoAckSentTask, 32000L);
        }
    }

    @Override
    public void setBackToBackUserAgent() {
        this.isBackToBackUserAgent = true;
    }

    EventHeader getEventHeader() {
        return this.eventHeader;
    }

    void setEventHeader(EventHeader eventHeader) {
        this.eventHeader = eventHeader;
    }

    void setServerTransactionFlag(boolean z) {
        this.serverTransactionFlag = z;
    }

    void setReInviteFlag(boolean z) {
        this.reInviteFlag = z;
    }

    public boolean isSequnceNumberValidation() {
        return this.sequenceNumberValidation;
    }

    @Override
    public void disableSequenceNumberValidation() {
        this.sequenceNumberValidation = false;
    }

    public void acquireTimerTaskSem() {
        boolean zTryAcquire;
        try {
            zTryAcquire = this.timerTaskLock.tryAcquire(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            zTryAcquire = false;
        }
        if (!zTryAcquire) {
            throw new IllegalStateException("Impossible to acquire the dialog timer task lock");
        }
    }

    public void releaseTimerTaskSem() {
        this.timerTaskLock.release();
    }
}
