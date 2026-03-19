package com.android.server.pm.dex;

import android.os.Build;
import android.util.AtomicFile;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.FastPrintWriter;
import com.android.server.pm.AbstractStatsBase;
import com.android.server.pm.PackageManagerServiceUtils;
import dalvik.system.VMRuntime;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import libcore.io.IoUtils;

public class PackageDexUsage extends AbstractStatsBase<Void> {
    private static final String CODE_PATH_LINE_CHAR = "+";
    private static final String DEX_LINE_CHAR = "#";
    private static final String LOADING_PACKAGE_CHAR = "@";
    private static final int PACKAGE_DEX_USAGE_SUPPORTED_VERSION_1 = 1;
    private static final int PACKAGE_DEX_USAGE_SUPPORTED_VERSION_2 = 2;
    private static final int PACKAGE_DEX_USAGE_VERSION = 2;
    private static final String PACKAGE_DEX_USAGE_VERSION_HEADER = "PACKAGE_MANAGER__PACKAGE_DEX_USAGE__";
    private static final String SPLIT_CHAR = ",";
    private static final String TAG = "PackageDexUsage";
    static final String UNKNOWN_CLASS_LOADER_CONTEXT = "=UnknownClassLoaderContext=";
    static final String UNSUPPORTED_CLASS_LOADER_CONTEXT = "=UnsupportedClassLoaderContext=";
    static final String VARIABLE_CLASS_LOADER_CONTEXT = "=VariableClassLoaderContext=";

    @GuardedBy("mPackageUseInfoMap")
    private final Map<String, PackageUseInfo> mPackageUseInfoMap;

    public PackageDexUsage() {
        super("package-dex-usage.list", "PackageDexUsage_DiskWriter", false);
        this.mPackageUseInfoMap = new HashMap();
    }

