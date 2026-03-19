package com.android.bluetooth.mapclient;

import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.android.bluetooth.map.BluetoothMapContentObserver;
import com.android.bluetooth.mapclient.Bmessage;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class EventReport {
    private static final String TAG = "EventReport";
    private final String mFolder;
    private final String mHandle;
    private final Bmessage.Type mMsgType;
    private final String mOldFolder;
    private final Type mType;

    private EventReport(HashMap<String, String> map) throws IllegalArgumentException {
        this.mType = parseType(map.get(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE));
        if (this.mType != Type.MEMORY_FULL && this.mType != Type.MEMORY_AVAILABLE) {
            String str = map.get("handle");
            try {
                new BigInteger(map.get("handle"), 16);
                this.mHandle = map.get("handle");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid value for handle:" + str);
            }
        } else {
            this.mHandle = null;
        }
        this.mFolder = map.get("folder");
        this.mOldFolder = map.get("old_folder");
        if (this.mType != Type.MEMORY_FULL && this.mType != Type.MEMORY_AVAILABLE) {
            String str2 = map.get("msg_type");
            if (str2 != null && str2.isEmpty()) {
                this.mMsgType = null;
                return;
            } else {
                this.mMsgType = parseMsgType(str2);
                return;
            }
        }
        this.mMsgType = null;
    }

    static EventReport fromStream(DataInputStream dataInputStream) {
        try {
            XmlPullParser xmlPullParserNewPullParser = XmlPullParserFactory.newInstance().newPullParser();
            xmlPullParserNewPullParser.setInput(dataInputStream, "utf-8");
            for (int eventType = xmlPullParserNewPullParser.getEventType(); eventType != 1; eventType = xmlPullParserNewPullParser.next()) {
                if (eventType == 2 && xmlPullParserNewPullParser.getName().equals(NotificationCompat.CATEGORY_EVENT)) {
                    HashMap map = new HashMap();
                    for (int i = 0; i < xmlPullParserNewPullParser.getAttributeCount(); i++) {
                        map.put(xmlPullParserNewPullParser.getAttributeName(i), xmlPullParserNewPullParser.getAttributeValue(i));
                    }
                    return new EventReport(map);
                }
            }
            return null;
        } catch (IOException e) {
            Log.e(TAG, "I/O error when parsing XML", e);
            return null;
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "Invalid event received", e2);
            return null;
        } catch (XmlPullParserException e3) {
            Log.e(TAG, "XML parser error when parsing XML", e3);
            return null;
        }
    }

    private Type parseType(String str) throws IllegalArgumentException {
        for (Type type : Type.values()) {
            if (type.toString().equals(str)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid value for type: " + str);
    }

    private Bmessage.Type parseMsgType(String str) throws IllegalArgumentException {
        for (Bmessage.Type type : Bmessage.Type.values()) {
            if (type.name().equals(str)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid value for msg_type: " + str);
    }

    public Type getType() {
        return this.mType;
    }

    public String getHandle() {
        return this.mHandle;
    }

    public String getFolder() {
        return this.mFolder;
    }

    public String getOldFolder() {
        return this.mOldFolder;
    }

    public Bmessage.Type getMsgType() {
        return this.mMsgType;
    }

    public String toString() {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE, this.mType);
            jSONObject.put("handle", this.mHandle);
            jSONObject.put("folder", this.mFolder);
            jSONObject.put("old_folder", this.mOldFolder);
            jSONObject.put("msg_type", this.mMsgType);
        } catch (JSONException e) {
        }
        return jSONObject.toString();
    }

    public enum Type {
        NEW_MESSAGE("NewMessage"),
        DELIVERY_SUCCESS("DeliverySuccess"),
        SENDING_SUCCESS("SendingSuccess"),
        DELIVERY_FAILURE("DeliveryFailure"),
        SENDING_FAILURE("SendingFailure"),
        MEMORY_FULL("MemoryFull"),
        MEMORY_AVAILABLE("MemoryAvailable"),
        MESSAGE_DELETED("MessageDeleted"),
        MESSAGE_SHIFT("MessageShift");

        private final String mSpecName;

        Type(String str) {
            this.mSpecName = str;
        }

        @Override
        public String toString() {
            return this.mSpecName;
        }
    }
}
