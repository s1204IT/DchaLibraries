package com.android.contacts.datepicker;

import android.widget.NumberPicker;
import java.text.DecimalFormatSymbols;
import java.util.Formatter;
import java.util.Locale;

public class TwoDigitFormatter implements NumberPicker.Formatter {
    Formatter mFmt;
    char mZeroDigit;
    final StringBuilder mBuilder = new StringBuilder();
    final Object[] mArgs = new Object[1];

    public TwoDigitFormatter() {
        init(Locale.getDefault());
    }

    private void init(Locale locale) {
        this.mFmt = createFormatter(locale);
        this.mZeroDigit = getZeroDigit(locale);
    }

    @Override
    public String format(int i) {
        Locale locale = Locale.getDefault();
        if (this.mZeroDigit != getZeroDigit(locale)) {
            init(locale);
        }
        this.mArgs[0] = Integer.valueOf(i);
        this.mBuilder.delete(0, this.mBuilder.length());
        this.mFmt.format("%02d", this.mArgs);
        return this.mFmt.toString();
    }

    private static char getZeroDigit(Locale locale) {
        return DecimalFormatSymbols.getInstance(locale).getZeroDigit();
    }

    private Formatter createFormatter(Locale locale) {
        return new Formatter(this.mBuilder, locale);
    }
}
