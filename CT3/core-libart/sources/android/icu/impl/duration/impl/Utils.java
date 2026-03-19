package android.icu.impl.duration.impl;

import android.icu.impl.locale.BaseLocale;
import java.util.Locale;

public class Utils {
    public static final Locale localeFromString(String s) {
        String language = s;
        String region = "";
        String variant = "";
        int x = s.indexOf(BaseLocale.SEP);
        if (x != -1) {
            region = s.substring(x + 1);
            language = s.substring(0, x);
        }
        int x2 = region.indexOf(BaseLocale.SEP);
        if (x2 != -1) {
            variant = region.substring(x2 + 1);
            region = region.substring(0, x2);
        }
        return new Locale(language, region, variant);
    }

    public static String chineseNumber(long n, ChineseDigits zh) {
        if (n < 0) {
            n = -n;
        }
        if (n <= 10) {
            if (n == 2) {
                return String.valueOf(zh.liang);
            }
            return String.valueOf(zh.digits[(int) n]);
        }
        char[] buf = new char[40];
        char[] digits = String.valueOf(n).toCharArray();
        boolean inZero = true;
        boolean forcedZero = false;
        int x = buf.length;
        int i = digits.length;
        int u = -1;
        int l = -1;
        while (true) {
            int u2 = u;
            i--;
            if (i < 0) {
                break;
            }
            if (u2 == -1) {
                if (l != -1) {
                    x--;
                    buf[x] = zh.levels[l];
                    inZero = true;
                    forcedZero = false;
                }
                u = u2 + 1;
            } else {
                x--;
                u = u2 + 1;
                buf[x] = zh.units[u2];
                if (u == 3) {
                    u = -1;
                    l++;
                }
            }
            int d = digits[i] - '0';
            if (d == 0) {
                if (x < buf.length - 1 && u != 0) {
                    buf[x] = '*';
                }
                if (inZero || forcedZero) {
                    x--;
                    buf[x] = '*';
                } else {
                    x--;
                    buf[x] = zh.digits[0];
                    inZero = true;
                    forcedZero = u == 1;
                }
            } else {
                inZero = false;
                x--;
                buf[x] = zh.digits[d];
            }
        }
        if (n > 1000000) {
            boolean last = true;
            int i2 = buf.length - 3;
            while (buf[i2] != '0') {
                i2 -= 8;
                last = !last;
                if (i2 <= x) {
                    break;
                }
            }
            int i3 = buf.length - 7;
            do {
                if (buf[i3] == zh.digits[0] && !last) {
                    buf[i3] = '*';
                }
                i3 -= 8;
                last = !last;
            } while (i3 > x);
            if (n >= 100000000) {
                int i4 = buf.length - 8;
                do {
                    boolean empty = true;
                    int j = i4 - 1;
                    int e = Math.max(x - 1, i4 - 8);
                    while (true) {
                        if (j <= e) {
                            break;
                        }
                        if (buf[j] != '*') {
                            empty = false;
                            break;
                        }
                        j--;
                    }
                    if (empty) {
                        if (buf[i4 + 1] != '*' && buf[i4 + 1] != zh.digits[0]) {
                            buf[i4] = zh.digits[0];
                        } else {
                            buf[i4] = '*';
                        }
                    }
                    i4 -= 8;
                } while (i4 > x);
            }
        }
        for (int i5 = x; i5 < buf.length; i5++) {
            if (buf[i5] == zh.digits[2] && ((i5 >= buf.length - 1 || buf[i5 + 1] != zh.units[0]) && (i5 <= x || (buf[i5 - 1] != zh.units[0] && buf[i5 - 1] != zh.digits[0] && buf[i5 - 1] != '*')))) {
                buf[i5] = zh.liang;
            }
        }
        if (buf[x] == zh.digits[1] && (zh.ko || buf[x + 1] == zh.units[0])) {
            x++;
        }
        int w = x;
        for (int r = x; r < buf.length; r++) {
            if (buf[r] != '*') {
                buf[w] = buf[r];
                w++;
            }
        }
        return new String(buf, x, w - x);
    }

    public static class ChineseDigits {
        final char[] digits;
        final boolean ko;
        final char[] levels;
        final char liang;
        final char[] units;
        public static final ChineseDigits DEBUG = new ChineseDigits("0123456789s", "sbq", "WYZ", 'L', false);
        public static final ChineseDigits TRADITIONAL = new ChineseDigits("零一二三四五六七八九十", "十百千", "萬億兆", 20841, false);
        public static final ChineseDigits SIMPLIFIED = new ChineseDigits("零一二三四五六七八九十", "十百千", "万亿兆", 20004, false);
        public static final ChineseDigits KOREAN = new ChineseDigits("영일이삼사오육칠팔구십", "십백천", "만억?", 51060, true);

        ChineseDigits(String digits, String units, String levels, char liang, boolean ko) {
            this.digits = digits.toCharArray();
            this.units = units.toCharArray();
            this.levels = levels.toCharArray();
            this.liang = liang;
            this.ko = ko;
        }
    }
}
