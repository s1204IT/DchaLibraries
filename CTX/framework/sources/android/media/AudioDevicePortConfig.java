package android.media;

public class AudioDevicePortConfig extends AudioPortConfig {
    AudioDevicePortConfig(AudioDevicePort audioDevicePort, int i, int i2, int i3, AudioGainConfig audioGainConfig) {
        super(audioDevicePort, i, i2, i3, audioGainConfig);
    }

    AudioDevicePortConfig(AudioDevicePortConfig audioDevicePortConfig) {
        this(audioDevicePortConfig.port(), audioDevicePortConfig.samplingRate(), audioDevicePortConfig.channelMask(), audioDevicePortConfig.format(), audioDevicePortConfig.gain());
    }

    @Override
    public AudioDevicePort port() {
        return (AudioDevicePort) this.mPort;
    }
}
