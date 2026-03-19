package org.apache.xpath.res;

import java.text.MessageFormat;
import java.util.ListResourceBundle;
import org.apache.xml.res.XMLMessages;

public class XPATHMessages extends XMLMessages {
    private static ListResourceBundle XPATHBundle = new XPATHErrorResources();
    private static final String XPATH_ERROR_RESOURCES = "org.apache.xpath.res.XPATHErrorResources";

    public static final String createXPATHMessage(String str, Object[] objArr) {
        return createXPATHMsg(XPATHBundle, str, objArr);
    }

    public static final String createXPATHWarning(String str, Object[] objArr) {
        return createXPATHMsg(XPATHBundle, str, objArr);
    }

    public static final String createXPATHMsg(ListResourceBundle listResourceBundle, String str, Object[] objArr) {
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
