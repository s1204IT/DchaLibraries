package com.adobe.xmp.impl;

import com.adobe.xmp.XMPDateTime;
import com.adobe.xmp.XMPException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.SimpleTimeZone;

public final class ISO8601Converter {
    private ISO8601Converter() {
    }

    public static XMPDateTime parse(String iso8601String) throws XMPException {
        return parse(iso8601String, new XMPDateTimeImpl());
    }

    public static XMPDateTime parse(String iso8601String, XMPDateTime binValue) throws XMPException {
        ParameterAsserts.assertNotNull(iso8601String);
        ParseState input = new ParseState(iso8601String);
        boolean timeOnly = input.ch(0) == 'T' || (input.length() >= 2 && input.ch(1) == ':') || (input.length() >= 3 && input.ch(2) == ':');
        if (!timeOnly) {
            if (input.ch(0) == '-') {
                input.skip();
            }
            int value = input.gatherInt("Invalid year in date string", 9999);
            if (input.hasNext() && input.ch() != '-') {
                throw new XMPException("Invalid date string, after year", 5);
            }
            if (input.ch(0) == '-') {
                value = -value;
            }
            binValue.setYear(value);
            if (input.hasNext()) {
                input.skip();
                int value2 = input.gatherInt("Invalid month in date string", 12);
                if (input.hasNext() && input.ch() != '-') {
                    throw new XMPException("Invalid date string, after month", 5);
                }
                binValue.setMonth(value2);
                if (input.hasNext()) {
                    input.skip();
                    int value3 = input.gatherInt("Invalid day in date string", 31);
                    if (input.hasNext() && input.ch() != 'T') {
                        throw new XMPException("Invalid date string, after day", 5);
                    }
                    binValue.setDay(value3);
                    if (input.hasNext()) {
                    }
                }
            }
            return binValue;
        }
        binValue.setMonth(1);
        binValue.setDay(1);
        if (input.ch() == 'T') {
            input.skip();
        } else if (!timeOnly) {
            throw new XMPException("Invalid date string, missing 'T' after date", 5);
        }
        int value4 = input.gatherInt("Invalid hour in date string", 23);
        if (input.ch() != ':') {
            throw new XMPException("Invalid date string, after hour", 5);
        }
        binValue.setHour(value4);
        input.skip();
        int value5 = input.gatherInt("Invalid minute in date string", 59);
        if (input.hasNext() && input.ch() != ':' && input.ch() != 'Z' && input.ch() != '+' && input.ch() != '-') {
            throw new XMPException("Invalid date string, after minute", 5);
        }
        binValue.setMinute(value5);
        if (input.ch() == ':') {
            input.skip();
            int value6 = input.gatherInt("Invalid whole seconds in date string", 59);
            if (input.hasNext() && input.ch() != '.' && input.ch() != 'Z' && input.ch() != '+' && input.ch() != '-') {
                throw new XMPException("Invalid date string, after whole seconds", 5);
            }
            binValue.setSecond(value6);
            if (input.ch() == '.') {
                input.skip();
                int digits = input.pos();
                int value7 = input.gatherInt("Invalid fractional seconds in date string", 999999999);
                if (input.ch() != 'Z' && input.ch() != '+' && input.ch() != '-') {
                    throw new XMPException("Invalid date string, after fractional second", 5);
                }
                int digits2 = input.pos() - digits;
                while (digits2 > 9) {
                    value7 /= 10;
                    digits2--;
                }
                while (digits2 < 9) {
                    value7 *= 10;
                    digits2++;
                }
                binValue.setNanoSecond(value7);
            }
        }
        int tzSign = 0;
        int tzHour = 0;
        int tzMinute = 0;
        if (input.ch() == 'Z') {
            input.skip();
        } else if (input.hasNext()) {
            if (input.ch() == '+') {
                tzSign = 1;
            } else if (input.ch() == '-') {
                tzSign = -1;
            } else {
                throw new XMPException("Time zone must begin with 'Z', '+', or '-'", 5);
            }
            input.skip();
            tzHour = input.gatherInt("Invalid time zone hour in date string", 23);
            if (input.ch() != ':') {
                throw new XMPException("Invalid date string, after time zone hour", 5);
            }
            input.skip();
            tzMinute = input.gatherInt("Invalid time zone minute in date string", 59);
        }
        int offset = ((tzHour * 3600 * 1000) + (tzMinute * 60 * 1000)) * tzSign;
        binValue.setTimeZone(new SimpleTimeZone(offset, ""));
        if (input.hasNext()) {
            throw new XMPException("Invalid date string, extra chars at end", 5);
        }
        return binValue;
    }

    public static String render(XMPDateTime dateTime) {
        StringBuffer buffer = new StringBuffer();
        DecimalFormat df = new DecimalFormat("0000", new DecimalFormatSymbols(Locale.ENGLISH));
        buffer.append(df.format(dateTime.getYear()));
        if (dateTime.getMonth() == 0) {
            return buffer.toString();
        }
        df.applyPattern("'-'00");
        buffer.append(df.format(dateTime.getMonth()));
        if (dateTime.getDay() == 0) {
            return buffer.toString();
        }
        buffer.append(df.format(dateTime.getDay()));
        if (dateTime.getHour() != 0 || dateTime.getMinute() != 0 || dateTime.getSecond() != 0 || dateTime.getNanoSecond() != 0 || (dateTime.getTimeZone() != null && dateTime.getTimeZone().getRawOffset() != 0)) {
            buffer.append('T');
            df.applyPattern("00");
            buffer.append(df.format(dateTime.getHour()));
            buffer.append(':');
            buffer.append(df.format(dateTime.getMinute()));
            if (dateTime.getSecond() != 0 || dateTime.getNanoSecond() != 0) {
                double seconds = ((double) dateTime.getSecond()) + (((double) dateTime.getNanoSecond()) / 1.0E9d);
                df.applyPattern(":00.#########");
                buffer.append(df.format(seconds));
            }
            if (dateTime.getTimeZone() != null) {
                long timeInMillis = dateTime.getCalendar().getTimeInMillis();
                int offset = dateTime.getTimeZone().getOffset(timeInMillis);
                if (offset == 0) {
                    buffer.append('Z');
                } else {
                    int thours = offset / 3600000;
                    int tminutes = Math.abs((offset % 3600000) / 60000);
                    df.applyPattern("+00;-00");
                    buffer.append(df.format(thours));
                    df.applyPattern(":00");
                    buffer.append(df.format(tminutes));
                }
            }
        }
        return buffer.toString();
    }
}
