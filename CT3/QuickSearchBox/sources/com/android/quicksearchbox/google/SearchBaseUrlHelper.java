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

    public SearchBaseUrlHelper(Context context, HttpHelper helper, SearchSettings searchSettings, SharedPreferences prefs) {
        this.mHttpHelper = helper;
        this.mContext = context;
        this.mSearchSettings = searchSettings;
        prefs.registerOnSharedPreferenceChangeListener(this);
        maybeUpdateBaseUrlSetting(false);
    }

    public void maybeUpdateBaseUrlSetting(boolean force) {
        long lastUpdateTime = this.mSearchSettings.getSearchBaseDomainApplyTime();
        long currentTime = System.currentTimeMillis();
        if (!force && lastUpdateTime != -1 && currentTime - lastUpdateTime < 86400000) {
            return;
        }
        if (this.mSearchSettings.shouldUseGoogleCom()) {
            setSearchBaseDomain(getDefaultBaseDomain());
        } else {
            checkSearchDomain();
        }
    }

    public String getSearchBaseUrl() {
        return this.mContext.getResources().getString(R.string.google_search_base_pattern, getSearchDomain(), GoogleSearch.getLanguage(Locale.getDefault()));
    }

    public String getSearchDomain() {
        String domain = this.mSearchSettings.getSearchBaseDomain();
        if (domain == null) {
            domain = getDefaultBaseDomain();
        }
        if (domain.startsWith(".")) {
            return "www" + domain;
        }
        return domain;
    }

    private void checkSearchDomain() {
        final HttpHelper.GetRequest request = new HttpHelper.GetRequest("https://www.google.com/searchdomaincheck?format=domain");
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                try {
                    String domain = SearchBaseUrlHelper.this.mHttpHelper.get(request);
                    SearchBaseUrlHelper.this.setSearchBaseDomain(domain);
                    return null;
                } catch (Exception e) {
                    SearchBaseUrlHelper.this.getDefaultBaseDomain();
                    return null;
                }
            }
        }.execute(new Void[0]);
    }

    public String getDefaultBaseDomain() {
        return this.mContext.getResources().getString(R.string.default_search_domain);
    }

    public void setSearchBaseDomain(String domain) {
        this.mSearchSettings.setSearchBaseDomain(domain);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        if (!"use_google_com".equals(key)) {
            return;
        }
        maybeUpdateBaseUrlSetting(true);
    }
}