    public boolean record(String str, String str2, int i, String str3, boolean z, boolean z2, String str4, String str5) {
        if (!PackageManagerServiceUtils.checkISA(str3)) {
            throw new IllegalArgumentException("loaderIsa " + str3 + " is unsupported");
        }
        if (str5 == null) {
            throw new IllegalArgumentException("Null classLoaderContext");
        }
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo packageUseInfo = this.mPackageUseInfoMap.get(str);
            boolean z3 = true;
            if (packageUseInfo == null) {
                PackageUseInfo packageUseInfo2 = new PackageUseInfo();
                if (!z2) {
                    DexUseInfo dexUseInfo = new DexUseInfo(z, i, str5, str3);
                    packageUseInfo2.mDexUseInfoMap.put(str2, dexUseInfo);
                    maybeAddLoadingPackage(str, str4, dexUseInfo.mLoadingPackages);
                } else {
                    packageUseInfo2.mergeCodePathUsedByOtherApps(str2, z, str, str4);
                }
                this.mPackageUseInfoMap.put(str, packageUseInfo2);
                return true;
            }
            if (!z2) {
                DexUseInfo dexUseInfo2 = new DexUseInfo(z, i, str5, str3);
                boolean zMaybeAddLoadingPackage = maybeAddLoadingPackage(str, str4, dexUseInfo2.mLoadingPackages);
                DexUseInfo dexUseInfo3 = (DexUseInfo) packageUseInfo.mDexUseInfoMap.get(str2);
                if (dexUseInfo3 == null) {
                    packageUseInfo.mDexUseInfoMap.put(str2, dexUseInfo2);
                    return true;
                }
                if (i == dexUseInfo3.mOwnerUserId) {
                    if (!dexUseInfo3.merge(dexUseInfo2) && !zMaybeAddLoadingPackage) {
                        z3 = false;
                    }
                    return z3;
                }
                throw new IllegalArgumentException("Trying to change ownerUserId for  dex path " + str2 + " from " + dexUseInfo3.mOwnerUserId + " to " + i);
            }
            return packageUseInfo.mergeCodePathUsedByOtherApps(str2, z, str, str4);
        }
    }

    public void read() {
        read((Void) null);
    }

    void maybeWriteAsync() {
        maybeWriteAsync(null);
    }

    void writeNow() {
        writeInternal((Void) null);
    }

    @Override
    protected void writeInternal(Void r3) {
        FileOutputStream fileOutputStreamStartWrite;
        AtomicFile file = getFile();
        try {
            fileOutputStreamStartWrite = file.startWrite();
        } catch (IOException e) {
            e = e;
            fileOutputStreamStartWrite = null;
        }
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStreamStartWrite);
            write(outputStreamWriter);
            outputStreamWriter.flush();
            file.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e2) {
            e = e2;
            if (fileOutputStreamStartWrite != null) {
                file.failWrite(fileOutputStreamStartWrite);
            }
            Slog.e(TAG, "Failed to write usage for dex files", e);
        }
    }

    void write(Writer writer) {
        Map<String, PackageUseInfo> mapClonePackageUseInfoMap = clonePackageUseInfoMap();
        FastPrintWriter fastPrintWriter = new FastPrintWriter(writer);
        fastPrintWriter.print(PACKAGE_DEX_USAGE_VERSION_HEADER);
        fastPrintWriter.println(2);
        for (Map.Entry<String, PackageUseInfo> entry : mapClonePackageUseInfoMap.entrySet()) {
            String key = entry.getKey();
            PackageUseInfo value = entry.getValue();
            fastPrintWriter.println(key);
            for (Map.Entry entry2 : value.mCodePathsUsedByOtherApps.entrySet()) {
                String str = (String) entry2.getKey();
                Set set = (Set) entry2.getValue();
                fastPrintWriter.println(CODE_PATH_LINE_CHAR + str);
                fastPrintWriter.println(LOADING_PACKAGE_CHAR + String.join(SPLIT_CHAR, set));
            }
            for (Map.Entry entry3 : value.mDexUseInfoMap.entrySet()) {
                String str2 = (String) entry3.getKey();
                DexUseInfo dexUseInfo = (DexUseInfo) entry3.getValue();
                fastPrintWriter.println(DEX_LINE_CHAR + str2);
                fastPrintWriter.print(String.join(SPLIT_CHAR, Integer.toString(dexUseInfo.mOwnerUserId), writeBoolean(dexUseInfo.mIsUsedByOtherApps)));
                Iterator it = dexUseInfo.mLoaderIsas.iterator();
                while (it.hasNext()) {
                    fastPrintWriter.print(SPLIT_CHAR + ((String) it.next()));
                }
                fastPrintWriter.println();
                fastPrintWriter.println(LOADING_PACKAGE_CHAR + String.join(SPLIT_CHAR, dexUseInfo.mLoadingPackages));
                fastPrintWriter.println(dexUseInfo.getClassLoaderContext());
            }
        }
        fastPrintWriter.flush();
    }

    @Override
    protected void readInternal(Void r4) throws Throwable {
        BufferedReader bufferedReader;
        BufferedReader bufferedReader2 = null;
        try {
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(getFile().openRead()));
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
            e = e2;
        }
        try {
            read((Reader) bufferedReader);
            IoUtils.closeQuietly(bufferedReader);
        } catch (FileNotFoundException e3) {
            bufferedReader2 = bufferedReader;
            IoUtils.closeQuietly(bufferedReader2);
        } catch (IOException e4) {
            e = e4;
            bufferedReader2 = bufferedReader;
            Slog.w(TAG, "Failed to parse package dex usage.", e);
            IoUtils.closeQuietly(bufferedReader2);
        } catch (Throwable th2) {
            th = th2;
            bufferedReader2 = bufferedReader;
            IoUtils.closeQuietly(bufferedReader2);
            throw th;
        }
    }

    void read(Reader reader) throws IOException {
        HashMap map = new HashMap();
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line = bufferedReader.readLine();
        if (line == null) {
            throw new IllegalStateException("No version line found.");
        }
        if (!line.startsWith(PACKAGE_DEX_USAGE_VERSION_HEADER)) {
            throw new IllegalStateException("Invalid version line: " + line);
        }
        int i = Integer.parseInt(line.substring(PACKAGE_DEX_USAGE_VERSION_HEADER.length()));
        if (!isSupportedVersion(i)) {
            throw new IllegalStateException("Unexpected version: " + i);
        }
        HashSet hashSet = new HashSet();
        char c = 0;
        for (String str : Build.SUPPORTED_ABIS) {
            hashSet.add(VMRuntime.getInstructionSet(str));
        }
        String str2 = null;
        PackageUseInfo packageUseInfo = null;
        while (true) {
            String line2 = bufferedReader.readLine();
            if (line2 != null) {
                if (line2.startsWith(DEX_LINE_CHAR)) {
                    if (str2 == null) {
                        throw new IllegalStateException("Malformed PackageDexUsage file. Expected package line before dex line.");
                    }
                    String strSubstring = line2.substring(DEX_LINE_CHAR.length());
                    String line3 = bufferedReader.readLine();
                    if (line3 == null) {
                        throw new IllegalStateException("Could not find dexUseInfo line");
                    }
                    String[] strArrSplit = line3.split(SPLIT_CHAR);
                    if (strArrSplit.length < 3) {
                        throw new IllegalStateException("Invalid PackageDexUsage line: " + line3);
                    }
                    Set<String> setMaybeReadLoadingPackages = maybeReadLoadingPackages(bufferedReader, i);
                    DexUseInfo dexUseInfo = new DexUseInfo(readBoolean(strArrSplit[1]), Integer.parseInt(strArrSplit[c]), maybeReadClassLoaderContext(bufferedReader, i), null);
                    dexUseInfo.mLoadingPackages.addAll(setMaybeReadLoadingPackages);
                    for (int i2 = 2; i2 < strArrSplit.length; i2++) {
                        String str3 = strArrSplit[i2];
                        if (!hashSet.contains(str3)) {
                            Slog.wtf(TAG, "Unsupported ISA when parsing PackageDexUsage: " + str3);
                        } else {
                            dexUseInfo.mLoaderIsas.add(strArrSplit[i2]);
                        }
                    }
                    if (hashSet.isEmpty()) {
                        Slog.wtf(TAG, "Ignore dexPath when parsing PackageDexUsage because of unsupported isas. dexPath=" + strSubstring);
                    } else {
                        packageUseInfo.mDexUseInfoMap.put(strSubstring, dexUseInfo);
                    }
                } else if (line2.startsWith(CODE_PATH_LINE_CHAR)) {
                    if (i < 2) {
                        throw new IllegalArgumentException("Unexpected code path line when parsing PackageDexUseData: " + line2);
                    }
                    packageUseInfo.mCodePathsUsedByOtherApps.put(line2.substring(CODE_PATH_LINE_CHAR.length()), maybeReadLoadingPackages(bufferedReader, i));
                } else {
                    if (i >= 2) {
                        packageUseInfo = new PackageUseInfo();
                        str2 = line2;
                        c = 0;
                    } else {
                        String[] strArrSplit2 = line2.split(SPLIT_CHAR);
                        if (strArrSplit2.length != 2) {
                            throw new IllegalStateException("Invalid PackageDexUsage line: " + line2);
                        }
                        c = 0;
                        String str4 = strArrSplit2[0];
                        PackageUseInfo packageUseInfo2 = new PackageUseInfo();
                        packageUseInfo2.mUsedByOtherAppsBeforeUpgrade = readBoolean(strArrSplit2[1]);
                        str2 = str4;
                        packageUseInfo = packageUseInfo2;
                    }
                    map.put(str2, packageUseInfo);
                }
                c = 0;
            } else {
                synchronized (this.mPackageUseInfoMap) {
                    this.mPackageUseInfoMap.clear();
                    this.mPackageUseInfoMap.putAll(map);
                }
                return;
            }
        }
    }

    private String maybeReadClassLoaderContext(BufferedReader bufferedReader, int i) throws IOException {
        String line;
        if (i >= 2) {
            line = bufferedReader.readLine();
            if (line == null) {
                throw new IllegalStateException("Could not find the classLoaderContext line.");
            }
        } else {
            line = null;
        }
        return line == null ? UNKNOWN_CLASS_LOADER_CONTEXT : line;
    }

    private Set<String> maybeReadLoadingPackages(BufferedReader bufferedReader, int i) throws IOException {
        if (i >= 2) {
            String line = bufferedReader.readLine();
            if (line == null) {
                throw new IllegalStateException("Could not find the loadingPackages line.");
            }
            if (line.length() == LOADING_PACKAGE_CHAR.length()) {
                return Collections.emptySet();
            }
            HashSet hashSet = new HashSet();
            Collections.addAll(hashSet, line.substring(LOADING_PACKAGE_CHAR.length()).split(SPLIT_CHAR));
            return hashSet;
        }
        return Collections.emptySet();
    }

    private boolean maybeAddLoadingPackage(String str, String str2, Set<String> set) {
        return !str.equals(str2) && set.add(str2);
    }

    private boolean isSupportedVersion(int i) {
        return i == 1 || i == 2;
    }

    void syncData(Map<String, Set<Integer>> map, Map<String, Set<String>> map2) {
        synchronized (this.mPackageUseInfoMap) {
            Iterator<Map.Entry<String, PackageUseInfo>> it = this.mPackageUseInfoMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, PackageUseInfo> next = it.next();
                String key = next.getKey();
                PackageUseInfo value = next.getValue();
                Set<Integer> set = map.get(key);
                if (set == null) {
                    it.remove();
                } else {
                    Iterator it2 = value.mDexUseInfoMap.entrySet().iterator();
                    while (it2.hasNext()) {
                        if (!set.contains(Integer.valueOf(((DexUseInfo) ((Map.Entry) it2.next()).getValue()).mOwnerUserId))) {
                            it2.remove();
                        }
                    }
                    Set<String> set2 = map2.get(key);
                    Iterator it3 = value.mCodePathsUsedByOtherApps.entrySet().iterator();
                    while (it3.hasNext()) {
                        if (!set2.contains(((Map.Entry) it3.next()).getKey())) {
                            it3.remove();
                        }
                    }
                    if (value.mUsedByOtherAppsBeforeUpgrade) {
                        Iterator<String> it4 = set2.iterator();
                        while (it4.hasNext()) {
                            value.mergeCodePathUsedByOtherApps(it4.next(), true, null, null);
                        }
                    } else if (!value.isAnyCodePathUsedByOtherApps() && value.mDexUseInfoMap.isEmpty()) {
                        it.remove();
                    }
                }
            }
        }
    }

    boolean clearUsedByOtherApps(String str) {
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo packageUseInfo = this.mPackageUseInfoMap.get(str);
            if (packageUseInfo == null) {
                return false;
            }
            return packageUseInfo.clearCodePathUsedByOtherApps();
        }
    }

    public boolean removePackage(String str) {
        boolean z;
        synchronized (this.mPackageUseInfoMap) {
            z = this.mPackageUseInfoMap.remove(str) != null;
        }
        return z;
    }

    boolean removeUserPackage(String str, int i) {
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo packageUseInfo = this.mPackageUseInfoMap.get(str);
            boolean z = false;
            if (packageUseInfo == null) {
                return false;
            }
            Iterator it = packageUseInfo.mDexUseInfoMap.entrySet().iterator();
            while (it.hasNext()) {
                if (((DexUseInfo) ((Map.Entry) it.next()).getValue()).mOwnerUserId == i) {
                    it.remove();
                    z = true;
                }
            }
            if (packageUseInfo.mDexUseInfoMap.isEmpty() && !packageUseInfo.isAnyCodePathUsedByOtherApps()) {
                this.mPackageUseInfoMap.remove(str);
                z = true;
            }
            return z;
        }
    }

    boolean removeDexFile(String str, String str2, int i) {
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo packageUseInfo = this.mPackageUseInfoMap.get(str);
            if (packageUseInfo == null) {
                return false;
            }
            return removeDexFile(packageUseInfo, str2, i);
        }
    }

    private boolean removeDexFile(PackageUseInfo packageUseInfo, String str, int i) {
        DexUseInfo dexUseInfo = (DexUseInfo) packageUseInfo.mDexUseInfoMap.get(str);
        if (dexUseInfo == null || dexUseInfo.mOwnerUserId != i) {
            return false;
        }
        packageUseInfo.mDexUseInfoMap.remove(str);
        return true;
    }

    PackageUseInfo getPackageUseInfo(String str) {
        PackageUseInfo packageUseInfo;
        synchronized (this.mPackageUseInfoMap) {
            PackageUseInfo packageUseInfo2 = this.mPackageUseInfoMap.get(str);
            packageUseInfo = packageUseInfo2 == null ? null : new PackageUseInfo(packageUseInfo2);
        }
        return packageUseInfo;
    }

    Set<String> getAllPackagesWithSecondaryDexFiles() {
        HashSet hashSet = new HashSet();
        synchronized (this.mPackageUseInfoMap) {
            for (Map.Entry<String, PackageUseInfo> entry : this.mPackageUseInfoMap.entrySet()) {
                if (!entry.getValue().mDexUseInfoMap.isEmpty()) {
                    hashSet.add(entry.getKey());
                }
            }
        }
        return hashSet;
    }

    public void clear() {
        synchronized (this.mPackageUseInfoMap) {
            this.mPackageUseInfoMap.clear();
        }
    }

    private Map<String, PackageUseInfo> clonePackageUseInfoMap() {
        HashMap map = new HashMap();
        synchronized (this.mPackageUseInfoMap) {
            for (Map.Entry<String, PackageUseInfo> entry : this.mPackageUseInfoMap.entrySet()) {
                map.put(entry.getKey(), new PackageUseInfo(entry.getValue()));
            }
        }
        return map;
    }

    private String writeBoolean(boolean z) {
        return z ? "1" : "0";
    }

    private boolean readBoolean(String str) {
        if ("0".equals(str)) {
            return false;
        }
        if ("1".equals(str)) {
            return true;
        }
        throw new IllegalArgumentException("Unknown bool encoding: " + str);
    }

    public String dump() {
        StringWriter stringWriter = new StringWriter();
        write(stringWriter);
        return stringWriter.toString();
    }

    public static class PackageUseInfo {
        private final Map<String, Set<String>> mCodePathsUsedByOtherApps;
        private final Map<String, DexUseInfo> mDexUseInfoMap;
        private boolean mUsedByOtherAppsBeforeUpgrade;

        public PackageUseInfo() {
            this.mCodePathsUsedByOtherApps = new HashMap();
            this.mDexUseInfoMap = new HashMap();
        }

        public PackageUseInfo(PackageUseInfo packageUseInfo) {
            this.mCodePathsUsedByOtherApps = new HashMap();
            for (Map.Entry<String, Set<String>> entry : packageUseInfo.mCodePathsUsedByOtherApps.entrySet()) {
                this.mCodePathsUsedByOtherApps.put(entry.getKey(), new HashSet(entry.getValue()));
            }
            this.mDexUseInfoMap = new HashMap();
            for (Map.Entry<String, DexUseInfo> entry2 : packageUseInfo.mDexUseInfoMap.entrySet()) {
                this.mDexUseInfoMap.put(entry2.getKey(), new DexUseInfo(entry2.getValue()));
            }
        }

        private boolean mergeCodePathUsedByOtherApps(String str, boolean z, String str2, String str3) {
            boolean z2;
            if (!z) {
                return false;
            }
            Set<String> hashSet = this.mCodePathsUsedByOtherApps.get(str);
            if (hashSet != null) {
                z2 = false;
            } else {
                hashSet = new HashSet<>();
                this.mCodePathsUsedByOtherApps.put(str, hashSet);
                z2 = true;
            }
            boolean z3 = (str3 == null || str3.equals(str2) || !hashSet.add(str3)) ? false : true;
            if (!z2 && !z3) {
                return false;
            }
            return true;
        }

        public boolean isUsedByOtherApps(String str) {
            return this.mCodePathsUsedByOtherApps.containsKey(str);
        }

        public Map<String, DexUseInfo> getDexUseInfoMap() {
            return this.mDexUseInfoMap;
        }

        public Set<String> getLoadingPackages(String str) {
            return this.mCodePathsUsedByOtherApps.getOrDefault(str, null);
        }

        public boolean isAnyCodePathUsedByOtherApps() {
            return !this.mCodePathsUsedByOtherApps.isEmpty();
        }

        boolean clearCodePathUsedByOtherApps() {
            this.mUsedByOtherAppsBeforeUpgrade = true;
            if (this.mCodePathsUsedByOtherApps.isEmpty()) {
                return false;
            }
            this.mCodePathsUsedByOtherApps.clear();
            return true;
        }
    }

    public static class DexUseInfo {
        private String mClassLoaderContext;
        private boolean mIsUsedByOtherApps;
        private final Set<String> mLoaderIsas;
        private final Set<String> mLoadingPackages;
        private final int mOwnerUserId;

        public DexUseInfo(boolean z, int i, String str, String str2) {
            this.mIsUsedByOtherApps = z;
            this.mOwnerUserId = i;
            this.mClassLoaderContext = str;
            this.mLoaderIsas = new HashSet();
            if (str2 != null) {
                this.mLoaderIsas.add(str2);
            }
            this.mLoadingPackages = new HashSet();
        }

        public DexUseInfo(DexUseInfo dexUseInfo) {
            this.mIsUsedByOtherApps = dexUseInfo.mIsUsedByOtherApps;
            this.mOwnerUserId = dexUseInfo.mOwnerUserId;
            this.mClassLoaderContext = dexUseInfo.mClassLoaderContext;
            this.mLoaderIsas = new HashSet(dexUseInfo.mLoaderIsas);
            this.mLoadingPackages = new HashSet(dexUseInfo.mLoadingPackages);
        }

        private boolean merge(DexUseInfo dexUseInfo) {
            boolean z = this.mIsUsedByOtherApps;
            this.mIsUsedByOtherApps = this.mIsUsedByOtherApps || dexUseInfo.mIsUsedByOtherApps;
            boolean zAddAll = this.mLoaderIsas.addAll(dexUseInfo.mLoaderIsas);
            boolean zAddAll2 = this.mLoadingPackages.addAll(dexUseInfo.mLoadingPackages);
            String str = this.mClassLoaderContext;
            if (PackageDexUsage.UNKNOWN_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext)) {
                this.mClassLoaderContext = dexUseInfo.mClassLoaderContext;
            } else if (PackageDexUsage.UNSUPPORTED_CLASS_LOADER_CONTEXT.equals(dexUseInfo.mClassLoaderContext)) {
                this.mClassLoaderContext = PackageDexUsage.UNSUPPORTED_CLASS_LOADER_CONTEXT;
            } else if (!PackageDexUsage.UNSUPPORTED_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext) && !Objects.equals(this.mClassLoaderContext, dexUseInfo.mClassLoaderContext)) {
                this.mClassLoaderContext = PackageDexUsage.VARIABLE_CLASS_LOADER_CONTEXT;
            }
            return zAddAll || z != this.mIsUsedByOtherApps || zAddAll2 || !Objects.equals(str, this.mClassLoaderContext);
        }

        public boolean isUsedByOtherApps() {
            return this.mIsUsedByOtherApps;
        }

        public int getOwnerUserId() {
            return this.mOwnerUserId;
        }

        public Set<String> getLoaderIsas() {
            return this.mLoaderIsas;
        }

        public Set<String> getLoadingPackages() {
            return this.mLoadingPackages;
        }

        public String getClassLoaderContext() {
            return this.mClassLoaderContext;
        }

        public boolean isUnsupportedClassLoaderContext() {
            return PackageDexUsage.UNSUPPORTED_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext);
        }

        public boolean isUnknownClassLoaderContext() {
            return PackageDexUsage.UNKNOWN_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext);
        }

        public boolean isVariableClassLoaderContext() {
            return PackageDexUsage.VARIABLE_CLASS_LOADER_CONTEXT.equals(this.mClassLoaderContext);
        }
    }
}
