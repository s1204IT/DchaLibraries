package android.content.pm;

import android.content.pm.PackageParser;
import com.android.internal.annotations.VisibleForTesting;

@VisibleForTesting
public class OrgApacheHttpLegacyUpdater extends PackageSharedLibraryUpdater {
    public void updatePackage(PackageParser.Package r2) {
        if (apkTargetsApiLevelLessThanOrEqualToOMR1(r2)) {
            prefixRequiredLibrary(r2, "org.apache.http.legacy");
        }
    }
}
