package dalvik.system.profiler;

import dalvik.system.profiler.BinaryHprof;
import dalvik.system.profiler.HprofData;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class BinaryHprofReader {
    private static final boolean TRACE = false;
    private final DataInputStream in;
    private String version;
    private boolean strict = true;
    private final Map<HprofData.StackTrace, int[]> stackTraces = new HashMap();
    private final HprofData hprofData = new HprofData(this.stackTraces);
    private final Map<Integer, String> idToString = new HashMap();
    private final Map<Integer, String> idToClassName = new HashMap();
    private final Map<Integer, StackTraceElement> idToStackFrame = new HashMap();
    private final Map<Integer, HprofData.StackTrace> idToStackTrace = new HashMap();

    public BinaryHprofReader(InputStream inputStream) throws IOException {
        this.in = new DataInputStream(inputStream);
    }

    public boolean getStrict() {
        return this.strict;
    }

    public void setStrict(boolean strict) {
        if (this.version != null) {
            throw new IllegalStateException("cannot set strict after read()");
        }
        this.strict = strict;
    }

    private void checkRead() {
        if (this.version == null) {
            throw new IllegalStateException("data access before read()");
        }
    }

    public String getVersion() {
        checkRead();
        return this.version;
    }

    public HprofData getHprofData() {
        checkRead();
        return this.hprofData;
    }

    public void read() throws IOException {
        parseHeader();
        parseRecords();
    }

    private void parseHeader() throws IOException {
        parseVersion();
        parseIdSize();
        parseTime();
    }

    private void parseVersion() throws IOException {
        String version = BinaryHprof.readMagic(this.in);
        if (version == null) {
            throw new MalformedHprofException("Could not find HPROF version");
        }
        this.version = version;
    }

    private void parseIdSize() throws IOException {
        int idSize = this.in.readInt();
        if (idSize != 4) {
            throw new MalformedHprofException("Unsupported identifier size: " + idSize);
        }
    }

    private void parseTime() throws IOException {
        long time = this.in.readLong();
        this.hprofData.setStartMillis(time);
    }

    private void parseRecords() throws IOException {
        while (parseRecord()) {
        }
    }

    private boolean parseRecord() throws IOException {
        int tagOrEOF = this.in.read();
        if (tagOrEOF == -1) {
            return false;
        }
        byte tag = (byte) tagOrEOF;
        this.in.readInt();
        int recordLength = this.in.readInt();
        BinaryHprof.Tag hprofTag = BinaryHprof.Tag.get(tag);
        if (hprofTag == null) {
            skipRecord(hprofTag, recordLength);
            return true;
        }
        String error = hprofTag.checkSize(recordLength);
        if (error != null) {
            throw new MalformedHprofException(error);
        }
        switch (hprofTag) {
            case CONTROL_SETTINGS:
                parseControlSettings();
                return true;
            case STRING_IN_UTF8:
                parseStringInUtf8(recordLength);
                return true;
            case START_THREAD:
                parseStartThread();
                return true;
            case END_THREAD:
                parseEndThread();
                return true;
            case LOAD_CLASS:
                parseLoadClass();
                return true;
            case STACK_FRAME:
                parseStackFrame();
                return true;
            case STACK_TRACE:
                parseStackTrace(recordLength);
                return true;
            case CPU_SAMPLES:
                parseCpuSamples(recordLength);
                return true;
            default:
                skipRecord(hprofTag, recordLength);
                return true;
        }
    }

    private void skipRecord(BinaryHprof.Tag hprofTag, long recordLength) throws IOException {
        long skipped = this.in.skip(recordLength);
        if (skipped != recordLength) {
            throw new EOFException("Expected to skip " + recordLength + " bytes but only skipped " + skipped + " bytes");
        }
    }

    private void parseControlSettings() throws IOException {
        int flags = this.in.readInt();
        short depth = this.in.readShort();
        this.hprofData.setFlags(flags);
        this.hprofData.setDepth(depth);
    }

    private void parseStringInUtf8(int recordLength) throws IOException {
        int stringId = this.in.readInt();
        byte[] bytes = new byte[recordLength - 4];
        readFully(this.in, bytes);
        String string = new String(bytes, "UTF-8");
        String old = this.idToString.put(Integer.valueOf(stringId), string);
        if (old != null) {
            throw new MalformedHprofException("Duplicate string id: " + stringId);
        }
    }

    private static void readFully(InputStream in, byte[] dst) throws IOException {
        int offset = 0;
        int byteCount = dst.length;
        while (byteCount > 0) {
            int bytesRead = in.read(dst, offset, byteCount);
            if (bytesRead < 0) {
                throw new EOFException();
            }
            offset += bytesRead;
            byteCount -= bytesRead;
        }
    }

    private void parseLoadClass() throws IOException {
        int classId = this.in.readInt();
        readId();
        this.in.readInt();
        String className = readString();
        String old = this.idToClassName.put(Integer.valueOf(classId), className);
        if (old != null) {
            throw new MalformedHprofException("Duplicate class id: " + classId);
        }
    }

    private int readId() throws IOException {
        return this.in.readInt();
    }

    private String readString() throws IOException {
        int id = readId();
        if (id == 0) {
            return null;
        }
        String string = this.idToString.get(Integer.valueOf(id));
        if (string == null) {
            throw new MalformedHprofException("Unknown string id " + id);
        }
        return string;
    }

    private String readClass() throws IOException {
        int id = readId();
        String string = this.idToClassName.get(Integer.valueOf(id));
        if (string == null) {
            throw new MalformedHprofException("Unknown class id " + id);
        }
        return string;
    }

    private void parseStartThread() throws IOException {
        int threadId = this.in.readInt();
        int objectId = readId();
        this.in.readInt();
        String threadName = readString();
        String groupName = readString();
        String parentGroupName = readString();
        HprofData.ThreadEvent event = HprofData.ThreadEvent.start(objectId, threadId, threadName, groupName, parentGroupName);
        this.hprofData.addThreadEvent(event);
    }

    private void parseEndThread() throws IOException {
        int threadId = this.in.readInt();
        HprofData.ThreadEvent event = HprofData.ThreadEvent.end(threadId);
        this.hprofData.addThreadEvent(event);
    }

    private void parseStackFrame() throws IOException {
        int stackFrameId = readId();
        String methodName = readString();
        readString();
        String file = readString();
        String className = readClass();
        int line = this.in.readInt();
        StackTraceElement stackFrame = new StackTraceElement(className, methodName, file, line);
        StackTraceElement old = this.idToStackFrame.put(Integer.valueOf(stackFrameId), stackFrame);
        if (old != null) {
            throw new MalformedHprofException("Duplicate stack frame id: " + stackFrameId);
        }
    }

    private void parseStackTrace(int recordLength) throws IOException {
        int stackTraceId = this.in.readInt();
        int threadId = this.in.readInt();
        int frames = this.in.readInt();
        int expectedLength = (frames * 4) + 12;
        if (recordLength != expectedLength) {
            throw new MalformedHprofException("Expected stack trace record of size " + expectedLength + " based on number of frames but header specified a length of  " + recordLength);
        }
        StackTraceElement[] stackFrames = new StackTraceElement[frames];
        for (int i = 0; i < frames; i++) {
            int stackFrameId = readId();
            StackTraceElement stackFrame = this.idToStackFrame.get(Integer.valueOf(stackFrameId));
            if (stackFrame == null) {
                throw new MalformedHprofException("Unknown stack frame id " + stackFrameId);
            }
            stackFrames[i] = stackFrame;
        }
        HprofData.StackTrace stackTrace = new HprofData.StackTrace(stackTraceId, threadId, stackFrames);
        if (this.strict) {
            this.hprofData.addStackTrace(stackTrace, new int[1]);
        } else {
            int[] countCell = this.stackTraces.get(stackTrace);
            if (countCell == null) {
                this.hprofData.addStackTrace(stackTrace, new int[1]);
            }
        }
        HprofData.StackTrace old = this.idToStackTrace.put(Integer.valueOf(stackTraceId), stackTrace);
        if (old != null) {
            throw new MalformedHprofException("Duplicate stack trace id: " + stackTraceId);
        }
    }

    private void parseCpuSamples(int recordLength) throws IOException {
        int totalSamples = this.in.readInt();
        int samplesCount = this.in.readInt();
        int expectedLength = (samplesCount * 8) + 8;
        if (recordLength != expectedLength) {
            throw new MalformedHprofException("Expected CPU samples record of size " + expectedLength + " based on number of samples but header specified a length of  " + recordLength);
        }
        int total = 0;
        for (int i = 0; i < samplesCount; i++) {
            int count = this.in.readInt();
            int stackTraceId = this.in.readInt();
            HprofData.StackTrace stackTrace = this.idToStackTrace.get(Integer.valueOf(stackTraceId));
            if (stackTrace == null) {
                throw new MalformedHprofException("Unknown stack trace id " + stackTraceId);
            }
            if (count == 0) {
                throw new MalformedHprofException("Zero sample count for stack trace " + stackTrace);
            }
            int[] countCell = this.stackTraces.get(stackTrace);
            if (this.strict) {
                if (countCell[0] != 0) {
                    throw new MalformedHprofException("Setting sample count of stack trace " + stackTrace + " to " + count + " found it was already initialized to " + countCell[0]);
                }
            } else {
                count += countCell[0];
            }
            countCell[0] = count;
            total += count;
        }
        if (this.strict && totalSamples != total) {
            throw new MalformedHprofException("Expected a total of " + totalSamples + " samples but saw " + total);
        }
    }
}
