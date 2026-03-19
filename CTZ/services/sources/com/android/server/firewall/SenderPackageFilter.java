package com.android.server.firewall;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.os.RemoteException;
import android.os.UserHandle;
import com.android.server.pm.DumpState;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SenderPackageFilter implements Filter {
    private static final String ATTR_NAME = "name";
    public static final FilterFactory FACTORY = new FilterFactory("sender-package") {
        @Override
        public Filter newFilter(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            String attributeValue = xmlPullParser.getAttributeValue(null, "name");
            if (attributeValue == null) {
                throw new XmlPullParserException("A package name must be specified.", xmlPullParser, null);
            }
            return new SenderPackageFilter(attributeValue);
        }
    };
    public final String mPackageName;

    public SenderPackageFilter(String str) {
        this.mPackageName = str;
    }

    @Override
    public boolean matches(IntentFirewall intentFirewall, ComponentName componentName, Intent intent, int i, int i2, String str, int i3) {
        int packageUid;
        try {
            packageUid = AppGlobals.getPackageManager().getPackageUid(this.mPackageName, DumpState.DUMP_CHANGES, 0);
        } catch (RemoteException e) {
            packageUid = -1;
        }
        if (packageUid == -1) {
            return false;
        }
        return UserHandle.isSameApp(packageUid, i);
    }
}
