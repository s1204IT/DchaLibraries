package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MmsSmsProvider extends ContentProvider {
    private SQLiteOpenHelper mOpenHelper;
    private boolean mUseStrictPhoneNumberComparation;
    private static final UriMatcher URI_MATCHER = new UriMatcher(-1);
    private static final String[] MMS_SMS_COLUMNS = {"_id", "date", "date_sent", "read", "thread_id", "locked", "sub_id"};
    private static final String[] MMS_ONLY_COLUMNS = {"ct_cls", "ct_l", "ct_t", "d_rpt", "exp", "m_cls", "m_id", "m_size", "m_type", "msg_box", "pri", "read_status", "resp_st", "resp_txt", "retr_st", "retr_txt_cs", "rpt_a", "rr", "st", "sub", "sub_cs", "tr_id", "v", "text_only"};
    private static final String[] SMS_ONLY_COLUMNS = {"address", "body", "person", "reply_path_present", "service_center", "status", "subject", "type", "error_code"};
    private static final String[] THREADS_COLUMNS = {"_id", "date", "recipient_ids", "message_count"};
    private static final String[] CANONICAL_ADDRESSES_COLUMNS_1 = {"address"};
    private static final String[] CANONICAL_ADDRESSES_COLUMNS_2 = {"_id", "address"};
    private static final String[] UNION_COLUMNS = new String[(MMS_SMS_COLUMNS.length + MMS_ONLY_COLUMNS.length) + SMS_ONLY_COLUMNS.length];
    private static final Set<String> MMS_COLUMNS = new HashSet();
    private static final Set<String> SMS_COLUMNS = new HashSet();
    private static final String[] ID_PROJECTION = {"_id"};
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String[] SEARCH_STRING = new String[1];

    static {
        URI_MATCHER.addURI("mms-sms", "conversations", 0);
        URI_MATCHER.addURI("mms-sms", "complete-conversations", 7);
        URI_MATCHER.addURI("mms-sms", "conversations/#", 1);
        URI_MATCHER.addURI("mms-sms", "conversations/#/recipients", 2);
        URI_MATCHER.addURI("mms-sms", "conversations/#/subject", 9);
        URI_MATCHER.addURI("mms-sms", "conversations/obsolete", 11);
        URI_MATCHER.addURI("mms-sms", "messages/byphone/*", 3);
        URI_MATCHER.addURI("mms-sms", "threadID", 4);
        URI_MATCHER.addURI("mms-sms", "canonical-address/#", 5);
        URI_MATCHER.addURI("mms-sms", "canonical-addresses", 13);
        URI_MATCHER.addURI("mms-sms", "search", 14);
        URI_MATCHER.addURI("mms-sms", "searchSuggest", 15);
        URI_MATCHER.addURI("mms-sms", "pending", 6);
        URI_MATCHER.addURI("mms-sms", "undelivered", 8);
        URI_MATCHER.addURI("mms-sms", "notifications", 10);
        URI_MATCHER.addURI("mms-sms", "draft", 12);
        URI_MATCHER.addURI("mms-sms", "locked", 16);
        URI_MATCHER.addURI("mms-sms", "locked/#", 17);
        URI_MATCHER.addURI("mms-sms", "messageIdToThread", 18);
        initializeColumnSets();
    }

    @Override
    public boolean onCreate() {
        setAppOps(14, 15);
        this.mOpenHelper = MmsSmsDatabaseHelper.getInstance(getContext());
        this.mUseStrictPhoneNumberComparation = getContext().getResources().getBoolean(android.R.^attr-private.defaultQueryHint);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int proto;
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        Cursor cursor = null;
        switch (URI_MATCHER.match(uri)) {
            case 0:
                String simple = uri.getQueryParameter("simple");
                if (simple != null && simple.equals("true")) {
                    String threadType = uri.getQueryParameter("thread_type");
                    if (!TextUtils.isEmpty(threadType)) {
                        selection = concatSelections(selection, "type=" + threadType);
                    }
                    cursor = getSimpleConversations(projection, selection, selectionArgs, sortOrder);
                } else {
                    cursor = getConversations(projection, selection, sortOrder);
                }
                break;
            case 1:
                cursor = getConversationMessages(uri.getPathSegments().get(1), projection, selection, sortOrder);
                break;
            case 2:
                cursor = getConversationById(uri.getPathSegments().get(1), projection, selection, selectionArgs, sortOrder);
                break;
            case 3:
                cursor = getMessagesByPhoneNumber(uri.getPathSegments().get(2), projection, selection, sortOrder);
                break;
            case 4:
                List<String> recipients = uri.getQueryParameters("recipient");
                cursor = getThreadId(recipients);
                break;
            case 5:
                String extraSelection = "_id=" + uri.getPathSegments().get(1);
                String finalSelection = TextUtils.isEmpty(selection) ? extraSelection : extraSelection + " AND " + selection;
                cursor = db.query("canonical_addresses", CANONICAL_ADDRESSES_COLUMNS_1, finalSelection, selectionArgs, null, null, sortOrder);
                break;
            case 6:
                String protoName = uri.getQueryParameter("protocol");
                String msgId = uri.getQueryParameter("message");
                if (TextUtils.isEmpty(protoName)) {
                    proto = -1;
                } else {
                    proto = protoName.equals("sms") ? 0 : 1;
                }
                String extraSelection2 = proto != -1 ? "proto_type=" + proto : " 0=0 ";
                if (!TextUtils.isEmpty(msgId)) {
                    extraSelection2 = extraSelection2 + " AND msg_id=" + msgId;
                }
                String finalSelection2 = TextUtils.isEmpty(selection) ? extraSelection2 : "(" + extraSelection2 + ") AND " + selection;
                String finalOrder = TextUtils.isEmpty(sortOrder) ? "due_time" : sortOrder;
                cursor = db.query("pending_msgs", null, finalSelection2, selectionArgs, null, null, finalOrder);
                break;
            case 7:
                cursor = getCompleteConversations(projection, selection, sortOrder);
                break;
            case 8:
                cursor = getUndeliveredMessages(projection, selection, selectionArgs, sortOrder);
                break;
            case 9:
                cursor = getConversationById(uri.getPathSegments().get(1), projection, selection, selectionArgs, sortOrder);
                break;
            case 10:
            case 11:
            default:
                throw new IllegalStateException("Unrecognized URI:" + uri);
            case 12:
                cursor = getDraftThread(projection, selection, sortOrder);
                break;
            case 13:
                cursor = db.query("canonical_addresses", CANONICAL_ADDRESSES_COLUMNS_2, selection, selectionArgs, null, null, sortOrder);
                break;
            case 14:
                if (sortOrder != null || selection != null || selectionArgs != null || projection != null) {
                    throw new IllegalArgumentException("do not specify sortOrder, selection, selectionArgs, or projectionwith this query");
                }
                String searchString = uri.getQueryParameter("pattern") + "*";
                try {
                    cursor = db.rawQuery("SELECT sms._id AS _id,thread_id,address,body,date,date_sent,index_text,words._id FROM sms,words WHERE (index_text MATCH ? AND sms._id=words.source_id AND words.table_to_use=1) UNION SELECT pdu._id,thread_id,addr.address,part.text AS body,pdu.date,pdu.date_sent,index_text,words._id FROM pdu,part,addr,words WHERE ((part.mid=pdu._id) AND (addr.msg_id=pdu._id) AND (addr.type=151) AND (part.ct='text/plain') AND (index_text MATCH ?) AND (part._id = words.source_id) AND (words.table_to_use=2)) GROUP BY thread_id ORDER BY thread_id ASC, date DESC", new String[]{searchString, searchString});
                } catch (Exception ex) {
                    Log.e("MmsSmsProvider", "got exception: " + ex.toString());
                }
                break;
                break;
            case 15:
                SEARCH_STRING[0] = uri.getQueryParameter("pattern") + '*';
                if (sortOrder != null || selection != null || selectionArgs != null || projection != null) {
                    throw new IllegalArgumentException("do not specify sortOrder, selection, selectionArgs, or projectionwith this query");
                }
                cursor = db.rawQuery("SELECT snippet(words, '', ' ', '', 1, 1) as snippet FROM words WHERE index_text MATCH ? ORDER BY snippet LIMIT 50;", SEARCH_STRING);
                break;
                break;
            case 16:
                cursor = getFirstLockedMessage(projection, selection, sortOrder);
                break;
            case 17:
                try {
                    long threadId = Long.parseLong(uri.getLastPathSegment());
                    cursor = getFirstLockedMessage(projection, "thread_id=" + Long.toString(threadId), sortOrder);
                } catch (NumberFormatException e) {
                    Log.e("MmsSmsProvider", "Thread ID must be a long.");
                }
                break;
            case 18:
                try {
                    long id = Long.parseLong(uri.getQueryParameter("row_id"));
                    switch (Integer.parseInt(uri.getQueryParameter("table_to_use"))) {
                        case 1:
                            cursor = db.query("sms", new String[]{"thread_id"}, "_id=?", new String[]{String.valueOf(id)}, null, null, null);
                            break;
                        case 2:
                            cursor = db.rawQuery("SELECT thread_id FROM pdu,part WHERE ((part.mid=pdu._id) AND (part._id=?))", new String[]{String.valueOf(id)});
                            break;
                    }
                } catch (NumberFormatException e2) {
                }
                break;
        }
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), Telephony.MmsSms.CONTENT_URI);
        }
        return cursor;
    }

    private long getSingleAddressId(String address) {
        String[] selectionArgs;
        boolean isEmail = Telephony.Mms.isEmailAddress(address);
        boolean isPhoneNumber = Telephony.Mms.isPhoneNumber(address);
        String refinedAddress = isEmail ? address.toLowerCase() : address;
        String selection = "address=?";
        long retVal = -1;
        if (!isPhoneNumber) {
            selectionArgs = new String[]{refinedAddress};
        } else {
            selection = "address=? OR PHONE_NUMBERS_EQUAL(address, ?, " + (this.mUseStrictPhoneNumberComparation ? 1 : 0) + ")";
            selectionArgs = new String[]{refinedAddress, refinedAddress};
        }
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
            cursor = db.query("canonical_addresses", ID_PROJECTION, selection, selectionArgs, null, null, null);
            if (cursor.getCount() == 0) {
                ContentValues contentValues = new ContentValues(1);
                contentValues.put("address", refinedAddress);
                SQLiteDatabase db2 = this.mOpenHelper.getWritableDatabase();
                long retVal2 = db2.insert("canonical_addresses", "address", contentValues);
                Log.d("MmsSmsProvider", "getSingleAddressId: insert new canonical_address for xxxxxx, _id=" + retVal2);
                return retVal2;
            }
            if (cursor.moveToFirst()) {
                retVal = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            }
            if (cursor != null) {
                cursor.close();
            }
            return retVal;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Set<Long> getAddressIds(List<String> addresses) {
        Set<Long> result = new HashSet<>(addresses.size());
        for (String address : addresses) {
            if (!address.equals("insert-address-token")) {
                long id = getSingleAddressId(address);
                if (id != -1) {
                    result.add(Long.valueOf(id));
                } else {
                    Log.e("MmsSmsProvider", "getAddressIds: address ID not found for " + address);
                }
            }
        }
        return result;
    }

    private long[] getSortedSet(Set<Long> numbers) {
        int size = numbers.size();
        long[] result = new long[size];
        int i = 0;
        for (Long number : numbers) {
            result[i] = number.longValue();
            i++;
        }
        if (size > 1) {
            Arrays.sort(result);
        }
        return result;
    }

    private String getSpaceSeparatedNumbers(long[] numbers) {
        int size = numbers.length;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                buffer.append(' ');
            }
            buffer.append(numbers[i]);
        }
        return buffer.toString();
    }

    private void insertThread(String recipientIds, int numberOfRecipients) {
        ContentValues values = new ContentValues(4);
        long date = System.currentTimeMillis();
        values.put("date", Long.valueOf(date - (date % 1000)));
        values.put("recipient_ids", recipientIds);
        if (numberOfRecipients > 1) {
            values.put("type", (Integer) 1);
        }
        values.put("message_count", (Integer) 0);
        long result = this.mOpenHelper.getWritableDatabase().insert("threads", null, values);
        Log.d("MmsSmsProvider", "insertThread: created new thread_id " + result + " for recipientIds xxxxxxx");
        getContext().getContentResolver().notifyChange(Telephony.MmsSms.CONTENT_URI, null, true, -1);
    }

    private synchronized Cursor getThreadId(List<String> recipients) {
        Cursor cursor;
        Set<Long> addressIds = getAddressIds(recipients);
        String recipientIds = "";
        if (addressIds.size() == 0) {
            Log.e("MmsSmsProvider", "getThreadId: NO receipients specified -- NOT creating thread", new Exception());
            cursor = null;
        } else {
            if (addressIds.size() == 1) {
                for (Long addressId : addressIds) {
                    recipientIds = Long.toString(addressId.longValue());
                }
            } else {
                recipientIds = getSpaceSeparatedNumbers(getSortedSet(addressIds));
            }
            if (Log.isLoggable("MmsSmsProvider", 2)) {
                Log.d("MmsSmsProvider", "getThreadId: recipientIds (selectionArgs) =xxxxxxx");
            }
            String[] selectionArgs = {recipientIds};
            SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
            db.beginTransaction();
            cursor = null;
            try {
                try {
                    cursor = db.rawQuery("SELECT _id FROM threads WHERE recipient_ids=?", selectionArgs);
                    if (cursor.getCount() == 0) {
                        cursor.close();
                        Log.d("MmsSmsProvider", "getThreadId: create new thread_id for recipients xxxxxxxx");
                        insertThread(recipientIds, recipients.size());
                        cursor = db.rawQuery("SELECT _id FROM threads WHERE recipient_ids=?", selectionArgs);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            } catch (Throwable ex) {
                Log.e("MmsSmsProvider", ex.getMessage(), ex);
                db.endTransaction();
            }
            if (cursor != null && cursor.getCount() > 1) {
                Log.w("MmsSmsProvider", "getThreadId: why is cursorCount=" + cursor.getCount());
            }
        }
        return cursor;
    }

    private static String concatSelections(String selection1, String selection2) {
        if (TextUtils.isEmpty(selection1)) {
            return selection2;
        }
        return TextUtils.isEmpty(selection2) ? selection1 : selection1 + " AND " + selection2;
    }

    private static String[] handleNullMessageProjection(String[] projection) {
        return projection == null ? UNION_COLUMNS : projection;
    }

    private static String[] handleNullThreadsProjection(String[] projection) {
        return projection == null ? THREADS_COLUMNS : projection;
    }

    private static String handleNullSortOrder(String sortOrder) {
        return sortOrder == null ? "normalized_date ASC" : sortOrder;
    }

    private Cursor getSimpleConversations(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String selection2;
        if (selection == null) {
            selection2 = "_id IN (SELECT DISTINCT thread_id FROM sms where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM pdu where thread_id NOT NULL)";
        } else {
            selection2 = selection + " AND _id IN (SELECT DISTINCT thread_id FROM sms where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM pdu where thread_id NOT NULL)";
        }
        return this.mOpenHelper.getReadableDatabase().query("threads", projection, selection2, selectionArgs, null, null, " date DESC");
    }

    private Cursor getDraftThread(String[] projection, String selection, String sortOrder) {
        String[] innerProjection = {"_id", "thread_id"};
        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();
        mmsQueryBuilder.setTables("pdu");
        smsQueryBuilder.setTables("sms");
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery("transport_type", innerProjection, MMS_COLUMNS, 1, "mms", concatSelections(selection, "msg_box=3"), null, null);
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery("transport_type", innerProjection, SMS_COLUMNS, 1, "sms", concatSelections(selection, "type=3"), null, null);
        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();
        unionQueryBuilder.setDistinct(true);
        String unionQuery = unionQueryBuilder.buildUnionQuery(new String[]{mmsSubQuery, smsSubQuery}, null, null);
        SQLiteQueryBuilder outerQueryBuilder = new SQLiteQueryBuilder();
        outerQueryBuilder.setTables("(" + unionQuery + ")");
        String outerQuery = outerQueryBuilder.buildQuery(projection, null, null, null, sortOrder, null);
        return this.mOpenHelper.getReadableDatabase().rawQuery(outerQuery, EMPTY_STRING_ARRAY);
    }

    private Cursor getConversations(String[] projection, String selection, String sortOrder) {
        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();
        mmsQueryBuilder.setTables("pdu");
        smsQueryBuilder.setTables("sms");
        String[] columns = handleNullMessageProjection(projection);
        String[] innerMmsProjection = makeProjectionWithDateAndThreadId(UNION_COLUMNS, 1000);
        String[] innerSmsProjection = makeProjectionWithDateAndThreadId(UNION_COLUMNS, 1);
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery("transport_type", innerMmsProjection, MMS_COLUMNS, 1, "mms", concatSelections(selection, "(msg_box != 3 AND (m_type = 128 OR m_type = 132 OR m_type = 130))"), "thread_id", "date = MAX(date)");
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery("transport_type", innerSmsProjection, SMS_COLUMNS, 1, "sms", concatSelections(selection, "(type != 3)"), "thread_id", "date = MAX(date)");
        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();
        unionQueryBuilder.setDistinct(true);
        String unionQuery = unionQueryBuilder.buildUnionQuery(new String[]{mmsSubQuery, smsSubQuery}, null, null);
        SQLiteQueryBuilder outerQueryBuilder = new SQLiteQueryBuilder();
        outerQueryBuilder.setTables("(" + unionQuery + ")");
        String outerQuery = outerQueryBuilder.buildQuery(columns, null, "tid", "normalized_date = MAX(normalized_date)", sortOrder, null);
        return this.mOpenHelper.getReadableDatabase().rawQuery(outerQuery, EMPTY_STRING_ARRAY);
    }

    private Cursor getFirstLockedMessage(String[] projection, String selection, String sortOrder) {
        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();
        mmsQueryBuilder.setTables("pdu");
        smsQueryBuilder.setTables("sms");
        String[] idColumn = {"_id"};
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery("transport_type", idColumn, null, 1, "mms", selection, "_id", "locked=1");
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery("transport_type", idColumn, null, 1, "sms", selection, "_id", "locked=1");
        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();
        unionQueryBuilder.setDistinct(true);
        String unionQuery = unionQueryBuilder.buildUnionQuery(new String[]{mmsSubQuery, smsSubQuery}, null, "1");
        Cursor cursor = this.mOpenHelper.getReadableDatabase().rawQuery(unionQuery, EMPTY_STRING_ARRAY);
        return cursor;
    }

    private Cursor getCompleteConversations(String[] projection, String selection, String sortOrder) {
        String unionQuery = buildConversationQuery(projection, selection, sortOrder);
        return this.mOpenHelper.getReadableDatabase().rawQuery(unionQuery, EMPTY_STRING_ARRAY);
    }

    private String[] makeProjectionWithDateAndThreadId(String[] projection, int dateMultiple) {
        int projectionSize = projection.length;
        String[] result = new String[projectionSize + 2];
        result[0] = "thread_id AS tid";
        result[1] = "date * " + dateMultiple + " AS normalized_date";
        for (int i = 0; i < projectionSize; i++) {
            result[i + 2] = projection[i];
        }
        return result;
    }

    private Cursor getConversationMessages(String threadIdString, String[] projection, String selection, String sortOrder) {
        try {
            Long.parseLong(threadIdString);
            String finalSelection = concatSelections(selection, "thread_id = " + threadIdString);
            String unionQuery = buildConversationQuery(projection, finalSelection, sortOrder);
            return this.mOpenHelper.getReadableDatabase().rawQuery(unionQuery, EMPTY_STRING_ARRAY);
        } catch (NumberFormatException e) {
            Log.e("MmsSmsProvider", "Thread ID must be a Long.");
            return null;
        }
    }

    private Cursor getMessagesByPhoneNumber(String phoneNumber, String[] projection, String selection, String sortOrder) {
        String escapedPhoneNumber = DatabaseUtils.sqlEscapeString(phoneNumber);
        String finalMmsSelection = concatSelections(selection, "pdu._id = matching_addresses.address_msg_id");
        String finalSmsSelection = concatSelections(selection, "(address=" + escapedPhoneNumber + " OR PHONE_NUMBERS_EQUAL(address, " + escapedPhoneNumber + (this.mUseStrictPhoneNumberComparation ? ", 1))" : ", 0))"));
        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();
        mmsQueryBuilder.setDistinct(true);
        smsQueryBuilder.setDistinct(true);
        mmsQueryBuilder.setTables("pdu, (SELECT msg_id AS address_msg_id FROM addr WHERE (address=" + escapedPhoneNumber + " OR PHONE_NUMBERS_EQUAL(addr.address, " + escapedPhoneNumber + (this.mUseStrictPhoneNumberComparation ? ", 1))) " : ", 0))) ") + "AS matching_addresses");
        smsQueryBuilder.setTables("sms");
        String[] columns = handleNullMessageProjection(projection);
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery("transport_type", columns, MMS_COLUMNS, 0, "mms", finalMmsSelection, null, null);
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery("transport_type", columns, SMS_COLUMNS, 0, "sms", finalSmsSelection, null, null);
        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();
        unionQueryBuilder.setDistinct(true);
        String unionQuery = unionQueryBuilder.buildUnionQuery(new String[]{mmsSubQuery, smsSubQuery}, sortOrder, null);
        return this.mOpenHelper.getReadableDatabase().rawQuery(unionQuery, EMPTY_STRING_ARRAY);
    }

    private Cursor getConversationById(String threadIdString, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            Long.parseLong(threadIdString);
            String extraSelection = "_id=" + threadIdString;
            String finalSelection = concatSelections(selection, extraSelection);
            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
            String[] columns = handleNullThreadsProjection(projection);
            queryBuilder.setDistinct(true);
            queryBuilder.setTables("threads");
            return queryBuilder.query(this.mOpenHelper.getReadableDatabase(), columns, finalSelection, selectionArgs, sortOrder, null, null);
        } catch (NumberFormatException e) {
            Log.e("MmsSmsProvider", "Thread ID must be a Long.");
            return null;
        }
    }

    private static String joinPduAndPendingMsgTables() {
        return "pdu LEFT JOIN pending_msgs ON pdu._id = pending_msgs.msg_id";
    }

    private static String[] createMmsProjection(String[] old) {
        String[] newProjection = new String[old.length];
        for (int i = 0; i < old.length; i++) {
            if (old[i].equals("_id")) {
                newProjection[i] = "pdu._id";
            } else {
                newProjection[i] = old[i];
            }
        }
        return newProjection;
    }

    private Cursor getUndeliveredMessages(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String[] mmsProjection = createMmsProjection(projection);
        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();
        mmsQueryBuilder.setTables(joinPduAndPendingMsgTables());
        smsQueryBuilder.setTables("sms");
        String finalMmsSelection = concatSelections(selection, "msg_box = 4");
        String finalSmsSelection = concatSelections(selection, "(type = 4 OR type = 5 OR type = 6)");
        String[] smsColumns = handleNullMessageProjection(projection);
        String[] mmsColumns = handleNullMessageProjection(mmsProjection);
        String[] innerMmsProjection = makeProjectionWithDateAndThreadId(mmsColumns, 1000);
        String[] innerSmsProjection = makeProjectionWithDateAndThreadId(smsColumns, 1);
        Set<String> columnsPresentInTable = new HashSet<>(MMS_COLUMNS);
        columnsPresentInTable.add("pdu._id");
        columnsPresentInTable.add("err_type");
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery("transport_type", innerMmsProjection, columnsPresentInTable, 1, "mms", finalMmsSelection, null, null);
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery("transport_type", innerSmsProjection, SMS_COLUMNS, 1, "sms", finalSmsSelection, null, null);
        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();
        unionQueryBuilder.setDistinct(true);
        String unionQuery = unionQueryBuilder.buildUnionQuery(new String[]{smsSubQuery, mmsSubQuery}, null, null);
        SQLiteQueryBuilder outerQueryBuilder = new SQLiteQueryBuilder();
        outerQueryBuilder.setTables("(" + unionQuery + ")");
        String outerQuery = outerQueryBuilder.buildQuery(smsColumns, null, null, null, sortOrder, null);
        return this.mOpenHelper.getReadableDatabase().rawQuery(outerQuery, EMPTY_STRING_ARRAY);
    }

    private static String[] makeProjectionWithNormalizedDate(String[] projection, int dateMultiple) {
        int projectionSize = projection.length;
        String[] result = new String[projectionSize + 1];
        result[0] = "date * " + dateMultiple + " AS normalized_date";
        System.arraycopy(projection, 0, result, 1, projectionSize);
        return result;
    }

    private static String buildConversationQuery(String[] projection, String selection, String sortOrder) {
        String[] mmsProjection = createMmsProjection(projection);
        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();
        mmsQueryBuilder.setDistinct(true);
        smsQueryBuilder.setDistinct(true);
        mmsQueryBuilder.setTables(joinPduAndPendingMsgTables());
        smsQueryBuilder.setTables("sms");
        String[] smsColumns = handleNullMessageProjection(projection);
        String[] mmsColumns = handleNullMessageProjection(mmsProjection);
        String[] innerMmsProjection = makeProjectionWithNormalizedDate(mmsColumns, 1000);
        String[] innerSmsProjection = makeProjectionWithNormalizedDate(smsColumns, 1);
        Set<String> columnsPresentInTable = new HashSet<>(MMS_COLUMNS);
        columnsPresentInTable.add("pdu._id");
        columnsPresentInTable.add("err_type");
        String mmsSelection = concatSelections(selection, "msg_box != 3");
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery("transport_type", innerMmsProjection, columnsPresentInTable, 0, "mms", concatSelections(mmsSelection, "(msg_box != 3 AND (m_type = 128 OR m_type = 132 OR m_type = 130))"), null, null);
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery("transport_type", innerSmsProjection, SMS_COLUMNS, 0, "sms", concatSelections(selection, "(type != 3)"), null, null);
        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();
        unionQueryBuilder.setDistinct(true);
        String unionQuery = unionQueryBuilder.buildUnionQuery(new String[]{smsSubQuery, mmsSubQuery}, handleNullSortOrder(sortOrder), null);
        SQLiteQueryBuilder outerQueryBuilder = new SQLiteQueryBuilder();
        outerQueryBuilder.setTables("(" + unionQuery + ")");
        return outerQueryBuilder.buildQuery(smsColumns, null, null, null, sortOrder, null);
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android-dir/mms-sms";
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        Context context = getContext();
        int affectedRows = 0;
        switch (URI_MATCHER.match(uri)) {
            case 0:
                affectedRows = MmsProvider.deleteMessages(context, db, selection, selectionArgs, uri) + db.delete("sms", selection, selectionArgs);
                MmsSmsDatabaseHelper.updateAllThreads(db, null, null);
                break;
            case 1:
                try {
                    long threadId = Long.parseLong(uri.getLastPathSegment());
                    affectedRows = deleteConversation(uri, selection, selectionArgs);
                    MmsSmsDatabaseHelper.updateThread(db, threadId);
                } catch (NumberFormatException e) {
                    Log.e("MmsSmsProvider", "Thread ID must be a long.");
                }
                break;
            case 11:
                affectedRows = db.delete("threads", "_id NOT IN (SELECT DISTINCT thread_id FROM sms where thread_id NOT NULL UNION SELECT DISTINCT thread_id FROM pdu where thread_id NOT NULL)", null);
                break;
            default:
                throw new UnsupportedOperationException("MmsSmsProvider does not support deletes, inserts, or updates for this URI." + uri);
        }
        if (affectedRows > 0) {
            context.getContentResolver().notifyChange(Telephony.MmsSms.CONTENT_URI, null, true, -1);
        }
        return affectedRows;
    }

    private int deleteConversation(Uri uri, String selection, String[] selectionArgs) {
        String threadId = uri.getLastPathSegment();
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        String finalSelection = concatSelections(selection, "thread_id = " + threadId);
        return MmsProvider.deleteMessages(getContext(), db, finalSelection, selectionArgs, uri) + db.delete("sms", finalSelection, selectionArgs);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (URI_MATCHER.match(uri) == 6) {
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            long rowId = db.insert("pending_msgs", null, values);
            return Uri.parse(uri + "/" + rowId);
        }
        throw new UnsupportedOperationException("MmsSmsProvider does not support deletes, inserts, or updates for this URI." + uri);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int affectedRows;
        int callerUid = Binder.getCallingUid();
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        switch (URI_MATCHER.match(uri)) {
            case 0:
                ContentValues finalValues = new ContentValues(1);
                if (values.containsKey("archived")) {
                    finalValues.put("archived", values.getAsBoolean("archived"));
                }
                affectedRows = db.update("threads", finalValues, selection, selectionArgs);
                break;
            case 1:
                String threadIdString = uri.getPathSegments().get(1);
                affectedRows = updateConversation(threadIdString, values, selection, selectionArgs, callerUid);
                break;
            case 2:
            case 3:
            case 4:
            default:
                throw new UnsupportedOperationException("MmsSmsProvider does not support deletes, inserts, or updates for this URI." + uri);
            case 5:
                String extraSelection = "_id=" + uri.getPathSegments().get(1);
                String finalSelection = TextUtils.isEmpty(selection) ? extraSelection : extraSelection + " AND " + selection;
                affectedRows = db.update("canonical_addresses", values, finalSelection, null);
                break;
            case 6:
                affectedRows = db.update("pending_msgs", values, selection, null);
                break;
        }
        if (affectedRows > 0) {
            getContext().getContentResolver().notifyChange(Telephony.MmsSms.CONTENT_URI, null, true, -1);
        }
        return affectedRows;
    }

    private int updateConversation(String threadIdString, ContentValues values, String selection, String[] selectionArgs, int callerUid) {
        try {
            Long.parseLong(threadIdString);
            if (ProviderUtil.shouldRemoveCreator(values, callerUid)) {
                Log.w("MmsSmsProvider", ProviderUtil.getPackageNamesByUid(getContext(), callerUid) + " tries to update CREATOR");
                values.remove("creator");
                values.remove("creator");
            }
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            String finalSelection = concatSelections(selection, "thread_id=" + threadIdString);
            return db.update("pdu", values, finalSelection, selectionArgs) + db.update("sms", values, finalSelection, selectionArgs);
        } catch (NumberFormatException e) {
            Log.e("MmsSmsProvider", "Thread ID must be a Long.");
            return 0;
        }
    }

    private static void initializeColumnSets() {
        int commonColumnCount = MMS_SMS_COLUMNS.length;
        int mmsOnlyColumnCount = MMS_ONLY_COLUMNS.length;
        int smsOnlyColumnCount = SMS_ONLY_COLUMNS.length;
        Set<String> unionColumns = new HashSet<>();
        for (int i = 0; i < commonColumnCount; i++) {
            MMS_COLUMNS.add(MMS_SMS_COLUMNS[i]);
            SMS_COLUMNS.add(MMS_SMS_COLUMNS[i]);
            unionColumns.add(MMS_SMS_COLUMNS[i]);
        }
        for (int i2 = 0; i2 < mmsOnlyColumnCount; i2++) {
            MMS_COLUMNS.add(MMS_ONLY_COLUMNS[i2]);
            unionColumns.add(MMS_ONLY_COLUMNS[i2]);
        }
        for (int i3 = 0; i3 < smsOnlyColumnCount; i3++) {
            SMS_COLUMNS.add(SMS_ONLY_COLUMNS[i3]);
            unionColumns.add(SMS_ONLY_COLUMNS[i3]);
        }
        int i4 = 0;
        for (String columnName : unionColumns) {
            UNION_COLUMNS[i4] = columnName;
            i4++;
        }
    }
}
