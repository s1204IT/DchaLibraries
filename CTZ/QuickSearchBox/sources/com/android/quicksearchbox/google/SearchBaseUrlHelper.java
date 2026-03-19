package com.android.quicksearchbox.google;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SearchSettings;
import com.android.quicksearchbox.util.HttpHelper;
import java.util.Locale;

public class SearchBaseUrlHelper implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Context mContext;
    private final HttpHelper mHttpHelper;
    private final SearchSettings mSearchSettings;

    public SearchBaseUrlHelper(Context context, HttpHelper httpHelper, SearchSettings searchSettings, SharedPreferences sharedPreferences) {
        this.mHttpHelper = httpHelper;
        this.mContext = context;
        this.mSearchSettings = searchSettings;
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        maybeUpdateBaseUrlSetting(false);
    }

    public void maybeUpdateBaseUrlSetting(boolean z) {
        long searchBaseDomainApplyTime = this.mSearchSettings.getSearchBaseDomainApplyTime();
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (z || searchBaseDomainApplyTime == -1 || jCurrentTimeMillis - searchBaseDomainApplyTime >= 86400000) {
            if (this.mSearchSettings.shouldUseGoogleCom()) {
                setSearchBaseDomain(getDefaultBaseDomain());
            } else {
                checkSearchDomain();
            }
        }
    }

    public String getSearchBaseUrl() {
        return this.mContext.getResources().getString(R.string.google_search_base_pattern, getSearchDomain(), GoogleSearch.getLanguage(Locale.getDefault()));
    }

    public String getSearchDomain() {
        String searchBaseDomain = this.mSearchSettings.getSearchBaseDomain();
        if (searchBaseDomain == null) {
            searchBaseDomain = getDefaultBaseDomain();
        }
        if (searchBaseDomain.startsWith(".")) {
            return "www" + searchBaseDomain;
        }
        return searchBaseDomain;
    }

    private void checkSearchDomain() {
        final HttpHelper.GetRequest getRequest = new HttpHelper.GetRequest("https://www.google.com/searchdomaincheck?format=domain");
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) {
                try {
                    SearchBaseUrlHelper.this.setSearchBaseDomain(SearchBaseUrlHelper.this.mHttpHelper.get(getRequest));
                    return null;
                } catch (Exception e) {
                    SearchBaseUrlHelper.this.getDefaultBaseDomain();
                    return null;
                }
            }
        }.execute(new Void[0]);
    }

    private String getDefaultBaseDomain() {
        return this.mContext.getResources().getString(R.string.default_search_domain);
    }

    private void setSearchBaseDomain(String str) {
        this.mSearchSettings.setSearchBaseDomain(str);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        if ("use_google_com".equals(str)) {
            maybeUpdateBaseUrlSetting(true);
        }
    }
}
