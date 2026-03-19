package gov.nist.javax.sip.stack;

import gov.nist.core.InternalErrorHandler;
import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.ServerTransactionExt;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.Utils;
import gov.nist.javax.sip.header.Expires;
import gov.nist.javax.sip.header.RSeq;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.SIPTransaction;
import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.Timeout;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionState;
import javax.sip.address.Hop;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class SIPServerTransaction extends SIPTransaction implements ServerRequestInterface, ServerTransaction, ServerTransactionExt {
    private SIPDialog dialog;
    private SIPServerTransaction inviteTransaction;
    protected boolean isAckSeen;
    private SIPResponse pendingReliableResponse;
    private SIPClientTransaction pendingSubscribeTransaction;
    private Semaphore provisionalResponseSem;
    private ProvisionalResponseTask provisionalResponseTask;
    private transient ServerRequestInterface requestOf;
    private boolean retransmissionAlertEnabled;
    private RetransmissionAlertTimerTask retransmissionAlertTimerTask;
    private int rseqNumber;

    class RetransmissionAlertTimerTask extends SIPStackTimerTask {
        String dialogId;
        int ticks = 1;
        int ticksLeft = this.ticks;

        public RetransmissionAlertTimerTask(String str) {
        }

        @Override
        protected void runTask() {
            SIPServerTransaction sIPServerTransaction = SIPServerTransaction.this;
            this.ticksLeft--;
            if (this.ticksLeft == -1) {
                sIPServerTransaction.fireRetransmissionTimer();
                this.ticksLeft = 2 * this.ticks;
            }
        }
    }

    class ProvisionalResponseTask extends SIPStackTimerTask {
        int ticks = 1;
        int ticksLeft = this.ticks;

        public ProvisionalResponseTask() {
        }

        @Override
        protected void runTask() {
            SIPServerTransaction sIPServerTransaction = SIPServerTransaction.this;
            if (sIPServerTransaction.isTerminated()) {
                cancel();
                return;
            }
            this.ticksLeft--;
            if (this.ticksLeft == -1) {
                sIPServerTransaction.fireReliableResponseRetransmissionTimer();
                this.ticksLeft = 2 * this.ticks;
                this.ticks = this.ticksLeft;
                if (this.ticksLeft >= 64) {
                    cancel();
                    SIPServerTransaction.this.setState(SIPTransaction.TERMINATED_STATE);
                    SIPServerTransaction.this.fireTimeoutTimer();
                }
            }
        }
    }

    class ListenerExecutionMaxTimer extends SIPStackTimerTask {
        SIPServerTransaction serverTransaction;

        ListenerExecutionMaxTimer() {
            this.serverTransaction = SIPServerTransaction.this;
        }

        @Override
        protected void runTask() {
            try {
                if (this.serverTransaction.getState() == null) {
                    this.serverTransaction.terminate();
                    SIPTransactionStack sIPStack = this.serverTransaction.getSIPStack();
                    sIPStack.removePendingTransaction(this.serverTransaction);
                    sIPStack.removeTransaction(this.serverTransaction);
                }
            } catch (Exception e) {
                SIPServerTransaction.this.sipStack.getStackLogger().logError("unexpected exception", e);
            }
        }
    }

    class SendTrying extends SIPStackTimerTask {
        protected SendTrying() {
            if (SIPServerTransaction.this.sipStack.isLoggingEnabled()) {
                SIPServerTransaction.this.sipStack.getStackLogger().logDebug("scheduled timer for " + SIPServerTransaction.this);
            }
        }

        @Override
        protected void runTask() {
            SIPServerTransaction sIPServerTransaction = SIPServerTransaction.this;
            TransactionState realState = sIPServerTransaction.getRealState();
            if (realState == null || TransactionState.TRYING == realState) {
                if (SIPServerTransaction.this.sipStack.isLoggingEnabled()) {
                    SIPServerTransaction.this.sipStack.getStackLogger().logDebug(" sending Trying current state = " + sIPServerTransaction.getRealState());
                }
                try {
                    sIPServerTransaction.sendMessage(sIPServerTransaction.getOriginalRequest().createResponse(100, "Trying"));
                    if (SIPServerTransaction.this.sipStack.isLoggingEnabled()) {
                        SIPServerTransaction.this.sipStack.getStackLogger().logDebug(" trying sent " + sIPServerTransaction.getRealState());
                    }
                } catch (IOException e) {
                    if (SIPServerTransaction.this.sipStack.isLoggingEnabled()) {
                        SIPServerTransaction.this.sipStack.getStackLogger().logError("IO error sending  TRYING");
                    }
                }
            }
        }
    }

    class TransactionTimer extends SIPStackTimerTask {
        public TransactionTimer() {
            if (SIPServerTransaction.this.sipStack.isLoggingEnabled()) {
                SIPServerTransaction.this.sipStack.getStackLogger().logDebug("TransactionTimer() : " + SIPServerTransaction.this.getTransactionId());
            }
        }

        @Override
        protected void runTask() {
            if (SIPServerTransaction.this.isTerminated()) {
                try {
                    cancel();
                } catch (IllegalStateException e) {
                    if (!SIPServerTransaction.this.sipStack.isAlive()) {
                        return;
                    }
                }
                SIPServerTransaction.this.sipStack.getTimer().schedule(new SIPTransaction.LingerTimer(), 8000L);
                return;
            }
            SIPServerTransaction.this.fireTimer();
        }
    }

    private void sendResponse(SIPResponse sIPResponse) throws IOException {
        String host;
        try {
            if (isReliable()) {
                getMessageChannel().sendMessage(sIPResponse);
            } else {
                Via topmostVia = sIPResponse.getTopmostVia();
                String transport = topmostVia.getTransport();
                if (transport == null) {
                    throw new IOException("missing transport!");
                }
                int rPort = topmostVia.getRPort();
                if (rPort == -1) {
                    rPort = topmostVia.getPort();
                }
                if (rPort == -1) {
                    if (transport.equalsIgnoreCase(ListeningPoint.TLS)) {
                        rPort = 5061;
                    } else {
                        rPort = 5060;
                    }
                }
                if (topmostVia.getMAddr() != null) {
                    host = topmostVia.getMAddr();
                } else {
                    String parameter = topmostVia.getParameter("received");
                    if (parameter == null) {
                        host = topmostVia.getHost();
                    } else {
                        host = parameter;
                    }
                }
                Hop hopResolveAddress = this.sipStack.addressResolver.resolveAddress(new HopImpl(host, rPort, transport));
                MessageChannel messageChannelCreateRawMessageChannel = getSIPStack().createRawMessageChannel(getSipProvider().getListeningPoint(hopResolveAddress.getTransport()).getIPAddress(), getPort(), hopResolveAddress);
                if (messageChannelCreateRawMessageChannel != null) {
                    messageChannelCreateRawMessageChannel.sendMessage(sIPResponse);
                } else {
                    throw new IOException("Could not create a message channel for " + hopResolveAddress);
                }
            }
        } finally {
            startTransactionTimer();
        }
    }

    protected SIPServerTransaction(SIPTransactionStack sIPTransactionStack, MessageChannel messageChannel) {
        super(sIPTransactionStack, messageChannel);
        this.provisionalResponseSem = new Semaphore(1);
        if (sIPTransactionStack.maxListenerResponseTime != -1) {
            sIPTransactionStack.getTimer().schedule(new ListenerExecutionMaxTimer(), sIPTransactionStack.maxListenerResponseTime * 1000);
        }
        this.rseqNumber = (int) (Math.random() * 1000.0d);
        if (sIPTransactionStack.isLoggingEnabled()) {
            sIPTransactionStack.getStackLogger().logDebug("Creating Server Transaction" + getBranchId());
            sIPTransactionStack.getStackLogger().logStackTrace();
        }
    }

    public void setRequestInterface(ServerRequestInterface serverRequestInterface) {
        this.requestOf = serverRequestInterface;
    }

    public MessageChannel getResponseChannel() {
        return this;
    }

    @Override
    public boolean isMessagePartOfTransaction(SIPMessage sIPMessage) {
        ViaList viaHeaders;
        String method = sIPMessage.getCSeq().getMethod();
        if ((method.equals("INVITE") || !isTerminated()) && (viaHeaders = sIPMessage.getViaHeaders()) != null) {
            Via via = (Via) viaHeaders.getFirst();
            String branch = via.getBranch();
            if (branch != null && !branch.toLowerCase().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE)) {
                branch = null;
            }
            if (branch != null && getBranch() != null) {
                if (method.equals(Request.CANCEL)) {
                    if (getMethod().equals(Request.CANCEL) && getBranch().equalsIgnoreCase(branch) && via.getSentBy().equals(((Via) getOriginalRequest().getViaHeaders().getFirst()).getSentBy())) {
                        return true;
                    }
                } else if (getBranch().equalsIgnoreCase(branch) && via.getSentBy().equals(((Via) getOriginalRequest().getViaHeaders().getFirst()).getSentBy())) {
                    return true;
                }
            } else {
                String str = this.fromTag;
                String tag = sIPMessage.getFrom().getTag();
                boolean z = str == null || tag == null;
                String str2 = this.toTag;
                String tag2 = sIPMessage.getTo().getTag();
                boolean z2 = str2 == null || tag2 == null;
                boolean z3 = sIPMessage instanceof SIPResponse;
                if ((!sIPMessage.getCSeq().getMethod().equalsIgnoreCase(Request.CANCEL) || getOriginalRequest().getCSeq().getMethod().equalsIgnoreCase(Request.CANCEL)) && ((z3 || getOriginalRequest().getRequestURI().equals(((SIPRequest) sIPMessage).getRequestURI())) && ((z || (str != null && str.equalsIgnoreCase(tag))) && ((z2 || (str2 != null && str2.equalsIgnoreCase(tag2))) && getOriginalRequest().getCallId().getCallId().equalsIgnoreCase(sIPMessage.getCallId().getCallId()) && getOriginalRequest().getCSeq().getSeqNumber() == sIPMessage.getCSeq().getSeqNumber() && ((!sIPMessage.getCSeq().getMethod().equals(Request.CANCEL) || getOriginalRequest().getMethod().equals(sIPMessage.getCSeq().getMethod())) && via.equals(getOriginalRequest().getViaHeaders().getFirst())))))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void map() {
        TransactionState realState = getRealState();
        if (realState == null || realState == TransactionState.TRYING) {
            if (isInviteTransaction() && !this.isMapped && this.sipStack.getTimer() != null) {
                this.isMapped = true;
                this.sipStack.getTimer().schedule(new SendTrying(), 200L);
            } else {
                this.isMapped = true;
            }
        }
        this.sipStack.removePendingTransaction(this);
    }

    public boolean isTransactionMapped() {
        return this.isMapped;
    }

    @Override
    public void processRequest(SIPRequest sIPRequest, MessageChannel messageChannel) {
        boolean z;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("processRequest: " + sIPRequest.getFirstLine());
            this.sipStack.getStackLogger().logDebug("tx state = " + getRealState());
        }
        try {
            if (getRealState() == null) {
                setOriginalRequest(sIPRequest);
                setState(TransactionState.TRYING);
                setPassToListener();
                if (isInviteTransaction() && this.isMapped) {
                    sendMessage(sIPRequest.createResponse(100, "Trying"));
                }
                z = true;
            } else {
                if (isInviteTransaction() && TransactionState.COMPLETED == getRealState() && sIPRequest.getMethod().equals("ACK")) {
                    setState(TransactionState.CONFIRMED);
                    disableRetransmissionTimer();
                    if (!isReliable()) {
                        enableTimeoutTimer(this.TIMER_I);
                    } else {
                        setState(TransactionState.TERMINATED);
                    }
                    if (this.sipStack.isNon2XXAckPassedToListener()) {
                        this.requestOf.processRequest(sIPRequest, this);
                        return;
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("ACK received for server Tx " + getTransactionId() + " not delivering to application!");
                    }
                    semRelease();
                    return;
                }
                if (sIPRequest.getMethod().equals(getOriginalRequest().getMethod())) {
                    if (TransactionState.PROCEEDING == getRealState() || TransactionState.COMPLETED == getRealState()) {
                        semRelease();
                        if (this.lastResponse != null) {
                            super.sendMessage(this.lastResponse);
                        }
                    } else if (sIPRequest.getMethod().equals("ACK")) {
                        if (this.requestOf != null) {
                            this.requestOf.processRequest(sIPRequest, this);
                        } else {
                            semRelease();
                        }
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("completed processing retransmitted request : " + sIPRequest.getFirstLine() + this + " txState = " + getState() + " lastResponse = " + getLastResponse());
                        return;
                    }
                    return;
                }
                z = false;
            }
            if (TransactionState.COMPLETED != getRealState() && TransactionState.TERMINATED != getRealState() && this.requestOf != null) {
                if (getOriginalRequest().getMethod().equals(sIPRequest.getMethod())) {
                    if (z) {
                        this.requestOf.processRequest(sIPRequest, this);
                        return;
                    } else {
                        semRelease();
                        return;
                    }
                }
                if (this.requestOf != null) {
                    this.requestOf.processRequest(sIPRequest, this);
                    return;
                } else {
                    semRelease();
                    return;
                }
            }
            getSIPStack();
            if (SIPTransactionStack.isDialogCreated(getOriginalRequest().getMethod()) && getRealState() == TransactionState.TERMINATED && sIPRequest.getMethod().equals("ACK") && this.requestOf != null) {
                SIPDialog sIPDialog = this.dialog;
                if (sIPDialog == null || !sIPDialog.ackProcessed) {
                    if (sIPDialog != null) {
                        sIPDialog.ackReceived(sIPRequest);
                        sIPDialog.ackProcessed = true;
                    }
                    this.requestOf.processRequest(sIPRequest, this);
                } else {
                    semRelease();
                }
            } else if (sIPRequest.getMethod().equals(Request.CANCEL)) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Too late to cancel Transaction");
                }
                semRelease();
                try {
                    sendMessage(sIPRequest.createResponse(Response.OK));
                } catch (IOException e) {
                }
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Dropping request " + getRealState());
            }
        } catch (IOException e2) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logError("IOException ", e2);
            }
            semRelease();
            raiseIOExceptionEvent();
        }
    }

    @Override
    public void sendMessage(SIPMessage sIPMessage) throws IOException {
        try {
            SIPResponse sIPResponse = (SIPResponse) sIPMessage;
            int statusCode = sIPResponse.getStatusCode();
            try {
                if (getOriginalRequest().getTopmostVia().getBranch() != null) {
                    sIPResponse.getTopmostVia().setBranch(getBranch());
                } else {
                    sIPResponse.getTopmostVia().removeParameter("branch");
                }
                if (!getOriginalRequest().getTopmostVia().hasPort()) {
                    sIPResponse.getTopmostVia().removePort();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (!sIPResponse.getCSeq().getMethod().equals(getOriginalRequest().getMethod())) {
                sendResponse(sIPResponse);
                return;
            }
            if (getRealState() == TransactionState.TRYING) {
                if (statusCode / 100 == 1) {
                    setState(TransactionState.PROCEEDING);
                } else if (200 <= statusCode && statusCode <= 699) {
                    if (isInviteTransaction()) {
                        if (statusCode / 100 == 2) {
                            disableRetransmissionTimer();
                            disableTimeoutTimer();
                            this.collectionTime = 64;
                            setState(TransactionState.TERMINATED);
                            if (this.dialog != null) {
                                this.dialog.setRetransmissionTicks();
                            }
                        } else {
                            setState(TransactionState.COMPLETED);
                            if (!isReliable()) {
                                enableRetransmissionTimer();
                            }
                            enableTimeoutTimer(64);
                        }
                    } else if (isReliable()) {
                        setState(TransactionState.TERMINATED);
                    } else {
                        setState(TransactionState.COMPLETED);
                        enableTimeoutTimer(64);
                    }
                }
            } else if (getRealState() == TransactionState.PROCEEDING) {
                if (isInviteTransaction()) {
                    if (statusCode / 100 == 2) {
                        disableRetransmissionTimer();
                        disableTimeoutTimer();
                        this.collectionTime = 64;
                        setState(TransactionState.TERMINATED);
                        if (this.dialog != null) {
                            this.dialog.setRetransmissionTicks();
                        }
                    } else if (300 <= statusCode && statusCode <= 699) {
                        setState(TransactionState.COMPLETED);
                        if (!isReliable()) {
                            enableRetransmissionTimer();
                        }
                        enableTimeoutTimer(64);
                    }
                } else if (200 <= statusCode && statusCode <= 699) {
                    setState(TransactionState.COMPLETED);
                    if (isReliable()) {
                        setState(TransactionState.TERMINATED);
                    } else {
                        disableRetransmissionTimer();
                        enableTimeoutTimer(64);
                    }
                }
            } else if (TransactionState.COMPLETED == getRealState()) {
                return;
            }
            try {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("sendMessage : tx = " + this + " getState = " + getState());
                }
                this.lastResponse = sIPResponse;
                sendResponse(sIPResponse);
            } catch (IOException e2) {
                setState(TransactionState.TERMINATED);
                this.collectionTime = 0;
                throw e2;
            }
        } finally {
            startTransactionTimer();
        }
    }

    @Override
    public String getViaHost() {
        return getMessageChannel().getViaHost();
    }

    @Override
    public int getViaPort() {
        return getMessageChannel().getViaPort();
    }

    @Override
    protected void fireRetransmissionTimer() {
        try {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("fireRetransmissionTimer() -- ");
            }
            if (isInviteTransaction() && this.lastResponse != null) {
                if (this.retransmissionAlertEnabled && !this.sipStack.isTransactionPendingAck(this)) {
                    SipProviderImpl sipProvider = getSipProvider();
                    sipProvider.handleEvent(new TimeoutEvent(sipProvider, this, Timeout.RETRANSMIT), this);
                    return;
                }
                if (this.lastResponse.getStatusCode() / 100 > 2 && !this.isAckSeen) {
                    super.sendMessage(this.lastResponse);
                }
            }
        } catch (IOException e) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logException(e);
            }
            raiseErrorEvent(2);
        }
    }

    private void fireReliableResponseRetransmissionTimer() {
        try {
            super.sendMessage(this.pendingReliableResponse);
        } catch (IOException e) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logException(e);
            }
            setState(TransactionState.TERMINATED);
            raiseErrorEvent(2);
        }
    }

    @Override
    protected void fireTimeoutTimer() {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("SIPServerTransaction.fireTimeoutTimer this = " + this + " current state = " + getRealState() + " method = " + getOriginalRequest().getMethod());
        }
        if (getMethod().equals("INVITE") && this.sipStack.removeTransactionPendingAck(this)) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Found tx pending ACK - returning");
                return;
            }
            return;
        }
        SIPDialog sIPDialog = this.dialog;
        getSIPStack();
        if (SIPTransactionStack.isDialogCreated(getOriginalRequest().getMethod()) && (TransactionState.CALLING == getRealState() || TransactionState.TRYING == getRealState())) {
            sIPDialog.setState(SIPDialog.TERMINATED_STATE);
        } else if (getOriginalRequest().getMethod().equals("BYE") && sIPDialog != null && sIPDialog.isTerminatedOnBye()) {
            sIPDialog.setState(SIPDialog.TERMINATED_STATE);
        }
        if (TransactionState.COMPLETED == getRealState() && isInviteTransaction()) {
            raiseErrorEvent(1);
            setState(TransactionState.TERMINATED);
            this.sipStack.removeTransaction(this);
            return;
        }
        if (TransactionState.COMPLETED == getRealState() && !isInviteTransaction()) {
            setState(TransactionState.TERMINATED);
            this.sipStack.removeTransaction(this);
            return;
        }
        if (TransactionState.CONFIRMED == getRealState() && isInviteTransaction()) {
            setState(TransactionState.TERMINATED);
            this.sipStack.removeTransaction(this);
            return;
        }
        if (!isInviteTransaction() && (TransactionState.COMPLETED == getRealState() || TransactionState.CONFIRMED == getRealState())) {
            setState(TransactionState.TERMINATED);
            return;
        }
        if (isInviteTransaction() && TransactionState.TERMINATED == getRealState()) {
            raiseErrorEvent(1);
            if (sIPDialog != null) {
                sIPDialog.setState(SIPDialog.TERMINATED_STATE);
            }
        }
    }

    @Override
    public SIPResponse getLastResponse() {
        return this.lastResponse;
    }

    @Override
    public void setOriginalRequest(SIPRequest sIPRequest) {
        super.setOriginalRequest(sIPRequest);
    }

    @Override
    public void sendResponse(Response response) throws SipException {
        SIPResponse sIPResponse = (SIPResponse) response;
        SIPDialog sIPDialog = this.dialog;
        if (response == 0) {
            throw new NullPointerException("null response");
        }
        try {
            sIPResponse.checkHeaders();
            if (!sIPResponse.getCSeq().getMethod().equals(getMethod())) {
                throw new SipException("CSeq method does not match Request method of request that created the tx.");
            }
            if (getMethod().equals("SUBSCRIBE") && response.getStatusCode() / 100 == 2) {
                if (response.getHeader("Expires") == null) {
                    throw new SipException("Expires header is mandatory in 2xx response of SUBSCRIBE");
                }
                Expires expires = (Expires) getOriginalRequest().getExpires();
                Expires expires2 = (Expires) response.getExpires();
                if (expires != null && expires2.getExpires() > expires.getExpires()) {
                    throw new SipException("Response Expires time exceeds request Expires time : See RFC 3265 3.1.1");
                }
            }
            if (sIPResponse.getStatusCode() == 200 && sIPResponse.getCSeq().getMethod().equals("INVITE") && sIPResponse.getHeader("Contact") == null) {
                throw new SipException("Contact Header is mandatory for the OK to the INVITE");
            }
            if (!isMessagePartOfTransaction((SIPMessage) response)) {
                throw new SipException("Response does not belong to this transaction.");
            }
            try {
                if (this.pendingReliableResponse != null && getDialog() != null && getState() != TransactionState.TERMINATED && ((SIPResponse) response).getContentTypeHeader() != null && response.getStatusCode() / 100 == 2 && ((SIPResponse) response).getContentTypeHeader().getContentType().equalsIgnoreCase("application") && ((SIPResponse) response).getContentTypeHeader().getContentSubType().equalsIgnoreCase("sdp")) {
                    try {
                        if (!this.provisionalResponseSem.tryAcquire(1L, TimeUnit.SECONDS)) {
                            throw new SipException("cannot send response -- unacked povisional");
                        }
                    } catch (Exception e) {
                        this.sipStack.getStackLogger().logError("Could not acquire PRACK sem ", e);
                    }
                } else if (this.pendingReliableResponse != null && sIPResponse.isFinalResponse()) {
                    this.provisionalResponseTask.cancel();
                    this.provisionalResponseTask = null;
                }
                if (sIPDialog != null) {
                    if (sIPResponse.getStatusCode() / 100 == 2) {
                        SIPTransactionStack sIPTransactionStack = this.sipStack;
                        if (SIPTransactionStack.isDialogCreated(sIPResponse.getCSeq().getMethod())) {
                            if (sIPDialog.getLocalTag() == null && sIPResponse.getTo().getTag() == null) {
                                sIPResponse.getTo().setTag(Utils.getInstance().generateTag());
                            } else if (sIPDialog.getLocalTag() != null && sIPResponse.getToTag() == null) {
                                sIPResponse.setToTag(sIPDialog.getLocalTag());
                            } else if (sIPDialog.getLocalTag() != null && sIPResponse.getToTag() != null && !sIPDialog.getLocalTag().equals(sIPResponse.getToTag())) {
                                throw new SipException("Tag mismatch dialogTag is " + sIPDialog.getLocalTag() + " responseTag is " + sIPResponse.getToTag());
                            }
                        }
                    }
                    if (!sIPResponse.getCallId().getCallId().equals(sIPDialog.getCallId().getCallId())) {
                        throw new SipException("Dialog mismatch!");
                    }
                }
                String tag = ((SIPRequest) getRequest()).getFrom().getTag();
                if (tag != null && sIPResponse.getFromTag() != null && !sIPResponse.getFromTag().equals(tag)) {
                    throw new SipException("From tag of request does not match response from tag");
                }
                if (tag != null) {
                    sIPResponse.getFrom().setTag(tag);
                } else if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("WARNING -- Null From tag in request!!");
                }
                if (sIPDialog != null && response.getStatusCode() != 100) {
                    sIPDialog.setResponseTags(sIPResponse);
                    DialogState state = sIPDialog.getState();
                    sIPDialog.setLastResponse(this, (SIPResponse) response);
                    if (state == null && sIPDialog.getState() == DialogState.TERMINATED) {
                        sIPDialog.getSipProvider().handleEvent(new DialogTerminatedEvent(sIPDialog.getSipProvider(), sIPDialog), this);
                    }
                } else if (sIPDialog == null && getMethod().equals("INVITE") && this.retransmissionAlertEnabled && this.retransmissionAlertTimerTask == null && response.getStatusCode() / 100 == 2) {
                    String dialogId = ((SIPResponse) response).getDialogId(true);
                    this.retransmissionAlertTimerTask = new RetransmissionAlertTimerTask(dialogId);
                    this.sipStack.retransmissionAlertTransactions.put(dialogId, this);
                    this.sipStack.getTimer().schedule(this.retransmissionAlertTimerTask, 0L, 500L);
                }
                sendMessage((SIPResponse) response);
                if (sIPDialog != null) {
                    sIPDialog.startRetransmitTimer(this, (SIPResponse) response);
                }
            } catch (IOException e2) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logException(e2);
                }
                setState(TransactionState.TERMINATED);
                raiseErrorEvent(2);
                throw new SipException(e2.getMessage());
            } catch (ParseException e3) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logException(e3);
                }
                setState(TransactionState.TERMINATED);
                throw new SipException(e3.getMessage());
            }
        } catch (ParseException e4) {
            throw new SipException(e4.getMessage());
        }
    }

    private TransactionState getRealState() {
        return super.getState();
    }

    @Override
    public TransactionState getState() {
        if (isInviteTransaction() && TransactionState.TRYING == super.getState()) {
            return TransactionState.PROCEEDING;
        }
        return super.getState();
    }

    @Override
    public void setState(TransactionState transactionState) {
        if (transactionState == TransactionState.TERMINATED && isReliable() && !getSIPStack().cacheServerConnections) {
            this.collectionTime = 64;
        }
        super.setState(transactionState);
    }

    @Override
    protected void startTransactionTimer() {
        if (this.transactionTimerStarted.compareAndSet(false, true) && this.sipStack.getTimer() != null) {
            this.sipStack.getTimer().schedule(new TransactionTimer(), this.BASE_TIMER_INTERVAL, this.BASE_TIMER_INTERVAL);
        }
    }

    public boolean equals(Object obj) {
        if (!obj.getClass().equals(getClass())) {
            return false;
        }
        return getBranch().equalsIgnoreCase(((SIPServerTransaction) obj).getBranch());
    }

    @Override
    public Dialog getDialog() {
        return this.dialog;
    }

    @Override
    public void setDialog(SIPDialog sIPDialog, String str) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("setDialog " + this + " dialog = " + sIPDialog);
        }
        this.dialog = sIPDialog;
        if (str != null) {
            this.dialog.setAssigned();
        }
        if (this.retransmissionAlertEnabled && this.retransmissionAlertTimerTask != null) {
            this.retransmissionAlertTimerTask.cancel();
            if (this.retransmissionAlertTimerTask.dialogId != null) {
                this.sipStack.retransmissionAlertTransactions.remove(this.retransmissionAlertTimerTask.dialogId);
            }
            this.retransmissionAlertTimerTask = null;
        }
        this.retransmissionAlertEnabled = false;
    }

    @Override
    public void terminate() throws ObjectInUseException {
        setState(TransactionState.TERMINATED);
        if (this.retransmissionAlertTimerTask != null) {
            this.retransmissionAlertTimerTask.cancel();
            if (this.retransmissionAlertTimerTask.dialogId != null) {
                this.sipStack.retransmissionAlertTransactions.remove(this.retransmissionAlertTimerTask.dialogId);
            }
            this.retransmissionAlertTimerTask = null;
        }
    }

    protected void sendReliableProvisionalResponse(Response response) throws SipException {
        if (this.pendingReliableResponse != null) {
            throw new SipException("Unacknowledged response");
        }
        this.pendingReliableResponse = (SIPResponse) response;
        RSeq rSeq = (RSeq) response.getHeader("RSeq");
        if (response.getHeader("RSeq") == null) {
            rSeq = new RSeq();
            response.setHeader(rSeq);
        }
        try {
            this.rseqNumber++;
            rSeq.setSeqNumber(this.rseqNumber);
            this.lastResponse = (SIPResponse) response;
            if (getDialog() != null && !this.provisionalResponseSem.tryAcquire(1L, TimeUnit.SECONDS)) {
                throw new SipException("Unacknowledged response");
            }
            sendMessage((SIPMessage) response);
            this.provisionalResponseTask = new ProvisionalResponseTask();
            this.sipStack.getTimer().schedule(this.provisionalResponseTask, 0L, 500L);
        } catch (Exception e) {
            InternalErrorHandler.handleException(e);
        }
    }

    public SIPResponse getReliableProvisionalResponse() {
        return this.pendingReliableResponse;
    }

    public boolean prackRecieved() {
        if (this.pendingReliableResponse == null) {
            return false;
        }
        if (this.provisionalResponseTask != null) {
            this.provisionalResponseTask.cancel();
        }
        this.pendingReliableResponse = null;
        this.provisionalResponseSem.release();
        return true;
    }

    @Override
    public void enableRetransmissionAlerts() throws SipException {
        if (getDialog() != null) {
            throw new SipException("Dialog associated with tx");
        }
        if (!getMethod().equals("INVITE")) {
            throw new SipException("Request Method must be INVITE");
        }
        this.retransmissionAlertEnabled = true;
    }

    public boolean isRetransmissionAlertEnabled() {
        return this.retransmissionAlertEnabled;
    }

    public void disableRetransmissionAlerts() {
        if (this.retransmissionAlertTimerTask != null && this.retransmissionAlertEnabled) {
            this.retransmissionAlertTimerTask.cancel();
            this.retransmissionAlertEnabled = false;
            String str = this.retransmissionAlertTimerTask.dialogId;
            if (str != null) {
                this.sipStack.retransmissionAlertTransactions.remove(str);
            }
            this.retransmissionAlertTimerTask = null;
        }
    }

    public void setAckSeen() {
        this.isAckSeen = true;
    }

    public boolean ackSeen() {
        return this.isAckSeen;
    }

    public void setMapped(boolean z) {
        this.isMapped = true;
    }

    public void setPendingSubscribe(SIPClientTransaction sIPClientTransaction) {
        this.pendingSubscribeTransaction = sIPClientTransaction;
    }

    @Override
    public void releaseSem() {
        if (this.pendingSubscribeTransaction != null) {
            this.pendingSubscribeTransaction.releaseSem();
        } else if (this.inviteTransaction != null && getMethod().equals(Request.CANCEL)) {
            this.inviteTransaction.releaseSem();
        }
        super.releaseSem();
    }

    public void setInviteTransaction(SIPServerTransaction sIPServerTransaction) {
        this.inviteTransaction = sIPServerTransaction;
    }

    @Override
    public SIPServerTransaction getCanceledInviteTransaction() {
        return this.inviteTransaction;
    }

    public void scheduleAckRemoval() throws IllegalStateException {
        if (getMethod() == null || !getMethod().equals("ACK")) {
            StringBuilder sb = new StringBuilder();
            sb.append("Method is null[");
            sb.append(getMethod() == null);
            sb.append("] or method is not ACK[");
            sb.append(getMethod());
            sb.append("]");
            throw new IllegalStateException(sb.toString());
        }
        startTransactionTimer();
    }
}
