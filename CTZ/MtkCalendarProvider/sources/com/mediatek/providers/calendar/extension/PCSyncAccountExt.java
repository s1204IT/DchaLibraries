package com.mediatek.providers.calendar.extension;

import android.database.sqlite.SQLiteDatabase;

public class PCSyncAccountExt implements ITableExt {
    private String mTableName;

    public PCSyncAccountExt(String str) {
        this.mTableName = str;
    }

    @Override
    public void tableExtension(SQLiteDatabase sQLiteDatabase) {
        createPCSyncAccount(sQLiteDatabase);
    }

    private void createPCSyncAccount(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("INSERT INTO " + this.mTableName + " (account_name, account_type, calendar_displayName, calendar_color, calendar_access_level, sync_events, ownerAccount) VALUES ('PC Sync','LOCAL','PC Sync','-9215145','700','1','PC Sync');");
    }
}
