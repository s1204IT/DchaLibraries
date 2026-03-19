package com.mediatek.providers.calendar.extension;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.mediatek.providers.calendar.LogUtil;

public class MTKDatabaseUpgradeExt implements IDatabaseUpgradeExt {
    private boolean mIsUpgradeFromMTKVersion = false;

    @Override
    public int downgradeMTKVersionsIfNeeded(int i, SQLiteDatabase sQLiteDatabase) {
        if (i == 103) {
            this.mIsUpgradeFromMTKVersion = true;
            return downgradeFromMTKGBVersion(sQLiteDatabase);
        }
        if (i == 309) {
            this.mIsUpgradeFromMTKVersion = true;
            return downgradeFromMTKICSVersion(sQLiteDatabase);
        }
        return i;
    }

    @Override
    public int upgradeToMTKJBVersion(SQLiteDatabase sQLiteDatabase) {
        ensureMTKColumns(sQLiteDatabase);
        restoreMTKColumnsIfNeeded(sQLiteDatabase);
        return 404;
    }

    private int downgradeFromMTKGBVersion(SQLiteDatabase sQLiteDatabase) {
        LogUtil.v("MTKDatabaseUpgradeExt", "downgradeFromMTKGBVersion");
        backupMTKColumns(sQLiteDatabase);
        return 102;
    }

    private int downgradeFromMTKICSVersion(SQLiteDatabase sQLiteDatabase) {
        LogUtil.v("MTKDatabaseUpgradeExt", "downgradeFromMTKICSVersion");
        backupMTKColumns(sQLiteDatabase);
        return 308;
    }

    private void backupMTKColumns(SQLiteDatabase sQLiteDatabase) {
        LogUtil.v("MTKDatabaseUpgradeExt", "backupMTKColumns, sql = CREATE TABLE mtk_backup_table AS SELECT _id,createTime,modifyTime,isLunar,lunarRrule FROM Events;");
        sQLiteDatabase.execSQL("CREATE TABLE mtk_backup_table AS SELECT _id,createTime,modifyTime,isLunar,lunarRrule FROM Events;");
    }

    private void restoreMTKColumnsIfNeeded(SQLiteDatabase sQLiteDatabase) {
        if (this.mIsUpgradeFromMTKVersion) {
            LogUtil.v("MTKDatabaseUpgradeExt", "restoreMTKColumns, sql = UPDATE Events SET createTime=(SELECT createTime FROM mtk_backup_table WHERE createTime=Events.createTime),modifyTime=(SELECT modifyTime FROM mtk_backup_table WHERE modifyTime=Events.modifyTime),isLunar=(SELECT isLunar FROM mtk_backup_table WHERE isLunar=Events.isLunar),lunarRrule=(SELECT lunarRrule FROM mtk_backup_table WHERE lunarRrule=Events.lunarRrule);");
            sQLiteDatabase.execSQL("UPDATE Events SET createTime=(SELECT createTime FROM mtk_backup_table WHERE createTime=Events.createTime),modifyTime=(SELECT modifyTime FROM mtk_backup_table WHERE modifyTime=Events.modifyTime),isLunar=(SELECT isLunar FROM mtk_backup_table WHERE isLunar=Events.isLunar),lunarRrule=(SELECT lunarRrule FROM mtk_backup_table WHERE lunarRrule=Events.lunarRrule);");
            LogUtil.v("MTKDatabaseUpgradeExt", "drop backup table, sql = DROP TABLE mtk_backup_table;");
            sQLiteDatabase.execSQL("DROP TABLE mtk_backup_table;");
            return;
        }
        LogUtil.d("MTKDatabaseUpgradeExt", "not upgrade from MTK versions, no need to restore");
    }

    private void ensureMTKColumns(SQLiteDatabase sQLiteDatabase) {
        LogUtil.v("MTKDatabaseUpgradeExt", "ensure MTK Columns exists");
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("select * from Events where _id=0", null);
        if (cursorRawQuery == null) {
            LogUtil.e("MTKDatabaseUpgradeExt", "the cursor shouldn't be null");
            return;
        }
        if (cursorRawQuery.getColumnIndex("createTime") < 0) {
            LogUtil.v("MTKDatabaseUpgradeExt", "add column: createTime");
            sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN createTime INTEGER;");
        }
        if (cursorRawQuery.getColumnIndex("modifyTime") < 0) {
            LogUtil.v("MTKDatabaseUpgradeExt", "add column: modifyTime");
            sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN modifyTime INTEGER;");
        }
        if (cursorRawQuery.getColumnIndex("isLunar") < 0) {
            LogUtil.v("MTKDatabaseUpgradeExt", "add column: isLunar");
            sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN isLunar INTEGER NOT NULL DEFAULT 0;");
        }
        if (cursorRawQuery.getColumnIndex("lunarRrule") < 0) {
            LogUtil.v("MTKDatabaseUpgradeExt", "add column: lunarRrule");
            sQLiteDatabase.execSQL("ALTER TABLE Events ADD COLUMN lunarRrule TEXT;");
        }
        cursorRawQuery.close();
    }
}
