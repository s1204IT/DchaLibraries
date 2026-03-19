package com.android.providers.telephony;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.providers.telephony.MmsSmsProvider;
import com.google.android.mms.pdu.EncodedStringValue;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MmsSmsDatabaseHelper extends SQLiteOpenHelper {
    private final Context mContext;
    private LowStorageMonitor mLowStorageMonitor;
    private static final boolean MTK_WAPPUSH_SUPPORT = SystemProperties.get("ro.vendor.mtk_wappush_support").equals("1");
    private static final boolean MTK_ONLY_OWNER_SIM_SUPPORT = SystemProperties.get("ro.mtk_owner_sim_support").equals("1");
    private static final boolean MTK_RCS_SUPPORT = "1".equals(SystemProperties.get("ro.vendor.mtk_op01_rcs"));
    private static MmsSmsDatabaseHelper sDeInstance = null;
    private static MmsSmsDatabaseHelper sCeInstance = null;
    private static final String[] BIND_ARGS_NONE = new String[0];
    private static boolean sTriedAutoIncrement = false;
    private static boolean sFakeLowStorageTest = false;

    @VisibleForTesting
    public static String CREATE_SMS_TABLE_STRING = "CREATE TABLE sms (_id INTEGER PRIMARY KEY,thread_id INTEGER,address TEXT,m_size INTEGER,person INTEGER,date INTEGER,date_sent INTEGER DEFAULT 0,protocol INTEGER,read INTEGER DEFAULT 0,status INTEGER DEFAULT -1,type INTEGER,reply_path_present INTEGER,subject TEXT,body TEXT,service_center TEXT,locked INTEGER DEFAULT 0,sub_id INTEGER DEFAULT -1, error_code INTEGER DEFAULT 0,creator TEXT,seen INTEGER DEFAULT 0,ipmsg_id INTEGER DEFAULT 0, ref_id INTEGER,total_len INTEGER,rec_len INTEGER);";

    @VisibleForTesting
    public static String CREATE_ATTACHMENTS_TABLE_STRING = "CREATE TABLE attachments (sms_id INTEGER,content_url TEXT,offset INTEGER);";

    @VisibleForTesting
    public static String CREATE_RAW_TABLE_STRING = "CREATE TABLE raw (_id INTEGER PRIMARY KEY,date INTEGER,reference_number INTEGER,count INTEGER,sequence INTEGER,destination_port INTEGER,address TEXT,sub_id INTEGER DEFAULT -1, pdu TEXT,deleted INTEGER DEFAULT 0,message_body TEXT,display_originating_addr TEXT);";
    private static String UPDATE_RCS_THREAD_AFTER_INSERT_RCS_MESSAGE_DAPI = "CREATE TRIGGER update_rcs_thread_after_insert_rcs_message_dapi  AFTER INSERT ON rcs_message WHEN NEW.class=0   AND NEW.CHATMESSAGE_ISBLOCKED=0  BEGIN   UPDATE rcs_conversations     SET       DAPI_CONVERSATION_BODY=           (CASE WHEN rcs_conversations.DAPI_CONVERSATION_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP          THEN NEW.CHATMESSAGE_BODY          ELSE rcs_conversations.DAPI_CONVERSATION_BODY END),       DAPI_CONVERSATION_TIMESTAMP=           (CASE WHEN rcs_conversations.DAPI_CONVERSATION_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP          THEN NEW.CHATMESSAGE_TIMESTAMP          ELSE rcs_conversations.DAPI_CONVERSATION_TIMESTAMP END),       DAPI_CONVERSATION_MSG_COUNT= DAPI_CONVERSATION_MSG_COUNT+1,       DAPI_CONVERSATION_UNREAD_COUNT=            (CASE WHEN NEW.CHATMESSAGE_MESSAGE_STATUS=0           THEN DAPI_CONVERSATION_UNREAD_COUNT+1            ELSE DAPI_CONVERSATION_UNREAD_COUNT END),       DAPI_CONVERSATION_MIMETYPE=            (CASE WHEN rcs_conversations.DAPI_CONVERSATION_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP           THEN NEW.CHATMESSAGE_MIME_TYPE           ELSE rcs_conversations.DAPI_CONVERSATION_MIMETYPE END),       DAPI_CONVERSATION_TYPE=            (CASE WHEN rcs_conversations.DAPI_CONVERSATION_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP           THEN NEW.CHATMESSAGE_TYPE           ELSE rcs_conversations.DAPI_CONVERSATION_TYPE END),       DAPI_CONVERSATION_STATUS=            (CASE WHEN rcs_conversations.DAPI_CONVERSATION_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP           THEN NEW.CHATMESSAGE_MESSAGE_STATUS           ELSE rcs_conversations.DAPI_CONVERSATION_STATUS END)   WHERE rcs_conversations.CHATMESSAGE_CONVERSATION=NEW.CHATMESSAGE_CONVERSATION;  END;";

    private MmsSmsDatabaseHelper(Context context) {
        super(context, "mmssms.db", (SQLiteDatabase.CursorFactory) null, 670010);
        this.mContext = context;
    }

    static synchronized MmsSmsDatabaseHelper getInstanceForDe(Context context) {
        if (sDeInstance == null) {
            sDeInstance = new MmsSmsDatabaseHelper(ProviderUtil.getDeviceEncryptedContext(context));
        }
        return sDeInstance;
    }

    static synchronized MmsSmsDatabaseHelper getInstanceForCe(Context context) {
        if (sCeInstance == null) {
            if (StorageManager.isFileEncryptedNativeOrEmulated()) {
                sCeInstance = new MmsSmsDatabaseHelper(ProviderUtil.getCredentialEncryptedContext(context));
            } else {
                sCeInstance = getInstanceForDe(context);
            }
        }
        return sCeInstance;
    }

    public static void updateThread(SQLiteDatabase sQLiteDatabase, long j) {
        int count;
        String str;
        Cursor cursorRawQuery;
        if (j < 0) {
            updateAllThreads(sQLiteDatabase, null, null);
            return;
        }
        sQLiteDatabase.beginTransaction();
        try {
            try {
                if (MTK_WAPPUSH_SUPPORT) {
                    cursorRawQuery = sQLiteDatabase.rawQuery("select * from threads where type=2 AND _id=" + j, null);
                    if (cursorRawQuery != null) {
                        try {
                            if (cursorRawQuery.getCount() != 0) {
                                return;
                            } else {
                                cursorRawQuery.close();
                            }
                        } finally {
                        }
                    }
                }
                if ((MTK_WAPPUSH_SUPPORT ? sQLiteDatabase.delete("threads", "status = 0 AND _id = ? AND type <> ? AND _id NOT IN          (SELECT thread_id FROM sms where thread_id is not null            UNION SELECT thread_id FROM pdu where thread_id is not null)", new String[]{String.valueOf(j), String.valueOf(2)}) : sQLiteDatabase.delete("threads", "status = 0 AND _id = ? AND _id NOT IN          (SELECT thread_id FROM sms where thread_id is not null            UNION SELECT thread_id FROM pdu where thread_id is not null)", new String[]{String.valueOf(j)})) > 0) {
                    if (ThreadCache.getInstance() != null) {
                        ThreadCache.getInstance().remove(j);
                    }
                    Log.d("Mms/Provider/MmsSmsDatabaseHelper", "Delete wallpaper: begin");
                    File file = new File("/data/data/com.android.providers.telephony/app_wallpaper");
                    if (file.exists()) {
                        String str2 = j + ".jpeg";
                        Log.d("Mms/Provider/MmsSmsDatabaseHelper", "ThreadId: " + str2);
                        String[] list = file.list();
                        int length = list.length;
                        Log.d("Mms/Provider/MmsSmsDatabaseHelper", "i: " + length);
                        if (length > 0) {
                            for (int i = 0; i < length; i++) {
                                if (str2.equals(list[i])) {
                                    Log.d("Mms/Provider/MmsSmsDatabaseHelper", "isDelete " + new File("/data/data/com.android.providers.telephony/app_wallpaper", list[i]).delete());
                                }
                            }
                        }
                    }
                    removeOrphanedAddresses(sQLiteDatabase);
                } else {
                    updateThreadReadAfterDeleteMessage(sQLiteDatabase, j);
                    sQLiteDatabase.execSQL("  UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = " + j + "        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = " + j + "        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = " + j + ";");
                    StringBuilder sb = new StringBuilder();
                    sb.append("  UPDATE threads  SET  readcount =    (SELECT count(_id) FROM       (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms        WHERE ((read=1) AND thread_id = ");
                    sb.append(j);
                    sb.append(" AND (type != 3))        UNION        SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read        FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id        WHERE ((read=1) AND thread_id = ");
                    sb.append(j);
                    sb.append(" AND msg_box != 3 AND (msg_box != 3        AND (m_type = 128 OR m_type = 132 OR m_type = 130)))        UNION        SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast        WHERE ((read=1) AND thread_id = ");
                    sb.append(j);
                    sb.append(") ORDER BY normalized_date ASC)) , date_sent =    (SELECT date_sent FROM      (SELECT date_sent * 1000 as date_sent, date * 1000 AS date, thread_id FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128      UNION SELECT date_sent, date, thread_id FROM sms)    WHERE thread_id = ");
                    sb.append(j);
                    sb.append(" ORDER BY date DESC LIMIT 1)   WHERE threads._id = ");
                    sb.append(j);
                    sb.append("; ");
                    sQLiteDatabase.execSQL(sb.toString());
                    sQLiteDatabase.execSQL("  UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu         UNION SELECT date, thread_id FROM sms)     WHERE thread_id = " + j + " ORDER BY date DESC LIMIT 1),  snippet =    (SELECT snippet FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id FROM sms)     WHERE thread_id = " + j + " ORDER BY date DESC LIMIT 1),  snippet_cs =    (SELECT snippet_cs FROM        (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, 0 AS snippet_cs, thread_id FROM sms)     WHERE thread_id = " + j + " ORDER BY date DESC LIMIT 1),  has_attachment =    CASE     (SELECT COUNT(*) FROM part JOIN pdu      WHERE pdu.thread_id = " + j + "     AND part.ct != 'text/plain' AND part.ct != 'application/smil'      AND part.mid = pdu._id)   WHEN 0 THEN 0    ELSE 1    END   WHERE threads._id = " + j + ";");
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("SELECT thread_id FROM sms WHERE (type = 5 OR status >= 64) AND thread_id = ");
                    sb2.append(j);
                    Cursor cursorRawQuery2 = sQLiteDatabase.rawQuery(sb2.toString(), null);
                    if (cursorRawQuery2 != null) {
                        count = cursorRawQuery2.getCount();
                        cursorRawQuery2.close();
                    } else {
                        count = 0;
                    }
                    Cursor cursorRawQuery3 = sQLiteDatabase.rawQuery("SELECT _id FROM pdu WHERE thread_id = " + j + " AND m_type = 128", null);
                    str = " IN (";
                    if (cursorRawQuery3 != null) {
                        str = cursorRawQuery3.moveToFirst() ? " IN (" + cursorRawQuery3.getInt(0) : " IN (";
                        while (cursorRawQuery3.moveToNext()) {
                            str = str + "," + cursorRawQuery3.getInt(0);
                        }
                        cursorRawQuery3.close();
                    }
                    String str3 = str + ")";
                    cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id, err_type FROM pending_msgs WHERE err_type >= 10 AND msg_id " + str3, null);
                    if (cursorRawQuery != null) {
                        count += cursorRawQuery.getCount();
                    }
                    Log.d("Mms/Provider/MmsSmsDatabaseHelper", "updateThread, set error with mms id " + str3 + "; setError = " + count);
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("SELECT error FROM threads WHERE _id = ");
                    sb3.append(j);
                    cursorRawQuery = sQLiteDatabase.rawQuery(sb3.toString(), null);
                    if (cursorRawQuery != null) {
                        try {
                            if (cursorRawQuery.moveToNext() && cursorRawQuery.getInt(0) != count) {
                                sQLiteDatabase.execSQL("UPDATE threads SET error=" + count + " WHERE _id = " + j);
                            }
                            cursorRawQuery.close();
                        } finally {
                        }
                    }
                }
                sQLiteDatabase.setTransactionSuccessful();
            } catch (Throwable th) {
                Log.e("Mms/Provider/MmsSmsDatabaseHelper", th.getMessage(), th);
            }
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    public static void updateAllThreads(SQLiteDatabase sQLiteDatabase, String str, String[] strArr) {
        String str2;
        String[] strArr2;
        Log.d("Mms/Provider/MmsSmsDatabaseHelper", "updateAllThreads start");
        sQLiteDatabase.beginTransaction();
        try {
            if (str == null) {
                str2 = "";
            } else {
                try {
                    str2 = "WHERE (" + str + ")";
                } catch (Throwable th) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th.getMessage(), th);
                }
            }
            String str3 = "SELECT _id FROM threads WHERE status<>0 OR _id IN (SELECT DISTINCT thread_id FROM sms " + str2 + " UNION SELECT DISTINCT thread_id FROM pdu " + str2 + ")";
            Log.d("Mms/Provider/MmsSmsDatabaseHelper", "updateAllThreads query " + str3);
            if (strArr != null && strArr.length > 0) {
                strArr2 = new String[strArr.length * 2];
                for (int i = 0; i < strArr.length; i++) {
                    strArr2[i] = strArr[i];
                    strArr2[strArr.length + i] = strArr[i];
                }
            } else {
                strArr2 = null;
            }
            Cursor cursorRawQuery = sQLiteDatabase.rawQuery(str3, strArr2);
            if (cursorRawQuery != null) {
                while (cursorRawQuery.moveToNext()) {
                    try {
                        updateThread(sQLiteDatabase, cursorRawQuery.getInt(0));
                    } catch (Throwable th2) {
                        cursorRawQuery.close();
                        throw th2;
                    }
                }
                cursorRawQuery.close();
            }
            if (MTK_WAPPUSH_SUPPORT) {
                sQLiteDatabase.delete("threads", "status = 0 AND _id NOT IN (SELECT DISTINCT thread_id FROM sms where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM pdu where thread_id NOT NULL) AND type <> 2", null);
            } else {
                sQLiteDatabase.delete("threads", "status = 0 AND _id NOT IN (SELECT DISTINCT thread_id FROM sms where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM pdu where thread_id NOT NULL )", null);
            }
            removeOrphanedAddresses(sQLiteDatabase);
            sQLiteDatabase.setTransactionSuccessful();
            sQLiteDatabase.endTransaction();
            Log.d("Mms/Provider/MmsSmsDatabaseHelper", "updateAllThreads end");
        } catch (Throwable th3) {
            sQLiteDatabase.endTransaction();
            throw th3;
        }
    }

    protected static void removeOrphanedAddresses(SQLiteDatabase sQLiteDatabase) {
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT DISTINCT recipient_ids FROM threads", null);
        StringBuilder sb = new StringBuilder();
        if (cursorRawQuery != null) {
            try {
                if (cursorRawQuery.moveToFirst()) {
                    do {
                        String string = cursorRawQuery.getString(0);
                        if (!TextUtils.isEmpty(string)) {
                            String strTrim = string.trim();
                            if (!TextUtils.isEmpty(strTrim)) {
                                sb.append(strTrim.replaceAll(" ", ","));
                                sb.append(",");
                            }
                        }
                    } while (cursorRawQuery.moveToNext());
                }
            } finally {
                if (cursorRawQuery != null) {
                    cursorRawQuery.close();
                }
            }
        }
        String string2 = sb.toString();
        if (!TextUtils.isEmpty(string2) && string2.endsWith(",")) {
            string2 = string2.substring(0, string2.lastIndexOf(","));
        }
        if (!TextUtils.isEmpty(string2) && string2.startsWith(",")) {
            string2 = string2.substring(1, string2.length());
        }
        MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/MmsSmsDatabaseHelper", "removeOrphanedAddresses, recipient ids = " + string2);
        sQLiteDatabase.delete("canonical_addresses", "_id NOT IN (" + string2 + ")", null);
    }

    public static int deleteOneSms(SQLiteDatabase sQLiteDatabase, int i) {
        Cursor cursorQuery = sQLiteDatabase.query("sms", new String[]{"thread_id"}, "_id=" + i, null, null, null, null);
        int i2 = -1;
        if (cursorQuery != null) {
            if (cursorQuery.moveToFirst()) {
                i2 = cursorQuery.getInt(0);
            }
            cursorQuery.close();
        }
        int iDelete = sQLiteDatabase.delete("sms", "_id=" + i, null);
        if (i2 > 0) {
            updateThread(sQLiteDatabase, i2);
        }
        return iDelete;
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        if (MTK_WAPPUSH_SUPPORT) {
            createWapPushTables(sQLiteDatabase);
        }
        createMmsTables(sQLiteDatabase);
        createSmsTables(sQLiteDatabase);
        createCommonTables(sQLiteDatabase);
        createCBTables(sQLiteDatabase);
        createThreadSettingsTable(sQLiteDatabase);
        createCommonTriggers(sQLiteDatabase);
        createMmsTriggers(sQLiteDatabase);
        createWordsTables(sQLiteDatabase);
        createIndices(sQLiteDatabase);
        createQuickText(sQLiteDatabase);
        if (MTK_ONLY_OWNER_SIM_SUPPORT) {
            createUserSmsTable(sQLiteDatabase);
            createUserMmsTable(sQLiteDatabase);
            createUserCBTable(sQLiteDatabase);
        }
        createMwiTables(sQLiteDatabase);
        if (MTK_RCS_SUPPORT) {
            createRCSTables(sQLiteDatabase);
        }
    }

    private void populateWordsTable(SQLiteDatabase sQLiteDatabase) {
        Cursor cursorQuery = sQLiteDatabase.query("sms", new String[]{"_id", "body"}, null, null, null, null, null);
        if (cursorQuery != null) {
            try {
                cursorQuery.moveToPosition(-1);
                ContentValues contentValues = new ContentValues();
                while (cursorQuery.moveToNext()) {
                    contentValues.clear();
                    long j = cursorQuery.getLong(0);
                    String string = cursorQuery.getString(1);
                    contentValues.put("_id", Long.valueOf(j));
                    contentValues.put("index_text", string);
                    contentValues.put("source_id", Long.valueOf(j));
                    contentValues.put("table_to_use", (Integer) 1);
                    sQLiteDatabase.insert("words", "index_text", contentValues);
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        Cursor cursorQuery2 = sQLiteDatabase.query("part", new String[]{"_id", "text"}, "ct = 'text/plain'", null, null, null, null);
        if (cursorQuery2 != null) {
            try {
                cursorQuery2.moveToPosition(-1);
                ContentValues contentValues2 = new ContentValues();
                while (cursorQuery2.moveToNext()) {
                    contentValues2.clear();
                    long j2 = cursorQuery2.getLong(0);
                    String string2 = cursorQuery2.getString(1);
                    contentValues2.put("_id", Long.valueOf(j2));
                    contentValues2.put("index_text", string2);
                    contentValues2.put("source_id", Long.valueOf(j2));
                    contentValues2.put("table_to_use", (Integer) 1);
                    sQLiteDatabase.insert("words", "index_text", contentValues2);
                }
            } finally {
                if (cursorQuery2 != null) {
                    cursorQuery2.close();
                }
            }
        }
        if (cursorQuery2 != null) {
            cursorQuery2.close();
        }
        cursorQuery2 = sQLiteDatabase.query("wappush", new String[]{"_id", "url", "text"}, null, null, null, null, null);
        if (cursorQuery2 != null) {
            try {
                cursorQuery2.moveToPosition(-1);
                ContentValues contentValues3 = new ContentValues();
                while (cursorQuery2.moveToNext()) {
                    contentValues3.clear();
                    long j3 = cursorQuery2.getLong(0);
                    String string3 = cursorQuery2.getString(1);
                    String string4 = cursorQuery2.getString(2);
                    contentValues3.put("_id", Long.valueOf(j3));
                    contentValues3.put("index_text", string4 + " " + string3);
                    contentValues3.put("source_id", Long.valueOf(j3));
                    contentValues3.put("table_to_use", (Integer) 1);
                    sQLiteDatabase.insert("words", "index_text", contentValues3);
                }
            } finally {
                if (cursorQuery2 != null) {
                    cursorQuery2.close();
                }
            }
        }
    }

    private void createWordsTables(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.execSQL("CREATE VIRTUAL TABLE words USING FTS3 (_id INTEGER PRIMARY KEY, index_text TEXT, source_id INTEGER, table_to_use INTEGER);");
            sQLiteDatabase.execSQL("CREATE TRIGGER sms_words_update AFTER UPDATE OF body ON sms BEGIN UPDATE words  SET index_text = NEW.body WHERE (source_id=NEW._id AND table_to_use=1);  END;");
            sQLiteDatabase.execSQL("CREATE TRIGGER sms_words_delete AFTER DELETE ON sms BEGIN DELETE FROM   words WHERE source_id = OLD._id AND table_to_use = 1; END;");
            sQLiteDatabase.execSQL("CREATE TRIGGER wp_words_update AFTER UPDATE ON wappush BEGIN UPDATE words  SET index_text = coalesce(NEW.text||' '||NEW.url,NEW.text,NEW.url) WHERE (source_id=NEW._id AND table_to_use=3); END;");
            sQLiteDatabase.execSQL("CREATE TRIGGER wp_words_delete AFTER DELETE ON wappush BEGIN DELETE FROM  words WHERE source_id = OLD._id AND table_to_use = 3; END;");
            populateWordsTable(sQLiteDatabase);
        } catch (Exception e) {
            Log.e("Mms/Provider/MmsSmsDatabaseHelper", "got exception creating words table: " + e.toString());
        }
    }

    private void createIndices(SQLiteDatabase sQLiteDatabase) {
        createThreadIdIndex(sQLiteDatabase);
        createThreadIdDateIndex(sQLiteDatabase);
        createPartMidIndex(sQLiteDatabase);
        createAddrMsgIdIndex(sQLiteDatabase);
    }

    private void createThreadIdIndex(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS typeThreadIdIndex ON sms (type, thread_id);");
        } catch (Exception e) {
            Log.e("Mms/Provider/MmsSmsDatabaseHelper", "got exception creating indices: " + e.toString());
        }
    }

    private void createThreadIdDateIndex(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS threadIdDateIndex ON sms (thread_id, date);");
        } catch (Exception e) {
            Log.e("Mms/Provider/MmsSmsDatabaseHelper", "got exception creating indices: " + e.toString());
        }
    }

    private void createPartMidIndex(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS partMidIndex ON part (mid)");
        } catch (Exception e) {
            Log.e("Mms/Provider/MmsSmsDatabaseHelper", "got exception creating indices: " + e.toString());
        }
    }

    private void createAddrMsgIdIndex(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS addrMsgIdIndex ON addr (msg_id)");
        } catch (Exception e) {
            Log.e("Mms/Provider/MmsSmsDatabaseHelper", "got exception creating indices: " + e.toString());
        }
    }

    private void createMmsTables(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE pdu (_id INTEGER PRIMARY KEY AUTOINCREMENT,thread_id INTEGER,date INTEGER,date_sent INTEGER DEFAULT 0,msg_box INTEGER,read INTEGER DEFAULT 0,m_id TEXT,sub TEXT,sub_cs INTEGER,ct_t TEXT,ct_l TEXT,exp INTEGER,m_cls TEXT,m_type INTEGER,v INTEGER,m_size INTEGER,pri INTEGER,rr INTEGER,rpt_a INTEGER,resp_st INTEGER,st INTEGER,st_ext INTEGER DEFAULT 0,tr_id TEXT,retr_st INTEGER,retr_txt TEXT,retr_txt_cs INTEGER,read_status INTEGER,ct_cls INTEGER,resp_txt TEXT,d_tm INTEGER,d_rpt INTEGER,locked INTEGER DEFAULT 0,sub_id INTEGER DEFAULT -1, service_center TEXT,seen INTEGER DEFAULT 0,creator TEXT,text_only INTEGER DEFAULT 0);");
        sQLiteDatabase.execSQL("CREATE TABLE addr (_id INTEGER PRIMARY KEY,msg_id INTEGER,contact_id INTEGER,address TEXT,type INTEGER,charset INTEGER);");
        sQLiteDatabase.execSQL("CREATE TABLE part (_id INTEGER PRIMARY KEY AUTOINCREMENT,mid INTEGER,seq INTEGER DEFAULT 0,ct TEXT,name TEXT,chset INTEGER,cd TEXT,fn TEXT,cid TEXT,cl TEXT,ctt_s INTEGER,ctt_t TEXT,_data TEXT,text TEXT);");
        sQLiteDatabase.execSQL("CREATE TABLE rate (sent_time INTEGER);");
        sQLiteDatabase.execSQL("CREATE TABLE drm (_id INTEGER PRIMARY KEY,_data TEXT);");
        sQLiteDatabase.execSQL("CREATE VIEW pdu_restricted AS SELECT * FROM pdu WHERE (msg_box=1 OR msg_box=2) AND (m_type!=130);");
    }

    private void createMmsTriggers(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS part_cleanup");
        sQLiteDatabase.execSQL("CREATE TRIGGER part_cleanup DELETE ON pdu BEGIN   DELETE FROM part  WHERE mid=old._id;END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS addr_cleanup");
        sQLiteDatabase.execSQL("CREATE TRIGGER addr_cleanup DELETE ON pdu BEGIN   DELETE FROM addr  WHERE msg_id=old._id;END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS cleanup_delivery_and_read_report");
        sQLiteDatabase.execSQL("CREATE TRIGGER cleanup_delivery_and_read_report AFTER DELETE ON pdu WHEN old.m_type=128 BEGIN   DELETE FROM pdu  WHERE (m_type=134    OR m_type=136)    AND m_id=old.m_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_threads_on_insert_part");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_on_insert_part  AFTER INSERT ON part  WHEN new.ct != 'text/plain' AND new.ct != 'application/smil'  BEGIN   UPDATE threads SET has_attachment=1 WHERE _id IN    (SELECT pdu.thread_id FROM part JOIN pdu ON pdu._id=part.mid      WHERE part._id=new._id LIMIT 1);  END");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_threads_on_update_part");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_on_update_part  AFTER UPDATE of mid ON part  WHEN new.ct != 'text/plain' AND new.ct != 'application/smil'  BEGIN   UPDATE threads SET has_attachment=1 WHERE _id IN    (SELECT pdu.thread_id FROM part JOIN pdu ON pdu._id=part.mid      WHERE part._id=new._id LIMIT 1);  END");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_threads_on_delete_part");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_on_delete_part  AFTER DELETE ON part  WHEN old.ct != 'text/plain' AND old.ct != 'application/smil'  BEGIN   UPDATE threads SET has_attachment =    CASE     (SELECT COUNT(*) FROM part JOIN pdu      ON pdu._id=old.mid AND part.mid=pdu._id      WHERE part.ct != 'text/plain' AND part.ct != 'application/smil')    WHEN 0 THEN 0    ELSE 1    END    WHERE threads._id=(SELECT thread_id FROM pdu WHERE _id=old.mid);  END");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_threads_on_update_pdu");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_on_update_pdu  AFTER UPDATE of thread_id ON pdu  BEGIN   UPDATE threads SET has_attachment=1 WHERE _id IN    (SELECT pdu.thread_id FROM part JOIN pdu      WHERE part.ct != 'text/plain' AND part.ct != 'application/smil'      AND part.mid = pdu._id); END");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS delete_mms_pending_on_delete");
        sQLiteDatabase.execSQL("CREATE TRIGGER delete_mms_pending_on_delete AFTER DELETE ON pdu BEGIN   DELETE FROM pending_msgs  WHERE msg_id=old._id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS delete_mms_pending_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER delete_mms_pending_on_update AFTER UPDATE ON pdu WHEN (old.msg_box=4   OR old.msg_box=5)  AND new.msg_box!=4  AND new.msg_box!=5 BEGIN   DELETE FROM pending_msgs  WHERE msg_id=new._id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS insert_mms_pending_on_insert");
        sQLiteDatabase.execSQL("CREATE TRIGGER insert_mms_pending_on_insert AFTER INSERT ON pdu WHEN new.m_type=130  OR new.m_type=135 BEGIN   INSERT INTO pending_msgs    (proto_type,     msg_id,     msg_type,     err_type,     err_code,     retry_index,     due_time)   VALUES     (1,      new._id,      new.m_type,0,0,0,0);END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS insert_mms_pending_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER insert_mms_pending_on_update AFTER UPDATE ON pdu WHEN new.m_type=128  AND new.msg_box=4  AND old.msg_box!=4  AND old.msg_box!=5 BEGIN   INSERT INTO pending_msgs    (proto_type,     msg_id,     msg_type,     err_type,     err_code,     retry_index,     due_time)   VALUES     (1,      new._id,      new.m_type,0,0,0,0);END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS mms_words_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER mms_words_update AFTER UPDATE ON part BEGIN UPDATE words  SET index_text = NEW.text WHERE (source_id=NEW._id AND table_to_use=2);  END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS mms_words_delete");
        sQLiteDatabase.execSQL("CREATE TRIGGER mms_words_delete AFTER DELETE ON part BEGIN DELETE FROM  words WHERE source_id = OLD._id AND table_to_use = 2; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_date_subject_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_date_subject_on_update AFTER  UPDATE OF date, sub, msg_box  ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128  BEGIN  UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id;  END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_on_delete");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_on_insert");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_on_insert AFTER INSERT ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128   BEGIN  UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (new.m_type=132 OR new.m_type=130 OR new.m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id; UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id;   UPDATE threads SET status = 0   WHERE (new.m_type=132 OR new.m_type=130 OR new.m_type=128) AND threads._id=new.thread_id;   END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_read_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_read_on_update AFTER  UPDATE OF read  ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128 BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_threads_error_on_delete_mms");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_error_on_delete_mms   BEFORE DELETE ON pdu  WHEN OLD._id IN (SELECT DISTINCT msg_id                   FROM pending_msgs                   WHERE err_type >= 10) BEGIN   UPDATE threads SET error = error - 1  WHERE _id = OLD.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_threads_error_on_move_mms");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_error_on_move_mms   BEFORE UPDATE OF msg_box ON pdu   WHEN ((OLD.msg_box = 4 OR OLD.msg_box = 5)   AND NEW.msg_box != 4 AND NEW.msg_box != 5)   AND (OLD._id IN (SELECT DISTINCT msg_id                   FROM pending_msgs                   WHERE err_type >= 10)) BEGIN   UPDATE threads SET error = error - 1  WHERE _id = OLD.thread_id; END;");
    }

    private void createSmsTables(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(CREATE_SMS_TABLE_STRING);
        sQLiteDatabase.execSQL(CREATE_RAW_TABLE_STRING);
        sQLiteDatabase.execSQL(CREATE_ATTACHMENTS_TABLE_STRING);
        sQLiteDatabase.execSQL("CREATE TABLE sr_pending (reference_number INTEGER,action TEXT,data TEXT);");
        sQLiteDatabase.execSQL("CREATE VIEW sms_restricted AS SELECT * FROM sms WHERE type=1 OR type=2;");
    }

    private void createCommonTables(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE canonical_addresses (_id INTEGER PRIMARY KEY AUTOINCREMENT,address TEXT);");
        sQLiteDatabase.execSQL("CREATE TABLE threads (_id INTEGER PRIMARY KEY AUTOINCREMENT,date INTEGER DEFAULT 0,date_sent INTEGER DEFAULT 0,message_count INTEGER DEFAULT 0,readcount INTEGER DEFAULT 0,recipient_ids TEXT,snippet TEXT,snippet_cs INTEGER DEFAULT 0,read INTEGER DEFAULT 1,archived INTEGER DEFAULT 0,type INTEGER DEFAULT 0,error INTEGER DEFAULT 0,has_attachment INTEGER DEFAULT 0,li_date INTEGER DEFAULT 0,li_snippet TEXT,li_snippet_cs INTEGER DEFAULT 0,status INTEGER DEFAULT 0);");
        sQLiteDatabase.execSQL("CREATE TABLE pending_msgs (_id INTEGER PRIMARY KEY,proto_type INTEGER,msg_id INTEGER,msg_type INTEGER,err_type INTEGER,err_code INTEGER,retry_index INTEGER NOT NULL DEFAULT 0,due_time INTEGER,pending_sub_id INTEGER DEFAULT 0, last_try INTEGER);");
    }

    private void createCommonTriggers(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_on_insert AFTER INSERT ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128   BEGIN  UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (new.m_type=132 OR new.m_type=130 OR new.m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id; UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id;   UPDATE threads SET status = 0   WHERE (new.m_type=132 OR new.m_type=130 OR new.m_type=128) AND threads._id=new.thread_id;   END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_on_insert AFTER INSERT ON sms BEGIN UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE ((SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id) +          (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_date_subject_on_update AFTER  UPDATE OF date, body, type  ON sms BEGIN UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE ((SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id) +          (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_read_on_update AFTER  UPDATE OF read  ON sms BEGIN   UPDATE threads SET read =     CASE ((SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id) +          (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_on_add AFTER  UPDATE OF thread_id ON sms BEGIN UPDATE threads SET date = (SELECT max(date) as date FROM (SELECT date * 1000 as date FROM pdu WHERE (thread_id = new.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT date FROM sms WHERE (thread_id = new.thread_id))) WHERE _id = new.thread_id;UPDATE threads SET date_sent = (SELECT max(date_sent) as date_sent FROM (SELECT date_sent * 1000 as date_sent FROM pdu WHERE (thread_id = new.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT date_sent FROM sms WHERE (thread_id = new.thread_id))) WHERE _id = new.thread_id;UPDATE threads SET message_count = (SELECT count(_id) FROM (SELECT _id FROM sms WHERE thread_id = new.thread_id UNION SELECT _id FROM pdu WHERE thread_id = new.thread_id AND (m_type=132 OR m_type=130 OR m_type=128))) WHERE _id = new.thread_id;UPDATE threads SET readcount = (SELECT count(_id) FROM (SELECT _id FROM sms WHERE (thread_id = new.thread_id AND (read = 1) AND (type != 3)) UNION SELECT _id FROM pdu WHERE (thread_id = new.thread_id AND (m_type=132 OR m_type=130 OR m_type=128) AND read = 1 AND msg_box != 3))) WHERE _id = new.thread_id;UPDATE threads SET snippet = (SELECT snippet FROM (SELECT max(date), snippet FROM (SELECT max(date) * 1000 as date, sub as snippet FROM pdu WHERE (thread_id = new.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT max(date) as date, body as snippet FROM sms WHERE (thread_id = new.thread_id)) WHERE snippet is not null)) WHERE _id = new.thread_id;UPDATE threads SET snippet_cs = (SELECT snippet_cs FROM (SELECT max(date), snippet_cs FROM (SELECT max(date) * 1000 as date, sub_cs as snippet_cs FROM pdu WHERE (thread_id = new.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT max(date), 0 as snippet_cs FROM sms WHERE (thread_id = new.thread_id)))) WHERE _id = new.thread_id;UPDATE threads SET error = (SELECT count(_id) FROM (SELECT _id FROM sms WHERE thread_id = new.thread_id and type = 5 UNION SELECT _id FROM pdu WHERE thread_id = new.thread_id AND st = 135 AND (m_type=132 OR m_type=130 OR m_type=128))) WHERE _id = new.thread_id;END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_on_split AFTER  UPDATE OF thread_id ON sms BEGIN UPDATE threads SET date = (SELECT max(date) as date FROM (SELECT date * 1000 as date FROM pdu WHERE (thread_id = old.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT date FROM sms WHERE (thread_id = old.thread_id))) WHERE _id = old.thread_id;UPDATE threads SET date_sent = (SELECT max(date_sent) as date_sent FROM (SELECT date_sent * 1000 as date_sent FROM pdu WHERE (thread_id = old.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT date_sent FROM sms WHERE (thread_id = old.thread_id))) WHERE _id = old.thread_id;UPDATE threads SET message_count = (SELECT count(_id) FROM (SELECT _id FROM sms WHERE thread_id = old.thread_id UNION SELECT _id FROM pdu WHERE thread_id = old.thread_id AND (m_type=132 OR m_type=130 OR m_type=128))) WHERE _id = old.thread_id;UPDATE threads SET readcount = (SELECT count(_id) FROM (SELECT _id FROM sms WHERE (thread_id = old.thread_id AND (read = 1) AND (type != 3)) UNION SELECT _id FROM pdu WHERE (thread_id = old.thread_id AND (m_type=132 OR m_type=130 OR m_type=128) AND read = 1 AND msg_box != 3))) WHERE _id = old.thread_id;UPDATE threads SET snippet = (SELECT snippet FROM (SELECT max(date), snippet FROM (SELECT max(date) * 1000 as date, sub as snippet FROM pdu WHERE (thread_id = old.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT max(date), body as snippet FROM sms WHERE (thread_id = old.thread_id)) WHERE snippet is not null)) WHERE _id = old.thread_id;UPDATE threads SET snippet_cs = (SELECT snippet_cs FROM (SELECT max(date), snippet_cs FROM (SELECT max(date) * 1000 as date, sub_cs as snippet_cs FROM pdu WHERE (thread_id = old.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT max(date), 0 as snippet_cs FROM sms WHERE (thread_id = old.thread_id)))) WHERE _id = old.thread_id;UPDATE threads SET error = (SELECT count(_id) FROM (SELECT _id FROM sms WHERE thread_id = old.thread_id and type = 5 UNION SELECT pdu._id FROM pdu WHERE thread_id = old.thread_id AND st = 135 AND (m_type=132 OR m_type=130 OR m_type=128))) WHERE _id = old.thread_id;END;");
        if (MTK_WAPPUSH_SUPPORT) {
            sQLiteDatabase.execSQL("CREATE TRIGGER wappush_update_thread_on_update AFTER  UPDATE OF read  ON wappush BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM wappush          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
        }
        if (MTK_WAPPUSH_SUPPORT) {
            sQLiteDatabase.execSQL("CREATE TRIGGER delete_obsolete_threads_when_update_pdu AFTER UPDATE OF thread_id ON pdu WHEN old.thread_id != new.thread_id BEGIN   DELETE FROM threads   WHERE     _id = old.thread_id     AND _id NOT IN     (SELECT thread_id FROM sms     UNION SELECT thread_id FROM wappush      UNION SELECT thread_id from pdu      UNION SELECT thread_id from cellbroadcast); END;");
        } else {
            sQLiteDatabase.execSQL("CREATE TRIGGER delete_obsolete_threads_when_update_pdu AFTER UPDATE OF thread_id ON pdu WHEN old.thread_id != new.thread_id BEGIN   DELETE FROM threads   WHERE     _id = old.thread_id     AND _id NOT IN     (SELECT thread_id FROM sms      UNION SELECT thread_id from pdu      UNION SELECT thread_id from cellbroadcast); END;");
        }
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_error_on_update_mms   AFTER UPDATE OF err_type ON pending_msgs   WHEN (OLD.err_type < 10 AND NEW.err_type >= 10 AND NEW.proto_type = 1 AND NEW.msg_type = 128)    OR (OLD.err_type >= 10 AND NEW.err_type < 10) BEGIN  UPDATE threads SET error =     CASE      WHEN NEW.err_type >= 10 THEN error + 1      ELSE error - 1    END   WHERE _id =   (SELECT DISTINCT thread_id    FROM pdu    WHERE _id = NEW.msg_id); END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_error_on_update_sms   AFTER UPDATE OF type ON sms  WHEN (OLD.type != 5 AND NEW.type = 5)    OR (OLD.type = 5 AND NEW.type != 5) BEGIN   UPDATE threads SET error =     CASE      WHEN NEW.type = 5 THEN error + 1      ELSE error - 1    END   WHERE _id = NEW.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_lirelated_on_sms_locked   AFTER UPDATE OF locked ON sms  WHEN (OLD.locked != 1 AND NEW.locked = 1)    AND (NEW.date > (SELECT li_date FROM threads WHERE _id= NEW.thread_id)) BEGIN   UPDATE threads SET    li_date = NEW.date,     li_snippet = NEW.body  WHERE _id = NEW.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_lirelated_on_sms_unlock   AFTER UPDATE OF locked ON sms  WHEN (OLD.locked = 1 AND NEW.locked != 1)    AND (NEW.date = (SELECT li_date FROM threads WHERE _id= NEW.thread_id)) BEGIN   UPDATE threads SET li_date =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT date FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN ''    ELSE       (SELECT snippet FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet_cs =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT snippet_cs FROM        (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, 0 AS snippet_cs, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_lirelated_on_sms_delete   AFTER DELETE ON sms  WHEN OLD.locked = 1     AND OLD.date = (SELECT li_date FROM threads WHERE _id= OLD.thread_id) BEGIN   UPDATE threads SET li_date =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT date FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN ''    ELSE       (SELECT snippet FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet_cs =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT snippet_cs FROM        (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, 0 AS snippet_cs, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_lirelated_on_pdu_locked   AFTER UPDATE OF locked ON pdu  WHEN (OLD.locked != 1 AND NEW.locked = 1)    AND (NEW.date > (SELECT li_date FROM threads WHERE _id= NEW.thread_id)) BEGIN   UPDATE threads SET    li_date = NEW.date,     li_snippet = NEW.sub,     li_snippet_cs = NEW.sub_cs  WHERE _id = NEW.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_lirelated_on_pdu_unlock   AFTER UPDATE OF locked ON pdu  WHEN (OLD.locked = 1 AND NEW.locked != 1)    AND (NEW.date = (SELECT li_date FROM threads WHERE _id= NEW.thread_id)) BEGIN   UPDATE threads SET li_date =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT date FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN ''    ELSE       (SELECT snippet FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet_cs =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT snippet_cs FROM        (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, 0 AS snippet_cs, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_lirelated_on_pdu_delete   AFTER DELETE ON pdu  WHEN OLD.locked = 1     AND OLD.date = (SELECT li_date FROM threads WHERE _id= OLD.thread_id) BEGIN   UPDATE threads SET li_date =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT date FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN ''    ELSE       (SELECT snippet FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet_cs =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT snippet_cs FROM        (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, 0 AS snippet_cs, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER cb_update_thread_on_insert AFTER INSERT ON cellbroadcast BEGIN  UPDATE threads SET    date = (strftime('%s','now') * 1000),     type= 3,     snippet = new.body   WHERE threads._id = new.thread_id; UPDATE threads SET message_count =     (SELECT COUNT(cellbroadcast._id) FROM cellbroadcast LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id )  WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM cellbroadcast          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER cb_update_thread_read_on_update AFTER  UPDATE OF read  ON cellbroadcast BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM cellbroadcast          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER cb_update_thread_on_delete AFTER DELETE ON cellbroadcast BEGIN   UPDATE threads SET      date = (strftime('%s','now') * 1000)  WHERE threads._id = old.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(cellbroadcast._id) FROM cellbroadcast LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = old.thread_id)  WHERE threads._id = old.thread_id;   UPDATE threads SET snippet =    (SELECT body FROM     (SELECT date, body, thread_id FROM cellbroadcast)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id;   UPDATE threads SET date =    (SELECT date FROM     (SELECT date, body, thread_id FROM cellbroadcast)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM cellbroadcast          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = old.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS insert_thread_settings_when_insert_threads");
        sQLiteDatabase.execSQL("CREATE TRIGGER insert_thread_settings_when_insert_threads AFTER INSERT ON threads BEGIN   INSERT INTO thread_settings     (thread_id    ,spam    ,notification_enable    ,mute    ,mute_start    ,ringtone    ,_data    ,vibrate    , top)   VALUES     (new._id,     0,1,0,0,'','',1,0); END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS delete_thread_settings_when_delete_threads");
        sQLiteDatabase.execSQL("CREATE TRIGGER delete_thread_settings_when_delete_threads AFTER DELETE ON threads BEGIN   DELETE FROM thread_settings     WHERE thread_id=old._id; END;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        Log.w("Mms/Provider/MmsSmsDatabaseHelper", "Upgrading database from version " + i + " to " + i2 + ".");
        switch (i) {
            case 40:
                if (i2 <= 40) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion41(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th.getMessage(), th);
                } finally {
                }
                break;
            case 41:
                if (i2 <= 41) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion42(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th2) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th2.getMessage(), th2);
                } finally {
                }
                break;
            case 42:
                if (i2 <= 42) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion43(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th3) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th3.getMessage(), th3);
                } finally {
                }
                break;
            case 43:
                if (i2 <= 43) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion44(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th4) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th4.getMessage(), th4);
                } finally {
                }
                break;
            case 44:
                if (i2 <= 44) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion45(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th5) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th5.getMessage(), th5);
                } finally {
                }
                break;
            case 45:
                if (i2 <= 45) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion46(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th6) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th6.getMessage(), th6);
                } finally {
                }
                break;
            case 46:
                if (i2 <= 46) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion47(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th7) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th7.getMessage(), th7);
                } finally {
                }
                break;
            case 47:
                if (i2 <= 47) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion48(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th8) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th8.getMessage(), th8);
                } finally {
                }
                break;
            case 48:
                if (i2 <= 48) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    createWordsTables(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th9) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th9.getMessage(), th9);
                } finally {
                }
                break;
            case 49:
                if (i2 <= 49) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    createThreadIdIndex(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th10) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th10.getMessage(), th10);
                } finally {
                }
                break;
            case 50:
                if (i2 <= 50) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion51(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th11) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th11.getMessage(), th11);
                } finally {
                }
                break;
            case 51:
                if (i2 <= 51) {
                    return;
                }
            case 52:
                if (i2 <= 52) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion53(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th12) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th12.getMessage(), th12);
                } finally {
                }
                break;
            case 53:
                if (i2 <= 53) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion530100(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th13) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th13.getMessage(), th13);
                } finally {
                }
                if (i2 > 530100) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion530200(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th14) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th14.getMessage(), th14);
                } finally {
                }
                if (i2 > 530200) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion530300(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th15) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th15.getMessage(), th15);
                } finally {
                }
                if (i2 > 530300) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion540000(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th16) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th16.getMessage(), th16);
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", "Destroying all old data.");
                    dropAll(sQLiteDatabase);
                    onCreate(sQLiteDatabase);
                    return;
                } finally {
                }
                if (i2 > 540000) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion550000(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th17) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th17.getMessage(), th17);
                } finally {
                }
                if (i2 > 550000) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion550100(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th18) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th18.getMessage(), th18);
                } finally {
                }
                if (i2 > 550100) {
                    return;
                }
                sQLiteDatabase.beginTransaction();
                try {
                    upgradeDatabaseToVersion560000(sQLiteDatabase);
                    sQLiteDatabase.setTransactionSuccessful();
                } catch (Throwable th19) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th19.getMessage(), th19);
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", "Destroying all old data.");
                    dropAll(sQLiteDatabase);
                    onCreate(sQLiteDatabase);
                    return;
                } finally {
                }
                if (i2 > 560000) {
                    return;
                }
                if (MTK_ONLY_OWNER_SIM_SUPPORT) {
                    sQLiteDatabase.beginTransaction();
                    try {
                        upgradeDatabaseToVersion560100(sQLiteDatabase);
                        sQLiteDatabase.setTransactionSuccessful();
                        if (i2 > 560100) {
                            return;
                        }
                        sQLiteDatabase.beginTransaction();
                        try {
                            upgradeDatabaseToVersion560200(sQLiteDatabase);
                            sQLiteDatabase.setTransactionSuccessful();
                            if (i2 > 560200) {
                                return;
                            }
                            sQLiteDatabase.beginTransaction();
                            try {
                                upgradeDatabaseToVersion560300(sQLiteDatabase);
                                sQLiteDatabase.setTransactionSuccessful();
                                if (i2 > 560300) {
                                    return;
                                }
                                if (MTK_ONLY_OWNER_SIM_SUPPORT) {
                                    sQLiteDatabase.beginTransaction();
                                    try {
                                        upgradeDatabaseToVersion560400(sQLiteDatabase);
                                        sQLiteDatabase.setTransactionSuccessful();
                                        if (i2 > 560400) {
                                            return;
                                        }
                                        sQLiteDatabase.beginTransaction();
                                        try {
                                            upgradeDatabaseToVersion560500(sQLiteDatabase);
                                            sQLiteDatabase.setTransactionSuccessful();
                                            if (i2 > 560500) {
                                                return;
                                            }
                                            sQLiteDatabase.beginTransaction();
                                            try {
                                                upgradeDatabaseToVersion600000(sQLiteDatabase);
                                                sQLiteDatabase.setTransactionSuccessful();
                                                if (i2 > 600000) {
                                                    return;
                                                }
                                                sQLiteDatabase.beginTransaction();
                                                try {
                                                    upgradeDatabaseToVersion600100(sQLiteDatabase);
                                                    sQLiteDatabase.setTransactionSuccessful();
                                                    if (i2 > 600100) {
                                                        return;
                                                    }
                                                    sQLiteDatabase.beginTransaction();
                                                    try {
                                                        upgradeDatabaseToVersion600200(sQLiteDatabase);
                                                        sQLiteDatabase.setTransactionSuccessful();
                                                        if (i2 > 600200) {
                                                            return;
                                                        }
                                                        sQLiteDatabase.beginTransaction();
                                                        try {
                                                            upgradeDatabaseToVersion600300(sQLiteDatabase);
                                                            sQLiteDatabase.setTransactionSuccessful();
                                                            if (i2 > 600300) {
                                                                return;
                                                            }
                                                            sQLiteDatabase.beginTransaction();
                                                            try {
                                                                upgradeDatabaseToVersion600400(sQLiteDatabase);
                                                                sQLiteDatabase.setTransactionSuccessful();
                                                                if (i2 > 600400) {
                                                                    return;
                                                                }
                                                                sQLiteDatabase.beginTransaction();
                                                                try {
                                                                    upgradeDatabaseToVersion601000(sQLiteDatabase);
                                                                    sQLiteDatabase.setTransactionSuccessful();
                                                                    if (i2 > 601000) {
                                                                        return;
                                                                    }
                                                                    sQLiteDatabase.beginTransaction();
                                                                    try {
                                                                        upgradeDatabaseToVersion601010(sQLiteDatabase);
                                                                        sQLiteDatabase.setTransactionSuccessful();
                                                                        if (i2 > 601010) {
                                                                            return;
                                                                        }
                                                                        sQLiteDatabase.beginTransaction();
                                                                        try {
                                                                            upgradeDatabaseToVersion601011(sQLiteDatabase);
                                                                            sQLiteDatabase.setTransactionSuccessful();
                                                                            if (i2 > 601011) {
                                                                                return;
                                                                            }
                                                                            sQLiteDatabase.beginTransaction();
                                                                            try {
                                                                                upgradeDatabaseToVersion601021(sQLiteDatabase);
                                                                                sQLiteDatabase.setTransactionSuccessful();
                                                                                if (i2 > 601021) {
                                                                                    return;
                                                                                }
                                                                                sQLiteDatabase.beginTransaction();
                                                                                try {
                                                                                    upgradeDatabaseToVersion601022(sQLiteDatabase);
                                                                                    sQLiteDatabase.setTransactionSuccessful();
                                                                                    if (i2 > 601024) {
                                                                                        return;
                                                                                    }
                                                                                    sQLiteDatabase.beginTransaction();
                                                                                    try {
                                                                                        upgradeDatabaseToVersion621000(sQLiteDatabase);
                                                                                        upgradeDatabaseToVersion631000(sQLiteDatabase);
                                                                                        sQLiteDatabase.setTransactionSuccessful();
                                                                                        if (i2 > 631000) {
                                                                                            return;
                                                                                        }
                                                                                        sQLiteDatabase.beginTransaction();
                                                                                        try {
                                                                                            upgradeDatabaseToVersion641000(sQLiteDatabase);
                                                                                            sQLiteDatabase.setTransactionSuccessful();
                                                                                            if (i2 > 641000) {
                                                                                                return;
                                                                                            }
                                                                                            sQLiteDatabase.beginTransaction();
                                                                                            try {
                                                                                                upgradeDatabaseToVersion641001(i, sQLiteDatabase);
                                                                                                sQLiteDatabase.setTransactionSuccessful();
                                                                                                if (i2 > 641001) {
                                                                                                    return;
                                                                                                }
                                                                                                sQLiteDatabase.beginTransaction();
                                                                                                try {
                                                                                                    upgradeDatabaseToVersion641002(i, sQLiteDatabase);
                                                                                                    sQLiteDatabase.setTransactionSuccessful();
                                                                                                    if (i2 > 641002) {
                                                                                                        return;
                                                                                                    }
                                                                                                    sQLiteDatabase.beginTransaction();
                                                                                                    try {
                                                                                                        upgradeDatabaseToVersion661000(sQLiteDatabase);
                                                                                                        sQLiteDatabase.setTransactionSuccessful();
                                                                                                        if (i2 > 661000) {
                                                                                                            return;
                                                                                                        }
                                                                                                        sQLiteDatabase.beginTransaction();
                                                                                                        try {
                                                                                                            createPartMidIndex(sQLiteDatabase);
                                                                                                            createAddrMsgIdIndex(sQLiteDatabase);
                                                                                                            sQLiteDatabase.setTransactionSuccessful();
                                                                                                            if (i2 > 670000) {
                                                                                                                return;
                                                                                                            }
                                                                                                            sQLiteDatabase.beginTransaction();
                                                                                                            try {
                                                                                                                upgradeDatabaseToVersion670010(sQLiteDatabase);
                                                                                                                sQLiteDatabase.setTransactionSuccessful();
                                                                                                                return;
                                                                                                            } catch (Throwable th20) {
                                                                                                                Log.e("Mms/Provider/MmsSmsDatabaseHelper", th20.getMessage(), th20);
                                                                                                            } finally {
                                                                                                            }
                                                                                                        } catch (Throwable th21) {
                                                                                                            Log.e("Mms/Provider/MmsSmsDatabaseHelper", th21.getMessage(), th21);
                                                                                                        } finally {
                                                                                                        }
                                                                                                    } catch (Throwable th22) {
                                                                                                        Log.e("Mms/Provider/MmsSmsDatabaseHelper", th22.getMessage(), th22);
                                                                                                    } finally {
                                                                                                    }
                                                                                                } catch (Throwable th23) {
                                                                                                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th23.getMessage(), th23);
                                                                                                } finally {
                                                                                                }
                                                                                            } catch (Throwable th24) {
                                                                                                Log.e("Mms/Provider/MmsSmsDatabaseHelper", th24.getMessage(), th24);
                                                                                            } finally {
                                                                                            }
                                                                                        } catch (Throwable th25) {
                                                                                            Log.e("Mms/Provider/MmsSmsDatabaseHelper", th25.getMessage(), th25);
                                                                                            Log.e("Mms/Provider/MmsSmsDatabaseHelper", "Destroying all old data.");
                                                                                            dropAll(sQLiteDatabase);
                                                                                            onCreate(sQLiteDatabase);
                                                                                            return;
                                                                                        } finally {
                                                                                        }
                                                                                    } catch (Throwable th26) {
                                                                                        Log.e("Mms/Provider/MmsSmsDatabaseHelper", th26.getMessage(), th26);
                                                                                    } finally {
                                                                                    }
                                                                                } catch (Throwable th27) {
                                                                                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th27.getMessage(), th27);
                                                                                } finally {
                                                                                }
                                                                            } catch (Throwable th28) {
                                                                                Log.e("Mms/Provider/MmsSmsDatabaseHelper", th28.getMessage(), th28);
                                                                            } finally {
                                                                            }
                                                                        } catch (Throwable th29) {
                                                                            Log.e("Mms/Provider/MmsSmsDatabaseHelper", th29.getMessage(), th29);
                                                                        } finally {
                                                                        }
                                                                    } catch (Throwable th30) {
                                                                        Log.e("Mms/Provider/MmsSmsDatabaseHelper", th30.getMessage(), th30);
                                                                    } finally {
                                                                    }
                                                                } catch (Throwable th31) {
                                                                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th31.getMessage(), th31);
                                                                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", "Destroying all old data.");
                                                                    dropAll(sQLiteDatabase);
                                                                    onCreate(sQLiteDatabase);
                                                                    return;
                                                                } finally {
                                                                }
                                                            } catch (Throwable th32) {
                                                                Log.e("Mms/Provider/MmsSmsDatabaseHelper", th32.getMessage(), th32);
                                                            } finally {
                                                            }
                                                        } catch (Throwable th33) {
                                                            Log.e("Mms/Provider/MmsSmsDatabaseHelper", th33.getMessage(), th33);
                                                        } finally {
                                                        }
                                                    } catch (Throwable th34) {
                                                        Log.e("Mms/Provider/MmsSmsDatabaseHelper", th34.getMessage(), th34);
                                                        Log.e("Mms/Provider/MmsSmsDatabaseHelper", "Destroying all old data.");
                                                        dropAll(sQLiteDatabase);
                                                        onCreate(sQLiteDatabase);
                                                        return;
                                                    } finally {
                                                    }
                                                } catch (Throwable th35) {
                                                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", th35.getMessage(), th35);
                                                } finally {
                                                }
                                            } catch (Throwable th36) {
                                                Log.e("Mms/Provider/MmsSmsDatabaseHelper", th36.getMessage(), th36);
                                            } finally {
                                            }
                                        } catch (Throwable th37) {
                                            Log.e("Mms/Provider/MmsSmsDatabaseHelper", th37.getMessage(), th37);
                                            Log.e("Mms/Provider/MmsSmsDatabaseHelper", "Destroying all old data.");
                                            dropAll(sQLiteDatabase);
                                            onCreate(sQLiteDatabase);
                                            return;
                                        } finally {
                                        }
                                    } catch (Throwable th38) {
                                        Log.e("Mms/Provider/MmsSmsDatabaseHelper", th38.getMessage(), th38);
                                    } finally {
                                    }
                                } else if (i2 > 560400) {
                                }
                            } catch (Throwable th39) {
                                Log.e("Mms/Provider/MmsSmsDatabaseHelper", th39.getMessage(), th39);
                            } finally {
                            }
                        } catch (Throwable th40) {
                            Log.e("Mms/Provider/MmsSmsDatabaseHelper", th40.getMessage(), th40);
                        } finally {
                        }
                    } catch (Throwable th41) {
                        Log.e("Mms/Provider/MmsSmsDatabaseHelper", th41.getMessage(), th41);
                    } finally {
                    }
                } else if (i2 > 560100) {
                }
                Log.e("Mms/Provider/MmsSmsDatabaseHelper", "Destroying all old data.");
                dropAll(sQLiteDatabase);
                onCreate(sQLiteDatabase);
                return;
            default:
                switch (i) {
                    case 601010:
                        if (i2 > 601010) {
                        }
                    case 601011:
                        if (i2 > 601011) {
                        }
                        break;
                    default:
                        switch (i) {
                            case 601020:
                            case 601021:
                                if (i2 > 601021) {
                                }
                            case 601022:
                            case 601023:
                            case 601024:
                                if (i2 > 601024) {
                                }
                                break;
                            default:
                                switch (i) {
                                    case 641000:
                                        if (i2 > 641000) {
                                        }
                                    case 641001:
                                        if (i2 > 641001) {
                                        }
                                    case 641002:
                                        if (i2 > 641002) {
                                        }
                                        break;
                                    default:
                                        switch (i) {
                                            case 530100:
                                                if (i2 > 530100) {
                                                }
                                                break;
                                            case 530200:
                                                if (i2 > 530200) {
                                                }
                                                break;
                                            case 530300:
                                                if (i2 > 530300) {
                                                }
                                                break;
                                            case 540000:
                                                if (i2 > 540000) {
                                                }
                                                break;
                                            case 550000:
                                                if (i2 > 550000) {
                                                }
                                                break;
                                            case 550100:
                                                if (i2 > 550100) {
                                                }
                                                break;
                                            case 560000:
                                                if (i2 > 560000) {
                                                }
                                                break;
                                            case 560100:
                                                break;
                                            case 560200:
                                                if (i2 > 560200) {
                                                }
                                                break;
                                            case 560300:
                                                if (i2 > 560300) {
                                                }
                                                break;
                                            case 560400:
                                                break;
                                            case 560500:
                                                if (i2 > 560500) {
                                                }
                                                break;
                                            case 600000:
                                                if (i2 > 600000) {
                                                }
                                                break;
                                            case 600100:
                                                if (i2 > 600100) {
                                                }
                                                break;
                                            case 600200:
                                                if (i2 > 600200) {
                                                }
                                                break;
                                            case 600300:
                                                if (i2 > 600300) {
                                                }
                                                break;
                                            case 600400:
                                                if (i2 > 600400) {
                                                }
                                                break;
                                            case 601000:
                                                if (i2 > 601000) {
                                                }
                                                break;
                                            case 631000:
                                                if (i2 > 631000) {
                                                }
                                                break;
                                            case 661000:
                                                if (i2 > 661000) {
                                                }
                                                break;
                                            case 670000:
                                                if (i2 > 670000) {
                                                }
                                                break;
                                            default:
                                                Log.e("Mms/Provider/MmsSmsDatabaseHelper", "Destroying all old data.");
                                                dropAll(sQLiteDatabase);
                                                onCreate(sQLiteDatabase);
                                                return;
                                        }
                                        break;
                                }
                                break;
                        }
                        break;
                }
                break;
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        dropAll(sQLiteDatabase);
        onCreate(sQLiteDatabase);
    }

    private void dropAll(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS rcs_threads;");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS sys_ipmsg;");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS rcs_message;");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS rcs_conversations;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS pdu_restricted;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS sms_restricted;");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS canonical_addresses");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS threads");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS pending_msgs");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS sms");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS cellbroadcast");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS words");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS quicktext");
        if (MTK_WAPPUSH_SUPPORT) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS wappush");
        }
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS raw");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS attachments");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS thread_ids");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS sr_pending");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS thread_settings");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS pdu;");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS addr;");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS part;");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS rate;");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS drm;");
        if (MTK_ONLY_OWNER_SIM_SUPPORT) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS usersms");
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS usermms;");
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS usercb;");
        }
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS mwi");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS pdu_restricted;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS sms_restricted;");
    }

    private void upgradeDatabaseToVersion41(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_threads_error_on_move_mms");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_error_on_move_mms   BEFORE UPDATE OF msg_box ON pdu   WHEN (OLD.msg_box = 4 AND NEW.msg_box != 4)   AND (OLD._id IN (SELECT DISTINCT msg_id                   FROM pending_msgs                   WHERE err_type >= 10)) BEGIN   UPDATE threads SET error = error - 1  WHERE _id = OLD.thread_id; END;");
    }

    private void upgradeDatabaseToVersion42(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_on_delete");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS delete_obsolete_threads_sms");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_threads_error_on_delete_sms");
    }

    private void upgradeDatabaseToVersion43(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE threads ADD COLUMN has_attachment INTEGER DEFAULT 0");
        updateThreadsAttachmentColumn(sQLiteDatabase);
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_on_insert_part  AFTER INSERT ON part  WHEN new.ct != 'text/plain' AND new.ct != 'application/smil'  BEGIN   UPDATE threads SET has_attachment=1 WHERE _id IN    (SELECT pdu.thread_id FROM part JOIN pdu ON pdu._id=part.mid      WHERE part._id=new._id LIMIT 1);  END");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_on_delete_part  AFTER DELETE ON part  WHEN old.ct != 'text/plain' AND old.ct != 'application/smil'  BEGIN   UPDATE threads SET has_attachment =    CASE     (SELECT COUNT(*) FROM part JOIN pdu      ON pdu._id=old.mid AND part.mid=pdu._id      WHERE part.ct != 'text/plain' AND part.ct != 'application/smil')    WHEN 0 THEN 0    ELSE 1    END    WHERE threads._id=(SELECT thread_id FROM pdu WHERE _id=old.mid);  END");
    }

    private void upgradeDatabaseToVersion44(SQLiteDatabase sQLiteDatabase) {
        updateThreadsAttachmentColumn(sQLiteDatabase);
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_on_update_part  AFTER UPDATE of mid ON part  WHEN new.ct != 'text/plain' AND new.ct != 'application/smil'  BEGIN   UPDATE threads SET has_attachment=1 WHERE _id IN    (SELECT pdu.thread_id FROM part JOIN pdu ON pdu._id=part.mid      WHERE part._id=new._id LIMIT 1);  END");
    }

    private void upgradeDatabaseToVersion45(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE sms ADD COLUMN locked INTEGER DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE pdu ADD COLUMN locked INTEGER DEFAULT 0");
    }

    private void upgradeDatabaseToVersion46(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE part ADD COLUMN text TEXT");
        Cursor cursorQuery = sQLiteDatabase.query("part", new String[]{"_id", "_data", "text"}, "ct = 'text/plain' OR ct == 'application/smil'", null, null, null, null);
        ArrayList<String> arrayList = new ArrayList();
        try {
            sQLiteDatabase.beginTransaction();
            if (cursorQuery != null) {
                int columnIndex = cursorQuery.getColumnIndex("_data");
                while (cursorQuery.moveToNext()) {
                    String string = cursorQuery.getString(columnIndex);
                    if (string != null) {
                        try {
                            FileInputStream fileInputStream = new FileInputStream(string);
                            byte[] bArr = new byte[fileInputStream.available()];
                            fileInputStream.read(bArr);
                            sQLiteDatabase.execSQL("UPDATE part SET _data = NULL, text = ?", new String[]{new EncodedStringValue(bArr).getString()});
                            fileInputStream.close();
                            arrayList.add(string);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            sQLiteDatabase.setTransactionSuccessful();
        } finally {
            sQLiteDatabase.endTransaction();
            for (String str : arrayList) {
                try {
                    new File(str).delete();
                } catch (SecurityException e2) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", "unable to clean up old mms file for " + str, e2);
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    private void upgradeDatabaseToVersion47(SQLiteDatabase sQLiteDatabase) {
        updateThreadsAttachmentColumn(sQLiteDatabase);
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_on_update_pdu  AFTER UPDATE of thread_id ON pdu  BEGIN   UPDATE threads SET has_attachment=1 WHERE _id IN    (SELECT pdu.thread_id FROM part JOIN pdu      WHERE part.ct != 'text/plain' AND part.ct != 'application/smil'      AND part.mid = pdu._id); END");
    }

    private void upgradeDatabaseToVersion48(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE sms ADD COLUMN error_code INTEGER DEFAULT 0");
    }

    private void upgradeDatabaseToVersion51(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE sms add COLUMN seen INTEGER DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE pdu add COLUMN seen INTEGER DEFAULT 0");
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("seen", (Integer) 1);
            Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeDatabaseToVersion51: updated " + sQLiteDatabase.update("sms", contentValues, "read=1", null) + " rows in sms table to have READ=1");
            Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeDatabaseToVersion51: updated " + sQLiteDatabase.update("pdu", contentValues, "read=1", null) + " rows in pdu table to have READ=1");
        } catch (Exception e) {
            Log.e("Mms/Provider/MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeDatabaseToVersion51 caught ", e);
        }
    }

    private void upgradeDatabaseToVersion53(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_read_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_read_on_update AFTER  UPDATE OF read  ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128 BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
    }

    private void upgradeDatabaseToVersion530100(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE pdu ADD COLUMN service_center TEXT");
    }

    private void upgradeDatabaseToVersion530200(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE threads ADD COLUMN readcount INTEGER");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_on_insert");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_on_insert AFTER INSERT ON sms BEGIN UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE ((SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id) +          (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_date_subject_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_date_subject_on_update AFTER  UPDATE OF date, body, type  ON sms BEGIN UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE ((SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id) +          (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_read_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_read_on_update AFTER  UPDATE OF read  ON sms BEGIN   UPDATE threads SET read =     CASE ((SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id) +          (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_on_insert");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_on_insert AFTER INSERT ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128  BEGIN  UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id;  END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_date_subject_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_date_subject_on_update AFTER  UPDATE OF date, sub, msg_box  ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128  BEGIN  UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id;  END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_read_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_read_on_update AFTER  UPDATE OF read  ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128 BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS cb_update_thread_on_insert");
        sQLiteDatabase.execSQL("CREATE TRIGGER cb_update_thread_on_insert AFTER INSERT ON cellbroadcast BEGIN  UPDATE threads SET    date = (strftime('%s','now') * 1000),     type= 3,     snippet = new.body   WHERE threads._id = new.thread_id; UPDATE threads SET message_count =     (SELECT COUNT(cellbroadcast._id) FROM cellbroadcast LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id )  WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM cellbroadcast          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS cb_update_thread_read_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER cb_update_thread_read_on_update AFTER  UPDATE OF read  ON cellbroadcast BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM cellbroadcast          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id; END;");
    }

    private void upgradeDatabaseToVersion530300(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE threads ADD COLUMN status INTEGER DEFAULT 0");
    }

    private void upgradeDatabaseToVersion540000(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE sms ADD COLUMN date_sent INTEGER DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE pdu ADD COLUMN date_sent INTEGER DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE cellbroadcast ADD COLUMN date_sent INTEGER DEFAULT 0");
    }

    private void upgradeDatabaseToVersion550000(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS delete_obsolete_threads_pdu");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS delete_obsolete_threads_when_update_pdu");
    }

    private void upgradeDatabaseToVersion550100(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE sms ADD COLUMN ipmsg_id INTEGER DEFAULT 0");
        sQLiteDatabase.execSQL("CREATE TABLE thread_settings(_id INTEGER PRIMARY KEY,thread_id INTEGER,spam INTEGER DEFAULT 0,notification_enable INTEGER DEFAULT 1,mute INTEGER DEFAULT 0,mute_start INTEGER DEFAULT 0,ringtone TEXT,_data TEXT,vibrate INTEGER DEFAULT 1);");
        sQLiteDatabase.execSQL("CREATE TRIGGER insert_thread_settings_when_insert_threads AFTER INSERT ON threads BEGIN   INSERT INTO thread_settings     (thread_id    ,spam    ,notification_enable    ,mute    ,mute_start    ,ringtone    ,_data    ,vibrate)   VALUES     (new._id,     0,1,0,0,'','',1); END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER delete_thread_settings_when_delete_threads AFTER DELETE ON threads BEGIN   DELETE FROM thread_settings     WHERE thread_id=old._id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_on_insert");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_on_insert AFTER INSERT ON sms BEGIN UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE ((SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id) +          (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_date_subject_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_date_subject_on_update AFTER  UPDATE OF date, body, type  ON sms BEGIN UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE ((SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id) +          (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_read_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_read_on_update AFTER  UPDATE OF read  ON sms BEGIN   UPDATE threads SET read =     CASE ((SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id) +          (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_on_insert");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_on_insert AFTER INSERT ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128   BEGIN  UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (new.m_type=132 OR new.m_type=130 OR new.m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id; UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id;   UPDATE threads SET status = 0   WHERE (new.m_type=132 OR new.m_type=130 OR new.m_type=128) AND threads._id=new.thread_id;   END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_date_subject_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_date_subject_on_update AFTER  UPDATE OF date, sub, msg_box  ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128  BEGIN  UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id;  END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_read_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_read_on_update AFTER  UPDATE OF read  ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128 BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS cb_update_thread_on_insert");
        sQLiteDatabase.execSQL("CREATE TRIGGER cb_update_thread_on_insert AFTER INSERT ON cellbroadcast BEGIN  UPDATE threads SET    date = (strftime('%s','now') * 1000),     type= 3,     snippet = new.body   WHERE threads._id = new.thread_id; UPDATE threads SET message_count =     (SELECT COUNT(cellbroadcast._id) FROM cellbroadcast LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id )  WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM cellbroadcast          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS cb_update_thread_read_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER cb_update_thread_read_on_update AFTER  UPDATE OF read  ON cellbroadcast BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM cellbroadcast          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("ALTER TABLE threads ADD COLUMN li_date INTEGER DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE threads ADD COLUMN li_snippet TEXT");
        sQLiteDatabase.execSQL("ALTER TABLE threads ADD COLUMN li_snippet_cs INTEGER DEFAULT 0");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_lirelated_on_sms_locked   AFTER UPDATE OF locked ON sms  WHEN (OLD.locked != 1 AND NEW.locked = 1)    AND (NEW.date > (SELECT li_date FROM threads WHERE _id= NEW.thread_id)) BEGIN   UPDATE threads SET    li_date = NEW.date,     li_snippet = NEW.body  WHERE _id = NEW.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_lirelated_on_sms_unlock   AFTER UPDATE OF locked ON sms  WHEN (OLD.locked = 1 AND NEW.locked != 1)    AND (NEW.date = (SELECT li_date FROM threads WHERE _id= NEW.thread_id)) BEGIN   UPDATE threads SET li_date =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT date FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN ''    ELSE       (SELECT snippet FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet_cs =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT snippet_cs FROM        (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, 0 AS snippet_cs, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_lirelated_on_sms_delete   AFTER DELETE ON sms  WHEN OLD.locked = 1     AND OLD.date = (SELECT li_date FROM threads WHERE _id= OLD.thread_id) BEGIN   UPDATE threads SET li_date =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT date FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN ''    ELSE       (SELECT snippet FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet_cs =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT snippet_cs FROM        (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, 0 AS snippet_cs, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_lirelated_on_pdu_locked   AFTER UPDATE OF locked ON pdu  WHEN (OLD.locked != 1 AND NEW.locked = 1)    AND (NEW.date > (SELECT li_date FROM threads WHERE _id= NEW.thread_id)) BEGIN   UPDATE threads SET    li_date = NEW.date,     li_snippet = NEW.sub,     li_snippet_cs = NEW.sub_cs  WHERE _id = NEW.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_lirelated_on_pdu_unlock   AFTER UPDATE OF locked ON pdu  WHEN (OLD.locked = 1 AND NEW.locked != 1)    AND (NEW.date = (SELECT li_date FROM threads WHERE _id= NEW.thread_id)) BEGIN   UPDATE threads SET li_date =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT date FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN ''    ELSE       (SELECT snippet FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet_cs =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT snippet_cs FROM        (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, 0 AS snippet_cs, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_lirelated_on_pdu_delete   AFTER DELETE ON pdu  WHEN OLD.locked = 1     AND OLD.date = (SELECT li_date FROM threads WHERE _id= OLD.thread_id) BEGIN   UPDATE threads SET li_date =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT date FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN ''    ELSE       (SELECT snippet FROM        (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id;   UPDATE threads SET li_snippet_cs =     CASE (SELECT COUNT(*) FROM       (SELECT date * 1000 AS date, sub AS snippet, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128       UNION SELECT date, body AS snippet, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    WHEN 0 THEN 0    ELSE       (SELECT snippet_cs FROM        (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id, locked FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128         UNION SELECT date, 0 AS snippet_cs, thread_id, locked FROM sms)      WHERE thread_id = OLD.thread_id AND locked=1 ORDER BY date DESC LIMIT 1)    END  WHERE _id = OLD.thread_id; END;");
        Log.v("Mms/Provider/MmsSmsDatabaseHelper", "old thread thread_setting update begin");
        Cursor cursorQuery = sQLiteDatabase.query("threads", new String[]{"_id"}, null, null, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() > 0) {
                    while (cursorQuery.moveToNext()) {
                        long j = cursorQuery.getLong(0);
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("thread_id", Long.valueOf(j));
                        sQLiteDatabase.insert("thread_settings", null, contentValues);
                    }
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        Log.v("Mms/Provider/MmsSmsDatabaseHelper", "old thread thread_setting update end");
    }

    private void upgradeDatabaseToVersion560000(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DELETE FROM pdu WHERE thread_id IS NULL");
        sQLiteDatabase.execSQL("ALTER TABLE pdu ADD COLUMN text_only INTEGER DEFAULT 0");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_on_delete");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_on_delete AFTER DELETE ON pdu BEGIN   UPDATE threads SET      date = (strftime('%s','now') * 1000)  WHERE threads._id = old.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = old.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = old.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = old.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id) FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = old.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = old.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = old.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = old.thread_id;   UPDATE threads SET snippet =    (SELECT snippet FROM     (SELECT date * 1000 AS date, sub AS snippet, thread_id FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128      UNION SELECT date, body AS snippet, thread_id FROM sms)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id;   UPDATE threads SET snippet_cs =    (SELECT snippet_cs FROM     (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128      UNION SELECT date, 0 AS snippet_cs, thread_id FROM sms)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id;   UPDATE threads SET date_sent =    (SELECT date_sent FROM     (SELECT date_sent * 1000 as date_sent, date * 1000 AS date, thread_id FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128      UNION SELECT date_sent, date, thread_id FROM sms)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_on_delete");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_on_delete AFTER DELETE ON sms BEGIN   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = old.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = old.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = old.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id) FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = old.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = old.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = old.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = old.thread_id; END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_read_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_read_on_update AFTER  UPDATE OF read  ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128 BEGIN   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
    }

    private void upgradeDatabaseToVersion560100(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE usersms (_id INTEGER PRIMARY KEY,pdus TEXT,format TEXT,simId INTEGER);");
        sQLiteDatabase.execSQL("CREATE TABLE usermms (_id INTEGER PRIMARY KEY,transactionId INTEGER,pduType INTEGER,header TEXT,data TEXT,address TEXT,service_center TEXT,simId INTEGER);");
        sQLiteDatabase.execSQL("CREATE TABLE usercb (_id INTEGER PRIMARY KEY,action TEXT,pdus TEXT,simId INTEGER);");
    }

    private void upgradeDatabaseToVersion560200(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE threads ADD COLUMN date_sent INTEGER DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE pdu ADD COLUMN st_ext INTEGER DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE sms ADD COLUMN ref_id INTEGER");
        sQLiteDatabase.execSQL("ALTER TABLE sms ADD COLUMN total_len INTEGER");
        sQLiteDatabase.execSQL("ALTER TABLE sms ADD COLUMN rec_len INTEGER");
        sQLiteDatabase.execSQL("ALTER TABLE raw ADD COLUMN recv_time INTEGER");
        sQLiteDatabase.execSQL("ALTER TABLE raw ADD COLUMN upload_flag INTEGER");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_on_insert");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_on_insert");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_date_subject_on_update");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_date_subject_on_update");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_on_delete");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_on_delete");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_on_add");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_on_split");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_on_insert AFTER INSERT ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128   BEGIN  UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (new.m_type=132 OR new.m_type=130 OR new.m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id; UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id;   UPDATE threads SET status = 0   WHERE (new.m_type=132 OR new.m_type=130 OR new.m_type=128) AND threads._id=new.thread_id;   END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_on_insert AFTER INSERT ON sms BEGIN UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE ((SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id) +          (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_date_subject_on_update AFTER  UPDATE OF date, sub, msg_box  ON pdu   WHEN new.m_type=132    OR new.m_type=130    OR new.m_type=128  BEGIN  UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)) +          (SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id)      WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id;  END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_date_subject_on_update AFTER  UPDATE OF date, body, type  ON sms BEGIN UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE ((SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id) +          (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER pdu_update_thread_on_delete AFTER DELETE ON pdu BEGIN   UPDATE threads SET      date = (strftime('%s','now') * 1000)  WHERE threads._id = old.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = old.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = old.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = old.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id) FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = old.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = old.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = old.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = old.thread_id;   UPDATE threads SET snippet =    (SELECT snippet FROM     (SELECT date * 1000 AS date, sub AS snippet, thread_id FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128      UNION SELECT date, body AS snippet, thread_id FROM sms)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id;   UPDATE threads SET snippet_cs =    (SELECT snippet_cs FROM     (SELECT date * 1000 AS date, sub_cs AS snippet_cs, thread_id FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128      UNION SELECT date, 0 AS snippet_cs, thread_id FROM sms)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id;   UPDATE threads SET date_sent =    (SELECT date_sent FROM     (SELECT date_sent * 1000 as date_sent, date * 1000 AS date, thread_id FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128      UNION SELECT date_sent, date, thread_id FROM sms)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_on_delete AFTER DELETE ON sms BEGIN   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = old.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = old.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = old.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id) FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = old.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = old.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = old.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = old.thread_id;   UPDATE threads SET date_sent =    (SELECT date_sent FROM     (SELECT date_sent * 1000 as date_sent, date * 1000 AS date, thread_id FROM pdu WHERE m_type=132 OR m_type=130 OR m_type=128      UNION SELECT date_sent, date, thread_id FROM sms)    WHERE thread_id = OLD.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = OLD.thread_id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_on_add AFTER  UPDATE OF thread_id ON sms BEGIN UPDATE threads SET date = (SELECT max(date) as date FROM (SELECT date * 1000 as date FROM pdu WHERE (thread_id = new.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT date FROM sms WHERE (thread_id = new.thread_id))) WHERE _id = new.thread_id;UPDATE threads SET date_sent = (SELECT max(date_sent) as date_sent FROM (SELECT date_sent * 1000 as date_sent FROM pdu WHERE (thread_id = new.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT date_sent FROM sms WHERE (thread_id = new.thread_id))) WHERE _id = new.thread_id;UPDATE threads SET message_count = (SELECT count(_id) FROM (SELECT _id FROM sms WHERE thread_id = new.thread_id UNION SELECT _id FROM pdu WHERE thread_id = new.thread_id AND (m_type=132 OR m_type=130 OR m_type=128))) WHERE _id = new.thread_id;UPDATE threads SET readcount = (SELECT count(_id) FROM (SELECT _id FROM sms WHERE (thread_id = new.thread_id AND (read = 1) AND (type != 3)) UNION SELECT _id FROM pdu WHERE (thread_id = new.thread_id AND (m_type=132 OR m_type=130 OR m_type=128) AND read = 1 AND msg_box != 3))) WHERE _id = new.thread_id;UPDATE threads SET snippet = (SELECT snippet FROM (SELECT max(date), snippet FROM (SELECT max(date) * 1000 as date, sub as snippet FROM pdu WHERE (thread_id = new.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT max(date) as date, body as snippet FROM sms WHERE (thread_id = new.thread_id)) WHERE snippet is not null)) WHERE _id = new.thread_id;UPDATE threads SET snippet_cs = (SELECT snippet_cs FROM (SELECT max(date), snippet_cs FROM (SELECT max(date) * 1000 as date, sub_cs as snippet_cs FROM pdu WHERE (thread_id = new.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT max(date), 0 as snippet_cs FROM sms WHERE (thread_id = new.thread_id)))) WHERE _id = new.thread_id;UPDATE threads SET error = (SELECT count(_id) FROM (SELECT _id FROM sms WHERE thread_id = new.thread_id and type = 5 UNION SELECT _id FROM pdu WHERE thread_id = new.thread_id AND st = 135 AND (m_type=132 OR m_type=130 OR m_type=128))) WHERE _id = new.thread_id;END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_on_split AFTER  UPDATE OF thread_id ON sms BEGIN UPDATE threads SET date = (SELECT max(date) as date FROM (SELECT date * 1000 as date FROM pdu WHERE (thread_id = old.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT date FROM sms WHERE (thread_id = old.thread_id))) WHERE _id = old.thread_id;UPDATE threads SET date_sent = (SELECT max(date_sent) as date_sent FROM (SELECT date_sent * 1000 as date_sent FROM pdu WHERE (thread_id = old.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT date_sent FROM sms WHERE (thread_id = old.thread_id))) WHERE _id = old.thread_id;UPDATE threads SET message_count = (SELECT count(_id) FROM (SELECT _id FROM sms WHERE thread_id = old.thread_id UNION SELECT _id FROM pdu WHERE thread_id = old.thread_id AND (m_type=132 OR m_type=130 OR m_type=128))) WHERE _id = old.thread_id;UPDATE threads SET readcount = (SELECT count(_id) FROM (SELECT _id FROM sms WHERE (thread_id = old.thread_id AND (read = 1) AND (type != 3)) UNION SELECT _id FROM pdu WHERE (thread_id = old.thread_id AND (m_type=132 OR m_type=130 OR m_type=128) AND read = 1 AND msg_box != 3))) WHERE _id = old.thread_id;UPDATE threads SET snippet = (SELECT snippet FROM (SELECT max(date), snippet FROM (SELECT max(date) * 1000 as date, sub as snippet FROM pdu WHERE (thread_id = old.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT max(date), body as snippet FROM sms WHERE (thread_id = old.thread_id)) WHERE snippet is not null)) WHERE _id = old.thread_id;UPDATE threads SET snippet_cs = (SELECT snippet_cs FROM (SELECT max(date), snippet_cs FROM (SELECT max(date) * 1000 as date, sub_cs as snippet_cs FROM pdu WHERE (thread_id = old.thread_id AND (m_type=132 OR m_type=130 OR m_type=128)) UNION SELECT max(date), 0 as snippet_cs FROM sms WHERE (thread_id = old.thread_id)))) WHERE _id = old.thread_id;UPDATE threads SET error = (SELECT count(_id) FROM (SELECT _id FROM sms WHERE thread_id = old.thread_id and type = 5 UNION SELECT pdu._id FROM pdu WHERE thread_id = old.thread_id AND st = 135 AND (m_type=132 OR m_type=130 OR m_type=128))) WHERE _id = old.thread_id;END;");
    }

    private void upgradeDatabaseToVersion560300(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS pdu_update_thread_on_delete");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_on_delete");
    }

    private void upgradeDatabaseToVersion560400(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE usermms ADD COLUMN mimeType TEXT");
    }

    private void upgradeDatabaseToVersion560500(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_date_subject_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_update_thread_date_subject_on_update AFTER  UPDATE OF date, body, type  ON sms BEGIN UPDATE threads  SET  date =    (SELECT date FROM        (SELECT date * 1000 AS date, thread_id FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  date_sent =    (SELECT date_sent FROM        (SELECT date_sent * 1000 AS date_sent, date * 1000 as date, thread_id FROM pdu         WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT date_sent, date, thread_id FROM sms          WHERE thread_id = new.thread_id)      WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1)   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet =    (SELECT snippet FROM        (SELECT sub AS snippet, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT body AS snippet, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;  UPDATE threads  SET  snippet_cs =    (SELECT snippet_cs FROM        (SELECT sub_cs AS snippet_cs, thread_id, date * 1000 AS date FROM pdu          WHERE (m_type=132 OR m_type=130 OR m_type=128) AND (thread_id = new.thread_id)          UNION SELECT 0 AS snippet_cs, thread_id, date FROM sms          WHERE thread_id = new.thread_id ORDER BY date DESC LIMIT 1 )      WHERE thread_id = new.thread_id )   WHERE threads._id = new.thread_id;   UPDATE threads SET message_count =      (SELECT COUNT(sms._id) FROM sms LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND sms.type != 3) +      (SELECT COUNT(pdu._id) FROM pdu LEFT JOIN threads       ON threads._id = thread_id      WHERE thread_id = new.thread_id        AND (m_type=132 OR m_type=130 OR m_type=128)        AND msg_box != 3)   WHERE threads._id = new.thread_id;   UPDATE threads SET read =     CASE ((SELECT COUNT(*)          FROM sms          WHERE read = 0            AND thread_id = threads._id) +          (SELECT COUNT(*)          FROM pdu          WHERE read = 0            AND thread_id = threads._id             AND (m_type=132 OR m_type=130 OR m_type=128)))       WHEN 0 THEN 1      ELSE 0    END  WHERE threads._id = new.thread_id;   UPDATE threads SET readcount =   (SELECT count(_id)FROM   (SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM sms     WHERE ((read=1) AND thread_id = new.thread_id AND (type != 3))   UNION SELECT DISTINCT date * 1000 AS normalized_date, pdu._id, read   FROM pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id   WHERE ((read=1) AND thread_id = new.thread_id AND msg_box != 3 AND (msg_box != 3         AND (m_type = 128 OR m_type = 132 OR m_type = 130)))   UNION SELECT DISTINCT date * 1 AS normalized_date, _id, read FROM cellbroadcast    WHERE ((read=1) AND thread_id = new.thread_id) ORDER BY normalized_date ASC))    WHERE threads._id = new.thread_id; END;");
    }

    private void upgradeDatabaseToVersion600000(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE pdu_temp (_id INTEGER PRIMARY KEY AUTOINCREMENT,thread_id INTEGER,date INTEGER,date_sent INTEGER DEFAULT 0,msg_box INTEGER,read INTEGER DEFAULT 0,m_id TEXT,sub TEXT,sub_cs INTEGER,ct_t TEXT,ct_l TEXT,exp INTEGER,m_cls TEXT,m_type INTEGER,v INTEGER,m_size INTEGER,pri INTEGER,rr INTEGER,rpt_a INTEGER,resp_st INTEGER,st INTEGER,st_ext INTEGER DEFAULT 0,tr_id TEXT,retr_st INTEGER,retr_txt TEXT,retr_txt_cs INTEGER,read_status INTEGER,ct_cls INTEGER,resp_txt TEXT,d_tm INTEGER,d_rpt INTEGER,locked INTEGER DEFAULT 0,sub_id INTEGER DEFAULT -1, service_center TEXT,seen INTEGER DEFAULT 0,text_only INTEGER DEFAULT 0);");
        sQLiteDatabase.execSQL("INSERT INTO pdu_temp SELECT _id, thread_id, date, date_sent, msg_box, read, m_id, sub, sub_cs, ct_t, ct_l, exp, m_cls, m_type, v, m_size, pri, rr, rpt_a, resp_st, st, st_ext, tr_id, retr_st, retr_txt, retr_txt_cs, read_status, ct_cls, resp_txt, d_tm, d_rpt, locked, sim_id, service_center, seen, text_only from pdu;");
        sQLiteDatabase.execSQL("DROP TABLE pdu;");
        sQLiteDatabase.execSQL("ALTER TABLE pdu_temp RENAME TO pdu;");
        sQLiteDatabase.execSQL("CREATE TABLE pending_msgs_temp (_id INTEGER PRIMARY KEY,proto_type INTEGER,msg_id INTEGER,msg_type INTEGER,err_type INTEGER,err_code INTEGER,retry_index INTEGER NOT NULL DEFAULT 0,due_time INTEGER,pending_sub_id INTEGER DEFAULT 0, last_try INTEGER);");
        sQLiteDatabase.execSQL("INSERT INTO pending_msgs_temp SELECT * from pending_msgs;");
        sQLiteDatabase.execSQL("DROP TABLE pending_msgs;");
        sQLiteDatabase.execSQL("ALTER TABLE pending_msgs_temp RENAME TO pending_msgs;");
        sQLiteDatabase.execSQL("CREATE TABLE sms_temp (_id INTEGER PRIMARY KEY,thread_id INTEGER,address TEXT,m_size INTEGER,person INTEGER,date INTEGER,date_sent INTEGER DEFAULT 0,protocol INTEGER,read INTEGER DEFAULT 0,status INTEGER DEFAULT -1,type INTEGER,reply_path_present INTEGER,subject TEXT,body TEXT,service_center TEXT,locked INTEGER DEFAULT 0,sub_id LONG DEFAULT -1, error_code INTEGER DEFAULT 0,seen INTEGER DEFAULT 0,ipmsg_id INTEGER DEFAULT 0, ref_id INTEGER,total_len INTEGER,rec_len INTEGER);");
        sQLiteDatabase.execSQL("INSERT INTO sms_temp SELECT * from sms;");
        sQLiteDatabase.execSQL("DROP TABLE sms;");
        sQLiteDatabase.execSQL("ALTER TABLE sms_temp RENAME TO sms;");
        sQLiteDatabase.execSQL("CREATE TABLE raw_temp (_id INTEGER PRIMARY KEY,date INTEGER,reference_number INTEGER,count INTEGER,sequence INTEGER,destination_port INTEGER,address TEXT,sub_id LONG DEFAULT -1, pdu TEXT,recv_time INTEGER,upload_flag INTEGER);");
        sQLiteDatabase.execSQL("INSERT INTO raw_temp SELECT * from raw;");
        sQLiteDatabase.execSQL("DROP TABLE raw;");
        sQLiteDatabase.execSQL("ALTER TABLE raw_temp RENAME TO raw;");
        if (MTK_WAPPUSH_SUPPORT) {
            sQLiteDatabase.execSQL("CREATE TABLE wappush_temp (_id INTEGER PRIMARY KEY,thread_id INTEGER,address TEXT NOT NULL,service_center TEXT NOT NULL,seen INTEGER DEFAULT 0,read INTEGER DEFAULT 0,locked INTEGER DEFAULT 0,error INTEGER DEFAULT 0,sub_id INTEGER DEFAULT -1, date INTEGER,type INTEGER DEFAULT 0,siid TEXT,url TEXT,action INTEGER,created INTEGER,expiration INTEGER,text TEXT);");
            sQLiteDatabase.execSQL("INSERT INTO wappush_temp SELECT * from wappush;");
            sQLiteDatabase.execSQL("DROP TABLE wappush;");
            sQLiteDatabase.execSQL("ALTER TABLE wappush_temp RENAME TO wappush;");
        } else {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS wappush");
        }
        sQLiteDatabase.execSQL("CREATE TABLE cellbroadcast_temp (_id INTEGER PRIMARY KEY,sub_id INTEGER DEFAULT -1,locked INTEGER DEFAULT 0,body TEXT,channel_id INTEGER,thread_id INTEGER,read INTEGER DEFAULT 0,seen INTEGER DEFAULT 0,date_sent INTEGER DEFAULT 0,date INTEGER);");
        sQLiteDatabase.execSQL("INSERT INTO cellbroadcast_temp SELECT * from cellbroadcast;");
        sQLiteDatabase.execSQL("DROP TABLE cellbroadcast;");
        sQLiteDatabase.execSQL("ALTER TABLE cellbroadcast_temp RENAME TO cellbroadcast;");
        sQLiteDatabase.execSQL("ALTER TABLE pdu ADD COLUMN creator TEXT");
        sQLiteDatabase.execSQL("ALTER TABLE sms ADD COLUMN creator TEXT");
        sQLiteDatabase.execSQL("ALTER TABLE threads ADD COLUMN archived INTEGER DEFAULT 0");
        createCommonTriggers(sQLiteDatabase);
        createMmsTriggers(sQLiteDatabase);
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_words_update AFTER UPDATE OF body ON sms BEGIN UPDATE words  SET index_text = NEW.body WHERE (source_id=NEW._id AND table_to_use=1);  END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER sms_words_delete AFTER DELETE ON sms BEGIN DELETE FROM   words WHERE source_id = OLD._id AND table_to_use = 1; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER wp_words_update AFTER UPDATE ON wappush BEGIN UPDATE words  SET index_text = coalesce(NEW.text||' '||NEW.url,NEW.text,NEW.url) WHERE (source_id=NEW._id AND table_to_use=3); END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER wp_words_delete AFTER DELETE ON wappush BEGIN DELETE FROM  words WHERE source_id = OLD._id AND table_to_use = 3; END;");
    }

    private void upgradeDatabaseToVersion600100(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS delete_mms_pending_on_update");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS insert_mms_pending_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER delete_mms_pending_on_update AFTER UPDATE ON pdu WHEN old.msg_box=4  AND new.msg_box!=4  AND new.msg_box!=5 BEGIN   DELETE FROM pending_msgs  WHERE msg_id=new._id; END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER insert_mms_pending_on_update AFTER UPDATE ON pdu WHEN new.m_type=128  AND new.msg_box=4  AND old.msg_box!=4  AND old.msg_box!=5 BEGIN   INSERT INTO pending_msgs    (proto_type,     msg_id,     msg_type,     err_type,     err_code,     retry_index,     due_time)   VALUES     (1,      new._id,      new.m_type,0,0,0,0);END;");
    }

    private void upgradeDatabaseToVersion600200(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS delete_mms_pending_on_update");
        sQLiteDatabase.execSQL("CREATE TRIGGER delete_mms_pending_on_update AFTER UPDATE ON pdu WHEN (old.msg_box=4   OR old.msg_box=5)  AND new.msg_box!=4  AND new.msg_box!=5 BEGIN   DELETE FROM pending_msgs  WHERE msg_id=new._id; END;");
    }

    private void upgradeDatabaseToVersion600300(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_threads_error_on_update_sms");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_error_on_update_sms   AFTER UPDATE OF type, status ON sms  WHEN (OLD.type != 5 AND NEW.type = 5)    OR (OLD.type = 5 AND NEW.type != 5)     OR (OLD.status >= 64 AND NEW.status < 64)     OR (OLD.status < 64 AND NEW.status >= 64) BEGIN   UPDATE threads SET error =     CASE      WHEN NEW.type = 5 or NEW.status >= 64 THEN error + 1      ELSE error - 1    END   WHERE _id = NEW.thread_id; END;");
    }

    private void upgradeDatabaseToVersion600400(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE thread_settings ADD COLUMN top INTEGER DEFAULT 0");
    }

    private void upgradeDatabaseToVersion601000(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE VIEW sms_restricted AS SELECT * FROM sms WHERE type=1 OR type=2;");
        sQLiteDatabase.execSQL("CREATE VIEW pdu_restricted  AS SELECT * FROM pdu WHERE (msg_box=1 OR msg_box=2) AND (m_type!=130);");
    }

    private void upgradeDatabaseToVersion601010(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS sys_ipmsg;");
        if (MTK_RCS_SUPPORT) {
            dropAll(sQLiteDatabase);
            onCreate(sQLiteDatabase);
        }
    }

    private void upgradeDatabaseToVersion601011(SQLiteDatabase sQLiteDatabase) {
        if (MTK_RCS_SUPPORT) {
            sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_rcs_thread_after_update_rcs_message_status;");
            sQLiteDatabase.execSQL("CREATE TRIGGER update_rcs_thread_after_update_rcs_message_status  AFTER UPDATE OF CHATMESSAGE_MESSAGE_STATUS ON rcs_message WHEN NEW.class<11 BEGIN     UPDATE rcs_conversations        SET read=(                   CASE                        (SELECT COUNT(_id)                        FROM rcs_message WHERE                        CHATMESSAGE_MESSAGE_STATUS = 0 AND                        CHATMESSAGE_CONVERSATION= NEW.CHATMESSAGE_CONVERSATION )                   WHEN 0 THEN 0 ELSE 1  END),             CHATMESSAGE_UNREAD_COUNT=(                   SELECT COUNT(_id)                    FROM rcs_message WHERE                    CHATMESSAGE_MESSAGE_STATUS=0 AND                    CHATMESSAGE_CONVERSATION= NEW.CHATMESSAGE_CONVERSATION),             error=(                   CASE                        (SELECT COUNT(_id)                        FROM rcs_message WHERE                        CHATMESSAGE_MESSAGE_STATUS = 5 AND                        CHATMESSAGE_CONVERSATION= NEW.CHATMESSAGE_CONVERSATION )                   WHEN 0 THEN 0 ELSE 1  END)     WHERE rcs_conversations.CHATMESSAGE_CONVERSATION=NEW.CHATMESSAGE_CONVERSATION;  END;");
        }
    }

    private void upgradeDatabaseToVersion601021(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_threads_error_on_move_mms");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_error_on_move_mms   BEFORE UPDATE OF msg_box ON pdu   WHEN ((OLD.msg_box = 4 OR OLD.msg_box = 5)   AND NEW.msg_box != 4 AND NEW.msg_box != 5)   AND (OLD._id IN (SELECT DISTINCT msg_id                   FROM pending_msgs                   WHERE err_type >= 10)) BEGIN   UPDATE threads SET error = error - 1  WHERE _id = OLD.thread_id; END;");
    }

    private void upgradeDatabaseToVersion601022(SQLiteDatabase sQLiteDatabase) {
        if (MTK_RCS_SUPPORT) {
            sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_rcs_thread_after_insert_rcs_message;");
            sQLiteDatabase.execSQL("CREATE TRIGGER update_rcs_thread_after_insert_rcs_message  AFTER INSERT ON rcs_message BEGIN   UPDATE rcs_conversations     SET       CHATMESSAGE_BODY=        (CASE WHEN rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN NEW.CHATMESSAGE_BODY        ELSE rcs_conversations.CHATMESSAGE_BODY END),       CHATMESSAGE_TIMESTAMP=        (CASE WHEN rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN NEW.CHATMESSAGE_TIMESTAMP        ELSE rcs_conversations.CHATMESSAGE_TIMESTAMP END),       CHATMESSAGE_MESSAGE_COUNT=         (CASE WHEN NEW.class<11           THEN CHATMESSAGE_MESSAGE_COUNT+1            ELSE CHATMESSAGE_MESSAGE_COUNT END),       CHATMESSAGE_UNREAD_COUNT=         (CASE WHEN NEW.CHATMESSAGE_MESSAGE_STATUS=0           THEN CHATMESSAGE_UNREAD_COUNT+1            ELSE CHATMESSAGE_UNREAD_COUNT END),       CHATMESSAGE_MIME_TYPE=        (CASE WHEN rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN NEW.CHATMESSAGE_MIME_TYPE        ELSE rcs_conversations.CHATMESSAGE_MIME_TYPE END),       CHATMESSAGE_TYPE=        (CASE WHEN rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN NEW.CHATMESSAGE_TYPE        ELSE rcs_conversations.CHATMESSAGE_TYPE END), has_attachment=        (CASE WHEN NEW.CHATMESSAGE_TYPE = 5 AND rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN 1         ELSE has_attachment END),       read=        (CASE WHEN NEW.CHATMESSAGE_MESSAGE_STATUS=      0 AND rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN 0         ELSE read END),       class=        (CASE WHEN rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN NEW.class        ELSE rcs_conversations.class END)   WHERE rcs_conversations.CHATMESSAGE_CONVERSATION=NEW.CHATMESSAGE_CONVERSATION;  END;");
            sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_rcs_thread_after_insert_rcs_message_dapi;");
            sQLiteDatabase.execSQL(UPDATE_RCS_THREAD_AFTER_INSERT_RCS_MESSAGE_DAPI);
        }
    }

    private void upgradeDatabaseToVersion621000(SQLiteDatabase sQLiteDatabase) {
        try {
            String canonicalPath = this.mContext.getDir("parts", 0).getCanonicalPath();
            int iLastIndexOf = canonicalPath.lastIndexOf(File.separator, canonicalPath.lastIndexOf("parts"));
            String str = canonicalPath.substring(iLastIndexOf) + File.separator;
            sQLiteDatabase.execSQL("UPDATE part SET _data = '" + canonicalPath.substring(0, iLastIndexOf) + "' || SUBSTR(_data, INSTR(_data, '" + str + "')) WHERE INSTR(_data, '" + str + "') > 0");
        } catch (IOException e) {
            Log.e("Mms/Provider/MmsSmsDatabaseHelper", "openFile: check file path failed " + e, e);
        }
    }

    private void upgradeDatabaseToVersion631000(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE raw ADD COLUMN deleted INTEGER DEFAULT 0");
    }

    private void upgradeDatabaseToVersion641000(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE raw ADD COLUMN message_body TEXT");
    }

    private void upgradeDatabaseToVersion641001(int i, SQLiteDatabase sQLiteDatabase) {
        if (MTK_RCS_SUPPORT && i < 601023) {
            sQLiteDatabase.execSQL("ALTER TABLE rcs_conversations ADD COLUMN DAPI_CONVERSATION_STATUS INTEGER DEFAULT 0");
            createDAPIRCSThreadView(sQLiteDatabase);
            createRcsMessageStatusTriggers(sQLiteDatabase);
        }
    }

    private void upgradeDatabaseToVersion641002(int i, SQLiteDatabase sQLiteDatabase) {
        if (MTK_RCS_SUPPORT && i < 601024) {
            sQLiteDatabase.execSQL("ALTER TABLE rcs_conversations ADD COLUMN at_me INTEGER DEFAULT 0");
            createDAPIRCSThreadView(sQLiteDatabase);
            sQLiteDatabase.execSQL("ALTER TABLE rcs_message ADD COLUMN at_me INTEGER DEFAULT 0");
            createRcsAtMeTrigger(sQLiteDatabase);
            upgradeDatabaseToVersion601011(sQLiteDatabase);
        }
    }

    private void upgradeDatabaseToVersion661000(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.execSQL("ALTER TABLE raw ADD COLUMN display_originating_addr TEXT");
        } catch (Exception e) {
            Log.e("Mms/Provider/MmsSmsDatabaseHelper", "got exception upgradeDatabaseToVersion661000: " + e.toString());
        }
        createThreadIdDateIndex(sQLiteDatabase);
    }

    private void upgradeDatabaseToVersion670010(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_threads_error_on_update_sms");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_threads_error_on_update_sms   AFTER UPDATE OF type ON sms  WHEN (OLD.type != 5 AND NEW.type = 5)    OR (OLD.type = 5 AND NEW.type != 5) BEGIN   UPDATE threads SET error =     CASE      WHEN NEW.type = 5 THEN error + 1      ELSE error - 1    END   WHERE _id = NEW.thread_id; END;");
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase writableDatabase;
        boolean z;
        boolean z2;
        boolean z3;
        writableDatabase = super.getWritableDatabase();
        if (!sTriedAutoIncrement) {
            boolean z4 = true;
            sTriedAutoIncrement = true;
            boolean zHasAutoIncrement = hasAutoIncrement(writableDatabase, "threads");
            boolean zHasAutoIncrement2 = hasAutoIncrement(writableDatabase, "canonical_addresses");
            boolean zHasAutoIncrement3 = hasAutoIncrement(writableDatabase, "part");
            boolean zHasAutoIncrement4 = hasAutoIncrement(writableDatabase, "pdu");
            Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[getWritableDatabase] hasAutoIncrementThreads: " + zHasAutoIncrement + " hasAutoIncrementAddresses: " + zHasAutoIncrement2 + " hasAutoIncrementPart: " + zHasAutoIncrement3 + " hasAutoIncrementPdu: " + zHasAutoIncrement4);
            if (!zHasAutoIncrement) {
                writableDatabase.beginTransaction();
                try {
                    try {
                        upgradeThreadsTableToAutoIncrement(writableDatabase);
                        writableDatabase.setTransactionSuccessful();
                        writableDatabase.endTransaction();
                    } finally {
                    }
                } catch (Throwable th) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", "Failed to add autoIncrement to threads;: " + th.getMessage(), th);
                    writableDatabase.endTransaction();
                    z = false;
                }
            }
            z = true;
            if (!zHasAutoIncrement2) {
                writableDatabase.beginTransaction();
                try {
                    try {
                        upgradeAddressTableToAutoIncrement(writableDatabase);
                        writableDatabase.setTransactionSuccessful();
                    } catch (Throwable th2) {
                        Log.e("Mms/Provider/MmsSmsDatabaseHelper", "Failed to add autoIncrement to canonical_addresses: " + th2.getMessage(), th2);
                        writableDatabase.endTransaction();
                        z2 = false;
                    }
                } finally {
                }
            }
            z2 = true;
            if (!zHasAutoIncrement3) {
                writableDatabase.beginTransaction();
                try {
                    try {
                        upgradePartTableToAutoIncrement(writableDatabase);
                        writableDatabase.setTransactionSuccessful();
                        writableDatabase.endTransaction();
                    } finally {
                    }
                } catch (Throwable th3) {
                    Log.e("Mms/Provider/MmsSmsDatabaseHelper", "Failed to add autoIncrement to part: " + th3.getMessage(), th3);
                    writableDatabase.endTransaction();
                    z3 = false;
                }
            }
            z3 = true;
            if (!zHasAutoIncrement4) {
                writableDatabase.beginTransaction();
                try {
                    try {
                        upgradePduTableToAutoIncrement(writableDatabase);
                        writableDatabase.setTransactionSuccessful();
                    } catch (Throwable th4) {
                        Log.e("Mms/Provider/MmsSmsDatabaseHelper", "Failed to add autoIncrement to pdu: " + th4.getMessage(), th4);
                        writableDatabase.endTransaction();
                        z4 = false;
                        if (z) {
                            if (sFakeLowStorageTest) {
                            }
                            if (this.mLowStorageMonitor == null) {
                            }
                        }
                    }
                } finally {
                }
            }
            if (z || !z2 || !z3 || !z4) {
                if (sFakeLowStorageTest) {
                    sFakeLowStorageTest = false;
                }
                if (this.mLowStorageMonitor == null) {
                    Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[getWritableDatabase] turning on storage monitor");
                    this.mLowStorageMonitor = new LowStorageMonitor();
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
                    intentFilter.addAction("android.intent.action.DEVICE_STORAGE_OK");
                    this.mContext.registerReceiver(this.mLowStorageMonitor, intentFilter);
                }
            } else if (this.mLowStorageMonitor != null) {
                Log.d("Mms/Provider/MmsSmsDatabaseHelper", "Unregistering mLowStorageMonitor - we've upgraded");
                this.mContext.unregisterReceiver(this.mLowStorageMonitor);
                this.mLowStorageMonitor = null;
            }
        }
        return writableDatabase;
    }

    private boolean hasAutoIncrement(SQLiteDatabase sQLiteDatabase, String str) {
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='" + str + "'", null);
        if (cursorRawQuery != null) {
            try {
                if (cursorRawQuery.moveToFirst()) {
                    String string = cursorRawQuery.getString(0);
                    zContains = string != null ? string.contains("AUTOINCREMENT") : false;
                    Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[MmsSmsDb] tableName: " + str + " hasAutoIncrement: " + string + " result: " + zContains);
                }
            } finally {
                cursorRawQuery.close();
            }
        }
        return zContains;
    }

    private void upgradeThreadsTableToAutoIncrement(SQLiteDatabase sQLiteDatabase) {
        if (hasAutoIncrement(sQLiteDatabase, "threads")) {
            Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeThreadsTableToAutoIncrement: already upgraded");
            return;
        }
        Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeThreadsTableToAutoIncrement: upgrading");
        sQLiteDatabase.execSQL("CREATE TABLE threads_temp (_id INTEGER PRIMARY KEY AUTOINCREMENT,date INTEGER DEFAULT 0,date_sent INTEGER DEFAULT 0,message_count INTEGER DEFAULT 0,readcount INTEGER DEFAULT 0,recipient_ids TEXT,snippet TEXT,snippet_cs INTEGER DEFAULT 0,read INTEGER DEFAULT 1,type INTEGER DEFAULT 0,error INTEGER DEFAULT 0,has_attachment INTEGER DEFAULT 0,li_date INTEGER DEFAULT 0,li_snippet TEXT,li_snippet_cs INTEGER DEFAULT 0,status INTEGER DEFAULT 0);");
        sQLiteDatabase.execSQL("INSERT INTO threads_temp SELECT * from threads;");
        sQLiteDatabase.execSQL("DROP TABLE threads;");
        sQLiteDatabase.execSQL("ALTER TABLE threads_temp RENAME TO threads;");
    }

    private void upgradeAddressTableToAutoIncrement(SQLiteDatabase sQLiteDatabase) {
        if (hasAutoIncrement(sQLiteDatabase, "canonical_addresses")) {
            Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeAddressTableToAutoIncrement: already upgraded");
            return;
        }
        Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[MmsSmsDb] upgradeAddressTableToAutoIncrement: upgrading");
        sQLiteDatabase.execSQL("CREATE TABLE canonical_addresses_temp (_id INTEGER PRIMARY KEY AUTOINCREMENT,address TEXT);");
        sQLiteDatabase.execSQL("INSERT INTO canonical_addresses_temp SELECT * from canonical_addresses;");
        sQLiteDatabase.execSQL("DROP TABLE canonical_addresses;");
        sQLiteDatabase.execSQL("ALTER TABLE canonical_addresses_temp RENAME TO canonical_addresses;");
    }

    private class LowStorageMonitor extends BroadcastReceiver {
        public LowStorageMonitor() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[LowStorageMonitor] onReceive intent " + action);
            if ("android.intent.action.DEVICE_STORAGE_OK".equals(action)) {
                boolean unused = MmsSmsDatabaseHelper.sTriedAutoIncrement = false;
            }
        }
    }

    private void updateThreadsAttachmentColumn(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("UPDATE threads SET has_attachment=1 WHERE _id IN   (SELECT DISTINCT pdu.thread_id FROM part    JOIN pdu ON pdu._id=part.mid    WHERE part.ct != 'text/plain' AND part.ct != 'application/smil')");
    }

    public static void updateThreadReadAfterDeleteMessage(SQLiteDatabase sQLiteDatabase, long j) {
        if (MTK_WAPPUSH_SUPPORT) {
            sQLiteDatabase.execSQL(" UPDATE threads SET read =     CASE (SELECT COUNT(sms._id) FROM sms               WHERE sms.thread_id = " + j + "              AND sms.read=0) +           (SELECT COUNT(pdu._id) FROM pdu               WHERE pdu.thread_id = " + j + "              AND (pdu.m_type=132 OR pdu.m_type=130 OR pdu.m_type=128)               AND pdu.read=0) +            (SELECT COUNT(wappush._id) FROM wappush                WHERE  wappush.thread_id = " + j + "               AND wappush.read=0) +            (SELECT COUNT(cellbroadcast._id) FROM cellbroadcast                WHERE  cellbroadcast.thread_id = " + j + "               AND cellbroadcast.read=0)     WHEN 0 THEN 1     ELSE 0     END  WHERE threads._id = " + j + ";");
            return;
        }
        sQLiteDatabase.execSQL(" UPDATE threads SET read =     CASE (SELECT COUNT(sms._id) FROM sms               WHERE sms.thread_id = " + j + "              AND sms.read=0) +           (SELECT COUNT(pdu._id) FROM pdu               WHERE pdu.thread_id = " + j + "              AND (pdu.m_type=132 OR pdu.m_type=130 OR pdu.m_type=128)               AND pdu.read=0) +            (SELECT COUNT(cellbroadcast._id) FROM cellbroadcast                WHERE  cellbroadcast.thread_id = " + j + "               AND cellbroadcast.read=0)     WHEN 0 THEN 1     ELSE 0     END  WHERE threads._id = " + j + ";");
    }

    private void createWapPushTables(SQLiteDatabase sQLiteDatabase) {
        if (MTK_WAPPUSH_SUPPORT) {
            sQLiteDatabase.execSQL("CREATE TABLE wappush (_id INTEGER PRIMARY KEY,thread_id INTEGER,address TEXT NOT NULL,service_center TEXT NOT NULL,seen INTEGER DEFAULT 0,read INTEGER DEFAULT 0,locked INTEGER DEFAULT 0,error INTEGER DEFAULT 0,sub_id INTEGER DEFAULT -1, date INTEGER,type INTEGER DEFAULT 0,siid TEXT,url TEXT,action INTEGER,created INTEGER,expiration INTEGER,text TEXT);");
        }
    }

    private void createQuickText(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE quicktext (_id INTEGER PRIMARY KEY,text TEXT);");
    }

    private void createCBTables(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE cellbroadcast(_id INTEGER PRIMARY KEY,sub_id INTEGER DEFAULT -1,locked INTEGER DEFAULT 0,body TEXT,channel_id INTEGER,thread_id INTEGER,read INTEGER DEFAULT 0,seen INTEGER DEFAULT 0,date_sent INTEGER DEFAULT 0,date INTEGER);");
    }

    private void createThreadSettingsTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE thread_settings(_id INTEGER PRIMARY KEY,thread_id INTEGER,spam INTEGER DEFAULT 0,notification_enable INTEGER DEFAULT 1,mute INTEGER DEFAULT 0,mute_start INTEGER DEFAULT 0,ringtone TEXT,_data TEXT,vibrate INTEGER DEFAULT 1,top INTEGER DEFAULT 0);");
        sQLiteDatabase.execSQL("INSERT INTO thread_settings (_id,thread_id) VALUES (0,0)");
    }

    private void createUserSmsTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE usersms (_id INTEGER PRIMARY KEY,pdus TEXT,format TEXT,simId INTEGER);");
    }

    private void createUserMmsTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE usermms (_id INTEGER PRIMARY KEY,transactionId INTEGER,pduType INTEGER,header TEXT,data TEXT,address TEXT,service_center TEXT,simId INTEGER,mimeType TEXT);");
    }

    private void createUserCBTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE usercb (_id INTEGER PRIMARY KEY,action TEXT,pdus TEXT,simId INTEGER);");
    }

    private void upgradePartTableToAutoIncrement(SQLiteDatabase sQLiteDatabase) {
        if (hasAutoIncrement(sQLiteDatabase, "part")) {
            Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[MmsSmsDb] upgradePartTableToAutoIncrement: already upgraded");
            return;
        }
        Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[MmsSmsDb] upgradePartTableToAutoIncrement: upgrading");
        sQLiteDatabase.execSQL("CREATE TABLE part_temp (_id INTEGER PRIMARY KEY AUTOINCREMENT,mid INTEGER,seq INTEGER DEFAULT 0,ct TEXT,name TEXT,chset INTEGER,cd TEXT,fn TEXT,cid TEXT,cl TEXT,ctt_s INTEGER,ctt_t TEXT,_data TEXT,text TEXT);");
        sQLiteDatabase.execSQL("INSERT INTO part_temp SELECT * from part;");
        sQLiteDatabase.execSQL("DROP TABLE part;");
        sQLiteDatabase.execSQL("ALTER TABLE part_temp RENAME TO part;");
        createMmsTriggers(sQLiteDatabase);
    }

    private void upgradePduTableToAutoIncrement(SQLiteDatabase sQLiteDatabase) {
        if (hasAutoIncrement(sQLiteDatabase, "pdu")) {
            Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[MmsSmsDb] upgradePduTableToAutoIncrement: already upgraded");
            return;
        }
        Log.d("Mms/Provider/MmsSmsDatabaseHelper", "[MmsSmsDb] upgradePduTableToAutoIncrement: upgrading");
        sQLiteDatabase.execSQL("CREATE TABLE pdu_temp (_id INTEGER PRIMARY KEY AUTOINCREMENT,thread_id INTEGER,date INTEGER,date_sent INTEGER DEFAULT 0,msg_box INTEGER,read INTEGER DEFAULT 0,m_id TEXT,sub TEXT,sub_cs INTEGER,ct_t TEXT,ct_l TEXT,exp INTEGER,m_cls TEXT,m_type INTEGER,v INTEGER,m_size INTEGER,pri INTEGER,rr INTEGER,rpt_a INTEGER,resp_st INTEGER,st INTEGER,st_ext INTEGER DEFAULT 0,tr_id TEXT,retr_st INTEGER,retr_txt TEXT,retr_txt_cs INTEGER,read_status INTEGER,ct_cls INTEGER,resp_txt TEXT,d_tm INTEGER,d_rpt INTEGER,locked INTEGER DEFAULT 0,sub_id INTEGER DEFAULT -1,service_center TEXT,seen INTEGER DEFAULT 0,creator TEXT,text_only INTEGER DEFAULT 0);");
        sQLiteDatabase.execSQL("INSERT INTO pdu_temp SELECT * from pdu;");
        sQLiteDatabase.execSQL("DROP TABLE pdu;");
        sQLiteDatabase.execSQL("ALTER TABLE pdu_temp RENAME TO pdu;");
        createMmsTriggers(sQLiteDatabase);
    }

    public static void updateMultiThreads(SQLiteDatabase sQLiteDatabase, long[] jArr) {
        Log.d("Mms/Provider/MmsSmsDatabaseHelper", "updateMultiThreads start, deletedThreads.length = " + jArr.length);
        long[] jArr2 = null;
        if (MTK_WAPPUSH_SUPPORT) {
            sQLiteDatabase.delete("threads", "status = 0 AND type <> ? AND _id NOT IN          (SELECT thread_id FROM sms where thread_id is not null            UNION SELECT thread_id FROM pdu where thread_id is not null            UNION SELECT DISTINCT thread_id FROM cellbroadcast where thread_id is not null)", new String[]{String.valueOf(2)});
        } else {
            sQLiteDatabase.delete("threads", "status = 0 AND _id NOT IN          (SELECT thread_id FROM sms where thread_id is not null            UNION SELECT thread_id FROM pdu where thread_id is not null            UNION SELECT DISTINCT thread_id FROM cellbroadcast where thread_id is not null)", null);
        }
        removeOrphanedAddresses(sQLiteDatabase);
        Log.d("Mms/Provider/MmsSmsDatabaseHelper", "delete obsolete threads and addresses end");
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT _id FROM threads WHERE status<>0 OR _id IN (SELECT DISTINCT thread_id FROM sms  UNION SELECT DISTINCT thread_id FROM cellbroadcast  UNION SELECT DISTINCT thread_id FROM pdu)", null);
        if (cursorRawQuery != null) {
            jArr2 = new long[cursorRawQuery.getCount()];
            int i = 0;
            while (cursorRawQuery.moveToNext()) {
                try {
                    int i2 = i + 1;
                    jArr2[i] = cursorRawQuery.getLong(0);
                    i = i2;
                } finally {
                    cursorRawQuery.close();
                }
            }
        }
        Arrays.sort(jArr2);
        for (long j : jArr) {
            if (Arrays.binarySearch(jArr2, j) >= 0) {
                updateThread(sQLiteDatabase, j);
            } else if (j >= 0) {
                File file = new File("/data/data/com.android.providers.telephony/app_wallpaper");
                if (file.exists()) {
                    String str = j + ".jpeg";
                    String[] list = file.list();
                    int length = list.length;
                    if (length > 0) {
                        for (int i3 = 0; i3 < length; i3++) {
                            if (str.equals(list[i3])) {
                                Log.d("Mms/Provider/MmsSmsDatabaseHelper", "wallpaper " + str + "isDelete " + new File("/data/data/com.android.providers.telephony/app_wallpaper", list[i3]).delete());
                            }
                        }
                    }
                }
            }
        }
        Log.d("Mms/Provider/MmsSmsDatabaseHelper", "updateMultiThreads end");
    }

    private void createMwiTables(SQLiteDatabase sQLiteDatabase) {
        Log.d("Mms/Provider/MmsSmsDatabaseHelper", "Created table mwi");
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS mwi (_id INTEGER PRIMARY KEY,msg_account TEXT,to_account TEXT,from_account TEXT,subject TEXT,msg_date INTEGER,priority INTEGER,msg_id TEXT,msg_context INTEGER,seen INTEGER DEFAULT 0,read INTEGER DEFAULT 0,got_content INTEGER DEFAULT 0);");
    }

    private void createRCSTables(SQLiteDatabase sQLiteDatabase) {
        createRCSMessageTable(sQLiteDatabase);
        createRCSConverstionTable(sQLiteDatabase);
        createRCSThreadView(sQLiteDatabase);
        createRCSTriggers(sQLiteDatabase);
        createDAPIRCSThreadView(sQLiteDatabase);
        createRcsMessageStatusTriggers(sQLiteDatabase);
    }

    private void createRCSMessageTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE rcs_message ( _id INTEGER PRIMARY KEY AUTOINCREMENT, date_sent INTEGER DEFAULT 0, seen INTEGER DEFAULT 1, locked INTEGER DEFAULT 0, sub_id INTEGER DEFAULT -1, ipmsg_id INTEGER DEFAULT 0, class INTEGER DEFAULT 0, file_path TEXT, CHATMESSAGE_MESSAGE_ID TEXT, CHATMESSAGE_CHAT_ID TEXT, CHATMESSAGE_CONTACT_NUMBER TEXT, CHATMESSAGE_BODY TEXT, CHATMESSAGE_TIMESTAMP LONG DEFAULT 0, CHATMESSAGE_MESSAGE_STATUS INTEGER, CHATMESSAGE_TYPE INTEGER, CHATMESSAGE_DIRECTION INTEGER, CHATMESSAGE_FLAG INTEGER, CHATMESSAGE_ISBLOCKED INTEGER DEFAULT 0, CHATMESSAGE_CONVERSATION INTEGER DEFAULT 0, CHATMESSAGE_MIME_TYPE TEXT, at_me INTEGER DEFAULT 0 );");
    }

    private void createRCSConverstionTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE rcs_conversations ( _id INTEGER PRIMARY KEY AUTOINCREMENT, read INTEGER DEFAULT 1, error INTEGER DEFAULT 0, has_attachment INTEGER DEFAULT 0, class INTEGER DEFAULT 0, CHATMESSAGE_CONVERSATION LONG DEFAULT 0, CHATMESSAGE_RECIPIENTS TEXT, CHATMESSAGE_BODY TEXT, CHATMESSAGE_TIMESTAMP LONG DEFAULT 0, CHATMESSAGE_FLAG INTEGER DEFAULT 1, CHATMESSAGE_TYPE INTEGER DEFAULT 1, CHATMESSAGE_MESSAGE_COUNT INTEGER DEFAULT 0, CHATMESSAGE_UNREAD_COUNT INTEGER DEFAULT 0, CHATMESSAGE_MIME_TYPE TEXT, at_me INTEGER DEFAULT 0, DAPI_CONVERSATION_BODY TEXT, DAPI_CONVERSATION_TIMESTAMP LONG DEFAULT 0, DAPI_CONVERSATION_TYPE INTEGER DEFAULT 1, DAPI_CONVERSATION_UNREAD_COUNT INTEGER DEFAULT 0, DAPI_CONVERSATION_MSG_COUNT INTEGER DEFAULT 0, DAPI_CONVERSATION_MIMETYPE TEXT, DAPI_CONVERSATION_STATUS INTEGER DEFAULT 0);");
    }

    private void createRCSThreadView(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS rcs_threads;");
        sQLiteDatabase.execSQL("CREATE VIEW rcs_threads AS  SELECT     threads._id AS _id     ,CASE WHEN T5.CHATMESSAGE_TIMESTAMP>threads.date          THEN T5.CHATMESSAGE_BODY         ELSE threads.snippet END AS snippet     ,CASE WHEN T5.CHATMESSAGE_TIMESTAMP>threads.date          THEN 0 ELSE threads.snippet_cs END AS snippet_cs     ,0 AS date_sent     ,threads.type     ,CASE WHEN T5.CHATMESSAGE_TIMESTAMP>threads.date          THEN T5.CHATMESSAGE_TIMESTAMP         ELSE threads.date END AS date     ,T5.CHATMESSAGE_MESSAGE_COUNT -          T5.CHATMESSAGE_UNREAD_COUNT+threads.readcount          AS readcount     ,T5.CHATMESSAGE_MESSAGE_COUNT+threads.message_count          AS message_count     ,T5.error+threads.error AS error     ,CASE WHEN T5.CHATMESSAGE_UNREAD_COUNT         +threads.message_count-threads.readcount>0          THEN 0 ELSE 1 END AS read     ,CASE WHEN T5.has_attachment         +threads.has_attachment>0 THEN 1          ELSE 0 END AS has_attachment     ,threads.status     ,threads.recipient_ids     ,threads.archived    ,CASE WHEN T5.CHATMESSAGE_TIMESTAMP>threads.date           THEN T5.class          ELSE 0 END AS class     ,T5.CHATMESSAGE_RECIPIENTS AS CHATMESSAGE_RECIPIENTS     ,T5.CHATMESSAGE_FLAG AS CHATMESSAGE_FLAG     ,CASE WHEN T5.CHATMESSAGE_TIMESTAMP>threads.date          THEN T5.CHATMESSAGE_TYPE         ELSE 0 END AS CHATMESSAGE_TYPE     ,CASE WHEN T5.CHATMESSAGE_TIMESTAMP>threads.date          THEN T5.CHATMESSAGE_MIME_TYPE         ELSE NULL END AS CHATMESSAGE_MIME_TYPE     ,T5.at_me FROM rcs_conversations T5, threads  WHERE threads._id=T5.CHATMESSAGE_CONVERSATION; ");
    }

    private void createDAPIRCSThreadView(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS rcs_threads_dapi;");
        sQLiteDatabase.execSQL("CREATE VIEW rcs_threads_dapi AS  SELECT     threads._id AS CHATMESSAGE_CONVERSATION_ID    ,T.CHATMESSAGE_RECIPIENTS         AS CHATMESSAGE_RECIPIENTS    ,CASE WHEN T.DAPI_CONVERSATION_TIMESTAMP>threads.date          THEN T.DAPI_CONVERSATION_BODY         ELSE threads.snippet END AS CHATMESSAGE_BODY    ,CASE WHEN T.DAPI_CONVERSATION_TIMESTAMP>threads.date          THEN T.DAPI_CONVERSATION_TIMESTAMP         ELSE threads.date END AS CHATMESSAGE_TIMESTAMP    ,T.CHATMESSAGE_FLAG AS CHATMESSAGE_FLAG    ,CASE WHEN T.DAPI_CONVERSATION_TIMESTAMP>threads.date          THEN T.DAPI_CONVERSATION_TYPE         ELSE 0 END AS CHATMESSAGE_TYPE    ,T.DAPI_CONVERSATION_UNREAD_COUNT +          threads.message_count-threads.readcount         AS CHATMESSAGE_UNREAD_COUNT    ,T.DAPI_CONVERSATION_MSG_COUNT+threads.message_count          AS CHATMESSAGE_MESSAGE_COUNT    ,CASE WHEN T.DAPI_CONVERSATION_TIMESTAMP>threads.date          THEN T.DAPI_CONVERSATION_MIMETYPE         ELSE NULL END AS CHATMESSAGE_MIME_TYPE    ,T.DAPI_CONVERSATION_STATUS        AS CHATMESSAGE_MESSAGE_STATUS FROM rcs_conversations T, threads  WHERE threads._id=T.CHATMESSAGE_CONVERSATION UNION  SELECT     T.CHATMESSAGE_CONVERSATION AS            CHATMESSAGE_CONVERSATION_ID   ,T.CHATMESSAGE_RECIPIENTS AS            CHATMESSAGE_RECIPIENTS   ,T.DAPI_CONVERSATION_BODY AS            CHATMESSAGE_BODY   ,T.DAPI_CONVERSATION_TIMESTAMP AS            CHATMESSAGE_TIMESTAMP   ,T.CHATMESSAGE_FLAG AS            CHATMESSAGE_FLAG   ,T.DAPI_CONVERSATION_TYPE AS            CHATMESSAGE_TYPE   ,T.DAPI_CONVERSATION_UNREAD_COUNT AS            CHATMESSAGE_UNREAD_COUNT   ,T.DAPI_CONVERSATION_MSG_COUNT AS            CHATMESSAGE_MESSAGE_COUNT   ,T.DAPI_CONVERSATION_MIMETYPE AS            CHATMESSAGE_MIME_TYPE   ,T.DAPI_CONVERSATION_STATUS AS            CHATMESSAGE_MESSAGE_STATUS FROM rcs_conversations T  WHERE T.CHATMESSAGE_FLAG = 4 ; ");
    }

    private void createRCSTriggers(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS delete_conversation_after_thread_delete;");
        sQLiteDatabase.execSQL("CREATE TRIGGER delete_conversation_after_thread_delete  AFTER DELETE ON threads  BEGIN     DELETE FROM rcs_conversations     WHERE OLD._id=rcs_conversations.CHATMESSAGE_CONVERSATION;  END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_rcs_thread_after_insert_rcs_message;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_rcs_thread_after_insert_rcs_message  AFTER INSERT ON rcs_message BEGIN   UPDATE rcs_conversations     SET       CHATMESSAGE_BODY=        (CASE WHEN rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN NEW.CHATMESSAGE_BODY        ELSE rcs_conversations.CHATMESSAGE_BODY END),       CHATMESSAGE_TIMESTAMP=        (CASE WHEN rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN NEW.CHATMESSAGE_TIMESTAMP        ELSE rcs_conversations.CHATMESSAGE_TIMESTAMP END),       CHATMESSAGE_MESSAGE_COUNT=         (CASE WHEN NEW.class<11           THEN CHATMESSAGE_MESSAGE_COUNT+1            ELSE CHATMESSAGE_MESSAGE_COUNT END),       CHATMESSAGE_UNREAD_COUNT=         (CASE WHEN NEW.CHATMESSAGE_MESSAGE_STATUS=0           THEN CHATMESSAGE_UNREAD_COUNT+1            ELSE CHATMESSAGE_UNREAD_COUNT END),       CHATMESSAGE_MIME_TYPE=        (CASE WHEN rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN NEW.CHATMESSAGE_MIME_TYPE        ELSE rcs_conversations.CHATMESSAGE_MIME_TYPE END),       CHATMESSAGE_TYPE=        (CASE WHEN rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN NEW.CHATMESSAGE_TYPE        ELSE rcs_conversations.CHATMESSAGE_TYPE END), has_attachment=        (CASE WHEN NEW.CHATMESSAGE_TYPE = 5 AND rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN 1         ELSE has_attachment END),       read=        (CASE WHEN NEW.CHATMESSAGE_MESSAGE_STATUS=      0 AND rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN 0         ELSE read END),       class=        (CASE WHEN rcs_conversations.CHATMESSAGE_TIMESTAMP <=NEW.CHATMESSAGE_TIMESTAMP        THEN NEW.class        ELSE rcs_conversations.class END)   WHERE rcs_conversations.CHATMESSAGE_CONVERSATION=NEW.CHATMESSAGE_CONVERSATION;  END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_rcs_thread_after_insert_rcs_message_dapi;");
        sQLiteDatabase.execSQL(UPDATE_RCS_THREAD_AFTER_INSERT_RCS_MESSAGE_DAPI);
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_rcs_thread_after_update_rcs_message_status;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_rcs_thread_after_update_rcs_message_status  AFTER UPDATE OF CHATMESSAGE_MESSAGE_STATUS ON rcs_message WHEN NEW.class<11 BEGIN     UPDATE rcs_conversations        SET read=(                   CASE                        (SELECT COUNT(_id)                        FROM rcs_message WHERE                        CHATMESSAGE_MESSAGE_STATUS = 0 AND                        CHATMESSAGE_CONVERSATION= NEW.CHATMESSAGE_CONVERSATION )                   WHEN 0 THEN 0 ELSE 1  END),             CHATMESSAGE_UNREAD_COUNT=(                   SELECT COUNT(_id)                    FROM rcs_message WHERE                    CHATMESSAGE_MESSAGE_STATUS=0 AND                    CHATMESSAGE_CONVERSATION= NEW.CHATMESSAGE_CONVERSATION),             error=(                   CASE                        (SELECT COUNT(_id)                        FROM rcs_message WHERE                        CHATMESSAGE_MESSAGE_STATUS = 5 AND                        CHATMESSAGE_CONVERSATION= NEW.CHATMESSAGE_CONVERSATION )                   WHEN 0 THEN 0 ELSE 1  END)     WHERE rcs_conversations.CHATMESSAGE_CONVERSATION=NEW.CHATMESSAGE_CONVERSATION;  END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_rcs_thread_after_update_rcs_message_status_dapi;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_rcs_thread_after_update_rcs_message_status_dapi  AFTER UPDATE OF CHATMESSAGE_MESSAGE_STATUS ON rcs_message WHEN NEW.class=0 BEGIN     UPDATE rcs_conversations        SET DAPI_CONVERSATION_UNREAD_COUNT=(                   SELECT COUNT(_id)                    FROM rcs_message WHERE                    CHATMESSAGE_MESSAGE_STATUS=0 AND                    class=0 AND                    CHATMESSAGE_CONVERSATION= NEW.CHATMESSAGE_CONVERSATION)     WHERE rcs_conversations.CHATMESSAGE_CONVERSATION=NEW.CHATMESSAGE_CONVERSATION;  END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_rcs_thread_after_update_rcs_blocked_dapi;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_rcs_thread_after_update_rcs_blocked_dapi  AFTER UPDATE OF CHATMESSAGE_ISBLOCKED ON rcs_message WHEN NEW.class=0 BEGIN    UPDATE rcs_conversations     SET DAPI_CONVERSATION_MSG_COUNT=(          SELECT COUNT(_id)           FROM rcs_message WHERE class=0 AND CHATMESSAGE_CONVERSATION= NEW.CHATMESSAGE_CONVERSATION AND CHATMESSAGE_ISBLOCKED=0), DAPI_CONVERSATION_BODY=(          SELECT CHATMESSAGE_BODY          FROM rcs_message WHERE class=0 AND CHATMESSAGE_CONVERSATION= NEW.CHATMESSAGE_CONVERSATION AND CHATMESSAGE_ISBLOCKED=0           ORDER BY CHATMESSAGE_TIMESTAMP DESC LIMIT 1), DAPI_CONVERSATION_TIMESTAMP=(          SELECT CHATMESSAGE_TIMESTAMP          FROM rcs_message WHERE class=0 AND CHATMESSAGE_CONVERSATION= NEW.CHATMESSAGE_CONVERSATION AND CHATMESSAGE_ISBLOCKED=0           ORDER BY CHATMESSAGE_TIMESTAMP DESC LIMIT 1), DAPI_CONVERSATION_TYPE=(          SELECT CHATMESSAGE_TYPE          FROM rcs_message WHERE class=0 AND CHATMESSAGE_CONVERSATION= NEW.CHATMESSAGE_CONVERSATION AND CHATMESSAGE_ISBLOCKED=0           ORDER BY CHATMESSAGE_TIMESTAMP DESC LIMIT 1), DAPI_CONVERSATION_MIMETYPE=(          SELECT CHATMESSAGE_MIME_TYPE          FROM rcs_message WHERE class=0 AND CHATMESSAGE_CONVERSATION= NEW.CHATMESSAGE_CONVERSATION AND CHATMESSAGE_ISBLOCKED=0           ORDER BY CHATMESSAGE_TIMESTAMP DESC LIMIT 1)     WHERE rcs_conversations.CHATMESSAGE_CONVERSATION=NEW.CHATMESSAGE_CONVERSATION;  END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS rcs_words_update;");
        sQLiteDatabase.execSQL("CREATE TRIGGER rcs_words_update AFTER UPDATE OF CHATMESSAGE_BODY ON rcs_message  BEGIN UPDATE words  SET index_text = NEW.CHATMESSAGE_BODY  WHERE (source_id=NEW._id AND table_to_use=5);  END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS rcs_words_delete;");
        sQLiteDatabase.execSQL("CREATE TRIGGER rcs_words_delete AFTER DELETE ON rcs_message WHEN OLD.class=0 AND  OLD.CHATMESSAGE_TYPE=3 BEGIN DELETE FROM words  WHERE source_id=OLD._id AND table_to_use=5; END;");
        createRcsAtMeTrigger(sQLiteDatabase);
    }

    private void createRcsAtMeTrigger(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_rcs_thread_at_me_after_insert_rcs_message;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_rcs_thread_at_me_after_insert_rcs_message  AFTER INSERT ON rcs_message WHEN NEW.at_me=1 BEGIN  UPDATE rcs_conversations  SET at_me=    (SELECT COUNT(_id) FROM rcs_message     WHERE CHATMESSAGE_CONVERSATION=NEW.CHATMESSAGE_CONVERSATION AND at_me=1)  WHERE rcs_conversations.CHATMESSAGE_CONVERSATION=NEW.CHATMESSAGE_CONVERSATION;  END;");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS update_rcs_thread_at_me_after_update_rcs_message;");
        sQLiteDatabase.execSQL("CREATE TRIGGER update_rcs_thread_at_me_after_update_rcs_message  AFTER UPDATE OF at_me    ON rcs_message    WHEN OLD.at_me=1  BEGIN  UPDATE rcs_conversations  SET at_me=    (SELECT COUNT(_id) FROM rcs_message     WHERE CHATMESSAGE_CONVERSATION=NEW.CHATMESSAGE_CONVERSATION AND at_me=1)  WHERE rcs_conversations.CHATMESSAGE_CONVERSATION=NEW.CHATMESSAGE_CONVERSATION;  END;");
    }

    private void createRcsMessageStatusTriggers(SQLiteDatabase sQLiteDatabase) {
        String str = String.format("UPDATE rcs_conversations SET DAPI_CONVERSATION_STATUS = (SELECT (CASE WHEN T.msg_type=2 THEN T.status       WHEN T.msg_type=1 AND T.box=1 AND T.read=0 THEN 0      WHEN T.msg_type=1 AND T.box=1 AND T.read=1 THEN 2      WHEN T.msg_type=1 AND T.box=2 THEN 4      WHEN T.msg_type=1 AND T.box=3 THEN 6      WHEN T.msg_type=1 AND T.box=5 THEN 5      ELSE 3 END) AS MESSAGE_STATUS FROM ((%s) T))  WHERE rcs_conversations.CHATMESSAGE_CONVERSATION=%s ", String.format(" SELECT 1 AS msg_type, read, type AS box, date AS timestamp, 0 AS status, thread_id    FROM sms WHERE sms.thread_id=%s  UNION  SELECT 1 AS msg_type,read,msg_box AS box,date*1000 AS timestamp,0 AS status, thread_id    FROM pdu    WHERE (pdu.m_type=132 OR pdu.m_type=130 OR pdu.m_type=128) AND pdu.thread_id=%s  UNION  SELECT 2 AS msg_type, 0 AS read, 0 AS box, CHATMESSAGE_TIMESTAMP AS timestamp,        CHATMESSAGE_MESSAGE_STATUS AS status, CHATMESSAGE_CONVERSATION AS thread_id    FROM rcs_message WHERE CHATMESSAGE_CONVERSATION=%s AND class=0  ORDER BY timestamp DESC LIMIT 1", "NEW.thread_id", "NEW.thread_id", "NEW.thread_id"), "NEW.thread_id");
        String str2 = String.format("UPDATE rcs_conversations SET DAPI_CONVERSATION_STATUS = (SELECT (CASE WHEN T.msg_type=2 THEN T.status       WHEN T.msg_type=1 AND T.box=1 AND T.read=0 THEN 0      WHEN T.msg_type=1 AND T.box=1 AND T.read=1 THEN 2      WHEN T.msg_type=1 AND T.box=2 THEN 4      WHEN T.msg_type=1 AND T.box=3 THEN 6      WHEN T.msg_type=1 AND T.box=5 THEN 5      ELSE 3 END) AS MESSAGE_STATUS FROM ((%s) T))  WHERE rcs_conversations.CHATMESSAGE_CONVERSATION=%s ", String.format(" SELECT 1 AS msg_type, read, type AS box, date AS timestamp, 0 AS status, thread_id    FROM sms WHERE sms.thread_id=%s  UNION  SELECT 1 AS msg_type,read,msg_box AS box,date*1000 AS timestamp,0 AS status, thread_id    FROM pdu    WHERE (pdu.m_type=132 OR pdu.m_type=130 OR pdu.m_type=128) AND pdu.thread_id=%s  UNION  SELECT 2 AS msg_type, 0 AS read, 0 AS box, CHATMESSAGE_TIMESTAMP AS timestamp,        CHATMESSAGE_MESSAGE_STATUS AS status, CHATMESSAGE_CONVERSATION AS thread_id    FROM rcs_message WHERE CHATMESSAGE_CONVERSATION=%s AND class=0  ORDER BY timestamp DESC LIMIT 1", "NEW.CHATMESSAGE_CONVERSATION", "NEW.CHATMESSAGE_CONVERSATION", "NEW.CHATMESSAGE_CONVERSATION"), "NEW.CHATMESSAGE_CONVERSATION");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS rcs_update_status_on_insert_sms");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS rcs_update_status_on_update_sms_read");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS rcs_update_status_on_update_sms_type");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS rcs_update_status_on_insert_mms");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS rcs_update_status_on_update_mms_read");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS rcs_update_status_on_update_mms_msg_box");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS rcs_update_status_on_insert_rcs");
        sQLiteDatabase.execSQL("DROP TRIGGER IF EXISTS rcs_update_status_on_update_rcs");
        sQLiteDatabase.execSQL("CREATE TRIGGER rcs_update_status_on_insert_sms AFTER INSERT ON sms BEGIN " + str + " ;END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER rcs_update_status_on_update_sms_read AFTER UPDATE OF read ON sms BEGIN " + str + " ;END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER rcs_update_status_on_update_sms_type AFTER UPDATE OF type ON sms BEGIN " + str + " ;END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER rcs_update_status_on_insert_mms AFTER INSERT ON pdu BEGIN " + str + " ;END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER rcs_update_status_on_update_mms_read AFTER UPDATE OF read ON pdu BEGIN " + str + " ;END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER rcs_update_status_on_update_mms_msg_box AFTER UPDATE OF msg_box ON pdu BEGIN " + str + " ;END;");
        sQLiteDatabase.execSQL("CREATE TRIGGER rcs_update_status_on_update_rcs AFTER UPDATE OF CHATMESSAGE_MESSAGE_STATUS ON rcs_message BEGIN " + str2 + " ;END;");
    }
}
