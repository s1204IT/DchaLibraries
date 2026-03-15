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
import com.android.launcher3.LauncherSettings$Favorites;
import com.android.launcher3.LauncherSettings$WorkspaceScreens;
import com.android.launcher3.Utilities;
import com.android.launcher3.backup.nano.BackupProtos$DeviceProfieData;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.util.LongArrayMap;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

public class GridSizeMigrationTask {
    public static boolean ENABLED = Utilities.ATLEAST_N;
    private final ArrayList<DbEntry> mCarryOver;
    private final Context mContext;
    private final int mDestAllAppsRank;
    private final int mDestHotseatSize;
    private final ArrayList<Long> mEntryToRemove;
    private final InvariantDeviceProfile mIdp;
    private final boolean mShouldRemoveX;
    private final boolean mShouldRemoveY;
    private final int mSrcAllAppsRank;
    private final int mSrcHotseatSize;
    private final int mSrcX;
    private final int mSrcY;
    private final ContentValues mTempValues;
    private final int mTrgX;
    private final int mTrgY;
    private final ArrayList<ContentProviderOperation> mUpdateOperations;
    private final HashSet<String> mValidPackages;
    private final HashMap<String, Point> mWidgetMinSize;

    protected GridSizeMigrationTask(Context context, InvariantDeviceProfile idp, HashSet<String> validPackages, HashMap<String, Point> widgetMinSize, Point sourceSize, Point targetSize) {
        this.mWidgetMinSize = new HashMap<>();
        this.mTempValues = new ContentValues();
        this.mEntryToRemove = new ArrayList<>();
        this.mUpdateOperations = new ArrayList<>();
        this.mCarryOver = new ArrayList<>();
        this.mContext = context;
        this.mValidPackages = validPackages;
        this.mWidgetMinSize.putAll(widgetMinSize);
        this.mIdp = idp;
        this.mSrcX = sourceSize.x;
        this.mSrcY = sourceSize.y;
        this.mTrgX = targetSize.x;
        this.mTrgY = targetSize.y;
        this.mShouldRemoveX = this.mTrgX < this.mSrcX;
        this.mShouldRemoveY = this.mTrgY < this.mSrcY;
        this.mDestAllAppsRank = -1;
        this.mDestHotseatSize = -1;
        this.mSrcAllAppsRank = -1;
        this.mSrcHotseatSize = -1;
    }

