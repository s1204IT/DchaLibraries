package com.android.providers.contacts;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PhotoStore {
    private static final Object MKDIRS_LOCK = new Object();
    private final ContactsDatabaseHelper mDatabaseHelper;
    private SQLiteDatabase mDb;
    private final Map<Long, Entry> mEntries;
    private final File mStorePath;
    private final String TAG = PhotoStore.class.getSimpleName();
    private final String DIRECTORY = "photos";
    private long mTotalSize = 0;

    public PhotoStore(File rootDirectory, ContactsDatabaseHelper databaseHelper) {
        this.mStorePath = new File(rootDirectory, "photos");
        synchronized (MKDIRS_LOCK) {
            if (!this.mStorePath.exists() && !this.mStorePath.mkdirs()) {
                throw new RuntimeException("Unable to create photo storage directory " + this.mStorePath.getPath());
            }
        }
        this.mDatabaseHelper = databaseHelper;
        this.mEntries = new HashMap();
        initialize();
    }

    public void clear() {
        File[] files = this.mStorePath.listFiles();
        if (files != null) {
            for (File file : files) {
                cleanupFile(file);
            }
        }
        if (this.mDb == null) {
            this.mDb = this.mDatabaseHelper.getWritableDatabase();
        }
        this.mDb.delete("photo_files", null, null);
        this.mEntries.clear();
        this.mTotalSize = 0L;
    }

    public long getTotalSize() {
        return this.mTotalSize;
    }

    public Entry get(long key) {
        return this.mEntries.get(Long.valueOf(key));
    }

    public final void initialize() {
        File[] files = this.mStorePath.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    Entry entry = new Entry(file);
                    putEntry(entry.id, entry);
                } catch (NumberFormatException e) {
                    cleanupFile(file);
                }
            }
            this.mDb = this.mDatabaseHelper.getWritableDatabase();
        }
    }

    public Set<Long> cleanup(Set<Long> keysInUse) {
        Set<Long> keysToRemove = new HashSet<>();
        keysToRemove.addAll(this.mEntries.keySet());
        keysToRemove.removeAll(keysInUse);
        if (!keysToRemove.isEmpty()) {
            Log.d(this.TAG, "cleanup removing " + keysToRemove.size() + " entries");
            Iterator<Long> it = keysToRemove.iterator();
            while (it.hasNext()) {
                long key = it.next().longValue();
                remove(key);
            }
        }
        Set<Long> missingKeys = new HashSet<>();
        missingKeys.addAll(keysInUse);
        missingKeys.removeAll(this.mEntries.keySet());
        return missingKeys;
    }

    public long insert(PhotoProcessor photoProcessor) {
        return insert(photoProcessor, false);
    }

    public long insert(PhotoProcessor photoProcessor, boolean allowSmallImageStorage) {
        Bitmap displayPhoto = photoProcessor.getDisplayPhoto();
        int width = displayPhoto.getWidth();
        int height = displayPhoto.getHeight();
        int thumbnailDim = photoProcessor.getMaxThumbnailPhotoDim();
        if (allowSmallImageStorage || width > thumbnailDim || height > thumbnailDim) {
            File file = null;
            try {
                byte[] photoBytes = photoProcessor.getDisplayPhotoBytes();
                file = File.createTempFile("img", null, this.mStorePath);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(photoBytes);
                fos.close();
                ContentValues values = new ContentValues();
                values.put("height", Integer.valueOf(height));
                values.put("width", Integer.valueOf(width));
                values.put("filesize", Integer.valueOf(photoBytes.length));
                long id = this.mDb.insert("photo_files", null, values);
                if (id != 0) {
                    File target = getFileForPhotoFileId(id);
                    if (file.renameTo(target)) {
                        Entry entry = new Entry(target);
                        putEntry(entry.id, entry);
                        return id;
                    }
                }
            } catch (IOException e) {
            }
            if (file != null) {
                cleanupFile(file);
            }
        }
        return 0L;
    }

    private void cleanupFile(File file) {
        boolean deleted = file.delete();
        if (!deleted) {
            Log.d("Could not clean up file %s", file.getAbsolutePath());
        }
    }

    public void remove(long id) {
        cleanupFile(getFileForPhotoFileId(id));
        removeEntry(id);
    }

    private File getFileForPhotoFileId(long id) {
        return new File(this.mStorePath, String.valueOf(id));
    }

    private void putEntry(long id, Entry entry) {
        if (!this.mEntries.containsKey(Long.valueOf(id))) {
            this.mTotalSize += entry.size;
        } else {
            Entry oldEntry = this.mEntries.get(Long.valueOf(id));
            this.mTotalSize += entry.size - oldEntry.size;
        }
        this.mEntries.put(Long.valueOf(id), entry);
    }

    private void removeEntry(long id) {
        Entry entry = this.mEntries.get(Long.valueOf(id));
        if (entry != null) {
            this.mTotalSize -= entry.size;
            this.mEntries.remove(Long.valueOf(id));
        }
        this.mDb.delete("photo_files", "photo_files._id=?", new String[]{String.valueOf(id)});
    }

    public static class Entry {
        public final long id;
        public final String path;
        public final long size;

        public Entry(File file) {
            this.id = Long.parseLong(file.getName());
            this.size = file.length();
            this.path = file.getAbsolutePath();
        }
    }
}
