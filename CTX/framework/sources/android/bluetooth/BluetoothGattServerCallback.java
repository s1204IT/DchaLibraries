package android.bluetooth;

public abstract class BluetoothGattServerCallback {
    public void onConnectionStateChange(BluetoothDevice bluetoothDevice, int i, int i2) {
    }

    public void onServiceAdded(int i, BluetoothGattService bluetoothGattService) {
    }

    public void onCharacteristicReadRequest(BluetoothDevice bluetoothDevice, int i, int i2, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
    }

    public void onCharacteristicWriteRequest(BluetoothDevice bluetoothDevice, int i, BluetoothGattCharacteristic bluetoothGattCharacteristic, boolean z, boolean z2, int i2, byte[] bArr) {
    }

    public void onDescriptorReadRequest(BluetoothDevice bluetoothDevice, int i, int i2, BluetoothGattDescriptor bluetoothGattDescriptor) {
    }

    public void onDescriptorWriteRequest(BluetoothDevice bluetoothDevice, int i, BluetoothGattDescriptor bluetoothGattDescriptor, boolean z, boolean z2, int i2, byte[] bArr) {
    }

    public void onExecuteWrite(BluetoothDevice bluetoothDevice, int i, boolean z) {
    }

    public void onNotificationSent(BluetoothDevice bluetoothDevice, int i) {
    }

    public void onMtuChanged(BluetoothDevice bluetoothDevice, int i) {
    }

    public void onPhyUpdate(BluetoothDevice bluetoothDevice, int i, int i2, int i3) {
    }

    public void onPhyRead(BluetoothDevice bluetoothDevice, int i, int i2, int i3) {
    }

    public void onConnectionUpdated(BluetoothDevice bluetoothDevice, int i, int i2, int i3, int i4) {
    }
}
