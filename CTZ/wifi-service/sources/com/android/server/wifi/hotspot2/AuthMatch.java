package com.android.server.wifi.hotspot2;

import com.android.server.wifi.util.XmlUtil;

public abstract class AuthMatch {
    public static final int EXACT = 7;
    public static final int INDETERMINATE = 0;
    public static final int METHOD = 2;
    public static final int METHOD_PARAM = 3;
    public static final int NONE = -1;
    public static final int PARAM = 1;
    public static final int REALM = 4;

    public static String toString(int i) {
        if (i < 0) {
            return "None";
        }
        if (i == 0) {
            return "Indeterminate";
        }
        StringBuilder sb = new StringBuilder();
        if ((i & 4) != 0) {
            sb.append(XmlUtil.WifiEnterpriseConfigXmlUtil.XML_TAG_REALM);
        }
        if ((i & 2) != 0) {
            sb.append("Method");
        }
        if ((i & 1) != 0) {
            sb.append("Param");
        }
        return sb.toString();
    }
}
