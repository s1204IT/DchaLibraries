package gov.nist.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ThreadAuditor {
    private Map<Thread, ThreadHandle> threadHandles = new HashMap();
    private long pingIntervalInMillisecs = 0;

    public class ThreadHandle {
        private ThreadAuditor threadAuditor;
        private boolean isThreadActive = false;
        private Thread thread = Thread.currentThread();

        public ThreadHandle(ThreadAuditor threadAuditor) {
            this.threadAuditor = threadAuditor;
        }

        public boolean isThreadActive() {
            return this.isThreadActive;
        }

        protected void setThreadActive(boolean z) {
            this.isThreadActive = z;
        }

        public Thread getThread() {
            return this.thread;
        }

        public void ping() {
            this.threadAuditor.ping(this);
        }

        public long getPingIntervalInMillisecs() {
            return this.threadAuditor.getPingIntervalInMillisecs();
        }

        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("Thread Name: ");
            stringBuffer.append(this.thread.getName());
            stringBuffer.append(", Alive: ");
            stringBuffer.append(this.thread.isAlive());
            return stringBuffer.toString();
        }
    }

    public long getPingIntervalInMillisecs() {
        return this.pingIntervalInMillisecs;
    }

    public void setPingIntervalInMillisecs(long j) {
        this.pingIntervalInMillisecs = j;
    }

    public boolean isEnabled() {
        return this.pingIntervalInMillisecs > 0;
    }

    public synchronized ThreadHandle addCurrentThread() {
        ThreadHandle threadHandle;
        threadHandle = new ThreadHandle(this);
        if (isEnabled()) {
            this.threadHandles.put(Thread.currentThread(), threadHandle);
        }
        return threadHandle;
    }

    public synchronized void removeThread(Thread thread) {
        this.threadHandles.remove(thread);
    }

    public synchronized void ping(ThreadHandle threadHandle) {
        threadHandle.setThreadActive(true);
    }

    public synchronized void reset() {
        this.threadHandles.clear();
    }

    public synchronized String auditThreads() {
        String str;
        str = null;
        for (ThreadHandle threadHandle : this.threadHandles.values()) {
            if (!threadHandle.isThreadActive()) {
                Thread thread = threadHandle.getThread();
                if (str == null) {
                    str = "Thread Auditor Report:\n";
                }
                str = str + "   Thread [" + thread.getName() + "] has failed to respond to an audit request.\n";
            }
            threadHandle.setThreadActive(false);
        }
        return str;
    }

    public synchronized String toString() {
        String str;
        str = "Thread Auditor - List of monitored threads:\n";
        Iterator<ThreadHandle> it = this.threadHandles.values().iterator();
        while (it.hasNext()) {
            str = str + "   " + it.next().toString() + Separators.RETURN;
        }
        return str;
    }
}
