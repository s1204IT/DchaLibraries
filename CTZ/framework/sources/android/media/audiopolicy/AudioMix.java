package android.media.audiopolicy;

import android.annotation.SystemApi;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioSystem;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

@SystemApi
public class AudioMix {
    private static final int CALLBACK_FLAGS_ALL = 1;
    public static final int CALLBACK_FLAG_NOTIFY_ACTIVITY = 1;

    @SystemApi
    public static final int MIX_STATE_DISABLED = -1;

    @SystemApi
    public static final int MIX_STATE_IDLE = 0;

    @SystemApi
    public static final int MIX_STATE_MIXING = 1;
    public static final int MIX_TYPE_INVALID = -1;
    public static final int MIX_TYPE_PLAYERS = 0;
    public static final int MIX_TYPE_RECORDERS = 1;

    @SystemApi
    public static final int ROUTE_FLAG_LOOP_BACK = 2;

    @SystemApi
    public static final int ROUTE_FLAG_RENDER = 1;
    private static final int ROUTE_FLAG_SUPPORTED = 3;
    int mCallbackFlags;
    String mDeviceAddress;
    final int mDeviceSystemType;
    private AudioFormat mFormat;
    int mMixState;
    private int mMixType;
    private int mRouteFlags;
    private AudioMixingRule mRule;

    @Retention(RetentionPolicy.SOURCE)
    public @interface RouteFlags {
    }

    private AudioMix(AudioMixingRule audioMixingRule, AudioFormat audioFormat, int i, int i2, int i3, String str) {
        this.mMixType = -1;
        this.mMixState = -1;
        this.mRule = audioMixingRule;
        this.mFormat = audioFormat;
        this.mRouteFlags = i;
        this.mMixType = audioMixingRule.getTargetMixType();
        this.mCallbackFlags = i2;
        this.mDeviceSystemType = i3;
        this.mDeviceAddress = str == null ? new String("") : str;
    }

    @SystemApi
    public int getMixState() {
        return this.mMixState;
    }

    int getRouteFlags() {
        return this.mRouteFlags;
    }

    AudioFormat getFormat() {
        return this.mFormat;
    }

    AudioMixingRule getRule() {
        return this.mRule;
    }

    public int getMixType() {
        return this.mMixType;
    }

    void setRegistration(String str) {
        this.mDeviceAddress = str;
    }

    public String getRegistration() {
        return this.mDeviceAddress;
    }

