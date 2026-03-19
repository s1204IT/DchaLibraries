package gov.nist.javax.sip.stack;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.NameValueList;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.ClientTransactionExt;
import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.Utils;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.Contact;
import gov.nist.javax.sip.header.RecordRoute;
import gov.nist.javax.sip.header.RecordRouteList;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.header.TimeStamp;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.SIPTransaction;
import java.io.IOException;
import java.text.ParseException;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.SipException;
import javax.sip.Timeout;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionState;
import javax.sip.address.Hop;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.message.Request;

public class SIPClientTransaction extends SIPTransaction implements ServerResponseInterface, ClientTransaction, ClientTransactionExt {
    private int callingStateTimeoutCount;
    private SIPDialog defaultDialog;
    private SIPRequest lastRequest;
    private Hop nextHop;
    private boolean notifyOnRetransmit;
    private transient ServerResponseInterface respondTo;
    private ConcurrentHashMap<String, SIPDialog> sipDialogs;
    private boolean timeoutIfStillInCallingState;
    private String viaHost;
    private int viaPort;

    public class TransactionTimer extends SIPStackTimerTask {
        public TransactionTimer() {
        }

        @Override
        protected void runTask() {
            SIPClientTransaction sIPClientTransaction = SIPClientTransaction.this;
            SIPTransactionStack sIPTransactionStack = sIPClientTransaction.sipStack;
            if (sIPClientTransaction.isTerminated()) {
                if (sIPTransactionStack.isLoggingEnabled()) {
                    sIPTransactionStack.getStackLogger().logDebug("removing  = " + sIPClientTransaction + " isReliable " + sIPClientTransaction.isReliable());
                }
                sIPTransactionStack.removeTransaction(sIPClientTransaction);
                try {
                    cancel();
                } catch (IllegalStateException e) {
                    if (!sIPTransactionStack.isAlive()) {
                        return;
                    }
                }
                if (!sIPTransactionStack.cacheClientConnections && sIPClientTransaction.isReliable()) {
                    MessageChannel messageChannel = sIPClientTransaction.getMessageChannel();
                    int i = messageChannel.useCount - 1;
                    messageChannel.useCount = i;
                    if (i <= 0) {
                        sIPTransactionStack.getTimer().schedule(new SIPTransaction.LingerTimer(), 8000L);
                        return;
                    }
                    return;
                }
                if (sIPTransactionStack.isLoggingEnabled() && sIPClientTransaction.isReliable()) {
                    int i2 = sIPClientTransaction.getMessageChannel().useCount;
                    if (sIPTransactionStack.isLoggingEnabled()) {
                        sIPTransactionStack.getStackLogger().logDebug("Client Use Count = " + i2);
                        return;
                    }
                    return;
                }
                return;
            }
            sIPClientTransaction.fireTimer();
        }
    }

