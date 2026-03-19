package com.android.server.pm;

import android.util.Slog;
import java.util.Collections;
import java.util.Comparator;

final class PolicyComparator implements Comparator<Policy> {
    private boolean duplicateFound = false;

    PolicyComparator() {
    }

    public boolean foundDuplicate() {
        return this.duplicateFound;
    }

    @Override
    public int compare(Policy policy, Policy policy2) {
        if (policy.hasInnerPackages() != policy2.hasInnerPackages()) {
            return policy.hasInnerPackages() ? -1 : 1;
        }
        if (policy.getSignatures().equals(policy2.getSignatures())) {
            if (policy.hasGlobalSeinfo()) {
                this.duplicateFound = true;
                Slog.e("SELinuxMMAC", "Duplicate policy entry: " + policy.toString());
            }
            if (!Collections.disjoint(policy.getInnerPackages().keySet(), policy2.getInnerPackages().keySet())) {
                this.duplicateFound = true;
                Slog.e("SELinuxMMAC", "Duplicate policy entry: " + policy.toString());
                return 0;
            }
            return 0;
        }
        return 0;
    }
}
