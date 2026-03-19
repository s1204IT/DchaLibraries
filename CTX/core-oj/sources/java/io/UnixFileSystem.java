package java.io;

import dalvik.system.BlockGuard;
import java.security.AccessController;
import sun.security.action.GetPropertyAction;

class UnixFileSystem extends FileSystem {
    private ExpiringCache cache = new ExpiringCache();
    private ExpiringCache javaHomePrefixCache = new ExpiringCache();
    private final char slash = ((String) AccessController.doPrivileged(new GetPropertyAction("file.separator"))).charAt(0);
    private final char colon = ((String) AccessController.doPrivileged(new GetPropertyAction("path.separator"))).charAt(0);
    private final String javaHome = (String) AccessController.doPrivileged(new GetPropertyAction("java.home"));

    private native String canonicalize0(String str) throws IOException;

    private native boolean checkAccess0(File file, int i);

    private native boolean createDirectory0(File file);

    private native boolean createFileExclusively0(String str) throws IOException;

    private native boolean delete0(File file);

    private native int getBooleanAttributes0(String str);

    private native long getLastModifiedTime0(File file);

    private native long getLength0(File file);

    private native long getSpace0(File file, int i);

    private static native void initIDs();

    private native String[] list0(File file);

    private native boolean rename0(File file, File file2);

    private native boolean setLastModifiedTime0(File file, long j);

    private native boolean setPermission0(File file, int i, boolean z, boolean z2);

    private native boolean setReadOnly0(File file);

    @Override
    public char getSeparator() {
        return this.slash;
    }

    @Override
    public char getPathSeparator() {
        return this.colon;
    }

    @Override
    public String normalize(String str) {
        int length = str.length();
        char[] charArray = str.toCharArray();
        int i = 0;
        char c = 0;
        int i2 = 0;
        while (i < length) {
            char c2 = charArray[i];
            if (c2 != '/' || c != '/') {
                charArray[i2] = c2;
                i2++;
            }
            i++;
            c = c2;
        }
        if (c == '/' && length > 1) {
            i2--;
        }
        return i2 != length ? new String(charArray, 0, i2) : str;
    }

    @Override
    public int prefixLength(String str) {
        return (str.length() != 0 && str.charAt(0) == '/') ? 1 : 0;
    }

    @Override
    public String resolve(String str, String str2) {
        if (str2.isEmpty() || str2.equals("/")) {
            return str;
        }
        if (str2.charAt(0) == '/') {
            if (str.equals("/")) {
                return str2;
            }
            return str + str2;
        }
        if (str.equals("/")) {
            return str + str2;
        }
        return str + '/' + str2;
    }

    @Override
    public String getDefaultParent() {
        return "/";
    }

    @Override
    public String fromURIPath(String str) {
        if (str.endsWith("/") && str.length() > 1) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }

    @Override
    public boolean isAbsolute(File file) {
        return file.getPrefixLength() != 0;
    }

    @Override
    public String resolve(File file) {
        return isAbsolute(file) ? file.getPath() : resolve(System.getProperty("user.dir"), file.getPath());
    }

    @Override
    public String canonicalize(String str) throws IOException {
        String strParentOrNull;
        String str2;
        if (!useCanonCaches) {
            return canonicalize0(str);
        }
        String strCanonicalize0 = this.cache.get(str);
        if (strCanonicalize0 == null) {
            String strParentOrNull2 = null;
            if (useCanonPrefixCache && (strParentOrNull2 = parentOrNull(str)) != null && (str2 = this.javaHomePrefixCache.get(strParentOrNull2)) != null) {
                String strSubstring = str.substring(1 + strParentOrNull2.length());
                String str3 = str2 + this.slash + strSubstring;
                this.cache.put(strParentOrNull2 + this.slash + strSubstring, str3);
                strCanonicalize0 = str3;
            }
            if (strCanonicalize0 == null) {
                BlockGuard.getThreadPolicy().onReadFromDisk();
                strCanonicalize0 = canonicalize0(str);
                this.cache.put(str, strCanonicalize0);
                if (useCanonPrefixCache && strParentOrNull2 != null && strParentOrNull2.startsWith(this.javaHome) && (strParentOrNull = parentOrNull(strCanonicalize0)) != null && strParentOrNull.equals(strParentOrNull2)) {
                    File file = new File(strCanonicalize0);
                    if (file.exists() && !file.isDirectory()) {
                        this.javaHomePrefixCache.put(strParentOrNull2, strParentOrNull);
                    }
                }
            }
        }
        return strCanonicalize0;
    }

    static String parentOrNull(String str) {
        if (str == null) {
            return null;
        }
        char c = File.separatorChar;
        int length = str.length() - 1;
        int i = 0;
        int i2 = 0;
        for (int i3 = length; i3 > 0; i3--) {
            char cCharAt = str.charAt(i3);
            if (cCharAt == '.') {
                i++;
                if (i >= 2) {
                    return null;
                }
            } else {
                if (cCharAt == c) {
                    if ((i == 1 && i2 == 0) || i3 == 0 || i3 >= length - 1 || str.charAt(i3 - 1) == c) {
                        return null;
                    }
                    return str.substring(0, i3);
                }
                i2++;
                i = 0;
            }
        }
        return null;
    }

    @Override
    public int getBooleanAttributes(File file) {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        int booleanAttributes0 = getBooleanAttributes0(file.getPath());
        String name = file.getName();
        return booleanAttributes0 | (name.length() > 0 && name.charAt(0) == '.' ? 8 : 0);
    }

    @Override
    public boolean checkAccess(File file, int i) {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return checkAccess0(file, i);
    }

    @Override
    public long getLastModifiedTime(File file) {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return getLastModifiedTime0(file);
    }

    @Override
    public long getLength(File file) {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return getLength0(file);
    }

    @Override
    public boolean setPermission(File file, int i, boolean z, boolean z2) {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return setPermission0(file, i, z, z2);
    }

    @Override
    public boolean createFileExclusively(String str) throws IOException {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return createFileExclusively0(str);
    }

    @Override
    public boolean delete(File file) {
        this.cache.clear();
        this.javaHomePrefixCache.clear();
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return delete0(file);
    }

    @Override
    public String[] list(File file) {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return list0(file);
    }

    @Override
    public boolean createDirectory(File file) {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return createDirectory0(file);
    }

    @Override
    public boolean rename(File file, File file2) {
        this.cache.clear();
        this.javaHomePrefixCache.clear();
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return rename0(file, file2);
    }

    @Override
    public boolean setLastModifiedTime(File file, long j) {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return setLastModifiedTime0(file, j);
    }

    @Override
    public boolean setReadOnly(File file) {
        BlockGuard.getThreadPolicy().onWriteToDisk();
        return setReadOnly0(file);
    }

    @Override
    public File[] listRoots() {
        try {
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkRead("/");
            }
            return new File[]{new File("/")};
        } catch (SecurityException e) {
            return new File[0];
        }
    }

    @Override
    public long getSpace(File file, int i) {
        BlockGuard.getThreadPolicy().onReadFromDisk();
        return getSpace0(file, i);
    }

    @Override
    public int compare(File file, File file2) {
        return file.getPath().compareTo(file2.getPath());
    }

    @Override
    public int hashCode(File file) {
        return file.getPath().hashCode() ^ 1234321;
    }

    static {
        initIDs();
    }
}
