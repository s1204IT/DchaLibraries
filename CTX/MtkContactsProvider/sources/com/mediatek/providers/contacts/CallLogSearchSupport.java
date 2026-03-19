package com.mediatek.providers.contacts;

import android.R;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import com.android.providers.contacts.ContactsProvider2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class CallLogSearchSupport {
    private static final String[] SEARCH_SUGGESTIONS_BASED_ON_NAME_COLUMNS = {"_id", "suggest_text_1", "suggest_text_2", "suggest_icon_1", "suggest_icon_2", "suggest_intent_data_id", "suggest_shortcut_id"};
    private final Context mContext;

    private interface SearchSuggestionQuery {
        public static final String[] COLUMNS = {"_id", "number", "date", "type", "numbertype", "name", "photo_uri"};
    }

    public CallLogSearchSupport(Context context) {
        this.mContext = context;
    }

    private static class SearchSuggestion {
        long mCallsId;
        long mDate;
        String mIcon1;
        String mIcon2;
        int mIsVTCall;
        String mPhotoUri;
        boolean mProcessed;
        String mSortKey;
        String mText1;
        String mText2;
        int mType;

        public SearchSuggestion(long j) {
            this.mCallsId = j;
        }

        private void process() {
            if (this.mProcessed) {
                return;
            }
            Log.i("CallLogSearchSupport", "mPhotoUri : " + this.mPhotoUri);
            if (this.mPhotoUri != null) {
                this.mIcon1 = this.mPhotoUri.toString();
            } else {
                this.mIcon1 = String.valueOf(R.drawable.dropdown_normal_holo_dark);
            }
            int i = this.mType;
            if (i != 8) {
                switch (i) {
                    case 1:
                        if (this.mIsVTCall <= 0) {
                            this.mIcon2 = String.valueOf(com.android.providers.contacts.R.drawable.mtk_incoming_search);
                        } else {
                            this.mIcon2 = String.valueOf(com.android.providers.contacts.R.drawable.mtk_video_incoming_search);
                        }
                        break;
                    case 2:
                        if (this.mIsVTCall <= 0) {
                            this.mIcon2 = String.valueOf(com.android.providers.contacts.R.drawable.mtk_outing_search);
                        } else {
                            this.mIcon2 = String.valueOf(com.android.providers.contacts.R.drawable.mtk_video_outing_search);
                        }
                        break;
                    case 3:
                        if (this.mIsVTCall <= 0) {
                            this.mIcon2 = String.valueOf(com.android.providers.contacts.R.drawable.mtk_missed_search);
                        } else {
                            this.mIcon2 = String.valueOf(com.android.providers.contacts.R.drawable.mtk_video_missed_search);
                        }
                        break;
                }
            } else {
                this.mIcon2 = String.valueOf(com.android.providers.contacts.R.drawable.mtk_autoreject_search);
            }
            this.mProcessed = true;
        }

        public String getSortKey() {
            if (this.mSortKey == null) {
                process();
                this.mSortKey = Long.toString(this.mDate);
            }
            return this.mSortKey;
        }

        public ArrayList asList(String[] strArr) {
            process();
            ArrayList<Object> arrayList = new ArrayList<>();
            if (strArr == null) {
                arrayList.add(Long.valueOf(this.mCallsId));
                arrayList.add(this.mText1);
                arrayList.add(this.mText2);
                arrayList.add(this.mIcon1);
                arrayList.add(this.mIcon2);
                arrayList.add(Long.valueOf(this.mCallsId));
                arrayList.add(Long.valueOf(this.mCallsId));
            } else {
                for (String str : strArr) {
                    addColumnValue(arrayList, str);
                }
            }
            return arrayList;
        }

        private void addColumnValue(ArrayList<Object> arrayList, String str) {
            if ("_id".equals(str)) {
                arrayList.add(Long.valueOf(this.mCallsId));
                return;
            }
            if ("suggest_text_1".equals(str)) {
                arrayList.add(this.mText1);
                return;
            }
            if ("suggest_text_2".equals(str)) {
                arrayList.add(this.mText2);
                return;
            }
            if ("suggest_icon_1".equals(str)) {
                arrayList.add(this.mIcon1);
                return;
            }
            if ("suggest_icon_2".equals(str)) {
                arrayList.add(this.mIcon2);
                return;
            }
            if ("suggest_intent_data_id".equals(str)) {
                arrayList.add(Long.valueOf(this.mCallsId));
            } else {
                if ("suggest_shortcut_id".equals(str)) {
                    arrayList.add(Long.valueOf(this.mCallsId));
                    return;
                }
                throw new IllegalArgumentException("Invalid column name: " + str);
            }
        }
    }

    public Cursor handleSearchSuggestionsQuery(SQLiteDatabase sQLiteDatabase, Uri uri, String str) {
        if (uri.getPathSegments().size() <= 1) {
            return null;
        }
        return buildCursorForSearchSuggestions(sQLiteDatabase, uri.getLastPathSegment(), str);
    }

    private Cursor buildCursorForSearchSuggestions(SQLiteDatabase sQLiteDatabase, String str, String str2) {
        return buildCursorForSearchSuggestions(sQLiteDatabase, null, null, str, str2);
    }

    private Cursor buildCursorForSearchSuggestions(SQLiteDatabase sQLiteDatabase, String[] strArr, String str, String str2, String str3) {
        String str4;
        String str5;
        int i;
        String strStripSeparators;
        ArrayList arrayList = new ArrayList();
        HashMap map = new HashMap();
        boolean z = !TextUtils.isEmpty(str2);
        if (!TextUtils.isEmpty(str)) {
            str4 = str;
            if ("null".equals(str4)) {
                str4 = null;
            }
        }
        if (z) {
            if (isPhoneNumber(str2)) {
                strStripSeparators = PhoneNumberUtils.stripSeparators(str2);
            } else {
                strStripSeparators = str2;
            }
            str5 = TextUtils.isEmpty(str4) ? "calls.number GLOB '*" + strStripSeparators + "*' OR (name GLOB '*" + str2 + "*')" : str4 + " AND calls.number GLOB '*" + strStripSeparators + "*' OR (name GLOB '*" + str2 + "*')";
        } else {
            str5 = str4;
        }
        Cursor cursorQuery = sQLiteDatabase.query(false, "calls", SearchSuggestionQuery.COLUMNS, str5, null, "calls._id", null, null, str3);
        Log.d("CallLogSearchSupport", "[buildCursorForSearchSuggestions] where = " + str5 + "; Count =" + cursorQuery.getCount());
        while (true) {
            try {
                try {
                    if (!cursorQuery.moveToNext()) {
                        break;
                    }
                    long j = cursorQuery.getLong(0);
                    SearchSuggestion searchSuggestion = (SearchSuggestion) map.get(Long.valueOf(j));
                    if (searchSuggestion == null) {
                        searchSuggestion = new SearchSuggestion(j);
                        map.put(Long.valueOf(j), searchSuggestion);
                    }
                    searchSuggestion.mDate = cursorQuery.getLong(2);
                    new Time().set(searchSuggestion.mDate);
                    String string = cursorQuery.getString(1);
                    String string2 = cursorQuery.getString(5);
                    if (TextUtils.isEmpty(string2)) {
                        searchSuggestion.mText1 = this.mContext.getResources().getString(com.android.providers.contacts.R.string.unknown);
                        searchSuggestion.mText2 = string;
                    } else {
                        searchSuggestion.mText1 = string2;
                        searchSuggestion.mText2 = ((String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(this.mContext.getResources(), cursorQuery.getInt(4), null)) + " " + string;
                    }
                    searchSuggestion.mType = cursorQuery.getInt(3);
                    searchSuggestion.mPhotoUri = cursorQuery.getString(6);
                    arrayList.add(searchSuggestion);
                } catch (Exception e) {
                    Log.e("CallLogSearchSupport", "[buildCursorForSearchSuggestions] catched exception !!!");
                    e.printStackTrace();
                }
            } finally {
                cursorQuery.close();
            }
        }
        Collections.sort(arrayList, new Comparator<SearchSuggestion>() {
            @Override
            public int compare(SearchSuggestion searchSuggestion2, SearchSuggestion searchSuggestion3) {
                return searchSuggestion2.getSortKey().compareTo(searchSuggestion3.getSortKey());
            }
        });
        MatrixCursor matrixCursor = new MatrixCursor(strArr != null ? strArr : SEARCH_SUGGESTIONS_BASED_ON_NAME_COLUMNS);
        for (i = 0; i < arrayList.size(); i++) {
            matrixCursor.addRow(((SearchSuggestion) arrayList.get(i)).asList(strArr));
        }
        Log.i("CallLogSearchSupport", "[buildCursorForSearchSuggestions] retCursor = " + matrixCursor.getCount());
        return matrixCursor;
    }

    public Cursor handleSearchShortcutRefresh(SQLiteDatabase sQLiteDatabase, String[] strArr, String str, String str2) {
        return buildCursorForSearchSuggestions(sQLiteDatabase, null, "calls._id=" + str, null, null);
    }

    public boolean isPhoneNumber(String str) {
        return !TextUtils.isEmpty(str) && ContactsProvider2.countPhoneNumberDigits(str) > 0;
    }
}
