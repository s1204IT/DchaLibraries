package android.bluetooth.mesh.model;

import android.bluetooth.BluetoothMesh;
import android.bluetooth.mesh.MeshModel;
import android.util.Log;
import java.util.Arrays;

public class LightnessClientModel extends MeshModel {
    private static final boolean DBG = true;
    private static final String TAG = "LightnessClientModel";

    public LightnessClientModel(BluetoothMesh bluetoothMesh) {
        super(bluetoothMesh, 10);
    }

    @Override
    public void setTxMessageHeader(int i, int i2, int[] iArr, int i3, int i4, int i5, int i6, int i7) {
        Log.d(TAG, "setTxMessageHeader");
        super.setTxMessageHeader(i, i2, iArr, i3, i4, i5, i6, i7);
    }

    public void lightLightnessGet() {
        super.modelSendPacket();
    }

    public void lightLightnessSet(int i, int i2, int i3, int i4) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], i2, i3, i4};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightLightnessSetUnacknowledged(int i, int i2, int i3, int i4) {
        lightLightnessSet(i, i2, i3, i4);
    }

    public void lightLightnessLinearGet() {
        super.modelSendPacket();
    }

    public void lightLightnessLinearSet(int i, int i2, int i3, int i4) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], i2, i3, i4};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightLightnessLinearSetUnacknowledged(int i, int i2, int i3, int i4) {
        lightLightnessLinearSet(i, i2, i3, i4);
    }

    public void lightLightnessLastGet() {
        super.modelSendPacket();
    }

    public void lightLightnessDefaultGet() {
        super.modelSendPacket();
    }

    public void lightLightnessDefaultSet(int i) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1]};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightLightnessDefaultSetUnacknowledged(int i) {
        lightLightnessDefaultSet(i);
    }

    public void lightLightnessRangeGet() {
        super.modelSendPacket();
    }

    public void lightLightnessRangeSet(int i, int i2) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1]};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightLightnessRangeSetUnacknowledged(int i, int i2) {
        lightLightnessRangeSet(i, i2);
    }
}
