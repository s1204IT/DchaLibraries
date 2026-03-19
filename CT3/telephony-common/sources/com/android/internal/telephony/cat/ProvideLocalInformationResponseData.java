package com.android.internal.telephony.cat;

import java.io.ByteArrayOutputStream;

class ProvideLocalInformationResponseData extends ResponseData {
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

    public ProvideLocalInformationResponseData(int year, int month, int day, int hour, int minute, int second, int timezone) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.timezone = timezone;
    }

    public ProvideLocalInformationResponseData(byte[] language) {
        this.language = language;
    }

    public ProvideLocalInformationResponseData(int batteryState) {
        this.mBatteryState = batteryState;
    }

    @Override
    public void format(ByteArrayOutputStream buf) {
        if (this.mIsDate) {
            int tag = ComprehensionTlvTag.DATE_TIME_AND_TIMEZONE.value() | 128;
            buf.write(tag);
            buf.write(7);
            buf.write(this.year);
            buf.write(this.month);
            buf.write(this.day);
            buf.write(this.hour);
            buf.write(this.minute);
            buf.write(this.second);
            buf.write(this.timezone);
            return;
        }
        if (this.mIsLanguage) {
            int tag2 = ComprehensionTlvTag.LANGUAGE.value() | 128;
            buf.write(tag2);
            buf.write(2);
            for (byte b : this.language) {
                buf.write(b);
            }
            return;
        }
        if (!this.mIsBatteryState) {
            return;
        }
        int tag3 = ComprehensionTlvTag.BATTERY_STATE.value() | 128;
        buf.write(tag3);
        buf.write(1);
        buf.write(this.mBatteryState);
    }
}
