package java.util.logging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;

public class FileHandler extends StreamHandler {
    private static final int MAX_LOCKS = 100;
    private static final Set<String> locks = new HashSet();
    private boolean append;
    private int count;
    private File[] files;
    private int limit;
    private FileChannel lockFileChannel;
    private String lockFileName;
    private MeteredStream meter;
    private String pattern;

    private class MeteredStream extends OutputStream {
        final OutputStream out;
        int written;

        MeteredStream(OutputStream outputStream, int i) {
            this.out = outputStream;
            this.written = i;
        }

        @Override
        public void write(int i) throws IOException {
            this.out.write(i);
            this.written++;
        }

        @Override
        public void write(byte[] bArr) throws IOException {
            this.out.write(bArr);
            this.written += bArr.length;
        }

        @Override
        public void write(byte[] bArr, int i, int i2) throws IOException {
            this.out.write(bArr, i, i2);
            this.written += i2;
        }

        @Override
        public void flush() throws IOException {
            this.out.flush();
        }

        @Override
        public void close() throws IOException {
            this.out.close();
        }
    }

    private void open(File file, boolean z) throws IOException {
        int length;
        if (z) {
            length = (int) file.length();
        } else {
            length = 0;
        }
        this.meter = new MeteredStream(new BufferedOutputStream(new FileOutputStream(file.toString(), z)), length);
        setOutputStream(this.meter);
    }

    private void configure() {
        LogManager logManager = LogManager.getLogManager();
        String name = getClass().getName();
        this.pattern = logManager.getStringProperty(name + ".pattern", "%h/java%u.log");
        this.limit = logManager.getIntProperty(name + ".limit", 0);
        if (this.limit < 0) {
            this.limit = 0;
        }
        this.count = logManager.getIntProperty(name + ".count", 1);
        if (this.count <= 0) {
            this.count = 1;
        }
        this.append = logManager.getBooleanProperty(name + ".append", false);
        setLevel(logManager.getLevelProperty(name + ".level", Level.ALL));
        setFilter(logManager.getFilterProperty(name + ".filter", null));
        setFormatter(logManager.getFormatterProperty(name + ".formatter", new XMLFormatter()));
        try {
            setEncoding(logManager.getStringProperty(name + ".encoding", null));
        } catch (Exception e) {
            try {
                setEncoding(null);
            } catch (Exception e2) {
            }
        }
    }

    public FileHandler() throws IOException, SecurityException {
        checkPermission();
        configure();
        openFiles();
    }

    public FileHandler(String str) throws IOException, SecurityException {
        if (str.length() < 1) {
            throw new IllegalArgumentException();
        }
        checkPermission();
        configure();
        this.pattern = str;
        this.limit = 0;
        this.count = 1;
        openFiles();
    }

    public FileHandler(String str, boolean z) throws IOException, SecurityException {
        if (str.length() < 1) {
            throw new IllegalArgumentException();
        }
        checkPermission();
        configure();
        this.pattern = str;
        this.limit = 0;
        this.count = 1;
        this.append = z;
        openFiles();
    }

    public FileHandler(String str, int i, int i2) throws IOException, SecurityException {
        if (i < 0 || i2 < 1 || str.length() < 1) {
            throw new IllegalArgumentException();
        }
        checkPermission();
        configure();
        this.pattern = str;
        this.limit = i;
        this.count = i2;
        openFiles();
    }

    public FileHandler(String str, int i, int i2, boolean z) throws IOException, SecurityException {
        if (i < 0 || i2 < 1 || str.length() < 1) {
            throw new IllegalArgumentException();
        }
        checkPermission();
        configure();
        this.pattern = str;
        this.limit = i;
        this.count = i2;
        this.append = z;
        openFiles();
    }

    private boolean isParentWritable(Path path) {
        Path parent = path.getParent();
        if (parent == null) {
            parent = path.toAbsolutePath().getParent();
        }
        return parent != null && Files.isWritable(parent);
    }

