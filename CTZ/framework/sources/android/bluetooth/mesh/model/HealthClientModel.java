package android.bluetooth.mesh.model;

import android.bluetooth.BluetoothMesh;
import android.bluetooth.mesh.MeshModel;
import android.util.Log;
import java.util.Arrays;

public class HealthClientModel extends MeshModel {
    private static final boolean DBG = true;
    private static final String TAG = "HealthClientModel";

    public HealthClientModel(BluetoothMesh bluetoothMesh) {
        super(bluetoothMesh, 6);
    }

    @Override
    public void setTxMessageHeader(int i, int i2, int[] iArr, int i3, int i4, int i5, int i6, int i7) {
        Log.d(TAG, "setTxMessageHeader");
        super.setTxMessageHeader(i, i2, iArr, i3, i4, i5, i6, i7);
    }

    public void healthFaultGet(int i) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1]};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void healthFaultClear(int i) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1]};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void healthFaultClearUnacknowledged(int i) {
        healthFaultClear(i);
    }

    public void healthFaultTest(int i, int i2) {
        int[] iArr = {i, super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1]};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void healthFaultTestUnacknowledged(int i, int i2) {
        healthFaultTest(i, i2);
    }

    public void healthPeriodGet() {
        super.modelSendPacket();
    }

    public void healthPeriodSet(int i) {
        super.modelSendPacket(i);
    }

    public void healthPeriodSetUnacknowledged(int i) {
        healthPeriodSet(i);
    }

    public void healthAttentionGet() {
        super.modelSendPacket();
    }

    public void healthAttentionSet(int i) {
        super.modelSendPacket(i);
    }

    public void healthAttentionSetUnacknowledged(int i) {
        healthAttentionSet(i);
    }
}
