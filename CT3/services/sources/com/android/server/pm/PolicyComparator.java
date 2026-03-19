package com.android.server.pm;

import android.util.Slog;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

final class PolicyComparator implements Comparator<Policy> {
    private boolean duplicateFound = false;

    PolicyComparator() {
    }

    public boolean foundDuplicate() {
        return this.duplicateFound;
    }

    @Override
    public int compare(Policy p1, Policy p2) {
        if (p1.hasInnerPackages() != p2.hasInnerPackages()) {
            return p1.hasInnerPackages() ? -1 : 1;
        }
        if (p1.getSignatures().equals(p2.getSignatures())) {
            if (p1.hasGlobalSeinfo()) {
                this.duplicateFound = true;
                Slog.e("SELinuxMMAC", "Duplicate policy entry: " + p1.toString());
            }
            Map<String, String> p1Packages = p1.getInnerPackages();
            Map<String, String> p2Packages = p2.getInnerPackages();
            if (!Collections.disjoint(p1Packages.keySet(), p2Packages.keySet())) {
                this.duplicateFound = true;
                Slog.e("SELinuxMMAC", "Duplicate policy entry: " + p1.toString());
                return 0;
            }
            return 0;
        }
        return 0;
    }
}
