package java.lang;

import java.io.PrintStream;
import java.lang.Thread;
import java.util.Arrays;
import sun.misc.VM;

public class ThreadGroup implements Thread.UncaughtExceptionHandler {
    boolean daemon;
    boolean destroyed;
    ThreadGroup[] groups;
    int maxPriority;
    int nUnstartedThreads;
    String name;
    int ngroups;
    int nthreads;
    private final ThreadGroup parent;
    Thread[] threads;
    boolean vmAllowSuspension;
    static final ThreadGroup systemThreadGroup = new ThreadGroup();
    static final ThreadGroup mainThreadGroup = new ThreadGroup(systemThreadGroup, "main");

    private ThreadGroup() {
        this.nUnstartedThreads = 0;
        this.name = "system";
        this.maxPriority = 10;
        this.parent = null;
    }

    public ThreadGroup(String str) {
        this(Thread.currentThread().getThreadGroup(), str);
    }

    public ThreadGroup(ThreadGroup threadGroup, String str) {
        this(checkParentAccess(threadGroup), threadGroup, str);
    }

    private ThreadGroup(Void r1, ThreadGroup threadGroup, String str) {
        this.nUnstartedThreads = 0;
        this.name = str;
        this.maxPriority = threadGroup.maxPriority;
        this.daemon = threadGroup.daemon;
        this.vmAllowSuspension = threadGroup.vmAllowSuspension;
        this.parent = threadGroup;
        threadGroup.add(this);
    }

    private static Void checkParentAccess(ThreadGroup threadGroup) {
        threadGroup.checkAccess();
        return null;
    }

    public final String getName() {
        return this.name;
    }

    public final ThreadGroup getParent() {
        if (this.parent != null) {
            this.parent.checkAccess();
        }
        return this.parent;
    }

    public final int getMaxPriority() {
        return this.maxPriority;
    }

    public final boolean isDaemon() {
        return this.daemon;
    }

    public synchronized boolean isDestroyed() {
        return this.destroyed;
    }

    public final void setDaemon(boolean z) {
        checkAccess();
        this.daemon = z;
    }

