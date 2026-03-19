package org.apache.xml.serializer.utils;

import java.text.MessageFormat;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Messages {
    private final Locale m_locale = Locale.getDefault();
    private ListResourceBundle m_resourceBundle;
    private String m_resourceBundleName;

    Messages(String str) {
        this.m_resourceBundleName = str;
    }

    private Locale getLocale() {
        return this.m_locale;
    }

    private ListResourceBundle getResourceBundle() {
        return this.m_resourceBundle;
    }

    public final String createMessage(String str, Object[] objArr) {
        if (this.m_resourceBundle == null) {
            this.m_resourceBundle = loadResourceBundle(this.m_resourceBundleName);
        }
        if (this.m_resourceBundle != null) {
            return createMsg(this.m_resourceBundle, str, objArr);
        }
        return "Could not load the resource bundles: " + this.m_resourceBundleName;
    }

    private final String createMsg(ListResourceBundle listResourceBundle, String str, Object[] objArr) {
        String string;
        String str2;
        String str3 = null;
        if (str != null) {
            string = listResourceBundle.getString(str);
        } else {
            str = "";
            string = null;
        }
        boolean z = false;
        if (string == null) {
            try {
                MessageFormat.format(MsgKey.BAD_MSGKEY, str, this.m_resourceBundleName);
            } catch (Exception e) {
                String str4 = "The message key '" + str + "' is not in the message class '" + this.m_resourceBundleName + "'";
            }
        } else {
            if (objArr != null) {
                try {
                    int length = objArr.length;
                    for (int i = 0; i < length; i++) {
                        if (objArr[i] == null) {
                            objArr[i] = "";
                        }
                    }
                    str3 = MessageFormat.format(string, objArr);
                } catch (Exception e2) {
                    try {
                        str2 = MessageFormat.format(MsgKey.BAD_MSGFORMAT, str, this.m_resourceBundleName) + " " + string;
                    } catch (Exception e3) {
                        str2 = "The format of message '" + str + "' in message class '" + this.m_resourceBundleName + "' failed.";
                    }
                    str3 = str2;
                    z = true;
                }
            } else {
                str3 = string;
            }
            if (!z) {
                throw new RuntimeException(str3);
            }
            return str3;
        }
        z = true;
        if (!z) {
        }
    }

    private ListResourceBundle loadResourceBundle(String str) throws MissingResourceException {
        ListResourceBundle listResourceBundle;
        this.m_resourceBundleName = str;
        try {
            listResourceBundle = (ListResourceBundle) ResourceBundle.getBundle(this.m_resourceBundleName, getLocale());
        } catch (MissingResourceException e) {
            try {
                listResourceBundle = (ListResourceBundle) ResourceBundle.getBundle(this.m_resourceBundleName, new Locale("en", "US"));
            } catch (MissingResourceException e2) {
                throw new MissingResourceException("Could not load any resource bundles." + this.m_resourceBundleName, this.m_resourceBundleName, "");
            }
        }
        this.m_resourceBundle = listResourceBundle;
        return listResourceBundle;
    }

    private static String getResourceSuffix(Locale locale) {
        String str = "_" + locale.getLanguage();
        String country = locale.getCountry();
        if (country.equals("TW")) {
            return str + "_" + country;
        }
        return str;
    }
}
