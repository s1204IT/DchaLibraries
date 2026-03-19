package dalvik.system.profiler;

import android.icu.text.PluralRules;
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
                System.out.println("Problem creating dalvik.system.profiler.DalvikThreadSampler" + PluralRules.KEYWORD_RULE_SEPARATOR + e);
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
        Sampler sampler = null;
        if (interval < 1) {
            throw new IllegalArgumentException("interval < 1");
        }
        if (this.sampler != null) {
            throw new IllegalStateException("profiling already started");
        }
        this.sampler = new Sampler(this, sampler);
        this.hprofData.setStartMillis(System.currentTimeMillis());
        this.timer.scheduleAtFixedRate(this.sampler, 0L, interval);
    }

    public void stop() {
        if (this.sampler == null) {
            return;
        }
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

        Sampler(SamplingProfiler this$0, Sampler sampler) {
            this();
        }

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
                for (Thread thread : SamplingProfiler.this.currentThreads) {
                    if (thread == null) {
                        return;
                    }
                    if (thread != this.timerThread && (stackFrames = SamplingProfiler.this.threadSampler.getStackTrace(thread)) != null) {
                        recordStackTrace(thread, stackFrames);
                    }
                }
            }
        }

        private void recordStackTrace(Thread thread, StackTraceElement[] stackFrames) {
            Integer threadId = (Integer) SamplingProfiler.this.threadIds.get(thread);
            if (threadId == null) {
                throw new IllegalArgumentException("Unknown thread " + thread);
            }
            SamplingProfiler.this.mutableStackTrace.threadId = threadId.intValue();
            SamplingProfiler.this.mutableStackTrace.stackFrames = stackFrames;
            int[] countCell = (int[]) SamplingProfiler.this.stackTraces.get(SamplingProfiler.this.mutableStackTrace);
            if (countCell == null) {
                countCell = new int[1];
                StackTraceElement[] stackFramesCopy = (StackTraceElement[]) stackFrames.clone();
                SamplingProfiler samplingProfiler = SamplingProfiler.this;
                int i = samplingProfiler.nextStackTraceId;
                samplingProfiler.nextStackTraceId = i + 1;
                HprofData.StackTrace stackTrace = new HprofData.StackTrace(i, threadId.intValue(), stackFramesCopy);
                SamplingProfiler.this.hprofData.addStackTrace(stackTrace, countCell);
            }
            countCell[0] = countCell[0] + 1;
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
            SamplingProfiler samplingProfiler = SamplingProfiler.this;
            int threadId = samplingProfiler.nextThreadId;
            samplingProfiler.nextThreadId = threadId + 1;
            Integer old = (Integer) SamplingProfiler.this.threadIds.put(thread, Integer.valueOf(threadId));
            if (old != null) {
                throw new IllegalArgumentException("Thread already registered as " + old);
            }
            String threadName = thread.getName();
            ThreadGroup group = thread.getThreadGroup();
            String name = group == null ? null : group.getName();
            ThreadGroup parentGroup = group != null ? group.getParent() : null;
            String name2 = parentGroup == null ? null : parentGroup.getName();
            SamplingProfiler samplingProfiler2 = SamplingProfiler.this;
            int i = samplingProfiler2.nextObjectId;
            samplingProfiler2.nextObjectId = i + 1;
            HprofData.ThreadEvent event = HprofData.ThreadEvent.start(i, threadId, threadName, name, name2);
            SamplingProfiler.this.hprofData.addThreadEvent(event);
        }

        private void addEndThread(Thread thread) {
            if (thread == null) {
                throw new NullPointerException("thread == null");
            }
            Integer threadId = (Integer) SamplingProfiler.this.threadIds.remove(thread);
            if (threadId == null) {
                throw new IllegalArgumentException("Unknown thread " + thread);
            }
            HprofData.ThreadEvent event = HprofData.ThreadEvent.end(threadId.intValue());
            SamplingProfiler.this.hprofData.addThreadEvent(event);
        }
    }
}
