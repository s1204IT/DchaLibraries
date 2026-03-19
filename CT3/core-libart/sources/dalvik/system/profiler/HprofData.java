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

    private static final int[] f122dalviksystemprofilerHprofData$ThreadEventTypeSwitchesValues = null;
    private int depth;
    private int flags;
    private final Map<StackTrace, int[]> stackTraces;
    private long startMillis;
    private final List<ThreadEvent> threadHistory = new ArrayList();
    private final Map<Integer, ThreadEvent> threadIdToThreadEvent = new HashMap();

    private static int[] m308xb57bcfd4() {
        if (f122dalviksystemprofilerHprofData$ThreadEventTypeSwitchesValues != null) {
            return f122dalviksystemprofilerHprofData$ThreadEventTypeSwitchesValues;
        }
        int[] iArr = new int[ThreadEventType.valuesCustom().length];
        try {
            iArr[ThreadEventType.END.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[ThreadEventType.START.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        f122dalviksystemprofilerHprofData$ThreadEventTypeSwitchesValues = iArr;
        return iArr;
    }

    public enum ThreadEventType {
        START,
        END;

        public static ThreadEventType[] valuesCustom() {
            return values();
        }
    }

    public static final class ThreadEvent {

        private static final int[] f123dalviksystemprofilerHprofData$ThreadEventTypeSwitchesValues = null;
        public final String groupName;
        public final int objectId;
        public final String parentGroupName;
        public final int threadId;
        public final String threadName;
        public final ThreadEventType type;

        private static int[] m309xb57bcfd4() {
            if (f123dalviksystemprofilerHprofData$ThreadEventTypeSwitchesValues != null) {
                return f123dalviksystemprofilerHprofData$ThreadEventTypeSwitchesValues;
            }
            int[] iArr = new int[ThreadEventType.valuesCustom().length];
            try {
                iArr[ThreadEventType.END.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[ThreadEventType.START.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            f123dalviksystemprofilerHprofData$ThreadEventTypeSwitchesValues = iArr;
            return iArr;
        }

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

        public boolean equals(Object obj) {
            if ((obj instanceof ThreadEvent) && this.type == obj.type && this.objectId == obj.objectId && this.threadId == obj.threadId && equal(this.threadName, obj.threadName) && equal(this.groupName, obj.groupName)) {
                return equal(this.parentGroupName, obj.parentGroupName);
            }
            return false;
        }

        private static boolean equal(Object a, Object b) {
            if (a == b) {
                return true;
            }
            if (a != null) {
                return a.equals(b);
            }
            return false;
        }

        public String toString() {
            switch (m309xb57bcfd4()[this.type.ordinal()]) {
                case 1:
                    return String.format("THREAD END (id = %d)", Integer.valueOf(this.threadId));
                case 2:
                    return String.format("THREAD START (obj=%d, id = %d, name=\"%s\", group=\"%s\")", Integer.valueOf(this.objectId), Integer.valueOf(this.threadId), this.threadName, this.groupName);
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

        public boolean equals(Object obj) {
            if ((obj instanceof StackTrace) && this.threadId == obj.threadId) {
                return Arrays.equals(this.stackFrames, obj.stackFrames);
            }
            return false;
        }

        public String toString() {
            StringBuilder frames = new StringBuilder();
            if (this.stackFrames.length > 0) {
                frames.append('\n');
                for (StackTraceElement stackFrame : this.stackFrames) {
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

        Sample(StackTrace stackTrace, int count, Sample sample) {
            this(stackTrace, count);
        }

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

        public boolean equals(Object obj) {
            if ((obj instanceof Sample) && this.count == obj.count) {
                return this.stackTrace.equals(obj.stackTrace);
            }
            return false;
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
            Sample sample = new Sample(stackTrace, count, null);
            samples.add(sample);
        }
        return samples;
    }

    public void addThreadEvent(ThreadEvent event) {
        if (event == null) {
            throw new NullPointerException("event == null");
        }
        ThreadEvent old = this.threadIdToThreadEvent.put(Integer.valueOf(event.threadId), event);
        switch (m308xb57bcfd4()[event.type.ordinal()]) {
            case 1:
                if (old != null && old.type == ThreadEventType.END) {
                    throw new IllegalArgumentException("Duplicate ThreadEvent.end for id " + event.threadId);
                }
                break;
            case 2:
                if (old != null) {
                    throw new IllegalArgumentException("ThreadEvent already registered for id " + event.threadId);
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
        if (old == null) {
        } else {
            throw new IllegalArgumentException("StackTrace already registered for id " + stackTrace.stackTraceId + ":\n" + stackTrace);
        }
    }
}
