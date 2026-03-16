package com.android.calendarcommon2;

public class Duration {
    public int days;
    public int hours;
    public int minutes;
    public int seconds;
    public int sign = 1;
    public int weeks;

    public void parse(String str) throws DateException {
        this.sign = 1;
        this.weeks = 0;
        this.days = 0;
        this.hours = 0;
        this.minutes = 0;
        this.seconds = 0;
        int len = str.length();
        int index = 0;
        if (len >= 1) {
            char c = str.charAt(0);
            if (c == '-') {
                this.sign = -1;
                index = 0 + 1;
            } else if (c == '+') {
                index = 0 + 1;
            }
            if (len >= index) {
                if (str.charAt(index) != 'P') {
                    throw new DateException("Duration.parse(str='" + str + "') expected 'P' at index=" + index);
                }
                int index2 = index + 1;
                if (str.charAt(index2) == 'T') {
                    index2++;
                }
                int n = 0;
                while (index2 < len) {
                    char c2 = str.charAt(index2);
                    if (c2 >= '0' && c2 <= '9') {
                        n = (n * 10) + (c2 - '0');
                    } else if (c2 == 'W') {
                        this.weeks = n;
                        n = 0;
                    } else if (c2 == 'H') {
                        this.hours = n;
                        n = 0;
                    } else if (c2 == 'M') {
                        this.minutes = n;
                        n = 0;
                    } else if (c2 == 'S') {
                        this.seconds = n;
                        n = 0;
                    } else if (c2 == 'D') {
                        this.days = n;
                        n = 0;
                    } else if (c2 != 'T') {
                        throw new DateException("Duration.parse(str='" + str + "') unexpected char '" + c2 + "' at index=" + index2);
                    }
                    index2++;
                }
            }
        }
    }

    public long getMillis() {
        long factor = this.sign * 1000;
        return ((long) ((604800 * this.weeks) + (86400 * this.days) + (this.hours * 3600) + (this.minutes * 60) + this.seconds)) * factor;
    }
}
