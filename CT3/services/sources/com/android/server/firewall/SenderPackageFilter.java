package com.android.server.firewall;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.os.UserHandle;
import com.android.server.pm.PackageManagerService;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SenderPackageFilter implements Filter {
    private static final String ATTR_NAME = "name";
    public static final FilterFactory FACTORY = new FilterFactory("sender-package") {
        @Override
        public Filter newFilter(XmlPullParser parser) throws XmlPullParserException, IOException {
            String packageName = parser.getAttributeValue(null, SenderPackageFilter.ATTR_NAME);
            if (packageName == null) {
                throw new XmlPullParserException("A package name must be specified.", parser, null);
            }
            return new SenderPackageFilter(packageName);
        }
    };
    public final String mPackageName;

    public SenderPackageFilter(String packageName) {
        this.mPackageName = packageName;
    }

    @Override
    public boolean matches(IntentFirewall ifw, ComponentName resolvedComponent, Intent intent, int callerUid, int callerPid, String resolvedType, int receivingUid) {
        IPackageManager pm = AppGlobals.getPackageManager();
        int packageUid = -1;
        try {
            packageUid = pm.getPackageUid(this.mPackageName, PackageManagerService.DumpState.DUMP_PREFERRED_XML, 0);
        } catch (RemoteException e) {
        }
        if (packageUid == -1) {
            return false;
        }
        return UserHandle.isSameApp(packageUid, callerUid);
    }
}
