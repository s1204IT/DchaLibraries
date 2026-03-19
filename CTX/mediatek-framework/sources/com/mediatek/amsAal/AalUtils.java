package com.mediatek.amsAal;

import android.content.ComponentName;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.Xml;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class AalUtils {
    private static final int AAL_DEFAULT_LEVEL = 128;
    private static final int AAL_MAX_LEVEL = 256;
    private static final int AAL_MIN_LEVEL = 0;
    public static final int AAL_MODE_BALANCE = 1;
    public static final int AAL_MODE_LOWPOWER = 2;
    public static final int AAL_MODE_PERFORMANCE = 0;
    public static final int AAL_MODE_SIZE = 3;
    private static final int AAL_NULL_LEVEL = -1;
    private static final String TAG = "AalUtils";
    private static String sAalConfigXMLPath;
    private static boolean sEnabled;
    private static AalUtils sInstance;
    private int mAalMode = 1;
    private Map<AalIndex, Integer> mConfig = new HashMap();
    private AalConfig mCurrentConfig = null;
    private static boolean sDebug = false;
    private static boolean sIsAalSupported = SystemProperties.get("ro.vendor.mtk_aal_support").equals("1");

    private native void setSmartBacklightStrength(int i);

    static {
        boolean z = false;
        if (sIsAalSupported && SystemProperties.get("persist.vendor.sys.mtk_app_aal_support").equals("1")) {
            z = true;
        }
        sEnabled = z;
        sInstance = null;
        sAalConfigXMLPath = "/vendor/etc/ams_aal_config.xml";
    }

    AalUtils() {
        if (!sIsAalSupported) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not supported");
                return;
            }
            return;
        }
        try {
            parseXML();
        } catch (IOException e) {
            Slog.d(TAG, "IOException fail to parseXML, " + e);
        } catch (XmlPullParserException e2) {
            Slog.d(TAG, "XmlPullParserException fail to parseXML, " + e2);
        } catch (Exception e3) {
            Slog.d(TAG, "fail to parseXML, " + e3);
        }
    }

    public static boolean isSupported() {
        if (sDebug) {
            Slog.d(TAG, "isSupported = " + sIsAalSupported);
        }
        return sIsAalSupported;
    }

    public static AalUtils getInstance() {
        if (sInstance == null) {
            sInstance = new AalUtils();
        }
        return sInstance;
    }

    public void setAalMode(int i) {
        if (!sIsAalSupported) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not supported");
                return;
            }
            return;
        }
        setAalModeInternal(i);
    }

    public void setEnabled(boolean z) {
        if (!sIsAalSupported) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not supported");
                return;
            }
            return;
        }
        setEnabledInternal(z);
    }

    synchronized String setAalModeInternal(int i) {
        String str;
        if (!sEnabled) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not enabled");
            }
            return "AAL is not enabled";
        }
        if (i >= 0 && i < 3) {
            this.mAalMode = i;
            str = "setAalModeInternal " + this.mAalMode + "(" + modeToString(this.mAalMode) + ")";
        } else {
            str = "unknown mode " + i;
        }
        if (sDebug) {
            Slog.d(TAG, str);
        }
        return str;
    }

    public synchronized void setEnabledInternal(boolean z) {
        sEnabled = z;
        if (!sEnabled) {
            setDefaultSmartBacklightInternal("disabled");
            SystemProperties.set("persist.vendor.sys.mtk_app_aal_support", "0");
        } else {
            SystemProperties.set("persist.vendor.sys.mtk_app_aal_support", "1");
        }
        if (sDebug) {
            Slog.d(TAG, "setEnabledInternal(" + sEnabled + ")");
        }
    }

    public synchronized void setSmartBacklightInternal(ComponentName componentName) {
        setSmartBacklightInternal(componentName, this.mAalMode);
    }

    public synchronized void setSmartBacklightInternal(ComponentName componentName, int i) {
        if (!sEnabled) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not enabled");
            }
            return;
        }
        if (i < 0 || i >= 3) {
            if (sDebug) {
                Slog.d(TAG, "Unknown mode: " + i);
            }
            return;
        }
        if (this.mCurrentConfig == null) {
            if (sDebug) {
                Slog.d(TAG, "mCurrentConfig == null");
            }
            this.mCurrentConfig = new AalConfig(null, AAL_DEFAULT_LEVEL);
        }
        AalIndex aalIndex = new AalIndex(i, componentName.flattenToShortString());
        AalConfig aalConfig = getAalConfig(aalIndex);
        if (AAL_NULL_LEVEL == aalConfig.mLevel) {
            aalIndex = new AalIndex(i, componentName.getPackageName());
            aalConfig = getAalConfig(aalIndex);
            if (AAL_NULL_LEVEL == aalConfig.mLevel) {
                aalConfig.mLevel = AAL_DEFAULT_LEVEL;
            }
        }
        int iEnsureBacklightLevel = ensureBacklightLevel(aalConfig.mLevel);
        if (sDebug) {
            Slog.d(TAG, "setSmartBacklight current level: " + this.mCurrentConfig.mLevel + " for " + aalIndex);
        }
        if (this.mCurrentConfig.mLevel != iEnsureBacklightLevel) {
            Slog.d(TAG, "setSmartBacklightStrength(" + iEnsureBacklightLevel + ") for " + aalIndex);
            this.mCurrentConfig.mLevel = iEnsureBacklightLevel;
            this.mCurrentConfig.mName = aalIndex.getIndexName();
            setSmartBacklightStrength(iEnsureBacklightLevel);
        }
    }

    public synchronized void setDefaultSmartBacklightInternal(String str) {
        if (!sEnabled) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not enabled");
            }
            return;
        }
        if (this.mCurrentConfig != null && this.mCurrentConfig.mLevel != AAL_DEFAULT_LEVEL) {
            Slog.d(TAG, "setSmartBacklightStrength(128) " + str);
            this.mCurrentConfig.mLevel = AAL_DEFAULT_LEVEL;
            this.mCurrentConfig.mName = null;
            setSmartBacklightStrength(AAL_DEFAULT_LEVEL);
        }
    }

    public void onAfterActivityResumed(String str, String str2) {
        setSmartBacklightInternal(new ComponentName(str, str2));
    }

    public void onUpdateSleep(boolean z, boolean z2) {
        if (sDebug) {
            Slog.d(TAG, "onUpdateSleep before=" + z + " after=" + z2);
        }
        if (z != z2 && z2) {
            setDefaultSmartBacklightInternal("for sleep");
        }
    }

    private int ensureBacklightLevel(int i) {
        if (i < 0) {
            if (sDebug) {
                Slog.e(TAG, "Invalid AAL backlight level: " + i);
                return 0;
            }
            return 0;
        }
        if (i > 256) {
            if (sDebug) {
                Slog.e(TAG, "Invalid AAL backlight level: " + i);
            }
            return 256;
        }
        return i;
    }

    private AalConfig getAalConfig(AalIndex aalIndex) {
        int iIntValue;
        if (this.mConfig.containsKey(aalIndex)) {
            iIntValue = this.mConfig.get(aalIndex).intValue();
        } else {
            if (sDebug) {
                Slog.d(TAG, "No config for " + aalIndex);
            }
            iIntValue = AAL_NULL_LEVEL;
        }
        return new AalConfig(aalIndex.getIndexName(), iIntValue);
    }

    private String modeToString(int i) {
        switch (i) {
            case 0:
                return "AAL_MODE_PERFORMANCE";
            case 1:
                return "AAL_MODE_BALANCE";
            case 2:
                return "AAL_MODE_LOWPOWER";
            default:
                return "Unknown mode: " + i;
        }
    }

    private class AalConfig {
        public int mLevel;
        public String mName;

        public AalConfig(String str, int i) {
            this.mName = null;
            this.mLevel = AalUtils.AAL_NULL_LEVEL;
            this.mName = str;
            this.mLevel = i;
        }
    }

    private class AalIndex {
        private int mMode;
        private String mName;

        AalIndex(int i, String str) {
            set(i, str);
        }

        private void set(int i, String str) {
            this.mMode = i;
            this.mName = str;
        }

        public int getMode() {
            return this.mMode;
        }

        public String getIndexName() {
            return this.mName;
        }

        public String toString() {
            return "(" + this.mMode + ": " + AalUtils.this.modeToString(this.mMode) + ", " + this.mName + ")";
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof AalIndex)) {
                return false;
            }
            AalIndex aalIndex = (AalIndex) obj;
            if (this.mName == null && aalIndex.mName == null) {
                if (this.mMode != aalIndex.mMode) {
                    return false;
                }
                return true;
            }
            if (this.mName == null || aalIndex.mName == null || this.mMode != aalIndex.mMode || !this.mName.equals(aalIndex.mName)) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            String str = Integer.toString(this.mMode) + ":";
            if (this.mName != null) {
                str = str + Integer.toString(this.mName.hashCode());
            }
            return str.hashCode();
        }
    }

    public int dump(PrintWriter printWriter, String[] strArr, int i) {
        if (sIsAalSupported) {
            if (strArr.length <= 1) {
                printWriter.println(dumpDebugUsageInternal());
                return i;
            }
            String str = strArr[i];
            if ("dump".equals(str) && strArr.length == 2) {
                printWriter.println(dumpInternal());
                return i;
            }
            if ("debugon".equals(str) && strArr.length == 2) {
                printWriter.println(setDebugInternal(true));
                printWriter.println("App-based AAL debug on");
                return i;
            }
            if ("debugoff".equals(str) && strArr.length == 2) {
                printWriter.println(setDebugInternal(false));
                printWriter.println("App-based AAL debug off");
                return i;
            }
            if ("on".equals(str) && strArr.length == 2) {
                setEnabledInternal(true);
                printWriter.println("App-based AAL on");
                return i;
            }
            if ("off".equals(str) && strArr.length == 2) {
                setEnabledInternal(false);
                printWriter.println("App-based AAL off");
                return i;
            }
            if ("mode".equals(str) && strArr.length == 3) {
                int i2 = i + 1;
                printWriter.println(setAalModeInternal(Integer.parseInt(strArr[i2])));
                printWriter.println("Done");
                return i2;
            }
            if ("set".equals(str) && (strArr.length == 4 || strArr.length == 5)) {
                int i3 = i + 1;
                String str2 = strArr[i3];
                int i4 = i3 + 1;
                int i5 = Integer.parseInt(strArr[i4]);
                if (strArr.length == 4) {
                    printWriter.println(setSmartBacklightTableInternal(str2, i5));
                } else {
                    i4++;
                    printWriter.println(setSmartBacklightTableInternal(str2, i5, Integer.parseInt(strArr[i4])));
                }
                printWriter.println("Done");
                return i4;
            }
            printWriter.println(dumpDebugUsageInternal());
            return i;
        }
        printWriter.println("Not support App-based AAL");
        return i;
    }

    private String dumpDebugUsageInternal() {
        return "\nUsage:\n1. App-based AAL help:\n    adb shell dumpsys activity aal\n2. Dump App-based AAL settings:\n    adb shell dumpsys activity aal dump\n1. App-based AAL debug on:\n    adb shell dumpsys activity aal debugon\n1. App-based AAL debug off:\n    adb shell dumpsys activity aal debugoff\n3. Enable App-based AAL:\n    adb shell dumpsys activity aal on\n4. Disable App-based AAL:\n    adb shell dumpsys activity aal off\n5. Set App-based AAL mode:\n    adb shell dumpsys activity aal mode <mode>\n6. Set App-based AAL config for current mode:\n    adb shell dumpsys activity aal set <component> <value>\n7. Set App-based AAL config for the mode:\n    adb shell dumpsys activity aal set <component> <value> <mode>\n";
    }

    private synchronized String dumpInternal() {
        StringBuilder sb;
        sb = new StringBuilder();
        sb.append("\nApp-based AAL Mode: " + this.mAalMode + "(" + modeToString(this.mAalMode) + "), Supported: " + sIsAalSupported + ", Enabled: " + sEnabled + ", Debug: " + sDebug + "\n");
        int i = 1;
        for (AalIndex aalIndex : this.mConfig.keySet()) {
            sb.append("\n" + i + ". " + aalIndex + " - " + Integer.toString(this.mConfig.get(aalIndex).intValue()));
            i++;
        }
        if (i == 1) {
            sb.append("\nThere is no App-based AAL configuration.\n");
            sb.append(dumpDebugUsageInternal());
        }
        if (sDebug) {
            Slog.d(TAG, "dump config: " + sb.toString());
        }
        return sb.toString();
    }

    private synchronized String setDebugInternal(boolean z) {
        sDebug = z;
        return "Set Debug: " + z;
    }

    private synchronized String setSmartBacklightTableInternal(String str, int i) {
        return setSmartBacklightTableInternal(str, i, this.mAalMode);
    }

    private synchronized String setSmartBacklightTableInternal(String str, int i, int i2) {
        if (!sEnabled) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not enabled");
            }
            return "AAL is not enabled";
        }
        if (i2 < 0 || i2 >= 3) {
            String str2 = "Unknown mode: " + i2;
            if (sDebug) {
                Slog.d(TAG, str2);
            }
            return str2;
        }
        AalIndex aalIndex = new AalIndex(i2, str);
        if (sDebug) {
            Slog.d(TAG, "setSmartBacklightTable(" + i + ") for " + aalIndex);
        }
        this.mConfig.put(aalIndex, Integer.valueOf(i));
        return "Set(" + i + ") for " + aalIndex;
    }

    private void parseXML() throws XmlPullParserException, IOException {
        if (!new File(sAalConfigXMLPath).exists()) {
            if (sDebug) {
                Slog.d(TAG, "parseXML file not exists: " + sAalConfigXMLPath);
                return;
            }
            return;
        }
        FileReader fileReader = new FileReader(sAalConfigXMLPath);
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(fileReader);
        for (int eventType = xmlPullParserNewPullParser.getEventType(); eventType != 1; eventType = xmlPullParserNewPullParser.next()) {
            if (eventType != 0 && eventType == 2 && xmlPullParserNewPullParser.getName().equals("config")) {
                this.mConfig.put(new AalIndex(Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(0)), xmlPullParserNewPullParser.getAttributeValue(1)), Integer.valueOf(Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(2))));
            }
        }
        fileReader.close();
    }
}
