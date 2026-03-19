package sun.util.calendar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.TimeZone;
import sun.util.calendar.BaseCalendar;

public class LocalGregorianCalendar extends BaseCalendar {
    private Era[] eras;
    private String name;

    public static class Date extends BaseCalendar.Date {
        private int gregorianYear;

        protected Date() {
            this.gregorianYear = Integer.MIN_VALUE;
        }

        protected Date(TimeZone timeZone) {
            super(timeZone);
            this.gregorianYear = Integer.MIN_VALUE;
        }

        @Override
        public Date setEra(Era era) {
            if (getEra() != era) {
                super.setEra(era);
                this.gregorianYear = Integer.MIN_VALUE;
            }
            return this;
        }

        @Override
        public Date addYear(int i) {
            super.addYear(i);
            this.gregorianYear += i;
            return this;
        }

        @Override
        public Date setYear(int i) {
            if (getYear() != i) {
                super.setYear(i);
                this.gregorianYear = Integer.MIN_VALUE;
            }
            return this;
        }

        @Override
        public int getNormalizedYear() {
            return this.gregorianYear;
        }

        @Override
        public void setNormalizedYear(int i) {
            this.gregorianYear = i;
        }

        void setLocalEra(Era era) {
            super.setEra(era);
        }

        void setLocalYear(int i) {
            super.setYear(i);
        }

        @Override
        public String toString() {
            String abbreviation;
            String string = super.toString();
            String strSubstring = string.substring(string.indexOf(84));
            StringBuffer stringBuffer = new StringBuffer();
            Era era = getEra();
            if (era != null && (abbreviation = era.getAbbreviation()) != null) {
                stringBuffer.append(abbreviation);
            }
            stringBuffer.append(getYear());
            stringBuffer.append('.');
            CalendarUtils.sprintf0d(stringBuffer, getMonth(), 2).append('.');
            CalendarUtils.sprintf0d(stringBuffer, getDayOfMonth(), 2);
            stringBuffer.append(strSubstring);
            return stringBuffer.toString();
        }
    }

    static LocalGregorianCalendar getLocalGregorianCalendar(String str) {
        try {
            String property = CalendarSystem.getCalendarProperties().getProperty("calendar." + str + ".eras");
            if (property == null) {
                return null;
            }
            ArrayList arrayList = new ArrayList();
            StringTokenizer stringTokenizer = new StringTokenizer(property, ";");
            while (stringTokenizer.hasMoreTokens()) {
                StringTokenizer stringTokenizer2 = new StringTokenizer(stringTokenizer.nextToken().trim(), ",");
                boolean z = true;
                String str2 = null;
                String str3 = null;
                long j = 0;
                while (stringTokenizer2.hasMoreTokens()) {
                    String strNextToken = stringTokenizer2.nextToken();
                    int iIndexOf = strNextToken.indexOf(61);
                    if (iIndexOf == -1) {
                        return null;
                    }
                    String strSubstring = strNextToken.substring(0, iIndexOf);
                    String strSubstring2 = strNextToken.substring(iIndexOf + 1);
                    if ("name".equals(strSubstring)) {
                        str2 = strSubstring2;
                    } else if ("since".equals(strSubstring)) {
                        if (strSubstring2.endsWith("u")) {
                            j = Long.parseLong(strSubstring2.substring(0, strSubstring2.length() - 1));
                            z = false;
                        } else {
                            j = Long.parseLong(strSubstring2);
                        }
                    } else {
                        if (!"abbr".equals(strSubstring)) {
                            throw new RuntimeException("Unknown key word: " + strSubstring);
                        }
                        str3 = strSubstring2;
                    }
                }
                arrayList.add(new Era(str2, str3, j, z));
            }
            if (!arrayList.isEmpty()) {
                Era[] eraArr = new Era[arrayList.size()];
                arrayList.toArray(eraArr);
                return new LocalGregorianCalendar(str, eraArr);
            }
            throw new RuntimeException("No eras for " + str);
        } catch (IOException | IllegalArgumentException e) {
            throw new InternalError(e);
        }
    }

