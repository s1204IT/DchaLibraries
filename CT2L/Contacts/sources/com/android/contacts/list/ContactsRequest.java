package com.android.contacts.list;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class ContactsRequest implements Parcelable {
    public static Parcelable.Creator<ContactsRequest> CREATOR = new Parcelable.Creator<ContactsRequest>() {
        @Override
        public ContactsRequest[] newArray(int size) {
            return new ContactsRequest[size];
        }

        @Override
        public ContactsRequest createFromParcel(Parcel source) {
            ClassLoader classLoader = getClass().getClassLoader();
            ContactsRequest request = new ContactsRequest();
            request.mValid = source.readInt() != 0;
            request.mActionCode = source.readInt();
            request.mRedirectIntent = (Intent) source.readParcelable(classLoader);
            request.mTitle = source.readCharSequence();
            request.mSearchMode = source.readInt() != 0;
            request.mQueryString = source.readString();
            request.mIncludeProfile = source.readInt() != 0;
            request.mLegacyCompatibilityMode = source.readInt() != 0;
            request.mDirectorySearchEnabled = source.readInt() != 0;
            request.mContactUri = (Uri) source.readParcelable(classLoader);
            return request;
        }
    };
    private Uri mContactUri;
    private boolean mIncludeProfile;
    private boolean mLegacyCompatibilityMode;
    private String mQueryString;
    private Intent mRedirectIntent;
    private boolean mSearchMode;
    private CharSequence mTitle;
    private boolean mValid = true;
    private int mActionCode = 10;
    private boolean mDirectorySearchEnabled = true;

    public String toString() {
        return "{ContactsRequest:mValid=" + this.mValid + " mActionCode=" + this.mActionCode + " mRedirectIntent=" + this.mRedirectIntent + " mTitle=" + ((Object) this.mTitle) + " mSearchMode=" + this.mSearchMode + " mQueryString=" + this.mQueryString + " mIncludeProfile=" + this.mIncludeProfile + " mLegacyCompatibilityMode=" + this.mLegacyCompatibilityMode + " mDirectorySearchEnabled=" + this.mDirectorySearchEnabled + " mContactUri=" + this.mContactUri + "}";
    }

    public void copyFrom(ContactsRequest request) {
        this.mValid = request.mValid;
        this.mActionCode = request.mActionCode;
        this.mRedirectIntent = request.mRedirectIntent;
        this.mTitle = request.mTitle;
        this.mSearchMode = request.mSearchMode;
        this.mQueryString = request.mQueryString;
        this.mIncludeProfile = request.mIncludeProfile;
        this.mLegacyCompatibilityMode = request.mLegacyCompatibilityMode;
        this.mDirectorySearchEnabled = request.mDirectorySearchEnabled;
        this.mContactUri = request.mContactUri;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mValid ? 1 : 0);
        dest.writeInt(this.mActionCode);
        dest.writeParcelable(this.mRedirectIntent, 0);
        dest.writeCharSequence(this.mTitle);
        dest.writeInt(this.mSearchMode ? 1 : 0);
        dest.writeString(this.mQueryString);
        dest.writeInt(this.mIncludeProfile ? 1 : 0);
        dest.writeInt(this.mLegacyCompatibilityMode ? 1 : 0);
        dest.writeInt(this.mDirectorySearchEnabled ? 1 : 0);
        dest.writeParcelable(this.mContactUri, 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean isValid() {
        return this.mValid;
    }

    public Intent getRedirectIntent() {
        return this.mRedirectIntent;
    }

    public void setActivityTitle(CharSequence title) {
        this.mTitle = title;
    }

    public CharSequence getActivityTitle() {
        return this.mTitle;
    }

    public int getActionCode() {
        return this.mActionCode;
    }

    public void setActionCode(int actionCode) {
        this.mActionCode = actionCode;
    }

    public boolean isSearchMode() {
        return this.mSearchMode;
    }

    public void setSearchMode(boolean flag) {
        this.mSearchMode = flag;
    }

    public String getQueryString() {
        return this.mQueryString;
    }

    public void setQueryString(String string) {
        this.mQueryString = string;
    }

    public boolean shouldIncludeProfile() {
        return this.mIncludeProfile;
    }

    public boolean isLegacyCompatibilityMode() {
        return this.mLegacyCompatibilityMode;
    }

    public void setLegacyCompatibilityMode(boolean flag) {
        this.mLegacyCompatibilityMode = flag;
    }

    public boolean isDirectorySearchEnabled() {
        return this.mDirectorySearchEnabled;
    }

    public Uri getContactUri() {
        return this.mContactUri;
    }

    public void setContactUri(Uri contactUri) {
        this.mContactUri = contactUri;
    }
}
