package android.test;

import android.content.ContentProvider;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.FileUtils;
import android.util.Log;
import com.google.android.collect.Sets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Set;

@Deprecated
public class RenamingDelegatingContext extends ContextWrapper {
    private File mCacheDir;
    private Set<String> mDatabaseNames;
    private Context mFileContext;
    private Set<String> mFileNames;
    private String mFilePrefix;
    private final Object mSync;

    public static <T extends ContentProvider> T providerWithRenamedContext(Class<T> cls, Context context, String str) throws IllegalAccessException, InstantiationException {
        return (T) providerWithRenamedContext(cls, context, str, false);
    }

    public static <T extends ContentProvider> T providerWithRenamedContext(Class<T> contentProvider, Context c, String filePrefix, boolean allowAccessToExistingFilesAndDbs) throws IllegalAccessException, InstantiationException {
        T mProvider = contentProvider.newInstance();
        RenamingDelegatingContext mContext = new RenamingDelegatingContext(c, filePrefix);
        if (allowAccessToExistingFilesAndDbs) {
            mContext.makeExistingFilesAndDbsAccessible();
        }
        mProvider.attachInfoForTesting(mContext, null);
        return mProvider;
    }

    public void makeExistingFilesAndDbsAccessible() {
        String[] databaseList = this.mFileContext.databaseList();
        for (String diskName : databaseList) {
            if (shouldDiskNameBeVisible(diskName)) {
                this.mDatabaseNames.add(publicNameFromDiskName(diskName));
            }
        }
        String[] fileList = this.mFileContext.fileList();
        for (String diskName2 : fileList) {
            if (shouldDiskNameBeVisible(diskName2)) {
                this.mFileNames.add(publicNameFromDiskName(diskName2));
            }
        }
    }

    boolean shouldDiskNameBeVisible(String diskName) {
        return diskName.startsWith(this.mFilePrefix);
    }

    String publicNameFromDiskName(String diskName) {
        if (!shouldDiskNameBeVisible(diskName)) {
            throw new IllegalArgumentException("disk file should not be visible: " + diskName);
        }
        return diskName.substring(this.mFilePrefix.length(), diskName.length());
    }

    public RenamingDelegatingContext(Context context, String filePrefix) {
        super(context);
        this.mFilePrefix = null;
        this.mSync = new Object();
        this.mDatabaseNames = Sets.newHashSet();
        this.mFileNames = Sets.newHashSet();
        this.mFileContext = context;
        this.mFilePrefix = filePrefix;
    }

    public RenamingDelegatingContext(Context context, Context fileContext, String filePrefix) {
        super(context);
        this.mFilePrefix = null;
        this.mSync = new Object();
        this.mDatabaseNames = Sets.newHashSet();
        this.mFileNames = Sets.newHashSet();
        this.mFileContext = fileContext;
        this.mFilePrefix = filePrefix;
    }

    public String getDatabasePrefix() {
        return this.mFilePrefix;
    }

    private String renamedFileName(String name) {
        return this.mFilePrefix + name;
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        String internalName = renamedFileName(name);
        if (!this.mDatabaseNames.contains(name)) {
            this.mDatabaseNames.add(name);
            this.mFileContext.deleteDatabase(internalName);
        }
        return this.mFileContext.openOrCreateDatabase(internalName, mode, factory);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        String internalName = renamedFileName(name);
        if (!this.mDatabaseNames.contains(name)) {
            this.mDatabaseNames.add(name);
            this.mFileContext.deleteDatabase(internalName);
        }
        return this.mFileContext.openOrCreateDatabase(internalName, mode, factory, errorHandler);
    }

    @Override
    public boolean deleteDatabase(String name) {
        if (this.mDatabaseNames.contains(name)) {
            this.mDatabaseNames.remove(name);
            return this.mFileContext.deleteDatabase(renamedFileName(name));
        }
        return false;
    }

    @Override
    public File getDatabasePath(String name) {
        return this.mFileContext.getDatabasePath(renamedFileName(name));
    }

    @Override
    public String[] databaseList() {
        return (String[]) this.mDatabaseNames.toArray(new String[0]);
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        String internalName = renamedFileName(name);
        if (this.mFileNames.contains(name)) {
            return this.mFileContext.openFileInput(internalName);
        }
        throw new FileNotFoundException(internalName);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        this.mFileNames.add(name);
        return this.mFileContext.openFileOutput(renamedFileName(name), mode);
    }

    @Override
    public File getFileStreamPath(String name) {
        return this.mFileContext.getFileStreamPath(renamedFileName(name));
    }

    @Override
    public boolean deleteFile(String name) {
        if (this.mFileNames.contains(name)) {
            this.mFileNames.remove(name);
            return this.mFileContext.deleteFile(renamedFileName(name));
        }
        return false;
    }

    @Override
    public String[] fileList() {
        return (String[]) this.mFileNames.toArray(new String[0]);
    }

    @Override
    public File getCacheDir() {
        synchronized (this.mSync) {
            if (this.mCacheDir == null) {
                this.mCacheDir = new File(this.mFileContext.getCacheDir(), renamedFileName("cache"));
            }
            if (!this.mCacheDir.exists()) {
                if (!this.mCacheDir.mkdirs()) {
                    Log.w("RenamingDelegatingContext", "Unable to create cache directory");
                    return null;
                }
                FileUtils.setPermissions(this.mCacheDir.getPath(), 505, -1, -1);
            }
            return this.mCacheDir;
        }
    }
}
