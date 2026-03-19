package gov.nist.javax.sip.stack;

import gov.nist.core.Host;
import gov.nist.core.HostPort;
import gov.nist.core.Separators;
import gov.nist.core.ServerLogger;
import gov.nist.core.StackLogger;
import gov.nist.core.ThreadAuditor;
import gov.nist.core.net.AddressResolver;
import gov.nist.core.net.DefaultNetworkLayer;
import gov.nist.core.net.NetworkLayer;
import gov.nist.javax.sip.DefaultAddressResolver;
import gov.nist.javax.sip.ListeningPointImpl;
import gov.nist.javax.sip.LogRecordFactory;
import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.SipListenerExt;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.ParameterNames;
import gov.nist.javax.sip.header.Event;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.extensions.JoinHeader;
import gov.nist.javax.sip.header.extensions.ReplacesHeader;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Hop;
import javax.sip.address.Router;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;

public abstract class SIPTransactionStack implements SIPTransactionEventListener, SIPDialogEventListener {
    public static final int BASE_TIMER_INTERVAL = 500;
    public static final int CONNECTION_LINGER_TIME = 8;
    protected static final Set<String> dialogCreatingMethods = new HashSet();
    private AtomicInteger activeClientTransactionCount;
    protected AddressResolver addressResolver;
    protected boolean cacheClientConnections;
    protected boolean cacheServerConnections;
    protected boolean cancelClientTransactionChecked;
    protected boolean checkBranchId;
    private ConcurrentHashMap<String, SIPClientTransaction> clientTransactionTable;
    protected int clientTransactionTableHiwaterMark;
    protected int clientTransactionTableLowaterMark;
    protected DefaultRouter defaultRouter;
    protected ConcurrentHashMap<String, SIPDialog> dialogTable;
    protected ConcurrentHashMap<String, SIPDialog> earlyDialogTable;
    private ConcurrentHashMap<String, SIPClientTransaction> forkedClientTransactionTable;
    protected HashSet<String> forkedEvents;
    protected boolean generateTimeStampHeader;
    protected IOHandler ioHandler;
    protected boolean isAutomaticDialogErrorHandlingEnabled;
    protected boolean isAutomaticDialogSupportEnabled;
    protected boolean isBackToBackUserAgent;
    protected boolean isDialogTerminatedEventDeliveredForNullDialog;
    protected LogRecordFactory logRecordFactory;
    protected boolean logStackTraceOnMessageSend;
    protected int maxConnections;
    protected int maxContentLength;
    protected int maxForkTime;
    protected int maxListenerResponseTime;
    protected int maxMessageSize;
    private ConcurrentHashMap<String, SIPServerTransaction> mergeTable;
    private Collection<MessageProcessor> messageProcessors;
    protected boolean needsLogging;
    protected NetworkLayer networkLayer;
    private boolean non2XXAckPassedToListener;
    protected String outboundProxy;
    private ConcurrentHashMap<String, SIPServerTransaction> pendingTransactions;
    protected int readTimeout;
    protected int receiveUdpBufferSize;
    protected boolean remoteTagReassignmentAllowed;
    protected ConcurrentHashMap<String, SIPServerTransaction> retransmissionAlertTransactions;
    protected boolean rfc2543Supported;
    protected Router router;
    protected String routerPath;
    protected int sendUdpBufferSize;
    protected ServerLogger serverLogger;
    private ConcurrentHashMap<String, SIPServerTransaction> serverTransactionTable;
    protected int serverTransactionTableHighwaterMark;
    protected int serverTransactionTableLowaterMark;
    protected StackMessageFactory sipMessageFactory;
    protected String stackAddress;
    protected boolean stackDoesCongestionControl;
    protected InetAddress stackInetAddress;
    private StackLogger stackLogger;
    protected String stackName;
    private ConcurrentHashMap<String, SIPServerTransaction> terminatedServerTransactionsPendingAck;
    protected ThreadAuditor threadAuditor;
    protected int threadPoolSize;
    private Timer timer;
    protected boolean toExit;
    boolean udpFlag;
    protected boolean unlimitedClientTransactionTableSize;
    protected boolean unlimitedServerTransactionTableSize;
    protected boolean useRouterForAll;

    static {
        dialogCreatingMethods.add(Request.REFER);
        dialogCreatingMethods.add("INVITE");
        dialogCreatingMethods.add("SUBSCRIBE");
    }

    class PingTimer extends SIPStackTimerTask {
        ThreadAuditor.ThreadHandle threadHandle;

        public PingTimer(ThreadAuditor.ThreadHandle a_oThreadHandle) {
            this.threadHandle = a_oThreadHandle;
        }

        @Override
        protected void runTask() {
            if (SIPTransactionStack.this.getTimer() == null) {
                return;
            }
            if (this.threadHandle == null) {
                this.threadHandle = SIPTransactionStack.this.getThreadAuditor().addCurrentThread();
            }
            this.threadHandle.ping();
            SIPTransactionStack.this.getTimer().schedule(SIPTransactionStack.this.new PingTimer(this.threadHandle), this.threadHandle.getPingIntervalInMillisecs());
        }
    }

    class RemoveForkedTransactionTimerTask extends SIPStackTimerTask {
        private SIPClientTransaction clientTransaction;

        public RemoveForkedTransactionTimerTask(SIPClientTransaction sipClientTransaction) {
            this.clientTransaction = sipClientTransaction;
        }

        @Override
        protected void runTask() {
            SIPTransactionStack.this.forkedClientTransactionTable.remove(this.clientTransaction.getTransactionId());
        }
    }

