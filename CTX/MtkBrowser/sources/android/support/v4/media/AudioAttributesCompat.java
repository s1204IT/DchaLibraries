package android.support.v4.media;

import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.AudioAttributesCompatApi21;
import android.util.SparseIntArray;
import java.util.Arrays;

public class AudioAttributesCompat {
    private static final int[] SDK_USAGES;
    private static final SparseIntArray SUPPRESSIBLE_USAGES = new SparseIntArray();
    private static boolean sForceLegacyBehavior;
    private AudioAttributesCompatApi21.Wrapper mAudioAttributesWrapper;
    Integer mLegacyStream;
    int mUsage = 0;
    int mContentType = 0;
    int mFlags = 0;

    static {
        SUPPRESSIBLE_USAGES.put(5, 1);
        SUPPRESSIBLE_USAGES.put(6, 2);
        SUPPRESSIBLE_USAGES.put(7, 2);
        SUPPRESSIBLE_USAGES.put(8, 1);
        SUPPRESSIBLE_USAGES.put(9, 1);
        SUPPRESSIBLE_USAGES.put(10, 1);
        SDK_USAGES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16};
    }

    private AudioAttributesCompat() {
    }

    public static AudioAttributesCompat fromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes audioAttributes = (AudioAttributes) bundle.getParcelable("android.support.v4.media.audio_attrs.FRAMEWORKS");
            return audioAttributes == null ? null : wrap(audioAttributes);
        }
        int i = bundle.getInt("android.support.v4.media.audio_attrs.USAGE", 0);
        int i2 = bundle.getInt("android.support.v4.media.audio_attrs.CONTENT_TYPE", 0);
        int i3 = bundle.getInt("android.support.v4.media.audio_attrs.FLAGS", 0);
        AudioAttributesCompat audioAttributesCompat = new AudioAttributesCompat();
        audioAttributesCompat.mUsage = i;
        audioAttributesCompat.mContentType = i2;
        audioAttributesCompat.mFlags = i3;
        audioAttributesCompat.mLegacyStream = bundle.containsKey("android.support.v4.media.audio_attrs.LEGACY_STREAM_TYPE") ? Integer.valueOf(bundle.getInt("android.support.v4.media.audio_attrs.LEGACY_STREAM_TYPE")) : null;
        return audioAttributesCompat;
    }

    static int toVolumeStreamType(boolean z, int i, int i2) {
        if ((i & 1) == 1) {
            return z ? 1 : 7;
        }
        if ((i & 4) == 4) {
            return z ? 0 : 6;
        }
        switch (i2) {
            case 0:
                return z ? Integer.MIN_VALUE : 3;
            case 1:
            case 12:
            case 14:
            case 16:
                return 3;
            case 2:
                return 0;
            case 3:
                return z ? 0 : 8;
            case 4:
                return 4;
            case 5:
            case 7:
            case 8:
            case 9:
            case 10:
                return 5;
            case 6:
                return 2;
            case 11:
                return 10;
            case 13:
                return 1;
            case 15:
            default:
                if (!z) {
                    return 3;
                }
                throw new IllegalArgumentException("Unknown usage value " + i2 + " in audio attributes");
        }
    }

    static String usageToString(int i) {
        switch (i) {
            case 0:
                return new String("USAGE_UNKNOWN");
            case 1:
                return new String("USAGE_MEDIA");
            case 2:
                return new String("USAGE_VOICE_COMMUNICATION");
            case 3:
                return new String("USAGE_VOICE_COMMUNICATION_SIGNALLING");
            case 4:
                return new String("USAGE_ALARM");
            case 5:
                return new String("USAGE_NOTIFICATION");
            case 6:
                return new String("USAGE_NOTIFICATION_RINGTONE");
            case 7:
                return new String("USAGE_NOTIFICATION_COMMUNICATION_REQUEST");
            case 8:
                return new String("USAGE_NOTIFICATION_COMMUNICATION_INSTANT");
            case 9:
                return new String("USAGE_NOTIFICATION_COMMUNICATION_DELAYED");
            case 10:
                return new String("USAGE_NOTIFICATION_EVENT");
            case 11:
                return new String("USAGE_ASSISTANCE_ACCESSIBILITY");
            case 12:
                return new String("USAGE_ASSISTANCE_NAVIGATION_GUIDANCE");
            case 13:
                return new String("USAGE_ASSISTANCE_SONIFICATION");
            case 14:
                return new String("USAGE_GAME");
            case 15:
            default:
                return new String("unknown usage " + i);
            case 16:
                return new String("USAGE_ASSISTANT");
        }
    }

    public static AudioAttributesCompat wrap(Object obj) {
        if (Build.VERSION.SDK_INT < 21 || sForceLegacyBehavior) {
            return null;
        }
        AudioAttributesCompat audioAttributesCompat = new AudioAttributesCompat();
        audioAttributesCompat.mAudioAttributesWrapper = AudioAttributesCompatApi21.Wrapper.wrap((AudioAttributes) obj);
        return audioAttributesCompat;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AudioAttributesCompat audioAttributesCompat = (AudioAttributesCompat) obj;
        if (Build.VERSION.SDK_INT >= 21 && !sForceLegacyBehavior && this.mAudioAttributesWrapper != null) {
            return this.mAudioAttributesWrapper.unwrap().equals(audioAttributesCompat.unwrap());
        }
        if (this.mContentType == audioAttributesCompat.getContentType() && this.mFlags == audioAttributesCompat.getFlags() && this.mUsage == audioAttributesCompat.getUsage()) {
            if (this.mLegacyStream != null) {
                if (this.mLegacyStream.equals(audioAttributesCompat.mLegacyStream)) {
                    return true;
                }
            } else if (audioAttributesCompat.mLegacyStream == null) {
                return true;
            }
        }
        return false;
    }

    public int getContentType() {
        return (Build.VERSION.SDK_INT < 21 || sForceLegacyBehavior || this.mAudioAttributesWrapper == null) ? this.mContentType : this.mAudioAttributesWrapper.unwrap().getContentType();
    }

    public int getFlags() {
        if (Build.VERSION.SDK_INT >= 21 && !sForceLegacyBehavior && this.mAudioAttributesWrapper != null) {
            return this.mAudioAttributesWrapper.unwrap().getFlags();
        }
        int i = this.mFlags;
        int legacyStreamType = getLegacyStreamType();
        if (legacyStreamType == 6) {
            i |= 4;
        } else if (legacyStreamType == 7) {
            i |= 1;
        }
        return i & 273;
    }

    public int getLegacyStreamType() {
        return this.mLegacyStream != null ? this.mLegacyStream.intValue() : (Build.VERSION.SDK_INT < 21 || sForceLegacyBehavior) ? toVolumeStreamType(false, this.mFlags, this.mUsage) : AudioAttributesCompatApi21.toLegacyStreamType(this.mAudioAttributesWrapper);
    }

    public int getUsage() {
        return (Build.VERSION.SDK_INT < 21 || sForceLegacyBehavior || this.mAudioAttributesWrapper == null) ? this.mUsage : this.mAudioAttributesWrapper.unwrap().getUsage();
    }

    public int hashCode() {
        return (Build.VERSION.SDK_INT < 21 || sForceLegacyBehavior || this.mAudioAttributesWrapper == null) ? Arrays.hashCode(new Object[]{Integer.valueOf(this.mContentType), Integer.valueOf(this.mFlags), Integer.valueOf(this.mUsage), this.mLegacyStream}) : this.mAudioAttributesWrapper.unwrap().hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("AudioAttributesCompat:");
        if (unwrap() != null) {
            sb.append(" audioattributes=");
            sb.append(unwrap());
        } else {
            if (this.mLegacyStream != null) {
                sb.append(" stream=");
                sb.append(this.mLegacyStream);
                sb.append(" derived");
            }
            sb.append(" usage=");
            sb.append(usageToString());
            sb.append(" content=");
            sb.append(this.mContentType);
            sb.append(" flags=0x");
            sb.append(Integer.toHexString(this.mFlags).toUpperCase());
        }
        return sb.toString();
    }

    public Object unwrap() {
        if (this.mAudioAttributesWrapper != null) {
            return this.mAudioAttributesWrapper.unwrap();
        }
        return null;
    }

    String usageToString() {
        return usageToString(this.mUsage);
    }
}
