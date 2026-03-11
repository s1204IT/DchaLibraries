package com.android.systemui.volume;

import android.content.Context;
import android.media.AudioSystem;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.volume.VolumeDialogController;
import java.util.Arrays;

public class Events {
    public static Callback sCallback;
    private static final String TAG = Util.logTag(Events.class);
    private static final String[] EVENT_TAGS = {"show_dialog", "dismiss_dialog", "active_stream_changed", "expand", "key", "collection_started", "collection_stopped", "icon_click", "settings_click", "touch_level_changed", "level_changed", "internal_ringer_mode_changed", "external_ringer_mode_changed", "zen_mode_changed", "suppressor_changed", "mute_changed", "touch_level_done"};
    public static final String[] DISMISS_REASONS = {"unknown", "touch_outside", "volume_controller", "timeout", "screen_off", "settings_clicked", "done_clicked"};
    public static final String[] SHOW_REASONS = {"unknown", "volume_changed", "remote_volume_changed"};

    public interface Callback {
        void writeEvent(long j, int i, Object[] objArr);

        void writeState(long j, VolumeDialogController.State state);
    }

    public static void writeEvent(Context context, int tag, Object... list) {
        long time = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder("writeEvent ").append(EVENT_TAGS[tag]);
        if (list != null && list.length > 0) {
            sb.append(" ");
            switch (tag) {
                case 0:
                    MetricsLogger.visible(context, 207);
                    MetricsLogger.histogram(context, "volume_from_keyguard", ((Boolean) list[1]).booleanValue() ? 1 : 0);
                    sb.append(SHOW_REASONS[((Integer) list[0]).intValue()]).append(" keyguard=").append(list[1]);
                    break;
                case 1:
                    MetricsLogger.hidden(context, 207);
                    sb.append(DISMISS_REASONS[((Integer) list[0]).intValue()]);
                    break;
                case 2:
                    MetricsLogger.action(context, 210, ((Integer) list[0]).intValue());
                    sb.append(AudioSystem.streamToString(((Integer) list[0]).intValue()));
                    break;
                case 3:
                    MetricsLogger.visibility(context, 208, ((Boolean) list[0]).booleanValue());
                    sb.append(list[0]);
                    break;
                case 4:
                    MetricsLogger.action(context, 211, ((Integer) list[1]).intValue());
                    sb.append(AudioSystem.streamToString(((Integer) list[0]).intValue())).append(' ').append(list[1]);
                    break;
                case 5:
                case 6:
                case 8:
                default:
                    sb.append(Arrays.asList(list));
                    break;
                case 7:
                    MetricsLogger.action(context, 212, ((Integer) list[1]).intValue());
                    sb.append(AudioSystem.streamToString(((Integer) list[0]).intValue())).append(' ').append(iconStateToString(((Integer) list[1]).intValue()));
                    break;
                case 9:
                case 10:
                case 15:
                    sb.append(AudioSystem.streamToString(((Integer) list[0]).intValue())).append(' ').append(list[1]);
                    break;
                case 11:
                    sb.append(ringerModeToString(((Integer) list[0]).intValue()));
                    break;
                case 12:
                    MetricsLogger.action(context, 213, ((Integer) list[0]).intValue());
                    sb.append(ringerModeToString(((Integer) list[0]).intValue()));
                    break;
                case 13:
                    sb.append(zenModeToString(((Integer) list[0]).intValue()));
                    break;
                case 14:
                    sb.append(list[0]).append(' ').append(list[1]);
                    break;
                case 16:
                    MetricsLogger.action(context, 209, ((Integer) list[1]).intValue());
                    sb.append(AudioSystem.streamToString(((Integer) list[0]).intValue())).append(' ').append(list[1]);
                    break;
            }
        }
        Log.i(TAG, sb.toString());
        if (sCallback == null) {
            return;
        }
        sCallback.writeEvent(time, tag, list);
    }

    public static void writeState(long time, VolumeDialogController.State state) {
        if (sCallback == null) {
            return;
        }
        sCallback.writeState(time, state);
    }

    private static String iconStateToString(int iconState) {
        switch (iconState) {
            case 1:
                return "unmute";
            case 2:
                return "mute";
            case 3:
                return "vibrate";
            default:
                return "unknown_state_" + iconState;
        }
    }

    private static String ringerModeToString(int ringerMode) {
        switch (ringerMode) {
            case 0:
                return "silent";
            case 1:
                return "vibrate";
            case 2:
                return "normal";
            default:
                return "unknown";
        }
    }

    private static String zenModeToString(int zenMode) {
        switch (zenMode) {
            case 0:
                return "off";
            case 1:
                return "important_interruptions";
            case 2:
                return "no_interruptions";
            case 3:
                return "alarms";
            default:
                return "unknown";
        }
    }
}
