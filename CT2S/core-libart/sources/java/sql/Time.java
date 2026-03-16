package java.sql;

public class Time extends java.util.Date {
    private static final String PADDING = "00";
    private static final long serialVersionUID = 8397324403548013681L;

    @Deprecated
    public Time(int theHour, int theMinute, int theSecond) {
        super(70, 0, 1, theHour, theMinute, theSecond);
    }

    public Time(long theTime) {
        super(theTime);
    }

    @Override
    @Deprecated
    public int getDate() {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    @Deprecated
    public int getDay() {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    @Deprecated
    public int getMonth() {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    @Deprecated
    public int getYear() {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    @Deprecated
    public void setDate(int i) {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    @Deprecated
    public void setMonth(int i) {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    @Deprecated
    public void setYear(int i) {
        throw new IllegalArgumentException("unimplemented");
    }

    @Override
    public void setTime(long time) {
        super.setTime(time);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(8);
        format(getHours(), 2, sb);
        sb.append(':');
        format(getMinutes(), 2, sb);
        sb.append(':');
        format(getSeconds(), 2, sb);
        return sb.toString();
    }

    private void format(int date, int digits, StringBuilder sb) {
        String str = String.valueOf(date);
        if (digits - str.length() > 0) {
            sb.append(PADDING.substring(0, digits - str.length()));
        }
        sb.append(str);
    }

    public static Time valueOf(String timeString) {
        if (timeString == null) {
            throw new IllegalArgumentException("timeString == null");
        }
        int firstIndex = timeString.indexOf(58);
        int secondIndex = timeString.indexOf(58, firstIndex + 1);
        if (secondIndex == -1 || firstIndex == 0 || secondIndex + 1 == timeString.length()) {
            throw new IllegalArgumentException();
        }
        int hour = Integer.parseInt(timeString.substring(0, firstIndex));
        int minute = Integer.parseInt(timeString.substring(firstIndex + 1, secondIndex));
        int second = Integer.parseInt(timeString.substring(secondIndex + 1, timeString.length()));
        return new Time(hour, minute, second);
    }
}
