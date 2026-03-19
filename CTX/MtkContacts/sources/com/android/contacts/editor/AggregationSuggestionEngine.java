package com.android.contacts.editor;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.contacts.compat.AggregationSuggestionsCompat;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountWithDataSet;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.mediatek.contacts.util.ContactsPortableUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class AggregationSuggestionEngine extends HandlerThread {
    private AccountWithDataSet mAccountFilter;
    private long mContactId;
    private ContentObserver mContentObserver;
    private final Context mContext;
    private Cursor mDataCursor;
    private Handler mHandler;
    private Listener mListener;
    private Handler mMainHandler;
    private long[] mSuggestedContactIds;
    private Uri mSuggestionsUri;

    private static final class DataQuery {
        public static final String[] COLUMNS = {"contact_id", "lookup", "raw_contact_id", "mimetype", "data1", "is_super_primary", "account_type", "account_name", "data_set", "_id"};
    }

    public interface Listener {
        void onAggregationSuggestionChange();
    }

    public static final class Suggestion {
        public long contactId;
        public String contactLookupKey;
        public String emailAddress;
        public String name;
        public String nickname;
        public String phoneNumber;
        public long photoId = -1;
        public long rawContactId;

        public String toString() {
            return MoreObjects.toStringHelper((Class<?>) Suggestion.class).add("contactId", this.contactId).add("contactLookupKey", this.contactLookupKey).add("rawContactId", this.rawContactId).add("photoId", this.photoId).add("name", this.name).add("phoneNumber", this.phoneNumber).add("emailAddress", this.emailAddress).add("nickname", this.nickname).toString();
        }
    }

    private final class SuggestionContentObserver extends ContentObserver {
        private SuggestionContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z) {
            AggregationSuggestionEngine.this.scheduleSuggestionLookup();
        }
    }

    public AggregationSuggestionEngine(Context context) {
        super("AggregationSuggestions", 10);
        this.mSuggestedContactIds = new long[0];
        this.mContext = context.getApplicationContext();
        this.mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                AggregationSuggestionEngine.this.deliverNotification((Cursor) message.obj);
            }
        };
    }

    protected Handler getHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message message) {
                    AggregationSuggestionEngine.this.handleMessage(message);
                }
            };
        }
        return this.mHandler;
    }

    public void setContactId(long j) {
        if (j != this.mContactId) {
            this.mContactId = j;
            reset();
        }
    }

    public void setAccountFilter(AccountWithDataSet accountWithDataSet) {
        this.mAccountFilter = accountWithDataSet;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    public boolean quit() {
        if (this.mDataCursor != null) {
            this.mDataCursor.close();
        }
        this.mDataCursor = null;
        if (this.mContentObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
            this.mContentObserver = null;
        }
        return super.quit();
    }

    public void reset() {
        Handler handler = getHandler();
        handler.removeMessages(1);
        handler.sendEmptyMessage(0);
    }

    public void onNameChange(ValuesDelta valuesDelta) {
        this.mSuggestionsUri = buildAggregationSuggestionUri(valuesDelta);
        if (this.mSuggestionsUri != null) {
            if (this.mContentObserver == null) {
                this.mContentObserver = new SuggestionContentObserver(getHandler());
                this.mContext.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, this.mContentObserver);
            }
        } else if (this.mContentObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
            this.mContentObserver = null;
        }
        scheduleSuggestionLookup();
    }

    protected void scheduleSuggestionLookup() {
        Handler handler = getHandler();
        handler.removeMessages(1);
        if (this.mSuggestionsUri == null) {
            return;
        }
        handler.sendMessageDelayed(handler.obtainMessage(1, this.mSuggestionsUri), 300L);
    }

    private Uri buildAggregationSuggestionUri(ValuesDelta valuesDelta) {
        StringBuilder sb = new StringBuilder();
        appendValue(sb, valuesDelta, "data4");
        appendValue(sb, valuesDelta, "data2");
        appendValue(sb, valuesDelta, "data5");
        appendValue(sb, valuesDelta, "data3");
        appendValue(sb, valuesDelta, "data6");
        StringBuilder sb2 = new StringBuilder();
        appendValue(sb2, valuesDelta, "data9");
        appendValue(sb2, valuesDelta, "data8");
        appendValue(sb2, valuesDelta, "data7");
        if (sb.length() == 0 && sb2.length() == 0) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            ContactsContract.Contacts.AggregationSuggestions.Builder contactId = new ContactsContract.Contacts.AggregationSuggestions.Builder().setLimit(3).setContactId(this.mContactId);
            if (sb.length() != 0) {
                contactId.addNameParameter(sb.toString());
            }
            if (sb2.length() != 0) {
                contactId.addNameParameter(sb2.toString());
            }
            return contactId.build();
        }
        AggregationSuggestionsCompat.Builder contactId2 = new AggregationSuggestionsCompat.Builder().setLimit(3).setContactId(this.mContactId);
        if (sb.length() != 0) {
            contactId2.addNameParameter(sb.toString());
        }
        if (sb2.length() != 0) {
            contactId2.addNameParameter(sb2.toString());
        }
        return contactId2.build();
    }

    private void appendValue(StringBuilder sb, ValuesDelta valuesDelta, String str) {
        String asString = valuesDelta.getAsString(str);
        if (!TextUtils.isEmpty(asString)) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(asString);
        }
    }

    protected void handleMessage(Message message) {
        switch (message.what) {
            case 0:
                this.mSuggestedContactIds = new long[0];
                break;
            case 1:
                loadAggregationSuggestions((Uri) message.obj);
                break;
        }
    }

    private void loadAggregationSuggestions(Uri uri) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        Cursor cursorQuery = contentResolver.query(uri, new String[]{"_id"}, null, null, null);
        if (cursorQuery == null) {
            return;
        }
        try {
            if (getHandler().hasMessages(1)) {
                return;
            }
            if (updateSuggestedContactIds(cursorQuery)) {
                StringBuilder sb = new StringBuilder("mimetype IN ('vnd.android.cursor.item/phone_v2','vnd.android.cursor.item/email_v2','vnd.android.cursor.item/name','vnd.android.cursor.item/nickname','vnd.android.cursor.item/photo') AND contact_id IN (");
                int length = this.mSuggestedContactIds.length;
                for (int i = 0; i < length; i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(this.mSuggestedContactIds[i]);
                }
                sb.append(')');
                if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                    sb.append(" AND indicate_phone_or_sim_contact=-1");
                }
                sb.toString();
                Cursor cursorQuery2 = contentResolver.query(ContactsContract.Data.CONTENT_URI, DataQuery.COLUMNS, sb.toString(), null, "contact_id");
                if (cursorQuery2 != null) {
                    this.mMainHandler.sendMessage(this.mMainHandler.obtainMessage(2, cursorQuery2));
                }
            }
        } finally {
            cursorQuery.close();
        }
    }

    private boolean updateSuggestedContactIds(Cursor cursor) {
        int count = cursor.getCount();
        int i = 0;
        boolean z = count != this.mSuggestedContactIds.length;
        ArrayList arrayList = new ArrayList(count);
        while (cursor.moveToNext()) {
            long j = cursor.getLong(0);
            if (!z && Arrays.binarySearch(this.mSuggestedContactIds, j) < 0) {
                z = true;
            }
            arrayList.add(Long.valueOf(j));
        }
        if (z) {
            this.mSuggestedContactIds = new long[arrayList.size()];
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                this.mSuggestedContactIds[i] = ((Long) it.next()).longValue();
                i++;
            }
            Arrays.sort(this.mSuggestedContactIds);
        }
        return z;
    }

    protected void deliverNotification(Cursor cursor) {
        if (this.mDataCursor != null) {
            this.mDataCursor.close();
        }
        this.mDataCursor = cursor;
        if (this.mListener != null) {
            this.mListener.onAggregationSuggestionChange();
        }
    }

    public int getSuggestedContactCount() {
        if (this.mDataCursor != null) {
            return this.mDataCursor.getCount();
        }
        return 0;
    }

    public List<Suggestion> getSuggestions() {
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        if (this.mDataCursor != null && this.mAccountFilter != null) {
            Suggestion suggestion = null;
            this.mDataCursor.moveToPosition(-1);
            long j = -1;
            while (this.mDataCursor.moveToNext()) {
                long j2 = this.mDataCursor.getLong(2);
                if (j2 != j) {
                    suggestion = new Suggestion();
                    suggestion.rawContactId = j2;
                    suggestion.contactId = this.mDataCursor.getLong(0);
                    suggestion.contactLookupKey = this.mDataCursor.getString(1);
                    if (this.mAccountFilter.equals(new AccountWithDataSet(this.mDataCursor.getString(7), this.mDataCursor.getString(6), this.mDataCursor.getString(8)))) {
                        arrayListNewArrayList.add(suggestion);
                    }
                    j = j2;
                }
                String string = this.mDataCursor.getString(3);
                if ("vnd.android.cursor.item/phone_v2".equals(string)) {
                    String string2 = this.mDataCursor.getString(4);
                    int i = this.mDataCursor.getInt(5);
                    if (!TextUtils.isEmpty(string2) && (i != 0 || suggestion.phoneNumber == null)) {
                        suggestion.phoneNumber = string2;
                    }
                } else if ("vnd.android.cursor.item/email_v2".equals(string)) {
                    String string3 = this.mDataCursor.getString(4);
                    int i2 = this.mDataCursor.getInt(5);
                    if (!TextUtils.isEmpty(string3) && (i2 != 0 || suggestion.emailAddress == null)) {
                        suggestion.emailAddress = string3;
                    }
                } else if ("vnd.android.cursor.item/nickname".equals(string)) {
                    String string4 = this.mDataCursor.getString(4);
                    if (!TextUtils.isEmpty(string4)) {
                        suggestion.nickname = string4;
                    }
                } else if ("vnd.android.cursor.item/name".equals(string)) {
                    String string5 = this.mDataCursor.getString(4);
                    if (!TextUtils.isEmpty(string5) && suggestion.name == null) {
                        suggestion.name = string5;
                    }
                } else if ("vnd.android.cursor.item/photo".equals(string)) {
                    Long lValueOf = Long.valueOf(this.mDataCursor.getLong(9));
                    if (suggestion.photoId == -1) {
                        suggestion.photoId = lValueOf.longValue();
                    }
                }
            }
        }
        return arrayListNewArrayList;
    }
}
