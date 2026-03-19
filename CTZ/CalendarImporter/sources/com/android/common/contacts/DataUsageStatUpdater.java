package com.android.common.contacts;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import com.android.common.speech.LoggingEvents;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class DataUsageStatUpdater {
    private static final String TAG = DataUsageStatUpdater.class.getSimpleName();
    private final ContentResolver mResolver;

    public static final class DataUsageFeedback {
        static final Uri FEEDBACK_URI = Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, "usagefeedback");
        static final String USAGE_TYPE = "type";
        public static final String USAGE_TYPE_CALL = "call";
        public static final String USAGE_TYPE_LONG_TEXT = "long_text";
        public static final String USAGE_TYPE_SHORT_TEXT = "short_text";
    }

    public DataUsageStatUpdater(Context context) {
        this.mResolver = context.getContentResolver();
    }

    public boolean updateWithRfc822Address(Collection<CharSequence> collection) {
        if (collection == null) {
            return false;
        }
        HashSet hashSet = new HashSet();
        Iterator<CharSequence> it = collection.iterator();
        while (it.hasNext()) {
            for (Rfc822Token rfc822Token : Rfc822Tokenizer.tokenize(it.next().toString().trim())) {
                hashSet.add(rfc822Token.getAddress());
            }
        }
        return updateWithAddress(hashSet);
    }

    public boolean updateWithAddress(Collection<String> collection) {
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "updateWithAddress: " + Arrays.toString(collection.toArray()));
        }
        if (collection != null && !collection.isEmpty()) {
            ArrayList arrayList = new ArrayList();
            StringBuilder sb = new StringBuilder();
            String[] strArr = new String[collection.size()];
            arrayList.addAll(collection);
            Arrays.fill(strArr, "?");
            sb.append("data1 IN (");
            sb.append(TextUtils.join(",", strArr));
            sb.append(")");
            Cursor cursorQuery = this.mResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, new String[]{"contact_id", "_id"}, sb.toString(), (String[]) arrayList.toArray(new String[0]), null);
            if (cursorQuery == null) {
                Log.w(TAG, "Cursor for Email.CONTENT_URI became null.");
            } else {
                HashSet hashSet = new HashSet(cursorQuery.getCount());
                HashSet hashSet2 = new HashSet(cursorQuery.getCount());
                try {
                    cursorQuery.move(-1);
                    while (cursorQuery.moveToNext()) {
                        hashSet.add(Long.valueOf(cursorQuery.getLong(0)));
                        hashSet2.add(Long.valueOf(cursorQuery.getLong(1)));
                    }
                    cursorQuery.close();
                    return update(hashSet, hashSet2, DataUsageFeedback.USAGE_TYPE_LONG_TEXT);
                } catch (Throwable th) {
                    cursorQuery.close();
                    throw th;
                }
            }
        }
        return false;
    }

    public boolean updateWithPhoneNumber(Collection<String> collection) {
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "updateWithPhoneNumber: " + Arrays.toString(collection.toArray()));
        }
        if (collection != null && !collection.isEmpty()) {
            ArrayList arrayList = new ArrayList();
            StringBuilder sb = new StringBuilder();
            String[] strArr = new String[collection.size()];
            arrayList.addAll(collection);
            Arrays.fill(strArr, "?");
            sb.append("data1 IN (");
            sb.append(TextUtils.join(",", strArr));
            sb.append(")");
            Cursor cursorQuery = this.mResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"contact_id", "_id"}, sb.toString(), (String[]) arrayList.toArray(new String[0]), null);
            if (cursorQuery == null) {
                Log.w(TAG, "Cursor for Phone.CONTENT_URI became null.");
            } else {
                HashSet hashSet = new HashSet(cursorQuery.getCount());
                HashSet hashSet2 = new HashSet(cursorQuery.getCount());
                try {
                    cursorQuery.move(-1);
                    while (cursorQuery.moveToNext()) {
                        hashSet.add(Long.valueOf(cursorQuery.getLong(0)));
                        hashSet2.add(Long.valueOf(cursorQuery.getLong(1)));
                    }
                    cursorQuery.close();
                    return update(hashSet, hashSet2, DataUsageFeedback.USAGE_TYPE_SHORT_TEXT);
                } catch (Throwable th) {
                    cursorQuery.close();
                    throw th;
                }
            }
        }
        return false;
    }

    private boolean update(Collection<Long> collection, Collection<Long> collection2, String str) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (Build.VERSION.SDK_INT >= 14) {
            if (collection2.isEmpty()) {
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "Given list for data IDs is null. Ignoring.");
                }
            } else {
                if (this.mResolver.update(DataUsageFeedback.FEEDBACK_URI.buildUpon().appendPath(TextUtils.join(",", collection2)).appendQueryParameter(LoggingEvents.VoiceIme.EXTRA_TEXT_MODIFIED_TYPE, str).build(), new ContentValues(), null, null) > 0) {
                    return true;
                }
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "update toward data rows " + collection2 + " failed");
                }
                return false;
            }
        } else if (collection.isEmpty()) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "Given list for contact IDs is null. Ignoring.");
            }
        } else {
            StringBuilder sb = new StringBuilder();
            ArrayList arrayList = new ArrayList();
            String[] strArr = new String[collection.size()];
            Iterator<Long> it = collection.iterator();
            while (it.hasNext()) {
                arrayList.add(String.valueOf(it.next().longValue()));
            }
            Arrays.fill(strArr, "?");
            sb.append("_id IN (");
            sb.append(TextUtils.join(",", strArr));
            sb.append(")");
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "contactId where: " + sb.toString());
                Log.d(TAG, "contactId selection: " + arrayList);
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put("last_time_contacted", Long.valueOf(jCurrentTimeMillis));
            if (this.mResolver.update(ContactsContract.Contacts.CONTENT_URI, contentValues, sb.toString(), (String[]) arrayList.toArray(new String[0])) > 0) {
                return true;
            }
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "update toward raw contacts " + collection + " failed");
            }
        }
        return false;
    }
}
