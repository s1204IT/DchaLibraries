package com.android.bluetooth.map;

import android.support.v4.app.NotificationCompat;
import com.android.bluetooth.DeviceWorkArounds;
import com.android.bluetooth.map.BluetoothMapUtils;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.xmlpull.v1.XmlSerializer;

public class BluetoothMapMessageListingElement implements Comparable<BluetoothMapMessageListingElement> {
    private static final boolean D = false;
    private static final String TAG = "BluetoothMapMessageListingElement";
    private static final boolean V = false;
    private long mCpHandle = 0;
    private String mSubject = null;
    private long mDateTime = 0;
    private String mSenderName = null;
    private String mSenderAddressing = null;
    private String mReplytoAddressing = null;
    private String mRecipientName = null;
    private String mRecipientAddressing = null;
    private BluetoothMapUtils.TYPE mType = null;
    private boolean mMsgTypeAppParamSet = false;
    private int mSize = -1;
    private String mText = null;
    private String mReceptionStatus = null;
    private String mDeliveryStatus = null;
    private int mAttachmentSize = -1;
    private String mPriority = null;
    private boolean mRead = false;
    private String mSent = null;
    private String mProtect = null;
    private String mFolderType = null;
    private String mThreadId = null;
    private String mThreadName = null;
    private String mAttachmentMimeTypes = null;
    private boolean mReportRead = false;
    private int mCursorIndex = 0;

    public int getCursorIndex() {
        return this.mCursorIndex;
    }

    public void setCursorIndex(int i) {
        this.mCursorIndex = i;
    }

    public long getHandle() {
        return this.mCpHandle;
    }

    public void setHandle(long j) {
        this.mCpHandle = j;
    }

    public long getDateTime() {
        return this.mDateTime;
    }

