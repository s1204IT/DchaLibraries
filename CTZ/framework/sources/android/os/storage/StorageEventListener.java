package android.os.storage;

public class StorageEventListener {
    public void onUsbMassStorageConnectionChanged(boolean z) {
    }

    public void onStorageStateChanged(String str, String str2, String str3) {
    }

    public void onVolumeStateChanged(VolumeInfo volumeInfo, int i, int i2) {
    }

    public void onVolumeRecordChanged(VolumeRecord volumeRecord) {
    }

    public void onVolumeForgotten(String str) {
    }

    public void onDiskScanned(DiskInfo diskInfo, int i) {
    }

    public void onDiskDestroyed(DiskInfo diskInfo) {
    }
}
