package com.android.systemui.volume;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

class Util {
    private static final SimpleDateFormat HMMAA = new SimpleDateFormat("h:mm aa", Locale.US);
    private static int[] AUDIO_MANAGER_FLAGS = {1, 16, 4, 2, 8, 2048, 128, 4096, 1024};
    private static String[] AUDIO_MANAGER_FLAG_NAMES = {"SHOW_UI", "VIBRATE", "PLAY_SOUND", "ALLOW_RINGER_MODES", "REMOVE_SOUND_AND_VIBRATE", "SHOW_VIBRATE_HINT", "SHOW_SILENT_HINT", "FROM_KEY", "SHOW_UI_WARNINGS"};

    public static String logTag(Class<?> cls) {
        String str = "vol." + cls.getSimpleName();
        return str.length() < 23 ? str : str.substring(0, 23);
    }

    public static String ringerModeToString(int i) {
        switch (i) {
            case 0:
                return "RINGER_MODE_SILENT";
            case 1:
                return "RINGER_MODE_VIBRATE";
            case 2:
                return "RINGER_MODE_NORMAL";
            default:
                return "RINGER_MODE_UNKNOWN_" + i;
        }
    }

    public static String mediaMetadataToString(MediaMetadata mediaMetadata) {
        if (mediaMetadata == null) {
            return null;
        }
        return mediaMetadata.getDescription().toString();
    }

    public static String playbackInfoToString(MediaController.PlaybackInfo playbackInfo) {
        if (playbackInfo == null) {
            return null;
        }
        return String.format("PlaybackInfo[vol=%s,max=%s,type=%s,vc=%s],atts=%s", Integer.valueOf(playbackInfo.getCurrentVolume()), Integer.valueOf(playbackInfo.getMaxVolume()), playbackInfoTypeToString(playbackInfo.getPlaybackType()), volumeProviderControlToString(playbackInfo.getVolumeControl()), playbackInfo.getAudioAttributes());
    }

    public static String playbackInfoTypeToString(int i) {
        switch (i) {
            case 1:
                return "LOCAL";
            case 2:
                return "REMOTE";
            default:
                return "UNKNOWN_" + i;
        }
    }

    public static String playbackStateStateToString(int i) {
        switch (i) {
            case 0:
                return "STATE_NONE";
            case 1:
                return "STATE_STOPPED";
            case 2:
                return "STATE_PAUSED";
            case 3:
                return "STATE_PLAYING";
            default:
                return "UNKNOWN_" + i;
        }
    }

    public static String volumeProviderControlToString(int i) {
        switch (i) {
            case 0:
                return "VOLUME_CONTROL_FIXED";
            case 1:
                return "VOLUME_CONTROL_RELATIVE";
            case 2:
                return "VOLUME_CONTROL_ABSOLUTE";
            default:
                return "VOLUME_CONTROL_UNKNOWN_" + i;
        }
    }

    public static String playbackStateToString(PlaybackState playbackState) {
        if (playbackState == null) {
            return null;
        }
        return playbackStateStateToString(playbackState.getState()) + " " + playbackState;
    }

    public static String audioManagerFlagsToString(int i) {
        return bitFieldToString(i, AUDIO_MANAGER_FLAGS, AUDIO_MANAGER_FLAG_NAMES);
    }

    private static String bitFieldToString(int i, int[] iArr, String[] strArr) {
        if (i == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i2 = 0; i2 < iArr.length; i2++) {
            if ((iArr[i2] & i) != 0) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(strArr[i2]);
            }
            i &= ~iArr[i2];
        }
        if (i != 0) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append("UNKNOWN_");
            sb.append(i);
        }
        return sb.toString();
    }

    private static CharSequence emptyToNull(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() == 0) {
            return null;
        }
        return charSequence;
    }

    public static boolean setText(TextView textView, CharSequence charSequence) {
        if (Objects.equals(emptyToNull(textView.getText()), emptyToNull(charSequence))) {
            return false;
        }
        textView.setText(charSequence);
        return true;
    }

    public static final void setVisOrGone(View view, boolean z) {
        if (view != null) {
            if ((view.getVisibility() == 0) == z) {
                return;
            }
            view.setVisibility(z ? 0 : 8);
        }
    }

    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        return telephonyManager != null && telephonyManager.isVoiceCapable();
    }
}
