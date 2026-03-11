package com.android.launcher3.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.Utilities;
import com.android.launcher3.model.GridSizeMigrationTask;
import com.android.launcher3.util.LongArrayMap;
import java.util.ArrayList;
import java.util.Iterator;

public class LossyScreenMigrationTask extends GridSizeMigrationTask {
    private final SQLiteDatabase mDb;
    private final LongArrayMap<GridSizeMigrationTask.DbEntry> mOriginalItems;
    private final LongArrayMap<GridSizeMigrationTask.DbEntry> mUpdates;

    protected LossyScreenMigrationTask(Context context, InvariantDeviceProfile invariantDeviceProfile, SQLiteDatabase sQLiteDatabase) {
        super(context, invariantDeviceProfile, getValidPackages(context), new Point(invariantDeviceProfile.numColumns, invariantDeviceProfile.numRows + 1), new Point(invariantDeviceProfile.numColumns, invariantDeviceProfile.numRows));
        this.mDb = sQLiteDatabase;
        this.mOriginalItems = new LongArrayMap<>();
        this.mUpdates = new LongArrayMap<>();
    }

    @Override
    protected Cursor queryWorkspace(String[] strArr, String str) {
        return this.mDb.query(LauncherSettings.Favorites.TABLE_NAME, strArr, str, null, null, null, null);
    }

    @Override
    protected void update(GridSizeMigrationTask.DbEntry dbEntry) {
        this.mUpdates.put(dbEntry.id, dbEntry.copy());
    }

    @Override
    protected ArrayList<GridSizeMigrationTask.DbEntry> loadWorkspaceEntries(long j) {
        ArrayList<GridSizeMigrationTask.DbEntry> arrayListLoadWorkspaceEntries = super.loadWorkspaceEntries(j);
        for (GridSizeMigrationTask.DbEntry dbEntry : arrayListLoadWorkspaceEntries) {
            this.mOriginalItems.put(dbEntry.id, dbEntry.copy());
            dbEntry.cellY++;
            this.mUpdates.put(dbEntry.id, dbEntry.copy());
        }
        return arrayListLoadWorkspaceEntries;
    }

    public void migrateScreen0() {
        migrateScreen(0L);
        ContentValues contentValues = new ContentValues();
        for (GridSizeMigrationTask.DbEntry dbEntry : this.mUpdates) {
            GridSizeMigrationTask.DbEntry dbEntry2 = this.mOriginalItems.get(dbEntry.id);
            if (dbEntry2.cellX != dbEntry.cellX || dbEntry2.cellY != dbEntry.cellY || dbEntry2.spanX != dbEntry.spanX || dbEntry2.spanY != dbEntry.spanY) {
                contentValues.clear();
                dbEntry.addToContentValues(contentValues);
                this.mDb.update(LauncherSettings.Favorites.TABLE_NAME, contentValues, "_id = ?", new String[]{Long.toString(dbEntry.id)});
            }
        }
        Iterator<GridSizeMigrationTask.DbEntry> it = this.mCarryOver.iterator();
        while (it.hasNext()) {
            this.mEntryToRemove.add(Long.valueOf(it.next().id));
        }
        if (!this.mEntryToRemove.isEmpty()) {
            this.mDb.delete(LauncherSettings.Favorites.TABLE_NAME, Utilities.createDbSelectionQuery("_id", this.mEntryToRemove), null);
        }
    }
}
