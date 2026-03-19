package java.io;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import sun.misc.Unsafe;

public class File implements Serializable, Comparable<File> {
    static final boolean $assertionsDisabled = false;
    private static final long PATH_OFFSET;
    private static final long PREFIX_LENGTH_OFFSET;
    private static final Unsafe UNSAFE;
    private static final long serialVersionUID = 301077366599181567L;
    private volatile transient Path filePath;
    private final String path;
    private final transient int prefixLength;
    private transient PathStatus status = null;
    private static final FileSystem fs = DefaultFileSystem.getFileSystem();
    public static final char separatorChar = fs.getSeparator();
    public static final String separator = "" + separatorChar;
    public static final char pathSeparatorChar = fs.getPathSeparator();
    public static final String pathSeparator = "" + pathSeparatorChar;

    private enum PathStatus {
        INVALID,
        CHECKED
    }

    static {
        try {
            Unsafe unsafe = Unsafe.getUnsafe();
            PATH_OFFSET = unsafe.objectFieldOffset(File.class.getDeclaredField("path"));
            PREFIX_LENGTH_OFFSET = unsafe.objectFieldOffset(File.class.getDeclaredField("prefixLength"));
            UNSAFE = unsafe;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    final boolean isInvalid() {
        if (this.status == null) {
            this.status = this.path.indexOf(0) < 0 ? PathStatus.CHECKED : PathStatus.INVALID;
        }
        if (this.status == PathStatus.INVALID) {
            return true;
        }
        return $assertionsDisabled;
    }

    int getPrefixLength() {
        return this.prefixLength;
    }

    private File(String str, int i) {
        this.path = str;
        this.prefixLength = i;
    }

    private File(String str, File file) {
        this.path = fs.resolve(file.path, str);
        this.prefixLength = file.prefixLength;
    }

    public File(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.path = fs.normalize(str);
        this.prefixLength = fs.prefixLength(this.path);
    }

    public File(String str, String str2) {
        if (str2 == null) {
            throw new NullPointerException();
        }
        if (str != null && !str.isEmpty()) {
            this.path = fs.resolve(fs.normalize(str), fs.normalize(str2));
        } else {
            this.path = fs.normalize(str2);
        }
        this.prefixLength = fs.prefixLength(this.path);
    }

    public File(File file, String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        if (file != null) {
            if (file.path.equals("")) {
                this.path = fs.resolve(fs.getDefaultParent(), fs.normalize(str));
            } else {
                this.path = fs.resolve(file.path, fs.normalize(str));
            }
        } else {
            this.path = fs.normalize(str);
        }
        this.prefixLength = fs.prefixLength(this.path);
    }

    public File(URI uri) {
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }
        if (uri.isOpaque()) {
            throw new IllegalArgumentException("URI is not hierarchical");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("file")) {
            throw new IllegalArgumentException("URI scheme is not \"file\"");
        }
        if (uri.getAuthority() != null) {
            throw new IllegalArgumentException("URI has an authority component");
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("URI has a fragment component");
        }
        if (uri.getQuery() != null) {
            throw new IllegalArgumentException("URI has a query component");
        }
        String path = uri.getPath();
        if (path.equals("")) {
            throw new IllegalArgumentException("URI path component is empty");
        }
        String strFromURIPath = fs.fromURIPath(path);
        this.path = fs.normalize(separatorChar != '/' ? strFromURIPath.replace('/', separatorChar) : strFromURIPath);
        this.prefixLength = fs.prefixLength(this.path);
    }

    public String getName() {
        int iLastIndexOf = this.path.lastIndexOf(separatorChar);
        return iLastIndexOf < this.prefixLength ? this.path.substring(this.prefixLength) : this.path.substring(iLastIndexOf + 1);
    }

    public String getParent() {
        int iLastIndexOf = this.path.lastIndexOf(separatorChar);
        if (iLastIndexOf < this.prefixLength) {
            if (this.prefixLength > 0 && this.path.length() > this.prefixLength) {
                return this.path.substring(0, this.prefixLength);
            }
            return null;
        }
        return this.path.substring(0, iLastIndexOf);
    }

    public File getParentFile() {
        String parent = getParent();
        if (parent == null) {
            return null;
        }
        return new File(parent, this.prefixLength);
    }

    public String getPath() {
        return this.path;
    }

    public boolean isAbsolute() {
        return fs.isAbsolute(this);
    }

    public String getAbsolutePath() {
        return fs.resolve(this);
    }

    public File getAbsoluteFile() {
        String absolutePath = getAbsolutePath();
        return new File(absolutePath, fs.prefixLength(absolutePath));
    }

    public String getCanonicalPath() throws IOException {
        if (isInvalid()) {
            throw new IOException("Invalid file path");
        }
        return fs.canonicalize(fs.resolve(this));
    }

    public File getCanonicalFile() throws IOException {
        String canonicalPath = getCanonicalPath();
        return new File(canonicalPath, fs.prefixLength(canonicalPath));
    }

    private static String slashify(String str, boolean z) {
        if (separatorChar != '/') {
            str = str.replace(separatorChar, '/');
        }
        if (!str.startsWith("/")) {
            str = "/" + str;
        }
        if (!str.endsWith("/") && z) {
            return str + "/";
        }
        return str;
    }

    @Deprecated
    public URL toURL() throws MalformedURLException {
        if (isInvalid()) {
            throw new MalformedURLException("Invalid file path");
        }
        return new URL("file", "", slashify(getAbsolutePath(), getAbsoluteFile().isDirectory()));
    }

    public URI toURI() {
        try {
            File absoluteFile = getAbsoluteFile();
            String strSlashify = slashify(absoluteFile.getPath(), absoluteFile.isDirectory());
            if (strSlashify.startsWith("//")) {
                strSlashify = "//" + strSlashify;
            }
            return new URI("file", null, strSlashify, null);
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
    }

    public boolean canRead() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkRead(this.path);
        }
        if (isInvalid()) {
            return $assertionsDisabled;
        }
        return fs.checkAccess(this, 4);
    }

