package com.android.providers.settings;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Base64;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.StatsLog;
import android.util.TimeUtils;
import android.util.Xml;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class SettingsState {

    @GuardedBy("sLock")
    private static Signature sSystemSignature;

    @GuardedBy("mLock")
    private final Context mContext;

    @GuardedBy("mLock")
    private boolean mDirty;
    private final Handler mHandler;

    @GuardedBy("mLock")
    private final List<HistoricalOperation> mHistoricalOperations;

    @GuardedBy("mLock")
    public final int mKey;

    @GuardedBy("mLock")
    private long mLastNotWrittenMutationTimeMillis;
    private final Object mLock;

    @GuardedBy("mLock")
    private final int mMaxBytesPerAppPackage;

    @GuardedBy("mLock")
    private int mNextHistoricalOpIdx;

    @GuardedBy("mLock")
    private long mNextId;

    @GuardedBy("mLock")
    private final ArrayMap<String, Integer> mPackageToMemoryUsage;

    @GuardedBy("mLock")
    private final File mStatePersistFile;

    @GuardedBy("mLock")
    private final String mStatePersistTag;

    @GuardedBy("mLock")
    private boolean mWriteScheduled;
    private static final Object sLock = new Object();

    @GuardedBy("sLock")
    private static final SparseIntArray sSystemUids = new SparseIntArray();
    private final Object mWriteLock = new Object();

    @GuardedBy("mLock")
    private final ArrayMap<String, Setting> mSettings = new ArrayMap<>();
    private final Setting mNullSetting = new Setting(null, null, false, null, null) {
        @Override
        public boolean isNull() {
            return true;
        }
    };

    @GuardedBy("mLock")
    private int mVersion = -1;

    static long access$408(SettingsState settingsState) {
        long j = settingsState.mNextId;
        settingsState.mNextId = 1 + j;
        return j;
    }

    public static int makeKey(int i, int i2) {
        return (i << 28) | i2;
    }

    public static int getTypeFromKey(int i) {
        return i >>> 28;
    }

    public static int getUserIdFromKey(int i) {
        return i & 268435455;
    }

    public SettingsState(Context context, Object obj, File file, int i, int i2, Looper looper) {
        this.mContext = context;
        this.mLock = obj;
        this.mStatePersistFile = file;
        this.mStatePersistTag = "settings-" + getTypeFromKey(i) + "-" + getUserIdFromKey(i);
        this.mKey = i;
        this.mHandler = new MyHandler(looper);
        if (i2 == 20000) {
            this.mMaxBytesPerAppPackage = i2;
            this.mPackageToMemoryUsage = new ArrayMap<>();
        } else {
            this.mMaxBytesPerAppPackage = i2;
            this.mPackageToMemoryUsage = null;
        }
        this.mHistoricalOperations = Build.IS_DEBUGGABLE ? new ArrayList(20) : null;
        synchronized (this.mLock) {
            readStateSyncLocked();
        }
    }

    public int getVersionLocked() {
        return this.mVersion;
    }

    public Setting getNullSetting() {
        return this.mNullSetting;
    }

    public void setVersionLocked(int i) {
        if (i == this.mVersion) {
            return;
        }
        this.mVersion = i;
        scheduleWriteIfNeededLocked();
    }

    public void onPackageRemovedLocked(String str) {
        boolean z = false;
        for (int size = this.mSettings.size() - 1; size >= 0; size--) {
            String strKeyAt = this.mSettings.keyAt(size);
            if (!Settings.System.PUBLIC_SETTINGS.contains(strKeyAt) && !Settings.System.PRIVATE_SETTINGS.contains(strKeyAt) && str.equals(this.mSettings.valueAt(size).packageName)) {
                this.mSettings.removeAt(size);
                z = true;
            }
        }
        if (z) {
            scheduleWriteIfNeededLocked();
        }
    }

    public List<String> getSettingNamesLocked() {
        ArrayList arrayList = new ArrayList();
        int size = this.mSettings.size();
        for (int i = 0; i < size; i++) {
            arrayList.add(this.mSettings.keyAt(i));
        }
        return arrayList;
    }

    public Setting getSettingLocked(String str) {
        if (TextUtils.isEmpty(str)) {
            return this.mNullSetting;
        }
        Setting setting = this.mSettings.get(str);
        if (setting != null) {
            return new Setting(setting);
        }
        return this.mNullSetting;
    }

    public boolean updateSettingLocked(String str, String str2, String str3, boolean z, String str4) {
        if (!hasSettingLocked(str)) {
            return false;
        }
        return insertSettingLocked(str, str2, str3, z, str4);
    }

    public void resetSettingDefaultValueLocked(String str) {
        Setting settingLocked = getSettingLocked(str);
        if (settingLocked != null && !settingLocked.isNull() && settingLocked.getDefaultValue() != null) {
            String value = settingLocked.getValue();
            String defaultValue = settingLocked.getDefaultValue();
            Setting setting = new Setting(str, settingLocked.getValue(), null, settingLocked.getPackageName(), settingLocked.getTag(), false, settingLocked.getId());
            this.mSettings.put(str, setting);
            updateMemoryUsagePerPackageLocked(setting.getPackageName(), value, setting.getValue(), defaultValue, setting.getDefaultValue());
            scheduleWriteIfNeededLocked();
        }
    }

    public boolean insertSettingLocked(String str, String str2, String str3, boolean z, String str4) {
        String str5;
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        Setting setting = this.mSettings.get(str);
        String str6 = null;
        if (setting == null) {
            str5 = null;
        } else {
            str5 = setting.value;
        }
        if (setting != null) {
            str6 = setting.defaultValue;
        }
        String str7 = str6;
        if (setting != null) {
            if (!setting.update(str2, z, str4, str3, false)) {
                return false;
            }
        } else {
            Setting setting2 = new Setting(str, str2, z, str4, str3);
            this.mSettings.put(str, setting2);
            setting = setting2;
        }
        StatsLog.write(41, str, str2, setting.value, str5, str3, z, getUserIdFromKey(this.mKey), 1);
        addHistoricalOperationLocked("update", setting);
        updateMemoryUsagePerPackageLocked(str4, str5, str2, str7, setting.getDefaultValue());
        scheduleWriteIfNeededLocked();
        return true;
    }

    public void persistSyncLocked() {
        this.mHandler.removeMessages(1);
        doWriteState();
    }

    public boolean deleteSettingLocked(String str) {
        if (TextUtils.isEmpty(str) || !hasSettingLocked(str)) {
            return false;
        }
        Setting settingRemove = this.mSettings.remove(str);
        StatsLog.write(41, str, "", "", settingRemove.value, "", false, getUserIdFromKey(this.mKey), 2);
        updateMemoryUsagePerPackageLocked(settingRemove.packageName, settingRemove.value, null, settingRemove.defaultValue, null);
        addHistoricalOperationLocked("delete", settingRemove);
        scheduleWriteIfNeededLocked();
        return true;
    }

    public boolean resetSettingLocked(String str) {
        if (TextUtils.isEmpty(str) || !hasSettingLocked(str)) {
            return false;
        }
        Setting setting = this.mSettings.get(str);
        Setting setting2 = new Setting(setting);
        String value = setting.getValue();
        String defaultValue = setting.getDefaultValue();
        if (!setting.reset()) {
            return false;
        }
        updateMemoryUsagePerPackageLocked(setting.packageName, value, setting.getValue(), defaultValue, setting.getDefaultValue());
        addHistoricalOperationLocked("reset", setting2);
        scheduleWriteIfNeededLocked();
        return true;
    }

    public void destroyLocked(Runnable runnable) {
        this.mHandler.removeMessages(1);
        if (runnable != null) {
            if (this.mDirty) {
                this.mHandler.obtainMessage(1, runnable).sendToTarget();
            } else {
                runnable.run();
            }
        }
    }

    private void addHistoricalOperationLocked(String str, Setting setting) {
        if (this.mHistoricalOperations == null) {
            return;
        }
        HistoricalOperation historicalOperation = new HistoricalOperation(SystemClock.elapsedRealtime(), str, setting != null ? new Setting(setting) : null);
        if (this.mNextHistoricalOpIdx >= this.mHistoricalOperations.size()) {
            this.mHistoricalOperations.add(historicalOperation);
        } else {
            this.mHistoricalOperations.set(this.mNextHistoricalOpIdx, historicalOperation);
        }
        this.mNextHistoricalOpIdx++;
        if (this.mNextHistoricalOpIdx >= 20) {
            this.mNextHistoricalOpIdx = 0;
        }
    }

    void dumpHistoricalOperations(ProtoOutputStream protoOutputStream, long j) {
        synchronized (this.mLock) {
            if (this.mHistoricalOperations == null) {
                return;
            }
            int size = this.mHistoricalOperations.size();
            for (int i = 0; i < size; i++) {
                int i2 = (this.mNextHistoricalOpIdx - 1) - i;
                if (i2 < 0) {
                    i2 += size;
                }
                HistoricalOperation historicalOperation = this.mHistoricalOperations.get(i2);
                long jStart = protoOutputStream.start(j);
                protoOutputStream.write(1112396529665L, historicalOperation.mTimestamp);
                protoOutputStream.write(1138166333442L, historicalOperation.mOperation);
                if (historicalOperation.mSetting != null) {
                    protoOutputStream.write(1138166333443L, historicalOperation.mSetting.getName());
                }
                protoOutputStream.end(jStart);
            }
        }
    }

    public void dumpHistoricalOperations(PrintWriter printWriter) {
        synchronized (this.mLock) {
            if (this.mHistoricalOperations == null) {
                return;
            }
            printWriter.println("Historical operations");
            int size = this.mHistoricalOperations.size();
            for (int i = 0; i < size; i++) {
                int i2 = (this.mNextHistoricalOpIdx - 1) - i;
                if (i2 < 0) {
                    i2 += size;
                }
                HistoricalOperation historicalOperation = this.mHistoricalOperations.get(i2);
                printWriter.print(TimeUtils.formatForLogging(historicalOperation.mTimestamp));
                printWriter.print(" ");
                printWriter.print(historicalOperation.mOperation);
                if (historicalOperation.mSetting != null) {
                    printWriter.print(" ");
                    printWriter.print(historicalOperation.mSetting.getName());
                }
                printWriter.println();
            }
            printWriter.println();
            printWriter.println();
        }
    }

    private void updateMemoryUsagePerPackageLocked(String str, String str2, String str3, String str4, String str5) {
        if (this.mMaxBytesPerAppPackage == -1 || "android".equals(str)) {
            return;
        }
        int length = (((str3 != null ? str3.length() : 0) + (str5 != null ? str5.length() : 0)) - (str2 != null ? str2.length() : 0)) - (str4 != null ? str4.length() : 0);
        Integer num = this.mPackageToMemoryUsage.get(str);
        if (num != null) {
            length += num.intValue();
        }
        int iMax = Math.max(length, 0);
        if (iMax > this.mMaxBytesPerAppPackage) {
            throw new IllegalStateException("You are adding too many system settings. You should stop using system settings for app specific data package: " + str);
        }
        this.mPackageToMemoryUsage.put(str, Integer.valueOf(iMax));
    }

    private boolean hasSettingLocked(String str) {
        return this.mSettings.indexOfKey(str) >= 0;
    }

    private void scheduleWriteIfNeededLocked() {
        if (!this.mDirty) {
            this.mDirty = true;
            writeStateAsyncLocked();
        }
    }

    private void writeStateAsyncLocked() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (this.mWriteScheduled) {
            this.mHandler.removeMessages(1);
            if (jUptimeMillis - this.mLastNotWrittenMutationTimeMillis >= 2000) {
                this.mHandler.obtainMessage(1).sendToTarget();
                return;
            }
            long jMin = Math.min(200L, Math.max((this.mLastNotWrittenMutationTimeMillis + 2000) - jUptimeMillis, 0L));
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), jMin);
            return;
        }
        this.mLastNotWrittenMutationTimeMillis = jUptimeMillis;
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), 200L);
        this.mWriteScheduled = true;
    }

    private void doWriteState() {
        int i;
        ArrayMap arrayMap;
        FileOutputStream fileOutputStreamStartWrite;
        boolean z;
        int i2;
        XmlSerializer xmlSerializer;
        synchronized (this.mLock) {
            i = this.mVersion;
            arrayMap = new ArrayMap(this.mSettings);
            this.mDirty = false;
            this.mWriteScheduled = false;
        }
        synchronized (this.mWriteLock) {
            AtomicFile atomicFile = new AtomicFile(this.mStatePersistFile, this.mStatePersistTag);
            try {
                fileOutputStreamStartWrite = atomicFile.startWrite();
                try {
                    try {
                        XmlSerializer xmlSerializerNewSerializer = Xml.newSerializer();
                        xmlSerializerNewSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                        xmlSerializerNewSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                        xmlSerializerNewSerializer.startDocument(null, true);
                        xmlSerializerNewSerializer.startTag(null, "settings");
                        xmlSerializerNewSerializer.attribute(null, "version", String.valueOf(i));
                        int size = arrayMap.size();
                        int i3 = 0;
                        while (i3 < size) {
                            Setting setting = (Setting) arrayMap.valueAt(i3);
                            if (setting.isTransient()) {
                                i2 = i3;
                                xmlSerializer = xmlSerializerNewSerializer;
                            } else {
                                i2 = i3;
                                xmlSerializer = xmlSerializerNewSerializer;
                                writeSingleSetting(this.mVersion, xmlSerializerNewSerializer, setting.getId(), setting.getName(), setting.getValue(), setting.getDefaultValue(), setting.getPackageName(), setting.getTag(), setting.isDefaultFromSystem());
                            }
                            i3 = i2 + 1;
                            xmlSerializerNewSerializer = xmlSerializer;
                        }
                        XmlSerializer xmlSerializer2 = xmlSerializerNewSerializer;
                        xmlSerializer2.endTag(null, "settings");
                        xmlSerializer2.endDocument();
                        atomicFile.finishWrite(fileOutputStreamStartWrite);
                        IoUtils.closeQuietly(fileOutputStreamStartWrite);
                        z = true;
                    } catch (Throwable th) {
                        th = th;
                        Slog.wtf("SettingsState", "Failed to write settings, restoring backup", th);
                        atomicFile.failWrite(fileOutputStreamStartWrite);
                        IoUtils.closeQuietly(fileOutputStreamStartWrite);
                        z = false;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(fileOutputStreamStartWrite);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                fileOutputStreamStartWrite = null;
            }
        }
        if (z) {
            synchronized (this.mLock) {
                addHistoricalOperationLocked("persist", null);
            }
        }
    }

    static void writeSingleSetting(int i, XmlSerializer xmlSerializer, String str, String str2, String str3, String str4, String str5, String str6, boolean z) throws IOException {
        if (str == null || isBinary(str) || str2 == null || isBinary(str2) || str5 == null || isBinary(str5)) {
            return;
        }
        xmlSerializer.startTag(null, "setting");
        xmlSerializer.attribute(null, "id", str);
        xmlSerializer.attribute(null, "name", str2);
        setValueAttribute("value", "valueBase64", i, xmlSerializer, str3);
        xmlSerializer.attribute(null, "package", str5);
        if (str4 != null) {
            setValueAttribute("defaultValue", "defaultValueBase64", i, xmlSerializer, str4);
            xmlSerializer.attribute(null, "defaultSysSet", Boolean.toString(z));
            setValueAttribute("tag", "tagBase64", i, xmlSerializer, str6);
        }
        xmlSerializer.endTag(null, "setting");
    }

    static void setValueAttribute(String str, String str2, int i, XmlSerializer xmlSerializer, String str3) throws IOException {
        if (i >= 121) {
            if (str3 != null) {
                if (isBinary(str3)) {
                    xmlSerializer.attribute(null, str2, base64Encode(str3));
                    return;
                } else {
                    xmlSerializer.attribute(null, str, str3);
                    return;
                }
            }
            return;
        }
        if (str3 == null) {
            xmlSerializer.attribute(null, str, "null");
        } else {
            xmlSerializer.attribute(null, str, str3);
        }
    }

    private String getValueAttribute(XmlPullParser xmlPullParser, String str, String str2) {
        if (this.mVersion >= 121) {
            String attributeValue = xmlPullParser.getAttributeValue(null, str);
            if (attributeValue != null) {
                return attributeValue;
            }
            String attributeValue2 = xmlPullParser.getAttributeValue(null, str2);
            if (attributeValue2 != null) {
                return base64Decode(attributeValue2);
            }
            return null;
        }
        String attributeValue3 = xmlPullParser.getAttributeValue(null, str);
        if ("null".equals(attributeValue3)) {
            return null;
        }
        return attributeValue3;
    }

    private void readStateSyncLocked() {
        try {
            FileInputStream fileInputStreamOpenRead = new AtomicFile(this.mStatePersistFile).openRead();
            try {
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                    parseStateLocked(xmlPullParserNewPullParser);
                } catch (IOException | XmlPullParserException e) {
                    String str = "Failed parsing settings file: " + this.mStatePersistFile;
                    Slog.wtf("SettingsState", str);
                    throw new IllegalStateException(str, e);
                }
            } finally {
                IoUtils.closeQuietly(fileInputStreamOpenRead);
            }
        } catch (FileNotFoundException e2) {
            Slog.i("SettingsState", "No settings state " + this.mStatePersistFile);
            addHistoricalOperationLocked("initialize", null);
        }
    }

    public static boolean stateFileExists(File file) {
        return new AtomicFile(file).exists();
    }

    private void parseStateLocked(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4 && xmlPullParser.getName().equals("settings")) {
                        parseSettingsLocked(xmlPullParser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void parseSettingsLocked(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        String valueAttribute;
        boolean z;
        this.mVersion = Integer.parseInt(xmlPullParser.getAttributeValue(null, "version"));
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4 && xmlPullParser.getName().equals("setting")) {
                        String attributeValue = xmlPullParser.getAttributeValue(null, "id");
                        String attributeValue2 = xmlPullParser.getAttributeValue(null, "name");
                        String valueAttribute2 = getValueAttribute(xmlPullParser, "value", "valueBase64");
                        String attributeValue3 = xmlPullParser.getAttributeValue(null, "package");
                        String valueAttribute3 = getValueAttribute(xmlPullParser, "defaultValue", "defaultValueBase64");
                        if (valueAttribute3 == null) {
                            valueAttribute = null;
                            z = false;
                        } else {
                            z = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, "defaultSysSet"));
                            valueAttribute = getValueAttribute(xmlPullParser, "tag", "tagBase64");
                        }
                        this.mSettings.put(attributeValue2, new Setting(attributeValue2, valueAttribute2, valueAttribute3, attributeValue3, valueAttribute, z, attributeValue));
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private final class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                Runnable runnable = (Runnable) message.obj;
                SettingsState.this.doWriteState();
                if (runnable != null) {
                    runnable.run();
                }
            }
        }
    }

    private class HistoricalOperation {
        final String mOperation;
        final Setting mSetting;
        final long mTimestamp;

        public HistoricalOperation(long j, String str, Setting setting) {
            this.mTimestamp = j;
            this.mOperation = str;
            this.mSetting = setting;
        }
    }

    class Setting {
        private boolean defaultFromSystem;
        private String defaultValue;
        private String id;
        private String name;
        private String packageName;
        private String tag;
        private String value;

        public Setting(Setting setting) {
            this.name = setting.name;
            this.value = setting.value;
            this.defaultValue = setting.defaultValue;
            this.packageName = setting.packageName;
            this.id = setting.id;
            this.defaultFromSystem = setting.defaultFromSystem;
            this.tag = setting.tag;
        }

        public Setting(String str, String str2, boolean z, String str3, String str4) {
            this.name = str;
            update(str2, z, str3, str4, false);
        }

        public Setting(String str, String str2, String str3, String str4, String str5, boolean z, String str6) {
            SettingsState.this.mNextId = Math.max(SettingsState.this.mNextId, Long.parseLong(str6) + 1);
            String str7 = str2;
            init(str, "null".equals(str7) ? null : str7, str5, str3, str4, z, str6);
        }

        private void init(String str, String str2, String str3, String str4, String str5, boolean z, String str6) {
            this.name = str;
            this.value = str2;
            this.tag = str3;
            this.defaultValue = str4;
            this.packageName = str5;
            this.id = str6;
            this.defaultFromSystem = z;
        }

        public String getName() {
            return this.name;
        }

        public int getKey() {
            return SettingsState.this.mKey;
        }

        public String getValue() {
            return this.value;
        }

        public String getTag() {
            return this.tag;
        }

        public String getDefaultValue() {
            return this.defaultValue;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public boolean isDefaultFromSystem() {
            return this.defaultFromSystem;
        }

        public String getId() {
            return this.id;
        }

        public boolean isNull() {
            return false;
        }

        public boolean reset() {
            return update(this.defaultValue, false, this.packageName, null, true);
        }

        public boolean isTransient() {
            if (SettingsState.getTypeFromKey(getKey()) == 0) {
                return ArrayUtils.contains(Settings.Global.TRANSIENT_SETTINGS, getName());
            }
            return false;
        }

        public boolean update(String str, boolean z, String str2, String str3, boolean z2) {
            String str4;
            String str5;
            boolean z3;
            String str6 = "null".equals(str) ? null : str;
            boolean z4 = (z2 || isNull() || !SettingsState.isSystemPackage(SettingsState.this.mContext, str2)) ? false : true;
            if (z4) {
                z = true;
            }
            String str7 = this.defaultValue;
            boolean z5 = this.defaultFromSystem;
            if (z) {
                if (!Objects.equals(str6, this.defaultValue) && (!z5 || z4)) {
                    if (str6 == null) {
                        z5 = false;
                        str3 = null;
                    }
                    str7 = str6;
                }
                if (!z5 && str6 != null && z4) {
                    str4 = str3;
                    z3 = true;
                    str5 = str7;
                }
            } else {
                str4 = str3;
                str5 = str7;
                z3 = z5;
            }
            if (Objects.equals(str6, this.value) && Objects.equals(str5, this.defaultValue) && Objects.equals(str2, this.packageName) && Objects.equals(str4, this.tag) && z3 == this.defaultFromSystem) {
                return false;
            }
            init(this.name, str6, str4, str5, str2, z3, String.valueOf(SettingsState.access$408(SettingsState.this)));
            return true;
        }

        public String toString() {
            String str;
            StringBuilder sb = new StringBuilder();
            sb.append("Setting{name=");
            sb.append(this.name);
            sb.append(" value=");
            sb.append(this.value);
            if (this.defaultValue != null) {
                str = " default=" + this.defaultValue;
            } else {
                str = "";
            }
            sb.append(str);
            sb.append(" packageName=");
            sb.append(this.packageName);
            sb.append(" tag=");
            sb.append(this.tag);
            sb.append(" defaultFromSystem=");
            sb.append(this.defaultFromSystem);
            sb.append("}");
            return sb.toString();
        }
    }

    public static boolean isBinary(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (!((cCharAt >= ' ' && cCharAt <= 55295) || (cCharAt >= 57344 && cCharAt <= 65533))) {
                return true;
            }
        }
        return false;
    }

    private static String base64Encode(String str) {
        return Base64.encodeToString(toBytes(str), 2);
    }

    private static String base64Decode(String str) {
        return fromBytes(Base64.decode(str, 0));
    }

    private static byte[] toBytes(String str) {
        byte[] bArr = new byte[str.length() * 2];
        int i = 0;
        for (int i2 = 0; i2 < str.length(); i2++) {
            char cCharAt = str.charAt(i2);
            int i3 = i + 1;
            bArr[i] = (byte) (cCharAt >> '\b');
            i = i3 + 1;
            bArr[i3] = (byte) cCharAt;
        }
        return bArr;
    }

    private static String fromBytes(byte[] bArr) {
        StringBuffer stringBuffer = new StringBuffer(bArr.length / 2);
        int length = bArr.length - 1;
        for (int i = 0; i < length; i += 2) {
            stringBuffer.append((char) (((bArr[i] & 255) << 8) | (bArr[i + 1] & 255)));
        }
        return stringBuffer.toString();
    }

    public static boolean isSystemPackage(Context context, String str) {
        return isSystemPackage(context, str, Binder.getCallingUid());
    }

    public static boolean isSystemPackage(Context context, String str, int i) {
        synchronized (sLock) {
            if ("android".equals(str)) {
                return true;
            }
            if (!"com.android.shell".equals(str) && !"root".equals(str)) {
                int appId = UserHandle.getAppId(i);
                if (appId < 10000) {
                    sSystemUids.put(appId, appId);
                    return true;
                }
                int userId = UserHandle.getUserId(i);
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    try {
                        int packageUidAsUser = context.getPackageManager().getPackageUidAsUser(str, 0, userId);
                        if (UserHandle.getAppId(packageUidAsUser) < 10000) {
                            sSystemUids.put(packageUidAsUser, packageUidAsUser);
                            return true;
                        }
                        if (sSystemUids.indexOfKey(packageUidAsUser) >= 0) {
                            return true;
                        }
                        if (str.equals(((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getSetupWizardPackageName())) {
                            sSystemUids.put(packageUidAsUser, packageUidAsUser);
                            return true;
                        }
                        try {
                            PackageInfo packageInfoAsUser = context.getPackageManager().getPackageInfoAsUser(str, 64, userId);
                            if ((packageInfoAsUser.applicationInfo.flags & 8) != 0 && (packageInfoAsUser.applicationInfo.flags & 1) != 0) {
                                sSystemUids.put(packageUidAsUser, packageUidAsUser);
                                return true;
                            }
                            if (sSystemSignature == null) {
                                try {
                                    sSystemSignature = context.getPackageManager().getPackageInfoAsUser("android", 64, 0).signatures[0];
                                } catch (PackageManager.NameNotFoundException e) {
                                    return false;
                                }
                            }
                            if (!sSystemSignature.equals(packageInfoAsUser.signatures[0])) {
                                return false;
                            }
                            sSystemUids.put(packageUidAsUser, packageUidAsUser);
                            return true;
                        } catch (PackageManager.NameNotFoundException e2) {
                            return false;
                        }
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                } catch (PackageManager.NameNotFoundException e3) {
                    return false;
                }
            }
            return false;
        }
    }
}
