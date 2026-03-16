package com.android.ex.chips;

import android.net.Uri;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;

public class RecipientEntry {
    private final long mContactId;
    private final long mDataId;
    private final String mDestination;
    private final String mDestinationLabel;
    private final int mDestinationType;
    private final Long mDirectoryId;
    private final String mDisplayName;
    private final int mEntryType;
    private boolean mIsFirstLevel;
    private boolean mIsValid;
    private final String mLookupKey;
    private final Uri mPhotoThumbnailUri;
    private byte[] mPhotoBytes = null;
    private final boolean mIsDivider = false;

    protected RecipientEntry(int entryType, String displayName, String destination, int destinationType, String destinationLabel, long contactId, Long directoryId, long dataId, Uri photoThumbnailUri, boolean isFirstLevel, boolean isValid, String lookupKey) {
        this.mEntryType = entryType;
        this.mIsFirstLevel = isFirstLevel;
        this.mDisplayName = displayName;
        this.mDestination = destination;
        this.mDestinationType = destinationType;
        this.mDestinationLabel = destinationLabel;
        this.mContactId = contactId;
        this.mDirectoryId = directoryId;
        this.mDataId = dataId;
        this.mPhotoThumbnailUri = photoThumbnailUri;
        this.mIsValid = isValid;
        this.mLookupKey = lookupKey;
    }

    public boolean isValid() {
        return this.mIsValid;
    }

    public static boolean isCreatedRecipient(long id) {
        return id == -1 || id == -2;
    }

    public static RecipientEntry constructFakeEntry(String address, boolean isValid) {
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(address);
        String tokenizedAddress = tokens.length > 0 ? tokens[0].getAddress() : address;
        return new RecipientEntry(0, tokenizedAddress, tokenizedAddress, -1, null, -1L, null, -1L, null, true, isValid, null);
    }

    public static RecipientEntry constructFakePhoneEntry(String phoneNumber, boolean isValid) {
        return new RecipientEntry(0, phoneNumber, phoneNumber, -1, null, -1L, null, -1L, null, true, isValid, null);
    }

    private static String pickDisplayName(int displayNameSource, String displayName, String destination) {
        return displayNameSource > 20 ? displayName : destination;
    }

    public static RecipientEntry constructGeneratedEntry(String display, String address, boolean isValid) {
        return new RecipientEntry(0, display, address, -1, null, -2L, null, -2L, null, true, isValid, null);
    }

    public static RecipientEntry constructTopLevelEntry(String displayName, int displayNameSource, String destination, int destinationType, String destinationLabel, long contactId, Long directoryId, long dataId, String thumbnailUriAsString, boolean isValid, String lookupKey) {
        return new RecipientEntry(0, pickDisplayName(displayNameSource, displayName, destination), destination, destinationType, destinationLabel, contactId, directoryId, dataId, thumbnailUriAsString != null ? Uri.parse(thumbnailUriAsString) : null, true, isValid, lookupKey);
    }

    public static RecipientEntry constructSecondLevelEntry(String displayName, int displayNameSource, String destination, int destinationType, String destinationLabel, long contactId, Long directoryId, long dataId, String thumbnailUriAsString, boolean isValid, String lookupKey) {
        return new RecipientEntry(0, pickDisplayName(displayNameSource, displayName, destination), destination, destinationType, destinationLabel, contactId, directoryId, dataId, thumbnailUriAsString != null ? Uri.parse(thumbnailUriAsString) : null, false, isValid, lookupKey);
    }

    public int getEntryType() {
        return this.mEntryType;
    }

    public String getDisplayName() {
        return this.mDisplayName;
    }

    public String getDestination() {
        return this.mDestination;
    }

    public int getDestinationType() {
        return this.mDestinationType;
    }

    public String getDestinationLabel() {
        return this.mDestinationLabel;
    }

    public long getContactId() {
        return this.mContactId;
    }

    public Long getDirectoryId() {
        return this.mDirectoryId;
    }

    public long getDataId() {
        return this.mDataId;
    }

    public boolean isFirstLevel() {
        return this.mIsFirstLevel;
    }

    public Uri getPhotoThumbnailUri() {
        return this.mPhotoThumbnailUri;
    }

    public synchronized void setPhotoBytes(byte[] photoBytes) {
        this.mPhotoBytes = photoBytes;
    }

    public synchronized byte[] getPhotoBytes() {
        return this.mPhotoBytes;
    }

    public boolean isSelectable() {
        return this.mEntryType == 0;
    }

    public String getLookupKey() {
        return this.mLookupKey;
    }

    public String toString() {
        return this.mDisplayName + " <" + this.mDestination + ">, isValid=" + this.mIsValid;
    }
}
