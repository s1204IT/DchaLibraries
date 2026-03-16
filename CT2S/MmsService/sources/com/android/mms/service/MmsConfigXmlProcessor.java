package com.android.mms.service;

import android.content.ContentValues;
import android.util.Log;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class MmsConfigXmlProcessor {
    private final XmlPullParser mInputParser;
    private final StringBuilder mLogStringBuilder = new StringBuilder();
    private MmsConfigHandler mMmsConfigHandler = null;

    public interface MmsConfigHandler {
        void process(String str, String str2, String str3);
    }

    private MmsConfigXmlProcessor(XmlPullParser parser) {
        this.mInputParser = parser;
    }

    public static MmsConfigXmlProcessor get(XmlPullParser parser) {
        return new MmsConfigXmlProcessor(parser);
    }

    public MmsConfigXmlProcessor setMmsConfigHandler(MmsConfigHandler handler) {
        this.mMmsConfigHandler = handler;
        return this;
    }

    private int advanceToNextEvent(int eventType) throws XmlPullParserException, IOException {
        int nextEvent;
        do {
            nextEvent = this.mInputParser.next();
            if (nextEvent == eventType) {
                break;
            }
        } while (nextEvent != 1);
        return nextEvent;
    }

    public void process() {
        try {
            if (advanceToNextEvent(2) != 2) {
                throw new XmlPullParserException("MmsConfigXmlProcessor: expecting start tag @" + xmlParserDebugContext());
            }
            new ContentValues();
            String tagName = this.mInputParser.getName();
            if ("mms_config".equals(tagName)) {
                processMmsConfig();
            }
        } catch (IOException e) {
            Log.e("MmsService", "MmsConfigXmlProcessor: I/O failure " + e, e);
        } catch (XmlPullParserException e2) {
            Log.e("MmsService", "MmsConfigXmlProcessor: parsing failure " + e2, e2);
        }
    }

    private static String xmlParserEventString(int event) {
        switch (event) {
            case 0:
                return "START_DOCUMENT";
            case 1:
                return "END_DOCUMENT";
            case 2:
                return "START_TAG";
            case 3:
                return "END_TAG";
            case 4:
                return "TEXT";
            default:
                return Integer.toString(event);
        }
    }

    private String xmlParserDebugContext() {
        this.mLogStringBuilder.setLength(0);
        if (this.mInputParser != null) {
            try {
                int eventType = this.mInputParser.getEventType();
                this.mLogStringBuilder.append(xmlParserEventString(eventType));
                if (eventType == 2 || eventType == 3 || eventType == 4) {
                    this.mLogStringBuilder.append('<').append(this.mInputParser.getName());
                    for (int i = 0; i < this.mInputParser.getAttributeCount(); i++) {
                        this.mLogStringBuilder.append(' ').append(this.mInputParser.getAttributeName(i)).append('=').append(this.mInputParser.getAttributeValue(i));
                    }
                    this.mLogStringBuilder.append("/>");
                }
                return this.mLogStringBuilder.toString();
            } catch (XmlPullParserException e) {
                Log.e("MmsService", "xmlParserDebugContext: " + e, e);
            }
        }
        return "Unknown";
    }

    private void processMmsConfig() throws XmlPullParserException, IOException {
        int nextEvent;
        while (true) {
            nextEvent = this.mInputParser.next();
            if (nextEvent != 4) {
                if (nextEvent != 2) {
                    break;
                } else {
                    processMmsConfigKeyValue();
                }
            }
        }
        if (nextEvent != 3) {
            throw new XmlPullParserException("MmsConfig: expecting start or end tag @" + xmlParserDebugContext());
        }
    }

    private void processMmsConfigKeyValue() throws XmlPullParserException, IOException {
        String key = this.mInputParser.getAttributeValue(null, "name");
        String type = this.mInputParser.getName();
        int nextEvent = this.mInputParser.next();
        String value = null;
        if (nextEvent == 4) {
            value = this.mInputParser.getText();
            nextEvent = this.mInputParser.next();
        }
        if (nextEvent != 3) {
            throw new XmlPullParserException("MmsConfigXmlProcessor: expecting end tag @" + xmlParserDebugContext());
        }
        if (MmsConfig.isValidKey(key, type)) {
            if (this.mMmsConfigHandler != null) {
                this.mMmsConfigHandler.process(key, value, type);
                return;
            }
            return;
        }
        Log.w("MmsService", "MmsConfig: invalid key=" + key + " or type=" + type);
    }
}
