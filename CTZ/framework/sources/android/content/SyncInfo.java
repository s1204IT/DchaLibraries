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
        public SyncInfo createFromParcel(Parcel parcel) {
            return new SyncInfo(parcel);
        }

        @Override
        public SyncInfo[] newArray(int i) {
            return new SyncInfo[i];
        }
    };

    public static SyncInfo createAccountRedacted(int i, String str, long j) {
        return new SyncInfo(i, REDACTED_ACCOUNT, str, j);
    }

    public SyncInfo(int i, Account account, String str, long j) {
        this.authorityId = i;
        this.account = account;
        this.authority = str;
        this.startTime = j;
    }

    public SyncInfo(SyncInfo syncInfo) {
        this.authorityId = syncInfo.authorityId;
        this.account = new Account(syncInfo.account.name, syncInfo.account.type);
        this.authority = syncInfo.authority;
        this.startTime = syncInfo.startTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.authorityId);
        parcel.writeParcelable(this.account, i);
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
