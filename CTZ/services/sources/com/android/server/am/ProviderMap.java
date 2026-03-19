package com.android.server.am;

import android.content.ComponentName;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.SparseArray;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.utils.PriorityDump;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ProviderMap {
    private static final boolean DBG = false;
    private static final String TAG = "ProviderMap";
    private final ActivityManagerService mAm;
    private final HashMap<String, ContentProviderRecord> mSingletonByName = new HashMap<>();
    private final HashMap<ComponentName, ContentProviderRecord> mSingletonByClass = new HashMap<>();
    private final SparseArray<HashMap<String, ContentProviderRecord>> mProvidersByNamePerUser = new SparseArray<>();
    private final SparseArray<HashMap<ComponentName, ContentProviderRecord>> mProvidersByClassPerUser = new SparseArray<>();

    ProviderMap(ActivityManagerService activityManagerService) {
        this.mAm = activityManagerService;
    }

    ContentProviderRecord getProviderByName(String str) {
        return getProviderByName(str, -1);
    }

    ContentProviderRecord getProviderByName(String str, int i) {
        ContentProviderRecord contentProviderRecord = this.mSingletonByName.get(str);
        if (contentProviderRecord != null) {
            return contentProviderRecord;
        }
        return getProvidersByName(i).get(str);
    }

    ContentProviderRecord getProviderByClass(ComponentName componentName) {
        return getProviderByClass(componentName, -1);
    }

    ContentProviderRecord getProviderByClass(ComponentName componentName, int i) {
        ContentProviderRecord contentProviderRecord = this.mSingletonByClass.get(componentName);
        if (contentProviderRecord != null) {
            return contentProviderRecord;
        }
        return getProvidersByClass(i).get(componentName);
    }

    void putProviderByName(String str, ContentProviderRecord contentProviderRecord) {
        if (contentProviderRecord.singleton) {
            this.mSingletonByName.put(str, contentProviderRecord);
        } else {
            getProvidersByName(UserHandle.getUserId(contentProviderRecord.appInfo.uid)).put(str, contentProviderRecord);
        }
    }

    void putProviderByClass(ComponentName componentName, ContentProviderRecord contentProviderRecord) {
        if (contentProviderRecord.singleton) {
            this.mSingletonByClass.put(componentName, contentProviderRecord);
        } else {
            getProvidersByClass(UserHandle.getUserId(contentProviderRecord.appInfo.uid)).put(componentName, contentProviderRecord);
        }
    }

    void removeProviderByName(String str, int i) {
        if (this.mSingletonByName.containsKey(str)) {
            this.mSingletonByName.remove(str);
            return;
        }
        if (i < 0) {
            throw new IllegalArgumentException("Bad user " + i);
        }
        HashMap<String, ContentProviderRecord> providersByName = getProvidersByName(i);
        providersByName.remove(str);
        if (providersByName.size() == 0) {
            this.mProvidersByNamePerUser.remove(i);
        }
    }

    void removeProviderByClass(ComponentName componentName, int i) {
        if (this.mSingletonByClass.containsKey(componentName)) {
            this.mSingletonByClass.remove(componentName);
            return;
        }
        if (i < 0) {
            throw new IllegalArgumentException("Bad user " + i);
        }
        HashMap<ComponentName, ContentProviderRecord> providersByClass = getProvidersByClass(i);
        providersByClass.remove(componentName);
        if (providersByClass.size() == 0) {
            this.mProvidersByClassPerUser.remove(i);
        }
    }

    private HashMap<String, ContentProviderRecord> getProvidersByName(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Bad user " + i);
        }
        HashMap<String, ContentProviderRecord> map = this.mProvidersByNamePerUser.get(i);
        if (map == null) {
            HashMap<String, ContentProviderRecord> map2 = new HashMap<>();
            this.mProvidersByNamePerUser.put(i, map2);
            return map2;
        }
        return map;
    }

    HashMap<ComponentName, ContentProviderRecord> getProvidersByClass(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Bad user " + i);
        }
        HashMap<ComponentName, ContentProviderRecord> map = this.mProvidersByClassPerUser.get(i);
        if (map == null) {
            HashMap<ComponentName, ContentProviderRecord> map2 = new HashMap<>();
            this.mProvidersByClassPerUser.put(i, map2);
            return map2;
        }
        return map;
    }

    private boolean collectPackageProvidersLocked(String str, Set<String> set, boolean z, boolean z2, HashMap<ComponentName, ContentProviderRecord> map, ArrayList<ContentProviderRecord> arrayList) {
        boolean z3 = false;
        for (ContentProviderRecord contentProviderRecord : map.values()) {
            if ((str == null || (contentProviderRecord.info.packageName.equals(str) && (set == null || set.contains(contentProviderRecord.name.getClassName())))) && (contentProviderRecord.proc == null || z2 || !contentProviderRecord.proc.persistent)) {
                if (!z) {
                    return true;
                }
                arrayList.add(contentProviderRecord);
                z3 = true;
            }
        }
        return z3;
    }

    boolean collectPackageProvidersLocked(String str, Set<String> set, boolean z, boolean z2, int i, ArrayList<ContentProviderRecord> arrayList) {
        boolean zCollectPackageProvidersLocked;
        if (i == -1 || i == 0) {
            zCollectPackageProvidersLocked = collectPackageProvidersLocked(str, set, z, z2, this.mSingletonByClass, arrayList);
        } else {
            zCollectPackageProvidersLocked = false;
        }
        if (!z && zCollectPackageProvidersLocked) {
            return true;
        }
        if (i == -1) {
            for (int i2 = 0; i2 < this.mProvidersByClassPerUser.size(); i2++) {
                if (collectPackageProvidersLocked(str, set, z, z2, this.mProvidersByClassPerUser.valueAt(i2), arrayList)) {
                    if (!z) {
                        return true;
                    }
                    zCollectPackageProvidersLocked = true;
                }
            }
            return zCollectPackageProvidersLocked;
        }
        HashMap<ComponentName, ContentProviderRecord> providersByClass = getProvidersByClass(i);
        if (providersByClass != null) {
            return zCollectPackageProvidersLocked | collectPackageProvidersLocked(str, set, z, z2, providersByClass, arrayList);
        }
        return zCollectPackageProvidersLocked;
    }

    private boolean dumpProvidersByClassLocked(PrintWriter printWriter, boolean z, String str, String str2, boolean z2, HashMap<ComponentName, ContentProviderRecord> map) {
        Iterator<Map.Entry<ComponentName, ContentProviderRecord>> it = map.entrySet().iterator();
        String str3 = str2;
        boolean z3 = false;
        while (it.hasNext()) {
            ContentProviderRecord value = it.next().getValue();
            if (str == null || str.equals(value.appInfo.packageName)) {
                if (z2) {
                    printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    z2 = false;
                }
                if (str3 != null) {
                    printWriter.println(str3);
                    str3 = null;
                }
                z3 = true;
                printWriter.print("  * ");
                printWriter.println(value);
                value.dump(printWriter, "    ", z);
            }
        }
        return z3;
    }

    private boolean dumpProvidersByNameLocked(PrintWriter printWriter, String str, String str2, boolean z, HashMap<String, ContentProviderRecord> map) {
        String str3 = str2;
        boolean z2 = false;
        for (Map.Entry<String, ContentProviderRecord> entry : map.entrySet()) {
            ContentProviderRecord value = entry.getValue();
            if (str == null || str.equals(value.appInfo.packageName)) {
                if (z) {
                    printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    z = false;
                }
                if (str3 != null) {
                    printWriter.println(str3);
                    str3 = null;
                }
                z2 = true;
                printWriter.print("  ");
                printWriter.print(entry.getKey());
                printWriter.print(": ");
                printWriter.println(value.toShortString());
            }
        }
        return z2;
    }

    boolean dumpProvidersLocked(PrintWriter printWriter, boolean z, String str) {
        boolean zDumpProvidersByClassLocked;
        if (this.mSingletonByClass.size() > 0) {
            zDumpProvidersByClassLocked = dumpProvidersByClassLocked(printWriter, z, str, "  Published single-user content providers (by class):", false, this.mSingletonByClass) | false;
        } else {
            zDumpProvidersByClassLocked = false;
        }
        boolean zDumpProvidersByClassLocked2 = zDumpProvidersByClassLocked;
        for (int i = 0; i < this.mProvidersByClassPerUser.size(); i++) {
            zDumpProvidersByClassLocked2 |= dumpProvidersByClassLocked(printWriter, z, str, "  Published user " + this.mProvidersByClassPerUser.keyAt(i) + " content providers (by class):", zDumpProvidersByClassLocked2, this.mProvidersByClassPerUser.valueAt(i));
        }
        if (z) {
            boolean zDumpProvidersByNameLocked = dumpProvidersByNameLocked(printWriter, str, "  Single-user authority to provider mappings:", zDumpProvidersByClassLocked2, this.mSingletonByName) | zDumpProvidersByClassLocked2;
            for (int i2 = 0; i2 < this.mProvidersByNamePerUser.size(); i2++) {
                zDumpProvidersByNameLocked |= dumpProvidersByNameLocked(printWriter, str, "  User " + this.mProvidersByNamePerUser.keyAt(i2) + " authority to provider mappings:", zDumpProvidersByNameLocked, this.mProvidersByNamePerUser.valueAt(i2));
            }
            return zDumpProvidersByNameLocked;
        }
        return zDumpProvidersByClassLocked2;
    }

    private ArrayList<ContentProviderRecord> getProvidersForName(String str) {
        ArrayList arrayList = new ArrayList();
        ArrayList<ContentProviderRecord> arrayList2 = new ArrayList<>();
        Predicate predicateFilterRecord = DumpUtils.filterRecord(str);
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                arrayList.addAll(this.mSingletonByClass.values());
                for (int i = 0; i < this.mProvidersByClassPerUser.size(); i++) {
                    arrayList.addAll(this.mProvidersByClassPerUser.valueAt(i).values());
                }
                CollectionUtils.addIf(arrayList, arrayList2, predicateFilterRecord);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        arrayList2.sort(Comparator.comparing(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((ContentProviderRecord) obj).getComponentName();
            }
        }));
        return arrayList2;
    }

    protected boolean dumpProvider(FileDescriptor fileDescriptor, PrintWriter printWriter, String str, String[] strArr, int i, boolean z) {
        ArrayList<ContentProviderRecord> providersForName = getProvidersForName(str);
        boolean z2 = false;
        if (providersForName.size() <= 0) {
            return false;
        }
        int i2 = 0;
        while (i2 < providersForName.size()) {
            if (z2) {
                printWriter.println();
            }
            dumpProvider(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, fileDescriptor, printWriter, providersForName.get(i2), strArr, z);
            i2++;
            z2 = true;
        }
        return true;
    }

    private void dumpProvider(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, ContentProviderRecord contentProviderRecord, String[] strArr, boolean z) {
        for (String str2 : strArr) {
            if (!z && str2.contains(PriorityDump.PROTO_ARG)) {
                if (contentProviderRecord.proc != null && contentProviderRecord.proc.thread != null) {
                    dumpToTransferPipe(null, fileDescriptor, printWriter, contentProviderRecord, strArr);
                    return;
                }
                return;
            }
        }
        String str3 = str + "  ";
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                printWriter.print(str);
                printWriter.print("PROVIDER ");
                printWriter.print(contentProviderRecord);
                printWriter.print(" pid=");
                if (contentProviderRecord.proc != null) {
                    printWriter.println(contentProviderRecord.proc.pid);
                } else {
                    printWriter.println("(not running)");
                }
                if (z) {
                    contentProviderRecord.dump(printWriter, str3, true);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        if (contentProviderRecord.proc != null && contentProviderRecord.proc.thread != null) {
            printWriter.println("    Client:");
            printWriter.flush();
            dumpToTransferPipe("      ", fileDescriptor, printWriter, contentProviderRecord, strArr);
        }
    }

    protected boolean dumpProviderProto(FileDescriptor fileDescriptor, PrintWriter printWriter, String str, String[] strArr) {
        String[] strArr2 = (String[]) Arrays.copyOf(strArr, strArr.length + 1);
        strArr2[strArr.length] = PriorityDump.PROTO_ARG;
        ArrayList<ContentProviderRecord> providersForName = getProvidersForName(str);
        if (providersForName.size() <= 0) {
            return false;
        }
        for (int i = 0; i < providersForName.size(); i++) {
            ContentProviderRecord contentProviderRecord = providersForName.get(i);
            if (contentProviderRecord.proc != null && contentProviderRecord.proc.thread != null) {
                dumpToTransferPipe(null, fileDescriptor, printWriter, contentProviderRecord, strArr2);
                return true;
            }
        }
        return false;
    }

    private void dumpToTransferPipe(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, ContentProviderRecord contentProviderRecord, String[] strArr) {
        try {
            TransferPipe transferPipe = new TransferPipe();
            try {
                contentProviderRecord.proc.thread.dumpProvider(transferPipe.getWriteFd(), contentProviderRecord.provider.asBinder(), strArr);
                transferPipe.setBufferPrefix(str);
                transferPipe.go(fileDescriptor, 2000L);
                transferPipe.kill();
            } catch (Throwable th) {
                transferPipe.kill();
                throw th;
            }
        } catch (RemoteException e) {
            printWriter.println("      Got a RemoteException while dumping the service");
        } catch (IOException e2) {
            printWriter.println("      Failure while dumping the provider: " + e2);
        }
    }
}
