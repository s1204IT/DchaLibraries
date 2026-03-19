package android.icu.impl.duration;

import android.icu.impl.duration.BasicPeriodFormatterFactory;
import android.icu.impl.duration.impl.PeriodFormatterData;

class BasicPeriodFormatter implements PeriodFormatter {
    private BasicPeriodFormatterFactory.Customizations customs;
    private PeriodFormatterData data;
    private BasicPeriodFormatterFactory factory;
    private String localeName;

    BasicPeriodFormatter(BasicPeriodFormatterFactory basicPeriodFormatterFactory, String str, PeriodFormatterData periodFormatterData, BasicPeriodFormatterFactory.Customizations customizations) {
        this.factory = basicPeriodFormatterFactory;
        this.localeName = str;
        this.data = periodFormatterData;
        this.customs = customizations;
    }

    @Override
    public String format(Period period) {
        if (!period.isSet()) {
            throw new IllegalArgumentException("period is not set");
        }
        return format(period.timeLimit, period.inFuture, period.counts);
    }

    @Override
    public PeriodFormatter withLocale(String str) {
        if (!this.localeName.equals(str)) {
            return new BasicPeriodFormatter(this.factory, str, this.factory.getData(str), this.customs);
        }
        return this;
    }

    private String format(int i, boolean z, int[] iArr) {
        int i2;
        boolean z2;
        boolean z3;
        int i3;
        boolean z4;
        int i4;
        int i5;
        int i6;
        int[] iArr2 = iArr;
        int i7 = 0;
        int i8 = 0;
        while (true) {
            i2 = 1;
            if (i7 >= iArr2.length) {
                break;
            }
            if (iArr2[i7] > 0) {
                i8 |= 1 << i7;
            }
            i7++;
        }
        if (!this.data.allowZero()) {
            int i9 = 1;
            int i10 = 0;
            while (i10 < iArr2.length) {
                if ((i8 & i9) != 0 && iArr2[i10] == 1) {
                    i8 &= ~i9;
                }
                i10++;
                i9 <<= 1;
            }
            if (i8 == 0) {
                return null;
            }
        }
        if (this.data.useMilliseconds() != 0 && ((1 << TimeUnit.MILLISECOND.ordinal) & i8) != 0) {
            byte b = TimeUnit.SECOND.ordinal;
            byte b2 = TimeUnit.MILLISECOND.ordinal;
            int i11 = 1 << b;
            int i12 = 1 << b2;
            switch (this.data.useMilliseconds()) {
                case 1:
                    if ((i8 & i11) == 0) {
                        i8 |= i11;
                        iArr2[b] = 1;
                    }
                    iArr2[b] = iArr2[b] + ((iArr2[b2] - 1) / 1000);
                    i8 &= ~i12;
                    z2 = true;
                    break;
                case 2:
                    if ((i11 & i8) != 0) {
                        iArr2[b] = iArr2[b] + ((iArr2[b2] - 1) / 1000);
                        i8 &= ~i12;
                        z2 = true;
                        break;
                    }
            }
        } else {
            z2 = false;
        }
        int length = iArr2.length - 1;
        int i13 = 0;
        while (i13 < iArr2.length && ((1 << i13) & i8) == 0) {
            i13++;
        }
        while (length > i13 && ((1 << length) & i8) == 0) {
            length--;
        }
        int i14 = i13;
        while (true) {
            if (i14 > length) {
                z3 = true;
            } else if (((1 << i14) & i8) == 0 || iArr2[i14] <= 1) {
                i14++;
            } else {
                z3 = false;
            }
        }
        StringBuffer stringBuffer = new StringBuffer();
        int i15 = (!this.customs.displayLimit || z3) ? 0 : i;
        byte b3 = 2;
        int i16 = (!this.customs.displayDirection || z3) ? 0 : z ? 2 : 1;
        boolean zAppendPrefix = this.data.appendPrefix(i15, i16, stringBuffer);
        boolean z5 = i13 != length;
        boolean z6 = this.customs.separatorVariant != 0;
        boolean z7 = true;
        int i17 = i13;
        boolean zAppendUnitSeparator = zAppendPrefix;
        int i18 = 0;
        while (i17 <= length) {
            if (i18 != 0) {
                this.data.appendSkippedUnit(stringBuffer);
                z4 = i2;
                i4 = i17;
                i3 = 0;
            } else {
                i3 = i18;
                z4 = z7;
                i4 = i17;
            }
            while (true) {
                i5 = i4 + 1;
                if (i5 < length && ((i2 << i5) & i8) == 0) {
                    i3 = i2;
                    i4 = i5;
                }
            }
            TimeUnit timeUnit = TimeUnit.units[i17];
            int i19 = iArr2[i17] - 1;
            int i20 = this.customs.countVariant;
            if (i17 == length) {
                if (z2) {
                    i20 = 5;
                }
                i6 = i20;
            } else {
                i6 = 0;
            }
            int i21 = i17 == length ? i2 : 0;
            int i22 = i17;
            byte b4 = b3;
            int i23 = i15;
            StringBuffer stringBuffer2 = stringBuffer;
            int i24 = i3 | (this.data.appendUnit(timeUnit, i19, i6, this.customs.unitVariant, z6, zAppendUnitSeparator, z5, i21, z4, stringBuffer2) ? 1 : 0);
            if (this.customs.separatorVariant == 0 || i5 > length) {
                zAppendUnitSeparator = false;
            } else {
                zAppendUnitSeparator = this.data.appendUnitSeparator(timeUnit, this.customs.separatorVariant == b4, i22 == i13, i5 == length, stringBuffer2);
            }
            b3 = b4;
            i17 = i5;
            i18 = i24;
            i15 = i23;
            stringBuffer = stringBuffer2;
            iArr2 = iArr;
            i2 = 1;
            z7 = false;
        }
        StringBuffer stringBuffer3 = stringBuffer;
        this.data.appendSuffix(i15, i16, stringBuffer3);
        return stringBuffer3.toString();
    }
}