    public final void setMaxPriority(int i) {
        int i2;
        ThreadGroup[] threadGroupArr;
        synchronized (this) {
            checkAccess();
            if (i < 1) {
                i = 1;
            }
            if (i > 10) {
                i = 10;
            }
            this.maxPriority = this.parent != null ? Math.min(i, this.parent.maxPriority) : i;
            i2 = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, i2);
            } else {
                threadGroupArr = null;
            }
        }
        for (int i3 = 0; i3 < i2; i3++) {
            threadGroupArr[i3].setMaxPriority(i);
        }
    }

    public final boolean parentOf(ThreadGroup threadGroup) {
        while (threadGroup != null) {
            if (threadGroup != this) {
                threadGroup = threadGroup.parent;
            } else {
                return true;
            }
        }
        return false;
    }

    public final void checkAccess() {
    }

    public int activeCount() {
        ThreadGroup[] threadGroupArr;
        synchronized (this) {
            if (this.destroyed) {
                return 0;
            }
            int iActiveCount = this.nthreads;
            int i = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, i);
            } else {
                threadGroupArr = null;
            }
            for (int i2 = 0; i2 < i; i2++) {
                iActiveCount += threadGroupArr[i2].activeCount();
            }
            return iActiveCount;
        }
    }

    public int enumerate(Thread[] threadArr) {
        checkAccess();
        return enumerate(threadArr, 0, true);
    }

    public int enumerate(Thread[] threadArr, boolean z) {
        checkAccess();
        return enumerate(threadArr, 0, z);
    }

    private int enumerate(Thread[] threadArr, int i, boolean z) {
        int i2;
        synchronized (this) {
            if (this.destroyed) {
                return 0;
            }
            int length = this.nthreads;
            if (length > threadArr.length - i) {
                length = threadArr.length - i;
            }
            int iEnumerate = i;
            for (int i3 = 0; i3 < length; i3++) {
                if (this.threads[i3].isAlive()) {
                    threadArr[iEnumerate] = this.threads[i3];
                    iEnumerate++;
                }
            }
            ThreadGroup[] threadGroupArr = null;
            if (z) {
                i2 = this.ngroups;
                if (this.groups != null) {
                    threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, i2);
                }
            } else {
                i2 = 0;
            }
            if (z) {
                for (int i4 = 0; i4 < i2; i4++) {
                    iEnumerate = threadGroupArr[i4].enumerate(threadArr, iEnumerate, true);
                }
            }
            return iEnumerate;
        }
    }

    public int activeGroupCount() {
        ThreadGroup[] threadGroupArr;
        synchronized (this) {
            if (this.destroyed) {
                return 0;
            }
            int i = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, i);
            } else {
                threadGroupArr = null;
            }
            int iActiveGroupCount = i;
            for (int i2 = 0; i2 < i; i2++) {
                iActiveGroupCount += threadGroupArr[i2].activeGroupCount();
            }
            return iActiveGroupCount;
        }
    }

    public int enumerate(ThreadGroup[] threadGroupArr) {
        checkAccess();
        return enumerate(threadGroupArr, 0, true);
    }

    public int enumerate(ThreadGroup[] threadGroupArr, boolean z) {
        checkAccess();
        return enumerate(threadGroupArr, 0, z);
    }

    private int enumerate(ThreadGroup[] threadGroupArr, int i, boolean z) {
        int i2;
        synchronized (this) {
            if (this.destroyed) {
                return 0;
            }
            int length = this.ngroups;
            if (length > threadGroupArr.length - i) {
                length = threadGroupArr.length - i;
            }
            if (length > 0) {
                System.arraycopy(this.groups, 0, threadGroupArr, i, length);
                i += length;
            }
            ThreadGroup[] threadGroupArr2 = null;
            if (z) {
                i2 = this.ngroups;
                if (this.groups != null) {
                    threadGroupArr2 = (ThreadGroup[]) Arrays.copyOf(this.groups, i2);
                }
            } else {
                i2 = 0;
            }
            if (z) {
                for (int i3 = 0; i3 < i2; i3++) {
                    i = threadGroupArr2[i3].enumerate(threadGroupArr, i, true);
                }
            }
            return i;
        }
    }

    @Deprecated
    public final void stop() {
        if (stopOrSuspend(false)) {
            Thread.currentThread().stop();
        }
    }

    public final void interrupt() {
        int i;
        int i2;
        ThreadGroup[] threadGroupArr;
        synchronized (this) {
            checkAccess();
            for (int i3 = 0; i3 < this.nthreads; i3++) {
                this.threads[i3].interrupt();
            }
            i2 = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, i2);
            } else {
                threadGroupArr = null;
            }
        }
        for (i = 0; i < i2; i++) {
            threadGroupArr[i].interrupt();
        }
    }

    @Deprecated
    public final void suspend() {
        if (stopOrSuspend(true)) {
            Thread.currentThread().suspend();
        }
    }

    private boolean stopOrSuspend(boolean z) {
        boolean z2;
        int i;
        ThreadGroup[] threadGroupArr;
        Thread threadCurrentThread = Thread.currentThread();
        synchronized (this) {
            checkAccess();
            z2 = false;
            for (int i2 = 0; i2 < this.nthreads; i2++) {
                if (this.threads[i2] == threadCurrentThread) {
                    z2 = true;
                } else if (z) {
                    this.threads[i2].suspend();
                } else {
                    this.threads[i2].stop();
                }
            }
            i = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, i);
            } else {
                threadGroupArr = null;
            }
        }
        boolean z3 = z2;
        for (int i3 = 0; i3 < i; i3++) {
            z3 = threadGroupArr[i3].stopOrSuspend(z) || z3;
        }
        return z3;
    }

    @Deprecated
    public final void resume() {
        int i;
        int i2;
        ThreadGroup[] threadGroupArr;
        synchronized (this) {
            checkAccess();
            for (int i3 = 0; i3 < this.nthreads; i3++) {
                this.threads[i3].resume();
            }
            i2 = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, i2);
            } else {
                threadGroupArr = null;
            }
        }
        for (i = 0; i < i2; i++) {
            threadGroupArr[i].resume();
        }
    }

    public final void destroy() {
        int i;
        ThreadGroup[] threadGroupArr;
        int i2;
        synchronized (this) {
            checkAccess();
            if (this.destroyed || this.nthreads > 0) {
                throw new IllegalThreadStateException();
            }
            i = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, i);
            } else {
                threadGroupArr = null;
            }
            if (this.parent != null) {
                this.destroyed = true;
                this.ngroups = 0;
                this.groups = null;
                this.nthreads = 0;
                this.threads = null;
            }
        }
        for (i2 = 0; i2 < i; i2++) {
            threadGroupArr[i2].destroy();
        }
        if (this.parent != null) {
            this.parent.remove(this);
        }
    }

    private final void add(ThreadGroup threadGroup) {
        synchronized (this) {
            if (this.destroyed) {
                throw new IllegalThreadStateException();
            }
            if (this.groups == null) {
                this.groups = new ThreadGroup[4];
            } else if (this.ngroups == this.groups.length) {
                this.groups = (ThreadGroup[]) Arrays.copyOf(this.groups, this.ngroups * 2);
            }
            this.groups[this.ngroups] = threadGroup;
            this.ngroups++;
        }
    }

    private void remove(ThreadGroup threadGroup) {
        synchronized (this) {
            if (this.destroyed) {
                return;
            }
            int i = 0;
            while (true) {
                if (i >= this.ngroups) {
                    break;
                }
                if (this.groups[i] != threadGroup) {
                    i++;
                } else {
                    this.ngroups--;
                    System.arraycopy(this.groups, i + 1, this.groups, i, this.ngroups - i);
                    this.groups[this.ngroups] = null;
                    break;
                }
            }
            if (this.nthreads == 0) {
                notifyAll();
            }
            if (this.daemon && this.nthreads == 0 && this.nUnstartedThreads == 0 && this.ngroups == 0) {
                destroy();
            }
        }
    }

    void addUnstarted() {
        synchronized (this) {
            if (this.destroyed) {
                throw new IllegalThreadStateException();
            }
            this.nUnstartedThreads++;
        }
    }

    void add(Thread thread) {
        synchronized (this) {
            if (this.destroyed) {
                throw new IllegalThreadStateException();
            }
            if (this.threads == null) {
                this.threads = new Thread[4];
            } else if (this.nthreads == this.threads.length) {
                this.threads = (Thread[]) Arrays.copyOf(this.threads, this.nthreads * 2);
            }
            this.threads[this.nthreads] = thread;
            this.nthreads++;
            this.nUnstartedThreads--;
        }
    }

    void threadStartFailed(Thread thread) {
        synchronized (this) {
            remove(thread);
            this.nUnstartedThreads++;
        }
    }

    void threadTerminated(Thread thread) {
        synchronized (this) {
            remove(thread);
            if (this.nthreads == 0) {
                notifyAll();
            }
            if (this.daemon && this.nthreads == 0 && this.nUnstartedThreads == 0 && this.ngroups == 0) {
                destroy();
            }
        }
    }

    private void remove(Thread thread) {
        synchronized (this) {
            if (this.destroyed) {
                return;
            }
            int i = 0;
            while (true) {
                if (i >= this.nthreads) {
                    break;
                }
                if (this.threads[i] != thread) {
                    i++;
                } else {
                    Thread[] threadArr = this.threads;
                    int i2 = this.nthreads - 1;
                    this.nthreads = i2;
                    System.arraycopy(this.threads, i + 1, threadArr, i, i2 - i);
                    this.threads[this.nthreads] = null;
                    break;
                }
            }
        }
    }

    public void list() {
        list(System.out, 0);
    }

    void list(PrintStream printStream, int i) {
        int i2;
        int i3;
        int i4;
        ThreadGroup[] threadGroupArr;
        synchronized (this) {
            for (int i5 = 0; i5 < i; i5++) {
                try {
                    printStream.print(" ");
                } catch (Throwable th) {
                    throw th;
                }
            }
            printStream.println(this);
            i3 = i + 4;
            for (int i6 = 0; i6 < this.nthreads; i6++) {
                for (int i7 = 0; i7 < i3; i7++) {
                    printStream.print(" ");
                }
                printStream.println(this.threads[i6]);
            }
            i4 = this.ngroups;
            if (this.groups != null) {
                threadGroupArr = (ThreadGroup[]) Arrays.copyOf(this.groups, i4);
            } else {
                threadGroupArr = null;
            }
        }
        for (i2 = 0; i2 < i4; i2++) {
            threadGroupArr[i2].list(printStream, i3);
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable th) {
        if (this.parent != null) {
            this.parent.uncaughtException(thread, th);
            return;
        }
        Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (defaultUncaughtExceptionHandler != null) {
            defaultUncaughtExceptionHandler.uncaughtException(thread, th);
            return;
        }
        if (!(th instanceof ThreadDeath)) {
            System.err.print("Exception in thread \"" + thread.getName() + "\" ");
            th.printStackTrace(System.err);
        }
    }

    @Deprecated
    public boolean allowThreadSuspension(boolean z) {
        this.vmAllowSuspension = z;
        if (!z) {
            VM.unsuspendSomeThreads();
            return true;
        }
        return true;
    }

    public String toString() {
        return getClass().getName() + "[name=" + getName() + ",maxpri=" + this.maxPriority + "]";
    }
}
