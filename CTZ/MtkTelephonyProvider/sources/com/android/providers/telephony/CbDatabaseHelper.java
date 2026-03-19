package com.android.providers.telephony;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class CbDatabaseHelper extends SQLiteOpenHelper {
    private Context mContext;

    public CbDatabaseHelper(Context context) {
        super(context, "cb.db", (SQLiteDatabase.CursorFactory) null, 1);
        this.mContext = context;
    }

    private void createTables(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE channel(_id INTEGER PRIMARY KEY,name TEXT,number TEXT,enable BOOLEAN,sub_id LONG DEFAULT -1);");
        sQLiteDatabase.execSQL("CREATE TABLE messages(_id INTEGER PRIMARY KEY,sub_id LONG DEFAULT -1,body TEXT,channel_id INTEGER,thread_id INTEGER,read INTEGER DEFAULT 0,date_sent INTEGER DEFAULT 0,date INTEGER);");
        sQLiteDatabase.execSQL("CREATE TABLE threads(_id INTEGER PRIMARY KEY,date INTEGER,msg_count INTEGER,address_id INTEGER,read INTEGER DEFAULT 0,snippet TEXT);");
        sQLiteDatabase.execSQL("CREATE TABLE address(_id INTEGER PRIMARY KEY,address TEXT);");
        sQLiteDatabase.execSQL("CREATE TABLE cbraw (_id INTEGER PRIMARY KEY,msgID INTEGER,serialNum INTEGER,sequence INTEGER,count INTEGER,pdu TEXT,sub_id LONG DEFAULT -1);");
    }

    private void createCommonTriggers(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TRIGGER cb_update_thread_on_insert AFTER INSERT ON messages BEGIN  UPDATE threads SET    date = (strftime('%s','now') * 1000),     snippet = new.body   WHERE threads._id = new.thread_id;   UPDATE threads SET msg_count =      (SELECT COUNT(messages._id) FROM messages LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id )  WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM messages          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER cb_update_thread_read_on_update AFTER  UPDATE OF read  ON messages BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM messages          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER cb_update_thread_on_delete AFTER DELETE ON messages BEGIN   UPDATE threads SET      date = (strftime('%s','now') * 1000)  WHERE threads._id = old.thread_id;   UPDATE threads SET msg_count =      (SELECT COUNT(messages._id) FROM messages LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = old.thread_id)  WHERE threads._id = old.thread_id;   UPDATE threads SET snippet =    (SELECT body FROM     (SELECT date, body, thread_id FROM messages)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id;   UPDATE threads SET date =    (SELECT date FROM     (SELECT date, body, thread_id FROM messages)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id; END;");
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        createTables(sQLiteDatabase);
        createCommonTriggers(sQLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
    }
}
