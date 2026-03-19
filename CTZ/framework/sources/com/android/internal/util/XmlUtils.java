package com.android.internal.util;

import android.app.slice.Slice;
import android.app.slice.SliceItem;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Xml;
import com.android.ims.ImsConfig;
import com.android.internal.midi.MidiConstants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class XmlUtils {
    private static final String STRING_ARRAY_SEPARATOR = ":";

    public interface ReadMapCallback {
        Object readThisUnknownObjectXml(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException;
    }

    public interface WriteMapCallback {
        void writeUnknownObject(Object obj, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException;
    }

    public static void skipCurrentTag(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next == 3 && xmlPullParser.getDepth() <= depth) {
                    return;
                }
            } else {
                return;
            }
        }
    }

    public static final int convertValueToList(CharSequence charSequence, String[] strArr, int i) {
        if (charSequence != null) {
            for (int i2 = 0; i2 < strArr.length; i2++) {
                if (charSequence.equals(strArr[i2])) {
                    return i2;
                }
            }
        }
        return i;
    }

    public static final boolean convertValueToBoolean(CharSequence charSequence, boolean z) {
        if (charSequence == null) {
            return z;
        }
        if (charSequence.equals(WifiEnterpriseConfig.ENGINE_ENABLE) || charSequence.equals("true") || charSequence.equals("TRUE")) {
            return true;
        }
        return false;
    }

    public static final int convertValueToInt(CharSequence charSequence, int i) {
        int i2;
        int i3;
        if (charSequence == null) {
            return i;
        }
        String string = charSequence.toString();
        int length = string.length();
        int i4 = 10;
        if ('-' == string.charAt(0)) {
            i3 = -1;
            i2 = 1;
        } else {
            i2 = 0;
            i3 = 1;
        }
        if ('0' == string.charAt(i2)) {
            if (i2 == length - 1) {
                return 0;
            }
            int i5 = i2 + 1;
            char cCharAt = string.charAt(i5);
            if ('x' == cCharAt || 'X' == cCharAt) {
                i2 += 2;
                i4 = 16;
            } else {
                i4 = 8;
                i2 = i5;
            }
        } else if ('#' == string.charAt(i2)) {
            i2++;
            i4 = 16;
        }
        return Integer.parseInt(string.substring(i2), i4) * i3;
    }

    public static int convertValueToUnsignedInt(String str, int i) {
        if (str == null) {
            return i;
        }
        return parseUnsignedIntAttribute(str);
    }

    public static int parseUnsignedIntAttribute(CharSequence charSequence) {
        String string = charSequence.toString();
        int length = string.length();
        int i = 0;
        int i2 = 16;
        if ('0' != string.charAt(0)) {
            if ('#' == string.charAt(0)) {
                i = 1;
            } else {
                i2 = 10;
            }
        } else {
            if (length - 1 == 0) {
                return 0;
            }
            char cCharAt = string.charAt(1);
            if ('x' != cCharAt && 'X' != cCharAt) {
                i2 = 8;
                i = 1;
            } else {
                i = 2;
            }
        }
        return (int) Long.parseLong(string.substring(i), i2);
    }

    public static final void writeMapXml(Map map, OutputStream outputStream) throws XmlPullParserException, IOException {
        FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
        fastXmlSerializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
        fastXmlSerializer.startDocument(null, true);
        fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        writeMapXml(map, (String) null, fastXmlSerializer);
        fastXmlSerializer.endDocument();
    }

    public static final void writeListXml(List list, OutputStream outputStream) throws XmlPullParserException, IOException {
        XmlSerializer xmlSerializerNewSerializer = Xml.newSerializer();
        xmlSerializerNewSerializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
        xmlSerializerNewSerializer.startDocument(null, true);
        xmlSerializerNewSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        writeListXml(list, null, xmlSerializerNewSerializer);
        xmlSerializerNewSerializer.endDocument();
    }

    public static final void writeMapXml(Map map, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        writeMapXml(map, str, xmlSerializer, null);
    }

    public static final void writeMapXml(Map map, String str, XmlSerializer xmlSerializer, WriteMapCallback writeMapCallback) throws XmlPullParserException, IOException {
        if (map == null) {
            xmlSerializer.startTag(null, "null");
            xmlSerializer.endTag(null, "null");
            return;
        }
        xmlSerializer.startTag(null, "map");
        if (str != null) {
            xmlSerializer.attribute(null, "name", str);
        }
        writeMapXml(map, xmlSerializer, writeMapCallback);
        xmlSerializer.endTag(null, "map");
    }

    public static final void writeMapXml(Map map, XmlSerializer xmlSerializer, WriteMapCallback writeMapCallback) throws XmlPullParserException, IOException {
        if (map == null) {
            return;
        }
        for (Map.Entry entry : map.entrySet()) {
            writeValueXml(entry.getValue(), (String) entry.getKey(), xmlSerializer, writeMapCallback);
        }
    }

    public static final void writeListXml(List list, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        if (list == null) {
            xmlSerializer.startTag(null, "null");
            xmlSerializer.endTag(null, "null");
            return;
        }
        xmlSerializer.startTag(null, Slice.HINT_LIST);
        if (str != null) {
            xmlSerializer.attribute(null, "name", str);
        }
        int size = list.size();
        for (int i = 0; i < size; i++) {
            writeValueXml(list.get(i), null, xmlSerializer);
        }
        xmlSerializer.endTag(null, Slice.HINT_LIST);
    }

    public static final void writeSetXml(Set set, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        if (set == null) {
            xmlSerializer.startTag(null, "null");
            xmlSerializer.endTag(null, "null");
            return;
        }
        xmlSerializer.startTag(null, "set");
        if (str != null) {
            xmlSerializer.attribute(null, "name", str);
        }
        Iterator it = set.iterator();
        while (it.hasNext()) {
            writeValueXml(it.next(), null, xmlSerializer);
        }
        xmlSerializer.endTag(null, "set");
    }

    public static final void writeByteArrayXml(byte[] bArr, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        if (bArr == null) {
            xmlSerializer.startTag(null, "null");
            xmlSerializer.endTag(null, "null");
            return;
        }
        xmlSerializer.startTag(null, "byte-array");
        if (str != null) {
            xmlSerializer.attribute(null, "name", str);
        }
        xmlSerializer.attribute(null, "num", Integer.toString(bArr.length));
        StringBuilder sb = new StringBuilder(bArr.length * 2);
        for (byte b : bArr) {
            int i = (b >> 4) & 15;
            sb.append((char) (i >= 10 ? (i + 97) - 10 : i + 48));
            int i2 = b & MidiConstants.STATUS_CHANNEL_MASK;
            sb.append((char) (i2 >= 10 ? (97 + i2) - 10 : 48 + i2));
        }
        xmlSerializer.text(sb.toString());
        xmlSerializer.endTag(null, "byte-array");
    }

    public static final void writeIntArrayXml(int[] iArr, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        if (iArr == null) {
            xmlSerializer.startTag(null, "null");
            xmlSerializer.endTag(null, "null");
            return;
        }
        xmlSerializer.startTag(null, "int-array");
        if (str != null) {
            xmlSerializer.attribute(null, "name", str);
        }
        xmlSerializer.attribute(null, "num", Integer.toString(iArr.length));
        for (int i : iArr) {
            xmlSerializer.startTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
            xmlSerializer.attribute(null, "value", Integer.toString(i));
            xmlSerializer.endTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
        }
        xmlSerializer.endTag(null, "int-array");
    }

    public static final void writeLongArrayXml(long[] jArr, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        if (jArr == null) {
            xmlSerializer.startTag(null, "null");
            xmlSerializer.endTag(null, "null");
            return;
        }
        xmlSerializer.startTag(null, "long-array");
        if (str != null) {
            xmlSerializer.attribute(null, "name", str);
        }
        xmlSerializer.attribute(null, "num", Integer.toString(jArr.length));
        for (long j : jArr) {
            xmlSerializer.startTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
            xmlSerializer.attribute(null, "value", Long.toString(j));
            xmlSerializer.endTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
        }
        xmlSerializer.endTag(null, "long-array");
    }

    public static final void writeDoubleArrayXml(double[] dArr, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        if (dArr == null) {
            xmlSerializer.startTag(null, "null");
            xmlSerializer.endTag(null, "null");
            return;
        }
        xmlSerializer.startTag(null, "double-array");
        if (str != null) {
            xmlSerializer.attribute(null, "name", str);
        }
        xmlSerializer.attribute(null, "num", Integer.toString(dArr.length));
        for (double d : dArr) {
            xmlSerializer.startTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
            xmlSerializer.attribute(null, "value", Double.toString(d));
            xmlSerializer.endTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
        }
        xmlSerializer.endTag(null, "double-array");
    }

    public static final void writeStringArrayXml(String[] strArr, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        if (strArr == null) {
            xmlSerializer.startTag(null, "null");
            xmlSerializer.endTag(null, "null");
            return;
        }
        xmlSerializer.startTag(null, "string-array");
        if (str != null) {
            xmlSerializer.attribute(null, "name", str);
        }
        xmlSerializer.attribute(null, "num", Integer.toString(strArr.length));
        for (String str2 : strArr) {
            xmlSerializer.startTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
            xmlSerializer.attribute(null, "value", str2);
            xmlSerializer.endTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
        }
        xmlSerializer.endTag(null, "string-array");
    }

    public static final void writeBooleanArrayXml(boolean[] zArr, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        if (zArr == null) {
            xmlSerializer.startTag(null, "null");
            xmlSerializer.endTag(null, "null");
            return;
        }
        xmlSerializer.startTag(null, "boolean-array");
        if (str != null) {
            xmlSerializer.attribute(null, "name", str);
        }
        xmlSerializer.attribute(null, "num", Integer.toString(zArr.length));
        for (boolean z : zArr) {
            xmlSerializer.startTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
            xmlSerializer.attribute(null, "value", Boolean.toString(z));
            xmlSerializer.endTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
        }
        xmlSerializer.endTag(null, "boolean-array");
    }

    public static final void writeValueXml(Object obj, String str, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        writeValueXml(obj, str, xmlSerializer, null);
    }

    private static final void writeValueXml(Object obj, String str, XmlSerializer xmlSerializer, WriteMapCallback writeMapCallback) throws XmlPullParserException, IOException {
        String str2;
        if (obj == null) {
            xmlSerializer.startTag(null, "null");
            if (str != null) {
                xmlSerializer.attribute(null, "name", str);
            }
            xmlSerializer.endTag(null, "null");
            return;
        }
        if (obj instanceof String) {
            xmlSerializer.startTag(null, "string");
            if (str != null) {
                xmlSerializer.attribute(null, "name", str);
            }
            xmlSerializer.text(obj.toString());
            xmlSerializer.endTag(null, "string");
            return;
        }
        if (obj instanceof Integer) {
            str2 = SliceItem.FORMAT_INT;
        } else if (obj instanceof Long) {
            str2 = "long";
        } else if (obj instanceof Float) {
            str2 = "float";
        } else if (obj instanceof Double) {
            str2 = "double";
        } else if (obj instanceof Boolean) {
            str2 = "boolean";
        } else {
            if (obj instanceof byte[]) {
                writeByteArrayXml((byte[]) obj, str, xmlSerializer);
                return;
            }
            if (obj instanceof int[]) {
                writeIntArrayXml((int[]) obj, str, xmlSerializer);
                return;
            }
            if (obj instanceof long[]) {
                writeLongArrayXml((long[]) obj, str, xmlSerializer);
                return;
            }
            if (obj instanceof double[]) {
                writeDoubleArrayXml((double[]) obj, str, xmlSerializer);
                return;
            }
            if (obj instanceof String[]) {
                writeStringArrayXml((String[]) obj, str, xmlSerializer);
                return;
            }
            if (obj instanceof boolean[]) {
                writeBooleanArrayXml((boolean[]) obj, str, xmlSerializer);
                return;
            }
            if (obj instanceof Map) {
                writeMapXml((Map) obj, str, xmlSerializer);
                return;
            }
            if (obj instanceof List) {
                writeListXml((List) obj, str, xmlSerializer);
                return;
            }
            if (obj instanceof Set) {
                writeSetXml((Set) obj, str, xmlSerializer);
                return;
            }
            if (obj instanceof CharSequence) {
                xmlSerializer.startTag(null, "string");
                if (str != null) {
                    xmlSerializer.attribute(null, "name", str);
                }
                xmlSerializer.text(obj.toString());
                xmlSerializer.endTag(null, "string");
                return;
            }
            if (writeMapCallback != null) {
                writeMapCallback.writeUnknownObject(obj, str, xmlSerializer);
                return;
            }
            throw new RuntimeException("writeValueXml: unable to write value " + obj);
        }
        xmlSerializer.startTag(null, str2);
        if (str != null) {
            xmlSerializer.attribute(null, "name", str);
        }
        xmlSerializer.attribute(null, "value", obj.toString());
        xmlSerializer.endTag(null, str2);
    }

    public static final HashMap<String, ?> readMapXml(InputStream inputStream) throws XmlPullParserException, IOException {
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(inputStream, StandardCharsets.UTF_8.name());
        return (HashMap) readValueXml(xmlPullParserNewPullParser, new String[1]);
    }

    public static final ArrayList readListXml(InputStream inputStream) throws XmlPullParserException, IOException {
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(inputStream, StandardCharsets.UTF_8.name());
        return (ArrayList) readValueXml(xmlPullParserNewPullParser, new String[1]);
    }

    public static final HashSet readSetXml(InputStream inputStream) throws XmlPullParserException, IOException {
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(inputStream, null);
        return (HashSet) readValueXml(xmlPullParserNewPullParser, new String[1]);
    }

    public static final HashMap<String, ?> readThisMapXml(XmlPullParser xmlPullParser, String str, String[] strArr) throws XmlPullParserException, IOException {
        return readThisMapXml(xmlPullParser, str, strArr, null);
    }

    public static final HashMap<String, ?> readThisMapXml(XmlPullParser xmlPullParser, String str, String[] strArr, ReadMapCallback readMapCallback) throws XmlPullParserException, IOException {
        HashMap<String, ?> map = new HashMap<>();
        int eventType = xmlPullParser.getEventType();
        do {
            if (eventType == 2) {
                map.put(strArr[0], readThisValueXml(xmlPullParser, strArr, readMapCallback, false));
            } else if (eventType == 3) {
                if (xmlPullParser.getName().equals(str)) {
                    return map;
                }
                throw new XmlPullParserException("Expected " + str + " end tag at: " + xmlPullParser.getName());
            }
            eventType = xmlPullParser.next();
        } while (eventType != 1);
        throw new XmlPullParserException("Document ended before " + str + " end tag");
    }

    public static final ArrayMap<String, ?> readThisArrayMapXml(XmlPullParser xmlPullParser, String str, String[] strArr, ReadMapCallback readMapCallback) throws XmlPullParserException, IOException {
        ArrayMap<String, ?> arrayMap = new ArrayMap<>();
        int eventType = xmlPullParser.getEventType();
        do {
            if (eventType == 2) {
                arrayMap.put(strArr[0], readThisValueXml(xmlPullParser, strArr, readMapCallback, true));
            } else if (eventType == 3) {
                if (xmlPullParser.getName().equals(str)) {
                    return arrayMap;
                }
                throw new XmlPullParserException("Expected " + str + " end tag at: " + xmlPullParser.getName());
            }
            eventType = xmlPullParser.next();
        } while (eventType != 1);
        throw new XmlPullParserException("Document ended before " + str + " end tag");
    }

    public static final ArrayList readThisListXml(XmlPullParser xmlPullParser, String str, String[] strArr) throws XmlPullParserException, IOException {
        return readThisListXml(xmlPullParser, str, strArr, null, false);
    }

    private static final ArrayList readThisListXml(XmlPullParser xmlPullParser, String str, String[] strArr, ReadMapCallback readMapCallback, boolean z) throws XmlPullParserException, IOException {
        ArrayList arrayList = new ArrayList();
        int eventType = xmlPullParser.getEventType();
        do {
            if (eventType == 2) {
                arrayList.add(readThisValueXml(xmlPullParser, strArr, readMapCallback, z));
            } else if (eventType == 3) {
                if (xmlPullParser.getName().equals(str)) {
                    return arrayList;
                }
                throw new XmlPullParserException("Expected " + str + " end tag at: " + xmlPullParser.getName());
            }
            eventType = xmlPullParser.next();
        } while (eventType != 1);
        throw new XmlPullParserException("Document ended before " + str + " end tag");
    }

    public static final HashSet readThisSetXml(XmlPullParser xmlPullParser, String str, String[] strArr) throws XmlPullParserException, IOException {
        return readThisSetXml(xmlPullParser, str, strArr, null, false);
    }

    private static final HashSet readThisSetXml(XmlPullParser xmlPullParser, String str, String[] strArr, ReadMapCallback readMapCallback, boolean z) throws XmlPullParserException, IOException {
        HashSet hashSet = new HashSet();
        int eventType = xmlPullParser.getEventType();
        do {
            if (eventType == 2) {
                hashSet.add(readThisValueXml(xmlPullParser, strArr, readMapCallback, z));
            } else if (eventType == 3) {
                if (xmlPullParser.getName().equals(str)) {
                    return hashSet;
                }
                throw new XmlPullParserException("Expected " + str + " end tag at: " + xmlPullParser.getName());
            }
            eventType = xmlPullParser.next();
        } while (eventType != 1);
        throw new XmlPullParserException("Document ended before " + str + " end tag");
    }

    public static final byte[] readThisByteArrayXml(XmlPullParser xmlPullParser, String str, String[] strArr) throws XmlPullParserException, IOException {
        try {
            int i = Integer.parseInt(xmlPullParser.getAttributeValue(null, "num"));
            byte[] bArr = new byte[i];
            int eventType = xmlPullParser.getEventType();
            do {
                if (eventType == 4) {
                    if (i > 0) {
                        String text = xmlPullParser.getText();
                        if (text == null || text.length() != i * 2) {
                            throw new XmlPullParserException("Invalid value found in byte-array: " + text);
                        }
                        for (int i2 = 0; i2 < i; i2++) {
                            int i3 = 2 * i2;
                            char cCharAt = text.charAt(i3);
                            char cCharAt2 = text.charAt(i3 + 1);
                            bArr[i2] = (byte) (((cCharAt2 > 'a' ? (cCharAt2 - 'a') + 10 : cCharAt2 - '0') & 15) | (((cCharAt > 'a' ? (cCharAt - 'a') + 10 : cCharAt - '0') & 15) << 4));
                        }
                    }
                } else if (eventType == 3) {
                    if (xmlPullParser.getName().equals(str)) {
                        return bArr;
                    }
                    throw new XmlPullParserException("Expected " + str + " end tag at: " + xmlPullParser.getName());
                }
                eventType = xmlPullParser.next();
            } while (eventType != 1);
            throw new XmlPullParserException("Document ended before " + str + " end tag");
        } catch (NullPointerException e) {
            throw new XmlPullParserException("Need num attribute in byte-array");
        } catch (NumberFormatException e2) {
            throw new XmlPullParserException("Not a number in num attribute in byte-array");
        }
    }

    public static final int[] readThisIntArrayXml(XmlPullParser xmlPullParser, String str, String[] strArr) throws XmlPullParserException, IOException {
        try {
            int i = Integer.parseInt(xmlPullParser.getAttributeValue(null, "num"));
            xmlPullParser.next();
            int[] iArr = new int[i];
            int i2 = 0;
            int eventType = xmlPullParser.getEventType();
            do {
                if (eventType == 2) {
                    if (xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                        try {
                            iArr[i2] = Integer.parseInt(xmlPullParser.getAttributeValue(null, "value"));
                        } catch (NullPointerException e) {
                            throw new XmlPullParserException("Need value attribute in item");
                        } catch (NumberFormatException e2) {
                            throw new XmlPullParserException("Not a number in value attribute in item");
                        }
                    } else {
                        throw new XmlPullParserException("Expected item tag at: " + xmlPullParser.getName());
                    }
                } else if (eventType == 3) {
                    if (xmlPullParser.getName().equals(str)) {
                        return iArr;
                    }
                    if (xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                        i2++;
                    } else {
                        throw new XmlPullParserException("Expected " + str + " end tag at: " + xmlPullParser.getName());
                    }
                }
                eventType = xmlPullParser.next();
            } while (eventType != 1);
            throw new XmlPullParserException("Document ended before " + str + " end tag");
        } catch (NullPointerException e3) {
            throw new XmlPullParserException("Need num attribute in int-array");
        } catch (NumberFormatException e4) {
            throw new XmlPullParserException("Not a number in num attribute in int-array");
        }
    }

    public static final long[] readThisLongArrayXml(XmlPullParser xmlPullParser, String str, String[] strArr) throws XmlPullParserException, IOException {
        try {
            int i = Integer.parseInt(xmlPullParser.getAttributeValue(null, "num"));
            xmlPullParser.next();
            long[] jArr = new long[i];
            int i2 = 0;
            int eventType = xmlPullParser.getEventType();
            do {
                if (eventType == 2) {
                    if (xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                        try {
                            jArr[i2] = Long.parseLong(xmlPullParser.getAttributeValue(null, "value"));
                        } catch (NullPointerException e) {
                            throw new XmlPullParserException("Need value attribute in item");
                        } catch (NumberFormatException e2) {
                            throw new XmlPullParserException("Not a number in value attribute in item");
                        }
                    } else {
                        throw new XmlPullParserException("Expected item tag at: " + xmlPullParser.getName());
                    }
                } else if (eventType == 3) {
                    if (xmlPullParser.getName().equals(str)) {
                        return jArr;
                    }
                    if (xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                        i2++;
                    } else {
                        throw new XmlPullParserException("Expected " + str + " end tag at: " + xmlPullParser.getName());
                    }
                }
                eventType = xmlPullParser.next();
            } while (eventType != 1);
            throw new XmlPullParserException("Document ended before " + str + " end tag");
        } catch (NullPointerException e3) {
            throw new XmlPullParserException("Need num attribute in long-array");
        } catch (NumberFormatException e4) {
            throw new XmlPullParserException("Not a number in num attribute in long-array");
        }
    }

    public static final double[] readThisDoubleArrayXml(XmlPullParser xmlPullParser, String str, String[] strArr) throws XmlPullParserException, IOException {
        try {
            int i = Integer.parseInt(xmlPullParser.getAttributeValue(null, "num"));
            xmlPullParser.next();
            double[] dArr = new double[i];
            int i2 = 0;
            int eventType = xmlPullParser.getEventType();
            do {
                if (eventType == 2) {
                    if (xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                        try {
                            dArr[i2] = Double.parseDouble(xmlPullParser.getAttributeValue(null, "value"));
                        } catch (NullPointerException e) {
                            throw new XmlPullParserException("Need value attribute in item");
                        } catch (NumberFormatException e2) {
                            throw new XmlPullParserException("Not a number in value attribute in item");
                        }
                    } else {
                        throw new XmlPullParserException("Expected item tag at: " + xmlPullParser.getName());
                    }
                } else if (eventType == 3) {
                    if (xmlPullParser.getName().equals(str)) {
                        return dArr;
                    }
                    if (xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                        i2++;
                    } else {
                        throw new XmlPullParserException("Expected " + str + " end tag at: " + xmlPullParser.getName());
                    }
                }
                eventType = xmlPullParser.next();
            } while (eventType != 1);
            throw new XmlPullParserException("Document ended before " + str + " end tag");
        } catch (NullPointerException e3) {
            throw new XmlPullParserException("Need num attribute in double-array");
        } catch (NumberFormatException e4) {
            throw new XmlPullParserException("Not a number in num attribute in double-array");
        }
    }

    public static final String[] readThisStringArrayXml(XmlPullParser xmlPullParser, String str, String[] strArr) throws XmlPullParserException, IOException {
        try {
            int i = Integer.parseInt(xmlPullParser.getAttributeValue(null, "num"));
            xmlPullParser.next();
            String[] strArr2 = new String[i];
            int i2 = 0;
            int eventType = xmlPullParser.getEventType();
            do {
                if (eventType == 2) {
                    if (xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                        try {
                            strArr2[i2] = xmlPullParser.getAttributeValue(null, "value");
                        } catch (NullPointerException e) {
                            throw new XmlPullParserException("Need value attribute in item");
                        } catch (NumberFormatException e2) {
                            throw new XmlPullParserException("Not a number in value attribute in item");
                        }
                    } else {
                        throw new XmlPullParserException("Expected item tag at: " + xmlPullParser.getName());
                    }
                } else if (eventType == 3) {
                    if (xmlPullParser.getName().equals(str)) {
                        return strArr2;
                    }
                    if (xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                        i2++;
                    } else {
                        throw new XmlPullParserException("Expected " + str + " end tag at: " + xmlPullParser.getName());
                    }
                }
                eventType = xmlPullParser.next();
            } while (eventType != 1);
            throw new XmlPullParserException("Document ended before " + str + " end tag");
        } catch (NullPointerException e3) {
            throw new XmlPullParserException("Need num attribute in string-array");
        } catch (NumberFormatException e4) {
            throw new XmlPullParserException("Not a number in num attribute in string-array");
        }
    }

    public static final boolean[] readThisBooleanArrayXml(XmlPullParser xmlPullParser, String str, String[] strArr) throws XmlPullParserException, IOException {
        try {
            int i = Integer.parseInt(xmlPullParser.getAttributeValue(null, "num"));
            xmlPullParser.next();
            boolean[] zArr = new boolean[i];
            int i2 = 0;
            int eventType = xmlPullParser.getEventType();
            do {
                if (eventType == 2) {
                    if (xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                        try {
                            zArr[i2] = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, "value"));
                        } catch (NullPointerException e) {
                            throw new XmlPullParserException("Need value attribute in item");
                        } catch (NumberFormatException e2) {
                            throw new XmlPullParserException("Not a number in value attribute in item");
                        }
                    } else {
                        throw new XmlPullParserException("Expected item tag at: " + xmlPullParser.getName());
                    }
                } else if (eventType == 3) {
                    if (xmlPullParser.getName().equals(str)) {
                        return zArr;
                    }
                    if (xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                        i2++;
                    } else {
                        throw new XmlPullParserException("Expected " + str + " end tag at: " + xmlPullParser.getName());
                    }
                }
                eventType = xmlPullParser.next();
            } while (eventType != 1);
            throw new XmlPullParserException("Document ended before " + str + " end tag");
        } catch (NullPointerException e3) {
            throw new XmlPullParserException("Need num attribute in string-array");
        } catch (NumberFormatException e4) {
            throw new XmlPullParserException("Not a number in num attribute in string-array");
        }
    }

    public static final Object readValueXml(XmlPullParser xmlPullParser, String[] strArr) throws XmlPullParserException, IOException {
        int eventType = xmlPullParser.getEventType();
        while (eventType != 2) {
            if (eventType == 3) {
                throw new XmlPullParserException("Unexpected end tag at: " + xmlPullParser.getName());
            }
            if (eventType == 4) {
                throw new XmlPullParserException("Unexpected text: " + xmlPullParser.getText());
            }
            eventType = xmlPullParser.next();
            if (eventType == 1) {
                throw new XmlPullParserException("Unexpected end of document");
            }
        }
        return readThisValueXml(xmlPullParser, strArr, null, false);
    }

    private static final Object readThisValueXml(XmlPullParser xmlPullParser, String[] strArr, ReadMapCallback readMapCallback, boolean z) throws XmlPullParserException, IOException {
        int next;
        Object thisMapXml;
        Object thisPrimitiveValueXml = null;
        String attributeValue = xmlPullParser.getAttributeValue(null, "name");
        String name = xmlPullParser.getName();
        if (!name.equals("null")) {
            if (name.equals("string")) {
                String str = "";
                while (true) {
                    int next2 = xmlPullParser.next();
                    if (next2 != 1) {
                        if (next2 == 3) {
                            if (xmlPullParser.getName().equals("string")) {
                                strArr[0] = attributeValue;
                                return str;
                            }
                            throw new XmlPullParserException("Unexpected end tag in <string>: " + xmlPullParser.getName());
                        }
                        if (next2 == 4) {
                            str = str + xmlPullParser.getText();
                        } else if (next2 == 2) {
                            throw new XmlPullParserException("Unexpected start tag in <string>: " + xmlPullParser.getName());
                        }
                    } else {
                        throw new XmlPullParserException("Unexpected end of document in <string>");
                    }
                }
            } else {
                thisPrimitiveValueXml = readThisPrimitiveValueXml(xmlPullParser, name);
                if (thisPrimitiveValueXml == null) {
                    if (name.equals("byte-array")) {
                        byte[] thisByteArrayXml = readThisByteArrayXml(xmlPullParser, "byte-array", strArr);
                        strArr[0] = attributeValue;
                        return thisByteArrayXml;
                    }
                    if (name.equals("int-array")) {
                        int[] thisIntArrayXml = readThisIntArrayXml(xmlPullParser, "int-array", strArr);
                        strArr[0] = attributeValue;
                        return thisIntArrayXml;
                    }
                    if (name.equals("long-array")) {
                        long[] thisLongArrayXml = readThisLongArrayXml(xmlPullParser, "long-array", strArr);
                        strArr[0] = attributeValue;
                        return thisLongArrayXml;
                    }
                    if (name.equals("double-array")) {
                        double[] thisDoubleArrayXml = readThisDoubleArrayXml(xmlPullParser, "double-array", strArr);
                        strArr[0] = attributeValue;
                        return thisDoubleArrayXml;
                    }
                    if (name.equals("string-array")) {
                        String[] thisStringArrayXml = readThisStringArrayXml(xmlPullParser, "string-array", strArr);
                        strArr[0] = attributeValue;
                        return thisStringArrayXml;
                    }
                    if (name.equals("boolean-array")) {
                        boolean[] thisBooleanArrayXml = readThisBooleanArrayXml(xmlPullParser, "boolean-array", strArr);
                        strArr[0] = attributeValue;
                        return thisBooleanArrayXml;
                    }
                    if (name.equals("map")) {
                        xmlPullParser.next();
                        if (z) {
                            thisMapXml = readThisArrayMapXml(xmlPullParser, "map", strArr, readMapCallback);
                        } else {
                            thisMapXml = readThisMapXml(xmlPullParser, "map", strArr, readMapCallback);
                        }
                        strArr[0] = attributeValue;
                        return thisMapXml;
                    }
                    if (name.equals(Slice.HINT_LIST)) {
                        xmlPullParser.next();
                        ArrayList thisListXml = readThisListXml(xmlPullParser, Slice.HINT_LIST, strArr, readMapCallback, z);
                        strArr[0] = attributeValue;
                        return thisListXml;
                    }
                    if (name.equals("set")) {
                        xmlPullParser.next();
                        HashSet thisSetXml = readThisSetXml(xmlPullParser, "set", strArr, readMapCallback, z);
                        strArr[0] = attributeValue;
                        return thisSetXml;
                    }
                    if (readMapCallback != null) {
                        Object thisUnknownObjectXml = readMapCallback.readThisUnknownObjectXml(xmlPullParser, name);
                        strArr[0] = attributeValue;
                        return thisUnknownObjectXml;
                    }
                    throw new XmlPullParserException("Unknown tag: " + name);
                }
            }
        }
        do {
            next = xmlPullParser.next();
            if (next != 1) {
                if (next == 3) {
                    if (xmlPullParser.getName().equals(name)) {
                        strArr[0] = attributeValue;
                        return thisPrimitiveValueXml;
                    }
                    throw new XmlPullParserException("Unexpected end tag in <" + name + ">: " + xmlPullParser.getName());
                }
                if (next == 4) {
                    throw new XmlPullParserException("Unexpected text in <" + name + ">: " + xmlPullParser.getName());
                }
            } else {
                throw new XmlPullParserException("Unexpected end of document in <" + name + ">");
            }
        } while (next != 2);
        throw new XmlPullParserException("Unexpected start tag in <" + name + ">: " + xmlPullParser.getName());
    }

    private static final Object readThisPrimitiveValueXml(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        try {
            if (str.equals(SliceItem.FORMAT_INT)) {
                return Integer.valueOf(Integer.parseInt(xmlPullParser.getAttributeValue(null, "value")));
            }
            if (str.equals("long")) {
                return Long.valueOf(xmlPullParser.getAttributeValue(null, "value"));
            }
            if (str.equals("float")) {
                return new Float(xmlPullParser.getAttributeValue(null, "value"));
            }
            if (str.equals("double")) {
                return new Double(xmlPullParser.getAttributeValue(null, "value"));
            }
            if (str.equals("boolean")) {
                return Boolean.valueOf(xmlPullParser.getAttributeValue(null, "value"));
            }
            return null;
        } catch (NullPointerException e) {
            throw new XmlPullParserException("Need value attribute in <" + str + ">");
        } catch (NumberFormatException e2) {
            throw new XmlPullParserException("Not a number in value attribute in <" + str + ">");
        }
    }

    public static final void beginDocument(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        int next;
        do {
            next = xmlPullParser.next();
            if (next == 2) {
                break;
            }
        } while (next != 1);
        if (next != 2) {
            throw new XmlPullParserException("No start tag found");
        }
        if (!xmlPullParser.getName().equals(str)) {
            throw new XmlPullParserException("Unexpected start tag: found " + xmlPullParser.getName() + ", expected " + str);
        }
    }

    public static final void nextElement(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int next;
        do {
            next = xmlPullParser.next();
            if (next == 2) {
                return;
            }
        } while (next != 1);
    }

    public static boolean nextElementWithin(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException {
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next == 3 && xmlPullParser.getDepth() == i) {
                    return false;
                }
                if (next == 2 && xmlPullParser.getDepth() == i + 1) {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    public static int readIntAttribute(XmlPullParser xmlPullParser, String str, int i) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (TextUtils.isEmpty(attributeValue)) {
            return i;
        }
        try {
            return Integer.parseInt(attributeValue);
        } catch (NumberFormatException e) {
            return i;
        }
    }

    public static int readIntAttribute(XmlPullParser xmlPullParser, String str) throws IOException {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        try {
            return Integer.parseInt(attributeValue);
        } catch (NumberFormatException e) {
            throw new ProtocolException("problem parsing " + str + "=" + attributeValue + " as int");
        }
    }

    public static void writeIntAttribute(XmlSerializer xmlSerializer, String str, int i) throws IOException {
        xmlSerializer.attribute(null, str, Integer.toString(i));
    }

    public static long readLongAttribute(XmlPullParser xmlPullParser, String str, long j) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (TextUtils.isEmpty(attributeValue)) {
            return j;
        }
        try {
            return Long.parseLong(attributeValue);
        } catch (NumberFormatException e) {
            return j;
        }
    }

    public static long readLongAttribute(XmlPullParser xmlPullParser, String str) throws IOException {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        try {
            return Long.parseLong(attributeValue);
        } catch (NumberFormatException e) {
            throw new ProtocolException("problem parsing " + str + "=" + attributeValue + " as long");
        }
    }

    public static void writeLongAttribute(XmlSerializer xmlSerializer, String str, long j) throws IOException {
        xmlSerializer.attribute(null, str, Long.toString(j));
    }

    public static float readFloatAttribute(XmlPullParser xmlPullParser, String str) throws IOException {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        try {
            return Float.parseFloat(attributeValue);
        } catch (NumberFormatException e) {
            throw new ProtocolException("problem parsing " + str + "=" + attributeValue + " as long");
        }
    }

    public static void writeFloatAttribute(XmlSerializer xmlSerializer, String str, float f) throws IOException {
        xmlSerializer.attribute(null, str, Float.toString(f));
    }

    public static boolean readBooleanAttribute(XmlPullParser xmlPullParser, String str) {
        return Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, str));
    }

    public static boolean readBooleanAttribute(XmlPullParser xmlPullParser, String str, boolean z) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (attributeValue == null) {
            return z;
        }
        return Boolean.parseBoolean(attributeValue);
    }

    public static void writeBooleanAttribute(XmlSerializer xmlSerializer, String str, boolean z) throws IOException {
        xmlSerializer.attribute(null, str, Boolean.toString(z));
    }

    public static Uri readUriAttribute(XmlPullParser xmlPullParser, String str) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (attributeValue != null) {
            return Uri.parse(attributeValue);
        }
        return null;
    }

    public static void writeUriAttribute(XmlSerializer xmlSerializer, String str, Uri uri) throws IOException {
        if (uri != null) {
            xmlSerializer.attribute(null, str, uri.toString());
        }
    }

    public static String readStringAttribute(XmlPullParser xmlPullParser, String str) {
        return xmlPullParser.getAttributeValue(null, str);
    }

    public static void writeStringAttribute(XmlSerializer xmlSerializer, String str, CharSequence charSequence) throws IOException {
        if (charSequence != null) {
            xmlSerializer.attribute(null, str, charSequence.toString());
        }
    }

    public static byte[] readByteArrayAttribute(XmlPullParser xmlPullParser, String str) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (attributeValue == null) {
            return null;
        }
        return Base64.decode(attributeValue, 0);
    }

    public static void writeByteArrayAttribute(XmlSerializer xmlSerializer, String str, byte[] bArr) throws IOException {
        if (bArr != null) {
            xmlSerializer.attribute(null, str, Base64.encodeToString(bArr, 0));
        }
    }

    public static Bitmap readBitmapAttribute(XmlPullParser xmlPullParser, String str) {
        byte[] byteArrayAttribute = readByteArrayAttribute(xmlPullParser, str);
        if (byteArrayAttribute != null) {
            return BitmapFactory.decodeByteArray(byteArrayAttribute, 0, byteArrayAttribute.length);
        }
        return null;
    }

    @Deprecated
    public static void writeBitmapAttribute(XmlSerializer xmlSerializer, String str, Bitmap bitmap) throws IOException {
        if (bitmap != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, byteArrayOutputStream);
            writeByteArrayAttribute(xmlSerializer, str, byteArrayOutputStream.toByteArray());
        }
    }
}
