package android.icu.number;

import android.icu.impl.number.MacroProps;
import android.icu.impl.number.Padder;
import android.icu.number.NumberFormatter;
import android.icu.number.NumberFormatterSettings;
import android.icu.text.DecimalFormatSymbols;
import android.icu.text.NumberingSystem;
import android.icu.util.MeasureUnit;
import android.icu.util.ULocale;

public abstract class NumberFormatterSettings<T extends NumberFormatterSettings<?>> {
    static final int KEY_DECIMAL = 11;
    static final int KEY_GROUPER = 5;
    static final int KEY_INTEGER = 7;
    static final int KEY_LOCALE = 1;
    static final int KEY_MACROS = 0;
    static final int KEY_MAX = 13;
    static final int KEY_NOTATION = 2;
    static final int KEY_PADDER = 6;
    static final int KEY_ROUNDER = 4;
    static final int KEY_SIGN = 10;
    static final int KEY_SYMBOLS = 8;
    static final int KEY_THRESHOLD = 12;
    static final int KEY_UNIT = 3;
    static final int KEY_UNIT_WIDTH = 9;
    final int key;
    final NumberFormatterSettings<?> parent;
    volatile MacroProps resolvedMacros;
    final Object value;

    abstract T create(int i, Object obj);

    NumberFormatterSettings(NumberFormatterSettings<?> numberFormatterSettings, int i, Object obj) {
        this.parent = numberFormatterSettings;
        this.key = i;
        this.value = obj;
    }

    public T notation(Notation notation) {
        return (T) create(2, notation);
    }

    public T unit(MeasureUnit measureUnit) {
        return (T) create(3, measureUnit);
    }

    public T rounding(Rounder rounder) {
        return (T) create(4, rounder);
    }

    @Deprecated
    public T grouping(Grouper grouper) {
        return (T) create(5, grouper);
    }

    public T integerWidth(IntegerWidth integerWidth) {
        return (T) create(7, integerWidth);
    }

    public T symbols(DecimalFormatSymbols decimalFormatSymbols) {
        return (T) create(8, (DecimalFormatSymbols) decimalFormatSymbols.clone());
    }

    public T symbols(NumberingSystem numberingSystem) {
        return (T) create(8, numberingSystem);
    }

    public T unitWidth(NumberFormatter.UnitWidth unitWidth) {
        return (T) create(9, unitWidth);
    }

    public T sign(NumberFormatter.SignDisplay signDisplay) {
        return (T) create(10, signDisplay);
    }

    public T decimal(NumberFormatter.DecimalSeparatorDisplay decimalSeparatorDisplay) {
        return (T) create(11, decimalSeparatorDisplay);
    }

    @Deprecated
    public T macros(MacroProps macroProps) {
        return (T) create(0, macroProps);
    }

    @Deprecated
    public T padding(Padder padder) {
        return (T) create(6, padder);
    }

    @Deprecated
    public T threshold(Long l) {
        return (T) create(12, l);
    }

    MacroProps resolve() {
        if (this.resolvedMacros != null) {
            return this.resolvedMacros;
        }
        MacroProps macroProps = new MacroProps();
        for (NumberFormatterSettings numberFormatterSettings = this; numberFormatterSettings != null; numberFormatterSettings = numberFormatterSettings.parent) {
            switch (numberFormatterSettings.key) {
                case 0:
                    macroProps.fallback((MacroProps) numberFormatterSettings.value);
                    break;
                case 1:
                    if (macroProps.loc == null) {
                        macroProps.loc = (ULocale) numberFormatterSettings.value;
                    }
                    break;
                case 2:
                    if (macroProps.notation == null) {
                        macroProps.notation = (Notation) numberFormatterSettings.value;
                    }
                    break;
                case 3:
                    if (macroProps.unit == null) {
                        macroProps.unit = (MeasureUnit) numberFormatterSettings.value;
                    }
                    break;
                case 4:
                    if (macroProps.rounder == null) {
                        macroProps.rounder = (Rounder) numberFormatterSettings.value;
                    }
                    break;
                case 5:
                    if (macroProps.grouper == null) {
                        macroProps.grouper = (Grouper) numberFormatterSettings.value;
                    }
                    break;
                case 6:
                    if (macroProps.padder == null) {
                        macroProps.padder = (Padder) numberFormatterSettings.value;
                    }
                    break;
                case 7:
                    if (macroProps.integerWidth == null) {
                        macroProps.integerWidth = (IntegerWidth) numberFormatterSettings.value;
                    }
                    break;
                case 8:
                    if (macroProps.symbols == null) {
                        macroProps.symbols = numberFormatterSettings.value;
                    }
                    break;
                case 9:
                    if (macroProps.unitWidth == null) {
                        macroProps.unitWidth = (NumberFormatter.UnitWidth) numberFormatterSettings.value;
                    }
                    break;
                case 10:
                    if (macroProps.sign == null) {
                        macroProps.sign = (NumberFormatter.SignDisplay) numberFormatterSettings.value;
                    }
                    break;
                case 11:
                    if (macroProps.decimal == null) {
                        macroProps.decimal = (NumberFormatter.DecimalSeparatorDisplay) numberFormatterSettings.value;
                    }
                    break;
                case 12:
                    if (macroProps.threshold == null) {
                        macroProps.threshold = (Long) numberFormatterSettings.value;
                    }
                    break;
                default:
                    throw new AssertionError("Unknown key: " + numberFormatterSettings.key);
            }
        }
        this.resolvedMacros = macroProps;
        return macroProps;
    }

    public int hashCode() {
        return resolve().hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof NumberFormatterSettings)) {
            return false;
        }
        return resolve().equals(((NumberFormatterSettings) obj).resolve());
    }
}
