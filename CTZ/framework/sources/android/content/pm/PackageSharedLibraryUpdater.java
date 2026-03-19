package android.content.pm;

import android.content.pm.PackageParser;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import java.util.ArrayList;

@VisibleForTesting
public abstract class PackageSharedLibraryUpdater {
    public abstract void updatePackage(PackageParser.Package r1);

    static void removeLibrary(PackageParser.Package r1, String str) {
        r1.usesLibraries = ArrayUtils.remove(r1.usesLibraries, str);
        r1.usesOptionalLibraries = ArrayUtils.remove(r1.usesOptionalLibraries, str);
    }

    static <T> ArrayList<T> prefix(ArrayList<T> arrayList, T t) {
        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }
        arrayList.add(0, t);
        return arrayList;
    }

    private static boolean isLibraryPresent(ArrayList<String> arrayList, ArrayList<String> arrayList2, String str) {
        return ArrayUtils.contains(arrayList, str) || ArrayUtils.contains(arrayList2, str);
    }

    static boolean apkTargetsApiLevelLessThanOrEqualToOMR1(PackageParser.Package r1) {
        return r1.applicationInfo.targetSdkVersion < 28;
    }

    void prefixImplicitDependency(PackageParser.Package r4, String str, String str2) {
        ArrayList<String> arrayList = r4.usesLibraries;
        ArrayList<String> arrayList2 = r4.usesOptionalLibraries;
        if (!isLibraryPresent(arrayList, arrayList2, str2)) {
            if (ArrayUtils.contains(arrayList, str)) {
                prefix(arrayList, str2);
            } else if (ArrayUtils.contains(arrayList2, str)) {
                prefix(arrayList2, str2);
            }
            r4.usesLibraries = arrayList;
            r4.usesOptionalLibraries = arrayList2;
        }
    }

    void prefixRequiredLibrary(PackageParser.Package r3, String str) {
        ArrayList<String> arrayList = r3.usesLibraries;
        if (!isLibraryPresent(arrayList, r3.usesOptionalLibraries, str)) {
            r3.usesLibraries = prefix(arrayList, str);
        }
    }
}
