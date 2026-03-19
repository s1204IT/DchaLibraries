package gov.nist.javax.sip;

import gov.nist.core.ThreadAuditor;
import java.util.LinkedList;
import java.util.ListIterator;

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

    public void deliverEvent(gov.nist.javax.sip.EventWrapper r20) {
        throw new UnsupportedOperationException("Method not decompiled: gov.nist.javax.sip.EventScanner.deliverEvent(gov.nist.javax.sip.EventWrapper):void");
    }

    @Override
    public void run() {
        LinkedList eventsToDeliver;
        boolean zIsLoggingEnabled;
        boolean z;
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
