package com.android.server.sip;

import android.net.sip.ISipSession;
import android.net.sip.ISipSessionListener;
import android.net.sip.SipProfile;
import android.net.sip.SipSession;
import android.net.sip.SipSessionAdapter;
import android.telephony.Rlog;
import android.text.TextUtils;
import gov.nist.javax.sip.clientauthutils.AccountManager;
import gov.nist.javax.sip.clientauthutils.UserCredentials;
import gov.nist.javax.sip.header.ProxyAuthenticate;
import gov.nist.javax.sip.header.StatusLine;
import gov.nist.javax.sip.header.WWWAuthenticate;
import gov.nist.javax.sip.header.extensions.ReferredByHeader;
import gov.nist.javax.sip.header.extensions.ReplacesHeader;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ObjectInUseException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.Transaction;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ReferToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;

class SipSessionGroup implements SipListener {
    private static final String ANONYMOUS = "anonymous";
    private static final int CANCEL_CALL_TIMER = 3;
    private static final boolean DBG = false;
    private static final boolean DBG_PING = false;
    private static final EventObject DEREGISTER = new EventObject("Deregister");
    private static final EventObject END_CALL = new EventObject("End call");
    private static final int END_CALL_TIMER = 3;
    private static final int EXPIRY_TIME = 3600;
    private static final int INCALL_KEEPALIVE_INTERVAL = 10;
    private static final int KEEPALIVE_TIMEOUT = 5;
    private static final String TAG = "SipSession";
    private static final String THREAD_POOL_SIZE = "1";
    private static final long WAKE_LOCK_HOLDING_TIME = 500;
    private SipSessionImpl mCallReceiverSession;
    private String mExternalIp;
    private int mExternalPort;
    private String mLocalIp;
    private final SipProfile mLocalProfile;
    private final String mPassword;
    private Map<String, SipSessionImpl> mSessionMap = new HashMap();
    private SipHelper mSipHelper;
    private SipStack mSipStack;
    private SipWakeLock mWakeLock;
    private SipWakeupTimer mWakeupTimer;

    interface KeepAliveProcessCallback {
        void onError(int i, String str);

        void onResponse(boolean z);
    }

    public SipSessionGroup(SipProfile sipProfile, String str, SipWakeupTimer sipWakeupTimer, SipWakeLock sipWakeLock) throws SipException {
        this.mLocalProfile = sipProfile;
        this.mPassword = str;
        this.mWakeupTimer = sipWakeupTimer;
        this.mWakeLock = sipWakeLock;
        reset();
    }

    void setWakeupTimer(SipWakeupTimer sipWakeupTimer) {
        this.mWakeupTimer = sipWakeupTimer;
    }

    synchronized void reset() throws SipException {
        String hostAddress;
        Properties properties = new Properties();
        String protocol = this.mLocalProfile.getProtocol();
        int port = this.mLocalProfile.getPort();
        String proxyAddress = this.mLocalProfile.getProxyAddress();
        if (!TextUtils.isEmpty(proxyAddress)) {
            properties.setProperty("javax.sip.OUTBOUND_PROXY", proxyAddress + ':' + port + '/' + protocol);
        } else {
            proxyAddress = this.mLocalProfile.getSipDomain();
        }
        if (proxyAddress.startsWith("[") && proxyAddress.endsWith("]")) {
            proxyAddress = proxyAddress.substring(1, proxyAddress.length() - 1);
        }
        try {
            InetAddress[] allByName = InetAddress.getAllByName(proxyAddress);
            int length = allByName.length;
            int i = 0;
            while (true) {
                if (i < length) {
                    InetAddress inetAddress = allByName[i];
                    DatagramSocket datagramSocket = new DatagramSocket();
                    datagramSocket.connect(inetAddress, port);
                    if (datagramSocket.isConnected()) {
                        break;
                    }
                    datagramSocket.close();
                    i++;
                } else {
                    hostAddress = null;
                    break;
                }
            }
        } catch (Exception e) {
            hostAddress = null;
        }
        if (hostAddress == null) {
            return;
        }
        close();
        this.mLocalIp = hostAddress;
        properties.setProperty("javax.sip.STACK_NAME", getStackName());
        properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", THREAD_POOL_SIZE);
        this.mSipStack = SipFactory.getInstance().createSipStack(properties);
        try {
            SipProvider sipProviderCreateSipProvider = this.mSipStack.createSipProvider(this.mSipStack.createListeningPoint(hostAddress, port, protocol));
            sipProviderCreateSipProvider.addSipListener(this);
            this.mSipHelper = new SipHelper(this.mSipStack, sipProviderCreateSipProvider);
            this.mSipStack.start();
        } catch (Exception e2) {
            throw new SipException("failed to initialize SIP stack", e2);
        } catch (SipException e3) {
            throw e3;
        }
    }

    synchronized void onConnectivityChanged() {
        for (SipSessionImpl sipSessionImpl : (SipSessionImpl[]) this.mSessionMap.values().toArray(new SipSessionImpl[this.mSessionMap.size()])) {
            sipSessionImpl.onError(-10, "data connection lost");
        }
    }

    synchronized void resetExternalAddress() {
        this.mExternalIp = null;
        this.mExternalPort = 0;
    }

    public SipProfile getLocalProfile() {
        return this.mLocalProfile;
    }

    public String getLocalProfileUri() {
        return this.mLocalProfile.getUriString();
    }

    private String getStackName() {
        return "stack" + System.currentTimeMillis();
    }

    public synchronized void close() {
        onConnectivityChanged();
        this.mSessionMap.clear();
        closeToNotReceiveCalls();
        if (this.mSipStack != null) {
            this.mSipStack.stop();
            this.mSipStack = null;
            this.mSipHelper = null;
        }
        resetExternalAddress();
    }

    public synchronized boolean isClosed() {
        return this.mSipStack == null;
    }

    public synchronized void openToReceiveCalls(ISipSessionListener iSipSessionListener) {
        if (this.mCallReceiverSession == null) {
            this.mCallReceiverSession = new SipSessionCallReceiverImpl(iSipSessionListener);
        } else {
            this.mCallReceiverSession.setListener(iSipSessionListener);
        }
    }

    public synchronized void closeToNotReceiveCalls() {
        this.mCallReceiverSession = null;
    }

    public ISipSession createSession(ISipSessionListener iSipSessionListener) {
        if (isClosed()) {
            return null;
        }
        return new SipSessionImpl(iSipSessionListener);
    }

    synchronized boolean containsSession(String str) {
        return this.mSessionMap.containsKey(str);
    }

    private synchronized SipSessionImpl getSipSession(EventObject eventObject) {
        SipSessionImpl sipSessionImpl;
        sipSessionImpl = this.mSessionMap.get(SipHelper.getCallId(eventObject));
        if (sipSessionImpl != null && isLoggable(sipSessionImpl)) {
            for (String str : this.mSessionMap.keySet()) {
            }
        }
        if (sipSessionImpl == null) {
            sipSessionImpl = this.mCallReceiverSession;
        }
        return sipSessionImpl;
    }