    public boolean canWrite() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkWrite(this.path);
        }
        if (isInvalid()) {
            return $assertionsDisabled;
        }
        return fs.checkAccess(this, 2);
    }

    public boolean exists() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkRead(this.path);
        }
        if (isInvalid()) {
            return $assertionsDisabled;
        }
        return fs.checkAccess(this, 8);
    }

    public boolean isDirectory() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkRead(this.path);
        }
        if (isInvalid() || (fs.getBooleanAttributes(this) & 4) == 0) {
            return $assertionsDisabled;
        }
        return true;
    }

    public boolean isFile() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkRead(this.path);
        }
        if (isInvalid() || (fs.getBooleanAttributes(this) & 2) == 0) {
            return $assertionsDisabled;
        }
        return true;
    }

    public boolean isHidden() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkRead(this.path);
        }
        if (isInvalid() || (fs.getBooleanAttributes(this) & 8) == 0) {
            return $assertionsDisabled;
        }
        return true;
    }

    public long lastModified() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkRead(this.path);
        }
        if (isInvalid()) {
            return 0L;
        }
        return fs.getLastModifiedTime(this);
    }

    public long length() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkRead(this.path);
        }
        if (isInvalid()) {
            return 0L;
        }
        return fs.getLength(this);
    }

    public boolean createNewFile() throws IOException {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkWrite(this.path);
        }
        if (isInvalid()) {
            throw new IOException("Invalid file path");
        }
        return fs.createFileExclusively(this.path);
    }

    public boolean delete() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkDelete(this.path);
        }
        if (isInvalid()) {
            return $assertionsDisabled;
        }
        return fs.delete(this);
    }

    public void deleteOnExit() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkDelete(this.path);
        }
        if (isInvalid()) {
            return;
        }
        DeleteOnExitHook.add(this.path);
    }

    public String[] list() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkRead(this.path);
        }
        if (isInvalid()) {
            return null;
        }
        return fs.list(this);
    }

    public String[] list(FilenameFilter filenameFilter) {
        String[] list = list();
        if (list == null || filenameFilter == null) {
            return list;
        }
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < list.length; i++) {
            if (filenameFilter.accept(this, list[i])) {
                arrayList.add(list[i]);
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    public File[] listFiles() {
        String[] list = list();
        if (list == null) {
            return null;
        }
        int length = list.length;
        File[] fileArr = new File[length];
        for (int i = 0; i < length; i++) {
            fileArr[i] = new File(list[i], this);
        }
        return fileArr;
    }

    public File[] listFiles(FilenameFilter filenameFilter) {
        String[] list = list();
        if (list == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        for (String str : list) {
            if (filenameFilter == null || filenameFilter.accept(this, str)) {
                arrayList.add(new File(str, this));
            }
        }
        return (File[]) arrayList.toArray(new File[arrayList.size()]);
    }

    public File[] listFiles(FileFilter fileFilter) {
        String[] list = list();
        if (list == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        for (String str : list) {
            File file = new File(str, this);
            if (fileFilter == null || fileFilter.accept(file)) {
                arrayList.add(file);
            }
        }
        return (File[]) arrayList.toArray(new File[arrayList.size()]);
    }

    public boolean mkdir() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkWrite(this.path);
        }
        if (isInvalid()) {
            return $assertionsDisabled;
        }
        return fs.createDirectory(this);
    }

    public boolean mkdirs() {
        if (exists()) {
            return $assertionsDisabled;
        }
        if (mkdir()) {
            return true;
        }
        try {
            File canonicalFile = getCanonicalFile();
            File parentFile = canonicalFile.getParentFile();
            if (parentFile == null) {
                return $assertionsDisabled;
            }
            if ((parentFile.mkdirs() || parentFile.exists()) && canonicalFile.mkdir()) {
                return true;
            }
            return $assertionsDisabled;
        } catch (IOException e) {
            return $assertionsDisabled;
        }
    }

    public boolean renameTo(File file) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkWrite(this.path);
            securityManager.checkWrite(file.path);
        }
        if (file == null) {
            throw new NullPointerException();
        }
        if (isInvalid() || file.isInvalid()) {
            return $assertionsDisabled;
        }
        return fs.rename(this, file);
    }

    public boolean setLastModified(long j) {
        if (j < 0) {
            throw new IllegalArgumentException("Negative time");
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkWrite(this.path);
        }
        if (isInvalid()) {
            return $assertionsDisabled;
        }
        return fs.setLastModifiedTime(this, j);
    }

    public boolean setReadOnly() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkWrite(this.path);
        }
        if (isInvalid()) {
            return $assertionsDisabled;
        }
        return fs.setReadOnly(this);
    }

    public boolean setWritable(boolean z, boolean z2) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkWrite(this.path);
        }
        if (isInvalid()) {
            return $assertionsDisabled;
        }
        return fs.setPermission(this, 2, z, z2);
    }

    public boolean setWritable(boolean z) {
        return setWritable(z, true);
    }

    public boolean setReadable(boolean z, boolean z2) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkWrite(this.path);
        }
        if (isInvalid()) {
            return $assertionsDisabled;
        }
        return fs.setPermission(this, 4, z, z2);
    }

    public boolean setReadable(boolean z) {
        return setReadable(z, true);
    }

    public boolean setExecutable(boolean z, boolean z2) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkWrite(this.path);
        }
        if (isInvalid()) {
            return $assertionsDisabled;
        }
        return fs.setPermission(this, 1, z, z2);
    }

    public boolean setExecutable(boolean z) {
        return setExecutable(z, true);
    }

    public boolean canExecute() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkExec(this.path);
        }
        if (isInvalid()) {
            return $assertionsDisabled;
        }
        return fs.checkAccess(this, 1);
    }

    public static File[] listRoots() {
        return fs.listRoots();
    }

    public long getTotalSpace() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new RuntimePermission("getFileSystemAttributes"));
            securityManager.checkRead(this.path);
        }
        if (isInvalid()) {
            return 0L;
        }
        return fs.getSpace(this, 0);
    }

    public long getFreeSpace() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new RuntimePermission("getFileSystemAttributes"));
            securityManager.checkRead(this.path);
        }
        if (isInvalid()) {
            return 0L;
        }
        return fs.getSpace(this, 1);
    }

    public long getUsableSpace() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new RuntimePermission("getFileSystemAttributes"));
            securityManager.checkRead(this.path);
        }
        if (isInvalid()) {
            return 0L;
        }
        return fs.getSpace(this, 2);
    }

    private static class TempDirectory {
        private TempDirectory() {
        }

        static File generateFile(String str, String str2, File file) throws IOException {
            long jAbs;
            long jRandomLongInternal = Math.randomLongInternal();
            if (jRandomLongInternal == Long.MIN_VALUE) {
                jAbs = 0;
            } else {
                jAbs = Math.abs(jRandomLongInternal);
            }
            String str3 = str + Long.toString(jAbs) + str2;
            File file2 = new File(file, str3);
            if (!str3.equals(file2.getName()) || file2.isInvalid()) {
                if (System.getSecurityManager() != null) {
                    throw new IOException("Unable to create temporary file");
                }
                throw new IOException("Unable to create temporary file, " + ((Object) file2));
            }
            return file2;
        }
    }

    public static File createTempFile(String str, String str2, File file) throws IOException {
        File fileGenerateFile;
        if (str.length() < 3) {
            throw new IllegalArgumentException("Prefix string too short");
        }
        if (str2 == null) {
            str2 = ".tmp";
        }
        if (file == null) {
            file = new File(System.getProperty("java.io.tmpdir", "."));
        }
        do {
            fileGenerateFile = TempDirectory.generateFile(str, str2, file);
        } while ((fs.getBooleanAttributes(fileGenerateFile) & 1) != 0);
        if (!fs.createFileExclusively(fileGenerateFile.getPath())) {
            throw new IOException("Unable to create temporary file");
        }
        return fileGenerateFile;
    }

    public static File createTempFile(String str, String str2) throws IOException {
        return createTempFile(str, str2, null);
    }

    @Override
    public int compareTo(File file) {
        return fs.compare(this, file);
    }

    public boolean equals(Object obj) {
        if (obj != null && (obj instanceof File) && compareTo((File) obj) == 0) {
            return true;
        }
        return $assertionsDisabled;
    }

    public int hashCode() {
        return fs.hashCode(this);
    }

    public String toString() {
        return getPath();
    }

    private synchronized void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeChar(separatorChar);
    }

    private synchronized void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        String strReplace = (String) objectInputStream.readFields().get("path", (Object) null);
        char c = objectInputStream.readChar();
        if (c != separatorChar) {
            strReplace = strReplace.replace(c, separatorChar);
        }
        String strNormalize = fs.normalize(strReplace);
        UNSAFE.putObject(this, PATH_OFFSET, strNormalize);
        UNSAFE.putIntVolatile(this, PREFIX_LENGTH_OFFSET, fs.prefixLength(strNormalize));
    }

    public Path toPath() {
        Path path = this.filePath;
        if (path == null) {
            synchronized (this) {
                path = this.filePath;
                if (path == null) {
                    path = FileSystems.getDefault().getPath(this.path, new String[0]);
                    this.filePath = path;
                }
            }
        }
        return path;
    }
}
