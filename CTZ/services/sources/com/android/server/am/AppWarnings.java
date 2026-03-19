package com.android.server.am;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AtomicFile;
import android.util.DisplayMetrics;
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
import java.util.HashSet;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class AppWarnings {
    private static final String CONFIG_FILE_NAME = "packages-warnings.xml";
    public static final int FLAG_HIDE_COMPILE_SDK = 2;
    public static final int FLAG_HIDE_DEPRECATED_SDK = 4;
    public static final int FLAG_HIDE_DISPLAY_SIZE = 1;
    private static final String TAG = "AppWarnings";
    private final ActivityManagerService mAms;
    private final ConfigHandler mAmsHandler;
    private final AtomicFile mConfigFile;
    private DeprecatedTargetSdkVersionDialog mDeprecatedTargetSdkVersionDialog;
    private final Context mUiContext;
    private final UiHandler mUiHandler;
    private UnsupportedCompileSdkDialog mUnsupportedCompileSdkDialog;
    private UnsupportedDisplaySizeDialog mUnsupportedDisplaySizeDialog;
    private final HashMap<String, Integer> mPackageFlags = new HashMap<>();
    private HashSet<ComponentName> mAlwaysShowUnsupportedCompileSdkWarningActivities = new HashSet<>();

    void alwaysShowUnsupportedCompileSdkWarning(ComponentName componentName) {
        this.mAlwaysShowUnsupportedCompileSdkWarningActivities.add(componentName);
    }

    public AppWarnings(ActivityManagerService activityManagerService, Context context, Handler handler, Handler handler2, File file) throws Throwable {
        this.mAms = activityManagerService;
        this.mUiContext = context;
        this.mAmsHandler = new ConfigHandler(handler.getLooper());
        this.mUiHandler = new UiHandler(handler2.getLooper());
        this.mConfigFile = new AtomicFile(new File(file, CONFIG_FILE_NAME), "warnings-config");
        readConfigFromFileAmsThread();
    }

    public void showUnsupportedDisplaySizeDialogIfNeeded(ActivityRecord activityRecord) {
        Configuration globalConfiguration = this.mAms.getGlobalConfiguration();
        if (globalConfiguration.densityDpi != DisplayMetrics.DENSITY_DEVICE_STABLE && activityRecord.appInfo.requiresSmallestWidthDp > globalConfiguration.smallestScreenWidthDp) {
            this.mUiHandler.showUnsupportedDisplaySizeDialog(activityRecord);
        }
    }

    public void showUnsupportedCompileSdkDialogIfNeeded(ActivityRecord activityRecord) {
        if (activityRecord.appInfo.compileSdkVersion == 0 || activityRecord.appInfo.compileSdkVersionCodename == null || !this.mAlwaysShowUnsupportedCompileSdkWarningActivities.contains(activityRecord.realActivity)) {
            return;
        }
        int i = activityRecord.appInfo.compileSdkVersion;
        int i2 = Build.VERSION.SDK_INT;
        boolean z = !"REL".equals(activityRecord.appInfo.compileSdkVersionCodename);
        boolean z2 = !"REL".equals(Build.VERSION.CODENAME);
        if ((z && i < i2) || ((z2 && i2 < i) || (z && z2 && i2 == i && !Build.VERSION.CODENAME.equals(activityRecord.appInfo.compileSdkVersionCodename)))) {
            this.mUiHandler.showUnsupportedCompileSdkDialog(activityRecord);
        }
    }

    public void showDeprecatedTargetDialogIfNeeded(ActivityRecord activityRecord) {
        if (activityRecord.appInfo.targetSdkVersion < Build.VERSION.MIN_SUPPORTED_TARGET_SDK_INT) {
            this.mUiHandler.showDeprecatedTargetDialog(activityRecord);
        }
    }

    public void onStartActivity(ActivityRecord activityRecord) {
        showUnsupportedCompileSdkDialogIfNeeded(activityRecord);
        showUnsupportedDisplaySizeDialogIfNeeded(activityRecord);
        showDeprecatedTargetDialogIfNeeded(activityRecord);
    }

    public void onResumeActivity(ActivityRecord activityRecord) {
        showUnsupportedDisplaySizeDialogIfNeeded(activityRecord);
    }

    public void onPackageDataCleared(String str) {
        removePackageAndHideDialogs(str);
    }

    public void onPackageUninstalled(String str) {
        removePackageAndHideDialogs(str);
    }

    public void onDensityChanged() {
        this.mUiHandler.hideUnsupportedDisplaySizeDialog();
    }

    private void removePackageAndHideDialogs(String str) {
        this.mUiHandler.hideDialogsForPackage(str);
        synchronized (this.mPackageFlags) {
            this.mPackageFlags.remove(str);
            this.mAmsHandler.scheduleWrite();
        }
    }

    private void hideUnsupportedDisplaySizeDialogUiThread() {
        if (this.mUnsupportedDisplaySizeDialog != null) {
            this.mUnsupportedDisplaySizeDialog.dismiss();
            this.mUnsupportedDisplaySizeDialog = null;
        }
    }

    private void showUnsupportedDisplaySizeDialogUiThread(ActivityRecord activityRecord) {
        if (this.mUnsupportedDisplaySizeDialog != null) {
            this.mUnsupportedDisplaySizeDialog.dismiss();
            this.mUnsupportedDisplaySizeDialog = null;
        }
        if (activityRecord != null && !hasPackageFlag(activityRecord.packageName, 1)) {
            this.mUnsupportedDisplaySizeDialog = new UnsupportedDisplaySizeDialog(this, this.mUiContext, activityRecord.info.applicationInfo);
            this.mUnsupportedDisplaySizeDialog.show();
        }
    }

    private void showUnsupportedCompileSdkDialogUiThread(ActivityRecord activityRecord) {
        if (this.mUnsupportedCompileSdkDialog != null) {
            this.mUnsupportedCompileSdkDialog.dismiss();
            this.mUnsupportedCompileSdkDialog = null;
        }
        if (activityRecord != null && !hasPackageFlag(activityRecord.packageName, 2)) {
            this.mUnsupportedCompileSdkDialog = new UnsupportedCompileSdkDialog(this, this.mUiContext, activityRecord.info.applicationInfo);
            this.mUnsupportedCompileSdkDialog.show();
        }
    }

    private void showDeprecatedTargetSdkDialogUiThread(ActivityRecord activityRecord) {
        if (this.mDeprecatedTargetSdkVersionDialog != null) {
            this.mDeprecatedTargetSdkVersionDialog.dismiss();
            this.mDeprecatedTargetSdkVersionDialog = null;
        }
        if (activityRecord != null && !hasPackageFlag(activityRecord.packageName, 4)) {
            this.mDeprecatedTargetSdkVersionDialog = new DeprecatedTargetSdkVersionDialog(this, this.mUiContext, activityRecord.info.applicationInfo);
            this.mDeprecatedTargetSdkVersionDialog.show();
        }
    }

    private void hideDialogsForPackageUiThread(String str) {
        if (this.mUnsupportedDisplaySizeDialog != null && (str == null || str.equals(this.mUnsupportedDisplaySizeDialog.getPackageName()))) {
            this.mUnsupportedDisplaySizeDialog.dismiss();
            this.mUnsupportedDisplaySizeDialog = null;
        }
        if (this.mUnsupportedCompileSdkDialog != null && (str == null || str.equals(this.mUnsupportedCompileSdkDialog.getPackageName()))) {
            this.mUnsupportedCompileSdkDialog.dismiss();
            this.mUnsupportedCompileSdkDialog = null;
        }
        if (this.mDeprecatedTargetSdkVersionDialog != null) {
            if (str == null || str.equals(this.mDeprecatedTargetSdkVersionDialog.getPackageName())) {
                this.mDeprecatedTargetSdkVersionDialog.dismiss();
                this.mDeprecatedTargetSdkVersionDialog = null;
            }
        }
    }

    boolean hasPackageFlag(String str, int i) {
        return (getPackageFlags(str) & i) == i;
    }

    void setPackageFlag(String str, int i, boolean z) {
        synchronized (this.mPackageFlags) {
            int packageFlags = getPackageFlags(str);
            int i2 = z ? i | packageFlags : (~i) & packageFlags;
            if (packageFlags != i2) {
                if (i2 != 0) {
                    this.mPackageFlags.put(str, Integer.valueOf(i2));
                } else {
                    this.mPackageFlags.remove(str);
                }
                this.mAmsHandler.scheduleWrite();
            }
        }
    }

    private int getPackageFlags(String str) {
        int iIntValue;
        synchronized (this.mPackageFlags) {
            iIntValue = this.mPackageFlags.getOrDefault(str, 0).intValue();
        }
        return iIntValue;
    }

    private final class UiHandler extends Handler {
        private static final int MSG_HIDE_DIALOGS_FOR_PACKAGE = 4;
        private static final int MSG_HIDE_UNSUPPORTED_DISPLAY_SIZE_DIALOG = 2;
        private static final int MSG_SHOW_DEPRECATED_TARGET_SDK_DIALOG = 5;
        private static final int MSG_SHOW_UNSUPPORTED_COMPILE_SDK_DIALOG = 3;
        private static final int MSG_SHOW_UNSUPPORTED_DISPLAY_SIZE_DIALOG = 1;

        public UiHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    AppWarnings.this.showUnsupportedDisplaySizeDialogUiThread((ActivityRecord) message.obj);
                    break;
                case 2:
                    AppWarnings.this.hideUnsupportedDisplaySizeDialogUiThread();
                    break;
                case 3:
                    AppWarnings.this.showUnsupportedCompileSdkDialogUiThread((ActivityRecord) message.obj);
                    break;
                case 4:
                    AppWarnings.this.hideDialogsForPackageUiThread((String) message.obj);
                    break;
                case 5:
                    AppWarnings.this.showDeprecatedTargetSdkDialogUiThread((ActivityRecord) message.obj);
                    break;
            }
        }

        public void showUnsupportedDisplaySizeDialog(ActivityRecord activityRecord) {
            removeMessages(1);
            obtainMessage(1, activityRecord).sendToTarget();
        }

        public void hideUnsupportedDisplaySizeDialog() {
            removeMessages(2);
            sendEmptyMessage(2);
        }

        public void showUnsupportedCompileSdkDialog(ActivityRecord activityRecord) {
            removeMessages(3);
            obtainMessage(3, activityRecord).sendToTarget();
        }

        public void showDeprecatedTargetDialog(ActivityRecord activityRecord) {
            removeMessages(5);
            obtainMessage(5, activityRecord).sendToTarget();
        }

        public void hideDialogsForPackage(String str) {
            obtainMessage(4, str).sendToTarget();
        }
    }

    private final class ConfigHandler extends Handler {
        private static final int DELAY_MSG_WRITE = 10000;
        private static final int MSG_WRITE = 300;

        public ConfigHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 300) {
                AppWarnings.this.writeConfigToFileAmsThread();
            }
        }

        public void scheduleWrite() {
            removeMessages(300);
            sendEmptyMessageDelayed(300, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    private void writeConfigToFileAmsThread() {
        HashMap map;
        FileOutputStream fileOutputStreamStartWrite;
        IOException e;
        synchronized (this.mPackageFlags) {
            map = new HashMap(this.mPackageFlags);
        }
        try {
            fileOutputStreamStartWrite = this.mConfigFile.startWrite();
        } catch (IOException e2) {
            fileOutputStreamStartWrite = null;
            e = e2;
        }
        try {
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "packages");
            for (Map.Entry entry : map.entrySet()) {
                String str = (String) entry.getKey();
                int iIntValue = ((Integer) entry.getValue()).intValue();
                if (iIntValue != 0) {
                    fastXmlSerializer.startTag(null, Settings.ATTR_PACKAGE);
                    fastXmlSerializer.attribute(null, Settings.ATTR_NAME, str);
                    fastXmlSerializer.attribute(null, "flags", Integer.toString(iIntValue));
                    fastXmlSerializer.endTag(null, Settings.ATTR_PACKAGE);
                }
            }
            fastXmlSerializer.endTag(null, "packages");
            fastXmlSerializer.endDocument();
            this.mConfigFile.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e3) {
            e = e3;
            Slog.w(TAG, "Error writing package metadata", e);
            if (fileOutputStreamStartWrite != null) {
                this.mConfigFile.failWrite(fileOutputStreamStartWrite);
            }
        }
    }

    private void readConfigFromFileAmsThread() throws Throwable {
        Throwable th;
        FileInputStream fileInputStreamOpenRead;
        XmlPullParserException e;
        IOException e2;
        String attributeValue;
        int i;
        FileInputStream fileInputStream = null;
        try {
            try {
                try {
                    fileInputStreamOpenRead = this.mConfigFile.openRead();
                } catch (Throwable th2) {
                    th = th2;
                    if (0 != 0) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e3) {
                        }
                    }
                    throw th;
                }
            } catch (IOException e4) {
                fileInputStreamOpenRead = null;
                e2 = e4;
            } catch (XmlPullParserException e5) {
                fileInputStreamOpenRead = null;
                e = e5;
            } catch (Throwable th3) {
                th = th3;
                if (0 != 0) {
                }
                throw th;
            }
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
                        } catch (IOException e6) {
                            return;
                        }
                    }
                    return;
                }
                if ("packages".equals(xmlPullParserNewPullParser.getName())) {
                    int next = xmlPullParserNewPullParser.next();
                    do {
                        if (next == 2) {
                            String name = xmlPullParserNewPullParser.getName();
                            if (xmlPullParserNewPullParser.getDepth() == 2 && Settings.ATTR_PACKAGE.equals(name) && (attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, Settings.ATTR_NAME)) != null) {
                                String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, "flags");
                                if (attributeValue2 != null) {
                                    try {
                                        i = Integer.parseInt(attributeValue2);
                                    } catch (NumberFormatException e7) {
                                        i = 0;
                                    }
                                    this.mPackageFlags.put(attributeValue, Integer.valueOf(i));
                                } else {
                                    i = 0;
                                    this.mPackageFlags.put(attributeValue, Integer.valueOf(i));
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
            } catch (IOException e8) {
                e2 = e8;
                if (fileInputStreamOpenRead != null) {
                    Slog.w(TAG, "Error reading package metadata", e2);
                }
                if (fileInputStreamOpenRead == null) {
                } else {
                    fileInputStreamOpenRead.close();
                }
            } catch (XmlPullParserException e9) {
                e = e9;
                Slog.w(TAG, "Error reading package metadata", e);
                if (fileInputStreamOpenRead == null) {
                } else {
                    fileInputStreamOpenRead.close();
                }
            }
        } catch (IOException e10) {
        }
    }
}
