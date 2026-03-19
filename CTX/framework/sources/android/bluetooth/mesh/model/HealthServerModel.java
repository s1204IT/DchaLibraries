package android.bluetooth.mesh.model;

import android.bluetooth.BluetoothMesh;
import android.bluetooth.mesh.MeshModel;
import android.util.Log;
import java.util.Arrays;

public class HealthServerModel extends MeshModel {
    private static final boolean DBG = true;
    private static final String TAG = "HealthServerModel";
    public int attentionTimerState;
    public int currentFaultState;
    public int healthPeriodState;
    public int registeredFaultState;

    public HealthServerModel(BluetoothMesh bluetoothMesh) {
        super(bluetoothMesh, 5);
    }

    @Override
    public void setTxMessageHeader(int i, int i2, int[] iArr, int i3, int i4, int i5, int i6, int i7) {
        Log.d(TAG, "setTxMessageHeader");
        super.setTxMessageHeader(i, i2, iArr, i3, i4, i5, i6, i7);
    }

    public void healthCurrentStatus(int i, int i2, int[] iArr) {
        int[] iArr2 = new int[2 + iArr.length];
        iArr2[0] = super.TwoOctetsToArray(i2)[0];
        iArr2[1] = super.TwoOctetsToArray(i2)[1];
        for (int i3 = 0; i3 < iArr.length; i3++) {
            iArr2[i3 + 2] = iArr[i3];
        }
        Log.d(TAG, "params:" + Arrays.toString(iArr2));
        super.modelSendPacket(iArr2);
    }

    public void healthFaultStatus(int i, int i2, int[] iArr) {
        int[] iArr2 = new int[2 + iArr.length];
        iArr2[0] = super.TwoOctetsToArray(i2)[0];
        iArr2[1] = super.TwoOctetsToArray(i2)[1];
        for (int i3 = 0; i3 < iArr.length; i3++) {
            iArr2[i3 + 2] = iArr[i3];
        }
        Log.d(TAG, "params:" + Arrays.toString(iArr2));
        super.modelSendPacket(iArr2);
    }

    public void healthPeriodStatus(int i) {
        super.modelSendPacket(i);
    }

    public void healthAttentionStatus(int i) {
        super.modelSendPacket(i);
    }

    public void setCurrentFaultState(int i) {
        this.currentFaultState = i;
    }

    public void setRegisteredFaultState(int i) {
        this.registeredFaultState = i;
    }

    public void setHealthPeriodState(int i) {
        this.healthPeriodState = i;
    }

    public void setAttentionTimerState(int i) {
        this.attentionTimerState = i;
    }

    public int getCurrentFaultState() {
        return this.currentFaultState;
    }

    public int getRegisteredFaultState() {
        return this.registeredFaultState;
    }

    public int getHealthPeriodState() {
        return this.healthPeriodState;
    }

    public int getAttentionTimerState() {
        return this.attentionTimerState;
    }
}
