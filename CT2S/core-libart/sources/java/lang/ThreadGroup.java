package java.lang;

import java.lang.Thread;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import libcore.util.CollectionUtils;

public class ThreadGroup implements Thread.UncaughtExceptionHandler {
    private final List<ThreadGroup> groups;
    private boolean isDaemon;
    private boolean isDestroyed;
    private int maxPriority;
    private String name;
    final ThreadGroup parent;
    private final List<WeakReference<Thread>> threadRefs;
    private final Iterable<Thread> threads;
    static final ThreadGroup systemThreadGroup = new ThreadGroup();
    static final ThreadGroup mainThreadGroup = new ThreadGroup(systemThreadGroup, "main");

    public ThreadGroup(String name) {
        this(Thread.currentThread().getThreadGroup(), name);
    }

    public ThreadGroup(ThreadGroup parent, String name) {
        this.maxPriority = 10;
        this.threadRefs = new ArrayList(5);
        this.threads = CollectionUtils.dereferenceIterable(this.threadRefs, true);
        this.groups = new ArrayList(3);
        if (parent == null) {
            throw new NullPointerException("parent == null");
        }
        this.name = name;
        this.parent = parent;
        if (parent != null) {
            parent.add(this);
            setMaxPriority(parent.getMaxPriority());
            if (parent.isDaemon()) {
                setDaemon(true);
            }
        }
    }

    private ThreadGroup() {
        this.maxPriority = 10;
        this.threadRefs = new ArrayList(5);
        this.threads = CollectionUtils.dereferenceIterable(this.threadRefs, true);
        this.groups = new ArrayList(3);
        this.name = "system";
        this.parent = null;
    }

    public int activeCount() {
        int count = 0;
        synchronized (this.threadRefs) {
            for (Thread thread : this.threads) {
                if (thread.isAlive()) {
                    count++;
                }
            }
        }
        synchronized (this.groups) {
            for (ThreadGroup group : this.groups) {
                count += group.activeCount();
            }
        }
        return count;
    }

    public int activeGroupCount() {
        int count = 0;
        synchronized (this.groups) {
            for (ThreadGroup group : this.groups) {
                count += group.activeGroupCount() + 1;
            }
        }
        return count;
    }

    private void add(ThreadGroup g) throws IllegalThreadStateException {
        synchronized (this.groups) {
            if (this.isDestroyed) {
                throw new IllegalThreadStateException();
            }
            this.groups.add(g);
        }
    }

    @Deprecated
    public boolean allowThreadSuspension(boolean b) {
        return true;
    }

    public final void checkAccess() {
    }

    public final void destroy() {
        synchronized (this.threadRefs) {
            synchronized (this.groups) {
                if (this.isDestroyed) {
                    throw new IllegalThreadStateException("Thread group was already destroyed: " + (this.name != null ? this.name : "n/a"));
                }
                if (this.threads.iterator().hasNext()) {
                    throw new IllegalThreadStateException("Thread group still contains threads: " + (this.name != null ? this.name : "n/a"));
                }
                while (!this.groups.isEmpty()) {
                    this.groups.get(0).destroy();
                }
                if (this.parent != null) {
                    this.parent.remove(this);
                }
                this.isDestroyed = true;
            }
        }
    }

    private void destroyIfEmptyDaemon() {
        synchronized (this.threadRefs) {
            if (this.isDaemon && !this.isDestroyed && !this.threads.iterator().hasNext()) {
                synchronized (this.groups) {
                    if (this.groups.isEmpty()) {
                        destroy();
                    }
                }
            }
        }
    }

    public int enumerate(Thread[] threads) {
        return enumerate(threads, true);
    }

    public int enumerate(Thread[] threads, boolean recurse) {
        return enumerateGeneric(threads, recurse, 0, true);
    }

    public int enumerate(ThreadGroup[] groups) {
        return enumerate(groups, true);
    }

    public int enumerate(ThreadGroup[] groups, boolean recurse) {
        return enumerateGeneric(groups, recurse, 0, false);
    }

    private int enumerateGeneric(java.lang.Object[] r8, boolean r9, int r10, boolean r11) {
        if (r11) {
            r6 = r7.threadRefs;
            synchronized (r6) {
                ;
                r2 = r7.threadRefs.size() + (-1);
                r0 = r10;
                while (r2 >= 0) {
                    r4 = r7.threadRefs.get(r2).get();
                    if (r4 == null || !r4.isAlive()) {
                        r10 = r0;
                    } else {
                        if (r0 >= r8.length) {
                            return r0;
                        } else {
                            r10 = r0 + 1;
                            r8[r0] = r4;
                        }
                    }
                    r2 = r2 + (-1);
                    r0 = r10;
                }
                r10 = r0;
            }
        } else {
            r6 = r7.groups;
            synchronized (r6) {
                ;
                r2 = r7.groups.size() + (-1);
                r0 = r10;
                while (r2 >= 0) {
                    if (r0 >= r8.length) {
                        return r0;
                    } else {
                        r10 = r0 + 1;
                        r8[r0] = r7.groups.get(r2);
                        r2 = r2 + (-1);
                        r0 = r10;
                    }
                }
                r10 = r0;
            }
        }
        if (r9) {
            r6 = r7.groups;
            synchronized (r6) {
                ;
                r3 = r7.groups.iterator();
                while (r3.hasNext()) {
                    r1 = r3.next();
                    if (r10 >= r8.length) {
                        return r10;
                    } else {
                        r10 = r1.enumerateGeneric(r8, r9, r10, r11);
                    }
                }
            }
        }
        return r10;
    }

