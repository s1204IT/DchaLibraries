package com.mediatek.internal.telephony;

import android.os.Handler;

public interface IMtkConcatenatedSmsFwk {
    public static final String ACTION_CLEAR_OUT_SEGMENTS = "android.sms.ACTION_CLEAR_OUT_SEGMENTS";
    public static final int EVENT_DISPATCH_CONCATE_SMS_SEGMENTS = 3001;
    public static final int OUT_OF_DATE_TIME = 43200000;
    public static final String SQL_3GPP2_SMS = " AND (destination_port & 262144=262144)";
    public static final String SQL_3GPP_SMS = " AND (destination_port & 131072=131072)";
    public static final int UPLOAD_FLAG_NEW = 1;
    public static final int UPLOAD_FLAG_NONE = 0;
    public static final String UPLOAD_FLAG_TAG = "upload_flag";
    public static final int UPLOAD_FLAG_UPDATE = 2;

    void cancelTimer(Handler handler, Object obj);

    void deleteExistedSegments(MtkTimerRecord mtkTimerRecord);

    int getUploadFlag(MtkTimerRecord mtkTimerRecord);

    boolean isFirstConcatenatedSegment(String str, int i, boolean z);

    boolean isLastConcatenatedSegment(String str, int i, int i2, boolean z);

    byte[][] queryExistedSegments(MtkTimerRecord mtkTimerRecord);

    MtkTimerRecord queryTimerRecord(String str, int i, int i2, boolean z);

    void refreshTimer(Handler handler, Object obj);

    void setPhoneId(int i);

    void setUploadFlag(MtkTimerRecord mtkTimerRecord);

    void startTimer(Handler handler, Object obj);
}
