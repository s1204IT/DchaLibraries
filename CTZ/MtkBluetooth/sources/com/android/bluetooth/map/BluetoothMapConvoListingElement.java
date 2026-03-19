package com.android.bluetooth.map;

import android.util.Log;
import com.android.bluetooth.SignedLongLong;
import com.android.bluetooth.map.BluetoothMapUtils;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class BluetoothMapConvoListingElement implements Comparable<BluetoothMapConvoListingElement> {
    private static final boolean D = BluetoothMapService.DEBUG;
    private static final String TAG = "BluetoothMapConvoListingElement";
    private static final boolean V = false;
    private static final String XML_ATT_ID = "id";
    private static final String XML_ATT_LAST_ACTIVITY = "last_activity";
    private static final String XML_ATT_NAME = "name";
    private static final String XML_ATT_READ = "readstatus";
    private static final String XML_ATT_SUMMARY = "summary";
    private static final String XML_ATT_VERSION_COUNTER = "version_counter";
    public static final String XML_TAG_CONVERSATION = "conversation";
    private List<BluetoothMapConvoContactElement> mContacts;
    private SignedLongLong mId = null;
    private String mName = "";
    private long mLastActivity = -1;
    private boolean mRead = false;
    private boolean mReportRead = false;
    private long mVersionCounter = -1;
    private int mCursorIndex = 0;
    private BluetoothMapUtils.TYPE mType = null;
    private String mSummary = null;
    private String mSmsMmsContacts = null;

    public int getCursorIndex() {
        return this.mCursorIndex;
    }

    public void setCursorIndex(int i) {
        this.mCursorIndex = i;
        if (D) {
            Log.d(TAG, "setCursorIndex: " + i);
        }
    }

    public long getVersionCounter() {
        return this.mVersionCounter;
    }

    public void setVersionCounter(long j) {
        if (D) {
            Log.d(TAG, "setVersionCounter: " + j);
        }
        this.mVersionCounter = j;
    }

    public void incrementVersionCounter() {
        this.mVersionCounter++;
    }

    private void setVersionCounter(String str) {
        if (D) {
            Log.d(TAG, "setVersionCounter: " + str);
        }
        try {
            this.mVersionCounter = Long.parseLong(str);
        } catch (NumberFormatException e) {
            Log.w(TAG, "unable to parse XML versionCounter:" + str);
            this.mVersionCounter = -1L;
        }
    }

    public String getName() {
        return this.mName;
    }

    public void setName(String str) {
        if (D) {
            Log.d(TAG, "setName: " + str);
        }
        this.mName = str;
    }

    public BluetoothMapUtils.TYPE getType() {
        return this.mType;
    }

    public void setType(BluetoothMapUtils.TYPE type) {
        this.mType = type;
    }

    public List<BluetoothMapConvoContactElement> getContacts() {
        return this.mContacts;
    }

    public void setContacts(List<BluetoothMapConvoContactElement> list) {
        this.mContacts = list;
    }

    public void addContact(BluetoothMapConvoContactElement bluetoothMapConvoContactElement) {
        if (this.mContacts == null) {
            this.mContacts = new ArrayList();
        }
        this.mContacts.add(bluetoothMapConvoContactElement);
    }

    public void removeContact(BluetoothMapConvoContactElement bluetoothMapConvoContactElement) {
        this.mContacts.remove(bluetoothMapConvoContactElement);
    }

    public void removeContact(int i) {
        this.mContacts.remove(i);
    }

    public long getLastActivity() {
        return this.mLastActivity;
    }

    public String getLastActivityString() {
        return new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date(this.mLastActivity));
    }

    public void setLastActivity(long j) {
        if (D) {
            Log.d(TAG, "setLastActivity: " + j);
        }
        this.mLastActivity = j;
    }

    public void setLastActivity(String str) throws ParseException {
        this.mLastActivity = new SimpleDateFormat("yyyyMMdd'T'HHmmss").parse(str).getTime();
    }

    public String getRead() {
        if (this.mReportRead) {
            return this.mRead ? "READ" : "UNREAD";
        }
        return "UNKNOWN";
    }

    public boolean getReadBool() {
        return this.mRead;
    }

    public void setRead(boolean z, boolean z2) {
        this.mRead = z;
        if (D) {
            Log.d(TAG, "setRead: " + z);
        }
        this.mReportRead = z2;
    }

    private void setRead(String str) {
        if (str.trim().equalsIgnoreCase("yes")) {
            this.mRead = true;
        } else {
            this.mRead = false;
        }
        this.mReportRead = true;
    }

    public void setConvoId(long j, long j2) {
        this.mId = new SignedLongLong(j2, j);
        if (D) {
            Log.d(TAG, "setConvoId: " + j2 + " type:" + j);
        }
    }

    public String getConvoId() {
        return this.mId.toHexString();
    }

    public long getCpConvoId() {
        return this.mId.getLeastSignificantBits();
    }

    public void setSummary(String str) {
        this.mSummary = str;
    }

    public String getFullSummary() {
        return this.mSummary;
    }

    private String getSummary() {
        if (this.mSummary != null) {
            try {
                return new String(BluetoothMapUtils.truncateUtf8StringToBytearray(this.mSummary, 256), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Missing UTF-8 support on platform", e);
                return null;
            }
        }
        return null;
    }

    public String getSmsMmsContacts() {
        return this.mSmsMmsContacts;
    }

    public void setSmsMmsContacts(String str) {
        this.mSmsMmsContacts = str;
    }

    @Override
    public int compareTo(BluetoothMapConvoListingElement bluetoothMapConvoListingElement) {
        if (this.mLastActivity < bluetoothMapConvoListingElement.mLastActivity) {
            return 1;
        }
        if (this.mLastActivity > bluetoothMapConvoListingElement.mLastActivity) {
            return -1;
        }
        return 0;
    }

    public void encode(XmlSerializer xmlSerializer) throws IllegalStateException, IOException, IllegalArgumentException {
        xmlSerializer.startTag(null, XML_TAG_CONVERSATION);
        xmlSerializer.attribute(null, XML_ATT_ID, this.mId.toHexString());
        if (this.mName != null) {
            xmlSerializer.attribute(null, "name", BluetoothMapUtils.stripInvalidChars(this.mName));
        }
        if (this.mLastActivity != -1) {
            xmlSerializer.attribute(null, XML_ATT_LAST_ACTIVITY, getLastActivityString());
        }
        if (this.mReportRead) {
            xmlSerializer.attribute(null, XML_ATT_READ, getRead());
        }
        if (this.mVersionCounter != -1) {
            xmlSerializer.attribute(null, "version_counter", Long.toString(getVersionCounter()));
        }
        if (this.mSummary != null) {
            xmlSerializer.attribute(null, XML_ATT_SUMMARY, getSummary());
        }
        if (this.mContacts != null) {
            Iterator<BluetoothMapConvoContactElement> it = this.mContacts.iterator();
            while (it.hasNext()) {
                it.next().encode(xmlSerializer);
            }
        }
        xmlSerializer.endTag(null, XML_TAG_CONVERSATION);
    }

    public static BluetoothMapConvoListingElement createFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException, ParseException {
        BluetoothMapConvoListingElement bluetoothMapConvoListingElement = new BluetoothMapConvoListingElement();
        int attributeCount = xmlPullParser.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            String strTrim = xmlPullParser.getAttributeName(i).trim();
            String attributeValue = xmlPullParser.getAttributeValue(i);
            if (strTrim.equalsIgnoreCase(XML_ATT_ID)) {
                bluetoothMapConvoListingElement.mId = SignedLongLong.fromString(attributeValue);
            } else if (strTrim.equalsIgnoreCase("name")) {
                bluetoothMapConvoListingElement.mName = attributeValue;
            } else if (strTrim.equalsIgnoreCase(XML_ATT_LAST_ACTIVITY)) {
                bluetoothMapConvoListingElement.setLastActivity(attributeValue);
            } else if (strTrim.equalsIgnoreCase(XML_ATT_READ)) {
                bluetoothMapConvoListingElement.setRead(attributeValue);
            } else if (strTrim.equalsIgnoreCase("version_counter")) {
                bluetoothMapConvoListingElement.setVersionCounter(attributeValue);
            } else if (strTrim.equalsIgnoreCase(XML_ATT_SUMMARY)) {
                bluetoothMapConvoListingElement.setSummary(attributeValue);
            } else if (D) {
                Log.i(TAG, "Unknown XML attribute: " + xmlPullParser.getAttributeName(i));
            }
        }
        while (true) {
            int next = xmlPullParser.next();
            if (next == 3 || next == 1) {
                break;
            }
            if (xmlPullParser.getEventType() == 2) {
                String strTrim2 = xmlPullParser.getName().trim();
                if (strTrim2.equalsIgnoreCase("convocontact")) {
                    bluetoothMapConvoListingElement.addContact(BluetoothMapConvoContactElement.createFromXml(xmlPullParser));
                } else {
                    if (D) {
                        Log.i(TAG, "Unknown XML tag: " + strTrim2);
                    }
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
        return bluetoothMapConvoListingElement;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BluetoothMapConvoListingElement bluetoothMapConvoListingElement = (BluetoothMapConvoListingElement) obj;
        if (this.mContacts == null) {
            if (bluetoothMapConvoListingElement.mContacts != null) {
                return false;
            }
        } else if (!this.mContacts.equals(bluetoothMapConvoListingElement.mContacts)) {
            return false;
        }
        if (this.mLastActivity != bluetoothMapConvoListingElement.mLastActivity) {
            return false;
        }
        if (this.mName == null) {
            if (bluetoothMapConvoListingElement.mName != null) {
                return false;
            }
        } else if (!this.mName.equals(bluetoothMapConvoListingElement.mName)) {
            return false;
        }
        if (this.mRead == bluetoothMapConvoListingElement.mRead) {
            return true;
        }
        return false;
    }
}
