package android.bluetooth;

public abstract class BluetoothGattCallback {
    public void onPhyUpdate(BluetoothGatt bluetoothGatt, int i, int i2, int i3) {
    }

    public void onPhyRead(BluetoothGatt bluetoothGatt, int i, int i2, int i3) {
    }

    public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int i, int i2) {
    }

    public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int i) {
    }

    public void onCharacteristicRead(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic, int i) {
    }

    public void onCharacteristicWrite(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic, int i) {
    }

    public void onCharacteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
    }

    public void onDescriptorRead(BluetoothGatt bluetoothGatt, BluetoothGattDescriptor bluetoothGattDescriptor, int i) {
    }

    public void onDescriptorWrite(BluetoothGatt bluetoothGatt, BluetoothGattDescriptor bluetoothGattDescriptor, int i) {
    }

    public void onReliableWriteCompleted(BluetoothGatt bluetoothGatt, int i) {
    }

    public void onReadRemoteRssi(BluetoothGatt bluetoothGatt, int i, int i2) {
    }

    public void onMtuChanged(BluetoothGatt bluetoothGatt, int i, int i2) {
    }

    public void onConnectionUpdated(BluetoothGatt bluetoothGatt, int i, int i2, int i3, int i4) {
    }
}
