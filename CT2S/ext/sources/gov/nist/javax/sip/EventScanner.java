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
        Thread myThread = new Thread(this);
        myThread.setDaemon(false);
        this.sipStack = sipStackImpl;
        myThread.setName("EventScannerThread");
        myThread.start();
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
        SIPDialog dialog;
        EventObject sipEvent = eventWrapper.sipEvent;
        if (this.sipStack.isLoggingEnabled()) {
            this.sipStack.getStackLogger().logDebug("sipEvent = " + sipEvent + "source = " + sipEvent.getSource());
        }
        SipListener sipListener = !(sipEvent instanceof IOExceptionEvent) ? ((SipProviderImpl) sipEvent.getSource()).getSipListener() : this.sipStack.getSipListener();
        if (!(sipEvent instanceof RequestEvent)) {
            if (sipEvent instanceof ResponseEvent) {
                try {
                    ResponseEvent responseEvent = (ResponseEvent) sipEvent;
                    SIPResponse sipResponse = (SIPResponse) responseEvent.getResponse();
                    SIPDialog sipDialog = (SIPDialog) responseEvent.getDialog();
                    try {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logDebug("Calling listener for " + sipResponse.getFirstLine());
                        }
                        if (sipListener != null) {
                            SIPTransaction tx = eventWrapper.transaction;
                            if (tx != null) {
                                tx.setPassToListener();
                            }
                            sipListener.processResponse((ResponseEvent) sipEvent);
                        }
                        if (sipDialog != null && ((sipDialog.getState() == null || !sipDialog.getState().equals(DialogState.TERMINATED)) && (sipResponse.getStatusCode() == 481 || sipResponse.getStatusCode() == 408))) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("Removing dialog on 408 or 481 response");
                            }
                            sipDialog.doDeferredDelete();
                        }
                        if (sipResponse.getCSeq().getMethod().equals("INVITE") && sipDialog != null && sipResponse.getStatusCode() == 200) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("Warning! unacknowledged dialog. " + sipDialog.getState());
                            }
                            sipDialog.doDeferredDeleteIfNoAckSent(sipResponse.getCSeq().getSeqNumber());
                        }
                    } catch (Exception ex) {
                        this.sipStack.getStackLogger().logException(ex);
                    }
                    SIPClientTransaction ct = (SIPClientTransaction) eventWrapper.transaction;
                    if (ct != null && TransactionState.COMPLETED == ct.getState() && ct.getOriginalRequest() != null && !ct.getOriginalRequest().getMethod().equals("INVITE")) {
                        ct.clearState();
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
            if (sipEvent instanceof TimeoutEvent) {
                if (sipListener != null) {
                    try {
                        sipListener.processTimeout((TimeoutEvent) sipEvent);
                        return;
                    } catch (Exception ex2) {
                        this.sipStack.getStackLogger().logException(ex2);
                        return;
                    }
                }
                return;
            }
            if (sipEvent instanceof DialogTimeoutEvent) {
                if (sipListener != null) {
                    try {
                        if (sipListener instanceof SipListenerExt) {
                            ((SipListenerExt) sipListener).processDialogTimeout((DialogTimeoutEvent) sipEvent);
                            return;
                        }
                        return;
                    } catch (Exception ex3) {
                        this.sipStack.getStackLogger().logException(ex3);
                        return;
                    }
                }
                return;
            }
            if (sipEvent instanceof IOExceptionEvent) {
                if (sipListener != null) {
                    try {
                        sipListener.processIOException((IOExceptionEvent) sipEvent);
                        return;
                    } catch (Exception ex4) {
                        this.sipStack.getStackLogger().logException(ex4);
                        return;
                    }
                }
                return;
            }
            if (!(sipEvent instanceof TransactionTerminatedEvent)) {
                if (!(sipEvent instanceof DialogTerminatedEvent)) {
                    this.sipStack.getStackLogger().logFatalError("bad event" + sipEvent);
                    return;
                }
                if (sipListener != null) {
                    try {
                        sipListener.processDialogTerminated((DialogTerminatedEvent) sipEvent);
                        return;
                    } catch (AbstractMethodError e) {
                        if (this.sipStack.isLoggingEnabled()) {
                            this.sipStack.getStackLogger().logWarning("Unable to call sipListener.processDialogTerminated");
                            return;
                        }
                        return;
                    } catch (Exception ex5) {
                        this.sipStack.getStackLogger().logException(ex5);
                        return;
                    }
                }
                return;
            }
            try {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("About to deliver transactionTerminatedEvent");
                    this.sipStack.getStackLogger().logDebug("tx = " + ((TransactionTerminatedEvent) sipEvent).getClientTransaction());
                    this.sipStack.getStackLogger().logDebug("tx = " + ((TransactionTerminatedEvent) sipEvent).getServerTransaction());
                }
                if (sipListener != null) {
                    sipListener.processTransactionTerminated((TransactionTerminatedEvent) sipEvent);
                    return;
                }
                return;
            } catch (AbstractMethodError e2) {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logWarning("Unable to call sipListener.processTransactionTerminated");
                    return;
                }
                return;
            } catch (Exception ex6) {
                this.sipStack.getStackLogger().logException(ex6);
                return;
            }
        }
        try {
            SIPRequest sipRequest = (SIPRequest) ((RequestEvent) sipEvent).getRequest();
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("deliverEvent : " + sipRequest.getFirstLine() + " transaction " + eventWrapper.transaction + " sipEvent.serverTx = " + ((RequestEvent) sipEvent).getServerTransaction());
            }
            SIPServerTransaction tx2 = (SIPServerTransaction) this.sipStack.findTransaction(sipRequest, true);
            if (tx2 == null || tx2.passToListener()) {
                if (this.sipStack.findPendingTransaction(sipRequest) != null) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("transaction already exists!!");
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Done processing Message " + ((SIPRequest) ((RequestEvent) sipEvent).getRequest()).getFirstLine());
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
                SIPServerTransaction st = (SIPServerTransaction) eventWrapper.transaction;
                this.sipStack.putPendingTransaction(st);
            } else {
                if (!sipRequest.getMethod().equals("ACK") || !tx2.isInviteTransaction() || (tx2.getLastResponse().getStatusCode() / 100 != 2 && !this.sipStack.isNon2XXAckPassedToListener())) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("transaction already exists! " + tx2);
                    }
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Done processing Message " + ((SIPRequest) ((RequestEvent) sipEvent).getRequest()).getFirstLine());
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
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Detected broken client sending ACK with same branch! Passing...");
                }
            }
            sipRequest.setTransaction(eventWrapper.transaction);
            try {
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Calling listener " + sipRequest.getFirstLine());
                    this.sipStack.getStackLogger().logDebug("Calling listener " + eventWrapper.transaction);
                }
                if (sipListener != null) {
                    sipListener.processRequest((RequestEvent) sipEvent);
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.sipStack.getStackLogger().logDebug("Done processing Message " + sipRequest.getFirstLine());
                }
                if (eventWrapper.transaction != null && (dialog = (SIPDialog) eventWrapper.transaction.getDialog()) != null) {
                    dialog.requestConsumed();
                }
            } catch (Exception ex7) {
                this.sipStack.getStackLogger().logException(ex7);
            }
            if (this.sipStack.isLoggingEnabled()) {
                this.sipStack.getStackLogger().logDebug("Done processing Message " + ((SIPRequest) ((RequestEvent) sipEvent).getRequest()).getFirstLine());
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
        } finally {
        }
    }

    @Override
    public void run() {
        LinkedList eventsToDeliver;
        try {
            ThreadAuditor.ThreadHandle threadHandle = this.sipStack.getThreadAuditor().addCurrentThread();
            loop0: while (true) {
                synchronized (this.eventMutex) {
                    while (this.pendingEvents.isEmpty()) {
                        if (this.isStopped) {
                            break loop0;
                        }
                        try {
                            threadHandle.ping();
                            this.eventMutex.wait(threadHandle.getPingIntervalInMillisecs());
                        } catch (InterruptedException e) {
                            if (this.sipStack.isLoggingEnabled()) {
                                this.sipStack.getStackLogger().logDebug("Interrupted!");
                            }
                            if (!this.sipStack.isLoggingEnabled() || this.isStopped) {
                                return;
                            }
                            this.sipStack.getStackLogger().logFatalError("Event scanner exited abnormally");
                            return;
                        }
                    }
                    eventsToDeliver = this.pendingEvents;
                    this.pendingEvents = new LinkedList();
                }
                ListIterator iterator = eventsToDeliver.listIterator();
                while (iterator.hasNext()) {
                    EventWrapper eventWrapper = (EventWrapper) iterator.next();
                    if (this.sipStack.isLoggingEnabled()) {
                        this.sipStack.getStackLogger().logDebug("Processing " + eventWrapper + "nevents " + eventsToDeliver.size());
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
        } finally {
            if (this.sipStack.isLoggingEnabled() && !this.isStopped) {
                this.sipStack.getStackLogger().logFatalError("Event scanner exited abnormally");
            }
        }
    }
}
