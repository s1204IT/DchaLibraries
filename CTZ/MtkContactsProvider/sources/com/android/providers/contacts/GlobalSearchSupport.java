package com.android.providers.contacts;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.ContactsContract;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.providers.contacts.GlobalSearchSupportEx;
import java.util.ArrayList;

public class GlobalSearchSupport {
    private static final String[] SEARCH_SUGGESTIONS_COLUMNS = {"_id", "suggest_text_1", "suggest_text_2", "suggest_icon_1", "suggest_icon_2", "suggest_intent_data", "suggest_intent_action", "suggest_shortcut_id", "suggest_intent_extra_data", "suggest_last_access_hint"};
    private final ContactsProvider2 mContactsProvider;

    private static class SearchSuggestion {
        long contactId;
        String filter;
        String icon1;
        String icon2;
        String intentAction;
        String intentData;
        String lastAccessTime;
        String lookupKey;
        int mIsSdnContact;
        int mSimIndicator;
        int mSlotId;
        String photoUri;
        int presence;
        String text1;
        String text2;

        private SearchSuggestion() {
            this.presence = -1;
            this.mSimIndicator = -1;
            this.mSlotId = -1;
            this.mIsSdnContact = 0;
        }

        public ArrayList<?> asList(String[] strArr) {
            if (this.icon1 == null) {
                if (this.photoUri != null) {
                    this.icon1 = this.photoUri.toString();
                } else {
                    this.icon1 = String.valueOf(android.R.drawable.dropdown_normal_holo_dark);
                }
            }
            if (this.presence != -1) {
                this.icon2 = String.valueOf(ContactsContract.StatusUpdates.getPresenceIconResourceId(this.presence));
            } else {
                this.icon2 = null;
            }
            Log.i("GlobalSearchSupport", " icon2 : " + this.icon2);
            ArrayList<?> arrayList = new ArrayList<>();
            if (strArr == null) {
                arrayList.add(Long.valueOf(this.contactId));
                arrayList.add(this.text1);
                arrayList.add(this.text2);
                arrayList.add(this.icon1);
                arrayList.add(this.icon2);
                arrayList.add(this.intentData == null ? buildUri() : this.intentData);
                arrayList.add(this.intentAction);
                arrayList.add(this.lookupKey);
                arrayList.add(this.filter);
                arrayList.add(this.lastAccessTime);
            } else {
                for (String str : strArr) {
                    addColumnValue(arrayList, str);
                }
            }
            return arrayList;
        }

        private void addColumnValue(ArrayList<Object> arrayList, String str) {
            if ("_id".equals(str)) {
                arrayList.add(Long.valueOf(this.contactId));
                return;
            }
            if ("suggest_text_1".equals(str)) {
                arrayList.add(this.text1);
                return;
            }
            if ("suggest_text_2".equals(str)) {
                arrayList.add(this.text2);
                return;
            }
            if ("suggest_icon_1".equals(str)) {
                arrayList.add(this.icon1);
                return;
            }
            if ("suggest_icon_2".equals(str)) {
                arrayList.add(this.icon2);
                return;
            }
            if ("suggest_intent_data".equals(str)) {
                arrayList.add(this.intentData == null ? buildUri() : this.intentData);
                return;
            }
            if ("suggest_intent_data_id".equals(str)) {
                arrayList.add(this.lookupKey);
                return;
            }
            if ("suggest_shortcut_id".equals(str)) {
                arrayList.add(this.lookupKey);
                return;
            }
            if ("suggest_intent_extra_data".equals(str)) {
                arrayList.add(this.filter);
            } else {
                if ("suggest_last_access_hint".equals(str)) {
                    arrayList.add(this.lastAccessTime);
                    return;
                }
                throw new IllegalArgumentException("Invalid column name: " + str);
            }
        }

        private String buildUri() {
            return ContactsContract.Contacts.getLookupUri(this.contactId, this.lookupKey).toString();
        }

        public void reset() {
            this.contactId = 0L;
            this.photoUri = null;
            this.lookupKey = null;
            this.presence = -1;
            this.text1 = null;
            this.text2 = null;
            this.icon1 = null;
            this.icon2 = null;
            this.intentData = null;
            this.intentAction = null;
            this.filter = null;
            this.lastAccessTime = null;
        }
    }

    public GlobalSearchSupport(ContactsProvider2 contactsProvider2) {
        this.mContactsProvider = contactsProvider2;
    }

    public Cursor handleSearchSuggestionsQuery(SQLiteDatabase sQLiteDatabase, Uri uri, String[] strArr, String str, CancellationSignal cancellationSignal) {
        MatrixCursor matrixCursor = new MatrixCursor(strArr == null ? SEARCH_SUGGESTIONS_COLUMNS : strArr);
        if (uri.getPathSegments().size() > 1) {
            addSearchSuggestionsBasedOnFilter(matrixCursor, sQLiteDatabase, strArr, null, uri.getLastPathSegment(), str, cancellationSignal);
        }
        Log.d("GlobalSearchSupport", "[handleSearchSuggestionsQuery]result cursor count:" + Integer.valueOf(matrixCursor.getCount()));
        return matrixCursor;
    }

