package com.android.server.wm;

import android.graphics.Rect;
import android.os.Environment;
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class DisplaySettings {
    private static final String TAG = "WindowManager";
    private final HashMap<String, Entry> mEntries = new HashMap<>();
    private final AtomicFile mFile;

    public static class Entry {
        public final String name;
        public int overscanBottom;
        public int overscanLeft;
        public int overscanRight;
        public int overscanTop;

        public Entry(String _name) {
            this.name = _name;
        }
    }

    public DisplaySettings() {
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, "system");
        this.mFile = new AtomicFile(new File(systemDir, "display_settings.xml"));
    }

    public void getOverscanLocked(String name, String uniqueId, Rect outRect) {
        Entry entry;
        if (uniqueId == null || (entry = this.mEntries.get(uniqueId)) == null) {
            entry = this.mEntries.get(name);
        }
        if (entry != null) {
            outRect.left = entry.overscanLeft;
            outRect.top = entry.overscanTop;
            outRect.right = entry.overscanRight;
            outRect.bottom = entry.overscanBottom;
            return;
        }
        outRect.set(0, 0, 0, 0);
    }

    public void setOverscanLocked(String uniqueId, String name, int left, int top, int right, int bottom) {
        if (left == 0 && top == 0 && right == 0 && bottom == 0) {
            this.mEntries.remove(uniqueId);
            this.mEntries.remove(name);
            return;
        }
        Entry entry = this.mEntries.get(uniqueId);
        if (entry == null) {
            entry = new Entry(uniqueId);
            this.mEntries.put(uniqueId, entry);
        }
        entry.overscanLeft = left;
        entry.overscanTop = top;
        entry.overscanRight = right;
        entry.overscanBottom = bottom;
    }

    public void readSettingsLocked() {
        int type;
        try {
            FileInputStream stream = this.mFile.openRead();
            try {
                try {
                    try {
                        try {
                            try {
                                XmlPullParser parser = Xml.newPullParser();
                                parser.setInput(stream, StandardCharsets.UTF_8.name());
                                do {
                                    type = parser.next();
                                    if (type == 2) {
                                        break;
                                    }
                                } while (type != 1);
                                if (type != 2) {
                                    throw new IllegalStateException("no start tag found");
                                }
                                int outerDepth = parser.getDepth();
                                while (true) {
                                    int type2 = parser.next();
                                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                                        break;
                                    }
                                    if (type2 != 3 && type2 != 4) {
                                        String tagName = parser.getName();
                                        if (tagName.equals("display")) {
                                            readDisplay(parser);
                                        } else {
                                            Slog.w(TAG, "Unknown element under <display-settings>: " + parser.getName());
                                            XmlUtils.skipCurrentTag(parser);
                                        }
                                    }
                                }
                                if (1 == 0) {
                                    this.mEntries.clear();
                                }
                                try {
                                    stream.close();
                                } catch (IOException e) {
                                }
                            } catch (Throwable th) {
                                if (0 == 0) {
                                    this.mEntries.clear();
                                }
                                try {
                                    stream.close();
                                } catch (IOException e2) {
                                }
                                throw th;
                            }
                        } catch (IOException e3) {
                            Slog.w(TAG, "Failed parsing " + e3);
                            if (0 == 0) {
                                this.mEntries.clear();
                            }
                            try {
                                stream.close();
                            } catch (IOException e4) {
                            }
                        }
                    } catch (IllegalStateException e5) {
                        Slog.w(TAG, "Failed parsing " + e5);
                        if (0 == 0) {
                            this.mEntries.clear();
                        }
                        try {
                            stream.close();
                        } catch (IOException e6) {
                        }
                    }
                } catch (IndexOutOfBoundsException e7) {
                    Slog.w(TAG, "Failed parsing " + e7);
                    if (0 == 0) {
                        this.mEntries.clear();
                    }
                    try {
                        stream.close();
                    } catch (IOException e8) {
                    }
                } catch (XmlPullParserException e9) {
                    Slog.w(TAG, "Failed parsing " + e9);
                    if (0 == 0) {
                        this.mEntries.clear();
                    }
                    try {
                        stream.close();
                    } catch (IOException e10) {
                    }
                }
            } catch (NullPointerException e11) {
                Slog.w(TAG, "Failed parsing " + e11);
                if (0 == 0) {
                    this.mEntries.clear();
                }
                try {
                    stream.close();
                } catch (IOException e12) {
                }
            } catch (NumberFormatException e13) {
                Slog.w(TAG, "Failed parsing " + e13);
                if (0 == 0) {
                    this.mEntries.clear();
                }
                try {
                    stream.close();
                } catch (IOException e14) {
                }
            }
        } catch (FileNotFoundException e15) {
            Slog.i(TAG, "No existing display settings " + this.mFile.getBaseFile() + "; starting empty");
        }
    }

    private int getIntAttribute(XmlPullParser parser, String name) {
        try {
            String str = parser.getAttributeValue(null, name);
            if (str != null) {
                return Integer.parseInt(str);
            }
            return 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void readDisplay(XmlPullParser parser) throws XmlPullParserException, NumberFormatException, IOException {
        String name = parser.getAttributeValue(null, "name");
        if (name != null) {
            Entry entry = new Entry(name);
            entry.overscanLeft = getIntAttribute(parser, "overscanLeft");
            entry.overscanTop = getIntAttribute(parser, "overscanTop");
            entry.overscanRight = getIntAttribute(parser, "overscanRight");
            entry.overscanBottom = getIntAttribute(parser, "overscanBottom");
            this.mEntries.put(name, entry);
        }
        XmlUtils.skipCurrentTag(parser);
    }

    public void writeSettingsLocked() {
        try {
            FileOutputStream stream = this.mFile.startWrite();
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(stream, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, "display-settings");
                for (Entry entry : this.mEntries.values()) {
                    fastXmlSerializer.startTag(null, "display");
                    fastXmlSerializer.attribute(null, "name", entry.name);
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
                this.mFile.finishWrite(stream);
            } catch (IOException e) {
                Slog.w(TAG, "Failed to write display settings, restoring backup.", e);
                this.mFile.failWrite(stream);
            }
        } catch (IOException e2) {
            Slog.w(TAG, "Failed to write display settings: " + e2);
        }
    }
}
