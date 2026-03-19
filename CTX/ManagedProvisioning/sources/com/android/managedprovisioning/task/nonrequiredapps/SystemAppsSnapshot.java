package com.android.managedprovisioning.task.nonrequiredapps;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.os.UserManager;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SystemAppsSnapshot {
    private final Context mContext;
    private final IPackageManager mIPackageManager;
    private final Utils mUtils;

    public SystemAppsSnapshot(Context context) {
        this(context, AppGlobals.getPackageManager(), new Utils());
    }

    @VisibleForTesting
    SystemAppsSnapshot(Context context, IPackageManager iPackageManager, Utils utils) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mIPackageManager = (IPackageManager) Preconditions.checkNotNull(iPackageManager);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
    }

    public boolean hasSnapshot(int i) {
        return getSystemAppsFile(this.mContext, i).exists();
    }

    public Set<String> getSnapshot(int i) {
        return readSystemApps(getSystemAppsFile(this.mContext, i));
    }

    public void takeNewSnapshot(int i) {
        File systemAppsFile = getSystemAppsFile(this.mContext, i);
        systemAppsFile.getParentFile().mkdirs();
        writeSystemApps(this.mUtils.getCurrentSystemApps(this.mIPackageManager, i), systemAppsFile);
    }

    private void writeSystemApps(Set<String> set, File file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fileOutputStream, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, "system-apps");
            for (String str : set) {
                fastXmlSerializer.startTag(null, "item");
                fastXmlSerializer.attribute(null, "value", str);
                fastXmlSerializer.endTag(null, "item");
            }
            fastXmlSerializer.endTag(null, "system-apps");
            fastXmlSerializer.endDocument();
            fileOutputStream.close();
        } catch (IOException e) {
            ProvisionLogger.loge("IOException trying to write the system apps", e);
        }
    }

    private Set<String> readSystemApps(File file) {
        HashSet hashSet = new HashSet();
        if (!file.exists()) {
            return hashSet;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(fileInputStream, null);
            xmlPullParserNewPullParser.next();
            int depth = xmlPullParserNewPullParser.getDepth();
            while (true) {
                int next = xmlPullParserNewPullParser.next();
                if (next == 1 || (next == 3 && xmlPullParserNewPullParser.getDepth() <= depth)) {
                    break;
                }
                if (next != 3 && next != 4) {
                    String name = xmlPullParserNewPullParser.getName();
                    if (name.equals("item")) {
                        hashSet.add(xmlPullParserNewPullParser.getAttributeValue(null, "value"));
                    } else {
                        ProvisionLogger.loge("Unknown tag: " + name);
                    }
                }
            }
            fileInputStream.close();
        } catch (IOException e) {
            ProvisionLogger.loge("IOException trying to read the system apps", e);
        } catch (XmlPullParserException e2) {
            ProvisionLogger.loge("XmlPullParserException trying to read the system apps", e2);
        }
        return hashSet;
    }

    public static File getSystemAppsFile(Context context, int i) {
        int userSerialNumber = ((UserManager) context.getSystemService("user")).getUserSerialNumber(i);
        if (userSerialNumber == -1) {
            throw new IllegalArgumentException("Invalid userId : " + i);
        }
        return new File(getFolder(context), userSerialNumber + ".xml");
    }

    public static File getFolder(Context context) {
        return new File(context.getFilesDir(), "system_apps_v2");
    }

    public static File getLegacyFolder(Context context) {
        return new File(context.getFilesDir(), "system_apps");
    }
}
