package jp.co.benesse.dcha.databox;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import jp.co.benesse.dcha.databox.db.ContractKvs;
import jp.co.benesse.dcha.util.FileUtils;
import org.xmlpull.v1.XmlPullParser;

public class ImportUrlsXml {
    private static final Uri URI_TEST_ENVIRONMENT_INFO = Uri.withAppendedPath(ContractKvs.KVS.contentUri, "test.environment.info");
    private static final String XML_TAG_CONNECT_INFO = "connect_info";
    private static final String XML_TAG_ENVIRONMENT = "environment";
    private static final String XML_TAG_ID = "id";
    private static final String XML_TAG_TEXT = "text";
    private static final String XML_TAG_URL = "url";
    private static final String XML_TAG_VERSION = "version";

    public void delete(Context context) {
        try {
            context.getContentResolver().delete(URI_TEST_ENVIRONMENT_INFO, null, null);
        } catch (Exception unused) {
        }
    }

    public boolean execImport(Context context, File file) {
        if (context == null || file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            return false;
        }
        try {
            ContentResolver contentResolver = context.getContentResolver();
            for (Map.Entry<String, String> entry : parseXml(new FileInputStream(file)).entrySet()) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("key", entry.getKey());
                contentValues.put("value", entry.getValue());
                contentResolver.insert(URI_TEST_ENVIRONMENT_INFO, contentValues);
            }
            return !r8.isEmpty();
        } catch (Exception unused) {
            return false;
        }
    }

    protected Map<String, String> parseXml(InputStream inputStream) {
        HashMap map = new HashMap();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
        try {
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(inputStreamReader);
                HashMap map2 = null;
                String str = BuildConfig.FLAVOR;
                for (int eventType = xmlPullParserNewPullParser.getEventType(); eventType != 1; eventType = xmlPullParserNewPullParser.next()) {
                    if (eventType == 2) {
                        String name = xmlPullParserNewPullParser.getName();
                        if (XML_TAG_CONNECT_INFO.equals(name)) {
                            map2 = new HashMap();
                        }
                        str = name;
                    } else if (eventType == 3) {
                        String name2 = xmlPullParserNewPullParser.getName();
                        if (map2 != null && XML_TAG_CONNECT_INFO.equals(name2)) {
                            if (map2.containsKey(XML_TAG_ID) && map2.containsKey(XML_TAG_URL)) {
                                URL url = new URL((String) map2.get(XML_TAG_URL));
                                if (TextUtils.isEmpty(url.getHost()) || TextUtils.isEmpty(url.getProtocol())) {
                                    throw new MalformedURLException();
                                }
                                map.put(map2.get(XML_TAG_ID), url.toString());
                            } else if (map2.containsKey(XML_TAG_ID) && map2.containsKey(XML_TAG_TEXT)) {
                                map.put(map2.get(XML_TAG_ID), map2.get(XML_TAG_TEXT));
                            } else {
                                throw new IllegalArgumentException();
                            }
                            map2.clear();
                            map2 = null;
                        } else if ((XML_TAG_ENVIRONMENT.equals(name2) || XML_TAG_VERSION.equals(name2)) && !map.containsKey(name2)) {
                            map.put(name2, BuildConfig.FLAVOR);
                        }
                        str = BuildConfig.FLAVOR;
                    } else if (eventType == 4) {
                        if (XML_TAG_ENVIRONMENT.equals(str) || XML_TAG_VERSION.equals(str)) {
                            map.put(str, xmlPullParserNewPullParser.getText());
                        } else if (map2 != null && (XML_TAG_ID.equals(str) || XML_TAG_URL.equals(str) || XML_TAG_TEXT.equals(str))) {
                            map2.put(str, xmlPullParserNewPullParser.getText());
                        }
                    }
                }
                if (!map.containsKey(XML_TAG_ENVIRONMENT) || !map.containsKey(XML_TAG_VERSION)) {
                    map.clear();
                }
            } catch (Exception unused) {
                map.clear();
            }
            return map;
        } finally {
            FileUtils.close(inputStreamReader);
        }
    }
}
