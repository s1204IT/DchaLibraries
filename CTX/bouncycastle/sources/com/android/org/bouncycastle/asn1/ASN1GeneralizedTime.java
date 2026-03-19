package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class ASN1GeneralizedTime extends ASN1Primitive {
    private byte[] time;

    public static ASN1GeneralizedTime getInstance(Object obj) {
        if (obj == null || (obj instanceof ASN1GeneralizedTime)) {
            return (ASN1GeneralizedTime) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return (ASN1GeneralizedTime) fromByteArray((byte[]) obj);
            } catch (Exception e) {
                throw new IllegalArgumentException("encoding error in getInstance: " + e.toString());
            }
        }
        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    public static ASN1GeneralizedTime getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        ASN1Primitive object = aSN1TaggedObject.getObject();
        if (z || (object instanceof ASN1GeneralizedTime)) {
            return getInstance(object);
        }
        return new ASN1GeneralizedTime(((ASN1OctetString) object).getOctets());
    }

    public ASN1GeneralizedTime(String str) {
        this.time = Strings.toByteArray(str);
        try {
            getDate();
        } catch (ParseException e) {
            throw new IllegalArgumentException("invalid date string: " + e.getMessage());
        }
    }

    public ASN1GeneralizedTime(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US);
        simpleDateFormat.setTimeZone(new SimpleTimeZone(0, "Z"));
        this.time = Strings.toByteArray(simpleDateFormat.format(date));
    }

    public ASN1GeneralizedTime(Date date, Locale locale) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US);
        simpleDateFormat.setCalendar(Calendar.getInstance(Locale.US));
        simpleDateFormat.setTimeZone(new SimpleTimeZone(0, "Z"));
        this.time = Strings.toByteArray(simpleDateFormat.format(date));
    }

    ASN1GeneralizedTime(byte[] bArr) {
        this.time = bArr;
    }

    public String getTimeString() {
        return Strings.fromByteArray(this.time);
    }

    public String getTime() {
        String strFromByteArray = Strings.fromByteArray(this.time);
        if (strFromByteArray.charAt(strFromByteArray.length() - 1) == 'Z') {
            return strFromByteArray.substring(0, strFromByteArray.length() - 1) + "GMT+00:00";
        }
        int length = strFromByteArray.length() - 5;
        char cCharAt = strFromByteArray.charAt(length);
        if (cCharAt == '-' || cCharAt == '+') {
            StringBuilder sb = new StringBuilder();
            sb.append(strFromByteArray.substring(0, length));
            sb.append("GMT");
            int i = length + 3;
            sb.append(strFromByteArray.substring(length, i));
            sb.append(":");
            sb.append(strFromByteArray.substring(i));
            return sb.toString();
        }
        int length2 = strFromByteArray.length() - 3;
        char cCharAt2 = strFromByteArray.charAt(length2);
        if (cCharAt2 == '-' || cCharAt2 == '+') {
            return strFromByteArray.substring(0, length2) + "GMT" + strFromByteArray.substring(length2) + ":00";
        }
        return strFromByteArray + calculateGMTOffset();
    }

    private String calculateGMTOffset() {
        String str = "+";
        TimeZone timeZone = TimeZone.getDefault();
        int rawOffset = timeZone.getRawOffset();
        if (rawOffset < 0) {
            str = "-";
            rawOffset = -rawOffset;
        }
        int i = rawOffset / 3600000;
        int i2 = (rawOffset - (((i * 60) * 60) * 1000)) / 60000;
        try {
            if (timeZone.useDaylightTime() && timeZone.inDaylightTime(getDate())) {
                i += str.equals("+") ? 1 : -1;
            }
        } catch (ParseException e) {
        }
        return "GMT" + str + convert(i) + ":" + convert(i2);
    }

    private String convert(int i) {
        if (i < 10) {
            return "0" + i;
        }
        return Integer.toString(i);
    }

    public Date getDate() throws ParseException {
        SimpleDateFormat simpleDateFormat;
        char cCharAt;
        String strFromByteArray = Strings.fromByteArray(this.time);
        if (strFromByteArray.endsWith("Z")) {
            if (hasFractionalSeconds()) {
                simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss.SSS'Z'", Locale.US);
            } else {
                simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US);
            }
            simpleDateFormat.setTimeZone(new SimpleTimeZone(0, "Z"));
        } else if (strFromByteArray.indexOf(45) > 0 || strFromByteArray.indexOf(43) > 0) {
            strFromByteArray = getTime();
            if (hasFractionalSeconds()) {
                simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss.SSSz", Locale.US);
            } else {
                simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssz", Locale.US);
            }
            simpleDateFormat.setTimeZone(new SimpleTimeZone(0, "Z"));
        } else {
            if (hasFractionalSeconds()) {
                simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss.SSS", Locale.US);
            } else {
                simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            }
            simpleDateFormat.setTimeZone(new SimpleTimeZone(0, TimeZone.getDefault().getID()));
        }
        if (hasFractionalSeconds()) {
            String strSubstring = strFromByteArray.substring(14);
            int i = 1;
            while (i < strSubstring.length() && '0' <= (cCharAt = strSubstring.charAt(i)) && cCharAt <= '9') {
                i++;
            }
            int i2 = i - 1;
            if (i2 > 3) {
                strFromByteArray = strFromByteArray.substring(0, 14) + (strSubstring.substring(0, 4) + strSubstring.substring(i));
            } else if (i2 == 1) {
                strFromByteArray = strFromByteArray.substring(0, 14) + (strSubstring.substring(0, i) + "00" + strSubstring.substring(i));
            } else if (i2 == 2) {
                strFromByteArray = strFromByteArray.substring(0, 14) + (strSubstring.substring(0, i) + "0" + strSubstring.substring(i));
            }
        }
        return simpleDateFormat.parse(strFromByteArray);
    }

    private boolean hasFractionalSeconds() {
        for (int i = 0; i != this.time.length; i++) {
            if (this.time[i] == 46 && i == 14) {
                return true;
            }
        }
        return false;
    }

    @Override
    boolean isConstructed() {
        return false;
    }

    @Override
    int encodedLength() {
        int length = this.time.length;
        return 1 + StreamUtil.calculateBodyLength(length) + length;
    }

    @Override
    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(24, this.time);
    }

    @Override
    boolean asn1Equals(ASN1Primitive aSN1Primitive) {
        if (!(aSN1Primitive instanceof ASN1GeneralizedTime)) {
            return false;
        }
        return Arrays.areEqual(this.time, ((ASN1GeneralizedTime) aSN1Primitive).time);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.time);
    }
}
