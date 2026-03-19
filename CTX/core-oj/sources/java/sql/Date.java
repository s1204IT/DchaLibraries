package java.sql;

public class Date extends java.util.Date {
    static final long serialVersionUID = 1511598038487230103L;

    @Deprecated
    public Date(int i, int i2, int i3) {
        super(i, i2, i3);
    }

    public Date(long j) {
        super(j);
    }

    @Override
    public void setTime(long j) {
        super.setTime(j);
    }

    public static Date valueOf(String str) {
        Date date;
        if (str == null) {
            throw new IllegalArgumentException();
        }
        int iIndexOf = str.indexOf(45);
        int i = iIndexOf + 1;
        int iIndexOf2 = str.indexOf(45, i);
        if (iIndexOf > 0 && iIndexOf2 > 0 && iIndexOf2 < str.length() - 1) {
            String strSubstring = str.substring(0, iIndexOf);
            String strSubstring2 = str.substring(i, iIndexOf2);
            String strSubstring3 = str.substring(iIndexOf2 + 1);
            if (strSubstring.length() == 4 && strSubstring2.length() >= 1 && strSubstring2.length() <= 2 && strSubstring3.length() >= 1 && strSubstring3.length() <= 2) {
                int i2 = Integer.parseInt(strSubstring);
                int i3 = Integer.parseInt(strSubstring2);
                int i4 = Integer.parseInt(strSubstring3);
                if (i3 >= 1 && i3 <= 12 && i4 >= 1 && i4 <= 31) {
                    date = new Date(i2 - 1900, i3 - 1, i4);
                }
            }
        } else {
            date = null;
        }
        if (date == null) {
            throw new IllegalArgumentException();
        }
        return date;
    }

    @Override
    public String toString() {
        int year = super.getYear() + 1900;
        int month = super.getMonth() + 1;
        int date = super.getDate();
        char[] charArray = "2000-00-00".toCharArray();
        charArray[0] = Character.forDigit(year / 1000, 10);
        charArray[1] = Character.forDigit((year / 100) % 10, 10);
        charArray[2] = Character.forDigit((year / 10) % 10, 10);
        charArray[3] = Character.forDigit(year % 10, 10);
        charArray[5] = Character.forDigit(month / 10, 10);
        charArray[6] = Character.forDigit(month % 10, 10);
        charArray[8] = Character.forDigit(date / 10, 10);
        charArray[9] = Character.forDigit(date % 10, 10);
        return new String(charArray);
    }

    @Override
    @Deprecated
    public int getHours() {
        throw new IllegalArgumentException();
    }

    @Override
    @Deprecated
    public int getMinutes() {
        throw new IllegalArgumentException();
    }

    @Override
    @Deprecated
    public int getSeconds() {
        throw new IllegalArgumentException();
    }

    @Override
    @Deprecated
    public void setHours(int i) {
        throw new IllegalArgumentException();
    }

    @Override
    @Deprecated
    public void setMinutes(int i) {
        throw new IllegalArgumentException();
    }

    @Override
    @Deprecated
    public void setSeconds(int i) {
        throw new IllegalArgumentException();
    }
}
