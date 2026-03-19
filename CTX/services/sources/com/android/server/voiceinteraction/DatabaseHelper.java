package com.android.server.voiceinteraction;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.soundtrigger.SoundTrigger;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.backup.BackupManagerConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String CREATE_TABLE_SOUND_MODEL = "CREATE TABLE sound_model(model_uuid TEXT,vendor_uuid TEXT,keyphrase_id INTEGER,type INTEGER,data BLOB,recognition_modes INTEGER,locale TEXT,hint_text TEXT,users TEXT,PRIMARY KEY (keyphrase_id,locale,users))";
    static final boolean DBG = false;
    private static final String NAME = "sound_model.db";
    static final String TAG = "SoundModelDBHelper";
    private static final int VERSION = 6;

    public interface SoundModelContract {
        public static final String KEY_DATA = "data";
        public static final String KEY_HINT_TEXT = "hint_text";
        public static final String KEY_KEYPHRASE_ID = "keyphrase_id";
        public static final String KEY_LOCALE = "locale";
        public static final String KEY_MODEL_UUID = "model_uuid";
        public static final String KEY_RECOGNITION_MODES = "recognition_modes";
        public static final String KEY_TYPE = "type";
        public static final String KEY_USERS = "users";
        public static final String KEY_VENDOR_UUID = "vendor_uuid";
        public static final String TABLE = "sound_model";
    }

    public DatabaseHelper(Context context) {
        super(context, NAME, (SQLiteDatabase.CursorFactory) null, 6);
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(CREATE_TABLE_SOUND_MODEL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        if (i < 4) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS sound_model");
            onCreate(sQLiteDatabase);
        } else if (i == 4) {
            Slog.d(TAG, "Adding vendor UUID column");
            sQLiteDatabase.execSQL("ALTER TABLE sound_model ADD COLUMN vendor_uuid TEXT");
            i++;
        }
        if (i == 5) {
            Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT * FROM sound_model", null);
            ArrayList<SoundModelRecord> arrayList = new ArrayList();
            try {
                if (cursorRawQuery.moveToFirst()) {
                    do {
                        try {
                            arrayList.add(new SoundModelRecord(5, cursorRawQuery));
                        } catch (Exception e) {
                            Slog.e(TAG, "Failed to extract V5 record", e);
                        }
                    } while (cursorRawQuery.moveToNext());
                }
                cursorRawQuery.close();
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS sound_model");
                onCreate(sQLiteDatabase);
                for (SoundModelRecord soundModelRecord : arrayList) {
                    if (soundModelRecord.ifViolatesV6PrimaryKeyIsFirstOfAnyDuplicates(arrayList)) {
                        try {
                            long jWriteToDatabase = soundModelRecord.writeToDatabase(6, sQLiteDatabase);
                            if (jWriteToDatabase == -1) {
                                Slog.e(TAG, "Database write failed " + soundModelRecord.modelUuid + ": " + jWriteToDatabase);
                            }
                        } catch (Exception e2) {
                            Slog.e(TAG, "Failed to update V6 record " + soundModelRecord.modelUuid, e2);
                        }
                    }
                }
            } catch (Throwable th) {
                cursorRawQuery.close();
                throw th;
            }
        }
    }

    public boolean updateKeyphraseSoundModel(SoundTrigger.KeyphraseSoundModel keyphraseSoundModel) {
        synchronized (this) {
            SQLiteDatabase writableDatabase = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("model_uuid", keyphraseSoundModel.uuid.toString());
            if (keyphraseSoundModel.vendorUuid != null) {
                contentValues.put("vendor_uuid", keyphraseSoundModel.vendorUuid.toString());
            }
            contentValues.put(SoundModelContract.KEY_TYPE, (Integer) 0);
            contentValues.put("data", keyphraseSoundModel.data);
            if (keyphraseSoundModel.keyphrases == null || keyphraseSoundModel.keyphrases.length != 1) {
                return false;
            }
            contentValues.put(SoundModelContract.KEY_KEYPHRASE_ID, Integer.valueOf(keyphraseSoundModel.keyphrases[0].id));
            contentValues.put(SoundModelContract.KEY_RECOGNITION_MODES, Integer.valueOf(keyphraseSoundModel.keyphrases[0].recognitionModes));
            contentValues.put(SoundModelContract.KEY_USERS, getCommaSeparatedString(keyphraseSoundModel.keyphrases[0].users));
            contentValues.put(SoundModelContract.KEY_LOCALE, keyphraseSoundModel.keyphrases[0].locale);
            contentValues.put(SoundModelContract.KEY_HINT_TEXT, keyphraseSoundModel.keyphrases[0].text);
            try {
                return writableDatabase.insertWithOnConflict(SoundModelContract.TABLE, null, contentValues, 5) != -1;
            } finally {
                writableDatabase.close();
            }
        }
    }

    public boolean deleteKeyphraseSoundModel(int i, int i2, String str) {
        String languageTag = Locale.forLanguageTag(str).toLanguageTag();
        synchronized (this) {
            SoundTrigger.KeyphraseSoundModel keyphraseSoundModel = getKeyphraseSoundModel(i, i2, languageTag);
            if (keyphraseSoundModel == null) {
                return false;
            }
            SQLiteDatabase writableDatabase = getWritableDatabase();
            StringBuilder sb = new StringBuilder();
            sb.append("model_uuid='");
            sb.append(keyphraseSoundModel.uuid.toString());
            sb.append("'");
            try {
                return writableDatabase.delete(SoundModelContract.TABLE, sb.toString(), null) != 0;
            } finally {
                writableDatabase.close();
            }
        }
    }

    public SoundTrigger.KeyphraseSoundModel getKeyphraseSoundModel(int i, int i2, String str) {
        boolean z;
        UUID uuidFromString;
        String languageTag = Locale.forLanguageTag(str).toLanguageTag();
        synchronized (this) {
            String str2 = "SELECT  * FROM sound_model WHERE keyphrase_id= '" + i + "' AND " + SoundModelContract.KEY_LOCALE + "='" + languageTag + "'";
            SQLiteDatabase readableDatabase = getReadableDatabase();
            String str3 = null;
            Cursor cursorRawQuery = readableDatabase.rawQuery(str2, null);
            try {
                if (cursorRawQuery.moveToFirst()) {
                    while (true) {
                        if (cursorRawQuery.getInt(cursorRawQuery.getColumnIndex(SoundModelContract.KEY_TYPE)) == 0) {
                            String string = cursorRawQuery.getString(cursorRawQuery.getColumnIndex("model_uuid"));
                            if (string == null) {
                                Slog.w(TAG, "Ignoring SoundModel since it doesn't specify an ID");
                            } else {
                                int columnIndex = cursorRawQuery.getColumnIndex("vendor_uuid");
                                String string2 = columnIndex != -1 ? cursorRawQuery.getString(columnIndex) : str3;
                                byte[] blob = cursorRawQuery.getBlob(cursorRawQuery.getColumnIndex("data"));
                                int i3 = cursorRawQuery.getInt(cursorRawQuery.getColumnIndex(SoundModelContract.KEY_RECOGNITION_MODES));
                                int[] arrayForCommaSeparatedString = getArrayForCommaSeparatedString(cursorRawQuery.getString(cursorRawQuery.getColumnIndex(SoundModelContract.KEY_USERS)));
                                String string3 = cursorRawQuery.getString(cursorRawQuery.getColumnIndex(SoundModelContract.KEY_LOCALE));
                                String string4 = cursorRawQuery.getString(cursorRawQuery.getColumnIndex(SoundModelContract.KEY_HINT_TEXT));
                                if (arrayForCommaSeparatedString == null) {
                                    Slog.w(TAG, "Ignoring SoundModel since it doesn't specify users");
                                } else {
                                    int length = arrayForCommaSeparatedString.length;
                                    int i4 = 0;
                                    while (true) {
                                        if (i4 >= length) {
                                            z = false;
                                            break;
                                        }
                                        if (i2 != arrayForCommaSeparatedString[i4]) {
                                            i4++;
                                        } else {
                                            z = true;
                                            break;
                                        }
                                    }
                                    if (z) {
                                        SoundTrigger.Keyphrase[] keyphraseArr = {new SoundTrigger.Keyphrase(i, i3, string3, string4, arrayForCommaSeparatedString)};
                                        if (string2 != null) {
                                            uuidFromString = UUID.fromString(string2);
                                        } else {
                                            uuidFromString = null;
                                        }
                                        return new SoundTrigger.KeyphraseSoundModel(UUID.fromString(string), uuidFromString, blob, keyphraseArr);
                                    }
                                    if (!cursorRawQuery.moveToNext()) {
                                        break;
                                    }
                                    str3 = null;
                                }
                            }
                            if (!cursorRawQuery.moveToNext()) {
                            }
                        } else if (!cursorRawQuery.moveToNext()) {
                        }
                    }
                }
                Slog.w(TAG, "No SoundModel available for the given keyphrase");
                return null;
            } finally {
                cursorRawQuery.close();
                readableDatabase.close();
            }
        }
    }

    private static String getCommaSeparatedString(int[] iArr) {
        if (iArr == null) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iArr.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(iArr[i]);
        }
        return sb.toString();
    }

    private static int[] getArrayForCommaSeparatedString(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String[] strArrSplit = str.split(",");
        int[] iArr = new int[strArrSplit.length];
        for (int i = 0; i < strArrSplit.length; i++) {
            iArr[i] = Integer.parseInt(strArrSplit[i]);
        }
        return iArr;
    }

    private static class SoundModelRecord {
        public final byte[] data;
        public final String hintText;
        public final int keyphraseId;
        public final String locale;
        public final String modelUuid;
        public final int recognitionModes;
        public final int type;
        public final String users;
        public final String vendorUuid;

        public SoundModelRecord(int i, Cursor cursor) {
            this.modelUuid = cursor.getString(cursor.getColumnIndex("model_uuid"));
            if (i >= 5) {
                this.vendorUuid = cursor.getString(cursor.getColumnIndex("vendor_uuid"));
            } else {
                this.vendorUuid = null;
            }
            this.keyphraseId = cursor.getInt(cursor.getColumnIndex(SoundModelContract.KEY_KEYPHRASE_ID));
            this.type = cursor.getInt(cursor.getColumnIndex(SoundModelContract.KEY_TYPE));
            this.data = cursor.getBlob(cursor.getColumnIndex("data"));
            this.recognitionModes = cursor.getInt(cursor.getColumnIndex(SoundModelContract.KEY_RECOGNITION_MODES));
            this.locale = cursor.getString(cursor.getColumnIndex(SoundModelContract.KEY_LOCALE));
            this.hintText = cursor.getString(cursor.getColumnIndex(SoundModelContract.KEY_HINT_TEXT));
            this.users = cursor.getString(cursor.getColumnIndex(SoundModelContract.KEY_USERS));
        }

        private boolean V6PrimaryKeyMatches(SoundModelRecord soundModelRecord) {
            return this.keyphraseId == soundModelRecord.keyphraseId && stringComparisonHelper(this.locale, soundModelRecord.locale) && stringComparisonHelper(this.users, soundModelRecord.users);
        }

        public boolean ifViolatesV6PrimaryKeyIsFirstOfAnyDuplicates(List<SoundModelRecord> list) {
            for (SoundModelRecord soundModelRecord : list) {
                if (this != soundModelRecord && V6PrimaryKeyMatches(soundModelRecord) && !Arrays.equals(this.data, soundModelRecord.data)) {
                    return false;
                }
            }
            Iterator<SoundModelRecord> it = list.iterator();
            while (it.hasNext()) {
                SoundModelRecord next = it.next();
                if (V6PrimaryKeyMatches(next)) {
                    return this == next;
                }
            }
            return true;
        }

        public long writeToDatabase(int i, SQLiteDatabase sQLiteDatabase) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("model_uuid", this.modelUuid);
            if (i >= 5) {
                contentValues.put("vendor_uuid", this.vendorUuid);
            }
            contentValues.put(SoundModelContract.KEY_KEYPHRASE_ID, Integer.valueOf(this.keyphraseId));
            contentValues.put(SoundModelContract.KEY_TYPE, Integer.valueOf(this.type));
            contentValues.put("data", this.data);
            contentValues.put(SoundModelContract.KEY_RECOGNITION_MODES, Integer.valueOf(this.recognitionModes));
            contentValues.put(SoundModelContract.KEY_LOCALE, this.locale);
            contentValues.put(SoundModelContract.KEY_HINT_TEXT, this.hintText);
            contentValues.put(SoundModelContract.KEY_USERS, this.users);
            return sQLiteDatabase.insertWithOnConflict(SoundModelContract.TABLE, null, contentValues, 5);
        }

        private static boolean stringComparisonHelper(String str, String str2) {
            if (str != null) {
                return str.equals(str2);
            }
            return str == str2;
        }
    }
}
