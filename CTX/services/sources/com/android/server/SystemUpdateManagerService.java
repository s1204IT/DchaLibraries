package com.android.server;

import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.ISystemUpdateManager;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class SystemUpdateManagerService extends ISystemUpdateManager.Stub {
    private static final String INFO_FILE = "system-update-info.xml";
    private static final int INFO_FILE_VERSION = 0;
    private static final String KEY_BOOT_COUNT = "boot-count";
    private static final String KEY_INFO_BUNDLE = "info-bundle";
    private static final String KEY_UID = "uid";
    private static final String KEY_VERSION = "version";
    private static final String TAG = "SystemUpdateManagerService";
    private static final String TAG_INFO = "info";
    private static final int UID_UNKNOWN = -1;
    private final Context mContext;
    private final Object mLock = new Object();
    private int mLastUid = -1;
    private int mLastStatus = 0;
    private final AtomicFile mFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), INFO_FILE));

    public SystemUpdateManagerService(Context context) {
        this.mContext = context;
        synchronized (this.mLock) {
            loadSystemUpdateInfoLocked();
        }
    }

    public void updateSystemUpdateInfo(PersistableBundle persistableBundle) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", TAG);
        int i = persistableBundle.getInt("status", 0);
        if (i == 0) {
            Slog.w(TAG, "Invalid status info. Ignored");
            return;
        }
        int callingUid = Binder.getCallingUid();
        if (this.mLastUid == -1 || this.mLastUid == callingUid || i != 1) {
            synchronized (this.mLock) {
                saveSystemUpdateInfoLocked(persistableBundle, callingUid);
            }
            return;
        }
        Slog.i(TAG, "Inactive updater reporting IDLE status. Ignored");
    }

    public Bundle retrieveSystemUpdateInfo() {
        Bundle bundleLoadSystemUpdateInfoLocked;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.READ_SYSTEM_UPDATE_INFO") == -1 && this.mContext.checkCallingOrSelfPermission("android.permission.RECOVERY") == -1) {
            throw new SecurityException("Can't read system update info. Requiring READ_SYSTEM_UPDATE_INFO or RECOVERY permission.");
        }
        synchronized (this.mLock) {
            bundleLoadSystemUpdateInfoLocked = loadSystemUpdateInfoLocked();
        }
        return bundleLoadSystemUpdateInfoLocked;
    }

    private Bundle loadSystemUpdateInfoLocked() throws Throwable {
        PersistableBundle infoFileLocked;
        Throwable th;
        try {
            FileInputStream fileInputStreamOpenRead = this.mFile.openRead();
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                infoFileLocked = readInfoFileLocked(xmlPullParserNewPullParser);
                if (fileInputStreamOpenRead != null) {
                    try {
                        fileInputStreamOpenRead.close();
                    } catch (FileNotFoundException e) {
                        Slog.i(TAG, "No existing info file " + this.mFile.getBaseFile());
                    } catch (IOException e2) {
                        e = e2;
                        Slog.e(TAG, "Failed to read the info file:", e);
                    } catch (XmlPullParserException e3) {
                        e = e3;
                        Slog.e(TAG, "Failed to parse the info file:", e);
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                th = null;
                if (fileInputStreamOpenRead != null) {
                }
            }
        } catch (FileNotFoundException e4) {
            infoFileLocked = null;
            Slog.i(TAG, "No existing info file " + this.mFile.getBaseFile());
            if (infoFileLocked != null) {
            }
        } catch (IOException e5) {
            e = e5;
            infoFileLocked = null;
            Slog.e(TAG, "Failed to read the info file:", e);
            if (infoFileLocked != null) {
            }
        } catch (XmlPullParserException e6) {
            e = e6;
            infoFileLocked = null;
            Slog.e(TAG, "Failed to parse the info file:", e);
            if (infoFileLocked != null) {
            }
        }
        if (infoFileLocked != null) {
            return removeInfoFileAndGetDefaultInfoBundleLocked();
        }
        if (infoFileLocked.getInt(KEY_VERSION, -1) == -1) {
            Slog.w(TAG, "Invalid info file (invalid version). Ignored");
            return removeInfoFileAndGetDefaultInfoBundleLocked();
        }
        int i = infoFileLocked.getInt("uid", -1);
        if (i == -1) {
            Slog.w(TAG, "Invalid info file (invalid UID). Ignored");
            return removeInfoFileAndGetDefaultInfoBundleLocked();
        }
        int i2 = infoFileLocked.getInt(KEY_BOOT_COUNT, -1);
        if (i2 == -1 || i2 != getBootCount()) {
            Slog.w(TAG, "Outdated info file. Ignored");
            return removeInfoFileAndGetDefaultInfoBundleLocked();
        }
        PersistableBundle persistableBundle = infoFileLocked.getPersistableBundle(KEY_INFO_BUNDLE);
        if (persistableBundle == null) {
            Slog.w(TAG, "Invalid info file (missing info). Ignored");
            return removeInfoFileAndGetDefaultInfoBundleLocked();
        }
        int i3 = persistableBundle.getInt("status", 0);
        if (i3 == 0) {
            Slog.w(TAG, "Invalid info file (invalid status). Ignored");
            return removeInfoFileAndGetDefaultInfoBundleLocked();
        }
        this.mLastStatus = i3;
        this.mLastUid = i;
        return new Bundle(persistableBundle);
    }

    private void saveSystemUpdateInfoLocked(PersistableBundle persistableBundle, int i) {
        PersistableBundle persistableBundle2 = new PersistableBundle();
        persistableBundle2.putPersistableBundle(KEY_INFO_BUNDLE, persistableBundle);
        persistableBundle2.putInt(KEY_VERSION, 0);
        persistableBundle2.putInt("uid", i);
        persistableBundle2.putInt(KEY_BOOT_COUNT, getBootCount());
        if (writeInfoFileLocked(persistableBundle2)) {
            this.mLastUid = i;
            this.mLastStatus = persistableBundle.getInt("status");
        }
    }

    private PersistableBundle readInfoFileLocked(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next == 2 && TAG_INFO.equals(xmlPullParser.getName())) {
                    return PersistableBundle.restoreFromXml(xmlPullParser);
                }
            } else {
                return null;
            }
        }
    }

    private boolean writeInfoFileLocked(PersistableBundle persistableBundle) {
        FileOutputStream fileOutputStreamStartWrite;
        try {
            fileOutputStreamStartWrite = this.mFile.startWrite();
            try {
                XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, TAG_INFO);
                persistableBundle.saveToXml(fastXmlSerializer);
                fastXmlSerializer.endTag(null, TAG_INFO);
                fastXmlSerializer.endDocument();
                this.mFile.finishWrite(fileOutputStreamStartWrite);
                return true;
            } catch (IOException | XmlPullParserException e) {
                e = e;
                Slog.e(TAG, "Failed to save the info file:", e);
                if (fileOutputStreamStartWrite != null) {
                    this.mFile.failWrite(fileOutputStreamStartWrite);
                    return false;
                }
                return false;
            }
        } catch (IOException | XmlPullParserException e2) {
            e = e2;
            fileOutputStreamStartWrite = null;
        }
    }

    private Bundle removeInfoFileAndGetDefaultInfoBundleLocked() {
        if (this.mFile.exists()) {
            Slog.i(TAG, "Removing info file");
            this.mFile.delete();
        }
        this.mLastStatus = 0;
        this.mLastUid = -1;
        Bundle bundle = new Bundle();
        bundle.putInt("status", 0);
        return bundle;
    }

    private int getBootCount() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "boot_count", 0);
    }
}
