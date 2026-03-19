package android.bluetooth.mesh.model;

import android.bluetooth.BluetoothMesh;
import android.bluetooth.mesh.MeshModel;
import android.util.Log;

public class GenericOnOffServerModel extends MeshModel {
    private static final boolean DBG = true;
    private static final String TAG = "GenericOnOffServerModel";
    public int genericOnOffState;

    public GenericOnOffServerModel(BluetoothMesh bluetoothMesh) {
        super(bluetoothMesh, 7);
    }

    @Override
    public void setTxMessageHeader(int i, int i2, int[] iArr, int i3, int i4, int i5, int i6, int i7) {
        Log.d(TAG, "setTxMessageHeader");
        super.setTxMessageHeader(i, i2, iArr, i3, i4, i5, i6, i7);
    }

    public void genericOnOffStatus(int i, int i2, int i3) {
        Log.d(TAG, "genericOnOffStatus");
        super.modelSendPacket(i, i2, i3);
    }

    public void setGenericOnOffState(int i) {
        Log.d(TAG, "setGenericOnOffState state " + i);
        this.genericOnOffState = i;
    }

    public int getGenericOnOffState() {
        Log.d(TAG, "getGenericOnOffState state " + this.genericOnOffState);
        return this.genericOnOffState;
    }
}
