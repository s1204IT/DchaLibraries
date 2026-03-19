package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.android.providers.telephony.TelephonyBackupAgent;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MmsSmsProvider extends ContentProvider {
    private static final String THREAD_QUERY;
    private static boolean piLoggable;
    private SQLiteOpenHelper mOpenHelper;
    private boolean mUseStrictPhoneNumberComparation;
    private static final UriMatcher URI_MATCHER = new UriMatcher(-1);
    private static final Uri PICK_PHONE_EMAIL_URI = Uri.parse("content://com.android.contacts/data/phone_email");
    public static final Uri PICK_PHONE_EMAIL_FILTER_URI = Uri.withAppendedPath(PICK_PHONE_EMAIL_URI, "filter");
    private static final boolean MTK_WAPPUSH_SUPPORT = SystemProperties.get("ro.vendor.mtk_wappush_support").equals("1");
    private static final String[] MMS_SMS_COLUMNS = {"_id", "date", "date_sent", "read", "thread_id", "locked", "sub_id"};
    private static final String[] MMS_ONLY_COLUMNS = {"ct_cls", "ct_l", "ct_t", "d_rpt", "exp", "m_cls", "m_id", "m_size", "m_type", "msg_box", "pri", "read_status", "resp_st", "resp_txt", "retr_st", "retr_txt_cs", "rpt_a", "rr", "st", "sub", "sub_cs", "tr_id", "v", "service_center", "text_only"};
    private static final String[] THREAD_SETTINGS_COLUMNS = {"_id", "spam", "notification_enable", "mute", "mute_start", "ringtone", "_data", "vibrate"};
    private static final String[] SMS_ONLY_COLUMNS = {"address", "body", "person", "reply_path_present", "service_center", "status", "subject", "type", "error_code", "ipmsg_id"};
    private static final String[] CB_ONLY_COLUMNS = {"channel_id"};
    private static final String[] THREADS_COLUMNS = {"_id", "date", "recipient_ids", "message_count"};
    private static final String[] CANONICAL_ADDRESSES_COLUMNS_1 = {"address"};
    private static final String[] CANONICAL_ADDRESSES_COLUMNS_2 = {"_id", "address"};
    private static final String[] UNION_COLUMNS = new String[(MMS_SMS_COLUMNS.length + MMS_ONLY_COLUMNS.length) + SMS_ONLY_COLUMNS.length];
    private static final String[] CONVERSATION_SIM_ID_AND_MSG_COUNT = {"sms_number", "sms_unread", "mms_number", "mms_unread", "sim_indicator"};
    public static final Set<String> MMS_COLUMNS = new HashSet();
    public static final Set<String> SMS_COLUMNS = new HashSet();
    public static final Set<String> CB_COLUMNS = new HashSet();
    private static final String[] ID_PROJECTION = {"_id"};
    private static final String[] STATUS_PROJECTION = {"status"};
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    static {
        URI_MATCHER.addURI("mms-sms", "conversations", 0);
        URI_MATCHER.addURI("mms-sms", "conversations/extend", 33);
        URI_MATCHER.addURI("mms-sms", "complete-conversations", 7);
        URI_MATCHER.addURI("mms-sms", "conversations/#", 1);
        URI_MATCHER.addURI("mms-sms", "conversations/#/recipients", 2);
        URI_MATCHER.addURI("mms-sms", "conversations/#/subject", 9);
        URI_MATCHER.addURI("mms-sms", "conversations/obsolete", 11);
        URI_MATCHER.addURI("mms-sms", "conversations/obsolete/#", 41);
        URI_MATCHER.addURI("mms-sms", "messages/byphone/*", 3);
        URI_MATCHER.addURI("mms-sms", "threadID", 4);
        URI_MATCHER.addURI("mms-sms", "canonical-address/#", 5);
        URI_MATCHER.addURI("mms-sms", "canonical-addresses", 13);
        URI_MATCHER.addURI("mms-sms", "search", 14);
        URI_MATCHER.addURI("mms-sms", "searchSuggest", 15);
        URI_MATCHER.addURI("mms-sms", "search_suggest_shortcut/#", 37);
        URI_MATCHER.addURI("mms-sms", "searchAdvanced", 42);
        URI_MATCHER.addURI("mms-sms", "pending", 6);
        URI_MATCHER.addURI("mms-sms", "undelivered", 8);
        URI_MATCHER.addURI("mms-sms", "notifications", 10);
        URI_MATCHER.addURI("mms-sms", "draft", 12);
        URI_MATCHER.addURI("mms-sms", "locked", 16);
        URI_MATCHER.addURI("mms-sms", "locked/#", 17);
        URI_MATCHER.addURI("mms-sms", "quicktext", 19);
        URI_MATCHER.addURI("mms-sms", "cellbroadcast", 27);
        URI_MATCHER.addURI("mms-sms", "conversations/status/#", 26);
        URI_MATCHER.addURI("mms-sms", "messageIdToThread", 18);
        URI_MATCHER.addURI("mms-sms", "thread_id/#", 24);
        URI_MATCHER.addURI("mms-sms", "unread_count", 28);
        URI_MATCHER.addURI("mms-sms", "simid_list/#", 29);
        URI_MATCHER.addURI("mms-sms", "thread_settings", 31);
        URI_MATCHER.addURI("mms-sms", "thread_settings/#", 32);
        URI_MATCHER.addURI("mms-sms", "conversations_distinct/#", 34);
        URI_MATCHER.addURI("mms-sms", "conversations/history", 35);
        URI_MATCHER.addURI("mms-sms", "conversations/recent", 36);
        URI_MATCHER.addURI("mms-sms", "conversations/simid/#", 38);
        URI_MATCHER.addURI("mms-sms", "widget/thread/#", 40);
        URI_MATCHER.addURI("mms-sms", "conversations/unread", 41);
        URI_MATCHER.addURI("mms-sms", "conversations/group/unread_count", 44);
        URI_MATCHER.addURI("mms-sms", "conversations/map", 43);
        URI_MATCHER.addURI("mms-sms", "database_size", 44);
        initializeColumnSets();
        piLoggable = !"user".equals(SystemProperties.get("ro.build.type", "user"));
        if (MTK_WAPPUSH_SUPPORT) {
            THREAD_QUERY = "SELECT _id FROM threads WHERE type<>2 AND type<>3 AND recipient_ids=? AND status=0";
        } else {
            THREAD_QUERY = "SELECT _id FROM threads WHERE type<>3 AND recipient_ids=? AND status=0";
        }
    }

    @Override
    public boolean onCreate() {
        setAppOps(14, 15);
        this.mOpenHelper = MmsSmsDatabaseHelper.getInstanceForCe(getContext());
        this.mUseStrictPhoneNumberComparation = getContext().getResources().getBoolean(android.R.^attr-private.pointerIconWait);
        TelephonyBackupAgent.DeferredSmsMmsRestoreService.startIfFilesExist(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) throws Throwable {
        Cursor firstLockedMessage;
        ?? threadCache;
        ?? threadCache2;
        Cursor cBThreadId;
        Cursor cursorQuery;
        long j;
        long j2;
        String str3 = str2;
        boolean zIsAccessRestricted = ProviderUtil.isAccessRestricted(getContext(), getCallingPackage(), Binder.getCallingUid());
        String pduTable = MmsProvider.getPduTable(zIsAccessRestricted);
        String smsTable = SmsProvider.getSmsTable(zIsAccessRestricted);
        SQLiteDatabase readableDatabase = this.mOpenHelper.getReadableDatabase();
        int iMatch = URI_MATCHER.match(uri);
        Log.d("Mms/Provider/MmsSms", "query begin, uri = " + uri + ", selection = " + str);
        switch (iMatch) {
            case 0:
                String queryParameter = uri.getQueryParameter("simple");
                if (queryParameter == null || !queryParameter.equals("true")) {
                    firstLockedMessage = getConversations(strArr, str, str3, smsTable, pduTable);
                } else {
                    String queryParameter2 = uri.getQueryParameter("thread_type");
                    firstLockedMessage = getSimpleConversations(strArr, TextUtils.isEmpty(queryParameter2) ? MTK_WAPPUSH_SUPPORT ? concatSelections(str, "type<>2") : str : concatSelections(str, "type=" + queryParameter2), strArr2, str3);
                    notifyUnreadMessageNumberChanged(getContext());
                }
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                    firstLockedMessage.setNotificationUri(getContext().getContentResolver(), Telephony.MmsSms.CONTENT_URI);
                }
                return firstLockedMessage;
            case 1:
                String queryParameter3 = uri.getQueryParameter("isRcse");
                firstLockedMessage = (queryParameter3 == null || !queryParameter3.equals("true")) ? getConversationMessages(uri.getPathSegments().get(1), strArr, str, str3, smsTable, pduTable) : readableDatabase.query("threads", strArr, concatSelections(str, "_id=" + uri.getPathSegments().get(1)), strArr2, null, null, str3);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 2:
                firstLockedMessage = getConversationById(uri.getPathSegments().get(1), strArr, str, strArr2, str3);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 3:
                firstLockedMessage = getMessagesByPhoneNumber(uri.getPathSegments().get(2), strArr, str, str3, smsTable, pduTable);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 4:
                ?? r1 = 0;
                byte b = 0;
                List<String> queryParameters = uri.getQueryParameters("recipient");
                String queryParameter4 = uri.getQueryParameter("backupRestoreIndex");
                if (!TextUtils.isEmpty(queryParameter4) && queryParameter4.equals("1")) {
                    b = 1;
                } else if (TextUtils.isEmpty(queryParameter4) || !queryParameter4.equals("0")) {
                    b = !TextUtils.isEmpty(queryParameter4) ? (byte) 2 : (byte) -1;
                }
                try {
                    if (b == 1) {
                        ThreadCache.init(getContext());
                        r1 = 0;
                        threadCache2 = ThreadCache.getInstance();
                    } else {
                        if (b <= -1) {
                            threadCache2 = 0;
                            if (MTK_WAPPUSH_SUPPORT) {
                                cBThreadId = !uri.getQueryParameters("cellbroadcast").isEmpty() ? getCBThreadId(queryParameters) : getThreadId(queryParameters);
                                if (b > -1 && threadCache2 != 0) {
                                    threadCache2.add(cBThreadId, queryParameters);
                                }
                                if (b == 0 && threadCache2 != 0) {
                                    threadCache2.removeAll();
                                }
                            } else {
                                cBThreadId = !uri.getQueryParameters("wappush").isEmpty() ? getWapPushThreadId(queryParameters) : !uri.getQueryParameters("cellbroadcast").isEmpty() ? getCBThreadId(queryParameters) : getThreadId(queryParameters);
                                if (b > -1 && threadCache2 != 0) {
                                    threadCache2.add(cBThreadId, queryParameters);
                                }
                                if (b == 0 && threadCache2 != 0) {
                                    threadCache2.removeAll();
                                }
                            }
                            firstLockedMessage = cBThreadId;
                            Log.d("Mms/Provider/MmsSms", "query end");
                            if (firstLockedMessage != null) {
                            }
                            return firstLockedMessage;
                        }
                        threadCache = ThreadCache.getInstance();
                        r1 = 0;
                        threadCache2 = threadCache;
                        if (threadCache != 0) {
                            try {
                                long threadId = threadCache.getThreadId(queryParameters, this.mUseStrictPhoneNumberComparation);
                                int i = (threadId > 0L ? 1 : (threadId == 0L ? 0 : -1));
                                r1 = i;
                                threadCache2 = threadCache;
                                if (i > 0) {
                                    Cursor cursorFormCursor = threadCache.formCursor(threadId);
                                    if (b == 0 && threadCache != 0) {
                                        threadCache.removeAll();
                                    }
                                    return cursorFormCursor;
                                }
                            } catch (Throwable th) {
                                th = th;
                                if (b == 0 && threadCache != 0) {
                                    threadCache.removeAll();
                                }
                                throw th;
                            }
                        }
                    }
                    if (MTK_WAPPUSH_SUPPORT) {
                    }
                    firstLockedMessage = cBThreadId;
                    Log.d("Mms/Provider/MmsSms", "query end");
                    if (firstLockedMessage != null) {
                    }
                    return firstLockedMessage;
                } catch (Throwable th2) {
                    th = th2;
                    threadCache = r1;
                }
                break;
            case 5:
                String str4 = "_id=" + uri.getPathSegments().get(1);
                if (!TextUtils.isEmpty(str)) {
                    str4 = str4 + " AND " + str;
                }
                firstLockedMessage = readableDatabase.query("canonical_addresses", CANONICAL_ADDRESSES_COLUMNS_1, str4, strArr2, null, null, str3);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 6:
                int i2 = 0;
                String queryParameter5 = uri.getQueryParameter("protocol");
                String queryParameter6 = uri.getQueryParameter("message");
                if (TextUtils.isEmpty(queryParameter5)) {
                    i2 = -1;
                } else if (!queryParameter5.equals("sms")) {
                    i2 = 1;
                }
                String str5 = i2 != -1 ? "proto_type=" + i2 : " 0=0 ";
                if (!TextUtils.isEmpty(queryParameter6)) {
                    str5 = str5 + " AND msg_id=" + queryParameter6;
                }
                String str6 = TextUtils.isEmpty(str) ? str5 : "(" + str5 + ") AND " + str;
                if (TextUtils.isEmpty(str2)) {
                    str3 = "due_time";
                }
                firstLockedMessage = readableDatabase.query("pending_msgs", null, str6, strArr2, null, null, str3);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 7:
                firstLockedMessage = getCompleteConversations(strArr, str, str3, smsTable, pduTable);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 8:
                firstLockedMessage = getUndeliveredMessages(strArr, str, strArr2, str3, smsTable, pduTable, Boolean.valueOf(uri.getBooleanQueryParameter("includeNonPermanent", true)));
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 9:
                firstLockedMessage = getConversationById(uri.getPathSegments().get(1), strArr, str, strArr2, str3);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 10:
            case 11:
            case 20:
            case 21:
            case 22:
            case 23:
            case 25:
            case 27:
            case 30:
            case 39:
            default:
                throw new IllegalStateException("Unrecognized URI:" + uri);
            case 12:
                firstLockedMessage = getDraftThread(strArr, str, str3, smsTable, pduTable);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 13:
                firstLockedMessage = readableDatabase.query("canonical_addresses", CANONICAL_ADDRESSES_COLUMNS_2, str, strArr2, null, null, str3);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 14:
                if (str3 != null || str != null || strArr2 != null || strArr != null) {
                    throw new IllegalArgumentException("do not specify sortOrder, selection, selectionArgs, or projectionwith this query");
                }
                String queryParameter7 = uri.getQueryParameter("pattern");
                if (queryParameter7 != null) {
                    Log.d("Mms/Provider/MmsSms", "URI_SEARCH pattern = " + queryParameter7.length());
                }
                String str7 = "%" + toIsoString(queryParameter7.getBytes()) + "%";
                String strSearchContacts = searchContacts(queryParameter7, getContactsByNumber(queryParameter7));
                String str8 = "%" + queryParameter7 + "%";
                String str9 = String.format("SELECT * FROM (%s UNION %s) GROUP BY %s ORDER BY %s", String.format("SELECT %s FROM sms,words WHERE ((sms.body LIKE ? OR thread_id %s) AND sms._id=words.source_id AND words.table_to_use=1 AND (sms.thread_id IN (SELECT _id FROM threads)))", "sms._id as _id,thread_id,address,body,date,0 as index_text,words._id,0 as charset,0 as m_type,sms.type as msg_box,1 as msg_type", strSearchContacts), String.format(Locale.ENGLISH, "SELECT %s FROM pdu left join part ON pdu._id=part.mid AND part.ct='text/plain' left join addr on addr.msg_id=pdu._id  WHERE ((((addr.type=%d) AND (pdu.msg_box == %d)) OR ((addr.type=%d) AND (pdu.msg_box != %d))) AND (part.text LIKE ? OR pdu.sub LIKE ? OR thread_id %s) AND (pdu.thread_id IN (SELECT _id FROM threads)))", "pdu._id,thread_id,addr.address,pdu.sub as body,pdu.date,0 as index_text,0,addr.charset as charset,pdu.m_type as m_type,pdu.msg_box as msg_box,2 as msg_type", 137, 1, 151, 1, strSearchContacts), "thread_id", "date DESC");
                try {
                    cBThreadId = readableDatabase.rawQuery(str9, new String[]{str8, str8, str7});
                    Log.e("Mms/Provider/MmsSms", "rawQuery = " + str9);
                    firstLockedMessage = cBThreadId;
                    Log.d("Mms/Provider/MmsSms", "query end");
                    if (firstLockedMessage != null) {
                    }
                    return firstLockedMessage;
                } catch (Exception e) {
                    Log.e("Mms/Provider/MmsSms", "got exception: " + e.toString());
                    return null;
                }
            case 15:
                if (str3 != null || str != null || strArr2 != null || strArr != null) {
                    throw new IllegalArgumentException("do not specify sortOrder, selection, selectionArgs, or projectionwith this query");
                }
                Log.d("Mms/Provider/MmsSms", "query().URI_SEARCH_SUGGEST: uriStr = " + uri);
                String string = uri.toString();
                String strTrim = string.substring(string.lastIndexOf("pattern=") + "pattern=".length()).trim();
                Log.d("Mms/Provider/MmsSms", "query().URI_SEARCH_SUGGEST: searchString = \"" + strTrim + "\"");
                String str10 = "%" + strTrim + "%";
                if (strTrim.trim().equals("") || strTrim == null) {
                    firstLockedMessage = null;
                } else {
                    String strSearchContacts2 = searchContacts(strTrim, getContactsByNumber(strTrim));
                    firstLockedMessage = readableDatabase.rawQuery("SELECT DISTINCT _id, index_text AS suggest_text_1, _id AS suggest_shortcut_id, index_text AS snippet FROM words WHERE index_text IS NOT NULL AND length(index_text)>0  AND ((index_text LIKE ? AND table_to_use!=3)  OR (source_id " + queryIdAndFormatIn(readableDatabase, String.format("SELECT _id FROM sms WHERE thread_id " + strSearchContacts2, new Object[0])) + " AND table_to_use=1)  OR (source_id " + queryIdAndFormatIn(readableDatabase, String.format("SELECT part._id FROM part JOIN pdu  ON part.mid=pdu._id  WHERE part.ct='text/plain' AND pdu.thread_id " + strSearchContacts2, new Object[0])) + " AND table_to_use=2)  OR (source_id " + queryIdAndFormatIn(readableDatabase, String.format("SELECT _id FROM pdu WHERE thread_id " + strSearchContacts2, new Object[0])) + " AND table_to_use=4))  ORDER BY snippet LIMIT 50", new String[]{str10});
                    StringBuilder sb = new StringBuilder();
                    sb.append("search suggestion cursor count is : ");
                    sb.append(firstLockedMessage.getCount());
                    Log.d("Mms/Provider/MmsSms", sb.toString());
                }
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 16:
                firstLockedMessage = getFirstLockedMessage(strArr, str, str3, smsTable, pduTable);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 17:
                try {
                    firstLockedMessage = getFirstLockedMessage(strArr, "thread_id=" + Long.toString(Long.parseLong(uri.getLastPathSegment())), str3, smsTable, pduTable);
                } catch (NumberFormatException e2) {
                    Log.e("Mms/Provider/MmsSms", "Thread ID must be a long.");
                    firstLockedMessage = null;
                }
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 18:
                try {
                    long j3 = Long.parseLong(uri.getQueryParameter("row_id"));
                    switch (Integer.parseInt(uri.getQueryParameter("table_to_use"))) {
                        case 1:
                            cursorQuery = readableDatabase.query(smsTable, new String[]{"thread_id"}, "_id=?", new String[]{String.valueOf(j3)}, null, null, null);
                            firstLockedMessage = cursorQuery;
                            break;
                        case 2:
                            cursorQuery = readableDatabase.rawQuery("SELECT thread_id FROM " + pduTable + ",part WHERE ((part.mid=" + pduTable + "._id) AND (part._id=?))", new String[]{String.valueOf(j3)});
                            firstLockedMessage = cursorQuery;
                            break;
                        default:
                            firstLockedMessage = null;
                            break;
                    }
                } catch (NumberFormatException e3) {
                    firstLockedMessage = null;
                }
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 19:
                firstLockedMessage = readableDatabase.query("quicktext", strArr, str, strArr2, null, null, str3);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 24:
                firstLockedMessage = getRecipientsNumber(uri.getPathSegments().get(1));
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 26:
                try {
                    long j4 = Long.parseLong(uri.getLastPathSegment());
                    Log.d("Mms/Provider/MmsSms", "query URI_STATUS Thread ID is " + j4);
                    firstLockedMessage = readableDatabase.query("threads", STATUS_PROJECTION, "_id=" + Long.toString(j4), null, null, null, str3);
                    Log.d("Mms/Provider/MmsSms", "query URI_STATUS ok");
                } catch (NumberFormatException e4) {
                    Log.e("Mms/Provider/MmsSms", "Thread ID must be a long.");
                    firstLockedMessage = null;
                }
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 28:
                firstLockedMessage = getAllUnreadCount(readableDatabase);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 29:
                try {
                    long j5 = Long.parseLong(uri.getLastPathSegment());
                    Log.d("Mms/Provider/MmsSms", "query URI_SIMID_LIST Thread ID is " + j5);
                    firstLockedMessage = getSimidListByThread(readableDatabase, j5);
                } catch (NumberFormatException e5) {
                    Log.e("Mms/Provider/MmsSms", "URI_SIMID_LIST Thread ID must be a long.");
                    firstLockedMessage = null;
                }
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 31:
                firstLockedMessage = readableDatabase.query("thread_settings", strArr, str, strArr2, null, null, str3);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 32:
                firstLockedMessage = getConversationSettingsById(uri.getPathSegments().get(1), strArr, str, strArr2, str3);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 33:
                String queryParameter8 = uri.getQueryParameter("simple");
                if (queryParameter8 == null || !queryParameter8.equals("true")) {
                    firstLockedMessage = getConversations(strArr, str, str3, smsTable, pduTable);
                } else {
                    String queryParameter9 = uri.getQueryParameter("thread_type");
                    firstLockedMessage = getSimpleConversationsExtend(strArr, TextUtils.isEmpty(queryParameter9) ? MTK_WAPPUSH_SUPPORT ? concatSelections(str, "type<>2") : str : concatSelections(str, "type=" + queryParameter9), strArr2, str3);
                    notifyUnreadMessageNumberChanged(getContext());
                }
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 34:
                firstLockedMessage = getConversationMessagesDistinct(uri.getPathSegments().get(1), strArr, str, str3);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 35:
                String queryParameter10 = uri.getQueryParameter("numbers");
                firstLockedMessage = readableDatabase.rawQuery(String.format("SELECT * FROM (%s UNION %s) ORDER BY %s", String.format("SELECT sms._id,thread_id,address,date,body,type,1 as clssify FROM sms where address IN " + getAllMaybeNumbers(queryParameter10) + " AND type <> 3 AND type <> 4 AND type <> 5", new Object[0]), String.format("SELECT pdu._id,pdu.thread_id,addr.address,pdu.date * 1000 as date,pdu.sub as body,pdu.msg_box as type,2 as clssify FROM pdu,addr where (addr.msg_id = pdu._id) AND type <> 3 AND type <> 4 AND (addr.address IN " + getAllMaybeNumbers(queryParameter10) + ")", new Object[0]), "date DESC"), null);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 36:
                firstLockedMessage = readableDatabase.rawQuery(String.format("SELECT * FROM (%s UNION %s) GROUP BY %s ORDER BY %s", String.format("SELECT sms._id,sms.thread_id,address,date FROM sms WHERE type <> 3 AND type <> 4 AND type <> 5", new Object[0]), String.format("SELECT pdu._id,pdu.thread_id,addr.address as address,pdu.date * 1000 as date FROM pdu,addr WHERE addr.msg_id = pdu._id AND ((addr.type = 151 AND pdu.msg_box = 2) OR (addr.type = 137 AND pdu.msg_box = 1)) AND msg_box <> 3 AND msg_box <> 4", new Object[0]), "thread_id,address", "date DESC LIMIT 100"), null);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 37:
                firstLockedMessage = readableDatabase.rawQuery("SELECT _id, index_text AS suggest_text_1, _id AS suggest_shortcut_id, index_text AS snippet FROM words WHERE (_id = " + Long.decode(uri.getLastPathSegment()) + ")", null);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 38:
                return getSimIdAndMsgCount(readableDatabase, Long.parseLong(uri.getLastPathSegment()));
            case 40:
                try {
                    long j6 = Long.parseLong(uri.getLastPathSegment());
                    Log.d("Mms/Provider/MmsSms", "query URI_WIDGET_THREAD Thread ID is " + j6);
                    firstLockedMessage = getMsgInfo(readableDatabase, j6, str);
                } catch (NumberFormatException e6) {
                    Log.e("Mms/Provider/MmsSms", "URI_WIDGET_THREAD Thread ID must be a long.");
                    firstLockedMessage = null;
                }
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 41:
                notifyUnreadMessageNumberChanged(getContext());
                firstLockedMessage = null;
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 42:
                if (str3 != null || str != null || strArr2 != null || strArr != null) {
                    throw new IllegalArgumentException("do not specify sortOrder, selection, selectionArgs, or projectionwith this query");
                }
                String queryParameter11 = uri.getQueryParameter("content");
                String queryParameter12 = uri.getQueryParameter("name");
                String queryParameter13 = uri.getQueryParameter("number");
                String queryParameter14 = uri.getQueryParameter("begin_date");
                String queryParameter15 = uri.getQueryParameter("end_date");
                Log.d("Mms/Provider/MmsSms", "query(): URI_SEARCH_ADVANCED, content = " + queryParameter11 + ", name = " + queryParameter12 + ", number = " + queryParameter13 + ", beginDateStr = " + queryParameter14 + ", endDateStr = " + queryParameter15);
                StringBuilder sb2 = new StringBuilder();
                sb2.append("query(): URI_SEARCH_ADVANCED, content is null ?= ");
                sb2.append(queryParameter11 == null);
                sb2.append(", name is null ?= ");
                sb2.append(queryParameter12 == null);
                sb2.append(", number is null ?= ");
                sb2.append(queryParameter13 == null);
                Log.d("Mms/Provider/MmsSms", sb2.toString());
                try {
                    j = Long.parseLong(queryParameter14);
                } catch (NumberFormatException e7) {
                    j = 0;
                }
                try {
                    j2 = Long.parseLong(queryParameter15);
                } catch (NumberFormatException e8) {
                    j2 = 0;
                }
                boolean z = !TextUtils.isEmpty(queryParameter11);
                boolean z2 = !TextUtils.isEmpty(queryParameter12);
                boolean z3 = !TextUtils.isEmpty(queryParameter13);
                boolean z4 = j > 0;
                boolean z5 = j2 > 0;
                Log.d("Mms/Provider/MmsSms", "query(): URI_SEARCH_ADVANCED, hasContent = " + z + ", hasName = " + z2 + ", hasNumber = " + z3 + ", hasBeginDate = " + z4 + ", hasEndDate = " + z5);
                if (!z && !z2 && !z3 && !z4 && !z5) {
                    Log.e("Mms/Provider/MmsSms", "query(): URI_SEARCH_ADVANCED, no query parameter.");
                    return null;
                }
                if (z4 && z5 && j >= j2) {
                    Log.e("Mms/Provider/MmsSms", "query(): URI_SEARCH_ADVANCED, begin data later than end date.");
                    return null;
                }
                HashMap<String, String> map = new HashMap<>();
                if (z3) {
                    map = getContactsByNumber(queryParameter13);
                }
                if (z2) {
                    map = getContactsByName(queryParameter12, z3, map);
                }
                String strSearchContactsAdvanced = searchContactsAdvanced(queryParameter13, queryParameter12, map);
                if (TextUtils.isEmpty(strSearchContactsAdvanced) && (z2 || z3)) {
                    Log.e("Mms/Provider/MmsSms", "query(): URI_SEARCH_ADVANCED, no matched thread");
                    return null;
                }
                Locale locale = Locale.ENGLISH;
                StringBuilder sb3 = new StringBuilder();
                sb3.append("SELECT %s FROM sms,words WHERE (sms._id=words.source_id AND words.table_to_use=1 AND (sms.thread_id IN (SELECT _id FROM threads))");
                sb3.append(z ? " AND sms.body LIKE ?" : "");
                sb3.append(TextUtils.isEmpty(strSearchContactsAdvanced) ? "" : " AND thread_id " + strSearchContactsAdvanced);
                sb3.append(z4 ? " AND (sms.date >= " + j + " OR (sms.date_sent > 0 AND sms.date_sent >= " + j + "))" : "");
                sb3.append(z5 ? " AND (sms.date < " + j2 + " OR (sms.date_sent > 0 AND sms.date_sent < " + j2 + "))" : "");
                sb3.append(")");
                String str11 = String.format(locale, sb3.toString(), "sms._id as _id,thread_id,address,body,date,0 as index_text,words._id,0 as charset,0 as m_type,1 as msg_type");
                Log.d("Mms/Provider/MmsSms", "query(): URI_SEARCH_ADVANCED, smsQuery = " + str11);
                Locale locale2 = Locale.ENGLISH;
                StringBuilder sb4 = new StringBuilder();
                sb4.append("SELECT %s FROM pdu left join part,addr WHERE ((addr.msg_id=pdu._id) AND (((addr.type=%d) AND (pdu.msg_box == %d)) OR ((addr.type=%d) AND (pdu.msg_box != %d))) AND (pdu.thread_id IN (SELECT _id FROM threads)) AND (part.mid=pdu._id)");
                sb4.append(z ? " AND ((part.ct='text/plain' AND part.text LIKE ?) OR pdu.sub LIKE ?)" : "");
                sb4.append(TextUtils.isEmpty(strSearchContactsAdvanced) ? "" : " AND thread_id " + strSearchContactsAdvanced);
                sb4.append(z4 ? " AND (pdu.date * 1000 >= " + j + " OR (pdu.date_sent > 0 AND pdu.date_sent * 1000 >= " + j + "))" : "");
                sb4.append(z5 ? " AND (pdu.date * 1000 < " + j2 + " OR (pdu.date_sent > 0 AND pdu.date_sent * 1000 < " + j2 + "))" : "");
                sb4.append(")");
                String str12 = String.format(locale2, sb4.toString(), "pdu._id,thread_id,addr.address,pdu.sub as body,pdu.date,0 as index_text,0,addr.charset as charset,pdu.m_type as m_type,2 as msg_type", 137, 1, 151, 1);
                Log.d("Mms/Provider/MmsSms", "query(): URI_SEARCH_ADVANCED, mmsQuery = " + str12);
                String str13 = String.format("SELECT * FROM (%s UNION %s) GROUP BY %s ORDER BY %s", str11, str12, "thread_id", "date DESC");
                try {
                    if (z) {
                        String str14 = "%" + queryParameter11 + "%";
                        cBThreadId = readableDatabase.rawQuery(str13, new String[]{str14, str14, str14});
                    } else {
                        cBThreadId = readableDatabase.rawQuery(str13, null);
                    }
                    Log.d("Mms/Provider/MmsSms", "query(): URI_SEARCH_ADVANCED, rawQuery = " + str13);
                    firstLockedMessage = cBThreadId;
                    Log.d("Mms/Provider/MmsSms", "query end");
                    if (firstLockedMessage != null) {
                    }
                    return firstLockedMessage;
                } catch (Exception e9) {
                    Log.e("Mms/Provider/MmsSms", "query(): URI_SEARCH_ADVANCED, got exception: " + e9.toString(), e9);
                    return null;
                }
                break;
            case 43:
                firstLockedMessage = readableDatabase.rawQuery(String.format("SELECT threads._id FROM threads,canonical_addresses WHERE threads.recipient_ids = canonical_addresses._id AND canonical_addresses.address = %s", str), null);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
            case 44:
                firstLockedMessage = readableDatabase.rawQuery(String.format("SELECT COUNT(_id) FROM (SELECT ipmsg_id, _id FROM sms WHERE read = 0 AND ipmsg_id = 0 UNION SELECT ipmsg_id, _id FROM sms WHERE read = 0 GROUP BY ipmsg_id HAVING ipmsg_id < 0 UNION SELECT 0 as ipmsg_id, _id FROM pdu WHERE read = 0 AND (m_type=132 OR m_type=130 OR m_type=128) UNION SELECT 1 as ipmsg_id, _id FROM cellbroadcast WHERE read = 0)", new Object[0]), null);
                Log.d("Mms/Provider/MmsSms", "query end");
                if (firstLockedMessage != null) {
                }
                return firstLockedMessage;
        }
    }

    private Cursor getSimIdAndMsgCount(SQLiteDatabase sQLiteDatabase, long j) {
        int count;
        int i;
        int i2;
        long j2;
        int count2;
        int i3;
        int i4;
        Log.d("Mms/Provider/MmsSms", " threadId = " + j);
        String str = String.format("SELECT _id, sub_id, read, date FROM sms where thread_id = " + j + " AND sms.type!=3 ORDER BY date DESC", new Object[0]);
        String str2 = String.format("SELECT _id, sub_id, read, date * 1000 as date FROM pdu where thread_id = " + j + " AND (m_type=128 OR m_type=130 OR m_type=132) AND msg_box!=3 ORDER BY date DESC", new Object[0]);
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery(str, null);
        long j3 = 0;
        int i5 = 2;
        if (cursorRawQuery != null) {
            try {
                if (cursorRawQuery.getCount() > 0) {
                    count = cursorRawQuery.getCount();
                    cursorRawQuery.moveToFirst();
                    i = cursorRawQuery.getInt(1);
                    j2 = cursorRawQuery.getLong(3);
                    cursorRawQuery.moveToPosition(-1);
                    i2 = 0;
                    while (cursorRawQuery.moveToNext()) {
                        if (cursorRawQuery.getInt(2) == 0) {
                            i2++;
                        }
                    }
                } else {
                    count = 0;
                    i = 0;
                    i2 = 0;
                    j2 = 0;
                }
                cursorRawQuery.close();
            } finally {
            }
        } else {
            count = 0;
            i = 0;
            i2 = 0;
            j2 = 0;
        }
        cursorRawQuery = sQLiteDatabase.rawQuery(str2, null);
        if (cursorRawQuery != null) {
            try {
                Log.d("Mms/Provider/MmsSms", " mmsCursor.getCount() = " + cursorRawQuery.getCount());
                if (cursorRawQuery.getCount() > 0) {
                    count2 = cursorRawQuery.getCount();
                    cursorRawQuery.moveToFirst();
                    i3 = cursorRawQuery.getInt(1);
                    j3 = cursorRawQuery.getLong(3);
                    cursorRawQuery.moveToPosition(-1);
                    i4 = 0;
                    while (cursorRawQuery.moveToNext()) {
                        if (cursorRawQuery.getInt(i5) == 0) {
                            i4++;
                            Log.d("Mms/Provider/MmsSms", " mmsUnread = " + i4);
                            i5 = 2;
                        }
                    }
                } else {
                    count2 = 0;
                    i3 = 0;
                    i4 = 0;
                }
            } finally {
            }
        } else {
            count2 = 0;
            i3 = 0;
            i4 = 0;
        }
        if (j2 <= j3) {
            i = i3;
        }
        Log.d("Mms/Provider/MmsSms", "smsCount = " + count + " smsUnread = " + i2 + " mmsCount = " + count2 + " mmsUnread = " + i4 + " simId = " + i + " smsSimDate = " + j2 + " mmsSimDate = " + j3);
        Object[] objArr = {Integer.valueOf(count), Integer.valueOf(i2), Integer.valueOf(count2), Integer.valueOf(i4), Integer.valueOf(i)};
        MatrixCursor matrixCursor = new MatrixCursor(CONVERSATION_SIM_ID_AND_MSG_COUNT, 1);
        matrixCursor.addRow(objArr);
        return matrixCursor;
    }

    private Set<Long> getAddressIds(List<String> list) throws Throwable {
        Cursor cursorQuery;
        String str;
        boolean z;
        HashSet hashSet = new HashSet(list.size());
        HashMap map = new HashMap();
        HashMap map2 = new HashMap();
        Log.d("Mms/Provider/MmsSms", "getAddressIds begin");
        try {
            SQLiteDatabase readableDatabase = this.mOpenHelper.getReadableDatabase();
            readableDatabase.beginTransaction();
            cursorQuery = readableDatabase.query("canonical_addresses", CANONICAL_ADDRESSES_COLUMNS_2, null, null, null, null, null);
            int i = 7;
            if (cursorQuery != null) {
                while (cursorQuery.moveToNext()) {
                    try {
                        long j = cursorQuery.getLong(cursorQuery.getColumnIndexOrThrow("_id"));
                        String string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("address"));
                        CharBuffer charBufferAllocate = CharBuffer.allocate(7);
                        if (Telephony.Mms.isEmailAddress(string)) {
                            string = string.toLowerCase();
                        }
                        String strKey = key(string, charBufferAllocate);
                        ArrayList arrayList = (ArrayList) map2.get(strKey);
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                            map2.put(strKey, arrayList);
                        }
                        arrayList.add(string);
                        map.put(string, Long.valueOf(j));
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
            }
            HashSet<String> hashSet2 = new HashSet();
            for (String lowerCase : list) {
                if (!lowerCase.equals("insert-address-token")) {
                    boolean zIsEmailAddress = Telephony.Mms.isEmailAddress(lowerCase);
                    boolean zIsPhoneNumber = Telephony.Mms.isPhoneNumber(lowerCase);
                    if (zIsEmailAddress) {
                        lowerCase = lowerCase.toLowerCase();
                    }
                    String strKey2 = key(lowerCase, CharBuffer.allocate(i));
                    ArrayList arrayList2 = (ArrayList) map2.get(strKey2);
                    Long l = -1L;
                    String str2 = "";
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList();
                        map2.put(strKey2, arrayList2);
                    } else {
                        int i2 = 0;
                        while (i2 < arrayList2.size()) {
                            str = (String) arrayList2.get(i2);
                            l = (Long) map.get(str);
                            if (!str.equals(lowerCase) && (!zIsPhoneNumber || lowerCase == null || lowerCase.length() > 15 || str == null || str.length() > 15 || !PhoneNumberUtils.compare(lowerCase, str, this.mUseStrictPhoneNumberComparation))) {
                                i2++;
                                str2 = str;
                            }
                            z = true;
                        }
                    }
                    str = str2;
                    z = false;
                    if (z && l != null && l.longValue() != -1) {
                        MmsProviderLog.dpi("Mms/Provider/MmsSms", "getAddressIds: get exist id=" + l + ", refinedAddress=" + lowerCase + ", currentNumber=" + str);
                        hashSet.add(l);
                    } else if (!z || l != null) {
                        arrayList2.add(lowerCase);
                        hashSet2.add(lowerCase);
                    }
                }
                i = 7;
            }
            MmsProviderLog.dpi("Mms/Provider/MmsSms", "getAddressIds: insert new canonical_address for xxxxxx, addressess = " + hashSet2.toString());
            for (String str3 : hashSet2) {
                long jInsertCanonicalAddresses = insertCanonicalAddresses(this.mOpenHelper, str3);
                if (jInsertCanonicalAddresses != -1) {
                    hashSet.add(Long.valueOf(jInsertCanonicalAddresses));
                } else {
                    MmsProviderLog.dpi("Mms/Provider/MmsSms", "getAddressIds: address ID not found for " + str3);
                }
            }
            readableDatabase.setTransactionSuccessful();
            readableDatabase.endTransaction();
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return hashSet;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    private long[] getSortedSet(Set<Long> set) {
        int size = set.size();
        long[] jArr = new long[size];
        Iterator<Long> it = set.iterator();
        int i = 0;
        while (it.hasNext()) {
            jArr[i] = it.next().longValue();
            i++;
        }
        if (size > 1) {
            Arrays.sort(jArr);
        }
        return jArr;
    }

    private String getSpaceSeparatedNumbers(long[] jArr) {
        int length = jArr.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i != 0) {
                sb.append(' ');
            }
            sb.append(jArr[i]);
        }
        return sb.toString();
    }

    private void insertThread(String str, List<String> list) {
        ContentValues contentValues = new ContentValues(4);
        long jCurrentTimeMillis = System.currentTimeMillis();
        contentValues.put("date", Long.valueOf(jCurrentTimeMillis - (jCurrentTimeMillis % 1000)));
        contentValues.put("recipient_ids", str);
        if (list.size() > 1) {
            contentValues.put("type", (Integer) 1);
        } else if (list != null && list.size() == 1 && "35221601851".equals(list.get(0))) {
            contentValues.put("type", (Integer) 10);
        }
        contentValues.put("message_count", (Integer) 0);
        MmsProviderLog.dpi("Mms/Provider/MmsSms", "insertThread: created new thread_id " + this.mOpenHelper.getWritableDatabase().insert("threads", null, contentValues) + " for recipientIds " + str);
        getContext().getContentResolver().notifyChange(Telephony.MmsSms.CONTENT_URI, null, true, -1);
    }

    private synchronized Cursor getThreadId(List<String> list) {
        Cursor cursorRawQuery;
        Set<Long> addressIds = getAddressIds(list);
        String spaceSeparatedNumbers = "";
        Cursor cursorRawQuery2 = null;
        if (addressIds.size() == 0) {
            Log.e("Mms/Provider/MmsSms", "getThreadId: NO receipients specified -- NOT creating thread", new Exception());
            return null;
        }
        if (addressIds.size() == 1) {
            Iterator<Long> it = addressIds.iterator();
            while (it.hasNext()) {
                spaceSeparatedNumbers = Long.toString(it.next().longValue());
            }
        } else {
            spaceSeparatedNumbers = getSpaceSeparatedNumbers(getSortedSet(addressIds));
        }
        if (Log.isLoggable("Mms/Provider/MmsSms", 2)) {
            MmsProviderLog.dpi("Mms/Provider/MmsSms", "getThreadId: recipientIds (selectionArgs) =" + spaceSeparatedNumbers);
        }
        String[] strArr = {spaceSeparatedNumbers};
        SQLiteDatabase readableDatabase = this.mOpenHelper.getReadableDatabase();
        readableDatabase.beginTransaction();
        try {
            try {
                cursorRawQuery = readableDatabase.rawQuery(THREAD_QUERY, strArr);
            } catch (Throwable th) {
                th = th;
            }
            try {
                if (cursorRawQuery.getCount() == 0) {
                    cursorRawQuery.close();
                    MmsProviderLog.dpi("Mms/Provider/MmsSms", "getThreadId: create new thread_id for recipients " + list);
                    insertThread(spaceSeparatedNumbers, list);
                    cursorRawQuery2 = readableDatabase.rawQuery(THREAD_QUERY, strArr);
                } else {
                    cursorRawQuery2 = cursorRawQuery;
                }
                readableDatabase.setTransactionSuccessful();
            } catch (Throwable th2) {
                th = th2;
                cursorRawQuery2 = cursorRawQuery;
                Log.e("Mms/Provider/MmsSms", th.getMessage(), th);
            }
            if (cursorRawQuery2 != null && cursorRawQuery2.getCount() > 1) {
                Log.w("Mms/Provider/MmsSms", "getThreadId: why is cursorCount=" + cursorRawQuery2.getCount());
            }
            return cursorRawQuery2;
        } finally {
            readableDatabase.endTransaction();
        }
    }

    public static String concatSelections(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            return str2;
        }
        if (TextUtils.isEmpty(str2)) {
            return str;
        }
        return str + " AND " + str2;
    }

    public static String[] handleNullMessageProjection(String[] strArr) {
        return strArr == null ? UNION_COLUMNS : strArr;
    }

    private static String[] handleNullThreadsProjection(String[] strArr) {
        return strArr == null ? THREADS_COLUMNS : strArr;
    }

    public static String handleNullSortOrder(String str) {
        return str == null ? "normalized_date ASC" : str;
    }

    private Cursor getSimpleConversations(String[] strArr, String str, String[] strArr2, String str2) {
        return this.mOpenHelper.getReadableDatabase().query("threads", strArr, str, strArr2, null, null, " date DESC");
    }

    private Cursor getDraftThread(String[] strArr, String str, String str2, String str3, String str4) {
        String[] strArr2 = {"_id", "thread_id"};
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables(str4);
        sQLiteQueryBuilder2.setTables(str3);
        String strBuildUnionSubQuery = sQLiteQueryBuilder.buildUnionSubQuery("transport_type", strArr2, MMS_COLUMNS, 1, "mms", concatSelections(str, "msg_box=3"), null, null);
        String strBuildUnionSubQuery2 = sQLiteQueryBuilder2.buildUnionSubQuery("transport_type", strArr2, SMS_COLUMNS, 1, "sms", concatSelections(str, "type=3"), null, null);
        SQLiteQueryBuilder sQLiteQueryBuilder3 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder3.setDistinct(true);
        String strBuildUnionQuery = sQLiteQueryBuilder3.buildUnionQuery(new String[]{strBuildUnionSubQuery, strBuildUnionSubQuery2}, null, null);
        SQLiteQueryBuilder sQLiteQueryBuilder4 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder4.setTables("(" + strBuildUnionQuery + ")");
        return this.mOpenHelper.getReadableDatabase().rawQuery(sQLiteQueryBuilder4.buildQuery(strArr, null, null, null, str2, null), EMPTY_STRING_ARRAY);
    }

    private Cursor getConversations(String[] strArr, String str, String str2, String str3, String str4) {
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables(str4);
        sQLiteQueryBuilder2.setTables(str3);
        String[] strArrHandleNullMessageProjection = handleNullMessageProjection(strArr);
        String[] strArrMakeProjectionWithDateAndThreadId = makeProjectionWithDateAndThreadId(UNION_COLUMNS, 1000);
        String[] strArrMakeProjectionWithDateAndThreadId2 = makeProjectionWithDateAndThreadId(UNION_COLUMNS, 1);
        String strBuildUnionSubQuery = sQLiteQueryBuilder.buildUnionSubQuery("transport_type", strArrMakeProjectionWithDateAndThreadId, MMS_COLUMNS, 1, "mms", concatSelections(str, "(msg_box != 3 AND (m_type = 128 OR m_type = 132 OR m_type = 130))"), "thread_id", "date = MAX(date)");
        String strBuildUnionSubQuery2 = sQLiteQueryBuilder2.buildUnionSubQuery("transport_type", strArrMakeProjectionWithDateAndThreadId2, SMS_COLUMNS, 1, "sms", concatSelections(str, "(type != 3)"), "thread_id", "date = MAX(date)");
        SQLiteQueryBuilder sQLiteQueryBuilder3 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder3.setDistinct(true);
        String strBuildUnionQuery = sQLiteQueryBuilder3.buildUnionQuery(new String[]{strBuildUnionSubQuery, strBuildUnionSubQuery2}, null, null);
        SQLiteQueryBuilder sQLiteQueryBuilder4 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder4.setTables("(" + strBuildUnionQuery + ")");
        return this.mOpenHelper.getReadableDatabase().rawQuery(sQLiteQueryBuilder4.buildQuery(strArrHandleNullMessageProjection, null, "tid", "normalized_date = MAX(normalized_date)", str2, null), EMPTY_STRING_ARRAY);
    }

    private Cursor getFirstLockedMessage(String[] strArr, String str, String str2, String str3, String str4) {
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
        new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables(str4);
        sQLiteQueryBuilder2.setTables(str3);
        String[] strArr2 = {"_id"};
        String strBuildUnionSubQuery = sQLiteQueryBuilder.buildUnionSubQuery("transport_type", strArr2, null, 1, "mms", str, "_id", "locked=1");
        String strBuildUnionSubQuery2 = sQLiteQueryBuilder2.buildUnionSubQuery("transport_type", strArr2, null, 1, "sms", str, "_id", "locked=1");
        SQLiteQueryBuilder sQLiteQueryBuilder3 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder3.setDistinct(true);
        return this.mOpenHelper.getReadableDatabase().rawQuery(sQLiteQueryBuilder3.buildUnionQuery(new String[]{strBuildUnionSubQuery, strBuildUnionSubQuery2}, null, "1"), EMPTY_STRING_ARRAY);
    }

    private Cursor getCompleteConversations(String[] strArr, String str, String str2, String str3, String str4) {
        return this.mOpenHelper.getReadableDatabase().rawQuery(buildConversationQuery(strArr, str, str2, str3, str4), EMPTY_STRING_ARRAY);
    }

    private String[] makeProjectionWithDateAndThreadId(String[] strArr, int i) {
        int length = strArr.length;
        String[] strArr2 = new String[length + 2];
        strArr2[0] = "thread_id AS tid";
        strArr2[1] = "date * " + i + " AS normalized_date";
        for (int i2 = 0; i2 < length; i2++) {
            strArr2[i2 + 2] = strArr[i2];
        }
        return strArr2;
    }

    private Cursor getConversationMessages(String str, String[] strArr, String str2, String str3, String str4, String str5) {
        try {
            Long.parseLong(str);
            return this.mOpenHelper.getReadableDatabase().rawQuery(buildConversationQuery(strArr, concatSelections(str2, "thread_id = " + str), str3, str4, str5), EMPTY_STRING_ARRAY);
        } catch (NumberFormatException e) {
            Log.e("Mms/Provider/MmsSms", "Thread ID must be a Long.");
            return null;
        }
    }

    private Cursor getMessagesByPhoneNumber(String str, String[] strArr, String str2, String str3, String str4, String str5) {
        String strSqlEscapeString = DatabaseUtils.sqlEscapeString(str);
        String strConcatSelections = concatSelections(str2, str5 + "._id = matching_addresses.address_msg_id");
        StringBuilder sb = new StringBuilder();
        sb.append("(address=");
        sb.append(strSqlEscapeString);
        sb.append(" OR PHONE_NUMBERS_EQUAL(address, ");
        sb.append(strSqlEscapeString);
        sb.append(this.mUseStrictPhoneNumberComparation ? ", 1))" : ", 0))");
        String strConcatSelections2 = concatSelections(str2, sb.toString());
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setDistinct(true);
        sQLiteQueryBuilder2.setDistinct(true);
        StringBuilder sb2 = new StringBuilder();
        sb2.append(str5);
        sb2.append(", (SELECT msg_id AS address_msg_id FROM addr WHERE (address=");
        sb2.append(strSqlEscapeString);
        sb2.append(" OR PHONE_NUMBERS_EQUAL(addr.address, ");
        sb2.append(strSqlEscapeString);
        sb2.append(this.mUseStrictPhoneNumberComparation ? ", 1))) " : ", 0))) ");
        sb2.append("AS matching_addresses");
        sQLiteQueryBuilder.setTables(sb2.toString());
        sQLiteQueryBuilder2.setTables(str4);
        String[] strArrHandleNullMessageProjection = handleNullMessageProjection(strArr);
        String strBuildUnionSubQuery = sQLiteQueryBuilder.buildUnionSubQuery("transport_type", strArrHandleNullMessageProjection, MMS_COLUMNS, 0, "mms", strConcatSelections, null, null);
        String strBuildUnionSubQuery2 = sQLiteQueryBuilder2.buildUnionSubQuery("transport_type", strArrHandleNullMessageProjection, SMS_COLUMNS, 0, "sms", strConcatSelections2, null, null);
        SQLiteQueryBuilder sQLiteQueryBuilder3 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder3.setDistinct(true);
        return this.mOpenHelper.getReadableDatabase().rawQuery(sQLiteQueryBuilder3.buildUnionQuery(new String[]{strBuildUnionSubQuery, strBuildUnionSubQuery2}, str3, null), EMPTY_STRING_ARRAY);
    }

    private Cursor getConversationById(String str, String[] strArr, String str2, String[] strArr2, String str3) {
        try {
            Long.parseLong(str);
            String strConcatSelections = concatSelections(str2, "_id=" + str);
            SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
            String[] strArrHandleNullThreadsProjection = handleNullThreadsProjection(strArr);
            sQLiteQueryBuilder.setDistinct(true);
            sQLiteQueryBuilder.setTables("threads");
            return sQLiteQueryBuilder.query(this.mOpenHelper.getReadableDatabase(), strArrHandleNullThreadsProjection, strConcatSelections, strArr2, str3, null, null);
        } catch (NumberFormatException e) {
            Log.e("Mms/Provider/MmsSms", "Thread ID must be a Long.");
            return null;
        }
    }

    public static String joinPduAndPendingMsgTables(String str) {
        return str + " LEFT JOIN pending_msgs ON " + str + "._id = pending_msgs.msg_id";
    }

    public static String[] createMmsProjection(String[] strArr, String str) {
        String[] strArr2 = new String[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            if (strArr[i].equals("_id")) {
                strArr2[i] = str + "._id";
            } else {
                strArr2[i] = strArr[i];
            }
        }
        return strArr2;
    }

    private Cursor getUndeliveredMessages(String[] strArr, String str, String[] strArr2, String str2, String str3, String str4, Boolean bool) {
        String strConcatSelections;
        String[] strArrCreateMmsProjection = createMmsProjection(strArr, str4);
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables(joinPduAndPendingMsgTables(str4));
        sQLiteQueryBuilder2.setTables(str3);
        if (bool.booleanValue()) {
            Log.d("Mms/Provider/MmsSms", "getUndeliveredMessages true");
            strConcatSelections = concatSelections(str, "(msg_box = 4 OR msg_box = 5)");
        } else {
            Log.d("Mms/Provider/MmsSms", "getUndeliveredMessages false");
            strConcatSelections = concatSelections(str, "msg_box = 4 AND err_type = 10");
        }
        String str5 = strConcatSelections;
        String strConcatSelections2 = concatSelections(str, "(type = 4 OR type = 5 OR type = 6)");
        String[] strArrHandleNullMessageProjection = handleNullMessageProjection(strArr);
        String[] strArrMakeProjectionWithDateAndThreadId = makeProjectionWithDateAndThreadId(handleNullMessageProjection(strArrCreateMmsProjection), 1000);
        String[] strArrMakeProjectionWithDateAndThreadId2 = makeProjectionWithDateAndThreadId(strArrHandleNullMessageProjection, 1);
        HashSet hashSet = new HashSet(MMS_COLUMNS);
        hashSet.add(str4 + "._id");
        hashSet.add("err_type");
        String strBuildUnionSubQuery = sQLiteQueryBuilder.buildUnionSubQuery("transport_type", strArrMakeProjectionWithDateAndThreadId, hashSet, 1, "mms", str5, null, null);
        String strBuildUnionSubQuery2 = sQLiteQueryBuilder2.buildUnionSubQuery("transport_type", strArrMakeProjectionWithDateAndThreadId2, SMS_COLUMNS, 1, "sms", strConcatSelections2, null, null);
        SQLiteQueryBuilder sQLiteQueryBuilder3 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder3.setDistinct(true);
        String strBuildUnionQuery = sQLiteQueryBuilder3.buildUnionQuery(new String[]{strBuildUnionSubQuery2, strBuildUnionSubQuery}, null, null);
        SQLiteQueryBuilder sQLiteQueryBuilder4 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder4.setTables("(" + strBuildUnionQuery + ")");
        return this.mOpenHelper.getReadableDatabase().rawQuery(sQLiteQueryBuilder4.buildQuery(strArrHandleNullMessageProjection, null, null, null, str2, null), EMPTY_STRING_ARRAY);
    }

    public static String[] makeProjectionWithNormalizedDate(String[] strArr, int i) {
        int length = strArr.length;
        String[] strArr2 = new String[length + 1];
        strArr2[0] = "date * " + i + " AS normalized_date";
        System.arraycopy(strArr, 0, strArr2, 1, length);
        return strArr2;
    }

    private static String buildConversationQuery(String[] strArr, String str, String str2, String str3, String str4) {
        String[] strArrCreateMmsProjection = createMmsProjection(strArr, str4);
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setDistinct(true);
        sQLiteQueryBuilder2.setDistinct(true);
        sQLiteQueryBuilder.setTables(joinPduAndPendingMsgTables(str4));
        sQLiteQueryBuilder2.setTables(str3);
        String[] strArrHandleNullMessageProjection = handleNullMessageProjection(strArr);
        String[] strArrMakeProjectionWithNormalizedDate = makeProjectionWithNormalizedDate(handleNullMessageProjection(strArrCreateMmsProjection), 1000);
        String[] strArrMakeProjectionWithNormalizedDate2 = makeProjectionWithNormalizedDate(strArrHandleNullMessageProjection, 1);
        HashSet hashSet = new HashSet(MMS_COLUMNS);
        hashSet.add(str4 + "._id");
        hashSet.add("err_type");
        String strBuildUnionSubQuery = sQLiteQueryBuilder.buildUnionSubQuery("transport_type", strArrMakeProjectionWithNormalizedDate, hashSet, 0, "mms", concatSelections(concatSelections(str, "msg_box != 3"), "(msg_box != 3 AND (m_type = 128 OR m_type = 132 OR m_type = 130))"), null, null);
        String strBuildUnionSubQuery2 = sQLiteQueryBuilder2.buildUnionSubQuery("transport_type", strArrMakeProjectionWithNormalizedDate2, SMS_COLUMNS, 0, "sms", concatSelections(str, "(type != 3)"), null, null);
        SQLiteQueryBuilder sQLiteQueryBuilder3 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder3.setDistinct(true);
        sQLiteQueryBuilder3.setTables("cellbroadcast");
        String strBuildUnionSubQuery3 = sQLiteQueryBuilder3.buildUnionSubQuery("transport_type", makeProjectionWithNormalizedDate(handleNullMessageProjection(strArr), 1), CB_COLUMNS, 0, "cellbroadcast", str, null, null);
        SQLiteQueryBuilder sQLiteQueryBuilder4 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder4.setDistinct(true);
        String strBuildUnionQuery = sQLiteQueryBuilder4.buildUnionQuery(new String[]{strBuildUnionSubQuery2, strBuildUnionSubQuery, strBuildUnionSubQuery3}, handleNullSortOrder(str2), null);
        SQLiteQueryBuilder sQLiteQueryBuilder5 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder5.setTables("(" + strBuildUnionQuery + ")");
        return sQLiteQueryBuilder5.buildQuery(strArrHandleNullMessageProjection, null, null, null, str2, null);
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android-dir/mms-sms";
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int i;
        MmsProviderLog.dpi("Mms/Provider/MmsSms", "delete begin, uri = " + uri + ", selection = " + str);
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        Context context = getContext();
        int iMatch = URI_MATCHER.match(uri);
        int iDelete = 0;
        if (iMatch == 11) {
            String str2 = "status=0 AND _id NOT IN (SELECT DISTINCT thread_id FROM sms where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM cellbroadcast where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM pdu where thread_id NOT NULL)";
            if (MTK_WAPPUSH_SUPPORT) {
                str2 = "status=0 AND _id NOT IN (SELECT DISTINCT thread_id FROM sms where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM cellbroadcast where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM pdu where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM wappush where thread_id NOT NULL)";
            }
            iDelete = writableDatabase.delete("threads", str2, null) + writableDatabase.delete("threads", "recipient_ids = \"\" AND status=0", null);
            if (iDelete > 0) {
                Log.d("Mms/Provider/MmsSms", "delete,  delete obsolete threads end, removeOrphanedAddresses start");
                MmsSmsDatabaseHelper.removeOrphanedAddresses(writableDatabase);
            }
            deleteIPMsgWallPaper(writableDatabase, str2);
        } else if (iMatch == 19) {
            iDelete = writableDatabase.delete("quicktext", str, strArr);
        } else if (iMatch == 27) {
            iDelete = writableDatabase.delete("cellbroadcast", str, strArr);
        } else if (iMatch != 41) {
            switch (iMatch) {
                case 0:
                    String queryParameter = uri.getQueryParameter("multidelete");
                    Log.d("Mms/Provider/MmsSms", "delete URI_CONVERSATIONS begin, multidelete = " + queryParameter);
                    if (queryParameter != null && queryParameter.equals("true")) {
                        long[] jArr = new long[strArr.length];
                        int iDeleteConversation = 0;
                        int i2 = 0;
                        for (int length = strArr.length; iDelete < length; length = i) {
                            try {
                                long j = Long.parseLong(strArr[iDelete]);
                                Uri uriWithAppendedId = ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, j);
                                String queryParameter2 = uri.getQueryParameter("smsId");
                                String queryParameter3 = uri.getQueryParameter("mmsId");
                                if (!TextUtils.isEmpty(queryParameter2)) {
                                    i = length;
                                    uriWithAppendedId = uriWithAppendedId.buildUpon().appendQueryParameter("smsId", queryParameter2).build();
                                } else {
                                    i = length;
                                }
                                if (!TextUtils.isEmpty(queryParameter3)) {
                                    uriWithAppendedId = uriWithAppendedId.buildUpon().appendQueryParameter("mmsId", queryParameter3).build();
                                }
                                iDeleteConversation += deleteConversation(writableDatabase, uriWithAppendedId, str, null);
                                jArr[i2] = j;
                                iDelete++;
                                i2++;
                            } catch (NumberFormatException e) {
                                Log.e("Mms/Provider/MmsSms", "Thread ID must be a long.");
                            }
                        }
                        Log.d("Mms/Provider/MmsSms", "multi delete URI_CONVERSATIONS end");
                        if (iDeleteConversation > 0) {
                            MmsSmsDatabaseHelper.updateMultiThreads(writableDatabase, jArr);
                        }
                        iDelete = iDeleteConversation;
                    } else {
                        iDelete = deleteAllConversation(writableDatabase, uri, str, strArr);
                        Log.d("Mms/Provider/MmsSms", "delete URI_CONVERSATIONS end");
                        MmsSmsDatabaseHelper.updateAllThreads(writableDatabase, null, null);
                    }
                    break;
                case 1:
                    try {
                        long j2 = Long.parseLong(uri.getLastPathSegment());
                        iDelete = deleteConversation(writableDatabase, uri, str, strArr);
                        Log.d("Mms/Provider/MmsSms", "delete,  deleteConversation end, updateThread start");
                        MmsSmsDatabaseHelper.updateThread(writableDatabase, j2);
                    } catch (NumberFormatException e2) {
                        Log.e("Mms/Provider/MmsSms", "Thread ID must be a long.");
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("MmsSmsProvider does not support deletes, inserts, or updates for this URI.");
            }
        } else {
            try {
                long j3 = Long.parseLong(uri.getLastPathSegment());
                String str3 = "status=0 AND _id = ? AND _id NOT IN (SELECT DISTINCT thread_id FROM sms where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM cellbroadcast where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM pdu where thread_id NOT NULL)";
                if (MTK_WAPPUSH_SUPPORT) {
                    str3 = "status=0 AND _id = ? AND _id NOT IN (SELECT DISTINCT thread_id FROM sms where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM cellbroadcast where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM pdu where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM wappush where thread_id NOT NULL)";
                }
                iDelete = writableDatabase.delete("threads", str3, new String[]{String.valueOf(j3)}) + writableDatabase.delete("threads", "recipient_ids = \"\" AND status=0", null);
                if (iDelete > 0) {
                    Log.d("Mms/Provider/MmsSms", "delete,  delete obsolete thread end, removeOrphanedAddresses start");
                    MmsSmsDatabaseHelper.removeOrphanedAddresses(writableDatabase);
                }
                deleteIPMsgWallPaper(writableDatabase, str3);
            } catch (NumberFormatException e3) {
                Log.e("Mms/Provider/MmsSms", "Thread ID must be a long.");
            }
        }
        if (iDelete > 0) {
            context.getContentResolver().notifyChange(Telephony.MmsSms.CONTENT_URI, null, true, -1);
            notifyChange();
        }
        Log.d("Mms/Provider/MmsSms", "delete end, affectedRows = " + iDelete);
        return iDelete;
    }

    private int deleteConversation(SQLiteDatabase sQLiteDatabase, Uri uri, String str, String[] strArr) {
        String strConcatSelections;
        String strConcatSelections2;
        String strConcatSelections3 = concatSelections(str, "thread_id = " + uri.getLastPathSegment());
        String queryParameter = uri.getQueryParameter("smsId");
        String queryParameter2 = uri.getQueryParameter("mmsId");
        Log.d("Mms/Provider/MmsSms", "deleteConversation get max message smsId = " + queryParameter + " mmsId =" + queryParameter2);
        if (queryParameter != null) {
            strConcatSelections = concatSelections(strConcatSelections3, "_id<=" + queryParameter);
        } else {
            strConcatSelections = strConcatSelections3;
        }
        if (queryParameter2 != null) {
            strConcatSelections2 = concatSelections(strConcatSelections3, "_id<=" + queryParameter2);
        } else {
            strConcatSelections2 = strConcatSelections3;
        }
        return MmsProvider.deleteMessages(getContext(), sQLiteDatabase, strConcatSelections2, strArr, uri, false) + SmsProvider.deleteMessages(sQLiteDatabase, strConcatSelections, strArr) + sQLiteDatabase.delete("cellbroadcast", strConcatSelections3, strArr);
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Log.d("Mms/Provider/MmsSms", "insert begin, uri = " + uri + ", values = " + contentValues);
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        int iMatch = URI_MATCHER.match(uri);
        if (iMatch == 6) {
            return Uri.parse(uri + "/" + writableDatabase.insert("pending_msgs", null, contentValues));
        }
        if (iMatch == 19) {
            writableDatabase.insertOrThrow("quicktext", null, contentValues);
            return uri;
        }
        if (iMatch == 44) {
            return getDatabaseSize(uri);
        }
        throw new UnsupportedOperationException("MmsSmsProvider does not support deletes, inserts, or updates for this URI." + uri);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int callingUid = Binder.getCallingUid();
        String callingPackage = getCallingPackage();
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        Log.d("Mms/Provider/MmsSms", "update begin, uri = " + uri + ", values = " + contentValues + ", selection = " + str);
        int iUpdate = 0;
        switch (URI_MATCHER.match(uri)) {
            case 0:
                ContentValues contentValues2 = new ContentValues(1);
                if (contentValues.containsKey("archived")) {
                    contentValues2.put("archived", contentValues.getAsBoolean("archived"));
                }
                iUpdate = writableDatabase.update("threads", contentValues2, str, strArr);
                break;
            case 1:
                String str2 = uri.getPathSegments().get(1);
                String queryParameter = uri.getQueryParameter("isRcse");
                if (queryParameter != null && queryParameter.equals("true")) {
                    iUpdate = writableDatabase.update("threads", contentValues, concatSelections(str, "_id=" + str2), strArr);
                } else {
                    iUpdate = updateConversation(str2, contentValues, str, strArr, callingUid, callingPackage);
                }
                break;
            case 5:
                String str3 = "_id=" + uri.getPathSegments().get(1);
                if (!TextUtils.isEmpty(str)) {
                    str3 = str3 + " AND " + str;
                }
                iUpdate = writableDatabase.update("canonical_addresses", contentValues, str3, null);
                break;
            case 6:
                iUpdate = writableDatabase.update("pending_msgs", contentValues, str, null);
                break;
            case 19:
                iUpdate = writableDatabase.update("quicktext", contentValues, str, strArr);
                break;
            case 26:
                try {
                    long j = Long.parseLong(uri.getLastPathSegment());
                    Log.d("Mms/Provider/MmsSms", "update URI_STATUS Thread ID is " + j);
                    iUpdate = writableDatabase.update("threads", contentValues, "_id = " + Long.toString(j), null);
                    Log.d("Mms/Provider/MmsSms", "update URI_STATUS ok");
                } catch (NumberFormatException e) {
                    Log.e("Mms/Provider/MmsSms", "Thread ID must be a long.");
                }
                break;
            case 31:
                iUpdate = writableDatabase.update("thread_settings", contentValues, str, strArr);
                break;
            case 32:
                String path = getContext().getDir("wallpaper", 0).getPath();
                String asString = contentValues.getAsString("_data");
                MmsProviderLog.dpi("Mms/Provider/MmsSms", "wallpaperPath: " + asString);
                if (asString != null) {
                    Log.d("Mms/Provider/MmsSms", "wallpaperPath: exsited");
                    String str4 = uri.getPathSegments().get(1) + ".jpeg";
                    if (str4.equals("0.jpeg")) {
                        File file = new File(path, "general_wallpaper.jpeg");
                        if (file.exists()) {
                            Log.d("Mms/Provider/MmsSms", "isDelete " + file.delete());
                        }
                    } else {
                        String[] list = new File(path).list();
                        int length = list.length;
                        Log.d("Mms/Provider/MmsSms", "i: " + length);
                        if (length > 0) {
                            while (iUpdate < length) {
                                if (str4.equals(list[iUpdate])) {
                                    Log.d("Mms/Provider/MmsSms", "isDelete " + new File(path, list[iUpdate]).delete());
                                }
                                iUpdate++;
                            }
                        }
                    }
                }
                String str5 = "thread_id=" + uri.getPathSegments().get(1);
                if (!TextUtils.isEmpty(str)) {
                    str5 = str5 + " AND " + str;
                }
                iUpdate = writableDatabase.update("thread_settings", contentValues, str5, strArr);
                break;
            default:
                throw new UnsupportedOperationException("MmsSmsProvider does not support deletes, inserts, or updates for this URI." + uri);
        }
        if (iUpdate > 0) {
            notifyChange();
        }
        Log.d("Mms/Provider/MmsSms", "update end, affectedRows = " + iUpdate);
        return iUpdate;
    }

    private int updateConversation(String str, ContentValues contentValues, String str2, String[] strArr, int i, String str3) {
        try {
            Long.parseLong(str);
            if (ProviderUtil.shouldRemoveCreator(contentValues, i)) {
                Log.w("Mms/Provider/MmsSms", str3 + " tries to update CREATOR");
                contentValues.remove("creator");
                contentValues.remove("creator");
            }
            SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
            String strConcatSelections = concatSelections(str2, "thread_id=" + str);
            return writableDatabase.update("pdu", contentValues, strConcatSelections, strArr) + writableDatabase.update("sms", contentValues, strConcatSelections, strArr) + writableDatabase.update("cellbroadcast", contentValues, strConcatSelections, strArr);
        } catch (NumberFormatException e) {
            Log.e("Mms/Provider/MmsSms", "Thread ID must be a Long.");
            return 0;
        }
    }

    private static void initializeColumnSets() {
        int length = MMS_SMS_COLUMNS.length;
        int length2 = MMS_ONLY_COLUMNS.length;
        int length3 = SMS_ONLY_COLUMNS.length;
        int length4 = CB_ONLY_COLUMNS.length;
        HashSet hashSet = new HashSet();
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            MMS_COLUMNS.add(MMS_SMS_COLUMNS[i2]);
            SMS_COLUMNS.add(MMS_SMS_COLUMNS[i2]);
            CB_COLUMNS.add(MMS_SMS_COLUMNS[i2]);
            hashSet.add(MMS_SMS_COLUMNS[i2]);
        }
        for (int i3 = 0; i3 < length2; i3++) {
            MMS_COLUMNS.add(MMS_ONLY_COLUMNS[i3]);
            hashSet.add(MMS_ONLY_COLUMNS[i3]);
        }
        for (int i4 = 0; i4 < length3; i4++) {
            SMS_COLUMNS.add(SMS_ONLY_COLUMNS[i4]);
            hashSet.add(SMS_ONLY_COLUMNS[i4]);
        }
        for (int i5 = 0; i5 < length4; i5++) {
            CB_COLUMNS.add(CB_ONLY_COLUMNS[i5]);
        }
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            UNION_COLUMNS[i] = (String) it.next();
            i++;
        }
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(getContext());
        if (TextUtils.isEmpty(defaultSmsPackage)) {
            defaultSmsPackage = "None";
        }
        printWriter.println("Default SMS app: " + defaultSmsPackage);
    }

    private Cursor getMsgInfo(SQLiteDatabase sQLiteDatabase, long j, String str) {
        String str2;
        String str3 = " thread_id=" + j;
        String str4 = " thread_id=" + j + " AND (m_type=128 OR m_type=130 OR m_type=132)";
        if (str != null) {
            str2 = String.format(" SELECT _id, type AS msg_box, date FROM sms WHERE " + concatSelections(str, str3) + " UNION  SELECT _id, msg_box, date*1000 AS date FROM pdu WHERE " + concatSelections(str, str4), new Object[0]);
        } else {
            str2 = String.format(" SELECT _id, type AS msg_box, date FROM sms WHERE " + str3 + " UNION  SELECT _id, msg_box, date*1000 AS date FROM pdu WHERE " + str4 + " ORDER BY date DESC LIMIT 1", new Object[0]);
        }
        Log.d("Mms/Provider/MmsSms", "getMsgBox begin rawQuery = " + str2);
        return sQLiteDatabase.rawQuery(str2, null);
    }

    private Cursor getConversationSettingsById(String str, String[] strArr, String str2, String[] strArr2, String str3) {
        try {
            Long.parseLong(str);
            String strConcatSelections = concatSelections(str2, "thread_id=" + str);
            SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
            if (strArr == null) {
                strArr = THREAD_SETTINGS_COLUMNS;
            }
            sQLiteQueryBuilder.setDistinct(true);
            sQLiteQueryBuilder.setTables("thread_settings");
            return sQLiteQueryBuilder.query(this.mOpenHelper.getReadableDatabase(), strArr, strConcatSelections, strArr2, str3, null, null);
        } catch (NumberFormatException e) {
            Log.e("Mms/Provider/MmsSms", "Thread ID must be a Long.");
            return null;
        }
    }

    public static String queryIdAndFormatIn(SQLiteDatabase sQLiteDatabase, String str) {
        Log.d("Mms/Provider/MmsSms", "queryIdAndFormatIn sql is: " + str);
        Cursor cursorRawQuery = null;
        if (str != null && str.trim() != "") {
            cursorRawQuery = sQLiteDatabase.rawQuery(str, null);
        }
        if (cursorRawQuery == null) {
            return " IN () ";
        }
        try {
            Log.d("Mms/Provider/MmsSms", "queryIdAndFormatIn Cursor count is: " + cursorRawQuery.getCount());
            HashSet hashSet = new HashSet();
            while (cursorRawQuery.moveToNext()) {
                hashSet.add(Long.valueOf(cursorRawQuery.getLong(0)));
            }
            String strReplace = (" IN " + hashSet.toString()).replace('[', '(').replace(']', ')');
            Log.d("Mms/Provider/MmsSms", "queryIdAndFormatIn, In = " + strReplace);
            return strReplace;
        } finally {
            cursorRawQuery.close();
        }
    }

    private static Cursor getAllUnreadCount(SQLiteDatabase sQLiteDatabase) {
        Log.d("Mms/Provider/MmsSms", "getAllUnreadCount begin");
        return sQLiteDatabase.rawQuery("select sum(message_count - readcount) as unreadcount from threads where read = 0 and type<>2", null);
    }

    private Cursor getSimidListByThread(SQLiteDatabase sQLiteDatabase, long j) {
        String str = String.format("SELECT DISTINCT sub_id FROM(SELECT DISTINCT sub_id FROM sms WHERE thread_id=" + j + " AND type=1 UNION SELECT DISTINCT sub_id FROM pdu WHERE thread_id=" + j + " AND msg_box=1)", new Object[0]);
        StringBuilder sb = new StringBuilder();
        sb.append("getSimidListByThread begin rawQuery = ");
        sb.append(str);
        Log.d("Mms/Provider/MmsSms", sb.toString());
        return sQLiteDatabase.rawQuery(str, null);
    }

    private Cursor getRecipientsNumber(String str) {
        String str2 = String.format("SELECT recipient_ids FROM threads WHERE _id = " + str, new Object[0]);
        MmsProviderLog.dpi("Mms/Provider/MmsSms", "getRecipientsNumber " + str2);
        return this.mOpenHelper.getReadableDatabase().rawQuery(str2, EMPTY_STRING_ARRAY);
    }

    private HashMap<String, String> getContactsByNumber(String str) throws Throwable {
        Cursor cursorQuery;
        IllegalArgumentException e;
        Uri.Builder builderBuildUpon = PICK_PHONE_EMAIL_FILTER_URI.buildUpon();
        builderBuildUpon.appendPath(str);
        MmsProviderLog.dpi("Mms/Provider/MmsSms", "getContactsByNumber uri = " + builderBuildUpon.build().toString());
        HashMap<String, String> map = new HashMap<>();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            cursorQuery = getContext().getContentResolver().query(builderBuildUpon.build(), new String[]{"display_name", "data1"}, null, null, "sort_key");
            try {
                try {
                    Log.d("Mms/Provider/MmsSms", "getContactsByNumber getContentResolver query contact 1 cursor " + cursorQuery.getCount());
                    while (cursorQuery.moveToNext()) {
                        String string = cursorQuery.getString(0);
                        String validNumber = getValidNumber(cursorQuery.getString(1));
                        MmsProviderLog.dpi("Mms/Provider/MmsSms", "getContactsByNumber number = " + validNumber + " name = " + string);
                        map.put(validNumber, string);
                    }
                } catch (IllegalArgumentException e2) {
                    e = e2;
                    Log.d("Mms/Provider/MmsSms", e.toString());
                    if (cursorQuery != null) {
                    }
                }
            } catch (Throwable th) {
                th = th;
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        } catch (IllegalArgumentException e3) {
            e = e3;
            cursorQuery = null;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
            if (cursorQuery != null) {
            }
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
        return map;
    }

    private String searchContacts(String str, HashMap<String, String> map) {
        HashSet hashSet = new HashSet();
        Cursor cursorRawQuery = this.mOpenHelper.getReadableDatabase().rawQuery("SELECT _id,recipient_ids FROM threads", null);
        Cursor cursorRawQuery2 = this.mOpenHelper.getReadableDatabase().rawQuery("SELECT _id, address FROM canonical_addresses ", null);
        try {
            HashMap map2 = new HashMap();
            while (cursorRawQuery2.moveToNext()) {
                map2.put(Integer.valueOf(cursorRawQuery2.getInt(0)), cursorRawQuery2.getString(1));
            }
            while (cursorRawQuery.moveToNext()) {
                if (!TextUtils.isEmpty(cursorRawQuery.getString(1))) {
                    Long lValueOf = Long.valueOf(cursorRawQuery.getLong(0));
                    Iterator<String> it = getRecipientNumbers(cursorRawQuery.getString(1), map2).iterator();
                    while (true) {
                        if (it.hasNext()) {
                            String next = it.next();
                            if (next.toLowerCase().contains(str.toLowerCase())) {
                                hashSet.add(lValueOf);
                                break;
                            }
                            String str2 = map.get(next);
                            if (str2 == null) {
                                Iterator<String> it2 = map.keySet().iterator();
                                while (true) {
                                    if (!it2.hasNext()) {
                                        break;
                                    }
                                    String next2 = it2.next();
                                    if (PhoneNumberUtils.compare(next2, next)) {
                                        str2 = map.get(next2);
                                        break;
                                    }
                                }
                            }
                            if (str2 != null && str2.toLowerCase().contains(str.toLowerCase())) {
                                hashSet.add(lValueOf);
                                break;
                            }
                        }
                    }
                }
            }
            cursorRawQuery.close();
            cursorRawQuery2.close();
            Log.d("Mms/Provider/MmsSms", "searchContacts getContentResolver query recipient");
            String strReplace = (" IN " + hashSet.toString()).replace('[', '(').replace(']', ')');
            Log.d("Mms/Provider/MmsSms", "searchContacts in = " + strReplace);
            return strReplace;
        } catch (Throwable th) {
            cursorRawQuery.close();
            cursorRawQuery2.close();
            throw th;
        }
    }

    public static String getValidNumber(String str) {
        if (str == null) {
            return null;
        }
        String strReplaceAll = new String(str).replaceAll(" ", "").replaceAll("-", "");
        if (str.equals("Self_Item_Key") || Telephony.Mms.isEmailAddress(str)) {
            return str;
        }
        if (PhoneNumberUtils.isWellFormedSmsAddress(strReplaceAll)) {
            return strReplaceAll;
        }
        String number = PhoneNumberUtils.formatNumber(PhoneNumberUtils.stripSeparators(strReplaceAll));
        if (str.equals(number)) {
            return PhoneNumberUtils.stripSeparators(number);
        }
        return str;
    }

    public static Set<String> getRecipientNumbers(String str, HashMap<Integer, String> map) {
        HashSet hashSet = new HashSet();
        if (map == null || map.size() <= 0) {
            Log.d("Mms/Provider/MmsSms", "getRecipientNumbers contacts is null");
            return hashSet;
        }
        String[] strArrSplit = str.split(" ");
        for (int i = 0; i < strArrSplit.length; i++) {
            if (map.containsKey(Integer.valueOf(Integer.parseInt(strArrSplit[i])))) {
                hashSet.add(map.get(Integer.valueOf(Integer.parseInt(strArrSplit[i]))));
            }
        }
        return hashSet;
    }

    private Set<String> searchRecipients(String str) {
        HashSet hashSet = new HashSet();
        if (TextUtils.isEmpty(str)) {
            return hashSet;
        }
        String[] strArrSplit = str.split(" ");
        if (strArrSplit.length > 0) {
            String str2 = " IN (" + TextUtils.join(",", strArrSplit) + ") ";
            Cursor cursorRawQuery = this.mOpenHelper.getReadableDatabase().rawQuery("SELECT address FROM canonical_addresses WHERE _id" + str2, null);
            if (cursorRawQuery != null) {
                try {
                    if (cursorRawQuery.getCount() != 0) {
                        while (cursorRawQuery.moveToNext()) {
                            String string = cursorRawQuery.getString(0);
                            if (!TextUtils.isEmpty(string) && !string.trim().isEmpty()) {
                                hashSet.add(string);
                            }
                        }
                    }
                } finally {
                    cursorRawQuery.close();
                }
            }
            Log.d("Mms/Provider/MmsSms", "searchRecipients cursor is null");
            return hashSet;
        }
        return hashSet;
    }

    private long insertCanonicalAddresses(SQLiteOpenHelper sQLiteOpenHelper, String str) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put("address", str);
        return this.mOpenHelper.getWritableDatabase().insert("canonical_addresses", "address", contentValues);
    }

    private void insertWapPushThread(String str, int i) {
        ContentValues contentValues = new ContentValues(4);
        long jCurrentTimeMillis = System.currentTimeMillis();
        contentValues.put("date", Long.valueOf(jCurrentTimeMillis - (jCurrentTimeMillis % 1000)));
        contentValues.put("recipient_ids", str);
        contentValues.put("type", (Integer) 2);
        MmsProviderLog.dpi("WapPush/Provider", "insertThread: created new thread_id " + this.mOpenHelper.getWritableDatabase().insert("threads", null, contentValues) + " for recipientIds " + str);
        Log.w("WapPush/Provider", "insertWapPushThread!");
        notifyChange();
    }

    private synchronized Cursor getWapPushThreadId(List<String> list) {
        Cursor cursorRawQuery;
        Set<Long> addressIds = getAddressIds(list);
        String spaceSeparatedNumbers = "";
        if (addressIds.size() == 1) {
            Iterator<Long> it = addressIds.iterator();
            while (it.hasNext()) {
                spaceSeparatedNumbers = Long.toString(it.next().longValue());
            }
        } else {
            spaceSeparatedNumbers = getSpaceSeparatedNumbers(getSortedSet(addressIds));
        }
        if (Log.isLoggable("Mms/Provider/MmsSms", 2)) {
            MmsProviderLog.dpi("Mms/Provider/MmsSms", "getWapPushThreadId: recipientIds (selectionArgs) =" + spaceSeparatedNumbers);
        }
        String[] strArr = {spaceSeparatedNumbers};
        cursorRawQuery = this.mOpenHelper.getReadableDatabase().rawQuery("SELECT _id FROM threads WHERE type=2 AND recipient_ids=?", strArr);
        if (cursorRawQuery.getCount() == 0) {
            cursorRawQuery.close();
            MmsProviderLog.dpi("Mms/Provider/MmsSms", "getWapPushThreadId: create new thread_id for recipients " + list);
            insertWapPushThread(spaceSeparatedNumbers, list.size());
            cursorRawQuery = this.mOpenHelper.getReadableDatabase().rawQuery("SELECT _id FROM threads WHERE type=2 AND recipient_ids=?", strArr);
        }
        if (cursorRawQuery.getCount() > 1) {
            Log.w("Mms/Provider/MmsSms", "getWapPushThreadId: why is cursorCount=" + cursorRawQuery.getCount());
        }
        return cursorRawQuery;
    }

    private void insertCBThread(String str, int i) {
        ContentValues contentValues = new ContentValues(4);
        long jCurrentTimeMillis = System.currentTimeMillis();
        contentValues.put("date", Long.valueOf(jCurrentTimeMillis - (jCurrentTimeMillis % 1000)));
        contentValues.put("recipient_ids", str);
        contentValues.put("type", (Integer) 3);
        MmsProviderLog.dpi("Mms/Provider/MmsSms", "insertThread: created new thread_id " + this.mOpenHelper.getWritableDatabase().insert("threads", null, contentValues) + " for recipientIds " + str);
        Log.w("Mms/Provider/MmsSms", "insertCBThread!");
        notifyChange();
    }

    private synchronized Cursor getCBThreadId(List<String> list) {
        Cursor cursorRawQuery;
        Set<Long> addressIds = getAddressIds(list);
        String spaceSeparatedNumbers = "";
        if (addressIds.size() == 1) {
            Iterator<Long> it = addressIds.iterator();
            while (it.hasNext()) {
                spaceSeparatedNumbers = Long.toString(it.next().longValue());
            }
        } else {
            spaceSeparatedNumbers = getSpaceSeparatedNumbers(getSortedSet(addressIds));
        }
        if (Log.isLoggable("Mms/Provider/MmsSms", 2)) {
            MmsProviderLog.dpi("Mms/Provider/MmsSms", "getCBThreadId: recipientIds (selectionArgs) =" + spaceSeparatedNumbers);
        }
        String[] strArr = {spaceSeparatedNumbers};
        cursorRawQuery = this.mOpenHelper.getReadableDatabase().rawQuery("SELECT _id FROM threads WHERE type=3 AND recipient_ids=?", strArr);
        if (cursorRawQuery.getCount() == 0) {
            cursorRawQuery.close();
            MmsProviderLog.dpi("Mms/Provider/MmsSms", "getCBThreadId: create new thread_id for recipients " + list);
            insertCBThread(spaceSeparatedNumbers, list.size());
            cursorRawQuery = this.mOpenHelper.getReadableDatabase().rawQuery("SELECT _id FROM threads WHERE type=3 AND recipient_ids=?", strArr);
        }
        if (cursorRawQuery.getCount() > 1) {
            Log.w("Mms/Provider/MmsSms", "getCBThreadId: why is cursorCount=" + cursorRawQuery.getCount());
        }
        return cursorRawQuery;
    }

    private void notifyChange() {
        Log.i("Mms/Provider/MmsSms", "notifyChange");
        getContext().getContentResolver().notifyChange(Telephony.MmsSms.CONTENT_URI, null);
        notifyUnreadMessageNumberChanged(getContext());
    }

    private static int getUnreadMessageNumber(Context context) {
        Cursor cursorRawQuery = MmsSmsDatabaseHelper.getInstanceForCe(context).getReadableDatabase().rawQuery("select sum(message_count - readcount) as unreadcount from threads where read = 0 and type<>2", null);
        int i = 0;
        if (cursorRawQuery != null) {
            try {
                if (cursorRawQuery.moveToFirst()) {
                    i = cursorRawQuery.getInt(0);
                    Log.d("Mms/Provider/MmsSms", "get threads unread message count = " + i);
                }
            } finally {
                cursorRawQuery.close();
            }
        } else {
            Log.d("Mms/Provider/MmsSms", "can not get unread message count.");
        }
        return i;
    }

    public static void broadcastUnreadMessageNumber(Context context, int i) {
    }

    public static void recordUnreadMessageNumberToSys(Context context, int i) {
        Settings.System.putInt(context.getContentResolver(), "com_android_mms_mtk_unread", i);
    }

    public static void notifyUnreadMessageNumberChanged(Context context) {
        int unreadMessageNumber = getUnreadMessageNumber(context);
        recordUnreadMessageNumberToSys(context, unreadMessageNumber);
        broadcastUnreadMessageNumber(context, unreadMessageNumber);
    }

    private int deleteAllConversation(SQLiteDatabase sQLiteDatabase, Uri uri, String str, String[] strArr) {
        String strConcatSelections;
        String str2;
        uri.getLastPathSegment();
        String queryParameter = uri.getQueryParameter("smsId");
        String queryParameter2 = uri.getQueryParameter("mmsId");
        Log.d("Mms/Provider/MmsSms", "deleteAllConversation get max message smsId = " + queryParameter + " mmsId =" + queryParameter2);
        String str3 = "";
        String str4 = "";
        String str5 = "";
        if (str != null && str.contains("locked=0")) {
            str3 = " AND source_id not in (select _id from sms where locked=1)";
            str4 = " AND source_id not in (select _id from part where mid in (select _id from pdu where locked=1))";
            str5 = " AND source_id not in (select _id from pdu where locked=1)";
        }
        if (queryParameter != null) {
            strConcatSelections = concatSelections(str, "_id<=" + queryParameter);
            sQLiteDatabase.execSQL("DELETE FROM words WHERE table_to_use=1 AND source_id<=" + queryParameter + str3 + ";");
        } else {
            sQLiteDatabase.execSQL("DELETE FROM words WHERE table_to_use=1" + str3 + ";");
            strConcatSelections = str;
        }
        if (queryParameter2 != null) {
            String str6 = "DELETE FROM words WHERE (table_to_use=2 AND source_id<= (SELECT max(_id) FROM part WHERE mid<=" + queryParameter2 + ")" + str4 + ") OR (table_to_use=4 AND source_id<=" + queryParameter2 + str5 + ")";
            String strConcatSelections2 = concatSelections(str, "_id<=" + queryParameter2);
            sQLiteDatabase.execSQL(str6);
            str2 = strConcatSelections2;
        } else {
            sQLiteDatabase.execSQL("DELETE FROM words WHERE (table_to_use=2" + str4 + ") OR (table_to_use=4" + str5 + ");");
            str2 = str;
        }
        return MmsProvider.deleteMessages(getContext(), sQLiteDatabase, str2, strArr, uri, false) + SmsProvider.deleteMessages(sQLiteDatabase, strConcatSelections, strArr) + sQLiteDatabase.delete("cellbroadcast", str, strArr);
    }

    private String getAllMaybeNumbers(String str) {
        String[] strArrSplit = str.split(":");
        HashSet hashSet = new HashSet();
        if (strArrSplit.length == 0) {
            MmsProviderLog.dpi("Mms/Provider/MmsSms", "contact history numbers: " + str);
            return "('" + str + "')";
        }
        for (String str2 : strArrSplit) {
            int length = str2.length();
            if (length > 11) {
                hashSet.add("'" + str2.substring(length - 11, length) + "'");
                hashSet.add("'" + str2 + "'");
            } else {
                hashSet.add("'+86" + str2 + "'");
                hashSet.add("'" + str2 + "'");
            }
        }
        String strReplace = hashSet.toString().replace('[', '(').replace(']', ')');
        MmsProviderLog.dpi("Mms/Provider/MmsSms", "contact history numbers: " + strReplace);
        return strReplace;
    }

    private String searchContactsAdvanced(String str, String str2, HashMap<String, String> map) {
        String str3;
        MmsProviderLog.dpi("Mms/Provider/MmsSms", "searchContactsAdvanced(): numberPattern = " + str + ", namePattern = " + str2);
        boolean zIsEmpty = TextUtils.isEmpty(str) ^ true;
        boolean zIsEmpty2 = TextUtils.isEmpty(str2) ^ true;
        if (!zIsEmpty && !zIsEmpty2) {
            Log.d("Mms/Provider/MmsSms", "searchContactsAdvanced(): no number and no name");
            return null;
        }
        HashSet hashSet = new HashSet();
        Cursor cursorRawQuery = this.mOpenHelper.getReadableDatabase().rawQuery("SELECT _id,recipient_ids FROM threads", null);
        while (cursorRawQuery.moveToNext()) {
            try {
                Long lValueOf = Long.valueOf(cursorRawQuery.getLong(0));
                Log.d("Mms/Provider/MmsSms", "searchContactsAdvanced(): threadId = " + lValueOf);
                for (String str4 : searchRecipients(cursorRawQuery.getString(1))) {
                    if (!zIsEmpty || str4.toLowerCase().contains(str.toLowerCase())) {
                        if (zIsEmpty2) {
                            if (map != null && map.size() != 0) {
                                Iterator<String> it = map.keySet().iterator();
                                while (true) {
                                    if (it.hasNext()) {
                                        String next = it.next();
                                        if (PhoneNumberUtils.compareStrictly(next, str4)) {
                                            str3 = map.get(next);
                                            break;
                                        }
                                    } else {
                                        str3 = null;
                                        break;
                                    }
                                }
                                if (str3 == null || !str3.toLowerCase().contains(str2.toLowerCase())) {
                                }
                            }
                        }
                        hashSet.add(lValueOf);
                        break;
                    }
                }
            } catch (Throwable th) {
                cursorRawQuery.close();
                throw th;
            }
        }
        cursorRawQuery.close();
        if (hashSet.size() > 0) {
            String strReplace = (" IN " + hashSet.toString()).replace('[', '(').replace(']', ')');
            Log.d("Mms/Provider/MmsSms", "searchContactsAdvanced(): searchContacts, in = " + strReplace);
            return strReplace;
        }
        Log.w("Mms/Provider/MmsSms", "searchContactsAdvanced(): threadIds.size() = 0.");
        return null;
    }

    private HashMap<String, String> getContactsByName(String str, boolean z, HashMap<String, String> map) throws Throwable {
        MmsProviderLog.dpi("Mms/Provider/MmsSms", "getContactsByName(): namePattern = " + str + ", hasNumber = " + z);
        if (TextUtils.isEmpty(str) || (z && (map == null || map.size() == 0))) {
            return map;
        }
        HashMap<String, String> contactsByNumber = getContactsByNumber(str);
        if (z) {
            map.putAll(contactsByNumber);
            return map;
        }
        return contactsByNumber;
    }

    private Cursor getConversationMessagesDistinct(String str, String[] strArr, String str2, String str3) {
        try {
            Long.parseLong(str);
            return this.mOpenHelper.getReadableDatabase().rawQuery(buildConversationQueryDistinct(strArr, concatSelections(str2, "thread_id = " + str), str3), EMPTY_STRING_ARRAY);
        } catch (NumberFormatException e) {
            Log.e("Mms/Provider/MmsSms", "Thread ID must be a Long.");
            return null;
        }
    }

    private static String joinPduAddrAndPendingMsgTables() {
        return "pdu, addr LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id";
    }

    private static String buildConversationQueryDistinct(String[] strArr, String str, String str2) {
        String[] strArrCreateMmsProjection = createMmsProjection(strArr, "pdu");
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setDistinct(true);
        sQLiteQueryBuilder2.setDistinct(true);
        sQLiteQueryBuilder.setTables(joinPduAddrAndPendingMsgTables());
        sQLiteQueryBuilder2.setTables("sms");
        String[] strArrHandleNullMessageProjection = handleNullMessageProjection(strArr);
        String[] strArrMakeProjectionWithNormalizedDate = makeProjectionWithNormalizedDate(handleNullMessageProjection(strArrCreateMmsProjection), 1000);
        String[] strArrMakeProjectionWithNormalizedDate2 = makeProjectionWithNormalizedDate(strArrHandleNullMessageProjection, 1);
        HashSet hashSet = new HashSet(MMS_COLUMNS);
        hashSet.add("pdu._id");
        hashSet.add("err_type");
        hashSet.add("group_concat(addr.address) as mms_cc");
        hashSet.add("NULL as mms_cc");
        hashSet.add("group_concat(addr.charset) as mms_cc_encoding");
        hashSet.add("NULL as mms_cc_encoding");
        String strConcatSelections = concatSelections(str, "msg_box != 3");
        String strBuildUnionSubQuery = sQLiteQueryBuilder2.buildUnionSubQuery("transport_type", strArrMakeProjectionWithNormalizedDate2, SMS_COLUMNS, 0, "sms", concatSelections(str, "(type != 3)"), "ipmsg_id", "ipmsg_id < 0");
        String strBuildUnionSubQuery2 = sQLiteQueryBuilder2.buildUnionSubQuery("transport_type", strArrMakeProjectionWithNormalizedDate2, SMS_COLUMNS, 0, "sms", concatSelections(str, "(type != 3) AND (ipmsg_id >= 0)"), null, null);
        SQLiteQueryBuilder sQLiteQueryBuilder3 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder3.setDistinct(true);
        sQLiteQueryBuilder3.setTables("cellbroadcast");
        String strBuildUnionSubQuery3 = sQLiteQueryBuilder3.buildUnionSubQuery("transport_type", makeProjectionWithNormalizedDate(handleNullMessageProjection(strArr), 1), CB_COLUMNS, 0, "cellbroadcast", str, null, null);
        String strBuildUnionSubQuery4 = sQLiteQueryBuilder.buildUnionSubQuery("transport_type", makeProjectionForcc(false, strArrMakeProjectionWithNormalizedDate), hashSet, 0, "mms", concatSelections(strConcatSelections, "(msg_box != 3 AND (m_type = 128 OR m_type = 132 OR m_type = 130)) AND addr.msg_id = pdu._id AND pdu._id not in(select msg_id from addr where addr.type = 130)"), null, null);
        String strBuildUnionSubQuery5 = sQLiteQueryBuilder.buildUnionSubQuery("transport_type", makeProjectionForcc(true, strArrMakeProjectionWithNormalizedDate), hashSet, 0, "mms", concatSelections(strConcatSelections, "(msg_box != 3 AND (m_type = 128 OR m_type = 132 OR m_type = 130)) AND addr.msg_id = pdu._id AND pdu._id in (select msg_id from addr where addr.type = 130) AND ADDR.type = 130"), "pdu._id", null);
        SQLiteQueryBuilder sQLiteQueryBuilder4 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder4.setDistinct(true);
        String strBuildUnionQuery = sQLiteQueryBuilder4.buildUnionQuery(new String[]{strBuildUnionSubQuery, strBuildUnionSubQuery2, strBuildUnionSubQuery4, strBuildUnionSubQuery5, strBuildUnionSubQuery3}, handleNullSortOrder(str2), null);
        SQLiteQueryBuilder sQLiteQueryBuilder5 = new SQLiteQueryBuilder();
        sQLiteQueryBuilder5.setTables("(" + strBuildUnionQuery + ")");
        return sQLiteQueryBuilder5.buildQuery(strArrHandleNullMessageProjection, null, null, null, str2, null);
    }

    private static String[] makeProjectionForcc(boolean z, String[] strArr) {
        int length = strArr.length;
        String[] strArr2 = new String[length];
        for (int i = 0; i < length; i++) {
            if (strArr[i].equals("mms_cc")) {
                if (z) {
                    strArr2[i] = "group_concat(addr.address) as mms_cc";
                } else {
                    strArr2[i] = "NULL as mms_cc";
                }
            } else if (strArr[i].equals("mms_cc_encoding")) {
                if (z) {
                    strArr2[i] = "group_concat(addr.charset) as mms_cc_encoding";
                } else {
                    strArr2[i] = "NULL as mms_cc_encoding";
                }
            } else {
                strArr2[i] = strArr[i];
            }
        }
        return strArr2;
    }

    private Cursor getSimpleConversationsExtend(String[] strArr, String str, String[] strArr2, String str2) {
        for (int i = 0; i < strArr.length; i++) {
            if (strArr[i].equals("_id")) {
                strArr[i] = "threads._id";
            }
        }
        String strConcatSelections = concatSelections(str, "threads._id=thread_settings.thread_id");
        Log.d("Mms/Provider/MmsSms", "extend query selection:" + strConcatSelections);
        return this.mOpenHelper.getReadableDatabase().query("threads,thread_settings", strArr, strConcatSelections, strArr2, null, null, " date DESC");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        return openFileHelper(uri, str);
    }

    private void deleteIPMsgWallPaper(SQLiteDatabase sQLiteDatabase, String str) {
        Log.d("Mms/Provider/MmsSms", "delete wallpaper from obsolete begin");
        Cursor cursorQuery = sQLiteDatabase.query("threads", new String[]{"_id"}, str, null, null, null, null);
        if (cursorQuery == null) {
            return;
        }
        try {
            File file = new File("/data/data/com.android.providers.telephony/app_wallpaper");
            if (file.exists()) {
                if (cursorQuery.getCount() == 0) {
                    Log.d("Mms/Provider/MmsSms", "Cursor count: 0");
                    return;
                }
                while (cursorQuery.moveToNext()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(cursorQuery.getInt(0));
                    sb.append(".jpeg");
                    String string = sb.toString();
                    String[] list = file.list();
                    int length = list.length;
                    Log.d("Mms/Provider/MmsSms", "i: " + length);
                    if (length > 0) {
                        for (String str2 : list) {
                            if (string.equals(list[length])) {
                                Log.d("Mms/Provider/MmsSms", "isDelete " + new File("/data/data/com.android.providers.telephony/app_wallpaper", str2).delete());
                            }
                        }
                    }
                }
            }
        } finally {
            cursorQuery.close();
        }
    }

    public static String toIsoString(byte[] bArr) {
        try {
            return new String(bArr, "iso-8859-1");
        } catch (UnsupportedEncodingException e) {
            Log.e("Mms/Provider/MmsSms", "ISO_8859_1 must be supported!", e);
            return "";
        } catch (NullPointerException e2) {
            return "";
        }
    }

    protected static String key(String str, CharBuffer charBuffer) {
        charBuffer.clear();
        charBuffer.mark();
        int length = str.length();
        int i = 0;
        while (true) {
            length--;
            if (length < 0) {
                break;
            }
            char cCharAt = str.charAt(length);
            if (PhoneNumberUtils.isDialable(cCharAt)) {
                charBuffer.put(cCharAt);
                i++;
                if (i == 7) {
                    break;
                }
            }
        }
        charBuffer.reset();
        if (i > 0) {
            return charBuffer.toString();
        }
        return str;
    }

    private Uri getDatabaseSize(Uri uri) {
        return uri.buildUpon().appendQueryParameter("size", String.valueOf(new File("/data/user_de/0/com.android.providers.telephony/databases/mmssms.db").length())).build();
    }

    public static final class MmsProviderLog {
        public static void vpi(String str, String str2) {
            if (MmsSmsProvider.piLoggable) {
                Log.v(str, str2);
            }
        }

        public static void epi(String str, String str2) {
            if (MmsSmsProvider.piLoggable) {
                Log.e(str, str2);
            }
        }

        public static void ipi(String str, String str2) {
            if (MmsSmsProvider.piLoggable) {
                Log.i(str, str2);
            }
        }

        public static void dpi(String str, String str2) {
            if (MmsSmsProvider.piLoggable) {
                Log.d(str, str2);
            }
        }
    }

    @Override
    public Bundle call(String str, String str2, Bundle bundle) {
        if ("is_restoring".equals(str)) {
            Bundle bundle2 = new Bundle();
            bundle2.putBoolean("restoring", TelephonyBackupAgent.getIsRestoring());
            return bundle2;
        }
        Log.w("Mms/Provider/MmsSms", "Ignored unsupported " + str + " call");
        return null;
    }
}
