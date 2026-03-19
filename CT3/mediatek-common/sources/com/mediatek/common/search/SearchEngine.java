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
        public SearchEngine createFromParcel(Parcel in) {
            return new SearchEngine(in);
        }

        @Override
        public SearchEngine[] newArray(int size) {
            return new SearchEngine[size];
        }
    };

    public SearchEngine(String name, String[] data) {
        this.mName = name;
        this.mSearchEngineData = data;
    }

    public String getName() {
        return this.mName;
    }

    public String getLabel() {
        return this.mSearchEngineData[0];
    }

    public String getSearchUriForQuery(String query) {
        return getFormattedUri(getSearchUri(), query);
    }

    public String getSuggestUriForQuery(String query) {
        return getFormattedUri(getSuggestUri(), query);
    }

    public boolean supportsSuggestions() {
        if (TextUtils.isEmpty(getSuggestUri())) {
            return false;
        }
        return DBG;
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

    private String getFormattedUri(String templateUri, String query) {
        if (TextUtils.isEmpty(templateUri)) {
            return null;
        }
        String enc = this.mSearchEngineData[4];
        try {
            return templateUri.replace(PARAMETER_SEARCH_TERMS, URLEncoder.encode(query, enc));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Exception occured when encoding query " + query + " to " + enc);
            return null;
        }
    }

    public String toString() {
        return "SearchEngine{" + Arrays.toString(this.mSearchEngineData) + "}";
    }

    SearchEngine(Parcel in) {
        this.mName = in.readString();
        this.mSearchEngineData = new String[6];
        in.readStringArray(this.mSearchEngineData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mName);
        dest.writeStringArray(this.mSearchEngineData);
    }

    public static SearchEngine parseFrom(String configInfo, String sp) throws IllegalArgumentException {
        Log.i(TAG, "Parse From config file " + configInfo);
        if (configInfo == null || "".equals(configInfo)) {
            throw new IllegalArgumentException("Empty config info");
        }
        if (sp == null || "".equals(sp)) {
            sp = DEFAULT_SP;
        }
        String[] configData = configInfo.split(sp);
        if (configData.length != 7) {
            throw new IllegalArgumentException("Field Missing");
        }
        String engineName = parseField(configData, -1);
        String engineLabel = parseField(configData, 0);
        String engineKeyword = parseField(configData, 1);
        String engineFavicon = parseField(configData, 2);
        String engineSearchUri = parseField(configData, 3);
        String engineEncoding = parseField(configData, 4);
        String engineSuggestUri = parseField(configData, 5);
        Log.i(TAG, "SearchEngine consturctor called, search engine name is: " + engineName);
        if (engineSearchUri == null) {
            throw new IllegalArgumentException(engineName + " has an empty search URI");
        }
        Locale locale = Locale.getDefault();
        StringBuilder language = new StringBuilder(locale.getLanguage());
        if (!TextUtils.isEmpty(locale.getCountry())) {
            language.append('-');
            language.append(locale.getCountry());
        }
        String language_str = language.toString();
        String engineSearchUri2 = engineSearchUri.replace(PARAMETER_LANGUAGE, language_str);
        if (engineSuggestUri != null) {
            engineSuggestUri = engineSuggestUri.replace(PARAMETER_LANGUAGE, language_str);
        }
        if (engineEncoding == null) {
            engineEncoding = "UTF-8";
        }
        String engineSearchUri3 = engineSearchUri2.replace(PARAMETER_INPUT_ENCODING, engineEncoding);
        if (engineSuggestUri != null) {
            engineSuggestUri = engineSuggestUri.replace(PARAMETER_INPUT_ENCODING, engineEncoding);
        }
        String[] datas = {engineLabel, engineKeyword, engineFavicon, engineSearchUri3, engineEncoding, engineSuggestUri};
        SearchEngine newInstance = new SearchEngine(engineName, datas);
        return newInstance;
    }

    private static String parseField(String[] data, int fieldIndex) {
        int realFieldIndex = fieldIndex + 1;
        if (data.length - 1 < realFieldIndex || TextUtils.isEmpty(data[realFieldIndex]) || EMPTY.equals(data[realFieldIndex])) {
            return null;
        }
        return data[realFieldIndex];
    }
}
