package gov.nist.javax.sip.header;

import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import javax.sip.header.WarningHeader;

public class SIPDate implements Cloneable, Serializable {
    public static final String APR = "Apr";
    public static final String AUG = "Aug";
    public static final String DEC = "Dec";
    public static final String FEB = "Feb";
    public static final String FRI = "Fri";
    public static final String GMT = "GMT";
    public static final String JAN = "Jan";
    public static final String JUL = "Jul";
    public static final String JUN = "Jun";
    public static final String MAR = "Mar";
    public static final String MAY = "May";
    public static final String MON = "Mon";
    public static final String NOV = "Nov";
    public static final String OCT = "Oct";
    public static final String SAT = "Sat";
    public static final String SEP = "Sep";
    public static final String SUN = "Sun";
    public static final String THU = "Thu";
    public static final String TUE = "Tue";
    public static final String WED = "Wed";
    private static final long serialVersionUID = 8544101899928346909L;
    protected int day;
    protected int hour;
    private Calendar javaCal;
    protected int minute;
    protected int month;
    protected int second;
    protected String sipMonth;
    protected String sipWkDay;
    protected int wkday;
    protected int year;

    public boolean equals(Object obj) {
        if (obj.getClass() != getClass()) {
            return false;
        }
        SIPDate sIPDate = (SIPDate) obj;
        return this.wkday == sIPDate.wkday && this.day == sIPDate.day && this.month == sIPDate.month && this.year == sIPDate.year && this.hour == sIPDate.hour && this.minute == sIPDate.minute && this.second == sIPDate.second;
    }

    public SIPDate() {
        this.wkday = -1;
        this.day = -1;
        this.month = -1;
        this.year = -1;
        this.hour = -1;
        this.minute = -1;
        this.second = -1;
        this.javaCal = null;
    }

    public SIPDate(long j) {
        this.javaCal = new GregorianCalendar(TimeZone.getTimeZone("GMT:0"), Locale.getDefault());
        this.javaCal.setTime(new Date(j));
        this.wkday = this.javaCal.get(7);
        switch (this.wkday) {
            case 1:
                this.sipWkDay = "Sun";
                break;
            case 2:
                this.sipWkDay = "Mon";
                break;
            case 3:
                this.sipWkDay = "Tue";
                break;
            case 4:
                this.sipWkDay = "Wed";
                break;
            case 5:
                this.sipWkDay = "Thu";
                break;
            case 6:
                this.sipWkDay = "Fri";
                break;
            case 7:
                this.sipWkDay = "Sat";
                break;
            default:
                InternalErrorHandler.handleException("No date map for wkday " + this.wkday);
                break;
        }
        this.day = this.javaCal.get(5);
        this.month = this.javaCal.get(2);
        switch (this.month) {
            case 0:
                this.sipMonth = "Jan";
                break;
            case 1:
                this.sipMonth = "Feb";
                break;
            case 2:
                this.sipMonth = "Mar";
                break;
            case 3:
                this.sipMonth = "Apr";
                break;
            case 4:
                this.sipMonth = "May";
                break;
            case 5:
                this.sipMonth = "Jun";
                break;
            case 6:
                this.sipMonth = "Jul";
                break;
            case 7:
                this.sipMonth = "Aug";
                break;
            case 8:
                this.sipMonth = "Sep";
                break;
            case 9:
                this.sipMonth = "Oct";
                break;
            case WarningHeader.ATTRIBUTE_NOT_UNDERSTOOD:
                this.sipMonth = "Nov";
                break;
            case 11:
                this.sipMonth = "Dec";
                break;
            default:
                InternalErrorHandler.handleException("No date map for month " + this.month);
                break;
        }
        this.year = this.javaCal.get(1);
        this.hour = this.javaCal.get(11);
        this.minute = this.javaCal.get(12);
        this.second = this.javaCal.get(13);
    }

    public String encode() {
        String str;
        String str2;
        String str3;
        String str4;
        if (this.day < 10) {
            str = "0" + this.day;
        } else {
            str = "" + this.day;
        }
        if (this.hour < 10) {
            str2 = "0" + this.hour;
        } else {
            str2 = "" + this.hour;
        }
        if (this.minute < 10) {
            str3 = "0" + this.minute;
        } else {
            str3 = "" + this.minute;
        }
        if (this.second < 10) {
            str4 = "0" + this.second;
        } else {
            str4 = "" + this.second;
        }
        String str5 = "";
        if (this.sipWkDay != null) {
            str5 = "" + this.sipWkDay + Separators.COMMA + Separators.SP;
        }
        String str6 = str5 + str + Separators.SP;
        if (this.sipMonth != null) {
            str6 = str6 + this.sipMonth + Separators.SP;
        }
        return str6 + this.year + Separators.SP + str2 + Separators.COLON + str3 + Separators.COLON + str4 + Separators.SP + "GMT";
    }

    public Calendar getJavaCal() {
        if (this.javaCal == null) {
            setJavaCal();
        }
        return this.javaCal;
    }

    public String getWkday() {
        return this.sipWkDay;
    }

    public String getMonth() {
        return this.sipMonth;
    }

    public int getHour() {
        return this.hour;
    }

