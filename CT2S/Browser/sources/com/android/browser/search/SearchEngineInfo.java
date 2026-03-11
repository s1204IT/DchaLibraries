package com.android.browser.search;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import com.android.browser.R;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Locale;

public class SearchEngineInfo {
    private static String TAG = "SearchEngineInfo";
    private final String mName;
    private final String[] mSearchEngineData;

    public SearchEngineInfo(Context context, String name) throws IllegalArgumentException {
        this.mName = name;
        Resources res = context.getResources();
        String packageName = R.class.getPackage().getName();
        int id_data = res.getIdentifier(name, "array", packageName);
        if (id_data == 0) {
            throw new IllegalArgumentException("No resources found for " + name);
        }
        this.mSearchEngineData = res.getStringArray(id_data);
        if (this.mSearchEngineData == null) {
            throw new IllegalArgumentException("No data found for " + name);
        }
        if (this.mSearchEngineData.length != 6) {
            throw new IllegalArgumentException(name + " has invalid number of fields - " + this.mSearchEngineData.length);
        }
        if (TextUtils.isEmpty(this.mSearchEngineData[3])) {
            throw new IllegalArgumentException(name + " has an empty search URI");
        }
        Locale locale = context.getResources().getConfiguration().locale;
        StringBuilder language = new StringBuilder(locale.getLanguage());
        if (!TextUtils.isEmpty(locale.getCountry())) {
            language.append('-');
            language.append(locale.getCountry());
        }
        String language_str = language.toString();
        this.mSearchEngineData[3] = this.mSearchEngineData[3].replace("{language}", language_str);
        this.mSearchEngineData[5] = this.mSearchEngineData[5].replace("{language}", language_str);
        String enc = this.mSearchEngineData[4];
        if (TextUtils.isEmpty(enc)) {
            enc = "UTF-8";
            this.mSearchEngineData[4] = "UTF-8";
        }
        this.mSearchEngineData[3] = this.mSearchEngineData[3].replace("{inputEncoding}", enc);
        this.mSearchEngineData[5] = this.mSearchEngineData[5].replace("{inputEncoding}", enc);
    }

    public String getName() {
        return this.mName;
    }

    public String getLabel() {
        return this.mSearchEngineData[0];
    }

    public String getSearchUriForQuery(String query) {
        return getFormattedUri(searchUri(), query);
    }

    public String getSuggestUriForQuery(String query) {
        return getFormattedUri(suggestUri(), query);
    }

    public boolean supportsSuggestions() {
        return !TextUtils.isEmpty(suggestUri());
    }

    private String suggestUri() {
        return this.mSearchEngineData[5];
    }

    private String searchUri() {
        return this.mSearchEngineData[3];
    }

    private String getFormattedUri(String templateUri, String query) {
        if (TextUtils.isEmpty(templateUri)) {
            return null;
        }
        String enc = this.mSearchEngineData[4];
        try {
            return templateUri.replace("{searchTerms}", URLEncoder.encode(query, enc));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Exception occured when encoding query " + query + " to " + enc);
            return null;
        }
    }

    public String toString() {
        return "SearchEngineInfo{" + Arrays.toString(this.mSearchEngineData) + "}";
    }
}
