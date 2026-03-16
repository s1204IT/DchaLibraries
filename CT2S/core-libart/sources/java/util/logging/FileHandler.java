package java.util.logging;

import dalvik.bytecode.Opcodes;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Hashtable;
import libcore.io.IoUtils;

public class FileHandler extends StreamHandler {
    private static final boolean DEFAULT_APPEND = false;
    private static final int DEFAULT_COUNT = 1;
    private static final int DEFAULT_LIMIT = 0;
    private static final String DEFAULT_PATTERN = "%h/java%u.log";
    private static final String LCK_EXT = ".lck";
    private static final Hashtable<String, FileLock> allLocks = new Hashtable<>();
    private boolean append;
    private int count;
    private File[] files;
    private int limit;
    private LogManager manager;
    private MeasureOutputStream output;
    private String pattern;
    FileLock lock = null;
    String fileName = null;
    int uniqueID = -1;

    public FileHandler() throws IOException {
        init(null, null, null, null);
    }

    private void init(String p, Boolean a, Integer l, Integer c) throws IOException {
        this.manager = LogManager.getLogManager();
        this.manager.checkAccess();
        initProperties(p, a, l, c);
        initOutputFiles();
    }

    private void initOutputFiles() throws IOException {
        while (true) {
            this.uniqueID++;
            for (int generation = 0; generation < this.count; generation++) {
                this.files[generation] = new File(parseFileName(generation));
            }
            this.fileName = this.files[0].getAbsolutePath();
            synchronized (allLocks) {
                if (allLocks.get(this.fileName) == null) {
                    if (this.files[0].exists() && (!this.append || this.files[0].length() >= this.limit)) {
                        for (int i = this.count - 1; i > 0; i--) {
                            if (this.files[i].exists()) {
                                this.files[i].delete();
                            }
                            this.files[i - 1].renameTo(this.files[i]);
                        }
                    }
                    FileOutputStream fileStream = new FileOutputStream(this.fileName + LCK_EXT);
                    FileChannel channel = fileStream.getChannel();
                    this.lock = channel.tryLock();
                    if (this.lock == null) {
                        IoUtils.closeQuietly(fileStream);
                    } else {
                        allLocks.put(this.fileName, this.lock);
                        this.output = new MeasureOutputStream(new BufferedOutputStream(new FileOutputStream(this.fileName, this.append)), this.files[0].length());
                        setOutputStream(this.output);
                        return;
                    }
                }
            }
        }
    }

    private void initProperties(String p, Boolean a, Integer l, Integer c) {
        super.initProperties("ALL", (String) null, "java.util.logging.XMLFormatter", (String) null);
        String className = getClass().getName();
        if (p == null) {
            p = getStringProperty(className + ".pattern", DEFAULT_PATTERN);
        }
        this.pattern = p;
        if (this.pattern == null) {
            throw new NullPointerException("pattern == null");
        }
        if (this.pattern.isEmpty()) {
            throw new NullPointerException("pattern.isEmpty()");
        }
        this.append = a == null ? getBooleanProperty(className + ".append", false) : a.booleanValue();
        this.count = c == null ? getIntProperty(className + ".count", 1) : c.intValue();
        this.limit = l == null ? getIntProperty(className + ".limit", 0) : l.intValue();
        this.count = this.count < 1 ? 1 : this.count;
        this.limit = this.limit < 0 ? 0 : this.limit;
        this.files = new File[this.count];
    }

    void findNextGeneration() {
        super.close();
        for (int i = this.count - 1; i > 0; i--) {
            if (this.files[i].exists()) {
                this.files[i].delete();
            }
            this.files[i - 1].renameTo(this.files[i]);
        }
        try {
            this.output = new MeasureOutputStream(new BufferedOutputStream(new FileOutputStream(this.files[0])));
        } catch (FileNotFoundException e1) {
            getErrorManager().error("Error opening log file", e1, 4);
        }
        setOutputStream(this.output);
    }

