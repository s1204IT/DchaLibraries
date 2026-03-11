package com.android.quicksearchbox;

public interface SearchSettings {
    String getSearchBaseDomain();

    long getSearchBaseDomainApplyTime();

    void setSearchBaseDomain(String str);

    boolean shouldUseGoogleCom();

    void upgradeSettingsIfNeeded();
}