    protected SIPTransactionStack() {
        this.unlimitedServerTransactionTableSize = true;
        this.unlimitedClientTransactionTableSize = true;
        this.serverTransactionTableHighwaterMark = 5000;
        this.serverTransactionTableLowaterMark = 4000;
        this.clientTransactionTableHiwaterMark = 1000;
        this.clientTransactionTableLowaterMark = 800;
        this.activeClientTransactionCount = new AtomicInteger(0);
        this.rfc2543Supported = true;
        this.threadAuditor = new ThreadAuditor();
        this.cancelClientTransactionChecked = true;
        this.remoteTagReassignmentAllowed = true;
        this.logStackTraceOnMessageSend = true;
        this.stackDoesCongestionControl = true;
        this.isBackToBackUserAgent = false;
        this.isAutomaticDialogErrorHandlingEnabled = true;
        this.isDialogTerminatedEventDeliveredForNullDialog = false;
        this.maxForkTime = 0;
        this.toExit = false;
        this.forkedEvents = new HashSet<>();
        this.threadPoolSize = -1;
        this.cacheServerConnections = true;
        this.cacheClientConnections = true;
        this.maxConnections = -1;
        this.messageProcessors = new ArrayList();
        this.ioHandler = new IOHandler(this);
        this.readTimeout = -1;
        this.maxListenerResponseTime = -1;
        this.addressResolver = new DefaultAddressResolver();
        this.dialogTable = new ConcurrentHashMap<>();
        this.earlyDialogTable = new ConcurrentHashMap<>();
        this.clientTransactionTable = new ConcurrentHashMap<>();
        this.serverTransactionTable = new ConcurrentHashMap<>();
        this.terminatedServerTransactionsPendingAck = new ConcurrentHashMap<>();
        this.mergeTable = new ConcurrentHashMap<>();
        this.retransmissionAlertTransactions = new ConcurrentHashMap<>();
        this.timer = new Timer();
        this.pendingTransactions = new ConcurrentHashMap<>();
        this.forkedClientTransactionTable = new ConcurrentHashMap<>();
        if (!getThreadAuditor().isEnabled()) {
            return;
        }
        this.timer.schedule(new PingTimer(null), 0L);
    }

