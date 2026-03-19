package android.bluetooth.le;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AdvertiseData implements Parcelable {
    public static final Parcelable.Creator<AdvertiseData> CREATOR = new Parcelable.Creator<AdvertiseData>() {
        @Override
        public AdvertiseData[] newArray(int i) {
            return new AdvertiseData[i];
        }

        @Override
        public AdvertiseData createFromParcel(Parcel parcel) {
            Builder builder = new Builder();
            Iterator it = parcel.createTypedArrayList(ParcelUuid.CREATOR).iterator();
            while (it.hasNext()) {
                builder.addServiceUuid((ParcelUuid) it.next());
            }
            int i = parcel.readInt();
            for (int i2 = 0; i2 < i; i2++) {
                builder.addManufacturerData(parcel.readInt(), parcel.createByteArray());
            }
            int i3 = parcel.readInt();
            for (int i4 = 0; i4 < i3; i4++) {
                builder.addServiceData((ParcelUuid) parcel.readTypedObject(ParcelUuid.CREATOR), parcel.createByteArray());
            }
            builder.setIncludeTxPowerLevel(parcel.readByte() == 1);
            builder.setIncludeDeviceName(parcel.readByte() == 1);
            return builder.build();
        }
    };
    private final boolean mIncludeDeviceName;
    private final boolean mIncludeTxPowerLevel;
    private final SparseArray<byte[]> mManufacturerSpecificData;
    private final Map<ParcelUuid, byte[]> mServiceData;
    private final List<ParcelUuid> mServiceUuids;

    private AdvertiseData(List<ParcelUuid> list, SparseArray<byte[]> sparseArray, Map<ParcelUuid, byte[]> map, boolean z, boolean z2) {
        this.mServiceUuids = list;
        this.mManufacturerSpecificData = sparseArray;
        this.mServiceData = map;
        this.mIncludeTxPowerLevel = z;
        this.mIncludeDeviceName = z2;
    }

    public List<ParcelUuid> getServiceUuids() {
        return this.mServiceUuids;
    }

    public SparseArray<byte[]> getManufacturerSpecificData() {
        return this.mManufacturerSpecificData;
    }

    public Map<ParcelUuid, byte[]> getServiceData() {
        return this.mServiceData;
    }

    public boolean getIncludeTxPowerLevel() {
        return this.mIncludeTxPowerLevel;
    }

    public boolean getIncludeDeviceName() {
        return this.mIncludeDeviceName;
    }

    public int hashCode() {
        return Objects.hash(this.mServiceUuids, this.mManufacturerSpecificData, this.mServiceData, Boolean.valueOf(this.mIncludeDeviceName), Boolean.valueOf(this.mIncludeTxPowerLevel));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AdvertiseData advertiseData = (AdvertiseData) obj;
        if (Objects.equals(this.mServiceUuids, advertiseData.mServiceUuids) && BluetoothLeUtils.equals(this.mManufacturerSpecificData, advertiseData.mManufacturerSpecificData) && BluetoothLeUtils.equals(this.mServiceData, advertiseData.mServiceData) && this.mIncludeDeviceName == advertiseData.mIncludeDeviceName && this.mIncludeTxPowerLevel == advertiseData.mIncludeTxPowerLevel) {
            return true;
        }
        return false;
    }

    public String toString() {
        return "AdvertiseData [mServiceUuids=" + this.mServiceUuids + ", mManufacturerSpecificData=" + BluetoothLeUtils.toString(this.mManufacturerSpecificData) + ", mServiceData=" + BluetoothLeUtils.toString(this.mServiceData) + ", mIncludeTxPowerLevel=" + this.mIncludeTxPowerLevel + ", mIncludeDeviceName=" + this.mIncludeDeviceName + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedArray((ParcelUuid[]) this.mServiceUuids.toArray(new ParcelUuid[this.mServiceUuids.size()]), i);
        parcel.writeInt(this.mManufacturerSpecificData.size());
        for (int i2 = 0; i2 < this.mManufacturerSpecificData.size(); i2++) {
            parcel.writeInt(this.mManufacturerSpecificData.keyAt(i2));
            parcel.writeByteArray(this.mManufacturerSpecificData.valueAt(i2));
        }
        parcel.writeInt(this.mServiceData.size());
        for (ParcelUuid parcelUuid : this.mServiceData.keySet()) {
            parcel.writeTypedObject(parcelUuid, i);
            parcel.writeByteArray(this.mServiceData.get(parcelUuid));
        }
        parcel.writeByte(getIncludeTxPowerLevel() ? (byte) 1 : (byte) 0);
        parcel.writeByte(getIncludeDeviceName() ? (byte) 1 : (byte) 0);
    }

    public static final class Builder {
        private boolean mIncludeDeviceName;
        private boolean mIncludeTxPowerLevel;
        private List<ParcelUuid> mServiceUuids = new ArrayList();
        private SparseArray<byte[]> mManufacturerSpecificData = new SparseArray<>();
        private Map<ParcelUuid, byte[]> mServiceData = new ArrayMap();

        public Builder addServiceUuid(ParcelUuid parcelUuid) {
            if (parcelUuid == null) {
                throw new IllegalArgumentException("serivceUuids are null");
            }
            this.mServiceUuids.add(parcelUuid);
            return this;
        }

        public Builder addServiceData(ParcelUuid parcelUuid, byte[] bArr) {
            if (parcelUuid == null || bArr == null) {
                throw new IllegalArgumentException("serviceDataUuid or serviceDataUuid is null");
            }
            this.mServiceData.put(parcelUuid, bArr);
            return this;
        }

        public Builder addManufacturerData(int i, byte[] bArr) {
            if (i < 0) {
                throw new IllegalArgumentException("invalid manufacturerId - " + i);
            }
            if (bArr == null) {
                throw new IllegalArgumentException("manufacturerSpecificData is null");
            }
            this.mManufacturerSpecificData.put(i, bArr);
            return this;
        }

        public Builder setIncludeTxPowerLevel(boolean z) {
            this.mIncludeTxPowerLevel = z;
            return this;
        }

        public Builder setIncludeDeviceName(boolean z) {
            this.mIncludeDeviceName = z;
            return this;
        }

        public AdvertiseData build() {
            return new AdvertiseData(this.mServiceUuids, this.mManufacturerSpecificData, this.mServiceData, this.mIncludeTxPowerLevel, this.mIncludeDeviceName);
        }
    }
}
