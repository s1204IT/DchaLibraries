package com.android.server.am;

import android.app.AppGlobals;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.res.CompatibilityInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class CompatModePackages {
    public static final int COMPAT_FLAG_DONT_ASK = 1;
    public static final int COMPAT_FLAG_ENABLED = 2;
    private static final int MSG_WRITE = 300;
    private final AtomicFile mFile;
    private final CompatHandler mHandler;
    private final ActivityManagerService mService;
    private final String TAG = "ActivityManager";
    private final boolean DEBUG_CONFIGURATION = false;
    private final HashMap<String, Integer> mPackages = new HashMap<>();

    private final class CompatHandler extends Handler {
        public CompatHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CompatModePackages.MSG_WRITE:
                    CompatModePackages.this.saveCompatModes();
                    break;
            }
        }
    }

    public CompatModePackages(ActivityManagerService service, File systemDir, Handler handler) {
        String pkg;
        this.mService = service;
        this.mFile = new AtomicFile(new File(systemDir, "packages-compat.xml"));
        this.mHandler = new CompatHandler(handler.getLooper());
        FileInputStream fis = null;
        try {
            try {
                fis = this.mFile.openRead();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis, null);
                int eventType = parser.getEventType();
                while (eventType != 2 && eventType != 1) {
                    eventType = parser.next();
                }
                if (eventType == 1) {
                    if (fis != null) {
                        try {
                            fis.close();
                            return;
                        } catch (IOException e) {
                            return;
                        }
                    }
                    return;
                }
                String tagName = parser.getName();
                if ("compat-packages".equals(tagName)) {
                    int eventType2 = parser.next();
                    do {
                        if (eventType2 == 2) {
                            String tagName2 = parser.getName();
                            if (parser.getDepth() == 2 && "pkg".equals(tagName2) && (pkg = parser.getAttributeValue(null, "name")) != null) {
                                String mode = parser.getAttributeValue(null, "mode");
                                int modeInt = 0;
                                if (mode != null) {
                                    try {
                                        modeInt = Integer.parseInt(mode);
                                    } catch (NumberFormatException e2) {
                                    }
                                }
                                this.mPackages.put(pkg, Integer.valueOf(modeInt));
                            }
                        }
                        eventType2 = parser.next();
                    } while (eventType2 != 1);
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e3) {
                    }
                }
            } catch (IOException e4) {
                if (fis != null) {
                    Slog.w("ActivityManager", "Error reading compat-packages", e4);
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e5) {
                    }
                }
            } catch (XmlPullParserException e6) {
                Slog.w("ActivityManager", "Error reading compat-packages", e6);
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e7) {
                    }
                }
            }
        } catch (Throwable th) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e8) {
                }
            }
            throw th;
        }
    }

    public HashMap<String, Integer> getPackages() {
        return this.mPackages;
    }

    private int getPackageFlags(String packageName) {
        Integer flags = this.mPackages.get(packageName);
        if (flags != null) {
            return flags.intValue();
        }
        return 0;
    }

    public void handlePackageAddedLocked(String packageName, boolean updated) {
        boolean mayCompat = false;
        ApplicationInfo ai = null;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0, 0);
        } catch (RemoteException e) {
        }
        if (ai != null) {
            CompatibilityInfo ci = compatibilityInfoForPackageLocked(ai);
            if (!ci.alwaysSupportsScreen() && !ci.neverSupportsScreen()) {
                mayCompat = true;
            }
            if (updated && !mayCompat && this.mPackages.containsKey(packageName)) {
                this.mPackages.remove(packageName);
                this.mHandler.removeMessages(MSG_WRITE);
                Message msg = this.mHandler.obtainMessage(MSG_WRITE);
                this.mHandler.sendMessageDelayed(msg, 10000L);
            }
        }
    }

    public CompatibilityInfo compatibilityInfoForPackageLocked(ApplicationInfo ai) {
        CompatibilityInfo ci = new CompatibilityInfo(ai, this.mService.mConfiguration.screenLayout, this.mService.mConfiguration.smallestScreenWidthDp, (getPackageFlags(ai.packageName) & 2) != 0);
        return ci;
    }

    public int computeCompatModeLocked(ApplicationInfo ai) {
        boolean enabled = (getPackageFlags(ai.packageName) & 2) != 0;
        CompatibilityInfo info = new CompatibilityInfo(ai, this.mService.mConfiguration.screenLayout, this.mService.mConfiguration.smallestScreenWidthDp, enabled);
        if (info.alwaysSupportsScreen()) {
            return -2;
        }
        if (info.neverSupportsScreen()) {
            return -1;
        }
        return !enabled ? 0 : 1;
    }

    public boolean getFrontActivityAskCompatModeLocked() {
        ActivityRecord r = this.mService.getFocusedStack().topRunningActivityLocked(null);
        if (r == null) {
            return false;
        }
        return getPackageAskCompatModeLocked(r.packageName);
    }

    public boolean getPackageAskCompatModeLocked(String packageName) {
        return (getPackageFlags(packageName) & 1) == 0;
    }

    public void setFrontActivityAskCompatModeLocked(boolean ask) {
        ActivityRecord r = this.mService.getFocusedStack().topRunningActivityLocked(null);
        if (r != null) {
            setPackageAskCompatModeLocked(r.packageName, ask);
        }
    }

    public void setPackageAskCompatModeLocked(String packageName, boolean ask) {
        int curFlags = getPackageFlags(packageName);
        int newFlags = ask ? curFlags & (-2) : curFlags | 1;
        if (curFlags != newFlags) {
            if (newFlags != 0) {
                this.mPackages.put(packageName, Integer.valueOf(newFlags));
            } else {
                this.mPackages.remove(packageName);
            }
            this.mHandler.removeMessages(MSG_WRITE);
            Message msg = this.mHandler.obtainMessage(MSG_WRITE);
            this.mHandler.sendMessageDelayed(msg, 10000L);
        }
    }

    public int getFrontActivityScreenCompatModeLocked() {
        ActivityRecord r = this.mService.getFocusedStack().topRunningActivityLocked(null);
        if (r == null) {
            return -3;
        }
        return computeCompatModeLocked(r.info.applicationInfo);
    }

    public void setFrontActivityScreenCompatModeLocked(int mode) {
        ActivityRecord r = this.mService.getFocusedStack().topRunningActivityLocked(null);
        if (r == null) {
            Slog.w("ActivityManager", "setFrontActivityScreenCompatMode failed: no top activity");
        } else {
            setPackageScreenCompatModeLocked(r.info.applicationInfo, mode);
        }
    }

    public int getPackageScreenCompatModeLocked(String packageName) {
        ApplicationInfo ai = null;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0, 0);
        } catch (RemoteException e) {
        }
        if (ai == null) {
            return -3;
        }
        return computeCompatModeLocked(ai);
    }

    public void setPackageScreenCompatModeLocked(String packageName, int mode) {
        ApplicationInfo ai = null;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0, 0);
        } catch (RemoteException e) {
        }
        if (ai == null) {
            Slog.w("ActivityManager", "setPackageScreenCompatMode failed: unknown package " + packageName);
        } else {
            setPackageScreenCompatModeLocked(ai, mode);
        }
    }

    private void setPackageScreenCompatModeLocked(ApplicationInfo ai, int mode) {
        boolean enable;
        int newFlags;
        String packageName = ai.packageName;
        int curFlags = getPackageFlags(packageName);
        switch (mode) {
            case 0:
                enable = false;
                break;
            case 1:
                enable = true;
                break;
            case 2:
                enable = (curFlags & 2) == 0;
                break;
            default:
                Slog.w("ActivityManager", "Unknown screen compat mode req #" + mode + "; ignoring");
                return;
        }
        if (enable) {
            newFlags = curFlags | 2;
        } else {
            newFlags = curFlags & (-3);
        }
        CompatibilityInfo ci = compatibilityInfoForPackageLocked(ai);
        if (ci.alwaysSupportsScreen()) {
            Slog.w("ActivityManager", "Ignoring compat mode change of " + packageName + "; compatibility never needed");
            newFlags = 0;
        }
        if (ci.neverSupportsScreen()) {
            Slog.w("ActivityManager", "Ignoring compat mode change of " + packageName + "; compatibility always needed");
            newFlags = 0;
        }
        if (newFlags != curFlags) {
            if (newFlags != 0) {
                this.mPackages.put(packageName, Integer.valueOf(newFlags));
            } else {
                this.mPackages.remove(packageName);
            }
            CompatibilityInfo ci2 = compatibilityInfoForPackageLocked(ai);
            this.mHandler.removeMessages(MSG_WRITE);
            Message msg = this.mHandler.obtainMessage(MSG_WRITE);
            this.mHandler.sendMessageDelayed(msg, 10000L);
            ActivityStack stack = this.mService.getFocusedStack();
            ActivityRecord starting = stack.restartPackage(packageName);
            for (int i = this.mService.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord app = this.mService.mLruProcesses.get(i);
                if (app.pkgList.containsKey(packageName)) {
                    try {
                        if (app.thread != null) {
                            app.thread.updatePackageCompatibilityInfo(packageName, ci2);
                        }
                    } catch (Exception e) {
                    }
                }
            }
            if (starting != null) {
                stack.ensureActivityConfigurationLocked(starting, 0);
                stack.ensureActivitiesVisibleLocked(starting, 0);
            }
        }
    }

    void saveCompatModes() {
        HashMap<String, Integer> pkgs;
        synchronized (this.mService) {
            pkgs = new HashMap<>(this.mPackages);
        }
        FileOutputStream fos = null;
        try {
            fos = this.mFile.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "compat-packages");
            IPackageManager pm = AppGlobals.getPackageManager();
            int screenLayout = this.mService.mConfiguration.screenLayout;
            int smallestScreenWidthDp = this.mService.mConfiguration.smallestScreenWidthDp;
            for (Map.Entry<String, Integer> entry : pkgs.entrySet()) {
                String pkg = entry.getKey();
                int mode = entry.getValue().intValue();
                if (mode != 0) {
                    ApplicationInfo ai = null;
                    try {
                        ai = pm.getApplicationInfo(pkg, 0, 0);
                    } catch (RemoteException e) {
                    }
                    if (ai != null) {
                        CompatibilityInfo info = new CompatibilityInfo(ai, screenLayout, smallestScreenWidthDp, false);
                        if (!info.alwaysSupportsScreen() && !info.neverSupportsScreen()) {
                            fastXmlSerializer.startTag(null, "pkg");
                            fastXmlSerializer.attribute(null, "name", pkg);
                            fastXmlSerializer.attribute(null, "mode", Integer.toString(mode));
                            fastXmlSerializer.endTag(null, "pkg");
                        }
                    }
                }
            }
            fastXmlSerializer.endTag(null, "compat-packages");
            fastXmlSerializer.endDocument();
            this.mFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w("ActivityManager", "Error writing compat packages", e1);
            if (fos != null) {
                this.mFile.failWrite(fos);
            }
        }
    }
}
