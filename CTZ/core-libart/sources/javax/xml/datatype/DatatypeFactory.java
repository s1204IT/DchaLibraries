package javax.xml.datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import javax.xml.datatype.FactoryFinder;
import libcore.icu.RelativeDateTimeFormatter;

public abstract class DatatypeFactory {
    public static final String DATATYPEFACTORY_IMPLEMENTATION_CLASS = new String("org.apache.xerces.jaxp.datatype.DatatypeFactoryImpl");
    public static final String DATATYPEFACTORY_PROPERTY = "javax.xml.datatype.DatatypeFactory";

    public abstract Duration newDuration(long j);

    public abstract Duration newDuration(String str);

    public abstract Duration newDuration(boolean z, BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3, BigInteger bigInteger4, BigInteger bigInteger5, BigDecimal bigDecimal);

    public abstract XMLGregorianCalendar newXMLGregorianCalendar();

    public abstract XMLGregorianCalendar newXMLGregorianCalendar(String str);

    public abstract XMLGregorianCalendar newXMLGregorianCalendar(BigInteger bigInteger, int i, int i2, int i3, int i4, int i5, BigDecimal bigDecimal, int i6);

    public abstract XMLGregorianCalendar newXMLGregorianCalendar(GregorianCalendar gregorianCalendar);

    protected DatatypeFactory() {
    }

    public static DatatypeFactory newInstance() throws DatatypeConfigurationException {
        try {
            return (DatatypeFactory) FactoryFinder.find(DATATYPEFACTORY_PROPERTY, DATATYPEFACTORY_IMPLEMENTATION_CLASS);
        } catch (FactoryFinder.ConfigurationError e) {
            throw new DatatypeConfigurationException(e.getMessage(), e.getException());
        }
    }

    public static DatatypeFactory newInstance(String str, ClassLoader classLoader) throws DatatypeConfigurationException {
        Class<?> cls;
        if (str == null) {
            throw new DatatypeConfigurationException("factoryClassName == null");
        }
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        try {
            if (classLoader != null) {
                cls = classLoader.loadClass(str);
            } else {
                cls = Class.forName(str);
            }
            return (DatatypeFactory) cls.newInstance();
        } catch (ClassNotFoundException e) {
            throw new DatatypeConfigurationException(e);
        } catch (IllegalAccessException e2) {
            throw new DatatypeConfigurationException(e2);
        } catch (InstantiationException e3) {
            throw new DatatypeConfigurationException(e3);
        }
    }

    public Duration newDuration(boolean z, int i, int i2, int i3, int i4, int i5, int i6) {
        return newDuration(z, i != Integer.MIN_VALUE ? BigInteger.valueOf(i) : null, i2 != Integer.MIN_VALUE ? BigInteger.valueOf(i2) : null, i3 != Integer.MIN_VALUE ? BigInteger.valueOf(i3) : null, i4 != Integer.MIN_VALUE ? BigInteger.valueOf(i4) : null, i5 != Integer.MIN_VALUE ? BigInteger.valueOf(i5) : null, i6 != Integer.MIN_VALUE ? BigDecimal.valueOf(i6) : null);
    }

