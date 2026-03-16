package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ContactDirectoryManager {
    private final ContactsProvider2 mContactsProvider;
    private final Context mContext;
    private final PackageManager mPackageManager;

    private static final class DirectoryQuery {
        public static final String[] PROJECTION = {"accountName", "accountType", "displayName", "typeResourceId", "exportSupport", "shortcutSupport", "photoSupport"};
    }

    public static class DirectoryInfo {
        String accountName;
        String accountType;
        String authority;
        String displayName;
        long id;
        String packageName;
        int typeResourceId;
        int exportSupport = 0;
        int shortcutSupport = 0;
        int photoSupport = 0;

        public String toString() {
            return "DirectoryInfo:id=" + this.id + " packageName=" + this.accountType + " authority=" + this.authority + " accountName=*** accountType=" + this.accountType;
        }
    }

    public ContactDirectoryManager(ContactsProvider2 contactsProvider) {
        this.mContactsProvider = contactsProvider;
        this.mContext = contactsProvider.getContext();
        this.mPackageManager = this.mContext.getPackageManager();
    }

    public ContactsDatabaseHelper getDbHelper() {
        return (ContactsDatabaseHelper) this.mContactsProvider.getDatabaseHelper();
    }

    public void scanPackagesByUid(int callingUid) {
        String[] callerPackages = this.mPackageManager.getPackagesForUid(callingUid);
        if (callerPackages != null) {
            for (String str : callerPackages) {
                onPackageChanged(str);
            }
        }
    }

    private boolean areTypeResourceIdsValid() {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        Cursor cursor = db.query("directories", new String[]{"typeResourceId", "packageName", "typeResourceName"}, null, null, null, null, null);
        while (cursor.moveToNext()) {
            try {
                int resourceId = cursor.getInt(0);
                if (resourceId != 0) {
                    String packageName = cursor.getString(1);
                    String storedResourceName = cursor.getString(2);
                    String resourceName = getResourceNameById(packageName, resourceId);
                    if (!TextUtils.equals(storedResourceName, resourceName)) {
                        return false;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return true;
    }

    private String getResourceNameById(String packageName, int resourceId) {
        try {
            Resources resources = this.mPackageManager.getResourcesForApplication(packageName);
            return resources.getResourceName(resourceId);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        } catch (Resources.NotFoundException e2) {
            return null;
        }
    }

    public void scanAllPackages(boolean rescan) {
        if (rescan || !areTypeResourceIdsValid()) {
            getDbHelper().setProperty("directoryScanComplete", "0");
        }
        scanAllPackagesIfNeeded();
    }

    private void scanAllPackagesIfNeeded() {
        String scanComplete = getDbHelper().getProperty("directoryScanComplete", "0");
        if ("0".equals(scanComplete)) {
            long start = SystemClock.elapsedRealtime();
            int count = scanAllPackages();
            getDbHelper().setProperty("directoryScanComplete", "1");
            long end = SystemClock.elapsedRealtime();
            Log.i("ContactDirectoryManager", "Discovered " + count + " contact directories in " + (end - start) + "ms");
            this.mContactsProvider.notifyChange(false);
        }
    }

    static boolean isDirectoryProvider(ProviderInfo provider) {
        Bundle metaData;
        Object trueFalse;
        return (provider == null || (metaData = provider.metaData) == null || (trueFalse = metaData.get("android.content.ContactDirectory")) == null || !Boolean.TRUE.equals(trueFalse)) ? false : true;
    }

    static Set<String> getDirectoryProviderPackages(PackageManager pm) {
        Set<String> ret = Sets.newHashSet();
        List<PackageInfo> packages = pm.getInstalledPackages(136);
        if (packages != null) {
            for (PackageInfo packageInfo : packages) {
                if (packageInfo.providers != null) {
                    ProviderInfo[] arr$ = packageInfo.providers;
                    for (ProviderInfo provider : arr$) {
                        if (isDirectoryProvider(provider)) {
                            Log.d("ContactDirectoryManager", "Found " + provider.authority);
                            ret.add(provider.packageName);
                        }
                    }
                }
            }
        }
        return ret;
    }

    int scanAllPackages() {
        List<DirectoryInfo> directories;
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        insertDefaultDirectory(db);
        insertLocalInvisibleDirectory(db);
        int count = 0;
        StringBuilder deleteWhereBuilder = new StringBuilder();
        ArrayList<String> deleteWhereArgs = new ArrayList<>();
        deleteWhereBuilder.append("NOT (_id=? OR _id=?");
        deleteWhereArgs.add(String.valueOf(0L));
        deleteWhereArgs.add(String.valueOf(1L));
        for (String packageName : getDirectoryProviderPackages(this.mPackageManager)) {
            if (this.mContext.getPackageName().equals(packageName)) {
                Log.w("ContactDirectoryManager", "  skipping self");
            } else {
                try {
                    PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 136);
                    if (packageInfo != null && (directories = updateDirectoriesForPackage(packageInfo, true)) != null && !directories.isEmpty()) {
                        count += directories.size();
                        for (DirectoryInfo info : directories) {
                            deleteWhereBuilder.append(" OR ");
                            deleteWhereBuilder.append("(packageName=? AND authority=? AND accountName=? AND accountType=?)");
                            deleteWhereArgs.add(info.packageName);
                            deleteWhereArgs.add(info.authority);
                            deleteWhereArgs.add(info.accountName);
                            deleteWhereArgs.add(info.accountType);
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        deleteWhereBuilder.append(")");
        int deletedRows = db.delete("directories", deleteWhereBuilder.toString(), (String[]) deleteWhereArgs.toArray(new String[0]));
        Log.i("ContactDirectoryManager", "deleted " + deletedRows + " stale rows which don't have any relevant directory");
        return count;
    }

    private void insertDefaultDirectory(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("_id", (Long) 0L);
        values.put("packageName", this.mContext.getApplicationInfo().packageName);
        values.put("authority", "com.android.contacts");
        values.put("typeResourceId", Integer.valueOf(R.string.default_directory));
        values.put("typeResourceName", this.mContext.getResources().getResourceName(R.string.default_directory));
        values.put("exportSupport", (Integer) 0);
        values.put("shortcutSupport", (Integer) 2);
        values.put("photoSupport", (Integer) 3);
        db.replace("directories", null, values);
    }

    private void insertLocalInvisibleDirectory(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("_id", (Long) 1L);
        values.put("packageName", this.mContext.getApplicationInfo().packageName);
        values.put("authority", "com.android.contacts");
        values.put("typeResourceId", Integer.valueOf(R.string.local_invisible_directory));
        values.put("typeResourceName", this.mContext.getResources().getResourceName(R.string.local_invisible_directory));
        values.put("exportSupport", (Integer) 0);
        values.put("shortcutSupport", (Integer) 2);
        values.put("photoSupport", (Integer) 3);
        db.replace("directories", null, values);
    }

    public void onPackageChanged(String packageName) {
        PackageInfo packageInfo;
        try {
            packageInfo = this.mPackageManager.getPackageInfo(packageName, 136);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = new PackageInfo();
            packageInfo.packageName = packageName;
        }
        if (!this.mContext.getPackageName().equals(packageInfo.packageName)) {
            updateDirectoriesForPackage(packageInfo, false);
        }
    }

    private List<DirectoryInfo> updateDirectoriesForPackage(PackageInfo packageInfo, boolean initialScan) {
        ArrayList<DirectoryInfo> directories = Lists.newArrayList();
        ProviderInfo[] providers = packageInfo.providers;
        if (providers != null) {
            for (ProviderInfo provider : providers) {
                if (isDirectoryProvider(provider)) {
                    queryDirectoriesForAuthority(directories, provider);
                }
            }
        }
        if (directories.size() == 0 && initialScan) {
            return null;
        }
        SQLiteDatabase db = getDbHelper().getWritableDatabase();
        db.beginTransaction();
        try {
            updateDirectories(db, directories);
            StringBuilder sb = new StringBuilder("packageName=?");
            if (!directories.isEmpty()) {
                sb.append(" AND _id NOT IN(");
                for (DirectoryInfo info : directories) {
                    sb.append(info.id).append(",");
                }
                sb.setLength(sb.length() - 1);
                sb.append(")");
            }
            db.delete("directories", sb.toString(), new String[]{packageInfo.packageName});
            db.setTransactionSuccessful();
            db.endTransaction();
            this.mContactsProvider.resetDirectoryCache();
            return directories;
        } catch (Throwable th) {
            db.endTransaction();
            throw th;
        }
    }

    protected void queryDirectoriesForAuthority(ArrayList<DirectoryInfo> directoryInfo, ProviderInfo provider) {
        Uri uri = new Uri.Builder().scheme("content").authority(provider.authority).appendPath("directories").build();
        Cursor cursor = null;
        try {
            try {
                Cursor cursor2 = this.mContext.getContentResolver().query(uri, DirectoryQuery.PROJECTION, null, null, null);
                if (cursor2 == null) {
                    Log.i("ContactDirectoryManager", providerDescription(provider) + " returned a NULL cursor.");
                } else {
                    while (cursor2.moveToNext()) {
                        DirectoryInfo info = new DirectoryInfo();
                        info.packageName = provider.packageName;
                        info.authority = provider.authority;
                        info.accountName = cursor2.getString(0);
                        info.accountType = cursor2.getString(1);
                        info.displayName = cursor2.getString(2);
                        if (!cursor2.isNull(3)) {
                            info.typeResourceId = cursor2.getInt(3);
                        }
                        if (!cursor2.isNull(4)) {
                            int exportSupport = cursor2.getInt(4);
                            switch (exportSupport) {
                                case 0:
                                case 1:
                                case 2:
                                    info.exportSupport = exportSupport;
                                    break;
                                default:
                                    Log.e("ContactDirectoryManager", providerDescription(provider) + " - invalid export support flag: " + exportSupport);
                                    break;
                            }
                        }
                        if (!cursor2.isNull(5)) {
                            int shortcutSupport = cursor2.getInt(5);
                            switch (shortcutSupport) {
                                case 0:
                                case 1:
                                case 2:
                                    info.shortcutSupport = shortcutSupport;
                                    break;
                                default:
                                    Log.e("ContactDirectoryManager", providerDescription(provider) + " - invalid shortcut support flag: " + shortcutSupport);
                                    break;
                            }
                        }
                        if (!cursor2.isNull(6)) {
                            int photoSupport = cursor2.getInt(6);
                            switch (photoSupport) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                    info.photoSupport = photoSupport;
                                    break;
                                default:
                                    Log.e("ContactDirectoryManager", providerDescription(provider) + " - invalid photo support flag: " + photoSupport);
                                    break;
                            }
                        }
                        directoryInfo.add(info);
                    }
                }
                if (cursor2 != null) {
                    cursor2.close();
                }
            } catch (Throwable t) {
                Log.e("ContactDirectoryManager", providerDescription(provider) + " exception", t);
                if (0 != 0) {
                    cursor.close();
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                cursor.close();
            }
            throw th;
        }
    }

    private void updateDirectories(SQLiteDatabase db, ArrayList<DirectoryInfo> directoryInfo) {
        long id;
        for (DirectoryInfo info : directoryInfo) {
            ContentValues values = new ContentValues();
            values.put("packageName", info.packageName);
            values.put("authority", info.authority);
            values.put("accountName", info.accountName);
            values.put("accountType", info.accountType);
            values.put("typeResourceId", Integer.valueOf(info.typeResourceId));
            values.put("displayName", info.displayName);
            values.put("exportSupport", Integer.valueOf(info.exportSupport));
            values.put("shortcutSupport", Integer.valueOf(info.shortcutSupport));
            values.put("photoSupport", Integer.valueOf(info.photoSupport));
            if (info.typeResourceId != 0) {
                String resourceName = getResourceNameById(info.packageName, info.typeResourceId);
                values.put("typeResourceName", resourceName);
            }
            Cursor cursor = db.query("directories", new String[]{"_id"}, "packageName=? AND authority=? AND accountName=? AND accountType=?", new String[]{info.packageName, info.authority, info.accountName, info.accountType}, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    id = cursor.getLong(0);
                    db.update("directories", values, "_id=?", new String[]{String.valueOf(id)});
                } else {
                    id = db.insert("directories", null, values);
                }
                info.id = id;
            } finally {
                cursor.close();
            }
        }
    }

    protected String providerDescription(ProviderInfo provider) {
        return "Directory provider " + provider.packageName + "(" + provider.authority + ")";
    }
}