    private void openFiles() throws IOException {
        boolean z;
        LogManager.getLogManager().checkPermission();
        if (this.count < 1) {
            throw new IllegalArgumentException("file count = " + this.count);
        }
        if (this.limit < 0) {
            this.limit = 0;
        }
        InitializationErrorManager initializationErrorManager = new InitializationErrorManager();
        setErrorManager(initializationErrorManager);
        int i = -1;
        while (true) {
            i++;
            if (i > MAX_LOCKS) {
                throw new IOException("Couldn't get lock for " + this.pattern);
            }
            this.lockFileName = generate(this.pattern, 0, i).toString() + ".lck";
            synchronized (locks) {
                if (!locks.contains(this.lockFileName)) {
                    Path path = Paths.get(this.lockFileName, new String[0]);
                    boolean z2 = false;
                    FileChannel fileChannelOpen = null;
                    int i2 = -1;
                    while (fileChannelOpen == null) {
                        int i3 = i2 + 1;
                        if (i2 >= 1) {
                            break;
                        }
                        try {
                            z2 = true;
                            i2 = i3;
                            fileChannelOpen = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                        } catch (FileAlreadyExistsException e) {
                            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || !isParentWritable(path)) {
                                break;
                            }
                            try {
                                fileChannelOpen = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                            } catch (NoSuchFileException e2) {
                            } catch (IOException e3) {
                            }
                            i2 = i3;
                            if (fileChannelOpen == null) {
                            }
                        }
                    }
                    if (fileChannelOpen == null) {
                        this.lockFileChannel = fileChannelOpen;
                        try {
                            z = this.lockFileChannel.tryLock() != null;
                        } catch (IOException e4) {
                            z = z2;
                        } catch (OverlappingFileLockException e5) {
                            z = false;
                        }
                        if (z) {
                            break;
                        } else {
                            this.lockFileChannel.close();
                        }
                    }
                }
            }
        }
    }

    private File generate(String str, int i, int i2) throws IOException {
        String str2 = "";
        int i3 = 0;
        boolean z = false;
        File file = null;
        boolean z2 = false;
        while (i3 < str.length()) {
            char cCharAt = str.charAt(i3);
            i3++;
            char lowerCase = i3 < str.length() ? Character.toLowerCase(str.charAt(i3)) : (char) 0;
            if (cCharAt == '/') {
                file = file == null ? new File(str2) : new File(file, str2);
                str2 = "";
            } else {
                if (cCharAt == '%') {
                    if (lowerCase == 't') {
                        String property = System.getProperty("java.io.tmpdir");
                        if (property == null) {
                            property = System.getProperty("user.home");
                        }
                        i3++;
                        str2 = "";
                        file = new File(property);
                    } else if (lowerCase == 'h') {
                        file = new File(System.getProperty("user.home"));
                        i3++;
                        str2 = "";
                    } else if (lowerCase == 'g') {
                        str2 = str2 + i;
                        i3++;
                        z2 = true;
                    } else if (lowerCase == 'u') {
                        str2 = str2 + i2;
                        i3++;
                        z = true;
                    } else if (lowerCase == '%') {
                        str2 = str2 + "%";
                        i3++;
                    }
                }
                str2 = str2 + cCharAt;
            }
        }
        if (this.count > 1 && !z2) {
            str2 = str2 + "." + i;
        }
        if (i2 > 0 && !z) {
            str2 = str2 + "." + i2;
        }
        return str2.length() > 0 ? file == null ? new File(str2) : new File(file, str2) : file;
    }

    private synchronized void rotate() {
        Level level = getLevel();
        setLevel(Level.OFF);
        super.close();
        for (int i = this.count - 2; i >= 0; i--) {
            File file = this.files[i];
            File file2 = this.files[i + 1];
            if (file.exists()) {
                if (file2.exists()) {
                    file2.delete();
                }
                file.renameTo(file2);
            }
        }
        try {
            open(this.files[0], false);
        } catch (IOException e) {
            reportError(null, e, 4);
        }
        setLevel(level);
    }

    @Override
    public synchronized void publish(LogRecord logRecord) {
        if (isLoggable(logRecord)) {
            super.publish(logRecord);
            flush();
            if (this.limit > 0 && this.meter.written >= this.limit) {
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        FileHandler.this.rotate();
                        return null;
                    }
                });
            }
        }
    }

    @Override
    public synchronized void close() throws SecurityException {
        super.close();
        if (this.lockFileName == null) {
            return;
        }
        try {
            this.lockFileChannel.close();
        } catch (Exception e) {
        }
        synchronized (locks) {
            locks.remove(this.lockFileName);
        }
        new File(this.lockFileName).delete();
        this.lockFileName = null;
        this.lockFileChannel = null;
    }

    private static class InitializationErrorManager extends ErrorManager {
        Exception lastException;

        private InitializationErrorManager() {
        }

        @Override
        public void error(String str, Exception exc, int i) {
            this.lastException = exc;
        }
    }
}
