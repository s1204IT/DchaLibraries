package com.android.internal.telephony.cat;

import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.internal.telephony.cat.AppInterface;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.TimeZone;

class DTTZResponseData extends ResponseData {
    private Calendar mCalendar;

    public DTTZResponseData(Calendar calendar) {
        this.mCalendar = calendar;
    }

    @Override
    public void format(ByteArrayOutputStream byteArrayOutputStream) {
        if (byteArrayOutputStream == null) {
            return;
        }
        byteArrayOutputStream.write(128 | AppInterface.CommandType.PROVIDE_LOCAL_INFORMATION.value());
        byte[] bArr = new byte[8];
        bArr[0] = 7;
        if (this.mCalendar == null) {
            this.mCalendar = Calendar.getInstance();
        }
        bArr[1] = byteToBCD(this.mCalendar.get(1) % 100);
        bArr[2] = byteToBCD(this.mCalendar.get(2) + 1);
        bArr[3] = byteToBCD(this.mCalendar.get(5));
        bArr[4] = byteToBCD(this.mCalendar.get(11));
        bArr[5] = byteToBCD(this.mCalendar.get(12));
        bArr[6] = byteToBCD(this.mCalendar.get(13));
        String str = SystemProperties.get("persist.sys.timezone", "");
        if (TextUtils.isEmpty(str)) {
            bArr[7] = -1;
        } else {
            TimeZone timeZone = TimeZone.getTimeZone(str);
            bArr[7] = getTZOffSetByte(timeZone.getRawOffset() + timeZone.getDSTSavings());
        }
        for (byte b : bArr) {
            byteArrayOutputStream.write(b);
        }
    }

    private byte byteToBCD(int i) {
        if (i < 0 && i > 99) {
            CatLog.d(this, "Err: byteToBCD conversion Value is " + i + " Value has to be between 0 and 99");
            return (byte) 0;
        }
        return (byte) (((i % 10) << 4) | (i / 10));
    }

    private byte getTZOffSetByte(long j) {
        boolean z;
        if (j >= 0) {
            z = false;
        } else {
            z = true;
        }
        byte bByteToBCD = byteToBCD((int) (((long) (z ? -1 : 1)) * (j / 900000)));
        return z ? (byte) (bByteToBCD | 8) : bByteToBCD;
    }
}
