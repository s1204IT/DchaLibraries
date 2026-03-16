package com.android.providers.telephony;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.google.android.mms.pdu.EncodedStringValue;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class MmsSmsDatabaseHelper extends SQLiteOpenHelper {
    private final Context mContext;
    private LowStorageMonitor mLowStorageMonitor;
    private static MmsSmsDatabaseHelper sInstance = null;
    private static boolean sTriedAutoIncrement = false;
    private static boolean sFakeLowStorageTest = false;

    private MmsSmsDatabaseHelper(Context context) {
        super(context, "mmssms.db", (SQLiteDatabase.CursorFactory) null, 61);
        this.mContext = context;
    }

    static synchronized MmsSmsDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MmsSmsDatabaseHelper(context);
        }
        return sInstance;
    }

    private static void removeUnferencedCanonicalAddresses(SQLiteDatabase db) {
        Cursor c = db.query("threads", new String[]{"recipient_ids"}, null, null, null, null, null);
        if (c != null) {
            try {
                if (c.getCount() == 0) {
                    db.delete("canonical_addresses", null, null);
                } else {
                    HashSet<Integer> recipientIds = new HashSet<>();
                    while (c.moveToNext()) {
                        String[] recips = c.getString(0).split(" ");
                        for (String recip : recips) {
                            try {
                                int recipientId = Integer.parseInt(recip);
                                recipientIds.add(Integer.valueOf(recipientId));
                            } catch (Exception e) {
                            }
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    Iterator<Integer> iter = recipientIds.iterator();
                    while (iter.hasNext()) {
                        sb.append("_id != " + iter.next());
                        if (iter.hasNext()) {
                            sb.append(" AND ");
                        }
                    }
                    if (sb.length() > 0) {
                        db.delete("canonical_addresses", sb.toString(), null);
                    }
                }
            } finally {
                c.close();
            }
        }
    }

    public static void updateThread(SQLiteDatabase db, long thread_id) {
        if (thread_id < 0) {
            updateAllThreads(db, null, null);
            return;
        }
        db.beginTransaction();
        try {
            int rows = db.delete("threads", "_id = ? AND _id NOT IN          (SELECT thread_id FROM sms            UNION SELECT thread_id FROM pdu)", new String[]{String.valueOf(thread_id)});
            if (rows > 0) {
                removeUnferencedCanonicalAddresses(db);
            } else {
                db.execSQL("  UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = " + thread_id + "        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = " + thread_id + "        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = " + thread_id + ";");
                db.execSQL("  UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu         UNION SELECT date, thread_id FROM sms)     WHERE thread_id = " + thread_id + " ORDER BY date DESC LIMIT 1),  snippet =    (SELECT snippet FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id FROM pdu         UNION SELECT date, body AS snippet, thread_id FROM sms)     WHERE thread_id = " + thread_id + " ORDER BY date DESC LIMIT 1),  snippet_cs =    (SELECT snippet_cs FROM        (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id FROM pdu         UNION SELECT date, 0 AS snippet_cs, thread_id FROM sms)     WHERE thread_id = " + thread_id + " ORDER BY date DESC LIMIT 1)  WHERE threads._id = " + thread_id + ";");
                String query = "SELECT thread_id FROM sms WHERE type=5 AND thread_id = " + thread_id + " LIMIT 1";
                int setError = 0;
                Cursor c = db.rawQuery(query, null);
                if (c != null) {
                    try {
                        setError = c.getCount();
                        c.close();
                    } finally {
                    }
                }
                String errorQuery = "SELECT error FROM threads WHERE _id = " + thread_id;
                c = db.rawQuery(errorQuery, null);
                if (c != null) {
                    try {
                        if (c.moveToNext()) {
                            int curError = c.getInt(0);
                            if (curError != setError) {
                                db.execSQL("UPDATE threads SET error=" + setError + " WHERE _id = " + thread_id);
                            }
                        }
                    } finally {
                    }
                }
            }
            db.setTransactionSuccessful();
        } catch (Throwable ex) {
            Log.e("MmsSmsDatabaseHelper", ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }
    }

    public static void updateAllThreads(SQLiteDatabase db, String where, String[] whereArgs) {
        String where2;
        db.beginTransaction();
        try {
            if (where == null) {
                where2 = "";
            } else {
                where2 = "WHERE (" + where + ")";
            }
            String query = "SELECT _id FROM threads WHERE _id IN (SELECT DISTINCT thread_id FROM sms " + where2 + ")";
            Cursor c = db.rawQuery(query, whereArgs);
            if (c != null) {
                while (c.moveToNext()) {
                    try {
                        updateThread(db, c.getInt(0));
                    } finally {
                        c.close();
                    }
                }
            }
            db.delete("threads", "_id NOT IN (SELECT DISTINCT thread_id FROM sms where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM pdu where thread_id NOT NULL)", null);
            removeUnferencedCanonicalAddresses(db);
            db.setTransactionSuccessful();
        } catch (Throwable ex) {
            Log.e("MmsSmsDatabaseHelper", ex.getMessage(), ex);
        } finally {
            db.endTransaction();
        }
    }

    public static int deleteOneSms(SQLiteDatabase db, int message_id) {
        int thread_id = -1;
        Cursor c = db.query("sms", new String[]{"thread_id"}, "_id=" + message_id, null, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                thread_id = c.getInt(0);
            }
            c.close();
        }
        int rows = db.delete("sms", "_id=" + message_id, null);
        if (thread_id > 0) {
            updateThread(db, thread_id);
        }
        return rows;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createMmsTables(db);
        createSmsTables(db);
        createCommonTables(db);
        createCommonTriggers(db);
        createMmsTriggers(db);
        createWordsTables(db);
        createIndices(db);
    }

    private void populateWordsTable(SQLiteDatabase db) {
        Cursor mmsRows = db.query("sms", new String[]{"_id", "body"}, null, null, null, null, null);
        if (mmsRows != null) {
            try {
                mmsRows.moveToPosition(-1);
                ContentValues cv = new ContentValues();
                while (mmsRows.moveToNext()) {
                    cv.clear();
                    long id = mmsRows.getLong(0);
                    String body = mmsRows.getString(1);
                    cv.put("_id", Long.valueOf(id));
                    cv.put("index_text", body);
                    cv.put("source_id", Long.valueOf(id));
                    cv.put("table_to_use", (Integer) 1);
                    db.insert("words", "index_text", cv);
                }
            } finally {
            }
        }
        if (mmsRows != null) {
            mmsRows.close();
        }
        mmsRows = db.query("part", new String[]{"_id", "text"}, "ct = 'text/plain'", null, null, null, null);
        if (mmsRows != null) {
            try {
                mmsRows.moveToPosition(-1);
                ContentValues cv2 = new ContentValues();
                while (mmsRows.moveToNext()) {
                    cv2.clear();
                    long id2 = mmsRows.getLong(0);
                    String body2 = mmsRows.getString(1);
                    cv2.put("_id", Long.valueOf(id2));
                    cv2.put("index_text", body2);
                    cv2.put("source_id", Long.valueOf(id2));
                    cv2.put("table_to_use", (Integer) 1);
                    db.insert("words", "index_text", cv2);
                }
            } finally {
            }
        }
        if (mmsRows != null) {
            mmsRows.close();
        }
    }

    private void createWordsTables(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE VIRTUAL TABLE words USING FTS3 (_id INTEGER PRIMARY KEY, index_text TEXT, source_id INTEGER, table_to_use INTEGER);");
            db.execSQL("CREATE TRIGGER sms_words_update AFTER UPDATE ON sms BEGIN UPDATE words  SET index_text = NEW.body WHERE (source_id=NEW._id AND table_to_use=1);  END;");
            db.execSQL("CREATE TRIGGER sms_words_delete AFTER DELETE ON sms BEGIN DELETE FROM   words WHERE source_id = OLD._id AND table_to_use = 1; END;");
            populateWordsTable(db);
        } catch (Exception ex) {
            Log.e("MmsSmsDatabaseHelper", "got exception creating words table: " + ex.toString());
        }
    }

    private void createIndices(SQLiteDatabase db) {
        createThreadIdIndex(db);
    }

    private void createThreadIdIndex(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE INDEX IF NOT EXISTS typeThreadIdIndex ON sms (type, thread_id);");
        } catch (Exception ex) {
            Log.e("MmsSmsDatabaseHelper", "got exception creating indices: " + ex.toString());
        }
    }

    private void createMmsTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pdu (_id INTEGER PRIMARY KEY AUTOINCREMENT,thread_id INTEGER,date INTEGER,date_sent INTEGER DEFAULT 0,msg_box INTEGER,read INTEGER DEFAULT 0,m_id TEXT,sub TEXT,sub_cs INTEGER,ct_t TEXT,ct_l TEXT,exp INTEGER,m_cls TEXT,m_type INTEGER,v INTEGER,m_size INTEGER,pri INTEGER,rr INTEGER,rpt_a INTEGER,resp_st INTEGER,st INTEGER,tr_id TEXT,retr_st INTEGER,retr_txt TEXT,retr_txt_cs INTEGER,read_status INTEGER,ct_cls INTEGER,resp_txt TEXT,d_tm INTEGER,d_rpt INTEGER,locked INTEGER DEFAULT 0,sub_id INTEGER DEFAULT -1, seen INTEGER DEFAULT 0,creator TEXT,text_only INTEGER DEFAULT 0);");
        db.execSQL("CREATE TABLE addr (_id INTEGER PRIMARY KEY,msg_id INTEGER,contact_id INTEGER,address TEXT,type INTEGER,charset INTEGER);");
        db.execSQL("CREATE TABLE part (_id INTEGER PRIMARY KEY AUTOINCREMENT,mid INTEGER,seq INTEGER DEFAULT 0,ct TEXT,name TEXT,chset INTEGER,cd TEXT,fn TEXT,cid TEXT,cl TEXT,ctt_s INTEGER,ctt_t TEXT,_data TEXT,text TEXT);");
        db.execSQL("CREATE TABLE rate (sent_time INTEGER);");
        db.execSQL("CREATE TABLE drm (_id INTEGER PRIMARY KEY,_data TEXT);");
    }

    private void createMmsTriggers(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS part_cleanup");
        db.execSQL("CREATE TRIGGER part_cleanup DELETE ON pdu BEGIN   DELETE FROM part  WHERE mid=old._id;END;");
        db.execSQL("DROP TRIGGER IF EXISTS addr_cleanup");
        db.execSQL("CREATE TRIGGER addr_cleanup DELETE ON pdu BEGIN   DELETE FROM addr  WHERE msg_id=old._id;END;");
        db.execSQL("DROP TRIGGER IF EXISTS cleanup_delivery_and_read_report");
        db.execSQL("CREATE TRIGGER cleanup_delivery_and_read_report AFTER DELETE ON pdu WHEN old.m_type=128 BEGIN   DELETE FROM pdu  WHERE (m_type=134    OR m_type=136)    AND m_id=old.m_id; END;");
        db.execSQL("DROP TRIGGER IF EXISTS update_threads_on_insert_part");
        db.execSQL("CREATE TRIGGER update_threads_on_insert_part  AFTER INSERT ON part  WHEN new.ct != 'text/plain' AND new.ct != 'application/smil'  BEGIN   UPDATE threads SET has_attachment=1 WHERE _id IN    (SELECT pdu.thread_id FROM part JOIN pdu ON pdu._id=part.mid      WHERE part._id=new._id LIMIT 1);  END");
        db.execSQL("DROP TRIGGER IF EXISTS update_threads_on_update_part");
        db.execSQL("CREATE TRIGGER update_threads_on_update_part  AFTER UPDATE of mid ON part  WHEN new.ct != 'text/plain' AND new.ct != 'application/smil'  BEGIN   UPDATE threads SET has_attachment=1 WHERE _id IN    (SELECT pdu.thread_id FROM part JOIN pdu ON pdu._id=part.mid      WHERE part._id=new._id LIMIT 1);  END");
        db.execSQL("DROP TRIGGER IF EXISTS update_threads_on_delete_part");
        db.execSQL("CREATE TRIGGER update_threads_on_delete_part  AFTER DELETE ON part  WHEN old.ct != 'text/plain' AND old.ct != 'application/smil'  BEGIN   UPDATE threads SET has_attachment =    CASE     (SELECT COUNT(*) FROM part JOIN pdu      WHERE pdu.thread_id = threads._id      AND part.ct != 'text/plain' AND part.ct != 'application/smil'      AND part.mid = pdu._id)   WHEN 0 THEN 0    ELSE 1    END;  END");
        db.execSQL("DROP TRIGGER IF EXISTS update_threads_on_update_pdu");
        db.execSQL("CREATE TRIGGER update_threads_on_update_pdu  AFTER UPDATE of thread_id ON pdu  BEGIN   UPDATE threads SET has_attachment=1 WHERE _id IN    (SELECT pdu.thread_id FROM part JOIN pdu      WHERE part.ct != 'text/plain' AND part.ct != 'application/smil'      AND part.mid = pdu._id); END");
        db.execSQL("DROP TRIGGER IF EXISTS delete_mms_pending_on_delete");
        db.execSQL("CREATE TRIGGER delete_mms_pending_on_delete AFTER DELETE ON pdu BEGIN   DELETE FROM pending_msgs  WHERE msg_id=old._id; END;");
        db.execSQL("DROP TRIGGER IF EXISTS delete_mms_pending_on_update");
        db.execSQL("CREATE TRIGGER delete_mms_pending_on_update AFTER UPDATE ON pdu WHEN old.msg_box=4  AND new.msg_box!=4 BEGIN   DELETE FROM pending_msgs  WHERE msg_id=new._id; END;");
        db.execSQL("DROP TRIGGER IF EXISTS insert_mms_pending_on_insert");
        db.execSQL("CREATE TRIGGER insert_mms_pending_on_insert AFTER INSERT ON pdu WHEN new.m_type=130  OR new.m_type=135 BEGIN   INSERT INTO pending_msgs    (proto_type,     msg_id,     msg_type,     pending_sub_id,     err_type,     err_code,     retry_index,     due_time)   VALUES     (1,      new._id,      new.m_type,      new.sub_id,0,0,0,0);END;");
        db.execSQL("DROP TRIGGER IF EXISTS insert_mms_pending_on_update");
        db.execSQL("CREATE TRIGGER insert_mms_pending_on_update AFTER UPDATE ON pdu WHEN new.m_type=128  AND new.msg_box=4  AND old.msg_box!=4 BEGIN   INSERT INTO pending_msgs    (proto_type,     msg_id,     msg_type,     pending_sub_id,     err_type,     err_code,     retry_index,     due_time)   VALUES     (1,      new._id,      new.m_type,      new.sub_id,0,0,0,0);END;");
        db.execSQL("DROP TRIGGER IF EXISTS mms_words_update");
        db.execSQL("CREATE TRIGGER mms_words_update AFTER UPDATE ON part BEGIN UPDATE words  SET index_text = NEW.text WHERE (source_id=NEW._id AND table_to_use=2);  END;");
        db.execSQL("DROP TRIGGER IF EXISTS mms_words_delete");
        db.execSQL("CREATE TRIGGER mms_words_delete AFTER DELETE ON part BEGIN DELETE FROM  words WHERE source_id = OLD._id AND table_to_use = 2; END;");
        db.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_date_subject_on_update");
        db.execSQL("CREATE TRIGGER pdu_update_thread_date_subject_on_update AFTER  UPDATE OF date, sub, msg_box  ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128 BEGIN  UPDATE threads SET    date = (strftime('%s','now') * 1000),     snippet = new.sub,     snippet_cs = new.sub_cs  WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
        db.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_on_delete");
        db.execSQL("CREATE TRIGGER pdu_update_thread_on_delete AFTER DELETE ON pdu BEGIN   UPDATE threads SET      date = (strftime('%s','now') * 1000)  WHERE threads._id = old.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = old.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = old.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = old.thread_id;   UPDATE threads SET snippet =    (SELECT snippet FROM     (SELECT date * 1000 AS date, sub AS snippet, thread_id FROM pdu      UNION SELECT date, body AS snippet, thread_id FROM sms)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id;   UPDATE threads SET snippet_cs =    (SELECT snippet_cs FROM     (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id FROM pdu      UNION SELECT date, 0 AS snippet_cs, thread_id FROM sms)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id; END;");
        db.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_on_insert");
        db.execSQL("CREATE TRIGGER pdu_update_thread_on_insert AFTER INSERT ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128 BEGIN  UPDATE threads SET    date = (strftime('%s','now') * 1000),     snippet = new.sub,     snippet_cs = new.sub_cs  WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
        db.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_read_on_update");
        db.execSQL("CREATE TRIGGER pdu_update_thread_read_on_update AFTER  UPDATE OF read  ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128 BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
        db.execSQL("DROP TRIGGER IF EXISTS update_threads_error_on_delete_mms");
        db.execSQL("CREATE TRIGGER update_threads_error_on_delete_mms   BEFORE DELETE ON pdu  WHEN OLD._id IN (SELECT DISTINCT msg_id                   FROM pending_msgs                   WHERE err_type >= 10) BEGIN   UPDATE threads SET error = error - 1  WHERE _id = OLD.thread_id; END;");
        db.execSQL("DROP TRIGGER IF EXISTS update_threads_error_on_move_mms");
        db.execSQL("CREATE TRIGGER update_threads_error_on_move_mms   BEFORE UPDATE OF msg_box ON pdu   WHEN (OLD.msg_box = 4 AND NEW.msg_box != 4)   AND (OLD._id IN (SELECT DISTINCT msg_id                   FROM pending_msgs                   WHERE err_type >= 10)) BEGIN   UPDATE threads SET error = error - 1  WHERE _id = OLD.thread_id; END;");
    }

    private void createSmsTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sms (_id INTEGER PRIMARY KEY,thread_id INTEGER,address TEXT,person INTEGER,date INTEGER,date_sent INTEGER DEFAULT 0,protocol INTEGER,read INTEGER DEFAULT 0,status INTEGER DEFAULT -1,type INTEGER,reply_path_present INTEGER,subject TEXT,body TEXT,service_center TEXT,locked INTEGER DEFAULT 0,sub_id INTEGER DEFAULT -1, error_code INTEGER DEFAULT 0,creator TEXT,seen INTEGER DEFAULT 0);");
        db.execSQL("CREATE TABLE raw (_id INTEGER PRIMARY KEY,date INTEGER,reference_number INTEGER,count INTEGER,sequence INTEGER,destination_port INTEGER,address TEXT,sub_id INTEGER DEFAULT -1, pdu TEXT);");
        db.execSQL("CREATE TABLE attachments (sms_id INTEGER,content_url TEXT,offset INTEGER);");
        db.execSQL("CREATE TABLE sr_pending (reference_number INTEGER,action TEXT,data TEXT);");
    }

    private void createCommonTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE canonical_addresses (_id INTEGER PRIMARY KEY AUTOINCREMENT,address TEXT);");
        db.execSQL("CREATE TABLE threads (_id INTEGER PRIMARY KEY AUTOINCREMENT,date INTEGER DEFAULT 0,message_count INTEGER DEFAULT 0,recipient_ids TEXT,snippet TEXT,snippet_cs INTEGER DEFAULT 0,read INTEGER DEFAULT 1,archived INTEGER DEFAULT 0,type INTEGER DEFAULT 0,error INTEGER DEFAULT 0,has_attachment INTEGER DEFAULT 0);");
        db.execSQL("CREATE TABLE pending_msgs (_id INTEGER PRIMARY KEY,proto_type INTEGER,msg_id INTEGER,msg_type INTEGER,err_type INTEGER,err_code INTEGER,retry_index INTEGER NOT NULL DEFAULT 0,due_time INTEGER,pending_sub_id INTEGER DEFAULT -1, last_try INTEGER);");
    }

    private void createCommonTriggers(SQLiteDatabase db) {
        db.execSQL("CREATE TRIGGER sms_update_thread_on_insert AFTER INSERT ON sms BEGIN  UPDATE threads SET    date = (strftime('%s','now') * 1000),     snippet = new.body,     snippet_cs = 0  WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
        db.execSQL("CREATE TRIGGER sms_update_thread_date_subject_on_update AFTER  UPDATE OF date, body, type  ON sms BEGIN  UPDATE threads SET    date = (strftime('%s','now') * 1000),     snippet = new.body,     snippet_cs = 0  WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
        db.execSQL("CREATE TRIGGER sms_update_thread_read_on_update AFTER  UPDATE OF read  ON sms BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
        db.execSQL("CREATE TRIGGER update_threads_error_on_update_mms   AFTER UPDATE OF err_type ON pending_msgs   WHEN (OLD.err_type < 10 AND NEW.err_type >= 10)    OR (OLD.err_type >= 10 AND NEW.err_type < 10) BEGIN  UPDATE threads SET error =     CASE      WHEN NEW.err_type >= 10 THEN error + 1      ELSE error - 1    END   WHERE _id =   (SELECT DISTINCT thread_id    FROM pdu    WHERE _id = NEW.msg_id); END;");
        db.execSQL("CREATE TRIGGER update_threads_error_on_update_sms   AFTER UPDATE OF type ON sms  WHEN (OLD.type != 5 AND NEW.type = 5)    OR (OLD.type = 5 AND NEW.type != 5) BEGIN   UPDATE threads SET error =     CASE      WHEN NEW.type = 5 THEN error + 1      ELSE error - 1    END   WHERE _id = NEW.thread_id; END;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
        Log.w("MmsSmsDatabaseHelper", "Upgrading database from version " + oldVersion + " to " + currentVersion + ".");
        switch (oldVersion) {
            case 40:
                if (currentVersion > 40) {
                    db.beginTransaction();
                    try {
                        upgradeDatabaseToVersion41(db);
                        db.setTransactionSuccessful();
                        if (currentVersion <= 41) {
                            db.beginTransaction();
                            try {
                                upgradeDatabaseToVersion42(db);
                                db.setTransactionSuccessful();
                                if (currentVersion <= 42) {
                                    db.beginTransaction();
                                    try {
                                        upgradeDatabaseToVersion43(db);
                                        db.setTransactionSuccessful();
                                        if (currentVersion <= 43) {
                                            db.beginTransaction();
                                            try {
                                                upgradeDatabaseToVersion44(db);
                                                db.setTransactionSuccessful();
                                                if (currentVersion <= 44) {
                                                    db.beginTransaction();
                                                    try {
                                                        upgradeDatabaseToVersion45(db);
                                                        db.setTransactionSuccessful();
                                                        if (currentVersion <= 45) {
                                                            db.beginTransaction();
                                                            try {
                                                                upgradeDatabaseToVersion46(db);
                                                                db.setTransactionSuccessful();
                                                                if (currentVersion <= 46) {
                                                                    db.beginTransaction();
                                                                    try {
                                                                        upgradeDatabaseToVersion47(db);
                                                                        db.setTransactionSuccessful();
                                                                        if (currentVersion <= 47) {
                                                                            db.beginTransaction();
                                                                            try {
                                                                                upgradeDatabaseToVersion48(db);
                                                                                db.setTransactionSuccessful();
                                                                                if (currentVersion <= 48) {
                                                                                    db.beginTransaction();
                                                                                    try {
                                                                                        createWordsTables(db);
                                                                                        db.setTransactionSuccessful();
                                                                                        if (currentVersion <= 49) {
                                                                                            db.beginTransaction();
                                                                                            try {
                                                                                                createThreadIdIndex(db);
                                                                                                db.setTransactionSuccessful();
                                                                                                if (currentVersion <= 50) {
                                                                                                    db.beginTransaction();
                                                                                                    try {
                                                                                                        upgradeDatabaseToVersion51(db);
                                                                                                        db.setTransactionSuccessful();
                                                                                                        if (currentVersion <= 51) {
                                                                                                            return;
                                                                                                        }
                                                                                                        if (currentVersion <= 52) {
                                                                                                            db.beginTransaction();
                                                                                                            try {
                                                                                                                upgradeDatabaseToVersion53(db);
                                                                                                                db.setTransactionSuccessful();
                                                                                                                if (currentVersion <= 53) {
                                                                                                                    db.beginTransaction();
                                                                                                                    try {
                                                                                                                        upgradeDatabaseToVersion54(db);
                                                                                                                        db.setTransactionSuccessful();
                                                                                                                        if (currentVersion <= 54) {
                                                                                                                            db.beginTransaction();
                                                                                                                            try {
                                                                                                                                upgradeDatabaseToVersion55(db);
                                                                                                                                db.setTransactionSuccessful();
                                                                                                                                if (currentVersion <= 55) {
                                                                                                                                    db.beginTransaction();
                                                                                                                                    try {
                                                                                                                                        upgradeDatabaseToVersion56(db);
                                                                                                                                        db.setTransactionSuccessful();
                                                                                                                                        if (currentVersion <= 56) {
                                                                                                                                            db.beginTransaction();
                                                                                                                                            try {
                                                                                                                                                upgradeDatabaseToVersion57(db);
                                                                                                                                                db.setTransactionSuccessful();
                                                                                                                                                if (currentVersion <= 57) {
                                                                                                                                                    db.beginTransaction();
                                                                                                                                                    try {
                                                                                                                                                        upgradeDatabaseToVersion58(db);
                                                                                                                                                        db.setTransactionSuccessful();
                                                                                                                                                        if (currentVersion <= 58) {
                                                                                                                                                            db.beginTransaction();
                                                                                                                                                            try {
                                                                                                                                                                upgradeDatabaseToVersion59(db);
                                                                                                                                                                db.setTransactionSuccessful();
                                                                                                                                                                if (currentVersion <= 59) {
                                                                                                                                                                    db.beginTransaction();
                                                                                                                                                                    try {
                                                                                                                                                                        upgradeDatabaseToVersion60(db);
                                                                                                                                                                        db.setTransactionSuccessful();
                                                                                                                                                                        if (currentVersion <= 60) {
                                                                                                                                                                            db.beginTransaction();
                                                                                                                                                                            try {
                                                                                                                                                                                upgradeDatabaseToVersion61(db);
                                                                                                                                                                                db.setTransactionSuccessful();
                                                                                                                                                                                return;
                                                                                                                                                                            } catch (Throwable ex) {
                                                                                                                                                                                Log.e("MmsSmsDatabaseHelper", ex.getMessage(), ex);
                                                                                                                                                                                Log.e("MmsSmsDatabaseHelper", "Destroying all old data.");
                                                                                                                                                                                dropAll(db);
                                                                                                                                                                                onCreate(db);
                                                                                                                                                                                return;
                                                                                                                                                                            } finally {
                                                                                                                                                                            }
                                                                                                                                                                        }
                                                                                                                                                                        return;
                                                                                                                                                                    } catch (Throwable ex2) {
                                                                                                                                                                        Log.e("MmsSmsDatabaseHelper", ex2.getMessage(), ex2);
                                                                                                                                                                    } finally {
                                                                                                                                                                    }
                                                                                                                                                                } else {
                                                                                                                                                                    return;
                                                                                                                                                                }
                                                                                                                                                            } catch (Throwable ex3) {
                                                                                                                                                                Log.e("MmsSmsDatabaseHelper", ex3.getMessage(), ex3);
                                                                                                                                                            } finally {
                                                                                                                                                            }
                                                                                                                                                        } else {
                                                                                                                                                            return;
                                                                                                                                                        }
                                                                                                                                                    } catch (Throwable ex4) {
                                                                                                                                                        Log.e("MmsSmsDatabaseHelper", ex4.getMessage(), ex4);
                                                                                                                                                    } finally {
                                                                                                                                                    }
                                                                                                                                                } else {
                                                                                                                                                    return;
                                                                                                                                                }
                                                                                                                                            } catch (Throwable ex5) {
                                                                                                                                                Log.e("MmsSmsDatabaseHelper", ex5.getMessage(), ex5);
                                                                                                                                            } finally {
                                                                                                                                            }
                                                                                                                                        } else {
                                                                                                                                            return;
                                                                                                                                        }
                                                                                                                                    } catch (Throwable ex6) {
                                                                                                                                        Log.e("MmsSmsDatabaseHelper", ex6.getMessage(), ex6);
                                                                                                                                    } finally {
                                                                                                                                    }
                                                                                                                                } else {
                                                                                                                                    return;
                                                                                                                                }
                                                                                                                            } catch (Throwable ex7) {
                                                                                                                                Log.e("MmsSmsDatabaseHelper", ex7.getMessage(), ex7);
                                                                                                                            } finally {
                                                                                                                            }
                                                                                                                        } else {
                                                                                                                            return;
                                                                                                                        }
                                                                                                                    } catch (Throwable ex8) {
                                                                                                                        Log.e("MmsSmsDatabaseHelper", ex8.getMessage(), ex8);
                                                                                                                    } finally {
                                                                                                                    }
                                                                                                                } else {
                                                                                                                    return;
                                                                                                                }
                                                                                                            } catch (Throwable ex9) {
                                                                                                                Log.e("MmsSmsDatabaseHelper", ex9.getMessage(), ex9);
                                                                                                            } finally {
                                                                                                            }
                                                                                                        } else {
                                                                                                            return;
                                                                                                        }
                                                                                                    } catch (Throwable ex10) {
                                                                                                        Log.e("MmsSmsDatabaseHelper", ex10.getMessage(), ex10);
                                                                                                    } finally {
                                                                                                    }
                                                                                                } else {
                                                                                                    return;
                                                                                                }
                                                                                            } catch (Throwable ex11) {
                                                                                                Log.e("MmsSmsDatabaseHelper", ex11.getMessage(), ex11);
                                                                                            } finally {
                                                                                            }
                                                                                        } else {
                                                                                            return;
                                                                                        }
                                                                                    } catch (Throwable ex12) {
                                                                                        Log.e("MmsSmsDatabaseHelper", ex12.getMessage(), ex12);
                                                                                    } finally {
                                                                                    }
                                                                                } else {
                                                                                    return;
                                                                                }
                                                                            } catch (Throwable ex13) {
                                                                                Log.e("MmsSmsDatabaseHelper", ex13.getMessage(), ex13);
                                                                            } finally {
                                                                            }
                                                                        } else {
                                                                            return;
                                                                        }
                                                                    } catch (Throwable ex14) {
                                                                        Log.e("MmsSmsDatabaseHelper", ex14.getMessage(), ex14);
                                                                    } finally {
                                                                    }
                                                                } else {
                                                                    return;
                                                                }
                                                            } catch (Throwable ex15) {
                                                                Log.e("MmsSmsDatabaseHelper", ex15.getMessage(), ex15);
                                                            } finally {
                                                            }
                                                        } else {
                                                            return;
                                                        }
                                                    } catch (Throwable ex16) {
                                                        Log.e("MmsSmsDatabaseHelper", ex16.getMessage(), ex16);
                                                    } finally {
                                                    }
                                                } else {
                                                    return;
                                                }
                                            } catch (Throwable ex17) {
                                                Log.e("MmsSmsDatabaseHelper", ex17.getMessage(), ex17);
                                            } finally {
                                            }
                                        } else {
                                            return;
                                        }
                                    } catch (Throwable ex18) {
                                        Log.e("MmsSmsDatabaseHelper", ex18.getMessage(), ex18);
                                    } finally {
                                    }
                                } else {
                                    return;
                                }
                            } catch (Throwable ex19) {
                                Log.e("MmsSmsDatabaseHelper", ex19.getMessage(), ex19);
                            } finally {
                            }
                        } else {
                            return;
                        }
                    } catch (Throwable ex20) {
                        Log.e("MmsSmsDatabaseHelper", ex20.getMessage(), ex20);
                    } finally {
                    }
                } else {
                    return;
                }
                break;
            case 41:
                if (currentVersion <= 41) {
                }
                break;
            case 42:
                if (currentVersion <= 42) {
                }
                break;
            case 43:
                if (currentVersion <= 43) {
                }
                break;
            case 44:
                if (currentVersion <= 44) {
                }
                break;
            case 45:
                if (currentVersion <= 45) {
                }
                break;
            case 46:
                if (currentVersion <= 46) {
                }
                break;
            case 47:
                if (currentVersion <= 47) {
                }
                break;
            case 48:
                if (currentVersion <= 48) {
                }
                break;
            case 49:
                if (currentVersion <= 49) {
                }
                break;
            case 50:
                if (currentVersion <= 50) {
                }
                break;
            case 51:
                if (currentVersion <= 51) {
                }
            case 52:
                if (currentVersion <= 52) {
                }
                break;
            case 53:
                if (currentVersion <= 53) {
                }
                break;
            case 54:
                if (currentVersion <= 54) {
                }
                break;
            case 55:
                if (currentVersion <= 55) {
                }
                break;
            case 56:
                if (currentVersion <= 56) {
                }
                break;
            case 57:
                if (currentVersion <= 57) {
                }
                break;
            case 58:
                if (currentVersion <= 58) {
                }
                break;
            case 59:
                if (currentVersion <= 59) {
                }
                break;
            case 60:
                if (currentVersion <= 60) {
                }
            default:
                Log.e("MmsSmsDatabaseHelper", "Destroying all old data.");
                dropAll(db);
                onCreate(db);
                return;
        }
    }

    private void dropAll(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS canonical_addresses");
        db.execSQL("DROP TABLE IF EXISTS threads");
        db.execSQL("DROP TABLE IF EXISTS pending_msgs");
        db.execSQL("DROP TABLE IF EXISTS sms");
        db.execSQL("DROP TABLE IF EXISTS raw");
        db.execSQL("DROP TABLE IF EXISTS attachments");
        db.execSQL("DROP TABLE IF EXISTS thread_ids");
        db.execSQL("DROP TABLE IF EXISTS sr_pending");
        db.execSQL("DROP TABLE IF EXISTS pdu;");
        db.execSQL("DROP TABLE IF EXISTS addr;");
        db.execSQL("DROP TABLE IF EXISTS part;");
        db.execSQL("DROP TABLE IF EXISTS rate;");
        db.execSQL("DROP TABLE IF EXISTS drm;");
    }

    private void upgradeDatabaseToVersion41(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS update_threads_error_on_move_mms");
        db.execSQL("CREATE TRIGGER update_threads_error_on_move_mms   BEFORE UPDATE OF msg_box ON pdu   WHEN (OLD.msg_box = 4 AND NEW.msg_box != 4)   AND (OLD._id IN (SELECT DISTINCT msg_id                   FROM pending_msgs                   WHERE err_type >= 10)) BEGIN   UPDATE threads SET error = error - 1  WHERE _id = OLD.thread_id; END;");
    }

    private void upgradeDatabaseToVersion42(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_on_delete");
        db.execSQL("DROP TRIGGER IF EXISTS delete_obsolete_threads_sms");
        db.execSQL("DROP TRIGGER IF EXISTS update_threads_error_on_delete_sms");
    }

    private void upgradeDatabaseToVersion43(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE threads ADD COLUMN has_attachment INTEGER DEFAULT 0");
        updateThreadsAttachmentColumn(db);
        db.execSQL("CREATE TRIGGER update_threads_on_insert_part  AFTER INSERT ON part  WHEN new.ct != 'text/plain' AND new.ct != 'application/smil'  BEGIN   UPDATE threads SET has_attachment=1 WHERE _id IN    (SELECT pdu.thread_id FROM part JOIN pdu ON pdu._id=part.mid      WHERE part._id=new._id LIMIT 1);  END");
        db.execSQL("CREATE TRIGGER update_threads_on_delete_part  AFTER DELETE ON part  WHEN old.ct != 'text/plain' AND old.ct != 'application/smil'  BEGIN   UPDATE threads SET has_attachment =    CASE     (SELECT COUNT(*) FROM part JOIN pdu      WHERE pdu.thread_id = threads._id      AND part.ct != 'text/plain' AND part.ct != 'application/smil'      AND part.mid = pdu._id)   WHEN 0 THEN 0    ELSE 1    END;  END");
    }

    private void upgradeDatabaseToVersion44(SQLiteDatabase db) {
        updateThreadsAttachmentColumn(db);
        db.execSQL("CREATE TRIGGER update_threads_on_update_part  AFTER UPDATE of mid ON part  WHEN new.ct != 'text/plain' AND new.ct != 'application/smil'  BEGIN   UPDATE threads SET has_attachment=1 WHERE _id IN    (SELECT pdu.thread_id FROM part JOIN pdu ON pdu._id=part.mid      WHERE part._id=new._id LIMIT 1);  END");
    }

    private void upgradeDatabaseToVersion45(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE sms ADD COLUMN locked INTEGER DEFAULT 0");
        db.execSQL("ALTER TABLE pdu ADD COLUMN locked INTEGER DEFAULT 0");
    }

    private void upgradeDatabaseToVersion46(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE part ADD COLUMN text TEXT");
        Cursor textRows = db.query("part", new String[]{"_id", "_data", "text"}, "ct = 'text/plain' OR ct == 'application/smil'", null, null, null, null);
        ArrayList<String> filesToDelete = new ArrayList<>();
        try {
            db.beginTransaction();
            if (textRows != null) {
                int partDataColumn = textRows.getColumnIndex("_data");
                while (textRows.moveToNext()) {
                    String path = textRows.getString(partDataColumn);
                    if (path != null) {
                        try {
                            InputStream is = new FileInputStream(path);
                            byte[] data = new byte[is.available()];
                            is.read(data);
                            EncodedStringValue v = new EncodedStringValue(data);
                            db.execSQL("UPDATE part SET _data = NULL, text = ?", new String[]{v.getString()});
                            is.close();
                            filesToDelete.add(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            for (String pathToDelete : filesToDelete) {
                try {
                    new File(pathToDelete).delete();
                } catch (SecurityException ex) {
                    Log.e("MmsSmsDatabaseHelper", "unable to clean up old mms file for " + pathToDelete, ex);
                }
            }
            if (textRows != null) {
                textRows.close();
            }
        }
    }

    private void upgradeDatabaseToVersion47(SQLiteDatabase db) {
        updateThreadsAttachmentColumn(db);
        db.execSQL("CREATE TRIGGER update_threads_on_update_pdu  AFTER UPDATE of thread_id ON pdu  BEGIN   UPDATE threads SET has_attachment=1 WHERE _id IN    (SELECT pdu.thread_id FROM part JOIN pdu      WHERE part.ct != 'text/plain' AND part.ct != 'application/smil'      AND part.mid = pdu._id); END");
    }

    private void upgradeDatabaseToVersion48(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE sms ADD COLUMN error_code INTEGER DEFAULT 0");
    }

    private void upgradeDatabaseToVersion51(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE sms add COLUMN seen INTEGER DEFAULT 0");
        db.execSQL("ALTER TABLE pdu add COLUMN seen INTEGER DEFAULT 0");
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("seen", (Integer) 1);
            int count = db.update("sms", contentValues, "read=1", null);
            Log.d("MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeDatabaseToVersion51: updated " + count + " rows in sms table to have READ=1");
            int count2 = db.update("pdu", contentValues, "read=1", null);
            Log.d("MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeDatabaseToVersion51: updated " + count2 + " rows in pdu table to have READ=1");
        } catch (Exception ex) {
            Log.e("MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeDatabaseToVersion51 caught ", ex);
        }
    }

    private void upgradeDatabaseToVersion53(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_read_on_update");
        db.execSQL("CREATE TRIGGER pdu_update_thread_read_on_update AFTER  UPDATE OF read  ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128 BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
    }

    private void upgradeDatabaseToVersion54(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE sms ADD COLUMN date_sent INTEGER DEFAULT 0");
        db.execSQL("ALTER TABLE pdu ADD COLUMN date_sent INTEGER DEFAULT 0");
    }

    private void upgradeDatabaseToVersion55(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS delete_obsolete_threads_pdu");
        db.execSQL("DROP TRIGGER IF EXISTS delete_obsolete_threads_when_update_pdu");
    }

    private void upgradeDatabaseToVersion56(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE pdu ADD COLUMN text_only INTEGER DEFAULT 0");
    }

    private void upgradeDatabaseToVersion57(SQLiteDatabase db) {
        db.execSQL("DELETE FROM pdu WHERE thread_id IS NULL");
    }

    private void upgradeDatabaseToVersion58(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE pdu ADD COLUMN sub_id INTEGER DEFAULT -1");
        db.execSQL("ALTER TABLE pending_msgs ADD COLUMN pending_sub_id INTEGER DEFAULT -1");
        db.execSQL("ALTER TABLE sms ADD COLUMN sub_id INTEGER DEFAULT -1");
        db.execSQL("ALTER TABLE raw ADD COLUMN sub_id INTEGER DEFAULT -1");
    }

    private void upgradeDatabaseToVersion59(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE pdu ADD COLUMN creator TEXT");
        db.execSQL("ALTER TABLE sms ADD COLUMN creator TEXT");
    }

    private void upgradeDatabaseToVersion60(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE threads ADD COLUMN archived INTEGER DEFAULT 0");
    }

    private void upgradeDatabaseToVersion61(SQLiteDatabase db) {
        createMmsTriggers(db);
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase db;
        db = super.getWritableDatabase();
        if (!sTriedAutoIncrement) {
            sTriedAutoIncrement = true;
            boolean hasAutoIncrementThreads = hasAutoIncrement(db, "threads");
            boolean hasAutoIncrementAddresses = hasAutoIncrement(db, "canonical_addresses");
            boolean hasAutoIncrementPart = hasAutoIncrement(db, "part");
            boolean hasAutoIncrementPdu = hasAutoIncrement(db, "pdu");
            Log.d("MmsSmsDatabaseHelper", "[getWritableDatabase] hasAutoIncrementThreads: " + hasAutoIncrementThreads + " hasAutoIncrementAddresses: " + hasAutoIncrementAddresses + " hasAutoIncrementPart: " + hasAutoIncrementPart + " hasAutoIncrementPdu: " + hasAutoIncrementPdu);
            boolean autoIncrementThreadsSuccess = true;
            boolean autoIncrementAddressesSuccess = true;
            boolean autoIncrementPartSuccess = true;
            boolean autoIncrementPduSuccess = true;
            if (!hasAutoIncrementThreads) {
                db.beginTransaction();
                try {
                    try {
                        upgradeThreadsTableToAutoIncrement(db);
                        db.setTransactionSuccessful();
                        db.endTransaction();
                    } finally {
                    }
                } catch (Throwable ex) {
                    Log.e("MmsSmsDatabaseHelper", "Failed to add autoIncrement to threads;: " + ex.getMessage(), ex);
                    autoIncrementThreadsSuccess = false;
                    db.endTransaction();
                }
            }
            if (!hasAutoIncrementAddresses) {
                db.beginTransaction();
                try {
                    try {
                        upgradeAddressTableToAutoIncrement(db);
                        db.setTransactionSuccessful();
                    } finally {
                    }
                } catch (Throwable ex2) {
                    Log.e("MmsSmsDatabaseHelper", "Failed to add autoIncrement to canonical_addresses: " + ex2.getMessage(), ex2);
                    autoIncrementAddressesSuccess = false;
                    db.endTransaction();
                }
            }
            if (!hasAutoIncrementPart) {
                db.beginTransaction();
                try {
                    try {
                        upgradePartTableToAutoIncrement(db);
                        db.setTransactionSuccessful();
                        db.endTransaction();
                    } finally {
                    }
                } catch (Throwable ex3) {
                    Log.e("MmsSmsDatabaseHelper", "Failed to add autoIncrement to part: " + ex3.getMessage(), ex3);
                    autoIncrementPartSuccess = false;
                    db.endTransaction();
                }
            }
            if (!hasAutoIncrementPdu) {
                db.beginTransaction();
                try {
                    try {
                        upgradePduTableToAutoIncrement(db);
                        db.setTransactionSuccessful();
                    } finally {
                    }
                } catch (Throwable ex4) {
                    Log.e("MmsSmsDatabaseHelper", "Failed to add autoIncrement to pdu: " + ex4.getMessage(), ex4);
                    autoIncrementPduSuccess = false;
                    db.endTransaction();
                }
            }
            if (autoIncrementThreadsSuccess && autoIncrementAddressesSuccess && autoIncrementPartSuccess && autoIncrementPduSuccess) {
                if (this.mLowStorageMonitor != null) {
                    Log.d("MmsSmsDatabaseHelper", "Unregistering mLowStorageMonitor - we've upgraded");
                    this.mContext.unregisterReceiver(this.mLowStorageMonitor);
                    this.mLowStorageMonitor = null;
                }
            } else {
                if (sFakeLowStorageTest) {
                    sFakeLowStorageTest = false;
                }
                if (this.mLowStorageMonitor == null) {
                    Log.d("MmsSmsDatabaseHelper", "[getWritableDatabase] turning on storage monitor");
                    this.mLowStorageMonitor = new LowStorageMonitor();
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
                    intentFilter.addAction("android.intent.action.DEVICE_STORAGE_OK");
                    this.mContext.registerReceiver(this.mLowStorageMonitor, intentFilter);
                }
            }
        }
        return db;
    }

    private boolean hasAutoIncrement(SQLiteDatabase db, String tableName) {
        boolean result = false;
        String query = "SELECT sql FROM sqlite_master WHERE type='table' AND name='" + tableName + "'";
        Cursor c = db.rawQuery(query, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    String schema = c.getString(0);
                    result = schema != null ? schema.contains("AUTOINCREMENT") : false;
                    Log.d("MmsSmsDatabaseHelper", "[MmsSmsDb] tableName: " + tableName + " hasAutoIncrement: " + schema + " result: " + result);
                }
            } finally {
                c.close();
            }
        }
        return result;
    }

    private void upgradeThreadsTableToAutoIncrement(SQLiteDatabase db) {
        if (hasAutoIncrement(db, "threads")) {
            Log.d("MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeThreadsTableToAutoIncrement: already upgraded");
            return;
        }
        Log.d("MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeThreadsTableToAutoIncrement: upgrading");
        db.execSQL("CREATE TABLE threads_temp (_id INTEGER PRIMARY KEY AUTOINCREMENT,date INTEGER DEFAULT 0,message_count INTEGER DEFAULT 0,recipient_ids TEXT,snippet TEXT,snippet_cs INTEGER DEFAULT 0,read INTEGER DEFAULT 1,type INTEGER DEFAULT 0,error INTEGER DEFAULT 0,has_attachment INTEGER DEFAULT 0);");
        db.execSQL("INSERT INTO threads_temp SELECT * from threads;");
        db.execSQL("DROP TABLE threads;");
        db.execSQL("ALTER TABLE threads_temp RENAME TO threads;");
    }

    private void upgradeAddressTableToAutoIncrement(SQLiteDatabase db) {
        if (hasAutoIncrement(db, "canonical_addresses")) {
            Log.d("MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeAddressTableToAutoIncrement: already upgraded");
            return;
        }
        Log.d("MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeAddressTableToAutoIncrement: upgrading");
        db.execSQL("CREATE TABLE canonical_addresses_temp (_id INTEGER PRIMARY KEY AUTOINCREMENT,address TEXT);");
        db.execSQL("INSERT INTO canonical_addresses_temp SELECT * from canonical_addresses;");
        db.execSQL("DROP TABLE canonical_addresses;");
        db.execSQL("ALTER TABLE canonical_addresses_temp RENAME TO canonical_addresses;");
    }

    private void upgradePartTableToAutoIncrement(SQLiteDatabase db) {
        if (hasAutoIncrement(db, "part")) {
            Log.d("MmsSmsDatabaseHelper", "[MmsSmsDb] upgradePartTableToAutoIncrement: already upgraded");
            return;
        }
        Log.d("MmsSmsDatabaseHelper", "[MmsSmsDb] upgradePartTableToAutoIncrement: upgrading");
        db.execSQL("CREATE TABLE part_temp (_id INTEGER PRIMARY KEY AUTOINCREMENT,mid INTEGER,seq INTEGER DEFAULT 0,ct TEXT,name TEXT,chset INTEGER,cd TEXT,fn TEXT,cid TEXT,cl TEXT,ctt_s INTEGER,ctt_t TEXT,_data TEXT,text TEXT);");
        db.execSQL("INSERT INTO part_temp SELECT * from part;");
        db.execSQL("DROP TABLE part;");
        db.execSQL("ALTER TABLE part_temp RENAME TO part;");
        createMmsTriggers(db);
    }

    private void upgradePduTableToAutoIncrement(SQLiteDatabase db) {
        if (hasAutoIncrement(db, "pdu")) {
            Log.d("MmsSmsDatabaseHelper", "[MmsSmsDb] upgradePduTableToAutoIncrement: already upgraded");
            return;
        }
        Log.d("MmsSmsDatabaseHelper", "[MmsSmsDb] upgradePduTableToAutoIncrement: upgrading");
        db.execSQL("CREATE TABLE pdu_temp (_id INTEGER PRIMARY KEY AUTOINCREMENT,thread_id INTEGER,date INTEGER,date_sent INTEGER DEFAULT 0,msg_box INTEGER,read INTEGER DEFAULT 0,m_id TEXT,sub TEXT,sub_cs INTEGER,ct_t TEXT,ct_l TEXT,exp INTEGER,m_cls TEXT,m_type INTEGER,v INTEGER,m_size INTEGER,pri INTEGER,rr INTEGER,rpt_a INTEGER,resp_st INTEGER,st INTEGER,tr_id TEXT,retr_st INTEGER,retr_txt TEXT,retr_txt_cs INTEGER,read_status INTEGER,ct_cls INTEGER,resp_txt TEXT,d_tm INTEGER,d_rpt INTEGER,locked INTEGER DEFAULT 0,sub_id INTEGER DEFAULT -1, seen INTEGER DEFAULT 0,text_only INTEGER DEFAULT 0);");
        db.execSQL("INSERT INTO pdu_temp SELECT * from pdu;");
        db.execSQL("DROP TABLE pdu;");
        db.execSQL("ALTER TABLE pdu_temp RENAME TO pdu;");
        createMmsTriggers(db);
    }

    private class LowStorageMonitor extends BroadcastReceiver {
        public LowStorageMonitor() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("MmsSmsDatabaseHelper", "[LowStorageMonitor] onReceive intent " + action);
            if ("android.intent.action.DEVICE_STORAGE_OK".equals(action)) {
                boolean unused = MmsSmsDatabaseHelper.sTriedAutoIncrement = false;
            }
        }
    }

    private void updateThreadsAttachmentColumn(SQLiteDatabase db) {
        db.execSQL("UPDATE threads SET has_attachment=1 WHERE _id IN   (SELECT DISTINCT pdu.thread_id FROM part    JOIN pdu ON pdu._id=part.mid    WHERE part.ct != 'text/plain' AND part.ct != 'application/smil')");
    }
}
