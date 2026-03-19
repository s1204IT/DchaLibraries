package mf.org.apache.xerces.jaxp.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import mf.javax.xml.datatype.DatatypeFactory;
import mf.javax.xml.datatype.Duration;
import mf.javax.xml.datatype.XMLGregorianCalendar;

public class DatatypeFactoryImpl extends DatatypeFactory {
    @Override
    public Duration newDuration(String lexicalRepresentation) {
        return new DurationImpl(lexicalRepresentation);
    }

    @Override
    public Duration newDuration(long durationInMilliseconds) {
        return new DurationImpl(durationInMilliseconds);
    }

    @Override
    public Duration newDuration(boolean isPositive, BigInteger years, BigInteger months, BigInteger days, BigInteger hours, BigInteger minutes, BigDecimal seconds) {
        return new DurationImpl(isPositive, years, months, days, hours, minutes, seconds);
    }

    @Override
    public XMLGregorianCalendar newXMLGregorianCalendar() {
        return new XMLGregorianCalendarImpl();
    }

    @Override
    public XMLGregorianCalendar newXMLGregorianCalendar(String lexicalRepresentation) {
        return new XMLGregorianCalendarImpl(lexicalRepresentation);
    }

    @Override
    public XMLGregorianCalendar newXMLGregorianCalendar(GregorianCalendar cal) {
        return new XMLGregorianCalendarImpl(cal);
    }

    @Override
    public XMLGregorianCalendar newXMLGregorianCalendar(int year, int month, int day, int hour, int minute, int second, int millisecond, int timezone) {
        return XMLGregorianCalendarImpl.createDateTime(year, month, day, hour, minute, second, millisecond, timezone);
    }

    @Override
    public XMLGregorianCalendar newXMLGregorianCalendar(BigInteger year, int month, int day, int hour, int minute, int second, BigDecimal fractionalSecond, int timezone) {
        return new XMLGregorianCalendarImpl(year, month, day, hour, minute, second, fractionalSecond, timezone);
    }
}
