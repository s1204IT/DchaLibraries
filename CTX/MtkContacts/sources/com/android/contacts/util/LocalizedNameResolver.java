package com.android.contacts.util;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;
import com.android.contacts.R;
import com.android.contacts.model.account.ExternalAccountType;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class LocalizedNameResolver {
    public static String getAllContactsName(Context context, String str) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        if (str == null) {
            return null;
        }
        return resolveAllContactsName(context, str);
    }

    private static String resolveAllContactsName(Context context, String str) {
        for (AuthenticatorDescription authenticatorDescription : AccountManager.get(context).getAuthenticatorTypes()) {
            if (str.equals(authenticatorDescription.type)) {
                return resolveAllContactsNameFromMetaData(context, authenticatorDescription.packageName);
            }
        }
        return null;
    }

    private static String resolveAllContactsNameFromMetaData(Context context, String str) {
        XmlResourceParser xmlResourceParserLoadContactsXml = ExternalAccountType.loadContactsXml(context, str);
        if (xmlResourceParserLoadContactsXml != null) {
            return loadAllContactsNameFromXml(context, xmlResourceParserLoadContactsXml, str);
        }
        return null;
    }

    private static String loadAllContactsNameFromXml(Context context, XmlPullParser xmlPullParser, String str) {
        int next;
        try {
            AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlPullParser);
            do {
                next = xmlPullParser.next();
                if (next == 2) {
                    break;
                }
            } while (next != 1);
            if (next != 2) {
                throw new IllegalStateException("No start tag found");
            }
            int depth = xmlPullParser.getDepth();
            while (true) {
                int next2 = xmlPullParser.next();
                if ((next2 == 3 && xmlPullParser.getDepth() <= depth) || next2 == 1) {
                    break;
                }
                String name = xmlPullParser.getName();
                if (next2 == 2 && "ContactsDataKind".equals(name)) {
                    TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSetAsAttributeSet, R.styleable.ContactsDataKind);
                    try {
                        String nonResourceString = typedArrayObtainStyledAttributes.getNonResourceString(5);
                        if (nonResourceString != null) {
                            return nonResourceString;
                        }
                        int resourceId = typedArrayObtainStyledAttributes.getResourceId(5, 0);
                        if (resourceId == 0) {
                            return null;
                        }
                        try {
                            try {
                                return context.getPackageManager().getResourcesForApplication(str).getString(resourceId);
                            } catch (Resources.NotFoundException e) {
                                return null;
                            }
                        } catch (PackageManager.NameNotFoundException e2) {
                            return null;
                        }
                    } finally {
                        typedArrayObtainStyledAttributes.recycle();
                    }
                }
            }
            return null;
        } catch (IOException e3) {
            throw new IllegalStateException("Problem reading XML", e3);
        } catch (XmlPullParserException e4) {
            throw new IllegalStateException("Problem reading XML", e4);
        }
    }
}
