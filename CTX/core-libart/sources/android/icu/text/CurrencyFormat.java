package android.icu.text;

import android.icu.text.MeasureFormat;
import android.icu.util.CurrencyAmount;
import android.icu.util.Measure;
import android.icu.util.ULocale;
import java.io.ObjectStreamException;
import java.text.FieldPosition;
import java.text.ParsePosition;

class CurrencyFormat extends MeasureFormat {
    static final long serialVersionUID = -931679363692504634L;
    private NumberFormat fmt;
    private final transient MeasureFormat mf;

    public CurrencyFormat(ULocale uLocale) {
        setLocale(uLocale, uLocale);
        this.mf = MeasureFormat.getInstance(uLocale, MeasureFormat.FormatWidth.WIDE);
        this.fmt = NumberFormat.getCurrencyInstance(uLocale.toLocale());
    }

    @Override
    public Object clone() {
        CurrencyFormat currencyFormat = (CurrencyFormat) super.clone();
        currencyFormat.fmt = (NumberFormat) this.fmt.clone();
        return currencyFormat;
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        if (!(obj instanceof CurrencyAmount)) {
            throw new IllegalArgumentException("Invalid type: " + obj.getClass().getName());
        }
        CurrencyAmount currencyAmount = (CurrencyAmount) obj;
        this.fmt.setCurrency(currencyAmount.getCurrency());
        return this.fmt.format(currencyAmount.getNumber(), stringBuffer, fieldPosition);
    }

    @Override
    public CurrencyAmount parseObject(String str, ParsePosition parsePosition) {
        return this.fmt.parseCurrency(str, parsePosition);
    }

    @Override
    public StringBuilder formatMeasures(StringBuilder sb, FieldPosition fieldPosition, Measure... measureArr) {
        return this.mf.formatMeasures(sb, fieldPosition, measureArr);
    }

    @Override
    public MeasureFormat.FormatWidth getWidth() {
        return this.mf.getWidth();
    }

    @Override
    public NumberFormat getNumberFormat() {
        return this.mf.getNumberFormat();
    }

    private Object writeReplace() throws ObjectStreamException {
        return this.mf.toCurrencyProxy();
    }

    private Object readResolve() throws ObjectStreamException {
        return new CurrencyFormat(this.fmt.getLocale(ULocale.ACTUAL_LOCALE));
    }
}
