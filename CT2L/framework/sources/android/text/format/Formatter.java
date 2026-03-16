package android.text.format;

import android.content.Context;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import com.android.internal.R;

public final class Formatter {
    private static final int MILLIS_PER_MINUTE = 60000;
    private static final int SECONDS_PER_DAY = 86400;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;

    public static String formatFileSize(Context context, long number) {
        return formatFileSize(context, number, false);
    }

    public static String formatShortFileSize(Context context, long number) {
        return formatFileSize(context, number, true);
    }

    private static String formatFileSize(Context context, long number, boolean shorter) {
        String value;
        if (context == null) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        float result = number;
        int suffix = R.string.byteShort;
        if (result > 900.0f) {
            suffix = R.string.kilobyteShort;
            result /= 1024.0f;
        }
        if (result > 900.0f) {
            suffix = R.string.megabyteShort;
            result /= 1024.0f;
        }
        if (result > 900.0f) {
            suffix = R.string.gigabyteShort;
            result /= 1024.0f;
        }
        if (result > 900.0f) {
            suffix = R.string.terabyteShort;
            result /= 1024.0f;
        }
        if (result > 900.0f) {
            suffix = R.string.petabyteShort;
            result /= 1024.0f;
        }
        if (result < 1.0f) {
            value = String.format("%.2f", Float.valueOf(result));
        } else if (result < 10.0f) {
            if (shorter) {
                value = String.format("%.1f", Float.valueOf(result));
            } else {
                value = String.format("%.2f", Float.valueOf(result));
            }
        } else if (result >= 100.0f || shorter) {
            value = String.format("%.0f", Float.valueOf(result));
        } else {
            value = String.format("%.2f", Float.valueOf(result));
        }
        return context.getResources().getString(R.string.fileSizeSuffix, value, context.getString(suffix));
    }

    @Deprecated
    public static String formatIpAddress(int ipv4Address) {
        return NetworkUtils.intToInetAddress(ipv4Address).getHostAddress();
    }

    public static String formatShortElapsedTime(Context context, long millis) {
        long secondsLong = millis / 1000;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        if (secondsLong >= 86400) {
            days = (int) (secondsLong / 86400);
            secondsLong -= (long) (SECONDS_PER_DAY * days);
        }
        if (secondsLong >= 3600) {
            hours = (int) (secondsLong / 3600);
            secondsLong -= (long) (hours * SECONDS_PER_HOUR);
        }
        if (secondsLong >= 60) {
            minutes = (int) (secondsLong / 60);
            secondsLong -= (long) (minutes * 60);
        }
        int seconds = (int) secondsLong;
        if (days >= 2) {
            return context.getString(R.string.durationDays, Integer.valueOf(days + ((hours + 12) / 24)));
        }
        if (days > 0) {
            if (hours == 1) {
                return context.getString(R.string.durationDayHour, Integer.valueOf(days), Integer.valueOf(hours));
            }
            return context.getString(R.string.durationDayHours, Integer.valueOf(days), Integer.valueOf(hours));
        }
        if (hours >= 2) {
            return context.getString(R.string.durationHours, Integer.valueOf(hours + ((minutes + 30) / 60)));
        }
        if (hours > 0) {
            if (minutes == 1) {
                return context.getString(R.string.durationHourMinute, Integer.valueOf(hours), Integer.valueOf(minutes));
            }
            return context.getString(R.string.durationHourMinutes, Integer.valueOf(hours), Integer.valueOf(minutes));
        }
        if (minutes >= 2) {
            return context.getString(R.string.durationMinutes, Integer.valueOf(minutes + ((seconds + 30) / 60)));
        }
        if (minutes > 0) {
            if (seconds == 1) {
                return context.getString(R.string.durationMinuteSecond, Integer.valueOf(minutes), Integer.valueOf(seconds));
            }
            return context.getString(R.string.durationMinuteSeconds, Integer.valueOf(minutes), Integer.valueOf(seconds));
        }
        if (seconds == 1) {
            return context.getString(R.string.durationSecond, Integer.valueOf(seconds));
        }
        return context.getString(R.string.durationSeconds, Integer.valueOf(seconds));
    }

    public static String formatShortElapsedTimeRoundingUpToMinutes(Context context, long millis) {
        long minutesRoundedUp = ((millis + DateUtils.MINUTE_IN_MILLIS) - 1) / DateUtils.MINUTE_IN_MILLIS;
        if (minutesRoundedUp == 0) {
            return context.getString(R.string.durationMinutes, 0);
        }
        if (minutesRoundedUp == 1) {
            return context.getString(R.string.durationMinute, 1);
        }
        return formatShortElapsedTime(context, minutesRoundedUp * DateUtils.MINUTE_IN_MILLIS);
    }
}
