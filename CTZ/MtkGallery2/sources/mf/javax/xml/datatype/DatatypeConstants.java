package mf.javax.xml.datatype;

import mf.javax.xml.namespace.QName;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public final class DatatypeConstants {
    public static final Field DAYS;
    public static final Field HOURS;
    public static final Field MINUTES;
    public static final Field MONTHS;
    public static final Field SECONDS;
    public static final Field YEARS;
    public static final QName DATETIME = new QName("http://www.w3.org/2001/XMLSchema", SchemaSymbols.ATTVAL_DATETIME);
    public static final QName TIME = new QName("http://www.w3.org/2001/XMLSchema", SchemaSymbols.ATTVAL_TIME);
    public static final QName DATE = new QName("http://www.w3.org/2001/XMLSchema", SchemaSymbols.ATTVAL_DATE);
    public static final QName GYEARMONTH = new QName("http://www.w3.org/2001/XMLSchema", SchemaSymbols.ATTVAL_YEARMONTH);
    public static final QName GMONTHDAY = new QName("http://www.w3.org/2001/XMLSchema", SchemaSymbols.ATTVAL_MONTHDAY);
    public static final QName GYEAR = new QName("http://www.w3.org/2001/XMLSchema", SchemaSymbols.ATTVAL_YEAR);
    public static final QName GMONTH = new QName("http://www.w3.org/2001/XMLSchema", SchemaSymbols.ATTVAL_MONTH);
    public static final QName GDAY = new QName("http://www.w3.org/2001/XMLSchema", SchemaSymbols.ATTVAL_DAY);
    public static final QName DURATION = new QName("http://www.w3.org/2001/XMLSchema", SchemaSymbols.ATTVAL_DURATION);
    public static final QName DURATION_DAYTIME = new QName("http://www.w3.org/2003/11/xpath-datatypes", "dayTimeDuration");
    public static final QName DURATION_YEARMONTH = new QName("http://www.w3.org/2003/11/xpath-datatypes", "yearMonthDuration");

    static {
        Field field = null;
        YEARS = new Field("YEARS", 0, field);
        MONTHS = new Field("MONTHS", 1, field);
        DAYS = new Field("DAYS", 2, field);
        HOURS = new Field("HOURS", 3, field);
        MINUTES = new Field("MINUTES", 4, field);
        SECONDS = new Field("SECONDS", 5, field);
    }

    public static final class Field {
        private final int id;
        private final String str;

        private Field(String str, int id) {
            this.str = str;
            this.id = id;
        }

        Field(String str, int i, Field field) {
            this(str, i);
        }

        public String toString() {
            return this.str;
        }
    }
}
