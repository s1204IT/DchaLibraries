package dalvik.system.profiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HprofData {
    private int depth;
    private int flags;
    private final Map<StackTrace, int[]> stackTraces;
    private long startMillis;
    private final List<ThreadEvent> threadHistory = new ArrayList();
    private final Map<Integer, ThreadEvent> threadIdToThreadEvent = new HashMap();

    public enum ThreadEventType {
        START,
        END
    }

    public static final class ThreadEvent {
        public final String groupName;
        public final int objectId;
        public final String parentGroupName;
        public final int threadId;
        public final String threadName;
        public final ThreadEventType type;

        public static ThreadEvent start(int objectId, int threadId, String threadName, String groupName, String parentGroupName) {
            return new ThreadEvent(ThreadEventType.START, objectId, threadId, threadName, groupName, parentGroupName);
        }

        public static ThreadEvent end(int threadId) {
            return new ThreadEvent(ThreadEventType.END, threadId);
        }

        private ThreadEvent(ThreadEventType type, int objectId, int threadId, String threadName, String groupName, String parentGroupName) {
            if (threadName == null) {
                throw new NullPointerException("threadName == null");
            }
            this.type = ThreadEventType.START;
            this.objectId = objectId;
            this.threadId = threadId;
            this.threadName = threadName;
            this.groupName = groupName;
            this.parentGroupName = parentGroupName;
        }

        private ThreadEvent(ThreadEventType type, int threadId) {
            this.type = ThreadEventType.END;
            this.objectId = -1;
            this.threadId = threadId;
            this.threadName = null;
            this.groupName = null;
            this.parentGroupName = null;
        }

        public int hashCode() {
            int result = this.objectId + 527;
            return (((((((result * 31) + this.threadId) * 31) + hashCode(this.threadName)) * 31) + hashCode(this.groupName)) * 31) + hashCode(this.parentGroupName);
        }

        private static int hashCode(Object o) {
            if (o == null) {
                return 0;
            }
            return o.hashCode();
        }

        public boolean equals(Object o) {
            if (!(o instanceof ThreadEvent)) {
                return false;
            }
            ThreadEvent event = (ThreadEvent) o;
            return this.type == event.type && this.objectId == event.objectId && this.threadId == event.threadId && equal(this.threadName, event.threadName) && equal(this.groupName, event.groupName) && equal(this.parentGroupName, event.parentGroupName);
        }

        private static boolean equal(Object a, Object b) {
            return a == b || (a != null && a.equals(b));
        }

        public String toString() {
            switch (this.type) {
                case START:
                    return String.format("THREAD START (obj=%d, id = %d, name=\"%s\", group=\"%s\")", Integer.valueOf(this.objectId), Integer.valueOf(this.threadId), this.threadName, this.groupName);
                case END:
                    return String.format("THREAD END (id = %d)", Integer.valueOf(this.threadId));
                default:
                    throw new IllegalStateException(this.type.toString());
            }
        }
    }

    public static final class StackTrace {
        StackTraceElement[] stackFrames;
        public final int stackTraceId;
        int threadId;

        StackTrace() {
            this.stackTraceId = -1;
        }

        public StackTrace(int stackTraceId, int threadId, StackTraceElement[] stackFrames) {
            if (stackFrames == null) {
                throw new NullPointerException("stackFrames == null");
            }
            this.stackTraceId = stackTraceId;
            this.threadId = threadId;
            this.stackFrames = stackFrames;
        }

        public int getThreadId() {
            return this.threadId;
        }

        public StackTraceElement[] getStackFrames() {
            return this.stackFrames;
        }

        public int hashCode() {
            int result = this.threadId + 527;
            return (result * 31) + Arrays.hashCode(this.stackFrames);
        }

        public boolean equals(Object o) {
            if (!(o instanceof StackTrace)) {
                return false;
            }
            StackTrace s = (StackTrace) o;
            return this.threadId == s.threadId && Arrays.equals(this.stackFrames, s.stackFrames);
        }

        public String toString() {
            StringBuilder frames = new StringBuilder();
            if (this.stackFrames.length > 0) {
                frames.append('\n');
                StackTraceElement[] arr$ = this.stackFrames;
                for (StackTraceElement stackFrame : arr$) {
                    frames.append("\t at ");
                    frames.append(stackFrame);
                    frames.append('\n');
                }
            } else {
                frames.append("<empty>");
            }
            return "StackTrace[stackTraceId=" + this.stackTraceId + ", threadId=" + this.threadId + ", frames=" + ((Object) frames) + "]";
        }
    }

    public static final class Sample {
        public final int count;
        public final StackTrace stackTrace;

        private Sample(StackTrace stackTrace, int count) {
            if (stackTrace == null) {
                throw new NullPointerException("stackTrace == null");
            }
            if (count < 0) {
                throw new IllegalArgumentException("count < 0:" + count);
            }
            this.stackTrace = stackTrace;
            this.count = count;
        }

        public int hashCode() {
            int result = this.stackTrace.hashCode() + 527;
            return (result * 31) + this.count;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Sample)) {
                return false;
            }
            Sample s = (Sample) o;
            return this.count == s.count && this.stackTrace.equals(s.stackTrace);
        }

        public String toString() {
            return "Sample[count=" + this.count + " " + this.stackTrace + "]";
        }
    }

    public HprofData(Map<StackTrace, int[]> stackTraces) {
        if (stackTraces == null) {
            throw new NullPointerException("stackTraces == null");
        }
        this.stackTraces = stackTraces;
    }

    public long getStartMillis() {
        return this.startMillis;
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
    }

    public int getFlags() {
        return this.flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getDepth() {
        return this.depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public List<ThreadEvent> getThreadHistory() {
        return Collections.unmodifiableList(this.threadHistory);
    }

    public Set<Sample> getSamples() {
        Set<Sample> samples = new HashSet<>(this.stackTraces.size());
        for (Map.Entry<StackTrace, int[]> e : this.stackTraces.entrySet()) {
            StackTrace stackTrace = e.getKey();
            int[] countCell = e.getValue();
            int count = countCell[0];
            Sample sample = new Sample(stackTrace, count);
            samples.add(sample);
        }
        return samples;
    }

    public void addThreadEvent(ThreadEvent event) {
        if (event == null) {
            throw new NullPointerException("event == null");
        }
        ThreadEvent old = this.threadIdToThreadEvent.put(Integer.valueOf(event.threadId), event);
        switch (event.type) {
            case START:
                if (old != null) {
                    throw new IllegalArgumentException("ThreadEvent already registered for id " + event.threadId);
                }
                break;
            case END:
                if (old != null && old.type == ThreadEventType.END) {
                    throw new IllegalArgumentException("Duplicate ThreadEvent.end for id " + event.threadId);
                }
                break;
        }
        this.threadHistory.add(event);
    }

    public void addStackTrace(StackTrace stackTrace, int[] countCell) {
        if (!this.threadIdToThreadEvent.containsKey(Integer.valueOf(stackTrace.threadId))) {
            throw new IllegalArgumentException("Unknown thread id " + stackTrace.threadId);
        }
        int[] old = this.stackTraces.put(stackTrace, countCell);
        if (old != null) {
            throw new IllegalArgumentException("StackTrace already registered for id " + stackTrace.stackTraceId + ":\n" + stackTrace);
        }
    }
}
