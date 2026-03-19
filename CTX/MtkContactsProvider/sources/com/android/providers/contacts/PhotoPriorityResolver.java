package com.android.providers.contacts;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import android.util.ArrayMap;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PhotoPriorityResolver {
    private static final String[] METADATA_CONTACTS_NAMES = {"android.provider.ALTERNATE_CONTACTS_STRUCTURE", "android.provider.CONTACTS_STRUCTURE"};
    private Context mContext;
    private ArrayMap<String, Integer> mPhotoPriorities = new ArrayMap<>();

    public PhotoPriorityResolver(Context context) {
        this.mContext = context;
    }

    public synchronized int getPhotoPriority(String str) {
        if (str == null) {
            return 7;
        }
        Integer numValueOf = this.mPhotoPriorities.get(str);
        if (numValueOf == null) {
            numValueOf = Integer.valueOf(resolvePhotoPriority(str));
            this.mPhotoPriorities.put(str, numValueOf);
        }
        return numValueOf.intValue();
    }

    private int resolvePhotoPriority(String str) {
        for (AuthenticatorDescription authenticatorDescription : AccountManager.get(this.mContext).getAuthenticatorTypes()) {
            if (str.equals(authenticatorDescription.type)) {
                return resolvePhotoPriorityFromMetaData(authenticatorDescription.packageName);
            }
        }
        return 7;
    }

    int resolvePhotoPriorityFromMetaData(String str) {
        PackageManager packageManager = this.mContext.getPackageManager();
        List<ResolveInfo> listQueryIntentServices = packageManager.queryIntentServices(new Intent("android.content.SyncAdapter").setPackage(str), 132);
        if (listQueryIntentServices != null) {
            Iterator<ResolveInfo> it = listQueryIntentServices.iterator();
            while (it.hasNext()) {
                ServiceInfo serviceInfo = it.next().serviceInfo;
                if (serviceInfo != null) {
                    for (String str2 : METADATA_CONTACTS_NAMES) {
                        XmlResourceParser xmlResourceParserLoadXmlMetaData = serviceInfo.loadXmlMetaData(packageManager, str2);
                        if (xmlResourceParserLoadXmlMetaData != null) {
                            return loadPhotoPriorityFromXml(this.mContext, xmlResourceParserLoadXmlMetaData);
                        }
                    }
                }
            }
            return 7;
        }
        return 7;
    }

    private int loadPhotoPriorityFromXml(Context context, XmlPullParser xmlPullParser) {
        int next;
        do {
            try {
                next = xmlPullParser.next();
                if (next == 2) {
                    break;
                }
            } catch (IOException e) {
                throw new IllegalStateException("Problem reading XML", e);
            } catch (XmlPullParserException e2) {
                throw new IllegalStateException("Problem reading XML", e2);
            }
        } while (next != 1);
        if (next != 2) {
            throw new IllegalStateException("No start tag found");
        }
        int depth = xmlPullParser.getDepth();
        int iConvertValueToInt = 7;
        while (true) {
            int next2 = xmlPullParser.next();
            if ((next2 == 3 && xmlPullParser.getDepth() <= depth) || next2 == 1) {
                break;
            }
            String name = xmlPullParser.getName();
            if (next2 == 2 && "Picture".equals(name)) {
                int attributeCount = xmlPullParser.getAttributeCount();
                for (int i = 0; i < attributeCount; i++) {
                    String attributeName = xmlPullParser.getAttributeName(i);
                    if ("priority".equals(attributeName)) {
                        iConvertValueToInt = XmlUtils.convertValueToInt(xmlPullParser.getAttributeValue(i), 7);
                    } else {
                        throw new IllegalStateException("Unsupported attribute " + attributeName);
                    }
                }
            }
        }
    }
}
