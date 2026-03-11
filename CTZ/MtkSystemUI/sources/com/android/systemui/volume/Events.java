package com.android.systemui.volume;

import android.content.Context;
import android.media.AudioSystem;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.plugins.VolumeDialogController;
import java.util.Arrays;

public class Events {
    public static Callback sCallback;
    private static final String TAG = Util.logTag(Events.class);
    private static final String[] EVENT_TAGS = {"show_dialog", "dismiss_dialog", "active_stream_changed", "expand", "key", "collection_started", "collection_stopped", "icon_click", "settings_click", "touch_level_changed", "level_changed", "internal_ringer_mode_changed", "external_ringer_mode_changed", "zen_mode_changed", "suppressor_changed", "mute_changed", "touch_level_done", "zen_mode_config_changed", "ringer_toggle"};
    public static final String[] DISMISS_REASONS = {"unknown", "touch_outside", "volume_controller", "timeout", "screen_off", "settings_clicked", "done_clicked", "a11y_stream_changed", "output_chooser"};
    public static final String[] SHOW_REASONS = {"unknown", "volume_changed", "remote_volume_changed"};

    public interface Callback {
        void writeEvent(long j, int i, Object[] objArr);

        void writeState(long j, VolumeDialogController.State state);
    }

    public static void writeEvent(Context context, int i, Object... objArr) {
        MetricsLogger metricsLogger = new MetricsLogger();
        long jCurrentTimeMillis = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder("writeEvent ");
        sb.append(EVENT_TAGS[i]);
        if (objArr != null && objArr.length > 0) {
            sb.append(" ");
            switch (i) {
                case 0:
                    MetricsLogger.visible(context, 207);
                    MetricsLogger.histogram(context, "volume_from_keyguard", ((Boolean) objArr[1]).booleanValue() ? 1 : 0);
                    sb.append(SHOW_REASONS[((Integer) objArr[0]).intValue()]);
                    sb.append(" keyguard=");
                    sb.append(objArr[1]);
                    break;
                case 1:
                    MetricsLogger.hidden(context, 207);
                    sb.append(DISMISS_REASONS[((Integer) objArr[0]).intValue()]);
                    break;
                case 2:
                    MetricsLogger.action(context, 210, ((Integer) objArr[0]).intValue());
                    sb.append(AudioSystem.streamToString(((Integer) objArr[0]).intValue()));
                    break;
                case 3:
                    MetricsLogger.visibility(context, 208, ((Boolean) objArr[0]).booleanValue());
                    sb.append(objArr[0]);
                    break;
                case 4:
                    MetricsLogger.action(context, 211, ((Integer) objArr[0]).intValue());
                    sb.append(AudioSystem.streamToString(((Integer) objArr[0]).intValue()));
                    sb.append(' ');
                    sb.append(objArr[1]);
                    break;
                case 5:
                case 6:
                case 17:
                default:
                    sb.append(Arrays.asList(objArr));
                    break;
                case 7:
                    MetricsLogger.action(context, 212, ((Integer) objArr[0]).intValue());
                    sb.append(AudioSystem.streamToString(((Integer) objArr[0]).intValue()));
                    sb.append(' ');
                    sb.append(iconStateToString(((Integer) objArr[1]).intValue()));
                    break;
                case 8:
                    metricsLogger.action(1386);
                    break;
                case 9:
                case 10:
                case 15:
                    sb.append(AudioSystem.streamToString(((Integer) objArr[0]).intValue()));
                    sb.append(' ');
                    sb.append(objArr[1]);
                    break;
                case 11:
                    sb.append(ringerModeToString(((Integer) objArr[0]).intValue()));
                    break;
                case 12:
                    MetricsLogger.action(context, 213, ((Integer) objArr[0]).intValue());
                    sb.append(ringerModeToString(((Integer) objArr[0]).intValue()));
                    break;
                case 13:
                    sb.append(zenModeToString(((Integer) objArr[0]).intValue()));
                    break;
                case 14:
                    sb.append(objArr[0]);
                    sb.append(' ');
                    sb.append(objArr[1]);
                    break;
                case 16:
                    MetricsLogger.action(context, 209, ((Integer) objArr[1]).intValue());
                    sb.append(AudioSystem.streamToString(((Integer) objArr[0]).intValue()));
                    sb.append(' ');
                    sb.append(objArr[1]);
                    break;
                case 18:
                    metricsLogger.action(1385, ((Integer) objArr[0]).intValue());
                    break;
            }
        }
        Log.i(TAG, sb.toString());
        if (sCallback != null) {
            sCallback.writeEvent(jCurrentTimeMillis, i, objArr);
        }
    }

    public static void writeState(long j, VolumeDialogController.State state) {
        if (sCallback != null) {
            sCallback.writeState(j, state);
        }
    }

    private static String iconStateToString(int i) {
        switch (i) {
            case 1:
                return "unmute";
            case 2:
                return "mute";
            case 3:
                return "vibrate";
            default:
                return "unknown_state_" + i;
        }
    }

    private static String ringerModeToString(int i) {
        switch (i) {
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

    private static String zenModeToString(int i) {
        switch (i) {
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
