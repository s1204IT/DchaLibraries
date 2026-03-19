package android.bluetooth.mesh.model;

import android.bluetooth.BluetoothMesh;
import android.bluetooth.mesh.MeshModel;
import android.util.Log;
import java.util.Arrays;

public class CtlServerModel extends MeshModel {
    private static final boolean DBG = true;
    private static final String TAG = "CtlServerModel";
    public int genericLevelState;
    public int lightCTLDeltaUVDefaultState;
    public int lightCTLDeltaUVState;
    public int lightCTLState;
    public int lightCTLTemperatureDefaultState;
    public int lightCTLTemperatureRangeState;
    public int lightCTLTemperatureState;

    public CtlServerModel(BluetoothMesh bluetoothMesh) {
        super(bluetoothMesh, 500);
    }

    @Override
    public void setTxMessageHeader(int i, int i2, int[] iArr, int i3, int i4, int i5, int i6, int i7) {
        Log.d(TAG, "setTxMessageHeader");
        super.setTxMessageHeader(i, i2, iArr, i3, i4, i5, i6, i7);
    }

    public void lightCTLStatus(int i, int i2, int i3, int i4, int i5) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1], super.TwoOctetsToArray(i3)[0], super.TwoOctetsToArray(i3)[1], super.TwoOctetsToArray(i4)[0], super.TwoOctetsToArray(i4)[1], i5};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightCTLTemperatureRangeStatus(int i, int i2, int i3) {
        int[] iArr = {i, super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1], super.TwoOctetsToArray(i3)[0], super.TwoOctetsToArray(i3)[1]};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightCTLDefaultStatus(int i, int i2, int i3) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1], super.TwoOctetsToArray(i3)[0], super.TwoOctetsToArray(i3)[1]};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }
}
