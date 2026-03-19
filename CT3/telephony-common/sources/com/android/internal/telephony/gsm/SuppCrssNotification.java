package com.android.internal.telephony.gsm;

import android.telephony.PhoneNumberUtils;

public class SuppCrssNotification {
    public static final int CRSS_CALLED_LINE_ID_PREST = 1;
    public static final int CRSS_CALLING_LINE_ID_PREST = 2;
    public static final int CRSS_CALL_WAITING = 0;
    public static final int CRSS_CONNECTED_LINE_ID_PREST = 3;
    public String alphaid;
    public int cli_validity;
    public int code;
    public String number;
    public int type;

    public String toString() {
        return super.toString() + " CRSS Notification: code: " + this.code + " \"" + PhoneNumberUtils.stringFromStringAndTOA(this.number, this.type) + "\" " + this.alphaid + " cli_validity: " + this.cli_validity;
    }
}