    private String parseFileName(int gen) {
        int cur = 0;
        boolean hasUniqueID = false;
        boolean hasGeneration = false;
        String tempPath = System.getProperty("java.io.tmpdir");
        boolean tempPathHasSepEnd = tempPath == null ? false : tempPath.endsWith(File.separator);
        String homePath = System.getProperty("user.home");
        boolean homePathHasSepEnd = homePath == null ? false : homePath.endsWith(File.separator);
        StringBuilder sb = new StringBuilder();
        this.pattern = this.pattern.replace('/', File.separatorChar);
        char[] value = this.pattern.toCharArray();
        while (true) {
            int next = this.pattern.indexOf(37, cur);
            if (next >= 0) {
                int next2 = next + 1;
                if (next2 < this.pattern.length()) {
                    switch (value[next2]) {
                        case Opcodes.OP_FILLED_NEW_ARRAY_RANGE:
                            sb.append(value, cur, (next2 - cur) - 1).append('%');
                            break;
                        case Opcodes.OP_SPUT:
                            sb.append(value, cur, (next2 - cur) - 1).append(gen);
                            hasGeneration = true;
                            break;
                        case Opcodes.OP_SPUT_WIDE:
                            sb.append(value, cur, (next2 - cur) - 1).append(homePath);
                            if (!homePathHasSepEnd) {
                                sb.append(File.separator);
                            }
                            break;
                        case Opcodes.OP_INVOKE_VIRTUAL_RANGE:
                            sb.append(value, cur, (next2 - cur) - 1).append(tempPath);
                            if (!tempPathHasSepEnd) {
                                sb.append(File.separator);
                            }
                            break;
                        case Opcodes.OP_INVOKE_SUPER_RANGE:
                            sb.append(value, cur, (next2 - cur) - 1).append(this.uniqueID);
                            hasUniqueID = true;
                            break;
                        default:
                            sb.append(value, cur, next2 - cur);
                            break;
                    }
                    cur = next2 + 1;
                }
            } else {
                sb.append(value, cur, value.length - cur);
                if (!hasGeneration && this.count > 1) {
                    sb.append(".").append(gen);
                }
                if (!hasUniqueID && this.uniqueID > 0) {
                    sb.append(".").append(this.uniqueID);
                }
                return sb.toString();
            }
        }
    }

    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String property = this.manager.getProperty(key);
        if (property != null) {
            boolean result = defaultValue;
            if ("true".equalsIgnoreCase(property)) {
                result = true;
            } else if ("false".equalsIgnoreCase(property)) {
                result = false;
            }
            return result;
        }
        return defaultValue;
    }

    private String getStringProperty(String key, String defaultValue) {
        String property = this.manager.getProperty(key);
        return property == null ? defaultValue : property;
    }

    private int getIntProperty(String key, int defaultValue) {
        String property = this.manager.getProperty(key);
        if (property == null) {
            return defaultValue;
        }
        try {
            int result = Integer.parseInt(property);
            return result;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public FileHandler(String pattern) throws IOException {
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("Pattern cannot be empty");
        }
        init(pattern, null, 0, 1);
    }

    public FileHandler(String pattern, boolean append) throws IOException {
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("Pattern cannot be empty");
        }
        init(pattern, Boolean.valueOf(append), 0, 1);
    }

    public FileHandler(String pattern, int limit, int count) throws IOException {
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("Pattern cannot be empty");
        }
        if (limit < 0 || count < 1) {
            throw new IllegalArgumentException("limit < 0 || count < 1");
        }
        init(pattern, null, Integer.valueOf(limit), Integer.valueOf(count));
    }

    public FileHandler(String pattern, int limit, int count, boolean append) throws IOException {
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("Pattern cannot be empty");
        }
        if (limit < 0 || count < 1) {
            throw new IllegalArgumentException("limit < 0 || count < 1");
        }
        init(pattern, Boolean.valueOf(append), Integer.valueOf(limit), Integer.valueOf(count));
    }

    @Override
    public void close() {
        super.close();
        allLocks.remove(this.fileName);
        try {
            FileChannel channel = this.lock.channel();
            this.lock.release();
            channel.close();
            File file = new File(this.fileName + LCK_EXT);
            file.delete();
        } catch (IOException e) {
        }
    }

    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        flush();
        if (this.limit > 0 && this.output.getLength() >= this.limit) {
            findNextGeneration();
        }
    }

    static class MeasureOutputStream extends OutputStream {
        long length;
        OutputStream wrapped;

        public MeasureOutputStream(OutputStream stream, long currentLength) {
            this.wrapped = stream;
            this.length = currentLength;
        }

        public MeasureOutputStream(OutputStream stream) {
            this(stream, 0L);
        }

        @Override
        public void write(int oneByte) throws IOException {
            this.wrapped.write(oneByte);
            this.length++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.wrapped.write(b, off, len);
            this.length += (long) len;
        }

        @Override
        public void close() throws IOException {
            this.wrapped.close();
        }

        @Override
        public void flush() throws IOException {
            this.wrapped.flush();
        }

        public long getLength() {
            return this.length;
        }

        public void setLength(long newLength) {
            this.length = newLength;
        }
    }
}
