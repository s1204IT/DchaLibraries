package android.bluetooth.mesh.model;

import android.bluetooth.BluetoothMesh;
import android.bluetooth.mesh.MeshModel;
import android.util.Log;
import java.util.Arrays;

public class HslServerModel extends MeshModel {
    private static final boolean DBG = true;
    private static final String TAG = "HslServerModel";
    public int genericLevelState;
    public int lightHSLHueDefaultState;
    public int lightHSLHueRangeState;
    public int lightHSLHueState;
    public int lightHSLSaturationDefaultState;
    public int lightHSLSaturationRangeState;
    public int lightHSLSaturationState;
    public int lightHSLState;

    public HslServerModel(BluetoothMesh bluetoothMesh) {
        super(bluetoothMesh, 500);
    }

    @Override
    public void setTxMessageHeader(int i, int i2, int[] iArr, int i3, int i4, int i5, int i6, int i7) {
        Log.d(TAG, "setTxMessageHeader");
        super.setTxMessageHeader(i, i2, iArr, i3, i4, i5, i6, i7);
    }

    public void lightHSLStatus(int i, int i2, int i3, int i4) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1], super.TwoOctetsToArray(i3)[0], super.TwoOctetsToArray(i3)[1], i4};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightHSLTargetStatus(int i, int i2, int i3, int i4) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1], super.TwoOctetsToArray(i3)[0], super.TwoOctetsToArray(i3)[1], i4};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }

    public void lightHSLDefaultStatus(int i, int i2, int i3) {
        int[] iArr = {super.TwoOctetsToArray(i)[0], super.TwoOctetsToArray(i)[1], super.TwoOctetsToArray(i2)[0], super.TwoOctetsToArray(i2)[1], super.TwoOctetsToArray(i3)[0], super.TwoOctetsToArray(i3)[1]};
        Log.d(TAG, "params:" + Arrays.toString(iArr));
        super.modelSendPacket(iArr);
    }
}
