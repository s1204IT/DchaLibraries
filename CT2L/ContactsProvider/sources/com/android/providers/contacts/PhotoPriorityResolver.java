package com.android.providers.contacts;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.XmlResourceParser;
import com.android.internal.util.XmlUtils;
import com.google.android.collect.Maps;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PhotoPriorityResolver {
    private static final String[] METADATA_CONTACTS_NAMES = {"android.provider.ALTERNATE_CONTACTS_STRUCTURE", "android.provider.CONTACTS_STRUCTURE"};
    private Context mContext;
    private HashMap<String, Integer> mPhotoPriorities = Maps.newHashMap();

    public PhotoPriorityResolver(Context context) {
        this.mContext = context;
    }

    public synchronized int getPhotoPriority(String accountType) {
        int iIntValue;
        if (accountType == null) {
            iIntValue = 7;
        } else {
            Integer priority = this.mPhotoPriorities.get(accountType);
            if (priority == null) {
                priority = Integer.valueOf(resolvePhotoPriority(accountType));
                this.mPhotoPriorities.put(accountType, priority);
            }
            iIntValue = priority.intValue();
        }
        return iIntValue;
    }

    private int resolvePhotoPriority(String accountType) {
        AccountManager am = AccountManager.get(this.mContext);
        AuthenticatorDescription[] arr$ = am.getAuthenticatorTypes();
        for (AuthenticatorDescription auth : arr$) {
            if (accountType.equals(auth.type)) {
                return resolvePhotoPriorityFromMetaData(auth.packageName);
            }
        }
        return 7;
    }

    int resolvePhotoPriorityFromMetaData(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        Intent intent = new Intent("android.content.SyncAdapter").setPackage(packageName);
        List<ResolveInfo> intentServices = pm.queryIntentServices(intent, 132);
        if (intentServices != null) {
            for (ResolveInfo resolveInfo : intentServices) {
                ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                if (serviceInfo != null) {
                    String[] arr$ = METADATA_CONTACTS_NAMES;
                    for (String metadataName : arr$) {
                        XmlResourceParser parser = serviceInfo.loadXmlMetaData(pm, metadataName);
                        if (parser != null) {
                            return loadPhotoPriorityFromXml(this.mContext, parser);
                        }
                    }
                }
            }
        }
        return 7;
    }

    private int loadPhotoPriorityFromXml(Context context, XmlPullParser parser) {
        int type;
        int priority = 7;
        do {
            try {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } catch (IOException e) {
                throw new IllegalStateException("Problem reading XML", e);
            } catch (XmlPullParserException e2) {
                throw new IllegalStateException("Problem reading XML", e2);
            }
        } while (type != 1);
        if (type != 2) {
            throw new IllegalStateException("No start tag found");
        }
        int depth = parser.getDepth();
        while (true) {
            int type2 = parser.next();
            if ((type2 == 3 && parser.getDepth() <= depth) || type2 == 1) {
                break;
            }
            String name = parser.getName();
            if (type2 == 2 && "Picture".equals(name)) {
                int attributeCount = parser.getAttributeCount();
                for (int i = 0; i < attributeCount; i++) {
                    String attr = parser.getAttributeName(i);
                    if ("priority".equals(attr)) {
                        priority = XmlUtils.convertValueToInt(parser.getAttributeValue(i), 7);
                    } else {
                        throw new IllegalStateException("Unsupported attribute " + attr);
                    }
                }
            }
        }
    }
}
