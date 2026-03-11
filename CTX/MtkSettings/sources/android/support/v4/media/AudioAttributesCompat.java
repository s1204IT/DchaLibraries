package android.support.v4.media;

import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.AudioAttributesCompatApi21;
import android.util.SparseIntArray;
import com.android.settingslib.wifi.AccessPoint;
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

    public Object unwrap() {
        if (this.mAudioAttributesWrapper != null) {
            return this.mAudioAttributesWrapper.unwrap();
        }
        return null;
    }

    public int getLegacyStreamType() {
        if (this.mLegacyStream != null) {
            return this.mLegacyStream.intValue();
        }
        if (Build.VERSION.SDK_INT >= 21 && !sForceLegacyBehavior) {
            return AudioAttributesCompatApi21.toLegacyStreamType(this.mAudioAttributesWrapper);
        }
        return toVolumeStreamType(false, this.mFlags, this.mUsage);
    }

    public static AudioAttributesCompat wrap(Object aa) {
        if (Build.VERSION.SDK_INT >= 21 && !sForceLegacyBehavior) {
            AudioAttributesCompat aac = new AudioAttributesCompat();
            aac.mAudioAttributesWrapper = AudioAttributesCompatApi21.Wrapper.wrap((AudioAttributes) aa);
            return aac;
        }
        return null;
    }

    public int getContentType() {
        if (Build.VERSION.SDK_INT >= 21 && !sForceLegacyBehavior && this.mAudioAttributesWrapper != null) {
            return this.mAudioAttributesWrapper.unwrap().getContentType();
        }
        return this.mContentType;
    }

    public int getUsage() {
        if (Build.VERSION.SDK_INT >= 21 && !sForceLegacyBehavior && this.mAudioAttributesWrapper != null) {
            return this.mAudioAttributesWrapper.unwrap().getUsage();
        }
        return this.mUsage;
    }

    public int getFlags() {
        if (Build.VERSION.SDK_INT >= 21 && !sForceLegacyBehavior && this.mAudioAttributesWrapper != null) {
            return this.mAudioAttributesWrapper.unwrap().getFlags();
        }
        int flags = this.mFlags;
        int legacyStream = getLegacyStreamType();
        if (legacyStream == 6) {
            flags |= 4;
        } else if (legacyStream == 7) {
            flags |= 1;
        }
        return flags & 273;
    }

    public static AudioAttributesCompat fromBundle(Bundle bundle) {
        Integer numValueOf = null;
        if (bundle == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes frameworkAttrs = (AudioAttributes) bundle.getParcelable("android.support.v4.media.audio_attrs.FRAMEWORKS");
            if (frameworkAttrs == null) {
                return null;
            }
            return wrap(frameworkAttrs);
        }
        int usage = bundle.getInt("android.support.v4.media.audio_attrs.USAGE", 0);
        int contentType = bundle.getInt("android.support.v4.media.audio_attrs.CONTENT_TYPE", 0);
        int flags = bundle.getInt("android.support.v4.media.audio_attrs.FLAGS", 0);
        AudioAttributesCompat attr = new AudioAttributesCompat();
        attr.mUsage = usage;
        attr.mContentType = contentType;
        attr.mFlags = flags;
        if (bundle.containsKey("android.support.v4.media.audio_attrs.LEGACY_STREAM_TYPE")) {
            numValueOf = Integer.valueOf(bundle.getInt("android.support.v4.media.audio_attrs.LEGACY_STREAM_TYPE"));
        }
        attr.mLegacyStream = numValueOf;
        return attr;
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

    String usageToString() {
        return usageToString(this.mUsage);
    }

    static String usageToString(int usage) {
        switch (usage) {
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
            case AccessPoint.Speed.MODERATE:
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
                return new String("unknown usage " + usage);
            case 16:
                return new String("USAGE_ASSISTANT");
        }
    }

    static int toVolumeStreamType(boolean fromGetVolumeControlStream, int flags, int usage) {
        if ((flags & 1) == 1) {
            return fromGetVolumeControlStream ? 1 : 7;
        }
        if ((flags & 4) == 4) {
            return fromGetVolumeControlStream ? 0 : 6;
        }
        switch (usage) {
            case 0:
                if (fromGetVolumeControlStream) {
                    return AccessPoint.UNREACHABLE_RSSI;
                }
                return 3;
            case 1:
            case 12:
            case 14:
            case 16:
                return 3;
            case 2:
                return 0;
            case 3:
                return fromGetVolumeControlStream ? 0 : 8;
            case 4:
                return 4;
            case 5:
            case 7:
            case 8:
            case 9:
            case AccessPoint.Speed.MODERATE:
                return 5;
            case 6:
                return 2;
            case 11:
                return 10;
            case 13:
                return 1;
            case 15:
            default:
                if (!fromGetVolumeControlStream) {
                    return 3;
                }
                throw new IllegalArgumentException("Unknown usage value " + usage + " in audio attributes");
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudioAttributesCompat that = (AudioAttributesCompat) o;
        if (Build.VERSION.SDK_INT >= 21 && !sForceLegacyBehavior && this.mAudioAttributesWrapper != null) {
            return this.mAudioAttributesWrapper.unwrap().equals(that.unwrap());
        }
        if (this.mContentType == that.getContentType() && this.mFlags == that.getFlags() && this.mUsage == that.getUsage()) {
            if (this.mLegacyStream != null) {
                if (this.mLegacyStream.equals(that.mLegacyStream)) {
                    return true;
                }
            } else if (that.mLegacyStream == null) {
                return true;
            }
        }
        return false;
    }
}
