package com.mediatek.internal.telephony;

import android.util.Log;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ConferenceCallMessageHandler extends DefaultHandler {
    public static final String STATUS_ALERTING = "alerting";
    public static final String STATUS_CONNECTED = "connected";
    public static final String STATUS_CONNECT_FAIL = "connect-fail";
    public static final String STATUS_DIALING_IN = "dialing-in";
    public static final String STATUS_DIALING_OUT = "dialing-out";
    public static final String STATUS_DISCONNECTED = "disconnected";
    public static final String STATUS_DISCONNECTING = "disconnecting";
    public static final String STATUS_MUTED_VIA_FOCUS = "muted-via-focus";
    public static final String STATUS_ON_HOLD = "on-hold";
    public static final String STATUS_PENDING = "pending";
    private static final String TAG = "ConferenceCallMessageHandler";
    private String mHostInfo;
    private int mMaxUserCount;
    private String mTag;
    private String mTempVal;
    private User mUser;
    private List<User> mUsers;
    private int mCallId = -1;
    private int mIndex = 0;
    private int mUserCount = -1;
    private boolean mParsingHostInfo = false;

    public class User {
        private String mDisplayText;
        private String mEndPoint;
        private String mEntity;
        private int mIndex;
        private String mSipTelUri;
        private String mStatus = ConferenceCallMessageHandler.STATUS_DISCONNECTED;
        private int mConnectionIndex = -1;

        public User() {
        }

        void setEndPoint(String endPoint) {
            this.mEndPoint = endPoint;
        }

        void setEntity(String entity) {
            this.mEntity = entity;
        }

        void setSipTelUri(String uri) {
            this.mSipTelUri = uri;
        }

        void setDisplayText(String displayText) {
            this.mDisplayText = displayText;
        }

        void setStatus(String status) {
            this.mStatus = status;
        }

        void setIndex(int index) {
            this.mIndex = index;
        }

        public void setConnectionIndex(int index) {
            this.mConnectionIndex = index;
        }

        public String getEndPoint() {
            return this.mEndPoint;
        }

        public String getEntity() {
            return this.mEntity;
        }

        public String getSipTelUri() {
            return this.mSipTelUri;
        }

        public String getDisplayText() {
            return this.mDisplayText;
        }

        public String getStatus() {
            return this.mStatus;
        }

        public int getIndex() {
            return this.mIndex;
        }

        public int getConnectionIndex() {
            return this.mConnectionIndex;
        }
    }

    public List<User> getUsers() {
        return this.mUsers;
    }

    private void setMaxUserCount(String maxUserCount) {
        this.mMaxUserCount = Integer.parseInt(maxUserCount);
    }

    public int getMaxUserCount() {
        return this.mMaxUserCount;
    }

    public void setCallId(int callId) {
        this.mCallId = callId;
    }

    public int getCallId() {
        return this.mCallId;
    }

    @Override
    public void startDocument() throws SAXException {
        this.mUsers = new ArrayList();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.mTag == null) {
            Log.d(TAG, "Parse val failed: tag is null");
            return;
        }
        String val = new String(ch, start, length);
        Log.d(TAG, "Current tag: " + this.mTag + " val: " + val);
        if (this.mTag.equalsIgnoreCase("maximum-user-count")) {
            setMaxUserCount(val);
            Log.d(TAG, "MaxUserCount: " + getMaxUserCount());
        } else if (this.mTag.equalsIgnoreCase("user-count")) {
            this.mUserCount = Integer.parseInt(val);
            Log.d(TAG, "UserCount: " + getUserCount());
        } else if (this.mParsingHostInfo && this.mTag.equalsIgnoreCase("uri")) {
            this.mHostInfo = val;
            Log.d(TAG, "host-info: " + getHostInfo());
        }
        if (this.mUser == null) {
            Log.d(TAG, "Parse val failed: user is null");
            return;
        }
        if (this.mTag.equalsIgnoreCase("display-text")) {
            this.mUser.setDisplayText(val);
            Log.d(TAG, "user - DisplayText: " + this.mUser.getDisplayText());
        } else {
            if (!this.mTag.equalsIgnoreCase("status")) {
                return;
            }
            this.mUser.setStatus(val);
            Log.d(TAG, "Status: " + this.mUser.getStatus());
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("user")) {
            this.mIndex++;
            this.mUser = new User();
            this.mUser.setIndex(this.mIndex);
            this.mUser.setEntity(attributes.getValue(UsimPBMemInfo.STRING_NOT_SET, "entity"));
            Log.d(TAG, "user - entity: " + this.mUser.getEntity());
        } else if (qName.equalsIgnoreCase("endpoint")) {
            this.mUser.setEndPoint(attributes.getValue(UsimPBMemInfo.STRING_NOT_SET, "entity"));
            Log.d(TAG, "endpoint - entity: " + this.mUser.getEndPoint());
        } else if (qName.equalsIgnoreCase("host-info")) {
            this.mParsingHostInfo = true;
            Log.d(TAG, "start parsing host info");
        }
        this.mTag = qName;
        Log.d(TAG, "Conference uri: " + uri + ", localName: " + localName);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("user") && this.mUsers != null) {
            this.mUsers.add(this.mUser);
            this.mUser = null;
        } else if (qName.equalsIgnoreCase("host-info")) {
            this.mParsingHostInfo = false;
            Log.d(TAG, "end parsing host info");
        }
        this.mTag = null;
    }

    public int getUserCount() {
        return this.mUserCount;
    }

    public String getHostInfo() {
        return this.mHostInfo;
    }
}
