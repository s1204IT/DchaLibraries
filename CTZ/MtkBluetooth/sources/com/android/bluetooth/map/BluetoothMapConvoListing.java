package com.android.bluetooth.map;

import android.util.Log;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class BluetoothMapConvoListing {
    private static final boolean D = BluetoothMapService.DEBUG;
    private static final String TAG = "BluetoothMapConvoListing";
    private static final String XML_TAG = "MAP-convo-listing";
    private boolean mHasUnread = false;
    private List<BluetoothMapConvoListingElement> mList = new ArrayList();

    public void add(BluetoothMapConvoListingElement bluetoothMapConvoListingElement) {
        this.mList.add(bluetoothMapConvoListingElement);
        if (bluetoothMapConvoListingElement.getReadBool()) {
            this.mHasUnread = true;
        }
    }

    public int getCount() {
        if (this.mList != null) {
            return this.mList.size();
        }
        return 0;
    }

    public boolean hasUnread() {
        return this.mHasUnread;
    }

    public List<BluetoothMapConvoListingElement> getList() {
        return this.mList;
    }

    public byte[] encode() throws UnsupportedEncodingException {
        StringWriter stringWriter = new StringWriter();
        XmlSerializer fastXmlSerializer = new FastXmlSerializer();
        try {
            fastXmlSerializer.setOutput(stringWriter);
            fastXmlSerializer.startDocument("UTF-8", true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, XML_TAG);
            fastXmlSerializer.attribute(null, "version", "1.0");
            Iterator<BluetoothMapConvoListingElement> it = this.mList.iterator();
            while (it.hasNext()) {
                it.next().encode(fastXmlSerializer);
            }
            fastXmlSerializer.endTag(null, XML_TAG);
            fastXmlSerializer.endDocument();
        } catch (IOException e) {
            Log.w(TAG, e);
        } catch (IllegalArgumentException e2) {
            Log.w(TAG, e2);
        } catch (IllegalStateException e3) {
            Log.w(TAG, e3);
        }
        return stringWriter.toString().getBytes("UTF-8");
    }

    public void sort() {
        Collections.sort(this.mList);
    }

    public void segment(int i, int i2) {
        int iMin = Math.min(i, this.mList.size() - i2);
        if (iMin > 0) {
            this.mList = this.mList.subList(i2, iMin + i2);
            if (this.mList == null) {
                this.mList = new ArrayList();
                return;
            }
            return;
        }
        if (i2 > this.mList.size()) {
            this.mList = new ArrayList();
            Log.d(TAG, "offset greater than list size. Returning empty list");
        } else {
            this.mList = this.mList.subList(i2, this.mList.size());
        }
    }

    public void appendFromXml(InputStream inputStream) throws XmlPullParserException, IOException, ParseException {
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(inputStream, "UTF-8");
            while (true) {
                int next = xmlPullParserNewPullParser.next();
                if (next == 3 || next == 1) {
                    break;
                }
                if (xmlPullParserNewPullParser.getEventType() == 2) {
                    String name = xmlPullParserNewPullParser.getName();
                    if (!name.equalsIgnoreCase(XML_TAG)) {
                        if (D) {
                            Log.i(TAG, "Unknown XML tag: " + name);
                        }
                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                    }
                    readConversations(xmlPullParserNewPullParser);
                }
            }
        } finally {
            inputStream.close();
        }
    }

    private void readConversations(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException, ParseException {
        if (D) {
            Log.i(TAG, "readConversations(): ");
        }
        while (true) {
            int next = xmlPullParser.next();
            if (next != 3 && next != 1) {
                if (xmlPullParser.getEventType() == 2) {
                    String name = xmlPullParser.getName();
                    if (!name.trim().equalsIgnoreCase(BluetoothMapConvoListingElement.XML_TAG_CONVERSATION)) {
                        if (D) {
                            Log.i(TAG, "Unknown XML tag: " + name);
                        }
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    } else {
                        add(BluetoothMapConvoListingElement.createFromXml(xmlPullParser));
                    }
                }
            } else {
                return;
            }
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BluetoothMapConvoListing bluetoothMapConvoListing = (BluetoothMapConvoListing) obj;
        if (this.mHasUnread != bluetoothMapConvoListing.mHasUnread) {
            return false;
        }
        if (this.mList == null) {
            if (bluetoothMapConvoListing.mList != null) {
                return false;
            }
        } else if (!this.mList.equals(bluetoothMapConvoListing.mList)) {
            return false;
        }
        return true;
    }
}
