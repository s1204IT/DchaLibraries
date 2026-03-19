package sun.nio.fs;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Properties;

abstract class UnixFileStore extends FileStore {
    private static final Object loadLock = new Object();
    private static volatile Properties props;
    private final long dev;
    private final UnixMountEntry entry;
    private final UnixPath file;

    enum FeatureStatus {
        PRESENT,
        NOT_PRESENT,
        UNKNOWN
    }

    abstract UnixMountEntry findMountEntry() throws IOException;

    private static long devFor(UnixPath unixPath) throws IOException {
        try {
            return UnixFileAttributes.get(unixPath, true).dev();
        } catch (UnixException e) {
            e.rethrowAsIOException(unixPath);
            return 0L;
        }
    }

    UnixFileStore(UnixPath unixPath) throws IOException {
        this.file = unixPath;
        this.dev = devFor(unixPath);
        this.entry = findMountEntry();
    }

    UnixFileStore(UnixFileSystem unixFileSystem, UnixMountEntry unixMountEntry) throws IOException {
        this.file = new UnixPath(unixFileSystem, unixMountEntry.dir());
        this.dev = unixMountEntry.dev() == 0 ? devFor(this.file) : unixMountEntry.dev();
        this.entry = unixMountEntry;
    }

    UnixPath file() {
        return this.file;
    }

    long dev() {
        return this.dev;
    }

    UnixMountEntry entry() {
        return this.entry;
    }

    @Override
    public String name() {
        return this.entry.name();
    }

    @Override
    public String type() {
        return this.entry.fstype();
    }

    @Override
    public boolean isReadOnly() {
        return this.entry.isReadOnly();
    }

    private UnixFileStoreAttributes readAttributes() throws IOException {
        try {
            return UnixFileStoreAttributes.get(this.file);
        } catch (UnixException e) {
            e.rethrowAsIOException(this.file);
            return null;
        }
    }

    @Override
    public long getTotalSpace() throws IOException {
        UnixFileStoreAttributes attributes = readAttributes();
        return attributes.blockSize() * attributes.totalBlocks();
    }

    @Override
    public long getUsableSpace() throws IOException {
        UnixFileStoreAttributes attributes = readAttributes();
        return attributes.blockSize() * attributes.availableBlocks();
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        UnixFileStoreAttributes attributes = readAttributes();
        return attributes.blockSize() * attributes.freeBlocks();
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> cls) {
        if (cls == null) {
            throw new NullPointerException();
        }
        return (V) null;
    }

    @Override
    public Object getAttribute(String str) throws IOException {
        if (str.equals("totalSpace")) {
            return Long.valueOf(getTotalSpace());
        }
        if (str.equals("usableSpace")) {
            return Long.valueOf(getUsableSpace());
        }
        if (str.equals("unallocatedSpace")) {
            return Long.valueOf(getUnallocatedSpace());
        }
        throw new UnsupportedOperationException("'" + str + "' not recognized");
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> cls) {
        if (cls == null) {
            throw new NullPointerException();
        }
        if (cls == BasicFileAttributeView.class) {
            return true;
        }
        return (cls == PosixFileAttributeView.class || cls == FileOwnerAttributeView.class) && checkIfFeaturePresent("posix") != FeatureStatus.NOT_PRESENT;
    }

    @Override
    public boolean supportsFileAttributeView(String str) {
        if (str.equals("basic") || str.equals("unix")) {
            return true;
        }
        if (str.equals("posix")) {
            return supportsFileAttributeView(PosixFileAttributeView.class);
        }
        if (str.equals("owner")) {
            return supportsFileAttributeView(FileOwnerAttributeView.class);
        }
        return false;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof UnixFileStore)) {
            return false;
        }
        UnixFileStore unixFileStore = (UnixFileStore) obj;
        return this.dev == unixFileStore.dev && Arrays.equals(this.entry.dir(), unixFileStore.entry.dir());
    }

    public int hashCode() {
        return ((int) (this.dev ^ (this.dev >>> 32))) ^ Arrays.hashCode(this.entry.dir());
    }

    public String toString() {
        return Util.toString(this.entry.dir()) + " (" + this.entry.name() + ")";
    }

    FeatureStatus checkIfFeaturePresent(String str) {
        if (props == null) {
            synchronized (loadLock) {
                if (props == null) {
                    props = (Properties) AccessController.doPrivileged(new PrivilegedAction<Properties>() {
                        @Override
                        public Properties run() {
                            return UnixFileStore.loadProperties();
                        }
                    });
                }
            }
        }
        String property = props.getProperty(type());
        if (property != null) {
            for (String str2 : property.split("\\s")) {
                String lowerCase = str2.trim().toLowerCase();
                if (lowerCase.equals(str)) {
                    return FeatureStatus.PRESENT;
                }
                if (lowerCase.startsWith("no") && lowerCase.substring(2).equals(str)) {
                    return FeatureStatus.NOT_PRESENT;
                }
            }
        }
        return FeatureStatus.UNKNOWN;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try {
            SeekableByteChannel seekableByteChannelNewByteChannel = Files.newByteChannel(Paths.get(System.getProperty("java.home") + "/lib/fstypes.properties", new String[0]), new OpenOption[0]);
            Throwable th = null;
            try {
                properties.load(Channels.newReader(seekableByteChannelNewByteChannel, "UTF-8"));
                if (seekableByteChannelNewByteChannel != null) {
                    seekableByteChannelNewByteChannel.close();
                }
            } finally {
            }
        } catch (IOException e) {
        }
        return properties;
    }
}
