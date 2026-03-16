package com.android.providers.contacts;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.ContactsContract;
import android.text.TextUtils;
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
        String photoUri;
        int presence;
        String text1;
        String text2;

        private SearchSuggestion() {
            this.presence = -1;
        }

        public ArrayList<?> asList(String[] projection) {
            if (this.icon1 == null) {
                if (this.photoUri != null) {
                    this.icon1 = this.photoUri.toString();
                } else {
                    this.icon1 = String.valueOf(android.R.drawable.emo_im_laughing);
                }
            }
            if (this.presence != -1) {
                this.icon2 = String.valueOf(ContactsContract.StatusUpdates.getPresenceIconResourceId(this.presence));
            }
            ArrayList<?> arrayList = new ArrayList<>();
            if (projection == null) {
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
                for (String str : projection) {
                    addColumnValue(arrayList, str);
                }
            }
            return arrayList;
        }

        private void addColumnValue(ArrayList<Object> list, String column) {
            if ("_id".equals(column)) {
                list.add(Long.valueOf(this.contactId));
                return;
            }
            if ("suggest_text_1".equals(column)) {
                list.add(this.text1);
                return;
            }
            if ("suggest_text_2".equals(column)) {
                list.add(this.text2);
                return;
            }
            if ("suggest_icon_1".equals(column)) {
                list.add(this.icon1);
                return;
            }
            if ("suggest_icon_2".equals(column)) {
                list.add(this.icon2);
                return;
            }
            if ("suggest_intent_data".equals(column)) {
                list.add(this.intentData == null ? buildUri() : this.intentData);
                return;
            }
            if ("suggest_intent_data_id".equals(column)) {
                list.add(this.lookupKey);
                return;
            }
            if ("suggest_shortcut_id".equals(column)) {
                list.add(this.lookupKey);
            } else if ("suggest_intent_extra_data".equals(column)) {
                list.add(this.filter);
            } else {
                if ("suggest_last_access_hint".equals(column)) {
                    list.add(this.lastAccessTime);
                    return;
                }
                throw new IllegalArgumentException("Invalid column name: " + column);
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

    public GlobalSearchSupport(ContactsProvider2 contactsProvider) {
        this.mContactsProvider = contactsProvider;
    }

    public Cursor handleSearchSuggestionsQuery(SQLiteDatabase db, Uri uri, String[] projection, String limit, CancellationSignal cancellationSignal) {
        MatrixCursor cursor = new MatrixCursor(projection == null ? SEARCH_SUGGESTIONS_COLUMNS : projection);
        if (uri.getPathSegments().size() > 1) {
            String searchClause = uri.getLastPathSegment();
            addSearchSuggestionsBasedOnFilter(cursor, db, projection, null, searchClause, limit, cancellationSignal);
        }
        return cursor;
    }

    public Cursor handleSearchShortcutRefresh(SQLiteDatabase db, String[] projection, String lookupKey, String filter, CancellationSignal cancellationSignal) {
        long contactId;
        try {
            contactId = this.mContactsProvider.lookupContactIdByLookupKey(db, lookupKey);
        } catch (IllegalArgumentException e) {
            contactId = -1;
        }
        MatrixCursor cursor = new MatrixCursor(projection == null ? SEARCH_SUGGESTIONS_COLUMNS : projection);
        return addSearchSuggestionsBasedOnFilter(cursor, db, projection, "contacts._id=" + contactId, filter, null, cancellationSignal);
    }

    private Cursor addSearchSuggestionsBasedOnFilter(MatrixCursor cursor, SQLiteDatabase db, String[] projection, String selection, String filter, String limit, CancellationSignal cancellationSignal) {
        StringBuilder sb = new StringBuilder();
        boolean haveFilter = !TextUtils.isEmpty(filter);
        sb.append("SELECT _id, lookup, photo_thumb_uri, display_name, (SELECT mode FROM agg_presence WHERE presence_contact_id=contacts._id) AS contact_presence, last_time_contacted");
        if (haveFilter) {
            sb.append(", snippet");
        }
        sb.append(" FROM ");
        sb.append("view_contacts");
        sb.append(" AS contacts");
        if (haveFilter) {
            this.mContactsProvider.appendSearchIndexJoin(sb, filter, true, String.valueOf((char) 1), String.valueOf((char) 1), "…", 5, false);
        }
        if (selection != null) {
            sb.append(" WHERE ").append(selection);
        }
        if (limit != null) {
            sb.append(" LIMIT " + limit);
        }
        Cursor c = db.rawQuery(sb.toString(), null, cancellationSignal);
        SearchSuggestion suggestion = new SearchSuggestion();
        suggestion.filter = filter;
        while (c.moveToNext()) {
            try {
                suggestion.contactId = c.getLong(0);
                suggestion.lookupKey = c.getString(1);
                suggestion.photoUri = c.getString(2);
                suggestion.text1 = c.getString(3);
                suggestion.presence = c.isNull(4) ? -1 : c.getInt(4);
                suggestion.lastAccessTime = c.getString(5);
                if (haveFilter) {
                    suggestion.text2 = shortenSnippet(c.getString(6));
                }
                cursor.addRow(suggestion.asList(projection));
                suggestion.reset();
            } finally {
                c.close();
            }
        }
        return cursor;
    }

    private String shortenSnippet(String snippet) {
        int lastNl;
        if (snippet == null) {
            return null;
        }
        int from = 0;
        int to = snippet.length();
        int start = snippet.indexOf(1);
        if (start == -1) {
            return null;
        }
        int firstNl = snippet.lastIndexOf(10, start);
        if (firstNl != -1) {
            from = firstNl + 1;
        }
        int end = snippet.lastIndexOf(1);
        if (end != -1 && (lastNl = snippet.indexOf(10, end)) != -1) {
            to = lastNl;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            char c = snippet.charAt(i);
            if (c != 1 && c != 1) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
