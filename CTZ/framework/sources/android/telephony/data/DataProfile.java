package android.telephony.data;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public final class DataProfile implements Parcelable {
    public static final Parcelable.Creator<DataProfile> CREATOR = new Parcelable.Creator<DataProfile>() {
        @Override
        public DataProfile createFromParcel(Parcel parcel) {
            return new DataProfile(parcel);
        }

        @Override
        public DataProfile[] newArray(int i) {
            return new DataProfile[i];
        }
    };
    public static final int TYPE_3GPP = 1;
    public static final int TYPE_3GPP2 = 2;
    public static final int TYPE_COMMON = 0;
    private final String mApn;
    private final int mAuthType;
    private final int mBearerBitmap;
    private final boolean mEnabled;
    private final int mMaxConns;
    private final int mMaxConnsTime;
    private final boolean mModemCognitive;
    private final int mMtu;
    private final String mMvnoMatchData;
    private final String mMvnoType;
    private final String mPassword;
    private final int mProfileId;
    private final String mProtocol;
    private final String mRoamingProtocol;
    private final int mSupportedApnTypesBitmap;
    private final int mType;
    private final String mUserName;
    private final int mWaitTime;

    public DataProfile(int i, String str, String str2, int i2, String str3, String str4, int i3, int i4, int i5, int i6, boolean z, int i7, String str5, int i8, int i9, String str6, String str7, boolean z2) {
        int i10;
        this.mProfileId = i;
        this.mApn = str;
        this.mProtocol = str2;
        if (i2 == -1) {
            i10 = TextUtils.isEmpty(str3) ? 0 : 3;
        } else {
            i10 = i2;
        }
        this.mAuthType = i10;
        this.mUserName = str3;
        this.mPassword = str4;
        this.mType = i3;
        this.mMaxConnsTime = i4;
        this.mMaxConns = i5;
        this.mWaitTime = i6;
        this.mEnabled = z;
        this.mSupportedApnTypesBitmap = i7;
        this.mRoamingProtocol = str5;
        this.mBearerBitmap = i8;
        this.mMtu = i9;
        this.mMvnoType = str6;
        this.mMvnoMatchData = str7;
        this.mModemCognitive = z2;
    }

    public DataProfile(Parcel parcel) {
        this.mProfileId = parcel.readInt();
        this.mApn = parcel.readString();
        this.mProtocol = parcel.readString();
        this.mAuthType = parcel.readInt();
        this.mUserName = parcel.readString();
        this.mPassword = parcel.readString();
        this.mType = parcel.readInt();
        this.mMaxConnsTime = parcel.readInt();
        this.mMaxConns = parcel.readInt();
        this.mWaitTime = parcel.readInt();
        this.mEnabled = parcel.readBoolean();
        this.mSupportedApnTypesBitmap = parcel.readInt();
        this.mRoamingProtocol = parcel.readString();
        this.mBearerBitmap = parcel.readInt();
        this.mMtu = parcel.readInt();
        this.mMvnoType = parcel.readString();
        this.mMvnoMatchData = parcel.readString();
        this.mModemCognitive = parcel.readBoolean();
    }

    public int getProfileId() {
        return this.mProfileId;
    }

    public String getApn() {
        return this.mApn;
    }

    public String getProtocol() {
        return this.mProtocol;
    }

    public int getAuthType() {
        return this.mAuthType;
    }

    public String getUserName() {
        return this.mUserName;
    }

    public String getPassword() {
        return this.mPassword;
    }

    public int getType() {
        return this.mType;
    }

    public int getMaxConnsTime() {
        return this.mMaxConnsTime;
    }

    public int getMaxConns() {
        return this.mMaxConns;
    }

    public int getWaitTime() {
        return this.mWaitTime;
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public int getSupportedApnTypesBitmap() {
        return this.mSupportedApnTypesBitmap;
    }

    public String getRoamingProtocol() {
        return this.mRoamingProtocol;
    }

    public int getBearerBitmap() {
        return this.mBearerBitmap;
    }

    public int getMtu() {
        return this.mMtu;
    }

    public String getMvnoType() {
        return this.mMvnoType;
    }

    public String getMvnoMatchData() {
        return this.mMvnoMatchData;
    }

    public boolean isModemCognitive() {
        return this.mModemCognitive;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append("DataProfile=");
        sb.append(this.mProfileId);
        sb.append("/");
        sb.append(this.mProtocol);
        sb.append("/");
        sb.append(this.mAuthType);
        sb.append("/");
        if (Build.IS_USER) {
            str = "***/***/***";
        } else {
            str = this.mApn + "/" + this.mUserName + "/" + this.mPassword;
        }
        sb.append(str);
        sb.append("/");
        sb.append(this.mType);
        sb.append("/");
        sb.append(this.mMaxConnsTime);
        sb.append("/");
        sb.append(this.mMaxConns);
        sb.append("/");
        sb.append(this.mWaitTime);
        sb.append("/");
        sb.append(this.mEnabled);
        sb.append("/");
        sb.append(this.mSupportedApnTypesBitmap);
        sb.append("/");
        sb.append(this.mRoamingProtocol);
        sb.append("/");
        sb.append(this.mBearerBitmap);
        sb.append("/");
        sb.append(this.mMtu);
        sb.append("/");
        sb.append(this.mMvnoType);
        sb.append("/");
        sb.append(this.mMvnoMatchData);
        sb.append("/");
        sb.append(this.mModemCognitive);
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (obj instanceof DataProfile) {
            return obj == this || toString().equals(obj.toString());
        }
        return false;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mProfileId);
        parcel.writeString(this.mApn);
        parcel.writeString(this.mProtocol);
        parcel.writeInt(this.mAuthType);
        parcel.writeString(this.mUserName);
        parcel.writeString(this.mPassword);
        parcel.writeInt(this.mType);
        parcel.writeInt(this.mMaxConnsTime);
        parcel.writeInt(this.mMaxConns);
        parcel.writeInt(this.mWaitTime);
        parcel.writeBoolean(this.mEnabled);
        parcel.writeInt(this.mSupportedApnTypesBitmap);
        parcel.writeString(this.mRoamingProtocol);
        parcel.writeInt(this.mBearerBitmap);
        parcel.writeInt(this.mMtu);
        parcel.writeString(this.mMvnoType);
        parcel.writeString(this.mMvnoMatchData);
        parcel.writeBoolean(this.mModemCognitive);
    }
}
