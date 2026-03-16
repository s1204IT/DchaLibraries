package android.app;

import android.content.SharedPreferences;
import android.os.FileUtils;
import android.os.Looper;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import android.util.Log;
import com.android.internal.util.XmlUtils;
import com.google.android.collect.Maps;
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
    private static final boolean DEBUG = false;
    private static final String TAG = "SharedPreferencesImpl";
    private static final Object mContent = new Object();
    private final File mBackupFile;
    private final File mFile;
    private boolean mLoaded;
    private final int mMode;
    private long mStatSize;
    private long mStatTimestamp;
    private int mDiskWritesInFlight = 0;
    private final Object mWritingToDiskLock = new Object();
    private final WeakHashMap<SharedPreferences.OnSharedPreferenceChangeListener, Object> mListeners = new WeakHashMap<>();
    private Map<String, Object> mMap = null;

    static int access$308(SharedPreferencesImpl x0) {
        int i = x0.mDiskWritesInFlight;
        x0.mDiskWritesInFlight = i + 1;
        return i;
    }

    static int access$310(SharedPreferencesImpl x0) {
        int i = x0.mDiskWritesInFlight;
        x0.mDiskWritesInFlight = i - 1;
        return i;
    }

    SharedPreferencesImpl(File file, int mode) {
        this.mLoaded = false;
        this.mFile = file;
        this.mBackupFile = makeBackupFile(file);
        this.mMode = mode;
        this.mLoaded = false;
        startLoadFromDisk();
    }

    private void startLoadFromDisk() {
        synchronized (this) {
            this.mLoaded = false;
        }
        new Thread("SharedPreferencesImpl-load") {
            @Override
            public void run() {
                synchronized (SharedPreferencesImpl.this) {
                    SharedPreferencesImpl.this.loadFromDiskLocked();
                }
            }
        }.start();
    }

    private void loadFromDiskLocked() throws Throwable {
        BufferedInputStream str;
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
        Map map = null;
        StructStat stat = null;
        try {
            stat = Os.stat(this.mFile.getPath());
            if (this.mFile.canRead()) {
                BufferedInputStream str2 = null;
                try {
                    try {
                        str = new BufferedInputStream(new FileInputStream(this.mFile), 16384);
                    } catch (Throwable th) {
                        th = th;
                    }
                } catch (FileNotFoundException e) {
                    e = e;
                } catch (IOException e2) {
                    e = e2;
                } catch (XmlPullParserException e3) {
                    e = e3;
                }
                try {
                    map = XmlUtils.readMapXml(str);
                    IoUtils.closeQuietly(str);
                } catch (FileNotFoundException e4) {
                    e = e4;
                    str2 = str;
                    Log.w(TAG, "getSharedPreferences", e);
                    IoUtils.closeQuietly(str2);
                } catch (IOException e5) {
                    e = e5;
                    str2 = str;
                    Log.w(TAG, "getSharedPreferences", e);
                    IoUtils.closeQuietly(str2);
                } catch (XmlPullParserException e6) {
                    e = e6;
                    str2 = str;
                    Log.w(TAG, "getSharedPreferences", e);
                    IoUtils.closeQuietly(str2);
                } catch (Throwable th2) {
                    th = th2;
                    str2 = str;
                    IoUtils.closeQuietly(str2);
                    throw th;
                }
            }
        } catch (ErrnoException e7) {
        }
        this.mLoaded = true;
        if (map != null) {
            this.mMap = map;
            this.mStatTimestamp = stat.st_mtime;
            this.mStatSize = stat.st_size;
        } else {
            this.mMap = new HashMap();
        }
        notifyAll();
    }

    private static File makeBackupFile(File prefsFile) {
        return new File(prefsFile.getPath() + ".bak");
    }

    void startReloadIfChangedUnexpectedly() {
        synchronized (this) {
            if (hasFileChangedUnexpectedly()) {
                startLoadFromDisk();
            }
        }
    }

    private boolean hasFileChangedUnexpectedly() {
        boolean z;
        synchronized (this) {
            if (this.mDiskWritesInFlight > 0) {
                return false;
            }
            try {
                BlockGuard.getThreadPolicy().onReadFromDisk();
                StructStat stat = Os.stat(this.mFile.getPath());
                synchronized (this) {
                    z = (this.mStatTimestamp == stat.st_mtime && this.mStatSize == stat.st_size) ? false : true;
                }
                return z;
            } catch (ErrnoException e) {
                return true;
            }
        }
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        synchronized (this) {
            this.mListeners.put(listener, mContent);
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        synchronized (this) {
            this.mListeners.remove(listener);
        }
    }

    private void awaitLoadedLocked() {
        if (!this.mLoaded) {
            BlockGuard.getThreadPolicy().onReadFromDisk();
        }
        while (!this.mLoaded) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public Map<String, ?> getAll() {
        HashMap map;
        synchronized (this) {
            awaitLoadedLocked();
            map = new HashMap(this.mMap);
        }
        return map;
    }

    @Override
    public String getString(String key, String defValue) {
        String v;
        synchronized (this) {
            awaitLoadedLocked();
            v = (String) this.mMap.get(key);
            if (v == null) {
                v = defValue;
            }
        }
        return v;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Set<String> v;
        synchronized (this) {
            awaitLoadedLocked();
            v = (Set) this.mMap.get(key);
            if (v == null) {
                v = defValues;
            }
        }
        return v;
    }

    @Override
    public int getInt(String key, int defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Integer v = (Integer) this.mMap.get(key);
            if (v != null) {
                defValue = v.intValue();
            }
        }
        return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Long v = (Long) this.mMap.get(key);
            if (v != null) {
                defValue = v.longValue();
            }
        }
        return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Float v = (Float) this.mMap.get(key);
            if (v != null) {
                defValue = v.floatValue();
            }
        }
        return defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Boolean v = (Boolean) this.mMap.get(key);
            if (v != null) {
                defValue = v.booleanValue();
            }
        }
        return defValue;
    }

    @Override
    public boolean contains(String key) {
        boolean zContainsKey;
        synchronized (this) {
            awaitLoadedLocked();
            zContainsKey = this.mMap.containsKey(key);
        }
        return zContainsKey;
    }

    @Override
    public SharedPreferences.Editor edit() {
        synchronized (this) {
            awaitLoadedLocked();
        }
        return new EditorImpl();
    }

    private static class MemoryCommitResult {
        public boolean changesMade;
        public List<String> keysModified;
        public Set<SharedPreferences.OnSharedPreferenceChangeListener> listeners;
        public Map<?, ?> mapToWriteToDisk;
        public volatile boolean writeToDiskResult;
        public final CountDownLatch writtenToDiskLatch;

        private MemoryCommitResult() {
            this.writtenToDiskLatch = new CountDownLatch(1);
            this.writeToDiskResult = false;
        }

        public void setDiskWriteResult(boolean result) {
            this.writeToDiskResult = result;
            this.writtenToDiskLatch.countDown();
        }
    }

    public final class EditorImpl implements SharedPreferences.Editor {
        private final Map<String, Object> mModified = Maps.newHashMap();
        private boolean mClear = false;

        public EditorImpl() {
        }

        @Override
        public SharedPreferences.Editor putString(String key, String value) {
            synchronized (this) {
                this.mModified.put(key, value);
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
            synchronized (this) {
                this.mModified.put(key, values == null ? null : new HashSet(values));
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            synchronized (this) {
                this.mModified.put(key, Integer.valueOf(value));
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            synchronized (this) {
                this.mModified.put(key, Long.valueOf(value));
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            synchronized (this) {
                this.mModified.put(key, Float.valueOf(value));
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            synchronized (this) {
                this.mModified.put(key, Boolean.valueOf(value));
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            synchronized (this) {
                this.mModified.put(key, this);
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            synchronized (this) {
                this.mClear = true;
            }
            return this;
        }

        @Override
        public void apply() {
            final MemoryCommitResult mcr = commitToMemory();
            final Runnable awaitCommit = new Runnable() {
                @Override
                public void run() {
                    try {
                        mcr.writtenToDiskLatch.await();
                    } catch (InterruptedException e) {
                    }
                }
            };
            QueuedWork.add(awaitCommit);
            Runnable postWriteRunnable = new Runnable() {
                @Override
                public void run() {
                    awaitCommit.run();
                    QueuedWork.remove(awaitCommit);
                }
            };
            SharedPreferencesImpl.this.enqueueDiskWrite(mcr, postWriteRunnable);
            notifyListeners(mcr);
        }

        private MemoryCommitResult commitToMemory() {
            Object existingValue;
            MemoryCommitResult mcr = new MemoryCommitResult();
            synchronized (SharedPreferencesImpl.this) {
                if (SharedPreferencesImpl.this.mDiskWritesInFlight > 0) {
                    SharedPreferencesImpl.this.mMap = new HashMap(SharedPreferencesImpl.this.mMap);
                }
                mcr.mapToWriteToDisk = SharedPreferencesImpl.this.mMap;
                SharedPreferencesImpl.access$308(SharedPreferencesImpl.this);
                boolean hasListeners = SharedPreferencesImpl.this.mListeners.size() > 0;
                if (hasListeners) {
                    mcr.keysModified = new ArrayList();
                    mcr.listeners = new HashSet(SharedPreferencesImpl.this.mListeners.keySet());
                }
                synchronized (this) {
                    if (this.mClear) {
                        if (!SharedPreferencesImpl.this.mMap.isEmpty()) {
                            mcr.changesMade = true;
                            SharedPreferencesImpl.this.mMap.clear();
                        }
                        this.mClear = false;
                    }
                    for (Map.Entry<String, Object> e : this.mModified.entrySet()) {
                        String k = e.getKey();
                        Object v = e.getValue();
                        if (v == this || v == null) {
                            if (SharedPreferencesImpl.this.mMap.containsKey(k)) {
                                SharedPreferencesImpl.this.mMap.remove(k);
                                mcr.changesMade = true;
                                if (!hasListeners) {
                                    mcr.keysModified.add(k);
                                }
                            }
                        } else if (!SharedPreferencesImpl.this.mMap.containsKey(k) || (existingValue = SharedPreferencesImpl.this.mMap.get(k)) == null || !existingValue.equals(v)) {
                            SharedPreferencesImpl.this.mMap.put(k, v);
                            mcr.changesMade = true;
                            if (!hasListeners) {
                            }
                        }
                    }
                    this.mModified.clear();
                }
            }
            return mcr;
        }

        @Override
        public boolean commit() {
            MemoryCommitResult mcr = commitToMemory();
            SharedPreferencesImpl.this.enqueueDiskWrite(mcr, null);
            try {
                mcr.writtenToDiskLatch.await();
                notifyListeners(mcr);
                return mcr.writeToDiskResult;
            } catch (InterruptedException e) {
                return false;
            }
        }

        private void notifyListeners(final MemoryCommitResult mcr) {
            if (mcr.listeners != null && mcr.keysModified != null && mcr.keysModified.size() != 0) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    for (int i = mcr.keysModified.size() - 1; i >= 0; i--) {
                        String key = mcr.keysModified.get(i);
                        for (SharedPreferences.OnSharedPreferenceChangeListener listener : mcr.listeners) {
                            if (listener != null) {
                                listener.onSharedPreferenceChanged(SharedPreferencesImpl.this, key);
                            }
                        }
                    }
                    return;
                }
                ActivityThread.sMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        EditorImpl.this.notifyListeners(mcr);
                    }
                });
            }
        }
    }

    private void enqueueDiskWrite(final MemoryCommitResult mcr, final Runnable postWriteRunnable) {
        boolean wasEmpty;
        Runnable writeToDiskRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (SharedPreferencesImpl.this.mWritingToDiskLock) {
                    SharedPreferencesImpl.this.writeToFile(mcr);
                }
                synchronized (SharedPreferencesImpl.this) {
                    SharedPreferencesImpl.access$310(SharedPreferencesImpl.this);
                }
                if (postWriteRunnable != null) {
                    postWriteRunnable.run();
                }
            }
        };
        boolean isFromSyncCommit = postWriteRunnable == null;
        if (isFromSyncCommit) {
            synchronized (this) {
                wasEmpty = this.mDiskWritesInFlight == 1;
            }
            if (wasEmpty) {
                writeToDiskRunnable.run();
                return;
            }
        }
        QueuedWork.singleThreadExecutor().execute(writeToDiskRunnable);
    }

    private static FileOutputStream createFileOutputStream(File file) {
        FileOutputStream str = null;
        try {
            FileOutputStream str2 = new FileOutputStream(file);
            str = str2;
        } catch (FileNotFoundException e) {
            File parent = file.getParentFile();
            if (!parent.mkdir()) {
                Log.e(TAG, "Couldn't create directory for SharedPreferences file " + file);
                return null;
            }
            FileUtils.setPermissions(parent.getPath(), 505, -1, -1);
            try {
                FileOutputStream str3 = new FileOutputStream(file);
                str = str3;
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "Couldn't create SharedPreferences file " + file, e2);
            }
        }
        return str;
    }

    private void writeToFile(MemoryCommitResult mcr) {
        if (this.mFile.exists()) {
            if (!mcr.changesMade) {
                mcr.setDiskWriteResult(true);
                return;
            } else if (!this.mBackupFile.exists()) {
                if (!this.mFile.renameTo(this.mBackupFile)) {
                    Log.e(TAG, "Couldn't rename file " + this.mFile + " to backup file " + this.mBackupFile);
                    mcr.setDiskWriteResult(false);
                    return;
                }
            } else {
                this.mFile.delete();
            }
        }
        try {
            FileOutputStream str = createFileOutputStream(this.mFile);
            if (str == null) {
                mcr.setDiskWriteResult(false);
                return;
            }
            XmlUtils.writeMapXml(mcr.mapToWriteToDisk, str);
            FileUtils.sync(str);
            str.close();
            ContextImpl.setFilePermissionsFromMode(this.mFile.getPath(), this.mMode, 0);
            try {
                StructStat stat = Os.stat(this.mFile.getPath());
                synchronized (this) {
                    this.mStatTimestamp = stat.st_mtime;
                    this.mStatSize = stat.st_size;
                }
            } catch (ErrnoException e) {
            }
            this.mBackupFile.delete();
            mcr.setDiskWriteResult(true);
        } catch (IOException e2) {
            Log.w(TAG, "writeToFile: Got exception:", e2);
            if (this.mFile.exists() && !this.mFile.delete()) {
                Log.e(TAG, "Couldn't clean up partially-written file " + this.mFile);
            }
            mcr.setDiskWriteResult(false);
        } catch (XmlPullParserException e3) {
            Log.w(TAG, "writeToFile: Got exception:", e3);
            if (this.mFile.exists()) {
                Log.e(TAG, "Couldn't clean up partially-written file " + this.mFile);
            }
            mcr.setDiskWriteResult(false);
        }
    }
}
