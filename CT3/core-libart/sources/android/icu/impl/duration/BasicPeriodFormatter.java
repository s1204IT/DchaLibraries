package android.icu.impl.duration;

import android.icu.impl.duration.BasicPeriodFormatterFactory;
import android.icu.impl.duration.impl.PeriodFormatterData;

class BasicPeriodFormatter implements PeriodFormatter {
    private BasicPeriodFormatterFactory.Customizations customs;
    private PeriodFormatterData data;
    private BasicPeriodFormatterFactory factory;
    private String localeName;

    BasicPeriodFormatter(BasicPeriodFormatterFactory factory, String localeName, PeriodFormatterData data, BasicPeriodFormatterFactory.Customizations customs) {
        this.factory = factory;
        this.localeName = localeName;
        this.data = data;
        this.customs = customs;
    }

    @Override
    public String format(Period period) {
        if (!period.isSet()) {
            throw new IllegalArgumentException("period is not set");
        }
        return format(period.timeLimit, period.inFuture, period.counts);
    }

    @Override
    public PeriodFormatter withLocale(String locName) {
        if (!this.localeName.equals(locName)) {
            PeriodFormatterData newData = this.factory.getData(locName);
            return new BasicPeriodFormatter(this.factory, locName, newData, this.customs);
        }
        return this;
    }

    private String format(int tl, boolean inFuture, int[] counts) {
        int td;
        int mask = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                mask |= 1 << i;
            }
        }
        if (!this.data.allowZero()) {
            int i2 = 0;
            int m = 1;
            while (i2 < counts.length) {
                if ((mask & m) != 0 && counts[i2] == 1) {
                    mask &= ~m;
                }
                i2++;
                m <<= 1;
            }
            if (mask == 0) {
                return null;
            }
        }
        boolean forceD3Seconds = false;
        if (this.data.useMilliseconds() != 0 && ((1 << TimeUnit.MILLISECOND.ordinal) & mask) != 0) {
            int sx = TimeUnit.SECOND.ordinal;
            int mx = TimeUnit.MILLISECOND.ordinal;
            int sf = 1 << sx;
            int mf = 1 << mx;
            switch (this.data.useMilliseconds()) {
                case 1:
                    if ((mask & sf) == 0) {
                        mask |= sf;
                        counts[sx] = 1;
                    }
                    counts[sx] = counts[sx] + ((counts[mx] - 1) / 1000);
                    mask &= ~mf;
                    forceD3Seconds = true;
                    break;
                case 2:
                    if ((mask & sf) != 0) {
                        counts[sx] = counts[sx] + ((counts[mx] - 1) / 1000);
                        mask &= ~mf;
                        forceD3Seconds = true;
                    }
                    break;
            }
        }
        int first = 0;
        int last = counts.length - 1;
        while (first < counts.length && ((1 << first) & mask) == 0) {
            first++;
        }
        while (last > first && ((1 << last) & mask) == 0) {
            last--;
        }
        boolean isZero = true;
        int i3 = first;
        while (true) {
            if (i3 <= last) {
                if (((1 << i3) & mask) == 0 || counts[i3] <= 1) {
                    i3++;
                } else {
                    isZero = false;
                }
            }
        }
        StringBuffer sb = new StringBuffer();
        if (!this.customs.displayLimit || isZero) {
            tl = 0;
        }
        if (!this.customs.displayDirection || isZero) {
            td = 0;
        } else {
            td = inFuture ? 2 : 1;
        }
        boolean useDigitPrefix = this.data.appendPrefix(tl, td, sb);
        boolean multiple = first != last;
        boolean wasSkipped = true;
        boolean skipped = false;
        boolean countSep = this.customs.separatorVariant != 0;
        int i4 = first;
        int j = first;
        while (i4 <= last) {
            if (skipped) {
                this.data.appendSkippedUnit(sb);
                skipped = false;
                wasSkipped = true;
            }
            while (true) {
                j++;
                if (j < last && ((1 << j) & mask) == 0) {
                    skipped = true;
                }
            }
            TimeUnit unit = TimeUnit.units[i4];
            int count = counts[i4] - 1;
            int cv = this.customs.countVariant;
            if (i4 == last) {
                if (forceD3Seconds) {
                    cv = 5;
                }
            } else {
                cv = 0;
            }
            boolean isLast = i4 == last;
            boolean mustSkip = this.data.appendUnit(unit, count, cv, this.customs.unitVariant, countSep, useDigitPrefix, multiple, isLast, wasSkipped, sb);
            skipped |= mustSkip;
            wasSkipped = false;
            if (this.customs.separatorVariant != 0 && j <= last) {
                boolean afterFirst = i4 == first;
                boolean beforeLast = j == last;
                boolean fullSep = this.customs.separatorVariant == 2;
                useDigitPrefix = this.data.appendUnitSeparator(unit, fullSep, afterFirst, beforeLast, sb);
            } else {
                useDigitPrefix = false;
            }
            i4 = j;
        }
        this.data.appendSuffix(tl, td, sb);
        return sb.toString();
    }
}
