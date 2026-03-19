package android.app;

import android.content.SharedPreferences;
import android.os.FileUtils;
import android.os.Looper;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import android.system.StructTimespec;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ExponentiallyBucketedHistogram;
import com.android.internal.util.XmlUtils;
import dalvik.system.BlockGuard;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;

final class SharedPreferencesImpl implements SharedPreferences {
    private static final Object CONTENT = new Object();
    private static final boolean DEBUG = false;
    private static final long MAX_FSYNC_DURATION_MILLIS = 256;
    private static final String TAG = "SharedPreferencesImpl";
    private final File mBackupFile;

    @GuardedBy("this")
    private long mCurrentMemoryStateGeneration;

    @GuardedBy("mWritingToDiskLock")
    private long mDiskStateGeneration;
    private final File mFile;

    @GuardedBy("mLock")
    private boolean mLoaded;
    private final int mMode;

    @GuardedBy("mLock")
    private long mStatSize;

    @GuardedBy("mLock")
    private StructTimespec mStatTimestamp;
    private final Object mLock = new Object();
    private final Object mWritingToDiskLock = new Object();

    @GuardedBy("mLock")
    private int mDiskWritesInFlight = 0;

    @GuardedBy("mLock")
    private final WeakHashMap<SharedPreferences.OnSharedPreferenceChangeListener, Object> mListeners = new WeakHashMap<>();

    @GuardedBy("mWritingToDiskLock")
    private final ExponentiallyBucketedHistogram mSyncTimes = new ExponentiallyBucketedHistogram(16);
    private int mNumSync = 0;

    @GuardedBy("mLock")
    private Map<String, Object> mMap = null;

    @GuardedBy("mLock")
    private Throwable mThrowable = null;

    static int access$308(SharedPreferencesImpl sharedPreferencesImpl) {
        int i = sharedPreferencesImpl.mDiskWritesInFlight;
        sharedPreferencesImpl.mDiskWritesInFlight = i + 1;
        return i;
    }

    static int access$310(SharedPreferencesImpl sharedPreferencesImpl) {
        int i = sharedPreferencesImpl.mDiskWritesInFlight;
        sharedPreferencesImpl.mDiskWritesInFlight = i - 1;
        return i;
    }

    static long access$608(SharedPreferencesImpl sharedPreferencesImpl) {
        long j = sharedPreferencesImpl.mCurrentMemoryStateGeneration;
        sharedPreferencesImpl.mCurrentMemoryStateGeneration = 1 + j;
        return j;
    }

    SharedPreferencesImpl(File file, int i) {
        this.mLoaded = false;
        this.mFile = file;
        this.mBackupFile = makeBackupFile(file);
        this.mMode = i;
        this.mLoaded = false;
        startLoadFromDisk();
    }

    private void startLoadFromDisk() {
        synchronized (this.mLock) {
            this.mLoaded = false;
        }
        new Thread("SharedPreferencesImpl-load") {
            @Override
            public void run() {
                SharedPreferencesImpl.this.loadFromDisk();
            }
        }.start();
    }