    public boolean isAffectingUsage(int i) {
        return this.mRule.isAffectingUsage(i);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AudioMix audioMix = (AudioMix) obj;
        if (this.mRouteFlags == audioMix.mRouteFlags && this.mRule == audioMix.mRule && this.mMixType == audioMix.mMixType && this.mFormat == audioMix.mFormat) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mRouteFlags), this.mRule, Integer.valueOf(this.mMixType), this.mFormat);
    }

    @SystemApi
    public static class Builder {
        private int mCallbackFlags;
        private String mDeviceAddress;
        private int mDeviceSystemType;
        private AudioFormat mFormat;
        private int mRouteFlags;
        private AudioMixingRule mRule;

        Builder() {
            this.mRule = null;
            this.mFormat = null;
            this.mRouteFlags = 0;
            this.mCallbackFlags = 0;
            this.mDeviceSystemType = 0;
            this.mDeviceAddress = null;
        }

        @SystemApi
        public Builder(AudioMixingRule audioMixingRule) throws IllegalArgumentException {
            this.mRule = null;
            this.mFormat = null;
            this.mRouteFlags = 0;
            this.mCallbackFlags = 0;
            this.mDeviceSystemType = 0;
            this.mDeviceAddress = null;
            if (audioMixingRule == null) {
                throw new IllegalArgumentException("Illegal null AudioMixingRule argument");
            }
            this.mRule = audioMixingRule;
        }

        Builder setMixingRule(AudioMixingRule audioMixingRule) throws IllegalArgumentException {
            if (audioMixingRule == null) {
                throw new IllegalArgumentException("Illegal null AudioMixingRule argument");
            }
            this.mRule = audioMixingRule;
            return this;
        }

        Builder setCallbackFlags(int i) throws IllegalArgumentException {
            if (i != 0 && (i & 1) == 0) {
                throw new IllegalArgumentException("Illegal callback flags 0x" + Integer.toHexString(i).toUpperCase());
            }
            this.mCallbackFlags = i;
            return this;
        }

        Builder setDevice(int i, String str) {
            this.mDeviceSystemType = i;
            this.mDeviceAddress = str;
            return this;
        }

        @SystemApi
        public Builder setFormat(AudioFormat audioFormat) throws IllegalArgumentException {
            if (audioFormat == null) {
                throw new IllegalArgumentException("Illegal null AudioFormat argument");
            }
            this.mFormat = audioFormat;
            return this;
        }

        @SystemApi
        public Builder setRouteFlags(int i) throws IllegalArgumentException {
            if (i == 0) {
                throw new IllegalArgumentException("Illegal empty route flags");
            }
            if ((i & 3) == 0) {
                throw new IllegalArgumentException("Invalid route flags 0x" + Integer.toHexString(i) + "when configuring an AudioMix");
            }
            if ((i & (-4)) != 0) {
                throw new IllegalArgumentException("Unknown route flags 0x" + Integer.toHexString(i) + "when configuring an AudioMix");
            }
            this.mRouteFlags = i;
            return this;
        }

        @SystemApi
        public Builder setDevice(AudioDeviceInfo audioDeviceInfo) throws IllegalArgumentException {
            if (audioDeviceInfo == null) {
                throw new IllegalArgumentException("Illegal null AudioDeviceInfo argument");
            }
            if (!audioDeviceInfo.isSink()) {
                throw new IllegalArgumentException("Unsupported device type on mix, not a sink");
            }
            this.mDeviceSystemType = AudioDeviceInfo.convertDeviceTypeToInternalDevice(audioDeviceInfo.getType());
            this.mDeviceAddress = audioDeviceInfo.getAddress();
            return this;
        }

        @SystemApi
        public AudioMix build() throws IllegalArgumentException {
            if (this.mRule == null) {
                throw new IllegalArgumentException("Illegal null AudioMixingRule");
            }
            if (this.mRouteFlags == 0) {
                this.mRouteFlags = 2;
            }
            if (this.mRouteFlags == 3) {
                throw new IllegalArgumentException("Unsupported route behavior combination 0x" + Integer.toHexString(this.mRouteFlags));
            }
            if (this.mFormat == null) {
                int primaryOutputSamplingRate = AudioSystem.getPrimaryOutputSamplingRate();
                if (primaryOutputSamplingRate <= 0) {
                    primaryOutputSamplingRate = 44100;
                }
                this.mFormat = new AudioFormat.Builder().setSampleRate(primaryOutputSamplingRate).build();
            }
            if (this.mDeviceSystemType != 0 && this.mDeviceSystemType != 32768 && this.mDeviceSystemType != -2147483392) {
                if ((this.mRouteFlags & 1) == 0) {
                    throw new IllegalArgumentException("Can't have audio device without flag ROUTE_FLAG_RENDER");
                }
                if (this.mRule.getTargetMixType() != 0) {
                    throw new IllegalArgumentException("Unsupported device on non-playback mix");
                }
            } else {
                if ((this.mRouteFlags & 1) == 1) {
                    throw new IllegalArgumentException("Can't have flag ROUTE_FLAG_RENDER without an audio device");
                }
                if ((this.mRouteFlags & 3) == 2) {
                    if (this.mRule.getTargetMixType() == 0) {
                        this.mDeviceSystemType = 32768;
                    } else if (this.mRule.getTargetMixType() == 1) {
                        this.mDeviceSystemType = AudioSystem.DEVICE_IN_REMOTE_SUBMIX;
                    } else {
                        throw new IllegalArgumentException("Unknown mixing rule type");
                    }
                }
            }
            return new AudioMix(this.mRule, this.mFormat, this.mRouteFlags, this.mCallbackFlags, this.mDeviceSystemType, this.mDeviceAddress);
        }
    }
}
