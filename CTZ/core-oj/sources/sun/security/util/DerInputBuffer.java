package sun.security.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Date;
import java.util.TimeZone;
import sun.util.calendar.CalendarDate;
import sun.util.calendar.CalendarSystem;
import sun.util.calendar.Gregorian;

class DerInputBuffer extends ByteArrayInputStream implements Cloneable {
    DerInputBuffer(byte[] bArr) {
        super(bArr);
    }

    DerInputBuffer(byte[] bArr, int i, int i2) {
        super(bArr, i, i2);
    }

    DerInputBuffer dup() {
        try {
            DerInputBuffer derInputBuffer = (DerInputBuffer) clone();
            derInputBuffer.mark(Integer.MAX_VALUE);
            return derInputBuffer;
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    byte[] toByteArray() {
        int iAvailable = available();
        if (iAvailable <= 0) {
            return null;
        }
        byte[] bArr = new byte[iAvailable];
        System.arraycopy(this.buf, this.pos, bArr, 0, iAvailable);
        return bArr;
    }

    int getPos() {
        return this.pos;
    }

    byte[] getSlice(int i, int i2) {
        byte[] bArr = new byte[i2];
        System.arraycopy(this.buf, i, bArr, 0, i2);
        return bArr;
    }

    int peek() throws IOException {
        if (this.pos >= this.count) {
            throw new IOException("out of data");
        }
        return this.buf[this.pos];
    }

    public boolean equals(Object obj) {
        if (obj instanceof DerInputBuffer) {
            return equals((DerInputBuffer) obj);
        }
        return false;
    }

    boolean equals(DerInputBuffer derInputBuffer) {
        if (this == derInputBuffer) {
            return true;
        }
        int iAvailable = available();
        if (derInputBuffer.available() != iAvailable) {
            return false;
        }
        for (int i = 0; i < iAvailable; i++) {
            if (this.buf[this.pos + i] != derInputBuffer.buf[derInputBuffer.pos + i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int iAvailable = available();
        int i = this.pos;
        int i2 = 0;
        for (int i3 = 0; i3 < iAvailable; i3++) {
            i2 += this.buf[i + i3] * i3;
        }
        return i2;
    }

    void truncate(int i) throws IOException {
        if (i > available()) {
            throw new IOException("insufficient data");
        }
        this.count = this.pos + i;
    }

    BigInteger getBigInteger(int i, boolean z) throws IOException {
        if (i > available()) {
            throw new IOException("short read of integer");
        }
        if (i == 0) {
            throw new IOException("Invalid encoding: zero length Int value");
        }
        byte[] bArr = new byte[i];
        System.arraycopy(this.buf, this.pos, bArr, 0, i);
        skip(i);
        if (i >= 2 && bArr[0] == 0 && bArr[1] >= 0) {
            throw new IOException("Invalid encoding: redundant leading 0s");
        }
        if (z) {
            return new BigInteger(1, bArr);
        }
        return new BigInteger(bArr);
    }

    public int getInteger(int i) throws IOException {
        BigInteger bigInteger = getBigInteger(i, false);
        if (bigInteger.compareTo(BigInteger.valueOf(-2147483648L)) < 0) {
            throw new IOException("Integer below minimum valid value");
        }
        if (bigInteger.compareTo(BigInteger.valueOf(2147483647L)) > 0) {
            throw new IOException("Integer exceeds maximum valid value");
        }
        return bigInteger.intValue();
    }

    public byte[] getBitString(int i) throws IOException {
        if (i > available()) {
            throw new IOException("short read of bit string");
        }
        if (i == 0) {
            throw new IOException("Invalid encoding: zero length bit string");
        }
        byte b = this.buf[this.pos];
        if (b < 0 || b > 7) {
            throw new IOException("Invalid number of padding bits");
        }
        int i2 = i - 1;
        byte[] bArr = new byte[i2];
        System.arraycopy(this.buf, this.pos + 1, bArr, 0, i2);
        if (b != 0) {
            int i3 = i - 2;
            bArr[i3] = (byte) ((255 << b) & bArr[i3]);
        }
        skip(i);
        return bArr;
    }

    byte[] getBitString() throws IOException {
        return getBitString(available());
    }

    BitArray getUnalignedBitString() throws IOException {
        int length;
        if (this.pos >= this.count) {
            return null;
        }
        int iAvailable = available();
        int i = this.buf[this.pos] & Character.DIRECTIONALITY_UNDEFINED;
        if (i > 7) {
            throw new IOException("Invalid value for unused bits: " + i);
        }
        int i2 = iAvailable - 1;
        byte[] bArr = new byte[i2];
        if (bArr.length != 0) {
            length = (bArr.length * 8) - i;
        } else {
            length = 0;
        }
        System.arraycopy(this.buf, this.pos + 1, bArr, 0, i2);
        BitArray bitArray = new BitArray(length, bArr);
        this.pos = this.count;
        return bitArray;
    }

    public Date getUTCTime(int i) throws IOException {
        if (i > available()) {
            throw new IOException("short read of DER UTC Time");
        }
        if (i < 11 || i > 17) {
            throw new IOException("DER UTC Time length error");
        }
        return getTime(i, false);
    }

    public Date getGeneralizedTime(int i) throws IOException {
        if (i > available()) {
            throw new IOException("short read of DER Generalized Time");
        }
        if (i < 13 || i > 23) {
            throw new IOException("DER Generalized Time length error");
        }
        return getTime(i, true);
    }

    private Date getTime(int i, boolean z) throws IOException {
        String str;
        int iDigit;
        int i2;
        int i3;
        int iDigit2;
        int i4;
        if (z) {
            str = "Generalized";
            byte[] bArr = this.buf;
            int i5 = this.pos;
            this.pos = i5 + 1;
            int iDigit3 = Character.digit((char) bArr[i5], 10) * 1000;
            byte[] bArr2 = this.buf;
            int i6 = this.pos;
            this.pos = i6 + 1;
            int iDigit4 = iDigit3 + (Character.digit((char) bArr2[i6], 10) * 100);
            byte[] bArr3 = this.buf;
            int i7 = this.pos;
            this.pos = i7 + 1;
            int iDigit5 = iDigit4 + (Character.digit((char) bArr3[i7], 10) * 10);
            byte[] bArr4 = this.buf;
            int i8 = this.pos;
            this.pos = i8 + 1;
            iDigit = iDigit5 + Character.digit((char) bArr4[i8], 10);
            i2 = i - 2;
        } else {
            str = "UTC";
            byte[] bArr5 = this.buf;
            int i9 = this.pos;
            this.pos = i9 + 1;
            int iDigit6 = Character.digit((char) bArr5[i9], 10) * 10;
            byte[] bArr6 = this.buf;
            int i10 = this.pos;
            this.pos = i10 + 1;
            int iDigit7 = iDigit6 + Character.digit((char) bArr6[i10], 10);
            if (iDigit7 < 50) {
                iDigit = iDigit7 + Types.JAVA_OBJECT;
            } else {
                iDigit = iDigit7 + 1900;
            }
            i2 = i;
        }
        byte[] bArr7 = this.buf;
        int i11 = this.pos;
        this.pos = i11 + 1;
        int iDigit8 = Character.digit((char) bArr7[i11], 10) * 10;
        byte[] bArr8 = this.buf;
        int i12 = this.pos;
        this.pos = i12 + 1;
        int iDigit9 = iDigit8 + Character.digit((char) bArr8[i12], 10);
        byte[] bArr9 = this.buf;
        int i13 = this.pos;
        this.pos = i13 + 1;
        int iDigit10 = Character.digit((char) bArr9[i13], 10) * 10;
        byte[] bArr10 = this.buf;
        int i14 = this.pos;
        this.pos = i14 + 1;
        int iDigit11 = iDigit10 + Character.digit((char) bArr10[i14], 10);
        byte[] bArr11 = this.buf;
        int i15 = this.pos;
        this.pos = i15 + 1;
        int iDigit12 = Character.digit((char) bArr11[i15], 10) * 10;
        byte[] bArr12 = this.buf;
        int i16 = this.pos;
        this.pos = i16 + 1;
        int iDigit13 = iDigit12 + Character.digit((char) bArr12[i16], 10);
        byte[] bArr13 = this.buf;
        int i17 = this.pos;
        this.pos = i17 + 1;
        int iDigit14 = Character.digit((char) bArr13[i17], 10) * 10;
        byte[] bArr14 = this.buf;
        int i18 = this.pos;
        this.pos = i18 + 1;
        int iDigit15 = iDigit14 + Character.digit((char) bArr14[i18], 10);
        int i19 = i2 - 10;
        int iDigit16 = 0;
        if (i19 > 2 && i19 < 12) {
            byte[] bArr15 = this.buf;
            int i20 = this.pos;
            this.pos = i20 + 1;
            int iDigit17 = Character.digit((char) bArr15[i20], 10) * 10;
            byte[] bArr16 = this.buf;
            int i21 = this.pos;
            this.pos = i21 + 1;
            iDigit2 = iDigit17 + Character.digit((char) bArr16[i21], 10);
            int i22 = i19 - 2;
            if (this.buf[this.pos] == 46 || this.buf[this.pos] == 44) {
                int i23 = i22 - 1;
                this.pos++;
                int i24 = this.pos;
                int i25 = 0;
                for (byte b = 90; this.buf[i24] != b && this.buf[i24] != 43 && this.buf[i24] != 45; b = 90) {
                    i24++;
                    i25++;
                }
                switch (i25) {
                    case 1:
                        byte[] bArr17 = this.buf;
                        int i26 = this.pos;
                        this.pos = i26 + 1;
                        iDigit16 = 0 + (Character.digit((char) bArr17[i26], 10) * 100);
                        break;
                    case 2:
                        byte[] bArr18 = this.buf;
                        int i27 = this.pos;
                        this.pos = i27 + 1;
                        int iDigit18 = 0 + (Character.digit((char) bArr18[i27], 10) * 100);
                        byte[] bArr19 = this.buf;
                        int i28 = this.pos;
                        this.pos = i28 + 1;
                        iDigit16 = iDigit18 + (Character.digit((char) bArr19[i28], 10) * 10);
                        break;
                    case 3:
                        byte[] bArr20 = this.buf;
                        int i29 = this.pos;
                        this.pos = i29 + 1;
                        int iDigit19 = 0 + (Character.digit((char) bArr20[i29], 10) * 100);
                        byte[] bArr21 = this.buf;
                        int i30 = this.pos;
                        this.pos = i30 + 1;
                        int iDigit20 = iDigit19 + (Character.digit((char) bArr21[i30], 10) * 10);
                        byte[] bArr22 = this.buf;
                        int i31 = this.pos;
                        this.pos = i31 + 1;
                        iDigit16 = iDigit20 + Character.digit((char) bArr22[i31], 10);
                        break;
                    default:
                        throw new IOException("Parse " + str + " time, unsupported precision for seconds value");
                }
                i22 = i23 - i25;
            }
            i3 = i22;
            i4 = iDigit16;
        } else {
            i3 = i19;
            iDigit2 = 0;
            i4 = 0;
        }
        if (iDigit9 == 0 || iDigit11 == 0 || iDigit9 > 12 || iDigit11 > 31 || iDigit13 >= 24 || iDigit15 >= 60 || iDigit2 >= 60) {
            throw new IOException("Parse " + str + " time, invalid format");
        }
        Gregorian gregorianCalendar = CalendarSystem.getGregorianCalendar();
        CalendarDate calendarDateNewCalendarDate = gregorianCalendar.newCalendarDate((TimeZone) null);
        calendarDateNewCalendarDate.setDate(iDigit, iDigit9, iDigit11);
        calendarDateNewCalendarDate.setTimeOfDay(iDigit13, iDigit15, iDigit2, i4);
        long time = gregorianCalendar.getTime(calendarDateNewCalendarDate);
        if (i3 != 1 && i3 != 5) {
            throw new IOException("Parse " + str + " time, invalid offset");
        }
        byte[] bArr23 = this.buf;
        int i32 = this.pos;
        this.pos = i32 + 1;
        byte b2 = bArr23[i32];
        if (b2 == 43) {
            byte[] bArr24 = this.buf;
            int i33 = this.pos;
            this.pos = i33 + 1;
            int iDigit21 = Character.digit((char) bArr24[i33], 10) * 10;
            byte[] bArr25 = this.buf;
            int i34 = this.pos;
            this.pos = i34 + 1;
            int iDigit22 = iDigit21 + Character.digit((char) bArr25[i34], 10);
            byte[] bArr26 = this.buf;
            int i35 = this.pos;
            this.pos = i35 + 1;
            int iDigit23 = Character.digit((char) bArr26[i35], 10) * 10;
            byte[] bArr27 = this.buf;
            int i36 = this.pos;
            this.pos = i36 + 1;
            int iDigit24 = iDigit23 + Character.digit((char) bArr27[i36], 10);
            if (iDigit22 >= 24 || iDigit24 >= 60) {
                throw new IOException("Parse " + str + " time, +hhmm");
            }
            time -= (long) ((((iDigit22 * 60) + iDigit24) * 60) * 1000);
        } else if (b2 == 45) {
            byte[] bArr28 = this.buf;
            int i37 = this.pos;
            this.pos = i37 + 1;
            int iDigit25 = Character.digit((char) bArr28[i37], 10) * 10;
            byte[] bArr29 = this.buf;
            int i38 = this.pos;
            this.pos = i38 + 1;
            int iDigit26 = iDigit25 + Character.digit((char) bArr29[i38], 10);
            byte[] bArr30 = this.buf;
            int i39 = this.pos;
            this.pos = i39 + 1;
            int iDigit27 = Character.digit((char) bArr30[i39], 10) * 10;
            byte[] bArr31 = this.buf;
            int i40 = this.pos;
            this.pos = i40 + 1;
            int iDigit28 = iDigit27 + Character.digit((char) bArr31[i40], 10);
            if (iDigit26 >= 24 || iDigit28 >= 60) {
                throw new IOException("Parse " + str + " time, -hhmm");
            }
            time += (long) (((iDigit26 * 60) + iDigit28) * 60 * 1000);
        } else if (b2 != 90) {
            throw new IOException("Parse " + str + " time, garbage offset");
        }
        return new Date(time);
    }
}
