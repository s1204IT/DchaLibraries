package com.android.server.am;

import android.content.ComponentName;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.SparseArray;
import com.android.internal.os.TransferPipe;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ProviderMap {
    private static final boolean DBG = false;
    private static final String TAG = "ProviderMap";
    private final ActivityManagerService mAm;
    private final HashMap<String, ContentProviderRecord> mSingletonByName = new HashMap<>();
    private final HashMap<ComponentName, ContentProviderRecord> mSingletonByClass = new HashMap<>();
    private final SparseArray<HashMap<String, ContentProviderRecord>> mProvidersByNamePerUser = new SparseArray<>();
    private final SparseArray<HashMap<ComponentName, ContentProviderRecord>> mProvidersByClassPerUser = new SparseArray<>();

    ProviderMap(ActivityManagerService am) {
        this.mAm = am;
    }

    ContentProviderRecord getProviderByName(String name) {
        return getProviderByName(name, -1);
    }

    ContentProviderRecord getProviderByName(String name, int userId) {
        ContentProviderRecord record = this.mSingletonByName.get(name);
        if (record != null) {
            return record;
        }
        return getProvidersByName(userId).get(name);
    }

    ContentProviderRecord getProviderByClass(ComponentName name) {
        return getProviderByClass(name, -1);
    }

    ContentProviderRecord getProviderByClass(ComponentName name, int userId) {
        ContentProviderRecord record = this.mSingletonByClass.get(name);
        if (record != null) {
            return record;
        }
        return getProvidersByClass(userId).get(name);
    }

    void putProviderByName(String name, ContentProviderRecord record) {
        if (record.singleton) {
            this.mSingletonByName.put(name, record);
        } else {
            int userId = UserHandle.getUserId(record.appInfo.uid);
            getProvidersByName(userId).put(name, record);
        }
    }

    void putProviderByClass(ComponentName name, ContentProviderRecord record) {
        if (record.singleton) {
            this.mSingletonByClass.put(name, record);
        } else {
            int userId = UserHandle.getUserId(record.appInfo.uid);
            getProvidersByClass(userId).put(name, record);
        }
    }

    void removeProviderByName(String name, int userId) {
        if (this.mSingletonByName.containsKey(name)) {
            this.mSingletonByName.remove(name);
            return;
        }
        if (userId < 0) {
            throw new IllegalArgumentException("Bad user " + userId);
        }
        HashMap<String, ContentProviderRecord> map = getProvidersByName(userId);
        map.remove(name);
        if (map.size() != 0) {
            return;
        }
        this.mProvidersByNamePerUser.remove(userId);
    }

    void removeProviderByClass(ComponentName name, int userId) {
        if (this.mSingletonByClass.containsKey(name)) {
            this.mSingletonByClass.remove(name);
            return;
        }
        if (userId < 0) {
            throw new IllegalArgumentException("Bad user " + userId);
        }
        HashMap<ComponentName, ContentProviderRecord> map = getProvidersByClass(userId);
        map.remove(name);
        if (map.size() != 0) {
            return;
        }
        this.mProvidersByClassPerUser.remove(userId);
    }

    private HashMap<String, ContentProviderRecord> getProvidersByName(int userId) {
        if (userId < 0) {
            throw new IllegalArgumentException("Bad user " + userId);
        }
        HashMap<String, ContentProviderRecord> map = this.mProvidersByNamePerUser.get(userId);
        if (map == null) {
            HashMap<String, ContentProviderRecord> newMap = new HashMap<>();
            this.mProvidersByNamePerUser.put(userId, newMap);
            return newMap;
        }
        return map;
    }

    HashMap<ComponentName, ContentProviderRecord> getProvidersByClass(int userId) {
        if (userId < 0) {
            throw new IllegalArgumentException("Bad user " + userId);
        }
        HashMap<ComponentName, ContentProviderRecord> map = this.mProvidersByClassPerUser.get(userId);
        if (map == null) {
            HashMap<ComponentName, ContentProviderRecord> newMap = new HashMap<>();
            this.mProvidersByClassPerUser.put(userId, newMap);
            return newMap;
        }
        return map;
    }

    private boolean collectPackageProvidersLocked(String packageName, Set<String> filterByClasses, boolean doit, boolean evenPersistent, HashMap<ComponentName, ContentProviderRecord> providers, ArrayList<ContentProviderRecord> result) {
        boolean sameComponent;
        boolean didSomething = false;
        for (ContentProviderRecord provider : providers.values()) {
            if (packageName == null) {
                sameComponent = true;
            } else if (!provider.info.packageName.equals(packageName)) {
                sameComponent = false;
            } else {
                sameComponent = filterByClasses != null ? filterByClasses.contains(provider.name.getClassName()) : true;
            }
            if (sameComponent && (provider.proc == null || evenPersistent || !provider.proc.persistent)) {
                if (!doit) {
                    return true;
                }
                didSomething = true;
                result.add(provider);
            }
        }
        return didSomething;
    }

    boolean collectPackageProvidersLocked(String packageName, Set<String> filterByClasses, boolean doit, boolean evenPersistent, int userId, ArrayList<ContentProviderRecord> result) {
        boolean didSomething = false;
        if (userId == -1 || userId == 0) {
            didSomething = collectPackageProvidersLocked(packageName, filterByClasses, doit, evenPersistent, this.mSingletonByClass, result);
        }
        if (!doit && didSomething) {
            return true;
        }
        if (userId == -1) {
            for (int i = 0; i < this.mProvidersByClassPerUser.size(); i++) {
                if (collectPackageProvidersLocked(packageName, filterByClasses, doit, evenPersistent, this.mProvidersByClassPerUser.valueAt(i), result)) {
                    if (!doit) {
                        return true;
                    }
                    didSomething = true;
                }
            }
            return didSomething;
        }
        HashMap<ComponentName, ContentProviderRecord> items = getProvidersByClass(userId);
        if (items != null) {
            return didSomething | collectPackageProvidersLocked(packageName, filterByClasses, doit, evenPersistent, items, result);
        }
        return didSomething;
    }

    private boolean dumpProvidersByClassLocked(PrintWriter pw, boolean dumpAll, String dumpPackage, String header, boolean needSep, HashMap<ComponentName, ContentProviderRecord> map) {
        boolean written = false;
        for (Map.Entry<ComponentName, ContentProviderRecord> e : map.entrySet()) {
            ContentProviderRecord r = e.getValue();
            if (dumpPackage == null || dumpPackage.equals(r.appInfo.packageName)) {
                if (needSep) {
                    pw.println("");
                    needSep = false;
                }
                if (header != null) {
                    pw.println(header);
                    header = null;
                }
                written = true;
                pw.print("  * ");
                pw.println(r);
                r.dump(pw, "    ", dumpAll);
            }
        }
        return written;
    }

    private boolean dumpProvidersByNameLocked(PrintWriter pw, String dumpPackage, String header, boolean needSep, HashMap<String, ContentProviderRecord> map) {
        boolean written = false;
        for (Map.Entry<String, ContentProviderRecord> e : map.entrySet()) {
            ContentProviderRecord r = e.getValue();
            if (dumpPackage == null || dumpPackage.equals(r.appInfo.packageName)) {
                if (needSep) {
                    pw.println("");
                    needSep = false;
                }
                if (header != null) {
                    pw.println(header);
                    header = null;
                }
                written = true;
                pw.print("  ");
                pw.print(e.getKey());
                pw.print(": ");
                pw.println(r.toShortString());
            }
        }
        return written;
    }

    boolean dumpProvidersLocked(PrintWriter pw, boolean dumpAll, String dumpPackage) {
        boolean needSep = false;
        if (this.mSingletonByClass.size() > 0) {
            needSep = dumpProvidersByClassLocked(pw, dumpAll, dumpPackage, "  Published single-user content providers (by class):", false, this.mSingletonByClass);
        }
        for (int i = 0; i < this.mProvidersByClassPerUser.size(); i++) {
            HashMap<ComponentName, ContentProviderRecord> map = this.mProvidersByClassPerUser.valueAt(i);
            needSep |= dumpProvidersByClassLocked(pw, dumpAll, dumpPackage, "  Published user " + this.mProvidersByClassPerUser.keyAt(i) + " content providers (by class):", needSep, map);
        }
        if (dumpAll) {
            needSep |= dumpProvidersByNameLocked(pw, dumpPackage, "  Single-user authority to provider mappings:", needSep, this.mSingletonByName);
            for (int i2 = 0; i2 < this.mProvidersByNamePerUser.size(); i2++) {
                needSep |= dumpProvidersByNameLocked(pw, dumpPackage, "  User " + this.mProvidersByNamePerUser.keyAt(i2) + " authority to provider mappings:", needSep, this.mProvidersByNamePerUser.valueAt(i2));
            }
        }
        return needSep;
    }

    protected boolean dumpProvider(FileDescriptor fd, PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        ArrayList<ContentProviderRecord> allProviders = new ArrayList<>();
        ArrayList<ContentProviderRecord> providers = new ArrayList<>();
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                allProviders.addAll(this.mSingletonByClass.values());
                for (int i = 0; i < this.mProvidersByClassPerUser.size(); i++) {
                    allProviders.addAll(this.mProvidersByClassPerUser.valueAt(i).values());
                }
                if ("all".equals(name)) {
                    providers.addAll(allProviders);
                } else {
                    ComponentName componentName = name != null ? ComponentName.unflattenFromString(name) : null;
                    int objectId = 0;
                    if (componentName == null) {
                        try {
                            objectId = Integer.parseInt(name, 16);
                            name = null;
                            componentName = null;
                        } catch (RuntimeException e) {
                        }
                    }
                    for (int i2 = 0; i2 < allProviders.size(); i2++) {
                        ContentProviderRecord r1 = allProviders.get(i2);
                        if (componentName != null) {
                            if (r1.name.equals(componentName)) {
                                providers.add(r1);
                            }
                        } else if (name != null) {
                            if (r1.name.flattenToString().contains(name)) {
                                providers.add(r1);
                            }
                        } else if (System.identityHashCode(r1) == objectId) {
                            providers.add(r1);
                        }
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        if (providers.size() <= 0) {
            return false;
        }
        boolean needSep = false;
        for (int i3 = 0; i3 < providers.size(); i3++) {
            if (needSep) {
                pw.println();
            }
            needSep = true;
            dumpProvider("", fd, pw, providers.get(i3), args, dumpAll);
        }
        return true;
    }

    private void dumpProvider(String prefix, FileDescriptor fd, PrintWriter pw, ContentProviderRecord r, String[] args, boolean dumpAll) {
        String innerPrefix = prefix + "  ";
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                pw.print(prefix);
                pw.print("PROVIDER ");
                pw.print(r);
                pw.print(" pid=");
                if (r.proc != null) {
                    pw.println(r.proc.pid);
                } else {
                    pw.println("(not running)");
                }
                if (dumpAll) {
                    r.dump(pw, innerPrefix, true);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        if (r.proc == null || r.proc.thread == null) {
            return;
        }
        pw.println("    Client:");
        pw.flush();
        try {
            TransferPipe tp = new TransferPipe();
            try {
                r.proc.thread.dumpProvider(tp.getWriteFd().getFileDescriptor(), r.provider.asBinder(), args);
                tp.setBufferPrefix("      ");
                tp.go(fd, 2000L);
            } finally {
                tp.kill();
            }
        } catch (RemoteException e) {
            pw.println("      Got a RemoteException while dumping the service");
        } catch (IOException ex) {
            pw.println("      Failure while dumping the provider: " + ex);
        }
    }
}
