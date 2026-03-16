package com.android.contacts.common.util;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;
import com.android.contacts.common.model.account.ExternalAccountType;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class LocalizedNameResolver {
    public static String getAllContactsName(Context context, String accountType) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        if (accountType == null) {
            return null;
        }
        return resolveAllContactsName(context, accountType);
    }

    private static String resolveAllContactsName(Context context, String accountType) {
        AccountManager am = AccountManager.get(context);
        AuthenticatorDescription[] arr$ = am.getAuthenticatorTypes();
        for (AuthenticatorDescription auth : arr$) {
            if (accountType.equals(auth.type)) {
                return resolveAllContactsNameFromMetaData(context, auth.packageName);
            }
        }
        return null;
    }

    private static String resolveAllContactsNameFromMetaData(Context context, String packageName) {
        XmlResourceParser parser = ExternalAccountType.loadContactsXml(context, packageName);
        if (parser != null) {
            return loadAllContactsNameFromXml(context, parser, packageName);
        }
        return null;
    }

    private static String loadAllContactsNameFromXml(Context context, XmlPullParser parser, String packageName) {
        int type;
        try {
            AttributeSet attrs = Xml.asAttributeSet(parser);
            do {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } while (type != 1);
            if (type == 2) {
                int depth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if ((type2 == 3 && parser.getDepth() <= depth) || type2 == 1) {
                        break;
                    }
                    String name = parser.getName();
                    if (type2 == 2 && "ContactsDataKind".equals(name)) {
                        break;
                    }
                }
            } else {
                throw new IllegalStateException("No start tag found");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Problem reading XML", e);
        } catch (XmlPullParserException e2) {
            throw new IllegalStateException("Problem reading XML", e2);
        }
    }
}
