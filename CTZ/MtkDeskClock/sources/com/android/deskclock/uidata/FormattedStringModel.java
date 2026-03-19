package com.android.deskclock.uidata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.ArrayMap;
import android.util.SparseArray;
import com.google.android.flexbox.BuildConfig;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

final class FormattedStringModel {
    private Map<Integer, String> mLongWeekdayNames;
    private Map<Integer, String> mShortWeekdayNames;
    private final BroadcastReceiver mLocaleChangedReceiver = new LocaleChangedReceiver();
    private final SparseArray<SparseArray<String>> mNumberFormatCache = new SparseArray<>(3);

    FormattedStringModel(Context context) {
        context.registerReceiver(this.mLocaleChangedReceiver, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
    }

    String getFormattedNumber(int i) {
        return getFormattedNumber(false, i, i != 0 ? 1 + ((int) Math.log10(i)) : 1);
    }

    String getFormattedNumber(int i, int i2) {
        return getFormattedNumber(false, i, i2);
    }

    String getFormattedNumber(boolean z, int i, int i2) {
        if (i < 0) {
            throw new IllegalArgumentException("value may not be negative: " + i);
        }
        int i3 = z ? -i2 : i2;
        SparseArray<String> sparseArray = this.mNumberFormatCache.get(i3);
        if (sparseArray == null) {
            sparseArray = new SparseArray<>((int) Math.pow(10.0d, i2));
            this.mNumberFormatCache.put(i3, sparseArray);
        }
        String str = sparseArray.get(i);
        if (str == null) {
            String str2 = z ? "−" : BuildConfig.FLAVOR;
            String str3 = String.format(Locale.getDefault(), str2 + "%0" + i2 + "d", Integer.valueOf(i));
            sparseArray.put(i, str3);
            return str3;
        }
        return str;
    }

    String getShortWeekday(int i) {
        if (this.mShortWeekdayNames == null) {
            this.mShortWeekdayNames = new ArrayMap(7);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ccccc", Locale.getDefault());
            for (int i2 = 1; i2 <= 7; i2++) {
                this.mShortWeekdayNames.put(Integer.valueOf(i2), simpleDateFormat.format(new GregorianCalendar(2014, 6, (20 + i2) - 1).getTime()));
            }
        }
        return this.mShortWeekdayNames.get(Integer.valueOf(i));
    }

    String getLongWeekday(int i) {
        if (this.mLongWeekdayNames == null) {
            this.mLongWeekdayNames = new ArrayMap(7);
            GregorianCalendar gregorianCalendar = new GregorianCalendar(2014, 6, 20);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            for (int i2 = 1; i2 <= 7; i2++) {
                this.mLongWeekdayNames.put(Integer.valueOf(i2), simpleDateFormat.format(gregorianCalendar.getTime()));
                gregorianCalendar.add(6, 1);
            }
        }
        return this.mLongWeekdayNames.get(Integer.valueOf(i));
    }

    private final class LocaleChangedReceiver extends BroadcastReceiver {
        private LocaleChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            FormattedStringModel.this.mNumberFormatCache.clear();
            FormattedStringModel.this.mShortWeekdayNames = null;
            FormattedStringModel.this.mLongWeekdayNames = null;
        }
    }
}
