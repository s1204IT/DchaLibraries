package com.android.providers.telephony;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class CarrierDatabaseHelper extends SQLiteOpenHelper {
    private static final List<String> CARRIERS_UNIQUE_FIELDS = new ArrayList();

    public CarrierDatabaseHelper(Context context) {
        super(context, "CarrierInformation.db", (SQLiteDatabase.CursorFactory) null, 2);
    }

    static {
        CARRIERS_UNIQUE_FIELDS.add("mcc");
        CARRIERS_UNIQUE_FIELDS.add("mnc");
        CARRIERS_UNIQUE_FIELDS.add("key_type");
        CARRIERS_UNIQUE_FIELDS.add("mvno_type");
        CARRIERS_UNIQUE_FIELDS.add("mvno_match_data");
    }

    public static String getStringForCarrierKeyTableCreation(String str) {
        return "CREATE TABLE " + str + "(_id INTEGER PRIMARY KEY,mcc TEXT DEFAULT '',mnc TEXT DEFAULT '',mvno_type TEXT DEFAULT '',mvno_match_data TEXT DEFAULT '',key_type TEXT DEFAULT '',key_identifier TEXT DEFAULT '',public_key BLOB DEFAULT '',expiration_time INTEGER DEFAULT 0,last_modified INTEGER DEFAULT 0,UNIQUE (" + TextUtils.join(", ", CARRIERS_UNIQUE_FIELDS) + "));";
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(getStringForCarrierKeyTableCreation("carrier_key"));
    }

    public void createCarrierTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(getStringForCarrierKeyTableCreation("carrier_key"));
    }

    public void dropCarrierTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS carrier_key;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        Log.d("CarrierDatabaseHelper", "dbh.onUpgrade:+ db=" + sQLiteDatabase + " oldV=" + i + " newV=" + i2);
        if (i < 2) {
            dropCarrierTable(sQLiteDatabase);
            createCarrierTable(sQLiteDatabase);
        }
    }
}
