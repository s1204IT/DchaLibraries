package android.icu.number;

import android.icu.impl.Utility;
import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.DecimalQuantity_DualStorageBCD;
import android.icu.impl.number.MacroProps;
import android.icu.impl.number.MicroProps;
import android.icu.impl.number.NumberStringBuilder;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

public class LocalizedNumberFormatter extends NumberFormatterSettings<LocalizedNumberFormatter> {
    static final AtomicLongFieldUpdater<LocalizedNumberFormatter> callCount = AtomicLongFieldUpdater.newUpdater(LocalizedNumberFormatter.class, "callCountInternal");
    volatile long callCountInternal;
    volatile NumberFormatterImpl compiled;
    volatile LocalizedNumberFormatter savedWithUnit;

    LocalizedNumberFormatter(NumberFormatterSettings<?> numberFormatterSettings, int i, Object obj) {
        super(numberFormatterSettings, i, obj);
    }

    public FormattedNumber format(long j) {
        return format(new DecimalQuantity_DualStorageBCD(j));
    }

    public FormattedNumber format(double d) {
        return format(new DecimalQuantity_DualStorageBCD(d));
    }

    public FormattedNumber format(Number number) {
        return format(new DecimalQuantity_DualStorageBCD(number));
    }

    public FormattedNumber format(Measure measure) {
        MeasureUnit unit = measure.getUnit();
        Number number = measure.getNumber();
        if (Utility.equals(resolve().unit, unit)) {
            return format(number);
        }
        LocalizedNumberFormatter localizedNumberFormatter = this.savedWithUnit;
        if (localizedNumberFormatter == null || !Utility.equals(localizedNumberFormatter.resolve().unit, unit)) {
            localizedNumberFormatter = new LocalizedNumberFormatter(this, 3, unit);
            this.savedWithUnit = localizedNumberFormatter;
        }
        return localizedNumberFormatter.format(number);
    }

    @Deprecated
    public FormattedNumber format(DecimalQuantity decimalQuantity) {
        MicroProps microPropsApplyStatic;
        MacroProps macroPropsResolve = resolve();
        long jIncrementAndGet = callCount.incrementAndGet(this);
        NumberStringBuilder numberStringBuilder = new NumberStringBuilder();
        if (jIncrementAndGet == macroPropsResolve.threshold.longValue()) {
            this.compiled = NumberFormatterImpl.fromMacros(macroPropsResolve);
            microPropsApplyStatic = this.compiled.apply(decimalQuantity, numberStringBuilder);
        } else if (this.compiled != null) {
            microPropsApplyStatic = this.compiled.apply(decimalQuantity, numberStringBuilder);
        } else {
            microPropsApplyStatic = NumberFormatterImpl.applyStatic(macroPropsResolve, decimalQuantity, numberStringBuilder);
        }
        return new FormattedNumber(numberStringBuilder, decimalQuantity, microPropsApplyStatic);
    }

    @Override
    LocalizedNumberFormatter create(int i, Object obj) {
        return new LocalizedNumberFormatter(this, i, obj);
    }
}
