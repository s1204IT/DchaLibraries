package com.android.launcher3.provider;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Process;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.LongSparseArray;
import android.util.SparseBooleanArray;
import com.android.launcher3.AutoInstallsLayout;
import com.android.launcher3.DefaultLayoutParser;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.model.GridSizeMigrationTask;
import com.android.launcher3.util.LongArrayMap;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;

public class ImportDataTask {
    private static final int BATCH_INSERT_SIZE = 15;
    public static final String KEY_DATA_IMPORT_SRC_AUTHORITY = "data_import_src_authority";
    public static final String KEY_DATA_IMPORT_SRC_PKG = "data_import_src_pkg";
    private static final int MIN_ITEM_COUNT_FOR_SUCCESSFUL_MIGRATION = 6;
    private static final String TAG = "ImportDataTask";
    private final Context mContext;
    private int mHotseatSize;
    private int mMaxGridSizeX;
    private int mMaxGridSizeY;
    private final Uri mOtherFavoritesUri;
    private final Uri mOtherScreensUri;

    private ImportDataTask(Context context, String str) {
        this.mContext = context;
        this.mOtherScreensUri = Uri.parse("content://" + str + "/" + LauncherSettings.WorkspaceScreens.TABLE_NAME);
        this.mOtherFavoritesUri = Uri.parse("content://" + str + "/" + LauncherSettings.Favorites.TABLE_NAME);
    }

