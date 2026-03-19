package org.apache.xalan.res;

import java.util.ListResourceBundle;
import org.apache.xpath.res.XPATHMessages;

public class XSLMessages extends XPATHMessages {
    private static ListResourceBundle XSLTBundle = new XSLTErrorResources();

    public static final String createMessage(String str, Object[] objArr) {
        return createMsg(XSLTBundle, str, objArr);
    }

    public static final String createWarning(String str, Object[] objArr) {
        return createMsg(XSLTBundle, str, objArr);
    }
}
