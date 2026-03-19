package android.media.midi;

import android.os.Parcel;
import android.os.Parcelable;

public final class MidiDeviceStatus implements Parcelable {
    public static final Parcelable.Creator<MidiDeviceStatus> CREATOR = new Parcelable.Creator<MidiDeviceStatus>() {
        @Override
        public MidiDeviceStatus createFromParcel(Parcel parcel) {
            return new MidiDeviceStatus((MidiDeviceInfo) parcel.readParcelable(MidiDeviceInfo.class.getClassLoader()), parcel.createBooleanArray(), parcel.createIntArray());
        }

        @Override
        public MidiDeviceStatus[] newArray(int i) {
            return new MidiDeviceStatus[i];
        }
    };
    private static final String TAG = "MidiDeviceStatus";
    private final MidiDeviceInfo mDeviceInfo;
    private final boolean[] mInputPortOpen;
    private final int[] mOutputPortOpenCount;

    public MidiDeviceStatus(MidiDeviceInfo midiDeviceInfo, boolean[] zArr, int[] iArr) {
        this.mDeviceInfo = midiDeviceInfo;
        this.mInputPortOpen = new boolean[zArr.length];
        System.arraycopy(zArr, 0, this.mInputPortOpen, 0, zArr.length);
        this.mOutputPortOpenCount = new int[iArr.length];
        System.arraycopy(iArr, 0, this.mOutputPortOpenCount, 0, iArr.length);
    }

    public MidiDeviceStatus(MidiDeviceInfo midiDeviceInfo) {
        this.mDeviceInfo = midiDeviceInfo;
        this.mInputPortOpen = new boolean[midiDeviceInfo.getInputPortCount()];
        this.mOutputPortOpenCount = new int[midiDeviceInfo.getOutputPortCount()];
    }

    public MidiDeviceInfo getDeviceInfo() {
        return this.mDeviceInfo;
    }

    public boolean isInputPortOpen(int i) {
        return this.mInputPortOpen[i];
    }

    public int getOutputPortOpenCount(int i) {
        return this.mOutputPortOpenCount[i];
    }

    public String toString() {
        int inputPortCount = this.mDeviceInfo.getInputPortCount();
        int outputPortCount = this.mDeviceInfo.getOutputPortCount();
        StringBuilder sb = new StringBuilder("mInputPortOpen=[");
        for (int i = 0; i < inputPortCount; i++) {
            sb.append(this.mInputPortOpen[i]);
            if (i < inputPortCount - 1) {
                sb.append(",");
            }
        }
        sb.append("] mOutputPortOpenCount=[");
        for (int i2 = 0; i2 < outputPortCount; i2++) {
            sb.append(this.mOutputPortOpenCount[i2]);
            if (i2 < outputPortCount - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mDeviceInfo, i);
        parcel.writeBooleanArray(this.mInputPortOpen);
        parcel.writeIntArray(this.mOutputPortOpenCount);
    }
}
