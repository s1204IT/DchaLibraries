package mf.org.apache.xerces.impl.dv.xs;

import java.math.BigDecimal;
import java.math.BigInteger;
import mf.javax.xml.datatype.Duration;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.xs.AbstractDateTimeDV;

class YearMonthDurationDV extends DurationDV {
    YearMonthDurationDV() {
    }

    @Override
    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        try {
            return parse(content, 1);
        } catch (Exception e) {
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, "yearMonthDuration"});
        }
    }

    @Override
    protected Duration getDuration(AbstractDateTimeDV.DateTimeData date) {
        int sign = 1;
        if (date.year < 0 || date.month < 0) {
            sign = -1;
        }
        return datatypeFactory.newDuration(sign == 1, date.year != Integer.MIN_VALUE ? BigInteger.valueOf(date.year * sign) : null, date.month != Integer.MIN_VALUE ? BigInteger.valueOf(date.month * sign) : null, (BigInteger) null, (BigInteger) null, (BigInteger) null, (BigDecimal) null);
    }
}
