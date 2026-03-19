package com.android.quicksearchbox;

import android.content.Context;
import android.content.SharedPreferences;
import com.android.common.SharedPreferencesCompat;

public class SearchSettingsImpl implements SearchSettings {
    private final Config mConfig;
    private final Context mContext;

    public SearchSettingsImpl(Context context, Config config) {
        this.mContext = context;
        this.mConfig = config;
    }

    protected Context getContext() {
        return this.mContext;
    }

    @Override
    public void upgradeSettingsIfNeeded() {
    }

    public SharedPreferences getSearchPreferences() {
        return getContext().getSharedPreferences("SearchSettings", 0);
    }

    @Override
    public boolean shouldUseGoogleCom() {
        return getSearchPreferences().getBoolean("use_google_com", true);
    }

    @Override
    public long getSearchBaseDomainApplyTime() {
        return getSearchPreferences().getLong("search_base_domain_apply_time", -1L);
    }

    @Override
    public String getSearchBaseDomain() {
        return getSearchPreferences().getString("search_base_domain", null);
    }

    @Override
    public void setSearchBaseDomain(String str) {
        SharedPreferences.Editor editorEdit = getSearchPreferences().edit();
        editorEdit.putString("search_base_domain", str);
        editorEdit.putLong("search_base_domain_apply_time", System.currentTimeMillis());
        SharedPreferencesCompat.apply(editorEdit);
    }
}
