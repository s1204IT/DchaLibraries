package com.android.mtkex.chips;

import android.graphics.Bitmap;
import android.net.Uri;

public class RecipientEntry {
    private Bitmap mBitmap;
    private final long mContactId;
    private final long mDataId;
    private final String mDestination;
    private int mDestinationKind;
    private final String mDestinationLabel;
    private final int mDestinationType;
    private final String mDisplayName;
    private final int mEntryType;
    private boolean mIsFirstLevel;
    private final boolean mIsGalContact;
    private boolean mIsValid;
    private final Uri mPhotoThumbnailUri;
    private byte[] mPhotoBytes = null;
    private final boolean mIsDivider = false;

    private RecipientEntry(int i, String str, String str2, int i2, String str3, long j, long j2, Uri uri, boolean z, boolean z2, boolean z3) {
        this.mEntryType = i;
        this.mIsFirstLevel = z;
        this.mDisplayName = str;
        this.mDestination = str2;
        this.mDestinationType = i2;
        this.mDestinationLabel = str3;
        this.mContactId = j;
        this.mDataId = j2;
        this.mPhotoThumbnailUri = uri;
        this.mIsValid = z2;
        this.mIsGalContact = z3;
    }

    public boolean isValid() {
        return this.mIsValid;
    }

    public static boolean isCreatedRecipient(long j) {
        return j == -1 || j == -2;
    }

    public static RecipientEntry constructFakeEntry(String str, boolean z) {
        return new RecipientEntry(0, str, str, -1, null, -1L, -1L, null, true, z, false);
    }

    public static RecipientEntry constructFakeEntry(String str) {
        return constructFakeEntry(str, true);
    }

    public static RecipientEntry constructFakePhoneEntry(String str, boolean z) {
        return new RecipientEntry(0, str, str, -1, null, -1L, -1L, null, true, z, false);
    }

    private static String pickDisplayName(int i, String str, String str2) {
        return i > 20 ? str : str2;
    }

    public static RecipientEntry constructGeneratedEntry(String str, String str2, boolean z) {
        return new RecipientEntry(0, str, str2, -1, null, -2L, -2L, null, true, z, false);
    }

    public static RecipientEntry constructTopLevelEntry(String str, int i, String str2, int i2, String str3, long j, long j2, String str4, boolean z, boolean z2) {
        return new RecipientEntry(0, pickDisplayName(i, str, str2), str2, i2, str3, j, j2, str4 != null ? Uri.parse(str4) : null, true, z, z2);
    }

    public static RecipientEntry constructSecondLevelEntry(String str, int i, String str2, int i2, String str3, long j, long j2, String str4, boolean z, boolean z2) {
        return new RecipientEntry(0, pickDisplayName(i, str, str2), str2, i2, str3, j, j2, str4 != null ? Uri.parse(str4) : null, false, z, z2);
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

    public long getDataId() {
        return this.mDataId;
    }

    public boolean isFirstLevel() {
        return this.mIsFirstLevel;
    }

    public Uri getPhotoThumbnailUri() {
        return this.mPhotoThumbnailUri;
    }

    public synchronized void setPhotoBytes(byte[] bArr) {
        this.mPhotoBytes = bArr;
    }

    public synchronized byte[] getPhotoBytes() {
        return this.mPhotoBytes;
    }

    public boolean isSelectable() {
        return this.mEntryType == 0;
    }

    public String toString() {
        return this.mDisplayName + " <" + this.mDestination + ">, isValid=" + this.mIsValid;
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }

    public void setDestinationKind(int i) {
        this.mDestinationKind = i;
    }

    public int getDestinationKind() {
        return this.mDestinationKind;
    }
}
