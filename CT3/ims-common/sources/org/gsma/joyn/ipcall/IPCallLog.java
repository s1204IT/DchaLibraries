package org.gsma.joyn.ipcall;

import android.net.Uri;

public class IPCallLog {
    public static final String CALL_ID = "call_id";
    public static final String CONTACT_NUMBER = "contact_number";
    public static final Uri CONTENT_URI = Uri.parse("content://org.gsma.joyn.provider.ipcall/ipcall");
    public static final String DIRECTION = "direction";
    public static final String DURATION = "duration";
    public static final String ID = "_id";
    public static final String STATE = "state";
    public static final String TIMESTAMP = "timestamp";
}