    private LocalGregorianCalendar(String str, Era[] eraArr) {
        this.name = str;
        this.eras = eraArr;
        setEras(eraArr);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Date getCalendarDate() {
        return getCalendarDate(System.currentTimeMillis(), (CalendarDate) newCalendarDate());
    }

    @Override
    public Date getCalendarDate(long j) {
        return getCalendarDate(j, (CalendarDate) newCalendarDate());
    }

    @Override
    public Date getCalendarDate(long j, TimeZone timeZone) {
        return getCalendarDate(j, (CalendarDate) newCalendarDate(timeZone));
    }

    @Override
    public Date getCalendarDate(long j, CalendarDate calendarDate) {
        Date date = (Date) super.getCalendarDate(j, calendarDate);
        return adjustYear(date, j, date.getZoneOffset());
    }

    private Date adjustYear(Date date, long j, int i) {
        int length = this.eras.length - 1;
        while (true) {
            if (length < 0) {
                break;
            }
            Era era = this.eras[length];
            long since = era.getSince(null);
            if (era.isLocalTime()) {
                since -= (long) i;
            }
            if (j < since) {
                length--;
            } else {
                date.setLocalEra(era);
                date.setLocalYear((date.getNormalizedYear() - era.getSinceDate().getYear()) + 1);
                break;
            }
        }
        if (length < 0) {
            date.setLocalEra(null);
            date.setLocalYear(date.getNormalizedYear());
        }
        date.setNormalized(true);
        return date;
    }

    @Override
    public Date newCalendarDate() {
        return new Date();
    }

    @Override
    public Date newCalendarDate(TimeZone timeZone) {
        return new Date(timeZone);
    }

    @Override
    public boolean validate(CalendarDate calendarDate) {
        Date date = (Date) calendarDate;
        Era era = date.getEra();
        if (era == null) {
            if (calendarDate.getYear() >= this.eras[0].getSinceDate().getYear()) {
                return false;
            }
            date.setNormalizedYear(date.getYear());
        } else {
            if (!validateEra(era)) {
                return false;
            }
            date.setNormalizedYear((era.getSinceDate().getYear() + date.getYear()) - 1);
            Date dateNewCalendarDate = newCalendarDate(calendarDate.getZone());
            dateNewCalendarDate.setEra(era).setDate(calendarDate.getYear(), calendarDate.getMonth(), calendarDate.getDayOfMonth());
            normalize(dateNewCalendarDate);
            if (dateNewCalendarDate.getEra() != era) {
                return false;
            }
        }
        return super.validate(date);
    }

    private boolean validateEra(Era era) {
        for (int i = 0; i < this.eras.length; i++) {
            if (era == this.eras[i]) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean normalize(CalendarDate calendarDate) {
        if (calendarDate.isNormalized()) {
            return true;
        }
        normalizeYear(calendarDate);
        Date date = (Date) calendarDate;
        super.normalize(date);
        boolean z = false;
        long time = 0;
        int normalizedYear = date.getNormalizedYear();
        int length = this.eras.length - 1;
        Era era = null;
        while (true) {
            if (length < 0) {
                break;
            }
            era = this.eras[length];
            if (era.isLocalTime()) {
                CalendarDate sinceDate = era.getSinceDate();
                int year = sinceDate.getYear();
                if (normalizedYear > year) {
                    break;
                }
                if (normalizedYear == year) {
                    int month = date.getMonth();
                    int month2 = sinceDate.getMonth();
                    if (month > month2) {
                        break;
                    }
                    if (month == month2) {
                        int dayOfMonth = date.getDayOfMonth();
                        int dayOfMonth2 = sinceDate.getDayOfMonth();
                        if (dayOfMonth > dayOfMonth2) {
                            break;
                        }
                        if (dayOfMonth == dayOfMonth2) {
                            if (date.getTimeOfDay() < sinceDate.getTimeOfDay()) {
                                length--;
                            }
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
                length--;
            } else {
                if (!z) {
                    time = super.getTime(calendarDate);
                    z = true;
                }
                if (time >= era.getSince(calendarDate.getZone())) {
                    break;
                }
                length--;
            }
        }
        if (length >= 0) {
            date.setLocalEra(era);
            date.setLocalYear((date.getNormalizedYear() - era.getSinceDate().getYear()) + 1);
        } else {
            date.setEra((Era) null);
            date.setLocalYear(normalizedYear);
            date.setNormalizedYear(normalizedYear);
        }
        date.setNormalized(true);
        return true;
    }

    @Override
    void normalizeMonth(CalendarDate calendarDate) {
        normalizeYear(calendarDate);
        super.normalizeMonth(calendarDate);
    }

    void normalizeYear(CalendarDate calendarDate) {
        Date date = (Date) calendarDate;
        Era era = date.getEra();
        if (era == null || !validateEra(era)) {
            date.setNormalizedYear(date.getYear());
        } else {
            date.setNormalizedYear((era.getSinceDate().getYear() + date.getYear()) - 1);
        }
    }

    @Override
    public boolean isLeapYear(int i) {
        return CalendarUtils.isGregorianLeapYear(i);
    }

    public boolean isLeapYear(Era era, int i) {
        if (era == null) {
            return isLeapYear(i);
        }
        return isLeapYear((era.getSinceDate().getYear() + i) - 1);
    }

    @Override
    public void getCalendarDateFromFixedDate(CalendarDate calendarDate, long j) {
        Date date = (Date) calendarDate;
        super.getCalendarDateFromFixedDate(date, j);
        adjustYear(date, (j - 719163) * 86400000, 0);
    }
}
