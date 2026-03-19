package dalvik.system.profiler;

import dalvik.system.profiler.BinaryHprof;
import dalvik.system.profiler.HprofData;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class BinaryHprofWriter {

    private static final int[] f121dalviksystemprofilerHprofData$ThreadEventTypeSwitchesValues = null;
    private final HprofData data;
    private final DataOutputStream out;
    private int nextStringId = 1;
    private int nextClassId = 1;
    private int nextStackFrameId = 1;
    private final Map<String, Integer> stringToId = new HashMap();
    private final Map<String, Integer> classNameToId = new HashMap();
    private final Map<StackTraceElement, Integer> stackFrameToId = new HashMap();

    private static int[] m307xb57bcfd4() {
        if (f121dalviksystemprofilerHprofData$ThreadEventTypeSwitchesValues != null) {
            return f121dalviksystemprofilerHprofData$ThreadEventTypeSwitchesValues;
        }
        int[] iArr = new int[HprofData.ThreadEventType.valuesCustom().length];
        try {
            iArr[HprofData.ThreadEventType.END.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[HprofData.ThreadEventType.START.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        f121dalviksystemprofilerHprofData$ThreadEventTypeSwitchesValues = iArr;
        return iArr;
    }

    public static void write(HprofData data, OutputStream outputStream) throws IOException {
        new BinaryHprofWriter(data, outputStream).write();
    }

    private BinaryHprofWriter(HprofData data, OutputStream outputStream) {
        this.data = data;
        this.out = new DataOutputStream(outputStream);
    }

    private void write() throws IOException {
        try {
            writeHeader(this.data.getStartMillis());
            writeControlSettings(this.data.getFlags(), this.data.getDepth());
            for (HprofData.ThreadEvent event : this.data.getThreadHistory()) {
                writeThreadEvent(event);
            }
            Set<HprofData.Sample> samples = this.data.getSamples();
            int total = 0;
            for (HprofData.Sample sample : samples) {
                total += sample.count;
                writeStackTrace(sample.stackTrace);
            }
            writeCpuSamples(total, samples);
        } finally {
            this.out.flush();
        }
    }

    private void writeHeader(long dumpTimeInMilliseconds) throws IOException {
        this.out.writeBytes(BinaryHprof.MAGIC + "1.0.2");
        this.out.writeByte(0);
        this.out.writeInt(4);
        this.out.writeLong(dumpTimeInMilliseconds);
    }

    private void writeControlSettings(int flags, int depth) throws IOException {
        if (depth > 32767) {
            throw new IllegalArgumentException("depth too large for binary hprof: " + depth + " > 32767");
        }
        writeRecordHeader(BinaryHprof.Tag.CONTROL_SETTINGS, 0, BinaryHprof.Tag.CONTROL_SETTINGS.maximumSize);
        this.out.writeInt(flags);
        this.out.writeShort((short) depth);
    }

    private void writeThreadEvent(HprofData.ThreadEvent e) throws IOException {
        switch (m307xb57bcfd4()[e.type.ordinal()]) {
            case 1:
                writeStopThread(e);
                return;
            case 2:
                writeStartThread(e);
                return;
            default:
                throw new IllegalStateException(e.type.toString());
        }
    }

    private void writeStartThread(HprofData.ThreadEvent e) throws IOException {
        int threadNameId = writeString(e.threadName);
        int groupNameId = writeString(e.groupName);
        int parentGroupNameId = writeString(e.parentGroupName);
        writeRecordHeader(BinaryHprof.Tag.START_THREAD, 0, BinaryHprof.Tag.START_THREAD.maximumSize);
        this.out.writeInt(e.threadId);
        writeId(e.objectId);
        this.out.writeInt(0);
        writeId(threadNameId);
        writeId(groupNameId);
        writeId(parentGroupNameId);
    }

    private void writeStopThread(HprofData.ThreadEvent e) throws IOException {
        writeRecordHeader(BinaryHprof.Tag.END_THREAD, 0, BinaryHprof.Tag.END_THREAD.maximumSize);
        this.out.writeInt(e.threadId);
    }

    private void writeRecordHeader(BinaryHprof.Tag hprofTag, int timeDeltaInMicroseconds, int recordLength) throws IOException {
        String error = hprofTag.checkSize(recordLength);
        if (error != null) {
            throw new AssertionError(error);
        }
        this.out.writeByte(hprofTag.tag);
        this.out.writeInt(timeDeltaInMicroseconds);
        this.out.writeInt(recordLength);
    }

    private void writeId(int id) throws IOException {
        this.out.writeInt(id);
    }

    private int writeString(String string) throws IOException {
        if (string == null) {
            return 0;
        }
        Integer identifier = this.stringToId.get(string);
        if (identifier != null) {
            return identifier.intValue();
        }
        int id = this.nextStringId;
        this.nextStringId = id + 1;
        this.stringToId.put(string, Integer.valueOf(id));
        byte[] bytes = string.getBytes("UTF-8");
        writeRecordHeader(BinaryHprof.Tag.STRING_IN_UTF8, 0, bytes.length + 4);
        this.out.writeInt(id);
        this.out.write(bytes, 0, bytes.length);
        return id;
    }

    private void writeCpuSamples(int totalSamples, Set<HprofData.Sample> samples) throws IOException {
        int samplesCount = samples.size();
        if (samplesCount == 0) {
            return;
        }
        writeRecordHeader(BinaryHprof.Tag.CPU_SAMPLES, 0, (samplesCount * 8) + 8);
        this.out.writeInt(totalSamples);
        this.out.writeInt(samplesCount);
        for (HprofData.Sample sample : samples) {
            this.out.writeInt(sample.count);
            this.out.writeInt(sample.stackTrace.stackTraceId);
        }
    }

    private void writeStackTrace(HprofData.StackTrace stackTrace) throws IOException {
        int frames = stackTrace.stackFrames.length;
        int[] stackFrameIds = new int[frames];
        for (int i = 0; i < frames; i++) {
            stackFrameIds[i] = writeStackFrame(stackTrace.stackFrames[i]);
        }
        writeRecordHeader(BinaryHprof.Tag.STACK_TRACE, 0, (frames * 4) + 12);
        this.out.writeInt(stackTrace.stackTraceId);
        this.out.writeInt(stackTrace.threadId);
        this.out.writeInt(frames);
        for (int stackFrameId : stackFrameIds) {
            writeId(stackFrameId);
        }
    }

    private int writeLoadClass(String className) throws IOException {
        Integer identifier = this.classNameToId.get(className);
        if (identifier != null) {
            return identifier.intValue();
        }
        int id = this.nextClassId;
        this.nextClassId = id + 1;
        this.classNameToId.put(className, Integer.valueOf(id));
        int classNameId = writeString(className);
        writeRecordHeader(BinaryHprof.Tag.LOAD_CLASS, 0, BinaryHprof.Tag.LOAD_CLASS.maximumSize);
        this.out.writeInt(id);
        writeId(0);
        this.out.writeInt(0);
        writeId(classNameId);
        return id;
    }

    private int writeStackFrame(StackTraceElement stackFrame) throws IOException {
        Integer identifier = this.stackFrameToId.get(stackFrame);
        if (identifier != null) {
            return identifier.intValue();
        }
        int id = this.nextStackFrameId;
        this.nextStackFrameId = id + 1;
        this.stackFrameToId.put(stackFrame, Integer.valueOf(id));
        int classId = writeLoadClass(stackFrame.getClassName());
        int methodNameId = writeString(stackFrame.getMethodName());
        int sourceId = writeString(stackFrame.getFileName());
        writeRecordHeader(BinaryHprof.Tag.STACK_FRAME, 0, BinaryHprof.Tag.STACK_FRAME.maximumSize);
        writeId(id);
        writeId(methodNameId);
        writeId(0);
        writeId(sourceId);
        this.out.writeInt(classId);
        this.out.writeInt(stackFrame.getLineNumber());
        return id;
    }
}
