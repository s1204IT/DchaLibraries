package com.android.contacts.common;

public final class GroupMetaData {
    private String mAccountName;
    private String mAccountType;
    private String mDataSet;
    private boolean mDefaultGroup;
    private boolean mFavorites;
    private long mGroupId;
    private String mTitle;

    public GroupMetaData(String accountName, String accountType, String dataSet, long groupId, String title, boolean defaultGroup, boolean favorites) {
        this.mAccountName = accountName;
        this.mAccountType = accountType;
        this.mDataSet = dataSet;
        this.mGroupId = groupId;
        this.mTitle = title;
        this.mDefaultGroup = defaultGroup;
        this.mFavorites = favorites;
    }

    public long getGroupId() {
        return this.mGroupId;
    }

    public boolean isDefaultGroup() {
        return this.mDefaultGroup;
    }
}
