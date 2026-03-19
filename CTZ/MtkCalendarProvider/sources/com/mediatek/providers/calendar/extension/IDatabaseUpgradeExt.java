package com.mediatek.providers.calendar.extension;

import android.database.sqlite.SQLiteDatabase;

public interface IDatabaseUpgradeExt {
    int downgradeMTKVersionsIfNeeded(int i, SQLiteDatabase sQLiteDatabase);

    int upgradeToMTKJBVersion(SQLiteDatabase sQLiteDatabase);
}
