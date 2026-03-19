package java.util.prefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import sun.util.locale.BaseLocale;
import sun.util.logging.PlatformLogger;

public class FileSystemPreferences extends AbstractPreferences {
    private static final int EACCES = 13;
    private static final int EAGAIN = 11;
    private static final String[] EMPTY_STRING_ARRAY;
    private static final int ERROR_CODE = 1;
    private static int INIT_SLEEP_TIME = 0;
    private static final int LOCK_HANDLE = 0;
    private static int MAX_ATTEMPTS = 0;
    private static final int USER_READ_WRITE = 384;
    private static final int USER_RWX = 448;
    private static final int USER_RWX_ALL_RX = 493;
    private static final int USER_RW_ALL_READ = 420;
    private static boolean isSystemRootWritable;
    private static boolean isUserRootWritable;
    static File systemLockFile;
    static Preferences systemRoot;
    private static File systemRootDir;
    private static File systemRootModFile;
    private static long systemRootModTime;
    static File userLockFile;
    private static File userRootDir;
    private static File userRootModFile;
    private static long userRootModTime;
    final List<Change> changeLog;
    private final File dir;
    private final boolean isUserNode;
    private long lastSyncTime;
    NodeCreate nodeCreate;
    private Map<String, String> prefsCache;
    private final File prefsFile;
    private final File tmpFile;
    static Preferences userRoot = null;
    private static int userRootLockHandle = 0;
    private static int systemRootLockHandle = 0;
    private static boolean isUserRootModified = false;
    private static boolean isSystemRootModified = false;

    private static native int chmod(String str, int i);

    private static native int[] lockFile0(String str, int i, boolean z);

    private static native int unlockFile0(int i);

