package com.android.server.pm;

import android.util.SparseBooleanArray;
import com.android.server.pm.PackageManagerService;

class PackageVerificationState {
    private final PackageManagerService.InstallArgs mArgs;
    private boolean mRequiredVerificationComplete;
    private boolean mRequiredVerificationPassed;
    private final int mRequiredVerifierUid;
    private boolean mSufficientVerificationComplete;
    private boolean mSufficientVerificationPassed;
    private final SparseBooleanArray mSufficientVerifierUids = new SparseBooleanArray();
    private boolean mExtendedTimeout = false;

    public PackageVerificationState(int i, PackageManagerService.InstallArgs installArgs) {
        this.mRequiredVerifierUid = i;
        this.mArgs = installArgs;
    }

    public PackageManagerService.InstallArgs getInstallArgs() {
        return this.mArgs;
    }

    public void addSufficientVerifier(int i) {
        this.mSufficientVerifierUids.put(i, true);
    }

    public boolean setVerifierResponse(int i, int i2) {
        if (i == this.mRequiredVerifierUid) {
            this.mRequiredVerificationComplete = true;
            switch (i2) {
                case 2:
                    this.mSufficientVerifierUids.clear();
                case 1:
                    this.mRequiredVerificationPassed = true;
                    return true;
                default:
                    this.mRequiredVerificationPassed = false;
                    return true;
            }
        } else {
            if (!this.mSufficientVerifierUids.get(i)) {
                return false;
            }
            if (i2 == 1) {
                this.mSufficientVerificationComplete = true;
                this.mSufficientVerificationPassed = true;
            }
            this.mSufficientVerifierUids.delete(i);
            if (this.mSufficientVerifierUids.size() == 0) {
                this.mSufficientVerificationComplete = true;
            }
            return true;
        }
    }

    public boolean isVerificationComplete() {
        if (!this.mRequiredVerificationComplete) {
            return false;
        }
        if (this.mSufficientVerifierUids.size() == 0) {
            return true;
        }
        return this.mSufficientVerificationComplete;
    }

    public boolean isInstallAllowed() {
        if (!this.mRequiredVerificationPassed) {
            return false;
        }
        if (this.mSufficientVerificationComplete) {
            return this.mSufficientVerificationPassed;
        }
        return true;
    }

    public void extendTimeout() {
        if (!this.mExtendedTimeout) {
            this.mExtendedTimeout = true;
        }
    }

    public boolean timeoutExtended() {
        return this.mExtendedTimeout;
    }
}