    public String getDateTimeString() {
        return new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date(this.mDateTime));
    }

    public void setDateTime(long j) {
        this.mDateTime = j;
    }

    public String getSubject() {
        return this.mSubject;
    }

    public void setSubject(String str) {
        this.mSubject = str;
    }

    public String getSenderName() {
        return this.mSenderName;
    }

    public void setSenderName(String str) {
        this.mSenderName = str;
    }

    public String getSenderAddressing() {
        return this.mSenderAddressing;
    }

    public void setSenderAddressing(String str) {
        this.mSenderAddressing = str;
    }

    public String getReplyToAddressing() {
        return this.mReplytoAddressing;
    }

    public void setReplytoAddressing(String str) {
        this.mReplytoAddressing = str;
    }

    public String getRecipientName() {
        return this.mRecipientName;
    }

    public void setRecipientName(String str) {
        this.mRecipientName = str;
    }

    public String getRecipientAddressing() {
        return this.mRecipientAddressing;
    }

    public void setRecipientAddressing(String str) {
        this.mRecipientAddressing = str;
    }

    public BluetoothMapUtils.TYPE getType() {
        return this.mType;
    }

    public void setType(BluetoothMapUtils.TYPE type, boolean z) {
        this.mMsgTypeAppParamSet = z;
        this.mType = type;
    }

    public int getSize() {
        return this.mSize;
    }

    public void setSize(int i) {
        this.mSize = i;
    }

    public String getText() {
        return this.mText;
    }

    public void setText(String str) {
        this.mText = str;
    }

    public String getReceptionStatus() {
        return this.mReceptionStatus;
    }

    public void setReceptionStatus(String str) {
        this.mReceptionStatus = str;
    }

    public String getDeliveryStatus() {
        return this.mDeliveryStatus;
    }

    public void setDeliveryStatus(String str) {
        this.mDeliveryStatus = str;
    }

    public int getAttachmentSize() {
        return this.mAttachmentSize;
    }

    public void setAttachmentSize(int i) {
        this.mAttachmentSize = i;
    }

    public String getAttachmentMimeTypes() {
        return this.mAttachmentMimeTypes;
    }

    public void setAttachmentMimeTypes(String str) {
        this.mAttachmentMimeTypes = str;
    }

    public String getPriority() {
        return this.mPriority;
    }

    public void setPriority(String str) {
        this.mPriority = str;
    }

    public String getRead() {
        return this.mRead ? "yes" : "no";
    }

    public boolean getReadBool() {
        return this.mRead;
    }

    public void setRead(boolean z, boolean z2) {
        this.mRead = z;
        this.mReportRead = z2;
    }

    public String getSent() {
        return this.mSent;
    }

    public void setSent(String str) {
        this.mSent = str;
    }

    public String getProtect() {
        return this.mProtect;
    }

    public void setProtect(String str) {
        this.mProtect = str;
    }

    public void setThreadId(long j, BluetoothMapUtils.TYPE type) {
        if (j != -1) {
            this.mThreadId = BluetoothMapUtils.getMapConvoHandle(j, type);
        }
    }

    public String getThreadName() {
        return this.mThreadName;
    }

    public void setThreadName(String str) {
        this.mThreadName = str;
    }

    public String getFolderType() {
        return this.mFolderType;
    }

    public void setFolderType(String str) {
        this.mFolderType = str;
    }

    @Override
    public int compareTo(BluetoothMapMessageListingElement bluetoothMapMessageListingElement) {
        if (this.mDateTime < bluetoothMapMessageListingElement.mDateTime) {
            return 1;
        }
        if (this.mDateTime > bluetoothMapMessageListingElement.mDateTime) {
            return -1;
        }
        return 0;
    }

    public void encode(XmlSerializer xmlSerializer, boolean z) throws IllegalStateException, IOException, IllegalArgumentException {
        this.mSubject = BluetoothMapUtils.removeInvalidChar(this.mSubject);
        this.mRecipientName = BluetoothMapUtils.removeInvalidChar(this.mRecipientName);
        this.mRecipientAddressing = BluetoothMapUtils.removeInvalidChar(this.mRecipientAddressing);
        this.mSenderName = BluetoothMapUtils.removeInvalidChar(this.mSenderName);
        this.mSenderAddressing = BluetoothMapUtils.removeInvalidChar(this.mSenderAddressing);
        this.mReplytoAddressing = BluetoothMapUtils.removeInvalidChar(this.mReplytoAddressing);
        if (!BluetoothMapUtils.isLegalArgument(this.mSubject) || !BluetoothMapUtils.isLegalArgument(this.mRecipientName) || !BluetoothMapUtils.isLegalArgument(this.mRecipientAddressing) || !BluetoothMapUtils.isLegalArgument(this.mSenderName) || !BluetoothMapUtils.isLegalArgument(this.mSenderAddressing) || !BluetoothMapUtils.isLegalArgument(this.mReplytoAddressing)) {
            return;
        }
        xmlSerializer.startTag(null, NotificationCompat.CATEGORY_MESSAGE);
        xmlSerializer.attribute(null, "handle", BluetoothMapUtils.getMapHandle(this.mCpHandle, this.mType));
        if (this.mSubject != null) {
            String strStripInvalidChars = BluetoothMapUtils.stripInvalidChars(this.mSubject);
            if (DeviceWorkArounds.addressStartsWith(BluetoothMapService.getRemoteDevice().getAddress(), DeviceWorkArounds.MERCEDES_BENZ_CARKIT)) {
                strStripInvalidChars = strStripInvalidChars.replaceAll("[\\P{ASCII}&\"><]", "");
                if (strStripInvalidChars.isEmpty()) {
                    strStripInvalidChars = "---";
                }
            }
            xmlSerializer.attribute(null, BluetoothMapContract.MessageColumns.SUBJECT, strStripInvalidChars.substring(0, strStripInvalidChars.length() < 256 ? strStripInvalidChars.length() : 256));
        }
        if (this.mDateTime != 0) {
            xmlSerializer.attribute(null, "datetime", getDateTimeString());
        }
        if (this.mSenderName != null) {
            xmlSerializer.attribute(null, "sender_name", BluetoothMapUtils.stripInvalidChars(this.mSenderName));
        }
        if (this.mSenderAddressing != null) {
            xmlSerializer.attribute(null, "sender_addressing", this.mSenderAddressing);
        }
        if (this.mReplytoAddressing != null) {
            xmlSerializer.attribute(null, "replyto_addressing", this.mReplytoAddressing);
        }
        if (this.mRecipientName != null) {
            xmlSerializer.attribute(null, "recipient_name", BluetoothMapUtils.stripInvalidChars(this.mRecipientName));
        }
        if (this.mRecipientAddressing != null) {
            xmlSerializer.attribute(null, "recipient_addressing", this.mRecipientAddressing);
        }
        if (this.mMsgTypeAppParamSet && this.mType != null) {
            xmlSerializer.attribute(null, BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE, this.mType.name());
        }
        if (this.mSize != -1) {
            xmlSerializer.attribute(null, "size", Integer.toString(this.mSize));
        }
        if (this.mText != null) {
            xmlSerializer.attribute(null, BluetoothMapContract.MessagePartColumns.TEXT, this.mText);
        }
        if (this.mReceptionStatus != null) {
            xmlSerializer.attribute(null, "reception_status", this.mReceptionStatus);
        }
        if (this.mDeliveryStatus != null) {
            xmlSerializer.attribute(null, "delivery_status", this.mDeliveryStatus);
        }
        if (this.mAttachmentSize != -1) {
            xmlSerializer.attribute(null, BluetoothMapContract.MessageColumns.ATTACHMENT_SIZE, Integer.toString(this.mAttachmentSize));
        }
        if (this.mAttachmentMimeTypes != null) {
            xmlSerializer.attribute(null, BluetoothMapContract.MessageColumns.ATTACHMENT_MINE_TYPES, this.mAttachmentMimeTypes);
        }
        if (this.mPriority != null) {
            xmlSerializer.attribute(null, BluetoothMapContract.PresenceColumns.PRIORITY, this.mPriority);
        }
        if (this.mReportRead) {
            xmlSerializer.attribute(null, BluetoothMapContract.FILTER_READ_STATUS, getRead());
        }
        if (this.mSent != null) {
            xmlSerializer.attribute(null, BluetoothMapContract.DELIVERY_STATE_SENT, this.mSent);
        }
        if (this.mProtect != null) {
            xmlSerializer.attribute(null, "protected", this.mProtect);
        }
        if (this.mThreadId != null && z) {
            xmlSerializer.attribute(null, "conversation_id", this.mThreadId);
        }
        if (this.mThreadName != null && z) {
            xmlSerializer.attribute(null, "conversation_name", this.mThreadName);
        }
        if (this.mFolderType != null) {
            xmlSerializer.attribute(null, "folder_type", this.mFolderType);
        }
        xmlSerializer.endTag(null, NotificationCompat.CATEGORY_MESSAGE);
    }
}