    private static PlatformLogger getLogger() {
        return PlatformLogger.getLogger("java.util.prefs");
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                FileSystemPreferences.syncWorld();
            }
        });
        EMPTY_STRING_ARRAY = new String[0];
        INIT_SLEEP_TIME = 50;
        MAX_ATTEMPTS = 5;
    }

    static synchronized Preferences getUserRoot() {
        if (userRoot == null) {
            setupUserRoot();
            userRoot = new FileSystemPreferences(true);
        }
        return userRoot;
    }

    private static void setupUserRoot() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                File unused = FileSystemPreferences.userRootDir = new File(System.getProperty("java.util.prefs.userRoot", System.getProperty("user.home")), ".java/.userPrefs");
                if (!FileSystemPreferences.userRootDir.exists()) {
                    if (FileSystemPreferences.userRootDir.mkdirs()) {
                        try {
                            FileSystemPreferences.chmod(FileSystemPreferences.userRootDir.getCanonicalPath(), FileSystemPreferences.USER_RWX);
                        } catch (IOException e) {
                            FileSystemPreferences.getLogger().warning("Could not change permissions on userRoot directory. ");
                        }
                        FileSystemPreferences.getLogger().info("Created user preferences directory.");
                    } else {
                        FileSystemPreferences.getLogger().warning("Couldn't create user preferences directory. User preferences are unusable.");
                    }
                }
                boolean unused2 = FileSystemPreferences.isUserRootWritable = FileSystemPreferences.userRootDir.canWrite();
                String property = System.getProperty("user.name");
                FileSystemPreferences.userLockFile = new File(FileSystemPreferences.userRootDir, ".user.lock." + property);
                File unused3 = FileSystemPreferences.userRootModFile = new File(FileSystemPreferences.userRootDir, ".userRootModFile." + property);
                if (!FileSystemPreferences.userRootModFile.exists()) {
                    try {
                        FileSystemPreferences.userRootModFile.createNewFile();
                        int iChmod = FileSystemPreferences.chmod(FileSystemPreferences.userRootModFile.getCanonicalPath(), FileSystemPreferences.USER_READ_WRITE);
                        if (iChmod != 0) {
                            FileSystemPreferences.getLogger().warning("Problem creating userRoot mod file. Chmod failed on " + FileSystemPreferences.userRootModFile.getCanonicalPath() + " Unix error code " + iChmod);
                        }
                    } catch (IOException e2) {
                        FileSystemPreferences.getLogger().warning(e2.toString());
                    }
                }
                long unused4 = FileSystemPreferences.userRootModTime = FileSystemPreferences.userRootModFile.lastModified();
                return null;
            }
        });
    }

    static synchronized Preferences getSystemRoot() {
        if (systemRoot == null) {
            setupSystemRoot();
            systemRoot = new FileSystemPreferences(false);
        }
        return systemRoot;
    }

    private static void setupSystemRoot() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                File unused = FileSystemPreferences.systemRootDir = new File(System.getProperty("java.util.prefs.systemRoot", "/etc/.java"), ".systemPrefs");
                if (!FileSystemPreferences.systemRootDir.exists()) {
                    File unused2 = FileSystemPreferences.systemRootDir = new File(System.getProperty("java.home"), ".systemPrefs");
                    if (!FileSystemPreferences.systemRootDir.exists()) {
                        if (FileSystemPreferences.systemRootDir.mkdirs()) {
                            FileSystemPreferences.getLogger().info("Created system preferences directory in java.home.");
                            try {
                                FileSystemPreferences.chmod(FileSystemPreferences.systemRootDir.getCanonicalPath(), FileSystemPreferences.USER_RWX_ALL_RX);
                            } catch (IOException e) {
                            }
                        } else {
                            FileSystemPreferences.getLogger().warning("Could not create system preferences directory. System preferences are unusable.");
                        }
                    }
                }
                boolean unused3 = FileSystemPreferences.isSystemRootWritable = FileSystemPreferences.systemRootDir.canWrite();
                FileSystemPreferences.systemLockFile = new File(FileSystemPreferences.systemRootDir, ".system.lock");
                File unused4 = FileSystemPreferences.systemRootModFile = new File(FileSystemPreferences.systemRootDir, ".systemRootModFile");
                if (!FileSystemPreferences.systemRootModFile.exists() && FileSystemPreferences.isSystemRootWritable) {
                    try {
                        FileSystemPreferences.systemRootModFile.createNewFile();
                        int iChmod = FileSystemPreferences.chmod(FileSystemPreferences.systemRootModFile.getCanonicalPath(), FileSystemPreferences.USER_RW_ALL_READ);
                        if (iChmod != 0) {
                            FileSystemPreferences.getLogger().warning("Chmod failed on " + FileSystemPreferences.systemRootModFile.getCanonicalPath() + " Unix error code " + iChmod);
                        }
                    } catch (IOException e2) {
                        FileSystemPreferences.getLogger().warning(e2.toString());
                    }
                }
                long unused5 = FileSystemPreferences.systemRootModTime = FileSystemPreferences.systemRootModFile.lastModified();
                return null;
            }
        });
    }

    private abstract class Change {
        abstract void replay();

        private Change() {
        }
    }

    private class Put extends Change {
        String key;
        String value;

        Put(String str, String str2) {
            super();
            this.key = str;
            this.value = str2;
        }

        @Override
        void replay() {
            FileSystemPreferences.this.prefsCache.put(this.key, this.value);
        }
    }

    private class Remove extends Change {
        String key;

        Remove(String str) {
            super();
            this.key = str;
        }

        @Override
        void replay() {
            FileSystemPreferences.this.prefsCache.remove(this.key);
        }
    }

    private class NodeCreate extends Change {
        private NodeCreate() {
            super();
        }

        @Override
        void replay() {
        }
    }

    private void replayChanges() {
        int size = this.changeLog.size();
        for (int i = 0; i < size; i++) {
            this.changeLog.get(i).replay();
        }
    }

    private static void syncWorld() {
        Preferences preferences;
        Preferences preferences2;
        synchronized (FileSystemPreferences.class) {
            preferences = userRoot;
            preferences2 = systemRoot;
        }
        if (preferences != null) {
            try {
                preferences.flush();
            } catch (BackingStoreException e) {
                getLogger().warning("Couldn't flush user prefs: " + ((Object) e));
            }
        }
        if (preferences2 != null) {
            try {
                preferences2.flush();
            } catch (BackingStoreException e2) {
                getLogger().warning("Couldn't flush system prefs: " + ((Object) e2));
            }
        }
    }

    private FileSystemPreferences(boolean z) {
        super(null, "");
        this.prefsCache = null;
        this.lastSyncTime = 0L;
        this.changeLog = new ArrayList();
        this.nodeCreate = null;
        this.isUserNode = z;
        this.dir = z ? userRootDir : systemRootDir;
        this.prefsFile = new File(this.dir, "prefs.xml");
        this.tmpFile = new File(this.dir, "prefs.tmp");
    }

    public FileSystemPreferences(String str, File file, boolean z) {
        super(null, "");
        this.prefsCache = null;
        this.lastSyncTime = 0L;
        this.changeLog = new ArrayList();
        this.nodeCreate = null;
        this.isUserNode = z;
        this.dir = new File(str);
        this.prefsFile = new File(this.dir, "prefs.xml");
        this.tmpFile = new File(this.dir, "prefs.tmp");
        this.newNode = !this.dir.exists();
        if (this.newNode) {
            this.prefsCache = new TreeMap();
            this.nodeCreate = new NodeCreate();
            this.changeLog.add(this.nodeCreate);
        }
        if (z) {
            userLockFile = file;
            userRootModFile = new File(file.getParentFile(), file.getName() + ".rootmod");
            return;
        }
        systemLockFile = file;
        systemRootModFile = new File(file.getParentFile(), file.getName() + ".rootmod");
    }

    private FileSystemPreferences(FileSystemPreferences fileSystemPreferences, String str) {
        super(fileSystemPreferences, str);
        this.prefsCache = null;
        this.lastSyncTime = 0L;
        this.changeLog = new ArrayList();
        this.nodeCreate = null;
        this.isUserNode = fileSystemPreferences.isUserNode;
        this.dir = new File(fileSystemPreferences.dir, dirName(str));
        this.prefsFile = new File(this.dir, "prefs.xml");
        this.tmpFile = new File(this.dir, "prefs.tmp");
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                FileSystemPreferences.this.newNode = !FileSystemPreferences.this.dir.exists();
                return null;
            }
        });
        if (this.newNode) {
            this.prefsCache = new TreeMap();
            this.nodeCreate = new NodeCreate();
            this.changeLog.add(this.nodeCreate);
        }
    }

    @Override
    public boolean isUserNode() {
        return this.isUserNode;
    }

    @Override
    protected void putSpi(String str, String str2) {
        initCacheIfNecessary();
        this.changeLog.add(new Put(str, str2));
        this.prefsCache.put(str, str2);
    }

    @Override
    protected String getSpi(String str) {
        initCacheIfNecessary();
        return this.prefsCache.get(str);
    }

    @Override
    protected void removeSpi(String str) {
        initCacheIfNecessary();
        this.changeLog.add(new Remove(str));
        this.prefsCache.remove(str);
    }

    private void initCacheIfNecessary() {
        if (this.prefsCache != null) {
            return;
        }
        try {
            loadCache();
        } catch (Exception e) {
            this.prefsCache = new TreeMap();
        }
    }

    private void loadCache() throws BackingStoreException {
        Exception e;
        long jLastModified;
        TreeMap treeMap = new TreeMap();
        try {
            jLastModified = this.prefsFile.lastModified();
        } catch (Exception e2) {
            e = e2;
            jLastModified = 0;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(this.prefsFile);
            Throwable th = null;
            try {
                XmlSupport.importMap(fileInputStream, treeMap);
            } finally {
                $closeResource(th, fileInputStream);
            }
        } catch (Exception e3) {
            e = e3;
            if (e instanceof InvalidPreferencesFormatException) {
                getLogger().warning("Invalid preferences format in " + this.prefsFile.getPath());
                this.prefsFile.renameTo(new File(this.prefsFile.getParentFile(), "IncorrectFormatPrefs.xml"));
                treeMap = new TreeMap();
            } else if (e instanceof FileNotFoundException) {
                getLogger().warning("Prefs file removed in background " + this.prefsFile.getPath());
            } else {
                getLogger().warning("Exception while reading cache: " + e.getMessage());
                throw new BackingStoreException(e);
            }
        }
        this.prefsCache = treeMap;
        this.lastSyncTime = jLastModified;
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private void writeBackCache() throws BackingStoreException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws BackingStoreException {
                    try {
                        if (!FileSystemPreferences.this.dir.exists() && !FileSystemPreferences.this.dir.mkdirs()) {
                            throw new BackingStoreException(((Object) FileSystemPreferences.this.dir) + " create failed.");
                        }
                        FileOutputStream fileOutputStream = new FileOutputStream(FileSystemPreferences.this.tmpFile);
                        Throwable th = null;
                        try {
                            XmlSupport.exportMap(fileOutputStream, FileSystemPreferences.this.prefsCache);
                            fileOutputStream.close();
                            if (!FileSystemPreferences.this.tmpFile.renameTo(FileSystemPreferences.this.prefsFile)) {
                                throw new BackingStoreException("Can't rename " + ((Object) FileSystemPreferences.this.tmpFile) + " to " + ((Object) FileSystemPreferences.this.prefsFile));
                            }
                            return null;
                        } finally {
                        }
                    } catch (Exception e) {
                        if (e instanceof BackingStoreException) {
                            throw ((BackingStoreException) e);
                        }
                        throw new BackingStoreException(e);
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            throw ((BackingStoreException) e.getException());
        }
    }

    @Override
    protected String[] keysSpi() {
        initCacheIfNecessary();
        return (String[]) this.prefsCache.keySet().toArray(new String[this.prefsCache.size()]);
    }

    @Override
    protected String[] childrenNamesSpi() {
        return (String[]) AccessController.doPrivileged(new PrivilegedAction<String[]>() {
            @Override
            public String[] run() {
                ArrayList arrayList = new ArrayList();
                File[] fileArrListFiles = FileSystemPreferences.this.dir.listFiles();
                if (fileArrListFiles != null) {
                    for (int i = 0; i < fileArrListFiles.length; i++) {
                        if (fileArrListFiles[i].isDirectory()) {
                            arrayList.add(FileSystemPreferences.nodeName(fileArrListFiles[i].getName()));
                        }
                    }
                }
                return (String[]) arrayList.toArray(FileSystemPreferences.EMPTY_STRING_ARRAY);
            }
        });
    }

    @Override
    protected AbstractPreferences childSpi(String str) {
        return new FileSystemPreferences(this, str);
    }

    @Override
    public void removeNode() throws BackingStoreException {
        synchronized ((isUserNode() ? userLockFile : systemLockFile)) {
            if (!lockFile(false)) {
                throw new BackingStoreException("Couldn't get file lock.");
            }
            try {
                super.removeNode();
            } finally {
                unlockFile();
            }
        }
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws BackingStoreException {
                    if (!FileSystemPreferences.this.changeLog.contains(FileSystemPreferences.this.nodeCreate)) {
                        if (!FileSystemPreferences.this.dir.exists()) {
                            return null;
                        }
                        FileSystemPreferences.this.prefsFile.delete();
                        FileSystemPreferences.this.tmpFile.delete();
                        File[] fileArrListFiles = FileSystemPreferences.this.dir.listFiles();
                        if (fileArrListFiles.length != 0) {
                            FileSystemPreferences.getLogger().warning("Found extraneous files when removing node: " + ((Object) Arrays.asList(fileArrListFiles)));
                            for (File file : fileArrListFiles) {
                                file.delete();
                            }
                        }
                        if (FileSystemPreferences.this.dir.delete()) {
                            return null;
                        }
                        throw new BackingStoreException("Couldn't delete dir: " + ((Object) FileSystemPreferences.this.dir));
                    }
                    FileSystemPreferences.this.changeLog.remove(FileSystemPreferences.this.nodeCreate);
                    FileSystemPreferences.this.nodeCreate = null;
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw ((BackingStoreException) e.getException());
        }
    }

    @Override
    public synchronized void sync() throws BackingStoreException {
        boolean z;
        if (isUserNode()) {
            z = false;
        } else {
            z = !isSystemRootWritable;
        }
        synchronized ((isUserNode() ? userLockFile : systemLockFile)) {
            if (!lockFile(z)) {
                throw new BackingStoreException("Couldn't get file lock.");
            }
            final Long l = (Long) AccessController.doPrivileged(new PrivilegedAction<Long>() {
                @Override
                public Long run() {
                    long jLastModified;
                    if (FileSystemPreferences.this.isUserNode()) {
                        jLastModified = FileSystemPreferences.userRootModFile.lastModified();
                        boolean unused = FileSystemPreferences.isUserRootModified = FileSystemPreferences.userRootModTime == jLastModified;
                    } else {
                        jLastModified = FileSystemPreferences.systemRootModFile.lastModified();
                        boolean unused2 = FileSystemPreferences.isSystemRootModified = FileSystemPreferences.systemRootModTime == jLastModified;
                    }
                    return new Long(jLastModified);
                }
            });
            try {
                super.sync();
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        if (FileSystemPreferences.this.isUserNode()) {
                            long unused = FileSystemPreferences.userRootModTime = l.longValue() + 1000;
                            FileSystemPreferences.userRootModFile.setLastModified(FileSystemPreferences.userRootModTime);
                            return null;
                        }
                        long unused2 = FileSystemPreferences.systemRootModTime = l.longValue() + 1000;
                        FileSystemPreferences.systemRootModFile.setLastModified(FileSystemPreferences.systemRootModTime);
                        return null;
                    }
                });
            } finally {
                unlockFile();
            }
        }
    }

    @Override
    protected void syncSpi() throws BackingStoreException {
        syncSpiPrivileged();
    }

    private void syncSpiPrivileged() throws BackingStoreException {
        if (isRemoved()) {
            throw new IllegalStateException("Node has been removed");
        }
        if (this.prefsCache == null) {
            return;
        }
        if (!isUserNode() ? !isSystemRootModified : !isUserRootModified) {
            long jLastModified = this.prefsFile.lastModified();
            if (jLastModified != this.lastSyncTime) {
                loadCache();
                replayChanges();
                this.lastSyncTime = jLastModified;
            }
        } else if (this.lastSyncTime != 0 && !this.dir.exists()) {
            this.prefsCache = new TreeMap();
            replayChanges();
        }
        if (!this.changeLog.isEmpty()) {
            writeBackCache();
            long jLastModified2 = this.prefsFile.lastModified();
            if (this.lastSyncTime <= jLastModified2) {
                this.lastSyncTime = jLastModified2 + 1000;
                this.prefsFile.setLastModified(this.lastSyncTime);
            }
            this.changeLog.clear();
        }
    }

    @Override
    public void flush() throws BackingStoreException {
        if (isRemoved()) {
            return;
        }
        sync();
    }

    @Override
    protected void flushSpi() throws BackingStoreException {
    }

    private static boolean isDirChar(char c) {
        return (c <= 31 || c >= 127 || c == '/' || c == '.' || c == '_') ? false : true;
    }

    private static String dirName(String str) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (!isDirChar(str.charAt(i))) {
                return BaseLocale.SEP + Base64.byteArrayToAltBase64(byteArray(str));
            }
        }
        return str;
    }

    private static byte[] byteArray(String str) {
        int length = str.length();
        byte[] bArr = new byte[2 * length];
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            int i3 = i + 1;
            bArr[i] = (byte) (cCharAt >> '\b');
            i = i3 + 1;
            bArr[i3] = (byte) cCharAt;
        }
        return bArr;
    }

    private static String nodeName(String str) {
        int i = 0;
        if (str.charAt(0) != '_') {
            return str;
        }
        byte[] bArrAltBase64ToByteArray = Base64.altBase64ToByteArray(str.substring(1));
        StringBuffer stringBuffer = new StringBuffer(bArrAltBase64ToByteArray.length / 2);
        while (i < bArrAltBase64ToByteArray.length) {
            int i2 = i + 1;
            stringBuffer.append((char) (((bArrAltBase64ToByteArray[i] & Character.DIRECTIONALITY_UNDEFINED) << 8) | (bArrAltBase64ToByteArray[i2] & Character.DIRECTIONALITY_UNDEFINED)));
            i = i2 + 1;
        }
        return stringBuffer.toString();
    }

    private boolean lockFile(boolean z) throws SecurityException {
        int i;
        boolean zIsUserNode = isUserNode();
        File file = zIsUserNode ? userLockFile : systemLockFile;
        long j = INIT_SLEEP_TIME;
        int i2 = 0;
        for (int i3 = 0; i3 < MAX_ATTEMPTS; i3++) {
            try {
                int[] iArrLockFile0 = lockFile0(file.getCanonicalPath(), zIsUserNode ? USER_READ_WRITE : USER_RW_ALL_READ, z);
                i = iArrLockFile0[1];
                try {
                    if (iArrLockFile0[0] != 0) {
                        if (zIsUserNode) {
                            userRootLockHandle = iArrLockFile0[0];
                        } else {
                            systemRootLockHandle = iArrLockFile0[0];
                        }
                        return true;
                    }
                } catch (IOException e) {
                }
            } catch (IOException e2) {
                i = i2;
            }
            i2 = i;
            try {
                Thread.sleep(j);
                j *= 2;
            } catch (InterruptedException e3) {
                checkLockFile0ErrorCode(i2);
                return false;
            }
        }
        checkLockFile0ErrorCode(i2);
        return false;
    }

    private void checkLockFile0ErrorCode(int i) throws SecurityException {
        if (i == 13) {
            StringBuilder sb = new StringBuilder();
            sb.append("Could not lock ");
            sb.append(isUserNode() ? "User prefs." : "System prefs.");
            sb.append(" Lock file access denied.");
            throw new SecurityException(sb.toString());
        }
        if (i != 11) {
            PlatformLogger logger = getLogger();
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Could not lock ");
            sb2.append(isUserNode() ? "User prefs. " : "System prefs.");
            sb2.append(" Unix error code ");
            sb2.append(i);
            sb2.append(".");
            logger.warning(sb2.toString());
        }
    }

    private void unlockFile() {
        boolean zIsUserNode = isUserNode();
        if (zIsUserNode) {
            File file = userLockFile;
        } else {
            File file2 = systemLockFile;
        }
        int i = zIsUserNode ? userRootLockHandle : systemRootLockHandle;
        if (i == 0) {
            PlatformLogger logger = getLogger();
            StringBuilder sb = new StringBuilder();
            sb.append("Unlock: zero lockHandle for ");
            sb.append(zIsUserNode ? "user" : "system");
            sb.append(" preferences.)");
            logger.warning(sb.toString());
            return;
        }
        int iUnlockFile0 = unlockFile0(i);
        if (iUnlockFile0 != 0) {
            PlatformLogger logger2 = getLogger();
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Could not drop file-lock on ");
            sb2.append(isUserNode() ? "user" : "system");
            sb2.append(" preferences. Unix error code ");
            sb2.append(iUnlockFile0);
            sb2.append(".");
            logger2.warning(sb2.toString());
            if (iUnlockFile0 == 13) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Could not unlock");
                sb3.append(isUserNode() ? "User prefs." : "System prefs.");
                sb3.append(" Lock file access denied.");
                throw new SecurityException(sb3.toString());
            }
        }
        if (isUserNode()) {
            userRootLockHandle = 0;
        } else {
            systemRootLockHandle = 0;
        }
    }
}
