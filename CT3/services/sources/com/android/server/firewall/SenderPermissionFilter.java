package com.android.server.firewall;

import android.content.ComponentName;
import android.content.Intent;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class SenderPermissionFilter implements Filter {
    private static final String ATTR_NAME = "name";
    public static final FilterFactory FACTORY = new FilterFactory("sender-permission") {
        @Override
        public Filter newFilter(XmlPullParser parser) throws XmlPullParserException, IOException {
            SenderPermissionFilter senderPermissionFilter = null;
            String permission = parser.getAttributeValue(null, SenderPermissionFilter.ATTR_NAME);
            if (permission == null) {
                throw new XmlPullParserException("Permission name must be specified.", parser, null);
            }
            return new SenderPermissionFilter(permission, senderPermissionFilter);
        }
    };
    private final String mPermission;

    SenderPermissionFilter(String permission, SenderPermissionFilter senderPermissionFilter) {
        this(permission);
    }

    private SenderPermissionFilter(String permission) {
        this.mPermission = permission;
    }

    @Override
    public boolean matches(IntentFirewall ifw, ComponentName resolvedComponent, Intent intent, int callerUid, int callerPid, String resolvedType, int receivingUid) {
        return ifw.checkComponentPermission(this.mPermission, callerPid, callerUid, receivingUid, true);
    }
}
