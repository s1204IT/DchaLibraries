package android.app;

import android.os.FileUtils;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.SettingsStringUtil;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class DexLoadReporter implements BaseDexClassLoader.Reporter {
    private static final boolean DEBUG = false;
    private static final DexLoadReporter INSTANCE = new DexLoadReporter();
    private static final String TAG = "DexLoadReporter";

    @GuardedBy("mDataDirs")
    private final Set<String> mDataDirs = new HashSet();

    private DexLoadReporter() {
    }

    static DexLoadReporter getInstance() {
        return INSTANCE;
    }

    void registerAppDataDir(String str, String str2) {
        if (str2 != null) {
            synchronized (this.mDataDirs) {
                this.mDataDirs.add(str2);
            }
        }
    }

    public void report(List<BaseDexClassLoader> list, List<String> list2) {
        if (list.size() != list2.size()) {
            Slog.wtf(TAG, "Bad call to DexLoadReporter: argument size mismatch");
            return;
        }
        if (list2.isEmpty()) {
            Slog.wtf(TAG, "Bad call to DexLoadReporter: empty dex paths");
            return;
        }
        String[] strArrSplit = list2.get(0).split(File.pathSeparator);
        if (strArrSplit.length == 0) {
            return;
        }
        notifyPackageManager(list, list2);
        registerSecondaryDexForProfiling(strArrSplit);
    }

    private void notifyPackageManager(List<BaseDexClassLoader> list, List<String> list2) {
        ArrayList arrayList = new ArrayList(list2.size());
        Iterator<BaseDexClassLoader> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getClass().getName());
        }
        String strCurrentPackageName = ActivityThread.currentPackageName();
        try {
            ActivityThread.getPackageManager().notifyDexLoad(strCurrentPackageName, arrayList, list2, VMRuntime.getRuntime().vmInstructionSet());
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to notify PM about dex load for package " + strCurrentPackageName, e);
        }
    }

    private void registerSecondaryDexForProfiling(String[] strArr) {
        String[] strArr2;
        if (!SystemProperties.getBoolean("dalvik.vm.dexopt.secondary", false)) {
            return;
        }
        synchronized (this.mDataDirs) {
            strArr2 = (String[]) this.mDataDirs.toArray(new String[0]);
        }
        for (String str : strArr) {
            registerSecondaryDexForProfiling(str, strArr2);
        }
    }

    private void registerSecondaryDexForProfiling(String str, String[] strArr) {
        if (!isSecondaryDexFile(str, strArr)) {
            return;
        }
        File file = new File(str);
        File file2 = new File(file.getParent(), "oat");
        File file3 = new File(file2, file.getName() + ".cur.prof");
        if (!file2.exists() && !file2.mkdir()) {
            Slog.e(TAG, "Could not create the profile directory: " + file3);
            return;
        }
        try {
            file3.createNewFile();
            VMRuntime.registerAppInfo(file3.getPath(), new String[]{str});
        } catch (IOException e) {
            Slog.e(TAG, "Failed to create profile for secondary dex " + str + SettingsStringUtil.DELIMITER + e.getMessage());
        }
    }

    private boolean isSecondaryDexFile(String str, String[] strArr) {
        for (String str2 : strArr) {
            if (FileUtils.contains(str2, str)) {
                return true;
            }
        }
        return false;
    }
}
