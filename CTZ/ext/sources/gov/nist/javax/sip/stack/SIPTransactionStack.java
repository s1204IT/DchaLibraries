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

        public PingTimer(ThreadAuditor.ThreadHandle threadHandle) {
            this.threadHandle = threadHandle;
        }

        @Override
        protected void runTask() {
            if (SIPTransactionStack.this.getTimer() != null) {
                if (this.threadHandle == null) {
                    this.threadHandle = SIPTransactionStack.this.getThreadAuditor().addCurrentThread();
                }
                this.threadHandle.ping();
                SIPTransactionStack.this.getTimer().schedule(SIPTransactionStack.this.new PingTimer(this.threadHandle), this.threadHandle.getPingIntervalInMillisecs());
            }
        }
    }

    class RemoveForkedTransactionTimerTask extends SIPStackTimerTask {
        private SIPClientTransaction clientTransaction;

        public RemoveForkedTransactionTimerTask(SIPClientTransaction sIPClientTransaction) {
            this.clientTransaction = sIPClientTransaction;
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
        if (getThreadAuditor().isEnabled()) {
            this.timer.schedule(new PingTimer(null), 0L);
        }
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

    public SocketAddress obtainLocalAddress(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) throws IOException {
        return this.ioHandler.obtainLocalAddress(inetAddress, i, inetAddress2, i2);
    }

    public void disableLogging() {
        getStackLogger().disableLogging();
    }

    public void enableLogging() {
        getStackLogger().enableLogging();
    }

    public void printDialogTable() {
        if (isLoggingEnabled()) {
            getStackLogger().logDebug("dialog table  = " + this.dialogTable);
            System.out.println("dialog table = " + this.dialogTable);
        }
    }

    public SIPServerTransaction getRetransmissionAlertTransaction(String str) {
        return this.retransmissionAlertTransactions.get(str);
    }

    public static boolean isDialogCreated(String str) {
        return dialogCreatingMethods.contains(str);
    }

    public void addExtensionMethod(String str) {
        if (str.equals("NOTIFY")) {
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logDebug("NOTIFY Supported Natively");
                return;
            }
            return;
        }
        dialogCreatingMethods.add(str.trim().toUpperCase());
    }

    public void putDialog(SIPDialog sIPDialog) {
        String dialogId = sIPDialog.getDialogId();
        if (this.dialogTable.containsKey(dialogId)) {
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logDebug("putDialog: dialog already exists" + dialogId + " in table = " + this.dialogTable.get(dialogId));
                return;
            }
            return;
        }
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("putDialog dialogId=" + dialogId + " dialog = " + sIPDialog);
        }
        sIPDialog.setStack(this);
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logStackTrace();
        }
        this.dialogTable.put(dialogId, sIPDialog);
    }

    public SIPDialog createDialog(SIPTransaction sIPTransaction) {
        if (sIPTransaction instanceof SIPClientTransaction) {
            String dialogId = ((SIPRequest) sIPTransaction.getRequest()).getDialogId(false);
            if (this.earlyDialogTable.get(dialogId) != null) {
                SIPDialog sIPDialog = this.earlyDialogTable.get(dialogId);
                if (sIPDialog.getState() != null && sIPDialog.getState() != DialogState.EARLY) {
                    SIPDialog sIPDialog2 = new SIPDialog(sIPTransaction);
                    this.earlyDialogTable.put(dialogId, sIPDialog2);
                    return sIPDialog2;
                }
                return sIPDialog;
            }
            SIPDialog sIPDialog3 = new SIPDialog(sIPTransaction);
            this.earlyDialogTable.put(dialogId, sIPDialog3);
            return sIPDialog3;
        }
        return new SIPDialog(sIPTransaction);
    }

    public SIPDialog createDialog(SIPClientTransaction sIPClientTransaction, SIPResponse sIPResponse) {
        String dialogId = ((SIPRequest) sIPClientTransaction.getRequest()).getDialogId(false);
        if (this.earlyDialogTable.get(dialogId) != null) {
            SIPDialog sIPDialog = this.earlyDialogTable.get(dialogId);
            if (!sIPResponse.isFinalResponse()) {
                return sIPDialog;
            }
            this.earlyDialogTable.remove(dialogId);
            return sIPDialog;
        }
        return new SIPDialog(sIPClientTransaction, sIPResponse);
    }

    public SIPDialog createDialog(SipProviderImpl sipProviderImpl, SIPResponse sIPResponse) {
        return new SIPDialog(sipProviderImpl, sIPResponse);
    }

    public void removeDialog(SIPDialog sIPDialog) {
        String dialogId = sIPDialog.getDialogId();
        String earlyDialogId = sIPDialog.getEarlyDialogId();
        if (earlyDialogId != null) {
            this.earlyDialogTable.remove(earlyDialogId);
            this.dialogTable.remove(earlyDialogId);
        }
        if (dialogId != null) {
            if (this.dialogTable.get(dialogId) == sIPDialog) {
                this.dialogTable.remove(dialogId);
            }
            if (!sIPDialog.testAndSetIsDialogTerminatedEventDelivered()) {
                sIPDialog.getSipProvider().handleEvent(new DialogTerminatedEvent(sIPDialog.getSipProvider(), sIPDialog), null);
                return;
            }
            return;
        }
        if (this.isDialogTerminatedEventDeliveredForNullDialog && !sIPDialog.testAndSetIsDialogTerminatedEventDelivered()) {
            sIPDialog.getSipProvider().handleEvent(new DialogTerminatedEvent(sIPDialog.getSipProvider(), sIPDialog), null);
        }
    }

    public SIPDialog getDialog(String str) {
        SIPDialog sIPDialog = this.dialogTable.get(str);
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("getDialog(" + str + ") : returning " + sIPDialog);
        }
        return sIPDialog;
    }

    public void removeDialog(String str) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logWarning("Silently removing dialog from table");
        }
        this.dialogTable.remove(str);
    }

    public SIPClientTransaction findSubscribeTransaction(SIPRequest sIPRequest, ListeningPointImpl listeningPointImpl) {
        try {
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logDebug("ct table size = " + this.clientTransactionTable.size());
            }
            String tag = sIPRequest.getTo().getTag();
            if (tag == null) {
                return null;
            }
            Event event = (Event) sIPRequest.getHeader("Event");
            if (event == null) {
                if (this.stackLogger.isLoggingEnabled()) {
                    this.stackLogger.logDebug("event Header is null -- returning null");
                }
                if (this.stackLogger.isLoggingEnabled()) {
                    this.stackLogger.logDebug("findSubscribeTransaction : returning " + ((Object) null));
                }
                return null;
            }
            for (SIPClientTransaction sIPClientTransaction : this.clientTransactionTable.values()) {
                if (sIPClientTransaction.getMethod().equals("SUBSCRIBE")) {
                    String tag2 = sIPClientTransaction.from.getTag();
                    Event event2 = sIPClientTransaction.event;
                    if (event2 != null) {
                        if (this.stackLogger.isLoggingEnabled()) {
                            this.stackLogger.logDebug("ct.fromTag = " + tag2);
                            this.stackLogger.logDebug("thisToTag = " + tag);
                            this.stackLogger.logDebug("hisEvent = " + event2);
                            this.stackLogger.logDebug("eventHdr " + event);
                        }
                        if (tag2.equalsIgnoreCase(tag) && event2 != null && event.match(event2) && sIPRequest.getCallId().getCallId().equalsIgnoreCase(sIPClientTransaction.callId.getCallId())) {
                            sIPClientTransaction = sIPClientTransaction.acquireSem() ? sIPClientTransaction : null;
                            if (this.stackLogger.isLoggingEnabled()) {
                                this.stackLogger.logDebug("findSubscribeTransaction : returning " + sIPClientTransaction);
                            }
                            return sIPClientTransaction;
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

    public void addTransactionPendingAck(SIPServerTransaction sIPServerTransaction) {
        String branch = ((SIPRequest) sIPServerTransaction.getRequest()).getTopmostVia().getBranch();
        if (branch != null) {
            this.terminatedServerTransactionsPendingAck.put(branch, sIPServerTransaction);
        }
    }

    public SIPServerTransaction findTransactionPendingAck(SIPRequest sIPRequest) {
        return this.terminatedServerTransactionsPendingAck.get(sIPRequest.getTopmostVia().getBranch());
    }

    public boolean removeTransactionPendingAck(SIPServerTransaction sIPServerTransaction) {
        String branch = ((SIPRequest) sIPServerTransaction.getRequest()).getTopmostVia().getBranch();
        if (branch != null && this.terminatedServerTransactionsPendingAck.containsKey(branch)) {
            this.terminatedServerTransactionsPendingAck.remove(branch);
            return true;
        }
        return false;
    }

    public boolean isTransactionPendingAck(SIPServerTransaction sIPServerTransaction) {
        return this.terminatedServerTransactionsPendingAck.contains(((SIPRequest) sIPServerTransaction.getRequest()).getTopmostVia().getBranch());
    }

    public SIPTransaction findTransaction(SIPMessage sIPMessage, boolean z) throws Throwable {
        SIPTransaction sIPTransaction;
        SIPTransaction sIPTransaction2 = null;
        try {
            try {
                if (z) {
                    if (sIPMessage.getTopmostVia().getBranch() != null) {
                        String transactionId = sIPMessage.getTransactionId();
                        SIPServerTransaction sIPServerTransaction = this.serverTransactionTable.get(transactionId);
                        if (this.stackLogger.isLoggingEnabled()) {
                            getStackLogger().logDebug("serverTx: looking for key " + transactionId + " existing=" + this.serverTransactionTable);
                        }
                        if (transactionId.startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE)) {
                            if (getStackLogger().isLoggingEnabled()) {
                                getStackLogger().logDebug("findTransaction: returning  : " + sIPServerTransaction);
                            }
                            return sIPServerTransaction;
                        }
                        sIPTransaction2 = sIPServerTransaction;
                    }
                    for (SIPServerTransaction sIPServerTransaction2 : this.serverTransactionTable.values()) {
                        if (sIPServerTransaction2.isMessagePartOfTransaction(sIPMessage)) {
                            if (getStackLogger().isLoggingEnabled()) {
                                getStackLogger().logDebug("findTransaction: returning  : " + sIPServerTransaction2);
                            }
                            return sIPServerTransaction2;
                        }
                    }
                } else {
                    if (sIPMessage.getTopmostVia().getBranch() != null) {
                        String transactionId2 = sIPMessage.getTransactionId();
                        if (this.stackLogger.isLoggingEnabled()) {
                            getStackLogger().logDebug("clientTx: looking for key " + transactionId2);
                        }
                        SIPClientTransaction sIPClientTransaction = this.clientTransactionTable.get(transactionId2);
                        if (transactionId2.startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE)) {
                            if (getStackLogger().isLoggingEnabled()) {
                                getStackLogger().logDebug("findTransaction: returning  : " + sIPClientTransaction);
                            }
                            return sIPClientTransaction;
                        }
                        sIPTransaction2 = sIPClientTransaction;
                    }
                    for (SIPClientTransaction sIPClientTransaction2 : this.clientTransactionTable.values()) {
                        if (sIPClientTransaction2.isMessagePartOfTransaction(sIPMessage)) {
                            if (getStackLogger().isLoggingEnabled()) {
                                getStackLogger().logDebug("findTransaction: returning  : " + sIPClientTransaction2);
                            }
                            return sIPClientTransaction2;
                        }
                    }
                }
                if (getStackLogger().isLoggingEnabled()) {
                    getStackLogger().logDebug("findTransaction: returning  : " + sIPTransaction2);
                }
                return sIPTransaction2;
            } catch (Throwable th) {
                th = th;
                sIPTransaction2 = sIPTransaction;
                if (getStackLogger().isLoggingEnabled()) {
                    getStackLogger().logDebug("findTransaction: returning  : " + sIPTransaction2);
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            if (getStackLogger().isLoggingEnabled()) {
            }
            throw th;
        }
    }

    public SIPTransaction findCancelTransaction(SIPRequest sIPRequest, boolean z) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("findCancelTransaction request= \n" + sIPRequest + "\nfindCancelRequest isServer=" + z);
        }
        if (z) {
            for (SIPServerTransaction sIPServerTransaction : this.serverTransactionTable.values()) {
                if (sIPServerTransaction.doesCancelMatchTransaction(sIPRequest)) {
                    return sIPServerTransaction;
                }
            }
        } else {
            for (SIPClientTransaction sIPClientTransaction : this.clientTransactionTable.values()) {
                if (sIPClientTransaction.doesCancelMatchTransaction(sIPRequest)) {
                    return sIPClientTransaction;
                }
            }
        }
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("Could not find transaction for cancel request");
            return null;
        }
        return null;
    }

    protected SIPTransactionStack(StackMessageFactory stackMessageFactory) {
        this();
        this.sipMessageFactory = stackMessageFactory;
    }

    public SIPServerTransaction findPendingTransaction(SIPRequest sIPRequest) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("looking for pending tx for :" + sIPRequest.getTransactionId());
        }
        return this.pendingTransactions.get(sIPRequest.getTransactionId());
    }

    public SIPServerTransaction findMergedTransaction(SIPRequest sIPRequest) {
        if (!sIPRequest.getMethod().equals("INVITE")) {
            return null;
        }
        String mergeId = sIPRequest.getMergeId();
        SIPServerTransaction sIPServerTransaction = this.mergeTable.get(mergeId);
        if (mergeId == null) {
            return null;
        }
        if (sIPServerTransaction != null && !sIPServerTransaction.isMessagePartOfTransaction(sIPRequest)) {
            return sIPServerTransaction;
        }
        for (SIPDialog sIPDialog : this.dialogTable.values()) {
            if (sIPDialog.getFirstTransaction() != null && (sIPDialog.getFirstTransaction() instanceof ServerTransaction)) {
                SIPServerTransaction sIPServerTransaction2 = (SIPServerTransaction) sIPDialog.getFirstTransaction();
                SIPRequest originalRequest = ((SIPServerTransaction) sIPDialog.getFirstTransaction()).getOriginalRequest();
                if (!sIPServerTransaction2.isMessagePartOfTransaction(sIPRequest) && sIPRequest.getMergeId().equals(originalRequest.getMergeId())) {
                    return (SIPServerTransaction) sIPDialog.getFirstTransaction();
                }
            }
        }
        return null;
    }

    public void removePendingTransaction(SIPServerTransaction sIPServerTransaction) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("removePendingTx: " + sIPServerTransaction.getTransactionId());
        }
        this.pendingTransactions.remove(sIPServerTransaction.getTransactionId());
    }

    public void removeFromMergeTable(SIPServerTransaction sIPServerTransaction) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("Removing tx from merge table ");
        }
        String mergeId = ((SIPRequest) sIPServerTransaction.getRequest()).getMergeId();
        if (mergeId != null) {
            this.mergeTable.remove(mergeId);
        }
    }

    public void putInMergeTable(SIPServerTransaction sIPServerTransaction, SIPRequest sIPRequest) {
        String mergeId = sIPRequest.getMergeId();
        if (mergeId != null) {
            this.mergeTable.put(mergeId, sIPServerTransaction);
        }
    }

    public void mapTransaction(SIPServerTransaction sIPServerTransaction) {
        if (sIPServerTransaction.isMapped) {
            return;
        }
        addTransactionHash(sIPServerTransaction);
        sIPServerTransaction.isMapped = true;
    }

    public ServerRequestInterface newSIPServerRequest(SIPRequest sIPRequest, MessageChannel messageChannel) {
        String transactionId = sIPRequest.getTransactionId();
        sIPRequest.setMessageChannel(messageChannel);
        SIPServerTransaction sIPServerTransactionCreateServerTransaction = this.serverTransactionTable.get(transactionId);
        if (sIPServerTransactionCreateServerTransaction == null || !sIPServerTransactionCreateServerTransaction.isMessagePartOfTransaction(sIPRequest)) {
            Iterator<SIPServerTransaction> it = this.serverTransactionTable.values().iterator();
            if (transactionId.toLowerCase().startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE)) {
                sIPServerTransactionCreateServerTransaction = null;
            } else {
                SIPServerTransaction sIPServerTransaction = null;
                while (it.hasNext() && sIPServerTransaction == null) {
                    SIPServerTransaction next = it.next();
                    if (next.isMessagePartOfTransaction(sIPRequest)) {
                        sIPServerTransaction = next;
                    }
                }
                sIPServerTransactionCreateServerTransaction = sIPServerTransaction;
            }
            if (sIPServerTransactionCreateServerTransaction == null) {
                SIPServerTransaction sIPServerTransactionFindPendingTransaction = findPendingTransaction(sIPRequest);
                if (sIPServerTransactionFindPendingTransaction != null) {
                    sIPRequest.setTransaction(sIPServerTransactionFindPendingTransaction);
                    if (sIPServerTransactionFindPendingTransaction == null || !sIPServerTransactionFindPendingTransaction.acquireSem()) {
                        return null;
                    }
                    return sIPServerTransactionFindPendingTransaction;
                }
                sIPServerTransactionCreateServerTransaction = createServerTransaction(messageChannel);
                if (sIPServerTransactionCreateServerTransaction != null) {
                    sIPServerTransactionCreateServerTransaction.setOriginalRequest(sIPRequest);
                    sIPRequest.setTransaction(sIPServerTransactionCreateServerTransaction);
                }
            }
        }
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("newSIPServerRequest( " + sIPRequest.getMethod() + Separators.COLON + sIPRequest.getTopmostVia().getBranch() + "):" + sIPServerTransactionCreateServerTransaction);
        }
        if (sIPServerTransactionCreateServerTransaction != null) {
            sIPServerTransactionCreateServerTransaction.setRequestInterface(this.sipMessageFactory.newSIPServerRequest(sIPRequest, sIPServerTransactionCreateServerTransaction));
        }
        if (sIPServerTransactionCreateServerTransaction != null && sIPServerTransactionCreateServerTransaction.acquireSem()) {
            return sIPServerTransactionCreateServerTransaction;
        }
        if (sIPServerTransactionCreateServerTransaction == null) {
            return null;
        }
        try {
            if (sIPServerTransactionCreateServerTransaction.isMessagePartOfTransaction(sIPRequest) && sIPServerTransactionCreateServerTransaction.getMethod().equals(sIPRequest.getMethod())) {
                SIPResponse sIPResponseCreateResponse = sIPRequest.createResponse(100);
                sIPResponseCreateResponse.removeContent();
                sIPServerTransactionCreateServerTransaction.getMessageChannel().sendMessage(sIPResponseCreateResponse);
            }
        } catch (Exception e) {
            if (isLoggingEnabled()) {
                this.stackLogger.logError("Exception occured sending TRYING");
            }
        }
        return null;
    }

    public ServerResponseInterface newSIPServerResponse(SIPResponse sIPResponse, MessageChannel messageChannel) {
        String transactionId = sIPResponse.getTransactionId();
        SIPClientTransaction sIPClientTransaction = this.clientTransactionTable.get(transactionId);
        if (sIPClientTransaction == null || (!sIPClientTransaction.isMessagePartOfTransaction(sIPResponse) && !transactionId.startsWith(SIPConstants.BRANCH_MAGIC_COOKIE_LOWER_CASE))) {
            Iterator<SIPClientTransaction> it = this.clientTransactionTable.values().iterator();
            sIPClientTransaction = null;
            while (it.hasNext() && sIPClientTransaction == null) {
                SIPClientTransaction next = it.next();
                if (next.isMessagePartOfTransaction(sIPResponse)) {
                    sIPClientTransaction = next;
                }
            }
            if (sIPClientTransaction == null) {
                if (this.stackLogger.isLoggingEnabled(16)) {
                    messageChannel.logResponse(sIPResponse, System.currentTimeMillis(), "before processing");
                }
                return this.sipMessageFactory.newSIPServerResponse(sIPResponse, messageChannel);
            }
        }
        boolean zAcquireSem = sIPClientTransaction.acquireSem();
        if (this.stackLogger.isLoggingEnabled(16)) {
            sIPClientTransaction.logResponse(sIPResponse, System.currentTimeMillis(), "before processing");
        }
        if (zAcquireSem) {
            ServerResponseInterface serverResponseInterfaceNewSIPServerResponse = this.sipMessageFactory.newSIPServerResponse(sIPResponse, sIPClientTransaction);
            if (serverResponseInterfaceNewSIPServerResponse != null) {
                sIPClientTransaction.setResponseInterface(serverResponseInterfaceNewSIPServerResponse);
            } else {
                if (this.stackLogger.isLoggingEnabled()) {
                    this.stackLogger.logDebug("returning null - serverResponseInterface is null!");
                }
                sIPClientTransaction.releaseSem();
                return null;
            }
        } else if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("Could not aquire semaphore !!");
        }
        if (zAcquireSem) {
            return sIPClientTransaction;
        }
        return null;
    }

    public MessageChannel createMessageChannel(SIPRequest sIPRequest, MessageProcessor messageProcessor, Hop hop) throws IOException {
        Host host = new Host();
        host.setHostname(hop.getHost());
        HostPort hostPort = new HostPort();
        hostPort.setHost(host);
        hostPort.setPort(hop.getPort());
        MessageChannel messageChannelCreateMessageChannel = messageProcessor.createMessageChannel(hostPort);
        if (messageChannelCreateMessageChannel == null) {
            return null;
        }
        SIPClientTransaction sIPClientTransactionCreateClientTransaction = createClientTransaction(sIPRequest, messageChannelCreateMessageChannel);
        SIPClientTransaction sIPClientTransaction = sIPClientTransactionCreateClientTransaction;
        sIPClientTransaction.setViaPort(hop.getPort());
        sIPClientTransaction.setViaHost(hop.getHost());
        addTransactionHash(sIPClientTransactionCreateClientTransaction);
        return sIPClientTransactionCreateClientTransaction;
    }

    public SIPClientTransaction createClientTransaction(SIPRequest sIPRequest, MessageChannel messageChannel) {
        SIPClientTransaction sIPClientTransaction = new SIPClientTransaction(this, messageChannel);
        sIPClientTransaction.setOriginalRequest(sIPRequest);
        return sIPClientTransaction;
    }

    public SIPServerTransaction createServerTransaction(MessageChannel messageChannel) {
        if (this.unlimitedServerTransactionTableSize) {
            return new SIPServerTransaction(this, messageChannel);
        }
        if (Math.random() > 1.0d - ((double) (((float) (this.serverTransactionTable.size() - this.serverTransactionTableLowaterMark)) / ((float) (this.serverTransactionTableHighwaterMark - this.serverTransactionTableLowaterMark))))) {
            return null;
        }
        return new SIPServerTransaction(this, messageChannel);
    }

    public int getClientTransactionTableSize() {
        return this.clientTransactionTable.size();
    }

    public int getServerTransactionTableSize() {
        return this.serverTransactionTable.size();
    }

    public void addTransaction(SIPClientTransaction sIPClientTransaction) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("added transaction " + sIPClientTransaction);
        }
        addTransactionHash(sIPClientTransaction);
    }

    public void removeTransaction(SIPTransaction sIPTransaction) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("Removing Transaction = " + sIPTransaction.getTransactionId() + " transaction = " + sIPTransaction);
        }
        if (sIPTransaction instanceof SIPServerTransaction) {
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logStackTrace();
            }
            SIPServerTransaction sIPServerTransactionRemove = this.serverTransactionTable.remove(sIPTransaction.getTransactionId());
            String method = sIPTransaction.getMethod();
            SIPServerTransaction sIPServerTransaction = (SIPServerTransaction) sIPTransaction;
            removePendingTransaction(sIPServerTransaction);
            removeTransactionPendingAck(sIPServerTransaction);
            if (method.equalsIgnoreCase("INVITE")) {
                removeFromMergeTable(sIPServerTransaction);
            }
            SipProviderImpl sipProvider = sIPTransaction.getSipProvider();
            if (sIPServerTransactionRemove != null && sIPTransaction.testAndSetTransactionTerminatedEvent()) {
                sipProvider.handleEvent(new TransactionTerminatedEvent(sipProvider, (ServerTransaction) sIPTransaction), sIPTransaction);
                return;
            }
            return;
        }
        String transactionId = sIPTransaction.getTransactionId();
        SIPClientTransaction sIPClientTransactionRemove = this.clientTransactionTable.remove(transactionId);
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("REMOVED client tx " + sIPClientTransactionRemove + " KEY = " + transactionId);
            if (sIPClientTransactionRemove != null) {
                SIPClientTransaction sIPClientTransaction = sIPClientTransactionRemove;
                if (sIPClientTransaction.getMethod().equals("INVITE") && this.maxForkTime != 0) {
                    this.timer.schedule(new RemoveForkedTransactionTimerTask(sIPClientTransaction), this.maxForkTime * 1000);
                }
            }
        }
        if (sIPClientTransactionRemove != null && sIPTransaction.testAndSetTransactionTerminatedEvent()) {
            SipProviderImpl sipProvider2 = sIPTransaction.getSipProvider();
            sipProvider2.handleEvent(new TransactionTerminatedEvent(sipProvider2, (ClientTransaction) sIPTransaction), sIPTransaction);
        }
    }

    public void addTransaction(SIPServerTransaction sIPServerTransaction) throws IOException {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("added transaction " + sIPServerTransaction);
        }
        sIPServerTransaction.map();
        addTransactionHash(sIPServerTransaction);
    }

    private void addTransactionHash(SIPTransaction sIPTransaction) {
        SIPRequest originalRequest = sIPTransaction.getOriginalRequest();
        if (sIPTransaction instanceof SIPClientTransaction) {
            if (!this.unlimitedClientTransactionTableSize) {
                if (this.activeClientTransactionCount.get() > this.clientTransactionTableHiwaterMark) {
                    try {
                        synchronized (this.clientTransactionTable) {
                            this.clientTransactionTable.wait();
                            this.activeClientTransactionCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        if (this.stackLogger.isLoggingEnabled()) {
                            this.stackLogger.logError("Exception occured while waiting for room", e);
                        }
                    }
                }
            } else {
                this.activeClientTransactionCount.incrementAndGet();
            }
            String transactionId = originalRequest.getTransactionId();
            this.clientTransactionTable.put(transactionId, (SIPClientTransaction) sIPTransaction);
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logDebug(" putTransactionHash :  key = " + transactionId);
                return;
            }
            return;
        }
        String transactionId2 = originalRequest.getTransactionId();
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug(" putTransactionHash :  key = " + transactionId2);
        }
        this.serverTransactionTable.put(transactionId2, (SIPServerTransaction) sIPTransaction);
    }

    protected void decrementActiveClientTransactionCount() {
        if (this.activeClientTransactionCount.decrementAndGet() <= this.clientTransactionTableLowaterMark && !this.unlimitedClientTransactionTableSize) {
            synchronized (this.clientTransactionTable) {
                this.clientTransactionTable.notify();
            }
        }
    }

    protected void removeTransactionHash(SIPTransaction sIPTransaction) {
        if (sIPTransaction.getOriginalRequest() == null) {
            return;
        }
        if (sIPTransaction instanceof SIPClientTransaction) {
            String transactionId = sIPTransaction.getTransactionId();
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logStackTrace();
                this.stackLogger.logDebug("removing client Tx : " + transactionId);
            }
            this.clientTransactionTable.remove(transactionId);
            return;
        }
        if (sIPTransaction instanceof SIPServerTransaction) {
            String transactionId2 = sIPTransaction.getTransactionId();
            this.serverTransactionTable.remove(transactionId2);
            if (this.stackLogger.isLoggingEnabled()) {
                this.stackLogger.logDebug("removing server Tx : " + transactionId2);
            }
        }
    }

    @Override
    public synchronized void transactionErrorEvent(SIPTransactionErrorEvent sIPTransactionErrorEvent) {
        SIPTransaction sIPTransaction = (SIPTransaction) sIPTransactionErrorEvent.getSource();
        if (sIPTransactionErrorEvent.getErrorID() == 2) {
            sIPTransaction.setState(SIPTransaction.TERMINATED_STATE);
            if (sIPTransaction instanceof SIPServerTransaction) {
                ((SIPServerTransaction) sIPTransaction).collectionTime = 0;
            }
            sIPTransaction.disableTimeoutTimer();
            sIPTransaction.disableRetransmissionTimer();
        }
    }

    @Override
    public synchronized void dialogErrorEvent(SIPDialogErrorEvent sIPDialogErrorEvent) {
        SIPDialog sIPDialog = (SIPDialog) sIPDialogErrorEvent.getSource();
        SipListener sipListener = ((SipStackImpl) this).getSipListener();
        if (sIPDialog != null && !(sipListener instanceof SipListenerExt)) {
            sIPDialog.delete();
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
            for (MessageProcessor messageProcessor : getMessageProcessors()) {
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

    public void putPendingTransaction(SIPServerTransaction sIPServerTransaction) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("putPendingTransaction: " + sIPServerTransaction);
        }
        this.pendingTransactions.put(sIPServerTransaction.getTransactionId(), sIPServerTransaction);
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

    public void setThreadPoolSize(int i) {
        this.threadPoolSize = i;
    }

    public void setMaxConnections(int i) {
        this.maxConnections = i;
    }

    public Hop getNextHop(SIPRequest sIPRequest) throws SipException {
        if (this.useRouterForAll) {
            if (this.router != null) {
                return this.router.getNextHop(sIPRequest);
            }
            return null;
        }
        if (sIPRequest.getRequestURI().isSipURI() || sIPRequest.getRouteHeaders() != null) {
            return this.defaultRouter.getNextHop(sIPRequest);
        }
        if (this.router != null) {
            return this.router.getNextHop(sIPRequest);
        }
        return null;
    }

    public void setStackName(String str) {
        this.stackName = str;
    }

    protected void setHostAddress(String str) throws UnknownHostException {
        if (str.indexOf(58) != str.lastIndexOf(58) && str.trim().charAt(0) != '[') {
            this.stackAddress = '[' + str + ']';
        } else {
            this.stackAddress = str;
        }
        this.stackInetAddress = InetAddress.getByName(str);
    }

    public String getHostAddress() {
        return this.stackAddress;
    }

    protected void setRouter(Router router) {
        this.router = router;
    }

    public Router getRouter(SIPRequest sIPRequest) {
        if (sIPRequest.getRequestLine() == null) {
            return this.defaultRouter;
        }
        if (this.useRouterForAll) {
            return this.router;
        }
        if (sIPRequest.getRequestURI().getScheme().equals("sip") || sIPRequest.getRequestURI().getScheme().equals("sips")) {
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

    protected void addMessageProcessor(MessageProcessor messageProcessor) throws IOException {
        synchronized (this.messageProcessors) {
            this.messageProcessors.add(messageProcessor);
        }
    }

    protected void removeMessageProcessor(MessageProcessor messageProcessor) {
        synchronized (this.messageProcessors) {
            if (this.messageProcessors.remove(messageProcessor)) {
                messageProcessor.stop();
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

    protected MessageProcessor createMessageProcessor(InetAddress inetAddress, int i, String str) throws IOException {
        if (str.equalsIgnoreCase(ParameterNames.UDP)) {
            UDPMessageProcessor uDPMessageProcessor = new UDPMessageProcessor(inetAddress, this, i);
            addMessageProcessor(uDPMessageProcessor);
            this.udpFlag = true;
            return uDPMessageProcessor;
        }
        if (str.equalsIgnoreCase(ParameterNames.TCP)) {
            TCPMessageProcessor tCPMessageProcessor = new TCPMessageProcessor(inetAddress, this, i);
            addMessageProcessor(tCPMessageProcessor);
            return tCPMessageProcessor;
        }
        if (str.equalsIgnoreCase(ParameterNames.TLS)) {
            TLSMessageProcessor tLSMessageProcessor = new TLSMessageProcessor(inetAddress, this, i);
            addMessageProcessor(tLSMessageProcessor);
            return tLSMessageProcessor;
        }
        if (str.equalsIgnoreCase("sctp")) {
            try {
                MessageProcessor messageProcessor = (MessageProcessor) ClassLoader.getSystemClassLoader().loadClass("gov.nist.javax.sip.stack.sctp.SCTPMessageProcessor").newInstance();
                messageProcessor.initialize(inetAddress, i, this);
                addMessageProcessor(messageProcessor);
                return messageProcessor;
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("SCTP not supported (needs Java 7 and SCTP jar in classpath)");
            } catch (IllegalAccessException e2) {
                throw new IllegalArgumentException("Error initializing SCTP", e2);
            } catch (InstantiationException e3) {
                throw new IllegalArgumentException("Error initializing SCTP", e3);
            }
        }
        throw new IllegalArgumentException("bad transport");
    }

    protected void setMessageFactory(StackMessageFactory stackMessageFactory) {
        this.sipMessageFactory = stackMessageFactory;
    }

    public MessageChannel createRawMessageChannel(String str, int i, Hop hop) throws UnknownHostException {
        Host host = new Host();
        host.setHostname(hop.getHost());
        HostPort hostPort = new HostPort();
        hostPort.setHost(host);
        hostPort.setPort(hop.getPort());
        Iterator<MessageProcessor> it = this.messageProcessors.iterator();
        MessageChannel messageChannelCreateMessageChannel = null;
        while (it.hasNext() && messageChannelCreateMessageChannel == null) {
            MessageProcessor next = it.next();
            if (hop.getTransport().equalsIgnoreCase(next.getTransport()) && str.equals(next.getIpAddress().getHostAddress()) && i == next.getPort()) {
                try {
                    messageChannelCreateMessageChannel = next.createMessageChannel(hostPort);
                } catch (UnknownHostException e) {
                    if (this.stackLogger.isLoggingEnabled()) {
                        this.stackLogger.logException(e);
                    }
                    throw e;
                } catch (IOException e2) {
                    if (this.stackLogger.isLoggingEnabled()) {
                        this.stackLogger.logException(e2);
                    }
                }
            }
        }
        return messageChannelCreateMessageChannel;
    }

    public boolean isEventForked(String str) {
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("isEventForked: " + str + " returning " + this.forkedEvents.contains(str));
        }
        return this.forkedEvents.contains(str);
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

    public String auditStack(Set set, long j, long j2) {
        String strAuditDialogs = auditDialogs(set, j);
        String strAuditTransactions = auditTransactions(this.serverTransactionTable, j2);
        String strAuditTransactions2 = auditTransactions(this.clientTransactionTable, j2);
        if (strAuditDialogs != null || strAuditTransactions != null || strAuditTransactions2 != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("SIP Stack Audit:\n");
            if (strAuditDialogs == null) {
                strAuditDialogs = "";
            }
            sb.append(strAuditDialogs);
            if (strAuditTransactions == null) {
                strAuditTransactions = "";
            }
            sb.append(strAuditTransactions);
            if (strAuditTransactions2 == null) {
                strAuditTransactions2 = "";
            }
            sb.append(strAuditTransactions2);
            return sb.toString();
        }
        return null;
    }

    private String auditDialogs(Set set, long j) {
        LinkedList linkedList;
        String str = "  Leaked dialogs:\n";
        long jCurrentTimeMillis = System.currentTimeMillis();
        synchronized (this.dialogTable) {
            linkedList = new LinkedList(this.dialogTable.values());
        }
        Iterator it = linkedList.iterator();
        int i = 0;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            SIPDialog sIPDialog = (SIPDialog) it.next();
            CallIdHeader callId = sIPDialog != null ? sIPDialog.getCallId() : null;
            String callId2 = callId != null ? callId.getCallId() : null;
            if (sIPDialog != null && callId2 != null && !set.contains(callId2)) {
                if (sIPDialog.auditTag == 0) {
                    sIPDialog.auditTag = jCurrentTimeMillis;
                } else if (jCurrentTimeMillis - sIPDialog.auditTag >= j) {
                    i++;
                    DialogState state = sIPDialog.getState();
                    StringBuilder sb = new StringBuilder();
                    sb.append("dialog id: ");
                    sb.append(sIPDialog.getDialogId());
                    sb.append(", dialog state: ");
                    sb.append(state != null ? state.toString() : "null");
                    String string = sb.toString();
                    str = str + "    " + string + Separators.RETURN;
                    sIPDialog.setState(SIPDialog.TERMINATED_STATE);
                    if (this.stackLogger.isLoggingEnabled()) {
                        this.stackLogger.logDebug("auditDialogs: leaked " + string);
                    }
                }
            }
        }
        if (i <= 0) {
            return null;
        }
        return str + "    Total: " + Integer.toString(i) + " leaked dialogs detected and removed.\n";
    }

    private String auditTransactions(ConcurrentHashMap concurrentHashMap, long j) {
        String str = "  Leaked transactions:\n";
        long jCurrentTimeMillis = System.currentTimeMillis();
        Iterator it = new LinkedList(concurrentHashMap.values()).iterator();
        int i = 0;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            SIPTransaction sIPTransaction = (SIPTransaction) it.next();
            if (sIPTransaction != null) {
                if (sIPTransaction.auditTag == 0) {
                    sIPTransaction.auditTag = jCurrentTimeMillis;
                } else if (jCurrentTimeMillis - sIPTransaction.auditTag >= j) {
                    i++;
                    TransactionState state = sIPTransaction.getState();
                    SIPRequest originalRequest = sIPTransaction.getOriginalRequest();
                    String method = originalRequest != null ? originalRequest.getMethod() : null;
                    StringBuilder sb = new StringBuilder();
                    sb.append(sIPTransaction.getClass().getName());
                    sb.append(", state: ");
                    sb.append(state != null ? state.toString() : "null");
                    sb.append(", OR: ");
                    if (method == null) {
                        method = "null";
                    }
                    sb.append(method);
                    String string = sb.toString();
                    str = str + "    " + string + Separators.RETURN;
                    removeTransaction(sIPTransaction);
                    if (isLoggingEnabled()) {
                        this.stackLogger.logDebug("auditTransactions: leaked " + string);
                    }
                }
            }
        }
        if (i <= 0) {
            return null;
        }
        return str + "    Total: " + Integer.toString(i) + " leaked transactions detected and removed.\n";
    }

    public void setNon2XXAckPassedToListener(boolean z) {
        this.non2XXAckPassedToListener = z;
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
        HashSet hashSet = new HashSet();
        hashSet.addAll(this.dialogTable.values());
        hashSet.addAll(this.earlyDialogTable.values());
        return hashSet;
    }

    public Collection<Dialog> getDialogs(DialogState dialogState) {
        HashSet hashSet = new HashSet();
        if (DialogState.EARLY.equals(dialogState)) {
            hashSet.addAll(this.earlyDialogTable.values());
        } else {
            for (SIPDialog sIPDialog : this.dialogTable.values()) {
                if (sIPDialog.getState() != null && sIPDialog.getState().equals(dialogState)) {
                    hashSet.add(sIPDialog);
                }
            }
        }
        return hashSet;
    }

    public Dialog getReplacesDialog(ReplacesHeader replacesHeader) {
        String callId = replacesHeader.getCallId();
        String fromTag = replacesHeader.getFromTag();
        String toTag = replacesHeader.getToTag();
        StringBuffer stringBuffer = new StringBuffer(callId);
        if (toTag != null) {
            stringBuffer.append(Separators.COLON);
            stringBuffer.append(toTag);
        }
        if (fromTag != null) {
            stringBuffer.append(Separators.COLON);
            stringBuffer.append(fromTag);
        }
        String lowerCase = stringBuffer.toString().toLowerCase();
        if (this.stackLogger.isLoggingEnabled()) {
            this.stackLogger.logDebug("Looking for dialog " + lowerCase);
        }
        SIPDialog sIPDialog = this.dialogTable.get(lowerCase);
        if (sIPDialog == null) {
            for (SIPClientTransaction sIPClientTransaction : this.clientTransactionTable.values()) {
                if (sIPClientTransaction.getDialog(lowerCase) != null) {
                    return sIPClientTransaction.getDialog(lowerCase);
                }
            }
            return sIPDialog;
        }
        return sIPDialog;
    }

    public Dialog getJoinDialog(JoinHeader joinHeader) {
        String callId = joinHeader.getCallId();
        String fromTag = joinHeader.getFromTag();
        String toTag = joinHeader.getToTag();
        StringBuffer stringBuffer = new StringBuffer(callId);
        if (toTag != null) {
            stringBuffer.append(Separators.COLON);
            stringBuffer.append(toTag);
        }
        if (fromTag != null) {
            stringBuffer.append(Separators.COLON);
            stringBuffer.append(fromTag);
        }
        return this.dialogTable.get(stringBuffer.toString().toLowerCase());
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

    public void setReceiveUdpBufferSize(int i) {
        this.receiveUdpBufferSize = i;
    }

    public int getSendUdpBufferSize() {
        return this.sendUdpBufferSize;
    }

    public void setSendUdpBufferSize(int i) {
        this.sendUdpBufferSize = i;
    }

    public void setStackLogger(StackLogger stackLogger) {
        this.stackLogger = stackLogger;
    }

    public boolean checkBranchId() {
        return this.checkBranchId;
    }

    public void setLogStackTraceOnMessageSend(boolean z) {
        this.logStackTraceOnMessageSend = z;
    }

    public boolean isLogStackTraceOnMessageSend() {
        return this.logStackTraceOnMessageSend;
    }

    public void setDeliverDialogTerminatedEventForNullDialog() {
        this.isDialogTerminatedEventDeliveredForNullDialog = true;
    }

    public void addForkedClientTransaction(SIPClientTransaction sIPClientTransaction) {
        this.forkedClientTransactionTable.put(sIPClientTransaction.getTransactionId(), sIPClientTransaction);
    }

    public SIPClientTransaction getForkedTransaction(String str) {
        return this.forkedClientTransactionTable.get(str);
    }
}
