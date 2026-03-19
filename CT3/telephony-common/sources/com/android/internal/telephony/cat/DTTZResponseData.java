package com.android.internal.telephony.cat;

import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.internal.telephony.cat.AppInterface;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.TimeZone;

class DTTZResponseData extends ResponseData {
    private Calendar mCalendar;

    public DTTZResponseData(Calendar cal) {
        this.mCalendar = cal;
    }

    @Override
    public void format(ByteArrayOutputStream buf) {
        if (buf == null) {
            return;
        }
        int tag = AppInterface.CommandType.PROVIDE_LOCAL_INFORMATION.value() | 128;
        buf.write(tag);
        byte[] data = new byte[8];
        data[0] = 7;
        if (this.mCalendar == null) {
            this.mCalendar = Calendar.getInstance();
        }
        data[1] = byteToBCD(this.mCalendar.get(1) % 100);
        data[2] = byteToBCD(this.mCalendar.get(2) + 1);
        data[3] = byteToBCD(this.mCalendar.get(5));
        data[4] = byteToBCD(this.mCalendar.get(11));
        data[5] = byteToBCD(this.mCalendar.get(12));
        data[6] = byteToBCD(this.mCalendar.get(13));
        String tz = SystemProperties.get("persist.sys.timezone", UsimPBMemInfo.STRING_NOT_SET);
        if (TextUtils.isEmpty(tz)) {
            data[7] = -1;
        } else {
            TimeZone zone = TimeZone.getTimeZone(tz);
            int zoneOffset = zone.getRawOffset() + zone.getDSTSavings();
            data[7] = getTZOffSetByte(zoneOffset);
        }
        for (byte b : data) {
            buf.write(b);
        }
    }

    private byte byteToBCD(int value) {
        if (value < 0 && value > 99) {
            CatLog.d(this, "Err: byteToBCD conversion Value is " + value + " Value has to be between 0 and 99");
            return (byte) 0;
        }
        return (byte) ((value / 10) | ((value % 10) << 4));
    }

    private byte getTZOffSetByte(long offSetVal) {
        int i;
        boolean isNegative = offSetVal < 0;
        long tzOffset = offSetVal / 900000;
        byte bcdVal = byteToBCD((int) (tzOffset * ((long) (isNegative ? -1 : 1))));
        if (isNegative) {
            i = bcdVal | 8;
        } else {
            i = bcdVal;
        }
        return (byte) i;
    }
}
