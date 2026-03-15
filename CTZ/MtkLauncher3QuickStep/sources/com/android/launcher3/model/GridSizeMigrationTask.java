package com.android.launcher3.model;

import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.LongArrayMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

public class GridSizeMigrationTask {
    private static final boolean DEBUG = true;
    public static boolean ENABLED = Utilities.ATLEAST_NOUGAT;
    private static final String KEY_MIGRATION_SRC_HOTSEAT_COUNT = "migration_src_hotseat_count";
    private static final String KEY_MIGRATION_SRC_WORKSPACE_SIZE = "migration_src_workspace_size";
    private static final String TAG = "GridSizeMigrationTask";
    private static final float WT_APPLICATION = 0.8f;
    private static final float WT_FOLDER_FACTOR = 0.5f;
    private static final float WT_SHORTCUT = 1.0f;
    private static final float WT_WIDGET_FACTOR = 0.6f;
    private static final float WT_WIDGET_MIN = 2.0f;
    protected final ArrayList<DbEntry> mCarryOver;
    private final Context mContext;
    private final int mDestHotseatSize;
    protected final ArrayList<Long> mEntryToRemove;
    private final InvariantDeviceProfile mIdp;
    private final boolean mShouldRemoveX;
    private final boolean mShouldRemoveY;
    private final int mSrcHotseatSize;
    private final int mSrcX;
    private final int mSrcY;
    private final ContentValues mTempValues;
    private final int mTrgX;
    private final int mTrgY;
    private final ArrayList<ContentProviderOperation> mUpdateOperations;
    private final HashSet<String> mValidPackages;

    protected GridSizeMigrationTask(Context context, InvariantDeviceProfile invariantDeviceProfile, HashSet<String> hashSet, Point point, Point point2) {
        this.mTempValues = new ContentValues();
        this.mEntryToRemove = new ArrayList<>();
        this.mUpdateOperations = new ArrayList<>();
        this.mCarryOver = new ArrayList<>();
        this.mContext = context;
        this.mValidPackages = hashSet;
        this.mIdp = invariantDeviceProfile;
        this.mSrcX = point.x;
        this.mSrcY = point.y;
        this.mTrgX = point2.x;
        this.mTrgY = point2.y;
        this.mShouldRemoveX = this.mTrgX < this.mSrcX;
        this.mShouldRemoveY = this.mTrgY < this.mSrcY;
        this.mDestHotseatSize = -1;
        this.mSrcHotseatSize = -1;
    }

    protected GridSizeMigrationTask(Context context, InvariantDeviceProfile invariantDeviceProfile, HashSet<String> hashSet, int i, int i2) {
        this.mTempValues = new ContentValues();
        this.mEntryToRemove = new ArrayList<>();
        this.mUpdateOperations = new ArrayList<>();
        this.mCarryOver = new ArrayList<>();
        this.mContext = context;
        this.mIdp = invariantDeviceProfile;
        this.mValidPackages = hashSet;
        this.mSrcHotseatSize = i;
        this.mDestHotseatSize = i2;
        this.mTrgY = -1;
        this.mTrgX = -1;
        this.mSrcY = -1;
        this.mSrcX = -1;
        this.mShouldRemoveY = false;
        this.mShouldRemoveX = false;
    }

    private boolean applyOperations() throws Exception {
        if (!this.mUpdateOperations.isEmpty()) {
            this.mContext.getContentResolver().applyBatch(LauncherProvider.AUTHORITY, this.mUpdateOperations);
        }
        if (!this.mEntryToRemove.isEmpty()) {
            Log.d(TAG, "Removing items: " + TextUtils.join(", ", this.mEntryToRemove));
            this.mContext.getContentResolver().delete(LauncherSettings.Favorites.CONTENT_URI, Utilities.createDbSelectionQuery("_id", this.mEntryToRemove), null);
        }
        return (this.mUpdateOperations.isEmpty() && this.mEntryToRemove.isEmpty()) ? false : true;
    }

    protected boolean migrateHotseat() throws Exception {
        ArrayList<DbEntry> arrayListLoadHotseatEntries = loadHotseatEntries();
        int i = this.mDestHotseatSize;
        while (arrayListLoadHotseatEntries.size() > i) {
            DbEntry dbEntry = arrayListLoadHotseatEntries.get(arrayListLoadHotseatEntries.size() / 2);
            for (DbEntry dbEntry2 : arrayListLoadHotseatEntries) {
                if (dbEntry2.weight < dbEntry.weight) {
                    dbEntry = dbEntry2;
                }
            }
            this.mEntryToRemove.add(Long.valueOf(dbEntry.id));
            arrayListLoadHotseatEntries.remove(dbEntry);
        }
        int i2 = 0;
        for (DbEntry dbEntry3 : arrayListLoadHotseatEntries) {
            long j = i2;
            if (dbEntry3.screenId != j) {
                dbEntry3.screenId = j;
                dbEntry3.cellX = i2;
                dbEntry3.cellY = 0;
                update(dbEntry3);
            }
            i2++;
        }
        return applyOperations();
    }

