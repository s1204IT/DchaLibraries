package gov.nist.javax.sip;

import gov.nist.core.ThreadAuditor;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPDialog;
import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.stack.SIPTransaction;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.ListIterator;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipListener;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;

class EventScanner implements Runnable {
    private int[] eventMutex = {0};
    private boolean isStopped;
    private LinkedList pendingEvents;
    private int refCount;
    private SipStackImpl sipStack;

    public void incrementRefcount() {
        synchronized (this.eventMutex) {
            this.refCount++;
        }
    }

    public EventScanner(SipStackImpl sipStackImpl) {
        this.pendingEvents = new LinkedList();
        this.pendingEvents = new LinkedList();
        Thread thread = new Thread(this);
        thread.setDaemon(false);
        this.sipStack = sipStackImpl;
        thread.setName("EventScannerThread");
        thread.start();
    }

    public void addEvent(EventWrapper eventWrapper) {
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("addEvent " + eventWrapper);
        }
        synchronized (this.eventMutex) {
            this.pendingEvents.add(eventWrapper);
            this.eventMutex.notify();
        }
    }

    public void stop() {
        synchronized (this.eventMutex) {
            if (this.refCount > 0) {
                this.refCount--;
            }
            if (this.refCount == 0) {
                this.isStopped = true;
                this.eventMutex.notify();
            }
        }
    }

    public void forceStop() {
        synchronized (this.eventMutex) {
            this.isStopped = true;
            this.refCount = 0;
            this.eventMutex.notify();
        }
    }

    public void deliverEvent(EventWrapper eventWrapper) {
        SIPDialog sIPDialog;
        boolean zEquals;
        EventObject eventObject = eventWrapper.sipEvent;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("sipEvent = " + eventObject + "source = " + eventObject.getSource());
        }
        boolean z = eventObject instanceof IOExceptionEvent;
        SipListener sipListener = !z ? ((SipProviderImpl) eventObject.getSource()).getSipListener() : this.sipStack.getSipListener();
        if (eventObject instanceof RequestEvent) {
            try {
                SIPRequest sIPRequest = (SIPRequest) ((RequestEvent) eventObject).getRequest();
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("deliverEvent : " + sIPRequest.getFirstLine() + " transaction " + eventWrapper.transaction + " sipEvent.serverTx = " + ((RequestEvent) eventObject).getServerTransaction());
                }
                SIPServerTransaction sIPServerTransaction = (SIPServerTransaction) this.sipStack.findTransaction(sIPRequest, true);
                if (sIPServerTransaction == null || sIPServerTransaction.passToListener()) {
                    if (this.sipStack.findPendingTransaction(sIPRequest) != null) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("transaction already exists!!");
                        }
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("Done processing Message " + ((SIPRequest) ((RequestEvent) eventObject).getRequest()).getFirstLine());
                        }
                        if (eventWrapper.transaction != null && ((SIPServerTransaction) eventWrapper.transaction).passToListener()) {
                            ((SIPServerTransaction) eventWrapper.transaction).releaseSem();
                        }
                        if (eventWrapper.transaction != null) {
                            this.sipStack.removePendingTransaction((SIPServerTransaction) eventWrapper.transaction);
                        }
                        if (eventWrapper.transaction.getOriginalRequest().getMethod().equals("ACK")) {
                            eventWrapper.transaction.setState(TransactionState.TERMINATED);
                            return;
                        }
                        return;
                    }
                    this.sipStack.putPendingTransaction((SIPServerTransaction) eventWrapper.transaction);
                } else {
                    if (!sIPRequest.getMethod().equals("ACK") || !sIPServerTransaction.isInviteTransaction() || (sIPServerTransaction.getLastResponse().getStatusCode() / 100 != 2 && !this.sipStack.isNon2XXAckPassedToListener())) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("transaction already exists! " + sIPServerTransaction);
                        }
                        if (zEquals) {
                            return;
                        } else {
                            return;
                        }
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Detected broken client sending ACK with same branch! Passing...");
                    }
                }
                sIPRequest.setTransaction(eventWrapper.transaction);
                try {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Calling listener " + sIPRequest.getFirstLine());
                        this.sipStack.getStackLogger().logDebug("Calling listener " + eventWrapper.transaction);
                    }
                    if (sipListener != null) {
                        sipListener.processRequest((RequestEvent) eventObject);
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Done processing Message " + sIPRequest.getFirstLine());
                    }
                    if (eventWrapper.transaction != null && (sIPDialog = (SIPDialog) eventWrapper.transaction.getDialog()) != null) {
                        sIPDialog.requestConsumed();
                    }
                } catch (Exception e) {
                    this.sipStack.getStackLogger().logException(e);
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Done processing Message " + ((SIPRequest) ((RequestEvent) eventObject).getRequest()).getFirstLine());
                }
                if (eventWrapper.transaction != null && ((SIPServerTransaction) eventWrapper.transaction).passToListener()) {
                    ((SIPServerTransaction) eventWrapper.transaction).releaseSem();
                }
                if (eventWrapper.transaction != null) {
                    this.sipStack.removePendingTransaction((SIPServerTransaction) eventWrapper.transaction);
                }
                if (eventWrapper.transaction.getOriginalRequest().getMethod().equals("ACK")) {
                    eventWrapper.transaction.setState(TransactionState.TERMINATED);
                    return;
                }
                return;
            } finally {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Done processing Message " + ((SIPRequest) ((RequestEvent) eventObject).getRequest()).getFirstLine());
                }
                if (eventWrapper.transaction != null && ((SIPServerTransaction) eventWrapper.transaction).passToListener()) {
                    ((SIPServerTransaction) eventWrapper.transaction).releaseSem();
                }
                if (eventWrapper.transaction != null) {
                    this.sipStack.removePendingTransaction((SIPServerTransaction) eventWrapper.transaction);
                }
                if (eventWrapper.transaction.getOriginalRequest().getMethod().equals("ACK")) {
                    eventWrapper.transaction.setState(TransactionState.TERMINATED);
                }
            }
        }
        if (eventObject instanceof ResponseEvent) {
            try {
                ResponseEvent responseEvent = (ResponseEvent) eventObject;
                SIPResponse sIPResponse = (SIPResponse) responseEvent.getResponse();
                SIPDialog sIPDialog2 = (SIPDialog) responseEvent.getDialog();
                try {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Calling listener for " + sIPResponse.getFirstLine());
                    }
                    if (sipListener != null) {
                        SIPTransaction sIPTransaction = eventWrapper.transaction;
                        if (sIPTransaction != null) {
                            sIPTransaction.setPassToListener();
                        }
                        sipListener.processResponse((ResponseEvent) eventObject);
                    }
                    if (sIPDialog2 != null && ((sIPDialog2.getState() == null || !sIPDialog2.getState().equals(DialogState.TERMINATED)) && (sIPResponse.getStatusCode() == 481 || sIPResponse.getStatusCode() == 408))) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("Removing dialog on 408 or 481 response");
                        }
                        sIPDialog2.doDeferredDelete();
                    }
                    if (sIPResponse.getCSeq().getMethod().equals("INVITE") && sIPDialog2 != null && sIPResponse.getStatusCode() == 200) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("Warning! unacknowledged dialog. " + sIPDialog2.getState());
                        }
                        sIPDialog2.doDeferredDeleteIfNoAckSent(sIPResponse.getCSeq().getSeqNumber());
                    }
                } catch (Exception e2) {
                    this.sipStack.getStackLogger().logException(e2);
                }
                SIPClientTransaction sIPClientTransaction = (SIPClientTransaction) eventWrapper.transaction;
                if (sIPClientTransaction != null && TransactionState.COMPLETED == sIPClientTransaction.getState() && sIPClientTransaction.getOriginalRequest() != null && !sIPClientTransaction.getOriginalRequest().getMethod().equals("INVITE")) {
                    sIPClientTransaction.clearState();
                }
                if (eventWrapper.transaction == null || !eventWrapper.transaction.passToListener()) {
                    return;
                }
                eventWrapper.transaction.releaseSem();
                return;
            } catch (Throwable th) {
                if (eventWrapper.transaction != null && eventWrapper.transaction.passToListener()) {
                    eventWrapper.transaction.releaseSem();
                }
                throw th;
            }
        }
        if (eventObject instanceof TimeoutEvent) {
            if (sipListener != null) {
                try {
                    sipListener.processTimeout((TimeoutEvent) eventObject);
                    return;
                } catch (Exception e3) {
                    this.sipStack.getStackLogger().logException(e3);
                    return;
                }
            }
            return;
        }
        if (eventObject instanceof DialogTimeoutEvent) {
            if (sipListener != null) {
                try {
                    if (sipListener instanceof SipListenerExt) {
                        ((SipListenerExt) sipListener).processDialogTimeout((DialogTimeoutEvent) eventObject);
                        return;
                    }
                    return;
                } catch (Exception e4) {
                    this.sipStack.getStackLogger().logException(e4);
                    return;
                }
            }
            return;
        }
        if (z) {
            if (sipListener != null) {
                try {
                    sipListener.processIOException((IOExceptionEvent) eventObject);
                    return;
                } catch (Exception e5) {
                    this.sipStack.getStackLogger().logException(e5);
                    return;
                }
            }
            return;
        }
        if (!(eventObject instanceof TransactionTerminatedEvent)) {
            if (!(eventObject instanceof DialogTerminatedEvent)) {
                this.sipStack.getStackLogger().logFatalError("bad event" + eventObject);
                return;
            }
            if (sipListener != null) {
                try {
                    sipListener.processDialogTerminated((DialogTerminatedEvent) eventObject);
                    return;
                } catch (AbstractMethodError e6) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logWarning("Unable to call sipListener.processDialogTerminated");
                        return;
                    }
                    return;
                } catch (Exception e7) {
                    this.sipStack.getStackLogger().logException(e7);
                    return;
                }
            }
            return;
        }
        try {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("About to deliver transactionTerminatedEvent");
                this.sipStack.getStackLogger().logDebug("tx = " + ((TransactionTerminatedEvent) eventObject).getClientTransaction());
                this.sipStack.getStackLogger().logDebug("tx = " + ((TransactionTerminatedEvent) eventObject).getServerTransaction());
            }
            if (sipListener != null) {
                sipListener.processTransactionTerminated((TransactionTerminatedEvent) eventObject);
            }
        } catch (AbstractMethodError e8) {
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logWarning("Unable to call sipListener.processTransactionTerminated");
            }
        } catch (Exception e9) {
            this.sipStack.getStackLogger().logException(e9);
        }
    }

    @Override
    public void run() {
        LinkedList linkedList;
        boolean zIsLoggingEnabled;
        boolean z;
        try {
            ThreadAuditor.ThreadHandle threadHandleAddCurrentThread = this.sipStack.getThreadAuditor().addCurrentThread();
            loop0: while (true) {
                synchronized (this.eventMutex) {
                    while (this.pendingEvents.isEmpty()) {
                        if (this.isStopped) {
                            break loop0;
                        }
                        try {
                            threadHandleAddCurrentThread.ping();
                            this.eventMutex.wait(threadHandleAddCurrentThread.getPingIntervalInMillisecs());
                        } catch (InterruptedException e) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("Interrupted!");
                            }
                            if (zIsLoggingEnabled) {
                                if (!z) {
                                    return;
                                } else {
                                    return;
                                }
                            }
                            return;
                        }
                    }
                    linkedList = this.pendingEvents;
                    this.pendingEvents = new LinkedList();
                }
                ListIterator listIterator = linkedList.listIterator();
                while (listIterator.hasNext()) {
                    EventWrapper eventWrapper = (EventWrapper) listIterator.next();
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Processing " + eventWrapper + "nevents " + linkedList.size());
                    }
                    try {
                        deliverEvent(eventWrapper);
                    } catch (Exception e2) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logError("Unexpected exception caught while delivering event -- carrying on bravely", e2);
                        }
                    }
                }
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Stopped event scanner!!");
            }
            if (!this.sipStack.isLoggingEnabled() || this.isStopped) {
                return;
            }
            this.sipStack.getStackLogger().logFatalError("Event scanner exited abnormally");
        } finally {
            if (this.sipStack.isLoggingEnabled() && !this.isStopped) {
                this.sipStack.getStackLogger().logFatalError("Event scanner exited abnormally");
            }
        }
    }
}
