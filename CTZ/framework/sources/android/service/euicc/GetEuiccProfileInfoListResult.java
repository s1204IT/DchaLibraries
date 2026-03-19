package android.service.euicc;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.List;

@SystemApi
public final class GetEuiccProfileInfoListResult implements Parcelable {
    public static final Parcelable.Creator<GetEuiccProfileInfoListResult> CREATOR = new Parcelable.Creator<GetEuiccProfileInfoListResult>() {
        @Override
        public GetEuiccProfileInfoListResult createFromParcel(Parcel parcel) {
            return new GetEuiccProfileInfoListResult(parcel);
        }

        @Override
        public GetEuiccProfileInfoListResult[] newArray(int i) {
            return new GetEuiccProfileInfoListResult[i];
        }
    };
    private final boolean mIsRemovable;
    private final EuiccProfileInfo[] mProfiles;

    @Deprecated
    public final int result;

    public int getResult() {
        return this.result;
    }

    public List<EuiccProfileInfo> getProfiles() {
        if (this.mProfiles == null) {
            return null;
        }
        return Arrays.asList(this.mProfiles);
    }

    public boolean getIsRemovable() {
        return this.mIsRemovable;
    }

    public GetEuiccProfileInfoListResult(int i, EuiccProfileInfo[] euiccProfileInfoArr, boolean z) {
        this.result = i;
        this.mIsRemovable = z;
        if (this.result == 0) {
            this.mProfiles = euiccProfileInfoArr;
        } else {
            if (euiccProfileInfoArr != null) {
                throw new IllegalArgumentException("Error result with non-null profiles: " + i);
            }
            this.mProfiles = null;
        }
    }

    private GetEuiccProfileInfoListResult(Parcel parcel) {
        this.result = parcel.readInt();
        this.mProfiles = (EuiccProfileInfo[]) parcel.createTypedArray(EuiccProfileInfo.CREATOR);
        this.mIsRemovable = parcel.readBoolean();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.result);
        parcel.writeTypedArray(this.mProfiles, i);
        parcel.writeBoolean(this.mIsRemovable);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
