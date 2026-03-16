package com.android.contacts.common.model;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import com.android.contacts.common.GroupMetaData;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.util.DataStatus;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;

public class Contact {
    private final String mAltDisplayName;
    private final String mCustomRingtone;
    private String mDirectoryAccountName;
    private String mDirectoryAccountType;
    private String mDirectoryDisplayName;
    private int mDirectoryExportSupport;
    private final long mDirectoryId;
    private String mDirectoryType;
    private final String mDisplayName;
    private final int mDisplayNameSource;
    private final Exception mException;
    private ImmutableList<GroupMetaData> mGroups;
    private final long mId;
    private ImmutableList<AccountType> mInvitableAccountTypes;
    private final boolean mIsUserProfile;
    private final String mLookupKey;
    private final Uri mLookupUri;
    private final long mNameRawContactId;
    private final String mPhoneticName;
    private byte[] mPhotoBinaryData;
    private final long mPhotoId;
    private final String mPhotoUri;
    private final Integer mPresence;
    private ImmutableList<RawContact> mRawContacts;
    private final Uri mRequestedUri;
    private final boolean mSendToVoicemail;
    private final boolean mStarred;
    private final Status mStatus;
    private ImmutableMap<Long, DataStatus> mStatuses;
    private byte[] mThumbnailPhotoBinaryData;
    private final Uri mUri;

    private enum Status {
        LOADED,
        ERROR,
        NOT_FOUND
    }

    private Contact(Uri requestedUri, Status status, Exception exception) {
        if (status == Status.ERROR && exception == null) {
            throw new IllegalArgumentException("ERROR result must have exception");
        }
        this.mStatus = status;
        this.mException = exception;
        this.mRequestedUri = requestedUri;
        this.mLookupUri = null;
        this.mUri = null;
        this.mDirectoryId = -1L;
        this.mLookupKey = null;
        this.mId = -1L;
        this.mRawContacts = null;
        this.mStatuses = null;
        this.mNameRawContactId = -1L;
        this.mDisplayNameSource = 0;
        this.mPhotoId = -1L;
        this.mPhotoUri = null;
        this.mDisplayName = null;
        this.mAltDisplayName = null;
        this.mPhoneticName = null;
        this.mStarred = false;
        this.mPresence = null;
        this.mInvitableAccountTypes = null;
        this.mSendToVoicemail = false;
        this.mCustomRingtone = null;
        this.mIsUserProfile = false;
    }

    public static Contact forNotFound(Uri requestedUri) {
        return new Contact(requestedUri, Status.NOT_FOUND, null);
    }

    public Contact(Uri requestedUri, Uri uri, Uri lookupUri, long directoryId, String lookupKey, long id, long nameRawContactId, int displayNameSource, long photoId, String photoUri, String displayName, String altDisplayName, String phoneticName, boolean starred, Integer presence, boolean sendToVoicemail, String customRingtone, boolean isUserProfile) {
        this.mStatus = Status.LOADED;
        this.mException = null;
        this.mRequestedUri = requestedUri;
        this.mLookupUri = lookupUri;
        this.mUri = uri;
        this.mDirectoryId = directoryId;
        this.mLookupKey = lookupKey;
        this.mId = id;
        this.mRawContacts = null;
        this.mStatuses = null;
        this.mNameRawContactId = nameRawContactId;
        this.mDisplayNameSource = displayNameSource;
        this.mPhotoId = photoId;
        this.mPhotoUri = photoUri;
        this.mDisplayName = displayName;
        this.mAltDisplayName = altDisplayName;
        this.mPhoneticName = phoneticName;
        this.mStarred = starred;
        this.mPresence = presence;
        this.mInvitableAccountTypes = null;
        this.mSendToVoicemail = sendToVoicemail;
        this.mCustomRingtone = customRingtone;
        this.mIsUserProfile = isUserProfile;
    }

    public Contact(Uri requestedUri, Contact from) {
        this.mRequestedUri = requestedUri;
        this.mStatus = from.mStatus;
        this.mException = from.mException;
        this.mLookupUri = from.mLookupUri;
        this.mUri = from.mUri;
        this.mDirectoryId = from.mDirectoryId;
        this.mLookupKey = from.mLookupKey;
        this.mId = from.mId;
        this.mNameRawContactId = from.mNameRawContactId;
        this.mDisplayNameSource = from.mDisplayNameSource;
        this.mPhotoId = from.mPhotoId;
        this.mPhotoUri = from.mPhotoUri;
        this.mDisplayName = from.mDisplayName;
        this.mAltDisplayName = from.mAltDisplayName;
        this.mPhoneticName = from.mPhoneticName;
        this.mStarred = from.mStarred;
        this.mPresence = from.mPresence;
        this.mRawContacts = from.mRawContacts;
        this.mStatuses = from.mStatuses;
        this.mInvitableAccountTypes = from.mInvitableAccountTypes;
        this.mDirectoryDisplayName = from.mDirectoryDisplayName;
        this.mDirectoryType = from.mDirectoryType;
        this.mDirectoryAccountType = from.mDirectoryAccountType;
        this.mDirectoryAccountName = from.mDirectoryAccountName;
        this.mDirectoryExportSupport = from.mDirectoryExportSupport;
        this.mGroups = from.mGroups;
        this.mPhotoBinaryData = from.mPhotoBinaryData;
        this.mSendToVoicemail = from.mSendToVoicemail;
        this.mCustomRingtone = from.mCustomRingtone;
        this.mIsUserProfile = from.mIsUserProfile;
    }

