package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

public class DERUTCTime extends ASN1Primitive {
    private byte[] time;

    public static ASN1UTCTime getInstance(Object obj) {
        if (obj == null || (obj instanceof ASN1UTCTime)) {
            return (ASN1UTCTime) obj;
        }
        if (obj instanceof DERUTCTime) {
            return new ASN1UTCTime(((DERUTCTime) obj).time);
        }
        if (obj instanceof byte[]) {
            try {
                return (ASN1UTCTime) fromByteArray((byte[]) obj);
            } catch (Exception e) {
                throw new IllegalArgumentException("encoding error in getInstance: " + e.toString());
            }
        }
        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    public static ASN1UTCTime getInstance(ASN1TaggedObject obj, boolean explicit) {
        ASN1Object o = obj.getObject();
        return (explicit || (o instanceof ASN1UTCTime)) ? getInstance(o) : new ASN1UTCTime(((ASN1OctetString) o).getOctets());
    }

    public DERUTCTime(String time) {
        this.time = Strings.toByteArray(time);
        try {
            getDate();
        } catch (ParseException e) {
            throw new IllegalArgumentException("invalid date string: " + e.getMessage());
        }
    }

    public DERUTCTime(Date time) {
        SimpleDateFormat dateF = new SimpleDateFormat("yyMMddHHmmss'Z'");
        dateF.setTimeZone(new SimpleTimeZone(0, "Z"));
        this.time = Strings.toByteArray(dateF.format(time));
    }

    DERUTCTime(byte[] time) {
        this.time = time;
    }

    public Date getDate() throws ParseException {
        SimpleDateFormat dateF = new SimpleDateFormat("yyMMddHHmmssz");
        return dateF.parse(getTime());
    }

    public Date getAdjustedDate() throws ParseException {
        SimpleDateFormat dateF = new SimpleDateFormat("yyyyMMddHHmmssz");
        dateF.setTimeZone(new SimpleTimeZone(0, "Z"));
        return dateF.parse(getAdjustedTime());
    }

    public String getTime() {
        String stime = Strings.fromByteArray(this.time);
        if (stime.indexOf(45) < 0 && stime.indexOf(43) < 0) {
            if (stime.length() == 11) {
                return stime.substring(0, 10) + "00GMT+00:00";
            }
            return stime.substring(0, 12) + "GMT+00:00";
        }
        int index = stime.indexOf(45);
        if (index < 0) {
            index = stime.indexOf(43);
        }
        String d = stime;
        if (index == stime.length() - 3) {
            d = d + "00";
        }
        if (index == 10) {
            return d.substring(0, 10) + "00GMT" + d.substring(10, 13) + ":" + d.substring(13, 15);
        }
        return d.substring(0, 12) + "GMT" + d.substring(12, 15) + ":" + d.substring(15, 17);
    }

    public String getAdjustedTime() {
        String d = getTime();
        return d.charAt(0) < '5' ? "20" + d : "19" + d;
    }

    @Override
    boolean isConstructed() {
        return false;
    }

    @Override
    int encodedLength() {
        int length = this.time.length;
        return StreamUtil.calculateBodyLength(length) + 1 + length;
    }

    @Override
    void encode(ASN1OutputStream out) throws IOException {
        out.write(23);
        int length = this.time.length;
        out.writeLength(length);
        for (int i = 0; i != length; i++) {
            out.write(this.time[i]);
        }
    }

    @Override
    boolean asn1Equals(ASN1Primitive o) {
        if (o instanceof DERUTCTime) {
            return Arrays.areEqual(this.time, ((DERUTCTime) o).time);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.time);
    }

    public String toString() {
        return Strings.fromByteArray(this.time);
    }
}
