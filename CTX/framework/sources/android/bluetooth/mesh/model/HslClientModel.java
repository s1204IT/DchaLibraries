package android.bluetooth.mesh.model;

import android.bluetooth.BluetoothMesh;
import android.bluetooth.mesh.MeshModel;
import android.util.Log;
import java.util.Arrays;

public class HslClientModel extends MeshModel {
    private static final boolean DBG = true;
    private static final String TAG = "HslClientModel";

    public HslClientModel(BluetoothMesh bluetoothMesh) {
        super(bluetoothMesh, 13);
    }

    @Override
    public void setTxMessageHeader(int i, int i2, int[] iArr, int i3, int i4, int i5, int i6, int i7) {
        Log.d(TAG, "setTxMessageHeader");
        super.setTxMessageHeader(i, i2, iArr, i3, i4, i5, i6, i7);
    }

    public void lightHSLGet() {
        super.modelSendPacket();
    }

    public void lightHSLSet(int i, int i2, int i3, int i4, int i5, int i6) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1], super.TwoOctetsToArray(i3)[0], super.TwoOctetsToArray(i3)[1], i4, i5, i6};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightHSLSetUnacknowledged(int i, int i2, int i3, int i4, int i5, int i6) {
        lightHSLSet(i, i2, i3, i4, i5, i6);
    }

    public void lightHSLTargetGet() {
        super.modelSendPacket();
    }

    public void lightHSLDefaultGet() {
        super.modelSendPacket();
    }

    public void lightHSLDefaultSet(int i, int i2, int i3) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1], super.TwoOctetsToArray(i3)[0], super.TwoOctetsToArray(i3)[1]};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightHSLDefaultSetUnacknowledged(int i, int i2, int i3) {
        lightHSLDefaultSet(i, i2, i3);
    }

    public void lightHSLRangeGet() {
        super.modelSendPacket();
    }

    public void lightHSLRangeSet(int i, int i2, int i3, int i4) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1], super.TwoOctetsToArray(i3)[0], super.TwoOctetsToArray(i3)[1], super.TwoOctetsToArray(i4)[0], super.TwoOctetsToArray(i4)[1]};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightHSLRangeSetUnacknowledged(int i, int i2, int i3, int i4) {
        lightHSLRangeSet(i, i2, i3, i4);
    }

    public void lightHSLHueGet(int i, int i2, int i3, int i4) {
        Log.d(TAG, "lightHSLHueGet");
        super.modelSendPacket();
    }

    public void lightHSLHueSet(int i, int i2, int i3, int i4) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], i2, i3, i4};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightHSLHueSetUnacknowledged(int i, int i2, int i3, int i4) {
        lightHSLHueSet(i, i2, i3, i4);
    }

    public void lightHSLSaturationGet() {
        super.modelSendPacket();
    }

    public void lightHSLSaturationSet(int i, int i2, int i3, int i4) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], i2, i3, i4};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightHSLSaturationSetUnacknowledged(int i, int i2, int i3, int i4) {
        lightHSLSaturationSet(i, i2, i3, i4);
    }
}