    public void setDirectoryMetaData(String displayName, String directoryType, String accountType, String accountName, int exportSupport) {
        this.mDirectoryDisplayName = displayName;
        this.mDirectoryType = directoryType;
        this.mDirectoryAccountType = accountType;
        this.mDirectoryAccountName = accountName;
        this.mDirectoryExportSupport = exportSupport;
    }

    void setPhotoBinaryData(byte[] photoBinaryData) {
        this.mPhotoBinaryData = photoBinaryData;
    }

    void setThumbnailPhotoBinaryData(byte[] photoBinaryData) {
        this.mThumbnailPhotoBinaryData = photoBinaryData;
    }

    public Uri getLookupUri() {
        return this.mLookupUri;
    }

    public String getLookupKey() {
        return this.mLookupKey;
    }

    public RawContactDeltaList createRawContactDeltaList() {
        return RawContactDeltaList.fromIterator(getRawContacts().iterator());
    }

    long getId() {
        return this.mId;
    }

    public boolean isError() {
        return this.mStatus == Status.ERROR;
    }

    public boolean isNotFound() {
        return this.mStatus == Status.NOT_FOUND;
    }

    public boolean isLoaded() {
        return this.mStatus == Status.LOADED;
    }

    public long getNameRawContactId() {
        return this.mNameRawContactId;
    }

    public int getDisplayNameSource() {
        return this.mDisplayNameSource;
    }

    public boolean isDisplayNameFromOrganization() {
        return 30 == this.mDisplayNameSource;
    }

    public long getPhotoId() {
        return this.mPhotoId;
    }

    public String getPhotoUri() {
        return this.mPhotoUri;
    }

    public String getDisplayName() {
        return this.mDisplayName;
    }

    public String getAltDisplayName() {
        return this.mAltDisplayName;
    }

    public String getPhoneticName() {
        return this.mPhoneticName;
    }

    public boolean getStarred() {
        return this.mStarred;
    }

    public ImmutableList<AccountType> getInvitableAccountTypes() {
        return this.mInvitableAccountTypes;
    }

    public ImmutableList<RawContact> getRawContacts() {
        return this.mRawContacts;
    }

    public long getDirectoryId() {
        return this.mDirectoryId;
    }

    public boolean isDirectoryEntry() {
        return (this.mDirectoryId == -1 || this.mDirectoryId == 0 || this.mDirectoryId == 1) ? false : true;
    }

    public boolean isWritableContact(Context context) {
        return getFirstWritableRawContactId(context) != -1;
    }

    public long getFirstWritableRawContactId(Context context) {
        if (isDirectoryEntry()) {
            return -1L;
        }
        for (RawContact rawContact : getRawContacts()) {
            AccountType accountType = rawContact.getAccountType(context);
            if (accountType != null && accountType.areContactsWritable()) {
                return rawContact.getId().longValue();
            }
        }
        return -1L;
    }

    public int getDirectoryExportSupport() {
        return this.mDirectoryExportSupport;
    }

    public String getDirectoryAccountType() {
        return this.mDirectoryAccountType;
    }

    public String getDirectoryAccountName() {
        return this.mDirectoryAccountName;
    }

    public byte[] getPhotoBinaryData() {
        return this.mPhotoBinaryData;
    }

    public byte[] getThumbnailPhotoBinaryData() {
        return this.mThumbnailPhotoBinaryData;
    }

    public ArrayList<ContentValues> getContentValues() {
        if (this.mRawContacts.size() != 1) {
            throw new IllegalStateException("Cannot extract content values from an aggregated contact");
        }
        RawContact rawContact = this.mRawContacts.get(0);
        ArrayList<ContentValues> result = rawContact.getContentValues();
        if (this.mPhotoId == 0 && this.mPhotoBinaryData != null) {
            ContentValues photo = new ContentValues();
            photo.put("mimetype", "vnd.android.cursor.item/photo");
            photo.put("data15", this.mPhotoBinaryData);
            result.add(photo);
        }
        return result;
    }

    public ImmutableList<GroupMetaData> getGroupMetaData() {
        return this.mGroups;
    }

    public boolean isSendToVoicemail() {
        return this.mSendToVoicemail;
    }

    public String getCustomRingtone() {
        return this.mCustomRingtone;
    }

    public boolean isUserProfile() {
        return this.mIsUserProfile;
    }

    public String toString() {
        return "{requested=" + this.mRequestedUri + ",lookupkey=" + this.mLookupKey + ",uri=" + this.mUri + ",status=" + this.mStatus + "}";
    }

    void setRawContacts(ImmutableList<RawContact> rawContacts) {
        this.mRawContacts = rawContacts;
    }

    void setStatuses(ImmutableMap<Long, DataStatus> statuses) {
        this.mStatuses = statuses;
    }

    void setInvitableAccountTypes(ImmutableList<AccountType> accountTypes) {
        this.mInvitableAccountTypes = accountTypes;
    }

    void setGroupMetaData(ImmutableList<GroupMetaData> groups) {
        this.mGroups = groups;
    }
}
