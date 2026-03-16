package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.mms.util.DownloadDrmHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MmsProvider extends ContentProvider {
    private static final UriMatcher sURLMatcher = new UriMatcher(-1);
    private SQLiteOpenHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        setAppOps(14, 15);
        this.mOpenHelper = MmsSmsDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        int match = sURLMatcher.match(uri);
        switch (match) {
            case 0:
                constructQueryForBox(qb, 0);
                break;
            case 1:
                qb.setTables("pdu");
                qb.appendWhere("_id=" + uri.getPathSegments().get(0));
                break;
            case 2:
                constructQueryForBox(qb, 1);
                break;
            case 3:
            case 5:
            case 7:
            case 9:
                qb.setTables("pdu");
                qb.appendWhere("_id=" + uri.getPathSegments().get(1));
                qb.appendWhere(" AND msg_box=" + getMessageBoxByMatch(match));
                break;
            case 4:
                constructQueryForBox(qb, 2);
                break;
            case 6:
                constructQueryForBox(qb, 3);
                break;
            case 8:
                constructQueryForBox(qb, 4);
                break;
            case 10:
                qb.setTables("part");
                break;
            case 11:
                qb.setTables("part");
                qb.appendWhere("mid=" + uri.getPathSegments().get(0));
                break;
            case 12:
                qb.setTables("part");
                qb.appendWhere("_id=" + uri.getPathSegments().get(1));
                break;
            case 13:
                qb.setTables("addr");
                qb.appendWhere("msg_id=" + uri.getPathSegments().get(0));
                break;
            case 14:
                qb.setTables("rate");
                break;
            case 15:
                qb.setTables("addr INNER JOIN (SELECT P1._id AS id1, P2._id AS id2, P3._id AS id3, ifnull(P2.st, 0) AS delivery_status, ifnull(P3.read_status, 0) AS read_status, ifnull(P2.date, 0) AS delivery_date, ifnull(P3.date, 0) AS read_date FROM pdu P1 INNER JOIN pdu P2 ON P1.m_id=P2.m_id AND P2.m_type=134 LEFT JOIN pdu P3 ON P1.m_id=P3.m_id AND P3.m_type=136 UNION SELECT P1._id AS id1, P2._id AS id2, P3._id AS id3, ifnull(P2.st, 0) AS delivery_status, ifnull(P3.read_status, 0) AS read_status, ifnull(P2.date, 0) AS delivery_date, ifnull(P3.date, 0) AS read_date FROM pdu P1 INNER JOIN pdu P3 ON P1.m_id=P3.m_id AND P3.m_type=136 LEFT JOIN pdu P2 ON P1.m_id=P2.m_id AND P2.m_type=134) T ON (msg_id=id2 AND type=151) OR (msg_id=id3 AND type=137)");
                qb.appendWhere("T.id1 = " + uri.getLastPathSegment());
                qb.setDistinct(true);
                break;
            case 16:
                qb.setTables("addr join pdu on pdu._id = addr.msg_id");
                qb.appendWhere("pdu._id = " + uri.getLastPathSegment());
                qb.appendWhere(" AND addr.type = 151");
                break;
            case 17:
            default:
                Log.e("MmsProvider", "query: invalid request: " + uri);
                return null;
            case 18:
                qb.setTables("drm");
                qb.appendWhere("_id=" + uri.getLastPathSegment());
                break;
            case 19:
                qb.setTables("pdu group by thread_id");
                break;
        }
        String finalSortOrder = null;
        if (TextUtils.isEmpty(sortOrder)) {
            if (qb.getTables().equals("pdu")) {
                finalSortOrder = "date DESC";
            } else if (qb.getTables().equals("part")) {
                finalSortOrder = "seq";
            }
        } else {
            finalSortOrder = sortOrder;
        }
        try {
            SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
            Cursor ret = qb.query(db, projection, selection, selectionArgs, null, null, finalSortOrder);
            ret.setNotificationUri(getContext().getContentResolver(), uri);
            return ret;
        } catch (SQLiteException e) {
            Log.e("MmsProvider", "returning NULL cursor, query: " + uri, e);
            return null;
        }
    }

    private void constructQueryForBox(SQLiteQueryBuilder qb, int msgBox) {
        qb.setTables("pdu");
        if (msgBox != 0) {
            qb.appendWhere("msg_box=" + msgBox);
        }
    }

    @Override
    public String getType(Uri uri) {
        int match = sURLMatcher.match(uri);
        switch (match) {
            case 0:
            case 2:
            case 4:
            case 6:
            case 8:
                return "vnd.android-dir/mms";
            case 1:
            case 3:
            case 5:
            case 7:
            case 9:
                return "vnd.android/mms";
            case 10:
            case 11:
            default:
                return "*/*";
            case 12:
                Cursor cursor = this.mOpenHelper.getReadableDatabase().query("part", new String[]{"ct"}, "_id = ?", new String[]{uri.getLastPathSegment()}, null, null, null);
                if (cursor != null) {
                    try {
                        if (cursor.getCount() == 1 && cursor.moveToFirst()) {
                            return cursor.getString(0);
                        }
                        Log.e("MmsProvider", "cursor.count() != 1: " + uri);
                    } finally {
                        cursor.close();
                    }
                } else {
                    Log.e("MmsProvider", "cursor == null: " + uri);
                }
                return "*/*";
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String contentLocation;
        if (values != null && values.containsKey("_data")) {
            return null;
        }
        int callerUid = Binder.getCallingUid();
        int msgBox = 0;
        boolean notify = true;
        int match = sURLMatcher.match(uri);
        String table = "pdu";
        switch (match) {
            case 0:
                Integer msgBoxObj = values.getAsInteger("msg_box");
                msgBox = msgBoxObj != null ? msgBoxObj.intValue() : 1;
                break;
            case 1:
            case 3:
            case 5:
            case 7:
            case 9:
            case 10:
            case 12:
            case 15:
            case 16:
            default:
                Log.e("MmsProvider", "insert: invalid request: " + uri);
                return null;
            case 2:
                msgBox = 1;
                break;
            case 4:
                msgBox = 2;
                break;
            case 6:
                msgBox = 3;
                break;
            case 8:
                msgBox = 4;
                break;
            case 11:
                notify = false;
                table = "part";
                break;
            case 13:
                notify = false;
                table = "addr";
                break;
            case 14:
                notify = false;
                table = "rate";
                break;
            case 17:
                notify = false;
                table = "drm";
                break;
        }
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        Uri res = Telephony.Mms.CONTENT_URI;
        if (table.equals("pdu")) {
            boolean addDate = !values.containsKey("date");
            boolean addMsgBox = !values.containsKey("msg_box");
            filterUnsupportedKeys(values);
            ContentValues finalValues = new ContentValues(values);
            long timeInMillis = System.currentTimeMillis();
            if (addDate) {
                finalValues.put("date", Long.valueOf(timeInMillis / 1000));
            }
            if (addMsgBox && msgBox != 0) {
                finalValues.put("msg_box", Integer.valueOf(msgBox));
            }
            if (msgBox != 1) {
                finalValues.put("read", (Integer) 1);
            }
            Long threadId = values.getAsLong("thread_id");
            String address = values.getAsString("address");
            if ((threadId == null || threadId.longValue() == 0) && !TextUtils.isEmpty(address)) {
                finalValues.put("thread_id", Long.valueOf(Telephony.Threads.getOrCreateThreadId(getContext(), address)));
            }
            if (ProviderUtil.shouldSetCreator(finalValues, callerUid)) {
                finalValues.put("creator", ProviderUtil.getPackageNamesByUid(getContext(), callerUid));
            }
            long rowId = db.insert(table, null, finalValues);
            if (rowId <= 0) {
                Log.e("MmsProvider", "MmsProvider.insert: failed!");
                return null;
            }
            res = Uri.parse(res + "/" + rowId);
        } else if (table.equals("addr")) {
            ContentValues finalValues2 = new ContentValues(values);
            finalValues2.put("msg_id", uri.getPathSegments().get(0));
            long rowId2 = db.insert(table, null, finalValues2);
            if (rowId2 <= 0) {
                Log.e("MmsProvider", "Failed to insert address");
                return null;
            }
            res = Uri.parse(res + "/addr/" + rowId2);
        } else if (table.equals("part")) {
            ContentValues finalValues3 = new ContentValues(values);
            if (match == 11) {
                finalValues3.put("mid", uri.getPathSegments().get(0));
            }
            String contentType = values.getAsString("ct");
            boolean plainText = false;
            boolean smilText = false;
            if ("text/plain".equals(contentType)) {
                plainText = true;
            } else if ("application/smil".equals(contentType)) {
                smilText = true;
            }
            if (!plainText && !smilText) {
                String contentLocation2 = values.getAsString("cl");
                if (!TextUtils.isEmpty(contentLocation2)) {
                    File f = new File(contentLocation2);
                    contentLocation = "_" + f.getName();
                } else {
                    contentLocation = "";
                }
                String path = getContext().getDir("parts", 0).getPath() + "/PART_" + System.currentTimeMillis() + contentLocation;
                if (DownloadDrmHelper.isDrmConvertNeeded(contentType)) {
                    path = DownloadDrmHelper.modifyDrmFwLockFileExtension(path);
                }
                finalValues3.put("_data", path);
                File partFile = new File(path);
                if (!partFile.exists()) {
                    try {
                        if (!partFile.createNewFile()) {
                            throw new IllegalStateException("Unable to create new partFile: " + path);
                        }
                        FileUtils.setPermissions(path, 438, -1, -1);
                    } catch (IOException e) {
                        Log.e("MmsProvider", "createNewFile", e);
                        throw new IllegalStateException("Unable to create new partFile: " + path);
                    }
                }
            }
            long rowId3 = db.insert(table, null, finalValues3);
            if (rowId3 <= 0) {
                Log.e("MmsProvider", "MmsProvider.insert: failed!");
                return null;
            }
            res = Uri.parse(res + "/part/" + rowId3);
            if (plainText) {
                ContentValues cv = new ContentValues();
                cv.put("_id", Long.valueOf(2 + rowId3));
                cv.put("index_text", values.getAsString("text"));
                cv.put("source_id", Long.valueOf(rowId3));
                cv.put("table_to_use", (Integer) 2);
                db.insert("words", "index_text", cv);
            }
        } else if (table.equals("rate")) {
            long now = values.getAsLong("sent_time").longValue();
            long oneHourAgo = now - 3600000;
            db.delete(table, "sent_time<=" + oneHourAgo, null);
            db.insert(table, null, values);
        } else if (table.equals("drm")) {
            String path2 = getContext().getDir("parts", 0).getPath() + "/PART_" + System.currentTimeMillis();
            ContentValues finalValues4 = new ContentValues(1);
            finalValues4.put("_data", path2);
            File partFile2 = new File(path2);
            if (!partFile2.exists()) {
                try {
                    if (!partFile2.createNewFile()) {
                        throw new IllegalStateException("Unable to create new file: " + path2);
                    }
                } catch (IOException e2) {
                    Log.e("MmsProvider", "createNewFile", e2);
                    throw new IllegalStateException("Unable to create new file: " + path2);
                }
            }
            long rowId4 = db.insert(table, null, finalValues4);
            if (rowId4 <= 0) {
                Log.e("MmsProvider", "MmsProvider.insert: failed!");
                return null;
            }
            res = Uri.parse(res + "/drm/" + rowId4);
        } else {
            throw new AssertionError("Unknown table type: " + table);
        }
        if (notify) {
            notifyChange();
            return res;
        }
        return res;
    }

    private int getMessageBoxByMatch(int match) {
        switch (match) {
            case 2:
            case 3:
                return 1;
            case 4:
            case 5:
                return 2;
            case 6:
            case 7:
                return 3;
            case 8:
            case 9:
                return 4;
            default:
                throw new IllegalArgumentException("bad Arg: " + match);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String table;
        int deletedRows = 0;
        int match = sURLMatcher.match(uri);
        String extraSelection = null;
        boolean notify = false;
        switch (match) {
            case 0:
            case 2:
            case 4:
            case 6:
            case 8:
                notify = true;
                table = "pdu";
                if (match != 0) {
                    int msgBox = getMessageBoxByMatch(match);
                    extraSelection = "msg_box=" + msgBox;
                }
                String finalSelection = concatSelections(selection, extraSelection);
                SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
                if ("pdu".equals(table)) {
                    deletedRows = deleteMessages(getContext(), db, finalSelection, selectionArgs, uri);
                } else if ("part".equals(table)) {
                    deletedRows = deleteParts(db, finalSelection, selectionArgs);
                } else if ("drm".equals(table)) {
                    deletedRows = deleteTempDrmData(db, finalSelection, selectionArgs);
                } else {
                    deletedRows = db.delete(table, finalSelection, selectionArgs);
                }
                if (deletedRows > 0 && notify) {
                    notifyChange();
                }
                return deletedRows;
            case 1:
            case 3:
            case 5:
            case 7:
            case 9:
                notify = true;
                table = "pdu";
                extraSelection = "_id=" + uri.getLastPathSegment();
                String finalSelection2 = concatSelections(selection, extraSelection);
                SQLiteDatabase db2 = this.mOpenHelper.getWritableDatabase();
                if ("pdu".equals(table)) {
                }
                if (deletedRows > 0) {
                    notifyChange();
                }
                return deletedRows;
            case 10:
                table = "part";
                String finalSelection22 = concatSelections(selection, extraSelection);
                SQLiteDatabase db22 = this.mOpenHelper.getWritableDatabase();
                if ("pdu".equals(table)) {
                }
                if (deletedRows > 0) {
                }
                return deletedRows;
            case 11:
                table = "part";
                extraSelection = "mid=" + uri.getPathSegments().get(0);
                String finalSelection222 = concatSelections(selection, extraSelection);
                SQLiteDatabase db222 = this.mOpenHelper.getWritableDatabase();
                if ("pdu".equals(table)) {
                }
                if (deletedRows > 0) {
                }
                return deletedRows;
            case 12:
                table = "part";
                extraSelection = "_id=" + uri.getPathSegments().get(1);
                String finalSelection2222 = concatSelections(selection, extraSelection);
                SQLiteDatabase db2222 = this.mOpenHelper.getWritableDatabase();
                if ("pdu".equals(table)) {
                }
                if (deletedRows > 0) {
                }
                return deletedRows;
            case 13:
                table = "addr";
                extraSelection = "msg_id=" + uri.getPathSegments().get(0);
                String finalSelection22222 = concatSelections(selection, extraSelection);
                SQLiteDatabase db22222 = this.mOpenHelper.getWritableDatabase();
                if ("pdu".equals(table)) {
                }
                if (deletedRows > 0) {
                }
                return deletedRows;
            case 14:
            case 15:
            case 16:
            default:
                Log.w("MmsProvider", "No match for URI '" + uri + "'");
                return deletedRows;
            case 17:
                table = "drm";
                String finalSelection222222 = concatSelections(selection, extraSelection);
                SQLiteDatabase db222222 = this.mOpenHelper.getWritableDatabase();
                if ("pdu".equals(table)) {
                }
                if (deletedRows > 0) {
                }
                return deletedRows;
        }
    }

    static int deleteMessages(Context context, SQLiteDatabase db, String selection, String[] selectionArgs, Uri uri) {
        int count = 0;
        Cursor cursor = db.query("pdu", new String[]{"_id"}, selection, selectionArgs, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() != 0) {
                    while (cursor.moveToNext()) {
                        deleteParts(db, "mid = ?", new String[]{String.valueOf(cursor.getLong(0))});
                    }
                    cursor.close();
                    count = db.delete("pdu", selection, selectionArgs);
                    if (count > 0) {
                        Intent intent = new Intent("android.intent.action.CONTENT_CHANGED");
                        intent.putExtra("deleted_contents", uri);
                        context.sendBroadcast(intent);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return count;
    }

    private static int deleteParts(SQLiteDatabase db, String selection, String[] selectionArgs) {
        return deleteDataRows(db, "part", selection, selectionArgs);
    }

    private static int deleteTempDrmData(SQLiteDatabase db, String selection, String[] selectionArgs) {
        return deleteDataRows(db, "drm", selection, selectionArgs);
    }

    private static int deleteDataRows(SQLiteDatabase db, String table, String selection, String[] selectionArgs) {
        Cursor cursor = db.query(table, new String[]{"_data"}, selection, selectionArgs, null, null, null);
        if (cursor == null) {
            return 0;
        }
        try {
            if (cursor.getCount() == 0) {
                return 0;
            }
            while (cursor.moveToNext()) {
                try {
                    String path = cursor.getString(0);
                    if (path != null) {
                        new File(path).delete();
                    }
                } catch (Throwable ex) {
                    Log.e("MmsProvider", ex.getMessage(), ex);
                }
            }
            cursor.close();
            return db.delete(table, selection, selectionArgs);
        } finally {
            cursor.close();
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String table;
        ContentValues finalValues;
        int count;
        if (values != null && values.containsKey("_data")) {
            return 0;
        }
        int callerUid = Binder.getCallingUid();
        int match = sURLMatcher.match(uri);
        boolean notify = false;
        String msgId = null;
        switch (match) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 9:
                msgId = uri.getLastPathSegment();
            case 0:
            case 2:
            case 4:
            case 6:
            case 8:
                notify = true;
                table = "pdu";
                String extraSelection = null;
                if (!table.equals("pdu")) {
                    filterUnsupportedKeys(values);
                    if (ProviderUtil.shouldRemoveCreator(values, callerUid)) {
                        Log.w("MmsProvider", ProviderUtil.getPackageNamesByUid(getContext(), callerUid) + " tries to update CREATOR");
                        values.remove("creator");
                    }
                    finalValues = new ContentValues(values);
                    if (msgId != null) {
                        extraSelection = "_id=" + msgId;
                    }
                } else if (table.equals("part")) {
                    finalValues = new ContentValues(values);
                    switch (match) {
                        case 11:
                            extraSelection = "mid=" + uri.getPathSegments().get(0);
                            break;
                        case 12:
                            extraSelection = "_id=" + uri.getPathSegments().get(1);
                            break;
                    }
                } else {
                    return 0;
                }
                String finalSelection = concatSelections(selection, extraSelection);
                SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
                count = db.update(table, finalValues, finalSelection, selectionArgs);
                if (!notify && count > 0) {
                    notifyChange();
                    return count;
                }
            case 10:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            default:
                Log.w("MmsProvider", "Update operation for '" + uri + "' not implemented.");
                return 0;
            case 11:
            case 12:
                table = "part";
                String extraSelection2 = null;
                if (!table.equals("pdu")) {
                }
                String finalSelection2 = concatSelections(selection, extraSelection2);
                SQLiteDatabase db2 = this.mOpenHelper.getWritableDatabase();
                count = db2.update(table, finalValues, finalSelection2, selectionArgs);
                return !notify ? count : count;
            case 20:
                String path = getContext().getDir("parts", 0).getPath() + '/' + uri.getPathSegments().get(1);
                FileUtils.setPermissions(path, 420, -1, -1);
                return 0;
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        int match = sURLMatcher.match(uri);
        if (Log.isLoggable("MmsProvider", 2)) {
            Log.d("MmsProvider", "openFile: uri=" + uri + ", mode=" + mode + ", match=" + match);
        }
        if (match != 12) {
            return null;
        }
        return safeOpenFileHelper(uri, mode);
    }

    private ParcelFileDescriptor safeOpenFileHelper(Uri uri, String mode) throws FileNotFoundException {
        Cursor c = query(uri, new String[]{"_data"}, null, null, null);
        int count = c != null ? c.getCount() : 0;
        if (count != 1) {
            if (c != null) {
                c.close();
            }
            if (count == 0) {
                throw new FileNotFoundException("No entry for " + uri);
            }
            throw new FileNotFoundException("Multiple items at " + uri);
        }
        c.moveToFirst();
        int i = c.getColumnIndex("_data");
        String path = i >= 0 ? c.getString(i) : null;
        c.close();
        if (path == null) {
            throw new FileNotFoundException("Column _data not found.");
        }
        File filePath = new File(path);
        try {
            if (!filePath.getCanonicalPath().startsWith(getContext().getDir("parts", 0).getPath())) {
                Log.e("MmsProvider", "openFile: path " + filePath.getCanonicalPath() + " does not start with " + getContext().getDir("parts", 0).getPath());
                return null;
            }
            int modeBits = ParcelFileDescriptor.parseMode(mode);
            return ParcelFileDescriptor.open(filePath, modeBits);
        } catch (IOException e) {
            Log.e("MmsProvider", "openFile: create path failed " + e, e);
            return null;
        }
    }

    private void filterUnsupportedKeys(ContentValues values) {
        values.remove("d_tm_tok");
        values.remove("s_vis");
        values.remove("r_chg");
        values.remove("r_chg_dl_tok");
        values.remove("r_chg_dl");
        values.remove("r_chg_id");
        values.remove("r_chg_sz");
        values.remove("p_s_by");
        values.remove("p_s_d");
        values.remove("store");
        values.remove("mm_st");
        values.remove("mm_flg_tok");
        values.remove("mm_flg");
        values.remove("store_st");
        values.remove("store_st_txt");
        values.remove("stored");
        values.remove("totals");
        values.remove("mb_t");
        values.remove("mb_t_tok");
        values.remove("qt");
        values.remove("mb_qt");
        values.remove("mb_qt_tok");
        values.remove("m_cnt");
        values.remove("start");
        values.remove("d_ind");
        values.remove("e_des");
        values.remove("limit");
        values.remove("r_r_mod");
        values.remove("r_r_mod_txt");
        values.remove("st_txt");
        values.remove("apl_id");
        values.remove("r_apl_id");
        values.remove("aux_apl_id");
        values.remove("drm_c");
        values.remove("adp_a");
        values.remove("repl_id");
        values.remove("cl_id");
        values.remove("cl_st");
        values.remove("_id");
    }

    private void notifyChange() {
        getContext().getContentResolver().notifyChange(Telephony.MmsSms.CONTENT_URI, null, true, -1);
    }

    static {
        sURLMatcher.addURI("mms", null, 0);
        sURLMatcher.addURI("mms", "#", 1);
        sURLMatcher.addURI("mms", "inbox", 2);
        sURLMatcher.addURI("mms", "inbox/#", 3);
        sURLMatcher.addURI("mms", "sent", 4);
        sURLMatcher.addURI("mms", "sent/#", 5);
        sURLMatcher.addURI("mms", "drafts", 6);
        sURLMatcher.addURI("mms", "drafts/#", 7);
        sURLMatcher.addURI("mms", "outbox", 8);
        sURLMatcher.addURI("mms", "outbox/#", 9);
        sURLMatcher.addURI("mms", "part", 10);
        sURLMatcher.addURI("mms", "#/part", 11);
        sURLMatcher.addURI("mms", "part/#", 12);
        sURLMatcher.addURI("mms", "#/addr", 13);
        sURLMatcher.addURI("mms", "rate", 14);
        sURLMatcher.addURI("mms", "report-status/#", 15);
        sURLMatcher.addURI("mms", "report-request/#", 16);
        sURLMatcher.addURI("mms", "drm", 17);
        sURLMatcher.addURI("mms", "drm/#", 18);
        sURLMatcher.addURI("mms", "threads", 19);
        sURLMatcher.addURI("mms", "resetFilePerm/*", 20);
    }

    private static String concatSelections(String selection1, String selection2) {
        if (TextUtils.isEmpty(selection1)) {
            return selection2;
        }
        return TextUtils.isEmpty(selection2) ? selection1 : selection1 + " AND " + selection2;
    }
}
