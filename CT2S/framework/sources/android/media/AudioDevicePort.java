package android.media;

public class AudioDevicePort extends AudioPort {
    private final String mAddress;
    private final int mType;

    AudioDevicePort(AudioHandle handle, int[] samplingRates, int[] channelMasks, int[] formats, AudioGain[] gains, int type, String address) {
        super(handle, !AudioManager.isInputDevice(type) ? 2 : 1, samplingRates, channelMasks, formats, gains);
        this.mType = type;
        this.mAddress = address;
    }

    public int type() {
        return this.mType;
    }

    public String address() {
        return this.mAddress;
    }

    @Override
    public AudioDevicePortConfig buildConfig(int samplingRate, int channelMask, int format, AudioGainConfig gain) {
        return new AudioDevicePortConfig(this, samplingRate, channelMask, format, gain);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof AudioDevicePort)) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public String toString() {
        String type = this.mRole == 1 ? AudioSystem.getInputDeviceName(this.mType) : AudioSystem.getOutputDeviceName(this.mType);
        return "{" + super.toString() + ", mType: " + type + ", mAddress: " + this.mAddress + "}";
    }
}
