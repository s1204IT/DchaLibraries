package android.icu.number;

import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.MicroProps;
import android.icu.impl.number.NumberStringBuilder;
import android.icu.text.PluralRules;
import android.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.util.Arrays;

public class FormattedNumber {
    DecimalQuantity fq;
    MicroProps micros;
    NumberStringBuilder nsb;

    FormattedNumber(NumberStringBuilder numberStringBuilder, DecimalQuantity decimalQuantity, MicroProps microProps) {
        this.nsb = numberStringBuilder;
        this.fq = decimalQuantity;
        this.micros = microProps;
    }

    public String toString() {
        return this.nsb.toString();
    }

    public <A extends Appendable> A appendTo(A a) {
        try {
            a.append(this.nsb);
            return a;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public void populateFieldPosition(FieldPosition fieldPosition) {
        populateFieldPosition(fieldPosition, 0);
    }

    @Deprecated
    public void populateFieldPosition(FieldPosition fieldPosition, int i) {
        this.nsb.populateFieldPosition(fieldPosition, i);
        this.fq.populateUFieldPosition(fieldPosition);
    }

    public AttributedCharacterIterator getFieldIterator() {
        return this.nsb.getIterator();
    }

    public BigDecimal toBigDecimal() {
        return this.fq.toBigDecimal();
    }

    @Deprecated
    public String getPrefix() {
        NumberStringBuilder numberStringBuilder = new NumberStringBuilder();
        int iApply = this.micros.modOuter.apply(numberStringBuilder, 0, 0);
        this.micros.modInner.apply(numberStringBuilder, 0, iApply + this.micros.modMiddle.apply(numberStringBuilder, 0, iApply));
        return numberStringBuilder.subSequence(0, this.micros.modOuter.getPrefixLength() + this.micros.modMiddle.getPrefixLength() + this.micros.modInner.getPrefixLength()).toString();
    }

    @Deprecated
    public String getSuffix() {
        NumberStringBuilder numberStringBuilder = new NumberStringBuilder();
        int iApply = this.micros.modOuter.apply(numberStringBuilder, 0, 0);
        int iApply2 = iApply + this.micros.modMiddle.apply(numberStringBuilder, 0, iApply);
        return numberStringBuilder.subSequence(this.micros.modOuter.getPrefixLength() + this.micros.modMiddle.getPrefixLength() + this.micros.modInner.getPrefixLength(), iApply2 + this.micros.modInner.apply(numberStringBuilder, 0, iApply2)).toString();
    }

    @Deprecated
    public PluralRules.IFixedDecimal getFixedDecimal() {
        return this.fq;
    }

    public int hashCode() {
        return (Arrays.hashCode(this.nsb.toCharArray()) ^ Arrays.hashCode(this.nsb.toFieldArray())) ^ this.fq.toBigDecimal().hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof FormattedNumber)) {
            return false;
        }
        FormattedNumber formattedNumber = (FormattedNumber) obj;
        return this.fq.toBigDecimal().equals(formattedNumber.fq.toBigDecimal()) ^ (Arrays.equals(this.nsb.toCharArray(), formattedNumber.nsb.toCharArray()) ^ Arrays.equals(this.nsb.toFieldArray(), formattedNumber.nsb.toFieldArray()));
    }
}
