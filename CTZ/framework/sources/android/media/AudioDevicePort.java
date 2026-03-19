package android.media;

public class AudioDevicePort extends AudioPort {
    private final String mAddress;
    private final int mType;

    AudioDevicePort(AudioHandle audioHandle, String str, int[] iArr, int[] iArr2, int[] iArr3, int[] iArr4, AudioGain[] audioGainArr, int i, String str2) {
        super(audioHandle, AudioManager.isInputDevice(i) ? 1 : 2, str, iArr, iArr2, iArr3, iArr4, audioGainArr);
        this.mType = i;
        this.mAddress = str2;
    }

    public int type() {
        return this.mType;
    }

    public String address() {
        return this.mAddress;
    }

    @Override
    public AudioDevicePortConfig buildConfig(int i, int i2, int i3, AudioGainConfig audioGainConfig) {
        return new AudioDevicePortConfig(this, i, i2, i3, audioGainConfig);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AudioDevicePort)) {
            return false;
        }
        AudioDevicePort audioDevicePort = (AudioDevicePort) obj;
        if (this.mType != audioDevicePort.type()) {
            return false;
        }
        if ((this.mAddress == null && audioDevicePort.address() != null) || !this.mAddress.equals(audioDevicePort.address())) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        String outputDeviceName;
        if (this.mRole == 1) {
            outputDeviceName = AudioSystem.getInputDeviceName(this.mType);
        } else {
            outputDeviceName = AudioSystem.getOutputDeviceName(this.mType);
        }
        return "{" + super.toString() + ", mType: " + outputDeviceName + ", mAddress: " + this.mAddress + "}";
    }
}