    private void loadFromDisk() {
        HashMap<String, ?> mapXml;
        StructStat structStatStat;
        Object obj;
        BufferedInputStream bufferedInputStream;
        synchronized (this.mLock) {
            if (this.mLoaded) {
                return;
            }
            if (this.mBackupFile.exists()) {
                this.mFile.delete();
                this.mBackupFile.renameTo(this.mFile);
            }
            if (this.mFile.exists() && !this.mFile.canRead()) {
                Log.w(TAG, "Attempt to read preferences file " + this.mFile + " without permission");
            }
            Throwable th = null;
            try {
                structStatStat = Os.stat(this.mFile.getPath());
                try {
                    ?? CanRead = this.mFile.canRead();
                    try {
                        if (CanRead != 0) {
                            try {
                                bufferedInputStream = new BufferedInputStream(new FileInputStream(this.mFile), 16384);
                                try {
                                    mapXml = XmlUtils.readMapXml(bufferedInputStream);
                                    try {
                                        IoUtils.closeQuietly(bufferedInputStream);
                                    } catch (ErrnoException e) {
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                } catch (Exception e2) {
                                    e = e2;
                                    Log.w(TAG, "Cannot read " + this.mFile.getAbsolutePath(), e);
                                    IoUtils.closeQuietly(bufferedInputStream);
                                    mapXml = null;
                                }
                            } catch (Exception e3) {
                                e = e3;
                                bufferedInputStream = null;
                            } catch (Throwable th3) {
                                th = th3;
                                CanRead = 0;
                                IoUtils.closeQuietly((AutoCloseable) CanRead);
                                throw th;
                            }
                        } else {
                            mapXml = null;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                    }
                } catch (ErrnoException e4) {
                    mapXml = null;
                } catch (Throwable th5) {
                    mapXml = null;
                    th = th5;
                }
            } catch (ErrnoException e5) {
                structStatStat = null;
                mapXml = null;
            } catch (Throwable th6) {
                mapXml = null;
                th = th6;
                structStatStat = null;
            }
            synchronized (this.mLock) {
                this.mLoaded = true;
                this.mThrowable = th;
                if (th == null) {
                    try {
                        try {
                            if (mapXml != null) {
                                this.mMap = mapXml;
                                this.mStatTimestamp = structStatStat.st_mtim;
                                this.mStatSize = structStatStat.st_size;
                            } else {
                                this.mMap = new HashMap();
                            }
                            obj = this.mLock;
                        } catch (Throwable th7) {
                            this.mLock.notifyAll();
                            throw th7;
                        }
                    } catch (Throwable th8) {
                        this.mThrowable = th8;
                        obj = this.mLock;
                    }
                } else {
                    obj = this.mLock;
                }
                obj.notifyAll();
            }
        }
    }

    static File makeBackupFile(File file) {
        return new File(file.getPath() + ".bak");
    }

    void startReloadIfChangedUnexpectedly() {
        synchronized (this.mLock) {
            if (hasFileChangedUnexpectedly()) {
                startLoadFromDisk();
            }
        }
    }

    private boolean hasFileChangedUnexpectedly() {
        synchronized (this.mLock) {
            if (this.mDiskWritesInFlight > 0) {
                return false;
            }
            boolean z = true;
            try {
                BlockGuard.getThreadPolicy().onReadFromDisk();
                StructStat structStatStat = Os.stat(this.mFile.getPath());
                synchronized (this.mLock) {
                    if (structStatStat.st_mtim.equals(this.mStatTimestamp) && this.mStatSize == structStatStat.st_size) {
                        z = false;
                    }
                }
                return z;
            } catch (ErrnoException e) {
                return true;
            }
        }
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        synchronized (this.mLock) {
            this.mListeners.put(onSharedPreferenceChangeListener, CONTENT);
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        synchronized (this.mLock) {
            this.mListeners.remove(onSharedPreferenceChangeListener);
        }
    }

    @GuardedBy("mLock")
    private void awaitLoadedLocked() {
        if (!this.mLoaded) {
            BlockGuard.getThreadPolicy().onReadFromDisk();
        }
        while (!this.mLoaded) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
            }
        }
        if (this.mThrowable != null) {
            throw new IllegalStateException(this.mThrowable);
        }
    }

    @Override
    public Map<String, ?> getAll() {
        HashMap map;
        synchronized (this.mLock) {
            awaitLoadedLocked();
            map = new HashMap(this.mMap);
        }
        return map;
    }

    @Override
    public String getString(String str, String str2) {
        String str3;
        synchronized (this.mLock) {
            awaitLoadedLocked();
            str3 = (String) this.mMap.get(str);
            if (str3 == null) {
                str3 = str2;
            }
        }
        return str3;
    }

    @Override
    public Set<String> getStringSet(String str, Set<String> set) {
        Set<String> set2;
        synchronized (this.mLock) {
            awaitLoadedLocked();
            set2 = (Set) this.mMap.get(str);
            if (set2 == null) {
                set2 = set;
            }
        }
        return set2;
    }

    @Override
    public int getInt(String str, int i) {
        synchronized (this.mLock) {
            awaitLoadedLocked();
            Integer num = (Integer) this.mMap.get(str);
            if (num != null) {
                i = num.intValue();
            }
        }
        return i;
    }

    @Override
    public long getLong(String str, long j) {
        synchronized (this.mLock) {
            awaitLoadedLocked();
            Long l = (Long) this.mMap.get(str);
            if (l != null) {
                j = l.longValue();
            }
        }
        return j;
    }

    @Override
    public float getFloat(String str, float f) {
        synchronized (this.mLock) {
            awaitLoadedLocked();
            Float f2 = (Float) this.mMap.get(str);
            if (f2 != null) {
                f = f2.floatValue();
            }
        }
        return f;
    }

    @Override
    public boolean getBoolean(String str, boolean z) {
        synchronized (this.mLock) {
            awaitLoadedLocked();
            Boolean bool = (Boolean) this.mMap.get(str);
            if (bool != null) {
                z = bool.booleanValue();
            }
        }
        return z;
    }

    @Override
    public boolean contains(String str) {
        boolean zContainsKey;
        synchronized (this.mLock) {
            awaitLoadedLocked();
            zContainsKey = this.mMap.containsKey(str);
        }
        return zContainsKey;
    }

    @Override
    public SharedPreferences.Editor edit() {
        synchronized (this.mLock) {
            awaitLoadedLocked();
        }
        return new EditorImpl();
    }

    private static class MemoryCommitResult {
        final List<String> keysModified;
        final Set<SharedPreferences.OnSharedPreferenceChangeListener> listeners;
        final Map<String, Object> mapToWriteToDisk;
        final long memoryStateGeneration;
        boolean wasWritten;

        @GuardedBy("mWritingToDiskLock")
        volatile boolean writeToDiskResult;
        final CountDownLatch writtenToDiskLatch;

        private MemoryCommitResult(long j, List<String> list, Set<SharedPreferences.OnSharedPreferenceChangeListener> set, Map<String, Object> map) {
            this.writtenToDiskLatch = new CountDownLatch(1);
            this.writeToDiskResult = false;
            this.wasWritten = false;
            this.memoryStateGeneration = j;
            this.keysModified = list;
            this.listeners = set;
            this.mapToWriteToDisk = map;
        }

        void setDiskWriteResult(boolean z, boolean z2) {
            this.wasWritten = z;
            this.writeToDiskResult = z2;
            this.writtenToDiskLatch.countDown();
        }
    }

    public final class EditorImpl implements SharedPreferences.Editor {
        private final Object mEditorLock = new Object();

        @GuardedBy("mEditorLock")
        private final Map<String, Object> mModified = new HashMap();

        @GuardedBy("mEditorLock")
        private boolean mClear = false;

        public EditorImpl() {
        }

        @Override
        public SharedPreferences.Editor putString(String str, String str2) {
            synchronized (this.mEditorLock) {
                this.mModified.put(str, str2);
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String str, Set<String> set) {
            synchronized (this.mEditorLock) {
                this.mModified.put(str, set == null ? null : new HashSet(set));
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String str, int i) {
            synchronized (this.mEditorLock) {
                this.mModified.put(str, Integer.valueOf(i));
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String str, long j) {
            synchronized (this.mEditorLock) {
                this.mModified.put(str, Long.valueOf(j));
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String str, float f) {
            synchronized (this.mEditorLock) {
                this.mModified.put(str, Float.valueOf(f));
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String str, boolean z) {
            synchronized (this.mEditorLock) {
                this.mModified.put(str, Boolean.valueOf(z));
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String str) {
            synchronized (this.mEditorLock) {
                this.mModified.put(str, this);
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            synchronized (this.mEditorLock) {
                this.mClear = true;
            }
            return this;
        }

        @Override
        public void apply() {
            final long jCurrentTimeMillis = System.currentTimeMillis();
            final MemoryCommitResult memoryCommitResultCommitToMemory = commitToMemory();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        memoryCommitResultCommitToMemory.writtenToDiskLatch.await();
                    } catch (InterruptedException e) {
                    }
                }
            };
            QueuedWork.addFinisher(runnable);
            SharedPreferencesImpl.this.enqueueDiskWrite(memoryCommitResultCommitToMemory, new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                    QueuedWork.removeFinisher(runnable);
                }
            });
            notifyListeners(memoryCommitResultCommitToMemory);
        }

        private MemoryCommitResult commitToMemory() {
            Map map;
            ArrayList arrayList;
            HashSet hashSet;
            long j;
            Object obj;
            boolean z;
            synchronized (SharedPreferencesImpl.this.mLock) {
                if (SharedPreferencesImpl.this.mDiskWritesInFlight > 0) {
                    SharedPreferencesImpl.this.mMap = new HashMap(SharedPreferencesImpl.this.mMap);
                }
                map = SharedPreferencesImpl.this.mMap;
                SharedPreferencesImpl.access$308(SharedPreferencesImpl.this);
                boolean z2 = false;
                boolean z3 = SharedPreferencesImpl.this.mListeners.size() > 0;
                if (z3) {
                    ArrayList arrayList2 = new ArrayList();
                    hashSet = new HashSet(SharedPreferencesImpl.this.mListeners.keySet());
                    arrayList = arrayList2;
                } else {
                    arrayList = null;
                    hashSet = null;
                }
                synchronized (this.mEditorLock) {
                    if (this.mClear) {
                        if (map.isEmpty()) {
                            z = false;
                        } else {
                            map.clear();
                            z = true;
                        }
                        this.mClear = false;
                        z2 = z;
                    }
                    for (Map.Entry<String, Object> entry : this.mModified.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        if (value == this || value == null) {
                            if (map.containsKey(key)) {
                                map.remove(key);
                                if (z3) {
                                    arrayList.add(key);
                                }
                                z2 = true;
                            }
                        } else if (!map.containsKey(key) || (obj = map.get(key)) == null || !obj.equals(value)) {
                            map.put(key, value);
                            if (z3) {
                            }
                            z2 = true;
                        }
                    }
                    this.mModified.clear();
                    if (z2) {
                        SharedPreferencesImpl.access$608(SharedPreferencesImpl.this);
                    }
                    j = SharedPreferencesImpl.this.mCurrentMemoryStateGeneration;
                }
            }
            return new MemoryCommitResult(j, arrayList, hashSet, map);
        }

        @Override
        public boolean commit() {
            MemoryCommitResult memoryCommitResultCommitToMemory = commitToMemory();
            SharedPreferencesImpl.this.enqueueDiskWrite(memoryCommitResultCommitToMemory, null);
            try {
                memoryCommitResultCommitToMemory.writtenToDiskLatch.await();
                notifyListeners(memoryCommitResultCommitToMemory);
                return memoryCommitResultCommitToMemory.writeToDiskResult;
            } catch (InterruptedException e) {
                return false;
            }
        }

        private void notifyListeners(final MemoryCommitResult memoryCommitResult) {
            if (memoryCommitResult.listeners == null || memoryCommitResult.keysModified == null || memoryCommitResult.keysModified.size() == 0) {
                return;
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                for (int size = memoryCommitResult.keysModified.size() - 1; size >= 0; size--) {
                    String str = memoryCommitResult.keysModified.get(size);
                    for (SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener : memoryCommitResult.listeners) {
                        if (onSharedPreferenceChangeListener != null) {
                            onSharedPreferenceChangeListener.onSharedPreferenceChanged(SharedPreferencesImpl.this, str);
                        }
                    }
                }
                return;
            }
            ActivityThread.sMainThreadHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.notifyListeners(memoryCommitResult);
                }
            });
        }
    }

