package com.android.quicksearchbox;

/* loaded from: classes.dex */
public interface SearchSettings {
    String getSearchBaseDomain();

    long getSearchBaseDomainApplyTime();

    void setSearchBaseDomain(String str);

    boolean shouldUseGoogleCom();

    void upgradeSettingsIfNeeded();
}
