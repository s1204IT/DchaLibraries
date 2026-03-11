package com.android.settingslib.license;

import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class LicenseHtmlGeneratorFromXml {
    private final List<File> mXmlFiles;
    private final Map<String, String> mFileNameToContentIdMap = new HashMap();
    private final Map<String, String> mContentIdToFileContentMap = new HashMap();

    static class ContentIdAndFileNames {
        final String mContentId;
        final List<String> mFileNameList = new ArrayList();

        ContentIdAndFileNames(String str) {
            this.mContentId = str;
        }
    }

    private LicenseHtmlGeneratorFromXml(List<File> list) {
        this.mXmlFiles = list;
    }

    public static boolean generateHtml(List<File> list, File file) {
        return new LicenseHtmlGeneratorFromXml(list).generateHtml(file);
    }

    private boolean generateHtml(File file) {
        PrintWriter printWriter;
        Throwable e;
        Iterator<File> it = this.mXmlFiles.iterator();
        while (it.hasNext()) {
            parse(it.next());
        }
        if (this.mFileNameToContentIdMap.isEmpty() || this.mContentIdToFileContentMap.isEmpty()) {
            return false;
        }
        try {
            printWriter = new PrintWriter(file);
        } catch (FileNotFoundException | SecurityException e2) {
            printWriter = null;
            e = e2;
        }
        try {
            generateHtml(this.mFileNameToContentIdMap, this.mContentIdToFileContentMap, printWriter);
            printWriter.flush();
            printWriter.close();
            return true;
        } catch (FileNotFoundException | SecurityException e3) {
            e = e3;
            Log.e("LicenseHtmlGeneratorFromXml", "Failed to generate " + file, e);
            if (printWriter != null) {
                printWriter.close();
            }
            return false;
        }
    }

    private void parse(File file) {
        InputStreamReader fileReader;
        if (file == null || !file.exists() || file.length() == 0) {
            return;
        }
        InputStreamReader inputStreamReader = null;
        try {
            if (file.getName().endsWith(".gz")) {
                fileReader = new InputStreamReader(new GZIPInputStream(new FileInputStream(file)));
            } else {
                fileReader = new FileReader(file);
            }
            inputStreamReader = fileReader;
            parse(inputStreamReader, this.mFileNameToContentIdMap, this.mContentIdToFileContentMap);
            inputStreamReader.close();
        } catch (IOException | XmlPullParserException e) {
            Log.e("LicenseHtmlGeneratorFromXml", "Failed to parse " + file, e);
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e2) {
                    Log.w("LicenseHtmlGeneratorFromXml", "Failed to close " + file);
                }
            }
        }
    }

    static void parse(InputStreamReader inputStreamReader, Map<String, String> map, Map<String, String> map2) throws XmlPullParserException, IOException {
        HashMap map3 = new HashMap();
        Map<? extends String, ? extends String> map4 = new HashMap<>();
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(inputStreamReader);
        xmlPullParserNewPullParser.nextTag();
        xmlPullParserNewPullParser.require(2, "", "licenses");
        for (int eventType = xmlPullParserNewPullParser.getEventType(); eventType != 1; eventType = xmlPullParserNewPullParser.next()) {
            if (eventType == 2) {
                if ("file-name".equals(xmlPullParserNewPullParser.getName())) {
                    String attributeValue = xmlPullParserNewPullParser.getAttributeValue("", "contentId");
                    if (!TextUtils.isEmpty(attributeValue)) {
                        String strTrim = readText(xmlPullParserNewPullParser).trim();
                        if (!TextUtils.isEmpty(strTrim)) {
                            map3.put(strTrim, attributeValue);
                        }
                    }
                } else if ("file-content".equals(xmlPullParserNewPullParser.getName())) {
                    String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue("", "contentId");
                    if (!TextUtils.isEmpty(attributeValue2) && !map2.containsKey(attributeValue2) && !map4.containsKey(attributeValue2)) {
                        String text = readText(xmlPullParserNewPullParser);
                        if (!TextUtils.isEmpty(text)) {
                            map4.put(attributeValue2, text);
                        }
                    }
                }
            }
        }
        map.putAll(map3);
        map2.putAll(map4);
    }

    private static String readText(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        StringBuffer stringBuffer = new StringBuffer();
        int next = xmlPullParser.next();
        while (next == 4) {
            stringBuffer.append(xmlPullParser.getText());
            next = xmlPullParser.next();
        }
        return stringBuffer.toString();
    }

    static void generateHtml(Map<String, String> map, Map<String, String> map2, PrintWriter printWriter) {
        ArrayList<String> arrayList = new ArrayList();
        arrayList.addAll(map.keySet());
        Collections.sort(arrayList);
        printWriter.println("<html><head>\n<style type=\"text/css\">\nbody { padding: 0; font-family: sans-serif; }\n.same-license { background-color: #eeeeee;\n                border-top: 20px solid white;\n                padding: 10px; }\n.label { font-weight: bold; }\n.file-list { margin-left: 1em; color: blue; }\n</style>\n</head><body topmargin=\"0\" leftmargin=\"0\" rightmargin=\"0\" bottommargin=\"0\">\n<div class=\"toc\">\n<ul>");
        HashMap map3 = new HashMap();
        ArrayList<ContentIdAndFileNames> arrayList2 = new ArrayList();
        int i = 0;
        for (String str : arrayList) {
            String str2 = map.get(str);
            if (!map3.containsKey(str2)) {
                map3.put(str2, Integer.valueOf(i));
                arrayList2.add(new ContentIdAndFileNames(str2));
                i++;
            }
            int iIntValue = ((Integer) map3.get(str2)).intValue();
            ((ContentIdAndFileNames) arrayList2.get(iIntValue)).mFileNameList.add(str);
            printWriter.format("<li><a href=\"#id%d\">%s</a></li>\n", Integer.valueOf(iIntValue), str);
        }
        printWriter.println("</ul>\n</div><!-- table of contents -->\n<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        int i2 = 0;
        for (ContentIdAndFileNames contentIdAndFileNames : arrayList2) {
            printWriter.format("<tr id=\"id%d\"><td class=\"same-license\">\n", Integer.valueOf(i2));
            printWriter.println("<div class=\"label\">Notices for file(s):</div>");
            printWriter.println("<div class=\"file-list\">");
            Iterator<String> it = contentIdAndFileNames.mFileNameList.iterator();
            while (it.hasNext()) {
                printWriter.format("%s <br/>\n", it.next());
            }
            printWriter.println("</div><!-- file-list -->");
            printWriter.println("<pre class=\"license-text\">");
            printWriter.println(map2.get(contentIdAndFileNames.mContentId));
            printWriter.println("</pre><!-- license-text -->");
            printWriter.println("</td></tr><!-- same-license -->");
            i2++;
        }
        printWriter.println("</table></body></html>");
    }
}
