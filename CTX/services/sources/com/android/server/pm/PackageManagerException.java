package com.android.server.pm;

import android.content.pm.PackageParser;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import com.android.server.pm.Installer;

public class PackageManagerException extends Exception {
    public final int error;

    public PackageManagerException(String str) {
        super(str);
        this.error = RequestStatus.SYS_ETIMEDOUT;
    }

    public PackageManagerException(int i, String str) {
        super(str);
        this.error = i;
    }

    public PackageManagerException(int i, String str, Throwable th) {
        super(str, th);
        this.error = i;
    }

    public PackageManagerException(Throwable th) {
        super(th);
        this.error = RequestStatus.SYS_ETIMEDOUT;
    }

    public static PackageManagerException from(PackageParser.PackageParserException packageParserException) throws PackageManagerException {
        throw new PackageManagerException(packageParserException.error, packageParserException.getMessage(), packageParserException.getCause());
    }

    public static PackageManagerException from(Installer.InstallerException installerException) throws PackageManagerException {
        throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, installerException.getMessage(), installerException.getCause());
    }
}
