package android.bluetooth.mesh;

import android.bluetooth.le.ScanResult;
import android.util.Log;

public abstract class BluetoothMeshCallback {
    private static final String TAG = BluetoothMeshCallback.class.getSimpleName();

    public void onMeshEnabled() {
        Log.d(TAG, "onMeshEnabled:");
    }

    public void onConfigReset() {
        Log.d(TAG, "onConfigReset:");
    }

    public void onFriendShipStatus(int i, int i2) {
        Log.d(TAG, "onFriendShipStatus + addr " + i + "staus " + i2);
    }

    public void onOTAEvent(int i, int i2, long j, long j2, long j3, int i3, int i4, int i5, int i6, int i7, int[] iArr) {
        Log.d(TAG, "onOTAEvent + eventId " + i + " errorCode " + i2 + " nodesNum " + i3 + " curr_block " + i4 + " total_block " + i5 + " curr_chunk " + i6 + " chunk_mask " + i7);
    }

    public void onAdvReport(int i, int[] iArr, int i2, int i3, int[] iArr2) {
        Log.d(TAG, "onAdvReport + addrType " + i + " rssi " + i2 + " reportType " + i3);
    }

    public void onProvScanComplete() {
        Log.d(TAG, "onProvScanComplete:");
    }

    public void onScanUnProvDevice(int[] iArr, int i, int[] iArr2, int i2) {
        Log.d(TAG, "onScanUnProvDevice: uuid=" + iArr + " oobInfom=" + i + " uriHash=" + iArr2 + " rssi=" + i2);
    }

    public void onProvCapabilities(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        Log.d(TAG, "onProvCapabilities: numberOfElements=" + i + " algorithms=" + i2 + " publicKeyType=" + i3);
    }

    public void onRequestOobPublicKey() {
        Log.d(TAG, "onRequestOobPublicKey:");
    }

    public void onRequestOobAuthValue(int i, int i2, int i3) {
        Log.d(TAG, "onRequestOobAuthValue: method=" + i + " action=" + i2 + " size=" + i3);
    }

    public void onProvShowOobPublicKey(int[] iArr) {
        Log.d(TAG, "onProvShowOobPublicKey: publicKey=" + iArr);
    }

    public void onProvShowOobAuthValue(int[] iArr) {
        Log.d(TAG, "onProvShowOobAuthValue: authValue=" + iArr);
    }

    public void onProvDone(int i, int[] iArr, boolean z, boolean z2) {
        Log.d(TAG, "onProvDone: address=" + i + " success=" + z + " gattBearer=" + z2);
    }

    public void onScanResult(ScanResult scanResult) {
        Log.d(TAG, "onScanResult: scanResult=" + scanResult);
    }

    public void onKeyRefresh(int i, int i2) {
        Log.d(TAG, "onKeyRefresh: netKeyIndex=" + i + " phase=" + i2);
    }

    public void onIvUpdate(int i, int i2) {
        Log.d(TAG, "onIvUpdate: ivIndex=" + i + " state=" + i2);
    }

    public void onSeqChange(int i) {
        Log.d(TAG, "onSeqChange: seqNumber=" + i);
    }

    public void onProvFactor(int i, int[] iArr, int i2) {
        Log.d(TAG, "onProvFactor: type=" + i + " bufLen=" + i2);
    }

    public void onHeartbeat(int i, int i2) {
        Log.d(TAG, "onHeartbeat: address=" + i + " active=" + i2);
    }

    public void onBearerGattStatus(long j, int i) {
        Log.d(TAG, "onBearerGattStatus: handle=" + j + " status=" + i);
    }

    public void onEvtErrorCode(int i) {
        Log.d(TAG, "onEvtErrorCode: type=" + i);
    }

    public void onOTAMsgHandler(int i, BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessage) {
        Log.d(TAG, "onOTAMsgHandler: modelHandle=" + i);
    }
}
