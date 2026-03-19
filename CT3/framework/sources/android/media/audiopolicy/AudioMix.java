package android.media.audiopolicy;

import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioSystem;
import android.net.ProxyInfo;
import java.util.Objects;

public class AudioMix {
    private static final int CALLBACK_FLAGS_ALL = 1;
    public static final int CALLBACK_FLAG_NOTIFY_ACTIVITY = 1;
    public static final int MIX_STATE_DISABLED = -1;
    public static final int MIX_STATE_IDLE = 0;
    public static final int MIX_STATE_MIXING = 1;
    public static final int MIX_TYPE_INVALID = -1;
    public static final int MIX_TYPE_PLAYERS = 0;
    public static final int MIX_TYPE_RECORDERS = 1;
    public static final int ROUTE_FLAG_LOOP_BACK = 2;
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

    AudioMix(AudioMixingRule rule, AudioFormat format, int routeFlags, int callbackFlags, int deviceType, String deviceAddress, AudioMix audioMix) {
        this(rule, format, routeFlags, callbackFlags, deviceType, deviceAddress);
    }

    private AudioMix(AudioMixingRule rule, AudioFormat format, int routeFlags, int callbackFlags, int deviceType, String deviceAddress) {
        this.mMixType = -1;
        this.mMixState = -1;
        this.mRule = rule;
        this.mFormat = format;
        this.mRouteFlags = routeFlags;
        this.mMixType = rule.getTargetMixType();
        this.mCallbackFlags = callbackFlags;
        this.mDeviceSystemType = deviceType;
        this.mDeviceAddress = deviceAddress == null ? new String(ProxyInfo.LOCAL_EXCL_LIST) : deviceAddress;
    }

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

    void setRegistration(String regId) {
        this.mDeviceAddress = regId;
    }

    public String getRegistration() {
        return this.mDeviceAddress;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mRouteFlags), this.mRule, Integer.valueOf(this.mMixType), this.mFormat);
    }

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

        public Builder(AudioMixingRule rule) throws IllegalArgumentException {
            this.mRule = null;
            this.mFormat = null;
            this.mRouteFlags = 0;
            this.mCallbackFlags = 0;
            this.mDeviceSystemType = 0;
            this.mDeviceAddress = null;
            if (rule == null) {
                throw new IllegalArgumentException("Illegal null AudioMixingRule argument");
            }
            this.mRule = rule;
        }

        Builder setMixingRule(AudioMixingRule rule) throws IllegalArgumentException {
            if (rule == null) {
                throw new IllegalArgumentException("Illegal null AudioMixingRule argument");
            }
            this.mRule = rule;
            return this;
        }

        Builder setCallbackFlags(int flags) throws IllegalArgumentException {
            if (flags != 0 && (flags & 1) == 0) {
                throw new IllegalArgumentException("Illegal callback flags 0x" + Integer.toHexString(flags).toUpperCase());
            }
            this.mCallbackFlags = flags;
            return this;
        }

        Builder setDevice(int deviceType, String address) {
            this.mDeviceSystemType = deviceType;
            this.mDeviceAddress = address;
            return this;
        }

        public Builder setFormat(AudioFormat format) throws IllegalArgumentException {
            if (format == null) {
                throw new IllegalArgumentException("Illegal null AudioFormat argument");
            }
            this.mFormat = format;
            return this;
        }

        public Builder setRouteFlags(int routeFlags) throws IllegalArgumentException {
            if (routeFlags == 0) {
                throw new IllegalArgumentException("Illegal empty route flags");
            }
            if ((routeFlags & 3) == 0) {
                throw new IllegalArgumentException("Invalid route flags 0x" + Integer.toHexString(routeFlags) + "when configuring an AudioMix");
            }
            if ((routeFlags & (-4)) != 0) {
                throw new IllegalArgumentException("Unknown route flags 0x" + Integer.toHexString(routeFlags) + "when configuring an AudioMix");
            }
            this.mRouteFlags = routeFlags;
            return this;
        }

        public Builder setDevice(AudioDeviceInfo device) throws IllegalArgumentException {
            if (device == null) {
                throw new IllegalArgumentException("Illegal null AudioDeviceInfo argument");
            }
            if (!device.isSink()) {
                throw new IllegalArgumentException("Unsupported device type on mix, not a sink");
            }
            this.mDeviceSystemType = AudioDeviceInfo.convertDeviceTypeToInternalDevice(device.getType());
            this.mDeviceAddress = device.getAddress();
            return this;
        }

        public AudioMix build() throws IllegalArgumentException {
            AudioMix audioMix = null;
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
                int rate = AudioSystem.getPrimaryOutputSamplingRate();
                if (rate <= 0) {
                    rate = 44100;
                }
                this.mFormat = new AudioFormat.Builder().setSampleRate(rate).build();
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
            return new AudioMix(this.mRule, this.mFormat, this.mRouteFlags, this.mCallbackFlags, this.mDeviceSystemType, this.mDeviceAddress, audioMix);
        }
    }
}