    protected void reInit() {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("Re-initializing !");
        }
        this.messageProcessors = new ArrayList();
        this.ioHandler = new IOHandler(this);
        this.pendingTransactions = new ConcurrentHashMap<>();
        this.clientTransactionTable = new ConcurrentHashMap<>();
        this.serverTransactionTable = new ConcurrentHashMap<>();
        this.retransmissionAlertTransactions = new ConcurrentHashMap<>();
        this.mergeTable = new ConcurrentHashMap<>();
        this.dialogTable = new ConcurrentHashMap<>();
        this.earlyDialogTable = new ConcurrentHashMap<>();
        this.terminatedServerTransactionsPendingAck = new ConcurrentHashMap<>();
        this.forkedClientTransactionTable = new ConcurrentHashMap<>();
        this.timer = new Timer();
        this.activeClientTransactionCount = new AtomicInteger(0);
    }

    public SocketAddress obtainLocalAddress(InetAddress dst, int dstPort, InetAddress localAddress, int localPort) throws IOException {
        return this.ioHandler.obtainLocalAddress(dst, dstPort, localAddress, localPort);
    }

    public void disableLogging() {
        getStackLogger().disableLogging();
    }

    public void enableLogging() {
        getStackLogger().enableLogging();
    }

    public void printDialogTable() {
        if (!isLoggingEnabled()) {
            return;
        }
        getStackLogger().logDebug("dialog table  = " + this.dialogTable);
        System.out.println("dialog table = " + this.dialogTable);
    }

    public SIPServerTransaction getRetransmissionAlertTransaction(String dialogId) {
        return this.retransmissionAlertTransactions.get(dialogId);
    }

    public static boolean isDialogCreated(String method) {
        return dialogCreatingMethods.contains(method);
    }

    public void addExtensionMethod(String extensionMethod) {
        if (extensionMethod.equals("NOTIFY")) {
            if (!this.stackLogger.isLoggingEnabled()) {
                return;
            }
            this.stackLogger.logDebug("NOTIFY Supported Natively");
            return;
        }
        dialogCreatingMethods.add(extensionMethod.trim().toUpperCase());
    }

    public void putDialog(SIPDialog dialog) {
        String dialogId = dialog.getDialogId();
        if (this.dialogTable.containsKey(dialogId)) {
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logDebug("putDialog: dialog already exists" + dialogId + " in table = " + this.dialogTable.get(dialogId));
            }
        } else {
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logDebug("putDialog dialogId=" + dialogId + " dialog = " + dialog);
            }
            dialog.setStack(this);
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logStackTrace();
            }
            this.dialogTable.put(dialogId, dialog);
        }
    }

    public SIPDialog createDialog(SIPTransaction transaction) {
        if (transaction instanceof SIPClientTransaction) {
            String dialogId = ((SIPRequest) transaction.getRequest()).getDialogId(false);
            if (this.earlyDialogTable.get(dialogId) != null) {
                SIPDialog dialog = this.earlyDialogTable.get(dialogId);
                if (dialog.getState() == null || dialog.getState() == DialogState.EARLY) {
                    return dialog;
                }
                SIPDialog retval = new SIPDialog(transaction);
                this.earlyDialogTable.put(dialogId, retval);
                return retval;
            }
            SIPDialog retval2 = new SIPDialog(transaction);
            this.earlyDialogTable.put(dialogId, retval2);
            return retval2;
        }
        return new SIPDialog(transaction);
    }

    public SIPDialog createDialog(SIPClientTransaction transaction, SIPResponse sipResponse) {
        String dialogId = ((SIPRequest) transaction.getRequest()).getDialogId(false);
        if (this.earlyDialogTable.get(dialogId) != null) {
            SIPDialog retval = this.earlyDialogTable.get(dialogId);
            SIPDialog retval2 = retval;
            if (sipResponse.isFinalResponse()) {
                this.earlyDialogTable.remove(dialogId);
                return retval2;
            }
            return retval2;
        }
        SIPDialog retval3 = new SIPDialog(transaction, sipResponse);
        return retval3;
    }

    public SIPDialog createDialog(SipProviderImpl sipProvider, SIPResponse sipResponse) {
        return new SIPDialog(sipProvider, sipResponse);
    }

    public void removeDialog(SIPDialog dialog) {
        String id = dialog.getDialogId();
        String earlyId = dialog.getEarlyDialogId();
        if (earlyId != null) {
            this.earlyDialogTable.remove(earlyId);
            this.dialogTable.remove(earlyId);
        }
        if (id != null) {
            Object old = this.dialogTable.get(id);
            if (old == dialog) {
                this.dialogTable.remove(id);
            }
            if (dialog.testAndSetIsDialogTerminatedEventDelivered()) {
                return;
            }
            DialogTerminatedEvent event = new DialogTerminatedEvent(dialog.getSipProvider(), dialog);
            dialog.getSipProvider().handleEvent(event, null);
            return;
        }
        if (!this.isDialogTerminatedEventDeliveredForNullDialog || dialog.testAndSetIsDialogTerminatedEventDelivered()) {
            return;
        }
        DialogTerminatedEvent event2 = new DialogTerminatedEvent(dialog.getSipProvider(), dialog);
        dialog.getSipProvider().handleEvent(event2, null);
    }

    public SIPDialog getDialog(String dialogId) {
        SIPDialog sipDialog = this.dialogTable.get(dialogId);
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("getDialog(" + dialogId + ") : returning " + sipDialog);
        }
        return sipDialog;
    }

    public void removeDialog(String dialogId) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logWarning("Silently removing dialog from table");
        }
        this.dialogTable.remove(dialogId);
    }

    public SIPClientTransaction findSubscribeTransaction(SIPRequest notifyMessage, ListeningPointImpl listeningPoint) {
        try {
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logDebug("ct table size = " + this.clientTransactionTable.size());
            }
            String thisToTag = notifyMessage.getTo().getTag();
            if (thisToTag == null) {
                return null;
            }
            Event eventHdr = (Event) notifyMessage.getHeader("Event");
            if (eventHdr == null) {
                if (this.stackLogger.isLoggingEnabled()) {
                    this.stackLogger.logDebug("event Header is null -- returning null");
                }
                if (this.stackLogger.isLoggingEnabled()) {
                    this.stackLogger.logDebug("findSubscribeTransaction : returning " + ((Object) null));
                }
                return null;
            }
            for (SIPClientTransaction ct : this.clientTransactionTable.values()) {
                if (ct.getMethod().equals("SUBSCRIBE")) {
                    String fromTag = ct.from.getTag();
                    Event hisEvent = ct.event;
                    if (hisEvent == null) {
                        continue;
                    } else {
                        if (this.stackLogger.isLoggingEnabled()) {
                            this.stackLogger.logDebug("ct.fromTag = " + fromTag);
                            this.stackLogger.logDebug("thisToTag = " + thisToTag);
                            this.stackLogger.logDebug("hisEvent = " + hisEvent);
                            this.stackLogger.logDebug("eventHdr " + eventHdr);
                        }
                        if (fromTag.equalsIgnoreCase(thisToTag) && hisEvent != null && eventHdr.match(hisEvent) && notifyMessage.getCallId().getCallId().equalsIgnoreCase(ct.callId.getCallId())) {
                            retval = ct.acquireSem() ? ct : null;
                            if (this.stackLogger.isLoggingEnabled()) {
                                this.stackLogger.logDebug("findSubscribeTransaction : returning " + retval);
                            }
                            return retval;
                        }
                    }
                }
            }
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logDebug("findSubscribeTransaction : returning " + ((Object) null));
            }
            return null;
        } finally {
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logDebug("findSubscribeTransaction : returning " + ((Object) null));
            }
        }
    }

    public void addTransactionPendingAck(SIPServerTransaction serverTransaction) {
        String branchId = ((SIPRequest) serverTransaction.getRequest()).getTopmostVia().getBranch();
        if (branchId == null) {
            return;
        }
        this.terminatedServerTransactionsPendingAck.put(branchId, serverTransaction);
    }

    public SIPServerTransaction findTransactionPendingAck(SIPRequest ackMessage) {
        return this.terminatedServerTransactionsPendingAck.get(ackMessage.getTopmostVia().getBranch());
    }

    public boolean removeTransactionPendingAck(SIPServerTransaction serverTransaction) {
        String branchId = ((SIPRequest) serverTransaction.getRequest()).getTopmostVia().getBranch();
        if (branchId != null && this.terminatedServerTransactionsPendingAck.containsKey(branchId)) {
            this.terminatedServerTransactionsPendingAck.remove(branchId);
            return true;
        }
        return false;
    }

    public boolean isTransactionPendingAck(SIPServerTransaction serverTransaction) {
        String branchId = ((SIPRequest) serverTransaction.getRequest()).getTopmostVia().getBranch();
        return this.terminatedServerTransactionsPendingAck.contains(branchId);
    }

    public SIPTransaction findTransaction(SIPMessage sipMessage, boolean isServer) {
        SIPTransaction retval = null;
        try {
            if (isServer) {
                Via via = sipMessage.getTopmostVia();
                if (via.getBranch() != null) {
                    String key = sipMessage.getTransactionId();
                    retval = this.serverTransactionTable.get(key);
                    if (this.stackLogger.isLoggingEnabled()) {
                        getStackLogger().logDebug("serverTx: looking for key " + key + " existing=" + this.serverTransactionTable);
                    }
                    if (key.startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE)) {
                        return retval;
                    }
                }
                for (SIPServerTransaction sipServerTransaction : this.serverTransactionTable.values()) {
                    if (sipServerTransaction.isMessagePartOfTransaction(sipMessage)) {
                        if (getStackLogger().isLoggingEnabled()) {
                            getStackLogger().logDebug("findTransaction: returning  : " + sipServerTransaction);
                        }
                        return sipServerTransaction;
                    }
                }
            } else {
                Via via2 = sipMessage.getTopmostVia();
                if (via2.getBranch() != null) {
                    String key2 = sipMessage.getTransactionId();
                    if (this.stackLogger.isLoggingEnabled()) {
                        getStackLogger().logDebug("clientTx: looking for key " + key2);
                    }
                    retval = this.clientTransactionTable.get(key2);
                    if (key2.startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE)) {
                        if (getStackLogger().isLoggingEnabled()) {
                            getStackLogger().logDebug("findTransaction: returning  : " + retval);
                        }
                        return retval;
                    }
                }
                for (SIPClientTransaction clientTransaction : this.clientTransactionTable.values()) {
                    if (clientTransaction.isMessagePartOfTransaction(sipMessage)) {
                        if (getStackLogger().isLoggingEnabled()) {
                            getStackLogger().logDebug("findTransaction: returning  : " + clientTransaction);
                        }
                        return clientTransaction;
                    }
                }
            }
            if (getStackLogger().isLoggingEnabled()) {
                getStackLogger().logDebug("findTransaction: returning  : " + retval);
            }
            return retval;
        } finally {
            if (getStackLogger().isLoggingEnabled()) {
                getStackLogger().logDebug("findTransaction: returning  : " + ((Object) null));
            }
        }
    }

    public SIPTransaction findCancelTransaction(SIPRequest cancelRequest, boolean isServer) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("findCancelTransaction request= \n" + cancelRequest + "\nfindCancelRequest isServer=" + isServer);
        }
        if (isServer) {
            for (SIPTransaction transaction : this.serverTransactionTable.values()) {
                SIPServerTransaction sipServerTransaction = (SIPServerTransaction) transaction;
                if (sipServerTransaction.doesCancelMatchTransaction(cancelRequest)) {
                    return sipServerTransaction;
                }
            }
        } else {
            for (SIPTransaction transaction2 : this.clientTransactionTable.values()) {
                SIPClientTransaction sipClientTransaction = (SIPClientTransaction) transaction2;
                if (sipClientTransaction.doesCancelMatchTransaction(cancelRequest)) {
                    return sipClientTransaction;
                }
            }
        }
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("Could not find transaction for cancel request");
            return null;
        }
        return null;
    }

    protected SIPTransactionStack(StackMessageFactory messageFactory) {
        this();
        this.sipMessageFactory = messageFactory;
    }

    public SIPServerTransaction findPendingTransaction(SIPRequest requestReceived) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("looking for pending tx for :" + requestReceived.getTransactionId());
        }
        return this.pendingTransactions.get(requestReceived.getTransactionId());
    }

    public SIPServerTransaction findMergedTransaction(SIPRequest sipRequest) {
        if (!sipRequest.getMethod().equals("INVITE")) {
            return null;
        }
        String mergeId = sipRequest.getMergeId();
        SIPServerTransaction mergedTransaction = this.mergeTable.get(mergeId);
        if (mergeId == null) {
            return null;
        }
        if (mergedTransaction != null && !mergedTransaction.isMessagePartOfTransaction(sipRequest)) {
            return mergedTransaction;
        }
        for (Dialog dialog : this.dialogTable.values()) {
            if (dialog.getFirstTransaction() != null && (dialog.getFirstTransaction() instanceof ServerTransaction)) {
                SIPServerTransaction serverTransaction = (SIPServerTransaction) dialog.getFirstTransaction();
                SIPRequest transactionRequest = ((SIPServerTransaction) dialog.getFirstTransaction()).getOriginalRequest();
                if (!serverTransaction.isMessagePartOfTransaction(sipRequest) && sipRequest.getMergeId().equals(transactionRequest.getMergeId())) {
                    return (SIPServerTransaction) dialog.getFirstTransaction();
                }
            }
        }
        return null;
    }

    public void removePendingTransaction(SIPServerTransaction tr) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("removePendingTx: " + tr.getTransactionId());
        }
        this.pendingTransactions.remove(tr.getTransactionId());
    }

    public void removeFromMergeTable(SIPServerTransaction tr) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("Removing tx from merge table ");
        }
        String key = ((SIPRequest) tr.getRequest()).getMergeId();
        if (key == null) {
            return;
        }
        this.mergeTable.remove(key);
    }

    public void putInMergeTable(SIPServerTransaction sipTransaction, SIPRequest sipRequest) {
        String mergeKey = sipRequest.getMergeId();
        if (mergeKey == null) {
            return;
        }
        this.mergeTable.put(mergeKey, sipTransaction);
    }

    public void mapTransaction(SIPServerTransaction transaction) {
        if (transaction.isMapped) {
            return;
        }
        addTransactionHash(transaction);
        transaction.isMapped = true;
    }

    public ServerRequestInterface newSIPServerRequest(SIPRequest requestReceived, MessageChannel requestMessageChannel) {
        String key = requestReceived.getTransactionId();
        requestReceived.setMessageChannel(requestMessageChannel);
        SIPServerTransaction currentTransaction = this.serverTransactionTable.get(key);
        if (currentTransaction == null || !currentTransaction.isMessagePartOfTransaction(requestReceived)) {
            Iterator<SIPServerTransaction> transactionIterator = this.serverTransactionTable.values().iterator();
            currentTransaction = null;
            if (!key.toLowerCase().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE)) {
                while (transactionIterator.hasNext() && currentTransaction == null) {
                    SIPServerTransaction nextTransaction = transactionIterator.next();
                    if (nextTransaction.isMessagePartOfTransaction(requestReceived)) {
                        currentTransaction = nextTransaction;
                    }
                }
            }
            if (currentTransaction == null) {
                SIPServerTransaction currentTransaction2 = findPendingTransaction(requestReceived);
                if (currentTransaction2 != null) {
                    requestReceived.setTransaction(currentTransaction2);
                    if (currentTransaction2 == null || !currentTransaction2.acquireSem()) {
                        return null;
                    }
                    return currentTransaction2;
                }
                currentTransaction = createServerTransaction(requestMessageChannel);
                if (currentTransaction != null) {
                    currentTransaction.setOriginalRequest(requestReceived);
                    requestReceived.setTransaction(currentTransaction);
                }
            }
        }
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("newSIPServerRequest( " + requestReceived.getMethod() + Separators.COLON + requestReceived.getTopmostVia().getBranch() + "):" + currentTransaction);
        }
        if (currentTransaction != null) {
            currentTransaction.setRequestInterface(this.sipMessageFactory.newSIPServerRequest(requestReceived, currentTransaction));
        }
        if (currentTransaction != null && currentTransaction.acquireSem()) {
            return currentTransaction;
        }
        if (currentTransaction == null) {
            return null;
        }
        try {
            if (currentTransaction.isMessagePartOfTransaction(requestReceived) && currentTransaction.getMethod().equals(requestReceived.getMethod())) {
                SIPResponse trying = requestReceived.createResponse(100);
                trying.removeContent();
                currentTransaction.getMessageChannel().sendMessage(trying);
            }
        } catch (Exception e) {
            if (isLoggingEnabled()) {
                this.stackLogger.logError("Exception occured sending TRYING");
            }
        }
        return null;
    }

    public ServerResponseInterface newSIPServerResponse(SIPResponse responseReceived, MessageChannel responseMessageChannel) {
        String key = responseReceived.getTransactionId();
        SIPClientTransaction currentTransaction = this.clientTransactionTable.get(key);
        if (currentTransaction == null || (!currentTransaction.isMessagePartOfTransaction(responseReceived) && !key.startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE))) {
            Iterator<SIPClientTransaction> transactionIterator = this.clientTransactionTable.values().iterator();
            currentTransaction = null;
            while (transactionIterator.hasNext() && currentTransaction == null) {
                SIPClientTransaction nextTransaction = transactionIterator.next();
                if (nextTransaction.isMessagePartOfTransaction(responseReceived)) {
                    currentTransaction = nextTransaction;
                }
            }
            if (currentTransaction == null) {
                if (this.stackLogger.isLoggingEnabled(16)) {
                    responseMessageChannel.logResponse(responseReceived, System.currentTimeMillis(), "before processing");
                }
                return this.sipMessageFactory.newSIPServerResponse(responseReceived, responseMessageChannel);
            }
        }
        boolean acquired = currentTransaction.acquireSem();
        if (this.stackLogger.isLoggingEnabled(16)) {
            currentTransaction.logResponse(responseReceived, System.currentTimeMillis(), "before processing");
        }
        if (acquired) {
            ServerResponseInterface sri = this.sipMessageFactory.newSIPServerResponse(responseReceived, currentTransaction);
            if (sri != null) {
                currentTransaction.setResponseInterface(sri);
            } else {
                if (this.stackLogger.isLoggingEnabled()) {
                    this.stackLogger.logDebug("returning null - serverResponseInterface is null!");
                }
                currentTransaction.releaseSem();
                return null;
            }
        } else if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("Could not aquire semaphore !!");
        }
        if (acquired) {
            return currentTransaction;
        }
        return null;
    }

    public MessageChannel createMessageChannel(SIPRequest request, MessageProcessor mp, Hop nextHop) throws IOException {
        Host targetHost = new Host();
        targetHost.setHostname(nextHop.getHost());
        HostPort targetHostPort = new HostPort();
        targetHostPort.setHost(targetHost);
        targetHostPort.setPort(nextHop.getPort());
        MessageChannel mc = mp.createMessageChannel(targetHostPort);
        if (mc == null) {
            return null;
        }
        SIPClientTransaction sIPClientTransactionCreateClientTransaction = createClientTransaction(request, mc);
        sIPClientTransactionCreateClientTransaction.setViaPort(nextHop.getPort());
        sIPClientTransactionCreateClientTransaction.setViaHost(nextHop.getHost());
        addTransactionHash(sIPClientTransactionCreateClientTransaction);
        return sIPClientTransactionCreateClientTransaction;
    }

    public SIPClientTransaction createClientTransaction(SIPRequest sipRequest, MessageChannel encapsulatedMessageChannel) {
        SIPClientTransaction ct = new SIPClientTransaction(this, encapsulatedMessageChannel);
        ct.setOriginalRequest(sipRequest);
        return ct;
    }

    public SIPServerTransaction createServerTransaction(MessageChannel encapsulatedMessageChannel) {
        if (this.unlimitedServerTransactionTableSize) {
            return new SIPServerTransaction(this, encapsulatedMessageChannel);
        }
        float threshold = (this.serverTransactionTable.size() - this.serverTransactionTableLowaterMark) / (this.serverTransactionTableHighwaterMark - this.serverTransactionTableLowaterMark);
        boolean decision = Math.random() > 1.0d - ((double) threshold);
        if (decision) {
            return null;
        }
        return new SIPServerTransaction(this, encapsulatedMessageChannel);
    }

    public int getClientTransactionTableSize() {
        return this.clientTransactionTable.size();
    }

    public int getServerTransactionTableSize() {
        return this.serverTransactionTable.size();
    }

    public void addTransaction(SIPClientTransaction clientTransaction) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("added transaction " + clientTransaction);
        }
        addTransactionHash(clientTransaction);
    }

    public void removeTransaction(SIPTransaction sIPTransaction) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("Removing Transaction = " + sIPTransaction.getTransactionId() + " transaction = " + sIPTransaction);
        }
        if (sIPTransaction instanceof SIPServerTransaction) {
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logStackTrace();
            }
            Object removed = this.serverTransactionTable.remove(sIPTransaction.getTransactionId());
            String method = sIPTransaction.getMethod();
            removePendingTransaction(sIPTransaction);
            removeTransactionPendingAck(sIPTransaction);
            if (method.equalsIgnoreCase("INVITE")) {
                removeFromMergeTable(sIPTransaction);
            }
            SipProviderImpl sipProvider = sIPTransaction.getSipProvider();
            if (removed == null || !sIPTransaction.testAndSetTransactionTerminatedEvent()) {
                return;
            }
            TransactionTerminatedEvent event = new TransactionTerminatedEvent((Object) sipProvider, (ServerTransaction) sIPTransaction);
            sipProvider.handleEvent(event, sIPTransaction);
            return;
        }
        String key = sIPTransaction.getTransactionId();
        Object removed2 = this.clientTransactionTable.remove(key);
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("REMOVED client tx " + removed2 + " KEY = " + key);
            if (removed2 != null) {
                SIPClientTransaction clientTx = (SIPClientTransaction) removed2;
                if (clientTx.getMethod().equals("INVITE") && this.maxForkTime != 0) {
                    RemoveForkedTransactionTimerTask ttask = new RemoveForkedTransactionTimerTask(clientTx);
                    this.timer.schedule(ttask, this.maxForkTime * 1000);
                }
            }
        }
        if (removed2 == null || !sIPTransaction.testAndSetTransactionTerminatedEvent()) {
            return;
        }
        SipProviderImpl sipProvider2 = sIPTransaction.getSipProvider();
        TransactionTerminatedEvent event2 = new TransactionTerminatedEvent(sipProvider2, (ClientTransaction) sIPTransaction);
        sipProvider2.handleEvent(event2, sIPTransaction);
    }

    public void addTransaction(SIPServerTransaction serverTransaction) throws IOException {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("added transaction " + serverTransaction);
        }
        serverTransaction.map();
        addTransactionHash(serverTransaction);
    }

    private void addTransactionHash(SIPTransaction sipTransaction) {
        SIPRequest sipRequest = sipTransaction.getOriginalRequest();
        if (sipTransaction instanceof SIPClientTransaction) {
            if (!this.unlimitedClientTransactionTableSize) {
                if (this.activeClientTransactionCount.get() > this.clientTransactionTableHiwaterMark) {
                    try {
                        synchronized (this.clientTransactionTable) {
                            this.clientTransactionTable.wait();
                            this.activeClientTransactionCount.incrementAndGet();
                        }
                    } catch (Exception ex) {
                        if (this.stackLogger.isLoggingEnabled()) {
                            this.stackLogger.logError("Exception occured while waiting for room", ex);
                        }
                    }
                }
            } else {
                this.activeClientTransactionCount.incrementAndGet();
            }
            String key = sipRequest.getTransactionId();
            this.clientTransactionTable.put(key, sipTransaction);
            if (!this.stackLogger.isLoggingEnabled()) {
                return;
            }
            this.stackLogger.logDebug(" putTransactionHash :  key = " + key);
            return;
        }
        String key2 = sipRequest.getTransactionId();
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug(" putTransactionHash :  key = " + key2);
        }
        this.serverTransactionTable.put(key2, (SIPServerTransaction) sipTransaction);
    }

    protected void decrementActiveClientTransactionCount() {
        if (this.activeClientTransactionCount.decrementAndGet() > this.clientTransactionTableLowaterMark || this.unlimitedClientTransactionTableSize) {
            return;
        }
        synchronized (this.clientTransactionTable) {
            this.clientTransactionTable.notify();
        }
    }

    protected void removeTransactionHash(SIPTransaction sipTransaction) {
        SIPRequest sipRequest = sipTransaction.getOriginalRequest();
        if (sipRequest == null) {
            return;
        }
        if (sipTransaction instanceof SIPClientTransaction) {
            String key = sipTransaction.getTransactionId();
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logStackTrace();
                this.stackLogger.logDebug("removing client Tx : " + key);
            }
            this.clientTransactionTable.remove(key);
            return;
        }
        if (!(sipTransaction instanceof SIPServerTransaction)) {
            return;
        }
        String key2 = sipTransaction.getTransactionId();
        this.serverTransactionTable.remove(key2);
        if (!this.stackLogger.isLoggingEnabled()) {
            return;
        }
        this.stackLogger.logDebug("removing server Tx : " + key2);
    }

    @Override
    public synchronized void transactionErrorEvent(SIPTransactionErrorEvent transactionErrorEvent) {
        SIPTransaction transaction = (SIPTransaction) transactionErrorEvent.getSource();
        if (transactionErrorEvent.getErrorID() == 2) {
            transaction.setState(SIPTransaction.TERMINATED_STATE);
            if (transaction instanceof SIPServerTransaction) {
                transaction.collectionTime = 0;
            }
            transaction.disableTimeoutTimer();
            transaction.disableRetransmissionTimer();
        }
    }

    @Override
    public synchronized void dialogErrorEvent(SIPDialogErrorEvent dialogErrorEvent) {
        SIPDialog sipDialog = (SIPDialog) dialogErrorEvent.getSource();
        SipListener sipListener = ((SipStackImpl) this).getSipListener();
        if (sipDialog != null && !(sipListener instanceof SipListenerExt)) {
            sipDialog.delete();
        }
    }

    public void stopStack() {
        if (this.timer != null) {
            this.timer.cancel();
        }
        this.timer = null;
        this.pendingTransactions.clear();
        this.toExit = true;
        synchronized (this) {
            notifyAll();
        }
        synchronized (this.clientTransactionTable) {
            this.clientTransactionTable.notifyAll();
        }
        synchronized (this.messageProcessors) {
            MessageProcessor[] processorList = getMessageProcessors();
            for (MessageProcessor messageProcessor : processorList) {
                removeMessageProcessor(messageProcessor);
            }
            this.ioHandler.closeAll();
        }
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
        }
        this.clientTransactionTable.clear();
        this.serverTransactionTable.clear();
        this.dialogTable.clear();
        this.serverLogger.closeLogFile();
    }

    public void putPendingTransaction(SIPServerTransaction tr) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("putPendingTransaction: " + tr);
        }
        this.pendingTransactions.put(tr.getTransactionId(), tr);
    }

    public NetworkLayer getNetworkLayer() {
        if (this.networkLayer == null) {
            return DefaultNetworkLayer.SINGLETON;
        }
        return this.networkLayer;
    }

    public boolean isLoggingEnabled() {
        if (this.stackLogger == null) {
            return false;
        }
        return this.stackLogger.isLoggingEnabled();
    }

    public StackLogger getStackLogger() {
        return this.stackLogger;
    }

    public ServerLogger getServerLogger() {
        return this.serverLogger;
    }

    public int getMaxMessageSize() {
        return this.maxMessageSize;
    }

    public void setSingleThreaded() {
        this.threadPoolSize = 1;
    }

    public void setThreadPoolSize(int size) {
        this.threadPoolSize = size;
    }

    public void setMaxConnections(int nconnections) {
        this.maxConnections = nconnections;
    }

    public Hop getNextHop(SIPRequest sipRequest) throws SipException {
        if (this.useRouterForAll) {
            if (this.router != null) {
                return this.router.getNextHop(sipRequest);
            }
            return null;
        }
        if (sipRequest.getRequestURI().isSipURI() || sipRequest.getRouteHeaders() != null) {
            return this.defaultRouter.getNextHop(sipRequest);
        }
        if (this.router != null) {
            return this.router.getNextHop(sipRequest);
        }
        return null;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    protected void setHostAddress(String stackAddress) throws UnknownHostException {
        if (stackAddress.indexOf(58) != stackAddress.lastIndexOf(58) && stackAddress.trim().charAt(0) != '[') {
            this.stackAddress = '[' + stackAddress + ']';
        } else {
            this.stackAddress = stackAddress;
        }
        this.stackInetAddress = InetAddress.getByName(stackAddress);
    }

    public String getHostAddress() {
        return this.stackAddress;
    }

    protected void setRouter(Router router) {
        this.router = router;
    }

    public Router getRouter(SIPRequest request) {
        if (request.getRequestLine() == null) {
            return this.defaultRouter;
        }
        if (this.useRouterForAll) {
            return this.router;
        }
        if (request.getRequestURI().getScheme().equals("sip") || request.getRequestURI().getScheme().equals("sips")) {
            return this.defaultRouter;
        }
        if (this.router != null) {
            return this.router;
        }
        return this.defaultRouter;
    }

    public Router getRouter() {
        return this.router;
    }

    public boolean isAlive() {
        return !this.toExit;
    }

    protected void addMessageProcessor(MessageProcessor newMessageProcessor) throws IOException {
        synchronized (this.messageProcessors) {
            this.messageProcessors.add(newMessageProcessor);
        }
    }

    protected void removeMessageProcessor(MessageProcessor oldMessageProcessor) {
        synchronized (this.messageProcessors) {
            if (this.messageProcessors.remove(oldMessageProcessor)) {
                oldMessageProcessor.stop();
            }
        }
    }

    protected MessageProcessor[] getMessageProcessors() {
        MessageProcessor[] messageProcessorArr;
        synchronized (this.messageProcessors) {
            messageProcessorArr = (MessageProcessor[]) this.messageProcessors.toArray(new MessageProcessor[0]);
        }
        return messageProcessorArr;
    }

    protected MessageProcessor createMessageProcessor(InetAddress ipAddress, int port, String transport) throws IOException {
        if (transport.equalsIgnoreCase(ParameterNames.UDP)) {
            UDPMessageProcessor udpMessageProcessor = new UDPMessageProcessor(ipAddress, this, port);
            addMessageProcessor(udpMessageProcessor);
            this.udpFlag = true;
            return udpMessageProcessor;
        }
        if (transport.equalsIgnoreCase(ParameterNames.TCP)) {
            TCPMessageProcessor tcpMessageProcessor = new TCPMessageProcessor(ipAddress, this, port);
            addMessageProcessor(tcpMessageProcessor);
            return tcpMessageProcessor;
        }
        if (transport.equalsIgnoreCase(ParameterNames.TLS)) {
            TLSMessageProcessor tlsMessageProcessor = new TLSMessageProcessor(ipAddress, this, port);
            addMessageProcessor(tlsMessageProcessor);
            return tlsMessageProcessor;
        }
        if (transport.equalsIgnoreCase("sctp")) {
            try {
                Class<?> mpc = ClassLoader.getSystemClassLoader().loadClass("gov.nist.javax.sip.stack.sctp.SCTPMessageProcessor");
                MessageProcessor mp = (MessageProcessor) mpc.newInstance();
                mp.initialize(ipAddress, port, this);
                addMessageProcessor(mp);
                return mp;
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("SCTP not supported (needs Java 7 and SCTP jar in classpath)");
            } catch (IllegalAccessException ie) {
                throw new IllegalArgumentException("Error initializing SCTP", ie);
            } catch (InstantiationException ie2) {
                throw new IllegalArgumentException("Error initializing SCTP", ie2);
            }
        }
        throw new IllegalArgumentException("bad transport");
    }

    protected void setMessageFactory(StackMessageFactory messageFactory) {
        this.sipMessageFactory = messageFactory;
    }

    public MessageChannel createRawMessageChannel(String sourceIpAddress, int sourcePort, Hop nextHop) throws UnknownHostException {
        Host targetHost = new Host();
        targetHost.setHostname(nextHop.getHost());
        HostPort targetHostPort = new HostPort();
        targetHostPort.setHost(targetHost);
        targetHostPort.setPort(nextHop.getPort());
        MessageChannel newChannel = null;
        Iterator<MessageProcessor> it = this.messageProcessors.iterator();
        while (it.hasNext() && newChannel == null) {
            MessageProcessor nextProcessor = it.next();
            if (nextHop.getTransport().equalsIgnoreCase(nextProcessor.getTransport()) && sourceIpAddress.equals(nextProcessor.getIpAddress().getHostAddress()) && sourcePort == nextProcessor.getPort()) {
                try {
                    newChannel = nextProcessor.createMessageChannel(targetHostPort);
                } catch (UnknownHostException ex) {
                    if (this.stackLogger.isLoggingEnabled()) {
                        this.stackLogger.logException(ex);
                    }
                    throw ex;
                } catch (IOException e) {
                    if (this.stackLogger.isLoggingEnabled()) {
                        this.stackLogger.logException(e);
                    }
                }
            }
        }
        return newChannel;
    }

    public boolean isEventForked(String ename) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("isEventForked: " + ename + " returning " + this.forkedEvents.contains(ename));
        }
        return this.forkedEvents.contains(ename);
    }

    public AddressResolver getAddressResolver() {
        return this.addressResolver;
    }

    public void setAddressResolver(AddressResolver addressResolver) {
        this.addressResolver = addressResolver;
    }

    public void setLogRecordFactory(LogRecordFactory logRecordFactory) {
        this.logRecordFactory = logRecordFactory;
    }

    public ThreadAuditor getThreadAuditor() {
        return this.threadAuditor;
    }

    public String auditStack(Set activeCallIDs, long leakedDialogTimer, long leakedTransactionTimer) {
        String leakedDialogs = auditDialogs(activeCallIDs, leakedDialogTimer);
        String leakedServerTransactions = auditTransactions(this.serverTransactionTable, leakedTransactionTimer);
        String leakedClientTransactions = auditTransactions(this.clientTransactionTable, leakedTransactionTimer);
        if (leakedDialogs == null && leakedServerTransactions == null && leakedClientTransactions == null) {
            return null;
        }
        StringBuilder sbAppend = new StringBuilder().append("SIP Stack Audit:\n");
        if (leakedDialogs == null) {
            leakedDialogs = "";
        }
        StringBuilder sbAppend2 = sbAppend.append(leakedDialogs);
        if (leakedServerTransactions == null) {
            leakedServerTransactions = "";
        }
        StringBuilder sbAppend3 = sbAppend2.append(leakedServerTransactions);
        if (leakedClientTransactions == null) {
            leakedClientTransactions = "";
        }
        String auditReport = sbAppend3.append(leakedClientTransactions).toString();
        return auditReport;
    }

    private String auditDialogs(Set activeCallIDs, long leakedDialogTimer) {
        LinkedList<SIPDialog> dialogs;
        String auditReport = "  Leaked dialogs:\n";
        int leakedDialogs = 0;
        long currentTime = System.currentTimeMillis();
        synchronized (this.dialogTable) {
            dialogs = new LinkedList(this.dialogTable.values());
        }
        for (SIPDialog itDialog : dialogs) {
            CallIdHeader callIdHeader = itDialog != null ? itDialog.getCallId() : null;
            String callId = callIdHeader != null ? callIdHeader.getCallId() : null;
            if (itDialog != null && callId != null && !activeCallIDs.contains(callId)) {
                if (itDialog.auditTag == 0) {
                    itDialog.auditTag = currentTime;
                } else if (currentTime - itDialog.auditTag >= leakedDialogTimer) {
                    leakedDialogs++;
                    DialogState dialogState = itDialog.getState();
                    String dialogReport = "dialog id: " + itDialog.getDialogId() + ", dialog state: " + (dialogState != null ? dialogState.toString() : "null");
                    auditReport = auditReport + "    " + dialogReport + Separators.RETURN;
                    itDialog.setState(SIPDialog.TERMINATED_STATE);
                    if (this.stackLogger.isLoggingEnabled()) {
                        this.stackLogger.logDebug("auditDialogs: leaked " + dialogReport);
                    }
                }
            }
        }
        if (leakedDialogs > 0) {
            return auditReport + "    Total: " + Integer.toString(leakedDialogs) + " leaked dialogs detected and removed.\n";
        }
        return null;
    }

    private String auditTransactions(ConcurrentHashMap transactionsMap, long a_nLeakedTransactionTimer) {
        String auditReport = "  Leaked transactions:\n";
        int leakedTransactions = 0;
        long currentTime = System.currentTimeMillis();
        LinkedList<SIPTransaction> transactionsList = new LinkedList(transactionsMap.values());
        for (SIPTransaction sipTransaction : transactionsList) {
            if (sipTransaction != null) {
                if (sipTransaction.auditTag == 0) {
                    sipTransaction.auditTag = currentTime;
                } else if (currentTime - sipTransaction.auditTag >= a_nLeakedTransactionTimer) {
                    leakedTransactions++;
                    TransactionState transactionState = sipTransaction.getState();
                    SIPRequest origRequest = sipTransaction.getOriginalRequest();
                    String method = origRequest != null ? origRequest.getMethod() : null;
                    StringBuilder sbAppend = new StringBuilder().append(sipTransaction.getClass().getName()).append(", state: ").append(transactionState != null ? transactionState.toString() : "null").append(", OR: ");
                    if (method == null) {
                        method = "null";
                    }
                    String transactionReport = sbAppend.append(method).toString();
                    auditReport = auditReport + "    " + transactionReport + Separators.RETURN;
                    removeTransaction(sipTransaction);
                    if (isLoggingEnabled()) {
                        this.stackLogger.logDebug("auditTransactions: leaked " + transactionReport);
                    }
                }
            }
        }
        if (leakedTransactions > 0) {
            return auditReport + "    Total: " + Integer.toString(leakedTransactions) + " leaked transactions detected and removed.\n";
        }
        return null;
    }

    public void setNon2XXAckPassedToListener(boolean passToListener) {
        this.non2XXAckPassedToListener = passToListener;
    }

    public boolean isNon2XXAckPassedToListener() {
        return this.non2XXAckPassedToListener;
    }

    public int getActiveClientTransactionCount() {
        return this.activeClientTransactionCount.get();
    }

    public boolean isRfc2543Supported() {
        return this.rfc2543Supported;
    }

    public boolean isCancelClientTransactionChecked() {
        return this.cancelClientTransactionChecked;
    }

    public boolean isRemoteTagReassignmentAllowed() {
        return this.remoteTagReassignmentAllowed;
    }

    public Collection<Dialog> getDialogs() {
        HashSet<Dialog> dialogs = new HashSet<>();
        dialogs.addAll(this.dialogTable.values());
        dialogs.addAll(this.earlyDialogTable.values());
        return dialogs;
    }

    public Collection<Dialog> getDialogs(DialogState state) {
        HashSet<Dialog> matchingDialogs = new HashSet<>();
        if (DialogState.EARLY.equals(state)) {
            matchingDialogs.addAll(this.earlyDialogTable.values());
        } else {
            Collection<SIPDialog> dialogs = this.dialogTable.values();
            for (SIPDialog dialog : dialogs) {
                if (dialog.getState() != null && dialog.getState().equals(state)) {
                    matchingDialogs.add(dialog);
                }
            }
        }
        return matchingDialogs;
    }

    public Dialog getReplacesDialog(ReplacesHeader replacesHeader) {
        String cid = replacesHeader.getCallId();
        String fromTag = replacesHeader.getFromTag();
        String toTag = replacesHeader.getToTag();
        StringBuffer dialogId = new StringBuffer(cid);
        if (toTag != null) {
            dialogId.append(Separators.COLON);
            dialogId.append(toTag);
        }
        if (fromTag != null) {
            dialogId.append(Separators.COLON);
            dialogId.append(fromTag);
        }
        String did = dialogId.toString().toLowerCase();
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("Looking for dialog " + did);
        }
        Dialog replacesDialog = this.dialogTable.get(did);
        if (replacesDialog == null) {
            for (SIPClientTransaction ctx : this.clientTransactionTable.values()) {
                if (ctx.getDialog(did) != null) {
                    return ctx.getDialog(did);
                }
            }
            return replacesDialog;
        }
        return replacesDialog;
    }

    public Dialog getJoinDialog(JoinHeader joinHeader) {
        String cid = joinHeader.getCallId();
        String fromTag = joinHeader.getFromTag();
        String toTag = joinHeader.getToTag();
        StringBuffer retval = new StringBuffer(cid);
        if (toTag != null) {
            retval.append(Separators.COLON);
            retval.append(toTag);
        }
        if (fromTag != null) {
            retval.append(Separators.COLON);
            retval.append(fromTag);
        }
        return this.dialogTable.get(retval.toString().toLowerCase());
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public Timer getTimer() {
        return this.timer;
    }

    public int getReceiveUdpBufferSize() {
        return this.receiveUdpBufferSize;
    }

    public void setReceiveUdpBufferSize(int receiveUdpBufferSize) {
        this.receiveUdpBufferSize = receiveUdpBufferSize;
    }

    public int getSendUdpBufferSize() {
        return this.sendUdpBufferSize;
    }

    public void setSendUdpBufferSize(int sendUdpBufferSize) {
        this.sendUdpBufferSize = sendUdpBufferSize;
    }

    public void setStackLogger(StackLogger stackLogger) {
        this.stackLogger = stackLogger;
    }

    public boolean checkBranchId() {
        return this.checkBranchId;
    }

    public void setLogStackTraceOnMessageSend(boolean logStackTraceOnMessageSend) {
        this.logStackTraceOnMessageSend = logStackTraceOnMessageSend;
    }

    public boolean isLogStackTraceOnMessageSend() {
        return this.logStackTraceOnMessageSend;
    }

    public void setDeliverDialogTerminatedEventForNullDialog() {
        this.isDialogTerminatedEventDeliveredForNullDialog = true;
    }

    public void addForkedClientTransaction(SIPClientTransaction clientTransaction) {
        this.forkedClientTransactionTable.put(clientTransaction.getTransactionId(), clientTransaction);
    }

    public SIPClientTransaction getForkedTransaction(String transactionId) {
        return this.forkedClientTransactionTable.get(transactionId);
    }
}
