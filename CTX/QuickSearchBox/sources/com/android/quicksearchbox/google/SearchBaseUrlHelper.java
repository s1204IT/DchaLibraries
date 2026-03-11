package com.android.quicksearchbox.google;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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

    private void checkSearchDomain() {
        new AsyncTask<Void, Void, Void>(this, new HttpHelper.GetRequest("https://www.google.com/searchdomaincheck?format=domain")) {
            final SearchBaseUrlHelper this$0;
            final HttpHelper.GetRequest val$request;

            {
                this.this$0 = this;
                this.val$request = getRequest;
            }

            @Override
            public Void doInBackground(Void... voidArr) {
                try {
                    this.this$0.setSearchBaseDomain(this.this$0.mHttpHelper.get(this.val$request));
                } catch (Exception e) {
                    this.this$0.getDefaultBaseDomain();
                }
                return null;
            }
        }.execute(new Void[0]);
    }

    public String getDefaultBaseDomain() {
        return this.mContext.getResources().getString(2131296261);
    }

    public void setSearchBaseDomain(String str) {
        this.mSearchSettings.setSearchBaseDomain(str);
    }

    public String getSearchBaseUrl() {
        return this.mContext.getResources().getString(2131296260, getSearchDomain(), GoogleSearch.getLanguage(Locale.getDefault()));
    }

    public String getSearchDomain() {
        String searchBaseDomain = this.mSearchSettings.getSearchBaseDomain();
        if (searchBaseDomain == null) {
            searchBaseDomain = getDefaultBaseDomain();
        }
        if (!searchBaseDomain.startsWith(".")) {
            return searchBaseDomain;
        }
        return "www" + searchBaseDomain;
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        if ("use_google_com".equals(str)) {
            maybeUpdateBaseUrlSetting(true);
        }
    }
}
