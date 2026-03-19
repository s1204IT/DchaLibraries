package com.android.bluetooth.map;

import android.util.Log;
import com.android.bluetooth.SignedLongLong;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class BluetoothMapConvoContactElement implements Comparable<BluetoothMapConvoContactElement> {
    public static final long CONTACT_ID_TYPE_EMAIL = 2;
    public static final long CONTACT_ID_TYPE_IM = 3;
    public static final long CONTACT_ID_TYPE_SMS_MMS = 1;
    private static final boolean D = false;
    private static final String TAG = "BluetoothMapConvoContactElement";
    private static final boolean V = false;
    private static final String XML_ATT_CHAT_STATE = "chat_state";
    private static final String XML_ATT_DISPLAY_NAME = "display_name";
    private static final String XML_ATT_LAST_ACTIVITY = "last_activity";
    private static final String XML_ATT_NAME = "name";
    private static final String XML_ATT_PRESENCE_AVAILABILITY = "presence_availability";
    private static final String XML_ATT_PRESENCE_STATUS = "presence_status";
    private static final String XML_ATT_PRIORITY = "priority";
    private static final String XML_ATT_UCI = "x_bt_uci";
    private static final String XML_ATT_X_BT_UID = "x_bt_uid";
    protected static final String XML_TAG_CONVOCONTACT = "convocontact";
    private SignedLongLong mBtUid;
    private int mChatState;
    private String mDisplayName;
    private long mLastActivity;
    private String mName;
    private int mPresenceAvailability;
    private String mPresenceStatus;
    private int mPriority;
    private String mUci;

    public static BluetoothMapConvoContactElement createFromMapContact(MapContact mapContact, String str) {
        BluetoothMapConvoContactElement bluetoothMapConvoContactElement = new BluetoothMapConvoContactElement();
        bluetoothMapConvoContactElement.mUci = str;
        bluetoothMapConvoContactElement.mBtUid = new SignedLongLong(mapContact.getId(), 0L);
        bluetoothMapConvoContactElement.mDisplayName = mapContact.getName();
        return bluetoothMapConvoContactElement;
    }

    public BluetoothMapConvoContactElement(String str, String str2, String str3, String str4, int i, long j, int i2, int i3, String str5) {
        this.mUci = null;
        this.mName = null;
        this.mDisplayName = null;
        this.mPresenceStatus = null;
        this.mPresenceAvailability = -1;
        this.mPriority = -1;
        this.mLastActivity = -1L;
        this.mBtUid = null;
        this.mChatState = -1;
        this.mUci = str;
        this.mName = str2;
        this.mDisplayName = str3;
        this.mPresenceStatus = str4;
        this.mPresenceAvailability = i;
        this.mLastActivity = j;
        this.mChatState = i2;
        this.mPresenceStatus = str4;
        this.mPriority = i3;
        if (str5 != null) {
            try {
                this.mBtUid = SignedLongLong.fromString(str5);
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, e);
            }
        }
    }

    public BluetoothMapConvoContactElement() {
        this.mUci = null;
        this.mName = null;
        this.mDisplayName = null;
        this.mPresenceStatus = null;
        this.mPresenceAvailability = -1;
        this.mPriority = -1;
        this.mLastActivity = -1L;
        this.mBtUid = null;
        this.mChatState = -1;
    }

    public String getPresenceStatus() {
        return this.mPresenceStatus;
    }

    public String getDisplayName() {
        return this.mDisplayName;
    }

    public void setDisplayName(String str) {
        this.mDisplayName = str;
    }

    public void setPresenceStatus(String str) {
        this.mPresenceStatus = str;
    }

    public int getPresenceAvailability() {
        return this.mPresenceAvailability;
    }

    public void setPresenceAvailability(int i) {
        this.mPresenceAvailability = i;
    }

    public int getPriority() {
        return this.mPriority;
    }

    public void setPriority(int i) {
        this.mPriority = i;
    }

    public String getName() {
        return this.mName;
    }

    public void setName(String str) {
        this.mName = str;
    }

    public String getBtUid() {
        return this.mBtUid.toHexString();
    }

    public void setBtUid(SignedLongLong signedLongLong) {
        this.mBtUid = signedLongLong;
    }

    public int getChatState() {
        return this.mChatState;
    }

    public void setChatState(int i) {
        this.mChatState = i;
    }

    public void setChatState(String str) {
        this.mChatState = Integer.valueOf(str).intValue();
    }

    public String getLastActivityString() {
        return new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date(this.mLastActivity));
    }

    public void setLastActivity(long j) {
        this.mLastActivity = j;
    }

    public void setLastActivity(String str) throws ParseException {
        this.mLastActivity = new SimpleDateFormat("yyyyMMdd'T'HHmmss").parse(str).getTime();
    }

    public void setContactId(String str) {
        this.mUci = str;
    }

    public String getContactId() {
        return this.mUci;
    }

    @Override
    public int compareTo(BluetoothMapConvoContactElement bluetoothMapConvoContactElement) {
        if (this.mLastActivity < bluetoothMapConvoContactElement.mLastActivity) {
            return 1;
        }
        if (this.mLastActivity > bluetoothMapConvoContactElement.mLastActivity) {
            return -1;
        }
        return 0;
    }

    public void encode(XmlSerializer xmlSerializer) throws IllegalStateException, IOException, IllegalArgumentException {
        xmlSerializer.startTag(null, XML_TAG_CONVOCONTACT);
        if (this.mUci != null) {
            xmlSerializer.attribute(null, "x_bt_uci", this.mUci);
        }
        if (this.mDisplayName != null) {
            xmlSerializer.attribute(null, XML_ATT_DISPLAY_NAME, BluetoothMapUtils.stripInvalidChars(this.mDisplayName));
        }
        if (this.mName != null) {
            xmlSerializer.attribute(null, "name", BluetoothMapUtils.stripInvalidChars(this.mName));
        }
        if (this.mChatState != -1) {
            xmlSerializer.attribute(null, "chat_state", String.valueOf(this.mChatState));
        }
        if (this.mLastActivity != -1) {
            xmlSerializer.attribute(null, XML_ATT_LAST_ACTIVITY, getLastActivityString());
        }
        if (this.mBtUid != null) {
            xmlSerializer.attribute(null, "x_bt_uid", this.mBtUid.toHexString());
        }
        if (this.mPresenceAvailability != -1) {
            xmlSerializer.attribute(null, XML_ATT_PRESENCE_AVAILABILITY, String.valueOf(this.mPresenceAvailability));
        }
        if (this.mPresenceStatus != null) {
            xmlSerializer.attribute(null, XML_ATT_PRESENCE_STATUS, this.mPresenceStatus);
        }
        if (this.mPriority != -1) {
            xmlSerializer.attribute(null, "priority", String.valueOf(this.mPriority));
        }
        xmlSerializer.endTag(null, XML_TAG_CONVOCONTACT);
    }

    public static BluetoothMapConvoContactElement createFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException, ParseException {
        int attributeCount = xmlPullParser.getAttributeCount();
        if (attributeCount < 1) {
            throw new IllegalArgumentException("convocontact is not decorated with attributes");
        }
        BluetoothMapConvoContactElement bluetoothMapConvoContactElement = new BluetoothMapConvoContactElement();
        for (int i = 0; i < attributeCount; i++) {
            String strTrim = xmlPullParser.getAttributeName(i).trim();
            String attributeValue = xmlPullParser.getAttributeValue(i);
            if (strTrim.equalsIgnoreCase("x_bt_uci")) {
                bluetoothMapConvoContactElement.mUci = attributeValue;
            } else if (strTrim.equalsIgnoreCase("name")) {
                bluetoothMapConvoContactElement.mName = attributeValue;
            } else if (strTrim.equalsIgnoreCase(XML_ATT_DISPLAY_NAME)) {
                bluetoothMapConvoContactElement.mDisplayName = attributeValue;
            } else if (strTrim.equalsIgnoreCase("chat_state")) {
                bluetoothMapConvoContactElement.setChatState(attributeValue);
            } else if (strTrim.equalsIgnoreCase(XML_ATT_LAST_ACTIVITY)) {
                bluetoothMapConvoContactElement.setLastActivity(attributeValue);
            } else if (strTrim.equalsIgnoreCase("x_bt_uid")) {
                bluetoothMapConvoContactElement.setBtUid(SignedLongLong.fromString(attributeValue));
            } else if (strTrim.equalsIgnoreCase(XML_ATT_PRESENCE_AVAILABILITY)) {
                bluetoothMapConvoContactElement.mPresenceAvailability = Integer.parseInt(attributeValue);
            } else if (strTrim.equalsIgnoreCase(XML_ATT_PRESENCE_STATUS)) {
                bluetoothMapConvoContactElement.setPresenceStatus(attributeValue);
            } else if (strTrim.equalsIgnoreCase("priority")) {
                bluetoothMapConvoContactElement.setPriority(Integer.parseInt(attributeValue));
            }
        }
        xmlPullParser.nextTag();
        return bluetoothMapConvoContactElement;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BluetoothMapConvoContactElement bluetoothMapConvoContactElement = (BluetoothMapConvoContactElement) obj;
        if (this.mChatState != bluetoothMapConvoContactElement.mChatState) {
            return false;
        }
        if (this.mDisplayName == null) {
            if (bluetoothMapConvoContactElement.mDisplayName != null) {
                return false;
            }
        } else if (!this.mDisplayName.equals(bluetoothMapConvoContactElement.mDisplayName)) {
            return false;
        }
        if (this.mLastActivity != bluetoothMapConvoContactElement.mLastActivity) {
            return false;
        }
        if (this.mName == null) {
            if (bluetoothMapConvoContactElement.mName != null) {
                return false;
            }
        } else if (!this.mName.equals(bluetoothMapConvoContactElement.mName)) {
            return false;
        }
        if (this.mPresenceAvailability != bluetoothMapConvoContactElement.mPresenceAvailability) {
            return false;
        }
        if (this.mPresenceStatus == null) {
            if (bluetoothMapConvoContactElement.mPresenceStatus != null) {
                return false;
            }
        } else if (!this.mPresenceStatus.equals(bluetoothMapConvoContactElement.mPresenceStatus)) {
            return false;
        }
        if (this.mPriority == bluetoothMapConvoContactElement.mPriority) {
            return true;
        }
        return false;
    }
}
