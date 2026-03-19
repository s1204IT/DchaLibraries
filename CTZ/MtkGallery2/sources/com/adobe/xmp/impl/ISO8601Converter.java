package com.adobe.xmp.impl;

import com.adobe.xmp.XMPDateTime;
import com.adobe.xmp.XMPException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.SimpleTimeZone;

public final class ISO8601Converter {
    public static XMPDateTime parse(String str) throws XMPException {
        return parse(str, new XMPDateTimeImpl());
    }

    public static XMPDateTime parse(String str, XMPDateTime xMPDateTime) throws XMPException {
        int i;
        int iGatherInt;
        int i2;
        ParameterAsserts.assertNotNull(str);
        ParseState parseState = new ParseState(str);
        int i3 = 0;
        boolean z = parseState.ch(0) == 'T' || (parseState.length() >= 2 && parseState.ch(1) == ':') || (parseState.length() >= 3 && parseState.ch(2) == ':');
        if (!z) {
            if (parseState.ch(0) == '-') {
                parseState.skip();
            }
            int iGatherInt2 = parseState.gatherInt("Invalid year in date string", 9999);
            if (parseState.hasNext() && parseState.ch() != '-') {
                throw new XMPException("Invalid date string, after year", 5);
            }
            if (parseState.ch(0) == '-') {
                iGatherInt2 = -iGatherInt2;
            }
            xMPDateTime.setYear(iGatherInt2);
            if (!parseState.hasNext()) {
                return xMPDateTime;
            }
            parseState.skip();
            int iGatherInt3 = parseState.gatherInt("Invalid month in date string", 12);
            if (parseState.hasNext() && parseState.ch() != '-') {
                throw new XMPException("Invalid date string, after month", 5);
            }
            xMPDateTime.setMonth(iGatherInt3);
            if (!parseState.hasNext()) {
                return xMPDateTime;
            }
            parseState.skip();
            int iGatherInt4 = parseState.gatherInt("Invalid day in date string", 31);
            if (parseState.hasNext() && parseState.ch() != 'T') {
                throw new XMPException("Invalid date string, after day", 5);
            }
            xMPDateTime.setDay(iGatherInt4);
            if (!parseState.hasNext()) {
                return xMPDateTime;
            }
        } else {
            xMPDateTime.setMonth(1);
            xMPDateTime.setDay(1);
        }
        if (parseState.ch() == 'T') {
            parseState.skip();
        } else if (!z) {
            throw new XMPException("Invalid date string, missing 'T' after date", 5);
        }
        int iGatherInt5 = parseState.gatherInt("Invalid hour in date string", 23);
        if (parseState.ch() != ':') {
            throw new XMPException("Invalid date string, after hour", 5);
        }
        xMPDateTime.setHour(iGatherInt5);
        parseState.skip();
        int iGatherInt6 = parseState.gatherInt("Invalid minute in date string", 59);
        if (parseState.hasNext() && parseState.ch() != ':' && parseState.ch() != 'Z' && parseState.ch() != '+' && parseState.ch() != '-') {
            throw new XMPException("Invalid date string, after minute", 5);
        }
        xMPDateTime.setMinute(iGatherInt6);
        if (parseState.ch() == ':') {
            parseState.skip();
            int iGatherInt7 = parseState.gatherInt("Invalid whole seconds in date string", 59);
            if (parseState.hasNext() && parseState.ch() != '.' && parseState.ch() != 'Z' && parseState.ch() != '+' && parseState.ch() != '-') {
                throw new XMPException("Invalid date string, after whole seconds", 5);
            }
            xMPDateTime.setSecond(iGatherInt7);
            if (parseState.ch() == '.') {
                parseState.skip();
                int iPos = parseState.pos();
                int iGatherInt8 = parseState.gatherInt("Invalid fractional seconds in date string", 999999999);
                if (parseState.ch() != 'Z' && parseState.ch() != '+' && parseState.ch() != '-') {
                    throw new XMPException("Invalid date string, after fractional second", 5);
                }
                int iPos2 = parseState.pos() - iPos;
                while (iPos2 > 9) {
                    iGatherInt8 /= 10;
                    iPos2--;
                }
                while (iPos2 < 9) {
                    iGatherInt8 *= 10;
                    iPos2++;
                }
                xMPDateTime.setNanoSecond(iGatherInt8);
            }
        }
        if (parseState.ch() == 'Z') {
            parseState.skip();
        } else {
            if (parseState.hasNext()) {
                if (parseState.ch() != '+') {
                    if (parseState.ch() != '-') {
                        throw new XMPException("Time zone must begin with 'Z', '+', or '-'", 5);
                    }
                    i = -1;
                } else {
                    i = 1;
                }
                parseState.skip();
                int iGatherInt9 = parseState.gatherInt("Invalid time zone hour in date string", 23);
                if (parseState.ch() != ':') {
                    throw new XMPException("Invalid date string, after time zone hour", 5);
                }
                parseState.skip();
                iGatherInt = parseState.gatherInt("Invalid time zone minute in date string", 59);
                i2 = i;
                i3 = iGatherInt9;
            }
            xMPDateTime.setTimeZone(new SimpleTimeZone(((i3 * 3600 * 1000) + (iGatherInt * 60 * 1000)) * i2, ""));
            if (!parseState.hasNext()) {
                throw new XMPException("Invalid date string, extra chars at end", 5);
            }
            return xMPDateTime;
        }
        i2 = 0;
        iGatherInt = 0;
        xMPDateTime.setTimeZone(new SimpleTimeZone(((i3 * 3600 * 1000) + (iGatherInt * 60 * 1000)) * i2, ""));
        if (!parseState.hasNext()) {
        }
    }

