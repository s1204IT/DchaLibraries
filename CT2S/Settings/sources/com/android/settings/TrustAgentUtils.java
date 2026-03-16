package com.android.settings;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

public class TrustAgentUtils {

    public static class TrustAgentComponentInfo {
        ComponentName componentName;
        String summary;
        String title;
    }

    public static boolean checkProvidePermission(ResolveInfo resolveInfo, PackageManager pm) {
        String packageName = resolveInfo.serviceInfo.packageName;
        if (pm.checkPermission("android.permission.PROVIDE_TRUST_AGENT", packageName) == 0) {
            return true;
        }
        Log.w("TrustAgentUtils", "Skipping agent because package " + packageName + " does not have permission android.permission.PROVIDE_TRUST_AGENT.");
        return false;
    }

    public static ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            return null;
        }
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    public static TrustAgentComponentInfo getSettingsComponent(PackageManager pm, ResolveInfo resolveInfo) {
        XmlResourceParser parser;
        int type;
        if (resolveInfo == null || resolveInfo.serviceInfo == null || resolveInfo.serviceInfo.metaData == null) {
            return null;
        }
        String cn = null;
        TrustAgentComponentInfo trustAgentComponentInfo = new TrustAgentComponentInfo();
        XmlResourceParser parser2 = null;
        Exception caughtException = null;
        try {
            parser = resolveInfo.serviceInfo.loadXmlMetaData(pm, "android.service.trust.trustagent");
        } catch (PackageManager.NameNotFoundException e) {
            caughtException = e;
            if (0 != 0) {
                parser2.close();
            }
        } catch (IOException e2) {
            caughtException = e2;
            if (0 != 0) {
                parser2.close();
            }
        } catch (XmlPullParserException e3) {
            caughtException = e3;
            if (0 != 0) {
                parser2.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                parser2.close();
            }
            throw th;
        }
        if (parser == null) {
            Slog.w("TrustAgentUtils", "Can't find android.service.trust.trustagent meta-data");
            if (parser == null) {
                return null;
            }
            parser.close();
            return null;
        }
        Resources res = pm.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        do {
            type = parser.next();
            if (type == 1) {
                break;
            }
        } while (type != 2);
        String nodeName = parser.getName();
        if (!"trust-agent".equals(nodeName)) {
            Slog.w("TrustAgentUtils", "Meta-data does not start with trust-agent tag");
            if (parser == null) {
                return null;
            }
            parser.close();
            return null;
        }
        TypedArray sa = res.obtainAttributes(attrs, com.android.internal.R.styleable.TrustAgent);
        trustAgentComponentInfo.summary = sa.getString(1);
        trustAgentComponentInfo.title = sa.getString(0);
        cn = sa.getString(2);
        sa.recycle();
        if (parser != null) {
            parser.close();
        }
        if (caughtException != null) {
            Slog.w("TrustAgentUtils", "Error parsing : " + resolveInfo.serviceInfo.packageName, caughtException);
            return null;
        }
        if (cn != null && cn.indexOf(47) < 0) {
            cn = resolveInfo.serviceInfo.packageName + "/" + cn;
        }
        trustAgentComponentInfo.componentName = cn == null ? null : ComponentName.unflattenFromString(cn);
        return trustAgentComponentInfo;
    }
}
