package android.content.pm;

import android.content.pm.PackageBackwardCompatibility;
import android.content.pm.PackageParser;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@VisibleForTesting
public class PackageBackwardCompatibility extends PackageSharedLibraryUpdater {
    private static final PackageBackwardCompatibility INSTANCE;
    private static final String TAG = PackageBackwardCompatibility.class.getSimpleName();
    private final boolean mBootClassPathContainsATB;
    private final boolean mBootClassPathContainsOAHL;
    private final PackageSharedLibraryUpdater[] mPackageUpdaters;

    static {
        ArrayList arrayList = new ArrayList();
        boolean z = !addOptionalUpdater(arrayList, "android.content.pm.OrgApacheHttpLegacyUpdater", new Supplier() {
            @Override
            public final Object get() {
                return new PackageBackwardCompatibility.RemoveUnnecessaryOrgApacheHttpLegacyLibrary();
            }
        });
        arrayList.add(new AndroidTestRunnerSplitUpdater());
        INSTANCE = new PackageBackwardCompatibility(z, !addOptionalUpdater(arrayList, "android.content.pm.AndroidTestBaseUpdater", new Supplier() {
            @Override
            public final Object get() {
                return new PackageBackwardCompatibility.RemoveUnnecessaryAndroidTestBaseLibrary();
            }
        }), (PackageSharedLibraryUpdater[]) arrayList.toArray(new PackageSharedLibraryUpdater[0]));
    }

    private static boolean addOptionalUpdater(List<PackageSharedLibraryUpdater> list, String str, Supplier<PackageSharedLibraryUpdater> supplier) {
        Class clsAsSubclass;
        PackageSharedLibraryUpdater packageSharedLibraryUpdater;
        try {
            clsAsSubclass = PackageBackwardCompatibility.class.getClassLoader().loadClass(str).asSubclass(PackageSharedLibraryUpdater.class);
            Log.i(TAG, "Loaded " + str);
        } catch (ClassNotFoundException e) {
            Log.i(TAG, "Could not find " + str + ", ignoring");
            clsAsSubclass = null;
        }
        boolean z = false;
        if (clsAsSubclass == null) {
            packageSharedLibraryUpdater = supplier.get();
        } else {
            try {
                z = true;
                packageSharedLibraryUpdater = (PackageSharedLibraryUpdater) clsAsSubclass.getConstructor(new Class[0]).newInstance(new Object[0]);
            } catch (ReflectiveOperationException e2) {
                throw new IllegalStateException("Could not create instance of " + str, e2);
            }
        }
        list.add(packageSharedLibraryUpdater);
        return z;
    }

    @VisibleForTesting
    public static PackageSharedLibraryUpdater getInstance() {
        return INSTANCE;
    }

    public PackageBackwardCompatibility(boolean z, boolean z2, PackageSharedLibraryUpdater[] packageSharedLibraryUpdaterArr) {
        this.mBootClassPathContainsOAHL = z;
        this.mBootClassPathContainsATB = z2;
        this.mPackageUpdaters = packageSharedLibraryUpdaterArr;
    }

    @VisibleForTesting
    public static void modifySharedLibraries(PackageParser.Package r1) {
        INSTANCE.updatePackage(r1);
    }

    @Override
    public void updatePackage(PackageParser.Package r5) {
        for (PackageSharedLibraryUpdater packageSharedLibraryUpdater : this.mPackageUpdaters) {
            packageSharedLibraryUpdater.updatePackage(r5);
        }
    }

    @VisibleForTesting
    public static boolean bootClassPathContainsOAHL() {
        return INSTANCE.mBootClassPathContainsOAHL;
    }

    @VisibleForTesting
    public static boolean bootClassPathContainsATB() {
        return INSTANCE.mBootClassPathContainsATB;
    }

    @VisibleForTesting
    public static class AndroidTestRunnerSplitUpdater extends PackageSharedLibraryUpdater {
        @Override
        public void updatePackage(PackageParser.Package r3) {
            prefixImplicitDependency(r3, "android.test.runner", "android.test.mock");
        }
    }

    @VisibleForTesting
    public static class RemoveUnnecessaryOrgApacheHttpLegacyLibrary extends PackageSharedLibraryUpdater {
        @Override
        public void updatePackage(PackageParser.Package r2) {
            removeLibrary(r2, "org.apache.http.legacy");
        }
    }

    @VisibleForTesting
    public static class RemoveUnnecessaryAndroidTestBaseLibrary extends PackageSharedLibraryUpdater {
        @Override
        public void updatePackage(PackageParser.Package r2) {
            removeLibrary(r2, "android.test.base");
        }
    }
}
