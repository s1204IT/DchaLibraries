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

/* loaded from: classes.dex */
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

    @Override // com.android.launcher3.model.GridSizeMigrationTask
    protected Cursor queryWorkspace(String[] strArr, String str) {
        return this.mDb.query(LauncherSettings.Favorites.TABLE_NAME, strArr, str, null, null, null, null);
    }

    @Override // com.android.launcher3.model.GridSizeMigrationTask
    protected void update(GridSizeMigrationTask.DbEntry dbEntry) {
        this.mUpdates.put(dbEntry.id, dbEntry.copy());
    }

    @Override // com.android.launcher3.model.GridSizeMigrationTask
    protected ArrayList<GridSizeMigrationTask.DbEntry> loadWorkspaceEntries(long j) {
        ArrayList<GridSizeMigrationTask.DbEntry> arrayListLoadWorkspaceEntries = super.loadWorkspaceEntries(j);
        Iterator<GridSizeMigrationTask.DbEntry> it = arrayListLoadWorkspaceEntries.iterator();
        while (it.hasNext()) {
            GridSizeMigrationTask.DbEntry next = it.next();
            this.mOriginalItems.put(next.id, next.copy());
            next.cellY++;
            this.mUpdates.put(next.id, next.copy());
        }
        return arrayListLoadWorkspaceEntries;
    }

    public void migrateScreen0() {
        migrateScreen(0L);
        ContentValues contentValues = new ContentValues();
        Iterator<GridSizeMigrationTask.DbEntry> it = this.mUpdates.iterator();
        while (it.hasNext()) {
            GridSizeMigrationTask.DbEntry next = it.next();
            GridSizeMigrationTask.DbEntry dbEntry = this.mOriginalItems.get(next.id);
            if (dbEntry.cellX != next.cellX || dbEntry.cellY != next.cellY || dbEntry.spanX != next.spanX || dbEntry.spanY != next.spanY) {
                contentValues.clear();
                next.addToContentValues(contentValues);
                this.mDb.update(LauncherSettings.Favorites.TABLE_NAME, contentValues, "_id = ?", new String[]{Long.toString(next.id)});
            }
        }
        Iterator<GridSizeMigrationTask.DbEntry> it2 = this.mCarryOver.iterator();
        while (it2.hasNext()) {
            this.mEntryToRemove.add(Long.valueOf(it2.next().id));
        }
        if (!this.mEntryToRemove.isEmpty()) {
            this.mDb.delete(LauncherSettings.Favorites.TABLE_NAME, Utilities.createDbSelectionQuery("_id", this.mEntryToRemove), null);
        }
    }
}
