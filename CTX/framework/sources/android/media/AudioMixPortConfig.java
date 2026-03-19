package android.media;

public class AudioMixPortConfig extends AudioPortConfig {
    AudioMixPortConfig(AudioMixPort audioMixPort, int i, int i2, int i3, AudioGainConfig audioGainConfig) {
        super(audioMixPort, i, i2, i3, audioGainConfig);
    }

    @Override
    public AudioMixPort port() {
        return (AudioMixPort) this.mPort;
    }
}
