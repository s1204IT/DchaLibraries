package com.mediatek.contacts.model.account;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.contacts.model.account.AccountWithDataSet;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.Log;

public class AccountWithDataSetEx extends AccountWithDataSet {
    public static final Parcelable.Creator<AccountWithDataSetEx> CREATOR = new Parcelable.Creator<AccountWithDataSetEx>() {
        @Override
        public AccountWithDataSetEx createFromParcel(Parcel parcel) {
            return new AccountWithDataSetEx(parcel);
        }

        @Override
        public AccountWithDataSetEx[] newArray(int i) {
            return new AccountWithDataSetEx[i];
        }
    };
    public int mSubId;

    public AccountWithDataSetEx(String str, String str2, int i) {
        this(str, str2, (String) null);
        this.mSubId = i;
        Log.i("AccountWithDataSetEx", "[AccountWithDataSetEx]name:" + Log.anonymize(str) + ",type:" + str2 + ",subId:" + this.mSubId);
    }

    public AccountWithDataSetEx(String str, String str2, String str3) {
        super(str, str2, str3);
        this.mSubId = SubInfoUtils.getInvalidSubId();
        Log.i("AccountWithDataSetEx", "[AccountWithDataSetEx]name:" + Log.anonymize(str) + ",type:" + str2 + ",subId:" + this.mSubId + ",dataSet = " + Log.anonymize(str3));
    }

    public AccountWithDataSetEx(String str, String str2, String str3, int i) {
        super(str, str2, str3);
        this.mSubId = i;
        Log.i("AccountWithDataSetEx", "[AccountWithDataSetEx]name:" + Log.anonymize(str) + ",type:" + str2 + ",dataSet = " + Log.anonymize(str3) + ",subId:" + this.mSubId);
    }

    public AccountWithDataSetEx(Parcel parcel) {
        super(parcel);
        this.mSubId = parcel.readInt();
        Log.i("AccountWithDataSetEx", "[AccountWithDataSetEx]...");
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(this.mSubId);
    }

    @Override
    public int hashCode() {
        return (31 * super.hashCode()) + (this.mSubId == 0 ? 0 : this.mSubId);
    }

    @Override
    public String toString() {
        return "AccountWithDataSetEx {name=" + Log.anonymize(this.name) + ", type=" + this.type + ", dataSet=" + Log.anonymize(this.dataSet) + ", subId = " + this.mSubId + "}";
    }

    public int getSubId() {
        return this.mSubId;
    }

    public String getDisplayName() {
        String displaynameUsingSubId = SubInfoUtils.getDisplaynameUsingSubId(this.mSubId);
        Log.i("AccountWithDataSetEx", "[getDisplayName]displayName:" + displaynameUsingSubId + ", subId:" + this.mSubId);
        return displaynameUsingSubId;
    }

    public static boolean isLocalPhone(String str) {
        return "Local Phone Account".equals(str);
    }
}
