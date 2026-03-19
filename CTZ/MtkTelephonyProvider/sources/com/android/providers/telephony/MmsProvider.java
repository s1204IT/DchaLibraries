package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Binder;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import com.android.providers.telephony.MmsSmsProvider;
import com.android.providers.telephony.TelephonyBackupAgent;
import com.google.android.mms.pdu.CharacterSets;
import com.google.android.mms.util.DownloadDrmHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MmsProvider extends ContentProvider {
    private static final UriMatcher sURLMatcher;
    private SQLiteOpenHelper mOpenHelper;
    private static boolean notifyUnread = false;
    private static final Set<String> COLUMNS = new HashSet();
    private static final String[] ADDR_PDU_COLUMNS = {"_id", "msg_id", "contact_id", "address", "type", "charset", "pdu_id", "delivery_status", "read_status"};

    static {
        for (int i = 0; i < ADDR_PDU_COLUMNS.length; i++) {
            COLUMNS.add(ADDR_PDU_COLUMNS[i]);
        }
        sURLMatcher = new UriMatcher(-1);
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
        sURLMatcher.addURI("mms", "attachment_size", 21);
        sURLMatcher.addURI("mms", "resetFilePerm/*", 20);
    }

    @Override
    public boolean onCreate() {
        setAppOps(14, 15);
        this.mOpenHelper = MmsSmsDatabaseHelper.getInstanceForCe(getContext());
        TelephonyBackupAgent.DeferredSmsMmsRestoreService.startIfFilesExist(getContext());
        return true;
    }

    public static String getPduTable(boolean z) {
        return z ? "pdu_restricted" : "pdu";
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String str3;
        String str4;
        String pduTable = getPduTable(ProviderUtil.isAccessRestricted(getContext(), getCallingPackage(), Binder.getCallingUid()));
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Mms", "query begin, uri = " + uri + ", selection = " + str);
        int iMatch = sURLMatcher.match(uri);
        switch (iMatch) {
            case 0:
                constructQueryForBox(sQLiteQueryBuilder, 0, pduTable);
                break;
            case 1:
                sQLiteQueryBuilder.setTables(pduTable);
                sQLiteQueryBuilder.appendWhere("_id=" + uri.getPathSegments().get(0));
                break;
            case 2:
                constructQueryForBox(sQLiteQueryBuilder, 1, pduTable);
                break;
            case 3:
            case 5:
            case 7:
            case 9:
                sQLiteQueryBuilder.setTables(pduTable);
                sQLiteQueryBuilder.appendWhere("_id=" + uri.getPathSegments().get(1));
                sQLiteQueryBuilder.appendWhere(" AND msg_box=" + getMessageBoxByMatch(iMatch));
                break;
            case 4:
                constructQueryForBox(sQLiteQueryBuilder, 2, pduTable);
                break;
            case 6:
                constructQueryForBox(sQLiteQueryBuilder, 3, pduTable);
                break;
            case 8:
                constructQueryForBox(sQLiteQueryBuilder, 4, pduTable);
                break;
            case 10:
                sQLiteQueryBuilder.setTables("part");
                break;
            case 11:
                sQLiteQueryBuilder.setTables("part");
                sQLiteQueryBuilder.appendWhere("mid=" + uri.getPathSegments().get(0));
                break;
            case 12:
                sQLiteQueryBuilder.setTables("part");
                sQLiteQueryBuilder.appendWhere("_id=" + uri.getPathSegments().get(1));
                break;
            case 13:
                sQLiteQueryBuilder.setTables("addr");
                sQLiteQueryBuilder.appendWhere("msg_id=" + uri.getPathSegments().get(0));
                break;
            case 14:
                sQLiteQueryBuilder.setTables("rate");
                break;
            case 15:
                SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
                SQLiteQueryBuilder sQLiteQueryBuilder3 = new SQLiteQueryBuilder();
                SQLiteQueryBuilder sQLiteQueryBuilder4 = new SQLiteQueryBuilder();
                sQLiteQueryBuilder2.setTables(" addr inner join ( select _id as pdu_id, ifnull(st,0) as delivery_status,ifnull(read_status,0) as read_status from pdu  where (m_type=134) and (pdu_id in (select _id from pdu where m_id = (select m_id from pdu where _id = " + uri.getLastPathSegment() + ")))) on ( addr.msg_id=pdu_id and addr.type=151)");
                sQLiteQueryBuilder3.setTables(" addr inner join ( select _id as pdu_id, ifnull(st,0) as delivery_status, ifnull(read_status,0) as read_status from pdu  where (m_type=136) and (pdu_id in (select _id from pdu where m_id = (select m_id from pdu where _id = " + uri.getLastPathSegment() + ")))) on ( addr.msg_id=pdu_id and addr.type=137)");
                new String[]{"address", "delivery_status", "read_status"};
                String strBuildUnionQuery = sQLiteQueryBuilder4.buildUnionQuery(new String[]{sQLiteQueryBuilder2.buildUnionSubQuery("status", ADDR_PDU_COLUMNS, COLUMNS, 0, "delivery", null, null, null), sQLiteQueryBuilder3.buildUnionSubQuery("status", ADDR_PDU_COLUMNS, COLUMNS, 0, "readreport", null, null, null)}, null, null);
                Log.d("Mms/Provider/Mms", "unionQuery = " + strBuildUnionQuery);
                sQLiteQueryBuilder.setTables("(" + strBuildUnionQuery + ")");
                break;
            case 16:
                sQLiteQueryBuilder.setTables("addr join " + pduTable + " on " + pduTable + "._id = addr.msg_id");
                StringBuilder sb = new StringBuilder();
                sb.append(pduTable);
                sb.append("._id = ");
                sb.append(uri.getLastPathSegment());
                sQLiteQueryBuilder.appendWhere(sb.toString());
                sQLiteQueryBuilder.appendWhere(" AND addr.type = 151");
                break;
            case 17:
            default:
                Log.e("Mms/Provider/Mms", "query: invalid request: " + uri);
                return null;
            case 18:
                sQLiteQueryBuilder.setTables("drm");
                sQLiteQueryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
                break;
            case 19:
                sQLiteQueryBuilder.setTables(pduTable + " group by thread_id");
                break;
        }
        if (TextUtils.isEmpty(str2)) {
            if (sQLiteQueryBuilder.getTables().equals(pduTable)) {
                str4 = "date DESC";
            } else if (sQLiteQueryBuilder.getTables().equals("part")) {
                str4 = "seq";
            } else {
                str3 = null;
            }
            str3 = str4;
        } else {
            str3 = str2;
        }
        try {
            Cursor cursorQuery = sQLiteQueryBuilder.query(this.mOpenHelper.getReadableDatabase(), strArr, str, strArr2, null, null, str3);
            if (cursorQuery != null) {
                Log.d("Mms/Provider/Mms", "query getReadableDatabase query end cursor count =" + cursorQuery.getCount());
                cursorQuery.setNotificationUri(getContext().getContentResolver(), uri);
            }
            return cursorQuery;
        } catch (SQLiteException e) {
            Log.e("Mms/Provider/Mms", "returning NULL cursor, query: " + uri, e);
            return null;
        }
    }

    private void constructQueryForBox(SQLiteQueryBuilder sQLiteQueryBuilder, int i, String str) {
        sQLiteQueryBuilder.setTables(str);
        if (i != 0) {
            sQLiteQueryBuilder.appendWhere("msg_box=" + i);
        }
    }

    @Override
    public String getType(Uri uri) {
        int iMatch = sURLMatcher.match(uri);
        if (iMatch != 12) {
            switch (iMatch) {
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
                default:
                    return "*/*";
            }
        }
        Cursor cursorQuery = this.mOpenHelper.getReadableDatabase().query("part", new String[]{"ct"}, "_id = ?", new String[]{uri.getLastPathSegment()}, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() == 1 && cursorQuery.moveToFirst()) {
                    return cursorQuery.getString(0);
                }
                Log.e("Mms/Provider/Mms", "cursor.count() != 1: " + uri);
                return "*/*";
            } finally {
                cursorQuery.close();
            }
        }
        Log.e("Mms/Provider/Mms", "cursor == null: " + uri);
        return "*/*";
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        int iIntValue;
        boolean zBooleanValue;
        Uri uriWithAppendedId;
        boolean z;
        boolean z2;
        String str;
        String strModifyDrmFwLockFileExtension;
        ArrayList stringArrayList;
        long jInsert;
        int callingUid = Binder.getCallingUid();
        String callingPackage = getCallingPackage();
        MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Mms", "insert begin, uri = " + uri + ", values = " + contentValues);
        notifyUnread = true;
        int iMatch = sURLMatcher.match(uri);
        String str2 = "pdu";
        if (iMatch == 0) {
            Integer asInteger = contentValues.getAsInteger("msg_box");
            if (asInteger != null) {
                iIntValue = asInteger.intValue();
                zBooleanValue = true;
            } else {
                iIntValue = 1;
                zBooleanValue = true;
            }
        } else if (iMatch == 2) {
            zBooleanValue = contentValues.containsKey("need_notify") ? contentValues.getAsBoolean("need_notify").booleanValue() : true;
            iIntValue = 1;
        } else if (iMatch == 4) {
            zBooleanValue = true;
            iIntValue = 2;
        } else if (iMatch == 6) {
            iIntValue = 3;
            zBooleanValue = true;
        } else if (iMatch != 8) {
            if (iMatch == 11) {
                str2 = "part";
            } else if (iMatch == 17) {
                str2 = "drm";
            } else {
                if (iMatch == 21) {
                    return uri.buildUpon().appendQueryParameter("size", String.valueOf(getAttachmentsSize())).build();
                }
                switch (iMatch) {
                    case 13:
                        str2 = "addr";
                        break;
                    case 14:
                        str2 = "rate";
                        break;
                    default:
                        Log.e("Mms/Provider/Mms", "insert: invalid request: " + uri);
                        return null;
                }
            }
            iIntValue = 0;
            zBooleanValue = false;
        } else {
            zBooleanValue = true;
            iIntValue = 4;
        }
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        Uri uri2 = Telephony.Mms.CONTENT_URI;
        if (contentValues.containsKey("need_notify")) {
            contentValues.remove("need_notify");
        }
        long jInsert2 = 0;
        if (str2.equals("pdu")) {
            boolean z3 = !contentValues.containsKey("date");
            boolean z4 = !contentValues.containsKey("msg_box");
            filterUnsupportedKeys(contentValues);
            ContentValues contentValues2 = new ContentValues(contentValues);
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (z3) {
                contentValues2.put("date", Long.valueOf(jCurrentTimeMillis / 1000));
            }
            if (z4 && iIntValue != 0) {
                contentValues2.put("msg_box", Integer.valueOf(iIntValue));
            }
            if (iIntValue != 1) {
                contentValues2.put("read", (Integer) 1);
            }
            Long asLong = contentValues.getAsLong("thread_id");
            String asString = contentValues.getAsString("address");
            if ((asLong == null || asLong.longValue() == 0) && !TextUtils.isEmpty(asString)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Long lValueOf = Long.valueOf(getOrCreateThreadIdInternal(getContext(), asString));
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    contentValues2.put("thread_id", lValueOf);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
            if (ProviderUtil.shouldSetCreator(contentValues2, callingUid)) {
                contentValues2.put("creator", callingPackage);
            }
            long jInsert3 = writableDatabase.insert(str2, null, contentValues2);
            if (jInsert3 <= 0) {
                Log.e("Mms/Provider/Mms", "MmsProvider.insert: failed!");
                return null;
            }
            uriWithAppendedId = iIntValue == 1 ? ContentUris.withAppendedId(Telephony.Mms.Inbox.CONTENT_URI, jInsert3) : null;
            setThreadStatus(writableDatabase, contentValues, 0);
            uri2 = Uri.parse(uri2 + "/" + jInsert3);
            ContentValues contentValues3 = new ContentValues();
            contentValues3.put("_id", Long.valueOf(33554432 + jInsert3));
            String asString2 = contentValues2.getAsString("sub");
            if (contentValues2.containsKey("sub_cs") && contentValues2.getAsInteger("sub_cs") != null) {
                asString2 = transformSubjectByCharset(asString2, contentValues2.getAsInteger("sub_cs").intValue());
            }
            contentValues3.put("index_text", asString2);
            contentValues3.put("source_id", Long.valueOf(jInsert3));
            contentValues3.put("table_to_use", (Integer) 4);
            writableDatabase.insert("words", "index_text", contentValues3);
        } else {
            if (str2.equals("addr")) {
                ContentValues contentValues4 = new ContentValues(contentValues);
                contentValues4.put("msg_id", uri.getPathSegments().get(0));
                if (contentValues.containsKey("addresses")) {
                    stringArrayList = contentValues.getStringArrayList("addresses");
                    contentValues.remove("addresses");
                } else {
                    stringArrayList = null;
                }
                if (stringArrayList == null || stringArrayList.size() <= 0) {
                    jInsert = writableDatabase.insert(str2, null, contentValues4);
                    if (jInsert <= 0) {
                        Log.e("Mms/Provider/Mms", "Failed to insert address");
                        return null;
                    }
                } else {
                    ContentValues contentValues5 = new ContentValues(4);
                    writableDatabase.beginTransaction();
                    int i = 0;
                    while (i < stringArrayList.size()) {
                        contentValues.clear();
                        contentValues5.put("msg_id", uri.getPathSegments().get(0));
                        int i2 = i + 1;
                        contentValues5.put("address", (String) stringArrayList.get(i));
                        int i3 = i2 + 1;
                        contentValues5.put("charset", (String) stringArrayList.get(i2));
                        contentValues5.put("type", (String) stringArrayList.get(i3));
                        jInsert2 = writableDatabase.insert(str2, null, contentValues5);
                        i = i3 + 1;
                    }
                    writableDatabase.setTransactionSuccessful();
                    writableDatabase.endTransaction();
                    jInsert = jInsert2;
                }
                uri2 = Uri.parse(uri2 + "/addr/" + jInsert);
            } else if (str2.equals("part")) {
                boolean z5 = contentValues != null && contentValues.containsKey("_data");
                ContentValues contentValues6 = new ContentValues(contentValues);
                if (iMatch == 11) {
                    contentValues6.put("mid", uri.getPathSegments().get(0));
                }
                String asString3 = contentValues.getAsString("ct");
                if (!"text/plain".equals(asString3)) {
                    if (!"application/smil".equals(asString3)) {
                        z = false;
                    } else {
                        if (z5) {
                            Log.e("Mms/Provider/Mms", "insert: can't insert application/smil with _data");
                            return null;
                        }
                        z = true;
                    }
                    z2 = false;
                } else {
                    if (z5) {
                        Log.e("Mms/Provider/Mms", "insert: can't insert text/plain with _data");
                        return null;
                    }
                    z = false;
                    z2 = true;
                }
                if (!z2 && !z) {
                    if (!z5) {
                        String asString4 = contentValues.getAsString("cl");
                        if (TextUtils.isEmpty(asString4)) {
                            str = "";
                        } else {
                            str = "_" + new File(asString4).getName();
                        }
                        String str3 = getContext().getDir("parts", 0).getPath() + "/PART_" + System.currentTimeMillis();
                        if (str.contains(".dcf")) {
                            strModifyDrmFwLockFileExtension = str3 + ".dcf";
                        } else if (DownloadDrmHelper.isDrmConvertNeeded(asString3)) {
                            strModifyDrmFwLockFileExtension = str3 + ".fl";
                        } else {
                            strModifyDrmFwLockFileExtension = str3;
                        }
                        if (DownloadDrmHelper.isDrmConvertNeeded(asString3)) {
                            strModifyDrmFwLockFileExtension = DownloadDrmHelper.modifyDrmFwLockFileExtension(strModifyDrmFwLockFileExtension);
                        }
                    } else {
                        if (!"com.android.providers.telephony".equals(callingPackage)) {
                            Log.e("Mms/Provider/Mms", "insert: can't insert _data");
                            return null;
                        }
                        try {
                            strModifyDrmFwLockFileExtension = contentValues.getAsString("_data");
                            String canonicalPath = getContext().getDir("parts", 0).getCanonicalPath();
                            if (!new File(strModifyDrmFwLockFileExtension).getCanonicalPath().startsWith(canonicalPath)) {
                                Log.e("Mms/Provider/Mms", "insert: path " + strModifyDrmFwLockFileExtension + " does not start with " + canonicalPath);
                                return null;
                            }
                        } catch (IOException e) {
                            Log.e("Mms/Provider/Mms", "insert part: create path failed " + e, e);
                            return null;
                        }
                    }
                    contentValues6.put("_data", strModifyDrmFwLockFileExtension);
                    File file = new File(strModifyDrmFwLockFileExtension);
                    if (!file.exists()) {
                        try {
                            if (!file.createNewFile()) {
                                throw new IllegalStateException("Unable to create new partFile: " + strModifyDrmFwLockFileExtension);
                            }
                            FileUtils.setPermissions(strModifyDrmFwLockFileExtension, 438, -1, -1);
                        } catch (IOException e2) {
                            Log.e("Mms/Provider/Mms", "createNewFile", e2);
                            throw new IllegalStateException("Unable to create new partFile: " + strModifyDrmFwLockFileExtension);
                        }
                    }
                }
                long jInsert4 = writableDatabase.insert(str2, null, contentValues6);
                if (jInsert4 <= 0) {
                    Log.e("Mms/Provider/Mms", "MmsProvider.insert: failed!");
                    return null;
                }
                uri2 = Uri.parse(uri2 + "/part/" + jInsert4);
                if (z2) {
                    ContentValues contentValues7 = new ContentValues();
                    contentValues7.put("_id", Long.valueOf(8589934592L + jInsert4));
                    contentValues7.put("index_text", contentValues.getAsString("text"));
                    contentValues7.put("source_id", Long.valueOf(jInsert4));
                    contentValues7.put("table_to_use", (Integer) 2);
                    writableDatabase.insert("words", "index_text", contentValues7);
                }
            } else if (str2.equals("rate")) {
                writableDatabase.delete(str2, "sent_time<=" + (contentValues.getAsLong("sent_time").longValue() - 3600000), null);
                writableDatabase.insert(str2, null, contentValues);
            } else {
                if (!str2.equals("drm")) {
                    throw new AssertionError("Unknown table type: " + str2);
                }
                String str4 = getContext().getDir("parts", 0).getPath() + "/PART_" + System.currentTimeMillis();
                ContentValues contentValues8 = new ContentValues(1);
                contentValues8.put("_data", str4);
                File file2 = new File(str4);
                if (!file2.exists()) {
                    try {
                        if (!file2.createNewFile()) {
                            throw new IllegalStateException("Unable to create new file: " + str4);
                        }
                    } catch (IOException e3) {
                        Log.e("Mms/Provider/Mms", "createNewFile", e3);
                        throw new IllegalStateException("Unable to create new file: " + str4);
                    }
                }
                long jInsert5 = writableDatabase.insert(str2, null, contentValues8);
                if (jInsert5 <= 0) {
                    Log.e("Mms/Provider/Mms", "MmsProvider.insert: failed!");
                    return null;
                }
                uri2 = Uri.parse(uri2 + "/drm/" + jInsert5);
                uriWithAppendedId = null;
            }
            uriWithAppendedId = null;
        }
        if (zBooleanValue) {
            Log.d("Mms/Provider/Mms", "insert getWritebleDatabase notify");
            notifyUnread = false;
            notifyChange(uri, uriWithAppendedId);
        }
        Log.d("Mms/Provider/Mms", "insert succeed, uri = " + uri2);
        return uri2;
    }

    private int getMessageBoxByMatch(int i) {
        switch (i) {
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
                throw new IllegalArgumentException("bad Arg: " + i);
        }
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        String str2;
        boolean z;
        String[] strArr2;
        String str3;
        int iDeleteMessages;
        String str4;
        String str5;
        int iMatch = sURLMatcher.match(uri);
        Log.d("Mms/Provider/Mms", "delete begin, uri = " + uri + ", selection = " + str);
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        if (iMatch != 17) {
            switch (iMatch) {
                case 0:
                    if (str != null && str.equals("ForMultiDelete")) {
                        str = "_id IN " + SmsProvider.getSmsIdsFromArgs(strArr);
                        strArr = null;
                    }
                    str4 = "pdu";
                    if (iMatch != 0) {
                        strArr2 = strArr;
                        str2 = "pdu";
                        z = true;
                        str3 = null;
                        String strConcatSelections = concatSelections(str, str3);
                        if (!"pdu".equals(str2)) {
                        }
                        if (iDeleteMessages > 0) {
                            notifyChange(uri, null);
                        }
                        Log.d("Mms/Provider/Mms", "delete end, affectedRows = " + iDeleteMessages);
                        return iDeleteMessages;
                    }
                    str5 = "msg_box=" + getMessageBoxByMatch(iMatch);
                    strArr2 = strArr;
                    z = true;
                    String str6 = str4;
                    str3 = str5;
                    str2 = str6;
                    String strConcatSelections2 = concatSelections(str, str3);
                    iDeleteMessages = !"pdu".equals(str2) ? deleteMessages(getContext(), writableDatabase, strConcatSelections2, strArr2, uri, true) : "part".equals(str2) ? deleteParts(writableDatabase, strConcatSelections2, strArr2) : "drm".equals(str2) ? deleteTempDrmData(writableDatabase, strConcatSelections2, strArr2) : writableDatabase.delete(str2, strConcatSelections2, strArr2);
                    if (iDeleteMessages > 0 && z) {
                        notifyChange(uri, null);
                    }
                    Log.d("Mms/Provider/Mms", "delete end, affectedRows = " + iDeleteMessages);
                    return iDeleteMessages;
                case 1:
                case 3:
                case 5:
                case 7:
                case 9:
                    str4 = "pdu";
                    String str7 = "_id=" + uri.getLastPathSegment();
                    if (iMatch == 1) {
                        strArr2 = strArr;
                        str2 = "pdu";
                        z = true;
                        str3 = str7;
                        String strConcatSelections22 = concatSelections(str, str3);
                        if (!"pdu".equals(str2)) {
                        }
                        if (iDeleteMessages > 0) {
                        }
                        Log.d("Mms/Provider/Mms", "delete end, affectedRows = " + iDeleteMessages);
                        return iDeleteMessages;
                    }
                    str5 = str7 + " AND msg_box=" + getMessageBoxByMatch(iMatch);
                    strArr2 = strArr;
                    z = true;
                    String str62 = str4;
                    str3 = str5;
                    str2 = str62;
                    String strConcatSelections222 = concatSelections(str, str3);
                    if (!"pdu".equals(str2)) {
                    }
                    if (iDeleteMessages > 0) {
                    }
                    Log.d("Mms/Provider/Mms", "delete end, affectedRows = " + iDeleteMessages);
                    return iDeleteMessages;
                case 2:
                case 4:
                case 6:
                case 8:
                    str4 = "pdu";
                    if (iMatch != 0) {
                    }
                    break;
                case 10:
                    str2 = "part";
                    break;
                case 11:
                    str2 = "part";
                    str3 = "mid=" + uri.getPathSegments().get(0);
                    strArr2 = strArr;
                    z = false;
                    String strConcatSelections2222 = concatSelections(str, str3);
                    if (!"pdu".equals(str2)) {
                    }
                    if (iDeleteMessages > 0) {
                    }
                    Log.d("Mms/Provider/Mms", "delete end, affectedRows = " + iDeleteMessages);
                    return iDeleteMessages;
                case 12:
                    str2 = "part";
                    str3 = "_id=" + uri.getPathSegments().get(1);
                    strArr2 = strArr;
                    z = false;
                    String strConcatSelections22222 = concatSelections(str, str3);
                    if (!"pdu".equals(str2)) {
                    }
                    if (iDeleteMessages > 0) {
                    }
                    Log.d("Mms/Provider/Mms", "delete end, affectedRows = " + iDeleteMessages);
                    return iDeleteMessages;
                case 13:
                    str2 = "addr";
                    str3 = "msg_id=" + uri.getPathSegments().get(0);
                    strArr2 = strArr;
                    z = false;
                    String strConcatSelections222222 = concatSelections(str, str3);
                    if (!"pdu".equals(str2)) {
                    }
                    if (iDeleteMessages > 0) {
                    }
                    Log.d("Mms/Provider/Mms", "delete end, affectedRows = " + iDeleteMessages);
                    return iDeleteMessages;
                default:
                    Log.w("Mms/Provider/Mms", "No match for URI '" + uri + "'");
                    return 0;
            }
        } else {
            str2 = "drm";
        }
        strArr2 = strArr;
        z = false;
        str3 = null;
        String strConcatSelections2222222 = concatSelections(str, str3);
        if (!"pdu".equals(str2)) {
        }
        if (iDeleteMessages > 0) {
        }
        Log.d("Mms/Provider/Mms", "delete end, affectedRows = " + iDeleteMessages);
        return iDeleteMessages;
    }

    static int deleteMessages(Context context, SQLiteDatabase sQLiteDatabase, String str, String[] strArr, Uri uri, boolean z) {
        int iDelete;
        String str2;
        Log.d("Mms/Provider/Mms", "deleteMessages, start");
        Cursor cursorQuery = sQLiteDatabase.query("pdu", new String[]{"_id", "thread_id"}, str, strArr, null, null, null);
        int i = 0;
        if (cursorQuery == null) {
            return 0;
        }
        int count = cursorQuery.getCount();
        HashSet hashSet = new HashSet();
        try {
            if (cursorQuery.getCount() == 0) {
                return 0;
            }
            HashSet hashSet2 = new HashSet();
            loop0: while (true) {
                int i2 = 0;
                while (cursorQuery.moveToNext()) {
                    Long lValueOf = Long.valueOf(cursorQuery.getLong(0));
                    long j = cursorQuery.getLong(1);
                    hashSet2.add(lValueOf);
                    if (j > 0) {
                        hashSet.add(Long.valueOf(j));
                    }
                    i2++;
                    if (i2 % 50 <= 0 || cursorQuery.isLast()) {
                        break;
                    }
                }
                String str3 = "mid" + formatInClause(hashSet2);
                Log.d("Mms/Provider/Mms", "deleteMessages, delete parts where " + str3);
                deleteParts(sQLiteDatabase, str3, null);
                hashSet2.clear();
            }
            cursorQuery.close();
            Log.d("Mms/Provider/Mms", "deleteMessages, delete all parts end");
            deleteWordsBySelection(sQLiteDatabase, str, strArr);
            int iDelete2 = 100;
            if (count > 100) {
                if (TextUtils.isEmpty(str)) {
                    str2 = "_id in (select _id from pdu limit 100)";
                } else {
                    str2 = "_id in (select _id from pdu where " + str + " limit 100)";
                }
                iDelete = 0;
                while (iDelete2 > 0) {
                    iDelete2 = sQLiteDatabase.delete("pdu", str2, strArr);
                    iDelete += iDelete2;
                    Log.d("Mms/Provider/Mms", "deleteMessages, delete " + iDelete2 + " pdu");
                }
            } else {
                iDelete = sQLiteDatabase.delete("pdu", str, strArr);
            }
            Log.d("Mms/Provider/Mms", "deleteMessages, delete pdu end");
            if (iDelete > 0) {
                Intent intent = new Intent("android.intent.action.CONTENT_CHANGED");
                intent.putExtra("deleted_contents", uri);
                context.sendBroadcast(intent);
                if (z) {
                    if (hashSet.size() <= 2) {
                        Iterator it = hashSet.iterator();
                        while (it.hasNext()) {
                            MmsSmsDatabaseHelper.updateThread(sQLiteDatabase, ((Long) it.next()).longValue());
                        }
                    } else {
                        long[] jArr = new long[hashSet.size()];
                        Iterator it2 = hashSet.iterator();
                        while (it2.hasNext()) {
                            jArr[i] = ((Long) it2.next()).longValue();
                            i++;
                        }
                        MmsSmsDatabaseHelper.updateMultiThreads(sQLiteDatabase, jArr);
                    }
                }
            }
            return iDelete;
        } finally {
            cursorQuery.close();
        }
    }

    private static int deleteParts(SQLiteDatabase sQLiteDatabase, String str, String[] strArr) {
        return deleteDataRows(sQLiteDatabase, "part", str, strArr);
    }

    private static int deleteTempDrmData(SQLiteDatabase sQLiteDatabase, String str, String[] strArr) {
        return deleteDataRows(sQLiteDatabase, "drm", str, strArr);
    }

    private static int deleteDataRows(SQLiteDatabase sQLiteDatabase, String str, String str2, String[] strArr) {
        Cursor cursorQuery = sQLiteDatabase.query(str, new String[]{"_data"}, str2, strArr, null, null, null);
        if (cursorQuery == null) {
            return 0;
        }
        try {
            if (cursorQuery.getCount() == 0) {
                return 0;
            }
            while (cursorQuery.moveToNext()) {
                try {
                    String string = cursorQuery.getString(0);
                    if (string != null) {
                        new File(string).delete();
                    }
                } catch (Throwable th) {
                    Log.e("Mms/Provider/Mms", th.getMessage(), th);
                }
            }
            cursorQuery.close();
            return sQLiteDatabase.delete(str, str2, strArr);
        } finally {
            cursorQuery.close();
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        String lastPathSegment;
        String str2;
        boolean z;
        ContentValues contentValues2;
        String str3;
        SQLiteDatabase writableDatabase;
        int iUpdate;
        ContentValues contentValues3;
        boolean zBooleanValue;
        if (contentValues != null && contentValues.containsKey("_data")) {
            return 0;
        }
        int callingUid = Binder.getCallingUid();
        String callingPackage = getCallingPackage();
        int iMatch = sURLMatcher.match(uri);
        MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Mms", "update begin, uri = " + uri + ", values = " + contentValues + ", selection = " + str);
        long j = -1;
        if (iMatch != 20) {
            switch (iMatch) {
                case 0:
                case 2:
                case 4:
                case 6:
                case 8:
                    lastPathSegment = null;
                    str2 = "pdu";
                    z = true;
                    if (contentValues.containsKey("need_notify")) {
                        contentValues.remove("need_notify");
                    }
                    if (str2.equals("pdu")) {
                        filterUnsupportedKeys(contentValues);
                        if (ProviderUtil.shouldRemoveCreator(contentValues, callingUid)) {
                            Log.w("Mms/Provider/Mms", callingPackage + " tries to update CREATOR");
                            contentValues.remove("creator");
                        }
                        contentValues2 = new ContentValues(contentValues);
                        if (lastPathSegment != null) {
                            String str4 = "_id=" + lastPathSegment;
                            SQLiteDatabase writableDatabase2 = this.mOpenHelper.getWritableDatabase();
                            Cursor cursorQuery = writableDatabase2.query(str2, new String[]{"thread_id", "_id"}, str4, null, null, null, null);
                            if (cursorQuery != null) {
                                try {
                                    if (cursorQuery.getCount() > 0 && cursorQuery.moveToFirst()) {
                                        j = cursorQuery.getLong(0);
                                        int i = cursorQuery.getInt(cursorQuery.getColumnIndex("_id"));
                                        if (contentValues2.containsKey("sub")) {
                                            ContentValues contentValues4 = new ContentValues();
                                            long j2 = ((long) i) + 33554432;
                                            contentValues4.put("_id", Long.valueOf(j2));
                                            contentValues3 = contentValues2;
                                            String asString = contentValues3.getAsString("sub");
                                            if (contentValues3.containsKey("sub_cs") && contentValues3.getAsInteger("sub_cs") != null) {
                                                asString = transformSubjectByCharset(asString, contentValues3.getAsInteger("sub_cs").intValue());
                                            }
                                            contentValues4.put("index_text", asString);
                                            contentValues4.put("source_id", Integer.valueOf(i));
                                            contentValues4.put("table_to_use", (Integer) 4);
                                            writableDatabase2.update("words", contentValues4, "_id = " + j2 + " and table_to_use = 4", null);
                                            break;
                                        }
                                        cursorQuery.close();
                                        str3 = str4;
                                        contentValues2 = contentValues3;
                                    } else {
                                        contentValues3 = contentValues2;
                                        cursorQuery.close();
                                        str3 = str4;
                                        contentValues2 = contentValues3;
                                    }
                                } catch (Throwable th) {
                                    cursorQuery.close();
                                    throw th;
                                }
                            }
                        } else {
                            str3 = null;
                        }
                    } else if (str2.equals("part")) {
                        contentValues2 = new ContentValues(contentValues);
                        switch (iMatch) {
                            case 11:
                                str3 = "mid=" + uri.getPathSegments().get(0);
                                break;
                            case 12:
                                str3 = "_id=" + uri.getPathSegments().get(1);
                                break;
                            default:
                                str3 = null;
                                break;
                        }
                    } else {
                        return 0;
                    }
                    String strConcatSelections = concatSelections(str, str3);
                    writableDatabase = this.mOpenHelper.getWritableDatabase();
                    iUpdate = writableDatabase.update(str2, contentValues2, strConcatSelections, strArr);
                    if (z && iUpdate > 0) {
                        notifyUnread = false;
                        notifyChange(uri, null);
                    }
                    if (iUpdate > 0 && str2.equals("pdu") && contentValues2.containsKey("thread_id") && contentValues2.getAsLong("thread_id").longValue() != j) {
                        MmsSmsDatabaseHelper.updateThread(writableDatabase, j);
                    }
                    Log.d("Mms/Provider/Mms", "update end, affectedRows = " + iUpdate);
                    return iUpdate;
                case 1:
                case 3:
                case 5:
                case 7:
                case 9:
                    lastPathSegment = uri.getLastPathSegment();
                    str2 = "pdu";
                    z = true;
                    if (contentValues.containsKey("need_notify")) {
                    }
                    if (str2.equals("pdu")) {
                    }
                    String strConcatSelections2 = concatSelections(str, str3);
                    writableDatabase = this.mOpenHelper.getWritableDatabase();
                    iUpdate = writableDatabase.update(str2, contentValues2, strConcatSelections2, strArr);
                    if (z) {
                        notifyUnread = false;
                        notifyChange(uri, null);
                    }
                    if (iUpdate > 0) {
                        MmsSmsDatabaseHelper.updateThread(writableDatabase, j);
                    }
                    Log.d("Mms/Provider/Mms", "update end, affectedRows = " + iUpdate);
                    return iUpdate;
                default:
                    switch (iMatch) {
                        case 11:
                        case 12:
                            if (contentValues.containsKey("need_notify")) {
                                zBooleanValue = contentValues.getAsBoolean("need_notify").booleanValue();
                            } else {
                                zBooleanValue = false;
                            }
                            str2 = "part";
                            z = zBooleanValue;
                            lastPathSegment = null;
                            break;
                        default:
                            Log.w("Mms/Provider/Mms", "Update operation for '" + uri + "' not implemented.");
                            return 0;
                    }
                    if (contentValues.containsKey("need_notify")) {
                    }
                    if (str2.equals("pdu")) {
                    }
                    String strConcatSelections22 = concatSelections(str, str3);
                    writableDatabase = this.mOpenHelper.getWritableDatabase();
                    iUpdate = writableDatabase.update(str2, contentValues2, strConcatSelections22, strArr);
                    if (z) {
                    }
                    if (iUpdate > 0) {
                    }
                    Log.d("Mms/Provider/Mms", "update end, affectedRows = " + iUpdate);
                    return iUpdate;
            }
        }
        FileUtils.setPermissions(getContext().getDir("parts", 0).getPath() + '/' + uri.getPathSegments().get(1), 420, -1, -1);
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        int iMatch = sURLMatcher.match(uri);
        if (Log.isLoggable("Mms/Provider/Mms", 2)) {
            MmsSmsProvider.MmsProviderLog.dpi("Mms/Provider/Mms", "openFile: uri=" + uri + ", mode=" + str + ", match=" + iMatch);
        }
        if (iMatch != 12) {
            Log.v("Mms/Provider/Mms", "openFile openFile return null");
            return null;
        }
        return safeOpenFileHelper(uri, str);
    }

    private ParcelFileDescriptor safeOpenFileHelper(Uri uri, String str) throws FileNotFoundException {
        Cursor cursorQuery = query(uri, new String[]{"_data"}, null, null, null);
        int count = cursorQuery != null ? cursorQuery.getCount() : 0;
        if (count != 1) {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            if (count == 0) {
                Log.v("Mms/Provider/Mms", "openfile FileNotFoundException(No entry for)");
                throw new FileNotFoundException("No entry for " + uri);
            }
            Log.v("Mms/Provider/Mms", "openfile FileNotFoundException(Multiple items at)");
            throw new FileNotFoundException("Multiple items at " + uri);
        }
        cursorQuery.moveToFirst();
        int columnIndex = cursorQuery.getColumnIndex("_data");
        String string = columnIndex >= 0 ? cursorQuery.getString(columnIndex) : null;
        cursorQuery.close();
        if (string == null) {
            MmsSmsProvider.MmsProviderLog.vpi("Mms/Provider/Mms", "openfile path == null " + string);
        }
        File file = new File(string);
        try {
            if (!file.getCanonicalPath().startsWith(getContext().getDir("parts", 0).getCanonicalPath())) {
                MmsSmsProvider.MmsProviderLog.epi("Mms/Provider/Mms", "openFile: path " + file.getCanonicalPath() + " does not start with " + getContext().getDir("parts", 0).getCanonicalPath());
                Log.v("Mms/Provider/Mms", "openfile !filePath.getCanonicalPath().startsWith()");
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(str));
        } catch (IOException e) {
            Log.e("Mms/Provider/Mms", "openFile: create path failed " + e, e);
            return null;
        }
    }

    private void filterUnsupportedKeys(ContentValues contentValues) {
        contentValues.remove("d_tm_tok");
        contentValues.remove("s_vis");
        contentValues.remove("r_chg");
        contentValues.remove("r_chg_dl_tok");
        contentValues.remove("r_chg_dl");
        contentValues.remove("r_chg_id");
        contentValues.remove("r_chg_sz");
        contentValues.remove("p_s_by");
        contentValues.remove("p_s_d");
        contentValues.remove("store");
        contentValues.remove("mm_st");
        contentValues.remove("mm_flg_tok");
        contentValues.remove("mm_flg");
        contentValues.remove("store_st");
        contentValues.remove("store_st_txt");
        contentValues.remove("stored");
        contentValues.remove("totals");
        contentValues.remove("mb_t");
        contentValues.remove("mb_t_tok");
        contentValues.remove("qt");
        contentValues.remove("mb_qt");
        contentValues.remove("mb_qt_tok");
        contentValues.remove("m_cnt");
        contentValues.remove("start");
        contentValues.remove("d_ind");
        contentValues.remove("e_des");
        contentValues.remove("limit");
        contentValues.remove("r_r_mod");
        contentValues.remove("r_r_mod_txt");
        contentValues.remove("st_txt");
        contentValues.remove("apl_id");
        contentValues.remove("r_apl_id");
        contentValues.remove("aux_apl_id");
        contentValues.remove("drm_c");
        contentValues.remove("adp_a");
        contentValues.remove("repl_id");
        contentValues.remove("cl_id");
        contentValues.remove("cl_st");
        contentValues.remove("_id");
    }

    private void notifyChange(Uri uri, Uri uri2) {
        Context context = getContext();
        if (uri2 != null) {
            context.getContentResolver().notifyChange(uri2, null, true, -1);
        }
        context.getContentResolver().notifyChange(Telephony.MmsSms.CONTENT_URI, null, true, -1);
        context.getContentResolver().notifyChange(uri, null);
        if (!notifyUnread) {
            Log.d("Mms/Provider/Mms", "notifyChange, notify unread change");
            MmsSmsProvider.notifyUnreadMessageNumberChanged(getContext());
        }
        if (uri2 != null) {
            uri = uri2;
        }
        ProviderUtil.notifyIfNotDefaultSmsApp(uri, getCallingPackage(), context);
    }

    private static String concatSelections(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            return str2;
        }
        if (TextUtils.isEmpty(str2)) {
            return str;
        }
        return str + " AND " + str2;
    }

    private long getAttachmentsSize() {
        String[] strArr = {"_data"};
        Uri uri = Uri.parse("content://mms/part/");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Cursor cursorQuery = getContext().getContentResolver().query(uri, strArr, null, null, null);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            long length = 0;
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        Log.d("Mms/Provider/Mms", "getAttachmentsSize, count " + cursorQuery.getCount());
                        do {
                            String string = cursorQuery.getString(0);
                            if (string != null) {
                                File file = new File(string);
                                if (file.exists()) {
                                    length += file.length();
                                }
                            }
                        } while (cursorQuery.moveToNext());
                        return length;
                    }
                } finally {
                    Log.d("Mms/Provider/Mms", "getAttachmentsSize size = 0");
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            }
            Log.e("Mms/Provider/Mms", "getAttachmentsSize, cursor is empty or null");
            Log.d("Mms/Provider/Mms", "getAttachmentsSize size = 0");
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return 0L;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private static String formatInClause(Set<Long> set) {
        if (set == null || set.size() == 0) {
            return " IN ()";
        }
        return (" IN " + set.toString()).replace('[', '(').replace(']', ')');
    }

    private void setThreadStatus(SQLiteDatabase sQLiteDatabase, ContentValues contentValues, int i) {
        ContentValues contentValues2 = new ContentValues(1);
        contentValues2.put("status", Integer.valueOf(i));
        sQLiteDatabase.update("threads", contentValues2, "_id=" + contentValues.getAsLong("thread_id"), null);
    }

    private String transformSubjectByCharset(String str, int i) {
        if (str == null || str.equals("")) {
            return "";
        }
        try {
            return new String(str.getBytes("iso-8859-1"), CharacterSets.getMimeName(i));
        } catch (UnsupportedEncodingException e) {
            Log.e("Mms/Provider/Mms", "transformSubjectByCharset UnsupportedEncodingException");
            return str;
        }
    }

    private static int deleteWordsBySelection(SQLiteDatabase sQLiteDatabase, String str, String[] strArr) {
        String str2 = "";
        if (str != null && !str.equals("")) {
            str2 = " where " + str;
        }
        return sQLiteDatabase.delete("words", "source_id in (select _id from pdu" + str2 + ") and table_to_use = 4", strArr);
    }

    public static long getOrCreateThreadIdInternal(Context context, String str) {
        Uri.Builder builderBuildUpon = Uri.parse("content://mms-sms/threadID").buildUpon();
        if (Telephony.Mms.isEmailAddress(str)) {
            str = Telephony.Mms.extractAddrSpec(str);
        }
        builderBuildUpon.appendQueryParameter("recipient", str);
        Uri uriBuild = builderBuildUpon.build();
        Cursor cursorQuery = SqliteWrapper.query(context, context.getContentResolver(), uriBuild, new String[]{"_id"}, (String) null, (String[]) null, (String) null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    return cursorQuery.getLong(0);
                }
                Log.e("Mms/Provider/Mms", "getOrCreateThreadId returned no rows!");
            } finally {
                cursorQuery.close();
            }
        }
        Log.e("Mms/Provider/Mms", "getOrCreateThreadId failed with uri " + uriBuild.toString());
        throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
    }
}
