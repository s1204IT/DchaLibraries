package com.android.deskclock.stopwatch;

import android.content.Context;
import com.android.deskclock.R;
import java.text.DecimalFormatSymbols;

public class Stopwatches {
    public static String getShareTitle(Context context) {
        String[] mLabels = context.getResources().getStringArray(R.array.sw_share_strings);
        return mLabels[(int) (Math.random() * ((double) mLabels.length))];
    }

    public static String buildShareResults(Context context, String time, long[] laps) {
        StringBuilder b = new StringBuilder(context.getString(R.string.sw_share_main, time));
        b.append("\n");
        int lapsNum = laps != null ? laps.length : 0;
        if (lapsNum == 0) {
            return b.toString();
        }
        b.append(context.getString(R.string.sw_share_laps));
        b.append("\n");
        for (int i = 1; i <= lapsNum; i++) {
            b.append(getTimeText(context, laps[lapsNum - i], i));
            b.append("\n");
        }
        return b.toString();
    }

    public static String buildShareResults(Context context, long time, long[] laps) {
        return buildShareResults(context, getTimeText(context, time, -1), laps);
    }

    public static String getTimeText(Context context, long time, int lap) {
        String[] formats;
        int formatIndex;
        if (time < 0) {
            time = 0;
        }
        if (lap != -1) {
            formats = context.getResources().getStringArray(R.array.shared_laps_format_set);
        } else {
            formats = context.getResources().getStringArray(R.array.stopwatch_format_set);
        }
        char decimalSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator();
        long seconds = time / 1000;
        long hundreds = (time - (1000 * seconds)) / 10;
        long minutes = seconds / 60;
        long seconds2 = seconds - (60 * minutes);
        long hours = minutes / 60;
        long minutes2 = minutes - (60 * hours);
        if (hours >= 100) {
            formatIndex = 4;
        } else if (hours >= 10) {
            formatIndex = 3;
        } else if (hours > 0) {
            formatIndex = 2;
        } else if (minutes2 >= 10) {
            formatIndex = 1;
        } else {
            formatIndex = 0;
        }
        return String.format(formats[formatIndex], Long.valueOf(hours), Long.valueOf(minutes2), Long.valueOf(seconds2), Long.valueOf(hundreds), Character.valueOf(decimalSeparator), Integer.valueOf(lap));
    }

    public static String formatTimeText(long time, String format) {
        if (time < 0) {
            time = 0;
        }
        long seconds = time / 1000;
        long hundreds = (time - (1000 * seconds)) / 10;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        char decimalSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator();
        return String.format(format, Long.valueOf(hours), Long.valueOf(minutes - (60 * hours)), Long.valueOf(seconds - (60 * minutes)), Long.valueOf(hundreds), Character.valueOf(decimalSeparator));
    }
}
