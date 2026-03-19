package com.android.server.am;

import android.app.AppGlobals;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.Settings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class CompatModePackages {
    public static final int COMPAT_FLAG_DONT_ASK = 1;
    public static final int COMPAT_FLAG_ENABLED = 2;
    private static final int MSG_WRITE = 300;
    private static final String TAG = "ActivityManager";
    private static final String TAG_CONFIGURATION = TAG + ActivityManagerDebugConfig.POSTFIX_CONFIGURATION;
    private final AtomicFile mFile;
    private final CompatHandler mHandler;
    private final HashMap<String, Integer> mPackages = new HashMap<>();
    private final ActivityManagerService mService;

    private final class CompatHandler extends Handler {
        public CompatHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 300) {
                CompatModePackages.this.saveCompatModes();
            }
        }
    }

    public CompatModePackages(ActivityManagerService activityManagerService, File file, Handler handler) throws Throwable {
        FileInputStream fileInputStreamOpenRead;
        XmlPullParserException e;
        IOException e2;
        String attributeValue;
        int i;
        this.mService = activityManagerService;
        this.mFile = new AtomicFile(new File(file, "packages-compat.xml"), "compat-mode");
        ?? looper = handler.getLooper();
        this.mHandler = new CompatHandler(looper);
        try {
        } catch (Throwable th) {
            th = th;
        }
        try {
            try {
                fileInputStreamOpenRead = this.mFile.openRead();
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                    int eventType = xmlPullParserNewPullParser.getEventType();
                    while (eventType != 2 && eventType != 1) {
                        eventType = xmlPullParserNewPullParser.next();
                    }
                    if (eventType == 1) {
                        if (fileInputStreamOpenRead != null) {
                            try {
                                fileInputStreamOpenRead.close();
                                return;
                            } catch (IOException e3) {
                                return;
                            }
                        }
                        return;
                    }
                    if ("compat-packages".equals(xmlPullParserNewPullParser.getName())) {
                        int next = xmlPullParserNewPullParser.next();
                        do {
                            if (next == 2) {
                                String name = xmlPullParserNewPullParser.getName();
                                if (xmlPullParserNewPullParser.getDepth() == 2 && "pkg".equals(name) && (attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, Settings.ATTR_NAME)) != null) {
                                    String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, "mode");
                                    if (attributeValue2 != null) {
                                        try {
                                            i = Integer.parseInt(attributeValue2);
                                        } catch (NumberFormatException e4) {
                                            i = 0;
                                        }
                                        this.mPackages.put(attributeValue, Integer.valueOf(i));
                                    } else {
                                        i = 0;
                                        this.mPackages.put(attributeValue, Integer.valueOf(i));
                                    }
                                }
                            }
                            next = xmlPullParserNewPullParser.next();
                        } while (next != 1);
                    }
                    if (fileInputStreamOpenRead == null) {
                    } else {
                        fileInputStreamOpenRead.close();
                    }
                } catch (IOException e5) {
                    e2 = e5;
                    if (fileInputStreamOpenRead != null) {
                        Slog.w(TAG, "Error reading compat-packages", e2);
                    }
                    if (fileInputStreamOpenRead == null) {
                    } else {
                        fileInputStreamOpenRead.close();
                    }
                } catch (XmlPullParserException e6) {
                    e = e6;
                    Slog.w(TAG, "Error reading compat-packages", e);
                    if (fileInputStreamOpenRead == null) {
                    } else {
                        fileInputStreamOpenRead.close();
                    }
                }
            } catch (IOException e7) {
            }
        } catch (IOException e8) {
            fileInputStreamOpenRead = null;
            e2 = e8;
        } catch (XmlPullParserException e9) {
            fileInputStreamOpenRead = null;
            e = e9;
        } catch (Throwable th2) {
            looper = 0;
            th = th2;
            if (looper != 0) {
                try {
                    looper.close();
                } catch (IOException e10) {
                }
            }
            throw th;
        }
    }

    public HashMap<String, Integer> getPackages() {
        return this.mPackages;
    }

    private int getPackageFlags(String str) {
        Integer num = this.mPackages.get(str);
        if (num != null) {
            return num.intValue();
        }
        return 0;
    }

    public void handlePackageDataClearedLocked(String str) {
        removePackage(str);
    }

    public void handlePackageUninstalledLocked(String str) {
        removePackage(str);
    }

    private void removePackage(String str) {
        if (this.mPackages.containsKey(str)) {
            this.mPackages.remove(str);
            scheduleWrite();
        }
    }

    public void handlePackageAddedLocked(String str, boolean z) {
        ApplicationInfo applicationInfo;
        boolean z2 = false;
        try {
            applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(str, 0, 0);
        } catch (RemoteException e) {
            applicationInfo = null;
        }
        if (applicationInfo == null) {
            return;
        }
        CompatibilityInfo compatibilityInfoCompatibilityInfoForPackageLocked = compatibilityInfoForPackageLocked(applicationInfo);
        if (!compatibilityInfoCompatibilityInfoForPackageLocked.alwaysSupportsScreen() && !compatibilityInfoCompatibilityInfoForPackageLocked.neverSupportsScreen()) {
            z2 = true;
        }
        if (z && !z2 && this.mPackages.containsKey(str)) {
            this.mPackages.remove(str);
            scheduleWrite();
        }
    }

    private void scheduleWrite() {
        this.mHandler.removeMessages(300);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(300), JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    public CompatibilityInfo compatibilityInfoForPackageLocked(ApplicationInfo applicationInfo) {
        Configuration globalConfiguration = this.mService.getGlobalConfiguration();
        return new CompatibilityInfo(applicationInfo, globalConfiguration.screenLayout, globalConfiguration.smallestScreenWidthDp, (getPackageFlags(applicationInfo.packageName) & 2) != 0);
    }

    public int computeCompatModeLocked(ApplicationInfo applicationInfo) {
        boolean z = (getPackageFlags(applicationInfo.packageName) & 2) != 0;
        Configuration globalConfiguration = this.mService.getGlobalConfiguration();
        CompatibilityInfo compatibilityInfo = new CompatibilityInfo(applicationInfo, globalConfiguration.screenLayout, globalConfiguration.smallestScreenWidthDp, z);
        if (compatibilityInfo.alwaysSupportsScreen()) {
            return -2;
        }
        if (compatibilityInfo.neverSupportsScreen()) {
            return -1;
        }
        return z ? 1 : 0;
    }

    public boolean getFrontActivityAskCompatModeLocked() {
        ActivityRecord activityRecord = this.mService.getFocusedStack().topRunningActivityLocked();
        if (activityRecord == null) {
            return false;
        }
        return getPackageAskCompatModeLocked(activityRecord.packageName);
    }

    public boolean getPackageAskCompatModeLocked(String str) {
        return (getPackageFlags(str) & 1) == 0;
    }

    public void setFrontActivityAskCompatModeLocked(boolean z) {
        ActivityRecord activityRecord = this.mService.getFocusedStack().topRunningActivityLocked();
        if (activityRecord != null) {
            setPackageAskCompatModeLocked(activityRecord.packageName, z);
        }
    }

    public void setPackageAskCompatModeLocked(String str, boolean z) {
        setPackageFlagLocked(str, 1, z);
    }

    private void setPackageFlagLocked(String str, int i, boolean z) {
        int packageFlags = getPackageFlags(str);
        int i2 = z ? (~i) & packageFlags : i | packageFlags;
        if (packageFlags != i2) {
            if (i2 != 0) {
                this.mPackages.put(str, Integer.valueOf(i2));
            } else {
                this.mPackages.remove(str);
            }
            scheduleWrite();
        }
    }

    public int getFrontActivityScreenCompatModeLocked() {
        ActivityRecord activityRecord = this.mService.getFocusedStack().topRunningActivityLocked();
        if (activityRecord == null) {
            return -3;
        }
        return computeCompatModeLocked(activityRecord.info.applicationInfo);
    }

    public void setFrontActivityScreenCompatModeLocked(int i) {
        ActivityRecord activityRecord = this.mService.getFocusedStack().topRunningActivityLocked();
        if (activityRecord == null) {
            Slog.w(TAG, "setFrontActivityScreenCompatMode failed: no top activity");
        } else {
            setPackageScreenCompatModeLocked(activityRecord.info.applicationInfo, i);
        }
    }

    public int getPackageScreenCompatModeLocked(String str) {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(str, 0, 0);
        } catch (RemoteException e) {
            applicationInfo = null;
        }
        if (applicationInfo == null) {
            return -3;
        }
        return computeCompatModeLocked(applicationInfo);
    }

    public void setPackageScreenCompatModeLocked(String str, int i) {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(str, 0, 0);
        } catch (RemoteException e) {
            applicationInfo = null;
        }
        if (applicationInfo == null) {
            Slog.w(TAG, "setPackageScreenCompatMode failed: unknown package " + str);
            return;
        }
        setPackageScreenCompatModeLocked(applicationInfo, i);
    }

    private void setPackageScreenCompatModeLocked(ApplicationInfo applicationInfo, int i) {
        boolean z;
        int i2;
        String str = applicationInfo.packageName;
        int packageFlags = getPackageFlags(str);
        switch (i) {
            case 0:
                z = false;
                break;
            case 1:
                z = true;
                break;
            case 2:
                z = (packageFlags & 2) == 0;
                break;
            default:
                Slog.w(TAG, "Unknown screen compat mode req #" + i + "; ignoring");
                return;
        }
        if (z) {
            i2 = packageFlags | 2;
        } else {
            i2 = packageFlags & (-3);
        }
        CompatibilityInfo compatibilityInfoCompatibilityInfoForPackageLocked = compatibilityInfoForPackageLocked(applicationInfo);
        if (compatibilityInfoCompatibilityInfoForPackageLocked.alwaysSupportsScreen()) {
            Slog.w(TAG, "Ignoring compat mode change of " + str + "; compatibility never needed");
            i2 = 0;
        }
        if (compatibilityInfoCompatibilityInfoForPackageLocked.neverSupportsScreen()) {
            Slog.w(TAG, "Ignoring compat mode change of " + str + "; compatibility always needed");
            i2 = 0;
        }
        if (i2 != packageFlags) {
            if (i2 != 0) {
                this.mPackages.put(str, Integer.valueOf(i2));
            } else {
                this.mPackages.remove(str);
            }
            CompatibilityInfo compatibilityInfoCompatibilityInfoForPackageLocked2 = compatibilityInfoForPackageLocked(applicationInfo);
            scheduleWrite();
            ActivityStack focusedStack = this.mService.getFocusedStack();
            ActivityRecord activityRecordRestartPackage = focusedStack.restartPackage(str);
            for (int size = this.mService.mLruProcesses.size() - 1; size >= 0; size--) {
                ProcessRecord processRecord = this.mService.mLruProcesses.get(size);
                if (processRecord.pkgList.containsKey(str)) {
                    try {
                        if (processRecord.thread != null) {
                            if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                                Slog.v(TAG_CONFIGURATION, "Sending to proc " + processRecord.processName + " new compat " + compatibilityInfoCompatibilityInfoForPackageLocked2);
                            }
                            processRecord.thread.updatePackageCompatibilityInfo(str, compatibilityInfoCompatibilityInfoForPackageLocked2);
                        }
                    } catch (Exception e) {
                    }
                }
            }
            if (activityRecordRestartPackage != null) {
                activityRecordRestartPackage.ensureActivityConfiguration(0, false);
                focusedStack.ensureActivitiesVisibleLocked(activityRecordRestartPackage, 0, false);
            }
        }
    }

    void saveCompatModes() {
        HashMap map;
        FileOutputStream fileOutputStreamStartWrite;
        IOException e;
        ApplicationInfo applicationInfo;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                map = new HashMap(this.mPackages);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        try {
            fileOutputStreamStartWrite = this.mFile.startWrite();
        } catch (IOException e2) {
            fileOutputStreamStartWrite = null;
            e = e2;
        }
        try {
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "compat-packages");
            IPackageManager packageManager = AppGlobals.getPackageManager();
            Configuration globalConfiguration = this.mService.getGlobalConfiguration();
            int i = globalConfiguration.screenLayout;
            int i2 = globalConfiguration.smallestScreenWidthDp;
            for (Map.Entry entry : map.entrySet()) {
                String str = (String) entry.getKey();
                int iIntValue = ((Integer) entry.getValue()).intValue();
                if (iIntValue != 0) {
                    try {
                        applicationInfo = packageManager.getApplicationInfo(str, 0, 0);
                    } catch (RemoteException e3) {
                        applicationInfo = null;
                    }
                    if (applicationInfo != null) {
                        CompatibilityInfo compatibilityInfo = new CompatibilityInfo(applicationInfo, i, i2, false);
                        if (!compatibilityInfo.alwaysSupportsScreen() && !compatibilityInfo.neverSupportsScreen()) {
                            fastXmlSerializer.startTag(null, "pkg");
                            fastXmlSerializer.attribute(null, Settings.ATTR_NAME, str);
                            fastXmlSerializer.attribute(null, "mode", Integer.toString(iIntValue));
                            fastXmlSerializer.endTag(null, "pkg");
                        }
                    }
                }
            }
            fastXmlSerializer.endTag(null, "compat-packages");
            fastXmlSerializer.endDocument();
            this.mFile.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e4) {
            e = e4;
            Slog.w(TAG, "Error writing compat packages", e);
            if (fileOutputStreamStartWrite != null) {
                this.mFile.failWrite(fileOutputStreamStartWrite);
            }
        }
    }
}
