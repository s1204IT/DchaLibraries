package com.android.documentsui;

import android.app.ActivityManager;
import android.app.GrantedUriPermission;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.ArraySet;
import android.util.Log;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.prefs.ScopedAccessLocalPreferences;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ScopedAccessProvider extends ContentProvider {
    private static final UriMatcher sMatcher = new UriMatcher(-1);

    static {
        sMatcher.addURI("com.android.documentsui.scopedAccess", "packages/*", 1);
        sMatcher.addURI("com.android.documentsui.scopedAccess", "permissions/*", 2);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        if (SharedMinimal.DEBUG) {
            Log.v("ScopedAccessProvider", "query(" + uri + "): proj=" + Arrays.toString(strArr) + ", sel=" + str);
        }
        switch (sMatcher.match(uri)) {
            case 1:
                return getPackagesCursor();
            case 2:
                if (ArrayUtils.isEmpty(strArr2)) {
                    throw new UnsupportedOperationException("selections cannot be empty");
                }
                if (strArr2.length > 1) {
                    Log.w("ScopedAccessProvider", "Using just first entry of " + Arrays.toString(strArr2));
                }
                return getPermissionsCursor(strArr2[0]);
            default:
                throw new UnsupportedOperationException("Unsupported Uri " + uri);
        }
    }

    private Cursor getPackagesCursor() {
        Context context = getContext();
        final Set<String> allPackages = ScopedAccessLocalPreferences.getAllPackages(context);
        List list = ((ActivityManager) context.getSystemService("activity")).getGrantedUriPermissions(null).getList();
        if (!list.isEmpty()) {
            list.forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    allPackages.add(((GrantedUriPermission) obj).packageName);
                }
            });
        }
        if (ArrayUtils.isEmpty(allPackages)) {
            if (SharedMinimal.DEBUG) {
                Log.v("ScopedAccessProvider", "getPackagesCursor(): nothing to do");
            }
            return null;
        }
        if (SharedMinimal.DEBUG) {
            Log.v("ScopedAccessProvider", "getPackagesCursor(): denied=" + allPackages + ", granted=" + list);
        }
        final MatrixCursor matrixCursor = new MatrixCursor(StorageVolume.ScopedAccessProviderContract.TABLE_PACKAGES_COLUMNS, allPackages.size());
        allPackages.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                matrixCursor.addRow(new Object[]{(String) obj});
            }
        });
        return matrixCursor;
    }

    private Cursor getPermissionsCursor(String str) {
        Context context = getContext();
        ArraySet arraySet = new ArraySet();
        HashMap map = new HashMap();
        ArrayList arrayList = new ArrayList();
        List<GrantedUriPermission> list = ((ActivityManager) context.getSystemService("activity")).getGrantedUriPermissions(str).getList();
        if (SharedMinimal.DEBUG) {
            Log.v("ScopedAccessProvider", "am returned =" + list);
        }
        setGrantedPermissions(str, list, arrayList, arraySet, map);
        List<ScopedAccessLocalPreferences.Permission> allPermissions = ScopedAccessLocalPreferences.getAllPermissions(context);
        if (SharedMinimal.DEBUG) {
            Log.v("ScopedAccessProvider", "rawPermissions: " + allPermissions);
        }
        for (ScopedAccessLocalPreferences.Permission permission : allPermissions) {
            if (!str.equals(permission.pkg)) {
                if (SharedMinimal.DEBUG) {
                    Log.v("ScopedAccessProvider", "ignoring " + permission + " because package is not " + str);
                }
            } else if (permission.status != -1 && permission.status != 1) {
                if (SharedMinimal.DEBUG) {
                    Log.v("ScopedAccessProvider", "ignoring " + permission + " because of its status");
                }
            } else if (arraySet.contains(permission.uuid)) {
                if (SharedMinimal.DEBUG) {
                    Log.v("ScopedAccessProvider", "ignoring " + permission + " because whole volume is granted");
                }
            } else {
                Set<String> set = map.get(permission.uuid);
                if (set != null && set.contains(permission.directory)) {
                    Log.w("ScopedAccessProvider", "ignoring " + permission + " because it was granted already");
                } else {
                    arrayList.add(new Object[]{str, permission.uuid, SharedMinimal.getExternalDirectoryName(permission.directory), 0});
                }
            }
        }
        if (SharedMinimal.DEBUG) {
            Log.v("ScopedAccessProvider", "total permissions: " + arrayList.size());
        }
        final MatrixCursor matrixCursor = new MatrixCursor(StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COLUMNS, arrayList.size());
        arrayList.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                matrixCursor.addRow((Object[]) obj);
            }
        });
        return matrixCursor;
    }

    private void setGrantedPermissions(final String str, List<GrantedUriPermission> list, final List<Object[]> list2, final Set<String> set, Map<String, Set<String>> map) {
        for (ScopedAccessLocalPreferences.Permission permission : parseGrantedPermissions(list)) {
            if (permission.directory == null) {
                if (permission.uuid == null) {
                    Log.w("ScopedAccessProvider", "ignoring entry whose uuid and directory is null");
                } else {
                    set.add(permission.uuid);
                }
            } else if (!ArrayUtils.contains(Environment.STANDARD_DIRECTORIES, permission.directory)) {
                if (SharedMinimal.DEBUG) {
                    Log.v("ScopedAccessProvider", "Ignoring non-standard directory on " + permission);
                }
            } else {
                Set<String> hashSet = map.get(permission.uuid);
                if (hashSet == null) {
                    hashSet = new HashSet<>(1);
                    map.put(permission.uuid, hashSet);
                }
                hashSet.add(permission.directory);
            }
        }
        if (SharedMinimal.DEBUG) {
            Log.v("ScopedAccessProvider", "grantedVolumes=" + set + ", grantedDirectories=" + map);
        }
        set.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                list2.add(new Object[]{str, (String) obj, null, 1});
            }
        });
        map.forEach(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ScopedAccessProvider.lambda$setGrantedPermissions$5(set, list2, str, (String) obj, (Set) obj2);
            }
        });
    }

    static void lambda$setGrantedPermissions$5(Set set, final List list, final String str, final String str2, Set set2) {
        if (set.contains(str2)) {
            Log.w("ScopedAccessProvider", "Ignoring individual grants to " + str2 + ": " + set2);
            return;
        }
        set2.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                list.add(new Object[]{str, str2, (String) obj, 1});
            }
        });
    }

    private List<ScopedAccessLocalPreferences.Permission> parseGrantedPermissions(List<GrantedUriPermission> list) {
        String str;
        ArrayList arrayList = new ArrayList(list.size());
        for (GrantedUriPermission grantedUriPermission : list) {
            Uri uri = grantedUriPermission.uri;
            if (!"com.android.externalstorage.documents".equals(uri.getAuthority())) {
                Log.w("ScopedAccessProvider", "Wrong authority on " + uri);
            } else {
                List<String> pathSegments = uri.getPathSegments();
                if (pathSegments.size() < 2) {
                    Log.w("ScopedAccessProvider", "wrong path segments on " + uri);
                } else if (!"tree".equals(pathSegments.get(0))) {
                    Log.w("ScopedAccessProvider", "wrong path tree on " + uri);
                } else {
                    String[] strArrSplit = pathSegments.get(1).split(":");
                    if (strArrSplit.length != 1 && strArrSplit.length != 2) {
                        Log.w("ScopedAccessProvider", "could not parse uuid and directory on " + uri);
                    } else {
                        String str2 = null;
                        if ("home".equals(strArrSplit[0])) {
                            str2 = Environment.DIRECTORY_DOCUMENTS;
                            str = null;
                        } else {
                            if (!"primary".equals(strArrSplit[0])) {
                                str = strArrSplit[0];
                            } else {
                                str = null;
                            }
                            if (strArrSplit.length != 1) {
                                str2 = strArrSplit[1];
                            }
                        }
                        arrayList.add(new ScopedAccessLocalPreferences.Permission(grantedUriPermission.packageName, str, str2, 2));
                    }
                }
            }
        }
        return arrayList;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException("insert(): unsupported " + uri);
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        if (sMatcher.match(uri) != 2) {
            throw new UnsupportedOperationException("delete(): unsupported " + uri);
        }
        if (SharedMinimal.DEBUG) {
            Log.v("ScopedAccessProvider", "delete(" + uri + "): " + Arrays.toString(strArr));
        }
        Preconditions.checkArgument(strArr != null && strArr.length == 1, "Must have exactly 1 args: package_name" + Arrays.toString(strArr));
        return ScopedAccessLocalPreferences.clearScopedAccessPreferences(getContext(), strArr[0]);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        if (sMatcher.match(uri) != 2) {
            throw new UnsupportedOperationException("update(): unsupported " + uri);
        }
        if (SharedMinimal.DEBUG) {
            Log.v("ScopedAccessProvider", "update(" + uri + "): " + Arrays.toString(strArr) + " = " + contentValues);
        }
        Preconditions.checkArgument(strArr != null && strArr.length == 3, "Must have exactly 3 args: package_name, (nullable) uuid, (nullable) directory: " + Arrays.toString(strArr));
        String str2 = strArr[0];
        String str3 = strArr[1];
        String str4 = strArr[2];
        boolean zBooleanValue = contentValues.getAsBoolean("granted").booleanValue();
        if (!persistUriPermission(str2, str3, str4, zBooleanValue)) {
            return 0;
        }
        ScopedAccessLocalPreferences.setScopedAccessPermissionStatus(getContext(), str2, str3, SharedMinimal.getInternalDirectoryName(str4), zBooleanValue ? 2 : -1);
        return 1;
    }

    private boolean persistUriPermission(final String str, String str2, String str3, final boolean z) {
        StorageVolume storageVolume;
        StorageVolume primaryStorageVolume;
        final Context context = getContext();
        ContentProviderClient contentProviderClientAcquireContentProviderClient = context.getContentResolver().acquireContentProviderClient("com.android.externalstorage.documents");
        StorageManager storageManager = (StorageManager) context.getSystemService(StorageManager.class);
        if (str2 == null) {
            if (str3 == null) {
                Log.w("ScopedAccessProvider", "cannot grant full access to the primary volume");
                return false;
            }
            primaryStorageVolume = storageManager.getPrimaryStorageVolume();
        } else {
            StorageVolume[] volumeList = storageManager.getVolumeList();
            int length = volumeList.length;
            int i = 0;
            while (true) {
                if (i < length) {
                    storageVolume = volumeList[i];
                    if (str2.equals(storageVolume.getUuid())) {
                        break;
                    }
                    i++;
                } else {
                    storageVolume = null;
                    break;
                }
            }
            if (storageVolume == null) {
                Log.w("ScopedAccessProvider", "didn't find volume for UUID=" + str2);
                return false;
            }
            if (str3 != null && !Environment.isStandardDirectory(str3)) {
                Log.w("ScopedAccessProvider", "not a scoped directory: " + str3);
                return false;
            }
            primaryStorageVolume = storageVolume;
        }
        return SharedMinimal.getUriPermission(context, contentProviderClientAcquireContentProviderClient, primaryStorageVolume, SharedMinimal.getInternalDirectoryName(str3), UserHandle.getCallingUserId(), false, new SharedMinimal.GetUriPermissionCallback() {
            @Override
            public final boolean onResult(File file, String str4, boolean z2, boolean z3, Uri uri, Uri uri2) {
                return ScopedAccessProvider.lambda$persistUriPermission$6(this.f$0, context, str, z, file, str4, z2, z3, uri, uri2);
            }
        });
    }

    public static boolean lambda$persistUriPermission$6(ScopedAccessProvider scopedAccessProvider, Context context, String str, boolean z, File file, String str2, boolean z2, boolean z3, Uri uri, Uri uri2) {
        scopedAccessProvider.updatePermission(context, uri, str, z);
        return true;
    }

    private void updatePermission(Context context, Uri uri, String str, boolean z) {
        ContentResolver contentResolver = context.getContentResolver();
        if (z) {
            context.grantUriPermission(str, uri, 195);
            contentResolver.takePersistableUriPermission(str, uri, 3);
        } else {
            context.revokeUriPermission(uri, 195);
        }
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) throws Exception {
        ArrayList arrayList = new ArrayList();
        printWriter.print("Packages: ");
        Cursor packagesCursor = getPackagesCursor();
        Throwable th = null;
        try {
            if (packagesCursor != null) {
                try {
                    if (packagesCursor.getCount() == 0) {
                        printWriter.println("N/A");
                    } else {
                        printWriter.println(packagesCursor.getCount());
                        while (packagesCursor.moveToNext()) {
                            String string = packagesCursor.getString(0);
                            arrayList.add(string);
                            printWriter.print("  ");
                            printWriter.println(string);
                        }
                    }
                } finally {
                }
            } else {
                printWriter.println("N/A");
            }
            if (packagesCursor != null) {
                $closeResource(null, packagesCursor);
            }
            printWriter.print("Permissions: ");
            for (int i = 0; i < arrayList.size(); i++) {
                Cursor permissionsCursor = getPermissionsCursor((String) arrayList.get(i));
                if (permissionsCursor == null) {
                    try {
                        try {
                            printWriter.println("N/A");
                        } finally {
                            if (permissionsCursor != null) {
                                $closeResource(th, permissionsCursor);
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } else {
                    printWriter.println(permissionsCursor.getCount());
                    while (permissionsCursor.moveToNext()) {
                        printWriter.print("  ");
                        printWriter.print(permissionsCursor.getString(0));
                        printWriter.print('/');
                        boolean z = true;
                        String string2 = permissionsCursor.getString(1);
                        if (string2 != null) {
                            printWriter.print(string2);
                            printWriter.print('>');
                        }
                        printWriter.print(permissionsCursor.getString(2));
                        printWriter.print(": ");
                        if (permissionsCursor.getInt(3) != 1) {
                            z = false;
                        }
                        printWriter.println(z);
                    }
                }
                if (permissionsCursor != null) {
                    $closeResource(null, permissionsCursor);
                }
            }
            printWriter.print("Raw permissions: ");
            List<ScopedAccessLocalPreferences.Permission> allPermissions = ScopedAccessLocalPreferences.getAllPermissions(getContext());
            if (allPermissions.isEmpty()) {
                printWriter.println("N/A");
                return;
            }
            int size = allPermissions.size();
            printWriter.println(size);
            for (int i2 = 0; i2 < size; i2++) {
                ScopedAccessLocalPreferences.Permission permission = allPermissions.get(i2);
                printWriter.print("  ");
                printWriter.println(permission);
            }
        } finally {
            if (packagesCursor != null) {
                $closeResource(null, packagesCursor);
            }
        }
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
}
