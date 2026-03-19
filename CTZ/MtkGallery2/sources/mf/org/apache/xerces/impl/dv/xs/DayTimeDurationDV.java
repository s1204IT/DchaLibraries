package mf.org.apache.xerces.impl.dv.xs;

import java.math.BigDecimal;
import java.math.BigInteger;
import mf.javax.xml.datatype.Duration;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.xs.AbstractDateTimeDV;

class DayTimeDurationDV extends DurationDV {
    DayTimeDurationDV() {
    }

    @Override
    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        try {
            return parse(content, 2);
        } catch (Exception e) {
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, "dayTimeDuration"});
        }
    }

    @Override
    protected Duration getDuration(AbstractDateTimeDV.DateTimeData date) {
        int sign = 1;
        if (date.day < 0 || date.hour < 0 || date.minute < 0 || date.second < 0.0d) {
            sign = -1;
        }
        return datatypeFactory.newDuration(sign == 1, (BigInteger) null, (BigInteger) null, date.day != Integer.MIN_VALUE ? BigInteger.valueOf(date.day * sign) : null, date.hour != Integer.MIN_VALUE ? BigInteger.valueOf(date.hour * sign) : null, date.minute != Integer.MIN_VALUE ? BigInteger.valueOf(date.minute * sign) : null, date.second != -2.147483648E9d ? new BigDecimal(String.valueOf(((double) sign) * date.second)) : null);
    }
}