    public final int getMaxPriority() {
        return this.maxPriority;
    }

    public final String getName() {
        return this.name;
    }

    public final ThreadGroup getParent() {
        return this.parent;
    }

    public final void interrupt() {
        synchronized (this.threadRefs) {
            for (Thread thread : this.threads) {
                thread.interrupt();
            }
        }
        synchronized (this.groups) {
            for (ThreadGroup group : this.groups) {
                group.interrupt();
            }
        }
    }

    public final boolean isDaemon() {
        return this.isDaemon;
    }

    public synchronized boolean isDestroyed() {
        return this.isDestroyed;
    }

    public void list() {
        System.out.println();
        list(0);
    }

    private void list(int levels) {
        indent(levels);
        System.out.println(toString());
        int levels2 = levels + 1;
        synchronized (this.threadRefs) {
            for (Thread thread : this.threads) {
                indent(levels2);
                System.out.println(thread);
            }
        }
        synchronized (this.groups) {
            for (ThreadGroup group : this.groups) {
                group.list(levels2);
            }
        }
    }

    private void indent(int levels) {
        for (int i = 0; i < levels; i++) {
            System.out.print("    ");
        }
    }

    public final boolean parentOf(ThreadGroup g) {
        while (g != null) {
            if (this == g) {
                return true;
            }
            g = g.parent;
        }
        return false;
    }

    private void remove(ThreadGroup g) {
        synchronized (this.groups) {
            Iterator<ThreadGroup> i = this.groups.iterator();
            while (true) {
                if (!i.hasNext()) {
                    break;
                }
                ThreadGroup threadGroup = i.next();
                if (threadGroup.equals(g)) {
                    i.remove();
                    break;
                }
            }
        }
        destroyIfEmptyDaemon();
    }

    @Deprecated
    public final void resume() {
        synchronized (this.threadRefs) {
            for (Thread thread : this.threads) {
                thread.resume();
            }
        }
        synchronized (this.groups) {
            for (ThreadGroup group : this.groups) {
                group.resume();
            }
        }
    }

    public final void setDaemon(boolean isDaemon) {
        this.isDaemon = isDaemon;
    }

    public final void setMaxPriority(int newMax) {
        if (newMax <= this.maxPriority) {
            if (newMax < 1) {
                newMax = 1;
            }
            int parentPriority = this.parent == null ? newMax : this.parent.getMaxPriority();
            if (parentPriority > newMax) {
                parentPriority = newMax;
            }
            this.maxPriority = parentPriority;
            synchronized (this.groups) {
                for (ThreadGroup group : this.groups) {
                    group.setMaxPriority(newMax);
                }
            }
        }
    }

    @Deprecated
    public final void stop() {
        if (stopHelper()) {
            Thread.currentThread().stop();
        }
    }

    private boolean stopHelper() {
        boolean stopCurrent = false;
        synchronized (this.threadRefs) {
            Thread current = Thread.currentThread();
            for (Thread thread : this.threads) {
                if (thread == current) {
                    stopCurrent = true;
                } else {
                    thread.stop();
                }
            }
        }
        synchronized (this.groups) {
            for (ThreadGroup group : this.groups) {
                stopCurrent |= group.stopHelper();
            }
        }
        return stopCurrent;
    }

    @Deprecated
    public final void suspend() {
        if (suspendHelper()) {
            Thread.currentThread().suspend();
        }
    }

    private boolean suspendHelper() {
        boolean suspendCurrent = false;
        synchronized (this.threadRefs) {
            Thread current = Thread.currentThread();
            for (Thread thread : this.threads) {
                if (thread == current) {
                    suspendCurrent = true;
                } else {
                    thread.suspend();
                }
            }
        }
        synchronized (this.groups) {
            for (ThreadGroup group : this.groups) {
                suspendCurrent |= group.suspendHelper();
            }
        }
        return suspendCurrent;
    }

    public String toString() {
        return getClass().getName() + "[name=" + getName() + ",maxPriority=" + getMaxPriority() + "]";
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (this.parent != null) {
            this.parent.uncaughtException(t, e);
        } else if (Thread.getDefaultUncaughtExceptionHandler() != null) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(t, e);
        } else if (!(e instanceof ThreadDeath)) {
            e.printStackTrace(System.err);
        }
    }

    final void addThread(Thread thread) throws IllegalThreadStateException {
        synchronized (this.threadRefs) {
            if (this.isDestroyed) {
                throw new IllegalThreadStateException();
            }
            this.threadRefs.add(new WeakReference<>(thread));
        }
    }

    final void removeThread(Thread thread) throws IllegalThreadStateException {
        synchronized (this.threadRefs) {
            Iterator<Thread> i = this.threads.iterator();
            while (true) {
                if (!i.hasNext()) {
                    break;
                } else if (i.next().equals(thread)) {
                    i.remove();
                    break;
                }
            }
        }
        destroyIfEmptyDaemon();
    }
}
