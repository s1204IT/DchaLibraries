package com.android.server.firewall;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Process;
import android.os.RemoteException;
import android.util.Slog;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class SenderFilter {
    private static final String ATTR_TYPE = "type";
    public static final FilterFactory FACTORY = new FilterFactory("sender") {
        @Override
        public Filter newFilter(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            String attributeValue = xmlPullParser.getAttributeValue(null, "type");
            if (attributeValue == null) {
                throw new XmlPullParserException("type attribute must be specified for <sender>", xmlPullParser, null);
            }
            if (attributeValue.equals(SenderFilter.VAL_SYSTEM)) {
                return SenderFilter.SYSTEM;
            }
            if (attributeValue.equals(SenderFilter.VAL_SIGNATURE)) {
                return SenderFilter.SIGNATURE;
            }
            if (attributeValue.equals(SenderFilter.VAL_SYSTEM_OR_SIGNATURE)) {
                return SenderFilter.SYSTEM_OR_SIGNATURE;
            }
            if (attributeValue.equals(SenderFilter.VAL_USER_ID)) {
                return SenderFilter.USER_ID;
            }
            throw new XmlPullParserException("Invalid type attribute for <sender>: " + attributeValue, xmlPullParser, null);
        }
    };
    private static final Filter SIGNATURE = new Filter() {
        @Override
        public boolean matches(IntentFirewall intentFirewall, ComponentName componentName, Intent intent, int i, int i2, String str, int i3) {
            return intentFirewall.signaturesMatch(i, i3);
        }
    };
    private static final Filter SYSTEM = new Filter() {
        @Override
        public boolean matches(IntentFirewall intentFirewall, ComponentName componentName, Intent intent, int i, int i2, String str, int i3) {
            return SenderFilter.isPrivilegedApp(i, i2);
        }
    };
    private static final Filter SYSTEM_OR_SIGNATURE = new Filter() {
        @Override
        public boolean matches(IntentFirewall intentFirewall, ComponentName componentName, Intent intent, int i, int i2, String str, int i3) {
            return SenderFilter.isPrivilegedApp(i, i2) || intentFirewall.signaturesMatch(i, i3);
        }
    };
    private static final Filter USER_ID = new Filter() {
        @Override
        public boolean matches(IntentFirewall intentFirewall, ComponentName componentName, Intent intent, int i, int i2, String str, int i3) {
            return intentFirewall.checkComponentPermission(null, i2, i, i3, false);
        }
    };
    private static final String VAL_SIGNATURE = "signature";
    private static final String VAL_SYSTEM = "system";
    private static final String VAL_SYSTEM_OR_SIGNATURE = "system|signature";
    private static final String VAL_USER_ID = "userId";

    SenderFilter() {
    }

    static boolean isPrivilegedApp(int i, int i2) {
        if (i == 1000 || i == 0 || i2 == Process.myPid() || i2 == 0) {
            return true;
        }
        try {
            if ((AppGlobals.getPackageManager().getPrivateFlagsForUid(i) & 8) != 0) {
                return true;
            }
            return false;
        } catch (RemoteException e) {
            Slog.e("IntentFirewall", "Remote exception while retrieving uid flags", e);
            return false;
        }
    }
}