    public static String render(XMPDateTime xMPDateTime) {
        StringBuffer stringBuffer = new StringBuffer();
        DecimalFormat decimalFormat = new DecimalFormat("0000", new DecimalFormatSymbols(Locale.ENGLISH));
        stringBuffer.append(decimalFormat.format(xMPDateTime.getYear()));
        if (xMPDateTime.getMonth() == 0) {
            return stringBuffer.toString();
        }
        decimalFormat.applyPattern("'-'00");
        stringBuffer.append(decimalFormat.format(xMPDateTime.getMonth()));
        if (xMPDateTime.getDay() == 0) {
            return stringBuffer.toString();
        }
        stringBuffer.append(decimalFormat.format(xMPDateTime.getDay()));
        if (xMPDateTime.getHour() != 0 || xMPDateTime.getMinute() != 0 || xMPDateTime.getSecond() != 0 || xMPDateTime.getNanoSecond() != 0 || (xMPDateTime.getTimeZone() != null && xMPDateTime.getTimeZone().getRawOffset() != 0)) {
            stringBuffer.append('T');
            decimalFormat.applyPattern("00");
            stringBuffer.append(decimalFormat.format(xMPDateTime.getHour()));
            stringBuffer.append(':');
            stringBuffer.append(decimalFormat.format(xMPDateTime.getMinute()));
            if (xMPDateTime.getSecond() != 0 || xMPDateTime.getNanoSecond() != 0) {
                double second = ((double) xMPDateTime.getSecond()) + (((double) xMPDateTime.getNanoSecond()) / 1.0E9d);
                decimalFormat.applyPattern(":00.#########");
                stringBuffer.append(decimalFormat.format(second));
            }
            if (xMPDateTime.getTimeZone() != null) {
                int offset = xMPDateTime.getTimeZone().getOffset(xMPDateTime.getCalendar().getTimeInMillis());
                if (offset == 0) {
                    stringBuffer.append('Z');
                } else {
                    int i = offset / 3600000;
                    int iAbs = Math.abs((offset % 3600000) / 60000);
                    decimalFormat.applyPattern("+00;-00");
                    stringBuffer.append(decimalFormat.format(i));
                    decimalFormat.applyPattern(":00");
                    stringBuffer.append(decimalFormat.format(iAbs));
                }
            }
        }
        return stringBuffer.toString();
    }
}
