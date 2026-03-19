package android.companion;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.OneTimeUseBuilder;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AssociationRequest implements Parcelable {
    public static final Parcelable.Creator<AssociationRequest> CREATOR = new Parcelable.Creator<AssociationRequest>() {
        @Override
        public AssociationRequest createFromParcel(Parcel parcel) {
            return new AssociationRequest(parcel);
        }

        @Override
        public AssociationRequest[] newArray(int i) {
            return new AssociationRequest[i];
        }
    };
    private final List<DeviceFilter<?>> mDeviceFilters;
    private final boolean mSingleDevice;

    private AssociationRequest(boolean z, List<DeviceFilter<?>> list) {
        this.mSingleDevice = z;
        this.mDeviceFilters = CollectionUtils.emptyIfNull(list);
    }

    private AssociationRequest(Parcel parcel) {
        this(parcel.readByte() != 0, (List<DeviceFilter<?>>) parcel.readParcelableList(new ArrayList(), AssociationRequest.class.getClassLoader()));
    }

    public boolean isSingleDevice() {
        return this.mSingleDevice;
    }

    public List<DeviceFilter<?>> getDeviceFilters() {
        return this.mDeviceFilters;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AssociationRequest associationRequest = (AssociationRequest) obj;
        if (this.mSingleDevice == associationRequest.mSingleDevice && Objects.equals(this.mDeviceFilters, associationRequest.mDeviceFilters)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.mSingleDevice), this.mDeviceFilters);
    }

    public String toString() {
        return "AssociationRequest{mSingleDevice=" + this.mSingleDevice + ", mDeviceFilters=" + this.mDeviceFilters + '}';
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte(this.mSingleDevice ? (byte) 1 : (byte) 0);
        parcel.writeParcelableList(this.mDeviceFilters, i);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final class Builder extends OneTimeUseBuilder<AssociationRequest> {
        private boolean mSingleDevice = false;
        private ArrayList<DeviceFilter<?>> mDeviceFilters = null;

        public Builder setSingleDevice(boolean z) {
            checkNotUsed();
            this.mSingleDevice = z;
            return this;
        }

        public Builder addDeviceFilter(DeviceFilter<?> deviceFilter) {
            checkNotUsed();
            if (deviceFilter != null) {
                this.mDeviceFilters = ArrayUtils.add(this.mDeviceFilters, deviceFilter);
            }
            return this;
        }

        @Override
        public AssociationRequest build() {
            markUsed();
            return new AssociationRequest(this.mSingleDevice, this.mDeviceFilters);
        }
    }
}
