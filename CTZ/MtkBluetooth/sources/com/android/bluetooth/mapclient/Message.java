package com.android.bluetooth.mapclient;

import com.android.bluetooth.map.BluetoothMapContentObserver;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

public class Message {
    private final int mAttachmentSize;
    private final Date mDateTime;
    private final String mHandle;
    private final boolean mPriority;
    private final boolean mProtected;
    private final boolean mRead;
    private final ReceptionStatus mReceptionStatus;
    private final String mRecipientAddressing;
    private final String mRecipientName;
    private final String mReplytoAddressing;
    private final String mSenderAddressing;
    private final String mSenderName;
    private final boolean mSent;
    private final int mSize;
    private final String mSubject;
    private final boolean mText;
    private final Type mType;

    public enum ReceptionStatus {
        UNKNOWN,
        COMPLETE,
        FRACTIONED,
        NOTIFICATION
    }

    public enum Type {
        UNKNOWN,
        EMAIL,
        SMS_GSM,
        SMS_CDMA,
        MMS
    }

    Message(HashMap<String, String> map) throws IllegalArgumentException {
        int i;
        try {
            new BigInteger(map.get("handle"), 16);
            this.mHandle = map.get("handle");
            this.mSubject = map.get(BluetoothMapContract.MessageColumns.SUBJECT);
            String str = map.get("datetime");
            if (str != null) {
                this.mDateTime = new ObexTime(str).getTime();
            } else {
                this.mDateTime = null;
            }
            this.mSenderName = map.get("sender_name");
            this.mSenderAddressing = map.get("sender_addressing");
            this.mReplytoAddressing = map.get("replyto_addressing");
            this.mRecipientName = map.get("recipient_name");
            this.mRecipientAddressing = map.get("recipient_addressing");
            this.mType = strToType(map.get(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE));
            int i2 = 0;
            try {
                i = Integer.parseInt(map.get("size"));
            } catch (NumberFormatException e) {
                i = 0;
            }
            this.mSize = i;
            this.mText = yesnoToBoolean(map.get(BluetoothMapContract.MessagePartColumns.TEXT));
            this.mReceptionStatus = strToReceptionStatus(map.get("reception_status"));
            try {
                i2 = Integer.parseInt(map.get(BluetoothMapContract.MessageColumns.ATTACHMENT_SIZE));
            } catch (NumberFormatException e2) {
            }
            this.mAttachmentSize = i2;
            this.mPriority = yesnoToBoolean(map.get(BluetoothMapContract.PresenceColumns.PRIORITY));
            this.mRead = yesnoToBoolean(map.get(BluetoothMapContract.FILTER_READ_STATUS));
            this.mSent = yesnoToBoolean(map.get(BluetoothMapContract.DELIVERY_STATE_SENT));
            this.mProtected = yesnoToBoolean(map.get("protected"));
        } catch (NumberFormatException e3) {
            throw new IllegalArgumentException(e3);
        }
    }

    private boolean yesnoToBoolean(String str) {
        return "yes".equals(str);
    }

    private Type strToType(String str) {
        if ("EMAIL".equals(str)) {
            return Type.EMAIL;
        }
        if ("SMS_GSM".equals(str)) {
            return Type.SMS_GSM;
        }
        if ("SMS_CDMA".equals(str)) {
            return Type.SMS_CDMA;
        }
        if ("MMS".equals(str)) {
            return Type.MMS;
        }
        return Type.UNKNOWN;
    }

    private ReceptionStatus strToReceptionStatus(String str) {
        if (BluetoothMapContract.RECEPTION_STATE_COMPLETE.equals(str)) {
            return ReceptionStatus.COMPLETE;
        }
        if (BluetoothMapContract.RECEPTION_STATE_FRACTIONED.equals(str)) {
            return ReceptionStatus.FRACTIONED;
        }
        if (BluetoothMapContract.RECEPTION_STATE_NOTIFICATION.equals(str)) {
            return ReceptionStatus.NOTIFICATION;
        }
        return ReceptionStatus.UNKNOWN;
    }

    public String toString() {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("handle", this.mHandle);
            jSONObject.put(BluetoothMapContract.MessageColumns.SUBJECT, this.mSubject);
            jSONObject.put("datetime", this.mDateTime);
            jSONObject.put("sender_name", this.mSenderName);
            jSONObject.put("sender_addressing", this.mSenderAddressing);
            jSONObject.put("replyto_addressing", this.mReplytoAddressing);
            jSONObject.put("recipient_name", this.mRecipientName);
            jSONObject.put("recipient_addressing", this.mRecipientAddressing);
            jSONObject.put(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE, this.mType);
            jSONObject.put("size", this.mSize);
            jSONObject.put(BluetoothMapContract.MessagePartColumns.TEXT, this.mText);
            jSONObject.put("reception_status", this.mReceptionStatus);
            jSONObject.put(BluetoothMapContract.MessageColumns.ATTACHMENT_SIZE, this.mAttachmentSize);
            jSONObject.put(BluetoothMapContract.PresenceColumns.PRIORITY, this.mPriority);
            jSONObject.put(BluetoothMapContract.FILTER_READ_STATUS, this.mRead);
            jSONObject.put(BluetoothMapContract.DELIVERY_STATE_SENT, this.mSent);
            jSONObject.put("protected", this.mProtected);
        } catch (JSONException e) {
        }
        return jSONObject.toString();
    }

    public String getHandle() {
        return this.mHandle;
    }

    public String getSubject() {
        return this.mSubject;
    }

    public Date getDateTime() {
        return this.mDateTime;
    }

    public String getSenderName() {
        return this.mSenderName;
    }

    public String getSenderAddressing() {
        return this.mSenderAddressing;
    }

    public String getReplytoAddressing() {
        return this.mReplytoAddressing;
    }

    public String getRecipientName() {
        return this.mRecipientName;
    }

    public String getRecipientAddressing() {
        return this.mRecipientAddressing;
    }

    public Type getType() {
        return this.mType;
    }

    public int getSize() {
        return this.mSize;
    }

    public ReceptionStatus getReceptionStatus() {
        return this.mReceptionStatus;
    }

    public int getAttachmentSize() {
        return this.mAttachmentSize;
    }

    public boolean isText() {
        return this.mText;
    }

    public boolean isPriority() {
        return this.mPriority;
    }

    public boolean isRead() {
        return this.mRead;
    }

    public boolean isSent() {
        return this.mSent;
    }

    public boolean isProtected() {
        return this.mProtected;
    }
}
