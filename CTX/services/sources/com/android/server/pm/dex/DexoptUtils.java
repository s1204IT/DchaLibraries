package com.android.server.pm.dex;

import android.content.pm.ApplicationInfo;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.ClassLoaderFactory;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.PackageDexOptimizer;
import java.io.File;
import java.util.List;

public final class DexoptUtils {
    private static final String TAG = "DexoptUtils";

    private DexoptUtils() {
    }

    public static String[] getClassLoaderContexts(ApplicationInfo applicationInfo, String[] strArr, boolean[] zArr) {
        String strEncodeClasspath = encodeClasspath(strArr);
        String strEncodeClassLoader = encodeClassLoader(strEncodeClasspath, applicationInfo.classLoaderName);
        int i = 1;
        if (applicationInfo.getSplitCodePaths() == null) {
            return new String[]{strEncodeClassLoader};
        }
        String[] splitRelativeCodePaths = getSplitRelativeCodePaths(applicationInfo);
        String strEncodeClasspath2 = encodeClasspath(strEncodeClasspath, new File(applicationInfo.getBaseCodePath()).getName());
        String[] strArr2 = new String[splitRelativeCodePaths.length + 1];
        if (!zArr[0]) {
            strEncodeClassLoader = null;
        }
        strArr2[0] = strEncodeClassLoader;
        if (!applicationInfo.requestsIsolatedSplitLoading() || applicationInfo.splitDependencies == null) {
            while (i < strArr2.length) {
                strArr2[i] = zArr[i] ? encodeClassLoader(strEncodeClasspath2, applicationInfo.classLoaderName) : null;
                strEncodeClasspath2 = encodeClasspath(strEncodeClasspath2, splitRelativeCodePaths[i - 1]);
                i++;
            }
        } else {
            String[] strArr3 = new String[splitRelativeCodePaths.length];
            for (int i2 = 0; i2 < splitRelativeCodePaths.length; i2++) {
                strArr3[i2] = encodeClassLoader(splitRelativeCodePaths[i2], applicationInfo.splitClassLoaderNames[i2]);
            }
            String strEncodeClassLoader2 = encodeClassLoader(strEncodeClasspath2, applicationInfo.classLoaderName);
            SparseArray sparseArray = applicationInfo.splitDependencies;
            for (int i3 = 1; i3 < sparseArray.size(); i3++) {
                int iKeyAt = sparseArray.keyAt(i3);
                if (zArr[iKeyAt]) {
                    getParentDependencies(iKeyAt, strArr3, sparseArray, strArr2, strEncodeClassLoader2);
                }
            }
            while (i < strArr2.length) {
                String strEncodeClassLoader3 = encodeClassLoader(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, applicationInfo.splitClassLoaderNames[i - 1]);
                if (zArr[i]) {
                    if (strArr2[i] != null) {
                        strEncodeClassLoader3 = encodeClassLoaderChain(strEncodeClassLoader3, strArr2[i]);
                    }
                    strArr2[i] = strEncodeClassLoader3;
                } else {
                    strArr2[i] = null;
                }
                i++;
            }
        }
        return strArr2;
    }

    private static String getParentDependencies(int i, String[] strArr, SparseArray<int[]> sparseArray, String[] strArr2, String str) {
        if (i == 0) {
            return str;
        }
        if (strArr2[i] != null) {
            return strArr2[i];
        }
        int i2 = sparseArray.get(i)[0];
        String parentDependencies = getParentDependencies(i2, strArr, sparseArray, strArr2, str);
        if (i2 != 0) {
            parentDependencies = encodeClassLoaderChain(strArr[i2 - 1], parentDependencies);
        }
        strArr2[i] = parentDependencies;
        return parentDependencies;
    }

    private static String encodeClasspath(String[] strArr) {
        if (strArr == null || strArr.length == 0) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        StringBuilder sb = new StringBuilder();
        for (String str : strArr) {
            if (sb.length() != 0) {
                sb.append(":");
            }
            sb.append(str);
        }
        return sb.toString();
    }

    private static String encodeClasspath(String str, String str2) {
        if (str.isEmpty()) {
            return str2;
        }
        return str + ":" + str2;
    }

    static String encodeClassLoader(String str, String str2) {
        if (str.equals(PackageDexOptimizer.SKIP_SHARED_LIBRARY_CHECK)) {
            return str;
        }
        if (ClassLoaderFactory.isPathClassLoaderName(str2)) {
            str2 = "PCL";
        } else if (ClassLoaderFactory.isDelegateLastClassLoaderName(str2)) {
            str2 = "DLC";
        } else {
            Slog.wtf(TAG, "Unsupported classLoaderName: " + str2);
        }
        return str2 + "[" + str + "]";
    }

    static String encodeClassLoaderChain(String str, String str2) {
        if (str.equals(PackageDexOptimizer.SKIP_SHARED_LIBRARY_CHECK) || str2.equals(PackageDexOptimizer.SKIP_SHARED_LIBRARY_CHECK)) {
            return PackageDexOptimizer.SKIP_SHARED_LIBRARY_CHECK;
        }
        if (str.isEmpty()) {
            return str2;
        }
        if (str2.isEmpty()) {
            return str;
        }
        return str + ";" + str2;
    }

    static String[] processContextForDexLoad(List<String> list, List<String> list2) {
        if (list.size() != list2.size()) {
            throw new IllegalArgumentException("The size of the class loader names and the dex paths do not match.");
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Empty classLoadersNames");
        }
        String strEncodeClassLoaderChain = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        for (int i = 1; i < list.size(); i++) {
            if (!ClassLoaderFactory.isValidClassLoaderName(list.get(i))) {
                return null;
            }
            strEncodeClassLoaderChain = encodeClassLoaderChain(strEncodeClassLoaderChain, encodeClassLoader(encodeClasspath(list2.get(i).split(File.pathSeparator)), list.get(i)));
        }
        String str = list.get(0);
        if (!ClassLoaderFactory.isValidClassLoaderName(str)) {
            return null;
        }
        String[] strArrSplit = list2.get(0).split(File.pathSeparator);
        String[] strArr = new String[strArrSplit.length];
        String strEncodeClasspath = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        for (int i2 = 0; i2 < strArrSplit.length; i2++) {
            String str2 = strArrSplit[i2];
            strArr[i2] = encodeClassLoaderChain(encodeClassLoader(strEncodeClasspath, str), strEncodeClassLoaderChain);
            strEncodeClasspath = encodeClasspath(strEncodeClasspath, str2);
        }
        return strArr;
    }

    private static String[] getSplitRelativeCodePaths(ApplicationInfo applicationInfo) {
        String parent = new File(applicationInfo.getBaseCodePath()).getParent();
        String[] splitCodePaths = applicationInfo.getSplitCodePaths();
        String[] strArr = new String[splitCodePaths.length];
        for (int i = 0; i < splitCodePaths.length; i++) {
            File file = new File(splitCodePaths[i]);
            strArr[i] = file.getName();
            String parent2 = file.getParent();
            if (!parent2.equals(parent)) {
                Slog.wtf(TAG, "Split paths have different base paths: " + parent2 + " and " + parent);
            }
        }
        return strArr;
    }
}
