package com.android.gallery3d.onetimeinitializer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.gadget.WidgetDatabaseHelper;
import com.android.gallery3d.util.GalleryUtils;
import com.mediatek.gallery3d.util.Log;
import java.io.File;
import java.util.HashMap;
import java.util.List;

public class GalleryWidgetMigrator {
    private static final String NEW_EXT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final int RELATIVE_PATH_START = NEW_EXT_PATH.length();

    public static void migrateGalleryWidgets(Context context) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (NEW_EXT_PATH.equals(defaultSharedPreferences.getString("external_storage_path", null))) {
            return;
        }
        try {
            migrateGalleryWidgetsInternal(context);
            defaultSharedPreferences.edit().putString("external_storage_path", NEW_EXT_PATH).commit();
        } catch (Throwable th) {
            Log.w("Gallery2/GalleryWidgetMigrator", "migrateGalleryWidgets", th);
        }
    }

    private static void migrateGalleryWidgetsInternal(Context context) throws Throwable {
        DataManager dataManager = ((GalleryApp) context.getApplicationContext()).getDataManager();
        WidgetDatabaseHelper widgetDatabaseHelper = new WidgetDatabaseHelper(context);
        List<WidgetDatabaseHelper.Entry> entries = widgetDatabaseHelper.getEntries(2);
        if (entries == null) {
            return;
        }
        HashMap map = new HashMap(entries.size());
        for (WidgetDatabaseHelper.Entry entry : entries) {
            Path pathFromString = Path.fromString(entry.albumPath);
            if (((MediaSet) dataManager.getMediaObject(pathFromString)) instanceof LocalAlbum) {
                if (entry.relativePath != null && entry.relativePath.length() > 0) {
                    updateEntryUsingRelativePath(entry, widgetDatabaseHelper);
                } else {
                    map.put(Integer.valueOf(Integer.parseInt(pathFromString.getSuffix())), entry);
                }
            }
        }
        if (!map.isEmpty()) {
            migrateLocalEntries(context, (HashMap<Integer, WidgetDatabaseHelper.Entry>) map, widgetDatabaseHelper);
        }
    }

    private static void migrateLocalEntries(Context context, HashMap<Integer, WidgetDatabaseHelper.Entry> map, WidgetDatabaseHelper widgetDatabaseHelper) {
        String string = PreferenceManager.getDefaultSharedPreferences(context).getString("external_storage_path", null);
        if (string != null) {
            migrateLocalEntries(map, widgetDatabaseHelper, string);
            return;
        }
        migrateLocalEntries(map, widgetDatabaseHelper, "/mnt/sdcard");
        if (!map.isEmpty() && Build.VERSION.SDK_INT > 16) {
            migrateLocalEntries(map, widgetDatabaseHelper, "/storage/sdcard0");
        }
    }

    private static void migrateLocalEntries(HashMap<Integer, WidgetDatabaseHelper.Entry> map, WidgetDatabaseHelper widgetDatabaseHelper, String str) {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        updatePath(new File(externalStorageDirectory, "DCIM"), map, widgetDatabaseHelper, str);
        if (!map.isEmpty()) {
            updatePath(externalStorageDirectory, map, widgetDatabaseHelper, str);
        }
    }

    private static void updatePath(File file, HashMap<Integer, WidgetDatabaseHelper.Entry> map, WidgetDatabaseHelper widgetDatabaseHelper, String str) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (File file2 : fileArrListFiles) {
                if (file2.isDirectory() && !map.isEmpty()) {
                    String absolutePath = file2.getAbsolutePath();
                    WidgetDatabaseHelper.Entry entryRemove = map.remove(Integer.valueOf(GalleryUtils.getBucketId(str + absolutePath.substring(RELATIVE_PATH_START))));
                    if (entryRemove != null) {
                        String string = Path.fromString(entryRemove.albumPath).getParent().getChild(GalleryUtils.getBucketId(absolutePath)).toString();
                        Log.d("Gallery2/GalleryWidgetMigrator", "migrate from " + entryRemove.albumPath + " to " + string);
                        entryRemove.albumPath = string;
                        entryRemove.relativePath = absolutePath.substring(RELATIVE_PATH_START);
                        widgetDatabaseHelper.updateEntry(entryRemove);
                    }
                    updatePath(file2, map, widgetDatabaseHelper, str);
                }
            }
        }
    }

    private static void updateEntryUsingRelativePath(WidgetDatabaseHelper.Entry entry, WidgetDatabaseHelper widgetDatabaseHelper) {
        entry.albumPath = Path.fromString(entry.albumPath).getParent().getChild(GalleryUtils.getBucketId(NEW_EXT_PATH + entry.relativePath)).toString();
        widgetDatabaseHelper.updateEntry(entry);
    }
}
