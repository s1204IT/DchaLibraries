package com.android.server.wm;

import android.graphics.Rect;
import android.os.Environment;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.Settings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class DisplaySettings {
    private static final String TAG = "WindowManager";
    private final HashMap<String, Entry> mEntries = new HashMap<>();
    private final AtomicFile mFile = new AtomicFile(new File(new File(Environment.getDataDirectory(), "system"), "display_settings.xml"), "wm-displays");

    public static class Entry {
        public final String name;
        public int overscanBottom;
        public int overscanLeft;
        public int overscanRight;
        public int overscanTop;

        public Entry(String str) {
            this.name = str;
        }
    }

    public void getOverscanLocked(String str, String str2, Rect rect) {
        Entry entry;
        if (str2 == null || (entry = this.mEntries.get(str2)) == null) {
            entry = this.mEntries.get(str);
        }
        if (entry != null) {
            rect.left = entry.overscanLeft;
            rect.top = entry.overscanTop;
            rect.right = entry.overscanRight;
            rect.bottom = entry.overscanBottom;
            return;
        }
        rect.set(0, 0, 0, 0);
    }

    public void setOverscanLocked(String str, String str2, int i, int i2, int i3, int i4) {
        if (i == 0 && i2 == 0 && i3 == 0 && i4 == 0) {
            this.mEntries.remove(str);
            this.mEntries.remove(str2);
            return;
        }
        Entry entry = this.mEntries.get(str);
        if (entry == null) {
            entry = new Entry(str);
            this.mEntries.put(str, entry);
        }
        entry.overscanLeft = i;
        entry.overscanTop = i2;
        entry.overscanRight = i3;
        entry.overscanBottom = i4;
    }

    public void readSettingsLocked() {
        FileInputStream fileInputStreamClose;
        int next;
        try {
            try {
                FileInputStream fileInputStreamOpenRead = this.mFile.openRead();
                try {
                    try {
                        try {
                            try {
                                try {
                                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                                    xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                                    do {
                                        next = xmlPullParserNewPullParser.next();
                                        if (next == 2) {
                                            break;
                                        }
                                    } while (next != 1);
                                    if (next != 2) {
                                        throw new IllegalStateException("no start tag found");
                                    }
                                    int depth = xmlPullParserNewPullParser.getDepth();
                                    while (true) {
                                        int next2 = xmlPullParserNewPullParser.next();
                                        if (next2 == 1 || (next2 == 3 && xmlPullParserNewPullParser.getDepth() <= depth)) {
                                            break;
                                        }
                                        if (next2 != 3 && next2 != 4) {
                                            if (xmlPullParserNewPullParser.getName().equals("display")) {
                                                readDisplay(xmlPullParserNewPullParser);
                                            } else {
                                                Slog.w("WindowManager", "Unknown element under <display-settings>: " + xmlPullParserNewPullParser.getName());
                                                XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                            }
                                        }
                                    }
                                    fileInputStreamOpenRead.close();
                                } catch (IOException e) {
                                }
                            } catch (NumberFormatException e2) {
                                Slog.w("WindowManager", "Failed parsing " + e2);
                                this.mEntries.clear();
                                fileInputStreamOpenRead.close();
                            }
                        } catch (IOException e3) {
                            Slog.w("WindowManager", "Failed parsing " + e3);
                            this.mEntries.clear();
                            fileInputStreamOpenRead.close();
                        }
                    } catch (IllegalStateException e4) {
                        Slog.w("WindowManager", "Failed parsing " + e4);
                        this.mEntries.clear();
                        fileInputStreamOpenRead.close();
                    } catch (IndexOutOfBoundsException e5) {
                        Slog.w("WindowManager", "Failed parsing " + e5);
                        this.mEntries.clear();
                        fileInputStreamOpenRead.close();
                    }
                } catch (NullPointerException e6) {
                    Slog.w("WindowManager", "Failed parsing " + e6);
                    this.mEntries.clear();
                    fileInputStreamOpenRead.close();
                } catch (XmlPullParserException e7) {
                    Slog.w("WindowManager", "Failed parsing " + e7);
                    this.mEntries.clear();
                    fileInputStreamOpenRead.close();
                }
            } catch (FileNotFoundException e8) {
                Slog.i("WindowManager", "No existing display settings " + this.mFile.getBaseFile() + "; starting empty");
            }
        } catch (Throwable th) {
            this.mEntries.clear();
            try {
                fileInputStreamClose.close();
            } catch (IOException e9) {
            }
            throw th;
        }
    }

    private int getIntAttribute(XmlPullParser xmlPullParser, String str) {
        try {
            String attributeValue = xmlPullParser.getAttributeValue(null, str);
            if (attributeValue != null) {
                return Integer.parseInt(attributeValue);
            }
            return 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void readDisplay(XmlPullParser xmlPullParser) throws XmlPullParserException, NumberFormatException, IOException {
        String attributeValue = xmlPullParser.getAttributeValue(null, Settings.ATTR_NAME);
        if (attributeValue != null) {
            Entry entry = new Entry(attributeValue);
            entry.overscanLeft = getIntAttribute(xmlPullParser, "overscanLeft");
            entry.overscanTop = getIntAttribute(xmlPullParser, "overscanTop");
            entry.overscanRight = getIntAttribute(xmlPullParser, "overscanRight");
            entry.overscanBottom = getIntAttribute(xmlPullParser, "overscanBottom");
            this.mEntries.put(attributeValue, entry);
        }
        XmlUtils.skipCurrentTag(xmlPullParser);
    }

    public void writeSettingsLocked() {
        try {
            FileOutputStream fileOutputStreamStartWrite = this.mFile.startWrite();
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, "display-settings");
                for (Entry entry : this.mEntries.values()) {
                    fastXmlSerializer.startTag(null, "display");
                    fastXmlSerializer.attribute(null, Settings.ATTR_NAME, entry.name);
                    if (entry.overscanLeft != 0) {
                        fastXmlSerializer.attribute(null, "overscanLeft", Integer.toString(entry.overscanLeft));
                    }
                    if (entry.overscanTop != 0) {
                        fastXmlSerializer.attribute(null, "overscanTop", Integer.toString(entry.overscanTop));
                    }
                    if (entry.overscanRight != 0) {
                        fastXmlSerializer.attribute(null, "overscanRight", Integer.toString(entry.overscanRight));
                    }
                    if (entry.overscanBottom != 0) {
                        fastXmlSerializer.attribute(null, "overscanBottom", Integer.toString(entry.overscanBottom));
                    }
                    fastXmlSerializer.endTag(null, "display");
                }
                fastXmlSerializer.endTag(null, "display-settings");
                fastXmlSerializer.endDocument();
                this.mFile.finishWrite(fileOutputStreamStartWrite);
            } catch (IOException e) {
                Slog.w("WindowManager", "Failed to write display settings, restoring backup.", e);
                this.mFile.failWrite(fileOutputStreamStartWrite);
            }
        } catch (IOException e2) {
            Slog.w("WindowManager", "Failed to write display settings: " + e2);
        }
    }
}