    protected boolean migrateWorkspace() throws Exception {
        ArrayList<Long> arrayListLoadWorkspaceScreensDb = LauncherModel.loadWorkspaceScreensDb(this.mContext);
        if (arrayListLoadWorkspaceScreensDb.isEmpty()) {
            throw new Exception("Unable to get workspace screens");
        }
        Iterator<Long> it = arrayListLoadWorkspaceScreensDb.iterator();
        while (it.hasNext()) {
            long jLongValue = it.next().longValue();
            Log.d(TAG, "Migrating " + jLongValue);
            migrateScreen(jLongValue);
        }
        if (!this.mCarryOver.isEmpty()) {
            LongArrayMap longArrayMap = new LongArrayMap();
            for (DbEntry dbEntry : this.mCarryOver) {
                longArrayMap.put(dbEntry.id, dbEntry);
            }
            do {
                OptimalPlacementSolution optimalPlacementSolution = new OptimalPlacementSolution(new GridOccupancy(this.mTrgX, this.mTrgY), deepCopy(this.mCarryOver), 0, true);
                optimalPlacementSolution.find();
                if (optimalPlacementSolution.finalPlacedItems.size() > 0) {
                    long j = LauncherSettings.Settings.call(this.mContext.getContentResolver(), LauncherSettings.Settings.METHOD_NEW_SCREEN_ID).getLong(LauncherSettings.Settings.EXTRA_VALUE);
                    arrayListLoadWorkspaceScreensDb.add(Long.valueOf(j));
                    for (DbEntry dbEntry2 : optimalPlacementSolution.finalPlacedItems) {
                        if (!this.mCarryOver.remove(longArrayMap.get(dbEntry2.id))) {
                            throw new Exception("Unable to find matching items");
                        }
                        dbEntry2.screenId = j;
                        update(dbEntry2);
                    }
                } else {
                    throw new Exception("None of the items can be placed on an empty screen");
                }
            } while (!this.mCarryOver.isEmpty());
            Uri uri = LauncherSettings.WorkspaceScreens.CONTENT_URI;
            this.mUpdateOperations.add(ContentProviderOperation.newDelete(uri).build());
            int size = arrayListLoadWorkspaceScreensDb.size();
            for (int i = 0; i < size; i++) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("_id", Long.valueOf(arrayListLoadWorkspaceScreensDb.get(i).longValue()));
                contentValues.put(LauncherSettings.WorkspaceScreens.SCREEN_RANK, Integer.valueOf(i));
                this.mUpdateOperations.add(ContentProviderOperation.newInsert(uri).withValues(contentValues).build());
            }
        }
        return applyOperations();
    }

    protected void migrateScreen(long j) {
        ArrayList<DbEntry> arrayListLoadWorkspaceEntries = loadWorkspaceEntries(j);
        float[] fArr = new float[2];
        int i = Integer.MAX_VALUE;
        ArrayList<DbEntry> arrayList = null;
        float f = Float.MAX_VALUE;
        float f2 = Float.MAX_VALUE;
        int i2 = Integer.MAX_VALUE;
        for (int i3 = 0; i3 < this.mSrcX; i3++) {
            int i4 = i2;
            int i5 = i;
            ArrayList<DbEntry> arrayList2 = arrayList;
            float f3 = f;
            float f4 = f2;
            for (int i6 = this.mSrcY - 1; i6 >= 0; i6--) {
                ArrayList<DbEntry> arrayListTryRemove = tryRemove(i3, i6, 0, deepCopy(arrayListLoadWorkspaceEntries), fArr);
                if (fArr[0] < f3 || (fArr[0] == f3 && fArr[1] < f4)) {
                    float f5 = fArr[0];
                    float f6 = fArr[1];
                    if (this.mShouldRemoveX) {
                        i5 = i3;
                    }
                    if (this.mShouldRemoveY) {
                        i4 = i6;
                    }
                    arrayList2 = arrayListTryRemove;
                    f3 = f5;
                    f4 = f6;
                }
                if (!this.mShouldRemoveY) {
                    break;
                }
            }
            f = f3;
            f2 = f4;
            i = i5;
            i2 = i4;
            arrayList = arrayList2;
            if (!this.mShouldRemoveX) {
                break;
            }
        }
        Log.d(TAG, String.format("Removing row %d, column %d on screen %d", Integer.valueOf(i2), Integer.valueOf(i), Long.valueOf(j)));
        LongArrayMap longArrayMap = new LongArrayMap();
        for (DbEntry dbEntry : deepCopy(arrayListLoadWorkspaceEntries)) {
            longArrayMap.put(dbEntry.id, dbEntry);
        }
        for (DbEntry dbEntry2 : arrayList) {
            DbEntry dbEntry3 = (DbEntry) longArrayMap.get(dbEntry2.id);
            longArrayMap.remove(dbEntry2.id);
            if (!dbEntry2.columnsSame(dbEntry3)) {
                update(dbEntry2);
            }
        }
        Iterator it = longArrayMap.iterator();
        while (it.hasNext()) {
            this.mCarryOver.add((DbEntry) it.next());
        }
        if (!this.mCarryOver.isEmpty() && f == 0.0f) {
            GridOccupancy gridOccupancy = new GridOccupancy(this.mTrgX, this.mTrgY);
            gridOccupancy.markCells(0, 0, this.mTrgX, 0, true);
            Iterator<DbEntry> it2 = arrayList.iterator();
            while (it2.hasNext()) {
                gridOccupancy.markCells((ItemInfo) it2.next(), true);
            }
            OptimalPlacementSolution optimalPlacementSolution = new OptimalPlacementSolution(gridOccupancy, deepCopy(this.mCarryOver), 0, true);
            optimalPlacementSolution.find();
            if (optimalPlacementSolution.lowestWeightLoss == 0.0f) {
                for (DbEntry dbEntry4 : optimalPlacementSolution.finalPlacedItems) {
                    dbEntry4.screenId = j;
                    update(dbEntry4);
                }
                this.mCarryOver.clear();
            }
        }
    }

    protected void update(DbEntry dbEntry) {
        this.mTempValues.clear();
        dbEntry.addToContentValues(this.mTempValues);
        this.mUpdateOperations.add(ContentProviderOperation.newUpdate(LauncherSettings.Favorites.getContentUri(dbEntry.id)).withValues(this.mTempValues).build());
    }

    private ArrayList<DbEntry> tryRemove(int i, int i2, int i3, ArrayList<DbEntry> arrayList, float[] fArr) {
        GridOccupancy gridOccupancy = new GridOccupancy(this.mTrgX, this.mTrgY);
        gridOccupancy.markCells(0, 0, this.mTrgX, i3, true);
        if (!this.mShouldRemoveX) {
            i = Integer.MAX_VALUE;
        }
        if (!this.mShouldRemoveY) {
            i2 = Integer.MAX_VALUE;
        }
        ArrayList<DbEntry> arrayList2 = new ArrayList<>();
        ArrayList arrayList3 = new ArrayList();
        for (DbEntry dbEntry : arrayList) {
            if ((dbEntry.cellX <= i && dbEntry.spanX + dbEntry.cellX > i) || (dbEntry.cellY <= i2 && dbEntry.spanY + dbEntry.cellY > i2)) {
                arrayList3.add(dbEntry);
                if (dbEntry.cellX >= i) {
                    dbEntry.cellX--;
                }
                if (dbEntry.cellY >= i2) {
                    dbEntry.cellY--;
                }
            } else {
                if (dbEntry.cellX > i) {
                    dbEntry.cellX--;
                }
                if (dbEntry.cellY > i2) {
                    dbEntry.cellY--;
                }
                arrayList2.add(dbEntry);
                gridOccupancy.markCells((ItemInfo) dbEntry, true);
            }
        }
        OptimalPlacementSolution optimalPlacementSolution = new OptimalPlacementSolution(this, gridOccupancy, arrayList3, i3);
        optimalPlacementSolution.find();
        arrayList2.addAll(optimalPlacementSolution.finalPlacedItems);
        fArr[0] = optimalPlacementSolution.lowestWeightLoss;
        fArr[1] = optimalPlacementSolution.lowestMoveCost;
        return arrayList2;
    }

    private class OptimalPlacementSolution {
        ArrayList<DbEntry> finalPlacedItems;
        private final boolean ignoreMove;
        private final ArrayList<DbEntry> itemsToPlace;
        float lowestMoveCost;
        float lowestWeightLoss;
        private final GridOccupancy occupied;
        private final int startY;

        public OptimalPlacementSolution(GridSizeMigrationTask gridSizeMigrationTask, GridOccupancy gridOccupancy, ArrayList<DbEntry> arrayList, int i) {
            this(gridOccupancy, arrayList, i, false);
        }

        public OptimalPlacementSolution(GridOccupancy gridOccupancy, ArrayList<DbEntry> arrayList, int i, boolean z) {
            this.lowestWeightLoss = Float.MAX_VALUE;
            this.lowestMoveCost = Float.MAX_VALUE;
            this.occupied = gridOccupancy;
            this.itemsToPlace = arrayList;
            this.ignoreMove = z;
            this.startY = i;
            Collections.sort(this.itemsToPlace);
        }

        public void find() {
            find(0, 0.0f, 0.0f, new ArrayList<>());
        }

        public void find(int i, float f, float f2, ArrayList<DbEntry> arrayList) {
            float f3;
            float f4;
            int i2;
            float f5;
            float f6;
            int i3;
            int i4;
            float f7 = f;
            if (f7 < this.lowestWeightLoss) {
                if (f7 == this.lowestWeightLoss && f2 >= this.lowestMoveCost) {
                    return;
                }
                if (i >= this.itemsToPlace.size()) {
                    this.lowestWeightLoss = f7;
                    this.lowestMoveCost = f2;
                    this.finalPlacedItems = GridSizeMigrationTask.deepCopy(arrayList);
                    return;
                }
                DbEntry dbEntry = this.itemsToPlace.get(i);
                int i5 = dbEntry.cellX;
                int i6 = dbEntry.cellY;
                ArrayList<DbEntry> arrayList2 = new ArrayList<>(arrayList.size() + 1);
                arrayList2.addAll(arrayList);
                arrayList2.add(dbEntry);
                if (dbEntry.spanX > 1 || dbEntry.spanY > 1) {
                    int i7 = dbEntry.spanX;
                    int i8 = dbEntry.spanY;
                    for (int i9 = this.startY; i9 < GridSizeMigrationTask.this.mTrgY; i9++) {
                        int i10 = 0;
                        while (i10 < GridSizeMigrationTask.this.mTrgX) {
                            if (i10 != i5) {
                                dbEntry.cellX = i10;
                                f3 = 1.0f;
                                f4 = f2 + 1.0f;
                            } else {
                                f3 = 1.0f;
                                f4 = f2;
                            }
                            if (i9 != i6) {
                                dbEntry.cellY = i9;
                                f4 += f3;
                            }
                            if (this.ignoreMove) {
                                f4 = f2;
                            }
                            if (this.occupied.isRegionVacant(i10, i9, i7, i8)) {
                                this.occupied.markCells((ItemInfo) dbEntry, true);
                                find(i + 1, f7, f4, arrayList2);
                                this.occupied.markCells((ItemInfo) dbEntry, false);
                            }
                            if (i7 > dbEntry.minSpanX && this.occupied.isRegionVacant(i10, i9, i7 - 1, i8)) {
                                dbEntry.spanX--;
                                this.occupied.markCells((ItemInfo) dbEntry, true);
                                find(i + 1, f7, f4 + 1.0f, arrayList2);
                                this.occupied.markCells((ItemInfo) dbEntry, false);
                                dbEntry.spanX++;
                            }
                            if (i8 > dbEntry.minSpanY && this.occupied.isRegionVacant(i10, i9, i7, i8 - 1)) {
                                dbEntry.spanY--;
                                this.occupied.markCells((ItemInfo) dbEntry, true);
                                find(i + 1, f7, f4 + 1.0f, arrayList2);
                                this.occupied.markCells((ItemInfo) dbEntry, false);
                                dbEntry.spanY++;
                            }
                            if (i8 > dbEntry.minSpanY && i7 > dbEntry.minSpanX) {
                                i2 = i7;
                                if (this.occupied.isRegionVacant(i10, i9, i7 - 1, i8 - 1)) {
                                    dbEntry.spanX--;
                                    dbEntry.spanY--;
                                    this.occupied.markCells((ItemInfo) dbEntry, true);
                                    find(i + 1, f7, f4 + GridSizeMigrationTask.WT_WIDGET_MIN, arrayList2);
                                    this.occupied.markCells((ItemInfo) dbEntry, false);
                                    dbEntry.spanX++;
                                    dbEntry.spanY++;
                                }
                                dbEntry.cellX = i5;
                                dbEntry.cellY = i6;
                                i10++;
                                i7 = i2;
                            } else {
                                i2 = i7;
                            }
                            dbEntry.cellX = i5;
                            dbEntry.cellY = i6;
                            i10++;
                            i7 = i2;
                        }
                    }
                    find(i + 1, f7 + dbEntry.weight, f2, arrayList);
                    return;
                }
                int i11 = Integer.MAX_VALUE;
                int i12 = Integer.MAX_VALUE;
                int i13 = Integer.MAX_VALUE;
                for (int i14 = this.startY; i14 < GridSizeMigrationTask.this.mTrgY; i14++) {
                    for (int i15 = 0; i15 < GridSizeMigrationTask.this.mTrgX; i15++) {
                        if (this.occupied.cells[i15][i14]) {
                            i3 = i11;
                        } else {
                            if (!this.ignoreMove) {
                                i3 = i11;
                                i4 = ((dbEntry.cellX - i15) * (dbEntry.cellX - i15)) + ((dbEntry.cellY - i14) * (dbEntry.cellY - i14));
                            } else {
                                i3 = i11;
                                i4 = 0;
                            }
                            if (i4 < i13) {
                                i12 = i14;
                                i13 = i4;
                                i11 = i15;
                            }
                        }
                        i11 = i3;
                    }
                }
                if (i11 < GridSizeMigrationTask.this.mTrgX && i12 < GridSizeMigrationTask.this.mTrgY) {
                    if (i11 != i5) {
                        dbEntry.cellX = i11;
                        f5 = 1.0f;
                        f6 = f2 + 1.0f;
                    } else {
                        f5 = 1.0f;
                        f6 = f2;
                    }
                    if (i12 != i6) {
                        dbEntry.cellY = i12;
                        f6 += f5;
                    }
                    if (this.ignoreMove) {
                        f6 = f2;
                    }
                    this.occupied.markCells((ItemInfo) dbEntry, true);
                    int i16 = i + 1;
                    find(i16, f7, f6, arrayList2);
                    this.occupied.markCells((ItemInfo) dbEntry, false);
                    dbEntry.cellX = i5;
                    dbEntry.cellY = i6;
                    if (i16 < this.itemsToPlace.size() && this.itemsToPlace.get(i16).weight >= dbEntry.weight && !this.ignoreMove) {
                        find(i16, f7 + dbEntry.weight, f2, arrayList);
                        return;
                    }
                    return;
                }
                for (int i17 = i + 1; i17 < this.itemsToPlace.size(); i17++) {
                    f7 += this.itemsToPlace.get(i17).weight;
                }
                find(this.itemsToPlace.size(), f7 + dbEntry.weight, f2, arrayList);
            }
        }
    }

    private ArrayList<DbEntry> loadHotseatEntries() {
        Cursor cursorQuery = this.mContext.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, new String[]{"_id", LauncherSettings.BaseLauncherColumns.ITEM_TYPE, LauncherSettings.BaseLauncherColumns.INTENT, LauncherSettings.Favorites.SCREEN}, "container = -101", null, null, null);
        int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("_id");
        int columnIndexOrThrow2 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ITEM_TYPE);
        int columnIndexOrThrow3 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.INTENT);
        int columnIndexOrThrow4 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
        ArrayList<DbEntry> arrayList = new ArrayList<>();
        while (cursorQuery.moveToNext()) {
            DbEntry dbEntry = new DbEntry();
            dbEntry.id = cursorQuery.getLong(columnIndexOrThrow);
            dbEntry.itemType = cursorQuery.getInt(columnIndexOrThrow2);
            dbEntry.screenId = cursorQuery.getLong(columnIndexOrThrow4);
            if (dbEntry.screenId >= this.mSrcHotseatSize) {
                this.mEntryToRemove.add(Long.valueOf(dbEntry.id));
            } else {
                try {
                    int i = dbEntry.itemType;
                    if (i != 6) {
                        switch (i) {
                            case 0:
                            case 1:
                                verifyIntent(cursorQuery.getString(columnIndexOrThrow3));
                                dbEntry.weight = dbEntry.itemType == 0 ? WT_APPLICATION : 1.0f;
                                break;
                            case 2:
                                int folderItemsCount = getFolderItemsCount(dbEntry.id);
                                if (folderItemsCount == 0) {
                                    throw new Exception("Folder is empty");
                                }
                                dbEntry.weight = 0.5f * folderItemsCount;
                                break;
                                break;
                            default:
                                throw new Exception("Invalid item type");
                        }
                        arrayList.add(dbEntry);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Removing item " + dbEntry.id, e);
                    this.mEntryToRemove.add(Long.valueOf(dbEntry.id));
                }
            }
        }
        cursorQuery.close();
        return arrayList;
    }

    protected ArrayList<DbEntry> loadWorkspaceEntries(long j) {
        int i;
        long j2 = j;
        Cursor cursorQueryWorkspace = queryWorkspace(new String[]{"_id", LauncherSettings.BaseLauncherColumns.ITEM_TYPE, LauncherSettings.Favorites.CELLX, LauncherSettings.Favorites.CELLY, LauncherSettings.Favorites.SPANX, LauncherSettings.Favorites.SPANY, LauncherSettings.BaseLauncherColumns.INTENT, LauncherSettings.Favorites.APPWIDGET_PROVIDER, LauncherSettings.Favorites.APPWIDGET_ID}, "container = -100 AND screen = " + j2);
        int columnIndexOrThrow = cursorQueryWorkspace.getColumnIndexOrThrow("_id");
        int columnIndexOrThrow2 = cursorQueryWorkspace.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.ITEM_TYPE);
        int columnIndexOrThrow3 = cursorQueryWorkspace.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
        int columnIndexOrThrow4 = cursorQueryWorkspace.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
        int columnIndexOrThrow5 = cursorQueryWorkspace.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
        int columnIndexOrThrow6 = cursorQueryWorkspace.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);
        int columnIndexOrThrow7 = cursorQueryWorkspace.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.INTENT);
        int columnIndexOrThrow8 = cursorQueryWorkspace.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_PROVIDER);
        int columnIndexOrThrow9 = cursorQueryWorkspace.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_ID);
        ArrayList<DbEntry> arrayList = new ArrayList<>();
        while (cursorQueryWorkspace.moveToNext()) {
            DbEntry dbEntry = new DbEntry();
            int i2 = columnIndexOrThrow9;
            ArrayList<DbEntry> arrayList2 = arrayList;
            dbEntry.id = cursorQueryWorkspace.getLong(columnIndexOrThrow);
            dbEntry.itemType = cursorQueryWorkspace.getInt(columnIndexOrThrow2);
            dbEntry.cellX = cursorQueryWorkspace.getInt(columnIndexOrThrow3);
            dbEntry.cellY = cursorQueryWorkspace.getInt(columnIndexOrThrow4);
            dbEntry.spanX = cursorQueryWorkspace.getInt(columnIndexOrThrow5);
            dbEntry.spanY = cursorQueryWorkspace.getInt(columnIndexOrThrow6);
            dbEntry.screenId = j2;
            try {
                i = dbEntry.itemType;
            } catch (Exception e) {
                e = e;
                columnIndexOrThrow9 = i2;
            }
            if (i == 4) {
                verifyPackage(ComponentName.unflattenFromString(cursorQueryWorkspace.getString(columnIndexOrThrow8)).getPackageName());
                dbEntry.weight = Math.max(WT_WIDGET_MIN, WT_WIDGET_FACTOR * dbEntry.spanX * dbEntry.spanY);
                columnIndexOrThrow9 = i2;
                try {
                    LauncherAppWidgetProviderInfo launcherAppWidgetInfo = AppWidgetManagerCompat.getInstance(this.mContext).getLauncherAppWidgetInfo(cursorQueryWorkspace.getInt(columnIndexOrThrow9));
                    Point minSpans = null;
                    if (launcherAppWidgetInfo != null) {
                        minSpans = launcherAppWidgetInfo.getMinSpans();
                    }
                    if (minSpans != null) {
                        dbEntry.minSpanX = minSpans.x > 0 ? minSpans.x : dbEntry.spanX;
                        dbEntry.minSpanY = minSpans.y > 0 ? minSpans.y : dbEntry.spanY;
                    } else {
                        dbEntry.minSpanY = 2;
                        dbEntry.minSpanX = 2;
                    }
                    if (dbEntry.minSpanX > this.mTrgX || dbEntry.minSpanY > this.mTrgY) {
                        arrayList = arrayList2;
                        try {
                            throw new Exception("Widget can't be resized down to fit the grid");
                        } catch (Exception e2) {
                            e = e2;
                            Log.d(TAG, "Removing item " + dbEntry.id, e);
                            this.mEntryToRemove.add(Long.valueOf(dbEntry.id));
                            columnIndexOrThrow = columnIndexOrThrow;
                            columnIndexOrThrow2 = columnIndexOrThrow2;
                            j2 = j;
                        }
                    }
                } catch (Exception e3) {
                    e = e3;
                    arrayList = arrayList2;
                    Log.d(TAG, "Removing item " + dbEntry.id, e);
                    this.mEntryToRemove.add(Long.valueOf(dbEntry.id));
                    columnIndexOrThrow = columnIndexOrThrow;
                    columnIndexOrThrow2 = columnIndexOrThrow2;
                    j2 = j;
                }
            } else if (i != 6) {
                switch (i) {
                    case 0:
                    case 1:
                        verifyIntent(cursorQueryWorkspace.getString(columnIndexOrThrow7));
                        dbEntry.weight = dbEntry.itemType == 0 ? WT_APPLICATION : 1.0f;
                        break;
                    case 2:
                        int folderItemsCount = getFolderItemsCount(dbEntry.id);
                        if (folderItemsCount == 0) {
                            throw new Exception("Folder is empty");
                        }
                        dbEntry.weight = 0.5f * folderItemsCount;
                        break;
                        break;
                    default:
                        throw new Exception("Invalid item type");
                }
                columnIndexOrThrow9 = i2;
            }
            arrayList = arrayList2;
            arrayList.add(dbEntry);
        }
        cursorQueryWorkspace.close();
        return arrayList;
    }

    private int getFolderItemsCount(long j) {
        Cursor cursorQueryWorkspace = queryWorkspace(new String[]{"_id", LauncherSettings.BaseLauncherColumns.INTENT}, "container = " + j);
        int i = 0;
        while (cursorQueryWorkspace.moveToNext()) {
            try {
                verifyIntent(cursorQueryWorkspace.getString(1));
                i++;
            } catch (Exception e) {
                this.mEntryToRemove.add(Long.valueOf(cursorQueryWorkspace.getLong(0)));
            }
        }
        cursorQueryWorkspace.close();
        return i;
    }

    protected Cursor queryWorkspace(String[] strArr, String str) {
        return this.mContext.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, strArr, str, null, null, null);
    }

    private void verifyIntent(String str) throws Exception {
        Intent uri = Intent.parseUri(str, 0);
        if (uri.getComponent() != null) {
            verifyPackage(uri.getComponent().getPackageName());
        } else if (uri.getPackage() != null) {
            verifyPackage(uri.getPackage());
        }
    }

    private void verifyPackage(String str) throws Exception {
        if (!this.mValidPackages.contains(str)) {
            throw new Exception("Package not available");
        }
    }

    protected static class DbEntry extends ItemInfo implements Comparable<DbEntry> {
        public float weight;

        public DbEntry copy() {
            DbEntry dbEntry = new DbEntry();
            dbEntry.copyFrom(this);
            dbEntry.weight = this.weight;
            dbEntry.minSpanX = this.minSpanX;
            dbEntry.minSpanY = this.minSpanY;
            return dbEntry;
        }

        @Override
        public int compareTo(DbEntry dbEntry) {
            if (this.itemType == 4) {
                if (dbEntry.itemType == 4) {
                    return (dbEntry.spanY * dbEntry.spanX) - (this.spanX * this.spanY);
                }
                return -1;
            }
            if (dbEntry.itemType == 4) {
                return 1;
            }
            return Float.compare(dbEntry.weight, this.weight);
        }

        public boolean columnsSame(DbEntry dbEntry) {
            return dbEntry.cellX == this.cellX && dbEntry.cellY == this.cellY && dbEntry.spanX == this.spanX && dbEntry.spanY == this.spanY && dbEntry.screenId == this.screenId;
        }

        public void addToContentValues(ContentValues contentValues) {
            contentValues.put(LauncherSettings.Favorites.SCREEN, Long.valueOf(this.screenId));
            contentValues.put(LauncherSettings.Favorites.CELLX, Integer.valueOf(this.cellX));
            contentValues.put(LauncherSettings.Favorites.CELLY, Integer.valueOf(this.cellY));
            contentValues.put(LauncherSettings.Favorites.SPANX, Integer.valueOf(this.spanX));
            contentValues.put(LauncherSettings.Favorites.SPANY, Integer.valueOf(this.spanY));
        }
    }

    private static ArrayList<DbEntry> deepCopy(ArrayList<DbEntry> arrayList) {
        ArrayList<DbEntry> arrayList2 = new ArrayList<>(arrayList.size());
        Iterator<DbEntry> it = arrayList.iterator();
        while (it.hasNext()) {
            arrayList2.add(it.next().copy());
        }
        return arrayList2;
    }

    private static Point parsePoint(String str) {
        String[] strArrSplit = str.split(",");
        return new Point(Integer.parseInt(strArrSplit[0]), Integer.parseInt(strArrSplit[1]));
    }

    private static String getPointString(int i, int i2) {
        return String.format(Locale.ENGLISH, "%d,%d", Integer.valueOf(i), Integer.valueOf(i2));
    }

    public static void markForMigration(Context context, int i, int i2, int i3) {
        Utilities.getPrefs(context).edit().putString(KEY_MIGRATION_SRC_WORKSPACE_SIZE, getPointString(i, i2)).putInt(KEY_MIGRATION_SRC_HOTSEAT_COUNT, i3).apply();
    }

    public static boolean migrateGridIfNeeded(Context context) {
        boolean zMigrateHotseat;
        SharedPreferences prefs = Utilities.getPrefs(context);
        InvariantDeviceProfile idp = LauncherAppState.getIDP(context);
        String pointString = getPointString(idp.numColumns, idp.numRows);
        if (pointString.equals(prefs.getString(KEY_MIGRATION_SRC_WORKSPACE_SIZE, "")) && idp.numHotseatIcons == prefs.getInt(KEY_MIGRATION_SRC_HOTSEAT_COUNT, idp.numHotseatIcons)) {
            return true;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        try {
            try {
                HashSet<String> validPackages = getValidPackages(context);
                int i = prefs.getInt(KEY_MIGRATION_SRC_HOTSEAT_COUNT, idp.numHotseatIcons);
                zMigrateHotseat = i != idp.numHotseatIcons ? new GridSizeMigrationTask(context, LauncherAppState.getIDP(context), validPackages, i, idp.numHotseatIcons).migrateHotseat() : false;
                if (new MultiStepMigrationTask(validPackages, context).migrate(parsePoint(prefs.getString(KEY_MIGRATION_SRC_WORKSPACE_SIZE, pointString)), new Point(idp.numColumns, idp.numRows))) {
                    zMigrateHotseat = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during grid migration", e);
                Log.v(TAG, "Workspace migration completed in " + (System.currentTimeMillis() - jCurrentTimeMillis));
                prefs.edit().putString(KEY_MIGRATION_SRC_WORKSPACE_SIZE, pointString).putInt(KEY_MIGRATION_SRC_HOTSEAT_COUNT, idp.numHotseatIcons).apply();
                return false;
            }
        } catch (Throwable th) {
            Log.v(TAG, "Workspace migration completed in " + (System.currentTimeMillis() - jCurrentTimeMillis));
            prefs.edit().putString(KEY_MIGRATION_SRC_WORKSPACE_SIZE, pointString).putInt(KEY_MIGRATION_SRC_HOTSEAT_COUNT, idp.numHotseatIcons).apply();
            throw th;
        }
        if (zMigrateHotseat) {
            Cursor cursorQuery = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, null, null, null, null);
            boolean zMoveToNext = cursorQuery.moveToNext();
            cursorQuery.close();
            if (!zMoveToNext) {
                throw new Exception("Removed every thing during grid resize");
            }
            Log.v(TAG, "Workspace migration completed in " + (System.currentTimeMillis() - jCurrentTimeMillis));
            prefs.edit().putString(KEY_MIGRATION_SRC_WORKSPACE_SIZE, pointString).putInt(KEY_MIGRATION_SRC_HOTSEAT_COUNT, idp.numHotseatIcons).apply();
            throw th;
        }
        Log.v(TAG, "Workspace migration completed in " + (System.currentTimeMillis() - jCurrentTimeMillis));
        prefs.edit().putString(KEY_MIGRATION_SRC_WORKSPACE_SIZE, pointString).putInt(KEY_MIGRATION_SRC_HOTSEAT_COUNT, idp.numHotseatIcons).apply();
        return true;
    }

    protected static HashSet<String> getValidPackages(Context context) {
        HashSet<String> hashSet = new HashSet<>();
        Iterator<PackageInfo> it = context.getPackageManager().getInstalledPackages(8192).iterator();
        while (it.hasNext()) {
            hashSet.add(it.next().packageName);
        }
        hashSet.addAll(PackageInstallerCompat.getInstance(context).updateAndGetActiveSessionCache().keySet());
        return hashSet;
    }

    public static LongArrayMap<Object> removeBrokenHotseatItems(Context context) throws Exception {
        GridSizeMigrationTask gridSizeMigrationTask = new GridSizeMigrationTask(context, LauncherAppState.getIDP(context), getValidPackages(context), Integer.MAX_VALUE, Integer.MAX_VALUE);
        ArrayList<DbEntry> arrayListLoadHotseatEntries = gridSizeMigrationTask.loadHotseatEntries();
        gridSizeMigrationTask.applyOperations();
        LongArrayMap<Object> longArrayMap = new LongArrayMap<>();
        for (DbEntry dbEntry : arrayListLoadHotseatEntries) {
            longArrayMap.put(dbEntry.screenId, dbEntry);
        }
        return longArrayMap;
    }

    protected static class MultiStepMigrationTask {
        private final Context mContext;
        private final HashSet<String> mValidPackages;

        public MultiStepMigrationTask(HashSet<String> hashSet, Context context) {
            this.mValidPackages = hashSet;
            this.mContext = context;
        }

        public boolean migrate(Point point, Point point2) throws Exception {
            boolean z = false;
            if (!point2.equals(point)) {
                if (point.x < point2.x) {
                    point.x = point2.x;
                }
                if (point.y < point2.y) {
                    point.y = point2.y;
                }
                while (!point2.equals(point)) {
                    Point point3 = new Point(point);
                    if (point2.x < point3.x) {
                        point3.x--;
                    }
                    if (point2.y < point3.y) {
                        point3.y--;
                    }
                    if (runStepTask(point, point3)) {
                        z = true;
                    }
                    point.set(point3.x, point3.y);
                }
            }
            return z;
        }

        protected boolean runStepTask(Point point, Point point2) throws Exception {
            return new GridSizeMigrationTask(this.mContext, LauncherAppState.getIDP(this.mContext), this.mValidPackages, point, point2).migrateWorkspace();
        }
    }
}
