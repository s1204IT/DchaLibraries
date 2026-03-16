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
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
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

        public RetransmissionAlertTimerTask(String dialogId) {
        }

        @Override
        protected void runTask() {
            SIPServerTransaction serverTransaction = SIPServerTransaction.this;
            this.ticksLeft--;
            if (this.ticksLeft == -1) {
                serverTransaction.fireRetransmissionTimer();
                this.ticksLeft = this.ticks * 2;
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
            SIPServerTransaction serverTransaction = SIPServerTransaction.this;
            if (serverTransaction.isTerminated()) {
                cancel();
                return;
            }
            this.ticksLeft--;
            if (this.ticksLeft == -1) {
                serverTransaction.fireReliableResponseRetransmissionTimer();
                this.ticksLeft = this.ticks * 2;
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
                    SIPTransactionStack sipStack = this.serverTransaction.getSIPStack();
                    sipStack.removePendingTransaction(this.serverTransaction);
                    sipStack.removeTransaction(this.serverTransaction);
                }
            } catch (Exception ex) {
                SIPServerTransaction.this.sipStack.getStackLogger().logError("unexpected exception", ex);
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
            SIPServerTransaction serverTransaction = SIPServerTransaction.this;
            TransactionState realState = serverTransaction.getRealState();
            if (realState == null || TransactionState.TRYING == realState) {
                if (SIPServerTransaction.this.sipStack.isLoggingEnabled()) {
                    SIPServerTransaction.this.sipStack.getStackLogger().logDebug(" sending Trying current state = " + serverTransaction.getRealState());
                }
                try {
                    serverTransaction.sendMessage(serverTransaction.getOriginalRequest().createResponse(100, "Trying"));
                    if (SIPServerTransaction.this.sipStack.isLoggingEnabled()) {
                        SIPServerTransaction.this.sipStack.getStackLogger().logDebug(" trying sent " + serverTransaction.getRealState());
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
                TimerTask myTimer = new SIPTransaction.LingerTimer();
                SIPServerTransaction.this.sipStack.getTimer().schedule(myTimer, 8000L);
                return;
            }
            SIPServerTransaction.this.fireTimer();
        }
    }

    private void sendResponse(SIPResponse transactionResponse) throws IOException {
        String host;
        try {
            if (isReliable()) {
                getMessageChannel().sendMessage(transactionResponse);
            } else {
                Via via = transactionResponse.getTopmostVia();
                String transport = via.getTransport();
                if (transport == null) {
                    throw new IOException("missing transport!");
                }
                int port = via.getRPort();
                if (port == -1) {
                    port = via.getPort();
                }
                if (port == -1) {
                    if (transport.equalsIgnoreCase("TLS")) {
                        port = 5061;
                    } else {
                        port = 5060;
                    }
                }
                if (via.getMAddr() != null) {
                    host = via.getMAddr();
                } else {
                    host = via.getParameter("received");
                    if (host == null) {
                        host = via.getHost();
                    }
                }
                Hop hop = this.sipStack.addressResolver.resolveAddress(new HopImpl(host, port, transport));
                MessageChannel messageChannel = getSIPStack().createRawMessageChannel(getSipProvider().getListeningPoint(hop.getTransport()).getIPAddress(), getPort(), hop);
                if (messageChannel != null) {
                    messageChannel.sendMessage(transactionResponse);
                } else {
                    throw new IOException("Could not create a message channel for " + hop);
                }
            }
        } finally {
            startTransactionTimer();
        }
    }

    protected SIPServerTransaction(SIPTransactionStack sipStack, MessageChannel newChannelToUse) {
        super(sipStack, newChannelToUse);
        this.provisionalResponseSem = new Semaphore(1);
        if (sipStack.maxListenerResponseTime != -1) {
            sipStack.getTimer().schedule(new ListenerExecutionMaxTimer(), sipStack.maxListenerResponseTime * 1000);
        }
        this.rseqNumber = (int) (Math.random() * 1000.0d);
        if (sipStack.isLoggingEnabled()) {
            sipStack.getStackLogger().logDebug("Creating Server Transaction" + getBranchId());
            sipStack.getStackLogger().logStackTrace();
        }
    }

    public void setRequestInterface(ServerRequestInterface newRequestOf) {
        this.requestOf = newRequestOf;
    }

    public MessageChannel getResponseChannel() {
        return this;
    }

    @Override
    public boolean isMessagePartOfTransaction(SIPMessage messageToTest) {
        ViaList viaHeaders;
        String method = messageToTest.getCSeq().getMethod();
        if ((!method.equals("INVITE") && isTerminated()) || (viaHeaders = messageToTest.getViaHeaders()) == null) {
            return false;
        }
        Via topViaHeader = (Via) viaHeaders.getFirst();
        String messageBranch = topViaHeader.getBranch();
        if (messageBranch != null && !messageBranch.toLowerCase().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE)) {
            messageBranch = null;
        }
        if (messageBranch != null && getBranch() != null) {
            return method.equals(Request.CANCEL) ? getMethod().equals(Request.CANCEL) && getBranch().equalsIgnoreCase(messageBranch) && topViaHeader.getSentBy().equals(((Via) getOriginalRequest().getViaHeaders().getFirst()).getSentBy()) : getBranch().equalsIgnoreCase(messageBranch) && topViaHeader.getSentBy().equals(((Via) getOriginalRequest().getViaHeaders().getFirst()).getSentBy());
        }
        String originalFromTag = this.fromTag;
        String thisFromTag = messageToTest.getFrom().getTag();
        boolean skipFrom = originalFromTag == null || thisFromTag == null;
        String originalToTag = this.toTag;
        String thisToTag = messageToTest.getTo().getTag();
        boolean skipTo = originalToTag == null || thisToTag == null;
        boolean isResponse = messageToTest instanceof SIPResponse;
        if (messageToTest.getCSeq().getMethod().equalsIgnoreCase(Request.CANCEL) && !getOriginalRequest().getCSeq().getMethod().equalsIgnoreCase(Request.CANCEL)) {
            return false;
        }
        if (!isResponse && !getOriginalRequest().getRequestURI().equals(((SIPRequest) messageToTest).getRequestURI())) {
            return false;
        }
        if (!skipFrom && (originalFromTag == null || !originalFromTag.equalsIgnoreCase(thisFromTag))) {
            return false;
        }
        if ((!skipTo && (originalToTag == null || !originalToTag.equalsIgnoreCase(thisToTag))) || !getOriginalRequest().getCallId().getCallId().equalsIgnoreCase(messageToTest.getCallId().getCallId()) || getOriginalRequest().getCSeq().getSeqNumber() != messageToTest.getCSeq().getSeqNumber()) {
            return false;
        }
        if ((messageToTest.getCSeq().getMethod().equals(Request.CANCEL) && !getOriginalRequest().getMethod().equals(messageToTest.getCSeq().getMethod())) || !topViaHeader.equals(getOriginalRequest().getViaHeaders().getFirst())) {
            return false;
        }
        return true;
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
    public void processRequest(SIPRequest transactionRequest, MessageChannel sourceChannel) {
        boolean toTu = false;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("processRequest: " + transactionRequest.getFirstLine());
            this.sipStack.getStackLogger().logDebug("tx state = " + getRealState());
        }
        try {
            if (getRealState() == null) {
                setOriginalRequest(transactionRequest);
                setState(TransactionState.TRYING);
                toTu = true;
                setPassToListener();
                if (isInviteTransaction() && this.isMapped) {
                    sendMessage(transactionRequest.createResponse(100, "Trying"));
                }
            } else {
                if (isInviteTransaction() && TransactionState.COMPLETED == getRealState() && transactionRequest.getMethod().equals("ACK")) {
                    setState(TransactionState.CONFIRMED);
                    disableRetransmissionTimer();
                    if (!isReliable()) {
                        enableTimeoutTimer(this.TIMER_I);
                    } else {
                        setState(TransactionState.TERMINATED);
                    }
                    if (this.sipStack.isNon2XXAckPassedToListener()) {
                        this.requestOf.processRequest(transactionRequest, this);
                        return;
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("ACK received for server Tx " + getTransactionId() + " not delivering to application!");
                    }
                    semRelease();
                    return;
                }
                if (transactionRequest.getMethod().equals(getOriginalRequest().getMethod())) {
                    if (TransactionState.PROCEEDING == getRealState() || TransactionState.COMPLETED == getRealState()) {
                        semRelease();
                        if (this.lastResponse != null) {
                            super.sendMessage(this.lastResponse);
                        }
                    } else if (transactionRequest.getMethod().equals("ACK")) {
                        if (this.requestOf != null) {
                            this.requestOf.processRequest(transactionRequest, this);
                        } else {
                            semRelease();
                        }
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("completed processing retransmitted request : " + transactionRequest.getFirstLine() + this + " txState = " + getState() + " lastResponse = " + getLastResponse());
                        return;
                    }
                    return;
                }
            }
            if (TransactionState.COMPLETED != getRealState() && TransactionState.TERMINATED != getRealState() && this.requestOf != null) {
                if (getOriginalRequest().getMethod().equals(transactionRequest.getMethod())) {
                    if (toTu) {
                        this.requestOf.processRequest(transactionRequest, this);
                        return;
                    } else {
                        semRelease();
                        return;
                    }
                }
                if (this.requestOf != null) {
                    this.requestOf.processRequest(transactionRequest, this);
                    return;
                } else {
                    semRelease();
                    return;
                }
            }
            getSIPStack();
            if (SIPTransactionStack.isDialogCreated(getOriginalRequest().getMethod()) && getRealState() == TransactionState.TERMINATED && transactionRequest.getMethod().equals("ACK") && this.requestOf != null) {
                SIPDialog thisDialog = this.dialog;
                if (thisDialog == null || !thisDialog.ackProcessed) {
                    if (thisDialog != null) {
                        thisDialog.ackReceived(transactionRequest);
                        thisDialog.ackProcessed = true;
                    }
                    this.requestOf.processRequest(transactionRequest, this);
                } else {
                    semRelease();
                }
            } else if (transactionRequest.getMethod().equals(Request.CANCEL)) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Too late to cancel Transaction");
                }
                semRelease();
                try {
                    sendMessage(transactionRequest.createResponse(200));
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
    public void sendMessage(SIPMessage messageToSend) throws IOException {
        try {
            SIPResponse transactionResponse = (SIPResponse) messageToSend;
            int statusCode = transactionResponse.getStatusCode();
            try {
                if (getOriginalRequest().getTopmostVia().getBranch() != null) {
                    transactionResponse.getTopmostVia().setBranch(getBranch());
                } else {
                    transactionResponse.getTopmostVia().removeParameter("branch");
                }
                if (!getOriginalRequest().getTopmostVia().hasPort()) {
                    transactionResponse.getTopmostVia().removePort();
                }
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
            if (!transactionResponse.getCSeq().getMethod().equals(getOriginalRequest().getMethod())) {
                sendResponse(transactionResponse);
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
                this.lastResponse = transactionResponse;
                sendResponse(transactionResponse);
            } catch (IOException e) {
                setState(TransactionState.TERMINATED);
                this.collectionTime = 0;
                throw e;
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
                if (!this.retransmissionAlertEnabled || this.sipStack.isTransactionPendingAck(this)) {
                    if (this.lastResponse.getStatusCode() / 100 > 2 && !this.isAckSeen) {
                        super.sendMessage(this.lastResponse);
                        return;
                    }
                    return;
                }
                SipProviderImpl sipProvider = getSipProvider();
                TimeoutEvent txTimeout = new TimeoutEvent(sipProvider, this, Timeout.RETRANSMIT);
                sipProvider.handleEvent(txTimeout, this);
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
        SIPDialog dialog = this.dialog;
        getSIPStack();
        if (SIPTransactionStack.isDialogCreated(getOriginalRequest().getMethod()) && (TransactionState.CALLING == getRealState() || TransactionState.TRYING == getRealState())) {
            dialog.setState(SIPDialog.TERMINATED_STATE);
        } else if (getOriginalRequest().getMethod().equals("BYE") && dialog != null && dialog.isTerminatedOnBye()) {
            dialog.setState(SIPDialog.TERMINATED_STATE);
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
            if (dialog != null) {
                dialog.setState(SIPDialog.TERMINATED_STATE);
            }
        }
    }

    @Override
    public SIPResponse getLastResponse() {
        return this.lastResponse;
    }

    @Override
    public void setOriginalRequest(SIPRequest originalRequest) {
        super.setOriginalRequest(originalRequest);
    }

    @Override
    public void sendResponse(Response response) throws SipException {
        SIPResponse sipResponse = (SIPResponse) response;
        SIPDialog dialog = this.dialog;
        if (response == 0) {
            throw new NullPointerException("null response");
        }
        try {
            sipResponse.checkHeaders();
            if (!sipResponse.getCSeq().getMethod().equals(getMethod())) {
                throw new SipException("CSeq method does not match Request method of request that created the tx.");
            }
            if (getMethod().equals("SUBSCRIBE") && response.getStatusCode() / 100 == 2) {
                if (response.getHeader("Expires") == null) {
                    throw new SipException("Expires header is mandatory in 2xx response of SUBSCRIBE");
                }
                Expires requestExpires = (Expires) getOriginalRequest().getExpires();
                Expires responseExpires = (Expires) response.getExpires();
                if (requestExpires != null && responseExpires.getExpires() > requestExpires.getExpires()) {
                    throw new SipException("Response Expires time exceeds request Expires time : See RFC 3265 3.1.1");
                }
            }
            if (sipResponse.getStatusCode() == 200 && sipResponse.getCSeq().getMethod().equals("INVITE") && sipResponse.getHeader("Contact") == null) {
                throw new SipException("Contact Header is mandatory for the OK to the INVITE");
            }
            if (!isMessagePartOfTransaction((SIPMessage) response)) {
                throw new SipException("Response does not belong to this transaction.");
            }
            try {
                if (this.pendingReliableResponse != null && getDialog() != null && getState() != TransactionState.TERMINATED && ((SIPResponse) response).getContentTypeHeader() != null && response.getStatusCode() / 100 == 2 && ((SIPResponse) response).getContentTypeHeader().getContentType().equalsIgnoreCase("application") && ((SIPResponse) response).getContentTypeHeader().getContentSubType().equalsIgnoreCase("sdp")) {
                    try {
                        boolean acquired = this.provisionalResponseSem.tryAcquire(1L, TimeUnit.SECONDS);
                        if (!acquired) {
                            throw new SipException("cannot send response -- unacked povisional");
                        }
                    } catch (Exception ex) {
                        this.sipStack.getStackLogger().logError("Could not acquire PRACK sem ", ex);
                    }
                } else if (this.pendingReliableResponse != null && sipResponse.isFinalResponse()) {
                    this.provisionalResponseTask.cancel();
                    this.provisionalResponseTask = null;
                }
                if (dialog != null) {
                    if (sipResponse.getStatusCode() / 100 == 2) {
                        SIPTransactionStack sIPTransactionStack = this.sipStack;
                        if (SIPTransactionStack.isDialogCreated(sipResponse.getCSeq().getMethod())) {
                            if (dialog.getLocalTag() == null && sipResponse.getTo().getTag() == null) {
                                sipResponse.getTo().setTag(Utils.getInstance().generateTag());
                            } else if (dialog.getLocalTag() != null && sipResponse.getToTag() == null) {
                                sipResponse.setToTag(dialog.getLocalTag());
                            } else if (dialog.getLocalTag() != null && sipResponse.getToTag() != null && !dialog.getLocalTag().equals(sipResponse.getToTag())) {
                                throw new SipException("Tag mismatch dialogTag is " + dialog.getLocalTag() + " responseTag is " + sipResponse.getToTag());
                            }
                        }
                    }
                    if (!sipResponse.getCallId().getCallId().equals(dialog.getCallId().getCallId())) {
                        throw new SipException("Dialog mismatch!");
                    }
                }
                String fromTag = ((SIPRequest) getRequest()).getFrom().getTag();
                if (fromTag != null && sipResponse.getFromTag() != null && !sipResponse.getFromTag().equals(fromTag)) {
                    throw new SipException("From tag of request does not match response from tag");
                }
                if (fromTag != null) {
                    sipResponse.getFrom().setTag(fromTag);
                } else if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("WARNING -- Null From tag in request!!");
                }
                if (dialog != null && response.getStatusCode() != 100) {
                    dialog.setResponseTags(sipResponse);
                    DialogState oldState = dialog.getState();
                    dialog.setLastResponse(this, (SIPResponse) response);
                    if (oldState == null && dialog.getState() == DialogState.TERMINATED) {
                        DialogTerminatedEvent event = new DialogTerminatedEvent(dialog.getSipProvider(), dialog);
                        dialog.getSipProvider().handleEvent(event, this);
                    }
                } else if (dialog == null && getMethod().equals("INVITE") && this.retransmissionAlertEnabled && this.retransmissionAlertTimerTask == null && response.getStatusCode() / 100 == 2) {
                    String dialogId = ((SIPResponse) response).getDialogId(true);
                    this.retransmissionAlertTimerTask = new RetransmissionAlertTimerTask(dialogId);
                    this.sipStack.retransmissionAlertTransactions.put(dialogId, this);
                    this.sipStack.getTimer().schedule(this.retransmissionAlertTimerTask, 0L, 500L);
                }
                sendMessage((SIPResponse) response);
                if (dialog != null) {
                    Response response2 = (SIPResponse) response;
                    dialog.startRetransmitTimer(this, response2);
                }
            } catch (IOException ex2) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logException(ex2);
                }
                setState(TransactionState.TERMINATED);
                raiseErrorEvent(2);
                throw new SipException(ex2.getMessage());
            } catch (ParseException ex1) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logException(ex1);
                }
                setState(TransactionState.TERMINATED);
                throw new SipException(ex1.getMessage());
            }
        } catch (ParseException ex3) {
            throw new SipException(ex3.getMessage());
        }
    }

    private TransactionState getRealState() {
        return super.getState();
    }

    @Override
    public TransactionState getState() {
        return (isInviteTransaction() && TransactionState.TRYING == super.getState()) ? TransactionState.PROCEEDING : super.getState();
    }

    @Override
    public void setState(TransactionState newState) {
        if (newState == TransactionState.TERMINATED && isReliable() && !getSIPStack().cacheServerConnections) {
            this.collectionTime = 64;
        }
        super.setState(newState);
    }

    @Override
    protected void startTransactionTimer() {
        if (this.transactionTimerStarted.compareAndSet(false, true) && this.sipStack.getTimer() != null) {
            TimerTask myTimer = new TransactionTimer();
            this.sipStack.getTimer().schedule(myTimer, this.BASE_TIMER_INTERVAL, this.BASE_TIMER_INTERVAL);
        }
    }

    public boolean equals(Object other) {
        if (!other.getClass().equals(getClass())) {
            return false;
        }
        SIPServerTransaction sst = (SIPServerTransaction) other;
        return getBranch().equalsIgnoreCase(sst.getBranch());
    }

    @Override
    public Dialog getDialog() {
        return this.dialog;
    }

    @Override
    public void setDialog(SIPDialog sipDialog, String dialogId) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("setDialog " + this + " dialog = " + sipDialog);
        }
        this.dialog = sipDialog;
        if (dialogId != null) {
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
        RSeq rseq = (RSeq) response.getHeader("RSeq");
        if (response.getHeader("RSeq") == null) {
            rseq = new RSeq();
            response.setHeader(rseq);
        }
        try {
            this.rseqNumber++;
            rseq.setSeqNumber(this.rseqNumber);
            this.lastResponse = (SIPResponse) response;
            if (getDialog() != null) {
                boolean acquired = this.provisionalResponseSem.tryAcquire(1L, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new SipException("Unacknowledged response");
                }
            }
            sendMessage((SIPMessage) response);
            this.provisionalResponseTask = new ProvisionalResponseTask();
            this.sipStack.getTimer().schedule(this.provisionalResponseTask, 0L, 500L);
        } catch (Exception ex) {
            InternalErrorHandler.handleException(ex);
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
            String dialogId = this.retransmissionAlertTimerTask.dialogId;
            if (dialogId != null) {
                this.sipStack.retransmissionAlertTransactions.remove(dialogId);
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

    public void setMapped(boolean b) {
        this.isMapped = true;
    }

    public void setPendingSubscribe(SIPClientTransaction pendingSubscribeClientTx) {
        this.pendingSubscribeTransaction = pendingSubscribeClientTx;
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

    public void setInviteTransaction(SIPServerTransaction st) {
        this.inviteTransaction = st;
    }

    @Override
    public SIPServerTransaction getCanceledInviteTransaction() {
        return this.inviteTransaction;
    }

    public void scheduleAckRemoval() throws IllegalStateException {
        if (getMethod() == null || !getMethod().equals("ACK")) {
            throw new IllegalStateException("Method is null[" + (getMethod() == null) + "] or method is not ACK[" + getMethod() + "]");
        }
        startTransactionTimer();
    }
}
