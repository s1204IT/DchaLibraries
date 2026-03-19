package com.android.server.soundtrigger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.soundtrigger.SoundTrigger;
import java.util.UUID;

public class SoundTriggerDbHelper extends SQLiteOpenHelper {
    private static final String CREATE_TABLE_ST_SOUND_MODEL = "CREATE TABLE st_sound_model(model_uuid TEXT PRIMARY KEY,vendor_uuid TEXT,data BLOB )";
    static final boolean DBG = false;
    private static final String NAME = "st_sound_model.db";
    static final String TAG = "SoundTriggerDbHelper";
    private static final int VERSION = 1;

    public interface GenericSoundModelContract {
        public static final String KEY_DATA = "data";
        public static final String KEY_MODEL_UUID = "model_uuid";
        public static final String KEY_VENDOR_UUID = "vendor_uuid";
        public static final String TABLE = "st_sound_model";
    }

    public SoundTriggerDbHelper(Context context) {
        super(context, NAME, (SQLiteDatabase.CursorFactory) null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(CREATE_TABLE_ST_SOUND_MODEL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS st_sound_model");
        onCreate(sQLiteDatabase);
    }

    public boolean updateGenericSoundModel(SoundTrigger.GenericSoundModel genericSoundModel) {
        boolean z;
        synchronized (this) {
            SQLiteDatabase writableDatabase = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("model_uuid", genericSoundModel.uuid.toString());
            contentValues.put("vendor_uuid", genericSoundModel.vendorUuid.toString());
            contentValues.put("data", genericSoundModel.data);
            try {
                z = writableDatabase.insertWithOnConflict(GenericSoundModelContract.TABLE, null, contentValues, 5) != -1;
            } finally {
                writableDatabase.close();
            }
        }
        return z;
    }

    public SoundTrigger.GenericSoundModel getGenericSoundModel(UUID uuid) {
        synchronized (this) {
            SQLiteDatabase readableDatabase = getReadableDatabase();
            Cursor cursorRawQuery = readableDatabase.rawQuery("SELECT  * FROM st_sound_model WHERE model_uuid= '" + uuid + "'", null);
            try {
                if (!cursorRawQuery.moveToFirst()) {
                    return null;
                }
                return new SoundTrigger.GenericSoundModel(uuid, UUID.fromString(cursorRawQuery.getString(cursorRawQuery.getColumnIndex("vendor_uuid"))), cursorRawQuery.getBlob(cursorRawQuery.getColumnIndex("data")));
            } finally {
                cursorRawQuery.close();
                readableDatabase.close();
            }
        }
    }

    public boolean deleteGenericSoundModel(UUID uuid) {
        synchronized (this) {
            SoundTrigger.GenericSoundModel genericSoundModel = getGenericSoundModel(uuid);
            if (genericSoundModel == null) {
                return false;
            }
            SQLiteDatabase writableDatabase = getWritableDatabase();
            StringBuilder sb = new StringBuilder();
            sb.append("model_uuid='");
            sb.append(genericSoundModel.uuid.toString());
            sb.append("'");
            try {
                return writableDatabase.delete(GenericSoundModelContract.TABLE, sb.toString(), null) != 0;
            } finally {
                writableDatabase.close();
            }
        }
    }
}
