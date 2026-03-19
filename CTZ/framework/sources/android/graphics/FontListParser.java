package android.graphics;

import android.graphics.fonts.FontVariationAxis;
import android.media.TtmlUtils;
import android.media.tv.TvContract;
import android.os.DropBoxManager;
import android.security.KeyChain;
import android.speech.tts.TextToSpeech;
import android.text.FontConfig;
import android.util.Xml;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class FontListParser {
    private static final Pattern FILENAME_WHITESPACE_PATTERN = Pattern.compile("^[ \\n\\r\\t]+|[ \\n\\r\\t]+$");

    public static FontConfig parse(InputStream inputStream) throws XmlPullParserException, IOException {
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(inputStream, null);
            xmlPullParserNewPullParser.nextTag();
            return readFamilies(xmlPullParserNewPullParser);
        } finally {
            inputStream.close();
        }
    }

    private static FontConfig readFamilies(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        xmlPullParser.require(2, null, "familyset");
        while (xmlPullParser.next() != 3) {
            if (xmlPullParser.getEventType() == 2) {
                String name = xmlPullParser.getName();
                if (name.equals("family")) {
                    arrayList.add(readFamily(xmlPullParser));
                } else if (name.equals(KeyChain.EXTRA_ALIAS)) {
                    arrayList2.add(readAlias(xmlPullParser));
                } else {
                    skip(xmlPullParser);
                }
            }
        }
        return new FontConfig((FontConfig.Family[]) arrayList.toArray(new FontConfig.Family[arrayList.size()]), (FontConfig.Alias[]) arrayList2.toArray(new FontConfig.Alias[arrayList2.size()]));
    }

    private static FontConfig.Family readFamily(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        String[] strArrSplit;
        int i;
        String attributeValue = xmlPullParser.getAttributeValue(null, "name");
        String attributeValue2 = xmlPullParser.getAttributeValue(null, "lang");
        if (attributeValue2 != null) {
            strArrSplit = attributeValue2.split("\\s+");
        } else {
            strArrSplit = null;
        }
        String attributeValue3 = xmlPullParser.getAttributeValue(null, TextToSpeech.Engine.KEY_PARAM_VARIANT);
        ArrayList arrayList = new ArrayList();
        while (true) {
            i = 2;
            if (xmlPullParser.next() == 3) {
                break;
            }
            if (xmlPullParser.getEventType() == 2) {
                if (xmlPullParser.getName().equals("font")) {
                    arrayList.add(readFont(xmlPullParser));
                } else {
                    skip(xmlPullParser);
                }
            }
        }
        if (attributeValue3 != null) {
            if (attributeValue3.equals("compact")) {
                i = 1;
            } else if (!attributeValue3.equals("elegant")) {
            }
        } else {
            i = 0;
        }
        return new FontConfig.Family(attributeValue, (FontConfig.Font[]) arrayList.toArray(new FontConfig.Font[arrayList.size()]), strArrSplit, i);
    }

    private static FontConfig.Font readFont(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        String attributeValue = xmlPullParser.getAttributeValue(null, "index");
        int i = attributeValue == null ? 0 : Integer.parseInt(attributeValue);
        ArrayList arrayList = new ArrayList();
        String attributeValue2 = xmlPullParser.getAttributeValue(null, TvContract.PreviewPrograms.COLUMN_WEIGHT);
        int i2 = attributeValue2 == null ? 400 : Integer.parseInt(attributeValue2);
        boolean zEquals = "italic".equals(xmlPullParser.getAttributeValue(null, TtmlUtils.TAG_STYLE));
        String attributeValue3 = xmlPullParser.getAttributeValue(null, "fallbackFor");
        StringBuilder sb = new StringBuilder();
        while (xmlPullParser.next() != 3) {
            if (xmlPullParser.getEventType() == 4) {
                sb.append(xmlPullParser.getText());
            }
            if (xmlPullParser.getEventType() == 2) {
                if (xmlPullParser.getName().equals("axis")) {
                    arrayList.add(readAxis(xmlPullParser));
                } else {
                    skip(xmlPullParser);
                }
            }
        }
        return new FontConfig.Font(FILENAME_WHITESPACE_PATTERN.matcher(sb).replaceAll(""), i, (FontVariationAxis[]) arrayList.toArray(new FontVariationAxis[arrayList.size()]), i2, zEquals, attributeValue3);
    }

    private static FontVariationAxis readAxis(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        String attributeValue = xmlPullParser.getAttributeValue(null, DropBoxManager.EXTRA_TAG);
        String attributeValue2 = xmlPullParser.getAttributeValue(null, "stylevalue");
        skip(xmlPullParser);
        return new FontVariationAxis(attributeValue, Float.parseFloat(attributeValue2));
    }

    private static FontConfig.Alias readAlias(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int i;
        String attributeValue = xmlPullParser.getAttributeValue(null, "name");
        String attributeValue2 = xmlPullParser.getAttributeValue(null, "to");
        String attributeValue3 = xmlPullParser.getAttributeValue(null, TvContract.PreviewPrograms.COLUMN_WEIGHT);
        if (attributeValue3 == null) {
            i = 400;
        } else {
            i = Integer.parseInt(attributeValue3);
        }
        skip(xmlPullParser);
        return new FontConfig.Alias(attributeValue, attributeValue2, i);
    }

    private static void skip(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int i = 1;
        while (i > 0) {
            switch (xmlPullParser.next()) {
                case 2:
                    i++;
                    break;
                case 3:
                    i--;
                    break;
            }
        }
    }
}
