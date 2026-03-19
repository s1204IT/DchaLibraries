package com.android.bluetooth.mapclient;

import android.support.v4.app.NotificationCompat;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

class MessagesListing {
    private static final String TAG = "MessagesListing";
    private final ArrayList<Message> mMessages = new ArrayList<>();

    MessagesListing(InputStream inputStream) {
        parse(inputStream);
    }

    public void parse(InputStream inputStream) {
        try {
            XmlPullParser xmlPullParserNewPullParser = XmlPullParserFactory.newInstance().newPullParser();
            xmlPullParserNewPullParser.setInput(inputStream, "utf-8");
            for (int eventType = xmlPullParserNewPullParser.getEventType(); eventType != 1; eventType = xmlPullParserNewPullParser.next()) {
                if (eventType == 2 && xmlPullParserNewPullParser.getName().equals(NotificationCompat.CATEGORY_MESSAGE)) {
                    HashMap map = new HashMap();
                    for (int i = 0; i < xmlPullParserNewPullParser.getAttributeCount(); i++) {
                        map.put(xmlPullParserNewPullParser.getAttributeName(i), xmlPullParserNewPullParser.getAttributeValue(i));
                    }
                    try {
                        this.mMessages.add(new Message(map));
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Invalid <msg/>");
                    }
                }
            }
        } catch (IOException e2) {
            Log.e(TAG, "I/O error when parsing XML", e2);
        } catch (XmlPullParserException e3) {
            Log.e(TAG, "XML parser error when parsing XML", e3);
        }
    }

    public ArrayList<Message> getList() {
        return this.mMessages;
    }
}