    public boolean importWorkspace() throws Exception {
        ArrayList<Long> screenIdsFromCursor = LauncherDbUtils.getScreenIdsFromCursor(this.mContext.getContentResolver().query(this.mOtherScreensUri, null, null, null, LauncherSettings.WorkspaceScreens.SCREEN_RANK));
        FileLog.d(TAG, "Importing DB from " + this.mOtherFavoritesUri);
        if (screenIdsFromCursor.isEmpty()) {
            FileLog.e(TAG, "No data found to import");
            return false;
        }
        this.mMaxGridSizeY = 0;
        this.mMaxGridSizeX = 0;
        this.mHotseatSize = 0;
        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
        int size = screenIdsFromCursor.size();
        LongSparseArray<Long> longSparseArray = new LongSparseArray<>(size);
        for (int i = 0; i < size; i++) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("_id", Integer.valueOf(i));
            contentValues.put(LauncherSettings.WorkspaceScreens.SCREEN_RANK, Integer.valueOf(i));
            longSparseArray.put(screenIdsFromCursor.get(i).longValue(), Long.valueOf(i));
            arrayList.add(ContentProviderOperation.newInsert(LauncherSettings.WorkspaceScreens.CONTENT_URI).withValues(contentValues).build());
        }
        this.mContext.getContentResolver().applyBatch(LauncherProvider.AUTHORITY, arrayList);
        importWorkspaceItems(screenIdsFromCursor.get(0).longValue(), longSparseArray);
        GridSizeMigrationTask.markForMigration(this.mContext, this.mMaxGridSizeX, this.mMaxGridSizeY, this.mHotseatSize);
        LauncherSettings.Settings.call(this.mContext.getContentResolver(), LauncherSettings.Settings.METHOD_CLEAR_EMPTY_DB_FLAG);
        return true;
    }

    private void importWorkspaceItems(long j, LongSparseArray<Long> longSparseArray) throws Exception {
        Throwable th;
        Throwable th2;
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        Intent intent;
        int i9;
        HashSet hashSet;
        ArrayList<ContentProviderOperation> arrayList;
        int i10;
        String string = Long.toString(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(Process.myUserHandle()));
        ArrayList<ContentProviderOperation> arrayList2 = new ArrayList<>(15);
        HashSet hashSet2 = new HashSet();
        Cursor cursorQuery = this.mContext.getContentResolver().query(this.mOtherFavoritesUri, null, "profileId = ?", new String[]{string}, LauncherSettings.Favorites.CONTAINER);
        try {
            try {
                int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("_id");
                int columnIndexOrThrow2 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.INTENT);
                int columnIndexOrThrow3 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.TITLE);
                int columnIndexOrThrow4 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
                int columnIndexOrThrow5 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ITEM_TYPE);
                int columnIndexOrThrow6 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_PROVIDER);
                int columnIndexOrThrow7 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
                int columnIndexOrThrow8 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                int columnIndexOrThrow9 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
                int columnIndexOrThrow10 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
                int columnIndexOrThrow11 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);
                int columnIndexOrThrow12 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.Favorites.RANK);
                ArrayList<ContentProviderOperation> arrayList3 = arrayList2;
                int columnIndexOrThrow13 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON);
                int i11 = columnIndexOrThrow3;
                int columnIndexOrThrow14 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE);
                HashSet hashSet3 = hashSet2;
                int columnIndexOrThrow15 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE);
                int i12 = columnIndexOrThrow6;
                SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
                int i13 = columnIndexOrThrow12;
                ContentValues contentValues = new ContentValues();
                int i14 = columnIndexOrThrow13;
                int i15 = 0;
                int i16 = 0;
                while (cursorQuery.moveToNext()) {
                    try {
                        contentValues.clear();
                        int i17 = columnIndexOrThrow15;
                        int i18 = cursorQuery.getInt(columnIndexOrThrow);
                        int iMax = Math.max(i15, i18);
                        int i19 = columnIndexOrThrow;
                        int i20 = cursorQuery.getInt(columnIndexOrThrow5);
                        int i21 = cursorQuery.getInt(columnIndexOrThrow4);
                        int i22 = columnIndexOrThrow4;
                        int i23 = columnIndexOrThrow5;
                        long j2 = cursorQuery.getLong(columnIndexOrThrow7);
                        int i24 = columnIndexOrThrow7;
                        int i25 = cursorQuery.getInt(columnIndexOrThrow8);
                        int i26 = columnIndexOrThrow8;
                        int i27 = cursorQuery.getInt(columnIndexOrThrow9);
                        int i28 = columnIndexOrThrow9;
                        int i29 = cursorQuery.getInt(columnIndexOrThrow10);
                        int i30 = columnIndexOrThrow10;
                        int i31 = cursorQuery.getInt(columnIndexOrThrow11);
                        int i32 = columnIndexOrThrow11;
                        switch (i21) {
                            case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                i = i31;
                                this.mHotseatSize = Math.max(this.mHotseatSize, ((int) j2) + 1);
                                if (i20 != 4) {
                                    switch (i20) {
                                        case 0:
                                        case 1:
                                            Intent uri = Intent.parseUri(cursorQuery.getString(columnIndexOrThrow2), 0);
                                            if (Utilities.isLauncherAppTarget(uri)) {
                                                i2 = columnIndexOrThrow2;
                                                i6 = i17;
                                                i10 = 0;
                                            } else {
                                                contentValues.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE, cursorQuery.getString(columnIndexOrThrow14));
                                                i2 = columnIndexOrThrow2;
                                                i6 = i17;
                                                contentValues.put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE, cursorQuery.getString(i6));
                                                i10 = i20;
                                            }
                                            int i33 = i10;
                                            i3 = columnIndexOrThrow14;
                                            int i34 = i14;
                                            contentValues.put(LauncherSettings.BaseLauncherColumns.ICON, cursorQuery.getBlob(i34));
                                            i5 = i34;
                                            contentValues.put(LauncherSettings.BaseLauncherColumns.INTENT, uri.toUri(0));
                                            i4 = i13;
                                            contentValues.put(LauncherSettings.Favorites.RANK, Integer.valueOf(cursorQuery.getInt(i4)));
                                            contentValues.put(LauncherSettings.Favorites.RESTORED, (Integer) 1);
                                            i7 = i12;
                                            i8 = i33;
                                            intent = uri;
                                            break;
                                        case 2:
                                            sparseBooleanArray.put(i18, true);
                                            intent = new Intent();
                                            i2 = columnIndexOrThrow2;
                                            i3 = columnIndexOrThrow14;
                                            i4 = i13;
                                            i5 = i14;
                                            i6 = i17;
                                            i8 = i20;
                                            i7 = i12;
                                            break;
                                        default:
                                            FileLog.d(TAG, String.format("Skipping item %d, not a valid type %d", Integer.valueOf(i18), Integer.valueOf(i20)));
                                            i2 = columnIndexOrThrow2;
                                            i3 = columnIndexOrThrow14;
                                            i9 = i12;
                                            i4 = i13;
                                            i5 = i14;
                                            i6 = i17;
                                            i13 = i4;
                                            break;
                                    }
                                    columnIndexOrThrow15 = i6;
                                    columnIndexOrThrow = i19;
                                    i15 = iMax;
                                    columnIndexOrThrow4 = i22;
                                    columnIndexOrThrow5 = i23;
                                    columnIndexOrThrow7 = i24;
                                    columnIndexOrThrow8 = i26;
                                    columnIndexOrThrow9 = i28;
                                    columnIndexOrThrow10 = i30;
                                    columnIndexOrThrow11 = i32;
                                    columnIndexOrThrow2 = i2;
                                    columnIndexOrThrow14 = i3;
                                    i14 = i5;
                                    i12 = i9;
                                } else {
                                    i2 = columnIndexOrThrow2;
                                    i3 = columnIndexOrThrow14;
                                    i4 = i13;
                                    i5 = i14;
                                    i6 = i17;
                                    contentValues.put(LauncherSettings.Favorites.RESTORED, (Integer) 7);
                                    i7 = i12;
                                    contentValues.put(LauncherSettings.Favorites.APPWIDGET_PROVIDER, cursorQuery.getString(i7));
                                    i8 = i20;
                                    intent = null;
                                }
                                i9 = i7;
                                if (i21 != -101) {
                                    hashSet = hashSet3;
                                } else if (intent == null) {
                                    FileLog.d(TAG, String.format("Skipping item %d, null intent on hotseat", Integer.valueOf(i18)));
                                    i13 = i4;
                                    columnIndexOrThrow15 = i6;
                                    columnIndexOrThrow = i19;
                                    i15 = iMax;
                                    columnIndexOrThrow4 = i22;
                                    columnIndexOrThrow5 = i23;
                                    columnIndexOrThrow7 = i24;
                                    columnIndexOrThrow8 = i26;
                                    columnIndexOrThrow9 = i28;
                                    columnIndexOrThrow10 = i30;
                                    columnIndexOrThrow11 = i32;
                                    columnIndexOrThrow2 = i2;
                                    columnIndexOrThrow14 = i3;
                                    i14 = i5;
                                    i12 = i9;
                                } else {
                                    if (intent.getComponent() != null) {
                                        intent.setPackage(intent.getComponent().getPackageName());
                                    }
                                    hashSet = hashSet3;
                                    hashSet.add(getPackage(intent));
                                }
                                contentValues.put("_id", Integer.valueOf(i18));
                                contentValues.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, Integer.valueOf(i8));
                                contentValues.put(LauncherSettings.Favorites.CONTAINER, Integer.valueOf(i21));
                                contentValues.put(LauncherSettings.Favorites.SCREEN, Long.valueOf(j2));
                                contentValues.put(LauncherSettings.Favorites.CELLX, Integer.valueOf(i25));
                                contentValues.put(LauncherSettings.Favorites.CELLY, Integer.valueOf(i27));
                                contentValues.put(LauncherSettings.Favorites.SPANX, Integer.valueOf(i29));
                                contentValues.put(LauncherSettings.Favorites.SPANY, Integer.valueOf(i));
                                int i35 = i11;
                                contentValues.put(LauncherSettings.BaseLauncherColumns.TITLE, cursorQuery.getString(i35));
                                arrayList = arrayList3;
                                arrayList.add(ContentProviderOperation.newInsert(LauncherSettings.Favorites.CONTENT_URI).withValues(contentValues).build());
                                if (i21 < 0) {
                                    i16++;
                                }
                                if (arrayList.size() >= 15) {
                                    this.mContext.getContentResolver().applyBatch(LauncherProvider.AUTHORITY, arrayList);
                                    arrayList.clear();
                                }
                                i11 = i35;
                                hashSet3 = hashSet;
                                i13 = i4;
                                arrayList3 = arrayList;
                                columnIndexOrThrow15 = i6;
                                columnIndexOrThrow = i19;
                                i15 = iMax;
                                columnIndexOrThrow4 = i22;
                                columnIndexOrThrow5 = i23;
                                columnIndexOrThrow7 = i24;
                                columnIndexOrThrow8 = i26;
                                columnIndexOrThrow9 = i28;
                                columnIndexOrThrow10 = i30;
                                columnIndexOrThrow11 = i32;
                                columnIndexOrThrow2 = i2;
                                columnIndexOrThrow14 = i3;
                                i14 = i5;
                                i12 = i9;
                                break;
                            case -100:
                                Long l = longSparseArray.get(j2);
                                if (l != null) {
                                    long jLongValue = l.longValue();
                                    this.mMaxGridSizeX = Math.max(this.mMaxGridSizeX, i25 + i29);
                                    this.mMaxGridSizeY = Math.max(this.mMaxGridSizeY, i27 + i31);
                                    i = i31;
                                    j2 = jLongValue;
                                    if (i20 != 4) {
                                    }
                                    i9 = i7;
                                    if (i21 != -101) {
                                    }
                                    contentValues.put("_id", Integer.valueOf(i18));
                                    contentValues.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, Integer.valueOf(i8));
                                    contentValues.put(LauncherSettings.Favorites.CONTAINER, Integer.valueOf(i21));
                                    contentValues.put(LauncherSettings.Favorites.SCREEN, Long.valueOf(j2));
                                    contentValues.put(LauncherSettings.Favorites.CELLX, Integer.valueOf(i25));
                                    contentValues.put(LauncherSettings.Favorites.CELLY, Integer.valueOf(i27));
                                    contentValues.put(LauncherSettings.Favorites.SPANX, Integer.valueOf(i29));
                                    contentValues.put(LauncherSettings.Favorites.SPANY, Integer.valueOf(i));
                                    int i352 = i11;
                                    contentValues.put(LauncherSettings.BaseLauncherColumns.TITLE, cursorQuery.getString(i352));
                                    arrayList = arrayList3;
                                    arrayList.add(ContentProviderOperation.newInsert(LauncherSettings.Favorites.CONTENT_URI).withValues(contentValues).build());
                                    if (i21 < 0) {
                                    }
                                    if (arrayList.size() >= 15) {
                                    }
                                    i11 = i352;
                                    hashSet3 = hashSet;
                                    i13 = i4;
                                    arrayList3 = arrayList;
                                    columnIndexOrThrow15 = i6;
                                    columnIndexOrThrow = i19;
                                    i15 = iMax;
                                    columnIndexOrThrow4 = i22;
                                    columnIndexOrThrow5 = i23;
                                    columnIndexOrThrow7 = i24;
                                    columnIndexOrThrow8 = i26;
                                    columnIndexOrThrow9 = i28;
                                    columnIndexOrThrow10 = i30;
                                    columnIndexOrThrow11 = i32;
                                    columnIndexOrThrow2 = i2;
                                    columnIndexOrThrow14 = i3;
                                    i14 = i5;
                                    i12 = i9;
                                } else {
                                    FileLog.d(TAG, String.format("Skipping item %d, type %d not on a valid screen %d", Integer.valueOf(i18), Integer.valueOf(i20), Long.valueOf(j2)));
                                    i2 = columnIndexOrThrow2;
                                    i3 = columnIndexOrThrow14;
                                    i9 = i12;
                                    i4 = i13;
                                    i5 = i14;
                                    i6 = i17;
                                    i13 = i4;
                                    columnIndexOrThrow15 = i6;
                                    columnIndexOrThrow = i19;
                                    i15 = iMax;
                                    columnIndexOrThrow4 = i22;
                                    columnIndexOrThrow5 = i23;
                                    columnIndexOrThrow7 = i24;
                                    columnIndexOrThrow8 = i26;
                                    columnIndexOrThrow9 = i28;
                                    columnIndexOrThrow10 = i30;
                                    columnIndexOrThrow11 = i32;
                                    columnIndexOrThrow2 = i2;
                                    columnIndexOrThrow14 = i3;
                                    i14 = i5;
                                    i12 = i9;
                                }
                                break;
                            default:
                                i = i31;
                                if (sparseBooleanArray.get(i21)) {
                                    if (i20 != 4) {
                                    }
                                    i9 = i7;
                                    if (i21 != -101) {
                                    }
                                    contentValues.put("_id", Integer.valueOf(i18));
                                    contentValues.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, Integer.valueOf(i8));
                                    contentValues.put(LauncherSettings.Favorites.CONTAINER, Integer.valueOf(i21));
                                    contentValues.put(LauncherSettings.Favorites.SCREEN, Long.valueOf(j2));
                                    contentValues.put(LauncherSettings.Favorites.CELLX, Integer.valueOf(i25));
                                    contentValues.put(LauncherSettings.Favorites.CELLY, Integer.valueOf(i27));
                                    contentValues.put(LauncherSettings.Favorites.SPANX, Integer.valueOf(i29));
                                    contentValues.put(LauncherSettings.Favorites.SPANY, Integer.valueOf(i));
                                    int i3522 = i11;
                                    contentValues.put(LauncherSettings.BaseLauncherColumns.TITLE, cursorQuery.getString(i3522));
                                    arrayList = arrayList3;
                                    arrayList.add(ContentProviderOperation.newInsert(LauncherSettings.Favorites.CONTENT_URI).withValues(contentValues).build());
                                    if (i21 < 0) {
                                    }
                                    if (arrayList.size() >= 15) {
                                    }
                                    i11 = i3522;
                                    hashSet3 = hashSet;
                                    i13 = i4;
                                    arrayList3 = arrayList;
                                    columnIndexOrThrow15 = i6;
                                    columnIndexOrThrow = i19;
                                    i15 = iMax;
                                    columnIndexOrThrow4 = i22;
                                    columnIndexOrThrow5 = i23;
                                    columnIndexOrThrow7 = i24;
                                    columnIndexOrThrow8 = i26;
                                    columnIndexOrThrow9 = i28;
                                    columnIndexOrThrow10 = i30;
                                    columnIndexOrThrow11 = i32;
                                    columnIndexOrThrow2 = i2;
                                    columnIndexOrThrow14 = i3;
                                    i14 = i5;
                                    i12 = i9;
                                } else {
                                    FileLog.d(TAG, String.format("Skipping item %d, type %d not in a valid folder %d", Integer.valueOf(i18), Integer.valueOf(i20), Integer.valueOf(i21)));
                                }
                                i2 = columnIndexOrThrow2;
                                i3 = columnIndexOrThrow14;
                                i9 = i12;
                                i4 = i13;
                                i5 = i14;
                                i6 = i17;
                                i13 = i4;
                                columnIndexOrThrow15 = i6;
                                columnIndexOrThrow = i19;
                                i15 = iMax;
                                columnIndexOrThrow4 = i22;
                                columnIndexOrThrow5 = i23;
                                columnIndexOrThrow7 = i24;
                                columnIndexOrThrow8 = i26;
                                columnIndexOrThrow9 = i28;
                                columnIndexOrThrow10 = i30;
                                columnIndexOrThrow11 = i32;
                                columnIndexOrThrow2 = i2;
                                columnIndexOrThrow14 = i3;
                                i14 = i5;
                                i12 = i9;
                                break;
                        }
                    } catch (Throwable th3) {
                        th2 = th3;
                        th = null;
                        if (cursorQuery != null) {
                            throw th2;
                        }
                        if (th == null) {
                            cursorQuery.close();
                            throw th2;
                        }
                        try {
                            cursorQuery.close();
                            throw th2;
                        } catch (Throwable th4) {
                            th.addSuppressed(th4);
                            throw th2;
                        }
                    }
                }
                ArrayList<ContentProviderOperation> arrayList4 = arrayList3;
                HashSet hashSet4 = hashSet3;
                int i36 = i16;
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                FileLog.d(TAG, i36 + " items imported from external source");
                if (i36 < 6) {
                    throw new Exception("Insufficient data");
                }
                if (!arrayList4.isEmpty()) {
                    this.mContext.getContentResolver().applyBatch(LauncherProvider.AUTHORITY, arrayList4);
                    arrayList4.clear();
                }
                LongArrayMap<Object> longArrayMapRemoveBrokenHotseatItems = GridSizeMigrationTask.removeBrokenHotseatItems(this.mContext);
                int i37 = LauncherAppState.getIDP(this.mContext).numHotseatIcons;
                if (longArrayMapRemoveBrokenHotseatItems.size() < i37) {
                    new HotseatLayoutParser(this.mContext, new HotseatParserCallback(hashSet4, longArrayMapRemoveBrokenHotseatItems, arrayList4, i15 + 1, i37)).loadLayout(null, new ArrayList<>());
                    this.mHotseatSize = ((int) longArrayMapRemoveBrokenHotseatItems.keyAt(longArrayMapRemoveBrokenHotseatItems.size() - 1)) + 1;
                    if (arrayList4.isEmpty()) {
                        return;
                    }
                    this.mContext.getContentResolver().applyBatch(LauncherProvider.AUTHORITY, arrayList4);
                }
            } catch (Throwable th5) {
                try {
                    throw th5;
                } catch (Throwable th6) {
                    th2 = th6;
                    th = th5;
                    if (cursorQuery != null) {
                    }
                }
            }
        } catch (Throwable th7) {
            th = null;
            th2 = th7;
        }
    }

    private static String getPackage(Intent intent) {
        return intent.getComponent() != null ? intent.getComponent().getPackageName() : intent.getPackage();
    }

    public static boolean performImportIfPossible(Context context) throws Exception {
        SharedPreferences devicePrefs = Utilities.getDevicePrefs(context);
        String string = devicePrefs.getString(KEY_DATA_IMPORT_SRC_PKG, "");
        String string2 = devicePrefs.getString(KEY_DATA_IMPORT_SRC_AUTHORITY, "");
        if (TextUtils.isEmpty(string) || TextUtils.isEmpty(string2)) {
            return false;
        }
        devicePrefs.edit().remove(KEY_DATA_IMPORT_SRC_PKG).remove(KEY_DATA_IMPORT_SRC_AUTHORITY).commit();
        if (!LauncherSettings.Settings.call(context.getContentResolver(), LauncherSettings.Settings.METHOD_WAS_EMPTY_DB_CREATED).getBoolean(LauncherSettings.Settings.EXTRA_VALUE, false)) {
            return false;
        }
        for (ProviderInfo providerInfo : context.getPackageManager().queryContentProviders((String) null, context.getApplicationInfo().uid, 0)) {
            if (string.equals(providerInfo.packageName)) {
                if ((providerInfo.applicationInfo.flags & 1) == 0) {
                    return false;
                }
                if (string2.equals(providerInfo.authority) && (TextUtils.isEmpty(providerInfo.readPermission) || context.checkPermission(providerInfo.readPermission, Process.myPid(), Process.myUid()) == 0)) {
                    return new ImportDataTask(context, string2).importWorkspace();
                }
            }
        }
        return false;
    }

    private static int getMyHotseatLayoutId(Context context) {
        if (LauncherAppState.getIDP(context).numHotseatIcons <= 5) {
            return R.xml.dw_phone_hotseat;
        }
        return R.xml.dw_tablet_hotseat;
    }

    private static class HotseatLayoutParser extends DefaultLayoutParser {
        public HotseatLayoutParser(Context context, AutoInstallsLayout.LayoutParserCallback layoutParserCallback) {
            super(context, null, layoutParserCallback, context.getResources(), ImportDataTask.getMyHotseatLayoutId(context));
        }

        @Override
        protected ArrayMap<String, AutoInstallsLayout.TagParser> getLayoutElementsMap() {
            ArrayMap<String, AutoInstallsLayout.TagParser> arrayMap = new ArrayMap<>();
            arrayMap.put("favorite", new DefaultLayoutParser.AppShortcutWithUriParser());
            arrayMap.put("shortcut", new DefaultLayoutParser.UriShortcutParser(this.mSourceRes));
            arrayMap.put("resolve", new DefaultLayoutParser.ResolveParser());
            return arrayMap;
        }
    }

    private static class HotseatParserCallback implements AutoInstallsLayout.LayoutParserCallback {
        private final HashSet<String> mExistingApps;
        private final LongArrayMap<Object> mExistingItems;
        private final ArrayList<ContentProviderOperation> mOutOps;
        private final int mRequiredSize;
        private int mStartItemId;

        HotseatParserCallback(HashSet<String> hashSet, LongArrayMap<Object> longArrayMap, ArrayList<ContentProviderOperation> arrayList, int i, int i2) {
            this.mExistingApps = hashSet;
            this.mExistingItems = longArrayMap;
            this.mOutOps = arrayList;
            this.mRequiredSize = i2;
            this.mStartItemId = i;
        }

        @Override
        public long generateNewItemId() {
            int i = this.mStartItemId;
            this.mStartItemId = i + 1;
            return i;
        }

        @Override
        public long insertAndCheck(SQLiteDatabase sQLiteDatabase, ContentValues contentValues) {
            if (this.mExistingItems.size() >= this.mRequiredSize) {
                return 0L;
            }
            try {
                Intent uri = Intent.parseUri(contentValues.getAsString(LauncherSettings.BaseLauncherColumns.INTENT), 0);
                String str = ImportDataTask.getPackage(uri);
                if (str == null || this.mExistingApps.contains(str)) {
                    return 0L;
                }
                this.mExistingApps.add(str);
                long j = 0;
                while (this.mExistingItems.get(j) != null) {
                    j++;
                }
                this.mExistingItems.put(j, uri);
                contentValues.put(LauncherSettings.Favorites.SCREEN, Long.valueOf(j));
                this.mOutOps.add(ContentProviderOperation.newInsert(LauncherSettings.Favorites.CONTENT_URI).withValues(contentValues).build());
                return 0L;
            } catch (URISyntaxException e) {
                return 0L;
            }
        }
    }
}
