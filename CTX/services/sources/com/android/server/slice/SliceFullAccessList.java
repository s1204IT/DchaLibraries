package com.android.server.slice;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.SparseArray;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class SliceFullAccessList {
    static final int DB_VERSION = 1;
    private static final String TAG = "SliceFullAccessList";
    private static final String TAG_LIST = "slice-access-list";
    private static final String TAG_PKG = "pkg";
    private static final String TAG_USER = "user";
    private final Context mContext;
    private final String ATT_USER_ID = TAG_USER;
    private final String ATT_VERSION = "version";
    private final SparseArray<ArraySet<String>> mFullAccessPkgs = new SparseArray<>();

    public SliceFullAccessList(Context context) {
        this.mContext = context;
    }

    public boolean hasFullAccess(String str, int i) {
        ArraySet<String> arraySet = this.mFullAccessPkgs.get(i, null);
        return arraySet != null && arraySet.contains(str);
    }

    public void grantFullAccess(String str, int i) {
        ArraySet<String> arraySet = this.mFullAccessPkgs.get(i, null);
        if (arraySet == null) {
            arraySet = new ArraySet<>();
            this.mFullAccessPkgs.put(i, arraySet);
        }
        arraySet.add(str);
    }

    public void removeGrant(String str, int i) {
        ArraySet<String> arraySet = this.mFullAccessPkgs.get(i, null);
        if (arraySet == null) {
            arraySet = new ArraySet<>();
            this.mFullAccessPkgs.put(i, arraySet);
        }
        arraySet.remove(str);
    }

    public void writeXml(XmlSerializer xmlSerializer, int i) throws IOException {
        xmlSerializer.startTag(null, TAG_LIST);
        xmlSerializer.attribute(null, "version", String.valueOf(1));
        int size = this.mFullAccessPkgs.size();
        for (int i2 = 0; i2 < size; i2++) {
            int iKeyAt = this.mFullAccessPkgs.keyAt(i2);
            ArraySet<String> arraySetValueAt = this.mFullAccessPkgs.valueAt(i2);
            if (i == -1 || i == iKeyAt) {
                xmlSerializer.startTag(null, TAG_USER);
                xmlSerializer.attribute(null, TAG_USER, Integer.toString(iKeyAt));
                if (arraySetValueAt != null) {
                    int size2 = arraySetValueAt.size();
                    for (int i3 = 0; i3 < size2; i3++) {
                        xmlSerializer.startTag(null, TAG_PKG);
                        xmlSerializer.text(arraySetValueAt.valueAt(i3));
                        xmlSerializer.endTag(null, TAG_PKG);
                    }
                }
                xmlSerializer.endTag(null, TAG_USER);
            }
        }
        xmlSerializer.endTag(null, TAG_LIST);
    }

    public void readXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int intAttribute = XmlUtils.readIntAttribute(xmlPullParser, "version", 0);
        Iterator it = UserManager.get(this.mContext).getUsers(true).iterator();
        while (it.hasNext()) {
            upgradeXml(intAttribute, ((UserInfo) it.next()).getUserHandle().getIdentifier());
        }
        this.mFullAccessPkgs.clear();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                String name = xmlPullParser.getName();
                if (next != 3 || !TAG_LIST.equals(name)) {
                    if (next == 2 && TAG_USER.equals(name)) {
                        int intAttribute2 = XmlUtils.readIntAttribute(xmlPullParser, TAG_USER, 0);
                        ArraySet<String> arraySet = new ArraySet<>();
                        while (true) {
                            int next2 = xmlPullParser.next();
                            if (next2 == 1) {
                                break;
                            }
                            String name2 = xmlPullParser.getName();
                            if (next2 == 3 && TAG_USER.equals(name2)) {
                                break;
                            } else if (next2 == 2 && TAG_PKG.equals(name2)) {
                                arraySet.add(xmlPullParser.nextText());
                            }
                        }
                        this.mFullAccessPkgs.put(intAttribute2, arraySet);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    protected void upgradeXml(int i, int i2) {
    }
}
