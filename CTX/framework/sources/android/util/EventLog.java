package android.util;

import android.annotation.SystemApi;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventLog {
    private static final String COMMENT_PATTERN = "^\\s*(#.*)?$";
    private static final String TAG = "EventLog";
    private static final String TAGS_FILE = "/system/etc/event-log-tags";
    private static final String TAG_PATTERN = "^\\s*(\\d+)\\s+(\\w+)\\s*(\\(.*\\))?\\s*$";
    private static HashMap<String, Integer> sTagCodes = null;
    private static HashMap<Integer, String> sTagNames = null;

    public static native void readEvents(int[] iArr, Collection<Event> collection) throws IOException;

    @SystemApi
    public static native void readEventsOnWrapping(int[] iArr, long j, Collection<Event> collection) throws IOException;

    public static native int writeEvent(int i, float f);

    public static native int writeEvent(int i, int i2);

    public static native int writeEvent(int i, long j);

    public static native int writeEvent(int i, String str);

    public static native int writeEvent(int i, Object... objArr);

    public static final class Event {
        private static final int DATA_OFFSET = 4;
        private static final byte FLOAT_TYPE = 4;
        private static final int HEADER_SIZE_OFFSET = 2;
        private static final byte INT_TYPE = 0;
        private static final int LENGTH_OFFSET = 0;
        private static final byte LIST_TYPE = 3;
        private static final byte LONG_TYPE = 1;
        private static final int NANOSECONDS_OFFSET = 16;
        private static final int PROCESS_OFFSET = 4;
        private static final int SECONDS_OFFSET = 12;
        private static final byte STRING_TYPE = 2;
        private static final int THREAD_OFFSET = 8;
        private static final int UID_OFFSET = 24;
        private static final int V1_PAYLOAD_START = 20;
        private final ByteBuffer mBuffer;
        private Exception mLastWtf;

        Event(byte[] bArr) {
            this.mBuffer = ByteBuffer.wrap(bArr);
            this.mBuffer.order(ByteOrder.nativeOrder());
        }

        public int getProcessId() {
            return this.mBuffer.getInt(4);
        }

        @SystemApi
        public int getUid() {
            try {
                return this.mBuffer.getInt(24);
            } catch (IndexOutOfBoundsException e) {
                return -1;
            }
        }

        public int getThreadId() {
            return this.mBuffer.getInt(8);
        }

        public long getTimeNanos() {
            return (((long) this.mBuffer.getInt(12)) * 1000000000) + ((long) this.mBuffer.getInt(16));
        }

        public int getTag() {
            short s = this.mBuffer.getShort(2);
            if (s == 0) {
                s = 20;
            }
            return this.mBuffer.getInt(s);
        }

        public synchronized Object getData() {
            try {
                short s = this.mBuffer.getShort(2);
                if (s == 0) {
                    s = 20;
                }
                this.mBuffer.limit(this.mBuffer.getShort(0) + s);
                int i = s + 4;
                if (i >= this.mBuffer.limit()) {
                    return null;
                }
                this.mBuffer.position(i);
                return decodeObject();
            } catch (IllegalArgumentException e) {
                Log.wtf(EventLog.TAG, "Illegal entry payload: tag=" + getTag(), e);
                this.mLastWtf = e;
                return null;
            } catch (BufferUnderflowException e2) {
                Log.wtf(EventLog.TAG, "Truncated entry payload: tag=" + getTag(), e2);
                this.mLastWtf = e2;
                return null;
            }
        }

        private Object decodeObject() {
            byte b = this.mBuffer.get();
            switch (b) {
                case 0:
                    return Integer.valueOf(this.mBuffer.getInt());
                case 1:
                    return Long.valueOf(this.mBuffer.getLong());
                case 2:
                    try {
                        int i = this.mBuffer.getInt();
                        int iPosition = this.mBuffer.position();
                        this.mBuffer.position(iPosition + i);
                        return new String(this.mBuffer.array(), iPosition, i, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.wtf(EventLog.TAG, "UTF-8 is not supported", e);
                        this.mLastWtf = e;
                        return null;
                    }
                case 3:
                    int i2 = this.mBuffer.get();
                    if (i2 < 0) {
                        i2 += 256;
                    }
                    Object[] objArr = new Object[i2];
                    for (int i3 = 0; i3 < i2; i3++) {
                        objArr[i3] = decodeObject();
                    }
                    return objArr;
                case 4:
                    return Float.valueOf(this.mBuffer.getFloat());
                default:
                    throw new IllegalArgumentException("Unknown entry type: " + ((int) b));
            }
        }

        public static Event fromBytes(byte[] bArr) {
            return new Event(bArr);
        }

        public byte[] getBytes() {
            byte[] bArrArray = this.mBuffer.array();
            return Arrays.copyOf(bArrArray, bArrArray.length);
        }

        public Exception getLastError() {
            return this.mLastWtf;
        }

        public void clearError() {
            this.mLastWtf = null;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return Arrays.equals(this.mBuffer.array(), ((Event) obj).mBuffer.array());
        }

        public int hashCode() {
            return Arrays.hashCode(this.mBuffer.array());
        }
    }

    public static String getTagName(int i) {
        readTagsFile();
        return sTagNames.get(Integer.valueOf(i));
    }

    public static int getTagCode(String str) {
        readTagsFile();
        Integer num = sTagCodes.get(str);
        if (num != null) {
            return num.intValue();
        }
        return -1;
    }

    private static synchronized void readTagsFile() {
        BufferedReader bufferedReader;
        String line;
        if (sTagCodes == null || sTagNames == null) {
            sTagCodes = new HashMap<>();
            sTagNames = new HashMap<>();
            Pattern patternCompile = Pattern.compile(COMMENT_PATTERN);
            Pattern patternCompile2 = Pattern.compile(TAG_PATTERN);
            BufferedReader bufferedReader2 = null;
            bufferedReader2 = null;
            bufferedReader2 = null;
            try {
                try {
                    try {
                        bufferedReader = new BufferedReader(new FileReader(TAGS_FILE), 256);
                        while (true) {
                            try {
                                line = bufferedReader.readLine();
                                if (line == null) {
                                    break;
                                }
                                if (!patternCompile.matcher(line).matches()) {
                                    Matcher matcher = patternCompile2.matcher(line);
                                    if (matcher.matches()) {
                                        try {
                                            int i = Integer.parseInt(matcher.group(1));
                                            String strGroup = matcher.group(2);
                                            sTagCodes.put(strGroup, Integer.valueOf(i));
                                            sTagNames.put(Integer.valueOf(i), strGroup);
                                        } catch (NumberFormatException e) {
                                            Log.wtf(TAG, "Error in /system/etc/event-log-tags: " + line, e);
                                        }
                                    } else {
                                        Log.wtf(TAG, "Bad entry in /system/etc/event-log-tags: " + line);
                                    }
                                }
                            } catch (IOException e2) {
                                e = e2;
                                bufferedReader2 = bufferedReader;
                                Log.wtf(TAG, "Error reading /system/etc/event-log-tags", e);
                                if (bufferedReader2 != null) {
                                    bufferedReader2.close();
                                    bufferedReader2 = bufferedReader2;
                                }
                            } catch (Throwable th) {
                                th = th;
                                if (bufferedReader != null) {
                                    try {
                                        bufferedReader.close();
                                    } catch (IOException e3) {
                                    }
                                }
                                throw th;
                            }
                        }
                        bufferedReader.close();
                        bufferedReader2 = line;
                    } catch (Throwable th2) {
                        th = th2;
                        bufferedReader = bufferedReader2;
                    }
                } catch (IOException e4) {
                    e = e4;
                }
            } catch (IOException e5) {
            }
        }
    }
}
