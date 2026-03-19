package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.ComprehensionTlvTag;
import com.android.internal.telephony.cat.ResponseData;
import java.io.ByteArrayOutputStream;

class MtkProvideLocalInformationResponseData extends ResponseData {
    private int day;
    private int hour;
    private byte[] language;
    private int mBatteryState;
    private int minute;
    private int month;
    private int second;
    private int timezone;
    private int year;
    private boolean mIsDate = false;
    private boolean mIsLanguage = false;
    private boolean mIsBatteryState = true;

    public MtkProvideLocalInformationResponseData(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        this.year = i;
        this.month = i2;
        this.day = i3;
        this.hour = i4;
        this.minute = i5;
        this.second = i6;
        this.timezone = i7;
    }

    public MtkProvideLocalInformationResponseData(byte[] bArr) {
        this.language = bArr;
    }

    public MtkProvideLocalInformationResponseData(int i) {
        this.mBatteryState = i;
    }

    public void format(ByteArrayOutputStream byteArrayOutputStream) {
        if (this.mIsDate) {
            byteArrayOutputStream.write(ComprehensionTlvTag.DATE_TIME_AND_TIMEZONE.value() | 128);
            byteArrayOutputStream.write(7);
            byteArrayOutputStream.write(this.year);
            byteArrayOutputStream.write(this.month);
            byteArrayOutputStream.write(this.day);
            byteArrayOutputStream.write(this.hour);
            byteArrayOutputStream.write(this.minute);
            byteArrayOutputStream.write(this.second);
            byteArrayOutputStream.write(this.timezone);
            return;
        }
        if (!this.mIsLanguage) {
            if (this.mIsBatteryState) {
                byteArrayOutputStream.write(ComprehensionTlvTag.BATTERY_STATE.value() | 128);
                byteArrayOutputStream.write(1);
                byteArrayOutputStream.write(this.mBatteryState);
                return;
            }
            return;
        }
        byteArrayOutputStream.write(ComprehensionTlvTag.LANGUAGE.value() | 128);
        byteArrayOutputStream.write(2);
        for (byte b : this.language) {
            byteArrayOutputStream.write(b);
        }
    }
}
