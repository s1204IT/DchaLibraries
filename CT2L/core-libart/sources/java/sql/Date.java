package java.sql;

@FindBugsSuppressWarnings({"NM_SAME_SIMPLE_NAME_AS_SUPERCLASS"})
public class Date extends java.util.Date {
    private static final String PADDING = "0000";
    private static final long serialVersionUID = 1511598038487230103L;

    @Deprecated
    public Date(int theYear, int theMonth, int theDay) {
        super(theYear, theMonth, theDay);
    }

    public Date(long theDate) {
        super(normalizeTime(theDate));
    }

    @Override
    @Deprecated
    public int getHours() {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    @Deprecated
    public int getMinutes() {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    @Deprecated
    public int getSeconds() {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    @Deprecated
    public void setHours(int theHours) {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    @Deprecated
    public void setMinutes(int theMinutes) {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    @Deprecated
    public void setSeconds(int theSeconds) {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    public void setTime(long theTime) {
        super.setTime(normalizeTime(theTime));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(10);
        format(getYear() + 1900, 4, sb);
        sb.append('-');
        format(getMonth() + 1, 2, sb);
        sb.append('-');
        format(getDate(), 2, sb);
        return sb.toString();
    }

    private void format(int date, int digits, StringBuilder sb) {
        String str = String.valueOf(date);
        if (digits - str.length() > 0) {
            sb.append(PADDING.substring(0, digits - str.length()));
        }
        sb.append(str);
    }

    public static Date valueOf(String dateString) {
        if (dateString == null) {
            throw new IllegalArgumentException("dateString == null");
        }
        if (dateString.length() > 10) {
            throw new IllegalArgumentException();
        }
        String[] parts = dateString.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException();
        }
        int year = Integer.parsePositiveInt(parts[0]);
        int month = Integer.parsePositiveInt(parts[1]);
        int day = Integer.parsePositiveInt(parts[2]);
        return new Date(year - 1900, month - 1, day);
    }

    private static long normalizeTime(long theTime) {
        return theTime;
    }
}