    private void enqueueDiskWrite(final MemoryCommitResult memoryCommitResult, final Runnable runnable) {
        boolean z = false;
        final boolean z2 = runnable == null;
        Runnable runnable2 = new Runnable() {
            @Override
            public void run() {
                synchronized (SharedPreferencesImpl.this.mWritingToDiskLock) {
                    SharedPreferencesImpl.this.writeToFile(memoryCommitResult, z2);
                }
                synchronized (SharedPreferencesImpl.this.mLock) {
                    SharedPreferencesImpl.access$310(SharedPreferencesImpl.this);
                }
                if (runnable != null) {
                    runnable.run();
                }
            }
        };
        if (z2) {
            synchronized (this.mLock) {
                if (this.mDiskWritesInFlight == 1) {
                    z = true;
                }
            }
            if (z) {
                runnable2.run();
                return;
            }
        }
        QueuedWork.queue(runnable2, !z2);
    }

    private static FileOutputStream createFileOutputStream(File file) {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            File parentFile = file.getParentFile();
            if (!parentFile.mkdir()) {
                Log.e(TAG, "Couldn't create directory for SharedPreferences file " + file);
                return null;
            }
            FileUtils.setPermissions(parentFile.getPath(), 505, -1, -1);
            try {
                return new FileOutputStream(file);
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "Couldn't create SharedPreferences file " + file, e2);
                return null;
            }
        }
    }

    @GuardedBy("mWritingToDiskLock")
    private void writeToFile(MemoryCommitResult memoryCommitResult, boolean z) {
        boolean z2;
        if (this.mFile.exists()) {
            if (this.mDiskStateGeneration < memoryCommitResult.memoryStateGeneration) {
                if (!z) {
                    synchronized (this.mLock) {
                        z2 = this.mCurrentMemoryStateGeneration == memoryCommitResult.memoryStateGeneration;
                    }
                } else {
                    z2 = true;
                }
            } else {
                z2 = false;
            }
            if (!z2) {
                memoryCommitResult.setDiskWriteResult(false, true);
                return;
            }
            if (!this.mBackupFile.exists()) {
                if (!this.mFile.renameTo(this.mBackupFile)) {
                    Log.e(TAG, "Couldn't rename file " + this.mFile + " to backup file " + this.mBackupFile);
                    memoryCommitResult.setDiskWriteResult(false, false);
                    return;
                }
            } else {
                this.mFile.delete();
            }
        }
        try {
            FileOutputStream fileOutputStreamCreateFileOutputStream = createFileOutputStream(this.mFile);
            if (fileOutputStreamCreateFileOutputStream == null) {
                memoryCommitResult.setDiskWriteResult(false, false);
                return;
            }
            XmlUtils.writeMapXml(memoryCommitResult.mapToWriteToDisk, fileOutputStreamCreateFileOutputStream);
            long jCurrentTimeMillis = System.currentTimeMillis();
            FileUtils.sync(fileOutputStreamCreateFileOutputStream);
            long jCurrentTimeMillis2 = System.currentTimeMillis();
            fileOutputStreamCreateFileOutputStream.close();
            ContextImpl.setFilePermissionsFromMode(this.mFile.getPath(), this.mMode, 0);
            try {
                StructStat structStatStat = Os.stat(this.mFile.getPath());
                synchronized (this.mLock) {
                    this.mStatTimestamp = structStatStat.st_mtim;
                    this.mStatSize = structStatStat.st_size;
                }
            } catch (ErrnoException e) {
            }
            this.mBackupFile.delete();
            this.mDiskStateGeneration = memoryCommitResult.memoryStateGeneration;
            memoryCommitResult.setDiskWriteResult(true, true);
            long j = jCurrentTimeMillis2 - jCurrentTimeMillis;
            this.mSyncTimes.add((int) j);
            this.mNumSync++;
            if (this.mNumSync % 1024 == 0 || j > 256) {
                this.mSyncTimes.log(TAG, "Time required to fsync " + this.mFile + ": ");
            }
        } catch (IOException e2) {
            Log.w(TAG, "writeToFile: Got exception:", e2);
            if (this.mFile.exists() && !this.mFile.delete()) {
                Log.e(TAG, "Couldn't clean up partially-written file " + this.mFile);
            }
            memoryCommitResult.setDiskWriteResult(false, false);
        } catch (XmlPullParserException e3) {
            Log.w(TAG, "writeToFile: Got exception:", e3);
            if (this.mFile.exists()) {
                Log.e(TAG, "Couldn't clean up partially-written file " + this.mFile);
            }
            memoryCommitResult.setDiskWriteResult(false, false);
        }
    }
}
