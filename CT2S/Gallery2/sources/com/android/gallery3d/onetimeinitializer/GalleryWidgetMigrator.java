package com.android.gallery3d.onetimeinitializer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.gadget.WidgetDatabaseHelper;
import com.android.gallery3d.util.GalleryUtils;
import java.io.File;
import java.util.HashMap;
import java.util.List;

public class GalleryWidgetMigrator {
    private static final String NEW_EXT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final int RELATIVE_PATH_START = NEW_EXT_PATH.length();

    public static void migrateGalleryWidgets(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String extPath = prefs.getString("external_storage_path", null);
        boolean isDone = NEW_EXT_PATH.equals(extPath);
        if (!isDone) {
            try {
                migrateGalleryWidgetsInternal(context);
                prefs.edit().putString("external_storage_path", NEW_EXT_PATH).commit();
            } catch (Throwable t) {
                Log.w("GalleryWidgetMigrator", "migrateGalleryWidgets", t);
            }
        }
    }

    private static void migrateGalleryWidgetsInternal(Context context) {
        GalleryApp galleryApp = (GalleryApp) context.getApplicationContext();
        DataManager manager = galleryApp.getDataManager();
        WidgetDatabaseHelper dbHelper = new WidgetDatabaseHelper(context);
        List<WidgetDatabaseHelper.Entry> entries = dbHelper.getEntries(2);
        if (entries != null) {
            HashMap<Integer, WidgetDatabaseHelper.Entry> localEntries = new HashMap<>(entries.size());
            for (WidgetDatabaseHelper.Entry entry : entries) {
                Path path = Path.fromString(entry.albumPath);
                MediaSet mediaSet = (MediaSet) manager.getMediaObject(path);
                if (mediaSet instanceof LocalAlbum) {
                    if (entry.relativePath != null && entry.relativePath.length() > 0) {
                        updateEntryUsingRelativePath(entry, dbHelper);
                    } else {
                        int bucketId = Integer.parseInt(path.getSuffix());
                        localEntries.put(Integer.valueOf(bucketId), entry);
                    }
                }
            }
            if (!localEntries.isEmpty()) {
                migrateLocalEntries(context, localEntries, dbHelper);
            }
        }
    }

    private static void migrateLocalEntries(Context context, HashMap<Integer, WidgetDatabaseHelper.Entry> entries, WidgetDatabaseHelper dbHelper) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String oldExtPath = prefs.getString("external_storage_path", null);
        if (oldExtPath != null) {
            migrateLocalEntries(entries, dbHelper, oldExtPath);
            return;
        }
        migrateLocalEntries(entries, dbHelper, "/mnt/sdcard");
        if (!entries.isEmpty() && Build.VERSION.SDK_INT > 16) {
            migrateLocalEntries(entries, dbHelper, "/storage/sdcard0");
        }
    }

    private static void migrateLocalEntries(HashMap<Integer, WidgetDatabaseHelper.Entry> entries, WidgetDatabaseHelper dbHelper, String oldExtPath) {
        File root = Environment.getExternalStorageDirectory();
        updatePath(new File(root, "DCIM"), entries, dbHelper, oldExtPath);
        if (!entries.isEmpty()) {
            updatePath(root, entries, dbHelper, oldExtPath);
        }
    }

    private static void updatePath(File root, HashMap<Integer, WidgetDatabaseHelper.Entry> entries, WidgetDatabaseHelper dbHelper, String oldExtStorage) {
        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && !entries.isEmpty()) {
                    String path = file.getAbsolutePath();
                    String oldPath = oldExtStorage + path.substring(RELATIVE_PATH_START);
                    int oldBucketId = GalleryUtils.getBucketId(oldPath);
                    WidgetDatabaseHelper.Entry entry = entries.remove(Integer.valueOf(oldBucketId));
                    if (entry != null) {
                        int newBucketId = GalleryUtils.getBucketId(path);
                        String newAlbumPath = Path.fromString(entry.albumPath).getParent().getChild(newBucketId).toString();
                        Log.d("GalleryWidgetMigrator", "migrate from " + entry.albumPath + " to " + newAlbumPath);
                        entry.albumPath = newAlbumPath;
                        entry.relativePath = path.substring(RELATIVE_PATH_START);
                        dbHelper.updateEntry(entry);
                    }
                    updatePath(file, entries, dbHelper, oldExtStorage);
                }
            }
        }
    }

    private static void updateEntryUsingRelativePath(WidgetDatabaseHelper.Entry entry, WidgetDatabaseHelper dbHelper) {
        String newPath = NEW_EXT_PATH + entry.relativePath;
        int newBucketId = GalleryUtils.getBucketId(newPath);
        String newAlbumPath = Path.fromString(entry.albumPath).getParent().getChild(newBucketId).toString();
        entry.albumPath = newAlbumPath;
        dbHelper.updateEntry(entry);
    }
}
