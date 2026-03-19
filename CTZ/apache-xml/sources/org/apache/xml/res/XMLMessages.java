package org.apache.xml.res;

import java.text.MessageFormat;
import java.util.ListResourceBundle;
import java.util.Locale;

public class XMLMessages {
    protected static final String BAD_CODE = "BAD_CODE";
    protected static final String FORMAT_FAILED = "FORMAT_FAILED";
    private static ListResourceBundle XMLBundle = new XMLErrorResources();
    protected Locale fLocale = Locale.getDefault();

    public void setLocale(Locale locale) {
        this.fLocale = locale;
    }

    public Locale getLocale() {
        return this.fLocale;
    }

    public static final String createXMLMessage(String str, Object[] objArr) {
        return createMsg(XMLBundle, str, objArr);
    }

    public static final String createMsg(ListResourceBundle listResourceBundle, String str, Object[] objArr) {
        String string;
        boolean z;
        if (str != null) {
            string = listResourceBundle.getString(str);
        } else {
            string = null;
        }
        if (string == null) {
            string = listResourceBundle.getString("BAD_CODE");
            z = true;
        } else {
            z = false;
        }
        if (objArr != null) {
            try {
                int length = objArr.length;
                for (int i = 0; i < length; i++) {
                    if (objArr[i] == null) {
                        objArr[i] = "";
                    }
                }
                string = MessageFormat.format(string, objArr);
            } catch (Exception e) {
                string = listResourceBundle.getString("FORMAT_FAILED") + " " + string;
            }
        }
        if (z) {
            throw new RuntimeException(string);
        }
        return string;
    }
}
