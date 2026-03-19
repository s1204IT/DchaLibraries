package android.text.format;

import android.content.Context;
import android.content.res.Resources;
import android.icu.text.MeasureFormat;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.net.NetworkUtils;
import android.text.BidiFormatter;
import android.text.TextUtils;
import com.android.internal.R;
import java.util.Locale;

public final class Formatter {
    public static final int FLAG_CALCULATE_ROUNDED = 2;
    public static final int FLAG_IEC_UNITS = 8;
    public static final int FLAG_SHORTER = 1;
    public static final int FLAG_SI_UNITS = 4;
    private static final int MILLIS_PER_MINUTE = 60000;
    private static final int SECONDS_PER_DAY = 86400;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;

    public static class BytesResult {
        public final long roundedBytes;
        public final String units;
        public final String value;

        public BytesResult(String str, String str2, long j) {
            this.value = str;
            this.units = str2;
            this.roundedBytes = j;
        }
    }

    private static Locale localeFromContext(Context context) {
        return context.getResources().getConfiguration().getLocales().get(0);
    }

    private static String bidiWrap(Context context, String str) {
        if (TextUtils.getLayoutDirectionFromLocale(localeFromContext(context)) == 1) {
            return BidiFormatter.getInstance(true).unicodeWrap(str);
        }
        return str;
    }

    public static String formatFileSize(Context context, long j) {
        if (context == null) {
            return "";
        }
        BytesResult bytes = formatBytes(context.getResources(), j, 4);
        return bidiWrap(context, context.getString(R.string.fileSizeSuffix, bytes.value, bytes.units));
    }

    public static String formatShortFileSize(Context context, long j) {
        if (context == null) {
            return "";
        }
        BytesResult bytes = formatBytes(context.getResources(), j, 5);
        return bidiWrap(context, context.getString(R.string.fileSizeSuffix, bytes.value, bytes.units));
    }

    public static BytesResult formatBytes(Resources resources, long j, int i) {
        long j2;
        String str;
        long jRound;
        long j3 = j;
        int i2 = (i & 8) != 0 ? 1024 : 1000;
        boolean z = j3 < 0;
        if (z) {
            j3 = -j3;
        }
        float f = j3;
        int i3 = R.string.byteShort;
        if (f > 900.0f) {
            i3 = R.string.kilobyteShort;
            j2 = i2;
            f /= i2;
        } else {
            j2 = 1;
        }
        if (f > 900.0f) {
            i3 = R.string.megabyteShort;
            j2 *= (long) i2;
            f /= i2;
        }
        if (f > 900.0f) {
            i3 = R.string.gigabyteShort;
            j2 *= (long) i2;
            f /= i2;
        }
        if (f > 900.0f) {
            i3 = R.string.terabyteShort;
            j2 *= (long) i2;
            f /= i2;
        }
        if (f > 900.0f) {
            i3 = R.string.petabyteShort;
            j2 *= (long) i2;
            f /= i2;
        }
        int i4 = 100;
        if (j2 == 1 || f >= 100.0f) {
            str = "%.0f";
        } else {
            if (f < 1.0f) {
                str = "%.2f";
            } else if (f < 10.0f) {
                if ((i & 1) != 0) {
                    i4 = 10;
                    str = "%.1f";
                } else {
                    str = "%.2f";
                }
            } else if ((i & 1) != 0) {
                str = "%.0f";
            } else {
                str = "%.2f";
            }
            if (z) {
                f = -f;
            }
            String str2 = String.format(str, Float.valueOf(f));
            if ((i & 2) == 0) {
                jRound = (((long) Math.round(f * i4)) * j2) / ((long) i4);
            } else {
                jRound = 0;
            }
            return new BytesResult(str2, resources.getString(i3), jRound);
        }
        i4 = 1;
        if (z) {
        }
        String str22 = String.format(str, Float.valueOf(f));
        if ((i & 2) == 0) {
        }
        return new BytesResult(str22, resources.getString(i3), jRound);
    }

    @Deprecated
    public static String formatIpAddress(int i) {
        return NetworkUtils.intToInetAddress(i).getHostAddress();
    }

    public static String formatShortElapsedTime(Context context, long j) {
        int i;
        int i2;
        int i3;
        long j2 = j / 1000;
        if (j2 >= 86400) {
            i = (int) (j2 / 86400);
            j2 -= (long) (SECONDS_PER_DAY * i);
        } else {
            i = 0;
        }
        if (j2 >= 3600) {
            i2 = (int) (j2 / 3600);
            j2 -= (long) (i2 * 3600);
        } else {
            i2 = 0;
        }
        if (j2 >= 60) {
            i3 = (int) (j2 / 60);
            j2 -= (long) (i3 * 60);
        } else {
            i3 = 0;
        }
        int i4 = (int) j2;
        MeasureFormat measureFormat = MeasureFormat.getInstance(localeFromContext(context), MeasureFormat.FormatWidth.SHORT);
        if (i >= 2 || (i > 0 && i2 == 0)) {
            return measureFormat.format(new Measure(Integer.valueOf(i + ((i2 + 12) / 24)), MeasureUnit.DAY));
        }
        if (i > 0) {
            return measureFormat.formatMeasures(new Measure(Integer.valueOf(i), MeasureUnit.DAY), new Measure(Integer.valueOf(i2), MeasureUnit.HOUR));
        }
        if (i2 >= 2 || (i2 > 0 && i3 == 0)) {
            return measureFormat.format(new Measure(Integer.valueOf(i2 + ((i3 + 30) / 60)), MeasureUnit.HOUR));
        }
        if (i2 > 0) {
            return measureFormat.formatMeasures(new Measure(Integer.valueOf(i2), MeasureUnit.HOUR), new Measure(Integer.valueOf(i3), MeasureUnit.MINUTE));
        }
        if (i3 >= 2 || (i3 > 0 && i4 == 0)) {
            return measureFormat.format(new Measure(Integer.valueOf(i3 + ((i4 + 30) / 60)), MeasureUnit.MINUTE));
        }
        if (i3 > 0) {
            return measureFormat.formatMeasures(new Measure(Integer.valueOf(i3), MeasureUnit.MINUTE), new Measure(Integer.valueOf(i4), MeasureUnit.SECOND));
        }
        return measureFormat.format(new Measure(Integer.valueOf(i4), MeasureUnit.SECOND));
    }

    public static String formatShortElapsedTimeRoundingUpToMinutes(Context context, long j) {
        long j2 = ((j + DateUtils.MINUTE_IN_MILLIS) - 1) / DateUtils.MINUTE_IN_MILLIS;
        if (j2 == 0 || j2 == 1) {
            return MeasureFormat.getInstance(localeFromContext(context), MeasureFormat.FormatWidth.SHORT).format(new Measure(Long.valueOf(j2), MeasureUnit.MINUTE));
        }
        return formatShortElapsedTime(context, j2 * DateUtils.MINUTE_IN_MILLIS);
    }
}