    protected SIPClientTransaction(SIPTransactionStack sIPTransactionStack, MessageChannel messageChannel) {
        super(sIPTransactionStack, messageChannel);
        setBranch(Utils.getInstance().generateBranchId());
        this.messageProcessor = messageChannel.messageProcessor;
        setEncapsulatedChannel(messageChannel);
        this.notifyOnRetransmit = false;
        this.timeoutIfStillInCallingState = false;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Creating clientTransaction " + this);
            this.sipStack.getStackLogger().logStackTrace();
        }
        this.sipDialogs = new ConcurrentHashMap<>();
    }

    public void setResponseInterface(ServerResponseInterface serverResponseInterface) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("Setting response interface for " + this + " to " + serverResponseInterface);
            if (serverResponseInterface == null) {
                this.sipStack.getStackLogger().logStackTrace();
                this.sipStack.getStackLogger().logDebug("WARNING -- setting to null!");
            }
        }
        this.respondTo = serverResponseInterface;
    }

    public MessageChannel getRequestChannel() {
        return this;
    }

    @Override
    public boolean isMessagePartOfTransaction(SIPMessage sIPMessage) {
        ViaList viaHeaders = sIPMessage.getViaHeaders();
        String branch = ((Via) viaHeaders.getFirst()).getBranch();
        boolean z = getBranch() != null && branch != null && getBranch().toLowerCase().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE) && branch.toLowerCase().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE);
        if (TransactionState.COMPLETED == getState()) {
            if (z) {
                return getBranch().equalsIgnoreCase(((Via) viaHeaders.getFirst()).getBranch()) && getMethod().equals(sIPMessage.getCSeq().getMethod());
            }
            return getBranch().equals(sIPMessage.getTransactionId());
        }
        if (isTerminated()) {
            return false;
        }
        if (z) {
            if (viaHeaders == null || !getBranch().equalsIgnoreCase(((Via) viaHeaders.getFirst()).getBranch())) {
                return false;
            }
            return getOriginalRequest().getCSeq().getMethod().equals(sIPMessage.getCSeq().getMethod());
        }
        if (getBranch() != null) {
            return getBranch().equalsIgnoreCase(sIPMessage.getTransactionId());
        }
        return getOriginalRequest().getTransactionId().equalsIgnoreCase(sIPMessage.getTransactionId());
    }

    @Override
    public void sendMessage(SIPMessage sIPMessage) throws IOException {
        try {
            SIPRequest sIPRequest = (SIPRequest) sIPMessage;
            try {
                ((Via) sIPRequest.getViaHeaders().getFirst()).setBranch(getBranch());
            } catch (ParseException e) {
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Sending Message " + sIPMessage);
                this.sipStack.getStackLogger().logDebug("TransactionState " + getState());
            }
            if ((TransactionState.PROCEEDING == getState() || TransactionState.CALLING == getState()) && sIPRequest.getMethod().equals("ACK")) {
                if (isReliable()) {
                    setState(TransactionState.TERMINATED);
                } else {
                    setState(TransactionState.COMPLETED);
                }
                super.sendMessage(sIPRequest);
                return;
            }
            try {
                this.lastRequest = sIPRequest;
                if (getState() == null) {
                    setOriginalRequest(sIPRequest);
                    if (sIPRequest.getMethod().equals("INVITE")) {
                        setState(TransactionState.CALLING);
                    } else if (sIPRequest.getMethod().equals("ACK")) {
                        setState(TransactionState.TERMINATED);
                    } else {
                        setState(TransactionState.TRYING);
                    }
                    if (!isReliable()) {
                        enableRetransmissionTimer();
                    }
                    if (isInviteTransaction()) {
                        enableTimeoutTimer(64);
                    } else {
                        enableTimeoutTimer(64);
                    }
                }
                super.sendMessage(sIPRequest);
            } catch (IOException e2) {
                setState(TransactionState.TERMINATED);
                throw e2;
            }
        } finally {
            this.isMapped = true;
            startTransactionTimer();
        }
    }

    @Override
    public synchronized void processResponse(SIPResponse sIPResponse, MessageChannel messageChannel, SIPDialog sIPDialog) {
        if (getState() == null) {
            return;
        }
        if ((TransactionState.COMPLETED == getState() || TransactionState.TERMINATED == getState()) && sIPResponse.getStatusCode() / 100 == 1) {
            return;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("processing " + sIPResponse.getFirstLine() + "current state = " + getState());
            StackLogger stackLogger = this.sipStack.getStackLogger();
            StringBuilder sb = new StringBuilder();
            sb.append("dialog = ");
            sb.append(sIPDialog);
            stackLogger.logDebug(sb.toString());
        }
        this.lastResponse = sIPResponse;
        try {
            if (isInviteTransaction()) {
                inviteClientTransaction(sIPResponse, messageChannel, sIPDialog);
            } else {
                nonInviteClientTransaction(sIPResponse, messageChannel, sIPDialog);
            }
        } catch (IOException e) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logException(e);
            }
            setState(TransactionState.TERMINATED);
            raiseErrorEvent(2);
        }
    }

    private void nonInviteClientTransaction(SIPResponse sIPResponse, MessageChannel messageChannel, SIPDialog sIPDialog) throws IOException {
        int statusCode = sIPResponse.getStatusCode();
        if (TransactionState.TRYING == getState()) {
            if (statusCode / 100 == 1) {
                setState(TransactionState.PROCEEDING);
                enableRetransmissionTimer(8);
                enableTimeoutTimer(64);
                if (this.respondTo != null) {
                    this.respondTo.processResponse(sIPResponse, this, sIPDialog);
                    return;
                } else {
                    semRelease();
                    return;
                }
            }
            if (200 <= statusCode && statusCode <= 699) {
                if (this.respondTo != null) {
                    this.respondTo.processResponse(sIPResponse, this, sIPDialog);
                } else {
                    semRelease();
                }
                if (!isReliable()) {
                    setState(TransactionState.COMPLETED);
                    enableTimeoutTimer(this.TIMER_K);
                    return;
                } else {
                    setState(TransactionState.TERMINATED);
                    return;
                }
            }
            return;
        }
        if (TransactionState.PROCEEDING == getState()) {
            if (statusCode / 100 == 1) {
                if (this.respondTo != null) {
                    this.respondTo.processResponse(sIPResponse, this, sIPDialog);
                    return;
                } else {
                    semRelease();
                    return;
                }
            }
            if (200 <= statusCode && statusCode <= 699) {
                if (this.respondTo != null) {
                    this.respondTo.processResponse(sIPResponse, this, sIPDialog);
                } else {
                    semRelease();
                }
                disableRetransmissionTimer();
                disableTimeoutTimer();
                if (!isReliable()) {
                    setState(TransactionState.COMPLETED);
                    enableTimeoutTimer(this.TIMER_K);
                    return;
                } else {
                    setState(TransactionState.TERMINATED);
                    return;
                }
            }
            return;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug(" Not sending response to TU! " + getState());
        }
        semRelease();
    }

    private void inviteClientTransaction(SIPResponse sIPResponse, MessageChannel messageChannel, SIPDialog sIPDialog) throws IOException {
        int statusCode = sIPResponse.getStatusCode();
        if (TransactionState.TERMINATED == getState()) {
            boolean z = false;
            if (sIPDialog != null && sIPDialog.isAckSeen() && sIPDialog.getLastAckSent() != null && sIPDialog.getLastAckSent().getCSeq().getSeqNumber() == sIPResponse.getCSeq().getSeqNumber() && sIPResponse.getFromTag().equals(sIPDialog.getLastAckSent().getFromTag())) {
                z = true;
            }
            if (sIPDialog != null && z && sIPResponse.getCSeq().getMethod().equals(sIPDialog.getMethod())) {
                try {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("resending ACK");
                    }
                    sIPDialog.resendAck();
                } catch (SipException e) {
                }
            }
            return;
        }
        if (TransactionState.CALLING == getState()) {
            int i = statusCode / 100;
            if (i == 2) {
                disableRetransmissionTimer();
                disableTimeoutTimer();
                setState(TransactionState.TERMINATED);
                if (this.respondTo != null) {
                    this.respondTo.processResponse(sIPResponse, this, sIPDialog);
                    return;
                }
                return;
            }
            if (i == 1) {
                disableRetransmissionTimer();
                disableTimeoutTimer();
                setState(TransactionState.PROCEEDING);
                if (this.respondTo != null) {
                    this.respondTo.processResponse(sIPResponse, this, sIPDialog);
                    return;
                }
                return;
            }
            if (300 <= statusCode && statusCode <= 699) {
                try {
                    sendMessage((SIPRequest) createErrorAck());
                } catch (Exception e2) {
                    this.sipStack.getStackLogger().logError("Unexpected Exception sending ACK -- sending error AcK ", e2);
                }
                if (this.respondTo != null) {
                    this.respondTo.processResponse(sIPResponse, this, sIPDialog);
                }
                if (getDialog() != null && ((SIPDialog) getDialog()).isBackToBackUserAgent()) {
                    ((SIPDialog) getDialog()).releaseAckSem();
                }
                if (!isReliable()) {
                    setState(TransactionState.COMPLETED);
                    enableTimeoutTimer(this.TIMER_D);
                    return;
                } else {
                    setState(TransactionState.TERMINATED);
                    return;
                }
            }
            return;
        }
        if (TransactionState.PROCEEDING == getState()) {
            int i2 = statusCode / 100;
            if (i2 == 1) {
                if (this.respondTo != null) {
                    this.respondTo.processResponse(sIPResponse, this, sIPDialog);
                    return;
                }
                return;
            }
            if (i2 == 2) {
                setState(TransactionState.TERMINATED);
                if (this.respondTo != null) {
                    this.respondTo.processResponse(sIPResponse, this, sIPDialog);
                    return;
                }
                return;
            }
            if (300 <= statusCode && statusCode <= 699) {
                try {
                    sendMessage((SIPRequest) createErrorAck());
                } catch (Exception e3) {
                    InternalErrorHandler.handleException(e3);
                }
                if (getDialog() != null) {
                    ((SIPDialog) getDialog()).releaseAckSem();
                }
                if (!isReliable()) {
                    setState(TransactionState.COMPLETED);
                    enableTimeoutTimer(this.TIMER_D);
                } else {
                    setState(TransactionState.TERMINATED);
                }
                if (this.respondTo != null) {
                    this.respondTo.processResponse(sIPResponse, this, sIPDialog);
                    return;
                }
                return;
            }
            return;
        }
        if (TransactionState.COMPLETED == getState() && 300 <= statusCode && statusCode <= 699) {
            try {
                try {
                    sendMessage((SIPRequest) createErrorAck());
                } catch (Exception e4) {
                    InternalErrorHandler.handleException(e4);
                }
            } finally {
                semRelease();
            }
        }
    }

    @Override
    public void sendRequest() throws SipException {
        SIPDialog defaultDialog;
        SIPRequest originalRequest = getOriginalRequest();
        if (getState() != null) {
            throw new SipException("Request already sent");
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("sendRequest() " + originalRequest);
        }
        try {
            originalRequest.checkHeaders();
            if (getMethod().equals("SUBSCRIBE") && originalRequest.getHeader("Expires") == null && this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logWarning("Expires header missing in outgoing subscribe -- Notifier will assume implied value on event package");
            }
            try {
                if (getOriginalRequest().getMethod().equals(Request.CANCEL) && this.sipStack.isCancelClientTransactionChecked()) {
                    SIPClientTransaction sIPClientTransaction = (SIPClientTransaction) this.sipStack.findCancelTransaction(getOriginalRequest(), false);
                    if (sIPClientTransaction == null) {
                        throw new SipException("Could not find original tx to cancel. RFC 3261 9.1");
                    }
                    if (sIPClientTransaction.getState() == null) {
                        throw new SipException("State is null no provisional response yet -- cannot cancel RFC 3261 9.1");
                    }
                    if (!sIPClientTransaction.getMethod().equals("INVITE")) {
                        throw new SipException("Cannot cancel non-invite requests RFC 3261 9.1");
                    }
                } else if (getOriginalRequest().getMethod().equals("BYE") || getOriginalRequest().getMethod().equals("NOTIFY")) {
                    SIPDialog dialog = this.sipStack.getDialog(getOriginalRequest().getDialogId(false));
                    if (getSipProvider().isAutomaticDialogSupportEnabled() && dialog != null) {
                        throw new SipException("Dialog is present and AutomaticDialogSupport is enabled for  the provider -- Send the Request using the Dialog.sendRequest(transaction)");
                    }
                }
                if (getMethod().equals("INVITE") && (defaultDialog = getDefaultDialog()) != null && defaultDialog.isBackToBackUserAgent() && !defaultDialog.takeAckSem()) {
                    throw new SipException("Failed to take ACK semaphore");
                }
                this.isMapped = true;
                sendMessage(originalRequest);
            } catch (IOException e) {
                setState(TransactionState.TERMINATED);
                throw new SipException("IO Error sending request", e);
            }
        } catch (ParseException e2) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("missing required header");
            }
            throw new SipException(e2.getMessage());
        }
    }

    @Override
    protected void fireRetransmissionTimer() {
        try {
            if (getState() != null && this.isMapped) {
                boolean zIsInviteTransaction = isInviteTransaction();
                TransactionState state = getState();
                if (!zIsInviteTransaction || TransactionState.CALLING != state) {
                    if (zIsInviteTransaction) {
                        return;
                    }
                    if (TransactionState.TRYING != state && TransactionState.PROCEEDING != state) {
                        return;
                    }
                }
                if (this.lastRequest != null) {
                    if (this.sipStack.generateTimeStampHeader && this.lastRequest.getHeader("Timestamp") != null) {
                        long jCurrentTimeMillis = System.currentTimeMillis();
                        TimeStamp timeStamp = new TimeStamp();
                        try {
                            timeStamp.setTimeStamp(jCurrentTimeMillis);
                        } catch (InvalidArgumentException e) {
                            InternalErrorHandler.handleException(e);
                        }
                        this.lastRequest.setHeader(timeStamp);
                    }
                    super.sendMessage(this.lastRequest);
                    if (this.notifyOnRetransmit) {
                        getSipProvider().handleEvent(new TimeoutEvent(getSipProvider(), this, Timeout.RETRANSMIT), this);
                    }
                    if (this.timeoutIfStillInCallingState && getState() == TransactionState.CALLING) {
                        this.callingStateTimeoutCount--;
                        if (this.callingStateTimeoutCount == 0) {
                            getSipProvider().handleEvent(new TimeoutEvent(getSipProvider(), this, Timeout.RETRANSMIT), this);
                            this.timeoutIfStillInCallingState = false;
                        }
                    }
                }
            }
        } catch (IOException e2) {
            raiseIOExceptionEvent();
            raiseErrorEvent(2);
        }
    }

    @Override
    protected void fireTimeoutTimer() {
        SIPClientTransaction sIPClientTransaction;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("fireTimeoutTimer " + this);
        }
        SIPDialog sIPDialog = (SIPDialog) getDialog();
        if (TransactionState.CALLING == getState() || TransactionState.TRYING == getState() || TransactionState.PROCEEDING == getState()) {
            if (sIPDialog != null && (sIPDialog.getState() == null || sIPDialog.getState() == DialogState.EARLY)) {
                getSIPStack();
                if (SIPTransactionStack.isDialogCreated(getOriginalRequest().getMethod())) {
                    sIPDialog.delete();
                }
            } else if (sIPDialog != null && getOriginalRequest().getMethod().equalsIgnoreCase("BYE") && sIPDialog.isTerminatedOnBye()) {
                sIPDialog.delete();
            }
        }
        if (TransactionState.COMPLETED != getState()) {
            raiseErrorEvent(1);
            if (getOriginalRequest().getMethod().equalsIgnoreCase(Request.CANCEL) && (sIPClientTransaction = (SIPClientTransaction) getOriginalRequest().getInviteTransaction()) != null) {
                if ((sIPClientTransaction.getState() == TransactionState.CALLING || sIPClientTransaction.getState() == TransactionState.PROCEEDING) && sIPClientTransaction.getDialog() != null) {
                    sIPClientTransaction.setState(TransactionState.TERMINATED);
                    return;
                }
                return;
            }
            return;
        }
        setState(TransactionState.TERMINATED);
    }

    @Override
    public Request createCancel() throws SipException {
        SIPRequest originalRequest = getOriginalRequest();
        if (originalRequest == null) {
            throw new SipException("Bad state " + getState());
        }
        if (!originalRequest.getMethod().equals("INVITE")) {
            throw new SipException("Only INIVTE may be cancelled");
        }
        if (originalRequest.getMethod().equalsIgnoreCase("ACK")) {
            throw new SipException("Cannot Cancel ACK!");
        }
        SIPRequest sIPRequestCreateCancelRequest = originalRequest.createCancelRequest();
        sIPRequestCreateCancelRequest.setInviteTransaction(this);
        return sIPRequestCreateCancelRequest;
    }

    @Override
    public Request createAck() throws SipException {
        Contact contact;
        SIPRequest originalRequest = getOriginalRequest();
        if (originalRequest == null) {
            throw new SipException("bad state " + getState());
        }
        if (getMethod().equalsIgnoreCase("ACK")) {
            throw new SipException("Cannot ACK an ACK!");
        }
        if (this.lastResponse == null) {
            throw new SipException("bad Transaction state");
        }
        if (this.lastResponse.getStatusCode() < 200) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("lastResponse = " + this.lastResponse);
            }
            throw new SipException("Cannot ACK a provisional response!");
        }
        SIPRequest sIPRequestCreateAckRequest = originalRequest.createAckRequest((To) this.lastResponse.getTo());
        RecordRouteList recordRouteHeaders = this.lastResponse.getRecordRouteHeaders();
        if (recordRouteHeaders == null) {
            if (this.lastResponse.getContactHeaders() != null && this.lastResponse.getStatusCode() / 100 != 3) {
                sIPRequestCreateAckRequest.setRequestURI((URI) ((Contact) this.lastResponse.getContactHeaders().getFirst()).getAddress().getURI().clone());
            }
            return sIPRequestCreateAckRequest;
        }
        sIPRequestCreateAckRequest.removeHeader("Route");
        RouteList routeList = new RouteList();
        ListIterator<RecordRoute> listIterator = recordRouteHeaders.listIterator(recordRouteHeaders.size());
        while (listIterator.hasPrevious()) {
            RecordRoute recordRoutePrevious = listIterator.previous();
            Route route = new Route();
            route.setAddress((AddressImpl) ((AddressImpl) recordRoutePrevious.getAddress()).clone());
            route.setParameters((NameValueList) recordRoutePrevious.getParameters().clone());
            routeList.add(route);
        }
        Route route2 = null;
        if (this.lastResponse.getContactHeaders() != null) {
            contact = (Contact) this.lastResponse.getContactHeaders().getFirst();
        } else {
            contact = null;
        }
        if (!((SipURI) ((Route) routeList.getFirst()).getAddress().getURI()).hasLrParam()) {
            if (contact != null) {
                route2 = new Route();
                route2.setAddress((AddressImpl) ((AddressImpl) contact.getAddress()).clone());
            }
            Route route3 = (Route) routeList.getFirst();
            routeList.removeFirst();
            sIPRequestCreateAckRequest.setRequestURI(route3.getAddress().getURI());
            if (route2 != null) {
                routeList.add(route2);
            }
            sIPRequestCreateAckRequest.addHeader(routeList);
        } else if (contact != null) {
            sIPRequestCreateAckRequest.setRequestURI((URI) contact.getAddress().getURI().clone());
            sIPRequestCreateAckRequest.addHeader(routeList);
        }
        return sIPRequestCreateAckRequest;
    }

    private final Request createErrorAck() throws SipException, ParseException {
        SIPRequest originalRequest = getOriginalRequest();
        if (originalRequest == null) {
            throw new SipException("bad state " + getState());
        }
        if (!getMethod().equals("INVITE")) {
            throw new SipException("Can only ACK an INVITE!");
        }
        if (this.lastResponse == null) {
            throw new SipException("bad Transaction state");
        }
        if (this.lastResponse.getStatusCode() < 200) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("lastResponse = " + this.lastResponse);
            }
            throw new SipException("Cannot ACK a provisional response!");
        }
        return originalRequest.createErrorAck((To) this.lastResponse.getTo());
    }

    protected void setViaPort(int i) {
        this.viaPort = i;
    }

    protected void setViaHost(String str) {
        this.viaHost = str;
    }

    @Override
    public int getViaPort() {
        return this.viaPort;
    }

    @Override
    public String getViaHost() {
        return this.viaHost;
    }

    public Via getOutgoingViaHeader() {
        return getMessageProcessor().getViaHeader();
    }

    public void clearState() {
    }

    @Override
    public void setState(TransactionState transactionState) {
        if (transactionState == TransactionState.TERMINATED && isReliable() && !getSIPStack().cacheClientConnections) {
            this.collectionTime = 64;
        }
        if (super.getState() != TransactionState.COMPLETED && (transactionState == TransactionState.COMPLETED || transactionState == TransactionState.TERMINATED)) {
            this.sipStack.decrementActiveClientTransactionCount();
        }
        super.setState(transactionState);
    }

    @Override
    protected void startTransactionTimer() {
        if (this.transactionTimerStarted.compareAndSet(false, true)) {
            TransactionTimer transactionTimer = new TransactionTimer();
            if (this.sipStack.getTimer() != null) {
                this.sipStack.getTimer().schedule(transactionTimer, this.BASE_TIMER_INTERVAL, this.BASE_TIMER_INTERVAL);
            }
        }
    }

    @Override
    public void terminate() throws ObjectInUseException {
        setState(TransactionState.TERMINATED);
    }

    public boolean checkFromTag(SIPResponse sIPResponse) {
        String fromTag = ((SIPRequest) getRequest()).getFromTag();
        if (this.defaultDialog != null) {
            if ((fromTag == null) ^ (sIPResponse.getFrom().getTag() == null)) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("From tag mismatch -- dropping response");
                }
                return false;
            }
            if (fromTag != null && !fromTag.equalsIgnoreCase(sIPResponse.getFrom().getTag())) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("From tag mismatch -- dropping response");
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public void processResponse(SIPResponse sIPResponse, MessageChannel messageChannel) {
        SIPDialog dialog;
        SIPDialog dialog2;
        String method = sIPResponse.getCSeq().getMethod();
        String dialogId = sIPResponse.getDialogId(false);
        if (method.equals(Request.CANCEL) && this.lastRequest != null) {
            SIPClientTransaction sIPClientTransaction = (SIPClientTransaction) this.lastRequest.getInviteTransaction();
            if (sIPClientTransaction != null) {
                dialog = sIPClientTransaction.defaultDialog;
            } else {
                dialog = null;
            }
        } else {
            dialog = getDialog(dialogId);
        }
        if (dialog == null) {
            int statusCode = sIPResponse.getStatusCode();
            if (statusCode > 100 && statusCode < 300 && (sIPResponse.getToTag() != null || this.sipStack.isRfc2543Supported())) {
                SIPTransactionStack sIPTransactionStack = this.sipStack;
                if (SIPTransactionStack.isDialogCreated(method)) {
                    synchronized (this) {
                        if (this.defaultDialog != null) {
                            if (sIPResponse.getFromTag() != null) {
                                SIPResponse lastResponse = this.defaultDialog.getLastResponse();
                                String dialogId2 = this.defaultDialog.getDialogId();
                                if (lastResponse == null || (method.equals("SUBSCRIBE") && lastResponse.getCSeq().getMethod().equals("NOTIFY") && dialogId2.equals(dialogId))) {
                                    this.defaultDialog.setLastResponse(this, sIPResponse);
                                    dialog2 = this.defaultDialog;
                                } else {
                                    dialog2 = this.sipStack.getDialog(dialogId);
                                    if (dialog2 == null && this.defaultDialog.isAssigned()) {
                                        dialog2 = this.sipStack.createDialog(this, sIPResponse);
                                    }
                                }
                                dialog = dialog2;
                                if (dialog != null) {
                                    setDialog(dialog, dialog.getDialogId());
                                } else {
                                    this.sipStack.getStackLogger().logError("dialog is unexpectedly null", new NullPointerException());
                                }
                            } else {
                                throw new RuntimeException("Response without from-tag");
                            }
                        } else if (this.sipStack.isAutomaticDialogSupportEnabled) {
                            dialog = this.sipStack.createDialog(this, sIPResponse);
                            setDialog(dialog, dialog.getDialogId());
                        }
                    }
                }
            } else {
                dialog = this.defaultDialog;
            }
        } else {
            dialog.setLastResponse(this, sIPResponse);
        }
        processResponse(sIPResponse, messageChannel, dialog);
    }

    @Override
    public Dialog getDialog() {
        SIPDialog dialog;
        if (this.lastResponse != null && this.lastResponse.getFromTag() != null && this.lastResponse.getToTag() != null && this.lastResponse.getStatusCode() != 100) {
            dialog = getDialog(this.lastResponse.getDialogId(false));
        } else {
            dialog = null;
        }
        if (dialog == null) {
            dialog = this.defaultDialog;
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug(" sipDialogs =  " + this.sipDialogs + " default dialog " + this.defaultDialog + " retval " + dialog);
        }
        return dialog;
    }

    public SIPDialog getDialog(String str) {
        return this.sipDialogs.get(str);
    }

    @Override
    public void setDialog(SIPDialog sIPDialog, String str) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("setDialog: " + str + "sipDialog = " + sIPDialog);
        }
        if (sIPDialog == null) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("NULL DIALOG!!");
            }
            throw new NullPointerException("bad dialog null");
        }
        if (this.defaultDialog == null) {
            this.defaultDialog = sIPDialog;
            if (getMethod().equals("INVITE") && getSIPStack().maxForkTime != 0) {
                getSIPStack().addForkedClientTransaction(this);
            }
        }
        if (str != null && sIPDialog.getDialogId() != null) {
            this.sipDialogs.put(str, sIPDialog);
        }
    }

    public SIPDialog getDefaultDialog() {
        return this.defaultDialog;
    }

    public void setNextHop(Hop hop) {
        this.nextHop = hop;
    }

    @Override
    public Hop getNextHop() {
        return this.nextHop;
    }

    @Override
    public void setNotifyOnRetransmit(boolean z) {
        this.notifyOnRetransmit = z;
    }

    public boolean isNotifyOnRetransmit() {
        return this.notifyOnRetransmit;
    }

    @Override
    public void alertIfStillInCallingStateBy(int i) {
        this.timeoutIfStillInCallingState = true;
        this.callingStateTimeoutCount = i;
    }
}