    private synchronized void addSipSession(SipSessionImpl sipSessionImpl) {
        removeSipSession(sipSessionImpl);
        this.mSessionMap.put(sipSessionImpl.getCallId(), sipSessionImpl);
        if (isLoggable(sipSessionImpl)) {
            for (String str : this.mSessionMap.keySet()) {
            }
        }
    }

    private synchronized void removeSipSession(SipSessionImpl sipSessionImpl) {
        if (sipSessionImpl == this.mCallReceiverSession) {
            return;
        }
        String callId = sipSessionImpl.getCallId();
        SipSessionImpl sipSessionImplRemove = this.mSessionMap.remove(callId);
        if (sipSessionImplRemove != null && sipSessionImplRemove != sipSessionImpl) {
            this.mSessionMap.put(callId, sipSessionImplRemove);
            for (Map.Entry<String, SipSessionImpl> entry : this.mSessionMap.entrySet()) {
                if (entry.getValue() == sipSessionImplRemove) {
                    this.mSessionMap.remove(entry.getKey());
                }
            }
        }
        if (sipSessionImplRemove != null && isLoggable(sipSessionImplRemove)) {
            for (String str : this.mSessionMap.keySet()) {
            }
        }
    }

    public void processRequest(RequestEvent requestEvent) {
        if (isRequestEvent("INVITE", requestEvent)) {
            this.mWakeLock.acquire(WAKE_LOCK_HOLDING_TIME);
        }
        process(requestEvent);
    }

    public void processResponse(ResponseEvent responseEvent) {
        process(responseEvent);
    }

    public void processIOException(IOExceptionEvent iOExceptionEvent) {
        process(iOExceptionEvent);
    }