    public int getMinute() {
        return this.minute;
    }

    public int getSecond() {
        return this.second;
    }

    private void setJavaCal() {
        this.javaCal = new GregorianCalendar(TimeZone.getTimeZone("GMT:0"), Locale.getDefault());
        if (this.year != -1) {
            this.javaCal.set(1, this.year);
        }
        if (this.day != -1) {
            this.javaCal.set(5, this.day);
        }
        if (this.month != -1) {
            this.javaCal.set(2, this.month);
        }
        if (this.wkday != -1) {
            this.javaCal.set(7, this.wkday);
        }
        if (this.hour != -1) {
            this.javaCal.set(10, this.hour);
        }
        if (this.minute != -1) {
            this.javaCal.set(12, this.minute);
        }
        if (this.second != -1) {
            this.javaCal.set(13, this.second);
        }
    }

    public void setWkday(String str) throws IllegalArgumentException {
        this.sipWkDay = str;
        if (this.sipWkDay.compareToIgnoreCase("Mon") == 0) {
            this.wkday = 2;
            return;
        }
        if (this.sipWkDay.compareToIgnoreCase("Tue") == 0) {
            this.wkday = 3;
            return;
        }
        if (this.sipWkDay.compareToIgnoreCase("Wed") == 0) {
            this.wkday = 4;
            return;
        }
        if (this.sipWkDay.compareToIgnoreCase("Thu") == 0) {
            this.wkday = 5;
            return;
        }
        if (this.sipWkDay.compareToIgnoreCase("Fri") == 0) {
            this.wkday = 6;
            return;
        }
        if (this.sipWkDay.compareToIgnoreCase("Sat") == 0) {
            this.wkday = 7;
        } else {
            if (this.sipWkDay.compareToIgnoreCase("Sun") == 0) {
                this.wkday = 1;
                return;
            }
            throw new IllegalArgumentException("Illegal Week day :" + str);
        }
    }

    public void setDay(int i) throws IllegalArgumentException {
        if (i < 1 || i > 31) {
            throw new IllegalArgumentException("Illegal Day of the month " + Integer.toString(i));
        }
        this.day = i;
    }

    public void setMonth(String str) throws IllegalArgumentException {
        this.sipMonth = str;
        if (this.sipMonth.compareToIgnoreCase("Jan") == 0) {
            this.month = 0;
            return;
        }
        if (this.sipMonth.compareToIgnoreCase("Feb") == 0) {
            this.month = 1;
            return;
        }
        if (this.sipMonth.compareToIgnoreCase("Mar") == 0) {
            this.month = 2;
            return;
        }
        if (this.sipMonth.compareToIgnoreCase("Apr") == 0) {
            this.month = 3;
            return;
        }
        if (this.sipMonth.compareToIgnoreCase("May") == 0) {
            this.month = 4;
            return;
        }
        if (this.sipMonth.compareToIgnoreCase("Jun") == 0) {
            this.month = 5;
            return;
        }
        if (this.sipMonth.compareToIgnoreCase("Jul") == 0) {
            this.month = 6;
            return;
        }
        if (this.sipMonth.compareToIgnoreCase("Aug") == 0) {
            this.month = 7;
            return;
        }
        if (this.sipMonth.compareToIgnoreCase("Sep") == 0) {
            this.month = 8;
            return;
        }
        if (this.sipMonth.compareToIgnoreCase("Oct") == 0) {
            this.month = 9;
            return;
        }
        if (this.sipMonth.compareToIgnoreCase("Nov") == 0) {
            this.month = 10;
        } else {
            if (this.sipMonth.compareToIgnoreCase("Dec") == 0) {
                this.month = 11;
                return;
            }
            throw new IllegalArgumentException("Illegal Month :" + str);
        }
    }

    public void setYear(int i) throws IllegalArgumentException {
        if (i < 0) {
            throw new IllegalArgumentException("Illegal year : " + i);
        }
        this.javaCal = null;
        this.year = i;
    }

    public int getYear() {
        return this.year;
    }

    public void setHour(int i) throws IllegalArgumentException {
        if (i < 0 || i > 24) {
            throw new IllegalArgumentException("Illegal hour : " + i);
        }
        this.javaCal = null;
        this.hour = i;
    }

    public void setMinute(int i) throws IllegalArgumentException {
        if (i < 0 || i >= 60) {
            throw new IllegalArgumentException("Illegal minute : " + Integer.toString(i));
        }
        this.javaCal = null;
        this.minute = i;
    }

    public void setSecond(int i) throws IllegalArgumentException {
        if (i < 0 || i >= 60) {
            throw new IllegalArgumentException("Illegal second : " + Integer.toString(i));
        }
        this.javaCal = null;
        this.second = i;
    }

    public int getDeltaSeconds() {
        return ((int) (getJavaCal().getTime().getTime() - System.currentTimeMillis())) / 1000;
    }

    public Object clone() {
        try {
            SIPDate sIPDate = (SIPDate) super.clone();
            if (this.javaCal != null) {
                sIPDate.javaCal = (Calendar) this.javaCal.clone();
            }
            return sIPDate;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Internal error");
        }
    }
}