    public Duration newDurationDayTime(String str) {
        if (str == null) {
            throw new NullPointerException("lexicalRepresentation == null");
        }
        int iIndexOf = str.indexOf(84);
        if (iIndexOf < 0) {
            iIndexOf = str.length();
        }
        for (int i = 0; i < iIndexOf; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == 'Y' || cCharAt == 'M') {
                throw new IllegalArgumentException("Invalid dayTimeDuration value: " + str);
            }
        }
        return newDuration(str);
    }

    public Duration newDurationDayTime(long j) {
        boolean z;
        if (j == 0) {
            return newDuration(true, Integer.MIN_VALUE, Integer.MIN_VALUE, 0, 0, 0, 0);
        }
        boolean z2 = false;
        boolean z3 = true;
        if (j < 0) {
            if (j == Long.MIN_VALUE) {
                j++;
            } else {
                z3 = false;
            }
            j *= -1;
            z = false;
            z2 = z3;
        } else {
            z = true;
        }
        int i = (int) (j % RelativeDateTimeFormatter.MINUTE_IN_MILLIS);
        if (z2) {
            i++;
        }
        if (i % 1000 == 0) {
            int i2 = i / 1000;
            long j2 = j / RelativeDateTimeFormatter.MINUTE_IN_MILLIS;
            int i3 = (int) (j2 % 60);
            long j3 = j2 / 60;
            int i4 = (int) (j3 % 24);
            long j4 = j3 / 24;
            if (j4 <= 2147483647L) {
                return newDuration(z, Integer.MIN_VALUE, Integer.MIN_VALUE, (int) j4, i4, i3, i2);
            }
            return newDuration(z, (BigInteger) null, (BigInteger) null, BigInteger.valueOf(j4), BigInteger.valueOf(i4), BigInteger.valueOf(i3), BigDecimal.valueOf(i, 3));
        }
        BigDecimal bigDecimalValueOf = BigDecimal.valueOf(i, 3);
        long j5 = j / RelativeDateTimeFormatter.MINUTE_IN_MILLIS;
        BigInteger bigIntegerValueOf = BigInteger.valueOf(j5 % 60);
        long j6 = j5 / 60;
        return newDuration(z, (BigInteger) null, (BigInteger) null, BigInteger.valueOf(j6 / 24), BigInteger.valueOf(j6 % 24), bigIntegerValueOf, bigDecimalValueOf);
    }

    public Duration newDurationDayTime(boolean z, BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3, BigInteger bigInteger4) {
        return newDuration(z, (BigInteger) null, (BigInteger) null, bigInteger, bigInteger2, bigInteger3, bigInteger4 != null ? new BigDecimal(bigInteger4) : null);
    }

    public Duration newDurationDayTime(boolean z, int i, int i2, int i3, int i4) {
        return newDuration(z, Integer.MIN_VALUE, Integer.MIN_VALUE, i, i2, i3, i4);
    }

    public Duration newDurationYearMonth(String str) {
        if (str == null) {
            throw new NullPointerException("lexicalRepresentation == null");
        }
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == 'D' || cCharAt == 'T') {
                throw new IllegalArgumentException("Invalid yearMonthDuration value: " + str);
            }
        }
        return newDuration(str);
    }

    public Duration newDurationYearMonth(long j) {
        return newDuration(j);
    }

    public Duration newDurationYearMonth(boolean z, BigInteger bigInteger, BigInteger bigInteger2) {
        return newDuration(z, bigInteger, bigInteger2, (BigInteger) null, (BigInteger) null, (BigInteger) null, (BigDecimal) null);
    }

    public Duration newDurationYearMonth(boolean z, int i, int i2) {
        return newDuration(z, i, i2, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    public XMLGregorianCalendar newXMLGregorianCalendar(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        BigDecimal bigDecimalValueOf = null;
        BigInteger bigIntegerValueOf = i != Integer.MIN_VALUE ? BigInteger.valueOf(i) : null;
        if (i7 != Integer.MIN_VALUE) {
            if (i7 < 0 || i7 > 1000) {
                throw new IllegalArgumentException("javax.xml.datatype.DatatypeFactory#newXMLGregorianCalendar(int year, int month, int day, int hour, int minute, int second, int millisecond, int timezone)with invalid millisecond: " + i7);
            }
            bigDecimalValueOf = BigDecimal.valueOf(i7, 3);
        }
        return newXMLGregorianCalendar(bigIntegerValueOf, i2, i3, i4, i5, i6, bigDecimalValueOf, i8);
    }

    public XMLGregorianCalendar newXMLGregorianCalendarDate(int i, int i2, int i3, int i4) {
        return newXMLGregorianCalendar(i, i2, i3, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, i4);
    }

    public XMLGregorianCalendar newXMLGregorianCalendarTime(int i, int i2, int i3, int i4) {
        return newXMLGregorianCalendar(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, i, i2, i3, Integer.MIN_VALUE, i4);
    }

    public XMLGregorianCalendar newXMLGregorianCalendarTime(int i, int i2, int i3, BigDecimal bigDecimal, int i4) {
        return newXMLGregorianCalendar((BigInteger) null, Integer.MIN_VALUE, Integer.MIN_VALUE, i, i2, i3, bigDecimal, i4);
    }

    public XMLGregorianCalendar newXMLGregorianCalendarTime(int i, int i2, int i3, int i4, int i5) {
        BigDecimal bigDecimalValueOf;
        if (i4 != Integer.MIN_VALUE) {
            if (i4 < 0 || i4 > 1000) {
                throw new IllegalArgumentException("javax.xml.datatype.DatatypeFactory#newXMLGregorianCalendarTime(int hours, int minutes, int seconds, int milliseconds, int timezone)with invalid milliseconds: " + i4);
            }
            bigDecimalValueOf = BigDecimal.valueOf(i4, 3);
        } else {
            bigDecimalValueOf = null;
        }
        return newXMLGregorianCalendarTime(i, i2, i3, bigDecimalValueOf, i5);
    }
}
