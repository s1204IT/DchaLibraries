package android.bluetooth.mesh.model;

import android.bluetooth.BluetoothMesh;
import android.bluetooth.mesh.MeshModel;
import android.util.Log;

public class ConfigurationServerModel extends MeshModel {
    private static final boolean DBG = true;
    private static final String TAG = "ConfigurationServerModel";

    public ConfigurationServerModel(BluetoothMesh bluetoothMesh) {
        super(bluetoothMesh, 3);
    }

    @Override
    public void setConfigMessageHeader(int i, int i2, int i3, int i4, int i5) {
        Log.d(TAG, "setConfigMessageHeader");
        super.setConfigMessageHeader(i, i2, i3, i4, i5);
    }
}