    protected GridSizeMigrationTask(Context context, InvariantDeviceProfile idp, HashSet<String> validPackages, int srcHotseatSize, int srcAllAppsRank, int destHotseatSize, int destAllAppsRank) {
        this.mWidgetMinSize = new HashMap<>();
        this.mTempValues = new ContentValues();
        this.mEntryToRemove = new ArrayList<>();
        this.mUpdateOperations = new ArrayList<>();
        this.mCarryOver = new ArrayList<>();
        this.mContext = context;
        this.mIdp = idp;
        this.mValidPackages = validPackages;
        this.mSrcHotseatSize = srcHotseatSize;
        this.mSrcAllAppsRank = srcAllAppsRank;
        this.mDestHotseatSize = destHotseatSize;
        this.mDestAllAppsRank = destAllAppsRank;
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
            Log.d("GridSizeMigrationTask", "Removing items: " + TextUtils.join(", ", this.mEntryToRemove));
            this.mContext.getContentResolver().delete(LauncherSettings$Favorites.CONTENT_URI, Utilities.createDbSelectionQuery("_id", this.mEntryToRemove), null);
        }
        return (this.mUpdateOperations.isEmpty() && this.mEntryToRemove.isEmpty()) ? false : true;
    }

    protected boolean migrateHotseat() throws Exception {
        ArrayList<DbEntry> items = loadHotseatEntries();
        int requiredCount = this.mDestHotseatSize - 1;
        while (items.size() > requiredCount) {
            DbEntry toRemove = items.get(items.size() / 2);
            for (DbEntry entry : items) {
                if (entry.weight < toRemove.weight) {
                    toRemove = entry;
                }
            }
            this.mEntryToRemove.add(Long.valueOf(toRemove.id));
            items.remove(toRemove);
        }
        int newScreenId = 0;
        for (DbEntry entry2 : items) {
            if (entry2.screenId != newScreenId) {
                entry2.screenId = newScreenId;
                entry2.cellX = newScreenId;
                entry2.cellY = 0;
                update(entry2);
            }
            newScreenId++;
            if (newScreenId == this.mDestAllAppsRank) {
                newScreenId++;
            }
        }
        return applyOperations();
    }

    protected boolean migrateWorkspace() throws Exception {
        ArrayList<Long> allScreens = LauncherModel.loadWorkspaceScreensDb(this.mContext);
        if (allScreens.isEmpty()) {
            throw new Exception("Unable to get workspace screens");
        }
        Iterator screenId$iterator = allScreens.iterator();
        while (screenId$iterator.hasNext()) {
            long screenId = ((Long) screenId$iterator.next()).longValue();
            Log.d("GridSizeMigrationTask", "Migrating " + screenId);
            migrateScreen(screenId);
        }
        if (!this.mCarryOver.isEmpty()) {
            LongArrayMap<DbEntry> itemMap = new LongArrayMap<>();
            for (DbEntry e : this.mCarryOver) {
                itemMap.put(e.id, e);
            }
            do {
                OptimalPlacementSolution placement = new OptimalPlacementSolution((boolean[][]) Array.newInstance((Class<?>) Boolean.TYPE, this.mTrgX, this.mTrgY), deepCopy(this.mCarryOver), true);
                placement.find();
                if (placement.finalPlacedItems.size() > 0) {
                    long newScreenId = LauncherAppState.getLauncherProvider().generateNewScreenId();
                    allScreens.add(Long.valueOf(newScreenId));
                    for (DbEntry item : placement.finalPlacedItems) {
                        if (!this.mCarryOver.remove(itemMap.get(item.id))) {
                            throw new Exception("Unable to find matching items");
                        }
                        item.screenId = newScreenId;
                        update(item);
                    }
                } else {
                    throw new Exception("None of the items can be placed on an empty screen");
                }
            } while (!this.mCarryOver.isEmpty());
            Uri uri = LauncherSettings$WorkspaceScreens.CONTENT_URI;
            this.mUpdateOperations.add(ContentProviderOperation.newDelete(uri).build());
            int count = allScreens.size();
            for (int i = 0; i < count; i++) {
                ContentValues v = new ContentValues();
                v.put("_id", Long.valueOf(allScreens.get(i).longValue()));
                v.put("screenRank", Integer.valueOf(i));
                this.mUpdateOperations.add(ContentProviderOperation.newInsert(uri).withValues(v).build());
            }
        }
        return applyOperations();
    }

    private void migrateScreen(long screenId) {
        ArrayList<DbEntry> items = loadWorkspaceEntries(screenId);
        int removedCol = Integer.MAX_VALUE;
        int removedRow = Integer.MAX_VALUE;
        float removeWt = Float.MAX_VALUE;
        float moveWt = Float.MAX_VALUE;
        float[] outLoss = new float[2];
        ArrayList<DbEntry> finalItems = null;
        for (int x = 0; x < this.mSrcX; x++) {
            for (int y = 0; y < this.mSrcY; y++) {
                ArrayList<DbEntry> itemsOnScreen = tryRemove(x, y, deepCopy(items), outLoss);
                if (outLoss[0] < removeWt || (outLoss[0] == removeWt && outLoss[1] < moveWt)) {
                    removeWt = outLoss[0];
                    moveWt = outLoss[1];
                    if (this.mShouldRemoveX) {
                        removedCol = x;
                    }
                    if (this.mShouldRemoveY) {
                        removedRow = y;
                    }
                    finalItems = itemsOnScreen;
                }
                if (!this.mShouldRemoveY) {
                    break;
                }
            }
            if (!this.mShouldRemoveX) {
                break;
            }
        }
        Log.d("GridSizeMigrationTask", String.format("Removing row %d, column %d on screen %d", Integer.valueOf(removedRow), Integer.valueOf(removedCol), Long.valueOf(screenId)));
        LongArrayMap<DbEntry> itemMap = new LongArrayMap<>();
        for (DbEntry e : deepCopy(items)) {
            itemMap.put(e.id, e);
        }
        for (DbEntry item : finalItems) {
            DbEntry org = itemMap.get(item.id);
            itemMap.remove(item.id);
            if (!item.columnsSame(org)) {
                update(item);
            }
        }
        Iterator item$iterator = itemMap.iterator();
        while (item$iterator.hasNext()) {
            this.mCarryOver.add((DbEntry) item$iterator.next());
        }
        if (this.mCarryOver.isEmpty() || removeWt != 0.0f) {
            return;
        }
        boolean[][] occupied = (boolean[][]) Array.newInstance((Class<?>) Boolean.TYPE, this.mTrgX, this.mTrgY);
        Iterator item$iterator2 = finalItems.iterator();
        while (item$iterator2.hasNext()) {
            markCells(occupied, (DbEntry) item$iterator2.next(), true);
        }
        OptimalPlacementSolution placement = new OptimalPlacementSolution(occupied, deepCopy(this.mCarryOver), true);
        placement.find();
        if (placement.lowestWeightLoss != 0.0f) {
            return;
        }
        for (DbEntry item2 : placement.finalPlacedItems) {
            item2.screenId = screenId;
            update(item2);
        }
        this.mCarryOver.clear();
    }

    private void update(DbEntry item) {
        this.mTempValues.clear();
        item.addToContentValues(this.mTempValues);
        this.mUpdateOperations.add(ContentProviderOperation.newUpdate(LauncherSettings$Favorites.getContentUri(item.id)).withValues(this.mTempValues).build());
    }

    private ArrayList<DbEntry> tryRemove(int col, int row, ArrayList<DbEntry> items, float[] outLoss) {
        boolean[][] occupied = (boolean[][]) Array.newInstance((Class<?>) Boolean.TYPE, this.mTrgX, this.mTrgY);
        if (!this.mShouldRemoveX) {
            col = Integer.MAX_VALUE;
        }
        if (!this.mShouldRemoveY) {
            row = Integer.MAX_VALUE;
        }
        ArrayList<DbEntry> finalItems = new ArrayList<>();
        ArrayList<DbEntry> removedItems = new ArrayList<>();
        for (DbEntry item : items) {
            if ((item.cellX <= col && item.spanX + item.cellX > col) || (item.cellY <= row && item.spanY + item.cellY > row)) {
                removedItems.add(item);
                if (item.cellX >= col) {
                    item.cellX--;
                }
                if (item.cellY >= row) {
                    item.cellY--;
                }
            } else {
                if (item.cellX > col) {
                    item.cellX--;
                }
                if (item.cellY > row) {
                    item.cellY--;
                }
                finalItems.add(item);
                markCells(occupied, item, true);
            }
        }
        OptimalPlacementSolution placement = new OptimalPlacementSolution(this, occupied, removedItems);
        placement.find();
        finalItems.addAll(placement.finalPlacedItems);
        outLoss[0] = placement.lowestWeightLoss;
        outLoss[1] = placement.lowestMoveCost;
        return finalItems;
    }

    private void markCells(boolean[][] occupied, DbEntry item, boolean val) {
        for (int i = item.cellX; i < item.cellX + item.spanX; i++) {
            for (int j = item.cellY; j < item.cellY + item.spanY; j++) {
                occupied[i][j] = val;
            }
        }
    }

    private boolean isVacant(boolean[][] occupied, int x, int y, int w, int h) {
        if (x + w > this.mTrgX || y + h > this.mTrgY) {
            return false;
        }
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (occupied[i + x][j + y]) {
                    return false;
                }
            }
        }
        return true;
    }

    private class OptimalPlacementSolution {
        ArrayList<DbEntry> finalPlacedItems;
        private final boolean ignoreMove;
        private final ArrayList<DbEntry> itemsToPlace;
        float lowestMoveCost;
        float lowestWeightLoss;
        private final boolean[][] occupied;

        public OptimalPlacementSolution(GridSizeMigrationTask this$0, boolean[][] occupied, ArrayList<DbEntry> itemsToPlace) {
            this(occupied, itemsToPlace, false);
        }

        public OptimalPlacementSolution(boolean[][] occupied, ArrayList<DbEntry> itemsToPlace, boolean ignoreMove) {
            this.lowestWeightLoss = Float.MAX_VALUE;
            this.lowestMoveCost = Float.MAX_VALUE;
            this.occupied = occupied;
            this.itemsToPlace = itemsToPlace;
            this.ignoreMove = ignoreMove;
            Collections.sort(this.itemsToPlace);
        }

        public void find() {
            find(0, 0.0f, 0.0f, new ArrayList<>());
        }

        public void find(int index, float weightLoss, float moveCost, ArrayList<DbEntry> itemsPlaced) {
            if (weightLoss < this.lowestWeightLoss) {
                if (weightLoss == this.lowestWeightLoss && moveCost >= this.lowestMoveCost) {
                    return;
                }
                if (index >= this.itemsToPlace.size()) {
                    this.lowestWeightLoss = weightLoss;
                    this.lowestMoveCost = moveCost;
                    this.finalPlacedItems = GridSizeMigrationTask.deepCopy(itemsPlaced);
                    return;
                }
                DbEntry me = this.itemsToPlace.get(index);
                int myX = me.cellX;
                int myY = me.cellY;
                ArrayList<DbEntry> itemsIncludingMe = new ArrayList<>(itemsPlaced.size() + 1);
                itemsIncludingMe.addAll(itemsPlaced);
                itemsIncludingMe.add(me);
                if (me.spanX > 1 || me.spanY > 1) {
                    int myW = me.spanX;
                    int myH = me.spanY;
                    for (int y = 0; y < GridSizeMigrationTask.this.mTrgY; y++) {
                        for (int x = 0; x < GridSizeMigrationTask.this.mTrgX; x++) {
                            float newMoveCost = moveCost;
                            if (x != myX) {
                                me.cellX = x;
                                newMoveCost = moveCost + 1.0f;
                            }
                            if (y != myY) {
                                me.cellY = y;
                                newMoveCost += 1.0f;
                            }
                            if (this.ignoreMove) {
                                newMoveCost = moveCost;
                            }
                            if (GridSizeMigrationTask.this.isVacant(this.occupied, x, y, myW, myH)) {
                                GridSizeMigrationTask.this.markCells(this.occupied, me, true);
                                find(index + 1, weightLoss, newMoveCost, itemsIncludingMe);
                                GridSizeMigrationTask.this.markCells(this.occupied, me, false);
                            }
                            if (myW > me.minSpanX) {
                                if (GridSizeMigrationTask.this.isVacant(this.occupied, x, y, myW - 1, myH)) {
                                    me.spanX--;
                                    GridSizeMigrationTask.this.markCells(this.occupied, me, true);
                                    find(index + 1, weightLoss, 1.0f + newMoveCost, itemsIncludingMe);
                                    GridSizeMigrationTask.this.markCells(this.occupied, me, false);
                                    me.spanX++;
                                }
                            }
                            if (myH > me.minSpanY) {
                                if (GridSizeMigrationTask.this.isVacant(this.occupied, x, y, myW, myH - 1)) {
                                    me.spanY--;
                                    GridSizeMigrationTask.this.markCells(this.occupied, me, true);
                                    find(index + 1, weightLoss, 1.0f + newMoveCost, itemsIncludingMe);
                                    GridSizeMigrationTask.this.markCells(this.occupied, me, false);
                                    me.spanY++;
                                }
                            }
                            if (myH > me.minSpanY && myW > me.minSpanX) {
                                if (GridSizeMigrationTask.this.isVacant(this.occupied, x, y, myW - 1, myH - 1)) {
                                    me.spanX--;
                                    me.spanY--;
                                    GridSizeMigrationTask.this.markCells(this.occupied, me, true);
                                    find(index + 1, weightLoss, 2.0f + newMoveCost, itemsIncludingMe);
                                    GridSizeMigrationTask.this.markCells(this.occupied, me, false);
                                    me.spanX++;
                                    me.spanY++;
                                }
                            }
                            me.cellX = myX;
                            me.cellY = myY;
                        }
                    }
                    find(index + 1, me.weight + weightLoss, moveCost, itemsPlaced);
                    return;
                }
                int newDistance = Integer.MAX_VALUE;
                int newX = Integer.MAX_VALUE;
                int newY = Integer.MAX_VALUE;
                for (int y2 = 0; y2 < GridSizeMigrationTask.this.mTrgY; y2++) {
                    for (int x2 = 0; x2 < GridSizeMigrationTask.this.mTrgX; x2++) {
                        if (!this.occupied[x2][y2]) {
                            int dist = this.ignoreMove ? 0 : ((me.cellX - x2) * (me.cellX - x2)) + ((me.cellY - y2) * (me.cellY - y2));
                            if (dist < newDistance) {
                                newX = x2;
                                newY = y2;
                                newDistance = dist;
                            }
                        }
                    }
                }
                if (newX < GridSizeMigrationTask.this.mTrgX && newY < GridSizeMigrationTask.this.mTrgY) {
                    float newMoveCost2 = moveCost;
                    if (newX != myX) {
                        me.cellX = newX;
                        newMoveCost2 = moveCost + 1.0f;
                    }
                    if (newY != myY) {
                        me.cellY = newY;
                        newMoveCost2 += 1.0f;
                    }
                    if (this.ignoreMove) {
                        newMoveCost2 = moveCost;
                    }
                    GridSizeMigrationTask.this.markCells(this.occupied, me, true);
                    find(index + 1, weightLoss, newMoveCost2, itemsIncludingMe);
                    GridSizeMigrationTask.this.markCells(this.occupied, me, false);
                    me.cellX = myX;
                    me.cellY = myY;
                    if (index + 1 >= this.itemsToPlace.size() || this.itemsToPlace.get(index + 1).weight < me.weight || this.ignoreMove) {
                        return;
                    }
                    find(index + 1, me.weight + weightLoss, moveCost, itemsPlaced);
                    return;
                }
                for (int i = index + 1; i < this.itemsToPlace.size(); i++) {
                    weightLoss += this.itemsToPlace.get(i).weight;
                }
                find(this.itemsToPlace.size(), me.weight + weightLoss, moveCost, itemsPlaced);
            }
        }
    }

    private ArrayList<DbEntry> loadHotseatEntries() {
        Cursor c = this.mContext.getContentResolver().query(LauncherSettings$Favorites.CONTENT_URI, new String[]{"_id", "itemType", "intent", "screen"}, "container = -101", null, null, null);
        int indexId = c.getColumnIndexOrThrow("_id");
        int indexItemType = c.getColumnIndexOrThrow("itemType");
        int indexIntent = c.getColumnIndexOrThrow("intent");
        int indexScreen = c.getColumnIndexOrThrow("screen");
        ArrayList<DbEntry> entries = new ArrayList<>();
        while (c.moveToNext()) {
            DbEntry entry = new DbEntry();
            entry.id = c.getLong(indexId);
            entry.itemType = c.getInt(indexItemType);
            entry.screenId = c.getLong(indexScreen);
            if (entry.screenId >= this.mSrcHotseatSize) {
                this.mEntryToRemove.add(Long.valueOf(entry.id));
            } else {
                try {
                    switch (entry.itemType) {
                        case PackageInstallerCompat.STATUS_INSTALLED:
                        case PackageInstallerCompat.STATUS_INSTALLING:
                            verifyIntent(c.getString(indexIntent));
                            entry.weight = entry.itemType == 1 ? 1.0f : 0.8f;
                            break;
                        case PackageInstallerCompat.STATUS_FAILED:
                            int total = getFolderItemsCount(entry.id);
                            if (total == 0) {
                                throw new Exception("Folder is empty");
                            }
                            entry.weight = total * 0.5f;
                            break;
                            break;
                        default:
                            throw new Exception("Invalid item type");
                    }
                    entries.add(entry);
                } catch (Exception e) {
                    Log.d("GridSizeMigrationTask", "Removing item " + entry.id, e);
                    this.mEntryToRemove.add(Long.valueOf(entry.id));
                }
            }
        }
        c.close();
        return entries;
    }

    private ArrayList<DbEntry> loadWorkspaceEntries(long screen) {
        Cursor c = this.mContext.getContentResolver().query(LauncherSettings$Favorites.CONTENT_URI, new String[]{"_id", "itemType", "cellX", "cellY", "spanX", "spanY", "intent", "appWidgetProvider", "appWidgetId"}, "container = -100 AND screen = " + screen, null, null, null);
        int indexId = c.getColumnIndexOrThrow("_id");
        int indexItemType = c.getColumnIndexOrThrow("itemType");
        int indexCellX = c.getColumnIndexOrThrow("cellX");
        int indexCellY = c.getColumnIndexOrThrow("cellY");
        int indexSpanX = c.getColumnIndexOrThrow("spanX");
        int indexSpanY = c.getColumnIndexOrThrow("spanY");
        int indexIntent = c.getColumnIndexOrThrow("intent");
        int indexAppWidgetProvider = c.getColumnIndexOrThrow("appWidgetProvider");
        int indexAppWidgetId = c.getColumnIndexOrThrow("appWidgetId");
        ArrayList<DbEntry> entries = new ArrayList<>();
        while (c.moveToNext()) {
            DbEntry entry = new DbEntry();
            entry.id = c.getLong(indexId);
            entry.itemType = c.getInt(indexItemType);
            entry.cellX = c.getInt(indexCellX);
            entry.cellY = c.getInt(indexCellY);
            entry.spanX = c.getInt(indexSpanX);
            entry.spanY = c.getInt(indexSpanY);
            entry.screenId = screen;
            try {
                switch (entry.itemType) {
                    case PackageInstallerCompat.STATUS_INSTALLED:
                    case PackageInstallerCompat.STATUS_INSTALLING:
                        verifyIntent(c.getString(indexIntent));
                        entry.weight = entry.itemType == 1 ? 1.0f : 0.8f;
                        break;
                    case PackageInstallerCompat.STATUS_FAILED:
                        int total = getFolderItemsCount(entry.id);
                        if (total == 0) {
                            throw new Exception("Folder is empty");
                        }
                        entry.weight = total * 0.5f;
                        break;
                        break;
                    case 3:
                    default:
                        throw new Exception("Invalid item type");
                    case 4:
                        String provider = c.getString(indexAppWidgetProvider);
                        ComponentName cn = ComponentName.unflattenFromString(provider);
                        verifyPackage(cn.getPackageName());
                        entry.weight = Math.max(2.0f, entry.spanX * 0.6f * entry.spanY);
                        int widgetId = c.getInt(indexAppWidgetId);
                        LauncherAppWidgetProviderInfo pInfo = AppWidgetManagerCompat.getInstance(this.mContext).getLauncherAppWidgetInfo(widgetId);
                        Point spans = pInfo == null ? this.mWidgetMinSize.get(provider) : pInfo.getMinSpans(this.mIdp, this.mContext);
                        if (spans != null) {
                            entry.minSpanX = spans.x > 0 ? spans.x : entry.spanX;
                            entry.minSpanY = spans.y > 0 ? spans.y : entry.spanY;
                        } else {
                            entry.minSpanY = 2;
                            entry.minSpanX = 2;
                        }
                        if (entry.minSpanX > this.mTrgX || entry.minSpanY > this.mTrgY) {
                            throw new Exception("Widget can't be resized down to fit the grid");
                        }
                        break;
                }
                entries.add(entry);
            } catch (Exception e) {
                Log.d("GridSizeMigrationTask", "Removing item " + entry.id, e);
                this.mEntryToRemove.add(Long.valueOf(entry.id));
            }
        }
        c.close();
        return entries;
    }

    private int getFolderItemsCount(long folderId) {
        Cursor c = this.mContext.getContentResolver().query(LauncherSettings$Favorites.CONTENT_URI, new String[]{"_id", "intent"}, "container = " + folderId, null, null, null);
        int total = 0;
        while (c.moveToNext()) {
            try {
                verifyIntent(c.getString(1));
                total++;
            } catch (Exception e) {
                this.mEntryToRemove.add(Long.valueOf(c.getLong(0)));
            }
        }
        c.close();
        return total;
    }

    private void verifyIntent(String intentStr) throws Exception {
        Intent intent = Intent.parseUri(intentStr, 0);
        if (intent.getComponent() != null) {
            verifyPackage(intent.getComponent().getPackageName());
        } else {
            if (intent.getPackage() == null) {
                return;
            }
            verifyPackage(intent.getPackage());
        }
    }

    private void verifyPackage(String packageName) throws Exception {
        if (this.mValidPackages.contains(packageName)) {
        } else {
            throw new Exception("Package not available");
        }
    }

    private static class DbEntry extends ItemInfo implements Comparable<DbEntry> {
        public float weight;

        public DbEntry copy() {
            DbEntry entry = new DbEntry();
            entry.copyFrom(this);
            entry.weight = this.weight;
            entry.minSpanX = this.minSpanX;
            entry.minSpanY = this.minSpanY;
            return entry;
        }

        @Override
        public int compareTo(DbEntry another) {
            if (this.itemType == 4) {
                if (another.itemType == 4) {
                    return (another.spanY * another.spanX) - (this.spanX * this.spanY);
                }
                return -1;
            }
            if (another.itemType == 4) {
                return 1;
            }
            return Float.compare(another.weight, this.weight);
        }

        public boolean columnsSame(DbEntry org) {
            return org.cellX == this.cellX && org.cellY == this.cellY && org.spanX == this.spanX && org.spanY == this.spanY && org.screenId == this.screenId;
        }

        public void addToContentValues(ContentValues values) {
            values.put("screen", Long.valueOf(this.screenId));
            values.put("cellX", Integer.valueOf(this.cellX));
            values.put("cellY", Integer.valueOf(this.cellY));
            values.put("spanX", Integer.valueOf(this.spanX));
            values.put("spanY", Integer.valueOf(this.spanY));
        }
    }

    private static ArrayList<DbEntry> deepCopy(ArrayList<DbEntry> src) {
        ArrayList<DbEntry> dup = new ArrayList<>(src.size());
        for (DbEntry e : src) {
            dup.add(e.copy());
        }
        return dup;
    }

    private static Point parsePoint(String point) {
        String[] split = point.split(",");
        return new Point(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }

    private static String getPointString(int x, int y) {
        return String.format(Locale.ENGLISH, "%d,%d", Integer.valueOf(x), Integer.valueOf(y));
    }

    public static void markForMigration(Context context, HashSet<String> widgets, BackupProtos$DeviceProfieData srcProfile) {
        Utilities.getPrefs(context).edit().putString("migration_src_workspace_size", getPointString((int) srcProfile.desktopCols, (int) srcProfile.desktopRows)).putString("migration_src_hotseat_size", getPointString((int) srcProfile.hotseatCount, srcProfile.allappsRank)).putStringSet("migration_widget_min_size", widgets).apply();
    }

    public static boolean migrateGridIfNeeded(Context context) {
        String str;
        StringBuilder sb;
        String str2;
        StringBuilder sbAppend;
        long jCurrentTimeMillis;
        long j;
        StringBuilder sbAppend2;
        String string;
        SharedPreferences.Editor editorEdit;
        String str3;
        SharedPreferences.Editor editorPutString;
        String str4;
        SharedPreferences.Editor editorPutString2;
        String str5;
        SharedPreferences.Editor editorRemove;
        SharedPreferences prefs = Utilities.getPrefs(context);
        InvariantDeviceProfile idp = LauncherAppState.getInstance().getInvariantDeviceProfile();
        String gridSizeString = getPointString(idp.numColumns, idp.numRows);
        String hotseatSizeString = getPointString(idp.numHotseatIcons, idp.hotseatAllAppsRank);
        if (gridSizeString.equals(prefs.getString("migration_src_workspace_size", "")) && hotseatSizeString.equals(prefs.getString("migration_src_hotseat_size", ""))) {
            return true;
        }
        long migrationStartTime = System.currentTimeMillis();
        try {
            HashSet validPackages = new HashSet();
            for (PackageInfo info : context.getPackageManager().getInstalledPackages(0)) {
                validPackages.add(info.packageName);
            }
            validPackages.addAll(PackageInstallerCompat.getInstance(context).updateAndGetActiveSessionCache().keySet());
            Point srcHotseatSize = parsePoint(prefs.getString("migration_src_hotseat_size", hotseatSizeString));
            boolean dbChanged = (srcHotseatSize.x == idp.numHotseatIcons && srcHotseatSize.y == idp.hotseatAllAppsRank) ? false : new GridSizeMigrationTask(context, LauncherAppState.getInstance().getInvariantDeviceProfile(), validPackages, srcHotseatSize.x, srcHotseatSize.y, idp.numHotseatIcons, idp.hotseatAllAppsRank).migrateHotseat();
            Point targetSize = new Point(idp.numColumns, idp.numRows);
            Point sourceSize = parsePoint(prefs.getString("migration_src_workspace_size", gridSizeString));
            if (!targetSize.equals(sourceSize)) {
                ArrayList<Point> gridSizeSteps = new ArrayList<>();
                gridSizeSteps.add(new Point(3, 2));
                gridSizeSteps.add(new Point(3, 3));
                gridSizeSteps.add(new Point(4, 3));
                gridSizeSteps.add(new Point(4, 4));
                gridSizeSteps.add(new Point(5, 5));
                gridSizeSteps.add(new Point(6, 5));
                gridSizeSteps.add(new Point(6, 6));
                gridSizeSteps.add(new Point(7, 7));
                int sourceSizeIndex = gridSizeSteps.indexOf(sourceSize);
                int targetSizeIndex = gridSizeSteps.indexOf(targetSize);
                if (sourceSizeIndex <= -1 || targetSizeIndex <= -1) {
                    throw new Exception("Unable to migrate grid size from " + sourceSize + " to " + targetSize);
                }
                HashMap<String, Point> widgetMinSize = new HashMap<>();
                for (String s : Utilities.getPrefs(context).getStringSet("migration_widget_min_size", Collections.emptySet())) {
                    String[] parts = s.split("#");
                    widgetMinSize.put(parts[0], parsePoint(parts[1]));
                }
                while (targetSizeIndex < sourceSizeIndex) {
                    Point stepTargetSize = gridSizeSteps.get(sourceSizeIndex - 1);
                    Point stepSourceSize = gridSizeSteps.get(sourceSizeIndex);
                    if (new GridSizeMigrationTask(context, LauncherAppState.getInstance().getInvariantDeviceProfile(), validPackages, widgetMinSize, stepSourceSize, stepTargetSize).migrateWorkspace()) {
                        dbChanged = true;
                    }
                    sourceSizeIndex--;
                }
            }
            if (dbChanged) {
                Cursor c = context.getContentResolver().query(LauncherSettings$Favorites.CONTENT_URI, null, null, null, null);
                boolean hasData = c.moveToNext();
                c.close();
                if (!hasData) {
                    throw new Exception("Removed every thing during grid resize");
                }
            }
            return true;
        } catch (Exception e) {
            Log.e("GridSizeMigrationTask", "Error during grid migration", e);
            return false;
        } finally {
            Log.v("GridSizeMigrationTask", "Workspace migration completed in " + (System.currentTimeMillis() - migrationStartTime));
            prefs.edit().putString("migration_src_workspace_size", gridSizeString).putString("migration_src_hotseat_size", hotseatSizeString).remove("migration_widget_min_size").apply();
        }
    }
}
