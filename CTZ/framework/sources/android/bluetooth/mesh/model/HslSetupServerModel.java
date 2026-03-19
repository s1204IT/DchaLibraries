package android.bluetooth.mesh.model;

import android.bluetooth.BluetoothMesh;
import android.bluetooth.mesh.MeshModel;
import android.util.Log;

public class HslSetupServerModel extends MeshModel {
    private static final boolean DBG = true;
    private static final String TAG = "HslSetupServerModel";

    public HslSetupServerModel(BluetoothMesh bluetoothMesh) {
        super(bluetoothMesh, 12);
    }

    @Override
    public void setTxMessageHeader(int i, int i2, int[] iArr, int i3, int i4, int i5, int i6, int i7) {
        Log.d(TAG, "setTxMessageHeader");
        super.setTxMessageHeader(i, i2, iArr, i3, i4, i5, i6, i7);
    }
}