    public Cursor handleSearchShortcutRefresh(SQLiteDatabase sQLiteDatabase, String[] strArr, String str, String str2, CancellationSignal cancellationSignal) {
        long jLookupContactIdByLookupKey;
        try {
            jLookupContactIdByLookupKey = this.mContactsProvider.lookupContactIdByLookupKey(sQLiteDatabase, str);
        } catch (IllegalArgumentException e) {
            jLookupContactIdByLookupKey = -1;
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr == null ? SEARCH_SUGGESTIONS_COLUMNS : strArr);
        Log.d("GlobalSearchSupport", "[handleSearchShortcutRefresh]contactId:" + jLookupContactIdByLookupKey);
        Cursor cursorAddSearchSuggestionsBasedOnFilter = addSearchSuggestionsBasedOnFilter(matrixCursor, sQLiteDatabase, strArr, "contacts._id=" + jLookupContactIdByLookupKey, (str2 == null || str2.equals("null")) ? null : str2, null, cancellationSignal);
        StringBuilder sb = new StringBuilder();
        sb.append("[handleSearchShortcutRefresh]Result cursor count:");
        sb.append(cursorAddSearchSuggestionsBasedOnFilter == null ? "NULL" : Integer.valueOf(cursorAddSearchSuggestionsBasedOnFilter.getCount()));
        Log.d("GlobalSearchSupport", sb.toString());
        return GlobalSearchSupportEx.processCursor(cursorAddSearchSuggestionsBasedOnFilter, strArr, str, SEARCH_SUGGESTIONS_COLUMNS);
    }

    private Cursor addSearchSuggestionsBasedOnFilter(MatrixCursor matrixCursor, SQLiteDatabase sQLiteDatabase, String[] strArr, String str, String str2, String str3, CancellationSignal cancellationSignal) {
        StringBuilder sb = new StringBuilder();
        boolean z = !TextUtils.isEmpty(str2);
        sb.append("SELECT _id, lookup, photo_thumb_uri, display_name, (SELECT mode FROM agg_presence WHERE presence_contact_id=contacts._id) AS contact_presence, last_time_contacted");
        if (z) {
            sb.append(", snippet");
        }
        sb.append(", indicate_phone_or_sim_contact");
        sb.append(", is_sdn_contact");
        sb.append(" FROM ");
        sb.append("view_contacts");
        sb.append(" AS contacts");
        if (z) {
            this.mContactsProvider.appendSearchIndexJoin(sb, str2, true, String.valueOf((char) 1), String.valueOf((char) 1), "…", 5, false);
        }
        sb.append(" WHERE lookup IS NOT NULL");
        if (str != null) {
            sb.append(" AND ");
            sb.append(str);
        }
        if (str3 != null) {
            sb.append(" LIMIT " + str3);
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery(sb.toString(), null, cancellationSignal);
        SearchSuggestion searchSuggestion = new SearchSuggestion();
        searchSuggestion.filter = str2;
        while (cursorRawQuery.moveToNext()) {
            try {
                searchSuggestion.contactId = cursorRawQuery.getLong(0);
                searchSuggestion.lookupKey = cursorRawQuery.getString(1);
                searchSuggestion.photoUri = cursorRawQuery.getString(2);
                searchSuggestion.text1 = cursorRawQuery.getString(3);
                searchSuggestion.presence = cursorRawQuery.isNull(4) ? -1 : cursorRawQuery.getInt(4);
                searchSuggestion.lastAccessTime = cursorRawQuery.getString(5);
                int i = 6;
                if (z) {
                    searchSuggestion.text2 = shortenSnippet(cursorRawQuery.getString(6));
                }
                if (z) {
                    i = 7;
                }
                getCursorField(cursorRawQuery, searchSuggestion, i);
                matrixCursor.addRow(searchSuggestion.asList(strArr));
                searchSuggestion.reset();
            } catch (Throwable th) {
                cursorRawQuery.close();
                throw th;
            }
        }
        cursorRawQuery.close();
        long jCurrentTimeMillis2 = System.currentTimeMillis();
        Log.i("GlobalSearchSupport", "startTime : " + jCurrentTimeMillis + " | endTime : " + jCurrentTimeMillis2 + " | time : " + (jCurrentTimeMillis2 - jCurrentTimeMillis));
        return matrixCursor;
    }

    private String shortenSnippet(String str) {
        int iIndexOf;
        if (str == null) {
            return null;
        }
        int i = 0;
        int length = str.length();
        int iIndexOf2 = str.indexOf(1);
        if (iIndexOf2 == -1) {
            return null;
        }
        int iLastIndexOf = str.lastIndexOf(10, iIndexOf2);
        if (iLastIndexOf != -1) {
            i = iLastIndexOf + 1;
        }
        int iLastIndexOf2 = str.lastIndexOf(1);
        if (iLastIndexOf2 == -1 || (iIndexOf = str.indexOf(10, iLastIndexOf2)) == -1) {
            iIndexOf = length;
        }
        StringBuilder sb = new StringBuilder();
        while (i < iIndexOf) {
            char cCharAt = str.charAt(i);
            if (cCharAt != 1 && cCharAt != 1) {
                sb.append(cCharAt);
            }
            i++;
        }
        return sb.toString();
    }

    private void getCursorField(Cursor cursor, SearchSuggestion searchSuggestion, int i) {
        searchSuggestion.mSimIndicator = cursor.getInt(i);
        searchSuggestion.mIsSdnContact = cursor.getInt(i + 1);
        Context context = this.mContactsProvider.getContext();
        SubscriptionInfo activeSubscriptionInfo = SubscriptionManager.from(context).getActiveSubscriptionInfo(searchSuggestion.mSimIndicator);
        if (context != null && searchSuggestion.mSimIndicator > 0) {
            searchSuggestion.mSlotId = activeSubscriptionInfo.getSimSlotIndex();
        } else {
            searchSuggestion.mSlotId = -1;
        }
    }
}
