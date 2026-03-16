package android.content;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;

public class SyncInfo implements Parcelable {
    public final Account account;
    public final String authority;
    public final int authorityId;
    public final long startTime;
    private static final Account REDACTED_ACCOUNT = new Account("*****", "*****");
    public static final Parcelable.Creator<SyncInfo> CREATOR = new Parcelable.Creator<SyncInfo>() {
        @Override
        public SyncInfo createFromParcel(Parcel in) {
            return new SyncInfo(in);
        }

        @Override
        public SyncInfo[] newArray(int size) {
            return new SyncInfo[size];
        }
    };

    public static SyncInfo createAccountRedacted(int authorityId, String authority, long startTime) {
        return new SyncInfo(authorityId, REDACTED_ACCOUNT, authority, startTime);
    }

    public SyncInfo(int authorityId, Account account, String authority, long startTime) {
        this.authorityId = authorityId;
        this.account = account;
        this.authority = authority;
        this.startTime = startTime;
    }

    public SyncInfo(SyncInfo other) {
        this.authorityId = other.authorityId;
        this.account = new Account(other.account.name, other.account.type);
        this.authority = other.authority;
        this.startTime = other.startTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.authorityId);
        parcel.writeParcelable(this.account, flags);
        parcel.writeString(this.authority);
        parcel.writeLong(this.startTime);
    }

    SyncInfo(Parcel parcel) {
        this.authorityId = parcel.readInt();
        this.account = (Account) parcel.readParcelable(Account.class.getClassLoader());
        this.authority = parcel.readString();
        this.startTime = parcel.readLong();
    }
}
