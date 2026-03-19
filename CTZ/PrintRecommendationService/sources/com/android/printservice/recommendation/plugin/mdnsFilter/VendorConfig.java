package com.android.printservice.recommendation.plugin.mdnsFilter;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.ArrayMap;
import androidx.core.util.Preconditions;
import com.android.printservice.recommendation.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class VendorConfig {
    private static ArrayMap<String, VendorConfig> sConfigs;
    private static final Object sLock = new Object();
    public final List<String> mDNSNames;
    public final String name;
    public final String packageName;

    private interface TagReader<T> {
        T readTag(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException;
    }

    private VendorConfig(String str, String str2, List<String> list) {
        this.name = (String) Preconditions.checkStringNotEmpty(str);
        this.packageName = (String) Preconditions.checkStringNotEmpty(str2);
        this.mDNSNames = (List) Preconditions.checkCollectionElementsNotNull(list, "mDNSName");
    }

    public static Collection<VendorConfig> getAllConfigs(Context context) throws XmlPullParserException, IOException {
        Collection<VendorConfig> collectionValues;
        synchronized (sLock) {
            if (sConfigs == null) {
                sConfigs = readVendorConfigs(context);
            }
            collectionValues = sConfigs.values();
        }
        return collectionValues;
    }

    private static String readText(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        if (xmlPullParser.next() != 4) {
            return "";
        }
        String text = xmlPullParser.getText();
        xmlPullParser.nextTag();
        return text;
    }

    private static String readSimpleTag(Context context, XmlPullParser xmlPullParser, String str, boolean z) throws XmlPullParserException, IOException {
        xmlPullParser.require(2, null, str);
        String text = readText(xmlPullParser);
        xmlPullParser.require(3, null, str);
        if (z && text.startsWith("@")) {
            return context.getResources().getString(context.getResources().getIdentifier(text, null, context.getPackageName()));
        }
        return text;
    }

    private static <T> ArrayList<T> readTagList(XmlPullParser xmlPullParser, String str, String str2, TagReader<T> tagReader) throws XmlPullParserException, IOException {
        ArrayList<T> arrayList = new ArrayList<>();
        xmlPullParser.require(2, null, str);
        while (xmlPullParser.next() != 3) {
            if (xmlPullParser.getEventType() == 2) {
                if (xmlPullParser.getName().equals(str2)) {
                    arrayList.add(tagReader.readTag(xmlPullParser, str2));
                } else {
                    throw new XmlPullParserException("Unexpected subtag of " + str + ": " + xmlPullParser.getName());
                }
            }
        }
        return arrayList;
    }

    private static ArrayMap<String, VendorConfig> readVendorConfigs(final Context context) throws XmlPullParserException, IOException {
        XmlResourceParser xml = context.getResources().getXml(R.xml.vendorconfigs);
        do {
            Throwable th = null;
            try {
            } catch (Throwable th2) {
                if (xml != null) {
                    if (0 != 0) {
                        try {
                            xml.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        xml.close();
                    }
                }
                throw th2;
            }
        } while (xml.next() != 2);
        ArrayList tagList = readTagList(xml, "vendors", "vendor", new TagReader<VendorConfig>() {
            @Override
            public VendorConfig readTag(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
                return VendorConfig.readVendorConfig(context, xmlPullParser, str);
            }
        });
        ArrayMap<String, VendorConfig> arrayMap = new ArrayMap<>(tagList.size());
        int size = tagList.size();
        for (int i = 0; i < size; i++) {
            VendorConfig vendorConfig = (VendorConfig) tagList.get(i);
            arrayMap.put(vendorConfig.name, vendorConfig);
        }
        if (xml != null) {
            xml.close();
        }
        return arrayMap;
    }

    private static VendorConfig readVendorConfig(final Context context, XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        String simpleTag = null;
        xmlPullParser.require(2, null, str);
        String simpleTag2 = null;
        List listEmptyList = null;
        while (xmlPullParser.next() != 3) {
            if (xmlPullParser.getEventType() == 2) {
                String name = xmlPullParser.getName();
                byte b = -1;
                int iHashCode = name.hashCode();
                if (iHashCode != -1489741865) {
                    if (iHashCode != -807062458) {
                        if (iHashCode == 3373707 && name.equals("name")) {
                            b = 0;
                        }
                    } else if (name.equals("package")) {
                        b = 1;
                    }
                } else if (name.equals("mdns-names")) {
                    b = 2;
                }
                switch (b) {
                    case 0:
                        simpleTag = readSimpleTag(context, xmlPullParser, "name", false);
                        break;
                    case 1:
                        simpleTag2 = readSimpleTag(context, xmlPullParser, "package", true);
                        break;
                    case 2:
                        listEmptyList = readTagList(xmlPullParser, "mdns-names", "mdns-name", new TagReader<String>() {
                            @Override
                            public String readTag(XmlPullParser xmlPullParser2, String str2) throws XmlPullParserException, IOException {
                                return VendorConfig.readSimpleTag(context, xmlPullParser2, str2, true);
                            }
                        });
                        break;
                    default:
                        throw new XmlPullParserException("Unexpected subtag of " + str + ": " + name);
                }
            }
        }
        if (simpleTag == null) {
            throw new XmlPullParserException("name is required");
        }
        if (simpleTag2 == null) {
            throw new XmlPullParserException("package is required");
        }
        if (listEmptyList == null) {
            listEmptyList = Collections.emptyList();
        }
        return new VendorConfig(simpleTag, simpleTag2, Collections.unmodifiableList(listEmptyList));
    }

    public String toString() {
        return this.name + " -> " + this.packageName + ", " + this.mDNSNames;
    }
}
