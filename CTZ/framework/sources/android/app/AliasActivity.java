package android.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.security.KeyChain;
import android.util.AttributeSet;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AliasActivity extends Activity {
    public final String ALIAS_META_DATA = "android.app.alias";

    @Override
    protected void onCreate(Bundle bundle) throws Throwable {
        Throwable th;
        XmlPullParserException e;
        IOException e2;
        PackageManager.NameNotFoundException e3;
        super.onCreate(bundle);
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                XmlResourceParser xmlResourceParserLoadXmlMetaData = getPackageManager().getActivityInfo(getComponentName(), 128).loadXmlMetaData(getPackageManager(), "android.app.alias");
                try {
                    if (xmlResourceParserLoadXmlMetaData == null) {
                        throw new RuntimeException("Alias requires a meta-data field android.app.alias");
                    }
                    Intent alias = parseAlias(xmlResourceParserLoadXmlMetaData);
                    if (alias == null) {
                        throw new RuntimeException("No <intent> tag found in alias description");
                    }
                    startActivity(alias);
                    finish();
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        xmlResourceParserLoadXmlMetaData.close();
                    }
                } catch (PackageManager.NameNotFoundException e4) {
                    e3 = e4;
                    throw new RuntimeException("Error parsing alias", e3);
                } catch (IOException e5) {
                    e2 = e5;
                    throw new RuntimeException("Error parsing alias", e2);
                } catch (XmlPullParserException e6) {
                    e = e6;
                    throw new RuntimeException("Error parsing alias", e);
                }
            } catch (Throwable th2) {
                th = th2;
                if (0 != 0) {
                    xmlResourceParser.close();
                }
                throw th;
            }
        } catch (PackageManager.NameNotFoundException e7) {
            e3 = e7;
        } catch (IOException e8) {
            e2 = e8;
        } catch (XmlPullParserException e9) {
            e = e9;
        } catch (Throwable th3) {
            th = th3;
            if (0 != 0) {
            }
            throw th;
        }
    }

    private Intent parseAlias(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int next;
        AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlPullParser);
        do {
            next = xmlPullParser.next();
            if (next == 1) {
                break;
            }
        } while (next != 2);
        String name = xmlPullParser.getName();
        if (!KeyChain.EXTRA_ALIAS.equals(name)) {
            throw new RuntimeException("Alias meta-data must start with <alias> tag; found" + name + " at " + xmlPullParser.getPositionDescription());
        }
        int depth = xmlPullParser.getDepth();
        Intent intent = null;
        while (true) {
            int next2 = xmlPullParser.next();
            if (next2 == 1 || (next2 == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next2 != 3 && next2 != 4) {
                if ("intent".equals(xmlPullParser.getName())) {
                    Intent intent2 = Intent.parseIntent(getResources(), xmlPullParser, attributeSetAsAttributeSet);
                    if (intent == null) {
                        intent = intent2;
                    }
                } else {
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
        return intent;
    }
}
