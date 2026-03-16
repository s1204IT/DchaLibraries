package com.android.contacts.editor;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.contacts.common.model.ValuesDelta;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AggregationSuggestionEngine extends HandlerThread {
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
        public static final String[] COLUMNS = {"_id", "contact_id", "lookup", "photo_id", "display_name", "raw_contact_id", "mimetype", "data1", "is_super_primary", "data15", "account_type", "account_name", "data_set"};
    }

    public interface Listener {
        void onAggregationSuggestionChange();
    }

    public static final class RawContact {
        public String accountName;
        public String accountType;
        public String dataSet;
        public long rawContactId;

        public String toString() {
            return "ID: " + this.rawContactId + " account: " + this.accountType + "/" + this.accountName + " dataSet: " + this.dataSet;
        }
    }

    public static final class Suggestion {
        public long contactId;
        public String emailAddress;
        public String lookupKey;
        public String name;
        public String nickname;
        public String phoneNumber;
        public byte[] photo;
        public List<RawContact> rawContacts;

        public String toString() {
            return "ID: " + this.contactId + " rawContacts: " + this.rawContacts + " name: " + this.name + " phone: " + this.phoneNumber + " email: " + this.emailAddress + " nickname: " + this.nickname + (this.photo != null ? " [has photo]" : "");
        }
    }

    private final class SuggestionContentObserver extends ContentObserver {
        private SuggestionContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            AggregationSuggestionEngine.this.scheduleSuggestionLookup();
        }
    }

    public AggregationSuggestionEngine(Context context) {
        super("AggregationSuggestions", 10);
        this.mSuggestedContactIds = new long[0];
        this.mContext = context.getApplicationContext();
        this.mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                AggregationSuggestionEngine.this.deliverNotification((Cursor) msg.obj);
            }
        };
    }

    protected Handler getHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    AggregationSuggestionEngine.this.handleMessage(msg);
                }
            };
        }
        return this.mHandler;
    }

    public void setContactId(long contactId) {
        if (contactId != this.mContactId) {
            this.mContactId = contactId;
            reset();
        }
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

    public void onNameChange(ValuesDelta values) {
        this.mSuggestionsUri = buildAggregationSuggestionUri(values);
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
        if (this.mSuggestionsUri != null) {
            Message msg = handler.obtainMessage(1, this.mSuggestionsUri);
            handler.sendMessageDelayed(msg, 300L);
        }
    }

    private Uri buildAggregationSuggestionUri(ValuesDelta values) {
        StringBuilder nameSb = new StringBuilder();
        appendValue(nameSb, values, "data4");
        appendValue(nameSb, values, "data2");
        appendValue(nameSb, values, "data5");
        appendValue(nameSb, values, "data3");
        appendValue(nameSb, values, "data6");
        if (nameSb.length() == 0) {
            appendValue(nameSb, values, "data1");
        }
        StringBuilder phoneticNameSb = new StringBuilder();
        appendValue(phoneticNameSb, values, "data9");
        appendValue(phoneticNameSb, values, "data8");
        appendValue(phoneticNameSb, values, "data7");
        if (nameSb.length() == 0 && phoneticNameSb.length() == 0) {
            return null;
        }
        ContactsContract.Contacts.AggregationSuggestions.Builder builder = ContactsContract.Contacts.AggregationSuggestions.builder().setLimit(3).setContactId(this.mContactId);
        if (nameSb.length() != 0) {
            builder.addParameter("name", nameSb.toString());
        }
        if (phoneticNameSb.length() != 0) {
            builder.addParameter("name", phoneticNameSb.toString());
        }
        return builder.build();
    }

    private void appendValue(StringBuilder sb, ValuesDelta values, String column) {
        String value = values.getAsString(column);
        if (!TextUtils.isEmpty(value)) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(value);
        }
    }

    protected void handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                this.mSuggestedContactIds = new long[0];
                break;
            case 1:
                loadAggregationSuggestions((Uri) msg.obj);
                break;
        }
    }

    private void loadAggregationSuggestions(Uri uri) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        Cursor cursor = contentResolver.query(uri, new String[]{"_id"}, null, null, null);
        if (cursor != null) {
            try {
                if (!getHandler().hasMessages(1)) {
                    boolean changed = updateSuggestedContactIds(cursor);
                    if (changed) {
                        StringBuilder sb = new StringBuilder("mimetype IN ('vnd.android.cursor.item/phone_v2','vnd.android.cursor.item/email_v2','vnd.android.cursor.item/name','vnd.android.cursor.item/nickname','vnd.android.cursor.item/photo') AND contact_id IN (");
                        int count = this.mSuggestedContactIds.length;
                        for (int i = 0; i < count; i++) {
                            if (i > 0) {
                                sb.append(',');
                            }
                            sb.append(this.mSuggestedContactIds[i]);
                        }
                        sb.append(')');
                        sb.toString();
                        Cursor dataCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, DataQuery.COLUMNS, sb.toString(), null, "contact_id");
                        if (dataCursor != null) {
                            this.mMainHandler.sendMessage(this.mMainHandler.obtainMessage(2, dataCursor));
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    private boolean updateSuggestedContactIds(Cursor cursor) {
        int count = cursor.getCount();
        boolean changed = count != this.mSuggestedContactIds.length;
        ArrayList<Long> newIds = new ArrayList<>(count);
        while (cursor.moveToNext()) {
            long contactId = cursor.getLong(0);
            if (!changed && Arrays.binarySearch(this.mSuggestedContactIds, contactId) < 0) {
                changed = true;
            }
            newIds.add(Long.valueOf(contactId));
        }
        if (changed) {
            this.mSuggestedContactIds = new long[newIds.size()];
            int i = 0;
            for (Long newId : newIds) {
                this.mSuggestedContactIds[i] = newId.longValue();
                i++;
            }
            Arrays.sort(this.mSuggestedContactIds);
        }
        return changed;
    }

    protected void deliverNotification(Cursor dataCursor) {
        if (this.mDataCursor != null) {
            this.mDataCursor.close();
        }
        this.mDataCursor = dataCursor;
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
        ArrayList<Suggestion> list = Lists.newArrayList();
        if (this.mDataCursor != null) {
            Suggestion suggestion = null;
            long currentContactId = -1;
            this.mDataCursor.moveToPosition(-1);
            while (this.mDataCursor.moveToNext()) {
                long contactId = this.mDataCursor.getLong(1);
                if (contactId != currentContactId) {
                    suggestion = new Suggestion();
                    suggestion.contactId = contactId;
                    suggestion.name = this.mDataCursor.getString(4);
                    suggestion.lookupKey = this.mDataCursor.getString(2);
                    suggestion.rawContacts = Lists.newArrayList();
                    list.add(suggestion);
                    currentContactId = contactId;
                }
                long rawContactId = this.mDataCursor.getLong(5);
                if (!containsRawContact(suggestion, rawContactId)) {
                    RawContact rawContact = new RawContact();
                    rawContact.rawContactId = rawContactId;
                    rawContact.accountName = this.mDataCursor.getString(11);
                    rawContact.accountType = this.mDataCursor.getString(10);
                    rawContact.dataSet = this.mDataCursor.getString(12);
                    suggestion.rawContacts.add(rawContact);
                }
                String mimetype = this.mDataCursor.getString(6);
                if ("vnd.android.cursor.item/phone_v2".equals(mimetype)) {
                    String data = this.mDataCursor.getString(7);
                    int superprimary = this.mDataCursor.getInt(8);
                    if (!TextUtils.isEmpty(data) && (superprimary != 0 || suggestion.phoneNumber == null)) {
                        suggestion.phoneNumber = data;
                    }
                } else if ("vnd.android.cursor.item/email_v2".equals(mimetype)) {
                    String data2 = this.mDataCursor.getString(7);
                    int superprimary2 = this.mDataCursor.getInt(8);
                    if (!TextUtils.isEmpty(data2) && (superprimary2 != 0 || suggestion.emailAddress == null)) {
                        suggestion.emailAddress = data2;
                    }
                } else if ("vnd.android.cursor.item/nickname".equals(mimetype)) {
                    String data3 = this.mDataCursor.getString(7);
                    if (!TextUtils.isEmpty(data3)) {
                        suggestion.nickname = data3;
                    }
                } else if ("vnd.android.cursor.item/photo".equals(mimetype)) {
                    long dataId = this.mDataCursor.getLong(0);
                    long photoId = this.mDataCursor.getLong(3);
                    if (dataId == photoId && !this.mDataCursor.isNull(9)) {
                        suggestion.photo = this.mDataCursor.getBlob(9);
                    }
                }
            }
        }
        return list;
    }

    public boolean containsRawContact(Suggestion suggestion, long rawContactId) {
        if (suggestion.rawContacts != null) {
            int count = suggestion.rawContacts.size();
            for (int i = 0; i < count; i++) {
                if (suggestion.rawContacts.get(i).rawContactId == rawContactId) {
                    return true;
                }
            }
        }
        return false;
    }
}
