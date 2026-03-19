package java.sql;

public class Time extends java.util.Date {
    static final long serialVersionUID = 8397324403548013681L;

    @Deprecated
    public Time(int i, int i2, int i3) {
        super(70, 0, 1, i, i2, i3);
    }

    public Time(long j) {
        super(j);
    }

    @Override
    public void setTime(long j) {
        super.setTime(j);
    }

    public static Time valueOf(String str) {
        if (str == null) {
            throw new IllegalArgumentException();
        }
        int iIndexOf = str.indexOf(58);
        int i = iIndexOf + 1;
        int iIndexOf2 = str.indexOf(58, i);
        if (!((iIndexOf > 0) & (iIndexOf2 > 0) & (iIndexOf2 < str.length() - 1))) {
            throw new IllegalArgumentException();
        }
        return new Time(Integer.parseInt(str.substring(0, iIndexOf)), Integer.parseInt(str.substring(i, iIndexOf2)), Integer.parseInt(str.substring(iIndexOf2 + 1)));
    }

    @Override
    public String toString() {
        String string;
        String string2;
        String string3;
        int hours = super.getHours();
        int minutes = super.getMinutes();
        int seconds = super.getSeconds();
        if (hours < 10) {
            string = "0" + hours;
        } else {
            string = Integer.toString(hours);
        }
        if (minutes < 10) {
            string2 = "0" + minutes;
        } else {
            string2 = Integer.toString(minutes);
        }
        if (seconds < 10) {
            string3 = "0" + seconds;
        } else {
            string3 = Integer.toString(seconds);
        }
        return string + ":" + string2 + ":" + string3;
    }

    @Override
    @Deprecated
    public int getYear() {
        throw new IllegalArgumentException();
    }

    @Override
    @Deprecated
    public int getMonth() {
        throw new IllegalArgumentException();
    }

    @Override
    @Deprecated
    public int getDay() {
        throw new IllegalArgumentException();
    }

    @Override
    @Deprecated
    public int getDate() {
        throw new IllegalArgumentException();
    }

    @Override
    @Deprecated
    public void setYear(int i) {
        throw new IllegalArgumentException();
    }

    @Override
    @Deprecated
    public void setMonth(int i) {
        throw new IllegalArgumentException();
    }

    @Override
    @Deprecated
    public void setDate(int i) {
        throw new IllegalArgumentException();
    }
}
