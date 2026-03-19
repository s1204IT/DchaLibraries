package java.sql;

import sun.util.locale.LanguageTag;

public class Timestamp extends java.util.Date {
    static final long serialVersionUID = 2745179027874758501L;
    private int nanos;

    @Deprecated
    public Timestamp(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        super(i, i2, i3, i4, i5, i6);
        if (i7 > 999999999 || i7 < 0) {
            throw new IllegalArgumentException("nanos > 999999999 or < 0");
        }
        this.nanos = i7;
    }

    public Timestamp(long j) {
        long j2 = j / 1000;
        super(j2 * 1000);
        this.nanos = (int) ((j % 1000) * 1000000);
        if (this.nanos < 0) {
            this.nanos = 1000000000 + this.nanos;
            super.setTime((j2 - 1) * 1000);
        }
    }

    @Override
    public void setTime(long j) {
        long j2 = j / 1000;
        super.setTime(j2 * 1000);
        this.nanos = (int) ((j % 1000) * 1000000);
        if (this.nanos < 0) {
            this.nanos = 1000000000 + this.nanos;
            super.setTime((j2 - 1) * 1000);
        }
    }

    @Override
    public long getTime() {
        return super.getTime() + ((long) (this.nanos / 1000000));
    }

    public static Timestamp valueOf(String str) {
        boolean z;
        int i;
        int i2;
        int i3;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        int i4;
        if (str == null) {
            throw new IllegalArgumentException("null string");
        }
        String strTrim = str.trim();
        int iIndexOf = strTrim.indexOf(32);
        if (iIndexOf > 0) {
            int i5 = 0;
            String strSubstring = strTrim.substring(0, iIndexOf);
            String strSubstring2 = strTrim.substring(iIndexOf + 1);
            int iIndexOf2 = strSubstring.indexOf(45);
            int i6 = iIndexOf2 + 1;
            int iIndexOf3 = strSubstring.indexOf(45, i6);
            if (strSubstring2 == null) {
                throw new IllegalArgumentException("Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]");
            }
            int iIndexOf4 = strSubstring2.indexOf(58);
            int i7 = iIndexOf4 + 1;
            int iIndexOf5 = strSubstring2.indexOf(58, i7);
            int i8 = iIndexOf5 + 1;
            int iIndexOf6 = strSubstring2.indexOf(46, i8);
            if (iIndexOf2 <= 0 || iIndexOf3 <= 0 || iIndexOf3 >= strSubstring.length() - 1) {
                z = false;
                i = 0;
                i2 = 0;
                i3 = 0;
            } else {
                String strSubstring3 = strSubstring.substring(0, iIndexOf2);
                String strSubstring4 = strSubstring.substring(i6, iIndexOf3);
                String strSubstring5 = strSubstring.substring(iIndexOf3 + 1);
                if (strSubstring3.length() == 4 && strSubstring4.length() >= 1 && strSubstring4.length() <= 2 && strSubstring5.length() >= 1 && strSubstring5.length() <= 2) {
                    i = Integer.parseInt(strSubstring3);
                    i2 = Integer.parseInt(strSubstring4);
                    int i9 = Integer.parseInt(strSubstring5);
                    if (i2 < 1 || i2 > 12 || i9 < 1 || i9 > 31) {
                        i3 = i9;
                        z = false;
                    } else {
                        i3 = i9;
                        z = true;
                    }
                }
            }
            if (!z) {
                throw new IllegalArgumentException("Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]");
            }
            if (iIndexOf4 <= 0) {
                z2 = false;
            } else {
                z2 = true;
            }
            if (iIndexOf5 <= 0) {
                z3 = false;
            } else {
                z3 = true;
            }
            boolean z7 = z2 & z3;
            if (iIndexOf5 >= strSubstring2.length() - 1) {
                z4 = false;
            } else {
                z4 = true;
            }
            if (z7 & z4) {
                int i10 = Integer.parseInt(strSubstring2.substring(0, iIndexOf4));
                int i11 = Integer.parseInt(strSubstring2.substring(i7, iIndexOf5));
                if (iIndexOf6 <= 0) {
                    z5 = false;
                } else {
                    z5 = true;
                }
                if (iIndexOf6 >= strSubstring2.length() - 1) {
                    z6 = false;
                } else {
                    z6 = true;
                }
                if (z5 & z6) {
                    int i12 = Integer.parseInt(strSubstring2.substring(i8, iIndexOf6));
                    String strSubstring6 = strSubstring2.substring(iIndexOf6 + 1);
                    if (strSubstring6.length() > 9) {
                        throw new IllegalArgumentException("Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]");
                    }
                    if (!Character.isDigit(strSubstring6.charAt(0))) {
                        throw new IllegalArgumentException("Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]");
                    }
                    i5 = Integer.parseInt(strSubstring6 + "000000000".substring(0, 9 - strSubstring6.length()));
                    i4 = i12;
                } else {
                    if (iIndexOf6 > 0) {
                        throw new IllegalArgumentException("Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]");
                    }
                    i4 = Integer.parseInt(strSubstring2.substring(i8));
                }
                return new Timestamp(i - 1900, i2 - 1, i3, i10, i11, i4, i5);
            }
            throw new IllegalArgumentException("Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]");
        }
        throw new IllegalArgumentException("Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]");
    }

