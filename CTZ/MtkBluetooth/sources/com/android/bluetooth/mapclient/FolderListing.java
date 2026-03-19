package com.android.bluetooth.mapclient;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

class FolderListing {
    private static final String TAG = "FolderListing";
    private final ArrayList<String> mFolders = new ArrayList<>();

    FolderListing(InputStream inputStream) {
        parse(inputStream);
    }

    public void parse(InputStream inputStream) {
        try {
            XmlPullParser xmlPullParserNewPullParser = XmlPullParserFactory.newInstance().newPullParser();
            xmlPullParserNewPullParser.setInput(inputStream, "utf-8");
            for (int eventType = xmlPullParserNewPullParser.getEventType(); eventType != 1; eventType = xmlPullParserNewPullParser.next()) {
                if (eventType == 2 && xmlPullParserNewPullParser.getName().equals("folder")) {
                    this.mFolders.add(xmlPullParserNewPullParser.getAttributeValue(null, "name"));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "I/O error when parsing XML", e);
        } catch (XmlPullParserException e2) {
            Log.e(TAG, "XML parser error when parsing XML", e2);
        }
    }

    public ArrayList<String> getList() {
        return this.mFolders;
    }
}
