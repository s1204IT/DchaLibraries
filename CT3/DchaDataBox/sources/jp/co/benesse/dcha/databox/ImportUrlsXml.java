package jp.co.benesse.dcha.databox;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import jp.co.benesse.dcha.databox.db.ContractKvs;
import jp.co.benesse.dcha.databox.db.KvsColumns;
import jp.co.benesse.dcha.util.FileUtils;
import jp.co.benesse.dcha.util.Logger;
import org.xmlpull.v1.XmlPullParser;

public class ImportUrlsXml {
    private static final Uri URI_TEST_ENVIRONMENT_INFO = Uri.withAppendedPath(ContractKvs.KVS.contentUri, "test.environment.info");
    private static final String XML_TAG_CONNECT_INFO = "connect_info";
    private static final String XML_TAG_ENVIRONMENT = "environment";
    private static final String XML_TAG_ID = "id";
    private static final String XML_TAG_TEXT = "text";
    private static final String XML_TAG_URL = "url";
    private static final String XML_TAG_VERSION = "version";
    protected File mPath = null;

    public void setPath(File path) {
        this.mPath = path;
    }

    public void delete(Context context) {
        try {
            ContentResolver cr = context.getContentResolver();
            cr.delete(URI_TEST_ENVIRONMENT_INFO, null, null);
        } catch (Exception e) {
        }
    }

    public boolean execImport(Context context) throws Throwable {
        if (context == null || this.mPath == null || !this.mPath.exists() || !this.mPath.isFile() || !this.mPath.canRead()) {
            return false;
        }
        try {
            ContentResolver cr = context.getContentResolver();
            Map<String, String> elements = parseXml(this.mPath);
            for (Map.Entry<String, String> entry : elements.entrySet()) {
                ContentValues values = new ContentValues();
                values.put(KvsColumns.KEY, entry.getKey());
                values.put(KvsColumns.VALUE, entry.getValue());
                cr.insert(URI_TEST_ENVIRONMENT_INFO, values);
            }
            boolean result = !elements.isEmpty();
            return result;
        } catch (Exception e) {
            return false;
        }
    }

    protected Map<String, String> parseXml(File file) throws Throwable {
        FileInputStream fis;
        InputStreamReader isr;
        Map<String, String> result = new HashMap<>();
        InputStreamReader isr2 = null;
        FileInputStream fis2 = null;
        try {
            try {
                fis = new FileInputStream(file);
                try {
                    isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                } catch (Exception e) {
                    fis2 = fis;
                } catch (Throwable th) {
                    th = th;
                    fis2 = fis;
                }
            } catch (Exception e2) {
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            XmlPullParser xpp = Xml.newPullParser();
            xpp.setInput(isr);
            String tag = "";
            Map<String, String> elements = null;
            for (int eventType = xpp.getEventType(); eventType != 1; eventType = xpp.next()) {
                switch (eventType) {
                    case Logger.ALL:
                        tag = xpp.getName();
                        if (XML_TAG_CONNECT_INFO.equals(tag)) {
                            elements = new HashMap<>();
                        }
                        break;
                    case 3:
                        String tag2 = xpp.getName();
                        if (elements != null && XML_TAG_CONNECT_INFO.equals(tag2)) {
                            if (elements.containsKey(XML_TAG_ID) && elements.containsKey(XML_TAG_URL)) {
                                URL url = new URL(elements.get(XML_TAG_URL));
                                if (TextUtils.isEmpty(url.getHost()) || TextUtils.isEmpty(url.getProtocol())) {
                                    throw new MalformedURLException();
                                }
                                result.put(elements.get(XML_TAG_ID), url.toString());
                            } else if (elements.containsKey(XML_TAG_ID) && elements.containsKey(XML_TAG_TEXT)) {
                                result.put(elements.get(XML_TAG_ID), elements.get(XML_TAG_TEXT));
                            } else {
                                throw new IllegalArgumentException();
                            }
                            elements.clear();
                            elements = null;
                        } else if ((XML_TAG_ENVIRONMENT.equals(tag2) || XML_TAG_VERSION.equals(tag2)) && !result.containsKey(tag2)) {
                            result.put(tag2, "");
                        }
                        tag = "";
                        break;
                    case 4:
                        if (XML_TAG_ENVIRONMENT.equals(tag) || XML_TAG_VERSION.equals(tag)) {
                            result.put(tag, xpp.getText());
                        } else if (elements != null && (XML_TAG_ID.equals(tag) || XML_TAG_URL.equals(tag) || XML_TAG_TEXT.equals(tag))) {
                            elements.put(tag, xpp.getText());
                        }
                        break;
                }
            }
            if (!result.containsKey(XML_TAG_ENVIRONMENT) || !result.containsKey(XML_TAG_VERSION)) {
                result.clear();
            }
            FileUtils.close(isr);
            FileUtils.close(fis);
        } catch (Exception e3) {
            fis2 = fis;
            isr2 = isr;
            result.clear();
            FileUtils.close(isr2);
            FileUtils.close(fis2);
        } catch (Throwable th3) {
            th = th3;
            fis2 = fis;
            isr2 = isr;
            FileUtils.close(isr2);
            FileUtils.close(fis2);
            throw th;
        }
        return result;
    }
}
