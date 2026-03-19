package android.bluetooth;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothGattService implements Parcelable {
    public static final Parcelable.Creator<BluetoothGattService> CREATOR = new Parcelable.Creator<BluetoothGattService>() {
        @Override
        public BluetoothGattService createFromParcel(Parcel parcel) {
            return new BluetoothGattService(parcel);
        }

        @Override
        public BluetoothGattService[] newArray(int i) {
            return new BluetoothGattService[i];
        }
    };
    public static final int SERVICE_TYPE_PRIMARY = 0;
    public static final int SERVICE_TYPE_SECONDARY = 1;
    private boolean mAdvertisePreferred;
    protected List<BluetoothGattCharacteristic> mCharacteristics;
    protected BluetoothDevice mDevice;
    protected int mHandles;
    protected List<BluetoothGattService> mIncludedServices;
    protected int mInstanceId;
    protected int mServiceType;
    protected UUID mUuid;

    public BluetoothGattService(UUID uuid, int i) {
        this.mHandles = 0;
        this.mDevice = null;
        this.mUuid = uuid;
        this.mInstanceId = 0;
        this.mServiceType = i;
        this.mCharacteristics = new ArrayList();
        this.mIncludedServices = new ArrayList();
    }

    BluetoothGattService(BluetoothDevice bluetoothDevice, UUID uuid, int i, int i2) {
        this.mHandles = 0;
        this.mDevice = bluetoothDevice;
        this.mUuid = uuid;
        this.mInstanceId = i;
        this.mServiceType = i2;
        this.mCharacteristics = new ArrayList();
        this.mIncludedServices = new ArrayList();
    }

    public BluetoothGattService(UUID uuid, int i, int i2) {
        this.mHandles = 0;
        this.mDevice = null;
        this.mUuid = uuid;
        this.mInstanceId = i;
        this.mServiceType = i2;
        this.mCharacteristics = new ArrayList();
        this.mIncludedServices = new ArrayList();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(new ParcelUuid(this.mUuid), 0);
        parcel.writeInt(this.mInstanceId);
        parcel.writeInt(this.mServiceType);
        parcel.writeTypedList(this.mCharacteristics);
        ArrayList arrayList = new ArrayList(this.mIncludedServices.size());
        for (BluetoothGattService bluetoothGattService : this.mIncludedServices) {
            arrayList.add(new BluetoothGattIncludedService(bluetoothGattService.getUuid(), bluetoothGattService.getInstanceId(), bluetoothGattService.getType()));
        }
        parcel.writeTypedList(arrayList);
    }

    private BluetoothGattService(Parcel parcel) {
        this.mHandles = 0;
        this.mUuid = ((ParcelUuid) parcel.readParcelable(null)).getUuid();
        this.mInstanceId = parcel.readInt();
        this.mServiceType = parcel.readInt();
        this.mCharacteristics = new ArrayList();
        ArrayList<BluetoothGattCharacteristic> arrayListCreateTypedArrayList = parcel.createTypedArrayList(BluetoothGattCharacteristic.CREATOR);
        if (arrayListCreateTypedArrayList != null) {
            for (BluetoothGattCharacteristic bluetoothGattCharacteristic : arrayListCreateTypedArrayList) {
                bluetoothGattCharacteristic.setService(this);
                this.mCharacteristics.add(bluetoothGattCharacteristic);
            }
        }
        this.mIncludedServices = new ArrayList();
        ArrayList<BluetoothGattIncludedService> arrayListCreateTypedArrayList2 = parcel.createTypedArrayList(BluetoothGattIncludedService.CREATOR);
        if (arrayListCreateTypedArrayList != null) {
            for (BluetoothGattIncludedService bluetoothGattIncludedService : arrayListCreateTypedArrayList2) {
                this.mIncludedServices.add(new BluetoothGattService(null, bluetoothGattIncludedService.getUuid(), bluetoothGattIncludedService.getInstanceId(), bluetoothGattIncludedService.getType()));
            }
        }
    }

    BluetoothDevice getDevice() {
        return this.mDevice;
    }

    void setDevice(BluetoothDevice bluetoothDevice) {
        this.mDevice = bluetoothDevice;
    }

    public boolean addService(BluetoothGattService bluetoothGattService) {
        this.mIncludedServices.add(bluetoothGattService);
        return true;
    }

    public boolean addCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        this.mCharacteristics.add(bluetoothGattCharacteristic);
        bluetoothGattCharacteristic.setService(this);
        return true;
    }

    BluetoothGattCharacteristic getCharacteristic(UUID uuid, int i) {
        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : this.mCharacteristics) {
            if (uuid.equals(bluetoothGattCharacteristic.getUuid()) && bluetoothGattCharacteristic.getInstanceId() == i) {
                return bluetoothGattCharacteristic;
            }
        }
        return null;
    }

    public void setInstanceId(int i) {
        this.mInstanceId = i;
    }

    int getHandles() {
        return this.mHandles;
    }

    public void setHandles(int i) {
        this.mHandles = i;
    }

    public void addIncludedService(BluetoothGattService bluetoothGattService) {
        this.mIncludedServices.add(bluetoothGattService);
    }

    public UUID getUuid() {
        return this.mUuid;
    }

    public int getInstanceId() {
        return this.mInstanceId;
    }

    public int getType() {
        return this.mServiceType;
    }

    public List<BluetoothGattService> getIncludedServices() {
        return this.mIncludedServices;
    }

    public List<BluetoothGattCharacteristic> getCharacteristics() {
        return this.mCharacteristics;
    }

    public BluetoothGattCharacteristic getCharacteristic(UUID uuid) {
        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : this.mCharacteristics) {
            if (uuid.equals(bluetoothGattCharacteristic.getUuid())) {
                return bluetoothGattCharacteristic;
            }
        }
        return null;
    }

    public boolean isAdvertisePreferred() {
        return this.mAdvertisePreferred;
    }

    public void setAdvertisePreferred(boolean z) {
        this.mAdvertisePreferred = z;
    }
}
