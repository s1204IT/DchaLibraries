package com.mediatek.server.cta;

import android.content.pm.PackageParser;
import com.mediatek.cta.CtaPackageManagerInternal;

public class CtaPackageManagerInternalImpl extends CtaPackageManagerInternal {
    private CtaPermsController mCtaPermsController;

    public CtaPackageManagerInternalImpl(CtaPermsController ctaPermsController) {
        this.mCtaPermsController = ctaPermsController;
    }

    public void linkCtaPermissions(PackageParser.Package r2) {
        if (this.mCtaPermsController != null) {
            this.mCtaPermsController.linkCtaPermissions(r2);
        }
    }
}
