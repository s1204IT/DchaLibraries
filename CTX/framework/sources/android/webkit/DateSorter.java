package android.webkit;

import android.content.Context;
import android.content.res.Resources;
import com.android.internal.R;
import java.util.Calendar;
import java.util.Locale;
import libcore.icu.LocaleData;

public class DateSorter {
    public static final int DAY_COUNT = 5;
    private static final String LOGTAG = "webkit";
    private static final int NUM_DAYS_AGO = 7;
    private long[] mBins = new long[4];
    private String[] mLabels = new String[5];

    public DateSorter(Context context) {
        Resources resources = context.getResources();
        Calendar calendar = Calendar.getInstance();
        beginningOfDay(calendar);
        this.mBins[0] = calendar.getTimeInMillis();
        calendar.add(6, -1);
        this.mBins[1] = calendar.getTimeInMillis();
        calendar.add(6, -6);
        this.mBins[2] = calendar.getTimeInMillis();
        calendar.add(6, 7);
        calendar.add(2, -1);
        this.mBins[3] = calendar.getTimeInMillis();
        Locale locale = resources.getConfiguration().locale;
        LocaleData localeData = LocaleData.get(locale == null ? Locale.getDefault() : locale);
        this.mLabels[0] = localeData.today;
        this.mLabels[1] = localeData.yesterday;
        this.mLabels[2] = String.format(resources.getQuantityString(R.plurals.last_num_days, 7), 7);
        this.mLabels[3] = context.getString(R.string.last_month);
        this.mLabels[4] = context.getString(R.string.older);
    }

    public int getIndex(long j) {
        for (int i = 0; i < 4; i++) {
            if (j > this.mBins[i]) {
                return i;
            }
        }
        return 4;
    }

    public String getLabel(int i) {
        if (i < 0 || i >= 5) {
            return "";
        }
        return this.mLabels[i];
    }

    public long getBoundary(int i) {
        if (i < 0 || i > 4) {
            i = 0;
        }
        if (i == 4) {
            return Long.MIN_VALUE;
        }
        return this.mBins[i];
    }

    private void beginningOfDay(Calendar calendar) {
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
    }
}