    public void processTimeout(TimeoutEvent timeoutEvent) {
        process(timeoutEvent);
    }

    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        process(transactionTerminatedEvent);
    }

    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        process(dialogTerminatedEvent);
    }

    private synchronized void process(EventObject eventObject) {
        SipSessionImpl sipSession = getSipSession(eventObject);
        try {
            boolean zIsLoggable = isLoggable(sipSession, eventObject);
            boolean z = sipSession != null && sipSession.process(eventObject);
            if (zIsLoggable && z) {
                log("process: event new state after: " + SipSession.State.toString(sipSession.mState));
            }
        } catch (Throwable th) {
            loge("process: error event=" + eventObject, getRootCause(th));
            sipSession.onError(th);
        }
    }

    private String extractContent(Message message) {
        byte[] rawContent = message.getRawContent();
        if (rawContent != null) {
            try {
                if (message instanceof SIPMessage) {
                    return ((SIPMessage) message).getMessageContent();
                }
                return new String(rawContent, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        return null;
    }

    private void extractExternalAddress(ResponseEvent responseEvent) {
        ViaHeader header = responseEvent.getResponse().getHeader("Via");
        if (header == null) {
            return;
        }
        int rPort = header.getRPort();
        String received = header.getReceived();
        if (rPort > 0 && received != null) {
            this.mExternalIp = received;
            this.mExternalPort = rPort;
        }
    }

    private Throwable getRootCause(Throwable th) {
        Throwable cause = th.getCause();
        while (true) {
            Throwable th2 = cause;
            Throwable th3 = th;
            th = th2;
            if (th != null) {
                cause = th.getCause();
            } else {
                return th3;
            }
        }
    }

    private SipSessionImpl createNewSession(RequestEvent requestEvent, ISipSessionListener iSipSessionListener, ServerTransaction serverTransaction, int i) throws SipException {
        SipSessionImpl sipSessionImpl = new SipSessionImpl(iSipSessionListener);
        sipSessionImpl.mServerTransaction = serverTransaction;
        sipSessionImpl.mState = i;
        sipSessionImpl.mDialog = sipSessionImpl.mServerTransaction.getDialog();
        sipSessionImpl.mInviteReceived = requestEvent;
        sipSessionImpl.mPeerProfile = createPeerProfile(requestEvent.getRequest().getHeader("From"));
        sipSessionImpl.mPeerSessionDescription = extractContent(requestEvent.getRequest());
        return sipSessionImpl;
    }

    private class SipSessionCallReceiverImpl extends SipSessionImpl {
        private static final boolean SSCRI_DBG = true;
        private static final String SSCRI_TAG = "SipSessionCallReceiverImpl";

        public SipSessionCallReceiverImpl(ISipSessionListener iSipSessionListener) {
            super(iSipSessionListener);
        }

        private int processInviteWithReplaces(RequestEvent requestEvent, ReplacesHeader replacesHeader) {
            ReferredByHeader header;
            SipSessionImpl sipSessionImpl = (SipSessionImpl) SipSessionGroup.this.mSessionMap.get(replacesHeader.getCallId());
            if (sipSessionImpl == null) {
                return 481;
            }
            Dialog dialog = sipSessionImpl.mDialog;
            if (dialog == null) {
                return 603;
            }
            if (!dialog.getLocalTag().equals(replacesHeader.getToTag()) || !dialog.getRemoteTag().equals(replacesHeader.getFromTag()) || (header = requestEvent.getRequest().getHeader("Referred-By")) == null || !dialog.getRemoteParty().equals(header.getAddress())) {
                return 481;
            }
            return 200;
        }

        private void processNewInviteRequest(RequestEvent requestEvent) throws SipException {
            SipSessionImpl sipSessionImplCreateNewSession;
            ReplacesHeader replacesHeader = (ReplacesHeader) requestEvent.getRequest().getHeader("Replaces");
            if (replacesHeader == null) {
                sipSessionImplCreateNewSession = SipSessionGroup.this.createNewSession(requestEvent, this.mProxy, SipSessionGroup.this.mSipHelper.sendRinging(requestEvent, generateTag()), 3);
                this.mProxy.onRinging(sipSessionImplCreateNewSession, sipSessionImplCreateNewSession.mPeerProfile, sipSessionImplCreateNewSession.mPeerSessionDescription);
            } else {
                int iProcessInviteWithReplaces = processInviteWithReplaces(requestEvent, replacesHeader);
                log("processNewInviteRequest: " + replacesHeader + " response=" + iProcessInviteWithReplaces);
                if (iProcessInviteWithReplaces == 200) {
                    sipSessionImplCreateNewSession = SipSessionGroup.this.createNewSession(requestEvent, ((SipSessionImpl) SipSessionGroup.this.mSessionMap.get(replacesHeader.getCallId())).mProxy.getListener(), SipSessionGroup.this.mSipHelper.getServerTransaction(requestEvent), 3);
                    sipSessionImplCreateNewSession.mProxy.onCallTransferring(sipSessionImplCreateNewSession, sipSessionImplCreateNewSession.mPeerSessionDescription);
                } else {
                    SipSessionGroup.this.mSipHelper.sendResponse(requestEvent, iProcessInviteWithReplaces);
                    sipSessionImplCreateNewSession = null;
                }
            }
            if (sipSessionImplCreateNewSession != null) {
                SipSessionGroup.this.addSipSession(sipSessionImplCreateNewSession);
            }
        }

        @Override
        public boolean process(EventObject eventObject) throws SipException {
            if (SipSessionGroup.isLoggable(this, eventObject)) {
                log("process: " + this + ": " + SipSession.State.toString(this.mState) + ": processing " + SipSessionGroup.logEvt(eventObject));
            }
            if (!SipSessionGroup.isRequestEvent("INVITE", eventObject)) {
                if (SipSessionGroup.isRequestEvent("OPTIONS", eventObject)) {
                    SipSessionGroup.this.mSipHelper.sendResponse((RequestEvent) eventObject, 200);
                    return SSCRI_DBG;
                }
                return false;
            }
            processNewInviteRequest((RequestEvent) eventObject);
            return SSCRI_DBG;
        }

        private void log(String str) {
            Rlog.d(SSCRI_TAG, str);
        }
    }

    class SipSessionImpl extends ISipSession.Stub {
        private static final boolean SSI_DBG = true;
        private static final String SSI_TAG = "SipSessionImpl";
        int mAuthenticationRetryCount;
        ClientTransaction mClientTransaction;
        Dialog mDialog;
        boolean mInCall;
        RequestEvent mInviteReceived;
        SipProfile mPeerProfile;
        String mPeerSessionDescription;
        SipSessionImpl mReferSession;
        ReferredByHeader mReferredBy;
        String mReplaces;
        ServerTransaction mServerTransaction;
        SessionTimer mSessionTimer;
        private SipKeepAlive mSipKeepAlive;
        private SipSessionImpl mSipSessionImpl;
        SipSessionListenerProxy mProxy = new SipSessionListenerProxy();
        int mState = 0;

        class SessionTimer {
            private boolean mRunning = SipSessionImpl.SSI_DBG;

            SessionTimer() {
            }

            void start(final int i) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SessionTimer.this.sleep(i);
                        if (SessionTimer.this.mRunning) {
                            SessionTimer.this.timeout();
                        }
                    }
                }, "SipSessionTimerThread").start();
            }

            synchronized void cancel() {
                this.mRunning = false;
                notify();
            }

            private void timeout() {
                synchronized (SipSessionGroup.this) {
                    SipSessionImpl.this.onError(-5, "Session timed out!");
                }
            }

            private synchronized void sleep(int i) {
                try {
                    wait(i * 1000);
                } catch (InterruptedException e) {
                    SipSessionGroup.this.loge("session timer interrupted!", e);
                }
            }
        }

        public SipSessionImpl(ISipSessionListener iSipSessionListener) {
            setListener(iSipSessionListener);
        }

        SipSessionImpl duplicate() {
            return SipSessionGroup.this.new SipSessionImpl(this.mProxy.getListener());
        }

        private void reset() {
            this.mInCall = false;
            SipSessionGroup.this.removeSipSession(this);
            this.mPeerProfile = null;
            this.mState = 0;
            this.mInviteReceived = null;
            this.mPeerSessionDescription = null;
            this.mAuthenticationRetryCount = 0;
            this.mReferSession = null;
            this.mReferredBy = null;
            this.mReplaces = null;
            if (this.mDialog != null) {
                this.mDialog.delete();
            }
            this.mDialog = null;
            try {
                if (this.mServerTransaction != null) {
                    this.mServerTransaction.terminate();
                }
            } catch (ObjectInUseException e) {
            }
            this.mServerTransaction = null;
            try {
                if (this.mClientTransaction != null) {
                    this.mClientTransaction.terminate();
                }
            } catch (ObjectInUseException e2) {
            }
            this.mClientTransaction = null;
            cancelSessionTimer();
            if (this.mSipSessionImpl != null) {
                this.mSipSessionImpl.stopKeepAliveProcess();
                this.mSipSessionImpl = null;
            }
        }

        @Override
        public boolean isInCall() {
            return this.mInCall;
        }

        @Override
        public String getLocalIp() {
            return SipSessionGroup.this.mLocalIp;
        }

        @Override
        public SipProfile getLocalProfile() {
            return SipSessionGroup.this.mLocalProfile;
        }

        @Override
        public SipProfile getPeerProfile() {
            return this.mPeerProfile;
        }

        @Override
        public String getCallId() {
            return SipHelper.getCallId(getTransaction());
        }

        private Transaction getTransaction() {
            if (this.mClientTransaction != null) {
                return this.mClientTransaction;
            }
            if (this.mServerTransaction != null) {
                return this.mServerTransaction;
            }
            return null;
        }

        @Override
        public int getState() {
            return this.mState;
        }

        @Override
        public void setListener(ISipSessionListener iSipSessionListener) {
            SipSessionListenerProxy sipSessionListenerProxy = this.mProxy;
            if (iSipSessionListener instanceof SipSessionListenerProxy) {
                iSipSessionListener = ((SipSessionListenerProxy) iSipSessionListener).getListener();
            }
            sipSessionListenerProxy.setListener(iSipSessionListener);
        }

        private void doCommandAsync(final EventObject eventObject) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SipSessionImpl.this.processCommand(eventObject);
                    } catch (Throwable th) {
                        SipSessionGroup.this.loge("command error: " + eventObject + ": " + SipSessionGroup.this.mLocalProfile.getUriString(), SipSessionGroup.this.getRootCause(th));
                        SipSessionImpl.this.onError(th);
                    }
                }
            }, "SipSessionAsyncCmdThread").start();
        }

        @Override
        public void makeCall(SipProfile sipProfile, String str, int i) {
            doCommandAsync(SipSessionGroup.this.new MakeCallCommand(sipProfile, str, i));
        }

        @Override
        public void answerCall(String str, int i) {
            synchronized (SipSessionGroup.this) {
                if (this.mPeerProfile == null) {
                    return;
                }
                doCommandAsync(SipSessionGroup.this.new MakeCallCommand(this.mPeerProfile, str, i));
            }
        }

        @Override
        public void endCall() {
            doCommandAsync(SipSessionGroup.END_CALL);
        }

        @Override
        public void changeCall(String str, int i) {
            synchronized (SipSessionGroup.this) {
                if (this.mPeerProfile == null) {
                    return;
                }
                doCommandAsync(SipSessionGroup.this.new MakeCallCommand(this.mPeerProfile, str, i));
            }
        }

        @Override
        public void register(int i) {
            doCommandAsync(SipSessionGroup.this.new RegisterCommand(i));
        }

        @Override
        public void unregister() {
            doCommandAsync(SipSessionGroup.DEREGISTER);
        }

        private void processCommand(EventObject eventObject) throws SipException {
            if (SipSessionGroup.isLoggable(eventObject)) {
                log("process cmd: " + eventObject);
            }
            if (!process(eventObject)) {
                onError(-9, "cannot initiate a new transaction to execute: " + eventObject);
            }
        }

        protected String generateTag() {
            return String.valueOf((long) (Math.random() * 4.294967296E9d));
        }

        public String toString() {
            try {
                String string = super.toString();
                return string.substring(string.indexOf("@")) + ":" + SipSession.State.toString(this.mState);
            } catch (Throwable th) {
                return super.toString();
            }
        }

        public boolean process(EventObject eventObject) throws SipException {
            boolean zRegisteringToReady;
            if (SipSessionGroup.isLoggable(this, eventObject)) {
                log(" ~~~~~   " + this + ": " + SipSession.State.toString(this.mState) + ": processing " + SipSessionGroup.logEvt(eventObject));
            }
            synchronized (SipSessionGroup.this) {
                boolean z = false;
                if (SipSessionGroup.this.isClosed()) {
                    return false;
                }
                if (this.mSipKeepAlive != null && this.mSipKeepAlive.process(eventObject)) {
                    return SSI_DBG;
                }
                Dialog dialog = null;
                if (eventObject instanceof RequestEvent) {
                    dialog = ((RequestEvent) eventObject).getDialog();
                } else if (eventObject instanceof ResponseEvent) {
                    dialog = ((ResponseEvent) eventObject).getDialog();
                    SipSessionGroup.this.extractExternalAddress((ResponseEvent) eventObject);
                }
                if (dialog != null) {
                    this.mDialog = dialog;
                }
                switch (this.mState) {
                    case 0:
                        zRegisteringToReady = readyForCall(eventObject);
                        break;
                    case 1:
                    case 2:
                        zRegisteringToReady = registeringToReady(eventObject);
                        break;
                    case 3:
                        zRegisteringToReady = incomingCall(eventObject);
                        break;
                    case SipSession.State.INCOMING_CALL_ANSWERING:
                        zRegisteringToReady = incomingCallToInCall(eventObject);
                        break;
                    case 5:
                    case SipSession.State.OUTGOING_CALL_RING_BACK:
                        zRegisteringToReady = outgoingCall(eventObject);
                        break;
                    case SipSession.State.OUTGOING_CALL_CANCELING:
                        zRegisteringToReady = outgoingCallToReady(eventObject);
                        break;
                    case SipSession.State.IN_CALL:
                        zRegisteringToReady = inCall(eventObject);
                        break;
                    case SipSession.State.PINGING:
                    default:
                        zRegisteringToReady = false;
                        break;
                    case 10:
                        zRegisteringToReady = endingCall(eventObject);
                        break;
                }
                if (zRegisteringToReady || processExceptions(eventObject)) {
                    z = true;
                }
                return z;
            }
        }

        private boolean processExceptions(EventObject eventObject) throws SipException {
            if (SipSessionGroup.isRequestEvent("BYE", eventObject)) {
                SipSessionGroup.this.mSipHelper.sendResponse((RequestEvent) eventObject, 200);
                endCallNormally();
                return SSI_DBG;
            }
            if (SipSessionGroup.isRequestEvent("CANCEL", eventObject)) {
                SipSessionGroup.this.mSipHelper.sendResponse((RequestEvent) eventObject, 481);
                return SSI_DBG;
            }
            if (!(eventObject instanceof TransactionTerminatedEvent)) {
                if (SipSessionGroup.isRequestEvent("OPTIONS", eventObject)) {
                    SipSessionGroup.this.mSipHelper.sendResponse((RequestEvent) eventObject, 200);
                    return SSI_DBG;
                }
                if (eventObject instanceof DialogTerminatedEvent) {
                    processDialogTerminated((DialogTerminatedEvent) eventObject);
                    return SSI_DBG;
                }
                return false;
            }
            TransactionTerminatedEvent transactionTerminatedEvent = (TransactionTerminatedEvent) eventObject;
            if (isCurrentTransaction(transactionTerminatedEvent)) {
                if (eventObject instanceof TimeoutEvent) {
                    processTimeout((TimeoutEvent) eventObject);
                } else {
                    processTransactionTerminated(transactionTerminatedEvent);
                }
                return SSI_DBG;
            }
            return false;
        }

        private void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
            if (this.mDialog == dialogTerminatedEvent.getDialog()) {
                onError((Throwable) new SipException("dialog terminated"));
                return;
            }
            log("not the current dialog; current=" + this.mDialog + ", terminated=" + dialogTerminatedEvent.getDialog());
        }

        private boolean isCurrentTransaction(TransactionTerminatedEvent transactionTerminatedEvent) {
            ServerTransaction serverTransaction;
            ServerTransaction clientTransaction;
            if (transactionTerminatedEvent.isServerTransaction()) {
                serverTransaction = this.mServerTransaction;
            } else {
                serverTransaction = this.mClientTransaction;
            }
            if (transactionTerminatedEvent.isServerTransaction()) {
                clientTransaction = transactionTerminatedEvent.getServerTransaction();
            } else {
                clientTransaction = transactionTerminatedEvent.getClientTransaction();
            }
            if (serverTransaction == clientTransaction || this.mState == 9) {
                if (serverTransaction == null) {
                    return SSI_DBG;
                }
                log("transaction terminated: " + toString(serverTransaction));
                return SSI_DBG;
            }
            log("not the current transaction; current=" + toString(serverTransaction) + ", target=" + toString(clientTransaction));
            return false;
        }

        private String toString(Transaction transaction) {
            if (transaction == null) {
                return "null";
            }
            Request request = transaction.getRequest();
            Dialog dialog = transaction.getDialog();
            CSeqHeader header = request.getHeader("CSeq");
            Object[] objArr = new Object[4];
            objArr[0] = request.getMethod();
            objArr[1] = Long.valueOf(header.getSeqNumber());
            objArr[2] = transaction.getState();
            objArr[3] = dialog == null ? "-" : dialog.getState();
            return String.format("req=%s,%s,s=%s,ds=%s,", objArr);
        }

        private void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
            int i = this.mState;
            if (i == 0 || i == 8) {
                log("Transaction terminated; do nothing");
                return;
            }
            log("Transaction terminated early: " + this);
            onError(-3, "transaction terminated");
        }

        private void processTimeout(TimeoutEvent timeoutEvent) {
            log("processing Timeout...");
            int i = this.mState;
            if (i != 7) {
                switch (i) {
                    case 1:
                    case 2:
                        reset();
                        this.mProxy.onRegistrationTimeout(this);
                        break;
                    case 3:
                    case SipSession.State.INCOMING_CALL_ANSWERING:
                    case 5:
                        break;
                    default:
                        log("   do nothing");
                        break;
                }
            }
            onError(-5, timeoutEvent.toString());
        }

        private int getExpiryTime(Response response) {
            int expires;
            ContactHeader header = response.getHeader("Contact");
            if (header != null) {
                expires = header.getExpires();
            } else {
                expires = -1;
            }
            ExpiresHeader header2 = response.getHeader("Expires");
            if (header2 != null && (expires < 0 || expires > header2.getExpires())) {
                expires = header2.getExpires();
            }
            if (expires <= 0) {
                expires = SipSessionGroup.EXPIRY_TIME;
            }
            ExpiresHeader header3 = response.getHeader("Min-Expires");
            if (header3 != null && expires < header3.getExpires()) {
                expires = header3.getExpires();
            }
            log("Expiry time = " + expires);
            return expires;
        }

        private boolean registeringToReady(EventObject eventObject) throws SipException {
            int expiryTime;
            if (SipSessionGroup.expectResponse("REGISTER", eventObject)) {
                ResponseEvent responseEvent = (ResponseEvent) eventObject;
                Response response = responseEvent.getResponse();
                int statusCode = response.getStatusCode();
                if (statusCode == 200) {
                    if (this.mState == 1) {
                        expiryTime = getExpiryTime(responseEvent.getResponse());
                    } else {
                        expiryTime = -1;
                    }
                    onRegistrationDone(expiryTime);
                    return SSI_DBG;
                }
                if (statusCode == 401 || statusCode == 407) {
                    handleAuthentication(responseEvent);
                    return SSI_DBG;
                }
                if (statusCode >= 500) {
                    onRegistrationFailed(response);
                    return SSI_DBG;
                }
                return false;
            }
            return false;
        }

        private boolean handleAuthentication(ResponseEvent responseEvent) throws SipException {
            Response response = responseEvent.getResponse();
            if (getNonceFromResponse(response) == null) {
                onError(-2, "server does not provide challenge");
                return false;
            }
            if (this.mAuthenticationRetryCount < 2) {
                this.mClientTransaction = SipSessionGroup.this.mSipHelper.handleChallenge(responseEvent, getAccountManager());
                this.mDialog = this.mClientTransaction.getDialog();
                this.mAuthenticationRetryCount++;
                if (SipSessionGroup.isLoggable(this, responseEvent)) {
                    log("   authentication retry count=" + this.mAuthenticationRetryCount);
                }
                return SSI_DBG;
            }
            if (crossDomainAuthenticationRequired(response)) {
                onError(-11, getRealmFromResponse(response));
            } else {
                onError(-8, "incorrect username or password");
            }
            return false;
        }

        private boolean crossDomainAuthenticationRequired(Response response) {
            String realmFromResponse = getRealmFromResponse(response);
            if (realmFromResponse == null) {
                realmFromResponse = "";
            }
            return SipSessionGroup.this.mLocalProfile.getSipDomain().trim().equals(realmFromResponse.trim()) ^ SSI_DBG;
        }

        private AccountManager getAccountManager() {
            return new AccountManager() {
                public UserCredentials getCredentials(ClientTransaction clientTransaction, String str) {
                    return new UserCredentials() {
                        public String getUserName() {
                            String authUserName = SipSessionGroup.this.mLocalProfile.getAuthUserName();
                            if (TextUtils.isEmpty(authUserName)) {
                                return SipSessionGroup.this.mLocalProfile.getUserName();
                            }
                            return authUserName;
                        }

                        public String getPassword() {
                            return SipSessionGroup.this.mPassword;
                        }

                        public String getSipDomain() {
                            return SipSessionGroup.this.mLocalProfile.getSipDomain();
                        }
                    };
                }
            };
        }

        private String getRealmFromResponse(Response response) {
            WWWAuthenticate header = response.getHeader("WWW-Authenticate");
            if (header != null) {
                return header.getRealm();
            }
            ProxyAuthenticate header2 = response.getHeader("Proxy-Authenticate");
            if (header2 == null) {
                return null;
            }
            return header2.getRealm();
        }

        private String getNonceFromResponse(Response response) {
            WWWAuthenticate header = response.getHeader("WWW-Authenticate");
            if (header != null) {
                return header.getNonce();
            }
            ProxyAuthenticate header2 = response.getHeader("Proxy-Authenticate");
            if (header2 == null) {
                return null;
            }
            return header2.getNonce();
        }

        private String getResponseString(int i) {
            StatusLine statusLine = new StatusLine();
            statusLine.setStatusCode(i);
            statusLine.setReasonPhrase(SIPResponse.getReasonPhrase(i));
            return statusLine.encode();
        }

        private boolean readyForCall(EventObject eventObject) throws SipException {
            if (eventObject instanceof MakeCallCommand) {
                this.mState = 5;
                MakeCallCommand makeCallCommand = (MakeCallCommand) eventObject;
                this.mPeerProfile = makeCallCommand.getPeerProfile();
                if (this.mReferSession != null) {
                    SipSessionGroup.this.mSipHelper.sendReferNotify(this.mReferSession.mDialog, getResponseString(100));
                }
                this.mClientTransaction = SipSessionGroup.this.mSipHelper.sendInvite(SipSessionGroup.this.mLocalProfile, this.mPeerProfile, makeCallCommand.getSessionDescription(), generateTag(), this.mReferredBy, this.mReplaces);
                this.mDialog = this.mClientTransaction.getDialog();
                SipSessionGroup.this.addSipSession(this);
                startSessionTimer(makeCallCommand.getTimeout());
                this.mProxy.onCalling(this);
                return SSI_DBG;
            }
            if (!(eventObject instanceof RegisterCommand)) {
                if (SipSessionGroup.DEREGISTER != eventObject) {
                    return false;
                }
                this.mState = 2;
                this.mClientTransaction = SipSessionGroup.this.mSipHelper.sendRegister(SipSessionGroup.this.mLocalProfile, generateTag(), 0);
                this.mDialog = this.mClientTransaction.getDialog();
                SipSessionGroup.this.addSipSession(this);
                this.mProxy.onRegistering(this);
                return SSI_DBG;
            }
            this.mState = 1;
            this.mClientTransaction = SipSessionGroup.this.mSipHelper.sendRegister(SipSessionGroup.this.mLocalProfile, generateTag(), ((RegisterCommand) eventObject).getDuration());
            this.mDialog = this.mClientTransaction.getDialog();
            SipSessionGroup.this.addSipSession(this);
            this.mProxy.onRegistering(this);
            return SSI_DBG;
        }

        private boolean incomingCall(EventObject eventObject) throws SipException {
            if (!(eventObject instanceof MakeCallCommand)) {
                if (SipSessionGroup.END_CALL == eventObject) {
                    SipSessionGroup.this.mSipHelper.sendInviteBusyHere(this.mInviteReceived, this.mServerTransaction);
                    endCallNormally();
                    return SSI_DBG;
                }
                if (SipSessionGroup.isRequestEvent("CANCEL", eventObject)) {
                    SipSessionGroup.this.mSipHelper.sendResponse((RequestEvent) eventObject, 200);
                    SipSessionGroup.this.mSipHelper.sendInviteRequestTerminated(this.mInviteReceived.getRequest(), this.mServerTransaction);
                    endCallNormally();
                    return SSI_DBG;
                }
                return false;
            }
            this.mState = 4;
            MakeCallCommand makeCallCommand = (MakeCallCommand) eventObject;
            this.mServerTransaction = SipSessionGroup.this.mSipHelper.sendInviteOk(this.mInviteReceived, SipSessionGroup.this.mLocalProfile, makeCallCommand.getSessionDescription(), this.mServerTransaction, SipSessionGroup.this.mExternalIp, SipSessionGroup.this.mExternalPort);
            startSessionTimer(makeCallCommand.getTimeout());
            return SSI_DBG;
        }

        private boolean incomingCallToInCall(EventObject eventObject) {
            if (SipSessionGroup.isRequestEvent("ACK", eventObject)) {
                String strExtractContent = SipSessionGroup.this.extractContent(((RequestEvent) eventObject).getRequest());
                if (strExtractContent != null) {
                    this.mPeerSessionDescription = strExtractContent;
                }
                if (this.mPeerSessionDescription == null) {
                    onError(-4, "peer sdp is empty");
                } else {
                    establishCall(false);
                }
                return SSI_DBG;
            }
            if (SipSessionGroup.isRequestEvent("CANCEL", eventObject)) {
                return SSI_DBG;
            }
            return false;
        }

        private boolean outgoingCall(EventObject eventObject) throws SipException {
            if (!SipSessionGroup.expectResponse("INVITE", eventObject)) {
                if (SipSessionGroup.END_CALL != eventObject) {
                    if (!SipSessionGroup.isRequestEvent("INVITE", eventObject)) {
                        return false;
                    }
                    RequestEvent requestEvent = (RequestEvent) eventObject;
                    SipSessionGroup.this.mSipHelper.sendInviteBusyHere(requestEvent, requestEvent.getServerTransaction());
                    return SSI_DBG;
                }
                this.mState = 7;
                SipSessionGroup.this.mSipHelper.sendCancel(this.mClientTransaction);
                startSessionTimer(3);
                return SSI_DBG;
            }
            ResponseEvent responseEvent = (ResponseEvent) eventObject;
            Response response = responseEvent.getResponse();
            int statusCode = response.getStatusCode();
            if (statusCode == 200) {
                if (this.mReferSession != null) {
                    SipSessionGroup.this.mSipHelper.sendReferNotify(this.mReferSession.mDialog, getResponseString(200));
                    this.mReferSession = null;
                }
                SipSessionGroup.this.mSipHelper.sendInviteAck(responseEvent, this.mDialog);
                this.mPeerSessionDescription = SipSessionGroup.this.extractContent(response);
                establishCall(SSI_DBG);
                return SSI_DBG;
            }
            if (statusCode == 401 || statusCode == 407) {
                if (handleAuthentication(responseEvent)) {
                    SipSessionGroup.this.addSipSession(this);
                }
                return SSI_DBG;
            }
            if (statusCode == 491) {
                return SSI_DBG;
            }
            switch (statusCode) {
                case 180:
                case 181:
                case 182:
                case 183:
                    if (this.mState == 5) {
                        this.mState = 6;
                        cancelSessionTimer();
                        this.mProxy.onRingingBack(this);
                    }
                    return SSI_DBG;
                default:
                    if (this.mReferSession != null) {
                        SipSessionGroup.this.mSipHelper.sendReferNotify(this.mReferSession.mDialog, getResponseString(503));
                    }
                    if (statusCode >= 400) {
                        onError(response);
                        return SSI_DBG;
                    }
                    if (statusCode >= 300) {
                        return false;
                    }
                    return SSI_DBG;
            }
        }

        private boolean outgoingCallToReady(EventObject eventObject) throws SipException {
            if (eventObject instanceof ResponseEvent) {
                Response response = ((ResponseEvent) eventObject).getResponse();
                int statusCode = response.getStatusCode();
                if (!SipSessionGroup.expectResponse("CANCEL", eventObject)) {
                    if (!SipSessionGroup.expectResponse("INVITE", eventObject)) {
                        return false;
                    }
                    if (statusCode == 200) {
                        outgoingCall(eventObject);
                        return SSI_DBG;
                    }
                    if (statusCode == 487) {
                        endCallNormally();
                        return SSI_DBG;
                    }
                } else if (statusCode == 200) {
                    return SSI_DBG;
                }
                if (statusCode >= 400) {
                    onError(response);
                    return SSI_DBG;
                }
            } else if (eventObject instanceof TransactionTerminatedEvent) {
                onError((Throwable) new SipException("timed out"));
            }
            return false;
        }

        private boolean processReferRequest(RequestEvent requestEvent) throws SipException {
            try {
                ReferToHeader header = requestEvent.getRequest().getHeader("Refer-To");
                SipURI uri = header.getAddress().getURI();
                String header2 = uri.getHeader("Replaces");
                if (uri.getUser() == null) {
                    SipSessionGroup.this.mSipHelper.sendResponse(requestEvent, 400);
                    return false;
                }
                SipSessionGroup.this.mSipHelper.sendResponse(requestEvent, 202);
                SipSessionImpl sipSessionImplCreateNewSession = SipSessionGroup.this.createNewSession(requestEvent, this.mProxy.getListener(), SipSessionGroup.this.mSipHelper.getServerTransaction(requestEvent), 0);
                sipSessionImplCreateNewSession.mReferSession = this;
                sipSessionImplCreateNewSession.mReferredBy = requestEvent.getRequest().getHeader("Referred-By");
                sipSessionImplCreateNewSession.mReplaces = header2;
                sipSessionImplCreateNewSession.mPeerProfile = SipSessionGroup.createPeerProfile(header);
                sipSessionImplCreateNewSession.mProxy.onCallTransferring(sipSessionImplCreateNewSession, null);
                return SSI_DBG;
            } catch (IllegalArgumentException e) {
                throw new SipException("createPeerProfile()", e);
            }
        }

        private boolean inCall(EventObject eventObject) throws SipException {
            if (SipSessionGroup.END_CALL != eventObject) {
                if (!SipSessionGroup.isRequestEvent("INVITE", eventObject)) {
                    if (SipSessionGroup.isRequestEvent("BYE", eventObject)) {
                        SipSessionGroup.this.mSipHelper.sendResponse((RequestEvent) eventObject, 200);
                        endCallNormally();
                        return SSI_DBG;
                    }
                    if (SipSessionGroup.isRequestEvent("REFER", eventObject)) {
                        return processReferRequest((RequestEvent) eventObject);
                    }
                    if (eventObject instanceof MakeCallCommand) {
                        this.mState = 5;
                        MakeCallCommand makeCallCommand = (MakeCallCommand) eventObject;
                        this.mClientTransaction = SipSessionGroup.this.mSipHelper.sendReinvite(this.mDialog, makeCallCommand.getSessionDescription());
                        startSessionTimer(makeCallCommand.getTimeout());
                        return SSI_DBG;
                    }
                    if ((eventObject instanceof ResponseEvent) && SipSessionGroup.expectResponse("NOTIFY", eventObject)) {
                        return SSI_DBG;
                    }
                    return false;
                }
                this.mState = 3;
                RequestEvent requestEvent = (RequestEvent) eventObject;
                this.mInviteReceived = requestEvent;
                this.mPeerSessionDescription = SipSessionGroup.this.extractContent(requestEvent.getRequest());
                this.mServerTransaction = null;
                this.mProxy.onRinging(this, this.mPeerProfile, this.mPeerSessionDescription);
                return SSI_DBG;
            }
            this.mState = 10;
            SipSessionGroup.this.mSipHelper.sendBye(this.mDialog);
            this.mProxy.onCallEnded(this);
            startSessionTimer(3);
            return SSI_DBG;
        }

        private boolean endingCall(EventObject eventObject) throws SipException {
            if (SipSessionGroup.expectResponse("BYE", eventObject)) {
                ResponseEvent responseEvent = (ResponseEvent) eventObject;
                int statusCode = responseEvent.getResponse().getStatusCode();
                if ((statusCode == 401 || statusCode == 407) && handleAuthentication(responseEvent)) {
                    return SSI_DBG;
                }
                cancelSessionTimer();
                reset();
                return SSI_DBG;
            }
            return false;
        }

        private void startSessionTimer(int i) {
            if (i > 0) {
                this.mSessionTimer = new SessionTimer();
                this.mSessionTimer.start(i);
            }
        }

        private void cancelSessionTimer() {
            if (this.mSessionTimer != null) {
                this.mSessionTimer.cancel();
                this.mSessionTimer = null;
            }
        }

        private String createErrorMessage(Response response) {
            return String.format("%s (%d)", response.getReasonPhrase(), Integer.valueOf(response.getStatusCode()));
        }

        private void enableKeepAlive() {
            if (this.mSipSessionImpl != null) {
                this.mSipSessionImpl.stopKeepAliveProcess();
            } else {
                this.mSipSessionImpl = duplicate();
            }
            try {
                this.mSipSessionImpl.startKeepAliveProcess(10, this.mPeerProfile, null);
            } catch (SipException e) {
                SipSessionGroup.this.loge("keepalive cannot be enabled; ignored", e);
                this.mSipSessionImpl.stopKeepAliveProcess();
            }
        }

        private void establishCall(boolean z) {
            this.mState = 8;
            cancelSessionTimer();
            if (!this.mInCall && z) {
                enableKeepAlive();
            }
            this.mInCall = SSI_DBG;
            this.mProxy.onCallEstablished(this, this.mPeerSessionDescription);
        }

        private void endCallNormally() {
            reset();
            this.mProxy.onCallEnded(this);
        }

        private void endCallOnError(int i, String str) {
            reset();
            this.mProxy.onError(this, i, str);
        }

        private void endCallOnBusy() {
            reset();
            this.mProxy.onCallBusy(this);
        }

        private void onError(int i, String str) {
            cancelSessionTimer();
            switch (this.mState) {
                case 1:
                case 2:
                    onRegistrationFailed(i, str);
                    break;
                default:
                    endCallOnError(i, str);
                    break;
            }
        }

        private void onError(Throwable th) {
            Throwable rootCause = SipSessionGroup.this.getRootCause(th);
            onError(getErrorCode(rootCause), rootCause.toString());
        }

        private void onError(Response response) {
            int statusCode = response.getStatusCode();
            if (!this.mInCall && statusCode == 486) {
                endCallOnBusy();
            } else {
                onError(getErrorCode(statusCode), createErrorMessage(response));
            }
        }

        private int getErrorCode(int i) {
            switch (i) {
                case 403:
                case 404:
                case 406:
                case 410:
                case 480:
                case 488:
                    return -7;
                case 408:
                    return -5;
                case 414:
                case 484:
                case 485:
                    return -6;
                default:
                    if (i < 500) {
                        return -4;
                    }
                    return -2;
            }
        }

        private int getErrorCode(Throwable th) {
            th.getMessage();
            if (th instanceof UnknownHostException) {
                return -12;
            }
            if (th instanceof IOException) {
                return -1;
            }
            return -4;
        }

        private void onRegistrationDone(int i) {
            reset();
            this.mProxy.onRegistrationDone(this, i);
        }

        private void onRegistrationFailed(int i, String str) {
            reset();
            this.mProxy.onRegistrationFailed(this, i, str);
        }

        private void onRegistrationFailed(Response response) {
            onRegistrationFailed(getErrorCode(response.getStatusCode()), createErrorMessage(response));
        }

        public void startKeepAliveProcess(int i, KeepAliveProcessCallback keepAliveProcessCallback) throws SipException {
            synchronized (SipSessionGroup.this) {
                startKeepAliveProcess(i, SipSessionGroup.this.mLocalProfile, keepAliveProcessCallback);
            }
        }

        public void startKeepAliveProcess(int i, SipProfile sipProfile, KeepAliveProcessCallback keepAliveProcessCallback) throws SipException {
            synchronized (SipSessionGroup.this) {
                if (this.mSipKeepAlive != null) {
                    throw new SipException("Cannot create more than one keepalive process in a SipSession");
                }
                this.mPeerProfile = sipProfile;
                this.mSipKeepAlive = new SipKeepAlive();
                this.mProxy.setListener(this.mSipKeepAlive);
                this.mSipKeepAlive.start(i, keepAliveProcessCallback);
            }
        }

        public void stopKeepAliveProcess() {
            synchronized (SipSessionGroup.this) {
                if (this.mSipKeepAlive != null) {
                    this.mSipKeepAlive.stop();
                    this.mSipKeepAlive = null;
                }
            }
        }

        class SipKeepAlive extends SipSessionAdapter implements Runnable {
            private static final boolean SKA_DBG = true;
            private static final String SKA_TAG = "SipKeepAlive";
            private KeepAliveProcessCallback mCallback;
            private int mInterval;
            private boolean mRunning = false;
            private boolean mPortChanged = false;
            private int mRPort = 0;

            SipKeepAlive() {
            }

            void start(int i, KeepAliveProcessCallback keepAliveProcessCallback) {
                if (this.mRunning) {
                    return;
                }
                this.mRunning = SKA_DBG;
                this.mInterval = i;
                this.mCallback = new KeepAliveProcessCallbackProxy(keepAliveProcessCallback);
                SipSessionGroup.this.mWakeupTimer.set(i * 1000, this);
                log("start keepalive:" + SipSessionGroup.this.mLocalProfile.getUriString());
                run();
            }

            boolean process(EventObject eventObject) {
                if (this.mRunning && SipSessionImpl.this.mState == 9 && (eventObject instanceof ResponseEvent) && parseOptionsResult(eventObject)) {
                    if (!this.mPortChanged) {
                        SipSessionImpl.this.cancelSessionTimer();
                        SipSessionGroup.this.removeSipSession(SipSessionImpl.this);
                    } else {
                        SipSessionGroup.this.resetExternalAddress();
                        stop();
                    }
                    this.mCallback.onResponse(this.mPortChanged);
                    return SKA_DBG;
                }
                return false;
            }

            @Override
            public void onError(ISipSession iSipSession, int i, String str) {
                stop();
                this.mCallback.onError(i, str);
            }

            @Override
            public void run() {
                synchronized (SipSessionGroup.this) {
                    if (this.mRunning) {
                        try {
                            sendKeepAlive();
                        } catch (Throwable th) {
                            SipSessionGroup.this.loge("keepalive error: " + SipSessionGroup.this.mLocalProfile.getUriString(), SipSessionGroup.this.getRootCause(th));
                            if (this.mRunning) {
                                SipSessionImpl.this.onError(th);
                            }
                        }
                    }
                }
            }

            void stop() {
                synchronized (SipSessionGroup.this) {
                    log("stop keepalive:" + SipSessionGroup.this.mLocalProfile.getUriString() + ",RPort=" + this.mRPort);
                    this.mRunning = false;
                    SipSessionGroup.this.mWakeupTimer.cancel(this);
                    SipSessionImpl.this.reset();
                }
            }

            private void sendKeepAlive() throws SipException {
                synchronized (SipSessionGroup.this) {
                    SipSessionImpl.this.mState = 9;
                    SipSessionImpl.this.mClientTransaction = SipSessionGroup.this.mSipHelper.sendOptions(SipSessionGroup.this.mLocalProfile, SipSessionImpl.this.mPeerProfile, SipSessionImpl.this.generateTag());
                    SipSessionImpl.this.mDialog = SipSessionImpl.this.mClientTransaction.getDialog();
                    SipSessionGroup.this.addSipSession(SipSessionImpl.this);
                    SipSessionImpl.this.startSessionTimer(5);
                }
            }

            private boolean parseOptionsResult(EventObject eventObject) {
                if (!SipSessionGroup.expectResponse("OPTIONS", eventObject)) {
                    return false;
                }
                int rPortFromResponse = getRPortFromResponse(((ResponseEvent) eventObject).getResponse());
                if (rPortFromResponse != -1) {
                    if (this.mRPort == 0) {
                        this.mRPort = rPortFromResponse;
                    }
                    if (this.mRPort != rPortFromResponse) {
                        this.mPortChanged = SKA_DBG;
                        log(String.format("rport is changed: %d <> %d", Integer.valueOf(this.mRPort), Integer.valueOf(rPortFromResponse)));
                        this.mRPort = rPortFromResponse;
                    } else {
                        log("rport is the same: " + rPortFromResponse);
                    }
                } else {
                    log("peer did not respond rport");
                }
                return SKA_DBG;
            }

            private int getRPortFromResponse(Response response) {
                ViaHeader header = response.getHeader("Via");
                if (header == null) {
                    return -1;
                }
                return header.getRPort();
            }

            private void log(String str) {
                Rlog.d(SKA_TAG, str);
            }
        }

        private void log(String str) {
            Rlog.d(SSI_TAG, str);
        }
    }

    private static boolean isRequestEvent(String str, EventObject eventObject) {
        try {
            if (eventObject instanceof RequestEvent) {
                return str.equals(((RequestEvent) eventObject).getRequest().getMethod());
            }
            return false;
        } catch (Throwable th) {
            return false;
        }
    }

    private static String getCseqMethod(Message message) {
        return message.getHeader("CSeq").getMethod();
    }

    private static boolean expectResponse(String str, EventObject eventObject) {
        if (eventObject instanceof ResponseEvent) {
            return str.equalsIgnoreCase(getCseqMethod(((ResponseEvent) eventObject).getResponse()));
        }
        return false;
    }

    private static SipProfile createPeerProfile(HeaderAddress headerAddress) throws SipException {
        try {
            Address address = headerAddress.getAddress();
            SipURI uri = address.getURI();
            String user = uri.getUser();
            if (user == null) {
                user = ANONYMOUS;
            }
            int port = uri.getPort();
            SipProfile.Builder displayName = new SipProfile.Builder(user, uri.getHost()).setDisplayName(address.getDisplayName());
            if (port > 0) {
                displayName.setPort(port);
            }
            return displayName.build();
        } catch (IllegalArgumentException e) {
            throw new SipException("createPeerProfile()", e);
        } catch (ParseException e2) {
            throw new SipException("createPeerProfile()", e2);
        }
    }

    private static boolean isLoggable(SipSessionImpl sipSessionImpl) {
        return (sipSessionImpl == null || sipSessionImpl.mState != 9) ? false : false;
    }

    private static boolean isLoggable(EventObject eventObject) {
        return isLoggable(null, eventObject);
    }

    private static boolean isLoggable(SipSessionImpl sipSessionImpl, EventObject eventObject) {
        if (isLoggable(sipSessionImpl) && eventObject != null) {
            return eventObject instanceof ResponseEvent ? "OPTIONS".equals(((ResponseEvent) eventObject).getResponse().getHeader("CSeq")) ? false : false : ((eventObject instanceof RequestEvent) && isRequestEvent("OPTIONS", eventObject)) ? false : false;
        }
        return false;
    }

    private static String logEvt(EventObject eventObject) {
        if (eventObject instanceof RequestEvent) {
            return ((RequestEvent) eventObject).getRequest().toString();
        }
        if (eventObject instanceof ResponseEvent) {
            return ((ResponseEvent) eventObject).getResponse().toString();
        }
        return eventObject.toString();
    }

    private class RegisterCommand extends EventObject {
        private int mDuration;

        public RegisterCommand(int i) {
            super(SipSessionGroup.this);
            this.mDuration = i;
        }

        public int getDuration() {
            return this.mDuration;
        }
    }

    private class MakeCallCommand extends EventObject {
        private String mSessionDescription;
        private int mTimeout;

        public MakeCallCommand(SipProfile sipProfile, String str, int i) {
            super(sipProfile);
            this.mSessionDescription = str;
            this.mTimeout = i;
        }

        public SipProfile getPeerProfile() {
            return (SipProfile) getSource();
        }

        public String getSessionDescription() {
            return this.mSessionDescription;
        }

        public int getTimeout() {
            return this.mTimeout;
        }
    }

    static class KeepAliveProcessCallbackProxy implements KeepAliveProcessCallback {
        private static final String KAPCP_TAG = "KeepAliveProcessCallbackProxy";
        private KeepAliveProcessCallback mCallback;

        KeepAliveProcessCallbackProxy(KeepAliveProcessCallback keepAliveProcessCallback) {
            this.mCallback = keepAliveProcessCallback;
        }

        private void proxy(Runnable runnable) {
            new Thread(runnable, "SIP-KeepAliveProcessCallbackThread").start();
        }

        @Override
        public void onResponse(final boolean z) {
            if (this.mCallback == null) {
                return;
            }
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        KeepAliveProcessCallbackProxy.this.mCallback.onResponse(z);
                    } catch (Throwable th) {
                        KeepAliveProcessCallbackProxy.this.loge("onResponse", th);
                    }
                }
            });
        }

        @Override
        public void onError(final int i, final String str) {
            if (this.mCallback == null) {
                return;
            }
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        KeepAliveProcessCallbackProxy.this.mCallback.onError(i, str);
                    } catch (Throwable th) {
                        KeepAliveProcessCallbackProxy.this.loge("onError", th);
                    }
                }
            });
        }

        private void loge(String str, Throwable th) {
            Rlog.e(KAPCP_TAG, str, th);
        }
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }

    private void loge(String str, Throwable th) {
        Rlog.e(TAG, str, th);
    }
}
