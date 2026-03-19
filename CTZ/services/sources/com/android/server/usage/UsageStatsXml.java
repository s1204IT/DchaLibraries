package com.android.server.usage;

import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class UsageStatsXml {
    static final String CHECKED_IN_SUFFIX = "-c";
    private static final int CURRENT_VERSION = 1;
    private static final String TAG = "UsageStatsXml";
    private static final String USAGESTATS_TAG = "usagestats";
    private static final String VERSION_ATTR = "version";

    public static long parseBeginTime(AtomicFile atomicFile) throws IOException {
        return parseBeginTime(atomicFile.getBaseFile());
    }

    public static long parseBeginTime(File file) throws IOException {
        String name = file.getName();
        while (name.endsWith(CHECKED_IN_SUFFIX)) {
            name = name.substring(0, name.length() - CHECKED_IN_SUFFIX.length());
        }
        try {
            return Long.parseLong(name);
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
    }

    public static void read(AtomicFile atomicFile, IntervalStats intervalStats) throws IOException {
        try {
            FileInputStream fileInputStreamOpenRead = atomicFile.openRead();
            try {
                intervalStats.beginTime = parseBeginTime(atomicFile);
                read(fileInputStreamOpenRead, intervalStats);
                intervalStats.lastTimeSaved = atomicFile.getLastModifiedTime();
                try {
                    fileInputStreamOpenRead.close();
                } catch (IOException e) {
                }
            } catch (Throwable th) {
                try {
                    fileInputStreamOpenRead.close();
                } catch (IOException e2) {
                }
                throw th;
            }
        } catch (FileNotFoundException e3) {
            Slog.e(TAG, "UsageStats Xml", e3);
            throw e3;
        }
    }

    public static void write(AtomicFile atomicFile, IntervalStats intervalStats) throws IOException {
        FileOutputStream fileOutputStreamStartWrite = atomicFile.startWrite();
        try {
            write(fileOutputStreamStartWrite, intervalStats);
            atomicFile.finishWrite(fileOutputStreamStartWrite);
            atomicFile.failWrite(null);
        } catch (Throwable th) {
            atomicFile.failWrite(fileOutputStreamStartWrite);
            throw th;
        }
    }

    static void read(InputStream inputStream, IntervalStats intervalStats) throws IOException {
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        try {
            xmlPullParserNewPullParser.setInput(inputStream, "utf-8");
            XmlUtils.beginDocument(xmlPullParserNewPullParser, USAGESTATS_TAG);
            String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, VERSION_ATTR);
            try {
                if (Integer.parseInt(attributeValue) == 1) {
                    UsageStatsXmlV1.read(xmlPullParserNewPullParser, intervalStats);
                    return;
                }
                Slog.e(TAG, "Unrecognized version " + attributeValue);
                throw new IOException("Unrecognized version " + attributeValue);
            } catch (NumberFormatException e) {
                Slog.e(TAG, "Bad version");
                throw new IOException(e);
            }
        } catch (XmlPullParserException e2) {
            Slog.e(TAG, "Failed to parse Xml", e2);
            throw new IOException(e2);
        }
    }

    static void write(OutputStream outputStream, IntervalStats intervalStats) throws IOException {
        FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
        fastXmlSerializer.setOutput(outputStream, "utf-8");
        fastXmlSerializer.startDocument("utf-8", true);
        fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        fastXmlSerializer.startTag((String) null, USAGESTATS_TAG);
        fastXmlSerializer.attribute((String) null, VERSION_ATTR, Integer.toString(1));
        UsageStatsXmlV1.write(fastXmlSerializer, intervalStats);
        fastXmlSerializer.endTag((String) null, USAGESTATS_TAG);
        fastXmlSerializer.endDocument();
    }
}
