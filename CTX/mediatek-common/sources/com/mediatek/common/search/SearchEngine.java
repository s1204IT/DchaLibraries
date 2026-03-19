package com.mediatek.common.search;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Locale;

public final class SearchEngine implements Parcelable {
    private static final boolean DBG = true;
    private static final String DEFAULT_SP = "--";
    private static final String EMPTY = "nil";
    public static final int FAVICON = 2;
    private static final int FIELD_ENCODING = 4;
    private static final int FIELD_FAVICON = 2;
    private static final int FIELD_KEYWORD = 1;
    private static final int FIELD_LABEL = 0;
    private static final int FIELD_SEARCH_URI = 3;
    private static final int FIELD_SUGGEST_URI = 5;
    public static final int NAME = -1;
    private static final int NUM_FIELDS = 6;
    private static final String PARAMETER_INPUT_ENCODING = "{inputEncoding}";
    private static final String PARAMETER_LANGUAGE = "{language}";
    private static final String PARAMETER_SEARCH_TERMS = "{searchTerms}";
    private final String mName;
    private final String[] mSearchEngineData;
    private static String TAG = "SearchEngine";
    public static final Parcelable.Creator<SearchEngine> CREATOR = new Parcelable.Creator<SearchEngine>() {
        @Override
        public SearchEngine createFromParcel(Parcel parcel) {
            return new SearchEngine(parcel);
        }

        @Override
        public SearchEngine[] newArray(int i) {
            return new SearchEngine[i];
        }
    };

    public SearchEngine(String str, String[] strArr) {
        this.mName = str;
        this.mSearchEngineData = strArr;
    }

    public String getName() {
        return this.mName;
    }

    public String getLabel() {
        return this.mSearchEngineData[0];
    }

    public String getSearchUriForQuery(String str) {
        return getFormattedUri(getSearchUri(), str);
    }

    public String getSuggestUriForQuery(String str) {
        return getFormattedUri(getSuggestUri(), str);
    }

    public boolean supportsSuggestions() {
        return TextUtils.isEmpty(getSuggestUri()) ^ DBG;
    }

    public String getKeyWord() {
        return this.mSearchEngineData[1];
    }

    public String getFaviconUri() {
        return this.mSearchEngineData[2];
    }

    private String getSuggestUri() {
        return this.mSearchEngineData[5];
    }

    private String getSearchUri() {
        return this.mSearchEngineData[3];
    }

    private String getFormattedUri(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String str3 = this.mSearchEngineData[4];
        try {
            return str.replace(PARAMETER_SEARCH_TERMS, URLEncoder.encode(str2, str3));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Exception occured when encoding query " + str2 + " to " + str3);
            return null;
        }
    }

    public String toString() {
        return "SearchEngine{" + Arrays.toString(this.mSearchEngineData) + "}";
    }

    SearchEngine(Parcel parcel) {
        this.mName = parcel.readString();
        this.mSearchEngineData = new String[6];
        parcel.readStringArray(this.mSearchEngineData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mName);
        parcel.writeStringArray(this.mSearchEngineData);
    }

    public static SearchEngine parseFrom(String str, String str2) throws IllegalArgumentException {
        Log.i(TAG, "Parse From config file " + str);
        if (str == null || "".equals(str)) {
            throw new IllegalArgumentException("Empty config info");
        }
        if (str2 == null || "".equals(str2)) {
            str2 = DEFAULT_SP;
        }
        String[] strArrSplit = str.split(str2);
        if (strArrSplit.length != 7) {
            throw new IllegalArgumentException("Field Missing");
        }
        String field = parseField(strArrSplit, -1);
        String field2 = parseField(strArrSplit, 0);
        String field3 = parseField(strArrSplit, 1);
        String field4 = parseField(strArrSplit, 2);
        String field5 = parseField(strArrSplit, 3);
        String field6 = parseField(strArrSplit, 4);
        String field7 = parseField(strArrSplit, 5);
        Log.i(TAG, "SearchEngine consturctor called, search engine name is: " + field);
        if (field5 == null) {
            throw new IllegalArgumentException(field + " has an empty search URI");
        }
        Locale locale = Locale.getDefault();
        StringBuilder sb = new StringBuilder(locale.getLanguage());
        if (!TextUtils.isEmpty(locale.getCountry())) {
            sb.append('-');
            sb.append(locale.getCountry());
        }
        String string = sb.toString();
        String strReplace = field5.replace(PARAMETER_LANGUAGE, string);
        if (field7 != null) {
            field7 = field7.replace(PARAMETER_LANGUAGE, string);
        }
        if (field6 == null) {
            field6 = "UTF-8";
        }
        String strReplace2 = strReplace.replace(PARAMETER_INPUT_ENCODING, field6);
        if (field7 != null) {
            field7 = field7.replace(PARAMETER_INPUT_ENCODING, field6);
        }
        return new SearchEngine(field, new String[]{field2, field3, field4, strReplace2, field6, field7});
    }

    private static String parseField(String[] strArr, int i) {
        int i2 = i + 1;
        if (strArr.length - 1 < i2 || TextUtils.isEmpty(strArr[i2]) || EMPTY.equals(strArr[i2])) {
            return null;
        }
        return strArr[i2];
    }
}
