package com.android.bluetooth.mapclient;

import java.util.Date;

public final class MessagesFilter {
    public static final byte MESSAGE_TYPE_ALL = 0;
    public static final byte MESSAGE_TYPE_EMAIL = 4;
    public static final byte MESSAGE_TYPE_MMS = 8;
    public static final byte MESSAGE_TYPE_SMS_CDMA = 2;
    public static final byte MESSAGE_TYPE_SMS_GSM = 1;
    public static final byte PRIORITY_ANY = 0;
    public static final byte PRIORITY_HIGH = 1;
    public static final byte PRIORITY_NON_HIGH = 2;
    public static final byte READ_STATUS_ANY = 0;
    public static final byte READ_STATUS_READ = 2;
    public static final byte READ_STATUS_UNREAD = 1;
    public byte messageType = 0;
    public String periodBegin = null;
    public String periodEnd = null;
    public byte readStatus = 0;
    public String recipient = null;
    public String originator = null;
    public byte priority = 0;

    public void setMessageType(byte b) {
        this.messageType = b;
    }

    public void setPeriod(Date date, Date date2) {
        if (date != null) {
            this.periodBegin = new ObexTime(date).toString();
        }
        if (date2 != null) {
            this.periodEnd = new ObexTime(date2).toString();
        }
    }

    public void setReadStatus(byte b) {
        this.readStatus = b;
    }

    public void setRecipient(String str) {
        if (str != null && str.isEmpty()) {
            this.recipient = null;
        } else {
            this.recipient = str;
        }
    }

    public void setOriginator(String str) {
        if (str != null && str.isEmpty()) {
            this.originator = null;
        } else {
            this.originator = str;
        }
    }

    public void setPriority(byte b) {
        this.priority = b;
    }
}