    @Override
    public String toString() {
        String str;
        String string;
        String string2;
        String string3;
        String string4;
        String string5;
        String str2;
        int year = super.getYear() + 1900;
        int month = super.getMonth() + 1;
        int date = super.getDate();
        int hours = super.getHours();
        int minutes = super.getMinutes();
        int seconds = super.getSeconds();
        if (year < 1000) {
            String str3 = "" + year;
            str = "0000".substring(0, 4 - str3.length()) + str3;
        } else {
            str = "" + year;
        }
        if (month < 10) {
            string = "0" + month;
        } else {
            string = Integer.toString(month);
        }
        if (date < 10) {
            string2 = "0" + date;
        } else {
            string2 = Integer.toString(date);
        }
        if (hours < 10) {
            string3 = "0" + hours;
        } else {
            string3 = Integer.toString(hours);
        }
        if (minutes < 10) {
            string4 = "0" + minutes;
        } else {
            string4 = Integer.toString(minutes);
        }
        if (seconds < 10) {
            string5 = "0" + seconds;
        } else {
            string5 = Integer.toString(seconds);
        }
        if (this.nanos == 0) {
            str2 = "0";
        } else {
            String string6 = Integer.toString(this.nanos);
            String str4 = "000000000".substring(0, 9 - string6.length()) + string6;
            char[] cArr = new char[str4.length()];
            str4.getChars(0, str4.length(), cArr, 0);
            int i = 8;
            while (cArr[i] == '0') {
                i--;
            }
            str2 = new String(cArr, 0, i + 1);
        }
        StringBuffer stringBuffer = new StringBuffer(20 + str2.length());
        stringBuffer.append(str);
        stringBuffer.append(LanguageTag.SEP);
        stringBuffer.append(string);
        stringBuffer.append(LanguageTag.SEP);
        stringBuffer.append(string2);
        stringBuffer.append(" ");
        stringBuffer.append(string3);
        stringBuffer.append(":");
        stringBuffer.append(string4);
        stringBuffer.append(":");
        stringBuffer.append(string5);
        stringBuffer.append(".");
        stringBuffer.append(str2);
        return stringBuffer.toString();
    }

    public int getNanos() {
        return this.nanos;
    }

    public void setNanos(int i) {
        if (i > 999999999 || i < 0) {
            throw new IllegalArgumentException("nanos > 999999999 or < 0");
        }
        this.nanos = i;
    }

    public boolean equals(Timestamp timestamp) {
        return super.equals((Object) timestamp) && this.nanos == timestamp.nanos;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Timestamp) {
            return equals((Timestamp) obj);
        }
        return false;
    }

    public boolean before(Timestamp timestamp) {
        return compareTo(timestamp) < 0;
    }

    public boolean after(Timestamp timestamp) {
        return compareTo(timestamp) > 0;
    }

    public int compareTo(Timestamp timestamp) {
        long time = getTime();
        long time2 = timestamp.getTime();
        int i = time < time2 ? -1 : time == time2 ? 0 : 1;
        if (i == 0) {
            if (this.nanos > timestamp.nanos) {
                return 1;
            }
            if (this.nanos < timestamp.nanos) {
                return -1;
            }
        }
        return i;
    }

    @Override
    public int compareTo(java.util.Date date) {
        if (date instanceof Timestamp) {
            return compareTo((Timestamp) date);
        }
        return compareTo(new Timestamp(date.getTime()));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
