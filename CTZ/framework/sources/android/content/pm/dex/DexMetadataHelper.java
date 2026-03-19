package android.content.pm.dex;

import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.util.ArrayMap;
import android.util.jar.StrictJarFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DexMetadataHelper {
    private static final String DEX_METADATA_FILE_EXTENSION = ".dm";

    private DexMetadataHelper() {
    }

    public static boolean isDexMetadataFile(File file) {
        return isDexMetadataPath(file.getName());
    }

    private static boolean isDexMetadataPath(String str) {
        return str.endsWith(DEX_METADATA_FILE_EXTENSION);
    }

    public static long getPackageDexMetadataSize(PackageParser.PackageLite packageLite) {
        Iterator<String> it = getPackageDexMetadata(packageLite).values().iterator();
        long length = 0;
        while (it.hasNext()) {
            length += new File(it.next()).length();
        }
        return length;
    }

    public static File findDexMetadataForFile(File file) {
        File file2 = new File(buildDexMetadataPathForFile(file));
        if (file2.exists()) {
            return file2;
        }
        return null;
    }

    public static Map<String, String> getPackageDexMetadata(PackageParser.Package r0) {
        return buildPackageApkToDexMetadataMap(r0.getAllCodePaths());
    }

    private static Map<String, String> getPackageDexMetadata(PackageParser.PackageLite packageLite) {
        return buildPackageApkToDexMetadataMap(packageLite.getAllCodePaths());
    }

    private static Map<String, String> buildPackageApkToDexMetadataMap(List<String> list) {
        ArrayMap arrayMap = new ArrayMap();
        for (int size = list.size() - 1; size >= 0; size--) {
            String str = list.get(size);
            String strBuildDexMetadataPathForFile = buildDexMetadataPathForFile(new File(str));
            if (Files.exists(Paths.get(strBuildDexMetadataPathForFile, new String[0]), new LinkOption[0])) {
                arrayMap.put(str, strBuildDexMetadataPathForFile);
            }
        }
        return arrayMap;
    }

    public static String buildDexMetadataPathForApk(String str) {
        if (!PackageParser.isApkPath(str)) {
            throw new IllegalStateException("Corrupted package. Code path is not an apk " + str);
        }
        return str.substring(0, str.length() - PackageParser.APK_FILE_EXTENSION.length()) + DEX_METADATA_FILE_EXTENSION;
    }

    private static String buildDexMetadataPathForFile(File file) {
        if (PackageParser.isApkFile(file)) {
            return buildDexMetadataPathForApk(file.getPath());
        }
        return file.getPath() + DEX_METADATA_FILE_EXTENSION;
    }

    public static void validatePackageDexMetadata(PackageParser.Package r1) throws PackageParser.PackageParserException {
        Iterator<String> it = getPackageDexMetadata(r1).values().iterator();
        while (it.hasNext()) {
            validateDexMetadataFile(it.next());
        }
    }

    private static void validateDexMetadataFile(String str) throws PackageParser.PackageParserException {
        try {
            try {
                new StrictJarFile(str, false, false).close();
            } catch (IOException e) {
            }
        } catch (IOException e2) {
            throw new PackageParser.PackageParserException(PackageManager.INSTALL_FAILED_BAD_DEX_METADATA, "Error opening " + str, e2);
        }
    }

    public static void validateDexPaths(String[] strArr) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < strArr.length; i++) {
            if (PackageParser.isApkPath(strArr[i])) {
                arrayList.add(strArr[i]);
            }
        }
        ArrayList arrayList2 = new ArrayList();
        for (String str : strArr) {
            if (isDexMetadataPath(str)) {
                boolean z = true;
                int size = arrayList.size() - 1;
                while (true) {
                    if (size >= 0) {
                        if (str.equals(buildDexMetadataPathForFile(new File((String) arrayList.get(size))))) {
                            break;
                        } else {
                            size--;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                if (!z) {
                    arrayList2.add(str);
                }
            }
        }
        if (!arrayList2.isEmpty()) {
            throw new IllegalStateException("Unmatched .dm files: " + arrayList2);
        }
    }
}
