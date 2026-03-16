package dalvik.system.profiler;

import dalvik.system.profiler.BinaryHprof;
import dalvik.system.profiler.HprofData;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public final class SamplingProfiler {
    private final int depth;
    private Sampler sampler;
    private final ThreadSet threadSet;
    private final Map<HprofData.StackTrace, int[]> stackTraces = new HashMap();
    private final HprofData hprofData = new HprofData(this.stackTraces);
    private final Timer timer = new Timer("SamplingProfiler", true);
    private int nextThreadId = 200001;
    private int nextStackTraceId = 300001;
    private int nextObjectId = 1;
    private Thread[] currentThreads = new Thread[0];
    private final Map<Thread, Integer> threadIds = new HashMap();
    private final HprofData.StackTrace mutableStackTrace = new HprofData.StackTrace();
    private final ThreadSampler threadSampler = findDefaultThreadSampler();

    public interface ThreadSet {
        Thread[] threads();
    }

    static int access$1108(SamplingProfiler x0) {
        int i = x0.nextThreadId;
        x0.nextThreadId = i + 1;
        return i;
    }

    static int access$1208(SamplingProfiler x0) {
        int i = x0.nextObjectId;
        x0.nextObjectId = i + 1;
        return i;
    }

    static int access$908(SamplingProfiler x0) {
        int i = x0.nextStackTraceId;
        x0.nextStackTraceId = i + 1;
        return i;
    }

    public SamplingProfiler(int depth, ThreadSet threadSet) {
        this.depth = depth;
        this.threadSet = threadSet;
        this.threadSampler.setDepth(depth);
        this.hprofData.setFlags(BinaryHprof.ControlSettings.CPU_SAMPLING.bitmask);
        this.hprofData.setDepth(depth);
    }

    private static ThreadSampler findDefaultThreadSampler() {
        if ("Dalvik Core Library".equals(System.getProperty("java.specification.name"))) {
            try {
                return (ThreadSampler) Class.forName("dalvik.system.profiler.DalvikThreadSampler").newInstance();
            } catch (Exception e) {
                System.out.println("Problem creating dalvik.system.profiler.DalvikThreadSampler: " + e);
            }
        }
        return new PortableThreadSampler();
    }

    public static ThreadSet newArrayThreadSet(Thread... threads) {
        return new ArrayThreadSet(threads);
    }

    private static class ArrayThreadSet implements ThreadSet {
        private final Thread[] threads;

        public ArrayThreadSet(Thread... threads) {
            if (threads == null) {
                throw new NullPointerException("threads == null");
            }
            this.threads = threads;
        }

        @Override
        public Thread[] threads() {
            return this.threads;
        }
    }

    public static ThreadSet newThreadGroupThreadSet(ThreadGroup threadGroup) {
        return new ThreadGroupThreadSet(threadGroup);
    }

    private static class ThreadGroupThreadSet implements ThreadSet {
        private int lastThread;
        private final ThreadGroup threadGroup;
        private Thread[] threads;

        public ThreadGroupThreadSet(ThreadGroup threadGroup) {
            if (threadGroup == null) {
                throw new NullPointerException("threadGroup == null");
            }
            this.threadGroup = threadGroup;
            resize();
        }

        private void resize() {
            int count = this.threadGroup.activeCount();
            this.threads = new Thread[count * 2];
            this.lastThread = 0;
        }

        @Override
        public Thread[] threads() {
            int threadCount;
            while (true) {
                threadCount = this.threadGroup.enumerate(this.threads);
                if (threadCount != this.threads.length) {
                    break;
                }
                resize();
            }
            if (threadCount < this.lastThread) {
                Arrays.fill(this.threads, threadCount, this.lastThread, (Object) null);
            }
            this.lastThread = threadCount;
            return this.threads;
        }
    }

    public void start(int interval) {
        if (interval < 1) {
            throw new IllegalArgumentException("interval < 1");
        }
        if (this.sampler != null) {
            throw new IllegalStateException("profiling already started");
        }
        this.sampler = new Sampler();
        this.hprofData.setStartMillis(System.currentTimeMillis());
        this.timer.scheduleAtFixedRate(this.sampler, 0L, interval);
    }

    public void stop() {
        if (this.sampler != null) {
            synchronized (this.sampler) {
                this.sampler.stop = true;
                while (!this.sampler.stopped) {
                    try {
                        this.sampler.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            this.sampler = null;
        }
    }

    public void shutdown() {
        stop();
        this.timer.cancel();
    }

    public HprofData getHprofData() {
        if (this.sampler != null) {
            throw new IllegalStateException("cannot access hprof data while sampling");
        }
        return this.hprofData;
    }

    private class Sampler extends TimerTask {
        private boolean stop;
        private boolean stopped;
        private Thread timerThread;

        private Sampler() {
        }

        @Override
        public void run() {
            StackTraceElement[] stackFrames;
            synchronized (this) {
                if (this.stop) {
                    cancel();
                    this.stopped = true;
                    notifyAll();
                    return;
                }
                if (this.timerThread == null) {
                    this.timerThread = Thread.currentThread();
                }
                Thread[] newThreads = SamplingProfiler.this.threadSet.threads();
                if (!Arrays.equals(SamplingProfiler.this.currentThreads, newThreads)) {
                    updateThreadHistory(SamplingProfiler.this.currentThreads, newThreads);
                    SamplingProfiler.this.currentThreads = (Thread[]) newThreads.clone();
                }
                Thread[] arr$ = SamplingProfiler.this.currentThreads;
                for (Thread thread : arr$) {
                    if (thread != null) {
                        if (thread != this.timerThread && (stackFrames = SamplingProfiler.this.threadSampler.getStackTrace(thread)) != null) {
                            recordStackTrace(thread, stackFrames);
                        }
                    } else {
                        return;
                    }
                }
            }
        }

        private void recordStackTrace(Thread thread, StackTraceElement[] stackFrames) {
            Integer threadId = (Integer) SamplingProfiler.this.threadIds.get(thread);
            if (threadId != null) {
                SamplingProfiler.this.mutableStackTrace.threadId = threadId.intValue();
                SamplingProfiler.this.mutableStackTrace.stackFrames = stackFrames;
                int[] countCell = (int[]) SamplingProfiler.this.stackTraces.get(SamplingProfiler.this.mutableStackTrace);
                if (countCell == null) {
                    countCell = new int[1];
                    StackTraceElement[] stackFramesCopy = (StackTraceElement[]) stackFrames.clone();
                    HprofData.StackTrace stackTrace = new HprofData.StackTrace(SamplingProfiler.access$908(SamplingProfiler.this), threadId.intValue(), stackFramesCopy);
                    SamplingProfiler.this.hprofData.addStackTrace(stackTrace, countCell);
                }
                countCell[0] = countCell[0] + 1;
                return;
            }
            throw new IllegalArgumentException("Unknown thread " + thread);
        }

        private void updateThreadHistory(Thread[] oldThreads, Thread[] newThreads) {
            Collection<?> n = new HashSet<>(Arrays.asList(newThreads));
            Collection<?> o = new HashSet<>(Arrays.asList(oldThreads));
            Set<Thread> added = new HashSet<>((Collection<? extends Thread>) n);
            added.removeAll(o);
            Set<Thread> removed = new HashSet<>((Collection<? extends Thread>) o);
            removed.removeAll(n);
            for (Thread thread : added) {
                if (thread != null && thread != this.timerThread) {
                    addStartThread(thread);
                }
            }
            for (Thread thread2 : removed) {
                if (thread2 != null && thread2 != this.timerThread) {
                    addEndThread(thread2);
                }
            }
        }

        private void addStartThread(Thread thread) {
            if (thread == null) {
                throw new NullPointerException("thread == null");
            }
            int threadId = SamplingProfiler.access$1108(SamplingProfiler.this);
            Integer old = (Integer) SamplingProfiler.this.threadIds.put(thread, Integer.valueOf(threadId));
            if (old != null) {
                throw new IllegalArgumentException("Thread already registered as " + old);
            }
            String threadName = thread.getName();
            ThreadGroup group = thread.getThreadGroup();
            String groupName = group == null ? null : group.getName();
            ThreadGroup parentGroup = group == null ? null : group.getParent();
            String parentGroupName = parentGroup != null ? parentGroup.getName() : null;
            HprofData.ThreadEvent event = HprofData.ThreadEvent.start(SamplingProfiler.access$1208(SamplingProfiler.this), threadId, threadName, groupName, parentGroupName);
            SamplingProfiler.this.hprofData.addThreadEvent(event);
        }

        private void addEndThread(Thread thread) {
            if (thread != null) {
                Integer threadId = (Integer) SamplingProfiler.this.threadIds.remove(thread);
                if (threadId == null) {
                    throw new IllegalArgumentException("Unknown thread " + thread);
                }
                HprofData.ThreadEvent event = HprofData.ThreadEvent.end(threadId.intValue());
                SamplingProfiler.this.hprofData.addThreadEvent(event);
                return;
            }
            throw new NullPointerException("thread == null");
        }
    }
}
