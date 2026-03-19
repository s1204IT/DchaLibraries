package android.bluetooth.mesh.model;

import android.bluetooth.BluetoothMesh;
import android.bluetooth.mesh.MeshModel;
import android.util.Log;
import java.util.Arrays;

public class CtlClientModel extends MeshModel {
    private static final boolean DBG = true;
    private static final String TAG = "CtlClientModel";

    public CtlClientModel(BluetoothMesh bluetoothMesh) {
        super(bluetoothMesh, 11);
    }

    @Override
    public void setTxMessageHeader(int i, int i2, int[] iArr, int i3, int i4, int i5, int i6, int i7) {
        Log.d(TAG, "setTxMessageHeader");
        super.setTxMessageHeader(i, i2, iArr, i3, i4, i5, i6, i7);
    }

    public void lightCTLGet() {
        super.modelSendPacket();
    }

    public void lightCTLSet(int i, int i2, int i3, int i4, int i5, int i6) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1], super.TwoOctetsToArray(i3)[0], super.TwoOctetsToArray(i3)[1], i4, i5, i6};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightCTLSetUnacknowledged(int i, int i2, int i3, int i4, int i5, int i6) {
        lightCTLSet(i, i2, i3, i4, i5, i6);
    }

    public void lightCTLTemperatureGet() {
        super.modelSendPacket();
    }

    public void lightCTLTemperatureSet(int i, int i2, int i3, int i4, int i5) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1], i3, i4, i5};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightCTLTemperatureSetUnacknowledged(int i, int i2, int i3, int i4, int i5) {
        lightCTLTemperatureSet(i, i2, i3, i4, i5);
    }

    public void lightCTLDefaultGet() {
        super.modelSendPacket();
    }

    public void lightCTLDefaultSet(int i, int i2, int i3) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1], 0, 0};
        iArr[2] = super.TwoOctetsToArray(i3)[0];
        iArr[3] = super.TwoOctetsToArray(i3)[1];
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightCTLDefaultSetUnacknowledged(int i, int i2, int i3) {
        lightCTLDefaultSet(i, i2, i3);
    }

    public void lightCTLTemperatureRangeGet() {
        super.modelSendPacket();
    }

    public void lightCTLTemperatureRangeSet(int i, int i2) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1]};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightCTLTemperatureRangeSetUnacknowledged(int i, int i2) {
        lightCTLTemperatureRangeSet(i, i2);
    }
}
